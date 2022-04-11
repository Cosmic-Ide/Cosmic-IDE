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
 * Represents the class file of a module description ("module-info.class").
 *
 * @since 3.14
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IModularClassFile extends IClassFile {
	/**
	 * Returns the module description contained in this type root.
	 * An error-free {@link IModularClassFile} should always have a module.
	 *
	 * @throws JavaModelException
	 * @return the module description contained in the type root.
	 */
	@Override
	IModuleDescription getModule() throws JavaModelException;
}
