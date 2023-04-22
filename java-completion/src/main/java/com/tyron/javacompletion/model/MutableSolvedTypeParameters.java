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

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A mutable {@link SolvedTypeParameters}.
 */
public class MutableSolvedTypeParameters extends SolvedTypeParameters {
    public final HashMap<String, SolvedType> typeVariableMap;

    public MutableSolvedTypeParameters() {
        this.typeVariableMap = new HashMap<>();
    }

    public static MutableSolvedTypeParameters copyOf(SolvedTypeParameters solvedTypeParameters) {
        MutableSolvedTypeParameters ret = new MutableSolvedTypeParameters();
        ret.putAllTypeParameters(solvedTypeParameters.getTypeVariableMap());
        return ret;
    }

    @Override
    public ImmutableMap<String, SolvedType> getTypeVariableMap() {
        return ImmutableMap.copyOf(typeVariableMap);
    }

    @Override
    public Optional<SolvedType> getTypeParameter(String name) {
        return Optional.ofNullable(typeVariableMap.get(name));
    }

    @Override
    public Builder toBuilder() {
        throw new UnsupportedOperationException();
    }

    public void putTypeParameter(String name, SolvedType solvedType) {
        typeVariableMap.put(name, solvedType);
    }

    public void putAllTypeParameters(
            Map<String, SolvedType> allTypeParameters) {
        typeVariableMap.putAll(allTypeParameters);
    }

    public void removeTypeParameter(String name) {
        typeVariableMap.remove(name);
    }

    public SolvedTypeParameters toImmutable() {
        return SolvedTypeParameters.builder().putTypeParameters(typeVariableMap).build();
    }
}