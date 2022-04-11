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
 * Description of a local variable type table entry as specified in the JVM specifications
 * (added in J2SE 1.5).
 *
 * This interface may be implemented by clients.
 *
 * @since 3.0
 */
public interface ILocalVariableTypeTableEntry {

	/**
	 * Answer back the start pc of this entry as specified in
	 * the JVM specifications.
	 *
	 * @return the start pc of this entry as specified in
	 * the JVM specifications
	 */
	int getStartPC();

	/**
	 * Answer back the length of this entry as specified in
	 * the JVM specifications.
	 *
	 * @return the length of this entry as specified in
	 * the JVM specifications
	 */
	int getLength();

	/**
	 * Answer back the name index in the constant pool of this entry as specified in
	 * the JVM specifications.
	 *
	 * @return the name index in the constant pool of this entry as specified in
	 * the JVM specifications
	 */
	int getNameIndex();

	/**
	 * Answer back the signature index in the constant pool of this entry as specified in
	 * the JVM specifications.
	 *
	 * @return the signature index in the constant pool of this entry as specified in
	 * the JVM specifications
	 */
	int getSignatureIndex();

	/**
	 * Answer back the index of this entry as specified in
	 * the JVM specifications.
	 *
	 * @return the index of this entry as specified in
	 * the JVM specifications
	 */
	int getIndex();

	/**
	 * Answer back the name of this entry as specified in
	 * the JVM specifications.
	 *
	 * @return the name of this entry as specified in
	 * the JVM specifications
	 */
	char[] getName();

	/**
	 * Answer back the signature of this entry as specified in
	 * the JVM specifications.
	 *
	 * @return the signature of this entry as specified in
	 * the JVM specifications
	 */
	char[] getSignature();
}
