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
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;

/** Result of completion request. */
@AutoValue
public abstract class CompletionResult {

    public abstract Path getFilePath();

    public abstract int getLine();

    public abstract int getColumn();

    public abstract String getPrefix();

    public abstract ImmutableList<CompletionCandidate> getCompletionCandidates();

    public abstract TextEditOptions getTextEditOptions();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_CompletionResult.Builder();
    }

    /**
     * Check if the completor is processing a completion request that is an incremental completion of
     * the cached completion.
     */
    boolean isIncrementalCompletion(Path filePath, int line, int column, String prefix) {
        if (!getFilePath().equals(filePath)) {
            return false;
        }
        if (getLine() != line) {
            return false;
        }
        if (getColumn() > column) {
            return false;
        }
        if (!prefix.startsWith(getPrefix())) {
            return false;
        }
        // FIXME: This may break for complicated Unicodes.
        return prefix.length() - getPrefix().length() == column - getColumn();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setFilePath(Path filePath);

        public abstract Builder setLine(int line);

        public abstract Builder setColumn(int column);

        public abstract Builder setPrefix(String prefix);

        public abstract Builder setCompletionCandidates(
                ImmutableList<CompletionCandidate> completionCandidates);

        public abstract Builder setTextEditOptions(TextEditOptions textEditOptions);

        public abstract CompletionResult build();
    }
}