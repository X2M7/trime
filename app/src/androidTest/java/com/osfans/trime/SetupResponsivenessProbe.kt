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
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.osfans.trime.ui.main.MainActivity
import com.osfans.trime.ui.setup.SetupActivity
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** Repeated wizard navigation/status refresh without changing storage or user dictionaries. */
object SetupResponsivenessProbe {
    private fun notificationDialogLifecycle(instrumentation: Instrumentation): Int {
        check(!XXPermissions.isGranted(instrumentation.targetContext, Permission.POST_NOTIFICATIONS)) {
            "Notification dialog regression requires notifications denied on this disposable emulator"
        }
        fun <T> main(block: () -> T): T {
            var result: Result<T>? = null
            instrumentation.runOnMainSync { result = runCatching(block) }
            return checkNotNull(result).getOrThrow()
        }
        fun await(message: String, condition: () -> Boolean) {
            val deadline = SystemClock.uptimeMillis() + 15_000
            while (!condition()) {
                check(SystemClock.uptimeMillis() < deadline) { message }
                SystemClock.sleep(50)
            }
        }
        val field = MainActivity::class.java.getDeclaredField("notificationDialog").apply { isAccessible = true }
        val recheck = MainActivity::class.java.getDeclaredMethod("checkNotificationPermission").apply { isAccessible = true }
        fun dialog(activity: MainActivity) = field.get(activity) as AlertDialog?
        fun pending(activity: MainActivity): AlertDialog {
            await("Notification prompt was not shown by the real MainActivity") {
                main { dialog(activity)?.isShowing == true }
            }
            return main { checkNotNull(dialog(activity)) }
        }
        var checks = 0
        fun passed(name: String) {
            checks++
            instrumentation.sendStatus(0, Bundle().apply { putString("stream", "PASS notification $checks: $name\n") })
        }
        val activities = mutableListOf<Activity>()
        try {
            val first = (
                instrumentation.startActivitySync(
                    Intent(instrumentation.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                ) as MainActivity
                ).also { activities += it }
            val firstDialog = pending(first)
            main {
                repeat(3) { recheck.invoke(first) }
                check(dialog(first) === firstDialog && firstDialog.isShowing)
            }
            passed("repeated permission checks retain one showing prompt")

            val replacement = main {
                // Dialog posts OnDismiss on its Handler. Create the replacement
                // before that queued callback runs to exercise ownership identity.
                firstDialog.dismiss()
                recheck.invoke(first)
                checkNotNull(dialog(first)).also { check(it !== firstDialog && it.isShowing) }
            }
            instrumentation.waitForIdleSync()
            main { check(dialog(first) === replacement && replacement.isShowing) }
            passed("old queued dismissal cannot clear the replacement prompt")

            val oldDecor = main { checkNotNull(replacement.window).decorView }
            val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
            val recreated = try {
                main { first.recreate() }
                (monitor.waitForActivityWithTimeout(15_000) as? MainActivity)
                    .also { if (it != null) activities += it }
            } finally {
                instrumentation.removeMonitor(monitor)
            }
            checkNotNull(recreated) { "MainActivity did not recreate" }
            check(recreated !== first)
            val recreatedDialog = pending(recreated)
            await("Recreation retained the destroyed Activity's dialog window") {
                main { first.isDestroyed && dialog(first) == null && !replacement.isShowing && !oldDecor.isAttachedToWindow }
            }
            main { check(recreatedDialog !== replacement && recreatedDialog.isShowing) }
            passed("recreation removes the old window and shows a prompt owned by the new Activity")

            val recreatedDecor = main { checkNotNull(recreatedDialog.window).decorView }
            val cover = (
                instrumentation.startActivitySync(
                    Intent(instrumentation.targetContext, SetupActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                ) as SetupActivity
                ).also { activities += it }
            await("Stopped MainActivity retained its pending notification window") {
                main {
                    !recreated.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
                        dialog(recreated) == null && !recreatedDialog.isShowing && !recreatedDecor.isAttachedToWindow
                }
            }
            main {
                check(!recreated.isDestroyed)
                recheck.invoke(recreated)
                check(dialog(recreated) == null)
            }
            passed("covering the Activity dismisses its prompt and rejects late permission checks")

            main { cover.finish() }
            await("MainActivity did not resume after closing the covering page") {
                main { recreated.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
            }
            main { recheck.invoke(recreated) }
            val finishingDialog = pending(recreated)
            val finishingDecor = main { checkNotNull(finishingDialog.window).decorView }
            main { recreated.finish() }
            await("Finishing MainActivity retained its pending notification window") {
                main { recreated.isDestroyed && dialog(recreated) == null && !finishingDialog.isShowing && !finishingDecor.isAttachedToWindow }
            }
            passed("finishing with a pending prompt removes its window and owner reference")
            return checks
        } finally {
            main { activities.asReversed().forEach { if (!it.isFinishing && !it.isDestroyed) it.finish() } }
            await("Notification test Activity cleanup did not finish") { main { activities.all { it.isDestroyed } } }
        }
    }

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
        var notificationChecks = 0
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationChecks = notificationDialogLifecycle(instrumentation)
            } else {
                instrumentation.sendStatus(0, Bundle().apply { putString("stream", "SKIP: notification runtime-permission dialog lifecycle requires API 33+\n") })
            }
            check(ticks.get() >= 20) { "Main looper did not progress" }
            check(clicks >= 5) { "Wizard navigation was not exercised: $clicks clicks" }
            check(maxGap.get() < 4000) { "Wizard blocked main for ${maxGap.get()} ms" }
            passed = true
            result.putString("stream", "PASS: setup launch, 300 refresh requests, $clicks navigation clicks and pause; $notificationChecks notification dialog lifecycle checks; ticks=${ticks.get()}, maximum gap=${maxGap.get()} ms\n")
        } catch (error: Throwable) {
            result.putString("stream", "FAIL: ${error.stackTraceToString()}\n")
        } finally {
            handler.removeCallbacks(heartbeat)
            activity?.let { instrumentation.runOnMainSync { if (!it.isFinishing) it.finish() } }
        }
        result.putBoolean("passed", passed)
        result.putLong("max_main_gap_ms", maxGap.get())
        result.putInt("main_ticks", ticks.get())
        result.putInt("notification_dialog_checks", notificationChecks)
        instrumentation.finish(if (passed) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
    }
}
