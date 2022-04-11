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
import org.eclipse.jdt.core.util.INestHostAttribute;

public class NestHostAttribute extends ClassFileAttribute implements INestHostAttribute {

	private int hostIndex;
	private char[] hostName;

	public NestHostAttribute(byte[] classFileBytes, IConstantPool constantPool, int offset)
			throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		int index = u2At(classFileBytes, 6, offset);
		this.hostIndex = index;
		IConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(index);
		if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Class) {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		this.hostName = constantPoolEntry.getClassInfoName();
	}

	@Override
	public char[] getNestHostName() {
		return this.hostName;
	}

	@Override
	public int getNestHostIndex() {
		return this.hostIndex;
	}
	@Override
	public String toString() {
		return new String(this.hostName);
	}
}