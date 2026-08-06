/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */

package org.cosmicide.plugin

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.common.Prefs
import org.cosmicide.exec.ProcessExecutor
import org.cosmicide.exec.linux.LinuxProcessRunner
import org.cosmicide.project.CommandExecutionService
import org.cosmicide.project.CommandRequest
import org.cosmicide.project.CommandResult
import org.cosmicide.project.ToolProcessService
import org.cosmicide.util.jdksDir
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

internal class AndroidCommandExecutionService(context: Context) :
    CommandExecutionService,
    ToolProcessService {
    private val appContext = context.applicationContext

    override fun isCommandAvailable(command: String, workingDirectory: File): Boolean {
        return runCatching {
            val jdkDirectory = appContext.jdksDir().resolve(Prefs.currentJDK)
            LinuxProcessRunner.resolveExecutable(
                commandName = command,
                workingDir = workingDirectory,
                pathEntries = LinuxProcessRunner.toolchainPathEntries(appContext, jdkDirectory)
            ).isFile
        }.getOrDefault(false)
    }

    override suspend fun execute(
        request: CommandRequest,
        onOutput: (String) -> Unit
    ): CommandResult = withContext(Dispatchers.IO) {
        require(request.workingDirectory.isDirectory) {
            "Working directory does not exist: ${request.workingDirectory.absolutePath}"
        }

        val process = start(request, redirectErrorStream = true)
        val captured = StringBuilder()
        val callerJob = currentCoroutineContext()[Job]
        val cancellationWatcher = CoroutineScope(Dispatchers.IO).launch {
            while (callerJob?.isActive == true) delay(CANCELLATION_POLL_MILLIS.milliseconds)
            if (callerJob?.isCancelled == true && process.isAlive) process.destroy()
        }

        try {
            process.inputStream.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break

                    val chunk = String(buffer, 0, count, Charsets.UTF_8)
                    captured.append(chunk)
                    if (captured.length > MAX_CAPTURED_OUTPUT_CHARS) {
                        captured.delete(0, captured.length - MAX_CAPTURED_OUTPUT_CHARS)
                    }
                    onOutput(chunk)
                }
            }

            CommandResult(
                exitCode = process.waitFor(),
                output = captured.toString()
            )
        } finally {
            cancellationWatcher.cancel()
            if (process.isAlive) {
                process.destroy()
            }
        }
    }

    override fun start(request: CommandRequest, redirectErrorStream: Boolean): Process {
        require(request.workingDirectory.isDirectory) {
            "Working directory does not exist: ${request.workingDirectory.absolutePath}"
        }

        // Wrap all commands in bash with -l (login) flag to load startup files.
        // The .bash_profile created by LinuxProcessRunner includes a bridge that sources .bashrc,
        // so a login shell will load both .bash_profile and .bashrc.
        // Append " && exit" to ensure the shell terminates properly.
        val escapedCommand = escapeForBashDoubleQuotes(request.command)
        val escapedArgs = if (request.arguments.isNotEmpty()) {
            " " + request.arguments.joinToString(" ") { escapeForBashDoubleQuotes(it) }
        } else {
            ""
        }
        val commandLine = "$escapedCommand$escapedArgs && exit"
        val finalCommand = "bash"
        val finalArgs = listOf("-l", "-c", commandLine)

        return ProcessExecutor.startCommand(
            context = appContext,
            command = finalCommand,
            args = finalArgs,
            workingDir = request.workingDirectory,
            redirectErrorStream = redirectErrorStream,
            environmentOverrides = request.environment + mapOf(
                APP_FILES_DIR_ENV to appContext.filesDir.resolve("arch").absolutePath
            )
        )
    }

    /**
     * Escapes a string for use in a double-quoted bash -c command argument.
     * Only escapes characters that would break the double-quoted string:
     * - backslash (\\ -> \\\\)
     * - double quote (" -> \\")
     * - newline (\n -> \\n)
     * - carriage return (\r -> \\r)
     * 
     * Note: We do NOT escape $, `, or other bash special characters because
     * those should be interpreted by bash when executing the command.
     */
    private fun escapeForBashDoubleQuotes(s: String): String {
        return s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }

    private companion object {
        const val APP_FILES_DIR_ENV = "APP_FILES_DIR"
        const val MAX_CAPTURED_OUTPUT_CHARS = 64_000
        const val CANCELLATION_POLL_MILLIS = 50L
    }
}
