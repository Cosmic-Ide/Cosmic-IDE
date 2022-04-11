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
 *                          Bug 409245 - [1.8][compiler] Type annotations dropped when call is routed through a synthetic bridge method
 *                          Bug 409250 - [1.8][compiler] Various loose ends in 308 code generation
 *        Stephan Herrmann - Contribution for
 *							Bug 400874 - [1.8][compiler] Inference infrastructure should evolve to meet JLS8 18.x (Part G of JSR335 spec)
 *							Bug 424415 - [1.8][compiler] Eventual resolution of ReferenceExpression is not seen to be happening.
 *							Bug 427438 - [1.8][compiler] NPE at org.eclipse.jdt.internal.compiler.ast.ConditionalExpression.generateCode(ConditionalExpression.java:280)
 *******************************************************************************/
package org.eclipse.jdt.internal.eval;

import static org.eclipse.jdt.internal.compiler.ast.ExpressionContext.INVOCATION_CONTEXT;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.codegen.Opcodes;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;

public class CodeSnippetMessageSend extends MessageSend {
	EvaluationContext evaluationContext;
	FieldBinding delegateThis;
/**
 * CodeSnippetMessageSend constructor comment.
 */
public CodeSnippetMessageSend(EvaluationContext evaluationContext) {
	this.evaluationContext = evaluationContext;
}
/**
 * MessageSend code generation
 *
 * @param currentScope org.eclipse.jdt.internal.compiler.lookup.BlockScope
 * @param codeStream org.eclipse.jdt.internal.compiler.codegen.CodeStream
 * @param valueRequired boolean
 */
@Override
public void generateCode(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {
	int pc = codeStream.position;
	MethodBinding codegenBinding = this.binding.original();
	if (codegenBinding.canBeSeenBy(this.actualReceiverType.original(), this, currentScope)) {
		// generate receiver/enclosing instance access
		boolean isStatic = codegenBinding.isStatic();
		// outer access ?
		if (!isStatic && ((this.bits & DepthMASK) != 0)) {
			// outer method can be reached through emulation
			ReferenceBinding targetType = currentScope.enclosingSourceType().enclosingTypeAt((this.bits & DepthMASK) >> DepthSHIFT);
			Object[] path = currentScope.getEmulationPath(targetType, true /*only exact match*/, false/*consider enclosing arg*/);
			if (path == null) {
				// emulation was not possible (should not happen per construction)
				currentScope.problemReporter().needImplementation(this);
			} else {
				codeStream.generateOuterAccess(path, this, targetType, currentScope);
			}
		} else {
			this.receiver.generateCode(currentScope, codeStream, !isStatic);
			if ((this.bits & NeedReceiverGenericCast) != 0) {
				codeStream.checkcast(this.actualReceiverType);
			}
			codeStream.recordPositionsFrom(pc, this.sourceStart);
		}
		// generate arguments
		generateArguments(this.binding, this.arguments, currentScope, codeStream);
		// actual message invocation
		TypeBinding constantPoolDeclaringClass = CodeStream.getConstantPoolDeclaringClass(currentScope, codegenBinding, this.actualReceiverType, this.receiver.isImplicitThis());
		if (isStatic) {
			codeStream.invoke(Opcodes.OPC_invokestatic, codegenBinding, constantPoolDeclaringClass, this.typeArguments);
		} else if( (this.receiver.isSuper()) || codegenBinding.isPrivate()){
			codeStream.invoke(Opcodes.OPC_invokespecial, codegenBinding, constantPoolDeclaringClass, this.typeArguments);
		} else {
			if (constantPoolDeclaringClass.isInterface()) { // interface or annotation type
				codeStream.invoke(Opcodes.OPC_invokeinterface, codegenBinding, constantPoolDeclaringClass, this.typeArguments);
			} else {
				codeStream.invoke(Opcodes.OPC_invokevirtual, codegenBinding, constantPoolDeclaringClass, this.typeArguments);
			}
		}
	} else {
		codeStream.generateEmulationForMethod(currentScope, codegenBinding);
		// generate receiver/enclosing instance access
		boolean isStatic = codegenBinding.isStatic();
		// outer access ?
		if (!isStatic && ((this.bits & DepthMASK) != 0)) {
			// not supported yet
			currentScope.problemReporter().needImplementation(this);
		} else {
			this.receiver.generateCode(currentScope, codeStream, !isStatic);
			if ((this.bits & NeedReceiverGenericCast) != 0) {
				codeStream.checkcast(this.actualReceiverType);
			}
			codeStream.recordPositionsFrom(pc, this.sourceStart);
		}
		if (isStatic) {
			// we need an object on the stack which is ignored for the method invocation
			codeStream.aconst_null();
		}
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
		codeStream.invokeJavaLangReflectMethodInvoke();

		// convert the return value to the appropriate type for primitive types
		if (codegenBinding.returnType.isBaseType()) {
			int typeID = codegenBinding.returnType.id;
			if (typeID == T_void) {
				// remove the null from the stack
				codeStream.pop();
			}
			codeStream.checkcast(typeID);
			codeStream.getBaseTypeValue(typeID);
		} else {
			codeStream.checkcast(codegenBinding.returnType);
		}
	}
	// required cast must occur even if no value is required
	if (this.valueCast != null) codeStream.checkcast(this.valueCast);
	if (valueRequired){
		// implicit conversion if necessary
		codeStream.generateImplicitConversion(this.implicitConversion);
	} else {
		boolean isUnboxing = (this.implicitConversion & TypeIds.UNBOXING) != 0;
		// conversion only generated if unboxing
		if (isUnboxing) codeStream.generateImplicitConversion(this.implicitConversion);
		switch (isUnboxing ? postConversionType(currentScope).id : codegenBinding.returnType.id) {
			case T_long :
			case T_double :
				codeStream.pop2();
				break;
			case T_void :
				break;
			default :
				codeStream.pop();
		}
	}
	codeStream.recordPositionsFrom(pc, (int)(this.nameSourcePosition >>> 32)); // highlight selector
}
@Override
public void manageSyntheticAccessIfNecessary(BlockScope currentScope, FlowInfo flowInfo) {

	if ((flowInfo.tagBits & FlowInfo.UNREACHABLE_OR_DEAD) == 0) {
		// if method from parameterized type got found, use the original method at codegen time
		MethodBinding codegenBinding = this.binding.original();
		if (codegenBinding != this.binding) {
		    // extra cast needed if method return type was type variable
		    if (codegenBinding.returnType.isTypeVariable()) {
		        TypeVariableBinding variableReturnType = (TypeVariableBinding) codegenBinding.returnType;
		        if (TypeBinding.notEquals(variableReturnType.firstBound, this.binding.returnType)) { // no need for extra cast if same as first bound anyway
				    this.valueCast = this.binding.returnType;
		        }
		    }
		}
	}
}
@Override
public TypeBinding resolveType(BlockScope scope) {
	// Answer the signature return type
	// Base type promotion

	if (this.constant != Constant.NotAConstant) {
		this.constant = Constant.NotAConstant;
		boolean receiverCast = false;
		if (this.receiver instanceof CastExpression) {
			this.receiver.bits |= DisableUnnecessaryCastCheck; // will check later on
			receiverCast = true;
		}
		this.actualReceiverType = this.receiver.resolveType(scope);
		if (receiverCast && this.actualReceiverType != null) {
			// due to change of declaring class with receiver type, only identity cast should be notified
			if (TypeBinding.equalsEquals(((CastExpression)this.receiver).expression.resolvedType, this.actualReceiverType)) {
				scope.problemReporter().unnecessaryCast((CastExpression)this.receiver);
			}
		}
		// resolve type arguments (for generic constructor call)
		if (this.typeArguments != null) {
			int length = this.typeArguments.length;
			this.argumentsHaveErrors = false; // typeChecks all arguments
			this.genericTypeArguments = new TypeBinding[length];
			for (int i = 0; i < length; i++) {
				if ((this.genericTypeArguments[i] = this.typeArguments[i].resolveType(scope, true /* check bounds*/)) == null) {
					this.argumentsHaveErrors = true;
				}
			}
			if (this.argumentsHaveErrors) {
				return null;
			}
		}
		// will check for null after args are resolved
		if (this.arguments != null) {
			this.argumentsHaveErrors = false; // typeChecks all arguments
			int length = this.arguments.length;
			this.argumentTypes = new TypeBinding[length];
			for (int i = 0; i < length; i++) {
				Expression argument = this.arguments[i];
				if (argument instanceof CastExpression) {
					argument.bits |= DisableUnnecessaryCastCheck; // will check later on
					this.argsContainCast = true;
				}
				argument.setExpressionContext(INVOCATION_CONTEXT);
				if ((this.argumentTypes[i] = this.arguments[i].resolveType(scope)) == null)
					this.argumentsHaveErrors = true;
			}
			if (this.argumentsHaveErrors) {
				if(this.actualReceiverType instanceof ReferenceBinding) {
					// record any selector match, for clients who may still need hint about possible method match
					this.binding = scope.findMethod((ReferenceBinding)this.actualReceiverType, this.selector, new TypeBinding[]{}, this, false);
				}
				return null;
			}
		}
		if (this.actualReceiverType == null) {
			return null;
		}
		// base type cannot receive any message
		if (this.actualReceiverType.isBaseType()) {
			scope.problemReporter().errorNoMethodFor(this, this.actualReceiverType, this.argumentTypes);
			return null;
		}
	}
	findMethodBinding(scope);

	if (!this.binding.isValidBinding()) {
		if (this.binding instanceof ProblemMethodBinding
			&& ((ProblemMethodBinding) this.binding).problemId() == ProblemReasons.NotVisible) {
			if (this.evaluationContext.declaringTypeName != null) {
				this.delegateThis = scope.getField(scope.enclosingSourceType(), EvaluationConstants.DELEGATE_THIS, this);
				if (this.delegateThis == null){ // if not found then internal error, field should have been found
					this.constant = Constant.NotAConstant;
					scope.problemReporter().invalidMethod(this, this.binding, scope);
					return null;
				}
			} else {
				this.constant = Constant.NotAConstant;
				scope.problemReporter().invalidMethod(this, this.binding, scope);
				return null;
			}
			CodeSnippetScope localScope = new CodeSnippetScope(scope);
			MethodBinding privateBinding =
				this.receiver instanceof CodeSnippetThisReference && ((CodeSnippetThisReference) this.receiver).isImplicit
					? localScope.getImplicitMethod((ReferenceBinding)this.delegateThis.type, this.selector, this.argumentTypes, this)
					: localScope.getMethod(this.delegateThis.type, this.selector, this.argumentTypes, this);
			if (!privateBinding.isValidBinding()) {
				if (this.binding.declaringClass == null) {
					if (this.actualReceiverType instanceof ReferenceBinding) {
						this.binding.declaringClass = (ReferenceBinding) this.actualReceiverType;
					} else { // really bad error ....
						scope.problemReporter().errorNoMethodFor(this, this.actualReceiverType, this.argumentTypes);
						return null;
					}
				}
				scope.problemReporter().invalidMethod(this, this.binding, scope);
				return null;
			} else {
				this.binding = privateBinding;
			}
		} else {
			if (this.binding.declaringClass == null) {
				if (this.actualReceiverType instanceof ReferenceBinding) {
					this.binding.declaringClass = (ReferenceBinding) this.actualReceiverType;
				} else { // really bad error ....
					scope.problemReporter().errorNoMethodFor(this, this.actualReceiverType, this.argumentTypes);
					return null;
				}
			}
			scope.problemReporter().invalidMethod(this, this.binding, scope);
			return null;
		}
	}
	if (!this.binding.isStatic()) {
		// the "receiver" must not be a type, in other words, a NameReference that the TC has bound to a Type
		if (this.receiver instanceof NameReference
				&& (((NameReference) this.receiver).bits & Binding.TYPE) != 0) {
			scope.problemReporter().mustUseAStaticMethod(this, this.binding);
		} else {
			// handle indirect inheritance thru variable secondary bound
			// receiver may receive generic cast, as part of implicit conversion
			TypeBinding oldReceiverType = this.actualReceiverType;
			this.actualReceiverType = this.actualReceiverType.getErasureCompatibleType(this.binding.declaringClass);
			this.receiver.computeConversion(scope, this.actualReceiverType, this.actualReceiverType);
			if (TypeBinding.notEquals(this.actualReceiverType, oldReceiverType) && TypeBinding.notEquals(this.receiver.postConversionType(scope), this.actualReceiverType)) { // record need for explicit cast at codegen since receiver could not handle it
				this.bits |= NeedReceiverGenericCast;
			}
		}
	}
	if (checkInvocationArguments(scope, this.receiver, this.actualReceiverType, this.binding, this.arguments, this.argumentTypes, this.argsContainCast, this)) {
		this.bits |= ASTNode.Unchecked;
	}

	//-------message send that are known to fail at compile time-----------
	if (this.binding.isAbstract()) {
		if (this.receiver.isSuper()) {
			scope.problemReporter().cannotDireclyInvokeAbstractMethod(this, this.binding);
		}
		// abstract private methods cannot occur nor abstract static............
	}
	if (isMethodUseDeprecated(this.binding, scope, true, this))
		scope.problemReporter().deprecatedMethod(this.binding, this);

	// from 1.5 compliance on, array#clone() returns the array type (but binding still shows Object)
	if (this.actualReceiverType.isArrayType()
			&& this.binding.parameters == Binding.NO_PARAMETERS
			&& scope.compilerOptions().complianceLevel >= ClassFileConstants.JDK1_5
			&& CharOperation.equals(this.binding.selector, CLONE)) {
		this.resolvedType = this.actualReceiverType;
	} else {
		TypeBinding returnType = this.binding.returnType;

		if (returnType != null) {
			if ((this.bits & ASTNode.Unchecked) != 0 && this.genericTypeArguments == null) {
				returnType = scope.environment().convertToRawType(returnType.erasure(), true);
			}
			returnType = returnType.capture(scope, this.sourceStart, this.sourceEnd);
		}
		this.resolvedType = returnType;
	}
	return this.resolvedType;
}
}

