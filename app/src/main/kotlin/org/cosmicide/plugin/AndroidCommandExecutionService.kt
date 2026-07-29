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
        return ProcessExecutor.startCommand(
            context = appContext,
            command = request.command,
            args = request.arguments,
            workingDir = request.workingDirectory,
            redirectErrorStream = redirectErrorStream,
            environmentOverrides = request.environment + mapOf(
                APP_FILES_DIR_ENV to appContext.filesDir.resolve("arch").absolutePath
            )
        )
    }

    private companion object {
        const val APP_FILES_DIR_ENV = "APP_FILES_DIR"
        const val MAX_CAPTURED_OUTPUT_CHARS = 64_000
        const val CANCELLATION_POLL_MILLIS = 50L
    }
}
