/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Stephan Herrmann - Contributions for bug 215139 and bug 295894
 *     Microsoft Corporation - Contribution for bug 575562 - improve completion search performance
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search;

import java.util.*;
import java.util.regex.Pattern;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.internal.compiler.*;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.core.*;
import org.eclipse.jdt.internal.core.search.indexing.*;
import org.eclipse.jdt.internal.core.search.matching.*;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * Search basic engine. Public search engine (see {@link org.eclipse.jdt.core.search.SearchEngine}
 * for detailed comment), now uses basic engine functionalities.
 * Note that search basic engine does not implement deprecated functionalities...
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class BasicSearchEngine {

	/*
	 * A default parser to parse non-reconciled working copies
	 */
	private Parser parser;
	private CompilerOptions compilerOptions;

	/*
	 * A list of working copies that take precedence over their original
	 * compilation units.
	 */
	private ICompilationUnit[] workingCopies;

	/*
	 * A working copy owner whose working copies will take precedent over
	 * their original compilation units.
	 */
	private WorkingCopyOwner workingCopyOwner;

	/**
	 * For tracing purpose.
	 */
	public static boolean VERBOSE = false;

	/*
	 * Creates a new search basic engine.
	 */
	public BasicSearchEngine() {
		// will use working copies of PRIMARY owner
	}

	/**
	 * @see SearchEngine#SearchEngine(ICompilationUnit[]) for detailed comment.
	 */
	public BasicSearchEngine(ICompilationUnit[] workingCopies) {
		this.workingCopies = workingCopies;
	}

	char convertTypeKind(int typeDeclarationKind) {
		switch(typeDeclarationKind) {
			case TypeDeclaration.CLASS_DECL : return IIndexConstants.CLASS_SUFFIX;
			case TypeDeclaration.INTERFACE_DECL : return IIndexConstants.INTERFACE_SUFFIX;
			case TypeDeclaration.ENUM_DECL : return IIndexConstants.ENUM_SUFFIX;
			case TypeDeclaration.ANNOTATION_TYPE_DECL : return IIndexConstants.ANNOTATION_TYPE_SUFFIX;
			default : return IIndexConstants.TYPE_SUFFIX;
		}
	}
	/**
	 * @see SearchEngine#SearchEngine(WorkingCopyOwner) for detailed comment.
	 */
	public BasicSearchEngine(WorkingCopyOwner workingCopyOwner) {
		this.workingCopyOwner = workingCopyOwner;
	}

	/**
	 * @see SearchEngine#createHierarchyScope(IType) for detailed comment.
	 */
	public static IJavaSearchScope createHierarchyScope(IType type) throws JavaModelException {
		return createHierarchyScope(type, DefaultWorkingCopyOwner.PRIMARY);
	}

	/**
	 * @see SearchEngine#createHierarchyScope(IType,WorkingCopyOwner) for detailed comment.
	 */
	public static IJavaSearchScope createHierarchyScope(IType type, WorkingCopyOwner owner) throws JavaModelException {
		return new HierarchyScope(type, owner);
	}

	/**
	 * @see SearchEngine#createStrictHierarchyScope(IJavaProject,IType,boolean,boolean,WorkingCopyOwner) for detailed comment.
	 */
	public static IJavaSearchScope createStrictHierarchyScope(IJavaProject project, IType type, boolean onlySubtypes, boolean includeFocusType, WorkingCopyOwner owner) throws JavaModelException {
		return new HierarchyScope(project, type, owner, onlySubtypes, true, includeFocusType);
	}

	/**
	 * @see SearchEngine#createJavaSearchScope(IJavaElement[]) for detailed comment.
	 */
	public static IJavaSearchScope createJavaSearchScope(IJavaElement[] elements) {
		return createJavaSearchScope(false, elements, true);
	}

	public static IJavaSearchScope createJavaSearchScope(boolean excludeTestCode, IJavaElement[] elements) {
		return createJavaSearchScope(excludeTestCode, elements, true);
	}

	public static IJavaSearchScope createJavaSearchScope(IJavaElement[] elements, boolean includeReferencedProjects) {
		return createJavaSearchScope(false, elements, includeReferencedProjects);
	}
	/**
	 * @see SearchEngine#createJavaSearchScope(boolean, IJavaElement[], boolean) for detailed comment.
	 */
	public static IJavaSearchScope createJavaSearchScope(boolean excludeTestCode, IJavaElement[] elements, boolean includeReferencedProjects) {
		int includeMask = IJavaSearchScope.SOURCES | IJavaSearchScope.APPLICATION_LIBRARIES | IJavaSearchScope.SYSTEM_LIBRARIES;
		if (includeReferencedProjects) {
			includeMask |= IJavaSearchScope.REFERENCED_PROJECTS;
		}
		return createJavaSearchScope(excludeTestCode, elements, includeMask);
	}

	public static IJavaSearchScope createJavaSearchScope(IJavaElement[] elements, int includeMask) {
		return createJavaSearchScope(false, elements, includeMask);
	}
	/**
	 * @see SearchEngine#createJavaSearchScope(boolean, IJavaElement[], int) for detailed comment.
	 */
	public static IJavaSearchScope createJavaSearchScope(boolean excludeTestCode, IJavaElement[] elements, int includeMask) {
		HashSet projectsToBeAdded = new HashSet(2);
		for (int i = 0, length = elements.length; i < length; i++) {
			IJavaElement element = elements[i];
			if (element instanceof JavaProject) {
				projectsToBeAdded.add(element);
			}
		}
		JavaSearchScope scope = new JavaSearchScope(excludeTestCode);
		for (int i = 0, length = elements.length; i < length; i++) {
			IJavaElement element = elements[i];
			if (element != null) {
				try {
					if (projectsToBeAdded.contains(element)) {
						scope.add((JavaProject)element, includeMask, projectsToBeAdded);
					} else {
						scope.add(element);
					}
				} catch (JavaModelException e) {
					// ignore
				}
			}
		}
		return scope;
	}

	/**
	 * @see SearchEngine#createTypeNameMatch(IType, int) for detailed comment.
	 */
	public static TypeNameMatch createTypeNameMatch(IType type, int modifiers) {
		return new JavaSearchTypeNameMatch(type, modifiers);
	}

	/**
	 * @see SearchEngine#createMethodNameMatch(IMethod, int) for detailed comment.
	 */
	public static MethodNameMatch createMethodNameMatch(IMethod method, int modifiers) {
		return new JavaSearchMethodNameMatch(method, modifiers);
	}

	/**
	 * @see SearchEngine#createWorkspaceScope() for detailed comment.
	 */
	public static IJavaSearchScope createWorkspaceScope() {
		return JavaModelManager.getJavaModelManager().getWorkspaceScope();
	}

	/**
	 * Searches for matches to a given query. Search queries can be created using helper
	 * methods (from a String pattern or a Java element) and encapsulate the description of what is
	 * being searched (for example, search method declarations in a case sensitive way).
	 *
	 * @param scope the search result has to be limited to the given scope
	 * @param requestor a callback object to which each match is reported
	 */
	void findMatches(SearchPattern pattern, SearchParticipant[] participants, IJavaSearchScope scope, SearchRequestor requestor, IProgressMonitor monitor) throws CoreException {
		try {
			if (VERBOSE) {
				Util.verbose("Searching for pattern: " + pattern.toString()); //$NON-NLS-1$
				Util.verbose(scope.toString());
			}
			if (participants == null) {
				if (VERBOSE) Util.verbose("No participants => do nothing!"); //$NON-NLS-1$
				return;
			}

			/* initialize progress monitor */
			int length = participants.length;
			SubMonitor loopMonitor = SubMonitor.convert(monitor, Messages.engine_searching, length);
			IndexManager indexManager = JavaModelManager.getIndexManager();
			requestor.beginReporting();
			for (int i = 0; i < length; i++) {
				SubMonitor iterationMonitor = loopMonitor.split(1).setWorkRemaining(100);

				SearchParticipant participant = participants[i];
				try {
					iterationMonitor.subTask(Messages.bind(Messages.engine_searching_indexing, new String[] {participant.getDescription()}));
					participant.beginSearching();
					requestor.enterParticipant(participant);
					PathCollector pathCollector = new PathCollector();
					indexManager.performConcurrentJob(
						new PatternSearchJob(pattern, participant, scope, pathCollector),
						IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
						iterationMonitor.split(50));

					// locate index matches if any (note that all search matches could have been issued during index querying)
					iterationMonitor.subTask(Messages.bind(Messages.engine_searching_matching, new String[] {participant.getDescription()}));
					String[] indexMatchPaths = pathCollector.getPaths();
					if (indexMatchPaths != null) {
						pathCollector = null; // release
						int indexMatchLength = indexMatchPaths.length;
						SearchDocument[] indexMatches = new SearchDocument[indexMatchLength];
						for (int j = 0; j < indexMatchLength; j++) {
							indexMatches[j] = participant.getDocument(indexMatchPaths[j]);
						}
						SearchDocument[] matches = MatchLocator.addWorkingCopies(pattern, indexMatches, getWorkingCopies(), participant);
						participant.locateMatches(matches, pattern, scope, requestor, iterationMonitor.split(50));
					}
				} finally {
					requestor.exitParticipant(participant);
					participant.doneSearching();
				}
			}
		} finally {
			requestor.endReporting();
			if (monitor != null) {
				monitor.done();
			}
		}
	}
	/**
	 * Returns a new default Java search participant.
	 *
	 * @return a new default Java search participant
	 * @since 3.0
	 */
	public static SearchParticipant getDefaultSearchParticipant() {
		return new JavaSearchParticipant();
	}

	/**
	 * @param matchRule
	 */
	public static String getMatchRuleString(final int matchRule) {
		if (matchRule == 0) {
			return "R_EXACT_MATCH"; //$NON-NLS-1$
		}
		StringBuilder buffer = new StringBuilder();
		for (int i=1; i<=16; i++) {
			int bit = matchRule & (1<<(i-1));
			if (bit != 0 && buffer.length()>0) buffer.append(" | "); //$NON-NLS-1$
			switch (bit) {
				case SearchPattern.R_PREFIX_MATCH:
					buffer.append("R_PREFIX_MATCH"); //$NON-NLS-1$
					break;
				case SearchPattern.R_CASE_SENSITIVE:
					buffer.append("R_CASE_SENSITIVE"); //$NON-NLS-1$
					break;
				case SearchPattern.R_EQUIVALENT_MATCH:
					buffer.append("R_EQUIVALENT_MATCH"); //$NON-NLS-1$
					break;
				case SearchPattern.R_ERASURE_MATCH:
					buffer.append("R_ERASURE_MATCH"); //$NON-NLS-1$
					break;
				case SearchPattern.R_FULL_MATCH:
					buffer.append("R_FULL_MATCH"); //$NON-NLS-1$
					break;
				case SearchPattern.R_PATTERN_MATCH:
					buffer.append("R_PATTERN_MATCH"); //$NON-NLS-1$
					break;
				case SearchPattern.R_REGEXP_MATCH:
					buffer.append("R_REGEXP_MATCH"); //$NON-NLS-1$
					break;
				case SearchPattern.R_CAMELCASE_MATCH:
					buffer.append("R_CAMELCASE_MATCH"); //$NON-NLS-1$
					break;
				case SearchPattern.R_CAMELCASE_SAME_PART_COUNT_MATCH:
					buffer.append("R_CAMELCASE_SAME_PART_COUNT_MATCH"); //$NON-NLS-1$
					break;
			}
		}
		return buffer.toString();
	}

	/**
	 * Return kind of search corresponding to given value.
	 *
	 * @param searchFor
	 */
	public static String getSearchForString(final int searchFor) {
		switch (searchFor) {
			case IJavaSearchConstants.TYPE:
				return ("TYPE"); //$NON-NLS-1$
			case IJavaSearchConstants.METHOD:
				return ("METHOD"); //$NON-NLS-1$
			case IJavaSearchConstants.PACKAGE:
				return ("PACKAGE"); //$NON-NLS-1$
			case IJavaSearchConstants.CONSTRUCTOR:
				return ("CONSTRUCTOR"); //$NON-NLS-1$
			case IJavaSearchConstants.FIELD:
				return ("FIELD"); //$NON-NLS-1$
			case IJavaSearchConstants.CLASS:
				return ("CLASS"); //$NON-NLS-1$
			case IJavaSearchConstants.INTERFACE:
				return ("INTERFACE"); //$NON-NLS-1$
			case IJavaSearchConstants.ENUM:
				return ("ENUM"); //$NON-NLS-1$
			case IJavaSearchConstants.ANNOTATION_TYPE:
				return ("ANNOTATION_TYPE"); //$NON-NLS-1$
			case IJavaSearchConstants.CLASS_AND_ENUM:
				return ("CLASS_AND_ENUM"); //$NON-NLS-1$
			case IJavaSearchConstants.CLASS_AND_INTERFACE:
				return ("CLASS_AND_INTERFACE"); //$NON-NLS-1$
			case IJavaSearchConstants.INTERFACE_AND_ANNOTATION:
				return ("INTERFACE_AND_ANNOTATION"); //$NON-NLS-1$
		}
		return "UNKNOWN"; //$NON-NLS-1$
	}

	private Parser getParser() {
		if (this.parser == null) {
			this.compilerOptions = new CompilerOptions(JavaCore.getOptions());
			ProblemReporter problemReporter =
				new ProblemReporter(
					DefaultErrorHandlingPolicies.proceedWithAllProblems(),
					this.compilerOptions,
					new DefaultProblemFactory());
			this.parser = new Parser(problemReporter, true);
		}
		return this.parser;
	}

	/*
	 * Returns the list of working copies used by this search engine.
	 * Returns null if none.
	 */
	private ICompilationUnit[] getWorkingCopies() {
		ICompilationUnit[] copies;
		if (this.workingCopies != null) {
			if (this.workingCopyOwner == null) {
				copies = JavaModelManager.getJavaModelManager().getWorkingCopies(DefaultWorkingCopyOwner.PRIMARY, false/*don't add primary WCs a second time*/);
				if (copies == null) {
					copies = this.workingCopies;
				} else {
					HashMap pathToCUs = new HashMap();
					for (int i = 0, length = copies.length; i < length; i++) {
						ICompilationUnit unit = copies[i];
						pathToCUs.put(unit.getPath(), unit);
					}
					for (int i = 0, length = this.workingCopies.length; i < length; i++) {
						ICompilationUnit unit = this.workingCopies[i];
						pathToCUs.put(unit.getPath(), unit);
					}
					int length = pathToCUs.size();
					copies = new ICompilationUnit[length];
					pathToCUs.values().toArray(copies);
				}
			} else {
				copies = this.workingCopies;
			}
		} else if (this.workingCopyOwner != null) {
			copies = JavaModelManager.getJavaModelManager().getWorkingCopies(this.workingCopyOwner, true/*add primary WCs*/);
		} else {
			copies = JavaModelManager.getJavaModelManager().getWorkingCopies(DefaultWorkingCopyOwner.PRIMARY, false/*don't add primary WCs a second time*/);
		}
		if (copies == null) return null;

		// filter out primary working copies that are saved
		ICompilationUnit[] result = null;
		int length = copies.length;
		int index = 0;
		for (int i = 0; i < length; i++) {
			CompilationUnit copy = (CompilationUnit)copies[i];
			try {
				if (!copy.isPrimary()
						|| copy.hasUnsavedChanges()
						|| copy.hasResourceChanged()) {
					if (result == null) {
						result = new ICompilationUnit[length];
					}
					result[index++] = copy;
				}
			}  catch (JavaModelException e) {
				// copy doesn't exist: ignore
			}
		}
		if (index != length && result != null) {
			System.arraycopy(result, 0, result = new ICompilationUnit[index], 0, index);
		}
		return result;
	}

	/*
	 * Returns the working copy to use to do the search on the given Java element.
	 */
	private ICompilationUnit[] getWorkingCopies(IJavaElement element) {
		if (element instanceof IMember) {
			ICompilationUnit cu = ((IMember)element).getCompilationUnit();
			if (cu != null && cu.isWorkingCopy()) {
				return new ICompilationUnit[] { cu };
			}
		} else if (element instanceof ICompilationUnit) {
			return new ICompilationUnit[] { (ICompilationUnit) element };
		}

		return null;
	}

	boolean match(char patternTypeSuffix, int modifiers) {
		switch(patternTypeSuffix) {
			case IIndexConstants.CLASS_SUFFIX :
				return (modifiers & (Flags.AccAnnotation | Flags.AccInterface | Flags.AccEnum)) == 0;
			case IIndexConstants.CLASS_AND_INTERFACE_SUFFIX:
				return (modifiers & (Flags.AccAnnotation | Flags.AccEnum)) == 0;
			case IIndexConstants.CLASS_AND_ENUM_SUFFIX:
				return (modifiers & (Flags.AccAnnotation | Flags.AccInterface)) == 0;
			case IIndexConstants.INTERFACE_SUFFIX :
				return (modifiers & Flags.AccInterface) != 0;
			case IIndexConstants.INTERFACE_AND_ANNOTATION_SUFFIX:
				return (modifiers & (Flags.AccInterface | Flags.AccAnnotation)) != 0;
			case IIndexConstants.ENUM_SUFFIX :
				return (modifiers & Flags.AccEnum) != 0;
			case IIndexConstants.ANNOTATION_TYPE_SUFFIX :
				return (modifiers & Flags.AccAnnotation) != 0;
		}
		return true;
	}

	boolean match(char patternTypeSuffix, char[] patternPkg, int matchRulePkg, char[] patternTypeName, int matchRuleType, int typeKind, char[] pkg, char[] typeName) {
		switch(patternTypeSuffix) {
			case IIndexConstants.CLASS_SUFFIX :
				if (typeKind != TypeDeclaration.CLASS_DECL) return false;
				break;
			case IIndexConstants.CLASS_AND_INTERFACE_SUFFIX:
				if (typeKind != TypeDeclaration.CLASS_DECL && typeKind != TypeDeclaration.INTERFACE_DECL) return false;
				break;
			case IIndexConstants.CLASS_AND_ENUM_SUFFIX:
				if (typeKind != TypeDeclaration.CLASS_DECL && typeKind != TypeDeclaration.ENUM_DECL) return false;
				break;
			case IIndexConstants.INTERFACE_SUFFIX :
				if (typeKind != TypeDeclaration.INTERFACE_DECL) return false;
				break;
			case IIndexConstants.INTERFACE_AND_ANNOTATION_SUFFIX:
				if (typeKind != TypeDeclaration.INTERFACE_DECL && typeKind != TypeDeclaration.ANNOTATION_TYPE_DECL) return false;
				break;
			case IIndexConstants.ENUM_SUFFIX :
				if (typeKind != TypeDeclaration.ENUM_DECL) return false;
				break;
			case IIndexConstants.ANNOTATION_TYPE_SUFFIX :
				if (typeKind != TypeDeclaration.ANNOTATION_TYPE_DECL) return false;
				break;
			case IIndexConstants.TYPE_SUFFIX : // nothing
		}

		boolean isPkgCaseSensitive = (matchRulePkg & SearchPattern.R_CASE_SENSITIVE) != 0;
		if (patternPkg != null && !CharOperation.equals(patternPkg, pkg, isPkgCaseSensitive))
			return false;

		boolean isCaseSensitive = (matchRuleType & SearchPattern.R_CASE_SENSITIVE) != 0;
		if (patternTypeName != null) {
			boolean isCamelCase = (matchRuleType & (SearchPattern.R_CAMELCASE_MATCH | SearchPattern.R_CAMELCASE_SAME_PART_COUNT_MATCH)) != 0;

			if ((matchRuleType & SearchPattern.R_SUBSTRING_MATCH) != 0 && CharOperation.substringMatch(patternTypeName, typeName))
				return true;
			if ((matchRuleType & SearchPattern.R_SUBWORD_MATCH) != 0 && CharOperation.subWordMatch(patternTypeName, typeName))
				return true;

			int matchMode = matchRuleType & JavaSearchPattern.MATCH_MODE_MASK;
			if (!isCaseSensitive && !isCamelCase) {
				patternTypeName = CharOperation.toLowerCase(patternTypeName);
			}
			boolean matchFirstChar = !isCaseSensitive || patternTypeName[0] == typeName[0];
			switch(matchMode) {
				case SearchPattern.R_EXACT_MATCH :
					return matchFirstChar && CharOperation.equals(patternTypeName, typeName, isCaseSensitive);
				case SearchPattern.R_PREFIX_MATCH :
					return matchFirstChar && CharOperation.prefixEquals(patternTypeName, typeName, isCaseSensitive);
				case SearchPattern.R_PATTERN_MATCH :
					return CharOperation.match(patternTypeName, typeName, isCaseSensitive);
				case SearchPattern.R_REGEXP_MATCH :
					return Pattern.matches(new String(patternTypeName), new String(typeName));
				case SearchPattern.R_CAMELCASE_MATCH:
					if (matchFirstChar && CharOperation.camelCaseMatch(patternTypeName, typeName, false)) {
						return true;
					}
					return !isCaseSensitive && matchFirstChar && CharOperation.prefixEquals(patternTypeName, typeName, false);
				case SearchPattern.R_CAMELCASE_SAME_PART_COUNT_MATCH:
					return matchFirstChar && CharOperation.camelCaseMatch(patternTypeName, typeName, true);
			}
		}
		return true;

	}

	boolean match(char[] patternName, int matchRule, char[] name) {
		boolean isCaseSensitive = (matchRule & SearchPattern.R_CASE_SENSITIVE) != 0;
		if (patternName != null) {
			boolean isCamelCase = (matchRule & (SearchPattern.R_CAMELCASE_MATCH | SearchPattern.R_CAMELCASE_SAME_PART_COUNT_MATCH)) != 0;
			int matchMode = matchRule & JavaSearchPattern.MATCH_MODE_MASK;
			if (!isCaseSensitive && !isCamelCase) {
				patternName = CharOperation.toLowerCase(patternName);
			}
			boolean matchFirstChar = !isCaseSensitive || patternName[0] == name[0];
			switch(matchMode) {
				case SearchPattern.R_EXACT_MATCH :
					return matchFirstChar && CharOperation.equals(patternName, name, isCaseSensitive);
				case SearchPattern.R_PREFIX_MATCH :
					return matchFirstChar && CharOperation.prefixEquals(patternName, name, isCaseSensitive);
				case SearchPattern.R_PATTERN_MATCH :
					return CharOperation.match(patternName, name, isCaseSensitive);
				case SearchPattern.R_REGEXP_MATCH :
					return Pattern.matches(new String(patternName), new String(name));
				case SearchPattern.R_CAMELCASE_MATCH:
					if (matchFirstChar && CharOperation.camelCaseMatch(patternName, name, false)) {
						return true;
					}
					return !isCaseSensitive && matchFirstChar && CharOperation.prefixEquals(patternName, name, false);
				case SearchPattern.R_CAMELCASE_SAME_PART_COUNT_MATCH:
					return matchFirstChar && CharOperation.camelCaseMatch(patternName, name, true);
			}
		}
		return true;
	}

	boolean match(char[] patternPkg, int matchRulePkg,
			char[] patternDeclaringQualifier, int matchRuleDeclaringQualifier,
			char[] patternDeclaringSimpleName, int matchRuleDeclaringSimpleName,
			char[] patternMethodName, int methodMatchRule,
			char[] packageName, char[] declaringQualifier, char[] declaringSimpleName, char[] methodName) {

		if (patternPkg != null && !CharOperation.equals(patternPkg, packageName, (matchRulePkg & SearchPattern.R_CASE_SENSITIVE) != 0))
			return false;

		return match(patternDeclaringQualifier, matchRuleDeclaringQualifier, declaringQualifier) &&
				match(patternDeclaringSimpleName, matchRuleDeclaringSimpleName, declaringSimpleName) &&
				match(patternMethodName, methodMatchRule, methodName);

	}

	boolean match(char[] patternFusedQualifier, int matchRuleFusedQualifier,
			char[] patternMethodName, int methodMatchRule,
			char[] packageName, char[] declaringQualifier, char[] declaringSimpleName, char[] methodName) {

		char[] q = packageName != null ? packageName : CharOperation.NO_CHAR;
		if (declaringQualifier != null && declaringQualifier.length > 0) {
			q = q.length > 0 ? CharOperation.concat(q, declaringQualifier, '.') : declaringQualifier;
		}
		if (declaringSimpleName != null && declaringSimpleName.length > 0) {
			q = q.length > 0 ? CharOperation.concat(q, declaringSimpleName, '.') : declaringSimpleName;
		}

		return match(patternFusedQualifier, matchRuleFusedQualifier, q) &&
				match(patternMethodName, methodMatchRule, methodName);

	}
	/**
	 * Searches for matches of a given search pattern. Search patterns can be created using helper
	 * methods (from a String pattern or a Java element) and encapsulate the description of what is
	 * being searched (for example, search method declarations in a case sensitive way).
	 *
	 * @see SearchEngine#search(SearchPattern, SearchParticipant[], IJavaSearchScope, SearchRequestor, IProgressMonitor)
	 * 	for detailed comment
	 */
	public void search(SearchPattern pattern, SearchParticipant[] participants, IJavaSearchScope scope, SearchRequestor requestor, IProgressMonitor monitor) throws CoreException {
		if (VERBOSE) {
			Util.verbose("BasicSearchEngine.search(SearchPattern, SearchParticipant[], IJavaSearchScope, SearchRequestor, IProgressMonitor)"); //$NON-NLS-1$
		}
		findMatches(pattern, participants, scope, requestor, monitor);
	}

	public void searchAllConstructorDeclarations(
			final char[] packageName,
			final char[] typeName,
			final int typeMatchRule,
			IJavaSearchScope scope,
			final IRestrictedAccessConstructorRequestor nameRequestor,
			int waitingPolicy,
			IProgressMonitor progressMonitor)  throws JavaModelException {
		searchAllConstructorDeclarations(
				packageName,
				typeName,
				typeMatchRule,
				scope,
				true,
				nameRequestor,
				waitingPolicy,
				progressMonitor);
	}

	/**
	 *
	 * Searches for constructor declarations in the given scope.
	 *
	 * @param resolveDocumentName used to tell SearchEngine whether to resolve
	 *                            the document name for each result entry.
	 */
	public void searchAllConstructorDeclarations(
		final char[] packageName,
		final char[] typeName,
		final int typeMatchRule,
		IJavaSearchScope scope,
		final boolean resolveDocumentName,
		final IRestrictedAccessConstructorRequestor nameRequestor,
		int waitingPolicy,
		IProgressMonitor progressMonitor)  throws JavaModelException {

		try {
			// Validate match rule first
			final int validatedTypeMatchRule = SearchPattern.validateMatchRule(typeName == null ? null : new String (typeName), typeMatchRule);

			final int pkgMatchRule = SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE;
			final char NoSuffix = IIndexConstants.TYPE_SUFFIX; // Used as TYPE_SUFFIX has no effect in method #match(char, char[] , int, char[], int , int, char[], char[])

			// Debug
			if (VERBOSE) {
				Util.verbose("BasicSearchEngine.searchAllConstructorDeclarations(char[], char[], int, IJavaSearchScope, IRestrictedAccessConstructorRequestor, int, IProgressMonitor)"); //$NON-NLS-1$
				Util.verbose("	- package name: "+(packageName==null?"null":new String(packageName))); //$NON-NLS-1$ //$NON-NLS-2$
				Util.verbose("	- type name: "+(typeName==null?"null":new String(typeName))); //$NON-NLS-1$ //$NON-NLS-2$
				Util.verbose("	- type match rule: "+getMatchRuleString(typeMatchRule)); //$NON-NLS-1$
				if (validatedTypeMatchRule != typeMatchRule) {
					Util.verbose("	- validated type match rule: "+getMatchRuleString(validatedTypeMatchRule)); //$NON-NLS-1$
				}
				Util.verbose("	- scope: "+scope); //$NON-NLS-1$
			}
			if (validatedTypeMatchRule == -1) return; // invalid match rule => return no results

			// Create pattern
			IndexManager indexManager = JavaModelManager.getIndexManager();
			final ConstructorDeclarationPattern pattern = new ConstructorDeclarationPattern(
					packageName,
					typeName,
					validatedTypeMatchRule);

			// Get working copy path(s). Store in a single string in case of only one to optimize comparison in requestor
			final HashSet workingCopyPaths = new HashSet();
			String workingCopyPath = null;
			ICompilationUnit[] copies = getWorkingCopies();
			final int copiesLength = copies == null ? 0 : copies.length;
			if (copies != null) {
				if (copiesLength == 1) {
					workingCopyPath = copies[0].getPath().toString();
				} else {
					for (int i = 0; i < copiesLength; i++) {
						ICompilationUnit workingCopy = copies[i];
						workingCopyPaths.add(workingCopy.getPath().toString());
					}
				}
			}
			final String singleWkcpPath = workingCopyPath;

			// Index requestor
			IndexQueryRequestor searchRequestor = new IndexQueryRequestor(){
				@Override
				public boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord, SearchParticipant participant, AccessRuleSet access) {
					// Filter unexpected types
					ConstructorDeclarationPattern record = (ConstructorDeclarationPattern)indexRecord;

					if ((record.extraFlags & ExtraFlags.IsMemberType) != 0) {
						return true; // filter out member classes
					}
					if ((record.extraFlags & ExtraFlags.IsLocalType) != 0) {
						return true; // filter out local and anonymous classes
					}
					switch (copiesLength) {
						case 0:
							break;
						case 1:
							if (singleWkcpPath.equals(documentPath)) {
								return true; // filter out *the* working copy
							}
							break;
						default:
							if (workingCopyPaths.contains(documentPath)) {
								return true; // filter out working copies
							}
							break;
					}

					// Accept document path
					AccessRestriction accessRestriction = null;
					if (access != null) {
						// Compute document relative path
						int pkgLength = (record.declaringPackageName==null || record.declaringPackageName.length==0) ? 0 : record.declaringPackageName.length+1;
						int nameLength = record.declaringSimpleName==null ? 0 : record.declaringSimpleName.length;
						char[] path = new char[pkgLength+nameLength];
						int pos = 0;
						if (pkgLength > 0) {
							System.arraycopy(record.declaringPackageName, 0, path, pos, pkgLength-1);
							CharOperation.replace(path, '.', '/');
							path[pkgLength-1] = '/';
							pos += pkgLength;
						}
						if (nameLength > 0) {
							System.arraycopy(record.declaringSimpleName, 0, path, pos, nameLength);
							pos += nameLength;
						}
						// Update access restriction if path is not empty
						if (pos > 0) {
							accessRestriction = access.getViolatedRestriction(path);
						}
					}
					nameRequestor.acceptConstructor(
							record.modifiers,
							record.declaringSimpleName,
							record.parameterCount,
							record.signature,
							record.parameterTypes,
							record.parameterNames,
							record.declaringTypeModifiers,
							record.declaringPackageName,
							record.extraFlags,
							documentPath,
							accessRestriction);
					return true;
				}
			};

			SubMonitor subMonitor = SubMonitor.convert(progressMonitor, Messages.engine_searching, 1000);
			// add type names from indexes
			indexManager.performConcurrentJob(
				new PatternSearchJob(
					pattern,
					getDefaultSearchParticipant(), // Java search only
					scope,
					resolveDocumentName,
					true,
					searchRequestor),
				waitingPolicy,
				subMonitor.split(Math.max(1000-copiesLength, 0)));

			// add type names from working copies
			if (copies != null) {
				for (int i = 0; i < copiesLength; i++) {
					SubMonitor iterationMonitor = subMonitor.split(1);
					final ICompilationUnit workingCopy = copies[i];
					if (scope instanceof HierarchyScope) {
						if (!((HierarchyScope)scope).encloses(workingCopy, iterationMonitor)) continue;
					} else {
						if (!scope.encloses(workingCopy)) continue;
					}

					final String path = workingCopy.getPath().toString();
					if (workingCopy.isConsistent()) {
						IPackageDeclaration[] packageDeclarations = workingCopy.getPackageDeclarations();
						char[] packageDeclaration = packageDeclarations.length == 0 ? CharOperation.NO_CHAR : packageDeclarations[0].getElementName().toCharArray();
						IType[] allTypes = workingCopy.getAllTypes();
						for (int j = 0, allTypesLength = allTypes.length; j < allTypesLength; j++) {
							IType type = allTypes[j];
							char[] simpleName = type.getElementName().toCharArray();
							if (match(NoSuffix, packageName, pkgMatchRule, typeName, validatedTypeMatchRule, 0/*no kind*/, packageDeclaration, simpleName) && !type.isMember()) {

								int extraFlags = ExtraFlags.getExtraFlags(type);

								boolean hasConstructor = false;

								IMethod[] methods = type.getMethods();
								for (int k = 0; k < methods.length; k++) {
									IMethod method = methods[k];
									if (method.isConstructor()) {
										hasConstructor = true;

										String[] stringParameterNames = method.getParameterNames();
										String[] stringParameterTypes = method.getParameterTypes();
										int length = stringParameterNames.length;
										char[][] parameterNames = new char[length][];
										char[][] parameterTypes = new char[length][];
										for (int l = 0; l < length; l++) {
											parameterNames[l] = stringParameterNames[l].toCharArray();
											parameterTypes[l] = Signature.toCharArray(Signature.getTypeErasure(stringParameterTypes[l]).toCharArray());
										}

										nameRequestor.acceptConstructor(
												method.getFlags(),
												simpleName,
												parameterNames.length,
												null,// signature is not used for source type
												parameterTypes,
												parameterNames,
												type.getFlags(),
												packageDeclaration,
												extraFlags,
												path,
												null);
									}
								}

								if (!hasConstructor) {
									nameRequestor.acceptConstructor(
											Flags.AccPublic,
											simpleName,
											-1,
											null, // signature is not used for source type
											CharOperation.NO_CHAR_CHAR,
											CharOperation.NO_CHAR_CHAR,
											type.getFlags(),
											packageDeclaration,
											extraFlags,
											path,
											null);
								}
							}
						}
					} else {
						Parser basicParser = getParser();
						org.eclipse.jdt.internal.compiler.env.ICompilationUnit unit = (org.eclipse.jdt.internal.compiler.env.ICompilationUnit) workingCopy;
						CompilationResult compilationUnitResult = new CompilationResult(unit, 0, 0, this.compilerOptions.maxProblemsPerUnit);
						CompilationUnitDeclaration parsedUnit = basicParser.dietParse(unit, compilationUnitResult);
						if (parsedUnit != null) {
							final char[] packageDeclaration = parsedUnit.currentPackage == null ? CharOperation.NO_CHAR : CharOperation.concatWith(parsedUnit.currentPackage.getImportName(), '.');
							class AllConstructorDeclarationsVisitor extends ASTVisitor {
								private TypeDeclaration[] declaringTypes = new TypeDeclaration[0];
								private int declaringTypesPtr = -1;

								private void endVisit(TypeDeclaration typeDeclaration) {
									if (!hasConstructor(typeDeclaration) && typeDeclaration.enclosingType == null) {

										if (match(NoSuffix, packageName, pkgMatchRule, typeName, validatedTypeMatchRule, 0/*no kind*/, packageDeclaration, typeDeclaration.name)) {
											nameRequestor.acceptConstructor(
													Flags.AccPublic,
													typeName,
													-1,
													null, // signature is not used for source type
													CharOperation.NO_CHAR_CHAR,
													CharOperation.NO_CHAR_CHAR,
													typeDeclaration.modifiers,
													packageDeclaration,
													ExtraFlags.getExtraFlags(typeDeclaration),
													path,
													null);
										}
									}

									this.declaringTypes[this.declaringTypesPtr] = null;
									this.declaringTypesPtr--;
								}

								@Override
								public void endVisit(TypeDeclaration typeDeclaration, CompilationUnitScope s) {
									endVisit(typeDeclaration);
								}

								@Override
								public void endVisit(TypeDeclaration memberTypeDeclaration, ClassScope s) {
									endVisit(memberTypeDeclaration);
								}

								private boolean hasConstructor(TypeDeclaration typeDeclaration) {
									AbstractMethodDeclaration[] methods = typeDeclaration.methods;
									int length = methods == null ? 0 : methods.length;
									for (int j = 0; j < length; j++) {
										if (methods[j].isConstructor()) {
											return true;
										}
									}

									return false;
								}
								@Override
								public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope classScope) {
									TypeDeclaration typeDeclaration = this.declaringTypes[this.declaringTypesPtr];
									if (match(NoSuffix, packageName, pkgMatchRule, typeName, validatedTypeMatchRule, 0/*no kind*/, packageDeclaration, typeDeclaration.name)) {
										Argument[] arguments = constructorDeclaration.arguments;
										int length = arguments == null ? 0 : arguments.length;
										char[][] parameterNames = new char[length][];
										char[][] parameterTypes = new char[length][];
										for (int l = 0; l < length; l++) {
											Argument argument = arguments[l];
											parameterNames[l] = argument.name;
											if (argument.type instanceof SingleTypeReference) {
												parameterTypes[l] = ((SingleTypeReference)argument.type).token;
											} else {
												parameterTypes[l] = CharOperation.concatWith(((QualifiedTypeReference)argument.type).tokens, '.');
											}
										}

										TypeDeclaration enclosing = typeDeclaration.enclosingType;
										char[][] enclosingTypeNames = CharOperation.NO_CHAR_CHAR;
										while (enclosing != null) {
											enclosingTypeNames = CharOperation.arrayConcat(new char[][] {enclosing.name}, enclosingTypeNames);
											if ((enclosing.bits & ASTNode.IsMemberType) != 0) {
												enclosing = enclosing.enclosingType;
											} else {
												enclosing = null;
											}
										}

										nameRequestor.acceptConstructor(
												constructorDeclaration.modifiers,
												typeName,
												parameterNames.length,
												null, // signature is not used for source type
												parameterTypes,
												parameterNames,
												typeDeclaration.modifiers,
												packageDeclaration,
												ExtraFlags.getExtraFlags(typeDeclaration),
												path,
												null);
									}
									return false; // no need to find constructors from local/anonymous type
								}
								@Override
								public boolean visit(TypeDeclaration typeDeclaration, BlockScope blockScope) {
									return false;
								}

								private boolean visit(TypeDeclaration typeDeclaration) {
									if(this.declaringTypes.length <= ++this.declaringTypesPtr) {
										int length = this.declaringTypesPtr;
										System.arraycopy(this.declaringTypes, 0, this.declaringTypes = new TypeDeclaration[length * 2 + 1], 0, length);
									}
									this.declaringTypes[this.declaringTypesPtr] = typeDeclaration;
									return true;
								}

								@Override
								public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope s) {
									return visit(typeDeclaration);
								}

								@Override
								public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope s) {
									return visit(memberTypeDeclaration);
								}
							}
							parsedUnit.traverse(new AllConstructorDeclarationsVisitor(), parsedUnit.scope);
						}
					}
				}
			}
		} finally {
			if (progressMonitor != null) {
				progressMonitor.done();
			}
		}
	}

	/**
	 * Searches for all method declarations in the given scope.
	 * 	 * <p>
	 * Warning: This API is in experimental phase and may be modified/removed. Do not use this until this
	 * comment is removed.
	 * </p>
	 *
	 * @see SearchEngine#searchAllMethodNames(char[], int, char[], int, IJavaSearchScope, MethodNameMatchRequestor, int, IProgressMonitor)
	 * for detailed comments
	 */
	public void searchAllMethodNames(
			final char[] qualifier,
			final int qualifierMatchRule,
			final char[] methodName,
			final int methodMatchRule,
			IJavaSearchScope scope,
			final IRestrictedAccessMethodRequestor nameRequestor,
			int waitingPolicy,
			IProgressMonitor progressMonitor)  throws JavaModelException {

			// Validate match rule first
			final int validatedMethodMatchRule = SearchPattern.validateMatchRule(methodName == null ? null : new String (methodName), methodMatchRule);
			// Debug
			if (VERBOSE) {
				Util.verbose("BasicSearchEngine.searchAllMethodDeclarations(char[] qualifier,  "//$NON-NLS-1$
						+ "char[] methodName, int methodMatchRule, IJavaSearchScope, IRestrictedAccessConstructorRequestor, int waitingPolicy, IProgressMonitor)"); //$NON-NLS-1$
				Util.verbose("	- qualifier name: "+(qualifier==null?"null":new String(qualifier))); //$NON-NLS-1$ //$NON-NLS-2$
				Util.verbose("	- method name: "+(methodName==null?"null":new String(methodName))); //$NON-NLS-1$ //$NON-NLS-2$
				Util.verbose("	- method match rule: "+getMatchRuleString(methodMatchRule)); //$NON-NLS-1$
				if (validatedMethodMatchRule != methodMatchRule) {
					Util.verbose("	- validated method match rule: "+getMatchRuleString(validatedMethodMatchRule)); //$NON-NLS-1$
				}
				Util.verbose("	- scope: "+scope); //$NON-NLS-1$
			}
			if (validatedMethodMatchRule == -1) return; // invalid match rule => return no results

			// Create pattern
			IndexManager indexManager = JavaModelManager.getIndexManager();
			final MethodDeclarationPattern pattern = new MethodDeclarationPattern(qualifier, methodName, methodMatchRule);

			// Get working copy path(s). Store in a single string in case of only one to optimize comparison in requestor
			final HashSet workingCopyPaths = new HashSet();
			String workingCopyPath = null;
			ICompilationUnit[] copies = getWorkingCopies();
			final int copiesLength = copies == null ? 0 : copies.length;
			if (copies != null) {
				if (copiesLength == 1) {
					workingCopyPath = copies[0].getPath().toString();
				} else {
					for (int i = 0; i < copiesLength; i++) {
						ICompilationUnit workingCopy = copies[i];
						workingCopyPaths.add(workingCopy.getPath().toString());
					}
				}
			}
			final String singleWkcpPath = workingCopyPath;

			// Index requestor
			IndexQueryRequestor searchRequestor = new IndexQueryRequestor(){
				@Override
				public boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord, SearchParticipant participant, AccessRuleSet access) {
					MethodDeclarationPattern record = (MethodDeclarationPattern)indexRecord;

					if ((record.extraFlags & ExtraFlags.IsLocalType) != 0) {
						return true; // filter out local and anonymous classes
					}
					switch (copiesLength) {
						case 0:
							break;
						case 1:
							if (singleWkcpPath.equals(documentPath)) {
								return true; // filter out *the* working copy
							}
							break;
						default:
							if (workingCopyPaths.contains(documentPath)) {
								return true; // filter out working copies
							}
							break;
					}

					// Accept document path
					AccessRestriction accessRestriction = null;
					if (access != null) {
						// Compute document relative path
						int pkgLength = (record.declaringPackageName==null || record.declaringPackageName.length==0) ? 0 : record.declaringPackageName.length+1;
						int qualificationLength = (record.declaringQualification == null || record.declaringQualification.length == 0) ? 0 : record.declaringQualification.length;
						int nameLength = record.declaringSimpleName==null ? 0 : record.declaringSimpleName.length;
						char[] path = new char[pkgLength + qualificationLength + nameLength];
						int pos = 0;
						if (pkgLength > 0) {
							System.arraycopy(record.declaringPackageName, 0, path, pos, pkgLength-1);
							CharOperation.replace(path, '.', '/');
							path[pkgLength-1] = '/';
							pos += pkgLength;
						}
						if (qualificationLength > 0) {
							System.arraycopy(record.declaringQualification, 0, path, pos, qualificationLength);
						}
						if (nameLength > 0) {
							System.arraycopy(record.declaringSimpleName, 0, path, pos, nameLength);
							pos += nameLength;
						}
						// Update access restriction if path is not empty
						if (pos > 0) {
							accessRestriction = access.getViolatedRestriction(path);
						}
					}
					if (match(qualifier, qualifierMatchRule, methodName, methodMatchRule,
							record.declaringPackageName, record.declaringQualification, record.declaringSimpleName, record.selector)) {
						nameRequestor.acceptMethod(
								record.selector,
								record.parameterCount,
								record.declaringQualification,
								record.declaringSimpleName,
								record.declaringTypeModifiers,
								record.declaringPackageName,
								record.signature,
								record.parameterTypes,
								record.parameterNames,
								record.returnSimpleName,
								record.modifiers,
								documentPath,
								accessRestriction,
								-1 /* method index not applicable as there is no IType here */);
					}
					return true;
				}
			};

			SubMonitor subMonitor = SubMonitor.convert(progressMonitor, Messages.engine_searching, 1000);
			// add type names from indexes
			indexManager.performConcurrentJob(
				new PatternSearchJob(
					pattern,
					getDefaultSearchParticipant(), // Java search only
					scope,
					searchRequestor),
				waitingPolicy,
				subMonitor.split(Math.max(1000-copiesLength, 0)));

			// add type names from working copies
			if (copies != null) {
				for (int i = 0; i < copiesLength; i++) {
					SubMonitor iterationMonitor = subMonitor.split(1);
					final ICompilationUnit workingCopy = copies[i];
					if (scope instanceof HierarchyScope) {
						if (!((HierarchyScope)scope).encloses(workingCopy, iterationMonitor)) continue;
					} else {
						if (!scope.encloses(workingCopy)) continue;
					}

					final String path = workingCopy.getPath().toString();
					if (workingCopy.isConsistent()) {
						IPackageDeclaration[] packageDeclarations = workingCopy.getPackageDeclarations();
						char[] packageDeclaration = packageDeclarations.length == 0 ? CharOperation.NO_CHAR : packageDeclarations[0].getElementName().toCharArray();

						IType[] allTypes = workingCopy.getAllTypes();
						for (int j = 0, allTypesLength = allTypes.length; j < allTypesLength; j++) {
							IType type = allTypes[j];
							IJavaElement parent = type.getParent();
							char[] rDeclaringQualification = parent instanceof IType ? ((IType) parent).getTypeQualifiedName('.').toCharArray() : CharOperation.NO_CHAR;
							char[] rSimpleName = type.getElementName().toCharArray();
							char[] q = CharOperation.concatNonEmpty(packageDeclaration, '.', rDeclaringQualification, '.', rSimpleName);
							if (!match(qualifier, qualifierMatchRule, q))
								continue;
							reportMatchingMethods(methodName, methodMatchRule, nameRequestor, path,
									packageDeclaration, type, rDeclaringQualification, rSimpleName);
						}
					} else {
						Parser basicParser = getParser();
						org.eclipse.jdt.internal.compiler.env.ICompilationUnit unit = (org.eclipse.jdt.internal.compiler.env.ICompilationUnit) workingCopy;
						CompilationResult compilationUnitResult = new CompilationResult(unit, 0, 0, this.compilerOptions.maxProblemsPerUnit);
						CompilationUnitDeclaration parsedUnit = basicParser.dietParse(unit, compilationUnitResult);
						if (parsedUnit != null) {
							final char[] packageDeclaration = parsedUnit.currentPackage == null ? CharOperation.NO_CHAR : CharOperation.concatWith(parsedUnit.currentPackage.getImportName(), '.');
							class AllMethodDeclarationVisitor extends ASTVisitor {

								class TypeInfo {
									public TypeDeclaration typeDecl;
									public IType type;
									public boolean visitMethods;
									public char[] enclosingTypeName;

									TypeInfo(TypeDeclaration typeDecl, boolean visitMethods, char[] enclosingTypeName) {
										this.typeDecl = typeDecl;
										this.type = workingCopy.getType(new String(typeDecl.name));
										this.visitMethods = visitMethods;
										this.enclosingTypeName = enclosingTypeName;
									}
								}
								Stack<TypeInfo> typeInfoStack = new Stack<>();
								IType getCurrentType() {
									int l = this.typeInfoStack.size();
									if (l <= 0) return null;
									TypeInfo typeInfo = this.typeInfoStack.get(0);
									IType type = typeInfo.type;
									if (type == null) {
										TypeInfo ti = this.typeInfoStack.get(0);
										ti.type = ti.type == null ? workingCopy.getType(new String(ti.typeDecl.name)) : ti.type;
										type = ti.type;
										for (int j = 1; j < l && type != null; ++j) {
											ti = this.typeInfoStack.get(j);
											if (ti.type == null) {
												ti.type = type.getType(new String(ti.typeDecl.name));
											}
											type = ti.type;
										}
									}
									return type;
								}

								private void addStackEntry(TypeDeclaration typeDeclaration, char[] enclosingTypeName) {
									char[] q = CharOperation.concatNonEmpty(packageDeclaration, '.', enclosingTypeName, '.', typeDeclaration.name);
									boolean visitMethods = match(qualifier, qualifierMatchRule, q);
									this.typeInfoStack.push(new TypeInfo(typeDeclaration, visitMethods, enclosingTypeName));
								}
								@Override
								public void endVisit(TypeDeclaration typeDeclaration, CompilationUnitScope s) {
									this.typeInfoStack.pop();
								}
								@Override
								public void endVisit(TypeDeclaration memberTypeDeclaration, ClassScope s) {
									this.typeInfoStack.pop();
								}
								@Override
								public boolean visit(MethodDeclaration methodDeclaration, ClassScope classScope) {
									TypeInfo typeInfo = this.typeInfoStack.peek();
									if (typeInfo.visitMethods &&
										match(methodName, methodMatchRule, methodDeclaration.selector)) {
										reportMatchingMethod(path, packageDeclaration,
												typeInfo.enclosingTypeName,
												typeInfo.typeDecl,
												methodDeclaration,
												getCurrentType(),
												nameRequestor);
									}

									return false; // no need to find methods from local/anonymous type
								}
								@Override
								public boolean visit(TypeDeclaration typeDeclaration, BlockScope blockScope) {
									return false; // do not visit local/anonymous types
								}
								@Override
								public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope s) {
									addStackEntry(typeDeclaration, CharOperation.NO_CHAR);
									return true;
								}
								@Override
								public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope s) {
									TypeInfo typeInfo = this.typeInfoStack.peek();
									addStackEntry(memberTypeDeclaration, typeInfo.enclosingTypeName == CharOperation.NO_CHAR ? typeInfo.typeDecl.name :
											CharOperation.concat(typeInfo.enclosingTypeName, typeInfo.typeDecl.name, '.'));
									return true;
								}
							}
							parsedUnit.traverse(new AllMethodDeclarationVisitor(), parsedUnit.scope);
						}
					}
				}
			}
		}

	/**
	 * Searches for all method declarations in the given scope.
	 * 	 * <p>
	 * Warning: This API is in experimental phase and may be modified/removed. Do not use this until this
	 * comment is removed.
	 * </p>
	 *
	 * @see SearchEngine#searchAllMethodNames(char[], int, char[], int, char[], int, char[], int, IJavaSearchScope, MethodNameMatchRequestor, int, IProgressMonitor)
	 * for detailed comments
	 */
	public void searchAllMethodNames(
			final char[] packageName,
			final int pkgMatchRule,
			final char[] declaringQualification,
			final int declQualificationMatchRule,
			final char[] declaringSimpleName,
			final int declSimpleNameMatchRule,
			final char[] methodName,
			final int methodMatchRule,
			IJavaSearchScope scope,
			final IRestrictedAccessMethodRequestor nameRequestor,
			int waitingPolicy,
			IProgressMonitor progressMonitor)  throws JavaModelException {

			// Validate match rule first
			final int validatedMethodMatchRule = SearchPattern.validateMatchRule(methodName == null ? null : new String (methodName), methodMatchRule);
			// Debug
			if (VERBOSE) {
				Util.verbose("BasicSearchEngine.searchAllMethodDeclarations(char[] packageName, char[] declaringQualification, char[] declaringSimpleName, "//$NON-NLS-1$
						+ "char[] methodName, int methodMatchRule, IJavaSearchScope, IRestrictedAccessConstructorRequestor, int waitingPolicy, IProgressMonitor)"); //$NON-NLS-1$
				Util.verbose("	- package name: "+(packageName==null?"null":new String(packageName))); //$NON-NLS-1$ //$NON-NLS-2$
				Util.verbose("	- declaringQualification name: "+(declaringQualification==null?"null":new String(declaringQualification))); //$NON-NLS-1$ //$NON-NLS-2$
				Util.verbose("	- declaringSimple name: "+(declaringSimpleName==null?"null":new String(declaringSimpleName))); //$NON-NLS-1$ //$NON-NLS-2$
				Util.verbose("	- method name: "+(methodName==null?"null":new String(methodName))); //$NON-NLS-1$ //$NON-NLS-2$
				Util.verbose("	- method match rule: "+getMatchRuleString(methodMatchRule)); //$NON-NLS-1$
				if (validatedMethodMatchRule != methodMatchRule) {
					Util.verbose("	- validated method match rule: "+getMatchRuleString(validatedMethodMatchRule)); //$NON-NLS-1$
				}
				Util.verbose("	- scope: "+scope); //$NON-NLS-1$
			}
			if (validatedMethodMatchRule == -1) return; // invalid match rule => return no results

			// Create pattern
			IndexManager indexManager = JavaModelManager.getIndexManager();
			final MethodDeclarationPattern pattern = new MethodDeclarationPattern(packageName, declaringQualification, declaringSimpleName, methodName, methodMatchRule);

			// Get working copy path(s). Store in a single string in case of only one to optimize comparison in requestor
			final HashSet workingCopyPaths = new HashSet();
			String workingCopyPath = null;
			ICompilationUnit[] copies = getWorkingCopies();
			final int copiesLength = copies == null ? 0 : copies.length;
			if (copies != null) {
				if (copiesLength == 1) {
					workingCopyPath = copies[0].getPath().toString();
				} else {
					for (int i = 0; i < copiesLength; i++) {
						ICompilationUnit workingCopy = copies[i];
						workingCopyPaths.add(workingCopy.getPath().toString());
					}
				}
			}
			final String singleWkcpPath = workingCopyPath;

			// Index requestor
			IndexQueryRequestor searchRequestor = new IndexQueryRequestor(){
				@Override
				public boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord, SearchParticipant participant, AccessRuleSet access) {
					MethodDeclarationPattern record = (MethodDeclarationPattern)indexRecord;

					if ((record.extraFlags & ExtraFlags.IsLocalType) != 0) {
						return true; // filter out local and anonymous classes
					}
					switch (copiesLength) {
						case 0:
							break;
						case 1:
							if (singleWkcpPath.equals(documentPath)) {
								return true; // filter out *the* working copy
							}
							break;
						default:
							if (workingCopyPaths.contains(documentPath)) {
								return true; // filter out working copies
							}
							break;
					}

					// Accept document path
					AccessRestriction accessRestriction = null;
					if (access != null) {
						// Compute document relative path
						int pkgLength = (record.declaringPackageName==null || record.declaringPackageName.length==0) ? 0 : record.declaringPackageName.length+1;
						int qualificationLength = (record.declaringQualification == null || record.declaringQualification.length == 0) ? 0 : record.declaringQualification.length;
						int nameLength = record.declaringSimpleName==null ? 0 : record.declaringSimpleName.length;
						char[] path = new char[pkgLength + qualificationLength + nameLength];
						int pos = 0;
						if (pkgLength > 0) {
							System.arraycopy(record.declaringPackageName, 0, path, pos, pkgLength-1);
							CharOperation.replace(path, '.', '/');
							path[pkgLength-1] = '/';
							pos += pkgLength;
						}
						if (qualificationLength > 0) {
							System.arraycopy(record.declaringQualification, 0, path, pos, qualificationLength);
						}
						if (nameLength > 0) {
							System.arraycopy(record.declaringSimpleName, 0, path, pos, nameLength);
							pos += nameLength;
						}
						// Update access restriction if path is not empty
						if (pos > 0) {
							accessRestriction = access.getViolatedRestriction(path);
						}
					}
					if (match(packageName, pkgMatchRule, declaringQualification, declQualificationMatchRule, declaringSimpleName, declSimpleNameMatchRule, methodName, methodMatchRule,
							record.declaringPackageName, record.declaringQualification, record.declaringSimpleName, record.selector)) {
						nameRequestor.acceptMethod(
								record.selector,
								record.parameterCount,
								record.declaringQualification,
								record.declaringSimpleName,
								record.declaringTypeModifiers,
								record.declaringPackageName,
								record.signature,
								record.parameterTypes,
								record.parameterNames,
								record.returnSimpleName,
								record.modifiers,
								documentPath,
								accessRestriction,
								-1 /* method index not applicable as there is no IType here */);
					}
					return true;
				}
			};

			SubMonitor subMonitor = SubMonitor.convert(progressMonitor, Messages.engine_searching, 1000);
			// add type names from indexes
			indexManager.performConcurrentJob(
				new PatternSearchJob(
					pattern,
					getDefaultSearchParticipant(), // Java search only
					scope,
					searchRequestor),
				waitingPolicy,
				subMonitor.split(Math.max(1000-copiesLength, 0)));

			// add type names from working copies
			if (copies != null) {
				boolean isPkgCaseSensitive = (pkgMatchRule & SearchPattern.R_CASE_SENSITIVE) != 0;
				for (int i = 0; i < copiesLength; i++) {
					SubMonitor iterationMonitor = subMonitor.split(1);
					final ICompilationUnit workingCopy = copies[i];
					if (scope instanceof HierarchyScope) {
						if (!((HierarchyScope)scope).encloses(workingCopy, iterationMonitor)) continue;
					} else {
						if (!scope.encloses(workingCopy)) continue;
					}

					final String path = workingCopy.getPath().toString();
					if (workingCopy.isConsistent()) {
						IPackageDeclaration[] packageDeclarations = workingCopy.getPackageDeclarations();
						char[] packageDeclaration = packageDeclarations.length == 0 ? CharOperation.NO_CHAR : packageDeclarations[0].getElementName().toCharArray();
						if (packageName != null && !CharOperation.equals(packageName, packageDeclaration, isPkgCaseSensitive))
							continue;

						IType[] allTypes = workingCopy.getAllTypes();
						for (int j = 0, allTypesLength = allTypes.length; j < allTypesLength; j++) {
							IType type = allTypes[j];
							IJavaElement parent = type.getParent();
							char[] rDeclaringQualification = parent instanceof IType ? ((IType) parent).getTypeQualifiedName('.').toCharArray() : CharOperation.NO_CHAR;
							char[] rSimpleName = type.getElementName().toCharArray();
							if (!match(declaringQualification, declQualificationMatchRule, rDeclaringQualification) ||
									!match(declaringSimpleName, declSimpleNameMatchRule, rSimpleName))
								continue;
							reportMatchingMethods(methodName, methodMatchRule, nameRequestor, path,
									packageDeclaration, type, rDeclaringQualification, rSimpleName);
						}
					} else {
						Parser basicParser = getParser();
						org.eclipse.jdt.internal.compiler.env.ICompilationUnit unit = (org.eclipse.jdt.internal.compiler.env.ICompilationUnit) workingCopy;
						CompilationResult compilationUnitResult = new CompilationResult(unit, 0, 0, this.compilerOptions.maxProblemsPerUnit);
						CompilationUnitDeclaration parsedUnit = basicParser.dietParse(unit, compilationUnitResult);
						if (parsedUnit != null) {
							final char[] packageDeclaration = parsedUnit.currentPackage == null ? CharOperation.NO_CHAR : CharOperation.concatWith(parsedUnit.currentPackage.getImportName(), '.');
							class AllMethodDeclarationVisitor extends ASTVisitor {

								class TypeInfo {
									public TypeDeclaration typeDecl;
									public IType type;
									public boolean visitMethods;
									public char[] enclosingTypeName;

									TypeInfo(TypeDeclaration typeDecl, boolean visitMethods, char[] enclosingTypeName) {
										this.typeDecl = typeDecl;
										this.type = workingCopy.getType(new String(typeDecl.name));
										this.visitMethods = visitMethods;
										this.enclosingTypeName = enclosingTypeName;
									}
								}
								Stack<TypeInfo> typeInfoStack = new Stack<>();
								IType getCurrentType() {
									int l = this.typeInfoStack.size();
									if (l <= 0) return null;
									TypeInfo typeInfo = this.typeInfoStack.get(0);
									IType type = typeInfo.type;
									if (type == null) {
										TypeInfo ti = this.typeInfoStack.get(0);
										ti.type = ti.type == null ? workingCopy.getType(new String(ti.typeDecl.name)) : ti.type;
										type = ti.type;
										for (int j = 1; j < l && type != null; ++j) {
											ti = this.typeInfoStack.get(j);
											if (ti.type == null) {
												ti.type = type.getType(new String(ti.typeDecl.name));
											}
											type = ti.type;
										}
									}
									return type;
								}

								private void addStackEntry(TypeDeclaration typeDeclaration, char[] enclosingTypeName) {
									boolean visitMethods = match(declaringQualification, declQualificationMatchRule, enclosingTypeName) &&
											match(declaringSimpleName, declSimpleNameMatchRule, typeDeclaration.name);
									this.typeInfoStack.push(new TypeInfo(typeDeclaration, visitMethods, enclosingTypeName));
								}
								@Override
								public void endVisit(TypeDeclaration typeDeclaration, CompilationUnitScope s) {
									this.typeInfoStack.pop();
								}
								@Override
								public void endVisit(TypeDeclaration memberTypeDeclaration, ClassScope s) {
									this.typeInfoStack.pop();
								}
								@Override
								public boolean visit(MethodDeclaration methodDeclaration, ClassScope classScope) {
									TypeInfo typeInfo = this.typeInfoStack.peek();
									if (typeInfo.visitMethods &&
										match(methodName, methodMatchRule, methodDeclaration.selector)) {
										reportMatchingMethod(path, packageDeclaration,
												typeInfo.enclosingTypeName,
												typeInfo.typeDecl,
												methodDeclaration,
												getCurrentType(),
												nameRequestor);
									}

									return false; // no need to find methods from local/anonymous type
								}
								@Override
								public boolean visit(TypeDeclaration typeDeclaration, BlockScope blockScope) {
									return false; // do not visit local/anonymous types
								}
								@Override
								public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope s) {
									addStackEntry(typeDeclaration, CharOperation.NO_CHAR);
									return true;
								}
								@Override
								public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope s) {
									TypeInfo typeInfo = this.typeInfoStack.peek();
									addStackEntry(memberTypeDeclaration, typeInfo.enclosingTypeName == CharOperation.NO_CHAR ? typeInfo.typeDecl.name :
											CharOperation.concat(typeInfo.enclosingTypeName, typeInfo.typeDecl.name, '.'));
									return true;
								}
							}
							if (match(packageName, pkgMatchRule, packageDeclaration))
								parsedUnit.traverse(new AllMethodDeclarationVisitor(), parsedUnit.scope);
						}
					}
				}
			}
		}

	void reportMatchingMethod(
			final String path,
			final char[] packageDeclaration,
			final char[] declaringQualifier,
			final TypeDeclaration typeDeclaration,
			final MethodDeclaration methodDeclaration,
			final IType type,
			final IRestrictedAccessMethodRequestor nameRequestor) {

		Argument[] arguments = methodDeclaration.arguments;
		int argsLength = 0;
		char[][] parameterTypes = CharOperation.NO_CHAR_CHAR;
		char[][] parameterNames = CharOperation.NO_CHAR_CHAR;
		if (arguments != null) {
			argsLength = arguments.length;
			parameterTypes = new char[argsLength][];
			parameterNames = new char[argsLength][];
		}
		for (int i = 0; i < argsLength; ++i) {
			Argument argument = arguments[i];
			parameterNames[i] = argument.name;
			parameterTypes[i] = CharOperation.concatWith(argument.type.getTypeName(), '.');
		}
		if (nameRequestor instanceof MethodNameMatchRequestorWrapper) {
			IMethod method = type.getMethod(new String(methodDeclaration.selector), CharOperation.toStrings(parameterTypes));
			((MethodNameMatchRequestorWrapper)nameRequestor).requestor.acceptMethodNameMatch(new JavaSearchMethodNameMatch(method, methodDeclaration.modifiers));
		} else {
			char[] returnType = CharOperation.toString(methodDeclaration.returnType.getTypeName()).toCharArray();
			nameRequestor.acceptMethod(
					methodDeclaration.selector,
					argsLength,
					declaringQualifier,
					typeDeclaration.name,
					typeDeclaration.modifiers,
					packageDeclaration,
					null,
					parameterTypes,
					parameterNames,
					returnType,
					methodDeclaration.modifiers,
					path,
					null,
					-1 /* method index */);
		}
	}
	void reportMatchingMethods(final char[] methodName, final int methodMatchRule,
			final IRestrictedAccessMethodRequestor nameRequestor, final String path, char[] packageDeclaration,
			IType type, char[] rDeclaringQualification, char[] rSimpleName)
					throws JavaModelException {
		IMethod[] methods = type.getMethods();

		for (int k = 0; k < methods.length; k++) {
			IMethod method = methods[k];
			if (method.isConstructor()) continue;

			char[] rMethodName = method.getElementName().toCharArray();
			if (match(methodName, methodMatchRule, rMethodName)) {
				if (nameRequestor instanceof MethodNameMatchRequestorWrapper) {
					((MethodNameMatchRequestorWrapper) nameRequestor).requestor.acceptMethodNameMatch(new JavaSearchMethodNameMatch(method, method.getFlags()));
				} else {

					String[] stringParameterNames = method.getParameterNames();
					String[] stringParameterTypes = method.getParameterTypes();
					int length = stringParameterNames.length;
					char[][] parameterNames = new char[length][];
					char[][] parameterTypes = new char[length][];
					for (int l = 0; l < length; l++) {
						parameterNames[l] = stringParameterNames[l].toCharArray();
						parameterTypes[l] = Signature.toCharArray(Signature.getTypeErasure(stringParameterTypes[l]).toCharArray());
					}
					String returnSignature = method.getReturnType();
					char[] signature = returnSignature.toCharArray();
					char[] returnErasure = Signature.toCharArray(Signature.getTypeErasure(signature));
					CharOperation.replace(returnErasure, '$', '.');
					char[] returnTypeName =  returnErasure;

					nameRequestor.acceptMethod(
							rMethodName,
							parameterNames.length,
							rDeclaringQualification,
							rSimpleName,
							type.getFlags(),
							packageDeclaration,
							null, // signature not used for source
							parameterTypes,
							parameterNames,
							returnTypeName,
							method.getFlags(),
							path,
							null,
							k);
				}
			}
		}
	}

	/**
	 * Searches for all secondary types in the given scope.
	 * The search can be selecting specific types (given a package or a type name
	 * prefix and match modes).
	 */
	public void searchAllSecondaryTypeNames(
			IPackageFragmentRoot[] sourceFolders,
			final IRestrictedAccessTypeRequestor nameRequestor,
			boolean waitForIndexes,
			IProgressMonitor progressMonitor)  throws JavaModelException {

		try {
			if (VERBOSE) {
				Util.verbose("BasicSearchEngine.searchAllSecondaryTypeNames(IPackageFragmentRoot[], IRestrictedAccessTypeRequestor, boolean, IProgressMonitor)"); //$NON-NLS-1$
				StringBuilder buffer = new StringBuilder("	- source folders: "); //$NON-NLS-1$
				int length = sourceFolders.length;
				for (int i=0; i<length; i++) {
					if (i==0) {
						buffer.append('[');
					} else {
						buffer.append(',');
					}
					buffer.append(sourceFolders[i].getElementName());
				}
				buffer.append("]\n	- waitForIndexes: "); //$NON-NLS-1$
				buffer.append(waitForIndexes);
				Util.verbose(buffer.toString());
			}

			IndexManager indexManager = JavaModelManager.getIndexManager();
			final TypeDeclarationPattern pattern = new SecondaryTypeDeclarationPattern();

			// Get working copy path(s). Store in a single string in case of only one to optimize comparison in requestor
			final HashSet workingCopyPaths = new HashSet();
			String workingCopyPath = null;
			ICompilationUnit[] copies = getWorkingCopies();
			final int copiesLength = copies == null ? 0 : copies.length;
			if (copies != null) {
				if (copiesLength == 1) {
					workingCopyPath = copies[0].getPath().toString();
				} else {
					for (int i = 0; i < copiesLength; i++) {
						ICompilationUnit workingCopy = copies[i];
						workingCopyPaths.add(workingCopy.getPath().toString());
					}
				}
			}
			final String singleWkcpPath = workingCopyPath;

			// Index requestor
			IndexQueryRequestor searchRequestor = new IndexQueryRequestor(){
				@Override
				public boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord, SearchParticipant participant, AccessRuleSet access) {
					// Filter unexpected types
					TypeDeclarationPattern record = (TypeDeclarationPattern)indexRecord;
					if (!record.secondary) {
						return true; // filter maint types
					}
					if (record.enclosingTypeNames == IIndexConstants.ONE_ZERO_CHAR) {
						return true; // filter out local and anonymous classes
					}
					switch (copiesLength) {
						case 0:
							break;
						case 1:
							if (singleWkcpPath.equals(documentPath)) {
								return true; // fliter out *the* working copy
							}
							break;
						default:
							if (workingCopyPaths.contains(documentPath)) {
								return true; // filter out working copies
							}
							break;
					}

					// Accept document path
					AccessRestriction accessRestriction = null;
					if (access != null) {
						// Compute document relative path
						int pkgLength = (record.pkg==null || record.pkg.length==0) ? 0 : record.pkg.length+1;
						int nameLength = record.simpleName==null ? 0 : record.simpleName.length;
						char[] path = new char[pkgLength+nameLength];
						int pos = 0;
						if (pkgLength > 0) {
							System.arraycopy(record.pkg, 0, path, pos, pkgLength-1);
							CharOperation.replace(path, '.', '/');
							path[pkgLength-1] = '/';
							pos += pkgLength;
						}
						if (nameLength > 0) {
							System.arraycopy(record.simpleName, 0, path, pos, nameLength);
							pos += nameLength;
						}
						// Update access restriction if path is not empty
						if (pos > 0) {
							accessRestriction = access.getViolatedRestriction(path);
						}
					}
					nameRequestor.acceptType(record.modifiers, record.pkg, record.simpleName, record.enclosingTypeNames, documentPath, accessRestriction);
					return true;
				}
			};

			// add type names from indexes
			try {
				SubMonitor subMonitor = SubMonitor.convert(progressMonitor, Messages.engine_searching, 100);
				indexManager.performConcurrentJob(
					new PatternSearchJob(
						pattern,
						getDefaultSearchParticipant(), // Java search only
						createJavaSearchScope(sourceFolders),
						searchRequestor),
					waitForIndexes
						? IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH
						: IJavaSearchConstants.FORCE_IMMEDIATE_SEARCH,
					subMonitor.split(100));
			} catch (OperationCanceledException oce) {
				// do nothing
			}
		} finally {
			if (progressMonitor != null) {
				progressMonitor.done();
			}
		}
	}

	/**
	 * Searches for all top-level types and member types in the given scope.
	 * The search can be selecting specific types (given a package or a type name
	 * prefix and match modes).
	 *
	 * @see SearchEngine#searchAllTypeNames(char[], int, char[], int, int, IJavaSearchScope, TypeNameRequestor, int, IProgressMonitor)
	 * 	for detailed comment
	 */
	public void searchAllTypeNames(
			final char[] packageName,
			final int packageMatchRule,
			final char[] typeName,
			final int typeMatchRule,
			int searchFor,
			IJavaSearchScope scope,
			final IRestrictedAccessTypeRequestor nameRequestor,
			int waitingPolicy,
			IProgressMonitor progressMonitor)  throws JavaModelException {
		searchAllTypeNames(
				packageName,
				packageMatchRule,
				typeName,
				typeMatchRule,
				searchFor,
				scope,
				true,
				nameRequestor,
				waitingPolicy,
				progressMonitor);
	}

	/**
	 * Searches for all top-level types and member types in the given scope.
	 * The search can be selecting specific types (given a package or a type name
	 * prefix and match modes).
	 *
	 * @param resolveDocumentName used to tell SearchEngine whether to resolve
	 *                            the document name for each result entry.
	 *
	 * @see SearchEngine#searchAllTypeNames(char[], int, char[], int, int, IJavaSearchScope, TypeNameRequestor, int, IProgressMonitor)
	 * 	for detailed comment
	 */
	public void searchAllTypeNames(
		final char[] packageName,
		final int packageMatchRule,
		final char[] typeName,
		final int typeMatchRule,
		int searchFor,
		IJavaSearchScope scope,
		final boolean resolveDocumentName,
		final IRestrictedAccessTypeRequestor nameRequestor,
		int waitingPolicy,
		IProgressMonitor progressMonitor)  throws JavaModelException {

		try {
			// Validate match rule first
			final int validatedTypeMatchRule = SearchPattern.validateMatchRule(typeName == null ? null : new String (typeName), typeMatchRule);

			// Debug
			if (VERBOSE) {
				Util.verbose("BasicSearchEngine.searchAllTypeNames(char[], char[], int, int, IJavaSearchScope, IRestrictedAccessTypeRequestor, int, IProgressMonitor)"); //$NON-NLS-1$
				Util.verbose("	- package name: "+(packageName==null?"null":new String(packageName))); //$NON-NLS-1$ //$NON-NLS-2$
				Util.verbose("	- package match rule: "+getMatchRuleString(packageMatchRule)); //$NON-NLS-1$
				Util.verbose("	- type name: "+(typeName==null?"null":new String(typeName))); //$NON-NLS-1$ //$NON-NLS-2$
				Util.verbose("	- type match rule: "+getMatchRuleString(typeMatchRule)); //$NON-NLS-1$
				if (validatedTypeMatchRule != typeMatchRule) {
					Util.verbose("	- validated type match rule: "+getMatchRuleString(validatedTypeMatchRule)); //$NON-NLS-1$
				}
				Util.verbose("	- search for: "+searchFor); //$NON-NLS-1$
				Util.verbose("	- scope: "+scope); //$NON-NLS-1$
			}
			if (validatedTypeMatchRule == -1) return; // invalid match rule => return no results

			// Create pattern
			IndexManager indexManager = JavaModelManager.getIndexManager();
			final char typeSuffix;
			switch(searchFor){
				case IJavaSearchConstants.CLASS :
					typeSuffix = IIndexConstants.CLASS_SUFFIX;
					break;
				case IJavaSearchConstants.CLASS_AND_INTERFACE :
					typeSuffix = IIndexConstants.CLASS_AND_INTERFACE_SUFFIX;
					break;
				case IJavaSearchConstants.CLASS_AND_ENUM :
					typeSuffix = IIndexConstants.CLASS_AND_ENUM_SUFFIX;
					break;
				case IJavaSearchConstants.INTERFACE :
					typeSuffix = IIndexConstants.INTERFACE_SUFFIX;
					break;
				case IJavaSearchConstants.INTERFACE_AND_ANNOTATION :
					typeSuffix = IIndexConstants.INTERFACE_AND_ANNOTATION_SUFFIX;
					break;
				case IJavaSearchConstants.ENUM :
					typeSuffix = IIndexConstants.ENUM_SUFFIX;
					break;
				case IJavaSearchConstants.ANNOTATION_TYPE :
					typeSuffix = IIndexConstants.ANNOTATION_TYPE_SUFFIX;
					break;
				default :
					typeSuffix = IIndexConstants.TYPE_SUFFIX;
					break;
			}
			final TypeDeclarationPattern pattern = packageMatchRule == SearchPattern.R_EXACT_MATCH
				? new TypeDeclarationPattern(
					packageName,
					null,
					typeName,
					typeSuffix,
					validatedTypeMatchRule)
				: new QualifiedTypeDeclarationPattern(
					packageName,
					packageMatchRule,
					typeName,
					typeSuffix,
					validatedTypeMatchRule);

			// Get working copy path(s). Store in a single string in case of only one to optimize comparison in requestor
			final HashSet workingCopyPaths = new HashSet();
			String workingCopyPath = null;
			ICompilationUnit[] copies = getWorkingCopies();
			final int copiesLength = copies == null ? 0 : copies.length;
			if (copies != null) {
				if (copiesLength == 1) {
					workingCopyPath = copies[0].getPath().toString();
				} else {
					for (int i = 0; i < copiesLength; i++) {
						ICompilationUnit workingCopy = copies[i];
						workingCopyPaths.add(workingCopy.getPath().toString());
					}
				}
			}
			final String singleWkcpPath = workingCopyPath;

			// Index requestor
			IndexQueryRequestor searchRequestor = new IndexQueryRequestor(){
				@Override
				public boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord, SearchParticipant participant, AccessRuleSet access) {
					// Filter unexpected types
					TypeDeclarationPattern record = (TypeDeclarationPattern)indexRecord;
					if (record.enclosingTypeNames == IIndexConstants.ONE_ZERO_CHAR) {
						return true; // filter out local and anonymous classes
					}
					switch (copiesLength) {
						case 0:
							break;
						case 1:
							if (singleWkcpPath.equals(documentPath)) {
								return true; // filter out *the* working copy
							}
							break;
						default:
							if (workingCopyPaths.contains(documentPath)) {
								return true; // filter out working copies
							}
							break;
					}

					// Accept document path
					AccessRestriction accessRestriction = null;
					if (access != null) {
						// Compute document relative path
						int pkgLength = (record.pkg==null || record.pkg.length==0) ? 0 : record.pkg.length+1;
						int nameLength = record.simpleName==null ? 0 : record.simpleName.length;
						char[] path = new char[pkgLength+nameLength];
						int pos = 0;
						if (pkgLength > 0) {
							System.arraycopy(record.pkg, 0, path, pos, pkgLength-1);
							CharOperation.replace(path, '.', '/');
							path[pkgLength-1] = '/';
							pos += pkgLength;
						}
						if (nameLength > 0) {
							System.arraycopy(record.simpleName, 0, path, pos, nameLength);
							pos += nameLength;
						}
						// Update access restriction if path is not empty
						if (pos > 0) {
							accessRestriction = access.getViolatedRestriction(path);
						}
					}
					if (match(record.typeSuffix, record.modifiers)) {
						nameRequestor.acceptType(record.modifiers, record.pkg, record.simpleName, record.enclosingTypeNames, documentPath, accessRestriction);
					}
					return true;
				}
			};

			SubMonitor subMonitor = SubMonitor.convert(progressMonitor, Messages.engine_searching, 1000);

			// add type names from indexes
			indexManager.performConcurrentJob(
				new PatternSearchJob(
					pattern,
					getDefaultSearchParticipant(), // Java search only
					scope,
					resolveDocumentName,
					true,
					searchRequestor),
				waitingPolicy,
				subMonitor.split(Math.max(1000-copiesLength, 0)));

			// add type names from working copies
			if (copies != null) {
				for (int i = 0; i < copiesLength; i++) {
					SubMonitor iterationMonitor = subMonitor.split(i);
					final ICompilationUnit workingCopy = copies[i];
					if (scope instanceof HierarchyScope) {
						if (!((HierarchyScope)scope).encloses(workingCopy, iterationMonitor)) continue;
					} else {
						if (!scope.encloses(workingCopy)) continue;
					}
					final String path = workingCopy.getPath().toString();
					if (workingCopy.isConsistent()) {
						IPackageDeclaration[] packageDeclarations = workingCopy.getPackageDeclarations();
						char[] packageDeclaration = packageDeclarations.length == 0 ? CharOperation.NO_CHAR : packageDeclarations[0].getElementName().toCharArray();
						IType[] allTypes = workingCopy.getAllTypes();
						for (int j = 0, allTypesLength = allTypes.length; j < allTypesLength; j++) {
							IType type = allTypes[j];
							IJavaElement parent = type.getParent();
							char[][] enclosingTypeNames;
							if (parent instanceof IType) {
								char[] parentQualifiedName = ((IType)parent).getTypeQualifiedName('.').toCharArray();
								enclosingTypeNames = CharOperation.splitOn('.', parentQualifiedName);
							} else {
								enclosingTypeNames = CharOperation.NO_CHAR_CHAR;
							}
							char[] simpleName = type.getElementName().toCharArray();
							int kind;
							if (type.isEnum()) {
								kind = TypeDeclaration.ENUM_DECL;
							} else if (type.isAnnotation()) {
								kind = TypeDeclaration.ANNOTATION_TYPE_DECL;
							}	else if (type.isClass() || type.isRecord()) {
								kind = TypeDeclaration.CLASS_DECL;
							} else /*if (type.isInterface())*/ {
								kind = TypeDeclaration.INTERFACE_DECL;
							}
							if (match(typeSuffix, packageName, packageMatchRule, typeName, validatedTypeMatchRule, kind, packageDeclaration, simpleName)) {
								if (nameRequestor instanceof TypeNameMatchRequestorWrapper) {
									((TypeNameMatchRequestorWrapper)nameRequestor).requestor.acceptTypeNameMatch(new JavaSearchTypeNameMatch(type, type.getFlags()));
								} else {
									nameRequestor.acceptType(type.getFlags(), packageDeclaration, simpleName, enclosingTypeNames, path, null);
								}
							}
						}
					} else {
						Parser basicParser = getParser();
						org.eclipse.jdt.internal.compiler.env.ICompilationUnit unit = (org.eclipse.jdt.internal.compiler.env.ICompilationUnit) workingCopy;
						CompilationResult compilationUnitResult = new CompilationResult(unit, 0, 0, this.compilerOptions.maxProblemsPerUnit);
						CompilationUnitDeclaration parsedUnit = basicParser.dietParse(unit, compilationUnitResult);
						if (parsedUnit != null) {
							final char[] packageDeclaration = parsedUnit.currentPackage == null ? CharOperation.NO_CHAR : CharOperation.concatWith(parsedUnit.currentPackage.getImportName(), '.');
							class AllTypeDeclarationsVisitor extends ASTVisitor {
								@Override
								public boolean visit(TypeDeclaration typeDeclaration, BlockScope blockScope) {
									return false; // no local/anonymous type
								}
								@Override
								public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope compilationUnitScope) {
									if (match(typeSuffix, packageName, packageMatchRule, typeName, validatedTypeMatchRule, TypeDeclaration.kind(typeDeclaration.modifiers), packageDeclaration, typeDeclaration.name)) {
										if (nameRequestor instanceof TypeNameMatchRequestorWrapper) {
											IType type = workingCopy.getType(new String(typeName));
											((TypeNameMatchRequestorWrapper)nameRequestor).requestor.acceptTypeNameMatch(new JavaSearchTypeNameMatch(type, typeDeclaration.modifiers));
										} else {
											nameRequestor.acceptType(typeDeclaration.modifiers, packageDeclaration, typeDeclaration.name, CharOperation.NO_CHAR_CHAR, path, null);
										}
									}
									return true;
								}
								@Override
								public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope classScope) {
									if (match(typeSuffix, packageName, packageMatchRule, typeName, validatedTypeMatchRule, TypeDeclaration.kind(memberTypeDeclaration.modifiers), packageDeclaration, memberTypeDeclaration.name)) {
										// compute enclosing type names
										TypeDeclaration enclosing = memberTypeDeclaration.enclosingType;
										char[][] enclosingTypeNames = CharOperation.NO_CHAR_CHAR;
										while (enclosing != null) {
											enclosingTypeNames = CharOperation.arrayConcat(new char[][] {enclosing.name}, enclosingTypeNames);
											if ((enclosing.bits & ASTNode.IsMemberType) != 0) {
												enclosing = enclosing.enclosingType;
											} else {
												enclosing = null;
											}
										}
										// report
										if (nameRequestor instanceof TypeNameMatchRequestorWrapper) {
											IType type = workingCopy.getType(new String(enclosingTypeNames[0]));
											for (int j=1, l=enclosingTypeNames.length; j<l; j++) {
												type = type.getType(new String(enclosingTypeNames[j]));
											}
											((TypeNameMatchRequestorWrapper)nameRequestor).requestor.acceptTypeNameMatch(new JavaSearchTypeNameMatch(type, 0));
										} else {
											nameRequestor.acceptType(memberTypeDeclaration.modifiers, packageDeclaration, memberTypeDeclaration.name, enclosingTypeNames, path, null);
										}
									}
									return true;
								}
							}
							parsedUnit.traverse(new AllTypeDeclarationsVisitor(), parsedUnit.scope);
						}
					}
				}
			}
		} finally {
			if (progressMonitor != null) {
				progressMonitor.done();
			}
		}
	}

	/**
	 * Searches for all top-level types and member types in the given scope using  a case sensitive exact match
	 * with the given qualified names and type names.
	 *
	 * @see SearchEngine#searchAllTypeNames(char[][], char[][], IJavaSearchScope, TypeNameRequestor, int, IProgressMonitor)
	 * 	for detailed comment
	 */
	public void searchAllTypeNames(
		final char[][] qualifications,
		final char[][] typeNames,
		final int matchRule,
		int searchFor,
		IJavaSearchScope scope,
		final IRestrictedAccessTypeRequestor nameRequestor,
		int waitingPolicy,
		IProgressMonitor progressMonitor)  throws JavaModelException {

		try {
			// Debug
			if (VERBOSE) {
				Util.verbose("BasicSearchEngine.searchAllTypeNames(char[][], char[][], int, int, IJavaSearchScope, IRestrictedAccessTypeRequestor, int, IProgressMonitor)"); //$NON-NLS-1$
				Util.verbose("	- package name: "+(qualifications==null?"null":new String(CharOperation.concatWith(qualifications, ',')))); //$NON-NLS-1$ //$NON-NLS-2$
				Util.verbose("	- type name: "+(typeNames==null?"null":new String(CharOperation.concatWith(typeNames, ',')))); //$NON-NLS-1$ //$NON-NLS-2$
				Util.verbose("	- match rule: "+getMatchRuleString(matchRule)); //$NON-NLS-1$
				Util.verbose("	- search for: "+searchFor); //$NON-NLS-1$
				Util.verbose("	- scope: "+scope); //$NON-NLS-1$
			}
			IndexManager indexManager = JavaModelManager.getIndexManager();

			// Create pattern
			final char typeSuffix;
			switch(searchFor){
				case IJavaSearchConstants.CLASS :
					typeSuffix = IIndexConstants.CLASS_SUFFIX;
					break;
				case IJavaSearchConstants.CLASS_AND_INTERFACE :
					typeSuffix = IIndexConstants.CLASS_AND_INTERFACE_SUFFIX;
					break;
				case IJavaSearchConstants.CLASS_AND_ENUM :
					typeSuffix = IIndexConstants.CLASS_AND_ENUM_SUFFIX;
					break;
				case IJavaSearchConstants.INTERFACE :
					typeSuffix = IIndexConstants.INTERFACE_SUFFIX;
					break;
				case IJavaSearchConstants.INTERFACE_AND_ANNOTATION :
					typeSuffix = IIndexConstants.INTERFACE_AND_ANNOTATION_SUFFIX;
					break;
				case IJavaSearchConstants.ENUM :
					typeSuffix = IIndexConstants.ENUM_SUFFIX;
					break;
				case IJavaSearchConstants.ANNOTATION_TYPE :
					typeSuffix = IIndexConstants.ANNOTATION_TYPE_SUFFIX;
					break;
				default :
					typeSuffix = IIndexConstants.TYPE_SUFFIX;
					break;
			}
			final MultiTypeDeclarationPattern pattern = new MultiTypeDeclarationPattern(qualifications, typeNames, typeSuffix, matchRule);

			// Get working copy path(s). Store in a single string in case of only one to optimize comparison in requestor
			final HashSet workingCopyPaths = new HashSet();
			String workingCopyPath = null;
			ICompilationUnit[] copies = getWorkingCopies();
			final int copiesLength = copies == null ? 0 : copies.length;
			if (copies != null) {
				if (copiesLength == 1) {
					workingCopyPath = copies[0].getPath().toString();
				} else {
					for (int i = 0; i < copiesLength; i++) {
						ICompilationUnit workingCopy = copies[i];
						workingCopyPaths.add(workingCopy.getPath().toString());
					}
				}
			}
			final String singleWkcpPath = workingCopyPath;

			// Index requestor
			IndexQueryRequestor searchRequestor = new IndexQueryRequestor(){
				@Override
				public boolean acceptIndexMatch(String documentPath, SearchPattern indexRecord, SearchParticipant participant, AccessRuleSet access) {
					// Filter unexpected types
					QualifiedTypeDeclarationPattern record = (QualifiedTypeDeclarationPattern) indexRecord;
					if (record.enclosingTypeNames == IIndexConstants.ONE_ZERO_CHAR) {
						return true; // filter out local and anonymous classes
					}
					switch (copiesLength) {
						case 0:
							break;
						case 1:
							if (singleWkcpPath.equals(documentPath)) {
								return true; // filter out *the* working copy
							}
							break;
						default:
							if (workingCopyPaths.contains(documentPath)) {
								return true; // filter out working copies
							}
							break;
					}

					// Accept document path
					AccessRestriction accessRestriction = null;
					if (access != null) {
						// Compute document relative path
						int qualificationLength = (record.qualification == null || record.qualification.length == 0) ? 0 : record.qualification.length + 1;
						int nameLength = record.simpleName == null ? 0 : record.simpleName.length;
						char[] path = new char[qualificationLength + nameLength];
						int pos = 0;
						if (qualificationLength > 0) {
							System.arraycopy(record.qualification, 0, path, pos, qualificationLength - 1);
							CharOperation.replace(path, '.', '/');

							// Access rules work on package level and should not discriminate on enclosing types.
							boolean isNestedType = record.enclosingTypeNames != null && record.enclosingTypeNames.length > 0;
							path[qualificationLength-1] = isNestedType ? '$' : '/';
							pos += qualificationLength;
						}
						if (nameLength > 0) {
							System.arraycopy(record.simpleName, 0, path, pos, nameLength);
							pos += nameLength;
						}
						// Update access restriction if path is not empty
						if (pos > 0) {
							accessRestriction = access.getViolatedRestriction(path);
						}
					}
					nameRequestor.acceptType(record.modifiers, record.pkg, record.simpleName, record.enclosingTypeNames, documentPath, accessRestriction);
					return true;
				}
			};

			SubMonitor subMonitor = SubMonitor.convert(progressMonitor, Messages.engine_searching, 100);
			// add type names from indexes
			indexManager.performConcurrentJob(
				new PatternSearchJob(
					pattern,
					getDefaultSearchParticipant(), // Java search only
					scope,
					searchRequestor),
				waitingPolicy,
				subMonitor.split(100));

			// add type names from working copies
			if (copies != null) {
				for (int i = 0, length = copies.length; i < length; i++) {
					ICompilationUnit workingCopy = copies[i];
					final String path = workingCopy.getPath().toString();
					if (workingCopy.isConsistent()) {
						IPackageDeclaration[] packageDeclarations = workingCopy.getPackageDeclarations();
						char[] packageDeclaration = packageDeclarations.length == 0 ? CharOperation.NO_CHAR : packageDeclarations[0].getElementName().toCharArray();
						IType[] allTypes = workingCopy.getAllTypes();
						for (int j = 0, allTypesLength = allTypes.length; j < allTypesLength; j++) {
							IType type = allTypes[j];
							IJavaElement parent = type.getParent();
							char[][] enclosingTypeNames;
							char[] qualification = packageDeclaration;
							if (parent instanceof IType) {
								char[] parentQualifiedName = ((IType)parent).getTypeQualifiedName('.').toCharArray();
								enclosingTypeNames = CharOperation.splitOn('.', parentQualifiedName);
								qualification = CharOperation.concat(qualification, parentQualifiedName);
							} else {
								enclosingTypeNames = CharOperation.NO_CHAR_CHAR;
							}
							char[] simpleName = type.getElementName().toCharArray();
							char suffix = IIndexConstants.TYPE_SUFFIX;
							if (type.isClass()) {
								suffix = IIndexConstants.CLASS_SUFFIX;
							} else if (type.isInterface()) {
								suffix = IIndexConstants.INTERFACE_SUFFIX;
							} else if (type.isEnum()) {
								suffix = IIndexConstants.ENUM_SUFFIX;
							} else if (type.isAnnotation()) {
								suffix = IIndexConstants.ANNOTATION_TYPE_SUFFIX;
							}
							if (pattern.matchesDecodedKey(new QualifiedTypeDeclarationPattern(qualification, simpleName, suffix, matchRule))) {
								nameRequestor.acceptType(type.getFlags(), packageDeclaration, simpleName, enclosingTypeNames, path, null);
							}
						}
					} else {
						Parser basicParser = getParser();
						org.eclipse.jdt.internal.compiler.env.ICompilationUnit unit = (org.eclipse.jdt.internal.compiler.env.ICompilationUnit) workingCopy;
						CompilationResult compilationUnitResult = new CompilationResult(unit, 0, 0, this.compilerOptions.maxProblemsPerUnit);
						CompilationUnitDeclaration parsedUnit = basicParser.dietParse(unit, compilationUnitResult);
						if (parsedUnit != null) {
							final char[] packageDeclaration = parsedUnit.currentPackage == null
								? CharOperation.NO_CHAR
								: CharOperation.concatWith(parsedUnit.currentPackage.getImportName(), '.');
							class AllTypeDeclarationsVisitor extends ASTVisitor {
								@Override
								public boolean visit(TypeDeclaration typeDeclaration, BlockScope blockScope) {
									return false; // no local/anonymous type
								}
								@Override
								public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope compilationUnitScope) {
									SearchPattern decodedPattern =
										new QualifiedTypeDeclarationPattern(packageDeclaration, typeDeclaration.name, convertTypeKind(TypeDeclaration.kind(typeDeclaration.modifiers)), matchRule);
									if (pattern.matchesDecodedKey(decodedPattern)) {
										nameRequestor.acceptType(typeDeclaration.modifiers, packageDeclaration, typeDeclaration.name, CharOperation.NO_CHAR_CHAR, path, null);
									}
									return true;
								}
								@Override
								public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope classScope) {
									// compute enclosing type names
									char[] qualification = packageDeclaration;
									TypeDeclaration enclosing = memberTypeDeclaration.enclosingType;
									char[][] enclosingTypeNames = CharOperation.NO_CHAR_CHAR;
									while (enclosing != null) {
										qualification = CharOperation.concat(qualification, enclosing.name, '.');
										enclosingTypeNames = CharOperation.arrayConcat(new char[][] {enclosing.name}, enclosingTypeNames);
										if ((enclosing.bits & ASTNode.IsMemberType) != 0) {
											enclosing = enclosing.enclosingType;
										} else {
											enclosing = null;
										}
									}
									SearchPattern decodedPattern =
										new QualifiedTypeDeclarationPattern(qualification, memberTypeDeclaration.name, convertTypeKind(TypeDeclaration.kind(memberTypeDeclaration.modifiers)), matchRule);
									if (pattern.matchesDecodedKey(decodedPattern)) {
										nameRequestor.acceptType(memberTypeDeclaration.modifiers, packageDeclaration, memberTypeDeclaration.name, enclosingTypeNames, path, null);
									}
									return true;
								}
							}
							parsedUnit.traverse(new AllTypeDeclarationsVisitor(), parsedUnit.scope);
						}
					}
				}
			}
		} finally {
			if (progressMonitor != null) {
				progressMonitor.done();
			}
		}
	}

	public void searchDeclarations(IJavaElement enclosingElement, SearchRequestor requestor, SearchPattern pattern, IProgressMonitor monitor) throws JavaModelException {
		try {
			if (VERBOSE) {
				Util.verbose("	- java element: "+enclosingElement); //$NON-NLS-1$
			}
			IJavaSearchScope scope = createJavaSearchScope(new IJavaElement[] {enclosingElement});
			IResource resource = ((JavaElement) enclosingElement).resource();
			if (enclosingElement instanceof IMember) {
				IMember member = (IMember) enclosingElement;
				ICompilationUnit cu = member.getCompilationUnit();
				if (cu != null) {
					resource = cu.getResource();
				} else if (member.isBinary()) {
					// binary member resource cannot be used as this
					// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=148215
					resource = null;
				}
			}
			try {
				if (resource instanceof IFile) {
					try {
						requestor.beginReporting();
						if (VERBOSE) {
							Util.verbose("Searching for " + pattern + " in " + resource.getFullPath()); //$NON-NLS-1$//$NON-NLS-2$
						}
						SearchParticipant participant = getDefaultSearchParticipant();
						SearchDocument[] documents = MatchLocator.addWorkingCopies(
							pattern,
							new SearchDocument[] {new JavaSearchDocument(enclosingElement.getPath().toString(), participant)},
							getWorkingCopies(enclosingElement),
							participant);
						participant.locateMatches(
							documents,
							pattern,
							scope,
							requestor,
							monitor);
					} finally {
						requestor.endReporting();
					}
				} else {
					search(
						pattern,
						new SearchParticipant[] {getDefaultSearchParticipant()},
						scope,
						requestor,
						monitor);
				}
			} catch (CoreException e) {
				if (e instanceof JavaModelException)
					throw (JavaModelException) e;
				throw new JavaModelException(e);
			}
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}

	/**
	 * Searches for all declarations of the fields accessed in the given element.
	 * The element can be a compilation unit or a source type/method/field.
	 * Reports the field declarations using the given requestor.
	 *
	 * @see SearchEngine#searchDeclarationsOfAccessedFields(IJavaElement, SearchRequestor, IProgressMonitor)
	 * 	for detailed comment
	 */
	public void searchDeclarationsOfAccessedFields(IJavaElement enclosingElement, SearchRequestor requestor, IProgressMonitor monitor) throws JavaModelException {
		if (VERBOSE) {
			Util.verbose("BasicSearchEngine.searchDeclarationsOfAccessedFields(IJavaElement, SearchRequestor, SearchPattern, IProgressMonitor)"); //$NON-NLS-1$
		}
		// Do not accept other kind of element type than those specified in the spec
		switch (enclosingElement.getElementType()) {
			case IJavaElement.FIELD:
			case IJavaElement.METHOD:
			case IJavaElement.TYPE:
			case IJavaElement.COMPILATION_UNIT:
				// valid element type
				break;
			default:
				throw new IllegalArgumentException();
		}
		SearchPattern pattern = new DeclarationOfAccessedFieldsPattern(enclosingElement);
		searchDeclarations(enclosingElement, requestor, pattern, monitor);
	}

	/**
	 * Searches for all declarations of the types referenced in the given element.
	 * The element can be a compilation unit or a source type/method/field.
	 * Reports the type declarations using the given requestor.
	 *
	 * @see SearchEngine#searchDeclarationsOfReferencedTypes(IJavaElement, SearchRequestor, IProgressMonitor)
	 * 	for detailed comment
	 */
	public void searchDeclarationsOfReferencedTypes(IJavaElement enclosingElement, SearchRequestor requestor, IProgressMonitor monitor) throws JavaModelException {
		if (VERBOSE) {
			Util.verbose("BasicSearchEngine.searchDeclarationsOfReferencedTypes(IJavaElement, SearchRequestor, SearchPattern, IProgressMonitor)"); //$NON-NLS-1$
		}
		// Do not accept other kind of element type than those specified in the spec
		switch (enclosingElement.getElementType()) {
			case IJavaElement.FIELD:
			case IJavaElement.METHOD:
			case IJavaElement.TYPE:
			case IJavaElement.COMPILATION_UNIT:
				// valid element type
				break;
			default:
				throw new IllegalArgumentException();
		}
		SearchPattern pattern = new DeclarationOfReferencedTypesPattern(enclosingElement);
		searchDeclarations(enclosingElement, requestor, pattern, monitor);
	}

	/**
	 * Searches for all declarations of the methods invoked in the given element.
	 * The element can be a compilation unit or a source type/method/field.
	 * Reports the method declarations using the given requestor.
	 *
	 * @see SearchEngine#searchDeclarationsOfSentMessages(IJavaElement, SearchRequestor, IProgressMonitor)
	 * 	for detailed comment
	 */
	public void searchDeclarationsOfSentMessages(IJavaElement enclosingElement, SearchRequestor requestor, IProgressMonitor monitor) throws JavaModelException {
		if (VERBOSE) {
			Util.verbose("BasicSearchEngine.searchDeclarationsOfSentMessages(IJavaElement, SearchRequestor, SearchPattern, IProgressMonitor)"); //$NON-NLS-1$
		}
		// Do not accept other kind of element type than those specified in the spec
		switch (enclosingElement.getElementType()) {
			case IJavaElement.FIELD:
			case IJavaElement.METHOD:
			case IJavaElement.TYPE:
			case IJavaElement.COMPILATION_UNIT:
				// valid element type
				break;
			default:
				throw new IllegalArgumentException();
		}
		SearchPattern pattern = new DeclarationOfReferencedMethodsPattern(enclosingElement);
		searchDeclarations(enclosingElement, requestor, pattern, monitor);
	}
}
