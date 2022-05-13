package com.pranav.lib_android.code.disassembler;

import org.eclipse.jdt.internal.core.util.Disassembler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;

public class EclipseDisassembler {

    final byte[] classFileBytes;

    public EclipseDisassembler(String classFile) throws IOException, InvalidPathException {
       
        classFileBytes = Files.readAllBytes(Paths.get(classFile));
    }

    public String disassemble() throws Throwable {
        return new Disassembler().disassemble(classFileBytes, System.lineSeparator());
    }
}
