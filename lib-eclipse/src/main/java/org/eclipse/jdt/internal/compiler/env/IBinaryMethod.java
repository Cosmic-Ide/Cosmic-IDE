/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
 *     Stephan Herrmann - Contribution for bug 186342 - [compiler][null] Using annotations for null checking
 *     Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *         Bug 407191 - [1.8] Binary access support for type annotations
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.env;

// clinit methods (synthetics too?) can be returned from IBinaryType>>getMethods()
// BUT do not have to be... the compiler will ignore them when building the binding.
// The synthetic argument of a member type's constructor (i.e. the first arg of a non-static
// member type) is also ignored by the compiler, BUT in this case it must be included
// in the constructor's signature.

public interface IBinaryMethod extends IGenericMethod {

/**
 * Answer the runtime visible and invisible annotations for this method or null if none.
 */
IBinaryAnnotation[] getAnnotations();

/**
 * Return {@link ClassSignature} for a Class {@link java.lang.Class}.
 * Return {@link org.eclipse.jdt.internal.compiler.impl.Constant} for compile-time constant of primitive type, as well as String literals.
 * Return {@link EnumConstantSignature} if value is an enum constant.
 * Return {@link IBinaryAnnotation} for annotation type.
 * Return {@link Object}[] for array type.
 *
 * @return default value of this annotation method
 */
Object getDefaultValue();

/**
 * Answer the resolved names of the exception types in the
 * class file format as specified in section 4.2 of the Java 2 VM spec
 * or null if the array is empty.
 *
 * For example, java.lang.String is java/lang/String.
 */
char[][] getExceptionTypeNames();

/**
 * Answer the receiver's MethodSignature, which describes the type parameters,
 * parameter types, return type, and exception types
 * as specified in "4.7.9.1 Signatures" of the Java SE 8 VM spec.
 */
char[] getGenericSignature();

/**
 * Answer the receiver's method descriptor which describes the parameter &
 * return types as specified in section 4.4.3 of the Java 2 VM spec.
 *
 * For example:
 *   - int foo(String) is (Ljava/lang/String;)I
 *   - Object[] foo(int) is (I)[Ljava/lang/Object;
 */
char[] getMethodDescriptor();

/**
 * Answer the annotations on the <code>index</code>th parameter or null if none
 * @param index the index of the parameter of interest
 * @param classFileName (file) name of the declaring class for error reporting
 */
IBinaryAnnotation[] getParameterAnnotations(int index, char[] classFileName);

/**
 * Answer the number of parameter annotations that can be retrieved
 * using {@link #getParameterAnnotations(int, char[])}.
 * @return one beyond the highest legal argument to {@link #getParameterAnnotations(int, char[])}.
 */
int getAnnotatedParametersCount();

/**
 * Answer the name of the method.
 *
 * For a constructor, answer <init> & <clinit> for a clinit method.
 */
char[] getSelector();

/**
 * Answer the tagbits set according to the bits for annotations.
 */
long getTagBits();

/**
 * Answer whether the receiver represents a class initializer method.
 */
boolean isClinit();

/**
 * Answer the type annotations on this method.
 */
IBinaryTypeAnnotation[] getTypeAnnotations();
}
