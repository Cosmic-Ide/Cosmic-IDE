package org.cosmicide.exec

import android.content.Context
import org.cosmicide.common.Prefs
import org.cosmicide.exec.linux.LinuxProcessRunner
import org.cosmicide.util.jdksDir
import java.io.File

/**
 * Simplified process execution utility that wraps LinuxProcessRunner
 * for common use cases like running gradlew, java commands, etc.
 */
object ProcessExecutor {

    /**
     * Starts a command with the standard Cosmic toolchain environment and returns the live process.
     *
     * Use this for long-running or bidirectional protocols where the caller needs direct access
     * to stdin/stdout instead of the one-way executeCommand callback.
     */
    fun startCommand(
        context: Context,
        command: String,
        args: List<String> = emptyList(),
        workingDir: File,
        redirectErrorStream: Boolean = true,
        environmentOverrides: Map<String, String> = emptyMap()
    ): Process {
        val appContext = context.applicationContext
        val jdkDir = appContext.jdksDir().resolve(Prefs.currentJDK)
        val pathEntries = LinuxProcessRunner.toolchainPathEntries(appContext, jdkDir)

        val binary = LinuxProcessRunner.resolveExecutable(
            commandName = command,
            workingDir = workingDir,
            pathEntries = pathEntries
        )

        val runnerConfig = LinuxProcessRunner.Configuration(
            binary = binary,
            arguments = args,
            workingDir = workingDir,
            environmentOverrides = toolchainEnvironment(appContext, jdkDir) + environmentOverrides,
            pathEntries = pathEntries,
            redirectErrorStream = redirectErrorStream
        )

        return LinuxProcessRunner.start(appContext, runnerConfig)
    }

    /**
     * Executes a command in the project directory with proper environment setup
     *
     * @param context Android context
     * @param command The command to execute (e.g., "./gradlew", "java")
     * @param args Command arguments
     * @param workingDir Working directory
     * @param onOutput Callback for output chunks
     */
    fun executeCommand(
        context: Context,
        command: String,
        args: List<String> = emptyList(),
        workingDir: File,
        onOutput: (String) -> Unit
    ) {
        val appContext = context.applicationContext
        val jdkDir = appContext.jdksDir().resolve(Prefs.currentJDK)
        val pathEntries = LinuxProcessRunner.toolchainPathEntries(appContext, jdkDir)

        val binary = LinuxProcessRunner.resolveExecutable(
            commandName = command,
            workingDir = workingDir,
            pathEntries = pathEntries
        )

        val runnerConfig = LinuxProcessRunner.Configuration(
            binary = binary,
            arguments = args,
            workingDir = workingDir,
            environmentOverrides = toolchainEnvironment(appContext, jdkDir),
            pathEntries = pathEntries
        )

        LinuxProcessRunner.execute(
            context = appContext,
            config = runnerConfig,
            onOutputReceived = onOutput,
            onProcessStarted = { _ -> }
        )
    }

    /**
     * Executes gradlew build in the specified project directory
     *
     * @param context Android context
     * @param projectRoot Project root directory (must contain gradlew)
     * @param onOutput Callback for output chunks
     */
    fun executeGradleBuild(
        context: Context,
        projectRoot: File,
        onOutput: (String) -> Unit
    ) {
        executeCommand(
            context = context,
            command = "./gradlew",
            args = listOf("build"),
            workingDir = projectRoot,
            onOutput = onOutput
        )
    }

    /**
     * Executes gradlew with custom task in the specified project directory
     *
     * @param context Android context
     * @param projectRoot Project root directory
     * @param task Gradle task to execute (e.g., "build", "clean", "test")
     * @param onOutput Callback for output chunks
     */
    fun executeGradleTask(
        context: Context,
        projectRoot: File,
        task: String,
        onOutput: (String) -> Unit
    ) {
        executeCommand(
            context = context,
            command = "./gradlew",
            args = listOf(task),
            workingDir = projectRoot,
            onOutput = onOutput
        )
    }

    private fun toolchainEnvironment(context: Context, jdkDir: File): Map<String, String> {
        return LinuxProcessRunner.toolchainEnvironment(
            jdkDir
        )
    }
}
