/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.codeassist.complete.CompletionParser;
import org.eclipse.jdt.internal.codeassist.complete.CompletionScanner;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.util.SimpleSetOfCharArray;
import org.eclipse.jdt.internal.compiler.util.Util;

public class UnresolvedReferenceNameFinder extends ASTVisitor {
	private static final int MAX_LINE_COUNT = 100;
	private static final int FAKE_BLOCKS_COUNT = 20;

	public static interface UnresolvedReferenceNameRequestor {
		public void acceptName(char[] name);
	}

	private UnresolvedReferenceNameRequestor requestor;

	private CompletionEngine completionEngine;
	private CompletionParser parser;
	private CompletionScanner completionScanner;

	private int parentsPtr;
	private ASTNode[] parents;

	private int potentialVariableNamesPtr;
	private char[][] potentialVariableNames;
	private int[] potentialVariableNameStarts;

	private SimpleSetOfCharArray acceptedNames = new SimpleSetOfCharArray();

	public UnresolvedReferenceNameFinder(CompletionEngine completionEngine) {
		this.completionEngine = completionEngine;
		this.parser = completionEngine.parser;
		this.completionScanner = (CompletionScanner) this.parser.scanner;
	}

	private void acceptName(char[] name) {
		// the null check is added to fix bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=166570
		if (name == null) return;

		if (!CharOperation.prefixEquals(this.completionEngine.completionToken, name, false /* ignore case */)
				&& !(this.completionEngine.options.camelCaseMatch && CharOperation.camelCaseMatch(this.completionEngine.completionToken, name))) return;

		if (this.acceptedNames.includes(name)) return;

		this.acceptedNames.add(name);

		// accept result
		this.requestor.acceptName(name);
	}

	public void find(
			char[] startWith,
			Initializer initializer,
			ClassScope scope,
			int from,
			char[][] discouragedNames,
			UnresolvedReferenceNameRequestor nameRequestor) {
		MethodDeclaration fakeMethod =
			this.findAfter(startWith, scope, from, initializer.bodyEnd, MAX_LINE_COUNT, false, discouragedNames, nameRequestor);
		if (fakeMethod != null) fakeMethod.traverse(this, scope);
	}

	public void find(
			char[] startWith,
			AbstractMethodDeclaration methodDeclaration,
			int from,
			char[][] discouragedNames,
			UnresolvedReferenceNameRequestor nameRequestor) {
		MethodDeclaration fakeMethod =
			this.findAfter(startWith, methodDeclaration.scope, from, methodDeclaration.bodyEnd, MAX_LINE_COUNT, false, discouragedNames, nameRequestor);
		if (fakeMethod != null) fakeMethod.traverse(this, methodDeclaration.scope.classScope());
	}

	public void findAfter(
			char[] startWith,
			Scope scope,
			ClassScope classScope,
			int from,
			int to,
			char[][] discouragedNames,
			UnresolvedReferenceNameRequestor nameRequestor) {
		MethodDeclaration fakeMethod =
			this.findAfter(startWith, scope, from, to, MAX_LINE_COUNT / 2, true, discouragedNames, nameRequestor);
		if (fakeMethod != null) fakeMethod.traverse(this, classScope);
	}

	private MethodDeclaration findAfter(
			char[] startWith,
			Scope s,
			int from,
			int to,
			int maxLineCount,
			boolean outsideEnclosingBlock,
			char[][] discouragedNames,
			UnresolvedReferenceNameRequestor nameRequestor) {
		this.requestor = nameRequestor;

		// reinitialize completion scanner to be usable as a normal scanner
		this.completionScanner.cursorLocation = 0;

		if (!outsideEnclosingBlock) {
			// compute location of the end of the current block
			this.completionScanner.resetTo(from + 1, to);
			this.completionScanner.jumpOverBlock();

			to = this.completionScanner.startPosition - 1;
		}

		int maxEnd =
			this.completionScanner.getLineEnd(
					Util.getLineNumber(from, this.completionScanner.lineEnds, 0, this.completionScanner.linePtr) + maxLineCount);

		int end;
		if (maxEnd < 0) {
			end = to;
		} else {
			end = maxEnd < to ? maxEnd : to;
		}

		this.parser.startRecordingIdentifiers(from, end);
		int savedCursorLocation = this.parser.cursorLocation;
		this.parser.cursorLocation = Integer.MAX_VALUE; // limit (from,end) already defines what we want to look at.

		MethodDeclaration fakeMethod = this.parser.parseSomeStatements(
				from,
				end,
				outsideEnclosingBlock ? FAKE_BLOCKS_COUNT : 0,
				s.compilationUnitScope().referenceContext);

		this.parser.cursorLocation = savedCursorLocation;
		this.parser.stopRecordingIdentifiers();

		if(!initPotentialNamesTables(discouragedNames)) return null;

		this.parentsPtr = -1;
		this.parents = new ASTNode[10];

		return fakeMethod;
	}

	public void findBefore(
			char[] startWith,
			Scope scope,
			ClassScope classScope,
			int from,
			int recordTo,
			int parseTo,
			char[][] discouragedNames,
			UnresolvedReferenceNameRequestor nameRequestor) {
		MethodDeclaration fakeMethod =
			this.findBefore(startWith, scope, from, recordTo, parseTo, MAX_LINE_COUNT / 2, discouragedNames, nameRequestor);
		if (fakeMethod != null) fakeMethod.traverse(this, classScope);
	}

	private MethodDeclaration findBefore(
			char[] startWith,
			Scope s,
			int from,
			int recordTo,
			int parseTo,
			int maxLineCount,
			char[][] discouragedNames,
			UnresolvedReferenceNameRequestor nameRequestor) {
		this.requestor = nameRequestor;

		// reinitialize completion scanner to be usable as a normal scanner
		this.completionScanner.cursorLocation = 0;

		int minStart =
			this.completionScanner.getLineStart(
					Util.getLineNumber(recordTo, this.completionScanner.lineEnds, 0, this.completionScanner.linePtr) - maxLineCount);

		int start;
		int fakeBlocksCount;
		if (minStart <= from) {
			start = from;
			fakeBlocksCount = 0;
		} else {
			start = minStart;
			fakeBlocksCount = FAKE_BLOCKS_COUNT;
		}

		this.parser.startRecordingIdentifiers(start, recordTo);

		MethodDeclaration fakeMethod = this.parser.parseSomeStatements(
				start,
				parseTo,
				fakeBlocksCount,
				s.compilationUnitScope().referenceContext);

		this.parser.stopRecordingIdentifiers();

		if(!initPotentialNamesTables(discouragedNames)) return null;

		this.parentsPtr = -1;
		this.parents = new ASTNode[10];

		return fakeMethod;
	}

	private boolean initPotentialNamesTables(char[][] discouragedNames) {
		char[][] pvns = this.parser.potentialVariableNames;
		int[] pvnss = this.parser.potentialVariableNameStarts;
		int pvnsPtr = this.parser.potentialVariableNamesPtr;

		if (pvnsPtr < 0) return false; // there is no potential names

		// remove null and discouragedNames
		int discouragedNamesCount = discouragedNames == null ? 0 : discouragedNames.length;
		int j = -1;
		next : for (int i = 0; i <= pvnsPtr; i++) {
			char[] temp = pvns[i];

			if (temp == null) continue next;

			for (int k = 0; k < discouragedNamesCount; k++) {
				if (CharOperation.equals(temp, discouragedNames[k], false)) {
					continue next;
				}
			}

			pvns[i] = null;
			pvns[++j] = temp;
			pvnss[j] = pvnss[i];
		}
		pvnsPtr = j;

		if (pvnsPtr < 0) return false; // there is no potential names

		this.potentialVariableNames = pvns;
		this.potentialVariableNameStarts = pvnss;
		this.potentialVariableNamesPtr = pvnsPtr;

		return true;
	}

	private void popParent() {
		this.parentsPtr--;
	}
	private void pushParent(ASTNode parent) {
		int length = this.parents.length;
		if (this.parentsPtr >= length - 1) {
			System.arraycopy(this.parents, 0, this.parents = new ASTNode[length * 2], 0, length);
		}
		this.parents[++this.parentsPtr] = parent;
	}

	private ASTNode getEnclosingDeclaration() {
		int i = this.parentsPtr;
		while (i > -1) {
			ASTNode parent = this.parents[i];
			if (parent instanceof AbstractMethodDeclaration) {
				return parent;
			} else if (parent instanceof Initializer) {
				return parent;
			} else if (parent instanceof FieldDeclaration) {
				return parent;
			} else if (parent instanceof TypeDeclaration) {
				return parent;
			}
			i--;
		}
		return null;
	}

	@Override
	public boolean visit(Block block, BlockScope blockScope) {
		ASTNode enclosingDeclaration = getEnclosingDeclaration();
		removeLocals(block.statements, enclosingDeclaration.sourceStart, block.sourceEnd);
		pushParent(block);
		return true;
	}

	@Override
	public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope classScope) {
		if (((constructorDeclaration.bits & ASTNode.IsDefaultConstructor) == 0) && !constructorDeclaration.isClinit()) {
			removeLocals(
					constructorDeclaration.arguments,
					constructorDeclaration.declarationSourceStart,
					constructorDeclaration.declarationSourceEnd);
			removeLocals(
					constructorDeclaration.statements,
					constructorDeclaration.declarationSourceStart,
					constructorDeclaration.declarationSourceEnd);
		}
		pushParent(constructorDeclaration);
		return true;
	}

	@Override
	public boolean visit(FieldDeclaration fieldDeclaration, MethodScope methodScope) {
		pushParent(fieldDeclaration);
		return true;
	}

	@Override
	public boolean visit(Initializer initializer, MethodScope methodScope) {
		pushParent(initializer);
		return true;
	}

	@Override
	public boolean visit(MethodDeclaration methodDeclaration, ClassScope classScope) {
		removeLocals(
				methodDeclaration.arguments,
				methodDeclaration.declarationSourceStart,
				methodDeclaration.declarationSourceEnd);
		removeLocals(
				methodDeclaration.statements,
				methodDeclaration.declarationSourceStart,
				methodDeclaration.declarationSourceEnd);
		pushParent(methodDeclaration);
		return true;
	}

	@Override
	public boolean visit(TypeDeclaration localTypeDeclaration, BlockScope blockScope) {
		removeFields(localTypeDeclaration);
		pushParent(localTypeDeclaration);
		return true;
	}

	@Override
	public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope classScope) {
		removeFields(memberTypeDeclaration);
		pushParent(memberTypeDeclaration);
		return true;
	}

	@Override
	public void endVisit(Block block, BlockScope blockScope) {
		popParent();
	}

	@Override
	public void endVisit(Argument argument, BlockScope blockScope) {
		endVisitRemoved(argument.declarationSourceStart, argument.sourceEnd);
	}

	@Override
	public void endVisit(Argument argument, ClassScope classScope) {
		endVisitRemoved(argument.declarationSourceStart, argument.sourceEnd);
	}

	@Override
	public void endVisit(ConstructorDeclaration constructorDeclaration, ClassScope classScope) {
		if (((constructorDeclaration.bits & ASTNode.IsDefaultConstructor) == 0) && !constructorDeclaration.isClinit()) {
			endVisitPreserved(constructorDeclaration.bodyStart, constructorDeclaration.bodyEnd);
		}
		popParent();
	}

	@Override
	public void endVisit(FieldDeclaration fieldDeclaration, MethodScope methodScope) {
		endVisitRemoved(fieldDeclaration.declarationSourceStart, fieldDeclaration.sourceEnd);
		endVisitPreserved(fieldDeclaration.sourceEnd, fieldDeclaration.declarationEnd);
		popParent();
	}

	@Override
	public void endVisit(Initializer initializer, MethodScope methodScope) {
		endVisitPreserved(initializer.bodyStart, initializer.bodyEnd);
		popParent();
	}

	@Override
	public void endVisit(LocalDeclaration localDeclaration, BlockScope blockScope) {
		endVisitRemoved(localDeclaration.declarationSourceStart, localDeclaration.sourceEnd);
	}

	@Override
	public void endVisit(MethodDeclaration methodDeclaration, ClassScope classScope) {
		endVisitPreserved(
				methodDeclaration.bodyStart,
				methodDeclaration.bodyEnd);
		popParent();
	}

	@Override
	public void endVisit(TypeDeclaration typeDeclaration, BlockScope blockScope) {
		endVisitRemoved(typeDeclaration.sourceStart, typeDeclaration.declarationSourceEnd);
		popParent();
	}

	@Override
	public void endVisit(TypeDeclaration typeDeclaration, ClassScope classScope) {
		endVisitRemoved(typeDeclaration.sourceStart, typeDeclaration.declarationSourceEnd);
		popParent();
	}

	private int indexOfFisrtNameAfter(int position) {
		int left = 0;
		int right = this.potentialVariableNamesPtr;

		next : while (true) {
			if (right < left) return -1;

			int mid = left + (right - left) / 2;
			int midPosition = this.potentialVariableNameStarts[mid];
			if (midPosition < 0) {
				int nextMid = indexOfNextName(mid);
				if (nextMid < 0 || right < nextMid) { // no next index or next index is after 'right'
					right = mid - 1;
					continue next;
				}
				mid = nextMid;
				midPosition = this.potentialVariableNameStarts[nextMid];

				if (mid == right) { // mid and right are at the same index, we must move 'left'
					int leftPosition = this.potentialVariableNameStarts[left];
					if (leftPosition < 0 || leftPosition < position) { // 'left' is empty or 'left' is before the position
						int nextLeft = indexOfNextName(left);
						if (nextLeft < 0) return - 1;

						left = nextLeft;
						continue next;
					}

					return left;
				}
			}

			if (left != right) {
				if (midPosition < position) {
					left = mid + 1;
				} else {
					right = mid;
				}
			} else {
				if (midPosition < position) {
					return -1;
				}
				return mid;
			}
		}
	}

	private int indexOfNextName(int index) {
		int nextIndex = index + 1;
		while (nextIndex <= this.potentialVariableNamesPtr &&
				this.potentialVariableNames[nextIndex] == null) {
			int jumpIndex = -this.potentialVariableNameStarts[nextIndex];
			if (jumpIndex > 0) {
				nextIndex = jumpIndex;
			} else {
				nextIndex++;
			}
		}

		if (this.potentialVariableNamesPtr < nextIndex) {
			if  (index < this.potentialVariableNamesPtr) {
				this.potentialVariableNamesPtr = index;
			}
			return -1;
		}
		if (index + 1 < nextIndex) {
			this.potentialVariableNameStarts[index + 1] = -nextIndex;
		}
		return nextIndex;
	}

	private void removeNameAt(int index) {
		this.potentialVariableNames[index] = null;
		int nextIndex = indexOfNextName(index);
		if (nextIndex != -1) {
			this.potentialVariableNameStarts[index] = -nextIndex;
		} else {
			this.potentialVariableNamesPtr = index - 1;
		}
	}

	private void endVisitPreserved(int start, int end) {
		int i = indexOfFisrtNameAfter(start);
		done : while (i != -1) {
			int nameStart = this.potentialVariableNameStarts[i];
			if (start < nameStart && nameStart < end) {
				acceptName(this.potentialVariableNames[i]);
				removeNameAt(i);
			}

			if (end < nameStart) break done;
			i = indexOfNextName(i);
		}
	}

	private void endVisitRemoved(int start, int end) {
		int i = indexOfFisrtNameAfter(start);
		done : while (i != -1) {
			int nameStart = this.potentialVariableNameStarts[i];
			if (start < nameStart && nameStart < end) {
				removeNameAt(i);
			}

			if (end < nameStart) break done;
			i = indexOfNextName(i);
		}
	}

	private void removeLocals(Statement[] statements, int start, int end) {
		if (statements != null) {
			for (int i = 0; i < statements.length; i++) {
				if (statements[i] instanceof LocalDeclaration) {
					LocalDeclaration localDeclaration = (LocalDeclaration) statements[i];
					int j = indexOfFisrtNameAfter(start);
					done : while (j != -1) {
						int nameStart = this.potentialVariableNameStarts[j];
						if (start <= nameStart && nameStart <= end) {
							if (CharOperation.equals(this.potentialVariableNames[j], localDeclaration.name, false)) {
								removeNameAt(j);
							}
						}

						if (end < nameStart) break done;
						j = indexOfNextName(j);
					}
				}
			}

		}
	}

	private void removeFields(TypeDeclaration typeDeclaration) {
		int start = typeDeclaration.declarationSourceStart;
		int end = typeDeclaration.declarationSourceEnd;

		FieldDeclaration[] fieldDeclarations = typeDeclaration.fields;
		if (fieldDeclarations != null) {
			for (int i = 0; i < fieldDeclarations.length; i++) {
				int j = indexOfFisrtNameAfter(start);
				done : while (j != -1) {
					int nameStart = this.potentialVariableNameStarts[j];
					if (start <= nameStart && nameStart <= end) {
						if (CharOperation.equals(this.potentialVariableNames[j], fieldDeclarations[i].name, false)) {
							removeNameAt(j);
						}
					}

					if (end < nameStart) break done;
					j = indexOfNextName(j);
				}
			}
		}
	}
}
