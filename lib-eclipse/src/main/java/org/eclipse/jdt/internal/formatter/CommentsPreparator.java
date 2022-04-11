/*******************************************************************************
 * Copyright (c) 2014, 2020 Mateusz Matela and others.
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
 *     Mateusz Matela <mateusz.matela@gmail.com> - [formatter] Bad line breaking in Eclipse javadoc comments - https://bugs.eclipse.org/348338
 *     Lars Vogel <Lars.Vogel@vogella.com> - Contributions for
 *     						Bug 473178
 *******************************************************************************/
package org.eclipse.jdt.internal.formatter;

import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameCOMMENT_BLOCK;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameCOMMENT_JAVADOC;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameCOMMENT_LINE;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameNotAToken;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameStringLiteral;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameWHITESPACE;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNamepackage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.jdt.internal.formatter.Token.WrapMode;
import org.eclipse.jdt.internal.formatter.Token.WrapPolicy;

public class CommentsPreparator extends ASTVisitor {

	public static final int COMMENT_LINE_SEPARATOR_LENGTH = 3;

	private final static Pattern NLS_TAG_PATTERN = Pattern.compile("//\\$NON-NLS-([0-9]+)\\$"); //$NON-NLS-1$
	private final static Pattern STRING_LITERAL_PATTERN = Pattern.compile("\".*?(\\\\(\\\\\\\\)*\".*?)*\""); //$NON-NLS-1$
	private final static Pattern HTML_TAG_PATTERN;
	private final static Pattern HTML_ATTRIBUTE_PATTERN;
	static {
		String formatCodeTags = "(pre)"; //$NON-NLS-1$
		String separateLineTags = "(dl|hr|nl|p|ul|ol|table|tr)"; //$NON-NLS-1$
		String breakBeforeTags = "(dd|dt|li|td|th|h1|h2|h3|h4|h5|h6|q)"; //$NON-NLS-1$
		String breakAfterTags = "(br)"; //$NON-NLS-1$
		String noFormatTags = "(code|tt)"; //$NON-NLS-1$
		String otherTags = "([\\S&&[^<>]]++)"; //$NON-NLS-1$
		String ws = "(?>[ \\t]++|[\\r\\n]++[ \\t]*+\\*?)"; // whitespace or line break with optional asterisk //$NON-NLS-1$
		String attributeValue = "(?>\"[^\"]*\")|(?>\'[^\']*\')|[\\S&&[^/>\"\']]++"; //$NON-NLS-1$
		String attribute = "(?>" + ws + "+[\\S&&[^=]]+" + ws + "*(=)" + ws + "*(?>" + attributeValue  + "))"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		HTML_TAG_PATTERN = Pattern.compile("<(/)?+(?:" //$NON-NLS-1$
				+ formatCodeTags + '|' + separateLineTags + '|' + breakBeforeTags + '|' + breakAfterTags + '|' + noFormatTags + '|' + otherTags + ')'
				+ "(" + attribute + "*)" + ws + "*/?>", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		HTML_ATTRIBUTE_PATTERN = Pattern.compile(attribute);
	}

	private final static Pattern HTML_ENTITY_PATTERN = Pattern
			.compile("&(#x[0-9a-fA-F]+)?(#[0-9]+)?(lt)?(gt)?(nbsp)?(amp)?(circ)?(tilde)?(quot)?;"); //$NON-NLS-1$
	private final static String HTML_ENTITY_REPLACE = "   <> &^~\""; //$NON-NLS-1$

	// Param tags list copied from IJavaDocTagConstants in legacy formatter for compatibility.
	// There were the following comments:
	// TODO (frederic) should have another name than 'param' for the following tags
	// TODO (frederic) investigate how and why this list was created
	private final static List<String> PARAM_TAGS = Arrays.asList(
			TagElement.TAG_PARAM,
			TagElement.TAG_EXCEPTION,
			TagElement.TAG_SERIALFIELD,
			TagElement.TAG_THROWS);

	private final static List<String> IMMUTABLE_TAGS = Arrays.asList(TagElement.TAG_CODE, TagElement.TAG_LITERAL);

	private final TokenManager tm;
	private final DefaultCodeFormatterOptions options;
	private final String sourceLevel;
	private final String formatDisableTag;
	private final String formatEnableTag;

	private Token lastLineComment;
	private int lastLineCommentPosition;

	private Token lastFormatOffComment;

	private TokenManager ctm;
	private List<Token> commentStructure;
	private int commentIndent;
	/** Index: position within current comment; Value: whether wrapping on special characters is allowed */
	private boolean[] allowSubstituteWrapping;

	private int noFormatTagOpenStart = -1;
	private int formatCodeTagOpenEnd = -1;
	private int lastFormatCodeClosingTagIndex = -1;
	private ArrayList<Integer> commonAttributeAnnotations = new ArrayList<Integer>();
	private DefaultCodeFormatter commentCodeFormatter;

	public CommentsPreparator(TokenManager tm, DefaultCodeFormatterOptions options, String sourceLevel) {
		this.tm = tm;
		this.options = options;
		this.sourceLevel = sourceLevel;
		this.formatDisableTag = options.disabling_tag != null ? new String(options.disabling_tag) : null;
		this.formatEnableTag = options.enabling_tag != null ? new String(options.enabling_tag) : null;
	}

	@Override
	public boolean preVisit2(ASTNode node) {
		boolean isMalformed = (node.getFlags() & ASTNode.MALFORMED) != 0;
		return !isMalformed;
	}

	@Override
	public boolean visit(LineComment node) {
		int commentIndex = this.tm.firstIndexIn(node, TokenNameCOMMENT_LINE);
		handleLineComment(commentIndex);
		return true;
	}

	public void handleLineComment(int commentIndex) {
		Token commentToken = this.tm.get(commentIndex);

		boolean isOnFirstColumn = handleWhitespaceAround(commentIndex);

		if (handleFormatOnOffTags(commentToken))
			return;

		if (isOnFirstColumn) {
			if (this.options.comment_format_line_comment
					&& !this.options.comment_format_line_comment_starting_on_first_column) {
				this.lastLineComment = null;
				commentToken.setIndent(0);
				commentToken.setWrapPolicy(WrapPolicy.FORCE_FIRST_COLUMN);
				return;
			}
			if (this.options.never_indent_line_comments_on_first_column) {
				commentToken.setIndent(0);
				commentToken.setWrapPolicy(WrapPolicy.FORCE_FIRST_COLUMN);
			}
		}

		handleNLSTags(commentToken, commentIndex);

		int positionInLine = this.tm.findSourcePositionInLine(commentToken.originalStart);
		boolean isContinuation = commentIndex > 0 && this.tm.get(commentIndex - 1) == this.lastLineComment
				&& (positionInLine >= this.lastLineCommentPosition - this.options.indentation_size + 1)
				&& this.tm.countLineBreaksBetween(this.lastLineComment, commentToken) == 1;

		boolean isHeader = this.tm.isInHeader(commentIndex);
		boolean formattingEnabled = (isHeader ? this.options.comment_format_header : this.options.comment_format_line_comment);
		if (!formattingEnabled) {
			preserveWhitespace(commentToken, commentIndex);
			if (isContinuation) {
				WrapPolicy policy = this.lastLineComment.getWrapPolicy();
				if (policy == null) {
					int lineStart = this.tm.getPositionInLine(this.tm.findFirstTokenInLine(commentIndex - 1));
					int commentStart = this.tm.getPositionInLine(commentIndex - 1);
					policy = new WrapPolicy(WrapMode.WHERE_NECESSARY, commentIndex - 1, commentStart - lineStart);
				}
				commentToken.setWrapPolicy(policy);
				this.lastLineComment = commentToken;
			} else if (commentToken.getLineBreaksBefore() == 0) {
				this.lastLineComment = commentToken;
				this.lastLineCommentPosition = positionInLine;
			}
			return;
		}

		List<Token> structure = tokenizeLineComment(commentToken);
		if (isContinuation) {
			Token first = structure.get(0);
			first.breakBefore();
			first.setWrapPolicy(
					new WrapPolicy(WrapMode.WHERE_NECESSARY, commentIndex - 1, this.lastLineCommentPosition));

			// merge previous and current line comment
			Token previous = this.lastLineComment;
			Token merged = new Token(previous, previous.originalStart, commentToken.originalEnd, previous.tokenType);
			merged.putLineBreaksAfter(commentToken.getLineBreaksAfter());
			merged.setPreserveLineBreaksAfter(commentToken.isPreserveLineBreaksAfter());
			this.tm.remove(commentIndex - 1);
			this.tm.insert(commentIndex - 1, merged);
			this.tm.remove(commentIndex);
			List<Token> lastStructure = this.lastLineComment.getInternalStructure();
			lastStructure.addAll(structure);
			merged.setInternalStructure(lastStructure);
			this.lastLineComment = merged;
		} else {
			commentToken.setInternalStructure(structure);
			handleCompilerTags(commentToken, commentIndex);
			preserveWhitespace(commentToken, commentIndex);
			this.lastLineComment = commentToken;
			this.lastLineCommentPosition = positionInLine;
		}
	}

	private void preserveWhitespace(Token commentToken, int commentIndex) {
		if (this.options.comment_preserve_white_space_between_code_and_line_comments
				&& commentToken.getLineBreaksBefore() == 0 && commentIndex > 0) {
			commentToken.clearSpaceBefore();
			List<Token> structure = commentToken.getInternalStructure();
			if (structure != null && !structure.isEmpty())
				structure.get(0).clearSpaceBefore();

			Token previous = this.tm.get(commentIndex - 1);
			previous.clearSpaceAfter();
			if (previous.originalEnd + 1 >= commentToken.originalStart)
				return;
			if (structure == null || structure.isEmpty()) {
				structure = new ArrayList<>();
				structure.add(new Token(previous.originalEnd + 1, commentToken.originalEnd, TokenNameCOMMENT_LINE));
				commentToken.setInternalStructure(structure);
			} else {
				structure.add(0, new Token(previous.originalEnd + 1, commentToken.originalStart - 1,
						TokenNameWHITESPACE));
			}
		}
	}

	/**
	 * @return true if the comment contains on/off tag and should not be formatted
	 */
	private boolean handleFormatOnOffTags(Token commentToken) {
		if (!this.options.use_tags)
			return false;
		String commentString = this.tm.toString(commentToken);
		int offIndex = this.formatDisableTag != null ? commentString.lastIndexOf(this.formatDisableTag) : -1;
		int onIndex = this.formatEnableTag != null ? commentString.lastIndexOf(this.formatEnableTag) : -1;
		if (this.lastFormatOffComment == null) {
			if (offIndex > onIndex)
				this.lastFormatOffComment = commentToken;
		} else {
			if (onIndex > offIndex) {
				this.tm.addDisableFormatTokenPair(this.lastFormatOffComment, commentToken);
				this.lastFormatOffComment = null;
			}
		}
		return offIndex >= 0 || onIndex >= 0;
	}

	private void handleNLSTags(Token comment, int commentIndex) {
		List<Token> stringLiterals = findStringLiteralsInLine(commentIndex);
		if (stringLiterals.isEmpty())
			return;

		List<Token> commentFragments = new ArrayList<>();
		Matcher matcher = NLS_TAG_PATTERN.matcher(this.tm.toString(comment));
		int previousMatcherEnd = 0;
		boolean nlsFound = false;
		while (matcher.find()) {
			int nlsNumber = Integer.parseInt(matcher.group(1));
			if (nlsNumber > 0 && nlsNumber <= stringLiterals.size()) {
				if (matcher.start() > previousMatcherEnd) {
					Token fragment = new Token(comment.originalStart + previousMatcherEnd,
							comment.originalStart + matcher.start() - 1, TokenNameCOMMENT_LINE);
					commentFragments.add(fragment);
				}
				Token nlsTag = new Token(comment.originalStart + matcher.start(),
						comment.originalStart + matcher.end() - 1, TokenNameCOMMENT_LINE);
				stringLiterals.get(nlsNumber - 1).setNLSTag(nlsTag);
				nlsTag.setNLSTag(stringLiterals.get(nlsNumber - 1));
				commentFragments.add(nlsTag);
				nlsFound = true;
				previousMatcherEnd = matcher.end();
			}
		}
		if (nlsFound) {
			comment.setInternalStructure(commentFragments);
			if (comment.originalStart + previousMatcherEnd <= comment.originalEnd) {
				Token fragment = new Token(comment.originalStart + previousMatcherEnd, comment.originalEnd,
						TokenNameCOMMENT_LINE);
				commentFragments.add(fragment);
			}
		}
	}

	private void handleCompilerTags(Token commentToken, int commentIndex) {
		final String commentText = this.tm.toString(commentToken);
		final List<Token> structure = commentToken.getInternalStructure();
		if (commentText.startsWith("//$FALL-THROUGH$") //$NON-NLS-1$
				|| commentText.startsWith("//$IDENTITY-COMPARISON$")) { //$NON-NLS-1$
			structure.get(1).clearSpaceBefore();
		}
		if (commentText.contains("//$IDENTITY-COMPARISON$")) { //$NON-NLS-1$
			// make sure the whole line is not broken
			Token token = commentToken;
			for (int i = commentIndex; i > 0; i--) {
				Token left = this.tm.get(i - 1);
				if (this.tm.countLineBreaksBetween(left, token) > 0)
					break;
				token.clearLineBreaksBefore();
				left.clearLineBreaksAfter();
				token.setWrapPolicy(WrapPolicy.DISABLE_WRAP);
				token = left;
			}
		}
	}

	private List<Token> findStringLiteralsInLine(int lastTokenIndex) {
		List<Token> stringLiterals = new ArrayList<>();
		Token previous = this.tm.get(lastTokenIndex);
		for (int i = lastTokenIndex - 1; i >= 0; i--) {
			Token token = this.tm.get(i);
			if (this.tm.countLineBreaksBetween(token, previous) > 0)
				break;
			if (token.tokenType == TokenNameStringLiteral)
				stringLiterals.add(token);
			previous = token;
		}
		Collections.reverse(stringLiterals);
		return stringLiterals;
	}

	private List<Token> tokenizeLineComment(Token commentToken) {
		List<Token> fragments = commentToken.getInternalStructure();
		if (fragments == null) {
			fragments = Arrays.asList(commentToken);
		}
		ArrayList<Token> result = new ArrayList<>();
		for (int i = 0; i < fragments.size(); i++) {
			Token token = fragments.get(i);
			if (token.hasNLSTag()) {
				if (ScannerHelper.isWhitespace(this.tm.charAt(token.originalStart - 1)))
					token.spaceBefore();
				result.add(token);
				continue;
			}
			int sourcePosition = token.originalStart;
			if (sourcePosition == commentToken.originalStart) {
				// separate starting slashes
				while (sourcePosition <= token.originalEnd && this.tm.charAt(sourcePosition) == '/')
					sourcePosition++;
				result.add(new Token(commentToken.originalStart, sourcePosition - 1, TokenNameCOMMENT_LINE));
			}
			int tokenStart = sourcePosition;
			while (sourcePosition <= token.originalEnd + 1) {
				if (sourcePosition == token.originalEnd + 1
						|| ScannerHelper.isWhitespace(this.tm.charAt(sourcePosition))) {
					if (tokenStart < sourcePosition) {
						Token outputToken = new Token(tokenStart, sourcePosition - 1, TokenNameCOMMENT_LINE);
						outputToken.spaceBefore();
						result.add(outputToken);
					}
					tokenStart = sourcePosition + 1;
				}
				sourcePosition++;
			}
		}
		return result;
	}

	@Override
	public boolean visit(BlockComment node) {
		int commentIndex = this.tm.firstIndexIn(node, TokenNameCOMMENT_BLOCK);
		handleBlockComment(commentIndex);

		return true;
	}

	public void handleBlockComment(int commentIndex) {
		Token commentToken = this.tm.get(commentIndex);
		boolean isFirstColumn = handleWhitespaceAround(commentIndex);

		if (handleFormatOnOffTags(commentToken))
			return;

		boolean isHeader = this.tm.isInHeader(commentIndex);
		boolean formattingEnabled = (isHeader ? this.options.comment_format_header : this.options.comment_format_block_comment);
		if (this.tm.charAt(commentToken.originalStart + 2) == '-') {
			if (commentToken.getLineBreaksBefore() > 0 || commentIndex == 0
					|| this.tm.get(commentIndex - 1).getLineBreaksAfter() > 0) {
				formattingEnabled = false;
			} else {
				return;
			}
		}
		if (formattingEnabled && tokenizeMultilineComment(commentToken)) {
			this.commentStructure = commentToken.getInternalStructure();
			this.ctm = new TokenManager(this.commentStructure, this.tm);
			handleStringLiterals(this.tm.toString(commentToken), commentToken.originalStart);
			addSubstituteWraps();
		} else {
			commentToken.setInternalStructure(commentToLines(commentToken, -1));
		}

		if (this.options.never_indent_block_comments_on_first_column && isFirstColumn) {
			commentToken.setIndent(0);
			commentToken.setWrapPolicy(WrapPolicy.FORCE_FIRST_COLUMN);
		}
	}

	private boolean handleWhitespaceAround(int commentIndex) {
		Token commentToken = this.tm.get(commentIndex);
		char charBefore = commentToken.originalStart > 0 ? this.tm.charAt(commentToken.originalStart - 1) : 0;
		if (charBefore == ' ' || charBefore == '\t')
			commentToken.spaceBefore();

		if (commentToken.originalEnd < this.tm.getSourceLength() - 1) {
			char charAfter = this.tm.charAt(commentToken.originalEnd + 1);
			if (charAfter == ' ' || charAfter == '\t')
				commentToken.spaceAfter();
		}

		Token previous = null, next = null;
		int existingBreaksBefore = 2, existingBreaksAfter = 2;
		if (commentIndex > 0) {
			previous = this.tm.get(commentIndex - 1);
			existingBreaksBefore = this.tm.countLineBreaksBetween(previous, commentToken);
			if (existingBreaksBefore > 0) {
				commentToken.breakBefore();
				commentToken.clearSpaceBefore();
			}
		}
		if (commentIndex < this.tm.size() - 1) {
			next = this.tm.get(commentIndex + 1);
			existingBreaksAfter = this.tm.countLineBreaksBetween(commentToken, next);
			if (existingBreaksAfter > 0)
				commentToken.breakAfter();
		}

		if (existingBreaksBefore <= 1
				&& (previous.tokenType == TokenNameCOMMENT_LINE || previous.tokenType == TokenNameCOMMENT_BLOCK)) {
			if (previous.getWrapPolicy() != WrapPolicy.FORCE_FIRST_COLUMN)
				commentToken.setWrapPolicy(previous.getWrapPolicy());
		} else {
			int i = commentIndex + 2;
			while (existingBreaksAfter <= 1 && i < this.tm.size()
					&& (next.tokenType == TokenNameCOMMENT_LINE || next.tokenType == TokenNameCOMMENT_BLOCK)) {
				Token next2 = this.tm.get(i++);
				existingBreaksAfter = this.tm.countLineBreaksBetween(next, next2);
				next = next2;
			}

			if (existingBreaksBefore < existingBreaksAfter && previous != null) {
				if (previous.isPreserveLineBreaksAfter() || previous.getLineBreaksAfter() >= 2 || existingBreaksAfter < 2) {
					commentToken.putLineBreaksAfter(previous.getLineBreaksAfter());
					commentToken.setPreserveLineBreaksAfter(previous.isPreserveLineBreaksAfter());
					previous.clearLineBreaksAfter();
					previous.setPreserveLineBreaksAfter(true);
				}
			} else if (existingBreaksAfter < 2 && existingBreaksAfter <= existingBreaksBefore && next != null
					&& next.tokenType != TokenNamepackage /* doesn't apply to a comment before the package declaration */) {
				if (next.isPreserveLineBreaksBefore() || next.getLineBreaksBefore() >= 2 || existingBreaksBefore < 2) {
					commentToken.putLineBreaksBefore(next.getLineBreaksBefore());
					commentToken.setPreserveLineBreaksBefore(next.isPreserveLineBreaksBefore());
					next.clearLineBreaksBefore();
					next.setPreserveLineBreaksBefore(true);
				}
			}
		}

		boolean isFirstColumn = (charBefore == '\r' || charBefore == '\n' || commentToken.originalStart == 0);
		return isFirstColumn;
	}

	private List<Token> commentToLines(Token commentToken, int commentStartPositionInLine) {
		List<Token> lines = new ArrayList<>();

		int tab = this.options.tab_size;
		String commentText = this.tm.toString(commentToken);
		int commentStartPosition = commentStartPositionInLine;
		if (commentStartPosition < 0)
			commentStartPosition = this.tm.findSourcePositionInLine(commentToken.originalStart);
		int positionInLine = commentStartPosition;
		int lineStart = 0;
		int breaksBeforeFirstLine = 0;
		boolean firstLine = true; // all lines except first will be NotAToken to disable asterisk adding
		boolean emptyLine = true;

		for (int i = 0; i < commentText.length(); i++) {
			char c = commentText.charAt(i);
			switch (c) {
				case ' ':
					if ((lineStart == i && positionInLine < commentStartPosition)
							|| (emptyLine && positionInLine == commentToken.getIndent() - 1))
						lineStart = i + 1;
					positionInLine++;
					break;
				case '\t':
					if ((lineStart == i && positionInLine < commentStartPosition)
							|| (emptyLine && positionInLine == commentToken.getIndent() - 1))
						lineStart = i + 1;
					if (tab > 0)
						positionInLine += tab - positionInLine % tab;
					break;
				case '\r':
				case '\n':
					if (lineStart < i) {
						Token line = new Token(commentToken.originalStart + lineStart,
								commentToken.originalStart + i - 1,
								firstLine ? commentToken.tokenType : TokenNameNotAToken);
						line.breakAfter();
						if (lines.isEmpty())
							line.putLineBreaksBefore(breaksBeforeFirstLine);
						lines.add(line);
					} else if (!lines.isEmpty()) {
						Token previousLine = lines.get(lines.size() - 1);
						previousLine.putLineBreaksAfter(previousLine.getLineBreaksAfter() + 1);
					} else {
						breaksBeforeFirstLine++;
					}
					if (i + 1 < commentText.length() && commentText.charAt(i + 1) == (c == '\r' ? '\n' : '\r'))
						i++;
					lineStart = i + 1;
					positionInLine = 0;
					firstLine = false;
					emptyLine = true;
					break;
				default:
					positionInLine++;
					emptyLine = false;
			}
		}
		if (lineStart < commentText.length()) {
			Token line = new Token(commentToken.originalStart + lineStart, commentToken.originalEnd,
					firstLine ? commentToken.tokenType : TokenNameNotAToken);
			line.setWrapPolicy(WrapPolicy.DISABLE_WRAP);
			lines.add(line);
		}
		return lines;
	}

	@Override
	public boolean visit(Javadoc node) {
		this.noFormatTagOpenStart = -1;
		this.formatCodeTagOpenEnd = -1;
		this.lastFormatCodeClosingTagIndex = -1;
		this.commonAttributeAnnotations.clear();
		this.ctm = null;

		int commentIndex = this.tm.firstIndexIn(node, TokenNameCOMMENT_JAVADOC);
		Token commentToken = this.tm.get(commentIndex);

		if (node.getParent() == null) {
			// not a proper javadoc, treat as block comment
			handleWhitespaceAround(commentIndex);
		}
		if (commentIndex < this.tm.size() - 1)
			commentToken.breakAfter();

		if (handleFormatOnOffTags(commentToken))
			return false;

		boolean isHeader = this.tm.isInHeader(commentIndex);
		boolean formattingEnabled = (isHeader ? this.options.comment_format_header : this.options.comment_format_javadoc_comment);
		if (!formattingEnabled || !tokenizeMultilineComment(commentToken)) {
			commentToken.setInternalStructure(commentToLines(commentToken, -1));
			return false;
		}
		this.commentStructure = commentToken.getInternalStructure();
		this.commentIndent = this.tm.toIndent(commentToken.getIndent(), true);
		this.ctm = new TokenManager(commentToken.getInternalStructure(), this.tm);

		handleJavadocTagAlignment(node);
		handleJavadocBlankLines(node);

		return true;
	}

	@Override
	public void endVisit(Javadoc node) {
		if (this.ctm == null)
			return;
		addSubstituteWraps();
	}

	@Override
	public boolean visit(TagElement node) {
		String tagName = node.getTagName();
		if (tagName == null || tagName.length() <= 1)
			return true;

		int startIndex = tokenStartingAt(node.getStartPosition());
		int nodeEnd = node.getStartPosition() + node.getLength() - 1;
		while (ScannerHelper.isWhitespace(this.ctm.charAt(nodeEnd)))
			nodeEnd--;
		int endIndex = tokenEndingAt(nodeEnd);

		this.ctm.get(startIndex + 1).setWrapPolicy(WrapPolicy.DISABLE_WRAP);

		if (node.getParent() instanceof Javadoc) {
			assert this.ctm.toString(startIndex).startsWith(tagName);

			Token startTokeen = this.ctm.get(startIndex);
			if (startIndex > 1)
				startTokeen.breakBefore();

			handleHtml(node);
		}

		if (node.isNested() && IMMUTABLE_TAGS.contains(tagName) && startIndex < endIndex) {
			disableFormatting(startIndex, endIndex, false);
		}
		return true;
	}

	@Override
	public void endVisit(TagElement node) {
		String tagName = node.getTagName();
		if (tagName == null || tagName.length() <= 1) {
			handleHtml(node);
		} else if (TagElement.TAG_SEE.equals(tagName)) {
			handleStringLiterals(this.tm.toString(node), node.getStartPosition());
		}
	}

	private void handleJavadocTagAlignment(Javadoc node) {
		// Lists of tag tokens: index 0 for tag name, index 1 for param name (or throws etc) (may be null),
		// the rest for description
		List<List<Token>> javadocRootTags = new ArrayList<>();
		List<TagElement> tagElements = node.tags();
		for (TagElement tagElement : tagElements) {
			String tagName = tagElement.getTagName();
			if (tagName == null || tagName.length() <= 1)
				continue;
			int startIndex = tokenStartingAt(tagElement.getStartPosition());
			int nodeEnd = tagElement.getStartPosition() + tagElement.getLength() - 1;
			while (ScannerHelper.isWhitespace(this.ctm.charAt(nodeEnd)))
				nodeEnd--;
			int endIndex = tokenEndingAt(nodeEnd);

			List<Token> tagTokens = new ArrayList<>();
			tagTokens.add(this.ctm.get(startIndex));
			if (!PARAM_TAGS.contains(tagName) || startIndex == endIndex) {
				tagTokens.add(null);
			}
			for (int i = startIndex + 1; i <= endIndex; i++) {
				tagTokens.add(this.ctm.get(i));
			}
			javadocRootTags.add(tagTokens);
		}

		if (this.options.comment_align_tags_names_descriptions) {
			int maxTagNameLength = 0;
			int maxParamNameLength = 0;
			for (List<Token> tagTokens : javadocRootTags) {
				Token tagName = tagTokens.get(0);
				Token paramName = tagTokens.get(1);
				maxTagNameLength = Math.max(maxTagNameLength, this.tm.getLength(tagName, 0));
				if (paramName != null)
					maxParamNameLength = Math.max(maxParamNameLength, this.tm.getLength(paramName, 0));
			}
			int paramNameAlign = maxTagNameLength + 1;
			int descriptionAlign = paramNameAlign;
			if (maxParamNameLength > 0)
				descriptionAlign += maxParamNameLength + 1;
			for (List<Token> tagTokens : javadocRootTags) {
				alignJavadocTag(tagTokens, paramNameAlign, descriptionAlign);
			}
		} else if (this.options.comment_align_tags_descriptions_grouped) {
			int groupStart = 0;
			String groupTagName = null;
			int descriptionAlign = 0;
			for (int i = 0; i < javadocRootTags.size(); i++) {
				List<Token> tagTokens = javadocRootTags.get(i);
				String tagName = this.ctm.toString(tagTokens.get(0));
				if (!tagName.equals(groupTagName)) {
					for (int j = groupStart; j < i; j++) {
						alignJavadocTag(javadocRootTags.get(j), 0, descriptionAlign);
					}
					groupStart = i;
					groupTagName = tagName;
					descriptionAlign = 0;
				}
				int indent = tagName.length() + 1;
				if (tagTokens.get(1) != null)
					indent += 1 + this.ctm.getLength(tagTokens.get(1), 0);
				descriptionAlign = Math.max(descriptionAlign, indent);
			}
			for (int j = groupStart; j < javadocRootTags.size(); j++) {
				alignJavadocTag(javadocRootTags.get(j), 0, descriptionAlign);
			}
		} else {
			for (List<Token> tagTokens : javadocRootTags) {
				int tagNameLength = this.ctm.getLength(tagTokens.get(0), 0);
				int align = this.options.comment_indent_root_tags ? tagNameLength + 1 : 0;
				alignJavadocTag(tagTokens, 0, align);
			}
		}
	}

	private void handleJavadocBlankLines(Javadoc node) {
		List<TagElement> tagElements = node.tags();
		List<Integer> tagIndexes = tagElements.stream()
				.filter(t -> !t.isNested() && t.getTagName() != null && t.getTagName().length() > 1)
				.map(t -> tokenStartingAt(t.getStartPosition()))
				.collect(Collectors.toList());
		tagIndexes.addAll(this.commonAttributeAnnotations);
		Collections.sort(tagIndexes);

		String previousName = null;
		if (!tagIndexes.isEmpty()) {
			int firstIndex = tagIndexes.get(0);
			previousName = this.ctm.toString(firstIndex);
			if (this.options.comment_insert_empty_line_before_root_tags && firstIndex > 1)
				this.ctm.get(firstIndex).putLineBreaksBefore(2);
		}
		if (this.options.comment_insert_empty_line_between_different_tags) {
			for (int i = 1; i < tagIndexes.size(); i++) {
				Token tagToken = this.ctm.get(tagIndexes.get(i));
				String thisName = this.tm.toString(tagToken);
				boolean sameType = previousName.equals(thisName)
						|| (isCommonsAttributeAnnotation(previousName) && isCommonsAttributeAnnotation(thisName));
				if (!sameType)
					tagToken.putLineBreaksBefore(2);
				previousName = thisName;
			}
		}
	}

	private void alignJavadocTag(List<Token> tagTokens, int paramNameAlign, int descriptionAlign) {
		Token paramName = tagTokens.get(1);
		if (paramName != null) {
			paramName.setAlign(paramNameAlign);
			if (this.options.comment_insert_new_line_for_parameter && tagTokens.size() > 2) {
				tagTokens.get(2).breakBefore();
			}
		}

		boolean extraIndent = (paramName != null ? this.options.comment_indent_parameter_description : this.options.comment_indent_tag_description);
		for (int i = 2; i < tagTokens.size(); i++) {
			Token token = tagTokens.get(i);
			token.setAlign(descriptionAlign);
			token.setIndent(extraIndent ? this.options.indentation_size : 0);
		}
	}

	private void handleHtml(TagElement node) {
		if (!this.options.comment_format_html && !this.options.comment_format_source)
			return;
		String text = this.tm.toString(node);
		Matcher matcher = HTML_TAG_PATTERN.matcher(text);
		while (matcher.find()) {
			int startPos = matcher.start() + node.getStartPosition();
			int endPos = matcher.end() - 1 + node.getStartPosition();
			boolean isOpeningTag = (matcher.start(1) == matcher.end(1));

			if (this.options.comment_format_html) {
				// make sure tokens inside the tag are wrapped only as a substitute
				int firstTokenIndex = tokenStartingAt(startPos), lastTokenIndex = tokenEndingAt(endPos);
				for (int i = firstTokenIndex + 1; i <= lastTokenIndex; i++) {
					Token token = this.ctm.get(i);
					if (token.getWrapPolicy() == null)
						token.setWrapPolicy(WrapPolicy.SUBSTITUTE_ONLY);
				}

				// allow wraps around equals sign in attributes
				String attributesText = matcher.group(8);
				Matcher attrMatcher = HTML_ATTRIBUTE_PATTERN.matcher(attributesText);
				final int commentStart = this.ctm.get(0).originalStart;
				while (attrMatcher.find()) {
					int equalPos = node.getStartPosition() + matcher.start(8) + attrMatcher.start(1);
					assert this.tm.charAt(equalPos) == '=';
					this.allowSubstituteWrapping[equalPos - commentStart] = true;
				}
			}

			int matchedGroups = 0;
			for (int i = 2; i <= 7; i++)
				if (matcher.start(i) < matcher.end(i))
					matchedGroups++;
			if (matchedGroups != 1)
				continue;

			if (matcher.start(2) < matcher.end(2)) {
				handleFormatCodeTag(startPos, endPos, isOpeningTag);
			}
			if (this.options.comment_format_html) {
				if (TagElement.TAG_PARAM.equals(node.getTagName())
						&& this.ctm.findIndex(startPos, -1, false) == 1 + this.ctm.firstIndexIn(node, -1)) {
					continue; // it's a generic class parameter name, not an HTML tag
				}

				if (matcher.start(3) < matcher.end(3)) {
					handleSeparateLineTag(startPos, endPos);
				} else if (matcher.start(4) < matcher.end(4)) {
					handleBreakBeforeTag(startPos, endPos, isOpeningTag);
				} else if (matcher.start(5) < matcher.end(5)) {
					handleBreakAfterTag(startPos, endPos);
				} else if (matcher.start(6) < matcher.end(6)) {
					handleNoFormatTag(startPos, endPos, isOpeningTag);
				}
			}
		}
	}

	@Override
	public boolean visit(MethodRef node) {
		handleReference(node);
		return true;
	}

	@Override
	public boolean visit(MemberRef node) {
		handleReference(node);
		return true;
	}

	@Override
	public boolean visit(QualifiedName node) {
		handleReference(node);
		return false;
	}

	private void handleReference(ASTNode node) {
		ASTNode parent = node.getParent();
		if ((parent instanceof TagElement) && ((TagElement) parent).isNested()) {
			int firstIndex = tokenStartingAt(node.getStartPosition());
			int lastIndex = tokenEndingAt(node.getStartPosition() + node.getLength() - 1);
			if (this.ctm.charAt(this.ctm.get(lastIndex + 1).originalStart) == '}')
				lastIndex++;
			for (int i = firstIndex; i <= lastIndex; i++) {
				Token token = this.ctm.get(i);
				token.setWrapPolicy(WrapPolicy.DISABLE_WRAP);
			}
		}
	}

	private void handleStringLiterals(String text, int textStartPosition) {
		Matcher matcher = STRING_LITERAL_PATTERN.matcher(text);
		while (matcher.find()) {
			int startPosition = textStartPosition + matcher.start();
			int startIndex = this.ctm.findIndex(startPosition, -1, false);
			int endPosition = textStartPosition + matcher.end() - 1;
			int endIndex = this.ctm.findIndex(endPosition, -1, false);
			if (startIndex != endIndex) {
				startIndex = tokenStartingAt(startPosition);
				endIndex = tokenEndingAt(endPosition);
				disableFormatting(startIndex, endIndex, false);
			}
			noSubstituteWrapping(startPosition, endPosition);
		}
	}

	private void handleSeparateLineTag(int startPos, int endPos) {
		int openingTagIndex = tokenStartingAt(startPos);
		if (openingTagIndex > 1 && this.lastFormatCodeClosingTagIndex == openingTagIndex - 1) {
			Token token = this.ctm.get(openingTagIndex - 1);
			assert token.getLineBreaksAfter() == 2;
			token.clearLineBreaksAfter();
			token.breakAfter();
		}

		handleBreakBeforeTag(startPos, endPos, true);
		handleBreakAfterTag(startPos, endPos);
	}

	private void handleBreakBeforeTag(int start, int end, boolean isOpeningTag) {
		int firstPartIndex = tokenStartingAt(start);
		int lastPartIndex = tokenEndingAt(end);
		Token firstPartToken = this.ctm.get(firstPartIndex);
		firstPartToken.setWrapPolicy(null);
		if (isOpeningTag) {
			firstPartToken.breakBefore();
			this.ctm.get(lastPartIndex + 1).clearSpaceBefore();
		} else {
			firstPartToken.clearSpaceBefore();
		}
	}

	private void handleBreakAfterTag(int start, int end) {
		int tokenIndex = tokenEndingAt(end);
		this.ctm.get(tokenIndex).breakAfter();
	}

	private void handleNoFormatTag(int start, int end, boolean isOpeningTag) {
		if (isOpeningTag) {
			if (this.noFormatTagOpenStart < 0)
				this.noFormatTagOpenStart = start;
		} else if (this.noFormatTagOpenStart >= 0) {
			int openingTagIndex = tokenStartingAt(this.noFormatTagOpenStart);
			int closingTagIndex = tokenEndingAt(end);
			if (openingTagIndex < closingTagIndex) {
				disableFormatting(openingTagIndex, closingTagIndex, true);
			}
			this.noFormatTagOpenStart = -1;
		}
	}

	private void handleFormatCodeTag(int startPos, int endPos, boolean isOpeningTag) {
		if (!this.options.comment_format_source) {
			handleNoFormatTag(startPos, endPos, isOpeningTag);
			return;
		}

		// add empty lines before opening and after closing token
		handleSeparateLineTag(startPos, endPos);
		if (isOpeningTag) {
			int startIndex = tokenStartingAt(startPos);
			if (startIndex > 1)
				this.ctm.get(startIndex).putLineBreaksBefore(2);

			if (this.formatCodeTagOpenEnd < 0)
				this.formatCodeTagOpenEnd = endPos;
		} else if (this.formatCodeTagOpenEnd >= 0) {
			int endTagIndex = tokenEndingAt(endPos);
			if (endTagIndex < this.ctm.size() - 2)
				this.ctm.get(endTagIndex).putLineBreaksAfter(2);

			formatCode(startPos, endPos);
			this.formatCodeTagOpenEnd = -1;
			this.lastFormatCodeClosingTagIndex = this.ctm.findIndex(startPos, -1, true);
		}
	}

	private void fixJavadocTagAlign(Token baseToken, int fixFirstIndex) {
		for (int i = fixFirstIndex; i < this.ctm.size() - 1; i++) {
			Token token = this.ctm.get(i);
			if (token.getAlign() == 0 && token.getIndent() == 0)
				break;
			token.setAlign(baseToken.getAlign());
			token.setIndent(baseToken.getIndent());
		}
	}

	private void disableFormatting(int startIndex, int endIndex, boolean isHtml) {
		Token startToken = this.ctm.get(startIndex), endToken = this.ctm.get(endIndex);

		Token noFormatToken = new Token(startToken.originalStart, endToken.originalEnd, TokenNameCOMMENT_JAVADOC);
		List<Token> lines = commentToLines(noFormatToken, findCommentLineIndent(startIndex));
		for (Token line : lines)
			line.setToEscape(isHtml);
		Token first = lines.get(0);
		if (startToken.isSpaceBefore())
			first.spaceBefore();
		first.setAlign(startToken.getAlign());
		first.setIndent(startToken.getIndent());
		first.putLineBreaksBefore(startToken.getLineBreaksBefore());
		first.setWrapPolicy(startToken.getWrapPolicy());
		Token  last = lines.get(lines.size() - 1);
		last.putLineBreaksAfter(endToken.getLineBreaksAfter());

		fixJavadocTagAlign(startToken, endIndex + 1);

		List<Token> tokensToReplace = this.commentStructure.subList(startIndex, endIndex + 1);
		tokensToReplace.clear();
		tokensToReplace.addAll(lines);
	}

	private void disableFormattingExclusively(int openingTagIndex, int closingTagIndex) {
		Token openingTag = this.ctm.get(openingTagIndex);
		int noFormatStart = openingTag.originalEnd + 1;
		int noFormatEnd = this.ctm.get(closingTagIndex - 1).originalEnd;
		if (noFormatStart <= noFormatEnd) {
			Token noFormatToken = new Token(noFormatStart, noFormatEnd, TokenNameCOMMENT_JAVADOC);
			List<Token> lines = commentToLines(noFormatToken, findCommentLineIndent(openingTagIndex));
			for (Token line : lines)
				line.setToEscape(true);
			fixJavadocTagAlign(openingTag, closingTagIndex);
			List<Token> tokensToReplace = this.commentStructure.subList(openingTagIndex + 1, closingTagIndex);
			tokensToReplace.clear();
			tokensToReplace.addAll(lines);
		} else {
			this.commentStructure.subList(openingTagIndex + 1, closingTagIndex).clear();
			Token closingTag = this.ctm.get(closingTagIndex);
			if (this.ctm.countLineBreaksBetween(openingTag, closingTag) == 0) {
				openingTag.clearLineBreaksAfter();
				closingTag.clearLineBreaksBefore();
			}
		}
	}

	private int findCommentLineIndent(int commentFragmentIndex) {
		int position = this.ctm.get(commentFragmentIndex).originalStart;
		int lastNonWhitespace = position;
		while (--position > 0) {
			char c = this.ctm.charAt(position);
			if (c == '\r' || c == '\n')
				break;
			if (!ScannerHelper.isWhitespace(c))
				lastNonWhitespace = position;
		}
		if (lastNonWhitespace > 0 && this.ctm.charAt(lastNonWhitespace - 1) == ' ')
			lastNonWhitespace--;
		return this.ctm.getLength(position, lastNonWhitespace - 1, 0);
	}

	private int tokenStartingAt(int start) {
		int tokenIndex = this.ctm.findIndex(start, -1, false);
		Token token = this.ctm.get(tokenIndex);
		if (token.originalStart == start)
			return tokenIndex;

		assert start > token.originalStart && start <= token.originalEnd;
		splitToken(token, tokenIndex, start);
		return tokenIndex + 1;
	}

	private int tokenEndingAt(int end) {
		int tokenIndex = this.ctm.findIndex(end, -1, true);
		Token token = this.ctm.get(tokenIndex);
		if (token.originalEnd == end)
			return tokenIndex;

		assert end < token.originalEnd && end >= token.originalStart;
		splitToken(token, tokenIndex, end + 1);
		return tokenIndex;
	}

	private void splitToken(Token token, int tokenIndex, int splitPosition) {
		assert splitPosition > token.originalStart && splitPosition <= token.originalEnd;

		Token part1 = new Token(token.originalStart, splitPosition - 1, token.tokenType);
		Token part2 = new Token(splitPosition, token.originalEnd, token.tokenType);
		if (token.isSpaceBefore())
			part1.spaceBefore();
		part1.putLineBreaksBefore(token.getLineBreaksBefore());
		part2.putLineBreaksAfter(token.getLineBreaksAfter());
		part1.setIndent(token.getIndent());
		part2.setIndent(token.getIndent());
		part1.setAlign(token.getAlign());
		part2.setAlign(token.getAlign());
		part1.setWrapPolicy(token.getWrapPolicy());
		this.commentStructure.set(tokenIndex, part1);
		this.commentStructure.add(tokenIndex + 1, part2);
	}

	private boolean tokenizeMultilineComment(Token commentToken) {
		if (this.allowSubstituteWrapping == null || this.allowSubstituteWrapping.length < commentToken.countChars()) {
			this.allowSubstituteWrapping = new boolean[commentToken.countChars()];
		}
		boolean isJavadoc = commentToken.tokenType == TokenNameCOMMENT_JAVADOC;
		Arrays.fill(this.allowSubstituteWrapping, 0, commentToken.countChars(), !isJavadoc);

		final boolean cleanBlankLines = isJavadoc ? this.options.comment_clear_blank_lines_in_javadoc_comment
				: this.options.comment_clear_blank_lines_in_block_comment;

		List<Token> structure = new ArrayList<>();

		int firstTokenEnd = commentToken.originalStart + 1;
		while (firstTokenEnd < commentToken.originalEnd - 1 && this.tm.charAt(firstTokenEnd + 1) == '*')
			firstTokenEnd++;
		Token first = new Token(commentToken.originalStart, firstTokenEnd, commentToken.tokenType);
		first.spaceAfter();
		structure.add(first);

		int lastTokenStart = commentToken.originalEnd - 1;
		while (lastTokenStart - 1 > firstTokenEnd && this.tm.charAt(lastTokenStart - 1) == '*')
			lastTokenStart--;

		int position = firstTokenEnd + 1;
		int lineBreaks = 0;
		while (position <= commentToken.originalEnd) {
			// find line start
			for (int i = position; i < lastTokenStart; i++) {
				char c = this.tm.charAt(i);
				if (c == '\r' || c == '\n') {
					lineBreaks++;
					char c2 = this.tm.charAt(i + 1);
					if ((c2 == '\r' || c2 == '\n') && c2 != c)
						i++;
					position = i + 1;
				} else if (!ScannerHelper.isWhitespace(c)) {
					while (this.tm.charAt(i) == '*' && lineBreaks > 0)
						i++;
					position = i;
					break;
				}
			}

			int tokenStart = position;
			while (position <= commentToken.originalEnd + 1) {
				char c = 0;
				if (position == commentToken.originalEnd + 1 || position == lastTokenStart
						|| ScannerHelper.isWhitespace(c = this.tm.charAt(position))) {
					if (tokenStart < position) {
						Token outputToken = new Token(tokenStart, position - 1, commentToken.tokenType);
						outputToken.spaceBefore();
						if (lineBreaks > 0) {
							if (cleanBlankLines)
								lineBreaks = 1;
							if (lineBreaks > 1 || !this.options.join_lines_in_comments)
								outputToken.putLineBreaksBefore(lineBreaks);
						}
						if (this.tm.charAt(tokenStart) == '@') {
							outputToken.setWrapPolicy(WrapPolicy.DISABLE_WRAP);
							if (commentToken.tokenType == TokenNameCOMMENT_BLOCK && lineBreaks == 1
									&& structure.size() > 1) {
								outputToken.putLineBreaksBefore(cleanBlankLines ? 1 : 2);
							}
							if (lineBreaks > 0 && isCommonsAttributeAnnotation(this.tm.toString(outputToken))) {
								outputToken.breakBefore();
								this.commonAttributeAnnotations.add(structure.size());
							}
						}
						structure.add(outputToken);
						lineBreaks = 0;
					}
					if (c == '\r' || c == '\n')
						break;
					tokenStart = position == lastTokenStart ? position : position + 1;
				}
				position++;
			}
		}

		Token last = structure.get(structure.size() - 1);
		boolean newLinesAtBoundries = commentToken.tokenType == TokenNameCOMMENT_JAVADOC
				? this.options.comment_new_lines_at_javadoc_boundaries
				: this.options.comment_new_lines_at_block_boundaries;
		if (!newLinesAtBoundries) {
			structure.get(1).clearLineBreaksBefore();
			last.clearLineBreaksBefore();
		} else if (this.tm.countLineBreaksBetween(first, last) > 0) {
			first.breakAfter();
			last.breakBefore();
		}
		last.setAlign(1);

		if (structure.size() == 2)
			return false;
		commentToken.setInternalStructure(structure);
		return true;
	}

	private boolean isCommonsAttributeAnnotation(String tokenContent) {
		return tokenContent.startsWith("@@"); //$NON-NLS-1$
	}

	private void noSubstituteWrapping(int from, int to) {
		int commentStart = this.ctm.get(0).originalStart;
		assert commentStart <= from && from <= to && to <= this.ctm.get(this.ctm.size() - 1).originalEnd;
		Arrays.fill(this.allowSubstituteWrapping, from - commentStart, to - commentStart + 1, false);
	}

	private void addSubstituteWraps() {
		Token previous = this.ctm.get(0);
		int commentStart = previous.originalStart;
		for (int i = 1; i < this.ctm.size() - 1; i++) {
			Token token = this.ctm.get(i);
			boolean touchesPrevious = token.originalStart == this.ctm.get(i - 1).originalEnd + 1;
			if (touchesPrevious && token.getLineBreaksBefore() == 0 && previous.getLineBreaksAfter() == 0
					&& token.getWrapPolicy() == null) {
				boolean allowWrap = this.allowSubstituteWrapping[token.originalStart - commentStart];
				token.setWrapPolicy(allowWrap ? WrapPolicy.SUBSTITUTE_ONLY : WrapPolicy.DISABLE_WRAP);
			}
			for (int pos = token.originalStart + 1; pos < token.originalEnd; pos++) {
				if (!this.allowSubstituteWrapping[pos - commentStart])
					continue;
				char c = this.ctm.charAt(pos);
				if (!ScannerHelper.isJavaIdentifierPart(c)) {
					this.ctm.get(tokenStartingAt(pos)).setWrapPolicy(WrapPolicy.SUBSTITUTE_ONLY);
					this.ctm.get(tokenStartingAt(pos + 1)).setWrapPolicy(WrapPolicy.SUBSTITUTE_ONLY);
				}
			}
			previous = token;
		}
	}

	private void formatCode(int javadocNoFormatCloseStart, int javadocNoFormatCloseEnd) {
		int openingTagLastIndex = tokenEndingAt(this.formatCodeTagOpenEnd);
		int closingTagFirstIndex = tokenStartingAt(javadocNoFormatCloseStart);

		int codeStartPosition = this.formatCodeTagOpenEnd + 1;
		int codeEndPosition = javadocNoFormatCloseStart - 1;
		StringBuilder codeBuilder = new StringBuilder(codeEndPosition - codeStartPosition + 1);
		int[] positionMapping = new int[codeEndPosition - codeStartPosition + 1];
		// ^ index: original source position (minus startPosition), value: position in code string
		getCodeToFormat(codeStartPosition, codeEndPosition, codeBuilder, positionMapping);

		List<Token> formattedTokens = getCommentCodeFormatter().prepareFormattedCode(codeBuilder.toString());

		if (formattedTokens == null) {
			disableFormattingExclusively(openingTagLastIndex, closingTagFirstIndex);
			return;
		}

		formattedTokens = translateFormattedTokens(codeStartPosition, formattedTokens, positionMapping, null);

		Token openingToken = this.ctm.get(openingTagLastIndex);
		for (Token token : formattedTokens)
			token.setAlign(token.getAlign() + openingToken.getAlign() + openingToken.getIndent());
		fixJavadocTagAlign(openingToken, closingTagFirstIndex);

		// there are too few linebreaks at the start and end
		Token start = formattedTokens.get(0);
		start.putLineBreaksBefore(start.getLineBreaksBefore() + 1);
		Token end = formattedTokens.get(formattedTokens.size() - 1);
		end.putLineBreaksAfter(end.getLineBreaksAfter() + 1);
		// and there may be too many line breaks before closing tag
		this.ctm.get(closingTagFirstIndex).clearLineBreaksBefore();

		List<Token> tokensToReplace = this.commentStructure.subList(openingTagLastIndex + 1, closingTagFirstIndex);
		tokensToReplace.clear();
		tokensToReplace.addAll(formattedTokens);
	}

	private DefaultCodeFormatter getCommentCodeFormatter() {
		if (this.commentCodeFormatter == null) {
			Map<String, String> options2 = this.options.getMap();
			options2.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_LINE_LENGTH,
					String.valueOf(this.options.comment_line_length - this.commentIndent
							- COMMENT_LINE_SEPARATOR_LENGTH));
			options2.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT,
					String.valueOf(this.options.page_width - this.commentIndent - COMMENT_LINE_SEPARATOR_LENGTH));
			options2.put(CompilerOptions.OPTION_Source, this.sourceLevel);
			this.commentCodeFormatter = new DefaultCodeFormatter(options2);
		}
		return this.commentCodeFormatter;
	}

	private void getCodeToFormat(int startPos, int endPos, StringBuilder sb, int[] posMapping) {
		int position = 0; // original source position (minus startPos)

		// skip excessive line break at the beginning
		char c, c2;
		if ((c = this.ctm.charAt(position + startPos)) == '\r' || c == '\n') {
			posMapping[position++] = sb.length() - 1;
			if (((c2 = this.ctm.charAt(position + startPos)) == '\r' || c2 == '\n') && c2 != c)
				posMapping[position++] = sb.length() - 1;
		}

		while (position + startPos <= endPos) {
			int lineStart = position + startPos;
			for (int i = lineStart;; i++) {
				c = this.ctm.charAt(i);
				if (c == '\r' || c == '\n') {
					sb.append(c);
					lineStart = i + 1;
				} else if (!ScannerHelper.isWhitespace(c)) {
					if (c == '*')
						lineStart = (this.ctm.charAt(i + 1) == ' ') ? i + 2 : i + 1;
					break;
				}
			}
			int lineEnd = endPos + 1;
			for (int i = lineStart; i <= endPos; i++) {
				c = this.ctm.charAt(i);
				if (c == '\r' || c == '\n') {
					lineEnd = i;
					break;
				}
			}

			while (position + startPos < lineStart) {
				posMapping[position++] = sb.length() - 1;
			}

			int htmlEntityStart = -1;
			for (int i = lineStart; i < lineEnd; i++) {
				c = this.ctm.charAt(i);
				sb.append(c);
				posMapping[position++] = sb.length() - 1;

				if (c == '&') {
					htmlEntityStart = i;
				} else if (c == ';' && htmlEntityStart >= 0) {
					char replacementChar = getHtmlEntityChar(this.ctm.getSource().substring(htmlEntityStart, i + 1));
					if (replacementChar != 0) {
						sb.setLength(sb.length() - (i + 1 - htmlEntityStart));
						sb.append(replacementChar);
						for (int k = position - (i + 1 - htmlEntityStart); k < position; k++)
							posMapping[k] = sb.length() - 1;
					}
					htmlEntityStart = -1;
				}
			}
		}

		// remove last line if empty
		while (sb.length() > 0 && ((c = sb.charAt(sb.length() - 1)) == ' ' || c == '\t'))
			sb.deleteCharAt(sb.length() - 1);
		if (sb.length() > 0 && ((c = sb.charAt(sb.length() - 1)) == '\r' || c == '\n')) {
			sb.deleteCharAt(sb.length() - 1);
			if (sb.length() > 0 && ((c2 = sb.charAt(sb.length() - 1)) == '\r' || c2 == '\n') && c2 != c)
				sb.deleteCharAt(sb.length() - 1);
		}
	}

	private char getHtmlEntityChar(String entity) {
		Matcher matcher = HTML_ENTITY_PATTERN.matcher(entity);
		if (matcher.find()) {
			char replaceChar = 0;
			for (int i = 1; i < HTML_ENTITY_REPLACE.length(); i++) {
				int start = matcher.start(i);
				int end = matcher.end(i);
				if (start == end)
					continue; // group not matched
				if (replaceChar != 0)
					return 0; // more than one group matched
				switch (i) {
					case 1:
						replaceChar = (char) Integer.parseInt(entity.substring(start + 2, end), 16);
						break;
					case 2:
						replaceChar = (char) Integer.parseInt(entity.substring(start + 1, end), 10);
						break;
					default:
						replaceChar = HTML_ENTITY_REPLACE.charAt(i);
				}
			}
			return replaceChar;
		}
		return 0;
	}

	private List<Token> translateFormattedTokens(int startPosition, List<Token> formattedTokens, int[] positionMapping,
			HashMap<Token, Token> translationMap) {
		int previousLineBreaks = 0;
		List<Token> result = new ArrayList<>();
		for (Token token : formattedTokens) {
			int newStart = Arrays.binarySearch(positionMapping, token.originalStart);
			while (newStart > 0 && positionMapping[newStart - 1] == token.originalStart)
				newStart--;
			int newEnd = Arrays.binarySearch(positionMapping, token.originalEnd);
			while (newEnd + 1 < positionMapping.length && positionMapping[newEnd + 1] == token.originalEnd)
				newEnd++;
			Token translated = new Token(token, newStart + startPosition, newEnd + startPosition, token.tokenType);
			if (translated.getWrapPolicy() == null)
				translated.setWrapPolicy(WrapPolicy.DISABLE_WRAP);

			if (token.hasNLSTag()) {
				if (translationMap == null)
					translationMap = new HashMap<>();
				Token translatedNLS = translationMap.get(token.getNLSTag());
				if (translatedNLS != null) {
					translatedNLS.setNLSTag(translated);
					translated.setNLSTag(translatedNLS);
				} else {
					translationMap.put(token, translated);
				}
			}

			int lineBreaks = Math.max(previousLineBreaks, token.getLineBreaksBefore());
			List<Token> structure = token.getInternalStructure();
			if (structure != null && !structure.isEmpty()) {
				translated.setInternalStructure(translateFormattedTokens(startPosition, structure, positionMapping,
						translationMap));
			}
			translated.putLineBreaksBefore(lineBreaks);
			translated.setToEscape(true);
			result.add(translated);
			previousLineBreaks = token.getLineBreaksAfter();
		}
		result.get(result.size() - 1).putLineBreaksAfter(previousLineBreaks);

		return result;
	}

	public void finishUp() {
		if (this.lastFormatOffComment != null)
			this.tm.addDisableFormatTokenPair(this.lastFormatOffComment, this.tm.get(this.tm.size() - 1));
	}
}
