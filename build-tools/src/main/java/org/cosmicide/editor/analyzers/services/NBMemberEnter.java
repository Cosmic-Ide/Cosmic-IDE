/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.analyzers.services;


import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.comp.MemberEnter;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;

/**
 * @author lahvac
 */
public class NBMemberEnter extends MemberEnter {

    private final CancelService cancelService;
    private final JavacTrees trees;
    private final boolean backgroundScan;

    public NBMemberEnter(Context context, boolean backgroundScan) {
        super(context);
        cancelService = CancelService.instance(context);
        trees = context.get(JavacTrees.class);
        this.backgroundScan = backgroundScan;
    }

    public static void preRegister(Context context, boolean backgroundScan) {
        context.put(
                MemberEnter.class,
                (Context.Factory<MemberEnter>) c -> new NBMemberEnter(c, backgroundScan));
    }

    @Override
    public void visitTopLevel(JCTree.JCCompilationUnit tree) {
        cancelService.abortIfCanceled();
        super.visitTopLevel(tree);
    }

    @Override
    public void visitImport(JCTree.JCImport tree) {
        cancelService.abortIfCanceled();
        super.visitImport(tree);
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl tree) {
        cancelService.abortIfCanceled();
        super.visitMethodDef(tree);
        if (!backgroundScan && trees instanceof NBJavacTrees && !env.enclClass.defs.contains(tree)) {
            TreePath path = trees.getPath(env.toplevel, env.enclClass);
            if (path != null) {
                ((NBJavacTrees) trees).addPathForElement(tree.sym, new TreePath(path, tree));
            }
        }
    }

    @Override
    public void visitVarDef(JCTree.JCVariableDecl tree) {
        cancelService.abortIfCanceled();
        super.visitVarDef(tree);
    }
}