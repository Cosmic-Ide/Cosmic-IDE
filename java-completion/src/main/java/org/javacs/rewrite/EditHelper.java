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
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

import java.util.List;
import java.util.StringJoiner;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;

class EditHelper {
    final JavacTask task;

    EditHelper(JavacTask task) {
        this.task = task;
    }

    static String printMethod(final ExecutableElement method, final ExecutableType parameterizedType, MethodTree source) {
        StringBuilder buf = new StringBuilder();
        // TODO leading \n is extra, but needed for indent replaceAll trick
        buf.append("\n@Override\n");
        if (method.getModifiers().contains(Modifier.PUBLIC)) {
            buf.append("public ");
        }
        if (method.getModifiers().contains(Modifier.PROTECTED)) {
            buf.append("protected ");
        }
        buf.append(EditHelper.printType(parameterizedType.getReturnType())).append(" ");
        buf.append(method.getSimpleName()).append("(");
        buf.append(printParameters(parameterizedType, source));
        buf.append(") {\n    // TODO\n}");
        return buf.toString();
    }

    private static String printParameters(final ExecutableType method, final MethodTree source) {
        StringJoiner join = new StringJoiner(", ");
        for (int i = 0; i < method.getParameterTypes().size(); i++) {
            String type = EditHelper.printType(method.getParameterTypes().get(i));
            Name name = source.getParameters().get(i).getName();
            join.add(type + " " + name);
        }
        return join.toString();
    }

    static String printType(final TypeMirror type) {
        if (type instanceof DeclaredType) {
            DeclaredType declared = (DeclaredType) type;
            String string = printTypeName((TypeElement) declared.asElement());
            if (!declared.getTypeArguments().isEmpty()) {
                string = string + "<" + printTypeParameters(declared.getTypeArguments()) + ">";
            }
            return string;
        } else if (type instanceof ArrayType) {
            ArrayType array = (ArrayType) type;
            return printType(array.getComponentType()) + "[]";
        } else {
            return type.toString();
        }
    }

    private static String printTypeParameters(final List<? extends TypeMirror> arguments) {
        StringJoiner join = new StringJoiner(", ");
        for (TypeMirror a : arguments) {
            join.add(printType(a));
        }
        return join.toString();
    }

    static String printTypeName(final TypeElement type) {
        if (type.getEnclosingElement() instanceof TypeElement) {
            return printTypeName((TypeElement) type.getEnclosingElement()) + "." + type.getSimpleName();
        }
        return type.getSimpleName().toString();
    }

    static int indent(final JavacTask task, final CompilationUnitTree root, final ClassTree leaf) {
        SourcePositions pos = Trees.instance(task).getSourcePositions();
        LineMap lines = root.getLineMap();
        long startClass = pos.getStartPosition(root, leaf);
        long startLine = lines.getStartPosition(lines.getLineNumber(startClass));
        return (int) (startClass - startLine);
    }

    static int indent(final String contents, final int cursor) {
        int indent = 0;
        for (int i = 0; i <= cursor && i < contents.length(); i++) {
            char c = contents.charAt(i);
            if (c == '{') {
                indent++;
            } else if (c == '}') {
                indent--;
            }
        }
        return indent * 4;
    }

    static Position insertBefore(final JavacTask task, final CompilationUnitTree root, final Tree member) {
        SourcePositions pos = Trees.instance(task).getSourcePositions();
        LineMap lines = root.getLineMap();
        long start = pos.getStartPosition(root, member);
        int line = (int) lines.getLineNumber(start);
        return new Position(line - 1, 0);
    }

    static Position insertAfter(final JavacTask task, final CompilationUnitTree root, final Tree member) {
        SourcePositions pos = Trees.instance(task).getSourcePositions();
        LineMap lines = root.getLineMap();
        long end = pos.getEndPosition(root, member);
        int line = (int) lines.getLineNumber(end);
        return new Position(line, 0);
    }

    static Position insertAtEndOfClass(JavacTask task, CompilationUnitTree root, ClassTree leaf) {
        SourcePositions pos = Trees.instance(task).getSourcePositions();
        LineMap lines = root.getLineMap();
        long end = pos.getEndPosition(root, leaf);
        int line = (int) lines.getLineNumber(end);
        int column = (int) lines.getColumnNumber(end);
        return new Position(line - 1, column - 2);
    }

    TextEdit removeTree(final CompilationUnitTree root, final Tree remove) {
        SourcePositions pos = Trees.instance(task).getSourcePositions();
        LineMap lines = root.getLineMap();
        long start = pos.getStartPosition(root, remove);
        long end = pos.getEndPosition(root, remove);
        int startLine = (int) lines.getLineNumber(start);
        int startColumn = (int) lines.getColumnNumber(start);
        Position startPos = new Position(startLine - 1, startColumn - 1);
        int endLine = (int) lines.getLineNumber(end);
        int endColumn = (int) lines.getColumnNumber(end);
        Position endPos = new Position(endLine - 1, endColumn - 1);
        Range range = new Range(startPos, endPos);
        return new TextEdit(range, "");
    }
}
