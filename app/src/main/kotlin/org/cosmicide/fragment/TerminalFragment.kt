/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.fragment

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.KeyboardUtils
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalRenderer
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import kotlinx.coroutines.launch
import org.cosmicide.R
import org.cosmicide.common.BaseBindingFragment
import org.cosmicide.databinding.FragmentTerminalBinding
import org.cosmicide.project.Project
import org.cosmicide.util.ProjectHandler
import java.io.File

/**
 * A fragment for displaying information about the compilation process.
 */
class TerminalFragment : BaseBindingFragment<FragmentTerminalBinding>() {
    val project: Project? = ProjectHandler.getProject()

    override fun getViewBinding() = FragmentTerminalBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.terminalView.attachSession(getTerminalSession())
        binding.terminalView.setTerminalViewClient(TerminalClient(binding.terminalView))
        binding.terminalView.mRenderer =
            TerminalRenderer(28, ResourcesCompat.getFont(requireContext(), R.font.noto_sans_mono)!!)
        binding.terminalView.requestFocus()
        KeyboardUtils.showSoftInput(binding.terminalView)
    }

    private fun getTerminalSession(): TerminalSession {
        val cwd = project?.root?.absolutePath
            ?: if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                Environment.getExternalStorageDirectory().absolutePath
            } else {
                requireContext().filesDir.absolutePath
            }
        var shell = "/bin/sh"

        if (File(shell).exists().not()) {
            shell = "/system/bin/sh"
        }

        return TerminalSession(
            shell,
            cwd,
            arrayOf<String>(),
            arrayOf(),
            TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
            getTermSessionClient()
        )
    }

    private fun getTermSessionClient(): TerminalSessionClient {
        return object : TerminalSessionClient {
            override fun onTextChanged(changedSession: TerminalSession) {
                lifecycleScope.launch {
                    binding.terminalView.onScreenUpdated()
                }
            }

            override fun onTitleChanged(updatedSession: TerminalSession) {}

            override fun onSessionFinished(finishedSession: TerminalSession) {
                lifecycleScope.launch {
                    binding.terminalView.let {
                        KeyboardUtils.hideSoftInput(it)
                        it.mTermSession?.finishIfRunning()
                    }
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }

            override fun onCopyTextToClipboard(session: TerminalSession, text: String?) {
                ClipboardUtils.copyText(text)
            }

            override fun onPasteTextFromClipboard(session: TerminalSession?) {
                lifecycleScope.launch {
                    val clip = ClipboardUtils.getText().toString()
                    if (clip.trim { it <= ' ' }
                            .isNotEmpty() && binding.terminalView.mEmulator != null) {
                        binding.terminalView.mEmulator.paste(clip)
                    }
                }
            }

            override fun onBell(session: TerminalSession) {}

            override fun onColorsChanged(changedSession: TerminalSession) {}

            override fun onTerminalCursorStateChange(state: Boolean) {}
            override fun setTerminalShellPid(session: TerminalSession, pid: Int) {
                Log.d("TerminalFragment", "setTerminalShellPid: $pid")
            }

            override fun getTerminalCursorStyle(): Int {
                return TerminalEmulator.TERMINAL_CURSOR_STYLE_UNDERLINE
            }

            override fun logError(tag: String?, message: String?) {
                if (message != null) {
                    Log.e(tag, message)
                }
            }

            override fun logWarn(tag: String?, message: String?) {
                if (message != null) {
                    Log.w(tag, message)
                }
            }

            override fun logInfo(tag: String?, message: String?) {
                if (message != null) {
                    Log.i(tag, message)
                }
            }

            override fun logDebug(tag: String?, message: String?) {
                if (message != null) {
                    Log.d(tag, message)
                }
            }

            override fun logVerbose(tag: String?, message: String?) {
                if (message != null) {
                    Log.v(tag, message)
                }
            }

            override fun logStackTraceWithMessage(
                tag: String?,
                message: String?,
                e: Exception?
            ) {
                Log.e(tag, message + "\n" + Log.getStackTraceString(e))
            }

            override fun logStackTrace(tag: String?, e: Exception?) {
                Log.e(tag, Log.getStackTraceString(e))
            }

        }
    }

    class TerminalClient(val terminal: TerminalView) : TerminalViewClient {
        override fun logError(tag: String?, message: String?) {
            if (message != null) {
                Log.e(tag, message)
            }
        }

        override fun logWarn(tag: String?, message: String?) {
            if (message != null) {
                Log.w(tag, message)
            }
        }

        override fun logInfo(tag: String?, message: String?) {
            if (message != null) {
                Log.i(tag, message)
            }
        }

        override fun logDebug(tag: String?, message: String?) {
            if (message != null) {
                Log.d(tag, message)
            }
        }

        override fun logVerbose(tag: String?, message: String?) {
            if (message != null) {
                Log.v(tag, message)
            }
        }

        override fun logStackTraceWithMessage(
            tag: String?,
            message: String?,
            e: Exception?
        ) {
            Log.e(tag, message + "\n" + Log.getStackTraceString(e))
        }

        override fun logStackTrace(tag: String?, e: Exception?) {
            Log.e(tag, Log.getStackTraceString(e))
        }

        override fun onScale(scale: Float): Float {
            return scale
        }

        override fun onSingleTapUp(e: MotionEvent?) {
            if (terminal.mTermSession.isRunning) {
                terminal.requestFocus()
                KeyboardUtils.showSoftInput(terminal)
            }
        }

        override fun shouldBackButtonBeMappedToEscape(): Boolean {
            return false
        }

        override fun shouldEnforceCharBasedInput(): Boolean {
            return true
        }

        override fun shouldUseCtrlSpaceWorkaround(): Boolean {
            return false
        }

        override fun isTerminalViewSelected(): Boolean {
            return true
        }

        override fun copyModeChanged(copyMode: Boolean) {}

        override fun onKeyDown(keyCode: Int, e: KeyEvent?, session: TerminalSession?): Boolean {
            return false
        }

        override fun onKeyUp(keyCode: Int, e: KeyEvent?): Boolean {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (terminal.mTermSession.isRunning) {
                    terminal.mTermSession.finishIfRunning()
                }
                return true
            }
            return false
        }

        override fun onLongPress(event: MotionEvent?): Boolean {
            return false
        }

        override fun readControlKey(): Boolean {
            return false
        }

        override fun readAltKey(): Boolean {
            return false
        }

        override fun readShiftKey(): Boolean {
            return false
        }

        override fun readFnKey(): Boolean {
            return false
        }

        override fun onCodePoint(
            codePoint: Int,
            ctrlDown: Boolean,
            session: TerminalSession?
        ): Boolean {
            return false
        }

        override fun onEmulatorSet() {}
    }
}
