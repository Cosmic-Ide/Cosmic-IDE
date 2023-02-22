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

import java.util.Objects;
import com.tyron.javacompletion.file.TextPosition;

/**
 * Position in a text document expressed as zero-based line and character offset. A position is
 * between two characters like an 'insert' cursor in a editor.
 *
 * <p>See: https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#position
 */
public class Position extends TextPosition {
    private final int line;
    private final int character;

    // For GSON
    public Position() {
        this.line = 0;
        this.character = 0;
    }

    public Position(int line, int character) {
        this.line = line;
        this.character = character;
    }

    public static Position createFromTextPosition(TextPosition pos) {
        if (pos instanceof Position) {
            return (Position) pos;
        }
        return new Position(pos.getLine(), pos.getCharacter());
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public int getCharacter() {
        return character;
    }

    @Override
    public String toString() {
        return String.format("(%d, %d)", line, character);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Position)) {
            return false;
        }
        Position other = (Position) o;

        return this.line == other.line && this.character == other.character;
    }

    @Override
    public int hashCode() {
        return Objects.hash(line, character);
    }
}