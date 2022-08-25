package org.cosmic.ide

import android.content.ClipboardManager
import android.content.ClipData
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.cosmic.ide.databinding.ActivityConsoleBinding
import org.cosmic.ide.project.JavaProject
import org.cosmic.ide.ui.utils.addSystemWindowInsetToPadding
import org.cosmic.ide.android.task.exec.ExecuteDexTask
import java.io.File
import java.lang.reflect.InvocationTargetException

class ConsoleActivity : BaseActivity() {

    private lateinit var binding: ActivityConsoleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsoleBinding.inflate(getLayoutInflater())
        setContentView(binding.getRoot())

        setSupportActionBar(binding.toolbar)
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()?.setHomeButtonEnabled(true)
        binding.toolbar.setNavigationOnClickListener { _ -> onBackPressed() }

        val bundle = getIntent().getExtras()

        if (bundle != null) {
            val clazz = bundle.getString("class_to_execute")
            val projectPath = bundle.getString("project_path")
            val console = binding.console
            val project = JavaProject(File(projectPath!!))
            val task = ExecuteDexTask(ApplicationLoader.getDefaultSharedPreferences(), clazz!!, console.getInputStream(), console.getOutputStream(), console.getErrorStream())
            try {
                task.doFullTask(project)
            } catch (e: Throwable) {
                e.printStackTrace(console.getErrorStream())
            } 
        }
    }
}
