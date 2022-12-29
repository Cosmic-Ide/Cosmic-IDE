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

package org.javacs.completion;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

class FindInvocationAt extends TreePathScanner<TreePath, Long> {
    private final JavacTask task;
    private CompilationUnitTree root;

    FindInvocationAt(JavacTask task) {
        this.task = task;
    }

    @Override
    public TreePath visitCompilationUnit(CompilationUnitTree t, Long find) {
        root = t;
        return reduce(super.visitCompilationUnit(t, find), getCurrentPath());
    }

    @Override
    public TreePath visitMethodInvocation(MethodInvocationTree t, Long find) {
        var pos = Trees.instance(task).getSourcePositions();
        var start = pos.getEndPosition(root, t.getMethodSelect()) + 1;
        var end = pos.getEndPosition(root, t) - 1;
        if (start <= find && find <= end) {
            return reduce(super.visitMethodInvocation(t, find), getCurrentPath());
        }
        return super.visitMethodInvocation(t, find);
    }

    @Override
    public TreePath visitNewClass(NewClassTree t, Long find) {
        var pos = Trees.instance(task).getSourcePositions();
        var start = pos.getEndPosition(root, t.getIdentifier()) + 1;
        var end = pos.getEndPosition(root, t) - 1;
        if (start <= find && find <= end) {
            return reduce(super.visitNewClass(t, find), getCurrentPath());
        }
        return super.visitNewClass(t, find);
    }

    @Override
    public TreePath reduce(TreePath a, TreePath b) {
        if (a != null) return a;
        return b;
    }
}
