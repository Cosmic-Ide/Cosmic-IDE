/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler.util;

import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;

public final class HashtableOfPackage<P extends PackageBinding> extends CharDelegateMap<P> {
	public HashtableOfPackage() {
		// usually not very large:
		this(0); // Size 0 maps have faster get() because no hashing needed at all.
		// CAUTION THIS VALUES DEPEND ON THE WORKSPACE:
		// Statistics from a (!) large workspace:
		// ~100 times more get() then put() operations.
		// ~50% of all maps have 0 elements when finalized.
		// average size: 3 elements
		// average size of non empty: 5 elements
		// average key length: 8 chars
	}

	public HashtableOfPackage(int size) {
		super(size);
	}
}
