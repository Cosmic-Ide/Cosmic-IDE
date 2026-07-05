package org.cosmicide.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.cosmicide.ui.compile.CompileInfoScreen
import org.cosmicide.ui.editor.EditorScreen
import org.cosmicide.ui.home.HomeScreen
import org.cosmicide.ui.output.ProjectOutputScreen
import org.cosmicide.ui.project.NewProjectScreen
import org.cosmicide.ui.resource.InstallResourcesScreen
import org.cosmicide.ui.resource.JdkSettingsPanel
import org.cosmicide.ui.settings.AboutSettingsScreen
import org.cosmicide.ui.settings.CompilerSettingsScreen
import org.cosmicide.ui.settings.EditorSettingsScreen
import org.cosmicide.ui.settings.FormatterSettingsScreen
import org.cosmicide.ui.settings.GitSettingsScreen
import org.cosmicide.ui.settings.PluginsSettingsScreen
import org.cosmicide.ui.settings.SettingsScreen
import org.cosmicide.util.ProjectHandler
import org.cosmicide.util.ResourceUtil

@Composable
fun IDENavigation() {
    val backStack = rememberNavBackStack(
        if (ResourceUtil.isEnvironmentIncomplete()) {
            InstallResourceScreen
        } else {
            Home
        }
    )

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
                    backStack.add(JDKSettingsScreen)
                }
            }

            is Home -> NavEntry(key) {
                HomeScreen(
                    onNavigateToEditor = { project ->
                    ProjectHandler.setProject(project)
                    backStack.add(Editor(project))
                },
                    onNavigateToNewProject = { backStack.add(NewProject) },
                    onNavigateToSettings = { backStack.add(Settings) })
            }

            is Editor -> NavEntry(key) {
                EditorScreen(key.project, onCompile = {
                    backStack.add(CompileInfo)
                })
            }

            is NewProject -> NavEntry(key) {
                NewProjectScreen(onNavigateToEditor = {
                    backStack.add(Editor(it))
                }, onBack = {
                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                })
            }

            is CompileInfo -> NavEntry(key) {
                CompileInfoScreen(onNavigateBack = {
                    if (backStack.size > 1) backStack.removeLastOrNull()
                }, onCompileSuccess = {
                    backStack.add(ProjectOutput)
                })
            }

            is ProjectOutput -> NavEntry(key) {
                ProjectOutputScreen {
                    if (backStack.size > 1) {
                        backStack.removeLastOrNull() // remove output screen
                    }
                }
            }

            is Settings -> NavEntry(key) {
                SettingsScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onNavigateToCategory = { category ->
                        backStack.add(SettingsCategoryScreen(category.title))
                    })
            }

            is SettingsCategoryScreen -> NavEntry(key) {
                when (key.category) {
                    "Code editor" -> EditorSettingsScreen(onBack = { backStack.removeLastOrNull() })
                    "Compiler" -> CompilerSettingsScreen(onBack = { backStack.removeLastOrNull() })
                    "Formatter" -> FormatterSettingsScreen(onBack = { backStack.removeLastOrNull() })
                    "Plugins" -> PluginsSettingsScreen(onBack = { backStack.removeLastOrNull() })
                    "Git" -> GitSettingsScreen(onBack = { backStack.removeLastOrNull() })
                    "Toolchains" -> JdkSettingsPanel { backStack.removeLastOrNull() }
                    "About" -> AboutSettingsScreen(onBack = { backStack.removeLastOrNull() })
                    else -> Text("Category: ${key.category}")
                }
            }

            is JDKSettingsScreen -> NavEntry(key) {
                JdkSettingsPanel(onDismissRequested = {
                    backStack.add(Home); backStack.removeAt(
                    backStack.size - 2
                )
                })
            }

            else -> NavEntry(key) {
                Text("Unknown screen: $key")
            }
        }
    }
}