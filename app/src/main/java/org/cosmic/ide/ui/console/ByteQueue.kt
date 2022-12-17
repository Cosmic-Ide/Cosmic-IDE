package org.cosmic.ide.ui.console

/**
 * A circular byte buffer allowing one producer and one consumer thread.
 */
class ByteQueue(size: Int) {
    private val buffer = ByteArray(size)
    private var head = 0
    private var storedBytes = 0

    fun getBytesAvailable(): Int {
        return synchronized(this) {
            storedBytes
        }
    }

    @Throws(InterruptedException::class)
    fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length + offset > buffer.size) {
            throw IllegalArgumentException("length + offset > buffer.length")
        }
        if (length < 0) {
            throw IllegalArgumentException("length < 0")
        }
        if (length == 0) {
            return 0
        }
        return synchronized(this) {
            while (storedBytes == 0) {
                wait()
            }
            var totalRead = 0
            val bufferLength = this.buffer.size
            val wasFull = bufferLength == storedBytes
            while (length > 0 && storedBytes > 0) {
                val oneRun = Math.min(bufferLength - head, storedBytes)
                val bytesToCopy = Math.min(length, oneRun)
                System.arraycopy(this.buffer, head, buffer, offset, bytesToCopy)
                head += bytesToCopy
                if (head >= bufferLength) {
                    head = 0
                }
                storedBytes -= bytesToCopy
                length -= bytesToCopy
                offset += bytesToCopy
                totalRead += bytesToCopy
            }
            
            if (wasFull) {
                notify()
            }
            totalRead
        }
    }

    @Throws(InterruptedException::class)
    fun write(buffer: ByteArray, offset: Int, length: Int) {
        if (length + offset > buffer.size) {
            throw IllegalArgumentException("length + offset > buffer.length")
        }
        if (length < 0) {
            throw IllegalArgumentException("length < 0")
        }
        if (length == 0) {
            return
        }
        synchronized(this) {
            val bufferLength = this.buffer.size
            val wasEmpty = storedBytes == 0
            while (length > 0) {
                while (bufferLength == storedBytes) {
                    wait()
                }
                var tail = head + storedBytes
                val oneRun: Int
                if (tail >= bufferLength) {
                    tail = tail - bufferLength
                    oneRun = head - tail
                } else {
                    oneRun = bufferLength - tail
                }
                val bytesToCopy = Math.min(oneRun, length)
                System.arraycopy(buffer, offset, this.buffer, tail, bytesToCopy)
                offset += bytesToCopy
                storedBytes += bytesToCopy
                length -= bytesToCopy
            }
            if (wasEmpty) {
                notify()
            }
        }
    }
}
