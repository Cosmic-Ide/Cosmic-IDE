package org.cosmicide.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import org.cosmicide.common.Prefs
import org.cosmicide.editor.lsp.CustomLspConfiguration
import org.cosmicide.editor.lsp.CustomLspConfigurationStore
import org.cosmicide.plugin.CosmicPluginHost
import org.cosmicide.ui.settings.components.EditTextPreference
import org.cosmicide.ui.settings.components.SwitchPreference
import org.cosmicide.util.PreferenceKeys
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val customLspStore = remember { CustomLspConfigurationStore(context) }
    val installedPlugins = remember { CosmicPluginHost.pluginManager?.plugins.orEmpty() }
    val extensions = remember { CosmicPluginHost.configurableExtensions() }

    var refreshKey by remember { mutableIntStateOf(0) }
    var pluginRepo by remember {
        mutableStateOf(
            prefs.getString(PreferenceKeys.PLUGIN_REPOSITORY, Prefs.pluginRepository)
                ?: Prefs.pluginRepository
        )
    }
    var editedConfiguration by remember { mutableStateOf<CustomLspConfiguration?>(null) }
    var showCustomLspDialog by remember { mutableStateOf(false) }
    val customConfigurations = remember(refreshKey) { customLspStore.configurations() }

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
            SectionTitle("Extension providers")
            extensions.groupBy { it.category }.forEach { (category, items) ->
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                items.forEach { item ->
                    val enabled = CosmicPluginHost.extensionSettings
                        .isEnabled(item.extension)
                    SwitchPreference(
                        title = item.extension.displayName,
                        summary = item.extension.description.ifBlank { item.extension.id },
                        checked = enabled,
                        onCheckedChange = {
                            CosmicPluginHost.extensionSettings.setEnabled(item.extension, it)
                            refreshKey++
                        }
                    )
                }
            }

            HorizontalDivider()
            SectionTitle("Custom language servers")
            customConfigurations.forEach { configuration ->
                CustomLspRow(
                    configuration = configuration,
                    onEnabledChange = {
                        customLspStore.save(configuration.copy(enabled = it))
                        refreshKey++
                    },
                    onEdit = {
                        editedConfiguration = configuration
                        showCustomLspDialog = true
                    },
                    onDelete = {
                        customLspStore.remove(configuration.id)
                        refreshKey++
                    }
                )
            }
            Button(
                onClick = {
                    editedConfiguration = null
                    showCustomLspDialog = true
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add language server", modifier = Modifier.padding(start = 8.dp))
            }

            if (installedPlugins.isNotEmpty()) {
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

            HorizontalDivider()
            SectionTitle("Plugin repository")
            EditTextPreference(
                title = "Repository",
                summary = "Plugin index URL",
                value = pluginRepo,
                onValueChange = {
                    pluginRepo = it
                    prefs.edit().putString(PreferenceKeys.PLUGIN_REPOSITORY, it).apply()
                }
            )
        }
    }

    if (showCustomLspDialog) {
        CustomLspDialog(
            existing = editedConfiguration,
            onDismiss = { showCustomLspDialog = false },
            onSave = {
                customLspStore.save(it)
                showCustomLspDialog = false
                refreshKey++
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
    )
}

@Composable
private fun CustomLspRow(
    configuration: CustomLspConfiguration,
    onEnabledChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(configuration.name, style = MaterialTheme.typography.titleMedium)
            Text(
                ".${configuration.fileExtension} - ${
                    configuration.startScript.lineSequence().first()
                }",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
        Switch(checked = configuration.enabled, onCheckedChange = onEnabledChange)
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit ${configuration.name}")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete ${configuration.name}")
        }
    }
}

@Composable
private fun CustomLspDialog(
    existing: CustomLspConfiguration?,
    onDismiss: () -> Unit,
    onSave: (CustomLspConfiguration) -> Unit
) {
    var name by remember(existing?.id) { mutableStateOf(TextFieldValue(existing?.name.orEmpty())) }
    var extension by remember(existing?.id) {
        mutableStateOf(TextFieldValue(existing?.fileExtension.orEmpty()))
    }
    var startScript by remember(existing?.id) {
        mutableStateOf(TextFieldValue(existing?.startScript.orEmpty()))
    }
    var error by remember(existing?.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add language server" else "Edit language server") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = extension,
                    onValueChange = { extension = it },
                    label = { Text("File type") },
                    placeholder = { Text("rs") },
                    prefix = { Text(".") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
                OutlinedTextField(
                    value = startScript,
                    onValueChange = { startScript = it },
                    label = { Text("Starter code") },
                    placeholder = { Text("rust-analyzer") },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val configuration = CustomLspConfiguration(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    name = name.text,
                    fileExtension = extension.text,
                    startScript = startScript.text,
                    enabled = existing?.enabled ?: true
                ).normalized()
                runCatching(configuration::validate)
                    .onSuccess { onSave(configuration) }
                    .onFailure { error = it.message }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
