/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.core.jdom;

/**
 * Represents a Java compilation unit (source file with one of the
 * {@link org.eclipse.jdt.core.JavaCore#getJavaLikeExtensions()
 * Java-like extensions}).
 * The corresponding syntactic unit is CompilationUnit (JLS2 7.3).
 * Allowable child types for a compilation unit are <code>IDOMPackage</code>, <code>IDOMImport</code>,
 * and <code>IDOMType</code>.
 *
 * @deprecated The JDOM was made obsolete by the addition in 2.0 of the more
 * powerful, fine-grained DOM/AST API found in the
 * org.eclipse.jdt.core.dom package.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IDOMCompilationUnit extends IDOMNode {
/**
 * Returns the header comment for this compilation unit. The header comment
 * appears before the first declaration in a compilation unit.
 * The syntax for a comment corresponds to Comments (JLS2 3.7), <b>including</b>
 * comment delimiters.
 *
 * @return the header comment for this compilation unit, or <code>null</code> if
 *   no header comment is present
 */
public String getHeader();
/**
 * The <code>IDOMCompilationNode</code> refinement of this <code>IDOMNode</code>
 * method returns the name of this compilation unit.
 *
 * <p>The name of a compilation unit is the name of the first top-level public type
 * defined in the compilation unit, suffixed with one of the
 * {@link org.eclipse.jdt.core.JavaCore#getJavaLikeExtensions()
 * Java-like extensions}. For example, if the first
 * top-level public type defined in this compilation unit has the name "Hanoi",
 * then name of this compilation unit is "Hanoi.java".</p>
 *
 * <p>In the absence of a public top-level type, the name of the first top-level
 * type is used. In the absence of any type, the name of the compilation unit
 * is <code>null</code>.</p>
 *
 * @return the name of this compilation unit, or <code>null</code> if none
 */
@Override
public String getName();
/**
 * Sets the header comment for this compilation unit. The header comment
 * appears before the first declaration in a compilation unit.
 * The syntax for a comment corresponds to Comments (JLS2 3.7), <b>including</b>
 * comment delimiters.
 *
 * @param comment the header comment for this compilation unit, or <code>null</code> if
 *   indicating no header comment
 */
public void setHeader(String comment);
/**
 * The <code>IDOMCompilationNode</code> refinement of this <code>IDOMNode</code>
 * method has no effect (the name is computed from the types declared within it).
 *
 * @param name the given name
 */
@Override
public void setName(String name);
}
