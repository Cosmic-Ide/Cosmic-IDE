package org.cosmic.ide.android.task.exec

import android.content.SharedPreferences
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import org.cosmic.ide.android.interfaces.Task
import org.cosmic.ide.common.util.CoroutineUtil
import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.common.util.MultipleDexClassLoader
import org.cosmic.ide.project.JavaProject
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.io.InputStream
import java.lang.reflect.Modifier
import java.nio.file.Paths

class ExecuteDexTask(
    val prefs: SharedPreferences,
    val clazz: String,
    val inputStream: InputStream,
    val outputStream: PrintStream,
    val errorStream: PrintStream
) : Task {

    private var result: Any? = null

    override fun getTaskName(): String {
        return "Execute Java Task"
    }

    /*
     * Runs the main method of the program by loading it through
     * PathClassLoader
     */
    @Throws(Exception::class)
    override fun doFullTask(project: JavaProject) {
        val defaultIn = System.`in`
        val defaultOut = System.`out`
        val defaultErr = System.err
        val dexFile = project.getBinDirPath() + "classes.dex"
        System.setOut(outputStream)
        System.setErr(errorStream)
        System.setIn(inputStream)

        // Load the dex file into a ClassLoader
        val dexLoader = MultipleDexClassLoader()

        dexLoader.loadDex(dexFile)

        val libs = File(project.getLibDirPath()).listFiles()
        if (libs != null) {
            // check if all libs have been pre-dexed or not
            for (lib in libs) {
                val outDex = project.getBuildDirPath() + lib.getName().replaceAfterLast('.', "dex")

                if (!File(outDex).exists()) {
                    CoroutineUtil.inParallel {
                        D8.run(
                            D8Command.builder()
                                .setOutput(Paths.get(project.getBuildDirPath()), OutputMode.DexIndexed)
                                .addLibraryFiles(Paths.get(FileUtil.getClasspathDir(), "android.jar"))
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

        val loader = dexLoader.loadDex(FileUtil.getClasspathDir() + "kotlin-stdlib-1.7.20-Beta.jar")

        val calledClass = loader.loadClass(clazz)

        val method = calledClass.getDeclaredMethod("main", Array<String>::class.java)

        val args = prefs.getString("key_program_arguments", "")!!.trim()

        // Split arguments into an array
        val param = args.split("\\s+").toTypedArray()

        Thread {

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
                System.out.println(result.toString())
            } 
            System.setOut(defaultOut)
            System.setErr(defaultErr)
            System.setIn(defaultIn)
        }.start()
    }
}
