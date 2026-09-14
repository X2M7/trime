/*
 * SPDX-FileCopyrightText: 2015 - 2024 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.data.theme

import com.osfans.trime.data.theme.model.GeneralStyle
import com.osfans.trime.util.yaml.Node
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * GeneralStyle decode baseline: field-level values from the built-in trime.yaml
 * (golden values taken from the file itself), and graceful fallback to defaults
 * for malformed values (empty / wrong types / unknown enums) in incorrect.yaml.
 */
class GeneralStyleTest :
    BehaviorSpec({
        Given("the built-in trime.yaml") {
            val theme = ThemeTestSupport.decodeBuiltinTheme("trime.yaml")

            When("its style section is decoded") {
                val style = theme.generalStyle

                Then("plain scalar values from the file are preserved") {
                    style shouldNotBe null
                    style.autoCaps shouldBe false
                    style.candidatePadding shouldBe 5
                    style.candidateSpacing shouldBe 0f
                    style.candidateTextSize shouldBe 22f
                    style.candidateTextVerticalBias shouldBe 1f
                    style.candidateViewHeight shouldBe 28
                    style.commentHeight shouldBe 12
                    style.commentPosition shouldBe GeneralStyle.CommentPosition.RIGHT
                    style.commentTextSize shouldBe 10f
                    style.horizontalGap shouldBe 1
                    style.keyHeight shouldBe 44
                    style.keyLongTextSize shouldBe 14f
                    style.keyTextSize shouldBe 22f
                    style.keyWidth shouldBe 10f
                    style.labelTextSize shouldBe 22f
                    style.keyboardHeight shouldBe 250
                    style.keyboardHeightLand shouldBe 200
                    style.keyboardPaddingRight shouldBe 40
                    style.keyboardPaddingLand shouldBe 40
                }

                Then("fonts declared as a single scalar decode to an empty list") {
                    // By design (since 2bcdf382) fonts must be lists, e.g. `candidate_font: [han.ttf]`;
                    // a scalar like `candidate_font: han.ttf` decodes to an empty list (system font).
                    style.candidateFont shouldBe emptyList()
                    style.keyFont shouldBe emptyList()
                }

                Then("legacy preview fields produce visible popup dimensions and preserve the preview font") {
                    style.popupWidth shouldBe 38
                    style.popupHeight shouldBe 60
                    style.popupKeyHeight shouldBe 60
                    style.popupTextSize shouldBe 40f
                    style.popupFont shouldBe listOf("latin.ttf")
                    // Legacy preview_offset is not a bottom margin: the two use different origins.
                    style.popupBottomMargin shouldBe 68
                }

                Then("theme header is decoded") {
                    theme.name shouldBe "預設"
                }
            }
        }

        Given("a theme with empty/incorrect style values") {
            val theme = ThemeTestSupport.decodeThemeFile("src/test/assets/incorrect.yaml")

            When("its style section is decoded") {
                val style = theme.generalStyle

                Then("malformed values fall back to defaults without exception") {
                    style.autoCaps shouldBe false
                    style.candidateTextSize shouldBe 15f
                    style.candidateBorder shouldBe 0
                    style.candidateFont shouldBe emptyList()
                    style.commentPosition shouldBe GeneralStyle.CommentPosition.RIGHT
                    style.enterLabel shouldNotBe null
                    style.enterLabel.go shouldBe "go"
                    style.enterLabel.default shouldBe "default"
                }
            }
        }

        Given("an empty style section") {
            val style = GeneralStyle.decode(Node.Mapping())

            Then("decode equals the constructor defaults") {
                style shouldBe GeneralStyle.DEFAULTS
                style.popupBottomMargin shouldBe 68
                style.popupWidth shouldBe 38
                style.popupHeight shouldBe 48
                style.popupKeyHeight shouldBe 48
                style.popupTextSize shouldBe 23f
            }
            Then("decode fills explicit keys but keeps the defaults for the rest") {
                val style =
                    GeneralStyle.decode(
                        Node.Mapping(
                            Node.Scalar("candidate_text_size") to Node.Scalar("20"),
                        ),
                    )
                style.candidateTextSize shouldBe 20f
                style.keyHeight shouldBe 0
            }
        }

        Given("a theme that mixes modern popup fields and legacy preview fields") {
            fun style(vararg popup: Pair<String, Node>): GeneralStyle = GeneralStyle.decode(
                Node.Mapping(
                    Node.Scalar("preview_height") to Node.Scalar("60"),
                    Node.Scalar("preview_text_size") to Node.Scalar("40"),
                    Node.Scalar("preview_font") to Node.Scalar("legacy.ttf"),
                    *popup.map { Node.Scalar(it.first) to it.second }.toTypedArray(),
                ),
            )

            Then("modern popup dimensions and font lists take precedence") {
                val decoded = style(
                    "popup_bottom_margin" to Node.Scalar("70"),
                    "popup_width" to Node.Scalar("42"),
                    "popup_height" to Node.Scalar("52"),
                    "popup_key_height" to Node.Scalar("46"),
                    "popup_text_size" to Node.Scalar("25"),
                    "popup_font" to Node.Sequence(Node.Scalar("modern.ttf")),
                )
                decoded.popupBottomMargin shouldBe 70
                decoded.popupWidth shouldBe 42
                decoded.popupHeight shouldBe 52
                decoded.popupKeyHeight shouldBe 46
                decoded.popupTextSize shouldBe 25f
                decoded.popupFont shouldBe listOf("modern.ttf")
            }

            Then("explicit zero dimensions and empty fonts do not reactivate legacy previews") {
                val decoded = style(
                    "popup_bottom_margin" to Node.Scalar("0"),
                    "popup_width" to Node.Scalar("0"),
                    "popup_height" to Node.Scalar("0"),
                    "popup_key_height" to Node.Scalar("0"),
                    "popup_text_size" to Node.Scalar("0"),
                    "popup_font" to Node.Sequence(),
                )
                decoded.popupBottomMargin shouldBe 0
                decoded.popupWidth shouldBe 0
                decoded.popupHeight shouldBe 0
                decoded.popupKeyHeight shouldBe 0
                decoded.popupTextSize shouldBe 0f
                decoded.popupFont shouldBe emptyList()
            }

            Then("malformed modern values use defaults instead of reviving legacy settings") {
                val decoded = style(
                    "popup_width" to Node.Mapping(),
                    "popup_height" to Node.Scalar("invalid"),
                    "popup_key_height" to Node.Mapping(),
                    "popup_text_size" to Node.Mapping(),
                    "popup_font" to Node.Scalar("modern-scalar.ttf"),
                )
                decoded.popupWidth shouldBe GeneralStyle.DEFAULTS.popupWidth
                decoded.popupHeight shouldBe GeneralStyle.DEFAULTS.popupHeight
                decoded.popupKeyHeight shouldBe GeneralStyle.DEFAULTS.popupKeyHeight
                decoded.popupTextSize shouldBe GeneralStyle.DEFAULTS.popupTextSize
                decoded.popupFont shouldBe emptyList()
            }
        }

        Given("a third-party legacy theme with preview font lists") {
            Then("the popup retains font order and explicit legacy zero values") {
                val style = GeneralStyle.decode(
                    Node.Mapping(
                        Node.Scalar("preview_height") to Node.Scalar("0"),
                        Node.Scalar("preview_text_size") to Node.Scalar("0"),
                        Node.Scalar("preview_font") to Node.Sequence(Node.Scalar("a.ttf"), Node.Scalar("b.ttf")),
                    ),
                )
                style.popupHeight shouldBe 0
                style.popupKeyHeight shouldBe 0
                style.popupTextSize shouldBe 0f
                style.popupFont shouldBe listOf("a.ttf", "b.ttf")
            }
        }
    })
