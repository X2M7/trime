/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.ime.composition

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.sizeDp
import com.osfans.trime.R
import com.osfans.trime.core.T9Action
import com.osfans.trime.core.T9SpanProto
import com.osfans.trime.core.T9StateProto
import com.osfans.trime.data.theme.ColorManager
import splitties.dimensions.dp

/** Independent syllable controls. Hanzi remain in the existing candidate bar. */
class T9DisambiguationView(
    context: Context,
    private val action: (Int, T9Action, Int, Int, String) -> Unit,
) : LinearLayout(context) {
    private var state = T9StateProto()
    private val normalTextColor = ColorManager.getColor("candidate_text_color")
    private val selectedForeground = ColorManager.getColor("hilited_candidate_text_color")
    private val selectedBackground = ColorManager.getColor("hilited_candidate_back_color")
    private val segments = LinearLayout(context)
    private val segmentScroller = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        addView(segments, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
    }
    private val adapter = ChoiceAdapter()
    private val undo = icon("cmd_undo", R.string.undo) { send(T9Action.Undo) }
    private val unlock = icon("cmd_lock_open_variant_outline", R.string.t9_unlock) {
        send(T9Action.Unlock, state.focus)
    }
    private val choices = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        adapter = this@T9DisambiguationView.adapter
        itemAnimator = null
        overScrollMode = OVER_SCROLL_NEVER
    }

    init {
        id = View.generateViewId()
        isVisible = false
        background = ColorManager.getDrawable("candidate_background")
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            orientation = HORIZONTAL
            addView(segmentScroller, LayoutParams(0, dp(48), 0.35f))
            addView(choices, LayoutParams(0, dp(48), 0.65f))
            addView(unlock, LayoutParams(dp(48), dp(48)))
            addView(undo, LayoutParams(dp(48), dp(48)))
        } else {
            orientation = VERTICAL
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                addView(segmentScroller, LayoutParams(0, dp(48), 1f))
                addView(unlock, LayoutParams(dp(48), dp(48)))
                addView(undo, LayoutParams(dp(48), dp(48)))
            }
            addView(row, LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))
            addView(choices, LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))
        }
    }

    private fun send(kind: T9Action, start: Int = 0, end: Int = 0, spelling: String = "") {
        action(state.revision, kind, start, end, spelling)
    }

    private fun icon(name: String, label: Int, click: () -> Unit) = ImageButton(context).apply {
        setImageDrawable(IconicsDrawable(context, name).apply { sizeDp = 22 })
        imageTintList = ColorStateList.valueOf(normalTextColor)
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        contentDescription = context.getString(label)
        TooltipCompat.setTooltipText(this, contentDescription)
        setOnClickListener { click() }
    }

    private fun label() = TextView(context).apply {
        gravity = Gravity.CENTER
        textSize = 17f
        setSingleLine()
        minWidth = dp(48)
        setPadding(dp(12), 0, dp(12), 0)
        setTextColor(normalTextColor)
    }

    fun update(data: T9StateProto) {
        val focusChanged = data.focus != state.focus || data.input != state.input
        state = data
        isVisible = data.enabled
        if (!data.enabled) return
        segments.removeAllViews()
        data.segments.forEach { span ->
            segments.addView(
                label().apply {
                    text = span.spelling
                    val selected = span.start == data.focus
                    setTextColor(if (selected) selectedForeground else normalTextColor)
                    setBackgroundColor(if (selected) selectedBackground else android.graphics.Color.TRANSPARENT)
                    if (span.locked) {
                        setCompoundDrawablesRelativeWithIntrinsicBounds(
                            IconicsDrawable(context, "cmd_lock_outline").apply { sizeDp = 14 },
                            null,
                            null,
                            null,
                        )
                        compoundDrawablePadding = dp(4)
                        TextViewCompat.setCompoundDrawableTintList(
                            this,
                            ColorStateList.valueOf(if (selected) selectedForeground else normalTextColor),
                        )
                    }
                    contentDescription = context.getString(
                        if (span.locked) R.string.t9_locked_syllable else R.string.t9_input_segment,
                        span.spelling,
                    )
                    setOnClickListener { send(T9Action.Focus, span.start) }
                },
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT),
            )
        }
        unlock.isEnabled = data.segments.any { it.start == data.focus && it.locked }
        undo.isEnabled = data.canUndo
        unlock.alpha = if (unlock.isEnabled) 1f else 0.35f
        undo.alpha = if (undo.isEnabled) 1f else 0.35f
        adapter.update(data.choices)
        if (focusChanged) {
            choices.scrollToPosition(0)
            val selectedIndex = data.segments.indexOfFirst { it.start == data.focus }
            segments.getChildAt(selectedIndex)?.let { child ->
                child.post {
                    child.requestRectangleOnScreen(Rect(0, 0, child.width, child.height), true)
                }
            }
        }
    }

    private inner class ChoiceAdapter : RecyclerView.Adapter<ChoiceHolder>() {
        private var items = emptyArray<T9SpanProto>()

        fun update(value: Array<T9SpanProto>) {
            if (items.contentEquals(value)) return
            items = value
            notifyDataSetChanged()
        }

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ChoiceHolder(
            label().apply {
                layoutParams = RecyclerView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
            },
        )

        override fun onBindViewHolder(holder: ChoiceHolder, position: Int) {
            val span = items[position]
            holder.text.text = if (span.completion) context.getString(R.string.t9_completion, span.spelling) else span.spelling
            holder.text.setOnClickListener { send(T9Action.Lock, span.start, span.end, span.spelling) }
        }
    }

    private class ChoiceHolder(val text: TextView) : RecyclerView.ViewHolder(text)
}
