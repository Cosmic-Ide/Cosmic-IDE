/*******************************************************************************
 * Copyright (c) 2005, 2018 BEA Systems, Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    tyeung@bea.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.env;

/**
 * This represents class file information about an annotation instance.
 */
public interface IBinaryAnnotation {

/**
 * @return the signature of the annotation type.
 */
char[] getTypeName();

/**
 * @return the list of element value pairs of the annotation
 */
IBinaryElementValuePair[] getElementValuePairs();

/**
 * @return true, if this an external annotation
 */
default boolean isExternalAnnotation() {
	return false;
}

/**
 * @return true, if this is a @Deprecated annotation.
 */
default boolean isDeprecatedAnnotation() {
	return false;
}
}

