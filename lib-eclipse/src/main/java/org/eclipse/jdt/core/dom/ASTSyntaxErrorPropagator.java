/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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

package org.eclipse.jdt.core.dom;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;

/**
 * Internal AST visitor for propagating syntax errors.
 */
class ASTSyntaxErrorPropagator extends ASTVisitor {

	private CategorizedProblem[] problems;

	ASTSyntaxErrorPropagator(CategorizedProblem[] problems) {
		// visit Javadoc.tags() as well
		super(true);
		this.problems = problems;
	}

	private boolean checkAndTagAsMalformed(ASTNode node) {
		boolean tagWithErrors = false;
		search: for (int i = 0, max = this.problems.length; i < max; i++) {
			CategorizedProblem problem = this.problems[i];
			switch(problem.getID()) {
				case IProblem.ParsingErrorOnKeywordNoSuggestion :
				case IProblem.ParsingErrorOnKeyword :
				case IProblem.ParsingError :
				case IProblem.ParsingErrorNoSuggestion :
				case IProblem.ParsingErrorInsertTokenBefore :
				case IProblem.ParsingErrorInsertTokenAfter :
				case IProblem.ParsingErrorDeleteToken :
				case IProblem.ParsingErrorDeleteTokens :
				case IProblem.ParsingErrorMergeTokens :
				case IProblem.ParsingErrorInvalidToken :
				case IProblem.ParsingErrorMisplacedConstruct :
				case IProblem.ParsingErrorReplaceTokens :
				case IProblem.ParsingErrorNoSuggestionForTokens :
				case IProblem.ParsingErrorUnexpectedEOF :
				case IProblem.ParsingErrorInsertToComplete :
				case IProblem.ParsingErrorInsertToCompleteScope :
				case IProblem.ParsingErrorInsertToCompletePhrase :
				case IProblem.EndOfSource :
				case IProblem.InvalidHexa :
				case IProblem.InvalidOctal :
				case IProblem.InvalidCharacterConstant :
				case IProblem.InvalidEscape :
				case IProblem.InvalidInput :
				case IProblem.InvalidUnicodeEscape :
				case IProblem.InvalidFloat :
				case IProblem.NullSourceString :
				case IProblem.UnterminatedString :
				case IProblem.UnterminatedComment :
				case IProblem.InvalidDigit :
					break;
				default:
					continue search;
			}
			int position = problem.getSourceStart();
			int start = node.getStartPosition();
			int end = start + node.getLength();
			if ((start <= position) && (position <= end)) {
				node.setFlags(node.getFlags() | ASTNode.MALFORMED);
				// clear the bits on parent
				ASTNode currentNode = node.getParent();
				while (currentNode != null) {
					currentNode.setFlags(currentNode.getFlags() & ~ASTNode.MALFORMED);
					currentNode = currentNode.getParent();
				}
				tagWithErrors = true;
			}
		}
		return tagWithErrors;
	}

	@Override
	public boolean visit(FieldDeclaration node) {
		return checkAndTagAsMalformed(node);
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		return checkAndTagAsMalformed(node);
	}

	@Override
	public boolean visit(ModuleDeclaration node) {
		return checkAndTagAsMalformed(node);
	}

	@Override
	public boolean visit(PackageDeclaration node) {
		return checkAndTagAsMalformed(node);
	}

	@Override
	public boolean visit(ImportDeclaration node) {
		return checkAndTagAsMalformed(node);
	}

	@Override
	public boolean visit(CompilationUnit node) {
		return checkAndTagAsMalformed(node);
	}

	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		return checkAndTagAsMalformed(node);
	}

	@Override
	public boolean visit(EnumDeclaration node) {
		return checkAndTagAsMalformed(node);
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		return checkAndTagAsMalformed(node);
	}

	@Override
	public boolean visit(Initializer node) {
		return checkAndTagAsMalformed(node);
	}

}
