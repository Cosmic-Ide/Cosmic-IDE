package org.cosmicide.ui.editor

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.subscribeAlways
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.cascade.CascadeDropdownMenu
import org.cosmicide.build.dex.D8Task
import org.cosmicide.common.Prefs
import org.cosmicide.exec.linux.LinuxProcessRunner
import org.cosmicide.model.EditorViewModel
import org.cosmicide.project.Project
import org.cosmicide.util.ProjectHandler
import org.cosmicide.util.jdksDir
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

sealed class TreeDialogState {
    data class Create(val parent: File, val type: CreateType) : TreeDialogState()
    data class Rename(val target: File) : TreeDialogState()
    data class Delete(val target: File) : TreeDialogState()

    enum class CreateType(val title: String, val suffix: String = "") {
        KOTLIN_CLASS("Create Kotlin Class", ".kt"),
        JAVA_CLASS("Create Java Class", ".java"),
        FOLDER("Create Folder"),
        FILE("Create File")
    }
}

@Composable
fun EditorScreen(
    project: Project,
    onCompile: () -> Unit,
    viewModel: EditorViewModel = viewModel()
) {
    val state = remember { CodeEditorState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val openFiles = viewModel.openFiles
    val activeFile = viewModel.activeFile
    val fileContentCache = viewModel.fileContentCache

    val editor = remember {
        setCodeEditorFactory(context = context, state = state)
    }

    DisposableEffect(project.name) {
        CoroutineScope(Dispatchers.IO).launch {
        runJdtlsProcess(context, project) { process ->
            val pid = LinuxProcessRunner.getNativePid(process)
            while (process.isAlive) {
                val mem = LinuxProcessRunner.getResidentMemoryKb(pid)

                println("JDT mem: " + mem)
                Thread.sleep(5.seconds.inWholeMilliseconds)
            }
        }
        }

        onDispose {
            CoroutineScope(Dispatchers.IO).launch {
                stopJdtlsProcess()
            }
        }
    }

    val switchTab = { newFile: File ->
        activeFile?.let { file ->
            viewModel.updateCache(file, editor.text.toString())
        }
        viewModel.switchTab(newFile, editor.text.toString())
    }

    LaunchedEffect(activeFile) {
        activeFile?.let { file ->
            val content = fileContentCache[file] ?: withContext(Dispatchers.IO) { file.readText() }
            viewModel.updateCache(file, content)
            editor.setText(content)
            editor.applyEditorSettings(file)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Project Explorer",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider()
                ProjectTreeView(
                    rootDir = project.root,
                    onFileClick = { file ->
                        switchTab(file)
                        scope.launch { drawerState.close() }
                    },
                    onExecuteFile = { file ->
                        val executionPath =
                            file.absolutePath.replace(project.srcDir.absolutePath + "/", "")
                        ProjectHandler.clazz = executionPath.substringBeforeLast('.')
                        onCompile()
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    EditorToolbar(
                        project = project,
                        file = activeFile,
                        editor = editor,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onCompile = onCompile
                    )

                    if (openFiles.isNotEmpty()) {
                        PrimaryScrollableTabRow(
                            selectedTabIndex = openFiles.indexOf(activeFile).coerceAtLeast(0),
                            edgePadding = 8.dp,
                            containerColor = MaterialTheme.colorScheme.surface,
                            divider = { HorizontalDivider() }
                        ) {
                            openFiles.forEach { file ->
                                val isSelected = file == activeFile
                                Tab(
                                    selected = isSelected,
                                    onClick = { switchTab(file) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 10.dp
                                        )
                                    ) {
                                        Text(
                                            file.name,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                viewModel.closeTab(file)
                                            },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Close Tab",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(thickness = 1.dp)
                    }
                }
            }
        ) { innerPadding ->
            Box(
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (activeFile != null) {
                    TextEditorContent(editor)
                } else {
                    EmptyWorkspaceState(onOpenDrawer = { scope.launch { drawerState.open() } })
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectTreeView(
    rootDir: File,
    onFileClick: (File) -> Unit,
    onExecuteFile: (File) -> Unit
) {
    val context = LocalContext.current
    var expandedDirs by remember { mutableStateOf(setOf(rootDir)) }

    var contextMenuFile by remember { mutableStateOf<File?>(null) }
    var activeDialog by remember { mutableStateOf<TreeDialogState?>(null) }
    var treeTrigger by remember { mutableIntStateOf(0) }

    val toggleExpand = { dir: File ->
        if (expandedDirs.contains(dir)) {
            expandedDirs = expandedDirs - dir
        } else {
            val toExpand = mutableSetOf(dir)
            var current = dir

            while (true) {
                val kids = current.listFiles()
                if (kids != null && kids.size == 1 && kids[0].isDirectory) {
                    current = kids[0]
                    toExpand.add(current)
                } else {
                    break
                }
            }
            expandedDirs = expandedDirs + toExpand
        }
    }

    val treeItems = remember(expandedDirs, rootDir, treeTrigger) {
        val list = mutableListOf<Pair<File, Int>>()
        fun traverse(dir: File, depth: Int) {
            val children =
                dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: return
            for (child in children) {
                list.add(child to depth)
                if (child.isDirectory && expandedDirs.contains(child)) {
                    traverse(child, depth + 1)
                }
            }
        }
        traverse(rootDir, 0)
        list
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(treeItems, key = { it.first.absolutePath }) { (file, depth) ->
            val isExpanded = expandedDirs.contains(file)

            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (file.isDirectory) toggleExpand(file) else onFileClick(file)
                            },
                            onLongClick = {
                                contextMenuFile = file
                            }
                        )
                        .padding(vertical = 10.dp, horizontal = 16.dp)
                        .padding(start = (depth * 16).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (file.isDirectory) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(18.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                        contentDescription = null,
                        tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (contextMenuFile == file) {
                    CascadeDropdownMenu(
                        expanded = true,
                        onDismissRequest = { contextMenuFile = null }
                    ) {
                        if (file.isDirectory) {
                            // Nesting creation options inside a fluid sub-menu
                            DropdownMenuItem(
                                text = { Text("New...") },
                                children = {
                                    DropdownMenuItem(text = { Text("Kotlin Class") }, onClick = {
                                        activeDialog = TreeDialogState.Create(
                                            file,
                                            TreeDialogState.CreateType.KOTLIN_CLASS
                                        )
                                        contextMenuFile = null
                                    })
                                    DropdownMenuItem(text = { Text("Java Class") }, onClick = {
                                        activeDialog = TreeDialogState.Create(
                                            file,
                                            TreeDialogState.CreateType.JAVA_CLASS
                                        )
                                        contextMenuFile = null
                                    })
                                    HorizontalDivider()
                                    DropdownMenuItem(text = { Text("Folder") }, onClick = {
                                        activeDialog = TreeDialogState.Create(
                                            file,
                                            TreeDialogState.CreateType.FOLDER
                                        )
                                        contextMenuFile = null
                                    })
                                    DropdownMenuItem(text = { Text("File") }, onClick = {
                                        activeDialog = TreeDialogState.Create(
                                            file,
                                            TreeDialogState.CreateType.FILE
                                        )
                                        contextMenuFile = null
                                    })
                                }
                            )
                        } else {
                            if (file.extension == "kt" || file.extension == "java") {
                                DropdownMenuItem(text = { Text("Execute") }, onClick = {
                                    onExecuteFile(file)
                                    contextMenuFile = null
                                })
                            }
                            if (file.extension == "jar") {
                                DropdownMenuItem(text = { Text("DEX") }, onClick = {
                                    val dex =
                                        file.resolveSibling("${file.nameWithoutExtension}.dex")
                                    dex.createNewFile()
                                    D8Task.compileJar(file.toPath(), dex.parentFile!!.toPath())
                                    treeTrigger++
                                    contextMenuFile = null
                                })
                            }
                            DropdownMenuItem(text = { Text("Open External") }, onClick = {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "*/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open with"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                contextMenuFile = null
                            })
                        }

                        HorizontalDivider()

                        DropdownMenuItem(text = { Text("Rename") }, onClick = {
                            activeDialog = TreeDialogState.Rename(file)
                            contextMenuFile = null
                        })
                        DropdownMenuItem(text = {
                            Text(
                                "Delete",
                                color = MaterialTheme.colorScheme.error
                            )
                        }, onClick = {
                            activeDialog = TreeDialogState.Delete(file)
                            contextMenuFile = null
                        })
                    }
                }
            }
        }
    }

    when (val state = activeDialog) {
        is TreeDialogState.Create -> {
            TreeInputDialog(
                title = state.type.title,
                initialValue = "",
                suffix = state.type.suffix,
                onDismiss = { activeDialog = null },
                onConfirm = { name ->
                    val cleanName = name.replace("\\.", "") + state.type.suffix
                    val newTarget = state.parent.resolve(cleanName)
                    if (state.type == TreeDialogState.CreateType.FOLDER) {
                        newTarget.mkdirs()
                    } else {
                        newTarget.createNewFile()
                    }
                    if (state.parent.isDirectory && !expandedDirs.contains(state.parent)) {
                        toggleExpand(state.parent)
                    }
                    treeTrigger++
                    activeDialog = null
                }
            )
        }

        is TreeDialogState.Rename -> {
            TreeInputDialog(
                title = "Rename",
                initialValue = state.target.name,
                onDismiss = { activeDialog = null },
                onConfirm = { name ->
                    val cleanName = name.replace("\\.", "")
                    state.target.renameTo(state.target.parentFile!!.resolve(cleanName))
                    treeTrigger++
                    activeDialog = null
                }
            )
        }

        is TreeDialogState.Delete -> {
            AlertDialog(
                onDismissRequest = { activeDialog = null },
                title = { Text("Delete") },
                text = { Text("Are you sure you want to delete ${state.target.name}?") },
                confirmButton = {
                    TextButton(onClick = {
                        state.target.deleteRecursively()
                        treeTrigger++
                        activeDialog = null
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialog = null }) { Text("Cancel") }
                }
            )
        }

        null -> {}
    }
}

@Composable
fun TreeInputDialog(
    title: String,
    initialValue: String,
    suffix: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Name") },
                suffix = if (suffix.isNotEmpty()) {
                    { Text(suffix) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                enabled = text.isNotBlank(),
                shapes = ButtonDefaults.shapes()
            ) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EmptyWorkspaceState(onOpenDrawer: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Code,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Text(
            text = "No file open",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        )
        Button(onClick = onOpenDrawer, shapes = ButtonDefaults.shapes()) {
            Text("Open Project Explorer")
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalTime::class
)
@Composable
fun EditorToolbar(
    project: Project,
    file: File?,
    editor: CodeEditor,
    onOpenDrawer: () -> Unit,
    onCompile: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showGoToLineDialog by remember { mutableStateOf(false) }
    var showProgramArgsDialog by remember { mutableStateOf(false) }
    var showJREArgsDialog by remember { mutableStateOf(false) }
    var showCustomCommandDialog by remember { mutableStateOf(false) }

    val undoManager = editor.text.undoManager
    var canUndo by remember { mutableStateOf(undoManager.canUndo()) }
    var canRedo by remember { mutableStateOf(undoManager.canRedo()) }

    var savedContentHash by remember { mutableIntStateOf(0) }
    var editorContentHash by remember { mutableIntStateOf(0) }

    LaunchedEffect(file) {
        if (file != null && file.exists()) {
            withContext(Dispatchers.IO) {
                savedContentHash = file.readBytes().contentHashCode()
                editorContentHash = savedContentHash
            }
        }
    }

    editor.subscribeAlways<ContentChangeEvent> {
        canUndo = undoManager.canUndo()
        canRedo = undoManager.canRedo()
        editorContentHash = editor.text.toString().toByteArray().contentHashCode()
    }

    TopAppBar(
        title = {
            Text(
                text = file?.name ?: "Cosmic IDE",
                style = MaterialTheme.typography.titleMediumEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
            }
        },
        actions = {
            IconButton(onClick = onCompile) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Compile",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = {
                editor.undo()
                canUndo = undoManager.canUndo()
                canRedo = undoManager.canRedo()
                editorContentHash = editor.text.toString().toByteArray().contentHashCode()
            }, enabled = file != null) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
            }
            IconButton(onClick = {
                editor.redo()
                canUndo = undoManager.canUndo()
                canRedo = undoManager.canRedo()
                editorContentHash = editor.text.toString().toByteArray().contentHashCode()
            }, enabled = file != null) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
            }

            Box {
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Editor Options")
                }

                CascadeDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Execution") },
                        children = {
                            DropdownMenuItem(text = { Text("Program Arguments") }, onClick = {
                                showProgramArgsDialog = true
                                showMenu = false
                            })
                            DropdownMenuItem(text = { Text("Runtime Arguments") }, onClick = {
                                showJREArgsDialog = true
                                showMenu = false
                            })
                            DropdownMenuItem(text = { Text("Custom Command") }, onClick = {
                                showCustomCommandDialog = true
                                showMenu = false
                            })
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Navigation") },
                        children = {
                            DropdownMenuItem(text = { Text("Go To Line") }, onClick = {
                                showGoToLineDialog = true
                                showMenu = false
                            })
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("File Options") },
                        children = {
                            DropdownMenuItem(text = { Text("View Statistics") }, onClick = {
                                showMenu = false
                            })
                        }
                    )
                    DropdownMenuItem(text = { Text("Settings") }, onClick = {
                        showMenu = false
                    })
                }
            }
        }
    )

    if (showGoToLineDialog) {
        GoToLineDialog(
            lineCount = editor.lineCount,
            onDismiss = { showGoToLineDialog = false },
            onConfirm = { lineNumber ->
                editor.jumpToLine(lineNumber - 1)
                showGoToLineDialog = false
            }
        )
    }

    if (showProgramArgsDialog) {
        ProgramArgumentDialog(
            title = "Program Arguments",
            savedArgs = project.args,
            onSave = { args ->
                project.args = args
            },
            onDismiss = {
                showProgramArgsDialog = false
            }
        )
    }

    if (showJREArgsDialog) {
        ProgramArgumentDialog(
            title = "Runtime Arguments",
            savedArgs = project.runtimeArgs,
            onSave = { args ->
                project.runtimeArgs = args
            },
            onDismiss = {
                showJREArgsDialog = false
            }
        )
    }

    if (showCustomCommandDialog) {
        CustomCommandDialog(
            project = project,
            onDismiss = { showCustomCommandDialog = false }
        )
    }
}

@Composable
fun GoToLineDialog(lineCount: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go to Line") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { char -> char.isDigit() } },
                label = { Text("Line number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
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
                },
                shapes = ButtonDefaults.shapes()
            ) {
                Text("Go")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CustomCommandDialog(
    project: Project,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val outputScrollState = rememberScrollState()

    var commandInput by remember { mutableStateOf("") }
    var outputLog by remember { mutableStateOf("Ready in ${project.root.absolutePath}\n") }
    var isRunning by remember { mutableStateOf(false) }
    var currentProcess by remember { mutableStateOf<Process?>(null) }

    fun appendOutput(chunk: String) {
        scope.launch(Dispatchers.Main) {
            outputLog += chunk
        }
    }

    LaunchedEffect(outputLog) {
        outputScrollState.scrollTo(outputScrollState.maxValue)
    }

    DisposableEffect(currentProcess) {
        onDispose {
            currentProcess?.takeIf { it.isAlive }?.destroyForcibly()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isRunning) onDismiss() },
        title = { Text("Custom Command") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    label = { Text("Command") },
                    placeholder = { Text("java -version") },
                    singleLine = true,
                    enabled = !isRunning,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.size(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 320.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(12.dp)
                        .verticalScroll(outputScrollState)
                ) {
                    SelectionContainer {
                        Text(
                            text = outputLog,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration =
                        )
                    }
                }

                if (isRunning) {
                    Spacer(modifier = Modifier.size(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val submittedCommand = commandInput.trim()
                    if (submittedCommand.isEmpty() || isRunning) return@Button

                    isRunning = true
                    outputLog = "Executing: $submittedCommand\n\n"

                    scope.launch(Dispatchers.IO) {
                        try {
                            val jdkDir = context.jdksDir().resolve("jdk-" + Prefs.currentJDK)
                            val pathEntries = LinuxProcessRunner.toolchainPathEntries(context, jdkDir)
                            val commandParts = LinuxProcessRunner.parseCommandLine(submittedCommand)
                            val binary = LinuxProcessRunner.resolveExecutable(
                                commandName = commandParts.first(),
                                workingDir = project.root,
                                pathEntries = pathEntries
                            )
                            val tempDir = context.cacheDir
                            val runnerConfig = LinuxProcessRunner.Configuration(
                                binary = binary,
                                arguments = commandParts.drop(1),
                                workingDir = project.root,
                                environmentOverrides = LinuxProcessRunner.toolchainEnvironment(
                                    context,
                                    jdkDir
                                ) + mapOf(
                                    "TMPDIR" to tempDir.absolutePath,
                                    "TMP" to tempDir.absolutePath,
                                    "TEMP" to tempDir.absolutePath
                                ),
                                pathEntries = pathEntries
                            )

                            LinuxProcessRunner.execute(
                                context = context,
                                config = runnerConfig,
                                onOutputReceived = ::appendOutput,
                                onProcessStarted = { process ->
                                    scope.launch(Dispatchers.Main) {
                                        currentProcess = process
                                    }
                                }
                            )
                        } catch (e: Exception) {
                            appendOutput("\nExecution failed: ${e.message}\n")
                        } finally {
                            withContext(Dispatchers.Main) {
                                currentProcess = null
                                isRunning = false
                            }
                        }
                    }
                },
                enabled = commandInput.isNotBlank() && !isRunning,
                shapes = ButtonDefaults.shapes()
            ) {
                Text("Run")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isRunning
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ProgramArgumentDialog(
    title: String,
    savedArgs: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var args by remember { mutableStateOf(savedArgs.joinToString(" ")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = args,
                label = { Text(title) },
                singleLine = true,
                onValueChange = { args = it }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(args.split(' '))
                    onDismiss()
                },
                shapes = ButtonDefaults.shapes()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Statistics(content: Content, onDismiss: () -> Unit) {
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

@Composable
private fun TextEditorContent(editor: CodeEditor) {
    var charset by remember { mutableStateOf("UTF-8") }
    var position by remember { mutableStateOf("1:1") }
    var statsDialogShown by remember { mutableStateOf(false) }

    editor.subscribeAlways<SelectionChangeEvent> { event ->
        position = event.left.let {
            "${it.line + 1}:${it.column}"
        }
    }
    Column {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = position,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily.Monospace
            )

            Text(
                modifier = Modifier
                    .weight(1f)
                    .clickable(true) {
                        statsDialogShown = true
                    },
                text = charset,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End
            )
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { editor },
            onRelease = {
                editor.release()
            }
        )
    }

    if (statsDialogShown) {
        Statistics(editor.text) {
            statsDialogShown = false
        }
    }
}
