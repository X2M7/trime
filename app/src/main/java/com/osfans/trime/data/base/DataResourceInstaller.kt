// SPDX-License-Identifier: GPL-3.0-or-later
package com.osfans.trime.data.base

import com.osfans.trime.data.sync.SyncRelativePath
import com.osfans.trime.util.FileUtils
import java.io.File

internal object DataResourceInstaller {
    fun install(
        sharedDir: File,
        changes: List<DataDiff>,
        copyAsset: (String, File) -> Unit,
        publishChecksums: () -> Unit,
    ) {
        // Validate the whole plan before modifying any file. Never resolve an
        // asset into the adjacent runtime user directory or flatten subpaths.
        val plan = changes.sortedByDescending { it.ordinal }.map { change ->
            require(change.path.startsWith("shared/")) { "Not a shared resource: ${change.path}" }
            change to SyncRelativePath.resolveContained(sharedDir, change.path.removePrefix("shared/"))
        }
        plan.forEach { (change, destination) ->
            when (change) {
                is DataDiff.CreateFile, is DataDiff.UpdateFile -> copyAsset(change.path, destination)
                is DataDiff.DeleteDir, is DataDiff.DeleteFile -> FileUtils.delete(destination).getOrThrow()
            }
        }
        publishChecksums()
    }
}
