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

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

class DiagnosticVisitor extends TreeScanner<Void, Map<TreePath, String>> {
    private static final Logger LOG = Logger.getLogger("main");
    private final JavacTask task;
    private final CancelChecker checker;
    private final Trees trees;
    private final Map<Element, TreePath> privateDeclarations = new HashMap<>(), localVariables = new HashMap<>();
    private final Set<Element> used = new HashSet<>();
    // Copied from TreePathScanner
    // We need to be able to call scan(path, _) recursively
    private TreePath path;
    private CompilationUnitTree root;
    private Map<String, TreePath> declaredExceptions = new HashMap<>();
    private Set<String> observedExceptions = new HashSet<>();

    DiagnosticVisitor(JavacTask task, CancelChecker checker) {
        this.task = task;
        this.checker = checker;
        this.trees = Trees.instance(task);
    }

    private void scanPath(TreePath path) {
        TreePath prev = this.path;
        this.path = path;
        try {
            path.getLeaf().accept(this, null);
        } finally {
            this.path = prev; // So we can call scan(path, _) recursively
        }
    }

    @Override
    public Void scan(Tree tree, Map<TreePath, String> p) {
        if (tree == null) return null;

        TreePath prev = path;
        path = new TreePath(path, tree);

        try {
            return tree.accept(this, p);
        } finally {
            path = prev;
        }
    }

    Set<Element> notUsed() {
        var unused = new HashSet<Element>();
        unused.addAll(privateDeclarations.keySet());
        unused.addAll(localVariables.keySet());
        unused.removeAll(used);
        // Remove if there are any null elements somehow ended up being added
        // during async work which calls `lint`
        unused.removeIf(Objects::isNull);
        // Remove if <error > field was injected while forming the AST
        unused.removeIf(i -> i.toString().equals("<error>"));
        return unused;
    }

    private void foundPrivateDeclaration() {
        checker.checkCanceled();
        privateDeclarations.put(trees.getElement(path), path);
    }

    private void foundLocalVariable() {
        checker.checkCanceled();
        localVariables.put(trees.getElement(path), path);
    }

    private void foundReference() {
        checker.checkCanceled();
        var toEl = trees.getElement(path);
        if (toEl == null) {
            return;
        }
        if (toEl.asType().getKind() == TypeKind.ERROR) {
            foundPseudoReference(toEl);
            return;
        }
        sweep(toEl);
    }

    private void foundPseudoReference(Element toEl) {
        var parent = toEl.getEnclosingElement();
        if (!(parent instanceof TypeElement type)) {
            return;
        }
        var memberName = toEl.getSimpleName();
        for (var member : type.getEnclosedElements()) {
            if (member.getSimpleName().contentEquals(memberName)) {
                sweep(member);
            }
        }
    }

    private void sweep(Element toEl) {
        var firstUse = used.add(toEl);
        var notScanned = firstUse && privateDeclarations.containsKey(toEl);
        if (notScanned) {
            scanPath(privateDeclarations.get(toEl));
        }
    }

    private boolean isReachable(TreePath path) {
        // Check if t is reachable because it's public
        var t = path.getLeaf();
        if (t instanceof VariableTree v) {
            var isPrivate = v.getModifiers().getFlags().contains(Modifier.PRIVATE);
            if (!isPrivate || isLocalVariable(path)) {
                return true;
            }
        }
        if (t instanceof MethodTree m) {
            var isPrivate = m.getModifiers().getFlags().contains(Modifier.PRIVATE);
            var isEmptyConstructor = m.getParameters().isEmpty() && m.getReturnType() == null;
            if (!isPrivate || isEmptyConstructor) {
                return true;
            }
        }
        if (t instanceof ClassTree c) {
            var isPrivate = c.getModifiers().getFlags().contains(Modifier.PRIVATE);
            if (!isPrivate) {
                return true;
            }
        }
        // Check if t has been referenced by a reachable element
        var el = trees.getElement(path);
        return used.contains(el);
    }

    private boolean isLocalVariable(TreePath path) {
        var kind = path.getLeaf().getKind();
        if (kind != Tree.Kind.VARIABLE) {
            return false;
        }
        var parent = path.getParentPath().getLeaf().getKind();
        if (parent == Tree.Kind.CLASS || parent == Tree.Kind.INTERFACE) {
            return false;
        }
        if (parent == Tree.Kind.METHOD) {
            var method = (MethodTree) path.getParentPath().getLeaf();
            return method.getBody() != null;
        }
        return true;
    }

    private Map<String, TreePath> declared(MethodTree t) {
        var names = new HashMap<String, TreePath>();
        for (var e : t.getThrows()) {
            var path = new TreePath(this.path, e);
            var to = trees.getElement(path);
            if (!(to instanceof TypeElement type)) continue;
            var name = type.getQualifiedName().toString();
            names.put(name, path);
        }
        return names;
    }

    @Override
    public Void visitCompilationUnit(CompilationUnitTree t, Map<TreePath, String> notThrown) {
        root = t;
        checker.checkCanceled();
        return super.visitCompilationUnit(t, notThrown);
    }

    @Override
    public Void visitVariable(VariableTree t, Map<TreePath, String> notThrown) {
        checker.checkCanceled();
        if (isLocalVariable(path)) {
            foundLocalVariable();
            super.visitVariable(t, notThrown);
        } else if (isReachable(path)) {
            super.visitVariable(t, notThrown);
        } else {
            foundPrivateDeclaration();
        }
        return null;
    }

    @Override
    public Void visitMethod(MethodTree t, Map<TreePath, String> notThrown) {
        checker.checkCanceled();
        // Create a new method scope
        var pushDeclared = declaredExceptions;
        var pushObserved = observedExceptions;
        declaredExceptions = declared(t);
        observedExceptions = new HashSet<>();
        // Recursively scan for 'throw' and method calls
        super.visitMethod(t, notThrown);
        // Check for exceptions that were never thrown
        for (var exception : declaredExceptions.keySet()) {
            if (!observedExceptions.contains(exception)) {
                notThrown.put(declaredExceptions.get(exception), exception);
            }
        }
        declaredExceptions = pushDeclared;
        observedExceptions = pushObserved;

        if (!isReachable(path)) {
            foundPrivateDeclaration();
        }
        return null;
    }

    @Override
    public Void visitClass(ClassTree t, Map<TreePath, String> notThrown) {
        checker.checkCanceled();
        if (isReachable(path)) {
            super.visitClass(t, notThrown);
        } else {
            foundPrivateDeclaration();
        }
        return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree t, Map<TreePath, String> notThrown) {
        foundReference();
        return super.visitIdentifier(t, notThrown);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree t, Map<TreePath, String> notThrown) {
        foundReference();
        return super.visitMemberSelect(t, notThrown);
    }

    @Override
    public Void visitMemberReference(MemberReferenceTree t, Map<TreePath, String> notThrown) {
        foundReference();
        return super.visitMemberReference(t, notThrown);
    }

    @Override
    public Void visitNewClass(NewClassTree t, Map<TreePath, String> notThrown) {
        foundReference();
        return super.visitNewClass(t, notThrown);
    }

    @Override
    public Void visitThrow(ThrowTree t, Map<TreePath, String> notThrown) {
        checker.checkCanceled();
        var path = new TreePath(this.path, t.getExpression());
        var type = trees.getTypeMirror(path);
        addThrown(type);
        return super.visitThrow(t, notThrown);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree t, Map<TreePath, String> notThrown) {
        checker.checkCanceled();
        var target = trees.getElement(this.path);
        if (target instanceof ExecutableElement method) {
            for (var type : method.getThrownTypes()) {
                addThrown(type);
            }
        }
        return super.visitMethodInvocation(t, notThrown);
    }

    private void addThrown(TypeMirror type) {
        checker.checkCanceled();
        if (type instanceof DeclaredType declared) {
            var el = (TypeElement) declared.asElement();
            var name = el.getQualifiedName().toString();
            observedExceptions.add(name);
        }
    }
}