/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime

import android.app.Activity
import android.app.Instrumentation
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.osfans.trime.daemon.RimeDaemon
import com.osfans.trime.daemon.RimeSession
import com.osfans.trime.data.theme.ThemeManager
import com.osfans.trime.ime.keyboard.KeyAction
import com.osfans.trime.ime.keyboard.Keyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** Emulator-only integration probe; no user dictionary is cleared or imported. */
class StartupResponsivenessInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        val result = Bundle()
        val handler = Handler(Looper.getMainLooper())
        val ticks = AtomicInteger()
        val maxGap = AtomicLong()
        var previous = SystemClock.uptimeMillis()
        val heartbeat = object : Runnable {
            override fun run() {
                val now = SystemClock.uptimeMillis()
                maxGap.updateAndGet { maxOf(it, now - previous) }
                previous = now
                ticks.incrementAndGet()
                handler.postDelayed(this, 50)
            }
        }
        var session: RimeSession? = null
        var success = false
        fun phase(name: String) {
            sendStatus(0, Bundle().apply { putString("stream", "$name: main ticks=${ticks.get()}, maximum gap=${maxGap.get()} ms\n") })
        }
        try {
            check(Build.HARDWARE in setOf("ranchu", "goldfish")) { "This probe only runs on an emulator" }
            handler.post(heartbeat)
            runOnMainSync { session = RimeDaemon.createSession(javaClass.name) }
            val rime = checkNotNull(session)
            runBlocking {
                withTimeout(300_000) {
                    rime.runOnReady { }
                    phase("Engine ready")
                    withContext(Dispatchers.Main) { ThemeManager.init(targetContext.resources.configuration) }
                    val selected = ThemeManager.prefs.selectedTheme.getValue()
                    repeat(3) { ThemeManager.selectTheme(selected) }
                    try {
                        check(ThemeManager.selectTheme("__missing_anr_test__") == "trime")
                    } finally {
                        ThemeManager.selectTheme(selected)
                    }
                    phase("Theme reload and fallback complete")
                    rime.runOnReady {
                        val originalSchema = selectedSchemaId()
                        val originalAscii = getRuntimeOption("ascii_mode")
                        try {
                            check(selectSchema("luna_pinyin_t9"))
                            for (enabled in listOf(true, false)) {
                                setRuntimeOption("ascii_mode", enabled)
                                check(getRuntimeOptionCached("ascii_mode") == enabled)
                            }
                            check(selectSchema("luna_pinyin"))
                            check(getRuntimeOptionCached("ascii_mode") == getRuntimeOption("ascii_mode"))
                        } finally {
                            selectSchema(originalSchema)
                            setRuntimeOption("ascii_mode", originalAscii)
                        }
                    }
                    phase("Schema and option cache verified")
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
        }
        result.putLong("max_main_gap_ms", maxGap.get())
        result.putInt("main_ticks", ticks.get())
        result.putBoolean("passed", success)
        finish(if (success) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
    }
}
