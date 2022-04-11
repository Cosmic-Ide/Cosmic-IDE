/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core;

import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.core.util.KeyKind;
import org.eclipse.jdt.internal.core.util.KeyToSignature;

/**
 * Utility class to decode or create a binding key.
 * <p>
 * This class is not intended to be subclassed by clients.
 * </p>
 *
 * @see org.eclipse.jdt.core.dom.IBinding#getKey()
 * @since 3.1
 */
public final class BindingKey {

	private String key;

	/**
	 * Creates a new binding key.
	 *
	 * @param key the key to decode
	 */
	public BindingKey(String key) {
		this.key = key;
	}

	/**
	 * Creates a new array type binding key from the given type binding key and the given array dimension.
	 * <p>
	 * For example:
	 * <pre>
	 * <code>
	 * createArrayTypeBindingKey("Ljava/lang/Object;", 1) -> "[Ljava/lang/Object;"
	 * createArrayTypeBindingKey("I", 2) -> "[[I"
	 * </code>
	 * </pre>
	 *
	 * @param typeKey the binding key of the given type
	 * @param arrayDimension the given array dimension
	 * @return a new array type binding key
	 */
	public static String createArrayTypeBindingKey(String typeKey, int arrayDimension) {
		// Note this implementation is heavily dependent on ArrayTypeBinding#computeUniqueKey()
		StringBuilder buffer = new StringBuilder();
		while (arrayDimension-- > 0)
			buffer.append('[');
		buffer.append(typeKey);
		return buffer.toString();
	}

	/**
	 * Creates a new parameterized type binding key from the given generic type binding key and the given argument type binding keys.
	 * If the argument type keys array is empty, then a raw type binding key is created.
	 * <p>
	 * For example:
	 * <pre>
	 * <code>
	 * createParameterizedTypeBindingKey(
	 *     "Ljava/util/Map&lt;TK;TV;&gt;;",
	 *     new String[] {"Ljava/lang/String;", "Ljava/lang/Object;"}) -&gt;
	 *       "Ljava/util/Map&lt;Ljava/lang/String;Ljava/lang/Object;&gt;;"
	 * createParameterizedTypeBindingKey(
	 *     "Ljava/util/List&lt;TE;&gt;;", new String[] {}) -&gt;
	 *       "Ljava/util/List&lt;&gt;;"
	 * </code>
	 * </pre>
	 *
	 * @param genericTypeKey the binding key of the generic type
	 * @param argumentTypeKeys the possibly empty list of binding keys of argument types
	 * @return a new parameterized type binding key
	 */
	public static String createParameterizedTypeBindingKey(String genericTypeKey, String[] argumentTypeKeys) {
		// Note this implementation is heavily dependent on ParameterizedTypeBinding#computeUniqueKey() and its subclasses
		StringBuilder buffer = new StringBuilder();
		buffer.append(Signature.getTypeErasure(genericTypeKey));
		buffer.insert(buffer.length()-1, '<');
		for (int i = 0, length = argumentTypeKeys.length; i < length; i++) {
			String argumentTypeKey = argumentTypeKeys[i];
			buffer.insert(buffer.length()-1, argumentTypeKey);
		}
		buffer.insert(buffer.length()-1, '>');
		return buffer.toString();
	}

	/**
	 * Creates a new type binding key from the given type name. The type name must be either
	 * a fully qualified name, an array type name or a primitive type name.
	 * If the type name is fully qualified, then it is expected to be dot-based.
	 * Note that inner types, generic types and parameterized types are not supported.
	 * <p>
	 * For example:
	 * <pre>
	 * <code>
	 * createTypeBindingKey("int") -> "I"
	 * createTypeBindingKey("java.lang.String") -> "Ljava/lang/String;"
	 * createTypeBindingKey("boolean[]") -> "[Z"
	 * </code>
	 * </pre>
	 *
	 * @param typeName the possibly qualified type name
	 * @return a new type binding key
	 */
	public static String createTypeBindingKey(String typeName) {
		// Note this implementation is heavily dependent on TypeBinding#computeUniqueKey() and its subclasses
		return Signature.createTypeSignature(typeName.replace('.', '/'), true/*resolved*/);
	}

	/**
	 * Creates a new type variable binding key from the given type variable name and the given declaring key.
	 * The declaring key can either be a type binding key or a method binding key.
	 * <p>
	 * For example:
	 * <pre>
	 * <code>
	 * createTypeVariableBindingKey("T", "Ljava/util/List&lt;TE;&gt;;") -&gt;
	 *   "Ljava/util/List&lt;TE;&gt;;:TT;"
	 * createTypeVariableBindingKey("SomeTypeVariable", "Lp/X;.foo()V") -&gt;
	 *   "Lp/X;.foo()V:TSomeTypeVariable;"
	 * </code>
	 * </pre>
	 *
	 * @param typeVariableName the name of the given type variable
	 * @param declaringKey the binding key of the type or method the type variable belongs to
	 * @return a new type variable binding key
	 */
	public static String createTypeVariableBindingKey(String typeVariableName, String declaringKey) {
		// Note this implementation is heavily dependent on TypeVariableBinding#computeUniqueKey()
		StringBuilder buffer = new StringBuilder();
		buffer.append(declaringKey);
		buffer.append(':');
		buffer.append('T');
		buffer.append(typeVariableName);
		buffer.append(';');
		return buffer.toString();
	}

	/**
	 * Creates a new wildcard type binding key from the given type binding key and the given wildcard kind
	 * (one of {@link Signature#C_STAR}, {@link Signature#C_SUPER}, or {@link Signature#C_EXTENDS}.
	 * If the wildcard is {@link Signature#C_STAR}, the given type binding key is ignored.
	 * <p>
	 * For example:
	 * <pre>
	 * <code>
	 * createWilcardTypeBindingKey(null, Signature.C_STAR) -&gt; "*"
	 * createWilcardTypeBindingKey("Ljava/util/List&lt;TE;&gt;;",
	 *    Signature.C_SUPER) -&gt; "-Ljava/util/List&lt;TE;&gt;;"
	 * createWilcardTypeBindingKey("Ljava/util/ArrayList;", Signature.C_EXTENDS) -&gt;
	 *    "+Ljava/util/ArrayList;"
	 * </code>
	 * </pre>
	 *
	 * @param typeKey the binding key of the given type
	 * @param kind one of {@link Signature#C_STAR}, {@link Signature#C_SUPER}, or {@link Signature#C_EXTENDS}
	 * @return a new wildcard type binding key
	 * @deprecated  This method is missing crucial information necessary for proper wildcard binding key creation.
	 * @see org.eclipse.jdt.core.BindingKey#createWildcardTypeBindingKey(String, char, String, int)
	 */
	public static String createWilcardTypeBindingKey(String typeKey, char kind) {
		// Note this implementation is supposed to closely follow the behavior in WildcardBinding#computeUniqueKey()
		// but it doesn't and hence the deprecation. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=234609
		switch (kind) {
			case Signature.C_STAR:
				return "*"; //$NON-NLS-1$
			case Signature.C_SUPER:
				return '-' + typeKey;
			case Signature.C_EXTENDS:
				return '+' + typeKey;
		}
		return null;
	}

	/**
	 * Creates a new wildcard type binding key from the given generic type binding key, the given wildcard
	 * kind (one of {@link Signature#C_STAR}, {@link Signature#C_SUPER}, or {@link Signature#C_EXTENDS}
	 * the given bound type binding key and the given rank. If the wildcard kind is {@link Signature#C_STAR},
	 * the given bound type binding key is ignored.
	 * <p>
	 * For example:
	 * <pre>
	 * <code>
	 * createWildcardTypeBindingKey("Ljava/util/ArrayList;", Signature.C_STAR, null, 0) -&gt; "Ljava/util/ArrayList;{0}*"
	 * createWildcardTypeBindingKey("Ljava/util/ArrayList;", Signature.C_SUPER, "Ljava/lang/String;", 0) -&gt; "Ljava/util/ArrayList;{0}-Ljava/lang/String;"
	 * createWildcardTypeBindingKey("Ljava/util/HashMap;", Signature.C_EXTENDS, "Ljava/lang/String;", 1) -&gt;
	 *    "Ljava/util/HashMap;{1}+Ljava/lang/String;"
	 * </code>
	 * </pre>
	 *
	 * @param genericTypeKey the binding key of the generic type
	 * @param boundKind one of {@link Signature#C_STAR}, {@link Signature#C_SUPER}, or {@link Signature#C_EXTENDS}
	 * @param boundTypeKey the binding key of the bounding type.
	 * @param rank the relative position of this wild card type in the parameterization of the generic type.
	 * @return a new wildcard type binding key
	 * @since 3.5
	 */

	public static String createWildcardTypeBindingKey(String genericTypeKey, char boundKind, String boundTypeKey, int rank) {
		// Note this implementation is heavily dependent on WildcardBinding#computeUniqueKey()
		String wildCardKey;
		switch (boundKind) {
			case Signature.C_STAR:
				wildCardKey = new String(TypeConstants.WILDCARD_STAR);
				break;
			case Signature.C_SUPER:
				wildCardKey = new String(TypeConstants.WILDCARD_MINUS) + boundTypeKey;
				break;
			case Signature.C_EXTENDS:
				wildCardKey = new String(TypeConstants.WILDCARD_PLUS) + boundTypeKey;
				break;
			default:
				return null;
		}
		return genericTypeKey + '{' + rank + '}' + wildCardKey;
	}

	/**
	 * Returns the binding key of the declaring type of the element represented by this binding key. If the binding key
	 * does not represent a member or if the member doesn't have a declaring type, returns <code>null</code>.
	 *
	 * <p>
	 * Note that only binding keys for references to methods and fields
	 * are fully supported. The binding keys for declarations will not have type parameters.
	 *
	 * @return the type binding key or <code>null</code>
	 * @since 3.7.1
	 */
	public BindingKey getDeclaringType() {
		int end = this.key.lastIndexOf(Signature.C_DOT);
		if (end == -1) {
			end = this.key.lastIndexOf(Signature.C_DOLLAR); // for inner types
			if (end == -1) return null;
		}
		KeyKind kind = new KeyKind(this.key);
		kind.parse();
		if ((kind.flags & KeyKind.F_LOCAL_VAR) != 0) {
			// declaring type for locals doesn't make sense, hence return null.
			return null;
		}
		String typeKey = this.key.substring(0, end);
		if (typeKey.charAt(typeKey.length()-1) != Signature.C_SEMICOLON)  {
			typeKey += Signature.C_SEMICOLON;
		}
		return new BindingKey(typeKey);
	}
	/**
	 * Returns the thrown exception signatures of the element represented by this binding key.
	 * If this binding key does not  represent a method or does not throw any exception,
	 * returns an empty array.
	 *
	 * @return the thrown exceptions signatures
	 * @since 3.3
	 */
	public String[] getThrownExceptions() {
		KeyToSignature keyToSignature = new KeyToSignature(this.key, KeyToSignature.THROWN_EXCEPTIONS);
		keyToSignature.parse();
		return keyToSignature.getThrownExceptions();
	}

	/**
	 * Returns the type argument signatures of the element represented by this binding key.
	 * If this binding key doesn't represent a parameterized type or a parameterized method,
	 * returns an empty array.
	 *
	 * @return the type argument signatures
	 */
	public String[] getTypeArguments() {
		KeyToSignature keyToSignature = new KeyToSignature(this.key, KeyToSignature.TYPE_ARGUMENTS);
		keyToSignature.parse();
		return keyToSignature.getTypeArguments();
	}

	/**
	 * Returns whether this binding key represents a raw type.
	 *
	 * @return whether this binding key represents a raw type
	 */
	public boolean isRawType() {
		KeyKind kind = new KeyKind(this.key);
		kind.parse();
		return (kind.flags & KeyKind.F_RAW_TYPE) != 0;
	}

	/**
	 * Returns whether this binding key represents a parameterized type, or if its declaring type is a parameterized type.
	 *
	 * @return whether this binding key represents a parameterized type
	 */
	public boolean isParameterizedType() {
		KeyKind kind = new KeyKind(this.key);
		kind.parse();
		return (kind.flags & KeyKind.F_PARAMETERIZED_TYPE) != 0;
	}

	/**
	 * Returns whether this binding key represents a parameterized method, or if its declaring method is a parameterized method.
	 *
	 * @return whether this binding key represents a parameterized method
	 */
	public boolean isParameterizedMethod() {
		KeyKind kind = new KeyKind(this.key);
		kind.parse();
		return (kind.flags & KeyKind.F_PARAMETERIZED_METHOD) != 0;
	}

	/**
	 * Transforms this binding key into a resolved signature.
	 * If this binding key represents a field, the returned signature is
	 * the field type's signature.
	 *
	 * @return the resolved signature for this binding key
	 * @see Signature
	 * @since 3.2
	 */
	public String toSignature() {
		KeyToSignature keyToSignature = new KeyToSignature(this.key, KeyToSignature.SIGNATURE);
		keyToSignature.parse();
		return keyToSignature.signature.toString();
	}

	@Override
	public String toString() {
		return this.key;
	}
}
