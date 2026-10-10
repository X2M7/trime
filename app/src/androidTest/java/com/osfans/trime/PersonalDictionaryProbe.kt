// SPDX-License-Identifier: GPL-3.0-or-later
package com.osfans.trime

import android.app.Activity
import android.app.Instrumentation
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import com.osfans.trime.core.CandidateSnapshot
import com.osfans.trime.core.Rime
import com.osfans.trime.core.RimeConfig
import com.osfans.trime.core.RimeDispatcher
import com.osfans.trime.core.RimeLifecycle
import com.osfans.trime.core.RimeMessage
import com.osfans.trime.daemon.RimeDaemon
import com.osfans.trime.daemon.RimeSession
import com.osfans.trime.data.base.DataManager
import com.osfans.trime.data.prefs.AppPrefs
import com.osfans.trime.data.userdict.UserDictManager
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Properties
import java.util.UUID

/** Real APK engine acceptance in a dedicated emulator; all learned words are owned fixtures. */
object PersonalDictionaryProbe {
    private val phrases = linkedMapOf("茶雲翎" to "cha'yun'ling", "墨羽燈" to "mo'yu'deng")
    private val rankTexts = List(200) { "示例候选${it.toString().padStart(3, '0')}" }

    internal fun writeFixture(prefix: String): List<File> {
        val root = DataManager.userDataDir
        val dictionary = File(root, "$prefix.dict.yaml")
        val rankedDictionary = File(root, "${prefix}_rank.dict.yaml")
        check(dictionary.createNewFile() && rankedDictionary.createNewFile())
        val header = "---\nname: %s\nversion: \"1\"\nsort: by_weight\nuse_preset_vocabulary: false\n...\n"
        dictionary.writeText(header.format(prefix) + "茶\tcha\t100\n雲\tyun\t100\n翎\tling\t100\n墨\tmo\t100\n羽\tyu\t100\n燈\tdeng\t100\n")
        rankedDictionary.writeText(
            header.format("${prefix}_rank") + rankTexts.mapIndexed { index, text -> "$text\ta\t${200 - index}" }.joinToString("\n") +
                "\n刷新后的候选\tb\t100\n",
        )
        return listOf("full", "t9", "rank").map { suffix ->
            val schema = "${prefix}_$suffix"
            val name = if (suffix == "rank") "${prefix}_rank" else prefix
            val translator = if (suffix == "rank") "table_translator" else "script_translator"
            val alphabet = if (suffix == "rank") "abcdefghijklmnopqrstuvwxyz" else "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ987654321"
            val algebra = if (suffix == "t9") {
                "  algebra:\n    - derive/^(.*)$/\\U$1/\n    - xlit/ABCDEFGHIJKLMNOPQRSTUVWXYZ/22233344455566677778889999/\n"
            } else {
                ""
            }
            File(root, "$schema.schema.yaml").apply {
                check(createNewFile())
                writeText(
                    "schema:\n  schema_id: $schema\n  name: T10 $suffix probe\n  version: \"1\"\n" +
                        "switches:\n  - name: ascii_mode\n    reset: 0\n    states: [native, ascii]\n" +
                        "ascii_composer:\n  switch_key:\n    Shift_L: inline_ascii\n    Shift_R: commit_text\n" +
                        "engine:\n  processors: [ascii_composer, speller, selector, navigator, express_editor]\n" +
                        "  segmentors: [ascii_segmentor, abc_segmentor, fallback_segmentor]\n  translators: [$translator]\n" +
                        "speller:\n  alphabet: $alphabet\n  delimiter: \" '\"\n" + algebra +
                        "translator:\n  dictionary: $name\n  prism: $schema\n" +
                        "  enable_user_dict: ${suffix != "rank"}\n  enable_sentence: ${suffix != "rank"}\n  enable_completion: false\n" +
                        "menu:\n  page_size: 5\n",
                )
            }
        }
    }

    private fun code(spelling: String, nineKey: Boolean): String = if (!nineKey) {
        spelling
    } else {
        spelling.filter { it != '\'' }.map { "22233344455566677778889999"[it - 'a'] }.joinToString("")
    }

    private fun records(contents: String): Map<Pair<String, String>, Int> = contents.lineSequence()
        .filter { it.isNotBlank() && !it.startsWith('#') }.associate { line ->
            val fields = line.split('\t')
            check(fields.size == 3) { "Malformed native export" }
            (fields[0] to fields[1].trim()) to fields[2].toInt()
        }

    fun run(instrumentation: Instrumentation) {
        val result = Bundle()
        val prefix = "__t10_${UUID.randomUUID().toString().replace("-", "")}"
        val sessionName = "${PersonalDictionaryProbe::class.java.name}-$prefix"
        val firstEditor = SystemClock.elapsedRealtimeNanos().coerceAtLeast(1L)
        val privateEditor = firstEditor + 1
        val secondEditor = firstEditor + 2
        val userConfig = File(DataManager.userDataDir, "user.yaml")
        val pinFile = File(DataManager.userDataDir, ".trime-candidate-pins.json")
        val pinFiles = listOf(pinFile, File(pinFile.path + ".bak"), File(pinFile.path + ".new"))
        val originalPins = mutableMapOf<File, ByteArray?>()
        var originalUserConfig: ByteArray? = null
        var stateCaptured = false
        var fixtureStarted = false
        var session: RimeSession? = null
        var originalSchema: String? = null
        var originalAscii = false
        var originalPaging = false
        var originalTips = false
        var tipsCaptured = false
        var passed = false
        var failure: Throwable? = null
        var checks = 0
        var nativeRestarts = 0
        val commits = mutableListOf<String>()

        fun phase(description: String) {
            checks++
            android.util.Log.i("TrimeRuntimeAudit", "T10: $description")
            instrumentation.sendStatus(0, Bundle().apply { putString("stream", "PASS: T10 $description\n") })
        }

        suspend fun stopped() {
            val state = withTimeout(120_000) {
                RimeDaemon.engineState.first { it == RimeLifecycle.State.STOPPED || it == RimeLifecycle.State.FAILED }
            }
            check(state == RimeLifecycle.State.STOPPED) { "Native engine did not finalize cleanly" }
            check(RimeDaemon.getFirstSessionOrNull() == null)
        }

        fun start() {
            check(session == null)
            instrumentation.runOnMainSync { session = RimeDaemon.createSession(sessionName) }
        }

        fun dispatcher(): RimeDispatcher {
            val realRime = RimeDaemon::class.java.getDeclaredMethod("getRealRime").apply { isAccessible = true }.invoke(RimeDaemon) as Rime
            return Rime::class.java.getDeclaredField("dispatcher").apply { isAccessible = true }.get(realRime) as RimeDispatcher
        }

        try {
            check(Build.HARDWARE in setOf("ranchu", "goldfish")) { "T10 probe only runs on an emulator" }
            check(File(instrumentation.targetContext.getExternalFilesDir(null), "runtime-audit-dedicated").isFile)
            check(RimeDaemon.getFirstSessionOrNull() == null) { "T10 probe requires an idle daemon" }
            runBlocking { stopped() }
            originalUserConfig = userConfig.takeIf(File::exists)?.readBytes()
            pinFiles.forEach { originalPins[it] = it.takeIf(File::exists)?.readBytes() }
            stateCaptured = true
            instrumentation.runOnMainSync {
                val tips = AppPrefs.defaultInstance().general.asciiSwitchTips
                originalTips = tips.getValue()
                tipsCaptured = true
                tips.setValue(false)
            }
            start()
            runBlocking {
                withTimeout(300_000) {
                    checkNotNull(session).runOnReady {
                        originalSchema = selectedSchemaId()
                        originalAscii = getRuntimeOption("ascii_mode")
                        originalPaging = Rime::class.java.getDeclaredField("pagingMode").apply { isAccessible = true }.getBoolean(
                            RimeDaemon::class.java.getDeclaredMethod("getRealRime").apply { isAccessible = true }.invoke(RimeDaemon),
                        )
                    }
                    dispatcher().runConfined {
                        fixtureStarted = true
                        writeFixture(prefix).forEach { check(Rime.deployRimeSchemaFile(it.absolutePath)) { "Owned T10 schema deployment failed" } }
                        // The shipped schemas derive the same user DB name from luna_pinyin.
                        for (schema in listOf("luna_pinyin", "luna_pinyin_simp", "luna_pinyin_t9")) {
                            RimeConfig.openSchema(schema).use { config ->
                                check(config.getString("translator/dictionary") == "luna_pinyin")
                                check(config.getString("translator/user_dict").let { it == null || it == "luna_pinyin" })
                            }
                        }
                    }
                    phase("shipped full-pinyin and nine-key schemas identify shared luna_pinyin user dictionary; custom_phrase remains separate")
                    val interactionChecks = CandidateInteractionAssertions.verify(instrumentation)
                    phase("$interactionChecks actual candidate gesture view checks reject rebound rows and preserve accessibility clicks; no window drawing claim")
                    val pinStorageChecks = CandidatePinStorageAssertions.verify(instrumentation, dispatcher())
                    phase("$pinStorageChecks real AtomicFile and confined JNI checks preserve failed/corrupt storage, reload 512 pins and release local references before restoring normal rules")

                    suspend fun export(): Map<Pair<String, String>, Int> = checkNotNull(session).runOnReady {
                        maintainUserDictionaries {
                            val stream = ByteArrayOutputStream()
                            UserDictManager.exportUserDict(stream, prefix, "$prefix-export.txt").getOrThrow()
                            records(stream.toString("UTF-8"))
                        }
                    }

                    suspend fun query(schema: String, spelling: String): CandidateSnapshot = checkNotNull(session).runOnReady {
                        check(selectSchema(schema))
                        setRuntimeOption("ascii_mode", false)
                        setCandidatePagingMode(false)
                        clearComposition()
                        spelling.forEach { check(processKey(it.code)) { "Unhandled fixture key" } }
                        check(getRawInput() == spelling)
                        candidateSnapshotCached.also { check(it.isValid) }
                    }

                    suspend fun withObserver(block: suspend () -> Unit) {
                        val rime = checkNotNull(session)
                        val observer = launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
                            rime.run { messageFlow }.collect { message ->
                                if (message is RimeMessage.CommitTextMessage && !message.data.text.isNullOrEmpty()) {
                                    synchronized(commits) { commits.add(checkNotNull(message.data.text)) }
                                }
                            }
                        }
                        try {
                            block()
                        } finally {
                            observer.cancelAndJoin()
                        }
                    }

                    suspend fun commit(text: String, operation: suspend () -> Unit) {
                        val previous = synchronized(commits) { commits.size }
                        operation()
                        withTimeout(10_000) {
                            while (synchronized(commits) { commits.size } == previous) delay(10)
                        }
                        check(synchronized(commits) { commits.drop(previous) } == listOf(text)) { "Native selection committed wrong or duplicate text" }
                    }

                    suspend fun select(text: String, snapshot: CandidateSnapshot) = commit(text) {
                        checkNotNull(session).runOnReady {
                            val rows = getCandidates(0, 80, snapshot)
                            val index = rows.indexOfFirst { it.text == text }
                            check(index >= 0) { "Fictional phrase is absent from bounded native candidates" }
                            check(selectCandidate(index, true, snapshot))
                        }
                    }

                    checkNotNull(session).runOnReady {
                        check(selectSchema("${prefix}_full"))
                        beginEditor(firstEditor, nullInputType = false, privacyMode = false)
                    }
                    val cold = export()
                    check(phrases.keys.none { text -> cold.keys.any { it.first == text } })
                    withObserver {
                        for ((text, spelling) in phrases) {
                            repeat(3) {
                                select(text, query("${prefix}_full", spelling))
                                checkNotNull(session).runOnReady { processKey(0xff1b) }
                            }
                        }
                    }
                    val learned = export()
                    phrases.forEach { (text, spelling) -> check((learned[text to spelling.replace('\'', ' ')] ?: 0) >= 3) }
                    phase("six exact fictional phrase commits become positive persistent native user dictionary records")

                    checkNotNull(session).runOnReady { finishOwnedEditors(longArrayOf(firstEditor)) }
                    RimeDaemon.destroySession(sessionName)
                    session = null
                    stopped()
                    nativeRestarts++
                    start()
                    checkNotNull(session).runOnReady { beginEditor(secondEditor, nullInputType = false, privacyMode = false) }
                    for (nineKey in listOf(false, true)) {
                        for ((text, spelling) in phrases) {
                            val snapshot = query("${prefix}_${if (nineKey) "t9" else "full"}", code(spelling, nineKey))
                            checkNotNull(session).runOnReady { check(getCandidates(0, 80, snapshot).first().text == text) }
                        }
                    }
                    check(export() == learned) { "Read-only restart queries changed learning" }
                    phase("actual native finalize and recreate retains learned phrases; both full-pinyin and nine-key schemas reuse one owned dictionary")

                    val first = phrases.entries.first()
                    val forgetSnapshot = query("${prefix}_full", first.value)
                    checkNotNull(session).runOnReady {
                        val index = getCandidates(0, 80, forgetSnapshot).indexOfFirst { it.text == first.key }
                        check(deleteCandidate(index, true, forgetSnapshot))
                    }
                    val forgotten = export()
                    check((first.key to first.value.replace('\'', ' ')) !in forgotten)
                    val other = phrases.entries.last()
                    check(forgotten[other.key to other.value.replace('\'', ' ')] == learned[other.key to other.value.replace('\'', ' ')])
                    withObserver { select(first.key, query("${prefix}_full", first.value)) }
                    val relearned = export()
                    check((relearned[first.key to first.value.replace('\'', ' ')] ?: 0) > 0)
                    phase("forget removes the positive learned record without changing another phrase; underlying sentence can return and relearn")

                    checkNotNull(session).runOnReady { beginEditor(privateEditor, nullInputType = false, privacyMode = true) }
                    withObserver { select("茶羽翎", query("${prefix}_full", "cha'yu'ling")) }
                    check(export() == relearned) { "Private fixture commit changed persistent learning" }
                    checkNotNull(session).runOnReady { beginEditor(secondEditor, nullInputType = false, privacyMode = false) }
                    phase("private fictional phrase commits to editor while learning records remain unchanged; ordinary mode resumes")

                    val baseline = rankTexts
                    val rankSchema = "${prefix}_rank"
                    val stale = query(rankSchema, "a")
                    query(rankSchema, "b")
                    checkNotNull(session).runOnReady {
                        val before = candidateSnapshotCached
                        check(!pinCandidate(120, true, stale) && !resetCandidateRanking(stale))
                        check(getRawInput() == "b" && candidateSnapshotCached == before)
                    }
                    var current = query(rankSchema, "a")
                    checkNotNull(session).runOnReady {
                        check(getCandidates(0, 144, current).map { it.text } == baseline.take(144))
                        check(!pinCandidate(144, true, current)) { "Out-of-window pin was accepted" }
                        check(pinCandidate(120, true, current))
                    }
                    val expected = listOf(rankTexts[120]) + rankTexts.take(120) + rankTexts.drop(121)
                    current = checkNotNull(session).runOnReady { candidateSnapshotCached }
                    checkNotNull(session).runOnReady {
                        check(getCandidates(0, 144, current).map { it.text } + getCandidates(144, 144, current).map { it.text } == expected)
                    }
                    phase("stale pin/reset do not alter refreshed input; native pin window is bounded at 144 and preserves the entire 200-row lazy tail")

                    checkNotNull(session).runOnReady { finishOwnedEditors(longArrayOf(secondEditor)) }
                    RimeDaemon.destroySession(sessionName)
                    session = null
                    stopped()
                    nativeRestarts++
                    start()
                    checkNotNull(session).runOnReady { beginEditor(secondEditor, nullInputType = false, privacyMode = false) }
                    current = query(rankSchema, "a")
                    checkNotNull(session).runOnReady {
                        check(getCandidates(0, 144, current).map { it.text } == expected.take(144))
                    }
                    withObserver {
                        select(rankTexts[120], current)
                        query(rankSchema, "a")
                        commit(rankTexts[120]) { checkNotNull(session).runOnReady { check(processKey(' '.code)) } }
                        query(rankSchema, "a")
                        commit(rankTexts[120]) { checkNotNull(session).runOnReady { check(processKey('1'.code)) } }
                        query(rankSchema, "a")
                        commit(expected[1]) { checkNotNull(session).runOnReady { check(processKey('2'.code)) } }
                        current = query(rankSchema, "a")
                        checkNotNull(session).runOnReady { check(changeCandidatePage(false, current)) }
                        commit(expected[6]) { checkNotNull(session).runOnReady { check(processKey('2'.code)) } }
                        checkNotNull(session).runOnReady { clearComposition() }
                        commit(rankTexts[120]) { checkNotNull(session).runOnReady { check(simulateKeySequence("a{space}")) } }
                        checkNotNull(session).runOnReady { check(getRawInput().isEmpty()) }
                    }
                    current = query(rankSchema, "a")
                    checkNotNull(session).runOnReady {
                        check(resetCandidateRanking(current))
                        val restored = candidateSnapshotCached
                        check(getCandidates(0, 144, restored).map { it.text } == baseline.take(144))
                    }
                    phase("pin survives native restart; direct selection, Space, numeric selection, paged number and macro each commit the displayed text once; reset restores ordering")

                    current = query(rankSchema, "a")
                    checkNotNull(session).runOnReady { check(pinCandidate(120, true, current)) }
                    val pinnedBytes = pinFile.readBytes()
                    checkNotNull(session).runOnReady { beginEditor(privateEditor, nullInputType = false, privacyMode = true) }
                    current = query(rankSchema, "a")
                    checkNotNull(session).runOnReady {
                        check(getCandidates(0, 144, current).map { it.text } == baseline.take(144))
                        check(!pinCandidate(121, true, current) && !resetCandidateRanking(current))
                    }
                    check(pinFile.readBytes().contentEquals(pinnedBytes))
                    checkNotNull(session).runOnReady { beginEditor(secondEditor, nullInputType = false, privacyMode = false) }
                    current = query(rankSchema, "a")
                    checkNotNull(session).runOnReady {
                        check(getCandidates(0, 144, current).first().text == rankTexts[120])
                        check(resetCandidateRanking(current))
                    }
                    phase("privacy excludes saved candidate pins and rejects management writes; ordinary editor restores saved ranking")

                    checkNotNull(session).runOnReady {
                        maintainUserDictionaries { UserDictManager.backupForRecovery(prefix).getOrThrow() }
                    }
                    val beforeImport = export()
                    val added = "茶羽翎\tcha yu ling\t9\n"
                    checkNotNull(session).runOnReady {
                        maintainUserDictionaries {
                            check(UserDictManager.importUserDict(ByteArrayInputStream(added.toByteArray()), prefix, "$prefix-import.txt").getOrThrow() == 1)
                        }
                    }
                    check(export()["茶羽翎" to "cha yu ling"] == 9)
                    checkNotNull(session).runOnReady {
                        maintainUserDictionaries { UserDictManager.recoverUserDict(prefix).getOrThrow() }
                    }
                    check(export() == beforeImport) { "Exact recovery retained post-backup records" }
                    checkNotNull(session).runOnReady {
                        maintainUserDictionaries {
                            check(UserDictManager.restoreUserDict(ByteArrayInputStream("invalid snapshot".toByteArray()), "$prefix-invalid.userdb.txt").isFailure)
                        }
                    }
                    check(export() == beforeImport) { "Rejected malformed restore changed learned records" }
                    phase("real dictionary import works; exact file recovery removes added records, malformed migration is rejected without learning loss")
                    val storageChecks = UserDictionaryStorageAssertions.verify(checkNotNull(session), dispatcher(), prefix)
                    phase("$storageChecks actual phrase source, portable snapshot and pending journal storage checks; original phrase source and backups restored")
                }
            }
            passed = true
        } catch (error: Throwable) {
            failure = error
        } finally {
            fun clean(block: () -> Unit) {
                runCatching(block).onFailure { error ->
                    failure?.addSuppressed(error) ?: run { failure = error }
                    passed = false
                }
            }
            clean {
                runBlocking {
                    withContext(NonCancellable) {
                        withTimeout(120_000) {
                            session?.runOnReady {
                                finishOwnedEditors(longArrayOf(firstEditor, privateEditor, secondEditor))
                                originalSchema?.let { check(selectSchema(it)) }
                                setRuntimeOption("ascii_mode", originalAscii)
                                setCandidatePagingMode(originalPaging)
                                clearComposition()
                            }
                        }
                    }
                }
            }
            var nativeStopped = false
            clean {
                if (session != null) {
                    RimeDaemon.destroySession(sessionName)
                    session = null
                }
                runBlocking { stopped() }
                nativeStopped = true
            }
            if (nativeStopped) {
                if (fixtureStarted) clean { PrivacyModeProbe.removeOwnedFiles(prefix) }
                if (stateCaptured) {
                    clean {
                        originalUserConfig?.let(userConfig::writeBytes) ?: check(!userConfig.exists() || userConfig.delete())
                        originalPins.forEach { (file, bytes) -> bytes?.let(file::writeBytes) ?: check(!file.exists() || file.delete()) }
                    }
                }
                if (fixtureStarted) {
                    clean {
                        val backups = File(instrumentation.targetContext.filesDir, "userdict-backups")
                        backups.listFiles()?.filter(File::isDirectory)?.forEach { directory ->
                            val metadata = File(directory, "metadata")
                            if (metadata.isFile) {
                                val properties = Properties().apply { metadata.inputStream().use(::load) }
                                if (properties.getProperty("target") == "$prefix.userdb") check(directory.deleteRecursively())
                            }
                        }
                    }
                }
            }
            if (tipsCaptured) clean { instrumentation.runOnMainSync { AppPrefs.defaultInstance().general.asciiSwitchTips.setValue(originalTips) } }
        }
        result.putBoolean("passed", passed)
        result.putInt("checks", checks)
        result.putInt("native_restarts", nativeRestarts)
        result.putInt("fixture_candidates", rankTexts.size)
        result.putString("stream", failure?.let { "FAIL: ${it.stackTraceToString()}\n" } ?: "PASS: T10 $checks learning/ranking/recovery checks; $nativeRestarts native finalize/recreate cycles; no APK upgrade or UI performance claim\n")
        instrumentation.finish(if (passed) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
    }
}
