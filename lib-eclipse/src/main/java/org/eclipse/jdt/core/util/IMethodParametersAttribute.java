/*******************************************************************************
 * Copyright (c) 2013 Jesper Steen Moeller and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Jesper Steen Moeller - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.util;

/**
 * Description of a method's parameters names as described in the JVM specifications.
 *
 * This interface may be implemented by clients.
 *
 * @since 3.10
 */
public interface IMethodParametersAttribute extends IClassFileAttribute {

	/**
	 * Answer back the number of parameters for this method as specified in
	 * the JVM specifications.
	 *
	 * @return the number of parameters for this method as specified in
	 * the JVM specifications
	 */
	int getMethodParameterLength();

	/**
	 * Answer back the name for the i'th parameter. Answer null if no
	 * name is available.
	 *
	 * @return back the name for the i'th parameter. Returns null if no
	 * name is available.
	 */
	char[] getParameterName(int i);

	/**
	 * Answer back the access flags for the i'th parameter, a mask of
	 * <code>ACC_FINAL</code>, <code>ACC_SYNTHETIC</code>, and <code>ACC_MANDATED</code>.
	 *
	 * @return the access flags for the i'th parameter.
	 */
	short getAccessFlags(int i);

}
