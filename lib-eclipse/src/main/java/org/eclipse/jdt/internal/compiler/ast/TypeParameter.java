/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Stephan Herrmann - Contributions for
 *								bug 392384 - [1.8][compiler][null] Restore nullness info from type annotations in class files
 *								Bug 415043 - [1.8][null] Follow-up re null type annotations after bug 392099
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 415543 - [1.8][compiler] Incorrect bound index in RuntimeInvisibleTypeAnnotations attribute
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import java.util.List;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.TypeReference.AnnotationCollector;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.AnnotationContext;
import org.eclipse.jdt.internal.compiler.codegen.AnnotationTargetTypeConstants;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;

public class TypeParameter extends AbstractVariableDeclaration {

    public TypeVariableBinding binding;
	public TypeReference[] bounds;

	/**
	 * @see org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration#getKind()
	 */
	@Override
	public int getKind() {
		return TYPE_PARAMETER;
	}

	public void checkBounds(Scope scope) {

		if (this.type != null) {
			this.type.checkBounds(scope);
		}
		if (this.bounds != null) {
			for (int i = 0, length = this.bounds.length; i < length; i++) {
				this.bounds[i].checkBounds(scope);
			}
		}
	}

	public void getAllAnnotationContexts(int targetType, int typeParameterIndex, List<AnnotationContext> allAnnotationContexts) {
		AnnotationCollector collector = new AnnotationCollector(this, targetType, typeParameterIndex, allAnnotationContexts);
		if (this.annotations != null) {
			int annotationsLength = this.annotations.length;
			for (int i = 0; i < annotationsLength; i++)
				this.annotations[i].traverse(collector, (BlockScope) null);
		}
		switch(collector.targetType) {
			case AnnotationTargetTypeConstants.CLASS_TYPE_PARAMETER :
				collector.targetType = AnnotationTargetTypeConstants.CLASS_TYPE_PARAMETER_BOUND;
				break;
			case AnnotationTargetTypeConstants.METHOD_TYPE_PARAMETER :
				collector.targetType = AnnotationTargetTypeConstants.METHOD_TYPE_PARAMETER_BOUND;
		}
		int boundIndex = 0;
		if (this.type != null) {
			// boundIndex 0 is always a class
			if (this.type.resolvedType.isInterface())
				boundIndex = 1;
			if ((this.type.bits & ASTNode.HasTypeAnnotations) != 0) {
				collector.info2 = boundIndex;
				this.type.traverse(collector, (BlockScope) null);
			}
		}
		if (this.bounds != null) {
			int boundsLength = this.bounds.length;
			for (int i = 0; i < boundsLength; i++) {
				TypeReference bound = this.bounds[i];
				if ((bound.bits & ASTNode.HasTypeAnnotations) == 0) {
					continue;
				}
				collector.info2 = ++boundIndex;
				bound.traverse(collector, (BlockScope) null);
			}
		}
	}
	private void internalResolve(Scope scope, boolean staticContext) {
	    // detect variable/type name collisions
		if (this.binding != null) {
			Binding existingType = scope.parent.getBinding(this.name, Binding.TYPE, this, false/*do not resolve hidden field*/);
			if (existingType != null
					&& this.binding != existingType
					&& existingType.isValidBinding()
					&& (existingType.kind() != Binding.TYPE_PARAMETER || !staticContext)) {
				scope.problemReporter().typeHiding(this, existingType);
			}
		}
		if (this.annotations != null || scope.environment().usesNullTypeAnnotations()) {
			resolveAnnotations(scope);
		}
		if (CharOperation.equals(this.name, TypeConstants.VAR)) {
			if (scope.compilerOptions().sourceLevel < ClassFileConstants.JDK10) {
				scope.problemReporter().varIsReservedTypeNameInFuture(this);
			} else {
				scope.problemReporter().varIsNotAllowedHere(this);
			}
		}
		scope.problemReporter().validateRestrictedKeywords(this.name, this);
	}

	@Override
	public void resolve(BlockScope scope) {
		internalResolve(scope, scope.methodScope().isStatic);
	}

	public void resolve(ClassScope scope) {
		internalResolve(scope, scope.enclosingSourceType().isStatic());
	}

	public void resolveAnnotations(Scope scope) {
		BlockScope resolutionScope = Scope.typeAnnotationsResolutionScope(scope);
		if (resolutionScope != null) {
			AnnotationBinding [] annotationBindings = resolveAnnotations(resolutionScope, this.annotations, this.binding, false);
			LookupEnvironment environment = scope.environment();
			boolean isAnnotationBasedNullAnalysisEnabled = environment.globalOptions.isAnnotationBasedNullAnalysisEnabled;
			if (annotationBindings != null && annotationBindings.length > 0) {
				this.binding.setTypeAnnotations(annotationBindings, isAnnotationBasedNullAnalysisEnabled);
				scope.referenceCompilationUnit().compilationResult.hasAnnotations = true;
			}
			if (isAnnotationBasedNullAnalysisEnabled) {
				if (this.binding != null && this.binding.isValidBinding()) {
					if (!this.binding.hasNullTypeAnnotations()
							&& scope.hasDefaultNullnessFor(Binding.DefaultLocationTypeParameter, this.sourceStart())) {
						AnnotationBinding[] annots = new AnnotationBinding[] { environment.getNonNullAnnotation() };
						TypeVariableBinding previousBinding = this.binding;
						this.binding = (TypeVariableBinding) environment.createAnnotatedType(this.binding, annots);

						if (scope instanceof MethodScope) {
							/*
							 * for method type parameters, references to the bindings have already been copied into
							 * MethodBinding.typeVariables - update them.
							 */
							MethodScope methodScope = (MethodScope) scope;
							if (methodScope.referenceContext instanceof AbstractMethodDeclaration) {
								MethodBinding methodBinding = ((AbstractMethodDeclaration) methodScope.referenceContext).binding;
								if (methodBinding != null) {
									methodBinding.updateTypeVariableBinding(previousBinding, this.binding);
								}
							}
						}
					}
					this.binding.evaluateNullAnnotations(scope, this);
				}
			}
		}
	}

	@Override
	public StringBuffer printStatement(int indent, StringBuffer output) {
		if (this.annotations != null) {
			printAnnotations(this.annotations, output);
			output.append(' ');
		}
		output.append(this.name);
		if (this.type != null) {
			output.append(" extends "); //$NON-NLS-1$
			this.type.print(0, output);
		}
		if (this.bounds != null){
			for (int i = 0; i < this.bounds.length; i++) {
				output.append(" & "); //$NON-NLS-1$
				this.bounds[i].print(0, output);
			}
		}
		return output;
	}

	@Override
	public void generateCode(BlockScope currentScope, CodeStream codeStream) {
	    // nothing to do
	}

	@Override
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.annotations != null) {
				int annotationsLength = this.annotations.length;
				for (int i = 0; i < annotationsLength; i++)
					this.annotations[i].traverse(visitor, scope);
			}
			if (this.type != null) {
				this.type.traverse(visitor, scope);
			}
			if (this.bounds != null) {
				int boundsLength = this.bounds.length;
				for (int i = 0; i < boundsLength; i++) {
					this.bounds[i].traverse(visitor, scope);
				}
			}
		}
		visitor.endVisit(this, scope);
	}

	public void traverse(ASTVisitor visitor, ClassScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.annotations != null) {
				int annotationsLength = this.annotations.length;
				for (int i = 0; i < annotationsLength; i++)
					this.annotations[i].traverse(visitor, scope);
			}
			if (this.type != null) {
				this.type.traverse(visitor, scope);
			}
			if (this.bounds != null) {
				int boundsLength = this.bounds.length;
				for (int i = 0; i < boundsLength; i++) {
					this.bounds[i].traverse(visitor, scope);
				}
			}
		}
		visitor.endVisit(this, scope);
	}
}
