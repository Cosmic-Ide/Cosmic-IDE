package org.cosmic.ide.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import org.cosmic.ide.ApplicationLoader
import org.cosmic.ide.ProblemMarker
import org.cosmic.ide.R
import org.cosmic.ide.activity.MainActivity
import org.cosmic.ide.activity.editor.CodeEditorView
import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.databinding.FragmentCodeEditorBinding
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

        currentFile = File(arguments?.getString("path", ""))
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
            arrayOf("->", "{", "}", "(", ")", ",", ".", ";", "\"", "?", "+", "-", "*", "/"),
            arrayOf("\t", "{}", "}", "(", ")", ",", ".", ";", "\"\"", "?", "+", "-", "*", "/")
        )

        if (currentFile.exists()) {
            try {
                binding.editor.setText(FileUtil.readFile(currentFile))
            } catch (e: IOException) {
                (requireActivity() as MainActivity).dialog("Failed to open file", e.toString(), true)
            }
            if (currentFile.getPath().endsWith(".kt")) {
                setEditorLanguage(LANGUAGE_KOTLIN)
            } else if (currentFile.getPath().endsWith(".java") ||
                currentFile.endsWith(".jav")
            ) {
                setEditorLanguage(LANGUAGE_JAVA)
            }
            binding.editor
                .getText()
                .addContentListener(
                    ProblemMarker(
                        ApplicationLoader.applicationContext(),
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

    private fun configureEditor(editor: CodeEditorView) {
        editor.setTypefaceText(ResourcesCompat.getFont(requireContext(), R.font.jetbrains_mono_regular))
        editor.setTextSize(12F)
        editor.setEdgeEffectColor(Color.TRANSPARENT)
        editor.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO)

        var props = editor.getProps().apply {
            overScrollEnabled = false
            allowFullscreen = false
            deleteEmptyLineFast = false
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
            else -> binding.editor.setEditorLanguage(EmptyLanguage())
        }
        binding.editor.setColorScheme(getColorScheme())
    }

    private fun getColorScheme(): TextMateColorScheme {
        try {
            var themeSource: IThemeSource
            if (ApplicationLoader.isDarkMode(requireContext())) {
                themeSource =
                    IThemeSource.fromInputStream(
                        requireContext().getAssets().open("textmate/darcula.json"),
                        "darcula.json",
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
            return EmptyLanguage()
        }
    }

    private fun getKotlinLanguage(): Language {
        try {
            return TextMateLanguage.create(
                IGrammarSource.fromInputStream(
                    requireContext().assets.open("textmate/kotlin/syntaxes/kotlin.tmLanguage"),
                    "kotlin.tmLanguage",
                    null
                ), 
                InputStreamReader(
                    requireContext().assets.open("textmate/kotlin/language-configuration.json")
                ),
                getColorScheme().themeSource
            )
        } catch (e: IOException) {
            return EmptyLanguage()
        }
    }

    fun getEditor(): CodeEditorView {
        return binding.editor
    }

    fun save() {
        if (currentFile.exists()) {
            var oldContents = ""
            try {
                oldContents = FileUtil.readFile(currentFile)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            if (oldContents.equals(binding.editor.getText().toString())) return
            try {
                FileUtil.writeFileFromString(currentFile, binding.editor.getText().toString())
            } catch (e: IOException) {
                // ignored
            }
        }
    }

    companion object {
        const val LANGUAGE_JAVA = 0
        const val LANGUAGE_KOTLIN = 1
        const val TAG = "CodeEditorFragment"

        fun newInstance(file: File): CodeEditorFragment {
            val args: Bundle = Bundle()
            args.putString("path", file.getAbsolutePath())
            val fragment = CodeEditorFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
