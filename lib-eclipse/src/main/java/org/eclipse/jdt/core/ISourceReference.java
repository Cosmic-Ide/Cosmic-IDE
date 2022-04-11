/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
 * Common protocol for Java elements that have associated source code.
 * This set consists of {@link IClassFile}, {@link ICompilationUnit},
 * {@link IPackageDeclaration}, {@link IImportDeclaration},
 * {@link IImportContainer}, {@link IType}, {@link IField},
 * {@link IMethod}, {@link IInitializer}, {@link ITypeParameter},
 * {@link ILocalVariable}, and {@link IAnnotation}.
 * <p>
 * Note: For <code>IClassFile</code>, <code>IType</code> and other members
 * derived from a binary type, the implementation returns source iff the
 * element has attached source code.
 * </p>
 * <p>
 * Source reference elements may be working copies if they were created from
 * a compilation unit that is a working copy.
 * </p>
 *
 * @see IPackageFragmentRoot#attachSource(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IProgressMonitor)
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ISourceReference {
/**
 * Returns whether this element exists in the model.
 *
 * @return <code>true</code> if this element exists in the Java model
 * @since 2.0
 */
boolean exists();

/**
 * Returns the source code associated with this element.
 * This extracts the substring from the source buffer containing this source
 * element. This corresponds to the source range that would be returned by
 * <code>getSourceRange</code>.
 * <p>
 * For class files, this returns the source of the entire compilation unit
 * associated with the class file (if there is one).
 * </p>
 *
 * @return the source code, or <code>null</code> if this element has no
 *   associated source code
 * @exception JavaModelException if an exception occurs while accessing its corresponding resource
 */
String getSource() throws JavaModelException;
/**
 * Returns the source range associated with this element.
 * <p>
 * For class files, this returns the range of the entire compilation unit
 * associated with the class file (if there is one).
 * </p>
 * <p>
 * If this element has no associated source code, either <code>null</code> is returned,
 * or a source range with a -1 offset and a 0 length. {@link SourceRange#isAvailable(ISourceRange)}
 * can be used to detect that case.
 * </p>
 *
 * @return the source range, or either <code>null</code> or [-1, 0] if this element has no
 *   associated source code
 * @exception JavaModelException if an exception occurs while accessing its corresponding resource
 * @see SourceRange#isAvailable(ISourceRange)
 */
ISourceRange getSourceRange() throws JavaModelException;

/**
 * Returns the name range associated with this element.
 *
 * <p>If the element is an {@link IMember}, it returns
 * the source range of this member's simple name,
 * or <code>null</code> if this member does not have a name
 * (for example, an initializer), or if this member does not have
 * associated source code (for example, a binary type).</p>
 *
 * <p>If this element is an {@link IImportDeclaration}, the source range
 * of this import declaration's name, or <code>null</code> if this import
 * declaration does not have associated source code (for example, a binary type).
 * <br>The source range for the name includes the trailing '*' if the call to
 * {@link IImportDeclaration#isOnDemand()} returns true.
 * </p>
 *
 * <p>If this element is an {@link IPackageDeclaration}, the source range of
 * this package declaration's name, or <code>null</code> if this package
 * declaration does not have associated source code (for example, a binary type).</p>
 *
 * <p>If this element is an {@link IAnnotation}, the source range of
 * this annotation's name, or <code>null</code> if this annotation does not have
 * associated source code (for example, in a binary type).</p>
 *
 * <p>If this element is an {@link ITypeParameter}, the source range of this
 * type parameter's name, or <code>null</code> if this type parameter does not have
 * associated source code (for example, in a binary type).</p>
 *
 * <p>If this element is an {@link ITypeRoot} or {@link IImportContainer}, it
 * returns null.</p>
 *
 * @return the name range associated with this element, or <code>null</code> if
 * not available
 *
 * @since 3.7
 */
ISourceRange getNameRange() throws JavaModelException;
}
