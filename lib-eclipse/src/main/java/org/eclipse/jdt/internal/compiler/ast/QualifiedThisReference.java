/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *								bug 382350 - [1.8][compiler] Unable to invoke inherited default method via I.super.m() syntax
 *								bug 404649 - [1.8][compiler] detect illegal reference to indirect or redundant super
 *     Jesper S Moller <jesper@selskabet.org> - Contributions for
 *								bug 378674 - "The method can be declared as static" is wrong
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class QualifiedThisReference extends ThisReference {

	public TypeReference qualification;
	ReferenceBinding currentCompatibleType;

	public QualifiedThisReference(TypeReference name, int sourceStart, int sourceEnd) {
		super(sourceStart, sourceEnd);
		this.qualification = name;
		name.bits |= IgnoreRawTypeCheck; // no need to worry about raw type usage
		this.sourceStart = name.sourceStart;
	}

	@Override
	public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {

		return flowInfo;
	}

	@Override
	public FlowInfo analyseCode(
		BlockScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo,
		boolean valueRequired) {

		return flowInfo;
	}

	/**
	 * Code generation for QualifiedThisReference
	 *
	 * @param currentScope org.eclipse.jdt.internal.compiler.lookup.BlockScope
	 * @param codeStream org.eclipse.jdt.internal.compiler.codegen.CodeStream
	 * @param valueRequired boolean
	 */
	@Override
	public void generateCode(
		BlockScope currentScope,
		CodeStream codeStream,
		boolean valueRequired) {

		int pc = codeStream.position;
		if (valueRequired) {
			if ((this.bits & DepthMASK) != 0) {
				Object[] emulationPath =
					currentScope.getEmulationPath(this.currentCompatibleType, true /*only exact match*/, false/*consider enclosing arg*/);
				codeStream.generateOuterAccess(emulationPath, this, this.currentCompatibleType, currentScope);
			} else {
				// nothing particular after all
				codeStream.aload_0();
			}
		}
		codeStream.recordPositionsFrom(pc, this.sourceStart);
	}

	@Override
	public TypeBinding resolveType(BlockScope scope) {

		this.constant = Constant.NotAConstant;
		// X.this is not a param/raw type as denoting enclosing instance
		TypeBinding type = this.qualification.resolveType(scope, true /* check bounds*/);
		if (type == null || !type.isValidBinding()) return null;
		// X.this is not a param/raw type as denoting enclosing instance
		type = type.erasure();

		// resolvedType needs to be converted to parameterized
		if (type instanceof ReferenceBinding) {
			this.resolvedType = scope.environment().convertToParameterizedType((ReferenceBinding) type);
		} else {
			// error case
			this.resolvedType = type;
		}

		// the qualification MUST exactly match some enclosing type name
		// It is possible to qualify 'this' by the name of the current class
		int depth = findCompatibleEnclosing(scope.referenceType().binding, type, scope);
		this.bits &= ~DepthMASK; // flush previous depth if any
		this.bits |= (depth & 0xFF) << DepthSHIFT; // encoded depth into 8 bits

		if (this.currentCompatibleType == null) {
			if (this.resolvedType.isValidBinding())
				scope.problemReporter().noSuchEnclosingInstance(type, this, false);
			// otherwise problem will be reported by the caller
			return this.resolvedType;
		} else {
			scope.tagAsAccessingEnclosingInstanceStateOf(this.currentCompatibleType, false /* type variable access */);
		}

		// Ensure one cannot write code like: B() { super(B.this); }
		if (depth == 0) {
			checkAccess(scope, null);
		} // if depth>0, path emulation will diagnose bad scenarii
		else if (scope.compilerOptions().complianceLevel >= ClassFileConstants.JDK16) {
			MethodScope ms = scope.methodScope();
			if (ms.isStatic)
				ms.problemReporter().errorThisSuperInStatic(this);
		}
		MethodScope methodScope = scope.namedMethodScope();
		if (methodScope != null) {
			MethodBinding method = methodScope.referenceMethodBinding();
			if (method != null) {
				TypeBinding receiver = method.receiver;
				while (receiver != null) {
					if (TypeBinding.equalsEquals(receiver, this.resolvedType))
						return this.resolvedType = receiver;
					receiver = receiver.enclosingType();
				}
			}
		}
		return this.resolvedType;
	}

	int findCompatibleEnclosing(ReferenceBinding enclosingType, TypeBinding type, BlockScope scope) {
		int depth = 0;
		this.currentCompatibleType = enclosingType;
		while (this.currentCompatibleType != null && TypeBinding.notEquals(this.currentCompatibleType, type)) {
			depth++;
			this.currentCompatibleType = this.currentCompatibleType.isStatic() ? null : this.currentCompatibleType.enclosingType();
		}
		return depth;
	}

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output) {

		return this.qualification.print(0, output).append(".this"); //$NON-NLS-1$
	}

	@Override
	public void traverse(
		ASTVisitor visitor,
		BlockScope blockScope) {

		if (visitor.visit(this, blockScope)) {
			this.qualification.traverse(visitor, blockScope);
		}
		visitor.endVisit(this, blockScope);
	}

	@Override
	public void traverse(
			ASTVisitor visitor,
			ClassScope blockScope) {

		if (visitor.visit(this, blockScope)) {
			this.qualification.traverse(visitor, blockScope);
		}
		visitor.endVisit(this, blockScope);
	}
}
