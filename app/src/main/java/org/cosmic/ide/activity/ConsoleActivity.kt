package org.cosmic.ide.activity

import android.content.ClipboardManager
import android.content.ClipData
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import org.cosmic.ide.R
import org.cosmic.ide.ApplicationLoader
import org.cosmic.ide.databinding.ActivityConsoleBinding
import org.cosmic.ide.project.JavaProject
import org.cosmic.ide.util.addSystemWindowInsetToPadding
import org.cosmic.ide.util.Constants.PROJECT_PATH
import org.cosmic.ide.android.task.exec.ExecuteDexTask
import org.cosmic.ide.common.util.CoroutineUtil

import java.io.File
import java.lang.reflect.InvocationTargetException


class ConsoleActivity : BaseActivity() {

    private lateinit var binding: ActivityConsoleBinding
    private lateinit var project: JavaProject
    private lateinit var classToExecute: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsoleBinding.inflate(getLayoutInflater())
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        getSupportActionBar()?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { _ -> onBackPressed() }

        binding.appbar.addSystemWindowInsetToPadding(false, true, false, false)
        binding.scrollView.addSystemWindowInsetToPadding(false, false, false, true)

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
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.console_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.getItemId()) {
            R.id.recompile_menu_bttn -> {
                executeDex()
                true
            }
            R.id.cancel_menu_bttn -> {
                finishAffinity()
                true
            }
            else -> super.onOptionsItemSelected(item) 
        }
    }

    private fun executeDex() {
        val console = binding.console
        getSupportActionBar()?.setSubtitle("Running")
        val task = ExecuteDexTask(
                ApplicationLoader.getDefaultSharedPreferences(),
                classToExecute,
                console.getInputStream(),
                console.getOutputStream(),
                console.getErrorStream(),
                {
                    // console.stop()
                    getSupportActionBar()?.setSubtitle("Stopped")
                }
        )
        task.doFullTask(project)
    }
}
