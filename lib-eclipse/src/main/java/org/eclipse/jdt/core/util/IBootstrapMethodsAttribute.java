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
package org.eclipse.jdt.core.util;

/**
 * Description of a bootstrap methods attribute as described in the JVM specifications.
 *
 * This interface may be implemented by clients.
 *
 * @since 3.8
 */
public interface IBootstrapMethodsAttribute extends IClassFileAttribute {

	/**
	 * Answer back the number of bootstrap methods of this entry as specified in
	 * the JVM specifications.
	 *
	 * @return the number of bootstrap methods of this entry as specified in
	 * the JVM specifications
	 */
	int getBootstrapMethodsLength();

	/**
	 * Answer back the bootstrap methods table of this entry as specified in
	 * the JVM specifications. Answer an empty array if none.
	 *
	 * @return the bootstrap methods table of this entry as specified in
	 * the JVM specifications. Answer an empty array if none
	 */
	IBootstrapMethodsEntry[] getBootstrapMethods();

}
