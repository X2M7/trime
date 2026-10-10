/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Region
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.inspector.WindowInspector
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.osfans.trime.core.Rime
import com.osfans.trime.core.RimeApi
import com.osfans.trime.core.RimeSchema
import com.osfans.trime.daemon.RimeDaemon
import com.osfans.trime.daemon.RimeSession
import com.osfans.trime.data.base.DataManager
import com.osfans.trime.data.prefs.AppPrefs
import com.osfans.trime.data.theme.ThemeManager
import com.osfans.trime.data.theme.model.KeyActionToken
import com.osfans.trime.ime.broadcast.EnterKeyDisplayDelegate
import com.osfans.trime.ime.candidates.CandidateViewHolder
import com.osfans.trime.ime.candidates.compact.CompactCandidateDelegate
import com.osfans.trime.ime.candidates.popup.PagedCandidatesUi
import com.osfans.trime.ime.candidates.popup.PopupCandidatesMode
import com.osfans.trime.ime.composition.CandidatesView
import com.osfans.trime.ime.composition.PreeditDelegate
import com.osfans.trime.ime.composition.T9DisambiguationView
import com.osfans.trime.ime.core.AutoScaleTextView
import com.osfans.trime.ime.core.EditorPolicy
import com.osfans.trime.ime.core.InputView
import com.osfans.trime.ime.core.PrivacyModeState
import com.osfans.trime.ime.core.TrimeInputMethodService
import com.osfans.trime.ime.keyboard.CommonKeyboardActionListener
import com.osfans.trime.ime.keyboard.InputFeedbackManager
import com.osfans.trime.ime.keyboard.KeyAction
import com.osfans.trime.ime.keyboard.KeyBehavior
import com.osfans.trime.ime.keyboard.KeyView
import com.osfans.trime.ime.keyboard.KeyboardWindow
import com.osfans.trime.ime.switches.SwitchOptionAdapter
import com.osfans.trime.ime.switches.SwitchOptionEntry
import com.osfans.trime.ime.switches.SwitchOptionWindow
import com.osfans.trime.ui.main.EditorProbeActivity
import com.osfans.trime.util.sp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.kodein.di.direct
import org.kodein.di.instance
import java.io.File
import java.security.MessageDigest
import java.util.UUID

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

    /** Check the actual native 64 row, including pixels captured before software drawing. */
    private fun verifyCandidateGlyphs(instrumentation: Instrumentation, service: TrimeInputMethodService, theme: String) {
        fun <T> main(block: () -> T): T {
            var value: Result<T>? = null
            instrumentation.runOnMainSync { value = runCatching(block) }
            return checkNotNull(value).getOrThrow()
        }
        fun sha256(file: File) = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        fun colorDistance(first: Int, second: Int) = kotlin.math.abs(Color.red(first) - Color.red(second)) +
            kotlin.math.abs(Color.green(first) - Color.green(second)) +
            kotlin.math.abs(Color.blue(first) - Color.blue(second))

        val directory = File(instrumentation.targetContext.filesDir, "runtime-audit/candidate-glyph/$theme")
        check(directory.isDirectory || directory.mkdirs())
        val ownedNames = listOf("screen.png", "label-0.png", "label-1.png", "report.json")
        ownedNames.forEach { name ->
            val file = File(directory, name)
            check(!file.exists() || (file.isFile && file.delete())) { "Cannot clear owned candidate evidence: $name" }
        }
        val runId = UUID.randomUUID().toString()
        instrumentation.sendStatus(
            0,
            Bundle().apply {
                putString("candidate_glyph_case", theme)
                putString("candidate_glyph_run_id", runId)
            },
        )
        val records = JSONArray()
        val nativeState = JSONObject()
        var passed = false
        var failure = ""
        var screenshot: Bitmap? = null
        try {
            val (delegate, rime) = main {
                val input = roots().asSequence().flatMap(::descendants).filterIsInstance<InputView>().first { it.isShown }
                input.di.direct.instance<CompactCandidateDelegate>() to input.di.direct.instance<RimeSession>()
            }
            val holder = main {
                checkNotNull(delegate.view.findViewHolderForAdapterPosition(0) as? CandidateViewHolder)
            }
            val snapshot = main { holder.snapshot }
            fun checkCurrentSnapshot() = main {
                check(service.isCurrentEditor(snapshot.editorToken) && delegate.adapter.snapshot == snapshot && holder.snapshot == snapshot) {
                    "Editor or bound candidate changed during the pixel capture"
                }
                rime.run {
                    check(candidateSnapshotCached == snapshot) { "Bound candidate is older than the current native snapshot" }
                    val raw = t9Cached.input
                    val schema = statusCached.schemaId
                    check(t9Cached.enabled && raw == "64" && schema == "luna_pinyin_t9") { "Candidate capture no longer owns native T9 64" }
                    nativeState.put("editor", snapshot.editorToken)
                    nativeState.put("generation", snapshot.generation)
                    nativeState.put("request", snapshot.requestId)
                    nativeState.put("raw_input", raw)
                    nativeState.put("raw_input_source", "t9Cached.input")
                    nativeState.put("schema_id", schema)
                    check(candidateSnapshotCached == snapshot) { "Native snapshot advanced while reading its cached metadata" }
                }
            }
            val expected = main { listOf(holder.text, holder.comment).filter { it.isNotEmpty() } }
            check(expected.firstOrNull()?.any { it in '\u3400'..'\u9fff' } == true) { "64 did not produce a native Hanzi candidate" }
            val labels = main {
                descendants(holder.itemView).filterIsInstance<AutoScaleTextView>()
                    .filter { it.isShown && it.text.isNotEmpty() }.toList()
            }
            check(labels.size == expected.size && main { labels.map { it.text.toString() } } == expected) {
                "Live candidate labels do not match their native holder"
            }
            // A software draw can invalidate cached framework state. Capture hardware pixels first.
            checkCurrentSnapshot()
            nativeState.put("capture_start_ns", SystemClock.elapsedRealtimeNanos())
            val screen = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            nativeState.put("capture_end_ns", SystemClock.elapsedRealtimeNanos())
            screenshot = screen
            File(directory, "screen.png").outputStream().use {
                check(screen.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
            checkCurrentSnapshot()
            labels.forEachIndexed { index, view ->
                val (software, record) = main {
                    val node = view.createAccessibilityNodeInfo()
                    val accessibleText = try {
                        node.text?.toString().orEmpty()
                    } finally {
                        @Suppress("DEPRECATION")
                        node.recycle()
                    }
                    val point = IntArray(2)
                    view.getLocationOnScreen(point)
                    val bounds = Rect(point[0], point[1], point[0] + view.width, point[1] + view.height)
                    check(
                        view.width > 0 && view.height > 0 && bounds.left >= 0 && bounds.top >= 0 &&
                            bounds.right <= screen.width && bounds.bottom <= screen.height,
                    )
                    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.translate(-view.scrollX.toFloat(), -view.scrollY.toFloat())
                    view.draw(canvas)
                    val color = view.currentTextColor
                    val background = screen.getPixel(bounds.left, bounds.top)
                    var softwareInk = 0
                    var screenInk = 0
                    for (y in 0 until view.height) {
                        for (x in 0 until view.width) {
                            val softwarePixel = bitmap.getPixel(x, y)
                            if (Color.alpha(softwarePixel) >= 128 && colorDistance(softwarePixel, color) <= 54) softwareInk++
                            val pixel = screen.getPixel(bounds.left + x, bounds.top + y)
                            if (colorDistance(pixel, color) <= 54 && colorDistance(pixel, background) > 54) screenInk++
                        }
                    }
                    bitmap to JSONObject().apply {
                        put("text", view.text.toString())
                        put("accessibility_text", accessibleText)
                        put("screen_bounds", JSONArray(listOf(bounds.left, bounds.top, bounds.right, bounds.bottom)))
                        put("text_color_argb", color.toLong() and 0xffffffffL)
                        put("text_size_px", view.textSize)
                        put("text_scale_x", view.textScaleX)
                        put("baseline_px", view.baseline)
                        put("scroll_x_px", view.scrollX)
                        put("scroll_y_px", view.scrollY)
                        put("software_ink_pixels", softwareInk)
                        put("screen_ink_pixels", screenInk)
                        put("required_screen_ink_pixels", (softwareInk / 4).coerceAtLeast(1))
                        put("accessibility_passed", accessibleText == expected[index])
                        put("glyph_passed", Color.alpha(color) > 8 && softwareInk > 0 && screenInk >= (softwareInk / 4).coerceAtLeast(1))
                    }
                }
                try {
                    File(directory, "label-$index.png").outputStream().use {
                        check(software.compress(Bitmap.CompressFormat.PNG, 100, it))
                    }
                } finally {
                    software.recycle()
                }
                records.put(record)
                check(record.getBoolean("accessibility_passed")) { "Candidate label has no matching Unicode accessibility text: $record" }
                check(record.getBoolean("glyph_passed")) { "Candidate label has no visible screen glyph: $record" }
            }
            passed = true
        } catch (error: Throwable) {
            failure = error.stackTraceToString()
            throw error
        } finally {
            screenshot?.recycle()
            val attachments = JSONObject()
            ownedNames.filter { it != "report.json" }.forEach { name ->
                File(directory, name).takeIf { it.isFile }?.let { attachments.put(name, sha256(it)) }
            }
            File(directory, "report.json").writeText(
                JSONObject().apply {
                    put("run_id", runId)
                    put("theme", theme)
                    put("fixture", "native T9 64")
                    put("native_state", nativeState)
                    put("hardware_capture_before_software_draw", true)
                    put("labels", records)
                    put("passed", passed)
                    put("error", failure)
                    put("attachments_sha256", attachments)
                }.toString(2) + "\n",
            )
        }
        instrumentation.sendStatus(0, Bundle().apply { putString("stream", "PASS: $theme native 64 candidate Unicode accessibility and actual screen glyphs\n") })
    }

    private suspend fun verifyQueuedModeRestoreAcrossThemeReload(
        instrumentation: Instrumentation,
        service: TrimeInputMethodService,
        inputManager: InputMethodManager,
        chat: EditText,
        themeId: String,
    ) {
        fun <T> main(block: () -> T): T {
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) return block()
            var value: Result<T>? = null
            instrumentation.runOnMainSync { value = runCatching(block) }
            return checkNotNull(value).getOrThrow()
        }
        fun input(): InputView? = roots().asSequence().flatMap(::descendants).filterIsInstance<InputView>().firstOrNull { it.isShown }
        fun keyboard() = checkNotNull(input()).di.direct.instance<KeyboardWindow>().currentKeyboard
        suspend fun until(label: String, predicate: () -> Boolean) {
            withTimeout(30_000) { while (!main(predicate)) delay(50) }
            instrumentation.sendStatus(0, Bundle().apply { putString("stream", "PASS: $label\n") })
        }
        suspend fun api(block: suspend RimeApi.() -> Unit) {
            var outcome: Result<Unit>? = null
            val job = main { service.postRimeJob { outcome = runCatching { block() } } }
            job.join()
            check(!job.isCancelled) { "Engine/editor job cancelled" }
            checkNotNull(outcome) { "Editor changed before operation ran" }.getOrThrow()
            main { }
        }

        // Hold the native queue after the restricted editor has entered ASCII mode. Returning to
        // chat queues its restore while a deployment-style theme rebuild creates another
        // KeyboardWindow from the still-ASCII native cache. The restore snapshot must survive the
        // rebuild so both windows queue the same final Chinese mode.
        val restoreQueueEntered = CompletableDeferred<Unit>()
        val restoreQueueRelease = CompletableDeferred<Unit>()
        val restoreQueueGate = main {
            service.postRimeJob {
                restoreQueueEntered.complete(Unit)
                restoreQueueRelease.await()
            }
        }
        val inputViewBeforeBlockedReload = main { checkNotNull(input()) }
        try {
            withTimeout(10_000) { restoreQueueEntered.await() }
            main {
                chat.requestFocus()
                inputManager.showSoftInput(chat, 0)
            }
            until("editor chat owns connection while mode restore is queued") {
                service.currentInputEditorInfo?.fieldId == chat.id && input() != null
            }
            until("queued chat restore selects T9 layout") {
                input() != null && keyboard().isT9Layout
            }
            ThemeManager.selectTheme(themeId, forceReload = true)
            until("forced theme reload overlaps the queued editor restore") {
                input()?.let { it !== inputViewBeforeBlockedReload && keyboard().isT9Layout } == true
            }
        } finally {
            restoreQueueRelease.complete(Unit)
        }
        withTimeout(10_000) { restoreQueueGate.join() }
        check(!restoreQueueGate.isCancelled) { "Mode-restore queue gate was cancelled" }
        api {
            check(!statusCached.isAsciiMode) { "Theme reload overwrote the queued Chinese-mode restore" }
        }
        check(main { service.editorModeOverride == null }) { "Completed editor-mode restore left a stale snapshot" }
        instrumentation.sendStatus(
            0,
            Bundle().apply { putString("stream", "PASS: theme reload preserves a queued restricted-editor mode restore\n") },
        )
    }

    private suspend fun verifyQueuedModeRestoreAcrossPrivacyChange(
        instrumentation: Instrumentation,
        service: TrimeInputMethodService,
        inputManager: InputMethodManager,
        restrictedEditor: EditText,
        chat: EditText,
    ) {
        fun <T> main(block: () -> T): T {
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) return block()
            var value: Result<T>? = null
            instrumentation.runOnMainSync { value = runCatching(block) }
            return checkNotNull(value).getOrThrow()
        }
        fun input(): InputView? = roots().asSequence().flatMap(::descendants).filterIsInstance<InputView>().firstOrNull { it.isShown }
        fun keyboard() = checkNotNull(input()).di.direct.instance<KeyboardWindow>().currentKeyboard
        suspend fun until(label: String, predicate: () -> Boolean) {
            withTimeout(30_000) { while (!main(predicate)) delay(50) }
            instrumentation.sendStatus(0, Bundle().apply { putString("stream", "PASS: $label\n") })
        }
        suspend fun api(block: suspend RimeApi.() -> Unit) {
            var outcome: Result<Unit>? = null
            val job = main { service.postRimeJob { outcome = runCatching { block() } } }
            job.join()
            check(!job.isCancelled) { "Engine/editor job cancelled" }
            checkNotNull(outcome) { "Editor changed before operation ran" }.getOrThrow()
            main { }
        }

        val manualPrivacyMode = AppPrefs.defaultInstance().general.manualPrivacyMode
        check(!manualPrivacyMode.getValue()) { "Queued privacy-boundary fixture requires ordinary mode" }
        main {
            restrictedEditor.requestFocus()
            inputManager.showSoftInput(restrictedEditor, 0)
        }
        until("privacy-boundary number editor owns connection") {
            service.currentInputEditorInfo?.fieldId == restrictedEditor.id && input() != null
        }
        until("privacy-boundary number editor applies its ASCII layout") {
            input() != null && !keyboard().isT9Layout
        }
        api { check(statusCached.isAsciiMode) { "Privacy-boundary fixture did not enter ASCII mode" } }

        val restoreQueueEntered = CompletableDeferred<Unit>()
        val restoreQueueRelease = CompletableDeferred<Unit>()
        val restoreQueueGate = main {
            service.postRimeJob {
                restoreQueueEntered.complete(Unit)
                restoreQueueRelease.await()
            }
        }
        var privacyEnabled = false
        try {
            withTimeout(10_000) { restoreQueueEntered.await() }
            main {
                chat.requestFocus()
                inputManager.showSoftInput(chat, 0)
            }
            until("editor chat owns connection before queued privacy change") {
                service.currentInputEditorInfo?.fieldId == chat.id && input() != null
            }
            until("queued privacy-boundary restore selects T9 layout") {
                input() != null && keyboard().isT9Layout
            }
            check(main { service.editorModeOverride != null }) {
                "Queued privacy-boundary restore lost its restricted-editor snapshot before replay"
            }
            main { manualPrivacyMode.setValue(true) }
            privacyEnabled = true
            until("manual privacy supersedes the queued editor generation") {
                service.isPrivacyModeActive && PrivacyModeState.active
            }
            restoreQueueRelease.complete(Unit)
            withTimeout(10_000) { restoreQueueGate.join() }
            check(!restoreQueueGate.isCancelled) { "Privacy-boundary queue gate was cancelled" }
            api {
                check(Rime.isRimePrivacyGuardEnabledForTesting()) {
                    "Queued privacy change did not bind the native privacy policy"
                }
                check(!statusCached.isAsciiMode) {
                    "Privacy change discarded the queued normal-editor mode restore"
                }
            }
            check(main { service.editorModeOverride == null }) {
                "Privacy change left a stale restricted-editor mode snapshot"
            }
        } finally {
            restoreQueueRelease.complete(Unit)
            if (privacyEnabled) main { manualPrivacyMode.setValue(false) }
        }
        until("ordinary mode returns after queued privacy change") {
            !service.isPrivacyModeActive && !PrivacyModeState.active
        }
        api {
            check(!Rime.isRimePrivacyGuardEnabledForTesting())
            check(!statusCached.isAsciiMode) { "Privacy exit lost the normal editor's Chinese mode" }
        }
        instrumentation.sendStatus(
            0,
            Bundle().apply {
                putString("stream", "PASS: privacy changes preserve a queued restricted-editor mode restore\n")
            },
        )
    }

    private suspend fun verifyHardwareEditorPolicies(
        instrumentation: Instrumentation,
        service: TrimeInputMethodService,
        editor: Activity,
        chat: EditText,
        other: EditText,
    ) {
        fun <T> main(block: () -> T): T {
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) return block()
            var value: Result<T>? = null
            instrumentation.runOnMainSync { value = runCatching(block) }
            return checkNotNull(value).getOrThrow()
        }
        fun input(): InputView? = roots().asSequence().flatMap(::descendants).filterIsInstance<InputView>().firstOrNull { it.isShown }
        fun keyboard() = checkNotNull(input()).di.direct.instance<KeyboardWindow>().currentKeyboard
        suspend fun until(label: String, predicate: () -> Boolean) {
            withTimeout(30_000) { while (!main(predicate)) delay(50) }
            instrumentation.sendStatus(0, Bundle().apply { putString("stream", "PASS: $label\n") })
        }
        suspend fun api(block: suspend RimeApi.() -> Unit) {
            var outcome: Result<Unit>? = null
            val job = main { service.postRimeJob { outcome = runCatching { block() } } }
            job.join()
            check(!job.isCancelled) { "Engine/editor job cancelled" }
            checkNotNull(outcome) { "Editor changed before operation ran" }.getOrThrow()
            main { }
        }
        fun speechAllowed(): Boolean = InputFeedbackManager::class.java
            .getDeclaredField("speechAllowed")
            .apply { isAccessible = true }
            .getBoolean(InputFeedbackManager)
        val inputManager = editor.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        suspend fun focusVirtual(field: EditText) {
            main {
                field.requestFocus()
                inputManager.showSoftInput(field, 0)
            }
            until("virtual editor ${field.hint} owns connection") {
                service.currentInputEditorInfo?.fieldId == field.id && input() != null
            }
        }
        suspend fun focusPhysical(field: EditText) {
            main { field.requestFocus() }
            until("physical editor ${field.hint} owns connection") {
                service.currentInputEditorInfo?.fieldId == field.id
            }
            api { }
        }

        val candidatesMode = AppPrefs.defaultInstance().candidates.mode
        val originalCandidatesMode = candidatesMode.getValue()
        val manualPrivacyMode = AppPrefs.defaultInstance().general.manualPrivacyMode
        check(!manualPrivacyMode.getValue()) { "Hardware policy fixture requires ordinary mode" }
        try {
            main {
                other.hint = "soft-number-before-hardware"
                other.inputType = InputType.TYPE_CLASS_NUMBER
                other.setText("")
            }
            focusVirtual(other)
            val keyboardWindow = main { checkNotNull(input()).di.direct.instance<KeyboardWindow>() }
            val restrictedEditorField = KeyboardWindow::class.java.getDeclaredField("restrictedEditor").apply { isAccessible = true }
            until("soft number establishes its restricted layout before hardware input") {
                input() != null && !keyboard().isT9Layout
            }
            check(main { restrictedEditorField.getBoolean(keyboardWindow) }) {
                "Soft number did not establish a restricted keyboard policy"
            }
            api { check(statusCached.isAsciiMode) }
            check(main { service.editorModeOverride != null }) {
                "Soft number did not retain the preceding editor mode"
            }

            main { candidatesMode.setValue(PopupCandidatesMode.INPUT_DEVICE) }
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_6)
            until("physical keyboard hides the virtual keyboard") {
                service.isInputViewShown && input() == null
            }
            focusPhysical(chat)
            api {
                check(!statusCached.isAsciiMode) {
                    "Hardware chat did not restore the mode saved before soft number input"
                }
                check(getRawInput().isEmpty())
            }
            check(main { service.editorModeOverride == null }) {
                "Hardware chat left a stale soft-number mode snapshot"
            }
            main {
                @Suppress("DEPRECATION")
                service.onViewClicked(false)
            }
            until("same hardware chat restores its virtual T9 keyboard") {
                input() != null && keyboard().isT9Layout
            }
            check(!main { restrictedEditorField.getBoolean(keyboardWindow) }) {
                "Virtual chat retained the hidden number editor policy"
            }
            api {
                check(!statusCached.isAsciiMode) {
                    "Virtual chat returned from hardware input with a stale ASCII mode"
                }
            }
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_6)
            until("physical key hides the restored virtual keyboard") { input() == null }
            api {
                check(getRawInput() == "6")
                clearComposition()
                setRuntimeOption("ascii_mode", true)
            }
            main {
                @Suppress("DEPRECATION")
                service.onViewClicked(false)
            }
            until("same hardware chat returns after an explicit hardware mode change") {
                input() != null && keyboard().isT9Layout
            }
            api {
                check(statusCached.isAsciiMode) {
                    "Virtual chat discarded the mode selected during hardware input"
                }
                setRuntimeOption("ascii_mode", false)
            }
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_6)
            until("physical key hides the mode-restored virtual keyboard") { input() == null }
            api {
                check(getRawInput() == "6")
                clearComposition()
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
                val expectedPrivate = "password" in name
                check(service.isPrivacyModeActive == expectedPrivate) { "$name privacy state mismatch" }
                check(PrivacyModeState.active == expectedPrivate) { "$name process privacy state mismatch" }
                check(speechAllowed() != expectedPrivate) { "$name speech privacy state mismatch" }
                api { check(Rime.isRimePrivacyGuardEnabledForTesting() == expectedPrivate) }
                api { check(!statusCached.isAsciiMode) { "Hardware test must exercise an unchanged Chinese engine mode" } }
                if (name == "hardware-number") {
                    main { manualPrivacyMode.setValue(true) }
                    until("manual privacy reaches the hardware number editor") {
                        service.isPrivacyModeActive && PrivacyModeState.active
                    }
                    api {
                        check(Rime.isRimePrivacyGuardEnabledForTesting())
                        check(!statusCached.isAsciiMode) {
                            "Privacy rebind applied a hidden virtual-keyboard policy to hardware input"
                        }
                    }
                    main { manualPrivacyMode.setValue(false) }
                    until("manual privacy leaves the hardware number editor") {
                        !service.isPrivacyModeActive && !PrivacyModeState.active
                    }
                    api {
                        check(!Rime.isRimePrivacyGuardEnabledForTesting())
                        check(!statusCached.isAsciiMode) {
                            "Privacy exit left hardware input in a hidden ASCII policy"
                        }
                        setRuntimeOption("ascii_mode", true)
                    }
                    main { manualPrivacyMode.setValue(true) }
                    until("second manual privacy change reaches the hardware number editor") {
                        service.isPrivacyModeActive && PrivacyModeState.active
                    }
                    api {
                        check(Rime.isRimePrivacyGuardEnabledForTesting())
                        check(statusCached.isAsciiMode) {
                            "Privacy rebind overwrote a hardware editor's explicit ASCII mode"
                        }
                    }
                    main { manualPrivacyMode.setValue(false) }
                    until("second manual privacy change leaves the hardware number editor") {
                        !service.isPrivacyModeActive && !PrivacyModeState.active
                    }
                    api {
                        check(!Rime.isRimePrivacyGuardEnabledForTesting())
                        check(statusCached.isAsciiMode) {
                            "Privacy exit overwrote a hardware editor's explicit ASCII mode"
                        }
                        setRuntimeOption("ascii_mode", false)
                    }
                }
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_6)
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_4)
                until("$name receives physical digits directly") { other.text.toString() == "64" }
                api { check(getRawInput().isEmpty()) { "$name leaked hardware input to Rime" } }
                focusPhysical(chat)
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_6)
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_4)
                api {
                    check(!statusCached.isAsciiMode && getRawInput() == "64") {
                        "Physical chat did not retain Chinese input after $name"
                    }
                    clearComposition()
                }
            }
        } finally {
            main {
                manualPrivacyMode.setValue(false)
                candidatesMode.setValue(originalCandidatesMode)
                @Suppress("DEPRECATION")
                service.onViewClicked(false)
                chat.requestFocus()
                inputManager.showSoftInput(chat, 0)
            }
        }
        until("virtual chat returns after hardware policy checks") {
            service.currentInputEditorInfo?.fieldId == chat.id &&
                input() != null &&
                keyboard().isT9Layout
        }
        api { check(!statusCached.isAsciiMode) }
        instrumentation.sendStatus(
            0,
            Bundle().apply { putString("stream", "PASS: hardware editors preserve and restore native input modes\n") },
        )
    }

    private suspend fun verifyTransientKeyboardAcrossPrivacyChange(
        instrumentation: Instrumentation,
        service: TrimeInputMethodService,
    ) {
        fun <T> main(block: () -> T): T {
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) return block()
            var value: Result<T>? = null
            instrumentation.runOnMainSync { value = runCatching(block) }
            return checkNotNull(value).getOrThrow()
        }
        fun input(): InputView? = roots().asSequence().flatMap(::descendants).filterIsInstance<InputView>().firstOrNull { it.isShown }
        suspend fun until(label: String, predicate: () -> Boolean) {
            withTimeout(30_000) { while (!main(predicate)) delay(50) }
            instrumentation.sendStatus(0, Bundle().apply { putString("stream", "PASS: $label\n") })
        }
        suspend fun api(block: suspend RimeApi.() -> Unit) {
            var outcome: Result<Unit>? = null
            val job = main { service.postRimeJob { outcome = runCatching { block() } } }
            job.join()
            check(!job.isCancelled) { "Engine/editor job cancelled" }
            checkNotNull(outcome) { "Editor changed before operation ran" }.getOrThrow()
            main { }
        }

        val manualPrivacyMode = AppPrefs.defaultInstance().general.manualPrivacyMode
        check(!manualPrivacyMode.getValue()) { "Transient keyboard fixture requires ordinary mode" }
        val window = main { checkNotNull(input()).di.direct.instance<KeyboardWindow>() }
        try {
            main { window.switchKeyboard("symbols") }
            until("manual privacy layout fixture selects a transient keyboard") {
                !window.currentKeyboard.isLock && !window.currentKeyboard.isT9Layout
            }
            val transientKeyboard = main { window.currentKeyboard }
            api {
                check(statusCached.isAsciiMode) {
                    "Transient symbols keyboard did not establish its native ASCII mode"
                }
            }
            main { manualPrivacyMode.setValue(true) }
            until("manual privacy reaches the transient keyboard") {
                service.isPrivacyModeActive && PrivacyModeState.active
            }
            api {
                check(Rime.isRimePrivacyGuardEnabledForTesting())
                check(statusCached.isAsciiMode) {
                    "Privacy entry changed the transient keyboard's native mode"
                }
            }
            check(main { window.currentKeyboard === transientKeyboard }) {
                "Privacy entry replaced the user's transient keyboard"
            }
            main { manualPrivacyMode.setValue(false) }
            until("manual privacy leaves the transient keyboard") {
                !service.isPrivacyModeActive && !PrivacyModeState.active
            }
            api {
                check(!Rime.isRimePrivacyGuardEnabledForTesting())
                check(statusCached.isAsciiMode) {
                    "Privacy exit changed the transient keyboard's native mode"
                }
            }
            check(main { window.currentKeyboard === transientKeyboard }) {
                "Privacy exit replaced the user's transient keyboard"
            }
        } finally {
            main {
                manualPrivacyMode.setValue(false)
                window.switchKeyboard(".last_lock")
            }
        }
        until("manual privacy layout fixture restores T9") { window.currentKeyboard.isT9Layout }
        api { check(!statusCached.isAsciiMode) }
        instrumentation.sendStatus(
            0,
            Bundle().apply { putString("stream", "PASS: manual privacy preserves the user's transient keyboard\n") },
        )
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
        val manualPrivacyMode = AppPrefs.defaultInstance().general.manualPrivacyMode
        val originalManualPrivacyMode = manualPrivacyMode.getValue()
        var originalSchema = ""
        var originalAscii = false
        var fixture: File? = null
        val touchRegionRunId = UUID.randomUUID().toString()
        val touchRegionMeasurements = JSONArray()
        fun touchStage(stage: String, theme: String, round: Int = -1, elapsedMs: Long = -1) {
            val marker = "TOUCH_REGION run=$touchRegionRunId stage=$stage theme=$theme round=$round elapsed_ms=$elapsedMs uptime_ms=${SystemClock.uptimeMillis()}"
            Log.i("TrimeProbe", marker)
            instrumentation.sendStatus(0, Bundle().apply { putString("touch_region_phase", marker) })
        }
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
            main { manualPrivacyMode.setValue(false) }
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
            val editor = instrumentation.startActivitySync(Intent(context, EditorProbeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            activity = editor
            val actions = mutableListOf<Int>()
            val fields = main {
                val row = LinearLayout(editor).apply { orientation = LinearLayout.VERTICAL }
                // The fixture owns these editors in the same PID as the IME. Keep Android's
                // platform SpellChecker out of the runtime log while preserving the exact
                // EditorInfo inputType values under test.
                val chat = object : EditText(editor) {
                    override fun isSuggestionsEnabled(): Boolean = false
                }.apply {
                    id = View.generateViewId()
                    hint = "chat"
                    inputType = InputType.TYPE_CLASS_TEXT
                    imeOptions = EditorInfo.IME_ACTION_SEND
                }
                val other = object : EditText(editor) {
                    override fun isSuggestionsEnabled(): Boolean = false
                }.apply {
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
                // The debug-only, non-floating host keeps a real application surface beside
                // the preedit. adjustNothing leaves that surface available beneath the IME.
                editor.window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                chat.requestFocus()
                Triple(chat, other, row)
            }
            val (chat, other, container) = fields
            runBlocking {
                withTimeout(900_000) {
                    suspend fun until(label: String, predicate: () -> Boolean) {
                        withTimeout(30_000) { while (!main(predicate)) delay(50) }
                        phase("PASS: $label")
                    }
                    fun input(): InputView? = roots().asSequence().flatMap(::descendants).filterIsInstance<InputView>().firstOrNull { it.isShown }
                    val inputManager = editor.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    // Window focus and the served editor are established after setContentView.
                    // On API 21 this first shows LatinIME, before the runner switches to Trime.
                    until("initial editor attached, laid out and served") {
                        chat.isAttachedToWindow && chat.isLaidOut && chat.width > 0 && chat.height > 0 &&
                            chat.hasWindowFocus() && chat.hasFocus() && inputManager.isActive(chat)
                    }
                    main { inputManager.showSoftInput(chat, 0) }
                    until("initial keyboard visible") { input() != null }
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
                    fun enter() = main { checkNotNull(input()).di.direct.instance<CommonKeyboardActionListener>().listener.onAction(KeyAction(KeyActionToken.Plain("Return"), ThemeManager.activeTheme.presetKeys)) }
                    fun speechAllowed(): Boolean = InputFeedbackManager::class.java
                        .getDeclaredField("speechAllowed")
                        .apply { isAccessible = true }
                        .getBoolean(InputFeedbackManager)
                    suspend fun focus(field: EditText) {
                        main {
                            field.requestFocus()
                            (editor.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(field, 0)
                        }
                        until("editor ${field.hint} owns connection") { service.currentInputEditorInfo?.fieldId == field.id && input() != null }
                        api { }
                    }
                    api { clearComposition() }
                    EngineUnavailableGlyphProbe.verify(instrumentation, service, chat)
                    PopupColorRegression.verify(instrumentation, main { checkNotNull(input()) }, chat)
                    api {
                        check(selectSchema("luna_pinyin_t9"))
                        setRuntimeOption("ascii_mode", false)
                    }
                    until("selected T9 is actually a nine-key layout") { input() != null && keyboard().isT9Layout }
                    fun touchInsets() = InputMethodService.Insets().also(service::onComputeInsets)
                    fun visibleWindowBounds(view: View): Rect {
                        val bounds = Rect()
                        if (!view.isAttachedToWindow || !view.isShown || !view.getLocalVisibleRect(bounds)) return bounds.apply { setEmpty() }
                        val location = IntArray(2)
                        view.getLocationInWindow(location)
                        bounds.offset(location[0], location[1])
                        return bounds
                    }
                    fun regionCovers(view: View): Boolean {
                        val bounds = visibleWindowBounds(view)
                        return !bounds.isEmpty && Region(bounds).apply {
                            op(touchInsets().touchableRegion, Region.Op.DIFFERENCE)
                        }.isEmpty
                    }
                    fun checkNoTouchPopup() {
                        check(
                            roots().none { root ->
                                (root.layoutParams as? WindowManager.LayoutParams)?.title?.toString()?.startsWith("PopupWindow:") == true
                            },
                        ) { "Composition must not create a separate PopupWindow surface" }
                    }
                    fun tapActualView(view: View, leadingText: Boolean = false, imeTarget: Boolean = true) {
                        val point = main {
                            if (imeTarget) check(regionCovers(view))
                            val location = IntArray(2)
                            val bounds = Rect()
                            check(view.getLocalVisibleRect(bounds))
                            view.getLocationOnScreen(location)
                            val x = if (leadingText) view.paddingLeft + 1 else bounds.centerX()
                            val y = bounds.centerY()
                            check(bounds.contains(x, y))
                            if (!imeTarget) {
                                val decor = checkNotNull(service.window.window).decorView
                                val decorScreen = IntArray(2).also(decor::getLocationOnScreen)
                                val decorWindow = IntArray(2).also(decor::getLocationInWindow)
                                check(
                                    !touchInsets().touchableRegion.contains(
                                        location[0] + x - decorScreen[0] + decorWindow[0],
                                        location[1] + y - decorScreen[1] + decorWindow[1],
                                    ),
                                ) { "Host pointer target overlaps the IME touch region" }
                            }
                            (location[0] + x).toFloat() to (location[1] + y).toFloat()
                        }
                        val down = SystemClock.uptimeMillis()
                        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, point.first, point.second, 0)
                            try {
                                event.source = InputDevice.SOURCE_TOUCHSCREEN
                                check(instrumentation.uiAutomation.injectInputEvent(event, true)) { "Actual pointer injection was rejected" }
                            } finally {
                                event.recycle()
                            }
                        }
                    }
                    suspend fun verifyTouchRegion(theme: String, committedBefore: String) {
                        val iv = main { checkNotNull(input()) }
                        val preedit = main { iv.di.direct.instance<PreeditDelegate>().ui }
                        val root = preedit.root
                        val pendingBefore = main { chat.text.toString() }
                        val before = committedBefore
                        touchStage("before_region_lifecycle", theme)
                        main {
                            val insets = touchInsets()
                            val keyboardPosition = IntArray(2).also(iv.keyboardView::getLocationInWindow)
                            check(insets.contentTopInsets == keyboardPosition[1] && insets.visibleTopInsets == keyboardPosition[1])
                            check(insets.touchableInsets == InputMethodService.Insets.TOUCHABLE_INSETS_REGION)
                            check(regionCovers(root))
                            val bounds = visibleWindowBounds(root)
                            check(visibleWindowBounds(iv).contains(bounds)) { "Preedit must be reachable through its actual InputView ancestor" }
                            val decor = checkNotNull(service.window.window).decorView
                            check(bounds.right < decor.width - 1)
                            check(!insets.touchableRegion.contains(decor.width - 1, bounds.centerY())) { "Blank app space beside preedit must remain touch-through" }
                            checkNoTouchPopup()
                        }
                        val hostPointerActions = mutableListOf<Int>()
                        val hostContent = main { editor.findViewById<FrameLayout>(android.R.id.content) }
                        fun hostGapGeometry(): Pair<Rect, String> {
                            val decor = checkNotNull(service.window.window).decorView
                            val preeditBounds = visibleWindowBounds(root)
                            val decorScreen = IntArray(2).also(decor::getLocationOnScreen)
                            val decorWindow = IntArray(2).also(decor::getLocationInWindow)
                            val hostScreen = IntArray(2).also(hostContent::getLocationOnScreen)
                            val hostVisible = Rect()
                            val hostIsVisible = hostContent.isShown && hostContent.getLocalVisibleRect(hostVisible)
                            hostVisible.offset(hostScreen[0], hostScreen[1])
                            val windowScreenX = decorScreen[0] - decorWindow[0]
                            val windowScreenY = decorScreen[1] - decorWindow[1]
                            val preeditScreen = Rect(preeditBounds).apply { offset(windowScreenX, windowScreenY) }
                            val available = Rect(preeditScreen.right, preeditScreen.top, windowScreenX + decor.width, preeditScreen.bottom)
                            val targetScreen = Rect()
                            val targetWindow = Rect()
                            val insets = touchInsets()
                            if (!preeditBounds.isEmpty && hostIsVisible && available.intersect(hostVisible) && available.width() >= 32 && available.height() >= 32) {
                                targetScreen.set(available.centerX() - 16, available.centerY() - 16, available.centerX() + 16, available.centerY() + 16)
                                targetWindow.set(targetScreen)
                                targetWindow.offset(-windowScreenX, -windowScreenY)
                                if (!Region(targetWindow).apply { op(insets.touchableRegion, Region.Op.INTERSECT) }.isEmpty) targetScreen.setEmpty()
                            }
                            return targetScreen to "hostVisible=$hostVisible hostScreen=${hostScreen.contentToString()} " +
                                "hostSize=${hostContent.width}x${hostContent.height} hostScroll=${hostContent.scrollX},${hostContent.scrollY} " +
                                "hostPadding=${hostContent.paddingLeft},${hostContent.paddingTop},${hostContent.paddingRight},${hostContent.paddingBottom} " +
                                "decorScreen=${decorScreen.contentToString()} decorWindow=${decorWindow.contentToString()} decorSize=${decor.width}x${decor.height} " +
                                "preeditWindow=$preeditBounds preeditScreen=$preeditScreen intersection=$available targetScreen=$targetScreen targetWindow=$targetWindow " +
                                "touchRegion=${insets.touchableRegion}"
                        }
                        var hostGeometryDiagnostic = "Host geometry has not been sampled"
                        try {
                            until("$theme actual host content can fit a pointer target beside preedit") {
                                val (target, diagnostic) = hostGapGeometry()
                                hostGeometryDiagnostic = diagnostic
                                !target.isEmpty
                            }
                        } catch (failure: TimeoutCancellationException) {
                            throw IllegalStateException("$theme host pointer target geometry did not become reachable: $hostGeometryDiagnostic", failure)
                        }
                        var expectedHostTargetScreen = Rect()
                        val hostTarget = main {
                            val (targetScreen, diagnostic) = hostGapGeometry()
                            check(!targetScreen.isEmpty) { "$theme host pointer target geometry changed: $diagnostic" }
                            expectedHostTargetScreen = Rect(targetScreen)
                            val hostScreen = IntArray(2).also(hostContent::getLocationOnScreen)
                            val left = targetScreen.left - hostScreen[0] + hostContent.scrollX - hostContent.paddingLeft
                            val top = targetScreen.top - hostScreen[1] + hostContent.scrollY - hostContent.paddingTop
                            check(left >= 0 && top >= 0 && left + 32 <= hostContent.width && top + 32 <= hostContent.height) {
                                "$theme host pointer local placement is outside content: left=$left top=$top $diagnostic"
                            }
                            phase("HOST_POINTER_GEOMETRY: $theme left=$left top=$top $diagnostic")
                            object : EditText(editor) {
                                @SuppressLint("ClickableViewAccessibility")
                                override fun onTouchEvent(event: MotionEvent): Boolean {
                                    hostPointerActions.add(event.actionMasked)
                                    return true
                                }
                            }.apply {
                                minimumHeight = 0
                                minimumWidth = 0
                                setPadding(0, 0, 0, 0)
                                isFocusable = false
                                hostContent.addView(
                                    this,
                                    FrameLayout.LayoutParams(32, 32).apply {
                                        leftMargin = left
                                        topMargin = top
                                    },
                                )
                            }
                        }
                        try {
                            until("$theme host editor target is visible in the empty preedit gap") { hostTarget.isShown && hostTarget.isLaidOut }
                            delay(150)
                            touchStage("before_host_gap_pointer", theme)
                            main {
                                val actualScreen = Rect()
                                check(hostTarget.getLocalVisibleRect(actualScreen)) { "$theme host pointer target is clipped: ${hostGapGeometry().second}" }
                                val targetLocation = IntArray(2).also(hostTarget::getLocationOnScreen)
                                actualScreen.offset(targetLocation[0], targetLocation[1])
                                check(actualScreen == expectedHostTargetScreen) {
                                    "$theme host pointer target moved or was clipped: expected=$expectedHostTargetScreen actual=$actualScreen ${hostGapGeometry().second}"
                                }
                            }
                            tapActualView(hostTarget, imeTarget = false)
                            check(
                                main {
                                    hostPointerActions.filter { it == MotionEvent.ACTION_DOWN || it == MotionEvent.ACTION_UP } ==
                                        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)
                                },
                            ) { "Empty space beside preedit intercepted the host editor pointer" }
                            touchStage("after_host_gap_pointer", theme)
                            phase("PASS: $theme empty preedit gap routes a real pointer to the host editor")
                        } finally {
                            main { (hostTarget.parent as? ViewGroup)?.removeView(hostTarget) }
                        }
                        val originalVisibility = main { root.visibility }
                        try {
                            main { root.visibility = View.INVISIBLE }
                            until("$theme invisible preedit leaves the IME touch region") { !regionCovers(root) }
                        } finally {
                            main { root.visibility = originalVisibility }
                        }
                        until("$theme visible preedit resumes the IME touch region") { regionCovers(root) }
                        val inputVisibility = main { iv.visibility }
                        try {
                            main { iv.visibility = View.INVISIBLE }
                            until("$theme invisible preedit ancestor leaves the IME touch region") { !regionCovers(root) }
                        } finally {
                            main { iv.visibility = inputVisibility }
                        }
                        until("$theme visible preedit ancestor resumes the IME touch region") { regionCovers(root) }
                        main {
                            val parent = root.parent as ViewGroup
                            val params = root.layoutParams
                            parent.removeView(root)
                            try {
                                check(!regionCovers(root)) { "Detached preedit must not retain a touch region" }
                            } finally {
                                parent.addView(root, params)
                            }
                        }
                        until("$theme reattached preedit restores only its visible touch region") { regionCovers(root) }
                        // Preserve the historical host protocol label for the replaced overlay path.
                        phase("PASS: detached touch overlay cannot crash or leak across reattachment")
                        delay(150)
                        check(main { iv.rime.run { compositionCached.cursorPos > 0 } }) { "Preedit pointer needs a nonzero native caret before the tap" }
                        touchStage("before_preedit_pointer", theme)
                        tapActualView(preedit.preedit, leadingText = true)
                        until("$theme real preedit pointer changes the native caret without committing") {
                            iv.rime.run { compositionCached.cursorPos == 0 && compositionCached.length > 0 } && chat.text.toString() == pendingBefore
                        }
                        api {
                            check(getRawInput() == "ni")
                            clearComposition()
                        }
                        touchStage("after_preedit_pointer", theme)
                        repeat(20) { round ->
                            touchStage("before_rapid_show", theme, round)
                            val showStart = SystemClock.elapsedRealtimeNanos()
                            type("ni")
                            api { check(getRawInput() == "ni") }
                            until("$theme rapid $round preedit included") { preedit.preedit.isShown && regionCovers(preedit.preedit) }
                            val showMs = (SystemClock.elapsedRealtimeNanos() - showStart) / 1_000_000
                            touchStage("before_rapid_clear", theme, round)
                            val clearStart = SystemClock.elapsedRealtimeNanos()
                            api { clearComposition() }
                            until("$theme rapid $round cleared preedit excluded") { !preedit.preedit.isShown && !regionCovers(preedit.preedit) }
                            val clearMs = (SystemClock.elapsedRealtimeNanos() - clearStart) / 1_000_000
                            main { checkNoTouchPopup() }
                            touchRegionMeasurements.put(
                                JSONObject().apply {
                                    put("theme", theme)
                                    put("round", round)
                                    put("show_ms", showMs)
                                    put("clear_ms", clearMs)
                                },
                            )
                            touchStage("after_rapid_clear", theme, round, clearMs)
                            check(showMs < 2_000 && clearMs < 2_000) { "Rapid preedit operation exceeded the 2s stall regression limit: show=$showMs clear=$clearMs" }
                            check(main { chat.text.toString() == before }) { "Showing/clearing preedit unexpectedly committed editor text" }
                        }
                        val previousMode = main { candidatesMode.getValue() }
                        try {
                            main { candidatesMode.setValue(PopupCandidatesMode.INPUT_DEVICE) }
                            touchStage("before_hardware_candidate", theme)
                            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_N)
                            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_I)
                            fun floatingCandidates() = roots().asSequence().flatMap(::descendants).filterIsInstance<CandidatesView>().firstOrNull { it.isShown }
                            until("$theme hardware candidates own their main-window touch region") {
                                service.isInputViewShown && input() == null && floatingCandidates()?.let(::regionCovers) == true
                            }
                            val cv = main { checkNotNull(floatingCandidates()) }
                            val recycler = main { descendants(cv).filterIsInstance<RecyclerView>().first() }
                            until("$theme hardware first candidate has a current native snapshot") {
                                val holder = recycler.findViewHolderForAdapterPosition(0) as? PagedCandidatesUi.UiHolder.Candidate
                                holder != null && holder.ui.root.isShown && holder.snapshot == iv.rime.run { candidateSnapshotCached }
                            }
                            val holder = main { recycler.findViewHolderForAdapterPosition(0) as PagedCandidatesUi.UiHolder.Candidate }
                            var expected = ""
                            api {
                                check(getRawInput() == "ni")
                                expected = getCandidates(0, 1, holder.snapshot).single().text
                            }
                            check(expected.isNotEmpty() && main { holder.ui.root.text.toString().contains(expected) })
                            delay(150)
                            tapActualView(holder.ui.root)
                            until("$theme real floating candidate pointer commits its native text exactly once") { chat.text.toString() == before + expected }
                            api { check(getRawInput().isEmpty()) }
                            delay(400)
                            check(main { chat.text.toString() == before + expected }) { "Floating candidate commit repeated after its snapshot changed" }
                            main { checkNoTouchPopup() }
                            touchStage("after_hardware_candidate", theme)
                        } finally {
                            main {
                                candidatesMode.setValue(previousMode)
                                chat.setText(before)
                                chat.setSelection(chat.text.length)
                                @Suppress("DEPRECATION")
                                service.onViewClicked(false)
                                inputManager.showSoftInput(chat, 0)
                            }
                        }
                        until("$theme virtual full-pinyin returns after real floating candidate touch") { input() != null && !keyboard().isT9Layout }
                        type("ni")
                        api { check(getRawInput() == "ni") }
                        until("$theme preedit restored before visibility regression") { regionCovers(preedit.preedit) }
                        phase("PASS: $theme main IME touch region, real preedit/candidate pointers and 20 rapid show-clear rounds")
                    }
                    suspend fun verifyT9Accessibility(theme: String) {
                        fun currentViews(): Map<Int, KeyView> {
                            val activeInput = checkNotNull(input())
                            check(keyboard().isT9Layout) { "$theme accessibility fixture is not actual T9" }
                            val visible = descendants(activeInput).filterIsInstance<KeyView>().filter { it.isShown }.toList()
                            return keyboard().keys.indices.associateWith { index -> visible.single { it.id == index } }
                        }
                        fun descriptionsMatch(ascii: Boolean, expectedViews: Map<Int, KeyView>?): Boolean {
                            val active = keyboard()
                            val views = currentViews()
                            if (expectedViews != null && views.any { (index, view) -> expectedViews[index] !== view }) return false
                            val labels = listOf("ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ")
                            for (digit in 2..9) {
                                val key = active.keys.single { it.code == KeyEvent.KEYCODE_0 + digit }
                                val description = "$digit ${if (ascii) digit.toString() else labels[digit - 2]}"
                                if (views.getValue(key.index).contentDescription?.toString() != description) return false
                            }
                            val symbols = active.keys.single { it.click?.select == "symbols" }
                            val symbolDescription = if (ascii) {
                                if (theme == "trime") "符號" else "更多"
                            } else {
                                "符号"
                            }
                            if (views.getValue(symbols.index).contentDescription?.toString() != symbolDescription) return false
                            for ((code, nativeLabel, asciiLabel) in listOf(
                                Triple(KeyEvent.KEYCODE_COMMA, "，", ","),
                                Triple(KeyEvent.KEYCODE_PERIOD, "。", "."),
                            )) {
                                val key = active.keys.single { it.code == code }
                                if (views.getValue(key.index).contentDescription?.toString() != if (ascii) asciiLabel else nativeLabel) return false
                            }
                            return true
                        }
                        val cached = main { currentViews() }
                        api {
                            setRuntimeOption("ascii_mode", true)
                            check(statusCached.isAsciiMode)
                        }
                        until("$theme cached T9 accessibility changes to actual ASCII labels") { descriptionsMatch(true, cached) }
                        api {
                            setRuntimeOption("ascii_mode", false)
                            check(!statusCached.isAsciiMode)
                        }
                        until("$theme cached T9 accessibility changes back to ABC and Chinese punctuation") { descriptionsMatch(false, cached) }
                        // Freeze the next queued native mode until a fresh themed T9 view has
                        // actually been constructed from the preceding ASCII native cache.
                        api { setRuntimeOption("ascii_mode", true) }
                        until("$theme cached T9 is ASCII before a theme rebuild") { descriptionsMatch(true, cached) }
                        val entered = CompletableDeferred<Unit>()
                        val release = CompletableDeferred<Unit>()
                        val held = main {
                            service.postRimeJob {
                                entered.complete(Unit)
                                release.await()
                            }
                        }
                        val rebuilt = try {
                            withTimeout(30_000) { entered.await() }
                            ThemeManager.selectTheme(theme, forceReload = true)
                            until("$theme rebuild creates new real T9 views while ASCII mode is queued") {
                                input() != null && keyboard().isT9Layout && currentViews().all { (index, view) -> cached[index] !== view }
                            }
                            main {
                                currentViews().also { rebuilt ->
                                    check(descriptionsMatch(true, rebuilt)) { "$theme rebuilt T9 did not describe its actual initial ASCII labels" }
                                }
                            }
                        } finally {
                            release.complete(Unit)
                            held.join()
                        }
                        api {
                            setRuntimeOption("ascii_mode", false)
                            check(!statusCached.isAsciiMode)
                        }
                        until("$theme rebuilt T9 accessibility refreshes native labels without replacing views") { descriptionsMatch(false, rebuilt) }
                    }
                    for (theme in listOf("trime", "tongwenfeng.trime")) {
                        ThemeManager.selectTheme(theme)
                        until("$theme T9 before schema switch") { input() != null && keyboard().isT9Layout }
                        verifyT9Accessibility(theme)
                        if (theme == "trime") {
                            ResidentWindowColorRegression.verify(instrumentation, main { checkNotNull(input()) })
                        }
                        api {
                            check(selectSchema("luna_pinyin_simp"))
                            setRuntimeOption("ascii_mode", false)
                        }
                        until("$theme full pinyin replaces locked T9") { input() != null && !keyboard().isT9Layout }
                        val touchCommittedBefore = main { chat.text.toString() }
                        type("ni")
                        api { check(getRawInput() == "ni") }
                        val preeditView = main { checkNotNull(input()).di.direct.instance<PreeditDelegate>().ui.preedit }
                        until("$theme full-pinyin preedit overlay is visible") { preeditView.isShown && regionCovers(preeditView) }
                        verifyTouchRegion(theme, touchCommittedBefore)
                        val imm = editor.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                        touchStage("before_hide", theme)
                        main { imm.hideSoftInputFromWindow(chat.windowToken, 0) }
                        until("$theme hiding input dismisses the preedit overlay") { !service.isInputViewShown && !regionCovers(preeditView) }
                        touchStage("before_show", theme)
                        main { imm.showSoftInput(chat, 0) }
                        until("$theme showing input restores the preedit overlay") { service.isInputViewShown && regionCovers(preeditView) }
                        api {
                            check(getRawInput() == "ni")
                            clearComposition()
                        }
                        touchStage("after_hide_show_clear", theme)
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
                        type("64")
                        api { check(getRawInput() == "64") }
                        until("$theme native Hanzi row is laid out") {
                            input()?.di?.direct?.instance<CompactCandidateDelegate>()?.view
                                ?.findViewHolderForAdapterPosition(0)?.itemView
                                ?.let { it.isShown && it.isLaidOut && !it.isLayoutRequested } == true
                        }
                        instrumentation.waitForIdleSync()
                        delay(150)
                        verifyCandidateGlyphs(instrumentation, service, theme)
                        api { clearComposition() }
                        // A narrow numeric SPACE key must show its purpose without clipping
                        // the longer current schema name. Check both shipped themes, not
                        // arbitrary user-defined labels or typography.
                        for (numericType in listOf(InputType.TYPE_CLASS_PHONE, InputType.TYPE_CLASS_NUMBER)) {
                            main {
                                other.hint = "$theme numeric space"
                                other.inputType = numericType
                                other.imeOptions = EditorInfo.IME_ACTION_DONE
                                other.setImeActionLabel(null, 0)
                                other.setText("")
                            }
                            focus(other)
                            until("$theme numeric space key is laid out") {
                                val activeInput = input() ?: return@until false
                                val window = activeInput.di.direct.instance<KeyboardWindow>()
                                val selectedId = KeyboardWindow::class.java.getDeclaredField("currentKeyboardId").apply { isAccessible = true }.get(window)
                                if (selectedId != "number" || window.currentKeyboard.isT9Layout) return@until false
                                val keys = window.currentKeyboard.keys
                                val space = keys.singleOrNull { it.code == KeyEvent.KEYCODE_SPACE } ?: return@until false
                                descendants(activeInput).filterIsInstance<KeyView>()
                                    .singleOrNull { it.id == keys.indexOf(space) }
                                    ?.let { it.isShown && it.width > 0 && it.height > 0 } == true
                            }
                            main {
                                val keys = keyboard().keys
                                val space = keys.single { it.code == KeyEvent.KEYCODE_SPACE }
                                check(space.getCode(KeyBehavior.CLICK) == KeyEvent.KEYCODE_SPACE) { "$theme space lost its action" }
                                val view = descendants(checkNotNull(input())).filterIsInstance<KeyView>()
                                    .single { it.id == keys.indexOf(space) }
                                val visible = Rect()
                                check(view.getLocalVisibleRect(visible) && visible == Rect(0, 0, view.width, view.height)) {
                                    "$theme numeric space key is clipped: $visible"
                                }
                                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                                try {
                                    // Render the actual KeyView before reading its final text paint;
                                    // nominal theme size alone would miss drawing-time fitting.
                                    view.draw(Canvas(bitmap))
                                    val paint = KeyView::class.java.getDeclaredField("textPaint").apply { isAccessible = true }.get(view) as Paint
                                    check(paint.textAlign == Paint.Align.CENTER) { "Numeric space bounds require the actual centered text paint" }
                                    val label = space.getLabel()
                                    val glyphs = Rect()
                                    paint.getTextBounds(label, 0, label.length, glyphs)
                                    val centerX = (view.width - view.paddingLeft - view.paddingRight) / 2f + view.paddingLeft + view.sp(space.keyTextOffsetX)
                                    val left = centerX - paint.measureText(label) / 2f + glyphs.left
                                    val right = centerX - paint.measureText(label) / 2f + glyphs.right
                                    check(!glyphs.isEmpty && left >= view.paddingLeft && right <= view.width - view.paddingRight) {
                                        "$theme numeric space label '$label' is horizontally clipped: $left..$right in ${view.paddingLeft}..${view.width - view.paddingRight}"
                                    }
                                    val metrics = paint.fontMetrics
                                    val baseline = (view.height - view.paddingTop - view.paddingBottom) / 2f + view.paddingTop -
                                        (metrics.ascent + metrics.descent) / 2f + view.sp(space.keyTextOffsetY)
                                    check(baseline + glyphs.top >= view.paddingTop && baseline + glyphs.bottom <= view.height - view.paddingBottom) {
                                        "$theme numeric space label '$label' is vertically clipped"
                                    }
                                    check(label == "空格") { "$theme numeric space must identify the space action" }
                                } finally {
                                    bitmap.recycle()
                                }
                            }
                            phase("PASS: $theme numeric space action and rendered label fit its visible key")
                            focus(chat)
                            until("$theme T9 returns after numeric space check") { input() != null && keyboard().isT9Layout }
                        }
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
                        val expectedPrivate = "password" in name
                        check(service.isPrivacyModeActive == expectedPrivate) { "$name privacy state mismatch" }
                        check(PrivacyModeState.active == expectedPrivate) { "$name process privacy state mismatch" }
                        check(speechAllowed() != expectedPrivate) { "$name speech privacy state mismatch" }
                        api {
                            check(Rime.isRimePrivacyGuardEnabledForTesting() == expectedPrivate) {
                                "$name native privacy state mismatch"
                            }
                        }
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
                        other.hint = "no-personalized-learning"
                        other.inputType = InputType.TYPE_CLASS_TEXT
                        other.imeOptions = EditorInfo.IME_ACTION_DONE or EditorPolicy.NO_PERSONALIZED_LEARNING_FLAG
                    }
                    focus(other)
                    check(service.isPrivacyModeActive && PrivacyModeState.active)
                    check(!speechAllowed())
                    check(main { keyboard().isT9Layout }) { "No-learning flag must not force an unrelated keyboard layout" }
                    api { check(Rime.isRimePrivacyGuardEnabledForTesting()) }
                    focus(chat)
                    check(!service.isPrivacyModeActive && !PrivacyModeState.active)
                    api { check(!Rime.isRimePrivacyGuardEnabledForTesting()) }
                    phase("PASS: no-personalized-learning flag controls privacy without replacing T9")

                    verifyTransientKeyboardAcrossPrivacyChange(instrumentation, service)

                    main { manualPrivacyMode.setValue(true) }
                    until("manual privacy reaches the active editor") {
                        service.isPrivacyModeActive && PrivacyModeState.active && !speechAllowed()
                    }
                    api { check(Rime.isRimePrivacyGuardEnabledForTesting()) }
                    val privateUndoSuffix = "x"
                    val privateUndoPrefix = "virtual-password-"
                    val slideDelete = main {
                        descendants(checkNotNull(input()))
                            .filterIsInstance<KeyView>()
                            .first { it.isSlideDelete }
                    }
                    val deletedTextBuffer = KeyView::class.java
                        .getDeclaredField("deletedTextBuffer")
                        .apply { isAccessible = true }
                    api { clearComposition() }
                    main {
                        check(chat.text.isEmpty()) { "Private slide-delete fixture requires an empty editor" }
                        check(checkNotNull(service.currentInputConnection).commitText(privateUndoPrefix + privateUndoSuffix, 1))
                    }
                    until("private slide-delete fixture reaches the active connection") {
                        chat.text.toString() == privateUndoPrefix + privateUndoSuffix
                    }
                    main { checkNotNull(slideDelete.onSlide).invoke(-1, 0f, 0f) }
                    until("private slide-delete stages the virtual suffix") {
                        chat.text.toString() == privateUndoPrefix
                    }
                    main {
                        @Suppress("UNCHECKED_CAST")
                        val buffered = deletedTextBuffer.get(slideDelete) as Collection<String>
                        check(buffered.toList() == listOf(privateUndoSuffix)) {
                            "Private slide-delete buffer did not retain only the virtual suffix"
                        }
                    }
                    main { manualPrivacyMode.setValue(false) }
                    until("manual privacy restores the active editor") {
                        !service.isPrivacyModeActive && !PrivacyModeState.active && speechAllowed()
                    }
                    api { check(!Rime.isRimePrivacyGuardEnabledForTesting()) }
                    main {
                        @Suppress("UNCHECKED_CAST")
                        val buffered = deletedTextBuffer.get(slideDelete) as Collection<String>
                        check(buffered.isEmpty()) { "Private slide-delete buffer survived the privacy boundary" }
                        checkNotNull(slideDelete.onSlide).invoke(1, 0f, 0f)
                    }
                    instrumentation.waitForIdleSync()
                    delay(250)
                    main {
                        check(chat.text.toString() == privateUndoPrefix) {
                            "Private slide-delete undo survived the privacy boundary"
                        }
                        check(checkNotNull(service.currentInputConnection).deleteSurroundingText(privateUndoPrefix.length, 0))
                    }
                    until("private slide-delete fixture is cleared") { chat.text.isEmpty() }
                    phase("PASS: manual privacy atomically gates speech and native learning, then restores normal input")
                    PrivacyTriggerSinkProbe.verify(
                        instrumentation,
                        PrivacyTriggerSinkProbe.Harness(
                            service = service,
                            chat = chat,
                            target = other,
                            focus = ::focus,
                            runApi = ::api,
                            inputTextContains = { token ->
                                input()?.let { activeInput ->
                                    descendants(activeInput)
                                        .filterIsInstance<TextView>()
                                        .any { it.text.toString().contains(token) }
                                } == true
                            },
                            speechAllowed = ::speechAllowed,
                            phase = ::phase,
                        ),
                    )
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

                    verifyHardwareEditorPolicies(instrumentation, service, editor, chat, other)

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
                              preset_keyboards/bopomofo/ascii_keyboard: default
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
                    val inputViewBeforeRedeploy = main { checkNotNull(input()) }
                    val tipsPreference = AppPrefs.defaultInstance().general.asciiSwitchTips
                    val previousTipsPreference = tipsPreference.getValue()
                    // Observe the real timer without exposing a production test API.
                    val realRime = RimeDaemon::class.java.getDeclaredMethod("getRealRime").apply {
                        isAccessible = true
                    }.invoke(RimeDaemon) as Rime
                    val tipJobField = Rime::class.java.getDeclaredField("asciiSwitchTipsJob").apply {
                        isAccessible = true
                    }
                    try {
                        main { tipsPreference.setValue(true) }
                        api {
                            setRuntimeOption("ascii_mode", true)
                            setRuntimeOption("ascii_mode", false)
                            val previousTip = checkNotNull(tipJobField.get(realRime) as Job?)
                            check(previousTip.isActive) { "No pending ASCII tip before redeploy" }
                            updateConfig()
                            check(previousTip.isCancelled) { "Old native context's ASCII tip survived redeploy" }
                            val currentTip = checkNotNull(tipJobField.get(realRime) as Job?)
                            check(currentTip !== previousTip) { "Redeploy reused the old ASCII-tip timer" }
                            withTimeout(5000) { currentTip.join() }
                            check(currentTip.isCompleted && !currentTip.isCancelled) { "New engine's ASCII tip did not clear normally" }
                            // startRime() intentionally publishes a fresh, non-editor ASCII/schema
                            // tip. Assert the complete transient-state boundary after that new
                            // timer restores the native engine's empty composition.
                            checkTransientEditorStateCleared("successful native restart after ASCII tip")
                        }
                        phase("PASS: redeploy cancels the old ASCII tip and the new engine clears its own tip")
                    } finally {
                        main { tipsPreference.setValue(previousTipsPreference) }
                    }
                    until("deployment reloads the active theme and preserves compatible T9") {
                        input()?.let { it !== inputViewBeforeRedeploy && keyboard().isT9Layout } == true
                    }
                    api {
                        check(!statusCached.isAsciiMode) { "Theme reload lost the active editor's Chinese mode" }
                    }
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
                    // currentInputEditorInfo is published by onStartInput before the separate
                    // onStartInputView callback applies KeyboardWindow's editor policy. The
                    // visible layout change is the main-thread completion boundary for that
                    // callback; the following queued API read then observes its mode update.
                    until("redeployed number editor applies its ASCII layout") {
                        input() != null && !keyboard().isT9Layout
                    }
                    api { check(statusCached.isAsciiMode) { "Number editor did not enter ASCII mode" } }

                    verifyQueuedModeRestoreAcrossThemeReload(instrumentation, service, inputManager, chat, id)
                    verifyQueuedModeRestoreAcrossPrivacyChange(
                        instrumentation,
                        service,
                        inputManager,
                        other,
                        chat,
                    )

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
                    suspend fun awaitPlatformImeHidden(index: Int) {
                        val completed = kotlinx.coroutines.withTimeoutOrNull(10_000) {
                            var stableSince = 0L
                            while (true) {
                                val hidden = main {
                                    if (Build.VERSION.SDK_INT < 30) {
                                        !service.isInputViewShown
                                    } else {
                                        val insets = ViewCompat.getRootWindowInsets(chat)
                                        !service.isInputViewShown &&
                                            insets != null &&
                                            !insets.isVisible(WindowInsetsCompat.Type.ime()) &&
                                            insets.getInsets(WindowInsetsCompat.Type.ime()).bottom == 0
                                    }
                                }
                                val now = SystemClock.uptimeMillis()
                                if (hidden) {
                                    if (stableSince == 0L) stableSince = now
                                    if (now - stableSince >= 700L) break
                                } else {
                                    stableSince = 0L
                                }
                                delay(40)
                            }
                            true
                        } == true
                        check(completed) {
                            "IME hide did not settle at cycle $index: " +
                                main {
                                    "viewShown=${service.isInputViewShown}, " +
                                        "insetsVisible=${ViewCompat.getRootWindowInsets(chat)?.isVisible(WindowInsetsCompat.Type.ime())}, " +
                                        "editorFocused=${chat.hasFocus()}"
                                }
                        }
                    }
                    repeat(100) { index ->
                        main { imm.hideSoftInputFromWindow(chat.windowToken, 0) }
                        awaitPlatformImeHidden(index + 1)
                        main {
                            chat.requestFocus()
                            check(chat.hasFocus()) { "Editor lost focus before show cycle ${index + 1}" }
                            check(imm.showSoftInput(chat, 0)) { "IME show request rejected at cycle ${index + 1}" }
                        }
                        val shown = kotlinx.coroutines.withTimeoutOrNull(10_000) {
                            while (!main { service.isInputViewShown && input() != null && keyboard().isT9Layout }) delay(40)
                            true
                        } == true
                        check(shown) {
                            "IME did not show at cycle ${index + 1}: " +
                                main {
                                    "viewShown=${service.isInputViewShown}, " +
                                        "insetsVisible=${ViewCompat.getRootWindowInsets(chat)?.isVisible(WindowInsetsCompat.Type.ime())}, " +
                                        "editorFocused=${chat.hasFocus()}"
                                }
                        }
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
                    awaitPlatformImeHidden(101)
                    main {
                        chat.requestFocus()
                        check(imm.showSoftInput(chat, 0)) { "IME show request rejected after locked composition" }
                    }
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
                manualPrivacyMode.setValue(originalManualPrivacyMode)
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
        val touchReport = JSONObject().apply {
            put("run_id", touchRegionRunId)
            put("api", Build.VERSION.SDK_INT)
            put("main_pid", android.os.Process.myPid())
            put("measurement", "instrumentation wall time including Rime queue, main-thread dispatch and visible-state polling; not candidate latency/P95")
            put("rapid_stall_limit_ms", 2_000)
            put("rapid_rounds", touchRegionMeasurements)
            put("editor_probe_passed", passed)
        }
        result.putString("touch_region_metrics", touchReport.toString())
        val touchDirectory = File(context.filesDir, "runtime-audit/touch-region")
        check(touchDirectory.isDirectory || touchDirectory.mkdirs())
        File(touchDirectory, "report.json").writeText(touchReport.toString(2) + "\n")
        result.putBoolean("passed", passed)
        instrumentation.finish(if (passed) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
    }
}
