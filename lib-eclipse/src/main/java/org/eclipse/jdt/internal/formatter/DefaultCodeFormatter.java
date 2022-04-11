/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *     IBM Corporation - initial API and implementation
 *     Jesper Steen Moller - Contributions for
 *								bug 404146 - [1.7][compiler] nested try-catch-finally-blocks leads to unrunnable Java byte code
 *     Harry Terkelsen (het@google.com) - Bug 449262 - Allow the use of third-party Java formatters
 *     Mateusz Matela <mateusz.matela@gmail.com> - [formatter] Formatter does not format Java code correctly, especially when max line width is set - https://bugs.eclipse.org/303519
 *     Mateusz Matela <mateusz.matela@gmail.com> - [formatter] follow up bug for comments - https://bugs.eclipse.org/458208
 *     Lars Vogel <Lars.Vogel@vogella.com> - Contributions for
 *     						Bug 473178
 *******************************************************************************/
package org.eclipse.jdt.internal.formatter;

import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameCOMMENT_BLOCK;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameCOMMENT_JAVADOC;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameCOMMENT_LINE;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameEOF;
import static org.eclipse.jdt.internal.compiler.parser.TerminalTokens.TokenNameNotAToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.SourceModule;
import org.eclipse.jdt.internal.formatter.linewrap.CommentWrapExecutor;
import org.eclipse.jdt.internal.formatter.linewrap.WrapPreparator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

public class DefaultCodeFormatter extends CodeFormatter {

	/**
	 * Debug trace
	 */
	public static boolean DEBUG = false;

	private static final int K_COMMENTS_MASK = K_SINGLE_LINE_COMMENT
		| K_MULTI_LINE_COMMENT
		| K_JAVA_DOC;

	// Mask for code formatter kinds
	private static final int K_MASK = K_UNKNOWN
		| K_EXPRESSION
		| K_STATEMENTS
		| K_CLASS_BODY_DECLARATIONS
		| K_COMPILATION_UNIT
		| K_MODULE_INFO
		| K_COMMENTS_MASK;

	private static final Map<Integer, Integer> FORMAT_TO_PARSER_KIND = new HashMap<>();
	static {
		FORMAT_TO_PARSER_KIND.put(K_COMPILATION_UNIT, ASTParser.K_COMPILATION_UNIT);
		FORMAT_TO_PARSER_KIND.put(K_MODULE_INFO, ASTParser.K_COMPILATION_UNIT);
		FORMAT_TO_PARSER_KIND.put(K_CLASS_BODY_DECLARATIONS, ASTParser.K_CLASS_BODY_DECLARATIONS);
		FORMAT_TO_PARSER_KIND.put(K_STATEMENTS, ASTParser.K_STATEMENTS);
		FORMAT_TO_PARSER_KIND.put(K_EXPRESSION, ASTParser.K_EXPRESSION);
	}

	private DefaultCodeFormatterOptions originalOptions;
	private DefaultCodeFormatterOptions workingOptions;

	private Object oldCommentFormatOption;
	private String sourceLevel;
	public boolean previewEnabled;

	private String sourceString;
	char[] sourceArray;
	private List<IRegion> formatRegions;

	private ASTNode astRoot;
	private List<Token> tokens = new ArrayList<>();
	private TokenManager tokenManager;

	public DefaultCodeFormatter() {
		this(new DefaultCodeFormatterOptions(DefaultCodeFormatterConstants.getJavaConventionsSettings()), null);
	}

	public DefaultCodeFormatter(DefaultCodeFormatterOptions options) {
		this(options, null);
	}

	public DefaultCodeFormatter(Map<String, String> options) {
		this(null, options);
	}

	public DefaultCodeFormatter(DefaultCodeFormatterOptions defaultCodeFormatterOptions, Map<String, String> options) {
		initOptions(defaultCodeFormatterOptions, options);
	}

	private void initOptions(DefaultCodeFormatterOptions defaultCodeFormatterOptions, Map<String, String> options) {
		if (options != null) {
			this.originalOptions = new DefaultCodeFormatterOptions(options);
			this.workingOptions = new DefaultCodeFormatterOptions(options);
			this.oldCommentFormatOption = getOldCommentFormatOption(options);
			String compilerSource = options.get(CompilerOptions.OPTION_Source);
			this.sourceLevel = compilerSource != null ? compilerSource : CompilerOptions.getLatestVersion();
			this.previewEnabled = JavaCore.ENABLED.equals(options.get(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES));
		} else {
			Map<String, String> settings = DefaultCodeFormatterConstants.getJavaConventionsSettings();
			this.originalOptions = new DefaultCodeFormatterOptions(settings);
			this.workingOptions = new DefaultCodeFormatterOptions(settings);
			this.oldCommentFormatOption = DefaultCodeFormatterConstants.TRUE;
			this.sourceLevel = CompilerOptions.getLatestVersion();
		}
		if (defaultCodeFormatterOptions != null) {
			this.originalOptions.set(defaultCodeFormatterOptions.getMap());
			this.workingOptions.set(defaultCodeFormatterOptions.getMap());
		}
	}

	@Deprecated
	private Object getOldCommentFormatOption(Map<String, String> options) {
		return options.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT);
	}

	@Override
	public String createIndentationString(final int indentationLevel) {
		if (indentationLevel < 0) {
			throw new IllegalArgumentException();
		}

		StringBuilder sb = new StringBuilder();
		int indent = indentationLevel * this.originalOptions.indentation_size;
		TextEditsBuilder.appendIndentationString(sb, this.originalOptions.tab_char, this.originalOptions.tab_size,
				indent, 0);
		return sb.toString();
	}

	/**
	 * @see org.eclipse.jdt.core.formatter.CodeFormatter#format(int, java.lang.String, int, int, int, java.lang.String)
	 */
	@Override
	public TextEdit format(int kind, String source, int offset, int length, int indentationLevel, String lineSeparator) {
		return format(kind, source, new IRegion[] { new Region(offset, length) }, indentationLevel, lineSeparator);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TextEdit format(int kind, String source, IRegion[] regions, int indentationLevel, String lineSeparator) {
		if (!regionsSatisfiesPreconditions(regions, source.length())) {
			throw new IllegalArgumentException();
		}
		this.formatRegions = Arrays.asList(regions);

		updateWorkingOptions(indentationLevel, lineSeparator, kind);

		if ((kind & K_COMMENTS_MASK) != 0)
			return formatComments(source, kind & K_COMMENTS_MASK);

		if (prepareFormattedCode(source, kind) == null)
			return this.tokens.isEmpty() ? new MultiTextEdit() : null;

		MultiTextEdit result = new MultiTextEdit();
		TextEditsBuilder resultBuilder = new TextEditsBuilder(this.sourceString, this.formatRegions, this.tokenManager,
				this.workingOptions);
		this.tokenManager.traverse(0, resultBuilder);
		for (TextEdit edit : resultBuilder.getEdits()) {
			result.addChild(edit);
		}
		return result;
	}

	private boolean init(String source, int kind) {

		// this is convenient for debugging (see Token.toString())
		// Token.source = source;

		this.sourceString = source;
		this.sourceArray = source.toCharArray();
		this.tokens.clear();
		this.tokenManager = new TokenManager(this.tokens, source, this.workingOptions);

		tokenizeSource(kind);
		return !this.tokens.isEmpty();
	}

	List<Token> prepareFormattedCode(String source) {
		this.formatRegions = Arrays.asList(new Region(0, source.length()));
		return prepareFormattedCode(source, CodeFormatter.K_UNKNOWN);
	}

	private List<Token> prepareFormattedCode(String source, int kind) {
		if (!init(source, kind))
			return null;

		this.astRoot = parseSourceCode(kind);
		if (this.astRoot == null)
			return null;

		if (kind != CodeFormatter.K_UNKNOWN)
			findHeader();

		prepareSpaces();
		prepareLineBreaks();
		prepareComments();
		prepareWraps(kind);

		return this.tokens;
	}

	private void findHeader() {
		if (this.astRoot instanceof CompilationUnit) {
			CompilationUnit unit = (CompilationUnit) this.astRoot;
			List<TypeDeclaration> types = unit.types();
			ASTNode firstElement = types.isEmpty() ? unit.getPackage() : types.get(0);
			if (firstElement != null) {
				int headerEndIndex = this.tokenManager.firstIndexIn(firstElement, -1);
				this.tokenManager.setHeaderEndIndex(headerEndIndex);
			}
		}
	}

	private TextEdit formatComments(String source, int kind) {
		MultiTextEdit result = new MultiTextEdit();
		if (!init(source, kind))
			return result;

		CommentsPreparator commentsPreparator = new CommentsPreparator(this.tokenManager, this.workingOptions,
				this.sourceLevel);
		CommentWrapExecutor commentWrapper = new CommentWrapExecutor(this.tokenManager, this.workingOptions);
		switch (kind) {
			case K_JAVA_DOC:
				for (Token token : this.tokens) {
					if (token.tokenType == TokenNameCOMMENT_JAVADOC) {
						CompilationUnit cu = (CompilationUnit) parseSourceCode(ASTParser.K_COMPILATION_UNIT);
						Javadoc javadoc = (Javadoc) cu.getCommentList().get(0);
						javadoc.accept(commentsPreparator);
						int startPosition = this.tokenManager.findSourcePositionInLine(token.originalStart);
						commentWrapper.wrapMultiLineComment(token, startPosition, false, false);
					}
				}
				break;
			case K_MULTI_LINE_COMMENT:
				for (int i = 0; i < this.tokens.size(); i++) {
					Token token = this.tokens.get(i);
					if (token.tokenType == TokenNameCOMMENT_BLOCK) {
						commentsPreparator.handleBlockComment(i);
						int startPosition = this.tokenManager.findSourcePositionInLine(token.originalStart);
						commentWrapper.wrapMultiLineComment(token, startPosition, false, false);
					}
				}
				break;
			case K_SINGLE_LINE_COMMENT:
				for (int i = 0; i < this.tokens.size(); i++) {
					Token token = this.tokens.get(i);
					if (token.tokenType == TokenNameCOMMENT_LINE) {
						commentsPreparator.handleLineComment(i);
						if (i >= this.tokens.size() || this.tokens.get(i) != token) {
							// current token has been removed and merged with previous one
							i--;
							token = this.tokens.get(i);
						}
						int startPosition = this.tokenManager.findSourcePositionInLine(token.originalStart);
						commentWrapper.wrapLineComment(token, startPosition);
					}
				}
				break;
			default:
				throw new AssertionError(String.valueOf(kind));
		}

		applyFormatOff();

		TextEditsBuilder resultBuilder = new TextEditsBuilder(source, this.formatRegions, this.tokenManager,
				this.workingOptions);
		resultBuilder.setAlignChar(DefaultCodeFormatterOptions.SPACE);
		for (Token token : this.tokens) {
			List<Token> structure = token.getInternalStructure();
			if (token.isComment() && structure != null && !structure.isEmpty())
				resultBuilder.processComment(token);
		}

		for (TextEdit edit : resultBuilder.getEdits()) {
			result.addChild(edit);
		}
		return result;
	}

	private ASTNode parseSourceCode(int kind) {
		kind = kind & K_MASK;
		if (kind != K_UNKNOWN) {
			ASTNode astNode = createParser(kind).createAST(null);
			if (kind == K_COMPILATION_UNIT || kind == K_MODULE_INFO)
				return astNode;
			return hasErrors(astNode) ? null : astNode;
		}

		int[] kindsToTry = { K_COMPILATION_UNIT, K_EXPRESSION, K_CLASS_BODY_DECLARATIONS, K_STATEMENTS, K_MODULE_INFO };
		for (int kindToTry : kindsToTry) {
			ASTNode astNode = createParser(kindToTry).createAST(null);
			if (!hasErrors(astNode)) {
				if (kindToTry == K_MODULE_INFO)
					tokenizeSource(kindToTry); // run scanner again to get module specific tokens
				return astNode;
			}
		}
		return null;
	}

	private ASTParser createParser(int kind) {
		ASTParser parser = ASTParser.newParser(AST.JLS17);

		if (kind == K_MODULE_INFO) {
			parser.setSource(createDummyModuleInfoCompilationUnit());
		} else {
			parser.setSource(this.sourceArray);
		}
		parser.setKind(FORMAT_TO_PARSER_KIND.get(kind));

		Map<String, String> parserOptions = JavaCore.getOptions();
		parserOptions.put(CompilerOptions.OPTION_Source, this.sourceLevel);
		parserOptions.put(CompilerOptions.OPTION_DocCommentSupport, CompilerOptions.ENABLED);
		parserOptions.put(CompilerOptions.OPTION_EnablePreviews, CompilerOptions.ENABLED); //TODO
		parserOptions.put(CompilerOptions.OPTION_ReportPreviewFeatures, CompilerOptions.IGNORE);
		parser.setCompilerOptions(parserOptions);
		return parser;
	}

	private ICompilationUnit createDummyModuleInfoCompilationUnit() {
		JavaProject dummyProject = new JavaProject() {
			@Override
			public Map<String, String> getOptions(boolean inheritJavaCoreOptions) {
				return new HashMap<>();
			}

			@Override
			public IModuleDescription getModuleDescription() throws JavaModelException {
				return new SourceModule(this, ""); //$NON-NLS-1$
			}
		};
		return new org.eclipse.jdt.internal.core.CompilationUnit(null, TypeConstants.MODULE_INFO_FILE_NAME_STRING,
				null) {
			@Override
			public char[] getContents() {
				return DefaultCodeFormatter.this.sourceArray;
			}

			@Override
			public JavaProject getJavaProject() {
				return dummyProject;
			}
		};
	}

	private boolean hasErrors(ASTNode astNode) {
		CompilationUnit root = (CompilationUnit) astNode.getRoot();
		for (IProblem problem : root.getProblems()) {
			if (problem.isError())
				return true;
		}
		return false;
	}

	private void tokenizeSource(int kind) {
		this.tokens.clear();
		Scanner scanner = new Scanner(true, false, false/* nls */, CompilerOptions.versionToJdkLevel(this.sourceLevel),
				null/* taskTags */, null/* taskPriorities */, false/* taskCaseSensitive */, this.previewEnabled);
		scanner.setSource(this.sourceArray);
		scanner.fakeInModule = (kind & K_MODULE_INFO) != 0;
		while (true) {
			try {
				int tokenType = scanner.getNextToken();
				if (tokenType == TokenNameEOF)
					break;
				Token token = Token.fromCurrent(scanner, tokenType);
				this.tokens.add(token);
			} catch (InvalidInputException e) {
				Token token = Token.fromCurrent(scanner, TokenNameNotAToken);
				this.tokens.add(token);
			}
		}
	}

	private void prepareSpaces() {
		SpacePreparator spacePreparator = new SpacePreparator(this.tokenManager, this.workingOptions);
		this.astRoot.accept(spacePreparator);
		spacePreparator.finishUp();
	}

	private void prepareLineBreaks() {
		LineBreaksPreparator breaksPreparator = new LineBreaksPreparator(this.tokenManager, this.workingOptions);
		this.astRoot.accept(breaksPreparator);
		breaksPreparator.finishUp();
		this.astRoot.accept(new OneLineEnforcer(this.tokenManager, this.workingOptions));
	}

	private void prepareComments() {
		CommentsPreparator commentsPreparator = new CommentsPreparator(this.tokenManager, this.workingOptions,
				this.sourceLevel);
		List<Comment> comments = ((CompilationUnit) this.astRoot.getRoot()).getCommentList();
		for (Comment comment : comments) {
			comment.accept(commentsPreparator);
		}
		commentsPreparator.finishUp();
	}

	private void prepareWraps(int kind) {
		WrapPreparator wrapPreparator = new WrapPreparator(this.tokenManager, this.workingOptions, kind);
		this.astRoot.accept(wrapPreparator);
		applyFormatOff();
		wrapPreparator.finishUp(this.astRoot, this.formatRegions);
	}

	private void applyFormatOff() {
		for (Token[] offPair : this.tokenManager.getDisableFormatTokenPairs()) {
			final int offStart = offPair[0].originalStart;
			final int offEnd = offPair[1].originalEnd;

			offPair[0].setWrapPolicy(null);
			offPair[0]
					.setIndent(Math.min(offPair[0].getIndent(), this.tokenManager.findSourcePositionInLine(offStart)));

			final List<IRegion> result = new ArrayList<>();
			for (IRegion region : this.formatRegions) {
				final int start = region.getOffset(), end = region.getOffset() + region.getLength() - 1;
				if (offEnd < start || end < offStart) {
					result.add(region);
				} else if (offStart <= start && end <= offEnd) {
					// whole region off
				} else {
					if (start < offStart)
						result.add(new Region(start, offStart - start));
					if (offEnd < end)
						result.add(new Region(offEnd + 1, end - offEnd));
				}
			}
			this.formatRegions = result;
		}
	}

	/**
	 * True if
	 * <li>1. All regions are within maxLength
	 * <li>2. regions are sorted
	 * <li>3. regions are not overlapping
	 */
	private boolean regionsSatisfiesPreconditions(IRegion[] regions, int maxLength) {
		int regionsLength = regions == null ? 0 : regions.length;
		if (regionsLength == 0) {
			return false;
		}

		IRegion first = regions[0];
		if (first.getOffset() < 0 || first.getLength() < 0 || first.getOffset() + first.getLength() > maxLength) {
			return false;
		}

		int lastOffset = first.getOffset() + first.getLength() - 1;
		for (int i = 1; i < regionsLength; i++) {
			IRegion current = regions[i];
			if (lastOffset > current.getOffset()) {
				return false;
			}

			if (current.getOffset() < 0 || current.getLength() < 0
					|| current.getOffset() + current.getLength() > maxLength) {
				return false;
			}

			lastOffset = current.getOffset() + current.getLength() - 1;
		}

		return true;
	}

	private void updateWorkingOptions(int indentationLevel, String lineSeparator, int kind) {
		this.workingOptions.line_separator = lineSeparator != null ? lineSeparator
				: this.originalOptions.line_separator;
		if (this.workingOptions.line_separator == null)
			this.workingOptions.line_separator = Util.LINE_SEPARATOR;

		this.workingOptions.initial_indentation_level = indentationLevel;

		this.workingOptions.comment_format_javadoc_comment = this.originalOptions.comment_format_javadoc_comment
				&& canFormatComment(kind, K_JAVA_DOC);
		this.workingOptions.comment_format_block_comment = this.originalOptions.comment_format_block_comment
				&& canFormatComment(kind, K_MULTI_LINE_COMMENT);
		this.workingOptions.comment_format_line_comment = this.originalOptions.comment_format_line_comment
				&& canFormatComment(kind, K_SINGLE_LINE_COMMENT);
	}

	private boolean canFormatComment(int kind, int commentKind) {
		if ((kind & F_INCLUDE_COMMENTS) != 0)
			return true;
		if (DefaultCodeFormatterConstants.FALSE.equals(this.oldCommentFormatOption))
			return false;
		if ((kind & K_MASK) == commentKind)
			return true;
		if (kind == K_UNKNOWN && DefaultCodeFormatterConstants.TRUE.equals(this.oldCommentFormatOption))
			return true;
		return false;
	}

	@Override
	public void setOptions(Map<String, String> options) {
		initOptions(null, options);
	}
}
