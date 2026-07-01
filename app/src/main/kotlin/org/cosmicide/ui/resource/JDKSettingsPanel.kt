/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.ui.resource

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.sdk.manager.jdk.FoojayClient
import org.cosmicide.util.jdks
import org.cosmicide.util.jdksDir
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream

sealed class JdkAction(val version: String, val vendorParam: String) {
    class Install(version: String, vendorParam: String) : JdkAction(version, vendorParam)
    class Uninstall(version: String, vendorParam: String) : JdkAction(version, vendorParam)
}

enum class TaskStatus { PENDING, IN_PROGRESS, SUCCESS, ERROR }

data class TaskState(
    val action: JdkAction,
    var status: TaskStatus = TaskStatus.PENDING,
    var progress: Float = 0f,
    var message: String = "Waiting..."
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun JdkSettingsPanel(
    onDismissRequested: () -> Unit,
) {
    val foojayClient = remember { FoojayClient() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Data State
    var distributions by remember { mutableStateOf<List<FoojayClient.Distribution>>(emptyList()) }
    var selectedDistro by remember { mutableStateOf<FoojayClient.Distribution?>(null) }
    var globalLoading by remember { mutableStateOf(true) }

    // State Tracking
    val installedVersions = remember { mutableStateMapOf<String, Boolean>() }
    val targetVersions = remember { mutableStateMapOf<String, Boolean>() }

    // Execution Queue State
    var isProcessingScreenActive by remember { mutableStateOf(false) }
    val taskQueue = remember { mutableStateListOf<TaskState>() }
    var isAllTasksComplete by remember { mutableStateOf(false) }

    val refreshInstalledRegistry: () -> Unit = {
        val currentVendor = selectedDistro?.apiParam ?: ""
        installedVersions.clear()

        if (currentVendor.isNotEmpty()) {
            context.jdks().forEach {
                if (it.distributor.equals(currentVendor, ignoreCase = true)) {
                    installedVersions[it.version] = true
                }
            }
        }

        // Reset targets to match installed state when refreshed or vendor changed
        targetVersions.clear()
        installedVersions.keys.forEach { targetVersions[it] = true }
    }

    LaunchedEffect(Unit) {
        foojayClient.fetchMaintainedDistributions()
            .onSuccess { list ->
                distributions = list
                selectedDistro = list.find { it.apiParam == "semeru" } ?: list.firstOrNull()
                refreshInstalledRegistry()
                globalLoading = false
            }
            .onFailure { globalLoading = false }
    }

    LaunchedEffect(selectedDistro) {
        if (!globalLoading) refreshInstalledRegistry()
    }

    // Determine pending changes
    val pendingInstalls = targetVersions.filter { it.value && installedVersions[it.key] != true }.keys
    val pendingUninstalls = installedVersions.filter { it.value && targetVersions[it.key] != true }.keys
    val hasPendingChanges = pendingInstalls.isNotEmpty() || pendingUninstalls.isNotEmpty()

    val executeTaskQueue = {
        val vendor = selectedDistro!!.apiParam
        taskQueue.clear()

        pendingInstalls.forEach { taskQueue.add(TaskState(JdkAction.Install(it, vendor))) }
        pendingUninstalls.forEach { taskQueue.add(TaskState(JdkAction.Uninstall(it, vendor))) }

        isProcessingScreenActive = true
        isAllTasksComplete = false

        scope.launch {
            for (i in taskQueue.indices) {
                val task = taskQueue[i]
                taskQueue[i] = task.copy(status = TaskStatus.IN_PROGRESS, message = "Starting...")

                val targetDirName = "${task.action.vendorParam}-${task.action.version}"

                when (task.action) {
                    is JdkAction.Install -> {
                        val hostOs = FoojayClient.OS.resolve(System.getProperty("os.name") ?: "linux")
                        val hostArch = FoojayClient.Arch.resolve(System.getProperty("os.arch") ?: "aarch64")

                        taskQueue[i] = taskQueue[i].copy(message = "Resolving artifacts...")

                        val resolveResult = foojayClient.resolveLatestArtifact(task.action.vendorParam, task.action.version, hostOs, hostArch)
                        if (resolveResult.isSuccess) {
                            val artifact = resolveResult.getOrNull()!!
                            val targetArchiveFile = context.cacheDir.resolve(artifact.filename)

                            taskQueue[i] = taskQueue[i].copy(message = "Downloading...")

                            val downloadResult = foojayClient.downloadArtifactWithProgress(artifact, targetArchiveFile) { progress ->
                                taskQueue[i] = taskQueue[i].copy(progress = progress / 100f, message = "Downloading ($progress%)")
                            }

                            if (downloadResult.isSuccess && downloadResult.getOrNull() == true) {
                                taskQueue[i] = taskQueue[i].copy(progress = -1f, message = "Extracting runtime...")
                                val targetDir = context.jdksDir().resolve(targetDirName)
                                val extracted = extractTarGz(targetArchiveFile, targetDir)

                                if (targetArchiveFile.exists()) targetArchiveFile.delete()

                                if (extracted) {
                                    taskQueue[i] = taskQueue[i].copy(status = TaskStatus.SUCCESS, message = "Installed successfully")
                                } else {
                                    taskQueue[i] = taskQueue[i].copy(status = TaskStatus.ERROR, message = "Extraction failed")
                                }
                            } else {
                                taskQueue[i] = taskQueue[i].copy(status = TaskStatus.ERROR, message = "Download failed")
                            }
                        } else {
                            taskQueue[i] = taskQueue[i].copy(status = TaskStatus.ERROR, message = "Resolution failed")
                        }
                    }
                    is JdkAction.Uninstall -> {
                        taskQueue[i] = taskQueue[i].copy(progress = -1f, message = "Deleting files...")
                        withContext(Dispatchers.IO) {
                            context.jdksDir().resolve(targetDirName).deleteRecursively()
                        }
                        taskQueue[i] = taskQueue[i].copy(status = TaskStatus.SUCCESS, progress = 1f, message = "Uninstalled successfully")
                    }
                }
            }
            refreshInstalledRegistry()
            isAllTasksComplete = true
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("JDK Toolchains", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (isProcessingScreenActive && isAllTasksComplete) {
                        IconButton(onClick = { isProcessingScreenActive = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    } else if (!isProcessingScreenActive) {
                        IconButton(onClick = onDismissRequested) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    }
                },
                actions = {
                    if (!isProcessingScreenActive) {
                        IconButton(onClick = refreshInstalledRegistry) {
                            Icon(Icons.Default.Refresh, "Refresh Sync")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !isProcessingScreenActive && hasPendingChanges,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Pending Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${pendingInstalls.size} to install, ${pendingUninstalls.size} to remove",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { executeTaskQueue() },
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Apply")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {

            AnimatedContent(
                targetState = isProcessingScreenActive,
                transitionSpec = {
                    slideInHorizontally(tween(400)) { if (targetState) it else -it } + fadeIn() togetherWith
                            slideOutHorizontally(tween(400)) { if (targetState) -it else it } + fadeOut()
                },
                label = "Screen Transition"
            ) { isProcessing ->
                if (isProcessing) {
                    ProcessingScreen(taskQueue, isAllTasksComplete) {
                        isProcessingScreenActive = false
                    }
                } else {
                    SelectionScreen(
                        globalLoading = globalLoading,
                        distributions = distributions,
                        selectedDistro = selectedDistro,
                        onDistroSelected = { selectedDistro = it },
                        installedVersions = installedVersions,
                        targetVersions = targetVersions,
                        onTargetToggled = { version, isTargeted ->
                            targetVersions[version] = isTargeted
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionScreen(
    globalLoading: Boolean,
    distributions: List<FoojayClient.Distribution>,
    selectedDistro: FoojayClient.Distribution?,
    onDistroSelected: (FoojayClient.Distribution) -> Unit,
    installedVersions: Map<String, Boolean>,
    targetVersions: Map<String, Boolean>,
    onTargetToggled: (String, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Vendor Selection
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (!globalLoading) expanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedDistro?.name ?: if (globalLoading) "Loading vendors..." else "Select Vendor",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Provider") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    distributions.forEach { distro ->
                        DropdownMenuItem(
                            text = {
                                Text(distro.name, fontWeight = if (distro == selectedDistro) FontWeight.Bold else FontWeight.Normal)
                            },
                            trailingIcon = if (distro.apiParam == "semeru") {
                                { Badge { Text("Recommended") } }
                            } else null,
                            onClick = {
                                onDistroSelected(distro)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // SDK List
        if (globalLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            selectedDistro?.let { distro ->
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 100.dp), // Space for bottom bar
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = "Available Versions",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }

                    items(distro.versions) { version ->
                        val isCurrentlyInstalled = installedVersions[version] == true
                        val isTargeted = targetVersions[version] ?: isCurrentlyInstalled

                        val willBeInstalled = isTargeted && !isCurrentlyInstalled
                        val willBeRemoved = !isTargeted && isCurrentlyInstalled

                        ListItem(
                            headlineContent = { Text("Java SE $version", fontWeight = FontWeight.SemiBold) },
                            supportingContent = {
                                if (willBeInstalled) Text("Marked for installation", color = MaterialTheme.colorScheme.primary)
                                else if (willBeRemoved) Text("Marked for removal", color = MaterialTheme.colorScheme.error)
                                else if (isCurrentlyInstalled) Text("Installed")
                                else Text("Available for download")
                            },
                            leadingContent = {
                                Checkbox(
                                    checked = isTargeted,
                                    onCheckedChange = { onTargetToggled(version, it) }
                                )
                            },
                            trailingContent = {
                                if (isCurrentlyInstalled && !willBeRemoved) {
                                    Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            colors = ListItemDefaults.colors(
                                containerColor = when {
                                    willBeInstalled -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    willBeRemoved -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                    isCurrentlyInstalled -> MaterialTheme.colorScheme.surfaceContainer
                                    else -> Color.Transparent
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessingScreen(
    tasks: List<TaskState>,
    isComplete: Boolean,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = if (isComplete) "All tasks completed" else "Applying Changes...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(tasks) { _, task ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status Icon
                        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            when (task.status) {
                                TaskStatus.PENDING -> Icon(
                                    if (task.action is JdkAction.Install) Icons.Rounded.Download else Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                TaskStatus.IN_PROGRESS -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                TaskStatus.SUCCESS -> Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                TaskStatus.ERROR -> Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Task Details
                        Column(modifier = Modifier.weight(1f)) {
                            val title = when (task.action) {
                                is JdkAction.Install -> "Install JDK ${task.action.version}"
                                is JdkAction.Uninstall -> "Uninstall JDK ${task.action.version}"
                            }

                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(task.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            if (task.status == TaskStatus.IN_PROGRESS && task.progress >= 0f) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { task.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            } else if (task.status == TaskStatus.IN_PROGRESS && task.progress < 0f) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp)))
                            }
                        }
                    }
                }
            }
        }

        if (isComplete) {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(56.dp),
                shapes = ButtonDefaults.shapes()
            ) {
                Text("Finish", fontSize = MaterialTheme.typography.titleMedium.fontSize)
            }
        }
    }
}

private suspend fun extractTarGz(tarGz: File, targetDir: File): Boolean = withContext(Dispatchers.IO) {
    if (!tarGz.exists()) return@withContext false

    try {
        var rootPrefix: String? = null
        val buffer = ByteArray(8192)
        val header = ByteArray(512)

        GZIPInputStream(tarGz.inputStream()).use { gisin ->

            while (gisin.readNBytes(header, 0, 512) == 512) {
                if (header[0].toInt() == 0) break

                val rawName = header.readTarString(0, 100)
                if (rawName.isEmpty()) continue

                val size = header.readTarOctal(124, 12)
                val cleanName = rawName.removePrefix("./")

                if (rootPrefix == null) {
                    rootPrefix = cleanName.substringBefore('/')
                }

                val prefixWithSlash = "$rootPrefix/"
                val strippedName = when {
                    cleanName.startsWith(prefixWithSlash) -> cleanName.substring(prefixWithSlash.length)
                    cleanName == rootPrefix -> ""
                    else -> cleanName
                }

                if (strippedName.isNotEmpty()) {
                    val targetFile = targetDir.resolve(strippedName)

                    if (rawName.endsWith("/")) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        targetFile.outputStream().use { fos ->
                            gisin.copyBounded(fos, size, buffer)
                        }
                    }
                } else {
                    gisin.skipNBytes(size)
                }

                val padding = (512 - (size % 512)) % 512
                gisin.skipNBytes(padding)
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun ByteArray.readTarString(offset: Int, length: Int) =
    String(this, offset, length).trim { it <= ' ' || it.code == 0 }

private fun ByteArray.readTarOctal(offset: Int, length: Int) =
    readTarString(offset, length).toLongOrNull(8) ?: 0L

private fun InputStream.copyBounded(out: OutputStream, size: Long, buffer: ByteArray) {
    var remain = size
    while (remain > 0) {
        val chunk = this.read(buffer, 0, minOf(remain, buffer.size.toLong()).toInt())
        if (chunk == -1) break
        out.write(buffer, 0, chunk)
        remain -= chunk
    }
}