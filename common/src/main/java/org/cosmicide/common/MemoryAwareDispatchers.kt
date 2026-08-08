package org.cosmicide.common

import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService

object MemoryAwareDispatchers {

    private val ioExecutor: ExecutorService by lazy {
        val threadCount = MemoryUtils.recommendedIOThreads()
        Executors.newFixedThreadPool(threadCount) { runnable ->
            Thread(runnable, "cosmic-io-${threadCount}").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY - 1
            }
        }
    }

    val IO: ExecutorService
        get() = ioExecutor

    fun shutdown() {
        ioExecutor.shutdownNow()
    }
}
