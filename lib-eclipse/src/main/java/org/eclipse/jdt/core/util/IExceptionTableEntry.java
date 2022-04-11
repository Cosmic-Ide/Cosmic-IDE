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
 * The class represents an entry in the exception table of a ICodeAttribute as
 * specified in the JVM specifications.
 *
 * This interface may be implemented by clients.
 *
 * @since 2.0
 */
public interface IExceptionTableEntry {

	/**
	 * Answer back the start pc of this entry.
	 *
	 * @return the start pc of this entry
	 */
	int getStartPC();

	/**
	 * Answer back the end pc of this entry.
	 *
	 * @return the end pc of this entry
	 */
	int getEndPC();

	/**
	 * Answer back the handler pc of this entry.
	 *
	 * @return the handler pc of this entry
	 */
	int getHandlerPC();

	/**
	 * Answer back the catch type index in the constant pool.
	 *
	 * @return the catch type index in the constant pool
	 */
	int getCatchTypeIndex();

	/**
	 * Answer back the catch type name, null if getCatchTypeIndex() returns 0.
	 * This is the case for any exception handler.
	 *
	 * @return the catch type name, null if getCatchTypeIndex() returns 0.
	 * This is the case for any exception handler
	 */
	char[] getCatchType();
}
