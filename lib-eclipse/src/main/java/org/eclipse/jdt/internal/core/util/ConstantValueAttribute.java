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
import org.eclipse.jdt.core.util.IConstantPoolEntry;
import org.eclipse.jdt.core.util.IConstantValueAttribute;

/**
 * Default implementation of IConstantValueAttribute.
 */
public class ConstantValueAttribute
	extends ClassFileAttribute
	implements IConstantValueAttribute {

	private int constantValueIndex;
	private IConstantPoolEntry constantPoolEntry;


	ConstantValueAttribute(byte[] classFileBytes, IConstantPool constantPool, int offset) throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		this.constantValueIndex = u2At(classFileBytes, 6, offset);
		this.constantPoolEntry = constantPool.decodeEntry(this.constantValueIndex);
	}
	/**
	 * @see IConstantValueAttribute#getConstantValue()
	 */
	@Override
	public IConstantPoolEntry getConstantValue() {
		return this.constantPoolEntry;
	}

	/**
	 * @see IConstantValueAttribute#getConstantValueIndex()
	 */
	@Override
	public int getConstantValueIndex() {
		return this.constantValueIndex;
	}
}
