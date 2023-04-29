package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.FragmentTransaction
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import org.cosmicide.rewrite.BuildConfig
import org.cosmicide.rewrite.R

/**
 * A [PreferenceFragmentCompat] subclass to display the preferences UI.
 */
class PreferencesFragment : PreferenceFragmentCompat() {

    companion object {
        const val KEY_VERSION = "version"
        const val KEY_PLUGINS = "plugins"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        findPreference<Preference>(KEY_VERSION)?.apply {
            summary = BuildConfig.VERSION_NAME
        }

        findPreference<Preference>(KEY_PLUGINS)?.let { pluginsPreference ->
            pluginsPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    add(R.id.fragment_container, PluginsFragment())
                    addToBackStack(null)
                    setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                }.commit()

                true
            }
        }
    }

    override fun onCreateRecyclerView(inflater: LayoutInflater, parent: ViewGroup, savedInstanceState: Bundle?): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        recyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.preference_layout_fall_down)
        return recyclerView
    }
}