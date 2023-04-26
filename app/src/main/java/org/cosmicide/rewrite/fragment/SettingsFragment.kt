package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.view.View
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.databinding.FragmentSettingsBinding

/**
 * Fragment for displaying settings screen.
 */
class SettingsFragment : BaseBindingFragment<FragmentSettingsBinding>() {

    override lateinit var binding: FragmentSettingsBinding

    override fun getViewBinding(): FragmentSettingsBinding {
        binding = FragmentSettingsBinding.inflate(layoutInflater)
        return binding
    }

    override fun getView(): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

    }
}