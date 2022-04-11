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
package org.eclipse.jdt.internal.core.util;

import org.eclipse.jdt.core.util.ClassFormatException;
import org.eclipse.jdt.core.util.IAnnotationComponentValue;
import org.eclipse.jdt.core.util.IAnnotationDefaultAttribute;
import org.eclipse.jdt.core.util.IConstantPool;

/**
 * Default implementation of AnnotationDefaultAttribute.
 *
 * @since 3.0
 */
public class AnnotationDefaultAttribute extends ClassFileAttribute
		implements
			IAnnotationDefaultAttribute {

	private IAnnotationComponentValue memberValue;

	/**
	 * Constructor for AnnotationDefaultAttribute.
	 * @param classFileBytes
	 * @param constantPool
	 * @param offset
	 * @throws ClassFormatException
	 */
	public AnnotationDefaultAttribute(
			byte[] classFileBytes,
			IConstantPool constantPool,
			int offset)
			throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		this.memberValue = new AnnotationComponentValue(classFileBytes, constantPool, offset + 6);
	}

	@Override
	public IAnnotationComponentValue getMemberValue() {
		return this.memberValue;
	}
}
