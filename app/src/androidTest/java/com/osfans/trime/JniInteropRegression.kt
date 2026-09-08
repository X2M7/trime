// SPDX-License-Identifier: GPL-3.0-or-later
package com.osfans.trime

import com.osfans.trime.core.RimeConfig
import com.osfans.trime.data.base.DataManager
import com.osfans.trime.data.opencc.OpenCCDictManager
import java.io.File
import java.util.UUID

internal object JniInteropRegression {
    fun verify() {
        val supplementary = "\uD842\uDFB7\uD83C\uDF0F"
        val config = File(DataManager.sharedDataDir, "opencc/t2s.json")
        check(config.isFile)
        repeat(20) {
            check(OpenCCDictManager.openCCLineConv("漢語$supplementary", config.path) == "汉语$supplementary")
            for (text in listOf("a\u0000b", "\u0000", "\u0000a\u0000", "a\u0000\u0000b")) {
                check(OpenCCDictManager.openCCLineConv(text, config.path) == text)
            }
        }
        check(OpenCCDictManager.openCCLineConv("a\uD800x\uDC00", config.path) == "a\uFFFDx\uFFFD")
        val id = "__jni_${UUID.randomUUID()}_$supplementary"
        val source = File(DataManager.userDataDir, "$id.yaml")
        val content = File.createTempFile("__jni", ".yaml", DataManager.userDataDir)
        fun quote(value: String) = "'${value.replace("'", "'\\''")}'"
        fun nativeShell(script: String) {
            val process = ProcessBuilder("/system/bin/sh").redirectErrorStream(true).start()
            process.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(script) }
            val output = process.inputStream.bufferedReader().use { it.readText() }
            check(process.waitFor() == 0) { "Native UTF-8 fixture failed: $output" }
        }
        try {
            // Android 5's Java file API encodes supplementary filenames differently
            // from native UTF-8. Feed bytes through stdin so this still tests a real
            // Unicode config path on every API, rather than skipping the JNI check.
            content.writeText("value: '$supplementary'\n")
            nativeShell("cat ${quote(content.path)} > ${quote(source.path)}\n")
            RimeConfig.openUserConfig(id).use { check(it.getString("value") == supplementary) }
            // OpenCC must return with the Java exception pending, without making
            // another JNI allocation (CheckJNI on API 21 catches that misuse).
            val missing = File(DataManager.userDataDir, "$id-missing.json")
            val failure = runCatching { OpenCCDictManager.openCCLineConv("test", missing.path) }.exceptionOrNull()
            check(failure is Exception && failure.message?.contains(supplementary) == true)
            check(OpenCCDictManager.openCCLineConv("漢語", config.path) == "汉语")
        } finally {
            try {
                nativeShell("rm -f ${quote(source.path)}\n")
            } finally {
                check(content.delete())
            }
        }
    }
}
