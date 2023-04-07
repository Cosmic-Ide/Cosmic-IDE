package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.cosmicide.project.Language
import org.cosmicide.project.Project
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.databinding.FragmentNewProjectBinding
import org.cosmicide.rewrite.model.ProjectViewModel
import org.cosmicide.rewrite.util.Constants
import org.cosmicide.rewrite.util.FileUtil
import org.cosmicide.rewrite.util.ProjectHandler
import java.io.File
import java.io.IOException

class NewProjectFragment : Fragment() {

    private lateinit var binding: FragmentNewProjectBinding
    private val viewModel: ProjectViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewProjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCreate.setOnClickListener {
            val projectName = binding.projectName.text?.toString()
            val packageName = binding.packageName.text?.toString()
            if (projectName.isNullOrEmpty()) {
                binding.projectName.error = "Project name cannot be empty"
                return@setOnClickListener
            }
            if (packageName.isNullOrEmpty()) {
                binding.packageName.error = "Package name cannot be empty"
                return@setOnClickListener
            }

            val language = if (binding.useKotlin.isChecked) Language.Kotlin else Language.Java
            createProject(language, projectName, packageName)
            parentFragmentManager.popBackStack()
        }
    }

    private fun createProject(language: Language, projectName: String, packageName: String) {
        try {
            val root = File(FileUtil.projectDir, projectName).apply { mkdirs() }
            val project = Project(root = root, language = language)
            val srcDir = project.srcDir.invoke().apply { mkdirs() }
            val mainFile = File(srcDir, "Main.${language.extension}")
            mainFile.createMainFile(language, packageName)
            viewModel.loadProjects()
            ProjectHandler.setProject(project)
        } catch (e: IOException) {
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun File.createMainFile(language: Language, packageName: String) {
        val content = language.classFileContent(name = "Main", packageName = packageName)
        writeText(content)
    }
}