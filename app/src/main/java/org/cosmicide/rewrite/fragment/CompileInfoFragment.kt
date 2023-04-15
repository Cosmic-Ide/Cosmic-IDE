package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.build.BuildReporter
import org.cosmicide.project.Project
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.common.Prefs
import org.cosmicide.rewrite.compile.Compiler
import org.cosmicide.rewrite.databinding.FragmentCompileInfoBinding
import org.cosmicide.rewrite.extension.setFont
import org.cosmicide.rewrite.util.ProjectHandler

/**
 * A fragment for displaying information about the compilation process.
 */
class CompileInfoFragment : BaseBindingFragment<FragmentCompileInfoBinding>() {
    val project: Project = ProjectHandler.getProject()
        ?: throw IllegalStateException("No project set")
    val reporter by lazy {
        BuildReporter { report ->
            if (report.message.isEmpty()) return@BuildReporter
            // Update the info editor with the build output
            val text = binding.infoEditor.text
            val cursor = text.cursor
            text.insert(
                cursor.rightLine,
                cursor.rightColumn,
                "${report.kind}: ${report.message}\n"
            )
        }
    }
    val compiler: Compiler = Compiler(project, reporter)

    override fun getViewBinding() = FragmentCompileInfoBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.infoEditor.apply {
            setFont()
            colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            setEditorLanguage(TextMateLanguage.create("source.build", true))
            editable = false
            setTextSize(Prefs.editorFontSize)
            isLineNumberEnabled = false
            isWordwrap = true
            invalidate()
        }

        binding.toolbar.apply {
            title = "Compiling ${project.name}"
            setNavigationIcon(R.drawable.baseline_arrow_back_ios_24)
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                compiler.compile()
                if (reporter.buildSuccess) {
                    withContext(Dispatchers.Main) {
                        navigateToProjectOutputFragment()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.infoEditor.setText(e.stackTraceToString())
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.infoEditor.release()
        super.onDestroyView()
    }

    private fun navigateToProjectOutputFragment() {
        parentFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, ProjectOutputFragment())
            addToBackStack(null)
            setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        }.commit()
    }
}