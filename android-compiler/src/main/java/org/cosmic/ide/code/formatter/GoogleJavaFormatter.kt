package org.cosmic.ide.code.formatter

import com.google.googlejavaformat.java.Formatter
import com.google.googlejavaformat.java.JavaFormatterOptions

class GoogleJavaFormatter(
    private val source: String
) {

    fun format(): String {
        val options =
            JavaFormatterOptions.builder()
                .style(JavaFormatterOptions.Style.AOSP) // Use 4 spaces for tab
                .formatJavadoc(true) // Format Javadoc
                .build()
        val formatter = Formatter(options)
        try {
            return formatter.formatSourceAndFixImports(source)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return source
    }
}
