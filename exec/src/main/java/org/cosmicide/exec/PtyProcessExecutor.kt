package org.cosmicide.exec

import android.content.Context
import org.cosmicide.common.Prefs
import org.cosmicide.exec.linux.LinuxProcessRunner
import org.cosmicide.exec.linux.PtyProcess
import org.cosmicide.util.jdksDir
import java.io.File

/**
 * Simplified PTY process execution utility that wraps LinuxProcessRunner
 * for interactive terminal use cases. Provides clean API similar to ProcessExecutor
 * but for PTY-based processes with terminal I/O support.
 */
object PtyProcessExecutor {

    /**
     * Starts a command in a PTY with the standard Cosmic toolchain environment.
     * Returns a PtyProcess that can be used for interactive terminal I/O.
     *
     * Use this for interactive commands that need terminal features like:
     * - ANSI color support
     * - Input handling (readln, etc.)
     * - Terminal resizing
     * - Proper signal handling (Ctrl+C, etc.)
     *
     * @param context Android context
     * @param command The command to execute (e.g., "./gradlew", "bash", "python")
     * @param args Command arguments
     * @param workingDir Working directory
     * @param terminalRows Initial terminal rows (default: 25)
     * @param terminalColumns Initial terminal columns (default: 80)
     * @return PtyProcess with PTY attached
     */
    fun startTerminalCommand(
        context: Context,
        command: String,
        args: List<String> = emptyList(),
        workingDir: File,
        terminalRows: Int = 25,
        terminalColumns: Int = 80
    ): PtyProcess {
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
            pathEntries = pathEntries,
            usePty = true,
            terminalRows = terminalRows,
            terminalColumns = terminalColumns
        )

        return LinuxProcessRunner.startWithPty(appContext, runnerConfig)
    }

    /**
     * Starts gradlew build in a PTY in the specified project directory.
     *
     * @param context Android context
     * @param projectRoot Project root directory (must contain gradlew)
     * @param terminalRows Initial terminal rows (default: 25)
     * @param terminalColumns Initial terminal columns (default: 80)
     * @return PtyProcess with Gradle build running
     */
    fun startGradleBuild(
        context: Context,
        projectRoot: File,
        terminalRows: Int = 25,
        terminalColumns: Int = 80
    ): PtyProcess {
        return startTerminalCommand(
            context = context,
            command = "./gradlew",
            args = listOf("build"),
            workingDir = projectRoot,
            terminalRows = terminalRows,
            terminalColumns = terminalColumns
        )
    }

    /**
     * Starts gradlew with custom task in a PTY in the specified project directory.
     *
     * @param context Android context
     * @param projectRoot Project root directory
     * @param task Gradle task to execute (e.g., "build", "clean", "test")
     * @param terminalRows Initial terminal rows (default: 25)
     * @param terminalColumns Initial terminal columns (default: 80)
     * @return PtyProcess with Gradle task running
     */
    fun startGradleTask(
        context: Context,
        projectRoot: File,
        task: String,
        terminalRows: Int = 25,
        terminalColumns: Int = 80
    ): PtyProcess {
        return startTerminalCommand(
            context = context,
            command = "./gradlew",
            args = listOf(task),
            workingDir = projectRoot,
            terminalRows = terminalRows,
            terminalColumns = terminalColumns
        )
    }

    /**
     * Starts an interactive bash shell in a PTY.
     *
     * @param context Android context
     * @param workingDir Working directory
     * @param terminalRows Initial terminal rows (default: 25)
     * @param terminalColumns Initial terminal columns (default: 80)
     * @return PtyProcess with bash shell running
     */
    fun startShell(
        context: Context,
        workingDir: File,
        terminalRows: Int = 25,
        terminalColumns: Int = 80
    ): PtyProcess {
        return startTerminalCommand(
            context = context,
            command = "bash",
            args = listOf("-i"),
            workingDir = workingDir,
            terminalRows = terminalRows,
            terminalColumns = terminalColumns
        )
    }

    /**
     * Common toolchain environment setup.
     */
    private fun toolchainEnvironment(context: Context, jdkDir: File): Map<String, String> {
        return LinuxProcessRunner.toolchainEnvironment(
            jdkDir
        ) + mapOf(
            "TMPDIR" to context.cacheDir.absolutePath,
            "TMP" to context.cacheDir.absolutePath,
            "TEMP" to context.cacheDir.absolutePath
        )
    }
}
