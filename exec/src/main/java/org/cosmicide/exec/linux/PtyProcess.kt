package org.cosmicide.exec.linux

import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Wraps a PtyTerminal with convenience methods for terminal control and process management.
 * The child process is attached to the PTY at the native level.
 */
class PtyProcess(
    private val pty: PtyTerminal
) {
    private val waited = AtomicBoolean(false)

    @Volatile
    private var exitCode: Int? = null

    /**
     * Gets the underlying PTY terminal
     */
    fun getPty(): PtyTerminal = pty

    /**
     * Gets the child process ID
     */
    fun getChildPid(): Int = pty.getChildPid()

    /**
     * Gets input stream from PTY master (terminal output)
     */
    fun getInputStream(): InputStream = pty.getMasterInputStream()

    /**
     * Gets output stream to PTY master (terminal input)
     */
    fun getOutputStream(): OutputStream = pty.getMasterOutputStream()

    /**
     * Sets terminal window size (sends SIGWINCH to process group)
     */
    fun setTerminalSize(rows: Int, columns: Int) {
        if (rows <= 0 || columns <= 0) {
            throw IllegalArgumentException("Terminal dimensions must be positive")
        }
        pty.setWindowSize(rows, columns)
    }

    /**
     * Sends signal to process group
     * 
     * Signal codes:
     *  - 1 = SIGHUP (hangup)
     *  - 2 = SIGINT (Ctrl+C - interrupt)
     *  - 3 = SIGQUIT (Ctrl+\)
     *  - 9 = SIGKILL (force kill)
     *  - 15 = SIGTERM (terminate gracefully)
     * 
     * Note: Sends to process GROUP, so all children also get the signal.
     */
    fun sendSignal(signal: Int): Boolean {
        val pid = getChildPid()
        val delivered = pid > 0 && nativeSendSignal(pid, signal)
        Log.d(PtyLogTag, "sendSignal pid=$pid signal=$signal delivered=$delivered")
        return delivered
    }

    /**
     * Sends a signal to the current foreground process group of the PTY.
     * This is the correct target for terminal controls like Ctrl+C because
     * interactive shells move foreground commands into their own process group.
     */
    fun sendSignalToForegroundProcessGroup(signal: Int): Boolean {
        val delivered = pty.sendSignalToForegroundProcessGroup(signal)
        Log.d(
            PtyLogTag,
            "sendSignalToForegroundProcessGroup pid=${getChildPid()} signal=$signal delivered=$delivered"
        )
        return delivered
    }

    fun interrupt(): Boolean {
        return sendSignalToForegroundProcessGroup(2) // SIGINT
    }

    /**
     * Attempts graceful termination (SIGTERM)
     */
    fun terminate(): Boolean {
        return sendSignal(15) // SIGTERM
    }

    /**
     * Forces termination (SIGKILL)
     */
    fun kill(): Boolean {
        return sendSignal(9) // SIGKILL
    }

    /**
     * Waits for child process to complete (BLOCKING)
     * 
     * This is a BLOCKING operation that reaps the child exactly once.
     * After calling waitFor(), the child is consumed.
     * 
     * IMPORTANT: Call from a background thread (not UI thread).
     * The caller is responsible for managing process lifecycle:
     * 1. Call terminate() to send SIGTERM
     * 2. Poll or sleep to give process time to exit
     * 3. Call kill() to send SIGKILL if still alive
     * 4. Call waitFor() to reap the process
     * 5. Close the PTY with close()
     * 
     * Returns: exit code (0 for success, non-zero for failure/signal)
     */
    fun waitFor(): Int {
        exitCode?.let { return it }

        if (!waited.compareAndSet(false, true)) {
            return exitCode ?: -1
        }

        val pid = getChildPid()
        val code = if (pid > 0) {
            nativeWaitPid(pid)
        } else {
            -1
        }
        exitCode = code
        return code
    }

    /**
     * Closes the PTY master file descriptor
     * 
     * This only closes the file descriptor. It does NOT:
     * - Send signals
     * - Wait for process
     * - Reap the child
     * 
     * Process lifecycle is caller's responsibility:
     * 1. Send terminate()/kill() if needed
     * 2. Call waitFor() in a background thread
     * 3. Only then call close()
     */
    fun close() {
        pty.close()
    }

    companion object {
        /**
         * Creates a PTY process wrapper
         */
        fun create(pty: PtyTerminal): PtyProcess {
            return PtyProcess(pty)
        }
    }
}

private external fun nativeSendSignal(pid: Int, signal: Int): Boolean
private external fun nativeWaitPid(pid: Int): Int
private const val PtyLogTag = "CosmicPty"
