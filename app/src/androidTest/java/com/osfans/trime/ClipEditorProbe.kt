// SPDX-License-Identifier: GPL-3.0-or-later
package com.osfans.trime

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.widget.EditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.osfans.trime.ui.main.ClipEditActivity
import java.io.File

object ClipEditorProbe {
    fun run(instrumentation: Instrumentation) {
        val context = instrumentation.targetContext
        val result = Bundle()
        var activity: Activity? = null
        var passed = false
        try {
            check(Build.HARDWARE in setOf("ranchu", "goldfish")) { "Emulator only" }
            check(Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD).startsWith("${context.packageName}/")) {
                "Select this build of Trime as the default input method first"
            }
            val editor = instrumentation.startActivitySync(Intent(context, ClipEditActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            activity = editor
            var shown = false
            val deadline = SystemClock.uptimeMillis() + 30_000
            while (!shown && SystemClock.uptimeMillis() < deadline) {
                instrumentation.runOnMainSync {
                    val field = editor.findViewById<EditText>(R.id.clip_edit_text)
                    shown = field.hasFocus() && ViewCompat.getRootWindowInsets(field)?.isVisible(WindowInsetsCompat.Type.ime()) == true
                }
                if (!shown) SystemClock.sleep(100)
            }
            check(shown) { "Trime was not visible in the clipboard editor" }
            val screenshot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            try {
                File(context.cacheDir, "clip-editor-probe.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
            } finally {
                screenshot.recycle()
            }
            result.putString("stream", "PASS: clipboard editor focuses its text field and displays Trime; no clipboard entry changed\n")
            passed = true
        } catch (failure: Throwable) {
            result.putString("stream", "FAIL: ${failure.stackTraceToString()}\n")
        } finally {
            activity?.let { instrumentation.runOnMainSync { it.finish() } }
        }
        result.putBoolean("passed", passed)
        instrumentation.finish(if (passed) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
    }
}
