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
 * This class represents a stack map table attribute.
 *
 * This interface may be implemented by clients.
 *
 * @since 3.2
 */
public interface IStackMapTableAttribute extends IClassFileAttribute {

	/**
	 * Answer back the number of stack map frames of this atribute as specified in
	 * the JVM specifications.
	 *
	 * @return the number of stack map frames of this atribute as specified in
	 * the JVM specifications
	 */
	int getNumberOfEntries();

	/**
	 * Answer back the stack map frames for this attribute as specified
	 * in the JVM specifications.
	 *
	 * @return the stack map frames for this attribute as specified
	 * in the JVM specifications
	 */
	IStackMapFrame[] getStackMapFrame();
}
