package org.cosmicide.rewrite.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dalvik.system.DexClassLoader
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import org.cosmicide.project.Project
import org.cosmicide.rewrite.compile.Compiler
import org.cosmicide.rewrite.databinding.FragmentCompileInfoBinding
import org.cosmicide.rewrite.editor.util.EditorUtil
import org.cosmicide.rewrite.util.Constants
import java.io.OutputStream
import java.io.PrintStream

class ProjectOutputFragment : Fragment() {
    private lateinit var project: Project
    private lateinit var binding: FragmentCompileInfoBinding
    private lateinit var compiler: Compiler

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        project = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(Constants.PROJECT, Project::class.java)!!
        } else {
            arguments?.getSerializable(Constants.PROJECT) as Project
        }
        compiler = Compiler(project)
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
        binding.infoEditor.setTextSize(14f)
        EditorUtil.setEditorFont(binding.infoEditor)
        // Inflate the layout for this fragment
        requireActivity().lifecycleScope.launchWhenStarted {
            val systemOut = PrintStream(object : OutputStream() {
                override fun write(p0: Int) {
                    val text = binding.infoEditor.text
                    val cursor = text.cursor
                    text.insert(cursor.rightLine, cursor.rightColumn, p0.toChar().toString())
                }
            })
            System.setOut(systemOut)
            System.setErr(systemOut)
            val loader = DexClassLoader(
                project.binDir.resolve("classes.dex").absolutePath,
                project.binDir.toString(),
                null,
                ClassLoader.getSystemClassLoader()
            )
            loader.loadClass("Main").getDeclaredMethod("main", Array<String>::class.java)
                .invoke(null, arrayOf<String>())
        }
        return binding.root
    }
}