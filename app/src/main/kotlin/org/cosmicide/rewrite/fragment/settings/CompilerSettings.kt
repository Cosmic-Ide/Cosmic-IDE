/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment.settings

import androidx.fragment.app.FragmentActivity
import de.Maxr1998.modernpreferences.PreferenceScreen
import de.Maxr1998.modernpreferences.helpers.editText
import de.Maxr1998.modernpreferences.helpers.singleChoice
import de.Maxr1998.modernpreferences.helpers.switch
import de.Maxr1998.modernpreferences.preferences.choice.SelectionItem
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.util.PreferenceKeys

class CompilerSettings(private val activity: FragmentActivity) : SettingsProvider {

    private val javaVersionValues: Array<String>
        get() = activity.resources.getStringArray(R.array.java_version_entries)
    private val javaVersionItems: List<SelectionItem>
        get() = javaVersionValues.zip(javaVersionValues).map { SelectionItem(it.first, it.second, null) }

    override fun provideSettings(builder: PreferenceScreen.Builder) {
        builder.apply {
            switch(PreferenceKeys.COMPILER_USE_FJFS) {
                title = activity.getString(R.string.fast_jar_fs)
                summary =
                    activity.getString(R.string.experimental_caution)
                defaultValue = false
            }

            switch(PreferenceKeys.COMPILER_USE_K2) {
                title = activity.getString(R.string.k2_compiler)
                summary = activity.getString(R.string.experimental_caution)
                defaultValue = false
            }

            switch(PreferenceKeys.COMPILER_USE_SSVM) {
                title = activity.getString(R.string.ssvm)
                summary = activity.getString(R.string.experimental_caution)
                defaultValue = false
            }

            singleChoice(PreferenceKeys.COMPILER_JAVA_VERSIONS, javaVersionItems) {
                title = activity.getString(R.string.java_version)
                summary = activity.getString(R.string.java_version_desc)
                initialSelection = "17"
            }

            editText(PreferenceKeys.COMPILER_JAVAC_FLAGS) {
                title = activity.getString(R.string.additional_javac_flags)
                summaryProvider = { activity.getString(R.string.additional_javac_flags_desc) }
            }
        }
    }
}
