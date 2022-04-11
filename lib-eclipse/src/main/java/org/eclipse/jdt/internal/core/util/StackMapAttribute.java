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
import org.eclipse.jdt.core.util.IStackMapFrame;
import org.eclipse.jdt.core.util.IStackMapAttribute;
/**
 * Default implementation of IStackMapAttribute.
 * @see IStackMapAttribute
 */
public class StackMapAttribute
	extends ClassFileAttribute
	implements IStackMapAttribute {

	private static final IStackMapFrame[] NO_FRAMES = new IStackMapFrame[0];
	private static final byte[] NO_ENTRIES = new byte[0];

	private int numberOfEntries;
	private IStackMapFrame[] frames;

	private byte[] bytes;

	/**
	 * Constructor for LineNumberAttribute.
	 * @param classFileBytes
	 * @param constantPool
	 * @param offset
	 * @throws ClassFormatException
	 */
	public StackMapAttribute(
			byte[] classFileBytes,
			IConstantPool constantPool,
			int offset)
			throws ClassFormatException {
		super(classFileBytes, constantPool, offset);

		final int length = u2At(classFileBytes, 6, offset);
		this.numberOfEntries = length;
		if (length != 0) {
			int readOffset = 8;
			this.frames = new IStackMapFrame[length];
			for (int i = 0; i < length; i++) {
				DefaultStackMapFrame frame = new DefaultStackMapFrame(classFileBytes, constantPool, offset + readOffset);
				this.frames[i] = frame;
				readOffset += frame.sizeInBytes();
			}
		} else {
			this.frames = NO_FRAMES;
		}
		final int byteLength = (int) u4At(classFileBytes, 2, offset);

		if (length != 0) {
			System.arraycopy(classFileBytes, offset + 6, this.bytes = new byte[byteLength], 0, byteLength);
		} else {
			this.bytes = NO_ENTRIES;
		}
	}

	@Override
	public int getNumberOfEntries() {
		return this.numberOfEntries;
	}

	@Override
	public IStackMapFrame[] getStackMapFrame() {
		return this.frames;
	}

	/**
	 */
	public byte[] getBytes() {
		return this.bytes;
	}
}
