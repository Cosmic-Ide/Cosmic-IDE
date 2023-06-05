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

import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.javacs.CompilerProvider;
import org.javacs.ParseTask;

import java.nio.file.Path;
import java.util.Map;

public class AddImport implements Rewrite {
    final Path file;
    final String className;

    public AddImport(Path file, String className) {
        this.file = file;
        this.className = className;
    }

    @Override
    public Map<Path, TextEdit[]> rewrite(CompilerProvider compiler) {
        var task = compiler.parse(file);
        var point = insertPosition(task);
        var text = "import " + className + ";\n";
        TextEdit[] edits = {new TextEdit(new Range(point, point), text)};
        return Map.of(file, edits);
    }

    private Position insertPosition(ParseTask task) {
        var imports = task.root.getImports();
        for (var i : imports) {
            var next = i.getQualifiedIdentifier().toString();
            if (className.compareTo(next) < 0) {
                return insertBefore(task, i);
            }
        }
        if (!imports.isEmpty()) {
            var last = imports.get(imports.size() - 1);
            return insertAfter(task, last);
        }
        if (task.root.getPackage() != null) {
            return insertAfter(task, task.root.getPackage());
        }
        return new Position(0, 0);
    }

    private Position insertBefore(ParseTask task, Tree i) {
        var pos = Trees.instance(task.task).getSourcePositions();
        var offset = pos.getStartPosition(task.root, i);
        var line = (int) task.root.getLineMap().getLineNumber(offset);
        return new Position(line - 1, 0);
    }

    private Position insertAfter(ParseTask task, Tree i) {
        var pos = Trees.instance(task.task).getSourcePositions();
        var offset = pos.getStartPosition(task.root, i);
        var line = (int) task.root.getLineMap().getLineNumber(offset);
        return new Position(line, 0);
    }
}
