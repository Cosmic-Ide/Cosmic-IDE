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