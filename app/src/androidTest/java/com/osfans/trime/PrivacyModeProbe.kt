// SPDX-License-Identifier: GPL-3.0-or-later
package com.osfans.trime

import android.app.Activity
import android.app.Instrumentation
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.PersistableBundle
import com.osfans.trime.core.Rime
import com.osfans.trime.core.RimeKeyEvent
import com.osfans.trime.daemon.RimeDaemon
import com.osfans.trime.daemon.RimeSession
import com.osfans.trime.data.SymbolHistory
import com.osfans.trime.data.base.DataManager
import com.osfans.trime.data.db.ClipboardHelper
import com.osfans.trime.data.db.CollectionHelper
import com.osfans.trime.data.db.Database
import com.osfans.trime.data.db.DatabaseBean
import com.osfans.trime.data.db.PersistencePermitExpiredException
import com.osfans.trime.data.prefs.AppPrefs
import com.osfans.trime.ime.core.PrivacyMode
import com.osfans.trime.ime.core.PrivacyModeState
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/** Emulator-only acceptance probe for T07. Every persistent fixture has a unique owned prefix. */
internal object PrivacyModeProbe {
    private const val SENSITIVE_CLIP_EXTRA = "android.content.extra.IS_SENSITIVE"

    private data class TreeDigest(
        val entries: Int,
        val sha256: String,
    )

    private fun shell(
        instrumentation: Instrumentation,
        command: String,
    ): String {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor)
            .bufferedReader()
            .use { it.readText() }
    }

    /** Retain continuation lines only while their preceding threadtime record belongs to this process. */
    private fun logForPid(
        output: String,
        pid: Int,
    ): String {
        val header = Regex("^\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d+\\s+(\\d+)\\s+\\d+\\s+[VDIWEFAS]\\s+")
        return buildString {
            var owned = false
            output.lineSequence().forEach { line ->
                header.find(line)?.let { owned = it.groupValues[1] == pid.toString() }
                if (owned) appendLine(line)
            }
        }
    }

    private fun userDbDigest(prefix: String): TreeDigest {
        val root = DataManager.userDataDir
        val paths =
            root.walkTopDown()
                .filter { entry ->
                    entry != root && entry.relativeTo(root).invariantSeparatorsPath
                        .split('/')
                        .any { it.startsWith("$prefix.userdb") }
                }.filterNot { it.name in setOf("LOCK", "LOG", "LOG.old") }
                .sortedBy { it.relativeTo(root).invariantSeparatorsPath }
                .toList()
        val digest = MessageDigest.getInstance("SHA-256")
        paths.forEach { entry ->
            val relative = entry.relativeTo(root).invariantSeparatorsPath
            digest.update(if (entry.isDirectory) 0.toByte() else 1.toByte())
            digest.update(relative.toByteArray(Charsets.UTF_8))
            digest.update(0.toByte())
            if (entry.isFile) {
                entry.inputStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        digest.update(buffer, 0, read)
                    }
                }
            }
        }
        return TreeDigest(paths.size, digest.digest().joinToString("") { "%02x".format(it) })
    }

    private suspend fun stableUserDbDigest(prefix: String): TreeDigest {
        var previous = userDbDigest(prefix)
        repeat(30) {
            delay(100)
            val current = userDbDigest(prefix)
            if (current == previous) return current
            previous = current
        }
        error("Temporary user dictionary did not settle")
    }

    private fun removeOwnedFiles(prefix: String) {
        val root = DataManager.userDataDir
        root.walkBottomUp()
            .filter { it != root && it.name.startsWith(prefix) }
            .forEach { check(!it.exists() || it.deleteRecursively()) { "Could not remove an owned privacy fixture" } }
        check(root.walkTopDown().none { it != root && it.name.startsWith(prefix) }) {
            "An owned privacy fixture remains"
        }
    }

    private fun luaGearEvents(marker: File): List<String> = if (!marker.exists() || marker.length() == 0L) {
        emptyList()
    } else {
        marker.readLines().filter { it.isNotBlank() }.map { it.substringBefore('\t') }
    }

    private fun writeRimeFixture(
        prefix: String,
        privateCandidate: String,
    ): File {
        val dictionary = File(DataManager.userDataDir, "$prefix.dict.yaml")
        val schema = File(DataManager.userDataDir, "$prefix.schema.yaml")
        val luaDirectory = File(DataManager.userDataDir, "lua")
        check(luaDirectory.exists() || luaDirectory.mkdirs()) { "Could not create the Lua fixture directory" }
        val luaModule = File(luaDirectory, "$prefix.lua")
        val luaMarker = File(DataManager.userDataDir, "$prefix.lua_gear_marker")
        check(dictionary.createNewFile() && schema.createNewFile() && luaModule.createNewFile()) {
            "Temporary Rime fixture already exists"
        }
        val markerPath = luaMarker.absolutePath.replace("\\", "\\\\").replace("'", "\\'")
        luaModule.writeText(
            """
            local probe = {}

            local function mark(event, env)
              local marker = assert(io.open('$markerPath', 'ab'))
              marker:write(event, '\t', env.engine.context.input, '\n')
              marker:close()
            end

            local function fini(event)
              return function(env)
                mark('fini_' .. event, env)
              end
            end

            probe.processor = {}
            function probe.processor.func(key, env)
              mark('processor:' .. key:repr(), env)
              return 2
            end
            probe.processor.fini = fini('processor')

            probe.segmentor = {}
            function probe.segmentor.func(segmentation, env)
              mark('segmentor', env)
              return true
            end
            probe.segmentor.fini = fini('segmentor')

            probe.translator = {}
            function probe.translator.func(input, segment, env)
              mark('translator:' .. input, env)
              yield(Candidate('lua_probe', segment.start, segment._end, 'lua_probe_decoy', ''))
            end
            probe.translator.fini = fini('translator')

            probe.filter = {}
            function probe.filter.tags_match(segment, env)
              mark('filter_tags_match', env)
              return true
            end
            function probe.filter.func(input, env, candidates)
              mark('filter', env)
              for candidate in input:iter() do
                yield(candidate)
              end
            end
            probe.filter.fini = fini('filter')

            return probe
            """.trimIndent() + "\n",
        )
        dictionary.writeText(
            """
            ---
            name: $prefix
            version: "1"
            sort: by_weight
            use_preset_vocabulary: false
            ...
            decoy	q	100
            $privateCandidate	q	1
            """.trimIndent() + "\n",
        )
        schema.writeText(
            """
            schema:
              schema_id: $prefix
              name: T07 privacy probe
              version: "1"

            switches:
              - name: ascii_mode
                reset: 0
                states: [ native, ascii ]

            engine:
              processors:
                - lua_processor@*$prefix*processor
                - ascii_composer
                - chord_composer
                - key_binder
                - speller
                - punctuator
                - selector
                - navigator
                - express_editor
              segmentors:
                - lua_segmentor@*$prefix*segmentor
                - ascii_segmentor
                - abc_segmentor
                - punct_segmentor
                - fallback_segmentor
              translators:
                - punct_translator
                - lua_translator@*$prefix*translator
                - table_translator
              filters:
                - lua_filter@*$prefix*filter
                - uniquifier

            speller:
              alphabet: q
              delimiter: " '"

            chord_composer:
              alphabet: x
              bindings:
                Return: commit_raw_input

            ascii_composer:
              switch_key:
                Shift_L: inline_ascii

            key_binder:
              bindings:
                - { when: always, accept: y, toggle: __privacy_probe_binding }

            punctuator:
              half_shape:
                '"': { pair: [ PAIR_OPEN, PAIR_CLOSE ] }

            translator:
              dictionary: $prefix
              enable_user_dict: true
              enable_sentence: false
              enable_completion: false
            """.trimIndent() + "\n",
        )
        return luaMarker
    }

    fun run(instrumentation: Instrumentation) {
        val result = Bundle()
        var passed = false
        var checks = 0
        val pid = android.os.Process.myPid()
        val fixturePrefix = "__t07_privacy_${pid}_${UUID.randomUUID().toString().replace("-", "")}"
        val sensitiveClip = "virtualotp${UUID.randomUUID().toString().replace("-", "")}"
        val appOwnedSensitiveClip = "trimesensitivelog${UUID.randomUUID().toString().replace("-", "")}"
        val secondAppOwnedSensitiveClip = "trimesensitivecrash${UUID.randomUUID().toString().replace("-", "")}"
        val privateClip = "virtualpin${UUID.randomUUID().toString().replace("-", "")}"
        val normalClip = "ordinarytext${UUID.randomUUID().toString().replace("-", "")}"
        val editableClip = "editableclip${UUID.randomUUID().toString().replace("-", "")}"
        val editableCollection = "editablecollection${UUID.randomUUID().toString().replace("-", "")}"
        val privateClipEdit = "privateclipedit${UUID.randomUUID().toString().replace("-", "")}"
        val privateCollectionEdit = "privatecollectionedit${UUID.randomUUID().toString().replace("-", "")}"
        val privateSymbol = "privatesymbol${UUID.randomUUID().toString().replace("-", "")}"
        val retainedSymbol = "retainedsymbol${UUID.randomUUID().toString().replace("-", "")}"
        val normalSymbol = "normalsymbol${UUID.randomUUID().toString().replace("-", "")}"
        val privateCandidate = "virtualpassword${UUID.randomUUID().toString().replace("-", "")}"
        val privacyOwner = System.nanoTime().let { if (it == 0L) 1L else it }
        val privateEditor = privacyOwner xor 0x51A7L
        val normalEditor = privacyOwner xor 0x6E0BL
        val secondNormalEditor = privacyOwner xor 0x71B2L
        val sessionName = "${PrivacyModeProbe::class.java.name}-$pid"
        var session: RimeSession? = null
        var originalSchema: String? = null
        var privateEditorStarted = false
        var normalEditorStarted = false
        var stateStarted = false
        var enabledSchemaCount = 0
        var recordedFailure: Throwable? = null
        val generalPrefs = AppPrefs.defaultInstance().general
        val clipboardPrefs = AppPrefs.defaultInstance().clipboard
        val previousManualPrivacy = generalPrefs.manualPrivacyMode.getValue()
        val previousClipboardListening = clipboardPrefs.clipboardListening.getValue()
        val previousLastBean = ClipboardHelper.lastBean
        val symbolHistoryFile = File(instrumentation.targetContext.filesDir, SymbolHistory.FILE_NAME)
        val previousSymbolHistory = symbolHistoryFile.takeIf(File::exists)?.readBytes()
        val clipboard = instrumentation.targetContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val database = ClipboardHelper::class.java.getDeclaredField("clbDb").let {
            it.isAccessible = true
            it.get(ClipboardHelper) as Database
        }
        val dao = database.databaseDao()
        val collectionDatabase = CollectionHelper::class.java.getDeclaredField("cltDb").let {
            it.isAccessible = true
            it.get(CollectionHelper) as Database
        }
        val collectionDao = collectionDatabase.databaseDao()

        fun phase(name: String) {
            checks++
            instrumentation.sendStatus(0, Bundle().apply { putString("stream", "PASS $checks: $name\n") })
        }

        fun setClipboard(clip: ClipData) {
            instrumentation.runOnMainSync {
                clipboard.setPrimaryClip(clip)
                // The listener is asynchronous on some releases. Invoke the same production entry
                // point once so a negative assertion never passes because delivery was late.
                ClipboardHelper.onPrimaryClipChanged()
            }
        }

        suspend fun assertNotCaptured(text: String) {
            delay(750)
            check(dao.find(text) == null) { "Private clipboard content reached Room" }
            check(ClipboardHelper.lastBean?.text != text) { "Private clipboard content reached the in-memory suggestion" }
        }

        suspend fun awaitCaptured(text: String): DatabaseBean = withTimeout(15_000) {
            var captured: DatabaseBean? = null
            while (captured == null) {
                captured = dao.find(text)?.takeIf { ClipboardHelper.lastBean?.text == text }
                if (captured != null) break
                delay(50)
            }
            checkNotNull(captured)
        }

        try {
            check(Build.HARDWARE in setOf("ranchu", "goldfish")) { "Emulator only" }
            check(File(instrumentation.targetContext.getExternalFilesDir(null), "runtime-audit-dedicated").isFile) {
                "Requires a dedicated runtime-audit installation"
            }
            check(RimeDaemon.getFirstSessionOrNull() == null) { "Privacy probe requires an idle Rime daemon" }
            instrumentation.runOnMainSync {
                generalPrefs.manualPrivacyMode.setValue(false)
                clipboardPrefs.clipboardListening.setValue(true)
                PrivacyModeState.initialize(manual = false)
                ClipboardHelper.clearTransientSuggestion()
            }

            runBlocking {
                withTimeout(600_000) {
                    instrumentation.runOnMainSync {
                        ClipboardHelper.setSensitivePrimaryClip(
                            clipboard,
                            ClipData.newPlainText("private app log", appOwnedSensitiveClip),
                        )
                        ClipboardHelper.setSensitivePrimaryClip(
                            clipboard,
                            ClipData.newPlainText("private app crash", secondAppOwnedSensitiveClip),
                        )
                    }
                    instrumentation.waitForIdleSync()
                    assertNotCaptured(appOwnedSensitiveClip)
                    assertNotCaptured(secondAppOwnedSensitiveClip)
                    phase("consecutive app-owned sensitive clips are excluded on API 21 and later")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val clip = ClipData.newPlainText("private test", sensitiveClip)
                        val extras = clip.description.extras?.let(::PersistableBundle) ?: PersistableBundle()
                        extras.putBoolean(SENSITIVE_CLIP_EXTRA, true)
                        clip.description.extras = extras
                        setClipboard(clip)
                        assertNotCaptured(sensitiveClip)
                        phase("sensitive ClipDescription is excluded from history and suggestions")
                    } else {
                        phase("API 21 correctly skips unsupported sensitive ClipDescription metadata")
                    }

                    val luaGearMarker = writeRimeFixture(fixturePrefix, privateCandidate)
                    session = RimeDaemon.createSession(sessionName)
                    val activeSession = checkNotNull(session)
                    activeSession.runOnReady {
                        originalSchema =
                            selectedSchemaId().also {
                                check(it.isNotBlank()) { "Original schema has no identifier" }
                            }
                        check(Rime.deployRimeSchemaFile(File(DataManager.userDataDir, "$fixturePrefix.schema.yaml").path)) {
                            "Temporary privacy schema deployment failed"
                        }
                        check(selectSchema(fixturePrefix)) { "Temporary privacy schema selection failed" }
                        setRuntimeOption("ascii_mode", false)
                        clearComposition()
                    }
                    check(luaGearEvents(luaGearMarker).isEmpty()) {
                        "The Lua gear marker was not clean before private input"
                    }
                    val beforePrivate = stableUserDbDigest(fixturePrefix)

                    setClipboard(ClipData.newPlainText("editable test", editableClip))
                    val editableClipboardBean = awaitCaptured(editableClip)
                    val editableCollectionBean =
                        checkNotNull(
                            collectionDao.get(
                                collectionDao.insert(DatabaseBean(text = editableCollection)),
                            ),
                        )
                    SymbolHistory(180).apply {
                        load()
                        insert(retainedSymbol)
                        save()
                    }
                    // This resident instance intentionally remains unloaded until privacy is on.
                    // Its first ordinary insert must restore the disk history before saving.
                    val sharedSymbolHistory = SymbolHistory(180)

                    shell(instrumentation, "logcat -c")
                    PrivacyModeState.beginInput(privacyOwner, PrivacyMode(password = true))
                    stateStarted = true
                    setClipboard(ClipData.newPlainText("private test", privateClip))
                    assertNotCaptured(privateClip)
                    phase("active unified privacy state excludes an ordinary clipboard item")

                    check(
                        runCatching {
                            ClipboardHelper.updateText(editableClipboardBean.id, privateClipEdit)
                        }.exceptionOrNull() is PersistencePermitExpiredException,
                    ) { "Privacy mode did not reject a clipboard history edit" }
                    check(
                        runCatching {
                            CollectionHelper.updateText(editableCollectionBean.id, privateCollectionEdit)
                        }.exceptionOrNull() is PersistencePermitExpiredException,
                    ) { "Privacy mode did not reject a collection edit" }
                    check(dao.get(editableClipboardBean.id)?.text == editableClip && dao.find(privateClipEdit) == null) {
                        "Privacy mode changed an existing clipboard history row"
                    }
                    check(
                        collectionDao.get(editableCollectionBean.id)?.text == editableCollection &&
                            collectionDao.find(privateCollectionEdit) == null,
                    ) { "Privacy mode changed an existing collection row" }
                    phase("privacy mode rejects edits to existing clipboard and collection rows")

                    sharedSymbolHistory.load()
                    val symbolHistoryBeforePrivateSave = symbolHistoryFile.readBytes()
                    sharedSymbolHistory.insert(privateSymbol)
                    sharedSymbolHistory.save()
                    check(sharedSymbolHistory.toOrderedList().isEmpty()) { "Private symbol history remained visible" }
                    check(symbolHistoryFile.readBytes().contentEquals(symbolHistoryBeforePrivateSave)) {
                        "Private symbol input changed symbol_history"
                    }
                    check(!symbolHistoryFile.readText().contains(privateSymbol)) {
                        "Private symbol input reached symbol_history"
                    }
                    phase("privacy mode hides symbol history and rejects symbol persistence")

                    activeSession.runOnReady {
                        beginEditor(privateEditor, nullInputType = false, privacyMode = true)
                        privateEditorStarted = true
                        check(Rime.isRimePrivacyGuardEnabledForTesting()) { "Native privacy guard is inactive" }
                        check(Rime.getRimeCommitHistorySizeForTesting() == 0) { "Private editor inherited commit history" }

                        suspend fun commitFixtureCandidate() {
                            clearComposition()
                            check(processKey('q'.code)) { "Fixture key was not processed" }
                            val index = getCandidates(0, 10).indexOfFirst { it.text == privateCandidate }
                            check(index >= 0) { "Fixture candidate is unavailable" }
                            check(selectCandidate(index, false)) { "Fixture candidate was not selected" }
                            check(getRawInput().isEmpty()) { "Fixture candidate did not commit" }
                        }

                        repeat(8) {
                            commitFixtureCandidate()
                            check(Rime.getRimeCommitHistorySizeForTesting() == 0) {
                                "Private commit entered Rime context history"
                            }
                        }
                        check(luaGearEvents(luaGearMarker).isEmpty()) {
                            "A Lua gear callback observed private input before schema teardown"
                        }

                        check(selectSchema(checkNotNull(originalSchema))) {
                            "Could not tear down the private Lua fixture"
                        }
                        check(Rime.isRimePrivacyGuardEnabledForTesting()) {
                            "Private Lua teardown disabled the privacy guard"
                        }
                        check(Rime.getRimeCommitHistorySizeForTesting() == 0) {
                            "Private Lua teardown restored private history"
                        }
                        check(
                            Rime.deployRimeSchemaFile(
                                File(DataManager.userDataDir, "$fixturePrefix.schema.yaml").path,
                            ),
                        ) { "Private Lua fixture redeployment failed" }
                        check(selectSchema(fixturePrefix)) { "Private Lua fixture reselection failed" }
                        setRuntimeOption("ascii_mode", false)
                        clearComposition()
                        check(processKey('q'.code)) { "Redeployed private fixture key was not processed" }
                        check(getCandidates(0, 10).any { it.text == privateCandidate }) {
                            "Redeployed private fixture candidate is unavailable"
                        }
                        check(selectSchema(checkNotNull(originalSchema))) {
                            "Could not tear down the redeployed private Lua fixture"
                        }
                        check(Rime.isRimePrivacyGuardEnabledForTesting()) {
                            "Redeployed private Lua teardown disabled the privacy guard"
                        }
                        check(Rime.getRimeCommitHistorySizeForTesting() == 0) {
                            "Redeployed private Lua teardown restored private history"
                        }

                        val enabledSchemaIds = enabledSchemata().map { it.id }
                        check(enabledSchemaIds.isNotEmpty()) { "No enabled schema is available for privacy validation" }
                        check(enabledSchemaIds.none { it.isBlank() }) { "An enabled schema has no identifier" }
                        enabledSchemaIds.distinct().also { schemaIds ->
                            enabledSchemaCount = schemaIds.size
                            schemaIds.forEach { schemaId ->
                                clearComposition()
                                check(selectSchema(schemaId)) { "Could not select enabled schema '$schemaId'" }
                                check(selectedSchemaId() == schemaId) { "Enabled schema '$schemaId' was not selected" }
                                check(Rime.isRimePrivacyGuardEnabledForTesting()) {
                                    "Enabled schema '$schemaId' disabled the privacy guard"
                                }
                                check(Rime.getRimeCommitHistorySizeForTesting() == 0) {
                                    "Enabled schema '$schemaId' restored private history"
                                }
                            }
                        }
                        check(selectSchema(checkNotNull(originalSchema))) { "Could not restore the original schema" }
                        check(Rime.isRimePrivacyGuardEnabledForTesting()) { "Original schema restore disabled the privacy guard" }
                        check(Rime.getRimeCommitHistorySizeForTesting() == 0) { "Original schema restore restored private history" }

                        // Keep the malicious fixture alive across the editor boundary. Left
                        // exercises Navigator::input_/spans_, while the unhandled printable key
                        // would remain in ChordComposer::raw_sequence_ without guarded cleanup.
                        check(selectSchema(fixturePrefix)) { "Could not select the built-in state fixture" }
                        setRuntimeOption("ascii_mode", false)
                        clearComposition()
                        check(processKey('q'.code)) { "Private navigation fixture key was not processed" }
                        check(processKey(RimeKeyEvent.getKeycodeByName("Left"))) {
                            "Navigator did not process a private composition"
                        }
                        clearComposition()
                        setRuntimeOption("_auto_commit", false)
                        val privatePairPreviews = mutableListOf<String?>()
                        repeat(3) {
                            check(processKey('"'.code)) { "Private paired punctuation was not processed" }
                            privatePairPreviews += compositionCached.commitTextPreview
                            clearComposition()
                        }
                        setRuntimeOption("_auto_commit", true)
                        check(privatePairPreviews == listOf("PAIR_OPEN", "PAIR_CLOSE", "PAIR_OPEN")) {
                            "Private paired punctuation lost its in-session alternation: $privatePairPreviews"
                        }
                        check(!processKey('z'.code)) { "Unhandled private chord fixture key was consumed" }
                        check(getRawInput().isEmpty()) { "Unhandled private chord fixture changed the Rime context" }
                        check(!processKey(RimeKeyEvent.getKeycodeByName("Return"))) {
                            "A directly committed private key was replayed by ChordComposer"
                        }
                        check(processKey('y'.code)) { "Private key-binding fixture was not processed" }
                        check(processKey('q'.code)) { "Private inline-ASCII fixture key was not processed" }
                        val shiftDown = RimeKeyEvent.parse("Shift_L")
                        val shiftUp = RimeKeyEvent.parse("Shift_L+Release")
                        processKey(shiftDown.value, shiftDown.modifiers.toUInt())
                        processKey(shiftUp.value, shiftUp.modifiers.toUInt())
                        check(getRuntimeOption("ascii_mode")) { "Private inline ASCII mode did not activate" }

                        // The schema switcher owns a temporary Context. Exercise a privacy
                        // change while it is active so the primary editor Context and its
                        // stateful gears cannot survive behind the switcher and replay later.
                        clearComposition()
                        setRuntimeOption("ascii_mode", false)
                        check(processKey('q'.code)) { "Private switcher fixture key was not processed" }
                        check(processKey(RimeKeyEvent.getKeycodeByName("Left"))) {
                            "Navigator did not retain the private switcher fixture"
                        }
                        val privateChordDown = RimeKeyEvent.parse("x")
                        check(processKey(privateChordDown.value, privateChordDown.modifiers.toUInt())) {
                            "ChordComposer did not retain the private held key"
                        }
                        check(processKey(RimeKeyEvent.getKeycodeByName("F4"))) {
                            "Schema switcher did not activate during private input"
                        }
                        check(selectedSchemaId() == ".default") {
                            "F4 was consumed without activating the schema switcher"
                        }
                        beginEditor(privateEditor, nullInputType = false, privacyMode = false)
                        check(!Rime.isRimePrivacyGuardEnabledForTesting()) {
                            "Manual privacy exit left the native guard enabled"
                        }
                        check(!getRuntimeOption("trime_privacy_mode")) {
                            "Primary Rime context retained the private runtime option"
                        }
                        check(!commitComposition()) {
                            "Private primary composition replayed after leaving an active switcher"
                        }
                        check(getRawInput().isEmpty()) {
                            "Private primary input survived an active-switcher boundary"
                        }
                        beginEditor(privateEditor, nullInputType = false, privacyMode = true)
                        check(Rime.isRimePrivacyGuardEnabledForTesting()) {
                            "Manual privacy re-entry did not restore the native guard"
                        }
                        check(selectedSchemaId() == fixturePrefix) {
                            "Schema switcher remained selected after the privacy boundary"
                        }
                        check(getRuntimeOption("trime_privacy_mode")) {
                            "Primary Rime context did not receive the private runtime option"
                        }
                        val privateChordUp = RimeKeyEvent.parse("Release+x")
                        check(!processKey(privateChordUp.value, privateChordUp.modifiers.toUInt())) {
                            "Held private chord replayed after the active-switcher boundary"
                        }
                        check(!processKey(RimeKeyEvent.getKeycodeByName("Escape"))) {
                            "Schema switcher remained active across a privacy boundary"
                        }
                        check(getRawInput().isEmpty()) {
                            "Switcher navigation or chord state replayed after privacy re-entry"
                        }
                        check(Rime.getRimeCommitHistorySizeForTesting() == 0) {
                            "Active-switcher transition restored private history"
                        }
                    }
                    val afterPrivateCommits = stableUserDbDigest(fixturePrefix)
                    check(afterPrivateCommits == beforePrivate) { "Private input changed the fixture user dictionary" }
                    check(luaGearEvents(luaGearMarker).isEmpty()) {
                        "A Lua gear callback or fini hook wrote through io.open during private input"
                    }
                    phase(
                        "malicious Lua processor, segmentor, translator, filter/tags_match and fini hooks " +
                            "remain inert across private teardown and redeployment",
                    )
                    activeSession.runOnReady {
                        check(selectedSchemaId() == fixturePrefix) { "Built-in state fixture was not retained" }
                        check(Rime.isRimePrivacyGuardEnabledForTesting()) { "Schema switch disabled the privacy guard" }
                        check(Rime.getRimeCommitHistorySizeForTesting() == 0) { "Schema switch restored private history" }
                        finishOwnedEditors(longArrayOf(privateEditor + 1))
                        check(Rime.isRimePrivacyGuardEnabledForTesting()) {
                            "A foreign service generation cleared the private editor"
                        }
                        finishOwnedEditors(longArrayOf(privateEditor - 1, privateEditor))
                        privateEditorStarted = false
                        check(!Rime.isRimePrivacyGuardEnabledForTesting()) {
                            "Privacy guard remained enabled after owned-editor cleanup"
                        }
                    }
                    val afterPrivate = stableUserDbDigest(fixturePrefix)
                    check(afterPrivate == beforePrivate) { "Private input changed the fixture user dictionary" }
                    PrivacyModeState.endInput(privacyOwner)
                    stateStarted = false
                    phase(
                        "private commits, $enabledSchemaCount enabled schemas and active-switcher boundaries " +
                            "retain the guard without history or userdb writes",
                    )

                    val privateLogs = logForPid(shell(instrumentation, "logcat -d -v threadtime"), pid)
                    check(
                        listOf(privateClip, privateClipEdit, privateCollectionEdit, privateSymbol, privateCandidate)
                            .none { privateLogs.contains(it) },
                    ) {
                        "Private content appeared in application log output"
                    }
                    phase("application log output contains no private fixture content")

                    setClipboard(ClipData.newPlainText("ordinary test", normalClip))
                    awaitCaptured(normalClip)
                    phase("ordinary clipboard persistence resumes after privacy mode")

                    sharedSymbolHistory.insert(normalSymbol)
                    sharedSymbolHistory.save()
                    val resumedSymbolHistory = symbolHistoryFile.readText()
                    check(
                        resumedSymbolHistory.contains(retainedSymbol) &&
                            resumedSymbolHistory.contains(normalSymbol) &&
                            !resumedSymbolHistory.contains(privateSymbol),
                    ) {
                        "Normal symbol history did not preserve ordinary state or reject private input"
                    }
                    phase("ordinary symbol history persistence resumes after privacy mode")

                    activeSession.runOnReady {
                        beginEditor(normalEditor, nullInputType = false, privacyMode = false)
                        normalEditorStarted = true
                        check(!Rime.isRimePrivacyGuardEnabledForTesting()) { "Normal editor unexpectedly enabled privacy" }
                        check(!getRuntimeOption("ascii_mode")) { "Private inline ASCII mode crossed the editor boundary" }
                        check(selectSchema(fixturePrefix)) { "Could not select the normal-learning fixture" }
                        setRuntimeOption("ascii_mode", false)
                        clearComposition()
                        setRuntimeOption("_auto_commit", false)
                        check(processKey('"'.code)) { "Ordinary paired punctuation was not processed" }
                        check(compositionCached.commitTextPreview == "PAIR_OPEN") {
                            "Private punctuation parity crossed the editor boundary"
                        }
                        clearComposition()
                        setRuntimeOption("_auto_commit", true)
                        check(!processKey(RimeKeyEvent.getKeycodeByName("Return"))) {
                            "Private ChordComposer raw input crossed the editor boundary"
                        }
                        check(getRawInput().isEmpty()) { "Private chord state restored a normal composition" }
                        check(processKey('q'.code)) { "Normal navigation fixture key was not processed" }
                        check(processKey(RimeKeyEvent.getKeycodeByName("Left"))) {
                            "Navigator did not resume in ordinary mode"
                        }
                        check(getRawInput() == "q") { "Ordinary navigation changed the fixture input" }
                        clearComposition()
                        check(!processKey('z'.code)) { "Ordinary direct-key fixture was unexpectedly consumed" }
                        check(processKey('y'.code)) { "Ordinary key-binding fixture was not processed" }
                        beginEditor(secondNormalEditor, nullInputType = false, privacyMode = false)
                        check(!processKey(RimeKeyEvent.getKeycodeByName("Return"))) {
                            "Ordinary ChordComposer raw input crossed an editor boundary"
                        }
                        check(getRawInput().isEmpty()) { "Ordinary editor boundary restored stale chord input" }
                    }
                    phase("stateful chord, navigation and paired-punctuation gears clear only at editor boundaries")
                    val beforeNormal = stableUserDbDigest(fixturePrefix)
                    activeSession.runOnReady {
                        check(processKey('q'.code)) { "Normal fixture key was not processed" }
                        val index = getCandidates(0, 10).indexOfFirst { it.text == privateCandidate }
                        check(index >= 0) { "Normal fixture candidate is unavailable" }
                        val ordinaryEvents = luaGearEvents(luaGearMarker)
                        check(ordinaryEvents.any { it.startsWith("processor:") }) {
                            "The Lua processor fixture did not execute in ordinary mode"
                        }
                        check("segmentor" in ordinaryEvents) {
                            "The Lua segmentor fixture did not execute in ordinary mode"
                        }
                        check(ordinaryEvents.any { it.startsWith("translator:") }) {
                            "The Lua translator fixture did not execute in ordinary mode"
                        }
                        check("filter_tags_match" in ordinaryEvents) {
                            "The Lua filter tags_match fixture did not execute in ordinary mode"
                        }
                        check("filter" in ordinaryEvents) {
                            "The Lua filter fixture did not execute in ordinary mode"
                        }
                        check(selectCandidate(index, false)) { "Normal fixture candidate was not selected" }
                        check(getRawInput().isEmpty()) { "Normal fixture candidate did not commit" }
                        // Memory batches a selected candidate until an unhandled key or the next
                        // query. Force that production boundary before reading the LevelDB tree.
                        processKey(' '.code)
                        check(Rime.getRimeCommitHistorySizeForTesting() > 0) { "Normal commit did not enter Rime history" }
                    }
                    val afterNormal = stableUserDbDigest(fixturePrefix)
                    check(afterNormal != beforeNormal && afterNormal.entries > 0) {
                        "Normal learning did not change the fixture user dictionary"
                    }
                    activeSession.runOnReady {
                        check(selectSchema(checkNotNull(originalSchema))) {
                            "Could not tear down the ordinary Lua fixture"
                        }
                        check(!Rime.isRimePrivacyGuardEnabledForTesting()) {
                            "Ordinary Lua teardown unexpectedly restored privacy mode"
                        }
                    }
                    val eventsAfterNormalTeardown = luaGearEvents(luaGearMarker).toSet()
                    check(
                        setOf(
                            "fini_processor",
                            "fini_segmentor",
                            "fini_translator",
                            "fini_filter",
                        ).all { it in eventsAfterNormalTeardown },
                    ) { "Ordinary Lua fini hooks did not all execute after private-mode recovery" }
                    phase("normal commit history and user dictionary learning resume")
                    phase("all Lua gear callbacks and fini hooks resume after private mode")
                }
            }
            passed = true
            result.putString("stream", "PASS: $checks T07 clipboard, Rime learning, schema, log and restoration checks\n")
        } catch (failure: Throwable) {
            recordedFailure = failure
            result.putString("stream", "FAIL: ${failure.stackTraceToString()}\n")
        } finally {
            var cleanupFailure: Throwable? = null
            fun clean(block: () -> Unit) {
                runCatching(block).onFailure { failure ->
                    cleanupFailure?.addSuppressed(failure) ?: run { cleanupFailure = failure }
                }
            }
            clean {
                runBlocking {
                    withContext(NonCancellable) {
                        session?.let { activeSession ->
                            activeSession.runOnReady {
                                if (privateEditorStarted) finishEditor(privateEditor)
                                if (normalEditorStarted) {
                                    finishOwnedEditors(longArrayOf(normalEditor, secondNormalEditor))
                                }
                                clearComposition()
                                originalSchema?.let { check(selectSchema(it)) { "Could not restore the original schema" } }
                            }
                        }
                        if (stateStarted) PrivacyModeState.endInput(privacyOwner)
                        listOf(
                            sensitiveClip,
                            appOwnedSensitiveClip,
                            secondAppOwnedSensitiveClip,
                            privateClip,
                            normalClip,
                            editableClip,
                            privateClipEdit,
                        ).forEach { text ->
                            while (true) {
                                val bean = dao.find(text) ?: break
                                ClipboardHelper.delete(bean.id)
                            }
                        }
                        listOf(editableCollection, privateCollectionEdit).forEach { text ->
                            while (true) {
                                val bean = collectionDao.find(text) ?: break
                                CollectionHelper.delete(bean.id)
                            }
                        }
                    }
                }
            }
            clean { session?.let { RimeDaemon.destroySession(sessionName) } }
            clean { removeOwnedFiles(fixturePrefix) }
            clean {
                if (previousSymbolHistory == null) {
                    check(!symbolHistoryFile.exists() || symbolHistoryFile.delete()) {
                        "Could not remove the probe-created symbol history"
                    }
                } else {
                    symbolHistoryFile.writeBytes(previousSymbolHistory)
                }
            }
            clean {
                instrumentation.runOnMainSync {
                    clipboardPrefs.clipboardListening.setValue(false)
                    PrivacyModeState.initialize(manual = false)
                    clipboard.setPrimaryClip(ClipData.newPlainText("privacy-probe-cleanup", ""))
                    ClipboardHelper.onPrimaryClipChanged()
                    generalPrefs.manualPrivacyMode.setValue(previousManualPrivacy)
                    PrivacyModeState.initialize(previousManualPrivacy)
                    ClipboardHelper.lastBean = previousLastBean
                    clipboardPrefs.clipboardListening.setValue(previousClipboardListening)
                }
            }
            cleanupFailure?.let { failure ->
                passed = false
                if (recordedFailure == null) {
                    result.putString("stream", "FAIL: ${failure.stackTraceToString()}\n")
                } else {
                    recordedFailure.addSuppressed(failure)
                }
            }
        }
        result.putBoolean("passed", passed)
        result.putInt("checks", checks)
        result.putInt("enabled_schema_count", enabledSchemaCount)
        instrumentation.finish(if (passed) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
    }
}
