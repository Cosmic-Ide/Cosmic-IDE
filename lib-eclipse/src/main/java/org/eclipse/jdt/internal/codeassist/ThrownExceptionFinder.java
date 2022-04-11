/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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
package org.eclipse.jdt.internal.codeassist;

import java.util.Stack;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.ThrowStatement;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.UnionTypeReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.util.SimpleSet;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ThrownExceptionFinder extends ASTVisitor {

	private SimpleSet thrownExceptions;
	private Stack exceptionsStack;
	private SimpleSet caughtExceptions;
	private SimpleSet discouragedExceptions;

	/**
	 * Finds the thrown exceptions minus the ones that are already caught in previous catch blocks.
	 * Exception is already caught even if its super type is being caught. Also computes, separately,
	 * a list comprising of (a)those exceptions that have been caught already and (b)those exceptions that are thrown
	 * by the method and whose super type has been caught already.
	 * @param tryStatement
	 * @param scope
	 */
	public void processThrownExceptions(TryStatement tryStatement, BlockScope scope) {
		this.thrownExceptions = new SimpleSet();
		this.exceptionsStack = new Stack();
		this.caughtExceptions = new SimpleSet();
		this.discouragedExceptions = new SimpleSet();
		tryStatement.traverse(this, scope);
		removeCaughtExceptions(tryStatement, true /*remove unchecked exceptions this time*/);
	}

	private void acceptException(ReferenceBinding binding) {
		if (binding != null && binding.isValidBinding()) {
			this.thrownExceptions.add(binding);
		}
	}

	@Override
	public void endVisit(MessageSend messageSend, BlockScope scope) {
		if (messageSend.binding != null) {
			endVisitMethodInvocation(messageSend.binding);
		}
		super.endVisit(messageSend, scope);
	}

	@Override
	public void endVisit(AllocationExpression allocationExpression, BlockScope scope) {
		if (allocationExpression.binding != null) {
			endVisitMethodInvocation(allocationExpression.binding);
		}
		super.endVisit(allocationExpression, scope);
	}

	@Override
	public void endVisit(ThrowStatement throwStatement, BlockScope scope) {
		acceptException((ReferenceBinding)throwStatement.exception.resolvedType);
		super.endVisit(throwStatement, scope);
	}


	private void endVisitMethodInvocation(MethodBinding methodBinding) {
		ReferenceBinding[] thrownExceptionBindings = methodBinding.thrownExceptions;
		int length = thrownExceptionBindings == null ? 0 : thrownExceptionBindings.length;
		for (int i = 0; i < length; i++) {
			acceptException(thrownExceptionBindings[i]);
		}
	}


	/**
	 * Returns all the already caught exceptions in catch blocks, found by the call to
	 * {@link ThrownExceptionFinder#processThrownExceptions(TryStatement, BlockScope)}
	 * @return Returns an array of those exceptions that have been caught already in previous catch or
	 * multi-catch blocks of the same try block. (Exceptions caught in inner try-catches are obtained via
	 * {@link ThrownExceptionFinder#getDiscouragedExceptions()}.
	 */
	public ReferenceBinding[] getAlreadyCaughtExceptions() {
		ReferenceBinding[] allCaughtExceptions = new ReferenceBinding[this.caughtExceptions.elementSize];
		this.caughtExceptions.asArray(allCaughtExceptions);
		return allCaughtExceptions;
	}

	/**
	 * Returns all the thrown exceptions minus the ones that are already caught in previous catch blocks
	 * (of the same try), found by the call to
	 * {@link ThrownExceptionFinder#processThrownExceptions(TryStatement, BlockScope)}.
	 * @return Returns an array of thrown exceptions that are still not caught in any catch block.
	 */
	public ReferenceBinding[] getThrownUncaughtExceptions() {
		ReferenceBinding[] result = new ReferenceBinding[this.thrownExceptions.elementSize];
		this.thrownExceptions.asArray(result);
		return result;
	}

	/**
	 * Returns all exceptions that are discouraged to use because (a) they are already caught in some inner try-catch,
	 * or (b) their super exception has already been caught.
	 * @return all discouraged exceptions
	 */
	public ReferenceBinding[] getDiscouragedExceptions() {
		ReferenceBinding[] allDiscouragedExceptions = new ReferenceBinding[this.discouragedExceptions.elementSize];
		this.discouragedExceptions.asArray(allDiscouragedExceptions);
		return allDiscouragedExceptions;
	}
	@Override
	public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
		return visitType(typeDeclaration);
	}

	@Override
	public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
		return visitType(memberTypeDeclaration);
	}

	@Override
	public boolean visit(TypeDeclaration localTypeDeclaration, BlockScope scope) {
		return visitType(localTypeDeclaration);
	}

	private boolean visitType(TypeDeclaration typeDeclaration) {
		return false;
	}

	@Override
	public boolean visit(TryStatement tryStatement, BlockScope scope) {
		this.exceptionsStack.push(this.thrownExceptions);
		SimpleSet exceptionSet = new SimpleSet();
		this.thrownExceptions = exceptionSet;
		tryStatement.tryBlock.traverse(this, scope);

		removeCaughtExceptions(tryStatement, false);

		this.thrownExceptions = (SimpleSet)this.exceptionsStack.pop();

		Object[] values = exceptionSet.values;
		for (int i = 0; i < values.length; i++) {
			if (values[i] != null) {
				this.thrownExceptions.add(values[i]);
			}
		}

		Block[] catchBlocks = tryStatement.catchBlocks;
		int length = catchBlocks == null ? 0 : catchBlocks.length;
		for (int i = 0; i < length; i++) {
			catchBlocks[i].traverse(this, scope);
		}
		return false;
	}

	private void removeCaughtExceptions(TryStatement tryStatement, boolean recordUncheckedCaughtExceptions) {
		Argument[] catchArguments = tryStatement.catchArguments;
		int length = catchArguments == null ? 0 : catchArguments.length;
		for (int i = 0; i < length; i++) {
			if (catchArguments[i].type instanceof UnionTypeReference) {
				UnionTypeReference unionTypeReference = (UnionTypeReference) catchArguments[i].type;
				TypeBinding caughtException;
				for (int j = 0; j < unionTypeReference.typeReferences.length; j++) {
					caughtException = unionTypeReference.typeReferences[j].resolvedType;
					if ((caughtException instanceof ReferenceBinding) && caughtException.isValidBinding()) {	// might be null when its the completion node
						if (recordUncheckedCaughtExceptions) {
							// is in outermost try-catch. Remove all caught exceptions, unchecked or checked
							removeCaughtException((ReferenceBinding)caughtException);
							this.caughtExceptions.add(caughtException);
						} else {
							// is in some inner try-catch. Discourage already caught checked exceptions
							// from being proposed in an outer catch.
							if (!caughtException.isUncheckedException(true)) {
								this.discouragedExceptions.add(caughtException);
							}
						}
					}
				}
			} else {
				TypeBinding exception = catchArguments[i].type.resolvedType;
				if ((exception instanceof ReferenceBinding) && exception.isValidBinding()) {
					if (recordUncheckedCaughtExceptions) {
						// is in outermost try-catch. Remove all caught exceptions, unchecked or checked
						removeCaughtException((ReferenceBinding)exception);
						this.caughtExceptions.add(exception);
					} else {
						// is in some inner try-catch. Discourage already caught checked exceptions
						// from being proposed in an outer catch
						if (!exception.isUncheckedException(true)) {
							this.discouragedExceptions.add(exception);
						}
					}
				}
			}
		}
	}

	private void removeCaughtException(ReferenceBinding caughtException) {
		Object[] exceptions = this.thrownExceptions.values;
		for (int i = 0; i < exceptions.length; i++) {
			ReferenceBinding exception = (ReferenceBinding)exceptions[i];
			if (exception != null) {
				if (TypeBinding.equalsEquals(exception, caughtException)) {
					this.thrownExceptions.remove(exception);
				} else if (caughtException.isSuperclassOf(exception)) {
					// catching the sub-exception when super has been caught already will give an error
					// so remove it from thrown list and lower the relevance for cases when it is found
					// from searchAllTypes(..)
					this.thrownExceptions.remove(exception);
					this.discouragedExceptions.add(exception);
				}
			}
		}
	}
}
