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
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.Trees;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.javacs.CompileTask;
import org.javacs.CompilerProvider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Logger;

import javax.lang.model.element.Modifier;

public class GenerateRecordConstructor implements Rewrite {
    private static final Logger LOG = Logger.getLogger("main");
    final String className;

    public GenerateRecordConstructor(String className) {
        this.className = className;
    }

    @Override
    public Map<Path, TextEdit[]> rewrite(CompilerProvider compiler) {
        LOG.info("Generate default constructor for " + className + "...");
        // TODO this needs to fall back on looking for inner classes and package-private classes
        var file = compiler.findTypeDeclaration(className);
        try (var task = compiler.compile(file)) {
            var typeElement = task.task.getElements().getTypeElement(className);
            var typeTree = Trees.instance(task.task).getTree(typeElement);
            var fields = fieldsNeedingInitialization(typeTree);
            var parameters = generateParameters(task, fields);
            var initializers = generateInitializers(task, fields);
            var buf = new StringBuffer();
            buf.append("\n");
            if (typeTree.getModifiers().getFlags().contains(Modifier.PUBLIC)) {
                buf.append("public ");
            }
            buf.append(simpleName(className))
                    .append("(")
                    .append(parameters)
                    .append(") {\n    ")
                    .append(initializers)
                    .append("\n}");
            var string = buf.toString();
            var indent = EditHelper.indent(task.task, task.root(), typeTree) + 4;
            string = string.replaceAll("\n", "\n" + " ".repeat(indent));
            string = string + "\n\n";
            var insert = insertPoint(task, typeTree);
            TextEdit[] edits = {new TextEdit(new Range(insert, insert), string)};
            return Map.of(file, edits);
        }
    }

    private List<VariableTree> fieldsNeedingInitialization(ClassTree typeTree) {
        var fields = new ArrayList<VariableTree>();
        for (var member : typeTree.getMembers()) {
            if (!(member instanceof VariableTree)) continue;
            var field = member;
            if (field.getInitializer() != null) continue;
            var flags = field.getModifiers().getFlags();
            if (flags.contains(Modifier.STATIC)) continue;
            if (!flags.contains(Modifier.FINAL)) continue;
            fields.add(field);
        }
        return fields;
    }

    private String generateParameters(CompileTask task, List<VariableTree> fields) {
        var join = new StringJoiner(", ");
        for (var f : fields) {
            join.add(extract(task, f.getType()) + " " + f.getName());
        }
        return join.toString();
    }

    private String generateInitializers(CompileTask task, List<VariableTree> fields) {
        var join = new StringJoiner("\n    ");
        for (var f : fields) {
            join.add("this." + f.getName() + " = " + f.getName() + ";");
        }
        return join.toString();
    }

    private CharSequence extract(CompileTask task, Tree typeTree) {
        try {
            var contents = task.root().getSourceFile().getCharContent(true);
            var pos = Trees.instance(task.task).getSourcePositions();
            var start = (int) pos.getStartPosition(task.root(), typeTree);
            var end = (int) pos.getEndPosition(task.root(), typeTree);
            return contents.subSequence(start, end);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String simpleName(String className) {
        var dot = className.lastIndexOf('.');
        if (dot != -1) {
            return className.substring(dot + 1);
        }
        return className;
    }

    private Position insertPoint(CompileTask task, ClassTree typeTree) {
        for (var member : typeTree.getMembers()) {
            if (member.getKind() == Tree.Kind.METHOD) {
                var method = member;
                if (method.getReturnType() == null) continue;
                LOG.info("...insert constructor before " + method.getName());
                return EditHelper.insertBefore(task.task, task.root(), method);
            }
        }
        LOG.info("...insert constructor at end of class");
        return EditHelper.insertAtEndOfClass(task.task, task.root(), typeTree);
    }
}
