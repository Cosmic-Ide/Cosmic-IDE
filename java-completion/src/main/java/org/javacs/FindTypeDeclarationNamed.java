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

package org.javacs;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreeScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class FindTypeDeclarationNamed extends TreeScanner<ClassTree, String> {
    private final List<CharSequence> qualifiedName = new ArrayList<>();

    @Override
    public ClassTree visitCompilationUnit(CompilationUnitTree t, String find) {
        var name = Objects.toString(t.getPackageName(), "");
        qualifiedName.add(name);
        return super.visitCompilationUnit(t, find);
    }

    @Override
    public ClassTree visitClass(ClassTree t, String find) {
        qualifiedName.add(t.getSimpleName());
        if (String.join(".", qualifiedName).equals(find)) {
            return t;
        }
        var recurse = super.visitClass(t, find);
        qualifiedName.remove(qualifiedName.size() - 1);
        return recurse;
    }

    @Override
    public ClassTree reduce(ClassTree a, ClassTree b) {
        if (a != null) return a;
        return b;
    }
}
