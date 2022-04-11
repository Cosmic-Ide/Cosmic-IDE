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
 *     Microsoft Corporation - contribution for bug 575562 - improve completion search performance
 *******************************************************************************/
package org.eclipse.jdt.core.search;

import java.io.IOException;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IModularClassFile;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.jdt.internal.core.LocalVariable;
import org.eclipse.jdt.internal.core.index.EntryResult;
import org.eclipse.jdt.internal.core.index.Index;
import org.eclipse.jdt.internal.core.search.HierarchyScope;
import org.eclipse.jdt.internal.core.search.IndexQueryRequestor;
import org.eclipse.jdt.internal.core.search.JavaSearchScope;
import org.eclipse.jdt.internal.core.search.StringOperation;
import org.eclipse.jdt.internal.core.search.indexing.IIndexConstants;
import org.eclipse.jdt.internal.core.search.indexing.QualifierQuery;
import org.eclipse.jdt.internal.core.search.matching.AndPattern;
import org.eclipse.jdt.internal.core.search.matching.ConstructorPattern;
import org.eclipse.jdt.internal.core.search.matching.FieldPattern;
import org.eclipse.jdt.internal.core.search.matching.LocalVariablePattern;
import org.eclipse.jdt.internal.core.search.matching.MatchLocator;
import org.eclipse.jdt.internal.core.search.matching.MethodPattern;
import org.eclipse.jdt.internal.core.search.matching.ModulePattern;
import org.eclipse.jdt.internal.core.search.matching.OrPattern;
import org.eclipse.jdt.internal.core.search.matching.PackageDeclarationPattern;
import org.eclipse.jdt.internal.core.search.matching.PackageReferencePattern;
import org.eclipse.jdt.internal.core.search.matching.QualifiedTypeDeclarationPattern;
import org.eclipse.jdt.internal.core.search.matching.SuperTypeReferencePattern;
import org.eclipse.jdt.internal.core.search.matching.TypeDeclarationPattern;
import org.eclipse.jdt.internal.core.search.matching.TypeParameterPattern;
import org.eclipse.jdt.internal.core.search.matching.TypeReferencePattern;


/**
 * A search pattern defines how search results are found. Use <code>SearchPattern.createPattern</code>
 * to create a search pattern.
 * <p>
 * Search patterns are used during the search phase to decode index entries that were added during the indexing phase
 * (see {@link SearchDocument#addIndexEntry(char[], char[])}). When an index is queried, the
 * index categories and keys to consider are retrieved from the search pattern using {@link #getIndexCategories()} and
 * {@link #getIndexKey()}, as well as the match rule (see {@link #getMatchRule()}). A blank pattern is
 * then created (see {@link #getBlankPattern()}). This blank pattern is used as a record as follows.
 * For each index entry in the given index categories and that starts with the given key, the blank pattern is fed using
 * {@link #decodeIndexKey(char[])}. The original pattern is then asked if it matches the decoded key using
 * {@link #matchesDecodedKey(SearchPattern)}. If it matches, a search document is created for this index entry
 * using {@link SearchParticipant#getDocument(String)}.
 *
 * </p><p>
 * This class is intended to be sub-classed by clients. A default behavior is provided for each of the methods above, that
 * clients can override if they wish.
 * </p>
 * @see #createPattern(org.eclipse.jdt.core.IJavaElement, int)
 * @see #createPattern(String, int, int, int)
 * @since 3.0
 */
public abstract class SearchPattern {

	// Rules for pattern matching: (exact, prefix, pattern) [ | case sensitive]
	/**
	 * Match rule: The search pattern matches exactly the search result,
	 * that is, the source of the search result equals the search pattern.
	 */
	public static final int R_EXACT_MATCH = 0;

	/**
	 * Match rule: The search pattern is a prefix of the search result.
	 */
	public static final int R_PREFIX_MATCH = 0x0001;

	/**
	 * Match rule: The search pattern contains one or more wild cards ('*' or '?').
	 * A '*' wild-card can replace 0 or more characters in the search result.
	 * A '?' wild-card replaces exactly 1 character in the search result.
	 */
	public static final int R_PATTERN_MATCH = 0x0002;

	/**
	 * Match rule: The search pattern contains a regular expression.
	 * <p><b>Warning:</b> Implemented only for module declaration search.
	 * The support for this rule is <b>not yet implemented for others</b></p>
	 */
	public static final int R_REGEXP_MATCH = 0x0004;

	/**
	 * Match rule: The search pattern matches the search result only if cases are the same.
	 * Can be combined to previous rules, e.g. {@link #R_EXACT_MATCH} | {@link #R_CASE_SENSITIVE}
	 */
	public static final int R_CASE_SENSITIVE = 0x0008;

	/**
	 * Match rule: The search pattern matches search results as raw/parameterized types/methods with same erasure.
	 * This mode has no effect on other java elements search.<br>
	 * Type search example:
	 * 	<ul>
	 * 	<li>pattern: <code>List&lt;Exception&gt;</code></li>
	 * 	<li>match: <code>List&lt;Object&gt;</code></li>
	 * 	</ul>
	 * Method search example:
	 * 	<ul>
	 * 	<li>declaration: <code>&lt;T&gt;foo(T t)</code></li>
	 * 	<li>pattern: <code>&lt;Exception&gt;foo(new Exception())</code></li>
	 * 	<li>match: <code>&lt;Object&gt;foo(new Object())</code></li>
	 * 	</ul>
	 * Can be combined to all other match rules, e.g. {@link #R_CASE_SENSITIVE} | {@link #R_ERASURE_MATCH}
	 * This rule is not activated by default, so raw types or parameterized types with same erasure will not be found
	 * for pattern List&lt;String&gt;,
	 * Note that with this pattern, the match selection will be only on the erasure even for parameterized types.
	 * @since 3.1
	 */
	public static final int R_ERASURE_MATCH = 0x0010;

	/**
	 * Match rule: The search pattern matches search results as raw/parameterized types/methods with equivalent type parameters.
	 * This mode has no effect on other java elements search.<br>
	 * Type search example:
	 * <ul>
	 * 	<li>pattern: <code>List&lt;Exception&gt;</code></li>
	 * 	<li>match:
	 * 		<ul>
	 * 		<li><code>List&lt;? extends Throwable&gt;</code></li>
	 * 		<li><code>List&lt;? super RuntimeException&gt;</code></li>
	 * 		<li><code>List&lt;?&gt;</code></li>
	 *			</ul>
	 * 	</li>
	 * 	</ul>
	 * Method search example:
	 * 	<ul>
	 * 	<li>declaration: <code>&lt;T&gt;foo(T t)</code></li>
	 * 	<li>pattern: <code>&lt;Exception&gt;foo(new Exception())</code></li>
	 * 	<li>match:
	 * 		<ul>
	 * 		<li><code>&lt;? extends Throwable&gt;foo(new Exception())</code></li>
	 * 		<li><code>&lt;? super RuntimeException&gt;foo(new Exception())</code></li>
	 * 		<li><code>foo(new Exception())</code></li>
	 *			</ul>
	 * 	</ul>
	 * Can be combined to all other match rules, e.g. {@link #R_CASE_SENSITIVE} | {@link #R_EQUIVALENT_MATCH}
	 * This rule is not activated by default, so raw types or equivalent parameterized types will not be found
	 * for pattern List&lt;String&gt;,
	 * This mode is overridden by {@link  #R_ERASURE_MATCH} as erasure matches obviously include equivalent ones.
	 * That means that pattern with rule set to {@link #R_EQUIVALENT_MATCH} | {@link  #R_ERASURE_MATCH}
	 * will return same results than rule only set with {@link  #R_ERASURE_MATCH}.
	 * @since 3.1
	 */
	public static final int R_EQUIVALENT_MATCH = 0x0020;

	/**
	 * Match rule: The search pattern matches exactly the search result,
	 * that is, the source of the search result equals the search pattern.
	 * @since 3.1
	 */
	public static final int R_FULL_MATCH = 0x0040;

	/**
	 * Match rule: The search pattern contains a Camel Case expression.
	 * <p>
	 * Examples:
	 * <ul>
	 * 	<li>'NPE' type string pattern will match
	 * 		'NullPointerException' and 'NoPermissionException' types,</li>
	 * 	<li>'NuPoEx' type string pattern will only match
	 * 		'NullPointerException' type.</li>
	 * </ul>
	 *
	 * This rule is not intended to be combined with any other match rule. In case
	 * of other match rule flags are combined with this one, then match rule validation
	 * will return a modified rule in order to perform a better appropriate search request
	 * (see {@link #validateMatchRule(String, int)} for more details).
	 * <p>
	 * @see #camelCaseMatch(String, String) for a detailed explanation of Camel
	 * 	Case matching.
	 *
	 * @since 3.2
	 */
	public static final int R_CAMELCASE_MATCH = 0x0080;

	/**
	 * Match rule: The search pattern contains a Camel Case expression with
	 * a strict expected number of parts.
	 * <br>
	 * Examples:
	 * <ul>
	 * 	<li>'HM' type string pattern will match 'HashMap' and 'HtmlMapper' types,
	 * 		but not 'HashMapEntry'
	 * 	</li>
	 * 	<li>'HMap' type string pattern will still match previous 'HashMap' and
	 * 		'HtmlMapper' types, but not 'HighMagnitude'
	 * 	</li>
	 * </ul>
	 *
	 * This rule is not intended to be combined with any other match rule. In case
	 * of other match rule flags are combined with this one, then match rule validation
	 * will return a modified rule in order to perform a better appropriate search request
	 * (see {@link #validateMatchRule(String, int)} for more details).
	 * <p>
	 * @see CharOperation#camelCaseMatch(char[], char[], boolean) for a detailed
	 * explanation of Camel Case matching.
	 *<p>
	 * @since 3.4
	 */
	public static final int R_CAMELCASE_SAME_PART_COUNT_MATCH = 0x0100;

	/**
	 * Match rule: The search pattern contains a substring expression in a case-insensitive way.
	 * <p>
	 * Examples:
	 * <ul>
	 * 	<li>'bar' string pattern will match
	 * 		'bar1', 'Bar' and 'removeBar' types,</li>
	 * </ul>
	 *
	 * This rule is not intended to be combined with any other match rule. In case
	 * of other match rule flags are combined with this one, then match rule validation
	 * will return a modified rule in order to perform a better appropriate search request
	 * (see {@link #validateMatchRule(String, int)} for more details).
	 *
	 * <p>
	 * This is implemented only for code assist and not available for normal search.
	 *
	 * @since 3.12
	 */
	public static final int R_SUBSTRING_MATCH = 0x0200;

	/**
	 * Match rule: The search pattern contains a subword expression in a case-insensitive way.
	 * <p>
	 * Examples:
	 * <ul>
	 * 	<li>'addlist' string pattern will match
	 * 		'addListener' and 'addChangeListener'</li>
	 * </ul>
	 *
	 * This rule is not intended to be combined with any other match rule. In case
	 * of other match rule flags are combined with this one, then match rule validation
	 * will return a modified rule in order to perform a better appropriate search request
	 * (see {@link #validateMatchRule(String, int)} for more details).
	 *
	 * <p>
	 * This is implemented only for code assist and not available for normal search.
	 *
	 * @noreference This is not intended to be referenced by clients as it is a part of Java preview feature.
	 * @since 3.21
	 */
	public static final int R_SUBWORD_MATCH = 0x0400;

	private static final int MODE_MASK = R_EXACT_MATCH
		| R_PREFIX_MATCH
		| R_PATTERN_MATCH
		| R_REGEXP_MATCH
		| R_CAMELCASE_MATCH
		| R_CAMELCASE_SAME_PART_COUNT_MATCH
		| R_SUBSTRING_MATCH
		| R_SUBWORD_MATCH;

	private int matchRule;

	/**
	 * The focus element (used for reference patterns)
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public IJavaElement focus;

	/**
	 * The encoded index qualifier query which is used to narrow down number of indexes to search based on the qualifier.
	 * This is optional. In absence all indexes provided by scope will be searched.
	 * <br>
	 * The encoded query format is as following
	 * <pre>
	 * CATEGORY1[,CATEGORY2]:SIMPLE_KEY:QUALIFIED_KEY
	 * </pre>
	 * if the category is not provided, then the index qualifier search will be done for all type of qualifiers.
	 *
	 * @noreference This field is not intended to be referenced by clients.
	 * @see QualifierQuery#encodeQuery(org.eclipse.jdt.internal.core.search.indexing.QualifierQuery.QueryCategory[], char[], char[])
	 */
	public char[] indexQualifierQuery;

	/**
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public int kind;

	/**
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public boolean mustResolve = true;

/**
 * Creates a search pattern with the rule to apply for matching index keys.
 * It can be exact match, prefix match, pattern match or regexp match.
 * Rule can also be combined with a case sensitivity flag.
 *
 * @param matchRule one of following match rule
 * 	<ul>
 * 		<li>{@link #R_EXACT_MATCH}</li>
 * 		<li>{@link #R_PREFIX_MATCH}</li>
 * 		<li>{@link #R_PATTERN_MATCH}</li>
 * 		<li>{@link #R_REGEXP_MATCH}</li>
 * 		<li>{@link #R_CAMELCASE_MATCH}</li>
 * 		<li>{@link #R_CAMELCASE_SAME_PART_COUNT_MATCH}</li>
 * 	</ul>
 * 	which may be also combined with one of following flag:
 * 	<ul>
 * 		<li>{@link #R_CASE_SENSITIVE}</li>
 * 		<li>{@link #R_ERASURE_MATCH}</li>
 * 		<li>{@link #R_EQUIVALENT_MATCH}</li>
 * 	</ul>
 *		For example,
 *		<ul>
 *			<li>{@link #R_EXACT_MATCH} | {@link #R_CASE_SENSITIVE}: if an exact
 *				and case sensitive match is requested,</li>
 *			<li>{@link #R_PREFIX_MATCH} if a case insensitive prefix match is requested</li>
 *			<li>{@link #R_EXACT_MATCH} | {@link #R_ERASURE_MATCH}: if a case
 *				insensitive and erasure match is requested.</li>
 *		</ul>
 * 	Note that {@link #R_ERASURE_MATCH} or {@link #R_EQUIVALENT_MATCH} has no effect
 * 	on non-generic types/methods search.
 * 	<p>
 * 	Note also that default behavior for generic types/methods search is to find exact matches.
 */
public SearchPattern(int matchRule) {
	this.matchRule = matchRule;
	// Set full match implicit mode
	if ((matchRule & (R_EQUIVALENT_MATCH | R_ERASURE_MATCH )) == 0) {
		this.matchRule |= R_FULL_MATCH;
	}
	// reset other incompatible flags
	if ((matchRule & R_CAMELCASE_MATCH) != 0) {
		this.matchRule &= ~R_CAMELCASE_SAME_PART_COUNT_MATCH;
		this.matchRule &= ~R_PREFIX_MATCH;
	} else if ((matchRule & R_CAMELCASE_SAME_PART_COUNT_MATCH) != 0) {
		this.matchRule &= ~R_PREFIX_MATCH;
	}
}

/**
 * @noreference This method is not intended to be referenced by clients.
 * @nooverride This method is not intended to be re-implemented or extended by clients.
 */
public void acceptMatch(String relativePath, String containerPath, char separator, SearchPattern pattern, IndexQueryRequestor requestor, SearchParticipant participant, IJavaSearchScope scope) {
	acceptMatch(relativePath, containerPath, separator, pattern, requestor, participant, scope, null);
}
/**
 * @noreference This method is not intended to be referenced by clients.
 * @nooverride This method is not intended to be re-implemented or extended by clients.
 */
public void acceptMatch(String relativePath, String containerPath, char separator, SearchPattern pattern, IndexQueryRequestor requestor, SearchParticipant participant, IJavaSearchScope scope, IProgressMonitor monitor) {

	if (scope instanceof JavaSearchScope) {
		JavaSearchScope javaSearchScope = (JavaSearchScope) scope;
		// Get document path access restriction from java search scope
		// Note that requestor has to verify if needed whether the document violates the access restriction or not
		AccessRuleSet access = javaSearchScope.getAccessRuleSet(relativePath, containerPath);
		if (access != JavaSearchScope.NOT_ENCLOSED) { // scope encloses the document path
			StringBuilder documentPath = new StringBuilder(containerPath.length() + 1 + relativePath.length());
			documentPath.append(containerPath);
			documentPath.append(separator);
			documentPath.append(relativePath);
			if (!requestor.acceptIndexMatch(documentPath.toString(), pattern, participant, access))
				throw new OperationCanceledException();
		}
	} else {
		StringBuilder buffer = new StringBuilder(containerPath.length() + 1 + relativePath.length());
		buffer.append(containerPath);
		buffer.append(separator);
		buffer.append(relativePath);
		String documentPath = buffer.toString();
		boolean encloses = (scope instanceof HierarchyScope) ? ((HierarchyScope)scope).encloses(documentPath, monitor)
							: scope.encloses(documentPath);
		if (encloses)
			if (!requestor.acceptIndexMatch(documentPath, pattern, participant, null))
				throw new OperationCanceledException();

	}
}
/**
 * @noreference This method is not intended to be referenced by clients.
 * @nooverride This method is not intended to be re-implemented or extended by clients.
 */
public SearchPattern currentPattern() {
	return this;
}
/**
 * Answers true if the pattern matches the given name using CamelCase rules, or
 * false otherwise. char[] CamelCase matching does NOT accept explicit wild-cards
 * '*' and '?' and is inherently case sensitive.
 * <p>
 * CamelCase denotes the convention of writing compound names without spaces,
 * and capitalizing every term. This function recognizes both upper and lower
 * CamelCase, depending whether the leading character is capitalized or not.
 * The leading part of an upper CamelCase pattern is assumed to contain a
 * sequence of capitals which are appearing in the matching name; e.g. 'NPE' will
 * match 'NullPointerException', but not 'NewPerfData'. A lower CamelCase pattern
 * uses a lowercase first character. In Java, type names follow the upper
 * CamelCase convention, whereas method or field names follow the lower
 * CamelCase convention.
 * <p>
 * The pattern may contain lowercase characters, which will be matched in a case
 * sensitive way. These characters must appear in sequence in the name.
 * For instance, 'NPExcep' will match 'NullPointerException', but not
 * 'NullPointerExCEPTION' or 'NuPoEx' will match 'NullPointerException', but not
 * 'NoPointerException'.
 * <p>
 * Digit characters are treated in a special way. They can be used in the pattern
 * but are not always considered as leading character. For instance, both
 * 'UTF16DSS' and 'UTFDSS' patterns will match 'UTF16DocumentScannerSupport'.
 * <p>
 * Using this method allows matching names to have more parts than the specified
 * pattern (see {@link #camelCaseMatch(String, String, boolean)}).<br>
 * For instance, 'HM' , 'HaMa' and  'HMap' patterns will match 'HashMap',
 * 'HatMapper' <b>and also</b> 'HashMapEntry'.
 * <p>
 * Examples:
 * <ol><li>  pattern = "NPE"
 *  name = NullPointerException / NoPermissionException
 *  result => true</li>
 * <li>  pattern = "NuPoEx"
 *  name = NullPointerException
 *  result => true</li>
 * <li>  pattern = "npe"
 *  name = NullPointerException
 *  result => false</li>
 * <li>  pattern = "IPL3"
 *  name = "IPerspectiveListener3"
 *  result => true</li>
 * <li>  pattern = "HM"
 *  name = "HashMapEntry"
 *  result => true</li>
 * <li>  pattern = "HMap"
 *  name = "HatMapper"
 *  result => true</li>
 * </ol>
 *
 * @see #camelCaseMatch(String, int, int, String, int, int, boolean) for algorithm
 * implementation
 *
 * @param pattern the given pattern
 * @param name the given name
 * @return true if the pattern matches the given name, false otherwise
 * @since 3.2
 */
public static final boolean camelCaseMatch(String pattern, String name) {
	if (pattern == null)
		return true; // null pattern is equivalent to '*'
	if (name == null)
		return false; // null name cannot match

	return camelCaseMatch(pattern, 0, pattern.length(), name, 0, name.length(), false/*not the same count of parts*/);
}

/**
 * Answers true if the pattern matches the given name using CamelCase rules, or
 * false otherwise. char[] CamelCase matching does NOT accept explicit wild-cards
 * '*' and '?' and is inherently case sensitive.
 * <p>
 * CamelCase denotes the convention of writing compound names without spaces,
 * and capitalizing every term. This function recognizes both upper and lower
 * CamelCase, depending whether the leading character is capitalized or not.
 * The leading part of an upper CamelCase pattern is assumed to contain a
 * sequence of capitals which are appearing in the matching name; e.g. 'NPE' will
 * match 'NullPointerException', but not 'NewPerfData'. A lower CamelCase pattern
 * uses a lowercase first character. In Java, type names follow the upper
 * CamelCase convention, whereas method or field names follow the lower
 * CamelCase convention.
 * <p>
 * The pattern may contain lowercase characters, which will be matched in a case
 * sensitive way. These characters must appear in sequence in the name.
 * For instance, 'NPExcep' will match 'NullPointerException', but not
 * 'NullPointerExCEPTION' or 'NuPoEx' will match 'NullPointerException', but not
 * 'NoPointerException'.
 * <p>
 * Digit characters are treated in a special way. They can be used in the pattern
 * but are not always considered as leading character. For instance, both
 * 'UTF16DSS' and 'UTFDSS' patterns will match 'UTF16DocumentScannerSupport'.
 * <p>
 * CamelCase can be restricted to match only the same count of parts. When this
 * restriction is specified the given pattern and the given name must have <b>exactly</b>
 * the same number of parts (i.e. the same number of uppercase characters).<br>
 * For instance, 'HM' , 'HaMa' and  'HMap' patterns will match 'HashMap' and
 * 'HatMapper' <b>but not</b> 'HashMapEntry'.
 * <p>
 * Examples:
 * <ol><li>  pattern = "NPE"
 *  name = NullPointerException / NoPermissionException
 *  result => true</li>
 * <li>  pattern = "NuPoEx"
 *  name = NullPointerException
 *  result => true</li>
 * <li>  pattern = "npe"
 *  name = NullPointerException
 *  result => false</li>
 * <li>  pattern = "IPL3"
 *  name = "IPerspectiveListener3"
 *  result => true</li>
 * <li>  pattern = "HM"
 *  name = "HashMapEntry"
 *  result => (samePartCount == false)</li>
 * </ol>
 *
 * @see #camelCaseMatch(String, int, int, String, int, int, boolean) for algorithm
 * 	implementation
 *
 * @param pattern the given pattern
 * @param name the given name
 * @param samePartCount flag telling whether the pattern and the name should
 * 	have the same count of parts or not.<br>
 * 	&nbsp;&nbsp;For example:
 * 	<ul>
 * 		<li>'HM' type string pattern will match 'HashMap' and 'HtmlMapper' types,
 * 				but not 'HashMapEntry'</li>
 * 		<li>'HMap' type string pattern will still match previous 'HashMap' and
 * 				'HtmlMapper' types, but not 'HighMagnitude'</li>
 * 	</ul>
 * @return true if the pattern matches the given name, false otherwise
 * @since 3.4
 */
public static final boolean camelCaseMatch(String pattern, String name, boolean samePartCount) {
	if (pattern == null)
		return true; // null pattern is equivalent to '*'
	if (name == null)
		return false; // null name cannot match

	return camelCaseMatch(pattern, 0, pattern.length(), name, 0, name.length(), samePartCount);
}

/**
 * Answers true if a sub-pattern matches the sub-part of the given name using
 * CamelCase rules, or false otherwise.  char[] CamelCase matching does NOT
 * accept explicit wild-cards '*' and '?' and is inherently case sensitive.
 * Can match only subset of name/pattern, considering end positions as non-inclusive.
 * The sub-pattern is defined by the patternStart and patternEnd positions.
 * <p>
 * CamelCase denotes the convention of writing compound names without spaces,
 * and capitalizing every term. This function recognizes both upper and lower
 * CamelCase, depending whether the leading character is capitalized or not.
 * The leading part of an upper CamelCase pattern is assumed to contain a
 * sequence of capitals which are appearing in the matching name; e.g. 'NPE' will
 * match 'NullPointerException', but not 'NewPerfData'. A lower CamelCase pattern
 * uses a lowercase first character. In Java, type names follow the upper
 * CamelCase convention, whereas method or field names follow the lower
 * CamelCase convention.
 * <p>
 * The pattern may contain lowercase characters, which will be matched in a case
 * sensitive way. These characters must appear in sequence in the name.
 * For instance, 'NPExcep' will match 'NullPointerException', but not
 * 'NullPointerExCEPTION' or 'NuPoEx' will match 'NullPointerException', but not
 * 'NoPointerException'.
 * <p>
 * Digit characters are treated in a special way. They can be used in the pattern
 * but are not always considered as leading character. For instance, both
 * 'UTF16DSS' and 'UTFDSS' patterns will match 'UTF16DocumentScannerSupport'.
 * <p>
 * Digit characters are treated in a special way. They can be used in the pattern
 * but are not always considered as leading character. For instance, both
 * 'UTF16DSS' and 'UTFDSS' patterns will match 'UTF16DocumentScannerSupport'.
 * <p>
 * Using this method allows matching names to have more parts than the specified
 * pattern (see {@link #camelCaseMatch(String, int, int, String, int, int, boolean)}).<br>
 * For instance, 'HM' , 'HaMa' and  'HMap' patterns will match 'HashMap',
 * 'HatMapper' <b>and also</b> 'HashMapEntry'.
 * <ol>
 * <li>  pattern = "NPE"
 *  patternStart = 0
 *  patternEnd = 3
 *  name = NullPointerException
 *  nameStart = 0
 *  nameEnd = 20
 *  result => true</li>
 * <li>  pattern = "NPE"
 *  patternStart = 0
 *  patternEnd = 3
 *  name = NoPermissionException
 *  nameStart = 0
 *  nameEnd = 21
 *  result => true</li>
 * <li>  pattern = "NuPoEx"
 *  patternStart = 0
 *  patternEnd = 6
 *  name = NullPointerException
 *  nameStart = 0
 *  nameEnd = 20
 *  result => true</li>
 * <li>  pattern = "NuPoEx"
 *  patternStart = 0
 *  patternEnd = 6
 *  name = NoPermissionException
 *  nameStart = 0
 *  nameEnd = 21
 *  result => false</li>
 * <li>  pattern = "npe"
 *  patternStart = 0
 *  patternEnd = 3
 *  name = NullPointerException
 *  nameStart = 0
 *  nameEnd = 20
 *  result => false</li>
 * <li>  pattern = "IPL3"
 *  patternStart = 0
 *  patternEnd = 3
 *  name = "IPerspectiveListener3"
 *  nameStart = 0
 *  nameEnd = 21
 *  result => true</li>
 * <li>  pattern = "HM"
 *  patternStart = 0
 *  patternEnd = 2
 *  name = "HashMapEntry"
 *  nameStart = 0
 *  nameEnd = 12
 *  result => true</li>
 * <li>  pattern = "HMap"
 *  patternStart = 0
 *  patternEnd = 4
 *  name = "HatMapper"
 *  nameStart = 0
 *  nameEnd = 9
 *  result => true</li>
 * </ol>
 *
 * @param pattern the given pattern
 * @param patternStart the start index of the pattern, inclusive
 * @param patternEnd the end index of the pattern, exclusive
 * @param name the given name
 * @param nameStart the start index of the name, inclusive
 * @param nameEnd the end index of the name, exclusive
 * @return true if a sub-pattern matches the sub-part of the given name, false otherwise
 * @since 3.2
 */
public static final boolean camelCaseMatch(String pattern, int patternStart, int patternEnd, String name, int nameStart, int nameEnd) {
	return camelCaseMatch(pattern, patternStart, patternEnd, name, nameStart, nameEnd, false/*not the same count of parts*/);
}

/**
 * Answers true if a sub-pattern matches the sub-part of the given name using
 * CamelCase rules, or false otherwise.  char[] CamelCase matching does NOT
 * accept explicit wild-cards '*' and '?' and is inherently case sensitive.
 * Can match only subset of name/pattern, considering end positions as
 * non-inclusive. The sub-pattern is defined by the patternStart and patternEnd
 * positions.
 * <p>
 * CamelCase denotes the convention of writing compound names without spaces,
 * and capitalizing every term. This function recognizes both upper and lower
 * CamelCase, depending whether the leading character is capitalized or not.
 * The leading part of an upper CamelCase pattern is assumed to contain
 * a sequence of capitals which are appearing in the matching name; e.g. 'NPE' will
 * match 'NullPointerException', but not 'NewPerfData'. A lower CamelCase pattern
 * uses a lowercase first character. In Java, type names follow the upper
 * CamelCase convention, whereas method or field names follow the lower
 * CamelCase convention.
 * <p>
 * The pattern may contain lowercase characters, which will be matched in a case
 * sensitive way. These characters must appear in sequence in the name.
 * For instance, 'NPExcep' will match 'NullPointerException', but not
 * 'NullPointerExCEPTION' or 'NuPoEx' will match 'NullPointerException', but not
 * 'NoPointerException'.
 * <p>
 * Digit characters are treated in a special way. They can be used in the pattern
 * but are not always considered as leading character. For instance, both
 * 'UTF16DSS' and 'UTFDSS' patterns will match 'UTF16DocumentScannerSupport'.
 * <p>
 * CamelCase can be restricted to match only the same count of parts. When this
 * restriction is specified the given pattern and the given name must have <b>exactly</b>
 * the same number of parts (i.e. the same number of uppercase characters).<br>
 * For instance, 'HM' , 'HaMa' and  'HMap' patterns will match 'HashMap' and
 * 'HatMapper' <b>but not</b> 'HashMapEntry'.
 * <p>
 * Examples:
 * <ol>
 * <li>  pattern = "NPE"
 *  patternStart = 0
 *  patternEnd = 3
 *  name = NullPointerException
 *  nameStart = 0
 *  nameEnd = 20
 *  result => true</li>
 * <li>  pattern = "NPE"
 *  patternStart = 0
 *  patternEnd = 3
 *  name = NoPermissionException
 *  nameStart = 0
 *  nameEnd = 21
 *  result => true</li>
 * <li>  pattern = "NuPoEx"
 *  patternStart = 0
 *  patternEnd = 6
 *  name = NullPointerException
 *  nameStart = 0
 *  nameEnd = 20
 *  result => true</li>
 * <li>  pattern = "NuPoEx"
 *  patternStart = 0
 *  patternEnd = 6
 *  name = NoPermissionException
 *  nameStart = 0
 *  nameEnd = 21
 *  result => false</li>
 * <li>  pattern = "npe"
 *  patternStart = 0
 *  patternEnd = 3
 *  name = NullPointerException
 *  nameStart = 0
 *  nameEnd = 20
 *  result => false</li>
 * <li>  pattern = "IPL3"
 *  patternStart = 0
 *  patternEnd = 3
 *  name = "IPerspectiveListener3"
 *  nameStart = 0
 *  nameEnd = 21
 *  result => true</li>
 * <li>  pattern = "HM"
 *  patternStart = 0
 *  patternEnd = 2
 *  name = "HashMapEntry"
 *  nameStart = 0
 *  nameEnd = 12
 *  result => (samePartCount == false)</li>
 * </ol>
 *
 * @see CharOperation#camelCaseMatch(char[], int, int, char[], int, int, boolean)
 * 	from which algorithm implementation has been entirely copied.
 *
 * @param pattern the given pattern
 * @param patternStart the start index of the pattern, inclusive
 * @param patternEnd the end index of the pattern, exclusive
 * @param name the given name
 * @param nameStart the start index of the name, inclusive
 * @param nameEnd the end index of the name, exclusive
 * @param samePartCount flag telling whether the pattern and the name should
 * 	have the same count of parts or not.<br>
 * 	&nbsp;&nbsp;For example:
 * 	<ul>
 * 		<li>'HM' type string pattern will match 'HashMap' and 'HtmlMapper' types,
 * 				but not 'HashMapEntry'</li>
 * 		<li>'HMap' type string pattern will still match previous 'HashMap' and
 * 				'HtmlMapper' types, but not 'HighMagnitude'</li>
 * 	</ul>
 * @return true if a sub-pattern matches the sub-part of the given name, false otherwise
 * @since 3.4
 */
public static final boolean camelCaseMatch(String pattern, int patternStart, int patternEnd, String name, int nameStart, int nameEnd, boolean samePartCount) {
	return StringOperation.getCamelCaseMatchingRegions(pattern, patternStart, patternEnd, name, nameStart, nameEnd, samePartCount) != null;
}

/**
 * Answers all the regions in a given name matching a given pattern using
 * a specified match rule.
 * <p>
 * Each of these regions is made of its starting index and its length in the given
 * name. They are all concatenated in a single array of <code>int</code>
 * which therefore always has an even length.
 * </p><p>
 * All returned regions are disjointed from each other. That means that the end
 * of a region is always different than the start of the following one.<br>
 * For example, if two regions are returned:<br>
 * <code>{ start1, length1, start2, length2 }</code><br>
 * then <code>start1+length1</code> will always be smaller than
 * <code>start2</code>.
 * </p><p>
 * The possible comparison rules between the name and the pattern are:
 * <ul>
 * <li>{@link #R_EXACT_MATCH exact matching}</li>
 * <li>{@link #R_PREFIX_MATCH prefix matching}</li>
 * <li>{@link #R_PATTERN_MATCH pattern matching}</li>
 * <li>{@link #R_CAMELCASE_MATCH camel case matching}</li>
 * <li>{@link #R_CAMELCASE_SAME_PART_COUNT_MATCH camel case matching with same parts count}</li>
 * </ul>
 * Each of these rules may be combined with the
 * {@link #R_CASE_SENSITIVE case sensitive flag} if the match comparison
 * should respect the case.
 * <p>
 * Examples:
 * <ol><li>  pattern = "NPE"
 *  name = NullPointerException / NoPermissionException
 *  matchRule = {@link #R_CAMELCASE_MATCH}
 *  result:  { 0, 1, 4, 1, 11, 1 } / { 0, 1, 2, 1, 12, 1 } </li>
 * <li>  pattern = "NuPoEx"
 *  name = NullPointerException
 *  matchRule = {@link #R_CAMELCASE_MATCH}
 *  result:  { 0, 2, 4, 2, 11, 2 }</li>
 * <li>  pattern = "IPL3"
 *  name = "IPerspectiveListener3"
 *  matchRule = {@link #R_CAMELCASE_MATCH}
 *  result:  { 0, 2, 12, 1, 20, 1 }</li>
 * <li>  pattern = "HashME"
 *  name = "HashMapEntry"
 *  matchRule = {@link #R_CAMELCASE_MATCH}
 *  result:  { 0, 5, 7, 1 }</li>
 * <li>  pattern = "N???Po*Ex?eption"
 *  name = NullPointerException
 *  matchRule = {@link #R_PATTERN_MATCH} | {@link #R_CASE_SENSITIVE}
 *  result:  { 0, 1, 4, 2, 11, 2, 14, 6 }</li>
 * <li>  pattern = "Ha*M*ent*"
 *  name = "HashMapEntry"
 *  matchRule = {@link #R_PATTERN_MATCH}
 *  result:  { 0, 2, 4, 1, 7, 3 }</li>
 * </ol>
 *
 * @see #camelCaseMatch(String, String, boolean) for more details on the
 * 	camel case behavior
 * @see CharOperation#match(char[], char[], boolean) for more details on the
 * 	pattern match behavior
 *
 * @param pattern the given pattern. If <code>null</code>,
 *     then an empty region (<code>new int[0]</code>) will be returned
 *     showing that the name matches the pattern but no common
 *     character has been found.
 * @param name the given name
 * @param matchRule the rule to apply for the comparison.<br>
 *     The following values are accepted:
 *     <ul>
 *         <li>{@link #R_EXACT_MATCH}</li>
 *         <li>{@link #R_PREFIX_MATCH}</li>
 *         <li>{@link #R_PATTERN_MATCH}</li>
 *         <li>{@link #R_CAMELCASE_MATCH}</li>
 *         <li>{@link #R_CAMELCASE_SAME_PART_COUNT_MATCH}</li>
 *     </ul>
 *     <p>
 *     Each of these valid values may be also combined with
 *     the {@link #R_CASE_SENSITIVE} flag.
 *     </p>
 *     Some examples:
 *     <ul>
 *         <li>{@link #R_EXACT_MATCH} | {@link #R_CASE_SENSITIVE}:
 *                 if an exact case sensitive match is expected,</li>
 *         <li>{@link #R_PREFIX_MATCH}:
 *                 if a case insensitive prefix match is expected,</li>
 *         <li>{@link #R_CAMELCASE_MATCH}:
 *                 if a case insensitive camel case match is expected,</li>
 *         <li>{@link #R_CAMELCASE_SAME_PART_COUNT_MATCH}
 *                 | {@link #R_CASE_SENSITIVE}:
 *                 if a case sensitive camel case with same parts count match
 *                 is expected,</li>
 *         <li>etc.</li>
 *     </ul>
 * @return an array of <code>int</code> having two slots per returned
 *     regions (the first one is the region starting index and the second one
 *     is the region length or <code>null</code> if the given name does not
 *     match the given pattern).
 *     <p>
 *     The returned regions may be empty (<code>new int[0]</code>) if the
 *     pattern is <code>null</code> (whatever the match rule is). The returned
 *     regions will also be empty if the pattern is only made of <code>'?'</code>
 *     and/or <code>'*'</code> character(s) (e.g. <code>'*'</code>,
 *     <code>'?*'</code>, <code>'???'</code>, etc.) when using a pattern
 *     match rule.
 *     </p>
 *
 * @since 3.5
 */
public static final int[] getMatchingRegions(String pattern, String name, int matchRule) {
	if (name == null) return null;
	final int nameLength = name.length();
	if (pattern == null) {
		return new int[] { 0, nameLength };
	}
	final int patternLength = pattern.length();
	boolean countMatch = false;
	switch (matchRule) {
		case SearchPattern.R_EXACT_MATCH:
			if (patternLength == nameLength && pattern.equalsIgnoreCase(name)) {
				return new int[] { 0, patternLength };
			}
			break;
		case SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE:
			if (patternLength == nameLength && pattern.equals(name)) {
				return new int[] { 0, patternLength };
			}
			break;
		case SearchPattern.R_PREFIX_MATCH:
			if (patternLength <= nameLength && name.substring(0, patternLength).equalsIgnoreCase(pattern)) {
				return new int[] { 0, patternLength };
			}
			break;
		case SearchPattern.R_PREFIX_MATCH | SearchPattern.R_CASE_SENSITIVE:
			if (name.startsWith(pattern)) {
				return new int[] { 0, patternLength };
			}
			break;
		case SearchPattern.R_CAMELCASE_SAME_PART_COUNT_MATCH:
			countMatch = true;
			//$FALL-THROUGH$
		case SearchPattern.R_CAMELCASE_MATCH:
			if (patternLength <= nameLength) {
				int[] regions = StringOperation.getCamelCaseMatchingRegions(pattern, 0, patternLength, name, 0, nameLength, countMatch);
				if (regions != null) return regions;
				if (name.substring(0, patternLength).equalsIgnoreCase(pattern)) {
					return new int[] { 0, patternLength };
				}
			}
			break;
		case SearchPattern.R_CAMELCASE_SAME_PART_COUNT_MATCH | SearchPattern.R_CASE_SENSITIVE:
			countMatch = true;
			//$FALL-THROUGH$
		case SearchPattern.R_CAMELCASE_MATCH | SearchPattern.R_CASE_SENSITIVE:
			if (patternLength <= nameLength) {
				return StringOperation.getCamelCaseMatchingRegions(pattern, 0, patternLength, name, 0, nameLength, countMatch);
			}
			break;
		case SearchPattern.R_PATTERN_MATCH:
			return StringOperation.getPatternMatchingRegions(pattern, 0, patternLength, name, 0, nameLength, false);
		case SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE:
			return StringOperation.getPatternMatchingRegions(pattern, 0, patternLength, name, 0, nameLength, true);
		case SearchPattern.R_SUBSTRING_MATCH:
			if (patternLength <= nameLength) {
				int next = CharOperation.indexOf(pattern.toCharArray(), name.toCharArray(), false);
				return next >= 0 ? new int[] {next, patternLength} : null;
			}
			break;
		case SearchPattern.R_SUBWORD_MATCH:
			return CharOperation.getSubWordMatchingRegions(pattern, name);
	}
	return null;
}

/**
 * Returns a search pattern that combines the given two patterns into an
 * "and" pattern. The search result will match both the left pattern and
 * the right pattern.
 *
 * @param leftPattern the left pattern
 * @param rightPattern the right pattern
 * @return an "and" pattern
 * @deprecated Unfortunately, this functionality is not fully supported yet
 * 	(see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=142044" for more details).
 * 	This might be done in a further version...
 */
public static SearchPattern createAndPattern(SearchPattern leftPattern, SearchPattern rightPattern) {
	return new AndPattern(leftPattern, rightPattern);
}

private static SearchPattern createFieldPattern(String patternString, int limitTo, int matchRule) {
	// use 1.7 as the source level as there are more valid tokens in 1.7 mode
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=376673
	Scanner scanner = new Scanner(false /*comment*/, true /*whitespace*/, false /*nls*/, ClassFileConstants.JDK1_7/*sourceLevel*/, null /*taskTags*/, null/*taskPriorities*/, true/*taskCaseSensitive*/);
	scanner.setSource(patternString.toCharArray());
	final int InsideDeclaringPart = 1;
	final int InsideType = 2;
	int lastToken = -1;

	String declaringType = null, fieldName = null;
	String type = null;
	int mode = InsideDeclaringPart;
	int token;
	try {
		token = scanner.getNextToken();
	} catch (InvalidInputException e) {
		return null;
	}
	while (token != TerminalTokens.TokenNameEOF) {
		switch(mode) {
			// read declaring type and fieldName
			case InsideDeclaringPart :
				switch (token) {
					case TerminalTokens.TokenNameDOT:
						if (declaringType == null) {
							if (fieldName == null) return null;
							declaringType = fieldName;
						} else {
							String tokenSource = scanner.getCurrentTokenString();
							declaringType += tokenSource + fieldName;
						}
						fieldName = null;
						break;
					case TerminalTokens.TokenNameWHITESPACE:
						if (!(TerminalTokens.TokenNameWHITESPACE == lastToken || TerminalTokens.TokenNameDOT == lastToken))
							mode = InsideType;
						break;
					default: // all other tokens are considered identifiers (see bug 21763 Problem in Java search [search])
						if (fieldName == null)
							fieldName = scanner.getCurrentTokenString();
						else
							fieldName += scanner.getCurrentTokenString();
				}
				break;
			// read type
			case InsideType:
				switch (token) {
					case TerminalTokens.TokenNameWHITESPACE:
						break;
					default: // all other tokens are considered identifiers (see bug 21763 Problem in Java search [search])
						if (type == null)
							type = scanner.getCurrentTokenString();
						else
							type += scanner.getCurrentTokenString();
				}
				break;
		}
		lastToken = token;
		try {
			token = scanner.getNextToken();
		} catch (InvalidInputException e) {
			return null;
		}
	}
	if (fieldName == null) return null;

	char[] fieldNameChars = fieldName.toCharArray();
	if (fieldNameChars.length == 1 && fieldNameChars[0] == '*') fieldNameChars = null;

	char[] declaringTypeQualification = null, declaringTypeSimpleName = null;
	char[] typeQualification = null, typeSimpleName = null;

	// extract declaring type infos
	if (declaringType != null) {
		char[] declaringTypePart = declaringType.toCharArray();
		int lastDotPosition = CharOperation.lastIndexOf('.', declaringTypePart);
		if (lastDotPosition >= 0) {
			declaringTypeQualification = CharOperation.subarray(declaringTypePart, 0, lastDotPosition);
			if (declaringTypeQualification.length == 1 && declaringTypeQualification[0] == '*')
				declaringTypeQualification = null;
			declaringTypeSimpleName = CharOperation.subarray(declaringTypePart, lastDotPosition+1, declaringTypePart.length);
		} else {
			declaringTypeSimpleName = declaringTypePart;
		}
		if (declaringTypeSimpleName.length == 1 && declaringTypeSimpleName[0] == '*')
			declaringTypeSimpleName = null;
	}
	// extract type infos
	if (type != null) {
		char[] typePart = type.toCharArray();
		int lastDotPosition = CharOperation.lastIndexOf('.', typePart);
		if (lastDotPosition >= 0) {
			typeQualification = CharOperation.subarray(typePart, 0, lastDotPosition);
			if (typeQualification.length == 1 && typeQualification[0] == '*') {
				typeQualification = null;
			} else {
				// prefix with a '*' as the full qualification could be bigger (because of an import)
				typeQualification = CharOperation.concat(IIndexConstants.ONE_STAR, typeQualification);
			}
			typeSimpleName = CharOperation.subarray(typePart, lastDotPosition+1, typePart.length);
		} else {
			typeSimpleName = typePart;
		}
		if (typeSimpleName.length == 1 && typeSimpleName[0] == '*')
			typeSimpleName = null;
	}
	// Create field pattern
	return new FieldPattern(
			fieldNameChars,
			declaringTypeQualification,
			declaringTypeSimpleName,
			typeQualification,
			typeSimpleName,
			limitTo,
			matchRule);
}

private static SearchPattern createMethodOrConstructorPattern(String patternString, int limitTo, int matchRule, boolean isConstructor) {
	// use 1.7 as the source level as there are more valid tokens in 1.7 mode
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=376673
	Scanner scanner = new Scanner(false /*comment*/, true /*whitespace*/, false /*nls*/, ClassFileConstants.JDK1_7/*sourceLevel*/, null /*taskTags*/, null/*taskPriorities*/, true/*taskCaseSensitive*/);
	scanner.setSource(patternString.toCharArray());
	final int InsideSelector = 1;
	final int InsideTypeArguments = 2;
	final int InsideParameter = 3;
	final int InsideReturnType = 4;
	int lastToken = -1;

	String declaringType = null, selector = null, parameterType = null;
	String[] parameterTypes = null;
	char[][] typeArguments = null;
	String typeArgumentsString = null;
	int parameterCount = -1;
	String returnType = null;
	boolean foundClosingParenthesis = false;
	int mode = InsideSelector;
	int token, argCount = 0;
	try {
		token = scanner.getNextToken();
	} catch (InvalidInputException e) {
		return null;
	}
	while (token != TerminalTokens.TokenNameEOF) {
		switch(mode) {
			// read declaring type and selector
			case InsideSelector :
				if (argCount == 0) {
					switch (token) {
						case TerminalTokens.TokenNameLESS:
							argCount++;
							if (selector == null || lastToken == TerminalTokens.TokenNameDOT) {
								typeArgumentsString = scanner.getCurrentTokenString();
								mode = InsideTypeArguments;
								break;
							}
							if (declaringType == null) {
								declaringType = selector;
							} else {
								declaringType += '.' + selector;
							}
							declaringType += scanner.getCurrentTokenString();
							selector = null;
							break;
						case TerminalTokens.TokenNameDOT:
							if (!isConstructor && typeArgumentsString != null) return null; // invalid syntax
							if (declaringType == null) {
								if (selector == null) return null; // invalid syntax
								declaringType = selector;
							} else if (selector != null) {
								declaringType += scanner.getCurrentTokenString() + selector;
							}
							selector = null;
							break;
						case TerminalTokens.TokenNameLPAREN:
							parameterTypes = new String[5];
							parameterCount = 0;
							mode = InsideParameter;
							break;
						case TerminalTokens.TokenNameWHITESPACE:
							switch (lastToken) {
								case TerminalTokens.TokenNameWHITESPACE:
								case TerminalTokens.TokenNameDOT:
								case TerminalTokens.TokenNameGREATER:
								case TerminalTokens.TokenNameRIGHT_SHIFT:
								case TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT:
									break;
								default:
									mode = InsideReturnType;
									break;
							}
							break;
						default: // all other tokens are considered identifiers (see bug 21763 Problem in Java search [search])
							if (selector == null)
								selector = scanner.getCurrentTokenString();
							else
								selector += scanner.getCurrentTokenString();
							break;
					}
				} else {
					if (declaringType == null) return null; // invalid syntax
					switch (token) {
						case TerminalTokens.TokenNameGREATER:
						case TerminalTokens.TokenNameRIGHT_SHIFT:
						case TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT:
							argCount--;
							break;
						case TerminalTokens.TokenNameLESS:
							argCount++;
							break;
					}
					declaringType += scanner.getCurrentTokenString();
				}
				break;
			// read type arguments
			case InsideTypeArguments:
				if (typeArgumentsString == null) return null; // invalid syntax
				typeArgumentsString += scanner.getCurrentTokenString();
				switch (token) {
					case TerminalTokens.TokenNameGREATER:
					case TerminalTokens.TokenNameRIGHT_SHIFT:
					case TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT:
						argCount--;
						if (argCount == 0) {
							String pseudoType = "Type"+typeArgumentsString; //$NON-NLS-1$
							typeArguments = Signature.getTypeArguments(Signature.createTypeSignature(pseudoType, false).toCharArray());
							mode = InsideSelector;
						}
						break;
					case TerminalTokens.TokenNameLESS:
						argCount++;
						break;
				}
				break;
			// read parameter types
			case InsideParameter :
				if (argCount == 0) {
					switch (token) {
						case TerminalTokens.TokenNameWHITESPACE:
							break;
						case TerminalTokens.TokenNameCOMMA:
							if (parameterType == null) return null;
							if (parameterTypes != null) {
								if (parameterTypes.length == parameterCount)
									System.arraycopy(parameterTypes, 0, parameterTypes = new String[parameterCount*2], 0, parameterCount);
								parameterTypes[parameterCount++] = parameterType;
							}
							parameterType = null;
							break;
						case TerminalTokens.TokenNameRPAREN:
							foundClosingParenthesis = true;
							if (parameterType != null && parameterTypes != null) {
								if (parameterTypes.length == parameterCount)
									System.arraycopy(parameterTypes, 0, parameterTypes = new String[parameterCount*2], 0, parameterCount);
								parameterTypes[parameterCount++] = parameterType;
							}
							mode = isConstructor ? InsideTypeArguments : InsideReturnType;
							break;
						case TerminalTokens.TokenNameLESS:
							argCount++;
							if (parameterType == null) return null; // invalid syntax
							// $FALL-THROUGH$ - fall through next case to add token
						default: // all other tokens are considered identifiers (see bug 21763 Problem in Java search [search])
							if (parameterType == null)
								parameterType = scanner.getCurrentTokenString();
							else
								parameterType += scanner.getCurrentTokenString();
					}
				} else {
					if (parameterType == null) return null; // invalid syntax
					switch (token) {
						case TerminalTokens.TokenNameGREATER:
						case TerminalTokens.TokenNameRIGHT_SHIFT:
						case TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT:
							argCount--;
							break;
						case TerminalTokens.TokenNameLESS:
							argCount++;
							break;
					}
					parameterType += scanner.getCurrentTokenString();
				}
				break;
			// read return type
			case InsideReturnType:
				if (argCount == 0) {
					switch (token) {
						case TerminalTokens.TokenNameWHITESPACE:
							break;
						case TerminalTokens.TokenNameLPAREN:
							parameterTypes = new String[5];
							parameterCount = 0;
							mode = InsideParameter;
							break;
						case TerminalTokens.TokenNameLESS:
							argCount++;
							if (returnType == null) return null; // invalid syntax
							// $FALL-THROUGH$ - fall through next case to add token
						default: // all other tokens are considered identifiers (see bug 21763 Problem in Java search [search])
							if (returnType == null)
								returnType = scanner.getCurrentTokenString();
							else
								returnType += scanner.getCurrentTokenString();
					}
				} else {
					if (returnType == null) return null; // invalid syntax
					switch (token) {
						case TerminalTokens.TokenNameGREATER:
						case TerminalTokens.TokenNameRIGHT_SHIFT:
						case TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT:
							argCount--;
							break;
						case TerminalTokens.TokenNameLESS:
							argCount++;
							break;
					}
					returnType += scanner.getCurrentTokenString();
				}
				break;
		}
		lastToken = token;
		try {
			token = scanner.getNextToken();
		} catch (InvalidInputException e) {
			return null;
		}
	}
	// parenthesis mismatch
	if (parameterCount>0 && !foundClosingParenthesis) return null;
	// type arguments mismatch
	if (argCount > 0) return null;

	char[] selectorChars = null;
	if (isConstructor) {
		// retrieve type for constructor patterns
		if (declaringType == null)
			declaringType = selector;
		else if (selector != null)
			declaringType += '.' + selector;
	} else {
		// get selector chars
		if (selector == null) return null;
		selectorChars = selector.toCharArray();
		if (selectorChars.length == 1 && selectorChars[0] == '*')
			selectorChars = null;
	}

	char[] declaringTypeQualification = null, declaringTypeSimpleName = null;
	char[] returnTypeQualification = null, returnTypeSimpleName = null;
	char[][] parameterTypeQualifications = null, parameterTypeSimpleNames = null;
	// Signatures
	String declaringTypeSignature = null;
	String returnTypeSignature = null;
	String[] parameterTypeSignatures = null;

	// extract declaring type infos
	if (declaringType != null) {
		// get declaring type part and signature
		char[] declaringTypePart = null;
		try {
			declaringTypeSignature = Signature.createTypeSignature(declaringType, false);
			if (declaringTypeSignature.indexOf(Signature.C_GENERIC_START) < 0) {
				declaringTypePart = declaringType.toCharArray();
			} else {
				declaringTypePart = Signature.toCharArray(Signature.getTypeErasure(declaringTypeSignature.toCharArray()));
			}
		}
		catch (IllegalArgumentException iae) {
			// declaring type is invalid
			return null;
		}
		int lastDotPosition = CharOperation.lastIndexOf('.', declaringTypePart);
		if (lastDotPosition >= 0) {
			declaringTypeQualification = CharOperation.subarray(declaringTypePart, 0, lastDotPosition);
			if (declaringTypeQualification.length == 1 && declaringTypeQualification[0] == '*')
				declaringTypeQualification = null;
			declaringTypeSimpleName = CharOperation.subarray(declaringTypePart, lastDotPosition+1, declaringTypePart.length);
		} else {
			declaringTypeSimpleName = declaringTypePart;
		}
		if (declaringTypeSimpleName.length == 1 && declaringTypeSimpleName[0] == '*')
			declaringTypeSimpleName = null;
	}
	// extract parameter types infos
	if (parameterCount >= 0) {
		parameterTypeQualifications = new char[parameterCount][];
		parameterTypeSimpleNames = new char[parameterCount][];
		parameterTypeSignatures = new String[parameterCount];
		for (int i = 0; i < parameterCount; i++) {
			// get parameter type part and signature
			char[] parameterTypePart = null;
			try {
				if (parameterTypes != null) {
					parameterTypeSignatures[i] = Signature.createTypeSignature(parameterTypes[i], false);
					if (parameterTypeSignatures[i].indexOf(Signature.C_GENERIC_START) < 0) {
						parameterTypePart = parameterTypes[i].toCharArray();
					} else {
						parameterTypePart = Signature.toCharArray(Signature.getTypeErasure(parameterTypeSignatures[i].toCharArray()));
					}
				}
			}
			catch (IllegalArgumentException iae) {
				// string is not a valid type syntax
				return null;
			}
			int lastDotPosition = parameterTypePart==null ? -1 : CharOperation.lastIndexOf('.', parameterTypePart);
			if (parameterTypePart != null && lastDotPosition >= 0) {
				parameterTypeQualifications[i] = CharOperation.subarray(parameterTypePart, 0, lastDotPosition);
				if (parameterTypeQualifications[i].length == 1 && parameterTypeQualifications[i][0] == '*') {
					parameterTypeQualifications[i] = null;
				} else {
					// prefix with a '*' as the full qualification could be bigger (because of an import)
					parameterTypeQualifications[i] = CharOperation.concat(IIndexConstants.ONE_STAR, parameterTypeQualifications[i]);
				}
				parameterTypeSimpleNames[i] = CharOperation.subarray(parameterTypePart, lastDotPosition+1, parameterTypePart.length);
			} else {
				parameterTypeQualifications[i] = null;
				parameterTypeSimpleNames[i] = parameterTypePart;
			}
			if (parameterTypeSimpleNames[i].length == 1 && parameterTypeSimpleNames[i][0] == '*')
				parameterTypeSimpleNames[i] = null;
		}
	}
	// extract return type infos
	if (returnType != null) {
		// get return type part and signature
		char[] returnTypePart = null;
		try {
			returnTypeSignature = Signature.createTypeSignature(returnType, false);
			if (returnTypeSignature.indexOf(Signature.C_GENERIC_START) < 0) {
				returnTypePart = returnType.toCharArray();
			} else {
				returnTypePart = Signature.toCharArray(Signature.getTypeErasure(returnTypeSignature.toCharArray()));
			}
		}
		catch (IllegalArgumentException iae) {
			// declaring type is invalid
			return null;
		}
		int lastDotPosition = CharOperation.lastIndexOf('.', returnTypePart);
		if (lastDotPosition >= 0) {
			returnTypeQualification = CharOperation.subarray(returnTypePart, 0, lastDotPosition);
			if (returnTypeQualification.length == 1 && returnTypeQualification[0] == '*') {
				returnTypeQualification = null;
			} else {
				// because of an import
				returnTypeQualification = CharOperation.concat(IIndexConstants.ONE_STAR, returnTypeQualification);
			}
			returnTypeSimpleName = CharOperation.subarray(returnTypePart, lastDotPosition+1, returnTypePart.length);
		} else {
			returnTypeSimpleName = returnTypePart;
		}
		if (returnTypeSimpleName.length == 1 && returnTypeSimpleName[0] == '*')
			returnTypeSimpleName = null;
	}
	// Create method/constructor pattern
	if (isConstructor) {
		return new ConstructorPattern(
				declaringTypeSimpleName,
				declaringTypeQualification,
				declaringTypeSignature,
				parameterTypeQualifications,
				parameterTypeSimpleNames,
				parameterTypeSignatures,
				typeArguments,
				limitTo,
				matchRule);
	} else {
		return new MethodPattern(
				selectorChars,
				declaringTypeQualification,
				declaringTypeSimpleName,
				declaringTypeSignature,
				returnTypeQualification,
				returnTypeSimpleName,
				returnTypeSignature,
				parameterTypeQualifications,
				parameterTypeSimpleNames,
				parameterTypeSignatures,
				typeArguments,
				limitTo,
				matchRule);
	}
}

private static SearchPattern createModulePattern(String patternString, int limitTo, int matchRule) {
	return new ModulePattern(patternString.toCharArray(), limitTo, matchRule);
}

/**
 * Returns a search pattern that combines the given two patterns into an
 * "or" pattern. The search result will match either the left pattern or the
 * right pattern.
 *
 * @param leftPattern the left pattern
 * @param rightPattern the right pattern
 * @return an "or" pattern
 */
public static SearchPattern createOrPattern(SearchPattern leftPattern, SearchPattern rightPattern) {
	return new OrPattern(leftPattern, rightPattern);
}

private static SearchPattern createPackagePattern(String patternString, int limitTo, int matchRule) {
	switch (limitTo) {
		case IJavaSearchConstants.DECLARATIONS :
			return new PackageDeclarationPattern(patternString.toCharArray(), matchRule);
		case IJavaSearchConstants.REFERENCES :
			return new PackageReferencePattern(patternString.toCharArray(), matchRule);
		case IJavaSearchConstants.ALL_OCCURRENCES :
			return new OrPattern(
				new PackageDeclarationPattern(patternString.toCharArray(), matchRule),
				new PackageReferencePattern(patternString.toCharArray(), matchRule)
			);
	}
	return null;
}

/**
 * Returns a search pattern based on a given string pattern. The string patterns support '*' wild-cards.
 * The remaining parameters are used to narrow down the type of expected results.
 *
 * <br>
 *	Examples:
 *	<ul>
 * 		<li>search for case insensitive references to <code>Object</code>:
 *			<code>createSearchPattern("Object", IJavaSearchConstants.TYPE, IJavaSearchConstants.REFERENCES, false);</code></li>
 *  	<li>search for case sensitive references to exact <code>Object()</code> constructor:
 *			<code>createSearchPattern("java.lang.Object()", IJavaSearchConstants.CONSTRUCTOR, IJavaSearchConstants.REFERENCES, true);</code></li>
 *  	<li>search for implementers of <code>java.lang.Runnable</code>:
 *			<code>createSearchPattern("java.lang.Runnable", IJavaSearchConstants.TYPE, IJavaSearchConstants.IMPLEMENTORS, true);</code></li>
 *  </ul>
 * @param stringPattern the given pattern
 * <ul>
 * 	<li>Type patterns have the following syntax:
 * 		<p><b><code>[qualification '.']typeName ['&lt;' typeArguments '&gt;']</code></b></p>
 *			<p>Examples:</p>
 *			<ul>
 * 			<li><code>java.lang.Object</code></li>
 *				<li><code>Runnable</code></li>
 *				<li><code>List&lt;String&gt;</code></li>
 *			</ul>
 *			<p>
 *			Type arguments can be specified to search for references to parameterized types
 * 		using following syntax:</p><p>
 * 		<b><code>'&lt;' { [ '?' {'extends'|'super'} ] type ( ',' [ '?' {'extends'|'super'} ] type )* | '?' } '&gt;'</code></b>
 * 		</p><div style="font-style:italic;">
 * 		Note that:
 * 		<ul>
 * 			<li>'*' is not valid inside type arguments definition &lt;&gt;</li>
 * 			<li>'?' is treated as a wildcard when it is inside &lt;&gt; (i.e. it must be put on first position of the type argument)</li>
 * 		</ul>
 * 		</div>
 * 		Since 3.14 for Java 9, Type Declaration Patterns can have module names also embedded with the following syntax
 * 		<p><b><code>[moduleName1[,moduleName2,..]]/[qualification '.']typeName ['&lt;' typeArguments '&gt;']</code></b>
 *      </p>
 *      <p>
 *      Unnamed modules can also be included and are represented either by an absence of module name implicitly
 *      or explicitly by specifying ALL-UNNAMED for module name.
 * 		Module graph search is also supported with the limitTo option set to <code>IJavaSearchConstants.MODULE_GRAPH</code>.
 *      In the module graph case, the given type is searched in all the modules required directly as well
 *      as indirectly by the given module(s).
 *      </p>
 *      <p>
 *      Note that whitespaces are ignored in between module names. It is an error to give multiple module separators - in such
 *      cases a null pattern will be returned.
 *      </p>
 *			<p>Examples:</p>
 *			<ul>
 * 				<li><code>java.base/java.lang.Object</code></li>
 *				<li><code>mod.one, mod.two/pack.X</code> find declaration in the list of given modules.</li>
 *				<li><code>/pack.X</code> find in the unnamed module.</li>
 *				<li><code>ALL-UNNAMED/pack.X</code> find in the unnamed module.</li>
 *			</ul>
 *			<p>
 * 	</li>
 * 	<li>Method patterns have the following syntax:
 * 		<p><b><code>[declaringType '.'] ['&lt;' typeArguments '&gt;'] methodName ['(' parameterTypes ')'] [returnType]</code></b></p>
 *			<p>Type arguments have the same syntax as explained in the type patterns section.</p>
 *			<p>Examples:</p>
 *			<ul>
 *				<li><code>java.lang.Runnable.run() void</code></li>
 *				<li><code>main(*)</code></li>
 *				<li><code>&lt;String&gt;toArray(String[])</code></li>
 *			</ul>
 *	</li>
 * 	<li>Constructor patterns have the following syntax:
 *			<p><b><code>['&lt;' typeArguments '&gt;'] [declaringQualification '.'] typeName ['(' parameterTypes ')']</code></b></p>
 *			<p>Type arguments have the same syntax as explained in the type patterns section.</p>
 *			<p><i>Note that the constructor name should not be entered as it is always the same as the type name.</i></p>
 *			<p>Examples:</p>
 *			<ul>
 *				<li><code>java.lang.Object()</code></li>
 *				<li><code>Test(*)</code></li>
 *				<li><code>&lt;Exception&gt;Sample(Exception)</code></li>
 *			</ul>
 * 		<br>
 * 	</li>
 * 	<li>Field patterns have the following syntax:
 *			<p><b><code>[declaringType '.'] fieldName [fieldType]</code></b></p>
 *			<p>Examples:</p>
 *			<ul>
 *				<li><code>java.lang.String.serialVersionUID long</code></li>
 *				<li><code>field*</code></li>
 *			</ul>
 * 	</li>
 * 	<li>Package patterns have the following syntax:
 *			<p><b><code>packageNameSegment {'.' packageNameSegment}</code></b></p>
 *			<p>Examples:</p>
 *			<ul>
 *				<li><code>java.lang</code></li>
 *				<li><code>org.e*.jdt.c*e</code></li>
 *			</ul>
 * 	</li>
 * </ul>
 * @param searchFor determines the nature of the searched elements
 *	<ul>
 * 	<li>{@link IJavaSearchConstants#CLASS}: only look for classes</li>
 *	<li>{@link IJavaSearchConstants#INTERFACE}: only look for interfaces</li>
 * 	<li>{@link IJavaSearchConstants#ENUM}: only look for enumeration</li>
 *	<li>{@link IJavaSearchConstants#ANNOTATION_TYPE}: only look for annotation type</li>
 * 	<li>{@link IJavaSearchConstants#CLASS_AND_ENUM}: only look for classes and enumerations</li>
 *	<li>{@link IJavaSearchConstants#CLASS_AND_INTERFACE}: only look for classes and interfaces</li>
 * 	<li>{@link IJavaSearchConstants#TYPE}: look for all types (i.e. classes, interfaces, enum and annotation types)</li>
 *	<li>{@link IJavaSearchConstants#FIELD}: look for fields</li>
 *	<li>{@link IJavaSearchConstants#METHOD}: look for methods</li>
 *	<li>{@link IJavaSearchConstants#CONSTRUCTOR}: look for constructors</li>
 *	<li>{@link IJavaSearchConstants#PACKAGE}: look for packages</li>
 *	<li>{@link IJavaSearchConstants#MODULE}: look for modules</li>
 *	</ul>
 * @param limitTo determines the nature of the expected matches
 *	<ul>
 * 	<li>{@link IJavaSearchConstants#DECLARATIONS DECLARATIONS}: will search declarations matching
 * 			with the corresponding element. In case the element is a method, declarations of matching
 * 			methods in sub-types will also be found, allowing to find declarations of abstract methods, etc.<br>
 * 			Note that additional flags {@link IJavaSearchConstants#IGNORE_DECLARING_TYPE IGNORE_DECLARING_TYPE} and
 * 			{@link IJavaSearchConstants#IGNORE_RETURN_TYPE IGNORE_RETURN_TYPE} are ignored for string patterns.
 * 			This is due to the fact that client may omit to define them in string pattern to have same behavior.
 * 	</li>
 *		 <li>{@link IJavaSearchConstants#REFERENCES REFERENCES}: will search references to the given element.</li>
 *		 <li>{@link IJavaSearchConstants#ALL_OCCURRENCES ALL_OCCURRENCES}: will search for either declarations or
 *				references as specified above.
 *		</li>
 *		 <li>{@link IJavaSearchConstants#IMPLEMENTORS IMPLEMENTORS}: for types, will find all types
 *				which directly implement/extend a given interface.
 *				Note that types may be only classes or only interfaces if {@link IJavaSearchConstants#CLASS CLASS} or
 *				{@link IJavaSearchConstants#INTERFACE INTERFACE} is respectively used instead of {@link IJavaSearchConstants#TYPE TYPE}.
 *		</li>
 *		 <li>{@link IJavaSearchConstants#MODULE_GRAPH MODULE_GRAPH}: for types with a module prefix,
 *             will find all types present in required modules (directly or indirectly required) ie
 *             in any module present in the module graph of the given module.
 *		</li>
 *		 <li>All other fine grain constants defined in the <b>limitTo</b> category
 *				of the {@link IJavaSearchConstants} are also accepted nature:
 * 			<table>
 *     			<tr>
 *         		<th>Fine grain constant
 *         		<th>Meaning
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#FIELD_DECLARATION_TYPE_REFERENCE FIELD_DECLARATION_TYPE_REFERENCE}
 *         		<td>Return only type references used as the type of a field declaration.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#LOCAL_VARIABLE_DECLARATION_TYPE_REFERENCE LOCAL_VARIABLE_DECLARATION_TYPE_REFERENCE}
 *         		<td>Return only type references used as the type of a local variable declaration.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#PARAMETER_DECLARATION_TYPE_REFERENCE PARAMETER_DECLARATION_TYPE_REFERENCE}
 *         		<td>Return only type references used as the type of a method parameter declaration.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#SUPERTYPE_TYPE_REFERENCE SUPERTYPE_TYPE_REFERENCE}
 *         		<td>Return only type references used as a super type or as a super interface.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#THROWS_CLAUSE_TYPE_REFERENCE THROWS_CLAUSE_TYPE_REFERENCE}
 *         		<td>Return only type references used in a throws clause.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#CAST_TYPE_REFERENCE CAST_TYPE_REFERENCE}
 *         		<td>Return only type references used in a cast expression.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#CATCH_TYPE_REFERENCE CATCH_TYPE_REFERENCE}
 *         		<td>Return only type references used in a catch header.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#CLASS_INSTANCE_CREATION_TYPE_REFERENCE CLASS_INSTANCE_CREATION_TYPE_REFERENCE}
 *         		<td>Return only type references used in class instance creation.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#RETURN_TYPE_REFERENCE RETURN_TYPE_REFERENCE}
 *         		<td>Return only type references used as a method return type.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#IMPORT_DECLARATION_TYPE_REFERENCE IMPORT_DECLARATION_TYPE_REFERENCE}
 *         		<td>Return only type references used in an import declaration.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#ANNOTATION_TYPE_REFERENCE ANNOTATION_TYPE_REFERENCE}
 *         		<td>Return only type references used as an annotation.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#TYPE_ARGUMENT_TYPE_REFERENCE TYPE_ARGUMENT_TYPE_REFERENCE}
 *         		<td>Return only type references used as a type argument in a parameterized type or a parameterized method.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#TYPE_VARIABLE_BOUND_TYPE_REFERENCE TYPE_VARIABLE_BOUND_TYPE_REFERENCE}
 *         		<td>Return only type references used as a type variable bound.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#WILDCARD_BOUND_TYPE_REFERENCE WILDCARD_BOUND_TYPE_REFERENCE}
 *         		<td>Return only type references used as a wildcard bound.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#INSTANCEOF_TYPE_REFERENCE INSTANCEOF_TYPE_REFERENCE}
 *         		<td>Return only type references used as a type of an <code>instanceof</code> expression.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#SUPER_REFERENCE SUPER_REFERENCE}
 *         		<td>Return only super field accesses or super method invocations (e.g. using the <code>super</code> qualifier).
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#QUALIFIED_REFERENCE QUALIFIED_REFERENCE}
 *         		<td>Return only qualified field accesses or qualified method invocations.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#THIS_REFERENCE THIS_REFERENCE}
 *         		<td>Return only primary field accesses or primary method invocations (e.g. using the <code>this</code> qualifier).
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#IMPLICIT_THIS_REFERENCE IMPLICIT_THIS_REFERENCE}
 *         		<td>Return only field accesses or method invocations without any qualification.
 *				<tr>
 *         		<td>{@link IJavaSearchConstants#METHOD_REFERENCE_EXPRESSION METHOD_REFERENCE_EXPRESSION}
 *         		<td>Return only method reference expressions (e.g. <code>A :: foo</code>).
 * 			</table>
 *		</li>
 *	</ul>
 * @param matchRule one of the following match rules
 * 	<ul>
 * 		<li>{@link #R_EXACT_MATCH}</li>
 * 		<li>{@link #R_PREFIX_MATCH}</li>
 * 		<li>{@link #R_PATTERN_MATCH}</li>
 * 		<li>{@link #R_CAMELCASE_MATCH}</li>
 * 		<li>{@link #R_CAMELCASE_SAME_PART_COUNT_MATCH}</li>
 * 	</ul>
 * 	, which may be also combined with one of the following flags:
 * 	<ul>
 * 		<li>{@link #R_CASE_SENSITIVE}</li>
 * 		<li>{@link #R_ERASURE_MATCH}</li>
 * 		<li>{@link #R_EQUIVALENT_MATCH}</li>
 * 	</ul>
 *		For example,
 *		<ul>
 *			<li>{@link #R_EXACT_MATCH} | {@link #R_CASE_SENSITIVE}: if an exact
 *				and case sensitive match is requested,</li>
 *			<li>{@link #R_PREFIX_MATCH} if a case insensitive prefix match is requested</li>
 *			<li>{@link #R_EXACT_MATCH} | {@link #R_ERASURE_MATCH}: if a case
 *				insensitive and erasure match is requested.</li>
 *		</ul>
 * 	<p>Note that {@link #R_ERASURE_MATCH} or {@link #R_EQUIVALENT_MATCH} has no effect
 * 	on non-generic types/methods search.</p>
 *
 * 	<p>Note that {@link #R_REGEXP_MATCH} is supported since 3.14  for the special case of
 * {@link IJavaSearchConstants#DECLARATIONS DECLARATIONS} search of
 * {@link IJavaSearchConstants#MODULE MODULE}</p>
 * 	<p>
 * 	Note also that the default behavior for generic types/methods search is to find exact matches.</p>
 * @return a search pattern on the given string pattern, or <code>null</code> if the string pattern is ill-formed
 */
public static SearchPattern createPattern(String stringPattern, int searchFor, int limitTo, int matchRule) {
	if (stringPattern == null || stringPattern.length() == 0) return null;

	if ((matchRule = validateMatchRule(stringPattern, searchFor, limitTo, matchRule)) == -1) {
		return null;
	}

	// Ignore additional nature flags
	limitTo &= ~(IJavaSearchConstants.IGNORE_DECLARING_TYPE+IJavaSearchConstants.IGNORE_RETURN_TYPE);

	switch (searchFor) {
		case IJavaSearchConstants.CLASS:
			return createTypePattern(stringPattern, limitTo, matchRule, IIndexConstants.CLASS_SUFFIX);
		case IJavaSearchConstants.CLASS_AND_INTERFACE:
			return createTypePattern(stringPattern, limitTo, matchRule, IIndexConstants.CLASS_AND_INTERFACE_SUFFIX);
		case IJavaSearchConstants.CLASS_AND_ENUM:
			return createTypePattern(stringPattern, limitTo, matchRule, IIndexConstants.CLASS_AND_ENUM_SUFFIX);
		case IJavaSearchConstants.INTERFACE:
			return createTypePattern(stringPattern, limitTo, matchRule, IIndexConstants.INTERFACE_SUFFIX);
		case IJavaSearchConstants.INTERFACE_AND_ANNOTATION:
			return createTypePattern(stringPattern, limitTo, matchRule, IIndexConstants.INTERFACE_AND_ANNOTATION_SUFFIX);
		case IJavaSearchConstants.ENUM:
			return createTypePattern(stringPattern, limitTo, matchRule, IIndexConstants.ENUM_SUFFIX);
		case IJavaSearchConstants.ANNOTATION_TYPE:
			return createTypePattern(stringPattern, limitTo, matchRule, IIndexConstants.ANNOTATION_TYPE_SUFFIX);
		case IJavaSearchConstants.TYPE:
			return createTypePattern(stringPattern, limitTo, matchRule, IIndexConstants.TYPE_SUFFIX);
		case IJavaSearchConstants.METHOD:
			return createMethodOrConstructorPattern(stringPattern, limitTo, matchRule, false/*not a constructor*/);
		case IJavaSearchConstants.CONSTRUCTOR:
			return createMethodOrConstructorPattern(stringPattern, limitTo, matchRule, true/*constructor*/);
		case IJavaSearchConstants.FIELD:
			return createFieldPattern(stringPattern, limitTo, matchRule);
		case IJavaSearchConstants.PACKAGE:
			return createPackagePattern(stringPattern, limitTo, matchRule);
		case IJavaSearchConstants.MODULE :
			return createModulePattern(stringPattern, limitTo, matchRule);
	}
	return null;
}

/**
 * Returns a search pattern based on a given Java element.
 * The pattern is used to trigger the appropriate search.
 * <br>
 * Note that for generic searches, the returned pattern consider {@link #R_ERASURE_MATCH} matches.
 * If other kind of generic matches (i.e. {@link #R_EXACT_MATCH} or {@link #R_EQUIVALENT_MATCH})
 * are expected, {@link #createPattern(IJavaElement, int, int)} method need to be used instead with
 * the explicit match rule specified.
 * <br>
 * The pattern can be parameterized as follows:
 *
 * @param element the Java element the search pattern is based on
 * @param limitTo determines the nature of the expected matches
 *	<ul>
 * 	<li>{@link IJavaSearchConstants#DECLARATIONS DECLARATIONS}: will search declarations matching
 * 			with the corresponding element. In case the element is a method, declarations of matching
 * 			methods in sub-types will also be found, allowing to find declarations of abstract methods, etc.
 *				Some additional flags may be specified while searching declaration:
 *				<ul>
 *					<li>{@link IJavaSearchConstants#IGNORE_DECLARING_TYPE IGNORE_DECLARING_TYPE}: declaring type will be ignored
 *							during the search.<br>
 *							For example using following test case:
 *					<pre>
 *                  class A { A method() { return null; } }
 *                  class B extends A { B method() { return null; } }
 *                  class C { A method() { return null; } }
 *					</pre>
 *							search for <code>method</code> declaration with this flag
 *							will return 2 matches: in A and in C
 *					</li>
 *					<li>{@link IJavaSearchConstants#IGNORE_RETURN_TYPE IGNORE_RETURN_TYPE}: return type will be ignored
 *							during the search.<br>
 *							Using same example, search for <code>method</code> declaration with this flag
 *							will return 2 matches: in A and in B.
 *					</li>
 *				</ul>
 *				Note that these two flags may be combined and both declaring and return types can be ignored
 *				during the search. Then, using same example, search for <code>method</code> declaration
 *				with these 2 flags will return 3 matches: in A, in B  and in C
 * 	</li>
 *		 <li>{@link IJavaSearchConstants#REFERENCES REFERENCES}: will search references to the given element.</li>
 *		 <li>{@link IJavaSearchConstants#ALL_OCCURRENCES ALL_OCCURRENCES}: will search for either declarations or
 *				references as specified above.
 *		</li>
 *		 <li>All other fine grain constants defined in the <b>limitTo</b> category
 *				of the {@link IJavaSearchConstants} are also accepted nature:
 * 			<table>
 *     			<tr>
 *         		<th>Fine grain constant
 *         		<th>Meaning
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#FIELD_DECLARATION_TYPE_REFERENCE FIELD_DECLARATION_TYPE_REFERENCE}
 *         		<td>Return only type references used as the type of a field declaration.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#LOCAL_VARIABLE_DECLARATION_TYPE_REFERENCE LOCAL_VARIABLE_DECLARATION_TYPE_REFERENCE}
 *         		<td>Return only type references used as the type of a local variable declaration.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#PARAMETER_DECLARATION_TYPE_REFERENCE PARAMETER_DECLARATION_TYPE_REFERENCE}
 *         		<td>Return only type references used as the type of a method parameter declaration.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#SUPERTYPE_TYPE_REFERENCE SUPERTYPE_TYPE_REFERENCE}
 *         		<td>Return only type references used as a super type or as a super interface.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#THROWS_CLAUSE_TYPE_REFERENCE THROWS_CLAUSE_TYPE_REFERENCE}
 *         		<td>Return only type references used in a throws clause.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#CAST_TYPE_REFERENCE CAST_TYPE_REFERENCE}
 *         		<td>Return only type references used in a cast expression.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#CATCH_TYPE_REFERENCE CATCH_TYPE_REFERENCE}
 *         		<td>Return only type references used in a catch header.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#CLASS_INSTANCE_CREATION_TYPE_REFERENCE CLASS_INSTANCE_CREATION_TYPE_REFERENCE}
 *         		<td>Return only type references used in class instance creation.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#RETURN_TYPE_REFERENCE RETURN_TYPE_REFERENCE}
 *         		<td>Return only type references used as a method return type.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#IMPORT_DECLARATION_TYPE_REFERENCE IMPORT_DECLARATION_TYPE_REFERENCE}
 *         		<td>Return only type references used in an import declaration.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#ANNOTATION_TYPE_REFERENCE ANNOTATION_TYPE_REFERENCE}
 *         		<td>Return only type references used as an annotation.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#TYPE_ARGUMENT_TYPE_REFERENCE TYPE_ARGUMENT_TYPE_REFERENCE}
 *         		<td>Return only type references used as a type argument in a parameterized type or a parameterized method.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#TYPE_VARIABLE_BOUND_TYPE_REFERENCE TYPE_VARIABLE_BOUND_TYPE_REFERENCE}
 *         		<td>Return only type references used as a type variable bound.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#WILDCARD_BOUND_TYPE_REFERENCE WILDCARD_BOUND_TYPE_REFERENCE}
 *         		<td>Return only type references used as a wildcard bound.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#INSTANCEOF_TYPE_REFERENCE INSTANCEOF_TYPE_REFERENCE}
 *         		<td>Return only type references used as a type of an <code>instanceof</code> expression.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#SUPER_REFERENCE SUPER_REFERENCE}
 *         		<td>Return only super field accesses or super method invocations (e.g. using the <code>super</code> qualifier).
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#QUALIFIED_REFERENCE QUALIFIED_REFERENCE}
 *         		<td>Return only qualified field accesses or qualified method invocations.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#THIS_REFERENCE THIS_REFERENCE}
 *         		<td>Return only primary field accesses or primary method invocations (e.g. using the <code>this</code> qualifier).
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#IMPLICIT_THIS_REFERENCE IMPLICIT_THIS_REFERENCE}
 *         		<td>Return only field accesses or method invocations without any qualification.
*				<tr>
 *         		<td>{@link IJavaSearchConstants#METHOD_REFERENCE_EXPRESSION METHOD_REFERENCE_EXPRESSION}
 *         		<td>Return only method reference expressions (e.g. <code>A :: foo</code>).
* 			</table>
 *		</li>
 *	</ul>
 * @return a search pattern for a Java element or <code>null</code> if the given element is ill-formed
 */
public static SearchPattern createPattern(IJavaElement element, int limitTo) {
	return createPattern(element, limitTo, R_EXACT_MATCH | R_CASE_SENSITIVE | R_ERASURE_MATCH);
}

/**
 * Returns a search pattern based on a given Java element.
 * The pattern is used to trigger the appropriate search, and can be parameterized as follows:
 *
 * @param element the Java element the search pattern is based on
 * @param limitTo determines the nature of the expected matches
 *	<ul>
 * 	<li>{@link IJavaSearchConstants#DECLARATIONS DECLARATIONS}: will search declarations matching
 * 			with the corresponding element. In case the element is a method, declarations of matching
 * 			methods in sub-types will also be found, allowing to find declarations of abstract methods, etc.
 *				Some additional flags may be specified while searching declaration:
 *				<ul>
 *					<li>{@link IJavaSearchConstants#IGNORE_DECLARING_TYPE IGNORE_DECLARING_TYPE}: declaring type will be ignored
 *							during the search.<br>
 *							For example using following test case:
 *					<pre>
 *                  class A { A method() { return null; } }
 *                  class B extends A { B method() { return null; } }
 *                  class C { A method() { return null; } }
 *					</pre>
 *							search for <code>method</code> declaration with this flag
 *							will return 2 matches: in A and in C
 *					</li>
 *					<li>{@link IJavaSearchConstants#IGNORE_RETURN_TYPE IGNORE_RETURN_TYPE}: return type will be ignored
 *							during the search.<br>
 *							Using same example, search for <code>method</code> declaration with this flag
 *							will return 2 matches: in A and in B.
 *					</li>
 *				</ul>
 *				Note that these two flags may be combined and both declaring and return types can be ignored
 *				during the search. Then, using same example, search for <code>method</code> declaration
 *				with these 2 flags will return 3 matches: in A, in B  and in C
 * 	</li>
 *		 <li>{@link IJavaSearchConstants#REFERENCES REFERENCES}: will search references to the given element.</li>
 *		 <li>{@link IJavaSearchConstants#ALL_OCCURRENCES ALL_OCCURRENCES}: will search for either declarations or
 *				references as specified above.
 *		</li>
 *		 <li>All other fine grain constants defined in the <b>limitTo</b> category
 *				of the {@link IJavaSearchConstants} are also accepted nature:
 * 			<table>
 *     			<tr>
 *         		<th>Fine grain constant
 *         		<th>Meaning
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#FIELD_DECLARATION_TYPE_REFERENCE FIELD_DECLARATION_TYPE_REFERENCE}
 *         		<td>Return only type references used as the type of a field declaration.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#LOCAL_VARIABLE_DECLARATION_TYPE_REFERENCE LOCAL_VARIABLE_DECLARATION_TYPE_REFERENCE}
 *         		<td>Return only type references used as the type of a local variable declaration.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#PARAMETER_DECLARATION_TYPE_REFERENCE PARAMETER_DECLARATION_TYPE_REFERENCE}
 *         		<td>Return only type references used as the type of a method parameter declaration.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#SUPERTYPE_TYPE_REFERENCE SUPERTYPE_TYPE_REFERENCE}
 *         		<td>Return only type references used as a super type or as a super interface.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#THROWS_CLAUSE_TYPE_REFERENCE THROWS_CLAUSE_TYPE_REFERENCE}
 *         		<td>Return only type references used in a throws clause.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#CAST_TYPE_REFERENCE CAST_TYPE_REFERENCE}
 *         		<td>Return only type references used in a cast expression.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#CATCH_TYPE_REFERENCE CATCH_TYPE_REFERENCE}
 *         		<td>Return only type references used in a catch header.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#CLASS_INSTANCE_CREATION_TYPE_REFERENCE CLASS_INSTANCE_CREATION_TYPE_REFERENCE}
 *         		<td>Return only type references used in class instance creation.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#RETURN_TYPE_REFERENCE RETURN_TYPE_REFERENCE}
 *         		<td>Return only type references used as a method return type.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#IMPORT_DECLARATION_TYPE_REFERENCE IMPORT_DECLARATION_TYPE_REFERENCE}
 *         		<td>Return only type references used in an import declaration.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#ANNOTATION_TYPE_REFERENCE ANNOTATION_TYPE_REFERENCE}
 *         		<td>Return only type references used as an annotation.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#TYPE_ARGUMENT_TYPE_REFERENCE TYPE_ARGUMENT_TYPE_REFERENCE}
 *         		<td>Return only type references used as a type argument in a parameterized type or a parameterized method.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#TYPE_VARIABLE_BOUND_TYPE_REFERENCE TYPE_VARIABLE_BOUND_TYPE_REFERENCE}
 *         		<td>Return only type references used as a type variable bound.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#WILDCARD_BOUND_TYPE_REFERENCE WILDCARD_BOUND_TYPE_REFERENCE}
 *         		<td>Return only type references used as a wildcard bound.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#INSTANCEOF_TYPE_REFERENCE INSTANCEOF_TYPE_REFERENCE}
 *         		<td>Return only type references used as a type of an <code>instanceof</code> expression.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#SUPER_REFERENCE SUPER_REFERENCE}
 *         		<td>Return only super field accesses or super method invocations (e.g. using the <code>super</code> qualifier).
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#QUALIFIED_REFERENCE QUALIFIED_REFERENCE}
 *         		<td>Return only qualified field accesses or qualified method invocations.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#THIS_REFERENCE THIS_REFERENCE}
 *         		<td>Return only primary field accesses or primary method invocations (e.g. using the <code>this</code> qualifier).
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#IMPLICIT_THIS_REFERENCE IMPLICIT_THIS_REFERENCE}
 *         		<td>Return only field accesses or method invocations without any qualification.
 *     			<tr>
 *         		<td>{@link IJavaSearchConstants#METHOD_REFERENCE_EXPRESSION METHOD_REFERENCE_EXPRESSION}
 *         		<td>Return only method reference expressions (e.g. <code>A :: foo</code>).
 *         		<tr>
 *         		<td>{@link IJavaSearchConstants#PERMITTYPE_TYPE_REFERENCE PERMITTYPE_TYPE_REFERENCE}
 *         		<td>Return only type references used as a permit type.
 * 			</table>
 * 	</li>
 *	</ul>
 * @param matchRule one of the following match rules:
 * 	<ul>
 * 		<li>{@link #R_EXACT_MATCH}</li>
 * 		<li>{@link #R_PREFIX_MATCH}</li>
 * 		<li>{@link #R_PATTERN_MATCH}</li>
 * 		<li>{@link #R_CAMELCASE_MATCH}</li>
 * 		<li>{@link #R_CAMELCASE_SAME_PART_COUNT_MATCH}</li>
 * 	</ul>
 * 	, which may be also combined with one of the following flags:
 * 	<ul>
 * 		<li>{@link #R_CASE_SENSITIVE}</li>
 * 		<li>{@link #R_ERASURE_MATCH}</li>
 * 		<li>{@link #R_EQUIVALENT_MATCH}</li>
 * 	</ul>
 *		For example,
 *		<ul>
 *			<li>{@link #R_EXACT_MATCH} | {@link #R_CASE_SENSITIVE}: if an exact
 *				and case sensitive match is requested,</li>
 *			<li>{@link #R_PREFIX_MATCH} if a case insensitive prefix match is requested</li>
 *			<li>{@link #R_EXACT_MATCH} | {@link #R_ERASURE_MATCH}: if a case
 *				insensitive and erasure match is requested.</li>
 *		</ul>
 * 	Note that {@link #R_ERASURE_MATCH} or {@link #R_EQUIVALENT_MATCH} has no effect
 * 	on non-generic types/methods search.
 * 	<p>
 * 	Note also that default behavior for generic types/methods search is to find exact matches.
 * @return a search pattern for a Java element or <code>null</code> if the given element is ill-formed
 * @since 3.1
 */
public static SearchPattern createPattern(IJavaElement element, int limitTo, int matchRule) {
	SearchPattern searchPattern = null;
	int lastDot;
	boolean ignoreDeclaringType = false;
	boolean ignoreReturnType = false;
	int maskedLimitTo = limitTo & ~(IJavaSearchConstants.IGNORE_DECLARING_TYPE+IJavaSearchConstants.IGNORE_RETURN_TYPE);
	if (maskedLimitTo == IJavaSearchConstants.DECLARATIONS || maskedLimitTo == IJavaSearchConstants.ALL_OCCURRENCES) {
		ignoreDeclaringType = (limitTo & IJavaSearchConstants.IGNORE_DECLARING_TYPE) != 0;
		ignoreReturnType = (limitTo & IJavaSearchConstants.IGNORE_RETURN_TYPE) != 0;
	}
	if ((matchRule = validateMatchRule(null, matchRule)) == -1) {
		return null;
	}
	char[] declaringSimpleName = null;
	char[] declaringQualification = null;
	switch (element.getElementType()) {
		case IJavaElement.FIELD :
			IField field = (IField) element;
			if (!ignoreDeclaringType) {
				IType declaringClass = field.getDeclaringType();
				declaringSimpleName = declaringClass.getElementName().toCharArray();
				declaringQualification = declaringClass.getPackageFragment().getElementName().toCharArray();
				char[][] enclosingNames = enclosingTypeNames(declaringClass);
				if (enclosingNames.length > 0) {
					declaringQualification = CharOperation.concat(declaringQualification, CharOperation.concatWith(enclosingNames, '.'), '.');
				}
			}
			char[] name = field.getElementName().toCharArray();
			char[] typeSimpleName = null;
			char[] typeQualification = null;
			String typeSignature = null;
			if (!ignoreReturnType) {
				try {
					typeSignature = field.getTypeSignature();
					char[] signature = typeSignature.toCharArray();
					char[] typeErasure = Signature.toCharArray(Signature.getTypeErasure(signature));
					CharOperation.replace(typeErasure, '$', '.');
					if ((lastDot = CharOperation.lastIndexOf('.', typeErasure)) == -1) {
						typeSimpleName = typeErasure;
					} else {
						typeSimpleName = CharOperation.subarray(typeErasure, lastDot + 1, typeErasure.length);
						typeQualification = CharOperation.subarray(typeErasure, 0, lastDot);
						if (!field.isBinary()) {
							// prefix with a '*' as the full qualification could be bigger (because of an import)
							typeQualification = CharOperation.concat(IIndexConstants.ONE_STAR, typeQualification);
						}
					}
				} catch (JavaModelException e) {
					return null;
				}
			}
			// Create field pattern
			searchPattern =
				new FieldPattern(
					name,
					declaringQualification,
					declaringSimpleName,
					typeQualification,
					typeSimpleName,
					typeSignature,
					limitTo,
					matchRule);

			//If field is record's component, create a OR pattern comprising of record's component and its accessor methods
			IType declaringType = field.getDeclaringType();
			try {
				if( declaringType.isRecord()){
					MethodPattern accessorMethodPattern = new MethodPattern(name,
							declaringQualification,
							declaringSimpleName,
							typeQualification,
							typeSimpleName,
							null,
							null,
							field.getDeclaringType(),
							limitTo,
							matchRule);

					searchPattern= new OrPattern(searchPattern,accessorMethodPattern);
				}
			} catch (JavaModelException e1) {
			// continue with previous searchPattern
			}

			break;
		case IJavaElement.IMPORT_DECLARATION :
			String elementName = element.getElementName();
			lastDot = elementName.lastIndexOf('.');
			if (lastDot == -1) return null; // invalid import declaration
			IImportDeclaration importDecl = (IImportDeclaration)element;
			if (importDecl.isOnDemand()) {
				searchPattern = createPackagePattern(elementName.substring(0, lastDot), maskedLimitTo, matchRule);
			} else {
				searchPattern =
					createTypePattern(
						elementName.substring(lastDot+1).toCharArray(),
						elementName.substring(0, lastDot).toCharArray(),
						null,
						null,
						null,
						maskedLimitTo,
						matchRule);
			}
			break;
		case IJavaElement.LOCAL_VARIABLE :
			LocalVariable localVar = (LocalVariable) element;
			searchPattern = new LocalVariablePattern(localVar, limitTo, matchRule);
			break;
		case IJavaElement.TYPE_PARAMETER:
			ITypeParameter typeParam = (ITypeParameter) element;
			boolean findParamDeclarations = true;
			boolean findParamReferences = true;
			switch (maskedLimitTo) {
				case IJavaSearchConstants.DECLARATIONS :
					findParamReferences = false;
					break;
				case IJavaSearchConstants.REFERENCES :
					findParamDeclarations = false;
					break;
			}
			searchPattern =
				new TypeParameterPattern(
					findParamDeclarations,
					findParamReferences,
					typeParam,
					matchRule);
			break;
		case IJavaElement.METHOD :
			IMethod method = (IMethod) element;
			boolean isConstructor;
			try {
				isConstructor = method.isConstructor();
			} catch (JavaModelException e) {
				return null;
			}
			IType declaringClass = method.getDeclaringType();
			if (ignoreDeclaringType) {
				if (isConstructor) declaringSimpleName = declaringClass.getElementName().toCharArray();
			} else {
				declaringSimpleName = declaringClass.getElementName().toCharArray();
				declaringQualification = declaringClass.getPackageFragment().getElementName().toCharArray();
				char[][] enclosingNames = enclosingTypeNames(declaringClass);
				if (enclosingNames.length > 0) {
					declaringQualification = CharOperation.concat(declaringQualification, CharOperation.concatWith(enclosingNames, '.'), '.');
				}
			}
			char[] selector = method.getElementName().toCharArray();
			char[] returnSimpleName = null;
			char[] returnQualification = null;
			String returnSignature = null;
			if (!ignoreReturnType) {
				try {
					returnSignature = method.getReturnType();
					char[] signature = returnSignature.toCharArray();
					char[] returnErasure = Signature.toCharArray(Signature.getTypeErasure(signature));
					CharOperation.replace(returnErasure, '$', '.');
					if ((lastDot = CharOperation.lastIndexOf('.', returnErasure)) == -1) {
						returnSimpleName = returnErasure;
					} else {
						returnSimpleName = CharOperation.subarray(returnErasure, lastDot + 1, returnErasure.length);
						returnQualification = CharOperation.subarray(returnErasure, 0, lastDot);
						if (!method.isBinary()) {
							// prefix with a '*' as the full qualification could be bigger (because of an import)
							CharOperation.concat(IIndexConstants.ONE_STAR, returnQualification);
						}
					}
				} catch (JavaModelException e) {
					return null;
				}
			}
			String[] parameterTypes = method.getParameterTypes();
			int paramCount = parameterTypes.length;
			char[][] parameterSimpleNames = new char[paramCount][];
			char[][] parameterQualifications = new char[paramCount][];
			String[] parameterSignatures = new String[paramCount];
			for (int i = 0; i < paramCount; i++) {
				parameterSignatures[i] = parameterTypes[i];
				char[] signature = parameterSignatures[i].toCharArray();
				char[] paramErasure = Signature.toCharArray(Signature.getTypeErasure(signature));
				CharOperation.replace(paramErasure, '$', '.');
				if ((lastDot = CharOperation.lastIndexOf('.', paramErasure)) == -1) {
					parameterSimpleNames[i] = paramErasure;
					parameterQualifications[i] = null;
				} else {
					parameterSimpleNames[i] = CharOperation.subarray(paramErasure, lastDot + 1, paramErasure.length);
					parameterQualifications[i] = CharOperation.subarray(paramErasure, 0, lastDot);
					if (!method.isBinary()) {
						// prefix with a '*' as the full qualification could be bigger (because of an import)
						CharOperation.concat(IIndexConstants.ONE_STAR, parameterQualifications[i]);
					}
				}
			}

			// Create method/constructor pattern
			if (isConstructor) {
				searchPattern =
					new ConstructorPattern(
						declaringSimpleName,
						declaringQualification,
						parameterQualifications,
						parameterSimpleNames,
						parameterSignatures,
						method,
						limitTo,
						matchRule);
			} else {
				searchPattern =
					new MethodPattern(
						selector,
						declaringQualification,
						declaringSimpleName,
						returnQualification,
						returnSimpleName,
						returnSignature,
						parameterQualifications,
						parameterSimpleNames,
						parameterSignatures,
						method,
						limitTo,
						matchRule);
			}
			break;
		case IJavaElement.TYPE :
			IType type = (IType)element;
			searchPattern = 	createTypePattern(
						type.getElementName().toCharArray(),
						type.getPackageFragment().getElementName().toCharArray(),
						ignoreDeclaringType ? null : enclosingTypeNames(type),
						null,
						type,
						maskedLimitTo,
						matchRule);
			break;
		case IJavaElement.PACKAGE_DECLARATION :
		case IJavaElement.PACKAGE_FRAGMENT :
			searchPattern = createPackagePattern(element.getElementName(), maskedLimitTo, matchRule);
			break;
		case IJavaElement.JAVA_MODULE :
			searchPattern = createModulePattern(element.getElementName(), maskedLimitTo, matchRule);
			break;
	}
	if (searchPattern != null)
		MatchLocator.setFocus(searchPattern, element);
	return searchPattern;
}

private static SearchPattern createTypePattern(char[] simpleName, char[] packageName, char[][] enclosingTypeNames, String typeSignature, IType type, int limitTo, int matchRule) {
	switch (limitTo) {
		case IJavaSearchConstants.DECLARATIONS :
			return new TypeDeclarationPattern(
				packageName,
				enclosingTypeNames,
				simpleName,
				IIndexConstants.TYPE_SUFFIX,
				matchRule);
		case IJavaSearchConstants.REFERENCES :
			if (type != null) {
				return new TypeReferencePattern(
					CharOperation.concatWith(packageName, enclosingTypeNames, '.'),
					simpleName,
					type,
					matchRule);
			}
			return new TypeReferencePattern(
				CharOperation.concatWith(packageName, enclosingTypeNames, '.'),
				simpleName,
				typeSignature,
				matchRule);
		case IJavaSearchConstants.IMPLEMENTORS :
			return new SuperTypeReferencePattern(
				CharOperation.concatWith(packageName, enclosingTypeNames, '.'),
				simpleName,
				SuperTypeReferencePattern.ONLY_SUPER_INTERFACES,
				matchRule);
		case IJavaSearchConstants.ALL_OCCURRENCES :
			return new OrPattern(
				new TypeDeclarationPattern(
					packageName,
					enclosingTypeNames,
					simpleName,
					IIndexConstants.TYPE_SUFFIX,
					matchRule),
				(type != null)
					? new TypeReferencePattern(
						CharOperation.concatWith(packageName, enclosingTypeNames, '.'),
						simpleName,
						type,
						matchRule)
					: new TypeReferencePattern(
						CharOperation.concatWith(packageName, enclosingTypeNames, '.'),
						simpleName,
						typeSignature,
						matchRule)
			);
		default:
			if (type != null) {
				return new TypeReferencePattern(
					CharOperation.concatWith(packageName, enclosingTypeNames, '.'),
					simpleName,
					type,
					limitTo,
					matchRule);
			}
	}
	return null;
}
private static SearchPattern createTypePattern(String patternString, int limitTo, int matchRule, char indexSuffix) {
	String[] arr = patternString.split(String.valueOf(IIndexConstants.SEPARATOR));
	String moduleName = null;
	if (arr.length == 2) {
		moduleName = arr[0];
		patternString = arr[1];
	}
	char[] patModName = moduleName != null ? moduleName.toCharArray() : null;
	// use 1.7 as the source level as there are more valid tokens in 1.7 mode
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=376673
	Scanner scanner = new Scanner(false /*comment*/, true /*whitespace*/, false /*nls*/, ClassFileConstants.JDK1_7/*sourceLevel*/, null /*taskTags*/, null/*taskPriorities*/, true/*taskCaseSensitive*/);
	scanner.setSource(patternString.toCharArray());
	String type = null;
	int token;
	try {
		token = scanner.getNextToken();
	} catch (InvalidInputException e) {
		return null;
	}
	int argCount = 0;
	while (token != TerminalTokens.TokenNameEOF) {
		if (argCount == 0) {
			switch (token) {
				case TerminalTokens.TokenNameWHITESPACE:
					break;
				case TerminalTokens.TokenNameLESS:
					argCount++;
					// $FALL-THROUGH$ - fall through default case to add token to type
				default: // all other tokens are considered identifiers (see bug 21763 Problem in Java search [search])
					if (type == null)
						type = scanner.getCurrentTokenString();
					else
						type += scanner.getCurrentTokenString();
			}
		} else {
			switch (token) {
				case TerminalTokens.TokenNameGREATER:
				case TerminalTokens.TokenNameRIGHT_SHIFT:
				case TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT:
					argCount--;
					break;
				case TerminalTokens.TokenNameLESS:
					argCount++;
					break;
			}
			if (type == null) return null; // invalid syntax
			type += scanner.getCurrentTokenString();
		}
		try {
			token = scanner.getNextToken();
		} catch (InvalidInputException e) {
			return null;
		}
	}
	if (type == null) return null;
	String typeSignature = null;
	char[] qualificationChars = null, typeChars = null;

	// get type part and signature
	char[] typePart = null;
	try {
		typeSignature = Signature.createTypeSignature(type, false);
		if (typeSignature.indexOf(Signature.C_GENERIC_START) < 0) {
			typePart = type.toCharArray();
		} else {
			typePart = Signature.toCharArray(Signature.getTypeErasure(typeSignature.toCharArray()));
		}
	}
	catch (IllegalArgumentException iae) {
		// string is not a valid type syntax
		return null;
	}

	// get qualification name
	int lastDotPosition = CharOperation.lastIndexOf('.', typePart);
	if (lastDotPosition >= 0) {
		qualificationChars = CharOperation.subarray(typePart, 0, lastDotPosition);
		if (qualificationChars.length == 1 && qualificationChars[0] == '*')
			qualificationChars = null;
		typeChars = CharOperation.subarray(typePart, lastDotPosition+1, typePart.length);
	} else {
		typeChars = typePart;
	}
	if (typeChars.length == 1 && typeChars[0] == '*') {
		typeChars = null;
	}
	boolean modGraph = false;
	switch (limitTo) {
		case IJavaSearchConstants.MODULE_GRAPH :
			modGraph = true;
			//$FALL-THROUGH$
		case IJavaSearchConstants.DECLARATIONS : // cannot search for explicit member types
			TypeDeclarationPattern typeDeclarationPattern = new QualifiedTypeDeclarationPattern(patModName, qualificationChars, typeChars, indexSuffix, matchRule);
			typeDeclarationPattern.moduleGraph = modGraph;
			return typeDeclarationPattern;
		case IJavaSearchConstants.REFERENCES :
			return new TypeReferencePattern(qualificationChars, typeChars, typeSignature, indexSuffix, matchRule);
		case IJavaSearchConstants.IMPLEMENTORS :
			return new SuperTypeReferencePattern(qualificationChars, typeChars, SuperTypeReferencePattern.ONLY_SUPER_INTERFACES, indexSuffix, matchRule);
		case IJavaSearchConstants.ALL_OCCURRENCES :
			return new OrPattern(
				new QualifiedTypeDeclarationPattern(patModName, qualificationChars, typeChars, indexSuffix, matchRule),// cannot search for explicit member types
				new TypeReferencePattern(qualificationChars, typeChars, typeSignature, indexSuffix, matchRule));
		default:
			return new TypeReferencePattern(qualificationChars, typeChars, typeSignature, limitTo, indexSuffix, matchRule);
	}
}
/**
 * Returns the enclosing type names of the given type.
 */
private static char[][] enclosingTypeNames(IType type) {
	IJavaElement parent = type.getParent();
	switch (parent.getElementType()) {
		case IJavaElement.CLASS_FILE:
			if (parent instanceof IModularClassFile)
				return null;
			// For a binary type, the parent is not the enclosing type, but the declaring type is.
			// (see bug 20532  Declaration of member binary type not found)
			IType declaringType = type.getDeclaringType();
			if (declaringType == null) return CharOperation.NO_CHAR_CHAR;
			return CharOperation.arrayConcat(
				enclosingTypeNames(declaringType),
				declaringType.getElementName().toCharArray());
		case IJavaElement.COMPILATION_UNIT:
			return CharOperation.NO_CHAR_CHAR;
		case IJavaElement.FIELD:
		case IJavaElement.INITIALIZER:
		case IJavaElement.METHOD:
			IType declaringClass = ((IMember) parent).getDeclaringType();
			return CharOperation.arrayConcat(
				enclosingTypeNames(declaringClass),
				new char[][] {declaringClass.getElementName().toCharArray(), IIndexConstants.ONE_STAR});
		case IJavaElement.TYPE:
			return CharOperation.arrayConcat(
				enclosingTypeNames((IType)parent),
				parent.getElementName().toCharArray());
		default:
			return null;
	}
}

/**
 * Decode the given index key in this pattern. The decoded index key is used by
 * {@link #matchesDecodedKey(SearchPattern)} to find out if the corresponding index entry
 * should be considered.
 * <p>
 * This method should be re-implemented in subclasses that need to decode an index key.
 * </p>
 *
 * @param key the given index key
 */
public void decodeIndexKey(char[] key) {
	// called from findIndexMatches(), override as necessary
}
/**
 * Query a given index for matching entries. Assumes the sender has opened the index and will close when finished.
 *
 * @noreference This method is not intended to be referenced by clients.
 * @nooverride This method is not intended to be re-implemented or extended by clients.
 */
public void findIndexMatches(Index index, IndexQueryRequestor requestor, SearchParticipant participant, IJavaSearchScope scope, IProgressMonitor monitor) throws IOException {
	findIndexMatches(index, requestor, participant, scope, true, monitor);
}

/**
 * Query a given index for matching entries. Assumes the sender has
 * opened the index and will close when finished.
 *
 * This API provides a flag to control whether to skip resolving
 * document name for the matching entries. If a SearchPattern subclass
 * has a different implementation of index matching, they have to
 * override this API to support document name resolving feature.
 *
 * @param index the target index to query
 * @param requestor the search requestor
 * @param participant the search participant
 * @param scope the search scope where the search results should be found
 * @param resolveDocumentName whether to skip the document name resolving
 *                            for the matching entries
 * @param monitor a progress monitor
 *
 * @noreference This method is not intended to be referenced by clients.
 * @nooverride This method is not intended to be re-implemented or extended by clients.
 */
public void findIndexMatches(Index index, IndexQueryRequestor requestor, SearchParticipant participant, IJavaSearchScope scope, boolean resolveDocumentName, IProgressMonitor monitor) throws IOException {
	if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
	try {
		index.startQuery();
		SearchPattern pattern = currentPattern();
		EntryResult[] entries = pattern.queryIn(index);
		if (entries == null) return;

		String containerPath = index.containerPath;
		char separator = index.separator;
		for (int i = 0, l = entries.length; i < l; i++) {
			if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();

			EntryResult entry = entries[i];
			SearchPattern decodedResult = pattern.getBlankPattern();
			decodedResult.decodeIndexKey(entry.getWord());
			if (pattern.matchesDecodedKey(decodedResult)) {
				// Since resolve document name is expensive, leave the decision to the search client
				// to decide whether to do so.
				if (resolveDocumentName) {
					String[] names = entry.getDocumentNames(index);
					for (int j = 0, n = names.length; j < n; j++)
						acceptMatch(names[j], containerPath, separator, decodedResult, requestor, participant, scope, monitor);
				} else {
					acceptMatch("", containerPath, separator, decodedResult, requestor, participant, scope, monitor); //$NON-NLS-1$
				}
			}
		}
	} finally {
		index.stopQuery();
	}
}
/**
 * Returns a blank pattern that can be used as a record to decode an index key.
 * <p>
 * Implementors of this method should return a new search pattern that is going to be used
 * to decode index keys.
 * </p>
 *
 * @return a new blank pattern
 * @see #decodeIndexKey(char[])
 */
public abstract SearchPattern getBlankPattern();
/**
 * Returns a key to find in relevant index categories, if null then all index entries are matched.
 * The key will be matched according to some match rule. These potential matches
 * will be further narrowed by the match locator, but precise match locating can be expensive,
 * and index query should be as accurate as possible so as to eliminate obvious false hits.
 * <p>
 * This method should be re-implemented in subclasses that need to narrow down the
 * index query.
 * </p>
 *
 * @return an index key from this pattern, or <code>null</code> if all index entries are matched.
 */
public char[] getIndexKey() {
	return null; // called from queryIn(), override as necessary
}
/**
 * Returns an array of index categories to consider for this index query.
 * These potential matches will be further narrowed by the match locator, but precise
 * match locating can be expensive, and index query should be as accurate as possible
 * so as to eliminate obvious false hits.
 * <p>
 * This method should be re-implemented in subclasses that need to narrow down the
 * index query.
 * </p>
 *
 * @return an array of index categories
 */
public char[][] getIndexCategories() {
	return CharOperation.NO_CHAR_CHAR; // called from queryIn(), override as necessary
}
/**
 * Returns the rule to apply for matching index keys. Can be exact match, prefix match, pattern match or regexp match.
 * Rule can also be combined with a case sensitivity flag.
 *
 * @return one of R_EXACT_MATCH, R_PREFIX_MATCH, R_PATTERN_MATCH, R_REGEXP_MATCH combined with R_CASE_SENSITIVE,
 *   e.g. R_EXACT_MATCH | R_CASE_SENSITIVE if an exact and case sensitive match is requested,
 *   or R_PREFIX_MATCH if a prefix non case sensitive match is requested.
 */
public final int getMatchRule() {
	return this.matchRule;
}
/**
 * @noreference This method is not intended to be referenced by clients.
 * @nooverride This method is not intended to be re-implemented or extended by clients.
 */
public boolean isPolymorphicSearch() {
	return false;
}
/**
 * Returns whether this pattern matches the given pattern (representing a decoded index key).
 * <p>
 * This method should be re-implemented in subclasses that need to narrow down the
 * index query.
 * </p>
 *
 * @param decodedPattern a pattern representing a decoded index key
 * @return whether this pattern matches the given pattern
 */
public boolean matchesDecodedKey(SearchPattern decodedPattern) {
	return true; // called from findIndexMatches(), override as necessary if index key is encoded
}

/**
 * Returns whether the given name matches the given pattern.
 * <p>
 * This method should be re-implemented in subclasses that need to define how
 * a name matches a pattern.
 * </p>
 *
 * @param pattern the given pattern, or <code>null</code> to represent "*"
 * @param name the given name
 * @return whether the given name matches the given pattern
 */
public boolean matchesName(char[] pattern, char[] name) {
	if (pattern == null) return true; // null is as if it was "*"
	if (name != null) {
		boolean isCaseSensitive = (this.matchRule & R_CASE_SENSITIVE) != 0;
		int matchMode = this.matchRule & MODE_MASK;
		boolean emptyPattern = pattern.length == 0;
		if (emptyPattern && (this.matchRule & R_PREFIX_MATCH) != 0) return true;
		boolean sameLength = pattern.length == name.length;
		boolean canBePrefix = name.length >= pattern.length;
		boolean matchFirstChar = !isCaseSensitive || emptyPattern || (name.length > 0 &&  pattern[0] == name[0]);

		if ((matchMode & R_SUBSTRING_MATCH) != 0) {
			if (CharOperation.substringMatch(pattern, name))
				return true;
			matchMode &= ~R_SUBSTRING_MATCH;
		}
		if ((matchMode & SearchPattern.R_SUBWORD_MATCH) != 0) {
			if (CharOperation.subWordMatch(pattern, name))
				return true;
			matchMode &= ~SearchPattern.R_SUBWORD_MATCH;
		}

		switch (matchMode) {
			case R_EXACT_MATCH :
				if (sameLength && matchFirstChar) {
					return CharOperation.equals(pattern, name, isCaseSensitive);
				}
				break;

			case R_PREFIX_MATCH :
				if (canBePrefix && matchFirstChar) {
					return CharOperation.prefixEquals(pattern, name, isCaseSensitive);
				}
				break;

			case R_PATTERN_MATCH :
				if (!isCaseSensitive)
					pattern = CharOperation.toLowerCase(pattern);
				return CharOperation.match(pattern, name, isCaseSensitive);

			case SearchPattern.R_CAMELCASE_MATCH:
				if (matchFirstChar && CharOperation.camelCaseMatch(pattern, name, false)) {
					return true;
				}
				// only test case insensitive as CamelCase already verified prefix case sensitive
				if (!isCaseSensitive && matchFirstChar && CharOperation.prefixEquals(pattern, name, false)) {
					return true;
				}
				break;

			case SearchPattern.R_CAMELCASE_SAME_PART_COUNT_MATCH:
				return matchFirstChar && CharOperation.camelCaseMatch(pattern, name, true);

			case R_REGEXP_MATCH :
				return Pattern.matches(new String(pattern), new String(name));
		}
	}
	return false;
}

/**
 * Validate compatibility between given string pattern and match rule.
 *<br>
 * In certain circumstances described in the table below, the returned match rule is
 * modified in order to provide a more efficient search pattern:
 * <ol>
 * 	<li>when the {@link #R_REGEXP_MATCH} flag is set, then <b>the pattern is
 * 		rejected</b> as this kind of match is not supported yet and <code>-1</code>
 * 		is returned).
 * 	</li>
 * 	<li>when the string pattern has <u>no</u> pattern characters (e.g. '*' or '?')
 * 		and the pattern match flag is set (i.e. the match rule has the {@link #R_PATTERN_MATCH}
 * 		flag), then <b>the pattern match flag is reset</b>.<br>
 * 		Reversely, when the string pattern has pattern characters and the pattern
 * 		match flag is <u>not</u> set, then <b>the pattern match flag is set</b>.
 * 	</li>
 * 	<li>when the {@link #R_PATTERN_MATCH} flag is set then, <b>other
 * 		{@link #R_PREFIX_MATCH}, {@link #R_CAMELCASE_MATCH} or
 * 		{@link #R_CAMELCASE_SAME_PART_COUNT_MATCH} flags are reset</b>
 * 		if they are tentatively combined.
 * 	</li>
 * 	<li>when the {@link #R_CAMELCASE_MATCH} flag is set, then <b>other
 * 		{@link #R_PREFIX_MATCH} or {@link #R_CAMELCASE_SAME_PART_COUNT_MATCH}
 * 		flags are reset</b> if they are tentatively combined.<br>
 * 		Reversely, if the string pattern cannot be a camel case pattern (i.e. contains
 * 		invalid Java identifier characters or does not have at least two uppercase
 * 		characters - one for method camel case patterns), then <b>the CamelCase
 * 		match flag is replaced with a prefix match flag</b>.
 * 	</li>
 * 	<li>when the {@link #R_CAMELCASE_SAME_PART_COUNT_MATCH} flag is set,
 * 		then <b>({@link #R_PREFIX_MATCH} flag is reset</b> if it's tentatively
 * 		combined.<br>
 * 		Reversely, if the string pattern cannot be a camel case pattern (i.e. contains
 * 		invalid Java identifier characters or does not have at least two uppercase
 * 		characters - one for method camel case patterns), then <b>the CamelCase
 * 		part count match flag is reset</b>.
 * 	</li>
 * </ol>
 * <i>Note: the rules are validated in the documented order. For example, it means
 * 	that as soon as the string pattern contains one pattern character, the pattern
 * 	match flag will be set and all other match flags reset: validation of rule 2)
 * 	followed by rule 3)...</i>
 *<p>
 *
 * @param stringPattern The string pattern
 * @param matchRule The match rule
 * @return Optimized valid match rule or -1 if an incompatibility was detected.
 * @since 3.2
 */
public static int validateMatchRule(String stringPattern, int matchRule) {

	// Verify Regexp match rule
	if ((matchRule & R_REGEXP_MATCH) != 0) {
		// regexp is not supported yet
		return -1; // need to enable for module declaration
	}

	// Verify Pattern match rule
	if (stringPattern != null) {
		int starIndex = stringPattern.indexOf('*');
		int questionIndex = stringPattern.indexOf('?');
		if (starIndex < 0 && questionIndex < 0) {
			// reset pattern match flag if any
			matchRule &= ~R_PATTERN_MATCH;
		} else {
			// force Pattern rule
			matchRule |= R_PATTERN_MATCH;
		}
	}
	if ((matchRule & R_PATTERN_MATCH) != 0) {
		// reset other incompatible flags
		matchRule &= ~R_CAMELCASE_MATCH;
		matchRule &= ~R_CAMELCASE_SAME_PART_COUNT_MATCH;
		matchRule &= ~R_PREFIX_MATCH;
		return matchRule;
	}

	// Verify Camel Case
	if ((matchRule & R_CAMELCASE_MATCH) != 0) {
		// reset other incompatible flags
		matchRule &= ~R_CAMELCASE_SAME_PART_COUNT_MATCH;
		matchRule &= ~R_PREFIX_MATCH;
		// validate camel case rule and modify it if not valid
		boolean validCamelCase = validateCamelCasePattern(stringPattern);
		if (!validCamelCase) {
			matchRule &= ~R_CAMELCASE_MATCH;
			matchRule |= R_PREFIX_MATCH;
		}
		return matchRule;
	}

	// Verify Camel Case with same count of parts
	if ((matchRule & R_CAMELCASE_SAME_PART_COUNT_MATCH) != 0) {
		// reset other incompatible flags
		matchRule &= ~R_PREFIX_MATCH;
		// validate camel case rule and modify it if not valid
		boolean validCamelCase = validateCamelCasePattern(stringPattern);
		if (!validCamelCase) {
			matchRule &= ~R_CAMELCASE_SAME_PART_COUNT_MATCH;
		}
		return matchRule;
	}

	// Return the validated match rule (modified if necessary)
	return matchRule;
}

// enabling special cases (read regular expressions) based on searchFor and limitTo
private static int validateMatchRule(String stringPattern, int searchFor, int limitTo, int matchRule) {
	if (searchFor == IJavaSearchConstants.MODULE &&
			limitTo == IJavaSearchConstants.DECLARATIONS &&
			matchRule == SearchPattern.R_REGEXP_MATCH)
		return matchRule;
	return validateMatchRule(stringPattern, matchRule);
}

/*
 * Validate pattern for a camel case match rule
 * @return
 */
private static boolean validateCamelCasePattern(String stringPattern) {
	if (stringPattern == null) return true;
	// verify sting pattern validity
	int length = stringPattern.length();
	boolean validCamelCase = true;
	boolean lowerCamelCase = false;
	int uppercase = 0;
	for (int i=0; i<length && validCamelCase; i++) {
		char ch = stringPattern.charAt(i);
		validCamelCase = i==0 ? ScannerHelper.isJavaIdentifierStart(ch) : ScannerHelper.isJavaIdentifierPart(ch);
		// at least one uppercase character is need in CamelCase pattern
		// (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=136313)
		if (ScannerHelper.isUpperCase(ch)) uppercase++;
		if (i==0) lowerCamelCase = uppercase == 0;
	}
	if (validCamelCase) {
		validCamelCase = lowerCamelCase ? uppercase > 0 : uppercase > 1 ;
	}
	return validCamelCase;
}

/**
 * @noreference This method is not intended to be referenced by clients.
 * @nooverride This method is not intended to be re-implemented or extended by clients.
 */
public EntryResult[] queryIn(Index index) throws IOException {
	return index.query(getIndexCategories(), getIndexKey(), getMatchRule());
}

/**
 * @see java.lang.Object#toString()
 */
@Override
public String toString() {
	return "SearchPattern"; //$NON-NLS-1$
}

/**
 * @since 3.25
 */
@Override
public SearchPattern clone() throws CloneNotSupportedException {
	return (SearchPattern) super.clone();
}
}
