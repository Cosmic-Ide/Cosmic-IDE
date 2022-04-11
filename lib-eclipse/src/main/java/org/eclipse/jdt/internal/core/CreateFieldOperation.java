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

import java.util.Iterator;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.core.util.Messages;

/**
 * <p>This operation creates a field declaration in a type.
 *
 * <p>Required Attributes:<ul>
 *  <li>Containing Type
 *  <li>The source code for the declaration. No verification of the source is
 *      performed.
 * </ul>
 */
@SuppressWarnings("rawtypes")
public class CreateFieldOperation extends CreateTypeMemberOperation {
/**
 * When executed, this operation will create a field with the given name
 * in the given type with the specified source.
 *
 * <p>By default the new field is positioned after the last existing field
 * declaration, or as the first member in the type if there are no
 * field declarations.
 */
public CreateFieldOperation(IType parentElement, String source, boolean force) {
	super(parentElement, source, force);
}
@Override
protected ASTNode generateElementAST(ASTRewrite rewriter, ICompilationUnit cu) throws JavaModelException {
	ASTNode node = super.generateElementAST(rewriter, cu);
	if (node.getNodeType() != ASTNode.FIELD_DECLARATION)
		throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.INVALID_CONTENTS));
	return node;
}
/**
 * @see CreateElementInCUOperation#generateResultHandle
 */
@Override
protected IJavaElement generateResultHandle() {
	return getType().getField(getASTNodeName());
}
/**
 * @see CreateElementInCUOperation#getMainTaskName()
 */
@Override
public String getMainTaskName(){
	return Messages.operation_createFieldProgress;
}
private VariableDeclarationFragment getFragment(ASTNode node) {
	Iterator fragments =  ((FieldDeclaration) node).fragments().iterator();
	if (this.anchorElement != null) {
		VariableDeclarationFragment fragment = null;
		String fragmentName = this.anchorElement.getElementName();
		while (fragments.hasNext()) {
			fragment = (VariableDeclarationFragment) fragments.next();
			if (fragment.getName().getIdentifier().equals(fragmentName)) {
				return fragment;
			}
		}
		return fragment;
	} else {
		return (VariableDeclarationFragment) fragments.next();
	}
}
/**
 * By default the new field is positioned after the last existing field
 * declaration, or as the first member in the type if there are no
 * field declarations.
 */
@Override
protected void initializeDefaultPosition() {
	IType parentElement = getType();
	try {
		IField[] fields = parentElement.getFields();
		if (fields != null && fields.length > 0) {
			final IField lastField = fields[fields.length - 1];
			if (parentElement.isEnum()) {
				IField field = lastField;
				if (!field.isEnumConstant()) {
					createAfter(lastField);
				}
			} else {
				createAfter(lastField);
			}
		} else {
			IJavaElement[] elements = parentElement.getChildren();
			if (elements != null && elements.length > 0) {
				createBefore(elements[0]);
			}
		}
	} catch (JavaModelException e) {
		// type doesn't exist: ignore
	}
}
/**
 * @see CreateTypeMemberOperation#verifyNameCollision
 */
@Override
protected IJavaModelStatus verifyNameCollision() {
	if (this.createdNode != null) {
		IType type= getType();
		String fieldName = getASTNodeName();
		if (type.getField(fieldName).exists()) {
			return new JavaModelStatus(
				IJavaModelStatusConstants.NAME_COLLISION,
				Messages.bind(Messages.status_nameCollision, fieldName));
		}
	}
	return JavaModelStatus.VERIFIED_OK;
}
private String getASTNodeName() {
	if (this.alteredName != null) return this.alteredName;
	return getFragment(this.createdNode).getName().getIdentifier();
}
@Override
protected SimpleName rename(ASTNode node, SimpleName newName) {
	VariableDeclarationFragment fragment = getFragment(node);
	SimpleName oldName = fragment.getName();
	fragment.setName(newName);
	return oldName;
}
}
