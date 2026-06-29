package org.cosmicide.ui.output

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.cosmicide.common.Prefs
import org.cosmicide.exec.linux.LinuxProcessRunner
import org.cosmicide.extension.setFont
import org.cosmicide.project.Project
import org.cosmicide.rewrite.util.FileUtil
import org.cosmicide.ui.editor.CodeEditor
import org.cosmicide.ui.editor.CodeEditorState
import org.cosmicide.util.ProjectHandler
import org.cosmicide.util.jdksDir
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProjectOutputScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val project = remember {
        ProjectHandler.getProject() ?: throw IllegalStateException("No project set")
    }

    val editorState = remember { CodeEditorState() }
    var isRunning by remember { mutableStateOf(false) }
    var currentProcess by remember { mutableStateOf<Process?>(null) }
    var executionTrigger by remember { mutableIntStateOf(0) }

    val appendOutput: (String) -> Unit = remember(editorState) {
        { logText ->
            editorState.editor?.post {
                val buffer = editorState.editor?.text ?: return@post
                buffer.insert(
                    buffer.lineCount - 1,
                    buffer.getColumnCount(buffer.lineCount - 1),
                    logText
                )
            }
        }
    }

    val killActiveProcess = {
        try {
            currentProcess?.destroyForcibly()
        } catch (_: Exception) {}
        currentProcess = null
        isRunning = false
    }

    LaunchedEffect(executionTrigger) {
        isRunning = true

        editorState.editor?.apply {
            setEditorLanguage(TextMateLanguage.create("source.build", false))
            colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance().currentThemeModel)
            setFont()
        }

        withContext(Dispatchers.IO) {
            try {
                val targetClass = parseTargetClassName(project) ?: run {
                    withContext(Dispatchers.Main) {
                        editorState.editor?.setText("No functional executable classes found in classes.dex")
                    }
                    return@withContext
                }

                val tempDir = context.cacheDir
                val kotlinBuiltin = listOf("kotlin-stdlib", "kotlin-reflect", "kotlin-script-runtime", "kotlinx-coroutines-core-jvm")

                val classpath = mutableListOf(project.binDir.resolve("classes").absolutePath)
                FileUtil.dataDir.resolve("kotlinc/lib/")
                    .listFiles { it.nameWithoutExtension in kotlinBuiltin }
                    ?.forEach { classpath.add(it.absolutePath) }

                project.buildDir.resolve("libs").listFiles()
                    ?.filter { it.extension == "jar" }
                    ?.forEach { classpath.add(it.absolutePath) }

                val javaArgs = mutableListOf(
                    "-Djava.io.tmpdir=${tempDir.absolutePath}",
                    "-Djdk.lang.Process.launchMechanism=FORK",
                    "-Dsun.net.spi.nameservice.provider.1=dns,sun",
                    "-Dnetworkaddress.cache.ttl=0",
                    *(project.runtimeArgs.toTypedArray()),
                    "-cp", classpath.joinToString(":"),
                    targetClass
                ).apply {
                    if (project.args.isNotEmpty()) addAll(project.args)
                }

                val jdkDir = context.jdksDir().resolve("jdk-" + Prefs.currentJDK)

                val runnerConfig = LinuxProcessRunner.Configuration(
                    binary = jdkDir.resolve("bin/java"),
                    arguments = javaArgs,
                    workingDir = project.root
                )

                LinuxProcessRunner.execute(context, runnerConfig, appendOutput) { process ->
                    currentProcess = process
                }

            } catch (e: Exception) {
                appendOutput("\nProcess runtime engine crash: ${e.message}\n")
            } finally {
                withContext(Dispatchers.Main) {
                    killActiveProcess()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Running ${project.name}",
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            killActiveProcess()
                            onNavigateBack()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (isRunning) appendOutput("--- Stopped ---\n")
                            killActiveProcess()
                            executionTrigger++
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload Process")
                        }
                        IconButton(onClick = {
                            killActiveProcess()
                        }) {
                            Icon(Icons.Default.Cancel, contentDescription = "Cancel Operation")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
                HorizontalDivider(thickness = 1.dp)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            CodeEditor(
                modifier = Modifier.weight(1f),
                state = editorState
            )

            if (currentProcess != null) {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(8.dp)
                ) {
                    Text("Memory Usage", style = MaterialTheme.typography.labelSmall)
                    MemoryStatsDashboard(currentProcess!!)
                }
            }
        }
    }
}

@Composable
fun LiveGraph(data: List<Long>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(120.dp).padding(16.dp)) {
        if (data.isEmpty()) return@Canvas

        val maxMem = (data.maxOrNull() ?: 1L).coerceAtLeast(1024L)
        val path = Path()

        val gridColor = Color.Gray.copy(alpha = 0.3f)
        drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(size.width, 0f))
        drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(0f, size.height), end = androidx.compose.ui.geometry.Offset(size.width, size.height))

        data.forEachIndexed { index, value ->
            val x = index.toFloat() / (49f).coerceAtLeast(1f) * size.width
            val y = size.height - (value.toFloat() / maxMem * size.height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path = path, color = Color.Green, style = Stroke(width = 4f))
    }
}

@Composable
fun MemoryStatsDashboard(process: Process) {
    var memoryHistory by remember { mutableStateOf(listOf<Long>()) }
    var currentMem by remember { mutableLongStateOf(0L) }

    LaunchedEffect(process) {
        val pid = LinuxProcessRunner.getNativePid(process)
        withContext(Dispatchers.IO) {
            while (process.isAlive) {
                val mem = LinuxProcessRunner.getResidentMemoryKb(pid)
                currentMem = mem
                memoryHistory = (memoryHistory + mem).takeLast(50)
                delay(1.seconds)
            }
        }
    }

    Column(modifier = Modifier.padding(8.dp)) {
        Text(
            text = "Heap Usage: $currentMem KB",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        LiveGraph(data = memoryHistory, modifier = Modifier.fillMaxWidth().height(80.dp))
    }
}

private fun parseTargetClassName(project: Project): String? {
    val classesDir = project.binDir.resolve("classes")
    if (!classesDir.exists() || !classesDir.isDirectory) return null

    if (ProjectHandler.clazz != null) {
        val requested = ProjectHandler.clazz!!.substringBeforeLast('.')
        ProjectHandler.clazz = null
        return requested
    }

    classesDir.walkTopDown().filter { it.extension == "class" }.forEach { file ->
        val node = ClassNode().apply {
            file.inputStream().use { ClassReader(it.readBytes()).accept(this, ClassReader.SKIP_CODE) }
        }

        val hasMain = node.methods.any { it.name == "main" && it.desc == "([Ljava/lang/String;)V" }

        if (hasMain) {
            return node.name.replace('/', '.')
        }
    }

    return null
}
