/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.jdom;

/**
 * The <coe>ILineSeparatorFinder</code> finds previous and next line separators
 * in source.
 */
public interface ILineStartFinder {
/**
 * Returns the position of the start of the line at or before the given source position.
 *
 * <p>This defaults to zero if the position corresponds to a position on the first line
 * of the source.
 */
public int getLineStart(int position);
}
