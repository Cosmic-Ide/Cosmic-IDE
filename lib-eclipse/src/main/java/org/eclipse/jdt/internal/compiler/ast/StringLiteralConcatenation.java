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
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;

/**
 * Flatten string literal
 */
public class StringLiteralConcatenation extends StringLiteral {
	private static final int INITIAL_SIZE = 5;
	public Expression[] literals;
	public int counter;
	/**
	 * Build a two-strings literal
	 * */
	public StringLiteralConcatenation(StringLiteral str1, StringLiteral str2) {
		super(str1.sourceStart, str1.sourceEnd);
		this.source = str1.source;
		this.literals = new StringLiteral[INITIAL_SIZE];
		this.counter = 0;
		this.literals[this.counter++] = str1;
		extendsWith(str2);
	}

	/**
	 *  Add the lit source to mine, just as if it was mine
	 */
	@Override
	public StringLiteralConcatenation extendsWith(StringLiteral lit) {
		this.sourceEnd = lit.sourceEnd;
		final int literalsLength = this.literals.length;
		if (this.counter == literalsLength) {
			// resize
			System.arraycopy(this.literals, 0, this.literals = new StringLiteral[literalsLength + INITIAL_SIZE], 0, literalsLength);
		}
		//uddate the source
		int length = this.source.length;
		System.arraycopy(
			this.source,
			0,
			this.source = new char[length + lit.source.length],
			0,
			length);
		System.arraycopy(lit.source, 0, this.source, length, lit.source.length);
		this.literals[this.counter++] = lit;
		return this;
	}

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output) {
		output.append("StringLiteralConcatenation{"); //$NON-NLS-1$
		for (int i = 0, max = this.counter; i < max; i++) {
			this.literals[i].printExpression(indent, output);
			output.append("+\n");//$NON-NLS-1$
		}
		return output.append('}');
	}

	@Override
	public char[] source() {
		return this.source;
	}

	@Override
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			for (int i = 0, max = this.counter; i < max; i++) {
				this.literals[i].traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}
}
