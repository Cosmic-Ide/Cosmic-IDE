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
        }.commitNow()
    }

    private fun navigateToEditorFragment() {
        parentFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, EditorFragment())
            addToBackStack(null)
            setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        }.commitNow()
    }
}