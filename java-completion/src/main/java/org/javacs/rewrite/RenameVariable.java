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

import com.sun.source.util.Trees;

import org.eclipse.lsp4j.TextEdit;
import org.javacs.CompilerProvider;

import java.nio.file.Path;
import java.util.Map;

public class RenameVariable implements Rewrite {
    final Path file;
    final int position;
    final String newName;

    public RenameVariable(Path file, int position, String newName) {
        this.file = file;
        this.position = position;
        this.newName = newName;
    }

    @Override
    public Map<Path, TextEdit[]> rewrite(CompilerProvider compiler) {
        try (var compile = compiler.compile(file)) {
            var trees = Trees.instance(compile.task);
            var root = compile.root();
            var found = new FindVariableAt(compile.task).scan(root, position);
            if (found == null) {
                return CANCELLED;
            }
            var rename = trees.getPath(root, found);
            var edits = new RenameHelper(compile).renameVariable(root, rename, newName);
            return Map.of(file, edits);
        }
    }
}
