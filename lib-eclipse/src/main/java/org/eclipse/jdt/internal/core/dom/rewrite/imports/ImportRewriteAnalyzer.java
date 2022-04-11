/*******************************************************************************
 * Copyright (c) 2000, 2013, 2014, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *		IBM Corporation - initial API and implementation
 *		Stephan Herrmann - Contribution for Bug 378024 - Ordering of comments between imports not preserved
 *		John Glassmyer <jogl@google.com> - import group sorting is broken - https://bugs.eclipse.org/430303
 *******************************************************************************/
package org.eclipse.jdt.internal.core.dom.rewrite.imports;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.dom.rewrite.imports.ConflictIdentifier.Conflicts;
import org.eclipse.jdt.internal.core.util.Util;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.text.edits.TextEdit;

/**
 * Allows the caller to specify imports to be added to or removed from a compilation unit and
 * creates a TextEdit which, applied to the compilation unit, effects the specified additions and
 * removals.
 * <p>
 * Operates in either of two modes (selected via {@link ImportRewriteConfiguration.Builder}'s
 * static factory methods):
 * <ul>
 * <li>Discarding original imports and totally sorting all imports added thereafter. This mode is
 * used by the Organize Imports operation.</li>
 * <li>Preserving original imports and placing each added import adjacent to the one most closely
 * matching it. This mode is used e.g. when Content Assist adds an import for a completed name.</li>
 * </ul>
 */
public final class ImportRewriteAnalyzer {
	/**
	 * Encapsulates, for a computed import rewrite, a {@code TextEdit} that can be applied to effect
	 * the rewrite as well as the names of imports created by the rewrite.
	 */
	public static final class RewriteResult {
		private final TextEdit textEdit;
		private final Set<ImportName> createdImports;

		RewriteResult(TextEdit textEdit, Set<ImportName> createdImports) {
			this.textEdit = textEdit;
			this.createdImports = Collections.unmodifiableSet(createdImports);
		}

		/**
		 * Returns a {@link TextEdit} describing the changes necessary to perform the rewrite.
		 */
		public TextEdit getTextEdit() {
			return this.textEdit;
		}

		public String[] getCreatedImports() {
			return extractQualifiedNames(false, this.createdImports);
		}

		public String[] getCreatedStaticImports() {
			return extractQualifiedNames(true, this.createdImports);
		}

		private String[] extractQualifiedNames(boolean b, Collection<ImportName> imports) {
			List<String> names = new ArrayList<String>(imports.size());
			for (ImportName importName : imports) {
				if (importName.isStatic == b) {
					names.add(importName.qualifiedName);
				}
			}

			return names.toArray(new String[names.size()]);
		}
	}

	/**
	 * Returns the value of the formatter option specifying how many blank lines to insert between
	 * import groups.
	 */
	private static int getBlankLinesBetweenImportGroups(IJavaProject javaProject) {
		int num = -1;

		String blankLinesOptionValue =
				javaProject.getOption(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BETWEEN_IMPORT_GROUPS, true);
		try {
			num = Integer.parseInt(blankLinesOptionValue);
		} catch (NumberFormatException e) {
			String message = String.format(
					"Could not parse the value of %s as an integer: %s", //$NON-NLS-1$
					DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BETWEEN_IMPORT_GROUPS,
					blankLinesOptionValue);
			Util.log(new Status(IStatus.WARNING, JavaCore.PLUGIN_ID, message, e));
		}

		return num >= 0 ? num : 1;
	}

	/**
	 * Returns the value of the formatter option specifying whether to insert a space between the
	 * imported name and the semicolon in an import declaration.
	 */
	private static boolean shouldInsertSpaceBeforeSemicolon(IJavaProject javaProject) {
		return JavaCore.INSERT.equals(
				javaProject.getOption(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON, true));
	}

	/**
	 * Reads the positions of each existing import declaration along with any associated comments,
	 * and returns these in a list whose iteration order reflects the existing order of the imports
	 * in the compilation unit.
	 */
	private static List<OriginalImportEntry> readOriginalImports(CompilationUnit compilationUnit) {
		List<ImportDeclaration> importDeclarations = compilationUnit.imports();

		if (importDeclarations.isEmpty()) {
			return Collections.emptyList();
		}

		List<Comment> comments = compilationUnit.getCommentList();

		int currentCommentIndex = 0;

		// Skip over package and file header comments (see https://bugs.eclipse.org/121428).
		ImportDeclaration firstImport = importDeclarations.get(0);
		PackageDeclaration packageDeclaration = compilationUnit.getPackage();
		int firstImportStartPosition = packageDeclaration == null
				? firstImport.getStartPosition()
				: compilationUnit.getExtendedStartPosition(packageDeclaration)
						+ compilationUnit.getExtendedLength(packageDeclaration);
		while (currentCommentIndex < comments.size()
				&& comments.get(currentCommentIndex).getStartPosition() < firstImportStartPosition) {
			currentCommentIndex++;
		}

		List<OriginalImportEntry> imports = new ArrayList<OriginalImportEntry>(importDeclarations.size());
		int previousExtendedEndPosition = -1;
		for (ImportDeclaration currentImport : importDeclarations) {
			int extendedEndPosition = compilationUnit.getExtendedStartPosition(currentImport)
					+ compilationUnit.getExtendedLength(currentImport);

			int commentAfterImportIndex = currentCommentIndex;
			while (commentAfterImportIndex < comments.size()
					&& comments.get(commentAfterImportIndex).getStartPosition() < extendedEndPosition) {
				commentAfterImportIndex++;
			}

			List<ImportComment> importComments;
			if (commentAfterImportIndex == currentCommentIndex) {
				importComments = Collections.emptyList();
			} else {
				importComments = selectImportComments(
						compilationUnit,
						comments,
						currentImport.getStartPosition(),
						currentCommentIndex,
						commentAfterImportIndex);
			}

			int importAndCommentsStartPosition = importComments.isEmpty()
					? currentImport.getStartPosition()
							: Math.min(currentImport.getStartPosition(), importComments.get(0).region.getOffset());

			IRegion leadingWhitespaceRegion;
			int precedingLineDelimiters;
			if (previousExtendedEndPosition == -1) {
				leadingWhitespaceRegion = new Region(importAndCommentsStartPosition, 0);
				precedingLineDelimiters = 0;
			} else {
				leadingWhitespaceRegion = new Region(
						previousExtendedEndPosition, importAndCommentsStartPosition - previousExtendedEndPosition);
				int importAndCommentsFirstLine = compilationUnit.getLineNumber(importAndCommentsStartPosition);
				int lastLineOfPrevious = compilationUnit.getLineNumber(previousExtendedEndPosition - 1);
				precedingLineDelimiters = importAndCommentsFirstLine - lastLineOfPrevious;
			}
			IRegion importAndCommentsRegion =
					new Region(importAndCommentsStartPosition, extendedEndPosition - importAndCommentsStartPosition);

			imports.add(new OriginalImportEntry(
					ImportName.createFor(currentImport),
					importComments,
					precedingLineDelimiters,
					leadingWhitespaceRegion,
					importAndCommentsRegion));

			currentCommentIndex = commentAfterImportIndex;
			previousExtendedEndPosition = extendedEndPosition;
		}

		return imports;
	}

	private static List<ImportComment> selectImportComments(
			CompilationUnit compilationUnit,
			List<Comment> comments,
			int importDeclarationStartPosition,
			int commentStartIndex,
			int commentEndIndex) {
		List<ImportComment> importComments = new ArrayList<ImportComment>(comments.size());

		Iterator<Comment> commentIterator = comments.subList(commentStartIndex, commentEndIndex).iterator();
		Comment currentComment = commentIterator.hasNext() ? commentIterator.next() : null;
		while (currentComment != null) {
			int currentCommentStartPosition = currentComment.getStartPosition();
			int currentCommentLength = currentComment.getLength();

			Comment nextComment = commentIterator.hasNext() ? commentIterator.next() : null;

			int succeedingLineDelims;
			int nextCommentStartPosition = nextComment == null ? Integer.MAX_VALUE : nextComment.getStartPosition();
			int nextStartPosition = Math.min(importDeclarationStartPosition, nextCommentStartPosition);
			if (nextStartPosition == Integer.MAX_VALUE) {
				// This trailing comment is located at the end of the import's extended range
				// and we don't care how many line delimiters follow it.
				succeedingLineDelims = 0;
			} else {
				int currentCommentEndLine =
						compilationUnit.getLineNumber(currentCommentStartPosition + currentCommentLength);
				int nextStartLine = compilationUnit.getLineNumber(nextStartPosition);
				succeedingLineDelims = nextStartLine - currentCommentEndLine;
			}

			importComments.add(new ImportComment(
					new Region(currentCommentStartPosition, currentCommentLength), succeedingLineDelims));

			currentComment = nextComment;
		}

		return importComments;
	}

	private static RewriteSite determineRewriteSite(
			CompilationUnit compilationUnit, List<OriginalImportEntry> originalImports) throws JavaModelException {
		IRegion importsRegion = determineImportsRegion(originalImports);

		IRegion surroundingRegion = determineSurroundingRegion(compilationUnit, importsRegion);

		boolean hasPrecedingElements = surroundingRegion.getOffset() != 0;

		boolean hasSucceedingElements =
				surroundingRegion.getOffset() + surroundingRegion.getLength() != compilationUnit.getLength();

		return new RewriteSite(
				surroundingRegion,
				importsRegion,
				hasPrecedingElements,
				hasSucceedingElements);
	}

	/**
	 * Determines the region originally occupied by imports and their associated comments.
	 * <p>
	 * Returns null if originalImports is null or empty.
	 */
	private static IRegion determineImportsRegion(List<OriginalImportEntry> originalImports) {
		if (originalImports == null || originalImports.isEmpty()) {
			return null;
		}

		OriginalImportEntry firstImport = originalImports.get(0);
		int start = firstImport.declarationAndComments.getOffset();

		OriginalImportEntry lastImport = originalImports.get(originalImports.size() - 1);
		int end = lastImport.declarationAndComments.getOffset()
				+ lastImport.declarationAndComments.getLength();

		return new Region(start, end - start);
	}

	/**
	 * Determines the region to be occupied by imports, their associated comments, and surrounding
	 * whitespace.
	 */
	private static IRegion determineSurroundingRegion(CompilationUnit compilationUnit, IRegion importsRegion) throws JavaModelException {
		NavigableMap<Integer, ASTNode> nodesTreeMap = mapTopLevelNodes(compilationUnit);

		int surroundingStart;
		int positionAfterImports;
		if (importsRegion == null) {
			PackageDeclaration packageDeclaration = compilationUnit.getPackage();
			if (packageDeclaration != null) {
				surroundingStart = compilationUnit.getExtendedStartPosition(packageDeclaration)
						+ compilationUnit.getExtendedLength(packageDeclaration);
			}
			else {
				surroundingStart = 0;
			}

			positionAfterImports = surroundingStart;
		} else {
			Entry<Integer, ASTNode> lowerEntry = nodesTreeMap.lowerEntry(importsRegion.getOffset());
			if (lowerEntry != null) {
				ASTNode precedingNode = lowerEntry.getValue();
				surroundingStart = precedingNode.getStartPosition() + precedingNode.getLength();
			} else {
				surroundingStart = 0;
			}

			positionAfterImports = importsRegion.getOffset() + importsRegion.getLength();
		}

		int surroundingEnd = positionAfterImports;
		IBuffer buffer = compilationUnit.getTypeRoot().getBuffer();
		int length = buffer.getLength();
		while (surroundingEnd < length && Character.isWhitespace(buffer.getChar(surroundingEnd))) {
			surroundingEnd++;
		}

		return new Region(surroundingStart, surroundingEnd - surroundingStart);
	}

	/**
	 * Builds a NavigableMap containing all of the given compilation unit's top-level nodes
	 * (package declaration, import declarations, type declarations, and non-doc comments),
	 * keyed by start position.
	 */
	private static NavigableMap<Integer, ASTNode> mapTopLevelNodes(CompilationUnit compilationUnit) {
		NavigableMap<Integer, ASTNode> map = new TreeMap<Integer, ASTNode>();

		Collection<ASTNode> nodes = new ArrayList<ASTNode>();
		if (compilationUnit.getPackage() != null) {
			nodes.add(compilationUnit.getPackage());
		}
		nodes.addAll(compilationUnit.imports());
		nodes.addAll(compilationUnit.types());
		for (Comment comment : ((List<Comment>) compilationUnit.getCommentList())) {
			// Include only top-level (non-doc) comments;
			// doc comments are contained within their parent nodes' ranges.
			if (comment.getParent() == null) {
				nodes.add(comment);
			}
		}

		for (ASTNode node : nodes) {
			map.put(node.getStartPosition(), node);
		}

		return map;
	}

	/**
	 * Builds an {@code IdentityHashMap} having the elements of {@code imports} as values and each
	 * element's {@code importName} as corresponding key. This map can be used to recall the {@code
	 * ImportEntry} corresponding to a given {@code ImportName} instance even when there are
	 * duplicate import declarations (where multiple {@code ImportEntry}s have equal, but not
	 * identical, {@code ImportName}s).
	 */
	private static Map<ImportName, OriginalImportEntry> mapImportsByNameIdentity(List<OriginalImportEntry> imports) {
		Map<ImportName, OriginalImportEntry> importsByName = new IdentityHashMap<ImportName, OriginalImportEntry>();

		for (OriginalImportEntry currentImport : imports) {
			importsByName.put(currentImport.importName, currentImport);
		}

		return Collections.unmodifiableMap(importsByName);
	}

	/**
	 * Returns a new {@code List} containing those elements of {@code imports} (in their existing
	 * order) not contained in {@code importsToSubtract}.
	 */
	private static List<ImportName> subtractImports(
			Collection<ImportName> existingImports, Set<ImportName> importsToSubtract) {
		List<ImportName> remainingImports = new ArrayList<ImportName>(existingImports.size());
		for (ImportName existingImport : existingImports) {
			if (!importsToSubtract.contains(existingImport)) {
				remainingImports.add(existingImport);
			}
		}
		return remainingImports;
	}

	private final List<OriginalImportEntry> originalImportEntries;
	private final List<ImportName> originalImportsList;
	private final Set<ImportName> originalImportsSet;

	private final ImportDeclarationWriter importDeclarationWriter;

	private final ImportAdder importAdder;

	private final Set<ImportName> importsToAdd;
	private final Set<ImportName> importsToRemove;

	private final boolean reportAllResultantImportsAsCreated;

	private final Set<String> typeExplicitSimpleNames;
	private final Set<String> staticExplicitSimpleNames;

	private final Set<String> implicitImportContainerNames;

	private final ConflictIdentifier conflictIdentifier;

	private final OnDemandComputer onDemandComputer;

	private final Map<ImportName, OriginalImportEntry> importsByNameIdentity;

	private final String lineDelimiter;

	private final ImportEditor importEditor;

	public ImportRewriteAnalyzer(
			ICompilationUnit cu,
			CompilationUnit astRoot,
			ImportRewriteConfiguration configuration) throws JavaModelException {
		this.originalImportEntries = Collections.unmodifiableList(readOriginalImports(astRoot));

		List<ImportName> importsList = new ArrayList<ImportName>(this.originalImportEntries.size());
		Set<ImportName> importsSet = new HashSet<ImportName>();
		for (ImportEntry originalImportEntry : this.originalImportEntries) {
			ImportName importName = originalImportEntry.importName;
			importsList.add(importName);
			importsSet.add(importName);
		}
		this.originalImportsList = Collections.unmodifiableList(importsList);
		this.originalImportsSet = Collections.unmodifiableSet(importsSet);

		this.importsToAdd = new LinkedHashSet<ImportName>();

		this.importsToRemove = new LinkedHashSet<ImportName>();
		if (configuration.originalImportHandling.shouldRemoveOriginalImports()) {
			this.importsToRemove.addAll(importsSet);
			this.reportAllResultantImportsAsCreated = true;
		} else {
			this.reportAllResultantImportsAsCreated = false;
		}

		this.typeExplicitSimpleNames = new HashSet<String>();
		this.staticExplicitSimpleNames = new HashSet<String>();

		ImportGroupComparator importGroupComparator = new ImportGroupComparator(configuration.importOrder);

		JavaProject javaProject = (JavaProject) cu.getJavaProject();

		this.importAdder = configuration.originalImportHandling.createImportAdder(new ImportComparator(
				importGroupComparator,
				configuration.typeContainerSorting.createContainerComparator(javaProject),
				configuration.staticContainerSorting.createContainerComparator(javaProject)));

		this.implicitImportContainerNames =
				configuration.implicitImportIdentification.determineImplicitImportContainers(cu);

		this.onDemandComputer = new OnDemandComputer(
				configuration.typeOnDemandThreshold,
				configuration.staticOnDemandThreshold);

		this.conflictIdentifier = new ConflictIdentifier(
				this.onDemandComputer,
				new TypeConflictingSimpleNameFinder(javaProject, new SearchEngine()),
				new StaticConflictingSimpleNameFinder(javaProject),
				this.implicitImportContainerNames);

		this.importsByNameIdentity = mapImportsByNameIdentity(this.originalImportEntries);

		this.importDeclarationWriter = new ImportDeclarationWriter(shouldInsertSpaceBeforeSemicolon(javaProject));

		this.lineDelimiter = cu.findRecommendedLineSeparator();

		this.importEditor = new ImportEditor(
				this.lineDelimiter,
				configuration.originalImportHandling.shouldFixAllLineDelimiters(),
				getBlankLinesBetweenImportGroups(javaProject) + 1,
				importGroupComparator,
				this.originalImportEntries,
				determineRewriteSite(astRoot, this.originalImportEntries),
				this.importDeclarationWriter);
	}

	/**
	 * Specifies that applying the rewrite should result in the compilation unit containing the
	 * specified import.
	 * <p>
	 * Has no effect if the compilation unit otherwise would contain the given import.
	 * <p>
	 * Overrides any previous corresponding call to {@link #removeImport}.
	 */
	public void addImport(boolean isStatic, String qualifiedName) {
		ImportName importToAdd = ImportName.createFor(isStatic, qualifiedName);
		this.importsToAdd.add(importToAdd);
		this.importsToRemove.remove(importToAdd);
	}

	/**
	 * Specifies that applying the rewrite should result in the compilation unit not containing the
	 * specified import.
	 * <p>
	 * Has no effect if the compilation unit otherwise would not contain the given import.
	 * <p>
	 * Overrides any previous corresponding call to {@link #addImport}.
	 */
	public void removeImport(boolean isStatic, String qualifiedName) {
		ImportName importToRemove = ImportName.createFor(isStatic, qualifiedName);
		this.importsToAdd.remove(importToRemove);
		this.importsToRemove.add(importToRemove);
	}

	/**
	 * Specifies that any import of the given simple name must be explicit - that it may neither be
	 * reduced into an on-demand (".*") import nor be filtered as an implicit (e.g. "java.lang.*")
	 * import.
	 */
	public void requireExplicitImport(boolean isStatic, String simpleName) {
		if (isStatic) {
			this.staticExplicitSimpleNames.add(simpleName);
		} else {
			this.typeExplicitSimpleNames.add(simpleName);
		}
	}

	/**
	 * Computes and returns the result of performing the rewrite, incorporating all changes
	 * specified by calls to {@link #addImport}, {@link #removeImport}, and
	 * {@link #requireExplicitImport}.
	 * <p>
	 * This method has no side-effects.
	 */
	public RewriteResult analyzeRewrite(IProgressMonitor monitor) throws JavaModelException {
		List<ImportName> computedImportOrder = computeImportOrder(monitor);

		List<ImportEntry> resultingImportEntries = matchExistingOrCreateNew(computedImportOrder);

		TextEdit edit = this.importEditor.createTextEdit(resultingImportEntries);

		Set<ImportName> createdImports = new HashSet<ImportName>(computedImportOrder);
		if (!this.reportAllResultantImportsAsCreated) {
			createdImports.removeAll(this.originalImportsSet);
		}

		return new RewriteResult(edit, createdImports);
	}

	private List<ImportName> computeImportOrder(IProgressMonitor progressMonitor) throws JavaModelException {
		Set<ImportName> importsWithAdditionsAndRemovals = new HashSet<ImportName>(this.originalImportsSet);
		importsWithAdditionsAndRemovals.addAll(this.importsToAdd);
		importsWithAdditionsAndRemovals.removeAll(this.importsToRemove);

		Set<ImportName> touchedContainers = determineTouchedContainers();

		Conflicts conflicts = this.conflictIdentifier.identifyConflicts(
				importsWithAdditionsAndRemovals,
				touchedContainers,
				this.typeExplicitSimpleNames,
				this.staticExplicitSimpleNames,
				progressMonitor);

		Set<String> allTypeExplicitSimpleNames = new HashSet<String>(this.typeExplicitSimpleNames);
		allTypeExplicitSimpleNames.addAll(conflicts.typeConflicts);

		Set<String> allStaticExplicitSimpleNames = new HashSet<String>(this.staticExplicitSimpleNames);
		allStaticExplicitSimpleNames.addAll(conflicts.staticConflicts);

		Set<ImportName> implicitImports = identifyImplicitImports(this.importsToAdd, allTypeExplicitSimpleNames);
		List<ImportName> importsWithoutImplicits =
				subtractImports(importsWithAdditionsAndRemovals, implicitImports);

		Collection<OnDemandReduction> onDemandReductions = this.onDemandComputer.identifyPossibleReductions(
				new HashSet<ImportName>(importsWithoutImplicits),
				touchedContainers,
				allTypeExplicitSimpleNames,
				allStaticExplicitSimpleNames);

		ImportsDelta delta = computeDelta(implicitImports, onDemandReductions);

		List<ImportName> importsWithRemovals = subtractImports(this.originalImportsList, delta.importsToRemove);

		List<ImportName> importsWithAdditions = this.importAdder.addImports(importsWithRemovals, delta.importsToAdd);

		return importsWithAdditions;
	}

	private Set<ImportName> determineTouchedContainers() {
		Collection<ImportName> touchedContainers = new ArrayList<ImportName>(
				this.importsToAdd.size() + this.importsToRemove.size());

		for (ImportName addedImport : this.importsToAdd) {
			touchedContainers.add(addedImport.getContainerOnDemand());
		}
		for (ImportName removedImport : this.importsToRemove) {
			touchedContainers.add(removedImport.getContainerOnDemand());
		}

		return Collections.unmodifiableSet(new HashSet<ImportName>(touchedContainers));
	}

	private Set<ImportName> identifyImplicitImports(
			Collection<ImportName> addedImports, Set<String> allTypeExplicitSimpleNames) {
		if (this.implicitImportContainerNames.isEmpty()) {
			return Collections.emptySet();
		}

		Collection<ImportName> implicits = new ArrayList<ImportName>(addedImports.size());
		for (ImportName addedImport : addedImports) {
			boolean isImplicit = this.implicitImportContainerNames.contains(addedImport.containerName)
					&& !allTypeExplicitSimpleNames.contains(addedImport.simpleName);
			if (isImplicit) {
				implicits.add(addedImport);
			}
		}

		if (implicits.isEmpty()) {
			return Collections.emptySet();
		}

		return new HashSet<ImportName>(implicits);
	}

	private List<ImportEntry> matchExistingOrCreateNew(Collection<ImportName> importNames) {
		List<ImportEntry> importEntries = new ArrayList<ImportEntry>(importNames.size());
		for (ImportName importName : importNames) {
			ImportEntry importEntry = this.importsByNameIdentity.get(importName);

			if (importEntry == null) {
				importEntry = new NewImportEntry(importName);
			}

			importEntries.add(importEntry);
		}
		return importEntries;
	}

	private ImportsDelta computeDelta(
			Collection<ImportName> implicitImports, Collection<OnDemandReduction> onDemandReductions) {
		Collection<ImportName> additions = new ArrayList<ImportName>(this.originalImportsList.size());
		additions.addAll(this.importsToAdd);

		Collection<ImportName> removals = new ArrayList<ImportName>(this.originalImportsList.size());
		removals.addAll(this.importsToRemove);
		removals.addAll(implicitImports);

		additions.removeAll(removals);

		for (OnDemandReduction onDemandReduction : onDemandReductions) {
			additions.removeAll(onDemandReduction.reducibleImports);
			removals.addAll(onDemandReduction.reducibleImports);

			additions.add(onDemandReduction.containerOnDemand);
			removals.remove(onDemandReduction.containerOnDemand);
		}

		return new ImportsDelta(additions, removals);
	}
}
