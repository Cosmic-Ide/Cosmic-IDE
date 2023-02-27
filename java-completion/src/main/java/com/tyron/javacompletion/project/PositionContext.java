/*
 *  This file is part of CodeAssist.
 *
 *  CodeAssist is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CodeAssist is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with CodeAssist.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tyron.javacompletion.project;

import com.google.auto.value.AutoValue;
import com.tyron.javacompletion.parser.FileContentFixer;
import com.sun.source.tree.ErroneousTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.nio.file.Path;
import java.util.Optional;
import com.tyron.javacompletion.logging.JLogger;
import com.tyron.javacompletion.model.EntityScope;
import com.tyron.javacompletion.model.FileScope;
import com.tyron.javacompletion.model.Module;
import com.tyron.javacompletion.parser.LineMapUtil;
import com.tyron.javacompletion.parser.TreePathFormatter;

/** All information inferred from a given cursor position of a file. */
@AutoValue
public abstract class PositionContext {
    private static final JLogger logger = JLogger.createForEnclosingClass();

    public abstract EntityScope getScopeAtPosition();

    public abstract Module getModule();

    public abstract FileScope getFileScope();

    public abstract TreePath getTreePath();

    /**
     * Gets position of the parsed content of the file.
     *
     * <p>Because the content of a file may be modified by {@link FileContentFixer}, the parsed
     * content may be different from the original content of the file. So this position can not be
     * used in the context of the original position.
     */
    public abstract int getPosition();

    public abstract EndPosTable getEndPosTable();

    public static Optional<PositionContext> createForPosition(
            ModuleManager moduleManger, Path filePath, int line, int column) {
        Optional<FileItem> fileItem = moduleManger.getFileItem(filePath);
        if (!fileItem.isPresent()) {
            return Optional.empty();
        }
        return createForPosition(fileItem.get().getModule(), filePath, line, column);
    }

    /**
     * Creates a {@link PositionContext} instance based on the given file path and position.
     *
     * @param module the module of the project
     * @param filePath normalized path of the file to be completed
     * @param line 0-based line number of the completion point
     * @param column 0-based character offset from the beginning of the line to the completion point
     */
    public static Optional<PositionContext> createForPosition(
            Module module, Path filePath, int line, int column) {
        Optional<FileScope> inputFileScope = module.getFileScope(filePath.toString());
        if (!inputFileScope.isPresent()) {
            return Optional.empty();
        }

        if (!inputFileScope.get().getCompilationUnit().isPresent()) {
            return Optional.empty();
        }

        LineMap lineMap = inputFileScope.get().getLineMap().get();
        int position = LineMapUtil.getPositionFromZeroBasedLineAndColumn(lineMap, line, column);
        return Optional.of(createForFixedPosition(module, inputFileScope.get(), position));
    }

    /**
     * Creates a {@link PositionContext} instance based on the given position.
     *
     * @param module the module of the project
     * @param inputFileScope normalized path of the file to be completed
     * @param position 0-based offset of the file content that may be updated by {@link
     *     FileContentFixer}
     */
    public static PositionContext createForFixedPosition(
            Module module, FileScope inputFileScope, int position) {
        JCCompilationUnit compilationUnit = inputFileScope.getCompilationUnit().get();
        EntityScope scopeAtPosition = inputFileScope.getEntityScopeAt(position - 1);
        PositionAstScanner scanner = new PositionAstScanner(compilationUnit.endPositions, position);
        logger.fine("Starting PositionAstScanner, position: %s", position);
        TreePath treePath = scanner.scan(compilationUnit, null);
        logger.fine("TreePath for position: %s", TreePathFormatter.formatTreePath(treePath));

        return new AutoValue_PositionContext(
                scopeAtPosition, module, inputFileScope, treePath, position, compilationUnit.endPositions);
    }

    /** A {@link TreePathScanner} that returns the tree path enclosing the given position. */
    private static class PositionAstScanner extends TreePathScanner<TreePath, Void> {
        private final EndPosTable endPosTable;
        private final int position;

        private PositionAstScanner(EndPosTable endPosTable, int position) {
            this.endPosTable = endPosTable;
            this.position = position;
        }

        @Override
        public TreePath scan(Tree tree, Void unused) {
            if (tree == null) {
                return null;
            }

            JCTree jcTree = (JCTree) tree;
            int startPosition = jcTree.getStartPosition();
            int endPosition = jcTree.getEndPosition(endPosTable);
            boolean positionInNodeRange =
                    (startPosition < 0 || startPosition <= position)
                            && (position < endPosition || endPosition < 0);
            logger.fine(
                    "PositionAstScanner: visiting node: %s, start: %s, end: %s.%s",
                    tree.accept(new TreePathFormatter.TreeFormattingVisitor(), null),
                    jcTree.getStartPosition(),
                    jcTree.getEndPosition(endPosTable),
                    positionInNodeRange ? " ✔" : "");
            if (!positionInNodeRange) {
                return null;
            }
            TreePath currentPath = new TreePath(getCurrentPath(), tree);

            TreePath ret = super.scan(tree, null);
            if (ret != null) {
                return ret;
            }

            return (tree instanceof ErroneousTree) ? null : currentPath;
        }

        @Override
        public TreePath visitErroneous(ErroneousTree node, Void unused) {
            for (Tree tree : node.getErrorTrees()) {
                TreePath ret = scan(tree, unused);
                if (ret != null) {
                    return ret;
                }
            }
            return null;
        }

        @Override
        public TreePath reduce(TreePath r1, TreePath r2) {
            if (r1 != null) {
                return r1;
            }
            return r2;
        }
    }
}