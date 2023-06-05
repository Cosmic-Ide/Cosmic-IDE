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

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import java.util.logging.Logger;

import javax.lang.model.element.Element;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;

class FindMethodCallAt extends TreePathScanner<MethodInvocationTree, Integer> {
    private static final Logger LOG = Logger.getLogger("main");
    private final Trees trees;
    private final SourcePositions pos;
    private CompilationUnitTree root;
    private boolean isMemberSelect;
    private boolean isStatic;
    private String returnType;
    private ClassTree enclosingTree;
    private TreePath enclosingTreePath;
    private TypeElement declaredInTopLevel;
    private TypeElement enclosingElement;

    FindMethodCallAt(JavacTask task) {
        this.trees = Trees.instance(task);
        this.pos = trees.getSourcePositions();
    }

    public String getReturnType() {
        return returnType;
    }

    public TypeElement getDeclaringType() {
        return declaredInTopLevel;
    }

    public TypeElement getEnclosingElement() {
        return enclosingElement;
    }

    public ClassTree getEnclosingClass() {
        return enclosingTree;
    }

    public TreePath getEnclosingTreePath() {
        return enclosingTreePath;
    }

    public boolean isMemberSelect() {
        return isMemberSelect;
    }

    public boolean isStaticAccess() {
        return isStatic;
    }

    @Override
    public MethodInvocationTree visitCompilationUnit(CompilationUnitTree t, Integer find) {
        root = t;
        return super.visitCompilationUnit(t, find);
    }

    /**
     * A method invocation
     * <pre>
     * 	foo();
     * 	field.foo();
     * 	Class.foo();
     * 	Class.field.foo();
     * </pre>
     */
    @Override
    public MethodInvocationTree visitMethodInvocation(MethodInvocationTree t, Integer find) {
        var smaller = super.visitMethodInvocation(t, find);
        if (smaller != null) {
            return smaller;
        }

        if (pos.getStartPosition(root, t) <= find && find < pos.getEndPosition(root, t)) {
            if (t.getMethodSelect() instanceof MemberSelectTree) {
                checkForQualifiedName((MemberSelectTree) t.getMethodSelect(), find);
            }
            return t;
        }
        return null;
    }

    /**
     * Find method invocation while declaring a variable
     * E.g. <pre> int x = foo(); </pre>
     */
    @Override
    public MethodInvocationTree visitVariable(VariableTree tree, Integer find) {
        if (tree != null) {
            var start = pos.getStartPosition(root, tree);
            var end = pos.getEndPosition(root, tree);
            if (start <= find && find <= end && tree.getInitializer() instanceof MethodInvocationTree) {
                returnType = tree.getType().toString();
                return visitMethodInvocation((MethodInvocationTree) tree.getInitializer(), find);
            }
        }
        return super.visitVariable(tree, find);
    }

    /**
     * Find method invocation while initializing a variable
     * E.g.
     * <pre>
     * 	int x;
     * 	...
     * 	x = foo();
     * </pre>
     */
    @Override
    public MethodInvocationTree visitAssignment(AssignmentTree tree, Integer find) {
        if (tree != null) {
            var start = pos.getStartPosition(root, tree);
            var end = pos.getEndPosition(root, tree);
            if (start <= find && find <= end && tree.getExpression() instanceof MethodInvocationTree) {
                returnType = findType();
                return visitMethodInvocation((MethodInvocationTree) tree.getExpression(), find);
            }
        }
        return super.visitAssignment(tree, find);

    }

    private String findType() {
        if (this.trees != null && getCurrentPath() != null) {
            var typeMirror = this.trees.getTypeMirror(getCurrentPath());
            if (typeMirror != null
                    && typeMirror.getKind() != TypeKind.NONE
                    && typeMirror.getKind() != TypeKind.ERROR)
                return typeMirror.toString();
        }
        return null;
    }

    @Override
    public MethodInvocationTree reduce(MethodInvocationTree r1, MethodInvocationTree r2) {
        if (r1 != null) return r1;
        return r2;
    }

    private void checkForQualifiedName(MemberSelectTree tree, Integer find) {
        if (tree != null) {
            var start = pos.getStartPosition(root, tree);
            var end = pos.getEndPosition(root, tree);
            if (start <= find && find <= end) {
                var element = trees.getElement(trees.getPath(root, tree));

                // Is this a static access?
                isStatic = element instanceof TypeElement;

                // Find enclosing element to get the TypeElement from which this method is being called
                this.enclosingElement = enclosingType(element);

                // find top level declaration to get the qualified name of the class
                this.declaredInTopLevel = enclosingTopLevelType(enclosingElement);

                // Get the tree path of the enclosing element
                this.enclosingTreePath = trees.getPath(enclosingElement);

                // Get the ClassTree of the enclosing element
                // Will be needed in CreateMissingMethod.java for EditHelper
                this.enclosingTree = enclosingClass(this.enclosingTreePath);

                isMemberSelect = enclosingElement != null
                        && declaredInTopLevel != null
                        && enclosingTreePath != null
                        && enclosingTree != null;

                LOG.info(String.format("checkForQualifiedName\nisStatic: %s\nenclosingElement: %s\ndeclaredInTopLevel: %s\nenclosingTreePath: %s\nenclosingTree: %s\nisMemberSelect: %s", String.valueOf(isStatic), String.valueOf(enclosingElement), String.valueOf(declaredInTopLevel), String.valueOf(enclosingTreePath), String.valueOf(enclosingTree), String.valueOf(isMemberSelect)));
            }
        }
    }

    private ClassTree enclosingClass(TreePath path) {
        while (path != null) {
            if (path.getLeaf() instanceof ClassTree) {
                ClassTree tree = (ClassTree) path.getLeaf();
                return tree;
            }

            path = path.getParentPath();
        }
        return null;
    }

    private TypeElement enclosingTopLevelType(Element element) {
        while (element != null) {
            if (element instanceof TypeElement) {
                TypeElement type = (TypeElement) element;
                if (type.getNestingKind() == NestingKind.TOP_LEVEL)
                    return type;
            }

            element = element.getEnclosingElement();
        }
        return null;
    }

    private TypeElement enclosingType(Element element) {
        while (element != null) {
            if (element instanceof TypeElement) {
                TypeElement type = (TypeElement) element;
                if (trees.getTree(type) instanceof ClassTree)
                    return type;
            }

            element = element.getEnclosingElement();
        }
        return null;
    }
}
