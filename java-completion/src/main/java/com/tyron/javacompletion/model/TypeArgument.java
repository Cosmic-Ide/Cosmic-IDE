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