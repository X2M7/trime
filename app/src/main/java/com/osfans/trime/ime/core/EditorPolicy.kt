/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.ime.core

import android.text.InputType
import android.view.inputmethod.EditorInfo

/** Editor restrictions are temporary; they must never become the user's Chinese mode. */
enum class EditorKeyboard { USER, ASCII, NUMBER }

object EditorPolicy {
    fun isPassword(inputType: Int): Boolean = when (inputType and InputType.TYPE_MASK_CLASS) {
        InputType.TYPE_CLASS_NUMBER -> inputType and InputType.TYPE_MASK_VARIATION == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        InputType.TYPE_CLASS_TEXT -> inputType and InputType.TYPE_MASK_VARIATION in setOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        )
        else -> false
    }

    fun keyboard(inputType: Int, imeOptions: Int): EditorKeyboard = when (inputType and InputType.TYPE_MASK_CLASS) {
        InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE, InputType.TYPE_CLASS_DATETIME -> EditorKeyboard.NUMBER
        else -> when {
            imeOptions and EditorInfo.IME_FLAG_FORCE_ASCII != 0 -> EditorKeyboard.ASCII
            inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT &&
                inputType and InputType.TYPE_MASK_VARIATION in setOf(
                    InputType.TYPE_TEXT_VARIATION_URI,
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                    InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
                    InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                ) -> EditorKeyboard.ASCII
            else -> EditorKeyboard.USER
        }
    }

    /** Null means a real Enter key, including multiline editors with NO_ENTER_ACTION. */
    fun enterAction(inputType: Int, imeOptions: Int, actionId: Int, actionLabel: CharSequence?): Int? {
        if (inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_NULL ||
            imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0
        ) {
            return null
        }
        if (!actionLabel.isNullOrEmpty() && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) return actionId
        return (imeOptions and EditorInfo.IME_MASK_ACTION).takeUnless {
            it == EditorInfo.IME_ACTION_NONE || it == EditorInfo.IME_ACTION_UNSPECIFIED
        }
    }

    fun enterAction(info: EditorInfo): Int? = enterAction(info.inputType, info.imeOptions, info.actionId, info.actionLabel)
}

data class EditorModeOverride(val schemaId: String, val keyboardId: String, val asciiMode: Boolean)
