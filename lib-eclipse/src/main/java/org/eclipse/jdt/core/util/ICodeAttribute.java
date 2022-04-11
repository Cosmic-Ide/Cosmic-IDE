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
 * Description of a code attribute as described in the JVM specifications.
 *
 * This interface may be implemented by clients.
 *
 * @since 2.0
 */
public interface ICodeAttribute extends IClassFileAttribute {
	/**
	 * Answer back the max locals value of the code attribute.
	 *
	 * @return the max locals value of the code attribute
	 */
	int getMaxLocals();

	/**
	 * Answer back the max stack value of the code attribute.
	 *
	 * @return the max stack value of the code attribute
	 */
	int getMaxStack();

	/**
	 * Answer back the line number attribute, if it exists, null otherwise.
	 *
	 * @return the line number attribute, if it exists, null otherwise
	 */
	ILineNumberAttribute getLineNumberAttribute();

	/**
	 * Answer back the local variable attribute, if it exists, null otherwise.
	 *
	 * @return the local variable attribute, if it exists, null otherwise
	 */
	ILocalVariableAttribute getLocalVariableAttribute();

	/**
	 * Answer back the array of exception entries, if they are present.
	 * An empty array otherwise.
	 *
	 * @return the array of exception entries, if they are present.
	 * An empty array otherwise
	 */
	IExceptionTableEntry[] getExceptionTable();

	/**
	 * Answer back the array of bytes, which represents all the opcodes as described
	 * in the JVM specifications.
	 *
	 * @return the array of bytes, which represents all the opcodes as described
	 * in the JVM specifications
	 */
	byte[] getBytecodes();

	/**
	 * Answer back the length of the bytecode contents.
	 *
	 * @return the length of the bytecode contents
	 */
	long getCodeLength();

	/**
	 * Answer back the attribute number of the code attribute.
	 *
	 * @return the attribute number of the code attribute
	 */
	int getAttributesCount();

	/**
	 * Answer back the collection of all attributes of the field info. It
	 * includes the LineNumberAttribute and the LocalVariableTableAttribute.
	 * Returns an empty collection if none.
	 *
	 * @return the collection of all attributes of the field info. It
	 * includes the LineNumberAttribute and the LocalVariableTableAttribute.
	 * Returns an empty collection if none
	 */
	IClassFileAttribute[] getAttributes();

	/**
	 * Answer back the exception table length of the code attribute.
	 *
	 * @return the exception table length of the code attribute
	 */
	int getExceptionTableLength();

	/**
	 * Define a Java opcodes walker. All actions are defined in the visitor.
	 * @param visitor The visitor to use to walk the opcodes.
	 *
	 * @exception ClassFormatException Exception thrown if the opcodes contain invalid bytes
	 */
	void traverse(IBytecodeVisitor visitor) throws ClassFormatException;
}
