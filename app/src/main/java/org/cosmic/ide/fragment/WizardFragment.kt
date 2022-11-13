package org.cosmic.ide.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.ThemeUtils
import com.google.android.material.transition.MaterialContainerTransform
import org.cosmic.ide.R
import org.cosmic.ide.databinding.FragmentWizardBinding
import org.cosmic.ide.util.addSystemWindowInsetToMargin
import org.cosmic.ide.util.addSystemWindowInsetToPadding

class WizardFragment : Fragment() {
    private lateinit var binding: FragmentWizardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWizardBinding.inflate(inflater, container, false)
        binding.fab.addSystemWindowInsetToMargin(false, false, false, true)
        binding.appBar.addSystemWindowInsetToPadding(false, true, false, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        enterTransition = MaterialContainerTransform().apply {
            startView = requireActivity().findViewById(R.id.fab)
            endView = view
            duration = 300L
            scrimColor = Color.TRANSPARENT
            containerColor = ThemeUtils.getThemeAttrColor(requireActivity(), com.google.android.material.R.attr.colorSurface)
            startContainerColor = ThemeUtils.getThemeAttrColor(requireActivity(), com.google.android.material.R.attr.colorPrimary)
            endContainerColor = ThemeUtils.getThemeAttrColor(requireActivity(), com.google.android.material.R.attr.colorSurface)
        }
    }
}