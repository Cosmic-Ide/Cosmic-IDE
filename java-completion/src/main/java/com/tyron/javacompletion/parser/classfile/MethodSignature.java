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
package com.tyron.javacompletion.parser.classfile;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.tyron.javacompletion.model.TypeParameter;
import com.tyron.javacompletion.model.TypeReference;

/** Parsed signature of a method. */
@AutoValue
public abstract class MethodSignature {
    public abstract ImmutableList<TypeParameter> getTypeParameters();

    public abstract ImmutableList<TypeReference> getParameters();

    public abstract TypeReference getResult();

    public abstract ImmutableList<TypeReference> getThrowsSignatures();

    public static Builder builder() {
        return new AutoValue_MethodSignature.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setTypeParameters(ImmutableList<TypeParameter> typeParameters);

        protected abstract ImmutableList.Builder<TypeReference> parametersBuilder();

        public Builder addParameter(TypeReference parameter) {
            parametersBuilder().add(parameter);
            return this;
        }

        public abstract Builder setResult(TypeReference result);

        protected abstract ImmutableList.Builder<TypeReference> throwsSignaturesBuilder();

        public Builder addThrowsSignature(TypeReference throwsSignature) {
            throwsSignaturesBuilder().add(throwsSignature);
            return this;
        }

        public abstract MethodSignature build();
    }
}