/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime

import android.app.Activity
import android.app.Instrumentation
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.osfans.trime.core.RimeConfig
import com.osfans.trime.core.RimeLifecycle
import com.osfans.trime.core.RimeSchema
import com.osfans.trime.core.T9Action
import com.osfans.trime.daemon.RimeDaemon
import com.osfans.trime.daemon.RimeSession
import com.osfans.trime.data.theme.ColorManager
import com.osfans.trime.data.theme.ThemeManager
import com.osfans.trime.ime.core.TrimeInputMethodService
import com.osfans.trime.ime.keyboard.KeyAction
import com.osfans.trime.ime.keyboard.Keyboard
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** Emulator-only integration probe; no user dictionary is cleared or imported. */
class StartupResponsivenessInstrumentation : Instrumentation() {
    private var safOnly = false
    private var safTree: String? = null
    private var safRevoke = false
    private var clipOnly = false
    private var clipSaveOnly = false
    private var t02Only = false
    private var t03Only = false
    private var t03GeometryOnly = false
    private var t05Only = false
    private var setupOnly = false
    private var shutdownOnly = false
    private var failureOnly = false
    private var feedbackOnly = false
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        safOnly = arguments?.getString("saf") == "true"
        safTree = arguments?.getString("safTree")
        safRevoke = arguments?.getString("safRevoke") == "true"
        clipOnly = arguments?.getString("clip") == "true"
        clipSaveOnly = arguments?.getString("clipSave") == "true"
        t02Only = arguments?.getString("t02") == "true"
        t03Only = arguments?.getString("t03") == "true"
        t03GeometryOnly = arguments?.getString("t03GeometryOnly") == "true" || arguments?.getString("t05GeometryOnly") == "true"
        t05Only = arguments?.getString("t05") == "true"
        setupOnly = arguments?.getString("setup") == "true"
        shutdownOnly = arguments?.getString("shutdown") == "true"
        failureOnly = arguments?.getString("startupFailure") == "true"
        feedbackOnly = arguments?.getString("feedback") == "true"
        start()
    }

    override fun onStart() {
        sendStatus(0, Bundle().apply { putInt("audit_pid", android.os.Process.myPid()) })
        // start() can launch this thread before Application.onCreate() returns.
        // Never initialize production preferences from the test to hide that race.
        val applicationWait = SystemClock.uptimeMillis()
        waitForIdleSync()
        sendStatus(0, Bundle().apply { putLong("application_ready_wait_ms", SystemClock.uptimeMillis() - applicationWait) })
        if (feedbackOnly) {
            InputFeedbackProbe.run(this)
            return
        }
        if (failureOnly) {
            StartupFailureProbe.run(this)
            return
        }
        if (setupOnly) {
            SetupResponsivenessProbe.run(this)
            return
        }
        if (t02Only || t03Only || t05Only) {
            T9EditingProbe.run(this, t03Only, t03GeometryOnly, t05Only)
            return
        }
        if (clipOnly) {
            ClipEditorProbe.run(this)
            return
        }
        if (clipSaveOnly) {
            ClipSaveProbe.run(this)
            return
        }
        if (safOnly) {
            SafCompatibilityProbe.run(this, safTree, safRevoke)
            return
        }
        val result = Bundle()
        val handler = Handler(Looper.getMainLooper())
        val ticks = AtomicInteger()
        val maxGap = AtomicLong()
        var previous = SystemClock.uptimeMillis()
        val heartbeat = object : Runnable {
            override fun run() {
                val now = SystemClock.uptimeMillis()
                // Only the main looper writes; keep the probe executable on API 21.
                maxGap.set(maxOf(maxGap.get(), now - previous))
                previous = now
                ticks.incrementAndGet()
                handler.postDelayed(this, 50)
            }
        }
        var session: RimeSession? = null
        var success = false
        fun phase(name: String) {
            android.util.Log.i("TrimeRuntimeAudit", name)
            sendStatus(0, Bundle().apply { putString("stream", "$name: main ticks=${ticks.get()}, maximum gap=${maxGap.get()} ms\n") })
        }
        try {
            check(Build.HARDWARE in setOf("ranchu", "goldfish")) { "This probe only runs on an emulator" }
            handler.post(heartbeat)
            if (shutdownOnly) {
                runBlocking {
                    withTimeout(300_000) {
                        check(RimeDaemon.getFirstSessionOrNull() == null) { "Shutdown probe requires no other clients" }
                        val first = withContext(Dispatchers.Main) { RimeDaemon.createSession("abandoned-startup") }
                        val stopped = first.lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) { awaitCancellation() }
                        RimeDaemon.engineState.first { it == RimeLifecycle.State.STARTING }
                        withContext(Dispatchers.Main) { RimeDaemon.destroySession("abandoned-startup") }
                        stopped.join()
                        check(stopped.isCancelled) { "Removed client's jobs were not cancelled" }
                        RimeDaemon.engineState.first { it == RimeLifecycle.State.STOPPED }
                        phase("Abandoned startup finalized")
                        val retained = withContext(Dispatchers.Main) {
                            val removed = RimeDaemon.createSession("replaced-startup")
                            val stale = removed.lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) { awaitCancellation() }
                            RimeDaemon.destroySession("replaced-startup")
                            check(stale.isCancelled)
                            val next = RimeDaemon.createSession("retained-startup")
                            check(runCatching { removed.run { isReady } }.isFailure)
                            next to next.lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) { awaitCancellation() }
                        }
                        try {
                            retained.first.runOnReady { check(isReady) }
                            delay(200)
                            check(!retained.second.isCancelled) { "A new client was shut down by the abandoned startup" }
                        } finally {
                            withContext(Dispatchers.Default) { RimeDaemon.destroySession("retained-startup") }
                        }
                        retained.second.join()
                        check(retained.second.isCancelled)
                        RimeDaemon.engineState.first { it == RimeLifecycle.State.STOPPED }
                        phase("Reattached client retained until its own shutdown")
                        repeat(5) { cycle ->
                            val previous = withContext(Dispatchers.Main) { RimeDaemon.createSession("rapid-reconnect") }
                            previous.runOnReady { check(isReady) }
                            val next = withContext(Dispatchers.Main) {
                                RimeDaemon.destroySession("rapid-reconnect")
                                RimeDaemon.createSession("rapid-reconnect")
                            }
                            check(next !== previous)
                            check(runCatching { previous.run { isReady } }.isFailure)
                            next.runOnReady { check(isReady) }
                            val restartStarted = async(start = CoroutineStart.UNDISPATCHED) {
                                RimeDaemon.engineState.first { it == RimeLifecycle.State.STOPPING || it == RimeLifecycle.State.STOPPED }
                            }
                            withContext(Dispatchers.Main) { RimeDaemon.restartRime() }
                            restartStarted.await()
                            next.runOnReady { check(isReady) }
                            withContext(Dispatchers.Main) { RimeDaemon.destroySession("rapid-reconnect") }
                            RimeDaemon.engineState.first { it == RimeLifecycle.State.STOPPED }
                            phase("Ready/reconnect/restart cycle $cycle completed")
                        }
                    }
                }
                check(maxGap.get() < 4000)
                success = true
                result.putString("stream", "PASS: abandoned startup cleanup and reattach; main ticks=${ticks.get()}, maximum gap=${maxGap.get()} ms\n")
                return
            }
            runOnMainSync {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    check(TrimeInputMethodService().onCreateInlineSuggestionsRequest(Bundle()) == null)
                }
            }
            runOnMainSync { session = RimeDaemon.createSession(javaClass.name) }
            val rime = checkNotNull(session)
            runBlocking {
                withTimeout(300_000) {
                    rime.runOnReady { }
                    phase("Engine ready")
                    withContext(Dispatchers.Main) { ThemeManager.init(targetContext.resources.configuration) }
                    val selected = ThemeManager.prefs.selectedTheme.getValue()
                    val originalScope = ColorManager.currentScope()
                    repeat(3) {
                        ThemeManager.selectTheme(selected)
                        check(ColorManager.currentScope() === originalScope) { "Equal-theme reload detached the view scope" }
                    }
                    try {
                        phase("Inject missing theme")
                        check(ThemeManager.selectTheme("__missing_anr_test__") == "trime")
                    } finally {
                        ThemeManager.selectTheme(selected)
                        phase("Missing theme restored")
                    }
                    phase("Theme reload and fallback complete")
                    rime.runOnReady {
                        JniInteropRegression.verify()
                        RimeConfig.openConfig("default").use { expected ->
                            check(RimeSchema(".default").alphabet == (expected.getString("speller/alphabet") ?: ""))
                            expected.close()
                            expected.close()
                            check(expected.getString("speller/alphabet") == null)
                        }
                        val presets = ThemeManager.activeTheme.presetKeyboards
                        if (selected == "trime") {
                            val base = checkNotNull(presets["default"])
                            val letter = checkNotNull(presets["letter"])
                            check(letter == base.copy(asciiMode = true, resetAsciiMode = true, lock = false))
                            check(presets["scj6"] == presets["cangjie5"])
                        }
                        val originalSchema = selectedSchemaId()
                        val originalAscii = getRuntimeOption("ascii_mode")
                        try {
                            check(selectSchema("luna_pinyin_t9"))
                            for (enabled in listOf(true, false)) {
                                setRuntimeOption("ascii_mode", enabled)
                                check(getRuntimeOptionCached("ascii_mode") == enabled)
                            }
                            clearComposition()
                            "64426".forEach { check(processKey(it.code)) }
                            check(t9Cached.choices.any { it.spelling == "mi" })
                            val ni = t9Cached.choices.first { it.spelling == "ni" && !it.completion }
                            check(t9Action(t9Cached.revision, T9Action.Lock, ni.start, ni.end, ni.spelling))
                            val locked = t9Cached
                            check(locked.input == "64426" && locked.segments.any { it.locked && it.spelling == "ni" })
                            withContext(Dispatchers.Main) { ThemeMergeRegression.verify(targetContext, locked) }
                            check(t9Cached.revision == locked.revision && t9Cached.input == "64426")
                            clearComposition()
                            check(selectSchema("luna_pinyin"))
                            check(getRuntimeOptionCached("ascii_mode") == getRuntimeOption("ascii_mode"))
                        } finally {
                            clearComposition()
                            selectSchema(originalSchema)
                            setRuntimeOption("ascii_mode", originalAscii)
                        }
                    }
                    phase("Schema, option cache and portrait/landscape T9 recolor verified")
                    rime.runOnReady { SchemaPickerRegression.verify(targetContext, this) }
                    phase("F4 schema picker and empty list verified")
                    val queries = async(Dispatchers.Default) {
                        repeat(20) { rime.runOnReady { selectedSchemata() } }
                    }
                    withContext(Dispatchers.Main) {
                        val theme = ThemeManager.activeTheme
                        val keyboard = Keyboard(targetContext, theme, 720, theme.presetKeyboards["luna_pinyin_t9"])
                        val mode = KeyAction("Mode_switch")
                        repeat(100) { mode.getLabel(keyboard) }
                    }
                    queries.await()
                    phase("Busy queries and toggle drawing complete")
                    delay(200)
                }
            }
            check(ticks.get() >= 4) { "Main looper did not make progress" }
            check(maxGap.get() < 4000) { "Main looper stalled for ${maxGap.get()} ms" }
            success = true
            result.putString("stream", "PASS: startup, theme reload/fallback, schema/option cache, busy queries and toggle drawing; main ticks=${ticks.get()}, maximum gap=${maxGap.get()} ms\n")
        } catch (e: Throwable) {
            result.putString("stream", "FAIL: ${e.stackTraceToString()}\n")
        } finally {
            handler.removeCallbacks(heartbeat)
            if (session != null) RimeDaemon.destroySession(javaClass.name)
            if (shutdownOnly) {
                listOf("abandoned-startup", "replaced-startup", "retained-startup", "rapid-reconnect").forEach {
                    RimeDaemon.destroySession(it)
                }
            }
            result.putLong("max_main_gap_ms", maxGap.get())
            result.putInt("main_ticks", ticks.get())
            result.putBoolean("passed", success)
            finish(if (success) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
        }
    }
}
