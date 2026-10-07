/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.ime.keyboard

import com.osfans.trime.data.theme.ThemeTestSupport
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class KeyboardConfigResolverTest :
    StringSpec({
        val base = ThemeTestSupport.decodeBuiltinTheme("trime.yaml").presetKeyboards.getValue("default")

        "valid import chains and unknown-keyboard fallback retain the complete preset" {
            val presets = mapOf("default" to base, "a" to base.copy(importPreset = "b"), "b" to base.copy(importPreset = "default"))
            resolveKeyboardConfig("a", presets) shouldBe base
            resolveKeyboardConfig("missing", presets) shouldBe base
            resolveKeyboardConfig("missing", emptyMap()) shouldBe null
        }

        "self imports and missing alias targets cannot recurse through the default forever" {
            shouldThrow<IllegalArgumentException> { resolveKeyboardConfig("default", mapOf("default" to base.copy(importPreset = "default"))) }
            shouldThrow<IllegalArgumentException> { resolveKeyboardConfig("default", mapOf("default" to base.copy(importPreset = "missing"))) }
        }

        "multi-keyboard cycles are bounded" {
            shouldThrow<IllegalArgumentException> {
                resolveKeyboardConfig("a", mapOf("a" to base.copy(importPreset = "b"), "b" to base.copy(importPreset = "a")))
            }
        }

        "switch targets resolve existing and compatible layouts before the default" {
            val presets =
                linkedMapOf(
                    "default" to base,
                    "custom" to base,
                    "__compat_symbols" to base,
                )
            val fallbacks = mapOf("symbols" to "__compat_symbols", "number" to "missing")

            normalizeKeyboardTarget("custom", presets, fallbacks) shouldBe "custom"
            normalizeKeyboardTarget("symbols", presets, fallbacks) shouldBe "__compat_symbols"
            normalizeKeyboardTarget("number", presets, fallbacks) shouldBe "default"
            normalizeKeyboardTarget("unknown", presets, fallbacks) shouldBe "default"
        }

        "a missing default still resolves to an existing compatible letter layout" {
            val presets = linkedMapOf("custom" to base, "__compat_letter" to base)
            normalizeKeyboardTarget("unknown", presets, mapOf("letter" to "__compat_letter")) shouldBe "__compat_letter"
        }

        "landscape targets can reuse compatibility lookup without replacing a valid base" {
            val presets = mapOf("base" to base.copy(landscapeKeyboard = "symbols"), "__compat_symbols" to base)
            val fallbacks = mapOf("symbols" to "__compat_symbols")

            resolveLandscapeKeyboard("base", presets, fallbacks) shouldBe "__compat_symbols"
            resolveLandscapeKeyboard(
                "base",
                presets + ("base" to base.copy(landscapeKeyboard = "missing_landscape")),
                fallbacks,
            ) shouldBe "base"
        }
    })
