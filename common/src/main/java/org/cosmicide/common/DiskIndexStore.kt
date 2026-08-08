package org.cosmicide.common

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class DiskIndexStore(private val storeDir: File) {

    init {
        storeDir.mkdirs()
    }

    fun getOrCreateSegment(key: String, builder: () -> ByteArray): DiskIndexSegment {
        val hash = sha256(key)
        val segmentFile = storeDir.resolve("$hash.idx")
        val metaFile = storeDir.resolve("$hash.meta")

        if (segmentFile.exists() && metaFile.exists()) {
            val size = metaFile.readText().trim().toLongOrNull() ?: 0L
            return DiskIndexSegment(segmentFile, size)
        }

        val data = builder()
        writeAtomic(segmentFile, data)
        metaFile.writeText(data.size.toString())

        return DiskIndexSegment(segmentFile, data.size.toLong())
    }

    fun invalidate(key: String) {
        val hash = sha256(key)
        storeDir.resolve("$hash.idx").delete()
        storeDir.resolve("$hash.meta").delete()
    }

    fun clear() {
        storeDir.listFiles()?.forEach { it.delete() }
    }

    private fun writeAtomic(file: File, data: ByteArray) {
        val temp = File.createTempFile(file.name, ".tmp", file.parentFile)
        try {
            temp.writeBytes(data)
            temp.renameTo(file)
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}

class DiskIndexSegment(
    private val file: File,
    val size: Long
) {
    private val raf: RandomAccessFile by lazy { RandomAccessFile(file, "r") }
    private val channel: FileChannel by lazy { raf.channel }

    @Volatile
    private var mappedBuffer: MappedByteBuffer? = null

    fun readBlock(offset: Long, length: Int): ByteArray {
        val buf = ByteArray(length)
        synchronized(this) {
            channel.read(ByteBuffer.wrap(buf), offset)
        }
        return buf
    }

    fun map(): MappedByteBuffer {
        return mappedBuffer ?: synchronized(this) {
            mappedBuffer ?: channel.map(
                FileChannel.MapMode.READ_ONLY,
                0,
                size
            ).also { mappedBuffer = it }
        }
    }

    fun close() {
        synchronized(this) {
            mappedBuffer?.force()
            mappedBuffer = null
            channel.close()
            raf.close()
        }
    }
}
