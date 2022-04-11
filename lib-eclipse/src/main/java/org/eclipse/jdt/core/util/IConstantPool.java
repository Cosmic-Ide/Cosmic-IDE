/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 * Description of a constant pool as described in the JVM specifications.
 *
 * This interface may be implemented by clients.
 *
 * @since 2.0
 */
public interface IConstantPool {

	/**
	 * Answer back the number of entries in the constant pool.
	 *
	 * @return the number of entries in the constant pool
	 */
	int getConstantPoolCount();

	/**
	 * Answer back the type of the entry at the given index in the constant pool.
	 *
	 * @param index the index of the entry in the constant pool
	 * @return the type of the entry at the index @index in the constant pool
	 */
	int getEntryKind(int index);

	/**
	 * Answer back the entry at the given index in the constant pool.
	 *
	 * <p>The return value can be an instance of {@link IConstantPoolEntry2} if the value returned
	 * by {@link #getEntryKind(int)} is either {@link IConstantPoolConstant#CONSTANT_MethodHandle},
	 * {@link IConstantPoolConstant#CONSTANT_MethodType},
	 * {@link IConstantPoolConstant#CONSTANT_InvokeDynamic},
	 * {@link IConstantPoolConstant#CONSTANT_Dynamic}.</p>
	 *
	 * <p>The return value can be an instance of {@link IConstantPoolEntry3} if the value returned
	 * by {@link #getEntryKind(int)} is either {@link IConstantPoolConstant#CONSTANT_Module}
	 * or {@link IConstantPoolConstant#CONSTANT_Package}.</p>
	 *
	 * @param index the index of the entry in the constant pool
	 * @return the entry at the given index in the constant pool
	 */
	IConstantPoolEntry decodeEntry(int index);
}
