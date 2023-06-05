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

package org.javacs.index;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.javacs.ParseTask;
import org.javacs.StringSearch;

import java.util.List;
import java.util.Objects;

class FindSymbolsMatching extends TreePathScanner<Void, List<SymbolInformation>> {

    private final ParseTask task;
    private final String query;
    private CompilationUnitTree root;
    private CharSequence containerName;

    FindSymbolsMatching(ParseTask task, String query) {
        this.task = task;
        this.query = query;
    }

    private static SymbolKind asSymbolKind(Tree.Kind k) {
        switch (k) {
            case ANNOTATION_TYPE:
            case CLASS:
                return SymbolKind.Class;
            case ENUM:
                return SymbolKind.Enum;
            case INTERFACE:
                return SymbolKind.Interface;
            case METHOD:
                return SymbolKind.Method;
            case TYPE_PARAMETER:
                return SymbolKind.TypeParameter;
            case VARIABLE:
                // This method is used for symbol-search functionality,
                // where we only return fields, not local variables
                return SymbolKind.Field;
            default:
                return null;
        }
    }

    @Override
    public Void visitCompilationUnit(CompilationUnitTree t, List<SymbolInformation> list) {
        root = t;
        containerName = Objects.toString(t.getPackageName(), "");
        return super.visitCompilationUnit(t, list);
    }

    @Override
    public Void visitClass(ClassTree t, List<SymbolInformation> list) {
        if (StringSearch.matchesTitleCase(t.getSimpleName(), query)) {
            var info = new SymbolInformation();
            info.setName(t.getSimpleName().toString());
            info.setKind(asSymbolKind(t.getKind()));
            info.setLocation(location(t));
            info.setContainerName(containerName.toString());
            list.add(info);
        }
        var push = containerName;
        containerName = t.getSimpleName();
        super.visitClass(t, list);
        containerName = push;
        return null;
    }

    @Override
    public Void visitMethod(MethodTree t, List<SymbolInformation> list) {
        if (StringSearch.matchesTitleCase(t.getName(), query)) {
            var info = new SymbolInformation();
            info.setName(t.getName().toString());
            info.setKind(asSymbolKind(t.getKind()));
            info.setLocation(location(t));
            info.setContainerName(containerName.toString());
            list.add(info);
        }
        var push = containerName;
        containerName = t.getName();
        super.visitMethod(t, list);
        containerName = push;
        return null;
    }

    @Override
    public Void visitVariable(VariableTree t, List<SymbolInformation> list) {
        if (getCurrentPath().getParentPath().getLeaf() instanceof ClassTree
                && StringSearch.matchesTitleCase(t.getName(), query)) {
            var info = new SymbolInformation();
            info.setName(t.getName().toString());
            info.setKind(asSymbolKind(t.getKind()));
            info.setLocation(location(t));
            info.setContainerName(containerName.toString());
            list.add(info);
        }
        var push = containerName;
        containerName = t.getName();
        super.visitVariable(t, list);
        containerName = push;
        return null;
    }

    private Location location(Tree t) {
        var trees = Trees.instance(task.task);
        var pos = trees.getSourcePositions();
        var lines = task.root.getLineMap();
        var start = pos.getStartPosition(root, t);
        var end = pos.getEndPosition(root, t);
        var startLine = (int) lines.getLineNumber(start);
        var startColumn = (int) lines.getColumnNumber(start);
        var endLine = (int) lines.getLineNumber(end);
        var endColumn = (int) lines.getColumnNumber(end);
        var range = new Range(new Position(startLine - 1, startColumn - 1), new Position(endLine - 1, endColumn - 1));
        return new Location(root.getSourceFile().toUri().toString(), range);
    }
}
