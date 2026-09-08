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
