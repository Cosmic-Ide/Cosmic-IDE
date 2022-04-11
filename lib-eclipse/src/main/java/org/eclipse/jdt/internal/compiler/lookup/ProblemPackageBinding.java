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
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

public class ProblemPackageBinding extends PlainPackageBinding {
	private int problemId;
// NOTE: must only answer the subset of the name related to the problem

ProblemPackageBinding(char[][] compoundName, int problemId, LookupEnvironment environment) {
	super(compoundName, environment);
	this.problemId = problemId;
}
ProblemPackageBinding(char[] name, int problemId, LookupEnvironment environment) {
	this(new char[][] {name}, problemId, environment);
}
/**
 * API
 * Answer the problem id associated with the receiver.
 * NoError if the receiver is a valid binding.
 */
@Override
public final int problemId() {
	return this.problemId;
}
}
