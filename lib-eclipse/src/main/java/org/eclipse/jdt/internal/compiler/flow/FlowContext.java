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
 *     							bug 358827 - [1.7] exception analysis for t-w-r spoils null analysis
 *								bug 186342 - [compiler][null] Using annotations for null checking
 *								bug 368546 - [compiler][resource] Avoid remaining false positives found when compiling the Eclipse SDK
 *								bug 365859 - [compiler][null] distinguish warnings based on flow analysis vs. null annotations
 *								bug 345305 - [compiler][null] Compiler misidentifies a case of "variable can only be null"
 *								bug 383368 - [compiler][null] syntactic null analysis for field references
 *								bug 402993 - [null] Follow up of bug 401088: Missing warning about redundant null check
 *								bug 403086 - [compiler][null] include the effect of 'assert' in syntactic null analysis for fields
 *								bug 403147 - [compiler][null] FUP of bug 400761: consolidate interaction between unboxing, NPE, and deferred checking
 *								Bug 453483 - [compiler][null][loop] Improve null analysis for loops
 *								Bug 455723 - Nonnull argument not correctly inferred in loop
 *								Bug 415790 - [compiler][resource]Incorrect potential resource leak warning in for loop with close in try/catch
 *								Bug 446691 - [1.8][null][compiler] NullPointerException in SingleNameReference.analyseCode
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.flow;

import java.util.ArrayList;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FakedTrackingVariable;
import org.eclipse.jdt.internal.compiler.ast.LabeledStatement;
import org.eclipse.jdt.internal.compiler.ast.LambdaExpression;
import org.eclipse.jdt.internal.compiler.ast.NullAnnotationMatching;
import org.eclipse.jdt.internal.compiler.ast.Reference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SubRoutineStatement;
import org.eclipse.jdt.internal.compiler.ast.ThrowStatement;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.BranchLabel;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.CatchParameterBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.lookup.VariableBinding;

/**
 * Reflects the context of code analysis, keeping track of enclosing
 *	try statements, exception handlers, etc...
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class FlowContext implements TypeConstants {

	// preempt marks looping contexts
	public final static FlowContext NotContinuableContext = new FlowContext(null, null, true);
	public ASTNode associatedNode;
	public FlowContext parent;
	public FlowInfo initsOnFinally;
		// only used within try blocks; remembers upstream flow info mergedWith
		// any null related operation happening within the try block
	/**
	 * Used to record whether effects in a try block affect the finally-block
	 * conditionally or unconditionally.
	 * -1 means: no effect,
	 * 0 means: unconditional effect,
	 * > 0 means levels of nested conditional structures.
	 */
	public int conditionalLevel = -1;

	public int tagBits;

	// array to store the provided and expected types from the potential error location (for display in error messages):
	public TypeBinding[][] providedExpectedTypes = null;

	// record field references known to be non-null
	//   this array will never shrink, only grow. reset happens by nulling expired entries
	//   this array grows in lock step with timesToLiveForNullCheckInfo, which controls expiration
	private Reference[] nullCheckedFieldReferences = null;
	private int[] timesToLiveForNullCheckInfo = null;

	public static final int DEFER_NULL_DIAGNOSTIC = 0x1;
	public static final int PREEMPT_NULL_DIAGNOSTIC = 0x2;
	// inside an assertFalse or a not-expression checks for equality / inequality have reversed meaning for syntactic analysis for fields:
	public static final int INSIDE_NEGATION = 0x4;
	/**
	 * used to hide null comparison related warnings inside assert statements
	 */
	public static final int HIDE_NULL_COMPARISON_WARNING = 0x1000;
	public static final int HIDE_NULL_COMPARISON_WARNING_MASK = 0xF000;

public static final int CAN_ONLY_NULL_NON_NULL = 0x0000;
//check against null and non null, with definite values -- comparisons
public static final int CAN_ONLY_NULL = 0x0001;
//check against null, with definite values -- comparisons
public static final int CAN_ONLY_NON_NULL = 0x0002;
//check against non null, with definite values -- comparisons
public static final int MAY_NULL = 0x0003;
//check binding a value to a @NonNull variable
public final static int ASSIGN_TO_NONNULL = 0x0080;
//check against an unboxing conversion
public static final int IN_UNBOXING = 0x0010;
//check against unclosed resource at early exit:
public static final int EXIT_RESOURCE = 0x0800;
// check against null, with potential values -- NPE guard
public static final int CHECK_MASK = 0x00FF;
public static final int IN_COMPARISON_NULL = 0x0100;
public static final int IN_COMPARISON_NON_NULL = 0x0200;
// check happened in a comparison
public static final int IN_ASSIGNMENT = 0x0300;
// check happened in an assignment
public static final int IN_INSTANCEOF = 0x0400;
// check happened in an instanceof expression
public static final int CONTEXT_MASK = ~CHECK_MASK & ~HIDE_NULL_COMPARISON_WARNING_MASK;

public FlowContext(FlowContext parent, ASTNode associatedNode, boolean inheritNullFieldChecks) {
	this.parent = parent;
	this.associatedNode = associatedNode;
	if (parent != null) {
		if ((parent.tagBits & (FlowContext.DEFER_NULL_DIAGNOSTIC | FlowContext.PREEMPT_NULL_DIAGNOSTIC)) != 0) {
			this.tagBits |= FlowContext.DEFER_NULL_DIAGNOSTIC;
		}
		this.initsOnFinally = parent.initsOnFinally;
		this.conditionalLevel = parent.conditionalLevel;
		if (inheritNullFieldChecks)
			copyNullCheckedFieldsFrom(parent); // re-use list if there is one
	}
}

public void copyNullCheckedFieldsFrom(FlowContext other) {
	Reference[] fieldReferences = other.nullCheckedFieldReferences;
	if (fieldReferences != null && fieldReferences.length > 0 && fieldReferences[0] != null) {
		this.nullCheckedFieldReferences = other.nullCheckedFieldReferences;
		this.timesToLiveForNullCheckInfo = other.timesToLiveForNullCheckInfo;
	}
}

/**
 * Record that a reference to a field has been seen in a non-null state.
 *
 * @param reference Can be a SingleNameReference, a FieldReference or a QualifiedNameReference resolving to a field
 * @param timeToLive control how many expire events are needed to expire this information
 */
public void recordNullCheckedFieldReference(Reference reference, int timeToLive) {
	if (this.nullCheckedFieldReferences == null) {
		// first entry:
		this.nullCheckedFieldReferences = new Reference[] { reference, null };
		this.timesToLiveForNullCheckInfo = new int[] { timeToLive, -1 };
	} else {
		int len = this.nullCheckedFieldReferences.length;
		// insert into first empty slot:
		for (int i=0; i<len; i++) {
			if (this.nullCheckedFieldReferences[i] == null) {
				this.nullCheckedFieldReferences[i] = reference;
				this.timesToLiveForNullCheckInfo[i] = timeToLive;
				return;
			}
		}
		// grow arrays:
		System.arraycopy(this.nullCheckedFieldReferences, 0, this.nullCheckedFieldReferences=new Reference[len+2], 0, len);
		System.arraycopy(this.timesToLiveForNullCheckInfo, 0, this.timesToLiveForNullCheckInfo=new int[len+2], 0, len);
		this.nullCheckedFieldReferences[len] = reference;
		this.timesToLiveForNullCheckInfo[len] = timeToLive;
	}
}

/** If a null checked field has been recorded recently, increase its time to live. */
public void extendTimeToLiveForNullCheckedField(int t) {
	if (this.timesToLiveForNullCheckInfo != null) {
		for (int i = 0; i < this.timesToLiveForNullCheckInfo.length; i++)
			if (this.timesToLiveForNullCheckInfo[i] > 0)
				this.timesToLiveForNullCheckInfo[i] += t;
	}
}

/**
 * Forget any information about fields that were previously known to be non-null.
 *
 * Will only cause any effect if CompilerOptions.enableSyntacticNullAnalysisForFields
 * (implicitly by guards before calls to {@link #recordNullCheckedFieldReference(Reference, int)}).
 */
public void expireNullCheckedFieldInfo() {
	if (this.nullCheckedFieldReferences != null) {
		for (int i = 0; i < this.nullCheckedFieldReferences.length; i++) {
			if (--this.timesToLiveForNullCheckInfo[i] == 0)
				this.nullCheckedFieldReferences[i] = null;
		}
	}
}

/**
 * Is the given field reference equivalent to a reference that is freshly known to be non-null?
 * Can only return true if CompilerOptions.enableSyntacticNullAnalysisForFields
 * (implicitly by guards before calls to {@link #recordNullCheckedFieldReference(Reference, int)}).
 */
public boolean isNullcheckedFieldAccess(Reference reference) {
	if (this.nullCheckedFieldReferences == null)  // always null unless CompilerOptions.enableSyntacticNullAnalysisForFields
		return false;
	int len = this.nullCheckedFieldReferences.length;
	for (int i=0; i<len; i++) {
		Reference checked = this.nullCheckedFieldReferences[i];
		if (checked == null) {
			continue;
		}
		if (checked.isEquivalent(reference)) {
			return true;
		}
	}
	return false;
}

public BranchLabel breakLabel() {
	return null;
}

public void checkExceptionHandlers(TypeBinding raisedException, ASTNode location, FlowInfo flowInfo, BlockScope scope) {
	checkExceptionHandlers(raisedException, location, flowInfo, scope, false);
}
/**
 * @param isExceptionOnAutoClose This is for checking exception handlers for exceptions raised during the
 * auto close of resources inside a try with resources statement. (Relevant for
 * source levels 1.7 and above only)
 */
public void checkExceptionHandlers(TypeBinding raisedException, ASTNode location, FlowInfo flowInfo, BlockScope scope, boolean isExceptionOnAutoClose) {
	// LIGHT-VERSION OF THE EQUIVALENT WITH AN ARRAY OF EXCEPTIONS
	// check that all the argument exception types are handled
	// JDK Compatible implementation - when an exception type is thrown,
	// all related catch blocks are marked as reachable... instead of those only
	// until the point where it is safely handled (Smarter - see comment at the end)
	FlowContext traversedContext = this;
	ArrayList abruptlyExitedLoops = null;
	if (scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_7 && location instanceof ThrowStatement) {
		Expression throwExpression = ((ThrowStatement)location).exception;
		LocalVariableBinding throwArgBinding = throwExpression.localVariableBinding();
		if (throwExpression instanceof SingleNameReference // https://bugs.eclipse.org/bugs/show_bug.cgi?id=350361
				&& throwArgBinding instanceof CatchParameterBinding && throwArgBinding.isEffectivelyFinal()) {
			CatchParameterBinding parameter = (CatchParameterBinding) throwArgBinding;
			checkExceptionHandlers(parameter.getPreciseTypes(), location, flowInfo, scope);
			return;
		}
	}
	while (traversedContext != null) {
		SubRoutineStatement sub;
		if (((sub = traversedContext.subroutine()) != null) && sub.isSubRoutineEscaping()) {
			// traversing a non-returning subroutine means that all unhandled
			// exceptions will actually never get sent...
			return;
		}

		// filter exceptions that are locally caught from the innermost enclosing
		// try statement to the outermost ones.
		if (traversedContext instanceof ExceptionHandlingFlowContext) {
			ExceptionHandlingFlowContext exceptionContext =
				(ExceptionHandlingFlowContext) traversedContext;
			ReferenceBinding[] caughtExceptions;
			if ((caughtExceptions = exceptionContext.handledExceptions) != Binding.NO_EXCEPTIONS) {
				boolean definitelyCaught = false;
				for (int caughtIndex = 0, caughtCount = caughtExceptions.length;
					caughtIndex < caughtCount;
					caughtIndex++) {
					ReferenceBinding caughtException = caughtExceptions[caughtIndex];
					FlowInfo exceptionFlow = flowInfo;
				    int state = caughtException == null
				    	? Scope.EQUAL_OR_MORE_SPECIFIC /* any exception */
				        : Scope.compareTypes(raisedException, caughtException);
				    if (abruptlyExitedLoops != null && caughtException != null && state != Scope.NOT_RELATED) {
				    	for (int i = 0, abruptlyExitedLoopsCount = abruptlyExitedLoops.size(); i < abruptlyExitedLoopsCount; i++) {
							LoopingFlowContext loop = (LoopingFlowContext) abruptlyExitedLoops.get(i);
							loop.recordCatchContextOfEscapingException(exceptionContext, caughtException, flowInfo);
						}
				    	exceptionFlow = FlowInfo.DEAD_END; // don't use flow info on first round, flow info will be evaluated during loopback simulation
					}
					switch (state) {
						case Scope.EQUAL_OR_MORE_SPECIFIC :
							exceptionContext.recordHandlingException(
								caughtException,
								exceptionFlow.unconditionalInits(),
								raisedException,
								raisedException, // precise exception that will be caught
								location,
								definitelyCaught);
							// was it already definitely caught ?
							definitelyCaught = true;
							break;
						case Scope.MORE_GENERIC :
							exceptionContext.recordHandlingException(
								caughtException,
								exceptionFlow.unconditionalInits(),
								raisedException,
								caughtException,
								location,
								false);
							// was not caught already per construction
					}
				}
				if (definitelyCaught)
					return;
			}
			// method treatment for unchecked exceptions
			if (exceptionContext.isMethodContext) {
				if (raisedException.isUncheckedException(false))
					return;
				boolean shouldMergeUnhandledExceptions = exceptionContext instanceof ExceptionInferenceFlowContext;

				// anonymous constructors are allowed to throw any exceptions (their thrown exceptions
				// clause will be fixed up later as per JLS 8.6).
				if (exceptionContext.associatedNode instanceof AbstractMethodDeclaration) {
					AbstractMethodDeclaration method = (AbstractMethodDeclaration)exceptionContext.associatedNode;
					if (method.isConstructor() && method.binding.declaringClass.isAnonymousType())
						shouldMergeUnhandledExceptions = true;
				}
				if (shouldMergeUnhandledExceptions) {
					exceptionContext.mergeUnhandledException(raisedException);
					return; // no need to complain, will fix up constructor/lambda exceptions
				}
				break; // not handled anywhere, thus jump to error handling
			}
		} else if (traversedContext instanceof LoopingFlowContext) {
			if (abruptlyExitedLoops == null) {
				abruptlyExitedLoops = new ArrayList(5);
			}
			abruptlyExitedLoops.add(traversedContext);
		}

		traversedContext.recordReturnFrom(flowInfo.unconditionalInits());

		if (!isExceptionOnAutoClose) {
			if (traversedContext instanceof InsideSubRoutineFlowContext) {
				ASTNode node = traversedContext.associatedNode;
				if (node instanceof TryStatement) {
					TryStatement tryStatement = (TryStatement) node;
					flowInfo.addInitializationsFrom(tryStatement.subRoutineInits); // collect inits
				}
			}
		}
		traversedContext = traversedContext.getLocalParent();
	}
	// if reaches this point, then there are some remaining unhandled exception types.
	if (isExceptionOnAutoClose) {
		scope.problemReporter().unhandledExceptionFromAutoClose(raisedException, location);
	} else {
		scope.problemReporter().unhandledException(raisedException, location);
	}
}

public void checkExceptionHandlers(TypeBinding[] raisedExceptions, ASTNode location, FlowInfo flowInfo, BlockScope scope) {
	// check that all the argument exception types are handled
	// JDK Compatible implementation - when an exception type is thrown,
	// all related catch blocks are marked as reachable... instead of those only
	// until the point where it is safely handled (Smarter - see comment at the end)
	int remainingCount; // counting the number of remaining unhandled exceptions
	int raisedCount; // total number of exceptions raised
	if ((raisedExceptions == null)
		|| ((raisedCount = raisedExceptions.length) == 0))
		return;
	remainingCount = raisedCount;

	// duplicate the array of raised exceptions since it will be updated
	// (null replaces any handled exception)
	System.arraycopy(
		raisedExceptions,
		0,
		(raisedExceptions = new TypeBinding[raisedCount]),
		0,
		raisedCount);
	FlowContext traversedContext = this;

	ArrayList abruptlyExitedLoops = null;
	while (traversedContext != null) {
		SubRoutineStatement sub;
		if (((sub = traversedContext.subroutine()) != null) && sub.isSubRoutineEscaping()) {
			// traversing a non-returning subroutine means that all unhandled
			// exceptions will actually never get sent...
			return;
		}
		// filter exceptions that are locally caught from the innermost enclosing
		// try statement to the outermost ones.
		if (traversedContext instanceof ExceptionHandlingFlowContext) {
			ExceptionHandlingFlowContext exceptionContext =
				(ExceptionHandlingFlowContext) traversedContext;
			ReferenceBinding[] caughtExceptions;
			if ((caughtExceptions = exceptionContext.handledExceptions) != Binding.NO_EXCEPTIONS) {
				int caughtCount = caughtExceptions.length;
				boolean[] locallyCaught = new boolean[raisedCount]; // at most

				for (int caughtIndex = 0; caughtIndex < caughtCount; caughtIndex++) {
					ReferenceBinding caughtException = caughtExceptions[caughtIndex];
					for (int raisedIndex = 0; raisedIndex < raisedCount; raisedIndex++) {
						TypeBinding raisedException;
						if ((raisedException = raisedExceptions[raisedIndex]) != null) {
							FlowInfo exceptionFlow = flowInfo;
						    int state = caughtException == null
						    	? Scope.EQUAL_OR_MORE_SPECIFIC /* any exception */
						        : Scope.compareTypes(raisedException, caughtException);
						    if (abruptlyExitedLoops != null && caughtException != null && state != Scope.NOT_RELATED) {
						    	for (int i = 0, abruptlyExitedLoopsCount = abruptlyExitedLoops.size(); i < abruptlyExitedLoopsCount; i++) {
									LoopingFlowContext loop = (LoopingFlowContext) abruptlyExitedLoops.get(i);
									loop.recordCatchContextOfEscapingException(exceptionContext, caughtException, flowInfo);
								}
						    	exceptionFlow = FlowInfo.DEAD_END; // don't use flow info on first round, flow info will be evaluated during loopback simulation
							}
							switch (state) {
								case Scope.EQUAL_OR_MORE_SPECIFIC :
									exceptionContext.recordHandlingException(
										caughtException,
										exceptionFlow.unconditionalInits(),
										raisedException,
										raisedException, // precise exception that will be caught
										location,
										locallyCaught[raisedIndex]);
									// was already definitely caught ?
									if (!locallyCaught[raisedIndex]) {
										locallyCaught[raisedIndex] = true;
										// remember that this exception has been definitely caught
										remainingCount--;
									}
									break;
								case Scope.MORE_GENERIC :
									exceptionContext.recordHandlingException(
										caughtException,
										exceptionFlow.unconditionalInits(),
										raisedException,
										caughtException,
										location,
										false);
									// was not caught already per construction
							}
						}
					}
				}
				// remove locally caught exceptions from the remaining ones
				for (int i = 0; i < raisedCount; i++) {
					if (locallyCaught[i]) {
						raisedExceptions[i] = null; // removed from the remaining ones.
					}
				}
			}
			// method treatment for unchecked exceptions
			if (exceptionContext.isMethodContext) {
				for (int i = 0; i < raisedCount; i++) {
					TypeBinding raisedException;
					if ((raisedException = raisedExceptions[i]) != null) {
						if (raisedException.isUncheckedException(false)) {
							remainingCount--;
							raisedExceptions[i] = null;
						}
					}
				}
				boolean shouldMergeUnhandledException = exceptionContext instanceof ExceptionInferenceFlowContext;
				// anonymous constructors are allowed to throw any exceptions (their thrown exceptions
				// clause will be fixed up later as per JLS 8.6).
				if (exceptionContext.associatedNode instanceof AbstractMethodDeclaration) {
					AbstractMethodDeclaration method = (AbstractMethodDeclaration)exceptionContext.associatedNode;
					if (method.isConstructor() && method.binding.declaringClass.isAnonymousType())
						shouldMergeUnhandledException = true;
				}
				if (shouldMergeUnhandledException) {
					for (int i = 0; i < raisedCount; i++) {
						TypeBinding raisedException;
						if ((raisedException = raisedExceptions[i]) != null) {
							exceptionContext.mergeUnhandledException(raisedException);
						}
					}
					return; // no need to complain, will fix up constructor/lambda exceptions
				}
				break; // not handled anywhere, thus jump to error handling
			}
        } else if (traversedContext instanceof LoopingFlowContext) {
			if (abruptlyExitedLoops == null) {
				abruptlyExitedLoops = new ArrayList(5);
			}
			abruptlyExitedLoops.add(traversedContext);
		}
		if (remainingCount == 0)
			return;

		traversedContext.recordReturnFrom(flowInfo.unconditionalInits());

		if (traversedContext instanceof InsideSubRoutineFlowContext) {
			ASTNode node = traversedContext.associatedNode;
			if (node instanceof TryStatement) {
				TryStatement tryStatement = (TryStatement) node;
				flowInfo.addInitializationsFrom(tryStatement.subRoutineInits); // collect inits
			}
		}
		traversedContext = traversedContext.getLocalParent();
	}
	// if reaches this point, then there are some remaining unhandled exception types.
	nextReport: for (int i = 0; i < raisedCount; i++) {
		TypeBinding exception;
		if ((exception = raisedExceptions[i]) != null) {
			// only one complaint if same exception declared to be thrown more than once
			for (int j = 0; j < i; j++) {
				if (TypeBinding.equalsEquals(raisedExceptions[j], exception)) continue nextReport; // already reported
			}
			scope.problemReporter().unhandledException(exception, location);
		}
	}
}

public BranchLabel continueLabel() {
	return null;
}

public FlowInfo getInitsForFinalBlankInitializationCheck(TypeBinding declaringType, FlowInfo flowInfo) {
	FlowContext current = this;
	FlowInfo inits = flowInfo;
	do {
		if (current instanceof InitializationFlowContext) {
			InitializationFlowContext initializationContext = (InitializationFlowContext) current;
			if (TypeBinding.equalsEquals(((TypeDeclaration)initializationContext.associatedNode).binding, declaringType)) {
				return inits;
			}
			inits = initializationContext.initsBeforeContext;
			current = initializationContext.initializationParent;
		} else if (current instanceof ExceptionHandlingFlowContext) {
			if(current instanceof FieldInitsFakingFlowContext) {
				return FlowInfo.DEAD_END; // isDefinitelyAssigned will return true for all fields
			}
			ExceptionHandlingFlowContext exceptionContext = (ExceptionHandlingFlowContext) current;
			current = exceptionContext.initializationParent == null ? exceptionContext.parent : exceptionContext.initializationParent;
		} else {
			current = current.getLocalParent();
		}
	} while (current != null);
	// not found
	throw new IllegalStateException(declaringType.debugName());
}

/*
 * lookup through break labels
 */
public FlowContext getTargetContextForBreakLabel(char[] labelName) {
	FlowContext current = this, lastNonReturningSubRoutine = null;
	while (current != null) {
		if (current.isNonReturningContext()) {
			lastNonReturningSubRoutine = current;
		}
		char[] currentLabelName;
		if (((currentLabelName = current.labelName()) != null)
			&& CharOperation.equals(currentLabelName, labelName)) {
			((LabeledStatement)current.associatedNode).bits |= ASTNode.LabelUsed;
			if (lastNonReturningSubRoutine == null)
				return current;
			return lastNonReturningSubRoutine;
		}
		current = current.getLocalParent();
	}
	// not found
	return null;
}

/*
 * lookup through continue labels
 */
public FlowContext getTargetContextForContinueLabel(char[] labelName) {
	FlowContext current = this;
	FlowContext lastContinuable = null;
	FlowContext lastNonReturningSubRoutine = null;

	while (current != null) {
		if (current.isNonReturningContext()) {
			lastNonReturningSubRoutine = current;
		} else {
			if (current.isContinuable()) {
				lastContinuable = current;
			}
		}

		char[] currentLabelName;
		if ((currentLabelName = current.labelName()) != null && CharOperation.equals(currentLabelName, labelName)) {
			((LabeledStatement)current.associatedNode).bits |= ASTNode.LabelUsed;

			// matching label found
			if ((lastContinuable != null)
					&& (current.associatedNode.concreteStatement()	== lastContinuable.associatedNode)) {

				if (lastNonReturningSubRoutine == null) return lastContinuable;
				return lastNonReturningSubRoutine;
			}
			// label is found, but not a continuable location
			return FlowContext.NotContinuableContext;
		}
		current = current.getLocalParent();
	}
	// not found
	return null;
}

/*
 * lookup a default break through breakable locations
 */
public FlowContext getTargetContextForDefaultBreak() {
	FlowContext current = this, lastNonReturningSubRoutine = null;
	while (current != null) {
		if (current.isNonReturningContext()) {
			lastNonReturningSubRoutine = current;
		}
		if (current.isBreakable() && current.labelName() == null) {
			if (lastNonReturningSubRoutine == null) return current;
			return lastNonReturningSubRoutine;
		}
		current = current.getLocalParent();
	}
	// not found
	return null;
}
/*
 * lookup a default yield through switch expression locations
 */
public FlowContext getTargetContextForDefaultYield() {
	FlowContext current = this, lastNonReturningSubRoutine = null;
	while (current != null) {
		if (current.isNonReturningContext()) {
			lastNonReturningSubRoutine = current;
		}
		if (current.isBreakable() && current.labelName() == null && ((SwitchFlowContext) current).isExpression){
			if (lastNonReturningSubRoutine == null) return current;
			return lastNonReturningSubRoutine;
		}
		current = current.getLocalParent();
	}
	// not found
	return null;
}

/*
 * lookup a default continue amongst continuable locations
 */
public FlowContext getTargetContextForDefaultContinue() {
	FlowContext current = this, lastNonReturningSubRoutine = null;
	while (current != null) {
		if (current.isNonReturningContext()) {
			lastNonReturningSubRoutine = current;
		}
		if (current.isContinuable()) {
			if (lastNonReturningSubRoutine == null)
				return current;
			return lastNonReturningSubRoutine;
		}
		current = current.getLocalParent();
	}
	// not found
	return null;
}

/**
 * Answer flow context that corresponds to initialization. Suitably override in subtypes.
 */
public FlowContext getInitializationContext() {
	return null;
}

/**
 * Answer the parent flow context but be careful not to cross the boundary of a nested type,
 * or null if no such parent exists.
 */
public FlowContext getLocalParent() {
	if (this.associatedNode instanceof AbstractMethodDeclaration || this.associatedNode instanceof TypeDeclaration || this.associatedNode instanceof LambdaExpression)
		return null;
	return this.parent;
}

public String individualToString() {
	return "Flow context"; //$NON-NLS-1$
}

public FlowInfo initsOnBreak() {
	return FlowInfo.DEAD_END;
}

public UnconditionalFlowInfo initsOnReturn() {
	return FlowInfo.DEAD_END;
}

public boolean isBreakable() {
	return false;
}

public boolean isContinuable() {
	return false;
}

public boolean isNonReturningContext() {
	return false;
}

public boolean isSubRoutine() {
	return false;
}

public char[] labelName() {
	return null;
}

/**
 * Record a given null status of a given local variable as it will be seen in the finally block.
 * @param local the local variable being observed
 * @param nullStatus the null status of local at the current point in the flow
 */
public void markFinallyNullStatus(LocalVariableBinding local, int nullStatus) {
	if (this.initsOnFinally == null) return;
	if (this.conditionalLevel == -1) return;
	if (this.conditionalLevel == 0) {
		// node is unconditionally reached, take nullStatus as is:
		this.initsOnFinally.markNullStatus(local, nullStatus);
		return;
	}
	// node is reached only conditionally, weaken status to potentially_ and merge with previous
	UnconditionalFlowInfo newInfo = this.initsOnFinally.unconditionalCopy();
	newInfo.markNullStatus(local, nullStatus);
	this.initsOnFinally = this.initsOnFinally.mergedWith(newInfo);
}

/**
 * Merge the effect of a statement presumably contained in a try-block,
 * i.e., record how the collected info will affect the corresponding finally-block.
 * Precondition: caller has checked that initsOnFinally != null.
 * @param flowInfo info after executing a statement of the try-block.
 */
public void mergeFinallyNullInfo(FlowInfo flowInfo) {
	if (this.initsOnFinally == null) return;
	if (this.conditionalLevel == -1) return;
	if (this.conditionalLevel == 0) {
		// node is unconditionally reached, take null info as is:
		this.initsOnFinally.addNullInfoFrom(flowInfo);
		return;
	}
	// node is reached only conditionally: merge flowInfo with existing since both paths are possible
	this.initsOnFinally = this.initsOnFinally.mergedWith(flowInfo.unconditionalCopy());
}

/**
 * Record the fact that an abrupt exit has been observed, one of:
 * - potential exception (incl. unchecked exceptions)
 * - break
 * - continue
 * - return
 */
public void recordAbruptExit() {
	if (this.conditionalLevel > -1) {
		this.conditionalLevel++;
		// delegate up up-to the enclosing try-finally:
		if (!(this instanceof ExceptionHandlingFlowContext) && this.parent != null) {
			this.parent.recordAbruptExit();
		}
	}
}

public void recordBreakFrom(FlowInfo flowInfo) {
	// default implementation: do nothing
}

public void recordBreakTo(FlowContext targetContext) {
	// default implementation: do nothing
}

public void recordContinueFrom(FlowContext innerFlowContext, FlowInfo flowInfo) {
	// default implementation: do nothing
}

/**
 * Record that we found an early exit from a method while a resource is in scope.
 * @param scope enclosing scope
 * @param flowInfo flowInfo at the point of the early exit
 * @param trackingVar representation of the resource
 * @param reference the return or throw statement marking the early exit
 * @return true if the situation has been handled by this flow context.
 */
public boolean recordExitAgainstResource(BlockScope scope, FlowInfo flowInfo, FakedTrackingVariable trackingVar, ASTNode reference) {
	return false; // not handled
}

protected void recordProvidedExpectedTypes(TypeBinding providedType, TypeBinding expectedType, int nullCount) {
	if (nullCount == 0) {
		this.providedExpectedTypes = new TypeBinding[5][];
	} else if (this.providedExpectedTypes == null) {
		int size = 5;
		while (size <= nullCount) size *= 2;
		this.providedExpectedTypes = new TypeBinding[size][];
	}
	else if (nullCount >= this.providedExpectedTypes.length) {
		int oldLen = this.providedExpectedTypes.length;
		System.arraycopy(this.providedExpectedTypes, 0,
			this.providedExpectedTypes = new TypeBinding[nullCount * 2][], 0, oldLen);
	}
	this.providedExpectedTypes[nullCount] = new TypeBinding[]{providedType, expectedType};
}

protected boolean recordFinalAssignment(VariableBinding variable, Reference finalReference) {
	return true; // keep going
}

/**
 * Record a null reference for use by deferred checks. Only looping or
 * finally contexts really record that information. Other contexts
 * immediately check for unboxing.
 * @param local the local variable involved in the check
 * @param location the location triggering the analysis, for normal null dereference
 *      this is an expression resolving to 'local', for resource leaks it is an
 *      early exit statement.
 * @param checkType the checkType against which the check must be performed; one of
 * 		{@link #CAN_ONLY_NULL CAN_ONLY_NULL}, {@link #CAN_ONLY_NULL_NON_NULL
 * 		CAN_ONLY_NULL_NON_NULL}, {@link #MAY_NULL MAY_NULL},
 *      {@link #CAN_ONLY_NON_NULL CAN_ONLY_NON_NULL}, potentially
 *      combined with a context indicator (one of {@link #IN_COMPARISON_NULL},
 *      {@link #IN_COMPARISON_NON_NULL}, {@link #IN_ASSIGNMENT} or {@link #IN_INSTANCEOF}).
 *      <br>
 *      Alternatively, a {@link #IN_UNBOXING} check can e requested.
 * @param nullInfo the null flow info observed at this first visit of location.
 */
protected final void recordNullReference(LocalVariableBinding local,
	ASTNode location, int checkType, FlowInfo nullInfo) {
	recordNullReferenceWithAnnotationStatus(local, location, checkType, nullInfo, null);
}

/**
 * Record a null reference for use by deferred checks. Only looping or
 * finally contexts really record that information. Other contexts
 * immediately check for unboxing.
 * @param local the local variable involved in the check
 * @param location the location triggering the analysis, for normal null dereference
 *      this is an expression resolving to 'local', for resource leaks it is an
 *      early exit statement.
 * @param checkType the checkType against which the check must be performed; one of
 * 		{@link #CAN_ONLY_NULL CAN_ONLY_NULL}, {@link #CAN_ONLY_NULL_NON_NULL
 * 		CAN_ONLY_NULL_NON_NULL}, {@link #MAY_NULL MAY_NULL},
 *      {@link #CAN_ONLY_NON_NULL CAN_ONLY_NON_NULL}, potentially
 *      combined with a context indicator (one of {@link #IN_COMPARISON_NULL},
 *      {@link #IN_COMPARISON_NON_NULL}, {@link #IN_ASSIGNMENT} or {@link #IN_INSTANCEOF}).
 *      <br>
 *      Alternatively, a {@link #IN_UNBOXING} check can e requested.
 * @param nullInfo the null flow info observed at this first visit of location.
 * @param nullAnnotationStatus if null annotations are analysed this may hold more information
 * 		about the exact kind of problem, can be <code>null</code>
 */
protected void recordNullReferenceWithAnnotationStatus(LocalVariableBinding local,
	ASTNode location, int checkType, FlowInfo nullInfo, NullAnnotationMatching nullAnnotationStatus) {
	// default implementation: do nothing
}

/**
 * Either AST analysis or checking of a child flow context has encountered an unboxing situation.
 * Record this fact for handling at an appropriate point in time.
 * @param nullStatus the status as we know it so far.
 */
public void recordUnboxing(Scope scope, Expression expression, int nullStatus, FlowInfo flowInfo) {
	// default: handle immediately:
	checkUnboxing(scope, expression, flowInfo);
}
/** During deferred checking re-visit a previously recording unboxing situation. */
protected void checkUnboxing(Scope scope, Expression expression, FlowInfo flowInfo) {
	int status = expression.nullStatus(flowInfo, this);
	if ((status & FlowInfo.NULL) != 0) {
		scope.problemReporter().nullUnboxing(expression, expression.resolvedType);
		return;
	} else if ((status & FlowInfo.POTENTIALLY_NULL) != 0) {
		scope.problemReporter().potentialNullUnboxing(expression, expression.resolvedType);
		return;
	} else if ((status & FlowInfo.NON_NULL) != 0) {
		return;
	}
	// not handled, perhaps our parent will eventually have something to say?
	if (this.parent != null) {
		this.parent.recordUnboxing(scope, expression, FlowInfo.UNKNOWN, flowInfo);
	}
}

public void recordReturnFrom(UnconditionalFlowInfo flowInfo) {
	// default implementation: do nothing
}

public void recordSettingFinal(VariableBinding variable, Reference finalReference, FlowInfo flowInfo) {
	if ((flowInfo.tagBits & FlowInfo.UNREACHABLE_OR_DEAD) == 0)	{
	// for initialization inside looping statement that effectively loops
	FlowContext context = this;
	while (context != null) {
		if (!context.recordFinalAssignment(variable, finalReference)) {
			break; // no need to keep going
		}
		context = context.getLocalParent();
	}
	}
}

/**
 * Record a null reference for use by deferred checks. Only looping or
 * finally contexts really record that information. The context may
 * emit an error immediately depending on the status of local against
 * flowInfo and its nature (only looping of finally contexts defer part
 * of the checks; nonetheless, contexts that are nested into a looping or a
 * finally context get affected and delegate some checks to their enclosing
 * context).
 * @param scope the scope into which the check is performed
 * @param local the local variable involved in the check
 * @param location the location triggering the analysis, for normal null dereference
 *      this is an expression resolving to 'local', for resource leaks it is an
 *      early exit statement.
 * @param checkType the status against which the check must be performed; one
 * 		of {@link #CAN_ONLY_NULL CAN_ONLY_NULL}, {@link #CAN_ONLY_NULL_NON_NULL
 * 		CAN_ONLY_NULL_NON_NULL}, {@link #MAY_NULL MAY_NULL}, potentially
 *      combined with a context indicator (one of {@link #IN_COMPARISON_NULL},
 *      {@link #IN_COMPARISON_NON_NULL}, {@link #IN_ASSIGNMENT} or {@link #IN_INSTANCEOF})
 *      and a bit to indicate whether the reference is being recorded inside an assert,
 *      {@link #HIDE_NULL_COMPARISON_WARNING}
 * @param flowInfo the flow info at the check point; deferring contexts will
 *  	perform supplementary checks against flow info instances that cannot
 *  	be known at the time of calling this method (they are influenced by
 * 		code that follows the current point)
 */
public void recordUsingNullReference(Scope scope, LocalVariableBinding local,
		ASTNode location, int checkType, FlowInfo flowInfo) {
	if ((flowInfo.tagBits & FlowInfo.UNREACHABLE) != 0 ||
			flowInfo.isDefinitelyUnknown(local)) {
		return;
	}
	// if reference is being recorded inside an assert, we will not raise redundant null check warnings
	checkType |= (this.tagBits & FlowContext.HIDE_NULL_COMPARISON_WARNING);
	int checkTypeWithoutHideNullWarning = checkType & ~FlowContext.HIDE_NULL_COMPARISON_WARNING_MASK;
	switch (checkTypeWithoutHideNullWarning) {
		case CAN_ONLY_NULL_NON_NULL | IN_COMPARISON_NULL:
		case CAN_ONLY_NULL_NON_NULL | IN_COMPARISON_NON_NULL:
			if (flowInfo.isDefinitelyNonNull(local)) {
				if (checkTypeWithoutHideNullWarning == (CAN_ONLY_NULL_NON_NULL | IN_COMPARISON_NON_NULL)) {
					if ((checkType & HIDE_NULL_COMPARISON_WARNING) == 0) {
						scope.problemReporter().localVariableRedundantCheckOnNonNull(local, location);
					}
					flowInfo.initsWhenFalse().setReachMode(FlowInfo.UNREACHABLE_BY_NULLANALYSIS);
				} else {
					scope.problemReporter().localVariableNonNullComparedToNull(local, location);
					flowInfo.initsWhenTrue().setReachMode(FlowInfo.UNREACHABLE_BY_NULLANALYSIS);
				}
				return;
			}
			else if (flowInfo.cannotBeDefinitelyNullOrNonNull(local)) {
				return;
			}
			//$FALL-THROUGH$
		case CAN_ONLY_NULL | IN_COMPARISON_NULL:
		case CAN_ONLY_NULL | IN_COMPARISON_NON_NULL:
		case CAN_ONLY_NULL | IN_ASSIGNMENT:
		case CAN_ONLY_NULL | IN_INSTANCEOF:
			Expression reference = (Expression)location;
			if (flowInfo.isDefinitelyNull(local)) {
				switch(checkTypeWithoutHideNullWarning & CONTEXT_MASK) {
					case FlowContext.IN_COMPARISON_NULL:
						if (((checkTypeWithoutHideNullWarning & CHECK_MASK) == CAN_ONLY_NULL) && (reference.implicitConversion & TypeIds.UNBOXING) != 0) { // check for auto-unboxing first and report appropriate warning
							scope.problemReporter().localVariableNullReference(local, reference);
							return;
						}
						if ((checkType & HIDE_NULL_COMPARISON_WARNING) == 0) {
							scope.problemReporter().localVariableRedundantCheckOnNull(local, reference);
						}
						flowInfo.initsWhenFalse().setReachMode(FlowInfo.UNREACHABLE_BY_NULLANALYSIS);
						return;
					case FlowContext.IN_COMPARISON_NON_NULL:
						if (((checkTypeWithoutHideNullWarning & CHECK_MASK) == CAN_ONLY_NULL) && (reference.implicitConversion & TypeIds.UNBOXING) != 0) { // check for auto-unboxing first and report appropriate warning
							scope.problemReporter().localVariableNullReference(local, reference);
							return;
						}
						scope.problemReporter().localVariableNullComparedToNonNull(local, reference);
						flowInfo.initsWhenTrue().setReachMode(FlowInfo.UNREACHABLE_BY_NULLANALYSIS);
						return;
					case FlowContext.IN_ASSIGNMENT:
						scope.problemReporter().localVariableRedundantNullAssignment(local, reference);
						return;
					case FlowContext.IN_INSTANCEOF:
						scope.problemReporter().localVariableNullInstanceof(local, reference);
						return;
				}
			} else if (flowInfo.isPotentiallyNull(local)) {
				switch(checkTypeWithoutHideNullWarning & CONTEXT_MASK) {
					case FlowContext.IN_COMPARISON_NULL:
						if (((checkTypeWithoutHideNullWarning & CHECK_MASK) == CAN_ONLY_NULL) && (reference.implicitConversion & TypeIds.UNBOXING) != 0) { // check for auto-unboxing first and report appropriate warning
							scope.problemReporter().localVariablePotentialNullReference(local, reference);
							return;
						}
						break;
					case FlowContext.IN_COMPARISON_NON_NULL:
						if (((checkTypeWithoutHideNullWarning & CHECK_MASK) == CAN_ONLY_NULL) && (reference.implicitConversion & TypeIds.UNBOXING) != 0) { // check for auto-unboxing first and report appropriate warning
							scope.problemReporter().localVariablePotentialNullReference(local, reference);
							return;
						}
						break;
				}
			} else if (flowInfo.cannotBeDefinitelyNullOrNonNull(local)) {
				return;
			}
			break;
		case MAY_NULL :
			if (flowInfo.isDefinitelyNull(local)) {
				scope.problemReporter().localVariableNullReference(local, location);
				return;
			}
			if (flowInfo.isPotentiallyNull(local)) {
				if(local.type.isFreeTypeVariable()) {
					scope.problemReporter().localVariableFreeTypeVariableReference(local, location);
					return;
				}
				scope.problemReporter().localVariablePotentialNullReference(local, location);
				return;
			}
			break;
		default:
			// never happens
	}
	if (this.parent != null) {
		this.parent.recordUsingNullReference(scope, local, location, checkType,
				flowInfo);
	}
}

void removeFinalAssignmentIfAny(Reference reference) {
	// default implementation: do nothing
}

public SubRoutineStatement subroutine() {
	return null;
}

@Override
public String toString() {
	StringBuilder buffer = new StringBuilder();
	FlowContext current = this;
	int parentsCount = 0;
	while ((current = current.parent) != null) {
		parentsCount++;
	}
	FlowContext[] parents = new FlowContext[parentsCount + 1];
	current = this;
	int index = parentsCount;
	while (index >= 0) {
		parents[index--] = current;
		current = current.parent;
	}
	for (int i = 0; i < parentsCount; i++) {
		for (int j = 0; j < i; j++)
			buffer.append('\t');
		buffer.append(parents[i].individualToString()).append('\n');
	}
	buffer.append('*');
	for (int j = 0; j < parentsCount + 1; j++)
		buffer.append('\t');
	buffer.append(individualToString()).append('\n');
	return buffer.toString();
}

/**
 * Record that a nullity mismatch was detected against an annotated type reference.
 * @param currentScope scope for error reporting
 * @param expression the expression violating the specification
 * @param providedType the type of the provided value, i.e., either expression or an element thereof (in ForeachStatements)
 * @param expectedType the declared type of the spec'ed variable, for error reporting.
 * @param flowInfo the flowInfo observed when visiting expression
 * @param nullStatus the null status of expression at the current location
 * @param annotationStatus status from type annotation analysis, or null
 */
public void recordNullityMismatch(BlockScope currentScope, Expression expression, TypeBinding providedType, TypeBinding expectedType, FlowInfo flowInfo, int nullStatus, NullAnnotationMatching annotationStatus) {
	if (providedType == null) {
		return; // assume type error was already reported
	}
	if (expression.localVariableBinding() != null) { // flowContext cannot yet handle non-localvar expressions (e.g., fields)
		// find the inner-most flowContext that might need deferred handling:
		FlowContext currentContext = this;
		while (currentContext != null) {
			// some flow contexts implement deferred checking, should we participate in that?
			int isInsideAssert = 0x0;
			if ((this.tagBits & FlowContext.HIDE_NULL_COMPARISON_WARNING) != 0) {
				isInsideAssert = FlowContext.HIDE_NULL_COMPARISON_WARNING;
			}
			if (currentContext.internalRecordNullityMismatch(expression, providedType, flowInfo, nullStatus, annotationStatus, expectedType, ASSIGN_TO_NONNULL | isInsideAssert))
				return;
			currentContext = currentContext.parent;
		}
	}
	// no reason to defer, so report now:
	if (annotationStatus != null)
		currentScope.problemReporter().nullityMismatchingTypeAnnotation(expression, providedType, expectedType, annotationStatus);
	else
		currentScope.problemReporter().nullityMismatch(expression, providedType, expectedType, nullStatus,
														currentScope.environment().getNonNullAnnotationName());
}
protected boolean internalRecordNullityMismatch(Expression expression, TypeBinding providedType, FlowInfo flowInfo, int nullStatus, NullAnnotationMatching nullAnnotationStatus, TypeBinding expectedType, int checkType) {
	// nop, to be overridden in subclasses
	return false; // not recorded
}
}
