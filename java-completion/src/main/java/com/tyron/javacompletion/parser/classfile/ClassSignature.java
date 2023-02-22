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

/** Parsed signature of a class. */
@AutoValue
public abstract class ClassSignature {
    public abstract ImmutableList<TypeParameter> getTypeParameters();

    public abstract TypeReference getSuperClass();

    public abstract ImmutableList<TypeReference> getInterfaces();

    public static Builder builder() {
        return new AutoValue_ClassSignature.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        protected abstract ImmutableList.Builder<TypeParameter> typeParametersBuilder();

        public abstract Builder setTypeParameters(ImmutableList<TypeParameter> typeParameters);

        public Builder addTypeParameter(TypeParameter typeParameter) {
            typeParametersBuilder().add(typeParameter);
            return this;
        }

        public abstract Builder setSuperClass(TypeReference superClass);

        protected abstract ImmutableList.Builder<TypeReference> interfacesBuilder();

        public Builder addInterface(TypeReference iface) {
            interfacesBuilder().add(iface);
            return this;
        }

        public abstract ClassSignature build();
    }
}