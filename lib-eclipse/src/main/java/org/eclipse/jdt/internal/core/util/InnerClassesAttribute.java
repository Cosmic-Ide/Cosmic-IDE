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
import org.eclipse.jdt.core.util.IConstantPool;
import org.eclipse.jdt.core.util.IInnerClassesAttribute;
import org.eclipse.jdt.core.util.IInnerClassesAttributeEntry;

/**
 * Default implementation of IInnerClassesAttribute.
 */
public class InnerClassesAttribute extends ClassFileAttribute implements IInnerClassesAttribute {
	private static final IInnerClassesAttributeEntry[] NO_ENTRIES = new IInnerClassesAttributeEntry[0];

	private int numberOfClasses;
	private IInnerClassesAttributeEntry[] entries;
	/**
	 * Constructor for InnerClassesAttribute.
	 * @param classFileBytes
	 * @param constantPool
	 * @param offset
	 * @throws ClassFormatException
	 */
	public InnerClassesAttribute(
		byte[] classFileBytes,
		IConstantPool constantPool,
		int offset)
		throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		this.numberOfClasses = u2At(classFileBytes, 6, offset);
		final int length = this.numberOfClasses;
		if (length != 0) {
			int readOffset = 8;
			this.entries = new IInnerClassesAttributeEntry[length];
			for (int i = 0; i < length; i++) {
				this.entries[i] = new InnerClassesAttributeEntry(classFileBytes, constantPool, offset + readOffset);
				readOffset += 8;
			}
		} else {
			this.entries = NO_ENTRIES;
		}
	}

	/**
	 * @see IInnerClassesAttribute#getInnerClassAttributesEntries()
	 */
	@Override
	public IInnerClassesAttributeEntry[] getInnerClassAttributesEntries() {
		return this.entries;
	}

	/**
	 * @see IInnerClassesAttribute#getNumberOfClasses()
	 */
	@Override
	public int getNumberOfClasses() {
		return this.numberOfClasses;
	}

}
