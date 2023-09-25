/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.fragment.settings

import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import de.Maxr1998.modernpreferences.PreferenceScreen
import de.Maxr1998.modernpreferences.helpers.multiChoice
import de.Maxr1998.modernpreferences.helpers.singleChoice
import de.Maxr1998.modernpreferences.preferences.choice.SelectionItem
import org.cosmicide.R
import org.cosmicide.util.PreferenceKeys

class FormatterSettings(private val activity: FragmentActivity) : SettingsProvider {

    private val ktfmtStyles: Array<String>
        get() = activity.resources.getStringArray(R.array.ktfmt_styles)
    private val gjfOptions: Array<String>
        get() = activity.resources.getStringArray(R.array.gjf_options)
    private val gjfStyles: Array<String>
        get() = activity.resources.getStringArray(R.array.gjf_styles)

    private val ktfmtStyleItems: List<SelectionItem>
        get() = ktfmtStyles.zip(ktfmtStyles).map { SelectionItem(it.first, it.second, null) }
    private val gjfOptionItems: List<SelectionItem>
        get() = gjfOptions.zip(gjfOptions).map { SelectionItem(it.first, it.second, null) }
    private val gjfStyleItems: List<SelectionItem>
        get() = gjfStyles.zip(gjfStyles).map { SelectionItem(it.first, it.second, null) }

    override fun provideSettings(builder: PreferenceScreen.Builder) {
        builder.apply {
            icon = ResourcesCompat.getDrawable(
                activity.resources,
                R.drawable.outline_edit_note_24,
                activity.theme
            )
            singleChoice(PreferenceKeys.FORMATTER_KTFMT_STYLE, ktfmtStyleItems) {
                title = "Kotlin code formatter styles"
                summary = "Choose a style for formatting Kotlin code"
                initialSelection = "google"
            }

            multiChoice(PreferenceKeys.FORMATTER_GJF_OPTIONS, gjfOptionItems) {
                title = "Google Java Formatter options"
                summary = "Choose options for formatting Java code"
                initialSelections = setOf("--skip-javadoc-formatting")
            }

            singleChoice(PreferenceKeys.FORMATTER_GJF_STYLE, gjfStyleItems) {
                title = "Google Java Formatter styles"
                summary = "Choose a style for formatting Java code"
                initialSelection = "aosp"
            }
        }
    }
}