/*******************************************************************************
 * Copyright (c) 2011, 2020 GK Software AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *     Nikolay Metchev (nikolaymetchev@gmail.com) - Contributions for
 *								bug 411098 - [compiler][resource] Invalid Resource Leak Warning using ternary operator inside try-with-resource
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.flow.FinallyFlowContext;
import org.eclipse.jdt.internal.compiler.flow.FlowContext;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.compiler.util.Util;

/**
 * A faked local variable declaration used for keeping track of data flows of a
 * special variable. Certain events will be recorded by changing the null info
 * for this variable.
 *
 * See bug 349326 - [1.7] new warning for missing try-with-resources
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class FakedTrackingVariable extends LocalDeclaration {

	private static final char[] UNASSIGNED_CLOSEABLE_NAME = "<unassigned Closeable value>".toCharArray(); //$NON-NLS-1$
	private static final char[] UNASSIGNED_CLOSEABLE_NAME_TEMPLATE = "<unassigned Closeable value from line {0}>".toCharArray(); //$NON-NLS-1$
	private static final char[] TEMPLATE_ARGUMENT = "{0}".toCharArray(); //$NON-NLS-1$

	// a call to close() was seen at least on one path:
	private static final int CLOSE_SEEN = 1;
	// the resource is shared with outside code either by
	// - passing as an arg in a method call or
	// - obtaining this from a method call or array reference
	// Interpret that we may or may not be responsible for closing
	private static final int SHARED_WITH_OUTSIDE = 2;
	// the resource is likely owned by outside code (owner has responsibility to close):
	// - obtained as argument of the current method, or via a field read
	// - stored into a field
	// - returned as the result of this method
	private static final int OWNED_BY_OUTSIDE = 4;
	// If close() is invoked from a nested method (inside a local type) report remaining problems only as potential:
	private static final int CLOSED_IN_NESTED_METHOD = 8;
	// explicit closing has been reported already against this resource:
	private static final int REPORTED_EXPLICIT_CLOSE = 16;
	// a location independent potential problem has been reported against this resource:
	private static final int REPORTED_POTENTIAL_LEAK = 32;
	// a location independent definitive problem has been reported against this resource:
	private static final int REPORTED_DEFINITIVE_LEAK = 64;
	// a local declarations that acts as the element variable of a foreach loop (should never suggest to use t-w-r):
	private static final int FOREACH_ELEMENT_VAR = 128;
	// - passed as an effectively final resource in t-w-r JDK 9 and above
	private static final int TWR_EFFECTIVELY_FINAL = 256;
	public MessageSend acquisition;

	public static boolean TEST_372319 = false; // see https://bugs.eclipse.org/372319

	/**
	 * Bitset of {@link #CLOSE_SEEN}, {@link #SHARED_WITH_OUTSIDE}, {@link #OWNED_BY_OUTSIDE}, {@link #CLOSED_IN_NESTED_METHOD}, {@link #REPORTED_EXPLICIT_CLOSE}, {@link #REPORTED_POTENTIAL_LEAK} and {@link #REPORTED_DEFINITIVE_LEAK}.
	 */
	private int globalClosingState = 0;

	public LocalVariableBinding originalBinding; // the real local being tracked, can be null for preliminary track vars for allocation expressions

	public FakedTrackingVariable innerTracker; // chained tracking variable of a chained (wrapped) resource
	public FakedTrackingVariable outerTracker; // inverse of 'innerTracker'

	MethodScope methodScope; // designates the method declaring this variable

	private HashMap recordedLocations; // initially null, ASTNode -> Integer

	// temporary storage while analyzing "res = new Res();":
	private ASTNode currentAssignment; // temporarily store the assignment as the location for error reporting

	// if tracking var was allocated from a finally context, record here the flow context of the corresponding try block
	private FlowContext tryContext;

	public FakedTrackingVariable(LocalVariableBinding original, ASTNode location, FlowInfo flowInfo, FlowContext flowContext, int nullStatus) {
		super(original.name, location.sourceStart, location.sourceEnd);
		this.type = new SingleTypeReference(
				TypeConstants.OBJECT,
				((long)this.sourceStart <<32)+this.sourceEnd);
		this.methodScope = original.declaringScope.methodScope();
		this.originalBinding = original;
		// inside a finally block?
		while (flowContext != null) {
			if (flowContext instanceof FinallyFlowContext) {
				// yes -> connect to the corresponding try block:
				this.tryContext = ((FinallyFlowContext) flowContext).tryContext;
				break;
			}
			flowContext = flowContext.parent;
		}
		resolve(original.declaringScope);
		if (nullStatus != 0)
			flowInfo.markNullStatus(this.binding, nullStatus); // mark that this flow has seen the resource
	}

	/* Create an unassigned tracking variable while analyzing an allocation expression: */
	private FakedTrackingVariable(BlockScope scope, ASTNode location, FlowInfo flowInfo, int nullStatus) {
		super(UNASSIGNED_CLOSEABLE_NAME, location.sourceStart, location.sourceEnd);
		this.type = new SingleTypeReference(
				TypeConstants.OBJECT,
				((long)this.sourceStart <<32)+this.sourceEnd);
		this.methodScope = scope.methodScope();
		this.originalBinding = null;
		resolve(scope);
		if (nullStatus != 0)
			flowInfo.markNullStatus(this.binding, nullStatus); // mark that this flow has seen the resource
	}

	private void attachTo(LocalVariableBinding local) {
		local.closeTracker = this;
		this.originalBinding = local;
	}

	@Override
	public void generateCode(BlockScope currentScope, CodeStream codeStream)
	{ /* NOP - this variable is completely dummy, ie. for analysis only. */ }

	@Override
	public void resolve (BlockScope scope) {
		// only need the binding, which is used as reference in FlowInfo methods.
		this.binding = new LocalVariableBinding(
				this.name,
				scope.getJavaLangObject(),  // dummy, just needs to be a reference type
				0,
				false);
		this.binding.closeTracker = this;
		this.binding.declaringScope = scope;
		this.binding.setConstant(Constant.NotAConstant);
		this.binding.useFlag = LocalVariableBinding.USED;
		// use a free slot without assigning it:
		this.binding.id = scope.registerTrackingVariable(this);
	}

	/**
	 * If expression resolves to a value of type AutoCloseable answer the variable that tracks closing of that local.
	 * Covers two cases:
	 * <ul>
	 * <li>value is a local variable reference, create tracking variable it if needed.
	 * <li>value is an allocation expression, return a preliminary tracking variable if set.
	 * </ul>
	 * @param expression
	 * @return a new {@link FakedTrackingVariable} or null.
	 */
	public static FakedTrackingVariable getCloseTrackingVariable(Expression expression, FlowInfo flowInfo, FlowContext flowContext) {
		while (true) {
			if (expression instanceof CastExpression)
				expression = ((CastExpression) expression).expression;
			else if (expression instanceof Assignment)
				expression = ((Assignment) expression).expression;
			else if (expression instanceof ConditionalExpression) {
				FakedTrackingVariable falseTrackingVariable = getCloseTrackingVariable(((ConditionalExpression)expression).valueIfFalse, flowInfo, flowContext);
				if (falseTrackingVariable != null) {
					return falseTrackingVariable;
				}
				return getCloseTrackingVariable(((ConditionalExpression)expression).valueIfTrue, flowInfo, flowContext);
			} else if (expression instanceof SwitchExpression) {
				for (Expression re : ((SwitchExpression) expression).resultExpressions) {
					FakedTrackingVariable fakedTrackingVariable = getCloseTrackingVariable(re, flowInfo, flowContext);
					if (fakedTrackingVariable != null) {
						return fakedTrackingVariable;
					}
				}
				return null;
			}
			else
				break;
		}
		if (expression instanceof SingleNameReference) {
			SingleNameReference name = (SingleNameReference) expression;
			if (name.binding instanceof LocalVariableBinding) {
				LocalVariableBinding local = (LocalVariableBinding)name.binding;
				if (local.closeTracker != null)
					return local.closeTracker;
				if (!isAnyCloseable(expression.resolvedType))
					return null;
				if ((local.tagBits & TagBits.IsResource) != 0)
					return null;
				// tracking var doesn't yet exist. This happens in finally block
				// which is analyzed before the corresponding try block
				Statement location = local.declaration;
				local.closeTracker = new FakedTrackingVariable(local, location, flowInfo, flowContext, FlowInfo.UNKNOWN);
				if (local.isParameter()) {
					local.closeTracker.globalClosingState |= OWNED_BY_OUTSIDE;
					// status of this tracker is now UNKNOWN
				}
				return local.closeTracker;
			}
		} else if (expression instanceof AllocationExpression) {
			// return any preliminary tracking variable from analyseCloseableAllocation
			return ((AllocationExpression) expression).closeTracker;
		} else if (expression instanceof MessageSend) {
			// return any preliminary tracking variable from analyseCloseableAcquisition
			return ((MessageSend) expression).closeTracker;
		}
		return null;
	}

	/**
	 * Before analyzing an assignment of this shape: <code>singleName = new Allocation()</code>
	 * connect any tracking variable of the LHS with the allocation on the RHS.
	 * Also the assignment is temporarily stored in the tracking variable in case we need to
	 * report errors because the assignment leaves the old LHS value unclosed.
	 * In this case the assignment should be used as the error location.
	 *
	 * @param location the assignment/local declaration being analyzed
	 * @param local the local variable being assigned to
	 * @param rhs the rhs of the assignment resp. the initialization of the local variable declaration.
	 * 		<strong>Precondition:</strong> client has already checked that the resolved type of this expression is either a closeable type or NULL.
	 */
	public static FakedTrackingVariable preConnectTrackerAcrossAssignment(ASTNode location, LocalVariableBinding local, Expression rhs, FlowInfo flowInfo) {
		FakedTrackingVariable closeTracker = null;
		if (containsAllocation(rhs)) {
			closeTracker = local.closeTracker;
			if (closeTracker == null) {
				if (rhs.resolvedType != TypeBinding.NULL) { // not NULL means valid closeable as per method precondition
					closeTracker = new FakedTrackingVariable(local, location, flowInfo, null, FlowInfo.UNKNOWN);
					if (local.isParameter()) {
						closeTracker.globalClosingState |= OWNED_BY_OUTSIDE;
					}
				}
			}
			if (closeTracker != null) {
				closeTracker.currentAssignment = location;
				preConnectTrackerAcrossAssignment(location, local, flowInfo, closeTracker, rhs);
			}
		} else if (rhs instanceof MessageSend) {
			closeTracker = local.closeTracker;
			if (closeTracker != null) {
				handleReassignment(flowInfo, closeTracker, location);
			}
			if (rhs.resolvedType != TypeBinding.NULL) { // not NULL means valid closeable as per method precondition
				closeTracker = new FakedTrackingVariable(local, location, flowInfo, null, FlowInfo.UNKNOWN);
				closeTracker.currentAssignment = location;
				((MessageSend) rhs).closeTracker = closeTracker;
			}
		}
		return closeTracker;
	}

	private static boolean containsAllocation(SwitchExpression location) {
		for (Expression re : location.resultExpressions) {
			if (containsAllocation(re))
				return true;
		}
		return false;
	}
	private static boolean containsAllocation(ASTNode location) {
		if (location instanceof AllocationExpression) {
			return true;
		} else if (location instanceof ConditionalExpression) {
			ConditionalExpression conditional = (ConditionalExpression) location;
			return containsAllocation(conditional.valueIfTrue) || containsAllocation(conditional.valueIfFalse);
		} else if (location instanceof SwitchExpression) {
			return containsAllocation((SwitchExpression) location);
		} else if (location instanceof CastExpression) {
			return containsAllocation(((CastExpression) location).expression);
		} else if (location instanceof MessageSend) {
			if (isFluentMethod(((MessageSend) location).binding))
				return containsAllocation(((MessageSend) location).receiver);
		}
		return false;
	}

	private static void preConnectTrackerAcrossAssignment(ASTNode location, LocalVariableBinding local, FlowInfo flowInfo,
			FakedTrackingVariable closeTracker, Expression expression) {
		if (expression instanceof AllocationExpression) {
			preConnectTrackerAcrossAssignment(location, local, flowInfo, (AllocationExpression) expression, closeTracker);
		} else if (expression instanceof ConditionalExpression) {
			preConnectTrackerAcrossAssignment(location, local, flowInfo, (ConditionalExpression) expression, closeTracker);
		}  else if (expression instanceof SwitchExpression) {
			preConnectTrackerAcrossAssignment(location, local, flowInfo, (SwitchExpression) expression, closeTracker);
		} else if (expression instanceof CastExpression) {
			preConnectTrackerAcrossAssignment(location, local, ((CastExpression) expression).expression, flowInfo);
		} else if (expression instanceof MessageSend) {
			if (isFluentMethod(((MessageSend) expression).binding))
				preConnectTrackerAcrossAssignment(location, local, ((MessageSend) expression).receiver, flowInfo);
		}
	}

	private static void preConnectTrackerAcrossAssignment(ASTNode location, LocalVariableBinding local, FlowInfo flowInfo,
			ConditionalExpression conditional, FakedTrackingVariable closeTracker) {
		preConnectTrackerAcrossAssignment(location, local, flowInfo, closeTracker, conditional.valueIfFalse);
		preConnectTrackerAcrossAssignment(location, local, flowInfo, closeTracker, conditional.valueIfTrue);
	}

	private static void preConnectTrackerAcrossAssignment(ASTNode location, LocalVariableBinding local, FlowInfo flowInfo,
			SwitchExpression se, FakedTrackingVariable closeTracker) {
		for (Expression re : se.resultExpressions) {
			preConnectTrackerAcrossAssignment(location, local, flowInfo, closeTracker, re);
		}
	}

	private static void preConnectTrackerAcrossAssignment(ASTNode location, LocalVariableBinding local, FlowInfo flowInfo,
			AllocationExpression allocationExpression, FakedTrackingVariable closeTracker) {
		allocationExpression.closeTracker = closeTracker;
		if (allocationExpression.arguments != null && allocationExpression.arguments.length > 0) {
			// also push into nested allocations, see https://bugs.eclipse.org/368709
			FakedTrackingVariable inner = preConnectTrackerAcrossAssignment(location, local, allocationExpression.arguments[0], flowInfo);
			if (inner != closeTracker && closeTracker.innerTracker == null)
				closeTracker.innerTracker = inner;
		}
	}

	/**
	 * Compute/assign a tracking variable for a freshly allocated closeable value, using information from our white lists.
	 * See  Bug 358903 - Filter practically unimportant resource leak warnings
	 */
	public static void analyseCloseableAllocation(BlockScope scope, FlowInfo flowInfo, AllocationExpression allocation) {
		// client has checked that the resolvedType is an AutoCloseable, hence the following cast is safe:
		if (((ReferenceBinding)allocation.resolvedType).hasTypeBit(TypeIds.BitResourceFreeCloseable)) {
			// remove unnecessary attempts (closeable is not relevant)
			if (allocation.closeTracker != null) {
				allocation.closeTracker.withdraw();
				allocation.closeTracker = null;
			}
		} else if (((ReferenceBinding)allocation.resolvedType).hasTypeBit(TypeIds.BitWrapperCloseable)) {
			boolean isWrapper = true;
			if (allocation.arguments != null &&  allocation.arguments.length > 0) {
				// find the wrapped resource represented by its tracking var:
				FakedTrackingVariable innerTracker = findCloseTracker(scope, flowInfo, allocation.arguments[0]);
				if (innerTracker != null) {
					FakedTrackingVariable currentInner = innerTracker;
					do {
						if (currentInner == allocation.closeTracker)
							return; // self wrap (res = new Res(res)) -> neither change (here) nor remove (below)
						// also check for indirect cycles, see https://bugs.eclipse.org/368709
						currentInner = currentInner.innerTracker;
					} while (currentInner != null);
					int newStatus = FlowInfo.NULL;
					if (allocation.closeTracker == null) {
						allocation.closeTracker = new FakedTrackingVariable(scope, allocation, flowInfo, FlowInfo.UNKNOWN); // no local available, closeable is unassigned
					} else {
						if (scope.finallyInfo != null) {
							// inject results from analysing a finally block onto the newly connected wrapper
							int finallyStatus = scope.finallyInfo.nullStatus(allocation.closeTracker.binding);
							if (finallyStatus != FlowInfo.UNKNOWN)
								newStatus = finallyStatus;
						}
					}
					if (allocation.closeTracker.innerTracker != null && allocation.closeTracker.innerTracker != innerTracker) {
						innerTracker = pickMoreUnsafe(allocation.closeTracker.innerTracker, innerTracker, scope, flowInfo);
					}
					allocation.closeTracker.innerTracker = innerTracker;
					innerTracker.outerTracker = allocation.closeTracker;
					flowInfo.markNullStatus(allocation.closeTracker.binding, newStatus);
					if (newStatus != FlowInfo.NULL) {
						// propagate results from a finally block also into nested resources:
						FakedTrackingVariable currentTracker = innerTracker;
						while (currentTracker != null) {
							flowInfo.markNullStatus(currentTracker.binding, newStatus);
							currentTracker.globalClosingState |= allocation.closeTracker.globalClosingState;
							currentTracker = currentTracker.innerTracker;
						}
					}
					return; // keep chaining wrapper (by avoiding to fall through to removeTrackingVar below)
				} else {
					if (!isAnyCloseable(allocation.arguments[0].resolvedType)) {
						isWrapper = false; // argument is not closeable
					}
				}
			} else {
				isWrapper = false; // no argument
			}
			// successful wrapper detection has exited above, let's see why that failed
			if (isWrapper) {
				// remove unnecessary attempts (wrapper has no relevant inner)
				if (allocation.closeTracker != null) {
					allocation.closeTracker.withdraw();
					allocation.closeTracker = null;
				}
			} else {
				// allocation does not provide a resource as the first argument -> don't treat as a wrapper
				handleRegularResource(scope, flowInfo, allocation);
			}
		} else { // regular resource
			handleRegularResource(scope, flowInfo, allocation);
		}
	}

	/**
	 * Check if a message send acquires a closeable from its receiver, see:
	 * Bug 463320 - [compiler][resource] potential "resource leak" problem disappears when local variable inlined
	 */
	public static FlowInfo analyseCloseableAcquisition(BlockScope scope, FlowInfo flowInfo, MessageSend acquisition) {
		if (isFluentMethod(acquisition.binding)) {
			// share the existing close tracker of the receiver (if any):
			acquisition.closeTracker = findCloseTracker(scope, flowInfo, acquisition.receiver);
			return flowInfo;
		}
		// client has checked that the resolvedType is an AutoCloseable, hence the following cast is safe:
		if (((ReferenceBinding)acquisition.resolvedType).hasTypeBit(TypeIds.BitResourceFreeCloseable)) {
			// remove unnecessary attempts (closeable is not relevant)
			if (acquisition.closeTracker != null) {
				acquisition.closeTracker.withdraw();
				acquisition.closeTracker = null;
			}
			return flowInfo;
		} else { // regular resource
			FakedTrackingVariable tracker = acquisition.closeTracker;
			if (tracker != null) {
				// pre-connected tracker means: directly assigning the acquisition to a local, forget special treatment:
				tracker.withdraw();
				acquisition.closeTracker = null;
				return flowInfo;
			} else {
				tracker = new FakedTrackingVariable(scope, acquisition, flowInfo, FlowInfo.UNKNOWN); // no local available, closeable is unassigned
				acquisition.closeTracker = tracker;
			}
			tracker.acquisition = acquisition;
			FlowInfo outsideInfo = flowInfo.copy();
			outsideInfo.markAsDefinitelyNonNull(tracker.binding);
			flowInfo.markAsDefinitelyNull(tracker.binding);
			return FlowInfo.conditional(outsideInfo, flowInfo);
		}
	}

	private static boolean isFluentMethod(MethodBinding binding) {
		if (binding.isStatic())
			return false;
		ReferenceBinding declaringClass = binding.declaringClass;
		if (declaringClass.equals(binding.returnType)) {
			for (char[][] compoundName : TypeConstants.FLUENT_RESOURCE_CLASSES) {
				if (CharOperation.equals(compoundName, declaringClass.compoundName))
					return true;
			}
		}
		return false;
	}

	private static FakedTrackingVariable pickMoreUnsafe(FakedTrackingVariable tracker1, FakedTrackingVariable tracker2, BlockScope scope, FlowInfo info) {
		// whichever of the two trackers has stronger indication to be leaking will be returned,
		// the other one will be removed from the scope (considered to be merged into the former).
		int status1 = info.nullStatus(tracker1.binding);
		int status2 = info.nullStatus(tracker2.binding);
		if (status1 == FlowInfo.NULL || status2 == FlowInfo.NON_NULL) return pick(tracker1, tracker2, scope);
		if (status1 == FlowInfo.NON_NULL || status2 == FlowInfo.NULL) return pick(tracker2, tracker1, scope);
		if ((status1 & FlowInfo.POTENTIALLY_NULL) != 0) return pick(tracker1, tracker2, scope);
		if ((status2 & FlowInfo.POTENTIALLY_NULL) != 0) return pick(tracker2, tracker1, scope);
		return pick(tracker1, tracker2, scope);
	}

	private static FakedTrackingVariable pick(FakedTrackingVariable tracker1, FakedTrackingVariable tracker2, BlockScope scope) {
		tracker2.withdraw();
		return tracker1;
	}

	private static void handleRegularResource(BlockScope scope, FlowInfo flowInfo, AllocationExpression allocation) {
		FakedTrackingVariable presetTracker = allocation.closeTracker;
		if (presetTracker != null && presetTracker.originalBinding != null) {
			// the current assignment forgets a previous resource in the LHS, may cause a leak
			// report now because handleResourceAssignment can't distinguish this from a self-wrap situation
			handleReassignment(flowInfo, presetTracker, presetTracker.currentAssignment);
		} else {
			allocation.closeTracker = new FakedTrackingVariable(scope, allocation, flowInfo, FlowInfo.UNKNOWN); // no local available, closeable is unassigned
		}
		flowInfo.markAsDefinitelyNull(allocation.closeTracker.binding);
	}

	private static void handleReassignment(FlowInfo flowInfo, FakedTrackingVariable existingTracker, ASTNode location) {
		int closeStatus = flowInfo.nullStatus(existingTracker.binding);
		if (closeStatus != FlowInfo.NON_NULL		// old resource was not closed
				&& closeStatus != FlowInfo.UNKNOWN	// old resource had some flow information
				&& !flowInfo.isDefinitelyNull(existingTracker.originalBinding)		// old resource was not null
				&& !(location instanceof LocalDeclaration))	// forgetting old val in local decl is syntactically impossible
			existingTracker.recordErrorLocation(location, closeStatus);
	}

	/** Find an existing tracking variable for the argument of an allocation for a resource wrapper. */
	private static FakedTrackingVariable findCloseTracker(BlockScope scope, FlowInfo flowInfo, Expression arg)
	{
		while (arg instanceof Assignment) {
			Assignment assign = (Assignment)arg;
			LocalVariableBinding innerLocal = assign.localVariableBinding();
			if (innerLocal != null) {
				// nested assignment has already been processed
				return innerLocal.closeTracker;
			} else {
				arg = assign.expression; // unwrap assignment and fall through
			}
		}
		if (arg instanceof SingleNameReference) {
			// is allocation arg a reference to an existing closeable?
			LocalVariableBinding local = arg.localVariableBinding();
			if (local != null) {
				return local.closeTracker;
			}
		} else if (arg instanceof AllocationExpression) {
			// nested allocation
			return ((AllocationExpression)arg).closeTracker;
		} else if (arg instanceof MessageSend) {
			return ((MessageSend) arg).closeTracker;
		}
		return null; // not a tracked expression
	}

	/**
	 * Given the rhs of an assignment or local declaration has a (Auto)Closeable type (or null), setup for leak analysis now:
	 * Create or re-use a tracking variable, and wire and initialize everything.
	 * @param scope scope containing the assignment
	 * @param upstreamInfo info without analysis of the rhs, use this to determine the status of a resource being disconnected
	 * @param flowInfo info with analysis of the rhs, use this for recording resource status because this will be passed downstream
	 * @param flowContext
	 * @param location where to report warnigs/errors against
	 * @param rhs the right hand side of the assignment, this expression is to be analyzed.
	 *			The caller has already checked that the rhs is either of a closeable type or null.
	 * @param local the local variable into which the rhs is being assigned
	 */
	public static void handleResourceAssignment(BlockScope scope, FlowInfo upstreamInfo, FlowInfo flowInfo, FlowContext flowContext, ASTNode location, Expression rhs, LocalVariableBinding local)
	{
		// does the LHS (local) already have a tracker, indicating we may leak a resource by the assignment?
		FakedTrackingVariable previousTracker = null;
		FakedTrackingVariable disconnectedTracker = null;
		if (local.closeTracker != null) {
			// assigning to a variable already holding an AutoCloseable, has it been closed before?
			previousTracker = local.closeTracker;
			int nullStatus = upstreamInfo.nullStatus(local);
			if (nullStatus != FlowInfo.NULL && nullStatus != FlowInfo.UNKNOWN) // only if previous value may be relevant
				disconnectedTracker = previousTracker; // report error below, unless we have a self-wrap assignment
		}

		rhsAnalyis:
		if (rhs.resolvedType != TypeBinding.NULL) {
			// new value is AutoCloseable, start tracking, possibly re-using existing tracker var:
			FakedTrackingVariable rhsTrackVar = getCloseTrackingVariable(rhs, flowInfo, flowContext);
			if (rhsTrackVar != null) {								// 1. if RHS has a tracking variable...
				if (local.closeTracker == null) {
					// null shouldn't occur but let's play safe:
					if (rhsTrackVar.originalBinding != null)
						local.closeTracker = rhsTrackVar;			//		a.: let fresh LHS share it
					if (rhsTrackVar.currentAssignment == location) {
						// pre-set tracker from lhs - passed from outside (or foreach)?
						// now it's a fresh resource
						rhsTrackVar.globalClosingState &= ~(SHARED_WITH_OUTSIDE|OWNED_BY_OUTSIDE|FOREACH_ELEMENT_VAR);
					}
				} else {
					if (rhs instanceof AllocationExpression || rhs instanceof ConditionalExpression || rhs instanceof SwitchExpression || rhs instanceof MessageSend) {
						if (rhsTrackVar == disconnectedTracker)
							return;									// 		b.: self wrapper: res = new Wrap(res); -> done!
						if (local.closeTracker == rhsTrackVar
								&& ((rhsTrackVar.globalClosingState & OWNED_BY_OUTSIDE) != 0)) {
																	// 		c.: assigning a fresh resource (pre-connected alloc)
																	//			to a local previously holding an alien resource -> start over
							local.closeTracker = new FakedTrackingVariable(local, location, flowInfo, flowContext, FlowInfo.NULL);
							// still check disconnectedTracker below
							break rhsAnalyis;
						}
					}
					rhsTrackVar.attachTo(local);					//		d.: conflicting LHS and RHS, proceed with recordErrorLocation below
				}
				// keep close-status of RHS unchanged across this assignment
			} else if (previousTracker != null) {					// 2. re-use tracking variable from the LHS?
				FlowContext currentFlowContext = flowContext;
				checkReuseTracker : {
					if (previousTracker.tryContext != null) {
						while (currentFlowContext != null) {
							if (previousTracker.tryContext == currentFlowContext) {
								// "previous" location was the finally block of the current try statement.
								// -> This is not a re-assignment.
								// see https://bugs.eclipse.org/388996
								break checkReuseTracker;
							}
							currentFlowContext = currentFlowContext.parent;
						}
					}
					// re-assigning from a fresh value, mark as not-closed again:
					if ((previousTracker.globalClosingState & (SHARED_WITH_OUTSIDE|OWNED_BY_OUTSIDE|FOREACH_ELEMENT_VAR)) == 0
							&& flowInfo.hasNullInfoFor(previousTracker.binding)) // avoid spilling info into a branch that doesn't see the corresponding resource
						flowInfo.markAsDefinitelyNull(previousTracker.binding);
					local.closeTracker = analyseCloseableExpression(flowInfo, flowContext, local, location, rhs, previousTracker);
				}
			} else {												// 3. no re-use, create a fresh tracking variable:
				rhsTrackVar = analyseCloseableExpression(flowInfo, flowContext, local, location, rhs, null);
				if (rhsTrackVar != null) {
					rhsTrackVar.attachTo(local);
					// a fresh resource, mark as not-closed:
					if ((rhsTrackVar.globalClosingState & (SHARED_WITH_OUTSIDE|OWNED_BY_OUTSIDE|FOREACH_ELEMENT_VAR)) == 0)
						flowInfo.markAsDefinitelyNull(rhsTrackVar.binding);
// TODO(stephan): this might be useful, but I could not find a test case for it:
//					if (flowContext.initsOnFinally != null)
//						flowContext.initsOnFinally.markAsDefinitelyNonNull(trackerBinding);
				}
			}
		}

		if (disconnectedTracker != null) {
			if (disconnectedTracker.innerTracker != null && disconnectedTracker.innerTracker.binding.declaringScope == scope) {
				// discard tracker for the wrapper but keep the inner:
				disconnectedTracker.innerTracker.outerTracker = null;
				scope.pruneWrapperTrackingVar(disconnectedTracker);
			} else {
				int upstreamStatus = upstreamInfo.nullStatus(disconnectedTracker.binding);
				if (upstreamStatus != FlowInfo.NON_NULL)
					disconnectedTracker.recordErrorLocation(location, upstreamStatus);
			}
		}
	}
	/**
	 * Analyze structure of a closeable expression, matching (chained) resources against our white lists.
	 * @param flowInfo where to record close status
	 * @param local local variable to which the closeable is being assigned
	 * @param location where to flag errors/warnings against
	 * @param expression expression to be analyzed
	 * @param previousTracker when analyzing a re-assignment we may already have a tracking variable for local,
	 *  		which we should then re-use
	 * @return a tracking variable associated with local or null if no need to track
	 */
	private static FakedTrackingVariable analyseCloseableExpression(FlowInfo flowInfo, FlowContext flowContext, LocalVariableBinding local,
									ASTNode location, Expression expression, FakedTrackingVariable previousTracker)
	{
		// unwrap uninteresting nodes:
		while (true) {
			if (expression instanceof Assignment)
				expression = ((Assignment)expression).expression;
			else if (expression instanceof CastExpression)
				expression = ((CastExpression) expression).expression;
			else
				break;
		}

		boolean isResourceProducer = false;
		if (expression.resolvedType instanceof ReferenceBinding) {
			ReferenceBinding resourceType = (ReferenceBinding) expression.resolvedType;
			if (resourceType.hasTypeBit(TypeIds.BitResourceFreeCloseable)) {
				if (isBlacklistedMethod(expression))
					isResourceProducer = true;
				else
					return null; // (a) resource-free closeable: -> null
			}
		}

		// analyze by node type:
		if (expression instanceof AllocationExpression) {
			// allocation expressions already have their tracking variables analyzed by analyseCloseableAllocation(..)
			FakedTrackingVariable tracker = ((AllocationExpression) expression).closeTracker;
			if (tracker != null && tracker.originalBinding == null) {
				// tracker without original binding (unassigned closeable) shouldn't reach here but let's play safe
				return null;
			}
			return tracker;
		} else if (expression instanceof MessageSend
				|| expression instanceof ArrayReference)
		{
			// we *might* be responsible for the resource obtained
			FakedTrackingVariable tracker = new FakedTrackingVariable(local, location, flowInfo, flowContext, FlowInfo.POTENTIALLY_NULL); // shed some doubt
			if (!isResourceProducer)
				tracker.globalClosingState |= SHARED_WITH_OUTSIDE;
			return tracker;
		} else if (
				(expression.bits & RestrictiveFlagMASK) == Binding.FIELD
				||((expression instanceof QualifiedNameReference)
						&& ((QualifiedNameReference) expression).isFieldAccess()))
		{
			// responsibility for this resource probably lies at a higher level
			FakedTrackingVariable tracker = new FakedTrackingVariable(local, location, flowInfo, flowContext, FlowInfo.UNKNOWN);
			tracker.globalClosingState |= OWNED_BY_OUTSIDE;
			// leave state as UNKNOWN, the bit OWNED_BY_OUTSIDE will prevent spurious warnings
			return tracker;
		}

		if (local.closeTracker != null)
			// (c): inner has already been analyzed: -> re-use track var
			return local.closeTracker;
		FakedTrackingVariable newTracker = new FakedTrackingVariable(local, location, flowInfo, flowContext, FlowInfo.UNKNOWN);
		LocalVariableBinding rhsLocal = expression.localVariableBinding();
		if (rhsLocal != null && rhsLocal.isParameter()) {
			newTracker.globalClosingState |= OWNED_BY_OUTSIDE;
		}
		return newTracker;
	}

	private static boolean isBlacklistedMethod(Expression expression) {
		if (expression instanceof MessageSend) {
			MethodBinding method = ((MessageSend) expression).binding;
			if (method != null && method.isValidBinding())
				// for all methods in java.nio.file.Files that return a resource (Stream) it really needs closing
				return CharOperation.equals(method.declaringClass.compoundName, TypeConstants.JAVA_NIO_FILE_FILES);
		}
		return false;
	}

	public static void cleanUpAfterAssignment(BlockScope currentScope, int lhsBits, Expression expression) {
		// remove all remaining track vars with no original binding

		// unwrap uninteresting nodes:
		while (true) {
			if (expression instanceof Assignment)
				expression = ((Assignment)expression).expression;
			else if (expression instanceof CastExpression)
				expression = ((CastExpression) expression).expression;
			else
				break;
		}
		if (expression instanceof AllocationExpression) {
			FakedTrackingVariable tracker = ((AllocationExpression) expression).closeTracker;
			if (tracker != null && tracker.originalBinding == null) {
				tracker.withdraw();
				((AllocationExpression) expression).closeTracker = null;
			}
		} else if (expression instanceof MessageSend) {
			FakedTrackingVariable tracker = ((MessageSend) expression).closeTracker;
			if (tracker != null && tracker.originalBinding == null) {
				tracker.withdraw();
				((MessageSend) expression).closeTracker = null;
			}
		} else {
			// assignment passing a local into a field?
			LocalVariableBinding local = expression.localVariableBinding();
			if (local != null && local.closeTracker != null && ((lhsBits & Binding.FIELD) != 0))
				local.closeTracker.withdraw(); // TODO: may want to use local.closeTracker.markPassedToOutside(..,true)
		}
	}

	/** Unassigned closeables are not visible beyond their enclosing statement, immediately report & remove after each statement. */
	public static void cleanUpUnassigned(BlockScope scope, ASTNode location, FlowInfo flowInfo) {
		if (!scope.hasResourceTrackers()) return;
		location.traverse(new ASTVisitor() {
				@Override
				public boolean visit(MessageSend messageSend, BlockScope skope) {
					FakedTrackingVariable closeTracker = messageSend.closeTracker;
					if (closeTracker != null) {
						if (closeTracker.originalBinding == null) {
							int nullStatus = flowInfo.nullStatus(closeTracker.binding);
							if ((nullStatus & (FlowInfo.POTENTIALLY_NULL | FlowInfo.NULL)) != 0) {
								closeTracker.reportError(skope.problemReporter(), messageSend, nullStatus);
							}
							closeTracker.withdraw();
						}
					}
					return true;
				}
			},
			scope);
	}

	/** Answer wither the given type binding is a subtype of java.lang.AutoCloseable. */
	public static boolean isAnyCloseable(TypeBinding typeBinding) {
		return typeBinding instanceof ReferenceBinding
			&& ((ReferenceBinding)typeBinding).hasTypeBit(TypeIds.BitAutoCloseable|TypeIds.BitCloseable);
	}

	public int findMostSpecificStatus(FlowInfo flowInfo, BlockScope currentScope, BlockScope locationScope) {
		int status = FlowInfo.UNKNOWN;
		FakedTrackingVariable currentTracker = this;
		// loop as to consider wrappers (per white list) encapsulating an inner resource.
		while (currentTracker != null) {
			LocalVariableBinding currentVar = currentTracker.binding;
			int currentStatus = getNullStatusAggressively(currentVar, flowInfo);
			if (locationScope != null) // only check at method exit points
				currentStatus = mergeCloseStatus(locationScope, currentStatus, currentVar, currentScope);
			if (currentStatus == FlowInfo.NON_NULL) {
				status = currentStatus;
				break; // closed -> stop searching
			} else if (status == FlowInfo.NULL || status == FlowInfo.UNKNOWN) {
				status = currentStatus; // improved although not yet safe -> keep searching for better
			}
			currentTracker = currentTracker.innerTracker;
		}
		return status;
	}

	/**
	 * Get the null status looking even into unreachable flows
	 * @param local
	 * @param flowInfo
	 * @return one of the constants FlowInfo.{NULL,POTENTIALLY_NULL,POTENTIALLY_NON_NULL,NON_NULL}.
	 */
	private int getNullStatusAggressively(LocalVariableBinding local, FlowInfo flowInfo) {
		if (flowInfo == FlowInfo.DEAD_END) {
			return FlowInfo.UNKNOWN;
		}
		int reachMode = flowInfo.reachMode();
		int status = 0;
		try {
			// unreachable flowInfo is too shy in reporting null-issues, temporarily forget reachability:
			if (reachMode != FlowInfo.REACHABLE)
				flowInfo.tagBits &= ~FlowInfo.UNREACHABLE;
			status = flowInfo.nullStatus(local);
			if (TEST_372319) { // see https://bugs.eclipse.org/372319
				try {
					Thread.sleep(5); // increase probability of concurrency bug
				} catch (InterruptedException e) { /* nop */ }
			}
		} finally {
			// reset
			flowInfo.tagBits |= reachMode;
		}
		// at this point some combinations are not useful so flatten to a single bit:
		if ((status & FlowInfo.NULL) != 0) {
			if ((status & (FlowInfo.NON_NULL | FlowInfo.POTENTIALLY_NON_NULL)) != 0)
				return FlowInfo.POTENTIALLY_NULL; 	// null + doubt = pot null
			return FlowInfo.NULL;
		} else if ((status & FlowInfo.NON_NULL) != 0) {
			if ((status & FlowInfo.POTENTIALLY_NULL) != 0)
				return FlowInfo.POTENTIALLY_NULL;	// non-null + doubt = pot null
			return FlowInfo.NON_NULL;
		} else if ((status & FlowInfo.POTENTIALLY_NULL) != 0) {
			return FlowInfo.POTENTIALLY_NULL;
		} else if (status == FlowInfo.UNKNOWN) {
			// if unassigned resource (not having an originalBinding) is not withdrawn it is unclosed:
			if (this.originalBinding == null)
				return FlowInfo.NULL;
		}
		return status;
	}

	public int mergeCloseStatus(BlockScope currentScope, int status, LocalVariableBinding local, BlockScope outerScope) {
		// get the most suitable null status representing whether resource 'binding' has been closed
		// start at 'currentScope' and potentially travel out until 'outerScope'
		// at each scope consult any recorded 'finallyInfo'.
		if (status != FlowInfo.NON_NULL) {
			if (currentScope.finallyInfo != null) {
				int finallyStatus = currentScope.finallyInfo.nullStatus(local);
				if (finallyStatus == FlowInfo.NON_NULL)
					return finallyStatus;
				if (finallyStatus != FlowInfo.NULL && currentScope.finallyInfo.hasNullInfoFor(local)) // neither is NON_NULL, but not both are NULL => call it POTENTIALLY_NULL
					status = FlowInfo.POTENTIALLY_NULL;
			}
			if (currentScope != outerScope && currentScope.parent instanceof BlockScope)
				return mergeCloseStatus(((BlockScope) currentScope.parent), status, local, outerScope);
		}
		return status;
	}

	/** Mark that this resource is closed locally. */
	public void markClose(FlowInfo flowInfo, FlowContext flowContext) {
		FakedTrackingVariable current = this;
		do {
			flowInfo.markAsDefinitelyNonNull(current.binding);
			current.globalClosingState |= CLOSE_SEEN;
			flowContext.markFinallyNullStatus(current.binding, FlowInfo.NON_NULL);
			current = current.innerTracker;
		} while (current != null);
	}

	/** Mark that this resource is closed from a nested method (inside a local class). */
	public void markClosedInNestedMethod() {
		this.globalClosingState |= CLOSED_IN_NESTED_METHOD;
	}

	/** Mark that this resource is closed from a try-with-resource with the tracking variable being effectively final). */
	public void markClosedEffectivelyFinal() {
		this.globalClosingState |= TWR_EFFECTIVELY_FINAL;
	}
	/**
	 * Mark that this resource is passed to some outside code
	 * (as argument to a method/ctor call or as a return value from the current method),
	 * and thus should be considered as potentially closed.
	 * @param owned should the resource be considered owned by some outside?
	 */
	public static FlowInfo markPassedToOutside(BlockScope scope, Expression expression, FlowInfo flowInfo, FlowContext flowContext, boolean owned) {

		FakedTrackingVariable trackVar = getCloseTrackingVariable(expression, flowInfo, flowContext);
		if (trackVar != null) {
			// insert info that the tracked resource *may* be closed (by the target method, i.e.)
			FlowInfo infoResourceIsClosed = owned ? flowInfo : flowInfo.copy();
			int flag = owned ? OWNED_BY_OUTSIDE : SHARED_WITH_OUTSIDE;
			do {
				trackVar.globalClosingState |= flag;
				if (scope.methodScope() != trackVar.methodScope)
					trackVar.globalClosingState |= CLOSED_IN_NESTED_METHOD;
				infoResourceIsClosed.markAsDefinitelyNonNull(trackVar.binding);
			} while ((trackVar = trackVar.innerTracker) != null);
			if (owned) {
				return infoResourceIsClosed; // don't let downstream signal any problems on this flow
			} else {
				return FlowInfo.conditional(flowInfo, infoResourceIsClosed).unconditionalCopy(); // only report potential problems on this flow
			}
		}
		return flowInfo;
	}

	public static void markForeachElementVar(LocalDeclaration local) {
		if (local.binding != null && local.binding.closeTracker != null) {
			local.binding.closeTracker.globalClosingState |= FOREACH_ELEMENT_VAR;
		}
	}

	/**
	 * Iterator for a set of FakedTrackingVariable, which dispenses the elements
	 * according to the priorities defined by enum {@link Stage}.
	 * Resources whose outer is owned by an enclosing scope are never answered,
	 * unless we are analysing on behalf of an exit (return/throw).
	 */
	public static class IteratorForReporting implements Iterator<FakedTrackingVariable> {

		private final Set<FakedTrackingVariable> varSet;
		private final Scope scope;
		private final boolean atExit;

		private Stage stage;
		private Iterator<FakedTrackingVariable> iterator;
		private FakedTrackingVariable next;

		enum Stage {
			/** 1. prio: all top-level resources, ie., resources with no outer. */
			OuterLess,
			/** 2. prio: resources whose outer has already been processed (element of the same varSet). */
			InnerOfProcessed,
			/** 3. prio: resources whose outer is not owned by any enclosing scope. */
			InnerOfNotEnclosing,
			/** 4. prio: when analysing on behalf of an exit point: anything not picked before. */
			AtExit
		}

		public IteratorForReporting(List<FakedTrackingVariable> variables, Scope scope, boolean atExit) {
			this.varSet = new HashSet<>(variables);
			this.scope = scope;
			this.atExit = atExit;
			setUpForStage(Stage.OuterLess);
		}
		@Override
		public boolean hasNext() {
			FakedTrackingVariable trackingVar;
			switch (this.stage) {
				case OuterLess:
					while (this.iterator.hasNext()) {
						trackingVar = this.iterator.next();
						if (trackingVar.outerTracker == null)
							return found(trackingVar);
					}
					setUpForStage(Stage.InnerOfProcessed);
					//$FALL-THROUGH$
				case InnerOfProcessed:
					while (this.iterator.hasNext()) {
						trackingVar = this.iterator.next();
						FakedTrackingVariable outer = trackingVar.outerTracker;
						if (outer.binding.declaringScope == this.scope && !this.varSet.contains(outer))
							return found(trackingVar);
					}
					setUpForStage(Stage.InnerOfNotEnclosing);
					//$FALL-THROUGH$
				case InnerOfNotEnclosing:
					searchAlien: while (this.iterator.hasNext()) {
						trackingVar = this.iterator.next();
						FakedTrackingVariable outer = trackingVar.outerTracker;
						if (!this.varSet.contains(outer)) {
							Scope outerTrackerScope = outer.binding.declaringScope;
							Scope currentScope = this.scope;
							while ((currentScope = currentScope.parent) instanceof BlockScope) {
								if (outerTrackerScope == currentScope)
									break searchAlien;
							}
							return found(trackingVar);
						}
					}
					setUpForStage(Stage.AtExit);
					//$FALL-THROUGH$
				case AtExit:
					if (this.atExit && this.iterator.hasNext())
						return found(this.iterator.next());
					return false;
				default: throw new IllegalStateException("Unexpected Stage "+this.stage); //$NON-NLS-1$
			}
		}
		private boolean found(FakedTrackingVariable trackingVar) {
			this.iterator.remove();
			this.next = trackingVar;
			return true;
		}
		private void setUpForStage(Stage nextStage) {
			this.iterator = this.varSet.iterator();
			this.stage = nextStage;
		}
		@Override
		public FakedTrackingVariable next() {
			return this.next;
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Answer true if we know for sure that no resource is bound to this variable
	 * at the point of 'flowInfo'.
	 */
	public boolean hasDefinitelyNoResource(FlowInfo flowInfo) {
		if (this.originalBinding == null) return false; // shouldn't happen but keep quiet.
		if (flowInfo.isDefinitelyNull(this.originalBinding)) {
			return true;
		}
		if (!(flowInfo.isDefinitelyAssigned(this.originalBinding)
				|| flowInfo.isPotentiallyAssigned(this.originalBinding))) {
			return true;
		}
		return false;
	}

	public boolean isClosedInFinallyOfEnclosing(BlockScope scope) {
		BlockScope currentScope = scope;
		while (true) {
			if (currentScope.finallyInfo != null
					&& currentScope.finallyInfo.isDefinitelyNonNull(this.binding)) {
				return true; // closed in enclosing finally
			}
			if (!(currentScope.parent instanceof BlockScope)) {
				return false;
			}
			currentScope = (BlockScope) currentScope.parent;
		}
	}
	/**
	 * If current is the same as 'returnedResource' or a wrapper thereof,
	 * mark as reported and return true, otherwise false.
	 */
	public boolean isResourceBeingReturned(FakedTrackingVariable returnedResource) {
		FakedTrackingVariable current = this;
		do {
			if (current == returnedResource) {
				this.globalClosingState |= REPORTED_DEFINITIVE_LEAK;
				return true;
			}
			current = current.innerTracker;
		} while (current != null);
		return false;
	}

	public void withdraw() {
		// must unregister at the declaringScope, note that twr resources are owned by the scope enclosing the twr
		this.binding.declaringScope.removeTrackingVar(this);
	}

	public void recordErrorLocation(ASTNode location, int nullStatus) {
		if ((this.globalClosingState & OWNED_BY_OUTSIDE) != 0) {
			return;
		}
		if (this.recordedLocations == null)
			this.recordedLocations = new HashMap();
		this.recordedLocations.put(location, Integer.valueOf(nullStatus));
	}

	public boolean reportRecordedErrors(Scope scope, int mergedStatus, boolean atDeadEnd) {
		FakedTrackingVariable current = this;
		while (current.globalClosingState == 0) {
			current = current.innerTracker;
			if (current == null) {
				// no relevant state found -> report:
				if (atDeadEnd && neverClosedAtLocations())
					mergedStatus = FlowInfo.NULL;
				if ((mergedStatus & (FlowInfo.NULL|FlowInfo.POTENTIALLY_NULL|FlowInfo.POTENTIALLY_NON_NULL)) != 0) {
					reportError(scope.problemReporter(), null, mergedStatus);
					return true;
				} else {
					break;
				}
			}
		}
		boolean hasReported = false;
		if (this.recordedLocations != null) {
			Iterator locations = this.recordedLocations.entrySet().iterator();
			int reportFlags = 0;
			while (locations.hasNext()) {
				Map.Entry entry = (Entry) locations.next();
				reportFlags |= reportError(scope.problemReporter(), (ASTNode)entry.getKey(), ((Integer)entry.getValue()).intValue());
				hasReported = true;
			}
			if (reportFlags != 0) {
				// after all locations have been reported, mark as reported to prevent duplicate report via an outer wrapper
				current = this;
				do {
					current.globalClosingState |= reportFlags;
				} while ((current = current.innerTracker) != null);
			}
		}
		return hasReported;
	}

	private boolean neverClosedAtLocations() {
		if (this.recordedLocations != null) {
			for (Object value : this.recordedLocations.values())
				if (!value.equals(FlowInfo.NULL))
					return false;
		}
		return true;
	}

	public int reportError(ProblemReporter problemReporter, ASTNode location, int nullStatus) {
		if ((this.globalClosingState & OWNED_BY_OUTSIDE) != 0) {
			return 0; // TODO: should we still propagate some flags??
		}
		// which degree of problem?
		boolean isPotentialProblem = false;
		if (nullStatus == FlowInfo.NULL) {
			if ((this.globalClosingState & CLOSED_IN_NESTED_METHOD) != 0)
				isPotentialProblem = true;
		} else if ((nullStatus & (FlowInfo.POTENTIALLY_NULL|FlowInfo.POTENTIALLY_NON_NULL)) != 0) {
			isPotentialProblem = true;
		}
		// report:
		if (isPotentialProblem) {
			if ((this.globalClosingState & (REPORTED_POTENTIAL_LEAK|REPORTED_DEFINITIVE_LEAK)) != 0)
				return 0;
//			if ((this.globalClosingState & (ACQUIRED_FROM_OUTSIDE)) != 0
//					&& location instanceof ReturnStatement
//					&& ((ReturnStatement) location).expression == this.acquisition)
//				return 0; // directly returning a resource acquired from a message send: don't assume responsibility
			problemReporter.potentiallyUnclosedCloseable(this, location);
		} else {
			if ((this.globalClosingState & (REPORTED_DEFINITIVE_LEAK)) != 0)
				return 0;
			problemReporter.unclosedCloseable(this, location);
		}
		// propagate flag to inners:
		int reportFlag = isPotentialProblem ? REPORTED_POTENTIAL_LEAK : REPORTED_DEFINITIVE_LEAK;
		if (location == null) { // if location != null flags will be set after the loop over locations
			FakedTrackingVariable current = this;
			do {
				current.globalClosingState |= reportFlag;
			} while ((current = current.innerTracker) != null);
		}
		return reportFlag;
	}

	public void reportExplicitClosing(ProblemReporter problemReporter) {
		if ((this.globalClosingState & (TWR_EFFECTIVELY_FINAL|OWNED_BY_OUTSIDE|REPORTED_EXPLICIT_CLOSE|FOREACH_ELEMENT_VAR)) == 0) { // can't use t-w-r for OWNED_BY_OUTSIDE
			this.globalClosingState |= REPORTED_EXPLICIT_CLOSE;
			problemReporter.explicitlyClosedAutoCloseable(this);
		}
	}

	public String nameForReporting(ASTNode location, ReferenceContext referenceContext) {
		if (this.name == UNASSIGNED_CLOSEABLE_NAME) {
			if (location != null && referenceContext != null) {
				CompilationResult compResult = referenceContext.compilationResult();
				if (compResult != null) {
					int[] lineEnds = compResult.getLineSeparatorPositions();
					int resourceLine = Util.getLineNumber(this.sourceStart, lineEnds , 0, lineEnds.length-1);
					int reportLine = Util.getLineNumber(location.sourceStart, lineEnds , 0, lineEnds.length-1);
					if (resourceLine != reportLine) {
						char[] replacement = Integer.toString(resourceLine).toCharArray();
						return String.valueOf(CharOperation.replace(UNASSIGNED_CLOSEABLE_NAME_TEMPLATE, TEMPLATE_ARGUMENT, replacement));
					}
				}
			}
		}
		return String.valueOf(this.name);
	}
}
