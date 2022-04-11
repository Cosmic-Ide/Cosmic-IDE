/*******************************************************************************
 * Copyright (c) 2018, 2022 Mateusz Matela and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mateusz Matela <mateusz.matela@gmail.com> - Initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.jdt.internal.formatter;

import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameLBRACE;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameRBRACE;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNamewhile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ModuleDeclaration;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

/** Implementation of the "Keep braced code on one line" feature. */
public class OneLineEnforcer extends ASTVisitor {
	private final TokenManager tm;
	private final DefaultCodeFormatterOptions options;

	public OneLineEnforcer(TokenManager tokenManager, DefaultCodeFormatterOptions options) {
		this.tm = tokenManager;
		this.options = options;
	}

	@Override
	public boolean preVisit2(ASTNode node) {
		boolean isMalformed = (node.getFlags() & ASTNode.MALFORMED) != 0;
		return !isMalformed;
	}

	@Override
	public void endVisit(TypeDeclaration node) {
		if (node.getParent().getLength() == 0)
			return; // this is a fake block created by parsing in statements mode
		tryKeepOnOneLine(node, node.getName(), node.bodyDeclarations(), this.options.keep_type_declaration_on_one_line);
	}

	@Override
	public void endVisit(EnumDeclaration node) {
		List<ASTNode> items = new ArrayList<>();
		items.addAll(node.bodyDeclarations());
		items.addAll(node.enumConstants());
		tryKeepOnOneLine(node, node.getName(), items, this.options.keep_enum_declaration_on_one_line);
	}

	@Override
	public void endVisit(AnnotationTypeDeclaration node) {
		tryKeepOnOneLine(node, node.getName(), node.bodyDeclarations(),
				this.options.keep_annotation_declaration_on_one_line);
	}

	@Override
	public void endVisit(RecordDeclaration node) {
		tryKeepOnOneLine(node, node.getName(), node.bodyDeclarations(),
				this.options.keep_record_declaration_on_one_line);
	}

	@Override
	public void endVisit(AnonymousClassDeclaration node) {
		if (node.getParent() instanceof EnumConstantDeclaration) {
			tryKeepOnOneLine(node, null, node.bodyDeclarations(),
					this.options.keep_enum_constant_declaration_on_one_line);
		} else {
			tryKeepOnOneLine(node, null, node.bodyDeclarations(),
					this.options.keep_anonymous_type_declaration_on_one_line);
		}
	}

	@Override
	public void endVisit(SwitchExpression node) {
		handleSwitchBody(node, node.getExpression(), node.statements());
	}

	@Override
	public void endVisit(SwitchStatement node) {
		handleSwitchBody(node, node.getExpression(), node.statements());
	}

	private void handleSwitchBody(ASTNode node, Expression expression, List<Statement> statements) {
		if (statements.isEmpty() || ((SwitchCase) statements.get(0)).isSwitchLabeledRule()) {
			List<Statement> items = statements.stream().filter(SwitchCase.class::isInstance)
					.collect(Collectors.toList());
			tryKeepOnOneLine(node, expression, items, this.options.keep_switch_body_block_on_one_line);
		}
	}

	@Override
	public void endVisit(Block node) {
		ASTNode parent = node.getParent();
		List<Statement> statements = node.statements();
		if (parent.getLength() == 0)
			return; // this is a fake block created by parsing in statements mode
		String oneLineOption;
		if (parent instanceof MethodDeclaration) {
			MethodDeclaration method = (MethodDeclaration) parent;
			oneLineOption = method.isCompactConstructor() ? this.options.keep_record_constructor_on_one_line
					: this.options.keep_method_body_on_one_line;
			if (this.options.keep_simple_getter_setter_on_one_line) {
				String name = method.getName().getIdentifier();
				Type returnType = method.getReturnType2();
				boolean returnsVoid = returnType instanceof PrimitiveType
						&& ((PrimitiveType) returnType).getPrimitiveTypeCode() == PrimitiveType.VOID;
				boolean isGetter = name.matches("(is|get)\\p{Lu}.*") //$NON-NLS-1$
						&& !method.isConstructor() && !returnsVoid && method.parameters().isEmpty()
						&& statements.size() == 1 && statements.get(0) instanceof ReturnStatement;
				boolean isSetter = name.matches("set\\p{Lu}.*") //$NON-NLS-1$
						&& !method.isConstructor() && returnsVoid && method.parameters().size() == 1
						&& statements.size() == 1 && statements.get(0) instanceof ExpressionStatement
						&& ((ExpressionStatement) statements.get(0)).getExpression() instanceof Assignment;
				if (isGetter || isSetter)
					oneLineOption = DefaultCodeFormatterConstants.ONE_LINE_ALWAYS;
			}
		} else if (parent instanceof IfStatement && ((IfStatement) parent).getElseStatement() == null) {
			oneLineOption = this.options.keep_if_then_body_block_on_one_line;
			if (this.options.keep_guardian_clause_on_one_line) {
				boolean isGuardian = statements.size() == 1 && (statements.get(0) instanceof ReturnStatement
						|| statements.get(0) instanceof ThrowStatement);
				// guard clause cannot start with a comment: https://bugs.eclipse.org/58565
				int openBraceIndex = this.tm.firstIndexIn(node, TokenNameLBRACE);
				isGuardian = isGuardian && !this.tm.get(openBraceIndex + 1).isComment();
				if (isGuardian)
					oneLineOption = DefaultCodeFormatterConstants.ONE_LINE_ALWAYS;
			}
		} else if (parent instanceof LambdaExpression) {
			oneLineOption = this.options.keep_lambda_body_block_on_one_line;
		} else if (parent instanceof ForStatement || parent instanceof EnhancedForStatement
				|| parent instanceof WhileStatement) {
			oneLineOption = this.options.keep_loop_body_block_on_one_line;
		} else if (parent instanceof DoStatement) {
			oneLineOption = this.options.keep_loop_body_block_on_one_line;
			int openBraceIndex = this.tm.firstIndexIn(node, TokenNameLBRACE);
			int closeBraceIndex = this.tm.lastIndexIn(node, TokenNameRBRACE);
			Token whileToken = this.tm.firstTokenAfter(node, TokenNamewhile);
			int lastIndex = whileToken.getLineBreaksBefore() == 0 ? this.tm.lastIndexIn(parent, -1) : closeBraceIndex;
			tryKeepOnOneLine(openBraceIndex, closeBraceIndex, lastIndex, statements, oneLineOption);
			return;
		} else if (isSwitchCaseWithArrow(node)) {
			oneLineOption = this.options.keep_switch_case_with_arrow_on_one_line;
		} else {
			oneLineOption = this.options.keep_code_block_on_one_line;
		}
		tryKeepOnOneLine(node, null, statements, oneLineOption);
	}

	private boolean isSwitchCaseWithArrow(Block node) {
		List<Statement> siblings;
		if (node.getParent() instanceof SwitchStatement) {
			siblings = ((SwitchStatement)node.getParent()).statements();
		} else if (node.getParent() instanceof SwitchExpression) {
			siblings = ((SwitchExpression)node.getParent()).statements();
		} else {
			return false;
		}
		return ((SwitchCase) siblings.get(0)).isSwitchLabeledRule();
	}

	@Override
	public void endVisit(ModuleDeclaration node) {
		tryKeepOnOneLine(node, node.getName(), node.moduleStatements(), this.options.keep_type_declaration_on_one_line);
	}

	private void tryKeepOnOneLine(ASTNode node, ASTNode nodeBeforeOpenBrace, List<? extends ASTNode> items,
			String oneLineOption) {
		int openBraceIndex = nodeBeforeOpenBrace == null ? this.tm.firstIndexIn(node, TokenNameLBRACE)
				: this.tm.firstIndexAfter(nodeBeforeOpenBrace, TokenNameLBRACE);
		int closeBraceIndex = this.tm.lastIndexIn(node, TokenNameRBRACE);
		tryKeepOnOneLine(openBraceIndex, closeBraceIndex, closeBraceIndex, items, oneLineOption);
	}

	private void tryKeepOnOneLine(int openBraceIndex, int closeBraceIndex, int lastIndex, List<? extends ASTNode> items,
			String oneLineOption) {
		if (DefaultCodeFormatterConstants.ONE_LINE_NEVER.equals(oneLineOption))
			return;
		if (DefaultCodeFormatterConstants.ONE_LINE_IF_EMPTY.equals(oneLineOption) && !items.isEmpty())
			return;
		if (DefaultCodeFormatterConstants.ONE_LINE_IF_SINGLE_ITEM.equals(oneLineOption) && items.size() > 1)
			return;
		if (DefaultCodeFormatterConstants.ONE_LINE_PRESERVE.equals(oneLineOption)
				&& this.tm.countLineBreaksBetween(this.tm.get(openBraceIndex), this.tm.get(lastIndex)) > 0)
			return;

		Set<Integer> breakIndexes = items.stream().map(n -> this.tm.firstIndexIn(n, -1)).collect(Collectors.toSet());
		breakIndexes.add(openBraceIndex + 1);
		breakIndexes.add(closeBraceIndex);
		Token prev = this.tm.get(openBraceIndex);
		int startPos = this.tm.getPositionInLine(openBraceIndex);
		int pos = startPos + this.tm.getLength(prev, startPos);
		for (int i = openBraceIndex + 1; i <= lastIndex; i++) {
			Token token = this.tm.get(i);
			int preexistingBreaks = this.tm.countLineBreaksBetween(prev, token);
			if (this.options.number_of_empty_lines_to_preserve > 0 && preexistingBreaks > 1)
				return; // blank line will be preserved
			boolean isSpace = prev.isSpaceAfter() || token.isSpaceBefore();
			if (prev.isComment() || token.isComment()) {
				if (preexistingBreaks > 0)
					return; // line break around a comment will be preserved
				char charBefore = this.tm.charAt(token.originalStart - 1);
				isSpace = isSpace || charBefore == ' ' || charBefore == '\t';
			}
			if (prev.getLineBreaksAfter() > 0 || token.getLineBreaksBefore() > 0) {
				if (!breakIndexes.contains(i))
					return; // extra line break within an item, can't remove it
				isSpace = isSpace || !(i == closeBraceIndex && i == openBraceIndex + 1);
			}
			if (isSpace)
				pos++;
			pos += this.tm.getLength(token, pos);
			prev = token;
		}
		if (!items.isEmpty()) {
			ASTNode itemsParent = items.get(0).getParent();
			if (itemsParent.getParent() instanceof LambdaExpression || itemsParent instanceof SwitchExpression
					|| itemsParent.getParent() instanceof SwitchExpression
					|| itemsParent.getParent() instanceof SwitchStatement) {
				// lambda/case body could be put in a wrapped line, so only check its own width
				// and re-wrap if necessary in WrapPreparator.handleOneLineEnforced()
				pos -= this.tm.get(openBraceIndex).getIndent();
			}
			if (pos > this.options.page_width)
				return; // line width limit exceeded
		}

		for (Integer i : breakIndexes) {
			prev = this.tm.get(i - 1);
			prev.clearLineBreaksAfter();
			Token token = this.tm.get(i);
			token.clearLineBreaksBefore();
			if (!items.isEmpty())
				token.spaceBefore();
		}
	}
}
