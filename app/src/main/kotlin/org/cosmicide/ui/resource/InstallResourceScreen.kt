package org.cosmicide.ui.resource

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import org.cosmicide.util.extractTarZstStream
import org.cosmicide.util.restoreSymlinksFromManifest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InstallResourcesScreen(
    onMoveToJdkManager: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var isRunning by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Ready to configure environment assets.") }
    var progressDetailsText by remember { mutableStateOf("Foundational resources will be deployed.") }
    var currentProgress by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current

    val runSetupChain = {
        isRunning = true
        scope.launch {
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
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
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
