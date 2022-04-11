/*******************************************************************************
 * Copyright (c) 2013, 2019 GK Software AG, and others.
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
 *     IBM Corporation - Bug fixes
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FunctionalExpression;
import org.eclipse.jdt.internal.compiler.ast.Invocation;
import org.eclipse.jdt.internal.compiler.ast.LambdaExpression;
import org.eclipse.jdt.internal.compiler.ast.ReferenceExpression;
import org.eclipse.jdt.internal.compiler.ast.SwitchExpression;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants.BoundCheckStatus;
import org.eclipse.jdt.internal.compiler.util.Sorting;

/**
 * Main class for new type inference as per JLS8 sect 18.
 * Keeps contextual state and drives the algorithm.
 *
 * <h2>Inference Basics</h2>
 * <ul>
 * <li>18.1.1 Inference variables: {@link InferenceVariable}</li>
 * <li>18.1.2 Constraint Formulas: subclasses of {@link ConstraintFormula}</li>
 * <li>18.1.3 Bounds: {@link TypeBound}<br/>
 * 	Capture bounds are directly captured in {@link BoundSet#captures}, throws-bounds in {@link BoundSet#inThrows}.<br/>
 * 	Also: {@link BoundSet}: main state during inference.</li>
 * </ul>
 * Each instance of {@link InferenceContext18} manages instances of the above and coordinates the inference process.
 * <h3>Queries and utilities</h3>
 * <ul>
 * <li>{@link TypeBinding#isProperType(boolean)}:
 * 	 used to exclude "types" that mention inference variables (18.1.1).</li>
 * <li>{@link TypeBinding#mentionsAny(TypeBinding[], int)}:
 * 	 does the receiver type binding mention any of the given types?</li>
 * <li>{@link TypeBinding#substituteInferenceVariable(InferenceVariable, TypeBinding)}:
 * 	 replace occurrences of an inference variable with a proper type.</li>
 * <li>{@link TypeBinding#collectInferenceVariables(Set)}:
 * 	 collect all inference variables mentioned in the receiver type into the given set.</li>
 * <li>{@link TypeVariableBinding#getTypeBounds(InferenceVariable, InferenceSubstitution)}:
 * 	Compute the initial type bounds for one inference variable as per JLS8 sect 18.1.3.</li>
 * </ul>
 * <h2>Phases of Inference</h2>
 * <ul>
 * <li>18.2 <b>Reduction</b>: {@link #reduce()} with most work happening in implementations of
 *  {@link ConstraintFormula#reduce(InferenceContext18)}:
 *  <ul>
 *  <li>18.2.1 Expression Compatibility Constraints: {@link ConstraintExpressionFormula#reduce(InferenceContext18)}.</li>
 *  <li>18.2.2 Type Compatibility Constraints ff. {@link ConstraintTypeFormula#reduce(InferenceContext18)}.</li>
 *  </ul></li>
 * <li>18.3 <b>Incorporation</b>: {@link BoundSet#incorporate(InferenceContext18)}; during inference new constraints
 * 	are accepted via {@link BoundSet#reduceOneConstraint(InferenceContext18, ConstraintFormula)} (combining 18.2 & 18.3)</li>
 * <li>18.4 <b>Resolution</b>: {@link #resolve(InferenceVariable[])}.
 * </ul>
 * Some of the above operations accumulate their results into {@link #currentBounds}, whereas
 * the last phase <em>returns</em> the resulting bound set while keeping the previous state in {@link #currentBounds}.
 * <h2>18.5. Uses of Inference</h2>
 * These are the main entries from the compiler into the inference engine:
 * <dl>
 * <dt>18.5.1 Invocation Applicability Inference</dt>
 * <dd>{@link #inferInvocationApplicability(MethodBinding, TypeBinding[], boolean)}. Prepare the initial state for
 * 	inference of a generic invocation - no target type used at this point.
 *  Need to call {@link #solve(boolean)} with true afterwards to produce the intermediate result.<br/>
 *  Called indirectly from {@link Scope#findMethod(ReferenceBinding, char[], TypeBinding[], InvocationSite, boolean)} et al
 *  to select applicable methods into overload resolution.</dd>
 * <dt>18.5.2 Invocation Type Inference</dt>
 * <dd>{@link InferenceContext18#inferInvocationType(TypeBinding, InvocationSite, MethodBinding)}. After a
 * 	most specific method has been picked, and given a target type determine the final generic instantiation.
 *  As long as a target type is still unavailable this phase keeps getting deferred.</br>
 *  Different wrappers exist for the convenience of different callers.</dd>
 * <dt>18.5.3 Functional Interface Parameterization Inference</dt>
 * <dd>Controlled from {@link LambdaExpression#resolveType(BlockScope)}.</dd>
 * <dt>18.5.4 More Specific Method Inference</dt>
 * <dd><em>Not Yet Implemented</em></dd>
 * </dl>
 * For 18.5.1 and 18.5.2 high-level control is implemented in
 *  {@link ParameterizedGenericMethodBinding#computeCompatibleMethod(MethodBinding, TypeBinding[], Scope, InvocationSite)}.
 * <h2>Inference Lifecycle</h2>
 * <li>Decision whether or not an invocation is a <b>variable-arity</b> invocation is made by first attempting
 * 		to solve 18.5.1 in mode {@link #CHECK_LOOSE}. Only if that fails, another attempt is made in mode {@link #CHECK_VARARG}.
 * 		Which of these two attempts was successful is stored in {@link #inferenceKind}.
 * 		This field must be consulted whenever arguments of an invocation should be further processed.
 * 		See also {@link #getParameter(TypeBinding[], int, boolean)} and its clients.</li>
 * </ul>
 */
public class InferenceContext18 {

	/** to conform with javac regarding https://bugs.openjdk.java.net/browse/JDK-8026527 */
	static final boolean SIMULATE_BUG_JDK_8026527 = true;

	/** Temporary workaround until we know fully what to do with https://bugs.openjdk.java.net/browse/JDK-8054721
	 *  It looks likely that we have a bug independent of this JLS bug in that we clear the capture bounds eagerly.
	*/
	static final boolean SHOULD_WORKAROUND_BUG_JDK_8054721 = true; // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=437444#c24 onwards

	static final boolean SHOULD_WORKAROUND_BUG_JDK_8153748 = true; // emulating javac behaviour after private email communication

	/**
	 * Detail flag to control the extent of {@link #SIMULATE_BUG_JDK_8026527}.
	 * A setting of 'false' implements the advice from http://mail.openjdk.java.net/pipermail/lambda-spec-experts/2013-December/000447.html
	 * i.e., raw types are not considered as compatible in constraints/bounds derived from invocation arguments,
	 * but only for constraints derived from type variable bounds.
	 */
	static final boolean ARGUMENT_CONSTRAINTS_ARE_SOFT = false;

	// --- Main State of the Inference: ---

	/** the invocation being inferred (for 18.5.1 and 18.5.2) */
	InvocationSite currentInvocation;
	/** arguments of #currentInvocation, if any */
	Expression[] invocationArguments;

	/** The inference variables for which as solution is sought. */
	InferenceVariable[] inferenceVariables;

	/** Constraints that have not yet been reduced and incorporated. */
	ConstraintFormula[] initialConstraints;
	ConstraintExpressionFormula[] finalConstraints; // for final revalidation at a "macroscopic" level

	/** The accumulated type bounds etc. */
	BoundSet currentBounds;

	/** One of CHECK_STRICT, CHECK_LOOSE, or CHECK_VARARGS. */
	int inferenceKind;
	/** Marks how much work has been done so far? Used to avoid performing any of these tasks more than once. */
	public int stepCompleted = NOT_INFERRED;

	public static final int NOT_INFERRED = 0;
	/** Applicability Inference (18.5.1) has been completed. */
	public static final int APPLICABILITY_INFERRED = 1;
	/** Invocation Type Inference (18.5.2) has been completed (for some target type). */
	public static final int TYPE_INFERRED = 2;
	public static final int TYPE_INFERRED_FINAL = 3; // as above plus asserting that target type was a proper type

	/** Signals whether any type compatibility makes use of unchecked conversion. */
	public List<ConstraintFormula> constraintsWithUncheckedConversion;
	public boolean usesUncheckedConversion;
	public InferenceContext18 outerContext;
	private Set<InferenceContext18> seenInnerContexts;
	Scope scope;
	LookupEnvironment environment;
	ReferenceBinding object; // java.lang.Object
	public BoundSet b2;
	private BoundSet b3;
	/** Not per JLS: inbox for emulation of how javac passes type bounds from inner to outer */
	private BoundSet innerInbox;
	/** Not per JLS: signal when current is ready to directly merge all bounds from inner. */
	private boolean directlyAcceptingInnerBounds = false;
	/** Not per JLS: pushing bounds from inner to outer may have to be deferred till after overload resolution, store here a runnable to perform the push. */
	private Runnable pushToOuterJob = null;

	public static boolean isSameSite(InvocationSite site1, InvocationSite site2) {
		if (site1 == site2)
			return true;
		if (site1 == null || site2 == null)
			return false;
		if (site1.sourceStart() == site2.sourceStart() && site1.sourceEnd() == site2.sourceEnd())
			return true;
		return false;
	}

	public static final int CHECK_UNKNOWN = 0;
	public static final int CHECK_STRICT = 1;
	public static final int CHECK_LOOSE = 2;
	public static final int CHECK_VARARG = 3;

	static class SuspendedInferenceRecord {
		InvocationSite site;
		Expression[] invocationArguments;
		InferenceVariable[] inferenceVariables;
		int inferenceKind;
		boolean usesUncheckedConversion;
		SuspendedInferenceRecord(InvocationSite site, Expression[] invocationArguments, InferenceVariable[] inferenceVariables, int inferenceKind, boolean usesUncheckedConversion) {
			this.site = site;
			this.invocationArguments = invocationArguments;
			this.inferenceVariables = inferenceVariables;
			this.inferenceKind = inferenceKind;
			this.usesUncheckedConversion = usesUncheckedConversion;
		}
	}

	/** Construct an inference context for an invocation (method/constructor). */
	public InferenceContext18(Scope scope, Expression[] arguments, InvocationSite site, InferenceContext18 outerContext) {
		this.scope = scope;
		this.environment = scope.environment();
		this.object = scope.getJavaLangObject();
		this.invocationArguments = arguments;
		this.currentInvocation = site;
		this.outerContext = outerContext;
		if (site instanceof Invocation)
			scope.compilationUnitScope().registerInferredInvocation((Invocation) site);
	}

	public InferenceContext18(Scope scope) {
		this.scope = scope;
		this.environment = scope.environment();
		this.object = scope.getJavaLangObject();
	}

	/**
	 * JLS 18.1.3: Create initial bounds from a given set of type parameters declarations.
	 * @return the set of inference variables created for the given typeParameters
	 */
	public InferenceVariable[] createInitialBoundSet(TypeVariableBinding[] typeParameters) {
		//
		if (this.currentBounds == null) {
			this.currentBounds = new BoundSet();
		}
		if (typeParameters != null) {
			InferenceVariable[] newInferenceVariables = addInitialTypeVariableSubstitutions(typeParameters);
			this.currentBounds.addBoundsFromTypeParameters(this, typeParameters, newInferenceVariables);
			return newInferenceVariables;
		}
		return Binding.NO_INFERENCE_VARIABLES;
	}

	/**
	 * Substitute any type variables mentioned in 'type' by the corresponding inference variable, if one exists.
	 */
	public TypeBinding substitute(TypeBinding type) {
		InferenceSubstitution inferenceSubstitution = new InferenceSubstitution(this);
		return 	inferenceSubstitution.substitute(inferenceSubstitution, type);
	}

	/** JLS 18.5.1: compute bounds from formal and actual parameters. */
	public void createInitialConstraintsForParameters(TypeBinding[] parameters, boolean checkVararg, TypeBinding varArgsType, MethodBinding method) {
		if (this.invocationArguments == null)
			return;
		int len = checkVararg ? parameters.length - 1 : Math.min(parameters.length, this.invocationArguments.length);
		int maxConstraints = checkVararg ? this.invocationArguments.length : len;
		int numConstraints = 0;
		boolean ownConstraints;
		if (this.initialConstraints == null) {
			this.initialConstraints = new ConstraintFormula[maxConstraints];
			ownConstraints = true;
		} else {
			numConstraints = this.initialConstraints.length;
			maxConstraints += numConstraints;
			System.arraycopy(this.initialConstraints, 0,
					this.initialConstraints=new ConstraintFormula[maxConstraints], 0, numConstraints);
			ownConstraints = false; // these are lifted from a nested poly expression.
		}
		for (int i = 0; i < len; i++) {
			TypeBinding thetaF = substitute(parameters[i]);
			if (this.invocationArguments[i].isPertinentToApplicability(parameters[i], method)) {
				this.initialConstraints[numConstraints++] = new ConstraintExpressionFormula(this.invocationArguments[i], thetaF, ReductionResult.COMPATIBLE, ARGUMENT_CONSTRAINTS_ARE_SOFT);
			} else if (!isTypeVariableOfCandidate(parameters[i], method)) {
				this.initialConstraints[numConstraints++] = new ConstraintExpressionFormula(this.invocationArguments[i], thetaF, ReductionResult.POTENTIALLY_COMPATIBLE);
			} // else we know it is potentially compatible, no need to assert.
		}
		if (checkVararg && varArgsType instanceof ArrayBinding) {
			varArgsType = ((ArrayBinding)varArgsType).elementsType();
			TypeBinding thetaF = substitute(varArgsType);
			for (int i = len; i < this.invocationArguments.length; i++) {
				if (this.invocationArguments[i].isPertinentToApplicability(varArgsType, method)) {
					this.initialConstraints[numConstraints++] = new ConstraintExpressionFormula(this.invocationArguments[i], thetaF, ReductionResult.COMPATIBLE, ARGUMENT_CONSTRAINTS_ARE_SOFT);
				} else if (!isTypeVariableOfCandidate(varArgsType, method)) {
					this.initialConstraints[numConstraints++] = new ConstraintExpressionFormula(this.invocationArguments[i], thetaF, ReductionResult.POTENTIALLY_COMPATIBLE);
				} // else we know it is potentially compatible, no need to assert.
			}
		}
		if (numConstraints == 0)
			this.initialConstraints = ConstraintFormula.NO_CONSTRAINTS;
		else if (numConstraints < maxConstraints)
			System.arraycopy(this.initialConstraints, 0, this.initialConstraints = new ConstraintFormula[numConstraints], 0, numConstraints);
		if (ownConstraints) { // lifted constraints get validated at their own context.
			final int length = this.initialConstraints.length;
			System.arraycopy(this.initialConstraints, 0, this.finalConstraints = new ConstraintExpressionFormula[length], 0, length);
		}
	}

	private boolean isTypeVariableOfCandidate(TypeBinding type, MethodBinding candidate) {
		// cf. FunctionalExpression.isPertinentToApplicability()
		if (type instanceof TypeVariableBinding) {
			Binding declaringElement = ((TypeVariableBinding) type).declaringElement;
			if (declaringElement == candidate)
				return true;
			if (candidate.isConstructor() && declaringElement == candidate.declaringClass)
				return true;
		}
		return false;
	}

	private InferenceVariable[] addInitialTypeVariableSubstitutions(TypeBinding[] typeVariables) {
		int len = typeVariables.length;
		if (len == 0) {
			if (this.inferenceVariables == null)
				this.inferenceVariables = Binding.NO_INFERENCE_VARIABLES;
			return Binding.NO_INFERENCE_VARIABLES;
		}
		InferenceVariable[] newVariables = new InferenceVariable[len];
		for (int i = 0; i < len; i++)
			newVariables[i] = InferenceVariable.get(typeVariables[i], i, this.currentInvocation, this.scope, this.object, true);
		addInferenceVariables(newVariables);
		return newVariables;
	}

	private void addInferenceVariables(InferenceVariable[] newVariables) {
		if (this.inferenceVariables == null || this.inferenceVariables.length == 0) {
			this.inferenceVariables = newVariables;
		} else {
			// merge into this.inferenceVariables:
			int len = newVariables.length;
			int prev = this.inferenceVariables.length;
			System.arraycopy(this.inferenceVariables, 0, this.inferenceVariables = new InferenceVariable[len+prev], 0, prev);
			System.arraycopy(newVariables, 0, this.inferenceVariables, prev, len);
		}
	}

	/** Add new inference variables for the given type variables. */
	public InferenceVariable[] addTypeVariableSubstitutions(TypeBinding[] typeVariables) {
		int len2 = typeVariables.length;
		InferenceVariable[] newVariables = new InferenceVariable[len2];
		InferenceVariable[] toAdd = new InferenceVariable[len2];
		int numToAdd = 0;
		for (int i = 0; i < typeVariables.length; i++) {
			if (typeVariables[i] instanceof InferenceVariable)
				newVariables[i] = (InferenceVariable) typeVariables[i]; // prevent double substitution of an already-substituted inferenceVariable
			else
				toAdd[numToAdd++] =
					newVariables[i] = InferenceVariable.get(typeVariables[i], i, this.currentInvocation, this.scope, this.object, false);
		}
		if (numToAdd > 0) {
			int start = 0;
			if (this.inferenceVariables != null) {
				int len1 = this.inferenceVariables.length;
				System.arraycopy(this.inferenceVariables, 0, this.inferenceVariables = new InferenceVariable[len1+numToAdd], 0, len1);
				start = len1;
			} else {
				this.inferenceVariables = new InferenceVariable[numToAdd];
			}
			System.arraycopy(toAdd, 0, this.inferenceVariables, start, numToAdd);
		}
		return newVariables;
	}

	/** JLS 18.1.3 Bounds: throws α: the inference variable α appears in a throws clause */
	public void addThrowsContraints(TypeBinding[] parameters, InferenceVariable[] variables, ReferenceBinding[] thrownExceptions) {
		for (int i = 0; i < parameters.length; i++) {
			TypeBinding parameter = parameters[i];
			for (int j = 0; j < thrownExceptions.length; j++) {
				if (TypeBinding.equalsEquals(parameter, thrownExceptions[j])) {
					this.currentBounds.inThrows.add(variables[i].prototype());
					break;
				}
			}
		}
	}

	/** JLS 18.5.1 Invocation Applicability Inference. */
	public void inferInvocationApplicability(MethodBinding method, TypeBinding[] arguments, boolean isDiamond) {
		ConstraintExpressionFormula.inferInvocationApplicability(this, method, arguments, isDiamond, this.inferenceKind);
	}

	/** Perform steps from JLS 18.5.2. needed for computing the bound set B3. */
	boolean computeB3(InvocationSite invocationSite, TypeBinding targetType, MethodBinding method)
				throws InferenceFailureException
	{
		boolean result = ConstraintExpressionFormula.inferPolyInvocationType(this, invocationSite, targetType, method);
		if (result) {
			mergeInnerBounds();
			if (this.b3 == null)
				this.b3 = this.currentBounds.copy();
		}
		return result;
	}

	/** JLS 18.5.2 Invocation Type Inference
	 */
	public BoundSet inferInvocationType(TypeBinding expectedType, InvocationSite invocationSite, MethodBinding method) throws InferenceFailureException
	{
		// not JLS: simply ensure that null hints from the return type have been seen even in standalone contexts:
		if (expectedType == null && method.returnType != null)
			substitute(method.returnType); // result is ignore, the only effect is on InferenceVariable.nullHints

		this.currentBounds = this.b2.copy();

		int step = (expectedType == null || expectedType.isProperType(true)) ? TYPE_INFERRED_FINAL : TYPE_INFERRED;

		try {
			// bullets 1&2: definitions only.
			if (expectedType != null
					&& expectedType != TypeBinding.VOID
					&& invocationSite instanceof Expression && ((Expression) invocationSite).isTrulyExpression()
					&& ((Expression)invocationSite).isPolyExpression(method))
			{
				// 3. bullet: special treatment for poly expressions
				if (!computeB3(invocationSite, expectedType, method)) {
					return null;
				}
			} else {
				mergeInnerBounds();
				this.b3 = this.currentBounds.copy();
			}

			if (SHOULD_WORKAROUND_BUG_JDK_8153748) { // "before 18.5.2", but should not spill into b3 ... (heuristically)
				ReductionResult jdk8153748result = addJDK_8153748ConstraintsFromInvocation(this.invocationArguments, method, new InferenceSubstitution(this));
				if (jdk8153748result != null) {
					if (!this.currentBounds.incorporate(this))
						return null;
				}
			}

			pushBoundsToOuter();
			this.directlyAcceptingInnerBounds = true;

			// 4. bullet: assemble C:
			Set<ConstraintFormula> c = new LinkedHashSet<ConstraintFormula>();
			if (!addConstraintsToC(this.invocationArguments, c, method, this.inferenceKind, invocationSite))
				return null;
			// 5. bullet: determine B4 from C
			List<Set<InferenceVariable>> components = this.currentBounds.computeConnectedComponents(this.inferenceVariables);
			while (!c.isEmpty()) {
				// *
				Set<ConstraintFormula> bottomSet = findBottomSet(c, allOutputVariables(c), components);
				if (bottomSet.isEmpty()) {
					bottomSet.add(pickFromCycle(c));
				}
				// *
				c.removeAll(bottomSet);
				// * The union of the input variables of all the selected constraints, α1, ..., αm, ...
				Set<InferenceVariable> allInputs = new LinkedHashSet<InferenceVariable>();
				Iterator<ConstraintFormula> bottomIt = bottomSet.iterator();
				while (bottomIt.hasNext()) {
					allInputs.addAll(bottomIt.next().inputVariables(this));
				}
				InferenceVariable[] variablesArray = allInputs.toArray(new InferenceVariable[allInputs.size()]);
				//   ... is resolved
				if (!this.currentBounds.incorporate(this))
					return null;
				BoundSet solution = resolve(variablesArray);
				// in rare cases resolving just one set of variables doesn't suffice,
				// don't bother with finding the necessary superset, just resolve all:
				if (solution == null)
					solution = resolve(this.inferenceVariables);
				// * ~ apply substitutions to all constraints:
				bottomIt = bottomSet.iterator();
				while (bottomIt.hasNext()) {
					ConstraintFormula constraint = bottomIt.next();
					if (solution != null)
						if (!constraint.applySubstitution(solution, variablesArray))
							return null;
				// * reduce and incorporate
					if (!this.currentBounds.reduceOneConstraint(this, constraint))
						return null;
				}
			}
			// 6. bullet: solve
			BoundSet solution = solve();
			if (solution == null || !isResolved(solution)) {
				this.currentBounds = this.b2; // don't let bounds from unsuccessful attempt leak into subsequent attempts
				return null;
			}
			// we're done, start reporting:
			reportUncheckedConversions(solution);
			if (step == TYPE_INFERRED_FINAL)
				this.currentBounds = solution; // this is final, keep the result:
			return solution;
		} finally {
			this.stepCompleted = step;
		}
	}

	// ---  not per JLS: emulate how javac passes type bounds from inner to outer: ---
	/** Not per JLS: push current bounds to outer inference if outer is ready for it. */
	private void pushBoundsToOuter() {
		pushBoundsTo(this.outerContext);
	}

	/** Not per JLS: invent more bubbling up of inner bounds. */
	public void pushBoundsTo(InferenceContext18 outer) {
		if (outer != null && outer.stepCompleted >= APPLICABILITY_INFERRED) {
			boolean deferred = outer.currentInvocation instanceof Invocation; // need to wait till after overload resolution?
			BoundSet toPush = deferred ? this.currentBounds.copy() : this.currentBounds;
			Runnable job = () -> {
				if (outer.directlyAcceptingInnerBounds) {
					outer.currentBounds.addBounds(toPush, this.environment);
				} else if (outer.innerInbox == null) {
					outer.innerInbox = deferred ? toPush : toPush.copy(); // copy now, unless already copied on behalf of 'deferred'
				} else {
					outer.innerInbox.addBounds(toPush, this.environment);
				}
			};
			if (deferred) {
				this.pushToOuterJob = job;
			} else {
				job.run(); // TODO(stephan): ever reached? for ReferenceExpression? (would need a corresponding new call to flushBoundOutbox()).
			}
		}
	}
	/** Not JLS: after overload resolution is done, perform the push of type bounds to outer inference, if any. */
	public void flushBoundOutbox() {
		if (this.pushToOuterJob != null) {
			this.pushToOuterJob.run();
			this.pushToOuterJob = null;
		}
	}
	/** Not JLS: merge pending bounds of inner inference into current. */
	private void mergeInnerBounds() {
		if (this.innerInbox != null) {
			this.currentBounds.addBounds(this.innerInbox, this.environment);
			this.innerInbox = null;
		}
	}

	interface InferenceOperation {
		boolean perform() throws InferenceFailureException;
	}
	/** Not per JLS: if operation succeeds merge new bounds from inner into current. */
	private boolean collectingInnerBounds(InferenceOperation operation) throws InferenceFailureException {
		boolean result = operation.perform();
		if (result)
			mergeInnerBounds();
		else
			this.innerInbox = null;
		return result;
	}
	// ---

	private ReductionResult addJDK_8153748ConstraintsFromInvocation(Expression[] arguments, MethodBinding method, InferenceSubstitution substitution)
			throws InferenceFailureException
	{
		// not per JLS, trying to mimic javac behavior
		boolean constraintAdded = false;
		if (arguments != null) {
			for (int i = 0; i < arguments.length; i++) {
				Expression argument = arguments[i];
				TypeBinding parameter = getParameter(method.parameters, i, method.isVarargs());
				if (parameter == null)
					return ReductionResult.FALSE;
				parameter = substitution.substitute(substitution, parameter);
				ReductionResult result = addJDK_8153748ConstraintsFromExpression(argument, parameter, method, substitution);
				if (result == ReductionResult.FALSE)
					return ReductionResult.FALSE;
				if (result == ReductionResult.TRUE)
					constraintAdded = true;
			}
		}
		return constraintAdded ? ReductionResult.TRUE : null;
	}

	private ReductionResult addJDK_8153748ConstraintsFromExpression(Expression argument, TypeBinding parameter, MethodBinding method,
			InferenceSubstitution substitution)
			throws InferenceFailureException
	{
		if (argument instanceof FunctionalExpression) {
			return addJDK_8153748ConstraintsFromFunctionalExpr((FunctionalExpression) argument, parameter, method);
		} else if (argument instanceof Invocation && argument.isPolyExpression(method)) {
			Invocation invocation = (Invocation) argument;
			Expression[] innerArgs = invocation.arguments();
			MethodBinding innerMethod = invocation.binding();
			if (innerMethod != null && innerMethod.isValidBinding()) {
				substitution = enrichSubstitution(substitution, invocation, innerMethod);
				return addJDK_8153748ConstraintsFromInvocation(innerArgs, innerMethod.shallowOriginal(), substitution);
			}
		} else if (argument instanceof ConditionalExpression) {
			ConditionalExpression ce = (ConditionalExpression) argument;
			if (addJDK_8153748ConstraintsFromExpression(ce.valueIfTrue, parameter, method, substitution) == ReductionResult.FALSE)
				return ReductionResult.FALSE;
			return addJDK_8153748ConstraintsFromExpression(ce.valueIfFalse, parameter, method, substitution);
		} else if (argument instanceof SwitchExpression) {
			SwitchExpression se = (SwitchExpression) argument;
			ReductionResult result = ReductionResult.FALSE;
			for (Expression re : se.resultExpressions) {
				result = addJDK_8153748ConstraintsFromExpression(re, parameter, method, substitution);
				if (result == ReductionResult.FALSE)
					break;
			}
			return result;
		}
		return null;
	}

	private ReductionResult addJDK_8153748ConstraintsFromFunctionalExpr(FunctionalExpression functionalExpr, TypeBinding targetType, MethodBinding method) throws InferenceFailureException {
		if (!functionalExpr.isPertinentToApplicability(targetType, method)) {
			ConstraintFormula exprConstraint = new ConstraintExpressionFormula(functionalExpr, targetType, ReductionResult.COMPATIBLE, ARGUMENT_CONSTRAINTS_ARE_SOFT);
			if (collectingInnerBounds(() -> exprConstraint.inputVariables(this).isEmpty())) { // input variable would signal: not ready for inference
				if (!collectingInnerBounds(() -> reduceAndIncorporate(exprConstraint)))
					return ReductionResult.FALSE;
				ConstraintFormula excConstraint = new ConstraintExceptionFormula(functionalExpr, targetType); // ??
				if (!collectingInnerBounds(() -> reduceAndIncorporate(excConstraint)))
					return ReductionResult.FALSE;
				return ReductionResult.TRUE;
			}
		}
		return null;
	}

	InferenceSubstitution enrichSubstitution(InferenceSubstitution substitution, Invocation innerInvocation, MethodBinding innerMethod) {
		if (innerMethod instanceof ParameterizedGenericMethodBinding) {
			InferenceContext18 innerContext = innerInvocation.getInferenceContext((ParameterizedMethodBinding) innerMethod);
			if (innerContext != null)
				return substitution.addContext(innerContext);
		}
		return substitution;
	}

	private boolean addConstraintsToC(Expression[] exprs, Set<ConstraintFormula> c, MethodBinding method, int inferenceKindForMethod, InvocationSite site)
			throws InferenceFailureException
	{
		TypeBinding[] fs;
		if (exprs != null) {
			int k = exprs.length;
			int p = method.parameters.length;
			if (method.isVarargs()) {
				if (k < p - 1) return false;
			} else if (k != p) {
				return false;
			}
			switch (inferenceKindForMethod) {
				case CHECK_STRICT:
				case CHECK_LOOSE:
					fs = method.parameters;
					break;
				case CHECK_VARARG:
					fs = varArgTypes(method.parameters, k);
					break;
				default:
					throw new IllegalStateException("Unexpected checkKind "+this.inferenceKind); //$NON-NLS-1$
			}
			for (int i = 0; i < k; i++) {
				TypeBinding fsi = fs[Math.min(i, p-1)];
				InferenceSubstitution inferenceSubstitution = new InferenceSubstitution(this.environment, this.inferenceVariables, site);
				TypeBinding substF = inferenceSubstitution.substitute(inferenceSubstitution,fsi);
				if (!addConstraintsToC_OneExpr(exprs[i], c, fsi, substF, method))
					return false;
	        }
		}
		return true;
	}

	private boolean addConstraintsToC_OneExpr(Expression expri, Set<ConstraintFormula> c, TypeBinding fsi, TypeBinding substF, MethodBinding method)
			throws InferenceFailureException
	{
		boolean substFIsProperType = substF.isProperType(true);
		// -- not per JLS, emulate javac behavior:
		substF = Scope.substitute(getResultSubstitution(this.b3), substF);
		// --

		// For all i (1 ≤ i ≤ k), if ei is not pertinent to applicability, the set contains ⟨ei → θ Fi⟩.
		if (!expri.isPertinentToApplicability(fsi, method)) {
			c.add(new ConstraintExpressionFormula(expri, substF, ReductionResult.COMPATIBLE, ARGUMENT_CONSTRAINTS_ARE_SOFT));
		}
		if (expri instanceof FunctionalExpression) {
			c.add(new ConstraintExceptionFormula((FunctionalExpression) expri, substF));
			if (expri instanceof LambdaExpression) {
				// https://bugs.openjdk.java.net/browse/JDK-8038747
				LambdaExpression lambda = (LambdaExpression) expri;
				BlockScope skope = lambda.enclosingScope;
				if (substF.isFunctionalInterface(skope)) { // could be an inference variable.
					ReferenceBinding t = (ReferenceBinding) substF;
					ParameterizedTypeBinding withWildCards = InferenceContext18.parameterizedWithWildcard(t);
					if (withWildCards != null) {
						t = ConstraintExpressionFormula.findGroundTargetType(this, skope, lambda, withWildCards);
					}
					MethodBinding functionType;
					if (t != null && (functionType = t.getSingleAbstractMethod(skope, true)) != null && (lambda = lambda.resolveExpressionExpecting(t, this.scope, this)) != null) {
						TypeBinding r = functionType.returnType;
						Expression[] resultExpressions = lambda.resultExpressions();
						for (int i = 0, length = resultExpressions == null ? 0 : resultExpressions.length; i < length; i++) {
							Expression resultExpression = resultExpressions[i];
							if (!addConstraintsToC_OneExpr(resultExpression, c, r.original(), r, method))
								return false;
						}
					}
				}
			}
		} else if (expri instanceof Invocation && expri.isPolyExpression()) {

			if (substFIsProperType) // https://bugs.openjdk.java.net/browse/JDK-8052325
				return true;

			Invocation invocation = (Invocation) expri;
			MethodBinding innerMethod = invocation.binding();
			if (innerMethod == null)
				return true; 		  // -> proceed with no new C set elements.

			Expression[] arguments = invocation.arguments();
			TypeBinding[] argumentTypes = arguments == null ? Binding.NO_PARAMETERS : new TypeBinding[arguments.length];
			for (int i = 0; i < argumentTypes.length; i++)
				argumentTypes[i] = arguments[i].resolvedType;
			InferenceContext18 innerContext = null;
			if (innerMethod instanceof ParameterizedGenericMethodBinding)
				 innerContext = invocation.getInferenceContext((ParameterizedGenericMethodBinding) innerMethod);

			if (innerContext != null) {
				MethodBinding shallowMethod = innerMethod.shallowOriginal();
				innerContext.outerContext = this;
				if (innerContext.stepCompleted < InferenceContext18.APPLICABILITY_INFERRED) // shouldn't happen, but let's play safe
					innerContext.inferInvocationApplicability(shallowMethod, argumentTypes, shallowMethod.isConstructor());
				if (!innerContext.computeB3(invocation, substF, shallowMethod))
					return false;
				if (innerContext.addConstraintsToC(arguments, c, innerMethod.genericMethod(), innerContext.inferenceKind, invocation)) {
					this.currentBounds.addBounds(innerContext.currentBounds, this.environment);
					return true;
				}
				return false;
			} else {
				int applicabilityKind = getInferenceKind(innerMethod, argumentTypes);
				return this.addConstraintsToC(arguments, c, innerMethod.genericMethod(), applicabilityKind, invocation);
			}
		} else if (expri instanceof ConditionalExpression) {
			ConditionalExpression ce = (ConditionalExpression) expri;
			return addConstraintsToC_OneExpr(ce.valueIfTrue, c, fsi, substF, method)
					&& addConstraintsToC_OneExpr(ce.valueIfFalse, c, fsi, substF, method);
		} else if (expri instanceof SwitchExpression) {
			SwitchExpression se = (SwitchExpression) expri;
			for (Expression re : se.resultExpressions) {
				if (!addConstraintsToC_OneExpr(re, c, fsi, substF, method))
					return false;
			}
			return true;
		}
		return true;
	}


	protected int getInferenceKind(MethodBinding nonGenericMethod, TypeBinding[] argumentTypes) {
		switch (this.scope.parameterCompatibilityLevel(nonGenericMethod, argumentTypes)) {
			case Scope.AUTOBOX_COMPATIBLE:
				return CHECK_LOOSE;
			case Scope.VARARGS_COMPATIBLE:
				return CHECK_VARARG;
			default:
				return CHECK_STRICT;
		}
	}

	/**
	 * 18.5.3 Functional Interface Parameterization Inference
	 */
	public ReferenceBinding inferFunctionalInterfaceParameterization(LambdaExpression lambda, BlockScope blockScope,
			ParameterizedTypeBinding targetTypeWithWildCards)
	{
		TypeBinding[] q = createBoundsForFunctionalInterfaceParameterizationInference(targetTypeWithWildCards);
		if (q == null || q.length != lambda.arguments().length) {
			// fail  TODO: can this still happen here?
		} else {
			if (reduceWithEqualityConstraints(lambda.argumentTypes(), q)) {
				ReferenceBinding genericType = targetTypeWithWildCards.genericType();
				TypeBinding[] a = targetTypeWithWildCards.arguments; // a is not-null by construction of parameterizedWithWildcard()
				TypeBinding[] aprime = getFunctionInterfaceArgumentSolutions(a);
				// If F<A'1, ..., A'm> is a well-formed type, ...
				ParameterizedTypeBinding ptb = blockScope.environment().createParameterizedType(genericType, aprime, targetTypeWithWildCards.enclosingType());
				TypeVariableBinding[] vars = ptb.genericType().typeVariables();
				ParameterizedTypeBinding captured = ptb.capture(blockScope, lambda.sourceStart, lambda.sourceEnd);
				for (int i = 0; i < vars.length; i++) {
					if (vars[i].boundCheck(captured, aprime[i], blockScope, lambda) == BoundCheckStatus.MISMATCH)
						return null;
				}
				return ptb;
			}
		}
		return targetTypeWithWildCards;
	}

	/**
	 * Create initial bound set for 18.5.3 Functional Interface Parameterization Inference
	 * @param functionalInterface the functional interface F<A1,..Am>
	 * @return the parameter types Q1..Qk of the function type of the type F<α1, ..., αm>, or null
	 */
	TypeBinding[] createBoundsForFunctionalInterfaceParameterizationInference(ParameterizedTypeBinding functionalInterface) {
		if (this.currentBounds == null)
			this.currentBounds = new BoundSet();
		TypeBinding[] a = functionalInterface.arguments;
		if (a == null)
			return null;
		InferenceVariable[] alpha = addInitialTypeVariableSubstitutions(a);

		for (int i = 0; i < a.length; i++) {
			TypeBound bound;
			if (a[i].kind() == Binding.WILDCARD_TYPE) {
				WildcardBinding wildcard = (WildcardBinding) a[i];
				switch(wildcard.boundKind) {
    				case Wildcard.EXTENDS :
    					bound = new TypeBound(alpha[i], wildcard.allBounds(), ReductionResult.SUBTYPE);
    					break;
    				case Wildcard.SUPER :
    					bound = new TypeBound(alpha[i], wildcard.bound, ReductionResult.SUPERTYPE);
    					break;
    				case Wildcard.UNBOUND :
    					bound = new TypeBound(alpha[i], this.object, ReductionResult.SUBTYPE);
    					break;
    				default:
    					continue; // cannot
				}
			} else {
				bound = new TypeBound(alpha[i], a[i], ReductionResult.SAME);
			}
			this.currentBounds.addBound(bound, this.environment);
		}
		TypeBinding falpha = substitute(functionalInterface);
		return falpha.getSingleAbstractMethod(this.scope, true).parameters;
	}

	public boolean reduceWithEqualityConstraints(TypeBinding[] p, TypeBinding[] q) {
		if (p != null) {
			for (int i = 0; i < p.length; i++) {
				try {
					if (!this.reduceAndIncorporate(ConstraintTypeFormula.create(p[i], q[i], ReductionResult.SAME)))
						return false;
				} catch (InferenceFailureException e) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 18.5.4 More Specific Method Inference
	 */
	public boolean isMoreSpecificThan(MethodBinding m1, MethodBinding m2, boolean isVarArgs, boolean isVarArgs2) {
		// TODO: we don't yet distinguish vararg-with-passthrough from vararg-with-exactly-one-vararg-arg
		if (isVarArgs != isVarArgs2) {
			return isVarArgs2;
		}
		Expression[] arguments = this.invocationArguments;
		int numInvocArgs = arguments == null ? 0 : arguments.length;
		TypeVariableBinding[] p = m2.typeVariables();
		TypeBinding[] s = m1.parameters;
		TypeBinding[] t = new TypeBinding[m2.parameters.length];
		createInitialBoundSet(p);
		for (int i = 0; i < t.length; i++)
			t[i] = substitute(m2.parameters[i]);

		try {
			for (int i = 0; i < numInvocArgs; i++) {
				TypeBinding si = getParameter(s, i, isVarArgs);
				TypeBinding ti = getParameter(t, i, isVarArgs);
				Boolean result = moreSpecificMain(si, ti, this.invocationArguments[i]);
				if (result == Boolean.FALSE)
					return false;
				if (result == null)
					if (!reduceAndIncorporate(ConstraintTypeFormula.create(si, ti, ReductionResult.SUBTYPE)))
						return false;
			}
			if (t.length == numInvocArgs + 1) {
				TypeBinding skplus1 = getParameter(s, numInvocArgs, true);
				TypeBinding tkplus1 = getParameter(t, numInvocArgs, true);
				if (!reduceAndIncorporate(ConstraintTypeFormula.create(skplus1, tkplus1, ReductionResult.SUBTYPE)))
					return false;
			}
			return solve() != null;
		} catch (InferenceFailureException e) {
			return false;
		}
	}

	// FALSE: inference fails
	// TRUE:  constraints have been incorporated
	// null:  need to create the si <: ti constraint
	private Boolean moreSpecificMain(TypeBinding si, TypeBinding ti, Expression expri) throws InferenceFailureException {
		if (si.isProperType(true) && ti.isProperType(true)) {
			return expri.sIsMoreSpecific(si, ti, this.scope) ? Boolean.TRUE : Boolean.FALSE;
		}
		// "if Ti is not a functional interface type" specifically requests the si <: ti constraint created by our caller
		if (!ti.isFunctionalInterface(this.scope))
			return null;

		TypeBinding funcI = ti.original();
		// "It must be determined whether Si satisfies the following five conditions:"
		// (we negate each condition for early exit):
		if (si.isFunctionalInterface(this.scope)) {			// bullet 1
			if (siSuperI(si, funcI) || siSubI(si, funcI))
				return null;								// bullets 2 & 3
			if (si instanceof IntersectionTypeBinding18) {
				TypeBinding[] elements = ((IntersectionTypeBinding18)si).intersectingTypes;
				checkSuper: {
					for (int i = 0; i < elements.length; i++)
						if (!siSuperI(elements[i], funcI))
							break checkSuper;
					return null;							// bullet 4
					// each element of the intersection is a superinterface of I, or a parameterization of a superinterface of I.
				}
				for (int i = 0; i < elements.length; i++)
					if (siSubI(elements[i], funcI))
						return null;						// bullet 5
						// some element of the intersection is a subinterface of I, or a parameterization of a subinterface of I.
			}
			// all passed, time to do some work:
			TypeBinding siCapture = si.capture(this.scope, expri.sourceStart, expri.sourceEnd);
			MethodBinding sam = siCapture.getSingleAbstractMethod(this.scope, false); // no wildcards should be left needing replacement
			TypeBinding[] u = sam.parameters;
			TypeBinding r1 = sam.isConstructor() ? sam.declaringClass : sam.returnType;
			sam = ti.getSingleAbstractMethod(this.scope, true); // TODO
			TypeBinding[] v = sam.parameters;
			TypeBinding r2 = sam.isConstructor() ? sam.declaringClass : sam.returnType;
			return Boolean.valueOf(checkExpression(expri, u, r1, v, r2));
		}
		return null;
	}

	private boolean checkExpression(Expression expri, TypeBinding[] u, TypeBinding r1, TypeBinding[] v, TypeBinding r2)
			throws InferenceFailureException {
		if (expri instanceof LambdaExpression && !((LambdaExpression)expri).argumentsTypeElided()) {
			for (int i = 0; i < u.length; i++) {
				if (!reduceAndIncorporate(ConstraintTypeFormula.create(u[i], v[i], ReductionResult.SAME)))
					return false;
			}
			if (r2.id == TypeIds.T_void)
				return true;
			LambdaExpression lambda = (LambdaExpression) expri;
			Expression[] results = lambda.resultExpressions();
			if (results != Expression.NO_EXPRESSIONS) {
				if (r1.isFunctionalInterface(this.scope) && r2.isFunctionalInterface(this.scope)
						&& !(r1.isCompatibleWith(r2) || r2.isCompatibleWith(r1))) {
					// "these rules are applied recursively to R1 and R2, for each result expression in expi."
					// (what does "applied .. to R1 and R2" mean? Why mention R1/R2 and not U/V?)
					for (int i = 0; i < results.length; i++) {
						if (!checkExpression(results[i], u, r1, v, r2))
							return false;
					}
					return true;
				}
				checkPrimitive1: if (r1.isPrimitiveType() && !r2.isPrimitiveType()) {
					// check: each result expression is a standalone expression of a primitive type
					for (int i = 0; i < results.length; i++) {
						if (results[i].isPolyExpression() || (results[i].resolvedType != null && !results[i].resolvedType.isPrimitiveType()))
							break checkPrimitive1;
					}
					return true;
				}
				checkPrimitive2: if (r2.isPrimitiveType() && !r1.isPrimitiveType()) {
					for (int i = 0; i < results.length; i++) {
						// for all expressions (not for any expression not)
						if (!(
								(!results[i].isPolyExpression() && (results[i].resolvedType != null && !results[i].resolvedType.isPrimitiveType())) // standalone of a referencetype
								|| results[i].isPolyExpression()))	// or a poly
							break checkPrimitive2;
					}
					return true;
				}
			}
			return reduceAndIncorporate(ConstraintTypeFormula.create(r1, r2, ReductionResult.SUBTYPE));
		} else if (expri instanceof ReferenceExpression && ((ReferenceExpression)expri).isExactMethodReference()) {
			ReferenceExpression reference = (ReferenceExpression) expri;
			for (int i = 0; i < u.length; i++) {
				if (!reduceAndIncorporate(ConstraintTypeFormula.create(u[i], v[i], ReductionResult.SAME)))
					return false;
			}
			if (r2.id == TypeIds.T_void)
				return true;
			MethodBinding method = reference.getExactMethod();
			TypeBinding returnType = method.isConstructor() ? method.declaringClass : method.returnType;
			if (r1.isPrimitiveType() && !r2.isPrimitiveType() && returnType.isPrimitiveType())
				return true;
			if (r2.isPrimitiveType() && !r1.isPrimitiveType() && !returnType.isPrimitiveType())
				return true;
			return reduceAndIncorporate(ConstraintTypeFormula.create(r1, r2, ReductionResult.SUBTYPE));
		} else if (expri instanceof ConditionalExpression) {
			ConditionalExpression cond = (ConditionalExpression) expri;
			return  checkExpression(cond.valueIfTrue, u, r1, v, r2) && checkExpression(cond.valueIfFalse, u, r1, v, r2);
		} else if (expri instanceof SwitchExpression) {
			SwitchExpression se = (SwitchExpression) expri;
			for (Expression re : se.resultExpressions) {
				if (!checkExpression(re, u, r1, v, r2))
					return false;
			}
			return true;
		} else {
			return false;
		}
	}

	private boolean siSuperI(TypeBinding si, TypeBinding funcI) {
		if (TypeBinding.equalsEquals(si, funcI) || TypeBinding.equalsEquals(si.original(), funcI))
			return true;
		TypeBinding[] superIfcs = funcI.superInterfaces();
		if (superIfcs == null) return false;
		for (int i = 0; i < superIfcs.length; i++) {
			if (siSuperI(si, superIfcs[i].original()))
				return true;
		}
		return false;
	}

	private boolean siSubI(TypeBinding si, TypeBinding funcI) {
		if (TypeBinding.equalsEquals(si, funcI) || TypeBinding.equalsEquals(si.original(), funcI))
			return true;
		TypeBinding[] superIfcs = si.superInterfaces();
		if (superIfcs == null) return false;
		for (int i = 0; i < superIfcs.length; i++) {
			if (siSubI(superIfcs[i], funcI))
				return true;
		}
		return false;
	}

	// ========== Below this point: implementation of the generic algorithm: ==========

	/**
	 * Try to solve the inference problem defined by constraints and bounds previously registered.
	 * @return a bound set representing the solution, or null if inference failed
	 * @throws InferenceFailureException a compile error has been detected during inference
	 */
	public /*@Nullable*/ BoundSet solve(boolean inferringApplicability) throws InferenceFailureException {

		if (!reduce())
			return null;
		if (!this.currentBounds.incorporate(this))
			return null;
		if (inferringApplicability)
			this.b2 = this.currentBounds.copy(); // Preserve the result after reduction, without effects of resolve() for later use in invocation type inference.

		BoundSet solution = resolve(this.inferenceVariables);

		/* If inferring applicability make a final pass over the initial constraints preserved as final constraints to make sure they hold true at a macroscopic level.
		   See https://bugs.eclipse.org/bugs/show_bug.cgi?id=426537#c55 onwards.
		*/
		if (inferringApplicability && solution != null && this.finalConstraints != null) {
			for (ConstraintExpressionFormula constraint: this.finalConstraints) {
				if (constraint.left.isPolyExpression())
					continue; // avoid redundant re-inference, inner poly's own constraints get validated in its own context & poly invocation type inference proved compatibility against target.
				constraint.applySubstitution(solution, this.inferenceVariables);
				if (!this.currentBounds.reduceOneConstraint(this, constraint)) {
					return null;
				}
			}
		}
		return solution;
	}

	public /*@Nullable*/ BoundSet solve() throws InferenceFailureException {
		return solve(false);
	}

	public /*@Nullable*/ BoundSet solve(InferenceVariable[] toResolve) throws InferenceFailureException {
		if (!reduce())
			return null;
		if (!this.currentBounds.incorporate(this))
			return null;

		return resolve(toResolve);
	}

	/**
	 * JLS 18.2. reduce all initial constraints
	 * @throws InferenceFailureException
	 */
	private boolean reduce() throws InferenceFailureException {
		// Caution: This can be reentered recursively even as an earlier call is munching through the constraints !
		for (int i = 0; this.initialConstraints != null && i < this.initialConstraints.length; i++) {
			final ConstraintFormula currentConstraint = this.initialConstraints[i];
			if (currentConstraint == null)
				continue;
			this.initialConstraints[i] = null;
			if (!this.currentBounds.reduceOneConstraint(this, currentConstraint))
				return false;
		}
		this.initialConstraints = null;
		return true;
	}

	/**
	 * Have all inference variables been instantiated successfully?
	 */
	public boolean isResolved(BoundSet boundSet) {
		if (this.inferenceVariables != null) {
			for (int i = 0; i < this.inferenceVariables.length; i++) {
				if (!boundSet.isInstantiated(this.inferenceVariables[i]))
					return false;
			}
		}
		return true;
	}

	/**
	 * Retrieve the resolved solutions for all given type variables.
	 * @param typeParameters
	 * @param boundSet where instantiations are to be found
	 * @return array containing the substituted types or <code>null</code> elements for any type variable that could not be substituted.
	 */
	public TypeBinding /*@Nullable*/[] getSolutions(TypeVariableBinding[] typeParameters, InvocationSite site, BoundSet boundSet) {
		int len = typeParameters.length;
		TypeBinding[] substitutions = new TypeBinding[len];
		InferenceVariable[] outerVariables = null;
		if (this.outerContext != null && this.outerContext.stepCompleted < TYPE_INFERRED)
			outerVariables = this.outerContext.inferenceVariables;
		for (int i = 0; i < typeParameters.length; i++) {
			for (int j = 0; j < this.inferenceVariables.length; j++) {
				InferenceVariable variable = this.inferenceVariables[j];
				if (isSameSite(variable.site, site) && TypeBinding.equalsEquals(variable.typeParameter, typeParameters[i])) {
					TypeBinding outerVar = null;
					if (outerVariables != null && (outerVar = boundSet.getEquivalentOuterVariable(variable, outerVariables)) != null)
						substitutions[i] = outerVar;
					else
						substitutions[i] = boundSet.getInstantiation(variable, this.environment);
					break;
				}
			}
			if (substitutions[i] == null)
				return null;
		}
		return substitutions;
	}

	/** When inference produces a new constraint, reduce it to a suitable type bound and add the latter to the bound set. */
	public boolean reduceAndIncorporate(ConstraintFormula constraint) throws InferenceFailureException {
		return this.currentBounds.reduceOneConstraint(this, constraint); // TODO(SH): should we immediately call a diat incorporate, or can we simply wait for the next round?
	}

	/**
	 * <b>JLS 18.4</b> Resolution
	 * @return answer null if some constraint resolved to FALSE, otherwise the boundset representing the solution
	 * @throws InferenceFailureException
	 */
	private /*@Nullable*/ BoundSet resolve(InferenceVariable[] toResolve) throws InferenceFailureException {
		this.captureId = 0;
		// NOTE: 18.5.2 ...
		// "(While it was necessary to demonstrate that the inference variables in B1 could be resolved
		//   in order to establish applicability, the resulting instantiations are not considered part of B1.)
		// For this reason, resolve works on a temporary bound set, copied before any modification.
		BoundSet tmpBoundSet = this.currentBounds;
		if (this.inferenceVariables != null) {
			// find a minimal set of dependent variables:
			Set<InferenceVariable> variableSet;
			while ((variableSet = getSmallestVariableSet(tmpBoundSet, toResolve)) != null) {
				int oldNumUninstantiated = tmpBoundSet.numUninstantiatedVariables(this.inferenceVariables);
				final int numVars = variableSet.size();
				if (numVars > 0) {
					final InferenceVariable[] variables = variableSet.toArray(new InferenceVariable[numVars]);
					variables: if (!tmpBoundSet.hasCaptureBound(variableSet)) {
						// try to instantiate this set of variables in a fresh copy of the bound set:
						BoundSet prevBoundSet = tmpBoundSet;
						tmpBoundSet = tmpBoundSet.copy();
						for (int j = 0; j < variables.length; j++) {
							InferenceVariable variable = variables[j];
							// try lower bounds:
							TypeBinding[] lowerBounds = tmpBoundSet.lowerBounds(variable, true/*onlyProper*/);
							if (lowerBounds != Binding.NO_TYPES) {
								TypeBinding lub = this.scope.lowerUpperBound(lowerBounds);
								if (lub == TypeBinding.VOID || lub == null)
									return null;
								tmpBoundSet.addBound(new TypeBound(variable, lub, ReductionResult.SAME), this.environment);
							} else {
								TypeBinding[] upperBounds = tmpBoundSet.upperBounds(variable, true/*onlyProper*/);
								// check exception bounds:
								if (tmpBoundSet.inThrows.contains(variable.prototype()) && tmpBoundSet.hasOnlyTrivialExceptionBounds(variable, upperBounds)) {
									TypeBinding runtimeException = this.scope.getType(TypeConstants.JAVA_LANG_RUNTIMEEXCEPTION, 3);
									tmpBoundSet.addBound(new TypeBound(variable, runtimeException, ReductionResult.SAME), this.environment);
								} else {
									// try upper bounds:
									TypeBinding glb = this.object;
									if (upperBounds != Binding.NO_TYPES) {
										if (upperBounds.length == 1) {
											glb = upperBounds[0];
										} else {
											TypeBinding[] glbs = Scope.greaterLowerBound(upperBounds, this.scope, this.environment);
											if (glbs == null) {
												return null;
											} else if (glbs.length == 1) {
												glb = glbs[0];
											} else {
												glb = intersectionFromGlb(glbs);
												if (glb == null) {
													// inconsistent intersection
													tmpBoundSet = prevBoundSet; // clean up
													break variables; // and start over
												}
											}
										}
									}
									tmpBoundSet.addBound(new TypeBound(variable, glb, ReductionResult.SAME), this.environment);
								}
							}
						}
						if (tmpBoundSet.incorporate(this))
							continue;
						tmpBoundSet = prevBoundSet;// clean-up for second attempt
					}
					// Otherwise, a second attempt is made...
					Sorting.sortInferenceVariables(variables); // ensure stability of capture IDs
					final CaptureBinding18[] zs = new CaptureBinding18[numVars];
					for (int j = 0; j < numVars; j++)
						zs[j] = freshCapture(variables[j]);
					final BoundSet kurrentBoundSet = tmpBoundSet;
					Substitution theta = new Substitution() {
						@Override
						public LookupEnvironment environment() {
							return InferenceContext18.this.environment;
						}
						@Override
						public boolean isRawSubstitution() {
							return false;
						}
						@Override
						public TypeBinding substitute(TypeVariableBinding typeVariable) {
							for (int j = 0; j < numVars; j++)
								if (TypeBinding.equalsEquals(variables[j], typeVariable))
									return zs[j];
							/* If we have an instantiation, lower it to the instantiation. We don't want downstream abstractions to be confused about multiple versions of bounds without
							   and with instantiations propagated by incorporation. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=430686. There is no value whatsoever in continuing
							   to speak in two tongues. Also fixes https://bugs.eclipse.org/bugs/show_bug.cgi?id=425031.
							*/
							if (typeVariable instanceof InferenceVariable) {
								InferenceVariable inferenceVariable = (InferenceVariable) typeVariable;
								TypeBinding instantiation = kurrentBoundSet.getInstantiation(inferenceVariable, null);
								if (instantiation != null)
									return instantiation;
							}
							return typeVariable;
						}
					};
					for (int j = 0; j < numVars; j++) {
						InferenceVariable variable = variables[j];
						CaptureBinding18 zsj = zs[j];
						// add lower bounds:
						TypeBinding[] lowerBounds = tmpBoundSet.lowerBounds(variable, true/*onlyProper*/);
						if (lowerBounds != Binding.NO_TYPES) {
							TypeBinding lub = this.scope.lowerUpperBound(lowerBounds);
							if (lub != TypeBinding.VOID && lub != null)
								zsj.lowerBound = lub;
						}
						// add upper bounds:
						TypeBinding[] upperBounds = tmpBoundSet.upperBounds(variable, false/*onlyProper*/);
						if (upperBounds != Binding.NO_TYPES) {
							for (int k = 0; k < upperBounds.length; k++)
								upperBounds[k] = Scope.substitute(theta, upperBounds[k]);
							if (!setUpperBounds(zsj, upperBounds))
								continue; // at violation of well-formedness skip this candidate and proceed
						}
						if (tmpBoundSet == this.currentBounds)
							tmpBoundSet = tmpBoundSet.copy();
						Iterator<ParameterizedTypeBinding> captureKeys = tmpBoundSet.captures.keySet().iterator();
						Set<ParameterizedTypeBinding> toRemove = new LinkedHashSet<ParameterizedTypeBinding>();
						while (captureKeys.hasNext()) {
							ParameterizedTypeBinding key = captureKeys.next();
							int len = key.arguments.length;
							for (int i = 0; i < len; i++) {
								if (TypeBinding.equalsEquals(key.arguments[i], variable)) {
									toRemove.add(key);
									break;
								}
							}
						}
						captureKeys = toRemove.iterator();
						while (captureKeys.hasNext())
							tmpBoundSet.captures.remove(captureKeys.next());
						tmpBoundSet.addBound(new TypeBound(variable, zsj, ReductionResult.SAME), this.environment);
					}
					if (tmpBoundSet.incorporate(this)) {
						if (tmpBoundSet.numUninstantiatedVariables(this.inferenceVariables) == oldNumUninstantiated)
							return null; // abort because we made no progress
						continue;
					}
					return null;
				}
			}
		}
		return tmpBoundSet;
	}

	private TypeBinding intersectionFromGlb(TypeBinding[] glbs) {
		ReferenceBinding[] refGlbs = new ReferenceBinding[glbs.length];
		for (int i = 0; i < glbs.length; i++) {
			TypeBinding typeBinding = glbs[i];
			if (typeBinding instanceof ReferenceBinding) {
				refGlbs[i] = (ReferenceBinding) typeBinding;
			} else {
				return null;
			}
		}
		IntersectionTypeBinding18 intersection = (IntersectionTypeBinding18) this.environment.createIntersectionType18(refGlbs);
		if (ReferenceBinding.isConsistentIntersection(intersection.intersectingTypes))
			return intersection;
		return null;
	}

	int captureId = 0;

	/** For 18.4: "Let Z1, ..., Zn be fresh type variables" use capture bindings. */
	private CaptureBinding18 freshCapture(InferenceVariable variable) {
		int id = this.captureId++;
		char[] sourceName = CharOperation.concat("Z".toCharArray(), '#', String.valueOf(id).toCharArray(), '-', variable.sourceName); //$NON-NLS-1$
		int start = this.currentInvocation != null ? this.currentInvocation.sourceStart() : 0;
		int end = this.currentInvocation != null ? this.currentInvocation.sourceEnd() : 0;
		return new CaptureBinding18(this.scope.enclosingSourceType(), sourceName, variable.typeParameter.shortReadableName(),
						start, end, id, this.environment);
	}
	// === ===

	private boolean setUpperBounds(CaptureBinding18 typeVariable, TypeBinding[] substitutedUpperBounds) {
		// 18.4: ... define the upper bound of Zi as glb(L1θ, ..., Lkθ)
		if (substitutedUpperBounds.length == 1) {
			return typeVariable.setUpperBounds(substitutedUpperBounds, this.object); // shortcut
		} else {
			TypeBinding[] glbs = Scope.greaterLowerBound(substitutedUpperBounds, this.scope, this.environment);
			if (glbs == null)
				return false;
			if (typeVariable.lowerBound != null) {
				for (int i = 0; i < glbs.length; i++) {
					if (!typeVariable.lowerBound.isCompatibleWith(glbs[i]))
						return false; // not well-formed
				}
			}
			// for deterministic results sort this array by id:
			sortTypes(glbs);
			if (!typeVariable.setUpperBounds(glbs, this.object))
				return false;
		}
		return true;
	}

	static void sortTypes(TypeBinding[] types) {
		Arrays.sort(types, new Comparator<TypeBinding>() {
			@Override
			public int compare(TypeBinding o1, TypeBinding o2) {
				int i1 = o1.id, i2 = o2.id;
				return (i1<i2 ? -1 : (i1==i2 ? 0 : 1));
			}
		});
	}

	/**
	 * Find the smallest set of uninstantiated inference variables not depending
	 * on any uninstantiated variable outside the set.
	 */
	private Set<InferenceVariable> getSmallestVariableSet(BoundSet bounds, InferenceVariable[] subSet) {
		// "Given a set of inference variables to resolve, let V be the union of this set and
		//  all variables upon which the resolution of at least one variable in this set depends."
		Set<InferenceVariable> v = new LinkedHashSet<InferenceVariable>();
		Map<InferenceVariable,Set<InferenceVariable>> dependencies = new HashMap<>(); // compute only once, store for the final loop over 'v'.
		for (InferenceVariable iv : subSet) {
			Set<InferenceVariable> tmp = new LinkedHashSet<>();
			addDependencies(bounds, tmp, iv);
			dependencies.put(iv, tmp);
			v.addAll(tmp);
		}
		// "If every variable in V has an instantiation, then resolution succeeds and this procedure terminates."
		//  -> (implicit if result remains unassigned)
		// "Otherwise, let { α1, ..., αn } be a non-empty subset of uninstantiated variables in V such that ...
		int min = Integer.MAX_VALUE;
		Set<InferenceVariable> result = null;
		// "i) for all i (1 ≤ i ≤ n), ..."
		for (InferenceVariable currentVariable : v) {
			if (!bounds.isInstantiated(currentVariable)) {
				// "... if αi depends on the resolution of a variable β, then either β has an instantiation or there is some j such that β = αj; ..."
				Set<InferenceVariable> set = dependencies.get(currentVariable);
				if (set == null) // not an element of the original subSet, still need to fetch this var's dependencies
					addDependencies(bounds, set = new LinkedHashSet<>(), currentVariable);
				//  "... and ii) there exists no non-empty proper subset of { α1, ..., αn } with this property."
				int cur = set.size();
				if (cur == 1)
					return set; // won't get smaller
				if (cur < min) {
					result = set;
					min = cur;
				}
			}
		}
		return result;
	}

	private void addDependencies(BoundSet boundSet, Set<InferenceVariable> variableSet, InferenceVariable currentVariable) {
		if (boundSet.isInstantiated(currentVariable)) return; // not added
		if (!variableSet.add(currentVariable)) return; // already present
		for (int j = 0; j < this.inferenceVariables.length; j++) {
			InferenceVariable nextVariable = this.inferenceVariables[j];
			if (TypeBinding.equalsEquals(nextVariable, currentVariable)) continue;
			if (boundSet.dependsOnResolutionOf(currentVariable, nextVariable))
				addDependencies(boundSet, variableSet, nextVariable);
		}
	}

	private ConstraintFormula pickFromCycle(Set<ConstraintFormula> c) {
		// Detail from 18.5.2 bullet 6.1

		// Note on performance: this implementation could quite possibly be optimized a lot.
		// However, we only *very rarely* reach here,
		// so nobody should really be affected by the performance penalty paid here.

		// Note on spec conformance: the spec seems to require _all_ criteria (i)-(iv) to be fulfilled
		// with the sole exception of (iii), which should only be used, if _any_ constraints matching (i) & (ii)
		// also fulfill this condition.
		// Experiments, however, show that strict application of the above is prone to failing to pick any constraint,
		// causing non-termination of the algorithm.
		// Since that is not acceptable, I'm *interpreting* the spec to request a search for a constraint
		// that "best matches" the given conditions.

		// collect all constraints participating in a cycle
		HashMap<ConstraintFormula,Set<ConstraintFormula>> dependencies = new HashMap<ConstraintFormula, Set<ConstraintFormula>>();
		Set<ConstraintFormula> cycles = new LinkedHashSet<ConstraintFormula>();
		for (ConstraintFormula constraint : c) {
			Collection<InferenceVariable> infVars = constraint.inputVariables(this);
			for (ConstraintFormula other : c) {
				if (other == constraint) continue;
				if (dependsOn(infVars, other.outputVariables(this))) {
					// found a dependency, record it:
					Set<ConstraintFormula> targetSet = dependencies.get(constraint);
					if (targetSet == null)
						dependencies.put(constraint, targetSet = new LinkedHashSet<ConstraintFormula>());
					targetSet.add(other);
					// look for a cycle:
					Set<ConstraintFormula> nodesInCycle = new LinkedHashSet<ConstraintFormula>();
					if (isReachable(dependencies, other, constraint, new LinkedHashSet<ConstraintFormula>(), nodesInCycle)) {
						// found a cycle, record the involved nodes:
						cycles.addAll(nodesInCycle);
					}
				}
			}
		}
		Set<ConstraintFormula> outside = new LinkedHashSet<ConstraintFormula>(c);
		outside.removeAll(cycles);

		Set<ConstraintFormula> candidatesII = new LinkedHashSet<ConstraintFormula>();
		// (i): participates in a cycle:
		candidates: for (ConstraintFormula candidate : cycles) {
			Collection<InferenceVariable> infVars = candidate.inputVariables(this);
			// (ii) does not depend on any constraints outside the cycle
			for (ConstraintFormula out : outside) {
				if (dependsOn(infVars, out.outputVariables(this)))
					continue candidates;
			}
			candidatesII.add(candidate);
		}
		if (candidatesII.isEmpty())
			candidatesII = c; // not spec'ed but needed to avoid returning null below, witness: java.util.stream.Collectors

		// tentatively: (iii)  has the form ⟨Expression → T⟩
		Set<ConstraintFormula> candidatesIII = new LinkedHashSet<ConstraintFormula>();
		for (ConstraintFormula candidate : candidatesII) {
			if (candidate instanceof ConstraintExpressionFormula)
				candidatesIII.add(candidate);
		}
		if (candidatesIII.isEmpty()) {
			candidatesIII = candidatesII; // no constraint fulfills (iii) -> ignore this condition
		} else { // candidatesIII contains all relevant constraints ⟨Expression → T⟩
			// (iv) contains an expression that appears to the left of the expression
			// 		of every other constraint satisfying the previous three requirements

			// collect containment info regarding all expressions in candidate constraints:
			// (a) find minimal enclosing expressions:
			Map<ConstraintExpressionFormula,ConstraintExpressionFormula> expressionContainedBy = new LinkedHashMap<ConstraintExpressionFormula, ConstraintExpressionFormula>();
			for (ConstraintFormula one : candidatesIII) {
				ConstraintExpressionFormula oneCEF = (ConstraintExpressionFormula) one;
				Expression exprOne = oneCEF.left;
				for (ConstraintFormula two : candidatesIII) {
					if (one == two) continue;
					ConstraintExpressionFormula twoCEF = (ConstraintExpressionFormula) two;
					Expression exprTwo = twoCEF.left;
					if (doesExpressionContain(exprOne, exprTwo)) {
						ConstraintExpressionFormula previous = expressionContainedBy.get(two);
						if (previous == null || doesExpressionContain(previous.left, exprOne)) // only if improving
							expressionContainedBy.put(twoCEF, oneCEF);
					}
				}
			}
			// (b) build the tree from the above
			Map<ConstraintExpressionFormula,Set<ConstraintExpressionFormula>> containmentForest = new LinkedHashMap<ConstraintExpressionFormula, Set<ConstraintExpressionFormula>>();
			for (Map.Entry<ConstraintExpressionFormula, ConstraintExpressionFormula> parentRelation : expressionContainedBy.entrySet()) {
				ConstraintExpressionFormula parent = parentRelation.getValue();
				Set<ConstraintExpressionFormula> children = containmentForest.get(parent);
				if (children == null)
					containmentForest.put(parent, children = new LinkedHashSet<ConstraintExpressionFormula>());
				children.add(parentRelation.getKey());
			}

			// approximate the spec by searching the largest containment tree:
			int bestRank = -1;
			ConstraintExpressionFormula candidate = null;
			for (ConstraintExpressionFormula parent : containmentForest.keySet()) {
				int rank = rankNode(parent, expressionContainedBy, containmentForest);
				if (rank > bestRank) {
					bestRank = rank;
					candidate = parent;
				}
			}
			if (candidate != null)
				return candidate;
		}

		if (candidatesIII.isEmpty())
			throw new IllegalStateException("cannot pick constraint from cyclic set"); //$NON-NLS-1$
		return candidatesIII.iterator().next();
	}

	/**
	 * Does the first constraint depend on the other?
	 * The first constraint is represented by its input variables and the other constraint by its output variables.
	 */
	private boolean dependsOn(Collection<InferenceVariable> inputsOfFirst, Collection<InferenceVariable> outputsOfOther) {
		for (InferenceVariable iv : inputsOfFirst) {
			for (InferenceVariable otherIV : outputsOfOther)
				if (this.currentBounds.dependsOnResolutionOf(iv, otherIV))
					return true;
		}
		return false;
	}

	/** Does 'deps' contain a chain of dependencies leading from 'from' to 'to'? */
	private boolean isReachable(Map<ConstraintFormula,Set<ConstraintFormula>> deps, ConstraintFormula from, ConstraintFormula to,
			Set<ConstraintFormula> nodesVisited, Set<ConstraintFormula> nodesInCycle)
	{
		if (from == to) {
			nodesInCycle.add(from);
			return true;
		}
		if (!nodesVisited.add(from))
			return false;
		Set<ConstraintFormula> targetSet = deps.get(from);
		if (targetSet != null) {
			for (ConstraintFormula tgt : targetSet) {
				if (isReachable(deps, tgt, to, nodesVisited, nodesInCycle)) {
					nodesInCycle.add(from);
					return true;
				}
			}
		}
		return false;
	}

	/** Does exprOne lexically contain exprTwo? */
	private boolean doesExpressionContain(Expression exprOne, Expression exprTwo) {
		if (exprTwo.sourceStart > exprOne.sourceStart) {
			return exprTwo.sourceEnd <= exprOne.sourceEnd;
		} else if (exprTwo.sourceStart == exprOne.sourceStart) {
			return exprTwo.sourceEnd < exprOne.sourceEnd;
		}
		return false;
	}

	/** non-roots answer -1, roots answer the size of the spanned tree */
	private int rankNode(ConstraintExpressionFormula parent,
			Map<ConstraintExpressionFormula,ConstraintExpressionFormula> expressionContainedBy,
			Map<ConstraintExpressionFormula, Set<ConstraintExpressionFormula>> containmentForest)
	{
		if (expressionContainedBy.get(parent) != null)
			return -1; // not a root
		Set<ConstraintExpressionFormula> children = containmentForest.get(parent);
		if (children == null)
			return 1; // unconnected node or leaf
		int sum = 1;
		for (ConstraintExpressionFormula child : children) {
			int cRank = rankNode(child, expressionContainedBy, containmentForest);
			if (cRank > 0)
				sum += cRank;
		}
		return sum;
	}

	private Set<ConstraintFormula> findBottomSet(Set<ConstraintFormula> constraints,
			Set<InferenceVariable> allOutputVariables, List<Set<InferenceVariable>> components)
	{
		// 18.5.2 bullet 5.(1)
		//  A subset of constraints is selected, satisfying the property that,
		//  for each constraint, no input variable can influence an output variable of another constraint in C. ...
		//  An inference variable α can influence an inference variable β if α depends on the resolution of β (§18.4), or vice versa;
		//  or if there exists a third inference variable γ such that α can influence γ and γ can influence β.  ...
		Set<ConstraintFormula> result = new LinkedHashSet<ConstraintFormula>();
	  constraintLoop:
		for (ConstraintFormula constraint : constraints) {
			for (InferenceVariable in : constraint.inputVariables(this)) {
				if (canInfluenceAnyOf(in, allOutputVariables, components))
					continue constraintLoop;
			}
			result.add(constraint);
		}
		return result;
	}

	private boolean canInfluenceAnyOf(InferenceVariable in, Set<InferenceVariable> allOuts, List<Set<InferenceVariable>> components) {
		// can influence == lives in the same component
		for (Set<InferenceVariable> component : components) {
			if (component.contains(in)) {
				for (InferenceVariable out : allOuts)
					if (component.contains(out))
						return true;
				return false;
			}
		}
		return false;
	}

	Set<InferenceVariable> allOutputVariables(Set<ConstraintFormula> constraints) {
		Set<InferenceVariable> result = new LinkedHashSet<InferenceVariable>();
		Iterator<ConstraintFormula> it = constraints.iterator();
		while (it.hasNext()) {
			result.addAll(it.next().outputVariables(this));
		}
		return result;
	}

	private TypeBinding[] varArgTypes(TypeBinding[] parameters, int k) {
		TypeBinding[] types = new TypeBinding[k];
		int declaredLength = parameters.length-1;
		System.arraycopy(parameters, 0, types, 0, declaredLength);
		TypeBinding last = ((ArrayBinding)parameters[declaredLength]).elementsType();
		for (int i = declaredLength; i < k; i++)
			types[i] = last;
		return types;
	}

	public SuspendedInferenceRecord enterPolyInvocation(InvocationSite invocation, Expression[] innerArguments) {
		SuspendedInferenceRecord record = new SuspendedInferenceRecord(this.currentInvocation, this.invocationArguments, this.inferenceVariables, this.inferenceKind, this.usesUncheckedConversion);
		this.inferenceVariables = null;
		this.invocationArguments = innerArguments;
		this.currentInvocation = invocation;
		this.usesUncheckedConversion = false;
		return record;
	}

	public SuspendedInferenceRecord enterLambda(LambdaExpression lambda) {
		SuspendedInferenceRecord record = new SuspendedInferenceRecord(this.currentInvocation, this.invocationArguments, this.inferenceVariables, this.inferenceKind, this.usesUncheckedConversion);
		this.inferenceVariables = null;
		this.invocationArguments = null;
		this.usesUncheckedConversion = false;
		return record;
	}

	public void integrateInnerInferenceB2(InferenceContext18 innerCtx) {
		this.currentBounds.addBounds(innerCtx.b2, this.environment);
		this.inferenceVariables = innerCtx.inferenceVariables;
		this.inferenceKind = innerCtx.inferenceKind;
		if (!isSameSite(innerCtx.currentInvocation, this.currentInvocation))
			innerCtx.outerContext = this;
		this.usesUncheckedConversion = innerCtx.usesUncheckedConversion;
	}

	public void resumeSuspendedInference(SuspendedInferenceRecord record, InferenceContext18 innerContext) {
		// merge inference variables:
		boolean firstTime = collectInnerContext(innerContext);
		if (this.inferenceVariables == null) { // no new ones, assume we aborted prematurely
			this.inferenceVariables = record.inferenceVariables;
		} else if(!firstTime) {
			// Use a set to eliminate duplicates.
			final Set<InferenceVariable> uniqueVariables = new LinkedHashSet<>();
			uniqueVariables.addAll(Arrays.asList(record.inferenceVariables));
			uniqueVariables.addAll(Arrays.asList(this.inferenceVariables));
			this.inferenceVariables = uniqueVariables.toArray(new InferenceVariable[uniqueVariables.size()]);
		} else {
			int l1 = this.inferenceVariables.length;
			int l2 = record.inferenceVariables.length;
			System.arraycopy(this.inferenceVariables, 0, this.inferenceVariables=new InferenceVariable[l1+l2], l2, l1);
			System.arraycopy(record.inferenceVariables, 0, this.inferenceVariables, 0, l2);
		}

		// replace invocation site & arguments:
		this.currentInvocation = record.site;
		this.invocationArguments = record.invocationArguments;
		this.inferenceKind = record.inferenceKind;
		this.usesUncheckedConversion = record.usesUncheckedConversion;
	}

	private boolean collectInnerContext(final InferenceContext18 innerContext) {
		if(innerContext == null) {
			return false;
		}
		if(this.seenInnerContexts == null) {
			this.seenInnerContexts = new LinkedHashSet<>();
		}
		return this.seenInnerContexts.add(innerContext);
	}

	private Substitution getResultSubstitution(final BoundSet result) {
		return new Substitution() {
			@Override
			public LookupEnvironment environment() {
				return InferenceContext18.this.environment;
			}
			@Override
			public boolean isRawSubstitution() {
				return false;
			}
			@Override
			public TypeBinding substitute(TypeVariableBinding typeVariable) {
				if (typeVariable instanceof InferenceVariable) {
					TypeBinding instantiation = result.getInstantiation((InferenceVariable) typeVariable, InferenceContext18.this.environment);
					if (instantiation != null)
						return instantiation;
				}
				return typeVariable;
			}
		};
	}

	public boolean isVarArgs() {
		return this.inferenceKind == CHECK_VARARG;
	}

	/**
	 * Retrieve the rank'th parameter, possibly respecting varargs invocation, see 15.12.2.4.
	 * Returns null if out of bounds and CHECK_VARARG was not requested.
	 * Precondition: isVarArgs implies method.isVarargs()
	 */
	public static TypeBinding getParameter(TypeBinding[] parameters, int rank, boolean isVarArgs) {
		if (isVarArgs) {
			if (rank >= parameters.length-1)
				return ((ArrayBinding)parameters[parameters.length-1]).elementsType();
		} else if (rank >= parameters.length) {
			return null;
		}
		return parameters[rank];
	}

	/**
	 * Create a problem method signaling failure of invocation type inference,
	 * unless the given candidate is tolerable to be compatible with buggy javac.
	 */
	public MethodBinding getReturnProblemMethodIfNeeded(TypeBinding expectedType, MethodBinding method) {
		if (InferenceContext18.SIMULATE_BUG_JDK_8026527 && expectedType != null
				&& !(method.original() instanceof SyntheticFactoryMethodBinding)
				&& (method.returnType instanceof ReferenceBinding || method.returnType instanceof ArrayBinding)) {
			if (!expectedType.isProperType(true))
				return null; // not ready
			if (this.environment.convertToRawType(method.returnType.erasure(), false).isCompatibleWith(expectedType))
				return method; // don't count as problem.
		}
		/* We used to check if expected type is null and if so return method, but that is wrong - it injects an incompatible method into overload resolution.
		   if we get here with expected type set to null at all, the target context does not define a target type (vanilla context), so inference has done its
		   best and nothing more to do than to signal error.
		 */
		ProblemMethodBinding problemMethod = new ProblemMethodBinding(method, method.selector, method.parameters, ProblemReasons.InvocationTypeInferenceFailure);
		problemMethod.returnType = expectedType != null ? expectedType : method.returnType;
		problemMethod.inferenceContext = this;
		return problemMethod;
	}

	// debugging:
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("Inference Context"); //$NON-NLS-1$
		switch (this.stepCompleted) {
			case NOT_INFERRED: buf.append(" (initial)");break; //$NON-NLS-1$
			case APPLICABILITY_INFERRED: buf.append(" (applicability inferred)");break; //$NON-NLS-1$
			case TYPE_INFERRED: buf.append(" (type inferred)");break; //$NON-NLS-1$
			case TYPE_INFERRED_FINAL: buf.append(" (type inferred final)");break; //$NON-NLS-1$
		}
		switch (this.inferenceKind) {
			case CHECK_STRICT: buf.append(" (strict)");break; //$NON-NLS-1$
			case CHECK_LOOSE: buf.append(" (loose)");break; //$NON-NLS-1$
			case CHECK_VARARG: buf.append(" (vararg)");break; //$NON-NLS-1$
		}
		if (this.currentBounds != null && isResolved(this.currentBounds))
			buf.append(" (resolved)"); //$NON-NLS-1$
		buf.append('\n');
		if (this.inferenceVariables != null) {
			buf.append("Inference Variables:\n"); //$NON-NLS-1$
			for (int i = 0; i < this.inferenceVariables.length; i++) {
				buf.append('\t').append(this.inferenceVariables[i].sourceName).append("\t:\t"); //$NON-NLS-1$
				if (this.currentBounds != null && this.currentBounds.isInstantiated(this.inferenceVariables[i]))
					buf.append(this.currentBounds.getInstantiation(this.inferenceVariables[i], this.environment).readableName());
				else
					buf.append("NOT INSTANTIATED"); //$NON-NLS-1$
				buf.append('\n');
			}
		}
		if (this.initialConstraints != null) {
			buf.append("Initial Constraints:\n"); //$NON-NLS-1$
			for (int i = 0; i < this.initialConstraints.length; i++)
				if (this.initialConstraints[i] != null)
					buf.append('\t').append(this.initialConstraints[i].toString()).append('\n');
		}
		if (this.currentBounds != null)
			buf.append(this.currentBounds.toString());
		return buf.toString();
	}

	/**
	 * If 'type' is a parameterized type and one of its arguments is a wildcard answer the casted type, else null.
	 * A nonnull answer is ensured to also have nonnull arguments.
	 */
	public static ParameterizedTypeBinding parameterizedWithWildcard(TypeBinding type) {
		if (type == null || type.kind() != Binding.PARAMETERIZED_TYPE)
			return null;
		ParameterizedTypeBinding parameterizedType = (ParameterizedTypeBinding) type;
		TypeBinding[] arguments = parameterizedType.arguments;
		if (arguments != null) {
			for (int i = 0; i < arguments.length; i++)
				if (arguments[i].isWildcard())
					return parameterizedType;
		}
		return null;
	}

	public TypeBinding[] getFunctionInterfaceArgumentSolutions(TypeBinding[] a) {
		int m = a.length;
		TypeBinding[] aprime = new TypeBinding[m];
		for (int i = 0; i < this.inferenceVariables.length; i++) {
			InferenceVariable alphai = this.inferenceVariables[i];
			TypeBinding t = this.currentBounds.getInstantiation(alphai, this.environment);
			if (t != null)
				aprime[i] = t;
			else
				aprime[i] = a[i];
		}
		return aprime;
	}

	/** Record the fact that the given constraint requires unchecked conversion. */
	public void recordUncheckedConversion(ConstraintTypeFormula constraint) {
		if (this.constraintsWithUncheckedConversion == null)
			this.constraintsWithUncheckedConversion = new ArrayList<ConstraintFormula>();
		this.constraintsWithUncheckedConversion.add(constraint);
		this.usesUncheckedConversion = true;
	}

	void reportUncheckedConversions(BoundSet solution) {
		if (this.constraintsWithUncheckedConversion != null) {
			int len = this.constraintsWithUncheckedConversion.size();
			Substitution substitution = getResultSubstitution(solution);
			for (int i = 0; i < len; i++) {
				ConstraintTypeFormula constraint = (ConstraintTypeFormula) this.constraintsWithUncheckedConversion.get(i);
				TypeBinding expectedType = constraint.right;
				TypeBinding providedType = constraint.left;
				if (!expectedType.isProperType(true)) {
					expectedType = Scope.substitute(substitution, expectedType);
				}
				if (!providedType.isProperType(true)) {
					providedType = Scope.substitute(substitution, providedType);
				}
/* FIXME(stephan): enable once we solved:
                    (a) avoid duplication with traditional reporting
                    (b) improve location to report against
				if (this.currentInvocation instanceof Expression)
					this.scope.problemReporter().unsafeTypeConversion((Expression) this.currentInvocation, providedType, expectedType);
 */
			}
		}
	}

	/** For use by 15.12.2.6 Method Invocation Type */
	public boolean usesUncheckedConversion() {
		return this.constraintsWithUncheckedConversion != null;
	}

	// INTERIM: infrastructure for detecting failures caused by specific known incompleteness:
	public static void missingImplementation(String msg) {
		throw new UnsupportedOperationException(msg);
	}

	public void forwardResults(BoundSet result, Invocation invocation, ParameterizedMethodBinding pmb, TypeBinding targetType) {
		if (targetType != null)
			invocation.registerResult(targetType, pmb);
		Expression[] arguments = invocation.arguments();
		for (int i = 0, length = arguments == null ? 0 : arguments.length; i < length; i++) {
			Expression [] expressions = arguments[i].getPolyExpressions();
			for (int j = 0, jLength = expressions.length; j < jLength; j++) {
				Expression expression = expressions[j];
				if (!(expression instanceof Invocation))
					continue;
				Invocation polyInvocation = (Invocation) expression;
				MethodBinding binding = polyInvocation.binding();
				if (binding == null || !binding.isValidBinding())
					continue;
				ParameterizedMethodBinding methodSubstitute = null;
				if (binding instanceof ParameterizedGenericMethodBinding) {
					MethodBinding shallowOriginal = binding.shallowOriginal();
					TypeBinding[] solutions = getSolutions(shallowOriginal.typeVariables(), polyInvocation, result);
					if (solutions == null)  // in CEF.reduce, we lift inner poly expressions into outer context only if their target type has inference variables.
						continue;
					methodSubstitute = this.environment.createParameterizedGenericMethod(shallowOriginal, solutions);
				} else {
					if (!binding.isConstructor() || !(binding instanceof ParameterizedMethodBinding))
						continue; // throw ISE ?
					MethodBinding shallowOriginal = binding.shallowOriginal();
					ReferenceBinding genericType = shallowOriginal.declaringClass;
					TypeBinding[] solutions = getSolutions(genericType.typeVariables(), polyInvocation, result);
					if (solutions == null)  // in CEF.reduce, we lift inner poly expressions into outer context only if their target type has inference variables.
						continue;
					ParameterizedTypeBinding parameterizedType = this.environment.createParameterizedType(genericType, solutions, binding.declaringClass.enclosingType());
					for (MethodBinding parameterizedMethod : parameterizedType.methods()) {
						if (parameterizedMethod.original() == shallowOriginal) {
							methodSubstitute = (ParameterizedMethodBinding) parameterizedMethod;
							break;
						}
					}
				}
				if (methodSubstitute == null || !methodSubstitute.isValidBinding())
					continue;
				boolean variableArity = pmb.isVarargs();
				final TypeBinding[] parameters = pmb.parameters;
				if (variableArity && parameters.length == arguments.length && i == length - 1) {
					TypeBinding returnType = methodSubstitute.returnType.capture(this.scope, expression.sourceStart, expression.sourceEnd);
					if (returnType.isCompatibleWith(parameters[parameters.length - 1], this.scope)) {
						variableArity = false;
					}
				}
				TypeBinding parameterType = InferenceContext18.getParameter(parameters, i, variableArity);
				forwardResults(result, polyInvocation, methodSubstitute, parameterType);
			}
		}
	}

	public void cleanUp() {
		this.b2 = null;
		this.currentBounds = null;
	}
}
