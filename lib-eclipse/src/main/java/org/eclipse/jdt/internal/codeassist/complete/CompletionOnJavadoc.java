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
package org.eclipse.jdt.internal.codeassist.complete;

public interface CompletionOnJavadoc {

	// Bit pattern for javadoc completion flags
	int JAVADOC = 0x0001;
	int EXCEPTION = 0x0002;
	int TEXT = 0x0004;
	int BASE_TYPES = 0x0008;
	int ONLY_INLINE_TAG = 0x0010;
	int REPLACE_TAG = 0x0020;
	int FORMAL_REFERENCE = 0x0040;
	int ALL_POSSIBLE_TAGS = 0x0080;

	/**
	 * Get completion node flags.
	 *
	 * @return int Flags of the javadoc completion node.
	 */
	public int getCompletionFlags();

	/**
	 * @param flags The completionFlags to add.
	 */
	public void addCompletionFlags(int flags);

}
