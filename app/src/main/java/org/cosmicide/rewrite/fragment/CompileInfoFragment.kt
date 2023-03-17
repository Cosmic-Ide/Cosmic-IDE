package org.cosmicide.rewrite.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import org.cosmicide.build.BuildReporter
import org.cosmicide.build.java.JavaCompileTask
import org.cosmicide.project.Project
import org.cosmicide.rewrite.databinding.FragmentCompileInfoBinding
import org.cosmicide.rewrite.editor.util.EditorUtil
import org.cosmicide.rewrite.util.Constants

class CompileInfoFragment : Fragment() {
    private lateinit var project: Project
    private lateinit var binding: FragmentCompileInfoBinding

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        project = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(Constants.PROJECT, Project::class.java)!!
        } else {
            arguments?.getSerializable(Constants.PROJECT) as Project
        }
        super.onCreate(savedInstanceState)
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
        binding.infoEditor.setTextSize(18f)
        EditorUtil.setEditorFont(binding.infoEditor)
        // Inflate the layout for this fragment
        requireActivity().lifecycleScope.launchWhenStarted {
            JavaCompileTask(project).execute(BuildReporter { kind, message ->
                if (message.isEmpty()) return@BuildReporter
                val text = binding.infoEditor.text
                val cursor = text.cursor
                text.insert(cursor.rightLine, cursor.rightColumn, "$kind: $message\n")
            })
        }
        return binding.root
    }
}