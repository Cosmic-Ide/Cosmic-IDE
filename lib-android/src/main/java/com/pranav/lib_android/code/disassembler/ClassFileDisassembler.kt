package com.pranav.lib_android.code.disassembler

import java.io.File

import org.eclipse.jdt.internal.core.util.Disassembler

class ClassFileDisassembler(classFile: String) {

  val classFileBytes: ByteArray

  init {
    classFileBytes = File(classFile).readBytes()
  }
	
	fun disassemble(): String {
		return Disassembler()
		    .disassemble(classFileBytes, System.lineSeparator())
	}
}
