package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dalvik.system.DexClassLoader
import dalvik.system.DexFile
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import kotlinx.coroutines.launch
import org.cosmicide.project.Project
import org.cosmicide.rewrite.compile.Compiler
import org.cosmicide.rewrite.databinding.FragmentCompileInfoBinding
import org.cosmicide.rewrite.extension.setFont
import org.cosmicide.rewrite.util.ProjectHandler
import java.io.OutputStream
import java.io.PrintStream
import java.lang.reflect.Modifier

class ProjectOutputFragment : Fragment() {
    private val project: Project = ProjectHandler.getProject()
        ?: throw IllegalStateException("No project set")
    private lateinit var binding: FragmentCompileInfoBinding
    private val compiler: Compiler = Compiler(project)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCompileInfoBinding.inflate(inflater, container, false)

        binding.infoEditor.apply {
            colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            setEditorLanguage(TextMateLanguage.create("source.build", false))
            editable = false
            isWordwrap = true
            setTextSize(14f)
            setFont()
        }

        lifecycleScope.launch {
            val systemOut = PrintStream(object : OutputStream() {
                override fun write(p0: Int) {
                    val text = binding.infoEditor.text
                    val cursor = text.cursor
                    text.insert(cursor.rightLine, cursor.rightColumn, p0.toChar().toString())
                }
            })
            System.setOut(systemOut)
            System.setErr(systemOut)

            val dexFile = DexFile(project.binDir.resolve("classes.dex"))
            val classes = dexFile.entries().toList()
            val className = classes[0]

            val loader = DexClassLoader(
                project.binDir.resolve("classes.dex").absolutePath,
                project.binDir.toString(),
                null,
                this@ProjectOutputFragment.javaClass.classLoader
            )

            runCatching {
                loader.loadClass(className)
            }.onSuccess { clazz ->
                if (clazz.declaredMethods.any { it.name == "main" }) {
                    val method = clazz.getDeclaredMethod("main", Array<String>::class.java)
                    if (Modifier.isStatic(method.modifiers)) {
                        method.invoke(null, arrayOf<String>())
                    } else if (Modifier.isPublic(method.modifiers)) {
                        method.invoke(clazz.newInstance(), arrayOf<String>())
                    } else {
                        System.err.println("Main method is not public or static")
                    }
                } else {
                    System.err.println("No main method found")
                }
            }.onFailure { e ->
                System.err.println("Error loading class: ${e.message}")
            }.also {
                systemOut.close()
            }
        }

        return binding.root
    }

    
}