/*******************************************************************************
 * Copyright (c) 2007, 2009 BEA Systems, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    wharley@bea.com - initial API and implementation
 *
 *******************************************************************************/

package org.eclipse.jdt.internal.core.builder;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;

/**
 * Used to convert an IFile into an ICompilationUnit,
 * for clients outside of this package.
 * @since 3.3
 */
public interface ICompilationUnitLocator {
	public ICompilationUnit fromIFile(IFile file);
}
