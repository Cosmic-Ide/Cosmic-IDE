package com.pranav.java.ide.compiler;

import com.pranav.android.analyzer.JavacAnalyzer;
import com.pranav.common.util.ConcurrentUtil;
import com.pranav.common.util.DiagnosticWrapper;
import com.pranav.java.ide.HighlightUtil;

import io.github.rosemoe.sora.text.LineNumberCalculator;
import io.github.rosemoe.sora.widget.CodeEditor;

import android.widget.Toast;

public class ProblemMarker {

    private CodeEditor editor;
    private JavacAnalyzer analyzer;

    public ProblemMarker(CodeEditor editor) {
        this.editor = editor;
        this.analyzer = new JavacAnalyzer(editor.getContext());
    }

    public void run() {
        ConcurrentUtil.inParallel(
                () -> {
                    if (!analyzer.isFirstRun()) {
                        analyzer.reset();
                    }
                    try {
                        analyzer.analyze();
                        HighlightUtil.clearSpans(editor.getStyles());
                        HighlightUtil.markDiagnostics(
                                editor, analyzer.getDiagnostics(), editor.getStyles());
                        Toast.makeText(editor.getContext(), "markDiagnostics call completed", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private void setLineAndColumn(DiagnosticWrapper diagnostic) {
        try {
            // Calculate and update the start and end line number and columns
            var startCalculator = new LineNumberCalculator(editor.getText().toString());
            startCalculator.update((int) diagnostic.getLineNumber());
            diagnostic.setStartLine((int) diagnostic.getLineNumber());
            diagnostic.setStartColumn(startCalculator.getColumn());
            var endCalculator = new LineNumberCalculator(editor.getText().toString());
            endCalculator.update((int) diagnostic.getLineNumber());
            diagnostic.setEndLine((int) diagnostic.getLineNumber());
            diagnostic.setEndColumn(endCalculator.getColumn());
        } catch (IndexOutOfBoundsException ignored) {
            // unknown index, dont update line numbers
        }
    }
}
