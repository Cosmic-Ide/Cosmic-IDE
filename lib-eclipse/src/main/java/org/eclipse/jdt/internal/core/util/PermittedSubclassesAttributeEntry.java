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
import org.eclipse.jdt.core.util.IConstantPoolConstant;
import org.eclipse.jdt.core.util.IConstantPoolEntry;
import org.eclipse.jdt.core.util.IPermittedSubclassesAttributeEntry;

public class PermittedSubclassesAttributeEntry extends ClassFileStruct implements IPermittedSubclassesAttributeEntry {

	private int permittedClassNameIndex;
	private char[] permittedClassName;

	public PermittedSubclassesAttributeEntry(byte[] classFileBytes, IConstantPool constantPool, int offset)
			throws ClassFormatException {
		this.permittedClassNameIndex = u2At(classFileBytes, 0, offset);
		if (this.permittedClassNameIndex != 0) {
			IConstantPoolEntry constantPoolEntry;
			constantPoolEntry = constantPool.decodeEntry(this.permittedClassNameIndex);
			if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Class) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.permittedClassName = constantPoolEntry.getClassInfoName();
		}
	}

	@Override
	public String toString() {
		return new String(this.permittedClassName);
	}

	@Override
	public char[] getPermittedSubclassName() {
		return this.permittedClassName;
	}

	@Override
	public int gePermittedSubclassIndex() {
		return this.permittedClassNameIndex;
	}
}

