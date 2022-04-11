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
package org.eclipse.jdt.internal.core.search.indexing;

import java.util.stream.Stream;

import org.eclipse.jdt.core.compiler.CharOperation;

/**
 * The index qualifier query which is used for searching MetaIndex qualifications in IndexManager.
 */
public final class QualifierQuery {
	public static final char[] NO_CHARS = new char[0];

	public enum QueryCategory {
		SUPER,
		REF;

		public static QueryCategory[] fromEncoded(char[][] encoded) {
			return Stream.of(encoded).map(c -> QueryCategory.valueOf(String.valueOf(c))).toArray(QueryCategory[]::new);
		}
	}


	private QueryCategory[] categories;

	private char[] simpleKey;

	private char[] qualifiedKey;

	private QualifierQuery(QueryCategory[] categories, char[] simpleKey, char[] qualifiedKey) {
		this.categories = categories;
		this.simpleKey = simpleKey;
		this.qualifiedKey = qualifiedKey;
	}

	public static QualifierQuery fromEncodedQuery(char[] query) {
		char[][] components = CharOperation.splitOn(':', query);
		return new QualifierQuery(QueryCategory.fromEncoded(CharOperation.splitOn(',', components[0])), components[1],
				components.length == 3 ? components[2] : NO_CHARS);
	}

	public static char[] encodeQuery(QueryCategory[] categories, char[] simpleKey, char[] qualifierKey) {
		char[] category = CharOperation.concatWith(Stream.of(categories).map(c -> c.name().toCharArray()).toArray(char[][]::new), ',');
		return CharOperation.concatWith(new char[][] {category, simpleKey, qualifierKey}, ':');
	}

	public QueryCategory[] getCategories() {
		return this.categories;
	}

	public char[] getQualifiedKey() {
		return this.qualifiedKey;
	}

	public char[] getSimpleKey() {
		return this.simpleKey;
	}
}