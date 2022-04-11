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
 *     Stephan Herrmann <stephan@cs.tu-berlin.de> - Contributions for
 *							bug 319201 - [null] no warning when unboxing SingleNameReference causes NPE
 *							bug 292478 - Report potentially null across variable assignment
 *							bug 335093 - [compiler][null] minimal hook for future null annotation support
 *							bug 349326 - [1.7] new warning for missing try-with-resources
 *							bug 186342 - [compiler][null] Using annotations for null checking
 *							bug 358903 - Filter practically unimportant resource leak warnings
 *							bug 370639 - [compiler][resource] restore the default for resource leak warnings
 *							bug 365859 - [compiler][null] distinguish warnings based on flow analysis vs. null annotations
 *							bug 388996 - [compiler][resource] Incorrect 'potential resource leak'
 *							bug 394768 - [compiler][resource] Incorrect resource leak warning when creating stream in conditional
 *							bug 395002 - Self bound generic class doesn't resolve bounds properly for wildcards for certain parametrisation.
 *							bug 383368 - [compiler][null] syntactic null analysis for field references
 *							bug 400761 - [compiler][null] null may be return as boolean without a diagnostic
 *							Bug 392238 - [1.8][compiler][null] Detect semantically invalid null type annotations
 *							Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *							Bug 427438 - [1.8][compiler] NPE at org.eclipse.jdt.internal.compiler.ast.ConditionalExpression.generateCode(ConditionalExpression.java:280)
 *							Bug 430150 - [1.8][null] stricter checking against type variables
 *							Bug 453483 - [compiler][null][loop] Improve null analysis for loops
 *     Jesper S Moller - Contributions for
 *							Bug 378674 - "The method can be declared as static" is wrong
 *							Bug 527554 - [18.3] Compiler support for JEP 286 Local-Variable Type
 *							Bug 529556 - [18.3] Add content assist support for 'var' as a type
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *							Bug 409250 - [1.8][compiler] Various loose ends in 308 code generation
 *							Bug 426616 - [1.8][compiler] Type Annotations, multiple problems
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import static org.eclipse.jdt.internal.compiler.ast.ExpressionContext.ASSIGNMENT_CONTEXT;
import static org.eclipse.jdt.internal.compiler.ast.ExpressionContext.VANILLA_CONTEXT;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.impl.*;
import org.eclipse.jdt.internal.compiler.ast.TypeReference.AnnotationCollector;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.parser.RecoveryScanner;

public class LocalDeclaration extends AbstractVariableDeclaration {

	public LocalVariableBinding binding;

	public LocalDeclaration(
		char[] name,
		int sourceStart,
		int sourceEnd) {

		this.name = name;
		this.sourceStart = sourceStart;
		this.sourceEnd = sourceEnd;
		this.declarationEnd = sourceEnd;
	}

@Override
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	// record variable initialization if any
	if ((flowInfo.tagBits & FlowInfo.UNREACHABLE_OR_DEAD) == 0) {
		this.bits |= ASTNode.IsLocalDeclarationReachable; // only set if actually reached
	}
	if (this.initialization == null) {
		return flowInfo;
	}
	this.initialization.checkNPEbyUnboxing(currentScope, flowContext, flowInfo);

	FlowInfo preInitInfo = null;
	boolean shouldAnalyseResource = this.binding != null
			&& flowInfo.reachMode() == FlowInfo.REACHABLE
			&& currentScope.compilerOptions().analyseResourceLeaks
			&& FakedTrackingVariable.isAnyCloseable(this.initialization.resolvedType);
	if (shouldAnalyseResource) {
		preInitInfo = flowInfo.unconditionalCopy();
		// analysis of resource leaks needs additional context while analyzing the RHS:
		FakedTrackingVariable.preConnectTrackerAcrossAssignment(this, this.binding, this.initialization, flowInfo);
	}

	flowInfo =
		this.initialization
			.analyseCode(currentScope, flowContext, flowInfo)
			.unconditionalInits();

	if (shouldAnalyseResource)
		FakedTrackingVariable.handleResourceAssignment(currentScope, preInitInfo, flowInfo, flowContext, this, this.initialization, this.binding);
	else
		FakedTrackingVariable.cleanUpAfterAssignment(currentScope, Binding.LOCAL, this.initialization);

	int nullStatus = this.initialization.nullStatus(flowInfo, flowContext);
	if (!flowInfo.isDefinitelyAssigned(this.binding)){// for local variable debug attributes
		this.bits |= FirstAssignmentToLocal;
	} else {
		this.bits &= ~FirstAssignmentToLocal;  // int i = (i = 0);
	}
	flowInfo.markAsDefinitelyAssigned(this.binding);
	if (currentScope.compilerOptions().isAnnotationBasedNullAnalysisEnabled) {
		nullStatus = NullAnnotationMatching.checkAssignment(currentScope, flowContext, this.binding, flowInfo, nullStatus, this.initialization, this.initialization.resolvedType);
	}
	if ((this.binding.type.tagBits & TagBits.IsBaseType) == 0) {
		flowInfo.markNullStatus(this.binding, nullStatus);
		// no need to inform enclosing try block since its locals won't get
		// known by the finally block
	}
	return flowInfo;
}

	public void checkModifiers() {

		//only potential valid modifier is <<final>>
		if (((this.modifiers & ExtraCompilerModifiers.AccJustFlag) & ~ClassFileConstants.AccFinal) != 0)
			//AccModifierProblem -> other (non-visibility problem)
			//AccAlternateModifierProblem -> duplicate modifier
			//AccModifierProblem | AccAlternateModifierProblem -> visibility problem"

			this.modifiers = (this.modifiers & ~ExtraCompilerModifiers.AccAlternateModifierProblem) | ExtraCompilerModifiers.AccModifierProblem;
	}

	/**
	 * Code generation for a local declaration:
	 *	i.e.&nbsp;normal assignment to a local variable + unused variable handling
	 */
	@Override
	public void generateCode(BlockScope currentScope, CodeStream codeStream) {

		// even if not reachable, variable must be added to visible if allocated (28298)
		if (this.binding.resolvedPosition != -1) {
			codeStream.addVisibleLocalVariable(this.binding);
		}
		if ((this.bits & IsReachable) == 0) {
			return;
		}
		int pc = codeStream.position;

		// something to initialize?
		generateInit: {
			if (this.initialization == null)
				break generateInit;
			// forget initializing unused or final locals set to constant value (final ones are inlined)
			if (this.binding.resolvedPosition < 0) {
				if (this.initialization.constant != Constant.NotAConstant)
					break generateInit;
				// if binding unused generate then discard the value
				this.initialization.generateCode(currentScope, codeStream, false);
				break generateInit;
			}
			this.initialization.generateCode(currentScope, codeStream, true);
			// 26903, need extra cast to store null in array local var
			if (this.binding.type.isArrayType()
				&& ((this.initialization instanceof CastExpression)	// arrayLoc = (type[])null
						&& (((CastExpression)this.initialization).innermostCastedExpression().resolvedType == TypeBinding.NULL))){
				codeStream.checkcast(this.binding.type);
			}
			codeStream.store(this.binding, false);
			if ((this.bits & ASTNode.FirstAssignmentToLocal) != 0) {
				/* Variable may have been initialized during the code initializing it
					e.g. int i = (i = 1);
				*/
				this.binding.recordInitializationStartPC(codeStream.position);
			}
		}
		codeStream.recordPositionsFrom(pc, this.sourceStart);
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration#getKind()
	 */
	@Override
	public int getKind() {
		return LOCAL_VARIABLE;
	}

	// for local variables
	public void getAllAnnotationContexts(int targetType, LocalVariableBinding localVariable, List<AnnotationContext> allAnnotationContexts) {
		AnnotationCollector collector = new AnnotationCollector(this, targetType, localVariable, allAnnotationContexts);
		this.traverseWithoutInitializer(collector, (BlockScope) null);
	}

	// for arguments
	public void getAllAnnotationContexts(int targetType, int parameterIndex, List<AnnotationContext> allAnnotationContexts) {
		AnnotationCollector collector = new AnnotationCollector(this, targetType, parameterIndex, allAnnotationContexts);
		this.traverse(collector, (BlockScope) null);
	}

	public boolean isArgument() {
		return false;
	}
	public boolean isReceiver() {
		return false;
	}
	public TypeBinding patchType(TypeBinding newType) {
		// Perform upwards projection on type wrt mentioned type variables
		TypeBinding[] mentionedTypeVariables= findCapturedTypeVariables(newType);
		if (mentionedTypeVariables != null && mentionedTypeVariables.length > 0) {
			newType = newType.upwardsProjection(this.binding.declaringScope, mentionedTypeVariables);
		}
		this.type.resolvedType = newType;
		if (this.binding != null) {
			this.binding.type = newType;
			this.binding.markInitialized();
		}
		return this.type.resolvedType;
	}

	private TypeVariableBinding[] findCapturedTypeVariables(TypeBinding typeBinding) {
		final Set<TypeVariableBinding> mentioned = new HashSet<>();
		TypeBindingVisitor.visit(new TypeBindingVisitor() {
			@Override
			public boolean visit(TypeVariableBinding typeVariable) {
				if (typeVariable.isCapture())
					mentioned.add(typeVariable);
				return super.visit(typeVariable);
			}
		}, typeBinding);
		if (mentioned.isEmpty()) return null;
		return mentioned.toArray(new TypeVariableBinding[mentioned.size()]);
	}

	private static Expression findPolyExpression(Expression e) {
		// This is simpler than using an ASTVisitor, since we only care about a very few select cases.
		if (e instanceof FunctionalExpression) {
			return e;
		}
		if (e instanceof ConditionalExpression) {
			ConditionalExpression ce = (ConditionalExpression)e;
			Expression candidate = findPolyExpression(ce.valueIfTrue);
			if (candidate == null) {
				candidate = findPolyExpression(ce.valueIfFalse);
			}
			if (candidate != null) return candidate;
		}
		if (e instanceof SwitchExpression) {
			SwitchExpression se = (SwitchExpression)e;
			for (Expression re : se.resultExpressions) {
				Expression candidate = findPolyExpression(re);
				if (candidate != null) return candidate;
			}
		}
		return null;
	}

	@Override
	public void resolve(BlockScope scope) {
		resolve(scope, false);
	}
	public void resolve(BlockScope scope, boolean isPatternVariable) {
		// prescan NNBD
		handleNonNullByDefault(scope, this.annotations, this);

		TypeBinding variableType = null;
		boolean variableTypeInferenceError = false;
		boolean isTypeNameVar = isTypeNameVar(scope);
		if (isTypeNameVar && !isPatternVariable) {
			if ((this.bits & ASTNode.IsForeachElementVariable) == 0) {
				// infer a type from the initializer
				if (this.initialization != null) {
					variableType = checkInferredLocalVariableInitializer(scope);
					variableTypeInferenceError = variableType != null;
				} else {
					// That's always an error
					scope.problemReporter().varLocalWithoutInitizalier(this);
					variableType = scope.getJavaLangObject();
					variableTypeInferenceError = true;
				}
			}
		} else {
			variableType = this.type.resolveType(scope, true /* check bounds*/);
		}

		this.bits |= (this.type.bits & ASTNode.HasTypeAnnotations);
		checkModifiers();
		if (variableType != null) {
			if (variableType == TypeBinding.VOID) {
				scope.problemReporter().variableTypeCannotBeVoid(this);
				return;
			}
			if (variableType.isArrayType() && ((ArrayBinding) variableType).leafComponentType == TypeBinding.VOID) {
				scope.problemReporter().variableTypeCannotBeVoidArray(this);
				return;
			}
		}

		Binding existingVariable = scope.getBinding(this.name, Binding.VARIABLE, this, false /*do not resolve hidden field*/);
		if (existingVariable != null && existingVariable.isValidBinding()){
			boolean localExists = existingVariable instanceof LocalVariableBinding;
			if (localExists && (this.bits & ASTNode.ShadowsOuterLocal) != 0 && scope.isLambdaSubscope() && this.hiddenVariableDepth == 0) {
				scope.problemReporter().lambdaRedeclaresLocal(this);
			} else if (localExists && this.hiddenVariableDepth == 0) {
				scope.problemReporter().redefineLocal(this);
			} else {
				scope.problemReporter().localVariableHiding(this, existingVariable, false);
			}
		}

		if ((this.modifiers & ClassFileConstants.AccFinal)!= 0 && this.initialization == null) {
			this.modifiers |= ExtraCompilerModifiers.AccBlankFinal;
		}
		if (isTypeNameVar) {
			// Create binding for the initializer's type
			// In order to resolve self-referential initializers, we must declare the variable with a placeholder type (j.l.Object), and then patch it later
			this.binding = new LocalVariableBinding(this, variableType != null ? variableType : scope.getJavaLangObject(), this.modifiers, false) {
				private boolean isInitialized = false;

				@Override
				public void markReferenced() {
					if (! this.isInitialized) {
						scope.problemReporter().varLocalReferencesItself(LocalDeclaration.this);
						this.type = null;
						this.isInitialized = true; // Quell additional type errors
					}
				}
				@Override
				public void markInitialized() {
					this.isInitialized = true;
				}
			};
		} else {
			// create a binding from the specified type
			this.binding = new LocalVariableBinding(this, variableType, this.modifiers, false /*isArgument*/);
		}
		scope.addLocalVariable(this.binding);
		this.binding.setConstant(Constant.NotAConstant);
		// allow to recursivelly target the binding....
		// the correct constant is harmed if correctly computed at the end of this method

		if (variableType == null) {
			if (this.initialization != null) {
				if (this.initialization instanceof CastExpression) {
					((CastExpression)this.initialization).setVarTypeDeclaration(true);
				}
				this.initialization.resolveType(scope); // want to report all possible errors
				if (isTypeNameVar && this.initialization.resolvedType != null) {
					if (TypeBinding.equalsEquals(TypeBinding.NULL, this.initialization.resolvedType)) {
						scope.problemReporter().varLocalInitializedToNull(this);
						variableTypeInferenceError = true;
					} else if (TypeBinding.equalsEquals(TypeBinding.VOID, this.initialization.resolvedType)) {
						scope.problemReporter().varLocalInitializedToVoid(this);
						variableTypeInferenceError = true;
					}
					variableType = patchType(this.initialization.resolvedType);
				} else {
					variableTypeInferenceError = true;
				}
			}
		}
		this.binding.markInitialized();
		if (variableTypeInferenceError) {
			return;
		}
		boolean resolveAnnotationsEarly = false;
		if (scope.environment().usesNullTypeAnnotations()
				&& !isTypeNameVar // 'var' does not provide a target type
				&& variableType != null && variableType.isValidBinding()) {
			resolveAnnotationsEarly = this.initialization instanceof Invocation
					|| this.initialization instanceof ConditionalExpression
					|| this.initialization instanceof SwitchExpression
					|| this.initialization instanceof ArrayInitializer;
		}
		if (resolveAnnotationsEarly) {
			// these are definitely no constants, so resolving annotations early should be safe
			resolveAnnotations(scope, this.annotations, this.binding, true);
			// for type inference having null annotations upfront gives better results
			variableType = this.type.resolvedType;
		}
		if (this.initialization != null) {
			if (this.initialization instanceof ArrayInitializer) {
				TypeBinding initializationType = this.initialization.resolveTypeExpecting(scope, variableType);
				if (initializationType != null) {
					((ArrayInitializer) this.initialization).binding = (ArrayBinding) initializationType;
					this.initialization.computeConversion(scope, variableType, initializationType);
				}
			} else {
				this.initialization.setExpressionContext(isTypeNameVar ? VANILLA_CONTEXT : ASSIGNMENT_CONTEXT);
				this.initialization.setExpectedType(variableType);
				TypeBinding initializationType = this.initialization.resolvedType != null ? this.initialization.resolvedType : this.initialization.resolveType(scope);
				if (initializationType != null) {
					if (TypeBinding.notEquals(variableType, initializationType)) // must call before computeConversion() and typeMismatchError()
						scope.compilationUnitScope().recordTypeConversion(variableType, initializationType);
					if (this.initialization.isConstantValueOfTypeAssignableToType(initializationType, variableType)
						|| initializationType.isCompatibleWith(variableType, scope)) {
						this.initialization.computeConversion(scope, variableType, initializationType);
						if (initializationType.needsUncheckedConversion(variableType)) {
						    scope.problemReporter().unsafeTypeConversion(this.initialization, initializationType, variableType);
						}
						if (this.initialization instanceof CastExpression
								&& (this.initialization.bits & ASTNode.UnnecessaryCast) == 0) {
							CastExpression.checkNeedForAssignedCast(scope, variableType, (CastExpression) this.initialization);
						}
					} else if (isBoxingCompatible(initializationType, variableType, this.initialization, scope)) {
						this.initialization.computeConversion(scope, variableType, initializationType);
						if (this.initialization instanceof CastExpression
								&& (this.initialization.bits & ASTNode.UnnecessaryCast) == 0) {
							CastExpression.checkNeedForAssignedCast(scope, variableType, (CastExpression) this.initialization);
						}
					} else {
						if ((variableType.tagBits & TagBits.HasMissingType) == 0) {
							// if problem already got signaled on type, do not report secondary problem
							scope.problemReporter().typeMismatchError(initializationType, variableType, this.initialization, null);
						}
					}
				}
			}
			// check for assignment with no effect
			if (this.binding == Expression.getDirectBinding(this.initialization)) {
				scope.problemReporter().assignmentHasNoEffect(this, this.name);
			}
			// change the constant in the binding when it is final
			// (the optimization of the constant propagation will be done later on)
			// cast from constant actual type to variable type
			this.binding.setConstant(
				this.binding.isFinal()
					? this.initialization.constant.castTo((variableType.id << 4) + this.initialization.constant.typeID())
					: Constant.NotAConstant);
		}
		// if init could be a constant only resolve annotation at the end, for constant to be positioned before (96991)
		if (!resolveAnnotationsEarly)
			resolveAnnotations(scope, this.annotations, this.binding, true);
		Annotation.isTypeUseCompatible(this.type, scope, this.annotations);
		validateNullAnnotations(scope);
	}

	void validateNullAnnotations(BlockScope scope) {
		if (!scope.validateNullAnnotation(this.binding.tagBits, this.type, this.annotations))
			this.binding.tagBits &= ~TagBits.AnnotationNullMASK;
	}

	/*
	 * Checks the initializer for simple errors, and reports an error as needed. If error is found,
	 * returns a reasonable match for further type checking.
	 */
	private TypeBinding checkInferredLocalVariableInitializer(BlockScope scope) {
		TypeBinding errorType = null;
		if (this.initialization instanceof ArrayInitializer) {
			scope.problemReporter().varLocalCannotBeArrayInitalizers(this);
			errorType = scope.createArrayType(scope.getJavaLangObject(), 1); // Treat as array of anything
		} else {
			// Catch-22: isPolyExpression() is not reliable BEFORE resolveType, so we need to peek to suppress the errors
			Expression polyExpression = findPolyExpression(this.initialization);
			if (polyExpression instanceof ReferenceExpression) {
				scope.problemReporter().varLocalCannotBeMethodReference(this);
				errorType = TypeBinding.NULL;
			} else if (polyExpression != null) { // Should be instanceof LambdaExpression, but this is safer
				scope.problemReporter().varLocalCannotBeLambda(this);
				errorType = TypeBinding.NULL;
			}
		}
		if (this.type.dimensions() > 0 || this.type.extraDimensions() > 0) {
			scope.problemReporter().varLocalCannotBeArray(this);
			errorType = scope.createArrayType(scope.getJavaLangObject(), 1); // This is just to quell some warnings
		}
		if ((this.bits & ASTNode.IsAdditionalDeclarator) != 0) {
			scope.problemReporter().varLocalMultipleDeclarators(this);
			errorType = this.initialization.resolveType(scope);
		}
		return errorType;
	}

	@Override
	public void traverse(ASTVisitor visitor, BlockScope scope) {

		if (visitor.visit(this, scope)) {
			if (this.annotations != null) {
				int annotationsLength = this.annotations.length;
				for (int i = 0; i < annotationsLength; i++)
					this.annotations[i].traverse(visitor, scope);
			}
			this.type.traverse(visitor, scope);
			if (this.initialization != null)
				this.initialization.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}

	private void traverseWithoutInitializer(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.annotations != null) {
				int annotationsLength = this.annotations.length;
				for (int i = 0; i < annotationsLength; i++)
					this.annotations[i].traverse(visitor, scope);
			}
			this.type.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}

	public boolean isRecoveredFromLoneIdentifier() { // recovered from lonely identifier or identifier cluster ?
		return this.name == RecoveryScanner.FAKE_IDENTIFIER &&
				(this.type instanceof SingleTypeReference || (this.type instanceof QualifiedTypeReference && !(this.type instanceof ArrayQualifiedTypeReference))) && this.initialization == null && !this.type.isBaseTypeReference();
	}

	public boolean isTypeNameVar(Scope scope) {
		return this.type != null && this.type.isTypeNameVar(scope);
	}

}
