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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.*;

/**
 * Handle for an import declaration. Info object is a ImportDeclarationElementInfo.
 * @see IImportDeclaration
 */

public class ImportDeclaration extends SourceRefElement implements IImportDeclaration {

	protected String name;
	protected boolean isOnDemand;

/**
 * Constructs an ImportDeclaration in the given import container
 * with the given name.
 */
protected ImportDeclaration(ImportContainer parent, String name, boolean isOnDemand) {
	super(parent);
	this.name = name;
	this.isOnDemand = isOnDemand;
}
@Override
public boolean equals(Object o) {
	if (!(o instanceof ImportDeclaration)) return false;
	return super.equals(o);
}
@Override
public String getElementName() {
	if (this.isOnDemand)
		return this.name + ".*"; //$NON-NLS-1$
	return this.name;
}
public String getNameWithoutStar() {
	return this.name;
}
/**
 * @see IJavaElement
 */
@Override
public int getElementType() {
	return IMPORT_DECLARATION;
}
/**
 * @see org.eclipse.jdt.core.IImportDeclaration#getFlags()
 */
@Override
public int getFlags() throws JavaModelException {
	ImportDeclarationElementInfo info = (ImportDeclarationElementInfo)getElementInfo();
	return info.getModifiers();
}
/**
 * @see JavaElement#getHandleMemento(StringBuffer)
 * For import declarations, the handle delimiter is associated to the import container already
 */
@Override
protected void getHandleMemento(StringBuffer buff) {
	getParent().getHandleMemento(buff);
	escapeMementoName(buff, getElementName());
	if (this.occurrenceCount > 1) {
		buff.append(JEM_COUNT);
		buff.append(this.occurrenceCount);
	}
}
/**
 * @see JavaElement#getHandleMemento()
 */
@Override
protected char getHandleMementoDelimiter() {
	// For import declarations, the handle delimiter is associated to the import container already
	Assert.isTrue(false, "Should not be called"); //$NON-NLS-1$
	return 0;
}
@Override
public ISourceRange getNameRange() throws JavaModelException {
	ImportDeclarationElementInfo info = (ImportDeclarationElementInfo) getElementInfo();
	return info.getNameRange();
}

@Override
public JavaElement getPrimaryElement(boolean checkOwner) {
	CompilationUnit cu = (CompilationUnit)this.getParent().getParent();
	if (checkOwner && cu.isPrimary()) return this;
	return cu.getImport(getElementName());
}
/**
 * Returns true if the import is on-demand (ends with ".*")
 */
@Override
public boolean isOnDemand() {
	return this.isOnDemand;
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
protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
	buffer.append(tabString(tab));
	buffer.append("import "); //$NON-NLS-1$
	toStringName(buffer);
	if (info == null) {
		buffer.append(" (not open)"); //$NON-NLS-1$
	}
}
}
