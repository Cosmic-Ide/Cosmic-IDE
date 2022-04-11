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
 * Description of a inner class info as described in the JVM
 * specifications.
 *
 * This interface may be implemented by clients.
 *
 * @since 2.0
 */
public interface IInnerClassesAttributeEntry {

	/**
	 * Answer back the access flag of this inner classes attribute as specified in
	 * the JVM specifications.
	 *
	 * @return the access flag of this inner classes attribute as specified in
	 * the JVM specifications
	 */
	int getAccessFlags();

	/**
	 * Answer back the inner name index of this inner classes attribute as specified in
	 * the JVM specifications.
	 *
	 * @return the inner name index of this inner classes attribute as specified in
	 * the JVM specifications
	 */
	int getInnerNameIndex();

	/**
	 * Answer back the outer class name index of this inner classes attribute as specified in
	 * the JVM specifications.
	 *
	 * @return the outer class name index of this inner classes attribute as specified in
	 * the JVM specifications
	 */
	int getOuterClassNameIndex();

	/**
	 * Answer back the inner class name index of this inner classes attribute as specified in
	 * the JVM specifications.
	 *
	 * @return the inner class name index of this inner classes attribute as specified in
	 * the JVM specifications
	 */
	int getInnerClassNameIndex();

	/**
	 * Answer back the inner name of this inner classes attribute as specified in
	 * the JVM specifications, null if inner name index is equals to zero.
	 *
	 * @return the inner name of this inner classes attribute as specified in
	 * the JVM specifications, null if inner name index is equals to zero
	 */
	char[] getInnerName();

	/**
	 * Answer back the outer class name of this inner classes attribute as specified in
	 * the JVM specifications, null if outer class name index is equals to zero.
	 *
	 * @return the outer class name of this inner classes attribute as specified in
	 * the JVM specifications, null if outer class name index is equals to zero
	 */
	char[] getOuterClassName();

	/**
	 * Answer back the inner class name of this inner classes attribute as specified in
	 * the JVM specifications, null if inner class name index is equals to zero.
	 *
	 * @return the inner class name of this inner classes attribute as specified in
	 * the JVM specifications, null if inner class name index is equals to zero
	 */
	char[] getInnerClassName();

}
