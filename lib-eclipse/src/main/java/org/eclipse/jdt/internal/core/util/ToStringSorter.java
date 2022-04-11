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
package org.eclipse.jdt.internal.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * The SortOperation takes a collection of objects and returns
 * a sorted collection of these objects. The sorting of these
 * objects is based on their toString(). They are sorted in
 * alphabetical order.
 */
public class ToStringSorter <T> {
	private final Function<T, String> toString;

	public ToStringSorter(Function<T, String> toString) {
		this.toString = toString;
	}

	static class Pair<T> implements Comparable<Pair<T>> {
		final T object;
		final String string;
		public Pair(T k, String s) {
			this.object = k;
			this.string = s;
		}

		@Override
		public int compareTo(Pair<T> other) {
			return this.string.compareTo(other.string);
		}
	}

	/**
	 *  Return a new sorted collection from this unsorted collection.
	 */
	public List<Pair<T>> sort(Collection<T> unSorted) {
		int size = unSorted.size();
		//copy the list so can return a new sorted collection
		List<Pair <T>> sortedObjects = new ArrayList<>(size);
		unSorted.forEach(k -> sortedObjects.add(new Pair<>(k, this.toString.apply(k))));
		Collections.sort(sortedObjects);
		return sortedObjects;
	}
}
