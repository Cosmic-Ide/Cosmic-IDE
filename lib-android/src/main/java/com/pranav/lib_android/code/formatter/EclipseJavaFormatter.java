package com.pranav.lib_android.code.formatter;

import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

public class EclipseJavaFormatter {

    private String source;

    public EclipseJavaFormatter(String source) {
        this.source = source;
    }

    public String format() {
        DefaultCodeFormatterOptions options =
                DefaultCodeFormatterOptions.getEclipseDefaultSettings();

        final DefaultCodeFormatter codeFormatter = new DefaultCodeFormatter(options);

        final TextEdit edit =
                codeFormatter.format(
                        DefaultCodeFormatter.K_COMPILATION_UNIT,
                        source,
                        0, // starting index
                        source.length(), // length
                        0, // initial indentation
                        System.lineSeparator() // line separator
                        );

        final IDocument document = new Document(source);
        try {
            edit.apply(document);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        // return the formatted code
        return document.get();
    }
}
