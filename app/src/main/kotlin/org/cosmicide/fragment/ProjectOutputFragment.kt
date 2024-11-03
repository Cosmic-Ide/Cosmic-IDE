/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cosmicide.R
import org.cosmicide.common.BaseBindingFragment
import org.cosmicide.databinding.FragmentCompileInfoBinding
import org.cosmicide.editor.EditorInputStream
import org.cosmicide.project.Project
import org.cosmicide.rewrite.util.MultipleDexClassLoader
import org.cosmicide.util.ProjectHandler
import org.cosmicide.util.makeDexReadOnlyIfNeeded
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
            when (it.itemId) {
                R.id.reload -> {
                    val text = binding.infoEditor.text
                    if (isRunning) {
                        parentFragmentManager.commit {
                            replace(R.id.fragment_container, ProjectOutputFragment())
                        }
                    }
                    text.insert(text.cursor.rightLine, text.cursor.rightColumn, "--- Stopped ---\n")
                    checkClasses()
                    true
                }

                R.id.cancel -> {
                    parentFragmentManager.commit {
                        remove(this@ProjectOutputFragment)
                    }
                    true
                }

                else -> false
            }
        }

        binding.infoEditor.apply {
            setEditorLanguage(TextMateLanguage.create("source.build", false))
        }

        binding.toolbar.title = "Running ${project.name}"
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.commit {
                remove(this@ProjectOutputFragment)
            }
        }

        binding.infoEditor.postDelayed(::checkClasses, 250)
    }

    fun checkClasses() {
        val dex = project.binDir.resolve("classes.dex")
        if (!dex.exists()) {
            binding.infoEditor.setText("classes.dex not found")
            return
        }
        val bufferedInputStream = dex.inputStream().buffered()
        val dexFile = DexBackedDexFile.fromInputStream(
            Opcodes.forApi(33),
            bufferedInputStream
        )
        bufferedInputStream.close()
        val classes = dexFile.classes.map { it.type.substring(1, it.type.length - 1) }
        if (classes.isEmpty()) {
            binding.infoEditor.setText("No classes found")
            return
        }

        println("Found ${classes.size} classes")
        println("Available classes:")
        classes.forEach {
            println("  $it")
        }
        var index = classes.firstOrNull { it.endsWith("Main") }
            ?: classes.firstOrNull { it.endsWith("MainKt") } ?: classes.first()

        if (ProjectHandler.clazz != null) {
            println("Running ${ProjectHandler.clazz}")
            index = ProjectHandler.clazz!!.substringBeforeLast('.')
            ProjectHandler.clazz = null
        }

        runClass(index)
    }

    fun runClass(className: String) = lifecycleScope.launch(Dispatchers.IO) {
        val systemOut = PrintStream(object : OutputStream() {
            override fun write(p0: Int) {
                val text = binding.infoEditor.text
                lifecycleScope.launch {
                    text.insert(
                        text.lineCount - 1,
                        text.getColumnCount(text.lineCount - 1),
                        p0.toChar().toString()
                    )
                }
            }
        })
        System.setOut(systemOut)
        System.setErr(systemOut)
        System.setIn(EditorInputStream(binding.infoEditor))

        val loader = MultipleDexClassLoader(classLoader = javaClass.classLoader!!)

        loader.loadDex(makeDexReadOnlyIfNeeded(project.binDir.resolve("classes.dex")))

        project.buildDir.resolve("libs").listFiles()?.filter { it.extension == "dex" }?.forEach {
            loader.loadDex(makeDexReadOnlyIfNeeded(it))
        }

        runCatching {
            loader.loader.loadClass(className)
        }.onSuccess { clazz ->
            isRunning = true
            System.setProperty("project.dir", project.root.absolutePath)
            if (clazz.declaredMethods.any {
                    it.name == "main" && it.parameterCount == 1 && it.parameterTypes[0] == Array<String>::class.java
                }) {
                val method = clazz.getDeclaredMethod("main", Array<String>::class.java)
                try {
                    if (Modifier.isStatic(method.modifiers)) {
                        method.invoke(null, project.args.toTypedArray())
                    } else if (Modifier.isPublic(method.modifiers)) {
                        method.invoke(
                            clazz.getDeclaredConstructor().newInstance(),
                            project.args.toTypedArray()
                        )
                    } else {
                        System.err.println("Main method is not public or static")
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            } else {
                System.err.println("No main method found")
            }
        }.onFailure { e ->
            System.err.println("Error loading class: ${e.message}")
        }.also {
            systemOut.close()
            System.`in`.close()
            isRunning = false
        }
    }
}

