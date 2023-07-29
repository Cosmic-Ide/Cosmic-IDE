/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.adapter

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SubscriptionReceipt
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.subscribeEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cosmicide.build.Javap
import org.cosmicide.editor.analyzers.EditorDiagnosticsMarker
import org.cosmicide.rewrite.databinding.EditorFragmentBinding
import org.cosmicide.rewrite.editor.IdeEditor
import org.cosmicide.rewrite.editor.language.KotlinLanguage
import org.cosmicide.rewrite.editor.language.TsLanguageJava
import org.cosmicide.rewrite.extension.setFont
import org.cosmicide.rewrite.model.FileViewModel
import org.cosmicide.rewrite.util.ProjectHandler
import java.io.File

class EditorAdapter(val fragment: Fragment, val fileViewModel: FileViewModel) :
    FragmentStateAdapter(fragment) {

    private var ids = fileViewModel.files.value!!.map { it.hashCode().toLong() }
    val fragments = mutableListOf<CodeEditorFragment>()

    init {
        fileViewModel.files.observe(fragment.viewLifecycleOwner) { files ->
            notifyItemRangeChanged(0, itemCount)
            fragments.clear()
            ids = files.map { it.hashCode().toLong() }
        }
        System.loadLibrary("android-tree-sitter")
    }

    override fun getItemCount(): Int {
        return fileViewModel.files.value!!.size
    }

    override fun createFragment(position: Int): Fragment {
        val fragment = CodeEditorFragment().apply {
            arguments = Bundle().apply {
                putSerializable("file", fileViewModel.files.value!![position])
            }
        }
        fragments.add(fragment)
        return fragment
    }

    fun getItem(position: Int): CodeEditorFragment? {
        if (position >= fragments.size) return null
        if (position < 0) return null
        return fragments[position]
    }

    override fun getItemId(position: Int): Long {
        return fileViewModel.files.value!![position].hashCode().toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        return ids.contains(itemId)
    }

    fun saveAll() {
        fragments.forEach { it.save() }
    }

    class CodeEditorFragment : Fragment() {

        private lateinit var eventReceiver: SubscriptionReceipt<ContentChangeEvent>
        private lateinit var binding: EditorFragmentBinding
        lateinit var editor: IdeEditor
        val file by lazy { requireArguments().getSerializable("file") as File }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            binding = EditorFragmentBinding.inflate(inflater, container, false)
            editor = binding.editor
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            setupSymbols()
            setText()
            editor.setFont()
            setColorScheme()
            lifecycleScope.launch(Dispatchers.IO) {
                setEditorLanguage()
            }
        }

        private fun setupSymbols() {
            binding.apply {
                symbolView.bindEditor(editor)
                symbolView.addSymbols(
                    arrayOf(
                        "→",
                        "(",
                        ")",
                        "{",
                        "}",
                        "[",
                        "]",
                        ";",
                        ",",
                        ".",
                    ),
                    arrayOf(
                        "\t",
                        "(",
                        ")",
                        "{",
                        "}",
                        "[",
                        "]",
                        ";",
                        ",",
                        ".",
                    )
                )
            }
        }

        private fun setEditorLanguage() {
            val project = ProjectHandler.getProject() ?: return
            when (file.extension) {
                "java" -> {
                    editor.setEditorLanguage(
                        TsLanguageJava.getInstance(
                            editor,
                            project,
                            file
                        )
                    )
                    eventReceiver =
                        editor.subscribeEvent(EditorDiagnosticsMarker(editor, file, project))
                }

                "kt" -> {
                    if (editor.editorLanguage is KotlinLanguage) return
                    editor.setEditorLanguage(
                        KotlinLanguage(
                            editor,
                            project,
                            file
                        )
                    )
                }

                "class" -> {
                    editor.setEditorLanguage(TextMateLanguage.create("source.java", true))
                }

                else -> {
                    editor.setEditorLanguage(EmptyLanguage())
                }
            }
        }

        private fun setColorScheme() {
            editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
        }

        private fun setText() {
            val isBinary = file.extension == "class"

            if (isBinary) {
                editor.setText(Javap.disassemble(file.absolutePath))
                return
            }

            println("Reading file: ${file.absolutePath}")
            editor.setText(file.readText())
        }

        fun save() {
            if (file.extension == "class") return
            file.writeText(editor.text.toString())
        }

        override fun onConfigurationChanged(newConfig: Configuration) {
            super.onConfigurationChanged(newConfig)
            setColorScheme()
        }

        override fun onDestroy() {
            super.onDestroy()
            if (::eventReceiver.isInitialized) eventReceiver.unsubscribe()
            if (::editor.isInitialized) editor.release()
        }
    }
}
