/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.osfans.trime.ime.core.PrivacyModeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

object CollectionHelper : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default) {
    private lateinit var cltDb: Database
    private lateinit var cltDao: DatabaseDao

    private val mutex = Mutex()

    private var lastBean: DatabaseBean? = null

    fun init(context: Context) {
        cltDb =
            Room
                .databaseBuilder(context, Database::class.java, "collection.db")
                .addMigrations(Database.MIGRATION_3_4)
                .build()
        cltDao = cltDb.databaseDao()
    }

    suspend fun get(id: Int) = cltDao.get(id)

    suspend fun haveUnpinned() = cltDao.haveUnpinned()

    fun allBeans() = cltDao.allBeans()

    suspend fun pin(id: Int) = cltDao.updatePinned(id, true)

    suspend fun unpin(id: Int) = cltDao.updatePinned(id, false)

    suspend fun delete(id: Int) = cltDao.delete(id)

    suspend fun deleteAll(skipPinned: Boolean = true) {
        if (skipPinned) {
            cltDao.deleteAllUnpinned()
        } else {
            cltDao.deleteAll()
        }
    }

    suspend fun updateText(
        id: Int,
        text: String,
    ) {
        val permit = clipboardPersistenceGate.acquire(isSensitive = false)
            ?: throw PersistencePermitExpiredException()
        try {
            mutex.withLock {
                val existing = checkNotNull(cltDao.get(id)) { "Collection entry no longer exists" }
                val updated = existing.copy(text = text)
                cltDb.withTransaction {
                    clipboardPersistenceGate.guardTransaction(permit) {
                        check(cltDao.updateText(id, text) == 1) { "Collection entry no longer exists" }
                    }
                }
                PrivacyModeState.publishIfOrdinary(permit) {
                    if (lastBean?.id == id) lastBean = updated
                }
            }
        } finally {
            clipboardPersistenceGate.release(permit)
        }
    }

    fun addNewBean(text: String) {
        val permit = clipboardPersistenceGate.acquire(isSensitive = false) ?: return
        launch {
            try {
                mutex.withLock {
                    val bean = DatabaseBean(text = text)
                    if (bean.text.isNullOrBlank()) return@withLock
                    cltDao.find(text)?.let { existing ->
                        val refreshed =
                            cltDb.withTransaction {
                                clipboardPersistenceGate.guardTransaction(permit) {
                                    cltDao.updateTime(existing.id, bean.time)
                                    existing.copy(time = bean.time)
                                }
                            }
                        PrivacyModeState.publishIfOrdinary(permit) {
                            lastBean = refreshed
                        }
                        return@withLock
                    }
                    val insertedBean =
                        cltDb.withTransaction {
                            clipboardPersistenceGate.guardTransaction(permit) {
                                val rowId = cltDao.insert(bean)
                                val id = rowId.toInt()
                                cltDao.get(rowId) ?: bean.copy(id = id)
                            }
                        }
                    PrivacyModeState.publishIfOrdinary(permit) {
                        lastBean = insertedBean
                    }
                }
            } catch (_: PersistencePermitExpiredException) {
                // Room rolled the transaction back; never expose the rejected content.
            } catch (e: Exception) {
                Timber.w("Failed to update collection database: $e")
            }
        }.invokeOnCompletion {
            // Completion handlers also run when a coroutine is cancelled before its body starts.
            clipboardPersistenceGate.release(permit)
        }
    }
}
