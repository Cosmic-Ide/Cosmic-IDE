package com.pranav.lib_android.code.formatter

import com.pranav.lib_android.util.ConcurrentUtil
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter
import org.eclipse.jface.text.Document

class Formatter(javaSource: String) {
	
	val source: String
	
	init {
		source = javaSource
	}
	
	fun format(): String {
		val options = DefaultCodeFormatterOptions.getEclipseDefaultSettings()
		
		val codeFormatter = DefaultCodeFormatter(options)
		
		val edit = codeFormatter.format(
	    	DefaultCodeFormatter.K_COMPILATION_UNIT,
	    	source,
    		0, // starting index
    		source.length(), // length
    		0, // initial indentation
	    	System.lineSeparator() // line separator
		)
		
		val document = Document(source)
		ConcurrentUtil.execute({
			try {
				edit.apply(document)
			} catch (e: Exception) {
				throw IllegalStateException(e)
			}
		})
		// return the formatted code
		return document.get()
	}
}
