/*
 * SPDX-FileCopyrightText: 2015 - 2024 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ime.candidates.unrolled

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.osfans.trime.core.CandidateSnapshot
import com.osfans.trime.data.theme.ThemeScope
import com.osfans.trime.ime.candidates.CandidateIdentity
import com.osfans.trime.ime.candidates.CandidateItemUi
import com.osfans.trime.ime.candidates.CandidateRow
import com.osfans.trime.ime.candidates.CandidateViewHolder

open class PagingCandidateViewAdapter(
    val scope: ThemeScope,
) : PagingDataAdapter<CandidateRow, CandidateViewHolder>(diffCallback) {
    companion object {
        private val diffCallback =
            object : DiffUtil.ItemCallback<CandidateRow>() {
                override fun areItemsTheSame(
                    oldItem: CandidateRow,
                    newItem: CandidateRow,
                ): Boolean = oldItem.snapshot == newItem.snapshot && oldItem.index == newItem.index

                override fun areContentsTheSame(
                    oldItem: CandidateRow,
                    newItem: CandidateRow,
                ): Boolean = oldItem == newItem
            }
    }

    var offset: Int = 0
        private set

    var highlightedIndex: Int = -1
        private set

    var snapshot = CandidateSnapshot()
        private set

    var total: Int = -1
        private set

    fun refreshWith(offset: Int, total: Int, highlightedIndex: Int, snapshot: CandidateSnapshot) {
        this.offset = offset
        this.total = total
        this.highlightedIndex = highlightedIndex
        this.snapshot = snapshot
        refresh()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CandidateViewHolder = CandidateViewHolder(CandidateItemUi(parent.context, scope))

    override fun onBindViewHolder(
        holder: CandidateViewHolder,
        position: Int,
    ) {
        val row = getItem(position)
        if (row == null) {
            holder.ui.root.identity = null
            holder.idx = -1
            holder.snapshot = CandidateSnapshot.EMPTY
            return
        }
        val item = row.candidate
        val idx = row.index
        val highlighted = idx == highlightedIndex
        holder.ui.update(item, highlighted)
        holder.text = item.text
        holder.comment = item.comment
        holder.idx = idx
        holder.snapshot = row.snapshot
        holder.ui.root.identity = CandidateIdentity(row.snapshot, idx)
    }
}
