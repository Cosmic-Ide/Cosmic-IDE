package io.github.rosemoe.sora.util;


import com.pranav.common.util.DiagnosticWrapper;

import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.SpansUtils;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.widget.CodeEditor;

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
        diagnostics.forEach(
                it -> {
                    int startLine;
                    int startColumn;
                    int endLine;
                    int endColumn;
                    if (it.getPosition() != DiagnosticWrapper.USE_LINE_POS) {
                        if (it.getStartPosition() == -1) {
                            it.setStartPosition(it.getPosition());
                        }
                        if (it.getEndPosition() == -1) {
                            it.setEndPosition(it.getPosition());
                        }

                        if (it.getStartPosition() > editor.getText().length()) {
                            return;
                        }
                        if (it.getEndPosition() > editor.getText().length()) {
                            return;
                        }
                        var start =
                                editor.getCursor()
                                        .getIndexer()
                                        .getCharPosition((int) it.getStartPosition());
                        var end =
                                editor.getCursor()
                                        .getIndexer()
                                        .getCharPosition((int) it.getEndPosition());

                        int sLine = start.getLine();
                        int sColumn = start.getColumn();
                        int eLine = end.getLine();
                        int eColumn = end.getColumn();

                        // the editor does not support marking underline spans for the same
                        // start and end
                        // index
                        // to work around this, we just subtract one to the start index
                        if (sLine == eLine && eColumn == sColumn) {
                            sColumn--;
                            eColumn++;
                        }

                        it.setStartLine(sLine);
                        it.setEndLine(eLine);
                        it.setStartColumn(sColumn);
                        it.setEndColumn(eColumn);
                    }
                    startLine = it.getStartLine();
                    startColumn = it.getStartColumn();
                    endLine = it.getEndLine();
                    endColumn = it.getEndColumn();

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
}
