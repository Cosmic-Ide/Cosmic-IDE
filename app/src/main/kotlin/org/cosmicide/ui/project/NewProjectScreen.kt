package org.cosmicide.ui.project

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.R
import org.cosmicide.model.ProjectViewModel
import org.cosmicide.project.Language
import org.cosmicide.project.Project
import org.cosmicide.tooling.RemoteGradleConnector
import org.cosmicide.tooling.ToolingServerManager
import org.cosmicide.util.FileUtil
import java.io.IOException
import java.io.OutputStream

/**
 * DSL types supported by Gradle for project configuration.
 */
enum class DslType(val gradleValue: String) {
    KOTLIN("kotlin"),
    GROOVY("groovy")
}

/**
 * Test frameworks available for different languages.
 */
enum class TestFramework(val gradleValue: String, val displayName: String) {
    // Java/Kotlin test frameworks
    JUNIT("junit-jupiter", "JUnit"),
    TESTNG("testng", "TestNG"),
    KotlinTest("kotlintest", "KotlinTest"),

    // Scala test frameworks
    SCALATEST("scalatest", "ScalaTest"),
}

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val creationLogScrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var language by remember { mutableStateOf(Language.Kotlin as Language) }
    var dslType by remember { mutableStateOf(DslType.KOTLIN) }
    var splitProject by remember { mutableStateOf(false) }
    var testFramework by remember(language) {
        mutableStateOf(
            if (language == Language.Scala) TestFramework.SCALATEST else TestFramework.JUNIT
        )
    }
    var isCreating by remember { mutableStateOf(false) }
    var creationLog by remember { mutableStateOf("") }

    fun appendCreationLog(text: String) {
        if (text.isEmpty()) return
        scope.launch {
            creationLog = (creationLog + text).takeLast(MAX_VISIBLE_LOG_CHARS)
        }
    }

    LaunchedEffect(creationLog) {
        creationLogScrollState.scrollTo(creationLogScrollState.maxValue)
    }

    val normalizedName = name.trim()
    val isNameInvalid = name.isNotEmpty() && !PROJECT_NAME.matches(normalizedName)
    val projectAlreadyExists = !isNameInvalid && normalizedName.isNotEmpty() &&
            FileUtil.projectDir.resolve(normalizedName).exists()
    val isPackageInvalid =
        packageName.isNotEmpty() && !PACKAGE_NAME.matches(packageName.trim())
    val isFormValid = normalizedName.isNotEmpty() && packageName.isNotBlank() &&
            !isNameInvalid && !projectAlreadyExists && !isPackageInvalid

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
                    IconButton(onClick = onBack, enabled = !isCreating) {
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
                    horizontalArrangement = Arrangement.spacedBy(
                        ButtonGroupDefaults.ConnectedSpaceBetween
                    ),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    ToggleButton(
                        checked = language == Language.Java,
                        onCheckedChange = { language = Language.Java },
                        enabled = !isCreating,
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Java", style = MaterialTheme.typography.labelLarge)
                    }

                    ToggleButton(
                        checked = language == Language.Kotlin,
                        onCheckedChange = { language = Language.Kotlin },
                        enabled = !isCreating,
                        shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Kotlin", style = MaterialTheme.typography.labelLarge)
                    }

                    ToggleButton(
                        checked = language == Language.Scala,
                        onCheckedChange = { language = Language.Scala },
                        enabled = !isCreating,
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Scala", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Build script DSL",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        ButtonGroupDefaults.ConnectedSpaceBetween
                    ),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    ToggleButton(
                        checked = dslType == DslType.KOTLIN,
                        onCheckedChange = { dslType = DslType.KOTLIN },
                        enabled = !isCreating,
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Kotlin", style = MaterialTheme.typography.labelLarge)
                    }
                    ToggleButton(
                        checked = dslType == DslType.GROOVY,
                        onCheckedChange = { dslType = DslType.GROOVY },
                        enabled = !isCreating,
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Groovy", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Project structure",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        ButtonGroupDefaults.ConnectedSpaceBetween
                    ),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    ToggleButton(
                        checked = !splitProject,
                        onCheckedChange = { splitProject = false },
                        enabled = !isCreating,
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Single module", style = MaterialTheme.typography.labelLarge)
                    }
                    ToggleButton(
                        checked = splitProject,
                        onCheckedChange = { splitProject = true },
                        enabled = !isCreating,
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Split project", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            val availableTestFrameworks = remember(language) {
                when (language) {
                    Language.Java -> listOf(
                        TestFramework.JUNIT,
                        TestFramework.TESTNG
                    )

                    Language.Kotlin -> listOf(
                        TestFramework.JUNIT,
                        TestFramework.KotlinTest
                    )

                    Language.Scala -> listOf(
                        TestFramework.SCALATEST
                    )
                }
            }

            if (availableTestFrameworks.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Test framework",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            ButtonGroupDefaults.ConnectedSpaceBetween
                        ),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        availableTestFrameworks.forEachIndexed { index, framework ->
                            ToggleButton(
                                checked = testFramework == framework,
                                onCheckedChange = { testFramework = framework },
                                enabled = !isCreating,
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    availableTestFrameworks.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    framework.displayName,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
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
                    enabled = !isCreating,
                    isError = isNameInvalid || projectAlreadyExists,
                    supportingText = {
                        when {
                            projectAlreadyExists -> Text("A project with this title already exists")
                            isNameInvalid -> Text("Use letters, numbers, dots, underscores, or hyphens")
                            else -> Unit
                        }
                    }
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
                    enabled = !isCreating,
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

            if (isCreating || creationLog.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Gradle output",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                onClick = {
                    if (isFormValid) {
                        isCreating = true
                        creationLog = "Preparing ${language.name} project...\n"
                        scope.launch {
                            createProjectWithToolingApi(
                                context = context,
                                language = language,
                                name = normalizedName,
                                packageName = packageName.trim(),
                                dslType = dslType,
                                splitProject = splitProject,
                                testFramework = testFramework,
                                onLog = ::appendCreationLog
                            ).fold(
                                onSuccess = { project ->
                                    isCreating = false
                                    viewModel.loadProjects()
                                    onNavigateToEditor(project)
                                },
                                onFailure = { error ->
                                    isCreating = false
                                    snackbarHostState.showSnackbar(
                                        error.message ?: "Gradle could not create the project"
                                    )
                                }
                            )
                        }
                    }
                },
                enabled = isFormValid && !isCreating,
                modifier = Modifier.fillMaxWidth(),
                shapes = ButtonDefaults.shapes(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
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
}

private suspend fun createProjectWithToolingApi(
    context: Context,
    language: Language,
    name: String,
    packageName: String,
    dslType: DslType,
    splitProject: Boolean,
    testFramework: TestFramework,
    onLog: (String) -> Unit
): Result<Project> = withContext(Dispatchers.IO) {
    runCatching {
        val projectsDirectory = FileUtil.projectDir.canonicalFile
        val root = projectsDirectory.resolve(name).canonicalFile

        require(root.parentFile == projectsDirectory) { "Invalid project title" }
        require(!root.exists()) { "A project with this title already exists" }
        check(root.mkdirs()) { "Could not create the project directory" }

        val standardOutput = ProjectCreationOutputStream(onLog)
        val standardError = ProjectCreationOutputStream(onLog)

        try {
            onLog("Starting Gradle Tooling API provider...\n")
            val connection = RemoteGradleConnector(context)
                .forProjectDirectory(root)
                .connect()

            try {
                onLog("Running Gradle init (${language.gradleInitType})...\n")
                val args = mutableListOf(
                    "init",
                    "--type=${language.gradleInitType}",
                    "--dsl=${dslType.gradleValue}",
                    "--project-name=$name",
                    "--package=$packageName",
                    if (splitProject) "--split-project" else "--no-split-project",
                    "--test-framework=${testFramework.gradleValue}",
                    "--use-defaults",
                    "--no-incubating",
                    "--no-comments"
                )
                connection.newBuild()
                    .forTasks("init")
                    .withArguments(args)
                    .setStandardOutput(standardOutput)
                    .setStandardError(standardError)
                    .run()
            } finally {
                connection.close()
                ToolingServerManager.stopCurrent()
            }

            onLog("Project created successfully.\n")
            Project(root = root, language = language)
        } catch (error: Throwable) {
            ToolingServerManager.stopCurrent()
            root.deleteRecursively()

            val gradleMessage = standardError.lastNonBlankLine()
                ?: error.message
                ?: "unknown Gradle error"
            onLog("Project creation failed: $gradleMessage\n")
            throw IOException("Gradle project creation failed: $gradleMessage", error)
        }
    }
}

private val Language.gradleInitType: String
    get() = when (this) {
        Language.Java -> "java-application"
        Language.Kotlin -> "kotlin-application"
        Language.Scala -> "scala-application"
    }

private class ProjectCreationOutputStream(
    private val onText: (String) -> Unit
) : OutputStream() {
    private val captured = StringBuilder()

    @Synchronized
    override fun write(value: Int) {
        write(byteArrayOf(value.toByte()), 0, 1)
    }

    @Synchronized
    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        if (length <= 0) return

        val text = String(bytes, offset, length, Charsets.UTF_8)
        captured.append(text)
        if (captured.length > MAX_CAPTURED_LOG_CHARS) {
            captured.delete(0, captured.length - MAX_CAPTURED_LOG_CHARS)
        }
        onText(text)
    }

    @Synchronized
    fun lastNonBlankLine(): String? {
        return captured.lineSequence().lastOrNull { it.isNotBlank() }
    }
}

private const val MAX_VISIBLE_LOG_CHARS = 24_000
private const val MAX_CAPTURED_LOG_CHARS = 8_000
private val PROJECT_NAME = Regex("[A-Za-z][A-Za-z0-9._-]*")
private val PACKAGE_NAME = Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*")
