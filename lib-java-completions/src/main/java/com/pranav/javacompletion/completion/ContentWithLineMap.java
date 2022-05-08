package com.pranav.javacompletion.completion;

import com.google.auto.value.AutoValue;
import com.pranav.javacompletion.file.FileManager;
import com.pranav.javacompletion.logging.JLogger;
import com.pranav.javacompletion.model.FileScope;
import com.pranav.javacompletion.parser.FileContentFixer;
import com.pranav.javacompletion.parser.LineMapUtil;

import org.openjdk.source.tree.LineMap;
import org.openjdk.tools.javac.tree.JCTree.JCCompilationUnit;

import java.nio.file.Path;

/**
 * Combines file content with line map for easier lookup.
 *
 * <p>The content and line map are from the original content, i.e. not modified by {@link
 * FileContentFixer}.
 */
@AutoValue
abstract class ContentWithLineMap {
    private static final JLogger logger = JLogger.createForEnclosingClass();

    abstract CharSequence getContent();

    abstract LineMap getLineMap();

    abstract Path getFilePath();

    /** Gets the content before cursor position (line, column) as prefix for completion. */
    String extractCompletionPrefix(int line, int column) {
        int position =
                LineMapUtil.getPositionFromZeroBasedLineAndColumn(getLineMap(), line, column);
        if (position < 0) {
            logger.warning(
                    "Position of (%s, %s): %s is negative when getting completion prefix for file"
                        + " %s",
                    line, column, position, getFilePath());
        }
        if (position >= getContent().length()) {
            logger.warning(
                    "Position of (%s, %s): %s is greater than the length of the content %s when "
                            + "getting completion prefix for file %s",
                    line, column, position, getContent().length(), getFilePath());
        }

        int start = position - 1;
        while (start >= 0 && Character.isJavaIdentifierPart(getContent().charAt(start))) {
            start--;
        }
        return getContent().subSequence(start + 1, position).toString();
    }

    String substring(int line, int column, int length) {
        int position =
                LineMapUtil.getPositionFromZeroBasedLineAndColumn(getLineMap(), line, column);
        if (position < 0) {
            logger.warning(
                    "Position of (%s, %s): %s is negative when getting substring for file %s",
                    line, column, position, getFilePath());
            return "";
        }
        CharSequence content = getContent();
        if (content.length() < position) {
            logger.warning(
                    "Position of (%s, %s): %s is greater than the length of the content %s when "
                            + "getting substring for file %s",
                    line, column, position, content.length(), getFilePath());
            return "";
        }
        return content.subSequence(position, Math.min(content.length(), position + length))
                .toString();
    }

    /** Create an instance from the path of a file and its parsed {@link FileScope} */
    static ContentWithLineMap create(FileScope fileScope, FileManager fileManager, Path filePath) {
        CharSequence content = fileManager.getFileContent(filePath).orElse(null);
        if (content == null) {
            logger.warning("Cannot get file content of %s", filePath);
            content = "";
        }

        JCCompilationUnit compilationUnit = fileScope.getCompilationUnit().get();
        LineMap lineMap = compilationUnit.getLineMap();

        return new AutoValue_ContentWithLineMap(content, lineMap, filePath);
    }
}
