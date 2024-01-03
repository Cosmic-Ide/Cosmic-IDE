package io.appwrite.services

import io.appwrite.Client
import io.appwrite.exceptions.AppwriteException
import io.appwrite.extensions.forEachAsync
import io.appwrite.extensions.fromJson
import io.appwrite.extensions.jsonCast
import io.appwrite.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.internal.concurrent.TaskRunner
import okhttp3.internal.ws.RealWebSocket
import java.util.*
import android.util.Log
import kotlin.coroutines.CoroutineContext

class Realtime(client: Client) : Service(client), CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private companion object {
        private const val TYPE_ERROR = "error"
        private const val TYPE_EVENT = "event"

        private const val DEBOUNCE_MILLIS = 1L

        private var socket: RealWebSocket? = null
        private var activeChannels = mutableSetOf<String>()
        private var activeSubscriptions = mutableMapOf<Int, RealtimeCallback>()

        private var subCallDepth = 0
        private var reconnectAttempts = 0
        private var subscriptionsCounter = 0
        private var reconnect = true
    }

    private fun createSocket() {
        if (activeChannels.isEmpty()) {
            return
        }

        val queryParamBuilder = StringBuilder()
            .append("project=${client.config["project"]}")

        activeChannels.forEach {
            queryParamBuilder
                .append("&channels[]=$it")
        }

        val request = Request.Builder()
            .url("${client.endPointRealtime}/realtime?$queryParamBuilder")
            .build()

        if (socket != null) {
            reconnect = false
            closeSocket()
        }

        socket = RealWebSocket(
            taskRunner = TaskRunner.INSTANCE,
            originalRequest = request,
            listener = AppwriteWebSocketListener(),
            random = Random(),
            pingIntervalMillis = client.http.pingIntervalMillis.toLong(),
            extensions = null,
            minimumDeflateSize = client.http.minWebSocketMessageToCompress
        )

        socket!!.connect(client.http)
    }

    private fun closeSocket() {
        socket?.close(RealtimeCode.POLICY_VIOLATION.value, null)
    }

    private fun getTimeout() = when {
        reconnectAttempts < 5 -> 1000L
        reconnectAttempts < 15 -> 5000L
        reconnectAttempts < 100 -> 10000L
        else -> 60000L
    }

    fun subscribe(
        vararg channels: String,
        callback: (RealtimeResponseEvent<Any>) -> Unit,
    ) = subscribe(
        channels = channels,
        Any::class.java,
        callback
    )

    fun <T> subscribe(
        vararg channels: String,
        payloadType: Class<T>,
        callback: (RealtimeResponseEvent<T>) -> Unit,
    ): RealtimeSubscription {
        val counter = subscriptionsCounter++

        activeChannels.addAll(channels)
        activeSubscriptions[counter] = RealtimeCallback(
            channels.toList(),
            payloadType,
            callback as (RealtimeResponseEvent<*>) -> Unit
        )

        launch {
            subCallDepth++
            delay(DEBOUNCE_MILLIS)
            if (subCallDepth == 1) {
                createSocket()
            }
            subCallDepth--
        }

        return RealtimeSubscription {
            activeSubscriptions.remove(counter)
            cleanUp(*channels)
            createSocket()
        }
    }

    private fun cleanUp(vararg channels: String) {
        activeChannels.removeAll { channel ->
            if (!channels.contains(channel)) {
                return@removeAll false
            }
            activeSubscriptions.values.none { callback ->
                callback.channels.contains(channel)
            }
        }
    }

    private inner class AppwriteWebSocketListener : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            reconnectAttempts = 0
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)

            launch(IO) {
                val message = text.fromJson<RealtimeResponse>()
                when (message.type) {
                    TYPE_ERROR -> handleResponseError(message)
                    TYPE_EVENT -> handleResponseEvent(message)
                }
            }
        }

        private fun handleResponseError(message: RealtimeResponse) {
            throw message.data.jsonCast<AppwriteException>()
        }

        private suspend fun handleResponseEvent(message: RealtimeResponse) {
            val event = message.data.jsonCast<RealtimeResponseEvent<Any>>()
            if (event.channels.isEmpty()) {
                return
            }
            if (!event.channels.any { activeChannels.contains(it) }) {
                return
            }
            activeSubscriptions.values.forEachAsync { subscription ->
                if (event.channels.any { subscription.channels.contains(it) }) {
                    event.payload = event.payload.jsonCast(subscription.payloadClass)
                    subscription.callback(event)
                }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosing(webSocket, code, reason)
            if (!reconnect || code == RealtimeCode.POLICY_VIOLATION.value) {
                reconnect = true
                return
            }

            val timeout = getTimeout()

            Log.e(
                this@Realtime::class.java.name,
                "Realtime disconnected. Re-connecting in ${timeout / 1000} seconds.",
                AppwriteException(reason, code)
            )

            launch {
                delay(timeout)
                reconnectAttempts++
                createSocket()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            t.printStackTrace()
        }
    }
}