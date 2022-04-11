/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
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
 *     Jesper S Moller - Contributions for
 *							bug 382701 - [1.8][compiler] Implement semantic analysis of Lambda expressions & Reference expression
 *							Bug 405066 - [1.8][compiler][codegen] Implement code generation infrastructure for JSR335
 *     Stephan Herrmann - Contribution for
 *							Bug 400874 - [1.8][compiler] Inference infrastructure should evolve to meet JLS8 18.x (Part G of JSR335 spec)
 *							Bug 423504 - [1.8] Implement "18.5.3 Functional Interface Parameterization Inference"
 *							Bug 425142 - [1.8][compiler] NPE in ConstraintTypeFormula.reduceSubType
 *							Bug 425153 - [1.8] Having wildcard allows incompatible types in a lambda expression
 *							Bug 425156 - [1.8] Lambda as an argument is flagged with incompatible error
 *							Bug 424403 - [1.8][compiler] Generic method call with method reference argument fails to resolve properly.
 *							Bug 427438 - [1.8][compiler] NPE at org.eclipse.jdt.internal.compiler.ast.ConditionalExpression.generateCode(ConditionalExpression.java:280)
 *							Bug 428352 - [1.8][compiler] Resolution errors don't always surface
 *							Bug 446442 - [1.8] merge null annotations from super methods
 *     Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 405104 - [1.8][compiler][codegen] Implement support for serializeable lambdas
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import static org.eclipse.jdt.internal.compiler.ast.ExpressionContext.VANILLA_CONTEXT;

import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.IntersectionTypeBinding18;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodVerifier;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.RawTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBindingVisitor;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;

public abstract class FunctionalExpression extends Expression {

	protected TypeBinding expectedType;
	public MethodBinding descriptor;
	public MethodBinding binding;                 // Code generation binding. May include synthetics. See getMethodBinding()
	protected MethodBinding actualMethodBinding;  // void of synthetics.
	boolean ignoreFurtherInvestigation;
	protected ExpressionContext expressionContext = VANILLA_CONTEXT;
	public CompilationResult compilationResult;
	public BlockScope enclosingScope;
	public int bootstrapMethodNumber = -1;
	public boolean shouldCaptureInstance = false; // Whether the expression needs access to instance data of enclosing type
	protected static IErrorHandlingPolicy silentErrorHandlingPolicy = DefaultErrorHandlingPolicies.ignoreAllProblems();
	private boolean hasReportedSamProblem = false;
	public boolean hasDescripterProblem;
	public boolean isSerializable;
	public int ordinal;

	public FunctionalExpression(CompilationResult compilationResult) {
		this.compilationResult = compilationResult;
	}

	public FunctionalExpression() {
		super();
	}

	@Override
	public boolean isBoxingCompatibleWith(TypeBinding targetType, Scope scope) {
		return false;
	}

	public void setCompilationResult(CompilationResult compilationResult) {
		this.compilationResult = compilationResult;
	}

	// Return the actual (non-code generation) method binding that is void of synthetics.
	public MethodBinding getMethodBinding() {
		return null;
	}

	@Override
	public void setExpectedType(TypeBinding expectedType) {
		this.expectedType = expectedType;
	}

	@Override
	public void setExpressionContext(ExpressionContext context) {
		this.expressionContext = context;
	}

	@Override
	public ExpressionContext getExpressionContext() {
		return this.expressionContext;
	}

	@Override
	public boolean isPolyExpression(MethodBinding candidate) {
		return true;
	}
	@Override
	public boolean isPolyExpression() {
		return true; // always as per introduction of part D, JSR 335
	}

	@Override
	public boolean isFunctionalType() {
		return true;
	}

	@Override
	public boolean isPertinentToApplicability(TypeBinding targetType, MethodBinding method) {
		if (targetType instanceof TypeVariableBinding) {
			TypeVariableBinding typeVariable = (TypeVariableBinding) targetType;
			if (method != null) { // when called from type inference
				if (typeVariable.declaringElement == method)
					return false;
				if (method.isConstructor() && typeVariable.declaringElement == method.declaringClass)
					return false;
			} else { // for internal calls
				if (typeVariable.declaringElement instanceof MethodBinding)
					return false;
			}
		}
		return true;
	}

	@Override
	public TypeBinding invocationTargetType() {
		if (this.expectedType == null) return null;
		// when during inference this expression mimics as an invocationSite,
		// we simulate an *invocation* of this functional expression,
		// where the expected type of the expression is the return type of the sam:
		MethodBinding sam = this.expectedType.getSingleAbstractMethod(this.enclosingScope, true);
		if (sam != null && sam.problemId() != ProblemReasons.NoSuchSingleAbstractMethod) {
			if (sam.isConstructor())
				return sam.declaringClass;
			else
				return sam.returnType;
		}
		return null;
	}

	@Override
	public TypeBinding expectedType() {
		return this.expectedType;
	}

	public boolean argumentsTypeElided() { return true; /* only exception: lambda with explicit argument types. */ }

	// Notify the compilation unit that it contains some functional types, taking care not to add any transient copies. this is assumed not to be a copy
	public int recordFunctionalType(Scope scope) {
		while (scope != null) {
			switch (scope.kind) {
				case Scope.METHOD_SCOPE :
					ReferenceContext context = ((MethodScope) scope).referenceContext;
					if (context instanceof LambdaExpression) {
						LambdaExpression expression = (LambdaExpression) context;
						if (expression != expression.original) // fake universe.
							return 0;
					}
					break;
				case Scope.COMPILATION_UNIT_SCOPE :
					CompilationUnitDeclaration unit = ((CompilationUnitScope) scope).referenceContext;
					return unit.record(this);
			}
			scope = scope.parent;
		}
		return 0; // not reached.
	}

	@Override
	public TypeBinding resolveType(BlockScope blockScope) {
		return resolveType(blockScope, false);
	}

	public TypeBinding resolveType(BlockScope blockScope, boolean skipKosherCheck) {
		this.constant = Constant.NotAConstant;
		this.enclosingScope = blockScope;
		MethodBinding sam = this.expectedType == null ? null : this.expectedType.getSingleAbstractMethod(blockScope, argumentsTypeElided());
		if (sam == null) {
			blockScope.problemReporter().targetTypeIsNotAFunctionalInterface(this);
			return null;
		}
		if (!sam.isValidBinding() && sam.problemId() != ProblemReasons.ContradictoryNullAnnotations) {
			return reportSamProblem(blockScope, sam);
		}

		this.descriptor = sam;
		if (skipKosherCheck || kosherDescriptor(blockScope, sam, true)) {
			if (this.expectedType instanceof IntersectionTypeBinding18) {
				ReferenceBinding[] intersectingTypes =  ((IntersectionTypeBinding18)this.expectedType).intersectingTypes;
				for (int t = 0, max = intersectingTypes.length; t < max; t++) {
					if (intersectingTypes[t].findSuperTypeOriginatingFrom(TypeIds.T_JavaIoSerializable, false /*Serializable is not a class*/) != null) {
						this.isSerializable = true;
						break;
					}
				}
			} else if (this.expectedType.findSuperTypeOriginatingFrom(TypeIds.T_JavaIoSerializable, false /*Serializable is not a class*/) != null) {
				this.isSerializable = true;
			}
			LookupEnvironment environment = blockScope.environment();
			if (environment.globalOptions.isAnnotationBasedNullAnalysisEnabled) {
				NullAnnotationMatching.checkForContradictions(sam, this, blockScope);
			}
			return this.resolvedType = this.expectedType;
		}

		return this.resolvedType = null;
	}

	protected TypeBinding reportSamProblem(BlockScope blockScope, MethodBinding sam) {
		if (this.hasReportedSamProblem)
			return null;
		switch (sam.problemId()) {
			case ProblemReasons.NoSuchSingleAbstractMethod:
				blockScope.problemReporter().targetTypeIsNotAFunctionalInterface(this);
				this.hasReportedSamProblem = true;
				break;
			case ProblemReasons.NotAWellFormedParameterizedType:
				blockScope.problemReporter().illFormedParameterizationOfFunctionalInterface(this);
				this.hasReportedSamProblem = true;
				break;
		}
		return null;
	}

	static class VisibilityInspector extends TypeBindingVisitor {

		private Scope scope;
		private boolean shouldChatter;
        private boolean visible = true;
		private FunctionalExpression expression;

		public VisibilityInspector(FunctionalExpression expression, Scope scope, boolean shouldChatter) {
			this.scope = scope;
			this.shouldChatter = shouldChatter;
			this.expression = expression;
		}

		private void checkVisibility(ReferenceBinding referenceBinding) {
			if (!referenceBinding.canBeSeenBy(this.scope)) {
				this.visible = false;
				if (this.shouldChatter)
					this.scope.problemReporter().descriptorHasInvisibleType(this.expression, referenceBinding);
			}
		}

		@Override
		public boolean visit(ReferenceBinding referenceBinding) {
			checkVisibility(referenceBinding);
			return true;
		}


		@Override
		public boolean visit(ParameterizedTypeBinding parameterizedTypeBinding) {
			checkVisibility(parameterizedTypeBinding);
			return true;
		}

		@Override
		public boolean visit(RawTypeBinding rawTypeBinding) {
			checkVisibility(rawTypeBinding);
			return true;
		}

		public boolean visible(TypeBinding type) {
			TypeBindingVisitor.visit(this, type);
			return this.visible;
		}

		public boolean visible(TypeBinding[] types) {
			TypeBindingVisitor.visit(this, types);
			return this.visible;
		}

	}

	public boolean kosherDescriptor(Scope scope, MethodBinding sam, boolean shouldChatter) {

		VisibilityInspector inspector = new VisibilityInspector(this, scope, shouldChatter);

		boolean status = true;
		if (!inspector.visible(sam.returnType))
			status = false;
		if (!inspector.visible(sam.parameters))
			status = false;
		if (!inspector.visible(sam.thrownExceptions))
			status = false;
		if (!inspector.visible(this.expectedType))
			status = false;
		this.hasDescripterProblem |= !status;
		return status;
	}

	public int nullStatus(FlowInfo flowInfo) {
		return FlowInfo.NON_NULL;
	}

	public int diagnosticsSourceEnd() {
		return this.sourceEnd;
	}

	public MethodBinding[] getRequiredBridges() {

		class BridgeCollector {

			MethodBinding [] bridges;
			MethodBinding method;
			char [] selector;
			LookupEnvironment environment;
			Scope scope;

			BridgeCollector(ReferenceBinding functionalType, MethodBinding method) {
				this.method = method;
				this.selector = method.selector;
				this.environment = FunctionalExpression.this.enclosingScope.environment();
				this.scope = FunctionalExpression.this.enclosingScope;
				collectBridges(new ReferenceBinding[]{functionalType});
			}

			void collectBridges(ReferenceBinding[] interfaces) {
				int length = interfaces == null ? 0 : interfaces.length;
				for (int i = 0; i < length; i++) {
					ReferenceBinding superInterface = interfaces[i];
					if (superInterface == null)
						continue;
					MethodBinding [] methods = superInterface.getMethods(this.selector);
					for (int j = 0, count = methods == null ? 0 : methods.length; j < count; j++) {
						MethodBinding inheritedMethod = methods[j];
						if (inheritedMethod == null || this.method == inheritedMethod)  // descriptor declaring class may not be same functional interface target type.
							continue;
						if (inheritedMethod.isStatic() || inheritedMethod.redeclaresPublicObjectMethod(this.scope))
							continue;
						inheritedMethod = MethodVerifier.computeSubstituteMethod(inheritedMethod, this.method, this.environment);
						if (inheritedMethod == null || !MethodVerifier.isSubstituteParameterSubsignature(this.method, inheritedMethod, this.environment) ||
								   !MethodVerifier.areReturnTypesCompatible(this.method, inheritedMethod, this.environment))
							continue;
						final MethodBinding originalInherited = inheritedMethod.original();
						final MethodBinding originalOverride = this.method.original();
						if (!originalOverride.areParameterErasuresEqual(originalInherited) || TypeBinding.notEquals(originalOverride.returnType.erasure(), originalInherited.returnType.erasure()))
							add(originalInherited);
					}
					collectBridges(superInterface.superInterfaces());
				}
			}
			void add(MethodBinding inheritedMethod) {
				if (this.bridges == null) {
					this.bridges = new MethodBinding[] { inheritedMethod };
					return;
				}
				int length = this.bridges.length;
				for (int i = 0; i < length; i++) {
					if (this.bridges[i].areParameterErasuresEqual(inheritedMethod) && TypeBinding.equalsEquals(this.bridges[i].returnType.erasure(), inheritedMethod.returnType.erasure()))
						return;
				}
				System.arraycopy(this.bridges, 0, this.bridges = new MethodBinding[length + 1], 0, length);
				this.bridges[length] = inheritedMethod;
			}
			MethodBinding [] getBridges () {
				return this.bridges;
			}
		}

		ReferenceBinding functionalType;
		if (this.expectedType instanceof IntersectionTypeBinding18) {
			functionalType = (ReferenceBinding) ((IntersectionTypeBinding18)this.expectedType).getSAMType(this.enclosingScope);
		} else {
			functionalType = (ReferenceBinding) this.expectedType;
		}
		return new BridgeCollector(functionalType, this.descriptor).getBridges();
	}
	boolean requiresBridges() {
		return getRequiredBridges() != null;
	}
	public void cleanUp() {
		// to be overridden by sub-classes
	}
}