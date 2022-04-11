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

/**
 * Abstract class that defines helpers methods for decoding .class file.
 */
public abstract class ClassFileStruct {

	protected double doubleAt(byte[] reference, int relativeOffset, int structOffset) {
		return (Double.longBitsToDouble(i8At(reference, relativeOffset, structOffset)));
	}

	protected float floatAt(byte[] reference, int relativeOffset, int structOffset) {
		return (Float.intBitsToFloat(i4At(reference, relativeOffset, structOffset)));
	}
	protected int i1At(byte[] reference, int relativeOffset, int structOffset) {
		return reference[relativeOffset + structOffset];
	}
	protected int i2At(byte[] reference, int relativeOffset, int structOffset) {
		int position = relativeOffset + structOffset;
		return (reference[position++] << 8) + (reference[position] & 0xFF);
	}
	protected int i4At(byte[] reference, int relativeOffset, int structOffset) {
		int position = relativeOffset + structOffset;
		return ((reference[position++] & 0xFF) << 24)
			+ ((reference[position++] & 0xFF) << 16)
			+ ((reference[position++] & 0xFF) << 8)
			+ (reference[position] & 0xFF);
	}
	protected long i8At(byte[] reference, int relativeOffset, int structOffset) {
		int position = relativeOffset + structOffset;
		return (((long) (reference[position++] & 0xFF)) << 56)
						+ (((long) (reference[position++] & 0xFF)) << 48)
						+ (((long) (reference[position++] & 0xFF)) << 40)
						+ (((long) (reference[position++] & 0xFF)) << 32)
						+ (((long) (reference[position++] & 0xFF)) << 24)
						+ (((long) (reference[position++] & 0xFF)) << 16)
						+ (((long) (reference[position++] & 0xFF)) << 8)
						+ (reference[position++] & 0xFF);
	}

	protected int u1At(byte[] reference, int relativeOffset, int structOffset) {
		return (reference[relativeOffset + structOffset] & 0xFF);
	}
	protected int u2At(byte[] reference, int relativeOffset, int structOffset) {
		int position = relativeOffset + structOffset;
		return ((reference[position++] & 0xFF) << 8) + (reference[position] & 0xFF);
	}
	protected long u4At(byte[] reference, int relativeOffset, int structOffset) {
		int position = relativeOffset + structOffset;
		return (
			((reference[position++] & 0xFFL) << 24)
				+ ((reference[position++] & 0xFF) << 16)
				+ ((reference[position++] & 0xFF) << 8)
				+ (reference[position] & 0xFF));
	}
	protected char[] utf8At(byte[] reference, int relativeOffset, int structOffset, int bytesAvailable) {
		int length = bytesAvailable;
		char outputBuf[] = new char[bytesAvailable];
		int outputPos = 0;
		int readOffset = structOffset + relativeOffset;

		while (length != 0) {
			int x = reference[readOffset++] & 0xFF;
			length--;
			if ((0x80 & x) != 0) {
				if ((x & 0x20) != 0) {
					length-=2;
					x = ((x & 0xF) << 12) + ((reference[readOffset++] & 0x3F) << 6) + (reference[readOffset++] & 0x3F);
				} else {
					length--;
					x = ((x & 0x1F) << 6) + (reference[readOffset++] & 0x3F);
				}
			}
			outputBuf[outputPos++] = (char) x;
		}

		if (outputPos != bytesAvailable) {
			System.arraycopy(outputBuf, 0, (outputBuf = new char[outputPos]), 0, outputPos);
		}
		return outputBuf;
	}

	final boolean equals(char[] first, char[] second) {
		if (first == second)
			return true;
		if (first == null || second == null)
			return false;
		if (first.length != second.length)
			return false;

		for (int i = first.length; --i >= 0;)
			if (first[i] != second[i])
				return false;
		return true;
	}
}
