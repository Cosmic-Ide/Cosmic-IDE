package org.cosmic.ide.android.task.exec

import android.content.Context
import android.content.SharedPreferences

import dalvik.system.PathClassLoader

import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode

import org.cosmic.ide.android.interfaces.*
import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.common.util.MultipleDexClassLoader
import org.cosmic.ide.project.JavaProject

import java.io.OutputStream
import java.io.PrintStream
import java.io.File
import java.nio.file.Paths
import java.lang.reflect.Modifier

class ExecuteDexTask(preferences: SharedPreferences, claz: String) : Task {

    private val clazz: String
    private var result: Any? = null
    private val log = StringBuilder()
    private val prefs: SharedPreferences

    init {
        clazz = claz
        prefs = preferences
    }

    override fun getTaskName(): String {
        return "Execute Java Task";
    }

    /*
     * Runs the main method pf the program by loading it through
     * PathClassLoader
     */
    @Throws(Exception::class)
    override fun doFullTask(project: JavaProject) {
        val defaultOut = System.out
        val defaultErr = System.err
        val dexFile = project.getBinDirPath() + "classes.dex"
        val out =
                object : OutputStream() {
                    override fun write(b: Int) {
                        log.append(b.toChar())
                    }

                    override fun toString(): String {
                        return log.toString()
                    }
                }
        System.setOut(PrintStream(out))
        System.setErr(PrintStream(out))

        // Load the dex file into a ClassLoader
        val dexLoader = MultipleDexClassLoader()

        dexLoader.loadDex(dexFile)

        val libs = File(project.getLibDirPath()).listFiles()
        if (libs != null) {
            // check if all libs have been pre-dexed or not
            for (lib in libs) {
                val outDex = project.getBuildDirPath() + lib.getName().replace(".jar", ".dex")

                if (!File(outDex).exists()) {
                    D8.run(
                        D8Command.builder()
                                .setOutput(Paths.get(project.getBuildDirPath()), OutputMode.DexIndexed)
                                .addLibraryFiles(Paths.get(FileUtil.getClasspathDir(), "android.jar"))
                                .addProgramFiles(lib.toPath())
                                .build()
                    )
                    File(project.getBuildDirPath(), "classes.dex").renameTo(File(outDex))
                }
                // load library into ClassLoader
                dexLoader.loadDex(outDex)
            }
        }

        val loader = dexLoader.loadDex(FileUtil.getClasspathDir() + "kotlin-stdlib-1.7.10.jar")

        val calledClass = loader.loadClass(clazz)

        val method = calledClass.getDeclaredMethod("main", Array<String>::class.java)

        val args = prefs.getString("program_arguments", "")!!.trim()

        // Split argument into an array
        val param = args.split("\\s+").toTypedArray()

        if (Modifier.isStatic(method.getModifiers())) {
            // If the method is static, directly call it
            result = method.invoke(null, param as? Any)
        } else if (Modifier.isPublic(method.getModifiers())) {
            // If the method is public, create an instance of the class,
            // and then call it on the instance
            val classInstance = calledClass.getConstructor().newInstance()
            result = method.invoke(classInstance, param as? Any)
        }
        if (result != null) {
            log.append(result.toString())
        }
        System.setOut(defaultOut)
        System.setErr(defaultErr)
    }

    /*
     * Returns all the system logs recorded while executing the method
     */
    fun getLogs(): String {
        return log.toString()
    }
}
