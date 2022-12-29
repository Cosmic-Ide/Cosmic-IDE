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

package org.javacs.semantics;

import static org.javacs.services.JavaLanguageServer.Range_NONE;
import static javax.lang.model.element.ElementKind.ANNOTATION_TYPE;
import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.ENUM;
import static javax.lang.model.element.ElementKind.ENUM_CONSTANT;
import static javax.lang.model.element.ElementKind.EXCEPTION_PARAMETER;
import static javax.lang.model.element.ElementKind.FIELD;
import static javax.lang.model.element.ElementKind.INSTANCE_INIT;
import static javax.lang.model.element.ElementKind.INTERFACE;
import static javax.lang.model.element.ElementKind.LOCAL_VARIABLE;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.ElementKind.PACKAGE;
import static javax.lang.model.element.ElementKind.PARAMETER;
import static javax.lang.model.element.ElementKind.RESOURCE_VARIABLE;
import static javax.lang.model.element.ElementKind.STATIC_INIT;
import static javax.lang.model.element.ElementKind.TYPE_PARAMETER;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import org.cosmic.ide.lsp.SemanticHighlight;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.javacs.CompileTask;
import org.javacs.FileStore;
import org.javacs.markup.RangeHelper;

import java.nio.file.Paths;
import java.util.logging.Logger;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;

class SemanticHighlighter extends TreePathScanner<Void, SemanticHighlight> {

    private static final Logger LOG = Logger.getLogger("main");
    private final Trees trees;
    private final CompileTask task;
    private final CancelChecker checker;

    SemanticHighlighter(CompileTask task, CancelChecker checker) {
        this.task = task;
        this.checker = checker;
        this.trees = Trees.instance(task.task);
    }

    private void putSemantics(Name name, SemanticHighlight colors) {
        if (name.contentEquals("this") || name.contentEquals("super") || name.contentEquals("class")) {
            return;
        }
        checker.checkCanceled();
        var fromPath = getCurrentPath();

        // Cancel the visit in case a task was cancelled/closed
        if (task == null || task.task == null) {
            throw new RuntimeException("Task is null");
        }

        var toEl = trees.getElement(fromPath);
        if (toEl == null) {
            return;
        }

        var range = find(fromPath, name);
        if (range == Range_NONE)
            return;

        var kind = toEl.getKind();

        if (kind == PACKAGE) {
            colors.packages.add(range);
            return;
        }

        if (kind == ENUM) {
            colors.enumTypes.add(range);
            return;
        }

        if (kind == CLASS) {
            colors.classNames.add(range);
            return;
        }

        if (kind == ANNOTATION_TYPE) {
            colors.annotationTypes.add(range);
            return;
        }

        if (kind == INTERFACE) {
            colors.interfaces.add(range);
            return;
        }

        if (kind == ENUM_CONSTANT) {
            colors.enums.add(range);
            return;
        }

        if (kind == FIELD) {
            if (toEl.getModifiers().contains(Modifier.STATIC)) {
                colors.statics.add(range);
            } else {
                colors.fields.add(range);
            }
            return;
        }

        if (kind == METHOD) {
            colors.methodDeclarations.add(range);
            return;
        }

        if (kind == PARAMETER) {
            colors.parameters.add(range);
            return;
        }

        if (kind == LOCAL_VARIABLE) {
            colors.locals.add(range);
            return;
        }

        if (kind == EXCEPTION_PARAMETER) {
            colors.exceptionParams.add(range);
            return;
        }

        if (kind == CONSTRUCTOR) {
            colors.constructors.add(range);
            return;
        }

        if (kind == STATIC_INIT) {
            colors.staticInits.add(range);
            return;
        }

        if (kind == INSTANCE_INIT) {
            colors.instanceInits.add(range);
            return;
        }

        if (kind == TYPE_PARAMETER) {
            colors.typeParams.add(range);
            return;
        }

        if (kind == RESOURCE_VARIABLE) {
            colors.resourceVariables.add(range);
        }
    }

    private Range find(TreePath path, Name name) {
        // Find region containing name
        checker.checkCanceled();
        var pos = trees.getSourcePositions();
        var root = path.getCompilationUnit();
        var leaf = path.getLeaf();
        var start = (int) pos.getStartPosition(root, leaf);
        var end = (int) pos.getEndPosition(root, leaf);
        // Adjust start to remove LHS of declarations and member selections
        if (leaf instanceof MemberSelectTree select) {
            start = (int) pos.getEndPosition(root, select.getExpression());
        } else if (leaf instanceof VariableTree declaration) {
            start = (int) pos.getEndPosition(root, declaration.getType());
        }
        // If no position, give up
        if (start == -1 || end == -1) {
            return Range_NONE;
        }
        // Find name inside expression
        var file = Paths.get(root.getSourceFile().toUri());
        var contents = FileStore.contents(file);
        var region = contents.substring(start, end);
        start += region.indexOf(name.toString());
        end = start + name.length();
        return RangeHelper.range(root, start, end);
    }

    @Override
    public Void visitIdentifier(IdentifierTree t, SemanticHighlight colors) {
        putSemantics(t.getName(), colors);
        return super.visitIdentifier(t, colors);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree t, SemanticHighlight colors) {
        putSemantics(t.getIdentifier(), colors);
        return super.visitMemberSelect(t, colors);
    }

    @Override
    public Void visitVariable(VariableTree t, SemanticHighlight colors) {
        putSemantics(t.getName(), colors);
        return super.visitVariable(t, colors);
    }

    @Override
    public Void visitClass(ClassTree t, SemanticHighlight colors) {
        putSemantics(t.getSimpleName(), colors);

        return super.visitClass(t, colors);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, SemanticHighlight colors) {
        checker.checkCanceled();
        Name name = null;
        var select = tree.getMethodSelect();
        if (select instanceof MemberSelectTree) {
            name = ((MemberSelectTree) select).getIdentifier();
        } else if (select instanceof IdentifierTree) {
            name = ((IdentifierTree) select).getName();
        }

        if (name != null) {
            var range = find(getCurrentPath(), name);
            if (range != null) {
                colors.methodInvocations.add(range);
            }
        }
        return super.visitMethodInvocation(tree, colors);

    }

    @Override
    public Void visitMethod(MethodTree tree, SemanticHighlight colors) {
        putSemantics(tree.getName(), colors);
        return super.visitMethod(tree, colors);
    }
}