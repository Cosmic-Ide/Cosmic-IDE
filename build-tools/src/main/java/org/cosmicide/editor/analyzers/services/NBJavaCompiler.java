/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.analyzers.services;

import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * @author lahvac
 */
public class NBJavaCompiler extends JavaCompiler {

    private final CancelService cancelService;
    private Consumer<Env<AttrContext>> desugarCallback;
    private boolean desugaring;

    public NBJavaCompiler(Context context) {
        super(context);
        cancelService = CancelService.instance(context);
    }

    public static void preRegister(Context context) {
        context.put(compilerKey, (Context.Factory<JavaCompiler>) NBJavaCompiler::new);
    }

    @Override
    public void processAnnotations(List<JCTree.JCCompilationUnit> roots, Collection<String> classnames) {
        if (roots.isEmpty()) {
            super.processAnnotations(roots, classnames);
        } else {
            setOrigin(roots.head.sourcefile.toUri().toString());
            try {
                super.processAnnotations(roots, classnames);
            } finally {
                setOrigin("");
            }
        }
    }

    private void setOrigin(String origin) {
        fileManager.handleOption("apt-origin", Collections.singletonList(origin).iterator());
    }

    public void setDesugarCallback(Consumer<Env<AttrContext>> callback) {
        this.desugarCallback = callback;
    }

    @Override
    protected void desugar(Env<AttrContext> env, Queue<Pair<Env<AttrContext>, JCTree.JCClassDecl>> results) {
        boolean prevDesugaring = desugaring;
        try {
            desugaring = true;
            super.desugar(env, results);
        } finally {
            desugaring = prevDesugaring;
        }
    }

    void maybeInvokeDesugarCallback(Env<AttrContext> env) {
        if (desugaring && desugarCallback != null) {
            desugarCallback.accept(env);
        }
    }

}