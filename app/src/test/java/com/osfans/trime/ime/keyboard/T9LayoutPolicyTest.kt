/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.ime.keyboard

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class T9LayoutPolicyTest :
    StringSpec({
        "narrow widths use a horizontal list, wide widths reserve a sidebar" {
            T9LayoutPolicy.widths(360, 0, 1f, 0).choices shouldBe 0
            T9LayoutPolicy.widths(412, 0, 1f, 0).choices shouldBe 88
            T9LayoutPolicy.widths(412, 0, 2f, 0).choices shouldBe 0
            T9LayoutPolicy.widths(800, 0, 2f, 0).choices shouldBe 132
        }
        "all supported widths leave at least 48dp for the narrowest key" {
            for (width in listOf(360, 412, 600, 800, 900)) {
                for (font in listOf(1f, 1.3f, 2f)) {
                    for (hand in 0..2) {
                        val result = T9LayoutPolicy.widths(width, 12, font, hand)
                        val keyboard = width - result.left - result.right - result.choices
                        (keyboard * 0.16 >= 48) shouldBe true
                        if (hand != 0) keyboard shouldBe 320
                    }
                }
            }
        }
        "left and right handed layouts are mirrored without changing key order" {
            val left = T9LayoutPolicy.widths(412, 4, 1f, 1)
            val right = T9LayoutPolicy.widths(412, 4, 1f, 2)
            left.left shouldBe right.right
            left.right shouldBe right.left
            left.choices shouldBe 0
        }
        "short landscape keeps pinyin beside a 320dp one-handed keyboard" {
            for (height in listOf(360, 412)) {
                for (font in listOf(1f, 2f)) {
                    for (hand in 1..2) {
                        val result = T9LayoutPolicy.widths(800, 0, font, hand, height)
                        (result.choices > 0) shouldBe true
                        (800 - result.left - result.right - result.choices) shouldBe 320
                        (T9LayoutPolicy.height(height, 200, 80) + 48 + 40 + 48 + 28 <= height) shouldBe true
                    }
                }
            }
        }
        "height caps preserve controls and minimum targets" {
            T9LayoutPolicy.height(412, 200, 80) shouldBe 196
            T9LayoutPolicy.height(360, 200, 80) shouldBe 192
            T9LayoutPolicy.height(800, 240, 80) shouldBe 320
            T9LayoutPolicy.height(800, 240, 1) shouldBe 192
            T9LayoutPolicy.height(800, 240, 0) shouldBe 240
        }
    })
