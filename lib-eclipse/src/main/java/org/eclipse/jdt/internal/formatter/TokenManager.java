/*******************************************************************************
 * Copyright (c) 2014, 2021 Mateusz Matela and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.formatter;

import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameCOMMENT_LINE;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameNotAToken;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameStringLiteral;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameTextBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.internal.formatter.Token.WrapMode;
import org.eclipse.jdt.internal.formatter.linewrap.CommentWrapExecutor;

/**
 * A helper class that can be used to easily access source code and find tokens on any position.
 * It also has some other methods that are useful on multiple stages of formatting.
 */
public class TokenManager implements Iterable<Token> {

	private static final Pattern COMMENT_LINE_ANNOTATION_PATTERN = Pattern.compile("^(\\s*\\*?\\s*)(@)"); //$NON-NLS-1$

	private final List<Token> tokens;
	private final String source;
	private final int tabSize;
	private final int tabChar;
	private final boolean wrapWithSpaces;

	final CommentWrapExecutor commentWrapper;

	private HashMap<Integer, Integer> tokenIndexToNLSAlign;
	private List<Token[]> formatOffTagPairs = new ArrayList<>();
	private int headerEndIndex = 0;

	public TokenManager(List<Token> tokens, String source, DefaultCodeFormatterOptions options) {
		this.tokens = tokens;
		this.source = source;
		this.tabSize = options.tab_size;
		this.tabChar = options.tab_char;
		this.wrapWithSpaces = options.use_tabs_only_for_leading_indentations;
		this.commentWrapper = new CommentWrapExecutor(this, options);
	}

	public TokenManager(List<Token> tokens, TokenManager parent) {
		this.tokens = tokens;
		this.source = parent.source;
		this.tabSize = parent.tabSize;
		this.tabChar = parent.tabChar;
		this.wrapWithSpaces = parent.wrapWithSpaces;
		this.commentWrapper = parent.commentWrapper;
	}

	public Token get(int index) {
		return this.tokens.get(index);
	}

	/**
	 * @return total number of tokens
	 */
	public int size() {
		return this.tokens.size();
	}

	/**
	 * Removes the token at given index.
	 * <p>Warning: never call this method after wrap policies have been added to tokens
	 * since wrap parent indexes may become invalid.
	 */
	public void remove(int tokenIndex) {
		this.tokens.remove(tokenIndex);
	}

	/**
	 * Adds given token at given index.
	 * <p>Warning: never call this method after wrap policies have been added to tokens
	 * since wrap parent indexes may become invalid.
	 */
	public void insert(int tokenIndex, Token token) {
		this.tokens.add(tokenIndex, token);
	}

	/**
	 * Gets token text with characters escaped as HTML entities where necessary.
	 * @param tokenIndex index of the token to get.
	 */
	public String toString(int tokenIndex) {
		return toString(get(tokenIndex));
	}

	/**
	 * Gets token text with characters escaped as HTML entities where necessary.
	 */
	public String toString(Token token) {
		if (token.isToEscape())
			return getEscapedTokenString(token);
		return token.toString(this.source);
	}

	/**
	 * @return part of the source code defined by given node's position and length.
	 */
	public String toString(ASTNode node) {
		return this.source.substring(node.getStartPosition(), node.getStartPosition() + node.getLength());
	}

	public String getSource() {
		return this.source;
	}

	public int indexOf(Token token) {
		int index = findIndex(token.originalStart, -1, false);
		if (get(index) != token)
			return -1;
		return index;
	}

	public char charAt(int sourcePosition) {
		return this.source.charAt(sourcePosition);
	}

	public int getSourceLength() {
		return this.source.length();
	}

	public int findIndex(int positionInSource, int tokenType, boolean forward) {
		// binary search
		int left = 0, right = size() - 1;
		while (left < right) {
			int index = (right + left) / 2;
			Token token = get(index);
			if (token.originalStart <= positionInSource && positionInSource <= token.originalEnd) {
				left = index;
				break;
			}
			if (token.originalEnd < positionInSource) {
				left = index + 1;
			} else {
				assert token.originalStart > positionInSource;
				right = index - 1;
			}
		}
		int index = left;
		if (!forward && get(index).originalStart > positionInSource)
			index--;
		if (forward && get(index).originalEnd < positionInSource)
			index++;
		while (tokenType >= 0 && get(index).tokenType != tokenType) {
			index += forward ? 1 : -1;
		}
		return index;
	}

	@Override
	public Iterator<Token> iterator() {
		return this.tokens.iterator();
	}

	public Stream<Token> stream() {
		return this.tokens.stream();
	}

	public int firstIndexIn(ASTNode node, int tokenType) {
		int index = findIndex(node.getStartPosition(), tokenType, true);
		assert tokenInside(node, index);
		return index;
	}

	public Token firstTokenIn(ASTNode node, int tokenType) {
		return get(firstIndexIn(node, tokenType));
	}

	public int lastIndexIn(ASTNode node, int tokenType) {
		int index = findIndex(node.getStartPosition() + node.getLength() - 1, tokenType, false);
		assert tokenInside(node, index);
		return index;
	}

	public Token lastTokenIn(ASTNode node, int tokenType) {
		return get(lastIndexIn(node, tokenType));
	}

	public int firstIndexAfter(ASTNode node, int tokenType) {
		return findIndex(node.getStartPosition() + node.getLength(), tokenType, true);
	}

	public Token firstTokenAfter(ASTNode node, int tokenType) {
		return get(firstIndexAfter(node, tokenType));
	}

	public int firstIndexBefore(ASTNode node, int tokenType) {
		return findIndex(node.getStartPosition() - 1, tokenType, false);
	}

	public Token firstTokenBefore(ASTNode node, int tokenType) {
		return get(firstIndexBefore(node, tokenType));
	}

	public int countLineBreaksBetween(Token previous, Token current) {
		int start = previous != null ? previous.originalEnd + 1 : 0;
		int end = current != null ? current.originalStart : this.source.length();
		return countLineBreaksBetween(this.source, start, end);
	}

	public int countLineBreaksBetween(String text, int startPosition, int endPosition) {
		int result = 0;
		for (int i = startPosition; i < endPosition; i++) {
			switch (text.charAt(i)) {
				case '\r':
					result++;
					if (i + 1 < endPosition && text.charAt(i + 1) == '\n')
						i++;
					break;
				case '\n':
					result++;
					if (i + 1 < endPosition && text.charAt(i + 1) == '\r')
						i++;
					break;
			}
		}
		return result;
	}

	private TokenTraverser positionInLineCounter = new TokenTraverser() {
		private boolean isNLSTagInLine = false;

		@Override
		protected boolean token(Token traversed, int index) {
			if (index == this.value) {
				this.isNLSTagInLine = false;
				return false;
			}
			if (traversed.hasNLSTag()) {
				assert traversed.tokenType == TokenNameStringLiteral;
				this.isNLSTagInLine = true;
			}
			if (traversed.getAlign() > 0)
				this.counter = traversed.getAlign();
			if (traversed.tokenType == TokenNameTextBlock) {
				List<Token> lines = traversed.getInternalStructure();
				if (lines == null) {
					this.counter = getLength(traversed, 0);
				} else {
					this.counter = traversed.getIndent() + lines.get(1).getIndent();
					this.counter += getLength(lines.get(lines.size() - 1), this.counter);
				}
			} else if (traversed.isComment()) {
				assert traversed.tokenType != TokenNameCOMMENT_LINE;
				this.counter = TokenManager.this.commentWrapper.wrapMultiLineComment(traversed, this.counter, true,
						this.isNLSTagInLine);
			} else {
				this.counter += getLength(traversed, this.counter);
			}
			if (isSpaceAfter())
				this.counter++;
			return true;
		}
	};

	public int getPositionInLine(int tokenIndex) {
		Token token = get(tokenIndex);
		// find the first token in line and calculate position of given token
		int firstTokenIndex = token.getLineBreaksBefore() > 0 ? tokenIndex : findFirstTokenInLine(tokenIndex);
		Token firstToken = get(firstTokenIndex);
		int startingPosition = toIndent(firstToken.getIndent(), firstToken.getWrapPolicy() != null);

		this.positionInLineCounter.value = tokenIndex;
		this.positionInLineCounter.counter = startingPosition;
		traverse(firstTokenIndex, this.positionInLineCounter);
		return Math.max(this.positionInLineCounter.counter, token.getAlign());
	}

	public int findSourcePositionInLine(int position) {
		int lineStartPosition = position;
		char c;
		while (lineStartPosition > 0 && (c = charAt(lineStartPosition)) != '\r' && c != '\n')
			lineStartPosition--;
		int positionInLine = getLength(lineStartPosition, position - 1, 0);
		return positionInLine;
	}

	private String getEscapedTokenString(Token token) {
		if (token.getLineBreaksBefore() > 0 && charAt(token.originalStart) == '@') {
			return "&#64;" + this.source.substring(token.originalStart + 1, token.originalEnd + 1); //$NON-NLS-1$
		} else if (token.tokenType == TokenNameNotAToken) {
			String text = token.toString(this.source);
			Matcher matcher = COMMENT_LINE_ANNOTATION_PATTERN.matcher(text);
			if (matcher.find()) {
				return matcher.group(1) + "&#64;" + text.substring(matcher.end(2)); //$NON-NLS-1$
			}
		}
		return token.toString(this.source);
	}

	/**
	 * @param token the token to measure
	 * @param startPosition position in line of the first character (affects tabs calculation)
	 * @return actual length of given token, considering tabs and escaping characters as HTML entities
	 */
	public int getLength(Token token, int startPosition) {
		int length = getLength(token.originalStart, token.originalEnd, startPosition);
		if (token.isToEscape()) {
			if (token.getLineBreaksBefore() > 0 && charAt(token.originalStart) == '@') {
				length += 4; // 4 = "&#64;".length() - "@".length()
			} else if (token.tokenType == TokenNameNotAToken) {
				Matcher matcher = COMMENT_LINE_ANNOTATION_PATTERN.matcher(token.toString(this.source));
				if (matcher.find()) {
					length += 4; // 4 = "&#64;".length() - "@".length()
				}
			}
		}
		return length;
	}

	/**
	 * Calculates the length of a source code fragment.
	 * @param originalStart the first position of the source code fragment
	 * @param originalEnd the last position of the source code fragment
	 * @param startPosition position in line of the first character (affects tabs calculation)
	 * @return length, considering tabs and escaping characters as HTML entities
	 */
	public int getLength(int originalStart, int originalEnd, int startPosition) {
		int position = startPosition;
		for (int i = originalStart; i <= originalEnd; i++) {
			switch (this.source.charAt(i)) {
				case '\t':
					if (this.tabSize > 0)
						position += this.tabSize - position % this.tabSize;
					break;
				case '\r':
				case '\n':
					position = 0;
					break;
				default:
					position++;
			}
		}
		return position - startPosition;
	}

	/**
	 * @param indent desired indentation (in positions, not in levels)
	 * @param isWrapped whether indented element is wrapped
	 * @return actual indentation that can be achieved with current settings
	 */
	public int toIndent(int indent, boolean isWrapped) {
		if (this.tabChar == DefaultCodeFormatterOptions.TAB && !(isWrapped && this.wrapWithSpaces)) {
			int tab = this.tabSize;
			if (tab <= 0)
				return 0;
			indent = ((indent + tab - 1) / tab) * tab;
		}
		return indent;
	}

	public int traverse(int startIndex, TokenTraverser traverser) {
		return traverser.traverse(this.tokens, startIndex);
	}

	public int findFirstTokenInLine(int startIndex) {
		return findFirstTokenInLine(startIndex, false, false);
	}

	public int findFirstTokenInLine(int startIndex, boolean includeWraps, boolean includeIndents) {
		Token previous = get(startIndex); // going backwards, previous has higher index than current
		for (int i = startIndex - 1; i >= 0; i--) {
			Token token = get(i);
			if (token.getLineBreaksAfter() > 0 || previous.getLineBreaksBefore() > 0) {
				boolean include = previous.getWrapPolicy() != null
						&& (previous.getWrapPolicy().wrapMode == WrapMode.BLOCK_INDENT ? includeIndents : includeWraps);
				if (!include)
					return i + 1;
			}
			previous = token;
		}
		return 0;
	}

	private boolean tokenInside(ASTNode node, int index) {
		return get(index).originalStart >= node.getStartPosition()
				&& get(index).originalEnd <= node.getStartPosition() + node.getLength();
	}

	public boolean isStringConcatenation(InfixExpression node) {
		if (!node.getOperator().equals(Operator.PLUS))
			return false;
		List<Expression> operands = new ArrayList<Expression>(node.extendedOperands());
		operands.add(node.getLeftOperand());
		operands.add(node.getRightOperand());
		for (Expression o : operands) {
			if (o instanceof StringLiteral)
				return true;
			if ((o instanceof InfixExpression) && isStringConcatenation((InfixExpression) o))
				return true;
		}
		return false;
	}

	public boolean isFake(TypeDeclaration node) {
		// might be a fake type created by parsing in class body mode
		return node.getName().getStartPosition() == -1;
	}

	public void addNLSAlignIndex(int index, int align) {
		if (this.tokenIndexToNLSAlign == null)
			this.tokenIndexToNLSAlign = new HashMap<Integer, Integer>();
		this.tokenIndexToNLSAlign.put(index, align);
	}

	public int getNLSAlign(int index) {
		if (this.tokenIndexToNLSAlign == null)
			return 0;
		Integer align = this.tokenIndexToNLSAlign.get(index);
		return align != null ? align : 0;
	}

	public void setHeaderEndIndex(int headerEndIndex) {
		this.headerEndIndex = headerEndIndex;
	}

	public boolean isInHeader(int tokenIndex) {
		return tokenIndex < this.headerEndIndex;
	}

	public void addDisableFormatTokenPair(Token formatOffTag, Token formatOnTag) {
		this.formatOffTagPairs.add(new Token[] { formatOffTag, formatOnTag });
	}

	public List<Token[]> getDisableFormatTokenPairs() {
		return this.formatOffTagPairs;
	}
}
