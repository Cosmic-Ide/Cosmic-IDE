/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.view.View
import androidx.core.os.BundleCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.Maxr1998.modernpreferences.Preference
import de.Maxr1998.modernpreferences.PreferencesAdapter
import de.Maxr1998.modernpreferences.helpers.screen
import de.Maxr1998.modernpreferences.helpers.subScreen
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.databinding.FragmentSettingsBinding
import org.cosmicide.rewrite.fragment.settings.*

/**
 * Fragment for displaying settings screen.
 */
class SettingsFragment : BaseBindingFragment<FragmentSettingsBinding>() {

    override fun getViewBinding() = FragmentSettingsBinding.inflate(layoutInflater)

    private val appearanceSettings = AppearanceSettings(requireActivity())
    private val editorSettings = EditorSettings()
    private val formatterSettings = FormatterSettings(requireActivity())
    private val compilerSettings = CompilerSettings(requireActivity())
    private val pluginsSettings = PluginsSettings(requireActivity())

    private val preferencesAdapter: PreferencesAdapter
        get() = binding.preferencesView.adapter as PreferencesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Preference.Config.dialogBuilderFactory = { context -> MaterialAlertDialogBuilder(context) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Warning!")
            .setMessage("We have recently updated the API for settings, and as a result, some functions may not work as expected. We apologize for any inconvenience this may cause. Please note that we are working on a fix, which will be available soon.")
            .setPositiveButton(android.R.string.ok, null)
            .show()

        val screen = screen(requireContext()) {
            subScreen {
                collapseIcon = true
                title = "Appearance"
                summary = "Customize the appearance of the IDE"
                appearanceSettings.provideSettings(this)
            }
            subScreen {
                collapseIcon = true
                title = "Code editor"
                summary = "Customize the code editor as you see fit"
                editorSettings.provideSettings(this)
            }
            subScreen {
                collapseIcon = true
                title = "Compiler"
                summary = "Customize compilers as you see fit"
                compilerSettings.provideSettings(this)
            }
            subScreen {
                collapseIcon = true
                title = "Formatter"
                summary = "Customize code formatting as you see fit"
                formatterSettings.provideSettings(this)
            }
            subScreen {
                collapseIcon = true
                title = "Plugins"
                summary = "Lots of plugins are waiting for you"
                pluginsSettings.provideSettings(this)
            }
        }

        val adapter = PreferencesAdapter(screen)
        if (savedInstanceState != null) {
            BundleCompat.getParcelable(
                savedInstanceState,
                "adapter",
                PreferencesAdapter.SavedState::class.java
            )?.let(adapter::loadSavedState)
        }

        binding.preferencesView.adapter = adapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("adapter", preferencesAdapter.getSavedState())
    }
}
