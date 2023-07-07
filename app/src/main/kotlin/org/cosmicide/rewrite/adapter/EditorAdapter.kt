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
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import org.cosmicide.build.Javap
import org.cosmicide.editor.analyzers.EditorDiagnosticsMarker
import org.cosmicide.rewrite.editor.IdeEditor
import org.cosmicide.rewrite.editor.language.JavaLanguage
import org.cosmicide.rewrite.editor.language.KotlinLanguage
import org.cosmicide.rewrite.extension.setFont
import org.cosmicide.rewrite.model.FileViewModel
import org.cosmicide.rewrite.util.ProjectHandler
import java.io.File

class EditorAdapter(val fragment: Fragment, val fileViewModel: FileViewModel) :
    FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return fileViewModel.files.value!!.size
    }

    override fun createFragment(position: Int): Fragment {
        return CodeEditorFragment(fileViewModel.files.value!![position])
    }

    fun getItem(position: Int): CodeEditorFragment? {
        return fragment.childFragmentManager.findFragmentByTag("f$position") as CodeEditorFragment?
    }

    class CodeEditorFragment(val file: File) : Fragment() {

        lateinit var editor: IdeEditor
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            editor = IdeEditor(requireContext())
            return editor
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            setEditorLanguage()
            setColorScheme()
            setText()
        }

        private fun setEditorLanguage() {

            when (file.extension) {
                "java" -> {
                    if (editor.editorLanguage is JavaLanguage) return
                    editor.setEditorLanguage(
                        JavaLanguage(
                            editor,
                            ProjectHandler.getProject()!!,
                            file
                        )
                    )
                    editor.text.addContentListener(EditorDiagnosticsMarker.INSTANCE)
                    EditorDiagnosticsMarker.INSTANCE.init(
                        editor,
                        file,
                        ProjectHandler.getProject()!!
                    )
                }

                "kt" -> {
                    if (editor.editorLanguage is KotlinLanguage) return
                    editor.setEditorLanguage(
                        KotlinLanguage(
                            editor,
                            ProjectHandler.getProject()!!,
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

            if (file.extension != "java") {
                editor.text.removeContentListener(EditorDiagnosticsMarker.INSTANCE)
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
    }
}
