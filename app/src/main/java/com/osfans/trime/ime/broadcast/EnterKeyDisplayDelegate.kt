/*
 * SPDX-FileCopyrightText: 2015 - 2024 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ime.broadcast

import android.view.inputmethod.EditorInfo
import com.osfans.trime.data.theme.Theme
import com.osfans.trime.ime.core.EditorEnterAction
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class EnterKeyDisplayDelegate(override val di: DI) : DIAware {
    private val broadcaster: InputBroadcaster by instance()
    private val theme: Theme by instance()

    companion object {
        const val DEFAULT_LABEL = "Enter"
    }

    enum class Mode {
        ACTION_LABEL_NEVER,
        ACTION_LABEL_ONLY,
        ACTION_LABEL_PREFERRED,
        CUSTOM_PREFERRED,
    }

    val mode: Mode = runCatching { Mode.entries[theme.style.enterLabelMode] }.getOrDefault(Mode.ACTION_LABEL_NEVER)

    var keyLabel: String = DEFAULT_LABEL
        private set

    private var actionLabel: String = DEFAULT_LABEL

    private fun labelFromEditorInfo(info: EditorInfo): String {
        val action = EditorEnterAction.resolve(info.inputType, info.imeOptions, info.actionId, info.actionLabel)
        return EditorEnterAction.label(action, info.actionLabel, mode.ordinal, theme.style.enterLabels.default) { actionId ->
            with(theme.style.enterLabels) {
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> done
                    EditorInfo.IME_ACTION_GO -> go
                    EditorInfo.IME_ACTION_NEXT -> next
                    EditorInfo.IME_ACTION_PREVIOUS -> pre
                    EditorInfo.IME_ACTION_SEARCH -> search
                    EditorInfo.IME_ACTION_SEND -> send
                    else -> null
                }
            }
        }
    }

    fun updateLabelOnEditorInfo(info: EditorInfo) {
        actionLabel = labelFromEditorInfo(info)
        if (keyLabel == actionLabel) return
        keyLabel = actionLabel
        broadcaster.onEnterKeyLabelUpdate(keyLabel)
    }
}
