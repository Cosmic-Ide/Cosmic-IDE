/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.darq.utils.extensions.awaitBinderReceived
import com.kieronquinn.app.darq.utils.extensions.isShizukuInstalled
import kotlinx.coroutines.launch
import org.cosmicide.rewrite.common.Prefs
import org.cosmicide.rewrite.databinding.ActivityMainBinding
import org.cosmicide.rewrite.fragment.InstallResourcesFragment
import org.cosmicide.rewrite.fragment.ProjectFragment
import org.cosmicide.rewrite.util.CommonUtils
import org.cosmicide.rewrite.util.ResourceUtil
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.OnRequestPermissionResultListener
import rikka.shizuku.ShizukuProvider

class MainActivity : AppCompatActivity() {

    var themeInt = 0
    private lateinit var binding: ActivityMainBinding
    val shizukuPermissionCode = 1

    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        val accent = Prefs.appAccent
            themeInt = CommonUtils.getAccent(accent)
            setTheme(themeInt)
        enableEdgeToEdge()
        return super.onCreateView(parent, name, context, attrs)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        binding = ActivityMainBinding.inflate(layoutInflater)

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

        Shizuku.addRequestPermissionResultListener(listener)

        if (isShizukuInstalled()) {
            lifecycleScope.launch {
                awaitBinderReceived()
                CommonUtils.showSnackBar(binding.root, "Shizuku is ready")
            }
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                requestPermission()
            } else {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    lifecycleScope.launch {
                        awaitBinderReceived()
                        CommonUtils.showSnackBar(binding.root, "Shizuku is ready")
                    }
                }
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
                        CommonUtils.showSnackBar(binding.root, "Shizuku is ready")
                    }
                }
            }
        }

    fun requestPermission() {
        if (Shizuku.isPreV11()) {
            requestPermissions(arrayOf(ShizukuProvider.PERMISSION), shizukuPermissionCode)
        } else {
            Shizuku.requestPermission(shizukuPermissionCode)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(listener)
    }
}
