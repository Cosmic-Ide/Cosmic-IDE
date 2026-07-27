/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.tooling

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.cosmicide.exec.ProcessExecutor
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

/**
 * API for the Gradle tooling server.
 *
 * This class manages a subprocess that runs the Gradle tooling server (from feature/tooling module)
 * and provides methods to communicate with it using JSON-RPC protocol.
 */
class ToolingServer(
    context: Context,
    val projectDir: File,
    private val toolingJar: File
) {

    companion object {
        private const val TAG = "ToolingServer"
        private const val TOOLING_JAR_ASSET = "gradle-tooling.jar"

        /**
         * Create a tooling server backed by the jar bundled in app assets.
         *
         * The jar is copied when the server starts, so project-open lifecycle owners can
         * construct the server without doing file I/O on the UI thread.
         */
        fun bundled(context: Context, projectDir: File): ToolingServer {
            val appContext = context.applicationContext
            return ToolingServer(
                context = appContext,
                projectDir = projectDir.absoluteFile,
                toolingJar = appContext.filesDir.resolve(TOOLING_JAR_ASSET)
            )
        }
    }

    @SuppressLint("StaticFieldLeak")
    private val context = context.applicationContext
    private val gson = Gson()
    private var process: Process? = null
    private var outputWriter: BufferedWriter? = null
    private var inputReader: BufferedReader? = null
    private var serverThread: Thread? = null
    private val running = java.util.concurrent.atomic.AtomicBoolean(false)

    // Callbacks for async responses
    private val pendingRequests = ConcurrentHashMap<String, (JsonElement?) -> Unit>()
    private val pendingErrors = ConcurrentHashMap<String, (String, String?, Throwable?) -> Unit>()
    private val writeLock = Any()

    // Event listeners
    private val eventListeners = CopyOnWriteArrayList<(String, JsonObject) -> Unit>()

    /**
     * Start the tooling server subprocess using ProcessExecutor.
     *
     * @param onReady Callback when server is ready
     * @param onError Callback on error
     */
    @Synchronized
    fun start(
        onReady: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        if (running.getAndSet(true)) {
            Log.w(TAG, "Tooling server already running")
            onReady()
            return
        }

        try {
            installBundledToolingJar()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install tooling jar", e)
            onError(e)
            running.set(false)
            return
        }

        val args = listOf(
            "--enable-native-access=ALL-UNNAMED",
            "-jar",
            toolingJar.absolutePath,
            "--project-dir",
            projectDir.absolutePath
        )

        Log.d(TAG, "Starting tooling server with command: java ${args.joinToString(" ")}")

        try {
            process = ProcessExecutor.startCommand(
                context = context,
                command = "java",
                args = args,
                workingDir = context.filesDir
            )

            // Set up I/O streams for bidirectional communication
            outputWriter = BufferedWriter(OutputStreamWriter(process!!.outputStream))
            inputReader = BufferedReader(InputStreamReader(process!!.inputStream))

            // Start thread to read server output
            serverThread = thread(start = true, isDaemon = true) {
                try {
                    var line: String?
                    while (inputReader?.readLine().also { line = it } != null) {
                        line?.let { handleServerOutput(it) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading from tooling server", e)
                } finally {
                    running.set(false)
                }
            }

            // Send ping to verify server is ready
            thread(start = true, isDaemon = true) {
                Thread.sleep(1000)
                ping { result, error ->
                    if (error != null) {
                        Log.e(TAG, "Ping failed", error)
                        onError(error)
                        running.set(false)
                    } else {
                        Log.d(TAG, "Tooling server started successfully")
                        onReady()
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start tooling server process", e)
            onError(e)
            running.set(false)
        }
    }

    /**
     * Stop the tooling server subprocess.
     */
    @Synchronized
    fun stop() {
        if (!running.getAndSet(false)) {
            return
        }

        Log.d(TAG, "Stopping tooling server")

        // Send shutdown request
        try {
            val requestId = generateRequestId()
            val request = JsonObject().apply {
                addProperty("id", requestId)
                addProperty("method", "shutdown")
                add("params", JsonObject())
            }
            outputWriter?.write(gson.toJson(request) + "\n")
            outputWriter?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending shutdown request", e)
        }

        // Terminate the process before closing the reader. BufferedReader.close() can block
        // waiting for the same lock held by the server thread while it is blocked in readLine().
        // Destroying the process closes the pipe and lets that read finish first.
        try {
            process?.destroyForcibly()
            outputWriter?.close()
            serverThread?.join(2000) // Wait up to 2 seconds
            inputReader?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up tooling server", e)
        } finally {
            outputWriter = null
            inputReader = null
            process = null
            serverThread = null
            pendingRequests.clear()
            pendingErrors.clear()
        }
    }

    /**
     * Check if the server is currently running.
     */
    fun isRunning(): Boolean = running.get()

    /**
     * Send a ping request to check if the server is alive.
     */
    fun ping(
        callback: (result: JsonObject?, error: Throwable?) -> Unit
    ) {
        requestObject("ping", callback = callback)
    }

    /**
     * Send a generic request to the tooling server.
     *
     * The server decides how to interpret the method against this server's project.
     * App-level helpers below are only default abstractions over this method, so new
     * Gradle tasks do not require a new client/server command pair.
     */
    fun request(
        method: String,
        params: JsonObject? = null,
        callback: (result: JsonElement?, error: Throwable?) -> Unit
    ) {
        val requestId = generateRequestId()
        val requestParams = params?.deepCopy() ?: JsonObject()

        val request = JsonObject().apply {
            addProperty("id", requestId)
            addProperty("method", method)
            if (requestParams.size() > 0) {
                add("params", requestParams)
            }
        }

        pendingRequests[requestId] = { response ->
            callback(response, null)
        }
        pendingErrors[requestId] = { message, errorType, error ->
            callback(null, error ?: Exception("${errorType ?: "ServerError"}: $message"))
        }

        sendRequest(request)
    }

    /**
     * Cancel a running Gradle operation.
     *
     * @param opId Operation ID to cancel
     */
    fun cancel(
        opId: String,
        callback: (result: JsonObject?, error: Throwable?) -> Unit
    ) {
        val params = JsonObject().apply {
            addProperty("opId", opId)
        }

        requestObject("gradle/cancel", params, callback)
    }

    /**
     * Close this server's Gradle project connection.
     */
    fun closeProject(
        callback: (result: JsonObject?, error: Throwable?) -> Unit
    ) {
        requestObject("gradle/closeProject", callback = callback)
    }

    /**
     * Notify the server that project files have changed.
     */
    fun notifyChanged(
        paths: List<String> = emptyList(),
        callback: (result: JsonObject?, error: Throwable?) -> Unit
    ) {
        val params = JsonObject().apply {
            add("paths", gson.toJsonTree(paths))
        }

        requestObject("gradle/notifyChanged", params, callback)
    }

    /**
     * Register an event listener for server events.
     */
    fun addEventListener(listener: (eventType: String, data: JsonObject) -> Unit) {
        eventListeners.add(listener)
    }

    /**
     * Remove an event listener.
     */
    fun removeEventListener(listener: (eventType: String, JsonObject) -> Unit) {
        eventListeners.remove(listener)
    }

    private fun generateRequestId(): String {
        return "req-${UUID.randomUUID()}"
    }

    private fun installBundledToolingJar() {
        toolingJar.parentFile?.mkdirs()
        context.assets.open(TOOLING_JAR_ASSET).use { input ->
            toolingJar.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun generateOperationId(): String {
        return "op-${UUID.randomUUID()}"
    }

    private fun requestObject(
        method: String,
        params: JsonObject? = null,
        callback: (result: JsonObject?, error: Throwable?) -> Unit
    ) {
        request(method, params) { result, error ->
            if (error != null) {
                callback(null, error)
                return@request
            }

            if (result == null || !result.isJsonObject) {
                callback(null, IllegalStateException("Expected object result for $method"))
                return@request
            }

            callback(result.asJsonObject, null)
        }
    }

    private fun sendRequest(request: JsonObject) {
        try {
            val jsonString = gson.toJson(request)
            Log.d(TAG, "Sending request: $jsonString")

            val writer =
                outputWriter ?: throw IllegalStateException("Tooling server is not running")

            synchronized(writeLock) {
                writer.write(jsonString)
                writer.write("\n")
                writer.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending request", e)
            failPendingRequest(request, e)
        }
    }

    private fun failPendingRequest(request: JsonObject, error: Throwable) {
        val id = request.get("id")?.takeUnless { it.isJsonNull }?.asString ?: return
        pendingRequests.remove(id)
        pendingErrors.remove(id)
            ?.invoke(error.message ?: error.javaClass.name, error.javaClass.name, error)
    }

    fun sendInput(
        opId: String,
        text: String,
        callback: (result: JsonObject?, error: Throwable?) -> Unit
    ) {
        val params = JsonObject().apply {
            addProperty("opId", opId)
            addProperty("text", text)
        }

        requestObject("gradle/input", params, callback)
    }

    private fun handleServerOutput(line: String) {
        try {
            val jsonElement = JsonParser.parseString(line)
            if (!jsonElement.isJsonObject) {
                Log.w(TAG, "Received non-object JSON: $line")
                return
            }

            val jsonObject = jsonElement.asJsonObject

            // Check if it's a response (has "id" field)
            if (jsonObject.has("id")) {
                val id = jsonObject.get("id").takeUnless { it.isJsonNull }?.asString
                if (id == null) {
                    Log.w(TAG, "Received response without request id: $line")
                    return
                }

                if (jsonObject.has("result")) {
                    // Successful response
                    val result = jsonObject.get("result")
                    val requestCallback = pendingRequests.remove(id)
                    requestCallback?.invoke(result)
                    pendingErrors.remove(id)
                } else if (jsonObject.has("error")) {
                    // Error response
                    val errorObj = jsonObject.get("error").asJsonObject
                    val type = when {
                        errorObj.has("code") -> errorObj.get("code").asString
                        errorObj.has("type") -> errorObj.get("type").asString
                        else -> "ServerError"
                    }
                    val message = if (errorObj.has("message")) {
                        errorObj.get("message").asString
                    } else {
                        ""
                    }

                    val errorCallback = pendingErrors.remove(id)
                    errorCallback?.invoke(message, type, null)
                    pendingRequests.remove(id)
                }
            } else if (jsonObject.has("event")) {
                val eventType = jsonObject.get("event").asString
                val params = jsonObject.deepCopy()
                params.remove("event")
                dispatchEvent(eventType, params)
            } else if (jsonObject.has("method")) {
                // JSON-RPC-style event notification
                val method = jsonObject.get("method").asString
                val params =
                    if (jsonObject.has("params") && jsonObject.get("params").isJsonObject) {
                        jsonObject.get("params").asJsonObject
                    } else {
                        JsonObject()
                    }
                dispatchEvent(method, params)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server output: $line", e)
        }
    }

    private fun dispatchEvent(eventType: String, params: JsonObject) {
        for (listener in eventListeners) {
            try {
                listener(eventType, params)
            } catch (e: Exception) {
                Log.e(TAG, "Error in event listener", e)
            }
        }
    }
}
