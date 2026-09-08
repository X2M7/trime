/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.osfans.trime.R
import com.osfans.trime.data.prefs.AppPrefs
import com.osfans.trime.data.sync.DataStorageMode
import com.osfans.trime.databinding.FragmentSetupBinding
import com.osfans.trime.util.serializable

class SetupFragment : Fragment() {
    private lateinit var binding: FragmentSetupBinding

    private val page: SetupPage by lazy { requireArguments().serializable("page")!! }

    private val prefs = AppPrefs.defaultInstance().profile
    private var rendering = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSetupBinding.inflate(inflater).apply {
            storageModeOptions.setOnCheckedChangeListener { _, checkedId ->
                if (rendering) return@setOnCheckedChangeListener
                val newMode = when (checkedId) {
                    R.id.sync_from_external_option -> DataStorageMode.EXTERNAL_SYNC
                    R.id.app_specific_storage_option -> DataStorageMode.APP_STORAGE
                    else -> return@setOnCheckedChangeListener
                }
                (requireActivity() as SetupActivity).changeStorageMode(newMode)
            }
            syncFromExternalDesc.setOnClickListener { syncFromExternalOption.isChecked = true }
            appSpecificStorageDesc.setOnClickListener { appSpecificStorageOption.isChecked = true }
        }
        sync()
        return binding.root
    }

    // Called on window focus changed
    fun sync() {
        if (!::binding.isInitialized || view == null) return
        val activity = requireActivity() as SetupActivity
        val done = activity.isPageDone(page)
        val isStorageModePage = page == SetupPage.Mode
        val checkedId = when (prefs.dataStorageMode.getValue()) {
            DataStorageMode.EXTERNAL_SYNC -> R.id.sync_from_external_option
            DataStorageMode.APP_STORAGE -> R.id.app_specific_storage_option
        }
        with(binding) {
            storageModeOptions.visibility = if (isStorageModePage) View.VISIBLE else View.GONE
            rendering = true
            storageModeOptions.check(checkedId)
            rendering = false
            syncFromExternalOption.isEnabled = !activity.changingStorageMode
            appSpecificStorageOption.isEnabled = !activity.changingStorageMode
            syncFromExternalDesc.isEnabled = !activity.changingStorageMode
            appSpecificStorageDesc.isEnabled = !activity.changingStorageMode

            stepText.text = page.getStepText(requireContext())
            hintText.text = page.getHintText(requireContext())
            val showActionButton = !done && page.showActionButton()
            actionButton.visibility = if (showActionButton) View.VISIBLE else View.GONE
            actionButton.isEnabled = !activity.changingStorageMode
            actionButton.text = page.getButtonText(requireContext())
            actionButton.setOnClickListener { page.getButtonAction(requireActivity()) }
            doneText.visibility = if (done) View.VISIBLE else View.GONE
            doneIcon.visibility = if (done) View.VISIBLE else View.GONE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sync()
    }
}
