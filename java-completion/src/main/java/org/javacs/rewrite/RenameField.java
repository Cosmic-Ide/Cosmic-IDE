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

import org.eclipse.lsp4j.TextEdit;
import org.javacs.CompilerProvider;

import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

public class RenameField implements Rewrite {
    private static final Logger LOG = Logger.getLogger("main");
    final String className, fieldName, newName;

    public RenameField(String className, String fieldName, String newName) {
        this.className = className;
        this.fieldName = fieldName;
        this.newName = newName;
    }

    @Override
    public Map<Path, TextEdit[]> rewrite(CompilerProvider compiler) {
        LOG.info("Rewrite " + className + "#" + fieldName + " to " + newName + "...");
        var paths = compiler.findMemberReferences(className, fieldName);
        if (paths.length == 0) {
            LOG.warning("...no references to " + className + "#" + fieldName);
            return Map.of();
        }
        LOG.info("...check " + paths.length + " files for references");
        try (var compile = compiler.compile(paths)) {
            var helper = new RenameHelper(compile);
            var edits = helper.renameField(compile.roots, className, fieldName, newName);
            return edits;
        }
    }
}
