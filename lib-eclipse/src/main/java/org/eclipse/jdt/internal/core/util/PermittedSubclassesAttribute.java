/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
import org.eclipse.jdt.core.util.IPermittedSubclassesAttributeEntry;
import org.eclipse.jdt.core.util.IPermittedSubclassesAttribute;

/**
 * Default implementation of IPermittedSubclassesAttribute.
 */
public class PermittedSubclassesAttribute extends ClassFileAttribute implements IPermittedSubclassesAttribute {
	private static final IPermittedSubclassesAttributeEntry[] NO_ENTRIES = new IPermittedSubclassesAttributeEntry[0];

	private int permittedSubclasses;
	private IPermittedSubclassesAttributeEntry[] entries;

	/**
	 * Constructor for PermittedSubclassesAttribute.
	 * @param classFileBytes
	 * @param constantPool
	 * @param offset
	 * @throws ClassFormatException
	 */
	public PermittedSubclassesAttribute(
		byte[] classFileBytes,
		IConstantPool constantPool,
		int offset)
		throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		this.permittedSubclasses = u2At(classFileBytes, 6, offset);
		final int length = this.permittedSubclasses;
		if (length != 0) {
			int readOffset = 8;
			this.entries = new IPermittedSubclassesAttributeEntry[length];
			for (int i = 0; i < length; i++) {
				this.entries[i] = new PermittedSubclassesAttributeEntry(classFileBytes, constantPool, offset + readOffset);
				readOffset += 2;
			}
		} else {
			this.entries = NO_ENTRIES;
		}
	}

	@Override
	public int getNumberOfPermittedSubclasses() {
		return this.permittedSubclasses;
	}

	@Override
	public IPermittedSubclassesAttributeEntry[] getPermittedSubclassAttributesEntries() {
		return this.entries;
	}

}
