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
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *          Bug 407191 - [1.8] Binary access support for type annotations
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.classfmt;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.codegen.AttributeNamesConstants;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryField;
import org.eclipse.jdt.internal.compiler.env.IBinaryTypeAnnotation;
import org.eclipse.jdt.internal.compiler.impl.*;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.util.Util;

@SuppressWarnings("rawtypes")
public class FieldInfo extends ClassFileStruct implements IBinaryField, Comparable {
	protected int accessFlags;
	protected int attributeBytes;
	protected Constant constant;
	protected char[] descriptor;
	protected char[] name;
	protected char[] signature;
	protected int signatureUtf8Offset;
	protected long tagBits;
	protected Object wrappedConstantValue;
	protected long version;

public static FieldInfo createField(byte classFileBytes[], int offsets[], int offset, long version) {
	FieldInfo fieldInfo = new FieldInfo(classFileBytes, offsets, offset, version);

	int attributesCount = fieldInfo.u2At(6);
	int readOffset = 8;
	AnnotationInfo[] annotations = null;
	TypeAnnotationInfo[] typeAnnotations = null;
	for (int i = 0; i < attributesCount; i++) {
		// check the name of each attribute
		int utf8Offset = fieldInfo.constantPoolOffsets[fieldInfo.u2At(readOffset)] - fieldInfo.structOffset;
		char[] attributeName = fieldInfo.utf8At(utf8Offset + 3, fieldInfo.u2At(utf8Offset + 1));
		if (attributeName.length > 0) {
			switch(attributeName[0]) {
				case 'S' :
					if (CharOperation.equals(AttributeNamesConstants.SignatureName, attributeName))
						fieldInfo.signatureUtf8Offset = fieldInfo.constantPoolOffsets[fieldInfo.u2At(readOffset + 6)] - fieldInfo.structOffset;
					break;
				case 'R' :
					AnnotationInfo[] decodedAnnotations = null;
					TypeAnnotationInfo[] decodedTypeAnnotations = null;
					if (CharOperation.equals(attributeName, AttributeNamesConstants.RuntimeVisibleAnnotationsName)) {
						decodedAnnotations = fieldInfo.decodeAnnotations(readOffset, true);
					} else if (CharOperation.equals(attributeName, AttributeNamesConstants.RuntimeInvisibleAnnotationsName)) {
						decodedAnnotations = fieldInfo.decodeAnnotations(readOffset, false);
					} else if (CharOperation.equals(attributeName, AttributeNamesConstants.RuntimeVisibleTypeAnnotationsName)) {
						decodedTypeAnnotations = fieldInfo.decodeTypeAnnotations(readOffset, true);
					} else if (CharOperation.equals(attributeName, AttributeNamesConstants.RuntimeInvisibleTypeAnnotationsName)) {
						decodedTypeAnnotations = fieldInfo.decodeTypeAnnotations(readOffset, false);
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
		readOffset += (6 + fieldInfo.u4At(readOffset + 2));
	}
	fieldInfo.attributeBytes = readOffset;

	if (typeAnnotations != null)
		return new FieldInfoWithTypeAnnotation(fieldInfo, annotations, typeAnnotations);
	if (annotations != null)
		return new FieldInfoWithAnnotation(fieldInfo, annotations);
	return fieldInfo;
}

/**
 * @param classFileBytes byte[]
 * @param offsets int[]
 * @param offset int
 * @param version class file version
 */
protected FieldInfo (byte classFileBytes[], int offsets[], int offset, long version) {
	super(classFileBytes, offsets, offset);
	this.accessFlags = -1;
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
	return new String(getName()).compareTo(new String(((FieldInfo) o).getName()));
}
@Override
public boolean equals(Object o) {
	if (!(o instanceof FieldInfo)) {
		return false;
	}
	return CharOperation.equals(getName(), ((FieldInfo) o).getName());
}
@Override
public int hashCode() {
	return CharOperation.hashCode(getName());
}
/**
 * Return the constant of the field.
 * Return org.eclipse.jdt.internal.compiler.impl.Constant.NotAConstant if there is none.
 * @return org.eclipse.jdt.internal.compiler.impl.Constant
 */
@Override
public Constant getConstant() {
	if (this.constant == null) {
		// read constant
		readConstantAttribute();
	}
	return this.constant;
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
 * Answer an int whose bits are set according the access constants
 * defined by the VM spec.
 * Set the AccDeprecated and AccSynthetic bits if necessary
 * @return int
 */
@Override
public int getModifiers() {
	if (this.accessFlags == -1) {
		// compute the accessflag. Don't forget the deprecated attribute
		this.accessFlags = u2At(0);
		readModifierRelatedAttributes();
	}
	return this.accessFlags;
}
/**
 * Answer the name of the field.
 * @return char[]
 */
@Override
public char[] getName() {
	if (this.name == null) {
		// read the name
		int utf8Offset = this.constantPoolOffsets[u2At(2)] - this.structOffset;
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
		int utf8Offset = this.constantPoolOffsets[u2At(4)] - this.structOffset;
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
 * Return a wrapper that contains the constant of the field.
 * @return java.lang.Object
 */
public Object getWrappedConstantValue() {

	if (this.wrappedConstantValue == null) {
		if (hasConstant()) {
			Constant fieldConstant = getConstant();
			switch (fieldConstant.typeID()) {
				case TypeIds.T_int :
					this.wrappedConstantValue = Integer.valueOf(fieldConstant.intValue());
					break;
				case TypeIds.T_byte :
					this.wrappedConstantValue = Byte.valueOf(fieldConstant.byteValue());
					break;
				case TypeIds.T_short :
					this.wrappedConstantValue = Short.valueOf(fieldConstant.shortValue());
					break;
				case TypeIds.T_char :
					this.wrappedConstantValue = Character.valueOf(fieldConstant.charValue());
					break;
				case TypeIds.T_float :
					this.wrappedConstantValue = Float.valueOf(fieldConstant.floatValue());
					break;
				case TypeIds.T_double :
					this.wrappedConstantValue = Double.valueOf(fieldConstant.doubleValue());
					break;
				case TypeIds.T_boolean :
					this.wrappedConstantValue = Util.toBoolean(fieldConstant.booleanValue());
					break;
				case TypeIds.T_long :
					this.wrappedConstantValue = Long.valueOf(fieldConstant.longValue());
					break;
				case TypeIds.T_JavaLangString :
					this.wrappedConstantValue = fieldConstant.stringValue();
			}
		}
	}
	return this.wrappedConstantValue;
}
/**
 * Return true if the field has a constant value attribute, false otherwise.
 * @return boolean
 */
public boolean hasConstant() {
	return getConstant() != Constant.NotAConstant;
}
/**
 * This method is used to fully initialize the contents of the receiver. All methodinfos, fields infos
 * will be therefore fully initialized and we can get rid of the bytes.
 */
protected void initialize() {
	getModifiers();
	getName();
	getConstant();
	getTypeName();
	getGenericSignature();
	reset();
}
/**
 * Return true if the field is a synthetic field, false otherwise.
 * @return boolean
 */
public boolean isSynthetic() {
	return (getModifiers() & ClassFileConstants.AccSynthetic) != 0;
}
private void readConstantAttribute() {
	int attributesCount = u2At(6);
	int readOffset = 8;
	boolean isConstant = false;
	for (int i = 0; i < attributesCount; i++) {
		int utf8Offset = this.constantPoolOffsets[u2At(readOffset)] - this.structOffset;
		char[] attributeName = utf8At(utf8Offset + 3, u2At(utf8Offset + 1));
		if (CharOperation
			.equals(attributeName, AttributeNamesConstants.ConstantValueName)) {
			isConstant = true;
			// read the right constant
			int relativeOffset = this.constantPoolOffsets[u2At(readOffset + 6)] - this.structOffset;
			switch (u1At(relativeOffset)) {
				case ClassFileConstants.IntegerTag :
					char[] sign = getTypeName();
					if (sign.length == 1) {
						switch (sign[0]) {
							case 'Z' : // boolean constant
								this.constant = BooleanConstant.fromValue(i4At(relativeOffset + 1) == 1);
								break;
							case 'I' : // integer constant
								this.constant = IntConstant.fromValue(i4At(relativeOffset + 1));
								break;
							case 'C' : // char constant
								this.constant = CharConstant.fromValue((char) i4At(relativeOffset + 1));
								break;
							case 'B' : // byte constant
								this.constant = ByteConstant.fromValue((byte) i4At(relativeOffset + 1));
								break;
							case 'S' : // short constant
								this.constant = ShortConstant.fromValue((short) i4At(relativeOffset + 1));
								break;
							default:
								this.constant = Constant.NotAConstant;
						}
					} else {
						this.constant = Constant.NotAConstant;
					}
					break;
				case ClassFileConstants.FloatTag :
					this.constant = FloatConstant.fromValue(floatAt(relativeOffset + 1));
					break;
				case ClassFileConstants.DoubleTag :
					this.constant = DoubleConstant.fromValue(doubleAt(relativeOffset + 1));
					break;
				case ClassFileConstants.LongTag :
					this.constant = LongConstant.fromValue(i8At(relativeOffset + 1));
					break;
				case ClassFileConstants.StringTag :
					utf8Offset = this.constantPoolOffsets[u2At(relativeOffset + 1)] - this.structOffset;
					this.constant =
						StringConstant.fromValue(
							String.valueOf(utf8At(utf8Offset + 3, u2At(utf8Offset + 1))));
					break;
			}
		}
		readOffset += (6 + u4At(readOffset + 2));
	}
	if (!isConstant) {
		this.constant = Constant.NotAConstant;
	}
}
private void readModifierRelatedAttributes() {
	int attributesCount = u2At(6);
	int readOffset = 8;
	for (int i = 0; i < attributesCount; i++) {
		int utf8Offset = this.constantPoolOffsets[u2At(readOffset)] - this.structOffset;
		char[] attributeName = utf8At(utf8Offset + 3, u2At(utf8Offset + 1));
		// test added for obfuscated .class file. See 79772
		if (attributeName.length != 0) {
			switch(attributeName[0]) {
				case 'D' :
					if (CharOperation.equals(attributeName, AttributeNamesConstants.DeprecatedName))
						this.accessFlags |= ClassFileConstants.AccDeprecated;
					break;
				case 'S' :
					if (CharOperation.equals(attributeName, AttributeNamesConstants.SyntheticName))
						this.accessFlags |= ClassFileConstants.AccSynthetic;
					break;
			}
		}
		readOffset += (6 + u4At(readOffset + 2));
	}
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
	throw new ClassFormatException(ClassFormatException.ErrBadFieldInfo);
}
@Override
public String toString() {
	StringBuffer buffer = new StringBuffer(getClass().getName());
	toStringContent(buffer);
	return buffer.toString();
}
protected void toStringContent(StringBuffer buffer) {
	int modifiers = getModifiers();
	buffer
		.append('{')
		.append(
			((modifiers & ClassFileConstants.AccDeprecated) != 0 ? "deprecated " : Util.EMPTY_STRING) //$NON-NLS-1$
				+ ((modifiers & 0x0001) == 1 ? "public " : Util.EMPTY_STRING) //$NON-NLS-1$
				+ ((modifiers & 0x0002) == 0x0002 ? "private " : Util.EMPTY_STRING) //$NON-NLS-1$
				+ ((modifiers & 0x0004) == 0x0004 ? "protected " : Util.EMPTY_STRING) //$NON-NLS-1$
				+ ((modifiers & 0x0008) == 0x000008 ? "static " : Util.EMPTY_STRING) //$NON-NLS-1$
				+ ((modifiers & 0x0010) == 0x0010 ? "final " : Util.EMPTY_STRING) //$NON-NLS-1$
				+ ((modifiers & 0x0040) == 0x0040 ? "volatile " : Util.EMPTY_STRING) //$NON-NLS-1$
				+ ((modifiers & 0x0080) == 0x0080 ? "transient " : Util.EMPTY_STRING)) //$NON-NLS-1$
		.append(getTypeName())
		.append(' ')
		.append(getName())
		.append(' ')
		.append(getConstant())
		.append('}')
		.toString();
}
}
