/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     							bug 332637 - Dead Code detection removing code that isn't dead
 *     							bug 358827 - [1.7] exception analysis for t-w-r spoils null analysis
 *     							bug 349326 - [1.7] new warning for missing try-with-resources
 *     							bug 359334 - Analysis for resource leak warnings does not consider exceptions as method exit points
 *								bug 358903 - Filter practically unimportant resource leak warnings
 *								bug 345305 - [compiler][null] Compiler misidentifies a case of "variable can only be null"
 *								bug 388996 - [compiler][resource] Incorrect 'potential resource leak'
 *								bug 401088 - [compiler][null] Wrong warning "Redundant null check" inside nested try statement
 *								bug 401092 - [compiler][null] Wrong warning "Redundant null check" in outer catch of nested try
 *								bug 402993 - [null] Follow up of bug 401088: Missing warning about redundant null check
 *								bug 384380 - False positive on a ?? Potential null pointer access ?? after a continue
 *								Bug 415790 - [compiler][resource]Incorrect potential resource leak warning in for loop with close in try/catch
 *								Bug 371614 - [compiler][resource] Wrong "resource leak" problem on return/throw inside while loop
 *								Bug 444964 - [1.7+][resource] False resource leak warning (try-with-resources for ByteArrayOutputStream - return inside for loop)
 *     Jesper Steen Moller - Contributions for
 *								bug 404146 - [1.7][compiler] nested try-catch-finally-blocks leads to unrunnable Java byte code
 *     Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class TryStatement extends SubRoutineStatement {

	static final char[] SECRET_RETURN_ADDRESS_NAME = " returnAddress".toCharArray(); //$NON-NLS-1$
	static final char[] SECRET_ANY_HANDLER_NAME = " anyExceptionHandler".toCharArray(); //$NON-NLS-1$
	static final char[] SECRET_PRIMARY_EXCEPTION_VARIABLE_NAME = " primaryException".toCharArray(); //$NON-NLS-1$
	static final char[] SECRET_CAUGHT_THROWABLE_VARIABLE_NAME = " caughtThrowable".toCharArray(); //$NON-NLS-1$;
	static final char[] SECRET_RETURN_VALUE_NAME = " returnValue".toCharArray(); //$NON-NLS-1$

	public Statement[] resources = new Statement[0];
	public Block tryBlock;
	public Block[] catchBlocks;

	public Argument[] catchArguments;

	public Block finallyBlock;
	BlockScope scope;

	public UnconditionalFlowInfo subRoutineInits;
	ReferenceBinding[] caughtExceptionTypes;
	boolean[] catchExits;

	BranchLabel subRoutineStartLabel;
	public LocalVariableBinding anyExceptionVariable,
		returnAddressVariable,
		secretReturnValue;

	ExceptionLabel[] declaredExceptionLabels; // only set while generating code

	// for inlining/optimizing JSR instructions
	private Object[] reusableJSRTargets;
	private BranchLabel[] reusableJSRSequenceStartLabels;
	private int[] reusableJSRStateIndexes;
	private int reusableJSRTargetsCount = 0;

	private static final int NO_FINALLY = 0;										// no finally block
	private static final int FINALLY_SUBROUTINE = 1; 					// finally is generated as a subroutine (using jsr/ret bytecodes)
	private static final int FINALLY_DOES_NOT_COMPLETE = 2;		// non returning finally is optimized with only one instance of finally block
	private static final int FINALLY_INLINE = 3;								// finally block must be inlined since cannot use jsr/ret bytecodes >1.5

	// for local variables table attributes
	int mergedInitStateIndex = -1;
	int preTryInitStateIndex = -1;
	int postTryInitStateIndex = -1;
	int[] postResourcesInitStateIndexes;
	int naturalExitMergeInitStateIndex = -1;
	int[] catchExitInitStateIndexes;
	private LocalVariableBinding primaryExceptionVariable;
	private LocalVariableBinding caughtThrowableVariable;
	private ExceptionLabel[] resourceExceptionLabels;
	private int[] caughtExceptionsCatchBlocks;

	public SwitchExpression enclosingSwitchExpression = null;
@Override
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {

	// Consider the try block and catch block so as to compute the intersection of initializations and
	// the minimum exit relative depth amongst all of them. Then consider the subroutine, and append its
	// initialization to the try/catch ones, if the subroutine completes normally. If the subroutine does not
	// complete, then only keep this result for the rest of the analysis

	// process the finally block (subroutine) - create a context for the subroutine

	this.preTryInitStateIndex =
		currentScope.methodScope().recordInitializationStates(flowInfo);

	if (this.anyExceptionVariable != null) {
		this.anyExceptionVariable.useFlag = LocalVariableBinding.USED;
	}
	if (this.primaryExceptionVariable != null) {
		this.primaryExceptionVariable.useFlag = LocalVariableBinding.USED;
	}
	if (this.caughtThrowableVariable != null) {
		this.caughtThrowableVariable.useFlag = LocalVariableBinding.USED;
	}
	if (this.returnAddressVariable != null) { // TODO (philippe) if subroutine is escaping, unused
		this.returnAddressVariable.useFlag = LocalVariableBinding.USED;
	}
	int resourcesLength = this.resources.length;
	if (resourcesLength > 0) {
		this.postResourcesInitStateIndexes = new int[resourcesLength];
	}


	if (this.subRoutineStartLabel == null) {
		// no finally block -- this is a simplified copy of the else part
		if (flowContext instanceof FinallyFlowContext) {
			// if this TryStatement sits inside another TryStatement, establish the wiring so that
			// FlowContext.markFinallyNullStatus can report into initsOnFinally of the outer try context:
			FinallyFlowContext finallyContext = (FinallyFlowContext) flowContext;
			finallyContext.outerTryContext = finallyContext.tryContext;
		}
		// process the try block in a context handling the local exceptions.
		ExceptionHandlingFlowContext handlingContext =
			new ExceptionHandlingFlowContext(
				flowContext,
				this,
				this.caughtExceptionTypes,
				this.caughtExceptionsCatchBlocks,
				null,
				this.scope,
				flowInfo);
		handlingContext.conditionalLevel = 0; // start collection initsOnFinally
		// only try blocks initialize that member - may consider creating a
		// separate class if needed

		FlowInfo tryInfo = flowInfo.copy();
		for (int i = 0; i < resourcesLength; i++) {
			final Statement resource = this.resources[i];
			tryInfo = resource.analyseCode(currentScope, handlingContext, tryInfo);
			this.postResourcesInitStateIndexes[i] = currentScope.methodScope().recordInitializationStates(tryInfo);
			TypeBinding resolvedType = null;
			LocalVariableBinding localVariableBinding = null;
			if (resource instanceof LocalDeclaration) {
				localVariableBinding = ((LocalDeclaration) resource).binding;
				resolvedType = localVariableBinding.type;
				if (localVariableBinding.closeTracker != null) {
					// this was false alarm, we don't need to track the resource
					localVariableBinding.closeTracker.withdraw();
					localVariableBinding.closeTracker = null;
				}
			} else { //expression
				if (resource instanceof NameReference && ((NameReference) resource).binding instanceof LocalVariableBinding) {
					localVariableBinding = (LocalVariableBinding) ((NameReference) resource).binding;
				}
				resolvedType = ((Expression) resource).resolvedType;
				recordCallingClose(currentScope, flowContext, tryInfo, (Expression)resource);
			}
			if (localVariableBinding != null) {
				localVariableBinding.useFlag = LocalVariableBinding.USED; // Is implicitly used anyways.
			}
			MethodBinding closeMethod = findCloseMethod(resource, resolvedType);
			if (closeMethod != null && closeMethod.isValidBinding() && closeMethod.returnType.id == TypeIds.T_void) {
				ReferenceBinding[] thrownExceptions = closeMethod.thrownExceptions;
				for (int j = 0, length = thrownExceptions.length; j < length; j++) {
					handlingContext.checkExceptionHandlers(thrownExceptions[j], this.resources[i], tryInfo, currentScope, true);
				}
			}
		}
		if (!this.tryBlock.isEmptyBlock()) {
			tryInfo = this.tryBlock.analyseCode(currentScope, handlingContext, tryInfo);
			if ((tryInfo.tagBits & FlowInfo.UNREACHABLE_OR_DEAD) != 0)
				this.bits |= ASTNode.IsTryBlockExiting;
		}
		if (resourcesLength > 0) {
			this.postTryInitStateIndex = currentScope.methodScope().recordInitializationStates(tryInfo);
			// the resources are not in scope after the try block, so remove their assignment info
			// to avoid polluting the state indices. However, do this after the postTryInitStateIndex is calculated since
			// it is used to add or remove assigned resources during code gen
			for (int i = 0; i < resourcesLength; i++) {
				if (this.resources[i] instanceof LocalDeclaration)
				tryInfo.resetAssignmentInfo(((LocalDeclaration) this.resources[i]).binding);
			}
		}
		// check unreachable catch blocks
		handlingContext.complainIfUnusedExceptionHandlers(this.scope, this);

		// process the catch blocks - computing the minimal exit depth amongst try/catch
		if (this.catchArguments != null) {
			int catchCount;
			this.catchExits = new boolean[catchCount = this.catchBlocks.length];
			this.catchExitInitStateIndexes = new int[catchCount];
			for (int i = 0; i < catchCount; i++) {
				// keep track of the inits that could potentially have led to this exception handler (for final assignments diagnosis)
				FlowInfo catchInfo = prepareCatchInfo(flowInfo, handlingContext, tryInfo, i);
				flowContext.conditionalLevel++;
				catchInfo =
					this.catchBlocks[i].analyseCode(
						currentScope,
						flowContext,
						catchInfo);
				flowContext.conditionalLevel--;
				this.catchExitInitStateIndexes[i] = currentScope.methodScope().recordInitializationStates(catchInfo);
				this.catchExits[i] =
					(catchInfo.tagBits & FlowInfo.UNREACHABLE_OR_DEAD) != 0;
				tryInfo = tryInfo.mergedWith(catchInfo.unconditionalInits());
			}
		}
		this.mergedInitStateIndex =
			currentScope.methodScope().recordInitializationStates(tryInfo);

		// chain up null info registry
		flowContext.mergeFinallyNullInfo(handlingContext.initsOnFinally);

		return tryInfo;
	} else {
		InsideSubRoutineFlowContext insideSubContext;
		FinallyFlowContext finallyContext;
		UnconditionalFlowInfo subInfo;
		// analyse finally block first
		insideSubContext = new InsideSubRoutineFlowContext(flowContext, this);
		if (flowContext instanceof FinallyFlowContext) {
			// if this TryStatement sits inside another TryStatement, establish the wiring so that
			// FlowContext.markFinallyNullStatus can report into initsOnFinally of the outer try context:
			insideSubContext.outerTryContext = ((FinallyFlowContext)flowContext).tryContext;
		}

		// process the try block in a context handling the local exceptions.
		// (advance instantiation so we can wire this into the FinallyFlowContext)
		ExceptionHandlingFlowContext handlingContext =
			new ExceptionHandlingFlowContext(
				insideSubContext,
				this,
				this.caughtExceptionTypes,
				this.caughtExceptionsCatchBlocks,
				null,
				this.scope,
				flowInfo);
		insideSubContext.initsOnFinally = handlingContext.initsOnFinally;

		subInfo =
			this.finallyBlock
				.analyseCode(
					currentScope,
					finallyContext = new FinallyFlowContext(flowContext, this.finallyBlock, handlingContext),
					flowInfo.nullInfoLessUnconditionalCopy())
				.unconditionalInits();
		handlingContext.conditionalLevel = 0; // start collection initsOnFinally only after analysing the finally block
		if (subInfo == FlowInfo.DEAD_END) {
			this.bits |= ASTNode.IsSubRoutineEscaping;
			this.scope.problemReporter().finallyMustCompleteNormally(this.finallyBlock);
		} else {
			// for resource analysis we need the finallyInfo in these nested scopes:
			FlowInfo finallyInfo = subInfo.copy();
			this.tryBlock.scope.finallyInfo = finallyInfo;
			if (this.catchBlocks != null) {
				for (int i = 0; i < this.catchBlocks.length; i++)
					this.catchBlocks[i].scope.finallyInfo = finallyInfo;
			}
		}
		this.subRoutineInits = subInfo;
		// only try blocks initialize that member - may consider creating a
		// separate class if needed

		FlowInfo tryInfo = flowInfo.copy();
		for (int i = 0; i < resourcesLength; i++) {
			final Statement resource = this.resources[i];
			tryInfo = resource.analyseCode(currentScope, handlingContext, tryInfo);
			this.postResourcesInitStateIndexes[i] = currentScope.methodScope().recordInitializationStates(tryInfo);
			TypeBinding resolvedType = null;
			LocalVariableBinding localVariableBinding = null;
			if (resource instanceof LocalDeclaration) {
				localVariableBinding = ((LocalDeclaration) this.resources[i]).binding;
				resolvedType = localVariableBinding.type;
				if (localVariableBinding.closeTracker != null) {
					// this was false alarm, we don't need to track the resource
					localVariableBinding.closeTracker.withdraw();
					// keep the tracking variable in the resourceBinding in order to prevent creating a new one while analyzing the try block
				}
			} else { // Expression
				if (resource instanceof NameReference && ((NameReference) resource).binding instanceof LocalVariableBinding) {
					localVariableBinding = (LocalVariableBinding)((NameReference) resource).binding;
				}
				recordCallingClose(currentScope, flowContext, tryInfo, (Expression)resource);
				resolvedType = ((Expression) resource).resolvedType;
			}
			if (localVariableBinding != null) {
				localVariableBinding.useFlag = LocalVariableBinding.USED; // Is implicitly used anyways.
			}
			MethodBinding closeMethod = findCloseMethod(resource, resolvedType);
			if (closeMethod != null && closeMethod.isValidBinding() && closeMethod.returnType.id == TypeIds.T_void) {
				ReferenceBinding[] thrownExceptions = closeMethod.thrownExceptions;
				for (int j = 0, length = thrownExceptions.length; j < length; j++) {
					handlingContext.checkExceptionHandlers(thrownExceptions[j], this.resources[i], tryInfo, currentScope, true);
				}
			}
		}
		if (!this.tryBlock.isEmptyBlock()) {
			tryInfo = this.tryBlock.analyseCode(currentScope, handlingContext, tryInfo);
			if ((tryInfo.tagBits & FlowInfo.UNREACHABLE_OR_DEAD) != 0)
				this.bits |= ASTNode.IsTryBlockExiting;
		}
		if (resourcesLength > 0) {
			this.postTryInitStateIndex = currentScope.methodScope().recordInitializationStates(tryInfo);
			// the resources are not in scope after the try block, so remove their assignment info
			// to avoid polluting the state indices. However, do this after the postTryInitStateIndex is calculated since
			// it is used to add or remove assigned resources during code gen
			for (int i = 0; i < resourcesLength; i++) {
				if (this.resources[i] instanceof LocalDeclaration)
					tryInfo.resetAssignmentInfo(((LocalDeclaration)this.resources[i]).binding);
			}
		}
		// check unreachable catch blocks
		handlingContext.complainIfUnusedExceptionHandlers(this.scope, this);

		// process the catch blocks - computing the minimal exit depth amongst try/catch
		if (this.catchArguments != null) {
			int catchCount;
			this.catchExits = new boolean[catchCount = this.catchBlocks.length];
			this.catchExitInitStateIndexes = new int[catchCount];
			for (int i = 0; i < catchCount; i++) {
				// keep track of the inits that could potentially have led to this exception handler (for final assignments diagnosis)
				FlowInfo catchInfo = prepareCatchInfo(flowInfo, handlingContext, tryInfo, i);
				insideSubContext.conditionalLevel = 1;
				catchInfo =
					this.catchBlocks[i].analyseCode(
						currentScope,
						insideSubContext,
						catchInfo);
				this.catchExitInitStateIndexes[i] = currentScope.methodScope().recordInitializationStates(catchInfo);
				this.catchExits[i] =
					(catchInfo.tagBits & FlowInfo.UNREACHABLE_OR_DEAD) != 0;
				tryInfo = tryInfo.mergedWith(catchInfo.unconditionalInits());
			}
		}
		// we also need to check potential multiple assignments of final variables inside the finally block
		// need to include potential inits from returns inside the try/catch parts - 1GK2AOF
		finallyContext.complainOnDeferredChecks(
			((tryInfo.tagBits & FlowInfo.UNREACHABLE) == 0 ?
				flowInfo.unconditionalCopy().
					addPotentialInitializationsFrom(tryInfo).
					// lighten the influence of the try block, which may have
					// exited at any point
					addPotentialInitializationsFrom(insideSubContext.initsOnReturn) :
				insideSubContext.initsOnReturn).
			addNullInfoFrom(
					handlingContext.initsOnFinally),
			currentScope);

		// chain up null info registry
		flowContext.mergeFinallyNullInfo(handlingContext.initsOnFinally);

		this.naturalExitMergeInitStateIndex =
			currentScope.methodScope().recordInitializationStates(tryInfo);
		if (subInfo == FlowInfo.DEAD_END) {
			this.mergedInitStateIndex =
				currentScope.methodScope().recordInitializationStates(subInfo);
			return subInfo;
		} else {
			FlowInfo mergedInfo = tryInfo.addInitializationsFrom(subInfo);
			this.mergedInitStateIndex =
				currentScope.methodScope().recordInitializationStates(mergedInfo);
			return mergedInfo;
		}
	}
}
private void recordCallingClose(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo, Expression closeTarget) {
	FakedTrackingVariable trackingVariable = FakedTrackingVariable.getCloseTrackingVariable(closeTarget, flowInfo, flowContext);
	if (trackingVariable != null) { // null happens if target is not a local variable or not an AutoCloseable
		if (trackingVariable.methodScope == currentScope.methodScope()) {
			trackingVariable.markClose(flowInfo, flowContext);
		} else {
			trackingVariable.markClosedInNestedMethod();
		}
		trackingVariable.markClosedEffectivelyFinal();
	}
}
private MethodBinding findCloseMethod(final ASTNode resource, TypeBinding type) {
	MethodBinding closeMethod = null;
	if (type != null && type.isValidBinding() && type instanceof ReferenceBinding) {
		ReferenceBinding binding = (ReferenceBinding) type;
		closeMethod = binding.getExactMethod(ConstantPool.Close, new TypeBinding [0], this.scope.compilationUnitScope()); // scope needs to be tighter
		if(closeMethod == null) {
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=380112
			// closeMethod could be null if the binding is from an interface
			// extending from multiple interfaces.
			InvocationSite site = new InvocationSite.EmptyWithAstNode(resource);
			closeMethod = this.scope.compilationUnitScope().findMethod(binding, ConstantPool.Close, new TypeBinding[0], site, false);
		}
	}
	return closeMethod;
}
private FlowInfo prepareCatchInfo(FlowInfo flowInfo, ExceptionHandlingFlowContext handlingContext, FlowInfo tryInfo, int i) {
	FlowInfo catchInfo;
	if (isUncheckedCatchBlock(i)) {
		catchInfo =
			flowInfo.unconditionalCopy().
				addPotentialInitializationsFrom(
					handlingContext.initsOnException(i)).
				addPotentialInitializationsFrom(tryInfo).
				addPotentialInitializationsFrom(
					handlingContext.initsOnReturn).
			addNullInfoFrom(handlingContext.initsOnFinally);
	} else {
		FlowInfo initsOnException = handlingContext.initsOnException(i);
		catchInfo =
			flowInfo.nullInfoLessUnconditionalCopy()
				.addPotentialInitializationsFrom(initsOnException)
				.addNullInfoFrom(initsOnException) // <<== Null info only from here!
				.addPotentialInitializationsFrom(
						tryInfo.nullInfoLessUnconditionalCopy())
				.addPotentialInitializationsFrom(
						handlingContext.initsOnReturn.nullInfoLessUnconditionalCopy());
	}

	// catch var is always set
	LocalVariableBinding catchArg = this.catchArguments[i].binding;
	catchInfo.markAsDefinitelyAssigned(catchArg);
	catchInfo.markAsDefinitelyNonNull(catchArg);
	/*
	"If we are about to consider an unchecked exception handler, potential inits may have occured inside
	the try block that need to be detected , e.g.
	try { x = 1; throwSomething();} catch(Exception e){ x = 2} "
	"(uncheckedExceptionTypes notNil and: [uncheckedExceptionTypes at: index])
	ifTrue: [catchInits addPotentialInitializationsFrom: tryInits]."
	*/
	if (this.tryBlock.statements == null && this.resources == null) { // https://bugs.eclipse.org/bugs/show_bug.cgi?id=350579
		catchInfo.setReachMode(FlowInfo.UNREACHABLE_OR_DEAD);
	}
	return catchInfo;
}
// Return true if the catch block corresponds to an unchecked exception making allowance for multi-catch blocks.
private boolean isUncheckedCatchBlock(int catchBlock) {
	if (this.caughtExceptionsCatchBlocks == null) {
		return this.caughtExceptionTypes[catchBlock].isUncheckedException(true);
	}
	for (int i = 0, length = this.caughtExceptionsCatchBlocks.length; i < length; i++) {
		if (this.caughtExceptionsCatchBlocks[i] == catchBlock) {
			if (this.caughtExceptionTypes[i].isUncheckedException(true)) {
				return true;
			}
		}
	}
	return false;
}

@Override
public ExceptionLabel enterAnyExceptionHandler(CodeStream codeStream) {
	if (this.subRoutineStartLabel == null)
		return null;
	return super.enterAnyExceptionHandler(codeStream);
}

@Override
public void enterDeclaredExceptionHandlers(CodeStream codeStream) {
	for (int i = 0, length = this.declaredExceptionLabels == null ? 0 : this.declaredExceptionLabels.length; i < length; i++) {
		this.declaredExceptionLabels[i].placeStart();
	}
	int resourceCount = this.resources.length;
	if (resourceCount > 0 && this.resourceExceptionLabels != null) { // https://bugs.eclipse.org/bugs/show_bug.cgi?id=375248
		// Reinstall handlers
		for (int i = resourceCount; i >= 0; --i) {
			this.resourceExceptionLabels[i].placeStart();
		}
	}
}

@Override
public void exitAnyExceptionHandler() {
	if (this.subRoutineStartLabel == null)
		return;
	super.exitAnyExceptionHandler();
}

@Override
public void exitDeclaredExceptionHandlers(CodeStream codeStream) {
	for (int i = 0, length = this.declaredExceptionLabels == null ? 0 : this.declaredExceptionLabels.length; i < length; i++) {
		this.declaredExceptionLabels[i].placeEnd();
	}
}

private int finallyMode() {
	if (this.subRoutineStartLabel == null) {
		return NO_FINALLY;
	} else if (isSubRoutineEscaping()) {
		return FINALLY_DOES_NOT_COMPLETE;
	} else if (this.scope.compilerOptions().inlineJsrBytecode) {
		return FINALLY_INLINE;
	} else {
		return FINALLY_SUBROUTINE;
	}
}
/**
 * Try statement code generation with or without jsr bytecode use
 *	post 1.5 target level, cannot use jsr bytecode, must instead inline finally block
 * returnAddress is only allocated if jsr is allowed
 */
@Override
public void generateCode(BlockScope currentScope, CodeStream codeStream) {
	if ((this.bits & ASTNode.IsReachable) == 0) {
		return;
	}
	boolean isStackMapFrameCodeStream = codeStream instanceof StackMapFrameCodeStream;
	// in case the labels needs to be reinitialized
	// when the code generation is restarted in wide mode
	this.anyExceptionLabel = null;
	this.reusableJSRTargets = null;
	this.reusableJSRSequenceStartLabels = null;
	this.reusableJSRTargetsCount = 0;

	int pc = codeStream.position;
	int finallyMode = finallyMode();

	boolean requiresNaturalExit = false;
	// preparing exception labels
	int maxCatches = this.catchArguments == null ? 0 : this.catchArguments.length;
	ExceptionLabel[] exceptionLabels;
	if (maxCatches > 0) {
		exceptionLabels = new ExceptionLabel[maxCatches];
		for (int i = 0; i < maxCatches; i++) {
			Argument argument = this.catchArguments[i];
			ExceptionLabel exceptionLabel = null;
			if ((argument.binding.tagBits & TagBits.MultiCatchParameter) != 0) {
				MultiCatchExceptionLabel multiCatchExceptionLabel = new MultiCatchExceptionLabel(codeStream, argument.binding.type);
				multiCatchExceptionLabel.initialize((UnionTypeReference) argument.type, argument.annotations);
				exceptionLabel = multiCatchExceptionLabel;
			} else {
				exceptionLabel = new ExceptionLabel(codeStream, argument.binding.type, argument.type, argument.annotations);
			}
			exceptionLabel.placeStart();
			exceptionLabels[i] = exceptionLabel;
		}
	} else {
		exceptionLabels = null;
	}
	if (this.subRoutineStartLabel != null) {
		this.subRoutineStartLabel.initialize(codeStream);
		enterAnyExceptionHandler(codeStream);
	}
	// generate the try block
	try {
		this.declaredExceptionLabels = exceptionLabels;
		int resourceCount = this.resources.length;
		if (resourceCount > 0) {
			// Please see https://bugs.eclipse.org/bugs/show_bug.cgi?id=338402#c16
			this.resourceExceptionLabels = new ExceptionLabel[resourceCount + 1];
			codeStream.aconst_null();
			codeStream.store(this.primaryExceptionVariable, false /* value not required */);
			codeStream.addVariable(this.primaryExceptionVariable);
			codeStream.aconst_null();
			codeStream.store(this.caughtThrowableVariable, false /* value not required */);
			codeStream.addVariable(this.caughtThrowableVariable);
			for (int i = 0; i <= resourceCount; i++) {
				// put null for the exception type to treat them as any exception handlers (equivalent to a try/finally)
				this.resourceExceptionLabels[i] = new ExceptionLabel(codeStream, null);
				this.resourceExceptionLabels[i].placeStart();
				if (i < resourceCount) {
					Statement stmt = this.resources[i];
					if (stmt instanceof NameReference) {
						NameReference ref = (NameReference) stmt;
						ref.bits |= ASTNode.IsCapturedOuterLocal; // TODO: selective flagging if ref.binding is not one of earlier inlined LVBs.
						VariableBinding binding = (VariableBinding) ref.binding; // Only LVB expected here.
						ref.checkEffectiveFinality(binding, this.scope);
					} else if (stmt instanceof FieldReference) {
						FieldReference fieldReference = (FieldReference) stmt;
						if (!fieldReference.binding.isFinal())
							this.scope.problemReporter().cannotReferToNonFinalField(fieldReference.binding, fieldReference);
					}
					stmt.generateCode(this.scope, codeStream); // Initialize resources ...
				}
			}
		}
		this.tryBlock.generateCode(this.scope, codeStream);
		if (resourceCount > 0) {
			for (int i = resourceCount; i >= 0; i--) {
				BranchLabel exitLabel = new BranchLabel(codeStream);
				this.resourceExceptionLabels[i].placeEnd(); // outer handler if any is the one that should catch exceptions out of close()

				Statement stmt = i > 0 ? this.resources[i - 1] : null;
				if ((this.bits & ASTNode.IsTryBlockExiting) == 0) {
					// inline resource closure
					if (i > 0) {
						int invokeCloseStartPc = codeStream.position; // https://bugs.eclipse.org/bugs/show_bug.cgi?id=343785
						if (this.postTryInitStateIndex != -1) {
							/* https://bugs.eclipse.org/bugs/show_bug.cgi?id=361053, we are just past a synthetic instance of try-catch-finally.
							   Our initialization type state is the same as it was at the end of the just concluded try (catch rethrows)
							*/
							codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.postTryInitStateIndex);
							codeStream.addDefinitelyAssignedVariables(currentScope, this.postTryInitStateIndex);
						}
						generateCodeSnippet(stmt, codeStream, exitLabel, false /* record */);
						codeStream.recordPositionsFrom(invokeCloseStartPc, this.tryBlock.sourceEnd);
					}
					codeStream.goto_(exitLabel); // skip over the catch block.
				}

				if (i > 0) {
					// i is off by one
					codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.postResourcesInitStateIndexes[i - 1]);
					codeStream.addDefinitelyAssignedVariables(currentScope, this.postResourcesInitStateIndexes[i - 1]);
				} else {
					// For the first resource, its preset state is the preTryInitStateIndex
					codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.preTryInitStateIndex);
					codeStream.addDefinitelyAssignedVariables(currentScope, this.preTryInitStateIndex);
				}

				codeStream.pushExceptionOnStack(this.scope.getJavaLangThrowable());
				this.resourceExceptionLabels[i].place();
				if (i == resourceCount) {
					// inner most try's catch/finally can be a lot simpler.
					codeStream.store(this.primaryExceptionVariable, false);
					// fall through, invoke close() and re-throw.
				} else {
					BranchLabel elseLabel = new BranchLabel(codeStream), postElseLabel = new BranchLabel(codeStream);
					codeStream.store(this.caughtThrowableVariable, false);
					codeStream.load(this.primaryExceptionVariable);
					codeStream.ifnonnull(elseLabel);
					codeStream.load(this.caughtThrowableVariable);
					codeStream.store(this.primaryExceptionVariable, false);
					codeStream.goto_(postElseLabel);
					elseLabel.place();
					codeStream.load(this.primaryExceptionVariable);
					codeStream.load(this.caughtThrowableVariable);
					codeStream.if_acmpeq(postElseLabel);
					codeStream.load(this.primaryExceptionVariable);
					codeStream.load(this.caughtThrowableVariable);
					codeStream.invokeThrowableAddSuppressed();
					postElseLabel.place();
				}
				if (i > 0) {
					// inline resource close here rather than bracketing the current catch block with a try region.
					BranchLabel postCloseLabel = new BranchLabel(codeStream);
					generateCodeSnippet(stmt, codeStream, postCloseLabel, true /* record */, i, codeStream.position);
					postCloseLabel.place();
				}
				codeStream.load(this.primaryExceptionVariable);
				codeStream.athrow();
				exitLabel.place();
			}
			codeStream.removeVariable(this.primaryExceptionVariable);
			codeStream.removeVariable(this.caughtThrowableVariable);
		}
	} finally {
		this.declaredExceptionLabels = null;
		this.resourceExceptionLabels = null;  // https://bugs.eclipse.org/bugs/show_bug.cgi?id=375248
	}
	boolean tryBlockHasSomeCode = codeStream.position != pc;
	// flag telling if some bytecodes were issued inside the try block

	// place end positions of user-defined exception labels
	if (tryBlockHasSomeCode) {
		// natural exit may require subroutine invocation (if finally != null)
		BranchLabel naturalExitLabel = new BranchLabel(codeStream);
		BranchLabel postCatchesFinallyLabel = null;
		for (int i = 0; i < maxCatches; i++) {
			exceptionLabels[i].placeEnd();
		}
		if ((this.bits & ASTNode.IsTryBlockExiting) == 0) {
			int position = codeStream.position;
			switch(finallyMode) {
				case FINALLY_SUBROUTINE :
				case FINALLY_INLINE :
					requiresNaturalExit = true;
					if (this.naturalExitMergeInitStateIndex != -1) {
						codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.naturalExitMergeInitStateIndex);
						codeStream.addDefinitelyAssignedVariables(currentScope, this.naturalExitMergeInitStateIndex);
					}
					codeStream.goto_(naturalExitLabel);
					break;
				case NO_FINALLY :
					if (this.naturalExitMergeInitStateIndex != -1) {
						codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.naturalExitMergeInitStateIndex);
						codeStream.addDefinitelyAssignedVariables(currentScope, this.naturalExitMergeInitStateIndex);
					}
					codeStream.goto_(naturalExitLabel);
					break;
				case FINALLY_DOES_NOT_COMPLETE :
					codeStream.goto_(this.subRoutineStartLabel);
					break;
			}
			codeStream.recordPositionsFrom(position, this.tryBlock.sourceEnd);
			//goto is tagged as part of the try block
		}
		/* generate sequence of handler, all starting by storing the TOS (exception
		thrown) into their own catch variables, the one specified in the source
		that must denote the handled exception.
		*/
		exitAnyExceptionHandler();
		if (this.catchArguments != null) {
			postCatchesFinallyLabel = new BranchLabel(codeStream);

			for (int i = 0; i < maxCatches; i++) {
				/*
				 * This should not happen. For consistency purpose, if the exception label is never used
				 * we also don't generate the corresponding catch block, otherwise we have some
				 * unreachable bytecodes
				 */
				if (exceptionLabels[i].getCount() == 0) continue;
				enterAnyExceptionHandler(codeStream);
				// May loose some local variable initializations : affecting the local variable attributes
				if (this.preTryInitStateIndex != -1) {
					codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.preTryInitStateIndex);
					codeStream.addDefinitelyAssignedVariables(currentScope, this.preTryInitStateIndex);
				}
				codeStream.pushExceptionOnStack(exceptionLabels[i].exceptionType);
				exceptionLabels[i].place();
				// optimizing the case where the exception variable is not actually used
				LocalVariableBinding catchVar;
				int varPC = codeStream.position;
				if ((catchVar = this.catchArguments[i].binding).resolvedPosition != -1) {
					codeStream.store(catchVar, false);
					catchVar.recordInitializationStartPC(codeStream.position);
					codeStream.addVisibleLocalVariable(catchVar);
				} else {
					codeStream.pop();
				}
				codeStream.recordPositionsFrom(varPC, this.catchArguments[i].sourceStart);
				// Keep track of the pcs at diverging point for computing the local attribute
				// since not passing the catchScope, the block generation will exitUserScope(catchScope)
				this.catchBlocks[i].generateCode(this.scope, codeStream);
				exitAnyExceptionHandler();
				if (!this.catchExits[i]) {
					switch(finallyMode) {
						case FINALLY_INLINE :
							// inlined finally here can see all merged variables
							if (isStackMapFrameCodeStream) {
								((StackMapFrameCodeStream) codeStream).pushStateIndex(this.naturalExitMergeInitStateIndex);
							}
							if (this.catchExitInitStateIndexes[i] != -1) {
								codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.catchExitInitStateIndexes[i]);
								codeStream.addDefinitelyAssignedVariables(currentScope, this.catchExitInitStateIndexes[i]);
							}
							// entire sequence for finally is associated to finally block
							this.finallyBlock.generateCode(this.scope, codeStream);
							codeStream.goto_(postCatchesFinallyLabel);
							if (isStackMapFrameCodeStream) {
								((StackMapFrameCodeStream) codeStream).popStateIndex();
							}
							break;
						case FINALLY_SUBROUTINE :
							requiresNaturalExit = true;
							//$FALL-THROUGH$
						case NO_FINALLY :
							if (this.naturalExitMergeInitStateIndex != -1) {
								codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.naturalExitMergeInitStateIndex);
								codeStream.addDefinitelyAssignedVariables(currentScope, this.naturalExitMergeInitStateIndex);
							}
							codeStream.goto_(naturalExitLabel);
							break;
						case FINALLY_DOES_NOT_COMPLETE :
							codeStream.goto_(this.subRoutineStartLabel);
							break;
					}
				}
			}
		}
		// extra handler for trailing natural exit (will be fixed up later on when natural exit is generated below)
		ExceptionLabel naturalExitExceptionHandler = requiresNaturalExit && (finallyMode == FINALLY_SUBROUTINE)
					? new ExceptionLabel(codeStream, null)
					: null;

		// addition of a special handler so as to ensure that any uncaught exception (or exception thrown
		// inside catch blocks) will run the finally block
		int finallySequenceStartPC = codeStream.position;
		if (this.subRoutineStartLabel != null && this.anyExceptionLabel.getCount() != 0) {
			codeStream.pushExceptionOnStack(this.scope.getJavaLangThrowable());
			if (this.preTryInitStateIndex != -1) {
				// reset initialization state, as for a normal catch block
				codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.preTryInitStateIndex);
				codeStream.addDefinitelyAssignedVariables(currentScope, this.preTryInitStateIndex);
			}
			placeAllAnyExceptionHandler();
			if (naturalExitExceptionHandler != null) naturalExitExceptionHandler.place();

			switch(finallyMode) {
				case FINALLY_SUBROUTINE :
					// any exception handler
					codeStream.store(this.anyExceptionVariable, false);
					codeStream.jsr(this.subRoutineStartLabel);
					codeStream.recordPositionsFrom(finallySequenceStartPC, this.finallyBlock.sourceStart);
					int position = codeStream.position;
					codeStream.throwAnyException(this.anyExceptionVariable);
					codeStream.recordPositionsFrom(position, this.finallyBlock.sourceEnd);
					// subroutine
					this.subRoutineStartLabel.place();
					codeStream.pushExceptionOnStack(this.scope.getJavaLangThrowable());
					position = codeStream.position;
					codeStream.store(this.returnAddressVariable, false);
					codeStream.recordPositionsFrom(position, this.finallyBlock.sourceStart);
					this.finallyBlock.generateCode(this.scope, codeStream);
					position = codeStream.position;
					codeStream.ret(this.returnAddressVariable.resolvedPosition);
					codeStream.recordPositionsFrom(
						position,
						this.finallyBlock.sourceEnd);
					// the ret bytecode is part of the subroutine
					break;
				case FINALLY_INLINE :
					// any exception handler
					codeStream.store(this.anyExceptionVariable, false);
					codeStream.addVariable(this.anyExceptionVariable);
					codeStream.recordPositionsFrom(finallySequenceStartPC, this.finallyBlock.sourceStart);
					// subroutine
					this.finallyBlock.generateCode(currentScope, codeStream);
					position = codeStream.position;
					codeStream.throwAnyException(this.anyExceptionVariable);
					codeStream.removeVariable(this.anyExceptionVariable);
					if (this.preTryInitStateIndex != -1) {
						codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.preTryInitStateIndex);
					}
					this.subRoutineStartLabel.place();
					codeStream.recordPositionsFrom(position, this.finallyBlock.sourceEnd);
					break;
				case FINALLY_DOES_NOT_COMPLETE :
					// any exception handler
					codeStream.pop();
					this.subRoutineStartLabel.place();
					codeStream.recordPositionsFrom(finallySequenceStartPC, this.finallyBlock.sourceStart);
					// subroutine
					this.finallyBlock.generateCode(this.scope, codeStream);
					break;
			}

			// will naturally fall into subsequent code after subroutine invocation
			if (requiresNaturalExit) {
				switch(finallyMode) {
					case FINALLY_SUBROUTINE :
						naturalExitLabel.place();
						int position = codeStream.position;
						naturalExitExceptionHandler.placeStart();
						codeStream.jsr(this.subRoutineStartLabel);
						naturalExitExceptionHandler.placeEnd();
						codeStream.recordPositionsFrom(
							position,
							this.finallyBlock.sourceEnd);
						break;
					case FINALLY_INLINE :
						// inlined finally here can see all merged variables
						if (isStackMapFrameCodeStream) {
							((StackMapFrameCodeStream) codeStream).pushStateIndex(this.naturalExitMergeInitStateIndex);
						}
						if (this.naturalExitMergeInitStateIndex != -1) {
							codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.naturalExitMergeInitStateIndex);
							codeStream.addDefinitelyAssignedVariables(currentScope, this.naturalExitMergeInitStateIndex);
						}
						naturalExitLabel.place();
						// entire sequence for finally is associated to finally block
						this.finallyBlock.generateCode(this.scope, codeStream);
						if (postCatchesFinallyLabel != null) {
							position = codeStream.position;
							// entire sequence for finally is associated to finally block
							codeStream.goto_(postCatchesFinallyLabel);
							codeStream.recordPositionsFrom(
									position,
									this.finallyBlock.sourceEnd);
						}
						if (isStackMapFrameCodeStream) {
							((StackMapFrameCodeStream) codeStream).popStateIndex();
						}
						break;
					case FINALLY_DOES_NOT_COMPLETE :
						break;
					default :
						naturalExitLabel.place();
						break;
				}
			}
			if (postCatchesFinallyLabel != null) {
				postCatchesFinallyLabel.place();
			}
		} else {
			// no subroutine, simply position end label (natural exit == end)
			naturalExitLabel.place();
		}
	} else {
		// try block had no effect, only generate the body of the finally block if any
		if (this.subRoutineStartLabel != null) {
			this.finallyBlock.generateCode(this.scope, codeStream);
		}
	}
	// May loose some local variable initializations : affecting the local variable attributes
	if (this.mergedInitStateIndex != -1) {
		codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.mergedInitStateIndex);
		codeStream.addDefinitelyAssignedVariables(currentScope, this.mergedInitStateIndex);
	}
	codeStream.recordPositionsFrom(pc, this.sourceStart);
}
private void generateCodeSnippet(Statement statement, CodeStream codeStream, BranchLabel postCloseLabel, boolean record, int... values) {

	int i = -1;
	int invokeCloseStartPc = -1;
	if (record) {
		i = values[0];
		invokeCloseStartPc = values[1];
	}
	if (statement instanceof LocalDeclaration)
		generateCodeSnippet((LocalDeclaration)statement, codeStream, postCloseLabel, record, i, invokeCloseStartPc);
	else if (statement instanceof Reference)
		generateCodeSnippet((Reference)statement, codeStream, postCloseLabel, record, i, invokeCloseStartPc);
	// else abort
}

private void generateCodeSnippet(Reference reference, CodeStream codeStream, BranchLabel postCloseLabel, boolean record, int i, int invokeCloseStartPc) {
	reference.generateCode(this.scope, codeStream, true);
	codeStream.ifnull(postCloseLabel);
	reference.generateCode(this.scope, codeStream, true);
	codeStream.invokeAutoCloseableClose(reference.resolvedType);
	if (!record) return;
	codeStream.recordPositionsFrom(invokeCloseStartPc, this.tryBlock.sourceEnd);
	isDuplicateResourceReference(i);
}
private void generateCodeSnippet(LocalDeclaration localDeclaration, CodeStream codeStream, BranchLabel postCloseLabel, boolean record, int i, int invokeCloseStartPc) {
	LocalVariableBinding variableBinding = localDeclaration.binding;
	codeStream.load(variableBinding);
	codeStream.ifnull(postCloseLabel);
	codeStream.load(variableBinding);
	codeStream.invokeAutoCloseableClose(variableBinding.type);
	if (!record) return;
	codeStream.recordPositionsFrom(invokeCloseStartPc, this.tryBlock.sourceEnd);
	if (!isDuplicateResourceReference(i)) // do not remove duplicate variable now
		codeStream.removeVariable(variableBinding);
}

private boolean isDuplicateResourceReference(int index) {
	int len = this.resources.length;
	if (index < len && this.resources[index] instanceof Reference) {
		Reference ref = (Reference) this.resources[index];
		Binding refBinding =  ref instanceof NameReference ? ((NameReference) ref).binding :
			ref instanceof FieldReference ? ((FieldReference) ref).binding : null;
		if (refBinding == null) return false;

		//TODO: For field accesses in the form of a.b.c and b.c - could there be a non-trivial dup - to check?
		for (int i = 0; i < index; i++) {
			Statement stmt = this.resources[i];
			Binding b = stmt instanceof LocalDeclaration ? ((LocalDeclaration) stmt).binding :
				stmt instanceof NameReference ? ((NameReference) stmt).binding :
						stmt instanceof FieldReference ? ((FieldReference) stmt).binding : null;
			if (b == refBinding) {
				this.scope.problemReporter().duplicateResourceReference(ref);
				return true;
			}
		}
	}
	return false;
}

/**
 * @see SubRoutineStatement#generateSubRoutineInvocation(BlockScope, CodeStream, Object, int, LocalVariableBinding)
 */
@Override
public boolean generateSubRoutineInvocation(BlockScope currentScope, CodeStream codeStream, Object targetLocation, int stateIndex, LocalVariableBinding secretLocal) {

	int resourceCount = this.resources.length;
	if (resourceCount > 0 && this.resourceExceptionLabels != null) { // https://bugs.eclipse.org/bugs/show_bug.cgi?id=375248
		for (int i = resourceCount; i > 0; --i) {
			// Disarm the handlers and take care of resource closure.
			this.resourceExceptionLabels[i].placeEnd();
			BranchLabel exitLabel = new BranchLabel(codeStream);
			int invokeCloseStartPc = codeStream.position; // https://bugs.eclipse.org/bugs/show_bug.cgi?id=343785
			generateCodeSnippet(this.resources[i - 1], codeStream, exitLabel, false);
			codeStream.recordPositionsFrom(invokeCloseStartPc, this.tryBlock.sourceEnd);
			exitLabel.place();
		}
		this.resourceExceptionLabels[0].placeEnd(); // outermost should end here as well, will start again on enter
	}

	boolean isStackMapFrameCodeStream = codeStream instanceof StackMapFrameCodeStream;
	int finallyMode = finallyMode();
	switch(finallyMode) {
		case FINALLY_DOES_NOT_COMPLETE :
			if (this.switchExpression != null) {
				this.finallyBlock.generateCode(currentScope, codeStream);
				return true;
			}
			codeStream.goto_(this.subRoutineStartLabel);
			return true;

		case NO_FINALLY :
			if (this.switchExpression == null) { // already taken care at Yield
				exitDeclaredExceptionHandlers(codeStream);
			}
			return false;
	}
	// optimize subroutine invocation sequences, using the targetLocation (if any)
	CompilerOptions options = this.scope.compilerOptions();
	if (options.shareCommonFinallyBlocks && targetLocation != null) {
		boolean reuseTargetLocation = true;
		if (this.reusableJSRTargetsCount > 0) {
			nextReusableTarget: for (int i = 0, count = this.reusableJSRTargetsCount; i < count; i++) {
				Object reusableJSRTarget = this.reusableJSRTargets[i];
				differentTarget: {
					if (targetLocation == reusableJSRTarget)
						break differentTarget;
					if (targetLocation instanceof Constant
							&& reusableJSRTarget instanceof Constant
							&& ((Constant)targetLocation).hasSameValue((Constant) reusableJSRTarget)) {
						break differentTarget;
					}
					// cannot reuse current target
					continue nextReusableTarget;
				}
				// current target has been used in the past, simply branch to its label
				if ((this.reusableJSRStateIndexes[i] != stateIndex) && finallyMode == FINALLY_INLINE) {
					reuseTargetLocation = false;
					break nextReusableTarget;
				} else {
					codeStream.goto_(this.reusableJSRSequenceStartLabels[i]);
					return true;
				}
			}
		} else {
			this.reusableJSRTargets = new Object[3];
			this.reusableJSRSequenceStartLabels = new BranchLabel[3];
			this.reusableJSRStateIndexes = new int[3];
		}
		if (reuseTargetLocation) {
			if (this.reusableJSRTargetsCount == this.reusableJSRTargets.length) {
				System.arraycopy(this.reusableJSRTargets, 0, this.reusableJSRTargets = new Object[2*this.reusableJSRTargetsCount], 0, this.reusableJSRTargetsCount);
				System.arraycopy(this.reusableJSRSequenceStartLabels, 0, this.reusableJSRSequenceStartLabels = new BranchLabel[2*this.reusableJSRTargetsCount], 0, this.reusableJSRTargetsCount);
				System.arraycopy(this.reusableJSRStateIndexes, 0, this.reusableJSRStateIndexes = new int[2*this.reusableJSRTargetsCount], 0, this.reusableJSRTargetsCount);
			}
			this.reusableJSRTargets[this.reusableJSRTargetsCount] = targetLocation;
			BranchLabel reusableJSRSequenceStartLabel = new BranchLabel(codeStream);
			reusableJSRSequenceStartLabel.place();
			this.reusableJSRStateIndexes[this.reusableJSRTargetsCount] = stateIndex;
			this.reusableJSRSequenceStartLabels[this.reusableJSRTargetsCount++] = reusableJSRSequenceStartLabel;
		}
	}
	if (finallyMode == FINALLY_INLINE) {
		if (isStackMapFrameCodeStream) {
			((StackMapFrameCodeStream) codeStream).pushStateIndex(stateIndex);
		}
		// cannot use jsr bytecode, then simply inline the subroutine
		// inside try block, ensure to deactivate all catch block exception handlers while inlining finally block
		exitAnyExceptionHandler();
		exitDeclaredExceptionHandlers(codeStream);
		this.finallyBlock.generateCode(currentScope, codeStream);
		if (isStackMapFrameCodeStream) {
			((StackMapFrameCodeStream) codeStream).popStateIndex();
		}
	} else {
		// classic subroutine invocation, distinguish case of non-returning subroutine
		codeStream.jsr(this.subRoutineStartLabel);
		exitAnyExceptionHandler();
		exitDeclaredExceptionHandlers(codeStream);
	}
	return false;
}
@Override
public boolean isSubRoutineEscaping() {
	return (this.bits & ASTNode.IsSubRoutineEscaping) != 0;
}

@Override
public StringBuffer printStatement(int indent, StringBuffer output) {
	int length = this.resources.length;
	printIndent(indent, output).append("try" + (length == 0 ? "\n" : " (")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	for (int i = 0; i < length; i++) {
		Statement stmt = this.resources[i];
		if (stmt instanceof LocalDeclaration) {
			((LocalDeclaration) stmt).printAsExpression(0, output);
		} else if (stmt instanceof Reference) {
			((Reference) stmt).printExpression(0, output);
		} else continue;
		if (i != length - 1) {
			output.append(";\n"); //$NON-NLS-1$
			printIndent(indent + 2, output);
		}
	}
	if (length > 0) {
		output.append(")\n"); //$NON-NLS-1$
	}
	this.tryBlock.printStatement(indent + 1, output);

	//catches
	if (this.catchBlocks != null)
		for (int i = 0; i < this.catchBlocks.length; i++) {
				output.append('\n');
				printIndent(indent, output).append("catch ("); //$NON-NLS-1$
				this.catchArguments[i].print(0, output).append(")\n"); //$NON-NLS-1$
				this.catchBlocks[i].printStatement(indent + 1, output);
		}
	//finally
	if (this.finallyBlock != null) {
		output.append('\n');
		printIndent(indent, output).append("finally\n"); //$NON-NLS-1$
		this.finallyBlock.printStatement(indent + 1, output);
	}
	return output;
}

@Override
public void resolve(BlockScope upperScope) {
	// special scope for secret locals optimization.
	this.scope = new BlockScope(upperScope);

	BlockScope finallyScope = null;
    BlockScope resourceManagementScope = null; // Single scope to hold all resources and additional secret variables.
	int resourceCount = this.resources.length;
	if (resourceCount > 0) {
		resourceManagementScope = new BlockScope(this.scope);
		this.primaryExceptionVariable =
			new LocalVariableBinding(TryStatement.SECRET_PRIMARY_EXCEPTION_VARIABLE_NAME, this.scope.getJavaLangThrowable(), ClassFileConstants.AccDefault, false);
		resourceManagementScope.addLocalVariable(this.primaryExceptionVariable);
		this.primaryExceptionVariable.setConstant(Constant.NotAConstant); // not inlinable
		this.caughtThrowableVariable =
			new LocalVariableBinding(TryStatement.SECRET_CAUGHT_THROWABLE_VARIABLE_NAME, this.scope.getJavaLangThrowable(), ClassFileConstants.AccDefault, false);
		resourceManagementScope.addLocalVariable(this.caughtThrowableVariable);
		this.caughtThrowableVariable.setConstant(Constant.NotAConstant); // not inlinable
	}
	for (int i = 0; i < resourceCount; i++) {
		this.resources[i].resolve(resourceManagementScope);
		if (this.resources[i] instanceof LocalDeclaration) {
			LocalDeclaration node = (LocalDeclaration)this.resources[i];
			LocalVariableBinding localVariableBinding = node.binding;
			if (localVariableBinding != null && localVariableBinding.isValidBinding()) {
				localVariableBinding.modifiers |= ClassFileConstants.AccFinal;
				localVariableBinding.tagBits |= TagBits.IsResource;
				TypeBinding resourceType = localVariableBinding.type;
				if (resourceType instanceof ReferenceBinding) {
					if (resourceType.findSuperTypeOriginatingFrom(TypeIds.T_JavaLangAutoCloseable, false /*AutoCloseable is not a class*/) == null && resourceType.isValidBinding()) {
						upperScope.problemReporter().resourceHasToImplementAutoCloseable(resourceType, node.type);
						localVariableBinding.type = new ProblemReferenceBinding(CharOperation.splitOn('.', resourceType.shortReadableName()), null, ProblemReasons.InvalidTypeForAutoManagedResource);
					}
				} else if (resourceType != null) { // https://bugs.eclipse.org/bugs/show_bug.cgi?id=349862, avoid secondary error in problematic null case
					upperScope.problemReporter().resourceHasToImplementAutoCloseable(resourceType, node.type);
					localVariableBinding.type = new ProblemReferenceBinding(CharOperation.splitOn('.', resourceType.shortReadableName()), null, ProblemReasons.InvalidTypeForAutoManagedResource);
				}
			}
		} else { // expression
			Expression node = (Expression) this.resources[i];
			TypeBinding resourceType = node.resolvedType;
			if (resourceType instanceof ReferenceBinding) {
				if (resourceType.findSuperTypeOriginatingFrom(TypeIds.T_JavaLangAutoCloseable, false /*AutoCloseable is not a class*/) == null && resourceType.isValidBinding()) {
					upperScope.problemReporter().resourceHasToImplementAutoCloseable(resourceType, node);
					((Expression) this.resources[i]).resolvedType = new ProblemReferenceBinding(CharOperation.splitOn('.', resourceType.shortReadableName()), null, ProblemReasons.InvalidTypeForAutoManagedResource);
				}
			} else if (resourceType != null) { // https://bugs.eclipse.org/bugs/show_bug.cgi?id=349862, avoid secondary error in problematic null case
				upperScope.problemReporter().resourceHasToImplementAutoCloseable(resourceType, node);
				((Expression) this.resources[i]).resolvedType = new ProblemReferenceBinding(CharOperation.splitOn('.', resourceType.shortReadableName()), null, ProblemReasons.InvalidTypeForAutoManagedResource);
			}
		}
	}
	BlockScope tryScope = new BlockScope(resourceManagementScope != null ? resourceManagementScope : this.scope);

	if (this.finallyBlock != null) {
		if (this.finallyBlock.isEmptyBlock()) {
			if ((this.finallyBlock.bits & ASTNode.UndocumentedEmptyBlock) != 0) {
				this.scope.problemReporter().undocumentedEmptyBlock(this.finallyBlock.sourceStart, this.finallyBlock.sourceEnd);
			}
		} else {
			finallyScope = new BlockScope(this.scope, false); // don't add it yet to parent scope

			// provision for returning and forcing the finally block to run
			MethodScope methodScope = this.scope.methodScope();

			// the type does not matter as long as it is not a base type
			if (!upperScope.compilerOptions().inlineJsrBytecode) {
				this.returnAddressVariable =
					new LocalVariableBinding(TryStatement.SECRET_RETURN_ADDRESS_NAME, upperScope.getJavaLangObject(), ClassFileConstants.AccDefault, false);
				finallyScope.addLocalVariable(this.returnAddressVariable);
				this.returnAddressVariable.setConstant(Constant.NotAConstant); // not inlinable
			}
			this.subRoutineStartLabel = new BranchLabel();

			this.anyExceptionVariable =
				new LocalVariableBinding(TryStatement.SECRET_ANY_HANDLER_NAME, this.scope.getJavaLangThrowable(), ClassFileConstants.AccDefault, false);
			finallyScope.addLocalVariable(this.anyExceptionVariable);
			this.anyExceptionVariable.setConstant(Constant.NotAConstant); // not inlinable

			if (!methodScope.isInsideInitializer()) {
				MethodBinding methodBinding = methodScope.referenceContext instanceof AbstractMethodDeclaration ?
					((AbstractMethodDeclaration) methodScope.referenceContext).binding : (methodScope.referenceContext instanceof LambdaExpression ?
							((LambdaExpression)methodScope.referenceContext).binding : null);
				if (methodBinding != null) {
					TypeBinding methodReturnType = methodBinding.returnType;
					if (methodReturnType.id != TypeIds.T_void) {
						this.secretReturnValue =
							new LocalVariableBinding(
								TryStatement.SECRET_RETURN_VALUE_NAME,
								methodReturnType,
								ClassFileConstants.AccDefault,
								false);
						finallyScope.addLocalVariable(this.secretReturnValue);
						this.secretReturnValue.setConstant(Constant.NotAConstant); // not inlinable
					}
				}
			}
			this.finallyBlock.resolveUsing(finallyScope);
			// force the finally scope to have variable positions shifted after its try scope and catch ones
			int shiftScopesLength = this.catchArguments == null ? 1 : this.catchArguments.length + 1;
			finallyScope.shiftScopes = new BlockScope[shiftScopesLength];
			finallyScope.shiftScopes[0] = tryScope;
		}
	}
	this.tryBlock.resolveUsing(tryScope);

	// arguments type are checked against JavaLangThrowable in resolveForCatch(..)
	if (this.catchBlocks != null) {
		int length = this.catchArguments.length;
		TypeBinding[] argumentTypes = new TypeBinding[length];
		boolean containsUnionTypes = false;
		boolean catchHasError = false;
		for (int i = 0; i < length; i++) {
			BlockScope catchScope = new BlockScope(this.scope);
			if (finallyScope != null){
				finallyScope.shiftScopes[i+1] = catchScope;
			}
			// side effect on catchScope in resolveForCatch(..)
			Argument catchArgument = this.catchArguments[i];
			containsUnionTypes |= (catchArgument.type.bits & ASTNode.IsUnionType) != 0;
			if ((argumentTypes[i] = catchArgument.resolveForCatch(catchScope)) == null) {
				catchHasError = true;
			}
			this.catchBlocks[i].resolveUsing(catchScope);
		}
		if (catchHasError) {
			return;
		}
		// Verify that the catch clause are ordered in the right way:
		// more specialized first.
		verifyDuplicationAndOrder(length, argumentTypes, containsUnionTypes);
	} else {
		this.caughtExceptionTypes = new ReferenceBinding[0];
	}

	if (finallyScope != null){
		// add finallyScope as last subscope, so it can be shifted behind try/catch subscopes.
		// the shifting is necessary to achieve no overlay in between the finally scope and its
		// sibling in term of local variable positions.
		this.scope.addSubscope(finallyScope);
	}
}
@Override
public void traverse(ASTVisitor visitor, BlockScope blockScope) {
	if (visitor.visit(this, blockScope)) {
		Statement[] statements = this.resources;
		for (int i = 0, max = statements.length; i < max; i++) {
			statements[i].traverse(visitor, this.scope);
		}
		this.tryBlock.traverse(visitor, this.scope);
		if (this.catchArguments != null) {
			for (int i = 0, max = this.catchBlocks.length; i < max; i++) {
				this.catchArguments[i].traverse(visitor, this.scope);
				this.catchBlocks[i].traverse(visitor, this.scope);
			}
		}
		if (this.finallyBlock != null)
			this.finallyBlock.traverse(visitor, this.scope);
	}
	visitor.endVisit(this, blockScope);
}
protected void verifyDuplicationAndOrder(int length, TypeBinding[] argumentTypes, boolean containsUnionTypes) {
	// Verify that the catch clause are ordered in the right way:
	// more specialized first.
	if (containsUnionTypes) {
		int totalCount = 0;
		ReferenceBinding[][] allExceptionTypes = new ReferenceBinding[length][];
		for (int i = 0; i < length; i++) {
			if (argumentTypes[i] instanceof ArrayBinding)
				continue;
			ReferenceBinding currentExceptionType = (ReferenceBinding) argumentTypes[i];
			TypeReference catchArgumentType = this.catchArguments[i].type;
			if ((catchArgumentType.bits & ASTNode.IsUnionType) != 0) {
				TypeReference[] typeReferences = ((UnionTypeReference) catchArgumentType).typeReferences;
				int typeReferencesLength = typeReferences.length;
				ReferenceBinding[] unionExceptionTypes = new ReferenceBinding[typeReferencesLength];
				for (int j = 0; j < typeReferencesLength; j++) {
					unionExceptionTypes[j] = (ReferenceBinding) typeReferences[j].resolvedType;
				}
				totalCount += typeReferencesLength;
				allExceptionTypes[i] = unionExceptionTypes;
			} else {
				allExceptionTypes[i] = new ReferenceBinding[] { currentExceptionType };
				totalCount++;
			}
		}
		this.caughtExceptionTypes = new ReferenceBinding[totalCount];
		this.caughtExceptionsCatchBlocks  = new int[totalCount];
		for (int i = 0, l = 0; i < length; i++) {
			ReferenceBinding[] currentExceptions = allExceptionTypes[i];
			if (currentExceptions == null) continue;
			loop: for (int j = 0, max = currentExceptions.length; j < max; j++) {
				ReferenceBinding exception = currentExceptions[j];
				this.caughtExceptionTypes[l] = exception;
				this.caughtExceptionsCatchBlocks[l++] = i;
				// now iterate over all previous exceptions
				for (int k = 0; k < i; k++) {
					ReferenceBinding[] exceptions = allExceptionTypes[k];
					if (exceptions == null) continue;
					for (int n = 0, max2 = exceptions.length; n < max2; n++) {
						ReferenceBinding currentException = exceptions[n];
						if (exception.isCompatibleWith(currentException)) {
							TypeReference catchArgumentType = this.catchArguments[i].type;
							if ((catchArgumentType.bits & ASTNode.IsUnionType) != 0) {
								catchArgumentType = ((UnionTypeReference) catchArgumentType).typeReferences[j];
							}
							this.scope.problemReporter().wrongSequenceOfExceptionTypesError(
								catchArgumentType,
								exception,
								currentException);
							break loop;
						}
					}
				}
			}
		}
	} else {
		this.caughtExceptionTypes = new ReferenceBinding[length];
		for (int i = 0; i < length; i++) {
			if (argumentTypes[i] instanceof ArrayBinding)
				continue;
			this.caughtExceptionTypes[i] = (ReferenceBinding) argumentTypes[i];
			for (int j = 0; j < i; j++) {
				if (this.caughtExceptionTypes[i].isCompatibleWith(argumentTypes[j])) {
					this.scope.problemReporter().wrongSequenceOfExceptionTypesError(
						this.catchArguments[i].type,
						this.caughtExceptionTypes[i],
						argumentTypes[j]);
				}
			}
		}
	}
}
@Override
public boolean doesNotCompleteNormally() {
	if (!this.tryBlock.doesNotCompleteNormally()) {
		return (this.finallyBlock != null) ? this.finallyBlock.doesNotCompleteNormally() : false;
	}
	if (this.catchBlocks != null) {
		for (int i = 0; i < this.catchBlocks.length; i++) {
			if (!this.catchBlocks[i].doesNotCompleteNormally()) {
				return (this.finallyBlock != null) ? this.finallyBlock.doesNotCompleteNormally() : false;
			}
		}
	}
	return true;
}
@Override
public boolean completesByContinue() {
	if (this.tryBlock.completesByContinue()) {
		return (this.finallyBlock == null) ? true :
			!this.finallyBlock.doesNotCompleteNormally() || this.finallyBlock.completesByContinue();
	}
	if (this.catchBlocks != null) {
		for (int i = 0; i < this.catchBlocks.length; i++) {
			if (this.catchBlocks[i].completesByContinue()) {
				return (this.finallyBlock == null) ? true :
					!this.finallyBlock.doesNotCompleteNormally() || this.finallyBlock.completesByContinue();
			}
		}
	}
	return this.finallyBlock != null && this.finallyBlock.completesByContinue();
}
@Override
public boolean canCompleteNormally() {
	if (this.tryBlock.canCompleteNormally()) {
		return (this.finallyBlock != null) ? this.finallyBlock.canCompleteNormally() : true;
	}
	if (this.catchBlocks != null) {
		for (int i = 0; i < this.catchBlocks.length; i++) {
			if (this.catchBlocks[i].canCompleteNormally()) {
				return (this.finallyBlock != null) ? this.finallyBlock.canCompleteNormally() : true;
			}
		}
	}
	return false;
}
@Override
public boolean continueCompletes() {
	if (this.tryBlock.continueCompletes()) {
		return (this.finallyBlock == null) ? true :
			this.finallyBlock.canCompleteNormally() || this.finallyBlock.continueCompletes();
	}
	if (this.catchBlocks != null) {
		for (int i = 0; i < this.catchBlocks.length; i++) {
			if (this.catchBlocks[i].continueCompletes()) {
				return (this.finallyBlock == null) ? true :
					this.finallyBlock.canCompleteNormally() || this.finallyBlock.continueCompletes();
			}
		}
	}
	return this.finallyBlock != null && this.finallyBlock.continueCompletes();
}
}
