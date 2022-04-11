/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.util;

/**
 * Description of a nest host attribute as described in the JVM
 * specifications.
 * @since 3.16
 */
public interface INestHostAttribute {
	/**
	 * Answer back the class name as specified
	 * in the JVM specifications.
	 *
	 * @return the class name as specified
	 * in the JVM specifications
	 */
	char[] getNestHostName();

	/**
	 * Answer back the class name index.
	 *
	 * @return the class name index
	 */
	int getNestHostIndex();
}
