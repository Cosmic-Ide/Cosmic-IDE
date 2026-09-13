package org.cosmicide.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun PreferenceItem(
    title: String,
    summary: String? = null,
    icon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    index: Int = 0,
    count: Int = 1,
    shapes: ListItemShapes = ListItemDefaults.segmentedShapes(index, count),
    colors: ListItemColors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surface),
    onClick: () -> Unit = {},
) {
    SegmentedListItem(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shapes = shapes,
        colors = colors,
        leadingContent = icon,
        trailingContent = trailingContent,
        supportingContent = summary?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SwitchPreference(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    index: Int = 0,
    count: Int = 1,
) {
    SegmentedListItem(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth(),
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(
                0.7f
            )
        ),
        verticalAlignment = Alignment.CenterVertically,
        supportingContent = summary?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SingleChoicePreference(
    title: String,
    summary: String? = null,
    selectedItem: String,
    items: List<Pair<String, String>>,
    onItemSelected: (String) -> Unit,
    index: Int = 0,
    count: Int = 1,
    shapes: ListItemShapes = ListItemDefaults.segmentedShapes(index, count),
    colors: ListItemColors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surface)
) {
    var showDialog by remember { mutableStateOf(false) }

    PreferenceItem(
        title = title,
        summary = summary,
        index = index,
        count = count,
        shapes = shapes,
        colors = colors,
        onClick = { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            modifier = Modifier.fillMaxWidth(0.8f),
            onDismissRequest = { showDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items.forEach { (value, label) ->
                        item {
                            ListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        onItemSelected(value)
                                        showDialog = false
                                    },
                                leadingContent = {
                                    RadioButton(
                                        selected = value == selectedItem,
                                        onClick = null
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (value == selectedItem) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("Cancel")
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }
}

@Composable
fun MultiChoicePreference(
    title: String,
    summary: String? = null,
    selectedItems: Set<String>,
    items: List<Pair<String, String>>,
    onItemsSelected: (Set<String>) -> Unit,
    index: Int = 0,
    count: Int = 1,
    shapes: ListItemShapes = ListItemDefaults.segmentedShapes(index, count),
    colors: ListItemColors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surface)
) {
    var showDialog by remember { mutableStateOf(false) }
    var currentSelected by remember(selectedItems) { mutableStateOf(selectedItems) }

    PreferenceItem(
        title = title,
        summary = if (selectedItems.isEmpty()) "None" else summary,
        index = index,
        count = count,
        shapes = shapes,
        colors = colors,
        onClick = { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items.forEach { (value, label) ->
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    val newSet = if (currentSelected.contains(value)) {
                                        currentSelected - value
                                    } else {
                                        currentSelected + value
                                    }
                                    currentSelected = newSet
                                },
                            leadingContent = {
                                Checkbox(
                                    checked = currentSelected.contains(value),
                                    onCheckedChange = null
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        ) {
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onItemsSelected(currentSelected)
                        showDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SliderPreference(
    title: String,
    summary: String? = null,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
    index: Int = 0,
    count: Int = 1
) {
    SegmentedListItem(
        modifier = Modifier.fillMaxWidth(),
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surface),
        trailingContent = {
            Text(
                text = value.toInt().toString(),
                style = MaterialTheme.typography.labelLargeEmphasized,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        },
        overlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Slider(
                state = rememberSliderState(
                    value = value,
                    steps = steps,
                    trackRange = valueRange
                ),
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onValueChange
            )
        },
        content = {
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
fun EditTextPreference(
    title: String,
    summary: String? = null,
    value: String,
    onValueChange: (String) -> Unit,
    index: Int = 0,
    count: Int = 1,
    shapes: ListItemShapes = ListItemDefaults.segmentedShapes(index, count),
    colors: ListItemColors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surface)
) {
    var showDialog by remember { mutableStateOf(false) }
    var text by remember(value) { mutableStateOf(value) }

    PreferenceItem(
        title = title,
        summary = if (value.isEmpty()) "Not set" else summary,
        index = index,
        count = count,
        shapes = shapes,
        colors = colors,
        onClick = { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange(text)
                        showDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
