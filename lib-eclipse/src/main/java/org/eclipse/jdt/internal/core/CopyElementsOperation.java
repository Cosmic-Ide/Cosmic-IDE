/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.util.Messages;

/**
 * This operation copies/moves a collection of elements from their current
 * container to a new container, optionally renaming the
 * elements.
 * <p>Notes:<ul>
 *    <li>If there is already an element with the same name in
 *    the new container, the operation either overwrites or aborts,
 *    depending on the collision policy setting. The default setting is
 *	  abort.
 *
 *    <li>When constructors are copied to a type, the constructors
 *    are automatically renamed to the name of the destination
 *    type.
 *
 *	  <li>When main types are renamed (move within the same parent),
 *		the compilation unit and constructors are automatically renamed
 *
 *    <li>The collection of elements being copied must all share the
 *    same type of container (for example, must all be type members).
 *
 *    <li>The elements are inserted in the new container in the order given.
 *
 *    <li>The elements can be positioned in the new container - see #setInsertBefore.
 *    By default, the elements are inserted based on the default positions as specified in
 * 	the creation operation for that element type.
 *
 *    <li>This operation can be used to copy and rename elements within
 *    the same container.
 *
 *    <li>This operation only copies elements contained within compilation units.
 * </ul>
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class CopyElementsOperation extends MultiOperation implements SuffixConstants {


	private Map sources = new HashMap();
/**
 * When executed, this operation will copy the given elements to the
 * given containers.  The elements and destination containers must be in
 * the correct order. If there is > 1 destination, the number of destinations
 * must be the same as the number of elements being copied/moved/renamed.
 */
public CopyElementsOperation(IJavaElement[] elementsToCopy, IJavaElement[] destContainers, boolean force) {
	super(elementsToCopy, destContainers, force);
}
/**
 * When executed, this operation will copy the given elements to the
 * given container.
 */
public CopyElementsOperation(IJavaElement[] elementsToCopy, IJavaElement destContainer, boolean force) {
	this(elementsToCopy, new IJavaElement[]{destContainer}, force);
}
/**
 * Returns the <code>String</code> to use as the main task name
 * for progress monitoring.
 */
@Override
protected String getMainTaskName() {
	return Messages.operation_copyElementProgress;
}
/**
 * Returns the nested operation to use for processing this element
 */
protected JavaModelOperation getNestedOperation(IJavaElement element) {
	try {
		IJavaElement dest = getDestinationParent(element);
		switch (element.getElementType()) {
			case IJavaElement.PACKAGE_DECLARATION :
				return new CreatePackageDeclarationOperation(element.getElementName(), (ICompilationUnit) dest);
			case IJavaElement.IMPORT_DECLARATION :
				IImportDeclaration importDeclaration = (IImportDeclaration) element;
				return new CreateImportOperation(element.getElementName(), (ICompilationUnit) dest, importDeclaration.getFlags());
			case IJavaElement.TYPE :
				if (isRenamingMainType(element, dest)) {
					IPath path = element.getPath();
					String extension = path.getFileExtension();
					return new RenameResourceElementsOperation(new IJavaElement[] {dest}, new IJavaElement[] {dest.getParent()}, new String[]{getNewNameFor(element) + '.' + extension}, this.force);
				} else {
					String source = getSourceFor(element);
					String lineSeparator = org.eclipse.jdt.internal.core.util.Util.getLineSeparator(source, element.getJavaProject());
					return new CreateTypeOperation(dest, source + lineSeparator, this.force);
				}
			case IJavaElement.METHOD :
				String source = getSourceFor(element);
				String lineSeparator = org.eclipse.jdt.internal.core.util.Util.getLineSeparator(source, element.getJavaProject());
				return new CreateMethodOperation((IType) dest, source + lineSeparator, this.force);
			case IJavaElement.FIELD :
				source = getSourceFor(element);
				lineSeparator = org.eclipse.jdt.internal.core.util.Util.getLineSeparator(source, element.getJavaProject());
				return new CreateFieldOperation((IType) dest, source + lineSeparator, this.force);
			case IJavaElement.INITIALIZER :
				source = getSourceFor(element);
				lineSeparator = org.eclipse.jdt.internal.core.util.Util.getLineSeparator(source, element.getJavaProject());
				return new CreateInitializerOperation((IType) dest, source + lineSeparator);
			default :
				return null;
		}
	} catch (JavaModelException npe) {
		return null;
	}
}
/**
 * Returns the cached source for this element or compute it if not already cached.
 */
private String getSourceFor(IJavaElement element) throws JavaModelException {
	String source = (String) this.sources.get(element);
	if (source == null && element instanceof IMember) {
		source = ((IMember)element).getSource();
		this.sources.put(element, source);
	}
	return source;
}
/**
 * Returns <code>true</code> if this element is the main type of its compilation unit.
 */
protected boolean isRenamingMainType(IJavaElement element, IJavaElement dest) throws JavaModelException {
	if ((isRename() || getNewNameFor(element) != null)
		&& dest.getElementType() == IJavaElement.COMPILATION_UNIT) {
		String typeName = dest.getElementName();
		typeName = org.eclipse.jdt.internal.core.util.Util.getNameWithoutJavaLikeExtension(typeName);
		return element.getElementName().equals(typeName) && element.getParent().equals(dest);
	}
	return false;
}
/**
 * Copy/move the element from the source to destination, renaming
 * the elements as specified, honoring the collision policy.
 *
 * @exception JavaModelException if the operation is unable to
 * be completed
 */
@Override
protected void processElement(IJavaElement element) throws JavaModelException {
	JavaModelOperation op = getNestedOperation(element);
	boolean createElementInCUOperation =op instanceof CreateElementInCUOperation;
	if (op == null) {
		return;
	}
	if (createElementInCUOperation) {
		IJavaElement sibling = (IJavaElement) this.insertBeforeElements.get(element);
		if (sibling != null) {
			((CreateElementInCUOperation) op).setRelativePosition(sibling, CreateElementInCUOperation.INSERT_BEFORE);
		} else
			if (isRename()) {
				IJavaElement anchor = resolveRenameAnchor(element);
				if (anchor != null) {
					((CreateElementInCUOperation) op).setRelativePosition(anchor, CreateElementInCUOperation.INSERT_AFTER); // insert after so that the anchor is found before when deleted below
				}
			}
		String newName = getNewNameFor(element);
		if (newName != null) {
			((CreateElementInCUOperation) op).setAlteredName(newName);
		}
	}
	executeNestedOperation(op, 1);

	JavaElement destination = (JavaElement) getDestinationParent(element);
	ICompilationUnit unit= destination.getCompilationUnit();
	if (!unit.isWorkingCopy()) {
		unit.close();
	}

	if (createElementInCUOperation && isMove() && !isRenamingMainType(element, destination)) {
		JavaModelOperation deleteOp = new DeleteElementsOperation(new IJavaElement[] { element }, this.force);
		executeNestedOperation(deleteOp, 1);
	}
}
/**
 * Returns the anchor used for positioning in the destination for
 * the element being renamed. For renaming, if no anchor has
 * explicitly been provided, the element is anchored in the same position.
 */
private IJavaElement resolveRenameAnchor(IJavaElement element) throws JavaModelException {
	IParent parent = (IParent) element.getParent();
	IJavaElement[] children = parent.getChildren();
	for (int i = 0; i < children.length; i++) {
		IJavaElement child = children[i];
		if (child.equals(element)) {
			return child;
		}
	}
	return null;
}
/**
 * Possible failures:
 * <ul>
 *  <li>NO_ELEMENTS_TO_PROCESS - no elements supplied to the operation
 *	<li>INDEX_OUT_OF_BOUNDS - the number of renamings supplied to the operation
 *		does not match the number of elements that were supplied.
 * </ul>
 */
@Override
protected IJavaModelStatus verify() {
	IJavaModelStatus status = super.verify();
	if (!status.isOK()) {
		return status;
	}
	if (this.renamingsList != null && this.renamingsList.length != this.elementsToProcess.length) {
		return new JavaModelStatus(IJavaModelStatusConstants.INDEX_OUT_OF_BOUNDS);
	}
	return JavaModelStatus.VERIFIED_OK;
}
/**
 * @see MultiOperation
 *
 * Possible failure codes:
 * <ul>
 *
 *	<li>ELEMENT_DOES_NOT_EXIST - <code>element</code> or its specified destination is
 *		is <code>null</code> or does not exist. If a <code>null</code> element is
 *		supplied, no element is provided in the status, otherwise, the non-existant element
 *		is supplied in the status.
 *	<li>INVALID_ELEMENT_TYPES - <code>element</code> is not contained within a compilation unit.
 *		This operation only operates on elements contained within compilation units.
 *  <li>READ_ONLY - <code>element</code> is read only.
 *	<li>INVALID_DESTINATION - The destination parent specified for <code>element</code>
 *		is of an incompatible type. The destination for a package declaration or import declaration must
 *		be a compilation unit; the destination for a type must be a type or compilation
 *		unit; the destination for any type member (other than a type) must be a type. When
 *		this error occurs, the element provided in the operation status is the <code>element</code>.
 *	<li>INVALID_NAME - the new name for <code>element</code> does not have valid syntax.
 *      In this case the element and name are provided in the status.

 * </ul>
 */
@Override
protected void verify(IJavaElement element) throws JavaModelException {
	if (element == null || !element.exists())
		error(IJavaModelStatusConstants.ELEMENT_DOES_NOT_EXIST, element);

	if (element.getElementType() < IJavaElement.TYPE)
		error(IJavaModelStatusConstants.INVALID_ELEMENT_TYPES, element);

	if (element.isReadOnly())
		error(IJavaModelStatusConstants.READ_ONLY, element);

	IJavaElement dest = getDestinationParent(element);
	verifyDestination(element, dest);
	verifySibling(element, dest);
	if (this.renamingsList != null) {
		verifyRenaming(element);
	}
}
}
