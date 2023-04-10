package org.cosmicide.rewrite.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import kotlinx.coroutines.launch
import org.cosmicide.project.Project
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.databinding.FragmentEditorBinding
import org.cosmicide.rewrite.editor.JavaLanguage
import org.cosmicide.rewrite.editor.KotlinLanguage
import org.cosmicide.rewrite.extension.setFont
import org.cosmicide.rewrite.extension.setLanguageTheme
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.model.FileViewModel
import org.cosmicide.rewrite.util.FileIndex
import org.cosmicide.rewrite.util.ProjectHandler
import java.io.File

class EditorFragment : BaseBindingFragment<FragmentEditorBinding>() {
    private val project: Project = ProjectHandler.getProject()
        ?: throw IllegalStateException("No project set")
    private val fileIndex: FileIndex = FileIndex(project)
    private lateinit var fileViewModel: FileViewModel

    override fun getViewBinding() = FragmentEditorBinding.inflate(layoutInflater)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureToolbar()

        lifecycleScope.launch {
            fileViewModel = ViewModelProvider(this@EditorFragment)[FileViewModel::class.java]

            fileIndex.getFiles().takeIf { it.isNotEmpty() }?.let { files ->
                view.post {
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
        binding.editor.setColorScheme(TextMateColorScheme.create(ThemeRegistry.getInstance()))
        binding.editor.setFont()
        binding.editor.invalidate()
    }

    override fun onDestroyView() {
        fileViewModel.currentPosition.value?.let { pos ->
            fileViewModel.currentFile?.takeIf { it.exists() }
                ?.writeText(binding.editor.text.toString())
            fileIndex.putFiles(pos, fileViewModel.files.value!!)
        }
        super.onDestroyView()
    }

    private fun configureToolbar() {
        binding.toolbar.apply {
            title = project.name
            setNavigationOnClickListener {
                binding.drawer.open()
            }
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_compile -> {
                        navigateToCompileInfoFragment()
                        true
                    }

                    else -> false
                }
            }
        }
    }

    private fun navigateToCompileInfoFragment() {
        parentFragmentManager.beginTransaction()
            .add(R.id.fragment_container, CompileInfoFragment())
            .addToBackStack("EditorFragment")
            .commit()
    }
}
