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
import androidx.compose.ui.res.stringArrayResource
import androidx.preference.PreferenceManager
import org.cosmicide.R
import org.cosmicide.ui.settings.components.MultiChoicePreference
import org.cosmicide.ui.settings.components.SingleChoicePreference
import org.cosmicide.util.PreferenceKeys

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatterSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var ktfmtStyle by remember { mutableStateOf(prefs.getString(PreferenceKeys.FORMATTER_KTFMT_STYLE, "google") ?: "google") }
    var gjfOptions by remember { mutableStateOf(prefs.getStringSet(PreferenceKeys.FORMATTER_GJF_OPTIONS, setOf("--skip-javadoc-formatting")) ?: setOf()) }
    var gjfStyle by remember { mutableStateOf(prefs.getString(PreferenceKeys.FORMATTER_GJF_STYLE, "aosp") ?: "aosp") }

    val ktfmtStyleValues = stringArrayResource(R.array.ktfmt_styles)
    val gjfOptionValues = stringArrayResource(R.array.gjf_options)
    val gjfStyleValues = stringArrayResource(R.array.gjf_styles)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Formatter") },
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
                title = "Kotlin code formatter styles",
                summary = "Choose a style for formatting Kotlin code",
                selectedItem = ktfmtStyle,
                items = ktfmtStyleValues.map { it to it },
                onItemSelected = {
                    ktfmtStyle = it
                    prefs.edit().putString(PreferenceKeys.FORMATTER_KTFMT_STYLE, it).apply()
                }
            )

            MultiChoicePreference(
                title = "Google Java Formatter options",
                summary = "Choose options for formatting Java code",
                selectedItems = gjfOptions,
                items = gjfOptionValues.map { it to it },
                onItemsSelected = {
                    gjfOptions = it
                    prefs.edit().putStringSet(PreferenceKeys.FORMATTER_GJF_OPTIONS, it).apply()
                }
            )

            SingleChoicePreference(
                title = "Google Java Formatter styles",
                summary = "Choose a style for formatting Java code",
                selectedItem = gjfStyle,
                items = gjfStyleValues.map { it to it },
                onItemSelected = {
                    gjfStyle = it
                    prefs.edit().putString(PreferenceKeys.FORMATTER_GJF_STYLE, it).apply()
                }
            )
        }
    }
}
