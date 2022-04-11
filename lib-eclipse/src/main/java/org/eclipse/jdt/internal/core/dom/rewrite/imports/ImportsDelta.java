/*******************************************************************************
 * Copyright (c) 2015 Google Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     John Glassmyer <jogl@google.com> - import group sorting is broken - https://bugs.eclipse.org/430303
 *******************************************************************************/
package org.eclipse.jdt.internal.core.dom.rewrite.imports;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Encapsulates a set of imports to add and a set of imports to remove.
 */
final class ImportsDelta {
	final Set<ImportName> importsToAdd;
	final Set<ImportName> importsToRemove;

	ImportsDelta(Collection<ImportName> importsToAdd, Collection<ImportName> importsToRemove) {
		this.importsToAdd = Collections.unmodifiableSet(new HashSet<ImportName>(importsToAdd));
		this.importsToRemove = Collections.unmodifiableSet(new HashSet<ImportName>(importsToRemove));
	}

	@Override
	public String toString() {
		return String.format(
				"(additions: %s, removals: %s)", //$NON-NLS-1$
				this.importsToAdd,
				this.importsToRemove);
	}
}