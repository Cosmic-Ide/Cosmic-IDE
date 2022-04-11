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
 * Specification for a generic source code formatter. Client plug-ins can contribute
 * an implementation for an ICodeFormatter, through the extension point "org.eclipse.jdt.core.codeFormatter".
 * In case none is found, a default formatter can be provided through the ToolFactory.
 *
 * @see ToolFactory#createCodeFormatter()
 * @see ToolFactory#createDefaultCodeFormatter(java.util.Map options)
 * @since 2.0
 * @deprecated Use {@link org.eclipse.jdt.core.formatter.CodeFormatter} instead (note: options have changed)
 */
public interface ICodeFormatter {

	/**
	 * Formats the String <code>sourceString</code>,
	 * and returns a string containing the formatted version.
	 *
	 * @param string the string to format
	 * @param indentationLevel the initial indentation level, used
	 *      to shift left/right the entire source fragment. An initial indentation
	 *      level of zero has no effect.
	 * @param positions an array of positions to map. These are
	 *      character-based source positions inside the original source,
	 * 		arranged in non-decreasing order, for which corresponding positions in
	 *     the formatted source will be computed (so as to relocate elements associated
	 *     with the original source). It updates the positions array with updated
	 *     positions. If set to <code>null</code>, then no positions are mapped.
	 * @param lineSeparator the line separator to use in formatted source,
	 *     if set to <code>null</code>, then the platform default one will be used.
	 * @return the formatted output string.
	 */
	String format(String string, int indentationLevel, int[] positions, String lineSeparator);
}
