/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cosmicide.project.Project
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.adapter.ProjectAdapter
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.databinding.FragmentProjectBinding
import org.cosmicide.rewrite.model.ProjectViewModel
import org.cosmicide.rewrite.util.FileUtil
import org.cosmicide.rewrite.util.ProjectHandler
import org.cosmicide.rewrite.util.unzip


class ProjectFragment : BaseBindingFragment<FragmentProjectBinding>(),
    ProjectAdapter.OnProjectEventListener {
    private val projectAdapter = ProjectAdapter(this)
    private val viewModel by activityViewModels<ProjectViewModel>()
    private val documentPickerLauncher: ActivityResultLauncher<Intent> =
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
                        lifecycleScope.launch(Dispatchers.Main) {
                            binding.progressBar.visibility = View.GONE
                            viewModel.loadProjects()
                        }
                    }
                }
            }
        }.also { documentPickerLauncher = it }
    // for exporting a project


    override fun getViewBinding() = FragmentProjectBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setOnClickListeners()
        setUpProjectList()

        observeViewModelProjects()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProjects()
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        binding.fabs.importButton.visibility = View.GONE
        binding.fabs.newProjectTextview.visibility = View.GONE

        binding.fabs.fabNewProject.setOnClickListener {
            if (!binding.fabs.importButton.isVisible) {
                binding.fabs.importButton.visibility = View.VISIBLE
                binding.fabs.newProjectTextview.visibility = View.VISIBLE
                binding.fabs.importButton.setOnClickListener {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/zip" // Set the MIME type to filter the files, if needed
                    }

                    documentPickerLauncher.launch(intent)

                }
            } else {
                navigateToNewProjectFragment()
            }
        }
    }

    private fun setUpProjectList() {
        binding.projectList.apply {
            adapter = projectAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModelProjects() {
        viewModel.projects.observe(viewLifecycleOwner) { projects ->
            showSnackBarIfNoProjects(projects)
            projectAdapter.submitList(projects)
        }
    }

    private fun showSnackBarIfNoProjects(projects: List<Project>) {
        if (projects.isEmpty()) {
            Snackbar.make(
                requireView(),
                "No projects found",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onProjectClicked(project: Project) {
        ProjectHandler.setProject(project)
        navigateToEditorFragment()
    }

    override fun onProjectLongClicked(project: Project): Boolean {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Project")
            .setMessage("Are you sure, you want to delete ${project.name}")
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Delete") { _, _ ->
                project.delete()
                viewModel.loadProjects()
            }
            .show()
        return true
    }

    private fun navigateToNewProjectFragment() {
        setOnClickListeners()
        parentFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, NewProjectFragment())
            addToBackStack(null)
            setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        }.commit()
    }

    private fun navigateToEditorFragment() {
        parentFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, EditorFragment())
            addToBackStack(null)
            setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        }.commit()
    }
}