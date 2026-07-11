package org.cosmicide.ui.settings

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
import androidx.preference.PreferenceManager
import org.cosmicide.common.Prefs
import org.cosmicide.plugin.CosmicPluginHost
import org.cosmicide.ui.settings.components.EditTextPreference
import org.cosmicide.ui.settings.components.PreferenceItem
import org.cosmicide.util.PreferenceKeys

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val installedPlugins = remember { CosmicPluginHost.pluginManager?.plugins.orEmpty() }

    var pluginRepo by remember { mutableStateOf(prefs.getString(PreferenceKeys.PLUGIN_REPOSITORY, Prefs.pluginRepository) ?: Prefs.pluginRepository) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plugins") },
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
            PreferenceItem(
                title = "Available plugins",
                summary = "View available plugins",
                onClick = { /* TODO: Navigate to PluginList */ }
            )

            PreferenceItem(
                title = "Installed plugins",
                summary = "${installedPlugins.size} plugin(s) discovered",
                onClick = { /* TODO: Navigate to Plugins */ }
            )

            installedPlugins.forEach { plugin ->
                PreferenceItem(
                    title = plugin.descriptor.name,
                    summary = "${plugin.state} - ${plugin.descriptor.version}",
                    onClick = { }
                )
            }

            EditTextPreference(
                title = "Repository",
                summary = "Add a custom plugin repository",
                value = pluginRepo,
                onValueChange = {
                    pluginRepo = it
                    prefs.edit().putString(PreferenceKeys.PLUGIN_REPOSITORY, it).apply()
                }
            )
        }
    }
}
