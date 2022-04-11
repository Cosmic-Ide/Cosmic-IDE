/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.jdt.internal.eval;

/**
 * A global variable is a variable defined for an evaluation context and that persists
 * accross evaluations.
 */
public class GlobalVariable {
	char[] typeName;
	char[] name;
	char[] initializer;
	int declarationStart = -1, initializerStart = -1, initExpressionStart; // positions in the global variable class definition
	int initializerLineStart = -1; // line in the global variable class definition
/**
 * Creates a new global variable with the given type name, name and initializer.
 * initializer can be null if there is none.
 */
public GlobalVariable(char[] typeName, char[] name, char[] initializer) {
	this.typeName = typeName;
	this.name = name;
	this.initializer = initializer;
}
/**
 * Returns the initializer of this global variable. The initializer is a
 * variable initializer (i.e. an expression or an array initializer) as defined
 * in the Java Language Specifications.
 */
public char[] getInitializer() {
	return this.initializer;
}
/**
 * Returns the name of this global variable.
 */
public char[] getName() {
	return this.name;
}
/**
 * Returns the dot separated fully qualified name of the type of this global variable,
 * or its simple representation if it is a primitive type (e.g. int, boolean, etc.)
 */
public char[] getTypeName() {
	return this.typeName;
}
/**
 * Returns a readable representation of the receiver.
 * This is for debugging purpose only.
 */
@Override
public String toString() {
	StringBuilder buffer = new StringBuilder();
	buffer.append(this.typeName);
	buffer.append(" "); //$NON-NLS-1$
	buffer.append(this.name);
	if (this.initializer != null) {
		buffer.append("= "); //$NON-NLS-1$
		buffer.append(this.initializer);
	}
	buffer.append(";"); //$NON-NLS-1$
	return buffer.toString();
}
}
