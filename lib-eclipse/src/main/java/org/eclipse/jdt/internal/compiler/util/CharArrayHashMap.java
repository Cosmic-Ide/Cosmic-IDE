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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

public final class CharArrayHashMap<V> implements CharArrayMapper<V>, Serializable {
	private static final long serialVersionUID = -4247853285180184851L;

	// Constructing the intermediate CharArray is little overhead on each get().
	// Would be slightly better to have a HashMap specialization to char[] keys.
	// However this implementation is only used for large maps where algorithmic overhead dominates.
	private final HashMap<CharArray, V> map;

	public CharArrayHashMap(int initialCapacity) {
		this.map = new HashMap<>(initialCapacity);
	}

	@Override
	public Collection<V> values() {
		return new ArrayList<>(this.map.values());
	}

	@Override
	public Collection<char[]> keys() {
		return this.map.keySet().stream().map(s -> s.getKey()).collect(Collectors.toList());
	}

	@Override
	public boolean containsKey(char[] key) {
		return this.map.containsKey(new CharArray(key));
	}

	@Override
	public V get(char[] key) {
		return this.map.get(new CharArray(key));
	}

	@Override
	public V put(char[] key, V value) {
		return this.map.put(new CharArray(key), value);
	}

	@Override
	public int size() {
		return this.map.size();
	}

	@Override
	public String toString() {
		return CharArrayMapper.toString(this);
	}
}