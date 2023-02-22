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
import java.util.List;
import java.util.stream.Collectors;

/** Type parameter for parameterized classe and method declarations. */
@AutoValue
public abstract class TypeParameter {
    public abstract String getName();

    public abstract ImmutableList<TypeReference> getExtendBounds();

    public static TypeParameter create(String name, List<TypeReference> extendBounds) {
        return new AutoValue_TypeParameter(name, ImmutableList.copyOf(extendBounds));
    }

    public String toDisplayString() {
        if (getExtendBounds().isEmpty()) {
            return getName();
        }

        return getName()
                + " extends "
                + getExtendBounds().stream()
                .map(b -> b.toDisplayString())
                .collect(Collectors.joining(", "));
    }
}