package com.tyron.javacompletion.completion;

import com.google.auto.value.AutoValue;

/** Options for customizing candidate-generated text edit content. */
@AutoValue
public abstract class TextEditOptions {
    public static final TextEditOptions DEFAULT =
            TextEditOptions.builder().setAppendMethodArgumentSnippets(false).build();

    /**
     * True if the text edit should contain snippets of method arguments when completing a method
     * candidate.
     */
    public abstract boolean getAppendMethodArgumentSnippets();

    public static Builder builder() {
        return new AutoValue_TextEditOptions.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setAppendMethodArgumentSnippets(boolean value);

        public abstract TextEditOptions build();
    }
}