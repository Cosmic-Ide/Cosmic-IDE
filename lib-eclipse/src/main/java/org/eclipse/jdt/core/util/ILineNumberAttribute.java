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
 * Description of a line number attribute as described in the JVM specifications.
 *
 * This interface may be implemented by clients.
 *
 * @since 2.0
 */
public interface ILineNumberAttribute extends IClassFileAttribute {

	/**
	 * Answer back the line number table length as specified in
	 * the JVM specifications.
	 *
	 * @return the line number table length as specified in
	 * the JVM specifications
	 */
	int getLineNumberTableLength();

	/**
	 * Answer back the array of pairs (start pc, line number) as specified in the
	 * JVM specifications. Answer an empty array if none.
	 *
	 * @return the array of pairs (start pc, line number) as specified in the
	 * JVM specifications. Answer an empty array if none
	 */
	int[][] getLineNumberTable();

}
