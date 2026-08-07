package org.cosmicide.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.cosmicide.project.Project

@Serializable
sealed interface Screen : NavKey

@Serializable
data object InstallResourceScreen : Screen

@Serializable
data object Home : Screen

@Serializable
data object NewProject : Screen

@Serializable
data class Editor(val project: Project) : Screen

@Serializable
data class GradleTask(val project: Project, val task: String) : Screen

@Serializable
data object Settings : Screen

@Serializable
data class SettingsCategoryScreen(val destination: SettingsDestination) : Screen

@Serializable
enum class SettingsDestination {
    EDITOR,
    COMPILER,
    EXTENSIONS,
    TERMINAL,
    TOOLCHAINS,
    ABOUT
}

@Serializable
data object JDKSettingsScreen : Screen

@Serializable
data object TerminalSetupScreen : Screen

@Serializable
data class TerminalSession(
    val command: String,
    val workingDirectory: String
) : Screen

@Serializable
data class PluginScreen(
    val pluginId: String,
    val screenId: String,
    val args: Map<String, String> = emptyMap()
) : Screen
