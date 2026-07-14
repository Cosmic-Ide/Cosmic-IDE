package org.cosmicide.ui.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.view.ActionMode
import android.view.KeyEvent
import android.view.MenuItem
import com.termux.terminal.KeyHandler
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalOutput
import com.termux.view.TerminalView
import com.termux.view.textselection.TextSelectionCursorController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.exec.linux.LinuxProcessRunner
import org.cosmicide.exec.linux.PtyProcess
import org.cosmicide.plugin.runtime.hook.Hook
import org.cosmicide.plugin.runtime.hook.HookManager
import top.canyie.pine.Pine
import java.io.File
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

internal class TerminalController(
    private val context: Context,
    private val commandLine: String,
    private val workingDir: File,
    private val jdkDir: File,
    private val terminalView: TerminalView,
    private val modifierLatch: TerminalModifierLatch,
    private val scope: CoroutineScope,
    private val onTitleChanged: (String) -> Unit,
    private val onFailure: (Throwable) -> Unit,
    private val onProcessExit: (Int) -> Unit
) {
    private val closed = AtomicBoolean(false)
    private val lock = Any()
    private val terminalOutput = PtyTerminalOutput()

    private var process: PtyProcess? = null
    private var processOutput: OutputStream? = null
    private var processJob: kotlinx.coroutines.Job? = null
    private var geometry: TerminalGeometry? = null
    private var emulator: TerminalEmulator? = null

    init {
        installSelectionMenuHook()
        installTerminalInputHook()
    }

    fun startOrResize(textSizeDp: Int, typeface: Typeface) {
        if (closed.get()) return

        terminalView.applyTerminalAppearance(textSizeDp, typeface)

        val nextGeometry = terminalView.calculateGeometry(textSizeDp, typeface)
        if (nextGeometry == null) {
            return
        }

        val currentEmulator = emulator
        if (currentEmulator == null) {
            val createdEmulator = TerminalEmulator(
                terminalOutput,
                nextGeometry.columns,
                nextGeometry.rows,
                nextGeometry.cellWidthPixels,
                nextGeometry.cellHeightPixels,
                TerminalTranscriptRows,
                null
            )

            geometry = nextGeometry
            emulator = createdEmulator
            terminalView.mEmulator = createdEmulator
            terminalView.invalidate()

            startProcess(nextGeometry)
            return
        }

        if (geometry != nextGeometry) {
            geometry = nextGeometry

            currentEmulator.resize(
                nextGeometry.columns,
                nextGeometry.rows,
                nextGeometry.cellWidthPixels,
                nextGeometry.cellHeightPixels
            )

            process?.setTerminalSize(nextGeometry.rows, nextGeometry.columns)
            terminalView.invalidate()
        }
    }

    fun handleKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val ctrlDown = event.isCtrlPressed || modifierLatch.ctrl
        val altDown = event.isAltPressed || modifierLatch.alt

        val handled = handleKeyCode(
            keyCode = keyCode,
            ctrlPressed = ctrlDown,
            altPressed = altDown,
            shiftPressed = event.isShiftPressed
        )

        if (handled) {
            clearModifierLatch(ctrlDown, altDown)
            return true
        }

        val codePoint = event.unicodeChar
        if (codePoint <= 0) return false

        writeTerminalCodePoint(
            prependEscape = altDown,
            codePoint = codePoint.toTerminalCodePoint(ctrlDown)
        )

        clearModifierLatch(ctrlDown, altDown)
        return true
    }

    fun writeInputText(text: String) {
        val ctrlDown = modifierLatch.ctrl
        val altDown = modifierLatch.alt

        if (!ctrlDown && !altDown) {
            write(text)
            return
        }

        var index = 0
        while (index < text.length) {
            val codePoint = Character.codePointAt(text, index)

            writeTerminalCodePoint(
                prependEscape = altDown,
                codePoint = codePoint.toTerminalCodePoint(ctrlDown)
            )

            index += Character.charCount(codePoint)
        }

        clearModifierLatch(ctrlDown, altDown)
    }

    fun handleKeyCode(
        keyCode: Int,
        ctrlPressed: Boolean = false,
        altPressed: Boolean = false,
        shiftPressed: Boolean = false
    ): Boolean {
        val keyMod = (if (ctrlPressed) KeyHandler.KEYMOD_CTRL else 0) or
                (if (altPressed) KeyHandler.KEYMOD_ALT else 0) or
                (if (shiftPressed) KeyHandler.KEYMOD_SHIFT else 0)

        val code = KeyHandler.getCode(
            keyCode,
            keyMod,
            emulator?.isCursorKeysApplicationMode ?: false,
            emulator?.isKeypadApplicationMode ?: false
        )

        if (code != null) {
            write(code)
            return true
        }

        return false
    }

    fun writeTerminalCodePoint(prependEscape: Boolean, codePoint: Int) {
        if (prependEscape) write(Escape)

        if (codePoint in 0..31 || codePoint == 127) {
            write(byteArrayOf(codePoint.toByte()), 0, 1)
        } else {
            write(String(Character.toChars(codePoint)))
        }
    }

    fun write(data: String) {
        val bytes = data.toByteArray(StandardCharsets.UTF_8)
        write(bytes, 0, bytes.size)
    }

    fun write(data: ByteArray, offset: Int, count: Int) {
        if (closed.get()) return

        try {
            synchronized(lock) {
                processOutput?.write(data, offset, count)
                processOutput?.flush()
            }
        } catch (e: Exception) {
            if (!closed.get()) onFailure(e)
        }
    }

    fun terminate() {
        process?.terminate()
    }

    fun interrupt(): Boolean {
        return process?.interrupt() ?: false
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return

        processJob?.cancel()
        processJob = null

        process?.terminate()
        process?.close()
        process = null
        processOutput = null
    }

    private fun clearModifierLatch(ctrlDown: Boolean, altDown: Boolean) {
        if (!ctrlDown && !altDown) return
        modifierLatch.ctrl = false
        modifierLatch.alt = false
    }

    private fun startProcess(initialGeometry: TerminalGeometry) {
        if (processJob != null) return

        processJob = scope.launch(Dispatchers.IO) {
            var startedProcess: PtyProcess? = null

            try {
                val config = createTerminalConfig(
                    context = context,
                    commandLine = commandLine,
                    workingDir = workingDir,
                    jdkDir = jdkDir,
                    rows = initialGeometry.rows,
                    columns = initialGeometry.columns
                )

                val newProcess = LinuxProcessRunner.startWithPty(context, config)
                startedProcess = newProcess

                if (closed.get()) {
                    newProcess.terminate()
                    newProcess.waitFor()
                    newProcess.close()
                    return@launch
                }

                synchronized(lock) {
                    process = newProcess
                    processOutput = newProcess.getOutputStream()
                }

                val input = newProcess.getInputStream()
                val buffer = ByteArray(8192)

                while (!closed.get()) {
                    val read = input.read(buffer)
                    if (read <= 0) break

                    val chunk = buffer.copyOf(read)

                    withContext(Dispatchers.Main.immediate) {
                        emulator?.append(chunk, chunk.size)
                        terminalView.onScreenUpdated()
                    }
                }

                val exitCode = newProcess.waitFor()

                synchronized(lock) {
                    if (process === newProcess) {
                        process = null
                        processOutput = null
                    }
                }

                newProcess.close()

                if (!closed.get()) {
                    withContext(Dispatchers.Main.immediate) {
                        onTitleChanged("Exited ($exitCode)")
                        onProcessExit(exitCode)
                    }
                }
            } catch (e: Throwable) {
                if (!closed.get()) {
                    withContext(Dispatchers.Main.immediate) {
                        onFailure(e)
                    }
                }
            } finally {
                startedProcess?.close()
            }
        }
    }

    private fun installTerminalInputHook() {
        HookManager.registerHook(object : Hook(
            method = "inputCodePoint",
            argTypes = arrayOf(
                Int::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!,
                Boolean::class.javaPrimitiveType!!,
                Boolean::class.javaPrimitiveType!!
            ),
            type = TerminalView::class.java
        ) {
            override fun before(param: Pine.CallFrame) {
                val view = param.thisObject as? TerminalView ?: return
                if (view !== terminalView) return

                // If Termux session exists, let Termux handle it normally.
                if (view.mTermSession != null) return

                val codePoint = param.args[1] as Int
                val ctrlDown = (param.args[2] as Boolean) || view.mClient.readControlKey()
                val altDown = (param.args[3] as Boolean) || view.mClient.readAltKey()

                writeTerminalCodePoint(
                    prependEscape = altDown,
                    codePoint = codePoint.toTerminalCodePoint(ctrlDown)
                )

                // Void method: setting result skips original in Pine-style hooks.
                // Even if your wrapper still calls original, original just returns because mTermSession == null.
                param.result = null
            }
        })

        HookManager.registerHook(object : Hook(
            method = "handleKeyCode",
            argTypes = arrayOf(
                Int::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!
            ),
            type = TerminalView::class.java
        ) {
            override fun before(param: Pine.CallFrame) {
                val view = param.thisObject as? TerminalView ?: return
                if (view !== terminalView) return

                // If Termux has a real session, let original TerminalView handle it.
                if (view.mTermSession != null) return

                val keyCode = param.args[0] as Int
                val keyMod = param.args[1] as Int
                val term = view.mEmulator

                if (term == null) {
                    param.result = false
                    return
                }

                term.setCursorBlinkState(true)

                // Preserve Termux's built-in non-session actions, like Shift+PageUp/PageDown scrollback.
                if (view.handleKeyCodeAction(keyCode, keyMod)) {
                    param.result = true
                    return
                }

                val code = KeyHandler.getCode(
                    keyCode,
                    keyMod,
                    term.isCursorKeysApplicationMode,
                    term.isKeypadApplicationMode
                )

                if (code == null) {
                    param.result = false
                    return
                }

                write(code)
                param.result = true
            }
        })
    }

    private fun installSelectionMenuHook() {
        val controllerClass = TextSelectionCursorController::class.java

        val getSelectedTextMethod = controllerClass.getDeclaredMethod("getSelectedText").apply {
            isAccessible = true
        }
        val terminalViewField = controllerClass.getDeclaredField("terminalView").apply {
            isAccessible = true
        }

        fun outerController(callbackObject: Any): Any {
            val field = callbackObject.javaClass.getDeclaredField("this$0").apply {
                isAccessible = true
            }
            return field.get(callbackObject)!!
        }

        fun selectedText(selectionController: Any): String {
            return getSelectedTextMethod.invoke(selectionController) as? String ?: ""
        }

        HookManager.registerHook(object : Hook(
            method = "onActionItemClicked",
            argTypes = arrayOf(ActionMode::class.java, MenuItem::class.java),
            type = Class.forName("com.termux.view.textselection.TextSelectionCursorController$2")
        ) {
            override fun before(param: Pine.CallFrame) {
                val selectionController = outerController(param.thisObject)
                if (terminalViewField.get(selectionController) !== terminalView) return

                val item = param.args[1] as MenuItem

                when (item.itemId) {
                    1 -> {
                        terminalOutput.onCopyTextToClipboard(selectedText(selectionController))
                        terminalView.stopTextSelectionMode()
                        param.result = true
                    }

                    2 -> {
                        terminalView.stopTextSelectionMode()
                        terminalOutput.onPasteTextFromClipboard()
                        param.result = true
                    }
                }
            }
        })
    }

    private inner class PtyTerminalOutput : TerminalOutput() {
        override fun write(data: ByteArray, offset: Int, count: Int) {
            this@TerminalController.write(data, offset, count)
        }

        override fun titleChanged(oldTitle: String?, newTitle: String?) {
            onTitleChanged(newTitle.orEmpty())
        }

        override fun onCopyTextToClipboard(text: String?) {
            val clipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            clipboard.setPrimaryClip(
                ClipData.newPlainText("Terminal", text.orEmpty())
            )
        }

        override fun onPasteTextFromClipboard() {
            val clipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            val text = clipboard.primaryClip
                ?.getItemAt(0)
                ?.coerceToText(context)
                ?.toString()

            if (!text.isNullOrEmpty()) {
                write(text)
            }
        }

        override fun onBell() = Unit

        override fun onColorsChanged() {
            terminalView.onScreenUpdated()
        }
    }
}

private fun createTerminalConfig(
    context: Context,
    commandLine: String,
    workingDir: File,
    jdkDir: File,
    rows: Int,
    columns: Int
): LinuxProcessRunner.Configuration {
    val commandParts = LinuxProcessRunner.parseCommandLine(commandLine)
    if (commandParts.isEmpty()) {
        throw IllegalArgumentException("No command provided")
    }

    val pathEntries = LinuxProcessRunner.toolchainPathEntries(context, jdkDir)

    val binary = LinuxProcessRunner.resolveExecutable(
        commandName = commandParts.first(),
        workingDir = workingDir,
        pathEntries = pathEntries
    )

    val tempDir = context.cacheDir

    return LinuxProcessRunner.Configuration(
        binary = binary,
        arguments = commandParts.drop(1),
        workingDir = workingDir,
        environmentOverrides = LinuxProcessRunner.toolchainEnvironment(
            jdkDir
        ) + mapOf(
            "TMPDIR" to tempDir.absolutePath,
            "TMP" to tempDir.absolutePath,
            "TEMP" to tempDir.absolutePath
        ),
        pathEntries = pathEntries,
        usePty = true,
        terminalRows = rows,
        terminalColumns = columns
    )
}
