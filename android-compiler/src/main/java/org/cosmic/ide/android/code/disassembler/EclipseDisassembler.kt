package org.cosmic.ide.android.code.disassembler

import org.eclipse.jdt.core.util.ClassFileBytesDisassembler
import org.eclipse.jdt.internal.core.util.Disassembler
import java.nio.file.Files
import java.nio.file.Paths

class EclipseDisassembler(filePath: String) {

    private var classFileBytes: ByteArray

    init {
        classFileBytes = Files.readAllBytes(Paths.get(filePath))
    }

    @Throws(Throwable::class)
    fun disassemble(): String {
        return Disassembler().disassemble(classFileBytes, System.lineSeparator(), ClassFileBytesDisassembler.SYSTEM)
    }
}
