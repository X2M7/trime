/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ime.core

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.PopupWindow
import timber.log.Timber

class TouchEventReceiverWindow(
    private val contentView: View,
) {
    private val ctx = contentView.context

    private fun createWindow() = PopupWindow(
        object : View(ctx) {
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouchEvent(event: MotionEvent): Boolean = contentView.dispatchTouchEvent(event)
        },
    ).apply {
        // disable animation
        animationStyle = 0
    }

    private var window = createWindow()

    private var requested = false
    private var requestedBounds: Rect? = null
    private var shownBounds: Rect? = null
    private var observedTree: ViewTreeObserver? = null

    private val preDrawListener = ViewTreeObserver.OnPreDrawListener {
        updateWindow()
        true
    }

    private val cachedLocation = intArrayOf(0, 0)

    init {
        contentView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                observeTree()
                updateWindow()
            }

            override fun onViewDetachedFromWindow(v: View) {
                observedTree?.takeIf { it.isAlive }?.removeOnPreDrawListener(preDrawListener)
                observedTree = null
                dismiss()
            }
        })
        contentView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateWindow() }
        if (contentView.isAttachedToWindow) observeTree()
    }

    private fun observeTree() {
        val tree = contentView.viewTreeObserver
        if (tree === observedTree) return
        observedTree?.takeIf { it.isAlive }?.removeOnPreDrawListener(preDrawListener)
        tree.addOnPreDrawListener(preDrawListener)
        observedTree = tree
    }

    fun showAt(
        x: Int,
        y: Int,
        w: Int,
        h: Int,
    ) {
        requested = true
        requestedBounds = Rect(x, y, x + w, y + h)
        updateWindow()
    }

    fun show() {
        requested = true
        requestedBounds = null
        updateWindow()
    }

    private fun updateWindow() {
        if (!requested) return
        // Engine messages can arrive before a replacement input view is attached or laid out.
        if (!contentView.isAttachedToWindow || contentView.windowToken == null ||
            !contentView.isShown || contentView.windowVisibility != View.VISIBLE
        ) {
            window.dismiss()
            shownBounds = null
            return
        }
        val (x, y) = cachedLocation.also { contentView.getLocationInWindow(it) }
        val bounds = requestedBounds ?: Rect(x, y, x + contentView.width, y + contentView.height)
        if (bounds.isEmpty) {
            window.dismiss()
            shownBounds = null
            return
        }
        try {
            if (window.isShowing) {
                // Updating on every draw can schedule another traversal indefinitely.
                if (shownBounds != bounds) window.update(bounds.left, bounds.top, bounds.width(), bounds.height())
            } else {
                window.width = bounds.width()
                window.height = bounds.height()
                window.showAtLocation(contentView, Gravity.TOP or Gravity.START, bounds.left, bounds.top)
            }
            shownBounds = bounds
        } catch (_: WindowManager.BadTokenException) {
            // WindowManager can revoke a previously valid token during an IME switch.
            // A failed show leaves PopupWindow's flag set without registering a view.
            requested = false
            requestedBounds = null
            shownBounds = null
            window = createWindow()
            Timber.i("Input window token expired before the touch overlay attached")
        }
    }

    fun dismiss() {
        requested = false
        requestedBounds = null
        shownBounds = null
        window.dismiss()
    }
}
