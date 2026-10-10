/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.ime.candidates

import com.osfans.trime.core.CandidateSnapshot

/** An index has meaning only in the engine snapshot that supplied it. */
data class CandidateIdentity(val snapshot: CandidateSnapshot, val index: Int) {
    val isValid: Boolean get() = snapshot.isValid && index >= 0
}

/** Captures the row at touch-down so a rebound view cannot accept the release. */
class CandidateTouchState {
    private var pressed: CandidateIdentity? = null
    var isActive = false
        private set

    fun begin(identity: CandidateIdentity?) {
        pressed = identity
        isActive = true
    }

    fun cancel() {
        pressed = null
    }

    fun accepts(identity: CandidateIdentity?): Boolean = identity?.isValid == true && (!isActive || pressed == identity)

    fun finish() {
        pressed = null
        isActive = false
    }
}

/** Candidate text is not a unique identifier: duplicates and hash collisions are valid. */
internal fun candidateStableId(index: Int): Long = index.toLong()

/** Repeated layout notifications must not reset an expanded list's viewport. */
class CandidateViewportState {
    private var snapshot = CandidateSnapshot.EMPTY
    private var offset = -1

    fun update(nextSnapshot: CandidateSnapshot, nextOffset: Int): Boolean {
        val changed = snapshot != nextSnapshot || offset != nextOffset
        snapshot = nextSnapshot
        offset = nextOffset
        return changed
    }
}
