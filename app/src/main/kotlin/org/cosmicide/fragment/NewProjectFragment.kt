/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import com.google.android.material.snackbar.Snackbar
import org.cosmicide.R
import org.cosmicide.databinding.FragmentNewProjectBinding
import org.cosmicide.model.ProjectViewModel
import org.cosmicide.project.Language
import org.cosmicide.project.Project
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.util.FileUtil
import java.io.File
import java.io.IOException

class NewProjectFragment : BaseBindingFragment<FragmentNewProjectBinding>() {
    private val viewModel: ProjectViewModel by activityViewModels()

    override fun getViewBinding() = FragmentNewProjectBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnCreate.setOnClickListener {
            val projectName = binding.projectName.editText?.text.toString()
            val packageName = binding.packageName.editText?.text.toString()

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
                parentFragmentManager.commit {
                    remove(this@NewProjectFragment)
                    setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                }
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
            val root = FileUtil.projectDir.resolve(projectName).apply { mkdirs() }
            val project = Project(root = root, language = language)
            val srcDir = project.srcDir.apply { mkdirs() }
            val mainFile = srcDir.resolve("Main.${language.extension}")
            mainFile.createMainFile(language, packageName)
            viewModel.loadProjects()
            navigateToEditorFragment(project)
            true
        } catch (e: IOException) {
            Snackbar.make(
                requireView(),
                "Failed to create project: ${e.message}",
                Snackbar.LENGTH_LONG
            ).show()
            false
        }
    }

    private fun navigateToEditorFragment(project: Project) {
        parentFragmentManager.commit {
            add(R.id.fragment_container, EditorFragment.newInstance(project))
            addToBackStack(null)
            setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        }
    }

    private fun File.createMainFile(language: Language, packageName: String) {
        val content = language.classFileContent(name = "Main", packageName = packageName)
        writeText(content)
    }
}
