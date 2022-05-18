package com.pranav.lib_android.code.formatter;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.JavaFormatterOptions;

public class GoogleJavaFormatter {

    private String source;

    public GoogleJavaFormatter(String source) {
        this.source = source;
    }

    public String format() {
        var options =
                JavaFormatterOptions.builder()
                        .style(JavaFormatterOptions.Style.AOSP) // Use AOSP formatting style
                        .formatJavadoc(true) // Format Javadoc with code
                        .build();
        var formatter = new Formatter(options);
        try {
            return formatter.formatSourceAndFixImports(source);
        } catch (FormatterException e) {
            throw new IllegalStateException(e);
        }
    }
}
