package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import org.cosmicide.project.Project
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.adapter.ProjectAdapter
import org.cosmicide.rewrite.databinding.FragmentProjectBinding
import org.cosmicide.rewrite.model.ProjectViewModel
import org.cosmicide.rewrite.util.ProjectHandler

class ProjectFragment : Fragment(), ProjectAdapter.OnProjectEventListener {

    private val projectAdapter = ProjectAdapter(this)
    private val viewModel by activityViewModels<ProjectViewModel>()

    private lateinit var binding: FragmentProjectBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setOnClickListeners()
        setUpProjectList()

        observeViewModelProjects()
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
        return false
    }

    private fun navigateToNewProjectFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, NewProjectFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToEditorFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, EditorFragment())
            .addToBackStack(null)
            .commit()
    }
}