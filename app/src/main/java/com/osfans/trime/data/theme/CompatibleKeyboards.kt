/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.data.theme

import com.osfans.trime.core.RimeKeyMapping
import com.osfans.trime.data.theme.model.KeyActionToken
import com.osfans.trime.data.theme.model.PresetKey
import com.osfans.trime.ime.keyboard.resolveKeyboardConfig

/** In-memory only: never copy a desktop/theme configuration into the user's directory. */
internal fun Theme.withCompatibleKeyboards(builtin: Theme): Theme {
    val layouts = listOf("luna_pinyin_t9", "letter", "number", "symbols")
    var prefix = "__trime_compat_"
    while (presetKeys.keys.any { it.startsWith(prefix) } || presetKeyboards.keys.any { it.startsWith(prefix) }) prefix += "_"
    val ids = layouts.associateWith { prefix + it }
    val actions = mutableMapOf<String, PresetKey>()
    val keyboards = layouts.associate { name ->
        val keyboard = checkNotNull(resolveKeyboardConfig(name, builtin.presetKeyboards))
        ids.getValue(name) to keyboard.copy(
            asciiKeyboard = ids.getValue("letter"),
            landscapeKeyboard = "",
            importPreset = "",
            keys = keyboard.keys.map { key ->
                key.copy(
                    behaviors = key.behaviors.mapValues { (_, token) ->
                        if (token !is KeyActionToken.Plain) return@mapValues token
                        val id = prefix + "key_" + token.token
                        val literalSymbol = token.token.length == 1 && !token.token[0].isLetterOrDigit() && RimeKeyMapping.charToCode(token.token) == null
                        val preset = builtin.presetKeys[token.token] ?: if (literalSymbol || token.token.any { it.code > 127 } || '{' in token.token) {
                            PresetKey(text = token.token, label = token.token)
                        } else {
                            PresetKey(send = token.token)
                        }
                        actions[id] = preset.copy(select = ids[preset.select] ?: preset.select)
                        KeyActionToken.Plain(id)
                    },
                )
            },
        )
    }
    return copy(presetKeys = presetKeys + actions, presetKeyboards = presetKeyboards + keyboards, fallbackKeyboards = ids)
}
