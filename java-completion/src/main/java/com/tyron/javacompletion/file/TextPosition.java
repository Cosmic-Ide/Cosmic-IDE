package com.tyron.javacompletion.file;

import com.google.auto.value.AutoValue;

/**
 * Position in a text document expressed as zero-based line and character offset. A position is
 * between two characters like an 'insert' cursor in a editor.
 */
@AutoValue
public abstract class TextPosition {

    /** Gets the line position in a document (zero-based). */
    public abstract int getLine();

    /** Gets the character offset on a line in a document (zero-based). */
    public abstract int getCharacter();

    public static TextPosition create(int line, int character) {
        return new AutoValue_TextPosition(line, character);
    }
}