package org.cosmic.ide.android.code.formatter

import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions
import org.eclipse.jface.text.Document

class EclipseJavaFormatter(code: String) {

    private val source: String

    init {
        source = code;
    }

    fun format(): String {
        val options = DefaultCodeFormatterOptions.getEclipseDefaultSettings()

        val codeFormatter = DefaultCodeFormatter(options)

        val edit =
                codeFormatter.format(
                        DefaultCodeFormatter.K_COMPILATION_UNIT,
                        source,
                        0, // starting index
                        source.length, // length
                        0, // initial indentation
                        System.lineSeparator() // line separator
                        )

        val document = Document(source)
        try {
            edit.apply(document)
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
        // return the formatted code
        return document.get()
    }
}
