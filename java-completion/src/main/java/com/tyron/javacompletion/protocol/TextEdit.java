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