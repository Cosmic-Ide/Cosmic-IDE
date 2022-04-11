/*******************************************************************************
 * Copyright (c) 2014, 2022 Mateusz Matela and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mateusz Matela <mateusz.matela@gmail.com> - [formatter] Formatter does not format Java code correctly, especially when max line width is set - https://bugs.eclipse.org/303519
 *     Mateusz Matela <mateusz.matela@gmail.com> - [formatter] IndexOutOfBoundsException in TokenManager - https://bugs.eclipse.org/462945
 *     Mateusz Matela <mateusz.matela@gmail.com> - [formatter] follow up bug for comments - https://bugs.eclipse.org/458208
 *     IBM Corporation - DOM AST changes for JEP 354
 *******************************************************************************/
package org.eclipse.jdt.internal.formatter;

import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameAT;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameCOLON;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameCOMMA;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameCOMMENT_JAVADOC;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameLBRACE;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameRBRACE;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameSEMICOLON;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameTextBlock;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameelse;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNamefinally;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNamewhile;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ModuleDeclaration;
import org.eclipse.jdt.core.dom.ModuleDirective;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TextBlock;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.YieldStatement;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions.Alignment;
import org.eclipse.jdt.internal.formatter.Token.WrapMode;
import org.eclipse.jdt.internal.formatter.Token.WrapPolicy;

public class LineBreaksPreparator extends ASTVisitor {
	final private TokenManager tm;
	final private DefaultCodeFormatterOptions options;

	public LineBreaksPreparator(TokenManager tokenManager, DefaultCodeFormatterOptions options) {
		this.tm = tokenManager;
		this.options = options;
	}

	@Override
	public boolean preVisit2(ASTNode node) {
		boolean isMalformed = (node.getFlags() & ASTNode.MALFORMED) != 0;
		return !isMalformed;
	}

	@Override
	public boolean visit(CompilationUnit node) {
		List<ImportDeclaration> imports = node.imports();
		if (!imports.isEmpty() && this.tm.firstIndexIn(imports.get(0), -1) > 0)
			putBlankLinesBefore(imports.get(0), this.options.blank_lines_before_imports);

		for (int i = 1; i < imports.size(); i++) {
			int from = this.tm.lastIndexIn(imports.get(i - 1), -1);
			int to = this.tm.firstIndexIn(imports.get(i), -1);
			for (int j = from; j < to; j++) {
				Token token1 = this.tm.get(j);
				Token token2 = this.tm.get(j + 1);
				if (this.tm.countLineBreaksBetween(token1, token2) > 1)
					putBlankLinesAfter(token1, this.options.blank_lines_between_import_groups);
			}
		}

		List<AnnotationTypeDeclaration> types = node.types();
		if (!types.isEmpty()) {
			if (!imports.isEmpty())
				putBlankLinesBefore(types.get(0), this.options.blank_lines_after_imports);
			for (int i = 1; i < types.size(); i++)
				putBlankLinesBefore(types.get(i), this.options.blank_lines_between_type_declarations);
		}
		return true;
	}

	@Override
	public boolean visit(PackageDeclaration node) {
		if (node.getJavadoc() == null) {
			putBlankLinesBefore(node, this.options.blank_lines_before_package);
		} else {
			putBlankLinesAfter(this.tm.lastTokenIn(node.getJavadoc(), -1), this.options.blank_lines_before_package);
		}

		handleAnnotations(node.annotations(), this.options.insert_new_line_after_annotation_on_package);
		putBlankLinesAfter(this.tm.lastTokenIn(node, -1), this.options.blank_lines_after_package);
		return true;
	}

	@Override
	public boolean visit(ImportDeclaration node) {
		breakLineBefore(node);
		return true;
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		handleBodyDeclarations(node.bodyDeclarations());

		if (this.tm.isFake(node))
			return true;

		breakLineBefore(node);
		handleAnnotations(node.modifiers(), this.options.insert_new_line_after_annotation_on_type);
		handleBracedCode(node, node.getName(), this.options.brace_position_for_type_declaration,
				this.options.indent_body_declarations_compare_to_type_header);
		return true;
	}

	private void handleBodyDeclarations(List<BodyDeclaration> bodyDeclarations) {
		BodyDeclaration previous = null;
		for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
			int blankLines = 0;
			if (previous == null) {
				blankLines = this.options.blank_lines_before_first_class_body_declaration;
			} else if (!sameChunk(previous, bodyDeclaration)) {
				blankLines = this.options.blank_lines_before_new_chunk;
			} else if (bodyDeclaration instanceof FieldDeclaration) {
				blankLines = this.options.blank_lines_before_field;
			} else if (bodyDeclaration instanceof AbstractTypeDeclaration) {
				blankLines = this.options.blank_lines_before_member_type;
			} else if (bodyDeclaration instanceof MethodDeclaration) {
				blankLines = ((MethodDeclaration) bodyDeclaration).getBody() == null
						&& ((MethodDeclaration) previous).getBody() == null
								? this.options.blank_lines_before_abstract_method
								: this.options.blank_lines_before_method;
			} else if (bodyDeclaration instanceof AnnotationTypeMemberDeclaration) {
				blankLines = this.options.blank_lines_before_method;
			}
			putBlankLinesBefore(bodyDeclaration, blankLines);
			previous = bodyDeclaration;
		}
		if (previous != null) {
			ASTNode parent = previous.getParent();
			if (!(parent instanceof TypeDeclaration && this.tm.isFake((TypeDeclaration) parent))) {
				Token lastToken = this.tm.lastTokenIn(parent, -1);
				putBlankLinesBefore(lastToken, this.options.blank_lines_after_last_class_body_declaration);
			}
		}
	}

	private boolean sameChunk(BodyDeclaration bd1, BodyDeclaration bd2) {
		if (bd1.getClass().equals(bd2.getClass()))
			return true;
		if (bd1 instanceof AbstractTypeDeclaration && bd2 instanceof AbstractTypeDeclaration)
			return true;
		if ((bd1 instanceof FieldDeclaration || bd1 instanceof Initializer)
				&& (bd2 instanceof FieldDeclaration || bd2 instanceof Initializer))
			return true; // special case: initializers are often related to fields, don't separate
		return false;
	}

	@Override
	public boolean visit(EnumDeclaration node) {
		handleAnnotations(node.modifiers(), this.options.insert_new_line_after_annotation_on_type);
		handleBracedCode(node, node.getName(), this.options.brace_position_for_enum_declaration,
				this.options.indent_body_declarations_compare_to_enum_declaration_header);

		List<BodyDeclaration> declarations = node.bodyDeclarations();
		List<EnumConstantDeclaration> enumConstants = node.enumConstants();
		if (!declarations.isEmpty()) {
			if (!enumConstants.isEmpty()) {
				declarations = new ArrayList<>(declarations);
				declarations.add(0, enumConstants.get(0));
			}
			handleBodyDeclarations(declarations);
		}

		for (int i = 0; i < enumConstants.size(); i++) {
			EnumConstantDeclaration declaration = enumConstants.get(i);
			if (declaration.getJavadoc() != null)
				this.tm.firstTokenIn(declaration, TokenNameCOMMENT_JAVADOC).breakBefore();
			if (declaration.getAnonymousClassDeclaration() != null && i < enumConstants.size() - 1)
				this.tm.firstTokenAfter(declaration, TokenNameCOMMA).breakAfter();
		}

		// put breaks after semicolons
		int index = enumConstants.isEmpty() ? this.tm.firstIndexAfter(node.getName(), TokenNameLBRACE) + 1
				: this.tm.firstIndexAfter(enumConstants.get(enumConstants.size() - 1), -1);
		for (;; index++) {
			Token token = this.tm.get(index);
			if (token.isComment())
				continue;
			if (token.tokenType == TokenNameSEMICOLON)
				token.breakAfter();
			else
				break;
		}
		return true;
	}

	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		handleAnnotations(node.modifiers(), this.options.insert_new_line_after_annotation_on_type);
		handleBracedCode(node, node.getName(), this.options.brace_position_for_annotation_type_declaration,
				this.options.indent_body_declarations_compare_to_annotation_declaration_header);

		handleBodyDeclarations(node.bodyDeclarations());
		if (node.getModifiers() == 0)
			this.tm.firstTokenBefore(node.getName(), TokenNameAT).breakBefore();
		return true;
	}

	@Override
	public boolean visit(AnonymousClassDeclaration node) {
		if (node.getParent() instanceof EnumConstantDeclaration) {
			handleBracedCode(node, null, this.options.brace_position_for_enum_constant,
					this.options.indent_body_declarations_compare_to_enum_constant_header);
		} else {
			handleBracedCode(node, null, this.options.brace_position_for_anonymous_type_declaration,
					this.options.indent_body_declarations_compare_to_type_header);
		}
		handleBodyDeclarations(node.bodyDeclarations());
		return true;
	}

	@Override
	public boolean visit(RecordDeclaration node) {
		handleAnnotations(node.modifiers(), this.options.insert_new_line_after_annotation_on_type);
		handleBracedCode(node, node.getName(), this.options.brace_position_for_record_declaration,
				this.options.indent_body_declarations_compare_to_record_header);
		handleBodyDeclarations(node.bodyDeclarations());
		return true;
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		handleAnnotations(node.modifiers(), this.options.insert_new_line_after_annotation_on_method);
		if (node.getBody() == null)
			return true;

		String bracePosition = node.isCompactConstructor() ? this.options.brace_position_for_record_constructor
				: node.isConstructor() ? this.options.brace_position_for_constructor_declaration
						: this.options.brace_position_for_method_declaration;
		handleBracedCode(node.getBody(), null, bracePosition, this.options.indent_statements_compare_to_body,
				this.options.blank_lines_at_beginning_of_method_body, this.options.blank_lines_at_end_of_method_body);

		return true;
	}

	@Override
	public boolean visit(Block node) {
		List<Statement> statements = node.statements();
		for (Statement statement : statements) {
			if (this.options.put_empty_statement_on_new_line || !(statement instanceof EmptyStatement))
				breakLineBefore(statement);
		}
		ASTNode parent = node.getParent();
		if (parent.getLength() == 0)
			return true; // this is a fake block created by parsing in statements mode
		if (parent instanceof MethodDeclaration)
			return true; // braces have been handled in #visit(MethodDeclaration)

		String bracePosition = this.options.brace_position_for_block;
		if (parent instanceof SwitchStatement) {
			List<Statement> siblings = ((SwitchStatement) parent).statements();
			int blockPosition = siblings.indexOf(node);
			boolean isFirstInCase = blockPosition > 0 && (siblings.get(blockPosition - 1) instanceof SwitchCase);
			if (isFirstInCase)
				bracePosition = this.options.brace_position_for_block_in_case;
		} else if (parent instanceof LambdaExpression) {
			bracePosition = this.options.brace_position_for_lambda_body;
		}
		handleBracedCode(node, null, bracePosition, this.options.indent_statements_compare_to_block,
				this.options.blank_lines_at_beginning_of_code_block, this.options.blank_lines_at_end_of_code_block);

		if (parent instanceof Block) {
			blankLinesAroundBlock(node, ((Block) parent).statements());
		} else if (parent instanceof Statement && parent.getParent() instanceof Block) {
			blankLinesAroundBlock(parent, ((Block) parent.getParent()).statements());
		}

		return true;
	}

	private void blankLinesAroundBlock(ASTNode blockStatement, List<ASTNode> siblings) {
		putBlankLinesBefore(blockStatement, this.options.blank_lines_before_code_block);
		if (!this.options.put_empty_statement_on_new_line) {
			int blockIndex = siblings.indexOf(blockStatement);
			if (blockIndex + 1 < siblings.size() && siblings.get(blockIndex + 1) instanceof EmptyStatement)
				return;
		}
		putBlankLinesAfter(this.tm.lastTokenIn(blockStatement, -1), this.options.blank_lines_after_code_block);
	}

	@Override
	public boolean visit(SwitchStatement node) {
		handleBracedCode(node, node.getExpression(), this.options.brace_position_for_switch,
				this.options.indent_switchstatements_compare_to_switch,
				this.options.blank_lines_at_beginning_of_code_block, this.options.blank_lines_at_end_of_code_block);

		List<Statement> statements = node.statements();
		doSwitchStatementsIndentation(node, statements);
		doSwitchStatementsLineBreaks(statements);

		if (node.getParent() instanceof Block)
			blankLinesAroundBlock(node, ((Block) node.getParent()).statements());

		return true;
	}

	@Override
	public boolean visit(SwitchExpression node) {
		handleBracedCode(node, node.getExpression(), this.options.brace_position_for_switch,
				this.options.indent_switchstatements_compare_to_switch,
				this.options.blank_lines_at_beginning_of_code_block, this.options.blank_lines_at_end_of_code_block);

		List<Statement> statements = node.statements();
		doSwitchStatementsIndentation(node, statements);
		doSwitchStatementsLineBreaks(statements);

		return true;
	}

	private void doSwitchStatementsIndentation(ASTNode switchNode, List<Statement> statements) {
		if (this.options.indent_switchstatements_compare_to_cases) {
			int nonBreakStatementEnd = -1;
			for (Statement statement : statements) {
				boolean isBreaking = isSwitchBreakingStatement(statement);
				if (isBreaking && !(statement instanceof Block))
					adjustEmptyLineAfter(this.tm.lastIndexIn(statement, -1), -1);
				if (statement instanceof SwitchCase) {
					if (nonBreakStatementEnd >= 0) {
						// indent only comments between previous and current statement
						this.tm.get(nonBreakStatementEnd + 1).indent();
						this.tm.firstTokenIn(statement, -1).unindent();
					}
				} else if (!(statement instanceof BreakStatement || statement instanceof YieldStatement
						|| statement instanceof Block)) {
					indent(statement);
				}
				nonBreakStatementEnd = isBreaking ? -1 : this.tm.lastIndexIn(statement, -1);
			}
			if (nonBreakStatementEnd >= 0) {
				// indent comments between last statement and closing brace
				this.tm.get(nonBreakStatementEnd + 1).indent();
				this.tm.lastTokenIn(switchNode, TokenNameRBRACE).unindent();
			}
		}
		if (this.options.indent_breaks_compare_to_cases) {
			for (Statement statement : statements) {
				if (statement instanceof BreakStatement || statement instanceof YieldStatement)
					indent(statement);
			}
		}
	}

	private void doSwitchStatementsLineBreaks(List<Statement> statements) {
		boolean arrowMode = statements.stream()
				.anyMatch(s -> s instanceof SwitchCase &&((SwitchCase) s).isSwitchLabeledRule());
		Statement previous = null;
		for (Statement statement : statements) {
			boolean skip = statement instanceof Block // will add break in visit(Block) if necessary
					|| (arrowMode && !(statement instanceof SwitchCase))
					|| (statement instanceof EmptyStatement && !this.options.put_empty_statement_on_new_line);
			if (!skip) {
				boolean newGroup = !arrowMode && statement instanceof SwitchCase && isSwitchBreakingStatement(previous);
				int blankLines = newGroup ? this.options.blank_lines_between_statement_groups_in_switch : 0;
				putBlankLinesBefore(statement, blankLines);
			}
			previous = statement;
		}
	}

	private boolean isSwitchBreakingStatement(Statement statement) {
		return statement instanceof BreakStatement || statement instanceof ReturnStatement
				|| statement instanceof ContinueStatement || statement instanceof ThrowStatement
				|| statement instanceof YieldStatement || statement instanceof Block;
	}

	@Override
	public boolean visit(DoStatement node) {
		Statement body = node.getBody();
		boolean sameLine = this.options.keep_simple_do_while_body_on_same_line;
		if (!sameLine)
			handleLoopBody(body);
		if (this.options.insert_new_line_before_while_in_do_statement
				|| (!(body instanceof Block) && !(body instanceof EmptyStatement) && !sameLine)) {
			Token whileToken = this.tm.firstTokenBefore(node.getExpression(), TokenNamewhile);
			whileToken.breakBefore();
		}
		return true;
	}

	@Override
	public boolean visit(LabeledStatement node) {
		if (this.options.insert_new_line_after_label)
			this.tm.firstTokenIn(node, TokenNameCOLON).breakAfter();
		return true;
	}

	@Override
	public boolean visit(ArrayInitializer node) {
		int openBraceIndex = this.tm.firstIndexIn(node, TokenNameLBRACE);
		int closeBraceIndex = this.tm.lastIndexIn(node, TokenNameRBRACE);

		boolean isEmpty = openBraceIndex + 1 == closeBraceIndex;
		if (isEmpty) {
			adjustEmptyLineAfter(openBraceIndex, this.options.continuation_indentation_for_array_initializer);
			closeBraceIndex = this.tm.lastIndexIn(node, TokenNameRBRACE);
		}

		Token openBraceToken = this.tm.get(openBraceIndex);
		Token closeBraceToken = this.tm.get(closeBraceIndex);

		if (!(node.getParent() instanceof ArrayInitializer)) {
			Token afterOpenBraceToken = this.tm.get(openBraceIndex + 1);
			for (int i = 0; i < this.options.continuation_indentation_for_array_initializer; i++) {
				afterOpenBraceToken.indent();
				closeBraceToken.unindent();
			}
		}

		if (!isEmpty || !this.options.keep_empty_array_initializer_on_one_line)
			handleBracePosition(openBraceToken, closeBraceIndex, this.options.brace_position_for_array_initializer);

		if (!isEmpty) {
			if (this.options.insert_new_line_after_opening_brace_in_array_initializer)
				openBraceToken.breakAfter();
			if (this.options.insert_new_line_before_closing_brace_in_array_initializer)
				closeBraceToken.breakBefore();
		}
		return true;
	}

	@Override
	public boolean visit(VariableDeclarationStatement node) {
		handleAnnotations(node.modifiers(), this.options.insert_new_line_after_annotation_on_local_variable);
		return true;
	}

	@Override
	public boolean visit(SingleVariableDeclaration node) {
		handleAnnotations(node.modifiers(),
				node.getParent() instanceof EnhancedForStatement
						? this.options.insert_new_line_after_annotation_on_local_variable
						: this.options.insert_new_line_after_annotation_on_parameter);
		return true;
	}

	@Override
	public boolean visit(VariableDeclarationExpression node) {
		handleAnnotations(node.modifiers(), this.options.insert_new_line_after_annotation_on_local_variable);
		return true;
	}

	@Override
	public boolean visit(FieldDeclaration node) {
		handleAnnotations(node.modifiers(), this.options.insert_new_line_after_annotation_on_field);
		return true;
	}

	@Override
	public boolean visit(AnnotationTypeMemberDeclaration node) {
		handleAnnotations(node.modifiers(), this.options.insert_new_line_after_annotation_on_method);
		return true;
	}

	@Override
	public boolean visit(EnumConstantDeclaration node) {
		handleAnnotations(node.modifiers(), this.options.insert_new_line_after_annotation_on_enum_constant);
		return true;
	}

	private void handleAnnotations(List<? extends IExtendedModifier> modifiers, boolean breakAfter) {
		Annotation last = null;
		int i;
		for (i = 0; i < modifiers.size(); i++) {
			if (modifiers.get(i).isModifier())
				break;
			last = (Annotation) modifiers.get(i);
		}
		if (last != null && breakAfter) {
			this.tm.lastTokenIn(last, -1).breakAfter();
		}

		if (i < modifiers.size()) {
			// any annotations following other modifiers will be associated with declaration type
			handleAnnotations(modifiers.subList(i + 1, modifiers.size()),
					this.options.insert_new_line_after_type_annotation);
		}
	}

	@Override
	public boolean visit(WhileStatement node) {
		if (!this.options.keep_simple_while_body_on_same_line)
			handleLoopBody(node.getBody());
		return true;
	}

	@Override
	public boolean visit(ForStatement node) {
		if (!this.options.keep_simple_for_body_on_same_line)
			handleLoopBody(node.getBody());
		return true;
	}

	@Override
	public boolean visit(EnhancedForStatement node) {
		if (!this.options.keep_simple_for_body_on_same_line)
			handleLoopBody(node.getBody());
		return true;
	}

	private void handleLoopBody(Statement body) {
		if (body instanceof Block)
			return;
		if (body instanceof EmptyStatement && !this.options.put_empty_statement_on_new_line
				&& !(body.getParent() instanceof IfStatement))
			return;
		breakLineBefore(body);
		adjustEmptyLineAfter(this.tm.lastIndexIn(body, -1), -1);
		indent(body);
	}

	@Override
	public boolean visit(IfStatement node) {
		Statement elseNode = node.getElseStatement();
		Statement thenNode = node.getThenStatement();
		if (elseNode != null) {
			if (this.options.insert_new_line_before_else_in_if_statement || !(thenNode instanceof Block))
				this.tm.firstTokenBefore(elseNode, TokenNameelse).breakBefore();

			boolean keepElseOnSameLine = (this.options.keep_else_statement_on_same_line)
					|| (this.options.compact_else_if && (elseNode instanceof IfStatement));
			if (!keepElseOnSameLine)
				handleLoopBody(elseNode);
		}

		boolean keepThenOnSameLine = this.options.keep_then_statement_on_same_line
				|| (this.options.keep_simple_if_on_one_line && elseNode == null);
		if (!keepThenOnSameLine)
			handleLoopBody(thenNode);

		return true;
	}

	@Override
	public boolean visit(TryStatement node) {
		if (node.getFinally() != null && this.options.insert_new_line_before_finally_in_try_statement) {
			this.tm.firstTokenBefore(node.getFinally(), TokenNamefinally).breakBefore();
		}
		return true;
	}

	@Override
	public boolean visit(CatchClause node) {
		if (this.options.insert_new_line_before_catch_in_try_statement)
			breakLineBefore(node);
		return true;
	}

	@Override
	public boolean visit(ModuleDeclaration node) {
		// using settings for type declaration and fields for now, add new settings if necessary
		breakLineBefore(node);
		List<ModuleDirective> statements = node.moduleStatements();
		handleBracedCode(node, node.getName(), this.options.brace_position_for_type_declaration,
				this.options.indent_body_declarations_compare_to_type_header,
				statements.isEmpty() ? 0 : this.options.blank_lines_before_first_class_body_declaration,
				statements.isEmpty() ? 0 : this.options.blank_lines_after_last_class_body_declaration);

		ModuleDirective previous = null;
		for (ModuleDirective statement : statements) {
			if (previous != null) {
				boolean cameChunk = previous.getClass().equals(statement.getClass());
				putBlankLinesBefore(statement,
						cameChunk ? this.options.blank_lines_before_field : this.options.blank_lines_before_new_chunk);
			}
			previous = statement;
		}
		return true;
	}

	@Override
	public boolean visit(TextBlock node) {
		int indentOption = this.options.text_block_indentation;
		if (indentOption == Alignment.M_INDENT_PRESERVE)
			return true;
		Token block = this.tm.firstTokenIn(node, TokenNameTextBlock);
		ArrayList<Token> lines = new ArrayList<>();
		lines.add(new Token(block.originalStart, block.originalStart + 2, 0)); // first line; """
		int incidentalWhitespace = Integer.MAX_VALUE;
		int blankLines = -1; // will go to 0 on line break after first line
		int i = block.originalStart + 3;
		while (i <= block.originalEnd) {
			int lineStart = i;
			int firstNonBlank = -1;
			int lastNonBlank = -1;
			while (i <= block.originalEnd) {
				char c = this.tm.charAt(i++);
				if (c == '\r' || c == '\n') {
					char c2 = this.tm.charAt(i);
					if ((c2 == '\r' || c2 == '\n') && c2 != c)
						i++;
					break;
				}
				if (c != ' ' && c != '\t') {
					if (firstNonBlank == -1)
						firstNonBlank = i - 1;
					lastNonBlank = i - 1;
				}
			}
			if (firstNonBlank != -1) {
				Token line = new Token(lineStart, lastNonBlank, 0);
				line.putLineBreaksBefore(blankLines + 1);
				blankLines = 0;
				lines.add(line);
				incidentalWhitespace = Math.min(incidentalWhitespace, firstNonBlank - lineStart);
			} else {
				blankLines++;
			}
		}
		WrapPolicy wrapPolicy = new WrapPolicy(WrapMode.DISABLED, 0, -1, 0, 0, 1, false, false);
		for (i = 1; i < lines.size(); i++) {
			Token t = lines.get(i);
			Token line = new Token(t, t.originalStart + incidentalWhitespace, t.originalEnd, TokenNameTextBlock);
			line.setWrapPolicy(wrapPolicy);
			lines.set(i, line);
		}
		block.setInternalStructure(lines);
		return true;
	}

	private void breakLineBefore(ASTNode node) {
		this.tm.firstTokenIn(node, -1).breakBefore();
	}

	private void putBlankLinesBefore(ASTNode node, int linesCount) {
		int index = this.tm.firstIndexIn(node, -1);
		while (index > 0 && this.tm.get(index - 1).tokenType == TokenNameCOMMENT_JAVADOC)
			index--;
		putBlankLinesBefore(this.tm.get(index), linesCount);
	}

	private void putBlankLinesBefore(Token token, int linesCount) {
		if (linesCount >= 0) {
			token.putLineBreaksBefore(linesCount + 1);
		} else {
			token.putLineBreaksBefore(~linesCount + 1);
			token.setPreserveLineBreaksBefore(false);
		}
	}

	private void putBlankLinesAfter(Token token, int linesCount) {
		if (linesCount >= 0) {
			token.putLineBreaksAfter(linesCount + 1);
		} else {
			token.putLineBreaksAfter(~linesCount + 1);
			token.setPreserveLineBreaksAfter(false);
		}
	}

	private void handleBracedCode(ASTNode node, ASTNode nodeBeforeOpenBrace, String bracePosition, boolean indentBody) {
		handleBracedCode(node, nodeBeforeOpenBrace, bracePosition, indentBody, 0, 0);
	}

	private void handleBracedCode(ASTNode node, ASTNode nodeBeforeOpenBrace, String bracePosition, boolean indentBody,
			int blankLinesAfterOpeningBrace, int blankLinesBeforeClosingBrace) {
		int openBraceIndex = nodeBeforeOpenBrace == null
				? this.tm.firstIndexIn(node, TokenNameLBRACE)
				: this.tm.firstIndexAfter(nodeBeforeOpenBrace, TokenNameLBRACE);
		int closeBraceIndex = this.tm.lastIndexIn(node, TokenNameRBRACE);
		Token openBraceToken = this.tm.get(openBraceIndex);
		Token closeBraceToken = this.tm.get(closeBraceIndex);
		handleBracePosition(openBraceToken, closeBraceIndex, bracePosition);

		putBlankLinesAfter(openBraceToken, blankLinesAfterOpeningBrace);
		putBlankLinesBefore(closeBraceToken, blankLinesBeforeClosingBrace);

		if (indentBody) {
			adjustEmptyLineAfter(openBraceIndex, 1);
			this.tm.get(openBraceIndex + 1).indent();
			closeBraceToken.unindent();
		}
	}

	private void handleBracePosition(Token openBraceToken, int closeBraceIndex, String bracePosition) {
		if (bracePosition.equals(DefaultCodeFormatterConstants.NEXT_LINE)) {
			openBraceToken.breakBefore();
		} else if (bracePosition.equals(DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED)) {
			openBraceToken.breakBefore();
			openBraceToken.indent();
			if (closeBraceIndex + 1 < this.tm.size())
				this.tm.get(closeBraceIndex + 1).unindent();
		} else if (bracePosition.equals(DefaultCodeFormatterConstants.NEXT_LINE_ON_WRAP)) {
			openBraceToken.setNextLineOnWrap();
		}
	}

	private void adjustEmptyLineAfter(int tokenIndex, int indentationAdjustment) {
		if (tokenIndex + 1 >= this.tm.size())
			return;
		Token token = this.tm.get(tokenIndex);
		Token next = this.tm.get(tokenIndex + 1);
		if (this.tm.countLineBreaksBetween(token, next) < 2 || !this.options.indent_empty_lines)
			return;

		next.setEmptyLineIndentAdjustment(indentationAdjustment * this.options.indentation_size);
	}

	private void indent(ASTNode node) {
		int startIndex = this.tm.firstIndexIn(node, -1);
		while (startIndex > 0 && this.tm.get(startIndex - 1).isComment())
			startIndex--;
		this.tm.get(startIndex).indent();
		int lastIndex = this.tm.lastIndexIn(node, -1);
		if (lastIndex + 1 < this.tm.size())
			this.tm.get(lastIndex + 1).unindent();
	}

	public void finishUp() {
		// the visits only noted where indents increase and decrease,
		// now prepare actual indent values
		int currentIndent = this.options.initial_indentation_level;
		for (Token token : this.tm) {
			currentIndent += token.getIndent();
			token.setIndent(currentIndent * this.options.indentation_size);
		}
	}
}
