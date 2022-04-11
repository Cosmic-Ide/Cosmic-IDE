/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import org.eclipse.jdt.core.util.IConstantPoolConstant;
import org.eclipse.jdt.core.util.IConstantPoolEntry;
import org.eclipse.jdt.core.util.IModuleMainClassAttribute;

/**
 * Default implementation of IModuleMainClassAttribute
 */
public class ModuleMainClassAttribute extends ClassFileAttribute implements IModuleMainClassAttribute {

	private final int mainClassIndex;
	private final char[] mainClassName;

	/**
	 * Constructor for ModuleMainClassAttribute.
	 * @param classFileBytes
	 * @param constantPool
	 * @param offset
	 * @throws ClassFormatException
	 */
	public ModuleMainClassAttribute(	byte[] classFileBytes,	IConstantPool constantPool,	int offset)	throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		int readOffset = 6;
		this.mainClassIndex = u2At(classFileBytes, readOffset, offset);
		IConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(this.mainClassIndex);
		if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Class) {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		this.mainClassName = constantPoolEntry.getClassInfoName();
	}

	@Override
	public int getMainClassIndex() {
		return this.mainClassIndex;
	}

	@Override
	public char[] getMainClassName() {
		return this.mainClassName;
	}
}