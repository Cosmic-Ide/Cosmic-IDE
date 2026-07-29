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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.cosmicide.app.LocalAppContainer
import org.cosmicide.ui.settings.extensions.CustomLspSettingsSection
import org.cosmicide.ui.settings.extensions.CustomProjectTypesSettingsSection
import org.cosmicide.ui.settings.extensions.ExtensionProvidersSection
import org.cosmicide.ui.settings.extensions.PluginMarketplaceSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsSettingsScreen(
    onBack: () -> Unit,
    onRunSetupInTerminal: (String) -> Unit
) {
    val repository = LocalAppContainer.current.extensionsSettingsRepository
    var refreshVersion by remember { mutableIntStateOf(0) }
    var selectedTabName by rememberSaveable {
        mutableStateOf(ExtensionsSettingsTab.PROVIDERS.name)
    }
    val selectedTab = ExtensionsSettingsTab.entries
        .firstOrNull { it.name == selectedTabName }
        ?: ExtensionsSettingsTab.PROVIDERS
    val scrollStates = ExtensionsSettingsTab.entries.map { rememberScrollState() }
    val notifyChanged: () -> Unit = { refreshVersion += 1 }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Extensions") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    edgePadding = 8.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    ExtensionsSettingsTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTabName = tab.name },
                            text = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .then(
                    if (selectedTab == ExtensionsSettingsTab.PLUGINS) {
                        Modifier
                    } else {
                        Modifier.verticalScroll(scrollStates[selectedTab.ordinal])
                    }
                )
        ) {
            when (selectedTab) {
                ExtensionsSettingsTab.PROVIDERS -> ExtensionProvidersSection(
                    repository = repository,
                    refreshVersion = refreshVersion,
                    onChanged = notifyChanged
                )

                ExtensionsSettingsTab.LANGUAGES -> CustomLspSettingsSection(
                    repository = repository,
                    refreshVersion = refreshVersion,
                    onChanged = notifyChanged
                )

                ExtensionsSettingsTab.PROJECTS -> CustomProjectTypesSettingsSection(
                    repository = repository,
                    refreshVersion = refreshVersion,
                    onChanged = notifyChanged
                )

                ExtensionsSettingsTab.PLUGINS -> {
                    PluginMarketplaceSection(
                        repository = repository,
                        refreshVersion = refreshVersion,
                        onChanged = notifyChanged,
                        onRunSetupInTerminal = onRunSetupInTerminal
                    )
                }
            }
        }
    }
}

private enum class ExtensionsSettingsTab(val label: String) {
    PROVIDERS("Providers"),
    LANGUAGES("Languages"),
    PROJECTS("Projects"),
    PLUGINS("Plugins")
}
