package org.cosmic.ide.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.transition.MaterialFadeThrough
import org.cosmic.ide.R
import org.cosmic.ide.databinding.FragmentSettingsBinding
import org.cosmic.ide.fragment.settings.RootSettingsFragment

class SettingsFragment : Fragment(),
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "SettingsFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        if (savedInstanceState == null) {
            val fragment = RootSettingsFragment()

            childFragmentManager.commit {
                setReorderingAllowed(true)
                replace(binding.container.id, fragment)
            }
        } else {
            savedInstanceState.let {
                binding.collapsingToolbar.title = it.getCharSequence(TAG)
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.appBar.liftOnScrollTargetViewId = androidx.preference.R.id.recycler_view
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(TAG, binding.collapsingToolbar.title)
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        val fm = childFragmentManager
        val fragment = fm.fragmentFactory.instantiate(requireActivity().classLoader, pref.fragment ?: return false)
        fragment.setTargetFragment(caller, 0)
        binding.collapsingToolbar.title = pref.title ?: requireContext().getString(R.string.settings)
        openFragment(fragment)
        return true
    }

    private fun openFragment(fragment: Fragment) {
        childFragmentManager.commit {
            setReorderingAllowed(true)
            replace(binding.container.id, fragment)
            addToBackStack(null)
        }
    }
}
