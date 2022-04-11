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
import java.util.HashMap;
import java.util.Hashtable;

import org.eclipse.jdt.internal.core.util.LRUCache;
import org.eclipse.jdt.internal.core.util.Messages;

/**
 *	The <code>OverflowingLRUCache</code> is an LRUCache which attempts
 *	to maintain a size equal or less than its <code>fSpaceLimit</code>
 *	by removing the least recently used elements.
 *
 *	<p>The cache will remove elements which successfully close and all
 *	elements which are explicitly removed.
 *
 *	<p>If the cache cannot remove enough old elements to add new elements
 *	it will grow beyond <code>fSpaceLimit</code>. Later, it will attempt to
 *	shrink back to the maximum space limit.
 *
 *	The method <code>close</code> should attempt to close the element.  If
 *	the element is successfully closed it will return true and the element will
 *	be removed from the cache.  Otherwise the element will remain in the cache.
 *
 *	<p>The cache implicitly attempts shrinks on calls to <code>put</code>and
 *	<code>setSpaceLimit</code>.  Explicitly calling the <code>shrink</code> method
 *	will also cause the cache to attempt to shrink.
 *
 *	<p>The cache calculates the used space of all elements which implement
 *	<code>ILRUCacheable</code>.  All other elements are assumed to be of size one.
 *
 *	<p>Use the <code>#peek(Object)</code> and <code>#disableTimestamps()</code> method to
 *	circumvent the timestamp feature of the cache.  This feature is intended to be used
 *	only when the <code>#close(LRUCacheEntry)</code> method causes changes to the cache.
 *	For example, if a parent closes its children when </code>#close(LRUCacheEntry)</code> is called,
 *	it should be careful not to change the LRU linked list.  It can be sure it is not causing
 *	problems by calling <code>#peek(Object)</code> instead of <code>#get(Object)</code> method.
 *
 *	@see LRUCache
 */
public abstract class OverflowingLRUCache<K, V> extends LRUCache<K, V> {
	/**
	 * Indicates if the cache has been over filled and by how much.
	 */
	protected int overflow = 0;
	/**
	 *	Indicates whether or not timestamps should be updated
	 */
	protected boolean timestampsOn = true;
	/**
	 *	Indicates how much space should be reclaimed when the cache overflows.
	 *	Inital load factor of one third.
	 */
	protected double loadFactor = 0.333;
/**
 * Creates a OverflowingLRUCache.
 * @param size Size limit of cache.
 */
public OverflowingLRUCache(int size) {
	this(size, 0);
}
/**
 * Creates a OverflowingLRUCache.
 * @param size Size limit of cache.
 * @param overflow Size of the overflow.
 */
public OverflowingLRUCache(int size, int overflow) {
	super(size);
	this.overflow = overflow;
}
	/**
	 * Returns a new cache containing the same contents.
	 *
	 * @return New copy of this object.
	 */
	@Override
	public OverflowingLRUCache<K, V> clone() {

		OverflowingLRUCache<K, V> newCache = (OverflowingLRUCache<K, V>)newInstance(this.spaceLimit, this.overflow);
		LRUCacheEntry<K, V> qEntry;

		/* Preserve order of entries by copying from oldest to newest */
		qEntry = this.entryQueueTail;
		while (qEntry != null) {
			newCache.privateAdd (qEntry.key, qEntry.value, qEntry.space);
			qEntry = qEntry.previous;
		}
		return newCache;
	}
/**
 * Returns true if the element is successfully closed and
 * removed from the cache, otherwise false.
 *
 * <p>NOTE: this triggers an external remove from the cache
 * by closing the object.
 *
 */
protected abstract boolean close(LRUCacheEntry<K, V> entry);
	/**
	 *	Returns an enumerator of the values in the cache with the most
	 *	recently used first.
	 */
	public Enumeration<V> elements() {
		if (this.entryQueue == null)
			return new LRUCacheEnumerator<>(null);
		LRUCacheEnumerator.LRUEnumeratorElement<V> head =
			new LRUCacheEnumerator.LRUEnumeratorElement<>(this.entryQueue.value);
		LRUCacheEntry<K, V> currentEntry = this.entryQueue.next;
		LRUCacheEnumerator.LRUEnumeratorElement<V> currentElement = head;
		while(currentEntry != null) {
			currentElement.next = new LRUCacheEnumerator.LRUEnumeratorElement<>(currentEntry.value);
			currentElement = currentElement.next;

			currentEntry = currentEntry.next;
		}
		return new LRUCacheEnumerator<>(head);
	}

	@Override
	public double fillingRatio() {
		return (this.currentSpace + this.overflow) * 100.0 / this.spaceLimit;
	}

	/**
	 * For internal testing only.
	 * This method exposed only for testing purposes!
	 *
	 * @return Hashtable of entries
	 */
	public Hashtable<K, LRUCacheEntry<K, V>> getEntryTable() {
		return this.entryTable;
	}
/**
 * Returns the load factor for the cache.  The load factor determines how
 * much space is reclaimed when the cache exceeds its space limit.
 * @return double
 */
public double getLoadFactor() {
	return this.loadFactor;
}
	/**
	 *	@return The space by which the cache has overflown.
	 */
	public int getOverflow() {
		return this.overflow;
	}

	/**
	 * Ensures there is the specified amount of free space in the receiver,
	 * by removing old entries if necessary.  Returns true if the requested space was
	 * made available, false otherwise.  May not be able to free enough space
	 * since some elements cannot be removed until they are saved.
	 *
	 * @param space Amount of space to free up
	 */
	@Override
	protected boolean makeSpace(int space) {

		int limit = this.spaceLimit;
		if (this.overflow == 0 && this.currentSpace + space <= limit) {
			/* if space is already available */
			return true;
		}

		/* Free up space by removing oldest entries */
		int spaceNeeded = (int)((1 - this.loadFactor) * limit);
		spaceNeeded = (spaceNeeded > space) ? spaceNeeded : space;
		LRUCacheEntry<K, V> entry = this.entryQueueTail;

		try {
			// disable timestamps update while making space so that the previous and next links are not changed
			// (by a call to get(Object) for example)
			this.timestampsOn = false;

			while (this.currentSpace + spaceNeeded > limit && entry != null) {
				this.privateRemoveEntry(entry, false, false);
				entry = entry.previous;
			}
		} finally {
			this.timestampsOn = true;
		}

		/* check again, since we may have aquired enough space */
		if (this.currentSpace + space <= limit) {
			this.overflow = 0;
			return true;
		}

		/* update fOverflow */
		this.overflow = this.currentSpace + space - limit;
		return false;
	}

	/**
	 * Returns a new instance of the reciever.
	 */
protected abstract LRUCache<K, V> newInstance(int size, int newOverflow);

/**
 * For testing purposes only
 */
public void printStats() {
	int forwardListLength = 0;
	LRUCacheEntry<K, V> entry = this.entryQueue;
	while(entry != null) {
		forwardListLength++;
		entry = entry.next;
	}
	System.out.println("Forward length: " + forwardListLength); //$NON-NLS-1$

	int backwardListLength = 0;
	entry = this.entryQueueTail;
	while(entry != null) {
		backwardListLength++;
		entry = entry.previous;
	}
	System.out.println("Backward length: " + backwardListLength); //$NON-NLS-1$

	class Temp {
		public Class<?> clazz;
		public int count;
		public Temp(Class<?> aClass) {
			this.clazz = aClass;
			this.count = 1;
		}
		@Override
		public String toString() {
			return "Class: " + this.clazz + " has " + this.count + " entries."; //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-1$
		}
	}
	HashMap<Class<?>, Temp> h = new HashMap<>();
	for (K k : this.entryTable.keySet()) {
		entry = this.entryTable.get(k);
		Class<?> key = entry.value.getClass();
		Temp t = h.get(key);
		if (t == null) {
			h.put(key, new Temp(key));
		} else {
			t.count++;
		}
	}
	for (Temp temp : h.values()) {
		System.out.println(temp);
	}
}

	/**
	 *	Removes the entry from the entry queue.
	 *	Calls <code>privateRemoveEntry</code> with the external functionality enabled.
	 *
	 * @param shuffle indicates whether we are just shuffling the queue
	 * (in which case, the entry table is not modified).
	 */
	@Override
	protected void privateRemoveEntry (LRUCacheEntry<K, V> entry, boolean shuffle) {
		privateRemoveEntry(entry, shuffle, true);
	}
/**
 *	Removes the entry from the entry queue.  If <i>external</i> is true, the entry is removed
 *	without checking if it can be removed.  It is assumed that the client has already closed
 *	the element it is trying to remove (or will close it promptly).
 *
 *	If <i>external</i> is false, and the entry could not be closed, it is not removed and the
 *	pointers are not changed.
 *
 *	@param shuffle indicates whether we are just shuffling the queue
 *	(in which case, the entry table is not modified).
 */
protected void privateRemoveEntry(LRUCacheEntry<K, V> entry, boolean shuffle, boolean external) {

	if (!shuffle) {
		if (external) {
			this.entryTable.remove(entry.key);
			this.currentSpace -= entry.space;
		} else {
			if (!close(entry)) return;
			// buffer close will recursively call #privateRemoveEntry with external==true
			// thus entry will already be removed if reaching this point.
			if (this.entryTable.get(entry.key) == null){
				return;
			} else {
				// basic removal
				this.entryTable.remove(entry.key);
				this.currentSpace -= entry.space;
			}
		}
	}
	LRUCacheEntry<K, V> previous = entry.previous;
	LRUCacheEntry<K, V> next = entry.next;

	/* if this was the first entry */
	if (previous == null) {
		this.entryQueue = next;
	} else {
		previous.next = next;
	}
	/* if this was the last entry */
	if (next == null) {
		this.entryQueueTail = previous;
	} else {
		next.previous = previous;
	}
}

	@Override
	public V put(K key, V value) {
		/* attempt to rid ourselves of the overflow, if there is any */
		if (this.overflow > 0)
			shrink();

		/* Check whether there's an entry in the cache */
		int newSpace = spaceFor(value);
		LRUCacheEntry<K, V> entry = this.entryTable.get (key);

		if (entry != null) {

			/**
			 * Replace the entry in the cache if it would not overflow
			 * the cache.  Otherwise flush the entry and re-add it so as
			 * to keep cache within budget
			 */
			int oldSpace = entry.space;
			int newTotal = this.currentSpace - oldSpace + newSpace;
			if (newTotal <= this.spaceLimit) {
				updateTimestamp (entry);
				entry.value = value;
				entry.space = newSpace;
				this.currentSpace = newTotal;
				this.overflow = 0;
				return value;
			} else {
				privateRemoveEntry (entry, false, false);
			}
		}

		// attempt to make new space
		makeSpace(newSpace);

		// add without worring about space, it will
		// be handled later in a makeSpace call
		privateAdd (key, value, newSpace);

		return value;
	}
	/**
	 * Removes and returns the value in the cache for the given key.
	 * If the key is not in the cache, returns null.
	 *
	 * @param key Key of object to remove from cache.
	 * @return Value removed from cache.
	 */
	public V remove(K key) {
		return removeKey(key);
	}
/**
 * Sets the load factor for the cache.  The load factor determines how
 * much space is reclaimed when the cache exceeds its space limit.
 * @param newLoadFactor double
 * @throws IllegalArgumentException when the new load factor is not in (0.0, 1.0]
 */
public void setLoadFactor(double newLoadFactor) throws IllegalArgumentException {
	if(newLoadFactor <= 1.0 && newLoadFactor > 0.0)
		this.loadFactor = newLoadFactor;
	else
		throw new IllegalArgumentException(Messages.cache_invalidLoadFactor);
}

	@Override
	public void setSpaceLimit(int limit) {
		if (limit < this.spaceLimit) {
			makeSpace(this.spaceLimit - limit);
		}
		this.spaceLimit = limit;
	}
	/**
	 * Attempts to shrink the cache if it has overflown.
	 * Returns true if the cache shrinks to less than or equal to <code>fSpaceLimit</code>.
	 */
	public boolean shrink() {
		if (this.overflow > 0)
			return makeSpace(0);
		return true;
	}
/**
 * Returns a String that represents the value of this object.  This method
 * is for debugging purposes only.
 */
@Override
public String toString() {
	return
		toStringFillingRation("OverflowingLRUCache ") + //$NON-NLS-1$
		toStringContents();
}
/**
 * Updates the timestamp for the given entry, ensuring that the queue is
 * kept in correct order.  The entry must exist.
 *
 * <p>This method will do nothing if timestamps have been disabled.
 */
@Override
protected void updateTimestamp(LRUCacheEntry<K, V> entry) {
	if (this.timestampsOn) {
		entry.timestamp = this.timestampCounter++;
		if (this.entryQueue != entry) {
			this.privateRemoveEntry(entry, true);
			privateAddEntry(entry, true);
		}
	}
}
}
