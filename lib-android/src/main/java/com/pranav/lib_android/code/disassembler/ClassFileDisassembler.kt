package com.pranav.ide.code.disassembler

import com.google.common.io.Files

import java.io.File
import java.io.IOException

import org.eclipse.jdt.internal.core.util.Disassembler
import org.eclipse.jdt.core.util.ClassFormatException

class ClassFileDisassembler {
    
    lateinit val classFileBytes: byte[]
    
    constructor(classFile: String): ClassFileDisassembler {
		classFileBytes = Files.asByteSource(File(classFile)).read()
    }
	
	fun disassemble(): String {
		return Disassembler().disassemble(classFileBytes, System.lineSeparator())
	}
}
