// SPDX-License-Identifier: GPL-3.0-or-later
package com.osfans.trime

import android.app.Instrumentation
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.text.InputType
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.osfans.trime.core.Rime
import com.osfans.trime.core.RimeApi
import com.osfans.trime.data.SymbolHistory
import com.osfans.trime.data.base.DataManager
import com.osfans.trime.data.db.ClipboardHelper
import com.osfans.trime.data.db.Database
import com.osfans.trime.data.prefs.AppPrefs
import com.osfans.trime.ime.core.EditorPolicy
import com.osfans.trime.ime.core.PrivacyModeState
import com.osfans.trime.ime.core.TrimeInputMethodService
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.UUID

/** End-to-end T07 checks that start at real EditorInfo/preference boundaries. */
internal object PrivacyTriggerSinkProbe {
    private const val LOG_TAG = "T07TriggerSink"

    internal data class Harness(
        val service: TrimeInputMethodService,
        val chat: EditText,
        val target: EditText,
        val focus: suspend (EditText) -> Unit,
        val runApi: suspend (suspend RimeApi.() -> Unit) -> Unit,
        val inputTextContains: (String) -> Boolean,
        val speechAllowed: () -> Boolean,
        val phase: (String) -> Unit,
    )

    private enum class TriggerKind {
        PASSWORD,
        APP_REQUESTED,
        MANUAL,
    }

    private data class TriggerCase(
        val kind: TriggerKind,
        val code: Char,
        val candidate: String,
        val clipboard: String,
        val existingSuggestion: String,
        val symbol: String,
    ) {
        val label: String
            get() = when (kind) {
                TriggerKind.PASSWORD -> "password inputType"
                TriggerKind.APP_REQUESTED -> "IME_FLAG_NO_PERSONALIZED_LEARNING"
                TriggerKind.MANUAL -> "manual preference"
            }
    }

    private fun writeFixture(
        prefix: String,
        cases: List<TriggerCase>,
        ordinaryCode: Char,
        ordinaryCandidate: String,
    ) {
        val dictionary = File(DataManager.userDataDir, "$prefix.dict.yaml")
        val schema = File(DataManager.userDataDir, "$prefix.schema.yaml")
        check(dictionary.createNewFile() && schema.createNewFile()) {
            "Real-trigger privacy fixture already exists"
        }
        dictionary.writeText(
            buildString {
                appendLine("---")
                appendLine("name: $prefix")
                appendLine("version: \"1\"")
                appendLine("sort: by_weight")
                appendLine("use_preset_vocabulary: false")
                appendLine("...")
                cases.forEachIndexed { index, item ->
                    appendLine("${item.candidate}\t${item.code}\t${100 - index}")
                }
                appendLine("$ordinaryCandidate\t$ordinaryCode\t90")
            },
        )
        val alphabet = (cases.map { it.code } + ordinaryCode).joinToString("")
        schema.writeText(
            """
            schema:
              schema_id: $prefix
              name: T07 real trigger sink probe
              version: "1"

            switches:
              - name: ascii_mode
                reset: 0
                states: [ native, ascii ]

            engine:
              processors:
                - ascii_composer
                - key_binder
                - speller
                - punctuator
                - selector
                - navigator
                - express_editor
              segmentors:
                - ascii_segmentor
                - abc_segmentor
                - punct_segmentor
                - fallback_segmentor
              translators:
                - punct_translator
                - table_translator
              filters:
                - uniquifier

            speller:
              alphabet: $alphabet
              delimiter: " '"

            translator:
              dictionary: $prefix
              enable_user_dict: true
              enable_sentence: false
              enable_completion: false
            """.trimIndent() + "\n",
        )
    }

    suspend fun verify(
        instrumentation: Instrumentation,
        harness: Harness,
    ) {
        val context = instrumentation.targetContext
        fun <T> main(block: () -> T): T {
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) return block()
            var value: Result<T>? = null
            instrumentation.runOnMainSync { value = runCatching(block) }
            return checkNotNull(value).getOrThrow()
        }

        suspend fun awaitMain(
            label: String,
            predicate: () -> Boolean,
        ) {
            withTimeout(30_000) {
                while (!main(predicate)) delay(50)
            }
            harness.phase("PASS: $label")
        }

        val suffix = UUID.randomUUID().toString().replace("-", "")
        val prefix = "__t07_real_triggers_${android.os.Process.myPid()}_$suffix"
        val cases =
            listOf(
                TriggerCase(
                    TriggerKind.PASSWORD,
                    'q',
                    "virtualpassword$suffix",
                    "passwordotp$suffix",
                    "ordinarybeforepassword$suffix",
                    "passwordsymbol$suffix",
                ),
                TriggerCase(
                    TriggerKind.APP_REQUESTED,
                    'w',
                    "nolearningsecret$suffix",
                    "nolearningotp$suffix",
                    "ordinarybeforenolearning$suffix",
                    "nolearningsymbol$suffix",
                ),
                TriggerCase(
                    TriggerKind.MANUAL,
                    'e',
                    "manualsecret$suffix",
                    "manualotp$suffix",
                    "ordinarybeforemanual$suffix",
                    "manualsymbol$suffix",
                ),
            )
        val ordinaryCode = 'r'
        val ordinaryCandidate = "ordinarylearning$suffix"
        val ordinaryClipboard = "ordinaryclipboard$suffix"
        val ordinarySymbol = "ordinarysymbol$suffix"
        val logBoundary = UUID.randomUUID().toString().replace("-", "")
        val logStart = "T07_REAL_TRIGGER_BEGIN_$logBoundary"
        val logEnd = "T07_REAL_TRIGGER_END_$logBoundary"
        val pid = android.os.Process.myPid()

        val clipboardPrefs = AppPrefs.defaultInstance().clipboard
        val manualPrivacy = AppPrefs.defaultInstance().general.manualPrivacyMode
        val previousClipboardListening = clipboardPrefs.clipboardListening.getValue()
        val previousClipboardSuggestion = clipboardPrefs.clipboardSuggestion.getValue()
        val previousClipboardSuggestionTimeout = clipboardPrefs.clipboardSuggestionTimeout.getValue()
        val previousManualPrivacy = manualPrivacy.getValue()
        val previousLastBean = ClipboardHelper.lastBean
        val clipboard =
            main {
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            }
        val previousPrimaryClip = main { clipboard.primaryClip?.let(::ClipData) }
        val database =
            ClipboardHelper::class.java.getDeclaredField("clbDb").let {
                it.isAccessible = true
                it.get(ClipboardHelper) as Database
            }
        val dao = database.databaseDao()
        val expectedSymbolFile =
            TrimeApplication.getInstance().directBootAwareContext.filesDir.resolve(SymbolHistory.FILE_NAME)
        val previousSymbolHistory = expectedSymbolFile.takeIf(File::exists)?.readBytes()
        val symbolHistory = SymbolHistory(180)
        val symbolFile =
            SymbolHistory::class.java.getDeclaredField("file").let {
                it.isAccessible = true
                it.get(symbolHistory) as File
            }
        var schemaBeforeFixture = ""
        var logStarted = false
        var primaryFailure: Throwable? = null

        suspend fun setClipboard(text: String) {
            main {
                clipboard.setPrimaryClip(ClipData.newPlainText("T07 trigger", text))
                check(clipboard.primaryClip?.getItemAt(0)?.text?.toString() == text) {
                    "The real-trigger probe cannot read its own clipboard"
                }
                ClipboardHelper.onPrimaryClipChanged()
            }
        }

        suspend fun assertPrivateClipboardRejected(item: TriggerCase) {
            setClipboard(item.clipboard)
            delay(750)
            check(dao.find(item.clipboard) == null) { "${item.label} clipboard token reached Room" }
            check(ClipboardHelper.lastBean?.text != item.clipboard) {
                "${item.label} clipboard token reached lastBean"
            }
            check(!main { harness.inputTextContains(item.clipboard) }) {
                "${item.label} clipboard token reached input-bar text"
            }
        }

        suspend fun publishExistingSuggestion(item: TriggerCase) {
            setClipboard(item.existingSuggestion)
            withTimeout(15_000) {
                while (true) {
                    val persisted = dao.find(item.existingSuggestion) != null
                    val published = ClipboardHelper.lastBean?.text == item.existingSuggestion
                    val rendered = main { harness.inputTextContains(item.existingSuggestion) }
                    if (persisted && published && rendered) break
                    delay(50)
                }
            }
        }

        suspend fun selectFixtureCandidate(item: TriggerCase) {
            harness.runApi {
                check(selectedSchemaId() == prefix) { "${item.label} lost the fixture schema" }
                // Password editors intentionally request ASCII. Force the fixture's native
                // translator only for this assertion while retaining the real private editor.
                setRuntimeOption("ascii_mode", false)
                var expectedOrder: List<String>? = null
                repeat(8) {
                    clearComposition()
                    check(processKey(item.code.code)) { "${item.label} fixture key was not processed" }
                    val candidates = getCandidates(0, 10).map { it.text }
                    check(candidates.count { it == item.candidate } == 1) {
                        "${item.label} fixture candidate is ambiguous"
                    }
                    expectedOrder?.let { expected ->
                        check(candidates == expected) {
                            "${item.label} candidate text/order changed: $expected -> $candidates"
                        }
                    } ?: run { expectedOrder = candidates }
                    check(selectCandidate(candidates.indexOf(item.candidate), false)) {
                        "${item.label} fixture candidate was not selected"
                    }
                    check(getRawInput().isEmpty()) { "${item.label} fixture input did not commit" }
                    check(Rime.getRimeCommitHistorySizeForTesting() == 0) {
                        "${item.label} entered native commit history"
                    }
                }
            }
        }

        suspend fun activate(item: TriggerCase): EditText = when (item.kind) {
            TriggerKind.PASSWORD -> {
                main {
                    harness.target.hint = "t07-password"
                    harness.target.inputType =
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    harness.target.imeOptions = EditorInfo.IME_ACTION_DONE
                    harness.target.setText("")
                }
                harness.focus(harness.target)
                harness.target
            }

            TriggerKind.APP_REQUESTED -> {
                main {
                    harness.target.hint = "t07-no-personalized-learning"
                    harness.target.inputType = InputType.TYPE_CLASS_TEXT
                    harness.target.imeOptions =
                        EditorInfo.IME_ACTION_DONE or EditorPolicy.NO_PERSONALIZED_LEARNING_FLAG
                    harness.target.setText("")
                }
                harness.focus(harness.target)
                harness.target
            }

            TriggerKind.MANUAL -> {
                main {
                    harness.chat.setText("")
                    harness.chat.requestFocus()
                }
                harness.focus(harness.chat)
                main { manualPrivacy.setValue(true) }
                harness.chat
            }
        }

        suspend fun leavePrivate(item: TriggerCase) {
            when (item.kind) {
                TriggerKind.MANUAL -> main { manualPrivacy.setValue(false) }
                else -> harness.focus(harness.chat)
            }
            awaitMain("${item.label} restores the ordinary service policy") {
                !harness.service.isPrivacyModeActive &&
                    !PrivacyModeState.active &&
                    harness.speechAllowed()
            }
            harness.runApi {
                check(!Rime.isRimePrivacyGuardEnabledForTesting()) {
                    "${item.label} left the native guard enabled"
                }
                check(selectSchema(schemaBeforeFixture)) {
                    "Could not close the ${item.label} fixture schema"
                }
                clearComposition()
            }
            main { ClipboardHelper.onPrimaryClipChanged() }
            delay(750)
            check(dao.find(item.clipboard) == null) {
                "A delayed ${item.label} clipboard callback reached Room"
            }
            check(ClipboardHelper.lastBean?.text != item.clipboard) {
                "A delayed ${item.label} clipboard callback reached lastBean"
            }
        }

        try {
            check(symbolFile.canonicalFile == expectedSymbolFile.canonicalFile) {
                "Real-trigger symbol history resolved to an unexpected storage root"
            }
            main {
                manualPrivacy.setValue(false)
                clipboardPrefs.clipboardListening.setValue(true)
                clipboardPrefs.clipboardSuggestion.setValue(true)
                clipboardPrefs.clipboardSuggestionTimeout.setValue(100)
                ClipboardHelper.clearTransientSuggestion()
            }
            symbolHistory.load()
            val symbolBaseline = symbolFile.readBytes()
            val symbolBaselineEntries = symbolHistory.toOrderedList()
            writeFixture(prefix, cases, ordinaryCode, ordinaryCandidate)
            harness.runApi {
                schemaBeforeFixture = selectedSchemaId()
                check(schemaBeforeFixture.isNotBlank()) { "No schema before the real-trigger fixture" }
                check(
                    Rime.deployRimeSchemaFile(
                        File(DataManager.userDataDir, "$prefix.schema.yaml").path,
                    ),
                ) { "Real-trigger privacy schema deployment failed" }
                check(selectSchema(prefix)) { "Could not select the real-trigger fixture" }
                setRuntimeOption("ascii_mode", false)
                clearComposition()
                check(selectSchema(schemaBeforeFixture)) { "Could not close the real-trigger baseline" }
            }
            val userDbBaseline = PrivacyModeProbe.userDbSnapshot(prefix)
            Log.i(LOG_TAG, logStart)
            logStarted = true

            cases.forEach { item ->
                harness.runApi {
                    check(selectSchema(prefix)) { "Could not select the ${item.label} fixture" }
                    setRuntimeOption("ascii_mode", false)
                    clearComposition()
                }
                publishExistingSuggestion(item)
                val activeEditor = activate(item)
                awaitMain("${item.label} reaches the real policy and hides the existing clipboard suggestion") {
                    harness.service.isPrivacyModeActive &&
                        PrivacyModeState.active &&
                        !harness.speechAllowed() &&
                        ClipboardHelper.lastBean == null &&
                        !harness.inputTextContains(item.existingSuggestion)
                }
                check(dao.find(item.existingSuggestion) != null) {
                    "${item.label} deleted the pre-existing ordinary clipboard history row"
                }
                harness.runApi {
                    check(Rime.isRimePrivacyGuardEnabledForTesting()) {
                        "${item.label} did not reach the native guard"
                    }
                }
                selectFixtureCandidate(item)
                awaitMain("${item.label} commits its unique virtual secret") {
                    activeEditor.text.toString().contains(item.candidate)
                }
                check(main { (0..4).all { harness.service.getActiveText(it).isEmpty() } }) {
                    "${item.label} exposed private editor content through getActiveText"
                }
                assertPrivateClipboardRejected(item)

                val symbolBefore = symbolFile.readBytes()
                symbolHistory.insert(item.symbol)
                symbolHistory.save()
                check(item.symbol !in symbolHistory.toOrderedList()) {
                    "${item.label} exposed its private symbol"
                }
                check(symbolFile.readBytes().contentEquals(symbolBefore)) {
                    "${item.label} changed symbol history"
                }
                check(!symbolFile.readText().contains(item.symbol)) {
                    "${item.label} symbol token reached disk"
                }

                leavePrivate(item)
                val afterTrigger = PrivacyModeProbe.userDbSnapshot(prefix)
                check(afterTrigger == userDbBaseline) { "${item.label} changed the fixture user dictionary" }
                check(!afterTrigger.contents.contains(item.candidate)) {
                    "${item.label} candidate reached the fixture user dictionary"
                }
                check(symbolFile.readBytes().contentEquals(symbolBaseline)) {
                    "${item.label} left a symbol-history mutation"
                }
                check(symbolHistory.toOrderedList() == symbolBaselineEntries) {
                    "${item.label} changed the pre-existing symbol-history entries"
                }
                harness.phase(
                    "PASS: ${item.label} rejects userdb, private clipboard Room/lastBean/UI, " +
                        "editor content context, symbol history and speech while retaining ordinary history",
                )
            }

            Log.i(LOG_TAG, logEnd)
            logStarted = false
            val processLogs = PrivacyModeProbe.ownProcessLogs(pid)
            val startIndex = processLogs.indexOf(logStart)
            val endIndex = processLogs.indexOf(logEnd, startIndex + 1)
            check(startIndex >= 0 && endIndex > startIndex) {
                "Real-trigger application log boundaries were not captured"
            }
            val privateLogs = processLogs.substring(startIndex + logStart.length, endIndex)
            val privateTokens = cases.flatMap { listOf(it.candidate, it.clipboard, it.symbol) }
            check(privateTokens.none(privateLogs::contains)) {
                "A real-trigger private token appeared in application logs"
            }
            harness.phase("PASS: all three real trigger windows contain no private token in PID logs")

            main { harness.chat.setText("") }
            harness.focus(harness.chat)
            harness.runApi {
                check(selectSchema(prefix)) { "Could not select the ordinary recovery fixture" }
                setRuntimeOption("ascii_mode", false)
                var expectedOrder: List<String>? = null
                repeat(8) {
                    clearComposition()
                    check(processKey(ordinaryCode.code)) { "Ordinary fixture key was not processed" }
                    val candidates = getCandidates(0, 10).map { it.text }
                    check(candidates.count { it == ordinaryCandidate } == 1) {
                        "Ordinary recovery candidate is ambiguous"
                    }
                    expectedOrder?.let { expected ->
                        check(candidates == expected) {
                            "Ordinary candidate text/order changed: $expected -> $candidates"
                        }
                    } ?: run { expectedOrder = candidates }
                    check(selectCandidate(candidates.indexOf(ordinaryCandidate), false)) {
                        "Ordinary recovery candidate did not commit"
                    }
                    check(getRawInput().isEmpty()) { "Ordinary recovery input did not commit" }
                    // Flush every selected candidate through the same production memory
                    // boundary used by the standalone privacy probe before exporting userdb.
                    processKey(' '.code)
                }
                check(Rime.getRimeCommitHistorySizeForTesting() > 0) {
                    "Ordinary recovery did not populate commit history"
                }
                check(selectSchema(schemaBeforeFixture)) {
                    "Could not close the ordinary recovery fixture"
                }
            }
            awaitMain("ordinary input commits after all private triggers") {
                harness.chat.text.toString().contains(ordinaryCandidate)
            }
            check(main { harness.service.getActiveText(1) == ordinaryCandidate }) {
                "Ordinary committed-text context did not recover"
            }
            val afterOrdinary = PrivacyModeProbe.userDbSnapshot(prefix)
            check(
                afterOrdinary.entries > userDbBaseline.entries &&
                    afterOrdinary.contents.contains(ordinaryCandidate),
            ) { "Ordinary user dictionary learning did not recover" }
            check(cases.none { afterOrdinary.contents.contains(it.candidate) }) {
                "A private candidate appeared after ordinary recovery"
            }

            setClipboard(ordinaryClipboard)
            withTimeout(15_000) {
                while (true) {
                    val persisted = dao.find(ordinaryClipboard) != null
                    val published = ClipboardHelper.lastBean?.text == ordinaryClipboard
                    val rendered = main { harness.inputTextContains(ordinaryClipboard) }
                    if (persisted && published && rendered) break
                    delay(50)
                }
            }
            symbolHistory.insert(ordinarySymbol)
            symbolHistory.save()
            check(symbolFile.readText().contains(ordinarySymbol)) {
                "Ordinary symbol-history persistence did not recover"
            }
            check(!PrivacyModeState.active && harness.speechAllowed()) {
                "Ordinary Java privacy policy did not recover"
            }
            harness.runApi {
                check(!Rime.isRimePrivacyGuardEnabledForTesting()) {
                    "Ordinary native policy did not recover"
                }
            }
            harness.phase(
                "PASS: ordinary userdb learning, clipboard Room/lastBean/UI, symbol history and speech recover",
            )
        } catch (failure: Throwable) {
            primaryFailure = failure
            throw failure
        } finally {
            var cleanupFailure: Throwable? = null
            suspend fun clean(block: suspend () -> Unit) {
                runCatching { block() }.onFailure { failure ->
                    cleanupFailure?.addSuppressed(failure) ?: run { cleanupFailure = failure }
                }
            }

            if (logStarted) {
                runCatching { Log.i(LOG_TAG, logEnd) }
            }
            clean { main { manualPrivacy.setValue(false) } }
            clean { harness.focus(harness.chat) }
            clean {
                if (schemaBeforeFixture.isNotBlank()) {
                    harness.runApi {
                        clearComposition()
                        check(selectSchema(schemaBeforeFixture)) {
                            "Could not restore the schema after real-trigger cleanup"
                        }
                    }
                }
            }
            clean {
                main {
                    harness.chat.setText("")
                    harness.target.setText("")
                }
            }
            // Stop new framework callbacks before deleting the ordinary control row. Then wait
            // for any listener + explicit-callback pair already queued by setClipboard().
            clean { main { clipboardPrefs.clipboardListening.setValue(false) } }
            clean {
                main {
                    if (previousPrimaryClip != null) {
                        clipboard.setPrimaryClip(ClipData(previousPrimaryClip))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        clipboard.clearPrimaryClip()
                    } else {
                        // Android 5.0-8.1 has no public clearPrimaryClip API.
                        clipboard.setPrimaryClip(ClipData.newPlainText("T07 cleanup", ""))
                    }
                }
            }
            clean {
                val ownedClipboard =
                    cases.flatMap { listOf(it.clipboard, it.existingSuggestion) }.plus(ordinaryClipboard)
                withTimeout(15_000) {
                    var quietChecks = 0
                    while (quietChecks < 15) {
                        var deleted = false
                        ownedClipboard.forEach { text ->
                            while (true) {
                                val bean = dao.find(text) ?: break
                                ClipboardHelper.delete(bean.id)
                                deleted = true
                            }
                        }
                        quietChecks = if (deleted) 0 else quietChecks + 1
                        delay(50)
                    }
                }
            }
            clean {
                if (previousSymbolHistory == null) {
                    check(!symbolFile.exists() || symbolFile.delete()) {
                        "Could not remove real-trigger symbol history"
                    }
                } else {
                    symbolFile.writeBytes(previousSymbolHistory)
                }
            }
            clean {
                main {
                    ClipboardHelper.clearTransientSuggestion()
                    ClipboardHelper.lastBean = previousLastBean
                }
            }
            clean { main { clipboardPrefs.clipboardSuggestion.setValue(previousClipboardSuggestion) } }
            clean {
                main {
                    clipboardPrefs.clipboardSuggestionTimeout.setValue(previousClipboardSuggestionTimeout)
                }
            }
            clean { main { clipboardPrefs.clipboardListening.setValue(previousClipboardListening) } }
            clean { main { manualPrivacy.setValue(previousManualPrivacy) } }
            clean { PrivacyModeProbe.removeOwnedFiles(prefix) }
            cleanupFailure?.let { cleanup ->
                primaryFailure?.addSuppressed(cleanup) ?: throw cleanup
            }
        }
    }
}
