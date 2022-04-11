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
package org.eclipse.jdt.core;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

/**
 * Represents the outcome of an Java model operation. Status objects are
 * used inside <code>JavaModelException</code> objects to indicate what went
 * wrong.
 * <p>
 * Java model status object are distinguished by their plug-in id:
 * <code>getPlugin</code> returns <code>"org.eclipse.jdt.core"</code>.
 * <code>getCode</code> returns one of the status codes declared in
 * <code>IJavaModelStatusConstants</code>.
 * </p>
 * <p>
 * A Java model status may also carry additional information (that is, in
 * addition to the information defined in <code>IStatus</code>):
 * <ul>
 *   <li>elements - optional handles to Java elements associated with the failure</li>
 *   <li>string - optional string associated with the failure</li>
 * </ul>
 *
 * @see org.eclipse.core.runtime.IStatus
 * @see IJavaModelStatusConstants
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IJavaModelStatus extends IStatus {
/**
 * Returns any Java elements associated with the failure (see specification
 * of the status code), or an empty array if no elements are related to this
 * particular status code.
 *
 * @return the list of Java element culprits
 * @see IJavaModelStatusConstants
 */
IJavaElement[] getElements();
/**
 * Returns the path associated with the failure (see specification
 * of the status code), or <code>null</code> if the failure is not
 * one of <code>DEVICE_PATH</code>, <code>INVALID_PATH</code>,
 * <code>PATH_OUTSIDE_PROJECT</code>, or <code>RELATIVE_PATH</code>.
 *
 * @return the path that caused the failure, or <code>null</code> if none
 * @see IJavaModelStatusConstants#DEVICE_PATH
 * @see IJavaModelStatusConstants#INVALID_PATH
 * @see IJavaModelStatusConstants#PATH_OUTSIDE_PROJECT
 * @see IJavaModelStatusConstants#RELATIVE_PATH
 */
IPath getPath();
/**
 * Returns the string associated with the failure (see specification
 * of the status code), or <code>null</code> if no string is related to this
 * particular status code.
 *
 * @return the string culprit, or <code>null</code> if none
 * @see IJavaModelStatusConstants
 * @deprecated Use {@link IStatus#getMessage()} instead
 */
String getString();
/**
 * Returns whether this status indicates that a Java model element does not exist.
 * This convenience method is equivalent to
 * <code>getCode() == IJavaModelStatusConstants.ELEMENT_DOES_NOT_EXIST</code>.
 *
 * @return <code>true</code> if the status code indicates that a Java model
 *   element does not exist
 * @see IJavaModelStatusConstants#ELEMENT_DOES_NOT_EXIST
 */
boolean isDoesNotExist();
}
