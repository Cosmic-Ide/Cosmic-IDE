/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

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

import androidx.annotation.NonNull;

import com.tyron.javacompletion.file.TextRange;

import java.util.Objects;

/**
 * A range in a text document expressed as (zero-based) start and end positions.
 *
 * <p>A range is comparable to a selection in an editor. Therefore the end position is exclusive.
 *
 * <p>NOTE: the start and end position are character offsets, not byte offsets.
 *
 * <p>This class corresponds to the Range type defined by Language Server Protocol:
 * <a href="https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#range">...</a>
 */
public class Range extends TextRange {
    private final Position start;
    private final Position end;

    // For GSON
    public Range() {
        this.start = new Position();
        this.end = new Position();
    }

    public Range(Position start, Position end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public Position getStart() {
        return start;
    }

    @Override
    public Position getEnd() {
        return end;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("{start=%s, end=%s}", start, end);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Range other)) {
            return false;
        }

        return Objects.equals(this.start, other.start) && Objects.equals(this.end, other.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }
}