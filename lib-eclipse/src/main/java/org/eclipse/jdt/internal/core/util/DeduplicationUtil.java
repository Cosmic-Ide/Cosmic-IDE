/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
 *     Joerg Kubitz    - refactoring
 *******************************************************************************/

package org.eclipse.jdt.internal.core.util;

/** Utility to provide deduplication by best effort. **/
public final class DeduplicationUtil {
	private DeduplicationUtil() {
	}

	private static final WeakHashSet<Object> objectCache = new WeakHashSet<>();
	private static final WeakHashSet<String> stringSymbols = new WeakHashSet<>();
	private static final WeakHashSetOfCharArray charArraySymbols = new WeakHashSetOfCharArray();

	@SuppressWarnings("unchecked")
	public static <T> T internObject(T obj) {
		synchronized (objectCache) {
			return (T) objectCache.add(obj);
		}
	}

	public static char[] intern(char[] array) {
		synchronized (charArraySymbols) {
			return charArraySymbols.add(array);
		}
	}

	/*
	 * Used as a replacement for String#intern() that could prevent garbage collection of strings on some VMs.
	 */
	public static String intern(String s) {
		synchronized (stringSymbols) {
			return stringSymbols.add(s);
		}
	}
}
