// SPDX-License-Identifier: GPL-3.0-or-later
package com.osfans.trime

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Button
import com.osfans.trime.ui.setup.SetupActivity
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** Repeated wizard navigation/status refresh without changing storage or user dictionaries. */
object SetupResponsivenessProbe {
    fun run(instrumentation: Instrumentation) {
        val handler = Handler(Looper.getMainLooper())
        val ticks = AtomicInteger()
        val maxGap = AtomicLong()
        var previous = SystemClock.uptimeMillis()
        val heartbeat = object : Runnable {
            override fun run() {
                val now = SystemClock.uptimeMillis()
                maxGap.set(maxOf(maxGap.get(), now - previous))
                previous = now
                ticks.incrementAndGet()
                handler.postDelayed(this, 50)
            }
        }
        val result = Bundle()
        var activity: SetupActivity? = null
        var clicks = 0
        var passed = false
        try {
            check(Build.HARDWARE in setOf("ranchu", "goldfish"))
            handler.post(heartbeat)
            val intent = Intent(instrumentation.targetContext, SetupActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity = instrumentation.startActivitySync(intent) as SetupActivity
            val wizard = activity
            // IO refreshes may coalesce, but must never block navigation or the looper.
            repeat(30) { index ->
                instrumentation.runOnMainSync {
                    repeat(10) { wizard.refreshCurrentFragment() }
                    val id = if (index % 2 == 0) R.id.next_button else R.id.prev_button
                    wizard.findViewById<Button>(id)?.let {
                        if (it.isShown && it.isEnabled && it.text != wizard.getString(R.string.done)) {
                            it.performClick()
                            clicks++
                        }
                    }
                }
                SystemClock.sleep(100)
            }
            instrumentation.runOnMainSync { wizard.finish() }
            SystemClock.sleep(200)
            check(ticks.get() >= 20) { "Main looper did not progress" }
            check(clicks >= 5) { "Wizard navigation was not exercised: $clicks clicks" }
            check(maxGap.get() < 4000) { "Wizard blocked main for ${maxGap.get()} ms" }
            passed = true
            result.putString("stream", "PASS: setup launch, 300 refresh requests, $clicks navigation clicks and pause; ticks=${ticks.get()}, maximum gap=${maxGap.get()} ms\n")
        } catch (error: Throwable) {
            result.putString("stream", "FAIL: ${error.stackTraceToString()}\n")
        } finally {
            handler.removeCallbacks(heartbeat)
            activity?.let { instrumentation.runOnMainSync { if (!it.isFinishing) it.finish() } }
        }
        result.putBoolean("passed", passed)
        result.putLong("max_main_gap_ms", maxGap.get())
        result.putInt("main_ticks", ticks.get())
        instrumentation.finish(if (passed) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
    }
}
