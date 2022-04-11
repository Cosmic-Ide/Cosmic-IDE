/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core;

public interface JavadocConstants {

	String ANCHOR_PREFIX_END = "\""; //$NON-NLS-1$
	char[] ANCHOR_PREFIX_START = "<A NAME=\"".toCharArray(); //$NON-NLS-1$
	char[] ANCHOR_PREFIX_START_2 = "<A ID=\"".toCharArray(); //$NON-NLS-1$
	int ANCHOR_PREFIX_START_LENGTH = ANCHOR_PREFIX_START.length;
	int ANCHOR_PREFIX_START2_LENGTH = ANCHOR_PREFIX_START_2.length;
	char[] ANCHOR_SUFFIX = "</A>".toCharArray(); //$NON-NLS-1$
	int ANCHOR_SUFFIX_LENGTH = JavadocConstants.ANCHOR_SUFFIX.length;
	char[] CONSTRUCTOR_DETAIL = "<!-- ========= CONSTRUCTOR DETAIL ======== -->".toCharArray(); //$NON-NLS-1$
	char[] CONSTRUCTOR_SUMMARY = "<!-- ======== CONSTRUCTOR SUMMARY ======== -->".toCharArray(); //$NON-NLS-1$
	char[] FIELD_DETAIL= "<!-- ============ FIELD DETAIL =========== -->".toCharArray(); //$NON-NLS-1$
	char[] FIELD_SUMMARY = "<!-- =========== FIELD SUMMARY =========== -->".toCharArray(); //$NON-NLS-1$
	char[] ENUM_CONSTANT_SUMMARY = "<!-- =========== ENUM CONSTANT SUMMARY =========== -->".toCharArray(); //$NON-NLS-1$
	char[] ANNOTATION_TYPE_REQUIRED_MEMBER_SUMMARY = "<!-- =========== ANNOTATION TYPE REQUIRED MEMBER SUMMARY =========== -->".toCharArray(); //$NON-NLS-1$
	char[] ANNOTATION_TYPE_OPTIONAL_MEMBER_SUMMARY = "<!-- =========== ANNOTATION TYPE OPTIONAL MEMBER SUMMARY =========== -->".toCharArray(); //$NON-NLS-1$
	char[] END_OF_CLASS_DATA = "<!-- ========= END OF CLASS DATA ========= -->".toCharArray(); //$NON-NLS-1$
	String HTML_EXTENSION = ".html"; //$NON-NLS-1$
	String INDEX_FILE_NAME = "index.html"; //$NON-NLS-1$
	char[] METHOD_DETAIL = "<!-- ============ METHOD DETAIL ========== -->".toCharArray(); //$NON-NLS-1$
	char[] METHOD_SUMMARY = "<!-- ========== METHOD SUMMARY =========== -->".toCharArray(); //$NON-NLS-1$
	char[] NESTED_CLASS_SUMMARY = "<!-- ======== NESTED CLASS SUMMARY ======== -->".toCharArray(); //$NON-NLS-1$
	String PACKAGE_FILE_NAME = "package-summary.html"; //$NON-NLS-1$
	char[] PACKAGE_DESCRIPTION_START = "name=\"package_description\"".toCharArray(); //$NON-NLS-1$
	char[] PACKAGE_DESCRIPTION_START2 = "name=\"package.description\"".toCharArray(); //$NON-NLS-1$
	char[] PACKAGE_DESCRIPTION_START3 = "<div class=\"contentContainer\">".toCharArray(); //$NON-NLS-1$
	String MODULE_FILE_SUFFIX = "-summary.html"; //$NON-NLS-1$
	char[] MODULE_DESCRIPTION_START = "<!-- ============ MODULE DESCRIPTION =========== -->".toCharArray(); //$NON-NLS-1$
	char[] H2_PREFIX = "<H2".toCharArray(); //$NON-NLS-1$
	char[] H2_SUFFIX = "</H2>".toCharArray(); //$NON-NLS-1$
	int H2_SUFFIX_LENGTH = H2_SUFFIX.length;
	char[] BOTTOM_NAVBAR = "<!-- ======= START OF BOTTOM NAVBAR ====== -->".toCharArray(); //$NON-NLS-1$
	char[] SEPARATOR_START = "<!-- =".toCharArray(); //$NON-NLS-1$
	char[] START_OF_CLASS_DATA = "<!-- ======== START OF CLASS DATA ======== -->".toCharArray(); //$NON-NLS-1$
	int START_OF_CLASS_DATA_LENGTH = JavadocConstants.START_OF_CLASS_DATA.length;
	String P = "<P>"; //$NON-NLS-1$
	String DIV_CLASS_BLOCK = "<DIV CLASS=\"BLOCK\">"; //$NON-NLS-1$
}
