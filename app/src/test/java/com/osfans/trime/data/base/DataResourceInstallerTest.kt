// SPDX-License-Identifier: GPL-3.0-or-later
package com.osfans.trime.data.base

import com.osfans.trime.data.sync.AtomicLocalFileCopy
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.IOException
import kotlin.io.path.createTempDirectory

class DataResourceInstallerTest :
    StringSpec({
        "nested removal preserves the top-level namesake and user data" {
            val root = createTempDirectory().toFile()
            try {
                val shared = root.resolve("shared").apply { mkdirs() }
                shared.resolve("opencc").mkdirs()
                shared.resolve("opencc/old.txt").writeText("old")
                shared.resolve("old.txt").writeText("keep namesake")
                root.resolve("rime").mkdirs()
                root.resolve("rime/old.txt").writeText("user")
                var published = false
                DataResourceInstaller.install(
                    shared,
                    listOf(DataDiff.DeleteFile("shared/opencc/old.txt")),
                    { _, _ -> error("No copy expected") },
                    { published = true },
                )
                published shouldBe true
                shared.resolve("opencc/old.txt").exists() shouldBe false
                shared.resolve("old.txt").readText() shouldBe "keep namesake"
                root.resolve("rime/old.txt").readText() shouldBe "user"
            } finally {
                root.deleteRecursively()
            }
        }

        "failed copy preserves old content and checksum and succeeds on retry" {
            val shared = createTempDirectory().toFile()
            try {
                val dest = shared.resolve("dict.yaml").apply { writeText("old") }
                val changes = listOf(DataDiff.UpdateFile("shared/dict.yaml"))
                var published = false
                shouldThrow<IOException> {
                    DataResourceInstaller.install(shared, changes, { _, file ->
                        AtomicLocalFileCopy.writeFromStream(file) {
                            it.write("partial".toByteArray())
                            throw IOException("disk full")
                        }
                    }, { published = true })
                }
                published shouldBe false
                dest.readText() shouldBe "old"
                DataResourceInstaller.install(shared, changes, { _, file ->
                    AtomicLocalFileCopy.writeFromStream(file) { it.write("new".toByteArray()) }
                }, { published = true })
                published shouldBe true
                dest.readText() shouldBe "new"
                shared.listFiles()!!.map { it.name } shouldBe listOf("dict.yaml")
            } finally {
                shared.deleteRecursively()
            }
        }

        "invalid paths reject the complete plan before any file is changed" {
            val shared = createTempDirectory().toFile()
            try {
                shared.resolve("keep.txt").writeText("keep")
                for (path in listOf("rime/userdb", "shared/../rime/userdb", "shared/opencc/../../rime/userdb", "shared")) {
                    shouldThrow<RuntimeException> {
                        DataResourceInstaller.install(
                            shared,
                            listOf(DataDiff.DeleteFile("shared/keep.txt"), DataDiff.DeleteFile(path)),
                            { _, _ -> error("No copy expected") },
                            { error("Must not publish") },
                        )
                    }
                    shared.resolve("keep.txt").readText() shouldBe "keep"
                }
            } finally {
                shared.deleteRecursively()
            }
        }
    })
