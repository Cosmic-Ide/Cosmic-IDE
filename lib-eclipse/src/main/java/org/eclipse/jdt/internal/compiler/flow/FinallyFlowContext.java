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
 *								bug 186342 - [compiler][null] Using annotations for null checking
 *								bug 365519 - editorial cleanup after bug 186342 and bug 365387
 *								bug 368546 - [compiler][resource] Avoid remaining false positives found when compiling the Eclipse SDK
 *								bug 365859 - [compiler][null] distinguish warnings based on flow analysis vs. null annotations
 *								bug 385626 - @NonNull fails across loop boundaries
 *								bug 388996 - [compiler][resource] Incorrect 'potential resource leak'
 *								bug 403147 - [compiler][null] FUP of bug 400761: consolidate interaction between unboxing, NPE, and deferred checking
 *								Bug 453483 - [compiler][null][loop] Improve null analysis for loops
 *								Bug 455723 - Nonnull argument not correctly inferred in loop
 *     Jesper S Moller - Contributions for
 *								bug 404657 - [1.8][compiler] Analysis for effectively final variables fails to consider loops
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.flow;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.NullAnnotationMatching;
import org.eclipse.jdt.internal.compiler.ast.Reference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.lookup.VariableBinding;

/**
 * Reflects the context of code analysis, keeping track of enclosing
 *	try statements, exception handlers, etc...
 */
public class FinallyFlowContext extends TryFlowContext {

	Reference[] finalAssignments;
	VariableBinding[] finalVariables;
	int assignCount;

	// the following three arrays are in sync regarding their indices:
	LocalVariableBinding[] nullLocals; // slots can be null for checkType == IN_UNBOXING
	ASTNode[] nullReferences;	// Expressions for null checking, Statements for resource analysis
								// cast to Expression is safe if corresponding nullCheckType != EXIT_RESOURCE
	int[] nullCheckTypes;
	NullAnnotationMatching[] nullAnnotationStatuses;
	int nullCount;
	// see also the related field FlowContext#expectedTypes

	// back reference to the flow context of the corresponding try statement
	public FlowContext tryContext;

	public FinallyFlowContext(FlowContext parent, ASTNode associatedNode, ExceptionHandlingFlowContext tryContext) {
		super(parent, associatedNode);
		this.tryContext = tryContext;
	}

/**
 * Given some contextual initialization info (derived from a try block or a catch block), this
 * code will check that the subroutine context does not also initialize a final variable potentially set
 * redundantly.
 */
public void complainOnDeferredChecks(FlowInfo flowInfo, BlockScope scope) {

	// check redundant final assignments
	for (int i = 0; i < this.assignCount; i++) {
		VariableBinding variable = this.finalVariables[i];
		if (variable == null) continue;

		boolean complained = false; // remember if have complained on this final assignment
		if (variable instanceof FieldBinding) {
			// final field
			if (flowInfo.isPotentiallyAssigned((FieldBinding)variable)) {
				complained = true;
				scope.problemReporter().duplicateInitializationOfBlankFinalField((FieldBinding)variable, this.finalAssignments[i]);
			}
		} else {
			// final local variable
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
		// any reference reported at this level is removed from the parent context
		// where it could also be reported again
		if (complained) {
			FlowContext currentContext = this.getLocalParent();
			while (currentContext != null) {
				//if (currentContext.isSubRoutine()) {
				currentContext.removeFinalAssignmentIfAny(this.finalAssignments[i]);
				//}
				currentContext = currentContext.getLocalParent();
			}
		}
	}

	// check inconsistent null checks
	if ((this.tagBits & FlowContext.DEFER_NULL_DIAGNOSTIC) != 0) { // within an enclosing loop, be conservative
		for (int i = 0; i < this.nullCount; i++) {
			ASTNode location = this.nullReferences[i];
			switch (this.nullCheckTypes[i] & ~HIDE_NULL_COMPARISON_WARNING_MASK) {
				case ASSIGN_TO_NONNULL:
					int nullStatus = flowInfo.nullStatus(this.nullLocals[i]);
					if (nullStatus != FlowInfo.NON_NULL) {
						this.parent.recordNullityMismatch(scope, (Expression) location,
								this.providedExpectedTypes[i][0], this.providedExpectedTypes[i][1], flowInfo, nullStatus, null);
					}
					break;
				case IN_UNBOXING:
					checkUnboxing(scope, (Expression) location, flowInfo);
					break;
				default:
					this.parent.recordUsingNullReference(scope, this.nullLocals[i],
							this.nullReferences[i],	this.nullCheckTypes[i], flowInfo);
			}

		}
	}
	else { // no enclosing loop, be as precise as possible right now
		for (int i = 0; i < this.nullCount; i++) {
			ASTNode location = this.nullReferences[i];
			// final local variable
			LocalVariableBinding local = this.nullLocals[i];
			switch (this.nullCheckTypes[i] & ~HIDE_NULL_COMPARISON_WARNING_MASK) {
				case CAN_ONLY_NULL_NON_NULL | IN_COMPARISON_NULL:
				case CAN_ONLY_NULL_NON_NULL | IN_COMPARISON_NON_NULL:
					if (flowInfo.isDefinitelyNonNull(local)) {
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
						scope.problemReporter().localVariableNullReference(local, location);
						continue;
					}
					if (flowInfo.isPotentiallyNull(local)) {
						scope.problemReporter().localVariablePotentialNullReference(local, location);
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
				case IN_UNBOXING:
					checkUnboxing(scope, (Expression) location, flowInfo);
					break;
				default:
					// should not happen
			}
		}
	}
}

	@Override
	public String individualToString() {

		StringBuilder buffer = new StringBuilder("Finally flow context"); //$NON-NLS-1$
		buffer.append("[finalAssignments count - ").append(this.assignCount).append(']'); //$NON-NLS-1$
		buffer.append("[nullReferences count - ").append(this.nullCount).append(']'); //$NON-NLS-1$
		return buffer.toString();
	}

	@Override
	public boolean isSubRoutine() {
		return true;
	}

	@Override
	protected boolean recordFinalAssignment(
		VariableBinding binding,
		Reference finalAssignment) {
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
	public void recordUsingNullReference(Scope scope, LocalVariableBinding local,
			ASTNode location, int checkType, FlowInfo flowInfo) {
		if ((flowInfo.tagBits & FlowInfo.UNREACHABLE) == 0 && !flowInfo.isDefinitelyUnknown(local))	{
			// if reference is being recorded inside an assert, we will not raise redundant null check warnings
			checkType |= (this.tagBits & FlowContext.HIDE_NULL_COMPARISON_WARNING);
			int checkTypeWithoutHideNullWarning = checkType & ~FlowContext.HIDE_NULL_COMPARISON_WARNING_MASK;
			if ((this.tagBits & FlowContext.DEFER_NULL_DIAGNOSTIC) != 0) { // within an enclosing loop, be conservative
				switch (checkTypeWithoutHideNullWarning) {
					case CAN_ONLY_NULL_NON_NULL | IN_COMPARISON_NULL:
					case CAN_ONLY_NULL_NON_NULL | IN_COMPARISON_NON_NULL:
					case CAN_ONLY_NULL | IN_COMPARISON_NULL:
					case CAN_ONLY_NULL | IN_COMPARISON_NON_NULL:
					case CAN_ONLY_NULL | IN_ASSIGNMENT:
					case CAN_ONLY_NULL | IN_INSTANCEOF:
						Expression reference = (Expression) location;
						if (flowInfo.cannotBeNull(local)) {
							if (checkTypeWithoutHideNullWarning == (CAN_ONLY_NULL_NON_NULL | IN_COMPARISON_NON_NULL)) {
								if ((checkType & HIDE_NULL_COMPARISON_WARNING) == 0) {
									scope.problemReporter().localVariableRedundantCheckOnNonNull(local, reference);
								}
								flowInfo.initsWhenFalse().setReachMode(FlowInfo.UNREACHABLE_BY_NULLANALYSIS);
							} else if (checkTypeWithoutHideNullWarning == (CAN_ONLY_NULL_NON_NULL | IN_COMPARISON_NULL)) {
								scope.problemReporter().localVariableNonNullComparedToNull(local, reference);
								flowInfo.initsWhenTrue().setReachMode(FlowInfo.UNREACHABLE_BY_NULLANALYSIS);
							}
							return;
						}
						if (flowInfo.canOnlyBeNull(local)) {
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
						}
						break;
					case MAY_NULL :
						if (flowInfo.cannotBeNull(local)) {
							return;
						}
						if (flowInfo.canOnlyBeNull(local)) {
							scope.problemReporter().localVariableNullReference(local, location);
							return;
						}
						break;
					default:
						// never happens
				}
			}
			else { // no enclosing loop, be as precise as possible right now
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
						//$FALL-THROUGH$
					case CAN_ONLY_NULL | IN_COMPARISON_NULL:
					case CAN_ONLY_NULL | IN_COMPARISON_NON_NULL:
					case CAN_ONLY_NULL | IN_ASSIGNMENT:
					case CAN_ONLY_NULL | IN_INSTANCEOF:
						Expression reference = (Expression) location;
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
						}
						break;
					case MAY_NULL :
						if (flowInfo.isDefinitelyNull(local)) {
							scope.problemReporter().localVariableNullReference(local, location);
							return;
						}
						if (flowInfo.isPotentiallyNull(local)) {
							scope.problemReporter().localVariablePotentialNullReference(local, location);
							return;
						}
						if (flowInfo.isDefinitelyNonNull(local)) {
							return; // shortcut: cannot be null
						}
						break;
					default:
						// never happens
				}
			}
			recordNullReference(local, location, checkType, flowInfo);
			// prepare to re-check with try/catch flow info
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

@Override
protected void recordNullReferenceWithAnnotationStatus(LocalVariableBinding local,
	ASTNode expression, int checkType, FlowInfo nullInfo, NullAnnotationMatching nullAnnotationStatus) {
	if (this.nullCount == 0) {
		this.nullLocals = new LocalVariableBinding[5];
		this.nullReferences = new ASTNode[5];
		this.nullCheckTypes = new int[5];
		this.nullAnnotationStatuses = new NullAnnotationMatching[5];
	}
	else if (this.nullCount == this.nullLocals.length) {
		int newLength = this.nullCount * 2;
		System.arraycopy(this.nullLocals, 0,
			this.nullLocals = new LocalVariableBinding[newLength], 0,
			this.nullCount);
		System.arraycopy(this.nullReferences, 0,
			this.nullReferences = new ASTNode[newLength], 0,
			this.nullCount);
		System.arraycopy(this.nullCheckTypes, 0,
			this.nullCheckTypes = new int[newLength], 0,
			this.nullCount);
		System.arraycopy(this.nullAnnotationStatuses, 0,
			this.nullAnnotationStatuses = new NullAnnotationMatching[this.nullCount * 2], 0,
			this.nullCount);
	}
	this.nullLocals[this.nullCount] = local;
	this.nullReferences[this.nullCount] = expression;
	this.nullAnnotationStatuses[this.nullCount] = nullAnnotationStatus;
	this.nullCheckTypes[this.nullCount++] = checkType;
}
@Override
public void recordUnboxing(Scope scope, Expression expression, int nullStatus, FlowInfo flowInfo) {
	if (nullStatus == FlowInfo.NULL)
		super.recordUnboxing(scope, expression, nullStatus, flowInfo);
	else // defer checking:
		recordNullReference(null, expression, IN_UNBOXING, flowInfo);
}
@Override
protected boolean internalRecordNullityMismatch(Expression expression, TypeBinding providedType, FlowInfo flowInfo, int nullStatus, NullAnnotationMatching nullAnnotationStatus, TypeBinding expectedType, int checkType) {
	// cf. decision structure inside FinallyFlowContext.recordUsingNullReference(..)
	if (nullStatus == FlowInfo.UNKNOWN ||
			((this.tagBits & FlowContext.DEFER_NULL_DIAGNOSTIC) != 0 && nullStatus != FlowInfo.NULL)) {
		recordProvidedExpectedTypes(providedType, expectedType, this.nullCount);
		recordNullReferenceWithAnnotationStatus(expression.localVariableBinding(), expression, checkType, flowInfo, nullAnnotationStatus);
		return true;
	}
	return false;
}
}
