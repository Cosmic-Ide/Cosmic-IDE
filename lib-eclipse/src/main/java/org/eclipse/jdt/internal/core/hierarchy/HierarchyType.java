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
package org.eclipse.jdt.internal.core.hierarchy;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.compiler.env.IGenericType;

/**
 *
 * Partial implementation of an IGenericType used to
 * answer hierarchies.
 */
public class HierarchyType implements IGenericType {

	public IType typeHandle;
	public char[] name;
	public int modifiers;
	public char[] superclassName;
	public char[][] superInterfaceNames;
	public boolean anonymous;

public HierarchyType(
	IType typeHandle,
	char[] name,
	int modifiers,
	char[] superclassName,
	char[][] superInterfaceNames,
	boolean anonymous) {

	this.typeHandle = typeHandle;
	this.name = name;
	this.modifiers = modifiers;
	this.superclassName = superclassName;
	this.superInterfaceNames = superInterfaceNames;
	this.anonymous = anonymous;
}
/**
 * @see org.eclipse.jdt.internal.compiler.env.IDependent#getFileName()
 */
@Override
public char[] getFileName() {
	return this.typeHandle.getCompilationUnit().getElementName().toCharArray();
}

/**
 * Answer an int whose bits are set according the access constants
 * defined by the VM spec.
 */
@Override
public int getModifiers() {
	return this.modifiers;
}
/**
 * Answer whether the receiver contains the resolved binary form
 * or the unresolved source form of the type.
 */
@Override
public boolean isBinaryType() {
	return false;
}
/**
 * Answer whether the receiver is an anonymous type
 */
public boolean isAnonymous() {
	return this.anonymous;
}
}
