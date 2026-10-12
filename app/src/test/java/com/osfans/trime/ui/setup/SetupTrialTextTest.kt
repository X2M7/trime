/*
 * SPDX-FileCopyrightText: 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ui.setup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupTrialTextTest {
    @Test fun rejectsUnfinishedPinyinDigitsAndEmptyTrial() {
        listOf(null, "", "nihao", "64426", "Android 16", "🙂").forEach {
            assertFalse(containsCommittedChinese(it))
        }
    }

    @Test fun acceptsSimplifiedTraditionalAndSupplementaryHanCommit() {
        listOf("你好", "汉字", "漢字", "B站", String(Character.toChars(0x20000))).forEach {
            assertTrue(containsCommittedChinese(it))
        }
    }

    @Test fun findsHanAfterSupplementaryNonHanWithoutBreakingSurrogatePairs() {
        assertTrue(containsCommittedChinese("🙂你好"))
        assertFalse(containsCommittedChinese("🙂 Wi-Fi"))
    }
}
