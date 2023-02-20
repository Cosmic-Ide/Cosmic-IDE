package org.cosmic.ide.fragment

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import org.cosmic.ide.ProblemMarker
import org.cosmic.ide.R
import org.cosmic.ide.activity.MainActivity
import org.cosmic.ide.databinding.FragmentCodeEditorBinding
import org.cosmic.ide.ui.editor.JavaLanguage
import org.cosmic.ide.ui.editor.KotlinLanguage
import org.cosmic.ide.ui.editor.completion.CustomCompletionItemAdapter
import org.cosmic.ide.ui.editor.completion.CustomCompletionLayout
import org.cosmic.ide.ui.preference.Settings
import org.cosmic.ide.util.AndroidUtilities
import org.cosmic.ide.util.EditorUtil
import java.io.File
import java.io.IOException

class CodeEditorFragment : Fragment() {
    private var _binding: FragmentCodeEditorBinding? = null
    private val binding get() = _binding!!
    private val TAG = "CodeEditorFragment"
    private val settings: Settings by lazy { Settings() }

    private lateinit var currentFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentFile = File(arguments?.getString("path", "")!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCodeEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        configureEditor(binding.editor)

        binding.inputView.apply {
            bindEditor(binding.editor)
            addSymbol("→", "\t")
            addSymbols(arrayOf("{", "}", "(", ")", ",", ".", ";", "\"", "?", "+", "-", "*", "/"))
        }

        if (currentFile.exists()) {
            try {
                binding.editor.setText(currentFile.readText())
            } catch (e: IOException) {
                MaterialAlertDialogBuilder(
                    requireContext(),
                    AndroidUtilities.dialogFullWidthButtonsThemeOverlay
                )
                    .setTitle(requireContext().getString(R.string.error_file_open))
                    .setMessage(e.localizedMessage)
                    .setPositiveButton(requireContext().getString(R.string.dialog_close), null)
                    .setNegativeButton(requireContext().getString(R.string.copy_stacktrace)) { _, which ->
                        if (which == DialogInterface.BUTTON_NEGATIVE) {
                            AndroidUtilities.copyToClipboard(e.localizedMessage)
                        }
                    }
                    .show()
            }
            when (currentFile.extension) {
                "kt" -> setEditorLanguage(LANGUAGE_KOTLIN)
                "java" -> setEditorLanguage(LANGUAGE_JAVA)
                "smali" -> setEditorLanguage(LANGUAGE_SMALI)
                else -> setEditorLanguage(-1)
            }
            binding.editor
                .text
                .addContentListener(
                    ProblemMarker(
                        requireActivity(),
                        getEditor(),
                        currentFile,
                        (requireActivity() as MainActivity).project
                    )
                )
        }
    }

    override fun onDestroyView() {
        binding.editor.release()
//        provider.exit()
        _binding = null
        super.onDestroyView()
    }

    private fun configureEditor(editor: CodeEditor) {
        val fontSize = settings.fontSize.toFloat()

        with(editor) {
            getComponent(EditorAutoCompletion::class.java).setLayout(CustomCompletionLayout())
            getComponent(EditorAutoCompletion::class.java).setAdapter(CustomCompletionItemAdapter())
            typefaceText = ResourcesCompat.getFont(requireContext(), R.font.jetbrains_mono_light)
            setTextSize(fontSize)
            edgeEffectColor = Color.TRANSPARENT
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
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
            LANGUAGE_KOTLIN -> binding.editor.setEditorLanguage(getKotlinLanguage())
            LANGUAGE_JAVA -> binding.editor.setEditorLanguage(getJavaLanguage())
            LANGUAGE_SMALI -> binding.editor.setEditorLanguage(EditorUtil.smaliLanguage)
            else -> binding.editor.setEditorLanguage(EmptyLanguage())
        }
        binding.editor.colorScheme = EditorUtil.getColorScheme(requireActivity())
    }

    private fun getKotlinLanguage(): Language {
        return try {
            KotlinLanguage(binding.editor, (requireActivity() as MainActivity).project, currentFile)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to create instance of KotlinLanguage", e)
            EmptyLanguage()
        }
    }

    private fun getJavaLanguage(): Language {
        return try {
            JavaLanguage(binding.editor, (requireActivity() as MainActivity).project, currentFile)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to create instance of JavaLanguage", e)
            EmptyLanguage()
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
            val newContents = binding.editor.text.toString()
            if (oldContents == newContents) return
            try {
                currentFile.writeText(newContents)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to save file", e)
            }
        }
    }

    companion object {
        const val LANGUAGE_JAVA = 0
        const val LANGUAGE_KOTLIN = 1
        const val LANGUAGE_SMALI = 2

        fun newInstance(file: File) = CodeEditorFragment().apply {
            arguments = Bundle().apply {
                putString("path", file.absolutePath)
            }
        }
    }
}
