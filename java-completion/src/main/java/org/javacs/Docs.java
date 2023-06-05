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

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Logger;

import javax.tools.StandardLocation;

public class Docs {

    private static final Path NOT_FOUND = Paths.get("");
    private static final Logger LOG = Logger.getLogger("main");
    private static Path cacheSrcZip;
    /**
     * File manager with source-path + platform sources, which we will use to look up individual source files
     */
    final SourceFileManager fileManager = new SourceFileManager();

    Docs(Set<Path> docPath) {
        var srcZipPath = srcZip();
        // Path to source .jars + src.zip
        var sourcePath = new ArrayList<Path>(docPath);
        if (srcZipPath != NOT_FOUND) {
            sourcePath.add(srcZipPath);
        }
        try {
            fileManager.setLocationFromPaths(StandardLocation.SOURCE_PATH, sourcePath);
            if (srcZipPath != NOT_FOUND) {
                fileManager.setLocationFromPaths(StandardLocation.MODULE_SOURCE_PATH, Set.of(srcZipPath));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path srcZip() {
        if (cacheSrcZip == null) {
            cacheSrcZip = findSrcZip();
        }
        if (cacheSrcZip == NOT_FOUND) {
            return NOT_FOUND;
        }
        try {
            var fs = FileSystems.newFileSystem(cacheSrcZip, Docs.class.getClassLoader());
            return fs.getPath("/");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path findSrcZip() {
        var javaHome = JavaHomeHelper.javaHome();
        String[] locations = {
                "lib/src.zip", "src.zip",
        };
        for (var rel : locations) {
            var abs = javaHome.resolve(rel);
            if (Files.exists(abs)) {
                LOG.info("Found " + abs);
                return abs;
            }
        }
        LOG.warning("Couldn't find src.zip in " + javaHome);
        return NOT_FOUND;
    }

    public static DocCommentTree getDocumentation(TreePath path) {
        final Tree tree = path.getLeaf();
        if (path.getCompilationUnit() instanceof JCCompilationUnit && tree instanceof JCTree) {
            JCCompilationUnit unit = (JCCompilationUnit) path.getCompilationUnit();
            if (unit.docComments != null) {
                return unit.docComments.getCommentTree((JCTree) tree);
            }
        }
        return null;
    }
}
