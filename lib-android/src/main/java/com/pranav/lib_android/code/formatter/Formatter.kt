package com.pranav.ide.code.formatter

import java.util.Map
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.formatter.CodeFormatter
import org.eclipse.jdt.core.ToolFactory
import org.eclipse.text.edits.TextEdit
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.Document

class Formatter {
	
	lateinit val source: String
	
	constructor(source: String) {
		this.source = source
	}
	
	public String format() {
		Map options = DefaultCodeFormatterConstants.getEclipse21Settings()
		
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_7)
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM,
		JavaCore.VERSION_1_7)
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_7)
		options.put(
		DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ENUM_CONSTANTS,
		DefaultCodeFormatterConstants.createAlignmentValue(true,
		DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE,
		DefaultCodeFormatterConstants.INDENT_ON_COLUMN))
		
		options.put(
		DefaultCodeFormatterConstants.FORMATTER_ALIGN_TYPE_MEMBERS_ON_COLUMNS,
		DefaultCodeFormatterConstants.createAlignmentValue(true,
		DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE,
		DefaultCodeFormatterConstants.INDENT_ON_COLUMN))
		
		options.put(
		DefaultCodeFormatterConstants.FORMATTER_ALIGN_FIELDS_GROUPING_BLANK_LINES,
		DefaultCodeFormatterConstants.createAlignmentValue(true,
		DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE,
		DefaultCodeFormatterConstants.INDENT_ON_COLUMN))
		
		options.put(
		DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_IMPORTS,
		"1")
		
		options.put(
		DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_IMPORTS,
		"1")
		
		options.put(
		DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_JAVADOC_COMMENT,
		DefaultCodeFormatterConstants.TRUE)
		
		options.put(
		DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_BLOCK_COMMENT,
		DefaultCodeFormatterConstants.TRUE)
		
		options.put(
		DefaultCodeFormatterConstants.FORMATTER_COMMENT_PRESERVE_WHITE_SPACE_BETWEEN_CODE_AND_LINE_COMMENT,
		DefaultCodeFormatterConstants.TRUE)
		
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AT_END_OF_FILE_IF_MISSING,
		JavaCore.INSERT)
		
		val codeFormatter = ToolFactory
		.createCodeFormatter(options)
		
		val edit = codeFormatter.format(
		CodeFormatter.K_COMPILATION_UNIT
		| CodeFormatter.F_INCLUDE_COMMENTS,
		source,
		0, // starting index
		source.length(), // length
		0, // initial indentation
		System.lineSeparator() // line separator
		)
		
		final IDocument document = Document(source)
		final CountDownLatch latch = CountDownLatch(1)
		Executors.newSingleThreadExecutor().execute(() -> {
			try {
				edit.apply(document)
			} catch (e: Exception) {
				throw IllegalStateException(e)
			}
			latch.countDown()
		})
		try {
			latch.await()
		} catch (e: InterruptedException) {
			e.printStackTrace()
		}
		// return the formatted code
		return document.get()
	}
}
