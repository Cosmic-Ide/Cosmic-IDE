package org.cosmicide.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.core.net.toUri
import org.cosmicide.BuildConfig
import org.cosmicide.ui.settings.components.PreferenceItem
import org.cosmicide.ui.settings.components.SwitchPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen(
    gotoResourceScreen: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(
            context.packageName + "_preferences",
            Context.MODE_PRIVATE
        )
    }

    var analyticsEnabled by remember {
        mutableStateOf(
            prefs.getBoolean(
                "analytics_preference",
                true
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("About") }, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            })
        }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            PreferenceItem(
                title = "About",
                summary = "A free and open-source IDE for Android. It is licensed under the GNU General Public License v3.0."
            )

            PreferenceItem(
                title = "Donate",
                summary = "Donate to the developers. This will help us to keep the project alive. Thank you for your support!",
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW, "https://pranavpurwar.github.io/donate.html".toUri()
                        )
                    )
                })

            PreferenceItem(
                title = "App version", summary = BuildConfig.VERSION_NAME + if (BuildConfig.DEBUG) {
                    " (${BuildConfig.GIT_COMMIT})"
                } else {
                    ""
                }, onClick = {
                    // Logic for developer mode could go here
                })

            PreferenceItem(
                title = "Setup",
                summary = "Go to setup screen",
                onClick = { gotoResourceScreen() })

            PreferenceItem(
                title = "Source code", onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW, "https://github.com/Cosmic-IDE/Cosmic-IDE".toUri()
                        )
                    )
                })

            PreferenceItem(
                title = "Manage storage permission", onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    ("package:" + context.packageName).toUri()
                                )
                            )
                        } catch (_: Exception) {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                        }
                    } else {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                ("package:" + context.packageName).toUri()
                            )
                        )
                    }
                })

            SwitchPreference(
                title = "Analytics",
                summary = "Help us improve the app by sending anonymous usage data",
                checked = analyticsEnabled,
                onCheckedChange = {
                    analyticsEnabled = it
                    prefs.edit { putBoolean("analytics_preference", it) }
                })
        }
    }
}
