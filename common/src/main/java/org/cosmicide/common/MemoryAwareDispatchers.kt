package org.cosmicide.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

object MemoryAwareDispatchers {

    private val ioDispatcher: CoroutineDispatcher by lazy {
        val threadCount = MemoryUtils.recommendedIOThreads()
        Executors.newFixedThreadPool(threadCount) { runnable ->
            Thread(runnable, "cosmic-io-${threadCount}").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY - 1
            }
        }.asCoroutineDispatcher()
    }

    val IO: CoroutineDispatcher
        get() = ioDispatcher

    val Main: CoroutineDispatcher
        get() = Dispatchers.Main

    fun shutdown() {
        (ioDispatcher as? java.io.Closeable)?.close()
    }
}
