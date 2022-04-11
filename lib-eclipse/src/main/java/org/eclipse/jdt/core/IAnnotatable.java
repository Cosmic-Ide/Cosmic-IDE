/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.jdt.core;

/**
 * Common protocol for Java elements that can be annotated.
 *
 * @since 3.4
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IAnnotatable {

	/**
	 * Returns the annotation with the given name declared on this element.
	 * This is a handle-only method. The annotation may or may not exist.
	 *
	 * @param name the given simple name
	 * @return the annotation with the given name declared on this element
	 */
	IAnnotation getAnnotation(String name);

	/**
	 * Returns the annotations for this element.
	 * Returns an empty array if this element has no annotations.
	 *
	 * @exception JavaModelException if this element does not exist or if an
	 *      exception occurs while accessing its corresponding resource.
	 * @return the annotations of this element,
	 * 		in the order declared in the source, or an empty array if none
	 * @since 3.4
	 */
	IAnnotation[] getAnnotations() throws JavaModelException;
}
