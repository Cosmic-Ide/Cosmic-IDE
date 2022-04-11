/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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

import org.eclipse.core.runtime.IProgressMonitor;


/**
 * Represents an entire Java type root (either an <code>ICompilationUnit</code>
 * or an <code>IClassFile</code>).
 *
 * @see ICompilationUnit Note that methods {@link #findPrimaryType()} and {@link #getElementAt(int)}
 * 	were already implemented in this interface respectively since version 3.0 and version 1.0.
 * @see IClassFile Note that method {@link #getWorkingCopy(WorkingCopyOwner, IProgressMonitor)}
 * 	was already implemented in this interface since version 3.0.
 * @since 3.3
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ITypeRoot extends IJavaElement, IParent, IOpenable, ISourceReference, ICodeAssist {

/**
 * Finds the primary type of this Java type root (that is, the type with the same name as the
 * compilation unit, or the type of a class file), or <code>null</code> if no such a type exists.
 *
 * @return the found primary type of this Java type root, or <code>null</code> if no such a type exists
 */
IType findPrimaryType();
/**
 * Returns the module description contained in this type root or null if there is no module
 * in this type root.
 * <p>Only subtype {@link IModularClassFile} promises to return non-null.</p>
 *
 * @throws JavaModelException
 * @since 3.14
 * @return the module description contained in the type root or null.
 */
default IModuleDescription getModule() throws JavaModelException {
	return null;
}
/**
 * Returns the smallest element within this Java type root that
 * includes the given source position (that is, a method, field, etc.), or
 * <code>null</code> if there is no element other than the Java type root
 * itself at the given position, or if the given position is not
 * within the source range of the source of this Java type root.
 *
 * @param position a source position inside the Java type root
 * @return the innermost Java element enclosing a given source position or <code>null</code>
 *	if none (excluding the Java type root).
 * @throws JavaModelException if the Java type root does not exist or if an
 *	exception occurs while accessing its corresponding resource
 */
IJavaElement getElementAt(int position) throws JavaModelException;

/**
 * Returns a shared working copy on this compilation unit or class file using the given working copy owner to create
 * the buffer. If this is already a working copy of the given owner, the element itself is returned.
 * This API can only answer an already existing working copy if it is based on the same
 * original Java type root AND was using the same working copy owner (that is, as defined by {@link Object#equals}).
 * <p>
 * The life time of a shared working copy is as follows:
 * <ul>
 * <li>The first call to {@link #getWorkingCopy(WorkingCopyOwner, IProgressMonitor)}
 * 	creates a new working copy for this element</li>
 * <li>Subsequent calls increment an internal counter.</li>
 * <li>A call to {@link ICompilationUnit#discardWorkingCopy()} decrements the internal counter.</li>
 * <li>When this counter is 0, the working copy is discarded.
 * </ul>
 * So users of this method must discard exactly once the working copy.
 * <p>
 * Note that the working copy owner will be used for the life time of the shared working copy, that is if the
 * working copy is closed then reopened, this owner will be used.
 * The buffer will be automatically initialized with the original's Java type root content upon creation.
 * <p>
 * When the shared working copy instance is created, an ADDED IJavaElementDelta is reported on this
 * working copy.
 * </p><p>
 * A working copy can be created on a not-yet existing compilation unit.
 * In particular, such a working copy can then be committed in order to create
 * the corresponding compilation unit.
 * </p><p>
 * Note that possible problems of this working copy are reported using this method only
 * if the given working copy owner returns a problem requestor for this working copy
 * (see {@link WorkingCopyOwner#getProblemRequestor(ICompilationUnit)}).
 * </p>
 *
 * @param owner the working copy owner that creates a buffer that is used to get the content
 * 				of the working copy
 * @param monitor a progress monitor used to report progress while opening this compilation unit
 *                 or <code>null</code> if no progress should be reported
 * @throws JavaModelException if the contents of this element can
 *   	not be determined.
 * @return a new working copy of this Java type root using the given owner to create
 *		the buffer, or this Java type root if it is already a working copy
 */
ICompilationUnit getWorkingCopy(WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaModelException;

}
