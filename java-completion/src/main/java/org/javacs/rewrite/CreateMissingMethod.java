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
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.javacs.CompileTask;
import org.javacs.CompilerProvider;

import java.nio.file.Path;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Logger;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

public class CreateMissingMethod implements Rewrite {
    private static final Logger LOG = Logger.getLogger("main");
    final Path file;
    final int position;
    int argCount = -1;

    public CreateMissingMethod(Path file, int position) {
        this.file = file;
        this.position = position;
    }

    @Override
    public Map<Path, TextEdit[]> rewrite(CompilerProvider compiler) {
        try (var task = compiler.compile(file)) {
            final var trees = Trees.instance(task.task);
            final var methodFinder = new FindMethodCallAt(task.task);
            final var call = methodFinder.scan(task.root(), position);
            if (call == null) return CANCELLED;
            final var path = trees.getPath(task.root(), call);
            final var returnType = methodFinder.getReturnType();
            var sourceFile = file;
            var currentMethod = surroundingMethod(path);
            var insertText = "\n";

            insertText += printMethodHeader(task, call, returnType, methodFinder.isMemberSelect(), (currentMethod.getModifiers().getFlags().contains(Modifier.STATIC) || methodFinder.isStaticAccess())) + " {\n" +
                    "    // TODO: Implement this method\n" +
                    "    " + createReturnStatement(returnType) + "\n" +
                    "}";

            TextEdit[] edits = null;
            if (methodFinder.isMemberSelect()) {
                // Accessing method from another class
                final var compilationUnit = methodFinder.getEnclosingTreePath().getCompilationUnit();
                final var enclosingClass = methodFinder.getEnclosingClass();
                final var indent = EditHelper.indent(task.task, compilationUnit, enclosingClass) + 4;
                insertText = insertText.replaceAll("\n", "\n" + " ".repeat(indent));
                insertText = insertText + "\n";
                final var insertPoint = EditHelper.insertAtEndOfClass(task.task, compilationUnit, enclosingClass);
                edits = new TextEdit[]{new TextEdit(new Range(insertPoint, insertPoint), insertText)};
                sourceFile = Path.of(compilationUnit.getSourceFile().toUri());
            } else {
                var surroundingClass = surroundingClass(path);
                var indent = EditHelper.indent(task.task, task.root(), surroundingClass) + 4;
                insertText = insertText.replaceAll("\n", "\n" + " ".repeat(indent));
                insertText = insertText + "\n";
                var insertPoint = EditHelper.insertAfter(task.task, task.root(), surroundingMethod(path));
                edits = new TextEdit[]{new TextEdit(new Range(insertPoint, insertPoint), insertText)};
            }
            if (file != null && edits != null)
                return Map.of(sourceFile, edits);
            else return null;
        }
    }

    private String createReturnStatement(String returnType) {
        if (returnType == null) return "";
        String value = "null";
        switch (returnType) {
            case "int":
            case "byte":
            case "short":
            case "long":
            case "char":
                value = "0";
                break;
            case "float":
                value = "0f";
                break;
            case "double":
                value = "0.0";
                break;
            case "boolean":
                value = "false";
                break;

            /**
             * Finding type of variable declaration may result in an error
             * We should then simply return empty return type
             */
            case "(ERROR)":
                return ""; // Directly return empty string
            default:
                value = "null";
                break;

        }
        return String.format("return %s;", value);
    }

    private ClassTree surroundingClass(TreePath call) {
        while (call != null) {
            if (call.getLeaf() instanceof ClassTree) {
                return (ClassTree) call.getLeaf();
            }
            call = call.getParentPath();
        }
        throw new RuntimeException("No surrounding class");
    }

    private MethodTree surroundingMethod(TreePath call) {
        while (call != null) {
            if (call.getLeaf() instanceof MethodTree) {
                return (MethodTree) call.getLeaf();
            }
            call = call.getParentPath();
        }
        throw new RuntimeException("No surrounding method");
    }

    private String printMethodHeader(CompileTask task, MethodInvocationTree call, String type, boolean isMemeberSelect, boolean isStatic) {
        var methodName = extractMethodName(call.getMethodSelect());
        var returnType = type == null || "(ERROR)".equals(type) ? "void" : type;
        LOG.info("Creating missing method with return type: " + returnType);
        if (returnType.equals(methodName)) {
            returnType = "_";
        }
        var parameters = printParameters(task, call);
        var modifiers = isMemeberSelect ? "public" : "private";
        if (isStatic)
            modifiers += " static";
        return modifiers + " " + returnType + " " + methodName + "(" + parameters + ")";
    }

    private String printParameters(CompileTask task, MethodInvocationTree call) {
        var trees = Trees.instance(task.task);
        var join = new StringJoiner(", ");
        for (var i = 0; i < call.getArguments().size(); i++) {
            var type = trees.getTypeMirror(trees.getPath(task.root(), call.getArguments().get(i)));
            var name = guessParameterName(call.getArguments().get(i), type);
            var argType = EditHelper.printType(type);
            join.add(String.format("final %s %s", argType, name));
        }
        return join.toString();
    }

    private String extractMethodName(ExpressionTree method) {
        if (method instanceof IdentifierTree) {
            var id = (IdentifierTree) method;
            return id.getName().toString();
        } else if (method instanceof MemberSelectTree) {
            var select = (MemberSelectTree) method;
            return select.getIdentifier().toString();
        } else {
            return "giveMeAProperName";
        }
    }

    private String guessParameterName(Tree argument, TypeMirror type) {
        var fromTree = guessParameterNameFromTree(argument);
        if (!fromTree.isEmpty()) {
            return fromTree;
        }
        var fromType = guessParameterNameFromType(type);
        if (!fromType.isEmpty()) {
            return fromType;
        }

        argCount++;
        return "iWantAName" + argCount;
    }

    private String guessParameterNameFromTree(Tree argument) {
        if (argument instanceof IdentifierTree) {
            var id = (IdentifierTree) argument;
            return id.getName().toString();
        } else if (argument instanceof MemberSelectTree) {
            var select = (MemberSelectTree) argument;
            return select.getIdentifier().toString();
        } else if (argument instanceof MemberReferenceTree) {
            var reference = (MemberReferenceTree) argument;
            return reference.getName().toString();
        } else {
            return "";
        }
    }

    private String guessParameterNameFromType(TypeMirror type) {
        if (type instanceof DeclaredType) {
            var declared = (DeclaredType) type;
            var name = declared.asElement().getSimpleName();
            return "" + Character.toLowerCase(name.charAt(0)) + name.subSequence(1, name.length());
        } else {
            return "";
        }
    }
}
