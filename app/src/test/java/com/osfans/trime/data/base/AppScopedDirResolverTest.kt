// SPDX-FileCopyrightText: 2015 - 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.data.base

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files
import kotlin.io.path.createTempDirectory

class AppScopedDirResolverTest :
    StringSpec({
        "null parent stays null" {
            resolveWritableChildDir(null, "rime").shouldBeNull()
        }

        "writable parent yields a writable child dir" {
            val parent = createTempDirectory().toFile()
            try {
                val resolved = resolveWritableChildDir(parent, "rime")
                resolved.shouldNotBeNull()
                resolved.canWrite() shouldBe true
                resolved.name shouldBe "rime"
                resolved.parentFile?.canonicalFile shouldBe parent.canonicalFile
            } finally {
                parent.deleteRecursively()
            }
        }

        "later real parent succeeds after a null parent attempt" {
            resolveWritableChildDir(null, "shared").shouldBeNull()
            val parent = createTempDirectory().toFile()
            try {
                val resolved = resolveWritableChildDir(parent, "shared")
                resolved.shouldNotBeNull()
                resolved.canWrite() shouldBe true
            } finally {
                parent.deleteRecursively()
            }
        }

        "missing child is created under parent" {
            val parent = createTempDirectory().toFile()
            try {
                val child = File(parent, "rime")
                child.exists() shouldBe false
                val resolved = resolveWritableChildDir(parent, "rime")
                resolved.shouldNotBeNull()
                child.exists() shouldBe true
            } finally {
                parent.deleteRecursively()
            }
        }

        "ordinary file cannot masquerade as a parent directory" {
            val parent = createTempDirectory().toFile()
            try {
                val file = File(parent, "not-a-directory").also { it.writeText("data") }
                resolveWritableChildDir(file, "rime").shouldBeNull()
            } finally {
                parent.deleteRecursively()
            }
        }

        "ordinary file cannot masquerade as the requested child directory" {
            val parent = createTempDirectory().toFile()
            try {
                File(parent, "rime").writeText("data")
                resolveWritableChildDir(parent, "rime").shouldBeNull()
            } finally {
                parent.deleteRecursively()
            }
        }

        "child name cannot escape the canonical parent" {
            val parent = createTempDirectory().toFile()
            val outside = File(parent.parentFile, "escape-${parent.name}")
            try {
                resolveWritableChildDir(parent, "../${outside.name}").shouldBeNull()
                outside.exists() shouldBe false
            } finally {
                outside.deleteRecursively()
                parent.deleteRecursively()
            }
        }

        "symlink child cannot redirect outside the canonical parent" {
            val parent = createTempDirectory().toFile()
            val outside = createTempDirectory().toFile()
            try {
                Files.createSymbolicLink(File(parent, "rime").toPath(), outside.toPath())
                resolveWritableChildDir(parent, "rime").shouldBeNull()
            } finally {
                parent.deleteRecursively()
                outside.deleteRecursively()
            }
        }

        "symlink child cannot alias a sibling inside the canonical parent" {
            val parent = createTempDirectory().toFile()
            try {
                val sibling = File(parent, "other").also { it.mkdir() }
                Files.createSymbolicLink(File(parent, "rime").toPath(), sibling.toPath())
                resolveWritableChildDir(parent, "rime").shouldBeNull()
            } finally {
                parent.deleteRecursively()
            }
        }

        "security denial is reported as temporarily unavailable" {
            val denied = object : File("denied") {
                override fun exists(): Boolean = throw SecurityException("denied")
            }
            resolveWritableChildDir(denied, "rime").shouldBeNull()
        }
    })
