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
 *     Stephan Herrmann - Contributions for
 *								bug 335093 - [compiler][null] minimal hook for future null annotation support
 *								bug 349326 - [1.7] new warning for missing try-with-resources
 *								bug 186342 - [compiler][null] Using annotations for null checking
 *								bug 365983 - [compiler][null] AIOOB with null annotation analysis and varargs
 *								bug 368546 - [compiler][resource] Avoid remaining false positives found when compiling the Eclipse SDK
 *								bug 370930 - NonNull annotation not considered for enhanced for loops
 *								bug 365859 - [compiler][null] distinguish warnings based on flow analysis vs. null annotations
 *								bug 392862 - [1.8][compiler][null] Evaluate null annotations on array types
 *								bug 331649 - [compiler][null] consider null annotations for fields
 *								bug 383368 - [compiler][null] syntactic null analysis for field references
 *								Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								Bug 415043 - [1.8][null] Follow-up re null type annotations after bug 392099
 *								Bug 415291 - [1.8][null] differentiate type incompatibilities due to null annotations
 *								Bug 392238 - [1.8][compiler][null] Detect semantically invalid null type annotations
 *								Bug 416307 - [1.8][compiler][null] subclass with type parameter substitution confuses null checking
 *								Bug 417758 - [1.8][null] Null safety compromise during array creation.
 *								Bug 400874 - [1.8][compiler] Inference infrastructure should evolve to meet JLS8 18.x (Part G of JSR335 spec)
 *								Bug 424415 - [1.8][compiler] Eventual resolution of ReferenceExpression is not seen to be happening.
 *								Bug 418537 - [1.8][null] Fix null type annotation analysis for poly conditional expressions
 *								Bug 428352 - [1.8][compiler] Resolution errors don't always surface
 *								Bug 429430 - [1.8] Lambdas and method reference infer wrong exception type with generics (RuntimeException instead of IOException)
 *								Bug 435805 - [1.8][compiler][null] Java 8 compiler does not recognize declaration style null annotations
 *								Bug 453483 - [compiler][null][loop] Improve null analysis for loops
 *								Bug 455723 - Nonnull argument not correctly inferred in loop
 *        Andy Clement - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *                          Bug 409250 - [1.8][compiler] Various loose ends in 308 code generation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import java.util.function.BooleanSupplier;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.NullAnnotationMatching.CheckMode;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.*;

public abstract class Statement extends ASTNode {

	/**
	 * Answers true if the if is identified as a known coding pattern which
	 * should be tolerated by dead code analysis.
	 * e.g. if (DEBUG) print(); // no complaint
	 * Only invoked when overall condition is known to be optimizeable into false/true.
	 */
	protected static boolean isKnowDeadCodePattern(Expression expression) {
		// if (!DEBUG) print(); - tolerated
		if (expression instanceof UnaryExpression) {
			expression = ((UnaryExpression) expression).expression;
		}
		// if (DEBUG) print(); - tolerated
		if (expression instanceof Reference) return true;

//		if (expression instanceof BinaryExpression) {
//			BinaryExpression binary = (BinaryExpression) expression;
//			switch ((binary.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT/* operator */) {
//				case OperatorIds.AND_AND :
//				case OperatorIds.OR_OR :
//					break;
//				default:
//					// if (DEBUG_LEVEL > 0) print(); - tolerated
//					if ((binary.left instanceof Reference) && binary.right.constant != Constant.NotAConstant)
//						return true;
//					// if (0 < DEBUG_LEVEL) print(); - tolerated
//					if ((binary.right instanceof Reference) && binary.left.constant != Constant.NotAConstant)
//						return true;
//			}
//		}
		return false;
	}
public abstract FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo);
/** Lambda shape analysis: *Assuming* this is reachable, analyze if this completes normally i.e control flow can reach the textually next statement.
   For blocks, we don't perform intra-reachability analysis. We assume the lambda body is free of intrinsic control flow errors (if such errors
   exist they will not be flagged by this analysis, but are guaranteed to surface later on.)

   @see Block#doesNotCompleteNormally()
*/
public boolean doesNotCompleteNormally() {
	return false;
}

/** Lambda shape analysis: *Assuming* this is reachable, analyze if this completes by continuing i.e control flow cannot reach the textually next statement.
    This is necessitated by the fact that continue claims to not complete normally. So this is necessary to discriminate between do { continue; } while (false);
    which completes normally and do { throw new Exception(); } while (false); which does not complete normally.
*/
public boolean completesByContinue() {
	return false;
}

/**
 * Switch Expression analysis: *Assuming* this is reachable, analyze if this completes normally
 *  i.e control flow can reach the textually next statement, as per JLS 14 Sec 14.22
 *  For blocks, we don't perform intra-reachability analysis.
 *  Note: delinking this from a similar (opposite) {@link #doesNotCompleteNormally()} since that was
 *  coded for a specific purpose of Lambda Shape Analysis.
 */
public boolean canCompleteNormally() {
	return true;
}
/**
 * The equivalent function of completesByContinue - implements both the rules concerning continue with
 * and without a label.
 */
public boolean continueCompletes() {
	return false;
}
	public static final int NOT_COMPLAINED = 0;
	public static final int COMPLAINED_FAKE_REACHABLE = 1;
	public static final int COMPLAINED_UNREACHABLE = 2;
	LocalVariableBinding[] patternVarsWhenTrue;
	LocalVariableBinding[] patternVarsWhenFalse;


/** Analysing arguments of MessageSend, ExplicitConstructorCall, AllocationExpression. */
protected void analyseArguments(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo, MethodBinding methodBinding, Expression[] arguments)
{
	// compare actual null-status against parameter annotations of the called method:
	if (arguments != null) {
		CompilerOptions compilerOptions = currentScope.compilerOptions();
		if (compilerOptions.sourceLevel >= ClassFileConstants.JDK1_7 && methodBinding.isPolymorphic())
			return;
		boolean considerTypeAnnotations = currentScope.environment().usesNullTypeAnnotations();
		boolean hasJDK15NullAnnotations = methodBinding.parameterNonNullness != null;
		int numParamsToCheck = methodBinding.parameters.length;
		int varArgPos = -1;
		TypeBinding varArgsType = null;
		boolean passThrough = false;
		if (considerTypeAnnotations || hasJDK15NullAnnotations) {
			// check if varargs need special treatment:
			if (methodBinding.isVarargs()) {
				varArgPos = numParamsToCheck-1;
				// this if-block essentially copied from generateArguments(..):
				varArgsType = methodBinding.parameters[varArgPos];
				if (numParamsToCheck == arguments.length) {
					TypeBinding lastType = arguments[varArgPos].resolvedType;
					if (lastType == TypeBinding.NULL
							|| (varArgsType.dimensions() == lastType.dimensions()
							&& lastType.isCompatibleWith(varArgsType)))
						passThrough = true; // pass directly as-is
				}
				if (!passThrough)
					numParamsToCheck--; // with non-passthrough varargs last param is fed from individual args -> don't check
			}
		}
		if (considerTypeAnnotations) {
			for (int i=0; i<numParamsToCheck; i++) {
				TypeBinding expectedType = methodBinding.parameters[i];
				Boolean specialCaseNonNullness = hasJDK15NullAnnotations ? methodBinding.parameterNonNullness[i] : null;
				analyseOneArgument18(currentScope, flowContext, flowInfo, expectedType, arguments[i],
						specialCaseNonNullness, methodBinding.original().parameters[i]);
			}
			if (!passThrough && varArgsType instanceof ArrayBinding) {
				TypeBinding expectedType = ((ArrayBinding) varArgsType).elementsType();
				Boolean specialCaseNonNullness = hasJDK15NullAnnotations ? methodBinding.parameterNonNullness[varArgPos] : null;
				for (int i = numParamsToCheck; i < arguments.length; i++) {
					analyseOneArgument18(currentScope, flowContext, flowInfo, expectedType, arguments[i],
							specialCaseNonNullness, methodBinding.original().parameters[varArgPos]);
				}
			}
		} else if (hasJDK15NullAnnotations) {
			for (int i = 0; i < numParamsToCheck; i++) {
				if (methodBinding.parameterNonNullness[i] == Boolean.TRUE) {
					TypeBinding expectedType = methodBinding.parameters[i];
					Expression argument = arguments[i];
					int nullStatus = argument.nullStatus(flowInfo, flowContext); // slight loss of precision: should also use the null info from the receiver.
					if (nullStatus != FlowInfo.NON_NULL) // if required non-null is not provided
						flowContext.recordNullityMismatch(currentScope, argument, argument.resolvedType, expectedType, flowInfo, nullStatus, null);
				}
			}
		}
	}
}
void analyseOneArgument18(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo,
		TypeBinding expectedType, Expression argument, Boolean expectedNonNullness, TypeBinding originalExpected) {
	if (argument instanceof ConditionalExpression && argument.isPolyExpression()) {
		// drill into both branches using existing nullStatus per branch:
		ConditionalExpression ce = (ConditionalExpression) argument;
		ce.internalAnalyseOneArgument18(currentScope, flowContext, expectedType, ce.valueIfTrue, flowInfo, ce.ifTrueNullStatus, expectedNonNullness, originalExpected);
		ce.internalAnalyseOneArgument18(currentScope, flowContext, expectedType, ce.valueIfFalse, flowInfo, ce.ifFalseNullStatus, expectedNonNullness, originalExpected);
		return;
	} else 	if (argument instanceof SwitchExpression && argument.isPolyExpression()) {
		SwitchExpression se = (SwitchExpression) argument;
		for (int i = 0; i < se.resultExpressions.size(); i++) {
			se.internalAnalyseOneArgument18(currentScope, flowContext, expectedType,
					se.resultExpressions.get(i), flowInfo,
					se.resultExpressionNullStatus.get(i), expectedNonNullness, originalExpected);
		}
		return;
	}

	int nullStatus = argument.nullStatus(flowInfo, flowContext);
	internalAnalyseOneArgument18(currentScope, flowContext, expectedType, argument, flowInfo,
									nullStatus, expectedNonNullness, originalExpected);
}
void internalAnalyseOneArgument18(BlockScope currentScope, FlowContext flowContext, TypeBinding expectedType,
		Expression argument, FlowInfo flowInfo, int nullStatus, Boolean expectedNonNullness, TypeBinding originalExpected)
{
	// here we consume special case information generated in the ctor of ParameterizedGenericMethodBinding (see there):
	int statusFromAnnotatedNull = expectedNonNullness == Boolean.TRUE ? nullStatus : 0;

	NullAnnotationMatching annotationStatus = NullAnnotationMatching.analyse(expectedType, argument.resolvedType, nullStatus);

	if (!annotationStatus.isAnyMismatch() && statusFromAnnotatedNull != 0)
		expectedType = originalExpected; // to avoid reports mentioning '@NonNull null'!

	if (statusFromAnnotatedNull == FlowInfo.NULL) {
		// immediate reporting:
		currentScope.problemReporter().nullityMismatchingTypeAnnotation(argument, argument.resolvedType, expectedType, annotationStatus);
	} else if (annotationStatus.isAnyMismatch() || (statusFromAnnotatedNull & FlowInfo.POTENTIALLY_NULL) != 0) {
		if (!expectedType.hasNullTypeAnnotations() && expectedNonNullness == Boolean.TRUE) {
			// improve problem rendering when using a declaration annotation in a 1.8 setting
			LookupEnvironment env = currentScope.environment();
			expectedType = env.createAnnotatedType(expectedType, new AnnotationBinding[] {env.getNonNullAnnotation()});
		}
		flowContext.recordNullityMismatch(currentScope, argument, argument.resolvedType, expectedType, flowInfo, nullStatus, annotationStatus);
	}
}
/* package */ void checkAgainstNullAnnotation(BlockScope scope, FlowContext flowContext, FlowInfo flowInfo, Expression expr) {
	int nullStatus = expr.nullStatus(flowInfo, flowContext);
	long tagBits;
	MethodBinding methodBinding = null;
	boolean useTypeAnnotations = scope.environment().usesNullTypeAnnotations();
	try {
		methodBinding = scope.methodScope().referenceMethodBinding();
		tagBits = (useTypeAnnotations) ? methodBinding.returnType.tagBits : methodBinding.tagBits;
	} catch (NullPointerException npe) {
		// chain of references in try-block has several potential nulls;
		// any null means we cannot perform the following check
		return;
	}
	if (useTypeAnnotations) {
		checkAgainstNullTypeAnnotation(scope, methodBinding.returnType, expr, flowContext, flowInfo);
	} else if (nullStatus != FlowInfo.NON_NULL) {
		// if we can't prove non-null check against declared null-ness of the enclosing method:
		if ((tagBits & TagBits.AnnotationNonNull) != 0) {
			flowContext.recordNullityMismatch(scope, expr, expr.resolvedType, methodBinding.returnType, flowInfo, nullStatus, null);
		}
	}
}

protected void checkAgainstNullTypeAnnotation(BlockScope scope, TypeBinding requiredType, Expression expression, FlowContext flowContext, FlowInfo flowInfo) {
	if (expression instanceof ConditionalExpression && expression.isPolyExpression()) {
		// drill into both branches using existing nullStatus per branch:
		ConditionalExpression ce = (ConditionalExpression) expression;
		internalCheckAgainstNullTypeAnnotation(scope, requiredType, ce.valueIfTrue, ce.ifTrueNullStatus, flowContext, flowInfo);
		internalCheckAgainstNullTypeAnnotation(scope, requiredType, ce.valueIfFalse, ce.ifFalseNullStatus, flowContext, flowInfo);
		return;
	} else 	if (expression instanceof SwitchExpression && expression.isPolyExpression()) {
		SwitchExpression se = (SwitchExpression) expression;
		for (int i = 0; i < se.resultExpressions.size(); i++) {
			internalCheckAgainstNullTypeAnnotation(scope, requiredType,
					se.resultExpressions.get(i),
					se.resultExpressionNullStatus.get(i), flowContext, flowInfo);
		}
		return;
	}
	int nullStatus = expression.nullStatus(flowInfo, flowContext);
	internalCheckAgainstNullTypeAnnotation(scope, requiredType, expression, nullStatus, flowContext, flowInfo);
}
private void internalCheckAgainstNullTypeAnnotation(BlockScope scope, TypeBinding requiredType, Expression expression,
		int nullStatus, FlowContext flowContext, FlowInfo flowInfo) {
	NullAnnotationMatching annotationStatus = NullAnnotationMatching.analyse(requiredType, expression.resolvedType, null, null, nullStatus, expression, CheckMode.COMPATIBLE);
	if (annotationStatus.isDefiniteMismatch()) {
		scope.problemReporter().nullityMismatchingTypeAnnotation(expression, expression.resolvedType, requiredType, annotationStatus);
	} else {
		if (annotationStatus.wantToReport())
			annotationStatus.report(scope);
		if (annotationStatus.isUnchecked()) {
			flowContext.recordNullityMismatch(scope, expression, expression.resolvedType, requiredType, flowInfo, nullStatus, annotationStatus);
		}
	}
}

/**
 * INTERNAL USE ONLY.
 * This is used to redirect inter-statements jumps.
 */
public void branchChainTo(BranchLabel label) {
	// do nothing by default
}

// Inspect AST nodes looking for a break statement, descending into nested control structures only when necessary (looking for a break with a specific label.)
public boolean breaksOut(final char[] label) {
	return new ASTVisitor() {

		boolean breaksOut;
		@Override
		public boolean visit(TypeDeclaration type, BlockScope skope) { return label != null; }
		@Override
		public boolean visit(TypeDeclaration type, ClassScope skope) { return label != null; }
		@Override
		public boolean visit(LambdaExpression lambda, BlockScope skope) { return label != null;}
		@Override
		public boolean visit(WhileStatement whileStatement, BlockScope skope) { return label != null; }
		@Override
		public boolean visit(DoStatement doStatement, BlockScope skope) { return label != null; }
		@Override
		public boolean visit(ForeachStatement foreachStatement, BlockScope skope) { return label != null; }
		@Override
		public boolean visit(ForStatement forStatement, BlockScope skope) { return label != null; }
		@Override
		public boolean visit(SwitchStatement switchStatement, BlockScope skope) { return label != null; }

		@Override
		public boolean visit(BreakStatement breakStatement, BlockScope skope) {
			if (label == null || CharOperation.equals(label,  breakStatement.label))
				this.breaksOut = true;
	    	return false;
	    }
		@Override
		public boolean visit(YieldStatement yieldStatement, BlockScope skope) {
	    	return false;
	    }
		public boolean breaksOut() {
			Statement.this.traverse(this, null);
			return this.breaksOut;
		}
	}.breaksOut();
}

/* Inspect AST nodes looking for a continue statement with a label, descending into nested control structures.
   The label is presumed to be NOT attached to this. This condition is certainly true for lambda shape analysis
   where this analysis triggers only from do {} while (false); situations. See LabeledStatement.continuesAtOuterLabel
*/
public boolean continuesAtOuterLabel() {
	return new ASTVisitor() {
		boolean continuesToLabel;
		@Override
		public boolean visit(ContinueStatement continueStatement, BlockScope skope) {
			if (continueStatement.label != null)
				this.continuesToLabel = true;
	    	return false;
	    }
		public boolean continuesAtOuterLabel() {
			Statement.this.traverse(this, null);
			return this.continuesToLabel;
		}
	}.continuesAtOuterLabel();
}

// Report an error if necessary (if even more unreachable than previously reported
// complaintLevel = 0 if was reachable up until now, 1 if fake reachable (deadcode), 2 if fatal unreachable (error)
public int complainIfUnreachable(FlowInfo flowInfo, BlockScope scope, int previousComplaintLevel, boolean endOfBlock) {
	if ((flowInfo.reachMode() & FlowInfo.UNREACHABLE) != 0) {
		if ((flowInfo.reachMode() & FlowInfo.UNREACHABLE_OR_DEAD) != 0)
			this.bits &= ~ASTNode.IsReachable;
		if (flowInfo == FlowInfo.DEAD_END) {
			if (previousComplaintLevel < COMPLAINED_UNREACHABLE) {
				if (!this.doNotReportUnreachable())
					scope.problemReporter().unreachableCode(this);
				if (endOfBlock)
					scope.checkUnclosedCloseables(flowInfo, null, null, null);
			}
			return COMPLAINED_UNREACHABLE;
		} else {
			if (previousComplaintLevel < COMPLAINED_FAKE_REACHABLE) {
				scope.problemReporter().fakeReachable(this);
				if (endOfBlock)
					scope.checkUnclosedCloseables(flowInfo, null, null, null);
			}
			return COMPLAINED_FAKE_REACHABLE;
		}
	}
	return previousComplaintLevel;
}

protected boolean doNotReportUnreachable() {
	return false;
}
/**
 * Generate invocation arguments, considering varargs methods
 */
public void generateArguments(MethodBinding binding, Expression[] arguments, BlockScope currentScope, CodeStream codeStream) {
	if (binding.isVarargs()) {
		// 5 possibilities exist for a call to the vararg method foo(int i, int ... value) :
		//      foo(1), foo(1, null), foo(1, 2), foo(1, 2, 3, 4) & foo(1, new int[] {1, 2})
		TypeBinding[] params = binding.parameters;
		int paramLength = params.length;
		int varArgIndex = paramLength - 1;
		for (int i = 0; i < varArgIndex; i++) {
			arguments[i].generateCode(currentScope, codeStream, true);
		}
		ArrayBinding varArgsType = (ArrayBinding) params[varArgIndex]; // parameterType has to be an array type
		ArrayBinding codeGenVarArgsType = (ArrayBinding) binding.parameters[varArgIndex].erasure();
		int elementsTypeID = varArgsType.elementsType().id;
		int argLength = arguments == null ? 0 : arguments.length;

		if (argLength > paramLength) {
			// right number but not directly compatible or too many arguments - wrap extra into array
			// called with (argLength - lastIndex) elements : foo(1, 2) or foo(1, 2, 3, 4)
			// need to gen elements into an array, then gen each remaining element into created array
			codeStream.generateInlinedValue(argLength - varArgIndex);
			codeStream.newArray(codeGenVarArgsType); // create a mono-dimensional array
			for (int i = varArgIndex; i < argLength; i++) {
				codeStream.dup();
				codeStream.generateInlinedValue(i - varArgIndex);
				arguments[i].generateCode(currentScope, codeStream, true);
				codeStream.arrayAtPut(elementsTypeID, false);
			}
		} else if (argLength == paramLength) {
			// right number of arguments - could be inexact - pass argument as is
			TypeBinding lastType = arguments[varArgIndex].resolvedType;
			if (lastType == TypeBinding.NULL
				|| (varArgsType.dimensions() == lastType.dimensions()
					&& lastType.isCompatibleWith(codeGenVarArgsType))) {
				// foo(1, new int[]{2, 3}) or foo(1, null) --> last arg is passed as-is
				arguments[varArgIndex].generateCode(currentScope, codeStream, true);
			} else {
				// right number but not directly compatible or too many arguments - wrap extra into array
				// need to gen elements into an array, then gen each remaining element into created array
				codeStream.generateInlinedValue(1);
				codeStream.newArray(codeGenVarArgsType); // create a mono-dimensional array
				codeStream.dup();
				codeStream.generateInlinedValue(0);
				arguments[varArgIndex].generateCode(currentScope, codeStream, true);
				codeStream.arrayAtPut(elementsTypeID, false);
			}
		} else { // not enough arguments - pass extra empty array
			// scenario: foo(1) --> foo(1, new int[0])
			// generate code for an empty array of parameterType
			codeStream.generateInlinedValue(0);
			codeStream.newArray(codeGenVarArgsType); // create a mono-dimensional array
		}
	} else if (arguments != null) { // standard generation for method arguments
		for (int i = 0, max = arguments.length; i < max; i++)
			arguments[i].generateCode(currentScope, codeStream, true);
	}
}

public abstract void generateCode(BlockScope currentScope, CodeStream codeStream);

public boolean isBoxingCompatible(TypeBinding expressionType, TypeBinding targetType, Expression expression, Scope scope) {
	if (scope.isBoxingCompatibleWith(expressionType, targetType))
		return true;

	return expressionType.isBaseType()  // narrowing then boxing ? Only allowed for some target types see 362279
		&& !targetType.isBaseType()
		&& !targetType.isTypeVariable()
		&& scope.compilerOptions().sourceLevel >= org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants.JDK1_5 // autoboxing
		&& (targetType.id == TypeIds.T_JavaLangByte || targetType.id == TypeIds.T_JavaLangShort || targetType.id == TypeIds.T_JavaLangCharacter)
		&& expression.isConstantValueOfTypeAssignableToType(expressionType, scope.environment().computeBoxingType(targetType));
}

public boolean isEmptyBlock() {
	return false;
}

public boolean isValidJavaStatement() {
	//the use of this method should be avoid in most cases
	//and is here mostly for documentation purpose.....
	//while the parser is responsible for creating
	//welled formed expression statement, which results
	//in the fact that java-non-semantic-expression-used-as-statement
	//should not be parsed...thus not being built.
	//It sounds like the java grammar as help the compiler job in removing
	//-by construction- some statement that would have no effect....
	//(for example all expression that may do side-effects are valid statement
	// -this is an approximative idea.....-)

	return true;
}

@Override
public StringBuffer print(int indent, StringBuffer output) {
	return printStatement(indent, output);
}

public abstract StringBuffer printStatement(int indent, StringBuffer output);

public abstract void resolve(BlockScope scope);
public LocalVariableBinding[] getPatternVariablesWhenTrue() {
	return this.patternVarsWhenTrue;
}
public LocalVariableBinding[] getPatternVariablesWhenFalse() {
	return this.patternVarsWhenFalse;
}
public void addPatternVariablesWhenTrue(LocalVariableBinding[] vars) {
	this.patternVarsWhenTrue = addPatternVariables(this.patternVarsWhenTrue, vars);
}
public void addPatternVariablesWhenFalse(LocalVariableBinding[] vars) {
	this.patternVarsWhenFalse = addPatternVariables(this.patternVarsWhenFalse, vars);
}
private LocalVariableBinding[] addPatternVariables(LocalVariableBinding[] current, LocalVariableBinding[] add) {
	if (add == null || add.length == 0)
		return current;
	if (current == null) {
		current = add;
	} else {
		for (LocalVariableBinding local : add) {
			current = addPatternVariables(current, local);
		}
	}
	return current;
}
private LocalVariableBinding[] addPatternVariables(LocalVariableBinding[] current, LocalVariableBinding add) {
	int oldSize = current.length;
	// it's odd that we only look at the last element, but in most cases
	// we will only have one in the array. In the unlikely case of having two
	// distinct pattern variables, the cost is nothing but setting the same
	// bit twice on the same object.
	if (oldSize > 0 && current[oldSize - 1] == add) {
		return current;
	}
	int newLength = current.length + 1;
	System.arraycopy(current, 0, (current = new LocalVariableBinding[newLength]), 0, oldSize);
	current[oldSize] = add;
	return current;
}
public void promotePatternVariablesIfApplicable(LocalVariableBinding[] patternVariablesInScope, BooleanSupplier condition) {
	if (patternVariablesInScope != null && condition.getAsBoolean()) {
		for (LocalVariableBinding binding : patternVariablesInScope) {
			binding.modifiers &= ~ExtraCompilerModifiers.AccPatternVariable;
		}
	}
}
public void resolveWithPatternVariablesInScope(LocalVariableBinding[] patternVariablesInScope, BlockScope scope) {
	if (patternVariablesInScope != null) {
		for (LocalVariableBinding binding : patternVariablesInScope) {
			binding.modifiers &= ~ExtraCompilerModifiers.AccPatternVariable;
		}
		this.resolve(scope);
		for (LocalVariableBinding binding : patternVariablesInScope) {
			binding.modifiers |= ExtraCompilerModifiers.AccPatternVariable;
		}
	} else {
		resolve(scope);
	}
}
/**
 * Returns the resolved expression if any associated to this statement - used
 * parameter statement has to be either a SwitchStatement or a SwitchExpression
 */
public TypeBinding resolveExpressionType(BlockScope scope) {
	return null;
}
public boolean containsPatternVariable() {
	return false;
}
/**
 * Implementation of {@link org.eclipse.jdt.internal.compiler.lookup.InvocationSite#invocationTargetType}
 * suitable at this level. Subclasses should override as necessary.
 * @see org.eclipse.jdt.internal.compiler.lookup.InvocationSite#invocationTargetType()
 */
public TypeBinding invocationTargetType() {
	return null;
}
/** Simpler notion of expected type, suitable for code assist purposes. */
public TypeBinding expectedType() {
	// for all but FunctionalExpressions, this is the same as invocationTargetType.
	return invocationTargetType();
}
public ExpressionContext getExpressionContext() {
	return ExpressionContext.VANILLA_CONTEXT;
}
/**
 * For all constructor invocations: find the constructor binding;
 * if site.innersNeedUpdate() perform some post processing for those and produce
 * any updates as side-effects into 'argumentTypes'.
 */
protected MethodBinding findConstructorBinding(BlockScope scope, Invocation site, ReferenceBinding receiverType, TypeBinding[] argumentTypes) {
	MethodBinding ctorBinding = scope.getConstructor(receiverType, argumentTypes, site);
	return resolvePolyExpressionArguments(site, ctorBinding, argumentTypes, scope);
}
}
