/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
 *     IBM Corporation - added J2SE 1.5 support
 *******************************************************************************/
package org.eclipse.jdt.core;

/**
 * Represents a method (or constructor) declared in a type.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IMethod extends IMember, IAnnotatable {
/**
 * Returns a {@link IMemberValuePair member value pair} representing the default
 * value of this method if any, or <code>null</code> if this method's parent is
 * not an annotation type, or else if this method does not have a default value.
 * <p>
 * Note that {@link IMemberValuePair#getValue()} might return <code>null</code>.
 * Please see this method for more details.
 * </p>
 *
 * @exception JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @return a member pair value if any, or <code>null</code> if none
 * @since 3.4
 */
IMemberValuePair getDefaultValue() throws JavaModelException;
/**
 * Returns the simple name of this method.
 * For a constructor, this returns the simple name of the declaring type.
 * Note: This holds whether the constructor appears in a source or binary type
 * (even though class files internally define constructor names to be <code>"&lt;init&gt;"</code>).
 * For the class initialization methods in binary types, this returns
 * the special name <code>"&lt;clinit&gt;"</code>.
 * This is a handle-only method.
 * @return the simple name of this method
 */
@Override
String getElementName();
/**
 * Returns the type signatures of the exceptions this method throws,
 * in the order declared in the source. Returns an empty array
 * if this method throws no exceptions.
 * <p>
 * For example, a source method declaring <code>"throws IOException"</code>,
 * would return the array <code>{"QIOException;"}</code>.
 * </p>
 * <p>
 * The type signatures may be either unresolved (for source types)
 * or resolved (for binary types), and either basic (for basic types)
 * or rich (for parameterized types). See {@link Signature} for details.
 * </p>
 *
 * @exception JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @return the type signatures of the exceptions this method throws,
 * in the order declared in the source, an empty array if this method throws no exceptions
 * @see Signature
 */
String[] getExceptionTypes() throws JavaModelException;

/**
 * Returns the formal type parameter signatures for this method.
 * Returns an empty array if this method has no formal type parameters.
 * <p>
 * The formal type parameter signatures may be either unresolved (for source
 * types) or resolved (for binary types). See {@link Signature} for details.
 * </p>
 *
 * @exception JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @return the formal type parameter signatures of this method,
 * in the order declared in the source, an empty array if none
 * @see Signature
 * @since 3.0
 * @deprecated Use {@link #getTypeParameters()} instead
 */
String[] getTypeParameterSignatures() throws JavaModelException;
/**
 * Returns the formal type parameters for this method.
 * Returns an empty array if this method has no formal type parameters.
 *
 * @exception JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @return the formal type parameters of this method,
 * in the order declared in the source, an empty array if none
 * @since 3.1
 */
ITypeParameter[] getTypeParameters() throws JavaModelException;
/**
 * Returns the number of parameters of this method.
 * This is a handle-only method.
 *
 * @return the number of parameters of this method
 */
int getNumberOfParameters();

/**
 * Returns the parameters of this method.
 * <p>An empty array is returned, if the method has no parameters.</p>
 * <p>For binary types, associated source is used to retrieve the {@link ILocalVariable#getNameRange() name range},
 * {@link ILocalVariable#getSourceRange() source range} and the {@link ILocalVariable#getFlags() flags}.</p>
 * <p>These local variables can be used to retrieve the {@link ILocalVariable#getAnnotations() parameter annotations}.</p>
 *
 * @return the parameters of this method
 * @throws JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @since 3.7
 */
ILocalVariable[] getParameters() throws JavaModelException;

/**
 * Returns the binding key for this method only if the given method is {@link #isResolved() resolved}.
 * A binding key is a key that uniquely identifies this method. It allows access to:
 * <ul>
 * <li>generic info for parameterized methods</li>
 * <li>the actual return type for references to {@link Object#getClass() Object.getClass()}</li>
 * <li>the actual parameter types and return type for references to signature polymorphic methods from class MethodHandle</li>
 * </ul>
 *
 * <p>If the given method is not resolved, the returned key is simply the java element's key.
 * </p>
 * @return the binding key for this method
 * @see org.eclipse.jdt.core.dom.IBinding#getKey()
 * @see BindingKey
 * @see #isResolved()
 * @since 3.1
 */
String getKey();
/**
 * Returns the names of parameters in this method.
 * For binary types, associated source or attached Javadoc are used to retrieve the names.
 * If none can be retrieved, then these names are invented as "arg"+i, where i starts at 0.
 * Returns an empty array if this method has no parameters.
 *
 * <p>For example, a method declared as <code>public void foo(String text, int length)</code>
 * would return the array <code>{"text","length"}</code>.
 * </p>
 *
 * @exception JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @return the names of parameters in this method, an empty array if this method has no parameters
 */
String[] getParameterNames() throws JavaModelException;
/**
 * Returns the type signatures for the parameters of this method.
 * Returns an empty array if this method has no parameters.
 * This is a handle-only method.
 * <p>
 * For example, a source method declared as <code>public void foo(String text, int length)</code>
 * would return the array <code>{"QString;","I"}</code>.
 * </p>
 * <p>
 * The type signatures may be either unresolved (for source types)
 * or resolved (for binary types), and either basic (for basic types)
 * or rich (for parameterized types). See {@link Signature} for details.
 * </p>
 *
 * @return the type signatures for the parameters of this method, an empty array if this method has no parameters
 * @see Signature
 */
String[] getParameterTypes();
/**
 * Returns the names of parameters in this method.
 * For binary types, these names are invented as "arg"+i, where i starts at 0
 * (even if source is associated with the binary or if Javdoc is attached to the binary).
 * Returns an empty array if this method has no parameters.
 *
 * <p>For example, a method declared as <code>public void foo(String text, int length)</code>
 * would return the array <code>{"text","length"}</code>. For the same method in a
 * binary, this would return <code>{"arg0", "arg1"}</code>.
 * </p>
 *
 * @exception JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @return the names of parameters in this method, an empty array if this method has no parameters
 * @since 3.2
 */
String[] getRawParameterNames() throws JavaModelException;
/**
 * Returns the type signature of the return value of this method.
 * For constructors, this returns the signature for void.
 * <p>
 * For example, a source method declared as <code>public String getName()</code>
 * would return <code>"QString;"</code>.
 * </p>
 * <p>
 * The type signature may be either unresolved (for source types)
 * or resolved (for binary types), and either basic (for basic types)
 * or rich (for parameterized types). See {@link Signature} for details.
 * </p>
 *
 * @exception JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @return the type signature of the return value of this method, void  for constructors
 * @see Signature
 */
String getReturnType() throws JavaModelException;
/**
 * Returns the signature of this method. This includes the signatures for the
 * parameter types and return type, but does not include the method name,
 * exception types, or type parameters.
 * <p>
 * For example, a source method declared as <code>public void foo(String text, int length)</code>
 * would return <code>"(QString;I)V"</code>.
 * </p>
 * <p>
 * The type signatures embedded in the method signature may be either unresolved
 * (for source types) or resolved (for binary types), and either basic (for
 * basic types) or rich (for parameterized types). See {@link Signature} for
 * details.
 * </p>
 *
 * @return the signature of this method
 * @exception JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @see Signature
 */
String getSignature() throws JavaModelException;
/**
 * Returns the type parameter declared in this method with the given name.
 * This is a handle-only method. The type parameter may or may not exist.
 *
 * @param name the given simple name
 * @return the type parameter declared in this method with the given name
 * @since 3.1
 */
ITypeParameter getTypeParameter(String name);
/**
 * Returns whether this method is a constructor.
 *
 * @exception JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 *
 * @return true if this method is a constructor, false otherwise
 */
boolean isConstructor() throws JavaModelException;

/**
 * Returns whether this method is a main method.
 * It is a main method if:
 * <ul>
 * <li>its name is equal to <code>"main"</code></li>
 * <li>its return type is <code>void</code></li>
 * <li>it is <code>static</code> and <code>public</code></li>
 * <li>it defines one parameter whose type's simple name is <code>String[]</code></li>
 * </ul>
 *
 * @exception JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @since 2.0
 * @return true if this method is a main method, false otherwise
 */
boolean isMainMethod() throws JavaModelException;

/**
 * Returns whether this method represents a lambda expression.
 *
 * @since 3.10
 * @return true if this method represents a lambda expression, false otherwise.
 */
boolean isLambdaMethod();

/**
 * Returns whether this method represents a resolved method.
 * If a method is resolved, its key contains resolved information.
 *
 * @return whether this method represents a resolved method.
 * @since 3.1
 */
boolean isResolved();
/**
 * Returns whether this method is similar to the given method.
 * Two methods are similar if:
 * <ul>
 * <li>their element names are equal</li>
 * <li>they have the same number of parameters</li>
 * <li>the simple names of their parameter types are equal</li>
 * </ul>
 * This is a handle-only method.
 *
 * @param method the given method
 * @return true if this method is similar to the given method.
 * @see Signature#getSimpleName(char[])
 * @since 2.0
 */
boolean isSimilar(IMethod method);
}
