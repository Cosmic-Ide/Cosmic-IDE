package org.cosmicide.common

import java.util.LinkedHashMap

class BlockCache(private val maxBlocks: Int = 256) {

    private val cache = object : LinkedHashMap<String, ByteArray>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?): Boolean {
            return size > maxBlocks
        }
    }

    fun getOrLoad(key: String, loader: () -> ByteArray): ByteArray {
        synchronized(cache) {
            cache[key]?.let { return it }
        }

        val data = loader()
        synchronized(cache) {
            cache[key] = data
        }
        return data
    }

    fun invalidate(key: String) {
        synchronized(cache) {
            cache.remove(key)
        }
    }

    fun clear() {
        synchronized(cache) {
            cache.clear()
        }
    }

    val size: Int
        get() = synchronized(cache) { cache.size }
}
