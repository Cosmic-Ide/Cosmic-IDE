/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.util;

import org.eclipse.jdt.core.util.ClassFormatException;
import org.eclipse.jdt.core.util.IAnnotation;
import org.eclipse.jdt.core.util.IConstantPool;
import org.eclipse.jdt.core.util.IParameterAnnotation;

/**
 * Default implementation of IParameterAnnotation
 */
public class ParameterAnnotation extends ClassFileStruct implements IParameterAnnotation {

	private static final IAnnotation[] NO_ENTRIES = new IAnnotation[0];

	private int annotationsNumber;
	private IAnnotation[] annotations;
	private int readOffset;

	/**
	 * Constructor for Annotation.
	 *
	 * @param classFileBytes
	 * @param constantPool
	 * @param offset
	 * @throws ClassFormatException
	 */
	public ParameterAnnotation(
			byte[] classFileBytes,
			IConstantPool constantPool,
			int offset) throws ClassFormatException {

		final int length = u2At(classFileBytes, 0, offset);
		this.readOffset = 2;
		this.annotationsNumber = length;
		if (length != 0) {
			this.annotations = new IAnnotation[length];
			for (int i = 0; i < length; i++) {
				Annotation annotation = new Annotation(classFileBytes, constantPool, offset + this.readOffset);
				this.annotations[i] = annotation;
				this.readOffset += annotation.sizeInBytes();
			}
		} else {
			this.annotations = NO_ENTRIES;
		}
	}

	int sizeInBytes() {
		return this.readOffset;
	}

	@Override
	public IAnnotation[] getAnnotations() {
		return this.annotations;
	}

	@Override
	public int getAnnotationsNumber() {
		return this.annotationsNumber;
	}
}
