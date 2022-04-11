/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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
 * Description of an enclosing method attribute as described in the JVM specifications
 * (added in J2SE 1.5).
 *
 * This interface may be implemented by clients.
 *
 * @since 3.0
 */
public interface IEnclosingMethodAttribute extends IClassFileAttribute {

	/**
	 * Answer back the enclosing class name as specified
	 * in the JVM specifications.
	 *
	 * @return the enclosing class name as specified
	 * in the JVM specifications
	 */
	char[] getEnclosingClass();

	/**
	 * Answer back the enclosing class name index.
	 *
	 * @return the enclosing class name index
	 */
	int getEnclosingClassIndex();

	/**
	 * Answer back the method descriptor of the enclosing method as specified
	 * in the JVM specifications.
	 *
	 * @return the method descriptor of the enclosing method as specified
	 * in the JVM specifications
	 */
	char[] getMethodDescriptor();

	/**
	 * Answer back the descriptor index of the enclosing method.
	 *
	 * @return the descriptor index of the enclosing method
	 */
	int getMethodDescriptorIndex();

	/**
	 * Answer back the name of the enclosing method as specified
	 * in the JVM specifications.
	 *
	 * @return the name of the enclosing method as specified
	 * in the JVM specifications
	 */
	char[] getMethodName();

	/**
	 * Answer back the name index of the enclosing method.
	 *
	 * @return the name index of the enclosing method
	 */
	int getMethodNameIndex();

	/**
	 * Answer back the name and type index of this attribute.
	 *
	 * @return the name and type index of this attribute
	 */
	int getMethodNameAndTypeIndex();
}
