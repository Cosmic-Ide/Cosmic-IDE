package org.cosmicide.build

import com.sun.tools.javap.JavapTask
import java.io.OutputStream

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
        val outputStream = object : OutputStream() {
            private val stringBuilder = StringBuilder()
            override fun write(b: Int) {
                stringBuilder.append(b.toChar())
            }
            override fun toString(): String {
                return stringBuilder.toString()
            }
        }

        val javapTask = JavapTask().apply {
            handleOptions(args)
            setLog(outputStream)
        }
        javapTask.run()

        return outputStream.toString()
    }
}