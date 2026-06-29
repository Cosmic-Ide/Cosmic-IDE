package org.cosmicide.ui.settings

import android.app.UiModeManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat.getSystemService
import androidx.preference.PreferenceManager
import org.cosmicide.R
import org.cosmicide.ui.settings.components.SingleChoicePreference
import org.cosmicide.util.PreferenceKeys

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    
    var theme by remember { mutableStateOf(prefs.getString(PreferenceKeys.APP_THEME, "auto") ?: "auto") }

    val themeOptions = listOf(
        "auto" to stringResource(R.string.pref_theme_auto),
        "light" to stringResource(R.string.pref_theme_light),
        "dark" to stringResource(R.string.pref_theme_dark)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
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
            SingleChoicePreference(
                title = stringResource(R.string.app_theme),
                selectedItem = theme,
                items = themeOptions,
                onItemSelected = { newValue ->
                    theme = newValue
                    prefs.edit().putString(PreferenceKeys.APP_THEME, newValue).apply()
                    
                    val uiMode = when (newValue) {
                        "light" -> UiModeManager.MODE_NIGHT_NO
                        "dark" -> UiModeManager.MODE_NIGHT_YES
                        else -> UiModeManager.MODE_NIGHT_AUTO
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        getSystemService(context, UiModeManager::class.java)?.setApplicationNightMode(uiMode)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(
                            if (uiMode == UiModeManager.MODE_NIGHT_AUTO) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM 
                            else uiMode
                        )
                    }
                }
            )
        }
    }
}
