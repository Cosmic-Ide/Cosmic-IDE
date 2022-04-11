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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * Keeping existing imports in their existing order, inserts each new import before or after the
 * import to which it would be adjacent if all (existing and new) imports were totally ordered
 * together.
 * <p>
 * A new import that would sort between two existing imports which are not adjacent in the
 * existing order will be placed adjacent to the existing import with which it shares a longer
 * prefix of dot-separated name segments.
 */
final class OrderPreservingImportAdder implements ImportAdder {
	static class AdjacentImports {
		final Collection<ImportName> importsBefore = new ArrayList<ImportName>();
		final Collection<ImportName> importsAfter = new ArrayList<ImportName>();

		@Override
		public String toString() {
			return String.format("(%s, %s)", this.importsBefore.toString(), this.importsAfter.toString()); //$NON-NLS-1$
		}
	}

	/**
	 * Returns the number of prefixing dot-separated segments shared between the two names.
	 * <p>
	 * For example, {@code countMatchingPrefixSegments("foo.pack1.Class", "foo.pack2.Class")} will
	 * return 1 and {@code countMatchingPrefixSegments("foo.pack1.Class", "com.foo.pack1.Class")}
	 * will return 0.
	 */
	private static int countMatchingPrefixSegments(String name1, String name2) {
		if (name1.isEmpty() || name2.isEmpty()) {
			return 0;
		}

		int matchingSegments = 0;
		for (int i = 0; i <= name1.length() && i <= name2.length(); i++) {
			boolean atEndOfName1Segment = i == name1.length() || name1.charAt(i) == '.';
			boolean atEndOfName2Segment = i == name2.length() || name2.charAt(i) == '.';
			if (atEndOfName1Segment && atEndOfName2Segment) {
				matchingSegments++;
			} else if (atEndOfName1Segment || atEndOfName2Segment) {
				break;
			} else if (name1.charAt(i) != name2.charAt(i)) {
				break;
			}
		}

		return matchingSegments;
	}

	private final Comparator<ImportName> importComparator;

	OrderPreservingImportAdder(Comparator<ImportName> importComparator) {
		this.importComparator = importComparator;
	}

	@Override
	public List<ImportName> addImports(Collection<ImportName> existingImports, Collection<ImportName> importsToAdd) {
		if (importsToAdd.isEmpty()) {
			return new ArrayList<ImportName>(existingImports);
		}

		List<ImportName> sortedNewImports = new ArrayList<ImportName>(importsToAdd);
		sortedNewImports.removeAll(new HashSet<ImportName>(existingImports));
		Collections.sort(sortedNewImports, this.importComparator);

		if (existingImports.isEmpty()) {
			return sortedNewImports;
		}

		Map<ImportName, AdjacentImports> adjacentNewImports =
				determineAdjacentNewImports(new ArrayList<ImportName>(existingImports), sortedNewImports);

		List<ImportName> importsWithAdditions =
				new ArrayList<ImportName>(existingImports.size() + sortedNewImports.size());
		for (ImportName existingImport : existingImports) {
			// Remove the adjacent imports so they don't get inserted multiple times in the case
			// of duplicate imports.
			AdjacentImports adjacentImports = adjacentNewImports.remove(existingImport);

			if (adjacentImports != null) {
				importsWithAdditions.addAll(adjacentImports.importsBefore);
			}

			importsWithAdditions.add(existingImport);

			if (adjacentImports != null) {
				importsWithAdditions.addAll(adjacentImports.importsAfter);
			}
		}

		return importsWithAdditions;
	}

	/**
	 * Determines which new imports to place before and after each existing import.
	 * <p>
	 * Returns a Map where each key is an existing import and each corresponding value is an
	 * AdjacentImports containing those new imports which should be placed before and after that
	 * existing import. Each new import will be placed either before or after exactly one existing
	 * import.
	 *
	 * @param existingImports
	 *            Existing imports.
	 * @param sortedNewImports
	 *            Imports to be added. Must be in order as if sorted by this.importComparator.
	 */
	private Map<ImportName, AdjacentImports> determineAdjacentNewImports(
			Collection<ImportName> existingImports,
			Iterable<ImportName> sortedNewImports) {
		NavigableSet<ImportName> existingImportsTreeSet = new TreeSet<ImportName>(this.importComparator);
		existingImportsTreeSet.addAll(existingImports);

		Map<ImportName, AdjacentImports> adjacentNewImports = new HashMap<ImportName, AdjacentImports>();
		for (ImportName existingImport : existingImports) {
			adjacentNewImports.put(existingImport, new AdjacentImports());
		}

		for (ImportName newImport : sortedNewImports) {
			ImportName precedingExistingImport = existingImportsTreeSet.lower(newImport);
			ImportName succeedingExistingImport = existingImportsTreeSet.higher(newImport);

			if (shouldGroupWithSucceeding(newImport, precedingExistingImport, succeedingExistingImport)) {
				adjacentNewImports.get(succeedingExistingImport).importsBefore.add(newImport);
			} else {
				adjacentNewImports.get(precedingExistingImport).importsAfter.add(newImport);
			}
		}

		return adjacentNewImports;
	}

	/**
	 * Returns true if the new import should be placed before the existing import that would succeed
	 * it in sorted order, or false if the new import should be placed after the existing import
	 * that would precede it in sorted order.
	 */
	private boolean shouldGroupWithSucceeding(
			ImportName newImport, ImportName precedingExistingImport, ImportName succeedingExistingImport) {
		if (precedingExistingImport == null) {
			return true;
		} else if (succeedingExistingImport == null) {
			return false;
		} else {
			String containerName = newImport.containerName;

			int prefixSharedWithPreceding =
					countMatchingPrefixSegments(containerName, precedingExistingImport.containerName);

			int prefixSharedWithSucceeding =
					countMatchingPrefixSegments(containerName, succeedingExistingImport.containerName);

			return prefixSharedWithSucceeding > prefixSharedWithPreceding;
		}
	}
}