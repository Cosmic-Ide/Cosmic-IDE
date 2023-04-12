package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dalvik.system.DexClassLoader
import dalvik.system.DexFile
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cosmicide.project.Project
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.databinding.FragmentCompileInfoBinding
import org.cosmicide.rewrite.extension.setFont
import org.cosmicide.rewrite.util.ProjectHandler
import java.io.OutputStream
import java.io.PrintStream
import java.lang.reflect.Modifier

class ProjectOutputFragment : BaseBindingFragment<FragmentCompileInfoBinding>() {
    val project: Project = ProjectHandler.getProject()
        ?: throw IllegalStateException("No project set")

    override fun getViewBinding() = FragmentCompileInfoBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.inflateMenu(R.menu.output_menu)
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.reload -> {
                    val text = binding.infoEditor.text
                    text.insert(text.cursor.rightLine, text.cursor.rightColumn, "--- Stopped ---\n")
                    runProject()
                    true
                }

                else -> false
            }
        }

        binding.infoEditor.apply {
            colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            setEditorLanguage(TextMateLanguage.create("source.build", false))
            editable = false
            isWordwrap = true
            setTextSize(14f)
            setFont()
        }

        binding.toolbar.title = "Running ${project.name}"
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                runProject()
            }
        }
    }

    fun runProject() = CoroutineScope(Dispatchers.IO).launch {
        val systemOut = PrintStream(object : OutputStream() {
            override fun write(p0: Int) {
                val text = binding.infoEditor.text
                val cursor = text.cursor
                lifecycleScope.launch(Dispatchers.Main) {
                    text.insert(cursor.rightLine, cursor.rightColumn, p0.toChar().toString())
                }
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
}