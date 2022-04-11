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
 *Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Herrmann - Contribution for
 *								Bug 429813 - [1.8][dom ast] IMethodBinding#getJavaElement() should return IMethod for lambda
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.parser.*;

public class Initializer extends FieldDeclaration {

	public Block block;
	public int lastVisibleFieldID;
	public int bodyStart;
	public int bodyEnd;

	private MethodBinding methodBinding;

	public Initializer(Block block, int modifiers) {
		this.block = block;
		this.modifiers = modifiers;

		if (block != null) {
			this.declarationSourceStart = this.sourceStart = block.sourceStart;
		}
	}

	@Override
	public FlowInfo analyseCode(
		MethodScope currentScope,
		FlowContext flowContext,
		FlowInfo flowInfo) {

		if (this.block != null) {
			return this.block.analyseCode(currentScope, flowContext, flowInfo);
		}
		return flowInfo;
	}

	/**
	 * Code generation for a non-static initializer:
	 *    standard block code gen
	 *
	 * @param currentScope org.eclipse.jdt.internal.compiler.lookup.BlockScope
	 * @param codeStream org.eclipse.jdt.internal.compiler.codegen.CodeStream
	 */
	@Override
	public void generateCode(BlockScope currentScope, CodeStream codeStream) {

		if ((this.bits & IsReachable) == 0) {
			return;
		}
		int pc = codeStream.position;
		if (this.block != null) this.block.generateCode(currentScope, codeStream);
		codeStream.recordPositionsFrom(pc, this.sourceStart);
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration#getKind()
	 */
	@Override
	public int getKind() {
		return INITIALIZER;
	}

	@Override
	public boolean isStatic() {

		return (this.modifiers & ClassFileConstants.AccStatic) != 0;
	}

	public void parseStatements(
		Parser parser,
		TypeDeclaration typeDeclaration,
		CompilationUnitDeclaration unit) {

		//fill up the method body with statement
		parser.parse(this, typeDeclaration, unit);
	}

	@Override
	public StringBuffer printStatement(int indent, StringBuffer output) {

		if (this.modifiers != 0) {
			printIndent(indent, output);
			printModifiers(this.modifiers, output);
			if (this.annotations != null) {
				printAnnotations(this.annotations, output);
				output.append(' ');
			}
			output.append("{\n"); //$NON-NLS-1$
			if (this.block != null) {
				this.block.printBody(indent, output);
			}
			printIndent(indent, output).append('}');
			return output;
		} else if (this.block != null) {
			this.block.printStatement(indent, output);
		} else {
			printIndent(indent, output).append("{}"); //$NON-NLS-1$
		}
		return output;
	}

	@Override
	public void resolve(MethodScope scope) {

		FieldBinding previousField = scope.initializedField;
		int previousFieldID = scope.lastVisibleFieldID;
		try {
			scope.initializedField = null;
			scope.lastVisibleFieldID = this.lastVisibleFieldID;
			if (isStatic()) {
				ReferenceBinding declaringType = scope.enclosingSourceType();
				if (scope.compilerOptions().sourceLevel < ClassFileConstants.JDK16) {
					if (declaringType.isNestedType() && !declaringType.isStatic())
						scope.problemReporter().innerTypesCannotDeclareStaticInitializers(
							declaringType,
							this);
				}
			}
			if (this.block != null) this.block.resolve(scope);
		} finally {
		    scope.initializedField = previousField;
			scope.lastVisibleFieldID = previousFieldID;
		}
	}

	/** Method used only by DOM to support bindings of initializers. */
	public MethodBinding getMethodBinding() {
		if (this.methodBinding == null) {
			Scope scope = this.block.scope;
			this.methodBinding = isStatic()
					? new MethodBinding(ClassFileConstants.AccStatic, CharOperation.NO_CHAR, TypeBinding.VOID, Binding.NO_PARAMETERS, Binding.NO_EXCEPTIONS, scope.enclosingSourceType())
					: new MethodBinding(0, CharOperation.NO_CHAR, TypeBinding.VOID, Binding.NO_PARAMETERS, Binding.NO_EXCEPTIONS, scope.enclosingSourceType());
		}
		return this.methodBinding;
	}

	@Override
	public void traverse(ASTVisitor visitor, MethodScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.block != null) this.block.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}
}
