package org.cosmic.ide.activity

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
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
import org.cosmic.ide.git.model.Author
import org.cosmic.ide.git.usecases.createGitRepoWith
import org.cosmic.ide.project.JavaProject
import org.cosmic.ide.project.KotlinProject
import org.cosmic.ide.project.Project
import org.cosmic.ide.util.AndroidUtilities
import org.cosmic.ide.util.Constants
import org.cosmic.ide.util.addSystemWindowInsetToMargin
import org.cosmic.ide.util.addSystemWindowInsetToPadding
import java.io.File
import java.io.IOException
import java.util.*

class ProjectActivity : BaseActivity(), OnProjectEventListener {
    private var projectAdapter: ProjectAdapter? = null
    private var createNewProjectDialog: AlertDialog? = null
    private var projectBinding: DialogNewProjectBinding? = null
    private var binding: ActivityProjectBinding? = null
    private var mListener: OnProjectCreatedListener? = null

    interface OnProjectCreatedListener {
        fun onProjectCreated(project: Project?)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        setSupportActionBar(binding!!.toolbar)
        buildCreateNewProjectDialog()
        projectAdapter = ProjectAdapter()
        binding!!.projectRecycler.adapter = projectAdapter
        binding!!.projectRecycler.layoutManager = LinearLayoutManager(this)
        binding!!.projectRecycler.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )
        projectAdapter!!.setOnProjectEventListener(this)
        mListener = object : OnProjectCreatedListener {
            override fun onProjectCreated(project: Project?) {
                onProjectClicked(
                    project!!
                )
            }
        }
        binding!!.fab.addSystemWindowInsetToMargin(
            left = false,
            top = false,
            right = false,
            bottom = true
        )
        binding!!.appBar.addSystemWindowInsetToPadding(
            left = false,
            top = true,
            right = false,
            bottom = false
        )
        binding!!.projectRecycler.addSystemWindowInsetToPadding(
            left = false,
            top = false,
            right = false,
            bottom = true
        )
        binding!!.refreshLayout.setOnRefreshListener {
            loadProjects()
            binding!!.refreshLayout.isRefreshing = false
        }
        binding!!.fab.setOnClickListener { showCreateNewProjectDialog() }
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

    public override fun onResume() {
        super.onResume()
        loadProjects()
    }

    private fun buildCreateNewProjectDialog() {
        val builder = MaterialAlertDialogBuilder(
            this, AndroidUtilities.getDialogFullWidthButtonsThemeOverlay()
        )
            .setTitle(getString(R.string.create_project))
        projectBinding = DialogNewProjectBinding.inflate(LayoutInflater.from(builder.context))
        builder
            .setView(projectBinding!!.root)
            .setPositiveButton(getString(R.string.create), null)
            .setNegativeButton(getString(android.R.string.cancel), null)
        createNewProjectDialog = builder.create()
    }

    @WorkerThread
    private fun showCreateNewProjectDialog() {
        if (!createNewProjectDialog!!.isShowing) {
            createNewProjectDialog!!.show()
            val createBtn = createNewProjectDialog!!.findViewById<Button>(android.R.id.button1)
            createBtn!!.setOnClickListener {
                val projectName: String =
                    projectBinding!!.text1.text.toString().trim().replace("..", "")
                if (projectName.isEmpty()) {
                    return@setOnClickListener
                }
                val useKotlinTemplate: Boolean =
                    projectBinding!!.useKotlinTemplate.isChecked
                try {
                    val project: Project =
                        if (useKotlinTemplate) KotlinProject.newProject(
                            projectName
                        ) else JavaProject.newProject(projectName)
                    if (projectBinding!!.useGit.isChecked) {
                        val author: Author = Author(settings.gitUserName, settings.gitUserEmail)
                        project.projectDirPath.createGitRepoWith(
                            author,
                            "Initial Commit"
                        )
                    }
                    if (mListener != null) {
                        runOnUiThread {
                            if (createNewProjectDialog!!.isShowing) createNewProjectDialog!!.dismiss()
                            mListener!!.onProjectCreated(project)
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                loadProjects()
            }
            projectBinding!!.text1.setText("")
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
        ) { _: DialogInterface?, which: Int ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                project.delete()
                runOnUiThread { loadProjects() }
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
            val directories: Array<File>? =
                projectDir.listFiles { obj: File -> obj.isDirectory }
            val projects: ArrayList<Project> =
                ArrayList()
            if (directories != null) {
                Arrays.sort(
                    directories, Comparator.comparingLong { obj: File -> obj.lastModified() }
                )
                for (directory: File in directories) {
                    val project = File(directory, "src")
                    if (project.exists()) {
                        val javaProject =
                            JavaProject(File(directory.absolutePath))
                        projects.add(javaProject)
                    }
                }
            }
            runOnUiThread {
                projectAdapter!!.submitList(projects)
                toggleNullProject(projects)
            }
        }
    }

    private fun toggleNullProject(projects: List<Project>) {
        if (projects.isEmpty()) {
            binding!!.projectRecycler.visibility = View.GONE
            binding!!.emptyContainer.visibility = View.VISIBLE
        } else {
            binding!!.projectRecycler.visibility = View.VISIBLE
            binding!!.emptyContainer.visibility = View.GONE
        }
    }
}