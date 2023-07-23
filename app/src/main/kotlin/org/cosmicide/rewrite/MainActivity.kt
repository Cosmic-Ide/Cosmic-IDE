/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.commit
import org.cosmicide.rewrite.common.Prefs
import org.cosmicide.rewrite.databinding.ActivityMainBinding
import org.cosmicide.rewrite.fragment.InstallResourcesFragment
import org.cosmicide.rewrite.fragment.ProjectFragment
import org.cosmicide.rewrite.fragment.SettingsFragment
import org.cosmicide.rewrite.util.ResourceUtil

class MainActivity : AppCompatActivity() {

    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        setTheme(Prefs.appAccent.toInt())
        return super.onCreateView(parent, name, context, attrs)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val binding = ActivityMainBinding.inflate(layoutInflater)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val imeInset =
                ViewCompat.getRootWindowInsets(view)!!.getInsets(WindowInsetsCompat.Type.ime())

            val systemBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = systemBarInsets.left
                topMargin = systemBarInsets.top
                rightMargin = systemBarInsets.right
                bottomMargin = if (imeInset.bottom > 0) imeInset.bottom else systemBarInsets.bottom
            }

            WindowInsetsCompat.CONSUMED
        }

        setContentView(binding.root)
        if (ResourceUtil.missingResources().isNotEmpty()) {
            supportFragmentManager.commit {
                replace(binding.fragmentContainer.id, InstallResourcesFragment())
            }
        } else {
            supportFragmentManager.commit {
                replace(binding.fragmentContainer.id, ProjectFragment())
            }
        }
    }

    @Deprecated(
        "Only applies to API 29 and below",
        replaceWith = ReplaceWith("OnBackPressedDispatcher.addCallback(owner, callback)")
    )
    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is SettingsFragment) {
            fragment.onBackPressed()
            return
        }
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else super.onBackPressed()
    }
}
