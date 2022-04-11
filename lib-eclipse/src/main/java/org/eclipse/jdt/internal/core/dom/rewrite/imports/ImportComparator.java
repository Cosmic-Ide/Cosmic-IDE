/*******************************************************************************
 * Copyright (c) 2014 Google Inc and others.
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

import java.util.Comparator;

/**
 * Sorts imports by, in order of decreasing precedence, the following:
 * <ul>
 * <li>configured import group order</li>
 * <li>package name and/or qualified name of containing type (lexicographically)</li>
 * <li>qualified name of import (lexicographically)</li>
 * </ul>
 */
final class ImportComparator implements Comparator<ImportName> {
	private static Comparator<ImportName> createQualifiedNameComparator() {
		return new Comparator<ImportName>() {
			@Override
			public int compare(ImportName o1, ImportName o2) {
				return o1.qualifiedName.compareTo(o2.qualifiedName);
			}
		};
	}

	private final Comparator<ImportName> importGroupComparator;
	private final Comparator<ImportName> typeContainerComparator;
	private final Comparator<ImportName> staticContainerComparator;
	private final Comparator<ImportName> qualifiedNameComparator;

	ImportComparator(
			ImportGroupComparator importGroupComparator,
			Comparator<ImportName> typeContainerComparator,
			Comparator<ImportName> staticContainerComparator) {
		this.importGroupComparator = importGroupComparator;
		this.typeContainerComparator = typeContainerComparator;
		this.staticContainerComparator = staticContainerComparator;
		this.qualifiedNameComparator = createQualifiedNameComparator();
	}

	@Override
	public int compare(ImportName o1, ImportName o2) {
		final int comparison;

		int importGroupComparison = this.importGroupComparator.compare(o1, o2);
		if (importGroupComparison != 0) {
			comparison = importGroupComparison;
		} else {
			// The two imports sorted into the same import group, so o2.isStatic == o1.isStatic.
			Comparator<ImportName> containerComparator =
					o1.isStatic ? this.staticContainerComparator : this.typeContainerComparator;

			int containerComparison = containerComparator.compare(o1, o2);
			if (containerComparison != 0) {
				comparison = containerComparison;
			} else {
				comparison = this.qualifiedNameComparator.compare(o1, o2);
			}
		}

		return comparison;
	}
}
