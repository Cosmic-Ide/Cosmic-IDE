/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.util;

import org.eclipse.jdt.core.util.ClassFormatException;
import org.eclipse.jdt.core.util.IConstantPool;
import org.eclipse.jdt.core.util.INestMemberAttributeEntry;
import org.eclipse.jdt.core.util.INestMembersAttribute;

/**
 * Default implementation of INestMembersAttribute.
 */
public class NestMembersAttribute extends ClassFileAttribute implements INestMembersAttribute {
	private static final INestMemberAttributeEntry[] NO_ENTRIES = new INestMemberAttributeEntry[0];

	private int nestMembers;
	private INestMemberAttributeEntry[] entries;

	/**
	 * Constructor for NestMembersAttribute.
	 * @param classFileBytes
	 * @param constantPool
	 * @param offset
	 * @throws ClassFormatException
	 */
	public NestMembersAttribute(
		byte[] classFileBytes,
		IConstantPool constantPool,
		int offset)
		throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		this.nestMembers = u2At(classFileBytes, 6, offset);
		final int length = this.nestMembers;
		if (length != 0) {
			int readOffset = 8;
			this.entries = new INestMemberAttributeEntry[length];
			for (int i = 0; i < length; i++) {
				this.entries[i] = new NestMembersAttributeEntry(classFileBytes, constantPool, offset + readOffset);
				readOffset += 2;
			}
		} else {
			this.entries = NO_ENTRIES;
		}
	}

	@Override
	public int getNumberOfNestMembers() {
		return this.nestMembers;
	}

	@Override
	public INestMemberAttributeEntry[] getNestMemberAttributesEntries() {
		return this.entries;
	}

}
