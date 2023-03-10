package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.cosmicide.project.Java
import org.cosmicide.project.Kotlin
import org.cosmicide.project.Project
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.databinding.FragmentNewProjectBinding
import org.cosmicide.rewrite.util.Constants
import org.cosmicide.rewrite.util.FileUtil
import java.io.File

class NewProjectFragment : Fragment() {

    private var _binding: FragmentNewProjectBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentNewProjectBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCreate.setOnClickListener {
            if (binding.projectName.text!!.isEmpty()) {
                binding.projectName.error = "Project name cannot be empty"
                return@setOnClickListener
            }
            if (binding.packageName.text!!.isEmpty()) {
                binding.packageName.error = "Package name cannot be empty"
                return@setOnClickListener
            }
            val language = if (binding.useKotlin.isChecked) Kotlin else Java
            val root = File(FileUtil.projectDir, binding.projectName.text.toString())
            root.mkdirs()
            val project = Project(root, language)
            val srcDir = project.srcDir.invoke()
            srcDir.mkdirs()
            File(srcDir, "Main.${language.extension}").writeText(
                language.getClassFile(
                    "Main",
                    binding.packageName.text.toString()
                )
            )
            findNavController().navigate(R.id.NewProjectFragment_to_EditorFragment, Bundle().apply {
                putString(Constants.PROJECT_DIR, project.root.absolutePath)
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}