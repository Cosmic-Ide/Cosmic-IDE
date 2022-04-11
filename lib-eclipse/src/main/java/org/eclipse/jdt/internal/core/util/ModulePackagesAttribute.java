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

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.util.ClassFormatException;
import org.eclipse.jdt.core.util.IConstantPool;
import org.eclipse.jdt.core.util.IConstantPoolConstant;
import org.eclipse.jdt.core.util.IConstantPoolEntry;
import org.eclipse.jdt.core.util.IConstantPoolEntry3;
import org.eclipse.jdt.core.util.IModulePackagesAttribute;

/**
 * Default implementation of IModulePackagesAttribute
 */
public class ModulePackagesAttribute extends ClassFileAttribute implements IModulePackagesAttribute {

	private int packagesCount;
	private int[] packageIndices;
	private char[][] packageNames;

	/**
	 * Constructor for ModulePackagesAttribute.
	 * @param classFileBytes
	 * @param constantPool
	 * @param offset
	 * @throws ClassFormatException
	 */
	public ModulePackagesAttribute(	byte[] classFileBytes,	IConstantPool constantPool,	int offset)	throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		int readOffset = 6;
		final int length = u2At(classFileBytes, readOffset, offset);
		readOffset += 2;
		this.packagesCount = length;
		if (length != 0) {
			this.packageIndices = new int[length];
			this.packageNames = new char[length][0];
			for (int i = 0; i < length; i++) {
				this.packageIndices[i] = u2At(classFileBytes, readOffset, offset);
				readOffset += 2;
				IConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(this.packageIndices[i]);
				if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Package) {
					throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
				}
				char[] name = ((IConstantPoolEntry3) constantPoolEntry).getPackageName();
				this.packageNames[i] = name != null ? name : CharOperation.NO_CHAR;

			}
		} else {
			this.packageNames = CharOperation.NO_CHAR_CHAR;
		}
	}

	@Override
	public int getPackagesCount() {
		return this.packagesCount;
	}

	@Override
	public int[] getPackageIndices() {
		return this.packageIndices;
	}

	@Override
	public char[][] getPackageNames() {
		return this.packageNames;
	}
}