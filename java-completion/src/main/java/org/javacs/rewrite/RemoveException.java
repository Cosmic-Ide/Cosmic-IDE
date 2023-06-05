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

package org.javacs.rewrite;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.javacs.CompilerProvider;
import org.javacs.FindHelper;
import org.javacs.services.JavaLanguageServer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

public class RemoveException implements Rewrite {
    private static final Pattern THROWS = Pattern.compile("\\s*\\bthrows\\b");
    final String className, methodName;
    final String[] erasedParameterTypes;
    final String exceptionType;

    public RemoveException(String className, String methodName, String[] erasedParameterTypes, String exceptionType) {
        this.className = className;
        this.methodName = methodName;
        this.erasedParameterTypes = erasedParameterTypes;
        this.exceptionType = exceptionType;
    }

    @Override
    public Map<Path, TextEdit[]> rewrite(CompilerProvider compiler) {
        var file = compiler.findTypeDeclaration(className);
        try (var task = compiler.compile(file)) {
            var methodElement = FindHelper.findMethod(task, className, methodName, erasedParameterTypes);
            var methodTree = Trees.instance(task.task).getTree(methodElement);
            if (methodTree.getThrows().size() == 1) {
                var delete = removeEntireThrows(task.task, task.root(), methodTree);
                if (delete == JavaLanguageServer.TextEdit_NONE) return CANCELLED;
                TextEdit[] edits = {delete};
                return Map.of(file, edits);
            }
            TextEdit[] edits = {removeSingleException(task.task, task.root(), methodTree)};
            return Map.of(file, edits);
        }
    }

    private TextEdit removeEntireThrows(JavacTask task, CompilationUnitTree root, MethodTree method) {
        var trees = Trees.instance(task);
        var pos = trees.getSourcePositions();
        var startMethod = (int) pos.getStartPosition(root, method);
        CharSequence contents;
        try {
            contents = root.getSourceFile().getCharContent(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var matcher = THROWS.matcher(contents);
        if (!matcher.find(startMethod)) {
            return JavaLanguageServer.TextEdit_NONE;
        }
        var lines = root.getLineMap();
        var start = matcher.start();
        var startLine = (int) lines.getLineNumber(start);
        var startColumn = (int) lines.getColumnNumber(start);
        var startPos = new Position(startLine - 1, startColumn - 1);
        var lastException = method.getThrows().get(method.getThrows().size() - 1);
        var end = (int) pos.getEndPosition(root, lastException);
        var endLine = (int) lines.getLineNumber(end);
        var endColumn = (int) lines.getColumnNumber(end);
        var endPos = new Position(endLine - 1, endColumn - 1);
        return new TextEdit(new Range(startPos, endPos), "");
    }

    private TextEdit removeSingleException(JavacTask task, CompilationUnitTree root, MethodTree method) {
        var i = findNamedException(task, root, method);
        if (i == -1) return JavaLanguageServer.TextEdit_NONE;
        var trees = Trees.instance(task);
        var pos = trees.getSourcePositions();
        var exn = method.getThrows().get(i);
        var start = pos.getStartPosition(root, exn);
        var end = pos.getEndPosition(root, exn);
        if (i == 0) {
            end = removeTrailingComma(root, end);
        } else {
            start = removeLeadingComma(root, start);
        }
        var lines = root.getLineMap();
        var startLine = (int) lines.getLineNumber(start);
        var startColumn = (int) lines.getColumnNumber(start);
        var startPos = new Position(startLine - 1, startColumn - 1);
        var endLine = (int) lines.getLineNumber(end);
        var endColumn = (int) lines.getColumnNumber(end);
        var endPos = new Position(endLine - 1, endColumn - 1);
        return new TextEdit(new Range(startPos, endPos), "");
    }

    private int findNamedException(JavacTask task, CompilationUnitTree root, MethodTree method) {
        var trees = Trees.instance(task);
        for (var i = 0; i < method.getThrows().size(); i++) {
            var e = method.getThrows().get(i);
            var path = trees.getPath(root, e);
            var type = (DeclaredType) trees.getTypeMirror(path);
            var el = (TypeElement) type.asElement();
            if (el.getQualifiedName().contentEquals(exceptionType)) {
                return i;
            }
        }
        return -1;
    }

    private int removeLeadingComma(CompilationUnitTree root, long start) {
        var contents = contents(root);
        for (int i = (int) start; i > 0; i--) {
            if (contents.charAt(i) == ',') {
                return i;
            }
        }
        return -1;
    }

    private int removeTrailingComma(CompilationUnitTree root, long end) {
        var contents = contents(root);
        for (int i = (int) end; i < contents.length(); i++) {
            if (contents.charAt(i) == ',') {
                if (contents.charAt(i + 1) == ' ') {
                    return i + 2;
                } else {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    private CharSequence contents(CompilationUnitTree root) {
        try {
            return root.getSourceFile().getCharContent(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
