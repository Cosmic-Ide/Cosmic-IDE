/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
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
 *        Andy Clement - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *******************************************************************************/
package org.eclipse.jdt.core.util;

/**
 * Description of a runtime visible type annotations attribute as described in the JVM specifications
 * (added in JavaSE-1.8).
 *
 * This interface may be implemented by clients.
 *
 * @since 3.10
 */
public interface IRuntimeVisibleTypeAnnotationsAttribute extends IClassFileAttribute {

	/**
	 * Answer back the number of annotations as described in the JVM specifications.
	 *
	 * @return the number of annotations
	 */
	int getExtendedAnnotationsNumber();

	/**
	 * Answer back the extended annotations. Answers an empty collection if none.
	 *
	 * @return the extended annotations. Answers an empty collection if none.
	 */
	IExtendedAnnotation[] getExtendedAnnotations();
}