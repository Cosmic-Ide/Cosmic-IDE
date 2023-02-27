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

import java.util.Optional;

/**
 * Type variable applying to generic type references.
 *
 * <p>Example:
 *
 * <pre>{@code
 * SomeType<TypeVar1, ? extends SomeType2, ? super SompType3>
 * }</pre>
 */
public interface TypeArgument {

    /** Replaces the simple type names with the type found in {@code solvedTypeParameters}. */
    Optional<? extends TypeArgument> applyTypeParameters(SolvedTypeParameters solvedTypeParameters);

    String toDisplayString();
}