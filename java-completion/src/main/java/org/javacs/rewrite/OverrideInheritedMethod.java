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

import com.sun.source.tree.ClassTree;
import com.sun.source.util.Trees;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.javacs.CompilerProvider;
import org.javacs.FindHelper;
import org.javacs.FindTypeDeclarationAt;
import org.javacs.ParseTask;
import org.javacs.services.JavaLanguageServer;

import java.nio.file.Path;
import java.util.Map;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;

public class OverrideInheritedMethod implements Rewrite {
    final String superClassName, methodName;
    final String[] erasedParameterTypes;
    final Path file;
    final int insertPosition;

    public OverrideInheritedMethod(
            String superClassName, String methodName, String[] erasedParameterTypes, Path file, int insertPosition) {
        this.superClassName = superClassName;
        this.methodName = methodName;
        this.erasedParameterTypes = erasedParameterTypes;
        this.file = file;
        this.insertPosition = insertPosition;
    }

    @Override
    public Map<Path, TextEdit[]> rewrite(CompilerProvider compiler) {
        var insertPoint = insertNearCursor(compiler);
        var insertText = insertText(compiler);
        TextEdit[] edits = {new TextEdit(new Range(insertPoint, insertPoint), insertText)};
        return Map.of(file, edits);
    }

    private String insertText(CompilerProvider compiler) {
        try (var task = compiler.compile(file)) {
            var types = task.task.getTypes();
            var trees = Trees.instance(task.task);
            var superMethod = FindHelper.findMethod(task, superClassName, methodName, erasedParameterTypes);
            var thisTree = new FindTypeDeclarationAt(task.task).scan(task.root(), (long) insertPosition);
            var thisPath = trees.getPath(task.root(), thisTree);
            var thisClass = (TypeElement) trees.getElement(thisPath);
            var parameterizedType = (ExecutableType) types.asMemberOf((DeclaredType) thisClass.asType(), superMethod);
            var indent = EditHelper.indent(task.task, task.root(), thisTree) + 4;
            var sourceFile = compiler.findAnywhere(superClassName);
            if (sourceFile.isEmpty()) return "";
            var parse = compiler.parse(sourceFile.get());
            var source = FindHelper.findMethod(parse, superClassName, methodName, erasedParameterTypes);
            var text = EditHelper.printMethod(superMethod, parameterizedType, source);
            text = text.replaceAll("\n", "\n" + " ".repeat(indent));
            text = text + "\n\n";
            return text;
        }
    }

    private Position insertNearCursor(CompilerProvider compiler) {
        var task = compiler.parse(file);
        var parent = new FindTypeDeclarationAt(task.task).scan(task.root, (long) insertPosition);
        var next = nextMember(task, parent);
        if (next != JavaLanguageServer.Position_NONE) {
            return next;
        }
        return EditHelper.insertAtEndOfClass(task.task, task.root, parent);
    }

    private Position nextMember(ParseTask task, ClassTree parent) {
        var pos = Trees.instance(task.task).getSourcePositions();
        for (var member : parent.getMembers()) {
            var start = pos.getStartPosition(task.root, member);
            if (start > insertPosition) {
                var line = (int) task.root.getLineMap().getLineNumber(start);
                return new Position(line - 1, 0);
            }
        }
        return JavaLanguageServer.Position_NONE;
    }
}
