package org.cosmic.ide.android.code.decompiler

import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences
import java.io.File

class FernFlowerDecompiler {
    private val options = defaultOptions()

    fun decompile(
        className: String,
        classesFile: File
    ): String {
        val bytecodeProvider = FFBytecodeProvider()
        val resultSaver = FFResultSaver(className)

        val logger = object : IFernflowerLogger() {
            override fun writeMessage(p0: String?, p1: Severity?) {}

            override fun writeMessage(p0: String?, p1: Severity?, p2: Throwable?) {
                throw p2!!
            }
        }

        val decompiler = BaseDecompiler(bytecodeProvider, resultSaver, options, logger)
        decompiler.addSource(classesFile)
        decompiler.decompileContext()

        return resultSaver.result.ifEmpty {
            "// Error: Fernflower couldn't decompile $className.\n "
        }
    }

    fun getBanner() = """
            /*
             * Decompiled with Fernflower [ecc675ee43].
             */
        """.trimIndent() + "\n"

    private fun defaultOptions() = mapOf(
        IFernflowerPreferences.HIDE_DEFAULT_CONSTRUCTOR to "0",
        IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES to "1",
        IFernflowerPreferences.REMOVE_SYNTHETIC to "1",
        IFernflowerPreferences.REMOVE_BRIDGE to "1",
        IFernflowerPreferences.NEW_LINE_SEPARATOR to "1",
        IFernflowerPreferences.BANNER to getBanner(),
        IFernflowerPreferences.MAX_PROCESSING_METHOD to 60,
        IFernflowerPreferences.IGNORE_INVALID_BYTECODE to "1",
        IFernflowerPreferences.VERIFY_ANONYMOUS_CLASSES to "1"
    )
}
