/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.core.util.MementoTokenizer;

/**
 * @see IImportContainer
 */
public class ImportContainer extends SourceRefElement implements IImportContainer {
protected ImportContainer(CompilationUnit parent) {
	super(parent);
}
@Override
public boolean equals(Object o) {
	if (!(o instanceof ImportContainer)) return false;
	return super.equals(o);
}
/**
 * @see IJavaElement
 */
@Override
public int getElementType() {
	return IMPORT_CONTAINER;
}

@Override
public IJavaElement getHandleFromMemento(String token, MementoTokenizer memento, WorkingCopyOwner workingCopyOwner) {
	switch (token.charAt(0)) {
		case JEM_COUNT:
			return getHandleUpdatingCountFromMemento(memento, workingCopyOwner);
		case JEM_IMPORTDECLARATION:
			if (memento.hasMoreTokens()) {
				String importName = memento.nextToken();
				JavaElement importDecl = getImport(importName);
				return importDecl.getHandleFromMemento(memento, workingCopyOwner);
			} else {
				return this;
			}
	}
	return null;
}
/**
 * @see JavaElement#getHandleMemento()
 */
@Override
protected char getHandleMementoDelimiter() {
	return JavaElement.JEM_IMPORTDECLARATION;
}
/**
 * @see IImportContainer
 */
@Override
public ImportDeclaration getImport(String importName) {
	int index = importName.indexOf(".*"); ///$NON-NLS-1$
	boolean isOnDemand = index != -1;
	if (isOnDemand) {
		importName = importName.substring(0, index);
	}
	return getImport(importName, isOnDemand);
}
protected ImportDeclaration getImport(String importName, boolean isOnDemand) {
	return new ImportDeclaration(this, importName, isOnDemand);
}

@Override
public JavaElement getPrimaryElement(boolean checkOwner) {
	CompilationUnit cu = (CompilationUnit)this.getParent();
	if (checkOwner && cu.isPrimary()) return this;
	return cu.getImportContainer();
}
/**
 * @see ISourceReference
 */
@Override
public ISourceRange getSourceRange() throws JavaModelException {
	IJavaElement[] imports= getChildren();
	ISourceRange firstRange= ((ISourceReference)imports[0]).getSourceRange();
	ISourceRange lastRange= ((ISourceReference)imports[imports.length - 1]).getSourceRange();
	SourceRange range= new SourceRange(firstRange.getOffset(), lastRange.getOffset() + lastRange.getLength() - firstRange.getOffset());
	return range;
}
/**
 */
@Override
public String readableName() {

	return null;
}
/**
 * @private Debugging purposes
 */
@Override
protected void toString(int tab, StringBuffer buffer) {
	Object info = JavaModelManager.getJavaModelManager().peekAtInfo(this);
	if (info == null || !(info instanceof JavaElementInfo)) return;
	IJavaElement[] children = ((JavaElementInfo)info).getChildren();
	for (int i = 0; i < children.length; i++) {
		if (i > 0) buffer.append("\n"); //$NON-NLS-1$
		((JavaElement)children[i]).toString(tab, buffer);
	}
}
/**
 *  Debugging purposes
 */
@Override
protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
	buffer.append(tabString(tab));
	buffer.append("<import container>"); //$NON-NLS-1$
	if (info == null) {
		buffer.append(" (not open)"); //$NON-NLS-1$
	}
}
@Override
public ISourceRange getNameRange() {
	return null;
}
}
