/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.ui.main

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import com.osfans.trime.R

/** Disposable, non-floating host for same-process emulator editor probes. */
class EditorProbeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_CLIPBOARD_FOREGROUND, false)) {
            // A readable clipboard needs a focused window, but this fixture must own no
            // editor or IME session before it explicitly creates its Rime session.
            window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING,
            )
            val foreground = TextView(this).apply {
                setText(R.string.trime_app_name)
                isFocusableInTouchMode = true
            }
            setContentView(foreground)
            foreground.requestFocus()
        }
    }

    companion object {
        const val EXTRA_CLIPBOARD_FOREGROUND = "com.osfans.trime.debug.CLIPBOARD_FOREGROUND"
    }
}
