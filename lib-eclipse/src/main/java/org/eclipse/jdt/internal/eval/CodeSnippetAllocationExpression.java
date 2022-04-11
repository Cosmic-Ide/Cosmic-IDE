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
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *                          Bug 409245 - [1.8][compiler] Type annotations dropped when call is routed through a synthetic bridge method
 *                          Bug 409250 - [1.8][compiler] Various loose ends in 308 code generation
 *        Stephan Herrmann - Contribution for
 *							Bug 400874 - [1.8][compiler] Inference infrastructure should evolve to meet JLS8 18.x (Part G of JSR335 spec)
 *							Bug 424415 - [1.8][compiler] Eventual resolution of ReferenceExpression is not seen to be happening.
 *							Bug 427438 - [1.8][compiler] NPE at org.eclipse.jdt.internal.compiler.ast.ConditionalExpression.generateCode(ConditionalExpression.java:280)
 *******************************************************************************/
package org.eclipse.jdt.internal.eval;

import static org.eclipse.jdt.internal.compiler.ast.ExpressionContext.INVOCATION_CONTEXT;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.codegen.Opcodes;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;

public class CodeSnippetAllocationExpression extends AllocationExpression implements ProblemReasons, EvaluationConstants {
	EvaluationContext evaluationContext;
	FieldBinding delegateThis;
/**
 * CodeSnippetAllocationExpression constructor comment.
 */
public CodeSnippetAllocationExpression(EvaluationContext evaluationContext) {
	this.evaluationContext = evaluationContext;
}
@Override
public void generateCode(BlockScope currentScope, CodeStream codeStream, 	boolean valueRequired) {
	int pc = codeStream.position;
	MethodBinding codegenBinding = this.binding.original();
	ReferenceBinding allocatedType = codegenBinding.declaringClass;

	if (codegenBinding.canBeSeenBy(allocatedType, this, currentScope)) {
		codeStream.new_(this.type, allocatedType);
		if (valueRequired) {
			codeStream.dup();
		}
		// better highlight for allocation: display the type individually
		codeStream.recordPositionsFrom(pc, this.type.sourceStart);

		// handling innerclass instance allocation - enclosing instance arguments
		if (allocatedType.isNestedType()) {
			codeStream.generateSyntheticEnclosingInstanceValues(
				currentScope,
				allocatedType,
				enclosingInstance(),
				this);
		}
		// generate the arguments for constructor
		if (this.arguments != null) {
			for (int i = 0, count = this.arguments.length; i < count; i++) {
				this.arguments[i].generateCode(currentScope, codeStream, true);
			}
		}
		// handling innerclass instance allocation - outer local arguments
		if (allocatedType.isNestedType()) {
			codeStream.generateSyntheticOuterArgumentValues(
				currentScope,
				allocatedType,
				this);
		}
		// invoke constructor
		codeStream.invoke(Opcodes.OPC_invokespecial, codegenBinding, null /* default declaringClass */, this.typeArguments);
	} else {
		// private emulation using reflect
		codeStream.generateEmulationForConstructor(currentScope, codegenBinding);
		// generate arguments
		if (this.arguments != null) {
			int argsLength = this.arguments.length;
			codeStream.generateInlinedValue(argsLength);
			codeStream.newArray(currentScope.createArrayType(currentScope.getType(TypeConstants.JAVA_LANG_OBJECT, 3), 1));
			codeStream.dup();
			for (int i = 0; i < argsLength; i++) {
				codeStream.generateInlinedValue(i);
				this.arguments[i].generateCode(currentScope, codeStream, true);
				TypeBinding parameterBinding = codegenBinding.parameters[i];
				if (parameterBinding.isBaseType() && parameterBinding != TypeBinding.NULL) {
					codeStream.generateBoxingConversion(codegenBinding.parameters[i].id);
				}
				codeStream.aastore();
				if (i < argsLength - 1) {
					codeStream.dup();
				}
			}
		} else {
			codeStream.generateInlinedValue(0);
			codeStream.newArray(currentScope.createArrayType(currentScope.getType(TypeConstants.JAVA_LANG_OBJECT, 3), 1));
		}
		codeStream.invokeJavaLangReflectConstructorNewInstance();
		codeStream.checkcast(allocatedType);
	}
	codeStream.recordPositionsFrom(pc, this.sourceStart);
}
/* Inner emulation consists in either recording a dependency
 * link only, or performing one level of propagation.
 *
 * Dependency mechanism is used whenever dealing with source target
 * types, since by the time we reach them, we might not yet know their
 * exact need.
 */
@Override
public void manageEnclosingInstanceAccessIfNecessary(BlockScope currentScope, FlowInfo flowInfo) {
	// not supported yet
}
@Override
public void manageSyntheticAccessIfNecessary(BlockScope currentScope, FlowInfo flowInfo) {
	// do nothing
}
@Override
public TypeBinding resolveType(BlockScope scope) {
	// Propagate the type checking to the arguments, and check if the constructor is defined.
	this.constant = Constant.NotAConstant;
	this.resolvedType = this.type.resolveType(scope, true /* check bounds*/); // will check for null after args are resolved
	checkParameterizedAllocation: {
		if (this.type instanceof ParameterizedQualifiedTypeReference) { // disallow new X<String>.Y<Integer>()
			ReferenceBinding currentType = (ReferenceBinding)this.resolvedType;
			if (currentType == null) return currentType;
			do {
				// isStatic() is answering true for toplevel types
				if ((currentType.modifiers & ClassFileConstants.AccStatic) != 0) break checkParameterizedAllocation;
				if (currentType.isRawType()) break checkParameterizedAllocation;
			} while ((currentType = currentType.enclosingType())!= null);
			ParameterizedQualifiedTypeReference qRef = (ParameterizedQualifiedTypeReference) this.type;
			for (int i = qRef.typeArguments.length - 2; i >= 0; i--) {
				if (qRef.typeArguments[i] != null) {
					scope.problemReporter().illegalQualifiedParameterizedTypeAllocation(this.type, this.resolvedType);
					break;
				}
			}
		}
	}
	final boolean isDiamond = this.type != null && (this.type.bits & ASTNode.IsDiamond) != 0;
	// resolve type arguments (for generic constructor call)
	if (this.typeArguments != null) {
		int length = this.typeArguments.length;
		this.argumentsHaveErrors = scope.compilerOptions().sourceLevel < ClassFileConstants.JDK1_5;
		this.genericTypeArguments = new TypeBinding[length];
		for (int i = 0; i < length; i++) {
			TypeReference typeReference = this.typeArguments[i];
			if ((this.genericTypeArguments[i] = typeReference.resolveType(scope, true /* check bounds*/)) == null) {
				this.argumentsHaveErrors = true;
			}
			if (this.argumentsHaveErrors && typeReference instanceof Wildcard) {
				scope.problemReporter().illegalUsageOfWildcard(typeReference);
			}
		}
		if (isDiamond) {
			scope.problemReporter().diamondNotWithExplicitTypeArguments(this.typeArguments);
			return null;
		}
		if (this.argumentsHaveErrors) {
			if (this.arguments != null) { // still attempt to resolve arguments
				for (int i = 0, max = this.arguments.length; i < max; i++) {
					this.arguments[i].resolveType(scope);
				}
			}
			return null;
		}
	}

	// buffering the arguments' types
	this.argumentTypes = Binding.NO_PARAMETERS;
	if (this.arguments != null) {
		this.argumentsHaveErrors = false;
		int length = this.arguments.length;
		this.argumentTypes = new TypeBinding[length];
		for (int i = 0; i < length; i++) {
			Expression argument = this.arguments[i];
			if (argument instanceof CastExpression) {
				argument.bits |= DisableUnnecessaryCastCheck; // will check later on
				this.argsContainCast = true;
			}
			argument.setExpressionContext(INVOCATION_CONTEXT);
			if ((this.argumentTypes[i] = argument.resolveType(scope)) == null) {
				this.argumentsHaveErrors = true;
			}
		}
		if (this.argumentsHaveErrors) {
			return this.resolvedType;
		}
	}
	if (this.resolvedType == null) {
		return null;
	}
	if (!this.resolvedType.canBeInstantiated()) {
		scope.problemReporter().cannotInstantiate(this.type, this.resolvedType);
		return this.resolvedType;
	}
	if (isDiamond) {
		TypeBinding [] inferredTypes = inferElidedTypes(scope);
		if (inferredTypes == null) {
			scope.problemReporter().cannotInferElidedTypes(this);
			return this.resolvedType = null;
		}
		this.resolvedType = this.type.resolvedType = scope.environment().createParameterizedType(((ParameterizedTypeBinding) this.resolvedType).genericType(), inferredTypes, ((ParameterizedTypeBinding) this.resolvedType).enclosingType());
 	}

	ReferenceBinding allocatedType = (ReferenceBinding) this.resolvedType;
	this.binding = findConstructorBinding(scope, this, allocatedType, this.argumentTypes);

	if (!this.binding.isValidBinding()) {
		if (this.binding instanceof ProblemMethodBinding
			&& ((ProblemMethodBinding) this.binding).problemId() == NotVisible) {
			if (this.evaluationContext.declaringTypeName != null) {
				this.delegateThis = scope.getField(scope.enclosingSourceType(), DELEGATE_THIS, this);
				if (this.delegateThis == null) {
					if (this.binding.declaringClass == null) {
						this.binding.declaringClass = allocatedType;
					}
					if (this.type != null && !this.type.resolvedType.isValidBinding()) {
						return null;
					}
					scope.problemReporter().invalidConstructor(this, this.binding);
					return this.resolvedType;
				}
			} else {
				if (this.binding.declaringClass == null) {
					this.binding.declaringClass = allocatedType;
				}
				if (this.type != null && !this.type.resolvedType.isValidBinding()) {
					return null;
				}
				scope.problemReporter().invalidConstructor(this, this.binding);
				return this.resolvedType;
			}
			CodeSnippetScope localScope = new CodeSnippetScope(scope);
			MethodBinding privateBinding = localScope.getConstructor((ReferenceBinding)this.delegateThis.type, this.argumentTypes, this);
			if (!privateBinding.isValidBinding()) {
				if (this.binding.declaringClass == null) {
					this.binding.declaringClass = allocatedType;
				}
				if (this.type != null && !this.type.resolvedType.isValidBinding()) {
					return null;
				}
				scope.problemReporter().invalidConstructor(this, this.binding);
				return this.resolvedType;
			} else {
				this.binding = privateBinding;
			}
		} else {
			if (this.binding.declaringClass == null) {
				this.binding.declaringClass = allocatedType;
			}
			if (this.type != null && !this.type.resolvedType.isValidBinding()) {
				return null;
			}
			scope.problemReporter().invalidConstructor(this, this.binding);
			return this.resolvedType;
		}
	}
	if (isMethodUseDeprecated(this.binding, scope, true, this)) {
		scope.problemReporter().deprecatedMethod(this.binding, this);
	}
	if (this.arguments != null) {
		for (int i = 0; i < this.arguments.length; i++) {
			TypeBinding parameterType = this.binding.parameters[i];
			TypeBinding argumentType = this.argumentTypes[i];
			this.arguments[i].computeConversion(scope, parameterType, argumentType);
			if (argumentType.needsUncheckedConversion(parameterType)) {
				scope.problemReporter().unsafeTypeConversion(this.arguments[i], argumentType, parameterType);
			}
		}
		if (this.argsContainCast) {
			CastExpression.checkNeedForArgumentCasts(scope, null, allocatedType, this.binding, this.arguments, this.argumentTypes, this);
		}
	}
	if (allocatedType.isRawType() && this.binding.hasSubstitutedParameters()) {
		scope.problemReporter().unsafeRawInvocation(this, this.binding);
	}
	if (this.typeArguments != null && this.binding.original().typeVariables == Binding.NO_TYPE_VARIABLES) {
		scope.problemReporter().unnecessaryTypeArgumentsForMethodInvocation(this.binding, this.genericTypeArguments, this.typeArguments);
	}
	return allocatedType;
}
}
