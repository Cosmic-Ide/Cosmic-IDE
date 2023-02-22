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
package com.tyron.javacompletion.completion;

import com.google.auto.value.AutoValue;

/** Options for customizing candidate-generated text edit content. */
@AutoValue
public abstract class TextEditOptions {
    public static final TextEditOptions DEFAULT =
            TextEditOptions.builder().setAppendMethodArgumentSnippets(false).build();

    /**
     * True if the text edit should contain snippets of method arguments when completing a method
     * candidate.
     */
    public abstract boolean getAppendMethodArgumentSnippets();

    public static Builder builder() {
        return new AutoValue_TextEditOptions.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setAppendMethodArgumentSnippets(boolean value);

        public abstract TextEditOptions build();
    }
}