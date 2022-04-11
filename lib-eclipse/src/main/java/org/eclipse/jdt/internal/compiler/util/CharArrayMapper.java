/*******************************************************************************
 * Copyright (c) 2021 jkubitz and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     jkubitz - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.util;

import java.util.Collection;
import java.util.stream.Collectors;

public interface CharArrayMapper<V> extends Cloneable {

	public boolean containsKey(char[] key);

	public V get(char[] key);

	/** @return the previous value **/
	public V put(char[] key, V value);

	/** @return the number of keys **/
	public int size();

	/**
	 * Returns a copied collection of values.
	 *
	 * @return all values in undefined order. The order is not guaranteed to be stable.
	 **/
	public Collection<V> values();

	/**
	 * Returns a copied collection of keys.
	 *
	 * @return all keys in undefined order. The order is not guaranteed to be stable.
	 **/
	public Collection<char[]> keys();

	public static <V> String toString(CharArrayMapper<V> map) {
		return map.keys().stream().map(k -> new String(k) + "->" + map.get(k)) //$NON-NLS-1$
				.collect(Collectors.joining("\n")); //$NON-NLS-1$
	}

}
