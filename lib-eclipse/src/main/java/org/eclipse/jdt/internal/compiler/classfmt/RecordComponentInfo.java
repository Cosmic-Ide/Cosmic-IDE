/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.codegen.AttributeNamesConstants;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryTypeAnnotation;
import org.eclipse.jdt.internal.compiler.env.IRecordComponent;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;

/*
 * TODO: Refactor common code from FieldInfo since this mirrors field info mostly except for
 * the accessflags and the attribute_length size. However, since this is still in preview stage,
 * it is too early to disturb the field_info code. To be done if this gets standardized.
 */
@SuppressWarnings("rawtypes")
public class RecordComponentInfo extends ClassFileStruct implements IRecordComponent, Comparable {
	protected int attributeBytes;
	protected char[] descriptor;
	protected char[] name;
	protected char[] signature;
	protected int signatureUtf8Offset;
	protected long tagBits;
	protected long version;

public static RecordComponentInfo createComponent(byte classFileBytes[], int offsets[], int offset, long version) {
	RecordComponentInfo componentInfo = new RecordComponentInfo(classFileBytes, offsets, offset, version);

	int attributesCount = componentInfo.u2At(4);
	int readOffset = 6;
	AnnotationInfo[] annotations = null;
	TypeAnnotationInfo[] typeAnnotations = null;
	for (int i = 0; i < attributesCount; i++) {
		// check the name of each attribute
		int utf8Offset = componentInfo.constantPoolOffsets[componentInfo.u2At(readOffset)] - componentInfo.structOffset;
		char[] attributeName = componentInfo.utf8At(utf8Offset + 3, componentInfo.u2At(utf8Offset + 1));
		if (attributeName.length > 0) {
			switch(attributeName[0]) {
				case 'S' :
					if (CharOperation.equals(AttributeNamesConstants.SignatureName, attributeName))
						componentInfo.signatureUtf8Offset = componentInfo.constantPoolOffsets[componentInfo.u2At(readOffset + 6)] - componentInfo.structOffset;
					break;
				case 'R' :
					AnnotationInfo[] decodedAnnotations = null;
					TypeAnnotationInfo[] decodedTypeAnnotations = null;
					if (CharOperation.equals(attributeName, AttributeNamesConstants.RuntimeVisibleAnnotationsName)) {
						decodedAnnotations = componentInfo.decodeAnnotations(readOffset, true);
					} else if (CharOperation.equals(attributeName, AttributeNamesConstants.RuntimeInvisibleAnnotationsName)) {
						decodedAnnotations = componentInfo.decodeAnnotations(readOffset, false);
					} else if (CharOperation.equals(attributeName, AttributeNamesConstants.RuntimeVisibleTypeAnnotationsName)) {
						decodedTypeAnnotations = componentInfo.decodeTypeAnnotations(readOffset, true);
					} else if (CharOperation.equals(attributeName, AttributeNamesConstants.RuntimeInvisibleTypeAnnotationsName)) {
						decodedTypeAnnotations = componentInfo.decodeTypeAnnotations(readOffset, false);
					}
					if (decodedAnnotations != null) {
						if (annotations == null) {
							annotations = decodedAnnotations;
						} else {
							int length = annotations.length;
							AnnotationInfo[] combined = new AnnotationInfo[length + decodedAnnotations.length];
							System.arraycopy(annotations, 0, combined, 0, length);
							System.arraycopy(decodedAnnotations, 0, combined, length, decodedAnnotations.length);
							annotations = combined;
						}
					} else if (decodedTypeAnnotations != null) {
						if (typeAnnotations == null) {
							typeAnnotations = decodedTypeAnnotations;
						} else {
							int length = typeAnnotations.length;
							TypeAnnotationInfo[] combined = new TypeAnnotationInfo[length + decodedTypeAnnotations.length];
							System.arraycopy(typeAnnotations, 0, combined, 0, length);
							System.arraycopy(decodedTypeAnnotations, 0, combined, length, decodedTypeAnnotations.length);
							typeAnnotations = combined;
						}
					}
			}
		}
		readOffset += (6 + componentInfo.u4At(readOffset + 2));
	}
	componentInfo.attributeBytes = readOffset;

	if (typeAnnotations != null)
		return new ComponentInfoWithTypeAnnotation(componentInfo, annotations, typeAnnotations);
	if (annotations != null)
		return new ComponentInfoWithAnnotation(componentInfo, annotations);
	return componentInfo;
}

/**
 * @param classFileBytes byte[]
 * @param offsets int[]
 * @param offset int
 * @param version class file version
 */
protected RecordComponentInfo (byte classFileBytes[], int offsets[], int offset, long version) {
	super(classFileBytes, offsets, offset);
	this.signatureUtf8Offset = -1;
	this.version = version;
}
private AnnotationInfo[] decodeAnnotations(int offset, boolean runtimeVisible) {
	int numberOfAnnotations = u2At(offset + 6);
	if (numberOfAnnotations > 0) {
		int readOffset = offset + 8;
		AnnotationInfo[] newInfos = null;
		int newInfoCount = 0;
		for (int i = 0; i < numberOfAnnotations; i++) {
			// With the last parameter being 'false', the data structure will not be flushed out
			AnnotationInfo newInfo = new AnnotationInfo(this.reference, this.constantPoolOffsets,
				readOffset + this.structOffset, runtimeVisible, false);
			readOffset += newInfo.readOffset;
			long standardTagBits = newInfo.standardAnnotationTagBits;
			if (standardTagBits != 0) {
				this.tagBits |= standardTagBits;
				if (this.version < ClassFileConstants.JDK9 || (standardTagBits & TagBits.AnnotationDeprecated) == 0)
					continue;
			}
			if (newInfos == null)
				newInfos = new AnnotationInfo[numberOfAnnotations - i];
			newInfos[newInfoCount++] = newInfo;
		}
		if (newInfos != null) {
			if (newInfoCount != newInfos.length)
				System.arraycopy(newInfos, 0, newInfos = new AnnotationInfo[newInfoCount], 0, newInfoCount);
			return newInfos;
		}
	}
	return null; // nothing to record
}

TypeAnnotationInfo[] decodeTypeAnnotations(int offset, boolean runtimeVisible) {
	int numberOfAnnotations = u2At(offset + 6);
	if (numberOfAnnotations > 0) {
		int readOffset = offset + 8;
		TypeAnnotationInfo[] typeAnnos = new TypeAnnotationInfo[numberOfAnnotations];
		for (int i = 0; i < numberOfAnnotations; i++) {
			TypeAnnotationInfo newInfo = new TypeAnnotationInfo(this.reference, this.constantPoolOffsets, readOffset + this.structOffset, runtimeVisible, false);
			readOffset += newInfo.readOffset;
			typeAnnos[i] = newInfo;
		}
		return typeAnnos;
	}
	return null;
}

@Override
public int compareTo(Object o) {
	return new String(getName()).compareTo(new String(((RecordComponentInfo) o).getName()));
}
@Override
public boolean equals(Object o) {
	if (!(o instanceof RecordComponentInfo)) {
		return false;
	}
	return CharOperation.equals(getName(), ((RecordComponentInfo) o).getName());
}
@Override
public int hashCode() {
	return CharOperation.hashCode(getName());
}
@Override
public char[] getGenericSignature() {
	if (this.signatureUtf8Offset != -1) {
		if (this.signature == null) {
			// decode the signature
			this.signature = utf8At(this.signatureUtf8Offset + 3, u2At(this.signatureUtf8Offset + 1));
		}
		return this.signature;
	}
	return null;
}
/**
 * Answer the name of the component.
 * @return char[]
 */
@Override
public char[] getName() {
	if (this.name == null) {
		// read the name
		int utf8Offset = this.constantPoolOffsets[u2At(0)] - this.structOffset;
		this.name = utf8At(utf8Offset + 3, u2At(utf8Offset + 1));
	}
	return this.name;
}
@Override
public long getTagBits() {
	return this.tagBits;
}
/**
 * Answer the resolved name of the receiver's type in the
 * class file format as specified in section 4.3.2 of the Java 2 VM spec.
 *
 * For example:
 *   - java.lang.String is Ljava/lang/String;
 *   - an int is I
 *   - a 2 dimensional array of strings is [[Ljava/lang/String;
 *   - an array of floats is [F
 * @return char[]
 */
@Override
public char[] getTypeName() {
	if (this.descriptor == null) {
		// read the signature
		int utf8Offset = this.constantPoolOffsets[u2At(2)] - this.structOffset;
		this.descriptor = utf8At(utf8Offset + 3, u2At(utf8Offset + 1));
	}
	return this.descriptor;
}
/**
 * @return the annotations or null if there is none.
 */
@Override
public IBinaryAnnotation[] getAnnotations() {
	return null;
}

@Override
public IBinaryTypeAnnotation[] getTypeAnnotations() {
	return null;
}
/**
 * This method is used to fully initialize the contents of the receiver. All methodinfos, fields infos
 * will be therefore fully initialized and we can get rid of the bytes.
 */
protected void initialize() {
	getName();
	getTypeName();
	getGenericSignature();
	reset();
}
/**
 * Answer the size of the receiver in bytes.
 *
 * @return int
 */
public int sizeInBytes() {
	return this.attributeBytes;
}
public void throwFormatException() throws ClassFormatException {
	throw new ClassFormatException(ClassFormatException.ErrBadComponentInfo);
}
@Override
public String toString() {
	StringBuffer buffer = new StringBuffer(getClass().getName());
	toStringContent(buffer);
	return buffer.toString();
}
protected void toStringContent(StringBuffer buffer) {
	buffer
		.append('{')
		.append(getTypeName())
		.append(' ')
		.append(getName())
		.append(' ')
		.append('}')
		.toString();
}

@Override
public Constant getConstant() {
	// Doesn't really apply to a record component.
	return null;
}

@Override
public int getModifiers() {
	// Doesn't really apply to a record component.
	return 0;
}
}
