package org.cosmicide.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.cosmicide.app.LocalAppContainer
import org.cosmicide.ui.settings.extensions.CustomLspSettingsSection
import org.cosmicide.ui.settings.extensions.CustomProjectTypesSettingsSection
import org.cosmicide.ui.settings.extensions.ExtensionProvidersSection
import org.cosmicide.ui.settings.extensions.PluginMarketplaceSection

@Composable
fun ExtensionsSettingsScreen(
    onBack: () -> Unit,
    onRunSetupInTerminal: (String) -> Unit,
    onNavigateToPluginScreen: ((String, String, Map<String, String>) -> Unit)? = null,
    initialTab: ExtensionsSettingsTab = ExtensionsSettingsTab.PLUGINS
) {
    val repository = LocalAppContainer.current.extensionsSettingsRepository
    var refreshVersion by remember { mutableIntStateOf(0) }
    var selectedTabName by rememberSaveable {
        mutableStateOf(initialTab.name)
    }
    val selectedTab = ExtensionsSettingsTab.entries
        .firstOrNull { it.name == selectedTabName }
        ?: ExtensionsSettingsTab.PLUGINS
    val notifyChanged: () -> Unit = { refreshVersion += 1 }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Extensions") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
                PrimaryTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
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
        ) {
            when (selectedTab) {
                ExtensionsSettingsTab.PLUGINS -> {
                    PluginMarketplaceSection(
                        repository = repository,
                        refreshVersion = refreshVersion,
                        onChanged = notifyChanged,
                        onRunSetupInTerminal = onRunSetupInTerminal
                    )
                }

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
            }
        }
    }
}

enum class ExtensionsSettingsTab(val label: String) {
    PLUGINS("Plugins"),
    PROVIDERS("Providers"),
    LANGUAGES("Languages"),
    PROJECTS("Projects"),
}
