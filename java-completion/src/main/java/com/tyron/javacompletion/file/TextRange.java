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