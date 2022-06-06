package com.pranav.java.ide.compiler;

import com.pranav.android.analyzer.JavacAnalyzer;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.SpansUtils;

import java.util.List;

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
              if (wrapper.getCode().startsWith("compiler.note.deprecated")) {
                flag = Span.FLAG_DEPRECATED;
              } else {
              flag = Span.FLAG_WARNING;
              }
              break;
            default:
              flag = Span.FLAG_ERROR;
              break;
          }
          SpansUtils.markProblemRegion(editor.getStyles().getSpans(), flag, wrapper.getStartLine(), wrapper.getStartColumn(), wrapper.getEndLine(), wrapper.getEndColumn());
        }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public static void setLineAndColumn(DiagnosticWrapper diagnostic) {
          try {
              if (diagnostic.getStartLine() <= -1 && diagnostic.getStartPosition() > 0) {
                  var start = editor.getCharPosition(((int) diagnostic.getStartPosition()));
                  diagnostic.setStartLine(start.getLine() + 1);
                  diagnostic.setStartColumn(start.getColumn());
                  diagnostic.setLineNumber(start.getLine() + 1);
                  diagnostic.setColumnNumber(start.getColumn());
              }
              if (diagnostic.getEndLine() <= -1 && diagnostic.getEndPosition() > 0) {
                  var end = editor.getCharPosition(((int) diagnostic.getEndPosition()));
                  diagnostic.setEndLine(end.getLine() + 1);
                  diagnostic.setEndColumn(end.getColumn());
              }
          } catch (IndexOutOfBoundsException ignored) {
              // unknown index, dont display line number
          }
  }
}
