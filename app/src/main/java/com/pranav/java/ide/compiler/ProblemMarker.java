package com.pranav.java.ide.compiler;

import com.pranav.android.analyzer.JavacAnalyzer;

import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.SpansUtils;
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
        if (!analyzer.isFirstRun()) {
            analyzer.reset();
        }
        try {
            analyzer.analyze();
            for (Diagnostic<? extends JavaFileObject> diagnostic : analyzer.getDiagnostics()) {
                if (diagnostic.getSource() == null) {
                    return;
                }
                var wrapper = new DiagnosticWrapper(diagnostic);
                setLineAndColumn(wrapper);
                int flag;
                switch (wrapper.getKind()) {
                    case NOTE:
                    case WARNING:
                    case MANDATORY_WARNING:
                        if (wrapper.getCode().contains("deprecated")) {
                            flag = Span.FLAG_DEPRECATED;
                        } else {
                            flag = Span.FLAG_WARNING;
                        }
                        break;
                    case ERROR:
                        flag = Span.FLAG_ERROR;
                        break;
                    default:
                        flag = Span.FLAG_ERROR;
                        break;
                }
                SpansUtils.markProblemRegion(
                        editor.getStyles().getSpans(),
                        flag,
                        wrapper.getStartLine(),
                        wrapper.getStartColumn(),
                        wrapper.getEndLine(),
                        wrapper.getEndColumn());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setLineAndColumn(DiagnosticWrapper diagnostic) {
        try {
            // Calculate and update the start and end line number and columns
            var lineCalculator = new LineNumberCalculator(editor.getText().toString());
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
