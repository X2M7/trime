/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.data.theme

import com.osfans.trime.data.theme.model.KeyActionToken
import com.osfans.trime.data.theme.model.PresetKey
import com.osfans.trime.ime.keyboard.KeyBehavior
import com.osfans.trime.ime.keyboard.matchKeyboard
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CompatibleKeyboardsTest :
    StringSpec({
        val builtin = ThemeTestSupport.decodeBuiltinTheme("trime.yaml")
        val thirdParty = builtin.copy(
            name = "third-party without T9",
            presetKeyboards = builtin.presetKeyboards - "luna_pinyin_t9" - "number" - "letter",
            presetKeys = builtin.presetKeys + ("T9_digit_2" to PresetKey(commit = "wrong")) + ("2" to PresetKey(commit = "wrong")),
        )
        val patched = thirdParty.withCompatibleKeyboards(builtin)
        "third-party style and every original key and keyboard remain untouched" {
            patched.generalStyle shouldBe thirdParty.generalStyle
            patched.colorSchemes shouldBe thirdParty.colorSchemes
            thirdParty.presetKeyboards.forEach { (id, value) -> patched.presetKeyboards[id] shouldBe value }
            thirdParty.presetKeys.forEach { (id, value) -> patched.presetKeys[id] shouldBe value }
        }
        "mixed T9 alphabet with metadata uses the compatible T9 instead of QWERTY" {
            val id = matchKeyboard("luna_pinyin_t9", "luna_pinyin_t9", true, "abcABC123~", patched.presetKeyboards, patched.fallbackKeyboards)
            id shouldBe patched.fallbackKeyboards["luna_pinyin_t9"]
            patched.presetKeyboards.getValue(id).t9Layout shouldBe true
        }
        "numeric stroke schemas are never inferred to be pinyin T9" {
            matchKeyboard("stroke", "", false, "12345", patched.presetKeyboards, patched.fallbackKeyboards) shouldBe "stroke"
            matchKeyboard("unknown_numeric", "", false, "12345", patched.presetKeyboards, patched.fallbackKeyboards) shouldBe "qwerty0"
        }
        "full pinyin shuangpin and empty alphabet keep established defaults" {
            matchKeyboard("luna_pinyin", "", false, "abc", patched.presetKeyboards, patched.fallbackKeyboards) shouldBe "qwerty"
            matchKeyboard("shuangpin", "", false, "abc;", patched.presetKeyboards, patched.fallbackKeyboards) shouldBe "qwerty_"
            matchKeyboard(".default", "", false, "", patched.presetKeyboards, patched.fallbackKeyboards) shouldBe "default"
        }
        "explicit schema layout takes priority over alphabet inference" {
            matchKeyboard("custom", "symbols", false, "12345", patched.presetKeyboards, patched.fallbackKeyboards) shouldBe "symbols"
        }
        "compatible literal digits and long presses cannot be hijacked by same-named third-party keys" {
            val key = patched.presetKeyboards.getValue(patched.fallbackKeyboards.getValue("luna_pinyin_t9")).keys[1]
            val click = (key.behaviors.getValue(KeyBehavior.CLICK) as KeyActionToken.Plain).token
            val long = (key.behaviors.getValue(KeyBehavior.LONG_CLICK) as KeyActionToken.Plain).token
            patched.presetKeys.getValue(click).send shouldBe "2"
            patched.presetKeys.getValue(long).command shouldBe "t9_digit"
            patched.presetKeys.getValue(long).option shouldBe "2"
        }
        "reserved namespace collisions select a new namespace without overwriting user content" {
            val twice = patched.withCompatibleKeyboards(builtin)
            patched.presetKeyboards.forEach { (id, value) -> twice.presetKeyboards[id] shouldBe value }
            (twice.fallbackKeyboards != patched.fallbackKeyboards) shouldBe true
        }
        "unmapped literal parentheses stay text while mapped punctuation keeps key semantics" {
            val keys = patched.presetKeyboards.getValue(patched.fallbackKeyboards.getValue("symbols")).keys
            val actions = keys.flatMap { it.behaviors.values }.filterIsInstance<KeyActionToken.Plain>()
                .map { patched.presetKeys.getValue(it.token) }
            listOf("(", ")").forEach { symbol ->
                actions.any { it.text == symbol && it.label == symbol && it.send.isEmpty() } shouldBe true
            }
            actions.any { it.send == "." && it.text.isEmpty() } shouldBe true
        }
    })
