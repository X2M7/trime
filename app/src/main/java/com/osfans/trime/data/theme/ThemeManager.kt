/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.data.theme

import android.content.res.Configuration
import com.osfans.trime.data.base.DataManager
import com.osfans.trime.data.prefs.AppPrefs
import com.osfans.trime.util.WeakHashSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

object ThemeManager {
    fun interface OnThemeChangeListener {
        fun onThemeChange(theme: Theme)
    }

    fun getAllThemes(): List<ThemeItem> {
        val sharedThemes = ThemeFilesManager.listThemes(DataManager.sharedDataDir)
        val userThemes = ThemeFilesManager.listThemes(DataManager.userDataDir)
        return sharedThemes + userThemes
    }

    private var _activeTheme: Theme? = null
    private var _activeFindings: List<ThemeDiagnostics.Finding>? = null
    private var activeConfigId: String? = null
    private val loadMutex = Mutex()

    // Reading a theme must never start deployment from a view/layout callback.
    val activeTheme: Theme
        get() = checkNotNull(_activeTheme) { "ThemeManager.init must complete before creating themed views" }

    /**
     * What static checks found in the active theme, or null when it was read
     * but the checks could not run. The findings always belong to the load
     * that produced [activeTheme].
     */
    val activeFindings: List<ThemeDiagnostics.Finding>?
        get() {
            checkNotNull(_activeTheme) { "ThemeManager.init must complete before reading theme diagnostics" }
            return _activeFindings
        }

    private val onChangeListeners = WeakHashSet<OnThemeChangeListener>()

    fun addOnChangedListener(listener: OnThemeChangeListener) {
        onChangeListeners.add(listener)
    }

    fun removeOnChangedListener(listener: OnThemeChangeListener) {
        onChangeListeners.remove(listener)
    }

    private fun fireChange() {
        onChangeListeners.forEach { it.onThemeChange(activeTheme) }
    }

    val prefs = AppPrefs.defaultInstance().registerProvider(::ThemePrefs)

    private data class ResolvedTheme(
        val configId: String,
        val theme: Theme,
        val findings: List<ThemeDiagnostics.Finding>?,
    )

    private suspend fun getThemeById(id: String): ResolvedTheme {
        when (val result = ThemeLoader.loadTheme(id)) {
            is ThemeLoader.ThemeLoadResult.Success -> return ResolvedTheme(id, result.theme, result.findings)
            is ThemeLoader.ThemeLoadResult.Failure -> Timber.w(result.error)
        }

        // Keep a decoded, usable theme on a failed reload, not a possibly partial disk artifact.
        _activeTheme?.let { active ->
            activeConfigId?.let { return ResolvedTheme(it, active, _activeFindings) }
        }

        if (id != "trime") {
            when (val result = ThemeLoader.loadTheme("trime")) {
                is ThemeLoader.ThemeLoadResult.Success -> {
                    Timber.w("Theme '$id' is unavailable, fallback to default theme 'trime'")
                    return ResolvedTheme("trime", result.theme, result.findings)
                }

                is ThemeLoader.ThemeLoadResult.Failure -> Timber.w(result.error)
            }
        }

        var lastFailure: ThemeLoader.ThemeLoadError? = null
        for (fallbackId in getAllThemes().map { it.configId }.distinct()) {
            when (val result = ThemeLoader.loadTheme(fallbackId)) {
                is ThemeLoader.ThemeLoadResult.Success -> {
                    Timber.w("Theme '$id' is unavailable, fallback to available theme '$fallbackId'")
                    return ResolvedTheme(fallbackId, result.theme, result.findings)
                }

                is ThemeLoader.ThemeLoadResult.Failure -> lastFailure = result.error
            }
        }

        Timber.w(lastFailure, "No valid theme available")
        error("No valid theme available")
    }

    private fun applyTheme(resolvedTheme: ResolvedTheme) {
        val theme = resolvedTheme.theme
        // A structurally equal theme suppresses the change notification below, so the
        // UI tree keeps its views and their injected scope. Replace neither the
        // caches nor the scope in that case. Findings describe the loaded file,
        // however, so refresh them even when no view rebuild is needed.
        if (_activeTheme == theme) {
            activeConfigId = resolvedTheme.configId
            _activeFindings = resolvedTheme.findings
            return
        }
        KeyActionManager.resetCache()
        FontManager.resetCache(theme)
        LiquidData.init(theme)
        ColorManager.attachTheme(theme)
        _activeTheme = theme
        activeConfigId = resolvedTheme.configId
        _activeFindings = resolvedTheme.findings
        fireChange()
    }

    suspend fun init(configuration: Configuration) = withContext(Dispatchers.Main.immediate) {
        loadMutex.withLock {
            if (_activeTheme == null) {
                val resolved = withContext(Dispatchers.IO) { getThemeById(prefs.selectedTheme.getValue()) }
                try {
                    applyTheme(resolved)
                } catch (e: Exception) {
                    _activeTheme = null
                    _activeFindings = null
                    activeConfigId = null
                    throw e
                }
                prefs.selectedTheme.setValue(resolved.configId)
            }
            ColorManager.init(configuration)
        }
    }

    /**
     * Switches to theme [configId], falling back when it is unavailable.
     * Native deployment runs on RimeDispatcher, file parsing on [Dispatchers.IO],
     * and state changes and listener callbacks on the main thread.
     * @return the config id actually in effect; differs from [configId] when a fallback was used.
     */
    suspend fun selectTheme(configId: String): String = withContext(Dispatchers.Main.immediate) {
        loadMutex.withLock {
            val resolvedTheme = withContext(Dispatchers.IO) { getThemeById(configId) }
            applyTheme(resolvedTheme)
            prefs.selectedTheme.setValue(resolvedTheme.configId)
            resolvedTheme.configId
        }
    }
}
