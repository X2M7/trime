// SPDX-FileCopyrightText: 2015 - 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.data.theme

import com.osfans.trime.data.theme.model.PresetKey
import com.osfans.trime.ime.keyboard.KeyCode
import com.osfans.trime.ime.keyboard.KeyCodeTestSupport
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import timber.log.Timber

class KeyActionManagerTest :
    BehaviorSpec({
        val platformLookup = KeyCode.androidKeyNameToCode
        beforeSpec { KeyCode.androidKeyNameToCode = KeyCodeTestSupport::androidKeyNameToCode }
        afterSpec { KeyCode.androidKeyNameToCode = platformLookup }

        fun diagnostics(vararg presets: Pair<String, PresetKey>) = KeyActionManager.presetDiagnostics(mapOf(*presets))

        given("the built-in themes") {
            `when`("every preset is inspected at activation") {
                then("every send resolves, including real Android key constants") {
                    listOf("trime.yaml", "tongwenfeng.trime.yaml").forEach { file ->
                        val theme = ThemeTestSupport.decodeBuiltinTheme(file)
                        KeyActionManager.presetDiagnostics(theme.presetKeys).shouldBeEmpty()
                    }
                }
            }
        }

        given("a preset with a send value") {
            `when`("the send does not resolve to a key") {
                then("the named diagnostic is returned without a duplicate parser log") {
                    val messages = mutableListOf<String>()
                    val tree = object : Timber.Tree() {
                        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                            messages += message
                        }
                    }
                    Timber.plant(tree)
                    try {
                        diagnostics("broken" to PresetKey(send = "Bogus_Key")) shouldBe
                            listOf("preset 'broken' has an unrecognized send 'Bogus_Key'")
                        messages.shouldBeEmpty()
                    } finally {
                        Timber.uproot(tree)
                    }
                }
            }
            `when`("the send resolves to a key") {
                then("it is not reported") {
                    diagnostics("fine" to PresetKey(send = "q")).shouldBeEmpty()
                }
            }
            `when`("an unknown send is spelled like an Android constant") {
                then("uppercase spelling cannot hide an invalid binding") {
                    diagnostics("broken_platform" to PresetKey(send = "BOGUS_KEY")) shouldBe
                        listOf("preset 'broken_platform' has an unrecognized send 'BOGUS_KEY'")
                }
            }
            `when`("a modifier is followed by no recognized key") {
                then("the preset is still reported") {
                    diagnostics("broken_chord" to PresetKey(send = "Control+Bogus_Key")) shouldBe
                        listOf("preset 'broken_chord' has an unrecognized send 'Control+Bogus_Key'")
                }
            }
            `when`("the send is empty") {
                then("the preset is left to the command or text fields") {
                    diagnostics(
                        "cmd" to PresetKey(command = "run"),
                        "text" to PresetKey(text = "hello"),
                    ).shouldBeEmpty()
                }
            }
        }
    })
