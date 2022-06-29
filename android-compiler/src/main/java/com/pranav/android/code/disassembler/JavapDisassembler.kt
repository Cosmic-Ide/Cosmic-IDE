package com.pranav.android.code.disassembler

import com.sun.tools.javap.JavapTask

import java.io.StringWriter
import java.util.ArrayList

class JavapDisassembler(filePath: String) {

    private val path: String

    init {
        path = filePath
    }

    @Throws(Throwable::class)
    fun disassemble() : String {
        // Create an arraylist for storing javap arguments
        val args = listOf(
                "-c",
                path
        )
        // Create a StringWriter object that will store the output
        val writer = StringWriter()
        // Create a JavapTask to handle the arguments
        val task = JavapTask()
        task.handleOptions(args.toTypedArray())
        task.setLog(writer)
        task.run()
        // return the disassembled file as string
        return writer.toString()
    }
}
