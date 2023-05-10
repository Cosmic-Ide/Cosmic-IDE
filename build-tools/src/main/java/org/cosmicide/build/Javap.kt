package org.cosmicide.build

import com.sun.tools.javap.JavapTask
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * A utility object for disassembling Java class files using the javap tool.
 */
object Javap {

    /**
     * Disassembles the specified Java class file using the javap tool.
     * @param classPath The path to the class file to disassemble.
     * @return A string containing the disassembled code of the class file.
     */
    fun disassemble(classPath: String): String {
        val args = arrayOf("-c", "-l", "-constants", "-verbose", classPath)

        val outputStream = ByteArrayOutputStream()
        val printStream = PrintStream(outputStream)

        val javapTask = JavapTask()
        javapTask.handleOptions(args)
        javapTask.setLog(printStream)
        javapTask.run()

        return outputStream.toString()
    }
}