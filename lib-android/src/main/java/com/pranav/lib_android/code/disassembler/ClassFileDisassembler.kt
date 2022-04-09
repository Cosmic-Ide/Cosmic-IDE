package com.pranav.lib_android.code.disassembler

import com.google.common.io.Files

import java.io.File

import org.eclipse.jdt.internal.core.util.Disassembler

public class ClassFileDisassembler {
    
    var classFileBytes: Byte[]
    
    constructor(classFile: String) {
  		classFileBytes = Files.asByteSource(File(classFile)).read()
    }
	
	fun disassemble(): String {
		return Disassembler().disassemble(classFileBytes, System.lineSeparator())
	}
}
