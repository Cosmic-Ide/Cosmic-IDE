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

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.javacs.CompileTask;
import org.javacs.CompilerProvider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class AutoFixImports implements Rewrite {
    private static final Logger LOG = Logger.getLogger("main");
    final Path file;

    public AutoFixImports(Path file) {
        this.file = file;
    }

    @Override
    public Map<Path, TextEdit[]> rewrite(CompilerProvider compiler) {
        LOG.info("Fix imports in " + file + "...");
        try (CompileTask task = compiler.compile(file)) {
            var used = usedImports(task);
            var unresolved = unresolvedNames(task);
            var resolved = resolveNames(compiler, unresolved);
            var all = new ArrayList<String>();
            all.addAll(used);
            all.addAll(resolved.values());
            all.sort(String::compareTo); // TODO this is not always a good order
            var edits = new ArrayList<TextEdit>();
            edits.addAll(deleteImports(task));
            edits.add(insertImports(task, all));
            return Map.of(file, edits.toArray(new TextEdit[edits.size()]));
        }
    }

    private Set<String> usedImports(CompileTask task) {
        var used = new HashSet<String>();
        new FindUsedImports(task.task).scan(task.root(), used);
        return used;
    }

    private Set<String> unresolvedNames(CompileTask task) {
        var names = new HashSet<String>();
        for (var d : task.diagnostics) {
            if (!d.getCode().equals("compiler.err.cant.resolve.location")) continue;
            if (!d.getSource().toUri().equals(file.toUri())) continue;
            var start = (int) d.getStartPosition();
            var end = (int) d.getEndPosition();
            CharSequence contents;
            try {
                contents = d.getSource().getCharContent(true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            var name = contents.subSequence(start, end).toString();
            if (!name.matches("[A-Z]\\w+")) continue;
            names.add(name);
        }
        return names;
    }

    private Map<String, String> resolveNames(CompilerProvider compiler, Set<String> unresolved) {
        var resolved = new HashMap<String, String>();
        var alreadyImported = compiler.imports();
        for (var className : unresolved) {
            var candidates = new ArrayList<String>();
            for (var i : alreadyImported) {
                if (i.endsWith("." + className)) {
                    candidates.add(i);
                }
            }
            if (candidates.isEmpty()) continue;
            if (candidates.size() > 1) {
                LOG.warning("..." + className + " is ambiguous between " + String.join(", ", candidates));
                continue;
            }
            LOG.info("...resolve " + className + " to " + candidates.get(0));
            resolved.put(className, candidates.get(0));
        }
        // TODO import my own classes
        return resolved;
    }

    private List<TextEdit> deleteImports(CompileTask task) {
        var edits = new ArrayList<TextEdit>();
        var pos = Trees.instance(task.task).getSourcePositions();
        var root = task.root();
        for (var i : root.getImports()) {
            if (i.isStatic()) continue;
            var start = pos.getStartPosition(root, i);
            var line = (int) root.getLineMap().getLineNumber(start);
            var delete = new TextEdit(new Range(new Position(line - 1, 0), new Position(line, 0)), "");
            edits.add(delete);
        }
        return edits;
    }

    private TextEdit insertImports(CompileTask task, List<String> qualifiedNames) {
        var pos = insertPosition(task);
        var text = new StringBuilder();
        for (var i : qualifiedNames) {
            text.append("import ").append(i).append(";\n");
        }
        return new TextEdit(new Range(pos, pos), text.toString());
    }

    private Position insertPosition(CompileTask task) {
        var pos = Trees.instance(task.task).getSourcePositions();
        var root = task.root();
        // If there are imports, use the start of the first import as the insert position
        for (var i : root.getImports()) {
            if (!i.isStatic()) {
                var start = pos.getStartPosition(root, i);
                var line = (int) root.getLineMap().getLineNumber(start);
                return new Position(line - 1, 0);
            }
        }
        // If there are no imports, insert after the package declaration
        if (root.getPackage() != null) {
            var end = pos.getEndPosition(root, root.getPackage());
            var line = (int) root.getLineMap().getLineNumber(end) + 1;
            return new Position(line - 1, 0);
        }
        // If there are no imports and no package, insert at the top of the file
        return new Position(0, 0);
    }
}
