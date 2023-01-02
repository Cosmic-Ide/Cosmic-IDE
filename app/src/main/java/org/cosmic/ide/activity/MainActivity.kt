package org.cosmic.ide.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import com.github.pedrovgs.lynx.LynxActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.cosmic.ide.R
import org.cosmic.ide.activity.adapter.MainActionsListAdapter
import org.cosmic.ide.activity.model.MainScreenAction
import org.cosmic.ide.databinding.ActivityMainBinding
import org.cosmic.ide.databinding.DialogNewProjectBinding
import org.cosmic.ide.project.JavaProject
import org.cosmic.ide.project.KotlinProject
import org.cosmic.ide.util.addSystemWindowInsetToPadding
import org.cosmic.ide.util.AndroidUtilities
import org.cosmic.ide.util.Constants.PROJECT_PATH
import java.io.File

interface OnProjectCreatedListener {
    fun openProject(root: File)
}

class MainActivity : BaseActivity(), OnProjectCreatedListener {
    private lateinit var binding: ActivityMainBinding

    private var onProjectCreatedListener: OnProjectCreatedListener? = null
    private val newProjectBinding by lazy {
        DialogNewProjectBinding.inflate(layoutInflater)
    }
    private val newProjectDialog: AlertDialog by lazy {
        val dialog = MaterialAlertDialogBuilder(
            this, AndroidUtilities.dialogFullWidthButtonsThemeOverlay)
        dialog.apply {
            setTitle(getString(R.string.create_project))
            setView(newProjectBinding.root)
            setPositiveButton(getString(R.string.create), null)
            setNegativeButton(getString(android.R.string.cancel), null)
            setOnDismissListener {
                newProjectBinding.text1.setText("")
            }
        }.create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onProjectCreatedListener = this;

        val createProject =
            MainScreenAction(R.string.create_project, R.drawable.ic_add) { showNewProject() }
        val openProject =
            MainScreenAction(R.string.open_project, R.drawable.ic_folder) { pickDirectory() }
        val openSettings =
            MainScreenAction(R.string.settings, R.drawable.ic_settings) { gotoSettings() }
        val openLogcat =
            MainScreenAction(R.string.logcat, R.drawable.ic_list_alt) { gotoLynxActivity() }

        binding.root.addSystemWindowInsetToPadding(top = true, bottom = true)

        binding.getStartedActions.adapter =
            MainActionsListAdapter(
                listOf(
                    createProject,
                    openProject,
                    openSettings,
                    openLogcat
                )
            )
    }

    private fun gotoSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun gotoLynxActivity() {
        startActivity(LynxActivity.getIntent(this))
    }

    private fun pickDirectory() {
        pickDirectory(this::openProject)
    }

    override fun openProject(root: File) {
        val intent = Intent(this, HomeActivity::class.java).also {
            it.putExtra(PROJECT_PATH, root.absolutePath)
        }
        startActivity(intent)
    }

    private fun showNewProject() {
        val createButton = newProjectDialog.getButton(AlertDialog.BUTTON_POSITIVE)

        createButton?.setOnClickListener {
            val projectName =
                newProjectBinding.text1.text.toString().trim().replace("..", "")
            val kotlinChecked =
                newProjectBinding.useKotlinTemplate.isChecked

            if (projectName.isNullOrBlank()) {
                return@setOnClickListener
            }

            val project = if (kotlinChecked) {
                    KotlinProject.newProject(projectName)
                } else {
                    JavaProject.newProject(projectName)
                }

            runOnUiThread {
                onProjectCreatedListener?.openProject(project.rootFile)

                if (newProjectDialog.isShowing) {
                    newProjectDialog.dismiss()
                }
            }
        }

        if (!newProjectDialog.isShowing) {
            newProjectDialog.show()
        }
    }
}