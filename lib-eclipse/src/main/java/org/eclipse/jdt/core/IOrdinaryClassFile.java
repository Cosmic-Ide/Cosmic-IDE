/*******************************************************************************
 * Copyright (c) 2017 GK Software AG, and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core;

/**
 * Ordinary form of a {@link IClassFile} which holds exactly one <code>IType</code>.
 *
 * @since 3.14
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IOrdinaryClassFile extends IClassFile {
	/**
	 * Returns the type contained in this class file.
	 * This is a handle-only method. The type may or may not exist.
	 *
	 * <p>This method supersedes the corresponding super method.
	 * This method will never throw {@link UnsupportedOperationException}.</p>
	 *
	 * @return the type contained in this class file
	 */
	@Override
	IType getType();
}
