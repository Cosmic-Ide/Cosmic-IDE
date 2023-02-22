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
package com.tyron.javacompletion.completion;

import com.google.auto.value.AutoValue;
import com.tyron.javacompletion.parser.FileContentFixer;
import com.sun.source.tree.LineMap;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.nio.file.Path;
import com.tyron.javacompletion.file.FileManager;
import com.tyron.javacompletion.logging.JLogger;
import com.tyron.javacompletion.model.FileScope;
import com.tyron.javacompletion.parser.LineMapUtil;

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
        int position = LineMapUtil.getPositionFromZeroBasedLineAndColumn(getLineMap(), line, column);
        if (position < 0) {
            logger.warning(
                    "Position of (%s, %s): %s is negative when getting completion prefix for file %s",
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
        int position = LineMapUtil.getPositionFromZeroBasedLineAndColumn(getLineMap(), line, column);
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
        return content.subSequence(position, Math.min(content.length(), position + length)).toString();
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