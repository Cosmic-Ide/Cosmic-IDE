package org.cosmicide.tooling

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.gradle.tooling.CancellationToken
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.ResultHandler
import org.gradle.tooling.events.OperationDescriptor
import org.gradle.tooling.events.ProgressEvent
import org.gradle.tooling.events.ProgressListener
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.concurrent.thread

/**
 * Mutable config accumulated by the fluent setters (withArguments, setJavaHome, ...).
 * Not an implementation of any Gradle interface -- each Remote* class implements its
 * own interface directly and just reads/writes into one of these.
 */
internal class OperationState {
    val arguments = mutableListOf<String>()
    val jvmArguments = mutableListOf<String>()
    val systemProperties = mutableMapOf<String, String>()
    val environmentVariables = mutableMapOf<String, String>()
    var javaHome: File? = null
    var colorOutput: Boolean = false
    var detailedFailure: Boolean = false

    // local-only: these never cross the wire as JSON, they're bridged via opId-scoped events
    var stdout: OutputStream? = null
    var stderr: OutputStream? = null
    var stdin: InputStream? = null
    val progressListeners = mutableListOf<ProgressListener>()
    var cancellationToken: CancellationToken? = null

    fun toParams(): JsonObject = JsonObject().apply {
        if (arguments.isNotEmpty()) add("arguments", Gson().toJsonTree(arguments))
        if (jvmArguments.isNotEmpty()) add("jvmArguments", Gson().toJsonTree(jvmArguments))
        if (systemProperties.isNotEmpty()) add(
            "systemProperties",
            Gson().toJsonTree(systemProperties)
        )
        if (environmentVariables.isNotEmpty()) add("env", Gson().toJsonTree(environmentVariables))
        javaHome?.let { addProperty("javaHome", it.absolutePath) }
        addProperty("colorOutput", colorOutput)
        addProperty("detailedFailure", detailedFailure)
    }
}

internal fun newOpId(): String = "op-${UUID.randomUUID()}"

/** ResultHandler has 2 abstract methods -> not SAM-convertible, so build it explicitly. */
internal fun <T> resultHandler(
    onComplete: (T) -> Unit,
    onFailure: (Throwable) -> Unit
): ResultHandler<T> = object : ResultHandler<T> {
    override fun onComplete(result: T) = onComplete(result)
    override fun onFailure(failure: GradleConnectionException) = onFailure(failure)
}

internal fun wrapAsConnectionException(t: Throwable): GradleConnectionException =
    t as? GradleConnectionException ?: GradleConnectionException(t.message ?: t.javaClass.name, t)

/** Minimal ProgressEvent so we can forward server-side "gradle/progress" events to listeners. */
private class SimpleProgressEvent(
    private val displayNameValue: String,
    private val descriptorName: String
) : ProgressEvent {
    override fun getEventTime(): Long = System.currentTimeMillis()
    override fun getDisplayName(): String = displayNameValue
    override fun getDescriptor(): OperationDescriptor = object : OperationDescriptor {
        override fun getName(): String = descriptorName
        override fun getDisplayName(): String = descriptorName
        override fun getParent(): OperationDescriptor? = null
    }
}

/**
 * Routes this opId's "gradle/output" / "gradle/progress" / "gradle/inputRequested" events
 * to the locally-registered stdout/stderr/progressListeners/stdin. Call once per operation,
 * right before firing the request.
 */
internal fun wireStreams(server: ToolingServer, opId: String, state: OperationState) {
    server.addEventListener { eventType, params ->
        val eventOpId = params.get("opId")?.takeIf { it.isJsonPrimitive }?.asString
        if (eventOpId != opId) return@addEventListener

        when (eventType) {
            "gradle/output" -> {
                val stream = params.get("stream")?.asString
                val text = params.get("text")?.asString.orEmpty()
                val bytes = text.toByteArray(Charsets.UTF_8)
                if (stream == "stderr") state.stderr?.write(bytes) else state.stdout?.write(bytes)
            }

            "gradle/progress" -> {
                val displayName = params.get("displayName")?.asString.orEmpty()
                val descriptor = params.get("descriptor")?.asString.orEmpty()
                val event = SimpleProgressEvent(displayName, descriptor)
                state.progressListeners.forEach { it.statusChanged(event) }
            }

            "gradle/inputRequested" -> {
                val input = state.stdin ?: return@addEventListener
                thread(isDaemon = true) {
                    val line = input.bufferedReader().readLine() ?: return@thread
                    server.sendInput(opId, "$line\n") { _, _ -> }
                }
            }
        }
    }
}

/** Polls a CancellationToken and forwards cancellation to the server for this opId. */
internal fun wireCancellation(server: ToolingServer, opId: String, token: CancellationToken?) {
    if (token == null) return
    val done = java.util.concurrent.atomic.AtomicBoolean(false)
    thread(isDaemon = true) {
        while (!done.get() && !token.isCancellationRequested) Thread.sleep(200)
        if (!done.get() && token.isCancellationRequested) {
            server.cancel(opId) { _, _ -> }
        }
    }
    // caller marks `done` externally by simply letting the daemon thread die once the
    // op's callback fires -- fine for now since it's a daemon thread and cheap to poll
}
