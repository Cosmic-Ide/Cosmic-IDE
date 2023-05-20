/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.analyzers.services;

import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

/**
 * @author lahvac
 */
public class NBTreeMaker extends TreeMaker {

    private final Names names;
    private final Types types;
    private final Symtab syms;

    protected NBTreeMaker(Context context) {
        super(context);
        this.names = Names.instance(context);
        this.types = Types.instance(context);
        this.syms = Symtab.instance(context);
    }

    protected NBTreeMaker(JCCompilationUnit toplevel, Names names, Types types, Symtab syms) {
        super(toplevel, names, types, syms);
        this.names = names;
        this.types = types;
        this.syms = syms;
    }

    public static void preRegister(Context context) {
        context.put(treeMakerKey, (Context.Factory<TreeMaker>) NBTreeMaker::new);
    }

    @Override
    public TreeMaker forToplevel(JCCompilationUnit toplevel) {
        return new NBTreeMaker(toplevel, names, types, syms);
    }

}