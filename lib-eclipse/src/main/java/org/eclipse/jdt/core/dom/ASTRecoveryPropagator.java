/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.parser.RecoveryScanner;
import org.eclipse.jdt.internal.compiler.parser.RecoveryScannerData;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObjectToIntArray;

/**
 * Internal AST visitor for propagating syntax errors.
 */
@SuppressWarnings({"rawtypes"})
class ASTRecoveryPropagator extends DefaultASTVisitor {
	private static final int NOTHING = -1;
	HashtableOfObjectToIntArray endingTokens = new HashtableOfObjectToIntArray();
	{
		this.endingTokens.put(AnonymousClassDeclaration.class, new int[]{TerminalTokens.TokenNameRBRACE});
		this.endingTokens.put(ArrayAccess.class, new int[]{TerminalTokens.TokenNameRBRACKET});
		this.endingTokens.put(ArrayCreation.class, new int[]{NOTHING, TerminalTokens.TokenNameRBRACKET});
		this.endingTokens.put(ArrayInitializer.class, new int[]{TerminalTokens.TokenNameRBRACE});
		this.endingTokens.put(ArrayType.class, new int[]{TerminalTokens.TokenNameRBRACKET});
		this.endingTokens.put(AssertStatement.class, new int[]{TerminalTokens.TokenNameSEMICOLON});
		this.endingTokens.put(Block.class, new int[]{TerminalTokens.TokenNameRBRACE});
		this.endingTokens.put(BooleanLiteral.class, new int[]{TerminalTokens.TokenNamefalse, TerminalTokens.TokenNametrue});
		this.endingTokens.put(BreakStatement.class, new int[]{TerminalTokens.TokenNameSEMICOLON});
		this.endingTokens.put(CharacterLiteral.class, new int[]{TerminalTokens.TokenNameCharacterLiteral});
		this.endingTokens.put(ClassInstanceCreation.class, new int[]{TerminalTokens.TokenNameRBRACE, TerminalTokens.TokenNameRPAREN});
		this.endingTokens.put(ConstructorInvocation.class, new int[]{TerminalTokens.TokenNameSEMICOLON});
		this.endingTokens.put(ContinueStatement.class, new int[]{TerminalTokens.TokenNameSEMICOLON});
		this.endingTokens.put(DoStatement.class, new int[]{TerminalTokens.TokenNameRPAREN});
		this.endingTokens.put(EmptyStatement.class, new int[]{TerminalTokens.TokenNameSEMICOLON});
		this.endingTokens.put(ExpressionStatement.class, new int[]{TerminalTokens.TokenNameSEMICOLON});
		this.endingTokens.put(FieldDeclaration.class, new int[]{TerminalTokens.TokenNameSEMICOLON});
		this.endingTokens.put(ImportDeclaration.class, new int[]{TerminalTokens.TokenNameSEMICOLON});
		this.endingTokens.put(Initializer.class, new int[]{TerminalTokens.TokenNameRBRACE});
		this.endingTokens.put(MethodDeclaration.class, new int[]{NOTHING, TerminalTokens.TokenNameSEMICOLON});
		this.endingTokens.put(MethodInvocation.class, new int[]{TerminalTokens.TokenNameRPAREN});
		this.endingTokens.put(ModuleDeclaration.class, new int[]{TerminalTokens.TokenNameRBRACE});
		this.endingTokens.put(ModuleDirective.class, new int[]{TerminalTokens.TokenNameSEMICOLON});
		this.endingTokens.put(NullLiteral.class, new int[]{TerminalTokens.TokenNamenull});
		this.endingTokens.put(NumberLiteral.class, new int[]{TerminalTokens.TokenNameIntegerLiteral, TerminalTokens.TokenNameLongLiteral, TerminalTokens.TokenNameFloatingPointLiteral, TerminalTokens.TokenNameDoubleLiteral});
		this.endingTokens.put(PackageDeclaration.class, new int[]{TerminalTokens.TokenNameSEMICOLON});
		this.endingTokens.put(ParenthesizedExpression.class, new int[]{TerminalTokens.TokenNameRPAREN});
		this.endingTokens.put(PostfixExpression.class, new int[]{TerminalTokens.TokenNamePLUS_PLUS, TerminalTokens.TokenNameMINUS_MINUS});
		this.endingTokens.put(PrimitiveType.class, new int[]{TerminalTokens.TokenNamebyte, TerminalTokens.TokenNameshort, TerminalTokens.TokenNamechar, TerminalTokens.TokenNameint, TerminalTokens.TokenNamelong, TerminalTokens.TokenNamefloat, TerminalTokens.TokenNameboolean, TerminalTokens.TokenNamedouble, TerminalTokens.TokenNamevoid});
		this.endingTokens.put(ReturnStatement.class, new int[]{TerminalTokens.TokenNameSEMICOLON});
		this.endingTokens.put(SimpleName.class, new int[]{TerminalTokens.TokenNameIdentifier});
		this.endingTokens.put(SingleVariableDeclaration.class, new int[]{TerminalTokens.TokenNameSEMICOLON});
		this.endingTokens.put(StringLiteral.class, new int[]{TerminalTokens.TokenNameStringLiteral});
		this.endingTokens.put(SuperConstructorInvocation.class, new int[]{TerminalTokens.TokenNameSEMICOLON});
		this.endingTokens.put(SuperMethodInvocation.class, new int[]{TerminalTokens.TokenNameRPAREN});
		this.endingTokens.put(SwitchCase.class, new int[]{TerminalTokens.TokenNameCOLON});
		this.endingTokens.put(SwitchStatement.class, new int[]{TerminalTokens.TokenNameRBRACE});
		this.endingTokens.put(SynchronizedStatement.class, new int[]{TerminalTokens.TokenNameRBRACE});
		this.endingTokens.put(ThisExpression.class, new int[]{TerminalTokens.TokenNamethis});
		this.endingTokens.put(ThrowStatement.class, new int[]{TerminalTokens.TokenNameSEMICOLON});
		this.endingTokens.put(TypeDeclaration.class, new int[]{TerminalTokens.TokenNameRBRACE});
		this.endingTokens.put(TypeLiteral.class, new int[]{TerminalTokens.TokenNameclass});
		this.endingTokens.put(VariableDeclarationStatement.class, new int[]{TerminalTokens.TokenNameSEMICOLON});
	}

	private CategorizedProblem[] problems;
	private boolean[] usedOrIrrelevantProblems;

	private RecoveryScannerData data;
	private int blockDepth = 0;
	private int lastEnd;

	private int[] insertedTokensKind;
	private int[] insertedTokensPosition;
	private boolean[] insertedTokensFlagged;

	private boolean[] removedTokensFlagged;
	private boolean[] replacedTokensFlagged;

	private ArrayList<ASTNode> stack = new ArrayList<>();

	ASTRecoveryPropagator(CategorizedProblem[] problems, RecoveryScannerData data) {
		// visit Javadoc.tags() as well
		this.problems = problems;
		this.usedOrIrrelevantProblems = new boolean[problems.length];

		this.data = data;

		if(this.data != null) {

			int length = 0;
			for (int i = 0; i < data.insertedTokensPtr + 1; i++) {
				length += data.insertedTokens[i].length;
			}
			this.insertedTokensKind = new int[length];
			this.insertedTokensPosition = new int[length];
			this.insertedTokensFlagged = new boolean[length];
			int tokenCount = 0;
			for (int i = 0; i < data.insertedTokensPtr + 1; i++) {
				for (int j = 0; j < data.insertedTokens[i].length; j++) {
					this.insertedTokensKind[tokenCount] = data.insertedTokens[i][j];
					this.insertedTokensPosition[tokenCount] = data.insertedTokensPosition[i];
					tokenCount++;
				}
			}

			if(data.removedTokensPtr != -1) {
				this.removedTokensFlagged = new boolean[data.removedTokensPtr + 1];
			}
			if(data.replacedTokensPtr != -1) {
				this.replacedTokensFlagged = new boolean[data.replacedTokensPtr + 1];
			}
		}
	}

	@Override
	public void endVisit(Block node) {
		this.blockDepth--;
		if(this.blockDepth <= 0) {
			flagNodeWithInsertedTokens();
		}
		super.endVisit(node);
	}



	@Override
	public boolean visit(Block node) {
		boolean visitChildren = super.visit(node);
		this.blockDepth++;
		return visitChildren;
	}

	@Override
	protected boolean visitNode(ASTNode node) {
		if(this.blockDepth > 0) {
			int start = node.getStartPosition();
			int end = start + node.getLength() - 1;

			// continue to visit the node only if it contains tokens modifications

			if(this.insertedTokensFlagged != null) {
				for (int i = 0; i < this.insertedTokensFlagged.length; i++) {
					if(this.insertedTokensPosition[i] >= start &&
							this.insertedTokensPosition[i] <= end) {
						return true;
					}
				}
			}

			if(this.removedTokensFlagged != null) {
				for (int i = 0; i <= this.data.removedTokensPtr; i++) {
					if(this.data.removedTokensStart[i] >= start &&
							this.data.removedTokensEnd[i] <= end) {
						return true;
					}
				}
			}

			if(this.replacedTokensFlagged != null) {
				for (int i = 0; i <= this.data.replacedTokensPtr; i++) {
					if(this.data.replacedTokensStart[i] >= start &&
							this.data.replacedTokensEnd[i] <= end) {
						return true;
					}
				}
			}

			return false;
		}
		return true;
	}

	@Override
	protected void endVisitNode(ASTNode node) {
		int start = node.getStartPosition();
		int end = start + node.getLength() - 1;

		// is inside diet part of the ast
		if(this.blockDepth < 1) {
			switch (node.getNodeType()) {
				case ASTNode.ANNOTATION_TYPE_DECLARATION:
				case ASTNode.COMPILATION_UNIT:
				case ASTNode.ENUM_DECLARATION:
				case ASTNode.FIELD_DECLARATION:
				case ASTNode.IMPORT_DECLARATION:
				case ASTNode.INITIALIZER:
				case ASTNode.METHOD_DECLARATION:
				case ASTNode.MODULE_DECLARATION:
				case ASTNode.PACKAGE_DECLARATION:
				case ASTNode.TYPE_DECLARATION:
				case ASTNode.MARKER_ANNOTATION:
				case ASTNode.NORMAL_ANNOTATION:
				case ASTNode.SINGLE_MEMBER_ANNOTATION:
				case ASTNode.BLOCK:
					if(markIncludedProblems(start, end)) {
						node.setFlags(node.getFlags() | ASTNode.RECOVERED);
					}
					break;
			}
		} else {
			markIncludedProblems(start, end);

			if(this.insertedTokensFlagged != null) {
				if(this.lastEnd != end) {
					flagNodeWithInsertedTokens();
				}
				this.stack.add(node);
			}

			if(this.removedTokensFlagged != null) {
				for (int i = 0; i <= this.data.removedTokensPtr; i++) {
					if(!this.removedTokensFlagged[i] &&
							this.data.removedTokensStart[i] >= start &&
							this.data.removedTokensEnd[i] <= end) {
						node.setFlags(node.getFlags() | ASTNode.RECOVERED);
						this.removedTokensFlagged[i] = true;
					}
				}
			}

			if(this.replacedTokensFlagged != null) {
				for (int i = 0; i <= this.data.replacedTokensPtr; i++) {
					if(!this.replacedTokensFlagged[i] &&
							this.data.replacedTokensStart[i] >= start &&
							this.data.replacedTokensEnd[i] <= end) {
						node.setFlags(node.getFlags() | ASTNode.RECOVERED);
						this.replacedTokensFlagged[i] = true;
					}
				}
			}
		}
		this.lastEnd = end;
	}

	private void flagNodeWithInsertedTokens() {
		if(this.insertedTokensKind != null && this.insertedTokensKind.length > 0) {
			int s = this.stack.size();
			for (int i = s - 1; i > -1; i--) {
				flagNodesWithInsertedTokensAtEnd(this.stack.get(i));
			}
			for (int i = 0; i < s; i++) {
				flagNodesWithInsertedTokensInside(this.stack.get(i));
			}
			this.stack = new ArrayList<>();
		}
	}

	private boolean flagNodesWithInsertedTokensAtEnd(ASTNode node) {
		int[] expectedEndingToken = this.endingTokens.get(node.getClass());
		if (expectedEndingToken != null) {
			int start = node.getStartPosition();
			int end = start + node.getLength() - 1;

			boolean flagParent = false;
			done : for (int i = this.insertedTokensKind.length - 1; i > -1 ; i--) {
				if(!this.insertedTokensFlagged[i] &&
						this.insertedTokensPosition[i] == end){
					this.insertedTokensFlagged[i] = true;
					for (int j = 0; j < expectedEndingToken.length; j++) {
						if(expectedEndingToken[j] == this.insertedTokensKind[i]) {
							node.setFlags(node.getFlags() | ASTNode.RECOVERED);
							break done;
						}
					}
					flagParent = true;
				}
			}

			if(flagParent) {
				ASTNode parent = node.getParent();
				while (parent != null) {
					parent.setFlags(node.getFlags() | ASTNode.RECOVERED);
					if((parent.getStartPosition() + parent.getLength() - 1) != end) {
						parent = null;
					} else {
						parent = parent.getParent();
					}
				}
			}
		}
		return true;
	}

	private boolean flagNodesWithInsertedTokensInside(ASTNode node) {
		int start = node.getStartPosition();
		int end = start + node.getLength() - 1;
		for (int i = 0; i < this.insertedTokensKind.length; i++) {
			if(!this.insertedTokensFlagged[i] &&
					start <= this.insertedTokensPosition[i] &&
					this.insertedTokensPosition[i] < end){
				node.setFlags(node.getFlags() | ASTNode.RECOVERED);
				this.insertedTokensFlagged[i] = true;
			}
		}
		return true;
	}

	private boolean markIncludedProblems(int start, int end) {
		boolean foundProblems = false;
		next: for (int i = 0, max = this.problems.length; i < max; i++) {
			CategorizedProblem problem = this.problems[i];

			if(this.usedOrIrrelevantProblems[i]) continue next;

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
					this.usedOrIrrelevantProblems[i] = true;
					continue next;

			}

			int problemStart = problem.getSourceStart();
			int problemEnd = problem.getSourceEnd();
			if ((start <= problemStart) && (problemStart <= end) ||
					(start <= problemEnd) && (problemEnd <= end)) {
				this.usedOrIrrelevantProblems[i] = true;
				foundProblems = true;
			}
		}
		return foundProblems;
	}

	@Override
	public void endVisit(ExpressionStatement node) {
		endVisitNode(node);
		if ((node.getFlags() & ASTNode.RECOVERED) == 0) return;
		Expression expression = node.getExpression();
		if (expression.getNodeType() == ASTNode.ASSIGNMENT) {
			Assignment assignment = (Assignment) expression;
			Expression rightHandSide = assignment.getRightHandSide();
			if (rightHandSide.getNodeType() == ASTNode.SIMPLE_NAME) {
				SimpleName simpleName = (SimpleName) rightHandSide;
				if (CharOperation.equals(RecoveryScanner.FAKE_IDENTIFIER, simpleName.getIdentifier().toCharArray())) {
					Expression expression2 =  assignment.getLeftHandSide();
					// unparent the expression to add it in the expression stateemnt
					expression2.setParent(null, null);
					expression2.setFlags(expression2.getFlags() | ASTNode.RECOVERED);
					node.setExpression(expression2);
				}
			}
		}
	}

	@Override
	public void endVisit(ForStatement node) {
		endVisitNode(node);
		List initializers = node.initializers();
		if (initializers.size() == 1) {
			Expression expression = (Expression) initializers.get(0);
			if (expression.getNodeType() == ASTNode.VARIABLE_DECLARATION_EXPRESSION) {
				VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression) expression;
				List fragments = variableDeclarationExpression.fragments();
				for (int i = 0, max = fragments.size(); i <max; i++) {
					VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(i);
					SimpleName simpleName = fragment.getName();
					if (CharOperation.equals(RecoveryScanner.FAKE_IDENTIFIER, simpleName.getIdentifier().toCharArray())) {
						fragments.remove(fragment);
						variableDeclarationExpression.setFlags(variableDeclarationExpression.getFlags() | ASTNode.RECOVERED);
					}
				}
			}
		}
	}

	@Override
	public void endVisit(VariableDeclarationStatement node) {
		endVisitNode(node);
		List fragments = node.fragments();
		for (int i = 0, max = fragments.size(); i <max; i++) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(i);
			Expression expression = fragment.getInitializer();
			if (expression == null) continue;
			if ((expression.getFlags() & ASTNode.RECOVERED) == 0) continue;
			if (expression.getNodeType() == ASTNode.SIMPLE_NAME) {
				SimpleName simpleName = (SimpleName) expression;
				if (CharOperation.equals(RecoveryScanner.FAKE_IDENTIFIER, simpleName.getIdentifier().toCharArray())) {
					fragment.setInitializer(null);
					fragment.setFlags(fragment.getFlags() | ASTNode.RECOVERED);
				}
			}
		}
	}

	@Override
	public void endVisit(NormalAnnotation node) {
		endVisitNode(node);
		// is inside diet part of the ast
		if(this.blockDepth < 1) {
			List values = node.values();
			int size = values.size();
			if (size > 0) {
				MemberValuePair lastMemberValuePair = (MemberValuePair)values.get(size - 1);

				int annotationEnd = node.getStartPosition() + node.getLength();
				int lastMemberValuePairEnd = lastMemberValuePair.getStartPosition() + lastMemberValuePair.getLength();
				if (annotationEnd == lastMemberValuePairEnd) {
					node.setFlags(node.getFlags() | ASTNode.RECOVERED);
				}
			}
		}
	}

	@Override
	public void endVisit(SingleMemberAnnotation node) {
		endVisitNode(node);
		// is inside diet part of the ast
		if(this.blockDepth < 1) {
			Expression value = node.getValue();
			int annotationEnd = node.getStartPosition() + node.getLength();
			int valueEnd = value.getStartPosition() + value.getLength();
			if (annotationEnd == valueEnd) {
				node.setFlags(node.getFlags() | ASTNode.RECOVERED);
			}
		}
	}
}
