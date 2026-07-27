package org.cosmicide.ui.settings.extensions

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
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

    SectionTitle("Custom language servers")
    Text(
        text = "Only one custom server can be active for each file type.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
    configurations.forEach { configuration ->
        CustomLspRow(
            configuration = configuration,
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
    Button(
        onClick = {
            editedConfiguration = null
            showDialog = true
        },
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Text("Add language server", modifier = Modifier.padding(start = 8.dp))
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
                }${configuration.textMateGrammarLink?.let { "\nGrammar: $it" }.orEmpty()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
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
        title = { Text(if (existing == null) "Add language server" else "Edit language server") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                OutlinedTextField(
                    value = grammarLink,
                    onValueChange = { grammarLink = it },
                    label = { Text("TextMate grammar link (optional)") },
                    placeholder = { Text("https://example.com/language.tmLanguage.json") },
                    supportingText = {
                        Text("Use a direct URL, content URI, or absolute path. HTTPS links refresh weekly.")
                    },
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
            TextButton(onClick = {
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
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
