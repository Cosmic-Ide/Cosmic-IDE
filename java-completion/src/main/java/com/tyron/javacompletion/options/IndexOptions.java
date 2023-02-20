package com.tyron.javacompletion.options;

import com.google.auto.value.AutoValue;

/** Options on how a Java file should be indexed. */
@AutoValue
public abstract class IndexOptions {
    /** Indexes everything possible. */
    public static final IndexOptions.Builder FULL_INDEX_BUILDER =
            IndexOptions.builder().setShouldIndexMethodContent(true).setShouldIndexPrivate(true);
    /** Indexes only public classes/methods/etc... without indexing the contents. */
    public static final IndexOptions.Builder NON_PRIVATE_BUILDER =
            IndexOptions.builder().setShouldIndexPrivate(false).setShouldIndexMethodContent(false);

    public abstract boolean shouldIndexPrivate();

    public abstract boolean shouldIndexMethodContent();

    public static Builder builder() {
        return new AutoValue_IndexOptions.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract IndexOptions build();

        public abstract Builder setShouldIndexPrivate(boolean value);

        public abstract Builder setShouldIndexMethodContent(boolean value);
    }
}