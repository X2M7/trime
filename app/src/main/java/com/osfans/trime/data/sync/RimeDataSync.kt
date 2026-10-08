/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.data.sync

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import com.osfans.trime.TrimeApplication
import com.osfans.trime.data.base.DataManager
import com.osfans.trime.data.prefs.AppPrefs
import com.osfans.trime.util.DeployNotification
import com.osfans.trime.util.FileUtils
import com.osfans.trime.util.appContext
import io.planck.storageaccess.StorageAccess
import io.planck.storageaccess.StorageDocument
import io.planck.storageaccess.StorageWalkEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.ArrayDeque
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
internal data class SyncEntry(
    val size: Long,
    val lastModified: Long,
    val localSha256: String? = null,
)

@Serializable
internal data class SyncIndexData(
    val treeUri: String = "",
    val entries: Map<String, SyncEntry> = emptyMap(),
)

internal data class RuntimeStorageChoice(
    val mode: DataStorageMode,
    val treeUri: String,
)

/** Direct boot always uses the isolated device-protected hierarchy and never a persisted SAF tree. */
internal fun resolveRuntimeStorageChoice(
    configuredMode: DataStorageMode,
    configuredTreeUri: String,
    isDirectBootMode: Boolean,
): RuntimeStorageChoice = if (isDirectBootMode) {
    RuntimeStorageChoice(DataStorageMode.APP_STORAGE, "")
} else {
    RuntimeStorageChoice(configuredMode, configuredTreeUri)
}

/**
 * Mirrors the user data dir against the folder the user picked in [DataStorageMode.EXTERNAL_SYNC]:
 * files are imported before a rime maintenance and exported after a successful one.
 */
object RimeDataSync {
    private const val DEFAULT_MIME = "application/octet-stream"

    internal interface ExternalFileOperations {
        suspend fun list(dirUri: Uri): List<StorageDocument>

        suspend fun writeFile(
            dirUri: Uri,
            name: String,
            mime: String,
            write: suspend (OutputStream) -> Unit,
        ): StorageDocument

        suspend fun rename(
            uri: Uri,
            name: String,
        ): StorageDocument

        suspend fun delete(uri: Uri)

        suspend fun supportsRename(uri: Uri): Boolean

        suspend fun digest(uri: Uri): ByteArray
    }

    private object StorageExternalFileOperations : ExternalFileOperations {
        override suspend fun list(dirUri: Uri): List<StorageDocument> = SafDirectoryListing.list(appContext.contentResolver, dirUri)

        override suspend fun writeFile(
            dirUri: Uri,
            name: String,
            mime: String,
            write: suspend (OutputStream) -> Unit,
        ): StorageDocument = StorageAccess.writeFile(
            dirUri = dirUri,
            name = name,
            mime = mime,
        ) { output -> write(output) }

        override suspend fun rename(
            uri: Uri,
            name: String,
        ): StorageDocument = StorageAccess.rename(uri, name)

        override suspend fun delete(uri: Uri) = StorageAccess.delete(uri)

        override suspend fun supportsRename(uri: Uri): Boolean = appContext.contentResolver
            .query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
                null,
                null,
                null,
            )?.use { cursor ->
                check(cursor.moveToFirst()) { "Missing document: $uri" }
                cursor.getInt(0) and DocumentsContract.Document.FLAG_SUPPORTS_RENAME != 0
            } ?: error("Cannot query document: $uri")

        override suspend fun digest(uri: Uri): ByteArray = StorageAccess.readFile(uri) { SyncFingerprint.digest(it) }
    }

    /** Internal rime directories that never take part in the sync. */
    private const val SKIP_DIR = "build"
    private const val SKIP_DIR_SUBSTRING = ".userdb"
    private val operationArtifact =
        Regex("\\.trime-(?:replace|new|bak)-[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}\\.(?:tmp|bak)")

    private val parallelism = Runtime.getRuntime().availableProcessors().coerceIn(4, 8)

    private val prefs get() = AppPrefs.defaultInstance().profile

    // region Storage mode and picked folder

    private fun isDirectBootMode(): Boolean = TrimeApplication.getInstance().isDirectBootMode

    private fun runtimeStorageChoice(): RuntimeStorageChoice = resolveRuntimeStorageChoice(
        configuredMode = prefs.dataStorageMode.getValue(),
        configuredTreeUri = prefs.externalRimeTreeUri.getValue(),
        isDirectBootMode = isDirectBootMode(),
    )

    fun usesExternalSync(): Boolean = runtimeStorageChoice().mode == DataStorageMode.EXTERNAL_SYNC

    fun treeUri(): Uri? = runtimeStorageChoice().treeUri.takeIf { it.isNotBlank() }?.let(Uri::parse)

    fun hasExternalAccess(): Boolean = try {
        val uriString = treeUri()?.toString() ?: return false
        StorageAccess.persistedPermissions().any {
            it.uri.toString() == uriString && it.read && it.write
        }
    } catch (_: SecurityException) {
        false
    } catch (_: IllegalStateException) {
        false
    }

    fun isRuntimeReady(): Boolean = try {
        DataManager.resolvedUserDataDir() != null && DataManager.resolvedSharedDataDir() != null
    } catch (_: IllegalStateException) {
        false
    } catch (_: SecurityException) {
        false
    }

    fun isStorageAvailable(): Boolean = isRuntimeReady() && (!usesExternalSync() || hasExternalAccess())

    /**
     * Whether the user finished the storage-mode setup step.
     *
     * This does not require runtime dirs under `Context.getExternalFilesDir`
     * to be writable yet, so late media after reboot does not reopen the setup wizard.
     */
    internal fun isStorageChoiceComplete(
        mode: DataStorageMode,
        treeUri: String,
    ): Boolean = when (mode) {
        DataStorageMode.APP_STORAGE -> true
        DataStorageMode.EXTERNAL_SYNC -> treeUri.isNotBlank()
    }

    fun isStorageChoiceDone(): Boolean = runtimeStorageChoice().let { isStorageChoiceComplete(it.mode, it.treeUri) }

    /**
     * Remembers the folder picked by the user.
     *
     * The grant itself is already persisted by the picker; this only records which
     * folder the app should sync with and drops the grant of the previous one.
     */
    suspend fun persistTreeUri(uri: Uri) {
        check(!isDirectBootMode()) { "A SAF data folder cannot be selected during direct boot" }
        val previous = treeUri()
        val changed = previous?.toString() != uri.toString()
        try {
            if (changed) {
                saveIndex(SyncIndexData(treeUri = uri.toString()))
            }
            prefs.externalRimeTreeUri.setValue(uri.toString())
        } catch (failure: Exception) {
            if (changed) {
                runCatching { StorageAccess.releasePersistedPermission(uri) }
                    .exceptionOrNull()
                    ?.let(failure::addSuppressed)
            }
            throw failure
        }
        if (changed && previous != null) {
            runCatching { StorageAccess.releasePersistedPermission(previous) }
                .onFailure { Timber.w(it, "Unable to release the previous data folder") }
        }
    }

    /**
     * Continues with app-specific storage. The picked folder and its grant are kept, so switching
     * back to external sync does not ask the user to pick a folder again.
     */
    fun fallbackToAppStorage(reason: Throwable? = null) {
        if (!usesExternalSync()) return
        Timber.w(reason, "External sync unavailable; falling back to app-specific storage")
        prefs.dataStorageMode.setValue(DataStorageMode.APP_STORAGE)
        DeployNotification.showExternalSyncFallback()
    }

    // endregion

    // region Sync entry points

    suspend fun importToLocal(
        keepNotificationUntilDeploySuccess: Boolean = false,
        showProgress: Boolean = true,
    ): Result<SyncStats> {
        if (!usesExternalSync()) {
            return Result.success(SyncStats())
        }
        if (showProgress) DeployNotification.showProgress()
        var deferNotificationCleanup = false
        try {
            return withContext(Dispatchers.IO) {
                runCatching {
                    val treeUri = treeUri() ?: error("No data path selected")
                    check(hasExternalAccess()) { "No access to data path" }
                    val destRoot = DataManager.userDataDir
                    val index = loadIndex()
                    // LevelDB files may be open in the engine. Runtime synchronization always
                    // uses portable text dumps and never replaces a binary user database.
                    val skipUserDb = true
                    val ownId = SyncPathPolicy.readOwnInstallationId()
                    val syncDir =
                        SyncPathPolicy.treeRelativeSyncDir(
                            SyncPathPolicy.readOwnSyncDir(),
                            destRoot,
                        )
                    val skipPrefix =
                        ownId?.takeIf { it.isNotEmpty() }?.let {
                            runCatching { SyncPathPolicy.ownSyncPrefix(it, syncDir) }.getOrNull()
                        }
                    val entries = listExternalEntries(treeUri, skipUserDb, skipPrefix)
                    val externalPaths = entries.map { it.relativePath }.toSet()
                    val toCopy = entries.filter {
                        !it.file.isDir && SyncPathPolicy.shouldImport(it.relativePath, ownId, syncDir)
                    }
                    val createdDirs = LocalDirectoryGate()
                    val copyResults =
                        mapParallel(toCopy) { entry ->
                            importExternalFile(entry, destRoot, index, createdDirs)
                        }
                    val importStats = mergeStats(copyResults.map { it.result })
                    val complete = importStats.failed == 0
                    // A partial listing/copy must never be interpreted as proof that local
                    // files disappeared from the external tree.
                    val removeResult =
                        if (complete) {
                            removeLocalOrphans(
                                root = destRoot,
                                externalPaths = externalPaths,
                                ownId = ownId,
                                syncDir = syncDir,
                                knownEntries = index.entries,
                            )
                        } else {
                            CopyResult()
                        }
                    // Retire deletion history only after a complete import. This prevents a
                    // later unrelated local file from inheriting an old tombstone.
                    val history =
                        if (complete) index.entries.filterKeys { it in externalPaths } else index.entries
                    saveIndex(withCurrentTree(mergeIndexEntries(history, copyResults)))
                    val stats = importStats + removeResult
                    DeployNotification.notifyPartialCopyIfNeeded(
                        stats.failed,
                        "importToLocal",
                    )
                    check(stats.failed == 0) { "Failed to import ${stats.failed} file(s)" }
                    stats
                }.onFailure {
                    if (it is CancellationException) throw it
                    Timber.e(it, "importToLocal failed")
                }
            }.also { result ->
                deferNotificationCleanup = keepNotificationUntilDeploySuccess && result.isSuccess
            }
        } finally {
            if (!deferNotificationCleanup) DeployNotification.cancel()
        }
    }

    suspend fun exportConfigFilesToExternal(): Result<SyncStats> = withContext(Dispatchers.IO) {
        if (!usesExternalSync()) {
            return@withContext Result.success(SyncStats())
        }
        runCatching {
            Timber.d(
                "exportConfigFilesToExternal: exporting ${DataManager.POST_SCHEMA_DEPLOY_EXPORT_FILES}",
            )
            val treeUri = treeUri() ?: error("No data path selected")
            check(hasExternalAccess()) { "No access to data path" }
            val srcRoot = DataManager.userDataDir
            val index = loadIndex()
            val copyResults =
                DataManager.POST_SCHEMA_DEPLOY_EXPORT_FILES.map { fileName ->
                    val sourceFile = srcRoot.resolve(fileName)
                    if (!sourceFile.isFile) {
                        Timber.w("Skip exporting missing config file: $fileName")
                        IndexedCopyResult(CopyResult(skipped = 1), null)
                    } else {
                        exportLocalFile(
                            sourceFile = sourceFile,
                            destDirUri = treeUri,
                            relativePath = fileName,
                            index = index,
                            force = true,
                        )
                    }
                }
            saveIndex(withCurrentTree(mergeIndexEntries(index.entries, copyResults)))
            mergeStats(copyResults.map { it.result }).also { stats ->
                Timber.d(
                    "exportConfigFilesToExternal: copied=${stats.copied}, " +
                        "skipped=${stats.skipped}, failed=${stats.failed}",
                )
                check(stats.failed == 0) { "Failed to export ${stats.failed} config file(s)" }
            }
        }.onFailure {
            if (it is CancellationException) throw it
            Timber.e(it, "exportConfigFilesToExternal failed")
        }
    }

    suspend fun importThemeToLocal(configId: String): Result<SyncStats> = withContext(Dispatchers.IO) {
        runCatching {
            if (!usesExternalSync() || !hasExternalAccess()) {
                return@runCatching SyncStats()
            }
            val treeUri = treeUri() ?: return@runCatching SyncStats()
            val themeFile = uniqueChild(treeUri, "$configId.yaml")
            if (themeFile == null) {
                Timber.d("Theme file '$configId.yaml' not found at external root, skip import")
                return@runCatching SyncStats()
            }
            val index = loadIndex()
            val copyResult =
                importExternalFile(
                    entry = StorageWalkEntry(themeFile, themeFile.name),
                    destRoot = DataManager.userDataDir,
                    index = index,
                    createdDirs = LocalDirectoryGate(),
                )
            saveIndex(withCurrentTree(mergeIndexEntries(index.entries, listOf(copyResult))))
            val stats = mergeStats(listOf(copyResult.result))
            DeployNotification.notifyPartialCopyIfNeeded(
                stats.failed,
                "importThemeToLocal for '$configId'",
            )
            check(stats.failed == 0) { "Failed to import theme: $configId" }
            stats
        }.onFailure {
            if (it is CancellationException) throw it
            Timber.e(it, "importThemeToLocal failed for '$configId'")
        }
    }

    suspend fun exportToExternal(): Result<SyncStats> = withContext(Dispatchers.IO) {
        if (!usesExternalSync()) {
            return@withContext Result.success(SyncStats())
        }
        runCatching {
            val treeUri = treeUri() ?: error("No data path selected")
            check(hasExternalAccess()) { "No access to data path" }
            val ownId = SyncPathPolicy.readOwnInstallationId()
            if (ownId.isNullOrEmpty()) {
                Timber.w("Export skipped: installation_id unreadable")
                return@runCatching SyncStats()
            }
            val syncDirRaw = SyncPathPolicy.readOwnSyncDir()
            val userDataDir = DataManager.userDataDir
            val syncDir = SyncPathPolicy.treeRelativeSyncDir(syncDirRaw, userDataDir)
            val exportPrefix =
                runCatching { SyncPathPolicy.ownSyncPrefix(ownId, syncDir) }.getOrElse { e ->
                    Timber.w(e, "Export skipped: invalid installation_id or sync_dir")
                    return@runCatching SyncStats()
                }
            val srcRoot = SyncPathPolicy.localOwnSyncDir(ownId, syncDirRaw, userDataDir)
            if (!srcRoot.isDirectory) {
                Timber.w("Export skipped: local sync dir does not exist: ${srcRoot.path}")
                return@runCatching SyncStats()
            }
            val index = loadIndex()
            val localFiles = listLocalFiles(srcRoot)
            if (localFiles.isEmpty()) {
                return@runCatching SyncStats()
            }
            val pending =
                localFiles.mapNotNull { file ->
                    val localRelative =
                        runCatching {
                            SyncRelativePath.normalize(file.relativeTo(srcRoot).path.replace('\\', '/'))
                        }.getOrNull() ?: return@mapNotNull null
                    file to SyncRelativePath.normalize("$exportPrefix/$localRelative")
                }
            val dirUris = ensureRemoteDirectories(treeUri, pending.map { (_, relativePath) -> relativePath })
            val copyResults =
                mapParallel(pending) { (file, relativePath) ->
                    exportLocalFile(
                        sourceFile = file,
                        destDirUri = dirUris[relativePath.substringBeforeLast('/', "")] ?: treeUri,
                        relativePath = relativePath,
                        index = index,
                        force = false,
                    )
                }
            saveIndex(withCurrentTree(mergeIndexEntries(index.entries, copyResults)))
            mergeStats(copyResults.map { it.result }).also { stats ->
                DeployNotification.notifyPartialCopyIfNeeded(
                    stats.failed,
                    "exportToExternal",
                )
                check(stats.failed == 0) { "Failed to export ${stats.failed} file(s)" }
            }
        }.onFailure {
            if (it is CancellationException) throw it
            Timber.e(it, "exportToExternal failed")
        }
    }

    // endregion

    // region Per-file copy

    /** Copies one external file into the local user data dir. */
    private suspend fun importExternalFile(
        entry: StorageWalkEntry,
        destRoot: File,
        index: SyncIndexData,
        createdDirs: LocalDirectoryGate,
    ): IndexedCopyResult {
        val relativePath = entry.relativePath
        val size = entry.file.length
        val lastModified = entry.file.lastModified
        val destFile =
            runCatching {
                SyncRelativePath.resolveContained(destRoot, relativePath)
            }.getOrElse {
                Timber.w(it, "Rejected unsafe import path $relativePath")
                return IndexedCopyResult(CopyResult(failed = 1), null)
            }
        return runCatching {
            if (!shouldCopy(relativePath, size, lastModified, index) &&
                destFile.exists() &&
                destFile.length() == size &&
                destFile.lastModified() >= lastModified
            ) {
                return@runCatching IndexedCopyResult(
                    CopyResult(skipped = 1),
                    relativePath to SyncEntry(
                        size,
                        lastModified,
                        index.entries[relativePath]?.localSha256,
                    ),
                )
            }
            destFile.parentFile?.let { parent ->
                val parentRelative = parent.relativeTo(destRoot).path.replace('\\', '/')
                if (parentRelative.isNotEmpty()) {
                    createdDirs.ensure(destRoot, SyncRelativePath.normalize(parentRelative))
                }
            }
            val copied = copyFromSaf(entry.file.uri, destFile, size)
            if (lastModified > 0) destFile.setLastModified(lastModified)
            // StorageAccess maps a provider's null COLUMN_SIZE to zero. Preserve the actual
            // byte count for deletion safety while still forcing a future copy when metadata
            // remains unknown.
            val recordedSize = if (size > 0 || copied.bytes == 0L) size else copied.bytes
            IndexedCopyResult(
                CopyResult(copied = 1, bytesCopied = copied.bytes),
                relativePath to SyncEntry(recordedSize, lastModified, copied.sha256),
            )
        }.getOrElse {
            if (it is CancellationException) throw it
            Timber.w(it, "Failed to import $relativePath")
            IndexedCopyResult(CopyResult(failed = 1), null)
        }
    }

    /** Copies one local file into the external [destDirUri]. */
    private suspend fun exportLocalFile(
        sourceFile: File,
        destDirUri: Uri,
        relativePath: String,
        index: SyncIndexData,
        force: Boolean,
    ): IndexedCopyResult {
        val size = sourceFile.length()
        val lastModified = sourceFile.lastModified()
        return runCatching {
            val changed = force || shouldCopy(relativePath, size, lastModified, index)
            if (!changed) {
                val remote = uniqueChild(destDirUri, sourceFile.name)
                if (remote != null && remote.length == size) {
                    return@runCatching IndexedCopyResult(
                        CopyResult(skipped = 1),
                        relativePath to SyncEntry(
                            size,
                            lastModified,
                            index.entries[relativePath]?.localSha256,
                        ),
                    )
                }
            }
            val written = replaceExternalFile(sourceFile, destDirUri, sourceFile.name)
            IndexedCopyResult(
                CopyResult(copied = 1, bytesCopied = written.bytes),
                relativePath to SyncEntry(size, lastModified, written.sha256),
            )
        }.getOrElse {
            if (it is CancellationException) throw it
            Timber.w(it, "Failed to export $relativePath")
            IndexedCopyResult(CopyResult(failed = 1), null)
        }
    }

    /** Creates every directory that the given [relativePaths] need, once per directory. */
    private suspend fun ensureRemoteDirectories(
        treeUri: Uri,
        relativePaths: List<String>,
    ): Map<String, Uri> {
        val resolved = mutableMapOf("" to treeUri)
        relativePaths
            .map { it.substringBeforeLast('/', "") }
            .filter { it.isNotEmpty() }
            .flatMap { relativeDir ->
                val segments = relativeDir.split('/')
                segments.indices.map { segments.take(it + 1).joinToString("/") }
            }
            .distinct()
            .sortedBy { it.count { c -> c == '/' } }
            .forEach { relativeDir ->
                val parentDir = relativeDir.substringBeforeLast('/', "")
                val name = relativeDir.substringAfterLast('/')
                val parentUri = checkNotNull(resolved[parentDir])
                val existing = uniqueChild(parentUri, name)
                val directory = existing ?: StorageAccess.mkdirp(parentUri, name)
                check(directory.isDir && directory.name == name) {
                    "Remote path is not a directory: $relativeDir"
                }
                resolved[relativeDir] = directory.uri
            }
        return resolved - ""
    }

    /** Finds one exact child and fails closed when a provider exposes an ambiguous name. */
    private suspend fun uniqueChild(
        dirUri: Uri,
        name: String,
    ): StorageDocument? {
        SyncRelativePath.requireSafeName(name)
        val matches = SafDirectoryListing.list(appContext.contentResolver, dirUri).filter { it.name == name }
        check(matches.size <= 1) { "Remote directory contains duplicate name: $name" }
        return matches.singleOrNull()
    }

    /**
     * Copies the SAF document [srcUri] into [destFile] through a temporary file next to it,
     * so that a reader of [destFile] never observes a partially written file.
     *
     * @return the number of bytes written
     */
    private suspend fun copyFromSaf(
        srcUri: Uri,
        destFile: File,
        expectedSize: Long,
    ): CopiedContent = StorageAccess.readFile(srcUri) { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = writeFromStream(destFile) { output ->
            val copied = input.copyTo(DigestOutputStream(output, digest))
            check(expectedSize <= 0 || copied == expectedSize) {
                "Incomplete document: copied $copied bytes, expected $expectedSize"
            }
        }
        CopiedContent(bytes, SyncFingerprint.hex(digest.digest()))
    }

    /**
     * Publishes a local file to SAF without truncating an existing document in place.
     * The staged document is read back before the old document is renamed, and a failed
     * replacement restores the operation-specific backup whenever the provider permits it.
     */
    private suspend fun replaceExternalFile(
        sourceFile: File,
        destDirUri: Uri,
        fileName: String,
        operations: ExternalFileOperations = StorageExternalFileOperations,
    ): CopiedContent {
        SyncRelativePath.requireSafeName(fileName)
        val existing = uniqueReplacementChild(operations, destDirUri, fileName)
        check(existing?.isDir != true) { "Destination is a directory: $fileName" }
        val operationId = UUID.randomUUID().toString()
        val stagedName = ".trime-replace-$operationId.tmp"
        val backupName = ".trime-replace-$operationId.bak"
        var staged: StorageDocument? = null
        var backup: StorageDocument? = null
        var published: StorageDocument? = null
        var directPublishAttempted = false
        var committed = false
        val digest = MessageDigest.getInstance("SHA-256")
        var copiedBytes = -1L
        try {
            staged =
                operations.writeFile(
                    dirUri = destDirUri,
                    name = stagedName,
                    mime = DEFAULT_MIME,
                ) { output ->
                    sourceFile.inputStream().use { input ->
                        copiedBytes = input.copyTo(DigestOutputStream(output, digest))
                    }
                }
            check(staged.name == stagedName) { "Provider changed staged document name" }
            check(copiedBytes == sourceFile.length()) {
                "Staged $fileName with $copiedBytes bytes, expected ${sourceFile.length()}"
            }
            val sha256 = SyncFingerprint.hex(digest.digest())
            verifyExternalFile(operations, staged, copiedBytes, sha256)

            if (existing != null) {
                backup = operations.rename(existing.uri, backupName)
                check(backup.name == backupName) { "Provider changed backup document name" }
            }
            if (existing == null && !operations.supportsRename(staged.uri)) {
                directPublishAttempted = true
                var publishedBytes = -1L
                published =
                    operations.writeFile(
                        dirUri = destDirUri,
                        name = fileName,
                        mime = DEFAULT_MIME,
                    ) { output ->
                        sourceFile.inputStream().use { input ->
                            publishedBytes = input.copyTo(output)
                        }
                    }
                check(publishedBytes == copiedBytes) {
                    "Published $fileName with $publishedBytes bytes, expected $copiedBytes"
                }
            } else {
                published = operations.rename(staged.uri, fileName)
                staged = null
            }
            check(published.name == fileName) { "Provider changed published document name" }
            verifyExternalFile(operations, published, copiedBytes, sha256)
            committed = true
            return CopiedContent(copiedBytes, sha256)
        } catch (failure: Exception) {
            withContext(NonCancellable) {
                if (!committed) {
                    var publishedRemoved = true
                    val failedPublished =
                        published ?: if (directPublishAttempted) {
                            runCatching {
                                uniqueReplacementChild(operations, destDirUri, fileName)
                            }.getOrElse {
                                publishedRemoved = false
                                failure.addSuppressed(it)
                                null
                            }
                        } else {
                            null
                        }
                    failedPublished?.let { document ->
                        runCatching { operations.delete(document.uri) }
                            .onFailure {
                                publishedRemoved = false
                                failure.addSuppressed(it)
                            }
                    }
                    backup?.let { document ->
                        if (publishedRemoved) {
                            runCatching {
                                val restored = operations.rename(document.uri, fileName)
                                check(restored.name == fileName) { "Provider changed restored document name" }
                                backup = null
                            }.exceptionOrNull()?.let(failure::addSuppressed)
                        }
                    }
                }
            }
            throw failure
        } finally {
            withContext(NonCancellable) {
                // writeFile may have created the unique staged name before throwing.
                val leftoverStage =
                    staged ?: runCatching {
                        uniqueReplacementChild(operations, destDirUri, stagedName)
                    }.getOrNull()
                leftoverStage?.let { runCatching { operations.delete(it.uri) } }
                // Preserve recovery data after an unsuccessful replacement/rollback.
                if (committed) backup?.let { runCatching { operations.delete(it.uri) } }
            }
        }
    }

    /** Runs the production StorageAccess replacement path against a dedicated test tree. */
    internal suspend fun replaceExternalFileForTesting(
        sourceFile: File,
        destDirUri: Uri,
        fileName: String,
        operations: ExternalFileOperations = StorageExternalFileOperations,
    ): Long = replaceExternalFile(sourceFile, destDirUri, fileName, operations).bytes

    private suspend fun uniqueReplacementChild(
        operations: ExternalFileOperations,
        dirUri: Uri,
        name: String,
    ): StorageDocument? {
        SyncRelativePath.requireSafeName(name)
        val matches = operations.list(dirUri).filter { it.name == name }
        check(matches.size <= 1) { "Remote directory contains duplicate name: $name" }
        return matches.singleOrNull()
    }

    private suspend fun verifyExternalFile(
        operations: ExternalFileOperations,
        document: StorageDocument,
        expectedBytes: Long,
        expectedSha256: String,
    ) {
        // A zero length may mean unknown metadata in StorageAccess. The digest below still
        // detects truncation, so accepting it here does not weaken read-back verification.
        check(document.length <= 0 || document.length == expectedBytes) {
            "Document read-back length mismatch: ${document.name}"
        }
        val actual = operations.digest(document.uri)
        check(SyncFingerprint.hex(actual) == expectedSha256) {
            "Document read-back digest mismatch: ${document.name}"
        }
    }

    /**
     * Writes [destFile] from [copy] without ever exposing a partially written file: the content
     * first goes to a temporary file next to it and that file replaces [destFile] only when it is
     * complete. The original content is restored when the copy fails.
     *
     * @return the number of bytes written
     */
    internal fun writeFromStream(
        destFile: File,
        copy: (OutputStream) -> Unit,
    ): Long {
        val parent = destFile.parentFile ?: error("No parent for ${destFile.path}")
        check(!destFile.exists() || destFile.isFile) {
            "Destination is not a file: ${destFile.path}"
        }
        val operationId = UUID.randomUUID().toString()
        val incoming = File(parent, ".trime-new-$operationId.tmp")
        val backup = File(parent, ".trime-bak-$operationId.tmp")
        check(parent.mkdirs() || parent.isDirectory) { "Failed to create ${parent.path}" }
        var backedUp = false
        var committed = false
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
            committed = true

            if (backup.exists()) {
                backup.delete()
            }

            return expectedBytes
        } catch (e: Exception) {
            if (backedUp && backup.exists()) {
                runCatching {
                    if (destFile.exists()) {
                        check(destFile.delete()) { "Failed to remove incomplete ${destFile.path}" }
                    }
                    check(backup.renameTo(destFile)) { "Recovery retained at ${backup.path}" }
                }.exceptionOrNull()?.let(e::addSuppressed)
            }
            throw e
        } finally {
            incoming.delete()
            if (committed) backup.delete()
        }
    }

    /** Applies [transform] to every item with a bounded number of workers, keeping the input order. */
    internal suspend fun <T, R> mapParallel(
        items: List<T>,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        transform: suspend (T) -> R,
    ): List<R> {
        if (items.isEmpty()) return emptyList()
        val results = arrayOfNulls<Any?>(items.size)
        coroutineScope {
            val channel = Channel<Pair<Int, T>>(capacity = parallelism * 2)
            repeat(parallelism.coerceAtMost(items.size)) {
                launch(dispatcher) {
                    for ((index, item) in channel) {
                        results[index] = transform(item)
                    }
                }
            }
            items.forEachIndexed { index, item ->
                channel.send(index to item)
            }
            channel.close()
        }
        @Suppress("UNCHECKED_CAST")
        return results.map { checkNotNull(it) as R }
    }

    /** Creates every local directory only once, even when the copies run in parallel. */
    internal class LocalDirectoryGate {
        private val createdDirs = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
        private val locks = ConcurrentHashMap<String, Any>()

        fun ensure(
            root: File,
            relativeDir: String,
        ) {
            if (relativeDir.isEmpty()) return
            val lock = locks[relativeDir] ?: locks.putIfAbsent(relativeDir, Any()) ?: locks[relativeDir]!!
            synchronized(lock) {
                if (relativeDir in createdDirs) return
                val dir = SyncRelativePath.resolveContained(root, relativeDir)
                check(dir.mkdirs() || dir.isDirectory) { "Failed to create directory $relativeDir" }
                createdDirs.add(relativeDir)
            }
        }
    }

    private fun listLocalFiles(root: File): List<File> {
        if (!root.exists()) return emptyList()
        return root
            .walkTopDown()
            .onEnter { SyncRelativePath.isDirectPath(root, it) }
            .filter { SyncRelativePath.isDirectPath(root, it) }
            .filter { it.isFile }
            .filter {
                val relative = it.relativeTo(root).path.replace('\\', '/')
                !shouldSkip(relative)
            }.toList()
    }

    // endregion

    // region Orphan cleanup

    /**
     * Deletes local files that are not part of the external [externalPaths] listing,
     * keeping [SyncPathPolicy.shouldPreserveLocal] paths and empty directories in check.
     */
    internal fun removeLocalOrphans(
        root: File,
        externalPaths: Set<String>,
        ownId: String? = null,
        syncDir: String = SyncPathPolicy.DEFAULT_SYNC_DIR,
        knownEntries: Map<String, SyncEntry> = emptyMap(),
    ): CopyResult {
        if (!root.exists()) return CopyResult()
        var deleted = 0
        var failed = 0
        val emptiedParents = mutableSetOf<File>()
        root
            .walkBottomUp()
            .onEnter {
                SyncRelativePath.isDirectPath(root, it) &&
                    !shouldSkip(it.relativeTo(root).path, isDirectory = true)
            }
            .filter { it != root }
            .filter { SyncRelativePath.isDirectPath(root, it) }
            .filter {
                val relative = it.relativeTo(root).path.replace('\\', '/')
                !shouldSkip(relative, it.isDirectory)
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

                    file.isFile && relative !in externalPaths && knownEntries[relative]?.let { entry ->
                        entry.size >= 0 && entry.lastModified > 0 &&
                            file.length() == entry.size && file.lastModified() == entry.lastModified &&
                            entry.localSha256 != null &&
                            runCatching { SyncFingerprint.of(file) }.getOrNull() == entry.localSha256
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
        return CopyResult(deleted = deleted, failed = failed)
    }

    // endregion

    // region Path rules

    /** Whether [relativePath] belongs to an internal folder that the sync must not touch. */
    internal fun shouldSkip(
        relativePath: String,
        isDirectory: Boolean = false,
        skipUserDb: Boolean = true,
    ): Boolean {
        val normalized = relativePath.trimStart('/').trim().removePrefix("./")
        if (normalized.isEmpty()) return false
        val segments = normalized.split('/')
        if (operationArtifact.matches(segments.last())) return true
        if (segments.any { it == SKIP_DIR }) return true
        if (!skipUserDb) return false
        val dirSegments = if (isDirectory) segments else segments.dropLast(1)
        return dirSegments.any { it.contains(SKIP_DIR_SUBSTRING) }
    }

    /** Whether [relativePath] should be visited; [skipPrefix] drops that path and its descendants. */
    internal fun shouldVisit(
        relativePath: String,
        skipPrefix: String? = null,
    ): Boolean {
        if (skipPrefix.isNullOrEmpty()) return true
        return relativePath != skipPrefix && !relativePath.startsWith("$skipPrefix/")
    }

    private fun shouldSync(
        entry: StorageWalkEntry,
        skipUserDb: Boolean,
        skipPrefix: String?,
    ): Boolean {
        SyncRelativePath.requireSafeName(entry.file.name)
        check(SyncRelativePath.normalize(entry.relativePath) == entry.relativePath) {
            "Unsafe external path: ${entry.relativePath}"
        }
        return !shouldSkip(entry.relativePath, entry.file.isDir, skipUserDb) &&
            shouldVisit(entry.relativePath, skipPrefix)
    }

    /**
     * Walks the external [treeUri] and lists every entry that should take part in the sync.
     *
     * Subtrees rejected by [shouldSync] are never queried, so excluded `build`/`*.userdb`
     * directories neither cost a provider query each nor fail the walk when inaccessible. This
     * queries the provider directly because StorageAccess 1.2.1 does not expose
     * [DocumentsContract.EXTRA_LOADING]. Treating a partial cursor as complete could otherwise
     * turn its missing rows into local orphan deletions.
     */
    private suspend fun listExternalEntries(
        treeUri: Uri,
        skipUserDb: Boolean,
        skipPrefix: String?,
    ): List<StorageWalkEntry> = listExternalEntries(
        appContext.contentResolver,
        treeUri,
        skipUserDb,
        skipPrefix,
    )

    /** Runs the production external-listing validation against an isolated test provider. */
    internal suspend fun listExternalEntriesForTesting(
        contentResolver: ContentResolver,
        treeUri: Uri,
        skipUserDb: Boolean = true,
        skipPrefix: String? = null,
    ): List<StorageWalkEntry> = listExternalEntries(contentResolver, treeUri, skipUserDb, skipPrefix)

    private fun listExternalEntries(
        contentResolver: ContentResolver,
        treeUri: Uri,
        skipUserDb: Boolean,
        skipPrefix: String?,
    ): List<StorageWalkEntry> {
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        require(rootId.isNotEmpty()) { "External tree has no root document ID" }
        val entries = mutableListOf<StorageWalkEntry>()
        val visitedDocumentIds = mutableSetOf(rootId)
        val visitedPaths = mutableSetOf<String>()
        val pendingDirectories = ArrayDeque<ExternalDirectory>()
        pendingDirectories.addLast(ExternalDirectory(rootId, "", 0))

        while (pendingDirectories.isNotEmpty()) {
            val directory = pendingDirectories.removeLast()
            val childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, directory.documentId)
            val cursor =
                contentResolver.query(childrenUri, EXTERNAL_DOCUMENT_PROJECTION, null, null, null)
                    ?: throw IOException("Document provider returned no listing for $childrenUri")
            cursor.use {
                if (it.extras.getBoolean(DocumentsContract.EXTRA_LOADING)) {
                    throw IOException("Document listing is still loading: $childrenUri")
                }
                it.extras.getString(DocumentsContract.EXTRA_ERROR)?.takeIf(String::isNotBlank)?.let { error ->
                    throw IOException("Document provider failed to list $childrenUri: $error")
                }
                val idColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedColumn = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                while (it.moveToNext()) {
                    val documentId = it.getString(idColumn)
                    require(!documentId.isNullOrEmpty()) { "External entry has no document ID" }
                    check(visitedDocumentIds.add(documentId)) {
                        "External tree contains repeated document ID: $documentId"
                    }
                    val name = it.getString(nameColumn)
                    SyncRelativePath.requireSafeName(name)
                    val mimeType = it.getString(mimeColumn)
                    require(!mimeType.isNullOrEmpty()) { "External entry has no MIME type: $documentId" }
                    val relativePath =
                        if (directory.relativePath.isEmpty()) name else "${directory.relativePath}/$name"
                    check(visitedPaths.add(relativePath)) {
                        "External tree contains duplicate path: $relativePath"
                    }
                    val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
                    val document =
                        StorageDocument(
                            uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId),
                            name = name,
                            isDir = isDirectory,
                            length = if (it.isNull(sizeColumn)) 0L else it.getLong(sizeColumn),
                            lastModified = if (it.isNull(modifiedColumn)) 0L else it.getLong(modifiedColumn),
                            mimeType = if (isDirectory) null else mimeType,
                        )
                    val entry = StorageWalkEntry(document, relativePath)
                    if (!shouldSync(entry, skipUserDb, skipPrefix)) continue
                    val depth = directory.depth + 1
                    check(depth <= MAX_EXTERNAL_DEPTH) {
                        "External tree exceeds maximum depth $MAX_EXTERNAL_DEPTH at $relativePath"
                    }
                    entries.add(entry)
                    if (isDirectory) {
                        // Query a directory at the limit too. It is complete only when it has no
                        // included children; silently pruning it would create false orphans.
                        pendingDirectories.addLast(ExternalDirectory(documentId, relativePath, depth))
                    }
                }
            }
        }
        return entries
    }

    // endregion

    // region Sync index and stats

    /**
     * File-backed index of the synced paths, keyed by the tree URI of the picked folder.
     *
     * Not thread-safe: neither [loadIndex] nor [saveIndex] may run concurrently.
     */
    private const val INDEX_FILE = "rime_sync_index.json"
    private const val MAX_EXTERNAL_DEPTH = 64

    private val EXTERNAL_DOCUMENT_PROJECTION =
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )

    private data class ExternalDirectory(
        val documentId: String,
        val relativePath: String,
        val depth: Int,
    )

    private val indexJson = Json { ignoreUnknownKeys = true }

    private val indexFile: File
        get() = File(appContext.filesDir, INDEX_FILE)

    private fun loadIndex(): SyncIndexData {
        val stored =
            indexFile
                .takeIf { it.exists() }
                ?.readText()
                ?.let { runCatching { indexJson.decodeFromString<SyncIndexData>(it) }.getOrNull() }
                ?: SyncIndexData()
        val currentTreeUri = treeUri()?.toString().orEmpty()
        if (stored.treeUri != currentTreeUri) {
            return SyncIndexData(treeUri = currentTreeUri)
        }
        return stored
    }

    /** Exposes deletion history to the dedicated SAF instrumentation probe. */
    internal fun indexSnapshotForTesting(): SyncIndexData = loadIndex()

    private fun saveIndex(data: SyncIndexData) {
        writeFromStream(indexFile) { output ->
            output.write(indexJson.encodeToString(data).toByteArray(Charsets.UTF_8))
        }
    }

    private fun withCurrentTree(entries: Map<String, SyncEntry>): SyncIndexData = SyncIndexData(
        treeUri = treeUri()?.toString().orEmpty(),
        entries = entries,
    )

    internal fun shouldCopy(
        relativePath: String,
        size: Long,
        lastModified: Long,
        index: SyncIndexData,
    ): Boolean {
        if (size < 0 || lastModified <= 0) return true
        val cached = index.entries[relativePath] ?: return true
        return cached.size != size || cached.lastModified != lastModified
    }

    private fun mergeIndexEntries(
        existing: Map<String, SyncEntry>,
        results: List<IndexedCopyResult>,
    ): Map<String, SyncEntry> {
        val merged = existing.toMutableMap()
        results.forEach { indexed ->
            indexed.indexEntry?.let { (path, entry) -> merged[path] = entry }
        }
        return merged
    }

    private fun mergeStats(results: List<CopyResult>): SyncStats = results.fold(SyncStats()) { acc, r ->
        acc.copy(
            copied = acc.copied + r.copied,
            skipped = acc.skipped + r.skipped,
            deleted = acc.deleted + r.deleted,
            failed = acc.failed + r.failed,
            bytesCopied = acc.bytesCopied + r.bytesCopied,
        )
    }

    private operator fun SyncStats.plus(other: CopyResult): SyncStats = copy(
        deleted = deleted + other.deleted,
        failed = failed + other.failed,
    )

    // endregion

    // region Copy results

    internal data class CopyResult(
        val copied: Int = 0,
        val skipped: Int = 0,
        val deleted: Int = 0,
        val failed: Int = 0,
        val bytesCopied: Long = 0,
    )

    private data class IndexedCopyResult(
        val result: CopyResult,
        val indexEntry: Pair<String, SyncEntry>?,
    )

    private data class CopiedContent(
        val bytes: Long,
        val sha256: String,
    )

    // endregion

    data class SyncStats(
        val copied: Int = 0,
        val skipped: Int = 0,
        val deleted: Int = 0,
        val failed: Int = 0,
        val bytesCopied: Long = 0,
    )
}
