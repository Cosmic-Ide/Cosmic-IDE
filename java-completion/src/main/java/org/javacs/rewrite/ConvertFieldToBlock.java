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
import org.javacs.CompilerProvider;

import java.nio.file.Path;
import java.util.Map;

import javax.lang.model.element.Modifier;

public class ConvertFieldToBlock implements Rewrite {
    final Path file;
    final int position;

    public ConvertFieldToBlock(Path file, int position) {
        this.file = file;
        this.position = position;
    }

    @Override
    public Map<Path, TextEdit[]> rewrite(CompilerProvider compiler) {
        var task = compiler.parse(file);
        var trees = Trees.instance(task.task);
        var pos = trees.getSourcePositions();
        var lines = task.root.getLineMap();
        var variable = ConvertVariableToStatement.findVariable(task, position);
        if (variable == null) {
            return CANCELLED;
        }
        var expression = variable.getInitializer();
        if (!ConvertVariableToStatement.isExpressionStatement(expression)) {
            return CANCELLED;
        }
        var start = pos.getStartPosition(task.root, variable);
        var end = pos.getStartPosition(task.root, expression);
        var startLine = (int) lines.getLineNumber(start);
        var startColumn = (int) lines.getColumnNumber(start);
        var startPos = new Position(startLine - 1, startColumn - 1);
        var endLine = (int) lines.getLineNumber(end);
        var endColumn = (int) lines.getColumnNumber(end);
        var endPos = new Position(endLine - 1, endColumn - 1);
        var deleteLhs = new Range(startPos, endPos);
        var fixLhs = new TextEdit(deleteLhs, "{ ");
        if (variable.getModifiers().getFlags().contains(Modifier.STATIC)) {
            fixLhs.setNewText("static { ");
        }
        var right = pos.getEndPosition(task.root, variable);
        var rightLine = (int) lines.getLineNumber(right);
        var rightColumn = (int) lines.getColumnNumber(right);
        var rightPos = new Position(rightLine - 1, rightColumn - 1);
        var insertRight = new Range(rightPos, rightPos);
        var fixRhs = new TextEdit(insertRight, " }");
        TextEdit[] edits = {fixLhs, fixRhs};
        return Map.of(file, edits);
    }
}
