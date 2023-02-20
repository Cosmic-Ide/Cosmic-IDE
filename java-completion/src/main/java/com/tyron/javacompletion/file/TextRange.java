package com.tyron.javacompletion.file;

import com.google.auto.value.AutoValue;

/**
 * A range in a text document expressed as (zero-based) start and end positions.
 *
 * <p>A range is comparable to a selection in an editor. Therefore the end position is exclusive.
 */
@AutoValue
public abstract class TextRange {
    /** @return the range's start position, inclusive. */
    public abstract TextPosition getStart();

    /** @return the range's end position, exclusive. */
    public abstract TextPosition getEnd();

    public static TextRange create(TextPosition start, TextPosition end) {
        return new AutoValue_TextRange(start, end);
    }
}