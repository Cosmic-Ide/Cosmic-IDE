package org.cosmicide.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.cosmicide.app.LocalAppContainer
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

/**
 * Prepares a command to run in an interactive bash shell and then exit.
 * Uses double quotes to wrap the command, allowing bash features like process substitution to work.
 */
private fun commandWithExit(command: String): String {
    // Escape characters that need escaping in double quotes: backslash, double quote, dollar, backtick
    val escaped = command
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("$", "\\$")
        .replace("`", "\\`")
    return "bash -i -c \"$escaped && exit\""
}

@Composable
fun IDENavigation() {
    val context = LocalContext.current
    val projectSessionServices = LocalAppContainer.current.projectSessionServices
    val initialScreen: Screen = when {
        ResourceUtil.isBootstrapIncomplete() -> InstallResourceScreen
        ResourceUtil.isEnvironmentIncomplete() -> TerminalSetupScreen
        else -> Home
    }
    val backStack = rememberNavBackStack(
        initialScreen
    )

    DisposableEffect(Unit) {
        onDispose {
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
                        TerminalSetupScreen
                    )
                }
            }

            is Home -> NavEntry(key) {
                HomeScreen(
                    onNavigateToEditor = { project ->
                        backStack.add(Editor(project))
                    },
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
                                        command = commandWithExit(command),
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
                            TerminalSetupScreen
                        )
                    }, onBack = { backStack.removeLastOrNull() })
                }
            }

            is JDKSettingsScreen -> NavEntry(key) {
                JdkSettingsPanel(onDismissRequested = {
                        backStack.removeLastOrNull()
                        backStack.add(
                            Home
                        )
                })
            }

            is TerminalSetupScreen -> NavEntry(key) {
                val setupScript = remember { ResourceUtil.prepareLanguageServerSetupScript() }
                TerminalScreen(
                    onNavigateBack = {
                        backStack.removeLastOrNull()
                        backStack.add(Home)
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
