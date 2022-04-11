/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.internal.compiler.env.INameEnvironmentExtension;

/**
 * The name environment provides a callback API that the compiler
 * can use to look up types, compilation units, and packages in the
 * current environment.  The name environment is passed to the compiler
 * on creation.
 *
 * This name environment can be canceled using the monitor passed as an argument to
 * {@link #setMonitor(IProgressMonitor)}.
 *
 * @since 3.6
 */
public interface INameEnvironmentWithProgress extends INameEnvironmentExtension {

	/**
	 * Set the monitor for the given name environment. In order to be able to cancel this name environment calls,
	 * a non-null monitor should be given.
	 *
	 * @param monitor the given monitor
	 */
	void setMonitor(IProgressMonitor monitor);

}