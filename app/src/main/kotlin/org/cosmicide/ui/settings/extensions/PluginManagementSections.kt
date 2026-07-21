package org.cosmicide.ui.settings.extensions

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.cosmicide.ui.settings.components.EditTextPreference

@Composable
internal fun InstalledPluginsSection(repository: ExtensionsSettingsRepository) {
    val installedPlugins = remember { repository.installedPlugins() }
    if (installedPlugins.isEmpty()) return

    HorizontalDivider()
    SectionTitle("Installed plugins")
    installedPlugins.forEach { plugin ->
        Text(
            text = plugin.descriptor.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
        )
        Text(
            text = "${plugin.state} - ${plugin.descriptor.version}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
    }
}

@Composable
internal fun PluginRepositorySection(repository: ExtensionsSettingsRepository) {
    var pluginRepository by remember { mutableStateOf(repository.pluginRepository()) }

    HorizontalDivider()
    SectionTitle("Plugin repository")
    EditTextPreference(
        title = "Repository",
        summary = "Plugin index URL",
        value = pluginRepository,
        onValueChange = { value ->
            pluginRepository = value
            repository.setPluginRepository(value)
        }
    )
}
