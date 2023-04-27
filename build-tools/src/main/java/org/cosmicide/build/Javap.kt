package org.cosmicide.build

import com.sun.tools.javap.JavapTask
import java.io.OutputStream

class Javap {
    fun disassemble(classPath: String): String {
        val args = arrayOf("-c", "-l", "-constants", "-verbose", classPath)
        val stream = object : OutputStream() {
            private val builder = StringBuilder()
            override fun write(b: Int) {
                builder.append(b.toChar())
            }

            override fun toString(): String {
                return builder.toString()
            }
        }
        val task = JavapTask().apply {
            handleOptions(args)
            setLog(stream)
        }
        task.run()
        return stream.toString()

    }
}