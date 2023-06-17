/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment.settings

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import de.Maxr1998.modernpreferences.PreferenceScreen
import de.Maxr1998.modernpreferences.helpers.editText
import de.Maxr1998.modernpreferences.helpers.onClick
import de.Maxr1998.modernpreferences.helpers.pref
import de.Maxr1998.modernpreferences.helpers.singleChoice
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.fragment.PluginsFragment
import org.cosmicide.rewrite.fragment.PluginListFragment
import org.cosmicide.rewrite.util.PreferenceKeys

class PluginsSettings(private val activity: FragmentActivity) : SettingsProvider {

    override fun provideSettings(builder: PreferenceScreen.Builder) {
        builder.apply {
            pref(PreferenceKeys.AVAILABLE_PLUGINS) {
                title = "Available plugins"
                summary = "View available plugins"
                onClick {
                    activity.supportFragmentManager.beginTransaction().apply {
                        add(R.id.fragment_container, PluginListFragment())
                        addToBackStack(null)
                        setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    }.commit()
                    true
                }
            }

            pref(PreferenceKeys.INSTALLED_PLUGINS) {
                title = "Installed plugins"
                summary = "View installed plugins"
                onClick {
                    activity.supportFragmentManager.beginTransaction().apply {
                        add(R.id.fragment_container, PluginsFragment())
                        addToBackStack(null)
                        setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    }.commit()
                    true
                }
            }

            editText(PreferenceKeys.PLUGIN_REPOSITORY) {
                title = "Repository"
                summary = "Add a custom plugin repository"
            }
        }
    }
}