package org.cosmicide.ui.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.BuildConfig
import org.cosmicide.common.Prefs
import org.cosmicide.exec.linux.LinuxProcessRunner
import org.cosmicide.ui.settings.components.PreferenceItem
import org.cosmicide.ui.settings.components.SwitchPreference
import org.cosmicide.util.jdksDir

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var analyticsEnabled by remember { mutableStateOf(prefs.getBoolean("analytics_preference", true)) }
    var showTerminalDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            PreferenceItem(
                title = "About",
                summary = "A free and open-source IDE for Android. It is licensed under the GNU General Public License v3.0."
            )

            PreferenceItem(
                title = "Donate",
                summary = "Donate to the developers. This will help us to keep the project alive. Thank you for your support!",
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, "https://pranavpurwar.github.io/donate.html".toUri()))
                }
            )

            PreferenceItem(
                title = "App version",
                summary = BuildConfig.VERSION_NAME + if (BuildConfig.DEBUG) " (${BuildConfig.GIT_COMMIT})" else "",
                onClick = {
                    // Logic for developer mode could go here
                }
            )

            PreferenceItem(
                title = "Execute Linux command",
                summary = "Run an arbitrary command inside the embedded environment",
                onClick = { showTerminalDialog = true }
            )

            PreferenceItem(
                title = "Source code",
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/Cosmic-IDE/Cosmic-IDE".toUri()))
                }
            )

            PreferenceItem(
                title = "Manage storage permission",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, ("package:" + context.packageName).toUri()))
                        } catch (_: Exception) {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                        }
                    } else {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, ("package:" + context.packageName).toUri()))
                    }
                }
            )

            SwitchPreference(
                title = "Analytics",
                summary = "Help us improve the app by sending anonymous usage data",
                checked = analyticsEnabled,
                onCheckedChange = {
                    analyticsEnabled = it
                    prefs.edit().putBoolean("analytics_preference", it).apply()
                }
            )
            
            PreferenceItem(
                title = "Force crash",
                onClick = { throw RuntimeException("Forced crash") }
            )
        }
    }

    if (showTerminalDialog) {
        TerminalExecutionDialog(
            onDismiss = { showTerminalDialog = false }
        )
    }
}

@Composable
fun TerminalExecutionDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var commandInput by remember { mutableStateOf("") }
    var outputLog by remember { mutableStateOf("Glibc environment ready for command...\n") }
    var isRunning by remember { mutableStateOf(false) }
    val outputScrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = { if (!isRunning) onDismiss() },
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Glibc Execution Panel", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    label = { Text("Command") },
                    placeholder = { Text("e.g., /bin/ls -la") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    singleLine = true,
                    enabled = !isRunning,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 350.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = outputLog,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.verticalScroll(outputScrollState)
                        )
                    }
                }

                if (isRunning) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(MaterialTheme.shapes.small)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (commandInput.isNotBlank() && !isRunning) {
                        isRunning = true
                        outputLog = "Executing inside custom glibc layer...\n\n"

                        scope.launch(Dispatchers.IO) {
                            try {
                                val jdkDir = context.jdksDir().resolve("jdk-" + Prefs.currentJDK)
                                val workingDir = context.filesDir
                                val pathEntries = LinuxProcessRunner.toolchainPathEntries(context, jdkDir)
                                val commandParts = LinuxProcessRunner.parseCommandLine(commandInput.trim())
                                val binary = LinuxProcessRunner.resolveExecutable(
                                    commandName = commandParts.first(),
                                    workingDir = workingDir,
                                    pathEntries = pathEntries
                                )
                                val tempDir = context.cacheDir
                                val runnerConfig = LinuxProcessRunner.Configuration(
                                    binary = binary,
                                    arguments = commandParts.drop(1),
                                    workingDir = workingDir,
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
                                    onOutputReceived = { outputChunk ->
                                        scope.launch(Dispatchers.Main) {
                                            outputLog += outputChunk
                                            outputScrollState.animateScrollTo(outputScrollState.maxValue)
                                        }
                                    },
                                    onProcessStarted = {}
                                )
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    outputLog += "\nExecution Fault: ${e.message}\n"
                                }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    isRunning = false
                                }
                            }
                        }
                    }
                },
                enabled = commandInput.isNotBlank() && !isRunning,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
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
