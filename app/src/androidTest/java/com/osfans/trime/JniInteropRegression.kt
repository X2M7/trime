// SPDX-License-Identifier: GPL-3.0-or-later
package com.osfans.trime

import android.util.Log
import com.osfans.trime.core.RimeConfig
import com.osfans.trime.data.base.DataManager
import com.osfans.trime.data.opencc.OpenCCDictManager
import java.io.File
import java.util.UUID

internal object JniInteropRegression {
    fun verify() {
        verifyDictionaryConversions()
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

    private fun verifyDictionaryConversions() {
        val directory = File(DataManager.userDataDir, "__jni_opencc_${UUID.randomUUID()}")
        check(directory.mkdir())
        try {
            val source = directory.resolve("source.tmp")
            val binary = directory.resolve("dictionary.tmp")
            val roundTrip = directory.resolve("roundtrip.tmp")
            val truncated = directory.resolve("truncated.tmp")
            val entries = setOf("漢語\t汉语", "測試\t测试")
            fun verifyRoundTrip() {
                source.writeText(entries.joinToString("\n", postfix = "\n"))
                OpenCCDictManager.openCCDictConv(source.path, binary.path, OpenCCDictManager.MODE_TXT_TO_BIN)
                check(binary.length() > 0) { "OpenCC .tmp conversion produced no dictionary" }
                OpenCCDictManager.openCCDictConv(binary.path, roundTrip.path, OpenCCDictManager.MODE_BIN_TO_TXT)
                check(roundTrip.readLines().toSet() == entries) { "OpenCC .tmp roundtrip changed dictionary entries" }
            }
            verifyRoundTrip()
            Log.i("JniInteropRegression", "PASS: OpenCC explicit-format .tmp roundtrip")
            val complete = binary.readBytes()
            val headerSize = "OPENCC_MARISA_0.2.5".length
            // This fixture uses the bundled OCD2/Marisa format. The second cut
            // retains the 16-byte Marisa header but truncates its first data block.
            val cuts = listOf("header" to headerSize - 1, "trie" to headerSize + 16 + 1, "values-tail" to complete.size - 1)
            for ((label, size) in cuts) {
                check(size in 1 until complete.size)
                truncated.writeBytes(complete.copyOf(size))
                roundTrip.writeText("previous output")
                val failure = runCatching {
                    OpenCCDictManager.openCCDictConv(truncated.path, roundTrip.path, OpenCCDictManager.MODE_BIN_TO_TXT)
                }.exceptionOrNull()
                check(failure is Exception) { "OpenCC accepted nonempty $label truncation" }
                check(roundTrip.readText() == "previous output") { "Invalid OpenCC $label input replaced the output" }
                verifyRoundTrip()
                Log.i("JniInteropRegression", "PASS: OpenCC nonempty $label truncation rejected and conversion recovered")
            }
            source.writeText("missing tab and value\n")
            val retained = binary.readBytes()
            val malformed = runCatching {
                OpenCCDictManager.openCCDictConv(source.path, binary.path, OpenCCDictManager.MODE_TXT_TO_BIN)
            }.exceptionOrNull()
            check(malformed is Exception && malformed.message?.contains("Tabular not found") == true) {
                "Malformed text dictionary did not return the OpenCC parse exception"
            }
            check(binary.readBytes().contentEquals(retained)) { "Invalid text dictionary replaced the output" }
            verifyRoundTrip()
            Log.i("JniInteropRegression", "PASS: OpenCC malformed text rejected and conversion recovered")
        } finally {
            check(directory.deleteRecursively()) { "OpenCC JNI regression files were not removed" }
        }
    }
}
