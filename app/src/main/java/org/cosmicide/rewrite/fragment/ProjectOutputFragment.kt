package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
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
import org.cosmicide.rewrite.util.Prefs
import org.cosmicide.rewrite.util.ProjectHandler
import java.io.OutputStream
import java.io.PrintStream
import java.lang.reflect.Modifier

class ProjectOutputFragment : BaseBindingFragment<FragmentCompileInfoBinding>() {
    val project: Project = ProjectHandler.getProject()
        ?: throw IllegalStateException("No project set")
    lateinit var runThread: Thread
    var isRunning: Boolean = false

    override fun getViewBinding() = FragmentCompileInfoBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.inflateMenu(R.menu.output_menu)
        binding.toolbar.setOnMenuItemClickListener {
            val transaction = parentFragmentManager.beginTransaction()
            when (it.itemId) {
                R.id.reload -> {
                    val text = binding.infoEditor.text
                    if (isRunning) {
                        runThread.destroy()
                        transaction.apply {
                            replace(R.id.fragment_container, ProjectOutputFragment())
                            commit()
                        }
                    }
                    text.insert(text.cursor.rightLine, text.cursor.rightColumn, "--- Stopped ---\n")
                    runProject()
                    true
                }

                R.id.cancel -> {
                    if (isRunning) {
                        try {
                            runThread.destroy()
                        } catch (_: Throwable) {
                        }
                    }
                    transaction.remove(this).commit()

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
            setTextSize(Prefs.editorFontSize)
            setFont()
            invalidate()
        }

        binding.toolbar.title = "Running ${project.name}"
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        lifecycleScope.launch {
            runProject()
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
                Thread.sleep(1)
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
        runThread = Thread(kotlinx.coroutines.Runnable {
            runCatching {
                loader.loadClass(className)
            }.onSuccess { clazz ->
                isRunning = true
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
                isRunning = false
            }
        })
        runThread.start()
        }
}