package com.tyron.javacompletion.protocol;

/**
 * A textual edit applicable to a text document.
 *
 * <p>See: https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#textedit
 */
public class TextEdit {
    /**
     * The range of the text document to be manipulated. To insert text into a document create a range
     * where start === end.
     */
    public Range range;

    /** The string to be inserted. For delete operations use an empty string. */
    public String newText;

    public TextEdit(Range range, String newText) {
        this.range = range;
        this.newText = newText;
    }
}