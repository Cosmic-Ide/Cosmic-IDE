/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 * Description of permitted subclasses attribute as described in the JVM
 * specifications.
 *
 * @since 3.24
 */
public interface IPermittedSubclassesAttributeEntry {

	/**
	 * Answer back the permitted subclass name as specified
	 * in the JVM specifications.
	 *
	 * @return the permitted subclass name as specified
	 * in the JVM specifications
	 */
	char[] getPermittedSubclassName();

	/**
	 * Answer back the permitted subclass name index.
	 *
	 * @return the permitted class name index
	 */
	int gePermittedSubclassIndex();
}
