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
package org.eclipse.jdt.internal.codeassist;

public interface RelevanceConstants {


	/*
	 * Important: The following rules must be strictly adhered to while declaring new relevance constants or modifying the existing:
	 * 1. One or more relevance constants are used in combination to form a relevance.
	 * 2. A particular relevance constant can be added only once to form a relevance.
	 * 3. A resultant relevance (after combining all the applicable relevance constants) must be a positive number.
	 * 4. The value of R_DEFAULT is maintained at a positive value such that the sum of all the negative relevance constants
	 *    and R_DEFAULT must not be negative.
	 */
	int R_DEFAULT = 30;
	int R_INTERESTING = 5;
	int R_CASE = 10;
	int R_CAMEL_CASE = 5;
	int R_EXACT_NAME = 4;
	int R_VOID = -5;
	int R_EXPECTED_TYPE = 20;
	int R_EXACT_EXPECTED_TYPE = 30;
	int R_INTERFACE = 20;
	int R_CLASS = 20;
	int R_ENUM = 20;
	int R_ANNOTATION = 20;
	int R_EXCEPTION = 20;
	int R_ENUM_CONSTANT = 5;
	int R_ABSTRACT_METHOD = 20;
	int R_NON_STATIC = 11;
	int R_UNQUALIFIED = 3;
	int R_QUALIFIED = 2;
	int R_NAME_FIRST_PREFIX = 6;
	int R_NAME_PREFIX = 5;
	int R_NAME_FIRST_SUFFIX = 4;
	int R_NAME_SUFFIX = 3;
	int R_NAME_LESS_NEW_CHARACTERS = 15;
	int R_SUBSTRING = -21;
	int R_SUBWORD = -22;
	int R_METHOD_OVERIDE = 3;
	int R_NON_RESTRICTED = 3;
	int R_TRUE_OR_FALSE = 1;
	int R_INLINE_TAG = 31;
	int R_VALUE_TAG = 31;
	int R_NON_INHERITED = 2;
	int R_NO_PROBLEMS = 1;
	int R_RESOLVED = 1;
	int R_TARGET = 5;
	int R_FINAL = 3; // https://bugs.eclipse.org/bugs/show_bug.cgi?id=195346
	int R_CONSTRUCTOR = 3; // https://bugs.eclipse.org/bugs/show_bug.cgi?id=373409
	int R_MODULE_DECLARATION = 31;
}
