/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.git

import androidx.compose.runtime.Composable
import org.cosmicide.editor.EditorAction
import org.cosmicide.editor.EditorActionProvider
import org.cosmicide.editor.EditorExtensionPoints
import org.cosmicide.plugin.api.CosmicPlugin
import org.cosmicide.plugin.api.PluginContext
import org.cosmicide.plugin.api.PluginDescriptor
import org.cosmicide.plugin.git.ui.GitScreen
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
import org.cosmicide.project.ProjectCreationProvider
import org.cosmicide.project.ProjectCreationRequest
import org.cosmicide.project.ProjectCreationResult
import org.cosmicide.project.ProjectExtensionPoints
import org.cosmicide.ui.PluginScreenProvider
import org.cosmicide.ui.UiExtensionPoints
import java.io.File

class GitPlugin : CosmicPlugin {
    override fun activate(context: PluginContext) {
        val commands = context.services.require(IdeServices.COMMAND_EXECUTION)
        val gitService = GitService(commands)
        val owner = context.descriptor.id

        context.registerDisposable(
            context.extensions.register(
                point = UiExtensionPoints.PLUGIN_SCREEN,
                extension = GitScreenProvider(gitService),
                ownerPluginId = owner,
                priority = 300
            )
        )
        context.registerDisposable(
            context.extensions.register(
                point = EditorExtensionPoints.EDITOR_ACTION_PROVIDER,
                extension = GitEditorActionProvider(),
                ownerPluginId = owner,
                priority = 300
            )
        )
        context.registerDisposable(
            context.extensions.register(
                point = ProjectExtensionPoints.CREATION_PROVIDER,
                extension = GitCloneProjectProvider(commands),
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
            version = "2.0.0",
            entryClass = GitPlugin::class.java.name,
            description = "Integrated Git version control with dedicated changes, branches, history, and settings tabs.",
            author = "Cosmic IDE",
            capabilities = setOf(
                "process.execute",
                "project.create",
                "ui.screen",
                "ui.settings",
                "editor.action"
            )
        )
    }
}

private class GitScreenProvider(
    private val gitService: GitService
) : PluginScreenProvider {
    override val id = "org.cosmicide.git.screen"
    override val screenId = "git"
    override val title = "Git"
    override val displayName = "Git"
    override val description = "Version control and Git management screen"

    @Composable
    override fun Content(args: Map<String, String>) {
        GitScreen(gitService = gitService, args = args)
    }

    @Composable
    override fun Content() {
        GitScreen(gitService = gitService, args = emptyMap())
    }
}

private class GitEditorActionProvider : EditorActionProvider {
    override val id = "org.cosmicide.git.editorAction"
    override val displayName = "Git"
    override val description = "Open Git version control screen from the editor"

    override fun actions(project: Project, file: File?): List<EditorAction> {
        return listOf(
            EditorAction(
                id = "git",
                label = "Git",
                description = "Open Git version control for ${project.name}"
            ) { context ->
                context.navigateToPluginScreen(
                    screenId = "git",
                    args = mapOf("project_path" to context.project.root.absolutePath)
                )
            }
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

        val project = Project(destination, Language.Empty)
        return ProjectCreationResult(project, "Cloned ${project.name}")
    }

    private companion object {
        const val FIELD_URL = "url"
        const val FIELD_DIRECTORY = "directory"
        const val FIELD_BRANCH = "branch"
        const val FIELD_SHALLOW = "shallow"
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

private val PROJECT_DIRECTORY = Regex("[A-Za-z0-9][A-Za-z0-9._-]*")
private val SAFE_REF = Regex("[A-Za-z0-9][A-Za-z0-9._/-]*")
private val PROGRESS = Regex("(\\d{1,3})%")
