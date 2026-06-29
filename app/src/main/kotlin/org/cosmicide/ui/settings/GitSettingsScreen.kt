package org.cosmicide.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceManager
import org.cosmicide.ui.settings.components.EditTextPreference
import org.cosmicide.util.PreferenceKeys

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var username by remember { mutableStateOf(prefs.getString(PreferenceKeys.GIT_USERNAME, "") ?: "") }
    var email by remember { mutableStateOf(prefs.getString(PreferenceKeys.GIT_EMAIL, "") ?: "") }
    var apiKey by remember { mutableStateOf(prefs.getString(PreferenceKeys.GIT_API_KEY, "") ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Git") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            EditTextPreference(
                title = "Username",
                summary = "Enter your username here. This is required to push to GitHub",
                value = username,
                onValueChange = {
                    username = it
                    prefs.edit().putString(PreferenceKeys.GIT_USERNAME, it).apply()
                }
            )

            EditTextPreference(
                title = "Email",
                summary = "Enter your email address here. This is required to push to GitHub",
                value = email,
                onValueChange = {
                    email = it
                    prefs.edit().putString(PreferenceKeys.GIT_EMAIL, it).apply()
                }
            )

            EditTextPreference(
                title = "Personal Access Token",
                summary = "This is required to push to GitHub",
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    prefs.edit().putString(PreferenceKeys.GIT_API_KEY, it).apply()
                }
            )
        }
    }
}
