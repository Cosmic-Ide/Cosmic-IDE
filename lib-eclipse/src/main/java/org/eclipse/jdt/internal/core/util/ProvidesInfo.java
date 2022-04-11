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
import org.eclipse.jdt.core.util.IProvidesInfo;

public class ProvidesInfo extends ClassFileStruct implements IProvidesInfo {
	private int index;
	private char[] serviceName;
	private int implementationsCount;
	private int[] implementationIndices;
	private char[][] implementationNames;

	public ProvidesInfo(byte[] classFileBytes, IConstantPool constantPool, int offset) throws ClassFormatException {
		int readOffset = 0;
		this.index = u2At(classFileBytes, readOffset, offset);
		readOffset += 2;
		IConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(this.index);
		if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Class) {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		this.serviceName = constantPoolEntry.getClassInfoName();
		this.implementationsCount = u2At(classFileBytes, readOffset, offset);
		readOffset += 2;

		if (this.implementationsCount != 0) {
			this.implementationIndices = new int[this.implementationsCount];
			this.implementationNames = new char[this.implementationsCount][];
			for (int i = 0; i < this.implementationsCount; i++) {
				this.implementationIndices[i] = u2At(classFileBytes, readOffset, offset);
				readOffset += 2;
				constantPoolEntry = constantPool.decodeEntry(this.implementationIndices[i]);
				if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Class) {
					throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
				}
				this.implementationNames[i] = constantPoolEntry.getClassInfoName();
			}
		}
	}
	@Override
	public int getIndex() {
		return this.index;
	}

	@Override
	public char[] getServiceName() {
		return this.serviceName;
	}

	@Override
	public int getImplementationsCount() {
		return this.implementationsCount;
	}

	@Override
	public int[] getImplementationIndices() {
		return this.implementationIndices;
	}

	@Override
	public char[][] getImplementationNames() {
		return this.implementationNames;
	}

}
