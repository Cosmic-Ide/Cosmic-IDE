package com.tyron.javacompletion.file;


import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;

@AutoValue
public abstract class EditHistory {
    public abstract String getOriginalContent();

    public abstract ImmutableList<AppliedEdit> getAppliedEdits();

    public static EditHistory create(String originalContent, List<AppliedEdit> appliedEdits) {
        return new AutoValue_EditHistory(originalContent, ImmutableList.copyOf(appliedEdits));
    }

    @AutoValue
    public abstract static class AppliedEdit {
        public abstract TextRange getTextRange();

        public abstract Optional<Integer> getRangeLength();

        public abstract String getNewText();

        public static AppliedEdit create(
                TextRange textRange, Optional<Integer> rangeLength, String newText) {
            return new AutoValue_EditHistory_AppliedEdit(textRange, rangeLength, newText);
        }
    }
}