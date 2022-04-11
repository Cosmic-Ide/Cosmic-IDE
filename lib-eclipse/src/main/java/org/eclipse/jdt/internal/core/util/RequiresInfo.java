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
import org.eclipse.jdt.core.util.IRequiresInfo;

public class RequiresInfo extends ClassFileStruct implements IRequiresInfo {

	private int index;
	private char[] moduleName;
	private int flags;
	private int versionIndex;
	private char[] versionName;

	public RequiresInfo(byte[] classFileBytes, IConstantPool constantPool, int offset) throws ClassFormatException{
		int readOffset = 0;
		this.index = u2At(classFileBytes, readOffset, offset);
		readOffset += 2;
		IConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(this.index);
		if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Module) {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		this.moduleName = ((IConstantPoolEntry3) constantPoolEntry).getModuleName();
		this.flags = u2At(classFileBytes, readOffset, offset);
		readOffset += 2;
		this.versionIndex = u2At(classFileBytes, readOffset, offset);
		readOffset += 2;
		if (this.versionIndex != 0) {
			constantPoolEntry = constantPool.decodeEntry(this.versionIndex);
			if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Utf8) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.versionName = constantPoolEntry.getUtf8Value();
		} else {
			this.versionName = CharOperation.NO_CHAR;
		}
	}
	@Override
	public int getRequiresIndex() {
		return this.index;
	}

	@Override
	public char[] getRequiresModuleName() {
		return this.moduleName;
	}

	@Override
	public int getRequiresFlags() {
		return this.flags;
	}

	@Override
	public int getRequiresVersionIndex() {
		return this.versionIndex;
	}

	@Override
	public char[] getRequiresVersionValue() {
		return this.versionName;
	}

}
