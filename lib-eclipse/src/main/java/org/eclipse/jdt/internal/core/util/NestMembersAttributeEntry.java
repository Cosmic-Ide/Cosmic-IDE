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
import org.eclipse.jdt.core.util.IConstantPoolConstant;
import org.eclipse.jdt.core.util.IConstantPoolEntry;
import org.eclipse.jdt.core.util.INestMemberAttributeEntry;

public class NestMembersAttributeEntry extends ClassFileStruct implements INestMemberAttributeEntry {

	private int memberClassNameIndex;
	private char[] memberClassName;

	public NestMembersAttributeEntry(byte[] classFileBytes, IConstantPool constantPool, int offset)
			throws ClassFormatException {
		this.memberClassNameIndex = u2At(classFileBytes, 0, offset);
		if (this.memberClassNameIndex != 0) {
			IConstantPoolEntry constantPoolEntry;
			constantPoolEntry = constantPool.decodeEntry(this.memberClassNameIndex);
			if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Class) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.memberClassName = constantPoolEntry.getClassInfoName();
		}
	}

	@Override
	public char[] getNestMemberName() {
		return this.memberClassName;
	}

	@Override
	public int getNestMemberIndex() {
		return this.memberClassNameIndex;
	}

	@Override
	public String toString() {
		return new String(this.memberClassName);
	}
}

