/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.core.search;

import org.eclipse.jdt.internal.core.search.processing.*;

/**
 * <p>
 * This interface defines the constants used by the search engine.
 * </p>
 * <p>
 * This interface declares constants only.
 * </p>
 * @see org.eclipse.jdt.core.search.SearchEngine
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IJavaSearchConstants {

	/**
	 * The nature of searched element or the nature
	 * of match in unknown.
	 */
	int UNKNOWN = -1;

	/* Nature of searched element */

	/**
	 * The searched element is a type, which may include classes, interfaces,
	 * enums, and annotation types.
	 *
	 * @category searchFor
	 */
	int TYPE= 0;

	/**
	 * The searched element is a method.
	 *
	 * @category searchFor
	 */
	int METHOD= 1;

	/**
	 * The searched element is a package.
	 *
	 * @category searchFor
	 */
	int PACKAGE= 2;

	/**
	 * The searched element is a constructor.
	 *
	 * @category searchFor
	 */
	int CONSTRUCTOR= 3;

	/**
	 * The searched element is a field.
	 *
	 * @category searchFor
	 */
	int FIELD= 4;

	/**
	 * The searched element is a class.
	 * More selective than using {@link #TYPE}.
	 *
	 * @category searchFor
	 */
	int CLASS= 5;

	/**
	 * The searched element is an interface.
	 * More selective than using {@link #TYPE}.
	 *
	 * @category searchFor
	 */
	int INTERFACE= 6;

	/**
	 * The searched element is an enum.
	 * More selective than using {@link #TYPE}.
	 *
	 * @since 3.1
	 * @category searchFor
	 */
	int ENUM= 7;

	/**
	 * The searched element is an annotation type.
	 * More selective than using {@link #TYPE}.
	 *
	 * @since 3.1
	 * @category searchFor
	 */
	int ANNOTATION_TYPE= 8;

	/**
	 * The searched element is a class or enum type.
	 * More selective than using {@link #TYPE}.
	 *
	 * @since 3.1
	 * @category searchFor
	 */
	int CLASS_AND_ENUM= 9;

	/**
	 * The searched element is a class or interface type.
	 * More selective than using {@link #TYPE}.
	 *
	 * @since 3.1
	 * @category searchFor
	 */
	int CLASS_AND_INTERFACE= 10;

	/**
	 * The searched element is an interface or annotation type.
	 * More selective than using {@link #TYPE}.
	 * @since 3.3
	 * @category searchFor
	 */
	int INTERFACE_AND_ANNOTATION= 11;

	/**
	 * The searched element is a module.
	 * @since 3.14
	 * @category searchFor
	 */
	int MODULE= 12;
	/* Nature of match */

	/**
	 * The search result is a declaration.
	 * Can be used in conjunction with any of the nature of searched elements
	 * so as to better narrow down the search.
	 *
	 * @category limitTo
	 */
	int DECLARATIONS= 0;

	/**
	 * The search result is a type that implements an interface or extends a class.
	 * Used in conjunction with either TYPE or CLASS or INTERFACE, it will
	 * respectively search for any type implementing/extending a type,
	 * or rather exclusively search for classes implementing/extending the type, or
	 * interfaces extending the type.
	 *
	 * @category limitTo
	 */
	int IMPLEMENTORS= 1;

	/**
	 * The search result is a reference.
	 * Can be used in conjunction with any of the nature of searched elements
	 * so as to better narrow down the search.
	 * References can contain implementers since they are more generic kind
	 * of matches.
	 *
	 * @category limitTo
	 */
	int REFERENCES= 2;

	/**
	 * The search result is a declaration, a reference, or an implementer
	 * of an interface.
	 * Can be used in conjunction with any of the nature of searched elements
	 * so as to better narrow down the search.
	 *
	 * @category limitTo
	 */
	int ALL_OCCURRENCES= 3;

	/**
	 * When searching for field matches, it will exclusively find read accesses, as
	 * opposed to write accesses. Note that some expressions are considered both
	 * as field read/write accesses: for example, x++; x+= 1;
	 *
	 * @since 2.0
	 * @category limitTo
	 */
	int READ_ACCESSES = 4;

	/**
	 * When searching for field matches, it will exclusively find write accesses, as
	 * opposed to read accesses. Note that some expressions are considered both
	 * as field read/write accesses: for example,  x++; x+= 1;
	 *
	 * @since 2.0
	 * @category limitTo
	 */
	int WRITE_ACCESSES = 5;

	/**
	 * When searching for Type Declaration matches, and if a module is given, this
	 * will find type declaration matches in this module as well as the dependent
	 * module graph of the given module.
	 *
	 * @since 3.14
	 * @category limitTo
	 */
	int MODULE_GRAPH = 6;

	/**
	 * Ignore declaring type while searching result.
	 * Can be used in conjunction with any of the nature of match.
	 *
	 * @since 3.1
	 * @category limitTo
	 */
	int IGNORE_DECLARING_TYPE = 0x10;

	/**
	 * Ignore return type while searching result.
	 * Can be used in conjunction with any other nature of match.
	 * Note that:
	 * <ul>
	 * 	<li>for fields search, pattern will ignore field type</li>
	 * 	<li>this flag will have no effect for types search</li>
	 *	</ul>
	 *
	 * @since 3.1
	 * @category limitTo
	 */
	int IGNORE_RETURN_TYPE = 0x20;

	/**
	 * Return only type references used as the type of a field declaration.
	 * <p>
	 * When this flag is set, only {@link TypeReferenceMatch} matches will be
	 * returned.
	 *</p>
	 * @since 3.4
	 * @category limitTo
	 */
	int FIELD_DECLARATION_TYPE_REFERENCE = 0x40;

	/**
	 * Return only type references used as the type of a local variable declaration.
	 * <p>
	 * When this flag is set, only {@link TypeReferenceMatch} matches will be
	 * returned.
	 *</p>
	 * @since 3.4
	 * @category limitTo
	 */
	int LOCAL_VARIABLE_DECLARATION_TYPE_REFERENCE = 0x80;

	/**
	 * Return only type references used as the type of a method parameter
	 * declaration.
	 * <p>
	 * When this flag is set, only {@link TypeReferenceMatch} matches will be
	 * returned.
	 *</p>
	 * @since 3.4
	 * @category limitTo
	 */
	int PARAMETER_DECLARATION_TYPE_REFERENCE = 0x100;

	/**
	 * Return only type references used as a super type or as a super interface.
	 * <p>
	 * When this flag is set, only {@link TypeReferenceMatch} matches will be
	 * returned.
	 *</p>
	 * @since 3.4
	 * @category limitTo
	 */
	int SUPERTYPE_TYPE_REFERENCE = 0x200;

	/**
	 * Return only type references used in a throws clause.
	 * <p>
	 * When this flag is set, only {@link TypeReferenceMatch} matches will be
	 * returned.
	 *</p>
	 * @since 3.4
	 * @category limitTo
	 */
	int THROWS_CLAUSE_TYPE_REFERENCE = 0x400;

	/**
	 * Return only type references used in a cast expression.
	 * <p>
	 * When this flag is set, only {@link TypeReferenceMatch} matches will be
	 * returned.
	 *</p>
	 * @since 3.4
	 * @category limitTo
	 */
	int CAST_TYPE_REFERENCE = 0x800;

	/**
	 * Return only type references used in a catch header.
	 * <p>
	 * When this flag is set, only {@link TypeReferenceMatch} matches will be
	 * returned.
	 *</p>
	 * @since 3.4
	 * @category limitTo
	 */
	int CATCH_TYPE_REFERENCE = 0x1000;

	/**
	 * Return only type references used in class instance creation.
	 * <p>
	 * When this flag is set, only {@link TypeReferenceMatch} matches will be
	 * returned.
	 *</p><p>
	 *	Example:
	 *<pre>
	 * public class Test {
	 * 	Test() {}
	 * 	static Test bar()  {
	 * 		return new <i>Test</i>();
	 * 	}
	 * }
	 *</pre>
	 * Searching references to the type <code>Test</code> using this flag in the
	 * above snippet will match only the reference in italic.
	 * <p>
	 * Note that array creations are not returned when using this flag.
	 * </p>
	 * @since 3.4
	 * @category limitTo
	 */
	int CLASS_INSTANCE_CREATION_TYPE_REFERENCE = 0x2000;

	/**
	 * Return only type references used as a method return type.
	 * <p>
	 * When this flag is set, only {@link TypeReferenceMatch} matches will be
	 * returned.
	 *</p>
	 * @since 3.4
	 * @category limitTo
	 */
	int RETURN_TYPE_REFERENCE = 0x4000;

	/**
	 * Return only type references used in an import declaration.
	 * <p>
	 * When this flag is set, only {@link TypeReferenceMatch} matches will be
	 * returned.
	 *</p>
	 * @since 3.4
	 * @category limitTo
	 */
	int IMPORT_DECLARATION_TYPE_REFERENCE = 0x8000;

	/**
	 * Return only type references used as an annotation.
	 * <p>
	 * When this flag is set, only {@link TypeReferenceMatch} matches will be
	 * returned.
	 *</p>
	 * @since 3.4
	 * @category limitTo
	 */
	int ANNOTATION_TYPE_REFERENCE = 0x10000;

	/**
	 * Return only type references used as a type argument in a parameterized
	 * type or a parameterized method.
	 * <p>
	 * When this flag is set, only {@link TypeReferenceMatch} matches will be
	 * returned.
	 *</p>
	 * @since 3.4
	 * @category limitTo
	 */
	int TYPE_ARGUMENT_TYPE_REFERENCE = 0x20000;

	/**
	 * Return only type references used as a type variable bound.
	 * <p>
	 * When this flag is set, only {@link TypeReferenceMatch} matches will be
	 * returned.
	 *</p>
	 * @since 3.4
	 * @category limitTo
	 */
	int TYPE_VARIABLE_BOUND_TYPE_REFERENCE = 0x40000;

	/**
	 * Return only type references used as a wildcard bound.
	 * <p>
	 * When this flag is set, only {@link TypeReferenceMatch} matches will be
	 * returned.
	 *</p>
	 * @since 3.4
	 * @category limitTo
	 */
	int WILDCARD_BOUND_TYPE_REFERENCE = 0x80000;

	/**
	 * Return only type references used as a type of an <code>instanceof</code>
	 * expression.
	 * <p>
	 * When this flag is set, only {@link TypeReferenceMatch} matches will be
	 * returned.
	 *</p>
	 * @since 3.4
	 * @category limitTo
	 */
	int INSTANCEOF_TYPE_REFERENCE = 0x100000;

	/**
	 * Return only super field accesses or super method invocations (e.g. using the
	 * <code>super</code> qualifier).
	 * <p>
	 * When this flag is set, the kind of returned matches will depend on the
	 * specified nature of the searched element:
	 * <ul>
	 * 	<li>for the {@link #FIELD} nature, only {@link FieldReferenceMatch}
	 * 		matches will be returned,</li>
	 * 	<li>for the {@link #METHOD} nature, only {@link MethodReferenceMatch}
	 * 		matches will be returned.</li>
	 * </ul>
	 * @since 3.4
	 * @category limitTo
	 */
	int SUPER_REFERENCE = 0x1000000;

	/**
	 * Return only qualified field accesses or qualified method invocations.
	 * <p>
	 * When this flag is set, the kind of returned matches will depend on the
	 * specified nature of the searched element:
	 * <ul>
	 * 	<li>for the {@link #FIELD} nature, only {@link FieldReferenceMatch}
	 * 		matches will be returned,</li>
	 * 	<li>for the {@link #METHOD} nature, only {@link MethodReferenceMatch}
	 * 		matches will be returned.</li>
	 * </ul>
	 * @since 3.4
	 * @category limitTo
	 */
	int QUALIFIED_REFERENCE = 0x2000000;

	/**
	 * Return only primary field accesses or primary method invocations (e.g. using
	 * the <code>this</code> qualifier).
	 * <p>
	 * When this flag is set, the kind of returned matches will depend on the
	 * specified nature of the searched element:
	 * <ul>
	 * 	<li>for the {@link #FIELD} nature, only {@link FieldReferenceMatch}
	 * 		matches will be returned,</li>
	 * 	<li>for the {@link #METHOD} nature, only {@link MethodReferenceMatch}
	 * 		matches will be returned.</li>
	 * </ul>
	 * @since 3.4
	 * @category limitTo
	 */
	int THIS_REFERENCE = 0x4000000;

	/**
	 * Return only field accesses or method invocations without any qualification.
	 * <p>
	 * When this flag is set, the kind of returned matches will depend on the
	 * specified nature of the searched element:
	 * <ul>
	 * 	<li>for the {@link #FIELD} nature, only {@link FieldReferenceMatch}
	 * 		matches will be returned,</li>
	 * 	<li>for the {@link #METHOD} nature, only {@link MethodReferenceMatch}
	 * 		matches will be returned.</li>
	 * </ul>
	 * @since 3.4
	 * @category limitTo
	 */
	int IMPLICIT_THIS_REFERENCE = 0x8000000;

	/**
	 * Return only method reference expressions, e.g. <code>A::foo</code>.
	 * <p>
	 * When this flag is set, only {@link MethodReferenceMatch} matches will be
	 * returned.
	 *</p>
	 * @since 3.10
	 * @category limitTo
	 */
	int METHOD_REFERENCE_EXPRESSION = 0x10000000;

	/**
	 * Return only type references used as a permit type (Java 15)
	 * <p>
	 * When this flag is set, only {@link TypeReferenceMatch} matches will be
	 * returned.
	 *</p>
	 * @since 3.24
	 * @category limitTo
	 */
	int PERMITTYPE_TYPE_REFERENCE = 0x20000000;

	/* Syntactic match modes */

	/**
	 * The search pattern matches exactly the search result,
	 * that is, the source of the search result equals the search pattern.
	 *
	 * @deprecated Use {@link SearchPattern#R_EXACT_MATCH} instead.
	 * @category matchRule
	 */
	int EXACT_MATCH = 0;
	/**
	 * The search pattern is a prefix of the search result.
	 *
	 * @deprecated Use {@link SearchPattern#R_PREFIX_MATCH} instead.
	 * @category matchRule
	 */
	int PREFIX_MATCH = 1;
	/**
	 * The search pattern contains one or more wild cards ('*') where a
	 * wild-card can replace 0 or more characters in the search result.
	 *
	 * @deprecated Use {@link SearchPattern#R_PATTERN_MATCH} instead.
	 * @category matchRule
	 */
	int PATTERN_MATCH = 2;


	/* Case sensitivity */

	/**
	 * The search pattern matches the search result only
	 * if cases are the same.
	 *
	 * @deprecated Use the methods that take the matchMode
	 *   with {@link SearchPattern#R_CASE_SENSITIVE} as a matchRule instead.
	 * @category matchRule
	 */
	boolean CASE_SENSITIVE = true;
	/**
	 * The search pattern ignores cases in the search result.
	 *
	 * @deprecated Use the methods that take the matchMode
	 *   without {@link SearchPattern#R_CASE_SENSITIVE} as a matchRule instead.
	 * @category matchRule
	 */
	boolean CASE_INSENSITIVE = false;


	/* Waiting policies */

	/**
	 * The search operation starts immediately, even if the underlying indexer
	 * has not finished indexing the workspace. Results will more likely
	 * not contain all the matches.
	 */
	int FORCE_IMMEDIATE_SEARCH = IJob.ForceImmediate;
	/**
	 * The search operation throws an <code>org.eclipse.core.runtime.OperationCanceledException</code>
	 * if the underlying indexer has not finished indexing the workspace.
	 */
	int CANCEL_IF_NOT_READY_TO_SEARCH = IJob.CancelIfNotReady;
	/**
	 * The search operation waits for the underlying indexer to finish indexing
	 * the workspace before starting the search.
	 */
	int WAIT_UNTIL_READY_TO_SEARCH = IJob.WaitUntilReady;

	/* Special Constant for module search */

	/**
	 * The unnamed module is represented by this constant for making the intent explicit
	 * in searches involving modules
	 * @since 3.14
	 */
	char[] ALL_UNNAMED = "ALL-UNNAMED".toCharArray(); ////$NON-NLS-1$

}
