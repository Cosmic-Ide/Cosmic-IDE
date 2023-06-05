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

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.lang.model.element.TypeElement;

class FindUsedImports extends TreePathScanner<Void, Set<String>> {
    private final Trees trees;
    private final Set<String> imports = new HashSet<String>();

    FindUsedImports(JavacTask task) {
        this.trees = Trees.instance(task);
    }

    @Override
    public Void visitImport(ImportTree t, Set<String> references) {
        if (!t.isStatic()) {
            imports.add(Objects.toString(t.getQualifiedIdentifier(), ""));
        }
        return super.visitImport(t, references);
    }

    @Override
    public Void visitIdentifier(IdentifierTree t, Set<String> references) {
        var e = trees.getElement(getCurrentPath());
        if (e instanceof TypeElement) {
            var type = e;
            var qualifiedName = type.getQualifiedName().toString();
            var packageName = packageName(qualifiedName);
            var starImport = packageName + ".*";
            if (imports.contains(qualifiedName)) {
                references.add(qualifiedName);
            } else if (imports.contains(starImport)) {
                references.add(starImport);
            }
        }
        return null;
    }

    private String packageName(String qualifiedName) {
        var lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot != -1) {
            return qualifiedName.substring(0, lastDot);
        }
        return "";
    }
}
