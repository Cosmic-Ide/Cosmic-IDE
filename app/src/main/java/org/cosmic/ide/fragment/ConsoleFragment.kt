package org.cosmic.ide.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.transition.MaterialFadeThrough
import org.cosmic.ide.R
import org.cosmic.ide.android.task.exec.ExecuteDexTask
import org.cosmic.ide.databinding.FragmentConsoleBinding
import org.cosmic.ide.project.Project
import org.cosmic.ide.project.JavaProject
import org.cosmic.ide.ui.preference.Settings
import org.cosmic.ide.ui.widget.ConsoleEditText
import org.cosmic.ide.util.addSystemWindowInsetToPadding
import java.io.File

class ConsoleFragment : Fragment() {
    private val args: ConsoleFragmentArgs by navArgs()
    private var classToExecute: String? = null
    private var _binding: FragmentConsoleBinding? = null
    private val binding get() = _binding!!
    private var project: Project? = null
    private val settings = Settings()
    private lateinit var task: ExecuteDexTask

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()

        project = JavaProject(File(args.projectPath))
        classToExecute = args.projectPath
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentConsoleBinding.inflate(inflater, container, false)
        binding.root.addSystemWindowInsetToPadding(top = true, bottom = true)

        binding.toolbar.title = project?.projectName
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_recompile -> executeDex(binding.console)
                R.id.action_cancel -> findNavController().navigateUp()
            }
            true
        }

        binding.console.setOnApplyWindowInsetsListener { view, insets ->
            val isKeyboardVisible = insets.isVisible(Type.ime())
            val keyboardHeight = insets.getInsets(Type.ime()).bottom
            if (isKeyboardVisible) {
                view.updatePadding(bottom = keyboardHeight)
            }
            insets
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        executeDex(binding.console)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        classToExecute = null
        project = null
    }

    private fun executeDex(consoleView: ConsoleEditText) {
        if (project != null && classToExecute != null) {
            consoleView.flushInputStream()
            binding.toolbar.subtitle = consoleView.context.getString(R.string.console_state_running)
            task = ExecuteDexTask(
                settings.prefs,
                classToExecute!!,
                consoleView.inputStream,
                consoleView.outputStream,
                consoleView.errorStream
            ) {
                binding.toolbar.subtitle = consoleView.context.getString(R.string.console_state_stopped)
            }
            task.doFullTask(project!!)
        }
    }
}
