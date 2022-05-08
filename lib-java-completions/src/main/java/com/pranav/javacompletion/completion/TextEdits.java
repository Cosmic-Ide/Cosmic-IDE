package com.pranav.javacompletion.completion;

import com.google.common.base.Joiner;
import com.pranav.javacompletion.model.FileScope;
import com.pranav.javacompletion.project.FileItem;
import com.pranav.javacompletion.project.ModuleManager;
import com.pranav.javacompletion.protocol.Position;
import com.pranav.javacompletion.protocol.Range;
import com.pranav.javacompletion.protocol.TextEdit;

import org.openjdk.source.tree.CompilationUnitTree;
import org.openjdk.source.tree.IdentifierTree;
import org.openjdk.source.tree.ImportTree;
import org.openjdk.source.tree.LineMap;
import org.openjdk.source.tree.MemberSelectTree;
import org.openjdk.source.tree.Tree;
import org.openjdk.source.util.TreeScanner;
import org.openjdk.tools.javac.tree.EndPosTable;
import org.openjdk.tools.javac.tree.JCTree;
import org.openjdk.tools.javac.tree.JCTree.JCCompilationUnit;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/** Generates text edits for completion items. */
public class TextEdits {
    private static final Joiner QUALIFIER_JOINER = Joiner.on(".");
    private static final long INVALID_POS = -1;

    public Optional<TextEdit> forImportClass(
            ModuleManager moduleManager, Path filePath, String fullClassName) {
        Optional<FileItem> fileItem = moduleManager.getFileItem(filePath);
        if (!fileItem.isPresent()) {
            return Optional.empty();
        }
        FileScope fileScope = fileItem.get().getFileScope();

        if (!fileScope.getCompilationUnit().isPresent()) {
            return Optional.empty();
        }

        JCCompilationUnit complationUnit = fileScope.getCompilationUnit().get();
        LineMap lineMap = fileScope.getLineMap().get();
        EndPosTable endPosTable = complationUnit.endPositions;

        ImportClassScanner scanner = new ImportClassScanner(fullClassName, lineMap, endPosTable);
        scanner.scan(complationUnit, null);

        int numNewLineBefore = 0;
        int numNewLineAfter = 0;
        Range range;

        if (scanner.afterImportPos != INVALID_POS) {
            // New line, then insert import statement;
            range = createRange(scanner.afterImportPos, lineMap);
            numNewLineBefore = 1;
        } else if (scanner.beforeImportPos != INVALID_POS) {
            // Insert import statement, then new line.
            range = createRange(scanner.beforeImportPos, lineMap);
            numNewLineAfter = 1;
        } else if (scanner.afterStaticImportsPos != INVALID_POS) {
            // 1 blank line between static import and the new import, so two new lines.
            range = createRange(scanner.afterStaticImportsPos, lineMap);
            numNewLineBefore = 2;
        } else if (scanner.afterPackagePos != INVALID_POS) {
            // 1 blank line between package statement and the new import, so two new lines.
            range = createRange(scanner.afterPackagePos, lineMap);
            numNewLineBefore = 2;
        } else {
            // No package or import statements, insert to the first line and add a blank line below.
            range = new Range(new Position(0, 0), new Position(0, 0));
            numNewLineAfter = 2;
        }

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < numNewLineBefore; i++) {
            sb.append('\n');
        }
        sb.append("import ").append(fullClassName).append(';');
        for (int i = 0; i < numNewLineAfter; i++) {
            sb.append('\n');
        }

        return Optional.of(new TextEdit(range, sb.toString()));
    }

    private static Range createRange(long pos, LineMap lineMap) {
        Position position =
                new Position(
                        (int) lineMap.getLineNumber(pos) - 1,
                        (int) lineMap.getColumnNumber(pos) - 1);
        return new Range(position, position);
    }

    private static class ImportClassScanner extends TreeScanner<Void, Void> {
        private final String fullClassName;
        private final LineMap lineMap;
        private final EndPosTable endPosTable;

        private long afterPackagePos = INVALID_POS;
        private long beforeImportPos = INVALID_POS;
        private long afterImportPos = INVALID_POS;
        private long afterStaticImportsPos = INVALID_POS;
        private boolean isImported;

        private ImportClassScanner(String fullClassName, LineMap lineMap, EndPosTable endPosTable) {
            this.fullClassName = fullClassName;
            this.lineMap = lineMap;
            this.endPosTable = endPosTable;
        }

        @Override
        public Void visitCompilationUnit(CompilationUnitTree node, Void unused) {
            if (node.getPackageName() != null) {
                // It's weird that package end pos doesn't contain the ending semicolon.
                afterPackagePos = endPosTable.getEndPos((JCTree) node.getPackageName()) + 1;
            }

            if (node.getImports() != null) {
                for (ImportTree importTree : node.getImports()) {
                    scan(importTree, null);
                }
            }
            return null;
        }

        @Override
        public Void visitImport(ImportTree node, Void unused) {
            if (node.isStatic()) {
                afterStaticImportsPos = endPosTable.getEndPos((JCTree) node);
                return null;
            }

            String importedName = nameTreeToQualifiedName(node.getQualifiedIdentifier());
            int cmp = importedName.compareTo(fullClassName);
            if (cmp < 0) {
                // The existing import statement should be above the new one.
                afterImportPos = endPosTable.getEndPos((JCTree) node);
            } else if (cmp > 0) {
                // The new import statement should be above the existing one, and also
                // previous existing ones.
                if (beforeImportPos == INVALID_POS) {
                    beforeImportPos = ((JCTree) node).getStartPosition();
                }
            } else {
                isImported = true;
            }

            return null;
        }

        private static String nameTreeToQualifiedName(Tree name) {
            Deque<String> stack = new ArrayDeque<>();
            while (name instanceof MemberSelectTree) {
                MemberSelectTree qualifiedName = (MemberSelectTree) name;
                stack.addFirst(qualifiedName.getIdentifier().toString());
                name = qualifiedName.getExpression();
            }
            stack.addFirst(((IdentifierTree) name).getName().toString());
            return QUALIFIER_JOINER.join(stack);
        }
    }
}
