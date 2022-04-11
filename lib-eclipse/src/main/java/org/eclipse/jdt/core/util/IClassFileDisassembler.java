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
 * This interface is intended to be implemented to disassemble
 * IClassFileReader onto a String using the proper line separator.
 *
 * @since 2.0
 * @deprecated Use {@link ClassFileBytesDisassembler} instead
 */
public interface IClassFileDisassembler {

	/**
	 * The mode is the detailed mode to disassemble IClassFileReader. It returns the magic
	 * numbers, the version numbers and field and method descriptors.
	 */
	int DETAILED = 1;

	/**
	 * The mode is the default mode to disassemble IClassFileReader.
	 */
	int DEFAULT  = 2;
	/**
	 * Answers back the disassembled string of the IClassFileReader using the default
	 * mode.
	 * This is an output quite similar to the javap tool, using DEFAULT mode.
	 *
	 * @param classFileReader The classFileReader to be disassembled
	 * @param lineSeparator the line separator to use.
	 *
	 * @return the disassembled string of the IClassFileReader using the default mode.
	 */
	String disassemble(IClassFileReader classFileReader, String lineSeparator);

	/**
	 * Answers back the disassembled string of the IClassFileReader according to the
	 * mode.
	 * This is an output quite similar to the javap tool.
	 *
	 * @param classFileReader The classFileReader to be disassembled
	 * @param lineSeparator the line separator to use.
	 * @param mode the mode used to disassemble the IClassFileReader
	 *
	 * @return the disassembled string of the IClassFileReader according to the mode
	 */
	String disassemble(IClassFileReader classFileReader, String lineSeparator, int mode);
}
