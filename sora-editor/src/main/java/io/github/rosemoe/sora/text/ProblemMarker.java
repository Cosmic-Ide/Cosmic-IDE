package io.github.rosemoe.sora.text;

import com.pranav.analyzer.java.JavacAnalyzer;
import com.pranav.common.util.ConcurrentUtil;
import com.pranav.common.util.DiagnosticWrapper;

import io.github.rosemoe.sora.text.LineNumberCalculator;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.util.HighlightUtil;

public class ProblemMarker implements ContentListener {

    private CodeEditor editor;
    private JavacAnalyzer analyzer;

    public ProblemMarker(CodeEditor editor) {
        this.editor = editor;
        this.analyzer = new JavacAnalyzer(editor.getContext());
    }

    @Override
    public void beforeReplace(Content content) {}
    
    @Override
    public void afterInsert(
                Content content,
                int startLine,
                int startColumn,
                int endLine,
                int endColumn,
                CharSequence insertedContent
    ) {
      run();
    }
    
    @Override
    public void afterDelete(
                Content content,
                int startLine,
                int startColumn,
                int endLine,
                int endColumn,
                CharSequence deletedContent
    ) {
      run();
    }
    
    
    private void run() {
        ConcurrentUtil.inParallel(
                () -> {
                    if (!analyzer.isFirstRun()) {
                        analyzer.reset();
                    }
                    try {
                        analyzer.analyze();
                    } catch (IOException ignored) {}
                        HighlightUtil.clearDiagnostics(editor.getStyles());
                        HighlightUtil.markDiagnostics(
                                editor, analyzer.getDiagnostics(), editor.getStyles());
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
