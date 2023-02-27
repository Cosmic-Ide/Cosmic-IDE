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