package org.cosmic.ide.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import org.cosmic.ide.App
import org.cosmic.ide.ProblemMarker
import org.cosmic.ide.R
import org.cosmic.ide.activity.MainActivity
import org.cosmic.ide.activity.editor.KotlinLanguage
import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.databinding.FragmentCodeEditorBinding
import org.cosmic.ide.activity.editor.completion.CustomCompletionItemAdapter
import org.cosmic.ide.activity.editor.completion.CustomCompletionLayout
import org.eclipse.tm4e.core.registry.IThemeSource
import org.eclipse.tm4e.core.registry.IGrammarSource
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class CodeEditorFragment : Fragment() {

    private lateinit var binding: FragmentCodeEditorBinding

    private lateinit var currentFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentFile = File(arguments?.getString("path", "")!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCodeEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        configureEditor(binding.editor)

        val inputView = binding.inputView
        inputView.bindEditor(binding.editor)
        inputView.addSymbols(
            arrayOf("→", "{", "}", "(", ")", ",", ".", ";", "\"", "?", "+", "-", "*", "/"),
            arrayOf("\t", "{}", "}", "()", ")", ",", ".", ";", "\"\"", "?", "+", "-", "*", "/")
        )

        if (currentFile.exists()) {
            try {
                binding.editor.setText(currentFile.readText())
            } catch (e: IOException) {
                (requireActivity() as MainActivity).dialog("Failed to open file", e.toString(), true)
            }
            if (currentFile.extension.equals("kt")) {
                setEditorLanguage(LANGUAGE_KOTLIN)
            } else if (currentFile.extension.equals("java") ||
                currentFile.extension.equals("jav")
            ) {
                setEditorLanguage(LANGUAGE_JAVA)
            } else if (currentFile.extension.equals("smali")) {
                setEditorLanguage(LANGUAGE_SMALI)
            }
            binding.editor
                .getText()
                .addContentListener(
                    ProblemMarker(
                        requireActivity(),
                        getEditor(),
                        currentFile,
                        (requireActivity() as MainActivity).getProject()
                    )
                )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.editor.release()
    }

    private fun configureEditor(editor: CodeEditor) {
        with(editor) {
            getComponent(EditorAutoCompletion::class.java).setLayout(CustomCompletionLayout())
            getComponent(EditorAutoCompletion::class.java).setAdapter(CustomCompletionItemAdapter())
            setTypefaceText(ResourcesCompat.getFont(requireContext(), R.font.jetbrains_mono_regular))
            setTextSize(12F)
            setEdgeEffectColor(Color.TRANSPARENT)
            setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO)
        }
    }

    fun undo() {
        if (binding.editor.canUndo()) binding.editor.undo()
    }

    fun redo() {
        if (binding.editor.canRedo()) binding.editor.redo()
    }

    private fun setEditorLanguage(lang: Int) {
        when (lang) {
            LANGUAGE_JAVA -> binding.editor.setEditorLanguage(getJavaLanguage())
            LANGUAGE_KOTLIN -> binding.editor.setEditorLanguage(getKotlinLanguage())
            LANGUAGE_SMALI -> binding.editor.setEditorLanguage(getSmaliLanguage())
            else -> binding.editor.setEditorLanguage(EmptyLanguage())
        }
        binding.editor.setColorScheme(getColorScheme())
    }

    private fun getColorScheme(): TextMateColorScheme {
        try {
            var themeSource: IThemeSource
            if (App.isDarkMode(requireContext())) {
                themeSource =
                    IThemeSource.fromInputStream(
                        requireContext().getAssets().open("textmate/darcula.tmTheme.json"),
                        "darcula.tmTheme.json",
                        null
                    )
            } else {
                themeSource =
                    IThemeSource.fromInputStream(
                        requireContext().assets.open("textmate/light.tmTheme"),
                        "light.tmTheme",
                        null
                    )
            }
            return TextMateColorScheme.create(themeSource)
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }

    private fun getJavaLanguage(): Language {
        try {
            return TextMateLanguage.create(
                IGrammarSource.fromInputStream(
                    requireContext().assets.open("textmate/java/syntaxes/java.tmLanguage.json"),
                    "java.tmLanguage.json",
                    null
                ),
                InputStreamReader(
                    requireContext().assets.open("textmate/java/language-configuration.json")
                ),
                getColorScheme().themeSource
            )
        } catch (e: IOException) {
            Log.e("CodeEditorFragment", "Failed to create instance of TextMateLanguage", e);
            return EmptyLanguage()
        }
    }

    private fun getKotlinLanguage(): Language {
        try {
            return KotlinLanguage(binding.editor, (requireActivity() as MainActivity).getProject(), currentFile, getColorScheme().themeSource)
        } catch (e: IOException) {
            Log.e("CodeEditorFragment", "Failed to create instance of KotlinLanguage", e);
            return EmptyLanguage()
        }
    }

    private fun getSmaliLanguage(): Language {
        try {
            return TextMateLanguage.create(
                IGrammarSource.fromInputStream(
                    requireContext().assets.open("textmate/smali/syntaxes/smali.tmLanguage.json"),
                    "smali.tmLanguage.json",
                    null),
                InputStreamReader(
                    requireContext().assets.open("textmate/smali/language-configuration.json")
                ),
                getColorScheme().themeSource
            )
        } catch (e: IOException) {
            return EmptyLanguage()
        }
    }

    fun getEditor() = binding.editor

    fun save() {
        if (currentFile.exists()) {
            var oldContents = ""
            try {
                oldContents = currentFile.readText()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val newContents = binding.editor.getText().toString()
            if (oldContents.equals(newContents)) return
            try {
                currentFile.writeText(newContents)
            } catch (e: IOException) {
                Log.e("CodeEditorFragment", "Failed to save file", e);
            }
        }
    }

    companion object {
        const val LANGUAGE_JAVA = 0
        const val LANGUAGE_KOTLIN = 1
        const val LANGUAGE_SMALI = 2

        fun newInstance(file: File): CodeEditorFragment {
            val args: Bundle = Bundle()
            args.putString("path", file.absolutePath)
            val fragment = CodeEditorFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
