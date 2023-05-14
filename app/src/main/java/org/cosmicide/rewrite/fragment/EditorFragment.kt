package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.annotation.MenuRes
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import io.github.dingyi222666.view.treeview.Tree
import io.github.dingyi222666.view.treeview.TreeView
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import kotlinx.coroutines.launch
import org.cosmicide.build.Javap
import org.cosmicide.editor.analyzers.EditorDiagnosticsMarker
import org.cosmicide.project.Project
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.common.Prefs
import org.cosmicide.rewrite.databinding.FragmentEditorBinding
import org.cosmicide.rewrite.editor.language.JavaLanguage
import org.cosmicide.rewrite.editor.language.KotlinLanguage
import org.cosmicide.rewrite.extension.setCompletionLayout
import org.cosmicide.rewrite.extension.setFont
import org.cosmicide.rewrite.model.FileViewModel
import org.cosmicide.rewrite.treeview.FileSet
import org.cosmicide.rewrite.treeview.FileTreeNodeGenerator
import org.cosmicide.rewrite.treeview.ViewBinder
import org.cosmicide.rewrite.util.FileIndex
import org.cosmicide.rewrite.util.ProjectHandler
import java.io.File

class EditorFragment : BaseBindingFragment<FragmentEditorBinding>() {
    private val project: Project = ProjectHandler.getProject()
        ?: throw IllegalStateException("No project set")
    private val fileIndex: FileIndex = FileIndex(project)
    private lateinit var fileViewModel: FileViewModel

    override fun getViewBinding() = FragmentEditorBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureToolbar()

        lifecycleScope.launch {
            fileViewModel = ViewModelProvider(this@EditorFragment)[FileViewModel::class.java]
            val binder = ViewBinder(
                lifecycleScope,
                layoutInflater,
                fileViewModel,
                binding.included.treeview as TreeView<FileSet>
            )

            val rootItem = FileSet(project.root,
                traverseDirectory(project.root) as MutableSet<FileSet>
            )

            val generator = FileTreeNodeGenerator(rootItem)

            val tree = Tree.createTree<FileSet>().apply {
                this.generator = generator
                initTree()
            }

            (binding.included.treeview as TreeView<FileSet>).apply {
                bindCoroutineScope(lifecycleScope)
                this.tree = tree
                this.binder = binder
                nodeEventListener = binder
                refresh()
            }

            binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    fileViewModel.setCurrentPosition(tab.position)
                    val file = fileViewModel.currentFile
                    if (file?.extension == "class") {
                        binding.editor.setText(Javap.disassemble(file.absolutePath))
                    } else {
                        binding.editor.setText(fileViewModel.currentFile?.readText())
                    }
                    setEditorLanguage()
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    fileViewModel.currentFile?.writeText(binding.editor.text.toString())
                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                   fileViewModel.setCurrentPosition(tab.position)
                    val file = fileViewModel.currentFile
                    if (file?.extension == "class") {
                        binding.editor.setText(Javap.disassemble(file.absolutePath))
                    } else {
                        binding.editor.setText(fileViewModel.currentFile?.readText())
                    }
                    setEditorLanguage()
                }
            })


            fileViewModel.files.observe(viewLifecycleOwner) { files ->
                binding.tabLayout.removeAllTabs()
                files.forEach { file ->
                    val tab = binding.tabLayout.newTab().setText(file.name)
                    tab.view.setOnLongClickListener {
                        showMenu(it, R.menu.tab_menu, tab.position)
                        true
                    }
                    binding.tabLayout.apply {
                        addTab(tab)
                    }
                }
            }

            fileViewModel.currentPosition.observe(viewLifecycleOwner) { position ->
                position?.takeIf { it != -1 }?.let {
                    if (binding.drawer.isOpen) {
                        binding.drawer.close()
                    }
                    binding.tabLayout.selectTab(binding.tabLayout.getTabAt(position), true)
                }
            }

            fileIndex.getFiles().takeIf { it.isNotEmpty() }?.let { files ->
                view.post {
                    fileViewModel.updateFiles(files.toMutableList())
                }
            }
        }
    }

    private fun setEditorLanguage() {
        val file = fileViewModel.currentFile
        binding.editor.setEditorLanguage(
            when (file?.extension) {
                "kt" -> {
                    KotlinLanguage(binding.editor, project, file)
                }

                "java" -> {
                    binding.editor.text.addContentListener(
                        EditorDiagnosticsMarker(
                            binding.editor,
                            file,
                            project
                        )
                    )
                    JavaLanguage(binding.editor, project, file)
                }

                "class" -> {

                    TextMateLanguage.create("source.java", true)
                }

                else -> EmptyLanguage()
            }
        )
        binding.editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
        binding.editor.setFont()
    }

    override fun onDestroyView() {
        saveFile()
        fileViewModel.currentPosition.value?.let { pos ->
            fileIndex.putFiles(pos, fileViewModel.files.value!!)
        }
        super.onDestroyView()
    }
    
    private fun saveFile() {
        fileViewModel.currentPosition.value?.let { pos ->
            fileViewModel.currentFile?.takeIf { it.exists() }
                ?.writeText(binding.editor.text.toString())
        }
    }

    private fun configureToolbar() {
        binding.toolbar.apply {
            title = project.name
            setNavigationOnClickListener {
                binding.drawer.open()
            }
            setOnMenuItemClickListener {
                saveFile()
                when (it.itemId) {
                    R.id.action_compile -> {
                        navigateToCompileInfoFragment()
                        true
                    }
                    R.id.action_settings -> {
                        navigateToSettingsFragment()
                        true
                    }

                    else -> false
                }
            }
        }
    }

    private fun navigateToCompileInfoFragment() {
        parentFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, CompileInfoFragment())
            addToBackStack(null)
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        }.commit()
    }

    private fun navigateToSettingsFragment() {
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, SettingsFragment())
            addToBackStack(null)
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        }.commit()
    }

    private fun traverseDirectory(dir: File): Set<FileSet> {
        val set = mutableSetOf<FileSet>()
        val files = dir.listFiles() ?: return set
        for (file in files) {
            when {
                file.isFile -> set.add(FileSet(file))
                file.isDirectory -> {
                    val tempSet = mutableSetOf<FileSet>().apply {
                        addAll(traverseDirectory(file))
                    }
                    set.add(FileSet(file, tempSet))
                }
            }
        }
        return set
    }

    private fun showMenu(v: View, @MenuRes menuRes: Int, position: Int) {
        val popup = PopupMenu(context, v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.close_tab -> {
                    fileViewModel.removeFile(fileViewModel.files.value!![position])
                    binding.editor.setText("")
                }

                R.id.close_all_tab -> fileViewModel.removeAll()
                R.id.close_left_tab -> fileViewModel.removeLeft(position)
                R.id.close_right_tab -> fileViewModel.removeRight(position)
                R.id.close_other_tab -> fileViewModel.removeOthers(fileViewModel.currentFile!!)
            }
            true
        }
        popup.show()
    }
}
