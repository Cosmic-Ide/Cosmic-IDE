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
package org.eclipse.jdt.internal.compiler.classfmt;

import org.eclipse.jdt.internal.compiler.env.IBinaryNestedType;

/**
 * Describes one entry in the classes table of the InnerClasses attribute.
 * See the inner class specification (The class file attribute "InnerClasses").
 */

public class InnerClassInfo extends ClassFileStruct implements IBinaryNestedType {
	int innerClassNameIndex = -1;
	int outerClassNameIndex = -1;
	int innerNameIndex = -1;
	private char[] innerClassName;
	private char[] outerClassName;
	private char[] innerName;
	private int accessFlags = -1;
	private boolean readInnerClassName;
	private boolean readOuterClassName;
	private boolean readInnerName;

public InnerClassInfo(byte classFileBytes[], int offsets[], int offset) {
	super(classFileBytes, offsets, offset);
	this.innerClassNameIndex = u2At(0);
	this.outerClassNameIndex = u2At(2);
	this.innerNameIndex = u2At(4);
}

@Override
public char[] getEnclosingTypeName() {
	if (!this.readOuterClassName) {
		// read outer class name
		if (this.outerClassNameIndex != 0) {
			int utf8Offset =
				this.constantPoolOffsets[u2At(
					this.constantPoolOffsets[this.outerClassNameIndex] - this.structOffset + 1)]
					- this.structOffset;
			this.outerClassName = utf8At(utf8Offset + 3, u2At(utf8Offset + 1));
		}
		this.readOuterClassName = true;

	}
	return this.outerClassName;
}

@Override
public int getModifiers() {
	if (this.accessFlags == -1) {
		// read access flag
		this.accessFlags = u2At(6);
	}
	return this.accessFlags;
}

@Override
public char[] getName() {
	if (!this.readInnerClassName) {
		// read the inner class name
		if (this.innerClassNameIndex != 0) {
			int  classOffset = this.constantPoolOffsets[this.innerClassNameIndex] - this.structOffset;
			int utf8Offset = this.constantPoolOffsets[u2At(classOffset + 1)] - this.structOffset;
			this.innerClassName = utf8At(utf8Offset + 3, u2At(utf8Offset + 1));
		}
		this.readInnerClassName = true;
	}
	return this.innerClassName;
}
/**
 * Answer the source name of the member type.
 *
 * For example, p1.p2.A.M is M.
 * @return char[]
 */
public char[] getSourceName() {
	if (!this.readInnerName) {
		if (this.innerNameIndex != 0) {
			int utf8Offset = this.constantPoolOffsets[this.innerNameIndex] - this.structOffset;
			this.innerName = utf8At(utf8Offset + 3, u2At(utf8Offset + 1));
		}
		this.readInnerName = true;
	}
	return this.innerName;
}
/**
 * Answer the string representation of the receiver
 * @return java.lang.String
 */
@Override
public String toString() {
	StringBuilder buffer = new StringBuilder();
	if (getName() != null) {
		buffer.append(getName());
	}
	buffer.append("\n"); //$NON-NLS-1$
	if (getEnclosingTypeName() != null) {
		buffer.append(getEnclosingTypeName());
	}
	buffer.append("\n"); //$NON-NLS-1$
	if (getSourceName() != null) {
		buffer.append(getSourceName());
	}
	return buffer.toString();
}
/**
 * This method is used to fully initialize the contents of the receiver. All methodinfos, fields infos
 * will be therefore fully initialized and we can get rid of the bytes.
 */
void initialize() {
	getModifiers();
	getName();
	getSourceName();
	getEnclosingTypeName();
	reset();
}
}
