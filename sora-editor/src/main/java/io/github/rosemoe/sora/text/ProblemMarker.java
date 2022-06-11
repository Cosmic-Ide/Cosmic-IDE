package io.github.rosemoe.sora.text;

import com.pranav.analyzer.java.JavacAnalyzer;
import com.pranav.common.util.ConcurrentUtil;
import com.pranav.common.util.DiagnosticWrapper;
import com.pranav.common.Indexer;
import com.pranav.common.util.FileUtil;

import io.github.rosemoe.sora.text.LineNumberCalculator;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.util.HighlightUtil;

import java.io.IOException;

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
      run(content);
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
      run(content);
    }
    
    
    private void run(Content content) {
        ConcurrentUtil.inParallel(
                () -> {
                    if (!analyzer.isFirstRun()) {
                        analyzer.reset();
                    }
                    try {
                        FileUtil.writeFile(new Indexer("editor").getString("currentFile"), content.toString());
                        analyzer.analyze();
                    } catch (Exception ignored) {
                        // we shouldn't disturb the user for some issues
                    }
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
