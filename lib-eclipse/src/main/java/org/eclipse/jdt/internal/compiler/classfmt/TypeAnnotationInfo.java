/*******************************************************************************
 * Copyright (c) 2016 GoPivotal, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *          Bug 407191 - [1.8] Binary access support for type annotations
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.classfmt;

import java.util.Arrays;

import org.eclipse.jdt.internal.compiler.codegen.AnnotationTargetTypeConstants;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryTypeAnnotation;

/**
 * The TypeAnnotationInfo class does not currently support type annotations within code
 * blocks (those that have a target type of 0x40 and higher) - it is not yet clear that
 * these need to be accessible.
 */
public class TypeAnnotationInfo extends ClassFileStruct implements IBinaryTypeAnnotation {

	private AnnotationInfo annotation;

	private int targetType = 0;

	// info is used in different ways:
	// TargetType 0x00: CLASS_TYPE_PARAMETER: type parameter index
	// TargetType 0x01: METHOD_TYPE_PARAMETER: type parameter index
	// TargetType 0x10: CLASS_EXTENDS: supertype index (-1 = superclass, 0..N superinterface)
	// TargetType 0x11: CLASS_TYPE_PARAMETER_BOUND: type parameter index
	// TargetType 0x12: METHOD_TYPE_PARAMETER_BOUND: type parameter index
	// TargetType 0x16: METHOD_FORMAL_PARAMETER: method formal parameter index
	// TargetType 0x17: THROWS: throws type index
	private int info;

	// TargetType 0x11: CLASS_TYPE_PARAMETER_BOUND: bound index
	// TargetType 0x12: METHOD_TYPE_PARAMETER_BOUND: bound index
	private int info2;

	private int[] typePath; // each pair of ints in the array is a type path entry

	int readOffset = 0;


TypeAnnotationInfo(byte[] classFileBytes, int[] contantPoolOffsets, int offset) {
	super(classFileBytes, contantPoolOffsets, offset);
}

TypeAnnotationInfo(byte[] classFileBytes, int[] contantPoolOffsets, int offset, boolean runtimeVisible, boolean populate) {
	this(classFileBytes, contantPoolOffsets, offset);
	this.readOffset = 0;
	this.targetType = u1At(0);
	switch (this.targetType) {
		case AnnotationTargetTypeConstants.CLASS_TYPE_PARAMETER:
		case AnnotationTargetTypeConstants.METHOD_TYPE_PARAMETER:
			this.info = u1At(1); // typeParameterIndex
			this.readOffset += 2;
			break;

		case AnnotationTargetTypeConstants.CLASS_EXTENDS:
			this.info = u2At(1); // supertypeIndex
			this.readOffset += 3;
			break;

		case AnnotationTargetTypeConstants.CLASS_TYPE_PARAMETER_BOUND:
		case AnnotationTargetTypeConstants.METHOD_TYPE_PARAMETER_BOUND:
			this.info = u1At(1); // typeParameterIndex
			this.info2 = u1At(2); // boundIndex;
			this.readOffset += 3;
			break;

		case AnnotationTargetTypeConstants.FIELD:
		case AnnotationTargetTypeConstants.METHOD_RETURN:
		case AnnotationTargetTypeConstants.METHOD_RECEIVER:
			this.readOffset ++;
			break;

		case AnnotationTargetTypeConstants.METHOD_FORMAL_PARAMETER :
			this.info = u1At(1); // methodFormalParameterIndex
			this.readOffset += 2;
			break;

		case AnnotationTargetTypeConstants.THROWS :
			this.info = u2At(1); // throwsTypeIndex
			this.readOffset += 3;
			break;

		default:
			throw new IllegalStateException("Target type not handled "+this.targetType); //$NON-NLS-1$
	}
	int typePathLength = u1At(this.readOffset);
	this.readOffset ++;
	if (typePathLength == 0) {
		this.typePath = NO_TYPE_PATH;
	} else {
		this.typePath = new int[typePathLength*2];
		int index = 0;
		for (int i = 0; i < typePathLength; i++) {
			this.typePath[index++] = u1At(this.readOffset++); // entry kind
			this.typePath[index++] = u1At(this.readOffset++); // type argument index
		}
	}
	this.annotation = new AnnotationInfo(classFileBytes, this.constantPoolOffsets, this.structOffset + this.readOffset, runtimeVisible, populate);
	this.readOffset += this.annotation.readOffset;
}

@Override
public IBinaryAnnotation getAnnotation() {
	return this.annotation;
}

protected void initialize() {
	this.annotation.initialize();
}

@Override
protected void reset() {
	this.annotation.reset();
	super.reset();
}

@Override
public String toString() {
	return BinaryTypeFormatter.annotationToString(this);
}

@Override
public int getTargetType() {
	return this.targetType;
}

@Override
public int getSupertypeIndex() {
	// assert this.targetType == 0x10
	return this.info;
}

@Override
public int getTypeParameterIndex() {
	// assert this.targetType == 0x00 or 0x01
	return this.info;
}

@Override
public int getBoundIndex() {
	// assert this.targetType == 0x11 or 0x12
	return this.info2;
}

@Override
public int getMethodFormalParameterIndex() {
	// assert this.targetType == 0x16
	return this.info;
}

@Override
public int getThrowsTypeIndex() {
	// assert this.targetType == 0x17
	return this.info;
}

@Override
public int[] getTypePath() {
	return this.typePath;
}

@Override
public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + this.targetType;
	result = prime * result + this.info;
	result = prime * result + this.info2;
	if (this.typePath != null) {
		for (int i = 0, max = this.typePath.length; i < max; i++) {
			result = prime * result + this.typePath[i];
		}
	}
	return result;
}

@Override
public boolean equals(Object obj) {
	if (this == obj) {
		return true;
	}
	if (obj == null) {
		return false;
	}
	if (getClass() != obj.getClass()) {
		return false;
	}

	TypeAnnotationInfo other = (TypeAnnotationInfo) obj;

	if (this.targetType != other.targetType) {
		return false;
	}

	if (this.info != other.info) {
		return false;
	}

	if (this.info2 != other.info2) {
		return false;
	}

	if (!Arrays.equals(this.typePath, other.typePath)) {
		return false;
	}

	return this.annotation.equals(other.annotation);
}
}
