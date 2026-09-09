// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.ime.keyboard

import android.graphics.Point
import android.os.Build
import android.text.InputType
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import com.osfans.trime.R
import com.osfans.trime.core.CompositionProto
import com.osfans.trime.core.RimeMessage
import com.osfans.trime.core.SchemaItem
import com.osfans.trime.daemon.RimeSession
import com.osfans.trime.data.theme.KeyActionManager
import com.osfans.trime.data.theme.Theme
import com.osfans.trime.data.theme.model.TextKeyboard
import com.osfans.trime.ime.broadcast.EnterKeyDisplayDelegate
import com.osfans.trime.ime.broadcast.InputBroadcastReceiver
import com.osfans.trime.ime.core.EditorKeyboard
import com.osfans.trime.ime.core.EditorModeOverride
import com.osfans.trime.ime.core.EditorPolicy
import com.osfans.trime.ime.core.TrimeInputMethodService
import com.osfans.trime.ime.keyboard.KeyboardPrefs.isLandscapeMode
import com.osfans.trime.ime.popup.PopupDelegate
import com.osfans.trime.ime.window.BoardWindow
import com.osfans.trime.ime.window.ResidentWindow
import com.osfans.trime.util.isLandscape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.instance
import splitties.dimensions.dp
import splitties.systemservices.windowManager
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import timber.log.Timber

class KeyboardWindow(di: DI) :
    BoardWindow.NoBarBoardWindow(di),
    ResidentWindow,
    InputBroadcastReceiver {
    private val service: TrimeInputMethodService by instance()
    private val theme: Theme by instance()
    private val rime: RimeSession by instance()
    private val commonKeyboardActionListener: CommonKeyboardActionListener by instance()
    private val popup: PopupDelegate by instance()
    private val enterKeyDisplay: EnterKeyDisplayDelegate by instance()

    private val cursorCapsMode: Int
        get() =
            service.currentInputEditorInfo.run {
                if (inputType != InputType.TYPE_NULL) {
                    service.currentInputConnection?.getCursorCapsMode(inputType) ?: 0
                } else {
                    0
                }
            }

    private val _currentKeyboardHeight =
        MutableSharedFlow<Int>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    val currentKeyboardHeight = _currentKeyboardHeight.asSharedFlow()

    private lateinit var keyboardView: FrameLayout

    companion object : ResidentWindow.Key

    lateinit var currentKeyboard: Keyboard
        private set

    override val key: ResidentWindow.Key
        get() = KeyboardWindow

    private val presetKeyboardIds = theme.presetKeyboards.keys.toList()
    private val cycleKeyboardIds = presetKeyboardIds.filterNot { it in theme.fallbackKeyboards.values }
    private var currentKeyboardId = ""
    private var lastKeyboardId = ""
    private var lastLockKeyboardId = ""
    private var restrictedEditor = false
    private var desiredAsciiMode: Boolean? = null
    private var preservedSymbolMode: Pair<Keyboard, Boolean>? = null
    private val cachedKeyboards = mutableMapOf<String, Pair<Keyboard, KeyboardView>>()
    private val activeKeyboard: Keyboard? get() = cachedKeyboards[currentKeyboardId]?.first
    private val currentKeyboardView: KeyboardView? get() = cachedKeyboards[currentKeyboardId]?.second

    private val keyboardActionListener = commonKeyboardActionListener.listener

    private var lastIsPortrait: Boolean? = null
    private var containerWidth: Int = 0
    private var allowedWidth: Int = 0

    private val onKeyboardViewLayoutChangeListener =
        View.OnLayoutChangeListener { v, left, _, right, _, _, _, _, _ ->
            val width = right - left
            if (width > 0 && allowedWidth != width) {
                val isPortrait = !context.resources.configuration.isLandscape()
                lastIsPortrait = isPortrait
                containerWidth = width
                allowedWidth = width
                v.post { refreshKeyboards(isAll = true) }
            }
        }

    override fun onCreateView(): View {
        keyboardView = context.frameLayout(R.id.keyboard_view)
        keyboardView.addOnLayoutChangeListener(onKeyboardViewLayoutChangeListener)
        attachKeyboard(evalKeyboard(".default"), updateMode = false)
        return keyboardView
    }

    private fun detachCurrentView() {
        currentKeyboardView?.also {
            it.onDetach()
            keyboardView.removeView(it)
        }
        activeKeyboard?.takeUnless { restrictedEditor }?.let { keyboard ->
            keyboard.lastAsciiMode = preservedSymbolMode?.takeIf { it.first === keyboard }?.second
                ?: desiredAsciiMode ?: rime.run { statusCached }.isAsciiMode
        }
        preservedSymbolMode = null
    }

    /** 计算键盘可用宽度：优先使用已测量的容器宽度，否则回退到系统窗口测量。 */
    private fun computeAllowedWidth(): Int {
        val isPortrait = !context.resources.configuration.isLandscape()

        if (containerWidth > 0 && lastIsPortrait == isPortrait) {
            return containerWidth
        }

        val padding = theme.generalStyle.run {
            if (context.isLandscapeMode()) keyboardPaddingLand else keyboardPadding
        }

        val safeWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = context.windowManager.maximumWindowMetrics
            val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
            )
            val displayWidth = context.resources.displayMetrics.widthPixels
            val windowWidth = windowMetrics.bounds.width() - insets.left - insets.right
            if (windowWidth < displayWidth - context.dp(1)) displayWidth else windowWidth
        } else {
            @Suppress("DEPRECATION")
            val size = Point()
            @Suppress("DEPRECATION")
            context.windowManager.defaultDisplay.getSize(size)
            size.x
        }

        val width = safeWidth - 2 * context.dp(padding)
        allowedWidth = width
        return width
    }

    private fun selectKeyboardConfig(name: String): TextKeyboard? = try {
        resolveKeyboardConfig(name, theme.presetKeyboards)
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "Invalid keyboard import")
        theme.presetKeyboards["default"]?.takeIf { it.importPreset.isEmpty() }
    }

    private fun attachKeyboard(target: String, updateMode: Boolean = true) {
        currentKeyboardId = target
        if (!restrictedEditor) lastKeyboardId = target

        val config = selectKeyboardConfig(target)
        val keyboard = activeKeyboard ?: Keyboard(context, theme, computeAllowedWidth(), config)
        val view = currentKeyboardView ?: KeyboardView(context, theme, keyboard, popup, service, keyboardActionListener, enterKeyDisplay)

        if (activeKeyboard == null) {
            cachedKeyboards[target] = keyboard to view
            keyboard.lastAsciiMode = keyboard.asciiMode
        }

        keyboard.also {
            currentKeyboard = it
            runBlocking { _currentKeyboardHeight.emit(it.keyboardHeight) }
            if (it.isLock && !restrictedEditor) lastLockKeyboardId = target
            dispatchCapsState(it::setShifted)

            val currentMode = rime.run { statusCached }.isAsciiMode
            val targetMode = if (restrictedEditor) {
                true
            } else if (it.resetAsciiMode) {
                it.asciiMode
            } else {
                it.lastAsciiMode
            }

            if (updateMode) desiredAsciiMode = targetMode
            if (updateMode && currentMode != targetMode) {
                service.postRimeJob {
                    // Opening the symbol page is navigation, not a T9 commit command.
                    if ((target == "symbols" || target == theme.fallbackKeyboards["symbols"]) && t9Cached.enabled && statusCached.isComposing) {
                        withContext(Dispatchers.Main) {
                            if (activeKeyboard === keyboard) {
                                preservedSymbolMode = keyboard to targetMode
                            } else {
                                keyboard.lastAsciiMode = targetMode
                            }
                        }
                        return@postRimeJob
                    }
                    commitComposition()
                    setRuntimeOption("ascii_mode", targetMode)
                }
            }
        }

        view.let {
            it.updateEnterLabel()
            keyboardView.apply {
                (it.parent as? android.view.ViewGroup)?.removeView(it)
                add(it, lParams(matchParent, matchParent))
            }
        }
    }

    private fun smartMatchKeyboard(): String {
        val statusSchema = rime.run { statusCached }.schemaId
        val schema = rime.run { schemaCached }
        val schemaId = statusSchema.ifEmpty { schema.schemaId }
        return matchKeyboard(schemaId, schema.keyboard, schema.isPinyinT9, schema.alphabet, theme.presetKeyboards, theme.fallbackKeyboards)
    }

    private fun evalKeyboard(id: String): String {
        val currentIdx = cycleKeyboardIds.indexOfFirst { currentKeyboardId == it }
        val dot =
            when (id) {
                ".default" -> smartMatchKeyboard()
                ".prior" -> cycleKeyboardIds.getOrNull(currentIdx - 1) ?: currentKeyboardId
                ".next" -> cycleKeyboardIds.getOrNull(currentIdx + 1) ?: currentKeyboardId
                ".last" -> lastKeyboardId
                ".last_lock" -> lastLockKeyboardId
                ".ascii" -> {
                    var ascii = activeKeyboard?.asciiKeyboard
                    if (ascii.isNullOrEmpty()) {
                        ascii = lastLockKeyboardId
                    }
                    if (presetKeyboardIds.contains(ascii) && selectKeyboardConfig(ascii)?.asciiMode == true) {
                        ascii
                    } else {
                        theme.fallbackKeyboards["letter"] ?: "default"
                    }
                }
                else -> {
                    id.ifEmpty {
                        if (activeKeyboard?.isLock == true) currentKeyboardId else lastLockKeyboardId
                    }
                }
            }
        var final = dot.ifEmpty { smartMatchKeyboard() }

        // 切换到横屏布局
        if (service.isLandscapeMode()) {
            val landscape =
                theme.presetKeyboards[final]?.landscapeKeyboard ?: ""
            if (landscape.isNotEmpty() && presetKeyboardIds.contains(landscape)) final = landscape
        }
        return final
    }

    fun switchKeyboard(to: String) {
        service.lifecycleScope.launch(Dispatchers.Main.immediate) {
            val target = evalKeyboard(to)
            if (cachedKeyboards.containsKey(target)) {
                if (target == currentKeyboardId) return@launch
            }
            detachCurrentView()
            attachKeyboard(target)
            Timber.d("Switched to keyboard: $target")
        }
    }

    fun refreshKeyboards(isAll: Boolean = false) {
        val id = currentKeyboardId.ifEmpty { return }
        detachCurrentView()
        if (isAll) {
            cachedKeyboards.clear()
        } else {
            cachedKeyboards.remove(id)
        }
        attachKeyboard(id)
    }

    /** Repaints the keyboard after a color-scheme switch; keys re-resolve their colors. */
    override fun refreshColors() {
        currentKeyboardView?.invalidateAllKeys()
    }

    override fun onStartInput(info: EditorInfo) {
        val policy = EditorPolicy.keyboard(info.inputType, info.imeOptions)
        val schema = rime.run { statusCached.schemaId }
        val saved = service.editorModeOverride
        val ascii = desiredAsciiMode ?: rime.run { statusCached.isAsciiMode }
        if (policy != EditorKeyboard.USER && saved == null) {
            service.editorModeOverride = EditorModeOverride(schema, currentKeyboardId, ascii)
        }
        val target = when (policy) {
            EditorKeyboard.ASCII -> evalKeyboard(".ascii")
            EditorKeyboard.NUMBER -> "number".takeIf { it in presetKeyboardIds }
                ?: theme.fallbackKeyboards["number"] ?: "default"
            EditorKeyboard.USER -> saved?.takeIf { it.schemaId == schema && it.keyboardId in presetKeyboardIds }?.keyboardId
                ?: if (saved != null) smartMatchKeyboard() else evalKeyboard("")
        }
        detachCurrentView()
        restrictedEditor = policy != EditorKeyboard.USER
        attachKeyboard(target, updateMode = false)
        val targetMode = when {
            restrictedEditor -> true
            saved != null && saved.schemaId == schema -> saved.asciiMode
            saved != null -> currentKeyboard.asciiMode
            theme.generalStyle.resetAsciiModeOnFocusChange ->
                if (currentKeyboard.resetAsciiMode) currentKeyboard.asciiMode else currentKeyboard.lastAsciiMode
            else -> ascii
        }
        desiredAsciiMode = targetMode
        if (!restrictedEditor) {
            currentKeyboard.lastAsciiMode = targetMode
            service.editorModeOverride = null
        }
        // Always enqueue the final policy: an earlier layout switch may still be in flight.
        service.postRimeJob { setRuntimeOption("ascii_mode", targetMode) }
    }

    private fun dispatchCapsState(setShift: (Boolean, Boolean) -> Unit) {
        val status = rime.run { statusCached }
        // TODO: 启用自动首句大写后，点击方向键时，保持Shift锁定状态功能将无法生效
        if (theme.generalStyle.autoCaps && status.isAsciiMode && currentKeyboardView?.isCapsOn == false) {
            setShift(false, cursorCapsMode != 0)
        }
    }

    override fun onKeyAppearanceUpdate(composing: Boolean, menu: Boolean, paging: Boolean) {
        if (!rime.run { statusCached }.isAsciiMode) {
            activeKeyboard?.appearanceStateKeys?.forEach { key ->
                currentKeyboardView?.invalidateKeyByIndex(key.index)
            }
        }
    }

    override fun onSelectionUpdate(
        start: Int,
        end: Int,
    ) {
        dispatchCapsState { on, shifted ->
            activeKeyboard?.setShifted(on, shifted)?.let { if (it) currentKeyboardView?.invalidateAllKeys() }
        }
    }

    override fun onEnterKeyLabelUpdate(label: String) {
        currentKeyboardView?.updateEnterLabel()
    }

    override fun onRimeSchemaUpdated(schema: SchemaItem) {
        // A lock belongs to the previous schema. Otherwise a non-locking new
        // default (for example Tongwenfeng's full pinyin) reselects the old T9
        // layout on this or the next editor-focus update.
        lastLockKeyboardId = ""
        switchKeyboard(".default")
        service.currentInputEditorInfo?.let { onStartInput(it) }
    }

    override fun onRimeOptionUpdated(value: RimeMessage.OptionMessage.Data) {
        val option = value.option
        when {
            option == "ascii_mode" -> desiredAsciiMode = value.value
            option.startsWith("_keyboard_") -> {
                val target = option.removePrefix("_keyboard_")
                if (target.isNotEmpty()) {
                    switchKeyboard(target)
                }
            }
            option.startsWith("_key_") -> {
                val what = option.removePrefix("_key_")
                if (what.isNotEmpty() && value.value) {
                    commonKeyboardActionListener
                        .listener
                        .onAction(KeyActionManager.getAction(what))
                }
            }
        }
        currentKeyboardView?.invalidateAllKeys()
    }

    override fun onAttached() {
    }

    override fun onDetached() {
        currentKeyboardView?.onDetach()
    }

    fun finishInput() {
        currentKeyboardView?.onDetach()
    }
}
