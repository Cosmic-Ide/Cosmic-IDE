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
 *     Gábor Kövesdán - Contribution for Bug 350000 - [content assist] Include non-prefix matches in auto-complete suggestions
 *******************************************************************************/
package org.eclipse.jdt.internal.codeassist.impl;

import java.util.Map;

import org.eclipse.jdt.core.compiler.CharOperation;

@SuppressWarnings("rawtypes")
public class AssistOptions {
	/**
	 * Option IDs
	 */
	public static final String OPTION_PerformVisibilityCheck =
		"org.eclipse.jdt.core.codeComplete.visibilityCheck"; 	//$NON-NLS-1$
	public static final String OPTION_PerformDeprecationCheck =
		"org.eclipse.jdt.core.codeComplete.deprecationCheck"; 	//$NON-NLS-1$
	public static final String OPTION_ForceImplicitQualification =
		"org.eclipse.jdt.core.codeComplete.forceImplicitQualification"; 	//$NON-NLS-1$
	public static final String OPTION_FieldPrefixes =
		"org.eclipse.jdt.core.codeComplete.fieldPrefixes"; 	//$NON-NLS-1$
	public static final String OPTION_StaticFieldPrefixes =
		"org.eclipse.jdt.core.codeComplete.staticFieldPrefixes"; 	//$NON-NLS-1$
	public static final String OPTION_StaticFinalFieldPrefixes =
		"org.eclipse.jdt.core.codeComplete.staticFinalFieldPrefixes"; 	//$NON-NLS-1$
	public static final String OPTION_LocalPrefixes =
		"org.eclipse.jdt.core.codeComplete.localPrefixes"; 	//$NON-NLS-1$
	public static final String OPTION_ArgumentPrefixes =
		"org.eclipse.jdt.core.codeComplete.argumentPrefixes"; 	//$NON-NLS-1$
	public static final String OPTION_FieldSuffixes =
		"org.eclipse.jdt.core.codeComplete.fieldSuffixes"; 	//$NON-NLS-1$
	public static final String OPTION_StaticFieldSuffixes =
		"org.eclipse.jdt.core.codeComplete.staticFieldSuffixes"; 	//$NON-NLS-1$
	public static final String OPTION_StaticFinalFieldSuffixes =
		"org.eclipse.jdt.core.codeComplete.staticFinalFieldSuffixes"; 	//$NON-NLS-1$
	public static final String OPTION_LocalSuffixes =
		"org.eclipse.jdt.core.codeComplete.localSuffixes"; 	//$NON-NLS-1$
	public static final String OPTION_ArgumentSuffixes =
		"org.eclipse.jdt.core.codeComplete.argumentSuffixes"; 	//$NON-NLS-1$
	public static final String OPTION_PerformForbiddenReferenceCheck =
		"org.eclipse.jdt.core.codeComplete.forbiddenReferenceCheck"; 	//$NON-NLS-1$
	public static final String OPTION_PerformDiscouragedReferenceCheck =
		"org.eclipse.jdt.core.codeComplete.discouragedReferenceCheck"; 	//$NON-NLS-1$
	public static final String OPTION_CamelCaseMatch =
		"org.eclipse.jdt.core.codeComplete.camelCaseMatch"; 	//$NON-NLS-1$
	public static final String OPTION_SubwordMatch =
			"org.eclipse.jdt.core.codeComplete.subwordMatch"; 	//$NON-NLS-1$
	public static final String OPTION_SuggestStaticImports =
		"org.eclipse.jdt.core.codeComplete.suggestStaticImports"; 	//$NON-NLS-1$

	public static final String PROPERTY_SubstringMatch = "jdt.codeCompleteSubstringMatch"; //$NON-NLS-1$

	public static final String ENABLED = "enabled"; //$NON-NLS-1$
	public static final String DISABLED = "disabled"; //$NON-NLS-1$

	public boolean checkVisibility = false;
	public boolean checkDeprecation = false;
	public boolean checkForbiddenReference = false;
	public boolean checkDiscouragedReference = false;
	public boolean forceImplicitQualification = false;
	public boolean camelCaseMatch = true;
	public boolean substringMatch = true;
	public boolean subwordMatch = true;
	public boolean suggestStaticImport = true;
	public char[][] fieldPrefixes = null;
	public char[][] staticFieldPrefixes = null;
	public char[][] staticFinalFieldPrefixes = null;
	public char[][] localPrefixes = null;
	public char[][] argumentPrefixes = null;
	public char[][] fieldSuffixes = null;
	public char[][] staticFieldSuffixes = null;
	public char[][] staticFinalFieldSuffixes = null;
	public char[][] localSuffixes = null;
	public char[][] argumentSuffixes = null;

	/**
	 * Initializing the assist options with default settings
	 */
	public AssistOptions() {
		// Initializing the assist options with default settings
	}

	/**
	 * Initializing the assist options with external settings
	 */
	public AssistOptions(Map settings) {
		if (settings == null)
			return;

		set(settings);
	}
	public void set(Map optionsMap) {

		Object optionValue;
		if ((optionValue = optionsMap.get(OPTION_PerformVisibilityCheck)) != null) {
			if (ENABLED.equals(optionValue)) {
				this.checkVisibility = true;
			} else if (DISABLED.equals(optionValue)) {
				this.checkVisibility = false;
			}
		}
		if ((optionValue = optionsMap.get(OPTION_ForceImplicitQualification)) != null) {
			if (ENABLED.equals(optionValue)) {
				this.forceImplicitQualification = true;
			} else if (DISABLED.equals(optionValue)) {
				this.forceImplicitQualification = false;
			}
		}
		if ((optionValue = optionsMap.get(OPTION_FieldPrefixes)) != null) {
			if (optionValue instanceof String) {
				String stringValue = (String) optionValue;
				if (stringValue.length() > 0){
					this.fieldPrefixes = splitAndTrimOn(',', stringValue.toCharArray());
				} else {
					this.fieldPrefixes = null;
				}
			}
		}
		if ((optionValue = optionsMap.get(OPTION_StaticFieldPrefixes)) != null) {
			if (optionValue instanceof String) {
				String stringValue = (String) optionValue;
				if (stringValue.length() > 0){
					this.staticFieldPrefixes = splitAndTrimOn(',', stringValue.toCharArray());
				} else {
					this.staticFieldPrefixes = null;
				}
			}
		}
		if ((optionValue = optionsMap.get(OPTION_StaticFinalFieldPrefixes)) != null) {
			if (optionValue instanceof String) {
				String stringValue = (String) optionValue;
				if (stringValue.length() > 0){
					this.staticFinalFieldPrefixes = splitAndTrimOn(',', stringValue.toCharArray());
				} else {
					this.staticFinalFieldPrefixes = null;
				}
			}
		}
		if ((optionValue = optionsMap.get(OPTION_LocalPrefixes)) != null) {
			if (optionValue instanceof String) {
				String stringValue = (String) optionValue;
				if (stringValue.length() > 0){
					this.localPrefixes = splitAndTrimOn(',', stringValue.toCharArray());
				} else {
					this.localPrefixes = null;
				}
			}
		}
		if ((optionValue = optionsMap.get(OPTION_ArgumentPrefixes)) != null) {
			if (optionValue instanceof String) {
				String stringValue = (String) optionValue;
				if (stringValue.length() > 0){
					this.argumentPrefixes = splitAndTrimOn(',', stringValue.toCharArray());
				} else {
					this.argumentPrefixes = null;
				}
			}
		}
		if ((optionValue = optionsMap.get(OPTION_FieldSuffixes)) != null) {
			if (optionValue instanceof String) {
				String stringValue = (String) optionValue;
				if (stringValue.length() > 0){
					this.fieldSuffixes = splitAndTrimOn(',', stringValue.toCharArray());
				} else {
					this.fieldSuffixes = null;
				}
			}
		}
		if ((optionValue = optionsMap.get(OPTION_StaticFieldSuffixes)) != null) {
			if (optionValue instanceof String) {
				String stringValue = (String) optionValue;
				if (stringValue.length() > 0){
					this.staticFieldSuffixes = splitAndTrimOn(',', stringValue.toCharArray());
				} else {
					this.staticFieldSuffixes = null;
				}
			}
		}
		if ((optionValue = optionsMap.get(OPTION_StaticFinalFieldSuffixes)) != null) {
			if (optionValue instanceof String) {
				String stringValue = (String) optionValue;
				if (stringValue.length() > 0){
					this.staticFinalFieldSuffixes = splitAndTrimOn(',', stringValue.toCharArray());
				} else {
					this.staticFinalFieldSuffixes = null;
				}
			}
		}
		if ((optionValue = optionsMap.get(OPTION_LocalSuffixes)) != null) {
			if (optionValue instanceof String) {
				String stringValue = (String) optionValue;
				if (stringValue.length() > 0){
					this.localSuffixes = splitAndTrimOn(',', stringValue.toCharArray());
				} else {
					this.localSuffixes = null;
				}
			}
		}
		if ((optionValue = optionsMap.get(OPTION_ArgumentSuffixes)) != null) {
			if (optionValue instanceof String) {
				String stringValue = (String) optionValue;
				if (stringValue.length() > 0){
					this.argumentSuffixes = splitAndTrimOn(',', stringValue.toCharArray());
				} else {
					this.argumentSuffixes = null;
				}
			}
		}
		if ((optionValue = optionsMap.get(OPTION_PerformForbiddenReferenceCheck)) != null) {
			if (ENABLED.equals(optionValue)) {
				this.checkForbiddenReference = true;
			} else if (DISABLED.equals(optionValue)) {
				this.checkForbiddenReference = false;
			}
		}
		if ((optionValue = optionsMap.get(OPTION_PerformDiscouragedReferenceCheck)) != null) {
			if (ENABLED.equals(optionValue)) {
				this.checkDiscouragedReference = true;
			} else if (DISABLED.equals(optionValue)) {
				this.checkDiscouragedReference = false;
			}
		}
		if ((optionValue = optionsMap.get(OPTION_CamelCaseMatch)) != null) {
			if (ENABLED.equals(optionValue)) {
				this.camelCaseMatch = true;
			} else if (DISABLED.equals(optionValue)) {
				this.camelCaseMatch = false;
			}
		}
		if ("false".equals(System.getProperty(PROPERTY_SubstringMatch))) {  //$NON-NLS-1$
			this.substringMatch = false;
		}
		if ((optionValue = optionsMap.get(OPTION_SubwordMatch)) != null) {
			if (ENABLED.equals(optionValue)) {
				this.subwordMatch = true;
			} else if (DISABLED.equals(optionValue)) {
				this.subwordMatch = false;
			}
		}
		if ((optionValue = optionsMap.get(OPTION_PerformDeprecationCheck)) != null) {
			if (ENABLED.equals(optionValue)) {
				this.checkDeprecation = true;
			} else if (DISABLED.equals(optionValue)) {
				this.checkDeprecation = false;
			}
		}
		if ((optionValue = optionsMap.get(OPTION_SuggestStaticImports)) != null) {
			if (ENABLED.equals(optionValue)) {
				this.suggestStaticImport = true;
			} else if (DISABLED.equals(optionValue)) {
				this.suggestStaticImport = false;
			}
		}
	}

	private char[][] splitAndTrimOn(char divider, char[] arrayToSplit) {
		char[][] result = CharOperation.splitAndTrimOn(',', arrayToSplit);

		int length = result.length;

		int resultCount = 0;
		for (int i = 0; i < length; i++) {
			if(result[i].length != 0) {
				result[resultCount++] = result[i];
			}
		}
		if(resultCount != length) {
			System.arraycopy(result, 0, result = new char[resultCount][], 0, resultCount);
		}
		return result;
	}
}
