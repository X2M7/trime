/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.ime.candidates

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import com.osfans.trime.ime.keyboard.GestureFrame

/** Cancels a held candidate if an asynchronous refresh rebinds its view. */
class CandidateGestureFrame(context: Context) : GestureFrame(context) {
    private val touchState = CandidateTouchState()

    var identity: CandidateIdentity? = null
        set(value) {
            if (field != value && touchState.isActive) {
                touchState.cancel()
                cancelGesture()
            }
            field = value
        }

    // GestureFrame dispatches an accepted click through our performClick override.
    // Calling it again here would select twice; accessibility also uses that override.
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> touchState.begin(identity)
            MotionEvent.ACTION_CANCEL -> touchState.cancel()
        }
        if (event.actionMasked == MotionEvent.ACTION_UP && !touchState.accepts(identity)) {
            cancelGesture()
            touchState.finish()
            return true
        }
        return try {
            super.onTouchEvent(event)
        } finally {
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                touchState.finish()
            }
        }
    }

    override fun performClick(): Boolean = touchState.accepts(identity) && super.performClick()

    override fun performLongClick(): Boolean = touchState.accepts(identity) && super.performLongClick()

    override fun getBaseline(): Int {
        val child = getChildAt(0) ?: return super.getBaseline()
        return child.baseline.takeIf { it >= 0 }?.let { child.top + it } ?: super.getBaseline()
    }
}
