// SPDX-FileCopyrightText: 2015 - 2025 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.data.sync

import com.osfans.trime.util.FileUtils
import timber.log.Timber
import java.io.File

object OrphanCleaner {
    data class Result(
        val deleted: Int = 0,
        val failed: Int = 0,
    )

    fun removeLocalOrphans(
        root: File,
        externalPaths: Set<String>,
        ownId: String? = null,
        syncDir: String = SyncPathPolicy.DEFAULT_SYNC_DIR,
        knownEntries: Map<String, SyncEntry> = emptyMap(),
    ): Result {
        if (!root.exists()) return Result()
        var deleted = 0
        var failed = 0
        val emptiedParents = mutableSetOf<File>()
        root
            .walkBottomUp()
            .onEnter {
                SyncRelativePath.isDirectPath(root, it) &&
                    !SafTreeWalker.shouldSkip(it.relativeTo(root).path, isDirectory = true)
            }
            .filter { it != root }
            .filter { SyncRelativePath.isDirectPath(root, it) }
            .filter {
                val relative = it.relativeTo(root).path.replace('\\', '/')
                !SafTreeWalker.shouldSkip(relative, it.isDirectory)
            }.forEach { file ->
                val relative =
                    runCatching {
                        SyncRelativePath.normalize(file.relativeTo(root).path.replace('\\', '/'))
                    }.getOrElse {
                        Timber.w(it, "Skip orphan cleanup for unsafe path")
                        return@forEach
                    }
                when {
                    file.isFile && SyncPathPolicy.shouldPreserveLocal(relative, ownId, syncDir) -> Unit
                    file.isFile && relative !in externalPaths && knownEntries[relative]?.let {
                        it.size >= 0 && it.lastModified > 0 &&
                            file.length() == it.size && file.lastModified() == it.lastModified &&
                            it.localSha256 != null && runCatching { SyncFingerprint.of(file) }.getOrNull() == it.localSha256
                    } == true -> {
                        val deleteResult = FileUtils.delete(file)
                        if (deleteResult.isSuccess) {
                            deleted++
                            var parent = file.parentFile
                            while (parent != null && parent != root) {
                                emptiedParents.add(parent)
                                parent = parent.parentFile
                            }
                            Timber.i("Delete orphan $relative")
                        } else {
                            failed++
                            Timber.w(deleteResult.exceptionOrNull(), "Failed to delete orphan $relative")
                        }
                    }
                    file.isDirectory && file in emptiedParents && file.list()?.isEmpty() == true -> {
                        if (file.delete()) {
                            deleted++
                        } else {
                            failed++
                            Timber.w("Failed to delete empty directory $relative")
                        }
                    }
                }
            }
        return Result(deleted = deleted, failed = failed)
    }
}
