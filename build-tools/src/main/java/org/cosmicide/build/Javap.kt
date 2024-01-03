/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

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
