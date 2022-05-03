package com.pranav.lib_android.code.disassembler;

import kotlin.io.FilesKt;

import org.eclipse.jdt.internal.core.util.Disassembler;

import java.io.File;
import java.io.IOException;

public class EclipseDisassembler {

    final byte[] classFileBytes;

    public EclipseDisassembler(String classFile) throws IOException {
        classFileBytes = FilesKt.readBytes(new File(classFile));
    }

    public String disassemble() throws Throwable {
        return new Disassembler().disassemble(classFileBytes, System.lineSeparator());
    }
}
