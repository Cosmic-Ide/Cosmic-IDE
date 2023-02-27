package org.cosmic.ide.activity

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.pedrovgs.lynx.LynxActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.cosmic.ide.R
import org.cosmic.ide.activity.adapter.ProjectAdapter
import org.cosmic.ide.activity.adapter.ProjectAdapter.OnProjectEventListener
import org.cosmic.ide.common.util.CoroutineUtil.inParallel
import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.common.util.unzip
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
import java.util.Arrays
import java.util.Comparator

class ProjectActivity : BaseActivity(), OnProjectEventListener {
    private val REQUEST_CODE_SELECT_PROJECT = 0
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
    private val importListener = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == RESULT_OK) {
                        result.data?.data?.let { uri ->
                            val file = DocumentFile.fromSingleUri(this, uri)!!
                            val name = file.name?.substringBeforeLast(".")
                            val path = FileUtil.getProjectsDir() + name
                            val projectDir = File(path)
                            if (projectDir.exists()) {
                                MaterialAlertDialogBuilder(this)
                                    .setTitle("Project already exists")
                                    .setMessage("Do you want to overwrite it?")
                                    .setPositiveButton("Yes") { _, _ ->
                                        projectDir.deleteRecursively()
                                        contentResolver.openInputStream(uri)!!.use {
                                            it.unzip(File(FileUtil.getProjectsDir()))
                                            loadProjects()
                                        }
                                    }
                                    .setNegativeButton("No") { _, _ -> }
                                    .show()
                            } else {
                                contentResolver.openInputStream(uri)!!.use {
                                    it.unzip(projectDir)
                                    loadProjects()
                                }
                            }
                        }
                    }
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
            fab.setOnClickListener {
                showCreateNewProjectDialog()
            }
        }
        projectAdapter.onProjectEventListener = this@ProjectActivity
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.projects_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }

            R.id.action_logcat -> {
                startActivity(LynxActivity.getIntent(this))
            }

            R.id.import_project -> {
                // Create an intent for the user to select a directory
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "application/zip"
                }
                importListener.launch(intent)
            }
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
                    if (projectBinding.useGit.isChecked) {
                        val author = Author(settings.gitUserName, settings.gitUserEmail)
                        project.projectDirPath.createGitRepoWith(
                            author,
                            "Initial Commit"
                        )
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
                Arrays.sort(directories, Comparator.comparingLong(File::lastModified).reversed())
                for (directory in directories) {
                    projects.add(JavaProject(directory))
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
