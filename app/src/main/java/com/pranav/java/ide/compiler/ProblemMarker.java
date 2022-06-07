package com.pranav.java.ide.compiler;

import com.pranav.android.analyzer.JavacAnalyzer;
import com.pranav.common.util.ConcurrentUtil;
import com.pranav.common.util.DiagnosticWrapper;

import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.Spans;
import io.github.rosemoe.sora.text.LineNumberCalculator;
import io.github.rosemoe.sora.widget.CodeEditor;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

public class ProblemMarker {

    private CodeEditor editor;
    private JavacAnalyzer analyzer;

    public ProblemMarker(CodeEditor editor) {
        this.editor = editor;
        this.analyzer = new JavacAnalyzer(editor.getContext());
    }

    public void run() {
        ConcurrentUtil.execute(() -> {
        if (!analyzer.isFirstRun()) {
            analyzer.reset();
        }
        try {
            analyzer.analyze();
            HighlightUtil.markDiagnostics(editor, analyzer.getDiagnostics(), editor.getStyles());
        } catch (Exception e) {
            e.printStackTrace();
        }
        });
    }

    private void setLineAndColumn(DiagnosticWrapper diagnostic) {
        try {
            // Calculate and update the start and end line number and columns
            var startCalculator = new LineNumberCalculator(editor.getText().toString());
            startCalculator.update(diagnostic.getStartLine());
            diagnostic.setStartLine(startCalculator.getLine());
            diagnostic.setStartColumn(startCalculator.getColumn());
            var endCalculator = new LineNumberCalculator(editor.getText().toString());
            endCalculator.update(diagnostic.getEndLine());
            diagnostic.setEndLine(endCalculator.getLine());
            diagnostic.setEndColumn(endCalculator.getColumn());
        } catch (IndexOutOfBoundsException ignored) {
            // unknown index, dont update line numbers
        }
    }
}
