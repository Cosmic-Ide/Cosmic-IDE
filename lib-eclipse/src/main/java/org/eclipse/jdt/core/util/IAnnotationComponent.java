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
 * Description of an annotation component as described in the JVM specifications
 * (added in J2SE 1.5).
 *
 * This interface may be implemented by clients.
 *
 * @since 3.0
 */
public interface IAnnotationComponent {
	/**
	 * Answer back the component name index as described in the JVM specifications.
	 *
	 * @return the component name index
	 */
	int getComponentNameIndex();

	/**
	 * Answer back the component name as described in the JVM specifications.
	 *
	 * @return the component name
	 */
	char[] getComponentName();

	/**
	 * Answer back the component value as described in the JVM specifications.
	 *
	 * @return the component value
	 */
	IAnnotationComponentValue getComponentValue();
}
