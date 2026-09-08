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
    })
