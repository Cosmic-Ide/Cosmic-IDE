package com.pranav;

import com.pranav.analyzer.java.JavacAnalyzer;
import com.pranav.common.Indexer;
import com.pranav.common.util.ConcurrentUtil;
import com.pranav.common.util.DiagnosticWrapper;
import com.pranav.common.util.FileUtil;

import io.github.rosemoe.sora.text.*;
import io.github.rosemoe.sora.lang.diagnostic.*;
import io.github.rosemoe.sora.widget.CodeEditor;

public class ProblemMarker implements ContentListener {

    private CodeEditor editor;
    private JavacAnalyzer analyzer;
    private DiagnosticsContainer diagnostics = new DiagnosticsContainer();

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
            CharSequence insertedContent) {
        run(content);
    }

    @Override
    public void afterDelete(
            Content content,
            int startLine,
            int startColumn,
            int endLine,
            int endColumn,
            CharSequence deletedContent) {
        run(content);
    }

    private void run(Content content) {
        ConcurrentUtil.inParallel(
                () -> {
                    if (!analyzer.isFirstRun()) {
                        analyzer.reset();
                    }
                    try {
                        var path = new Indexer("editor").getString("currentFile");
                        var name = FileUtil.getFileName(path);
                        analyzer.analyze(name, content.toString());
                    } catch (Exception ignored) {
                        // we shouldn't disturb the user for some issues
                    }
                    diagnostics.reset();
                    diagnostics.addDiagnostics(analyzer.getDiagnostics());
                    editor.setDiagnostics(diagnostics);
                });
    }
}
