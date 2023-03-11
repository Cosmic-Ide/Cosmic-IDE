package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import org.cosmicide.project.Project
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.adapter.ProjectAdapter
import org.cosmicide.rewrite.databinding.FragmentProjectBinding
import org.cosmicide.rewrite.model.ProjectViewModel
import org.cosmicide.rewrite.util.Constants
import java.util.*

class ProjectFragment : Fragment(), ProjectAdapter.OnProjectEventListener {

    private var _binding: FragmentProjectBinding? = null
    private val projectAdapter = ProjectAdapter(this)
    private val viewModel by viewModels<ProjectViewModel>()

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setOnClickListeners()
        setUpProjectList()

        observeViewModelProjects()
    }

    private fun setOnClickListeners() {
        binding.importButton.setOnClickListener {
            navigateToNewProjectFragment()
        }

        binding.fabNewProject.setOnClickListener {
            navigateToNewProjectFragment()
        }
    }

    private fun navigateToNewProjectFragment() {
        findNavController().navigate(R.id.ProjectFragment_to_NewProjectFragment)
    }

    private fun setUpProjectList() {
        binding.projectList.apply {
            adapter = projectAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModelProjects() {
        viewModel.projects.observe(viewLifecycleOwner) { projects ->
            showSnackbarIfNoProjects(projects)
            projectAdapter.submitList(projects)
        }
    }

    private fun showSnackbarIfNoProjects(projects: List<Project>) {
        if (projects.isEmpty()) {
            Snackbar.make(
                requireView(),
                "No projects found",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onProjectClicked(project: Project) {
        navigateToEditorFragment(project.root.absolutePath)
    }

    private fun navigateToEditorFragment(projectDir: String) {
        val bundle = Bundle().apply {
            putString(Constants.PROJECT_DIR, projectDir)
        }
        findNavController().navigate(R.id.ProjectFragment_to_EditorFragment, bundle)
    }

    override fun onProjectLongClicked(project: Project): Boolean {
        return false
    }
}