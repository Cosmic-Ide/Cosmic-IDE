/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.internal.compiler.lookup.Binding;

/**
 * @see IField
 */

public class SourceField extends NamedMember implements IField {

/**
 * Constructs a handle to the field with the given name in the specified type.
 */
protected SourceField(JavaElement parent, String name) {
	super(parent, name);
}
@Override
public boolean equals(Object o) {
	if (!(o instanceof SourceField)) return false;
	return super.equals(o);
}
@Override
public ASTNode findNode(org.eclipse.jdt.core.dom.CompilationUnit ast) {
	// For field declarations, a variable declaration fragment is returned
	// Return the FieldDeclaration instead
	// For enum constant declaration, we return the node directly
	ASTNode node = super.findNode(ast);
	if (node == null) return null;
	if (node.getNodeType() == ASTNode.ENUM_CONSTANT_DECLARATION) {
		return node;
	}
	return node.getParent();
}
/**
 * @see IField
 */
@Override
public Object getConstant() throws JavaModelException {
	Object constant = null;
	SourceFieldElementInfo info = (SourceFieldElementInfo) getElementInfo();
	final char[] constantSourceChars = info.initializationSource;
	if (constantSourceChars == null) {
		return null;
	}

	String constantSource = new String(constantSourceChars);
	String signature = info.getTypeSignature();
	try {
		if (signature.equals(Signature.SIG_INT)) {
			constant = Integer.valueOf(constantSource);
		} else if (signature.equals(Signature.SIG_SHORT)) {
			constant = Short.valueOf(constantSource);
		} else if (signature.equals(Signature.SIG_BYTE)) {
			constant = Byte.valueOf(constantSource);
		} else if (signature.equals(Signature.SIG_BOOLEAN)) {
			constant = Boolean.valueOf(constantSource);
		} else if (signature.equals(Signature.SIG_CHAR)) {
			if (constantSourceChars.length != 3) {
				return null;
			}
			constant = Character.valueOf(constantSourceChars[1]);
		} else if (signature.equals(Signature.SIG_DOUBLE)) {
			constant = Double.valueOf(constantSource);
		} else if (signature.equals(Signature.SIG_FLOAT)) {
			constant = Float.valueOf(constantSource);
		} else if (signature.equals(Signature.SIG_LONG)) {
			if (constantSource.endsWith("L") || constantSource.endsWith("l")) { //$NON-NLS-1$ //$NON-NLS-2$
				int index = constantSource.lastIndexOf("L");//$NON-NLS-1$
				if (index != -1) {
					constant = Long.valueOf(constantSource.substring(0, index));
				} else {
					constant = Long.valueOf(constantSource.substring(0, constantSource.lastIndexOf("l")));//$NON-NLS-1$
				}
			} else {
				constant = Long.valueOf(constantSource);
			}
		} else if (signature.equals("QString;")) {//$NON-NLS-1$
			constant = constantSource;
		} else if (signature.equals("Qjava.lang.String;")) {//$NON-NLS-1$
			constant = constantSource;
		}
	} catch (NumberFormatException e) {
		// not a parsable constant
		return null;
	}
	return constant;
}
/**
 * @see IJavaElement
 */
@Override
public int getElementType() {
	return FIELD;
}

@Override
public String getKey() {
	try {
		return getKey(this, false/*don't open*/);
	} catch (JavaModelException e) {
		// happen only if force open is true
		return null;
	}
}
/**
 * @see JavaElement#getHandleMemento()
 */
@Override
protected char getHandleMementoDelimiter() {
	return JavaElement.JEM_FIELD;
}

@Override
public JavaElement getPrimaryElement(boolean checkOwner) {
	if (checkOwner) {
		CompilationUnit cu = (CompilationUnit)getAncestor(COMPILATION_UNIT);
		if (cu.isPrimary()) return this;
	}
	IJavaElement primaryParent =this.getParent().getPrimaryElement(false);
	return (JavaElement)((IType)primaryParent).getField(this.name);
}
/**
 * @see IField
 */
@Override
public String getTypeSignature() throws JavaModelException {
	SourceFieldElementInfo info = (SourceFieldElementInfo) getElementInfo();
	return info.getTypeSignature();
}

@Override
public boolean isEnumConstant() throws JavaModelException {
	return Flags.isEnum(getFlags());
}

@Override
public boolean isResolved() {
	return false;
}
@Override
public JavaElement resolved(Binding binding) {
	SourceRefElement resolvedHandle = new ResolvedSourceField(this.getParent(), this.name, new String(binding.computeUniqueKey()));
	resolvedHandle.occurrenceCount = this.occurrenceCount;
	return resolvedHandle;
}
/**
 * @private Debugging purposes
 */
@Override
protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
	buffer.append(tabString(tab));
	if (info == null) {
		toStringName(buffer);
		buffer.append(" (not open)"); //$NON-NLS-1$
	} else if (info == NO_INFO) {
		toStringName(buffer);
	} else {
		try {
			buffer.append(Signature.toString(getTypeSignature()));
			buffer.append(" "); //$NON-NLS-1$
			toStringName(buffer);
		} catch (JavaModelException e) {
			buffer.append("<JavaModelException in toString of " + getElementName()); //$NON-NLS-1$
		}
	}
}
@Override
public boolean isRecordComponent() throws JavaModelException {
	SourceFieldElementInfo info = (SourceFieldElementInfo) getElementInfo();
	return info.isRecordComponent;
}
}
