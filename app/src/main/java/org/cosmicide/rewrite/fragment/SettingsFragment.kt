package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.databinding.FragmentSettingsBinding

/**
 * Fragment for displaying settings screen.
 */
class SettingsFragment : BaseBindingFragment<FragmentSettingsBinding>() {

    override fun getViewBinding() = FragmentSettingsBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            parentFragmentManager.popBackStack()
        }
    }
}