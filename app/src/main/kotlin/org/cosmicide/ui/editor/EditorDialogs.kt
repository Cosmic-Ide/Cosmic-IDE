package org.cosmicide.ui.editor

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.DialogProperties
import io.github.rosemoe.sora.text.Content

@Composable
internal fun TasksDialog(
    tasks: List<String>,
    isLoading: Boolean,
    loadError: String?,
    onDismiss: () -> Unit,
    onTaskSelected: (String) -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Gradle Tasks") }, text = {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            loadError != null -> {
                Text(
                    text = "Failed to fetch tasks: $loadError",
                    color = MaterialTheme.colorScheme.error
                )
            }

            tasks.isEmpty() -> {
                Text("No Gradle tasks found")
            }

            else -> {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(tasks) { task ->
                        DropdownMenuItem(text = { Text(task) }, onClick = {
                            onTaskSelected(task)
                            onDismiss()
                        })
                    }
                }
            }
        }
    }, confirmButton = {}, dismissButton = {
        TextButton(onClick = onDismiss) { Text("Close") }
    })
}

@Composable
internal fun GoToLineDialog(lineCount: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Go to Line") }, text = {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.filter { char -> char.isDigit() } },
            label = { Text("Line number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }, confirmButton = {
        Button(
            onClick = {
                val lineNumber = text.toIntOrNull()
                if (lineNumber != null && lineNumber in 1..lineCount) {
                    onConfirm(lineNumber)
                } else {
                    Toast.makeText(
                        context,
                        "Invalid line number. Must be between 1 and $lineCount.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }, shapes = ButtonDefaults.shapes()
        ) {
            Text("Go")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    })
}

@Composable
internal fun ProgramArgumentDialog(
    title: String, savedArgs: List<String>, onSave: (List<String>) -> Unit, onDismiss: () -> Unit
) {
    var args by remember { mutableStateOf(savedArgs.joinToString(" ")) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        OutlinedTextField(
            value = args,
            label = { Text(title) },
            singleLine = true,
            onValueChange = { args = it })
    }, confirmButton = {
        Button(
            onClick = {
                onSave(args.split(' '))
                onDismiss()
            }, shapes = ButtonDefaults.shapes()
        ) {
            Text("Save")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    })
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun Statistics(content: Content, onDismiss: () -> Unit) {
    val bytes = content.toString().toByteArray().size
    val charCount = content.length

    AlertDialog(
        onDismissRequest = { onDismiss() },
        modifier = Modifier.fillMaxWidth(0.8f),
        title = { Text("Statistics") },
        text = {
            Column {
                Text("Byte Count: $bytes")
                Text("Character Count: $charCount")
                Text("Word Count: ${content.split(" ").size}")
                Text("Line Count: ${content.lineCount}")
            }
        },
        confirmButton = {
            TextButton(onDismiss) {
                Text("Dismiss")
            }
        },
        dismissButton = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}
