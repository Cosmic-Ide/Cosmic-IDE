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
import org.eclipse.jdt.core.util.IPackageVisibilityInfo;

public class PackageVisibilityInfo extends ClassFileStruct implements IPackageVisibilityInfo {

	private int index;
	private char[] packageName;
	private int flags;
	private int targetsCount;
	private int[] targetModuleIndices;
	private char[][] targetModuleNames;

	public PackageVisibilityInfo(byte[] classFileBytes, IConstantPool constantPool, int offset) throws ClassFormatException {
		int readOffset = 0;
		this.index = u2At(classFileBytes, readOffset, offset);
		readOffset += 2;
		IConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(this.index);
		if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Package) {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		char[] tmp = ((IConstantPoolEntry3) constantPoolEntry).getPackageName();
		this.packageName = tmp != null ? tmp : CharOperation.NO_CHAR;

		this.flags = u2At(classFileBytes, readOffset, offset);
		readOffset += 2;
		this.targetsCount = u2At(classFileBytes, readOffset, offset);
		readOffset += 2;

		if (this.targetsCount != 0) {
			this.targetModuleIndices = new int[this.targetsCount];
			this.targetModuleNames = new char[this.targetsCount][];
			for (int i = 0; i < this.targetsCount; i++) {
				this.targetModuleIndices[i] = u2At(classFileBytes, readOffset, offset);
				readOffset += 2;
				constantPoolEntry = constantPool.decodeEntry(this.targetModuleIndices[i]);
				if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Module) {
					throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
				}
				tmp = ((IConstantPoolEntry3) constantPoolEntry).getModuleName();
				this.targetModuleNames[i] = tmp != null ? tmp : CharOperation.NO_CHAR;
			}
		} else {
			this.targetModuleIndices = new int[0];
			this.targetModuleNames = CharOperation.NO_CHAR_CHAR;
		}
	}
	@Override
	public int getIndex() {
		return this.index;
	}

	@Override
	public char[] getPackageName() {
		return this.packageName;
	}

	@Override
	public int getFlags() {
		return this.flags;
	}

	@Override
	public int getTargetsCount() {
		return this.targetsCount;
	}

	@Override
	public int[] getTargetModuleIndices() {
		return this.targetModuleIndices;
	}

	@Override
	public char[][] getTargetModuleNames() {
		return this.targetModuleNames;
	}
}
