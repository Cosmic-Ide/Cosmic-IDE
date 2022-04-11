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
package org.eclipse.jdt.internal.core;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.util.Messages;

/**
 * This operation renames elements.
 *
 * <p>Notes:<ul>
 * <li>Resource rename is not supported - this operation only renames
 *	   elements contained in compilation units.
 * <li>When a main type is renamed, its compilation unit and constructors are renamed.
 * <li>Constructors cannot be renamed.
 * </ul>
 */
public class RenameElementsOperation extends MoveElementsOperation {
/**
 * When executed, this operation will rename the specified elements with the given names in the
 * corresponding destinations.
 */
public RenameElementsOperation(IJavaElement[] elements, IJavaElement[] destinations, String[] newNames, boolean force) {
	//a rename is a move to the same parent with a new name specified
	//these elements are from different parents
	super(elements, destinations, force);
	setRenamings(newNames);
}
/**
 * @see MultiOperation
 */
@Override
protected String getMainTaskName() {
	return Messages.operation_renameElementProgress;
}
/**
 * @see CopyElementsOperation#isRename()
 */
@Override
protected boolean isRename() {
	return true;
}
/**
 * @see MultiOperation
 */
@Override
protected IJavaModelStatus verify() {
	IJavaModelStatus status = super.verify();
	if (! status.isOK())
		return status;
	if (this.renamingsList == null || this.renamingsList.length == 0)
		return new JavaModelStatus(IJavaModelStatusConstants.NULL_NAME);
	return JavaModelStatus.VERIFIED_OK;
}
/**
 * @see MultiOperation
 */
@Override
protected void verify(IJavaElement element) throws JavaModelException {
	if (element == null || !element.exists())
		error(IJavaModelStatusConstants.ELEMENT_DOES_NOT_EXIST, element);

	if (element.isReadOnly())
		error(IJavaModelStatusConstants.READ_ONLY, element);

	if (!(element instanceof ISourceReference))
		error(IJavaModelStatusConstants.INVALID_ELEMENT_TYPES, element);

	int elementType = element.getElementType();
	if (elementType < IJavaElement.TYPE || elementType == IJavaElement.INITIALIZER)
		error(IJavaModelStatusConstants.INVALID_ELEMENT_TYPES, element);

	verifyRenaming(element);
}
}
