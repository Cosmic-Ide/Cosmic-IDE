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

import com.google.common.collect.ImmutableList;

import java.util.Optional;

public class LambdaTypeReference extends TypeReference {

    private final String name;

    public LambdaTypeReference(String name) {
        this.name = name;
    }

    @Override
    public String getSimpleName() {
        return name;
    }

    @Override
    protected ImmutableList<String> getUnformalizedFullName() {
        return ImmutableList.of();
    }

    @Override
    protected SimpleType getSimpleType() {
        return null;
    }

    @Override
    public Optional<ImmutableList<String>> getPackageName() {
        return Optional.empty();
    }

    @Override
    public Optional<ImmutableList<SimpleType>> getEnclosingClasses() {
        return Optional.empty();
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    protected Builder autoToBuilder() {
        return null;
    }

    @Override
    public ImmutableList<TypeArgument> getTypeArguments() {
        return ImmutableList.of();
    }
}
