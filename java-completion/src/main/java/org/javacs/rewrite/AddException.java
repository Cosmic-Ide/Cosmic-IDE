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

import com.sun.source.util.Trees;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.javacs.CompilerProvider;
import org.javacs.FindHelper;

import java.nio.file.Path;
import java.util.Map;

public class AddException implements Rewrite {
    final String className, methodName;
    final String[] erasedParameterTypes;
    final String exceptionType;

    public AddException(String className, String methodName, String[] erasedParameterTypes, String exceptionType) {
        this.className = className;
        this.methodName = methodName;
        this.erasedParameterTypes = erasedParameterTypes;
        this.exceptionType = exceptionType;
    }

    @Override
    public Map<Path, TextEdit[]> rewrite(CompilerProvider compiler) {
        var file = compiler.findTypeDeclaration(className);
        if (file == CompilerProvider.NOT_FOUND) {
            return CANCELLED;
        }
        try (var task = compiler.compile(file)) {
            var trees = Trees.instance(task.task);
            var methodElement = FindHelper.findMethod(task, className, methodName, erasedParameterTypes);
            var methodTree = trees.getTree(methodElement);
            var pos = trees.getSourcePositions();
            var lines = task.root().getLineMap();
            var startBody = pos.getStartPosition(task.root(), methodTree.getBody());
            var line = (int) lines.getLineNumber(startBody);
            var column = (int) lines.getColumnNumber(startBody);
            var insertPos = new Position(line - 1, column - 1);
            var packageName = "";
            var simpleName = exceptionType;
            var lastDot = simpleName.lastIndexOf('.');
            if (lastDot != -1) {
                packageName = exceptionType.substring(0, lastDot);
                simpleName = exceptionType.substring(lastDot + 1);
            }
            String insertText;
            if (methodTree.getThrows().isEmpty()) {
                insertText = "throws " + simpleName + " ";
            } else {
                insertText = ", " + simpleName + " ";
            }
            var insertThrows = new TextEdit(new Range(insertPos, insertPos), insertText);
            // TODO add import if needed
            TextEdit[] edits = {insertThrows};
            return Map.of(file, edits);
        }
    }
}
