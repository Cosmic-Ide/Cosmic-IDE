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
import kotlin.text.Regex

class NewProjectFragment : BaseBindingFragment<FragmentNewProjectBinding>() {
    private val viewModel: ProjectViewModel by activityViewModels()

    override fun getViewBinding() = FragmentNewProjectBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCreate.setOnClickListener {
            val projectName = binding.projectName.text.toString()
            val packageName = binding.packageName.text.toString()

            if (projectName.isEmpty()) {
                binding.projectName.error = "Project name cannot be empty"
                return@setOnClickListener
            }

            if (packageName.isEmpty()) {
                binding.packageName.error = "Package name cannot be empty"
                return@setOnClickListener
            }

            if (!projectName.matches(Regex("^[а-яА-Яa-zA-Z0-9]+$"))) {
                binding.projectName.error = "Project name contains invalid characters"
                return@setOnClickListener
            }

            if (!packageName.matches(Regex("^[a-zA-Z0-9.]+$"))) {
                binding.packageName.error = "Package name contains invalid characters"
                return@setOnClickListener
            }

            val language = when {
                binding.useKotlin.isChecked -> Language.Kotlin
                else -> Language.Java
            }

            val success = createProject(language, projectName, packageName)

            if (success) {
                parentFragmentManager.beginTransaction().apply {
                    remove(this@NewProjectFragment)
                    setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                }.commit()
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
        }.commit()
    }

    private fun File.createMainFile(language: Language, packageName: String) {
        val content = language.classFileContent(name = "Main", packageName = packageName)
        writeText(content)
    }
}