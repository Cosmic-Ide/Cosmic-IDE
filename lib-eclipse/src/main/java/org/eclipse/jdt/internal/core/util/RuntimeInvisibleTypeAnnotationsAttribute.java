/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
 *        Andy Clement - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *******************************************************************************/
package org.eclipse.jdt.internal.core.util;

import org.eclipse.jdt.core.util.ClassFormatException;
import org.eclipse.jdt.core.util.IConstantPool;
import org.eclipse.jdt.core.util.IExtendedAnnotation;
import org.eclipse.jdt.core.util.IRuntimeInvisibleTypeAnnotationsAttribute;

/**
 * Default implementation of IRuntimeInvisibleTypeAnnotations
 */
public class RuntimeInvisibleTypeAnnotationsAttribute
	extends ClassFileAttribute
	implements IRuntimeInvisibleTypeAnnotationsAttribute {

	private static final IExtendedAnnotation[] NO_ENTRIES = new IExtendedAnnotation[0];
	private int extendedAnnotationsNumber;
	private IExtendedAnnotation[] extendedAnnotations;

	/**
	 * Constructor for RuntimeInvisibleTypeAnnotations.
	 * @param classFileBytes
	 * @param constantPool
	 * @param offset
	 * @throws ClassFormatException
	 */
	public RuntimeInvisibleTypeAnnotationsAttribute(
			byte[] classFileBytes,
			IConstantPool constantPool,
			int offset)
			throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		// read extended annotations
		final int length = u2At(classFileBytes, 6, offset);
		this.extendedAnnotationsNumber = length;
		if (length != 0) {
			int readOffset = 8;
			this.extendedAnnotations = new IExtendedAnnotation[length];
			for (int i = 0; i < length; i++) {
				ExtendedAnnotation extendedAnnotation = new ExtendedAnnotation(classFileBytes, constantPool, offset + readOffset);
				this.extendedAnnotations[i] = extendedAnnotation;
				readOffset += extendedAnnotation.sizeInBytes();
			}
		} else {
			this.extendedAnnotations = NO_ENTRIES;
		}
	}

	@Override
	public IExtendedAnnotation[] getExtendedAnnotations() {
		return this.extendedAnnotations;
	}

	@Override
	public int getExtendedAnnotationsNumber() {
		return this.extendedAnnotationsNumber;
	}
}