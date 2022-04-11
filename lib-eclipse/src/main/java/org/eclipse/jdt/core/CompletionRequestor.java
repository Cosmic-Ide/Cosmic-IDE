/*******************************************************************************
 * Copyright (c) 2004, 2019 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.core;

import org.eclipse.jdt.core.compiler.IProblem;

/**
 * Abstract base class for a completion requestor which is passed completion
 * proposals as they are generated in response to a code assist request.
 * <p>
 * This class is intended to be subclassed by clients.
 * </p>
 * <p>
 * The code assist engine normally invokes methods on completion
 * requestor in the following sequence:
 * <pre>
 * requestor.beginReporting();
 * requestor.acceptContext(context);
 * requestor.accept(proposal_1);
 * requestor.accept(proposal_2);
 * ...
 * requestor.endReporting();
 * </pre>
 * <p>
 * If, however, the engine is unable to offer completion proposals
 * for whatever reason, <code>completionFailure</code> is called
 * with a problem object describing why completions were unavailable.
 * In this case, the sequence of calls is:
 * <pre>
 * requestor.beginReporting();
 * requestor.acceptContext(context);
 * requestor.completionFailure(problem);
 * requestor.endReporting();
 * </pre>
 * <p>
 * In either case, the bracketing <code>beginReporting</code>
 * <code>endReporting</code> calls are always made as well as
 * <code>acceptContext</code> call.
 * </p>
 * <p>
 * The class was introduced in 3.0 as a more evolvable replacement
 * for the <code>ICompletionRequestor</code> interface.
 * </p>
 *
 * @see ICodeAssist
 * @since 3.0
 */
public abstract class CompletionRequestor {

	/**
	 * The set of CompletionProposal kinds that this requestor
	 * ignores; <code>0</code> means the set is empty.
	 * 1 << completionProposalKind
	 */
	private int ignoreSet = 0;

	private String[] favoriteReferences;

	/**
	 * The set of CompletionProposal kinds that this requestor
	 * allows for required proposals; <code>0</code> means the set is empty.
	 * 1 << completionProposalKind
	 */
	private int requiredProposalAllowSet[] = null;

	private boolean requireExtendedContext = false;

	/**
	 * Creates a new completion requestor.
	 * The requestor is interested in all kinds of completion
	 * proposals; none will be ignored.
	 *
	 * Calls to this constructor are identical to calls to <code>CompletionRequestor(false)</code>
	 */
	public CompletionRequestor() {
		this(false);
	}

	/**
	 * Creates a new completion requestor.
	 * If <code>ignoreAll</code> is <code>true</code> the requestor is not interested in
	 * all kinds of completion proposals; all will be ignored. For each kind of completion proposals
	 * that is of interest, <code>setIgnored(kind, false)</code> must be called.
	 * If <code>ignoreAll</code> is <code>false</code> the requestor is interested in
	 * all kinds of completion proposals; none will be ignored.
	 *
	 * @param ignoreAll <code>true</code> to ignore all kinds of completion proposals,
	 * and <code>false</code> to propose all kinds
	 *
	 * @since 3.4
	 */
	public CompletionRequestor(boolean ignoreAll) {
		this.ignoreSet = ignoreAll ? 0xffffffff : 0x00000000;
	}

	/**
	 * Returns whether the given kind of completion proposal is ignored.
	 *
	 * @param completionProposalKind one of the kind constants declared
	 * on <code>CompletionProposal</code>
	 * @return <code>true</code> if the given kind of completion proposal
	 * is ignored by this requestor, and <code>false</code> if it is of
	 * interest
	 * @see #setIgnored(int, boolean)
	 * @see CompletionProposal#getKind()
	 */
	public boolean isIgnored(int completionProposalKind) {
		if (completionProposalKind < CompletionProposal.FIRST_KIND
			|| completionProposalKind > CompletionProposal.LAST_KIND) {
				throw new IllegalArgumentException("Unknown kind of completion proposal: "+completionProposalKind); //$NON-NLS-1$
		}
		return 0 != (this.ignoreSet & (1 << completionProposalKind));
	}

	/**
	 * Sets whether the given kind of completion proposal is ignored.
	 *
	 * @param completionProposalKind one of the kind constants declared
	 * on <code>CompletionProposal</code>
	 * @param ignore <code>true</code> if the given kind of completion proposal
	 * is ignored by this requestor, and <code>false</code> if it is of
	 * interest
	 * @see #isIgnored(int)
	 * @see CompletionProposal#getKind()
	 */
	public void setIgnored(int completionProposalKind, boolean ignore) {
		if (completionProposalKind < CompletionProposal.FIRST_KIND
			|| completionProposalKind > CompletionProposal.LAST_KIND) {
				throw new IllegalArgumentException("Unknown kind of completion proposal: "+completionProposalKind); //$NON-NLS-1$
		}
		if (ignore) {
			this.ignoreSet |= (1 << completionProposalKind);
		} else {
			this.ignoreSet &= ~(1 << completionProposalKind);
		}
	}

	/**
	 * Returns whether a proposal of a given kind with a required proposal
	 * of the given kind is allowed.
	 *
	 * @param proposalKind one of the kind constants declared
	 * @param requiredProposalKind one of the kind constants declared
	 * on <code>CompletionProposal</code>
	 * @return <code>true</code> if a proposal of a given kind with a required proposal
	 * of the given kind is allowed by this requestor, and <code>false</code>
	 * if it isn't of interest.
	 * <p>
	 * By default, all kinds of required proposals aren't allowed.
	 * </p>
	 * @see #setAllowsRequiredProposals(int, int, boolean)
	 * @see CompletionProposal#getKind()
	 * @see CompletionProposal#getRequiredProposals()
	 *
	 * @since 3.3
	 */
	public boolean isAllowingRequiredProposals(int proposalKind, int requiredProposalKind) {
		if (proposalKind < CompletionProposal.FIRST_KIND
			|| proposalKind > CompletionProposal.LAST_KIND) {
				throw new IllegalArgumentException("Unknown kind of completion proposal: "+requiredProposalKind); //$NON-NLS-1$
			}

		if (requiredProposalKind < CompletionProposal.FIRST_KIND
			|| requiredProposalKind > CompletionProposal.LAST_KIND) {
				throw new IllegalArgumentException("Unknown required kind of completion proposal: "+requiredProposalKind); //$NON-NLS-1$
		}
		if (this.requiredProposalAllowSet == null) return false;

		return 0 != (this.requiredProposalAllowSet[proposalKind] & (1 << requiredProposalKind));
	}

	/**
	 * Sets whether a proposal of a given kind with a required proposal
	 * of the given kind is allowed.
	 *
	 * A required proposal of a given kind is proposed even if {@link #isIgnored(int)}
	 * return <code>true</code> for that kind.
	 *
	 * Currently only a subset of kinds support required proposals. To see what combinations
	 * are supported you must look at {@link CompletionProposal#getRequiredProposals()}
	 * documentation.
	 *
	 * @param proposalKind one of the kind constants declared
	 * @param requiredProposalKind one of the kind constants declared
	 * on <code>CompletionProposal</code>
	 * @param allow <code>true</code> if a proposal of a given kind with a required proposal
	 * of the given kind is allowed by this requestor, and <code>false</code>
	 * if it isn't of interest
	 * @see #isAllowingRequiredProposals(int, int)
	 * @see CompletionProposal#getKind()
	 * @see CompletionProposal#getRequiredProposals()
	 *
	 * @since 3.3
	 */
	public void setAllowsRequiredProposals(int proposalKind, int requiredProposalKind, boolean allow) {
		if (proposalKind < CompletionProposal.FIRST_KIND
			|| proposalKind > CompletionProposal.LAST_KIND) {
				throw new IllegalArgumentException("Unknown kind of completion proposal: "+requiredProposalKind); //$NON-NLS-1$
		}
		if (requiredProposalKind < CompletionProposal.FIRST_KIND
			|| requiredProposalKind > CompletionProposal.LAST_KIND) {
				throw new IllegalArgumentException("Unknown required kind of completion proposal: "+requiredProposalKind); //$NON-NLS-1$
		}

		if (this.requiredProposalAllowSet == null) {
			this.requiredProposalAllowSet = new int[CompletionProposal.LAST_KIND + 1];
		}

		if (allow) {
			this.requiredProposalAllowSet[proposalKind] |= (1 << requiredProposalKind);
		} else {
			this.requiredProposalAllowSet[proposalKind] &= ~(1 << requiredProposalKind);
		}
	}

	/**
	 * Returns the favorite references which are used to compute some completion proposals.
	 * <p>
	 * A favorite reference is a qualified reference as it can be seen in an import statement.<br>
	 * e.g. <code>{"java.util.Arrays"}</code><br>
	 * It can be an on demand reference.<br>
	 * e.g. <code>{"java.util.Arrays.*"}</code>
	 * It can be a reference to a static method or field (as in a static import)<br>
	 * e.g. <code>{"java.util.Arrays.equals"}</code>
	 * </p>
	 * <p>
	 * Currently only on demand type references (<code>"java.util.Arrays.*"</code>),
	 * references to a static method or a static field are used to compute completion proposals.
	 * Other kind of reference could be used in the future.
	 * </p>
	 * @return favorite imports
	 *
	 * @since 3.3
	 */
	public String[] getFavoriteReferences() {
		return this.favoriteReferences;
	}

	/**
	 * Set the favorite references which will be used to compute some completion proposals.
	 * A favorite reference is a qualified reference as it can be seen in an import statement.<br>
	 *
	 * @param favoriteImports
	 *
	 * @see #getFavoriteReferences()
	 *
	 * @since 3.3
	 */
	public void setFavoriteReferences(String[] favoriteImports) {
		this.favoriteReferences = favoriteImports;
	}

	/**
	 * Pro forma notification sent before reporting a batch of
	 * completion proposals.
	 * <p>
	 * The default implementation of this method does nothing.
	 * Clients may override.
	 * </p>
	 */
	public void beginReporting() {
		// do nothing
	}

	/**
	 * Pro forma notification sent after reporting a batch of
	 * completion proposals.
	 * <p>
	 * The default implementation of this method does nothing.
	 * Clients may override.
	 * </p>
	 */
	public void endReporting() {
		// do nothing
	}

	/**
	 * Notification of failure to produce any completions.
	 * The problem object explains what prevented completing.
	 * <p>
	 * The default implementation of this method does nothing.
	 * Clients may override to receive this kind of notice.
	 * </p>
	 *
	 * @param problem the problem object
	 */
	public void completionFailure(IProblem problem) {
		// default behavior is to ignore
	}

	/**
	 * Proposes a completion. Has no effect if the kind of proposal
	 * is being ignored by this requestor. Callers should consider
	 * checking {@link #isIgnored(int)} before avoid creating proposal
	 * objects that would only be ignored.
	 * <p>
	 * Similarly, implementers should check
	 * {@link #isIgnored(int) isIgnored(proposal.getKind())}
	 * and ignore proposals that have been declared as uninteresting.
	 * The proposal object passed is only valid for the duration of
	 * completion operation.
	 *
	 * @param proposal the completion proposal
	 * @exception IllegalArgumentException if the proposal is null
	 */
	public abstract void accept(CompletionProposal proposal);

	/**
	 * Propose the context in which the completion occurs.
	 * <p>
	 * This method is called one and only one time before any call to
	 * {@link #accept(CompletionProposal)}.
	 * The default implementation of this method does nothing.
	 * Clients may override.
	 * </p>
	 * @param context the completion context
	 *
	 * @since 3.1
	 */
	public void acceptContext(CompletionContext context) {
		// do nothing
	}

	/**
	 * Returns whether this requestor requires an extended context.
	 *
	 * By default this method return <code>false</code>.
	 *
	 * @return <code>true</code> if this requestor requires an extended context.
	 *
	 * @see CompletionContext#isExtended()
	 *
	 * @since 3.4
	 */
	public boolean isExtendedContextRequired() {
		return this.requireExtendedContext;
	}


	/**
	 * Sets whether this requestor requires an extended context.
	 *
	 * @param require <code>true</code> if this requestor requires an extended context.
	 *
	 * @see CompletionContext#isExtended()
	 *
	 * @since 3.4
	 */
	public void setRequireExtendedContext(boolean require) {
		this.requireExtendedContext = require;
	}

	/**
	 * If this returns true, exclude test sources and dependencies.
	 *
	 * @return <code>true</code> if this requestor does not want to get any completions from test code.
	 * @see IClasspathAttribute#TEST
	 * @since 3.14
	 */
	public boolean isTestCodeExcluded() {
		return false;
	}
}
