/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
 * Description of a Module Main Class Attribute as described in the JVMS9 4.7.27
 *
 * This interface may be implemented by clients.
 *
 * @since 3.14
 */
public interface IModuleMainClassAttribute extends IClassFileAttribute {

	/**
	 * Answer back the main class index.
	 *
	 * @return the main class index
	 */
	int getMainClassIndex();

	/**
	 * Answer back the name of main class.
	 *
	 * @return the name of main class
	 */
	char[] getMainClassName();
}
