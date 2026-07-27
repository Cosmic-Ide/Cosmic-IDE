package org.cosmicide.ui.project

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.cosmicide.R
import org.cosmicide.project.Language

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun NewProjectFormContent(
    state: NewProjectFormState,
    validation: NewProjectFormValidation,
    isCreating: Boolean,
    creationLog: String,
    creationLogScrollState: ScrollState,
    onStateChange: (NewProjectFormState) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        FormSection(title = "Development language") {
            ConnectedToggleRow {
                ToggleButton(
                    checked = state.language == Language.Java,
                    onCheckedChange = { onStateChange(state.selectLanguage(Language.Java)) },
                    enabled = !isCreating,
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    modifier = Modifier.weight(1f)
                ) { Text("Java", style = MaterialTheme.typography.labelLarge) }
                ToggleButton(
                    checked = state.language == Language.Kotlin,
                    onCheckedChange = { onStateChange(state.selectLanguage(Language.Kotlin)) },
                    enabled = !isCreating,
                    shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                    modifier = Modifier.weight(1f)
                ) { Text("Kotlin", style = MaterialTheme.typography.labelLarge) }
                ToggleButton(
                    checked = state.language == Language.Scala,
                    onCheckedChange = { onStateChange(state.selectLanguage(Language.Scala)) },
                    enabled = !isCreating,
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    modifier = Modifier.weight(1f)
                ) { Text("Scala", style = MaterialTheme.typography.labelLarge) }
            }
        }

        FormSection(title = "Build script DSL") {
            ConnectedToggleRow {
                ToggleButton(
                    checked = state.dslType == DslType.GROOVY,
                    onCheckedChange = { onStateChange(state.copy(dslType = DslType.GROOVY)) },
                    enabled = !isCreating,
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    modifier = Modifier.weight(1f)
                ) { Text("Kotlin", style = MaterialTheme.typography.labelLarge) }
                ToggleButton(
                    checked = state.dslType == DslType.KOTLIN,
                    onCheckedChange = { onStateChange(state.copy(dslType = DslType.KOTLIN)) },
                    enabled = !isCreating,
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    modifier = Modifier.weight(1f)
                ) { Text("Groovy", style = MaterialTheme.typography.labelLarge) }
            }
        }

        FormSection(title = "Project structure") {
            ConnectedToggleRow {
                ToggleButton(
                    checked = !state.splitProject,
                    onCheckedChange = { onStateChange(state.copy(splitProject = false)) },
                    enabled = !isCreating,
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    modifier = Modifier.weight(1f)
                ) { Text("Single module", style = MaterialTheme.typography.labelLarge) }
                ToggleButton(
                    checked = state.splitProject,
                    onCheckedChange = { onStateChange(state.copy(splitProject = true)) },
                    enabled = !isCreating,
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    modifier = Modifier.weight(1f)
                ) { Text("Split project", style = MaterialTheme.typography.labelLarge) }
            }
        }

        if (state.availableTestFrameworks.size > 1) {
            FormSection(title = "Test framework") {
                ConnectedToggleRow {
                    state.availableTestFrameworks.forEachIndexed { index, framework ->
                        ToggleButton(
                            checked = state.testFramework == framework,
                            onCheckedChange = {
                                onStateChange(state.copy(testFramework = framework))
                            },
                            enabled = !isCreating,
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                state.availableTestFrameworks.lastIndex ->
                                    ButtonGroupDefaults.connectedTrailingButtonShapes()

                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(framework.displayName, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextField(
                value = state.name,
                onValueChange = { onStateChange(state.copy(name = it)) },
                label = { Text("Project title") },
                placeholder = { Text("MyAwesomeApplication") },
                leadingIcon = {
                    Icon(
                        Icons.Default.FolderOpen,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isCreating,
                isError = validation.isNameInvalid || validation.projectAlreadyExists,
                supportingText = {
                    when {
                        validation.projectAlreadyExists ->
                            Text("A project with this title already exists")

                        validation.isNameInvalid ->
                            Text("Use letters, numbers, dots, underscores, or hyphens")
                    }
                }
            )

            TextField(
                value = state.packageName,
                onValueChange = { onStateChange(state.copy(packageName = it)) },
                label = { Text("Package name") },
                placeholder = { Text("com.example.app") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Layers,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isCreating,
                isError = validation.isPackageInvalid,
                supportingText = {
                    if (validation.isPackageInvalid) {
                        Text(
                            text = "Invalid package naming convention style",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )
        }

        if (isCreating || creationLog.isNotEmpty()) {
            FormSection(title = "Gradle output") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium
                ) {
                    SelectionContainer {
                        Text(
                            text = creationLog.ifEmpty { "Waiting for Gradle..." },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 160.dp, max = 240.dp)
                                .verticalScroll(creationLogScrollState)
                                .padding(12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onCreate,
            enabled = validation.isValid && !isCreating,
            modifier = Modifier.fillMaxWidth(),
            shapes = ButtonDefaults.shapes(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (isCreating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text = stringResource(R.string.create_project),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConnectedToggleRow(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        content = content
    )
}
