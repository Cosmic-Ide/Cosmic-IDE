/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */

package org.cosmicide.plugin.git

import org.cosmicide.plugin.api.CosmicPlugin
import org.cosmicide.plugin.api.PluginContext
import org.cosmicide.plugin.api.PluginDescriptor
import org.cosmicide.project.CommandExecutionService
import org.cosmicide.project.CommandRequest
import org.cosmicide.project.IdeServices
import org.cosmicide.project.Language
import org.cosmicide.project.OperationMessageKind
import org.cosmicide.project.OperationReporter
import org.cosmicide.project.OperationUpdate
import org.cosmicide.project.PluginFormField
import org.cosmicide.project.PluginFormFieldType
import org.cosmicide.project.Project
import org.cosmicide.project.ProjectAction
import org.cosmicide.project.ProjectActionProvider
import org.cosmicide.project.ProjectActionRequest
import org.cosmicide.project.ProjectActionResult
import org.cosmicide.project.ProjectCreationProvider
import org.cosmicide.project.ProjectCreationRequest
import org.cosmicide.project.ProjectCreationResult
import org.cosmicide.project.ProjectExtensionPoints
import java.io.File

class GitPlugin : CosmicPlugin {
    override fun activate(context: PluginContext) {
        val commands = context.services.require(IdeServices.COMMAND_EXECUTION)
        val owner = context.descriptor.id

        context.registerDisposable(
            context.extensions.register(
                point = ProjectExtensionPoints.CREATION_PROVIDER,
                extension = GitCloneProjectProvider(commands),
                ownerPluginId = owner,
                priority = 300
            )
        )
        context.registerDisposable(
            context.extensions.register(
                point = ProjectExtensionPoints.ACTION_PROVIDER,
                extension = GitProjectActionProvider(commands),
                ownerPluginId = owner,
                priority = 300
            )
        )
    }

    companion object {
        const val PLUGIN_ID = "org.cosmicide.git"

        val descriptor = PluginDescriptor(
            id = PLUGIN_ID,
            name = "Git",
            version = "1.0.0",
            entryClass = GitPlugin::class.java.name,
            description = "Clone repositories and run Git operations using Cosmic's Linux environment.",
            author = "Cosmic IDE",
            capabilities = setOf("process.execute", "project.create", "project.actions")
        )
    }
}

private class GitCloneProjectProvider(
    private val commands: CommandExecutionService
) : ProjectCreationProvider {
    override val id = "org.cosmicide.git.clone"
    override val displayName = "Clone Git repository"
    override val description = "Clone a remote repository into the Cosmic projects directory."
    override val actionLabel = "Clone"
    override val fields = listOf(
        PluginFormField(
            id = FIELD_URL,
            label = "Repository URL",
            placeholder = "https://github.com/owner/repository.git",
            required = true
        ),
        PluginFormField(
            id = FIELD_DIRECTORY,
            label = "Project directory",
            description = "Leave blank to use the repository name.",
            placeholder = "repository"
        ),
        PluginFormField(
            id = FIELD_BRANCH,
            label = "Branch or tag",
            description = "Leave blank to use the remote default branch."
        ),
        PluginFormField(
            id = FIELD_SHALLOW,
            label = "Shallow clone (latest revision only)",
            type = PluginFormFieldType.BOOLEAN,
            defaultValue = "false"
        )
    )

    override suspend fun create(
        request: ProjectCreationRequest,
        reporter: OperationReporter
    ): ProjectCreationResult {
        val repositoryUrl = request.values[FIELD_URL].orEmpty().trim()
        require(repositoryUrl.isNotEmpty()) { "Repository URL is required" }
        require(!repositoryUrl.startsWith("-")) { "Repository URL cannot start with '-'" }

        val requestedDirectory = request.values[FIELD_DIRECTORY].orEmpty().trim()
        val directoryName = (requestedDirectory.ifEmpty { repositoryName(repositoryUrl) })
        require(PROJECT_DIRECTORY.matches(directoryName)) {
            "Project directory must start with a letter or number and use only letters, numbers, '.', '_' or '-'"
        }

        val projectsDirectory = request.projectsDirectory.canonicalFile
        val destination = projectsDirectory.resolve(directoryName).canonicalFile
        require(destination.parentFile == projectsDirectory) { "Invalid project directory" }
        require(!destination.exists()) { "A project named '$directoryName' already exists" }

        val branch = request.values[FIELD_BRANCH].orEmpty().trim()
        require(branch.isEmpty() || isSafeRef(branch)) { "Invalid branch or tag name" }

        val arguments = buildList {
            add("clone")
            add("--progress")
            if (request.values[FIELD_SHALLOW].toBoolean()) {
                add("--depth")
                add("1")
            }
            if (branch.isNotEmpty()) {
                add("--branch")
                add(branch)
            }
            add(repositoryUrl)
            add(directoryName)
        }

        reporter.report(OperationUpdate("Cloning $repositoryUrl…"))
        try {
            runGit(commands, projectsDirectory, arguments, reporter)
        } catch (error: Throwable) {
            if (destination.exists()) destination.deleteRecursively()
            throw error
        }

        val project = Project(destination, detectLanguage(destination))
        return ProjectCreationResult(project, "Cloned ${project.name}")
    }

    private companion object {
        const val FIELD_URL = "url"
        const val FIELD_DIRECTORY = "directory"
        const val FIELD_BRANCH = "branch"
        const val FIELD_SHALLOW = "shallow"
    }
}

private class GitProjectActionProvider(
    private val commands: CommandExecutionService
) : ProjectActionProvider {
    override val id = "org.cosmicide.git.operations"
    override val displayName = "Git operations"
    override val description =
        "Status, fetch, pull, push, stage, commit, branch and checkout actions."

    override fun actions(project: Project): List<ProjectAction> {
        if (!project.root.resolve(".git").exists()) {
            return listOf(ProjectAction(ACTION_INIT, "Initialize Git repository"))
        }

        return listOf(
            ProjectAction(ACTION_STATUS, "Git status"),
            ProjectAction(ACTION_FETCH, "Fetch"),
            ProjectAction(ACTION_PULL, "Pull"),
            ProjectAction(ACTION_PUSH, "Push"),
            ProjectAction(ACTION_STAGE_ALL, "Stage all changes"),
            ProjectAction(
                ACTION_COMMIT,
                "Commit staged changes",
                fields = listOf(
                    PluginFormField(
                        id = FIELD_MESSAGE,
                        label = "Commit message",
                        required = true
                    )
                )
            ),
            ProjectAction(ACTION_BRANCHES, "List branches"),
            ProjectAction(
                ACTION_CHECKOUT,
                "Checkout branch or tag",
                fields = listOf(
                    PluginFormField(
                        id = FIELD_REF,
                        label = "Branch or tag",
                        required = true
                    )
                )
            )
        )
    }

    override suspend fun execute(
        request: ProjectActionRequest,
        reporter: OperationReporter
    ): ProjectActionResult {
        val arguments = when (request.actionId) {
            ACTION_INIT -> listOf("init")
            ACTION_STATUS -> listOf("status", "--short", "--branch")
            ACTION_FETCH -> listOf("fetch", "--all", "--prune", "--progress")
            ACTION_PULL -> listOf("pull", "--progress")
            ACTION_PUSH -> listOf("push", "--progress")
            ACTION_STAGE_ALL -> listOf("add", "--all")
            ACTION_COMMIT -> {
                val message = request.values[FIELD_MESSAGE].orEmpty().trim()
                require(message.isNotEmpty()) { "Commit message is required" }
                listOf("commit", "-m", message)
            }

            ACTION_BRANCHES -> listOf("branch", "--all", "--verbose")
            ACTION_CHECKOUT -> {
                val ref = request.values[FIELD_REF].orEmpty().trim()
                require(isSafeRef(ref)) { "Invalid branch or tag name" }
                listOf("checkout", ref)
            }

            else -> error("Unknown Git action: ${request.actionId}")
        }

        reporter.report(OperationUpdate("Running git ${arguments.first()}…"))
        val result = runGit(commands, request.project.root, arguments, reporter)
        val message = result.output
            .lineSequence()
            .lastOrNull { it.isNotBlank() }
            ?.trim()
            ?: "Git operation completed"
        return ProjectActionResult(
            message = message,
            refreshProject = request.actionId == ACTION_INIT ||
                    request.actionId == ACTION_PULL ||
                    request.actionId == ACTION_CHECKOUT
        )
    }

    private companion object {
        const val ACTION_INIT = "init"
        const val ACTION_STATUS = "status"
        const val ACTION_FETCH = "fetch"
        const val ACTION_PULL = "pull"
        const val ACTION_PUSH = "push"
        const val ACTION_STAGE_ALL = "stageAll"
        const val ACTION_COMMIT = "commit"
        const val ACTION_BRANCHES = "branches"
        const val ACTION_CHECKOUT = "checkout"
        const val FIELD_MESSAGE = "message"
        const val FIELD_REF = "ref"
    }
}

private suspend fun runGit(
    commands: CommandExecutionService,
    workingDirectory: File,
    arguments: List<String>,
    reporter: OperationReporter
) = commands.execute(
    CommandRequest(
        command = "git",
        arguments = arguments,
        workingDirectory = workingDirectory,
        environment = mapOf("GIT_TERMINAL_PROMPT" to "0")
    )
) { chunk ->
    val percentage = gitProgress(chunk)
    reporter.report(
        OperationUpdate(
            message = chunk,
            progress = percentage,
            kind = OperationMessageKind.OUTPUT
        )
    )
}.also { result ->
    check(result.successful) {
        result.output.lineSequence().lastOrNull { it.isNotBlank() }?.trim()
            ?: "Git exited with code ${result.exitCode}"
    }
}

internal fun repositoryName(url: String): String {
    val path = url.trimEnd('/').substringAfterLast('/').substringAfterLast(':')
    return path.removeSuffix(".git").ifBlank { "repository" }
}

internal fun isSafeRef(ref: String): Boolean {
    return ref.isNotBlank() &&
            !ref.startsWith('-') &&
            !ref.endsWith('.') &&
            !ref.endsWith('/') &&
            !ref.contains("..") &&
            !ref.contains("@{") &&
            SAFE_REF.matches(ref)
}

internal fun gitProgress(chunk: String): Float? {
    return PROGRESS.findAll(chunk)
        .lastOrNull()
        ?.groupValues
        ?.get(1)
        ?.toFloatOrNull()
        ?.div(100f)
}

private fun detectLanguage(root: File): Language = when {
    hasSourceDirectory(root, "java") -> Language.Java
    hasSourceDirectory(root, "scala") -> Language.Scala
    else -> Language.Kotlin
}

private fun hasSourceDirectory(root: File, language: String): Boolean {
    return root.resolve("src/main/$language").isDirectory ||
            root.resolve("app/src/main/$language").isDirectory
}

private val PROJECT_DIRECTORY = Regex("[A-Za-z0-9][A-Za-z0-9._-]*")
private val SAFE_REF = Regex("[A-Za-z0-9][A-Za-z0-9._/-]*")
private val PROGRESS = Regex("(\\d{1,3})%")
