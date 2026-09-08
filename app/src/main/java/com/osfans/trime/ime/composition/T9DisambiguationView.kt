/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.ime.composition

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.sizeDp
import com.osfans.trime.R
import com.osfans.trime.core.T9Action
import com.osfans.trime.core.T9SpanProto
import com.osfans.trime.core.T9StateProto
import com.osfans.trime.data.theme.ThemeScope
import splitties.dimensions.dp

/** Independent syllable controls. Hanzi remain in the existing candidate bar. */
// Requires an input-window theme and engine callback; not an XML widget.
@SuppressLint("ViewConstructor")
class T9DisambiguationView(
    context: Context,
    private val scope: ThemeScope,
    private val action: (Int, T9Action, Int, Int, String) -> Unit,
) : LinearLayout(context) {
    private var state = T9StateProto()
    private val normalTextColor get() = scope.colors.candidateTextColor
    private val selectedForeground get() = scope.colors.hilitedCandidateTextColor
    private val selectedBackground get() = scope.colors.hilitedCandidateBackColor
    private val segments = LinearLayout(context)
    private val segmentScroller = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        addView(segments, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
    }
    private val adapter = ChoiceAdapter()
    private val labelContext = ContextThemeWrapper(context, R.style.Theme_TrimeAppTheme)
    private var layoutActive = false
    private var sideMode = false
    val sidebar = FrameLayout(context).apply { id = View.generateViewId() }
    private val undo = icon("cmd_undo", R.string.undo) { send(T9Action.Undo) }
    private val cancel = icon("cmd_close", R.string.t9_cancel) { send(T9Action.Cancel) }
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
        background = scope.drawable("candidate_background")
        sidebar.background = scope.drawable("candidate_background")
        orientation = VERTICAL
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            addView(segmentScroller, LayoutParams(0, dp(48), 1f))
            addView(unlock, LayoutParams(dp(48), dp(48)))
            addView(undo, LayoutParams(dp(48), dp(48)))
            addView(cancel, LayoutParams(dp(48), dp(48)))
        }
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))
        addView(choices, LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))
    }

    fun setKeyboardLayout(active: Boolean, side: Boolean) {
        layoutActive = active
        isVisible = active || state.enabled
        if (sideMode == side) return
        sideMode = side
        (choices.parent as ViewGroup).removeView(choices)
        (choices.layoutManager as LinearLayoutManager).orientation = if (side) RecyclerView.VERTICAL else RecyclerView.HORIZONTAL
        if (side) {
            sidebar.addView(choices, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        } else {
            addView(choices, LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))
        }
        // Recreate holders because their fixed axis changes with the list orientation.
        choices.adapter = null
        choices.recycledViewPool.clear()
        choices.adapter = adapter
    }

    private fun send(kind: T9Action, start: Int = 0, end: Int = 0, spelling: String = "") {
        action(state.revision, kind, start, end, spelling)
    }

    fun refreshColors() {
        background = scope.drawable("candidate_background")
        sidebar.background = scope.drawable("candidate_background")
        val tint = ColorStateList.valueOf(normalTextColor)
        undo.imageTintList = tint
        cancel.imageTintList = tint
        unlock.imageTintList = tint
        update(state)
        adapter.notifyItemRangeChanged(0, adapter.itemCount)
    }

    private fun icon(name: String, label: Int, click: () -> Unit) = ImageButton(context).apply {
        setImageDrawable(IconicsDrawable(context, name).apply { sizeDp = 22 })
        imageTintList = ColorStateList.valueOf(normalTextColor)
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        contentDescription = context.getString(label)
        TooltipCompat.setTooltipText(this, contentDescription)
        setOnClickListener { click() }
    }

    private fun label() = AppCompatTextView(labelContext).apply {
        gravity = Gravity.CENTER
        textSize = 17f
        setSingleLine()
        minWidth = dp(48)
        setPadding(dp(12), 0, dp(12), 0)
        setTextColor(normalTextColor)
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this, 10, 17, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
    }

    private fun sourceLabel(sources: Int): String {
        val labels = listOf(
            "n/l", "z/zh", "c/ch", "s/sh", "en/eng", "in/ing",
            context.getString(R.string.t9_source_adjacent),
            context.getString(R.string.t9_source_missing),
            context.getString(R.string.t9_source_repeat),
        )
        return labels.filterIndexed { index, _ -> sources and (1 shl index) != 0 }.joinToString(", ")
    }

    private fun showSpelling(view: TextView, span: T9SpanProto, normal: String) {
        view.setSingleLine(span.sources == 0)
        view.maxLines = if (span.sources == 0) 1 else 2
        if (span.sources == 0) {
            view.text = normal
            view.contentDescription = normal
        } else {
            val source = sourceLabel(span.sources)
            val text = SpannableStringBuilder(span.spelling).append('\n')
            val start = text.length
            text.append(source)
            text.setSpan(RelativeSizeSpan(0.65f), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            view.text = text
            val description = if (span.sources and 63 != 0) context.getString(R.string.t9_source_fuzzy, source) else source
            view.contentDescription = context.getString(R.string.t9_suggestion_source, span.spelling, description)
        }
        TooltipCompat.setTooltipText(view, view.contentDescription)
    }

    fun update(data: T9StateProto) {
        val focusChanged = data.focus != state.focus || data.input != state.input
        state = data
        isVisible = layoutActive || data.enabled
        segments.removeAllViews()
        data.segments.forEach { span ->
            segments.addView(
                label().apply {
                    showSpelling(this, span, span.spelling)
                    val selected = span.start == data.focus
                    isSelected = selected
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
                        contentDescription,
                    )
                    if (selected) contentDescription = context.getString(R.string.t9_current_segment, contentDescription)
                    setOnClickListener { action(data.revision, T9Action.Focus, span.start, 0, "") }
                },
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT),
            )
        }
        unlock.isEnabled = data.segments.any { it.start == data.focus && it.locked }
        undo.isEnabled = data.canUndo
        cancel.isEnabled = data.input.isNotEmpty()
        unlock.alpha = if (unlock.isEnabled) 1f else 0.35f
        undo.alpha = if (undo.isEnabled) 1f else 0.35f
        cancel.alpha = if (cancel.isEnabled) 1f else 0.35f
        adapter.update(data.choices, data.revision)
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
        private var revision = -1

        fun update(value: Array<T9SpanProto>, newRevision: Int) {
            if (items.contentEquals(value) && revision == newRevision) return
            val previous = items
            val previousRevision = revision
            val difference = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = previous.size
                override fun getNewListSize() = value.size
                override fun areItemsTheSame(old: Int, new: Int) = previous[old] == value[new]
                override fun areContentsTheSame(old: Int, new: Int) = previousRevision == newRevision && previous[old] == value[new]
            })
            items = value.copyOf()
            revision = newRevision
            difference.dispatchUpdatesTo(this)
        }

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ChoiceHolder(
            label().apply {
                layoutParams = RecyclerView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
                if (sideMode) {
                    layoutParams = RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, dp(48))
                    setPadding(dp(4), 0, dp(4), 0)
                    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this, 10, 20, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
                }
            },
        )

        override fun onBindViewHolder(holder: ChoiceHolder, position: Int) {
            val span = items[position]
            val boundRevision = revision
            holder.text.setTextColor(normalTextColor)
            val description = if (span.completion) context.getString(R.string.t9_completion, span.spelling) else span.spelling
            showSpelling(holder.text, span, if (sideMode && span.completion) "${span.spelling}+" else description)
            if (span.sources == 0) holder.text.contentDescription = description
            holder.text.setOnClickListener { action(boundRevision, T9Action.Lock, span.start, span.end, span.spelling) }
        }
    }

    private class ChoiceHolder(val text: TextView) : RecyclerView.ViewHolder(text)
}
