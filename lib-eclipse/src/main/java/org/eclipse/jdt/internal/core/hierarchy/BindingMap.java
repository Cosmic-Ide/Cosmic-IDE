/*******************************************************************************
 * Copyright (c) 2016 Google, Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Xenos (Google) - Initial implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.hierarchy;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;

/**
 * Maps a {@link TypeBinding} onto values. Two {@link TypeBinding}s are considered equivalent
 * if their IDs are the same or if they have TypeIds.NoId and they are identical objects.
 * <p>
 * Takes into account the fact that a ReferenceBinding may have its ID change from NoId
 * to a real ID at any time without notice. (This is a behavior that was observed in
 * TypeHierarchyTests.testAnonymousType01 -- if type IDs could be made invariant then it
 * would be possible to implement a more efficient map that never needs to perform an
 * exhaustive search.)
 */
public class BindingMap<V> {
	private Map<TypeBinding, V> identityMap = new IdentityHashMap<>();
	private Object[] mapIdToValue = new Object[0];
	private Set<TypeBinding> bindingsWithoutAnId = new HashSet<>();

	public void put(TypeBinding key, V value) {
		this.identityMap.put(key, value);
		if (key.id != TypeIds.NoId) {
			int targetId = key.id;
			insertIntoIdMap(targetId, value);
		} else {
			this.bindingsWithoutAnId.add(key);
		}
	}

	@SuppressWarnings("unchecked")
	public V get(TypeBinding key) {
		// Check if we can find this binding by identity
		V value = this.identityMap.get(key);
		if (value != null) {
			return value;
		}
		int targetId = key.id;
		if (targetId != TypeIds.NoId) {
			// Check if we can find this binding by value
			if (targetId < this.mapIdToValue.length) {
				value = (V)this.mapIdToValue[targetId];
			}
			if (value != null) {
				return value;
			}

			// Check if there are any bindings that previously had no ID that have
			// subsequently been assigned one.
			for (Iterator<TypeBinding> bindingIter = this.bindingsWithoutAnId.iterator(); bindingIter.hasNext();) {
				TypeBinding nextBinding = bindingIter.next();

				if (nextBinding.id != TypeIds.NoId) {
					insertIntoIdMap(nextBinding.id, this.identityMap.get(nextBinding));
					bindingIter.remove();
				}
			}

			// Now look again to see if this binding can be found
			if (targetId < this.mapIdToValue.length) {
				value = (V)this.mapIdToValue[targetId];
			}
		}

		return value;
	}

	private void insertIntoIdMap(int targetId, V value) {
		int requiredSize = targetId + 1;
		if (this.mapIdToValue.length < requiredSize) {
			int newSize = requiredSize * 2;
			Object[] newArray = new Object[newSize];
			System.arraycopy(this.mapIdToValue, 0, newArray, 0, this.mapIdToValue.length);
			this.mapIdToValue = newArray;
		}
		this.mapIdToValue[targetId] = value;
	}

	public void clear() {
		this.identityMap.clear();
		this.bindingsWithoutAnId.clear();
		this.mapIdToValue = new Object[0];
	}
}
