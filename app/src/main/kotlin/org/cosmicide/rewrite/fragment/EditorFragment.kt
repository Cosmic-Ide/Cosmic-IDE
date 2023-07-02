/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import io.github.dingyi222666.view.treeview.Tree
import io.github.dingyi222666.view.treeview.TreeView
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cosmic.ide.dependency.resolver.getArtifact
import org.cosmicide.build.Javap
import org.cosmicide.editor.analyzers.EditorDiagnosticsMarker
import org.cosmicide.project.Project
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.databinding.FragmentEditorBinding
import org.cosmicide.rewrite.databinding.NewDependencyBinding
import org.cosmicide.rewrite.editor.formatter.GoogleJavaFormat
import org.cosmicide.rewrite.editor.formatter.ktfmtFormatter
import org.cosmicide.rewrite.editor.language.JavaLanguage
import org.cosmicide.rewrite.editor.language.KotlinLanguage
import org.cosmicide.rewrite.extension.setFont
import org.cosmicide.rewrite.model.FileViewModel
import org.cosmicide.rewrite.treeview.FileSet
import org.cosmicide.rewrite.treeview.FileTreeNodeGenerator
import org.cosmicide.rewrite.treeview.ViewBinder
import org.cosmicide.rewrite.util.FileIndex
import org.cosmicide.rewrite.util.ProjectHandler
import java.io.File


class EditorFragment : BaseBindingFragment<FragmentEditorBinding>() {
    private lateinit var project: Project
    private val fileIndex by lazy { FileIndex(project) }
    private val fileViewModel by activityViewModels<FileViewModel>()

    override fun getViewBinding() = FragmentEditorBinding.inflate(layoutInflater)

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setColorScheme()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (ProjectHandler.getProject() == null) {
            Snackbar.make(
                binding.root,
                "No project selected, please select a project first",
                Snackbar.LENGTH_LONG
            ).show()
            return
        }
        project = ProjectHandler.getProject()!!
        configureToolbar()
        binding.editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())

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

        initViewModelListeners()
        initTreeView()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            private var isClicked = false
            override fun onTabSelected(tab: TabLayout.Tab) {
                val file = fileViewModel.files.value?.get(tab.position) ?: return
                val isBinary = file.extension == "class"
                if (file.exists().not()) {
                    Snackbar.make(
                        binding.root,
                        "File does not exist, please close this tab",
                        Snackbar.LENGTH_LONG
                    ).show()
                }

                if (fileViewModel.currentPosition.value != tab.position) {
                    fileViewModel.setCurrentPosition(tab.position)
                }

                if (isBinary) {
                    binding.editor.setText(Javap.disassemble(file.absolutePath))
                    return
                }
                binding.editor.setText(fileViewModel.currentFile?.readText())
                setEditorLanguage()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                fileViewModel.currentFile?.writeText(binding.editor.text.toString())
                binding.editor.setText("")
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                if (isClicked) {
                    fileViewModel.removeFile(fileViewModel.files.value!![tab.position])
                    binding.tabLayout.removeTabAt(tab.position)
                    binding.editor.setText("")
                    return
                }
                isClicked = true
                onTabSelected(tab)
                binding.root.postDelayed({ isClicked = false }, 10000) // doesnt work
            }
        })


        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.drawer.isOpen) {
                        binding.drawer.close()
                    } else {
                        parentFragmentManager.popBackStack()
                        onHidden()
                    }
                }
            })
    }

    private fun initViewModelListeners() {
        fileViewModel.files.observe(viewLifecycleOwner) { files ->
            binding.tabLayout.removeAllTabs()
            val appearanceModel = ShapeAppearanceModel.builder().setAllCornerSizes(48f).build()
            files.forEach { file ->
                val tab = binding.tabLayout.newTab().setText(file.name)
                tab.customView = Chip(requireContext()).apply {
                    text = file.name
                    chipStrokeWidth = 0f
                    shapeAppearanceModel = appearanceModel
                }
                (tab.customView as View).apply {
                    setOnLongClickListener {
                        showMenu(it, R.menu.tab_menu, tab.position)
                        true
                    }
                    setOnClickListener {
                        if (binding.tabLayout.selectedTabPosition != tab.position) tab.select()
                    }
                }
                binding.tabLayout.addTab(tab, false)
            }
        }

        fileViewModel.currentPosition.observe(viewLifecycleOwner) { pos ->
            if (pos == -1) return@observe
            if (binding.drawer.isOpen) binding.drawer.close()
            if (binding.tabLayout.selectedTabPosition != pos) {
                binding.tabLayout.getTabAt(pos)?.select()
            }
        }
        val files = fileIndex.getFiles()
        fileViewModel.updateFiles(files.toMutableList())
        if (fileViewModel.files.value!!.isEmpty()) {
            binding.editor.setText("No files are opened in the editor. Please open a file through the file manager.")
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

    private fun setEditorLanguage() {
        val file = fileViewModel.currentFile

        when (file!!.extension) {
            "java" -> {
                if (binding.editor.editorLanguage is JavaLanguage) return
                binding.editor.setEditorLanguage(JavaLanguage(binding.editor, project, file))
                binding.editor.text.addContentListener(EditorDiagnosticsMarker.INSTANCE)
                EditorDiagnosticsMarker.INSTANCE.init(binding.editor, file, project)
            }

            "kt" -> {
                if (binding.editor.editorLanguage is KotlinLanguage) return
                binding.editor.setEditorLanguage(KotlinLanguage(binding.editor, project, file))
            }

            "class" -> {
                binding.editor.setEditorLanguage(TextMateLanguage.create("source.class", true))
            }

            else -> {
                binding.editor.setEditorLanguage(EmptyLanguage())
            }
        }

        if (file.extension != "java") {
            binding.editor.text.removeContentListener(EditorDiagnosticsMarker.INSTANCE)
        }

        binding.editor.setFont()
    }

    private fun setColorScheme() {
        binding.editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
    }


    override fun onDestroyView() {
        saveFile()
        fileViewModel.currentPosition.value?.let { pos ->
            fileIndex.putFiles(pos, fileViewModel.files.value!!)
        }
        super.onDestroyView()
    }

    fun onHidden() {
        saveFile()
        fileViewModel.currentPosition.value?.let { pos ->
            fileIndex.putFiles(pos, fileViewModel.files.value!!)
        }
        binding.tabLayout.removeAllTabs()
    }


    private fun saveFile() {
        fileViewModel.currentFile?.let { file ->
            if (file.extension == "class") return // we don't wanna save the output from javap because that'll break the binary
            if (file.exists().not()) {
                file.createNewFile()
            }
            file.writeText(binding.editor.text.toString())
        }
    }

    private fun configureToolbar() {
        binding.toolbar.apply {
            title = project.name
            setNavigationOnClickListener {
                binding.editor.hideEditorWindows()
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

                    R.id.undo -> {
                        binding.editor.undo()
                        true
                    }

                    R.id.redo -> {
                        binding.editor.redo()
                        true
                    }

                    R.id.action_format -> {
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
                        }.commit()
                        true
                    }

                    else -> false
                }
            }
        }
    }

    private fun formatCodeAsync() {
        val text = binding.editor.text.toString()
        val content = binding.editor.text
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
