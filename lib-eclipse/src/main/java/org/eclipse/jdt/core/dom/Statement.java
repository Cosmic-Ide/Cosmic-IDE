/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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

import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;

/**
 * Abstract base class of AST nodes that represent statements.
 * There are many kinds of statements.
 * <p>
 * The grammar combines both Statement and BlockStatement.</p>
 * <pre>
 * Statement:
 *    {@link AssertStatement},
 *    {@link Block},
 *    {@link BreakStatement},
 *    {@link ConstructorInvocation},
 *    {@link ContinueStatement},
 *    {@link DoStatement},
 *    {@link EmptyStatement},
 *    {@link EnhancedForStatement}
 *    {@link ExpressionStatement},
 *    {@link ForStatement},
 *    {@link IfStatement},
 *    {@link LabeledStatement},
 *    {@link ReturnStatement},
 *    {@link SuperConstructorInvocation},
 *    {@link SwitchCase},
 *    {@link SwitchStatement},
 *    {@link SynchronizedStatement},
 *    {@link ThrowStatement},
 *    {@link TryStatement},
 *    {@link TypeDeclarationStatement},
 *    {@link VariableDeclarationStatement},
 *    {@link WhileStatement}
 * </pre>
 *
 * @since 2.0
 */
public abstract class Statement extends ASTNode {

	/**
	 * The leading comment, or <code>null</code> if none.
	 * Defaults to none.
	 *
	 * @deprecated The leading comment feature was removed in 2.1.
	 */
	private String optionalLeadingComment = null;

	/**
	 * Creates a new AST node for a statement owned by the given AST.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	Statement(AST ast) {
		super(ast);
	}

	/**
	 * Returns the leading comment string, including the starting
	 * and ending comment delimiters, and any embedded line breaks.
	 * <p>
	 * A leading comment is a comment that appears before the statement.
	 * It may be either a traditional comment or an end-of-line comment.
	 * Traditional comments must begin with "/&#42;, may contain line breaks,
	 * and must end with "&#42;/. End-of-line comments must begin with "//",
	 * must end with a line delimiter (as per JLS 3.7), and must not contain
	 * line breaks.
	 * </p>
	 *
	 * @return the comment string, or <code>null</code> if none
	 * @deprecated This feature was removed in the 2.1 release because it was
	 * only a partial, and inadequate, solution to the issue of associating
	 * comments with statements. Furthermore, AST.parseCompilationUnit did not
	 * associate leading comments, making this moot. Clients that need to access
	 * comments preceding a statement should either consult the compilation
	 * unit's {@linkplain CompilationUnit#getCommentList() comment table}
	 * or use a scanner to reanalyze the source text immediately preceding
	 * the statement's source range.
	 */
	public String getLeadingComment() {
		return this.optionalLeadingComment;
	}

	/**
	 * Sets or clears the leading comment string. The comment
	 * string must include the starting and ending comment delimiters,
	 * and any embedded linebreaks.
	 * <p>
	 * A leading comment is a comment that appears before the statement.
	 * It may be either a traditional comment or an end-of-line comment.
	 * Traditional comments must begin with "/&#42;, may contain line breaks,
	 * and must end with "&#42;/. End-of-line comments must begin with "//"
	 * (as per JLS 3.7), and must not contain line breaks.
	 * </p>
	 * <p>
	 * Examples:
	 * <pre>
	 * <code>
	 * setLeadingComment("/&#42; traditional comment &#42;/");  // correct
	 * setLeadingComment("missing comment delimiters");  // wrong
	 * setLeadingComment("/&#42; unterminated traditional comment ");  // wrong
	 * setLeadingComment("/&#42; broken\n traditional comment &#42;/");  // correct
	 * setLeadingComment("// end-of-line comment\n");  // correct
	 * setLeadingComment("// end-of-line comment without line terminator");  // correct
	 * setLeadingComment("// broken\n end-of-line comment\n");  // wrong
	 * </code>
	 * </pre>
	 *
	 * @param comment the comment string, or <code>null</code> if none
	 * @exception IllegalArgumentException if the comment string is invalid
	 * @deprecated This feature was removed in the 2.1 release because it was
	 * only a partial, and inadequate, solution to the issue of associating
	 * comments with statements.
	 */
	public void setLeadingComment(String comment) {
		if (comment != null) {
			char[] source = comment.toCharArray();
			Scanner scanner = this.ast.scanner;
			scanner.resetTo(0, source.length);
			scanner.setSource(source);
			try {
				int token;
				boolean onlyOneComment = false;
				while ((token = scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
					switch(token) {
						case TerminalTokens.TokenNameCOMMENT_BLOCK :
						case TerminalTokens.TokenNameCOMMENT_JAVADOC :
						case TerminalTokens.TokenNameCOMMENT_LINE :
							if (onlyOneComment) {
								throw new IllegalArgumentException();
							}
							onlyOneComment = true;
							break;
						default:
							onlyOneComment = false;
					}
				}
				if (!onlyOneComment) {
					throw new IllegalArgumentException();
				}
			} catch (InvalidInputException e) {
				throw new IllegalArgumentException(e);
			}
		}
		// we do not consider the obsolete comment as a structureal property
		// but we protect them nevertheless
		checkModifiable();
		this.optionalLeadingComment = comment;
	}

	/**
	 * Copies the leading comment from the given statement.
	 *
	 * @param source the statement that supplies the leading comment
	 * @since 2.1
	 */
	void copyLeadingComment(Statement source) {
		setLeadingComment(source.getLeadingComment());
	}

	@Override
	int memSize() {
		int size = BASE_NODE_SIZE + 1 * 4 + stringSize(getLeadingComment());
		return size;
	}
}

