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
package org.eclipse.jdt.core.util;

/**
 * Description of a source attribute as described in the JVM
 * specifications.
 *
 * This interface may be implemented by clients.
 *
 * @since 2.0
 */
public interface ISourceAttribute extends IClassFileAttribute {

	/**
	 * Answer back the source file index of this attribute.
	 *
	 * @return the source file index of this attribute
	 */
	int getSourceFileIndex();

	/**
	 * Answer back the source file name of this attribute.
	 *
	 * @return the source file name of this attribute
	 */
	char[] getSourceFileName();

}
