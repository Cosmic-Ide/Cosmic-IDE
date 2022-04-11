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
package org.eclipse.jdt.core.util;

/**
 * Exception thrown by a class file reader when encountering a error in decoding
 * information contained in a .class file.
 *
 * @since 2.0
 */
public class ClassFormatException extends Exception {

	public static final int ERROR_MALFORMED_UTF8 = 1;
	public static final int ERROR_TRUNCATED_INPUT = 2;
	public static final int INVALID_CONSTANT_POOL_ENTRY = 3;
	public static final int TOO_MANY_BYTES = 4;
	public static final int INVALID_ARGUMENTS_FOR_INVOKEINTERFACE = 5;
	public static final int INVALID_BYTECODE = 6;

	/**
	 * @since 3.0
	 */
	public static final int INVALID_TAG_CONSTANT = 7;

	/**
	 * @since 3.0
	 */
	public static final int INVALID_MAGIC_NUMBER = 8;

	private static final long serialVersionUID = 6582900558320612988L; // backward compatible

	/**
	 * Constructor for ClassFormatException.
	 * @param errorID the given error ID
	 */
	public ClassFormatException(int errorID) {
		// TODO (olivier) what is the errorID?
	}

	/**
	 * Constructor for ClassFormatException.
	 * @param message the message for the exception
	 */
	public ClassFormatException(String message) {
		super(message);
	}

	/**
	 * Constructor for ClassFormatException.
	 * @param message the message for the exception
	 * @param  cause  the cause of the exception
	 * @since 3.5
	 */
	public ClassFormatException(String message, Throwable cause) {
		super(message, cause);
	}
}
