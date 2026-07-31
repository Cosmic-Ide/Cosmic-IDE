/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */

package org.cosmicide.ui.plugin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.cosmicide.project.OperationReporter
import org.cosmicide.project.PluginFormField
import org.cosmicide.project.PluginFormFieldType
import org.cosmicide.project.Project
import org.cosmicide.project.ProjectAction
import org.cosmicide.project.ProjectActionProvider
import org.cosmicide.project.ProjectActionRequest
import org.cosmicide.project.ProjectCreationProvider
import org.cosmicide.project.ProjectCreationRequest
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectCreationDialog(
    provider: ProjectCreationProvider,
    projectsDirectory: File,
    onDismiss: () -> Unit,
    onProjectCreated: (Project, String) -> Unit
) {
    PluginOperationSheet(
        title = provider.displayName,
        description = provider.description,
        fields = provider.fields,
        runLabel = provider.actionLabel,
        onDismiss = onDismiss
    ) { values, reporter ->
        val result = provider.create(
            ProjectCreationRequest(projectsDirectory, values),
            reporter
        )
        onProjectCreated(result.project, result.message)
        result.message
    }
}

@Composable
fun ProjectActionDialog(
    provider: ProjectActionProvider,
    action: ProjectAction,
    project: Project,
    onDismiss: () -> Unit,
    onCompleted: (String, Boolean) -> Unit
) {
    PluginOperationDialog(
        title = action.label,
        description = action.description,
        fields = action.fields,
        runLabel = "Run",
        onDismiss = onDismiss
    ) { values, reporter ->
        val result = provider.execute(
            ProjectActionRequest(project, action.id, values),
            reporter
        )
        onCompleted(result.message, result.refreshProject)
        result.message
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PluginOperationSheet(
    title: String,
    description: String,
    fields: List<PluginFormField>,
    runLabel: String,
    onDismiss: () -> Unit,
    operation: suspend (Map<String, String>, OperationReporter) -> String
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden
    )
    val scope = rememberCoroutineScope()
    val contentScrollState = rememberScrollState()
    val outputScrollState = rememberScrollState()

    var values by remember(fields) {
        mutableStateOf(fields.associate { it.id to it.defaultValue })
    }
    var output by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf<Float?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    var completedMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var operationJob by remember { mutableStateOf<Job?>(null) }

    val isValid = fields
        .filter(PluginFormField::required)
        .all { values[it.id].orEmpty().isNotBlank() }

    LaunchedEffect(output) {
        outputScrollState.scrollTo(outputScrollState.maxValue)
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isRunning) onDismiss()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .verticalScroll(contentScrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                fields.forEach { field ->
                    PluginField(
                        field = field,
                        value = values[field.id].orEmpty(),
                        enabled = !isRunning,
                        onValueChange = { value ->
                            values = values + (field.id to value)
                        }
                    )
                }

                if (isRunning) {
                    if (progress == null) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = { progress ?: 0f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                errorMessage?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                }

                completedMessage?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.primary)
                }

                if (output.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.small
                    ) {
                        SelectionContainer {
                            Text(
                                text = output,
                                modifier = Modifier
                                    .heightIn(min = 120.dp, max = 260.dp)
                                    .verticalScroll(outputScrollState)
                                    .padding(10.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        if (isRunning) {
                            operationJob?.cancel()
                            isRunning = false
                        } else {
                            onDismiss()
                        }
                    }
                ) {
                    Text(if (isRunning) "Cancel" else "Dismiss")
                }

                if (completedMessage != null) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                } else {
                    TextButton(
                        enabled = isValid && !isRunning,
                        onClick = {
                            output = ""
                            errorMessage = null
                            progress = null
                            isRunning = true
                            operationJob = scope.launch {
                                try {
                                    completedMessage = operation(
                                        values,
                                        OperationReporter { update ->
                                            scope.launch {
                                                progress = update.progress ?: progress
                                                if (update.message.isNotEmpty()) {
                                                    output = (output + update.message)
                                                        .takeLast(MAX_VISIBLE_OUTPUT_CHARS)
                                                }
                                            }
                                        }
                                    )
                                } catch (_: CancellationException) {
                                    // Cancellation is user initiated; the process service terminates its child.
                                } catch (error: Throwable) {
                                    errorMessage = error.message ?: "Operation failed"
                                } finally {
                                    isRunning = false
                                }
                            }
                        }
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        Text(runLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginOperationDialog(
    title: String,
    description: String,
    fields: List<PluginFormField>,
    runLabel: String,
    onDismiss: () -> Unit,
    operation: suspend (Map<String, String>, OperationReporter) -> String
) {
    val scope = rememberCoroutineScope()
    val outputScrollState = rememberScrollState()
    var values by remember(fields) {
        mutableStateOf(fields.associate { it.id to it.defaultValue })
    }
    var output by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf<Float?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    var completedMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var operationJob by remember { mutableStateOf<Job?>(null) }

    val isValid = fields.filter { it.required }.all { values[it.id].orEmpty().isNotBlank() }

    LaunchedEffect(output) {
        outputScrollState.scrollTo(outputScrollState.maxValue)
    }

    AlertDialog(
        onDismissRequest = {
            if (!isRunning) onDismiss()
        },
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (description.isNotBlank()) {
                    Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                fields.forEach { field ->
                    PluginField(
                        field = field,
                        value = values[field.id].orEmpty(),
                        enabled = !isRunning,
                        onValueChange = { value -> values = values + (field.id to value) }
                    )
                }

                if (isRunning) {
                    if (progress == null) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = { progress ?: 0f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                completedMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }

                if (output.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.small
                    ) {
                        SelectionContainer {
                            Text(
                                text = output,
                                modifier = Modifier
                                    .heightIn(min = 120.dp, max = 260.dp)
                                    .verticalScroll(outputScrollState)
                                    .padding(10.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (completedMessage != null) {
                TextButton(onClick = onDismiss) { Text("Close") }
            } else {
                TextButton(
                    enabled = isValid && !isRunning,
                    onClick = {
                        output = ""
                        errorMessage = null
                        progress = null
                        isRunning = true
                        operationJob = scope.launch {
                            try {
                                completedMessage = operation(
                                    values,
                                    OperationReporter { update ->
                                        scope.launch {
                                            progress = update.progress ?: progress
                                            if (update.message.isNotEmpty()) {
                                                output = (output + update.message)
                                                    .takeLast(MAX_VISIBLE_OUTPUT_CHARS)
                                            }
                                        }
                                    }
                                )
                            } catch (_: CancellationException) {
                                // Cancellation is user initiated; the process service terminates its child.
                            } catch (error: Throwable) {
                                errorMessage = error.message ?: "Operation failed"
                            } finally {
                                isRunning = false
                            }
                        }
                    }
                ) {
                    Text(runLabel)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (isRunning) {
                        operationJob?.cancel()
                        isRunning = false
                    } else {
                        onDismiss()
                    }
                }
            ) {
                if (isRunning) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    Text("Cancel")
                } else {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun PluginField(
    field: PluginFormField,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    if (field.type == PluginFormFieldType.CHOICE) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(field.label, style = MaterialTheme.typography.labelLarge)
            field.options.forEach { option ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = value == option.value,
                        onClick = { onValueChange(option.value) },
                        enabled = enabled
                    )
                    Text(option.label)
                }
            }
            if (field.description.isNotBlank()) {
                Text(
                    field.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    if (field.type == PluginFormFieldType.BOOLEAN) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(field.label)
                if (field.description.isNotBlank()) {
                    Text(
                        field.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = value.toBoolean(),
                onCheckedChange = { onValueChange(it.toString()) },
                enabled = enabled
            )
        }
        return
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        label = { Text(field.label) },
        placeholder = field.placeholder.takeIf { it.isNotBlank() }?.let { { Text(it) } },
        supportingText = field.description.takeIf { it.isNotBlank() }?.let { { Text(it) } },
        visualTransformation = if (field.type == PluginFormFieldType.PASSWORD) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        }
    )
}

private const val MAX_VISIBLE_OUTPUT_CHARS = 32_000

