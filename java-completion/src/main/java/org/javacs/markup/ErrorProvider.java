/************************************************************************************
 * This file is part of Java Language Server (https://github.com/itsaky/java-language-server)
 *
 * Copyright (C) 2021 Akash Yadav
 *
 * Java Language Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Java Language Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Java Language Server.  If not, see <https://www.gnu.org/licenses/>.
 *
 **************************************************************************************/

package org.javacs.markup;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DiagnosticTag;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.javacs.CompileTask;
import org.javacs.FileStore;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;

public class ErrorProvider {
    private final CompileTask task;
    private final CancelChecker checker;

    public ErrorProvider(CompileTask task, CancelChecker checker) {
        this.task = task;
        this.checker = checker;
    }

    public PublishDiagnosticsParams[] errors() {
        try {
            return errorsInternal();
        } catch (Throwable th) {
            return new PublishDiagnosticsParams[0];
        }
    }

    private final PublishDiagnosticsParams[] errorsInternal() {
        if (!isTaskValid(task)) return new PublishDiagnosticsParams[0];
        var result = new PublishDiagnosticsParams[task.roots.size()];
        for (var i = 0; i < task.roots.size(); i++) {
            var root = task.roots.get(i);
            var uri = root.getSourceFile().toUri().toString();
            var diagnostics = new ArrayList<org.eclipse.lsp4j.Diagnostic>();
            diagnostics.addAll(compilerErrors(root));
            diagnostics.addAll(visitForDiagnostics(root));

            result[i] = new PublishDiagnosticsParams();
            result[i].setUri(uri);
            result[i].setDiagnostics(diagnostics);
        }
        // TODO hint fields that could be final

        return result;
    }

    private boolean isTaskValid(CompileTask task) {
        return task != null && task.task != null && task.roots != null;
    }

    private List<org.eclipse.lsp4j.Diagnostic> compilerErrors(CompilationUnitTree root) {
        var result = new ArrayList<org.eclipse.lsp4j.Diagnostic>();
        for (var d : task.diagnostics) {
            if (d.getSource() == null || !d.getSource().toUri().equals(root.getSourceFile().toUri()))
                continue;
            if (d.getStartPosition() == -1 || d.getEndPosition() == -1) continue;
            result.add(lspDiagnostic(d, root.getLineMap()));
        }
        return result;
    }

    private List<org.eclipse.lsp4j.Diagnostic> visitForDiagnostics(CompilationUnitTree root) {
        var result = new ArrayList<org.eclipse.lsp4j.Diagnostic>();
        if (task == null || task.task == null) {
            // Cannot provide any diagnostics with a null task
            return result;
        }
        var notThrown = new HashMap<TreePath, String>();
        var warnUnused = new DiagnosticVisitor(task.task, checker);
        warnUnused.scan(root, notThrown);
        for (var unusedEl : warnUnused.notUsed()) {
            result.add(warnUnused(unusedEl));
        }
        for (var location : notThrown.keySet()) {
            result.add(warnNotThrown(notThrown.get(location), location));
        }
        return result;
    }

    /**
     * lspDiagnostic(d, lines) converts d to LSP format, with its position shifted appropriately for the latest version
     * of the file.
     */
    private org.eclipse.lsp4j.Diagnostic lspDiagnostic(javax.tools.Diagnostic<? extends JavaFileObject> d, LineMap lines) {
        var start = d.getStartPosition();
        var end = d.getEndPosition();
        var startLine = (int) lines.getLineNumber(start);
        var startColumn = (int) lines.getColumnNumber(start);
        var endLine = (int) lines.getLineNumber(end);
        var endColumn = (int) lines.getColumnNumber(end);
        var severity = severity(d.getKind());
        var code = d.getCode();
        var message = d.getMessage(null);
        var result = new org.eclipse.lsp4j.Diagnostic();
        result.setSeverity(severity);
        result.setCode(code);
        result.setMessage(message);
        result.setRange(new Range(new Position(startLine - 1, startColumn - 1), new Position(endLine - 1, endColumn - 1)));
        return result;
    }

    private DiagnosticSeverity severity(javax.tools.Diagnostic.Kind kind) {
        switch (kind) {
            case ERROR:
                return DiagnosticSeverity.Error;
            case WARNING:
            case MANDATORY_WARNING:
                return DiagnosticSeverity.Warning;
            case NOTE:
                return DiagnosticSeverity.Information;
            case OTHER:
            default:
                return DiagnosticSeverity.Hint;
        }
    }

    private org.eclipse.lsp4j.Diagnostic warnNotThrown(String name, TreePath path) {
        checker.checkCanceled();
        var trees = Trees.instance(task.task);
        var pos = trees.getSourcePositions();
        var root = path.getCompilationUnit();
        var start = pos.getStartPosition(root, path.getLeaf());
        var end = pos.getEndPosition(root, path.getLeaf());
        checker.checkCanceled();
        var d = new org.eclipse.lsp4j.Diagnostic();
        d.setMessage(String.format("'%s' is not thrown in the body of the method", name));
        d.setRange(RangeHelper.range(root, start, end));
        d.setCode("unused_throws");
        d.setSeverity(DiagnosticSeverity.Information);
        d.setTags(List.of(DiagnosticTag.Unnecessary));
        return d;
    }

    private org.eclipse.lsp4j.Diagnostic warnUnused(Element unusedEl) {
        var trees = Trees.instance(task.task);
        var path = trees.getPath(unusedEl);
        if (path == null) {
            throw new RuntimeException(unusedEl + " has no path");
        }
        var root = path.getCompilationUnit();
        var leaf = path.getLeaf();
        var pos = trees.getSourcePositions();
        var start = (int) pos.getStartPosition(root, leaf);
        var end = (int) pos.getEndPosition(root, leaf);
        if (leaf instanceof VariableTree) {
            var v = leaf;
            var offset = (int) pos.getEndPosition(root, v.getType());
            if (offset != -1) {
                start = offset;
            }
        }
        var file = Paths.get(root.getSourceFile().toUri());
        var contents = FileStore.contents(file);
        var name = unusedEl.getSimpleName();
        if (name.contentEquals("<init>")) {
            name = unusedEl.getEnclosingElement().getSimpleName();
        }
        var region = contents.subSequence(start, end);
        var matcher = Pattern.compile("\\b" + name + "\\b").matcher(region);
        if (matcher.find()) {
            start += matcher.start();
            end = start + name.length();
        }
        var message = String.format("'%s' is not used", name);
        String code;
        DiagnosticSeverity severity;
        if (leaf instanceof VariableTree) {
            var parent = path.getParentPath().getLeaf();
            if (parent instanceof MethodTree) {
                code = "unused_param";
                severity = DiagnosticSeverity.Hint;
            } else if (parent instanceof BlockTree) {
                code = "unused_local";
                severity = DiagnosticSeverity.Information;
            } else if (parent instanceof ClassTree) {
                code = "unused_field";
                severity = DiagnosticSeverity.Information;
            } else {
                code = "unused_other";
                severity = DiagnosticSeverity.Hint;
            }
        } else if (leaf instanceof MethodTree) {
            code = "unused_method";
            severity = DiagnosticSeverity.Information;
        } else if (leaf instanceof ClassTree) {
            code = "unused_class";
            severity = DiagnosticSeverity.Information;
        } else {
            code = "unused_other";
            severity = DiagnosticSeverity.Information;
        }
        return lspWarnUnused(severity, code, message, start, end, root);
    }

    private org.eclipse.lsp4j.Diagnostic lspWarnUnused(
            DiagnosticSeverity severity, String code, String message, int start, int end, CompilationUnitTree root) {
        checker.checkCanceled();
        var result = new org.eclipse.lsp4j.Diagnostic();
        result.setSeverity(severity);
        result.setCode(code);
        result.setMessage(message);
        result.setTags(List.of(DiagnosticTag.Unnecessary));
        result.setRange(RangeHelper.range(root, start, end));
        return result;
    }

}
