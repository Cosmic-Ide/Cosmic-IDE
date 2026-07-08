package org.cosmicide.exec.linux

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Represents a pseudo-terminal (PTY) for interactive terminal I/O.
 * 
 * DESIGN:
 * - Child process is spawned inside the PTY (attached at native level)
 * - Slave FD never exposed to Kotlin - managed entirely in native code
 * - Only master_fd and child_pid are visible to Java
 * - Blocking I/O on master for proper InputStream behavior
 */
class PtyTerminal(
    private val masterFd: Int,
    private val childPid: Int
) {
    private val isClosed = AtomicBoolean(false)

    /**
     * Sets the terminal window size
     */
    fun setWindowSize(rows: Int, columns: Int) {
        if (isClosed.get()) throw IllegalStateException("PTY is closed")
        if (!nativeSetWindowSize(masterFd, rows, columns)) {
            throw RuntimeException("Failed to set window size")
        }
    }

    /**
     * Sends a signal to the PTY foreground process group, falling back to the
     * original child process group if the terminal does not report one.
     */
    fun sendSignalToForegroundProcessGroup(signal: Int): Boolean {
        if (isClosed.get()) throw IllegalStateException("PTY is closed")
        if (signal <= 0) return false
        return nativeSendSignalToForegroundProcessGroup(masterFd, childPid, signal)
    }

    /**
     * Gets master file descriptor input stream
     */
    fun getMasterInputStream(): InputStream {
        if (isClosed.get()) throw IllegalStateException("PTY is closed")
        return FileInputStream(masterFd)
    }

    /**
     * Gets master file descriptor output stream
     */
    fun getMasterOutputStream(): OutputStream {
        if (isClosed.get()) throw IllegalStateException("PTY is closed")
        return FileOutputStream(masterFd)
    }

    /**
     * Gets the child process ID
     */
    fun getChildPid(): Int = childPid

    /**
     * Gets the master file descriptor
     */
    fun getMasterFd(): Int = masterFd

    /**
     * Closes the PTY master file descriptor
     */
    fun close() {
        if (isClosed.compareAndSet(false, true)) {
            nativeClosePty(masterFd)
        }
    }

    /**
     * Checks if PTY is closed
     */
    fun isClosed(): Boolean = isClosed.get()

    companion object {
        /**
         * Allocates a new PTY pair and spawns a child process inside it
         */
        fun allocateAndSpawn(
            workingDir: File,
            executable: File,
            arguments: List<String>,
            environment: Map<String, String>?
        ): PtyTerminal {
            // Build arguments array (argv[0] is program name, then args)
            val argv = mutableListOf(executable.absolutePath)
            argv.addAll(arguments)

            // Build environment array if provided
            val envArray = if (environment != null) {
                environment.map { (k, v) -> "$k=$v" }.toTypedArray()
            } else {
                null
            }

            try {
                // Spawn child process inside PTY
                // Native function returns [master_fd, child_pid]
                val result = nativeSpawnInPty(
                    workingDir.absolutePath,
                    executable.absolutePath,
                    argv.toTypedArray(),
                    envArray
                )

                if (result.size < 2) {
                    throw RuntimeException("Failed to spawn process in PTY: invalid result")
                }

                val masterFd = result[0]
                val childPid = result[1]

                // FD 0 is valid (stdin), but PTY master will almost always be >= 3
                // childPid must be > 0
                if (masterFd < 0 || childPid <= 0) {
                    throw RuntimeException("Failed to spawn process in PTY (fd=$masterFd, pid=$childPid)")
                }

                return PtyTerminal(masterFd, childPid)
            } catch (e: Exception) {
                throw e
            }
        }

        init {
            try {
                System.loadLibrary("pty_native")
            } catch (e: UnsatisfiedLinkError) {
                // FAIL LOUDLY - don't silently continue
                throw RuntimeException(
                    "Failed to load PTY native library (pty_native): ${e.message}. " +
                            "Make sure libpty_native.so is in the native library path.",
                    e
                )
            }
        }
    }

    /**
     * Helper input stream for file descriptor (BLOCKING mode)
     * 
     * For blocking FD:
     * - read() > 0 means bytes available
     * - read() == -1 means EOF (slave closed and no more data)
     * - Never returns 0 (0 would mean NUL byte to caller, which is ambiguous)
     */
    private class FileInputStream(private val fd: Int) : InputStream() {
        private val buffer = ByteArray(4096)

        override fun read(): Int {
            val bytes = nativeRead(fd, buffer, 1)
            // For blocking read on PTY: > 0 means data, <= 0 means EOF
            return if (bytes > 0) buffer[0].toInt() and 0xFF else -1
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            checkBounds(b.size, off, len)
            if (len == 0) return 0  // Standard InputStream behavior

            val toRead = minOf(len, buffer.size)
            val bytesRead = nativeRead(fd, buffer, toRead)
            // For blocking read: > 0 means data, <= 0 means EOF
            return if (bytesRead > 0) {
                buffer.copyInto(b, off, 0, bytesRead)
                bytesRead
            } else {
                -1  // EOF/error
            }
        }

        override fun close() {
            // Don't close the fd directly; let PtyTerminal handle it
        }
    }

    /**
     * Helper output stream for file descriptor
     * 
     * nativeWrite() handles partial writes internally by looping until all bytes are written.
     */
    private class FileOutputStream(private val fd: Int) : OutputStream() {
        override fun write(b: Int) {
            write(byteArrayOf(b.toByte()))
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            checkBounds(b.size, off, len)
            if (len == 0) return

            val written = nativeWrite(fd, b, off, len)
            if (written < 0 || written < len) {
                throw RuntimeException("Write failed on PTY master FD (wrote $written of $len bytes)")
            }
        }

        override fun close() {
            // Don't close the fd directly; let PtyTerminal handle it
        }
    }
}

private fun checkBounds(size: Int, off: Int, len: Int) {
    if (off < 0 || len < 0 || off > size || len > size - off) {
        throw IndexOutOfBoundsException("size=$size off=$off len=$len")
    }
}

// Native method declarations
private external fun nativeSpawnInPty(
    workingDir: String,
    exePath: String,
    argv: Array<String>,
    envVars: Array<String>?
): IntArray

private external fun nativeClosePty(masterFd: Int): Boolean
private external fun nativeSetWindowSize(masterFd: Int, rows: Int, columns: Int): Boolean
private external fun nativeSendSignalToForegroundProcessGroup(
    masterFd: Int,
    fallbackPid: Int,
    signal: Int
): Boolean

private external fun nativeRead(fd: Int, buffer: ByteArray, len: Int): Int
private external fun nativeWrite(fd: Int, buffer: ByteArray, off: Int, len: Int): Int
