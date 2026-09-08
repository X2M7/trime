// SPDX-License-Identifier: GPL-3.0-or-later
package com.osfans.trime

import android.app.Activity
import android.app.Instrumentation
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsProvider
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.osfans.trime.core.RimeMaintenanceMutex
import com.osfans.trime.data.base.DataManager
import com.osfans.trime.data.prefs.AppPrefs
import com.osfans.trime.data.sync.AtomicLocalFileCopy
import com.osfans.trime.data.sync.AtomicSafFileCopy
import com.osfans.trime.data.sync.RimeDataSync
import com.osfans.trime.data.sync.SafPathCache
import com.osfans.trime.data.sync.SafTreeListing
import com.osfans.trime.data.sync.SafTreeWalker
import com.osfans.trime.data.sync.SyncIndex
import com.osfans.trime.provider.RimeDataProvider
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import kotlin.concurrent.thread

/** Uses Android cursors, pipes and DocumentsContract, with only UUID-scoped fixtures. */
object SafCompatibilityProbe {
    fun run(instrumentation: Instrumentation, externalTree: String? = null, revoke: Boolean = false) {
        val result = Bundle()
        var passed = false
        try {
            check(Build.HARDWARE in setOf("ranchu", "goldfish")) { "Emulator only" }
            var checks = if (Build.VERSION.SDK_INT >= 29) {
                verify(instrumentation)
            } else {
                check(externalTree != null) { "API 21-28 requires a real dedicated SAF tree; virtual resolver fixtures need API 29" }
                0
            }
            check(!revoke || externalTree != null) { "Revocation requires the dedicated tree URI" }
            externalTree?.let {
                verifyExternalTree(instrumentation, it, revoke)
                checks++
            }
            val externalResult = when {
                revoke -> "; system provider permission revoked and access denied"
                externalTree != null -> "; system provider persisted grant and round trip"
                else -> "; isolated provider fixtures only"
            }
            result.putString("stream", "PASS: $checks SAF checks$externalResult\n")
            result.putInt("checks", checks)
            passed = true
        } catch (failure: Throwable) {
            result.putString("stream", "FAIL: ${failure.stackTraceToString()}\n")
        }
        result.putBoolean("passed", passed)
        instrumentation.finish(if (passed) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
    }

    @RequiresApi(29)
    private object ResolverWrapper {
        fun wrap(provider: DocumentsProvider): ContentResolver = ContentResolver.wrap(provider)
    }

    private fun verifyExternalTree(instrumentation: Instrumentation, value: String, revoke: Boolean) {
        val tree = value.toUri()
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        check(tree.authority == "com.android.externalstorage.documents" && rootId.substringAfterLast('/').startsWith("trime-saf-")) {
            "System provider test requires a dedicated trime-saf-UUID directory"
        }
        val cr = instrumentation.targetContext.contentResolver
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        cr.takePersistableUriPermission(tree, flags)
        check(cr.persistedUriPermissions.any { it.uri == tree && it.isReadPermission && it.isWritePermission })
        val root = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
        if (revoke) {
            cr.releasePersistableUriPermission(tree, flags)
            check(cr.persistedUriPermissions.none { it.uri == tree })
            val failure = runCatching { cr.query(root, arrayOf(Document.COLUMN_DOCUMENT_ID), null, null, null)?.close() }.exceptionOrNull()
            check(failure is SecurityException) { "Access still allowed; stop the picker/app to release transient grants before this test" }
            return
        }
        val directory = checkNotNull(DocumentsContract.createDocument(cr, root, Document.MIME_TYPE_DIR, "probe-${UUID.randomUUID()}"))
        val directoryId = DocumentsContract.getDocumentId(directory)
        val source = File.createTempFile("saf-source", ".yaml", instrumentation.targetContext.cacheDir)
        try {
            val cache = SafPathCache(SafTreeListing(emptyList(), mapOf("" to directoryId)), directoryId)
            for (payload in listOf("initial data", "replacement\n".repeat(1024))) {
                source.writeText(payload)
                AtomicSafFileCopy.copyFromFile(cr, tree, cache, source, "config.yaml", "", "config.yaml")
                val entry = SafTreeWalker.listFiles(cr, tree, directoryId).single()
                val uri = DocumentsContract.buildDocumentUriUsingTree(tree, entry.documentId)
                check(cr.openInputStream(uri)?.bufferedReader()?.use { it.readText() } == payload)
            }
        } finally {
            source.delete()
            check(DocumentsContract.deleteDocument(cr, directory))
        }
        instrumentation.sendStatus(0, Bundle().apply { putString("stream", "PASS: system provider persisted grant, create, replace, read-back and cleanup\n") })
        verifyImportHistory(instrumentation, tree, rootId)
    }

    private fun verifyImportHistory(instrumentation: Instrumentation, tree: android.net.Uri, rootId: String) = runBlocking {
        RimeMaintenanceMutex.withLock {
            val context = instrumentation.targetContext
            check(RimeDataSync.usesExternalSync(context) && RimeDataSync.treeUri() == tree)
            val cr = context.contentResolver
            val id = "__saf_history_${UUID.randomUUID()}"
            val names = listOf("$id-edited.yaml", "$id-unchanged.yaml")
            val binaryNames = listOf("$id-existing.userdb", "$id-fresh.userdb")
            val portableName = "$id.userdb.txt"
            val migration = AppPrefs.defaultInstance().profile.userDbMigrated
            val previousMigration = migration.getValue()
            val untracked = File(DataManager.userDataDir, "$id-untracked.yaml")
            val source = File.createTempFile("saf-history", ".yaml", context.cacheDir)
            val cache = SafPathCache(SafTreeListing(emptyList(), mapOf("" to rootId)), rootId)
            try {
                untracked.writeText("personal")
                source.writeText("old")
                migration.setValue(false)
                val personalDb = File(DataManager.userDataDir, binaryNames.first())
                check(personalDb.mkdir())
                File(personalDb, "LOG").writeText("personal-learning")
                binaryNames.forEach { name -> AtomicSafFileCopy.copyFromFile(cr, tree, cache, source, "$name/LOG", name, "LOG") }
                AtomicSafFileCopy.copyFromFile(cr, tree, cache, source, portableName, "", portableName)
                names.forEach { name -> AtomicSafFileCopy.copyFromFile(cr, tree, cache, source, name, "", name) }
                RimeDataSync.importToLocal(context, showProgress = false).getOrThrow()
                check(untracked.readText() == "personal")
                check(File(personalDb, "LOG").readText() == "personal-learning")
                check(!File(DataManager.userDataDir, binaryNames.last()).exists())
                check(File(DataManager.userDataDir, portableName).readText() == "old")
                val history = SyncIndex.load().entries
                check(names.all { history[it]?.localSha256?.length == 64 })
                val edited = File(DataManager.userDataDir, names.first())
                val timestamp = edited.lastModified()
                edited.writeText("new")
                check(edited.setLastModified(timestamp))
                SafTreeWalker.listFiles(cr, tree, rootId).filter { it.relativePath in names }.forEach { entry ->
                    check(DocumentsContract.deleteDocument(cr, DocumentsContract.buildDocumentUriUsingTree(tree, entry.documentId)))
                }
                RimeDataSync.importToLocal(context, showProgress = false).getOrThrow()
                check(edited.readText() == "new")
                check(!File(DataManager.userDataDir, names.last()).exists())
                check(untracked.readText() == "personal")
                check(names.none { it in SyncIndex.load().entries })
                val blockedName = "$id-blocked.yaml"
                val blocked = File(DataManager.userDataDir, blockedName)
                try {
                    AtomicSafFileCopy.copyFromFile(cr, tree, cache, source, blockedName, "", blockedName)
                    check(blocked.mkdir())
                    check(RimeDataSync.importToLocal(context, showProgress = false).isFailure)
                    check(RimeDataSync.importThemeToLocal(context, blockedName.removeSuffix(".yaml")).isFailure)
                    check(blocked.isDirectory && untracked.readText() == "personal")
                    check(blockedName !in SyncIndex.load().entries)
                } finally {
                    SafTreeWalker.listFiles(cr, tree, rootId).filter { it.relativePath == blockedName }.forEach { entry ->
                        check(DocumentsContract.deleteDocument(cr, DocumentsContract.buildDocumentUriUsingTree(tree, entry.documentId)))
                    }
                    check(!blocked.exists() || blocked.delete())
                }
            } finally {
                migration.setValue(previousMigration)
                (binaryNames + portableName).forEach { name ->
                    SafTreeWalker.findFileEntry(cr, tree, rootId, name)?.let { entry ->
                        check(DocumentsContract.deleteDocument(cr, DocumentsContract.buildDocumentUriUsingTree(tree, entry.documentId)))
                    }
                    val local = File(DataManager.userDataDir, name)
                    check(!local.exists() || local.deleteRecursively())
                }
                SafTreeWalker.listFiles(cr, tree, rootId).filter { it.relativePath in names }.forEach { entry ->
                    check(DocumentsContract.deleteDocument(cr, DocumentsContract.buildDocumentUriUsingTree(tree, entry.documentId)))
                }
                names.forEach { File(DataManager.userDataDir, it).delete() }
                untracked.delete()
                source.delete()
            }
            instrumentation.sendStatus(
                0,
                Bundle().apply {
                    putString("stream", "PASS: real SAF import preserves untracked files and same-metadata local edits; only hash-verified deletions propagate\n")
                },
            )
        }
    }

    @RequiresApi(29)
    private fun verify(instrumentation: Instrumentation): Int {
        val context = instrumentation.targetContext
        val sandbox = File(context.cacheDir, "saf-probe-${UUID.randomUUID()}").apply { mkdirs() }
        val authority = "${context.packageName}.saf.test"
        val tree = DocumentsContract.buildTreeDocumentUri(authority, "root")
        val info = ProviderInfo().apply {
            this.authority = authority
            exported = true
            grantUriPermissions = true
            readPermission = "android.permission.MANAGE_DOCUMENTS"
            writePermission = readPermission
        }
        var checks = 0
        fun test(name: String, block: (FixtureProvider, ContentResolver, File) -> Unit) {
            val directory = File(sandbox, "case-${checks + 1}").apply { mkdir() }
            val provider = FixtureProvider(directory)
            provider.attachInfo(context, info)
            val resolver = ResolverWrapper.wrap(provider)
            val source = File(sandbox, "source").apply { writeText("new dictionary\n".repeat(1024)) }
            block(provider, resolver, source)
            checks++
            instrumentation.sendStatus(0, Bundle().apply { putString("stream", "PASS $checks: $name\n") })
        }
        fun copy(cr: ContentResolver, source: File, path: String = "sample.yaml") {
            val cache = SafPathCache(SafTreeListing(emptyList(), mapOf("" to "root")), "root")
            AtomicSafFileCopy.copyFromFile(cr, tree, cache, source, path, path.substringBeforeLast('/', ""), path.substringAfterLast('/'))
        }
        fun expectFailure(block: () -> Unit) {
            check(runCatching(block).isFailure) { "Failure was not reported" }
        }
        try {
            test("new file, nested Unicode/spaces and changed rename ID") { p, cr, source ->
                val name = "\u8bcd\u5178 folder/sample.yaml"
                copy(cr, source, name)
                val listing = SafTreeWalker.listTree(cr, tree, "root")
                check(listing.files.single().relativePath == name)
                check(p.file("sample.yaml").readBytes().contentEquals(source.readBytes()))
            }
            test("replace retains unrelated .bak file") { p, cr, source ->
                p.seed("sample.yaml", "old")
                p.seed("sample.yaml.bak", "personal backup")
                copy(cr, source)
                check(p.file("sample.yaml").readBytes().contentEquals(source.readBytes()))
                check(p.file("sample.yaml.bak").readText() == "personal backup")
                check(p.names().none { it.startsWith(".trime-") })
            }
            test("unknown metadata and nonseekable reliable pipe") { p, cr, source ->
                p.pipeReads = true
                copy(cr, source)
                val entry = SafTreeWalker.listFiles(cr, tree, "root").single()
                check(entry.size == -1L && entry.lastModified == 0L)
                val uri = DocumentsContract.buildDocumentUriUsingTree(tree, entry.documentId)
                val local = File(sandbox, "round-trip")
                val descriptor = checkNotNull(cr.openFileDescriptor(uri, "r"))
                ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
                    check(descriptor.canDetectErrors())
                    AtomicLocalFileCopy.writeFromStream(local) { output ->
                        input.copyTo(output)
                        descriptor.checkError()
                    }
                }
                check(local.readBytes().contentEquals(source.readBytes()))
            }
            test("write failure cleans temporary document") { p, cr, source ->
                p.seed("sample.yaml", "old")
                p.failWrite = true
                expectFailure { copy(cr, source) }
                check(p.file("sample.yaml").readText() == "old")
                check(p.names() == listOf("sample.yaml"))
            }
            test("reliable pipe error preserves original") { p, cr, source ->
                p.seed("sample.yaml", "old")
                p.pipeReads = true
                p.failPipeRead = true
                expectFailure { copy(cr, source) }
                check(p.file("sample.yaml").readText() == "old")
                check(p.names() == listOf("sample.yaml"))
            }
            test("read-back detects truncated provider output") { p, cr, source ->
                p.seed("sample.yaml", "old")
                p.corruptRead = true
                expectFailure { copy(cr, source) }
                check(p.file("sample.yaml").readText() == "old")
                check(p.names() == listOf("sample.yaml"))
            }
            test("publish failure restores backup with changed ID") { p, cr, source ->
                p.seed("sample.yaml", "old")
                p.failPublish = true
                expectFailure { copy(cr, source) }
                check(p.file("sample.yaml").readText() == "old")
                check(p.names() == listOf("sample.yaml"))
            }
            test("rollback failure retains recovery backup") { p, cr, source ->
                p.seed("sample.yaml", "old")
                p.failPublish = true
                p.failRestore = true
                expectFailure { copy(cr, source) }
                check(p.names().single().endsWith(".bak"))
                check(p.file(p.names().single()).readText() == "old")
            }
            test("provider without rename supports new files") { p, cr, source ->
                p.canRename = false
                copy(cr, source)
                check(p.file("sample.yaml").readBytes().contentEquals(source.readBytes()))
                check(p.names() == listOf("sample.yaml"))
            }
            test("provider without rename retains existing files") { p, cr, source ->
                p.canRename = false
                p.seed("sample.yaml", "old")
                expectFailure { copy(cr, source) }
                check(p.file("sample.yaml").readText() == "old")
                check(p.names() == listOf("sample.yaml"))
            }
            test("loading listing fails closed") { p, cr, source ->
                p.loading = true
                expectFailure { SafTreeWalker.listTree(cr, tree, "root") }
                expectFailure { copy(cr, source) }
                check(p.names().isEmpty())
            }
            test("null listing fails closed") { p, cr, source ->
                p.nullListing = true
                expectFailure { SafTreeWalker.listTree(cr, tree, "root") }
                expectFailure { copy(cr, source) }
                check(p.names().isEmpty())
            }
            test("unsafe provider display name rejected") { p, cr, _ ->
                p.seed("../escape", "old")
                expectFailure { SafTreeWalker.listTree(cr, tree, "root") }
            }
            test("duplicate display names rejected") { p, cr, _ ->
                p.seed("same", "a")
                p.seed("same", "b")
                expectFailure { SafTreeWalker.listTree(cr, tree, "root") }
                expectFailure { SafTreeWalker.findFileEntry(cr, tree, "root", "same") }
            }
            test("revoked permission does not alter destination") { p, cr, source ->
                p.seed("sample.yaml", "old")
                p.denied = true
                expectFailure { copy(cr, source) }
                check(p.file("sample.yaml").readText() == "old")
            }
            test("duplicate directory names rejected before index overwrite") { p, cr, _ ->
                repeat(2) { p.createDocument("root", Document.MIME_TYPE_DIR, "same") }
                expectFailure { SafTreeWalker.listTree(cr, tree, "root") }
            }
            val own = RimeDataProvider().apply { attachInfo(context, info) }
            val rootId = checkNotNull(context.getExternalFilesDir(null)).name
            check(!own.isChildDocument(rootId, "$rootId-sibling"))
            expectFailure { own.queryDocument("../escape", null) }
            expectFailure { own.createDocument(rootId, "text/plain", "../escape") }
            own.queryDocument(rootId, arrayOf(Document.COLUMN_FLAGS)).use {
                check(it.moveToFirst())
                check(it.getInt(0) and (Document.FLAG_SUPPORTS_DELETE or Document.FLAG_SUPPORTS_RENAME or Document.FLAG_SUPPORTS_MOVE) == 0)
            }
            checks += 4
            val ownTree = DocumentsContract.buildTreeDocumentUri(authority, rootId)
            val ownResolver = ResolverWrapper.wrap(own)
            val probeId = own.createDocument(rootId, Document.MIME_TYPE_DIR, ".saf-probe-${UUID.randomUUID()}")
            try {
                val cache = SafPathCache(SafTreeListing(emptyList(), mapOf("" to probeId)), probeId)
                val source = File(sandbox, "own-source")
                for (payload in listOf("initial", "updated")) {
                    source.writeText(payload)
                    AtomicSafFileCopy.copyFromFile(ownResolver, ownTree, cache, source, "config.yaml", "", "config.yaml")
                    val entry = SafTreeWalker.listFiles(ownResolver, ownTree, probeId).single()
                    val uri = DocumentsContract.buildDocumentUriUsingTree(ownTree, entry.documentId)
                    check(ownResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } == payload)
                }
                checks++
                instrumentation.sendStatus(0, Bundle().apply { putString("stream", "PASS: Trime provider create, replace and read-back\n") })
            } finally {
                own.deleteDocument(probeId)
            }
            return checks
        } finally {
            sandbox.deleteRecursively()
        }
    }

    private class FixtureProvider(private val directory: File) : DocumentsProvider() {
        private data class Node(val id: String, val parent: String, val name: String, val file: File, val directory: Boolean = false)
        private val nodes = linkedMapOf("root" to Node("root", "", "root", directory, true))
        var canRename = true
        var pipeReads = false
        var failPipeRead = false
        var failWrite = false
        var corruptRead = false
        var failPublish = false
        var failRestore = false
        var loading = false
        var nullListing = false
        var denied = false
        fun names() = nodes.values.filter { !it.directory }.map { it.name }
        fun file(name: String) = nodes.values.first { it.name == name }.file
        fun seed(name: String, value: String) {
            createDocument("root", "text/plain", name).let { nodes.getValue(it).file.writeText(value) }
        }
        override fun onCreate() = true
        override fun queryRoots(projection: Array<String>?) = MatrixCursor(projection ?: emptyArray())
        override fun isChildDocument(parentDocumentId: String, documentId: String) = nodes.containsKey(documentId)
        private fun cursor(projection: Array<out String>?, rows: List<Node>): Cursor {
            if (denied) throw SecurityException("Fixture permission revoked")
            val columns = projection ?: arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME)
            return MatrixCursor(columns).apply {
                extras = Bundle().apply { putBoolean(DocumentsContract.EXTRA_LOADING, loading) }
                rows.forEach { node ->
                    addRow(
                        columns.map { column ->
                            when (column) {
                                Document.COLUMN_DOCUMENT_ID -> node.id
                                Document.COLUMN_DISPLAY_NAME -> node.name
                                Document.COLUMN_MIME_TYPE -> if (node.directory) Document.MIME_TYPE_DIR else "text/plain"
                                Document.COLUMN_SIZE -> if (pipeReads) null else node.file.length()
                                Document.COLUMN_LAST_MODIFIED -> if (pipeReads) null else node.file.lastModified()
                                Document.COLUMN_FLAGS ->
                                    Document.FLAG_SUPPORTS_WRITE or Document.FLAG_SUPPORTS_DELETE or
                                        Document.FLAG_DIR_SUPPORTS_CREATE or (if (canRename) Document.FLAG_SUPPORTS_RENAME else 0)
                                else -> null
                            }
                        }.toTypedArray<Any?>(),
                    )
                }
            }
        }
        override fun queryDocument(documentId: String, projection: Array<out String>?) = cursor(projection, listOf(nodes.getValue(documentId)))
        override fun queryChildDocuments(parentDocumentId: String, projection: Array<String>?, sortOrder: String?) = if (nullListing) null else cursor(projection, nodes.values.filter { it.parent == parentDocumentId })
        override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
            val id = UUID.randomUUID().toString()
            val dir = mimeType == Document.MIME_TYPE_DIR
            val file = File(directory, id)
            if (dir) file.mkdir() else file.createNewFile()
            nodes[id] = Node(id, parentDocumentId, displayName, file, dir)
            return id
        }
        override fun renameDocument(documentId: String, displayName: String): String {
            val node = nodes.getValue(documentId)
            if (!canRename || (
                    displayName == "sample.yaml" &&
                        ((failPublish && node.name.endsWith(".tmp")) || (failRestore && node.name.endsWith(".bak")))
                    )
            ) {
                throw FileNotFoundException("Injected rename failure")
            }
            val id = UUID.randomUUID().toString()
            nodes.remove(documentId)
            nodes[id] = node.copy(id = id, name = displayName)
            return id
        }
        override fun deleteDocument(documentId: String) {
            nodes.remove(documentId)?.file?.delete()
        }
        override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
            val node = nodes.getValue(documentId)
            if (mode.contains('w') && failWrite) throw FileNotFoundException("Injected write failure")
            if (mode == "r" && corruptRead && node.name.endsWith(".tmp")) node.file.writeText("truncated")
            if (mode == "r" && pipeReads) {
                val pipe = ParcelFileDescriptor.createReliablePipe()
                thread(name = "saf-fixture-pipe") {
                    ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output ->
                        node.file.inputStream().use { it.copyTo(output) }
                        if (failPipeRead) pipe[1].closeWithError("Injected provider read failure")
                    }
                }
                return pipe[0]
            }
            return ParcelFileDescriptor.open(node.file, ParcelFileDescriptor.parseMode(mode))
        }
    }
}
