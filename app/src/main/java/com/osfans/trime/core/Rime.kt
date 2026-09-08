/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.core

import com.osfans.trime.BuildConfig
import com.osfans.trime.data.base.DataManager
import com.osfans.trime.data.opencc.OpenCCDictManager
import com.osfans.trime.data.prefs.AppPrefs
import com.osfans.trime.data.sync.ExternalSyncFallback
import com.osfans.trime.data.sync.RimeDataSync
import com.osfans.trime.ime.core.InlinePreeditMode
import com.osfans.trime.util.appContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.minutes

/**
 * Rime JNI and instance methods
 *
 * @see [librime](https://github.com/rime/librime)
 */
class Rime :
    RimeApi,
    RimeLifecycleOwner {
    private val lifecycleRegistry = RimeLifecycleRegistry()
    override val lifecycle get() = lifecycleRegistry

    override val messageFlow = messageFlow_.asSharedFlow()

    override val isReady: Boolean
        get() = lifecycle.currentState == RimeLifecycle.State.READY

    override var schemaCached = RimeSchema.empty()
        private set

    override var statusCached = StatusProto()
        private set

    override var compositionCached = CompositionProto()
        private set

    override var hasMenu: Boolean = false
        private set

    override var paging: Boolean = false
        private set

    override var t9Cached = T9StateProto()
        private set

    private val optionCache = RuntimeOptionCache()

    override fun getRuntimeOptionCached(option: String): Boolean = optionCache[option]

    override suspend fun refreshT9Options() = withRimeContext { emitResponse() }

    override suspend fun t9Action(
        revision: Int,
        action: T9Action,
        start: Int,
        end: Int,
        spelling: String,
    ): Boolean = withRimeContext {
        performRimeT9Action(revision, action.ordinal, start, end, spelling, AppPrefs.defaultInstance().keyboard.t9AssistMask())
            .also { emitResponse() }
    }

    private val dispatcher =
        RimeDispatcher(
            object : RimeDispatcher.RimeController {
                override fun nativeStartup() {
                    // Loading/relocating the native engine must not block Activity or IME creation.
                    System.loadLibrary("rime_jni")
                    startRime(startupFullCheck)
                    lifecycleRegistry.emitEvent(RimeLifecycle.Event.ON_READY)
                }

                override fun nativeFinalize() {
                    stopRime()
                }

                override fun nativeFailure(error: Throwable) {
                    Timber.e(error, "Rime engine lifecycle failed")
                    lifecycleRegistry.fail(error)
                    handleRimeMessage(RimeMessage.MessageType.Deploy.ordinal, arrayOf("failure"))
                    unregisterRimeMessageHandler(::handleRimeMessage)
                }
            },
        )

    private val inlinePreeditMode by AppPrefs.defaultInstance().general.inlinePreeditMode
    private val showAsciiSwitchTips by AppPrefs.defaultInstance().general.asciiSwitchTips

    private var asciiSwitchTipsJob: Job? = null
    private var startupFullCheck = false
    private var nativeInitialized = false
    private var isNullInputType = true
    private var lastAsciiTipsText = ""
    private var pagingMode = false

    init {
        if (lifecycle.currentState != RimeLifecycle.State.STOPPED) {
            throw IllegalStateException("Rime has already been created!")
        }
    }

    private suspend inline fun <T> withRimeContext(crossinline block: suspend () -> T): T = dispatcher.runConfined {
        block()
    }

    override suspend fun isEmpty(): Boolean = withRimeContext {
        getCurrentRimeSchema() == ".default" // 無方案
    }

    override suspend fun deploy(skipImport: Boolean) = RimeMaintenanceMutex.withLock {
        if (RimeDataSync.usesExternalSync()) {
            if (!RimeDataSync.hasExternalAccess(appContext)) {
                ExternalSyncFallback.fallbackToAppStorage(appContext)
            }
        }
        if (RimeDataSync.usesExternalSync() && !skipImport) {
            val importResult =
                RimeDataSync.importToLocal(appContext, keepNotificationUntilDeploySuccess = true)
            if (importResult.isFailure) {
                ExternalSyncFallback.fallbackToAppStorage(appContext, importResult.exceptionOrNull())
            } else {
                Timber.i("Import finished: ${importResult.getOrNull()}")
            }
        }
        val deployFinished = CompletableDeferred<Boolean>()
        val deployHandler: (RimeMessage<*>) -> Unit = { message ->
            if (message is RimeMessage.DeployMessage) {
                when (message.data) {
                    RimeMessage.DeployMessage.State.Start -> Unit
                    RimeMessage.DeployMessage.State.Success -> {
                        deployFinished.complete(true)
                    }
                    RimeMessage.DeployMessage.State.Failure -> {
                        deployFinished.complete(false)
                    }
                }
            }
        }
        registerRimeMessageHandler(deployHandler)
        try {
            withRimeContext {
                restartNative(true)
            }
            val success =
                withContext(Dispatchers.IO) {
                    withTimeout(5.minutes) {
                        deployFinished.await()
                    }
                }
            if (!success) throw RimeUnavailableException(IllegalStateException("Rime deploy failed"))
        } finally {
            unregisterRimeMessageHandler(deployHandler)
        }
    }

    override suspend fun updateConfig() = RimeMaintenanceMutex.withLock {
        withRimeContext { restartNative(false) }
    }

    override suspend fun deployConfigFile(fileName: String, versionKey: String): Boolean = RimeMaintenanceMutex.withLock {
        withRimeContext { deployRimeConfigFile(fileName, versionKey) }
    }

    override suspend fun syncUserData(): Boolean = RimeMaintenanceMutex.withLock {
        // Import configurations and portable text dumps before native merging.
        // Never replace open binary user databases with a file-by-file SAF copy.
        // Suppress import progress so synchronization does not show deployment UI.
        if (RimeDataSync.usesExternalSync() && RimeDataSync.hasExternalAccess(appContext)) {
            if (RimeDataSync.importToLocal(appContext, showProgress = false).isFailure) return@withLock false
        }
        // RimeSyncUserData schedules maintenance asynchronously and returns once the
        // worker is started. Wait for DeployMessage so callers (e.g. export) only
        // proceed after sync/<installation_id>/ has been written.
        val syncFinished = CompletableDeferred<Boolean>()
        val syncHandler: (RimeMessage<*>) -> Unit = { message ->
            if (message is RimeMessage.DeployMessage) {
                when (message.data) {
                    RimeMessage.DeployMessage.State.Success -> syncFinished.complete(true)
                    RimeMessage.DeployMessage.State.Failure -> syncFinished.complete(false)
                    else -> {}
                }
            }
        }
        registerRimeMessageHandler(syncHandler)
        val syncOk =
            try {
                val started = withRimeContext { syncRimeUserData() }
                if (!started) {
                    false
                } else {
                    withContext(Dispatchers.IO) {
                        withTimeout(5.minutes) {
                            syncFinished.await()
                        }
                    }
                }
            } finally {
                unregisterRimeMessageHandler(syncHandler)
            }
        if (!syncOk) return@withLock false
        if (!RimeDataSync.usesExternalSync()) return@withLock true
        if (!RimeDataSync.hasExternalAccess(appContext)) {
            Timber.w("Export skipped: no data path selected")
            return@withLock false
        }
        RimeDataSync.exportToExternal(appContext).isSuccess
    }

    override suspend fun processKey(
        value: Int,
        modifiers: UInt,
        isVirtual: Boolean,
    ): Boolean = withRimeContext {
        processKeyInner(value, modifiers.toInt(), isVirtual)
    }

    override suspend fun processKey(
        value: KeyValue,
        modifiers: KeyModifiers,
        isVirtual: Boolean,
    ): Boolean = withRimeContext {
        processKeyInner(value.value, modifiers.toInt(), isVirtual)
    }

    override suspend fun simulateKeySequence(sequence: String): Boolean = withRimeContext {
        Timber.d("simulateKeySequence: $sequence")
        if (simulateRimeKeySequence(sequence)) {
            val commit = getRimeCommit()
            val input = getRimeRawInput()
            if (!commit.text.isNullOrEmpty() || input.isNotEmpty()) {
                emitResponse(commit)
                true
            } else {
                emitResponse(CommitProto(sequence))
                false
            }
        } else {
            false
        }.also { Timber.d("simulateKeySequence ${if (it) "success" else "failed"}") }
    }

    override suspend fun selectCandidate(idx: Int, global: Boolean): Boolean = withRimeContext {
        selectRimeCandidate(idx, global).also { emitResponse() }
    }

    override suspend fun deleteCandidate(idx: Int, global: Boolean): Boolean = withRimeContext {
        deleteRimeCandidate(idx, global).also { emitResponse() }
    }

    override suspend fun changeCandidatePage(backward: Boolean): Boolean = withRimeContext {
        changeRimeCandidatePage(backward).also { emitResponse() }
    }

    override suspend fun moveCursorPos(position: Int, expectedPreedit: String?) = withRimeContext {
        if (expectedPreedit != null && getRimeContext().composition.preedit != expectedPreedit) return@withRimeContext
        setRimeCaretPos(position)
        emitResponse()
    }

    override suspend fun availableSchemata(): Array<SchemaItem> = withRimeContext { getAvailableRimeSchemaList() }

    override suspend fun enabledSchemata(): Array<SchemaItem> = withRimeContext { getSelectedRimeSchemaList() }

    override suspend fun setEnabledSchemata(schemaIds: Array<String>) = withRimeContext { selectRimeSchemas(schemaIds) }

    override suspend fun selectedSchemata(): Array<SchemaItem> = withRimeContext { getRimeSchemaList() }

    override suspend fun selectedSchemaId(): String = withRimeContext { getCurrentRimeSchema() }

    override suspend fun selectSchema(schemaId: String) = withRimeContext {
        selectRimeSchema(schemaId).also { if (it) emitResponse() }
    }

    override suspend fun currentSchema(): RimeSchema = withRimeContext {
        RimeSchema(getCurrentRimeSchema())
    }

    override suspend fun commitComposition(): Boolean = withRimeContext { commitRimeComposition().also { if (it) emitResponse() } }

    override suspend fun commitT9Digit(digit: Char): Boolean = withRimeContext {
        require(digit in '0'..'9')
        var pending = ""
        if (getRimeRawInput().isNotEmpty()) {
            // Use the same full-input Return path as T02, including a caret inside a locked syllable.
            processRimeKey(RimeKeyMapping.RimeKey_Return, 0)
            // Read the commit before any snapshot can discard its private-to-raw T9 mapping.
            pending = getRimeCommit().text.orEmpty()
            if (getRimeRawInput().isNotEmpty()) {
                emitResponse(CommitProto(pending))
                return@withRimeContext false
            }
        }
        clearRimeComposition()
        emitResponse(CommitProto(pending + digit))
        true
    }

    override suspend fun clearComposition() = withRimeContext {
        clearRimeComposition()
        emitResponse()
    }

    override suspend fun getRawInput(): String = withRimeContext {
        getRimeRawInput()
    }

    override suspend fun setRuntimeOption(
        option: String,
        value: Boolean,
    ): Unit = withRimeContext {
        setRimeOption(option, value)
    }

    override suspend fun getRuntimeOption(option: String): Boolean = withRimeContext {
        getRimeOption(option)
    }

    override suspend fun setNullInputType(value: Boolean) = withRimeContext {
        isNullInputType = value
    }

    override suspend fun getCandidates(
        startIndex: Int,
        limit: Int,
    ): Array<CandidateProto> = withRimeContext {
        getRimeCandidates(startIndex, limit)
    }

    override suspend fun setCandidatePagingMode(enabled: Boolean) = withRimeContext {
        pagingMode = enabled
        emitResponse()
    }

    private fun startRime(fullCheck: Boolean) {
        optionCache.clear()
        DataManager.sync()
        val sharedDataDir = DataManager.sharedDataDir.absolutePath
        val userDataDir = DataManager.userDataDir.absolutePath
        Timber.d(
            """
            Starting rime with:
            sharedDataDir: $sharedDataDir
            userDataDir: $userDataDir
            fullCheck: $fullCheck
            """.trimIndent(),
        )
        val deployFailed = java.util.concurrent.atomic.AtomicBoolean(false)
        val startupHandler: (RimeMessage<*>) -> Unit = { message ->
            if (message is RimeMessage.DeployMessage && message.data == RimeMessage.DeployMessage.State.Failure) {
                deployFailed.set(true)
            }
        }
        registerRimeMessageHandler(startupHandler)
        try {
            nativeInitialized = true
            startupRime(sharedDataDir, userDataDir, BuildConfig.BUILD_VERSION_NAME, fullCheck)
            check(!deployFailed.get()) { "Rime configuration deployment failed" }
        } finally {
            unregisterRimeMessageHandler(startupHandler)
        }
        // Create the engine session and publish restored options before any view
        // uses the schema or toggle labels for the first time.
        emitResponse()
    }

    private fun stopRime() {
        if (!nativeInitialized) return
        try {
            exitRime()
        } finally {
            nativeInitialized = false
        }
    }

    private fun restartNative(fullCheck: Boolean) {
        try {
            stopRime()
            startRime(fullCheck)
        } catch (e: Exception) {
            dispatcher.fail(e)
            throw RimeUnavailableException(e)
        } catch (e: LinkageError) {
            dispatcher.fail(e)
            throw RimeUnavailableException(e)
        }
    }

    private fun processKeyInner(value: Int, modifiers: Int, isVirtual: Boolean): Boolean {
        lastAsciiTipsText = asciiTipsText(getRimeStatus())
        val handled = processRimeKey(value, modifiers)
        emitResponse()
        if (!handled) {
            handleRimeMessage(
                10, // RimeMessage.MessageType.Key,
                arrayOf(value, modifiers, isVirtual),
            )
        }
        return handled
    }

    private fun asciiTipsText(status: StatusProto): String = when {
        status.isAsciiMode -> "En"
        status.schemaName.isNotEmpty() && !status.schemaName.startsWith('.') ->
            status.schemaName.take(2)
        else -> ""
    }

    private fun emitResponse(commit: CommitProto? = null) {
        val response = getRimeResponse(pagingMode)
        val t9 = getRimeT9State(AppPrefs.defaultInstance().keyboard.t9AssistMask())
        handleRimeMessage(4, arrayOf(commit ?: response.commit))
        handlePreedit(response.composition, t9.enabled)
        if (response.composition.length <= 0 && lastAsciiTipsText != asciiTipsText(response.status)) {
            showAsciiSwitchTips(response.status)
        }
        when (val candidates = response.candidates) {
            is Candidates.Paged -> handleRimeMessage(7, arrayOf(candidates))
            is Candidates.Bulk -> handleRimeMessage(9, arrayOf(candidates))
        }
        handleRimeMessage(8, arrayOf(response.status))
        handleRimeMessage(11, arrayOf(t9))
    }

    private fun handlePreedit(composition: CompositionProto, t9: Boolean = false) {
        val mode = if (isNullInputType) {
            InlinePreeditMode.DISABLE
        } else {
            inlinePreeditMode
        }
        val inlinePreedit = when {
            mode == InlinePreeditMode.DISABLE -> InlinePreeditProto("")
            mode == InlinePreeditMode.COMPOSING_TEXT || t9 ->
                InlinePreeditProto(composition.preedit ?: "", composition.cursorPos)
            else -> InlinePreeditProto(composition.commitTextPreview ?: "")
        }
        val composition = if (mode == InlinePreeditMode.COMPOSING_TEXT) {
            CompositionProto()
        } else {
            composition
        }
        handleRimeMessage(5, arrayOf(inlinePreedit))
        handleRimeMessage(6, arrayOf(composition))
    }

    private fun handleRimeMessage(it: RimeMessage<*>) {
        when (it) {
            is RimeMessage.SchemaMessage -> {
                t9Cached = T9StateProto()
                optionCache.clear()
                statusCached = getRimeStatus()
                schemaCached = RimeSchema(it.data.id)
            }
            is RimeMessage.OptionMessage -> {
                optionCache.update(it.data.option, it.data.value)
                // Option change won't trigger response update
                val status = getRimeStatus()
                statusCached = status
                updateSchemaCached(status)
                if (it.data.option == "ascii_mode") {
                    showAsciiSwitchTips(status)
                }
            }
            is RimeMessage.DeployMessage -> {
                if (it.data == RimeMessage.DeployMessage.State.Start) {
                    OpenCCDictManager.buildOpenCCDict()
                }
            }
            is RimeMessage.CompositionMessage -> {
                val composition = it.data
                compositionCached = composition
            }
            is RimeMessage.PagedCandidatesMessage -> {
                val paged = it.data
                paging = paged.hasPrevPage
                hasMenu = paged.candidates.isNotEmpty()
            }
            is RimeMessage.BulkCandidatesMessage -> {
                hasMenu = it.data.candidates.isNotEmpty()
            }
            is RimeMessage.StatusMessage -> {
                statusCached = it.data
                updateSchemaCached(it.data)
                refreshOptionCache()
            }
            is RimeMessage.T9Message -> t9Cached = it.data
            else -> {}
        }
    }

    private fun updateSchemaCached(status: StatusProto) {
        val (schemaId, schemaName) = status
        // Engine response update won't send SchemaMessage, but usually update RimeStatus
        if (schemaId != schemaCached.schemaId) {
            optionCache.clear()
            schemaCached = RimeSchema(schemaId)
            // notify downstream consumers that schema has changed
            messageFlow_.tryEmit(
                RimeMessage.SchemaMessage(
                    SchemaItem(schemaId, schemaName),
                ),
            )
        }
    }

    private fun refreshOptionCache() {
        val options = schemaCached.switches.flatMap {
            if (it.name.isEmpty()) it.options else listOf(it.name)
        }
        (options + "ascii_mode").distinct().forEach { optionCache.update(it, getRimeOption(it)) }
    }

    private fun showAsciiSwitchTips(status: StatusProto) {
        if (!showAsciiSwitchTips) return
        val tipsText = asciiTipsText(status)
        if (tipsText.isEmpty()) return

        lastAsciiTipsText = tipsText

        val tips = CompositionProto(tipsText)
        messageFlow_.tryEmit(RimeMessage.CompositionMessage(tips))
        compositionCached = tips
        asciiSwitchTipsJob?.cancel()
        asciiSwitchTipsJob = lifecycleScope.launch {
            delay(1000L)
            withRimeContext {
                val ctx = getRimeContext()
                handleRimeMessage(6, arrayOf(ctx.composition))
            }
        }
    }

    fun startup(fullCheck: Boolean = false) {
        if (!RimeDataSync.isStorageAvailable(appContext)) {
            lifecycleRegistry.fail(IllegalStateException("Rime storage is unavailable"))
            return
        }
        if (lifecycle.currentState == RimeLifecycle.State.FAILED) dispatcher.stop()
        if (lifecycle.currentState != RimeLifecycle.State.STOPPED && lifecycle.currentState != RimeLifecycle.State.FAILED) {
            Timber.w("Skip starting rime: not at stopped state!")
            return
        }
        registerRimeMessageHandler(::handleRimeMessage)
        startupFullCheck = fullCheck
        lifecycleRegistry.emitEvent(RimeLifecycle.Event.ON_START)
        dispatcher.start()
    }

    fun beginShutdown() {
        lifecycleRegistry.emitEvent(RimeLifecycle.Event.ON_STOP)
    }

    fun finishShutdown() {
        val state = lifecycle.currentState
        if (state == RimeLifecycle.State.FAILED) throw RimeUnavailableException(lifecycle.failureCause)
        check(state == RimeLifecycle.State.STOPPING)
        Timber.i("Rime finalize()")
        dispatcher.stop().let {
            if (it.isNotEmpty()) {
                Timber.w("${it.size} job(s) didn't get a chance to run!")
            }
        }
        if (lifecycle.currentState == RimeLifecycle.State.FAILED) throw RimeUnavailableException(lifecycle.failureCause)
        lifecycleRegistry.emitEvent(RimeLifecycle.Event.ON_STOPPED)
        unregisterRimeMessageHandler(::handleRimeMessage)
    }

    companion object {
        private val messageFlow_ =
            MutableSharedFlow<RimeMessage<*>>(
                extraBufferCapacity = 15,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )

        private val rimeMessageHandlers = CopyOnWriteArrayList<(RimeMessage<*>) -> Unit>()

        // init
        @JvmStatic
        external fun startupRime(
            sharedDir: String,
            userDir: String,
            versionName: String,
            fullCheck: Boolean,
        )

        @JvmStatic
        external fun exitRime()

        @JvmStatic
        external fun deployRimeSchemaFile(schemaFile: String): Boolean

        @JvmStatic
        external fun deployRimeConfigFile(
            fileName: String,
            versionKey: String,
        ): Boolean

        @JvmStatic
        external fun syncRimeUserData(): Boolean

        // input
        @JvmStatic
        external fun getRimeT9State(assistOptions: Int): T9StateProto

        @JvmStatic
        external fun performRimeT9Action(
            revision: Int,
            action: Int,
            start: Int,
            end: Int,
            spelling: String,
            assistOptions: Int,
        ): Boolean

        @JvmStatic
        external fun processRimeKey(
            keycode: Int,
            mask: Int,
        ): Boolean

        @JvmStatic
        external fun commitRimeComposition(): Boolean

        @JvmStatic
        external fun clearRimeComposition()

        // output
        @JvmStatic
        external fun getRimeCommit(): CommitProto

        @JvmStatic
        external fun getRimeContext(): ContextProto

        @JvmStatic
        external fun getRimeStatus(): StatusProto

        // runtime options
        @JvmStatic
        external fun setRimeOption(
            option: String,
            value: Boolean,
        )

        @JvmStatic
        external fun getRimeOption(option: String): Boolean

        @JvmStatic
        external fun getRimeSchemaList(): Array<SchemaItem>

        @JvmStatic
        external fun getCurrentRimeSchema(): String

        @JvmStatic
        external fun selectRimeSchema(schemaId: String): Boolean

        // testing
        @JvmStatic
        external fun simulateRimeKeySequence(keySequence: String): Boolean

        @JvmStatic
        external fun getRimeRawInput(): String

        @JvmStatic
        external fun getRimeCaretPos(): Int

        @JvmStatic
        external fun setRimeCaretPos(caretPos: Int)

        @JvmStatic
        external fun selectRimeCandidate(index: Int, global: Boolean): Boolean

        @JvmStatic
        external fun deleteRimeCandidate(index: Int, global: Boolean): Boolean

        @JvmStatic
        external fun changeRimeCandidatePage(backward: Boolean): Boolean

        @JvmStatic
        external fun getAvailableRimeSchemaList(): Array<SchemaItem>

        @JvmStatic
        external fun getSelectedRimeSchemaList(): Array<SchemaItem>

        @JvmStatic
        external fun selectRimeSchemas(schemaIds: Array<String>): Boolean

        @JvmStatic
        external fun getRimeCandidates(
            startIndex: Int,
            limit: Int,
        ): Array<CandidateProto>

        @JvmStatic
        external fun getRimeResponse(pagingMode: Boolean): RimeResponse

        @JvmStatic
        fun handleRimeMessage(
            type: Int,
            params: Array<Any>,
        ) {
            val message = RimeMessage.nativeCreate(type, params)
            Timber.d("Handling ${message.javaClass.simpleName}")
            rimeMessageHandlers.forEach { it.invoke(message) }
            messageFlow_.tryEmit(message)
        }

        private fun registerRimeMessageHandler(handler: (RimeMessage<*>) -> Unit) {
            if (rimeMessageHandlers.contains(handler)) return
            rimeMessageHandlers.add(handler)
        }

        private fun unregisterRimeMessageHandler(handler: (RimeMessage<*>) -> Unit) {
            rimeMessageHandlers.remove(handler)
        }
    }
}
