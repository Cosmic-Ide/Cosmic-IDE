package org.cosmicide.ui.project

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cosmicide.project.Language
import org.cosmicide.project.Project
import org.cosmicide.tooling.RemoteGradleConnector
import org.cosmicide.tooling.ToolingServerManager
import org.cosmicide.util.FileUtil
import java.io.File
import java.io.IOException
import java.io.OutputStream

internal data class GradleProjectCreationRequest(
    val language: Language,
    val name: String,
    val packageName: String,
    val dslType: DslType,
    val splitProject: Boolean,
    val testFramework: TestFramework
)

internal interface GradleProjectCreator {
    suspend fun create(
        request: GradleProjectCreationRequest,
        onLog: (String) -> Unit
    ): Result<Project>
}

internal class AndroidGradleProjectCreator(
    context: Context,
    private val projectsDirectory: File = FileUtil.projectDir
) : GradleProjectCreator {
    private val appContext = context.applicationContext

    override suspend fun create(
        request: GradleProjectCreationRequest,
        onLog: (String) -> Unit
    ): Result<Project> = withContext(Dispatchers.IO) {
        runCatching {
            val canonicalProjectsDirectory = projectsDirectory.canonicalFile
            val root = canonicalProjectsDirectory.resolve(request.name).canonicalFile

            require(root.parentFile == canonicalProjectsDirectory) { "Invalid project title" }
            require(!root.exists()) { "A project with this title already exists" }
            check(root.mkdirs()) { "Could not create the project directory" }

            val standardOutput = ProjectCreationOutputStream(onLog)
            val standardError = ProjectCreationOutputStream(onLog)

            try {
                onLog("Starting Gradle Tooling API provider...\n")
                val connection = RemoteGradleConnector.forProject(appContext, root).connect()

                try {
                    onLog("Running Gradle init (${request.language.gradleInitType})...\n")
                    connection.newBuild()
                        .forTasks("init")
                        .withArguments(gradleInitArguments(request))
                        .setStandardOutput(standardOutput)
                        .setStandardError(standardError)
                        .run()
                } finally {
                    connection.close()
                    ToolingServerManager.stopCurrent()
                }

                onLog("Project created successfully.\n")
                Project(root = root, language = request.language)
            } catch (error: Throwable) {
                ToolingServerManager.stopCurrent()
                root.deleteRecursively()

                val gradleMessage = standardError.lastNonBlankLine()
                    ?: error.message
                    ?: "unknown Gradle error"
                onLog("Project creation failed: $gradleMessage\n")
                throw IOException("Gradle project creation failed: $gradleMessage", error)
            }
        }
    }
}

internal fun gradleInitArguments(request: GradleProjectCreationRequest): List<String> = listOf(
    "init",
    "--type=${request.language.gradleInitType}",
    "--dsl=${request.dslType.gradleValue}",
    "--project-name=${request.name}",
    "--package=${request.packageName}",
    if (request.splitProject) "--split-project" else "--no-split-project",
    "--test-framework=${request.testFramework.gradleValue}",
    "--use-defaults",
    "--no-incubating",
    "--no-comments"
)

internal val Language.gradleInitType: String
    get() = when (this) {
        Language.Java -> "java-application"
        Language.Kotlin -> "kotlin-application"
        Language.Scala -> "scala-application"
    }

private class ProjectCreationOutputStream(
    private val onText: (String) -> Unit
) : OutputStream() {
    private val captured = StringBuilder()

    @Synchronized
    override fun write(value: Int) {
        write(byteArrayOf(value.toByte()), 0, 1)
    }

    @Synchronized
    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        if (length <= 0) return

        val text = String(bytes, offset, length, Charsets.UTF_8)
        captured.append(text)
        if (captured.length > MAX_CAPTURED_LOG_CHARS) {
            captured.delete(0, captured.length - MAX_CAPTURED_LOG_CHARS)
        }
        onText(text)
    }

    @Synchronized
    fun lastNonBlankLine(): String? =
        captured.lineSequence().lastOrNull { it.isNotBlank() }

    private companion object {
        const val MAX_CAPTURED_LOG_CHARS = 8_000
    }
}
