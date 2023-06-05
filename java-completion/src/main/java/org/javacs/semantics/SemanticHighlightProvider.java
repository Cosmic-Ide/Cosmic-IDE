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

package org.javacs.semantics;

import com.itsaky.lsp.SemanticHighlight;

import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.javacs.CompileTask;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SemanticHighlightProvider {

    private static final Logger LOG = Logger.getLogger("main");
    private final CompileTask task;
    private final CancelChecker checker;

    public SemanticHighlightProvider(CompileTask task, CancelChecker checker) {
        this.task = task;
        this.checker = checker;
    }

    public List<SemanticHighlight> highlights() {
        try {
            return highlightsInternal();
        } catch (Throwable th) {
            return List.of();
        }
    }

    private List<SemanticHighlight> highlightsInternal() {
        var colors = new ArrayList<SemanticHighlight>(task.roots.size());
        for (int i = 0; i < task.roots.size(); i++) {
            final var root = task.roots.get(i);
            final var color = new SemanticHighlight();
            color.uri = root.getSourceFile().toUri().toString();
            try {
                if (task == null || task.task == null) break;
                final var highlighter = new SemanticHighlighter(task, checker);
                highlighter.scan(root, color);
            } catch (Throwable th) {
            }

            colors.add(color);
        }
        return colors;
    }
}