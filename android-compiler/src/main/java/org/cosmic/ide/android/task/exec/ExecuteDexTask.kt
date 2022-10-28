package org.cosmic.ide.android.task.exec

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import org.cosmic.ide.android.interfaces.Task
import org.cosmic.ide.common.util.CoroutineUtil
import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.common.util.MultipleDexClassLoader
import org.cosmic.ide.project.Project
import org.cosmic.ide.CompilerUtil
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.io.InputStream
import java.lang.reflect.Modifier
import java.lang.reflect.InvocationTargetException
import java.nio.file.Paths

class ExecuteDexTask(
    private val prefs: SharedPreferences,
    private val clazz: String,
    private val inputStream: InputStream,
    private val outputStream: PrintStream,
    private val errorStream: PrintStream,
    private var postRunnable: Runnable?
) : Task {

    private var result: Any? = null
    private lateinit var sysIn: InputStream
    private lateinit var sysOut: PrintStream
    private lateinit var sysErr: PrintStream

    override fun getTaskName(): String {
        return "Execute Dex Task"
    }

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
        val dexFile = project.getBinDirPath() + "classes.dex"

        sysIn = System.`in`
        sysOut = System.`out`
        sysErr = System.err

        System.setOut(outputStream)
        System.setErr(errorStream)
        System.setIn(inputStream)

        // Load the dex file into a [MultipleDexClassLoader]
        val dexLoader = MultipleDexClassLoader()

        dexLoader.loadDex(dexFile)

        val libs = File(project.getLibDirPath()).listFiles()
        if (libs != null) {
            // Check if all libs have been pre-dexed or not
            for (lib in libs) {
                val outDex = project.getBuildDirPath() + lib.getName().replaceAfterLast('.', "dex")

                if (!File(outDex).exists()) {
                    CoroutineUtil.inParallel {
                        D8.run(
                            D8Command.builder()
                                .setOutput(Paths.get(project.getBuildDirPath()), OutputMode.DexIndexed)
                                .addClasspathFiles(CompilerUtil.getPlatformPaths())
                                .addProgramFiles(lib.toPath())
                                .build()
                        )
                        File(project.getBuildDirPath(), "classes.dex").renameTo(File(outDex))
                    }
                }
                // load library into ClassLoader
                dexLoader.loadDex(outDex)
            }
        }

        val args = prefs.getString("key_program_arguments", "")!!.trim()

        // Split arguments into an array
        val param = args.split("\\s+").toTypedArray()

        CoroutineUtil.inParallel {
            try {
                val calledClass = dexLoader.loader.loadClass(clazz)

                val method = calledClass.getDeclaredMethod("main", Array<String>::class.java)
                if (Modifier.isStatic(method.getModifiers())) {
                    // If the method is static, directly call it
                    result = method.invoke(null, param as? Any)
                } else if (Modifier.isPublic(method.getModifiers())) {
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
                e.getTargetException().printStackTrace(errorStream)
            } catch (e: Throwable) {
                e.printStackTrace(errorStream)
            } catch (e: Error) {
                e.printStackTrace(errorStream)
            }
            Handler(Looper.getMainLooper()).post(postRunnable!!);
        }
    }
}
