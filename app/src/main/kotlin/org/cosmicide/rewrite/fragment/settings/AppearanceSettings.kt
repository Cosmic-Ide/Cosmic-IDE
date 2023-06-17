/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment.settings

import androidx.fragment.app.FragmentActivity
import de.Maxr1998.modernpreferences.PreferenceScreen
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

    override fun provideSettings(builder: PreferenceScreen.Builder) {
        builder.apply {
            singleChoice(PreferenceKeys.APP_THEME, themeItems) {
                initialSelection = "auto"
                title = "App theme"
            }
        }
    }
}