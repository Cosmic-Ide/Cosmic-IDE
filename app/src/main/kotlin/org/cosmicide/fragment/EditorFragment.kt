/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.fragment

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
import kotlinx.coroutines.withContext
import org.cosmic.ide.dependency.resolver.api.Repository
import org.cosmic.ide.dependency.resolver.getArtifact
import org.cosmic.ide.dependency.resolver.repositories
import org.cosmicide.FileProvider.openFileWithExternalApp
import org.cosmicide.R
import org.cosmicide.adapter.EditorAdapter
import org.cosmicide.adapter.NavAdapter
import org.cosmicide.build.dex.D8Task
import org.cosmicide.common.BaseBindingFragment
import org.cosmicide.common.Prefs
import org.cosmicide.databinding.FragmentEditorBinding
import org.cosmicide.databinding.NavigationElementsBinding
import org.cosmicide.databinding.NewDependencyBinding
import org.cosmicide.databinding.TextDialogBinding
import org.cosmicide.databinding.TreeviewContextActionDialogItemBinding
import org.cosmicide.editor.IdeEditor
import org.cosmicide.editor.formatter.GoogleJavaFormat
import org.cosmicide.editor.formatter.ktfmtFormatter
import org.cosmicide.editor.language.KotlinLanguage
import org.cosmicide.model.FileViewModel
import org.cosmicide.project.Language
import org.cosmicide.project.Project
import org.cosmicide.util.CommonUtils
import org.cosmicide.util.FileFactoryProvider
import org.cosmicide.util.FileIndex
import org.cosmicide.util.ProjectHandler
import java.io.File
import java.io.OutputStream
import java.io.PrintStream

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
            isSaveEnabled = true
        }

        fileViewModel.updateFiles(fileIndex.getFiles())

        binding.tabLayout.isSmoothScrollingEnabled = false

        binding.included.refresher.apply {
            setOnRefreshListener {
                isRefreshing = true
                lifecycleScope.launch {
                    initTreeView()
                }
                isRefreshing = false
            }
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                fileViewModel.setCurrentPosition(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                editorAdapter.fragments[tab.position].let {
                    it.save()
                    it.hideWindows()
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                editorAdapter.saveAll()
                if (Prefs.doubleClickClose && fileViewModel.currentPosition.value == tab.position) {
                    fileViewModel.removeFile(tab.position)
                }
            }
        })

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    binding.apply {
                        if (drawer.isOpen) {
                            drawer.close()
                        } else {
                            editorAdapter.saveAll()
                            editorAdapter.releaseAll()

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

        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            tab.text = fileViewModel.files.value!![position].name
            tab.view.setOnLongClickListener {
                Log.d("EditorFragment", "onLongClick: $position")
                showMenu(it, R.menu.tab_menu, position)
                true
            }
        }.attach()
    }

    private fun initViewModelListeners() {
        fileViewModel.files.observe(viewLifecycleOwner, ::handleFilesUpdate)

        fileViewModel.currentPosition.observe(viewLifecycleOwner) { pos ->
            if (pos == -1) return@observe
            if (binding.drawer.isOpen) binding.drawer.close()
            val tab = binding.tabLayout.getTabAt(pos)
            if (tab != null && tab.isSelected.not()) {
                tab.select()
                binding.pager.currentItem = pos
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
                val newTab = tabLayout.newTab()
                newTab.text = file.name
                tabLayout.addTab(newTab, false)
            }
        }
    }

    override fun onDestroyView() {
        editorAdapter.saveAll()

        fileIndex.putFiles(
            binding.pager.currentItem, fileViewModel.files.value!!
        )

        fileViewModel.files.removeObservers(viewLifecycleOwner)
        fileViewModel.currentPosition.removeObservers(viewLifecycleOwner)
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
                editorAdapter.saveAll()
                binding.drawer.open()
            }

            if (Prefs.experimentsEnabled) {
                menu.findItem(R.id.action_chat).isVisible = true
            }

            setOnMenuItemClickListener { item ->
                getCurrentFragment()?.save()
                when (item.itemId) {
                    R.id.action_compile -> {
                        editorAdapter.fragments.forEach { fragment -> fragment.save() }
                        getCurrentFragment()?.hideWindows()
                        navigateToCompileInfoFragment()
                        true
                    }

                    R.id.action_settings -> {
                        getCurrentFragment()?.hideWindows()
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
                        getCurrentFragment()?.hideWindows()
                        val sheet = BottomSheetDialog(requireContext())
                        val binding = NewDependencyBinding.inflate(layoutInflater)
                        binding.apply {
                            download.setOnClickListener {
                                val newOut = object : OutputStream() {
                                    override fun write(b: Int) {
                                        lifecycleScope.launch(Dispatchers.Main) {
                                            val text = editor.text
                                            text.insert(
                                                text.lineCount - 1,
                                                text.getColumnCount(text.lineCount - 1),
                                                b.toChar().toString()
                                            )
                                        }
                                    }
                                }
                                System.setOut(PrintStream(newOut))

                                val dependency = dependency.editText?.text.toString().trim()
                                if (dependency.isNotEmpty()) {
                                    val arr = dependency.split(":")
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        repositories.apply s@{
                                            if (Prefs.repositories.isBlank()) {
                                                return@s
                                            }
                                            clear()

                                            Prefs.repositories.lines().forEach {
                                                val split = it.split(":")
                                                if (split.size != 2) return@forEach

                                                val name = split[0].trim()
                                                val url = split[1].trim()

                                                add(
                                                    object : Repository {
                                                        override fun getName() = name
                                                        override fun getURL() = url
                                                    }
                                                )
                                            }
                                        }
                                        val artifact = try {
                                            getArtifact(arr[0], arr[1], arr[2])
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            withContext(Dispatchers.Main) {
                                                binding.editor.setText(e.stackTraceToString())
                                            }
                                            return@launch
                                        }
                                        if (artifact == null) {
                                            withContext(Dispatchers.Main) {
                                                binding.editor.setText("Cannot find library")
                                            }
                                            return@launch
                                        }
                                        try {
                                            artifact.downloadArtifact(project.libDir)
                                            sheet.dismiss()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            withContext(Dispatchers.Main) {
                                                println(e.stackTraceToString())
                                            }
                                            return@launch
                                        }
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

                    R.id.arguments -> {
                        val binding = TextDialogBinding.inflate(layoutInflater)
                        MaterialAlertDialogBuilder(context).setTitle("Enter program arguments")
                            .setView(binding.root).setPositiveButton("Save") { _, _ ->
                                binding.textInputLayout.hint = "Arguments"
                                binding.textInputLayout.editText?.setText(
                                    project.args.joinToString(
                                        " "
                                    )
                                )
                                val args = binding.textInputLayout.editText?.text.toString()

                                // split args into a list considering both single and double quotes and ending with a space
                                val argList = mutableListOf<String>()
                                var arg = ""
                                var inSingleQuote = false
                                var inDoubleQuote = false
                                args.forEach { c ->
                                    when (c) {
                                        '\'' -> {
                                            arg += '\''
                                            if (inSingleQuote) {
                                                argList.add(arg)
                                                arg = ""
                                            }
                                            inSingleQuote = !inSingleQuote
                                        }

                                        '"' -> {
                                            arg += '"'
                                            if (inDoubleQuote) {
                                                argList.add(arg)
                                                arg = ""
                                            }
                                            inDoubleQuote = !inDoubleQuote
                                        }

                                        ' ' -> {
                                            if (inSingleQuote || inDoubleQuote) {
                                                arg += c
                                            } else {
                                                if (arg.isNotEmpty()) {
                                                    return@forEach
                                                }
                                                argList.add(arg)
                                                arg = ""
                                            }
                                        }

                                        else -> arg += c
                                    }
                                }
                                if (arg.isNotEmpty()) argList.add(arg)
                                project.args = argList.filterNot { it.isBlank() }

                            }.setNegativeButton("Cancel") { dialog, _ ->
                                dialog.dismiss()
                            }.show()
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
            CommonUtils.showSnackBar(binding.root, "Open a file first")
            return
        }

        val language = when (editor.file.extension) {
            "java", "jav" -> Language.Java
            "kt" -> Language.Kotlin
            else -> {
                CommonUtils.showSnackBar(binding.root, "Unsupported file format")
                return
            }
        }

        var symbols = mutableListOf<NavigationProvider.NavigationItem>()

        when (language) {
            is Language.Java -> {
                val psiJavaFile = FileFactoryProvider.getPsiJavaFile(
                    editor.file.name,
                    editor.editor.text.toString()
                )

                if (psiJavaFile.classes.isEmpty()) {
                    CommonUtils.showSnackBar(binding.root, "No navigation symbols found")
                    return
                }

                psiJavaFile.classes.forEach { psiClass ->
                    symbols.addAll(NavigationProvider.extractMethodsAndFields(psiClass))
                }
            }

            is Language.Kotlin -> {
                val ktEnv = (editor.editor.editorLanguage as KotlinLanguage).kotlinEnvironment

                val analysis = ktEnv.analysis
                if (analysis == null) {
                    CommonUtils.showSnackBar(binding.root, "File analysis not completed yet")
                    return
                }
                symbols = KtNavigationProvider.parseAnalysisContext(analysis)

                if (symbols.isEmpty()) {
                    CommonUtils.showSnackBar(binding.root, "No navigation symbols found")
                    return
                }
            }
        }
        showSymbols(symbols, editor.editor)
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
            CommonUtils.showSnackBar(binding.root, "No file selected")
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
                    withContext(Dispatchers.Main) {
                        content.replace(0, content.length, formatted)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
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

    private fun navigateToCompileInfoFragment(clazz: String? = null) {
        ProjectHandler.clazz = clazz
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
            val pos = position - 1

            when (it.itemId) {
                R.id.close_tab -> {
                    Log.d("EditorFragment", "Closing tab at $position")
                    editorAdapter.saveAll()
                    fileViewModel.removeFile(position)
                    if (pos > fileViewModel.currentPosition.value!!) {
                        fileViewModel.setCurrentPosition(pos)
                    }
                }

                R.id.close_all_tab -> fileViewModel.removeAll()
                R.id.close_left_tab -> fileViewModel.removeLeft(pos - 1)
                R.id.close_right_tab -> fileViewModel.removeRight(pos + 1)
                R.id.close_other_tab -> fileViewModel.removeOthers(fileViewModel.files.value!![pos])
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

            if (file.extension == "kt" || file.extension == "java") {
                popup.menu.findItem(R.id.execute).isVisible = true
            }
        }

        if (file.extension == "jar") {
            popup.menu.add("DEX").setOnMenuItemClickListener {
                val dex = file.resolveSibling("${file.nameWithoutExtension}.dex")
                dex.createNewFile()
                D8Task.compileJar(file.toPath(), dex.parentFile!!.toPath())
                true
            }
        }

        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.execute -> {
                    editorAdapter.fragments.forEach { fragment -> fragment.save() }
                    getCurrentFragment()?.hideWindows()
                    navigateToCompileInfoFragment(
                        file.absolutePath.replace(
                            project.srcDir.absolutePath + "/",
                            ""
                        )
                    )
                }
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
                        .setPositiveButton("Delete") { _, _ ->
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
