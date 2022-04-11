/*******************************************************************************
 * Copyright (c) 2013 GK Software AG.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

/**
 * Generalization over TypeBounds and ConstraintFormulas which both
 * can be created during reduction.
 */
public abstract class ReductionResult {

	protected static final ConstraintTypeFormula TRUE = new ConstraintTypeFormula() {
		/* empty body just to make abstract class instantiable */
		@Override
		public Object reduce(InferenceContext18 context) { return this; }
		@Override
		public String toString() { return "TRUE"; } //$NON-NLS-1$
	};
	protected static final ConstraintTypeFormula FALSE = new ConstraintTypeFormula() {
		/* empty body just to make abstract class instantiable */
		@Override
		public Object reduce(InferenceContext18 context) { return this; }
		@Override
		public String toString() { return "FALSE"; } //$NON-NLS-1$
	};

	// Relation kinds, mimic an enum:
	protected static final int COMPATIBLE = 1;
	protected static final int SUBTYPE = 2;
	protected static final int SUPERTYPE = 3;
	protected static final int SAME = 4;
	protected static final int TYPE_ARGUMENT_CONTAINED = 5;
	protected static final int CAPTURE = 6;
	static final int EXCEPTIONS_CONTAINED = 7;
	protected static final int POTENTIALLY_COMPATIBLE = 8;

	protected TypeBinding right; // note that the LHS differs between sub-classes.
	protected int relation;

	protected static String relationToString(int relation) {
		switch (relation) {
			case SAME: 			return " = "; //$NON-NLS-1$
			case COMPATIBLE: 	return " \u2192 "; //$NON-NLS-1$
			case POTENTIALLY_COMPATIBLE: return " \u2192? "; //$NON-NLS-1$
			case SUBTYPE: 		return " <: "; //$NON-NLS-1$
			case SUPERTYPE: 	return " :> "; //$NON-NLS-1$
			case TYPE_ARGUMENT_CONTAINED:
								return " <= "; //$NON-NLS-1$
			case CAPTURE:
								return " captureOf "; //$NON-NLS-1$
			default:
				throw new IllegalArgumentException("Unknown type relation "+relation); //$NON-NLS-1$
		}
	}
}
