/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.ime.candidates.unrolled

import androidx.paging.PagingSource
import com.osfans.trime.core.CandidateProto
import com.osfans.trime.core.CandidateSnapshot
import com.osfans.trime.core.RimeApi
import com.osfans.trime.core.StaleCandidateSnapshotException
import com.osfans.trime.daemon.RimeSession
import com.osfans.trime.ime.candidates.CandidateRow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.lang.reflect.Proxy

class CandidatesPagingSourceTest :
    StringSpec({
        "a known total keeps the final remaining candidate reachable" {
            nextCandidatePage(startIndex = 16, pageSize = 48, count = 48, total = 65) shouldBe 64
            nextCandidatePage(startIndex = 64, pageSize = 48, count = 1, total = 65) shouldBe null
        }

        "unknown totals stop on short or empty pages" {
            nextCandidatePage(16, 48, 48, -1) shouldBe 64
            nextCandidatePage(64, 48, 47, -1) shouldBe null
            nextCandidatePage(16, 48, 0, 200) shouldBe null
        }

        "a load carries its snapshot and caps the JNI batch" {
            val snapshot = CandidateSnapshot(7, 19, 23)
            var requestedStart = -1
            var requestedLimit = -1
            var requestedSnapshot: CandidateSnapshot? = null
            val api = candidateApi { start, limit, expected ->
                requestedStart = start
                requestedLimit = limit
                requestedSnapshot = expected
                arrayOf(CandidateProto("candidate", "", ""))
            }
            val source = CandidatesPagingSource(candidateSession(api), total = -1, offset = 16, snapshot = snapshot)
            val result = source.load(PagingSource.LoadParams.Refresh(null, 144, false)) as PagingSource.LoadResult.Page<Int, CandidateRow>
            requestedStart shouldBe 16
            requestedLimit shouldBe 48
            requestedSnapshot shouldBe snapshot
            result.data.single().index shouldBe 16
            result.data.single().snapshot shouldBe snapshot
        }

        "a stale engine snapshot invalidates the asynchronous page" {
            val api = candidateApi { _, _, _ -> throw StaleCandidateSnapshotException() }
            val source = CandidatesPagingSource(candidateSession(api), -1, 16, CandidateSnapshot(7, 19, 23))
            (source.load(PagingSource.LoadParams.Refresh(null, 48, false)) is PagingSource.LoadResult.Invalid<*, *>) shouldBe true
        }
    })

private fun candidateApi(load: (Int, Int, CandidateSnapshot?) -> Array<CandidateProto>): RimeApi = Proxy.newProxyInstance(RimeApi::class.java.classLoader, arrayOf(RimeApi::class.java)) { _, method, args ->
    check(method.name == "getCandidates") { "Unexpected API call: ${method.name}" }
    load(args!![0] as Int, args[1] as Int, args[2] as CandidateSnapshot?)
} as RimeApi

private fun candidateSession(api: RimeApi): RimeSession = object : RimeSession {
    override fun <T> run(block: suspend RimeApi.() -> T): T = error("Not used by paging")

    override suspend fun <T> runOnReady(block: suspend RimeApi.() -> T): T = block(api)

    override fun runIfReady(block: suspend RimeApi.() -> Unit) = error("Not used by paging")

    override val lifecycleScope = CoroutineScope(Dispatchers.Unconfined)
}
