/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.flow.FlowContext;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.flow.InitializationFlowContext;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.parser.Parser;

public class CompactConstructorDeclaration extends ConstructorDeclaration {

	public TypeDeclaration recordDeclaration;

	public CompactConstructorDeclaration(CompilationResult compilationResult) {
		super(compilationResult);
	}
	@Override
	public void parseStatements(Parser parser, CompilationUnitDeclaration unit) {
		this.constructorCall = SuperReference.implicitSuperConstructorCall();
		parser.parse(this, unit, false);
		this.containsSwitchWithTry = parser.switchWithTry;
	}
	@Override
	public void analyseCode(ClassScope classScope, InitializationFlowContext initializerFlowContext, FlowInfo flowInfo, int initialReachMode) {
		try {
			this.scope.isCompactConstructorScope = true;
			super.analyseCode(classScope, initializerFlowContext, flowInfo, initialReachMode);
		} finally {
			this.scope.isCompactConstructorScope = false;
		}
	}
	@Override
	protected void doFieldReachAnalysis(FlowInfo flowInfo, FieldBinding[] fields) {
		// do nothing
	}
	@Override
	protected void checkAndGenerateFieldAssignment(FlowContext flowContext, FlowInfo flowInfo, FieldBinding[] fields) {
		this.scope.isCompactConstructorScope = false;
		if (fields == null)
			return;
		/* JLS 15 Record addendum Sec 8.10.4 All fields corresponding to the record components of the
		 * record class are implicitly initialized to the value of the corresponding formal
		 * parameter after the body of the compact constructor.
		 * These fields are implicitly initialized in the order that they are declared
		 * in the record component list.
		 */
		List<Statement> fieldAssignments = new ArrayList<>();
		for (FieldBinding field : fields) {
			if (field.isStatic())
				continue;
			assert field.isFinal();

			FieldReference lhs = new FieldReference(field.name,0);
			lhs.receiver = new ThisReference(0, 0);
			//TODO: Check whether anything has to be done for null analysis.
			Assignment assignment = new Assignment(lhs, new SingleNameReference(field.name, 0), 0);
			assignment.resolveType(this.scope);
			assignment.analyseCode(this.scope, flowContext, flowInfo);
			assignment.bits |= ASTNode.IsImplicit;
			assert flowInfo.isDefinitelyAssigned(field);
			fieldAssignments.add(assignment);
		}
		if (fieldAssignments.isEmpty())
			return;

		Statement[] fa = fieldAssignments.toArray(new Statement[0]);
		if (this.statements == null) {
			this.statements = fa;
			return;
		}
		int len = this.statements.length;
		int fLen = fa.length;
		Statement[] stmts = new Statement[len + fLen];
		System.arraycopy(this.statements, 0, stmts, 0, len);
		System.arraycopy(fa, 0,	stmts, len, fLen);
		this.statements = stmts;
	}
}