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
    val options: List<PluginFormOption> = emptyList(),
    val visible: Boolean = true,
    /** Field id to required value mapping for conditional visibility. */
    val visibleWhen: Map<String, String> = emptyMap()
) {
    init {
        require(id.isNotBlank()) { "Field id must not be blank" }
        require(label.isNotBlank()) { "Field label must not be blank" }
        require(type != PluginFormFieldType.CHOICE || options.isNotEmpty()) {
            "Choice field '$id' must provide at least one option"
        }
        require(visibleWhen.keys.all { it.isNotBlank() }) {
            "Conditional visibility field ids must not be blank"
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

/**
 * A node in the project command tree shown by the editor.
 *
 * Leaf nodes provide trusted shell [command] text and open an interactive project terminal tab.
 * Branch nodes leave [command] blank and use [children] to create a submenu.
 */
data class ProjectCommand(
    val id: String,
    val label: String,
    val command: String = "",
    val description: String = "",
    val kind: ProjectCommandKind = ProjectCommandKind.OTHER,
    val children: List<ProjectCommand> = emptyList()
) {
    init {
        require(id.isNotBlank()) { "Project command id must not be blank" }
        require(label.isNotBlank()) { "Project command label must not be blank" }
        require(command.isNotBlank() || children.isNotEmpty()) {
            "Project command must provide shell text or child commands"
        }
        require(command.isBlank() || children.isEmpty()) {
            "Project command cannot provide both shell text and child commands"
        }
        require(children.isEmpty() || kind == ProjectCommandKind.OTHER) {
            "Project command groups cannot have an executable command kind"
        }
    }
}

/** Contributes command trees which the editor renders as submenus and opens in its bottom PTY. */
interface ProjectCommandProvider : ConfigurableExtension {
    fun commands(project: Project): List<ProjectCommand>
}

/**
 * A discoverable build-system task shown in the editor's task picker.
 *
 * [command] is trusted shell text executed in an interactive project terminal. [group] is a
 * user-facing category such as "Lifecycle" or "Verification".
 */
data class ProjectTask(
    val id: String,
    val label: String,
    val command: String,
    val description: String = "",
    val group: String = ""
) {
    init {
        require(id.isNotBlank()) { "Project task id must not be blank" }
        require(label.isNotBlank()) { "Project task label must not be blank" }
        require(command.isNotBlank()) { "Project task command must not be blank" }
    }
}

/**
 * Discovers build-system tasks for a matching project.
 *
 * Discovery is suspendable so providers may query a build model or process without blocking the
 * editor. Implementations should return stable task ids and preserve their preferred display order.
 */
interface ProjectTaskProvider : ConfigurableExtension {
    fun supports(project: Project): Boolean

    suspend fun tasks(project: Project): List<ProjectTask>
}

/**
 * Identifies a project layout that Cosmic does not know about itself.
 *
 * Installed language plugins should register one of these alongside their editor and project
 * creation contributions. The provider is consulted whenever the Projects screen scans a
 * directory, so the project keeps its language identity after an app restart.
 */
interface ProjectTypeProvider : ConfigurableExtension {
    val languageName: String

    val fileExtension: String

    fun supports(projectRoot: File): Boolean

    fun project(projectRoot: File): Project {
        return Project(
            root = projectRoot,
            language = Language.Custom(languageName, fileExtension)
        )
    }
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

/** App-owned process execution exposed to plugins with the Cosmic environment. */
interface CommandExecutionService {
    fun isCommandAvailable(command: String, workingDirectory: File): Boolean

    suspend fun execute(
        request: CommandRequest,
        onOutput: (String) -> Unit = {}
    ): CommandResult
}

/** Starts a long-lived process in Cosmic's toolchain environment. */
fun interface ToolProcessService {
    fun start(request: CommandRequest, redirectErrorStream: Boolean): Process
}

object ProjectExtensionPoints {
    @JvmField
    val TYPE_PROVIDER = ExtensionPoint(
        "org.cosmicide.project.typeProvider",
        ProjectTypeProvider::class.java
    )

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

    @JvmField
    val TASK_PROVIDER = ExtensionPoint(
        "org.cosmicide.project.taskProvider",
        ProjectTaskProvider::class.java
    )
}

object IdeServices {
    @JvmField
    val COMMAND_EXECUTION = ServiceKey(
        "org.cosmicide.ide.commandExecution",
        CommandExecutionService::class.java
    )

    @JvmField
    val TOOL_PROCESS = ServiceKey(
        "org.cosmicide.ide.toolProcess",
        ToolProcessService::class.java
    )
}
