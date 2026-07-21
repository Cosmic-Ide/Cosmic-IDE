package org.cosmicide.ui.settings.extensions

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.cosmicide.ui.settings.components.SwitchPreference

@Composable
internal fun ExtensionProvidersSection(
    repository: ExtensionsSettingsRepository,
    refreshVersion: Int,
    onChanged: () -> Unit
) {
    val extensions = remember(refreshVersion) { repository.extensionItems() }

    SectionTitle("Extension providers")
    extensions.groupBy { it.category }.forEach { (category, items) ->
        Text(
            text = category,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        items.forEach { item ->
            SwitchPreference(
                title = item.extension.displayName,
                summary = item.extension.description.ifBlank { item.extension.id },
                checked = repository.isExtensionEnabled(item),
                onCheckedChange = { enabled ->
                    repository.setExtensionEnabled(item, enabled)
                    onChanged()
                }
            )
        }
    }
}
