/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dev.pranav.jgit.tasks.Credentials
import dev.pranav.jgit.tasks.cloneRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.R
import org.cosmicide.adapter.ProjectAdapter
import org.cosmicide.common.Analytics
import org.cosmicide.common.BaseBindingFragment
import org.cosmicide.common.Prefs
import org.cosmicide.databinding.FragmentProjectBinding
import org.cosmicide.model.ProjectViewModel
import org.cosmicide.project.Project
import org.cosmicide.rewrite.util.FileUtil
import org.cosmicide.rewrite.util.compressToZip
import org.cosmicide.rewrite.util.unzip
import org.cosmicide.util.CommonUtils
import java.io.OutputStream
import java.io.PrintWriter

class ProjectFragment : BaseBindingFragment<FragmentProjectBinding>(),
    ProjectAdapter.OnProjectEventListener {

    private val projectAdapter = ProjectAdapter(this)
    private val viewModel by activityViewModels<ProjectViewModel>()
    private var project: Project? = null

    private val zipContract = registerForActivityResult(CreateDocument("application/zip")) { uri ->
        if (uri == null || uri.path == null) return@registerForActivityResult

        val root = project?.root ?: return@registerForActivityResult
        uri.let {
            lifecycleScope.launch(Dispatchers.Main) {
                binding.progressBar.visibility = View.VISIBLE

                val contentResolver = requireContext().contentResolver

                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri).use { outputStream ->
                        root.compressToZip(outputStream!!)
                    }
                }
                binding.progressBar.visibility = View.GONE

                CommonUtils.showSnackBar(requireView(), "Project backed up successfully")
            }
        }
    }

    private val documentPickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null) {
                    val uri = data.data ?: return@registerForActivityResult
                    val name = DocumentFile.fromSingleUri(
                        requireContext(),
                        uri
                    )!!.name?.substringBefore(".")
                    val projectPath = FileUtil.projectDir.resolve(name!!)
                    if (projectPath.exists()) {
                        Snackbar.make(
                            requireView(),
                            "Project already exists",
                            Snackbar.LENGTH_LONG
                        ).show()
                        return@registerForActivityResult
                    }
                    binding.progressBar.visibility = View.VISIBLE
                    projectPath.mkdirs()
                    lifecycleScope.launch(Dispatchers.IO) {
                        requireContext().contentResolver.openInputStream(uri)?.unzip(projectPath)
                        withContext(Dispatchers.Main) {
                            binding.progressBar.visibility = View.GONE
                            viewModel.loadProjects()
                        }
                    }
                }
            }
        }

    override fun getViewBinding() = FragmentProjectBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        askClientName()
        askForAnalyticsPermission()

        setOnClickListeners()
        binding.projectList.adapter = projectAdapter

        observeViewModelProjects()
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_settings -> {
                    parentFragmentManager.commit {
                        replace(R.id.fragment_container, SettingsFragment())
                        addToBackStack(null)
                        setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.loadProjects()
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        binding.fabs.importButton.visibility = View.GONE
        binding.fabs.fabNewProject.visibility = View.GONE
        binding.fabs.cancelText.visibility = View.GONE
        binding.fabs.gitClone.visibility = View.GONE

        binding.fabs.cancel.setOnClickListener {
            if (!binding.fabs.importButton.isVisible) {
                binding.fabs.importButton.visibility = View.VISIBLE
                binding.fabs.fabNewProject.visibility = View.VISIBLE
                binding.fabs.cancelText.visibility = View.VISIBLE
                binding.fabs.gitClone.visibility = View.VISIBLE
                binding.fabs.cancelFab.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.baseline_close_24,
                        activity?.theme
                    )
                )
                binding.fabs.importButton.setOnClickListener {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/zip"
                    }

                    documentPickerLauncher.launch(intent)
                }
                binding.fabs.fabNewProject.setOnClickListener {
                    navigateToNewProjectFragment()
                }
                binding.fabs.gitClone.setOnClickListener {
                    gitClone()
                }
            } else {
                binding.fabs.importButton.visibility = View.GONE
                binding.fabs.fabNewProject.visibility = View.GONE
                binding.fabs.cancelText.visibility = View.GONE
                binding.fabs.gitClone.visibility = View.GONE

                binding.fabs.cancelFab.setImageDrawable(
                    ResourcesCompat.getDrawable(resources, R.drawable.sharp_add_24, activity?.theme)
                )
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadProjects()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun showMenu(v: View, p: Project) {
        // show project_menu attached to view
        val popupMenu = PopupMenu(requireContext(), v)
        popupMenu.inflate(R.menu.project_menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.backup -> {
                    project = p
                    zipContract.launch("${p.name}.zip")
                    true
                }

                R.id.delete -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Delete Project")
                        .setMessage("Are you sure, you want to delete ${p.name}")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete") { _, _ ->
                            p.delete()
                            viewModel.loadProjects()
                        }
                        .show()
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }

    private fun observeViewModelProjects() {
        viewModel.projects.observe(viewLifecycleOwner) { projects ->
            projectAdapter.submitList(projects)

            if (projects.isEmpty() && binding.switcher.currentView != binding.noProjects) {
                binding.switcher.showNext()
            } else if (projects.isNotEmpty() && binding.switcher.currentView != binding.projectList) {
                binding.switcher.showPrevious()
            }
        }
    }

    override fun onProjectClicked(project: Project) {
        navigateToEditorFragment(project)
    }

    override fun onProjectLongClicked(project: Project, v: View): Boolean {
        showMenu(v, project)
        return true
    }

    fun gitClone() {
        val editText = EditText(requireContext()).apply {
            hint = "Enter git url"
        }
        if (Prefs.gitUsername.isEmpty() || Prefs.gitApiKey.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle("Git Clone")
                setMessage("Please enter your git username and api key in settings")
                setPositiveButton("Ok") { _, _ ->
                    parentFragmentManager.commit {
                        replace(R.id.fragment_container, SettingsFragment())
                        setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    }
                }
                show()
            }
            return
        }
        MaterialAlertDialogBuilder(requireContext()).apply {
            setView(editText)
            setTitle("Git Clone")
            setPositiveButton("Clone") { _, _ ->
                val url = editText.text.toString()
                var repoName = url.substringAfterLast("/")
                if (repoName.endsWith(".git")) {
                    repoName = repoName.substringBefore(".git")
                }
                val folder = FileUtil.projectDir.resolve(repoName)
                if (folder.exists()) {
                    Snackbar.make(requireView(), "Project already exists", Snackbar.LENGTH_LONG)
                        .show()
                    return@setPositiveButton
                }
                val textView = TextView(requireContext()).apply {
                    text = getString(R.string.clone)
                    setPadding(32, 32, 32, 32)
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                }
                val sheet = BottomSheetDialog(requireContext()).apply {
                    setContentView(textView)
                    setCancelable(false)
                    show()
                }
                binding.fabs.cancel.performClick()

                try {
                    lifecycleScope.launch(Dispatchers.IO) {
                        folder.cloneRepository(url,
                            PrintWriter(
                                object : OutputStream() {
                                    override fun write(p0: Int) {
                                        lifecycleScope.launch(Dispatchers.Main) {
                                            textView.append(p0.toChar().toString())
                                        }
                                    }

                                    override fun write(b: ByteArray?) {
                                        lifecycleScope.launch(Dispatchers.Main) {
                                            textView.append("\n" + b?.toString(Charsets.UTF_8))
                                        }
                                    }
                                }
                            ),
                            Credentials(Prefs.gitUsername, Prefs.gitApiKey))
                        viewModel.loadProjects()
                        lifecycleScope.launch(Dispatchers.Main) {
                            sheet.dismiss()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    sheet.dismiss()
                    lifecycleScope.launch {
                        CommonUtils.showSnackbarError(
                            requireView(),
                            e.message ?: "Unknown error",
                            e
                        )
                    }
                }
            }
        }.show()
    }

    private fun askClientName() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val enteredName = prefs.getString("client_name", null)
        if (enteredName != null) return

        val editText = EditText(requireContext()).apply {
            hint = "Enter your name"
        }

        MaterialAlertDialogBuilder(requireContext()).apply {
            setView(editText)
            setTitle("Enter your name")
            setPositiveButton("Okay") { _, _ ->
                val name = editText.text.toString()

                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT)
                        .show()
                    askClientName()
                    return@setPositiveButton
                }

                prefs.edit(commit = true) {
                    putString("client_name", name).apply()
                }
            }
            show()
        }
    }


    private fun askForAnalyticsPermission() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        if (prefs.getBoolean("analytics_preference_asked", false)) return
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(getString(R.string.analytics_permission_title))
            setMessage(R.string.analytics_permission_message)
            setCancelable(false)
            setPositiveButton(R.string.accept) { _, _ ->
                prefs.edit {
                    putBoolean("analytics_preference", true).apply()
                    putBoolean("analytics_preference_asked", true).apply()
                }
            }
            setNegativeButton(R.string.decline) { _, _ ->
                prefs.edit {
                    putBoolean("analytics_preference", false).apply()
                    putBoolean("analytics_preference_asked", true).apply()
                }
                Analytics.setAnalyticsCollectionEnabled(false)
            }
            allowEnterTransitionOverlap = true
            show()
        }
    }

    private fun navigateToNewProjectFragment() {
        setOnClickListeners()
        parentFragmentManager.commit {
            add(R.id.fragment_container, NewProjectFragment())
            addToBackStack(null)
            setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        }
    }

    private fun navigateToEditorFragment(project: Project) {
        parentFragmentManager.commit {
            add(R.id.fragment_container, EditorFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("project", project)
                }
            })
            addToBackStack(null)
            setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        }
    }
}
