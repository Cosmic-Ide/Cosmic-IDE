package org.cosmicide.rewrite.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cosmicide.build.BuildReporter
import org.cosmicide.project.Project
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.compile.Compiler
import org.cosmicide.rewrite.databinding.FragmentCompileInfoBinding
import org.cosmicide.rewrite.editor.util.EditorUtil
import org.cosmicide.rewrite.util.ProjectHandler

class CompileInfoFragment : Fragment() {
    private lateinit var project: Project
    private lateinit var binding: FragmentCompileInfoBinding
    private lateinit var compiler: Compiler

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        project = ProjectHandler.project!!
        compiler = Compiler(project)
        CoroutineScope(Dispatchers.IO).launch {
            val reporter = BuildReporter { kind, message ->
                if (message.isEmpty()) return@BuildReporter

                requireActivity().runOnUiThread {
                    val text = binding.infoEditor.text
                    val cursor = text.cursor
                    text.insert(cursor.rightLine, cursor.rightColumn, "$kind: $message\n")
                }
            }
            compiler.compile(reporter)
            println("Compile")
            if (reporter.compileSuccess) {
                requireActivity().runOnUiThread {
                    navigateToProjectOutputFragment()
                }
            }
        }
    }


    private fun navigateToProjectOutputFragment() {
        val navController = findNavController()
        navController.popBackStack(R.id.CompileInfoFragment, false)
        navController.navigate(
            R.id.CompileInfoFragment_to_ProjectOutputFragment
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCompileInfoBinding.inflate(inflater, container, false)

        binding.infoEditor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
        binding.infoEditor.setEditorLanguage(TextMateLanguage.create("source.build", false))
        binding.infoEditor.editable = false
        binding.infoEditor.isWordwrap = true
        binding.infoEditor.setTextSize(16f)
        binding.infoEditor.isLineNumberEnabled = false
        EditorUtil.setEditorFont(binding.infoEditor)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.infoEditor.release()
    }
}