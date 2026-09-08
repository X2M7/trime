/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ui.setup

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.osfans.trime.R
import com.osfans.trime.core.RimeMaintenanceMutex
import com.osfans.trime.data.prefs.AppPrefs
import com.osfans.trime.data.sync.DataStorageMode
import com.osfans.trime.data.sync.RimeDataSync
import com.osfans.trime.databinding.ActivitySetupBinding
import com.osfans.trime.ui.main.MainActivity
import com.osfans.trime.ui.setup.SetupPage.Companion.isLastPage
import com.osfans.trime.util.appContext
import com.osfans.trime.util.createNotificationChannel
import com.osfans.trime.util.startActivity
import com.osfans.trime.util.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.systemservices.notificationManager
import timber.log.Timber

class SetupActivity : FragmentActivity() {
    private lateinit var viewPager: ViewPager2

    private lateinit var skipButton: Button
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button

    private var completedPages = emptySet<SetupPage>()
    private var refreshJob: Job? = null
    private var refreshPending = false
    private var storageRevision = 0
    private var selectInitialPage = false
    var changingStorageMode = false
        private set

    fun isPageDone(page: SetupPage) = page in completedPages

    private val dataPathPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null || changingStorageMode) return@registerForActivityResult
            changingStorageMode = true
            storageRevision++
            renderStatus()
            lifecycleScope.launch {
                try {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            RimeMaintenanceMutex.withLock {
                                try {
                                    RimeDataSync.persistTreeUri(this@SetupActivity, uri)
                                    RimeDataSync.importToLocal(this@SetupActivity).getOrThrow()
                                } catch (failure: Exception) {
                                    if (failure !is CancellationException && RimeDataSync.treeUri() == uri) {
                                        RimeDataSync.clearExternalTree(this@SetupActivity)
                                    }
                                    throw failure
                                }
                            }
                        }
                        storageRevision++
                        refreshCurrentFragment()
                        toast(R.string.setup__data_path_imported)
                    }.onFailure {
                        if (it is CancellationException) throw it
                        storageRevision++
                        refreshCurrentFragment()
                        toast(R.string.setup__data_path_import_failed)
                    }
                } finally {
                    changingStorageMode = false
                    storageRevision++
                    refreshCurrentFragment()
                }
            }
        }

    fun launchDataPathPicker() {
        if (!changingStorageMode) dataPathPicker.launch(null as Uri?)
    }

    fun refreshCurrentFragment() {
        refreshPending = true
        if (refreshJob?.isActive == true) return
        refreshJob = lifecycleScope.launch {
            while (refreshPending) {
                refreshPending = false
                val revision = storageRevision
                val pages = withContext(Dispatchers.IO) {
                    SetupPage.entries.filter {
                        runCatching { it.isDone() }
                            .onFailure { error -> Timber.w(error, "Unable to read setup status") }
                            .getOrDefault(false)
                    }.toSet()
                }
                // Repeated refresh requests must not starve the UI; only changed storage invalidates a result.
                if (revision != storageRevision) {
                    refreshPending = true
                    continue
                }
                completedPages = pages
                if (selectInitialPage) {
                    selectInitialPage = false
                    SetupPage.entries.firstOrNull { it !in pages }?.let {
                        viewPager.setCurrentItem(it.ordinal, false)
                    }
                }
                renderStatus()
            }
        }
    }

    private fun renderStatus() {
        supportFragmentManager.fragments.forEach { (it as? SetupFragment)?.sync() }
        updateButtons()
    }

    fun changeStorageMode(mode: DataStorageMode) {
        val prefs = AppPrefs.defaultInstance().profile
        if (changingStorageMode || prefs.dataStorageMode.getValue() == mode) return
        changingStorageMode = true
        storageRevision++
        completedPages = completedPages - SetupPage.Mode
        renderStatus()
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RimeMaintenanceMutex.withLock {
                        if (prefs.dataStorageMode.getValue() == DataStorageMode.EXTERNAL_SYNC &&
                            mode == DataStorageMode.APP_STORAGE
                        ) {
                            prefs.userDbMigrated.setValue(false)
                            RimeDataSync.clearExternalTree(this@SetupActivity)
                        }
                        prefs.dataStorageMode.setValue(mode)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Unable to change storage mode")
                toast(R.string.setup__data_path_import_failed)
            } finally {
                storageRevision++
                changingStorageMode = false
                refreshCurrentFragment()
            }
        }
    }

    private fun completeSetup() {
        startActivity<MainActivity>()
        finish()
    }

    companion object {
        private var shown = false
        private const val CHANNEL_ID = "setup"
        private const val NOTIFY_ID = 87463

        suspend fun shouldShowUp(): Boolean {
            if (shown) return false
            val incomplete = withContext(Dispatchers.IO) {
                runCatching { SetupPage.hasUndonePage() }
                    .onFailure { Timber.w(it, "Unable to read initial setup status") }
                    .getOrDefault(true)
            }
            return !shown && incomplete
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivitySetupBinding.inflate(layoutInflater)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val sysBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(
                sysBars.left,
                sysBars.top,
                sysBars.right,
                sysBars.bottom,
            )
            windowInsets
        }
        setContentView(binding.root)
        skipButton = binding.skipButton.apply {
            text = getString(R.string.setup__skip)
            setOnClickListener {
                AlertDialog
                    .Builder(this@SetupActivity)
                    .setMessage(R.string.setup__skip_hint)
                    .setPositiveButton(R.string.setup__skip_hint_yes) { _, _ ->
                        completeSetup()
                    }.setNegativeButton(R.string.setup__skip_hint_no, null)
                    .show()
            }
        }
        prevButton =
            binding.prevButton.apply {
                text = getString(R.string.setup__prev)
                setOnClickListener { viewPager.currentItem -= 1 }
            }
        nextButton =
            binding.nextButton.apply {
                setOnClickListener {
                    if (viewPager.currentItem != SetupPage.entries.size - 1) {
                        viewPager.currentItem += 1
                    } else {
                        completeSetup()
                    }
                }
            }
        viewPager = binding.viewpager
        viewPager.adapter = Adapter()
        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) = updateButtons()
            },
        )
        selectInitialPage = savedInstanceState == null
        updateButtons()
        refreshCurrentFragment()
        shown = true
        createNotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.setup_channel),
        )
    }

    fun updateButtons() {
        val allDone = completedPages.size == SetupPage.entries.size
        val modeSetupDone = !changingStorageMode && isPageDone(SetupPage.Mode)
        val isFirstPage = viewPager.currentItem == 0
        val isLastPage = viewPager.currentItem.isLastPage()

        viewPager.isUserInputEnabled = modeSetupDone

        prevButton.isGone = isFirstPage
        skipButton.isGone = !modeSetupDone || allDone
        nextButton.text = getString(if (isLastPage) R.string.done else R.string.setup__next)
        nextButton.isGone = isLastPage && !allDone
        nextButton.isEnabled = modeSetupDone
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        refreshCurrentFragment()
    }

    override fun onPause() {
        if (completedPages.size != SetupPage.entries.size) {
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_trime_status)
                .setContentTitle(getText(R.string.trime_app_name))
                .setContentText(getText(R.string.setup__notify_hint))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, javaClass),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).setAutoCancel(true)
                .build()
                .let { notificationManager.notify(NOTIFY_ID, it) }
        }
        super.onPause()
    }

    override fun onResume() {
        notificationManager.cancel(NOTIFY_ID)
        super.onResume()
    }

    private inner class Adapter : FragmentStateAdapter(this) {
        override fun getItemCount(): Int = SetupPage.entries.size

        override fun createFragment(position: Int): Fragment = SetupFragment().apply {
            arguments = bundleOf("page" to SetupPage.entries[position])
        }
    }
}
