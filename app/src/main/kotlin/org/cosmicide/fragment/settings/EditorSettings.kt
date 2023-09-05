/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

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
import de.Maxr1998.modernpreferences.helpers.editText
import de.Maxr1998.modernpreferences.helpers.onCheckedChange
import de.Maxr1998.modernpreferences.helpers.seekBar
import de.Maxr1998.modernpreferences.helpers.switch
import org.cosmicide.R
import org.cosmicide.rewrite.common.Analytics
import org.cosmicide.util.PreferenceKeys

class EditorSettings(private val activity: FragmentActivity) : SettingsProvider {

    override fun provideSettings(builder: PreferenceScreen.Builder) {
        builder.apply {
            icon = ResourcesCompat.getDrawable(
                activity.resources,
                R.drawable.baseline_mode_edit_24,
                activity.theme
            )
            seekBar(PreferenceKeys.EDITOR_FONT_SIZE) {
                title = "Font size"
                summary = "Set the font size for the editor"
                max = 32
                min = 12
                showTickMarks = true
            }

            seekBar(PreferenceKeys.EDITOR_TAB_SIZE) {
                title = "Tab size"
                summary = "Set the tab size for the editor"
                max = 14
                min = 2
                default = 4
                showTickMarks = true
            }

            switch(PreferenceKeys.EDITOR_EXP_JAVA_COMPLETION) {
                title = "Experimental Java code completion"
                summary = "Uses an experimental Java Completion Engine"
                defaultValue = false

                onCheckedChange { isChecked ->
                    Analytics.logEvent("experimental_java_completion", isChecked)
                    true
                }
            }

            switch(PreferenceKeys.KOTLIN_REALTIME_ERRORS) {
                title = "Enable Kotlin real-time errors"
                summary =
                    "Enables real-time error checking for Kotlin files. This is a slow process and may cause lag. Recommended to turn off on complex projects."
                defaultValue = false

                onCheckedChange { isChecked ->
                    Analytics.logEvent("kotlin_realtime_errors", isChecked)
                    true
                }
            }

            editText(PreferenceKeys.EDITOR_FONT) {
                title = "Editor font"
                summary = "Enter the font path for editor"
                defaultValue = ""
            }

            switch(PreferenceKeys.STICKY_SCROLL) {
                title = "Sticky scroll"
                summary = "Enables sticky scroll in the editor"
                defaultValue = true

                onCheckedChange { isChecked ->
                    Analytics.logEvent("sticky_scroll", isChecked)
                    true
                }
            }

            switch(PreferenceKeys.EDITOR_USE_SPACES) {
                title = "Use spaces instead of tabs"
                summary = "Choose whether to use spaces instead of tab character"
                defaultValue = true
            }

            switch(PreferenceKeys.EDITOR_LIGATURES_ENABLE) {
                title = "Font ligatures"
                summary = "Enable & disable font ligatures"
                defaultValue = false
            }

            switch(PreferenceKeys.EDITOR_WORDWRAP_ENABLE) {
                title = "Word wrap"
                summary = "Enable & disable word wrap"
                defaultValue = false
            }

            switch(PreferenceKeys.BRACKET_PAIR_AUTOCOMPLETE) {
                title = "Bracket pair auto-completion"
                summary = "Enable & disable bracket pair auto-completion"
                defaultValue = true
            }

            switch(PreferenceKeys.EDITOR_SCROLLBAR_SHOW) {
                title = "Scrollbar"
                summary = "If enabled, shows scrollbar in the editor"
                defaultValue = true
            }

            switch(PreferenceKeys.QUICK_DELETE) {
                title = "Fast delete blank lines"
                summary = "If enabled, blank lines are deleted quickly in the editor"
                defaultValue = true
            }

            switch(PreferenceKeys.EDITOR_HW_ENABLE) {
                title = "Hardware acceleration"
                summary =
                    "Enabling this may result in increased memory usage, but will speed up editor rendering"
                defaultValue = true
            }

            switch(PreferenceKeys.EDITOR_NON_PRINTABLE_SYMBOLS_SHOW) {
                title = "Non-printable characters"
                summary = "If enabled, shows non-printable symbols in the editor"
                defaultValue = false
            }

            switch(PreferenceKeys.EDITOR_LINE_NUMBERS_SHOW) {
                title = "Line numbers"
                summary = "If enabled, shows editor line numbers"
                defaultValue = false
            }

            switch(PreferenceKeys.EDITOR_DOUBLE_CLICK_CLOSE) {
                title = "Double click to close"
                summary = "If enabled, double clicking on an opened tab will close it"
                defaultValue = false
            }
        }
    }
}
