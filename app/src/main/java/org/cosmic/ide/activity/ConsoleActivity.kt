package org.cosmic.ide.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type
import org.cosmic.ide.R
import org.cosmic.ide.android.task.exec.ExecuteDexTask
import org.cosmic.ide.databinding.ActivityConsoleBinding
import org.cosmic.ide.project.JavaProject
import org.cosmic.ide.util.Constants.PROJECT_PATH
import org.cosmic.ide.util.addSystemWindowInsetToPadding
import java.io.File

class ConsoleActivity : BaseActivity() {
    private lateinit var project: JavaProject
    private lateinit var classToExecute: String
    private lateinit var task: ExecuteDexTask
    private lateinit var binding: ActivityConsoleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsoleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.root.addSystemWindowInsetToPadding(
            left = false,
            top = true,
            right = false,
            bottom = true
        )

        ViewCompat.setOnApplyWindowInsetsListener(binding.console) { _, insets ->
            val isKeyboardVisible = insets.isVisible(Type.ime())
            val keyboardHeight = insets.getInsets(Type.ime()).bottom
            if (isKeyboardVisible) {
                binding.console.setPadding(0, 0, 0, keyboardHeight)
            }
            insets
        }

        val bundle = intent.extras

        if (bundle != null) {
            classToExecute = bundle.getString("class_to_execute")!!
            val projectPath = bundle.getString(PROJECT_PATH)
            project = JavaProject(File(projectPath!!))
            supportActionBar?.title = project.projectName
            executeDex()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.console_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_recompile -> executeDex()
            R.id.action_cancel -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.console.release()
        task.release()
    }

    private fun executeDex() {
        val console = binding.console
        console.flushInputStream()
        supportActionBar?.subtitle = getString(R.string.console_state_running)
        task = ExecuteDexTask(
            settings.prefs,
            classToExecute,
            console.inputStream,
            console.outputStream,
            console.errorStream
        ) {
            supportActionBar?.subtitle = getString(R.string.console_state_stopped)
        }
        task.doFullTask(project)
    }
}
