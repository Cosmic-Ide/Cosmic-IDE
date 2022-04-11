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
package org.eclipse.jdt.core;

/**
 * A source range defines an element's source coordinates relative to
 * its source buffer.
 *
 * <p>Clients may use the method {@link org.eclipse.jdt.core.SourceRange#isAvailable(ISourceRange)}
 * in order to find out if a source range is available. This method returns <code>false</code>
 * when the source range offset is equals to <code>-1</code>.</p>
 *
 * <p>Clients may use the default implementation provided by {@link SourceRange}.</p>
 *
 * @see SourceRange
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ISourceRange {

/**
 * Returns the number of characters of the source code for this element,
 * relative to the source buffer in which this element is contained.
 *
 * @return the number of characters of the source code for this element,
 * relative to the source buffer in which this element is contained
 */
int getLength();
/**
 * Returns the 0-based index of the first character of the source code for this element,
 * relative to the source buffer in which this element is contained. However, if the element
 * has no associated source code, an implementation may return -1.
 *
 * @return the 0-based index of the first character of the source code for this element,
 * relative to the source buffer in which this element is contained. However, if the element
 * has no associated source code, an implementation may return -1.
 */
int getOffset();
}
