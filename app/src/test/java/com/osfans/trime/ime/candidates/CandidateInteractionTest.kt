/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.ime.candidates

import com.osfans.trime.core.CandidateProto
import com.osfans.trime.core.CandidateSnapshot
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CandidateInteractionTest :
    StringSpec({
        val identity = CandidateIdentity(CandidateSnapshot(7, 11, 19), 2)

        "a refresh between touch down and release cannot select the replacement row" {
            val touch = CandidateTouchState()
            touch.begin(identity)
            touch.accepts(identity) shouldBe true
            listOf(
                identity.copy(index = 3),
                identity.copy(snapshot = identity.snapshot.copy(generation = 12)),
                identity.copy(snapshot = identity.snapshot.copy(editorToken = 8)),
                identity.copy(snapshot = identity.snapshot.copy(requestId = 20)),
            ).forEach { replacement -> touch.accepts(replacement) shouldBe false }
            touch.finish()
            touch.begin(identity.copy(snapshot = identity.snapshot.copy(generation = 12)))
            touch.accepts(identity.copy(snapshot = identity.snapshot.copy(generation = 12))) shouldBe true
        }

        "cancelled scrolling and rebound touches cannot revive by restoring the same row" {
            val touch = CandidateTouchState()
            touch.begin(identity)
            touch.cancel()
            touch.accepts(identity) shouldBe false
            touch.accepts(identity.copy(index = 3)) shouldBe false
            touch.finish()
            touch.begin(identity)
            touch.accepts(identity) shouldBe true
        }

        "accessibility actions work without a touch but require a valid row identity" {
            val touch = CandidateTouchState()
            touch.accepts(identity) shouldBe true
            touch.accepts(null) shouldBe false
            touch.accepts(identity.copy(index = -1)) shouldBe false
            touch.accepts(identity.copy(snapshot = CandidateSnapshot.EMPTY)) shouldBe false
            touch.begin(null)
            touch.accepts(identity) shouldBe false
        }

        "duplicate text and real string hash collisions receive separate stable row IDs" {
            val first = CandidateProto("FB", "", "")
            val collision = CandidateProto("Ea", "", "")
            first.hashCode() shouldBe collision.hashCode()
            val rows = listOf(first, collision, first).mapIndexed { index, candidate ->
                CandidateRow(candidate, index, identity.snapshot)
            }
            rows.map { candidateStableId(it.index) }.toSet().size shouldBe 3
            candidateStableId(Int.MAX_VALUE) shouldBe Int.MAX_VALUE.toLong()
        }

        "reopening and repeated layout notifications retain one expanded snapshot viewport" {
            val viewport = CandidateViewportState()
            viewport.update(identity.snapshot, 16) shouldBe true
            repeat(100) { viewport.update(identity.snapshot, 16) shouldBe false }
            viewport.update(identity.snapshot, 0) shouldBe true
            viewport.update(identity.snapshot, 0) shouldBe false
            viewport.update(identity.snapshot.copy(generation = 12), 0) shouldBe true
            viewport.update(identity.snapshot.copy(editorToken = 8), 0) shouldBe true
        }
    })
