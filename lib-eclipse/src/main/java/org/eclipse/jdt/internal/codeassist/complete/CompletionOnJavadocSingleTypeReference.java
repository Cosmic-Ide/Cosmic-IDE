/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

import org.eclipse.jdt.internal.compiler.ast.JavadocSingleTypeReference;

public class CompletionOnJavadocSingleTypeReference extends JavadocSingleTypeReference implements CompletionOnJavadoc {
	public int completionFlags = JAVADOC;

	public CompletionOnJavadocSingleTypeReference(char[] source, long pos, int tagStart, int tagEnd) {
		super(source, pos, tagStart, tagEnd);
	}

	public CompletionOnJavadocSingleTypeReference(JavadocSingleTypeReference typeRef) {
		super(typeRef.token, (((long)typeRef.sourceStart)<<32)+typeRef.sourceEnd, typeRef.tagSourceStart, typeRef.tagSourceStart);
	}

	@Override
	public void addCompletionFlags(int flags) {
		this.completionFlags |= flags;
	}

	public boolean completeAnException() {
		return (this.completionFlags & EXCEPTION) != 0;
	}

	public boolean completeInText() {
		return (this.completionFlags & TEXT) != 0;
	}

	public boolean completeBaseTypes() {
		return (this.completionFlags & BASE_TYPES) != 0;
	}

	public boolean completeFormalReference() {
		return (this.completionFlags & FORMAL_REFERENCE) != 0;
	}

	@Override
	public int getCompletionFlags() {
		return this.completionFlags;
	}

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output) {
		output.append("<CompletionOnJavadocSingleTypeReference:"); //$NON-NLS-1$
		super.printExpression(indent, output);
		indent++;
		if (this.completionFlags > 0) {
			output.append('\n');
			for (int i=0; i<indent; i++) output.append('\t');
			output.append("infos:"); //$NON-NLS-1$
			char separator = 0;
			if (completeAnException()) {
				output.append("exception"); //$NON-NLS-1$
				separator = ',';
			}
			if (completeInText()) {
				if (separator != 0) output.append(separator);
				output.append("text"); //$NON-NLS-1$
				separator = ',';
			}
			if (completeBaseTypes()) {
				if (separator != 0) output.append(separator);
				output.append("base types"); //$NON-NLS-1$
				separator = ',';
			}
			if (completeFormalReference()) {
				if (separator != 0) output.append(separator);
				output.append("formal reference"); //$NON-NLS-1$
				separator = ',';
			}
			output.append('\n');
		}
		indent--;
		for (int i=0; i<indent; i++) output.append('\t');
		return output.append('>');
	}
}
