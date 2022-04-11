/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
import org.eclipse.jdt.core.util.ISourceAttribute;

/**
 * Default implementation of ISourceAttribute
 */
public class SourceFileAttribute
	extends ClassFileAttribute
	implements ISourceAttribute {

	private int sourceFileIndex;
	private char[] sourceFileName;

	/**
	 * Constructor for SourceFileAttribute.
	 * @param classFileBytes
	 * @param constantPool
	 * @param offset
	 * @throws ClassFormatException
	 */
	public SourceFileAttribute(
		byte[] classFileBytes,
		IConstantPool constantPool,
		int offset)
		throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		this.sourceFileIndex = u2At(classFileBytes, 6, offset);
		IConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(this.sourceFileIndex);
		if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Utf8) {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		this.sourceFileName = constantPoolEntry.getUtf8Value();
	}
	/**
	 * @see ISourceAttribute#getSourceFileIndex()
	 */
	@Override
	public int getSourceFileIndex() {
		return this.sourceFileIndex;
	}

	/**
	 * @see ISourceAttribute#getSourceFileName()
	 */
	@Override
	public char[] getSourceFileName() {
		return this.sourceFileName;
	}

}
