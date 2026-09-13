package org.cosmicide.ui.settings.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.cosmicide.ui.settings.components.SwitchPreference

@Composable
internal fun ExtensionProvidersSection(
    repository: ExtensionsSettingsRepository,
    refreshVersion: Int,
    onChanged: () -> Unit
) {
    val extensions = remember(refreshVersion) { repository.extensionItems() }
    val grouped = remember(extensions) { extensions.groupBy { it.category } }

    val enabledStates = remember(extensions, refreshVersion) {
        mutableStateMapOf<String, Boolean>().apply {
            extensions.forEach { item ->
                put(item.extension.id, repository.isExtensionEnabled(item))
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Feature Providers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Enable or disable built-in and plugin-provided extension features for theme handling, code formatting, and language services.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        grouped.forEach { (category, items) ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                    items.forEachIndexed { index, item ->
                        val isChecked =
                            enabledStates[item.extension.id] ?: repository.isExtensionEnabled(item)
                        SwitchPreference(
                            title = item.extension.displayName,
                            summary = item.extension.description.ifBlank { item.extension.id },
                            checked = isChecked,
                            onCheckedChange = { enabled ->
                                enabledStates[item.extension.id] = enabled
                                repository.setExtensionEnabled(item, enabled)
                                onChanged()
                            },
                            index = index,
                            count = items.size
                        )
                    }
                }
            }
        }
    }
}
