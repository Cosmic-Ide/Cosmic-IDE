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
 * Description of a constant value attribute as described in the JVM
 * specifications.
 *
 * This interface may be implemented by clients.
 *
 * @since 2.0
 */
public interface IExceptionAttribute extends IClassFileAttribute {

	/**
	 * Answer back the number of exceptions of the exception attribute.
	 *
	 * @return the number of exceptions of the exception attribute
	 */
	int getExceptionsNumber();

	/**
	 * Answer back the exception names of the exception attribute. Answers an
	 * empty collection if none.
	 *
	 * @return the exception names of the exception attribute. Answers an
	 * empty collection if none
	 */
	char[][] getExceptionNames();

	/**
	 * Answer back the exception indexes of the exception attribute. Answers an
	 * empty collection if none.
	 *
	 * @return the exception indexes of the exception attribute. Answers an
	 * empty collection if none
	 */
	int[] getExceptionIndexes();
}
