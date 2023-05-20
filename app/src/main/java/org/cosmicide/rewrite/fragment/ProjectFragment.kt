/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.cosmicide.project.Project
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.adapter.ProjectAdapter
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.databinding.FragmentProjectBinding
import org.cosmicide.rewrite.model.ProjectViewModel
import org.cosmicide.rewrite.util.ProjectHandler

class ProjectFragment : BaseBindingFragment<FragmentProjectBinding>(), ProjectAdapter.OnProjectEventListener {
    private val projectAdapter = ProjectAdapter(this)
    private val viewModel by activityViewModels<ProjectViewModel>()

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
                    navigateToNewProjectFragment()
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
            .setNegativeButton("Cancel"){ dialog,_ ->
                dialog.dismiss()
            }
            .setPositiveButton("Delete"){ _, _ ->
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
            setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            addToBackStack(null)
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