/*******************************************************************************
 * Copyright (c) 2005, 2016 BEA Systems, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    tyeung@bea.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.classfmt;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.codegen.AttributeNamesConstants;

public class AnnotationMethodInfo extends MethodInfo {
	protected Object defaultValue = null;

public static MethodInfo createAnnotationMethod(byte classFileBytes[], int offsets[], int offset, long version) {
	MethodInfo methodInfo = new MethodInfo(classFileBytes, offsets, offset, version);
	int attributesCount = methodInfo.u2At(6);
	int readOffset = 8;
	AnnotationInfo[] annotations = null;
	Object defaultValue = null;
	TypeAnnotationInfo[] typeAnnotations = null;
	for (int i = 0; i < attributesCount; i++) {
		// check the name of each attribute
		int utf8Offset = methodInfo.constantPoolOffsets[methodInfo.u2At(readOffset)] - methodInfo.structOffset;
		char[] attributeName = methodInfo.utf8At(utf8Offset + 3, methodInfo.u2At(utf8Offset + 1));
		if (attributeName.length > 0) {
			switch(attributeName[0]) {
				case 'A':
					if (CharOperation.equals(attributeName, AttributeNamesConstants.AnnotationDefaultName)) {
						// readOffset + 6 so the offset is at the start of the 'member_value' entry
						// u2 attribute_name_index + u4 attribute_length = + 6
						AnnotationInfo info =
							new AnnotationInfo(methodInfo.reference, methodInfo.constantPoolOffsets, readOffset + 6 + methodInfo.structOffset);
						defaultValue = info.decodeDefaultValue();
					}
					break;
				case 'S' :
					if (CharOperation.equals(AttributeNamesConstants.SignatureName, attributeName))
						methodInfo.signatureUtf8Offset = methodInfo.constantPoolOffsets[methodInfo.u2At(readOffset + 6)] - methodInfo.structOffset;
					break;
				case 'R' :
					AnnotationInfo[] methodAnnotations = null;
					TypeAnnotationInfo[] methodTypeAnnotations = null;
					if (CharOperation.equals(attributeName, AttributeNamesConstants.RuntimeVisibleAnnotationsName)) {
						methodAnnotations = decodeMethodAnnotations(readOffset, true, methodInfo);
					} else if (CharOperation.equals(attributeName, AttributeNamesConstants.RuntimeInvisibleAnnotationsName)) {
						methodAnnotations = decodeMethodAnnotations(readOffset, false, methodInfo);
					} else if (CharOperation.equals(attributeName, AttributeNamesConstants.RuntimeVisibleTypeAnnotationsName)) {
						methodTypeAnnotations = decodeTypeAnnotations(readOffset, true, methodInfo);
					} else if (CharOperation.equals(attributeName, AttributeNamesConstants.RuntimeInvisibleTypeAnnotationsName)) {
						methodTypeAnnotations = decodeTypeAnnotations(readOffset, false, methodInfo);
					}
					if (methodAnnotations != null) {
						if (annotations == null) {
							annotations = methodAnnotations;
						} else {
							int length = annotations.length;
							AnnotationInfo[] newAnnotations = new AnnotationInfo[length + methodAnnotations.length];
							System.arraycopy(annotations, 0, newAnnotations, 0, length);
							System.arraycopy(methodAnnotations, 0, newAnnotations, length, methodAnnotations.length);
							annotations = newAnnotations;
						}
					} else if (methodTypeAnnotations != null) {
						if (typeAnnotations == null) {
							typeAnnotations = methodTypeAnnotations;
						} else {
							int length = typeAnnotations.length;
							TypeAnnotationInfo[] newAnnotations = new TypeAnnotationInfo[length + methodTypeAnnotations.length];
							System.arraycopy(typeAnnotations, 0, newAnnotations, 0, length);
							System.arraycopy(methodTypeAnnotations, 0, newAnnotations, length, methodTypeAnnotations.length);
							typeAnnotations = newAnnotations;
						}
					}
					break;
			}
		}
		readOffset += (6 + methodInfo.u4At(readOffset + 2));
	}
	methodInfo.attributeBytes = readOffset;

	if (defaultValue != null) {
		if (typeAnnotations != null)
			return new AnnotationMethodInfoWithTypeAnnotations(methodInfo, defaultValue, annotations, typeAnnotations);
		if (annotations != null) {
			return new AnnotationMethodInfoWithAnnotations(methodInfo, defaultValue, annotations);
		}
		return new AnnotationMethodInfo(methodInfo, defaultValue);
	}
	if (typeAnnotations != null)
		return new MethodInfoWithTypeAnnotations(methodInfo, annotations, null, typeAnnotations);
	if (annotations != null)
		return new MethodInfoWithAnnotations(methodInfo, annotations);
	return methodInfo;
}

AnnotationMethodInfo(MethodInfo methodInfo, Object defaultValue) {
	super(methodInfo.reference, methodInfo.constantPoolOffsets, methodInfo.structOffset, methodInfo.version);
	this.defaultValue = defaultValue;

	this.accessFlags = methodInfo.accessFlags;
	this.attributeBytes = methodInfo.attributeBytes;
	this.descriptor = methodInfo.descriptor;
	this.exceptionNames = methodInfo.exceptionNames;
	this.name = methodInfo.name;
	this.signature = methodInfo.signature;
	this.signatureUtf8Offset = methodInfo.signatureUtf8Offset;
	this.tagBits = methodInfo.tagBits;
}
@Override
public Object getDefaultValue() {
	return this.defaultValue;
}
}
