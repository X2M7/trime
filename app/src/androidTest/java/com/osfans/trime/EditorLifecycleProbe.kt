/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.inspector.WindowInspector
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.view.children
import com.osfans.trime.core.RimeApi
import com.osfans.trime.core.RimeSchema
import com.osfans.trime.daemon.RimeDaemon
import com.osfans.trime.daemon.RimeSession
import com.osfans.trime.data.base.DataManager
import com.osfans.trime.data.prefs.AppPrefs
import com.osfans.trime.data.theme.ThemeManager
import com.osfans.trime.ime.broadcast.EnterKeyDisplayDelegate
import com.osfans.trime.ime.candidates.compact.CompactCandidateDelegate
import com.osfans.trime.ime.candidates.popup.PopupCandidatesMode
import com.osfans.trime.ime.composition.PreeditDelegate
import com.osfans.trime.ime.composition.T9DisambiguationView
import com.osfans.trime.ime.core.InputView
import com.osfans.trime.ime.core.TouchEventReceiverWindow
import com.osfans.trime.ime.core.TrimeInputMethodService
import com.osfans.trime.ime.keyboard.CommonKeyboardActionListener
import com.osfans.trime.ime.keyboard.KeyAction
import com.osfans.trime.ime.keyboard.KeyView
import com.osfans.trime.ime.keyboard.KeyboardWindow
import com.osfans.trime.ime.switches.SwitchOptionAdapter
import com.osfans.trime.ime.switches.SwitchOptionEntry
import com.osfans.trime.ime.switches.SwitchOptionWindow
import com.osfans.trime.ui.main.ClipEditActivity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.kodein.di.direct
import org.kodein.di.instance
import java.io.File

/** Real InputConnections in an unsaved, disposable editor. Never resets learned words. */
object EditorLifecycleProbe {
    private fun descendants(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) view.children.forEach { yieldAll(descendants(it)) }
    }

    @SuppressLint("PrivateApi")
    private fun roots(): List<View> {
        if (Build.VERSION.SDK_INT >= 29) return WindowInspector.getGlobalWindowViews()
        val type = Class.forName("android.view.WindowManagerGlobal")
        val instance = type.getMethod("getInstance").invoke(null)
        @Suppress("UNCHECKED_CAST")
        return (type.getDeclaredField("mViews").apply { isAccessible = true }.get(instance) as List<View>).toList()
    }

    fun run(instrumentation: Instrumentation) {
        val context = instrumentation.targetContext
        val result = Bundle()
        var activity: Activity? = null
        var session: RimeSession? = null
        var web: WebView? = null
        var passed = false
        val originalTheme = ThemeManager.prefs.selectedTheme.getValue()
        val candidatesMode = AppPrefs.defaultInstance().candidates.mode
        val originalCandidatesMode = candidatesMode.getValue()
        var originalSchema = ""
        var originalAscii = false
        var fixture: File? = null
        fun <T> main(block: () -> T): T {
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) return block()
            var value: Result<T>? = null
            instrumentation.runOnMainSync { value = runCatching(block) }
            return checkNotNull(value).getOrThrow()
        }
        fun phase(text: String) = instrumentation.sendStatus(0, Bundle().apply { putString("stream", "$text\n") })
        try {
            check(Build.HARDWARE in setOf("ranchu", "goldfish")) { "Emulator only" }
            check(File(context.getExternalFilesDir(null), "runtime-audit-dedicated").isFile) { "Requires a dedicated runtime-audit installation" }
            session = main { RimeDaemon.createSession(javaClass.name) }
            runBlocking {
                withTimeout(300_000) {
                    checkNotNull(session).runOnReady {
                        originalSchema = selectedSchemaId()
                        originalAscii = statusCached.isAsciiMode
                    }
                }
                ThemeManager.init(context.resources.configuration)
            }
            val editor = instrumentation.startActivitySync(Intent(context, ClipEditActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            activity = editor
            val actions = mutableListOf<Int>()
            val fields = main {
                val row = LinearLayout(editor).apply { orientation = LinearLayout.VERTICAL }
                val chat = EditText(editor).apply {
                    id = View.generateViewId()
                    hint = "chat"
                    inputType = InputType.TYPE_CLASS_TEXT
                    imeOptions = EditorInfo.IME_ACTION_SEND
                }
                val other = EditText(editor).apply {
                    id = View.generateViewId()
                    hint = "target"
                }
                chat.setOnEditorActionListener { _, id, _ ->
                    actions.add(id)
                    false
                }
                other.setOnEditorActionListener { _, id, _ ->
                    actions.add(id)
                    id != 0
                }
                row.addView(chat, LinearLayout.LayoutParams(-1, 72))
                row.addView(other, LinearLayout.LayoutParams(-1, 72))
                editor.setContentView(row)
                chat.requestFocus()
                (editor.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(chat, 0)
                Triple(chat, other, row)
            }
            val (chat, other, container) = fields
            runBlocking {
                withTimeout(600_000) {
                    suspend fun until(label: String, predicate: () -> Boolean) {
                        withTimeout(30_000) { while (!main(predicate)) delay(50) }
                        phase("PASS: $label")
                    }
                    fun input(): InputView? = roots().asSequence().flatMap(::descendants).filterIsInstance<InputView>().firstOrNull { it.isShown }
                    until("initial keyboard visible") { input() != null }
                    val (overlayView, overlay, popup) = main {
                        val view = View(editor)
                        val receiver = TouchEventReceiverWindow(view)
                        val popup = TouchEventReceiverWindow::class.java.getDeclaredField("window").apply { isAccessible = true }.get(receiver) as PopupWindow
                        repeat(100) { receiver.showAt(0, 0, 32, 32) }
                        check(!popup.isShowing)
                        receiver.show()
                        container.addView(view, LinearLayout.LayoutParams(32, 32))
                        Triple(view, receiver, popup)
                    }
                    until("touch overlay waits for attachment and layout") { popup.isShowing }
                    main { overlayView.visibility = View.INVISIBLE }
                    until("invisible anchor dismisses its touch overlay without resizing") { !popup.isShowing }
                    main { overlayView.visibility = View.VISIBLE }
                    until("visible anchor resumes its requested touch overlay") { popup.isShowing }
                    main { container.visibility = View.INVISIBLE }
                    until("invisible ancestor dismisses the touch overlay") { !popup.isShowing }
                    main { container.visibility = View.VISIBLE }
                    until("visible ancestor resumes the touch overlay") { popup.isShowing }
                    main {
                        container.removeView(overlayView)
                        check(!popup.isShowing)
                        container.addView(overlayView, LinearLayout.LayoutParams(32, 32))
                    }
                    until("touch overlay anchor reattached") { overlayView.isAttachedToWindow && overlayView.width > 0 }
                    main {
                        check(!popup.isShowing) { "Detached overlay must not resurrect" }
                        overlay.show()
                        check(popup.isShowing)
                        overlay.dismiss()
                        check(!popup.isShowing)
                        container.removeView(overlayView)
                        overlay.show()
                        check(!popup.isShowing)
                        overlay.dismiss()
                    }
                    phase("PASS: detached touch overlay cannot crash or leak across reattachment")
                    val service = main { checkNotNull(input()).di.direct.instance<TrimeInputMethodService>() }
                    suspend fun api(block: suspend RimeApi.() -> Unit) {
                        var outcome: Result<Unit>? = null
                        val job = main { service.postRimeJob { outcome = runCatching { block() } } }
                        job.join()
                        check(!job.isCancelled) { "Engine/editor job cancelled" }
                        checkNotNull(outcome) { "Editor changed before operation ran" }.getOrThrow()
                        main { }
                    }
                    fun keyboard() = checkNotNull(input()).di.direct.instance<KeyboardWindow>().currentKeyboard
                    fun type(value: String) = main { checkNotNull(input()).di.direct.instance<CommonKeyboardActionListener>().listener.onText(value) }
                    fun enter() = main { checkNotNull(input()).di.direct.instance<CommonKeyboardActionListener>().listener.onAction(KeyAction("Return")) }
                    suspend fun focus(field: EditText) {
                        main {
                            field.requestFocus()
                            (editor.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(field, 0)
                        }
                        until("editor ${field.hint} owns connection") { service.currentInputEditorInfo?.fieldId == field.id && input() != null }
                        api { }
                    }
                    api {
                        check(selectSchema("luna_pinyin_t9"))
                        setRuntimeOption("ascii_mode", false)
                    }
                    until("selected T9 is actually a nine-key layout") { input() != null && keyboard().isT9Layout }
                    for (theme in listOf("trime", "tongwenfeng.trime")) {
                        ThemeManager.selectTheme(theme)
                        until("$theme T9 before schema switch") { input() != null && keyboard().isT9Layout }
                        api {
                            check(selectSchema("luna_pinyin_simp"))
                            setRuntimeOption("ascii_mode", false)
                        }
                        until("$theme full pinyin replaces locked T9") { input() != null && !keyboard().isT9Layout }
                        type("ni")
                        api { check(getRawInput() == "ni") }
                        val preeditOverlay = main {
                            val preedit = checkNotNull(input()).di.direct.instance<PreeditDelegate>()
                            val receiver = PreeditDelegate::class.java.getDeclaredField("touchEventReceiverWindow").apply { isAccessible = true }.get(preedit)
                            TouchEventReceiverWindow::class.java.getDeclaredField("window").apply { isAccessible = true }.get(receiver) as PopupWindow
                        }
                        until("$theme full-pinyin preedit overlay is visible") { preeditOverlay.isShowing }
                        val imm = editor.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                        main { imm.hideSoftInputFromWindow(chat.windowToken, 0) }
                        until("$theme hiding input dismisses the preedit overlay") { !service.isInputViewShown && !preeditOverlay.isShowing }
                        main { imm.showSoftInput(chat, 0) }
                        until("$theme showing input restores the preedit overlay") { service.isInputViewShown && preeditOverlay.isShowing }
                        api {
                            check(getRawInput() == "ni")
                            clearComposition()
                        }
                        main {
                            other.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        }
                        focus(other)
                        focus(chat)
                        api {
                            check(selectedSchemaId() == "luna_pinyin_simp" && !statusCached.isAsciiMode)
                        }
                        check(main { !keyboard().isT9Layout }) { "$theme restored an old schema's T9 lock after email" }
                        api {
                            check(selectSchema("luna_pinyin_t9"))
                            setRuntimeOption("ascii_mode", false)
                        }
                        until("$theme T9 returns after full pinyin") { input() != null && keyboard().isT9Layout }
                    }
                    ThemeManager.selectTheme(originalTheme)
                    until("original theme retains selected T9") { input() != null && keyboard().isT9Layout }
                    val cases = listOf(
                        Triple("email", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, EditorInfo.IME_ACTION_NEXT),
                        Triple("web-email", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS, EditorInfo.IME_ACTION_NEXT),
                        Triple("password", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, EditorInfo.IME_ACTION_DONE),
                        Triple("web-password", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD, EditorInfo.IME_ACTION_DONE),
                        Triple("visible-password", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, EditorInfo.IME_ACTION_DONE),
                        Triple("address", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI, EditorInfo.IME_ACTION_GO),
                        Triple("phone", InputType.TYPE_CLASS_PHONE, EditorInfo.IME_ACTION_DONE),
                        Triple("integer", InputType.TYPE_CLASS_NUMBER, EditorInfo.IME_ACTION_DONE),
                        Triple("numeric-password", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD, EditorInfo.IME_ACTION_DONE),
                        Triple("decimal", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED, EditorInfo.IME_ACTION_DONE),
                        Triple("search", InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_SEARCH),
                        Triple("multiline", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE, EditorInfo.IME_ACTION_NONE or EditorInfo.IME_FLAG_NO_ENTER_ACTION),
                    )
                    for ((name, inputType, options) in cases) {
                        main {
                            other.hint = name
                            other.inputType = inputType
                            other.imeOptions = options
                            other.setText("")
                            actions.clear()
                        }
                        focus(other)
                        val restricted = name !in setOf("search", "multiline")
                        api { check(statusCached.isAsciiMode == restricted) { "$name ASCII state mismatch" } }
                        check(main { keyboard().isT9Layout } != restricted) { "$name layout mismatch" }
                        if (restricted) {
                            type(if (name == "decimal") "12.3" else "12")
                            api { }
                            until("$name direct text") { other.text.toString() == if (name == "decimal") "12.3" else "12" }
                        }
                        val label = main { checkNotNull(input()).di.direct.instance<EnterKeyDisplayDelegate>().keyLabel }
                        check(label.isNotEmpty() && label != "null")
                        enter()
                        api { }
                        if (name != "multiline") {
                            until("$name editor action matches label '$label'") { actions.lastOrNull() == options and EditorInfo.IME_MASK_ACTION }
                        } else {
                            until("multiline gets newline") { other.text.toString().endsWith('\n') }
                        }
                        focus(chat)
                        api {
                            check(!statusCached.isAsciiMode)
                            check(selectedSchemaId() == "luna_pinyin_t9")
                        }
                        check(main { keyboard().isT9Layout }) { "$name did not restore T9" }
                    }
                    main {
                        other.inputType = InputType.TYPE_CLASS_TEXT
                        other.imeOptions = EditorInfo.IME_ACTION_SEARCH
                        other.setImeActionLabel("Continue", 731)
                        actions.clear()
                    }
                    focus(other)
                    check(main { checkNotNull(input()).di.direct.instance<EnterKeyDisplayDelegate>().keyLabel } == "Continue")
                    enter()
                    api { }
                    until("custom action uses ID 731") { actions.lastOrNull() == 731 }
                    focus(chat)

                    api { setRuntimeOption("ascii_mode", true) }
                    main {
                        other.inputType = InputType.TYPE_CLASS_NUMBER
                        other.setImeActionLabel(null, 0)
                    }
                    focus(other)
                    focus(chat)
                    api { check(statusCached.isAsciiMode) { "Temporary number mode lost the user's explicit ASCII choice" } }
                    api { setRuntimeOption("ascii_mode", false) }

                    main {
                        other.inputType = InputType.TYPE_CLASS_TEXT
                        other.imeOptions = EditorInfo.IME_ACTION_SEARCH
                        other.setImeActionLabel("Unusable action", 0)
                    }
                    focus(other)
                    check(main { checkNotNull(input()).di.direct.instance<EnterKeyDisplayDelegate>().keyLabel } != "Unusable action")
                    focus(chat)

                    // Hardware mode does not start KeyboardWindow for subsequent editors.
                    // Restricted fields must therefore bypass Rime independently of its mode.
                    main { candidatesMode.setValue(PopupCandidatesMode.INPUT_DEVICE) }
                    instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_6)
                    until("physical keyboard hides the virtual keyboard") { service.isInputViewShown && input() == null }
                    api {
                        check(getRawInput() == "6")
                        clearComposition()
                    }
                    suspend fun focusPhysical(field: EditText) {
                        main { field.requestFocus() }
                        until("physical editor ${field.hint} owns connection") { service.currentInputEditorInfo?.fieldId == field.id }
                        api { }
                    }
                    for ((name, inputType) in listOf(
                        "hardware-password" to (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD),
                        "hardware-email" to (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS),
                        "hardware-number" to InputType.TYPE_CLASS_NUMBER,
                    )) {
                        main {
                            other.hint = name
                            other.inputType = inputType
                            other.setText("")
                        }
                        focusPhysical(other)
                        api { check(!statusCached.isAsciiMode) { "Hardware test must exercise an unchanged Chinese engine mode" } }
                        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_6)
                        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_4)
                        until("$name receives physical digits directly") { other.text.toString() == "64" }
                        api { check(getRawInput().isEmpty()) { "$name leaked hardware input to Rime" } }
                        focusPhysical(chat)
                        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_6)
                        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_4)
                        api {
                            check(!statusCached.isAsciiMode && getRawInput() == "64") { "Physical chat did not retain Chinese input after $name" }
                            clearComposition()
                        }
                    }
                    main {
                        candidatesMode.setValue(originalCandidatesMode)
                        @Suppress("DEPRECATION")
                        service.onViewClicked(false)
                    }
                    focus(chat)

                    val queueEntered = CompletableDeferred<Unit>()
                    val queueRelease = CompletableDeferred<Unit>()
                    var overtook = false
                    main {
                        service.postRimeJob {
                            queueEntered.complete(Unit)
                            queueRelease.await()
                        }
                    }
                    queueEntered.await()
                    val following = main { service.postRimeJob { overtook = true } }
                    val waiting = async { following.join() }
                    delay(150)
                    check(!overtook) { "Joining a queued job bypassed the serial executor" }
                    queueRelease.complete(Unit)
                    waiting.await()
                    check(overtook)
                    phase("PASS: joining an engine job preserves queue order")

                    // A real suspend builder must retain the editor that requested its dialog.
                    // Reflection keeps this deterministic fault gate out of the production API.
                    main {
                        other.inputType = InputType.TYPE_CLASS_TEXT
                        other.imeOptions = EditorInfo.IME_ACTION_SEARCH
                        other.setImeActionLabel(null, 0)
                    }
                    val switchWindow = main { SwitchOptionWindow(checkNotNull(input()).di) }
                    val dialogEntered = CompletableDeferred<Unit>()
                    val dialogRelease = CompletableDeferred<Unit>()
                    val dialogReturned = CompletableDeferred<Unit>()
                    val delayedDialog = main { AlertDialog.Builder(service).setTitle("Editor-owned dialog audit").create() }
                    val delayedBuilder: suspend (RimeApi) -> Dialog = {
                        dialogEntered.complete(Unit)
                        dialogRelease.await()
                        dialogReturned.complete(Unit)
                        delayedDialog
                    }
                    main {
                        SwitchOptionWindow::class.java.declaredMethods.single { it.name == "showDialog" }.apply {
                            isAccessible = true
                        }.invoke(switchWindow, delayedBuilder)
                    }
                    try {
                        withTimeout(5000) { dialogEntered.await() }
                        main {
                            other.setText("fresh target")
                            other.requestFocus()
                        }
                        until("new editor replaces a pending switch-window dialog") { service.currentInputEditorInfo?.fieldId == other.id }
                        dialogRelease.complete(Unit)
                        withTimeout(5000) { dialogReturned.await() }
                        api { }
                        main {
                            check(!delayedDialog.isShowing) { "Old editor dialog appeared over the new editor" }
                            check(other.text.toString() == "fresh target") { "Old dialog changed the new editor text" }
                        }
                    } finally {
                        dialogRelease.complete(Unit)
                        main { delayedDialog.dismiss() }
                    }
                    focus(chat)

                    val switchAdapter = main {
                        // setAdapter invokes onAttachedToRecyclerView, which supplies the
                        // adapter's context for its real popup-menu action below.
                        switchWindow.onCreateView()
                        checkNotNull(switchWindow.view.adapter) as SwitchOptionAdapter
                    }
                    val queuedOption = "_editor_audit_queued_option"
                    api { setRuntimeOption(queuedOption, false) }
                    val optionEntered = CompletableDeferred<Unit>()
                    val optionRelease = CompletableDeferred<Unit>()
                    main {
                        service.postRimeJob {
                            optionEntered.complete(Unit)
                            optionRelease.await()
                        }
                    }
                    withTimeout(5000) { optionEntered.await() }
                    try {
                        main {
                            switchAdapter.onItemClick(
                                checkNotNull(input()),
                                SwitchOptionEntry.Custom(RimeSchema.Switch(name = queuedOption, states = listOf("Off", "On")), "Audit option", 0),
                            )
                        }
                        delay(150)
                        checkNotNull(session).runOnReady {
                            check(!getRuntimeOption(queuedOption)) { "Switch-window option bypassed the editor queue" }
                        }
                        main {
                            other.setText("")
                            other.requestFocus()
                        }
                        until("editor changes with a queued switch-window option") { service.currentInputEditorInfo?.fieldId == other.id }
                    } finally {
                        optionRelease.complete(Unit)
                    }
                    api {
                        check(!getRuntimeOption(queuedOption)) { "Old switch-window option changed the new editor" }
                        check(getRawInput().isEmpty())
                    }
                    check(main { other.text.isEmpty() }) { "Queued switch-window option committed into the new editor" }
                    focus(chat)

                    // A dismissed popup's retained callback must use the menu's original token,
                    // even when Android delivers its click after the next editor has bound.
                    val menuOptions = listOf("_editor_audit_menu_first", "_editor_audit_menu_second")
                    api { menuOptions.forEach { setRuntimeOption(it, false) } }
                    val oldMenu = main {
                        switchAdapter.onItemClick(
                            checkNotNull(input()),
                            SwitchOptionEntry.Custom(RimeSchema.Switch(options = menuOptions, states = listOf("First", "Second")), "Audit menu", 0),
                        )
                        checkNotNull(switchWindow.popupMenu).menu.also { switchWindow.onDetached() }
                    }
                    focus(other)
                    main { check(oldMenu.performIdentifierAction(0, 0)) { "Retained option menu callback did not execute" } }
                    delay(150)
                    api {
                        check(menuOptions.none { getRuntimeOption(it) }) { "Old option popup changed the new editor" }
                        check(getRawInput().isEmpty())
                    }
                    check(main { other.text.isEmpty() }) { "Old option popup committed into the new editor" }
                    focus(chat)
                    phase("PASS: delayed switch dialogs, queued options and stale popup callbacks remain editor-owned")

                    type("64")
                    api { check(getRawInput() == "64") }
                    until("compact Hanzi candidate is clickable") {
                        input()?.di?.direct?.instance<CompactCandidateDelegate>()?.view?.findViewHolderForAdapterPosition(0)?.itemView?.isShown == true
                    }
                    val beforeCandidate = main { chat.text.toString() }
                    val candidateEntered = CompletableDeferred<Unit>()
                    val candidateRelease = CompletableDeferred<Unit>()
                    main {
                        service.postRimeJob {
                            candidateEntered.complete(Unit)
                            candidateRelease.await()
                        }
                    }
                    candidateEntered.await()
                    try {
                        main {
                            checkNotNull(input()).di.direct.instance<CompactCandidateDelegate>().view
                                .findViewHolderForAdapterPosition(0)!!.itemView.performClick()
                        }
                        delay(150)
                        check(main { chat.text.toString() } == beforeCandidate) { "Candidate selection bypassed the serial queue" }
                        main {
                            other.setText("")
                            other.requestFocus()
                        }
                        until("focus changes while a candidate click is queued") { service.currentInputEditorInfo?.fieldId == other.id }
                    } finally {
                        candidateRelease.complete(Unit)
                    }
                    api { check(getRawInput().isEmpty()) }
                    check(main { other.text.isEmpty() && chat.text.toString() == beforeCandidate }) { "Queued candidate leaked across editors" }
                    focus(chat)

                    val entered = CompletableDeferred<Unit>()
                    val release = CompletableDeferred<Unit>()
                    main {
                        service.postRimeJob {
                            entered.complete(Unit)
                            release.await()
                            simulateKeySequence("STALE{Return}")
                        }
                    }
                    entered.await()
                    main {
                        other.setText("")
                        other.requestFocus()
                    }
                    until("focus changes while an old engine operation is suspended") { service.currentInputEditorInfo?.fieldId == other.id }
                    release.complete(Unit)
                    api { }
                    check(main { other.text.isEmpty() }) { "Old editor output leaked to the new editor" }
                    focus(chat)
                    api { check(getRawInput().isEmpty()) }

                    val heldKey = main { descendants(checkNotNull(input())).filterIsInstance<KeyView>().first() }
                    val down = SystemClock.uptimeMillis()
                    main {
                        MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, heldKey.width / 2f, heldKey.height / 2f, 0).let {
                            heldKey.dispatchTouchEvent(it)
                            it.recycle()
                        }
                        other.setText("")
                    }
                    focus(other)
                    main {
                        MotionEvent.obtain(down, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, heldKey.width / 2f, heldKey.height / 2f, 0).let {
                            heldKey.dispatchTouchEvent(it)
                            it.recycle()
                        }
                    }
                    api { }
                    check(main { other.text.isEmpty() }) { "Cancelled gesture clicked into the next editor" }
                    focus(chat)

                    // Rime's own include/patch resolver creates a real third-party theme without T9.
                    val id = "editor-audit-${android.os.Process.myPid()}.trime"
                    fixture = File(DataManager.userDataDir, "$id.yaml").also {
                        check(it.createNewFile())
                        it.writeText(
                            """
                            config_version: '1'
                            __include: trime:/
                            __patch:
                              name: Editor audit without nine-key layout
                              preset_keyboards/luna_pinyin_t9: null
                              preset_keyboards/number: null
                              preset_keyboards/letter: null
                            """.trimIndent(),
                        )
                    }
                    ThemeManager.selectTheme(id)
                    check(ThemeManager.prefs.selectedTheme.getValue() == id)
                    check("luna_pinyin_t9" !in ThemeManager.activeTheme.presetKeyboards)
                    until("third-party theme uses compatible T9") { input() != null && keyboard().isT9Layout }
                    val beforeRestart = main {
                        val token = service.editorToken
                        chat.setText("")
                        (editor.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).restartInput(chat)
                        token
                    }
                    until("editor-owned reset has restarted the connection") { service.editorToken != beforeRestart && service.currentInputEditorInfo?.fieldId == chat.id }
                    api { }
                    type("64")
                    api {
                        check(getRawInput() == "64") { "Compatible T9 raw='${getRawInput()}', ASCII=${statusCached.isAsciiMode}, schema=${selectedSchemaId()}" }
                        check(t9Cached.enabled)
                        selectCandidate(0, false)
                    }
                    until("compatible T9 produces Hanzi") { chat.text.any { it.code > 127 } }
                    val beforeRedeploy = main { chat.text.toString() }
                    api { updateConfig() }
                    type("64")
                    api {
                        check(getRawInput() == "64")
                        selectCandidate(0, false)
                    }
                    until("in-place redeploy retains editor ownership") { chat.text.length > beforeRedeploy.length }
                    main {
                        other.inputType = InputType.TYPE_CLASS_NUMBER
                        other.setImeActionLabel(null, 0)
                    }
                    focus(other)
                    api { check(statusCached.isAsciiMode) }
                    check(!main { keyboard().isT9Layout })
                    focus(chat)
                    api { check(!statusCached.isAsciiMode) }
                    check(main { keyboard().isT9Layout })

                    val webReady = CompletableDeferred<Unit>()
                    web = main {
                        WebView(editor).apply {
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView, url: String) {
                                    webReady.complete(Unit)
                                }
                            }
                            @SuppressLint("SetJavaScriptEnabled")
                            settings.javaScriptEnabled = true
                            container.addView(this, LinearLayout.LayoutParams(-1, 150))
                            loadDataWithBaseURL("https://example.invalid/", "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head><body><input id='field' type='email' style='width:200px;height:32px'></body></html>", "text/html", "UTF-8", null)
                        }
                    }
                    webReady.await()
                    main {
                        checkNotNull(web).requestFocus()
                        checkNotNull(web).evaluateJavascript("document.getElementById('field').focus()", null)
                    }
                    val webPoint = main {
                        val location = IntArray(2)
                        checkNotNull(web).getLocationOnScreen(location)
                        val density = editor.resources.displayMetrics.density
                        (location[0] + 30 * density) to (location[1] + 20 * density)
                    }
                    val webDown = SystemClock.uptimeMillis()
                    for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                        MotionEvent.obtain(webDown, SystemClock.uptimeMillis(), action, webPoint.first, webPoint.second, 0).let {
                            instrumentation.sendPointerSync(it)
                            it.recycle()
                        }
                    }
                    until("real WebView email InputConnection") { service.currentInputEditorInfo?.inputType?.and(InputType.TYPE_MASK_VARIATION) in setOf(InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS, InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) }
                    api { check(statusCached.isAsciiMode) }
                    type("12")
                    api { }
                    val webValue = CompletableDeferred<String>()
                    main { checkNotNull(web).evaluateJavascript("document.getElementById('field').value") { webValue.complete(it) } }
                    check(webValue.await() == "\"12\"")
                    focus(chat)
                    api { check(!statusCached.isAsciiMode) }
                    main {
                        container.removeView(web)
                        web?.destroy()
                        web = null
                    }

                    val baseline = main { chat.text.toString() }
                    val imm = editor.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    repeat(100) { index ->
                        main { imm.hideSoftInputFromWindow(chat.windowToken, 0) }
                        withTimeout(10_000) { while (main { service.isInputViewShown }) delay(40) }
                        main {
                            chat.requestFocus()
                            imm.showSoftInput(chat, 0)
                        }
                        withTimeout(10_000) { while (!main { service.isInputViewShown && input() != null && keyboard().isT9Layout }) delay(40) }
                        api {
                            check(!statusCached.isAsciiMode)
                            check(getRawInput().isEmpty())
                        }
                        check(main { chat.text.toString() } == baseline)
                        if ((index + 1) % 10 == 0) phase("PASS: ${index + 1}/100 show-hide cycles, T9 and editor text retained")
                    }
                    type("64426")
                    api { check(getRawInput() == "64426") }
                    until("pinyin choices still update after 100 visibility cycles") {
                        input()?.let { view ->
                            descendants(view).filterIsInstance<T9DisambiguationView>().flatMap(::descendants)
                                .filterIsInstance<TextView>().any { it.text.toString() == "ni" && it.isShown }
                        } == true
                    }
                    main {
                        descendants(checkNotNull(input())).filterIsInstance<T9DisambiguationView>().flatMap(::descendants)
                            .filterIsInstance<TextView>().first { it.text.toString() == "ni" && it.isShown }.performClick()
                    }
                    api {
                        check(getRawInput() == "64426")
                        check(t9Cached.segments.any { it.locked && it.spelling == "ni" })
                    }
                    main { imm.hideSoftInputFromWindow(chat.windowToken, 0) }
                    withTimeout(10_000) { while (main { service.isInputViewShown }) delay(40) }
                    main { imm.showSoftInput(chat, 0) }
                    until("locked composition survives hide and show") { service.isInputViewShown && input() != null }
                    api {
                        check(getRawInput() == "64426")
                        check(t9Cached.segments.any { it.locked && it.spelling == "ni" })
                        clearComposition()
                    }
                    until("cancelling after 100 cycles preserves committed text") { chat.text.toString() == baseline }
                    val screenshot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
                    try {
                        File(context.cacheDir, "editor-lifecycle.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    } finally {
                        screenshot.recycle()
                    }
                }
            }
            result.putString("stream", "PASS: input-field matrix, editor actions, stale output rejection, third-party T9 fallback, real WebView and 100 show/hide cycles\n")
            passed = true
        } catch (failure: Throwable) {
            result.putString("stream", "FAIL: ${failure.stackTraceToString()}\n")
        } finally {
            main {
                candidatesMode.setValue(originalCandidatesMode)
                web?.destroy()
                activity?.finish()
            }
            runBlocking {
                runCatching {
                    ThemeManager.selectTheme(originalTheme)
                    session?.runOnReady {
                        clearComposition()
                        if (originalSchema.isNotEmpty()) selectSchema(originalSchema)
                        setRuntimeOption("ascii_mode", originalAscii)
                    }
                }
            }
            fixture?.let { file ->
                file.delete()
                File(DataManager.stagingDir, file.name).delete()
            }
            main { RimeDaemon.destroySession(javaClass.name) }
            check(ThemeManager.prefs.selectedTheme.sharedPreferences.edit().commit())
        }
        result.putBoolean("passed", passed)
        instrumentation.finish(if (passed) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
    }
}
