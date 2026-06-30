package org.cosmicide.ui.output

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
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
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds
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
    val jdkDir = remember { context.jdksDir().resolve("jdk-" + Prefs.currentJDK) }

    val editorState = remember { CodeEditorState() }
    var isRunning by remember { mutableStateOf(false) }
    var currentProcess by remember { mutableStateOf<Process?>(null) }
    var executionTrigger by remember { mutableIntStateOf(0) }
    var isInstrumentationExpanded by remember { mutableStateOf(true) }

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
        } catch (_: Exception) {
        }
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
                val kotlinBuiltin = listOf(
                    "kotlin-stdlib",
                    "kotlin-reflect",
                    "kotlin-script-runtime",
                    "kotlinx-coroutines-core-jvm"
                )

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
                    "-XX:+UsePerfData",
                    *(project.runtimeArgs.toTypedArray()),
                    "-cp",
                    classpath.joinToString(":"),
                    targetClass
                ).apply {
                    if (project.args.isNotEmpty()) addAll(project.args)
                }

                val runnerConfig = LinuxProcessRunner.Configuration(
                    binary = jdkDir.resolve("bin/java"),
                    arguments = javaArgs,
                    workingDir = project.root,
                    environmentOverrides = mapOf(
                        "TMPDIR" to tempDir.absolutePath,
                        "TMP" to tempDir.absolutePath,
                        "TEMP" to tempDir.absolutePath
                    )
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
                        IconButton(
                            onClick = {
                                killActiveProcess()
                                onNavigateBack()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (isRunning) appendOutput("--- Stopped ---\n")
                                killActiveProcess()
                                executionTrigger++
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload Process")
                        }
                        IconButton(onClick = { killActiveProcess() }) {
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
                MemoryInstrumentationPane(
                    context = context,
                    process = currentProcess!!,
                    jdkDir = jdkDir,
                    workingDir = project.root,
                    expanded = isInstrumentationExpanded,
                    onToggleExpanded = { isInstrumentationExpanded = !isInstrumentationExpanded }
                )
            }
        }
    }
}

@Composable
fun LiveGraph(
    series: List<GraphSeries>,
    modifier: Modifier = Modifier,
    maxValue: Long? = null
) {
    Canvas(modifier = modifier.fillMaxWidth().height(120.dp).padding(16.dp)) {
        val allValues = series.flatMap { it.data }
        if (allValues.isEmpty()) return@Canvas

        val graphMax = (maxValue ?: (allValues.maxOrNull() ?: 1L)).coerceAtLeast(1L)

        val gridColor = Color.Gray.copy(alpha = 0.3f)
        drawLine(
            gridColor,
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width, 0f)
        )
        drawLine(
            gridColor,
            start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f)
        )
        drawLine(
            gridColor,
            start = androidx.compose.ui.geometry.Offset(0f, size.height),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height)
        )

        series.forEach { graphSeries ->
            val path = Path()

            graphSeries.data.forEachIndexed { index, value ->
                val maxIndex = (graphSeries.data.size - 1).coerceAtLeast(1).toFloat()
                val x = index.toFloat() / maxIndex * size.width
                val y = size.height - (value.toFloat() / graphMax.toFloat() * size.height)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = graphSeries.color,
                style = Stroke(width = 4f)
            )
        }
    }
}

@Composable
fun MemoryInstrumentationPane(
    context: Context,
    process: Process,
    jdkDir: File,
    workingDir: File,
    expanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    var selectedJstatViewMode by remember { mutableStateOf(JstatViewMode.Cause) }
    var rawSourceJstatViewMode by remember { mutableStateOf(JstatViewMode.Cause) }

    val activeSamplerMode = if (selectedJstatViewMode == JstatViewMode.Raw) {
        rawSourceJstatViewMode
    } else {
        selectedJstatViewMode
    }

    var residentMemoryHistory by remember { mutableStateOf(listOf<Long>()) }
    var edenHistory by remember { mutableStateOf(listOf<Long>()) }
    var oldHistory by remember { mutableStateOf(listOf<Long>()) }
    var metaspaceHistory by remember { mutableStateOf(listOf<Long>()) }
    var allocationRateHistory by remember { mutableStateOf(listOf<Long>()) }
    var loadedClassHistory by remember { mutableStateOf(listOf<Long>()) }

    var currentResidentKb by remember { mutableLongStateOf(0L) }
    var currentHeapSample by remember { mutableStateOf<JstatHeapSample?>(null) }
    var previousHeapSample by remember { mutableStateOf<JstatHeapSample?>(null) }
    var currentClassSample by remember { mutableStateOf<JstatClassSample?>(null) }

    var rawJstatLines by remember { mutableStateOf(listOf<String>()) }

    var currentAllocationRateKbPerSec by remember { mutableDoubleStateOf(0.0) }
    var currentGcOverheadPercent by remember { mutableDoubleStateOf(0.0) }

    var monitoredPid by remember { mutableIntStateOf(-1) }
    var jvmPid by remember { mutableIntStateOf(-1) }
    var samplerStatus by remember { mutableStateOf("Resolving JVM process...") }
    var classSamplerStatus by remember { mutableStateOf("Waiting for class sampler...") }

    val activeHeapJstatProcess = remember { AtomicReference<Process?>(null) }
    val activeClassJstatProcess = remember { AtomicReference<Process?>(null) }

    val heapSamplerGeneration = remember { AtomicLong(0L) }
    val classSamplerGeneration = remember { AtomicLong(0L) }

    DisposableEffect(process) {
        onDispose {
            heapSamplerGeneration.incrementAndGet()
            classSamplerGeneration.incrementAndGet()

            activeHeapJstatProcess.getAndSet(null)?.destroyForcibly()
            activeClassJstatProcess.getAndSet(null)?.destroyForcibly()
        }
    }

    LaunchedEffect(samplerStatus) {
        Log.d("jstat", samplerStatus)
    }

    LaunchedEffect(classSamplerStatus) {
        Log.d("jstat-class", classSamplerStatus)
    }

    LaunchedEffect(process) {
        val launcherPid = LinuxProcessRunner.getNativePid(process)
        if (launcherPid == -1) {
            samplerStatus = "Unable to resolve process id"
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            while (process.isAlive) {
                val pid = LinuxProcessRunner.getBestProcessMemoryPid(process)
                    .takeIf { it != -1 }
                    ?: launcherPid

                monitoredPid = pid

                val resolvedJvmPid = LinuxProcessRunner.getJvmPid(process)
                if (resolvedJvmPid != -1) {
                    jvmPid = resolvedJvmPid
                }

                val mem = LinuxProcessRunner.getResidentMemoryKb(pid)
                currentResidentKb = mem
                residentMemoryHistory = (residentMemoryHistory + mem).takeLast(50)

                delay(1.seconds)
            }
        }
    }

    LaunchedEffect(process, context, jdkDir, workingDir, activeSamplerMode) {
        val generation = heapSamplerGeneration.incrementAndGet()

        activeHeapJstatProcess.getAndSet(null)?.destroyForcibly()

        currentHeapSample = null
        previousHeapSample = null
        rawJstatLines = emptyList()
        edenHistory = emptyList()
        oldHistory = emptyList()
        metaspaceHistory = emptyList()
        allocationRateHistory = emptyList()
        currentAllocationRateKbPerSec = 0.0
        currentGcOverheadPercent = 0.0

        try {
            withContext(Dispatchers.IO) {
                while (process.isAlive && heapSamplerGeneration.get() == generation) {
                    val candidates = LinuxProcessRunner.getJvmPidCandidates(process)
                    if (candidates.isEmpty()) {
                        if (heapSamplerGeneration.get() == generation) {
                            samplerStatus = "Waiting for JVM process..."
                        }
                        delay(250.milliseconds)
                        continue
                    }

                    var emittedSample = false
                    var lastFailure = "jstat ${activeSamplerMode.option} did not emit heap data"

                    candidates.forEach { pid ->
                        if (!process.isAlive || emittedSample || heapSamplerGeneration.get() != generation) {
                            return@forEach
                        }

                        jvmPid = pid
                        samplerStatus = "Waiting for jstat ${activeSamplerMode.option} data for pid $pid..."

                        val failure = sampleJstatHeap(
                            context = context,
                            jdkDir = jdkDir,
                            workingDir = workingDir,
                            targetProcess = process,
                            pid = pid,
                            viewMode = activeSamplerMode,
                            onProcessStarted = { samplerProcess ->
                                activeHeapJstatProcess
                                    .getAndSet(samplerProcess)
                                    ?.destroyForcibly()
                            },
                            onProcessFinished = { samplerProcess ->
                                activeHeapJstatProcess.compareAndSet(samplerProcess, null)
                            },
                            onRawLine = { rawLine ->
                                if (heapSamplerGeneration.get() == generation) {
                                    rawJstatLines = (rawJstatLines + rawLine).takeLast(18)
                                }
                            }
                        ) { sample ->
                            if (heapSamplerGeneration.get() == generation) {
                                emittedSample = true

                                val previous = previousHeapSample
                                currentAllocationRateKbPerSec = estimateAllocationRateKbPerSec(previous, sample)
                                currentGcOverheadPercent = estimateGcOverheadPercent(previous, sample)

                                previousHeapSample = sample
                                currentHeapSample = sample

                                edenHistory = (edenHistory + sample.edenGraphValue).takeLast(50)
                                oldHistory = (oldHistory + sample.oldGraphValue).takeLast(50)
                                metaspaceHistory = (metaspaceHistory + sample.metaspaceGraphValue).takeLast(50)
                                allocationRateHistory = (
                                        allocationRateHistory + currentAllocationRateKbPerSec.roundToLong()
                                        ).takeLast(50)

                                samplerStatus =
                                    "Sampling ${activeSamplerMode.label} with jstat ${activeSamplerMode.option} for pid $pid"
                            }
                        }

                        if (!emittedSample && failure.isNotBlank()) {
                            lastFailure = failure
                        }
                    }

                    if (!emittedSample && process.isAlive && heapSamplerGeneration.get() == generation) {
                        samplerStatus = lastFailure
                        delay(500.milliseconds)
                    }
                }
            }
        } finally {
            activeHeapJstatProcess.getAndSet(null)?.destroyForcibly()
        }
    }

    LaunchedEffect(process, context, jdkDir, workingDir) {
        val generation = classSamplerGeneration.incrementAndGet()

        activeClassJstatProcess.getAndSet(null)?.destroyForcibly()

        try {
            withContext(Dispatchers.IO) {
                while (process.isAlive && classSamplerGeneration.get() == generation) {
                    val candidates = LinuxProcessRunner.getJvmPidCandidates(process)
                    if (candidates.isEmpty()) {
                        if (classSamplerGeneration.get() == generation) {
                            classSamplerStatus = "Waiting for JVM process..."
                        }
                        delay(250.milliseconds)
                        continue
                    }

                    var emittedSample = false
                    var lastFailure = "jstat -class did not emit class data"

                    candidates.forEach { pid ->
                        if (!process.isAlive || emittedSample || classSamplerGeneration.get() != generation) {
                            return@forEach
                        }

                        jvmPid = pid
                        classSamplerStatus = "Waiting for jstat -class data for pid $pid..."

                        val failure = sampleJstatClasses(
                            context = context,
                            jdkDir = jdkDir,
                            workingDir = workingDir,
                            targetProcess = process,
                            pid = pid,
                            onProcessStarted = { samplerProcess ->
                                activeClassJstatProcess
                                    .getAndSet(samplerProcess)
                                    ?.destroyForcibly()
                            },
                            onProcessFinished = { samplerProcess ->
                                activeClassJstatProcess.compareAndSet(samplerProcess, null)
                            }
                        ) { sample ->
                            if (classSamplerGeneration.get() == generation) {
                                emittedSample = true
                                currentClassSample = sample
                                loadedClassHistory = (loadedClassHistory + sample.loadedClasses).takeLast(50)
                                classSamplerStatus = "Sampling class loading with jstat -class for pid $pid"
                            }
                        }

                        if (!emittedSample && failure.isNotBlank()) {
                            lastFailure = failure
                        }
                    }

                    if (!emittedSample && process.isAlive && classSamplerGeneration.get() == generation) {
                        classSamplerStatus = lastFailure
                        delay(500.milliseconds)
                    }
                }
            }
        } finally {
            activeClassJstatProcess.getAndSet(null)?.destroyForcibly()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "JVM Instrumentation",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = samplerStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onToggleExpanded) {
                Icon(
                    imageVector = if (expanded) {
                        Icons.Default.KeyboardArrowDown
                    } else {
                        Icons.Default.KeyboardArrowUp
                    },
                    contentDescription = if (expanded) {
                        "Collapse JVM instrumentation"
                    } else {
                        "Expand JVM instrumentation"
                    }
                )
            }
        }

        if (expanded) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                divider = { HorizontalDivider() },
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Process") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Heap") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("GC") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Runtime") }
                )
            }

            JstatModeSelector(
                selectedMode = selectedJstatViewMode,
                activeSamplerMode = activeSamplerMode,
                onModeSelected = { mode ->
                    selectedJstatViewMode = mode
                    if (mode != JstatViewMode.Raw) {
                        rawSourceJstatViewMode = mode
                    }
                }
            )

            when (selectedTab) {
                0 -> ProcessMemoryTab(
                    currentResidentKb = currentResidentKb,
                    residentMemoryHistory = residentMemoryHistory,
                    monitoredPid = monitoredPid
                )

                1 -> {
                    if (selectedJstatViewMode == JstatViewMode.Raw) {
                        RawJstatTab(
                            activeSamplerMode = activeSamplerMode,
                            rawJstatLines = rawJstatLines
                        )
                    } else {
                        JvmHeapMemoryTab(
                            currentHeapSample = currentHeapSample,
                            edenHistory = edenHistory,
                            oldHistory = oldHistory,
                            currentResidentKb = currentResidentKb,
                            jvmPid = jvmPid
                        )
                    }
                }

                2 -> {
                    if (selectedJstatViewMode == JstatViewMode.Raw) {
                        RawJstatTab(
                            activeSamplerMode = activeSamplerMode,
                            rawJstatLines = rawJstatLines
                        )
                    } else {
                        JvmGcTab(
                            currentHeapSample = currentHeapSample,
                            currentAllocationRateKbPerSec = currentAllocationRateKbPerSec,
                            currentGcOverheadPercent = currentGcOverheadPercent,
                            allocationRateHistory = allocationRateHistory,
                            jvmPid = jvmPid
                        )
                    }
                }

                3 -> JvmRuntimeTab(
                    currentHeapSample = currentHeapSample,
                    currentClassSample = currentClassSample,
                    metaspaceHistory = metaspaceHistory,
                    loadedClassHistory = loadedClassHistory,
                    jvmPid = jvmPid,
                    classSamplerStatus = classSamplerStatus,
                    selectedJstatViewMode = selectedJstatViewMode,
                    activeSamplerMode = activeSamplerMode,
                    rawJstatLines = rawJstatLines
                )
            }
        }
    }
}

@Composable
private fun JstatModeSelector(
    selectedMode: JstatViewMode,
    activeSamplerMode: JstatViewMode,
    onModeSelected: (JstatViewMode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            JstatViewMode.entries.forEach { mode ->
                TextButton(
                    onClick = { onModeSelected(mode) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = when {
                            mode == JstatViewMode.Raw && selectedMode == mode -> {
                                "● Raw"
                            }

                            mode == JstatViewMode.Raw -> {
                                "Raw"
                            }

                            selectedMode == mode -> {
                                "● ${mode.label}"
                            }

                            else -> {
                                mode.label
                            }
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessMemoryTab(
    currentResidentKb: Long,
    residentMemoryHistory: List<Long>,
    monitoredPid: Int
) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        Text(
            text = "Current process memory: ${formatKb(currentResidentKb)}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Resident memory from /proc${
                monitoredPid.takeIf { it != -1 }?.let { "/$it" }.orEmpty()
            } for the resolved runtime process.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Legend("RSS", MaterialTheme.colorScheme.primary)
        }

        LiveGraph(
            series = listOf(
                GraphSeries("RSS", residentMemoryHistory, MaterialTheme.colorScheme.primary)
            ),
            modifier = Modifier.fillMaxWidth().height(92.dp)
        )
    }
}

@Composable
private fun JvmHeapMemoryTab(
    currentHeapSample: JstatHeapSample?,
    edenHistory: List<Long>,
    oldHistory: List<Long>,
    currentResidentKb: Long,
    jvmPid: Int
) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        currentHeapSample?.let { sample ->
            when (sample.layout) {
                JstatSampleLayout.MemoryKb -> {
                    Text(
                        text = "Heap sizes from jstat -gc.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricText("Eden", formatKb(sample.edenUsedKb))
                        MetricText("Old", formatKb(sample.oldUsedKb))
                        MetricText("Heap", formatKb(sample.liveHeapUsedKb))
                        MetricText("RSS", formatKb(currentResidentKb))
                    }

                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        UsageBarKb(
                            label = "Eden",
                            usedKb = sample.edenUsedKb,
                            capacityKb = sample.edenCapacityKb,
                            color = Color(0xFF43A047)
                        )
                        UsageBarKb(
                            label = "Old",
                            usedKb = sample.oldUsedKb,
                            capacityKb = sample.oldCapacityKb,
                            color = Color(0xFFFFA000)
                        )
                        UsageBarKb(
                            label = "Committed Java heap",
                            usedKb = sample.liveHeapUsedKb,
                            capacityKb = sample.committedHeapCapacityKb,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Eden + Survivor + Old represents active Java object heap utilization${
                            jvmPid.takeIf { it != -1 }?.let { " for JVM pid $it" }.orEmpty()
                        }.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                JstatSampleLayout.UtilizationPercent -> {
                    Text(
                        text = if (sample.hasGcCause) {
                            "Utilization percentages and GC causes from jstat -gccause."
                        } else {
                            "Utilization percentages from jstat -gcutil."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricText("Eden", formatPercent(sample.edenUtilPercent))
                        MetricText("Old", formatPercent(sample.oldUtilPercent))
                        MetricText("S0", formatPercent(sample.survivor0UtilPercent))
                        MetricText("S1", formatPercent(sample.survivor1UtilPercent))
                    }

                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        UsageBarPercent(
                            label = "Eden",
                            percent = sample.edenUtilPercent,
                            color = Color(0xFF43A047)
                        )
                        UsageBarPercent(
                            label = "Old",
                            percent = sample.oldUtilPercent,
                            color = Color(0xFFFFA000)
                        )
                        UsageBarPercent(
                            label = "Survivor 0",
                            percent = sample.survivor0UtilPercent,
                            color = MaterialTheme.colorScheme.primary
                        )
                        UsageBarPercent(
                            label = "Survivor 1",
                            percent = sample.survivor1UtilPercent,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Legend(
                    label = if (sample.layout == JstatSampleLayout.MemoryKb) "Eden KB" else "Eden %",
                    color = Color(0xFF43A047)
                )
                Legend(
                    label = if (sample.layout == JstatSampleLayout.MemoryKb) "Old KB" else "Old %",
                    color = Color(0xFFFFA000)
                )
            }

            LiveGraph(
                series = listOf(
                    GraphSeries("Eden", edenHistory, Color(0xFF43A047)),
                    GraphSeries("Old", oldHistory, Color(0xFFFFA000))
                ),
                modifier = Modifier.fillMaxWidth().height(92.dp),
                maxValue = if (sample.layout == JstatSampleLayout.UtilizationPercent) 100L else null
            )
        } ?: Text(
            text = "Waiting for JVM heap samples...",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun JvmGcTab(
    currentHeapSample: JstatHeapSample?,
    currentAllocationRateKbPerSec: Double,
    currentGcOverheadPercent: Double,
    allocationRateHistory: List<Long>,
    jvmPid: Int
) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        Text(
            text = "GC pressure shows whether heap usage is actually causing collection churn.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        currentHeapSample?.let { sample ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricText("Young GC", sample.youngGcCount.toString())
                MetricText("YGCT", formatSeconds(sample.youngGcTimeSeconds))
                MetricText("Full GC", sample.fullGcCount.toString())
                MetricText("FGCT", formatSeconds(sample.fullGcTimeSeconds))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricText("Total GC", formatSeconds(sample.totalGcTimeSeconds))
                MetricText("GC overhead", formatPercent(currentGcOverheadPercent))
                MetricText(
                    label = "Alloc rate",
                    value = if (sample.layout == JstatSampleLayout.MemoryKb) {
                        formatKbPerSec(currentAllocationRateKbPerSec)
                    } else {
                        "n/a"
                    }
                )
            }

            if (sample.concurrentGcCount > 0L || sample.concurrentGcTimeSeconds > 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricText("Concurrent GC", sample.concurrentGcCount.toString())
                    MetricText("CGCT", formatSeconds(sample.concurrentGcTimeSeconds))
                }
            }

            Text(
                text = "Cause: ${sample.rawGcCause ?: "not available in this mode"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = jvmPid.takeIf { it != -1 }
                    ?.let { "Sampling JVM pid $it." }
                    ?: "Resolving JVM pid...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (sample.layout == JstatSampleLayout.MemoryKb) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Legend("Allocation rate", MaterialTheme.colorScheme.primary)
                }

                LiveGraph(
                    series = listOf(
                        GraphSeries(
                            label = "Allocation rate",
                            data = allocationRateHistory,
                            color = MaterialTheme.colorScheme.primary
                        )
                    ),
                    modifier = Modifier.fillMaxWidth().height(92.dp)
                )
            }
        } ?: Text(
            text = "Waiting for GC samples...",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun JvmRuntimeTab(
    currentHeapSample: JstatHeapSample?,
    currentClassSample: JstatClassSample?,
    metaspaceHistory: List<Long>,
    loadedClassHistory: List<Long>,
    jvmPid: Int,
    classSamplerStatus: String,
    selectedJstatViewMode: JstatViewMode,
    activeSamplerMode: JstatViewMode,
    rawJstatLines: List<String>
) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        if (selectedJstatViewMode == JstatViewMode.Raw) {
            RawJstatTab(
                activeSamplerMode = activeSamplerMode,
                rawJstatLines = rawJstatLines
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
        }

        Text(
            text = "Runtime memory and class loading are useful for compiler, reflection, and classloader-heavy workloads.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        currentHeapSample?.let { sample ->
            when (sample.layout) {
                JstatSampleLayout.MemoryKb -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricText("Metaspace", formatKb(sample.metaspaceUsedKb))
                        MetricText("Meta cap", formatKb(sample.metaspaceCapacityKb))
                        MetricText("CCS", formatKb(sample.compressedClassUsedKb))
                        MetricText("CCS cap", formatKb(sample.compressedClassCapacityKb))
                    }

                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        UsageBarKb(
                            label = "Metaspace",
                            usedKb = sample.metaspaceUsedKb,
                            capacityKb = sample.metaspaceCapacityKb,
                            color = Color(0xFF5E35B1)
                        )
                        UsageBarKb(
                            label = "Compressed class space",
                            usedKb = sample.compressedClassUsedKb,
                            capacityKb = sample.compressedClassCapacityKb,
                            color = Color(0xFF00897B)
                        )
                    }
                }

                JstatSampleLayout.UtilizationPercent -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricText("Metaspace", formatPercent(sample.metaspaceUtilPercent))
                        MetricText("CCS", formatPercent(sample.compressedClassUtilPercent))
                    }

                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        UsageBarPercent(
                            label = "Metaspace",
                            percent = sample.metaspaceUtilPercent,
                            color = Color(0xFF5E35B1)
                        )
                        UsageBarPercent(
                            label = "Compressed class space",
                            percent = sample.compressedClassUtilPercent,
                            color = Color(0xFF00897B)
                        )
                    }
                }
            }

            Text(
                text = jvmPid.takeIf { it != -1 }
                    ?.let { "Runtime sample from JVM pid $it." }
                    ?: "Resolving JVM pid...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Legend(
                    label = if (sample.layout == JstatSampleLayout.MemoryKb) "Metaspace KB" else "Metaspace %",
                    color = Color(0xFF5E35B1)
                )
            }

            LiveGraph(
                series = listOf(
                    GraphSeries("Metaspace", metaspaceHistory, Color(0xFF5E35B1))
                ),
                modifier = Modifier.fillMaxWidth().height(92.dp),
                maxValue = if (sample.layout == JstatSampleLayout.UtilizationPercent) 100L else null
            )
        } ?: Text(
            text = "Waiting for runtime memory samples...",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

        Text(
            text = "Class Loading",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = classSamplerStatus,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )

        currentClassSample?.let { sample ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricText("Loaded", sample.loadedClasses.toString())
                MetricText("Unloaded", sample.unloadedClasses.toString())
                MetricText("Class data", formatKb(sample.loadedClassKb.roundToLong()))
                MetricText("Load time", formatSeconds(sample.classLoadTimeSeconds))
            }

            if (loadedClassHistory.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Legend("Loaded classes", MaterialTheme.colorScheme.primary)
                }

                LiveGraph(
                    series = listOf(
                        GraphSeries(
                            label = "Loaded classes",
                            data = loadedClassHistory,
                            color = MaterialTheme.colorScheme.primary
                        )
                    ),
                    modifier = Modifier.fillMaxWidth().height(92.dp)
                )
            }
        } ?: Text(
            text = "Waiting for class loading samples...",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun RawJstatTab(
    activeSamplerMode: JstatViewMode,
    rawJstatLines: List<String>
) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        Text(
            text = "Raw jstat output: ${activeSamplerMode.option}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Switch to Size, Percent, or Cause to change the raw output source.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )

        if (rawJstatLines.isEmpty()) {
            Text(
                text = "Waiting for raw jstat output...",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                rawJstatLines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun UsageBarKb(
    label: String,
    usedKb: Long,
    capacityKb: Long,
    color: Color
) {
    val fraction = if (capacityKb > 0L) {
        (usedKb.toFloat() / capacityKb.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    UsageBarRaw(
        label = label,
        value = if (capacityKb > 0L) {
            "${formatKb(usedKb)} / ${formatKb(capacityKb)} · ${formatPercent(fraction * 100.0)}"
        } else {
            "${formatKb(usedKb)} / unknown"
        },
        fraction = fraction,
        color = color
    )
}

@Composable
private fun UsageBarPercent(
    label: String,
    percent: Double,
    color: Color
) {
    UsageBarRaw(
        label = label,
        value = formatPercent(percent),
        fraction = (percent / 100.0).toFloat().coerceIn(0f, 1f),
        color = color
    )
}

@Composable
private fun UsageBarRaw(
    label: String,
    value: String,
    fraction: Float,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .padding(top = 3.dp)
        ) {
            drawRect(
                color = Color.Gray.copy(alpha = 0.22f),
                size = Size(size.width, size.height)
            )
            drawRect(
                color = color,
                size = Size(size.width * fraction, size.height)
            )
        }
    }
}

@Composable
private fun MetricText(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.width(10.dp).height(10.dp)) {
            drawCircle(color)
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

data class GraphSeries(
    val label: String,
    val data: List<Long>,
    val color: Color
)

private enum class JstatViewMode(
    val label: String,
    val option: String
) {
    Size("Size", "-gc"),
    Percent("Percent", "-gcutil"),
    Cause("Cause", "-gccause"),
    Raw("Raw", "-gccause")
}

private enum class JstatSampleLayout {
    MemoryKb,
    UtilizationPercent
}

private data class JstatHeapSample(
    val timestampMillis: Long = System.currentTimeMillis(),

    val layout: JstatSampleLayout,
    val sourceOption: String,
    val hasGcCause: Boolean,

    val survivor0CapacityKb: Long,
    val survivor1CapacityKb: Long,
    val survivor0UsedKb: Long,
    val survivor1UsedKb: Long,

    val edenCapacityKb: Long,
    val edenUsedKb: Long,

    val oldCapacityKb: Long,
    val oldUsedKb: Long,

    val metaspaceCapacityKb: Long,
    val metaspaceUsedKb: Long,

    val compressedClassCapacityKb: Long,
    val compressedClassUsedKb: Long,

    val survivor0UtilPercent: Double,
    val survivor1UtilPercent: Double,
    val edenUtilPercent: Double,
    val oldUtilPercent: Double,
    val metaspaceUtilPercent: Double,
    val compressedClassUtilPercent: Double,

    val youngGcCount: Long,
    val youngGcTimeSeconds: Double,

    val fullGcCount: Long,
    val fullGcTimeSeconds: Double,

    val concurrentGcCount: Long,
    val concurrentGcTimeSeconds: Double,

    val totalGcTimeSeconds: Double,

    val rawGcCause: String?
) {
    val liveHeapUsedKb: Long
        get() = edenUsedKb + oldUsedKb + survivor0UsedKb + survivor1UsedKb

    val committedHeapCapacityKb: Long
        get() = edenCapacityKb + oldCapacityKb + survivor0CapacityKb + survivor1CapacityKb

    val edenGraphValue: Long
        get() = when (layout) {
            JstatSampleLayout.MemoryKb -> edenUsedKb
            JstatSampleLayout.UtilizationPercent -> edenUtilPercent.roundToLong()
        }

    val oldGraphValue: Long
        get() = when (layout) {
            JstatSampleLayout.MemoryKb -> oldUsedKb
            JstatSampleLayout.UtilizationPercent -> oldUtilPercent.roundToLong()
        }

    val metaspaceGraphValue: Long
        get() = when (layout) {
            JstatSampleLayout.MemoryKb -> metaspaceUsedKb
            JstatSampleLayout.UtilizationPercent -> metaspaceUtilPercent.roundToLong()
        }
}

private data class JstatClassSample(
    val timestampMillis: Long = System.currentTimeMillis(),
    val loadedClasses: Long,
    val loadedClassKb: Double,
    val unloadedClasses: Long,
    val unloadedClassKb: Double,
    val classLoadTimeSeconds: Double
)

private fun sampleJstatHeap(
    context: Context,
    jdkDir: File,
    workingDir: File,
    targetProcess: Process,
    pid: Int,
    viewMode: JstatViewMode,
    onProcessStarted: (Process) -> Unit,
    onProcessFinished: (Process) -> Unit,
    onRawLine: (String) -> Unit,
    onSample: (JstatHeapSample) -> Unit
): String {
    val jstatProcess = try {
        LinuxProcessRunner.startJstatGcSampler(
            context = context,
            jdkDir = jdkDir,
            option = viewMode.option,
            pid = pid,
            workingDir = workingDir
        )
    } catch (exception: Exception) {
        return "Unable to start jstat ${viewMode.option} for pid $pid: ${exception.message.orEmpty()}"
    }

    onProcessStarted(jstatProcess)

    var headerLine: String? = null
    var emittedSample = false
    var lastOutput = ""

    try {
        jstatProcess.inputStream.bufferedReader().use { reader ->
            while (targetProcess.isAlive && jstatProcess.isAlive) {
                val line = reader.readLine() ?: break
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                Log.d("jstat", trimmed)
                onRawLine(trimmed)

                val header = headerLine
                val sample = header?.let {
                    parseJstatHeapSample(
                        sourceOption = viewMode.option,
                        headerLine = it,
                        valueLine = trimmed
                    )
                }

                if (sample != null) {
                    emittedSample = true
                    onSample(sample)
                    continue
                }

                lastOutput = trimmed

                if (isJstatHeapHeader(trimmed)) {
                    headerLine = trimmed
                }
            }
        }
    } catch (exception: Exception) {
        lastOutput = exception.message.orEmpty()
    } finally {
        onProcessFinished(jstatProcess)

        if (jstatProcess.isAlive) {
            jstatProcess.destroyForcibly()
        }
    }

    return when {
        emittedSample -> ""
        lastOutput.isBlank() -> "jstat ${viewMode.option} did not emit heap data for pid $pid"
        else -> "jstat ${viewMode.option} failed for pid $pid: $lastOutput"
    }
}

private fun sampleJstatClasses(
    context: Context,
    jdkDir: File,
    workingDir: File,
    targetProcess: Process,
    pid: Int,
    onProcessStarted: (Process) -> Unit,
    onProcessFinished: (Process) -> Unit,
    onSample: (JstatClassSample) -> Unit
): String {
    val jstatProcess = try {
        LinuxProcessRunner.startJstatClassSampler(
            context = context,
            jdkDir = jdkDir,
            pid = pid,
            workingDir = workingDir
        )
    } catch (exception: Exception) {
        return "Unable to start jstat -class for pid $pid: ${exception.message.orEmpty()}"
    }

    onProcessStarted(jstatProcess)

    var headerLine: String? = null
    var emittedSample = false
    var lastOutput = ""

    try {
        jstatProcess.inputStream.bufferedReader().use { reader ->
            while (targetProcess.isAlive && jstatProcess.isAlive) {
                val line = reader.readLine() ?: break
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                Log.d("jstat-class", trimmed)

                val header = headerLine
                val sample = header?.let { parseJstatClassSample(it, trimmed) }

                if (sample != null) {
                    emittedSample = true
                    onSample(sample)
                    continue
                }

                lastOutput = trimmed

                if (isJstatClassHeader(trimmed)) {
                    headerLine = trimmed
                }
            }
        }
    } catch (exception: Exception) {
        lastOutput = exception.message.orEmpty()
    } finally {
        onProcessFinished(jstatProcess)

        if (jstatProcess.isAlive) {
            jstatProcess.destroyForcibly()
        }
    }

    return when {
        emittedSample -> ""
        lastOutput.isBlank() -> "jstat -class did not emit data for pid $pid"
        else -> "jstat -class failed for pid $pid: $lastOutput"
    }
}

private fun isJstatHeapHeader(line: String): Boolean {
    val columns = line.trim().split(Regex("\\s+"))

    val gcLayout = columns.contains("EC") &&
            columns.contains("EU") &&
            columns.contains("OC") &&
            columns.contains("OU")

    val utilLayout = columns.contains("S0") &&
            columns.contains("S1") &&
            columns.contains("E") &&
            columns.contains("O") &&
            columns.contains("M") &&
            columns.contains("CCS") &&
            columns.contains("YGC") &&
            columns.contains("GCT")

    return gcLayout || utilLayout
}

private fun isJstatClassHeader(line: String): Boolean {
    val columns = line.trim().split(Regex("\\s+"))

    return columns.contains("Loaded") &&
            columns.contains("Bytes") &&
            columns.contains("Unloaded") &&
            columns.contains("Time")
}

private fun parseJstatHeapSample(
    sourceOption: String,
    headerLine: String,
    valueLine: String
): JstatHeapSample? {
    val headers = headerLine.trim().split(Regex("\\s+"))
    val values = valueLine.trim().split(Regex("\\s+"))

    if (headers.isEmpty() || values.isEmpty()) return null

    val isGcCapacityLayout = headers.contains("EC") &&
            headers.contains("EU") &&
            headers.contains("OC") &&
            headers.contains("OU")

    val isUtilLayout = headers.contains("S0") &&
            headers.contains("S1") &&
            headers.contains("E") &&
            headers.contains("O") &&
            headers.contains("M") &&
            headers.contains("CCS") &&
            headers.contains("YGC") &&
            headers.contains("GCT")

    if (!isGcCapacityLayout && !isUtilLayout) return null

    val firstCauseColumnIndex = listOf(
        headers.indexOf("LGCC"),
        headers.indexOf("GCC")
    ).filter { it >= 0 }.minOrNull() ?: headers.size

    val mapped = mutableMapOf<String, String>()

    headers.forEachIndexed { index, name ->
        if (index < values.size && index < firstCauseColumnIndex) {
            mapped[name] = values[index]
        }
    }

    fun number(column: String): Double {
        return mapped[column]
            ?.takeIf { it != "-" }
            ?.toDoubleOrNull()
            ?: 0.0
    }

    fun kb(column: String): Long {
        return number(column).roundToLong()
    }

    fun count(column: String): Long {
        return number(column).roundToLong()
    }

    fun seconds(column: String): Double {
        return number(column)
    }

    fun percent(column: String): Double {
        return number(column).coerceIn(0.0, 100.0)
    }

    val causes = extractGcCauses(headerLine, valueLine)
    val hasCauseColumns = headers.contains("LGCC") || headers.contains("GCC")

    return if (isGcCapacityLayout) {
        val s0Capacity = kb("S0C")
        val s1Capacity = kb("S1C")
        val s0Used = kb("S0U")
        val s1Used = kb("S1U")

        val edenCapacity = kb("EC")
        val edenUsed = kb("EU")

        val oldCapacity = kb("OC")
        val oldUsed = kb("OU")

        val metaspaceCapacity = kb("MC")
        val metaspaceUsed = kb("MU")

        val ccsCapacity = kb("CCSC")
        val ccsUsed = kb("CCSU")

        JstatHeapSample(
            layout = JstatSampleLayout.MemoryKb,
            sourceOption = sourceOption,
            hasGcCause = hasCauseColumns,

            survivor0CapacityKb = s0Capacity,
            survivor1CapacityKb = s1Capacity,
            survivor0UsedKb = s0Used,
            survivor1UsedKb = s1Used,

            edenCapacityKb = edenCapacity,
            edenUsedKb = edenUsed,

            oldCapacityKb = oldCapacity,
            oldUsedKb = oldUsed,

            metaspaceCapacityKb = metaspaceCapacity,
            metaspaceUsedKb = metaspaceUsed,

            compressedClassCapacityKb = ccsCapacity,
            compressedClassUsedKb = ccsUsed,

            survivor0UtilPercent = percentOf(s0Used, s0Capacity),
            survivor1UtilPercent = percentOf(s1Used, s1Capacity),
            edenUtilPercent = percentOf(edenUsed, edenCapacity),
            oldUtilPercent = percentOf(oldUsed, oldCapacity),
            metaspaceUtilPercent = percentOf(metaspaceUsed, metaspaceCapacity),
            compressedClassUtilPercent = percentOf(ccsUsed, ccsCapacity),

            youngGcCount = count("YGC"),
            youngGcTimeSeconds = seconds("YGCT"),

            fullGcCount = count("FGC"),
            fullGcTimeSeconds = seconds("FGCT"),

            concurrentGcCount = count("CGC"),
            concurrentGcTimeSeconds = seconds("CGCT"),

            totalGcTimeSeconds = seconds("GCT"),

            rawGcCause = causes
        )
    } else {
        JstatHeapSample(
            layout = JstatSampleLayout.UtilizationPercent,
            sourceOption = sourceOption,
            hasGcCause = hasCauseColumns,

            survivor0CapacityKb = 0L,
            survivor1CapacityKb = 0L,
            survivor0UsedKb = 0L,
            survivor1UsedKb = 0L,

            edenCapacityKb = 0L,
            edenUsedKb = 0L,

            oldCapacityKb = 0L,
            oldUsedKb = 0L,

            metaspaceCapacityKb = 0L,
            metaspaceUsedKb = 0L,

            compressedClassCapacityKb = 0L,
            compressedClassUsedKb = 0L,

            survivor0UtilPercent = percent("S0"),
            survivor1UtilPercent = percent("S1"),
            edenUtilPercent = percent("E"),
            oldUtilPercent = percent("O"),
            metaspaceUtilPercent = percent("M"),
            compressedClassUtilPercent = percent("CCS"),

            youngGcCount = count("YGC"),
            youngGcTimeSeconds = seconds("YGCT"),

            fullGcCount = count("FGC"),
            fullGcTimeSeconds = seconds("FGCT"),

            concurrentGcCount = count("CGC"),
            concurrentGcTimeSeconds = seconds("CGCT"),

            totalGcTimeSeconds = seconds("GCT"),

            rawGcCause = causes
        )
    }
}

private fun parseJstatClassSample(
    headerLine: String,
    valueLine: String
): JstatClassSample? {
    val headers = headerLine.trim().split(Regex("\\s+"))
    val values = valueLine.trim().split(Regex("\\s+"))

    if (!isJstatClassHeader(headerLine)) return null
    if (values.size < 5) return null

    val loadedIndex = headers.indexOf("Loaded").takeIf { it >= 0 } ?: 0
    val firstBytesIndex = headers.indexOf("Bytes").takeIf { it >= 0 } ?: 1
    val unloadedIndex = headers.indexOf("Unloaded").takeIf { it >= 0 } ?: 2
    val secondBytesIndex = headers.indexOfLast { it == "Bytes" }.takeIf { it >= 0 } ?: 3
    val timeIndex = headers.indexOf("Time").takeIf { it >= 0 } ?: 4

    fun valueAt(index: Int): String? {
        return values.getOrNull(index)?.takeIf { it != "-" }
    }

    return JstatClassSample(
        loadedClasses = valueAt(loadedIndex)?.toDoubleOrNull()?.roundToLong() ?: return null,
        loadedClassKb = valueAt(firstBytesIndex)?.toDoubleOrNull() ?: 0.0,
        unloadedClasses = valueAt(unloadedIndex)?.toDoubleOrNull()?.roundToLong() ?: 0L,
        unloadedClassKb = valueAt(secondBytesIndex)?.toDoubleOrNull() ?: 0.0,
        classLoadTimeSeconds = valueAt(timeIndex)?.toDoubleOrNull() ?: 0.0
    )
}

private fun extractGcCauses(
    headerLine: String,
    valueLine: String
): String? {
    if (!headerLine.contains("LGCC") && !headerLine.contains("GCC")) {
        return null
    }

    val headers = headerLine.trim().split(Regex("\\s+"))
    val gctIndex = headers.indexOf("GCT")
    if (gctIndex == -1) return null

    val gctEndOffset = endOffsetOfToken(valueLine, gctIndex) ?: return null

    val causeText = valueLine
        .substring(gctEndOffset)
        .trim()

    if (causeText.isBlank()) return null

    val parts = causeText
        .split(Regex("\\s{2,}"))
        .map { it.trim() }
        .filter { it.isNotBlank() }

    return when {
        parts.size >= 2 -> "Last: ${parts[0]} · Current: ${parts[1]}"
        parts.size == 1 -> parts[0]
        else -> causeText
    }
}

private fun endOffsetOfToken(
    line: String,
    tokenIndex: Int
): Int? {
    var currentToken = -1
    var inToken = false
    var tokenStart = -1

    line.forEachIndexed { index, char ->
        val isSpace = char.isWhitespace()

        if (!isSpace && !inToken) {
            currentToken++
            inToken = true
            tokenStart = index
        }

        if ((isSpace || index == line.lastIndex) && inToken) {
            val tokenEnd = if (isSpace) index else index + 1
            if (currentToken == tokenIndex && tokenStart != -1) {
                return tokenEnd
            }
            inToken = false
            tokenStart = -1
        }
    }

    return null
}

private fun estimateAllocationRateKbPerSec(
    previous: JstatHeapSample?,
    current: JstatHeapSample
): Double {
    if (previous == null) return 0.0
    if (previous.layout != JstatSampleLayout.MemoryKb) return 0.0
    if (current.layout != JstatSampleLayout.MemoryKb) return 0.0

    val elapsedSeconds = ((current.timestampMillis - previous.timestampMillis) / 1000.0)
        .coerceAtLeast(0.001)

    val edenDelta = current.edenUsedKb - previous.edenUsedKb

    val estimatedAllocatedKb = if (edenDelta >= 0L) {
        edenDelta
    } else if (current.youngGcCount > previous.youngGcCount && previous.edenCapacityKb > 0L) {
        val previousRemainingEden = (previous.edenCapacityKb - previous.edenUsedKb).coerceAtLeast(0L)
        previousRemainingEden + current.edenUsedKb
    } else {
        0L
    }

    return estimatedAllocatedKb / elapsedSeconds
}

private fun estimateGcOverheadPercent(
    previous: JstatHeapSample?,
    current: JstatHeapSample
): Double {
    if (previous == null) return 0.0

    val elapsedSeconds = ((current.timestampMillis - previous.timestampMillis) / 1000.0)
        .coerceAtLeast(0.001)

    val gcSeconds = (current.totalGcTimeSeconds - previous.totalGcTimeSeconds)
        .coerceAtLeast(0.0)

    return ((gcSeconds / elapsedSeconds) * 100.0).coerceIn(0.0, 100.0)
}

private fun percentOf(
    used: Long,
    capacity: Long
): Double {
    if (capacity <= 0L) return 0.0
    return ((used.toDouble() / capacity.toDouble()) * 100.0).coerceIn(0.0, 100.0)
}

private fun formatKb(kb: Long): String {
    return when {
        kb >= 1024L * 1024L -> {
            String.format(Locale.US, "%.2f GB", kb / 1024.0 / 1024.0)
        }

        kb >= 1024L -> {
            String.format(Locale.US, "%.1f MB", kb / 1024.0)
        }

        else -> {
            "$kb KB"
        }
    }
}

private fun formatKbPerSec(kbPerSec: Double): String {
    return when {
        kbPerSec >= 1024.0 * 1024.0 -> {
            String.format(Locale.US, "%.2f GB/s", kbPerSec / 1024.0 / 1024.0)
        }

        kbPerSec >= 1024.0 -> {
            String.format(Locale.US, "%.1f MB/s", kbPerSec / 1024.0)
        }

        else -> {
            String.format(Locale.US, "%.0f KB/s", kbPerSec)
        }
    }
}

private fun formatSeconds(seconds: Double): String {
    return when {
        seconds >= 60.0 -> {
            String.format(Locale.US, "%.1f min", seconds / 60.0)
        }

        seconds >= 1.0 -> {
            String.format(Locale.US, "%.2f s", seconds)
        }

        else -> {
            String.format(Locale.US, "%.0f ms", seconds * 1000.0)
        }
    }
}

private fun formatPercent(percent: Double): String {
    return String.format(Locale.US, "%.1f%%", percent)
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
            file.inputStream().use {
                ClassReader(it.readBytes()).accept(this, ClassReader.SKIP_CODE)
            }
        }

        val hasMain = node.methods.any {
            it.name == "main" && it.desc == "([Ljava/lang/String;)V"
        }

        if (hasMain) {
            return node.name.replace('/', '.')
        }
    }

    return null
}