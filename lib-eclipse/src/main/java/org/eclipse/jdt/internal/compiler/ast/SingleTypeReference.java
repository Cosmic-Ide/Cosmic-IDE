/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
 *								Bug 392238 - [1.8][compiler][null] Detect semantically invalid null type annotations
 *								Bug 429958 - [1.8][null] evaluate new DefaultLocation attribute of @NonNullByDefault
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;

public class SingleTypeReference extends TypeReference {

	public char[] token;

	public SingleTypeReference(char[] source, long pos) {

			this.token = source;
			this.sourceStart = (int) (pos>>>32)  ;
			this.sourceEnd = (int) (pos & 0x00000000FFFFFFFFL) ;

	}

	@Override
	public TypeReference augmentTypeWithAdditionalDimensions(int additionalDimensions, Annotation[][] additionalAnnotations, boolean isVarargs) {
		int totalDimensions = this.dimensions() + additionalDimensions;
		Annotation [][] allAnnotations = getMergedAnnotationsOnDimensions(additionalDimensions, additionalAnnotations);
		ArrayTypeReference arrayTypeReference = new ArrayTypeReference(this.token, totalDimensions, allAnnotations, (((long) this.sourceStart) << 32) + this.sourceEnd);
		arrayTypeReference.annotations = this.annotations;
		arrayTypeReference.bits |= (this.bits & ASTNode.HasTypeAnnotations);
		if (!isVarargs)
			arrayTypeReference.extendedDimensions = additionalDimensions;
		return arrayTypeReference;
	}

	@Override
	public char[] getLastToken() {
		return this.token;
	}
	@Override
	protected TypeBinding getTypeBinding(Scope scope) {
		if (this.resolvedType != null)
			return this.resolvedType;

		this.resolvedType = scope.getType(this.token);

		if (this.resolvedType instanceof TypeVariableBinding) {
			TypeVariableBinding typeVariable = (TypeVariableBinding) this.resolvedType;
			if (typeVariable.declaringElement instanceof SourceTypeBinding) {
				scope.tagAsAccessingEnclosingInstanceStateOf((ReferenceBinding) typeVariable.declaringElement, true /* type variable access */);
			}
		} else if (this.resolvedType instanceof LocalTypeBinding) {
			LocalTypeBinding localType = (LocalTypeBinding) this.resolvedType;
			MethodScope methodScope = scope.methodScope();
			if (methodScope != null && !methodScope.isStatic) {
				methodScope.tagAsAccessingEnclosingInstanceStateOf(localType, false /* ! type variable access */);
			}
		}

		if (scope.kind == Scope.CLASS_SCOPE && this.resolvedType.isValidBinding())
			if (((ClassScope) scope).detectHierarchyCycle(this.resolvedType, this))
				return null;
		return this.resolvedType;
	}

	@Override
	public char [][] getTypeName() {
		return new char[][] { this.token };
	}

	@Override
	public boolean isBaseTypeReference() {
		return this.token == BYTE    ||
			   this.token == SHORT   ||
			   this.token == INT     ||
			   this.token == LONG    ||
			   this.token == FLOAT   ||
			   this.token == DOUBLE  ||
			   this.token == CHAR    ||
			   this.token == BOOLEAN ||
			   this.token == NULL    ||
			   this.token == VOID;
	}

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output){
		if (this.annotations != null && this.annotations[0] != null) {
			printAnnotations(this.annotations[0], output);
			output.append(' ');
		}
		return output.append(this.token);
	}

	public TypeBinding resolveTypeEnclosing(BlockScope scope, ReferenceBinding enclosingType) {
		this.resolvedType = scope.getMemberType(this.token, enclosingType);
		boolean hasError = false;
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=391500
		resolveAnnotations(scope, 0); // defaultNullness not relevant, the only caller within the compiler: QAE
		TypeBinding memberType = this.resolvedType; // load after possible update in resolveAnnotations()
		if (!memberType.isValidBinding()) {
			hasError = true;
			scope.problemReporter().invalidEnclosingType(this, memberType, enclosingType);
			memberType = ((ReferenceBinding)memberType).closestMatch();
			if (memberType == null) {
				return null;
			}
		}
		if (isTypeUseDeprecated(memberType, scope))
			reportDeprecatedType(memberType, scope);
		memberType = scope.environment().convertToRawType(memberType, false /*do not force conversion of enclosing types*/);
		if (memberType.isRawType()
				&& (this.bits & IgnoreRawTypeCheck) == 0
				&& scope.compilerOptions().getSeverity(CompilerOptions.RawTypeReference) != ProblemSeverities.Ignore){
			scope.problemReporter().rawTypeReference(this, memberType);
		}
		if (hasError) {
			// do not store the computed type, keep the problem type instead
			return memberType;
		}
		return this.resolvedType = memberType;
	}

	@Override
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.annotations != null) {
				Annotation [] typeAnnotations = this.annotations[0];
				for (int i = 0, length = typeAnnotations == null ? 0 : typeAnnotations.length; i < length; i++)
					typeAnnotations[i].traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}

	@Override
	public void traverse(ASTVisitor visitor, ClassScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.annotations != null) {
				Annotation [] typeAnnotations = this.annotations[0];
				for (int i = 0, length = typeAnnotations == null ? 0 : typeAnnotations.length; i < length; i++)
					typeAnnotations[i].traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}
}
