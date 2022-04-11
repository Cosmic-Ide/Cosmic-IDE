/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Stephan Herrmann - Contribution for
 *								Bug 440474 - [null] textual encoding of external null annotations
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.classfmt.ExternalAnnotationProvider;

public class SignatureWrapper {
	public char[] signature;
	public int start;
	public int end;
	public int bracket;
	private boolean use15specifics;
	private boolean useExternalAnnotations;

	public SignatureWrapper(char[] signature, boolean use15specifics) {
		this.signature = signature;
		this.start = 0;
		this.end = this.bracket = -1;
		this.use15specifics = use15specifics;
		if (!use15specifics) removeTypeArguments();
	}
	public SignatureWrapper(char[] signature, boolean use15specifics, boolean useExternalAnnotations) {
		this.signature = signature;
		this.start = 0;
		this.end = this.bracket = -1;
		this.use15specifics = use15specifics;
		this.useExternalAnnotations = useExternalAnnotations;
		if (!use15specifics) removeTypeArguments();
	}
	public SignatureWrapper(char [] signature) {
		this(signature, true);
	}
	public boolean atEnd() {
		return this.start < 0 || this.start >= this.signature.length;
	}
	public boolean isParameterized() {
		return this.bracket == this.end;
	}
	public int computeEnd() {
		int index = this.start;
		if (this.useExternalAnnotations) {
			// in addition to '[' tokens accept null annotations after the first '['
			skipDimensions: while(true) {
				switch (this.signature[index]) {
					case ExternalAnnotationProvider.NONNULL :
					case ExternalAnnotationProvider.NULLABLE :
					case ExternalAnnotationProvider.NO_ANNOTATION :
						if (index == this.start)
							break skipDimensions;
						//$FALL-THROUGH$
					case '[':
						index++;
						break;
					default:
						break skipDimensions;
				}
			}
		} else {
			while (this.signature[index] == '[')
				index++;
		}
		switch (this.signature[index]) {
			case 'L' :
			case 'T' :
				this.end = CharOperation.indexOf(';', this.signature, this.start);
				if (this.bracket <= this.start) // already know it if its > start
					this.bracket = CharOperation.indexOf('<', this.signature, this.start);

				if (this.bracket > this.start && this.bracket < this.end)
					this.end = this.bracket;
				else if (this.end == -1)
					this.end = this.signature.length + 1;
				break;
			default :
				this.end = index;
		}

		if (this.use15specifics || this.end != this.bracket) {
			this.start = this.end + 1; // skip ';' or '<'
		} else {
			this.start = skipAngleContents(this.end) + 1;  // skip <<>*>;
			this.bracket = -1;
		}
		return this.end;
	}
	/**
	 * Removes the generic content from this.signature. Keeps the type parameter of the method, though.
	 * <p>
	 * E.g. running the signature <code>&lt;T:Ljava/lang/Object;&gt;(TT;Ljava/lang/String;)TT;</code> through
	 * this method results in
	 * <code>&lt;T:Ljava/lang/Object;&gt;(TT;Ljava/lang/String;)TT;</code>
	 * <p>
	 * But for the signature <code>(Lp18/Klass&lt;TT;&gt;.MethodInfo&lt;Ljava/lang/String;&gt;.InnerMethodInfo&lt;Ljava/lang/String;&gt;;)V</code>
	 * it produces <code>(Lp18/Klass.MethodInfo.InnerMethodInfo;)V</code>
	 *
	 */
	private void removeTypeArguments() {
		StringBuilder buffer = new StringBuilder();
		int offset = 0;
		int index = this.start;
		if (this.signature[0] == '<') {
			index++;
		}
		for (; index < this.signature.length; index++) {
			if (this.signature[index] == '<') {
				buffer.append(this.signature, offset, index - offset);
				index = offset = skipAngleContents(index);
			}
		}
		buffer.append(this.signature, offset, index - offset);
		this.signature = new char[buffer.length()];
		buffer.getChars(0, this.signature.length, this.signature, 0);
	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=324850, do not expose generics if we shouldn't
	public int skipAngleContents(int i) {
		if (this.signature[i] != '<') {
			return i;
		}
		int depth = 0, length = this.signature.length;
		for (++i; i < length; i++) {
			switch(this.signature[i]) {
				case '<' :
					depth++;
					break;
				case '>' :
					if (--depth < 0)
						return i + 1;
					break;
			}
		}
		return i;
	}
	public int skipTypeParameter() {
		// [Annot] Identifier ClassBound {InterfaceBound}
		this.start = CharOperation.indexOf(':', this.signature, this.start);
		while (charAtStart() == ':') {
			this.start++;
			if (charAtStart() != ':') // ClassBound may be empty
				this.start = skipAngleContents(computeEnd()) + 1;
		}
		return this.start;
	}
	public char[] wordUntil(char c) {
		this.end = CharOperation.indexOf(c, this.signature, this.start);
		return CharOperation.subarray(this.signature, this.start, this.start = this.end); // skip word
	}
	public char[] nextWord() {
		this.end = CharOperation.indexOf(';', this.signature, this.start);
		if (this.bracket <= this.start) // already know it if its > start
			this.bracket = CharOperation.indexOf('<', this.signature, this.start);
		int dot = CharOperation.indexOf('.', this.signature, this.start);

		if (this.bracket > this.start && this.bracket < this.end)
			this.end = this.bracket;
		if (dot > this.start && dot < this.end)
			this.end = dot;

		return CharOperation.subarray(this.signature, this.start, this.start = this.end); // skip word
	}
	/**  similar to nextWord() but don't stop at '.' */
	public char[] nextName() {
		this.end = CharOperation.indexOf(';', this.signature, this.start);
		if (this.bracket <= this.start) // already know it if its > start
			this.bracket = CharOperation.indexOf('<', this.signature, this.start);

		if (this.bracket > this.start && this.bracket < this.end)
			this.end = this.bracket;

		return CharOperation.subarray(this.signature, this.start, this.start = this.end); // skip name
	}

	/**  answer the next type (incl. type arguments), but don't advance any cursors */
	public char[] peekFullType() {
		int s = this.start, b = this.bracket, e = this.end;
		int peekEnd = skipAngleContents(computeEnd());
		this.start = s;
		this.bracket = b;
		this.end = e;
		return CharOperation.subarray(this.signature, s, peekEnd+1);
	}

	/**
	 * assuming a previously stored start of 's' followed by a call to computeEnd()
	 * now retrieve the content between these bounds including trailing angle content
	 */
	public char[] getFrom(int s) {
		if (this.end == this.bracket) {
			this.end = skipAngleContents(this.bracket);
			this.start = this.end + 1;
		}
		return CharOperation.subarray(this.signature, s, this.end+1);
	}
	public char[] tail() {
		return CharOperation.subarray(this.signature, this.start, this.signature.length);
	}
	@Override
	public String toString() {
		if (this.start >= 0 && this.start <= this.signature.length) {
			return new String(CharOperation.subarray(this.signature, 0, this.start)) + " ^ " //$NON-NLS-1$
					+ new String(CharOperation.subarray(this.signature, this.start, this.signature.length));
		}

		return new String(this.signature) + " @ " + this.start; //$NON-NLS-1$
	}

	public char charAtStart() {
		return this.signature[this.start];
	}
}
