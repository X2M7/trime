// SPDX-FileCopyrightText: 2015 - 2025 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.data.sync

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID

object AtomicLocalFileCopy {
    fun writeFromStream(
        destFile: File,
        copy: (OutputStream) -> Unit,
    ): Long {
        val parent = destFile.parentFile ?: error("No parent for ${destFile.path}")
        check(!destFile.exists() || destFile.isFile) { "Destination is not a file: ${destFile.path}" }
        val operationId = UUID.randomUUID().toString()
        val incoming = File(parent, ".trime-new-$operationId.tmp")
        val backup = File(parent, ".trime-bak-$operationId.tmp")
        parent.mkdirs()
        var backedUp = false
        try {
            FileOutputStream(incoming).use { output ->
                copy(output)
            }
            val expectedBytes = incoming.length()

            if (destFile.exists()) {
                if (!destFile.renameTo(backup)) {
                    error("Failed to back up ${destFile.path}")
                }
                backedUp = true
            }

            check(incoming.renameTo(destFile)) { "Failed to publish ${destFile.path}" }

            if (backup.exists()) {
                backup.delete()
            }

            return expectedBytes
        } catch (e: Exception) {
            if (backedUp && backup.exists()) {
                runCatching {
                    check(backup.renameTo(destFile)) { "Recovery retained at ${backup.path}" }
                }.exceptionOrNull()?.let(e::addSuppressed)
            }
            throw e
        } finally {
            incoming.delete()
        }
    }

    fun copyFromInput(
        source: FileInputStream,
        destFile: File,
    ): Long = writeFromStream(destFile) { output ->
        source.copyTo(output)
    }
}
