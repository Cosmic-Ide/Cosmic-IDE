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
package com.tyron.javacompletion.file;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.SimpleJavaFileObject;
import com.tyron.javacompletion.file.EditHistory.AppliedEdit;

/** Snapshot of the content of a file. */
public class FileSnapshot extends SimpleJavaFileObject {
    private static final Pattern LINE_END_PATTERN = Pattern.compile("$", Pattern.MULTILINE);

    private CharSequence originalContent;
    private final List<AppliedEdit> appliedEdits;
    private StringBuilder content;
    /** Maps line number to the position of the start of the line in the content string. */
    private final List<Integer> lineNumberMap;

    private FileSnapshot(URI fileUri, String content) {
        super(fileUri, Kind.SOURCE);
        this.content = new StringBuilder(content);
        this.originalContent = this.content;
        this.lineNumberMap = new ArrayList<>();
        this.appliedEdits = new LinkedList<>();
        remapLines();
    }

    /**
     * Loads the content of a file from {@code filename} and creates a {@link FileSnapshot} with the
     * content.
     */
    public static FileSnapshot create(URI fileUri, String content) throws IOException {
        return new FileSnapshot(fileUri, content);
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return content;
    }

    public String getContent() {
        return content.toString();
    }

    public EditHistory getEditHistory() {
        return EditHistory.create(originalContent.toString(), appliedEdits);
    }

    /**
     * Applies a change to the content.
     *
     * @param editRange the range of the content being modified. The original content within the range
     *     will be replaced by {@code newText}
     * @param newText the new content to replace the original content with {@code editRange}
     */
    public void applyEdit(TextRange editRange, Optional<Integer> rangeLength, String newText) {
        if (this.originalContent == this.content) {
            this.originalContent = this.content.toString();
        }
        appliedEdits.add(AppliedEdit.create(editRange, rangeLength, newText));

        int start = getPositionOffset(editRange.getStart());
        int end = getPositionOffset(editRange.getEnd());
        checkArgument(start <= end, "Range start is after range end.");

        if (rangeLength.isPresent()) {
            checkArgument(rangeLength.get() >= 0, "rangeLength %s is negative.", rangeLength.get());
            end = Math.min(end, start + rangeLength.get());
        }

        if (Strings.isNullOrEmpty(newText)) {
            if (start < end) {
                content.delete(start, end);
            }
        } else if (start < end) {
            content.replace(start, end, newText);
        } else {
            content.insert(start, newText);
        }

        remapLines();
    }

    public void setContent(String newText) {
        this.content = new StringBuilder(newText);

        remapLines();
    }

    private int getPositionOffset(TextPosition position) {
        checkArgument(
                position.getLine() >= 0 && position.getLine() < lineNumberMap.size(),
                "Line number %s out of range.",
                position.getLine());
        checkArgument(
                position.getCharacter() >= 0,
                "Position character %s is negative.",
                position.getCharacter());
        return Math.min(
                content.length(), lineNumberMap.get(position.getLine()) + position.getCharacter());
    }

    private void remapLines() {
        lineNumberMap.clear();

        Matcher matcher = LINE_END_PATTERN.matcher(content);
        int start = 0;
        lineNumberMap.add(start);
        while (matcher.find(start)) {
            int end = matcher.end();
            if (end == content.length()) {
                break;
            }
            if (content.charAt(end) == '\r'
                    && end < content.length() - 1
                    && content.charAt(end + 1) == '\n') {
                // Count for the extra "\n" in "\r\n" sequence.
                end++;
            }
            // Count for the matched line terminator.
            end++;
            start = end;
            lineNumberMap.add(start);
        }
    }

    @VisibleForTesting
    public static FileSnapshot createFromContent(String content) {
        return new FileSnapshot(URI.create("test://testContent"), content);
    }
}