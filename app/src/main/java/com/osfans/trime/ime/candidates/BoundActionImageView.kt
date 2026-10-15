/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.ime.candidates

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView

/** API 21 ImageView initializes its class after its View delegate has run. */
@SuppressLint("ViewConstructor")
internal class BoundActionImageView(context: Context) :
    ImageView(context),
    BoundActionOwner {
    override var clickBinding: VersionedClickBinding<*>? = null

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        clickBinding?.populateAccessibilityNodeInfo(info)
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean = clickBinding?.performBoundAccessibilityAction(action) { super.performAccessibilityAction(action, arguments) }
        ?: super.performAccessibilityAction(action, arguments)
}
