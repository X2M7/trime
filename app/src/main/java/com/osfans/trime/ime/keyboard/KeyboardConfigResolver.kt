/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.ime.keyboard

import com.osfans.trime.data.theme.model.TextKeyboard

internal fun resolveKeyboardConfig(name: String, presets: Map<String, TextKeyboard>): TextKeyboard? {
    val visited = mutableSetOf<String>()
    var current = name
    while (true) {
        val id = if (current in presets) current else "default"
        require(visited.add(id)) { "Cyclic keyboard import: ${visited.joinToString()} -> $id" }
        val config = presets[id] ?: return null
        if (config.importPreset.isEmpty()) return config
        current = config.importPreset
    }
}

internal fun matchKeyboard(
    schemaId: String,
    requested: String,
    pinyinT9: Boolean,
    alphabet: String,
    presets: Map<String, TextKeyboard>,
    fallbacks: Map<String, String>,
): String {
    if (requested.isNotEmpty() && requested in presets) return requested
    if (schemaId in presets) return schemaId
    if (pinyinT9) {
        return "luna_pinyin_t9".takeIf { it in presets }
            ?: fallbacks["luna_pinyin_t9"] ?: "default"
    }
    val layout = when {
        alphabet.isEmpty() -> "default"
        alphabet.all { it.isLetter() } -> "qwerty"
        alphabet.all { it.isLetter() || it in ",./;" } -> "qwerty_"
        alphabet.all { it.isLetterOrDigit() } -> "qwerty0"
        else -> "default"
    }
    return layout.takeIf { it in presets } ?: "default"
}
