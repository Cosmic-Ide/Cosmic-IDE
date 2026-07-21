package org.cosmicide.ui.settings.extensions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
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

    HorizontalDivider()
    SectionTitle("Custom project types")
    Text(
        text = "Register creation scripts and build, run, or utility commands. Commands run as trusted shell code.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
    projectTypes.forEach { configuration ->
        CustomProjectTypeRow(
            configuration = configuration,
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
    Button(
        onClick = {
            editedProjectType = null
            showDialog = true
        },
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Text("Add project type", modifier = Modifier.padding(start = 8.dp))
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
            val commandCount = configuration.commands.size +
                    listOf(
                        configuration.syncCommand,
                        configuration.buildCommand,
                        configuration.runCommand
                    ).count { !it.isNullOrBlank() }
            Text(
                text = "${configuration.markerFiles.size} markers · $commandCount commands",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        title = { Text(if (existing == null) "Add project type" else "Edit project type") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("Rust / Cargo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = markers,
                    onValueChange = { markers = it },
                    label = { Text("Marker files") },
                    placeholder = { Text("Cargo.toml\n.cargo/config.toml") },
                    supportingText = { Text("One relative path per line; any match activates this type.") },
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
            TextButton(onClick = {
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
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
        minLines = 2,
        maxLines = 6,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    )
}
