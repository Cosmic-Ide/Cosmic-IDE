package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.annotation.MenuRes
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import io.github.dingyi222666.view.treeview.Branch
import io.github.dingyi222666.view.treeview.DataSource
import io.github.dingyi222666.view.treeview.DataSourceScope
import io.github.dingyi222666.view.treeview.Leaf
import io.github.dingyi222666.view.treeview.Tree
import io.github.dingyi222666.view.treeview.TreeView
import io.github.dingyi222666.view.treeview.buildTree
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import kotlinx.coroutines.launch
import org.cosmicide.project.Project
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.databinding.FragmentEditorBinding
import org.cosmicide.rewrite.editor.JavaLanguage
import org.cosmicide.rewrite.editor.KotlinLanguage
import org.cosmicide.rewrite.extension.setFont
import org.cosmicide.rewrite.model.FileViewModel
import org.cosmicide.rewrite.treeview.ViewBinder
import org.cosmicide.rewrite.util.FileIndex
import org.cosmicide.rewrite.util.Prefs
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

            val tree = createTree()
            @Suppress("UNCHECKED_CAST")
            (binding.included.treeview as TreeView<DataSource<File>>).apply {
                bindCoroutineScope(lifecycleScope)
                this.tree = tree
                binder = ViewBinder(layoutInflater, fileViewModel)
                nodeEventListener = binder
            }

            binding.included.treeview.refresh()

            binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    fileViewModel.setCurrentPosition(tab.position)
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {

                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                    binding.editor.setText(fileViewModel.files.value!![tab.position].readText())
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
                    binding.tabLayout.addTab(tab)
                }
            }

            fileViewModel.currentPosition.observe(viewLifecycleOwner) { position ->
                position?.takeIf { it != -1 }?.let {
                    binding.tabLayout.selectTab(binding.tabLayout.getTabAt(position), true)
                    binding.editor.setText(fileViewModel.currentFile?.readText())
                    setEditorLanguage()
                }
            }

            fileIndex.getFiles().takeIf { it.isNotEmpty() }?.let { files ->
                view.post {
                    fileViewModel.updateFiles(files.toMutableList())
                }
            }
            if (fileViewModel.files.value!!.isEmpty()) {
                fileViewModel.addFile(
                    File(
                        project.srcDir.invoke(),
                        "Main.${project.language.extension}"
                    )
                )
            }
        }

        binding.editor.setTextSize(Prefs.editorFontSize)
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
        parentFragmentManager.beginTransaction()
            .add(R.id.fragment_container, CompileInfoFragment())
            .addToBackStack("EditorFragment")
            .commit()
    }

    private fun navigateToSettingsFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SettingsFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun createTree(): Tree<DataSource<File>> {
        return buildTree {
            val rootDir = project.root
            Branch(rootDir.name, data = rootDir) {
                transverseTree(rootDir, this)
            }
        }
    }

    private fun transverseTree(dir: File, parentBranch: DataSourceScope<File>) {
        val files = dir.listFiles()
        for (file in files!!) {
            when {
                file.isFile -> parentBranch.Leaf(file.name, file)
                file.isDirectory -> {
                    parentBranch.Branch(file.name, file) {
                        transverseTree(file, this)
                    }
                }
            }
        }
    }

    private fun showMenu(v: View, @MenuRes menuRes: Int, position: Int) {
        val popup = PopupMenu(context, v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.close_tab -> fileViewModel.removeFile(fileViewModel.files.value!![position])
                R.id.close_all_tab -> fileViewModel.removeAll()
                R.id.close_left_tab -> {
                    fileViewModel.removeLeft(position)
                }

                R.id.close_right_tab -> fileViewModel.removeRight(position)
                R.id.close_other_tab -> fileViewModel.removeOthers(fileViewModel.currentFile!!)
            }
            // Respond to menu item click.
            true
        }
        popup.setOnDismissListener {
            // Respond to popup being dismissed.
        }
        // Show the popup menu.
        popup.show()
    }

}
