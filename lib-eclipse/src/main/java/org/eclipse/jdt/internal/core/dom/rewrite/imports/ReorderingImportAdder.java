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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Totally sorts new and existing imports together, discarding the order of existing imports
 * and omitting duplicate entries.
 */
final class ReorderingImportAdder implements ImportAdder {
	private final Comparator<ImportName> importComparator;

	ReorderingImportAdder(Comparator<ImportName> importComparator) {
		this.importComparator = importComparator;
	}

	@Override
	public List<ImportName> addImports(Collection<ImportName> existingImports, Collection<ImportName> importsToAdd) {
		int setCapacity = 2 * (existingImports.size() + importsToAdd.size());
		Set<ImportName> uniqueImportsWithAdditions = new HashSet<ImportName>(setCapacity);
		uniqueImportsWithAdditions.addAll(existingImports);
		uniqueImportsWithAdditions.addAll(importsToAdd);

		List<ImportName> sortedImports = new ArrayList<>(uniqueImportsWithAdditions);
		Collections.sort(sortedImports, this.importComparator);

		return sortedImports;
	}
}
