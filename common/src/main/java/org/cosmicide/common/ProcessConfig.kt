package org.cosmicide.common

import java.lang.Runtime.getRuntime

object ProcessConfig {

    fun maxParallelProcesses(): Int {
        val workers = MemoryUtils.recommendedWorkerCount()
        return when (MemoryUtils.memoryPressureLevel) {
            MemoryUtils.MemoryPressure.CRITICAL -> 1
            MemoryUtils.MemoryPressure.HIGH -> 1
            MemoryUtils.MemoryPressure.MODERATE -> 2
            MemoryUtils.MemoryPressure.LOW -> workers
        }
    }

    fun shouldThrottle(): Boolean {
        return MemoryUtils.isLowMemory()
    }

    fun subprocessMemoryLimitMb(): Long {
        val availableMb = MemoryUtils.availableMemory / (1024 * 1024)
        val maxProcesses = maxParallelProcesses()
        return if (maxProcesses > 0) availableMb / maxProcesses else availableMb / 2
    }
}
