/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
package org.eclipse.jdt.internal.codeassist.complete;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.codeassist.CompletionEngine;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.parser.JavadocParser;
import org.eclipse.jdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;

/**
 * Parser specialized for decoding javadoc comments which includes cursor location for code completion.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class CompletionJavadocParser extends JavadocParser {

	// Initialize lengthes for block and inline tags tables
	public final static int INLINE_ALL_TAGS_LENGTH;
	public final static int BLOCK_ALL_TAGS_LENGTH;
	static {
		int length = 0;
		for (int i=0; i<INLINE_TAGS_LENGTH; i++) {
			length += INLINE_TAGS[i].length;
		}
		INLINE_ALL_TAGS_LENGTH  = length;
		length = 0;
		for (int i=0; i<BLOCK_TAGS_LENGTH; i++) {
			length += BLOCK_TAGS[i].length;
		}
		BLOCK_ALL_TAGS_LENGTH = length;
	}

	// Level tags are array of inline/block tags depending on compilation source level
	char[][][] levelTags = new char[2][][];
	int[] levelTagsLength = new int[2];

	// Completion specific info
	int cursorLocation;
	CompletionOnJavadoc completionNode = null;
	boolean pushText = false;
	boolean allPossibleTags = false;

	public CompletionJavadocParser(CompletionParser sourceParser) {
		super(sourceParser);
		this.scanner = new CompletionScanner(ClassFileConstants.JDK1_3);
		this.kind = COMPLETION_PARSER | TEXT_PARSE;
		initLevelTags();
		setSourceComplianceLevel();
	}

	/*
	 * Do not parse comment if completion location is not included.
	 */
	@Override
	public boolean checkDeprecation(int commentPtr) {
		boolean isDeprecated = false;

		this.cursorLocation = ((CompletionParser)this.sourceParser).cursorLocation;
		CompletionScanner completionScanner = (CompletionScanner)this.scanner;
		completionScanner.cursorLocation = this.cursorLocation;
		this.javadocStart = this.sourceParser.scanner.commentStarts[commentPtr];
		this.javadocEnd = this.sourceParser.scanner.commentStops[commentPtr];
		if (this.javadocStart <= this.cursorLocation && this.cursorLocation <= this.javadocEnd) {
			if (CompletionEngine.DEBUG) {
				System.out.println("COMPLETION in Javadoc:"); //$NON-NLS-1$
			}
			completionScanner.completionIdentifier = null;
			super.checkDeprecation(commentPtr);
		} else {
			if (this.sourceParser.scanner.commentTagStarts[commentPtr] != 0) {
				boolean previousValue = this.checkDocComment;
				this.checkDocComment = false;
				isDeprecated = super.checkDeprecation(commentPtr);
				this.checkDocComment = previousValue;
			}
			this.docComment = null;
		}
		return isDeprecated;
	}

	/*
	 * Replace stored Javadoc node with specific completion one.
	 */
	@Override
	protected boolean commentParse() {
		this.docComment = new CompletionJavadoc(this.javadocStart, this.javadocEnd);
		this.firstTagPosition = 1; // bug 429340: completion parser needs to parse the whole comment
		return super.commentParse();
	}

	/*
	 * Create argument expression. If it includes completion location, create and store completion node.
	 */
	@Override
	protected Object createArgumentReference(char[] name, int dim, boolean isVarargs, Object typeRef, long[] dimPositions, long argNamePos) throws InvalidInputException {
		// Create argument as we may need it after
		char[] argName = name==null ? CharOperation.NO_CHAR : name;
		Expression expression = (Expression) super.createArgumentReference(argName, dim, isVarargs, typeRef, dimPositions, argNamePos);
		// See if completion location is in argument
		int refStart = ((TypeReference)typeRef).sourceStart;
		int refEnd = ((TypeReference)typeRef).sourceEnd;
		boolean inCompletion = (refStart <= this.cursorLocation && this.cursorLocation <= refEnd) // completion cursor is between first and last stacked identifiers
			|| ((refStart == (refEnd+1) && refEnd == this.cursorLocation)); // or it's a completion on empty token
		if (this.completionNode == null && inCompletion) {
			JavadocArgumentExpression javadocArgument = (JavadocArgumentExpression) expression;
			TypeReference expressionType = javadocArgument.argument.type;
			if (expressionType instanceof JavadocSingleTypeReference) {
				this.completionNode = new CompletionOnJavadocSingleTypeReference((JavadocSingleTypeReference) expressionType);
			} else if (expressionType instanceof JavadocQualifiedTypeReference) {
				this.completionNode = new CompletionOnJavadocQualifiedTypeReference((JavadocQualifiedTypeReference) expressionType);
			}
			if (CompletionEngine.DEBUG) {
				System.out.println("	completion argument="+this.completionNode); //$NON-NLS-1$
			}
			return this.completionNode;
		}
		return expression;
	}

	/*
	 * Create field reference. If it includes completion location, create and store completion node.
	 */
	@Override
	protected Object createFieldReference(Object receiver) throws InvalidInputException {
		int refStart = (int) (this.identifierPositionStack[0] >>> 32);
		int refEnd = (int) this.identifierPositionStack[0];
		boolean inCompletion = (refStart <= (this.cursorLocation+1) && this.cursorLocation <= refEnd) // completion cursor is between first and last stacked identifiers
			|| ((refStart == (refEnd+1) && refEnd == this.cursorLocation)) // or it's a completion on empty token
			|| (this.memberStart == this.cursorLocation); // or it's a completion just after the member separator with an identifier after the cursor
		if (inCompletion) {
			JavadocFieldReference fieldRef = (JavadocFieldReference) super.createFieldReference(receiver);
			char[] name = this.sourceParser.compilationUnit.getMainTypeName();
			TypeDeclaration typeDecl = getParsedTypeDeclaration();
			if (typeDecl != null) {
				name = typeDecl.name;
			}
			this.completionNode = new CompletionOnJavadocFieldReference(fieldRef, this.memberStart, name);
			if (CompletionEngine.DEBUG) {
				System.out.println("	completion field="+this.completionNode); //$NON-NLS-1$
			}
			return this.completionNode;
		}
		return super.createFieldReference(receiver);
	}

	/*
	 * Verify if method identifier positions include completion location.
	 * If so, create method reference and store it.
	 * Otherwise return null as we do not need this reference.
	 */
	@Override
	protected Object createMethodReference(Object receiver, List arguments) throws InvalidInputException {
		int memberPtr = this.identifierLengthStack[0] - 1; // may be > 0 for inner class constructor reference
		int refStart = (int) (this.identifierPositionStack[memberPtr] >>> 32);
		int refEnd = (int) this.identifierPositionStack[memberPtr];
		boolean inCompletion = (refStart <= (this.cursorLocation+1) && this.cursorLocation <= refEnd) // completion cursor is between first and last stacked identifiers
			|| ((refStart == (refEnd+1) && refEnd == this.cursorLocation)) // or it's a completion on empty token
			|| (this.memberStart == this.cursorLocation); // or it's a completion just after the member separator with an identifier after the cursor
		if (inCompletion) {
			ASTNode node = (ASTNode) super.createMethodReference(receiver, arguments);
			if (node instanceof JavadocMessageSend) {
				JavadocMessageSend messageSend = (JavadocMessageSend) node;
				int nameStart = (int) (messageSend.nameSourcePosition >>> 32);
				int nameEnd = (int) messageSend.nameSourcePosition;
				if ((nameStart <= (this.cursorLocation+1) && this.cursorLocation <= nameEnd)) {
					this.completionNode = new CompletionOnJavadocFieldReference(messageSend, this.memberStart);
				} else {
					this.completionNode = new CompletionOnJavadocMessageSend(messageSend, this.memberStart);
				}
			} else if (node instanceof JavadocAllocationExpression) {
				this.completionNode = new CompletionOnJavadocAllocationExpression((JavadocAllocationExpression)node, this.memberStart);
			}
			if (CompletionEngine.DEBUG) {
				System.out.println("	completion method="+this.completionNode); //$NON-NLS-1$
			}
			return this.completionNode;
		}
		return super.createMethodReference(receiver, arguments);
	}

	/*
	 * Create type reference. If it includes completion location, create and store completion node.
	 */
	@Override
	protected Object createTypeReference(int primitiveToken, boolean canBeModule) {
		// Need to create type ref in case it was needed by members
		int nbIdentifiers = this.identifierLengthStack[this.identifierLengthPtr];
		int startPtr = this.identifierPtr - (nbIdentifiers-1);
		int refStart = (int) (this.identifierPositionStack[startPtr] >>> 32);
		int refEnd = (int) this.identifierPositionStack[this.identifierPtr];
		boolean inCompletion = (refStart <= (this.cursorLocation+1) && this.cursorLocation <= refEnd) // completion cursor is between first and last stacked identifiers
			|| ((refStart == (refEnd+1) && refEnd == this.cursorLocation)); // or it's a completion on empty token
		if (!inCompletion) {
			return super.createTypeReference(primitiveToken, canBeModule);
		}
		this.identifierLengthPtr--;
		if (nbIdentifiers == 1) { // Single Type ref
			this.completionNode = new CompletionOnJavadocSingleTypeReference(
						this.identifierStack[this.identifierPtr],
						this.identifierPositionStack[this.identifierPtr],
						this.tagSourceStart,
						this.tagSourceEnd);
		} else if (nbIdentifiers > 1) { // Qualified Type ref
			for (int i=startPtr; i<this.identifierPtr; i++) {
				int start = (int) (this.identifierPositionStack[i] >>> 32);
				int end = (int) this.identifierPositionStack[i];
				if (start <= this.cursorLocation && this.cursorLocation <= end) {
					if (i == startPtr) {
						this.completionNode = new CompletionOnJavadocSingleTypeReference(
									this.identifierStack[startPtr],
									this.identifierPositionStack[startPtr],
									this.tagSourceStart,
									this.tagSourceEnd);
					} else {
						char[][] tokens = new char[i][];
						System.arraycopy(this.identifierStack, startPtr, tokens, 0, i);
						long[] positions = new long[i+1];
						System.arraycopy(this.identifierPositionStack, startPtr, positions, 0, i+1);
						this.completionNode = new CompletionOnJavadocQualifiedTypeReference(tokens, this.identifierStack[i], positions, this.tagSourceStart, this.tagSourceEnd);
					}
					break;
				}
			}
			if (this.completionNode == null) {
				char[][] tokens = new char[nbIdentifiers-1][];
				System.arraycopy(this.identifierStack, startPtr, tokens, 0, nbIdentifiers-1);
				long[] positions = new long[nbIdentifiers];
				System.arraycopy(this.identifierPositionStack, startPtr, positions, 0, nbIdentifiers);
				this.completionNode = new CompletionOnJavadocQualifiedTypeReference(tokens, this.identifierStack[this.identifierPtr], positions, this.tagSourceStart, this.tagSourceEnd);
			}
		}

		if (CompletionEngine.DEBUG) {
			System.out.println("	completion partial qualified type="+this.completionNode); //$NON-NLS-1$
		}
		return this.completionNode;
	}

	@Override
	protected Object createModuleTypeReference(int primitiveToken, int moduleRefTokenCount) {

		// Need to create type ref in case it was needed by members
		int nbIdentifiers = this.identifierLengthStack[this.identifierLengthPtr];
		int startPtr = this.identifierPtr - (nbIdentifiers-1);
		int refStart = (int) (this.identifierPositionStack[startPtr] >>> 32);
		int refEnd = (int) this.identifierPositionStack[this.identifierPtr];
		boolean inCompletion = (refStart <= (this.cursorLocation+1) && this.cursorLocation <= refEnd) // completion cursor is between first and last stacked identifiers
			|| ((refStart == (refEnd+1) && refEnd == this.cursorLocation)); // or it's a completion on empty token
		if (!inCompletion) {
			return super.createModuleTypeReference(primitiveToken, moduleRefTokenCount);
		}

		JavadocModuleReference moduleRef= createModuleReference(moduleRefTokenCount);

		TypeReference typeRef = null;
		int size = this.identifierLengthStack[this.identifierLengthPtr];
		int newSize= size-moduleRefTokenCount;
		if (newSize == 1) { // Single Type ref
			typeRef = new CompletionOnJavadocSingleTypeReference(
					this.identifierStack[moduleRefTokenCount],
					this.identifierPositionStack[moduleRefTokenCount],
					this.tagSourceStart,
					this.tagSourceEnd);
		} else if (newSize > 1) { // Qualified Type ref
			for (int i=moduleRefTokenCount+1; i<this.identifierPtr; i++) {
				int start = (int) (this.identifierPositionStack[i] >>> 32);
				int end = (int) this.identifierPositionStack[i];
				if (start <= this.cursorLocation && this.cursorLocation <= end) {
					if (i == moduleRefTokenCount) {
						this.completionNode = new CompletionOnJavadocSingleTypeReference(
									this.identifierStack[moduleRefTokenCount+1],
									this.identifierPositionStack[moduleRefTokenCount+1],
									this.tagSourceStart,
									this.tagSourceEnd);
					} else {
						char[][] tokens = new char[i][];
						System.arraycopy(this.identifierStack, moduleRefTokenCount, tokens, 0, i);
						long[] positions = new long[i+1];
						System.arraycopy(this.identifierPositionStack, moduleRefTokenCount, positions, 0, i+1);
						typeRef = new CompletionOnJavadocQualifiedTypeReference(tokens, this.identifierStack[i], positions, this.tagSourceStart, this.tagSourceEnd);
					}
					break;
				}
			}
			if (this.completionNode == null) {
				char[][] tokens = new char[newSize-1][];
				System.arraycopy(this.identifierStack, moduleRefTokenCount, tokens, 0, newSize-1);
				long[] positions = new long[newSize];
				System.arraycopy(this.identifierPositionStack, moduleRefTokenCount, positions, 0, newSize);
				typeRef = new CompletionOnJavadocQualifiedTypeReference(tokens, this.identifierStack[this.identifierPtr], positions, this.tagSourceStart, this.tagSourceEnd);
			}

		}

		moduleRef.setTypeReference(typeRef);
		this.completionNode = (CompletionOnJavadocModuleReference)moduleRef;
		return moduleRef;
	}

	@Override
	protected JavadocModuleReference createModuleReference(int moduleRefTokenCount) {
		JavadocModuleReference moduleRef = null;
		char[][] tokens = new char[moduleRefTokenCount][];
		System.arraycopy(this.identifierStack, 0, tokens, 0, moduleRefTokenCount);
		long[] positions = new long[moduleRefTokenCount+1];
		System.arraycopy(this.identifierPositionStack, 0, positions, 0, moduleRefTokenCount+1);
		moduleRef = new CompletionOnJavadocModuleReference(tokens, this.identifierStack[this.identifierPtr], positions, this.tagSourceStart, this.tagSourceEnd);
		return moduleRef;
	}

	/*
	 * Get possible tags for a given prefix.
	 */
	private char[][][] possibleTags(char[] prefix, boolean newLine) {
		char[][][] possibleTags = new char[2][][];
		if (newLine) {
			System.arraycopy(this.levelTags[BLOCK_IDX], 0, possibleTags[BLOCK_IDX] = new char[this.levelTagsLength[BLOCK_IDX]][], 0, this.levelTagsLength[BLOCK_IDX]);
		} else {
			possibleTags[BLOCK_IDX] = CharOperation.NO_CHAR_CHAR;
		}
		System.arraycopy(this.levelTags[INLINE_IDX], 0, possibleTags[INLINE_IDX] = new char[this.levelTagsLength[INLINE_IDX]][], 0, this.levelTagsLength[INLINE_IDX]);
		if (prefix == null || prefix.length == 0) return possibleTags;
		int kinds = this.levelTags.length;
		for (int k=0; k<kinds; k++) {
			int length = possibleTags[k].length, size = 0;
			int indexes[] = new int[length];
			for (int i=0; i<length; i++) {
				if (CharOperation.prefixEquals(prefix, possibleTags[k][i], false)) {
					indexes[size++] = i;
				}
			}
			char[][] tags = new char[size][];
			for (int i=0; i<size; i++) {
				tags[i] = possibleTags[k][indexes[i]];
			}
			possibleTags[k] = tags;
		}
		return possibleTags;
	}

	private CompletionJavadoc getCompletionJavadoc() {
		return (CompletionJavadoc)this.docComment;
	}

	private CompletionParser getCompletionParser() {
		return (CompletionParser)this.sourceParser;
	}

	/*
	 * Init tags arrays for current source level.
	 */
	private void initLevelTags() {
		int level = ((int)(this.complianceLevel >>> 16)) - ClassFileConstants.MAJOR_VERSION_1_1 + 1;
		if ( level >= BLOCK_TAGS_LENGTH)
			return; // To support future JDKs
		// Init block tags
		this.levelTags[BLOCK_IDX] = new char[BLOCK_ALL_TAGS_LENGTH][];
		this.levelTagsLength[BLOCK_IDX] = 0;
		for (int i=0; i<=level; i++) {
			int length = BLOCK_TAGS[i].length;
			System.arraycopy(BLOCK_TAGS[i], 0, this.levelTags[BLOCK_IDX], this.levelTagsLength[BLOCK_IDX], length);
			this.levelTagsLength[BLOCK_IDX] += length;
		}
		if (this.levelTagsLength[BLOCK_IDX] < BLOCK_ALL_TAGS_LENGTH) {
			System.arraycopy(this.levelTags[BLOCK_IDX], 0, this.levelTags[BLOCK_IDX] = new char[this.levelTagsLength[BLOCK_IDX]][], 0, this.levelTagsLength[BLOCK_IDX]);
		}
		// Init inline tags
		this.levelTags[INLINE_IDX] = new char[INLINE_ALL_TAGS_LENGTH][];
		this.levelTagsLength[INLINE_IDX]= 0;
		for (int i=0; i<=level; i++) {
			int length = INLINE_TAGS[i].length;
			System.arraycopy(INLINE_TAGS[i], 0, this.levelTags[INLINE_IDX], this.levelTagsLength[INLINE_IDX], length);
			this.levelTagsLength[INLINE_IDX] += length;
		}
		if (this.levelTagsLength[INLINE_IDX] < INLINE_ALL_TAGS_LENGTH) {
			System.arraycopy(this.levelTags[INLINE_IDX], 0, this.levelTags[INLINE_IDX] = new char[this.levelTagsLength[INLINE_IDX]][], 0, this.levelTagsLength[INLINE_IDX]);
		}
	}
	/*
	 * Parse argument in @see tag method reference
	 */
	@Override
	protected Object parseArguments(Object receiver) throws InvalidInputException {

		if (this.tagSourceStart>this.cursorLocation) {
			return super.parseArguments(receiver);
		}

		// Init
		int modulo = 0; // should be 2 for (Type,Type,...) or 3 for (Type arg,Type arg,...)
		int iToken = 0;
		char[] argName = null;
		List arguments = new ArrayList(10);
		Object typeRef = null;
		int dim = 0;
		boolean isVarargs = false;
		long[] dimPositions = new long[20]; // assume that there won't be more than 20 dimensions...
		char[] name = null;
		long argNamePos = -1;
		boolean tokenWhiteSpace = this.scanner.tokenizeWhiteSpace;
		this.scanner.tokenizeWhiteSpace = false;

		try {
			// Parse arguments declaration if method reference
			nextArg : while (this.index < this.scanner.eofPosition) {

				// Read argument type reference
				try {
					typeRef = parseQualifiedName(false);
					if (this.abort) return null; // May be aborted by specialized parser
				} catch (InvalidInputException e) {
					break nextArg;
				}
				boolean firstArg = modulo == 0;
				if (firstArg) { // verify position
					if (iToken != 0)
						break nextArg;
				} else if ((iToken % modulo) != 0) {
						break nextArg;
				}
				if (typeRef == null) {
					if (firstArg && getCurrentTokenType() == TerminalTokens.TokenNameRPAREN) {
						this.lineStarted = true;
						return createMethodReference(receiver, null);
					}
					Object methodRef = createMethodReference(receiver, arguments);
					return syntaxRecoverEmptyArgumentType(methodRef);
				}
				if (this.index >= this.scanner.eofPosition) {
					int argumentStart = ((ASTNode)typeRef).sourceStart;
					Object argument = createArgumentReference(this.scanner.getCurrentIdentifierSource(), 0, false, typeRef, null, (((long)argumentStart)<<32)+this.tokenPreviousPosition-1);
					return syntaxRecoverArgumentType(receiver, arguments, argument);
				}
				if (this.index >= this.cursorLocation) {
					if (this.completionNode instanceof CompletionOnJavadocSingleTypeReference) {
						CompletionOnJavadocSingleTypeReference singleTypeReference = (CompletionOnJavadocSingleTypeReference) this.completionNode;
						if (singleTypeReference.token == null || singleTypeReference.token.length == 0) {
							Object methodRef = createMethodReference(receiver, arguments);
							return syntaxRecoverEmptyArgumentType(methodRef);
						}
					}
					if (this.completionNode instanceof CompletionOnJavadocQualifiedTypeReference) {
						CompletionOnJavadocQualifiedTypeReference qualifiedTypeReference = (CompletionOnJavadocQualifiedTypeReference) this.completionNode;
						if (qualifiedTypeReference.tokens == null || qualifiedTypeReference.tokens.length < qualifiedTypeReference.sourcePositions.length) {
							Object methodRef = createMethodReference(receiver, arguments);
							return syntaxRecoverEmptyArgumentType(methodRef);
						}
					}
				}
				iToken++;

				// Read possible additional type info
				dim = 0;
				isVarargs = false;
				if (readToken() == TerminalTokens.TokenNameLBRACKET) {
					// array declaration
					int dimStart = this.scanner.getCurrentTokenStartPosition();
					while (readToken() == TerminalTokens.TokenNameLBRACKET) {
						consumeToken();
						if (readToken() != TerminalTokens.TokenNameRBRACKET) {
							break nextArg;
						}
						consumeToken();
						dimPositions[dim++] = (((long) dimStart) << 32) + this.scanner.getCurrentTokenEndPosition();
					}
				} else if (readToken() == TerminalTokens.TokenNameELLIPSIS) {
					// ellipsis declaration
					int dimStart = this.scanner.getCurrentTokenStartPosition();
					dimPositions[dim++] = (((long) dimStart) << 32) + this.scanner.getCurrentTokenEndPosition();
					consumeToken();
					isVarargs = true;
				}

				// Read argument name
				argNamePos = -1;
				if (readToken() == TerminalTokens.TokenNameIdentifier) {
					consumeToken();
					if (firstArg) { // verify position
						if (iToken != 1)
							break nextArg;
					} else if ((iToken % modulo) != 1) {
							break nextArg;
					}
					if (argName == null) { // verify that all arguments name are declared
						if (!firstArg) {
							break nextArg;
						}
					}
					argName = this.scanner.getCurrentIdentifierSource();
					argNamePos = (((long)this.scanner.getCurrentTokenStartPosition())<<32)+this.scanner.getCurrentTokenEndPosition();
					iToken++;
				} else if (argName != null) { // verify that no argument name is declared
					break nextArg;
				}

				// Verify token position
				if (firstArg) {
					modulo = iToken + 1;
				} else {
					if ((iToken % modulo) != (modulo - 1)) {
						break nextArg;
					}
				}

				// Read separator or end arguments declaration
				int token = readToken();
				name = argName == null ? CharOperation.NO_CHAR : argName;
				if (token == TerminalTokens.TokenNameCOMMA) {
					// Create new argument
					Object argument = createArgumentReference(name, dim, isVarargs, typeRef, dimPositions, argNamePos);
					if (this.abort) return null; // May be aborted by specialized parser
					arguments.add(argument);
					consumeToken();
					iToken++;
				} else if (token == TerminalTokens.TokenNameRPAREN) {
					// Create new argument
					Object argument = createArgumentReference(name, dim, isVarargs, typeRef, dimPositions, argNamePos);
					if (this.abort) return null; // May be aborted by specialized parser
					arguments.add(argument);
					consumeToken();
					return createMethodReference(receiver, arguments);
				} else {
					Object argument = createArgumentReference(name, dim, isVarargs, typeRef, dimPositions, argNamePos);
					return syntaxRecoverArgumentType(receiver, arguments, argument);
				}
			}

			// Something wrong happened => Invalid input
			throw new InvalidInputException();
		} finally {
			// we have to make sure that this is reset to the previous value even if an exception occurs
			this.scanner.tokenizeWhiteSpace = tokenWhiteSpace;
		}
	}

		@Override
		protected boolean parseParam() throws InvalidInputException {
			int startPosition = this.index;
			int endPosition = this.index;
			long namePosition = (((long)startPosition)<<32) + endPosition;
			this.identifierPtr = -1;
			boolean valid = super.parseParam();
			if (this.identifierPtr > 2) return valid;
			// See if expression is concerned by completion
			char[] name = null;
			CompletionScanner completionScanner = (CompletionScanner) this.scanner;
			boolean isTypeParam = false;
			if (this.identifierPtr >= 0) {
				char[] identifier = null;
				switch (this.identifierPtr) {
					case 2:
						if (!valid && completionScanner.completionIdentifier != null && completionScanner.completionIdentifier.length == 0) {
							valid = pushParamName(true);
						}
						// $FALL-THROUGH$ - fall through next case to verify and get identifiers stack contents
					case 1:
						isTypeParam = this.identifierStack[0][0] == '<';
						identifier = this.identifierStack[1];
						namePosition = this.identifierPositionStack[1];
						break;
					case 0:
						identifier = this.identifierStack[0];
						namePosition = this.identifierPositionStack[0];
						isTypeParam = identifier.length > 0 && identifier[0] == '<';
						break;
				}
				if (identifier != null && identifier.length > 0 && ScannerHelper.isJavaIdentifierPart(this.complianceLevel, identifier[0])) {
					name = identifier;
				}
				startPosition = (int)(this.identifierPositionStack[0]>>32);
				endPosition = (int)this.identifierPositionStack[this.identifierPtr];
			}
			boolean inCompletion = (startPosition <= (this.cursorLocation+1) && this.cursorLocation <= endPosition) // completion cursor is between first and last stacked identifiers
				|| ((startPosition == (endPosition+1) && endPosition == this.cursorLocation)); // or it's a completion on empty token
			if (inCompletion) {
				if (this.completionNode == null) {
					if (isTypeParam) {
						this.completionNode = new CompletionOnJavadocTypeParamReference(name, namePosition, startPosition, endPosition);
					} else {
						this.completionNode = new CompletionOnJavadocParamNameReference(name, namePosition, startPosition, endPosition);
					}
					if (CompletionEngine.DEBUG) {
						System.out.println("	completion param="+this.completionNode); //$NON-NLS-1$
					}
				} else if (this.completionNode instanceof CompletionOnJavadocParamNameReference) {
					CompletionOnJavadocParamNameReference paramNameRef = (CompletionOnJavadocParamNameReference)this.completionNode;
					int nameStart = (int) (namePosition>>32);
					paramNameRef.sourceStart = nameStart;
					int nameEnd = (int) namePosition;
					if (nameStart<this.cursorLocation && this.cursorLocation<nameEnd) {
						paramNameRef.sourceEnd = this.cursorLocation + 1;
					} else {
						paramNameRef.sourceEnd = nameEnd;
					}
					paramNameRef.tagSourceStart = startPosition;
					paramNameRef.tagSourceEnd = endPosition;
				} else if (this.completionNode instanceof CompletionOnJavadocTypeParamReference) {
					CompletionOnJavadocTypeParamReference typeParamRef = (CompletionOnJavadocTypeParamReference)this.completionNode;
					int nameStart = (int) (namePosition>>32);
					typeParamRef.sourceStart = nameStart;
					int nameEnd = (int) namePosition;
					if (nameStart<this.cursorLocation && this.cursorLocation<nameEnd) {
						typeParamRef.sourceEnd = this.cursorLocation + 1;
					} else {
						typeParamRef.sourceEnd = nameEnd;
					}
					typeParamRef.tagSourceStart = startPosition;
					typeParamRef.tagSourceEnd = endPosition;
				}
			}
			return valid;
		}

		@Override
		protected boolean parseReference() throws InvalidInputException {
			return parseReference(false);
		}

		@Override
		protected boolean parseReference(boolean allowModule) throws InvalidInputException {
			boolean completed = this.completionNode != null;
			boolean valid = super.parseReference(allowModule);
			if (!completed && this.completionNode != null) {
				this.completionNode.addCompletionFlags(CompletionOnJavadoc.FORMAL_REFERENCE);
			}
			return valid;
		}

	@Override
	protected boolean parseTag(int previousPosition) throws InvalidInputException {
		int startPosition = this.inlineTagStarted ? this.inlineTagStart : previousPosition;
		boolean newLine = !this.lineStarted;
		boolean valid = super.parseTag(previousPosition);
		boolean inCompletion = (this.tagSourceStart <= (this.cursorLocation+1) && this.cursorLocation <= this.tagSourceEnd) // completion cursor is between first and last stacked identifiers
			|| ((this.tagSourceStart == (this.tagSourceEnd+1) && this.tagSourceEnd == this.cursorLocation)); // or it's a completion on empty token
		if (inCompletion) {
			int end = this.tagSourceEnd;
			if (this.inlineTagStarted && this.scanner.currentCharacter == '}') {
				end = this.scanner.currentPosition;
			}
			long position = (((long)startPosition)<<32) + end;
			int length = this.cursorLocation+1-this.tagSourceStart;
			char[] tag = new char[length];
			System.arraycopy(this.source, this.tagSourceStart, tag, 0, length);
			char[][][] tags = possibleTags(tag, newLine);
			if (tags != null) {
				this.completionNode = new CompletionOnJavadocTag(tag, position, startPosition, end, tags, this.allPossibleTags);
			}
		}
		return valid;
	}

	@Override
	protected boolean parseThrows() {
		try {
			Object typeRef = parseQualifiedName(true);
			if (this.completionNode != null) {
				this.completionNode.addCompletionFlags(CompletionOnJavadoc.EXCEPTION);
			}
			return pushThrowName(typeRef);
		} catch (InvalidInputException ex) {
			// ignore
		}
		return false;
	}

	/*
	 * Push param name reference. If it includes completion location, create and store completion node.
	 */
	@Override
	protected boolean pushParamName(boolean isTypeParam) {
		if (super.pushParamName(isTypeParam)) {
			Expression expression = (Expression) this.astStack[this.astPtr];
			// See if expression is concerned by completion
			if (expression.sourceStart <= (this.cursorLocation+1) && this.cursorLocation <= expression.sourceEnd) {
				if (isTypeParam) {
					this.completionNode = new CompletionOnJavadocTypeParamReference((JavadocSingleTypeReference)expression);
				} else {
					this.completionNode = new CompletionOnJavadocParamNameReference((JavadocSingleNameReference)expression);
				}
				if (CompletionEngine.DEBUG) {
					System.out.println("	completion param="+this.completionNode); //$NON-NLS-1$
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Push text. If it includes completion location, then rescan line to see if there's a possible
	 * reference under the cursor location.
	 *
	 * @see org.eclipse.jdt.internal.compiler.parser.AbstractCommentParser#pushText(int, int)
	 */
	@Override
	protected void pushText(int start, int end) {
		if (start <= this.cursorLocation && this.cursorLocation <= end) {
			this.scanner.resetTo(start, end);
			boolean tokenizeWhiteSpace = this.scanner.tokenizeWhiteSpace;
			this.scanner.tokenizeWhiteSpace = true;
			try {
				Object typeRef = null;
				this.pushText = true;

				// Get reference tokens
				int previousToken = TerminalTokens.TokenNameWHITESPACE;
				while (!this.scanner.atEnd() && this.completionNode == null && !this.abort) {
					int token = readTokenSafely();
					switch (token) {
						case TerminalTokens.TokenNameStringLiteral :
							int strStart = 0, strEnd = 0;
							if ((strStart=this.scanner.getCurrentTokenStartPosition()+1) <= this.cursorLocation &&
								this.cursorLocation <= (strEnd=this.scanner.getCurrentTokenEndPosition()-1))
							{
								this.scanner.resetTo(strStart, strEnd);
							}
							consumeToken();
							break;
						case TerminalTokens.TokenNameERROR :
							consumeToken();
							if (this.scanner.currentCharacter == '#') { // @see ...#member
								Object member = null;
								try {
									this.scanner.tokenizeWhiteSpace = false;
									member = parseMember(typeRef);
								} catch (InvalidInputException e) {
									consumeToken();
								}
								this.scanner.tokenizeWhiteSpace = true;
								if (this.completionNode != null) {
									int flags = this.inlineTagStarted ? 0 : CompletionOnJavadoc.TEXT|CompletionOnJavadoc.ONLY_INLINE_TAG;
									if (member instanceof JavadocMessageSend) {
										JavadocMessageSend msgSend = (JavadocMessageSend) member;
										this.completionNode = new CompletionOnJavadocMessageSend(msgSend, this.memberStart, flags);
										if (CompletionEngine.DEBUG) {
											System.out.println("	new completion method="+this.completionNode); //$NON-NLS-1$
										}
									} else if (member instanceof JavadocAllocationExpression) {
										JavadocAllocationExpression alloc = (JavadocAllocationExpression) member;
										this.completionNode = new CompletionOnJavadocAllocationExpression(alloc, this.memberStart, flags);
										if (CompletionEngine.DEBUG) {
											System.out.println("	new completion method="+this.completionNode); //$NON-NLS-1$
										}
									} else {
										this.completionNode.addCompletionFlags(flags);
									}
								}
							}
							break;
						case TerminalTokens.TokenNameIdentifier :
							try {
								this.scanner.tokenizeWhiteSpace = false;
								typeRef = parseQualifiedName(true);
								if (this.completionNode == null) {
									consumeToken();
									this.scanner.resetTo(this.tokenPreviousPosition, end);
									this.index = this.tokenPreviousPosition;
								}
							}
							catch (InvalidInputException e) {
								consumeToken();
							}
							finally {
								this.scanner.tokenizeWhiteSpace = true;
							}
							if (previousToken != TerminalTokens.TokenNameWHITESPACE) {
								typeRef = null;
								this.completionNode = null;
							}
							break;
						case TerminalTokens.TokenNameAT:
							consumeToken();
							try {
								this.scanner.tokenizeWhiteSpace = false;
								int startPosition = this.scanner.getCurrentTokenStartPosition();
								parseTag(startPosition);
								if (this.completionNode != null) {
									if (this.inlineTagStarted) {
										/* May be to replace invalid @value tag inside text?
										if (this.completionNode instanceof CompletionOnJavadocSingleTypeReference) {
											CompletionOnJavadocSingleTypeReference singleTypeReference = (CompletionOnJavadocSingleTypeReference) this.completionNode;
											singleTypeReference.tagSourceStart = startPosition;
											switch (this.tagValue) {
												case TAG_VALUE_VALUE:
//													singleTypeReference.completionFlags |= ONLY_INLINE_TAG;
													if (this.sourceLevel < ClassFileConstants.JDK1_5) singleTypeReference.completionFlags |= REPLACE_TAG;
													break;
											}
										} else if (this.completionNode instanceof CompletionOnJavadocQualifiedTypeReference) {
											CompletionOnJavadocQualifiedTypeReference qualifiedTypeRef = (CompletionOnJavadocQualifiedTypeReference) this.completionNode;
											qualifiedTypeRef.tagSourceStart = startPosition;
											switch (this.tagValue) {
												case TAG_VALUE_VALUE:
													singleTypeReference.completionFlags |= ONLY_INLINE_TAG;
													if (this.sourceLevel < ClassFileConstants.JDK1_5) qualifiedTypeRef.completionFlags |= REPLACE_TAG;
													break;
											}
										}
//										*/
									} else {
										/* May be to replace non-inline tag inside text?
										if (this.completionNode instanceof CompletionOnJavadocSingleTypeReference) {
											CompletionOnJavadocSingleTypeReference singleTypeReference = (CompletionOnJavadocSingleTypeReference) this.completionNode;
											singleTypeReference.tagSourceStart = startPosition;
											switch (this.tagValue) {
												case TAG_LINK_VALUE:
												case TAG_LINKPLAIN_VALUE:
													singleTypeReference.completionFlags |= ONLY_INLINE_TAG;
												case TAG_SEE_VALUE:
													singleTypeReference.completionFlags |= REPLACE_TAG;
													break;
											}
										} else if (this.completionNode instanceof CompletionOnJavadocQualifiedTypeReference) {
											CompletionOnJavadocQualifiedTypeReference qualifiedTypeRef = (CompletionOnJavadocQualifiedTypeReference) this.completionNode;
											qualifiedTypeRef.tagSourceStart = startPosition;
											switch (this.tagValue) {
												case TAG_LINK_VALUE:
												case TAG_LINKPLAIN_VALUE:
													qualifiedTypeRef.completionFlags |= ONLY_INLINE_TAG;
												case TAG_SEE_VALUE:
													qualifiedTypeRef.completionFlags |= REPLACE_TAG;
													break;
											}
										}
//										*/
									}
								}
							} catch (InvalidInputException e) {
								consumeToken();
							}
							this.scanner.tokenizeWhiteSpace = true;
							break;
						default :
							consumeToken();
							typeRef = null;
							break;
					}
					previousToken = token;
				}
			}
			finally {
				this.scanner.tokenizeWhiteSpace = tokenizeWhiteSpace;
				this.pushText = false;
			}

			// Reset position to avoid missing tokens when new line was encountered
			this.index = end;
			this.scanner.currentPosition = end;
			consumeToken();

			if (this.completionNode != null) {
				if (this.inlineTagStarted) {
					this.completionNode.addCompletionFlags(CompletionOnJavadoc.FORMAL_REFERENCE);
				} else {
					this.completionNode.addCompletionFlags(CompletionOnJavadoc.TEXT);
				}
			}
		}
	}

	@Override
	protected int readToken() throws InvalidInputException {
		int token = super.readToken();
		if (token == TerminalTokens.TokenNameIdentifier && this.scanner.currentPosition == this.scanner.startPosition) {
			// Scanner is looping on empty token => read it...
			this.scanner.getCurrentIdentifierSource();
		}
		return token;
	}

	/*
	 * Recover syntax on invalid qualified name.
	 */
	@Override
	protected Object syntaxRecoverQualifiedName(int primitiveToken) throws InvalidInputException {
		if (this.cursorLocation == ((int)this.identifierPositionStack[this.identifierPtr])) {
			// special case of completion just before the dot.
			return createTypeReference(primitiveToken);
		}
		int idLength = this.identifierLengthStack[this.identifierLengthPtr];
		char[][] tokens = new char[idLength][];
		int startPtr = this.identifierPtr-idLength+1;
		System.arraycopy(this.identifierStack, startPtr, tokens, 0, idLength);
		long[] positions = new long[idLength+1];
		System.arraycopy(this.identifierPositionStack, startPtr, positions, 0, idLength);
		positions[idLength] = (((long)this.tokenPreviousPosition)<<32) + this.tokenPreviousPosition;
		this.completionNode = new CompletionOnJavadocQualifiedTypeReference(tokens, CharOperation.NO_CHAR, positions, this.tagSourceStart, this.tagSourceEnd);

		if (CompletionEngine.DEBUG) {
			System.out.println("	completion partial qualified type="+this.completionNode); //$NON-NLS-1$
		}
		return this.completionNode;
	}

	/*
	 * Recover syntax on type argument in invalid method/constructor reference
	 */
	protected Object syntaxRecoverArgumentType(Object receiver, List arguments, Object argument) throws InvalidInputException {
		if (this.completionNode != null && !this.pushText) {
			this.completionNode.addCompletionFlags(CompletionOnJavadoc.BASE_TYPES);
			if (this.completionNode instanceof CompletionOnJavadocSingleTypeReference) {
				char[] token = ((CompletionOnJavadocSingleTypeReference)this.completionNode).token;
				if (token != null && token.length > 0) {
					return this.completionNode;
				}
			} else {
				return this.completionNode;
			}
		}
		// Filter empty token
		if (this.completionNode instanceof CompletionOnJavadocSingleTypeReference) {
			CompletionOnJavadocSingleTypeReference singleTypeReference = (CompletionOnJavadocSingleTypeReference) this.completionNode;
			if (singleTypeReference.token != null && singleTypeReference.token.length > 0) {
				arguments.add(argument);
			}
		} else if (this.completionNode instanceof CompletionOnJavadocQualifiedTypeReference) {
			CompletionOnJavadocQualifiedTypeReference qualifiedTypeReference = (CompletionOnJavadocQualifiedTypeReference) this.completionNode;
			if (qualifiedTypeReference.tokens != null && qualifiedTypeReference.tokens.length == qualifiedTypeReference.sourcePositions.length) {
				arguments.add(argument);
			}
		} else {
			arguments.add(argument);
		}
		Object methodRef = super.createMethodReference(receiver, arguments);
		if (methodRef instanceof JavadocMessageSend) {
			JavadocMessageSend msgSend = (JavadocMessageSend) methodRef;
			if (this.index > this.cursorLocation) {
				msgSend.sourceEnd = this.tokenPreviousPosition-1;
			}
			int nameStart = (int) (msgSend.nameSourcePosition >>> 32);
			int nameEnd = (int) msgSend.nameSourcePosition;
			if ((nameStart <= (this.cursorLocation+1) && this.cursorLocation <= nameEnd)) {
				this.completionNode = new CompletionOnJavadocFieldReference(msgSend, this.memberStart);
			} else {
				this.completionNode = new CompletionOnJavadocMessageSend(msgSend, this.memberStart);
			}
		} else if (methodRef instanceof JavadocAllocationExpression) {
			JavadocAllocationExpression allocExp = (JavadocAllocationExpression) methodRef;
			if (this.index > this.cursorLocation) {
				allocExp.sourceEnd = this.tokenPreviousPosition-1;
			}
			this.completionNode = new CompletionOnJavadocAllocationExpression(allocExp, this.memberStart);
		}
		if (CompletionEngine.DEBUG) {
			System.out.println("	completion method="+this.completionNode); //$NON-NLS-1$
		}
		return this.completionNode;
	}

	/*
	 * Recover syntax on empty type argument in invalid method/constructor reference
	 */
	protected Object syntaxRecoverEmptyArgumentType(Object methodRef) throws InvalidInputException {
		if (methodRef instanceof JavadocMessageSend) {
			JavadocMessageSend msgSend = (JavadocMessageSend) methodRef;
			if (this.index > this.cursorLocation) {
				msgSend.sourceEnd = this.tokenPreviousPosition-1;
			}
			this.completionNode = new CompletionOnJavadocMessageSend(msgSend, this.memberStart);
		} else if (methodRef instanceof JavadocAllocationExpression) {
			JavadocAllocationExpression allocExp = (JavadocAllocationExpression) methodRef;
			if (this.index > this.cursorLocation) {
				allocExp.sourceEnd = this.tokenPreviousPosition-1;
			}
			this.completionNode = new CompletionOnJavadocAllocationExpression(allocExp, this.memberStart);
		}
		if (CompletionEngine.DEBUG) {
			System.out.println("	completion method="+this.completionNode); //$NON-NLS-1$
		}
		return this.completionNode;
	}

	/*
	 * Store completion node into doc comment.
	 */
	@Override
	protected void updateDocComment() {
		super.updateDocComment();
		if (this.completionNode instanceof Expression && ((Expression) this.completionNode).isTrulyExpression()) {
			getCompletionParser().assistNodeParent = this.docComment;
			getCompletionParser().assistNode = (ASTNode) this.completionNode;
			getCompletionJavadoc().completionNode = (Expression) this.completionNode;
		}
	}

	@Override
	protected boolean verifySpaceOrEndComment() {
		CompletionScanner completionScanner = (CompletionScanner) this.scanner;
		if (completionScanner.completionIdentifier != null && completionScanner.completedIdentifierStart <= this.cursorLocation && this.cursorLocation <= completionScanner.completedIdentifierEnd) {
			// if we're on completion location do not verify end...
			return true;
		}
		return super.verifySpaceOrEndComment();
	}

}
