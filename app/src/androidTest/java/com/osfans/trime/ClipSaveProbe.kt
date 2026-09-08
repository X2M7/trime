// SPDX-License-Identifier: GPL-3.0-or-later
package com.osfans.trime

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.EditText
import androidx.room.withTransaction
import com.osfans.trime.data.db.ClipboardHelper
import com.osfans.trime.data.db.CollectionHelper
import com.osfans.trime.data.db.Database
import com.osfans.trime.data.db.DatabaseBean
import com.osfans.trime.data.db.DatabaseDao
import com.osfans.trime.ui.main.ClipEditActivity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID

/** Real Room writes and dialog lifecycle; only newly inserted UUID-tagged rows are removed. */
internal object ClipSaveProbe {
    fun run(instrumentation: Instrumentation) {
        val result = Bundle()
        var passed = false
        var checks = 0
        fun <T> main(block: () -> T): T {
            var outcome: Result<T>? = null
            instrumentation.runOnMainSync { outcome = runCatching(block) }
            return checkNotNull(outcome).getOrThrow()
        }
        fun editor(type: String, id: Int) = instrumentation.startActivitySync(
            Intent(instrumentation.targetContext, ClipEditActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(ClipEditActivity.CLIP_TYPE, type)
                .putExtra(ClipEditActivity.BEAN_ID, id),
        ) as ClipEditActivity
        suspend fun eventually(block: suspend () -> Boolean) = withTimeout(15_000) {
            while (!block()) delay(50)
        }
        fun checkPassed(name: String) {
            checks++
            instrumentation.sendStatus(0, Bundle().apply { putString("stream", "PASS $checks: $name\n") })
        }
        try {
            check(Build.HARDWARE in setOf("ranchu", "goldfish")) { "Emulator only" }
            instrumentation.sendStatus(
                0,
                Bundle().apply {
                    putString("selected_input_method", Settings.Secure.getString(instrumentation.targetContext.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD))
                },
            )
            runBlocking {
                withTimeout(180_000) {
                    for ((type, helper, prefix) in listOf(
                        Triple(ClipEditActivity.FROM_CLIPBOARD, ClipboardHelper, "clb"),
                        Triple(ClipEditActivity.FROM_COLLECTION, CollectionHelper, "clt"),
                    )) {
                        val db = helper.javaClass.getDeclaredField("${prefix}Db").apply { isAccessible = true }.get(helper) as Database
                        val daoField = helper.javaClass.getDeclaredField("${prefix}Dao").apply { isAccessible = true }
                        val dao = daoField.get(helper) as DatabaseDao
                        val cached = helper.javaClass.getDeclaredField("lastBean").apply { isAccessible = true }
                        val previousCache = main { cached.get(helper) }
                        val owned = mutableListOf<Int>()
                        var active: ClipEditActivity? = null
                        suspend fun insert(text: String): DatabaseBean {
                            val row = dao.insert(DatabaseBean(text = text, pinned = true))
                            return checkNotNull(dao.get(row)).also { owned += it.id }
                        }
                        try {
                            for (background in listOf(false, true)) {
                                val token = UUID.randomUUID().toString()
                                val bean = insert("initial-$token")
                                val expected = "saved-$token-\u4f60\u597d-\uD842\uDFB7"
                                main { cached.set(helper, bean) }
                                val activity = editor(type, bean.id).also { active = it }
                                val field = main { activity.findViewById<EditText>(R.id.clip_edit_text) }
                                val ok = main { activity.findViewById<View>(R.id.clip_edit_ok) }
                                eventually { main { field.text.toString() == bean.text && ok.isEnabled } }
                                val entered = CompletableDeferred<Unit>()
                                val release = CompletableDeferred<Unit>()
                                val blocked = launch(Dispatchers.Default) {
                                    db.withTransaction {
                                        entered.complete(Unit)
                                        release.await()
                                    }
                                }
                                try {
                                    entered.await()
                                    main {
                                        field.setText(expected)
                                        ok.performClick()
                                        field.setText("duplicate-$token")
                                        ok.performClick()
                                    }
                                    delay(300)
                                    check(main { !activity.isFinishing }) { "Editor closed before its database write completed" }
                                    check(main { !ok.isEnabled }) { "Duplicate save was not disabled" }
                                    check(main { (cached.get(helper) as DatabaseBean).text == bean.text }) { "Uncommitted text reached the cache" }
                                    if (background) {
                                        main { activity.finish() }
                                        eventually { main { activity.isDestroyed } }
                                    }
                                } finally {
                                    withContext(NonCancellable) {
                                        release.complete(Unit)
                                        blocked.join()
                                        db.withTransaction { }
                                    }
                                }
                                eventually { dao.get(bean.id)?.text == expected }
                                eventually { main { (cached.get(helper) as DatabaseBean).text == expected } }
                                eventually { main { activity.isDestroyed } }
                                checkPassed("$type: ${if (background) "save survives dialog destruction" else "save awaits commit and ignores duplicate click"}")
                                active = null
                            }

                            val missing = insert("missing-${UUID.randomUUID()}")
                            main { cached.set(helper, missing) }
                            val failed = editor(type, missing.id).also { active = it }
                            val field = main { failed.findViewById<EditText>(R.id.clip_edit_text) }
                            val ok = main { failed.findViewById<View>(R.id.clip_edit_ok) }
                            eventually { main { field.text.toString() == missing.text && ok.isEnabled } }
                            dao.delete(missing.id)
                            main {
                                field.setText("unsaved edit")
                                ok.performClick()
                            }
                            eventually { main { ok.isEnabled && !failed.isFinishing } }
                            check(main { field.text.toString() == "unsaved edit" })
                            check(main { cached.get(helper) == missing }) { "Failed write changed the cache" }
                            check(dao.get(missing.id) == null)
                            main { failed.findViewById<View>(R.id.clip_edit_cancel).performClick() }
                            eventually { main { failed.isDestroyed } }
                            active = null
                            checkPassed("$type: removed-row failure retains editor text and cache")

                            val first = insert("first-${UUID.randomUUID()}")
                            val second = insert("second-${UUID.randomUUID()}")
                            val reading = CompletableDeferred<Unit>()
                            val releaseRead = CompletableDeferred<Unit>()
                            daoField.set(
                                helper,
                                object : DatabaseDao by dao {
                                    override suspend fun get(id: Int): DatabaseBean? {
                                        if (id == first.id) {
                                            reading.complete(Unit)
                                            releaseRead.await()
                                        }
                                        return dao.get(id)
                                    }
                                },
                            )
                            try {
                                val loading = editor(type, first.id).also { active = it }
                                reading.await()
                                check(main { !loading.findViewById<View>(R.id.clip_edit_ok).isEnabled })
                                check(main { !loading.findViewById<View>(R.id.clip_edit_text).isEnabled })
                                main {
                                    instrumentation.callActivityOnNewIntent(
                                        loading,
                                        Intent().putExtra(ClipEditActivity.CLIP_TYPE, type)
                                            .putExtra(ClipEditActivity.BEAN_ID, second.id),
                                    )
                                }
                                eventually { main { loading.findViewById<EditText>(R.id.clip_edit_text).text.toString() == second.text } }
                                releaseRead.complete(Unit)
                                delay(200)
                                check(main { loading.findViewById<EditText>(R.id.clip_edit_text).text.toString() == second.text })
                                main {
                                    loading.findViewById<EditText>(R.id.clip_edit_text).setText("cancelled edit")
                                    loading.findViewById<View>(R.id.clip_edit_cancel).performClick()
                                }
                                eventually { main { loading.isDestroyed } }
                                active = null
                                check(dao.get(first.id)?.text == first.text && dao.get(second.id)?.text == second.text)
                                checkPassed("$type: loading disables edits, new intent supersedes old read, cancel preserves both rows")
                            } finally {
                                releaseRead.complete(Unit)
                                daoField.set(helper, dao)
                            }
                        } finally {
                            withContext(NonCancellable) {
                                active?.let { activity ->
                                    main { if (!activity.isFinishing) activity.finish() }
                                    eventually { main { activity.isDestroyed } }
                                }
                                daoField.set(helper, dao)
                                owned.forEach { dao.delete(it) }
                                main { cached.set(helper, previousCache) }
                            }
                        }
                    }
                }
            }
            passed = true
            result.putString("stream", "PASS: $checks real clipboard/collection save and lifecycle checks; only owned rows removed\n")
        } catch (failure: Throwable) {
            result.putString("stream", "FAIL: ${failure.stackTraceToString()}\n")
        }
        result.putBoolean("passed", passed)
        result.putInt("checks", checks)
        instrumentation.finish(if (passed) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
    }
}
