package org.cosmicide.ui.settings.extensions

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.cosmicide.editor.lsp.CustomLspConfiguration

@Composable
internal fun CustomLspSettingsSection(
    repository: ExtensionsSettingsRepository,
    refreshVersion: Int,
    onChanged: () -> Unit
) {
    var editedConfiguration by remember { mutableStateOf<CustomLspConfiguration?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    val configurations = remember(refreshVersion) {
        repository.customLspConfigurations()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Custom Language Servers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Configure language servers to handle code completion, diagnostics, and navigation for custom file types. Only one server can be active per file extension.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (configurations.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "No custom language servers configured.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    configurations.forEachIndexed { index, configuration ->
                        CustomLspRow(
                            configuration = configuration,
                            index = index,
                            count = configurations.size,
                            onEnabledChange = { enabled ->
                                repository.saveCustomLsp(configuration.copy(enabled = enabled))
                                onChanged()
                            },
                            onEdit = {
                                editedConfiguration = configuration
                                showDialog = true
                            },
                            onDelete = {
                                repository.removeCustomLsp(configuration.id)
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
                    editedConfiguration = null
                    showDialog = true
                },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add language server", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    if (showDialog) {
        CustomLspDialog(
            existing = editedConfiguration,
            onDismiss = { showDialog = false },
            onSave = { configuration ->
                repository.saveCustomLsp(configuration)
                showDialog = false
                onChanged()
            }
        )
    }
}

@Composable
private fun CustomLspRow(
    configuration: CustomLspConfiguration,
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
            Text(
                text = "${configuration.fileExtensions.joinToString { ".$it" }} • ${
                    configuration.startScript.lineSequence().first()
                }${configuration.textMateGrammarLink?.let { "\nGrammar: $it" }.orEmpty()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
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
private fun CustomLspDialog(
    existing: CustomLspConfiguration?,
    onDismiss: () -> Unit,
    onSave: (CustomLspConfiguration) -> Unit
) {
    val context = LocalContext.current
    var name by remember(existing?.id) { mutableStateOf(TextFieldValue(existing?.name.orEmpty())) }
    var extension by remember(existing?.id) {
        mutableStateOf(TextFieldValue(existing?.fileExtension.orEmpty()))
    }
    var startScript by remember(existing?.id) {
        mutableStateOf(TextFieldValue(existing?.startScript.orEmpty()))
    }
    var grammarLink by remember(existing?.id) {
        mutableStateOf(TextFieldValue(existing?.textMateGrammarLink.orEmpty()))
    }
    var error by remember(existing?.id) { mutableStateOf<String?>(null) }
    val grammarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            grammarLink = TextFieldValue(it.toString())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(if (existing == null) "Add language server" else "Edit language server") },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.8f),
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = extension,
                    onValueChange = { extension = it },
                    label = { Text("File types") },
                    placeholder = { Text("rs, rlib") },
                    supportingText = { Text("Separate extensions with commas or spaces.") },
                    shape = MaterialTheme.shapes.medium,
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
                    shape = MaterialTheme.shapes.medium,
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
                OutlinedTextField(
                    value = grammarLink,
                    onValueChange = { grammarLink = it },
                    label = { Text("TextMate grammar link (optional)") },
                    placeholder = { Text("https://example.com/language.tmLanguage.json") },
                    supportingText = {
                        Text("Use a direct URL, content URI, or absolute path. HTTPS links refresh weekly.")
                    },
                    shape = MaterialTheme.shapes.medium,
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
                TextButton(
                    onClick = {
                        grammarPicker.launch(
                            arrayOf(
                                "application/json",
                                "application/xml",
                                "text/*",
                                "application/octet-stream"
                            )
                        )
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Choose grammar file")
                }
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
                        buildCustomLspConfiguration(
                            existing = existing,
                            name = name.text,
                            fileExtension = extension.text,
                            startScript = startScript.text,
                            grammarLink = grammarLink.text
                        )
                    }
                        .onSuccess(onSave)
                        .onFailure { error = it.message }
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
