/*******************************************************************************
 * Copyright (c) 2021 jkubitz and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     jkubitz - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.builder;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

/** abstraction of DataOutputStream **/
public class CompressedWriter {
	private final DataOutputStream out;
	// --- context state: ---
	private char[] lastName;
	private final HashMap<String, Integer> lastWords;

	public CompressedWriter(DataOutputStream out) {
		this.out = out;
		this.lastName = new char[0];
		this.lastWords = new HashMap<>();
	}

	// ---- direct forwards to DataOutputStream: ---

	/** @see CompressedReader#readBoolean() **/
	public void writeBoolean(boolean v) throws IOException {
		this.out.writeBoolean(v);
	}

	/** @see CompressedReader#readByte() **/
	public void writeByte(int v) throws IOException {
		this.out.writeByte(v);
	}

	/** @see CompressedReader#readLong() **/
	public void writeLong(long v) throws IOException {
		this.out.writeLong(v);
	}

	/** @see CompressedReader#readInt() **/
	public void writeInt(int v) throws IOException {
		this.out.writeInt(v);
	}

	// ---- compressed outputs: ---

	/**
	 * Write the given name UTF8 encoded.
	 *
	 * @param name
	 *            chars to write
	 * @see CompressedReader#readChars()
	 **/
	public void writeChars(char[] name) throws IOException {
		// compress with UTF8 encoding
		// => typically only one byte per char
		this.out.writeUTF(new String(name));
	}

	/**
	 * Writes v using a list of likely values "typical". If v is element of "typical" then encode v as an index.
	 * Otherwise encoded as typical.length and plain encoded v;
	 *
	 * @param v
	 *            the number to write
	 * @param typical
	 *            a list of most used values
	 *
	 * @see CompressedReader#readIntWithHint(int[])
	 **/
	public void writeIntWithHint(int v, int[] typical) throws IOException {
		// => typically only one byte per int
		int i;
		// find index
		for (i = 0; i < typical.length; i++) {
			if (typical[i] == v) {
				break;
			}
		}
		writeIntInRange(i, typical.length + 1); // write index
		if (i == typical.length) {
			writeInt(v); // write plain
		}
	}

	/**
	 * Write a number v which must be 0 <= v <range. Otherwise IllegalArgumentException is thrown. Encodes only as many
	 * bytes as needed. I.e. skipping the high bytes.
	 *
	 * @param v
	 *            the number to write.
	 * @param range
	 *            the maximal possible value of v + 1
	 * @see CompressedReader#readIntInRange(int)
	 **/
	public void writeIntInRange(int v, int range) throws IOException {
		// => typically(0<range<256) only one byte per int
		if (range < 0 || range > 0xFFFF) {
			this.out.writeInt(v);
		} else {
			if (v >= range) {
				throw new IllegalArgumentException(v + "/" + range);//$NON-NLS-1$
			}
			if (v < 0) {
				throw new IllegalArgumentException(v + "/" + range);//$NON-NLS-1$
			}
			if (range <= 0xFF) {
				this.out.writeByte(v);
			} else {
				this.out.writeShort(v);
			}
		}
	}

	/**
	 * Writes the given String. If the String was already written before its index in the dictionary is encoded as
	 * index+1. Otherwise a 0 index is encoded and then the String is encoded as UTF8 and stored into dictionary.
	 *
	 * @param v
	 *            the String to write
	 * @see CompressedReader#readStringUsingDictionary()
	 **/
	public void writeStringUsingDictionary(String v) throws IOException {
		// => typically(less then 256 Strings) only one byte per String
		int size = this.lastWords.size();
		Integer index = this.lastWords.putIfAbsent(v, size);
		if (index == null) {
			// using 0 as magic number (instead of size) to get a low entropy output.
			writeIntInRange(0, size + 1); // write magic 0, which is skipped in the other case
			this.out.writeUTF(v);
		} else {
			writeIntInRange(index.intValue() + 1, size + 1); // write index+1 to skip magic 0
		}
	}

	/**
	 * Writes the given String. Encoded like a char array using last string.
	 *
	 * @param name
	 *            the String to write.
	 * @see CompressedReader#readStringUsingLast()
	 **/
	public void writeStringUsingLast(String name) throws IOException {
		// convenience Method for Strings
		writeCharsUsingLast(name.toCharArray());
	}

	/**
	 * Writes the given char array and remember it. Encoded by writing the length of common leading chars with the last
	 * given array followed by the remaining chars using a dictionary. Works best if the names are written in binary
	 * sorted order.
	 *
	 * @param name
	 *            the char array to write
	 * @see CompressedReader#readCharsUsingLast()
	 **/
	public void writeCharsUsingLast(char[] name) throws IOException {
		// compress by assuming the last name started with the same characters
		// => typically(qualified name from same package) only two bytes per package + unqualified name from dictionary
		int commonLength = commonLength(name, this.lastName, 255);
		this.out.writeByte(commonLength);
		writeStringUsingDictionary(new String(name, commonLength, name.length - commonLength));
		this.lastName = name;
	}

	private static int commonLength(char[] a, char[] b, int max) {
		int i = 0;
		int end = Math.min(Math.min(max, a.length), b.length);
		while (i < end && a[i] == b[i]) {
			i++;
		}
		return i;
	}
}