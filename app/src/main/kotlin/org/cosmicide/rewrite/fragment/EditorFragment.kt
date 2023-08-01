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
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.widget.treeview.OnItemClickListener
import com.widget.treeview.TreeViewAdapter
import dev.pranav.navigation.KtNavigationProvider
import dev.pranav.navigation.NavigationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cosmic.ide.dependency.resolver.getArtifact
import org.cosmicide.project.Language
import org.cosmicide.project.Project
import org.cosmicide.rewrite.FileProvider.openFileWithExternalApp
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.adapter.EditorAdapter
import org.cosmicide.rewrite.adapter.NavAdapter
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.databinding.FragmentEditorBinding
import org.cosmicide.rewrite.databinding.NavigationElementsBinding
import org.cosmicide.rewrite.databinding.NewDependencyBinding
import org.cosmicide.rewrite.databinding.TreeviewContextActionDialogItemBinding
import org.cosmicide.rewrite.editor.IdeEditor
import org.cosmicide.rewrite.editor.formatter.GoogleJavaFormat
import org.cosmicide.rewrite.editor.formatter.ktfmtFormatter
import org.cosmicide.rewrite.editor.language.KotlinLanguage
import org.cosmicide.rewrite.model.FileViewModel
import org.cosmicide.rewrite.util.FileFactoryProvider
import org.cosmicide.rewrite.util.FileIndex
import org.cosmicide.rewrite.util.ProjectHandler
import java.io.File


class EditorFragment : BaseBindingFragment<FragmentEditorBinding>() {
    private lateinit var fileIndex: FileIndex
    private val fileViewModel by activityViewModels<FileViewModel>()
    private lateinit var editorAdapter: EditorAdapter
    private val project by lazy { requireArguments().getSerializable("project") as Project }
    override var isBackHandled = true

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
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.pager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                if (binding.pager.currentItem != tab.position) {
                    binding.pager.currentItem = tab.position
                }
            }
        })

        fileViewModel.updateFiles(fileIndex.getFiles())

        binding.included.refresher.apply {
            setOnRefreshListener {
                isRefreshing = true
                lifecycleScope.launch {
                    initTreeView()
                }
                isRefreshing = false
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    binding.apply {
                        if (drawer.isOpen) {
                            drawer.close()
                        } else {
                            editorAdapter.saveAll()

                            fileIndex.putFiles(
                                binding.pager.currentItem, fileViewModel.files.value!!
                            )

                            fileViewModel.files.removeObservers(viewLifecycleOwner)
                            fileViewModel.removeAll()
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
        fileViewModel.setCurrentPosition(0)
        if (fileViewModel.files.value!!.isEmpty()) {
            binding.viewContainer.displayedChild = 1
        }
    }

    private fun initTreeView() {
        binding.included.recycler.apply {
            val nodes = TreeViewAdapter.merge(project.root)
            layoutManager = LinearLayoutManager(context)
            adapter = TreeViewAdapter(context, nodes).apply {
                setOnItemClickListener(object : OnItemClickListener {
                    override fun onItemClick(v: View, position: Int) {
                        val file = nodes[position].value
                        if (file.exists().not() || file.isDirectory) return
                        if (file.isFile) {
                            fileViewModel.addFile(file)
                        }
                    }

                    override fun onItemLongClick(v: View, position: Int) {
                        showTreeViewMenu(v, nodes[position].value)
                    }
                })
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

        fileIndex.putFiles(
            binding.pager.currentItem, fileViewModel.files.value!!
        )

        fileViewModel.files.removeObservers(viewLifecycleOwner)
        super.onDestroyView()
    }


    fun getCurrentFragment(): EditorAdapter.CodeEditorFragment? {
        val currentItemId = binding.pager.currentItem
        val fragments = editorAdapter.fragments
        if (currentItemId >= fragments.size) return null
        return fragments[currentItemId]
    }

    private fun configureToolbar() {
        binding.toolbar.apply {
            title = project.name
            setNavigationOnClickListener {
                Log.d("EditorFragment", "Saving files")
                editorAdapter.saveAll()
                binding.drawer.open()
            }
            setOnMenuItemClickListener {
                getCurrentFragment()?.save()
                when (it.itemId) {
                    R.id.action_compile -> {
                        editorAdapter.fragments.forEach { fragment -> fragment.save() }
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
                                var dependency = dependency.editText?.text.toString()
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
                        parentFragmentManager.commit {
                            add(R.id.fragment_container, ChatFragment())
                            addToBackStack(null)
                            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        }
                        true
                    }

                    R.id.action_git -> {
                        parentFragmentManager.commit {
                            add(R.id.fragment_container, GitFragment())
                            addToBackStack(null)
                            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        }
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

        val psiFile = if (language == Language.Kotlin) FileFactoryProvider.getKtPsiFile(
            editor.file.name,
            editor.editor.text.toString()
        ) else FileFactoryProvider.getPsiJavaFile(editor.file.name, editor.editor.text.toString())

        val children = psiFile.children
        Log.d("EditorFragment", "Children: $children")
        if (children.isEmpty()) {
            Snackbar.make(
                binding.root, "No navigation symbols found", Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        val ktEnv = (editor.editor.editorLanguage as KotlinLanguage).kotlinEnvironment
        val analysis = ktEnv.analysis
        if (analysis == null) {
            Snackbar.make(
                binding.root, "No navigation symbols found", Snackbar.LENGTH_SHORT
            ).show()
            return
        }
        val navItems = KtNavigationProvider.parseAnalysisContext(analysis)

        if (navItems.isEmpty()) {
            Snackbar.make(
                binding.root, "No methods or fields found", Snackbar.LENGTH_SHORT
            ).show()
            return
        }
        showSymbols(navItems, editor.editor)
    }

    private fun showSymbols(navItems: List<NavigationProvider.NavigationItem>, editor: IdeEditor) {
        val binding = NavigationElementsBinding.inflate(layoutInflater)
        binding.elementList.adapter =
            NavAdapter(requireContext(), navItems, editor.text.indexer)
        val bottomSheet = BottomSheetDialog(requireContext())
        binding.elementList.setOnItemClickListener { _, _, position, _ ->
            val item = navItems[position]
            val pos = editor.text.indexer.getCharPosition(item.startPosition)
            editor.cursor.set(pos.line, pos.column)
            bottomSheet.dismiss()
        }

        bottomSheet.apply {
            setContentView(binding.root)
            show()
        }
    }

    override fun onPause() {
        super.onPause()
        editorAdapter.saveAll()
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
                        Log.i("MainActivity", "Formatting java code")
                        GoogleJavaFormat.formatCode(text)
                    }

                    "kt" -> {
                        Log.i("MainActivity", "Formatting kotlin code")
                        ktfmtFormatter.formatCode(text)
                    }

                    else -> {
                        Log.i("MainActivity", "Unsupported language")
                        ""
                    }
                }

                if (formatted.isNotEmpty() && formatted != text) {
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
        parentFragmentManager.commit {
            add(R.id.fragment_container, CompileInfoFragment())
            addToBackStack(null)
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        }
    }

    private fun navigateToSettingsFragment() {
        parentFragmentManager.commit {
            add(R.id.fragment_container, SettingsFragment())
            addToBackStack(null)
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        }
    }

    private fun showMenu(v: View, @MenuRes menuRes: Int, position: Int) {
        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        popup.setOnMenuItemClickListener {
            popup.dismiss()
            when (it.itemId) {
                R.id.close_tab -> {
                    fileViewModel.removeFile(position)
                }

                R.id.close_all_tab -> fileViewModel.removeAll()
                R.id.close_left_tab -> fileViewModel.removeLeft(position - 1)
                R.id.close_right_tab -> fileViewModel.removeRight(position - 1)
                R.id.close_other_tab -> fileViewModel.removeOthers(fileViewModel.currentFile!!)
            }
            true
        }
        popup.show()
    }

    private fun showTreeViewMenu(v: View, file: File) {
        val popup = PopupMenu(v.context, v)
        popup.menuInflater.inflate(R.menu.treeview_menu, popup.menu)

        if (file.isDirectory) {
            popup.menu.removeItem(R.id.open_external)
        } else {
            popup.menu.removeItem(R.id.create_kotlin_class)
            popup.menu.removeItem(R.id.create_java_class)
            popup.menu.removeItem(R.id.create_folder)
        }

        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.create_kotlin_class -> {
                    val binding = TreeviewContextActionDialogItemBinding.inflate(layoutInflater)
                    binding.textInputLayout.suffixText = ".kt"
                    MaterialAlertDialogBuilder(v.context).setTitle("Create kotlin class")
                        .setView(binding.root).setPositiveButton("Create") { _, _ ->
                            var name = binding.textInputLayout.editText?.text.toString()
                            name = name.replace("\\.", "")
                            file.resolve("$name.kt").createNewFile()
                            initTreeView()
                        }.setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }.show()
                }

                R.id.create_java_class -> {
                    val binding = TreeviewContextActionDialogItemBinding.inflate(layoutInflater)
                    binding.textInputLayout.suffixText = ".java"
                    MaterialAlertDialogBuilder(v.context).setTitle("Create java class")
                        .setView(binding.root).setPositiveButton("Create") { _, _ ->
                            var name = binding.textInputLayout.editText?.text.toString()
                            name = name.replace("\\.", "")
                            file.resolve("$name.java").createNewFile()
                            initTreeView()
                        }.setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }.show()
                }

                R.id.create_folder -> {
                    val binding = TreeviewContextActionDialogItemBinding.inflate(layoutInflater)
                    MaterialAlertDialogBuilder(v.context).setTitle("Create folder")
                        .setView(binding.root).setPositiveButton("Create") { _, _ ->
                            var name = binding.textInputLayout.editText?.text.toString()
                            name = name.replace("\\.", "")
                            file.resolve(name).mkdirs()
                            initTreeView()
                        }.setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }.show()
                }

                R.id.create_file -> {
                    val binding = TreeviewContextActionDialogItemBinding.inflate(layoutInflater)
                    MaterialAlertDialogBuilder(v.context).setTitle("Create file")
                        .setView(binding.root).setPositiveButton("Create") { _, _ ->
                            var name = binding.textInputLayout.editText?.text.toString()
                            name = name.replace("\\.", "")
                            file.resolve(name).createNewFile()
                            initTreeView()
                        }.setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }.show()
                }

                R.id.rename -> {
                    val binding = TreeviewContextActionDialogItemBinding.inflate(layoutInflater)
                    binding.textInputLayout.editText?.setText(file.name)
                    MaterialAlertDialogBuilder(v.context).setTitle("Rename").setView(binding.root)
                        .setPositiveButton("Create") { _, _ ->
                            var name = binding.textInputLayout.editText?.text.toString()
                            name = name.replace("\\.", "")
                            file.renameTo(file.parentFile!!.resolve(name))
                            initTreeView()
                        }.setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }.show()
                }

                R.id.delete -> {
                    MaterialAlertDialogBuilder(v.context).setTitle("Delete")
                        .setMessage("Are you sure you want to delete this file")
                        .setPositiveButton("Create") { _, _ ->
                            file.deleteRecursively()
                            initTreeView()
                        }.setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }.show()
                }

                R.id.open_external -> {
                    openFileWithExternalApp(v.context, file)
                }
            }
            true
        }
        popup.show()
    }

    companion object {
        @JvmStatic
        fun newInstance(project: Project) = EditorFragment().apply {
            arguments = Bundle().apply {
                putSerializable("project", project)
            }
        }
    }
}
