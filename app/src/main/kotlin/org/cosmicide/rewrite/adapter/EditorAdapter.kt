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
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.itsaky.androidide.treesitter.java.TSLanguageJava
import io.github.rosemoe.sora.editor.ts.LocalsCaptureSpec
import io.github.rosemoe.sora.editor.ts.TsLanguage
import io.github.rosemoe.sora.editor.ts.TsLanguageSpec
import io.github.rosemoe.sora.editor.ts.predicate.builtin.MatchPredicate
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SubscriptionReceipt
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.subscribeEvent
import org.cosmicide.build.Javap
import org.cosmicide.editor.analyzers.EditorDiagnosticsMarker
import org.cosmicide.rewrite.common.Prefs
import org.cosmicide.rewrite.databinding.EditorFragmentBinding
import org.cosmicide.rewrite.editor.IdeEditor
import org.cosmicide.rewrite.editor.language.KotlinLanguage
import org.cosmicide.rewrite.extension.setFont
import org.cosmicide.rewrite.model.FileViewModel
import org.cosmicide.rewrite.util.ProjectHandler
import java.io.File

class EditorAdapter(val fragment: Fragment, val fileViewModel: FileViewModel) :
    FragmentStateAdapter(fragment) {

    private var ids = fileViewModel.files.value!!.map { it.hashCode().toLong() }
    private val fragments = mutableListOf<CodeEditorFragment>()

    init {
        fileViewModel.files.observe(fragment.viewLifecycleOwner) { files ->
            notifyItemRangeChanged(0, itemCount)
            fragments.clear()
            ids = files.map { it.hashCode().toLong() }
        }
        System.loadLibrary("android-tree-sitter")
        System.loadLibrary("tree-sitter-java")
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
            setColorScheme()
            setEditorLanguage()
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
                    val lang = TSLanguageJava.getInstance()
                    editor.setEditorLanguage(
                        TsLanguage(
                            TsLanguageSpec(
                                language = lang,
                                highlightScmSource = requireContext().assets.open("tree-sitter-queries/java/highlights.scm")
                                    .reader().readText(),
                                codeBlocksScmSource = requireContext().assets.open("tree-sitter-queries/java/blocks.scm")
                                    .reader().readText(),
                                bracketsScmSource = requireContext().assets.open("tree-sitter-queries/java/brackets.scm")
                                    .reader().readText(),
                                localsScmSource = requireContext().assets.open("tree-sitter-queries/java/locals.scm")
                                    .reader().readText(),
                                localsCaptureSpec = object : LocalsCaptureSpec() {
                                    override fun isScopeCapture(captureName: String): Boolean {
                                        return captureName == "scope"
                                    }

                                    override fun isReferenceCapture(captureName: String): Boolean {
                                        return captureName == "reference"
                                    }

                                    override fun isDefinitionCapture(captureName: String): Boolean {
                                        return captureName == "definition.var" || captureName == "definition.field"
                                    }

                                    override fun isMembersScopeCapture(captureName: String): Boolean {
                                        return captureName == "scope.members"
                                    }
                                },
                                predicates = listOf(
                                    MatchPredicate
                                )
                            ),
                            tab = Prefs.useSpaces.not(),
                            themeDescription = {
                                TextStyle.makeStyle(
                                    EditorColorScheme.COMMENT,
                                    0,
                                    false,
                                    true,
                                    false
                                ) applyTo "comment"
                                TextStyle.makeStyle(
                                    EditorColorScheme.KEYWORD,
                                    0,
                                    true,
                                    false,
                                    false
                                ) applyTo "keyword"
                                TextStyle.makeStyle(EditorColorScheme.LITERAL) applyTo arrayOf(
                                    "constant.builtin",
                                    "string",
                                    "number"
                                )
                                TextStyle.makeStyle(EditorColorScheme.IDENTIFIER_VAR) applyTo arrayOf(
                                    "variable.builtin",
                                    "variable",
                                    "constant"
                                )
                                TextStyle.makeStyle(EditorColorScheme.IDENTIFIER_NAME) applyTo arrayOf(
                                    "type.builtin",
                                    "type",
                                    "attribute"
                                )
                                TextStyle.makeStyle(EditorColorScheme.FUNCTION_NAME) applyTo arrayOf(
                                    "function.method",
                                    "function.builtin",
                                    "variable.field"
                                )
                                TextStyle.makeStyle(EditorColorScheme.OPERATOR) applyTo arrayOf("operator")
                            }
                        )
                    )
                    /*if (editor.editorLanguage is JavaLanguage) return
                    editor.setEditorLanguage(
                        JavaLanguage(
                            editor,
                            project,
                            file
                        )
                    )
                     */
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

            editor.setFont()
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
            editor.release()
        }
    }
}
