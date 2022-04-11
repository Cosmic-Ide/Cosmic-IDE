/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
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
import org.eclipse.jdt.core.util.IComponentInfo;
import org.eclipse.jdt.core.util.IConstantPool;
import org.eclipse.jdt.core.util.IRecordAttribute;

/**
 * Default implementation of {@link IRecordAttribute}.
 */
public class RecordAttribute extends ClassFileAttribute implements IRecordAttribute {
	private static final IComponentInfo[] NO_ENTRIES = new IComponentInfo[0];

	private int nComponents;
	private IComponentInfo[] entries;

	public RecordAttribute(
		byte[] classFileBytes,
		IConstantPool constantPool,
		int offset)
		throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		this.nComponents = u2At(classFileBytes, 6, offset);
		final int length = this.nComponents;
		if (length != 0) {
			int readOffset = 8;
			this.entries = new IComponentInfo[length];
			for (int i = 0; i < length; i++) {
				this.entries[i] = new ComponentInfo(classFileBytes, constantPool, offset + readOffset);
				readOffset += this.entries[i].sizeInBytes();
			}
		} else {
			this.entries = NO_ENTRIES;
		}
	}

	@Override
	public int getNumberOfComponents() {
		return this.nComponents;
	}

	@Override
	public IComponentInfo[] getComponentInfos() {
		return this.entries;
	}
}
