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

    private fun toolchainEnvironment(context: Context, jdkDir: File): Map<String, String> {
        return LinuxProcessRunner.toolchainEnvironment(
            jdkDir,
            context.filesDir.resolve("arch")
        )
    }
}
