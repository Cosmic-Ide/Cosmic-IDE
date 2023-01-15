package org.cosmic.ide.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialSharedAxis
import org.cosmic.ide.R
import org.cosmic.ide.databinding.FragmentProjectsBinding
import org.cosmic.ide.common.util.CoroutineUtil
import org.cosmic.ide.project.Project
import org.cosmic.ide.project.JavaProject
import org.cosmic.ide.ui.adapter.ProjectsAdapter
import org.cosmic.ide.ui.preference.Settings
import org.cosmic.ide.util.AndroidUtilities
import org.cosmic.ide.util.addSystemWindowInsetToPadding
import org.cosmic.ide.util.runOnUiThread
import java.io.File

class ProjectsFragment : BasePickerFragment(), ProjectsAdapter.OnProjectEventListener {
    private var _binding: FragmentProjectsBinding? = null
    private val binding get() = _binding!!
    private val projectsAdapter = ProjectsAdapter()
    private val settings = Settings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentProjectsBinding.inflate(inflater, container, false)
        binding.appBar.addSystemWindowInsetToPadding(top = true)

        projectsAdapter.onProjectEventListener = this
        binding.projectsList.apply {
            adapter = projectsAdapter
            addSystemWindowInsetToPadding(bottom = true)
        }

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_open_from_docs) {
                pickDirectory(this::onProjectClicked)
            }
            true
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadProjects()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onProjectClicked(root: File) {
        findNavController().navigate(ProjectsFragmentDirections.actionShowHomeFragment(root.absolutePath))
    }

    override fun onProjectLongClicked(project: Project): Boolean {
        showDeleteProjectDialog(project)
        return true
    }

    private fun showDeleteProjectDialog(project: Project) {
        val context = requireContext()
        AndroidUtilities.showSimpleAlert(
            requireActivity(),
            context.getString(R.string.dialog_delete),
            context.getString(R.string.dialog_confirm_delete, project.projectName),
            context.getString(android.R.string.ok),
            context.getString(android.R.string.cancel)
        ) { _, which ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                project.delete()
                loadProjects()
            }
        }
    }

    private fun loadProjects() {
        CoroutineUtil.inParallel {
            val projectsDirectory = File(settings.projectsDirectory)
            val directories =
                projectsDirectory.listFiles { file -> file.isDirectory }
            val projects = mutableListOf<Project>()

            if (directories != null) {
                directories.sortWith(Comparator.comparingLong { file -> file.lastModified() })
                for (directory in directories) {
                    projects.add(JavaProject(File(directory.absolutePath)))
                }
            }

            runOnUiThread {
                projectsAdapter.submitList(projects)
                toggleNullProject(projects)
            }
        }
    }

    private fun toggleNullProject(projects: List<Project>) {
        if (projects.isEmpty()) {
            binding.projectsList.visibility = View.GONE
            binding.emptyContainer.visibility = View.VISIBLE
        } else {
            binding.projectsList.visibility = View.VISIBLE
            binding.emptyContainer.visibility = View.GONE
        }
    }
}
