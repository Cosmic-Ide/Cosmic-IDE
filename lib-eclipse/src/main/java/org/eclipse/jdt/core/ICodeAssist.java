/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 * Common protocol for Java elements that support source code assist and code
 * resolve.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ICodeAssist {

	/**
	 * Performs code completion at the given offset position in this compilation unit,
	 * reporting results to the given completion requestor. The <code>offset</code>
	 * is the 0-based index of the character, after which code assist is desired.
	 * An <code>offset</code> of -1 indicates to code assist at the beginning of this
	 * compilation unit.
	 *
	 * @param offset the given offset position
	 * @param requestor the given completion requestor
	 *
	 * @exception JavaModelException if code assist could not be performed. Reasons include:<ul>
	 *  <li>This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 *  <li> The position specified is < -1 or is greater than this compilation unit's
	 *      source length (INDEX_OUT_OF_BOUNDS)
	 * </ul>
	 *
	 * @exception IllegalArgumentException if <code>requestor</code> is <code>null</code>
	 * @deprecated Use {@link #codeComplete(int, ICompletionRequestor)} instead.
	 */
	void codeComplete(int offset, ICodeCompletionRequestor requestor)
		throws JavaModelException;
	/**
	 * Performs code completion at the given offset position in this compilation unit,
	 * reporting results to the given completion requestor. The <code>offset</code>
	 * is the 0-based index of the character, after which code assist is desired.
	 * An <code>offset</code> of -1 indicates to code assist at the beginning of this
	 * compilation unit.
	 *
	 * @param offset the given offset position
	 * @param requestor the given completion requestor
	 * @exception JavaModelException if code assist could not be performed. Reasons include:<ul>
	 *  <li>This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 *  <li> The position specified is < -1 or is greater than this compilation unit's
	 *      source length (INDEX_OUT_OF_BOUNDS)
	 * </ul>
	 *
	 * @exception IllegalArgumentException if <code>requestor</code> is <code>null</code>
	 * @since 2.0
	 * @deprecated Use {@link #codeComplete(int, CompletionRequestor)} instead.
 	 */
	void codeComplete(int offset, ICompletionRequestor requestor)
		throws JavaModelException;

	/**
	 * Performs code completion at the given offset position in this compilation unit,
	 * reporting results to the given completion requestor. The <code>offset</code>
	 * is the 0-based index of the character, after which code assist is desired.
	 * An <code>offset</code> of -1 indicates to code assist at the beginning of this
	 * compilation unit.
	 * <p>
	 *
	 * @param offset the given offset position
	 * @param requestor the given completion requestor
	 * @exception JavaModelException if code assist could not be performed. Reasons include:<ul>
	 *  <li>This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 *  <li> The position specified is < -1 or is greater than this compilation unit's
	 *      source length (INDEX_OUT_OF_BOUNDS)
	 * </ul>
	 *
	 * @exception IllegalArgumentException if <code>requestor</code> is <code>null</code>
	 * @since 3.0
 	 */
	void codeComplete(int offset, CompletionRequestor requestor)
		throws JavaModelException;

	/**
	 * Performs code completion at the given offset position in this compilation unit,
	 * reporting results to the given completion requestor. The <code>offset</code>
	 * is the 0-based index of the character, after which code assist is desired.
	 * An <code>offset</code> of -1 indicates to code assist at the beginning of this
	 * compilation unit.
	 * <p>
	 * If {@link IProgressMonitor} is not <code>null</code> then some proposals which
	 * can be very long to compute are proposed. To avoid that the code assist operation
	 * take too much time a {@link IProgressMonitor} which automatically cancel the code
	 * assist operation when a specified amount of time is reached could be used.
	 *
	 * <pre>
	 * new IProgressMonitor() {
	 *     private final static int TIMEOUT = 500; //ms
	 *     private long endTime;
	 *     public void beginTask(String name, int totalWork) {
	 *         fEndTime= System.currentTimeMillis() + TIMEOUT;
	 *     }
	 *     public boolean isCanceled() {
	 *         return endTime <= System.currentTimeMillis();
	 *     }
	 *     ...
	 * };
	 * </pre>
	 * <p>
	 *
	 * @param offset the given offset position
	 * @param requestor the given completion requestor
	 * @param monitor the progress monitor used to report progress
	 * @exception JavaModelException if code assist could not be performed. Reasons include:<ul>
	 *  <li>This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 *  <li> The position specified is < -1 or is greater than this compilation unit's
	 *      source length (INDEX_OUT_OF_BOUNDS)
	 * </ul>
	 *
	 * @exception IllegalArgumentException if <code>requestor</code> is <code>null</code>
	 * @since 3.5
 	 */
	void codeComplete(int offset, CompletionRequestor requestor, IProgressMonitor monitor)
		throws JavaModelException;

	/**
	 * Performs code completion at the given offset position in this compilation unit,
	 * reporting results to the given completion requestor. The <code>offset</code>
	 * is the 0-based index of the character, after which code assist is desired.
	 * An <code>offset</code> of -1 indicates to code assist at the beginning of this
	 * compilation unit.
	 * It considers types in the working copies with the given owner first. In other words,
	 * the owner's working copies will take precedence over their original compilation units
	 * in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * </p>
	 *
	 * @param offset the given offset position
	 * @param requestor the given completion requestor
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @exception JavaModelException if code assist could not be performed. Reasons include:<ul>
	 *  <li>This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 *  <li> The position specified is < -1 or is greater than this compilation unit's
	 *      source length (INDEX_OUT_OF_BOUNDS)
	 * </ul>
	 *
	 * @exception IllegalArgumentException if <code>requestor</code> is <code>null</code>
	 * @since 3.0
	 * @deprecated Use {@link #codeComplete(int, CompletionRequestor, WorkingCopyOwner)} instead.
	 */
	void codeComplete(int offset, ICompletionRequestor requestor, WorkingCopyOwner owner)
		throws JavaModelException;

	/**
	 * Performs code completion at the given offset position in this compilation unit,
	 * reporting results to the given completion requestor. The <code>offset</code>
	 * is the 0-based index of the character, after which code assist is desired.
	 * An <code>offset</code> of -1 indicates to code assist at the beginning of this
	 * compilation unit.
	 * It considers types in the working copies with the given owner first. In other words,
	 * the owner's working copies will take precedence over their original compilation units
	 * in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * </p>
	 *
	 * @param offset the given offset position
	 * @param requestor the given completion requestor
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @exception JavaModelException if code assist could not be performed. Reasons include:<ul>
	 *  <li>This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 *  <li> The position specified is < -1 or is greater than this compilation unit's
	 *      source length (INDEX_OUT_OF_BOUNDS)
	 * </ul>
	 *
	 * @exception IllegalArgumentException if <code>requestor</code> is <code>null</code>
	 * @since 3.0
	 */
	void codeComplete(int offset, CompletionRequestor requestor, WorkingCopyOwner owner)
		throws JavaModelException;

	/**
	 * Performs code completion at the given offset position in this compilation unit,
	 * reporting results to the given completion requestor. The <code>offset</code>
	 * is the 0-based index of the character, after which code assist is desired.
	 * An <code>offset</code> of -1 indicates to code assist at the beginning of this
	 * compilation unit.
	 * It considers types in the working copies with the given owner first. In other words,
	 * the owner's working copies will take precedence over their original compilation units
	 * in the workspace.
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * </p>
	 * <p>
	 * If {@link IProgressMonitor} is not <code>null</code> then some proposals which
	 * can be very long to compute are proposed. To avoid that the code assist operation
	 * take too much time a {@link IProgressMonitor} which automatically cancel the code
	 * assist operation when a specified amount of time is reached could be used.
	 *
	 * <pre>
	 * new IProgressMonitor() {
	 *     private final static int TIMEOUT = 500; //ms
	 *     private long endTime;
	 *     public void beginTask(String name, int totalWork) {
	 *         fEndTime= System.currentTimeMillis() + TIMEOUT;
	 *     }
	 *     public boolean isCanceled() {
	 *         return endTime <= System.currentTimeMillis();
	 *     }
	 *     ...
	 * };
	 * </pre>
	 * <p>
	 *
	 * @param offset the given offset position
	 * @param requestor the given completion requestor
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @param monitor the progress monitor used to report progress
	 * @exception JavaModelException if code assist could not be performed. Reasons include:<ul>
	 *  <li>This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 *  <li> The position specified is < -1 or is greater than this compilation unit's
	 *      source length (INDEX_OUT_OF_BOUNDS)
	 * </ul>
	 *
	 * @exception IllegalArgumentException if <code>requestor</code> is <code>null</code>
	 * @since 3.5
	 */
	void codeComplete(int offset, CompletionRequestor requestor, WorkingCopyOwner owner, IProgressMonitor monitor)
		throws JavaModelException;

	/**
	 * Returns the Java elements corresponding to the given selected text in this compilation unit.
	 * The <code>offset</code> is the 0-based index of the first selected character.
	 * The <code>length</code> is the number of selected characters.
	 * <p>
	 * Note that if the <code>length</code> is 0 and the <code>offset</code> is inside an identifier
	 * or the index just after an identifier then this identifier is considered as the selection.
	 * </p>
	 *
	 * @param offset the given offset position
	 * @param length the number of selected characters
	 * @return the Java elements corresponding to the given selected text
	 *
	 * @exception JavaModelException if code resolve could not be performed. Reasons include:
	 * <ul>
	 *  <li>This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 *  <li> The range specified is not within this element's
	 *      source range (INDEX_OUT_OF_BOUNDS)
	 * </ul>
	 *
	 */
	IJavaElement[] codeSelect(int offset, int length) throws JavaModelException;
	/**
	 * Returns the Java elements corresponding to the given selected text in this compilation unit.
	 * The <code>offset</code> is the 0-based index of the first selected character.
	 * The <code>length</code> is the number of selected characters.
	 * It considers types in the working copies with the given owner first. In other words,
	 * the owner's working copies will take precedence over their original compilation units
	 * in the workspace.
	 * <p>
	 * Note that if the <code>length</code> is 0 and the <code>offset</code> is inside an identifier
	 * or the index just after an identifier then this identifier is considered as the selection.
	 * </p>
	 * <p>
	 * Note that if a working copy is empty, it will be as if the original compilation
	 * unit had been deleted.
	 * </p>
	 *
	 * @param offset the given offset position
	 * @param length the number of selected characters
	 * @param owner the owner of working copies that take precedence over their original compilation units
	 * @return the Java elements corresponding to the given selected text
	 *
	 * @exception JavaModelException if code resolve could not be performed. Reasons include:
	 * <ul>
	 *  <li>This Java element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
	 *  <li> The range specified is not within this element's
	 *      source range (INDEX_OUT_OF_BOUNDS)
	 * </ul>
	 * @since 3.0
	 */
	IJavaElement[] codeSelect(int offset, int length, WorkingCopyOwner owner) throws JavaModelException;
}
