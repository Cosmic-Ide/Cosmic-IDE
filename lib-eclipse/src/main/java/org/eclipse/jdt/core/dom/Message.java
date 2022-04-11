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

package org.eclipse.jdt.core.dom;

/**
 * Error message used to report potential errors found during the AST parsing
 * or name resolution. Instances of this class are immutable.
 *
 * @since 2.0
 */
public class Message {

	/**
	 * The message.
	 */
	private String message;

	/**
	 * The character index into the original source string, or -1 if none.
	 */
	private int startPosition;

	/**
	 * The length in characters of the original source file indicating
	 * where the source fragment corresponding to this message ends.
	 */
	private int length;

	/**
	 * Creates a message.
	 *
	 * @param message the localized message reported by the compiler
	 * @param startPosition the 0-based character index into the
	 *    original source file, or <code>-1</code> if no source position
	 *    information is to be recorded for this message
	 * @throws IllegalArgumentException if the message is null
	 * @throws IllegalArgumentException if the startPosition is lower than -1.
	 */
	public Message(String message, int startPosition) {
		if (message == null) {
			throw new IllegalArgumentException();
		}
		if (startPosition < -1) {
			throw new IllegalArgumentException();
		}
		this.message = message;
		this.startPosition = startPosition;
		this.length = 0;
	}

	/**
	 * Creates a message.
	 *
	 * @param message the localized message reported by the compiler
	 * @param startPosition the 0-based character index into the
	 *    original source file, or <code>-1</code> if no source position
	 *    information is to be recorded for this message
	 * @param length the length in character of the original source file indicating
	 * 	  where the source fragment corresponding to this message ends. 0 or a negative number
	 *    if none. A negative number will be converted to a 0-length.
	 * @throws IllegalArgumentException if the message is null
	 * @throws IllegalArgumentException if the startPosition is lower than -1.
	 */
	public Message(String message, int startPosition, int length) {
		if (message == null) {
			throw new IllegalArgumentException();
		}
		if (startPosition < -1) {
			throw new IllegalArgumentException();
		}
		this.message = message;
		this.startPosition = startPosition;
		if (length <= 0) {
			this.length = 0;
		} else {
			this.length = length;
		}
	}

	/**
	 * Returns the localized message.
	 *
	 * @return the localized message
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * Returns the character index into the original source file.
	 *
	 * @return the 0-based character index, or <code>-1</code>
	 *    if no source position information is recorded for this
	 *    message
	 * @deprecated Use {@link #getStartPosition()} instead.
	 * @see #getLength()
	 */
	public int getSourcePosition() {
		return getStartPosition();
	}

	/**
	 * Returns the character index into the original source file.
	 *
	 * @return the 0-based character index, or <code>-1</code>
	 *    if no source position information is recorded for this
	 *    message
	 * @see #getLength()
	 */
	public int getStartPosition() {
		return this.startPosition;
	}

	/**
	 * Returns the length in characters of the original source file indicating
	 * where the source fragment corresponding to this message ends.
	 *
	 * @return a length, or <code>0</code>
	 *    if no source length information is recorded for this message
	 * @see #getStartPosition()
	 */
	public int getLength() {
		return this.length;
	}
}
