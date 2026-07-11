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
data class GradleTask(val task: String) : Screen

@Serializable
data object ProjectOutput : Screen

@Serializable
data object Settings : Screen

@Serializable
data class SettingsCategoryScreen(val category: String) : Screen

@Serializable
data object JDKSettingsScreen : Screen

@Serializable
data object LanguageServerSetupScreen : Screen
