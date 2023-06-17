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
                title = "Fast implementation for Jar FS"
                summary =
                    "This experimental mode may speed up compilation time, but use with caution"
                defaultValue = false
            }

            switch(PreferenceKeys.COMPILER_USE_K2) {
                title = "K2 compiler for compilation"
                summary =
                    "This experimental mode may speed up compilation time, but use with caution"
                defaultValue = false
            }

            switch(PreferenceKeys.COMPILER_USE_SSVM) {
                title = "SSVM for running"
                summary = "This is a very experimental feature. Use with caution."
                defaultValue = false
            }

            singleChoice(PreferenceKeys.COMPILER_JAVA_VERSIONS, javaVersionItems) {
                title = "Java Version"
                summary = "Select the version of Java to use for compilation"
                initialSelection = "17"
            }

            editText(PreferenceKeys.COMPILER_JAVAC_FLAGS) {
                title = "Additional Javac flags"
                summaryProvider = { "View Javac flags" }
            }
        }
    }
}
