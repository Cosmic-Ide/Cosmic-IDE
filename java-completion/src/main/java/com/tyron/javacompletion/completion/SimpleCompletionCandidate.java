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
import java.util.Optional;

@AutoValue
public abstract class SimpleCompletionCandidate implements CompletionCandidate {
    public static Builder builder() {
        return new AutoValue_SimpleCompletionCandidate.Builder();
    }

    @Override
    public Optional<String> getInsertPlainText(TextEditOptions textEditOptions) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getInsertSnippet(TextEditOptions textEditOptions) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getDetail() {
        return Optional.empty();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setName(String name);

        public abstract Builder setKind(Kind kind);

        public abstract SimpleCompletionCandidate build();
    }
}
