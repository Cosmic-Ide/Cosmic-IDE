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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.cosmicide.app.LocalAppContainer
import org.cosmicide.ui.settings.extensions.CustomLspSettingsSection
import org.cosmicide.ui.settings.extensions.CustomProjectTypesSettingsSection
import org.cosmicide.ui.settings.extensions.ExtensionProvidersSection
import org.cosmicide.ui.settings.extensions.InstalledPluginsSection
import org.cosmicide.ui.settings.extensions.PluginRepositorySection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsSettingsScreen(onBack: () -> Unit) {
    val repository = LocalAppContainer.current.extensionsSettingsRepository
    var refreshVersion by remember { mutableIntStateOf(0) }
    val notifyChanged: () -> Unit = { refreshVersion += 1 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extensions") },
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
            ExtensionProvidersSection(
                repository = repository,
                refreshVersion = refreshVersion,
                onChanged = notifyChanged
            )
            CustomLspSettingsSection(
                repository = repository,
                refreshVersion = refreshVersion,
                onChanged = notifyChanged
            )
            CustomProjectTypesSettingsSection(
                repository = repository,
                refreshVersion = refreshVersion,
                onChanged = notifyChanged
            )
            InstalledPluginsSection(repository)
            PluginRepositorySection(repository)
        }
    }
}
