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
 *     Stephan Herrmann - contributions for
 *     							bug 336428 - [compiler][null] bogus warning "redundant null check" in condition of do {} while() loop
 *								bug 186342 - [compiler][null] Using annotations for null checking
 *								bug 365519 - editorial cleanup after bug 186342 and bug 365387
 *								bug 368546 - [compiler][resource] Avoid remaining false positives found when compiling the Eclipse SDK
 *								bug 365859 - [compiler][null] distinguish warnings based on flow analysis vs. null annotations
 *								bug 385626 - @NonNull fails across loop boundaries
 *								bug 345305 - [compiler][null] Compiler misidentifies a case of "variable can only be null"
 *								bug 376263 - Bogus "Potential null pointer access" warning
 *								bug 403147 - [compiler][null] FUP of bug 400761: consolidate interaction between unboxing, NPE, and deferred checking
 *								bug 406384 - Internal error with I20130413
 *								Bug 415413 - [compiler][null] NullpointerException in Null Analysis caused by interaction of LoopingFlowContext and FinallyFlowContext
 *								Bug 453483 - [compiler][null][loop] Improve null analysis for loops
 *								Bug 455557 - [jdt] NPE LoopingFlowContext.recordNullReference
 *								Bug 455723 - Nonnull argument not correctly inferred in loop
 *								Bug 415790 - [compiler][resource]Incorrect potential resource leak warning in for loop with close in try/catch
 *								Bug 421035 - [resource] False alarm of resource leak warning when casting a closeable in its assignment
 *     Jesper S Moller - contributions for
 *								bug 404657 - [1.8][compiler] Analysis for effectively final variables fails to consider loops
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.flow;

import java.util.ArrayList;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FakedTrackingVariable;
import org.eclipse.jdt.internal.compiler.ast.NullAnnotationMatching;
import org.eclipse.jdt.internal.compiler.ast.Reference;
import org.eclipse.jdt.internal.compiler.codegen.BranchLabel;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.lookup.VariableBinding;

/**
 * Reflects the context of code analysis, keeping track of enclosing
 *	try statements, exception handlers, etc...
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class LoopingFlowContext extends SwitchFlowContext {

	public BranchLabel continueLabel;
	public UnconditionalFlowInfo initsOnContinue = FlowInfo.DEAD_END;
	private UnconditionalFlowInfo upstreamNullFlowInfo;
	private LoopingFlowContext innerFlowContexts[] = null;
	private UnconditionalFlowInfo innerFlowInfos[] = null;
	private int innerFlowContextsCount = 0;
	private LabelFlowContext breakTargetContexts[] = null;
	private int breakTargetsCount = 0;

	Reference finalAssignments[];
	VariableBinding finalVariables[];
	int assignCount = 0;

	// the following three arrays are in sync regarding their indices:
	LocalVariableBinding[] nullLocals; // slots can be null for checkType == IN_UNBOXING
	ASTNode[] nullReferences;	// Expressions for null checking, Statements for resource analysis
								// cast to Expression is safe if corresponding nullCheckType != EXIT_RESOURCE
	int[] nullCheckTypes;
	UnconditionalFlowInfo[] nullInfos;	// detailed null info observed during the first visit of nullReferences[i], or null
	NullAnnotationMatching[] nullAnnotationStatuses;
	int nullCount;
	// see also the related field FlowContext#expectedTypes

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=321926
	static private class EscapingExceptionCatchSite {
		final ReferenceBinding caughtException;
		final ExceptionHandlingFlowContext catchingContext;
		final FlowInfo exceptionInfo; // flow leading to the location of throwing
		public EscapingExceptionCatchSite(ExceptionHandlingFlowContext catchingContext,	ReferenceBinding caughtException, FlowInfo exceptionInfo) {
			this.catchingContext = catchingContext;
			this.caughtException = caughtException;
			this.exceptionInfo = exceptionInfo;
		}
		void simulateThrowAfterLoopBack(FlowInfo flowInfo) {
			this.catchingContext.recordHandlingException(this.caughtException,
					flowInfo.unconditionalCopy().addNullInfoFrom(this.exceptionInfo).unconditionalInits(),
					null, // raised exception, irrelevant here,
					null, null, /* invocation site, irrelevant here */ true // we have no business altering the needed status.
					);
		}
	}
	private ArrayList escapingExceptionCatchSites = null;

	Scope associatedScope;

	public LoopingFlowContext(
		FlowContext parent,
		FlowInfo upstreamNullFlowInfo,
		ASTNode associatedNode,
		BranchLabel breakLabel,
		BranchLabel continueLabel,
		Scope associatedScope,
		boolean isPreTest) {
		super(parent, associatedNode, breakLabel, isPreTest, false);
		this.tagBits |= FlowContext.PREEMPT_NULL_DIAGNOSTIC;
			// children will defer to this, which may defer to its own parent
		this.continueLabel = continueLabel;
		this.associatedScope = associatedScope;
		this.upstreamNullFlowInfo = upstreamNullFlowInfo.unconditionalCopy();
	}

/**
 * Perform deferred checks relative to final variables duplicate initialization
 * of lack of initialization.
 * @param scope the scope to which this context is associated
 * @param flowInfo the flow info against which checks must be performed
 */
public void complainOnDeferredFinalChecks(BlockScope scope, FlowInfo flowInfo) {
	// complain on final assignments in loops
	for (int i = 0; i < this.assignCount; i++) {
		VariableBinding variable = this.finalVariables[i];
		if (variable == null) continue;
		boolean complained = false; // remember if have complained on this final assignment
		if (variable instanceof FieldBinding) {
			if (flowInfo.isPotentiallyAssigned((FieldBinding)variable)) {
				complained = true;
				scope.problemReporter().duplicateInitializationOfBlankFinalField(
					(FieldBinding) variable,
					this.finalAssignments[i]);
			}
		} else {
			if (flowInfo.isPotentiallyAssigned((LocalVariableBinding)variable)) {
				variable.tagBits &= ~TagBits.IsEffectivelyFinal;
				if (variable.isFinal()) {
					complained = true;
					scope.problemReporter().duplicateInitializationOfFinalLocal(
						(LocalVariableBinding) variable,
						this.finalAssignments[i]);
				}
			}
		}
		// any reference reported at this level is removed from the parent context where it
		// could also be reported again
		if (complained) {
			FlowContext context = this.getLocalParent();
			while (context != null) {
				context.removeFinalAssignmentIfAny(this.finalAssignments[i]);
				context = context.getLocalParent();
			}
		}
	}
}

/**
 * Perform deferred checks relative to the null status of local variables.
 * @param scope the scope to which this context is associated
 * @param callerFlowInfo the flow info against which checks must be performed
 */
public void complainOnDeferredNullChecks(BlockScope scope, FlowInfo callerFlowInfo) {
	complainOnDeferredNullChecks(scope, callerFlowInfo, true);
}
public void complainOnDeferredNullChecks(BlockScope scope, FlowInfo callerFlowInfo, boolean updateInitsOnBreak) {
	for (int i = 0 ; i < this.innerFlowContextsCount ; i++) {
		this.upstreamNullFlowInfo.
			addPotentialNullInfoFrom(
				this.innerFlowContexts[i].upstreamNullFlowInfo).
			addPotentialNullInfoFrom(this.innerFlowInfos[i]);
	}
	this.innerFlowContextsCount = 0;
	FlowInfo upstreamCopy = this.upstreamNullFlowInfo.copy();
	UnconditionalFlowInfo incomingInfo = this.upstreamNullFlowInfo.
		addPotentialNullInfoFrom(callerFlowInfo.unconditionalInitsWithoutSideEffect());
	if ((this.tagBits & FlowContext.DEFER_NULL_DIAGNOSTIC) != 0) {
		// check only immutable null checks on innermost looping context
		for (int i = 0; i < this.nullCount; i++) {
			LocalVariableBinding local = this.nullLocals[i];
			ASTNode location = this.nullReferences[i];
			FlowInfo flowInfo =  (this.nullInfos[i] != null)
									? incomingInfo.copy().addNullInfoFrom(this.nullInfos[i])
									: incomingInfo;
			// final local variable
			switch (this.nullCheckTypes[i] & ~HIDE_NULL_COMPARISON_WARNING_MASK) {
				case CAN_ONLY_NON_NULL | IN_COMPARISON_NULL:
				case CAN_ONLY_NON_NULL | IN_COMPARISON_NON_NULL:
					if (flowInfo.isDefinitelyNonNull(local)) {
						this.nullReferences[i] = null;
						if ((this.nullCheckTypes[i] & ~HIDE_NULL_COMPARISON_WARNING_MASK) == (CAN_ONLY_NON_NULL | IN_COMPARISON_NON_NULL)) {
							if ((this.nullCheckTypes[i] & HIDE_NULL_COMPARISON_WARNING) == 0) {
								scope.problemReporter().localVariableRedundantCheckOnNonNull(local, location);
							}
						} else {
							scope.problemReporter().localVariableNonNullComparedToNull(local, location);
						}
						continue;
					}
					break;
				case CAN_ONLY_NULL_NON_NULL | IN_COMPARISON_NULL:
				case CAN_ONLY_NULL_NON_NULL | IN_COMPARISON_NON_NULL:
					if (flowInfo.isDefinitelyNonNull(local)) {
						this.nullReferences[i] = null;
						if ((this.nullCheckTypes[i] & ~HIDE_NULL_COMPARISON_WARNING_MASK) == (CAN_ONLY_NULL_NON_NULL | IN_COMPARISON_NON_NULL)) {
							if ((this.nullCheckTypes[i] & HIDE_NULL_COMPARISON_WARNING) == 0) {
								scope.problemReporter().localVariableRedundantCheckOnNonNull(local, location);
							}
						} else {
							scope.problemReporter().localVariableNonNullComparedToNull(local, location);
						}
						continue;
					}
					if (flowInfo.isDefinitelyNull(local)) {
						this.nullReferences[i] = null;
						if ((this.nullCheckTypes[i] & ~HIDE_NULL_COMPARISON_WARNING_MASK) == (CAN_ONLY_NULL_NON_NULL | IN_COMPARISON_NULL)) {
							if ((this.nullCheckTypes[i] & HIDE_NULL_COMPARISON_WARNING) == 0) {
								scope.problemReporter().localVariableRedundantCheckOnNull(local, location);
							}
						} else {
							scope.problemReporter().localVariableNullComparedToNonNull(local, location);
						}
						continue;
					}
					break;
				case CAN_ONLY_NULL | IN_COMPARISON_NULL:
				case CAN_ONLY_NULL | IN_COMPARISON_NON_NULL:
				case CAN_ONLY_NULL | IN_ASSIGNMENT:
				case CAN_ONLY_NULL | IN_INSTANCEOF:
					Expression expression = (Expression)location;
					if (flowInfo.isDefinitelyNull(local)) {
						this.nullReferences[i] = null;
						switch(this.nullCheckTypes[i] & CONTEXT_MASK) {
							case FlowContext.IN_COMPARISON_NULL:
								if (((this.nullCheckTypes[i] & CHECK_MASK & ~HIDE_NULL_COMPARISON_WARNING_MASK) == CAN_ONLY_NULL) && (expression.implicitConversion & TypeIds.UNBOXING) != 0) { // check for auto-unboxing first and report appropriate warning
									scope.problemReporter().localVariableNullReference(local, expression);
									continue;
								}
								if ((this.nullCheckTypes[i] & HIDE_NULL_COMPARISON_WARNING) == 0) {
									scope.problemReporter().localVariableRedundantCheckOnNull(local, expression);
								}
								continue;
							case FlowContext.IN_COMPARISON_NON_NULL:
								if (((this.nullCheckTypes[i] & CHECK_MASK & ~HIDE_NULL_COMPARISON_WARNING_MASK) == CAN_ONLY_NULL) && (expression.implicitConversion & TypeIds.UNBOXING) != 0) { // check for auto-unboxing first and report appropriate warning
									scope.problemReporter().localVariableNullReference(local, expression);
									continue;
								}
								scope.problemReporter().localVariableNullComparedToNonNull(local, expression);
								continue;
							case FlowContext.IN_ASSIGNMENT:
								scope.problemReporter().localVariableRedundantNullAssignment(local, expression);
								continue;
							case FlowContext.IN_INSTANCEOF:
								scope.problemReporter().localVariableNullInstanceof(local, expression);
								continue;
						}
					} else if (flowInfo.isPotentiallyNull(local)) {
						switch(this.nullCheckTypes[i] & CONTEXT_MASK) {
							case FlowContext.IN_COMPARISON_NULL:
								this.nullReferences[i] = null;
								if (((this.nullCheckTypes[i] & CHECK_MASK & ~HIDE_NULL_COMPARISON_WARNING_MASK) == CAN_ONLY_NULL) && (expression.implicitConversion & TypeIds.UNBOXING) != 0) { // check for auto-unboxing first and report appropriate warning
									scope.problemReporter().localVariablePotentialNullReference(local, expression);
									continue;
								}
								break;
							case FlowContext.IN_COMPARISON_NON_NULL:
								this.nullReferences[i] = null;
								if (((this.nullCheckTypes[i] & CHECK_MASK & ~HIDE_NULL_COMPARISON_WARNING_MASK) == CAN_ONLY_NULL) && (expression.implicitConversion & TypeIds.UNBOXING) != 0) { // check for auto-unboxing first and report appropriate warning
									scope.problemReporter().localVariablePotentialNullReference(local, expression);
									continue;
								}
								break;
						}
					}
					break;
				case MAY_NULL:
					if (flowInfo.isDefinitelyNull(local)) {
						this.nullReferences[i] = null;
						scope.problemReporter().localVariableNullReference(local, location);
						continue;
					}
					break;
				case ASSIGN_TO_NONNULL:
					int nullStatus = flowInfo.nullStatus(local);
					if (nullStatus != FlowInfo.NON_NULL) {
						this.parent.recordNullityMismatch(scope, (Expression)location, this.providedExpectedTypes[i][0], this.providedExpectedTypes[i][1], flowInfo, nullStatus, null);
					}
					continue; // no more delegation to parent
				case EXIT_RESOURCE:
						FakedTrackingVariable trackingVar = local.closeTracker;
						if (trackingVar != null) {
							if (trackingVar.hasDefinitelyNoResource(flowInfo)) {
								continue; // no resource - no warning.
							}
							if (trackingVar.isClosedInFinallyOfEnclosing(scope)) {
								continue;
							}
							if (this.parent.recordExitAgainstResource(scope, flowInfo, trackingVar, location)) {
								this.nullReferences[i] = null;
								continue;
							}
						}
					break;
				case IN_UNBOXING:
					checkUnboxing(scope, (Expression) location, flowInfo);
					continue; // delegation to parent already handled in the above.
				default:
					// never happens
			}
			// https://bugs.eclipse.org/376263: avoid further deferring if the upstream info
			// already has definite information (which might get lost for deferred checking).
			if (!(this.nullCheckTypes[i] == MAY_NULL && upstreamCopy.isDefinitelyNonNull(local))) {
				this.parent.recordUsingNullReference(scope, local, location,
						this.nullCheckTypes[i], flowInfo);
			}
		}
	}
	else {
		// check inconsistent null checks on outermost looping context
		for (int i = 0; i < this.nullCount; i++) {
			ASTNode location = this.nullReferences[i];
			// final local variable
			LocalVariableBinding local = this.nullLocals[i];
			FlowInfo flowInfo =  (this.nullInfos[i] != null)
					? incomingInfo.copy().addNullInfoFrom(this.nullInfos[i])
					: incomingInfo;
			switch (this.nullCheckTypes[i] & ~HIDE_NULL_COMPARISON_WARNING_MASK) {
				case CAN_ONLY_NULL_NON_NULL | IN_COMPARISON_NULL:
				case CAN_ONLY_NULL_NON_NULL | IN_COMPARISON_NON_NULL:
					if (flowInfo.isDefinitelyNonNull(local)) {
						this.nullReferences[i] = null;
						if ((this.nullCheckTypes[i] & ~HIDE_NULL_COMPARISON_WARNING_MASK) == (CAN_ONLY_NULL_NON_NULL | IN_COMPARISON_NON_NULL)) {
							if ((this.nullCheckTypes[i] & HIDE_NULL_COMPARISON_WARNING) == 0) {
								scope.problemReporter().localVariableRedundantCheckOnNonNull(local, location);
							}
						} else {
							scope.problemReporter().localVariableNonNullComparedToNull(local, location);
						}
						continue;
					}
					//$FALL-THROUGH$
				case CAN_ONLY_NULL | IN_COMPARISON_NULL:
				case CAN_ONLY_NULL | IN_COMPARISON_NON_NULL:
				case CAN_ONLY_NULL | IN_ASSIGNMENT:
				case CAN_ONLY_NULL | IN_INSTANCEOF:
					Expression expression = (Expression) location;
					if (flowInfo.isDefinitelyNull(local)) {
						this.nullReferences[i] = null;
						switch(this.nullCheckTypes[i] & CONTEXT_MASK) {
							case FlowContext.IN_COMPARISON_NULL:
								if (((this.nullCheckTypes[i] & CHECK_MASK & ~HIDE_NULL_COMPARISON_WARNING_MASK) == CAN_ONLY_NULL) && (expression.implicitConversion & TypeIds.UNBOXING) != 0) { // check for auto-unboxing first and report appropriate warning
									scope.problemReporter().localVariableNullReference(local, expression);
									continue;
								}
								if ((this.nullCheckTypes[i] & HIDE_NULL_COMPARISON_WARNING) == 0) {
									scope.problemReporter().localVariableRedundantCheckOnNull(local, expression);
								}
								continue;
							case FlowContext.IN_COMPARISON_NON_NULL:
								if (((this.nullCheckTypes[i] & CHECK_MASK & ~HIDE_NULL_COMPARISON_WARNING_MASK) == CAN_ONLY_NULL) && (expression.implicitConversion & TypeIds.UNBOXING) != 0) { // check for auto-unboxing first and report appropriate warning
									scope.problemReporter().localVariableNullReference(local, expression);
									continue;
								}
								scope.problemReporter().localVariableNullComparedToNonNull(local, expression);
								continue;
							case FlowContext.IN_ASSIGNMENT:
								scope.problemReporter().localVariableRedundantNullAssignment(local, expression);
								continue;
							case FlowContext.IN_INSTANCEOF:
								scope.problemReporter().localVariableNullInstanceof(local, expression);
								continue;
						}
					} else if (flowInfo.isPotentiallyNull(local)) {
						switch(this.nullCheckTypes[i] & CONTEXT_MASK) {
							case FlowContext.IN_COMPARISON_NULL:
								this.nullReferences[i] = null;
								if (((this.nullCheckTypes[i] & CHECK_MASK & ~HIDE_NULL_COMPARISON_WARNING_MASK) == CAN_ONLY_NULL) && (expression.implicitConversion & TypeIds.UNBOXING) != 0) { // check for auto-unboxing first and report appropriate warning
									scope.problemReporter().localVariablePotentialNullReference(local, expression);
									continue;
								}
								break;
							case FlowContext.IN_COMPARISON_NON_NULL:
								this.nullReferences[i] = null;
								if (((this.nullCheckTypes[i] & CHECK_MASK & ~HIDE_NULL_COMPARISON_WARNING_MASK) == CAN_ONLY_NULL) && (expression.implicitConversion & TypeIds.UNBOXING) != 0) { // check for auto-unboxing first and report appropriate warning
									scope.problemReporter().localVariablePotentialNullReference(local, expression);
									continue;
								}
								break;
						}
					}
					break;
				case MAY_NULL:
					if (flowInfo.isDefinitelyNull(local)) {
						this.nullReferences[i] = null;
						scope.problemReporter().localVariableNullReference(local, location);
						continue;
					}
					if (flowInfo.isPotentiallyNull(local)) {
						this.nullReferences[i] = null;
						scope.problemReporter().localVariablePotentialNullReference(local, location);
						continue;
					}
					break;
				case ASSIGN_TO_NONNULL:
					int nullStatus = flowInfo.nullStatus(local);
					if (nullStatus != FlowInfo.NON_NULL) {
						char[][] annotationName = scope.environment().getNonNullAnnotationName();
						TypeBinding providedType = this.providedExpectedTypes[i][0];
						TypeBinding expectedType = this.providedExpectedTypes[i][1];
						Expression expression2 = (Expression) location;
						if (this.nullAnnotationStatuses[i] != null) {
							this.nullAnnotationStatuses[i] = this.nullAnnotationStatuses[i].withNullStatus(nullStatus);
							scope.problemReporter().nullityMismatchingTypeAnnotation(expression2, providedType, expectedType, this.nullAnnotationStatuses[i]);
						} else {
							scope.problemReporter().nullityMismatch(expression2, providedType, expectedType, nullStatus, annotationName);
						}
					}
					break;
				case EXIT_RESOURCE:
					nullStatus = flowInfo.nullStatus(local);
					if (nullStatus != FlowInfo.NON_NULL) {
						FakedTrackingVariable closeTracker = local.closeTracker;
						if (closeTracker != null) {
							if (closeTracker.hasDefinitelyNoResource(flowInfo)) {
								continue; // no resource - no warning.
							}
							if (closeTracker.isClosedInFinallyOfEnclosing(scope)) {
								continue;
							}
							nullStatus = closeTracker.findMostSpecificStatus(flowInfo, scope, null);
							closeTracker.recordErrorLocation(this.nullReferences[i], nullStatus);
							closeTracker.reportRecordedErrors(scope, nullStatus, flowInfo.reachMode() != FlowInfo.REACHABLE);
							this.nullReferences[i] = null;
							continue;
						}
					}
					break;
				case IN_UNBOXING:
					checkUnboxing(scope, (Expression) location, flowInfo);
					break;
				default:
					// never happens
			}
		}
	}
	// propagate breaks
	if(updateInitsOnBreak) {
		this.initsOnBreak.addPotentialNullInfoFrom(incomingInfo);
		for (int i = 0; i < this.breakTargetsCount; i++) {
			this.breakTargetContexts[i].initsOnBreak.addPotentialNullInfoFrom(incomingInfo);
		}
	}
}

	@Override
	public BranchLabel continueLabel() {
		return this.continueLabel;
	}

	@Override
	public String individualToString() {
		StringBuilder buffer = new StringBuilder("Looping flow context"); //$NON-NLS-1$
		buffer.append("[initsOnBreak - ").append(this.initsOnBreak.toString()).append(']'); //$NON-NLS-1$
		buffer.append("[initsOnContinue - ").append(this.initsOnContinue.toString()).append(']'); //$NON-NLS-1$
		buffer.append("[finalAssignments count - ").append(this.assignCount).append(']'); //$NON-NLS-1$
		buffer.append("[nullReferences count - ").append(this.nullCount).append(']'); //$NON-NLS-1$
		return buffer.toString();
	}

	@Override
	public boolean isContinuable() {
		return true;
	}

	public boolean isContinuedTo() {
		return this.initsOnContinue != FlowInfo.DEAD_END;
	}

@Override
public void recordBreakTo(FlowContext targetContext) {
	if (targetContext instanceof LabelFlowContext) {
		int current;
		if ((current = this.breakTargetsCount++) == 0) {
			this.breakTargetContexts = new LabelFlowContext[2];
		} else if (current == this.breakTargetContexts.length) {
			System.arraycopy(this.breakTargetContexts, 0, this.breakTargetContexts = new LabelFlowContext[current + 2], 0, current);
		}
		this.breakTargetContexts[current] = (LabelFlowContext) targetContext;
	}
}

@Override
public void recordContinueFrom(FlowContext innerFlowContext, FlowInfo flowInfo) {
	if ((flowInfo.tagBits & FlowInfo.UNREACHABLE_OR_DEAD) == 0)	{
		if ((this.initsOnContinue.tagBits & FlowInfo.UNREACHABLE_OR_DEAD) == 0) {
			this.initsOnContinue = this.initsOnContinue.
					mergedWith(flowInfo.unconditionalInitsWithoutSideEffect());
		}
		else {
			this.initsOnContinue = flowInfo.unconditionalCopy();
		}
		FlowContext inner = innerFlowContext;
		while (inner != this && !(inner instanceof LoopingFlowContext)) {
			inner = inner.parent;
			// we know that inner is reachable from this without crossing a type boundary
		}
		if (inner == this) {
			this.upstreamNullFlowInfo.
			addPotentialNullInfoFrom(
					flowInfo.unconditionalInitsWithoutSideEffect());
		}
		else {
			int length = 0;
			if (this.innerFlowContexts == null) {
				this.innerFlowContexts = new LoopingFlowContext[5];
				this.innerFlowInfos = new UnconditionalFlowInfo[5];
			}
			else if (this.innerFlowContextsCount ==
					(length = this.innerFlowContexts.length) - 1) {
				System.arraycopy(this.innerFlowContexts, 0,
						(this.innerFlowContexts = new LoopingFlowContext[length + 5]),
						0, length);
				System.arraycopy(this.innerFlowInfos, 0,
						(this.innerFlowInfos= new UnconditionalFlowInfo[length + 5]),
						0, length);
			}
			this.innerFlowContexts[this.innerFlowContextsCount] = (LoopingFlowContext) inner;
			this.innerFlowInfos[this.innerFlowContextsCount++] =
					flowInfo.unconditionalInitsWithoutSideEffect();
		}
	}
}

	@Override
	protected boolean recordFinalAssignment(
		VariableBinding binding,
		Reference finalAssignment) {

		// do not consider variables which are defined inside this loop
		if (binding instanceof LocalVariableBinding) {
			Scope scope = ((LocalVariableBinding) binding).declaringScope;
			while ((scope = scope.parent) != null) {
				if (scope == this.associatedScope)
					return false;
			}
		}
		if (this.assignCount == 0) {
			this.finalAssignments = new Reference[5];
			this.finalVariables = new VariableBinding[5];
		} else {
			if (this.assignCount == this.finalAssignments.length)
				System.arraycopy(
					this.finalAssignments,
					0,
					(this.finalAssignments = new Reference[this.assignCount * 2]),
					0,
					this.assignCount);
			System.arraycopy(
				this.finalVariables,
				0,
				(this.finalVariables = new VariableBinding[this.assignCount * 2]),
				0,
				this.assignCount);
		}
		this.finalAssignments[this.assignCount] = finalAssignment;
		this.finalVariables[this.assignCount++] = binding;
		return true;
	}

@Override
protected void recordNullReferenceWithAnnotationStatus(LocalVariableBinding local,
	ASTNode expression, int checkType, FlowInfo nullInfo, NullAnnotationMatching nullAnnotationStatus) {
	if (this.nullCount == 0) {
		this.nullLocals = new LocalVariableBinding[5];
		this.nullReferences = new ASTNode[5];
		this.nullCheckTypes = new int[5];
		this.nullInfos = new UnconditionalFlowInfo[5];
		this.nullAnnotationStatuses = new NullAnnotationMatching[5];
	}
	else if (this.nullCount == this.nullLocals.length) {
		System.arraycopy(this.nullLocals, 0,
			this.nullLocals = new LocalVariableBinding[this.nullCount * 2], 0, this.nullCount);
		System.arraycopy(this.nullReferences, 0,
			this.nullReferences = new ASTNode[this.nullCount * 2], 0, this.nullCount);
		System.arraycopy(this.nullCheckTypes, 0,
			this.nullCheckTypes = new int[this.nullCount * 2], 0, this.nullCount);
		System.arraycopy(this.nullInfos, 0,
			this.nullInfos = new UnconditionalFlowInfo[this.nullCount * 2], 0, this.nullCount);
		System.arraycopy(this.nullAnnotationStatuses, 0,
			this.nullAnnotationStatuses = new NullAnnotationMatching[this.nullCount * 2], 0, this.nullCount);
	}
	this.nullLocals[this.nullCount] = local;
	this.nullReferences[this.nullCount] = expression;
	this.nullCheckTypes[this.nullCount] = checkType;
	this.nullAnnotationStatuses[this.nullCount] = nullAnnotationStatus;
	this.nullInfos[this.nullCount++] = nullInfo != null ? nullInfo.unconditionalCopy() : null;
}
@Override
public void recordUnboxing(Scope scope, Expression expression, int nullStatus, FlowInfo flowInfo) {
	if (nullStatus == FlowInfo.NULL)
		super.recordUnboxing(scope, expression, nullStatus, flowInfo);
	else // defer checking:
		recordNullReference(null, expression, IN_UNBOXING, flowInfo);
}

/** Record the fact that we see an early exit (in 'reference') while 'trackingVar' is in scope and may be unclosed. */
@Override
public boolean recordExitAgainstResource(BlockScope scope, FlowInfo flowInfo, FakedTrackingVariable trackingVar, ASTNode reference) {
	LocalVariableBinding local = trackingVar.binding;
	if (flowInfo.isDefinitelyNonNull(local)) {
		return false;
	}
	if (flowInfo.isDefinitelyNull(local)) {
		scope.problemReporter().unclosedCloseable(trackingVar, reference);
		return true; // handled
	}
	if (flowInfo.isPotentiallyNull(local)) {
		scope.problemReporter().potentiallyUnclosedCloseable(trackingVar, reference);
		return true; // handled
	}
	recordNullReference(trackingVar.binding, reference, EXIT_RESOURCE, flowInfo);
	return true; // handled
}

@Override
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
			Expression reference = (Expression)location;
			if (flowInfo.isDefinitelyNonNull(local)) {
				if (checkTypeWithoutHideNullWarning == (CAN_ONLY_NULL_NON_NULL | IN_COMPARISON_NON_NULL)) {
					if ((this.tagBits & FlowContext.HIDE_NULL_COMPARISON_WARNING) == 0) {
						scope.problemReporter().localVariableRedundantCheckOnNonNull(local, reference);
					}
					flowInfo.initsWhenFalse().setReachMode(FlowInfo.UNREACHABLE_BY_NULLANALYSIS);
				} else {
					scope.problemReporter().localVariableNonNullComparedToNull(local, reference);
					flowInfo.initsWhenTrue().setReachMode(FlowInfo.UNREACHABLE_BY_NULLANALYSIS);
				}
			} else if (flowInfo.isDefinitelyNull(local)) {
				if (checkTypeWithoutHideNullWarning == (CAN_ONLY_NULL_NON_NULL | IN_COMPARISON_NULL)) {
					if ((this.tagBits & FlowContext.HIDE_NULL_COMPARISON_WARNING) == 0) {
						scope.problemReporter().localVariableRedundantCheckOnNull(local, reference);
					}
					flowInfo.initsWhenFalse().setReachMode(FlowInfo.UNREACHABLE_BY_NULLANALYSIS);
				} else {
					scope.problemReporter().localVariableNullComparedToNonNull(local, reference);
					flowInfo.initsWhenTrue().setReachMode(FlowInfo.UNREACHABLE_BY_NULLANALYSIS);
				}
			} else if (this.upstreamNullFlowInfo.isDefinitelyNonNull(local) && !flowInfo.isPotentiallyNull(local) && !flowInfo.isPotentiallyUnknown(local)) {
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=291418
				recordNullReference(local, reference, checkType, flowInfo);
				flowInfo.markAsDefinitelyNonNull(local);
			} else if (flowInfo.cannotBeDefinitelyNullOrNonNull(local)) {
				return; // no reason to complain, since there is definitely some uncertainty making the comparison relevant.
			} else {
					// note: pot non-null & pot null is already captured by cannotBeDefinitelyNullOrNonNull()
					if (flowInfo.isPotentiallyNonNull(local)) {
						// knowing 'local' can be non-null, we're only interested in seeing whether it can *only* be non-null
						recordNullReference(local, reference, CAN_ONLY_NON_NULL | checkType & (CONTEXT_MASK|HIDE_NULL_COMPARISON_WARNING_MASK), flowInfo);
					} else if (flowInfo.isPotentiallyNull(local)) {
						// knowing 'local' can be null, we're only interested in seeing whether it can *only* be null
						recordNullReference(local, reference, CAN_ONLY_NULL | checkType & (CONTEXT_MASK|HIDE_NULL_COMPARISON_WARNING_MASK), flowInfo);
					} else {
						recordNullReference(local, reference, checkType, flowInfo);
					}
			}
			return;
		case CAN_ONLY_NULL | IN_COMPARISON_NULL:
		case CAN_ONLY_NULL | IN_COMPARISON_NON_NULL:
		case CAN_ONLY_NULL | IN_ASSIGNMENT:
		case CAN_ONLY_NULL | IN_INSTANCEOF:
			reference = (Expression)location;
			if (flowInfo.isPotentiallyNonNull(local)
					|| flowInfo.isPotentiallyUnknown(local)
					|| flowInfo.isProtectedNonNull(local)) {
				// if variable is not null, we are not interested in recording null reference for deferred checks.
				// This is because CAN_ONLY_NULL means we're only interested in cases when variable can be null.
				return;
			}
			if (flowInfo.isDefinitelyNull(local)) {
				switch(checkTypeWithoutHideNullWarning & CONTEXT_MASK) {
					case FlowContext.IN_COMPARISON_NULL:
						if (((checkTypeWithoutHideNullWarning & CHECK_MASK) == CAN_ONLY_NULL) && (reference.implicitConversion & TypeIds.UNBOXING) != 0) { // check for auto-unboxing first and report appropriate warning
							scope.problemReporter().localVariableNullReference(local, reference);
							return;
						}
						if ((this.tagBits & FlowContext.HIDE_NULL_COMPARISON_WARNING) == 0) {
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
			}
			recordNullReference(local, reference, checkType, flowInfo);
			return;
		case MAY_NULL :
			if (flowInfo.isDefinitelyNonNull(local)) {
				return;
			}
			if (flowInfo.isDefinitelyNull(local)) {
				scope.problemReporter().localVariableNullReference(local, location);
				return;
			}
			if (flowInfo.isPotentiallyNull(local)) {
				scope.problemReporter().localVariablePotentialNullReference(local, location);
				return;
			}
			recordNullReference(local, location, checkType, flowInfo);
			return;
		default:
			// never happens
	}
}

	@Override
	void removeFinalAssignmentIfAny(Reference reference) {
		for (int i = 0; i < this.assignCount; i++) {
			if (this.finalAssignments[i] == reference) {
				this.finalAssignments[i] = null;
				this.finalVariables[i] = null;
				return;
			}
		}
	}

	/* Simulate a throw of an exception from inside a loop in its second or subsequent iteration.
	   See https://bugs.eclipse.org/bugs/show_bug.cgi?id=321926
	 */
	public void simulateThrowAfterLoopBack(FlowInfo flowInfo) {
		if (this.escapingExceptionCatchSites != null) {
			for (int i = 0, exceptionCount = this.escapingExceptionCatchSites.size(); i < exceptionCount; i++) {
				((EscapingExceptionCatchSite) this.escapingExceptionCatchSites.get(i)).simulateThrowAfterLoopBack(flowInfo);
			}
			this.escapingExceptionCatchSites = null; // don't care for it anymore.
		}
	}

	/* Record the fact that some exception thrown by code within this loop
	   is caught by an outer catch block. This is used to propagate data flow
	   along the edge back to the next iteration. See simulateThrowAfterLoopBack
	 */
	public void recordCatchContextOfEscapingException(ExceptionHandlingFlowContext catchingContext,	ReferenceBinding caughtException, FlowInfo exceptionInfo) {
		if (this.escapingExceptionCatchSites == null) {
			this.escapingExceptionCatchSites = new ArrayList(5);
		}
		this.escapingExceptionCatchSites.add(new EscapingExceptionCatchSite(catchingContext, caughtException, exceptionInfo));
	}

	public boolean hasEscapingExceptions() {
		return this.escapingExceptionCatchSites != null;
	}

	@Override
	protected boolean internalRecordNullityMismatch(Expression expression, TypeBinding providedType, FlowInfo flowInfo, int nullStatus, NullAnnotationMatching nullAnnotationStatus, TypeBinding expectedType, int checkType) {
		recordProvidedExpectedTypes(providedType, expectedType, this.nullCount);
		recordNullReferenceWithAnnotationStatus(expression.localVariableBinding(), expression, checkType, flowInfo, nullAnnotationStatus);
		return true;
	}
}
