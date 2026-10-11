/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ime.candidates.unrolled.window

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.view.View
import androidx.core.view.isNotEmpty
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import com.osfans.trime.daemon.RimeSession
import com.osfans.trime.data.theme.Theme
import com.osfans.trime.data.theme.ThemeScope
import com.osfans.trime.ime.bar.InputBarDelegate
import com.osfans.trime.ime.bar.UnrollButtonStateMachine
import com.osfans.trime.ime.broadcast.InputBroadcastReceiver
import com.osfans.trime.ime.candidates.CandidateViewHolder
import com.osfans.trime.ime.candidates.CandidateViewportState
import com.osfans.trime.ime.candidates.compact.CompactCandidateDelegate
import com.osfans.trime.ime.candidates.unrolled.CandidatesPagingSource
import com.osfans.trime.ime.candidates.unrolled.PagingCandidateViewAdapter
import com.osfans.trime.ime.candidates.unrolled.UnrolledCandidateLayout
import com.osfans.trime.ime.core.InputView
import com.osfans.trime.ime.core.TrimeInputMethodService
import com.osfans.trime.ime.keyboard.KeyboardWindow
import com.osfans.trime.ime.performance.InputLatencyLog
import com.osfans.trime.ime.window.BoardWindow
import com.osfans.trime.ime.window.BoardWindowManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance
import splitties.dimensions.dp
import kotlin.math.max

abstract class BaseUnrolledCandidateWindow(di: DI) :
    BoardWindow.NoBarBoardWindow(di),
    InputBroadcastReceiver {
    protected val service: TrimeInputMethodService by instance()
    protected val rime: RimeSession by instance()
    protected val scope: ThemeScope by instance()
    private val inputView: InputView by instance()
    private val bar: InputBarDelegate by instance()
    private val windowManager: BoardWindowManager by instance()
    private val compactCandidate: CompactCandidateDelegate by instance()

    protected val theme: Theme
        get() = scope.theme

    private lateinit var lifecycleCoroutineScope: LifecycleCoroutineScope
    private lateinit var candidateLayout: UnrolledCandidateLayout
    private val viewportState = CandidateViewportState()
    private var rememberedIndex: Int? = null
    private var rememberedPixelOffset = 0
    private var pendingScrollIndex: Int? = null
    private var pendingScrollPosition: Int? = null

    protected val separatorDrawable by lazy {
        ShapeDrawable(RectShape()).apply {
            val spacing = theme.style.candidateSpacing
            val intrinsicSize = max(spacing, context.dp(spacing)).toInt()
            intrinsicWidth = intrinsicSize
            intrinsicHeight = intrinsicSize
            paint.color = scope.colors.candidateSeparatorColor
        }
    }

    override fun refreshColors() {
        if (!::candidateLayout.isInitialized) return
        // the decorations share this drawable, so re-coloring its paint repaints the dividers
        separatorDrawable.paint.color = scope.colors.candidateSeparatorColor
        candidateLayout.refreshColors()
        // visible rows re-apply their colors on rebind
        adapter.notifyItemRangeChanged(0, adapter.itemCount)
    }

    abstract fun onCreateCandidateLayout(): UnrolledCandidateLayout

    final override fun onCreateView(): View {
        candidateLayout =
            onCreateCandidateLayout().apply {
                onCandidatesLaidOut = { restorePendingViewport() }
                onCandidatesDrawn = {
                    val snapshot = adapter.snapshot
                    val list = recyclerView
                    if (service.isCurrentEditor(snapshot.editorToken) &&
                        snapshot == rime.run { candidateSnapshotCached } && list.isNotEmpty()
                    ) {
                        var currentRows = true
                        for (index in 0 until list.childCount) {
                            val holder = list.getChildViewHolder(list.getChildAt(index)) as? CandidateViewHolder
                            if (holder?.snapshot != snapshot) {
                                currentRows = false
                                break
                            }
                        }
                        if (currentRows) InputLatencyLog.candidateDraw(snapshot)
                    }
                }
                recyclerView.apply {
                    // disable item cross-fade animation
                    itemAnimator = null
                }
            }
        adapter.addOnPagesUpdatedListener {
            val target = pendingScrollIndex ?: return@addOnPagesUpdatedListener
            val position = adapter.snapshot().items.indexOfFirst { it.index == target && it.snapshot == adapter.snapshot }
            if (position >= 0) {
                val list = candidateLayout.recyclerView
                if (pendingScrollPosition != position) {
                    pendingScrollPosition = position
                    list.scrollToPosition(position)
                }
                // Paging notifications can precede layout; the pixel adjustment needs a laid-out row.
                list.requestLayout()
            }
        }
        return candidateLayout
    }

    abstract val adapter: PagingCandidateViewAdapter
    abstract val layoutManager: RecyclerView.LayoutManager

    private fun restorePendingViewport() {
        val target = pendingScrollIndex ?: return
        val snapshot = adapter.snapshot
        if (!windowManager.isAttached(this) || !snapshot.isValid ||
            snapshot != compactCandidate.adapter.snapshot || !service.isCurrentEditor(snapshot.editorToken)
        ) {
            pendingScrollIndex = null
            pendingScrollPosition = null
            return
        }
        val position = pendingScrollPosition ?: return
        val list = candidateLayout.recyclerView
        if (list.isComputingLayout || list.isLayoutRequested || list.hasPendingAdapterUpdates()) {
            list.requestLayout()
            return
        }
        val holder = list.findViewHolderForAdapterPosition(position) as? CandidateViewHolder ?: return
        if (holder.snapshot != snapshot || holder.idx != target) return
        val currentOffset = layoutManager.getDecoratedTop(holder.itemView) - list.paddingTop
        // scrollBy may synchronously trigger paging callbacks. Consume the pending restore first.
        pendingScrollIndex = null
        pendingScrollPosition = null
        list.scrollBy(0, currentOffset - rememberedPixelOffset)
    }

    private var offsetJob: Job? = null

    private val candidatesPager by lazy {
        Pager(
            config = PagingConfig(
                pageSize = CandidatesPagingSource.PAGE_SIZE,
                initialLoadSize = CandidatesPagingSource.PAGE_SIZE,
                maxSize = CandidatesPagingSource.PAGE_SIZE * 3,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                CandidatesPagingSource(
                    rime,
                    total = adapter.total,
                    offset = adapter.offset,
                    snapshot = adapter.snapshot,
                    initialIndex = rememberedIndex ?: adapter.offset,
                )
            },
        )
    }

    private var candidatesSubmitJob: Job? = null

    override fun onAttached() {
        lifecycleCoroutineScope = candidateLayout.findViewTreeLifecycleOwner()!!.lifecycleScope
        bar.unrollButtonStateMachine.push(UnrollButtonStateMachine.TransitionEvent.UnrolledCandidatesAttached)
        var firstEmission = true
        offsetJob =
            lifecycleCoroutineScope.launch {
                compactCandidate.unrolledCandidateOffset.collect {
                    if (it.snapshot != compactCandidate.adapter.snapshot) return@collect
                    if (!it.snapshot.isValid || it.total == 0) {
                        windowManager.attachWindow(KeyboardWindow)
                    } else {
                        if (viewportState.update(it.snapshot, it.offset)) {
                            val anchor = compactCandidate.expandedViewportAnchor
                            val restoredIndex = anchor.restore(it.snapshot, it.offset)
                                ?.takeIf { index -> it.total < 0 || index < it.total }
                            rememberedIndex = restoredIndex ?: it.anchor.coerceAtLeast(it.offset)
                            rememberedPixelOffset = if (restoredIndex != null) anchor.restoreOffset(it.snapshot, it.offset) ?: 0 else 0
                            pendingScrollIndex = rememberedIndex
                            pendingScrollPosition = null
                            candidateLayout.resetPosition()
                            adapter.refreshWith(
                                offset = it.offset,
                                total = it.total,
                                highlightedIndex = it.highlighted,
                                snapshot = it.snapshot,
                            )
                        } else if (firstEmission) {
                            pendingScrollIndex = rememberedIndex
                            pendingScrollPosition = null
                        }
                    }
                    firstEmission = false
                }
            }
        candidatesSubmitJob =
            lifecycleCoroutineScope.launch {
                candidatesPager.flow.collectLatest {
                    adapter.submitData(it)
                }
            }
    }

    fun bindCandidateUiViewHolder(holder: CandidateViewHolder) {
        holder.itemView.run {
            setOnClickListener { _ ->
                val index = holder.idx
                val snapshot = holder.snapshot
                service.postRimeJob { selectCandidate(index, global = true, expectedSnapshot = snapshot) }
            }
            setOnLongClickListener { view ->
                inputView.showCandidateActionMenu(holder.idx, holder.text, view, global = true, snapshot = holder.snapshot)
                true
            }
        }
    }

    override fun onDetached() {
        val position = when (val manager = layoutManager) {
            is FlexboxLayoutManager -> manager.findFirstVisibleItemPosition()
            is LinearLayoutManager -> manager.findFirstVisibleItemPosition()
            else -> -1
        }
        val holder = candidateLayout.recyclerView.findViewHolderForAdapterPosition(position) as? CandidateViewHolder
        if (windowManager.isAttached(this) && pendingScrollIndex == null && holder?.snapshot == adapter.snapshot &&
            holder.snapshot == compactCandidate.adapter.snapshot && service.isCurrentEditor(holder.snapshot.editorToken)
        ) {
            val pixelOffset = layoutManager.getDecoratedTop(holder.itemView) - candidateLayout.recyclerView.paddingTop
            if (compactCandidate.expandedViewportAnchor.remember(holder.snapshot, adapter.offset, holder.idx, adapter.total, pixelOffset)) {
                rememberedIndex = holder.idx
                rememberedPixelOffset = pixelOffset
            }
        }
        pendingScrollIndex = null
        pendingScrollPosition = null
        bar.unrollButtonStateMachine.push(
            UnrollButtonStateMachine.TransitionEvent.UnrolledCandidatesDetached,
            UnrollButtonStateMachine.BooleanKey.UnrolledCandidatesEmpty to
                (compactCandidate.adapter.total == adapter.offset),
        )
        offsetJob?.cancel()
        candidatesSubmitJob?.cancel()
    }
}
