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