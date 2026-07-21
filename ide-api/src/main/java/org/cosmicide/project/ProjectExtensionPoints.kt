/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */

package org.cosmicide.project

import org.cosmicide.plugin.api.ConfigurableExtension
import org.cosmicide.plugin.api.ExtensionPoint
import org.cosmicide.plugin.api.ServiceKey
import java.io.File

/** UI-neutral fields which the app can render for plugin-provided operations. */
data class PluginFormField(
    val id: String,
    val label: String,
    val type: PluginFormFieldType = PluginFormFieldType.TEXT,
    val description: String = "",
    val placeholder: String = "",
    val defaultValue: String = "",
    val required: Boolean = false,
    val options: List<PluginFormOption> = emptyList()
) {
    init {
        require(id.isNotBlank()) { "Field id must not be blank" }
        require(label.isNotBlank()) { "Field label must not be blank" }
        require(type != PluginFormFieldType.CHOICE || options.isNotEmpty()) {
            "Choice field '$id' must provide at least one option"
        }
    }
}

data class PluginFormOption(
    val value: String,
    val label: String
) {
    init {
        require(value.isNotBlank()) { "Option value must not be blank" }
        require(label.isNotBlank()) { "Option label must not be blank" }
    }
}

enum class PluginFormFieldType {
    TEXT,
    PASSWORD,
    BOOLEAN,
    CHOICE
}

/** A command that must be shown in Cosmic's interactive terminal. */
data class TerminalAction(
    val id: String,
    val label: String,
    val command: String,
    val description: String = ""
) {
    init {
        require(id.isNotBlank()) { "Terminal action id must not be blank" }
        require(label.isNotBlank()) { "Terminal action label must not be blank" }
        require(command.isNotBlank()) { "Terminal command must not be blank" }
    }
}

enum class OperationMessageKind {
    STATUS,
    OUTPUT,
    WARNING,
    ERROR
}

data class OperationUpdate(
    val message: String,
    /** A normalized progress value, or null when progress is indeterminate. */
    val progress: Float? = null,
    val kind: OperationMessageKind = OperationMessageKind.STATUS
) {
    init {
        require(progress == null || progress in 0f..1f) { "Progress must be between 0 and 1" }
    }
}

fun interface OperationReporter {
    fun report(update: OperationUpdate)
}

data class ProjectCreationRequest(
    val projectsDirectory: File,
    val values: Map<String, String>
)

data class ProjectCreationResult(
    val project: Project,
    val message: String = "Project created successfully"
)

/** Contributes a new way to create or import a project on the home screen. */
interface ProjectCreationProvider : ConfigurableExtension {
    fun isAvailable(): Boolean = true

    val fields: List<PluginFormField>

    val actionLabel: String
        get() = "Create"

    /** Optional setup tasks, such as installing a required command-line tool. */
    val setupActions: List<TerminalAction>
        get() = emptyList()

    suspend fun create(
        request: ProjectCreationRequest,
        reporter: OperationReporter
    ): ProjectCreationResult
}

data class ProjectAction(
    val id: String,
    val label: String,
    val description: String = "",
    val fields: List<PluginFormField> = emptyList(),
    val destructive: Boolean = false
) {
    init {
        require(id.isNotBlank()) { "Project action id must not be blank" }
        require(label.isNotBlank()) { "Project action label must not be blank" }
    }
}

data class ProjectActionRequest(
    val project: Project,
    val actionId: String,
    val values: Map<String, String>
)

data class ProjectActionResult(
    val message: String,
    val refreshProject: Boolean = false
)

/** Contributes operations to each matching project's overflow menu. */
interface ProjectActionProvider : ConfigurableExtension {
    val setupActions: List<TerminalAction>
        get() = emptyList()

    fun actions(project: Project): List<ProjectAction>

    suspend fun execute(
        request: ProjectActionRequest,
        reporter: OperationReporter
    ): ProjectActionResult
}

enum class ProjectCommandKind {
    SYNC,
    BUILD,
    RUN,
    OTHER
}

/** A trusted shell command rendered as an interactive project terminal tab. */
data class ProjectCommand(
    val id: String,
    val label: String,
    val command: String,
    val description: String = "",
    val kind: ProjectCommandKind = ProjectCommandKind.OTHER
) {
    init {
        require(id.isNotBlank()) { "Project command id must not be blank" }
        require(label.isNotBlank()) { "Project command label must not be blank" }
        require(command.isNotBlank()) { "Project command must not be blank" }
    }
}

/** Contributes commands which the editor opens in its bottom PTY tool window. */
interface ProjectCommandProvider : ConfigurableExtension {
    fun commands(project: Project): List<ProjectCommand>
}

data class CommandRequest(
    val command: String,
    val arguments: List<String> = emptyList(),
    val workingDirectory: File,
    val environment: Map<String, String> = emptyMap()
) {
    init {
        require(command.isNotBlank()) { "Command must not be blank" }
    }
}

data class CommandResult(
    val exitCode: Int,
    val output: String
) {
    val successful: Boolean
        get() = exitCode == 0
}

/** App-owned process execution exposed to plugins with the Cosmic glibc environment. */
interface CommandExecutionService {
    fun isCommandAvailable(command: String, workingDirectory: File): Boolean

    suspend fun execute(
        request: CommandRequest,
        onOutput: (String) -> Unit = {}
    ): CommandResult
}

object ProjectExtensionPoints {
    @JvmField
    val CREATION_PROVIDER = ExtensionPoint(
        "org.cosmicide.project.creationProvider",
        ProjectCreationProvider::class.java
    )

    @JvmField
    val ACTION_PROVIDER = ExtensionPoint(
        "org.cosmicide.project.actionProvider",
        ProjectActionProvider::class.java
    )

    @JvmField
    val COMMAND_PROVIDER = ExtensionPoint(
        "org.cosmicide.project.commandProvider",
        ProjectCommandProvider::class.java
    )
}

object IdeServices {
    @JvmField
    val COMMAND_EXECUTION = ServiceKey(
        "org.cosmicide.ide.commandExecution",
        CommandExecutionService::class.java
    )
}
