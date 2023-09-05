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

import de.Maxr1998.modernpreferences.PreferenceScreen
import de.Maxr1998.modernpreferences.helpers.editText
import org.cosmicide.util.PreferenceKeys

class GitSettings : SettingsProvider {

    override fun provideSettings(builder: PreferenceScreen.Builder) {
        builder.apply {
            editText(PreferenceKeys.GIT_USERNAME) {
                title = "Username"
                defaultValue = ""
                summary = "Enter your username here. This is required to push to GitHub"
            }
            editText(PreferenceKeys.GIT_EMAIL) {
                title = "Email"
                defaultValue = ""
                summary = "Enter your email address here. This is required to push to GitHub"
            }
            editText(PreferenceKeys.GIT_API_KEY) {
                title = "Personal Access Token"
                summary = "This is required to push to GitHub"
                defaultValue = ""
            }
        }
    }
}