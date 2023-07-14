/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dev.pranav.navigation.KtNavigationProvider
import dev.pranav.navigation.NavigationProvider
import io.github.dingyi222666.view.treeview.Tree
import io.github.dingyi222666.view.treeview.TreeView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cosmic.ide.dependency.resolver.getArtifact
import org.cosmicide.project.Language
import org.cosmicide.project.Project
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.adapter.EditorAdapter
import org.cosmicide.rewrite.adapter.NavAdapter
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.databinding.FragmentEditorBinding
import org.cosmicide.rewrite.databinding.NavigationElementsBinding
import org.cosmicide.rewrite.databinding.NewDependencyBinding
import org.cosmicide.rewrite.editor.formatter.GoogleJavaFormat
import org.cosmicide.rewrite.editor.formatter.ktfmtFormatter
import org.cosmicide.rewrite.model.FileViewModel
import org.cosmicide.rewrite.treeview.FileSet
import org.cosmicide.rewrite.treeview.FileTreeNodeGenerator
import org.cosmicide.rewrite.treeview.ViewBinder
import org.cosmicide.rewrite.util.FileFactoryProvider
import org.cosmicide.rewrite.util.FileIndex
import org.cosmicide.rewrite.util.ProjectHandler
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

class EditorFragment(
    private val project: Project
) : BaseBindingFragment<FragmentEditorBinding>() {
    private lateinit var fileIndex: FileIndex
    private val fileViewModel by activityViewModels<FileViewModel>()
    private lateinit var editorAdapter: EditorAdapter

    override fun getViewBinding() = FragmentEditorBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileIndex = FileIndex(project)
        ProjectHandler.setProject(project)

        configureToolbar()
        initViewModelListeners()
        initTreeView()

        binding.pager.apply {
            editorAdapter = EditorAdapter(this@EditorFragment, fileViewModel)
            adapter = editorAdapter
            isUserInputEnabled = false
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.pager.currentItem = tab!!.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                binding.pager.currentItem = tab!!.position
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                binding.pager.currentItem = tab?.position ?: 0
            }
        })

        binding.included.refresher.apply {
            setOnRefreshListener {
                isRefreshing = true
                lifecycleScope.launch {
                    binding.included.treeview.tree.apply {
                        generator = FileTreeNodeGenerator(
                            FileSet(
                                project.root, traverseDirectory(project.root) as MutableSet<FileSet>
                            )
                        )
                        initTree()
                    }
                }
                isRefreshing = false
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    binding.apply {
                        val fragment =
                            parentFragmentManager.findFragmentById(R.id.fragment_container)
                        if (fragment !is EditorFragment) {
                            parentFragmentManager.popBackStack()
                            return
                        }
                        if (drawer.isOpen) {
                            drawer.close()
                        } else {
                            editorAdapter.saveAll()

                            fileIndex.putFiles(
                                binding.pager.currentItem,
                                fileViewModel.files.value!!
                            )

                            fileViewModel.removeAll()
                            fileViewModel.files.removeObservers(viewLifecycleOwner)

                            parentFragmentManager.popBackStack()
                        }
                    }
                }
            })

        TabLayoutMediator(binding.tabLayout, binding.pager, true, true) { tab, position ->
            tab.text = fileViewModel.files.value!![position].name
        }.attach()
    }

    private fun initViewModelListeners() {
        val indexedFiles = fileIndex.getFiles()

        fileViewModel.files.observe(viewLifecycleOwner) { files ->
            handleFilesUpdate(files)
        }

        fileViewModel.currentPosition.observe(viewLifecycleOwner) { pos ->
            if (pos == -1) return@observe
            if (binding.drawer.isOpen) binding.drawer.close()
            if (binding.tabLayout.selectedTabPosition != pos) {
                binding.tabLayout.getTabAt(pos)?.select()
            }
        }

        indexedFiles.forEach { file ->
            fileViewModel.addFile(file)
        }
        fileViewModel.setCurrentPosition(0)
        if (fileViewModel.files.value!!.isEmpty()) {
            binding.viewContainer.displayedChild = 1
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun initTreeView() {
        val treeview = binding.included.treeview as TreeView<FileSet>
        val binder = ViewBinder(
            lifecycleScope,
            layoutInflater,
            fileViewModel,
            treeview)

        lifecycleScope.launch(Dispatchers.IO) {
            val rootItem = FileSet(
                project.root, traverseDirectory(project.root) as MutableSet<FileSet>
            )

            val generator = FileTreeNodeGenerator(rootItem)

            val tree = Tree.createTree<FileSet>().apply {
                this.generator = generator
                initTree()
            }

            lifecycleScope.launch(Dispatchers.Main) {
                binding.included.treeview.apply {
                    bindCoroutineScope(lifecycleScope)
                    this.tree = tree
                    this.binder = binder
                    nodeEventListener = binder
                    lifecycleScope.launch { refresh() }
                }
            }
        }
    }

    private fun handleFilesUpdate(files: List<File>) {
        binding.apply {
            if (files.isEmpty()) {
                tabLayout.visibility = View.GONE
                viewContainer.displayedChild = 1
            } else {
                tabLayout.visibility = View.VISIBLE
                viewContainer.displayedChild = 0
            }
            tabLayout.removeAllTabs()
            files.forEach { file ->
                val tab = tabLayout.newTab().setText(file.name)
                tab.view.apply {
                    setOnLongClickListener {
                        showMenu(it, R.menu.tab_menu, tab.position)
                        true
                    }
                    setOnClickListener {
                        tab.select()
                    }
                }
                tabLayout.addTab(tab, false)
            }
        }
    }

    override fun onDestroyView() {
        editorAdapter.saveAll()
        super.onDestroyView()
    }


    fun getCurrentFragment(): EditorAdapter.CodeEditorFragment? {
        return editorAdapter.getItem(binding.pager.currentItem)
    }

    private fun configureToolbar() {
        binding.toolbar.apply {
            title = project.name
            setNavigationOnClickListener {
                Log.d("EditorFragment", "Saving files")
                Log.d("EditorFragment", "Adapter count: ${editorAdapter.itemCount}")
                Log.d("EditorFragment", "Tab count: ${binding.tabLayout.tabCount}")
                editorAdapter.saveAll()
                binding.drawer.open()
            }
            setOnMenuItemClickListener {
                getCurrentFragment()?.save()
                when (it.itemId) {
                    R.id.action_compile -> {
                        navigateToCompileInfoFragment()
                        true
                    }

                    R.id.action_settings -> {
                        navigateToSettingsFragment()
                        true
                    }

                    R.id.undo -> {
                        getCurrentFragment()?.editor?.undo()
                        true
                    }

                    R.id.redo -> {
                        getCurrentFragment()?.editor?.redo()
                        true
                    }

                    R.id.action_format -> {
                        if (editorAdapter.itemCount == 0) return@setOnMenuItemClickListener true
                        formatCodeAsync()
                        true
                    }

                    R.id.dependency_manager -> {
                        val sheet = BottomSheetDialog(requireContext())
                        val binding = NewDependencyBinding.inflate(layoutInflater)
                        binding.apply {
                            download.setOnClickListener {
                                var dependency = dependency.text.toString()
                                if (dependency.isNotEmpty()) {
                                    dependency =
                                        dependency.replace("implementation", "").replace("'", "")
                                            .replace("\"", "")
                                    dependency = dependency.trim()
                                    val arr = dependency.split(":")
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val artifact = try {
                                            getArtifact(arr[0], arr[1], arr[2])
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            lifecycleScope.launch(Dispatchers.Main) {
                                                binding.editor.setText(e.stackTraceToString())
                                            }
                                            return@launch
                                        }
                                        artifact.downloadArtifact(project.libDir)
                                        sheet.dismiss()
                                    }
                                }
                            }
                            editor.editable = false
                        }
                        sheet.setContentView(binding.root)
                        sheet.show()

                        true
                    }

                    R.id.action_chat -> {
                        parentFragmentManager.beginTransaction().apply {
                            add(R.id.fragment_container, ChatFragment())
                            addToBackStack(null)
                            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        }.commit()
                        true
                    }

                    R.id.action_git -> {
                        parentFragmentManager.beginTransaction().apply {
                            add(R.id.fragment_container, GitFragment())
                            addToBackStack(null)
                            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        }.commit()
                        true
                    }

                    R.id.nav_items -> {
                        showNavigationElements()
                        true
                    }

                    else -> false
                }
            }
        }
    }

    private fun showNavigationElements() {
        val editor = getCurrentFragment()
        if (editor == null) {
            Snackbar.make(
                binding.root, "Open a file first", Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        val language = when (editor.file.extension) {
            "java" -> Language.Java
            "kt" -> Language.Kotlin
            else -> {
                Snackbar.make(
                    binding.root, "Unsupported language", Snackbar.LENGTH_SHORT
                ).show()
                return
            }
        }

        val psiFile =
            if (language == Language.Kotlin)
                FileFactoryProvider.getKtPsiFile(editor.file.name, editor.editor.text.toString())
            else
                FileFactoryProvider.getPsiJavaFile(editor.file.name, editor.editor.text.toString())

        val classes = psiFile.classes
        if (classes.isEmpty() && language == Language.Java) {
            Snackbar.make(
                binding.root, "No classes found", Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        val navItems = when (language) {
            is Language.Java -> NavigationProvider.extractMethodsAndFields(classes[0])
            is Language.Kotlin -> KtNavigationProvider.parseKtFile(psiFile as KtFile)
        }

        if (navItems.isEmpty()) {
            Snackbar.make(
                binding.root, "No methods or fields found", Snackbar.LENGTH_SHORT
            ).show()
            return
        }
        val binding = NavigationElementsBinding.inflate(layoutInflater)
        binding.elementList.adapter =
            NavAdapter(requireContext(), navItems, editor.editor.text.indexer)
        val bottomSheet = BottomSheetDialog(requireContext())
        binding.elementList.setOnItemClickListener { _, _, position, _ ->
            val item = navItems[position]
            val pos = editor.editor.text.indexer.getCharPosition(item.startPosition)
            editor.editor.cursor.set(pos.line, pos.column)
            bottomSheet.dismiss()
        }

        bottomSheet.apply {
            setContentView(binding.root)
            show()
        }
    }

    private fun formatCodeAsync() {
        val fragment = getCurrentFragment()
        if (fragment == null) {
            Snackbar.make(
                binding.root, "No file selected", Snackbar.LENGTH_SHORT
            ).show()
            return
        }
        val content = fragment.editor.text
        val text = content.toString()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val formatted = when (fileViewModel.currentFile?.extension) {
                    "java" -> {
                        GoogleJavaFormat.formatCode(text)
                    }

                    "kt" -> {
                        ktfmtFormatter.formatCode(text)
                    }

                    else -> {
                        ""
                    }
                }

                if (formatted.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        content.replace(0, content.length, formatted)
                    }
                }
            } catch (e: Exception) {
                lifecycleScope.launch(Dispatchers.Main) {
                    Snackbar.make(
                        binding.root, "Failed to format code", Snackbar.LENGTH_SHORT
                    ).apply {
                        setAction("View Error") {
                            val sheet = BottomSheetDialog(requireContext())
                            sheet.setContentView(TextView(requireContext()).apply {
                                setText(e.stackTraceToString())
                            })
                            sheet.show()
                        }
                        show()
                    }
                }
                return@launch
            }
        }
    }

    private fun navigateToCompileInfoFragment() {
        editorAdapter.saveAll()
        parentFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, CompileInfoFragment())
            addToBackStack(null)
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        }.commit()
    }

    private fun navigateToSettingsFragment() {
        parentFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, SettingsFragment())
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
        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.close_tab -> {
                    fileViewModel.removeFile(position)
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
