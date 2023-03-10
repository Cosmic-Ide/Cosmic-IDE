package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import org.cosmicide.project.Java
import org.cosmicide.project.Project
import org.cosmicide.rewrite.MainActivity
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.adapter.ProjectAdapter
import org.cosmicide.rewrite.databinding.FragmentProjectBinding
import org.cosmicide.rewrite.util.Constants
import org.cosmicide.rewrite.util.FileUtil
import java.io.File
import java.util.Arrays

class ProjectFragment : Fragment(), ProjectAdapter.OnProjectEventListener {

    private var _binding: FragmentProjectBinding? = null
    private val projectAdapter = ProjectAdapter().apply {
        onProjectEventListener = this@ProjectFragment
    }

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

        binding.importButton.setOnClickListener {
            findNavController().navigate(R.id.ProjectFragment_to_NewProjectFragment)
        }

        binding.fabNewProject.setOnClickListener {
            findNavController().navigate(R.id.ProjectFragment_to_NewProjectFragment)
        }

        binding.projectList.apply {
            adapter = projectAdapter
            layoutManager = LinearLayoutManager(requireActivity())
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        loadProjects()
    }

    private fun loadProjects() {
        val projects = mutableListOf<Project>()
        val projectDir = FileUtil.projectDir
        val directories =
            projectDir.listFiles { file -> file.isDirectory }
        if (directories != null) {
            Arrays.sort(directories, Comparator.comparingLong(File::lastModified).reversed())
            for (directory in directories) {
                projects.add(Project(directory, Java))
            }
        }
        projectAdapter.submitList(projects)
        Log.d("ProjectFragment", "Projects: $projects")
        if (projects.isEmpty()) {
            Snackbar.make(
                (requireActivity() as MainActivity).binding.root,
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
        findNavController().navigate(R.id.ProjectFragment_to_EditorFragment, Bundle().apply {
            putString(Constants.PROJECT_DIR, project.root.absolutePath)
        })
    }

    override fun onProjectLongClicked(project: Project): Boolean {
        return true
    }

}
