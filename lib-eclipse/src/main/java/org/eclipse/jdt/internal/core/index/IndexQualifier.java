/*******************************************************************************
 * Copyright (c) 2021 Gayan Perera and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Gayan Perera - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.index;

/**
 * The index qualifier which is used for represent MetaIndex qualifications which contains a category and a search key.
 */
public final class IndexQualifier {
	private final char[] category;

	private final char[] key;

	private IndexQualifier(char[] category, char[] key) {
		this.category = category;
		this.key = key;
	}

	public static IndexQualifier qualifier(char[] category, char[] key) {
		return new IndexQualifier(category, key);
	}

	public char[] getCategory() {
		return this.category;
	}

	public char[] getKey() {
		return this.key;
	}
}