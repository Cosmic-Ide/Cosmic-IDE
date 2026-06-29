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
import org.cosmicide.chat.ChatProvider
import org.cosmicide.ui.settings.components.EditTextPreference
import org.cosmicide.ui.settings.components.SingleChoicePreference
import org.cosmicide.ui.settings.components.SliderPreference
import org.cosmicide.util.PreferenceKeys

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var apiKey by remember { mutableStateOf(prefs.getString(PreferenceKeys.GEMINI_API_KEY, "") ?: "") }
    var model by remember { mutableStateOf(prefs.getString(PreferenceKeys.GEMINI_MODEL, "gemini-2.0-flash") ?: "gemini-2.0-flash") }
    var temperature by remember { mutableStateOf(prefs.getString(PreferenceKeys.TEMPERATURE, "0.9") ?: "0.9") }
    var topP by remember { mutableStateOf(prefs.getString(PreferenceKeys.TOP_P, "1.0") ?: "1.0") }
    var topK by remember { mutableStateOf(prefs.getInt(PreferenceKeys.TOP_K, 1).toFloat()) }
    var maxTokens by remember { mutableStateOf(prefs.getInt(PreferenceKeys.MAX_TOKENS, 1024).toFloat()) }

    val tempOptions = (0..10).map { (it / 10.0f).toString() to (it / 10.0f).toString() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gemini") },
                navigationIcon = {
                    IconButton(onClick = {
                        ChatProvider.regenerateModel()
                        onBack()
                    }) {
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
                title = "Gemini API Key",
                summary = "The API key to connect to the Gemini API. You can get one at https://makersuite.google.com/app/apikey",
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    prefs.edit().putString(PreferenceKeys.GEMINI_API_KEY, it).apply()
                }
            )

            EditTextPreference(
                title = "Gemini Model",
                summary = "The model to use for Gemini. Default is 'gemini-2.0-flash'.",
                value = model,
                onValueChange = {
                    model = it
                    prefs.edit().putString(PreferenceKeys.GEMINI_MODEL, it).apply()
                }
            )

            SingleChoicePreference(
                title = "temperature",
                summary = "Controls the randomness of the output.",
                selectedItem = temperature,
                items = tempOptions,
                onItemSelected = {
                    temperature = it
                    prefs.edit().putString(PreferenceKeys.TEMPERATURE, it).apply()
                }
            )

            SingleChoicePreference(
                title = "top_p",
                summary = "The maximum cumulative probability of tokens to consider when sampling.",
                selectedItem = topP,
                items = tempOptions,
                onItemSelected = {
                    topP = it
                    prefs.edit().putString(PreferenceKeys.TOP_P, it).apply()
                }
            )

            SliderPreference(
                title = "top_k",
                summary = "top_k sets the maximum number of tokens to sample from on each step.",
                value = topK,
                valueRange = 1f..60f,
                steps = 59,
                onValueChange = {
                    topK = it
                    prefs.edit().putInt(PreferenceKeys.TOP_K, it.toInt()).apply()
                }
            )

            SliderPreference(
                title = "max_tokens",
                summary = "max_tokens sets the maximum number of tokens to generate.",
                value = maxTokens,
                valueRange = 60f..2048f,
                onValueChange = {
                    maxTokens = it
                    prefs.edit().putInt(PreferenceKeys.MAX_TOKENS, it.toInt()).apply()
                }
            )
        }
    }
}
