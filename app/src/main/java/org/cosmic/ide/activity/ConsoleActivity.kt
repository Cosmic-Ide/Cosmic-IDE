package org.cosmic.ide.activity

import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type
import org.cosmic.ide.App
import org.cosmic.ide.R
import org.cosmic.ide.android.task.exec.ExecuteDexTask
import org.cosmic.ide.databinding.ActivityConsoleBinding
import org.cosmic.ide.project.JavaProject
import org.cosmic.ide.util.Constants.PROJECT_PATH
import org.cosmic.ide.util.addSystemWindowInsetToPadding
import java.io.File

class ConsoleActivity : BaseActivity() {
    private lateinit var binding: ActivityConsoleBinding
    private lateinit var project: JavaProject
    private lateinit var classToExecute: String
    private var task: ExecuteDexTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsoleBinding.inflate(getLayoutInflater())
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { _ -> finish() }
        binding.toolbar.inflateMenu(R.menu.console_menu)
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_recompile -> executeDex()
                R.id.action_cancel -> finish()
            }
            true
        }

        binding.appBar.addSystemWindowInsetToPadding(false, true, false, false)
        binding.scrollView.addSystemWindowInsetToPadding(false, false, false, true)

        ViewCompat.setOnApplyWindowInsetsListener(binding.console) { _, insets ->
            val isKeyboardVisible = insets.isVisible(Type.ime())
            val keyboardHeight = insets.getInsets(Type.ime()).bottom
            if (isKeyboardVisible) {
                binding.console.setPadding(0, 0, 0, keyboardHeight)
            }
            insets
        }

        val bundle = getIntent().getExtras()

        if (bundle != null) {
            classToExecute = bundle.getString("class_to_execute")!!
            val projectPath = bundle.getString(PROJECT_PATH)
            project = JavaProject(File(projectPath!!))
            executeDex()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.console.release()
        task?.release()
    }

    private fun executeDex() {
        val console = binding.console
        console.flushInputStream()
        binding.toolbar.setSubtitle("Running")
        task = ExecuteDexTask(
            settings,
            classToExecute,
            console.getInputStream(),
            console.getOutputStream(),
            console.getErrorStream(),
            {
                binding.toolbar.setSubtitle("Stopped")
            }
        )
        task?.doFullTask(project)
    }
}
