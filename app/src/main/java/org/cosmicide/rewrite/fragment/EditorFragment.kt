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
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
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
    private val project: Project =
        ProjectHandler.getProject() ?: throw IllegalStateException("No project set")
    private val fileIndex: FileIndex = FileIndex(project)
    private lateinit var fileViewModel: FileViewModel

    override fun getViewBinding() = FragmentEditorBinding.inflate(layoutInflater)

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setEditorLanguage()
    }

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

            val rootItem = FileSet(
                project.root, traverseDirectory(project.root) as MutableSet<FileSet>
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
                val isBinary = fileViewModel.currentFile?.extension == "class"
                override fun onTabSelected(tab: TabLayout.Tab) {
                    if (fileViewModel.currentPosition.value != tab.position && isBinary.not()) fileViewModel.setCurrentPosition(
                        tab.position
                    )
                    val file = fileViewModel.currentFile!!
                    if (isBinary) {
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
                    tab.customView = Chip(requireContext()).apply {
                        text = file.name
                        chipStrokeWidth = 0f
                    }
                    (tab.customView as View).apply {
                        setOnLongClickListener {
                            showMenu(it, R.menu.tab_menu, tab.position)
                            true
                        }
                        setOnClickListener {
                            tab.select()
                        }
                    }
                    binding.tabLayout.apply {
                        addTab(tab, false)
                    }
                }
            }

            fileViewModel.currentPosition.observe(viewLifecycleOwner) { position ->
                position?.takeIf { it != -1 }?.let {
                    if (binding.drawer.isOpen) {
                        binding.drawer.close()
                    }
                    if (binding.tabLayout.selectedTabPosition != it) {
                        binding.tabLayout.getTabAt(it)?.select()
                    }
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
                            binding.editor, file, project
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
        fileViewModel.currentFile?.let { file ->
            if (file.exists()) {
                file.writeText(binding.editor.text.toString())
                return
            }
            file.createNewFile()
            file.writeText(binding.editor.text.toString())
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
