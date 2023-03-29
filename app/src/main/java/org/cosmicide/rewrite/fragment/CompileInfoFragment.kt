package org.cosmicide.rewrite.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.build.BuildReporter
import org.cosmicide.project.Project
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.compile.Compiler
import org.cosmicide.rewrite.databinding.FragmentCompileInfoBinding
import org.cosmicide.rewrite.extension.setFont
import org.cosmicide.rewrite.util.ProjectHandler

/**
 * A fragment for displaying information about the compilation process.
 */
class CompileInfoFragment : Fragment() {

    private val project: Project = ProjectHandler.getProject()
        ?: throw IllegalStateException("No project set")
    private val compiler: Compiler = Compiler(project)
    private lateinit var binding: FragmentCompileInfoBinding

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCompileInfoBinding.inflate(inflater, container, false)

        binding.infoEditor.apply {
            colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            setEditorLanguage(TextMateLanguage.create("source.build", false))
            editable = false
            setTextSize(16f)
            isWordwrap = true
            isLineNumberEnabled = false
            setFont()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reporter = BuildReporter { report ->
                    if (report.message.isEmpty()) {
                        return@BuildReporter
                    }

                    // Update the info editor with the build output
                    launch(Dispatchers.Main) {
                        val text = binding.infoEditor.text
                        val cursor = text.cursor
                        text.insert(cursor.rightLine, cursor.rightColumn, "${report.kind}: ${report.message}\n")
                    }
                }

                compiler.compile(reporter)
                if (reporter.buildSuccess) {
                    withContext(Dispatchers.Main) {
                        navigateToProjectOutputFragment()
                    }
                }
            } catch (e: Exception) {
                /* withContext(Dispatchers.Main) {
                    binding.infoEditor.text = e.message ?: "Unknown error"
                } */
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        binding.infoEditor.release()
        super.onDestroyView()
    }

    private fun navigateToProjectOutputFragment() {
        val navController = findNavController()
        // navController.popBackStack(R.id.CompileInfoFragment, false)
        navController.navigate(R.id.CompileInfoFragment_to_ProjectOutputFragment)
    }
}