package org.cosmicide.ui.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.cosmicide.R
import org.cosmicide.model.ProjectViewModel
import org.cosmicide.project.Language
import org.cosmicide.project.Project
import org.cosmicide.util.FileUtil
import java.io.File
import java.io.IOException

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun NewProjectScreen(
    viewModel: ProjectViewModel = viewModel(),
    onBack: () -> Unit,
    onNavigateToEditor: (Project) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var name by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var language by remember { mutableStateOf(Language.Kotlin as Language) }

    val isPackageInvalid =
        packageName.isNotEmpty() && !packageName.matches("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$".toRegex())
    val isFormValid = name.isNotBlank() && packageName.isNotBlank() && !isPackageInvalid

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.new_project),
                        style = MaterialTheme.typography.headlineMediumEmphasized
                    )
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Development language",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    ToggleButton(
                        checked = language == Language.Java,
                        onCheckedChange = { language = Language.Java },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Java", style = MaterialTheme.typography.labelLarge)
                    }

                    ToggleButton(
                        checked = language == Language.Kotlin,
                        onCheckedChange = { language = Language.Kotlin },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Kotlin", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
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
                )

                TextField(
                    value = packageName,
                    onValueChange = { packageName = it },
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
                    isError = isPackageInvalid,
                    supportingText = {
                        if (isPackageInvalid) {
                            Text(
                                text = "Invalid package naming convention style",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (isFormValid) {
                        val project = createProject(language, name, packageName)
                        if (project != null) {
                            viewModel.loadProjects()
                            onNavigateToEditor(project)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shapes = ButtonDefaults.shapes(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.create_project),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun createProject(
    language: Language,
    name: String,
    packageName: String
): Project? {
    return try {
        val projectName = name.replace("\\.", "")
        val root = FileUtil.projectDir.resolve(projectName).apply { mkdirs() }
        val project = Project(root = root, language = language)
        val srcDir = project.srcDir.apply { mkdirs() }
        val mainFile = srcDir.resolve(packageName.replace('.', '/')).apply { mkdirs() }
            .resolve("Main.${language.extension}")
        mainFile.createMainFile(language, packageName)
        project
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

private fun File.createMainFile(language: Language, packageName: String) {
    val content = language.classFileContent(name = "Main", packageName = packageName)
    writeText(content)
}
