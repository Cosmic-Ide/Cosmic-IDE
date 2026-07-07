package org.cosmicide.ui.resource

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.util.Download
import org.cosmicide.util.FileUtil
import org.cosmicide.util.ResourceUtil
import org.cosmicide.util.extractTarGzFolder
import org.cosmicide.util.extractTarZstStream
import org.cosmicide.util.extractZip
import org.cosmicide.util.restoreSymlinksFromManifest
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallResourcesScreen(
    onMoveToJdkManager: () -> Unit
) {
    val client = remember { HttpClient(CIO) }
    val scope = rememberCoroutineScope()

    val rawUrl = "https://github.com/Cosmic-Ide/binaries/raw/main/"
    val kotlinUrl =
        "https://github.com/JetBrains/kotlin/releases/download/v2.4.0/kotlin-compiler-2.4.0.zip"
    val jdtlsUrl =
        "https://www.eclipse.org/downloads/download.php?file=/jdtls/snapshots/jdt-language-server-latest.tar.gz"

    var isRunning by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Ready to configure environment assets.") }
    var progressDetailsText by remember { mutableStateOf("Foundational resources will be deployed.") }
    var currentProgress by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current

    val onProgressUpdate: (Long, Long) -> Unit = { downloaded, total ->
        val downloadedMB = downloaded / (1024f * 1024f)
        if (total > 0) {
            currentProgress = downloaded.toFloat() / total
            val totalMB = total / (1024f * 1024f)
            val percent = ((downloaded * 100) / total).toInt()
            progressDetailsText = String.format(
                Locale.getDefault(), "%.1f MB / %.1f MB (%d%%)", downloadedMB, totalMB, percent
            )
        } else {
            currentProgress = -1f
            progressDetailsText =
                String.format(Locale.getDefault(), "%.1f MB downloaded", downloadedMB)
        }
    }

    val runSetupChain = {
        isRunning = true
        scope.launch {
            val missingResources = withContext(Dispatchers.IO) { ResourceUtil.missingResources() }
            if (missingResources.isNotEmpty()) {
                statusText = "Downloading Core Internal Resources..."
                for (res in missingResources) {
                    val success = installResource(
                        client,
                        rawUrl + res.substringAfterLast('/'),
                        File(FileUtil.dataDir, res),
                        onProgressUpdate
                    )
                    if (!success) {
                        statusText = "Failed to sync core internal resources."
                        isRunning = false
                        return@launch
                    }
                }
            }

            val kotlinTargetDir = File(FileUtil.dataDir, "kotlinc")
            if (!kotlinTargetDir.exists() || kotlinTargetDir.listFiles()?.isEmpty() == true) {
                statusText = "Downloading Kotlin Compiler..."
                val kotlinArchiveFile = File(FileUtil.dataDir, "kotlin_compiler.zip")

                val downloadSuccess =
                    installResource(client, kotlinUrl, kotlinArchiveFile, onProgressUpdate)
                if (!downloadSuccess) {
                    statusText = "Network failure downloading Kotlin Compiler."
                    isRunning = false
                    return@launch
                }

                statusText = "Extracting Kotlin Compiler package..."
                currentProgress = -1f
                progressDetailsText = "Expanding archive structures onto local storage..."
                val extractionSuccess =
                    withContext(Dispatchers.IO) { extractZip(kotlinArchiveFile, FileUtil.dataDir) }
                if (kotlinArchiveFile.exists()) kotlinArchiveFile.delete()

                if (!extractionSuccess) {
                    statusText = "Failed to unpack Kotlin Compiler package."
                    isRunning = false
                    return@launch
                }
            }

            val jdtlsTargetDir = context.filesDir.resolve("jdtls")
            if (!jdtlsTargetDir.exists() || jdtlsTargetDir.listFiles()?.isEmpty() == true) {
                jdtlsTargetDir.mkdirs()
                statusText = "Downloading Eclipse JDT Language Server..."
                val jdtlsArchiveFile = context.cacheDir.resolve("jdtls_archive.tar.gz")

                val downloadSuccess =
                    installResource(client, jdtlsUrl, jdtlsArchiveFile, onProgressUpdate)
                if (!downloadSuccess) {
                    statusText = "Network failure downloading language components."
                    isRunning = false
                    return@launch
                }

                statusText = "Deploying Language Server architecture..."
                currentProgress = -1f
                progressDetailsText = "Extracting runtime distribution components..."
                val extractionSuccess = withContext(Dispatchers.IO) {
                    extractTarGzFolder(jdtlsArchiveFile, jdtlsTargetDir, null)
                }
                if (jdtlsArchiveFile.exists()) jdtlsArchiveFile.delete()

                if (!extractionSuccess) {
                    statusText = "Failed to deploy Language Server environment."
                    isRunning = false
                    return@launch
                }
            }

            val glibcTargetDir = context.filesDir.resolve("glibc")
            if (!glibcTargetDir.exists() || glibcTargetDir.listFiles()?.isEmpty() == true) {
                glibcTargetDir.mkdirs()
                statusText = "Deploying local runtime..."
                currentProgress = -1f
                progressDetailsText = "Extracting runtime..."

                val extractionSuccessGl = withContext(Dispatchers.IO) {
                    runCatching {
                        context.assets.open("glibc.tar.zst").use { assetIn ->
                            extractTarZstStream(assetIn, glibcTargetDir, "glibc/", longMax = 30)
                        } && restoreSymlinksFromManifest(glibcTargetDir)
                    }.getOrDefault(false)
                }

                if (!extractionSuccessGl) {
                    statusText = "Failed to deploy glibc runtime."
                    isRunning = false
                    return@launch
                }
            }

            statusText = "Workspace environment initialized!"
            isRunning = false
            onMoveToJdkManager()
        }
        Unit
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = {
                Text(
                    "Environment Init",
                    fontWeight = FontWeight.Bold
                )
            })
        }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = progressDetailsText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            AnimatedVisibility(visible = isRunning) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = runSetupChain,
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth(),
                shapes = ButtonDefaults.shapes()
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Initialize Workspace Environment")
            }
        }
    }
}

private suspend fun installResource(
    client: HttpClient, url: String, destinationFile: File, onProgressUpdate: (Long, Long) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    try {
        if (destinationFile.exists()) destinationFile.delete()
        Download(client, url, onProgressUpdate).start(destinationFile)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
