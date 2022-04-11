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
package org.eclipse.jdt.internal.core;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.WorkingCopyOwner;

/**
 * Wraps an IBufferFactory.
 * TODO remove when removing IBufferFactory
 * @deprecated
 */
public class BufferFactoryWrapper extends WorkingCopyOwner {

	public org.eclipse.jdt.core.IBufferFactory factory;

	private BufferFactoryWrapper(org.eclipse.jdt.core.IBufferFactory factory) {
		this.factory = factory;
	}

	public static WorkingCopyOwner create(org.eclipse.jdt.core.IBufferFactory factory) {
		return new BufferFactoryWrapper(factory);
	}

	@Override
	public IBuffer createBuffer(ICompilationUnit workingCopy) {
		if (this.factory == null) return super.createBuffer(workingCopy);
		return this.factory.createBuffer(workingCopy);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof BufferFactoryWrapper)) return false;
		BufferFactoryWrapper other = (BufferFactoryWrapper)obj;
		if (this.factory == null) return other.factory == null;
		return this.factory.equals(other.factory);
	}
	@Override
	public int hashCode() {
		if (this.factory == null) return 0;
		return this.factory.hashCode();
	}
	@Override
	public String toString() {
		return "FactoryWrapper for " + this.factory; //$NON-NLS-1$
	}
}
