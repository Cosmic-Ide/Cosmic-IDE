package org.cosmic.ide.android.task.exec

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import org.cosmic.ide.android.interfaces.Task
import org.cosmic.ide.android.task.dex.D8Task
import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.common.util.CoroutineUtil
import org.cosmic.ide.common.util.MultipleDexClassLoader
import org.cosmic.ide.project.Project
import java.io.File
import java.io.InputStream
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier

class ExecuteDexTask(
    private val prefs: SharedPreferences,
    private val clazz: String,
    private val inputStream: InputStream,
    private val outputStream: PrintStream,
    private val errorStream: PrintStream,
    private var postRunnable: Runnable?
) : Task {

    private var result: Any? = null
    private val sysIn = System.`in`
    private val sysOut = System.`out`
    private val sysErr = System.err

    fun release() {
        postRunnable = null

        System.setIn(sysIn)
        System.setOut(sysOut)
        System.setErr(sysErr)
    }

    /**
     * Runs the main method of the program by loading it through
     * PathClassLoader.
     *
     * @property [project] A project that contains dex classes.
     */
    override fun doFullTask(project: Project) {
        val dexFile = project.binDirPath + "classes.dex"

        System.setOut(outputStream)
        System.setErr(errorStream)
        System.setIn(inputStream)

        // Load the dex file into a [MultipleDexClassLoader]
        val dexLoader = MultipleDexClassLoader()

        dexLoader.loadDex(dexFile)
        dexLoader.loadDex(FileUtil.getClasspathDir() + "kotlin-stdlib-1.8.0.dex")

        // TODO: Move to D8Task
        val folder = File(project.libDirPath)
        if (folder.exists() && folder.isDirectory) {
            val libs = folder.listFiles()
            if (libs != null) {
                // Check if all libs have been pre-dexed or not
                for (lib in libs) {
                    val outDex = project.buildDirPath + "libs/" + lib.nameWithoutExtension + ".dex"

                    if (lib.extension == "jar" && !File(outDex).exists()) {
                        CoroutineUtil.execute {
                            D8Task.compileJar(lib.absolutePath)
                            File(project.libDirPath, "classes.dex").renameTo(File(outDex))
                        }
                    }
                    // load library into ClassLoader
                    dexLoader.loadDex(outDex)
                }
            }
        }

        val args = prefs.getString("program_arguments", "")!!.trim()

        // Split arguments into an array
        val param = args.split("\\s+").toTypedArray()

        CoroutineUtil.inParallel {
            try {
                val calledClass = dexLoader.loader.loadClass(clazz)

                val method = calledClass.getDeclaredMethod("main", Array<String>::class.java)
                val modifiers = method.modifiers
                if (Modifier.isStatic(modifiers)) {
                    // If the method is static, directly call it
                    result = method.invoke(null, param as? Any)
                } else if (Modifier.isPublic(modifiers)) {
                    // If the method is public, try to create an instance of the class,
                    // and then call it on the instance
                    val classInstance = calledClass.getConstructor().newInstance()
                    result = method.invoke(classInstance, param as? Any)
                }
                // Print the value of the method if it's not null
                if (result != null) {
                    println(result.toString())
                }
            } catch (e: InvocationTargetException) {
                e.targetException.printStackTrace(errorStream)
            } catch (e: Throwable) {
                e.printStackTrace(errorStream)
            } catch (e: Error) {
                e.printStackTrace(errorStream)
            }
            Handler(Looper.getMainLooper()).post(postRunnable!!)
        }
    }
}
