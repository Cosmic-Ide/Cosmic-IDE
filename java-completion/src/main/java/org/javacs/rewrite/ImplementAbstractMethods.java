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
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.javacs.CompileTask;
import org.javacs.CompilerProvider;
import org.javacs.FindHelper;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.Logger;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;

public class ImplementAbstractMethods implements Rewrite {
    private static final Logger LOG = Logger.getLogger("main");
    final Path file;
    final ClassTree tree;
    final TreePath path;

    public ImplementAbstractMethods(final Path file, final ClassTree tree, final TreePath path) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(tree);
        Objects.requireNonNull(path);

        this.file = file;
        this.tree = tree;
        this.path = path;
    }

    @Override
    public Map<Path, TextEdit[]> rewrite(CompilerProvider compiler) {
        var insertText = new StringJoiner("\n");
        try (var task = compiler.compile(file)) {
            var elements = task.task.getElements();
            var types = task.task.getTypes();
            var trees = Trees.instance(task.task);
            var thisClass = (TypeElement) trees.getElement(this.path);
            var thisType = (DeclaredType) thisClass.asType();
            var thisTree = trees.getTree(thisClass);
            var indent = EditHelper.indent(task.task, task.root(), thisTree) + 4;
            for (var member : elements.getAllMembers(thisClass)) {
                if (member.getKind() == ElementKind.METHOD && member.getModifiers().contains(Modifier.ABSTRACT)) {
                    var method = member;
                    var source = findSource(compiler, task, method);
                    if (source == null) {
                        LOG.warning("...couldn't find source for " + method);
                    }
                    var parameterizedType = (ExecutableType) types.asMemberOf(thisType, method);
                    var text = EditHelper.printMethod(method, parameterizedType, source);
                    text = text.replaceAll("\n", "\n" + " ".repeat(indent));
                    insertText.add(text);
                }
            }
            var insert = EditHelper.insertAtEndOfClass(task.task, task.root(), thisTree);
            TextEdit[] edits = {new TextEdit(new Range(insert, insert), insertText + "\n")};
            return Map.of(file, edits);
        }
    }

    private MethodTree findSource(CompilerProvider compiler, CompileTask task, ExecutableElement method) {
        var superClass = (TypeElement) method.getEnclosingElement();
        var superClassName = superClass.getQualifiedName().toString();
        var methodName = method.getSimpleName().toString();
        var erasedParameterTypes = FindHelper.erasedParameterTypes(task, method);
        var sourceFile = compiler.findAnywhere(superClassName);
        if (sourceFile.isEmpty()) return null;
        var parse = compiler.parse(sourceFile.get());
        return FindHelper.findMethod(parse, superClassName, methodName, erasedParameterTypes);
    }
}
