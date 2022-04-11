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
 * Represents an annotation on a package declaration, a type, a method, a field
 * or a local variable in a compilation unit or a class file.
 * <p>
 * Annotations are obtained using {@link IAnnotatable#getAnnotation(String)}.
 * </p><p>
 * Note that annotations are not children of their declaring element.
 * To get a list of the annotations use {@link IAnnotatable#getAnnotations()}.
 * </p>
 *
 * @since 3.4
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IAnnotation extends IJavaElement, ISourceReference {

	/**
	 * Returns the name of this annotation. If this annotation is coming from
	 * a compilation unit, this is either a simple name (e.g. for <code>@MyAnnot</code>, the name
 	 * is "MyAnnot"), or a qualified name  (e.g. for <code>@x. y.    MyAnnot</code>, the name is
 	 * "x.y.MyAnnot"). If this annotation is coming from a class file, this is always a fully
 	 * qualified name.
 	 * <p>Note that the name has been trimmed from its whitespaces. To extract the name as it
 	 * appears in the source, use {@link #getNameRange()}.
 	 * </p><p>
	 * This is a handle-only method.  The annotation may or may not be present.
	 * </p>
	 *
	 * @return the name of this annotation
	 */
	@Override
	String getElementName();

	/**
	 * Returns the member-value pairs of this annotation. Returns an empty
	 * array if this annotation is a marker annotation. Returns a size-1 array if this
	 * annotation is a single member annotation. In this case, the member
	 * name is always <code>"value"</code>.
	 *
	 * @return the member-value pairs of this annotation
	 * @throws JavaModelException if this element does not exist or if an
	 *		exception occurs while accessing its corresponding resource
	 */
	IMemberValuePair[] getMemberValuePairs() throws JavaModelException;

	/**
	 * Returns the position relative to the order this annotation is defined in the source.
	 * Numbering starts at 1 (thus the first occurrence is occurrence 1, not occurrence 0).
	 * <p>
	 * Two annotations ann1 and ann2 that are equal (e.g. 2 annotations with the same name on
	 * the same type) can be distinguished using their occurrence counts. If annotation
	 * ann1 appears first in the source, it will have an occurrence count of 1. If annotation
	 * ann2 appears right after annotation ann1, it will have an occurrence count of 2.
	 * </p><p>
	 * This is a handle-only method.  The annotation may or may not be present.
	 * </p>
	 *
	 * @return the position relative to the order this annotation is defined in the source
	 */
	int getOccurrenceCount();
}
