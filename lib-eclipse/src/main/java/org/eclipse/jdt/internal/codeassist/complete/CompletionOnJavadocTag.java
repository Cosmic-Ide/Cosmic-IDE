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
package org.eclipse.jdt.internal.codeassist.complete;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.JavadocSingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.parser.JavadocTagConstants;

public class CompletionOnJavadocTag extends JavadocSingleNameReference implements JavadocTagConstants, CompletionOnJavadoc {
	public int completionFlags = JAVADOC;
	public final static char[][][] NO_CHAR_CHAR_CHAR = new char[0][][];
	private char[][][] possibleTags = NO_CHAR_CHAR_CHAR;

	public CompletionOnJavadocTag(char[] source, long pos, int tagStart, int tagEnd, char[][][] possibleTags, boolean orphan) {
		super(source, pos, tagStart, tagEnd);
		this.possibleTags = possibleTags;
		if (orphan) this.completionFlags |= ALL_POSSIBLE_TAGS;
	}

	@Override
	public void addCompletionFlags(int flags) {
		this.completionFlags |= flags;
	}

	@Override
	public int getCompletionFlags() {
		return this.completionFlags;
	}

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output) {
		output.append("<CompleteOnJavadocTag:"); //$NON-NLS-1$
		output.append('@');
		if (this.token != null) super.printExpression(indent, output);
		// Print block tags
		char[][] blockTags = this.possibleTags[BLOCK_IDX];
		if (blockTags != null) {
			int length=blockTags.length;
			if (length > 0) {
				output.append("\npossible block tags:"); //$NON-NLS-1$
				for (int i=0; i<length; i++) {
					output.append("\n	- "); //$NON-NLS-1$
					output.append(blockTags[i]);
				}
				output.append('\n');
			}
		}
		// Print inline tags
		char[][] inlineTags = this.possibleTags[INLINE_IDX];
		if (inlineTags != null) {
			int length=inlineTags.length;
			if (length > 0) {
				output.append("\npossible inline tags:"); //$NON-NLS-1$
				for (int i=0; i<length; i++) {
					output.append("\n	- "); //$NON-NLS-1$
					output.append(inlineTags[i]);
				}
				output.append('\n');
			}
		}
		return output.append('>');
	}

	public void filterPossibleTags(Scope scope) {
		if (this.possibleTags == null || this.possibleTags.length == 0 || (this.completionFlags & ALL_POSSIBLE_TAGS) != 0) {
			return;
		}
		int kind = scope.kind;
		char[][] specifiedTags = null;
		switch (kind) {
			case Scope.COMPILATION_UNIT_SCOPE:
				// bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=255752
				// Check for FAKE_TYPE_NAME to allow proposals (@see CompletionParser#consumeCompilationUnit)
				CompilationUnitDeclaration compilationUnit = scope.referenceCompilationUnit();
				if(compilationUnit != null && compilationUnit.isModuleInfo() ) {
					specifiedTags = MODULE_TAGS;
					break;
				}
				if (compilationUnit != null &&
						(compilationUnit.types.length > 0 && compilationUnit.types[0].name == CompletionParser.FAKE_TYPE_NAME)) {
					specifiedTags = CLASS_TAGS;
				} else {
					specifiedTags = COMPILATION_UNIT_TAGS;
				}
				break;
			case Scope.CLASS_SCOPE:
				specifiedTags = CLASS_TAGS;
				break;
			case Scope.METHOD_SCOPE:
				MethodScope methodScope = (MethodScope) scope;
				if (methodScope.referenceMethod() == null) {
					if (methodScope.initializedField == null) {
						specifiedTags = PACKAGE_TAGS;
					} else {
						specifiedTags = FIELD_TAGS;
					}
				} else {
					specifiedTags = METHOD_TAGS;
				}
				break;
			default:
				return;
		}
		int kinds = this.possibleTags.length;
		for (int k=0; k<kinds; k++) {
			int length = this.possibleTags[k].length;
			int specLenth = specifiedTags.length;
			char[][] filteredTags = new char[length][];
			int size = 0;
			for (int i=0; i<length; i++) {
				char[] possibleTag = this.possibleTags[k][i];
				for (int j=0; j<specLenth; j++) {
					if (possibleTag[0] == specifiedTags[j][0] && CharOperation.equals(possibleTag, specifiedTags[j])) {
						if (possibleTag == TAG_PARAM) {
							switch (scope.kind) {
								case Scope.CLASS_SCOPE:
									if (scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5) {
										TypeDeclaration typeDecl = ((ClassScope)scope).referenceContext;
										boolean isRecordWithComponent = typeDecl.isRecord() && typeDecl.nRecordComponents >0 ;
										if (((ClassScope)scope).referenceContext.binding.isGenericType() || isRecordWithComponent) {
											filteredTags[size++] = possibleTag;
										}
									}
									break;
								case Scope.COMPILATION_UNIT_SCOPE:
									if (scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5) {
										filteredTags[size++] = possibleTag;
									}
									break;
								default:
									filteredTags[size++] = possibleTag;
									break;
							}
						} else {
							filteredTags[size++] = possibleTag;
						}
						break;
					}
				}
			}
			if (size<length) {
				System.arraycopy(filteredTags, 0, this.possibleTags[k] = new char[size][], 0, size);
			}
		}
	}

	/**
	 * Return possible block tags
	 *
	 * @return char[][]
	 */
	public char[][] getPossibleBlockTags() {
		return this.possibleTags[BLOCK_IDX];
	}

	/**
	 * Return possible inline tags
	 *
	 * @return char[][]
	 */
	public char[][] getPossibleInlineTags() {
		return this.possibleTags[INLINE_IDX];
	}
}
