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
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.osfans.trime.ui.main.MainActivity
import com.osfans.trime.ui.main.NavigationRoute
import com.osfans.trime.ui.setup.SetupActivity
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** Repeated wizard navigation/status refresh without changing storage or user dictionaries. */
object SetupResponsivenessProbe {
    private val visibleMainMenuItems =
        intArrayOf(
            R.id.action_deploy,
            R.id.action_test_input,
            R.id.action_developer,
            R.id.action_about,
        )
    private val hiddenMainMenuItems =
        intArrayOf(
            R.id.action_edit,
            R.id.action_delete,
        )

    private fun hasExpectedMainToolbarMenu(activity: MainActivity): Boolean {
        val expected = visibleMainMenuItems + hiddenMainMenuItems
        val menu = activity.findViewById<Toolbar>(R.id.toolbar)?.menu ?: return false
        if (menu.size() != expected.size) return false
        val actual = IntArray(menu.size()) { menu.getItem(it).itemId }
        return actual.toSet() == expected.toSet() &&
            actual.size == actual.toSet().size &&
            visibleMainMenuItems.all { menu.findItem(it)?.run { isVisible && isEnabled } == true } &&
            hiddenMainMenuItems.all { menu.findItem(it)?.isVisible == false }
    }

    /**
     * Android 5 also reports the Activity being destroyed to an ActivityMonitor.
     * Keep waiting until the monitor observes the live replacement created by
     * recreate(), then let the old/new lifecycle handoff finish.
     */
    private fun recreateAndAwaitReplacement(
        instrumentation: Instrumentation,
        previous: MainActivity,
    ): MainActivity {
        fun <T> main(block: () -> T): T {
            var result: Result<T>? = null
            instrumentation.runOnMainSync { result = runCatching(block) }
            return checkNotNull(result).getOrThrow()
        }
        val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
        val deadline = SystemClock.uptimeMillis() + 15_000
        try {
            main { previous.recreate() }
            while (true) {
                val remaining = deadline - SystemClock.uptimeMillis()
                check(remaining > 0) { "MainActivity did not produce a live replacement" }
                val seen = monitor.waitForActivityWithTimeout(remaining) as? MainActivity
                if (seen == null) {
                    check(SystemClock.uptimeMillis() < deadline) { "MainActivity did not recreate" }
                    continue
                }
                if (seen === previous || main { seen.isDestroyed || seen.isFinishing }) continue
                while (
                    !main {
                        previous.isDestroyed &&
                            !seen.isDestroyed &&
                            !seen.isFinishing &&
                            seen.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                    }
                ) {
                    check(SystemClock.uptimeMillis() < deadline) {
                        "MainActivity replacement did not finish its lifecycle handoff"
                    }
                    SystemClock.sleep(50)
                }
                return seen
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    private fun toolbarMenuLifecycle(instrumentation: Instrumentation): Int {
        fun <T> main(block: () -> T): T {
            var result: Result<T>? = null
            instrumentation.runOnMainSync { result = runCatching(block) }
            return checkNotNull(result).getOrThrow()
        }
        fun await(activity: MainActivity, message: String) {
            val deadline = SystemClock.uptimeMillis() + 15_000
            while (!main { hasExpectedMainToolbarMenu(activity) }) {
                check(SystemClock.uptimeMillis() < deadline) { message }
                SystemClock.sleep(50)
            }
        }
        var checks = 0
        fun passed(name: String) {
            checks++
            instrumentation.sendStatus(0, Bundle().apply { putString("stream", "PASS toolbar $checks: $name\n") })
        }
        val activities = mutableListOf<Activity>()
        try {
            val first = (
                instrumentation.startActivitySync(
                    Intent(instrumentation.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                ) as MainActivity
                ).also { activities += it }
            await(first, "Toolbar menu was not installed after the first pre-draw and focus handoff")
            passed("first launch installs one correctly configured menu")

            val recreated = recreateAndAwaitReplacement(instrumentation, first).also { activities += it }
            check(recreated !== first)
            await(recreated, "Recreated Activity did not install one correctly configured menu")
            passed("recreation preserves one correctly configured menu")
            return checks
        } finally {
            main { activities.asReversed().forEach { if (!it.isFinishing && !it.isDestroyed) it.finish() } }
            val deadline = SystemClock.uptimeMillis() + 15_000
            while (!main { activities.all { it.isDestroyed } }) {
                check(SystemClock.uptimeMillis() < deadline) { "Toolbar test Activity cleanup did not finish" }
                SystemClock.sleep(50)
            }
        }
    }

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
        fun awaitToolbarMenu(activity: MainActivity) {
            await("Deferred toolbar menu was not installed after the first pre-draw and focus handoff") {
                main { hasExpectedMainToolbarMenu(activity) }
            }
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
            awaitToolbarMenu(first)
            passed("first pre-draw and focus handoff install every toolbar action")
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
            awaitToolbarMenu(first)
            passed("old queued dismissal cannot clear the replacement prompt")

            val oldDecor = main { checkNotNull(replacement.window).decorView }
            val recreated = recreateAndAwaitReplacement(instrumentation, first).also { activities += it }
            check(recreated !== first)
            awaitToolbarMenu(recreated)
            passed("recreation installs every toolbar action exactly once")
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
            awaitToolbarMenu(recreated)
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

    private fun actionRunRestoration(instrumentation: Instrumentation): Int {
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
        val controllerField = MainActivity::class.java.getDeclaredField("navController").apply { isAccessible = true }
        val dialogField = MainActivity::class.java.getDeclaredField("notificationDialog").apply { isAccessible = true }
        fun controller(activity: MainActivity): NavController? = runCatching { controllerField.get(activity) as? NavController }.getOrNull()
        fun awaitRoute(activity: MainActivity, route: String, message: String) {
            await(message) { main { controller(activity)?.currentDestination?.route == route } }
        }
        fun awaitOwnerWindowFocused(activity: MainActivity, message: String) {
            await(message) {
                main {
                    !activity.isDestroyed &&
                        !activity.isFinishing &&
                        activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                        (
                            activity.hasWindowFocus() ||
                                (dialogField.get(activity) as? AlertDialog)
                                    ?.window?.decorView?.hasWindowFocus() == true
                            )
                }
            }
        }
        val activities = mutableListOf<Activity>()
        try {
            val command =
                Intent(instrumentation.targetContext, MainActivity::class.java)
                    .setAction(Intent.ACTION_RUN)
                    .putExtra(MainActivity.EXTRA_SETTINGS_ROUTE, NavigationRoute.About)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val first = (instrumentation.startActivitySync(command) as MainActivity).also { activities += it }
            awaitRoute(first, NavigationRoute.About.path, "ACTION_RUN did not reach the requested destination")
            main {
                check(first.intent.action == null && !first.intent.hasExtra(MainActivity.EXTRA_SETTINGS_ROUTE)) {
                    "MainActivity retained a consumed ACTION_RUN command"
                }
                val nav = checkNotNull(controller(first))
                nav.popBackStack(NavigationRoute.Main.path, false)
                nav.navigate(NavigationRoute.Developer.path)
                (dialogField.get(first) as? AlertDialog)?.dismiss()
            }
            awaitRoute(first, NavigationRoute.Developer.path, "Could not leave the direct destination")

            val recreated = recreateAndAwaitReplacement(instrumentation, first).also { activities += it }
            check(recreated !== first)
            awaitRoute(
                recreated,
                NavigationRoute.Developer.path,
                "Recreation replayed ACTION_RUN or lost the restored non-default destination",
            )
            main {
                check(recreated.intent.action == null && !recreated.intent.hasExtra(MainActivity.EXTRA_SETTINGS_ROUTE))
            }
            instrumentation.waitForIdleSync()
            awaitOwnerWindowFocused(recreated, "ACTION_RUN recreation did not finish its focus handoff")

            // MainActivity is singleTask. A command sent while it is alive must
            // be consumed through onNewIntent without becoming persistent state.
            val warmCommand =
                Intent(instrumentation.targetContext, MainActivity::class.java)
                    .setAction(Intent.ACTION_RUN)
                    .putExtra(MainActivity.EXTRA_SETTINGS_ROUTE, NavigationRoute.About)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            instrumentation.targetContext.startActivity(warmCommand)
            awaitRoute(recreated, NavigationRoute.About.path, "Warm ACTION_RUN did not reach the existing MainActivity")
            main {
                check(recreated.intent.action == null && !recreated.intent.hasExtra(MainActivity.EXTRA_SETTINGS_ROUTE)) {
                    "MainActivity retained a warm ACTION_RUN command"
                }
                val nav = checkNotNull(controller(recreated))
                nav.popBackStack(NavigationRoute.Main.path, false)
                nav.navigate(NavigationRoute.General.path)
                (dialogField.get(recreated) as? AlertDialog)?.dismiss()
            }
            awaitRoute(recreated, NavigationRoute.General.path, "Could not leave the warm direct destination")

            val warmRecreated = recreateAndAwaitReplacement(instrumentation, recreated).also { activities += it }
            check(warmRecreated !== recreated)
            awaitRoute(
                warmRecreated,
                NavigationRoute.General.path,
                "Warm recreation replayed ACTION_RUN or lost the restored non-default destination",
            )
            main {
                check(warmRecreated.intent.action == null && !warmRecreated.intent.hasExtra(MainActivity.EXTRA_SETTINGS_ROUTE))
                (dialogField.get(warmRecreated) as? AlertDialog)?.dismiss()
            }
            instrumentation.sendStatus(
                0,
                Bundle().apply {
                    putString(
                        "stream",
                        "PASS: cold and warm ACTION_RUN commands are consumed and preserve later destinations across recreation\n",
                    )
                },
            )
            return 2
        } finally {
            main { activities.asReversed().forEach { if (!it.isFinishing && !it.isDestroyed) it.finish() } }
            await("ACTION_RUN test Activity cleanup did not finish") { main { activities.all { it.isDestroyed } } }
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
        var toolbarChecks = 0
        var notificationChecks = 0
        var actionRunChecks = 0
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
            actionRunChecks = actionRunRestoration(instrumentation)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationChecks = notificationDialogLifecycle(instrumentation)
            } else {
                toolbarChecks = toolbarMenuLifecycle(instrumentation)
                instrumentation.sendStatus(0, Bundle().apply { putString("stream", "SKIP: notification runtime-permission dialog lifecycle requires API 33+\n") })
            }
            check(ticks.get() >= 20) { "Main looper did not progress" }
            check(clicks >= 5) { "Wizard navigation was not exercised: $clicks clicks" }
            check(maxGap.get() < 4000) { "Wizard blocked main for ${maxGap.get()} ms" }
            passed = true
            result.putString("stream", "PASS: setup launch, 300 refresh requests, $clicks navigation clicks and pause; $actionRunChecks ACTION_RUN restoration check; $toolbarChecks standalone toolbar checks; $notificationChecks notification dialog lifecycle checks; ticks=${ticks.get()}, maximum gap=${maxGap.get()} ms\n")
        } catch (error: Throwable) {
            result.putString("stream", "FAIL: ${error.stackTraceToString()}\n")
        } finally {
            handler.removeCallbacks(heartbeat)
            activity?.let { instrumentation.runOnMainSync { if (!it.isFinishing) it.finish() } }
        }
        result.putBoolean("passed", passed)
        result.putLong("max_main_gap_ms", maxGap.get())
        result.putInt("main_ticks", ticks.get())
        result.putInt("toolbar_menu_checks", toolbarChecks)
        result.putInt("notification_dialog_checks", notificationChecks)
        result.putInt("action_run_checks", actionRunChecks)
        instrumentation.finish(if (passed) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
    }
}
