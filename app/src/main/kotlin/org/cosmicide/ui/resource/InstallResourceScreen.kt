package org.cosmicide.ui.resource

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import org.cosmicide.BuildConfig
import org.cosmicide.util.ResourceUtil
import org.cosmicide.util.extractTarZstStream
import org.cosmicide.util.restoreSymlinksFromManifest

@Composable
fun InstallResourcesScreen(
    onMoveToJdkManager: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var isRunning by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Ready to configure environment assets.") }
    var progressDetailsText by remember { mutableStateOf("Required resources will be downloaded and installed.") }
    var currentProgress by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current

    val runSetupChain = {
        isRunning = true
        scope.launch {
            val glibcTargetDir = context.filesDir.resolve("glibc")
            if (!ResourceUtil.isRuntimeReady(glibcTargetDir)) {
                statusText = "Setting up local runtime..."
                currentProgress = -1f
                progressDetailsText = "Extracting runtime..."

                val extractionResult = withContext(Dispatchers.IO) {
                    runCatching {
                        val stagingDir = context.filesDir.resolve("glibc.staging")
                        check(stagingDir.deleteRecursively()) { "Cannot clear runtime staging directory." }
                        check(stagingDir.mkdirs()) { "Cannot create runtime staging directory." }
                        context.assets.open("glibc.tar.zst").use { assetIn ->
                            extractTarZstStream(assetIn, stagingDir, "glibc/", longMax = 30)
                                .getOrThrow()
                        }
                        restoreSymlinksFromManifest(stagingDir).getOrThrow()
                        stagingDir.resolve(".installed").writeText("1\n")
                        check(ResourceUtil.isRuntimeReady(stagingDir)) { "Runtime archive is incomplete." }

                        val backupDir = context.filesDir.resolve("glibc.backup")
                        check(backupDir.deleteRecursively()) { "Cannot clear runtime backup." }
                        if (glibcTargetDir.exists()) {
                            check(glibcTargetDir.renameTo(backupDir)) { "Cannot back up runtime." }
                        }
                        if (!stagingDir.renameTo(glibcTargetDir)) {
                            backupDir.renameTo(glibcTargetDir)
                            error("Cannot install runtime.")
                        }
                        backupDir.deleteRecursively()
                        context.filesDir.resolve("glibc-deploy-error.log").delete()
                    }.onFailure { error ->
                        if (error is CancellationException) throw error
                        if (BuildConfig.DEBUG) {
                            android.util.Log.e("RuntimeSetup", "Runtime deployment failed", error)
                            runCatching {
                                context.filesDir.resolve("glibc-deploy-error.log")
                                    .writeText(error.stackTraceToString())
                            }
                        } else {
                            android.util.Log.e("RuntimeSetup", "Runtime deployment failed: ${error.javaClass.simpleName}")
                        }
                    }
                }

                val error = extractionResult.exceptionOrNull()
                if (error != null) {
                    statusText = "Failed to deploy glibc runtime."
                    progressDetailsText = when (error) {
                        is UnsatisfiedLinkError -> "Cannot load the zstd native library. Please update the app."
                        is OutOfMemoryError -> "Not enough memory to extract the runtime. Close other apps and retry."
                        else -> error.message ?: error.javaClass.simpleName
                    }
                    isRunning = false
                    return@launch
                }
            }

            statusText = "Core runtime initialized!"
            progressDetailsText = "Continue by selecting a JDK toolchain."
            isRunning = false
            onMoveToJdkManager()
        }
        Unit
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Environment Init",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = progressDetailsText,
                style = MaterialTheme.typography.bodyLarge,
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
                        .clip(MaterialTheme.shapes.small)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = runSetupChain,
                enabled = !isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shapes = ButtonDefaults.shapes()
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Initialize Workspace Environment",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
