/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */

package org.cosmicide.plugin.customproject

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.cosmicide.plugin.api.CosmicPlugin
import org.cosmicide.plugin.api.PluginContext
import org.cosmicide.plugin.api.PluginDescriptor
import org.cosmicide.plugin.runtime.AndroidPluginServices
import org.cosmicide.project.CommandExecutionService
import org.cosmicide.project.CommandRequest
import org.cosmicide.project.IdeServices
import org.cosmicide.project.Language
import org.cosmicide.project.OperationMessageKind
import org.cosmicide.project.OperationReporter
import org.cosmicide.project.OperationUpdate
import org.cosmicide.project.PluginFormField
import org.cosmicide.project.PluginFormFieldType
import org.cosmicide.project.PluginFormOption
import org.cosmicide.project.Project
import org.cosmicide.project.ProjectCommand
import org.cosmicide.project.ProjectCommandKind
import org.cosmicide.project.ProjectCommandProvider
import org.cosmicide.project.ProjectCreationProvider
import org.cosmicide.project.ProjectCreationRequest
import org.cosmicide.project.ProjectCreationResult
import org.cosmicide.project.ProjectExtensionPoints
import org.cosmicide.util.PreferenceKeys
import java.io.File
import java.util.UUID

data class CustomProjectCommandConfiguration(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val command: String
) {
    fun normalized() = copy(name = name.trim(), command = command.trim())

    fun validate() {
        require(name.isNotBlank()) { "Command name is required" }
        require(command.isNotBlank()) { "Command code is required" }
    }
}

data class CustomProjectTypeConfiguration(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val markerFiles: List<String> = emptyList(),
    val createCommand: String = "",
    /** Nullable so configurations saved before sync support migrate safely through Gson. */
    val syncCommand: String? = null,
    val buildCommand: String = "",
    val runCommand: String = "",
    val commands: List<CustomProjectCommandConfiguration> = emptyList(),
    val enabled: Boolean = true
) {
    fun normalized() = copy(
        name = name.trim(),
        markerFiles = markerFiles.map(String::trim).filter(String::isNotEmpty).distinct(),
        createCommand = createCommand.trim(),
        syncCommand = syncCommand.orEmpty().trim(),
        buildCommand = buildCommand.trim(),
        runCommand = runCommand.trim(),
        commands = commands.map(CustomProjectCommandConfiguration::normalized)
    )

    fun validate() {
        require(name.isNotBlank()) { "Project type name is required" }
        markerFiles.forEach { marker ->
            val path = File(marker)
            require(
                !path.isAbsolute && marker != ".." && !marker.startsWith("../") &&
                        !marker.contains("/../")
            ) {
                "Marker files must be relative paths inside the project"
            }
        }
        commands.forEach(CustomProjectCommandConfiguration::validate)
        require(commands.map { it.id }.distinct().size == commands.size) {
            "Command ids must be unique"
        }
    }
}

class CustomProjectTypeStore(context: Context) {
    private val preferences =
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun configurations(): List<CustomProjectTypeConfiguration> {
        val json = preferences.getString(PreferenceKeys.CUSTOM_PROJECT_TYPES, null)
            ?: return emptyList()
        return runCatching {
            gson.fromJson<List<CustomProjectTypeConfiguration>>(json, CONFIGURATION_LIST_TYPE)
                .orEmpty()
                .map(CustomProjectTypeConfiguration::normalized)
                .onEach(CustomProjectTypeConfiguration::validate)
        }.onFailure {
            Log.w(TAG, "Ignoring invalid custom project type configuration", it)
        }.getOrDefault(emptyList())
    }

    fun save(configuration: CustomProjectTypeConfiguration) {
        val normalized = configuration.normalized().also(CustomProjectTypeConfiguration::validate)
        write(configurations().filterNot { it.id == normalized.id } + normalized)
    }

    fun remove(id: String) {
        write(configurations().filterNot { it.id == id })
    }

    private fun write(configurations: List<CustomProjectTypeConfiguration>) {
        preferences.edit {
            putString(PreferenceKeys.CUSTOM_PROJECT_TYPES, gson.toJson(configurations))
        }
    }

    private companion object {
        val CONFIGURATION_LIST_TYPE =
            object : TypeToken<List<CustomProjectTypeConfiguration>>() {}.type
        const val TAG = "CustomProjectTypes"
    }
}

class CustomProjectTypePlugin : CosmicPlugin {
    override fun activate(context: PluginContext) {
        val appContext = context.services.require(AndroidPluginServices.APPLICATION_CONTEXT)
        val commands = context.services.require(IdeServices.COMMAND_EXECUTION)
        val store = CustomProjectTypeStore(appContext)
        val owner = context.descriptor.id

        context.registerDisposable(
            context.extensions.register(
                point = ProjectExtensionPoints.CREATION_PROVIDER,
                extension = CustomProjectCreationProvider(store, commands),
                ownerPluginId = owner,
                priority = 200
            )
        )
        context.registerDisposable(
            context.extensions.register(
                point = ProjectExtensionPoints.COMMAND_PROVIDER,
                extension = CustomProjectCommandProvider(store),
                ownerPluginId = owner,
                priority = 200
            )
        )
    }

    companion object {
        const val PLUGIN_ID = "org.cosmicide.custom-project-types"

        val descriptor = PluginDescriptor(
            id = PLUGIN_ID,
            name = "Custom project types",
            version = "1.0.0",
            entryClass = CustomProjectTypePlugin::class.java.name,
            description = "Register project templates and editor build, run, and utility commands.",
            author = "Cosmic IDE",
            capabilities = setOf("project.create", "project.commands", "process.execute")
        )
    }
}

private class CustomProjectCreationProvider(
    private val store: CustomProjectTypeStore,
    private val commands: CommandExecutionService
) : ProjectCreationProvider {
    override val id = "org.cosmicide.custom-project-types.create"
    override val displayName = "Create custom project"
    override val description = "Create a project using a user-registered project type."

    override fun isAvailable(): Boolean = store.configurations().any { it.enabled }

    override val fields: List<PluginFormField>
        get() {
            val configurations = store.configurations().filter { it.enabled }
            return listOf(
                PluginFormField(
                    id = FIELD_TYPE,
                    label = "Project type",
                    type = PluginFormFieldType.CHOICE,
                    defaultValue = configurations.first().id,
                    required = true,
                    options = configurations.map { PluginFormOption(it.id, it.name) }
                ),
                PluginFormField(
                    id = FIELD_NAME,
                    label = "Project name",
                    placeholder = "my-project",
                    required = true
                )
            )
        }

    override suspend fun create(
        request: ProjectCreationRequest,
        reporter: OperationReporter
    ): ProjectCreationResult {
        val configuration = store.configurations().firstOrNull {
            it.enabled && it.id == request.values[FIELD_TYPE]
        } ?: error("The selected custom project type is no longer available")
        val projectName = request.values[FIELD_NAME].orEmpty().trim()
        require(PROJECT_NAME.matches(projectName)) {
            "Project name must start with a letter or number and use only letters, numbers, '.', '_' or '-'"
        }

        val projectsDirectory = request.projectsDirectory.canonicalFile
        val root = projectsDirectory.resolve(projectName).canonicalFile
        require(root.parentFile == projectsDirectory) { "Invalid project destination" }
        require(!root.exists()) { "A project named '$projectName' already exists" }
        check(root.mkdirs()) { "Could not create the project directory" }

        try {
            if (configuration.createCommand.isNotBlank()) {
                reporter.report(OperationUpdate("Creating ${configuration.name} project…"))
                val result = commands.execute(
                    CommandRequest(
                        command = "bash",
                        arguments = listOf("-lc", configuration.createCommand),
                        workingDirectory = root,
                        environment = projectEnvironment(root, configuration)
                    )
                ) { output ->
                    reporter.report(
                        OperationUpdate(output, kind = OperationMessageKind.OUTPUT)
                    )
                }
                check(result.successful) {
                    result.output.lineSequence().lastOrNull { it.isNotBlank() }?.trim()
                        ?: "Creation command exited with code ${result.exitCode}"
                }
            }

            val metadata = root.resolve(PROJECT_TYPE_METADATA)
            metadata.parentFile?.mkdirs()
            metadata.writeText(configuration.id)
            return ProjectCreationResult(
                Project(root, Language.Kotlin),
                "Created $projectName as ${configuration.name}"
            )
        } catch (error: Throwable) {
            root.deleteRecursively()
            throw error
        }
    }

    private companion object {
        const val FIELD_TYPE = "projectType"
        const val FIELD_NAME = "name"
    }
}

private class CustomProjectCommandProvider(
    private val store: CustomProjectTypeStore
) : ProjectCommandProvider {
    override val id = "org.cosmicide.custom-project-types.commands"
    override val displayName = "Custom project commands"
    override val description = "Show registered build, run, and utility commands in the editor."

    override fun commands(project: Project): List<ProjectCommand> {
        return store.configurations()
            .filter { it.enabled && it.matches(project.root) }
            .flatMap { configuration -> configuration.toProjectCommands() }
    }
}

private fun CustomProjectTypeConfiguration.matches(root: File): Boolean {
    val metadataId = runCatching {
        root.resolve(PROJECT_TYPE_METADATA).takeIf(File::isFile)?.readText()?.trim()
    }.getOrNull()
    return metadataId == id || markerFiles.any { root.resolve(it).exists() }
}

private fun CustomProjectTypeConfiguration.toProjectCommands(): List<ProjectCommand> = buildList {
    syncCommand?.takeIf(String::isNotBlank)?.let { command ->
        add(ProjectCommand("$id.sync", "Sync ${name}", command, kind = ProjectCommandKind.SYNC))
    }
    if (buildCommand.isNotBlank()) {
        add(
            ProjectCommand(
                "$id.build",
                "Build ${name}",
                buildCommand,
                kind = ProjectCommandKind.BUILD
            )
        )
    }
    if (runCommand.isNotBlank()) {
        add(ProjectCommand("$id.run", "Run ${name}", runCommand, kind = ProjectCommandKind.RUN))
    }
    commands.forEach { custom ->
        add(ProjectCommand("$id.${custom.id}", custom.name, custom.command))
    }
}

private fun projectEnvironment(
    root: File,
    configuration: CustomProjectTypeConfiguration
) = mapOf(
    "COSMIC_PROJECT_ROOT" to root.absolutePath,
    "COSMIC_PROJECT_NAME" to root.name,
    "COSMIC_PROJECT_TYPE" to configuration.name
)

private const val PROJECT_TYPE_METADATA = ".cosmic/project-type"
private val PROJECT_NAME = Regex("[A-Za-z0-9][A-Za-z0-9._-]*")
