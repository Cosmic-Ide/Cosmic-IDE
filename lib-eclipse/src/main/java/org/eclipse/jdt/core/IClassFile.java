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

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Represents a single <code>.class</code> file, holding the binary form
 * of either a type or a module:
 * <ul>
 * <li>A class file of type {@link IOrdinaryClassFile} has a single child of type <code>IType</code>,</li>
 * <li>a class file of type {@link IModularClassFile} has a single child of type <code>IModuleDescription</code>.</li>
 * </ul>
 * Class file elements need to be opened before they can be navigated.
 * If a class file cannot be parsed, its structure remains unknown. Use
 * <code>IJavaElement.isStructureKnown</code> to determine whether this is the
 * case.
 * <p>
 * Note: <code>IClassFile</code> extends <code>ISourceReference</code>.
 * Source can be obtained for a class file if and only if source has been attached to this
 * class file. The source associated with a class file is the source code of
 * the compilation unit it was (nominally) generated from.
 * </p>
 *
 * @see IPackageFragmentRoot#attachSource(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath, IProgressMonitor)
 * @noimplement This interface is not intended to be implemented by clients.
 */

public interface IClassFile extends ITypeRoot {

/**
 * Changes this class file handle into a working copy. A new {@link IBuffer} is
 * created using the given owner. Uses the primary owner if <code>null</code> is
 * specified.
 * <p>
 * When switching to working copy mode, problems are reported to the given
 * {@link IProblemRequestor}. Note that once in working copy mode, the given
 * {@link IProblemRequestor} is ignored. Only the original {@link IProblemRequestor}
 * is used to report subsequent problems.
 * </p>
 * <p>
 * Once in working copy mode, changes to this working copy or its children are done in memory.
 * Only the new buffer is affected.
 * </p>
 * <p>
 * Using {@link ICompilationUnit#commitWorkingCopy(boolean, IProgressMonitor)} on the working copy
 * will throw a <code>JavaModelException</code> as a class file is implicetly read-only.
 * </p>
 * <p>
 * If this class file was already in working copy mode, an internal counter is incremented and no
 * other action is taken on this working copy. To bring this working copy back into the original mode
 * (where it reflects the underlying resource), {@link ICompilationUnit#discardWorkingCopy} must be call as many
 * times as {@link #becomeWorkingCopy(IProblemRequestor, WorkingCopyOwner, IProgressMonitor)}.
 * </p>
 * <p>
 * The primary compilation unit of a class file's working copy does not exist if the class file is not
 * in working copy mode (<code>classFileWorkingCopy.getPrimary().exists() == false</code>).
 * </p>
 * <p>
 * The resource of a class file's working copy is <code>null</code> if the class file is in an external jar file.
 * </p>
 *
 * @param problemRequestor a requestor which will get notified of problems detected during
 * 	reconciling as they are discovered. The requestor can be set to <code>null</code> indicating
 * 	that the client is not interested in problems.
 * @param owner the given {@link WorkingCopyOwner}, or <code>null</code> for the primary owner
 * @param monitor a progress monitor used to report progress while opening this compilation unit
 * 	or <code>null</code> if no progress should be reported
 * @return a working copy for this class file
 * @throws JavaModelException if this compilation unit could not become a working copy.
 * @see ICompilationUnit#discardWorkingCopy()
 * @since 3.2
 * @deprecated Use {@link ITypeRoot#getWorkingCopy(WorkingCopyOwner, IProgressMonitor)} instead.
 * 	Note that if this deprecated method is used, problems will be reported to the given problem requestor
 * 	as well as the problem requestor returned by the working copy owner (if not null).
 */
ICompilationUnit becomeWorkingCopy(IProblemRequestor problemRequestor, WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaModelException;
/**
 * Returns the bytes contained in this class file.
 *
 * @return the bytes contained in this class file
 *
 * @exception JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource
 * @since 3.3
 */
byte[] getBytes() throws JavaModelException;
/**
 * Returns the type contained in this class file.
 * This is a handle-only method. The type may or may not exist.
 *
 * @return the type contained in this class file
 * @throws UnsupportedOperationException when invoked on an instance representing a modular class file.
 * @deprecated should only be used as {@link IOrdinaryClassFile#getType()}.
 */
@Deprecated
IType getType();
/**
 * Returns a working copy on the source associated with this class file using the given
 * factory to create the buffer, or <code>null</code> if there is no source associated
 * with the class file.
 * <p>
 * The buffer will be automatically initialized with the source of the class file
 * upon creation.
 * <p>
 * The only valid operations on this working copy are <code>getBuffer()</code> or <code>getOriginalElement</code>.
 *
 * @param monitor a progress monitor used to report progress while opening this compilation unit
 *                 or <code>null</code> if no progress should be reported
 * @param factory the factory that creates a buffer that is used to get the content of the working copy
 *                 or <code>null</code> if the internal factory should be used
 * @return a  a working copy on the source associated with this class file
 * @exception JavaModelException if the source of this class file can
 *   not be determined. Reasons include:
 * <ul>
 * <li> This class file does not exist (ELEMENT_DOES_NOT_EXIST)</li>
 * </ul>
 * @since 2.0
 * @deprecated Use {@link ITypeRoot#getWorkingCopy(WorkingCopyOwner, IProgressMonitor)} instead
 */
IJavaElement getWorkingCopy(IProgressMonitor monitor, IBufferFactory factory) throws JavaModelException;
/**
 * Returns whether this type represents a class. This is not guaranteed to be
 * instantaneous, as it may require parsing the underlying file.
 *
 * @return <code>true</code> if the class file represents a class.
 *
 * @exception JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource
 */
boolean isClass() throws JavaModelException;
/**
 * Returns whether this type represents an interface. This is not guaranteed to
 * be instantaneous, as it may require parsing the underlying file.
 *
 * @return <code>true</code> if the class file represents an interface.
 *
 * @exception JavaModelException if this element does not exist or if an
 *      exception occurs while accessing its corresponding resource
 */
boolean isInterface() throws JavaModelException;
}
