/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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

/**
 * Common protocol for Java elements that can be members of types.
 * This set consists of {@link IType}, {@link IMethod},
 * {@link IField}, {@link IInitializer} and {@link IModuleDescription}.
 * <p>
 * The children are listed in the order in which they appear in the source or class file.
 * </p>
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IMember extends IJavaElement, ISourceReference, ISourceManipulation, IParent {
/**
 * Returns the categories defined by this member's Javadoc. A category is the identifier
 * following the tag <code>@category</code> in the member's Javadoc.
 * Returns an empty array if no category is defined in this member's Javadoc.
 *
 * @return the categories defined by this member's doc
 * @exception JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 *  @since 3.2
 */
String[] getCategories() throws JavaModelException;
/**
 * Returns the class file in which this member is declared, or <code>null</code>
 * if this member is not declared in a class file (for example, a source type).
 * This is a handle-only method.
 *
 * @return the class file in which this member is declared, or <code>null</code>
 * if this member is not declared in a class file (for example, a source type)
 */
IClassFile getClassFile();
/**
 * Returns the compilation unit in which this member is declared, or <code>null</code>
 * if this member is not declared in a compilation unit (for example, a binary type).
 * This is a handle-only method.
 *
 * @return the compilation unit in which this member is declared, or <code>null</code>
 * if this member is not declared in a compilation unit (for example, a binary type)
 */
ICompilationUnit getCompilationUnit();
/**
 * Returns the type in which this member is declared, or <code>null</code>
 * if this member is not declared in a type (for example, a top-level type).
 * This is a handle-only method.
 *
 * @return the type in which this member is declared, or <code>null</code>
 * if this member is not declared in a type (for example, a top-level type)
 */
IType getDeclaringType();
/**
 * Returns the modifier flags for this member. The flags can be examined using class
 * <code>Flags</code>.
 * <p>
 * For {@linkplain #isBinary() binary} members, flags from the class file
 * as well as derived flags {@link Flags#AccAnnotationDefault} and {@link Flags#AccDefaultMethod} are included.
 * </p>
 * <p>
 * For source members, only flags as indicated in the source are returned. Thus if an interface
 * defines a method <code>void myMethod();</code>, the flags don't include the
 * 'public' flag. Source flags include {@link Flags#AccAnnotationDefault} as well.
 * </p>
 *
 * @exception JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @return the modifier flags for this member
 * @see Flags
 */
int getFlags() throws JavaModelException;
/**
 * Returns the Javadoc range if this element is from source or if this element
 * is a binary element with an attached source, null otherwise.
 *
 * <p>If this element is from source, the javadoc range is
 * extracted from the corresponding source.</p>
 * <p>If this element is from a binary, the javadoc is extracted from the
 * attached source if present.</p>
 * <p>If this element's openable is not consistent, then null is returned.</p>
 *
 * @exception JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource.
 * @return a source range corresponding to the javadoc source or <code>null</code>
 * if no source is available, this element has no javadoc comment or
 * this element's openable is not consistent
 * @see IOpenable#isConsistent()
 * @since 3.2
 */
ISourceRange getJavadocRange() throws JavaModelException;
/**
 * Returns the position relative to the order this member is defined in the source.
 * Numbering starts at 1 (thus the first occurrence is occurrence 1, not occurrence 0).
 * <p>
 * Two members m1 and m2 that are equal (e.g. 2 fields with the same name in
 * the same type) can be distinguished using their occurrence counts. If member
 * m1 appears first in the source, it will have an occurrence count of 1. If member
 * m2 appears right after member m1, it will have an occurrence count of 2.
 * </p><p>
 * The occurrence count can be used to distinguish initializers inside a type
 * or anonymous types inside a method.
 * </p><p>
 * This is a handle-only method.  The member may or may not be present.
 * </p>
 *
 * @return the position relative to the order this member is defined in the source
 * @since 3.2
 */
int getOccurrenceCount();
/**
 * Returns the Java type root in which this member is declared.
 * This is a handle-only method.
 *
 * @return the Java type root in which this member is declared.
 * @since 3.3
 */
ITypeRoot getTypeRoot();
/**
 * Returns the local or anonymous type declared in this source member with the given simple name and/or
 * with the specified position relative to the order they are defined in the source.
 * The name is empty if it is an anonymous type.
 * Numbering starts at 1 (thus the first occurrence is occurrence 1, not occurrence 0).
 * This is a handle-only method. The type may or may not exist.
 * Throws a <code>RuntimeException</code> if this member is not a source member.
 *
 * @param name the given simple name
 * @param occurrenceCount the specified position
 * @return the type with the given name and/or with the specified position relative to the order they are defined in the source
 * @since 3.0
 */
IType getType(String name, int occurrenceCount);
/**
 * Returns whether this member is from a class file.
 * This is a handle-only method.
 *
 * @return <code>true</code> if from a class file, and <code>false</code> if
 *   from a compilation unit
 */
boolean isBinary();
}
