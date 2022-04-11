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

public abstract class  MagicLiteral extends Literal {

	public MagicLiteral(int start , int end) {

		super(start,end);
	}

	@Override
	public boolean isValidJavaStatement(){

		return false ;
	}

	@Override
	public char[] source() {

		return null;
	}
}
