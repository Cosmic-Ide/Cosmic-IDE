/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import kotlinx.coroutines.launch
import org.cosmicide.databinding.ActivityMainBinding
import org.cosmicide.ui.IDENavigation
import org.cosmicide.ui.editor.resolveTheme
import org.cosmicide.ui.theme.IDETheme
import org.cosmicide.util.CommonUtils
import org.cosmicide.util.MaterialEditorTheme
import org.cosmicide.util.awaitBinderReceived
import org.cosmicide.util.isShizukuInstalled
import org.eclipse.tm4e.core.registry.IThemeSource
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.OnRequestPermissionResultListener
import rikka.shizuku.ShizukuProvider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val shizukuPermissionCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        enableEdgeToEdge()

        System.loadLibrary("android-tree-sitter")

//        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
//            val imeInset =
//                ViewCompat.getRootWindowInsets(view)!!.getInsets(WindowInsetsCompat.Type.ime())
//
//            val systemBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
//
//            view.setPadding(
//                systemBarInsets.left,
//                systemBarInsets.top,
//                systemBarInsets.right,
//                if (imeInset.bottom > 0) 0 else systemBarInsets.bottom
//            )
//
//            WindowInsetsCompat.CONSUMED
//        }

        //setContentView(binding.root)

        setContent {
            IDETheme {
                loadEditorThemes(MaterialTheme.colorScheme)

                IDENavigation()
            }
        }

//        if (ResourceUtil.missingResources().isNotEmpty()) {
//            supportFragmentManager.commit {
//                replace(binding.fragmentContainer.id, InstallResourcesFragment())
//            }
//        } else {
//            supportFragmentManager.commit {
//                replace(binding.fragmentContainer.id, ProjectFragment())
//            }
//        }

        Shizuku.addRequestPermissionResultListener(listener)

        lifecycleScope.launch {
            awaitBinderReceived()
            if (isShizukuInstalled() && Shizuku.shouldShowRequestPermissionRationale()) {
                requestPermission()
            }
        }
    }

    private val listener =
        OnRequestPermissionResultListener { _, grantResult ->
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            // Do stuff based on the result and the request code
            if (granted) {
                CommonUtils.showSnackBar(binding.root, "Permission Granted")
            } else {
                CommonUtils.showSnackBar(binding.root, "Permission Denied")
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    lifecycleScope.launch {
                        awaitBinderReceived()
                    }
                }
            }
        }

    private fun requestPermission() {
        if (Shizuku.isPreV11()) {
            requestPermissions(arrayOf(ShizukuProvider.PERMISSION), shizukuPermissionCode)
        } else {
            Shizuku.requestPermission(shizukuPermissionCode)
        }
    }

    private fun loadEditorThemes(colorScheme: ColorScheme) {
        val themes = arrayOf("darcula.json", "QuietLight.tmTheme.json")
        val themeRegistry = ThemeRegistry.getInstance()

        themes.forEach { name ->
            themeRegistry.loadTheme(
                ThemeModel(
                    IThemeSource.fromInputStream(
                        resolveTheme(this, colorScheme, name), name, null
                    ), name.substringBefore('.')
                ).apply {
                    isDark = name.substringBefore('.') == "darcula"
                }
            )
        }

        applyThemeBasedOnConfiguration()
    }

    fun Context.applyThemeBasedOnConfiguration() {
        val themeName =
            when (AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.MODE_NIGHT_YES -> "darcula"
                AppCompatDelegate.MODE_NIGHT_NO -> "light"
                else -> {
                    when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                        Configuration.UI_MODE_NIGHT_YES -> "darcula"
                        else -> "light"
                    }
                }
            }
        ThemeRegistry.getInstance().setTheme(themeName)
    }


    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(listener)
    }
}
