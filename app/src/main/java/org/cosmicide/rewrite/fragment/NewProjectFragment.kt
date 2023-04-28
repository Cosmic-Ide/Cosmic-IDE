package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import org.cosmicide.project.Language
import org.cosmicide.project.Project
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.databinding.FragmentNewProjectBinding
import org.cosmicide.rewrite.model.ProjectViewModel
import org.cosmicide.rewrite.util.FileUtil
import org.cosmicide.rewrite.util.ProjectHandler
import java.io.File
import java.io.IOException

class NewProjectFragment : BaseBindingFragment<FragmentNewProjectBinding>() {
    private val viewModel: ProjectViewModel by activityViewModels()

    override fun getViewBinding() = FragmentNewProjectBinding.inflate(layoutInflater)

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
            val success = createProject(language, projectName, packageName)
            if (success) {
                parentFragmentManager.beginTransaction().apply {
                    remove(this@NewProjectFragment)
                    setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                }.commitNow()
            }
        }
    }

    private fun createProject(
        language: Language,
        name: String,
        packageName: String
    ): Boolean {
        return try {
            val projectName = name.replace("\\.", "")
            val root = File(FileUtil.projectDir, projectName).apply { mkdirs() }
            val project = Project(root = root, language = language)
            val srcDir = project.srcDir.invoke().apply { mkdirs() }
            val mainFile = File(srcDir, "Main.${language.extension}")
            mainFile.createMainFile(language, packageName)
            viewModel.loadProjects()
            ProjectHandler.setProject(project)
            navigateToEditorFragment()
            true
        } catch (e: IOException) {
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun navigateToEditorFragment() {
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, EditorFragment())
            setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        }.commitNow()
    }

    private fun File.createMainFile(language: Language, packageName: String) {
        val content = language.classFileContent(name = "Main", packageName = packageName)
        writeText(content)
    }
}