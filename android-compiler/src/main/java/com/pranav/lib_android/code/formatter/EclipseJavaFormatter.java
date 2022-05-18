package com.pranav.lib_android.code.formatter;

import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.eclipse.jface.text.Document;

public class EclipseJavaFormatter {

    private String source;

    public EclipseJavaFormatter(String source) {
        this.source = source;
    }

    public String format() {
        var options = DefaultCodeFormatterOptions.getEclipseDefaultSettings();

        var codeFormatter = new DefaultCodeFormatter(options);

        var edit =
                codeFormatter.format(
                        DefaultCodeFormatter.K_COMPILATION_UNIT,
                        source,
                        0, // starting index
                        source.length(), // length
                        0, // initial indentation
                        System.lineSeparator() // line separator
                        );

        var document = new Document(source);
        try {
            edit.apply(document);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        // return the formatted code
        return document.get();
    }
}
