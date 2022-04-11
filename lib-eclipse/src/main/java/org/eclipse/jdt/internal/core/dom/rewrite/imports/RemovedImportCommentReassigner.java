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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Reassigns comments associated with removed imports (those present before but not present
 * after the rewrite) to resultant imports (those present after the rewrite).
 * <p>
 * Reassigns comments of removed single imports to the first (in iteration order) resultant
 * on-demand import having the same container name, if one exists.
 * <p>
 * Reassigns comments of removed on-demand imports to the first (in iteration order) resultant
 * single import having the same container name, if one exists.
 * <p>
 * Leaves unassigned any removed import comment not matching the above cases.
 */
final class RemovedImportCommentReassigner {
	private static Collection<OriginalImportEntry> retainImportsWithComments(Collection<OriginalImportEntry> imports) {
		Collection<OriginalImportEntry> importsWithComments = new ArrayList<OriginalImportEntry>(imports.size());
		for (OriginalImportEntry currentImport : imports) {
			if (!currentImport.comments.isEmpty()) {
				importsWithComments.add(currentImport);
			}
		}

		return importsWithComments;
	}

	private static boolean hasFloatingComment(OriginalImportEntry nextAssignedImport) {
		for (ImportComment importComment : nextAssignedImport.comments) {
			if (importComment.succeedingLineDelimiters > 1) {
				return true;
			}
		}

		return false;
	}

	private final Collection<OriginalImportEntry> originalImportsWithComments;

	RemovedImportCommentReassigner(List<OriginalImportEntry> originalImports) {
		this.originalImportsWithComments = retainImportsWithComments(originalImports);
	}

	/**
	 * Assigns comments of removed import entries (those in {@code originalImports} but not in
	 * {@code resultantImports}) to resultant import entries.
	 * <p>
	 * Returns a map containing the resulting assignments, where each key is an element of
	 * {@code resultantImports} and each value is a collection of comments reassigned to that
	 * resultant import.
	 */
	Map<ImportEntry, Collection<ImportComment>> reassignComments(Collection<ImportEntry> resultantImports) {
		Map<ImportEntry, Collection<OriginalImportEntry>> importAssignments = assignRemovedImports(resultantImports);

		Map<ImportEntry, Collection<ImportComment>> commentAssignments =
				new HashMap<ImportEntry, Collection<ImportComment>>();

		for (Map.Entry<ImportEntry, Collection<OriginalImportEntry>> importAssignment : importAssignments.entrySet()) {
			ImportEntry targetImport = importAssignment.getKey();
			if (targetImport != null) {
				Deque<ImportComment> assignedComments = new ArrayDeque<ImportComment>();

				Collection<OriginalImportEntry> assignedImports = importAssignment.getValue();

				Iterator<OriginalImportEntry> nextAssignedImportIterator = assignedImports.iterator();
				if (nextAssignedImportIterator.hasNext()) {
					nextAssignedImportIterator.next();
				}

				Iterator<OriginalImportEntry> assignedImportIterator = assignedImports.iterator();
				while (assignedImportIterator.hasNext()) {
					OriginalImportEntry currentAssignedImport = assignedImportIterator.next();
					OriginalImportEntry nextAssignedImport =
							nextAssignedImportIterator.hasNext() ? nextAssignedImportIterator.next() : null;

					assignedComments.addAll(currentAssignedImport.comments);

					if (nextAssignedImport != null && hasFloatingComment(nextAssignedImport)) {
						// Ensure that a blank line separates this removed import's comments
						// from the next removed import's floating comments.
						ImportComment lastComment = assignedComments.removeLast();
						ImportComment lastCommentWithTrailingBlankLine = new ImportComment(lastComment.region, 2);
						assignedComments.add(lastCommentWithTrailingBlankLine);
					}
				}

				commentAssignments.put(targetImport, assignedComments);
			}
		}

		return commentAssignments;
	}

	private Map<ImportEntry, Collection<OriginalImportEntry>> assignRemovedImports(Collection<ImportEntry> imports) {
		Collection<OriginalImportEntry> removedImportsWithComments = identifyRemovedImportsWithComments(imports);
		if (removedImportsWithComments.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<ImportName, ImportEntry> firstSingleForOnDemand = identifyFirstSingleForEachOnDemand(imports);
		Map<ImportName, ImportEntry> firstOccurrences = identifyFirstOccurrenceOfEachImportName(imports);

		Map<ImportEntry, Collection<OriginalImportEntry>> removedImportsForRetainedImport =
				new HashMap<ImportEntry, Collection<OriginalImportEntry>>();
		for (ImportEntry retainedImport : imports) {
			removedImportsForRetainedImport.put(retainedImport, new ArrayList<OriginalImportEntry>());
		}
		// The null key will map to the removed imports not assigned to any import.
		removedImportsForRetainedImport.put(null, new ArrayList<OriginalImportEntry>());

		for (OriginalImportEntry removedImport : removedImportsWithComments) {
			ImportName removedImportName = removedImport.importName;

			final ImportEntry retainedImport;
			if (removedImportName.isOnDemand()) {
				retainedImport = firstSingleForOnDemand.get(removedImportName);
			} else {
				retainedImport = firstOccurrences.get(removedImportName.getContainerOnDemand());
			}

			// retainedImport will be null if there's no corresponding import to which to assign the removed import.
			removedImportsForRetainedImport.get(retainedImport).add(removedImport);
		}

		return removedImportsForRetainedImport;
	}

	private Collection<OriginalImportEntry> identifyRemovedImportsWithComments(Collection<ImportEntry> imports) {
		Collection<OriginalImportEntry> removedImports =
				new ArrayList<OriginalImportEntry>(this.originalImportsWithComments);
		removedImports.removeAll(imports);
		return removedImports;
	}

	/**
	 * Assigns each removed on-demand import to the first single import in {@code imports} having
	 * the same container name.
	 * <p>
	 * Returns a map where each key is a single import and each value is the corresponding
	 * removed on-demand import.
	 * <p>
	 * The returned map only contains mappings to removed on-demand imports for which there are
	 * corresponding single imports in {@code imports}.
	 */
	private Map<ImportName, ImportEntry> identifyFirstSingleForEachOnDemand(Iterable<ImportEntry> imports) {
		Map<ImportName, ImportEntry> firstSingleImportForContainer = new HashMap<ImportName, ImportEntry>();
		for (ImportEntry currentImport : imports) {
			if (!currentImport.importName.isOnDemand()) {
				ImportName containerOnDemand = currentImport.importName.getContainerOnDemand();
				if (!firstSingleImportForContainer.containsKey(containerOnDemand)) {
					firstSingleImportForContainer.put(containerOnDemand, currentImport);
				}
			}
		}
		return firstSingleImportForContainer;
	}

	private Map<ImportName, ImportEntry> identifyFirstOccurrenceOfEachImportName(Iterable<ImportEntry> imports) {
		Map<ImportName, ImportEntry> firstOccurrenceOfImport = new HashMap<ImportName, ImportEntry>();
		for (ImportEntry resultantImport : imports) {
			if (!firstOccurrenceOfImport.containsKey(resultantImport.importName)) {
				firstOccurrenceOfImport.put(resultantImport.importName, resultantImport);
			}
		}
		return firstOccurrenceOfImport;
	}
}
