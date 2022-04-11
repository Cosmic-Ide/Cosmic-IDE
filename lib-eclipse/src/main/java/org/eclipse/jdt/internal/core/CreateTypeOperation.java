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
package org.eclipse.jdt.internal.core;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.core.util.Messages;

/**
 * <p>This operation creates a class or interface.
 *
 * <p>Required Attributes:<ul>
 *  <li>Parent element - must be a compilation unit, or type.
 *  <li>The source code for the type. No verification of the source is
 *      performed.
 * </ul>
 */
public class CreateTypeOperation extends CreateTypeMemberOperation {
/**
 * When executed, this operation will create a type unit
 * in the given parent element (a compilation unit, type)
 */
public CreateTypeOperation(IJavaElement parentElement, String source, boolean force) {
	super(parentElement, source, force);
}
@Override
protected ASTNode generateElementAST(ASTRewrite rewriter, ICompilationUnit cu) throws JavaModelException {
	ASTNode node = super.generateElementAST(rewriter, cu);
	if (!(node instanceof AbstractTypeDeclaration))
		throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.INVALID_CONTENTS));
	return node;
}

/**
 * @see CreateElementInCUOperation#generateResultHandle()
 */
@Override
protected IJavaElement generateResultHandle() {
	IJavaElement parent= getParentElement();
	switch (parent.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
			return ((ICompilationUnit)parent).getType(getASTNodeName());
		case IJavaElement.TYPE:
			return ((IType)parent).getType(getASTNodeName());
		// Note: creating local/anonymous type is not supported
	}
	return null;
}
/**
 * @see CreateElementInCUOperation#getMainTaskName()
 */
@Override
public String getMainTaskName(){
	return Messages.operation_createTypeProgress;
}
/**
 * Returns the <code>IType</code> the member is to be created in.
 */
@Override
protected IType getType() {
	IJavaElement parent = getParentElement();
	if (parent.getElementType() == IJavaElement.TYPE) {
		return (IType) parent;
	}
	return null;
}
/**
 * @see CreateTypeMemberOperation#verifyNameCollision
 */
@Override
protected IJavaModelStatus verifyNameCollision() {
	IJavaElement parent = getParentElement();
	switch (parent.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
			String typeName = getASTNodeName();
			if (((ICompilationUnit) parent).getType(typeName).exists()) {
				return new JavaModelStatus(
					IJavaModelStatusConstants.NAME_COLLISION,
					Messages.bind(Messages.status_nameCollision, typeName));
			}
			break;
		case IJavaElement.TYPE:
			typeName = getASTNodeName();
			if (((IType) parent).getType(typeName).exists()) {
				return new JavaModelStatus(
					IJavaModelStatusConstants.NAME_COLLISION,
					Messages.bind(Messages.status_nameCollision, typeName));
			}
			break;
		// Note: creating local/anonymous type is not supported
	}
	return JavaModelStatus.VERIFIED_OK;
}
@Override
public IJavaModelStatus verify() {
	IJavaModelStatus status = super.verify();
	if (!status.isOK())
		return status;
	try {
		IJavaElement parent = getParentElement();
		if (this.anchorElement != null && this.anchorElement.getElementType() == IJavaElement.FIELD
				&& parent.getElementType() == IJavaElement.TYPE && ((IType)parent).isEnum())
			return new JavaModelStatus(IJavaModelStatusConstants.INVALID_SIBLING, this.anchorElement);
	} catch (JavaModelException e) {
		return e.getJavaModelStatus();
	}
	return JavaModelStatus.VERIFIED_OK;
}
private String getASTNodeName() {
	return ((AbstractTypeDeclaration) this.createdNode).getName().getIdentifier();
}
@Override
protected SimpleName rename(ASTNode node, SimpleName newName) {
	AbstractTypeDeclaration type = (AbstractTypeDeclaration) node;
	SimpleName oldName = type.getName();
	type.setName(newName);
	return oldName;
}
}
