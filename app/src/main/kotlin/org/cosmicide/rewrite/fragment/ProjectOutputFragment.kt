/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import dev.xdark.ssvm.execution.VMException
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cosmicide.project.Project
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.common.Prefs
import org.cosmicide.rewrite.compile.ssvm.SSVM
import org.cosmicide.rewrite.databinding.FragmentCompileInfoBinding
import org.cosmicide.rewrite.editor.EditorInputStream
import org.cosmicide.rewrite.util.FileUtil
import org.cosmicide.rewrite.util.MultipleDexClassLoader
import org.cosmicide.rewrite.util.ProjectHandler
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.lang.reflect.Modifier
import java.util.jar.JarFile
import java.util.zip.ZipFile

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
            setEditorLanguage(TextMateLanguage.create("source.build", false))
            isWordwrap = true
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
        if (Prefs.useSSVM) {
            val jar = project.binDir.resolve("classes.jar")
            if (!jar.exists()) {
                binding.infoEditor.setText("classes.jar not found")
                return
            }
            JarFile(jar).use {
                val mainClass = it.entries().asSequence().firstOrNull { entry ->
                    entry.name == "Main.class"
                }
                if (mainClass == null) {
                    binding.infoEditor.setText("No entrypoint Main class found")
                    return
                }
                runClass(
                    mainClass.name.substringBeforeLast(".class")
                        .replace('/', '.')
                )
            }
            return
        }
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
        val index = classes.firstOrNull { it.endsWith("Main") } ?: classes.first()

        runClass(index)
    }

    fun runClass(className: String) = CoroutineScope(Dispatchers.IO).launch {
        val systemOut = PrintStream(object : OutputStream() {
            override fun write(p0: Int) {
                // This is a hack to allow the editor to update properly even when in a while(true) loop
                Thread.sleep(1)

                val text = binding.infoEditor.text
                lifecycleScope.launch(Dispatchers.Main) {
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

        if (Prefs.useSSVM) {
            isRunning = true
            initVM()
            invoke(className)
            isRunning = false
            return@launch
        }

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
            System.`in`.close()
            isRunning = false
        }
    }

    private val ssvm by lazy {
        SSVM(
            ZipFile(
                FileUtil.dataDir.resolve("rt.jar"),
                Charsets.UTF_8
            )
        )
    }

    private fun initVM() {
        val time = System.currentTimeMillis()

        catchVMException {
            ssvm.addProperty("project.dir", project.root.absolutePath)
            ssvm.addProperty("user.home", project.binDir.absolutePath)
            ssvm.addProperty(
                "java.home",
                project.binDir.absolutePath
            )
            // init VM
            ssvm.initVM()

            // add test JAR
            FileUtil.classpathDir.walk().filter { it.extension == "jar" }.forEach {
                ssvm.addURL(it)
            }
            project.libDir.walk().filter { it.extension == "jar" }.forEach {
                ssvm.addURL(it)
            }

            ssvm.addURL(project.binDir.resolve("classes.jar"))
        }

        println("[VM] Initialized in ${System.currentTimeMillis() - time} ms")
    }

    private fun invoke(className: String) {
        catchVMException {
            ssvm.invokeMainMethod(className)
        }
    }

    private fun catchVMException(runnable: () -> Unit) {
        try {
            runnable()
        } catch (ex: Throwable) {
            val cause = ex.cause
            System.err.println(
                "[VM] VM exception: ${
                    if (cause is VMException)
                        SSVM.throwableToString(cause.oop)
                    else
                        ex.stackTraceToString()
                }"
            )
        }
    }

    // Android 14+ doesn't allow loading writable dex files: https://developer.android.com/about/versions/14/behavior-changes-14#safer-dynamic-code-loading
    private fun makeDexReadOnlyIfNeeded(dexFile: File): File {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return dexFile
        }
        val target = requireContext().cacheDir.resolve(dexFile.name)
        if (target.exists()) {
            target.delete()
        }
        target.createNewFile()
        dexFile.inputStream().buffered().use {
            target.writeBytes(it.readBytes())
        }
        target.setReadOnly() // This is required for Android 14+
        return target
    }

    override fun onDestroy() {
        ssvm.release()
        super.onDestroy()
    }
}
