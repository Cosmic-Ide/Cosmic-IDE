/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
 *								bug 368546 - [compiler][resource] Avoid remaining false positives found when compiling the Eclipse SDK
 *								bug 370639 - [compiler][resource] restore the default for resource leak warnings
 *								bug 388996 - [compiler][resource] Incorrect 'potential resource leak'
 *								Bug 417758 - [1.8][null] Null safety compromise during array creation.
 *								Bug 427438 - [1.8][compiler] NPE at org.eclipse.jdt.internal.compiler.ast.ConditionalExpression.generateCode(ConditionalExpression.java:280)
 *								Bug 435805 - [1.8][compiler][null] Java 8 compiler does not recognize declaration style null annotations
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import static org.eclipse.jdt.internal.compiler.ast.ExpressionContext.ASSIGNMENT_CONTEXT;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class ArrayInitializer extends Expression {

	public Expression[] expressions;
	public ArrayBinding binding; //the type of the { , , , }

	/**
	 * ArrayInitializer constructor comment.
	 */
	public ArrayInitializer() {

		super();
	}

	@Override
	public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {

		if (this.expressions != null) {
			CompilerOptions compilerOptions = currentScope.compilerOptions();
			boolean analyseResources = compilerOptions.analyseResourceLeaks;
			boolean evalNullTypeAnnotations = currentScope.environment().usesNullTypeAnnotations();
			for (int i = 0, max = this.expressions.length; i < max; i++) {
				flowInfo = this.expressions[i].analyseCode(currentScope, flowContext, flowInfo).unconditionalInits();

				if (analyseResources && FakedTrackingVariable.isAnyCloseable(this.expressions[i].resolvedType)) {
					flowInfo = FakedTrackingVariable.markPassedToOutside(currentScope, this.expressions[i], flowInfo, flowContext, false);
				}
				if (evalNullTypeAnnotations) {
					checkAgainstNullTypeAnnotation(currentScope, this.binding.elementsType(), this.expressions[i], flowContext, flowInfo);
				}
			}
		}
		return flowInfo;
	}

	@Override
	public void generateCode(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {
		generateCode(null, null, currentScope, codeStream, valueRequired);
	}

	/**
	 * Code generation for a array initializer
	 */
	public void generateCode(TypeReference typeReference, ArrayAllocationExpression allocationExpression, BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {

		// Flatten the values and compute the dimensions, by iterating in depth into nested array initializers
		int pc = codeStream.position;
		int expressionLength = (this.expressions == null) ? 0: this.expressions.length;
		codeStream.generateInlinedValue(expressionLength);
		codeStream.newArray(typeReference, allocationExpression, this.binding);
		if (this.expressions != null) {
			// binding is an ArrayType, so I can just deal with the dimension
			int elementsTypeID = this.binding.dimensions > 1 ? -1 : this.binding.leafComponentType.id;
			for (int i = 0; i < expressionLength; i++) {
				Expression expr;
				if ((expr = this.expressions[i]).constant != Constant.NotAConstant) {
					switch (elementsTypeID) { // filter out initializations to default values
						case T_int :
						case T_short :
						case T_byte :
						case T_char :
						case T_long :
							if (expr.constant.longValue() != 0) {
								codeStream.dup();
								codeStream.generateInlinedValue(i);
								expr.generateCode(currentScope, codeStream, true);
								codeStream.arrayAtPut(elementsTypeID, false);
							}
							break;
						case T_float :
						case T_double :
							double constantValue = expr.constant.doubleValue();
							if (constantValue == -0.0 || constantValue != 0) {
								codeStream.dup();
								codeStream.generateInlinedValue(i);
								expr.generateCode(currentScope, codeStream, true);
								codeStream.arrayAtPut(elementsTypeID, false);
							}
							break;
						case T_boolean :
							if (expr.constant.booleanValue() != false) {
								codeStream.dup();
								codeStream.generateInlinedValue(i);
								expr.generateCode(currentScope, codeStream, true);
								codeStream.arrayAtPut(elementsTypeID, false);
							}
							break;
						default :
							if (!(expr instanceof NullLiteral)) {
								codeStream.dup();
								codeStream.generateInlinedValue(i);
								expr.generateCode(currentScope, codeStream, true);
								codeStream.arrayAtPut(elementsTypeID, false);
							}
					}
				} else if (!(expr instanceof NullLiteral)) {
					codeStream.dup();
					codeStream.generateInlinedValue(i);
					expr.generateCode(currentScope, codeStream, true);
					codeStream.arrayAtPut(elementsTypeID, false);
				}
			}
		}
		if (valueRequired) {
			codeStream.generateImplicitConversion(this.implicitConversion);
		} else {
			codeStream.pop();
		}
		codeStream.recordPositionsFrom(pc, this.sourceStart);
	}

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output) {

		output.append('{');
		if (this.expressions != null) {
			int j = 20 ;
			for (int i = 0 ; i < this.expressions.length ; i++) {
				if (i > 0) output.append(", "); //$NON-NLS-1$
				this.expressions[i].printExpression(0, output);
				j -- ;
				if (j == 0) {
					output.append('\n');
					printIndent(indent+1, output);
					j = 20;
				}
			}
		}
		return output.append('}');
	}

	@Override
	public TypeBinding resolveTypeExpecting(BlockScope scope, TypeBinding expectedType) {
		// Array initializers can only occur on the right hand side of an assignment
		// expression, therefore the expected type contains the valid information
		// concerning the type that must be enforced by the elements of the array initializer.

		// this method is recursive... (the test on isArrayType is the stop case)

		this.constant = Constant.NotAConstant;

		if (expectedType instanceof ArrayBinding) {
			// allow new List<?>[5]
			if ((this.bits & IsAnnotationDefaultValue) == 0) { // annotation default value need only to be commensurate JLS9.7
				// allow new List<?>[5] - only check for generic array when no initializer, since also checked inside initializer resolution
				TypeBinding leafComponentType = expectedType.leafComponentType();
				if (!leafComponentType.isReifiable()) {
				    scope.problemReporter().illegalGenericArray(leafComponentType, this);
				}
			}
			this.resolvedType = this.binding = (ArrayBinding) expectedType;
			if (this.expressions == null)
				return this.binding;
			TypeBinding elementType = this.binding.elementsType();
			for (int i = 0, length = this.expressions.length; i < length; i++) {
				Expression expression = this.expressions[i];
				expression.setExpressionContext(ASSIGNMENT_CONTEXT);
				expression.setExpectedType(elementType);
				TypeBinding expressionType = expression instanceof ArrayInitializer
						? expression.resolveTypeExpecting(scope, elementType)
						: expression.resolveType(scope);
				if (expressionType == null)
					continue;

				// Compile-time conversion required?
				if (TypeBinding.notEquals(elementType, expressionType)) // must call before computeConversion() and typeMismatchError()
					scope.compilationUnitScope().recordTypeConversion(elementType, expressionType);

				if (expression.isConstantValueOfTypeAssignableToType(expressionType, elementType)
						|| expressionType.isCompatibleWith(elementType)) {
					expression.computeConversion(scope, elementType, expressionType);
				} else if (isBoxingCompatible(expressionType, elementType, expression, scope)) {
					expression.computeConversion(scope, elementType, expressionType);
				} else {
					scope.problemReporter().typeMismatchError(expressionType, elementType, expression, null);
				}
			}
			return this.binding;
		}

		// infer initializer type for error reporting based on first element
		TypeBinding leafElementType = null;
		int dim = 1;
		if (this.expressions == null) {
			leafElementType = scope.getJavaLangObject();
		} else {
			Expression expression = this.expressions[0];
			while(expression != null && expression instanceof ArrayInitializer) {
				dim++;
				Expression[] subExprs = ((ArrayInitializer) expression).expressions;
				if (subExprs == null){
					leafElementType = scope.getJavaLangObject();
					expression = null;
					break;
				}
				expression = ((ArrayInitializer) expression).expressions[0];
			}
			if (expression != null) {
				leafElementType = expression.resolveType(scope);
			}
			// fault-tolerance - resolve other expressions as well
			for (int i = 1, length = this.expressions.length; i < length; i++) {
				expression = this.expressions[i];
				if (expression != null) {
					expression.resolveType(scope)	;
				}
			}		}
		if (leafElementType != null) {
			this.resolvedType = scope.createArrayType(leafElementType, dim);
			if (expectedType != null)
				scope.problemReporter().typeMismatchError(this.resolvedType, expectedType, this, null);
		}
		return null;
	}

	@Override
	public void traverse(ASTVisitor visitor, BlockScope scope) {

		if (visitor.visit(this, scope)) {
			if (this.expressions != null) {
				int expressionsLength = this.expressions.length;
				for (int i = 0; i < expressionsLength; i++)
					this.expressions[i].traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}
}
