/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.DynamicColors
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import kotlinx.coroutines.launch
import org.cosmicide.common.Prefs
import org.cosmicide.databinding.ActivityMainBinding
import org.cosmicide.fragment.InstallResourcesFragment
import org.cosmicide.fragment.ProjectFragment
import org.cosmicide.util.CommonUtils
import org.cosmicide.util.MaterialEditorTheme
import org.cosmicide.util.ResourceUtil
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
        loadEditorThemes()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val imeInset =
                ViewCompat.getRootWindowInsets(view)!!.getInsets(WindowInsetsCompat.Type.ime())

            val systemBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                systemBarInsets.left,
                systemBarInsets.top,
                systemBarInsets.right,
                if (imeInset.bottom > 0) 0 else systemBarInsets.bottom
            )

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

    private fun loadEditorThemes() {
        val themeRegistry = ThemeRegistry.getInstance()
        themeRegistry.loadTheme(loadTheme("darcula.json", "darcula"))
        themeRegistry.loadTheme(loadTheme("QuietLight.tmTheme.json", "QuietLight"))

        App.instance.get()!!.applyThemeBasedOnConfiguration()
    }


    private fun loadTheme(fileName: String, themeName: String): ThemeModel {
        val inputStream =
            MaterialEditorTheme.resolveTheme(this, fileName)
        val source = IThemeSource.fromInputStream(inputStream, fileName, null)
        return ThemeModel(source, themeName)
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(listener)
    }
}
