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

/**
 * This map uses a specialized implementation for few elements. Because compiler lookups are typically small maps.
 *
 * @author jkubitz
 */
public class CharDelegateMap<P> implements CharArrayMapper<P> {
	// Threshold was found during micro benchmarks. Its not very sensitive though.
	// Note the exact value would also depenend on the average key length since
	// bigDelegate hashes the whole key while smallDelegate only compares the chars until mismatch!
	private static final int SMAL_BIG_THRESHOLD = 5;

	// Instead of having a single CharArrayMapper<P> delegate
	// it showed to be a little bit faster to use two delegates to avoid polymorphic calls.
	// Only one of both will be not null:
	CharArrayMap<P> smallDelegate; // for few elements - avoiding hashing
	CharArrayHashMap<P> bigDelegate; // for many elements

	public CharDelegateMap() {
		this(0); // usually not very large
	}

	private CharArrayMapper<P> getDelegate() {
		return this.smallDelegate == null ? this.bigDelegate : this.smallDelegate;
	}

	public CharDelegateMap(int estimatedSize) {
		if (estimatedSize > SMAL_BIG_THRESHOLD) {
			this.bigDelegate = new CharArrayHashMap<>(estimatedSize);
		} else {
			this.smallDelegate = new CharArrayMap<>(estimatedSize);
		}
	}

	@Override
	public Collection<P> values() {
		return getDelegate().values();
	}

	@Override
	public boolean containsKey(char[] key) {
		return getDelegate().containsKey(key);
	}

	@Override
	public P get(char[] key) {
		if (this.smallDelegate != null) {
			return this.smallDelegate.get(key);
		}
		return this.bigDelegate.get(key);
	}

	@Override
	public P put(char[] key, P value) {
		if (this.smallDelegate != null) {
			P v = this.smallDelegate.put(key, value);
			if (this.smallDelegate.size() > SMAL_BIG_THRESHOLD) {
				toBigMap();
			}
			return v;
		} else {
			return this.bigDelegate.put(key, value);
		}
	}

	private void toBigMap() {
		this.bigDelegate = new CharArrayHashMap<>(this.smallDelegate.size());
		this.smallDelegate.transferTo(this.bigDelegate);
		this.smallDelegate = null;
	}

	@Override
	public int size() {
		return getDelegate().size();
	}

	@Override
	public String toString() {
		return CharArrayMapper.toString(this);
	}

	@Override
	public Collection<char[]> keys() {
		return getDelegate().keys();
	}
}
