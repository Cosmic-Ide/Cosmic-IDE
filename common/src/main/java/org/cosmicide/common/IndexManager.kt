package org.cosmicide.common

import android.content.Context
import java.io.File

object IndexManager {

    private lateinit var store: DiskIndexStore
    private val cache = BlockCache(maxBlocks = 512)
    private val activeSegments = ConcurrentHashMap<String, DiskIndexSegment>()

    fun init(context: Context) {
        val cacheDir = context.cacheDir.resolve("index-cache").apply { mkdirs() }
        store = DiskIndexStore(cacheDir)
    }

    fun getOrBuildIndex(
        projectKey: String,
        indexType: String,
        builder: () -> ByteArray
    ): DiskIndexSegment {
        val key = "$projectKey:$indexType"

        activeSegments[key]?.let { return it }

        val segment = store.getOrCreateSegment(key, builder)
        activeSegments[key] = segment
        return segment
    }

    fun readCached(key: String, blockOffset: Long, blockLength: Int): ByteArray {
        val cacheKey = "$key:$blockOffset:$blockLength"
        return cache.getOrLoad(cacheKey) {
            activeSegments[key]?.readBlock(blockOffset, blockLength)
                ?: throw IllegalStateException("Index segment not loaded: $key")
        }
    }

    fun invalidateProject(projectKey: String) {
        val prefix = "$projectKey:"
        activeSegments.keys.filter { it.startsWith(prefix) }.forEach { key ->
            activeSegments[key]?.close()
            activeSegments.remove(key)
            store.invalidate(key)
        }
    }

    fun invalidateAll() {
        activeSegments.values.forEach { it.close() }
        activeSegments.clear()
        cache.clear()
        store.clear()
    }

    fun shutdown() {
        activeSegments.values.forEach { it.close() }
        activeSegments.clear()
        cache.clear()
    }
}
