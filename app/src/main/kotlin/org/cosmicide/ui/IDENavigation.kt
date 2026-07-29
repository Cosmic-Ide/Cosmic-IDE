package org.cosmicide.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.cosmicide.app.LocalAppContainer
import org.cosmicide.ui.compile.GradleTaskScreen
import org.cosmicide.ui.editor.EditorScreen
import org.cosmicide.ui.home.HomeScreen
import org.cosmicide.ui.project.NewProjectScreen
import org.cosmicide.ui.resource.InstallResourcesScreen
import org.cosmicide.ui.resource.JdkSettingsPanel
import org.cosmicide.ui.settings.AboutSettingsScreen
import org.cosmicide.ui.settings.CompilerSettingsScreen
import org.cosmicide.ui.settings.EditorSettingsScreen
import org.cosmicide.ui.settings.ExtensionsSettingsScreen
import org.cosmicide.ui.settings.SettingsScreen
import org.cosmicide.ui.terminal.TerminalScreen
import org.cosmicide.util.ResourceUtil
import java.io.File

@Composable
fun IDENavigation() {
    val context = LocalContext.current
    val projectSessionServices = LocalAppContainer.current.projectSessionServices
    val initialScreen: Screen = when {
        ResourceUtil.isBootstrapIncomplete() -> InstallResourceScreen
        ResourceUtil.isJdkMissing() -> JDKSettingsScreen
        ResourceUtil.isLanguageServerSetupIncomplete() -> LanguageServerSetupScreen
        else -> Home
    }
    val backStack = rememberNavBackStack(
        initialScreen
    )

    val hasProjectSession = backStack.any { screen ->
        screen is Editor || screen is GradleTask
    }

    LaunchedEffect(hasProjectSession) {
        if (!hasProjectSession) {
            projectSessionServices.stopTooling()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            projectSessionServices.stopTooling()
        }
    }

    NavDisplay(
        backStack = backStack, onBack = { backStack.removeLastOrNull() }, entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        )
    ) { key ->
        return@NavDisplay when (key) {
            is InstallResourceScreen -> NavEntry(key) {
                InstallResourcesScreen {
                    backStack.removeLastOrNull()
                    backStack.add(
                        if (ResourceUtil.isJdkMissing()) {
                            JDKSettingsScreen
                        } else {
                            LanguageServerSetupScreen
                        }
                    )
                }
            }

            is Home -> NavEntry(key) {
                HomeScreen(
                    onNavigateToEditor = { project ->
                        backStack.add(Editor(project))
                    },
                    onNavigateToNewProject = { backStack.add(NewProject) },
                    onNavigateToSettings = { backStack.add(Settings) })
            }

            is Editor -> NavEntry(key) {
                EditorScreen(key.project)
            }

            is NewProject -> NavEntry(key) {
                NewProjectScreen(onNavigateToEditor = {
                    backStack.add(Editor(it))
                }, onBack = {
                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                })
            }

            is GradleTask -> NavEntry(key) {
                GradleTaskScreen(project = key.project, task = key.task, onNavigateBack = {
                    if (backStack.size > 1) backStack.removeLastOrNull()
                }, onTaskSuccess = {})
            }

            is Settings -> NavEntry(key) {
                SettingsScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onNavigateToCategory = { category ->
                        backStack.add(SettingsCategoryScreen(category.destination))
                    })
            }

            is SettingsCategoryScreen -> NavEntry(key) {
                when (key.destination) {
                    SettingsDestination.EDITOR ->
                        EditorSettingsScreen(onBack = { backStack.removeLastOrNull() })

                    SettingsDestination.COMPILER ->
                        CompilerSettingsScreen(onBack = { backStack.removeLastOrNull() })

                    SettingsDestination.EXTENSIONS ->
                        ExtensionsSettingsScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onRunSetupInTerminal = { command ->
                                backStack.add(
                                    TerminalSession(
                                        command = command,
                                        workingDirectory = context.filesDir.absolutePath
                                    )
                                )
                            }
                        )

                    SettingsDestination.TERMINAL ->
                        TerminalScreen(onNavigateBack = { backStack.removeLastOrNull() })

                    SettingsDestination.TOOLCHAINS ->
                        JdkSettingsPanel { backStack.removeLastOrNull() }

                    SettingsDestination.ABOUT -> AboutSettingsScreen(gotoResourceScreen = {
                        backStack.add(
                            LanguageServerSetupScreen
                        )
                    }, onBack = { backStack.removeLastOrNull() })
                }
            }

            is JDKSettingsScreen -> NavEntry(key) {
                JdkSettingsPanel(onDismissRequested = {
                    if (!ResourceUtil.isJdkMissing()) {
                        backStack.removeLastOrNull()
                        backStack.add(
                            if (ResourceUtil.isLanguageServerSetupIncomplete()) {
                                LanguageServerSetupScreen
                            } else {
                                Home
                            }
                        )
                    }
                })
            }

            is LanguageServerSetupScreen -> NavEntry(key) {
                val setupScript = remember { ResourceUtil.prepareLanguageServerSetupScript() }
                TerminalScreen(
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                        backStack.add(
                            if (ResourceUtil.isEnvironmentIncomplete()) {
                                JDKSettingsScreen
                            } else {
                                Home
                            }
                        )
                    },
                    initialCommand = "bash ${setupScript.absolutePath} ${context.filesDir.absolutePath} ${context.cacheDir.absolutePath}",
                    workingDir = context.filesDir,
                    setup = true,
                    onProcessExit = { exitCode ->
                        if (exitCode == 0 && !ResourceUtil.isEnvironmentIncomplete()) {
                            backStack.removeLastOrNull()
                            backStack.add(Home)
                        }
                    }
                )
            }

            is TerminalSession -> NavEntry(key) {
                TerminalScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                    initialCommand = key.command,
                    workingDir = File(key.workingDirectory)
                )
            }

            else -> NavEntry(key) {
                Text("Unknown screen: $key")
            }
        }
    }
}
