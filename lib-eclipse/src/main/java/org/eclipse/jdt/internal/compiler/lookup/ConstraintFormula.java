/*******************************************************************************
 * Copyright (c) 2013, 2015 GK Software AG.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Contributions for
 *     						Bug 473178
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of 18.1.2 in JLS8
 */
abstract class ConstraintFormula extends ReductionResult {

	static final List<InferenceVariable> EMPTY_VARIABLE_LIST = Collections.emptyList();
	static final ConstraintFormula[] NO_CONSTRAINTS = new ConstraintTypeFormula[0];

	// constants for unicode debug output from ASCII source files:
	static final char LEFT_ANGLE_BRACKET = '\u27E8';
	static final char RIGHT_ANGLE_BRACKET = '\u27E9';

	public abstract Object reduce(InferenceContext18 inferenceContext) throws InferenceFailureException;

	Collection<InferenceVariable> inputVariables(InferenceContext18 context) {
		return EMPTY_VARIABLE_LIST;
	}

	Collection<InferenceVariable> outputVariables(InferenceContext18 context) {
		Set<InferenceVariable> variables = new LinkedHashSet<>();
		this.right.collectInferenceVariables(variables);
		if (!variables.isEmpty())
			variables.removeAll(inputVariables(context));
		return variables;
	}

	public boolean applySubstitution(BoundSet solutionSet, InferenceVariable[] variables) {
		for (int i=0; i<variables.length; i++) {
			InferenceVariable variable = variables[i];
			TypeBinding instantiation = solutionSet.getInstantiation(variables[i], null);
			if (instantiation == null)
				return false;
			this.right = this.right.substituteInferenceVariable(variable, instantiation);
		}
		return true;
	}

	// for debug toString():
	protected void appendTypeName(StringBuffer buf, TypeBinding type) {
		if (type instanceof CaptureBinding18)
			buf.append(type.toString()); // contains more info than readable name
		else
			buf.append(type.readableName());
	}
}
