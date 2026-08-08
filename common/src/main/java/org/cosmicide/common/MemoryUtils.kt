package org.cosmicide.common

import java.lang.Runtime.getRuntime

object MemoryUtils {

    val maxMemory: Long
        get() = getRuntime().maxMemory()

    val totalMemory: Long
        get() = getRuntime().totalMemory()

    val freeMemory: Long
        get() = getRuntime().freeMemory()

    val usedMemory: Long
        get() = totalMemory - freeMemory

    val availableMemory: Long
        get() = maxMemory - usedMemory

    val memoryPressureLevel: MemoryPressure
        get() {
            val usageRatio = usedMemory.toFloat() / maxMemory
            return when {
                usageRatio > 0.9f -> MemoryPressure.CRITICAL
                usageRatio > 0.75f -> MemoryPressure.HIGH
                usageRatio > 0.5f -> MemoryPressure.MODERATE
                else -> MemoryPressure.LOW
            }
        }

    fun recommendedWorkerCount(): Int {
        val cores = Runtime.getRuntime().availableProcessors()
        val memMb = availableMemory / (1024 * 1024)

        return when {
            memMb < 128 -> 1
            memMb < 256 -> minOf(2, cores)
            memMb < 512 -> minOf(4, cores)
            else -> minOf(cores, 8)
        }
    }

    fun recommendedIOThreads(): Int {
        val workers = recommendedWorkerCount()
        return maxOf(workers, 2)
    }

    fun isLowMemory(): Boolean = memoryPressureLevel.ordinal >= MemoryPressure.HIGH.ordinal

    enum class MemoryPressure {
        LOW,
        MODERATE,
        HIGH,
        CRITICAL
    }
}
