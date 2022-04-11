/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
 * Common protocol for Java elements that support source code manipulations such
 * as copy, move, rename, and delete.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ISourceManipulation {
/**
 * Copies this element to the given container.
 *
 * @param container the container
 * @param sibling the sibling element before which the copy should be inserted,
 *   or <code>null</code> if the copy should be inserted as the last child of
 *   the container
 * @param rename the new name for the element, or <code>null</code> if the copy
 *   retains the name of this element
 * @param replace <code>true</code> if any existing child in the container with
 *   the target name should be replaced, and <code>false</code> to throw an
 *   exception in the event of a name collision
 * @param monitor a progress monitor
 * @exception JavaModelException if this element could not be copied. Reasons include:
 * <ul>
 * <li> This Java element, container element, or sibling does not exist (ELEMENT_DOES_NOT_EXIST)</li>
 * <li> A <code>CoreException</code> occurred while updating an underlying resource
 * <li> The container is of an incompatible type (INVALID_DESTINATION)
 * <li> The sibling is not a child of the given container (INVALID_SIBLING)
 * <li> The new name is invalid (INVALID_NAME)
 * <li> A child in the container already exists with the same name (NAME_COLLISION)
 *		and <code>replace</code> has been specified as <code>false</code>
 * <li> The container or this element is read-only (READ_ONLY)
 * </ul>
 *
 * @exception IllegalArgumentException if container is <code>null</code>
 * @see org.eclipse.jdt.core.IJavaModelStatusConstants#INVALID_DESTINATION
 */
void copy(IJavaElement container, IJavaElement sibling, String rename, boolean replace, IProgressMonitor monitor) throws JavaModelException;
/**
 * Deletes this element, forcing if specified and necessary.
 *
 * @param force a flag controlling whether underlying resources that are not
 *    in sync with the local file system will be tolerated (same as the force flag
 *	  in IResource operations).
 * @param monitor a progress monitor
 * @exception JavaModelException if this element could not be deleted. Reasons include:
 * <ul>
 * <li> This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
 * <li> A <code>CoreException</code> occurred while updating an underlying resource (CORE_EXCEPTION)</li>
 * <li> This element is read-only (READ_ONLY)</li>
 * </ul>
 */
void delete(boolean force, IProgressMonitor monitor) throws JavaModelException;
/**
 * Moves this element to the given container.
 *
 * @param container the container
 * @param sibling the sibling element before which the element should be inserted,
 *   or <code>null</code> if the element should be inserted as the last child of
 *   the container
 * @param rename the new name for the element, or <code>null</code> if the
 *   element retains its name
 * @param replace <code>true</code> if any existing child in the container with
 *   the target name should be replaced, and <code>false</code> to throw an
 *   exception in the event of a name collision
 * @param monitor a progress monitor
 * @exception JavaModelException if this element could not be moved. Reasons include:
 * <ul>
 * <li> This Java element, container element, or sibling does not exist (ELEMENT_DOES_NOT_EXIST)</li>
 * <li> A <code>CoreException</code> occurred while updating an underlying resource
 * <li> The container is of an incompatible type (INVALID_DESTINATION)
 * <li> The sibling is not a child of the given container (INVALID_SIBLING)
 * <li> The new name is invalid (INVALID_NAME)
 * <li> A child in the container already exists with the same name (NAME_COLLISION)
 *		and <code>replace</code> has been specified as <code>false</code>
 * <li> The container or this element is read-only (READ_ONLY)
 * </ul>
 *
 * @exception IllegalArgumentException if container is <code>null</code>
 * @see org.eclipse.jdt.core.IJavaModelStatusConstants#INVALID_DESTINATION
 */
void move(IJavaElement container, IJavaElement sibling, String rename, boolean replace, IProgressMonitor monitor) throws JavaModelException;
/**
 * Renames this element to the given name.
 *
 * @param name the new name for the element
 * @param replace <code>true</code> if any existing element with the target name
 *   should be replaced, and <code>false</code> to throw an exception in the
 *   event of a name collision
 * @param monitor a progress monitor
 * @exception JavaModelException if this element could not be renamed. Reasons include:
 * <ul>
 * <li> This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
 * <li> A <code>CoreException</code> occurred while updating an underlying resource
 * <li> The new name is invalid (INVALID_NAME)
 * <li> A child in the container already exists with the same name (NAME_COLLISION)
 *		and <code>replace</code> has been specified as <code>false</code>
 * <li> This element is read-only (READ_ONLY)
 * </ul>
 */
void rename(String name, boolean replace, IProgressMonitor monitor) throws JavaModelException;
}
