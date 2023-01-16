package org.cosmic.ide

import android.Manifest.permission
import android.content.pm.PackageManager
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import org.cosmic.ide.databinding.ActivityMainBinding
import org.cosmic.ide.ui.preference.Settings as AppSettings
import org.cosmic.ide.util.AndroidUtilities
import org.cosmic.ide.util.addSystemWindowInsetToPadding

class MainActivity : AppCompatActivity() {
    private val isStoragePermissionsGranted: Boolean
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                (ContextCompat.checkSelfPermission(this, permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED)
            }

    private val settings = AppSettings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)

        setupTheme()
        setContentView(binding.root)
        setupEdgeToEdge(binding.root)

        if (!isStoragePermissionsGranted) {
            requestStorage()
            return
        }
    }

    private fun setupTheme() {
        AppCompatDelegate.setDefaultNightMode(settings.theme)
    }

    private fun setupEdgeToEdge(contentView: View) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        contentView.addSystemWindowInsetToPadding(left = true, right = true)
    }

    private fun requestStorage() {
        if (isStoragePermissionsGranted) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")

            try {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        uri
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                AndroidUtilities.showToast("Cannot ask for permission. Please grant the permission manually.")
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission.WRITE_EXTERNAL_STORAGE, permission.READ_EXTERNAL_STORAGE),
                1000
            )
        }
    }

    private fun onStorageDenied() {
        AndroidUtilities.showSimpleAlert(
            this,
            getString(R.string.permission_issue),
            getString(R.string.grant_permission_message),
            getString(R.string.grant),
            getString(R.string.dialog_close)
        ) { _, which ->
            if (which != DialogInterface.BUTTON_POSITIVE) {
                finishAffinity()
            }
            requestStorage()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                onStorageDenied()
            }
        }
    }
}