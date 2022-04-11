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

import org.eclipse.jdt.core.compiler.CategorizedProblem;

/**
 * An EvaluationResult is the result of a code snippet evaluation, a global
 * variable evaluation or it is used to report problems against imports and
 * package declaration.
 * It primarily contains the representation of the resulting value (e.g. its
 * toString() representation). However if the code snippet, a global variable
 * definition, an import or the package declaration could not be compiled, it
 * contains the corresponding compilation problems.
 */
public class EvaluationResult {

	static final CategorizedProblem[] NO_PROBLEMS = new CategorizedProblem[0];

	char[] evaluationID;
	int evaluationType;
	CategorizedProblem[] problems;
	char[] displayString;
	char[] typeName;

	/**
	 * The evaluation result contains the value of a variable or
	 * it reports a problem on a variable. Note that if the problem is
	 * on the type of the variable, the source line number is -1. If the
	 * problem is on the name of the variable, the source line number is 0.
	 * Otherwise the source line number is relative to the initializer code.
	 */
	public static final int T_VARIABLE = 1;

	/**
	 * The evaluation result contains the value of a code snippet or
	 * it reports a problem on a code snippet.
	 */
	public static final int T_CODE_SNIPPET = 2;

	/**
	 * The evaluation result reports a problem on an import declaration.
	 */
	public static final int T_IMPORT = 3;

	/**
	 * The evaluation result reports a problem on a package declaration.
	 */
	public static final int T_PACKAGE = 4;

	/**
	 * The evaluation result reports an internal problem.
	 */
	public static final int T_INTERNAL = 5;

public EvaluationResult(char[] evaluationID, int evaluationType, char[] displayString, char[] typeName) {
	this.evaluationID = evaluationID;
	this.evaluationType = evaluationType;
	this.displayString = displayString;
	this.typeName = typeName;
	this.problems = NO_PROBLEMS;
}
public EvaluationResult(char[] evaluationID, int evaluationType, CategorizedProblem[] problems) {
	this.evaluationID = evaluationID;
	this.evaluationType = evaluationType;
	this.problems = problems;
}
/**
 * Adds the given problem to the list of problems of this evaluation result.
 */
void addProblem(CategorizedProblem problem) {
	CategorizedProblem[] existingProblems = this.problems;
	int existingLength = existingProblems.length;
	this.problems = new CategorizedProblem[existingLength + 1];
	System.arraycopy(existingProblems, 0, this.problems, 0, existingLength);
	this.problems[existingLength] = problem;
}
/**
 * Returns the ID of the evaluation.
 * If the result is about a global variable, returns the name of the variable.
 * If the result is about a code snippet, returns the code snippet.
 * If the result is about an import, returns the import.
 * If the result is about a package declaration, returns the package declaration.
 */
public char[] getEvaluationID() {
	return this.evaluationID;
}
/**
 * Returns the type of evaluation this result is about.
 * This indicates if the result is about a global variable,
 * a code snippet, an import or a package declaration.
 * Use getEvaluationID() to get the object itself.
 */
public int getEvaluationType() {
	return this.evaluationType;
}
/**
 * Returns an array of problems (errors and warnings) encountered
 * during the compilation of a code snippet or a global variable definition,
 * or during the analysis of a package name or an import.
 * Returns an empty array if there are no problems.
 */
public CategorizedProblem[] getProblems() {
	return this.problems;
}
/**
 * Returns a proxy object on this result's value.
 * Returns null if the result's value is null.
 * The returned value is undefined if there is no result.
 * The proxy object is expected to answer questions like:
 *   - What is the proxy type for this object?
 *	 - What is the toString() representation for this object?
 *   - What are the field names of this object?
 *   - What is the value for a given field name?
 * Special proxy objects are expected if the value is a primitive type.
 */
public Object getValue() {
	return null; // Not yet implemented
}
/**
 * Returns the displayable representation of this result's value.
 * This is obtained by sending toString() to the result object on the target side
 * if it is not a primitive value. If it is a primitive value, the corresponding
 * static toString(...) is used, e.g. Integer.toString(int n) if it is an int.
 * Returns null if there is no value.
 */
public char[] getValueDisplayString() {
	return this.displayString;
}
/**
 * Returns the dot-separated fully qualified name of this result's value type.
 * If the value is a primitive value, returns the toString() representation of its type
 * (e.g. "int", "boolean", etc.)
 * Returns null if there is no value.
 */
public char[] getValueTypeName() {
	return this.typeName;
}
/**
 * Returns whether there are errors in the code snippet or the global variable definition.
 */
public boolean hasErrors() {
	if (this.problems == null) {
		return false;
	} else {
		for (int i = 0; i < this.problems.length; i++) {
			if (this.problems[i].isError()) {
				return true;
			}
		}
		return false;
	}
}
/**
 * Returns whether there are problems in the code snippet or the global variable definition.
 */
public boolean hasProblems() {
	return (this.problems != null) && (this.problems.length != 0);
}
/**
 * Returns whether this result has a value.
 */
public boolean hasValue() {
	return this.displayString != null;
}
/**
 * Returns whether there are warnings in the code snippet or the global variable definition.
 */
public boolean hasWarnings() {
	if (this.problems == null) {
		return false;
	} else {
		for (int i = 0; i < this.problems.length; i++) {
			if (this.problems[i].isWarning()) {
				return true;
			}
		}
		return false;
	}
}
/**
 * Returns a readable representation of this result.
 * This is for debugging purpose only.
 */
@Override
public String toString() {
	StringBuilder buffer = new StringBuilder();
	switch (this.evaluationType) {
		case T_CODE_SNIPPET:
			buffer.append("Code snippet"); //$NON-NLS-1$
			break;
		case T_IMPORT:
			buffer.append("Import"); //$NON-NLS-1$
			break;
		case T_INTERNAL:
			buffer.append("Internal problem"); //$NON-NLS-1$
			break;
		case T_PACKAGE:
			buffer.append("Package"); //$NON-NLS-1$
			break;
		case T_VARIABLE:
			buffer.append("Global variable"); //$NON-NLS-1$
			break;
	}
	buffer.append(": "); //$NON-NLS-1$
	buffer.append(this.evaluationID == null ? "<unknown>".toCharArray() : this.evaluationID);  //$NON-NLS-1$
	buffer.append("\n"); //$NON-NLS-1$
	if (hasProblems()) {
		buffer.append("Problems:\n"); //$NON-NLS-1$
		for (int i = 0; i < this.problems.length; i++) {
			buffer.append(this.problems[i].toString());
		}
	} else {
		if (hasValue()) {
			buffer.append("("); //$NON-NLS-1$
			buffer.append(this.typeName);
			buffer.append(") "); //$NON-NLS-1$
			buffer.append(this.displayString);
		} else {
			buffer.append("(No explicit return value)"); //$NON-NLS-1$
		}
	}
	return buffer.toString();
}
}
