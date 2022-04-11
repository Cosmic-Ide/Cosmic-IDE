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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Contributions for
 *     						Bug 473178
 *******************************************************************************/
package org.eclipse.jdt.internal.core.dom.rewrite.imports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MoveSourceEdit;
import org.eclipse.text.edits.MoveTargetEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

/**
 * Creates TextEdits to apply changes to the order of import declarations to a compilation unit.
 */
final class ImportEditor {
	/**
	 * Iterates through the compilation unit's original import order, providing in turn each
	 * original import and the original start position of that import's leading delimiter.
	 */
	private static final class OriginalImportsCursor {
		private final Iterator<OriginalImportEntry> originalImportIterator;
		OriginalImportEntry currentOriginalImport;
		int currentPosition;

		OriginalImportsCursor(int startPosition, Collection<OriginalImportEntry> originalImportEntries) {
			this.originalImportIterator = originalImportEntries.iterator();
			this.currentPosition = startPosition;
			this.currentOriginalImport =
					this.originalImportIterator.hasNext() ? this.originalImportIterator.next() : null;
		}

		/**
		 * Advances this cursor to the next import in the original order.
		 */
		void advance() {
			IRegion declarationAndComments = this.currentOriginalImport.declarationAndComments;
			this.currentPosition = declarationAndComments.getOffset() + declarationAndComments.getLength();
			this.currentOriginalImport =
					this.originalImportIterator.hasNext() ? this.originalImportIterator.next() : null;
		}
	}

	private static final class ImportEdits {
		final Collection<TextEdit> leadingDelimiterEdits;
		final Collection<TextEdit> commentAndDeclarationEdits;

		ImportEdits(
				Collection<TextEdit> leadingDelimiterEdits,
				Collection<TextEdit> commentAndDeclarationEdits) {
			this.leadingDelimiterEdits = leadingDelimiterEdits;
			this.commentAndDeclarationEdits = commentAndDeclarationEdits;
		}
	}

	/**
	 * Maps by identity each import (as key), except the last, to the import (as value) which comes
	 * before it.
	 * <p>
	 * Maps by identity (rather than by hashcode) to handle cases of duplicate import declarations.
	 */
	private static Map<ImportName, ImportEntry> mapPrecedingImports(Collection<? extends ImportEntry> importEntries) {
		Map<ImportName, ImportEntry> precedingImports =
				new IdentityHashMap<>(importEntries.size());

		ImportEntry previousImport = null;
		for (ImportEntry currentImport : importEntries) {
			ImportName currentImportName = currentImport.importName;
			precedingImports.put(currentImportName, previousImport);
			previousImport = currentImport;
		}

		return precedingImports;
	}

	private static boolean containsFloatingComment(Iterable<ImportComment> comments) {
		for (ImportComment comment : comments) {
			if (comment.succeedingLineDelimiters > 1) {
				return true;
			}
		}

		return false;
	}

	private final String lineDelimiter;
	private final String twoLineDelimiters;
	private final boolean fixAllLineDelimiters;
	private final int lineDelimitersBetweenImportGroups;
	private final ImportGroupComparator importGroupComparator;
	private final RemovedImportCommentReassigner commentReassigner;
	private final Map<ImportName, ImportEntry> originalPrecedingImports;
	private final List<OriginalImportEntry> originalImportEntries;
	private final RewriteSite rewriteSite;
	private final ImportDeclarationWriter declarationWriter;

	/**
	 * @param lineDelimiter
	 *            the string to use as a line delimiter when generating text edits
	 * @param fixAllLineDelimiters
	 *            specifies whether to standardize whitespace between all imports (if true), or only
	 *            between pairs of imports not originally subsequent (if false)
	 * @param lineDelimitersBetweenImportGroups
	 *            the number of line delimiters desired between import declarations matching
	 *            different import groups
	 * @param importGroupComparator
	 *            used to determine whether two subsequent imports match the same import group
	 * @param originalImports
	 *            the original order of imports in the compilation unit
	 * @param rewriteSite
	 *            describes the location in the compilation unit where imports shall be rewriten
	 * @param importDeclarationWriter
	 *            used to render each new import declaration (one not originally present in the
	 *            compilation unit) as a string
	 */
	ImportEditor(
			String lineDelimiter,
			boolean fixAllLineDelimiters,
			int lineDelimitersBetweenImportGroups,
			ImportGroupComparator importGroupComparator,
			List<OriginalImportEntry> originalImports,
			RewriteSite rewriteSite,
			ImportDeclarationWriter importDeclarationWriter) {
		this.lineDelimiter = lineDelimiter;
		this.twoLineDelimiters = this.lineDelimiter.concat(this.lineDelimiter);
		this.fixAllLineDelimiters = fixAllLineDelimiters;
		this.lineDelimitersBetweenImportGroups = lineDelimitersBetweenImportGroups;
		this.importGroupComparator = importGroupComparator;
		this.originalImportEntries = originalImports;
		this.rewriteSite = rewriteSite;
		this.declarationWriter = importDeclarationWriter;

		this.commentReassigner = new RemovedImportCommentReassigner(originalImports);

		if (fixAllLineDelimiters) {
			this.originalPrecedingImports = Collections.emptyMap();
		} else {
			this.originalPrecedingImports = Collections.unmodifiableMap(mapPrecedingImports(originalImports));
		}
	}

	/**
	 * Generates and returns a TextEdit to replace or update the import declarations in the
	 * compilation unit to match the given list.
	 * <p>
	 * Standardizes whitespace between subsequent imports to the correct number of line delimiters
	 * (either for every pair of subsequent imports, or only for pairs not originally subsequent,
	 * depending on the value of {@link #fixAllLineDelimiters}).
	 * <p>
	 * Relocates leading and trailing comments of removed imports as determined by
	 * {@link #commentReassigner}.
	 */
	TextEdit createTextEdit(Collection<ImportEntry> resultantImports) {
		TextEdit edit = new MultiTextEdit();

		IRegion surroundingRegion = this.rewriteSite.surroundingRegion;

		if (resultantImports.isEmpty()) {
			if (this.originalImportEntries.isEmpty()) {
				// Leave the compilation unit as is.
			}
			else {
				// Replace original imports and surrounding whitespace with enough line delimiters
				// around preceding and/or succeeding elements.

				String newWhitespace;
				if (this.rewriteSite.hasPrecedingElements) {
					int newDelims = this.rewriteSite.hasSucceedingElements ? 2 : 1;
					newWhitespace = createDelimiter(newDelims);
				} else {
					newWhitespace = ""; //$NON-NLS-1$
				}

				edit.addChild(new ReplaceEdit(
						surroundingRegion.getOffset(), surroundingRegion.getLength(), newWhitespace));
			}
		}
		else {
			if (this.originalImportEntries.isEmpty()) {
				// Replace existing whitespace with preceding line delimiters, import declarations,
				// and succeeding line delimiters.

				Collection<TextEdit> importEdits = determineEditsForImports(
						surroundingRegion, resultantImports);

				if (this.rewriteSite.hasPrecedingElements) {
					edit.addChild(new InsertEdit(surroundingRegion.getOffset(), createDelimiter(2)));
				}

				edit.addChildren(importEdits.toArray(new TextEdit[importEdits.size()]));

				int newSucceedingDelims = this.rewriteSite.hasSucceedingElements ? 2 : 1;
				String newSucceeding = createDelimiter(newSucceedingDelims);
				edit.addChild(new InsertEdit(surroundingRegion.getOffset(), newSucceeding));
			}
			else {
				// Replace original imports with new ones, leaving surrounding whitespace in place.

				Collection<TextEdit> importEdits = determineEditsForImports(
						this.rewriteSite.importsRegion, resultantImports);

				edit.addChildren(importEdits.toArray(new TextEdit[importEdits.size()]));
			}
		}
		return edit;
	}

	/**
	 * Concatenates the given number of line delimiters into a single string.
	 */
	private String createDelimiter(int numberOfLineDelimiters) {
		if (numberOfLineDelimiters < 1) {
			throw new IllegalArgumentException();
		}

		if (numberOfLineDelimiters == 1) {
			return this.lineDelimiter;
		}

		if (numberOfLineDelimiters == 2) {
			return this.twoLineDelimiters;
		}

		StringBuilder correctDelimiter = new StringBuilder();
		for (int i = 0; i < numberOfLineDelimiters; i++) {
			correctDelimiter.append(this.lineDelimiter);
		}
		return correctDelimiter.toString();
	}

	private Collection<TextEdit> determineEditsForImports(
			IRegion importsRegion,
			Collection<ImportEntry> resultantImports) {
		Collection<TextEdit> edits = new ArrayList<>();

		Map<ImportEntry, Collection<ImportComment>> commentReassignments =
				this.commentReassigner.reassignComments(resultantImports);

		OriginalImportsCursor cursor = new OriginalImportsCursor(
				importsRegion.getOffset(), this.originalImportEntries);

		edits.addAll(placeResultantImports(cursor, resultantImports, commentReassignments));

		edits.addAll(deleteRemainingText(importsRegion, edits));

		// Omit the RangeMarkers used temporarily to mark the text of non-relocated imports.
		Collection<TextEdit> editsWithoutRangeMarkers = new ArrayList<>(edits.size());
		for (TextEdit edit : edits) {
			if (!(edit instanceof RangeMarker)) {
				editsWithoutRangeMarkers.add(edit);
			}
		}

		return editsWithoutRangeMarkers;
	}

	/**
	 * Creates TextEdits that place each resultant import in the correct (rewritten) position.
	 */
	private Collection<TextEdit> placeResultantImports(
			OriginalImportsCursor cursor,
			Collection<ImportEntry> resultantImports,
			Map<ImportEntry, Collection<ImportComment>> commentReassignments) {
		Collection<TextEdit> edits = new ArrayList<>();

		ImportEntry lastResultantImport = null;
		for (ImportEntry currentResultantImport : resultantImports) {
			if (currentResultantImport.isOriginal()) {
				// Skip forward to this import's place in the original order.
				while (cursor.currentOriginalImport != null
						&& cursor.currentOriginalImport != currentResultantImport) {
					cursor.advance();
				}
			}

			Collection<ImportComment> reassignedComments = commentReassignments.get(currentResultantImport);
			if (reassignedComments == null) {
				reassignedComments = Collections.emptyList();
			}

			ImportEdits importPlacement;
			if (currentResultantImport.isOriginal()) {
				OriginalImportEntry originalImport = currentResultantImport.asOriginalImportEntry();
				if (cursor.currentOriginalImport == currentResultantImport) {
					importPlacement = preserveStationaryImport(originalImport);
				} else {
					importPlacement = moveOriginalImport(originalImport, cursor.currentPosition);
				}
			} else {
				importPlacement = placeNewImport(currentResultantImport, cursor.currentPosition);
			}

			String newDelimiter = determineNewDelimiter(
					lastResultantImport, currentResultantImport, reassignedComments);
			if (newDelimiter == null) {
				edits.addAll(importPlacement.leadingDelimiterEdits);
			} else if (!newDelimiter.isEmpty()) {
				edits.add(new InsertEdit(cursor.currentPosition, newDelimiter));
			}

			if (!reassignedComments.isEmpty()) {
				edits.addAll(relocateComments(reassignedComments, cursor.currentPosition));

				boolean hasFloatingComment = currentResultantImport.isOriginal()
						&& containsFloatingComment(currentResultantImport.asOriginalImportEntry().comments);
				String delimiterAfterReassignedComments =
						hasFloatingComment ? this.twoLineDelimiters : this.lineDelimiter;
				edits.add(new InsertEdit(cursor.currentPosition, delimiterAfterReassignedComments));
			}

			edits.addAll(importPlacement.commentAndDeclarationEdits);

			if (currentResultantImport == cursor.currentOriginalImport) {
				cursor.advance();
			}

			lastResultantImport = currentResultantImport;
		}

		return edits;
	}

	/**
	 * Creates text edits to insert the text of a new import.
	 */
	private ImportEdits placeNewImport(ImportEntry currentResultantImport, int position) {
		String declaration = this.declarationWriter.writeImportDeclaration(currentResultantImport.importName);
		return new ImportEdits(
				Collections.<TextEdit>emptySet(),
				Collections.<TextEdit>singleton(new InsertEdit(position, declaration)));
	}

	/**
	 * Creates text edits to move an import's text to a new position.
	 */
	private ImportEdits moveOriginalImport(OriginalImportEntry importEntry, int position) {
		MoveSourceEdit leadingSourceEdit = new MoveSourceEdit(
				importEntry.leadingDelimiter.getOffset(), importEntry.leadingDelimiter.getLength());
		MoveTargetEdit leadingTargetEdit = new MoveTargetEdit(position, leadingSourceEdit);
		Collection<TextEdit> leadingDelimiterEdits = Arrays.asList(leadingSourceEdit, leadingTargetEdit);

		MoveSourceEdit importSourceEdit = new MoveSourceEdit(
				importEntry.declarationAndComments.getOffset(), importEntry.declarationAndComments.getLength());
		MoveTargetEdit importTargetEdit = new MoveTargetEdit(position, importSourceEdit);
		Collection<TextEdit> declarationAndCommentEdits = Arrays.asList(importSourceEdit, importTargetEdit);

		return new ImportEdits(leadingDelimiterEdits, declarationAndCommentEdits);
	}

	/**
	 * Creates RangeMarkers to mark a non-relocated import's text to prevent its deletion.
	 */
	private ImportEdits preserveStationaryImport(OriginalImportEntry importEntry) {
		return new ImportEdits(
				Collections.<TextEdit>singleton(new RangeMarker(
						importEntry.leadingDelimiter.getOffset(),
						importEntry.leadingDelimiter.getLength())),
				Collections.<TextEdit>singleton(new RangeMarker(
						importEntry.declarationAndComments.getOffset(),
						importEntry.declarationAndComments.getLength())));

	}

	/**
	 * Determines whether and how to standardize the whitespace between the end of the previous
	 * import (or its last trailing comment) and the start of the current import (or its first
	 * leading comment).
	 * <p>
	 * Returns a string containing the correct whitespace to place between the two imports, or
	 * {@code null} if the current import's original leading whitespace should be preserved.
	 */
	private String determineNewDelimiter(
			ImportEntry lastImport,
			ImportEntry currentImport,
			Collection<ImportComment> reassignedComments) {
		if (lastImport == null) {
			// The first import in the compilation unit needs no preceding line delimiters.
			return ""; //$NON-NLS-1$
		}

		boolean hasReassignedComments = !reassignedComments.isEmpty();

		if (!needsStandardDelimiter(lastImport, currentImport, hasReassignedComments)) {
			return null;
		}

		int numberOfLineDelimiters = 1;

		Collection<ImportComment> leadingComments;
		if (hasReassignedComments) {
			leadingComments = reassignedComments;
		} else if (currentImport.isOriginal()) {
			leadingComments = currentImport.asOriginalImportEntry().comments;
		} else {
			leadingComments = Collections.emptyList();
		}
		if (containsFloatingComment(leadingComments)) {
			// Prevent a floating leading comment from becoming attached to the preceding import.
			numberOfLineDelimiters = 2;
		}

		if (this.importGroupComparator.compare(lastImport.importName, currentImport.importName) != 0) {
			// Separate imports belonging to different import groups.
			numberOfLineDelimiters = Math.max(numberOfLineDelimiters, this.lineDelimitersBetweenImportGroups);
		}

		String standardDelimiter = createDelimiter(numberOfLineDelimiters);

		// Reuse the original preceding delimiter if it matches the standard delimiter, but only
		// if there are no reassigned comments (which would necessitate relocating the delimiter).
		if (currentImport.isOriginal() && !hasReassignedComments) {
			OriginalImportEntry originalImport = currentImport.asOriginalImportEntry();
			IRegion originalDelimiter = originalImport.leadingDelimiter;
			if (originalImport.precedingLineDelimiters == numberOfLineDelimiters) {
				boolean delimiterIsSameLength = originalDelimiter == null && standardDelimiter.isEmpty()
						|| originalDelimiter != null && originalDelimiter.getLength() == standardDelimiter.length();
				if (delimiterIsSameLength) {
					return null;
				}
			}
		}

		return standardDelimiter;
	}

	/**
	 * Determines whether the whitespace between two subsequent imports should be set to a standard
	 * number of line delimiters.
	 */
	private boolean needsStandardDelimiter(
			ImportEntry lastImport,
			ImportEntry currentImport,
			boolean hasReassignedComments) {
		boolean needsStandardDelimiter = false;

		if (this.fixAllLineDelimiters) {
			// In "Organize Imports" mode, all delimiters between imports are standardized.
			needsStandardDelimiter = true;
		} else if (!currentImport.isOriginal()) {
			// This (new) import does not have an original leading delimiter.
			needsStandardDelimiter = true;
		} else if (hasReassignedComments) {
			// Comments reassigned from removed imports are being prepended to this import.
			needsStandardDelimiter = true;
		} else {
			ImportEntry originalPrecedingImport = this.originalPrecedingImports.get(currentImport.importName);
			if (originalPrecedingImport == null || lastImport.importName != originalPrecedingImport.importName) {
				// This import follows a different import post-rewrite than pre-rewrite.
				needsStandardDelimiter = true;
			}
		}

		return needsStandardDelimiter;
	}

	private Collection<TextEdit> relocateComments(Collection<ImportComment> reassignedComments, int insertPosition) {
		if (reassignedComments.isEmpty()) {
			return Collections.emptyList();
		}

		Collection<TextEdit> edits = new ArrayList<>(reassignedComments.size() * 3);

		ImportComment lastComment = null;
		for (ImportComment currentComment : reassignedComments) {
			MoveSourceEdit sourceEdit = new MoveSourceEdit(
					currentComment.region.getOffset(), currentComment.region.getLength());
			edits.add(sourceEdit);

			if (lastComment != null)  {
				// Preserve blank lines between comments.
				int succeedingLineDelimiters = lastComment.succeedingLineDelimiters > 1 ? 2 : 1;

				edits.add(new InsertEdit(insertPosition, createDelimiter(succeedingLineDelimiters)));
			}

			edits.add(new MoveTargetEdit(insertPosition, sourceEdit));

			lastComment = currentComment;
		}

		return edits;
	}

	/**
	 * Creates TextEdits that delete text remaining between and after resultant imports.
	 */
	private static Collection<TextEdit> deleteRemainingText(IRegion importRegion, Collection<TextEdit> edits) {
		List<TextEdit> sortedEdits = new ArrayList<>(edits);
		Collections.sort(sortedEdits, new Comparator<TextEdit>() {
			@Override
			public int compare(TextEdit o1, TextEdit o2) {
				return o1.getOffset() - o2.getOffset();
			}
		});

		int deletePosition = importRegion.getOffset();

		Collection<TextEdit> deleteRemainingTextEdits = new ArrayList<>();
		for (TextEdit edit : sortedEdits) {
			if (edit.getOffset() > deletePosition) {
				deleteRemainingTextEdits.add(new DeleteEdit(deletePosition, edit.getOffset() - deletePosition));
			}

			int editEndPosition = edit.getOffset() + edit.getLength();
			deletePosition = Math.max(deletePosition, editEndPosition);
		}

		// Delete text remaining after the last import.
		int importRegionEndPosition = importRegion.getOffset() + importRegion.getLength();
		if (deletePosition < importRegionEndPosition) {
			deleteRemainingTextEdits.add(new DeleteEdit(deletePosition, importRegionEndPosition - deletePosition));
		}

		return deleteRemainingTextEdits;
	}
}