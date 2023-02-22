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
package com.tyron.javacompletion.model;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;

@AutoValue
public abstract class SolvedTypeParameters {
    public static final SolvedTypeParameters EMPTY = SolvedTypeParameters.builder().build();

    public abstract ImmutableMap<String, SolvedType> getTypeVariableMap();

    public Optional<SolvedType> getTypeParameter(String name) {
        return Optional.ofNullable(getTypeVariableMap().get(name));
    }


    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_SolvedTypeParameters.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        protected abstract ImmutableMap.Builder<String, SolvedType> typeVariableMapBuilder();

        public Builder putTypeParameter(String name, SolvedType solvedTypeParameter) {
            typeVariableMapBuilder().put(name, solvedTypeParameter);
            return this;
        }

        public Builder putTypeParameters(Map<String, SolvedType> solvedTypeParameters) {
            typeVariableMapBuilder().putAll(solvedTypeParameters);
            return this;
        }

        public abstract SolvedTypeParameters build();
    }
}