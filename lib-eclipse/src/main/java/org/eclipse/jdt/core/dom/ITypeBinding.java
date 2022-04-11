/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Herrmann - Contribution for
 *								Bug 429813 - [1.8][dom ast] IMethodBinding#getJavaElement() should return IMethod for lambda
 *******************************************************************************/

package org.eclipse.jdt.core.dom;

/**
 * A type binding represents a fully-resolved type. There are a number of
 * different kinds of type bindings:
 * <ul>
 * <li>a class - represents the class declaration;
 * possibly with type parameters</li>
 * <li>an interface - represents the class declaration;
 * possibly with type parameters</li>
 * <li>an enum - represents the enum declaration (enum types do not have
 * have type parameters)</li>
 * <li>an annotation - represents the annotation type declaration
 * (annotation types do not have have type parameters)</li>
 * <li>an array type - array types are referenced but not explicitly
 * declared</li>
 * <li>a primitive type (including the special return type <code>void</code>)
 * - primitive types are referenced but not explicitly declared</li>
 * <li>the null type - this is the special type of <code>null</code></li>
 * <li>a type variable - represents the declaration of a type variable;
 * possibly with type bounds</li>
 * <li>a wildcard type - represents a wild card used as a type argument in
 * a parameterized type reference</li>
 * <li>a raw type - represents a legacy non-parameterized reference to
 * a generic type</li>
 * <li>a parameterized type - represents an copy of a type declaration
 * with substitutions for its type parameters</li>
 * <li>a capture - represents a capture binding</li>
 * </ul>
 *
 * @see ITypeBinding#getDeclaredTypes()
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ITypeBinding extends IBinding {


	/**
	 * Answer an array type binding using the receiver and the given dimension.
	 *
	 * <p>If the receiver is an array binding, then the resulting dimension is the given dimension
	 * plus the dimension of the receiver. Otherwise the resulting dimension is the given
	 * dimension.</p>
	 *
	 * @param dimension the given dimension
	 * @return an array type binding
	 * @throws IllegalArgumentException:<ul>
	 * <li>if the receiver represents the void type</li>
	 * <li>if the resulting dimensions is lower than one or greater than 255</li>
	 * </ul>
	 * @since 3.3
	 */
	public ITypeBinding createArrayType(int dimension);

	/**
	 * Returns the binary name of this type binding.
	 * The binary name of a class is defined in the Java Language
	 * Specification 3rd edition, section 13.1.
	 * <p>
	 * Note that in some cases, the binary name may be unavailable.
	 * This may happen, for example, for a local type declared in
	 * unreachable code.
	 * </p>
	 *
	 * @return the binary name of this type, or <code>null</code>
	 * if the binary name is unknown
	 * @since 3.0
	 */
	public String getBinaryName();

	/**
	 * Returns the bound of this wildcard type if it has one.
	 * Returns <code>null</code> if this is not a wildcard type.
	 *
	 * @return the bound of this wildcard type, or <code>null</code> if none
	 * @see #isWildcardType()
	 * @see #isUpperbound()
	 * @see #getTypeBounds()
	 * @since 3.1
	 */
	public ITypeBinding getBound();

	/**
	 * Returns the generic type associated with this wildcard type, if it has one.
	 * Returns <code>null</code> if this is not a wildcard type.
	 *
	 * @return the generic type associated with this wildcard type, or <code>null</code> if none
	 * @see #isWildcardType()
	 * @since 3.5
	 */
	public ITypeBinding getGenericTypeOfWildcardType();

	/**
	 * Returns the rank associated with this wildcard type. The rank of this wild card type is the relative
	 * position of the wild card type in the parameterization of the associated generic type.
	 * Returns <code>-1</code> if this is not a wildcard type.
	 *
	 * @return the rank associated with this wildcard type, or <code>-1</code> if none
	 * @see #isWildcardType()
	 * @since 3.5
	 */
	public int getRank();

	/**
	 * Returns the binding representing the component type of this array type,
	 * or <code>null</code> if this is not an array type binding. The component
	 * type of an array might be an array type (with one dimension less than
	 * this array type).
	 *
	 * @return the component type binding, or <code>null</code> if this is
	 *   not an array type
	 * @see #getElementType()
	 * @since 3.2
	 */
	public ITypeBinding getComponentType();

	/**
	 * Returns a list of bindings representing all the fields declared
	 * as members of this class, interface, or enum type.
	 *
	 * <p>These include public, protected, default (package-private) access,
	 * and private fields declared by the class, but excludes inherited fields.
	 * Synthetic fields may or may not be included. Fields from binary types that
	 * reference unresolved types may not be included.</p>
	 *
	 * <p>Returns an empty list if the class, interface, or enum declares no fields,
	 * and for other kinds of type bindings that do not directly have members.</p>
	 *
	 * <p>The resulting bindings are in no particular order.</p>
	 *
	 * @return the list of bindings for the field members of this type,
	 *   or the empty list if this type does not have field members
	 */
	public IVariableBinding[] getDeclaredFields();

	/**
	 * Returns a list of method bindings representing all the methods and
	 * constructors declared for this class, interface, enum, or annotation
	 * type.
	 * <p>These include public, protected, default (package-private) access,
	 * and private methods Synthetic methods and constructors may or may not be
	 * included. Returns an empty list if the class, interface, or enum,
	 * type declares no methods or constructors, if the annotation type declares
	 * no members, or if this type binding represents some other kind of type
	 * binding. Methods from binary types that reference unresolved types may
	 * not be included.</p>
	 * <p>The resulting bindings are in no particular order.</p>
	 *
	 * @return the list of method bindings for the methods and constructors
	 *   declared by this class, interface, enum type, or annotation type,
	 *   or the empty list if this type does not declare any methods or constructors
	 */
	public IMethodBinding[] getDeclaredMethods();

	/**
	 * Returns the declared modifiers for this class or interface binding
	 * as specified in the original source declaration of the class or
	 * interface. The result may not correspond to the modifiers in the compiled
	 * binary, since the compiler may change them (in particular, for inner
	 * class emulation). The <code>getModifiers</code> method should be used if
	 * the compiled modifiers are needed. Returns -1 if this type does not
	 * represent a class or interface.
	 *
	 * @return the bit-wise or of <code>Modifier</code> constants
	 * @see #getModifiers()
	 * @see Modifier
	 * @deprecated  Use {@link #getModifiers()} instead.
	 * This method was never implemented properly and historically has simply
	 * delegated to the method <code>getModifiers</code>. Clients should call
	 * <code>getModifiers</code> method directly.
	 */
	public int getDeclaredModifiers();

	/**
	 * Returns a list of type bindings representing all the types declared as
	 * members of this class, interface, or enum type.
	 * These include public, protected, default (package-private) access,
	 * and private classes, interfaces, enum types, and annotation types
	 * declared by the type, but excludes inherited types. Returns an empty
	 * list if the type declares no type members, or if this type
	 * binding represents an array type, a primitive type, a type variable,
	 * a wildcard type, a capture, or the null type.
	 * The resulting bindings are in no particular order.
	 *
	 * @return the list of type bindings for the member types of this type,
	 *   or the empty list if this type does not have member types
	 */
	public ITypeBinding[] getDeclaredTypes();

	/**
	 * Returns the type binding representing the class, interface, or enum
	 * that declares this binding.
	 * <p>
	 * The declaring class of a member class, interface, enum, annotation
	 * type is the class, interface, or enum type of which it is a member.
	 * The declaring class of a local class or interface (including anonymous
	 * classes) is the innermost class or interface containing the expression
	 * or statement in which this type is declared.
	 * </p>
	 * <p>The declaring class of a type variable is the class in which the type
	 * variable is declared if it is declared on a type. It returns
	 * <code>null</code> otherwise.
	 * </p>
	 * <p>The declaring class of a capture binding is the innermost class or
	 * interface containing the expression or statement in which this capture is
	 * declared.
	 * </p>
	 * <p>Array types, primitive types, the null type, top-level types,
	 * wildcard types, recovered binding have no declaring class.
	 * </p>
	 *
	 * @return the binding of the type that declares this type, or
	 * <code>null</code> if none
	 */
	public ITypeBinding getDeclaringClass();

	/**
	 * Returns the method binding representing the method that declares this binding
	 * of a local type or type variable.
	 * <p>
	 * The declaring method of a local class or interface (including anonymous
	 * classes) is the innermost method containing the expression or statement in
	 * which this type is declared. Returns <code>null</code> if the type
	 * is declared in an initializer.
	 * </p>
	 * <p>
	 * The declaring method of a type variable is the method in which the type
	 * variable is declared if it is declared on a method. It
	 * returns <code>null</code> otherwise.
	 * </p>
	 * <p>Array types, primitive types, the null type, top-level types,
	 * wildcard types, capture bindings, and recovered binding have no
	 * declaring method.
	 * </p>
	 *
	 * @return the binding of the method that declares this type, or
	 * <code>null</code> if none
	 * @since 3.1
	 */
	public IMethodBinding getDeclaringMethod();

	/**
	 * If this type binding represents a local type, possibly an anonymous class, then:
	 * <ul>
	 * <li>If the local type is declared in the body of a method,
	 *   answers the binding of that declaring method.
	 * </li>
	 * <li>Otherwise, if the local type (an anonymous class in this case) is declared
	 *   in the initializer of a field, answers the binding of that declaring field.
	 * </li>
	 * <li>Otherwise, if the local type is declared in a static initializer or
	 *   an instance initializer, a method binding is returned to represent that initializer
	 *   (selector is an empty string in this case).
	 * </li>
	 * </ul>
	 * <p>
	 * If this type binding does not represent a local type, <code>null</code> is returned.
	 * </p>
	 * @return a method binding or field binding representing the member that
	 * contains the local type represented by this type binding,
	 * or null for non-local type bindings.
	 * @since 3.11
	 */
	public IBinding getDeclaringMember();

	/**
	 * Returns the binding of the module associated with the package in which
	 * this type is declared.
	 *
	 * <p>The module of a recovered type reference binding is the module
	 * associated with the package returned by {@link #getPackage()} ie either
	 * the module associated with the package of the enclosing type, or,
	 * if the type name is the name of a {@linkplain AST#resolveWellKnownType(String)
	 * well-known type},  the module associated with the package of the matching
	 * well-known type.</p>
	 *
	 * @return the binding of the module associated with the package
	 * in which this class, interface, enum, or annotation type is declared,
	 * or <code>null</code> if this type binding represents a primitive type, an array type,
	 * the null type, a type variable, a wildcard type or a capture binding.
	 *
	 * @since 3.14
	 */
	public default IModuleBinding getModule() {
		return null;
	}
	/**
	 * Returns the dimensionality of this array type, or <code>0</code> if this
	 * is not an array type binding.
	 *
	 * @return the number of dimension of this array type binding, or
	 *   <code>0</code> if this is not an array type
	 */
	public int getDimensions();

	/**
	 * Returns the binding representing the element type of this array type,
	 * or <code>null</code> if this is not an array type binding. The element
	 * type of an array type is never itself an array type.
	 *
	 * To get the type annotations on dimensions, clients should repeatedly
	 *  call getComponentType() and get the type annotations from there.
	 *
	 * @return the element type binding, or <code>null</code> if this is
	 *   not an array type
	 */
	public ITypeBinding getElementType();

	/**
	 * Returns the erasure of this type binding.
	 * <ul>
	 * <li>For parameterized types ({@link #isParameterizedType()})
	 * - returns the binding for the corresponding generic type.</li>
	 * <li>For raw types ({@link #isRawType()})
	 * - returns the binding for the corresponding generic type.</li>
	 * <li>For wildcard types ({@link #isWildcardType()})
	 * - returns the binding for the upper bound if it has one and
	 * java.lang.Object in other cases.</li>
	 * <li>For type variables ({@link #isTypeVariable()})
	 * - returns the binding for the erasure of the leftmost bound
	 * if it has bounds and java.lang.Object if it does not.</li>
	 * <li>For captures ({@link #isCapture()})
	 * - returns the binding for the erasure of the leftmost bound
	 * if it has bounds and java.lang.Object if it does not.</li>
	 * <li>For array types ({@link #isArray()}) - returns an array type of
	 * the same dimension ({@link #getDimensions()}) as this type
	 * binding for which the element type is the erasure of the element type
	 * ({@link #getElementType()}) of this type binding.</li>
	 * <li>For all other type bindings - returns the identical binding.</li>
	 * </ul>
	 *
	 * @return the erasure type binding
	 * @since 3.1
	 */
	public ITypeBinding getErasure();

	/**
	 * Returns the single abstract method that constitutes the single function
	 * contract (aside from any redeclarations of methods of <code>java.lang.Object</code>)
	 * of the receiver interface type or <code>null</code> if there is no such contract or if the receiver
	 * is not an interface.
	 * <p>
	 * The returned method binding may be synthetic and its {@link #getDeclaringClass() declaring type}
	 * may be a super interface type of this type binding.
	 * </p>
	 *
	 * @return the single abstract method that represents the single function contract, or
	 * <code>null</code> if the receiver is not a functional interface type
	 *
	 * @since 3.10
	 */
	public IMethodBinding getFunctionalInterfaceMethod();

	/**
	 * Returns a list of type bindings representing the direct superinterfaces
	 * of the class, interface, or enum type represented by this type binding.
	 * <p>
	 * If this type binding represents a class or enum type, the return value
	 * is an array containing type bindings representing all interfaces
	 * directly implemented by this class. The number and order of the interface
	 * objects in the array corresponds to the number and order of the interface
	 * names in the <code>implements</code> clause of the original declaration
	 * of this type.
	 * </p>
	 * <p>
	 * If this type binding represents an interface, the array contains
	 * type bindings representing all interfaces directly extended by this
	 * interface. The number and order of the interface objects in the array
	 * corresponds to the number and order of the interface names in the
	 * <code>extends</code> clause of the original declaration of this interface.
	 * </p>
	 * <p>
	 * If the class or enum implements no interfaces, or the interface extends
	 * no interfaces, or if this type binding represents an array type, a
	 * primitive type, the null type, a type variable, an annotation type,
	 * a wildcard type, or a capture binding, this method returns an array of
     * length 0.
	 * </p>
	 *
	 * @return the list of type bindings for the interfaces extended by this
	 *   class or enum, or interfaces extended by this interface, or otherwise
	 *   the empty list
	 */
	public ITypeBinding[] getInterfaces();

	/**
	 * Returns the compiled modifiers for this class, interface, enum,
	 * or annotation type binding.
	 * The result may not correspond to the modifiers as declared in the
	 * original source, since the compiler may change them (in particular,
	 * for inner class emulation).
	 * Returns 0 if this type does not represent a class, an interface, an enum, an annotation
	 * type or a recovered type.
	 *
	 * @return the compiled modifiers for this type binding or 0
	 * if this type does not represent a class, an interface, an enum, an annotation
	 * type or a recovered type.
	 */
	@Override
	public int getModifiers();

	/**
	 * Returns the unqualified name of the type represented by this binding
	 * if it has one.
	 * <ul>
	 * <li>For top-level types, member types, and local types,
	 * the name is the simple name of the type.
	 * Example: <code>"String"</code> or <code>"Collection"</code>.
	 * Note that the type parameters of a generic type are not included.</li>
	 * <li>For primitive types, the name is the keyword for the primitive type.
	 * Example: <code>"int"</code>.</li>
	 * <li>For the null type, the name is the string "null".</li>
	 * <li>For anonymous classes, which do not have a name,
	 * this method returns an empty string.</li>
	 * <li>For array types, the name is the unqualified name of the component
	 * type (as computed by this method) followed by "[]".
	 * Example: <code>"String[]"</code>. Note that the component type is never an
	 * an anonymous class.</li>
	 * <li>For type variables, the name is just the simple name of the
	 * type variable (type bounds are not included).
	 * Example: <code>"X"</code>.</li>
	 * <li>For type bindings that correspond to particular instances of a generic
	 * type arising from a parameterized type reference,
	 * the name is the unqualified name of the erasure type (as computed by this method)
	 * followed by the names (again, as computed by this method) of the type arguments
	 * surrounded by "&lt;&gt;" and separated by ",".
	 * Example: <code>"Collection&lt;String&gt;"</code>.
	 * </li>
	 * <li>For type bindings that correspond to particular instances of a generic
	 * type arising from a raw type reference, the name is the unqualified name of
	 * the erasure type (as computed by this method).
	 * Example: <code>"Collection"</code>.</li>
	 * <li>For wildcard types, the name is "?" optionally followed by
	 * a single space followed by the keyword "extends" or "super"
	 * followed a single space followed by the name of the bound (as computed by
	 * this method) when present.
	 * Example: <code>"? extends InputStream"</code>.
	 * </li>
     * <li>Capture types do not have a name. For these types,
     * and array types thereof, this method returns an empty string.</li>
	 * </ul>
	 *
	 * @return the unqualified name of the type represented by this binding,
	 * or the empty string if it has none
	 * @see #getQualifiedName()
	 */
	@Override
	public String getName();

	/**
	 * Returns the binding for the package in which this type is declared.
	 *
	 * <p>The package of a recovered type reference binding is either
	 * the package of the enclosing type, or, if the type name is the name of a
	 * {@linkplain AST#resolveWellKnownType(String) well-known type},
	 * the package of the matching well-known type.</p>
	 *
	 * @return the binding for the package in which this class, interface,
	 * enum, or annotation type is declared, or <code>null</code> if this type
	 * binding represents a primitive type, an array type, the null type,
	 * a type variable, a wildcard type, a capture binding.
	 */
	public IPackageBinding getPackage();

	/**
	 * Returns the fully qualified name of the type represented by this
	 * binding if it has one.
	 * <ul>
	 * <li>For top-level types, the fully qualified name is the simple name of
	 * the type preceded by the package name (or unqualified if in a default package)
	 * and a ".".
	 * Example: <code>"java.lang.String"</code> or <code>"java.util.Collection"</code>.
	 * Note that the type parameters of a generic type are not included.</li>
	 * <li>For members of top-level types, the fully qualified name is the
	 * simple name of the type preceded by the fully qualified name of the
	 * enclosing type (as computed by this method) and a ".".
	 * Example: <code>"java.io.ObjectInputStream.GetField"</code>.
	 * If the binding is for a member type that corresponds to a particular instance
	 * of a generic type arising from a parameterized type reference, the simple
	 * name of the type is followed by the fully qualified names of the type arguments
	 * (as computed by this method) surrounded by "&lt;&gt;" and separated by ",".
	 * Example: <code>"pkg.Outer.Inner&lt;java.lang.String&gt;"</code>.
	 * </li>
	 * <li>For primitive types, the fully qualified name is the keyword for
	 * the primitive type.
	 * Example: <code>"int"</code>.</li>
	 * <li>For the null type, the fully qualified name is the string
	 * "null".</li>
	 * <li>Local types (including anonymous classes) and members of local
	 * types do not have a fully qualified name. For these types, and array
	 * types thereof, this method returns an empty string.</li>
	 * <li>For array types whose component type has a fully qualified name,
	 * the fully qualified name is the fully qualified name of the component
	 * type (as computed by this method) followed by "[]".
	 * Example: <code>"java.lang.String[]"</code>.</li>
	 * <li>For type variables, the fully qualified name is just the name of the
	 * type variable (type bounds are not included).
	 * Example: <code>"X"</code>.</li>
	 * <li>For type bindings that correspond to particular instances of a generic
	 * type arising from a parameterized type reference,
	 * the fully qualified name is the fully qualified name of the erasure
	 * type followed by the fully qualified names of the type arguments surrounded by "&lt;&gt;" and separated by ",".
	 * Example: <code>"java.util.Collection&lt;java.lang.String&gt;"</code>.
	 * </li>
	 * <li>For type bindings that correspond to particular instances of a generic
	 * type arising from a raw type reference,
	 * the fully qualified name is the fully qualified name of the erasure type.
	 * Example: <code>"java.util.Collection"</code>. Note that the
	 * the type parameters are omitted.</li>
	 * <li>For wildcard types, the fully qualified name is "?" optionally followed by
	 * a single space followed by the keyword "extends" or "super"
	 * followed a single space followed by the fully qualified name of the bound
	 * (as computed by this method) when present.
	 * Example: <code>"? extends java.io.InputStream"</code>.
	 * </li>
    * <li>Capture types do not have a fully qualified name. For these types,
    * and array types thereof, this method returns an empty string.</li>
	 * </ul>
	 *
	 * @return the fully qualified name of the type represented by this
	 *    binding, or the empty string if it has none
	 * @see #getName()
	 * @since 2.1
	 */
	public String getQualifiedName();

	/**
	 * Returns the type binding for the superclass of the type represented
	 * by this class binding.
	 * <p>
	 * If this type binding represents any class other than the class
	 * <code>java.lang.Object</code>, then the type binding for the direct
	 * superclass of this class is returned. If this type binding represents
	 * the class <code>java.lang.Object</code>, then <code>null</code> is
	 * returned.
	 * <p>
	 * Loops that ascend the class hierarchy need a suitable termination test.
	 * Rather than test the superclass for <code>null</code>, it is more
	 * transparent to check whether the class is <code>Object</code>, by
	 * comparing whether the class binding is identical to
	 * <code>ast.resolveWellKnownType("java.lang.Object")</code>.
	 * </p>
	 * <p>
	 * If this type binding represents an interface, an array type, a
	 * primitive type, the null type, a type variable, an enum type,
	 * an annotation type, a wildcard type, or a capture binding then
	 * <code>null</code> is returned.
	 * </p>
	 *
	 * @return the superclass of the class represented by this type binding,
	 *    or <code>null</code> if none
	 * @see AST#resolveWellKnownType(String)
	 */
	public ITypeBinding getSuperclass();

	/**
	 * Returns the type annotations that this type reference is annotated with. Since JLS8,
	 * multiple instances of type bindings may be created if they are annotated with
	 * different type use annotations.
	 * <p>
	 * For example, the following three type references would produce three distinct type
	 * bindings for java.lang.String that share the same key:
	 * <ul>
	 * <li>java.lang.@Marker1 String</li>
	 * <li>java.lang.@Marker2 String</li>
	 * <li>java.lang.String</li>
	 * </ul>
	 * To get the type annotations on dimensions, clients should repeatedly call
	 * {@link #getComponentType()} and get the type annotations from there.

	 * @return type annotations specified on this type reference, or an empty array if
	 * no type use annotations are found.
	 * @see #getTypeDeclaration()
	 * @see #getKey()
	 * @since 3.10
	 */
	public IAnnotationBinding[] getTypeAnnotations();

	/**
	 * Returns the type arguments of this generic type instance, or the
	 * empty list for other type bindings.
	 * <p>
	 * Note that type arguments only occur on a type binding that represents
	 * an instance of a generic type corresponding to a parameterized type
	 * reference (e.g., <code>Collection&lt;String&gt;</code>).
	 * Do not confuse these with type parameters which only occur on the
	 * type binding corresponding directly to the declaration of the
	 * generic class or interface (e.g., <code>Collection&lt;T&gt;</code>).
	 * </p>
	 *
	 * @return the list of type bindings for the type arguments used to
	 * instantiate the corresponding generic type, or otherwise the empty list
	 * @see #getTypeDeclaration()
	 * @see #isGenericType()
	 * @see #isParameterizedType()
	 * @see #isRawType()
	 * @since 3.1
	 */
	public ITypeBinding[] getTypeArguments();

	/**
	 * Returns the upper type bounds of this type variable, wildcard, capture, or intersectionType.
	 * If the variable, wildcard, or capture had no explicit bound, then it returns an empty list.
     * <p>
     * Note that per construction, it can only contain one class or array type,
     * at most, and then it is located in first position.
     * </p>
     * <p>
     * Also note that array type bound may only occur in the case of a capture
     * binding, e.g. <code>capture-of ? extends Object[]</code>
     * </p>
	 *
	 * @return the list of upper bounds for this type variable, wildcard, capture, or intersection type
     * or otherwise the empty list
     * @see #isTypeVariable()
     * @see #isWildcardType()
	 * @see #isCapture()
	 * @see #isIntersectionType()
	 * @since 3.1
	 */
	public ITypeBinding[] getTypeBounds();

	/**
	 * Returns the binding for the type declaration corresponding to this type
	 * binding.
	 * <p>For parameterized types ({@link #isParameterizedType()})
	 * and most raw types ({@link #isRawType()}), this method returns the binding
	 * for the corresponding generic type ({@link #isGenericType()}.</p>
	 * <p>For raw member types ({@link #isRawType()}, {@link #isMember()})
	 * of a raw declaring class, the type declaration is a generic or a non-generic
	 * type.</p>
	 * <p>A different non-generic binding will be returned when one of the declaring
	 * types/methods was parameterized.</p>
	 * <p>For other type bindings, this method returns the binding for the type declaration
	 * corresponding to this type binding. In particular, for type bindings that
	 * contain a {@link #getTypeAnnotations() type annotation}, this method returns the binding for the type
	 * declaration, which does not contain the type annotations from the use site.</p>
	 *
	 * @return the declaration type binding
	 * @since 3.1
	 * @see #isEqualTo(IBinding)
	 */
	public ITypeBinding getTypeDeclaration();

	/**
	 * Returns the type parameters of this class or interface type binding.
	 * <p>
	 * Note that type parameters only occur on the binding of the
	 * declaring generic class or interface; e.g., <code>Collection&lt;T&gt;</code>.
	 * Type bindings corresponding to a raw or parameterized reference to a generic
	 * type do not carry type parameters (they instead have non-empty type arguments
	 * and non-trivial erasure).
	 * </p>
	 *
	 * @return the list of binding for the type variables for the type
	 * parameters of this type, or otherwise the empty list
	 * @see #isTypeVariable()
	 * @since 3.1
	 */
	public ITypeBinding[] getTypeParameters();

	/**
	 * Returns the corresponding wildcard binding of this capture binding.
     * Returns <code>null</code> if this type bindings does not represent
     * a capture binding.
	 *
	 * @return the corresponding wildcard binding for a capture
	 * binding, <code>null</code> otherwise
	 * @since 3.1
	 */
	public ITypeBinding getWildcard();

	/**
	 * Returns whether this type binding represents an annotation type.
	 * <p>
	 * Note that an annotation type is always an interface.
	 * </p>
	 *
	 * @return <code>true</code> if this object represents an annotation type,
	 *    and <code>false</code> otherwise
	 * @since 3.1
	 */
	public boolean isAnnotation();

	/**
	 * Returns whether this type binding represents an anonymous class.
	 * <p>
	 * An anonymous class is a subspecies of local class, and therefore mutually
	 * exclusive with member types. Note that anonymous classes have no name
	 * (<code>getName</code> returns the empty string).
	 * </p>
	 *
	 * @return <code>true</code> if this type binding is for an anonymous class,
	 *   and <code>false</code> otherwise
	 */
	public boolean isAnonymous();

	/**
	 * Returns whether this type binding represents an array type.
	 *
	 * @return <code>true</code> if this type binding is for an array type,
	 *   and <code>false</code> otherwise
	 * @see #getElementType()
	 * @see #getDimensions()
	 */
	public boolean isArray();

	/**
	 * Returns whether an expression of this type can be assigned to a variable
	 * of the given type, as specified in section 5.2 of <em>The Java Language
	 * Specification, Third Edition</em> (JLS3).
	 *
	 * <p>If the receiver or the argument is a recovered type, the answer is always false,
	 * unless the two types are identical or the argument is <code>java.lang.Object</code>.</p>
	 *
	 * @param variableType the type of a variable to check compatibility against
	 * @return <code>true</code> if an expression of this type can be assigned to a
	 *   variable of the given type, and <code>false</code> otherwise
	 * @since 3.1
	 */
	public boolean isAssignmentCompatible(ITypeBinding variableType);

	/**
	 * Returns whether this type binding represents a capture binding.
	 * <p>
	 * Capture bindings result from capture conversion as specified
	 * in section 5.1.10 of <em>The Java Language Specification,
	 * Third Edition</em> (JLS3).
	 * </p>
	 * <p>
	 * A capture binding may have upper bounds and a lower bound.
	 * Upper bounds may be accessed using {@link #getTypeBounds()},
	 * the lower bound must be accessed indirectly through the associated
	 * wildcard {@link #getWildcard()} when it is a lower bound wildcard.
	 * </p>
	 * <p>
	 * Note that capture bindings are distinct from type variables
     * (even though they are often depicted as synthetic type
     * variables); as such, {@link #isTypeVariable()} answers
     * <code>false</code> for capture bindings, and
     * {@link #isCapture()} answers <code>false</code> for type variables.
	 * </p>
     *
	 * @return <code>true</code> if this type binding is a capture,
	 *   and <code>false</code> otherwise
	 * @see #getTypeBounds()
	 * @see #getWildcard()
	 * @since 3.1
	 */
	public boolean isCapture();

	/**
	 * Returns whether this type is cast compatible with the given type,
	 * as specified in section 5.5 of <em>The Java Language
	 * Specification, Third Edition</em> (JLS3).
	 * <p>
	 * NOTE: The cast compatibility check performs backwards.
	 * When testing whether type B can be cast to type A, one would use:
	 * <code>A.isCastCompatible(B)</code>
	 * </p>
	 *
	 * <p>If the receiver or the argument is a recovered type, the answer is always false,
	 * unless the two types are identical or the argument is <code>java.lang.Object</code>.</p>
	 *
	 * @param type the type to check compatibility against
	 * @return <code>true</code> if this type is cast compatible with the
	 * given type, and <code>false</code> otherwise
	 * @since 3.1
	 */
	public boolean isCastCompatible(ITypeBinding type);

	/**
	 * Returns whether this type binding represents a class type or a recovered binding.
	 *
	 * @return <code>true</code> if this object represents a class or a recovered binding,
	 *    and <code>false</code> otherwise
	 */
	public boolean isClass();

	/**
	 * Returns whether this type binding represents an enum type.
	 *
	 * @return <code>true</code> if this object represents an enum type,
	 *    and <code>false</code> otherwise
	 * @since 3.1
	 */
	public boolean isEnum();

	/**
	 * Returns whether this type binding represents a record type.
	 *
	 * @return <code>true</code> if this object represents a record type,
	 *    and <code>false</code> otherwise
	 * @since 3.26
	 */
	public boolean isRecord();

	/**
	 * Returns whether this type binding originated in source code.
	 * Returns <code>false</code> for all primitive types, the null type,
	 * array types, and for all classes, interfaces, enums, annotation
	 * types, type variables, parameterized type references,
	 * raw type references, wildcard types, and capture bindings
     * whose information came from a pre-compiled binary class file.
	 *
	 * @return <code>true</code> if the type is in source code,
	 *    and <code>false</code> otherwise
	 */
	public boolean isFromSource();

	/**
	 * Returns whether this type binding represents a declaration of
	 * a generic class or interface.
	 * <p>
	 * Note that type parameters only occur on the binding of the
	 * declaring generic class or interface; e.g., <code>Collection&lt;T&gt;</code>.
	 * Type bindings corresponding to a raw or parameterized reference to a generic
	 * type do not carry type parameters (they instead have non-empty type arguments
	 * and non-trivial erasure).
	 * This method is fully equivalent to <code>getTypeParameters().length &gt; 0)</code>.
	 * </p>
	 * <p>
	 * Note that {@link #isGenericType()},
	 * {@link #isParameterizedType()},
	 * and {@link #isRawType()} are mutually exclusive.
	 * </p>
	 *
	 * @return <code>true</code> if this type binding represents a
	 * declaration of a generic class or interface, and <code>false</code> otherwise
	 * @see #getTypeParameters()
	 * @since 3.1
	 */
	public boolean isGenericType();

	/**
	 * Returns whether this type binding represents an interface type.
	 * <p>
	 * Note that an interface can also be an annotation type.
	 * </p>
	 *
	 * @return <code>true</code> if this object represents an interface,
	 *    and <code>false</code> otherwise
	 */
	public boolean isInterface();

	/**
	 * Returns whether this type binding represents an intersection binding.
	 * <p>
	 * Intersection types can be derived from type parameter bounds and cast
	 * expressions; they also arise in the processes of capture conversion
	 * and least upper bound computation as specified in section 4.9 of
	 * <em>The Java Language Specification, Java SE 8 Edition</em> (JLS8).
	 * </p>
	 * <p>
	 * All the types in the intersection type can be accessed using
	 * {@link #getTypeBounds()}. Wildcard types with more than one
	 * bound will also be reported as intersection type. To check whether this
	 * is a wildcard type, use {@link #isWildcardType()}.
	 * </p>
	 * @return <code>true</code> if this type binding is an intersecting type,
	 *   and <code>false</code> otherwise
	 * @see #getTypeBounds()
	 * @see ITypeBinding#isWildcardType()
	 * @since 3.12
	 */
	public boolean isIntersectionType();

	/**
	 * Returns whether this type binding represents a local class.
	 * <p>
	 * A local class is any nested class or enum type not declared as a member
	 * of another class or interface. A local class is a subspecies of nested
	 * type, and mutually exclusive with member types. For anonymous
	 * classes, which are considered a subspecies of local classes, this method
	 * returns true.
	 * </p>
	 * <p>
	 * Note: This deviates from JLS3 14.3, which states that anonymous types are
	 * not local types since they do not have a name. Also note that interfaces
	 * and annotation types cannot be local.
	 * </p>
	 *
	 * @return <code>true</code> if this type binding is for a local class or
	 * enum type, and <code>false</code> otherwise
	 */
	public boolean isLocal();

	/**
	 * Returns whether this type binding represents a member class or
	 * interface.
	 * <p>
	 * A member type is any type declared as a member of
	 * another type. A member type is a subspecies of nested
	 * type, and mutually exclusive with local types.
	 * </p>
	 *
	 * @return <code>true</code> if this type binding is for a member class,
	 *   interface, enum, or annotation type, and <code>false</code> otherwise
	 */
	public boolean isMember();

	/**
	 * Returns whether this type binding represents a nested class, interface,
	 * enum, or annotation type.
	 * <p>
	 * A nested type is any type whose declaration occurs within
	 * the body of another. The set of nested types is disjoint from the set of
	 * top-level types. Nested types further subdivide into member types, local
	 * types, and anonymous types.
	 * </p>
	 *
	 * @return <code>true</code> if this type binding is for a nested class,
	 *   interface, enum, or annotation type, and <code>false</code> otherwise
	 */
	public boolean isNested();

	/**
	 * Returns whether this type binding represents the null type.
	 * <p>
	 * The null type is the type of a <code>NullLiteral</code> node.
	 * </p>
	 *
	 * @return <code>true</code> if this type binding is for the null type,
	 *   and <code>false</code> otherwise
	 */
	public boolean isNullType();

	/**
	 * Returns whether this type binding represents an instance of
	 * a generic type corresponding to a parameterized type reference.
	 * <p>
	 * For example, an AST type like
	 * <code>Collection&lt;String&gt;</code> typically resolves to a
	 * type binding whose type argument is the type binding for the
	 * class <code>java.lang.String</code> and whose erasure is the type
	 * binding for the generic type <code>java.util.Collection</code>.
	 * </p>
	 * <p>
	 * Note that {@link #isGenericType()},
	 * {@link #isParameterizedType()},
	 * and {@link #isRawType()} are mutually exclusive.
	 * </p>
	 *
	 * @return <code>true</code> if this type binding represents a
	 * an instance of a generic type corresponding to a parameterized
	 * type reference, and <code>false</code> otherwise
	 * @see #getTypeArguments()
	 * @see #getTypeDeclaration()
	 * @since 3.1
	 */
	public boolean isParameterizedType();

	/**
	 * Returns whether this type binding represents a primitive type.
	 * <p>
	 * There are nine predefined type bindings to represent the eight primitive
	 * types and <code>void</code>. These have the same names as the primitive
	 * types that they represent, namely boolean, byte, char, short, int,
	 * long, float, and double, and void.
	 * </p>
	 *
	 * @return <code>true</code> if this type binding is for a primitive type,
	 *   and <code>false</code> otherwise
	 */
	public boolean isPrimitive();

	/**
	 * Returns whether this type binding represents an instance of
	 * a generic type corresponding to a raw type reference.
	 * <p>
	 * For example, an AST type like
	 * <code>Collection</code> typically resolves to a
	 * type binding whose type argument is the type binding for
	 * the class <code>java.lang.Object</code> (the
	 * default bound for the single type parameter of
	 * <code>java.util.Collection</code>) and whose erasure is the
	 * type binding for the generic type
	 * <code>java.util.Collection</code>.
	 * </p>
	 * <p>
	 * Note that {@link #isGenericType()},
	 * {@link #isParameterizedType()},
	 * and {@link #isRawType()} are mutually exclusive.
	 * </p>
	 *
	 * @return <code>true</code> if this type binding represents a
	 * an instance of a generic type corresponding to a raw
	 * type reference, and <code>false</code> otherwise
	 * @see #getTypeDeclaration()
	 * @see #getTypeArguments()
	 * @since 3.1
	 */
	public boolean isRawType();

	/**
	 * Returns whether this type is subtype compatible with the given type,
	 * as specified in section 4.10 of <em>The Java Language
	 * Specification, Third Edition</em> (JLS3).
	 *
	 * <p>If the receiver or the argument is a recovered type, the answer is always false,
	 * unless the two types are identical or the argument is <code>java.lang.Object</code>.</p>
	 *
	 * @param type the type to check compatibility against
	 * @return <code>true</code> if this type is subtype compatible with the
	 * given type, and <code>false</code> otherwise
	 * @since 3.1
	 */
	public boolean isSubTypeCompatible(ITypeBinding type);

	/**
	 * Returns whether this type binding represents a top-level class,
	 * interface, enum, or annotation type.
	 * <p>
	 * A top-level type is any type whose declaration does not occur within the
	 * body of another type declaration. The set of top level types is disjoint
	 * from the set of nested types.
	 * </p>
	 *
	 * @return <code>true</code> if this type binding is for a top-level class,
	 *   interface, enum, or annotation type, and <code>false</code> otherwise
	 */
	public boolean isTopLevel();

	/**
	 * Returns whether this type binding represents a type variable.
	 * Type variables bindings carry the type variable's bounds.
     * <p>
     * Note that type variables are distinct from capture bindings
     * (even though capture bindings are often depicted as synthetic
     * type variables); as such, {@link #isTypeVariable()} answers
     * <code>false</code> for capture bindings, and
     * {@link #isCapture()} answers <code>false</code> for type variables.
     * </p>
	 *
	 * @return <code>true</code> if this type binding is for a type variable,
	 *   and <code>false</code> otherwise
	 * @see #getName()
	 * @see #getTypeBounds()
	 * @since 3.1
	 */
	public boolean isTypeVariable();

	/**
	 * Returns whether this wildcard type is an upper bound
	 * ("extends") as opposed to a lower bound ("super").
	 * Note that this property is only relevant for wildcards
	 * that have a bound.
	 *
	 * @return <code>true</code> if this wildcard type has a bound that is
	 * an upper bound, and <code>false</code> in all other cases
	 * @see #isWildcardType()
	 * @see #getBound()
	 * @since 3.1
	 */
	public boolean isUpperbound();

	/**
	 * Returns whether this type binding represents a wildcard type. A wildcard
	 * type occurs only as an argument to a parameterized type reference.
	 * <p>
	 * For example, an AST type like
	 * <code>Collection&lt;? extends Number&gt;</code> typically resolves to a
	 * parameterized type binding whose type argument is a wildcard type
	 * with upper type bound <code>java.lang.Number</code>.
	 * </p>
	 *
	 * @return <code>true</code> if this object represents a wildcard type,
	 *    and <code>false</code> otherwise
	 * @since 3.1
	 * @see #getBound()
	 * @see #isUpperbound()
	 */
	public boolean isWildcardType();

}
