/*******************************************************************************
 * Copyright (c) 2019 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

/**
 * Defines a wrapper type for {@link ReferenceBinding} and provides proper hashCode() and equals() based on the wrapped
 * {@link ReferenceBinding} object identity comparison.
 * <p>
 * Note: {@link ReferenceBinding} defines a hashCode() which is not consistent with the respective equals()
 * implementation).
 */
final class ReferenceBindingSetWrapper {

	final ReferenceBinding referenceBinding;
	private int hashCode;

	ReferenceBindingSetWrapper(ReferenceBinding referenceBinding) {
		this.referenceBinding = referenceBinding;
		this.hashCode = referenceBinding.identityHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof ReferenceBindingSetWrapper) {
			ReferenceBindingSetWrapper other = (ReferenceBindingSetWrapper) obj;
			return identityEqual(this.referenceBinding, other.referenceBinding);
		}
		return false;
	}

	private static boolean identityEqual(Object o1, Object o2) {
		return o1 == o2;
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public String toString() {
		return this.referenceBinding.toString();
	}
}
