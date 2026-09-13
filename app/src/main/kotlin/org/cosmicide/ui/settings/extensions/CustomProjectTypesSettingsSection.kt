package org.cosmicide.ui.settings.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.cosmicide.plugin.customproject.CustomProjectTypeConfiguration

@Composable
internal fun CustomProjectTypesSettingsSection(
    repository: ExtensionsSettingsRepository,
    refreshVersion: Int,
    onChanged: () -> Unit
) {
    var editedProjectType by remember {
        mutableStateOf<CustomProjectTypeConfiguration?>(null)
    }
    var showDialog by remember { mutableStateOf(false) }
    val projectTypes = remember(refreshVersion) {
        repository.customProjectTypes()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Custom Project Types",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Register project creation scripts, marker files, and build or run commands. Commands run as trusted shell code.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (projectTypes.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "No custom project types registered.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    projectTypes.forEachIndexed { index, configuration ->
                        CustomProjectTypeRow(
                            configuration = configuration,
                            index = index,
                            count = projectTypes.size,
                            onEnabledChange = { enabled ->
                                repository.saveCustomProjectType(configuration.copy(enabled = enabled))
                                onChanged()
                            },
                            onEdit = {
                                editedProjectType = configuration
                                showDialog = true
                            },
                            onDelete = {
                                repository.removeCustomProjectType(configuration.id)
                                onChanged()
                            }
                        )
                    }
                }
            }
        }

        item {
            FilledTonalButton(
                onClick = {
                    editedProjectType = null
                    showDialog = true
                },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add project type", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    if (showDialog) {
        CustomProjectTypeDialog(
            existing = editedProjectType,
            onDismiss = { showDialog = false },
            onSave = { configuration ->
                repository.saveCustomProjectType(configuration)
                showDialog = false
                onChanged()
            }
        )
    }
}

@Composable
private fun CustomProjectTypeRow(
    configuration: CustomProjectTypeConfiguration,
    index: Int,
    count: Int,
    onEnabledChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    SegmentedListItem(
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        content = {
            Text(
                text = configuration.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            val commandCount = configuration.commands.size +
                    listOf(
                        configuration.syncCommand,
                        configuration.buildCommand,
                        configuration.runCommand
                    ).count { !it.isNullOrBlank() }
            Text(
                text = "${configuration.markerFiles.size} marker file(s) • $commandCount command(s)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = configuration.enabled, onCheckedChange = onEnabledChange)
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit ${configuration.name}")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete ${configuration.name}")
                }
            }
        }
    )
}

@Composable
private fun CustomProjectTypeDialog(
    existing: CustomProjectTypeConfiguration?,
    onDismiss: () -> Unit,
    onSave: (CustomProjectTypeConfiguration) -> Unit
) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var markers by remember(existing?.id) {
        mutableStateOf(existing?.markerFiles?.joinToString("\n").orEmpty())
    }
    var createCommand by remember(existing?.id) {
        mutableStateOf(existing?.createCommand.orEmpty())
    }
    var syncCommand by remember(existing?.id) {
        mutableStateOf(existing?.syncCommand.orEmpty())
    }
    var buildCommand by remember(existing?.id) {
        mutableStateOf(existing?.buildCommand.orEmpty())
    }
    var runCommand by remember(existing?.id) {
        mutableStateOf(existing?.runCommand.orEmpty())
    }
    var additionalCommands by remember(existing?.id) {
        mutableStateOf(
            existing?.commands?.joinToString("\n") { "${it.name} :: ${it.command}" }.orEmpty()
        )
    }
    var error by remember(existing?.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(if (existing == null) "Add project type" else "Edit project type") },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.8f),
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("Rust / Cargo") },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = markers,
                    onValueChange = { markers = it },
                    label = { Text("Marker files") },
                    placeholder = { Text("Cargo.toml\n.cargo/config.toml") },
                    supportingText = { Text("One relative path per line; any match activates this type.") },
                    shape = MaterialTheme.shapes.medium,
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
                ProjectCommandField(
                    value = createCommand,
                    onValueChange = { createCommand = it },
                    label = "Creation code (optional)",
                    placeholder = "cargo init ."
                )
                ProjectCommandField(
                    value = syncCommand,
                    onValueChange = { syncCommand = it },
                    label = "Sync code (optional)",
                    placeholder = "tool install || tool sync"
                )
                ProjectCommandField(
                    value = buildCommand,
                    onValueChange = { buildCommand = it },
                    label = "Build code (optional)",
                    placeholder = "cargo build"
                )
                ProjectCommandField(
                    value = runCommand,
                    onValueChange = { runCommand = it },
                    label = "Run code (optional)",
                    placeholder = "cargo run"
                )
                OutlinedTextField(
                    value = additionalCommands,
                    onValueChange = { additionalCommands = it },
                    label = { Text("Additional commands") },
                    placeholder = { Text("Test :: cargo test\nFormat :: cargo fmt") },
                    supportingText = { Text("One per line using Label :: shell code.") },
                    shape = MaterialTheme.shapes.medium,
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
                Text(
                    text = "Commands run with Bash in the project directory. Creation also receives COSMIC_PROJECT_ROOT, COSMIC_PROJECT_NAME, and COSMIC_PROJECT_TYPE.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
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
            TextButton(
                onClick = {
                    runCatching {
                        buildCustomProjectTypeConfiguration(
                            existing = existing,
                            name = name,
                            markers = markers,
                            createCommand = createCommand,
                            syncCommand = syncCommand,
                            buildCommand = buildCommand,
                            runCommand = runCommand,
                            additionalCommands = additionalCommands
                        )
                    }.onSuccess(onSave).onFailure { error = it.message }
                },
                shapes = ButtonDefaults.shapes()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text("Cancel") }
        }
    )
}

@Composable
private fun ProjectCommandField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        shape = MaterialTheme.shapes.medium,
        minLines = 2,
        maxLines = 6,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    )
}
