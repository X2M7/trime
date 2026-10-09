/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ime.candidates.unrolled

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.osfans.trime.core.CandidateSnapshot
import com.osfans.trime.core.StaleCandidateSnapshotException
import com.osfans.trime.daemon.RimeSession
import com.osfans.trime.ime.candidates.CandidateRow
import timber.log.Timber

class CandidatesPagingSource(
    val rime: RimeSession,
    val total: Int,
    val offset: Int,
    val snapshot: CandidateSnapshot,
) : PagingSource<Int, CandidateRow>() {
    companion object {
        const val PAGE_SIZE = 48
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CandidateRow> {
        // use candidate index for key, null means load from beginning (including offset)
        val startIndex = params.key ?: offset
        val pageSize = params.loadSize.coerceAtMost(PAGE_SIZE)
        Timber.d("getCandidates(offset=$startIndex, limit=$pageSize)")
        val candidates = try {
            rime.runOnReady { getCandidates(startIndex, pageSize, snapshot) }
        } catch (_: StaleCandidateSnapshotException) {
            return LoadResult.Invalid()
        }
        val prevKey = if (startIndex >= pageSize) startIndex - pageSize else null
        val nextKey = nextCandidatePage(startIndex, pageSize, candidates.size, total)
        return LoadResult.Page(
            candidates.mapIndexed { index, candidate -> CandidateRow(candidate, startIndex + index, snapshot) },
            prevKey,
            nextKey,
        )
    }

    // always reload from beginning
    override fun getRefreshKey(state: PagingState<Int, CandidateRow>) = null
}

internal fun nextCandidatePage(startIndex: Int, pageSize: Int, count: Int, total: Int): Int? = when {
    count == 0 -> null
    total >= 0 -> (startIndex + count).takeIf { it < total }
    count < pageSize -> null
    else -> startIndex + count
}
