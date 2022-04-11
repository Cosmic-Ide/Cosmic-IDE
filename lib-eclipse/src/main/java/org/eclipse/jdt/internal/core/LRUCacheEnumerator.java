/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core;

import java.util.Enumeration;

/**
 *	The <code>LRUCacheEnumerator</code> returns its elements in
 *	the order they are found in the <code>LRUCache</code>, with the
 *	most recent elements first.
 *
 *	Once the enumerator is created, elements which are later added
 *	to the cache are not returned by the enumerator.  However,
 *	elements returned from the enumerator could have been closed
 *	by the cache.
 */
public class LRUCacheEnumerator<V> implements Enumeration<V> {
	/**
	 *	Current element;
	 */
	protected LRUEnumeratorElement<V> elementQueue;

	public static class LRUEnumeratorElement<V> {
		/**
		 *	Value returned by <code>nextElement()</code>;
		 */
		public V value;

		/**
		 *	Next element
		 */
		public LRUEnumeratorElement<V> next;

		/**
		 * Constructor
		 */
		public LRUEnumeratorElement(V value) {
			this.value = value;
		}
	}
/**
 *	Creates a CacheEnumerator on the list of <code>LRUEnumeratorElements</code>.
 */
public LRUCacheEnumerator(LRUEnumeratorElement<V> firstElement) {
	this.elementQueue = firstElement;
}
/**
 * Returns true if more elements exist.
 */
@Override
public boolean hasMoreElements() {
	return this.elementQueue != null;
}
/**
 * Returns the next element.
 */
@Override
public V nextElement() {
	V temp = this.elementQueue.value;
	this.elementQueue = this.elementQueue.next;
	return temp;
}
}
