/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.ISourceField;
import org.eclipse.jdt.internal.compiler.env.ISourceImport;
import org.eclipse.jdt.internal.compiler.env.ISourceMethod;
import org.eclipse.jdt.internal.compiler.env.ISourceType;

/**
 * Element info for an IType element that originated from source.
 */
public class SourceTypeElementInfo extends AnnotatableInfo implements ISourceType {

	protected static final ISourceImport[] NO_IMPORTS = new ISourceImport[0];
	protected static final InitializerElementInfo[] NO_INITIALIZERS = new InitializerElementInfo[0];
	protected static final SourceField[] NO_FIELDS = new SourceField[0];
	protected static final SourceMethod[] NO_METHODS = new SourceMethod[0];
	protected static final SourceType[] NO_TYPES = new SourceType[0];

	protected IJavaElement[] children = JavaElement.NO_ELEMENTS;

	/**
	 * The name of the superclass for this type. This name
	 * is fully qualified for binary types and is NOT always
	 * fully qualified for source types.
	 */
	protected char[] superclassName;

	/**
	 * The names of the interfaces this type implements or
	 * extends. These names are fully qualified in the case
	 * of a binary type, and are NOT always fully qualified in the
	 * case of a source type
	 */
	protected char[][] superInterfaceNames;

	protected char[][] permittedTypeNames;

	/**
	 * Backpointer to my type handle - useful for translation
	 * from info to handle.
	 */
	protected IType handle = null;

	/*
	 * The type parameters of this source type. Empty if none.
	 */
	protected ITypeParameter[] typeParameters = TypeParameter.NO_TYPE_PARAMETERS;

	/*
	 * A map from an IJavaElement (this type or a child of this type) to a String[] (the categories of this element)
	 */
	protected HashMap<IJavaElement,String[]> categories;

protected void addCategories(IJavaElement element, char[][] elementCategories) {
	if (elementCategories == null) return;
	if (this.categories == null)
		this.categories = new HashMap<>();
	this.categories.put(element, CharOperation.toStrings(elementCategories));
}

/*
 * Return a map from an IJavaElement (this type or a child of this type) to a String[] (the categories of this element)
 */
public HashMap<IJavaElement, String[]> getCategories() {
	return this.categories;
}

@Override
public IJavaElement[] getChildren() {
	return this.children;
}

/**
 * Returns the ISourceType that is the enclosing type for this
 * type, or <code>null</code> if this type is a top level type.
 */
@Override
public ISourceType getEnclosingType() {
	IJavaElement parent= this.handle.getParent();
	if (parent != null && parent.getElementType() == IJavaElement.TYPE) {
		try {
			return (ISourceType)((JavaElement)parent).getElementInfo();
		} catch (JavaModelException e) {
			return null;
		}
	} else {
		return null;
	}
}
/**
 * @see ISourceType
 */
@Override
public ISourceField[] getFields() {
	SourceField[] fieldHandles = getFieldHandles();
	int length = fieldHandles.length;
	ISourceField[] fields = new ISourceField[length];
	for (int i = 0; i < length; i++) {
		try {
			ISourceField field = (ISourceField) fieldHandles[i].getElementInfo();
			fields[i] = field;
		} catch (JavaModelException e) {
			// ignore
		}
	}
	return fields;
}
public SourceField[] getFieldHandles() {
	int length = this.children.length;
	if (length == 0) return NO_FIELDS;
	SourceField[] fields = new SourceField[length];
	int fieldIndex = 0;
	for (int i = 0; i < length; i++) {
		IJavaElement child = this.children[i];
		if (child instanceof SourceField)
			fields[fieldIndex++] = (SourceField) child;
	}
	if (fieldIndex == 0) return NO_FIELDS;
	if (fieldIndex < length)
		System.arraycopy(fields, 0, fields = new SourceField[fieldIndex], 0, fieldIndex);
	return fields;
}
/**
 * @see org.eclipse.jdt.internal.compiler.env.IDependent#getFileName()
 */
@Override
public char[] getFileName() {
	return this.handle.getPath().toString().toCharArray();
}
/**
 * Returns the handle for this type info
 */
public IType getHandle() {
	return this.handle;
}
/*
 * Returns the InitializerElementInfos for this type.
 * Returns an empty array if none.
 */
public InitializerElementInfo[] getInitializers() {
	int length = this.children.length;
	if (length == 0) return NO_INITIALIZERS;
	InitializerElementInfo[] initializers = new InitializerElementInfo[length];
	int initializerIndex = 0;
	for (int i = 0; i < length; i++) {
		IJavaElement child = this.children[i];
		if (child instanceof Initializer) {
			try {
				InitializerElementInfo initializer = (InitializerElementInfo)((Initializer)child).getElementInfo();
				initializers[initializerIndex++] = initializer;
			} catch (JavaModelException e) {
				// ignore
			}
		}
	}
	if (initializerIndex == 0) return NO_INITIALIZERS;
	System.arraycopy(initializers, 0, initializers = new InitializerElementInfo[initializerIndex], 0, initializerIndex);
	return initializers;
}
/**
 * @see ISourceType
 */
@Override
public char[][] getInterfaceNames() {
	if (isAnonymous()) { // if anonymous type
		return null;
	}
	return this.superInterfaceNames;
}
/**
 * @see ISourceType
 */
@Override
public char[][] getPermittedSubtypeNames() {
	if (isAnonymous()) { // if anonymous type
		return null;
	}
	return this.permittedTypeNames;
}
/**
 * @see ISourceType
 */
@Override
public ISourceType[] getMemberTypes() {
	SourceType[] memberTypeHandles = getMemberTypeHandles();
	int length = memberTypeHandles.length;
	ISourceType[] memberTypes = new ISourceType[length];
	for (int i = 0; i < length; i++) {
		try {
			ISourceType type = (ISourceType) memberTypeHandles[i].getElementInfo();
			memberTypes[i] = type;
		} catch (JavaModelException e) {
			// ignore
		}
	}
	return memberTypes;
}
public SourceType[] getMemberTypeHandles() {
	int length = this.children.length;
	if (length == 0) return NO_TYPES;
	SourceType[] memberTypes = new SourceType[length];
	int typeIndex = 0;
	for (int i = 0; i < length; i++) {
		IJavaElement child = this.children[i];
		if (child instanceof SourceType)
			memberTypes[typeIndex++] = (SourceType)child;
	}
	if (typeIndex == 0) return NO_TYPES;
	if (typeIndex < length)
		System.arraycopy(memberTypes, 0, memberTypes = new SourceType[typeIndex], 0, typeIndex);
	return memberTypes;
}
/**
 * @see ISourceType
 */
@Override
public ISourceMethod[] getMethods() {
	SourceMethod[] methodHandles = getMethodHandles();
	int length = methodHandles.length;
	ISourceMethod[] methods = new ISourceMethod[length];
	int methodIndex = 0;
	for (int i = 0; i < length; i++) {
		try {
			ISourceMethod method = (ISourceMethod) methodHandles[i].getElementInfo();
			methods[methodIndex++] = method;
		} catch (JavaModelException e) {
			// ignore
		}
	}
	return methods;
}
public SourceMethod[] getMethodHandles() {
	int length = this.children.length;
	if (length == 0) return NO_METHODS;
	SourceMethod[] methods = new SourceMethod[length];
	int methodIndex = 0;
	for (int i = 0; i < length; i++) {
		IJavaElement child = this.children[i];
		if (child instanceof SourceMethod)
			methods[methodIndex++] = (SourceMethod) child;
	}
	if (methodIndex == 0) return NO_METHODS;
	if (methodIndex < length)
		System.arraycopy(methods, 0, methods = new SourceMethod[methodIndex], 0, methodIndex);
	return methods;
}
/**
 * @see org.eclipse.jdt.internal.compiler.env.ISourceType#getName()
 */
@Override
public char[] getName() {
	return this.handle.getElementName().toCharArray();
}
/**
 * @see ISourceType
 */
@Override
public char[] getSuperclassName() {
	if (isAnonymous()) { // if anonymous type
		char[][] interfaceNames = this.superInterfaceNames;
		if (interfaceNames != null && interfaceNames.length > 0) {
			return interfaceNames[0];
		}
	}
	return this.superclassName;
}
@Override
public char[][][] getTypeParameterBounds() {
	int length = this.typeParameters.length;
	char[][][] typeParameterBounds = new char[length][][];
	for (int i = 0; i < length; i++) {
		try {
			TypeParameterElementInfo info = (TypeParameterElementInfo) ((JavaElement)this.typeParameters[i]).getElementInfo();
			typeParameterBounds[i] = info.bounds;
		} catch (JavaModelException e) {
			// type parameter does not exist: ignore
		}
	}
	return typeParameterBounds;
}
@Override
public char[][] getTypeParameterNames() {
	int length = this.typeParameters.length;
	if (length == 0) return CharOperation.NO_CHAR_CHAR;
	char[][] typeParameterNames = new char[length][];
	for (int i = 0; i < length; i++) {
		typeParameterNames[i] = this.typeParameters[i].getElementName().toCharArray();
	}
	return typeParameterNames;
}
/**
 * @see ISourceType
 */
@Override
public boolean isBinaryType() {
	return false;
}

@Override
public boolean isAnonymous() {
	try {
		return this.handle.isAnonymous();
	} catch (JavaModelException e) {
		return false;
	}
}
/*
 * Returns whether the source type is an anonymous type of a member type.
 */
public boolean isAnonymousMember() {
	return false;
}
/**
 * Sets the handle for this type info
 */
protected void setHandle(IType handle) {
	this.handle = handle;
}
/**
 * Sets the (unresolved) name of this type's superclass
 */
protected void setSuperclassName(char[] superclassName) {
	this.superclassName = superclassName;
}
/**
 * Sets the (unresolved) names of the interfaces this type implements or extends
 */
protected void setSuperInterfaceNames(char[][] superInterfaceNames) {
	this.superInterfaceNames = superInterfaceNames;
}
protected void setPermittedSubtypeNames(char[][] permittedTypeNames) {
	this.permittedTypeNames = permittedTypeNames;
}
@Override
public String toString() {
	return "Info for " + this.handle.toString(); //$NON-NLS-1$
}
}
