package org.cosmicide.common

import android.os.Handler
import android.os.HandlerThread
import java.lang.Runtime.getRuntime

class MemoryMonitor(
    private val intervalMs: Long = 5000L,
    private val onMemoryPressureChanged: (MemoryUtils.MemoryPressure) -> Unit
) {
    private var monitorThread: HandlerThread? = null
    private var handler: Handler? = null
    private var lastPressure = MemoryUtils.MemoryPressure.LOW
    private var running = false

    fun start() {
        if (running) return
        running = true

        monitorThread = HandlerThread("cosmic-memory-monitor").apply { start() }
        handler = Handler(monitorThread!!.looper)

        handler?.post(object : Runnable {
            override fun run() {
                if (!running) return

                val currentPressure = MemoryUtils.memoryPressureLevel
                if (currentPressure != lastPressure) {
                    lastPressure = currentPressure
                    onMemoryPressureChanged(currentPressure)
                }

                handler?.postDelayed(this, intervalMs)
            }
        })
    }

    fun stop() {
        running = false
        handler?.removeCallbacksAndMessages(null)
        monitorThread?.quitSafely()
        monitorThread = null
        handler = null
    }

    val currentPressure: MemoryUtils.MemoryPressure
        get() = MemoryUtils.memoryPressureLevel
}
