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
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.util.Messages;

/**
 * This operation renames resources (Package fragments and compilation units).
 *
 * <p>Notes:<ul>
 * <li>When a compilation unit is renamed, its main type and the constructors of the
 * 		main type are renamed.
 * </ul>
 */
public class RenameResourceElementsOperation extends MoveResourceElementsOperation {
/**
 * When executed, this operation will rename the specified elements with the given names in the
 * corresponding destinations.
 */
public RenameResourceElementsOperation(IJavaElement[] elements, IJavaElement[] destinations, String[] newNames, boolean force) {
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
	return Messages.operation_renameResourceProgress;
}
/**
 * @see CopyResourceElementsOperation#isRename()
 */
@Override
protected boolean isRename() {
	return true;
}
/**
 * @see MultiOperation
 */
@Override
protected void verify(IJavaElement element) throws JavaModelException {
	super.verify(element);

	int elementType = element.getElementType();

	if (!(elementType == IJavaElement.COMPILATION_UNIT || elementType == IJavaElement.PACKAGE_FRAGMENT)) {
		error(IJavaModelStatusConstants.INVALID_ELEMENT_TYPES, element);
	}
	if (elementType == IJavaElement.COMPILATION_UNIT) {
		CompilationUnit cu = (CompilationUnit)element;
		if (cu.isWorkingCopy() && !cu.isPrimary()) {
			error(IJavaModelStatusConstants.INVALID_ELEMENT_TYPES, element);
		}
	}
	verifyRenaming(element);
}
}
