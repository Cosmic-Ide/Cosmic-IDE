/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment.settings

import android.app.UiModeManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import de.Maxr1998.modernpreferences.PreferenceScreen
import de.Maxr1998.modernpreferences.helpers.defaultOnSelectionChange
import de.Maxr1998.modernpreferences.helpers.singleChoice
import de.Maxr1998.modernpreferences.preferences.choice.SelectionItem
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.util.PreferenceKeys

class AppearanceSettings(private val activity: FragmentActivity) : SettingsProvider {

    private val themeValues: Array<String>
        get() = activity.resources.getStringArray(R.array.app_theme_entry_values)
    private val themeOptions: Array<String>
        get() = activity.resources.getStringArray(R.array.app_theme_entries)
    private val themeItems: List<SelectionItem>
        get() = themeValues.zip(themeOptions).map { SelectionItem(it.first, it.second, null) }

    private val accentItems = listOf(
        SelectionItem(R.style.Theme_CosmicIde.toString(), "default", null),
        SelectionItem(R.style.Theme_CosmicIde_Pyro.toString(), "pyro", null),
        SelectionItem(R.style.Theme_CosmicIde_Indigo.toString(), "indigo", null),
        SelectionItem(R.style.Theme_CosmicIde_Flamingo.toString(), "flamingo", null),
        SelectionItem(R.style.Theme_CosmicIde_Mint.toString(), "mint", null),
        SelectionItem(R.style.Theme_CosmicIde_Emerald.toString(), "emerald", null),
    )

    override fun provideSettings(builder: PreferenceScreen.Builder) {
        builder.apply {
            icon = ResourcesCompat.getDrawable(
                activity.resources,
                R.drawable.baseline_color_lens_24,
                activity.theme
            )
            singleChoice(PreferenceKeys.APP_THEME, themeItems) {
                initialSelection = activity.resources.getStringArray(R.array.app_theme_entry_values)
                    .first() // auto
                title = activity.getString(R.string.app_theme)
                icon = ResourcesCompat.getDrawable(
                    activity.resources,
                    R.drawable.outline_dark_mode_24,
                    activity.theme
                )
                defaultOnSelectionChange { newValue ->
                    val theme = getTheme(newValue)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        getSystemService(
                            activity,
                            UiModeManager::class.java
                        )?.setApplicationNightMode(theme)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(if (theme == UiModeManager.MODE_NIGHT_AUTO) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else theme)
                    }
                }
            }

            singleChoice(PreferenceKeys.APP_ACCENT, accentItems) {
                initialSelection = "default"
                title = "Choose Accent Color"
                defaultOnSelectionChange {
                    activity.recreate()
                }
            }
        }
    }

    private fun getTheme(value: String): Int {
        return when (value) {
            "light" -> UiModeManager.MODE_NIGHT_NO
            "dark" -> UiModeManager.MODE_NIGHT_YES
            else -> UiModeManager.MODE_NIGHT_AUTO
        }
    }
}