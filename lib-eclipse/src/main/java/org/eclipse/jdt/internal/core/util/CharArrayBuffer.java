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

import org.eclipse.jdt.internal.compiler.util.Util;

/**
 * The <code>CharArrayBuffer</code> is intended as a lightweight partial implementation
 * of the StringBuffer class, but using <code>char[]'s</code> instead of Strings.
 *
 * <p>The <code>CharArrayBuffer</code> maintains a list of <code>char[]'s</code>
 * which don't get appended until the user asks for them.  The following
 * code illustrates how to use the class.
 *
 * <code>
 * CharArrayBuffer buffer = new CharArrayBuffer(myCharArray);
 * buffer.append(moreBytes, 0, someLength);
 * myCharArray = buffer.getContents();
 * </code>
 *
 * <p>NOTE: This class is not Thread safe!
 */
public class CharArrayBuffer {
	/**
	 * This is the buffer of char arrays which must be appended together
	 * during the getContents method.
	 */
	protected char[][] buffer;

	/**
	 * The default buffer size.
	 */
	public static final int DEFAULT_BUFFER_SIZE = 10;

	/**
	 * The end of the buffer
	 */
	protected int end;

	/**
	 * The current size of the buffer.
	 */
	protected int size;

	/**
	 * A buffer of ranges which is maintained along with
	 * the buffer.  Ranges are of the form {start, length}.
	 * Enables append(char[] array, int start, int end).
	 */
	protected int[][] ranges;
/**
 * Creates a <code>CharArrayBuffer</code> with the default buffer size (10).
 */
public CharArrayBuffer() {
	this(null, DEFAULT_BUFFER_SIZE);
}
/**
 * Creates a <code>CharArrayBuffer</code> with the default buffer size,
 * and sets the first element in the buffer to be the given char[].
 *
 * @param first - the first element to be placed in the buffer, ignored if null
 */
public CharArrayBuffer(char[] first) {
	this(first, DEFAULT_BUFFER_SIZE);
}
/**
 * Creates a <code>CharArrayBuffer</code> with the given buffer size,
 * and sets the first element in the buffer to be the given char array.
 *
 * @param first - the first element of the buffer, ignored if null.
 * @param size - the buffer size, if less than 1, set to the DEFAULT_BUFFER_SIZE.
 */
public CharArrayBuffer(char[] first, int size) {
	this.size = (size > 0) ? size : DEFAULT_BUFFER_SIZE;
	this.buffer = new char[this.size][];
	this.ranges = new int[this.size][];
	this.end = 0;
	if (first != null)
		append(first, 0, first.length);
}
/**
 * Creates a <code>CharArrayBuffer</code> with the given buffer size.
 *
 * @param size - the size of the buffer.
 */
public CharArrayBuffer(int size) {
	this(null, size);
}
/**
 * Appends the entire given char array.  Given for convenience.
 *
 * @param src - a char array which is appended to the end of the buffer.
 */
public CharArrayBuffer append(char[] src) {
	if (src != null)
		append(src, 0, src.length);
	return this;
}
/**
 * Appends a sub array of the given array to the buffer.
 *
 * @param src - the next array of characters to be appended to the buffer, ignored if null
 * @param start - the start index in the src array.
 * @param length - the number of characters from start to be appended
 *
 * @throws ArrayIndexOutOfBoundsException - if arguments specify an array index out of bounds.
 */
public CharArrayBuffer append(char[] src, int start, int length) {
	if (start < 0) throw new ArrayIndexOutOfBoundsException();
	if (length < 0) throw new ArrayIndexOutOfBoundsException();
	if (src != null) {
		int srcLength = src.length;
		if (start > srcLength) throw new ArrayIndexOutOfBoundsException();
		if (length + start > srcLength) throw new ArrayIndexOutOfBoundsException();
		/** do length check here to allow exceptions to be thrown */
		if (length > 0) {
			if (this.end == this.size) {
				int size2 = this.size * 2;
				System.arraycopy(this.buffer, 0, (this.buffer = new char[size2][]), 0, this.size);
				System.arraycopy(this.ranges, 0, (this.ranges = new int[size2][]), 0, this.size);
				this.size *= 2;
			}
			this.buffer[this.end] = src;
			this.ranges[this.end] = new int[] {start, length};
			this.end++;
		}
	}
	return this;
}
/**
 * Appends the given char.  Given for convenience.
 *
 * @param c - a char which is appended to the end of the buffer.
 */
public CharArrayBuffer append(char c) {
	append(new char[] {c}, 0, 1);
	return this;
}
/**
 * Appends the given String to the buffer.  Given for convenience, use
 * #append(char[]) if possible
 *
 * @param src - a char array which is appended to the end of the buffer.
 */
public CharArrayBuffer append(String src) {
	if (src != null)
		append(src.toCharArray(), 0, src.length());
	return this;
}
/**
 * Returns the entire contents of the buffer as one
 * char[] or null if nothing has been put in the buffer.
 */
public char[] getContents() {
	if (this.end == 0)
		return null;

	// determine the length of the array
	int length = 0;
	for (int i = 0; i < this.end; i++)
		length += this.ranges[i][1];

	if (length > 0) {
		char[] result = new char[length];
		int current = 0;
		// copy the results
		for(int i = 0; i < this.end; i++) {
			int[] range = this.ranges[i];
			int length2 = range[1];
			System.arraycopy(this.buffer[i], range[0], result, current, length2);
			current += length2;
		}
		return result;
	}
	return null;
}
/**
 * Returns the contents of the buffer as a String, or
 * an empty string if the buffer is empty.
 */
@Override
public String toString() {
	char[] contents = getContents();
	return (contents != null) ? new String(contents) : Util.EMPTY_STRING;
}
}
