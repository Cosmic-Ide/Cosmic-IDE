/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
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

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.IBinaryMethod;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObjectToIntArray;
import org.eclipse.jdt.internal.core.util.Util;

public class JavadocContents {
	private static final int[] UNKNOWN_FORMAT = new int[0];

	private BinaryType type;
	private char[] content;

	private int childrenStart;

	private boolean hasComputedChildrenSections = false;
	private int indexOfFieldDetails;
	private int indexOfConstructorDetails;
	private int indexOfMethodDetails;
	private int indexOfEndOfClassData;

	private int indexOfFieldsBottom;
	private int indexOfAllMethodsTop;
	private int indexOfAllMethodsBottom;

	private int[] typeDocRange;
	private HashtableOfObjectToIntArray fieldDocRanges;
	private HashtableOfObjectToIntArray methodDocRanges;

	private int[] fieldAnchorIndexes;
	private int fieldAnchorIndexesCount;
	private int fieldLastAnchorFoundIndex;
	private int[] methodAnchorIndexes;
	private int methodAnchorIndexesCount;
	private int methodLastAnchorFoundIndex;
	private int[] unknownFormatAnchorIndexes;
	private int unknownFormatAnchorIndexesCount;
	private int unknownFormatLastAnchorFoundIndex;
	private int[] tempAnchorIndexes;
	private int tempAnchorIndexesCount;
	private int tempLastAnchorFoundIndex;

	public JavadocContents(BinaryType type, String content) {
		this(content);
		this.type = type;
	}

	public JavadocContents(String content) {
		this.content = content != null ? content.toCharArray() : null;
	}
	/*
	 * Returns the part of the javadoc that describe the type
	 */
	public String getTypeDoc() throws JavaModelException {
		if (this.content == null) return null;

		synchronized (this) {
			if (this.typeDocRange == null) {
				computeTypeRange();
			}
		}

		if (this.typeDocRange != null) {
			if (this.typeDocRange == UNKNOWN_FORMAT) throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.UNKNOWN_JAVADOC_FORMAT, this.type));
			return new String(this.content, this.typeDocRange[0], this.typeDocRange[1] - this.typeDocRange[0]);
		}
		return null;
	}

	public String getPackageDoc() throws JavaModelException {
		if (this.content == null) return null;
		int index = CharOperation.indexOf(JavadocConstants.PACKAGE_DESCRIPTION_START2, this.content, false, 0);
		if (index == -1) {
			index = CharOperation.indexOf(JavadocConstants.PACKAGE_DESCRIPTION_START, this.content, false, 0);
		}
		if (index != -1) {
			index = CharOperation.indexOf(JavadocConstants.ANCHOR_SUFFIX, this.content, false, index);
			if (index == -1) return null;

			int start = CharOperation.indexOf(JavadocConstants.H2_PREFIX, this.content, false, index);
			if (start != -1) {
				start = CharOperation.indexOf(JavadocConstants.H2_SUFFIX, this.content, false, start);
				if (start != -1) index = start + JavadocConstants.H2_SUFFIX_LENGTH;
			}
		} else {
			index = CharOperation.indexOf(JavadocConstants.PACKAGE_DESCRIPTION_START3, this.content, false, 0);
		}
		if (index != -1) {
			int end = CharOperation.indexOf(JavadocConstants.BOTTOM_NAVBAR, this.content, false, index);
			if (end == -1) end = this.content.length -1;
			return new String(this.content, index, end - index);
		}
		return null;
	}

	public String getModuleDoc() throws JavaModelException {
		if (this.content == null) return null;
		int index = CharOperation.indexOf(JavadocConstants.MODULE_DESCRIPTION_START, this.content, false, 0);
		if (index == -1) return null;
		int end = CharOperation.indexOf(JavadocConstants.BOTTOM_NAVBAR, this.content, false, index);
		if (end == -1) end = this.content.length -1;
		return new String(this.content, index, end - index);
	}

	/*
	 * Returns the part of the javadoc that describe a field of the type
	 */
	public String getFieldDoc(IField child) throws JavaModelException {
		if (this.content == null) return null;

		int[] range = null;
		synchronized (this) {
			if (this.fieldDocRanges == null) {
				this.fieldDocRanges = new HashtableOfObjectToIntArray();
			} else {
				range = this.fieldDocRanges.get(child);
			}

			if (range == null) {
				range = computeFieldRange(child);
				this.fieldDocRanges.put(child, range);
			}
		}

		if (range != null) {
			if (range == UNKNOWN_FORMAT) throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.UNKNOWN_JAVADOC_FORMAT, child));
			return new String(this.content, range[0], range[1] - range[0]);
		}
		return null;
	}

	/*
	 * Returns the part of the javadoc that describe a method of the type
	 */
	public String getMethodDoc(IMethod child) throws JavaModelException {
		if (this.content == null) return null;

		int[] range = null;
		synchronized (this) {
			if (this.methodDocRanges == null) {
				this.methodDocRanges = new HashtableOfObjectToIntArray();
			} else {
				range = this.methodDocRanges.get(child);
			}

			if (range == null) {
				range = computeMethodRange(child);
				this.methodDocRanges.put(child, range);
			}
		}

		if (range != null) {
			if (range == UNKNOWN_FORMAT) {
				throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.UNKNOWN_JAVADOC_FORMAT, child));
			}
			return new String(this.content, range[0], range[1] - range[0]);
		}
		return null;
	}

	/*
	 * Compute the ranges of the parts of the javadoc that describe each method of the type
	 */
	private int[] computeChildRange(char[] anchor, int indexOfSectionBottom) throws JavaModelException {

		// checks each known anchor locations
		if (this.tempAnchorIndexesCount > 0) {
			for (int i = 0; i < this.tempAnchorIndexesCount; i++) {
				int anchorEndStart = this.tempAnchorIndexes[i];

				if (anchorEndStart != -1 && CharOperation.prefixEquals(anchor, this.content, false, anchorEndStart)) {

					this.tempAnchorIndexes[i] = -1;

					return computeChildRange(anchorEndStart, anchor, indexOfSectionBottom);
				}
			}
		}

		int fromIndex = this.tempLastAnchorFoundIndex;
		int[] index;

		// check each next unknown anchor locations
		index = getAnchorIndex(fromIndex);
		while (index[0] != -1 && (index[0] < indexOfSectionBottom || indexOfSectionBottom == -1)) {
			fromIndex = index[0] + 1;

			int anchorEndStart = index[0] + index[1];

			this.tempLastAnchorFoundIndex = anchorEndStart;

			if (CharOperation.prefixEquals(anchor, this.content, false, anchorEndStart)) {
				return computeChildRange(anchorEndStart, anchor, indexOfSectionBottom);
			} else {
				if (this.tempAnchorIndexes.length == this.tempAnchorIndexesCount) {
					System.arraycopy(this.tempAnchorIndexes, 0, this.tempAnchorIndexes = new int[this.tempAnchorIndexesCount + 20], 0, this.tempAnchorIndexesCount);
				}

				this.tempAnchorIndexes[this.tempAnchorIndexesCount++] = anchorEndStart;
			}
			index = getAnchorIndex(fromIndex);
		}

		return null;
	}
	private int[] getAnchorIndex(int fromIndex) {
		int index = CharOperation.indexOf(JavadocConstants.ANCHOR_PREFIX_START, this.content, false, fromIndex);
		if (index != -1) {
			return new int[]{index, JavadocConstants.ANCHOR_PREFIX_START_LENGTH};
		}
		if (index == -1) {
			index = CharOperation.indexOf(JavadocConstants.ANCHOR_PREFIX_START_2, this.content, false, fromIndex);
		}
		if (index == -1) {
			return new int[]{-1, -1};
		} else {
			return new int[]{index, JavadocConstants.ANCHOR_PREFIX_START2_LENGTH};
		}
	}
	private int[] computeChildRange(int anchorEndStart, char[] anchor, int indexOfBottom) {
		int[] range = null;

		// try to find the bottom of the section
		if (indexOfBottom != -1) {
			// try to find the end of the anchor
			int indexOfEndLink = CharOperation.indexOf(JavadocConstants.ANCHOR_SUFFIX, this.content, false, anchorEndStart + anchor.length);
			if (indexOfEndLink != -1) {
				// try to find the next anchor
				int indexOfNextElement = getAnchorIndex(indexOfEndLink)[0];

				int javadocStart = indexOfEndLink + JavadocConstants.ANCHOR_SUFFIX_LENGTH;
				int javadocEnd = indexOfNextElement == -1 ? indexOfBottom : Math.min(indexOfNextElement, indexOfBottom);
				range = new int[]{javadocStart, javadocEnd};
			} else {
				// the anchor has no suffix
				range = UNKNOWN_FORMAT;
			}
		} else {
			// the detail section has no bottom
			range = UNKNOWN_FORMAT;
		}

		return range;
	}

	private void computeChildrenSections() {
		// try to find the next separator part
		int lastIndex = CharOperation.indexOf(JavadocConstants.SEPARATOR_START, this.content, false, this.childrenStart);
		lastIndex = lastIndex == -1 ? this.childrenStart : lastIndex;

		// try to find field detail start
		this.indexOfFieldDetails = CharOperation.indexOf(JavadocConstants.FIELD_DETAIL, this.content, false, lastIndex);
		lastIndex = this.indexOfFieldDetails == -1 ? lastIndex : this.indexOfFieldDetails;

		// try to find constructor detail start
		this.indexOfConstructorDetails = CharOperation.indexOf(JavadocConstants.CONSTRUCTOR_DETAIL, this.content, false, lastIndex);
		lastIndex = this.indexOfConstructorDetails == -1 ? lastIndex : this.indexOfConstructorDetails;

		// try to find method detail start
		this.indexOfMethodDetails = CharOperation.indexOf(JavadocConstants.METHOD_DETAIL, this.content, false, lastIndex);
		lastIndex = this.indexOfMethodDetails == -1 ? lastIndex : this.indexOfMethodDetails;

		// we take the end of class data
		this.indexOfEndOfClassData = CharOperation.indexOf(JavadocConstants.END_OF_CLASS_DATA, this.content, false, lastIndex);

		// try to find the field detail end
		this.indexOfFieldsBottom =
			this.indexOfConstructorDetails != -1 ? this.indexOfConstructorDetails :
				this.indexOfMethodDetails != -1 ? this.indexOfMethodDetails:
					this.indexOfEndOfClassData;

		this.indexOfAllMethodsTop =
			this.indexOfConstructorDetails != -1 ?
					this.indexOfConstructorDetails :
						this.indexOfMethodDetails;

		this.indexOfAllMethodsBottom = this.indexOfEndOfClassData;

		this.hasComputedChildrenSections = true;
	}

	/*
	 * Compute the ranges of the parts of the javadoc that describe each child of the type (fields, methods)
	 */
	private int[] computeFieldRange(IField field) throws JavaModelException {
		if (!this.hasComputedChildrenSections) {
			computeChildrenSections();
		}

		StringBuffer buffer = new StringBuffer(field.getElementName());
		buffer.append(JavadocConstants.ANCHOR_PREFIX_END);
		char[] anchor = String.valueOf(buffer).toCharArray();

		int[] range = null;

		if (this.indexOfFieldDetails == -1 || this.indexOfFieldsBottom == -1) {
			// the detail section has no top or bottom, so the doc has an unknown format
			if (this.unknownFormatAnchorIndexes == null) {
				this.unknownFormatAnchorIndexes = new int[this.type.getChildren().length];
				this.unknownFormatAnchorIndexesCount = 0;
				this.unknownFormatLastAnchorFoundIndex = this.childrenStart;
			}

			this.tempAnchorIndexes = this.unknownFormatAnchorIndexes;
			this.tempAnchorIndexesCount = this.unknownFormatAnchorIndexesCount;
			this.tempLastAnchorFoundIndex = this.unknownFormatLastAnchorFoundIndex;

			range = computeChildRange(anchor, this.indexOfFieldsBottom);

			this.unknownFormatLastAnchorFoundIndex = this.tempLastAnchorFoundIndex;
			this.unknownFormatAnchorIndexesCount = this.tempAnchorIndexesCount;
			this.unknownFormatAnchorIndexes = this.tempAnchorIndexes;
		} else {
			if (this.fieldAnchorIndexes == null) {
				this.fieldAnchorIndexes = new int[this.type.getFields().length];
				this.fieldAnchorIndexesCount = 0;
				this.fieldLastAnchorFoundIndex = this.indexOfFieldDetails;
			}

			this.tempAnchorIndexes = this.fieldAnchorIndexes;
			this.tempAnchorIndexesCount = this.fieldAnchorIndexesCount;
			this.tempLastAnchorFoundIndex = this.fieldLastAnchorFoundIndex;

			range = computeChildRange(anchor, this.indexOfFieldsBottom);

			this.fieldLastAnchorFoundIndex = this.tempLastAnchorFoundIndex;
			this.fieldAnchorIndexesCount = this.tempAnchorIndexesCount;
			this.fieldAnchorIndexes = this.tempAnchorIndexes;
		}

		return range;
	}

	/*
	 * Compute the ranges of the parts of the javadoc that describe each method of the type
	 */
	private int[] computeMethodRange(IMethod method) throws JavaModelException {
		if (!this.hasComputedChildrenSections) {
			computeChildrenSections();
		}

		char[] anchor = computeMethodAnchorPrefixEnd((BinaryMethod)method).toCharArray();

		int[] range = null;

		if (this.indexOfAllMethodsTop == -1 || this.indexOfAllMethodsBottom == -1) {
			// the detail section has no top or bottom, so the doc has an unknown format
			if (this.unknownFormatAnchorIndexes == null) {
				this.unknownFormatAnchorIndexes = new int[this.type.getChildren().length];
				this.unknownFormatAnchorIndexesCount = 0;
				this.unknownFormatLastAnchorFoundIndex = this.childrenStart;
			}

			this.tempAnchorIndexes = this.unknownFormatAnchorIndexes;
			this.tempAnchorIndexesCount = this.unknownFormatAnchorIndexesCount;
			this.tempLastAnchorFoundIndex = this.unknownFormatLastAnchorFoundIndex;

			range = computeChildRange(anchor, this.indexOfFieldsBottom);
			if (range == null) {
				range = computeChildRange(getJavadoc8Anchor(anchor), this.indexOfAllMethodsBottom);
			}

			this.unknownFormatLastAnchorFoundIndex = this.tempLastAnchorFoundIndex;
			this.unknownFormatAnchorIndexesCount = this.tempAnchorIndexesCount;
			this.unknownFormatAnchorIndexes = this.tempAnchorIndexes;
		} else {
			if (this.methodAnchorIndexes == null) {
				this.methodAnchorIndexes = new int[this.type.getFields().length];
				this.methodAnchorIndexesCount = 0;
				this.methodLastAnchorFoundIndex = this.indexOfAllMethodsTop;
			}

			this.tempAnchorIndexes = this.methodAnchorIndexes;
			this.tempAnchorIndexesCount = this.methodAnchorIndexesCount;
			this.tempLastAnchorFoundIndex = this.methodLastAnchorFoundIndex;

			range = computeChildRange(anchor, this.indexOfAllMethodsBottom);
			if (range == null) {
				range = computeChildRange(getJavadoc8Anchor(anchor), this.indexOfAllMethodsBottom);
			}

			this.methodLastAnchorFoundIndex = this.tempLastAnchorFoundIndex;
			this.methodAnchorIndexesCount = this.tempAnchorIndexesCount;
			this.methodAnchorIndexes = this.tempAnchorIndexes;
		}

		return range;
	}

	private static char[] getJavadoc8Anchor(char[] anchor) {
		// fix for bug 432284: [1.8] Javadoc-8-style anchors not found by IMethod#getAttachedJavadoc(..)
		char[] anchor8 = new char[anchor.length];
		int i8 = 0;
		for (int i = 0; i < anchor.length; i++) {
			char ch = anchor[i];
			switch (ch) {
				case '(':
				case ')':
				case ',':
					anchor8[i8++] = '-';
					break;
				case '[':
					anchor8[i8++] = ':';
					anchor8[i8++] = 'A';
					break;
				case ' ': // handled by preceding ','
				case ']': // handled by preceding '['
					break;
				default:
					anchor8[i8++] = ch;
			}
		}
		if (i8 != anchor.length) {
			anchor8 = CharOperation.subarray(anchor8, 0, i8);
		}
		return anchor8;
	}

	private String computeMethodAnchorPrefixEnd(BinaryMethod method) throws JavaModelException {
		String typeQualifiedName = null;
		if (this.type.isMember()) {
			IType currentType = this.type;
			StringBuilder buffer = new StringBuilder();
			while (currentType != null) {
				buffer.insert(0, currentType.getElementName());
				currentType = currentType.getDeclaringType();
				if (currentType != null) {
					buffer.insert(0, '.');
				}
			}
			typeQualifiedName = buffer.toString();
		} else {
			typeQualifiedName = this.type.getElementName();
		}

		String methodName = method.getElementName();
		if (method.isConstructor()) {
			methodName = typeQualifiedName;
		}
		IBinaryMethod info = (IBinaryMethod) method.getElementInfo();

		char[] genericSignature = info.getGenericSignature();
		String anchor = null;
		if (genericSignature != null) {
			genericSignature = CharOperation.replaceOnCopy(genericSignature, '/', '.');
			anchor = Util.toAnchor(0, genericSignature, methodName, Flags.isVarargs(method.getFlags()));
			if (anchor == null) throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.UNKNOWN_JAVADOC_FORMAT, method));
		} else {
			anchor = Signature.toString(method.getSignature().replace('/', '.'), methodName, null, true, false, Flags.isVarargs(method.getFlags()));
		}
		IType declaringType = this.type;
		if (declaringType.isMember()) {
			// might need to remove a part of the signature corresponding to the synthetic argument (only for constructor)
			if (method.isConstructor() && !Flags.isStatic(declaringType.getFlags())) {
				int indexOfOpeningParen = anchor.indexOf('(');
				if (indexOfOpeningParen == -1) {
					// should not happen as this is a method signature
					return null;
				}
				int index = indexOfOpeningParen;
				indexOfOpeningParen++;
				int indexOfComma = anchor.indexOf(',', index);
				if (indexOfComma != -1) {
					index = indexOfComma + 2;
				} else {
					// no argument, but a synthetic argument
					index = anchor.indexOf(')', index);
				}
				anchor = anchor.substring(0, indexOfOpeningParen) + anchor.substring(index);
			}
		}
		return anchor + JavadocConstants.ANCHOR_PREFIX_END;
	}

	/*
	 * Compute the range of the part of the javadoc that describe the type
	 */
	private void computeTypeRange() throws JavaModelException {
		final int indexOfStartOfClassData = CharOperation.indexOf(JavadocConstants.START_OF_CLASS_DATA, this.content, false);
		if (indexOfStartOfClassData == -1) {
			this.typeDocRange = UNKNOWN_FORMAT;
			return;
		}
		int indexOfNextSeparator = CharOperation.indexOf(JavadocConstants.SEPARATOR_START, this.content, false, indexOfStartOfClassData);
		if (indexOfNextSeparator == -1) {
			this.typeDocRange = UNKNOWN_FORMAT;
			return;
		}
		int indexOfNextSummary = CharOperation.indexOf(JavadocConstants.NESTED_CLASS_SUMMARY, this.content, false, indexOfNextSeparator);
		if (indexOfNextSummary == -1 && this.type.isEnum()) {
			// try to find enum constant summary start
			indexOfNextSummary = CharOperation.indexOf(JavadocConstants.ENUM_CONSTANT_SUMMARY, this.content, false, indexOfNextSeparator);
		}
		if (indexOfNextSummary == -1 && this.type.isAnnotation()) {
			// try to find required enum constant summary start
			indexOfNextSummary = CharOperation.indexOf(JavadocConstants.ANNOTATION_TYPE_REQUIRED_MEMBER_SUMMARY, this.content, false, indexOfNextSeparator);
			if (indexOfNextSummary == -1) {
				// try to find optional enum constant summary start
				indexOfNextSummary = CharOperation.indexOf(JavadocConstants.ANNOTATION_TYPE_OPTIONAL_MEMBER_SUMMARY, this.content, false, indexOfNextSeparator);
			}
		}
		if (indexOfNextSummary == -1) {
			// try to find field summary start
			indexOfNextSummary = CharOperation.indexOf(JavadocConstants.FIELD_SUMMARY, this.content, false, indexOfNextSeparator);
		}
		if (indexOfNextSummary == -1) {
			// try to find constructor summary start
			indexOfNextSummary = CharOperation.indexOf(JavadocConstants.CONSTRUCTOR_SUMMARY, this.content, false, indexOfNextSeparator);
		}
		if (indexOfNextSummary == -1) {
			// try to find method summary start
			indexOfNextSummary = CharOperation.indexOf(JavadocConstants.METHOD_SUMMARY, this.content, false, indexOfNextSeparator);
		}

		if (indexOfNextSummary == -1) {
			// we take the end of class data
			indexOfNextSummary = CharOperation.indexOf(JavadocConstants.END_OF_CLASS_DATA, this.content, false, indexOfNextSeparator);
		} else {
			// improve performance of computation of children ranges
			this.childrenStart = indexOfNextSummary + 1;
		}

		if (indexOfNextSummary == -1) {
			this.typeDocRange = UNKNOWN_FORMAT;
			return;
		}
		/*
		 * Cut off the type hierarchy, see bug 119844.
		 * We remove the contents between the start of class data and where
		 * we guess the actual class comment starts.
		 */
		int start = indexOfStartOfClassData + JavadocConstants.START_OF_CLASS_DATA_LENGTH;
		int indexOfFirstParagraph = CharOperation.indexOf(JavadocConstants.P.toCharArray(), this.content, false, start, indexOfNextSummary);
		int indexOfFirstDiv = CharOperation.indexOf(JavadocConstants.DIV_CLASS_BLOCK.toCharArray(), this.content, false, start, indexOfNextSummary);
		int afterHierarchy = indexOfNextSummary;
		if (indexOfFirstParagraph != -1 && indexOfFirstParagraph < afterHierarchy) {
			afterHierarchy = indexOfFirstParagraph;
		}
		if (indexOfFirstDiv != -1 && indexOfFirstDiv < afterHierarchy) {
			afterHierarchy = indexOfFirstDiv;
		}
		if (afterHierarchy != indexOfNextSummary) {
			start = afterHierarchy;
		}

		this.typeDocRange = new int[]{start, indexOfNextSummary};
	}
}
