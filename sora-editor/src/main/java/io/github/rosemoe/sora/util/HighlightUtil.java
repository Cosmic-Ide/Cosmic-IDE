package io.github.rosemoe.sora.util;


import com.pranav.common.util.DiagnosticWrapper;

import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.SpansUtils;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.text.LineNumberCalculator;

import java.util.ArrayList;
import java.util.List;

import javax.tools.Diagnostic;

public class HighlightUtil {

    /**
     * Highlights the list of given diagnostics, taking care of conversion between 1-based offsets
     * to 0-based offsets. It also makes the Diagnostic eligible for shifting as the user types.
     */
    public static void markDiagnostics(
            CodeEditor editor, List<DiagnosticWrapper> diagnostics, Styles styles) {
        final var content = editor.getText().toString();
        diagnostics.forEach(
                it -> {
                    setLineAndColumn(it, content);
                    int startLine = it.getStartLine();
                    int startColumn = it.getStartColumn();
                    int endLine = it.getEndLine();
                    int endColumn = it.getEndColumn();

                    int flag =
                            it.getKind() == Diagnostic.Kind.ERROR
                                    ? Span.FLAG_ERROR
                                    : Span.FLAG_WARNING;
                    SpansUtils.markProblemRegion(
                            styles.getSpans(), flag, startLine, startColumn, endLine, endColumn);
                });
        editor.setStyles(editor.getEditorLanguage().getAnalyzeManager(), styles);
    }

    public static void clearDiagnostics(Styles styles) {
        var spans = styles.getSpans();
        var read = spans.read();
        for (int i = 0; i < spans.getLineCount(); i++) {
            List<Span> original;
            try {
                original = read.getSpansOnLine(i);
            } catch (NullPointerException e) {
                continue;
            }
            var spansOnLine = new ArrayList<Span>(original);
            for (var span : spansOnLine) {
                span.problemFlags = 0;
            }
            spans.modify().setSpansOnLine(i, spansOnLine);
        }
    }

    private static void setLineAndColumn(DiagnosticWrapper diagnostic, String content) {
        try {
            // Calculate and update the start and end line number and columns
            var startCalculator = new LineNumberCalculator(content);
            startCalculator.update((int) diagnostic.getLineNumber());
            diagnostic.setStartLine((int) diagnostic.getLineNumber());
            diagnostic.setStartColumn(startCalculator.getColumn());
            var endCalculator = new LineNumberCalculator(content);
            endCalculator.update((int) diagnostic.getLineNumber());
            diagnostic.setEndLine((int) diagnostic.getLineNumber());
            diagnostic.setEndColumn(endCalculator.getColumn());
        } catch (IndexOutOfBoundsException ignored) {
            // unknown index, dont update line numbers
        }
    }
}
