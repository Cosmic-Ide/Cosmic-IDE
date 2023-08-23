/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.fragment

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.os.BundleCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.Maxr1998.modernpreferences.Preference
import de.Maxr1998.modernpreferences.PreferencesAdapter
import de.Maxr1998.modernpreferences.helpers.screen
import de.Maxr1998.modernpreferences.helpers.subScreen
import org.cosmicide.MainActivity
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.databinding.FragmentSettingsBinding
import org.cosmicide.fragment.settings.AboutSettings
import org.cosmicide.fragment.settings.AppearanceSettings
import org.cosmicide.fragment.settings.CompilerSettings
import org.cosmicide.fragment.settings.EditorSettings
import org.cosmicide.fragment.settings.FormatterSettings
import org.cosmicide.fragment.settings.GitSettings
import org.cosmicide.fragment.settings.PluginSettingsProvider

/**
 * Fragment for displaying settings screen.
 */
class SettingsFragment : BaseBindingFragment<FragmentSettingsBinding>() {
    private lateinit var preferencesAdapter: PreferencesAdapter
    override var isBackHandled = true

    override fun getViewBinding() = FragmentSettingsBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Preference.Config.dialogBuilderFactory = { context -> MaterialAlertDialogBuilder(context) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appearanceSettings = AppearanceSettings(requireActivity() as MainActivity)
        val editorSettings = EditorSettings(requireActivity())
        val formatterSettings = FormatterSettings(requireActivity())
        val compilerSettings = CompilerSettings(requireActivity())
        val pluginsSettings = PluginSettingsProvider(requireActivity())
        val gitSettings = GitSettings()
        val aboutSettings = AboutSettings(requireActivity())

        val screen = screen(requireContext()) {
            subScreen {
                collapseIcon = true
                title = "Appearance"
                summary = "Customize the appearance as you see fit"
                appearanceSettings.provideSettings(this)
            }
            subScreen {
                collapseIcon = true
                title = "Code editor"
                summary = "Customize editor settings"
                editorSettings.provideSettings(this)
            }
            subScreen {
                collapseIcon = true
                title = "Compiler"
                summary = "Configure compiler options and build process"
                compilerSettings.provideSettings(this)
            }
            subScreen {
                title = "Formatter"
                summary = "Adjust code formatting preferences"
                formatterSettings.provideSettings(this)
            }
            subScreen {
                title = "Plugins"
                summary = "Explore and install plugins to extend the functionality of the IDE"
                pluginsSettings.provideSettings(this)
            }
            subScreen {
                collapseIcon = true
                title = "Git"
                summary = "Configure Git integration"
                gitSettings.provideSettings(this)
            }
            subScreen {
                collapseIcon = true
                title = "About"
                summary = "Learn more about Cosmic IDE"
                aboutSettings.provideSettings(this)
            }
        }

        preferencesAdapter = PreferencesAdapter(screen)
        savedInstanceState?.let {
            preferencesAdapter.loadSavedState(
                BundleCompat.getParcelable(
                    it,
                    "adapter",
                    PreferencesAdapter.SavedState::class.java
                )!!
            )
        }

        binding.preferencesView.adapter = preferencesAdapter
        binding.toolbar.setNavigationOnClickListener {
            if (!preferencesAdapter.goBack()) {
                parentFragmentManager.popBackStack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isResumed.not()) {
                        return
                    }
                    if (!preferencesAdapter.goBack()) {
                        isEnabled = false
                        parentFragmentManager.popBackStack()
                    }
                }
            })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::preferencesAdapter.isInitialized.not()) {
            return
        }
        outState.putParcelable("adapter", preferencesAdapter.getSavedState())
    }
}