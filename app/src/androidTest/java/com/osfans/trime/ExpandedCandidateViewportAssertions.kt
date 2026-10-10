/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime

import android.app.Instrumentation
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.osfans.trime.core.RimeApi
import com.osfans.trime.ime.candidates.CandidateIdentity
import com.osfans.trime.ime.candidates.CandidateViewHolder
import com.osfans.trime.ime.candidates.compact.CompactCandidateDelegate
import com.osfans.trime.ime.candidates.unrolled.UnrolledCandidateLayout
import com.osfans.trime.ime.candidates.unrolled.window.FlexboxUnrolledCandidateWindow
import com.osfans.trime.ime.core.InputView
import com.osfans.trime.ime.core.TrimeInputMethodService
import com.osfans.trime.ime.keyboard.KeyboardWindow
import com.osfans.trime.ime.window.BoardWindowManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.kodein.di.direct
import org.kodein.di.instance

/** Uses two newly constructed production windows in the real IME and verifies the editor commit. */
object ExpandedCandidateViewportAssertions {
    suspend fun verify(instrumentation: Instrumentation, input: InputView, service: TrimeInputMethodService, editor: EditText): Int {
        fun <T> main(block: () -> T): T {
            var outcome: Result<T>? = null
            instrumentation.runOnMainSync { outcome = runCatching(block) }
            return checkNotNull(outcome).getOrThrow()
        }

        suspend fun until(predicate: () -> Boolean) {
            withTimeout(30_000) { while (!main(predicate)) delay(40) }
        }

        suspend fun <T> native(block: suspend RimeApi.() -> T): T {
            var outcome: Result<T>? = null
            val job = main { service.postRimeJob { outcome = runCatching { block() } } }
            job.join()
            check(!job.isCancelled) { "Expanded candidate native job was cancelled" }
            return checkNotNull(outcome).getOrThrow()
        }

        fun event(view: View, down: Long, action: Int, x: Float, y: Float) {
            val motion = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0)
            try {
                check(view.dispatchTouchEvent(motion))
            } finally {
                motion.recycle()
            }
        }

        val manager = main { input.di.direct.instance<BoardWindowManager>() }
        val compact = main { input.di.direct.instance<CompactCandidateDelegate>() }
        val originalEditor = main { editor.text.toString() }
        val snapshot = main { compact.adapter.snapshot }
        check(snapshot.isValid && main { service.isCurrentEditor(snapshot.editorToken) })
        var checks = 0
        var active: FlexboxUnrolledCandidateWindow? = null

        fun list(): RecyclerView = checkNotNull(manager.view.findViewById<UnrolledCandidateLayout>(R.id.unrolled_candidate_view)).recyclerView
        fun first(window: FlexboxUnrolledCandidateWindow, recycler: RecyclerView): CandidateViewHolder? {
            val position = window.layoutManager.findFirstVisibleItemPosition()
            return recycler.findViewHolderForAdapterPosition(position) as? CandidateViewHolder
        }

        try {
            val initial = main {
                FlexboxUnrolledCandidateWindow(input.di).also {
                    active = it
                    manager.attachWindow(it)
                }
            }
            until {
                manager.isAttached(initial) && initial.adapter.snapshot == snapshot &&
                    list().isShown && list().isLaidOut && !list().isLayoutRequested && first(initial, list())?.snapshot == snapshot
            }
            val initialList = main { list() }
            val initialIndex = main { checkNotNull(first(initial, initialList)).idx }
            main {
                val down = SystemClock.uptimeMillis()
                val x = initialList.width / 2f
                event(initialList, down, MotionEvent.ACTION_DOWN, x, initialList.height * 0.85f)
                for (step in 1..6) event(initialList, down, MotionEvent.ACTION_MOVE, x, initialList.height * (0.85f - step * 0.1f))
                event(initialList, down, MotionEvent.ACTION_UP, x, initialList.height * 0.25f)
                initialList.stopScroll()
            }
            until {
                initialList.scrollState == RecyclerView.SCROLL_STATE_IDLE && !initialList.isLayoutRequested &&
                    compact.adapter.snapshot == snapshot && initial.adapter.snapshot == snapshot &&
                    first(initial, initialList)?.let { it.snapshot == snapshot && it.idx > initialIndex } == true
            }
            val selected = main {
                check(initialList.scrollState == RecyclerView.SCROLL_STATE_IDLE && !initialList.isLayoutRequested)
                check(compact.adapter.snapshot == snapshot && initial.adapter.snapshot == snapshot)
                val holder = checkNotNull(first(initial, initialList))
                check(holder.snapshot == snapshot && holder.idx > initialIndex)
                val identity = CandidateIdentity(holder.snapshot, holder.idx)
                check(editor.text.toString() == originalEditor) { "Expanded scrolling committed editor text" }
                // Read the row immediately before onDetached captures it, without an intervening frame.
                manager.attachWindow(KeyboardWindow)
                check(!manager.isAttached(initial)) { "Collapsed expanded window remains the current window" }
                val restored = compact.expandedViewportAnchor.restore(snapshot, initial.adapter.offset)
                check(restored == identity.index) {
                    "Collapsed anchor differs: expected=${identity.index}, actual=$restored, offset=${initial.adapter.offset}, snapshot=$snapshot"
                }
                identity
            }
            checks++
            // The normal 100ms exit Slide temporarily reparents the removed root into an overlay.
            until { !manager.view.isLayoutRequested && initialList.parent?.parent == null }
            main {
                check(!manager.isAttached(initial)) { "Destroyed expanded window became current again" }
                val restored = compact.expandedViewportAnchor.restore(snapshot, initial.adapter.offset)
                check(restored == selected.index) {
                    "Destroyed anchor differs: expected=${selected.index}, actual=$restored, offset=${initial.adapter.offset}, snapshot=$snapshot"
                }
            }
            checks++

            val reopened = main {
                FlexboxUnrolledCandidateWindow(input.di).also {
                    active = it
                    manager.attachWindow(it)
                }
            }
            check(reopened !== initial && reopened.adapter !== initial.adapter && reopened.layoutManager !== initial.layoutManager)
            until {
                manager.isAttached(reopened) && reopened.adapter.snapshot == snapshot &&
                    list().isShown && !list().isLayoutRequested && first(reopened, list())?.idx == selected.index
            }
            val reopenedList = main { list() }
            check(reopenedList !== initialList)
            checks++
            repeat(2) {
                main { reopenedList.requestLayout() }
                until { !reopenedList.isLayoutRequested && first(reopened, reopenedList)?.idx == selected.index }
                check(main { editor.text.toString() == originalEditor }) { "Reopening or relayout committed editor text" }
                checks++
            }
            val expected = native { getCandidates(selected.index, 1, snapshot).single().text }
            val expectedEditor = main {
                val value = editor.text
                val start = BaseInputConnection.getComposingSpanStart(value)
                val end = BaseInputConnection.getComposingSpanEnd(value)
                if (start >= 0 && end >= start) {
                    value.toString().substring(0, start) + expected + value.toString().substring(end)
                } else {
                    check(editor.selectionStart == editor.selectionEnd && editor.selectionStart >= 0)
                    val caret = editor.selectionStart
                    value.toString().substring(0, caret) + expected + value.toString().substring(caret)
                }
            }
            main {
                val holder = checkNotNull(first(reopened, reopenedList))
                check(CandidateIdentity(holder.snapshot, holder.idx) == selected && holder.text == expected)
                val child = holder.itemView
                val left = maxOf(0, child.left)
                val right = minOf(reopenedList.width, child.right)
                val top = maxOf(0, child.top)
                val bottom = minOf(reopenedList.height, child.bottom)
                check(left < right && top < bottom)
                val down = SystemClock.uptimeMillis()
                event(reopenedList, down, MotionEvent.ACTION_DOWN, (left + right) / 2f, (top + bottom) / 2f)
                event(reopenedList, down, MotionEvent.ACTION_UP, (left + right) / 2f, (top + bottom) / 2f)
            }
            until { editor.text.toString() == expectedEditor }
            native { check(getRawInput().isEmpty()) }
            instrumentation.waitForIdleSync()
            check(main { editor.text.toString() == expectedEditor }) { "Restored candidate committed more than once or selected another entry" }
            checks++
        } finally {
            main { if (active?.let(manager::isAttached) == true) manager.attachWindow(KeyboardWindow) }
        }
        return checks
    }
}
