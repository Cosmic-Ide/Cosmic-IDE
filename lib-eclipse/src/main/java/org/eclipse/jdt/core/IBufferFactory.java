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
package org.eclipse.jdt.core;

/**
 * A factory that creates <code>IBuffer</code>s for openables.
 * <p>
 * This interface may be implemented by clients.
 * </p>
 * @since 2.0
 * @deprecated Use {@link WorkingCopyOwner} instead
 */
public interface IBufferFactory {

	/**
	 * Creates a buffer for the given owner.
	 * The new buffer will be initialized with the contents of the owner
	 * if and only if it was not already initialized by the factory (a buffer is uninitialized if
	 * its content is <code>null</code>).
	 *
	 * @param owner the owner of the buffer
	 * @return the newly created buffer
	 * @see IBuffer
	 */
	IBuffer createBuffer(IOpenable owner);
}

