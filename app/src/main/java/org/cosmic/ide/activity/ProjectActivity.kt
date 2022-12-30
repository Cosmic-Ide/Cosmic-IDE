package org.cosmic.ide.activity

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.pedrovgs.lynx.LynxActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.cosmic.ide.R
import org.cosmic.ide.activity.adapter.ProjectAdapter
import org.cosmic.ide.activity.adapter.ProjectAdapter.OnProjectEventListener
import org.cosmic.ide.common.util.CoroutineUtil.inParallel
import org.cosmic.ide.databinding.ActivityProjectBinding
import org.cosmic.ide.databinding.DialogNewProjectBinding
import org.cosmic.ide.project.JavaProject
import org.cosmic.ide.project.KotlinProject
import org.cosmic.ide.project.Project
import org.cosmic.ide.util.AndroidUtilities
import org.cosmic.ide.util.Constants
import org.cosmic.ide.util.addSystemWindowInsetToMargin
import org.cosmic.ide.util.addSystemWindowInsetToPadding
import java.io.File
import java.io.IOException

class ProjectActivity : BaseActivity(), OnProjectEventListener {
    private val projectAdapter = ProjectAdapter()
    private val projectBinding by lazy {
        DialogNewProjectBinding.inflate(layoutInflater)
    }
    private val createNewProjectDialog: AlertDialog by lazy {
        val dialog = MaterialAlertDialogBuilder(
            this, AndroidUtilities.dialogFullWidthButtonsThemeOverlay
        ).apply {
            setTitle(getString(R.string.create_project))
            setView(projectBinding.root)
            setPositiveButton(getString(R.string.create), null)
            setNegativeButton(getString(android.R.string.cancel), null)
        }
        dialog.create()
    }

    private val mListener = object : OnProjectCreatedListener {
        override fun onProjectCreated(project: Project) {
            onProjectClicked(project)
        }
    }

    private lateinit var binding: ActivityProjectBinding

    interface OnProjectCreatedListener {
        fun onProjectCreated(project: Project)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectBinding.inflate(layoutInflater).also {
            setContentView(it.root)
            setSupportActionBar(it.toolbar)
        }
        binding.projectRecycler.apply {
            adapter = projectAdapter
            layoutManager = LinearLayoutManager(this@ProjectActivity)
            addItemDecoration(
                DividerItemDecoration(
                    this@ProjectActivity,
                    DividerItemDecoration.VERTICAL
                )
            )
            addSystemWindowInsetToPadding(bottom = true)
        }
        binding.apply {
            fab.addSystemWindowInsetToMargin(bottom = true)
            appBar.addSystemWindowInsetToPadding(top = true)
            refreshLayout.setOnRefreshListener {
                loadProjects()
                refreshLayout.isRefreshing = false
            }
            fab.setOnClickListener { showCreateNewProjectDialog() }
        }
        projectAdapter.onProjectEventListener = this@ProjectActivity
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.projects_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
        } else if (id == R.id.action_logcat) {
            startActivity(LynxActivity.getIntent(this))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        loadProjects()
    }

    @WorkerThread
    private fun showCreateNewProjectDialog() {
        if (!createNewProjectDialog.isShowing) {
            createNewProjectDialog.show()
            val createBtn: Button = createNewProjectDialog.findViewById(android.R.id.button1)!!
            createBtn.setOnClickListener {
                val projectName =
                    projectBinding.text1.text.toString().trim().replace("..", "")
                if (projectName.isEmpty()) {
                    return@setOnClickListener
                }
                val useKotlinTemplate =
                    projectBinding.useKotlinTemplate.isChecked
                try {
                    val project = if (useKotlinTemplate) {
                        KotlinProject.newProject(projectName)
                    } else {
                        JavaProject.newProject(projectName)
                    }
                    runOnUiThread {
                        if (createNewProjectDialog.isShowing) {
                            createNewProjectDialog.dismiss()
                        }
                        mListener.onProjectCreated(project)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                loadProjects()
            }
            projectBinding.text1.setText("")
        }
    }

    @WorkerThread
    private fun showDeleteProjectDialog(project: Project) {
        AndroidUtilities.showSimpleAlert(
            this,
            getString(R.string.dialog_delete),
            getString(R.string.dialog_confirm_delete, project.projectName),
            getString(android.R.string.ok),
            getString(android.R.string.cancel)
        ) { _, which ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                project.delete()
                loadProjects()
            }
        }
    }

    override fun onProjectClicked(project: Project) {
        val projectPath = project.projectDirPath
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(Constants.PROJECT_PATH, projectPath)
        startActivity(intent)
    }

    override fun onProjectLongClicked(project: Project): Boolean {
        showDeleteProjectDialog(project)
        return true
    }

    @WorkerThread
    private fun loadProjects() {
        inParallel {
            val projectDir = File(JavaProject.getRootDirPath())
            val directories =
                projectDir.listFiles { file -> file.isDirectory }
            val projects = mutableListOf<Project>()
            if (directories != null) {
                directories.sortWith(Comparator.comparingLong { file -> file.lastModified() })
                for (directory in directories) {
                    val src = File(directory, "src")
                    if (src.exists()) {
                        val project =
                            JavaProject(File(directory.absolutePath))
                        projects.add(project)
                    }
                }
            }
            runOnUiThread {
                projectAdapter.submitList(projects)
                toggleNullProject(projects)
            }
        }
    }

    private fun toggleNullProject(projects: List<Project>) {
        if (projects.isEmpty()) {
            binding.projectRecycler.visibility = View.GONE
            binding.emptyContainer.visibility = View.VISIBLE
        } else {
            binding.projectRecycler.visibility = View.VISIBLE
            binding.emptyContainer.visibility = View.GONE
        }
    }
}
