package org.cosmic.ide.code.decompiler

import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider
import java.io.File
import java.util.jar.JarFile

class FFBytecodeProvider : IBytecodeProvider {

    override fun getBytecode(externalPath: String, internalPath: String?): ByteArray {
        val jar = JarFile(File(externalPath))

        val entry = jar.getJarEntry(internalPath)

        jar.getInputStream(entry).use {
            return it.readBytes()
        }
    }
}
