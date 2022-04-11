/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.util;

import org.eclipse.jdt.core.util.ClassFormatException;
import org.eclipse.jdt.core.util.IBootstrapMethodsEntry;
import org.eclipse.jdt.core.util.IConstantPool;

/**
 * Default implementation of {@link IBootstrapMethodsEntry}
 */
public class BootstrapMethodsEntry
	extends ClassFileStruct
	implements IBootstrapMethodsEntry {

	private int bootstrapMethodReference;
	private int[] bootstrapArguments;

	public BootstrapMethodsEntry(byte classFileBytes[], IConstantPool constantPool, int offset) throws ClassFormatException {
		this.bootstrapMethodReference = u2At(classFileBytes, 0, offset);
		int length = u2At(classFileBytes, 2, offset);
		int[] arguments = new int[length];
		int position = 4;
		for (int i = 0; i < length; i++) {
			arguments[i] = u2At(classFileBytes, position, offset);
			position += 2;
		}
		this.bootstrapArguments = arguments;
	}

	/**
	 * @see IBootstrapMethodsEntry#getBootstrapArguments()
	 */
	@Override
	public int[] getBootstrapArguments() {
		return this.bootstrapArguments;
	}

	/**
	 * @see IBootstrapMethodsEntry#getBootstrapMethodReference()
	 */
	@Override
	public int getBootstrapMethodReference() {
		return this.bootstrapMethodReference;
	}
}
