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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

/** abstraction of DataInputStream **/
public class CompressedReader {
	private final DataInputStream in;
	// --- context state: ---
	private char[] lastName;
	/** all Strings inside are distinct => it is smaller then the file size **/
	private final ArrayList<String> lastWords;

	public CompressedReader(DataInputStream in) {
		this.in = in;
		this.lastName = new char[0];
		this.lastWords = new ArrayList<>();
	}

	/** @see CompressedWriter#writeBoolean(boolean) **/
	public boolean readBoolean() throws IOException {
		return this.in.readBoolean();
	}

	/** @see CompressedWriter#writeByte(int) **/
	public byte readByte() throws IOException {
		return this.in.readByte();
	}

	/** @see CompressedWriter#writeLong(long) **/
	public long readLong() throws IOException {
		return this.in.readLong();
	}

	/** @see CompressedWriter#writeInt(int) **/
	public int readInt() throws IOException {
		return this.in.readInt();
	}

	/** @see CompressedWriter#writeChars(char[]) **/
	public char[] readChars() throws IOException {
		return this.in.readUTF().toCharArray();
	}

	/** @see CompressedWriter#writeStringUsingLast(String) **/
	public String readStringUsingLast() throws IOException {
		return new String(readCharsUsingLast());
	}

	/** @see CompressedWriter#writeStringUsingDictionary(String) **/
	public String readStringUsingDictionary() throws IOException {
		int size = this.lastWords.size();
		int index = readIntInRange(size + 1);
		String v;
		if (index == 0) { // magic 0?
			v = this.in.readUTF();
			this.lastWords.add(v);
		} else {
			v = this.lastWords.get(index - 1); // minus one to skip magic 0
		}
		return v;
	}

	/**
	 * Reads an integer number by decoding its index into the given "typical" Array. If the index is out of bounds
	 * decode the plain Integer.
	 *
	 * @param typical
	 *            The same range that has been used during writing.
	 * @see CompressedWriter#writeIntWithHint(int, int[])
	 **/
	public int readIntWithHint(int[] typical) throws IOException {
		int i = readIntInRange(typical.length + 1);
		if (i == typical.length) {
			return readInt();
		} else {
			return typical[i];
		}
	}

	/**
	 * Reads an integer number by only reading its low bytes. The number of bytes written depends on the given range.
	 *
	 * @param range
	 *            The same range that has been used during writing.
	 * @see CompressedWriter#writeIntInRange(int, int)
	 **/
	public int readIntInRange(int range) throws IOException {
		if (range < 0 || range > 0xFFFF) {
			return this.in.readInt();
		} else {
			if (range <= 0xFF) {
				return Byte.toUnsignedInt(this.in.readByte());
			} else {
				return Short.toUnsignedInt(this.in.readShort());
			}
		}
	}

	/**
	 * Reads an array of chars by using this first common chars of the last result. The number of common chars is
	 * decoded from a single byte. The remaining chars are read using the dictionary.
	 *
	 * @see CompressedWriter#writeCharsUsingLast(char[])
	 **/
	public char[] readCharsUsingLast() throws IOException {
		int commonLength = this.in.readByte() & 0xFF;
		char[] suffix = readStringUsingDictionary().toCharArray();
		char[] name = new char[commonLength + suffix.length];
		System.arraycopy(this.lastName, 0, name, 0, commonLength);
		System.arraycopy(suffix, 0, name, commonLength, suffix.length);
		this.lastName = name;
		return name;
	}

}