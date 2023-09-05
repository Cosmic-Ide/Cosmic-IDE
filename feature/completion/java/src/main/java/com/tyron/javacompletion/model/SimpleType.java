/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
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
package com.tyron.javacompletion.model;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/**
 * A type without information about its package name and enclosing classes.
 */
@AutoValue
public abstract class SimpleType {
    public static Builder builder() {
        return new AutoValue_SimpleType.Builder();
    }

    public abstract boolean isPrimitive();

    public abstract String getSimpleName();

    public abstract ImmutableList<TypeArgument> getTypeArguments();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setPrimitive(boolean isPrimitive);

        public abstract Builder setSimpleName(String simpleName);

        public abstract Builder setTypeArguments(ImmutableList<TypeArgument> typeArguments);

        protected abstract ImmutableList.Builder<TypeArgument> typeArgumentsBuilder();

        public void addTypeArgument(TypeArgument typeArgument) {
            typeArgumentsBuilder().add(typeArgument);
        }

        public abstract SimpleType build();
    }
}