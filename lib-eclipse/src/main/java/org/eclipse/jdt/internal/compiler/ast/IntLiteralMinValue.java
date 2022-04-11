/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import org.eclipse.jdt.internal.compiler.impl.*;

public class IntLiteralMinValue extends IntLiteral {

	final static char[] CharValue = new char[]{'-','2','1','4','7','4','8','3','6','4','8'};

public IntLiteralMinValue(char[] token, char[] reducedToken, int start, int end) {
	super(token, reducedToken, start, end, Integer.MIN_VALUE, IntConstant.fromValue(Integer.MIN_VALUE));
}

@Override
public void computeConstant(){
	/*precomputed at creation time*/ }
}
