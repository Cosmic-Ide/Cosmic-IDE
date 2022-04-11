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
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.*;


public class JavadocMessageSend extends MessageSend {

	public int tagSourceStart, tagSourceEnd;
	public int tagValue;

	public JavadocMessageSend(char[] name, long pos) {
		this.selector = name;
		this.nameSourcePosition = pos;
		this.sourceStart = (int) (this.nameSourcePosition >>> 32);
		this.sourceEnd = (int) this.nameSourcePosition;
		this.bits |= InsideJavadoc;
	}
	public JavadocMessageSend(char[] name, long pos, JavadocArgumentExpression[] arguments) {
		this(name, pos);
		this.arguments = arguments;
	}

	/*
	 * Resolves type on a Block or Class scope.
	 */
	private TypeBinding internalResolveType(Scope scope) {
		// Answer the signature return type
		// Base type promotion
		this.constant = Constant.NotAConstant;
		if (this.receiver == null) {
			this.actualReceiverType = scope.enclosingReceiverType();
		} else if (scope.kind == Scope.CLASS_SCOPE) {
			this.actualReceiverType = this.receiver.resolveType((ClassScope) scope);
		} else {
			this.actualReceiverType = this.receiver.resolveType((BlockScope) scope);
		}

		// will check for null after args are resolved

		boolean hasArgsTypeVar = false;
		if (this.arguments != null) {
			this.argumentsHaveErrors = false; // typeChecks all arguments
			int length = this.arguments.length;
			this.argumentTypes = new TypeBinding[length];
			for (int i = 0; i < length; i++){
				Expression argument = this.arguments[i];
				if (scope.kind == Scope.CLASS_SCOPE) {
					this.argumentTypes[i] = argument.resolveType((ClassScope)scope);
				} else {
					this.argumentTypes[i] = argument.resolveType((BlockScope)scope);
				}
				if (this.argumentTypes[i] == null) {
					this.argumentsHaveErrors = true;
				} else if (!hasArgsTypeVar) {
					hasArgsTypeVar = this.argumentTypes[i].isTypeVariable();
				}
			}
			if (this.argumentsHaveErrors) {
				return null;
			}
		}

		// check receiver type
		if (this.actualReceiverType == null) {
			return null;
		}
		this.actualReceiverType = scope.environment().convertToRawType(this.receiver.resolvedType, true /*force the conversion of enclosing types*/);
		ReferenceBinding enclosingType = scope.enclosingReceiverType();
		if (enclosingType==null ? false : enclosingType.isCompatibleWith(this.actualReceiverType)) {
			this.bits |= ASTNode.SuperAccess;
		}

		// base type cannot receive any message
		if (this.actualReceiverType.isBaseType()) {
			scope.problemReporter().javadocErrorNoMethodFor(this, this.actualReceiverType, this.argumentTypes, scope.getDeclarationModifiers());
			return null;
		}
		this.binding = scope.getMethod(this.actualReceiverType, this.selector, this.argumentTypes, this);
		if (!this.binding.isValidBinding()) {
			// Try method in enclosing types
			TypeBinding enclosingTypeBinding = this.actualReceiverType;
			MethodBinding methodBinding = this.binding;
			while (!methodBinding.isValidBinding() && (enclosingTypeBinding.isMemberType() || enclosingTypeBinding.isLocalType())) {
				enclosingTypeBinding = enclosingTypeBinding.enclosingType();
				methodBinding = scope.getMethod(enclosingTypeBinding, this.selector, this.argumentTypes, this);
			}
			if (methodBinding.isValidBinding()) {
				this.binding = methodBinding;
			} else {
				// Try to search a constructor instead
				enclosingTypeBinding = this.actualReceiverType;
				MethodBinding contructorBinding = this.binding;
				if (!contructorBinding.isValidBinding() && CharOperation.equals(this.selector, enclosingTypeBinding.shortReadableName())) {
					contructorBinding = scope.getConstructor((ReferenceBinding)enclosingTypeBinding, this.argumentTypes, this);
				}
				while (!contructorBinding.isValidBinding() && (enclosingTypeBinding.isMemberType() || enclosingTypeBinding.isLocalType())) {
					enclosingTypeBinding = enclosingTypeBinding.enclosingType();
					if (CharOperation.equals(this.selector, enclosingTypeBinding.shortReadableName())) {
						contructorBinding = scope.getConstructor((ReferenceBinding)enclosingTypeBinding, this.argumentTypes, this);
					}
				}
				if (contructorBinding.isValidBinding()) {
					this.binding = contructorBinding;
				}
			}
		}
		if (!this.binding.isValidBinding()) {
			// implicit lookup may discover issues due to static/constructor contexts. javadoc must be resilient
			switch (this.binding.problemId()) {
				case ProblemReasons.NonStaticReferenceInConstructorInvocation:
				case ProblemReasons.NonStaticReferenceInStaticContext:
				case ProblemReasons.InheritedNameHidesEnclosingName :
				case ProblemReasons.Ambiguous:
					MethodBinding closestMatch = ((ProblemMethodBinding)this.binding).closestMatch;
					if (closestMatch != null) {
						this.binding = closestMatch; // ignore problem if can reach target method through it
					}
			}
		}
		if (!this.binding.isValidBinding()) {
			if (this.receiver.resolvedType instanceof ProblemReferenceBinding) {
				// problem already got signaled on receiver, do not report secondary problem
				return null;
			}
			if (this.binding.declaringClass == null) {
				if (this.actualReceiverType instanceof ReferenceBinding) {
					this.binding.declaringClass = (ReferenceBinding) this.actualReceiverType;
				} else {
					scope.problemReporter().javadocErrorNoMethodFor(this, this.actualReceiverType, this.argumentTypes, scope.getDeclarationModifiers());
					return null;
				}
			}
			scope.problemReporter().javadocInvalidMethod(this, this.binding, scope.getDeclarationModifiers());
			// record the closest match, for clients who may still need hint about possible method match
			if (this.binding instanceof ProblemMethodBinding) {
				MethodBinding closestMatch = ((ProblemMethodBinding)this.binding).closestMatch;
				if (closestMatch != null) this.binding = closestMatch;
			}
			return this.resolvedType = this.binding == null ? null : this.binding.returnType;
		} else if (hasArgsTypeVar) {
			MethodBinding problem = new ProblemMethodBinding(this.binding, this.selector, this.argumentTypes, ProblemReasons.NotFound);
			scope.problemReporter().javadocInvalidMethod(this, problem, scope.getDeclarationModifiers());
		} else if (this.binding.isVarargs()) {
			int length = this.argumentTypes.length;
			if (!(this.binding.parameters.length == length && this.argumentTypes[length-1].isArrayType())) {
				MethodBinding problem = new ProblemMethodBinding(this.binding, this.selector, this.argumentTypes, ProblemReasons.NotFound);
				scope.problemReporter().javadocInvalidMethod(this, problem, scope.getDeclarationModifiers());
			}
		} else {
			int length = this.argumentTypes.length;
			for (int i=0; i<length; i++) {
				if (TypeBinding.notEquals(this.binding.parameters[i].erasure(), this.argumentTypes[i].erasure())) {
					MethodBinding problem = new ProblemMethodBinding(this.binding, this.selector, this.argumentTypes, ProblemReasons.NotFound);
					scope.problemReporter().javadocInvalidMethod(this, problem, scope.getDeclarationModifiers());
					break;
				}
			}
		}
		if (isMethodUseDeprecated(this.binding, scope, true, this)) {
			scope.problemReporter().javadocDeprecatedMethod(this.binding, this, scope.getDeclarationModifiers());
		}

		return this.resolvedType = this.binding.returnType;
	}

	@Override
	public boolean isSuperAccess() {
		return (this.bits & ASTNode.SuperAccess) != 0;
	}

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output){

		if (this.receiver != null) {
			this.receiver.printExpression(0, output);
		}
		output.append('#').append(this.selector).append('(');
		if (this.arguments != null) {
			for (int i = 0; i < this.arguments.length ; i ++) {
				if (i > 0) output.append(", "); //$NON-NLS-1$
				this.arguments[i].printExpression(0, output);
			}
		}
		return output.append(')');
	}

	@Override
	public TypeBinding resolveType(BlockScope scope) {
		return internalResolveType(scope);
	}

	@Override
	public TypeBinding resolveType(ClassScope scope) {
		return internalResolveType(scope);
	}

	/* (non-Javadoc)
	 * Redefine to capture javadoc specific signatures
	 * @see org.eclipse.jdt.internal.compiler.ast.ASTNode#traverse(org.eclipse.jdt.internal.compiler.ASTVisitor, org.eclipse.jdt.internal.compiler.lookup.BlockScope)
	 */
	@Override
	public void traverse(ASTVisitor visitor, BlockScope blockScope) {
		if (visitor.visit(this, blockScope)) {
			if (this.receiver != null) {
				this.receiver.traverse(visitor, blockScope);
			}
			if (this.arguments != null) {
				int argumentsLength = this.arguments.length;
				for (int i = 0; i < argumentsLength; i++)
					this.arguments[i].traverse(visitor, blockScope);
			}
		}
		visitor.endVisit(this, blockScope);
	}
	/* (non-Javadoc)
	 * Redefine to capture javadoc specific signatures
	 * @see org.eclipse.jdt.internal.compiler.ast.ASTNode#traverse(org.eclipse.jdt.internal.compiler.ASTVisitor, org.eclipse.jdt.internal.compiler.lookup.BlockScope)
	 */
	@Override
	public void traverse(ASTVisitor visitor, ClassScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.receiver != null) {
				this.receiver.traverse(visitor, scope);
			}
			if (this.arguments != null) {
				int argumentsLength = this.arguments.length;
				for (int i = 0; i < argumentsLength; i++)
					this.arguments[i].traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}
}
