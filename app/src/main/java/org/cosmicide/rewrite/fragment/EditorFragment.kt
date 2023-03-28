package org.cosmicide.rewrite.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cosmicide.project.Project
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.databinding.FragmentEditorBinding
import org.cosmicide.rewrite.editor.JavaLanguage
import org.cosmicide.rewrite.editor.KotlinLanguage
import org.cosmicide.rewrite.extension.setFont
import org.cosmicide.rewrite.extension.setLanguageTheme
import org.cosmicide.rewrite.model.FileViewModel
import org.cosmicide.rewrite.util.Constants
import org.cosmicide.rewrite.util.FileIndex
import org.cosmicide.rewrite.util.ProjectHandler
import java.io.File

class EditorFragment : Fragment() {

    private lateinit var project: Project
    private lateinit var fileIndex: FileIndex
    private lateinit var binding: FragmentEditorBinding
    private lateinit var fileViewModel: FileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        project = ProjectHandler.getProject() ?: throw IllegalStateException("No project set")
        fileIndex = FileIndex(project)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditorBinding.inflate(inflater, container, false)

        lifecycleScope.launch {
            fileViewModel = ViewModelProvider(this@EditorFragment)[FileViewModel::class.java]

            fileIndex.getFiles().takeIf { it.isNotEmpty() }?.let { files ->
                requireActivity().runOnUiThread {
                    fileViewModel.updateFiles(files.toMutableList())
                    files.forEach { file ->
                        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(file.name))
                    }
                }
            }

            binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    fileViewModel.setCurrentPosition(tab.position)
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    fileViewModel.removeFile(fileViewModel.files.value!![tab.position])
                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                    binding.editor.setText(fileViewModel.files.value!![tab.position].readText())
                }
            })

            fileViewModel.files.observe(viewLifecycleOwner) { files ->
                binding.tabLayout.removeAllTabs()
                files.forEach { file ->
                    binding.tabLayout.addTab(binding.tabLayout.newTab().setText(file.name))
                }
            }

            fileViewModel.currentPosition.observe(viewLifecycleOwner) { position ->
                position?.takeIf { it != -1 }?.let {
                    binding.editor.setText(fileViewModel.currentFile?.readText())
                    setEditorLanguage()
                }
            }

            fileViewModel.addFile(File(project.srcDir.invoke(),"Main.${project.language.extension}"))
        }

        binding.editor.setTextSize(20f)

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        ProjectHandler.onEditorFragmentChange(true)
    }

    override fun onResume() {
        super.onResume()
        ProjectHandler.onEditorFragmentChange(true)
    }

    private fun setEditorLanguage() {
        val file = fileViewModel.currentFile
        binding.editor.setEditorLanguage(
            when (file?.extension) {
                "kt" -> KotlinLanguage(binding.editor, project, file)
                "java" -> JavaLanguage(binding.editor, project, file)
                else -> EmptyLanguage()
            }
        )
        binding.editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
        binding.editor.setFont()
    }

    override fun onStop() {
        super.onStop()
        ProjectHandler.onEditorFragmentChange(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        fileViewModel.currentPosition.value?.let { pos ->
            fileViewModel.currentFile?.takeIf { it.exists() }?.writeText(binding.editor.text.toString())
            fileIndex.putFiles(pos, fileViewModel.files.value!!)
        }
        if (arguments?.getBoolean(Constants.NEW_PROJECT, false) == true) {
            findNavController().popBackStack(R.id.ProjectFragment, false)
        }
        ProjectHandler.onEditorFragmentChange(false)
    }
}