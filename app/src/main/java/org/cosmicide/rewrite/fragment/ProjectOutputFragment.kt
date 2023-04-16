package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import dalvik.system.DexClassLoader
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
                        transaction.apply {
                            replace(R.id.fragment_container, ProjectOutputFragment())
                            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                            commit()
                        }
                    }
                    text.insert(text.cursor.rightLine, text.cursor.rightColumn, "--- Stopped ---\n")
                    checkClasses()
                    true
                }

                R.id.cancel -> {
                    transaction.apply {
                        remove(this@ProjectOutputFragment)
                        setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    }.commit()

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
            invalidate()
        }

        binding.toolbar.title = "Running ${project.name}"
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.beginTransaction().apply {
                remove(this@ProjectOutputFragment)
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
            }.commit()
        }
        lifecycleScope.launch {
            checkClasses()
        }
    }

    fun checkClasses() {
        // TODO: Show a recyclerview with all classes and allow the user to select one
        val dexFile = DexBackedDexFile.fromInputStream(
            Opcodes.forApi(33),
            project.binDir.resolve("classes.dex").inputStream().buffered()
        )
        val classes = dexFile.classes.map { it.type.substring(1, it.type.length - 1) }
        if (classes.isEmpty()) {
            binding.infoEditor.setText("No classes found")
            return
        }
        val index = classes.firstOrNull { it.endsWith("Main") } ?: classes.first()

        runClass(index)
    }

    fun runClass(className: String) = CoroutineScope(Dispatchers.IO).launch {
        val systemOut = PrintStream(object : OutputStream() {
            override fun write(p0: Int) {
                val text = binding.infoEditor.text
                val cursor = text.cursor
                lifecycleScope.launch(Dispatchers.Main) {
                    text.insert(cursor.rightLine, cursor.rightColumn, p0.toChar().toString())
                }
                // This is a hack to allow the editor to update properly even when in a while(true) loop
                Thread.sleep(1)
            }
        })
        System.setOut(systemOut)
        System.setErr(systemOut)

        val loader = DexClassLoader(
            project.binDir.resolve("classes.dex").absolutePath,
            project.binDir.toString(),
            null,
            javaClass.classLoader
        )
        runCatching {
            loader.loadClass(className)
        }.onSuccess { clazz ->
                isRunning = true
                if (clazz.declaredMethods.any { it.name == "main" }) {
                    val method = clazz.getDeclaredMethod("main", Array<String>::class.java)
                    if (Modifier.isStatic(method.modifiers)) {
                        method.invoke(null, arrayOf<String>())
                    } else if (Modifier.isPublic(method.modifiers)) {
                        method.invoke(
                            clazz.getDeclaredConstructor().newInstance(),
                            arrayOf<String>()
                        )
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
    }

}