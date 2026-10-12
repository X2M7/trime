/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inspector.WindowInspector
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.children
import com.osfans.trime.core.RimeApi
import com.osfans.trime.core.RimeMessage
import com.osfans.trime.daemon.RimeDaemon
import com.osfans.trime.daemon.RimeSession
import com.osfans.trime.data.prefs.AppPrefs
import com.osfans.trime.data.prefs.ChineseVariant
import com.osfans.trime.data.prefs.InputLayout
import com.osfans.trime.data.prefs.InputSetupSelection
import com.osfans.trime.ime.candidates.CandidateViewHolder
import com.osfans.trime.ime.candidates.compact.CompactCandidateDelegate
import com.osfans.trime.ime.candidates.popup.PopupCandidatesMode
import com.osfans.trime.ime.core.InputView
import com.osfans.trime.ime.core.TrimeInputMethodService
import com.osfans.trime.ime.keyboard.KeyboardWindow
import com.osfans.trime.ui.setup.SetupActivity
import com.osfans.trime.ui.setup.SetupFragment
import com.osfans.trime.ui.setup.SetupPage
import com.osfans.trime.ui.setup.SetupTrialEditText
import com.osfans.trime.util.InputMethodUtils
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.kodein.di.direct
import org.kodein.di.instance
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Disposable-emulator UI/state regression. Real keyboard touch acceptance is a separate test. */
object SetupClosedLoopProbe {
    private fun descendants(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) view.children.forEach { yieldAll(descendants(it)) }
    }

    @SuppressLint("PrivateApi")
    private fun windowRoots(): List<View> {
        if (Build.VERSION.SDK_INT >= 29) return WindowInspector.getGlobalWindowViews()
        val type = Class.forName("android.view.WindowManagerGlobal")
        val instance = type.getMethod("getInstance").invoke(null)
        @Suppress("UNCHECKED_CAST")
        return (type.getDeclaredField("mViews").apply { isAccessible = true }.get(instance) as List<View>).toList()
    }

    /** Native unfinished input and the real bound candidate labels, without selecting a word. */
    private fun verifyAppearanceDuringComposition(
        instrumentation: Instrumentation,
        activity: SetupActivity,
        trial: SetupTrialEditText,
        session: RimeSession,
        deployments: AtomicInteger,
        passed: (String) -> Unit,
    ) {
        fun <T> main(block: () -> T): T {
            var outcome: Result<T>? = null
            instrumentation.runOnMainSync { outcome = runCatching(block) }
            return checkNotNull(outcome).getOrThrow()
        }
        fun await(message: String, condition: () -> Boolean) {
            val deadline = SystemClock.uptimeMillis() + 30_000
            while (!condition()) {
                check(SystemClock.uptimeMillis() < deadline) { message }
                SystemClock.sleep(50)
            }
        }
        fun input() = windowRoots().asSequence().flatMap(::descendants).filterIsInstance<InputView>().firstOrNull { it.isShown }

        main {
            check(
                activity.supportFragmentManager.fragments.filterIsInstance<SetupFragment>()
                    .first { it.setupPage == SetupPage.Try }.requireView()
                    .findViewById<Button>(R.id.action_button).performClick(),
            )
        }
        await("The real Open Trial button did not bind a visible Trime input view") {
            main {
                input()?.let { view ->
                    val service = view.di.direct.instance<TrimeInputMethodService>()
                    trial.hasFocus() && service.currentInputEditorInfo?.fieldId == trial.id &&
                        service.isCurrentEditor(service.editorToken) && service.currentInputConnection != null
                } == true
            }
        }
        val service = main { checkNotNull(input()).di.direct.instance<TrimeInputMethodService>() }
        val token = main { service.editorToken }
        check(main { service.isPrivacyModeActive }) { "The trial editor must prohibit personalized learning" }
        suspend fun <T> api(block: suspend RimeApi.() -> T): T {
            var outcome: Result<T>? = null
            val job = main { service.postRimeJob { outcome = runCatching { block() } } }
            withTimeout(30_000) { job.join() }
            check(!job.isCancelled) { "The editor-owned appearance operation was cancelled" }
            return checkNotNull(outcome) { "The trial editor changed before the appearance operation" }.getOrThrow()
        }
        val prefs = AppPrefs.defaultInstance()
        val preferences = prefs.candidates.textSize.sharedPreferences
        val keys = listOf(prefs.candidates.textSize.key, prefs.keyboard.keyboardHeight.key, prefs.candidates.mode.key)
        val previousPreferences = keys.associateWith { preferences.all[it] }
        val originalText = main { trial.text.toString().also { check(it.isEmpty()) } }
        val originalAscii = runBlocking {
            api {
                check(getRawInput().isEmpty())
                getRuntimeOption("ascii_mode")
            }
        }
        val initialDeployments = deployments.get()
        val commits = AtomicInteger()
        val commitObserver = session.lifecycleScope.launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
            session.run { messageFlow }.collect {
                if (it is RimeMessage.CommitTextMessage && !it.data.text.isNullOrEmpty()) commits.incrementAndGet()
            }
        }
        fun candidate(view: InputView): CandidateViewHolder? = view.di.direct.instance<CompactCandidateDelegate>().view
            .findViewHolderForAdapterPosition(0) as? CandidateViewHolder
        fun label(holder: CandidateViewHolder): TextView? = descendants(holder.itemView).filterIsInstance<TextView>()
            .firstOrNull { it.isShown && it.text.toString() == holder.text }
        fun checkUncommitted(raw: String, expectedText: String) {
            check(runBlocking { api { getRawInput() } } == raw) { "Appearance preference changed unfinished Rime input" }
            main {
                check(service.isCurrentEditor(token) && service.editorToken == token && service.currentInputEditorInfo?.fieldId == trial.id)
                val view = checkNotNull(input())
                val holder = checkNotNull(candidate(view))
                check(holder.itemView.isShown && holder.text == expectedText)
                check(holder.snapshot == session.run { candidateSnapshotCached } && holder.snapshot.editorToken == token)
                val value = checkNotNull(trial.text)
                val start = BaseInputConnection.getComposingSpanStart(value)
                val end = BaseInputConnection.getComposingSpanEnd(value)
                val committedText = if (start >= 0 && end >= start) value.removeRange(start, end).toString() else value.toString()
                check(committedText == originalText) {
                    "Appearance preference committed trial text outside the composing span"
                }
                check(!activity.isPageDone(SetupPage.Try))
            }
            instrumentation.waitForIdleSync()
            check(commits.get() == 0 && deployments.get() == initialDeployments) { "Appearance preference committed text or deployed configuration" }
        }
        try {
            main {
                prefs.candidates.mode.setValue(PopupCandidatesMode.DISABLED)
                prefs.candidates.textSize.setValue(12)
                prefs.keyboard.keyboardHeight.setValue(200)
            }
            runBlocking {
                api {
                    clearComposition()
                    setRuntimeOption("ascii_mode", false)
                    simulateKeySequence("hanyu")
                }
            }
            await("Native hanyu did not reach an actual visible 12sp compact candidate") {
                main {
                    input()?.let(::candidate)?.let { holder ->
                        holder.text.any { it in '\u3400'..'\u9fff' } && label(holder)?.let {
                            kotlin.math.abs(it.textSize - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, it.resources.displayMetrics)) < 0.5f &&
                                holder.snapshot == session.run { candidateSnapshotCached }
                        } == true
                    } == true
                }
            }
            val raw = runBlocking { api { getRawInput().also { check(it == "hanyu") } } }
            val expected = main { checkNotNull(candidate(checkNotNull(input()))).text }
            checkUncommitted(raw, expected)
            val beforeFont = main { checkNotNull(input()) }
            main { prefs.candidates.textSize.setValue(36) }
            await("The rebuilt input view did not restore its real 36sp compact candidate") {
                main {
                    input()?.takeIf { it !== beforeFont }?.let(::candidate)?.let { holder ->
                        holder.text == expected && label(holder)?.let {
                            kotlin.math.abs(it.textSize - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 36f, it.resources.displayMetrics)) < 0.5f &&
                                it.height >= it.lineHeight + it.compoundPaddingTop + it.compoundPaddingBottom &&
                                holder.snapshot == session.run { candidateSnapshotCached }
                        } == true
                    } == true
                }
            }
            checkUncommitted(raw, expected)
            passed("changing candidate size 12 to 36sp restores the actual visible candidate without committing or losing unfinished hanyu")
            val beforeHeight = main { checkNotNull(input()) }
            val smallHeight = main { beforeHeight.di.direct.instance<KeyboardWindow>().currentKeyboard.height }
            main { prefs.keyboard.keyboardHeight.setValue(400) }
            await("The rebuilt taller keyboard did not restore its real compact candidate") {
                main {
                    input()?.takeIf { it !== beforeHeight }?.let { view ->
                        val holder = candidate(view)
                        view.di.direct.instance<KeyboardWindow>().currentKeyboard.height > smallHeight &&
                            holder != null && holder.text == expected && label(holder) != null &&
                            holder.snapshot == session.run { candidateSnapshotCached }
                    } == true
                }
            }
            checkUncommitted(raw, expected)
            passed("changing keyboard height 200 to 400dp retains the same editor, visible candidate and pending input without deployment")
        } finally {
            try {
                runBlocking {
                    try {
                        api {
                            clearComposition()
                            setRuntimeOption("ascii_mode", originalAscii)
                        }
                    } finally {
                        commitObserver.cancelAndJoin()
                    }
                }
            } finally {
                main {
                    val editor = preferences.edit()
                    previousPreferences.forEach { (key, value) ->
                        when (value) {
                            null -> editor.remove(key)
                            is Int -> editor.putInt(key, value)
                            is String -> editor.putString(key, value)
                            else -> error("Unexpected appearance preference type")
                        }
                    }
                    check(editor.commit())
                    trial.setText(originalText)
                }
            }
        }
    }

    private fun restore(preferences: SharedPreferences, values: Map<String, *>) {
        val editor = preferences.edit().clear()
        values.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                else -> error("Unsupported setup preference value")
            }
        }
        check(editor.commit())
    }

    fun run(instrumentation: Instrumentation) {
        val result = Bundle()
        var passed = false
        var checks = 0
        val activities = mutableListOf<SetupActivity>()
        val context = instrumentation.targetContext
        val checkpoints = context.getSharedPreferences("trime_setup", Context.MODE_PRIVATE)
        val checkpointBackup = checkpoints.all
        val profile = AppPrefs.defaultInstance().profile
        val freshBackup = profile.freshInputSetupPending.getValue()
        val owner = "setup-closed-loop-${UUID.randomUUID()}"
        val session = RimeDaemon.createSession(owner)
        var originalSchema: String? = null
        var originalOptions = emptyMap<String, Boolean>()
        val deployments = AtomicInteger()
        var observer: kotlinx.coroutines.Job? = null

        fun <T> main(block: () -> T): T {
            var outcome: Result<T>? = null
            instrumentation.runOnMainSync { outcome = runCatching(block) }
            return checkNotNull(outcome).getOrThrow()
        }
        fun await(message: String, condition: () -> Boolean) {
            val deadline = SystemClock.uptimeMillis() + 60_000
            while (!condition()) {
                check(SystemClock.uptimeMillis() < deadline) { message }
                SystemClock.sleep(50)
            }
        }
        fun passed(name: String) {
            checks++
            instrumentation.sendStatus(0, Bundle().apply { putString("stream", "PASS setup closed loop $checks: $name\n") })
        }
        fun fragment(activity: SetupActivity, page: SetupPage): SetupFragment = activity.supportFragmentManager.fragments
            .filterIsInstance<SetupFragment>().first { it.setupPage == page && it.view != null }
        fun launch(): SetupActivity {
            val monitor = instrumentation.addMonitor(SetupActivity::class.java.name, null, false)
            val handler = Handler(context.mainLooper)
            val failure = AtomicReference<Throwable?>()
            val start = Runnable {
                runCatching {
                    context.startActivity(
                        Intent(context, SetupActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }.exceptionOrNull()?.let(failure::set)
            }
            try {
                instrumentation.sendStatus(
                    0,
                    Bundle().apply {
                        putString("debug_setup_launch_stage", if (activities.isEmpty()) "fresh" else "existing")
                        putInt("debug_setup_launch_currentpid", android.os.Process.myPid())
                    },
                )
                check(handler.post(start)) { "Unable to post the public Setup Activity launch" }
                var created: SetupActivity? = null
                await("The public Setup Activity launch did not create an Activity within 60 seconds") {
                    failure.get()?.let { throw it }
                    val seen = monitor.waitForActivityWithTimeout(100) as? SetupActivity
                    if (seen != null) {
                        activities += seen
                        created = seen
                    }
                    created != null
                }
                return checkNotNull(created)
            } finally {
                handler.removeCallbacks(start)
                instrumentation.removeMonitor(monitor)
            }
        }
        fun inputReady(activity: SetupActivity) {
            await("Input setup page did not finish reading the current scheme") {
                main { activity.currentSetupPage == SetupPage.Input && !activity.changingInput && activity.inputState != null }
            }
        }
        fun confirmInput(activity: SetupActivity) {
            main { check(fragment(activity, SetupPage.Input).requireView().findViewById<Button>(R.id.action_button).performClick()) }
            await("Applying the selected input scheme failed") { main { activity.isPageDone(SetupPage.Input) && !activity.changingInput } }
        }

        try {
            check(Build.HARDWARE in setOf("ranchu", "goldfish"))
            instrumentation.sendStatus(
                0,
                Bundle().apply { putInt("setup_ime_selection_waiting_pid", android.os.Process.myPid()) },
            )
            await("The disposable emulator did not actually enable and select this APK after the UI probe started") {
                InputMethodUtils.checkIsTrimeEnabled() && InputMethodUtils.checkIsTrimeSelected()
            }
            check(InputMethodUtils.checkIsTrimeEnabled() && InputMethodUtils.checkIsTrimeSelected()) {
                "The disposable emulator must have this APK enabled and selected before the UI probe"
            }
            runBlocking {
                session.runOnReady {
                    originalSchema = selectedSchemaId()
                    originalOptions = currentSchema().switches.filter { it.name.isNotEmpty() && it.name != "prediction" }
                        .associate { it.name to getRuntimeOption(it.name) }
                    clearComposition()
                }
            }
            observer = session.lifecycleScope.launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
                session.run { messageFlow }.collect {
                    if (it is RimeMessage.DeployMessage && it.data == RimeMessage.DeployMessage.State.Start) deployments.incrementAndGet()
                }
            }
            check(checkpoints.edit().clear().commit())
            profile.freshInputSetupPending.setValue(true)
            val fresh = launch()
            inputReady(fresh)
            main {
                val view = fragment(fresh, SetupPage.Input).requireView()
                check(view.findViewById<RadioButton>(R.id.full_pinyin_option).isChecked)
                check(view.findViewById<RadioButton>(R.id.simplified_option).isChecked)
                check(!fresh.isPageDone(SetupPage.Input))
                check(!fresh.findViewById<Button>(R.id.next_button).isEnabled)
            }
            passed("fresh wizard recommends full pinyin and simplified Chinese without pretending activation finished")
            check(runBlocking { session.runOnReady { selectedSchemaId() } } == originalSchema)
            passed("reading a recommendation does not change the current Rime schema")

            confirmInput(fresh)
            val applied = runBlocking { session.runOnReady { inspectInputSetup() } }
            check(applied.schemaId == "luna_pinyin_simp")
            check(applied.selection == InputSetupSelection(InputLayout.FULL_PINYIN, ChineseVariant.SIMPLIFIED))
            check(!profile.freshInputSetupPending.getValue())
            passed("applying the actual wizard button confirms the shared simplified full-pinyin configuration")
            main { check(fresh.findViewById<Button>(R.id.next_button).performClick()) }
            await("The actual trial editor was not shown") {
                main { fresh.currentSetupPage == SetupPage.Try && fragment(fresh, SetupPage.Try).trialInput()?.isShown == true }
            }
            val trial = main { fragment(fresh, SetupPage.Try).requireView().findViewById<SetupTrialEditText>(R.id.trial_input) }
            verifyAppearanceDuringComposition(instrumentation, fresh, trial, session, deployments, ::passed)
            main {
                trial.setText("你好")
                check(!fresh.isPageDone(SetupPage.Try))
                trial.text?.clear()
                val connection = checkNotNull(trial.onCreateInputConnection(EditorInfo()))
                check(connection.commitText("nihao", 1))
                check(connection.setComposingText("你好", 1))
                check(connection.finishComposingText())
            }
            SystemClock.sleep(150)
            main { check(!fresh.isPageDone(SetupPage.Try)) }
            check(!checkpoints.getBoolean("trial_completed", false))
            passed("programmatic text, Latin commits and unfinished composition cannot pass the trial")
            main {
                trial.text?.clear()
                check(checkNotNull(trial.onCreateInputConnection(EditorInfo())).commitText("你好", 1))
            }
            await("A successful committed Han result did not complete the trial") { main { fresh.isPageDone(SetupPage.Try) } }
            check(checkpoints.all.values.none { it is String && it.contains("你好") })
            passed("successful Chinese commit completes the trial while wizard preferences keep no trial text")

            val monitor = instrumentation.addMonitor(SetupActivity::class.java.name, null, false)
            try {
                main { fresh.recreate() }
                var replacement: SetupActivity? = null
                await("Setup recreation did not produce a replacement Activity") {
                    val seen = monitor.waitForActivityWithTimeout(100) as? SetupActivity
                    if (seen != null && seen !== fresh && main { !seen.isDestroyed && !seen.isFinishing }) replacement = seen
                    replacement != null
                }
                val recreated = checkNotNull(replacement).also { activities += it }
                await("Completed trial state was lost after recreation") {
                    main { fresh.isDestroyed && recreated.currentSetupPage == SetupPage.Try && recreated.isPageDone(SetupPage.Try) }
                }
                main { check(fragment(recreated, SetupPage.Try).trialInput()?.text.isNullOrEmpty()) }
                passed("recreation keeps completion checkpoints without restoring trial text")
                main {
                    val done = recreated.findViewById<Button>(R.id.next_button)
                    check(done.isShown && done.isEnabled && done.performClick())
                }
                await("The completed wizard was not destroyed after its Done button") {
                    main { recreated.isDestroyed }
                }
                check(!SetupPage.hasUndonePage())
                check(runBlocking { session.runOnReady { inspectInputSetup() } } == applied)
                passed("the real Done button exits the completed wizard and preserves the applied input selection")
            } finally {
                instrumentation.removeMonitor(monitor)
            }

            runBlocking {
                session.runOnReady {
                    configureInput(InputSetupSelection(InputLayout.T9, ChineseVariant.TRADITIONAL))
                }
            }
            check(checkpoints.edit().clear().commit())
            profile.freshInputSetupPending.setValue(false)
            val previous = runBlocking { session.runOnReady { inspectInputSetup() } }
            val existing = launch()
            inputReady(existing)
            main { check(fragment(existing, SetupPage.Input).requireView().findViewById<RadioButton>(R.id.keep_current_input_option).isChecked) }
            confirmInput(existing)
            val retained = runBlocking { session.runOnReady { inspectInputSetup() } }
            check(retained.schemaId == previous.schemaId && retained.selection == previous.selection)
            passed("existing traditional nine-key input stays unchanged when keeping the current scheme")
            main { check(existing.findViewById<Button>(R.id.next_button).performClick()) }
            await("Existing user could not open the trial page") { main { existing.currentSetupPage == SetupPage.Try } }
            main { check(fragment(existing, SetupPage.Try).requireView().findViewById<Button>(R.id.trial_later_button).performClick()) }
            await("Explicitly deferring the trial was not accepted") { main { existing.isPageDone(SetupPage.Try) } }
            check(checkpoints.getBoolean("deferred", false) && !checkpoints.getBoolean("trial_completed", false))
            check(!SetupPage.hasUndonePage())
            passed("trying later is explicit, does not fake a successful trial, and does not reopen completed system steps")
            check(deployments.get() == 0) { "Layout/charset selection unexpectedly started deployment" }
            passed("wizard schema and charset choices do not trigger unrelated deployment")
            passed = true
            result.putString("stream", "PASS: $checks setup UI/state checks; real keyboard touch acceptance is separate\n")
        } catch (error: Throwable) {
            result.putString("stream", "FAIL: ${error.stackTraceToString()}\n")
        } finally {
            main { activities.asReversed().forEach { if (!it.isDestroyed && !it.isFinishing) it.finish() } }
            runCatching {
                await("Setup probe Activity cleanup did not finish") { main { activities.all { it.isDestroyed } } }
                runBlocking {
                    observer?.cancelAndJoin()
                    session.runOnReady {
                        clearComposition()
                        originalSchema?.let { check(selectSchema(it)) }
                        originalOptions.forEach { (name, value) -> setRuntimeOption(name, value) }
                    }
                }
                restore(checkpoints, checkpointBackup)
                profile.freshInputSetupPending.setValue(freshBackup)
            }.onFailure {
                passed = false
                result.putString("stream", result.getString("stream", "") + "FAIL cleanup: ${it.stackTraceToString()}\n")
            }
            RimeDaemon.destroySession(owner)
        }
        result.putBoolean("passed", passed)
        result.putInt("setup_closed_loop_checks", checks)
        result.putInt("setup_deploy_start_count", deployments.get())
        instrumentation.finish(if (passed) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
    }
}
