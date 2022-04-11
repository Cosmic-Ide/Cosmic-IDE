/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *		IBM Corporation - initial API and implementation
 *		Stephan Herrmann - Contribution for
 *								bug 401035 - [1.8] A few tests have started failing recently
 *      Jesper Steen Møller - Contributions for
 *                               bug 529552 - [18.3] Add 'var' in completions
 *                               Bug 529556 - [18.3] Add content assist support for 'var' as a type
 *******************************************************************************/
package org.eclipse.jdt.internal.codeassist.complete;

import java.util.ArrayList;
import java.util.Arrays;

/*
 * Parser able to build specific completion parse nodes, given a cursorLocation.
 *
 * Cursor location denotes the position of the last character behind which completion
 * got requested:
 *  -1 means completion at the very beginning of the source
 *	0  means completion behind the first character
 *  n  means completion behind the n-th character
 */

import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.codeassist.impl.AssistParser;
import org.eclipse.jdt.internal.codeassist.impl.Keywords;
import org.eclipse.jdt.internal.codeassist.impl.RestrictedIdentifiers;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.AND_AND_Expression;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.ArrayReference;
import org.eclipse.jdt.internal.compiler.ast.AssertStatement;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.CaseStatement;
import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.CompoundAssignment;
import org.eclipse.jdt.internal.compiler.ast.ConditionalExpression;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.EmptyStatement;
import org.eclipse.jdt.internal.compiler.ast.EqualExpression;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ForStatement;
import org.eclipse.jdt.internal.compiler.ast.IfStatement;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.InstanceOfExpression;
import org.eclipse.jdt.internal.compiler.ast.IntLiteral;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MarkerAnnotation;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ModuleDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ModuleReference;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.NormalAnnotation;
import org.eclipse.jdt.internal.compiler.ast.OR_OR_Expression;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.PrefixExpression;
import org.eclipse.jdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.ReferenceExpression;
import org.eclipse.jdt.internal.compiler.ast.ReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.SuperReference;
import org.eclipse.jdt.internal.compiler.ast.SwitchExpression;
import org.eclipse.jdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.jdt.internal.compiler.ast.SynchronizedStatement;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.ThrowStatement;
import org.eclipse.jdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.jdt.internal.compiler.ast.TryStatement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.jdt.internal.compiler.ast.UnionTypeReference;
import org.eclipse.jdt.internal.compiler.ast.WhileStatement;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.impl.JavaFeature;
import org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.jdt.internal.compiler.parser.JavadocParser;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.parser.RecoveredAnnotation;
import org.eclipse.jdt.internal.compiler.parser.RecoveredBlock;
import org.eclipse.jdt.internal.compiler.parser.RecoveredElement;
import org.eclipse.jdt.internal.compiler.parser.RecoveredField;
import org.eclipse.jdt.internal.compiler.parser.RecoveredInitializer;
import org.eclipse.jdt.internal.compiler.parser.RecoveredLocalVariable;
import org.eclipse.jdt.internal.compiler.parser.RecoveredMethod;
import org.eclipse.jdt.internal.compiler.parser.RecoveredModule;
import org.eclipse.jdt.internal.compiler.parser.RecoveredPackageVisibilityStatement;
import org.eclipse.jdt.internal.compiler.parser.RecoveredProvidesStatement;
import org.eclipse.jdt.internal.compiler.parser.RecoveredType;
import org.eclipse.jdt.internal.compiler.parser.RecoveredUnit;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObjectToInt;
import org.eclipse.jdt.internal.compiler.util.Util;

public class CompletionParser extends AssistParser {
	// OWNER
	protected static final int COMPLETION_PARSER = 1024;
	protected static final int COMPLETION_OR_ASSIST_PARSER = ASSIST_PARSER + COMPLETION_PARSER;

	// KIND : all values known by CompletionParser are between 1025 and 1549
	protected static final int K_BLOCK_DELIMITER = COMPLETION_PARSER + 1; // whether we are inside a block
	protected static final int K_SELECTOR_INVOCATION_TYPE = COMPLETION_PARSER + 2; // whether we are inside a message send
	protected static final int K_SELECTOR_QUALIFIER = COMPLETION_PARSER + 3; // whether we are inside a message send
	protected static final int K_BETWEEN_CATCH_AND_RIGHT_PAREN = COMPLETION_PARSER + 4; // whether we are between the keyword 'catch' and the following ')'
	protected static final int K_NEXT_TYPEREF_IS_CLASS = COMPLETION_PARSER + 5; // whether the next type reference is a class
	protected static final int K_NEXT_TYPEREF_IS_INTERFACE = COMPLETION_PARSER + 6; // whether the next type reference is an interface
	protected static final int K_NEXT_TYPEREF_IS_EXCEPTION = COMPLETION_PARSER + 7; // whether the next type reference is an exception
	protected static final int K_BETWEEN_NEW_AND_LEFT_BRACKET = COMPLETION_PARSER + 8; // whether we are between the keyword 'new' and the following left braket, i.e. '[', '(' or '{'
	protected static final int K_INSIDE_THROW_STATEMENT = COMPLETION_PARSER + 9; // whether we are between the keyword 'throw' and the end of a throw statement
	protected static final int K_INSIDE_RETURN_STATEMENT = COMPLETION_PARSER + 10; // whether we are between the keyword 'return' and the end of a return statement
	protected static final int K_CAST_STATEMENT = COMPLETION_PARSER + 11; // whether we are between ')' and the end of a cast statement
	protected static final int K_LOCAL_INITIALIZER_DELIMITER = COMPLETION_PARSER + 12;
	protected static final int K_ARRAY_INITIALIZER = COMPLETION_PARSER + 13;
	protected static final int K_ARRAY_CREATION = COMPLETION_PARSER + 14;
	protected static final int K_UNARY_OPERATOR = COMPLETION_PARSER + 15;
	protected static final int K_BINARY_OPERATOR = COMPLETION_PARSER + 16;
	protected static final int K_ASSISGNMENT_OPERATOR = COMPLETION_PARSER + 17;
	protected static final int K_CONDITIONAL_OPERATOR = COMPLETION_PARSER + 18;
	protected static final int K_BETWEEN_IF_AND_RIGHT_PAREN = COMPLETION_PARSER + 19;
	protected static final int K_BETWEEN_WHILE_AND_RIGHT_PAREN = COMPLETION_PARSER + 20;
	protected static final int K_BETWEEN_FOR_AND_RIGHT_PAREN = COMPLETION_PARSER + 21;
	protected static final int K_BETWEEN_SWITCH_AND_RIGHT_PAREN = COMPLETION_PARSER + 22;
	protected static final int K_BETWEEN_SYNCHRONIZED_AND_RIGHT_PAREN = COMPLETION_PARSER + 23;
	protected static final int K_INSIDE_ASSERT_STATEMENT = COMPLETION_PARSER + 24;
	protected static final int K_SWITCH_LABEL= COMPLETION_PARSER + 25;
	protected static final int K_BETWEEN_CASE_AND_COLON = COMPLETION_PARSER + 26;
	protected static final int K_BETWEEN_DEFAULT_AND_COLON = COMPLETION_PARSER + 27;
	protected static final int K_BETWEEN_LEFT_AND_RIGHT_BRACKET = COMPLETION_PARSER + 28;
	protected static final int K_EXTENDS_KEYWORD = COMPLETION_PARSER + 29;
	protected static final int K_PARAMETERIZED_METHOD_INVOCATION = COMPLETION_PARSER + 30;
	protected static final int K_PARAMETERIZED_ALLOCATION = COMPLETION_PARSER + 31;
	protected static final int K_PARAMETERIZED_CAST = COMPLETION_PARSER + 32;
	protected static final int K_BETWEEN_ANNOTATION_NAME_AND_RPAREN = COMPLETION_PARSER + 33;
	protected static final int K_INSIDE_BREAK_STATEMENT = COMPLETION_PARSER + 34;
	protected static final int K_INSIDE_CONTINUE_STATEMENT = COMPLETION_PARSER + 35;
	protected static final int K_LABEL = COMPLETION_PARSER + 36;
	protected static final int K_MEMBER_VALUE_ARRAY_INITIALIZER = COMPLETION_PARSER + 37;
	protected static final int K_CONTROL_STATEMENT_DELIMITER = COMPLETION_PARSER + 38;
	protected static final int K_INSIDE_ASSERT_EXCEPTION = COMPLETION_PARSER + 39;
	protected static final int K_INSIDE_FOR_CONDITIONAL = COMPLETION_PARSER + 40;
	// added for https://bugs.eclipse.org/bugs/show_bug.cgi?id=261534
	protected static final int K_BETWEEN_INSTANCEOF_AND_RPAREN = COMPLETION_PARSER + 41;
	protected static final int K_INSIDE_IMPORT_STATEMENT = COMPLETION_PARSER + 43;
	protected static final int K_INSIDE_EXPORTS_STATEMENT = COMPLETION_PARSER + 44;
	protected static final int K_INSIDE_REQUIRES_STATEMENT = COMPLETION_PARSER + 45;
	protected static final int K_INSIDE_USES_STATEMENT = COMPLETION_PARSER + 46;
	protected static final int K_INSIDE_PROVIDES_STATEMENT = COMPLETION_PARSER + 47;
	protected static final int K_AFTER_PACKAGE_IN_PACKAGE_VISIBILITY_STATEMENT = COMPLETION_PARSER + 48;
	protected static final int K_AFTER_NAME_IN_PROVIDES_STATEMENT = COMPLETION_PARSER + 49;
	protected static final int K_AFTER_WITH_IN_PROVIDES_STATEMENT = COMPLETION_PARSER + 50;
	protected static final int K_INSIDE_OPENS_STATEMENT = COMPLETION_PARSER + 51;
	protected static final int K_YIELD_KEYWORD = COMPLETION_PARSER + 52;


	public final static char[] FAKE_TYPE_NAME = new char[]{' '};
	public final static char[] FAKE_METHOD_NAME = new char[]{' '};
	public final static char[] FAKE_ARGUMENT_NAME = new char[]{' '};
	public final static char[] VALUE = new char[]{'v', 'a', 'l', 'u', 'e'};

	/* public fields */

	public ASTNode assistNodeParent; // the parent node of assist node
	public ASTNode enclosingNode; // an enclosing node used by proposals inference

	/* the following fields are internal flags */

	// block kind
	static final int IF = 1;
	static final int TRY = 2;
	static final int CATCH = 3;
	static final int WHILE = 4;
	static final int SWITCH = 5;
	static final int FOR = 6;
	static final int DO = 7;
	static final int SYNCHRONIZED = 8;

	// label kind
	static final int DEFAULT = 1;

	// invocation type constants
	static final int EXPLICIT_RECEIVER = 0;
	static final int NO_RECEIVER = -1;
	static final int SUPER_RECEIVER = -2;
	static final int NAME_RECEIVER = -3;
	static final int ALLOCATION = -4;
	static final int QUALIFIED_ALLOCATION = -5;

	static final int QUESTION = 1;
	static final int COLON = 2;

	// K_BETWEEN_ANNOTATION_NAME_AND_RPAREN arguments
	static final int LPAREN_NOT_CONSUMED = 1;
	static final int LPAREN_CONSUMED = 2;
	static final int ANNOTATION_NAME_COMPLETION = 4;

	// K_PARAMETERIZED_METHOD_INVOCATION arguments
	static final int INSIDE_NAME = 1;

	// the type of the current invocation (one of the invocation type constants)
	int invocationType;

	// a pointer in the expression stack to the qualifier of a invocation
	int qualifier;

	// used to find if there is unused modifiers when building completion inside a method or an initializer
	boolean hasUnusedModifiers;

	// show if the current token can be an explicit constructor
	int canBeExplicitConstructor = NO;
	static final int NO = 0;
	static final int NEXTTOKEN = 1;
	static final int YES = 2;

	protected static final int LabelStackIncrement = 10;
	char[][] labelStack = new char[LabelStackIncrement][];
	int labelPtr = -1;

	boolean isAlreadyAttached;
	boolean shouldStackAssistNode;

	public boolean record = false;
	public boolean skipRecord = false;
	public int recordFrom;
	public int recordTo;
	public int potentialVariableNamesPtr;
	public char[][] potentialVariableNames;
	public int[] potentialVariableNameStarts;
	public int[] potentialVariableNameEnds;

	CompletionOnAnnotationOfType pendingAnnotation;

	private boolean storeSourceEnds;
	public HashtableOfObjectToInt sourceEnds;
	private boolean inReferenceExpression;
	private IProgressMonitor monitor;
	private int resumeOnSyntaxError = 0;
	private boolean consumedEnhancedFor;

public CompletionParser(ProblemReporter problemReporter, boolean storeExtraSourceEnds) {
	super(problemReporter);
	this.reportSyntaxErrorIsRequired = false;
	this.javadocParser.checkDocComment = true;
	this.annotationRecoveryActivated = false;
	if (storeExtraSourceEnds) {
		this.storeSourceEnds = true;
		this.sourceEnds = new HashtableOfObjectToInt();
	}
}
public CompletionParser(ProblemReporter problemReporter, boolean storeExtraSourceEnds, IProgressMonitor monitor) {
	this(problemReporter, storeExtraSourceEnds);
	this.monitor = monitor;
}
private void addPotentialName(char[] potentialVariableName, int start, int end) {
	int length = this.potentialVariableNames.length;
	if (this.potentialVariableNamesPtr >= length - 1) {
		System.arraycopy(
				this.potentialVariableNames,
				0,
				this.potentialVariableNames = new char[length * 2][],
				0,
				length);
		System.arraycopy(
				this.potentialVariableNameStarts,
				0,
				this.potentialVariableNameStarts = new int[length * 2],
				0,
				length);
		System.arraycopy(
				this.potentialVariableNameEnds,
				0,
				this.potentialVariableNameEnds = new int[length * 2],
				0,
				length);
	}
	this.potentialVariableNames[++this.potentialVariableNamesPtr] = potentialVariableName;
	this.potentialVariableNameStarts[this.potentialVariableNamesPtr] = start;
	this.potentialVariableNameEnds[this.potentialVariableNamesPtr] = end;
}
public void startRecordingIdentifiers(int from, int to) {
	this.record = true;
	this.skipRecord = false;
	this.recordFrom = from;
	this.recordTo = to;

	this.potentialVariableNamesPtr = -1;
	this.potentialVariableNames = new char[10][];
	this.potentialVariableNameStarts = new int[10];
	this.potentialVariableNameEnds = new int[10];
}
public void stopRecordingIdentifiers() {
	this.record = true;
	this.skipRecord = false;
}
@Override
public char[] assistIdentifier(){
	return ((CompletionScanner)this.scanner).completionIdentifier;
}
@Override
protected ASTNode assistNodeParent() {
	return this.assistNodeParent;
}

@Override
protected void detectAssistNodeParent(ASTNode node) {
	if (this.assistNode == null || this.assistNodeParent != null)
		return;
	CompletionNodeDetector detector = new CompletionNodeDetector(this.assistNode, node);
	if (detector.containsCompletionNode()) {
		this.assistNodeParent = detector.getCompletionNodeParent();
		if (this.assistNodeParent == null && node instanceof Statement && node != this.assistNode)
			this.assistNodeParent = node;
	}
}
protected void attachOrphanCompletionNode(){
	if(this.assistNode == null || this.isAlreadyAttached) return;

	this.isAlreadyAttached = true;

	if (this.isOrphanCompletionNode) {
		ASTNode orphan = this.assistNode;
		this.isOrphanCompletionNode = false;

		if (this.currentElement instanceof RecoveredUnit){
			if (orphan instanceof ImportReference){
				this.currentElement.add((ImportReference)orphan, 0);
			} else if (orphan instanceof ModuleDeclaration) {
				this.currentElement.add((ModuleDeclaration)orphan, 0);
			}
		} else if (this.currentElement instanceof RecoveredType){	/* if in context of a type, then persists the identifier into a fake field return type */
			RecoveredType recoveredType = (RecoveredType)this.currentElement;
			/* filter out cases where scanner is still inside type header */
			if (recoveredType.foundOpeningBrace) {
				/* generate a pseudo field with a completion on type reference */
				if (orphan instanceof TypeReference){
					if (isInsideModuleInfo()) return; //taken care elsewhere

					TypeReference fieldType;

					int kind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
					int info = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER);
					if(kind == K_BINARY_OPERATOR && info == LESS && this.identifierPtr > -1) {
						if(this.genericsLengthStack[this.genericsLengthPtr] > 0) {
							consumeTypeArguments();
						}
						pushOnGenericsStack(orphan);
						consumeTypeArguments();
						fieldType = getTypeReference(0);
						this.assistNodeParent = fieldType;
					} else {
						fieldType = (TypeReference)orphan;
					}

					CompletionOnFieldType fieldDeclaration = new CompletionOnFieldType(fieldType, false);

					// retrieve annotations if any
					int length;
					if ((length = this.expressionLengthStack[this.expressionLengthPtr]) != 0 &&
							this.expressionStack[this.expressionPtr] instanceof Annotation) {
						System.arraycopy(
							this.expressionStack,
							this.expressionPtr - length + 1,
							fieldDeclaration.annotations = new Annotation[length],
							0,
							length);
					}

					// retrieve available modifiers if any
					if (this.intPtr >= 2 && this.intStack[this.intPtr-1] == this.lastModifiersStart && this.intStack[this.intPtr-2] == this.lastModifiers){
						fieldDeclaration.modifiersSourceStart = this.intStack[this.intPtr-1];
						fieldDeclaration.modifiers = this.intStack[this.intPtr-2];
					}

					this.currentElement = this.currentElement.add(fieldDeclaration, 0);
					return;
				}
			}
		}
		/* if in context of a method, persists if inside arguments as a type */
		if (this.currentElement instanceof RecoveredMethod){
			RecoveredMethod recoveredMethod = (RecoveredMethod)this.currentElement;
			/* only consider if inside method header */
			if (!recoveredMethod.foundOpeningBrace) {
				//if (rParenPos < lParenPos){ // inside arguments
				if (orphan instanceof TypeReference){
					this.currentElement = this.currentElement.parent.add(
						new CompletionOnFieldType((TypeReference)orphan, true), 0);
					return;
				}

				if(orphan instanceof Annotation) {
					CompletionOnAnnotationOfType fakeType =
						new CompletionOnAnnotationOfType(
								FAKE_TYPE_NAME,
								this.compilationUnit.compilationResult(),
								(Annotation)orphan);
					fakeType.isParameter = true;
					this.currentElement.parent.add(fakeType, 0);
					this.pendingAnnotation = fakeType;
					return;
				}
			}
		}

		if(orphan instanceof MemberValuePair) {
			buildMoreAnnotationCompletionContext((MemberValuePair) orphan);
			return;
		}

		if(orphan instanceof Annotation) {
			popUntilCompletedAnnotationIfNecessary();

			CompletionOnAnnotationOfType fakeType =
				new CompletionOnAnnotationOfType(
						FAKE_TYPE_NAME,
						this.compilationUnit.compilationResult(),
						(Annotation)orphan);
			this.currentElement.add(fakeType, 0);

			if (!isInsideAnnotation()) {
				this.pendingAnnotation = fakeType;
			}

			return;
		}

		if ((topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BETWEEN_CATCH_AND_RIGHT_PAREN)) {
			if (this.assistNode instanceof CompletionOnSingleTypeReference &&
					((CompletionOnSingleTypeReference)this.assistNode).isException()) {
				buildMoreTryStatementCompletionContext((TypeReference)this.assistNode);
				return;
			} else if (this.assistNode instanceof CompletionOnQualifiedTypeReference &&
					((CompletionOnQualifiedTypeReference)this.assistNode).isException()) {
				buildMoreTryStatementCompletionContext((TypeReference)this.assistNode);
				return;
			} else if (this.assistNode instanceof CompletionOnParameterizedQualifiedTypeReference &&
					((CompletionOnParameterizedQualifiedTypeReference)this.assistNode).isException()) {
				buildMoreTryStatementCompletionContext((TypeReference)this.assistNode);
				return;
			}
		}

		// add the completion node to the method declaration or constructor declaration
		if (orphan instanceof Statement) {
			/* check for completion at the beginning of method body
				behind an invalid signature
			 */
			RecoveredMethod method = this.currentElement.enclosingMethod();
			if (method != null){
				AbstractMethodDeclaration methodDecl = method.methodDeclaration;
				if ((methodDecl.bodyStart == methodDecl.sourceEnd+1) // was missing opening brace
					&& (Util.getLineNumber(orphan.sourceStart, this.scanner.lineEnds, 0, this.scanner.linePtr)
							== Util.getLineNumber(methodDecl.sourceEnd, this.scanner.lineEnds, 0, this.scanner.linePtr))){
					return;
				}
			}
			// add the completion node as a statement to the list of block statements
			this.currentElement = this.currentElement.add((Statement)orphan, 0);
			return;
		}
	}

	if (isInsideAnnotation()) {
		// push top expression on ast stack if it contains the completion node
		Expression expression;
		if (this.expressionPtr > -1) {
			expression = this.expressionStack[this.expressionPtr];
			if(expression == this.assistNode) {
				if (this.topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_MEMBER_VALUE_ARRAY_INITIALIZER ) {
					ArrayInitializer arrayInitializer = new ArrayInitializer();
					arrayInitializer.expressions = new Expression[]{expression};
					char[] memberValueName = VALUE;
					if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER, 1) == K_ATTRIBUTE_VALUE_DELIMITER) {
						if (this.identifierLengthPtr > 0) {
							memberValueName = this.identifierStack[this.identifierPtr];
							int length = this.identifierLengthStack[this.identifierLengthPtr--];
							this.identifierPtr -= length;
						}
					}
					MemberValuePair memberValuePair = new MemberValuePair(memberValueName, expression.sourceStart,
							expression.sourceEnd, arrayInitializer);
					// The following if-statement is the result of inlining a call of buildMoreAnnotationCompletionContext
					// that was previously here. It might not be needed.
					if (this.astLengthPtr > -1) {
						this.astLengthPtr--;
					}
					TypeReference typeReference = getAnnotationType();

					NormalAnnotation annotation = new NormalAnnotation(typeReference, this.intStack[this.intPtr--]);
					annotation.memberValuePairs = new MemberValuePair[] { memberValuePair };

					CompletionOnAnnotationOfType fakeType = new CompletionOnAnnotationOfType(FAKE_TYPE_NAME,
							this.compilationUnit.compilationResult(), annotation);

					this.currentElement.add(fakeType, 0);
					this.pendingAnnotation = fakeType;
					this.assistNodeParent = new AssistNodeParentAnnotationArrayInitializer(typeReference, memberValueName);
				} else if(this.topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BETWEEN_ANNOTATION_NAME_AND_RPAREN) {
					if (expression instanceof SingleNameReference) {
						SingleNameReference nameReference = (SingleNameReference) expression;
						CompletionOnMemberValueName memberValueName = new CompletionOnMemberValueName(nameReference.token, nameReference.sourceStart, nameReference.sourceEnd);

						buildMoreAnnotationCompletionContext(memberValueName);
						return;
					} else if (expression instanceof QualifiedNameReference || expression instanceof StringLiteral) {
						MemberValuePair valuePair =
							new MemberValuePair(VALUE, expression.sourceStart, expression.sourceEnd, expression);
						buildMoreAnnotationCompletionContext(valuePair);
					}
				} else {
					int index;
					if((index = lastIndexOfElement(K_ATTRIBUTE_VALUE_DELIMITER)) != -1) {
						int attributeIndentifierPtr = this.elementInfoStack[index];
						int identLengthPtr = this.identifierLengthPtr;
						int identPtr = this.identifierPtr;
						while (attributeIndentifierPtr < identPtr) {
							identPtr -= this.identifierLengthStack[identLengthPtr--];
						}

						if(attributeIndentifierPtr != identPtr) return;

						this.identifierLengthPtr = identLengthPtr;
						this.identifierPtr = identPtr;

						this.identifierLengthPtr--;
						MemberValuePair memberValuePair = new MemberValuePair(
								this.identifierStack[this.identifierPtr--],
								expression.sourceStart,
								expression.sourceEnd,
								expression);

						buildMoreAnnotationCompletionContext(memberValuePair);
						return;
					}
				}
			} else {
				CompletionNodeDetector detector =  new CompletionNodeDetector(this.assistNode, expression);
				if(detector.containsCompletionNode()) {
					MemberValuePair valuePair =
						new MemberValuePair(VALUE, expression.sourceStart, expression.sourceEnd, expression);
					buildMoreAnnotationCompletionContext(valuePair);
				}
			}
		}

		if (this.astPtr > -1) {
			ASTNode node = this.astStack[this.astPtr];
			if(node instanceof MemberValuePair) {
				MemberValuePair memberValuePair = (MemberValuePair) node;
				CompletionNodeDetector detector =  new CompletionNodeDetector(this.assistNode, memberValuePair);
				if(detector.containsCompletionNode()) {
					buildMoreAnnotationCompletionContext(memberValuePair);
					this.assistNodeParent = detector.getCompletionNodeParent();
					return;
				}
			}
		}
	}

	if(this.genericsPtr > -1) {
		ASTNode node = this.genericsStack[this.genericsPtr];
		if(node instanceof Wildcard && ((Wildcard)node).bound == this.assistNode){
			int kind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
			if (kind == K_BINARY_OPERATOR) {
				int info = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER);
				if (info == LESS) {
					buildMoreGenericsCompletionContext(node, true);
					return;
				}
			}
			if(this.identifierLengthPtr > -1 && this.identifierLengthStack[this.identifierLengthPtr]!= 0) {
				this.pushOnElementStack(K_BINARY_OPERATOR, LESS);
				buildMoreGenericsCompletionContext(node, false);
				return;
			}
		}
	}

	if(this.currentElement instanceof RecoveredType || this.currentElement instanceof RecoveredMethod) {
		if(this.currentElement instanceof RecoveredType) {
			RecoveredType recoveredType = (RecoveredType)this.currentElement;
			if(recoveredType.foundOpeningBrace && this.genericsPtr > -1) {
				if(this.genericsStack[this.genericsPtr] instanceof TypeParameter) {
					TypeParameter typeParameter = (TypeParameter) this.genericsStack[this.genericsPtr];
					CompletionNodeDetector detector =  new CompletionNodeDetector(this.assistNode, typeParameter);
					if(detector.containsCompletionNode()) {
						this.currentElement.add(new CompletionOnMethodTypeParameter(new TypeParameter[]{typeParameter},this.compilationUnit.compilationResult()), 0);
					}
					return;
				}
			}
		}

		if ((!isInsideMethod() && !isInsideFieldInitialization())) {
			if(this.genericsPtr > -1 && this.genericsLengthPtr > -1 && this.genericsIdentifiersLengthPtr > -1) {
				int kind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
				int info = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER);
				if(kind == K_BINARY_OPERATOR && info == LESS) {
					consumeTypeArguments();
				}
				int numberOfIdentifiers = this.genericsIdentifiersLengthStack[this.genericsIdentifiersLengthPtr];
				int genPtr = this.genericsPtr;
				done : for(int i = 0; i <= this.identifierLengthPtr && numberOfIdentifiers > 0; i++){
					int identifierLength = this.identifierLengthStack[this.identifierLengthPtr - i];
					int length = this.genericsLengthStack[this.genericsLengthPtr - i];
					for(int j = 0; j < length; j++) {
						ASTNode node = this.genericsStack[genPtr - j];
						CompletionNodeDetector detector = new CompletionNodeDetector(this.assistNode, node);
						if(detector.containsCompletionNode()) {
							if(node == this.assistNode){
								if(this.identifierLengthPtr > -1 &&	this.identifierLengthStack[this.identifierLengthPtr]!= 0) {
									TypeReference ref = this.getTypeReference(0);
									this.assistNodeParent = ref;
								}
							} else {
								this.assistNodeParent = detector.getCompletionNodeParent();
							}
							break done;
						}
					}
					genPtr -= length;
					numberOfIdentifiers -= identifierLength;
				}
				if(this.assistNodeParent != null && this.assistNodeParent instanceof TypeReference) {
					if(this.currentElement instanceof RecoveredType) {
						this.currentElement = this.currentElement.add(new CompletionOnFieldType((TypeReference)this.assistNodeParent, false), 0);
					} else {
						this.currentElement = this.currentElement.add((TypeReference)this.assistNodeParent, 0);
					}
				}
			}
		}
	}

	// the following code applies only in methods, constructors or initializers
	if ((!isInsideMethod() && !isInsideFieldInitialization() && !isInsideAttributeValue())) {
		return;
	}
	if(this.assistNodeParent instanceof AssistNodeParentAnnotationArrayInitializer) {
		return;
	}

	if(this.genericsPtr > -1) {
		ASTNode node = this.genericsStack[this.genericsPtr];
		CompletionNodeDetector detector = new CompletionNodeDetector(this.assistNode, node);
		if(detector.containsCompletionNode()) {
			/* check for completion at the beginning of method body
				behind an invalid signature
			 */
			RecoveredMethod method = this.currentElement.enclosingMethod();
			if (method != null){
				AbstractMethodDeclaration methodDecl = method.methodDeclaration;
				if ((methodDecl.bodyStart == methodDecl.sourceEnd+1) // was missing opening brace
					&& (Util.getLineNumber(node.sourceStart, this.scanner.lineEnds, 0, this.scanner.linePtr)
						== Util.getLineNumber(methodDecl.sourceEnd, this.scanner.lineEnds, 0, this.scanner.linePtr))){
					return;
				}
			}
			if(node == this.assistNode){
				buildMoreGenericsCompletionContext(node, true);
			}
		}
	}

	// push top expression on ast stack if it contains the completion node
	Expression expression;
	if (this.expressionPtr > -1) {
		expression = this.expressionStack[this.expressionPtr];
		CompletionNodeDetector detector = new CompletionNodeDetector(this.assistNode, expression);
		if(detector.containsCompletionNode()) {
			/* check for completion at the beginning of method body
				behind an invalid signature
			 */
			RecoveredMethod method = this.currentElement.enclosingMethod();
			if (method != null){
				AbstractMethodDeclaration methodDecl = method.methodDeclaration;
				if ((methodDecl.bodyStart == methodDecl.sourceEnd+1) // was missing opening brace
					&& (Util.getLineNumber(expression.sourceStart, this.scanner.lineEnds, 0, this.scanner.linePtr)
						== Util.getLineNumber(methodDecl.sourceEnd, this.scanner.lineEnds, 0, this.scanner.linePtr))){
					return;
				}
			}
			if(expression == this.assistNode
				|| (expression instanceof Assignment	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=287939
					&& ((Assignment)expression).expression == this.assistNode
					&& ((this.expressionPtr > 0 && stackHasInstanceOfExpression(this.expressionStack, this.expressionPtr - 1))
							// In case of error in compilation unit, expression stack might not have instanceof exp, so try elementObjectInfoStack
						|| (this.elementPtr >= 0 && stackHasInstanceOfExpression(this.elementObjectInfoStack, this.elementPtr))))
				|| (expression instanceof AllocationExpression
					&& ((AllocationExpression)expression).type == this.assistNode)
				|| (expression instanceof AND_AND_Expression
						&& (this.elementPtr >= 0 && this.elementObjectInfoStack[this.elementPtr] instanceof InstanceOfExpression))
				|| (expression instanceof ConditionalExpression
						  && ((ConditionalExpression) expression).valueIfFalse == this.assistNode)){
				buildMoreCompletionContext(expression);
				if (this.assistNodeParent == null
					&& expression instanceof Assignment) {
					this.assistNodeParent = detector.getCompletionNodeParent();
				}
				return;
			} else {
				this.assistNodeParent = detector.getCompletionNodeParent();
				Expression outerExpression = detector.getCompletionNodeOuterExpression();
				if (outerExpression != null) {
					expression = outerExpression;
				}
				this.currentElement = this.currentElement.add(expression, 0);
				return;
			}
		}
	}
	if (this.astPtr > -1 && this.astStack[this.astPtr] instanceof LocalDeclaration) { // https://bugs.eclipse.org/bugs/show_bug.cgi?id=287939
		// To take care of:  if (a instance of X)  int i = a.|
		LocalDeclaration local = (LocalDeclaration) this.astStack[this.astPtr];
		if (local.initialization == this.assistNode) {
			Statement enclosing = buildMoreCompletionEnclosingContext(local);
			if (enclosing instanceof IfStatement) {
				if (this.currentElement instanceof RecoveredBlock) {
					// RecoveredLocalVariable must be removed from its parent because the IfStatement will be added instead
					RecoveredBlock recoveredBlock = (RecoveredBlock) this.currentElement;
					recoveredBlock.statements[--recoveredBlock.statementCount] = null;
					this.currentElement = this.currentElement.add(enclosing, 0);
				}
			}
		}
	}
}

private static class SavedState {
	final ASTNode assistNodeParent;
	final int parserCursorLocation;
	final int scannerCursorLocation;

	public SavedState(int parserCursorLocation, int scannerCursorLocation, ASTNode assistNodeParent) {
		this.parserCursorLocation = parserCursorLocation;
		this.scannerCursorLocation = scannerCursorLocation;
		this.assistNodeParent = assistNodeParent;
	}
}
@Override
public Object becomeSimpleParser() {
	CompletionScanner completionScanner = (CompletionScanner)this.scanner;
	SavedState parserState = new SavedState(this.cursorLocation, completionScanner.cursorLocation, this.assistNodeParent);

	this.cursorLocation = Integer.MAX_VALUE;
	completionScanner.cursorLocation = Integer.MAX_VALUE;

	return parserState;
}
private void buildMoreAnnotationCompletionContext(MemberValuePair memberValuePair) {
	if(this.identifierPtr < 0 || this.identifierLengthPtr < 0 ) return;

	TypeReference typeReference = getAnnotationType();

	int nodesToRemove = this.astPtr > -1 && this.astStack[this.astPtr] == memberValuePair ? 1 : 0;

	NormalAnnotation annotation;
	if (memberValuePair instanceof CompletionOnMemberValueName) {
		MemberValuePair[] memberValuePairs = null;
		int length;
		if (this.astLengthPtr > -1 && (length = this.astLengthStack[this.astLengthPtr--]) > nodesToRemove) {
			if (this.astStack[this.astPtr] instanceof MemberValuePair) {
				System.arraycopy(
					this.astStack,
					(this.astPtr -= length) + 1,
					memberValuePairs = new MemberValuePair[length - nodesToRemove],
					0,
					length - nodesToRemove);
			}
		}
		annotation =
			new CompletionOnAnnotationMemberValuePair(
					typeReference,
					this.intStack[this.intPtr--],
					memberValuePairs,
					memberValuePair);

		this.assistNode = memberValuePair;
		this.assistNodeParent = annotation;

		if (memberValuePair.sourceEnd >= this.lastCheckPoint) {
			this.lastCheckPoint = memberValuePair.sourceEnd + 1;
		}
	} else {
		MemberValuePair[] memberValuePairs = null;
		int length = 0;
		if (this.astLengthPtr > -1 && (length = this.astLengthStack[this.astLengthPtr--]) > nodesToRemove) {
			if (this.astStack[this.astPtr] instanceof MemberValuePair) {
				System.arraycopy(
					this.astStack,
					(this.astPtr -= length) + 1,
					memberValuePairs = new MemberValuePair[length - nodesToRemove + 1],
					0,
					length - nodesToRemove);
			}
			if(memberValuePairs != null) {
				memberValuePairs[length - nodesToRemove] = memberValuePair;
			} else {
				memberValuePairs = new MemberValuePair[]{memberValuePair};
			}
		} else {
			memberValuePairs = new MemberValuePair[]{memberValuePair};
		}

		annotation =
			new NormalAnnotation(
					typeReference,
					this.intStack[this.intPtr--]);
		annotation.memberValuePairs = memberValuePairs;
		this.assistNodeParent = annotation;

	}
	CompletionOnAnnotationOfType fakeType =
		new CompletionOnAnnotationOfType(
				FAKE_TYPE_NAME,
				this.compilationUnit.compilationResult(),
				annotation);

	this.currentElement.add(fakeType, 0);
	this.pendingAnnotation = fakeType;
}
private void buildMoreCompletionContext(Expression expression) {
	Statement statement = expression;
	int kind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
	if(kind != 0) {
		int info = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER);
		nextElement : switch (kind) {
			case K_SELECTOR_QUALIFIER :
				int selector = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER, 2);
				if(selector == THIS_CONSTRUCTOR || selector == SUPER_CONSTRUCTOR) {
					ExplicitConstructorCall call = new ExplicitConstructorCall(
						(selector == THIS_CONSTRUCTOR) ?
							ExplicitConstructorCall.This :
							ExplicitConstructorCall.Super
					);
					call.arguments = new Expression[] {expression};
					call.sourceStart = expression.sourceStart;
					call.sourceEnd = expression.sourceEnd;
					this.assistNodeParent = call;
				} else {
					int invocType = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER,1);
					int qualifierExprPtr = info;

					// find arguments
					int length = this.expressionLengthStack[this.expressionLengthPtr];

					// search previous arguments if missing
					if(this.expressionPtr > 0 && this.expressionLengthPtr > 0 && length == 1) {
						int start = (int) (this.identifierPositionStack[selector] >>> 32);
						if(this.expressionStack[this.expressionPtr-1] != null && this.expressionStack[this.expressionPtr-1].sourceStart > start) {
							length += this.expressionLengthStack[this.expressionLengthPtr-1];
						}

					}

					Expression[] arguments = null;
					if (length != 0) {
						arguments = new Expression[length];
						this.expressionPtr -= length;
						System.arraycopy(this.expressionStack, this.expressionPtr + 1, arguments, 0, length-1);
						arguments[length-1] = expression;
					}

					if(invocType != ALLOCATION && invocType != QUALIFIED_ALLOCATION) {
						MessageSend messageSend = new MessageSend();
						messageSend.selector = this.identifierStack[selector];
						messageSend.arguments = arguments;

						// find receiver
						switch (invocType) {
							case NO_RECEIVER:
								messageSend.receiver = ThisReference.implicitThis();
								break;
							case NAME_RECEIVER:
								// remove special flags for primitive types
								while (this.identifierLengthPtr >= 0 && this.identifierLengthStack[this.identifierLengthPtr] < 0) {
									this.identifierLengthPtr--;
								}

								// remove selector
								this.identifierPtr--;
								if(this.genericsPtr > -1 && this.genericsLengthPtr > -1 && this.genericsLengthStack[this.genericsLengthPtr] > 0) {
									// is inside a paremeterized method: bar.<X>.foo
									this.identifierLengthPtr--;
								} else {
									this.identifierLengthStack[this.identifierLengthPtr]--;
									length = this.typeAnnotationLengthStack[this.typeAnnotationLengthPtr--];
									Annotation [] typeAnnotations;
									if (length != 0) {
										System.arraycopy(
												this.typeAnnotationStack,
												(this.typeAnnotationPtr -= length) + 1,
												typeAnnotations = new Annotation[length],
												0,
												length);
										problemReporter().misplacedTypeAnnotations(typeAnnotations[0], typeAnnotations[typeAnnotations.length - 1]);
									}
								}
								// consume the receiver
								int identifierLength = this.identifierLengthStack[this.identifierLengthPtr];
								if(this.identifierPtr > -1 && identifierLength > 0 && this.identifierPtr + 1 >= identifierLength) {
									messageSend.receiver = getUnspecifiedReference();
								} else {
									messageSend = null;
								}
								break;
							case SUPER_RECEIVER:
								messageSend.receiver = new SuperReference(0, 0);
								break;
							case EXPLICIT_RECEIVER:
								messageSend.receiver = this.expressionStack[qualifierExprPtr];
								break;
							default :
								messageSend.receiver = ThisReference.implicitThis();
								break;
						}
						this.assistNodeParent = messageSend;
					} else {
						if(invocType == ALLOCATION) {
							AllocationExpression allocationExpr = new AllocationExpression();
							allocationExpr.arguments = arguments;
							pushOnGenericsIdentifiersLengthStack(this.identifierLengthStack[this.identifierLengthPtr]);
							pushOnGenericsLengthStack(0);
							allocationExpr.type = getTypeReference(0);
							this.assistNodeParent = allocationExpr;
						} else {
							QualifiedAllocationExpression allocationExpr = new QualifiedAllocationExpression();
							allocationExpr.enclosingInstance = this.expressionStack[qualifierExprPtr];
							allocationExpr.arguments = arguments;
							pushOnGenericsIdentifiersLengthStack(this.identifierLengthStack[this.identifierLengthPtr]);
							pushOnGenericsLengthStack(0);

							allocationExpr.type = getTypeReference(0);
							this.assistNodeParent = allocationExpr;
						}
					}
				}
				break nextElement;
			case K_INSIDE_RETURN_STATEMENT :
				if(info == this.bracketDepth) {
					ReturnStatement returnStatement = new ReturnStatement(expression, expression.sourceStart, expression.sourceEnd);
					this.assistNodeParent = returnStatement;
				}
				break nextElement;
			case K_CAST_STATEMENT :
				Expression castType;
				if(this.expressionPtr > 0
					&& ((castType = this.expressionStack[this.expressionPtr-1]) instanceof TypeReference)) {
					CastExpression cast = new CastExpression(expression, (TypeReference) castType);
					cast.sourceStart = castType.sourceStart;
					cast.sourceEnd= expression.sourceEnd;
					this.assistNodeParent = cast;
				}
				break nextElement;
			case K_UNARY_OPERATOR :
				if(this.expressionPtr > -1) {
					Expression operatorExpression = null;
					switch (info) {
						case PLUS_PLUS :
							operatorExpression = new PrefixExpression(expression,IntLiteral.One, PLUS, expression.sourceStart);
							break;
						case MINUS_MINUS :
							operatorExpression = new PrefixExpression(expression,IntLiteral.One, MINUS, expression.sourceStart);
							break;
						default :
							operatorExpression = new UnaryExpression(expression, info);
							break;
					}
					this.assistNodeParent = operatorExpression;
				}
				break nextElement;
			case K_BINARY_OPERATOR :
				if(this.expressionPtr > -1) {
					Expression operatorExpression = null;
					Expression left = null;
					if(this.expressionPtr == 0) {
						// it is  a ***_NotName rule
						if(this.identifierPtr > -1) {
							left = getUnspecifiedReferenceOptimized();
						}
					} else {
						left = this.expressionStack[this.expressionPtr-1];
						// is it a ***_NotName rule ?
						if(this.identifierPtr > -1) {
							int start = (int) (this.identifierPositionStack[this.identifierPtr] >>> 32);
							if(left.sourceStart < start) {
								left = getUnspecifiedReferenceOptimized();
							}
						}
					}

					if(left != null) {
						switch (info) {
							case AND_AND :
								operatorExpression = new AND_AND_Expression(left, expression, info);
								break;
							case OR_OR :
								operatorExpression = new OR_OR_Expression(left, expression, info);
								break;
							case EQUAL_EQUAL :
							case NOT_EQUAL :
								operatorExpression = new EqualExpression(left, expression, info);
								break;
							default :
								operatorExpression = new BinaryExpression(left, expression, info);
								break;
						}
					}
					if(operatorExpression != null) {
						this.assistNodeParent = operatorExpression;
					}
				}
				break nextElement;
			case K_ARRAY_INITIALIZER :
				ArrayInitializer arrayInitializer = new ArrayInitializer();
				arrayInitializer.expressions = new Expression[]{expression};
				this.expressionPtr -= this.expressionLengthStack[this.expressionLengthPtr--];

				if(this.expressionLengthPtr > -1
					&& this.expressionPtr > -1
					&& this.expressionStack[this.expressionPtr] != null
					&& this.expressionStack[this.expressionPtr].sourceStart > info) {
					this.expressionLengthPtr--;
				}

				if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER, 1) == K_ARRAY_CREATION) {
					ArrayAllocationExpression allocationExpression = new ArrayAllocationExpression();
					pushOnGenericsLengthStack(0);
					pushOnGenericsIdentifiersLengthStack(this.identifierLengthStack[this.identifierLengthPtr]);
					allocationExpression.type = getTypeReference(0);
					allocationExpression.type.bits |= ASTNode.IgnoreRawTypeCheck; // no need to worry about raw type usage
					int length = this.expressionLengthStack[this.expressionLengthPtr];
					allocationExpression.dimensions = new Expression[length];

					allocationExpression.initializer = arrayInitializer;
					this.assistNodeParent = allocationExpression;
				} else if(this.currentElement instanceof RecoveredField && !(this.currentElement instanceof RecoveredInitializer)) {
					RecoveredField recoveredField = (RecoveredField) this.currentElement;
					if(recoveredField.fieldDeclaration.type.dimensions() == 0) {
						Block block = new Block(0);
						block.sourceStart = info;
						this.currentElement = this.currentElement.add(block, 1);
					} else {
						statement = arrayInitializer;
					}
				} else if(this.currentElement instanceof RecoveredLocalVariable) {
					RecoveredLocalVariable recoveredLocalVariable = (RecoveredLocalVariable) this.currentElement;
					if(recoveredLocalVariable.localDeclaration.type.dimensions() == 0) {
						Block block = new Block(0);
						block.sourceStart = info;
						this.currentElement = this.currentElement.add(block, 1);
					} else {
						statement = arrayInitializer;
					}
				} else {
					statement = arrayInitializer;
				}
				break nextElement;
			case K_ARRAY_CREATION :
				ArrayAllocationExpression allocationExpression = new ArrayAllocationExpression();
				allocationExpression.type = getTypeReference(0);
				allocationExpression.dimensions = new Expression[]{expression};

				this.assistNodeParent = allocationExpression;
				break nextElement;
			case K_ASSISGNMENT_OPERATOR :
				if(this.expressionPtr > 0 && this.expressionStack[this.expressionPtr - 1] != null) {
					Assignment assignment;
					if(info == EQUAL) {
						assignment = new Assignment(
							this.expressionStack[this.expressionPtr - 1],
							expression,
							expression.sourceEnd
						);
					} else {
						assignment = new CompoundAssignment(
							this.expressionStack[this.expressionPtr - 1],
							expression,
							info,
							expression.sourceEnd
						);
					}
					this.assistNodeParent = assignment;
				}
				break nextElement;
			case K_CONDITIONAL_OPERATOR :
				if(info == QUESTION) {
					if(this.expressionPtr > 0) {
						this.expressionPtr--;
						this.expressionLengthPtr--;
						this.expressionStack[this.expressionPtr] = this.expressionStack[this.expressionPtr+1];
						popElement(K_CONDITIONAL_OPERATOR);
						buildMoreCompletionContext(expression);
						return;
					}
				} else {
					if(this.expressionPtr > 1) {
						this.expressionPtr = this.expressionPtr - 2;
						this.expressionLengthPtr = this.expressionLengthPtr - 2;
						this.expressionStack[this.expressionPtr] = this.expressionStack[this.expressionPtr+2];
						popElement(K_CONDITIONAL_OPERATOR);
						buildMoreCompletionContext(expression);
						return;
					}
				}
				break nextElement;
			case K_BETWEEN_LEFT_AND_RIGHT_BRACKET :
				ArrayReference arrayReference;
				if(this.identifierPtr < 0 && this.expressionPtr > 0 && this.expressionStack[this.expressionPtr] == expression) {
					arrayReference =
						new ArrayReference(
							this.expressionStack[this.expressionPtr-1],
							expression);
				} else {
					arrayReference =
						new ArrayReference(
							getUnspecifiedReferenceOptimized(),
							expression);
				}
				this.assistNodeParent = arrayReference;
				break;
			case K_BETWEEN_CASE_AND_COLON :
				if(this.expressionPtr > 0) {
					SwitchStatement switchStatement = new SwitchStatement();
					switchStatement.expression = this.expressionStack[this.expressionPtr - 1];
					if(this.astLengthPtr > -1 && this.astPtr > -1) {
						int length = this.astLengthStack[this.astLengthPtr];
						int newAstPtr = this.astPtr - length;
						ASTNode firstNode = this.astStack[newAstPtr + 1];
						if(length != 0 && firstNode.sourceStart > switchStatement.expression.sourceEnd) {
							switchStatement.statements = new Statement[length + 1];
							System.arraycopy(
								this.astStack,
								newAstPtr + 1,
								switchStatement.statements,
								0,
								length);
						}
					}
					CaseStatement caseStatement = new CaseStatement(expression, expression.sourceStart, expression.sourceEnd);
					if(switchStatement.statements == null) {
						switchStatement.statements = new Statement[]{caseStatement};
					} else {
						switchStatement.statements[switchStatement.statements.length - 1] = caseStatement;
					}
					this.assistNodeParent = switchStatement;
				}
				break;
			case K_BETWEEN_IF_AND_RIGHT_PAREN :
				IfStatement ifStatement = new IfStatement(expression, new EmptyStatement(expression.sourceEnd, expression.sourceEnd), expression.sourceStart, expression.sourceEnd);
				this.assistNodeParent = ifStatement;
				break nextElement;
			case K_BETWEEN_WHILE_AND_RIGHT_PAREN :
				WhileStatement whileStatement = new WhileStatement(expression, new EmptyStatement(expression.sourceEnd, expression.sourceEnd), expression.sourceStart, expression.sourceEnd);
				this.assistNodeParent = whileStatement;
				break nextElement;
			case K_INSIDE_FOR_CONDITIONAL: // https://bugs.eclipse.org/bugs/show_bug.cgi?id=253008
				ForStatement forStatement = new ForStatement(new Statement[0], expression, new Statement[0],
															 new EmptyStatement(expression.sourceEnd, expression.sourceEnd),
						                                     false,
						                                     expression.sourceStart, expression.sourceEnd);
				this.assistNodeParent = forStatement;
				break nextElement;
			case K_BETWEEN_SWITCH_AND_RIGHT_PAREN:
				SwitchStatement switchStatement = new SwitchStatement();
				switchStatement.expression = expression;
				switchStatement.statements = new Statement[0];
				this.assistNodeParent = switchStatement;
				break nextElement;
			case K_BETWEEN_SYNCHRONIZED_AND_RIGHT_PAREN :
				SynchronizedStatement synchronizedStatement = new SynchronizedStatement(expression, new Block(0), expression.sourceStart, expression.sourceEnd);
				this.assistNodeParent = synchronizedStatement;
				break nextElement;
			case K_INSIDE_THROW_STATEMENT:
				if(info == this.bracketDepth) {
					ThrowStatement throwStatement = new ThrowStatement(expression, expression.sourceStart, expression.sourceEnd);
					this.assistNodeParent = throwStatement;
				}
				break nextElement;
			case K_INSIDE_ASSERT_STATEMENT:
				if(info == this.bracketDepth) {
					AssertStatement assertStatement = new AssertStatement(expression, expression.sourceStart);
					this.assistNodeParent = assertStatement;
				}
				break nextElement;
			case K_INSIDE_ASSERT_EXCEPTION:
				if(info == this.bracketDepth) {
					AssertStatement assertStatement = new AssertStatement(expression, new TrueLiteral(expression.sourceStart, expression.sourceStart), expression.sourceStart);
					this.assistNodeParent = assertStatement;
				}
				break nextElement;
		}
	}
	if(this.assistNodeParent != null) {
		this.currentElement = this.currentElement.add(buildMoreCompletionEnclosingContext((Statement)this.assistNodeParent), 0);
	} else {
		if(this.currentElement instanceof RecoveredField && !(this.currentElement instanceof RecoveredInitializer)
			&& ((RecoveredField) this.currentElement).fieldDeclaration.initialization == null) {
			if (lastIndexOfElement(K_LAMBDA_EXPRESSION_DELIMITER) <= lastIndexOfElement(K_FIELD_INITIALIZER_DELIMITER))
				this.assistNodeParent = ((RecoveredField) this.currentElement).fieldDeclaration;
			this.currentElement = this.currentElement.add(buildMoreCompletionEnclosingContext(statement), 0);
		} else if(this.currentElement instanceof RecoveredLocalVariable
			&& ((RecoveredLocalVariable) this.currentElement).localDeclaration.initialization == null) {

			this.assistNodeParent = ((RecoveredLocalVariable) this.currentElement).localDeclaration;
			this.currentElement = this.currentElement.add(buildMoreCompletionEnclosingContext(statement), 0);
		} else {
			this.currentElement = this.currentElement.add(buildMoreCompletionEnclosingContext(expression), 0);
		}
	}
}
private Statement buildMoreCompletionEnclosingContext(Statement statement) {
	IfStatement ifStatement = null;
	int index = -1;
	/*
	 * What happens here? When we have an "instanceof" after the last
	 * K_CONTROL_STATEMENT_DELIMITER (which represents if or else-if), the former
	 * is taken to be the current point. Otherwise, the standard rule applies: i.e.
	 * pick the block if it comes after the K_CONTROL_STATEMENT_DELIMITER, otherwise pick the
	 * K_BLOCK_DELIMITER.
	 */
	int blockIndex = lastIndexOfElement(K_BLOCK_DELIMITER);
	int controlIndex = lastIndexOfElement(K_CONTROL_STATEMENT_DELIMITER);
	int instanceOfIndex = lastIndexOfElement(K_BETWEEN_INSTANCEOF_AND_RPAREN);
	if (instanceOfIndex != -1 && instanceOfIndex > controlIndex) {
		index = instanceOfIndex;
	} else if (controlIndex == -1) {
		index = blockIndex;
	} else {
		index = blockIndex != -1 && controlIndex < blockIndex ? blockIndex : controlIndex;
	}
	while (index >= 0) {
		// Try to find an enclosing if statement even if one is not found immediately preceding the completion node.
		int kind = this.elementKindStack[index];
		if ((kind == K_BLOCK_DELIMITER || kind == K_CONTROL_STATEMENT_DELIMITER || kind == K_BETWEEN_INSTANCEOF_AND_RPAREN) // same set as above
			&& this.elementInfoStack[index] == IF && this.elementObjectInfoStack[index] != null)
		{
			Expression condition = (Expression)this.elementObjectInfoStack[index];

			// If currentElement is a RecoveredLocalVariable then it can be contained in the if statement
			if (this.currentElement instanceof RecoveredLocalVariable &&
					this.currentElement.parent instanceof RecoveredBlock) {
				RecoveredLocalVariable recoveredLocalVariable = (RecoveredLocalVariable) this.currentElement;
				if (recoveredLocalVariable.localDeclaration.initialization == null &&
						statement instanceof Expression && ((Expression) statement).isTrulyExpression() &&
						condition.sourceStart < recoveredLocalVariable.localDeclaration.sourceStart) {
					this.currentElement.add(statement, 0);

					statement = recoveredLocalVariable.updatedStatement(0, new HashSet<TypeDeclaration>());

					// RecoveredLocalVariable must be removed from its parent because the IfStatement will be added instead
					RecoveredBlock recoveredBlock =  (RecoveredBlock) recoveredLocalVariable.parent;
					recoveredBlock.statements[--recoveredBlock.statementCount] = null;

					this.currentElement = recoveredBlock;

				}
			}
			if (statement instanceof AND_AND_Expression && this.assistNode instanceof Statement) {
				statement = (Statement) this.assistNode;
			}
			ifStatement =
				new IfStatement(
						condition,
						statement == condition ? new EmptyStatement(condition.sourceEnd, condition.sourceEnd) : statement,
						condition.sourceStart,
						statement.sourceEnd);
			index--;
			this.elementPtr = index;
			break;
		}
		index--;
	}
	if (ifStatement == null) {
		return statement;
	}
	// collect all if statements with instanceof expressions that enclose the completion node
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=304006
	while (index >= 0) {
		if (this.elementInfoStack[index] == IF && isInstanceOfGuard(this.elementObjectInfoStack[index])) {
			Expression condition = (Expression)this.elementObjectInfoStack[index];
			ifStatement =
				new IfStatement(
						condition,
						ifStatement,
						condition.sourceStart,
						ifStatement.sourceEnd);
			this.elementPtr = index;
		}
		index--;
	}
	this.enclosingNode = ifStatement;
	return ifStatement;
}
private boolean isInstanceOfGuard(Object object) {
	if (object instanceof InstanceOfExpression)
		return true;
	if (object instanceof AND_AND_Expression) {
		AND_AND_Expression expression = (AND_AND_Expression) object;
		return isInstanceOfGuard(expression.left) || isInstanceOfGuard(expression.right);
	}
	return false;
}
private void buildMoreGenericsCompletionContext(ASTNode node, boolean consumeTypeArguments) {
	int kind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
	if(kind != 0) {
		int info = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER);
		nextElement : switch (kind) {
			case K_BINARY_OPERATOR :
				int prevKind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER, 1);
				switch (prevKind) {
					case K_PARAMETERIZED_ALLOCATION :
						if(this.invocationType == ALLOCATION || this.invocationType == QUALIFIED_ALLOCATION) {
							this.currentElement = this.currentElement.add((TypeReference)node, 0);
						}
						break nextElement;
					case K_PARAMETERIZED_METHOD_INVOCATION :
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER, 1) == 0) {
							this.currentElement = this.currentElement.add((TypeReference)node, 0);
							break nextElement;
						}
				}
				if(info == LESS && node instanceof TypeReference) {
					if(this.identifierLengthPtr > -1 && this.identifierLengthStack[this.identifierLengthPtr]!= 0) {
						if (consumeTypeArguments) consumeTypeArguments();
						TypeReference ref = this.getTypeReference(0);
						if(prevKind == K_PARAMETERIZED_CAST) {
							ref = computeQualifiedGenericsFromRightSide(ref, 0, null);
						}
						if(this.currentElement instanceof RecoveredType) {
							this.currentElement = this.currentElement.add(new CompletionOnFieldType(ref, false), 0);
						} else {

							if (prevKind == K_BETWEEN_NEW_AND_LEFT_BRACKET) {

								AllocationExpression exp;
								if (this.expressionPtr > -1 && this.expressionStack[this.expressionPtr] instanceof AllocationExpression
										&& this.invocationType == QUALIFIED_ALLOCATION) { // https://bugs.eclipse.org/bugs/show_bug.cgi?id=361963
									exp = new QualifiedAllocationExpression();
									exp.type = ref;
									((QualifiedAllocationExpression)exp).enclosingInstance = this.expressionStack[this.expressionPtr];
								} else {
									exp = new AllocationExpression();
									exp.type = ref;
								}
								if (isInsideReturn()) {
									ReturnStatement returnStatement = new ReturnStatement(exp, exp.sourceStart, exp.sourceEnd);
									this.enclosingNode = returnStatement;
									this.currentElement  = this.currentElement.add(returnStatement,0);
								} else if (this.currentElement instanceof RecoveredLocalVariable) {
									if (((RecoveredLocalVariable)this.currentElement).localDeclaration.initialization == null) {
										this.enclosingNode = ((RecoveredLocalVariable) this.currentElement).localDeclaration;
										this.currentElement = this.currentElement.add(exp, 0);
									}
								} else if (this.currentElement instanceof RecoveredField) {
									if (((RecoveredField) this.currentElement).fieldDeclaration.initialization == null) {
										this.enclosingNode = ((RecoveredField) this.currentElement).fieldDeclaration;
										this.currentElement = this.currentElement.add(exp, 0);
									}
								} else {
									this.currentElement = this.currentElement.add(ref, 0);
								}
							} else {
								this.currentElement = this.currentElement.add(ref, 0);
							}
						}
					} else if (this.currentElement.enclosingMethod() != null &&
							this.currentElement.enclosingMethod().methodDeclaration.isConstructor()) {
						this.currentElement = this.currentElement.add((TypeReference)node, 0);
					}
				}
				break;
			default:
				// have generics been popped off the element stack but still linger on generics stacks?
				if (this.identifierLengthPtr != -1 && this.genericsIdentifiersLengthPtr != -1) {
					int length = this.identifierLengthStack[this.identifierLengthPtr];
					int numberOfIdentifiers = this.genericsIdentifiersLengthStack[this.genericsIdentifiersLengthPtr];
					if (length != numberOfIdentifiers || this.genericsLengthStack[this.genericsLengthPtr] != 0) {
						TypeReference ref = this.getTypeReference(0);
						this.currentElement = this.currentElement.add(ref, 0);
						return;
					}
				}
		}
	}
}
private void buildMoreTryStatementCompletionContext(TypeReference exceptionRef) {
	if (this.astLengthPtr > 0 &&
			this.astPtr > 2 &&
			this.astStack[this.astPtr -1] instanceof Block &&
			this.astStack[this.astPtr - 2] instanceof Argument) {
		TryStatement tryStatement = new TryStatement();

		int newAstPtr = this.astPtr - 1;

		int length = this.astLengthStack[this.astLengthPtr - 1];
		Block[] bks = (tryStatement.catchBlocks = new Block[length + 1]);
		Argument[] args = (tryStatement.catchArguments = new Argument[length + 1]);
		if (length != 0) {
			while (length-- > 0) {
				bks[length] = (Block) this.astStack[newAstPtr--];
				bks[length].statements = null; // statements of catch block won't be used
				args[length] = (Argument) this.astStack[newAstPtr--];
			}
		}

		bks[bks.length - 1] = new Block(0);
		if (this.astStack[this.astPtr] instanceof UnionTypeReference) {
			UnionTypeReference unionTypeReference = (UnionTypeReference) this.astStack[this.astPtr];
			args[args.length - 1] = new Argument(FAKE_ARGUMENT_NAME,0,unionTypeReference,0);
		} else {
			args[args.length - 1] = new Argument(FAKE_ARGUMENT_NAME,0,exceptionRef,0);
		}

		tryStatement.tryBlock = (Block) this.astStack[newAstPtr--];

		this.assistNodeParent = tryStatement;

		this.currentElement.add(tryStatement, 0);
	} else if (this.astLengthPtr > -1 &&
			this.astPtr > 0 &&
			this.astStack[this.astPtr - 1] instanceof Block) {
		TryStatement tryStatement = new TryStatement();

		int newAstPtr = this.astPtr - 1;

		Block[] bks = (tryStatement.catchBlocks = new Block[1]);
		Argument[] args = (tryStatement.catchArguments = new Argument[1]);

		bks[0] = new Block(0);
		if (this.astStack[this.astPtr] instanceof UnionTypeReference) {
			UnionTypeReference unionTypeReference = (UnionTypeReference) this.astStack[this.astPtr];
			args[0] = new Argument(FAKE_ARGUMENT_NAME,0,unionTypeReference,0);
		} else {
			args[0] = new Argument(FAKE_ARGUMENT_NAME,0,exceptionRef,0);
		}

		tryStatement.tryBlock = (Block) this.astStack[newAstPtr--];

		this.assistNodeParent = tryStatement;

		this.currentElement.add(tryStatement, 0);
	}else {
		this.currentElement = this.currentElement.add(exceptionRef, 0);
	}
}
@Override
public int bodyEnd(Initializer initializer){
	return this.cursorLocation;
}
@Override
protected void checkAndSetModifiers(int flag) {
	super.checkAndSetModifiers(flag);

	if (isInsideMethod()) {
		this.hasUnusedModifiers = true;
	}
}
@Override
protected void consumePushCombineModifiers() {
	super.consumePushCombineModifiers();

	if (isInsideMethod()) {
		this.hasUnusedModifiers = true;
	}
}
/**
 * Checks if the completion is on the type following a 'new'.
 * Returns whether we found a completion node.
 */
private boolean checkClassInstanceCreation() {
	if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BETWEEN_NEW_AND_LEFT_BRACKET) {
		int length = this.identifierLengthStack[this.identifierLengthPtr];
		int numberOfIdentifiers = this.genericsIdentifiersLengthStack[this.genericsIdentifiersLengthPtr];
		if (length != numberOfIdentifiers || this.genericsLengthStack[this.genericsLengthPtr] != 0) {
			// no class instance creation with a parameterized type
			return true;
		}

		// completion on type inside an allocation expression

		TypeReference type;
		if (this.invocationType == ALLOCATION) {
			// non qualified allocation expression
			AllocationExpression allocExpr = new AllocationExpression();
			if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER, 1) == K_INSIDE_THROW_STATEMENT
				&& topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER, 1) == this.bracketDepth) {
				pushOnElementStack(K_NEXT_TYPEREF_IS_EXCEPTION);
				type = getTypeReference(0);
				popElement(K_NEXT_TYPEREF_IS_EXCEPTION);
			} else {
				type = getTypeReference(0);
			}
			if(type instanceof CompletionOnSingleTypeReference) {
				((CompletionOnSingleTypeReference)type).isConstructorType = true;
			} else if (type instanceof CompletionOnQualifiedTypeReference) {
				((CompletionOnQualifiedTypeReference)type).isConstructorType = true;
			}
			allocExpr.type = type;
			allocExpr.sourceStart = type.sourceStart;
			allocExpr.sourceEnd = type.sourceEnd;
			pushOnExpressionStack(allocExpr);
			this.isOrphanCompletionNode = false;
		} else {
			// qualified allocation expression
			QualifiedAllocationExpression allocExpr = new QualifiedAllocationExpression();
			pushOnGenericsIdentifiersLengthStack(this.identifierLengthStack[this.identifierLengthPtr]);
			pushOnGenericsLengthStack(0);
			if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER, 1) == K_INSIDE_THROW_STATEMENT
				&& topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER, 1) == this.bracketDepth) {
				pushOnElementStack(K_NEXT_TYPEREF_IS_EXCEPTION);
				type = getTypeReference(0);
				popElement(K_NEXT_TYPEREF_IS_EXCEPTION);
			} else {
				type = getTypeReference(0);
			}
			if(type instanceof CompletionOnSingleTypeReference) {
				((CompletionOnSingleTypeReference)type).isConstructorType = true;
			}
			allocExpr.type = type;
			allocExpr.enclosingInstance = this.expressionStack[this.qualifier];
			allocExpr.sourceStart = this.intStack[this.intPtr--];
			allocExpr.sourceEnd = type.sourceEnd;
			this.expressionStack[this.qualifier] = allocExpr; // attach it now (it replaces the qualifier expression)
			this.isOrphanCompletionNode = false;
		}
		this.assistNode = type;
		this.lastCheckPoint = type.sourceEnd + 1;

		popElement(K_BETWEEN_NEW_AND_LEFT_BRACKET);
		return true;
	}
	return false;
}
/**
 * Checks if the completion is on the dot following an array type,
 * a primitive type or an primitive array type.
 * Returns whether we found a completion node.
 */
private boolean checkClassLiteralAccess() {
	if (this.identifierLengthPtr >= 1 && this.previousToken == TokenNameDOT) { // (NB: the top id length is 1 and it is for the completion identifier)
		int length;
		// if the penultimate id length is negative,
		// the completion is after a primitive type or a primitive array type
		if ((length = this.identifierLengthStack[this.identifierLengthPtr-1]) < 0) {
			// build the primitive type node
			int dim = isAfterArrayType() ? this.intStack[this.intPtr--] : 0;
			Annotation [][] annotationsOnDimensions = dim == 0 ? null : getAnnotationsOnDimensions(dim);
			SingleTypeReference typeRef = (SingleTypeReference)TypeReference.baseTypeReference(-length, dim, annotationsOnDimensions);
			typeRef.sourceStart = this.intStack[this.intPtr--];
			if (dim == 0) {
				typeRef.sourceEnd = this.intStack[this.intPtr--];
			} else {
				this.intPtr--;
				typeRef.sourceEnd = this.endPosition;
			}
			//typeRef.sourceEnd = typeRef.sourceStart + typeRef.token.length; // NB: It's ok to use the length of the token since it doesn't contain any unicode

			// find the completion identifier and its source positions
			char[] source = this.identifierStack[this.identifierPtr];
			long pos = this.identifierPositionStack[this.identifierPtr--];
			this.identifierLengthPtr--; // it can only be a simple identifier (so its length is one)

			// build the completion on class literal access node
			CompletionOnClassLiteralAccess access = new CompletionOnClassLiteralAccess(pos, typeRef);
			access.completionIdentifier = source;
			this.identifierLengthPtr--; // pop the length that was used to say it is a primitive type
			this.assistNode = access;
			this.isOrphanCompletionNode = true;
			return true;
		}

		// if the completion is after a regular array type
		if (isAfterArrayType()) {
			// find the completion identifier and its source positions
			char[] source = this.identifierStack[this.identifierPtr];
			long pos = this.identifierPositionStack[this.identifierPtr--];
			this.identifierLengthPtr--; // it can only be a simple identifier (so its length is one)

			// get the type reference
			pushOnGenericsIdentifiersLengthStack(this.identifierLengthStack[this.identifierLengthPtr]);
			pushOnGenericsLengthStack(0);

			TypeReference typeRef = getTypeReference(this.intStack[this.intPtr--]);

			// build the completion on class literal access node
			CompletionOnClassLiteralAccess access = new CompletionOnClassLiteralAccess(pos, typeRef);
			access.completionIdentifier = source;
			this.assistNode = access;
			this.isOrphanCompletionNode = true;
			return true;
		}

	}
	return false;
}
private boolean checkKeywordAndRestrictedIdentifiers() {
	if (this.currentElement instanceof RecoveredUnit) {
		RecoveredUnit unit = (RecoveredUnit) this.currentElement;
		if (unit.unitDeclaration.isModuleInfo()) return false;
		int index = -1;
		if ((index = this.indexOfAssistIdentifier()) > -1) {
			int ptr = this.identifierPtr - this.identifierLengthStack[this.identifierLengthPtr] + index + 1;

			char[] ident = this.identifierStack[ptr];
			long pos = this.identifierPositionStack[ptr];

			char[][] keywordsAndRestrictedIndentifiers = new char[Keywords.COUNT+RestrictedIdentifiers.COUNT][];
			int count = 0;
			if(unit.typeCount == 0
				&& (!this.compilationUnit.isPackageInfo() || this.compilationUnit.currentPackage != null)
				&& this.lastModifiers == ClassFileConstants.AccDefault) {
				keywordsAndRestrictedIndentifiers[count++] = Keywords.IMPORT;
			}
			if(unit.typeCount == 0
				&& unit.importCount == 0
				&& this.lastModifiers == ClassFileConstants.AccDefault
				&& this.compilationUnit.currentPackage == null) {
				keywordsAndRestrictedIndentifiers[count++] = Keywords.PACKAGE;
			}
			if (!this.compilationUnit.isPackageInfo()) {
				if((this.lastModifiers & ClassFileConstants.AccPublic) == 0) {
					boolean hasNoPublicType = true;
					for (int i = 0; i < unit.typeCount; i++) {
						if((unit.types[i].typeDeclaration.modifiers & ClassFileConstants.AccPublic) != 0) {
							hasNoPublicType = false;
							break;
						}
					}
					if(hasNoPublicType) {
						keywordsAndRestrictedIndentifiers[count++] = Keywords.PUBLIC;
					}
				}
				if((this.lastModifiers & ClassFileConstants.AccAbstract) == 0
					&& (this.lastModifiers & ClassFileConstants.AccFinal) == 0) {
					keywordsAndRestrictedIndentifiers[count++] = Keywords.ABSTRACT;
				}
				if((this.lastModifiers & ClassFileConstants.AccAbstract) == 0
					&& (this.lastModifiers & ClassFileConstants.AccFinal) == 0) {
					keywordsAndRestrictedIndentifiers[count++] = Keywords.FINAL;
				}

				keywordsAndRestrictedIndentifiers[count++] = Keywords.CLASS;
				if (this.options.complianceLevel >= ClassFileConstants.JDK1_5) {
					keywordsAndRestrictedIndentifiers[count++] = Keywords.ENUM;
				}
				if((this.lastModifiers & ClassFileConstants.AccFinal) == 0) {
					keywordsAndRestrictedIndentifiers[count++] = Keywords.INTERFACE;
				}
				if (JavaFeature.RECORDS.isSupported(this.options)) {
					keywordsAndRestrictedIndentifiers[count++] = RestrictedIdentifiers.RECORD;
				}
				if (JavaFeature.SEALED_CLASSES.isSupported(this.options)) {
					boolean nonSeal = (this.lastModifiers & ExtraCompilerModifiers.AccNonSealed) != 0;
					boolean seal = (this.lastModifiers & ExtraCompilerModifiers.AccSealed) != 0;
					if (!nonSeal && !seal) {
						keywordsAndRestrictedIndentifiers[count++] = RestrictedIdentifiers.SEALED;
						keywordsAndRestrictedIndentifiers[count++] = RestrictedIdentifiers.NON_SEALED;
					}
				}
			}
			if (count != 0) {
				System.arraycopy(keywordsAndRestrictedIndentifiers, 0, keywordsAndRestrictedIndentifiers = new char[count][], 0, count);

				this.assistNode = new CompletionOnKeyword2(ident, pos, keywordsAndRestrictedIndentifiers);
				this.lastCheckPoint = this.assistNode.sourceEnd + 1;
				this.isOrphanCompletionNode = true;
				return true;
			}
		}
	}
	return false;
}

private enum ModuleKeyword {
	FIRST_ALL,
	TO,
	PROVIDES_WITH,
	NOT_A_KEYWORD
}

private ModuleKeyword getKeyword() {
	ModuleKeyword keyword = ModuleKeyword.FIRST_ALL;
	if (isInModuleStatements()) {
		if (foundToken(K_AFTER_PACKAGE_IN_PACKAGE_VISIBILITY_STATEMENT)) keyword = ModuleKeyword.TO;
		else if (foundToken(K_AFTER_NAME_IN_PROVIDES_STATEMENT)) keyword = ModuleKeyword.PROVIDES_WITH;
		else keyword = ModuleKeyword.NOT_A_KEYWORD;
	}
	return keyword;
}
private char[][] getModuleKeywords(ModuleKeyword keyword) {
	if (keyword == ModuleKeyword.TO) return new char[][]{Keywords.TO};
	else if (keyword == ModuleKeyword.PROVIDES_WITH) return new char[][]{Keywords.WITH};
	else return new char[][]{Keywords.EXPORTS, Keywords.OPENS, Keywords.REQUIRES, Keywords.PROVIDES, Keywords.USES};
}
private boolean checkModuleInfoConstructs() {

	if (!isInsideModuleInfo()) return false;

	int index = -1;
	if ((index = this.indexOfAssistIdentifier()) <= -1) return false;

	if (this.currentElement instanceof RecoveredModule) {
		RecoveredModule module = (RecoveredModule) this.currentElement;
		if (checkModuleInfoKeyword(module, index)) return true;
	} else  {
		ModuleKeyword keyword = ModuleKeyword.NOT_A_KEYWORD;
		if (isInModuleStatements()) {
			if (foundToken(K_AFTER_PACKAGE_IN_PACKAGE_VISIBILITY_STATEMENT)) keyword =  ModuleKeyword.TO;
			if (foundToken(K_AFTER_NAME_IN_PROVIDES_STATEMENT)) keyword = ModuleKeyword.PROVIDES_WITH;
		}
		if (keyword == ModuleKeyword.NOT_A_KEYWORD) return false;

		int length = this.identifierLengthStack[this.identifierLengthPtr];
		int ptr = this.identifierPtr - length + index + 1;

		char[] ident = this.identifierStack[ptr];
		long pos = this.identifierPositionStack[ptr];
		char[][] keywords = getModuleKeywords(keyword);
		if (this.currentElement instanceof RecoveredPackageVisibilityStatement) {
			RecoveredPackageVisibilityStatement rPvs = (RecoveredPackageVisibilityStatement) this.currentElement;
			rPvs.add(new CompletionOnKeywordModule2(ident, pos, keywords), 0);
			return true;
		} else if (this.currentElement instanceof RecoveredProvidesStatement) {
			RecoveredProvidesStatement rPs = (RecoveredProvidesStatement) this.currentElement;
			rPs.add(new CompletionOnKeyword1(ident, pos, keywords), 0);
			return true;
		}
	}
	return false;
}
private boolean checkModuleInfoKeyword(RecoveredModule module, int index) {
	ModuleKeyword keyword = getKeyword();
	if (keyword == ModuleKeyword.NOT_A_KEYWORD) return false;

	int length = this.identifierLengthStack[this.identifierLengthPtr];
	int ptr = this.identifierPtr - length + index + 1;

	char[] ident = this.identifierStack[ptr];
	long pos = this.identifierPositionStack[ptr];
	char[][] keywords = getModuleKeywords(keyword);
	module.add(new CompletionOnKeywordModuleInfo(ident, pos, keywords), 0);
	return true;
}

private boolean checkInstanceofKeyword() {
	if(isInsideMethod()) {
		int kind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
		int index;
		if(kind != K_BLOCK_DELIMITER
			&& (index = indexOfAssistIdentifier()) > -1
			&& this.expressionPtr > -1
			&& this.expressionLengthStack[this.expressionPtr] == 1) {

			int ptr = this.identifierPtr - this.identifierLengthStack[this.identifierLengthPtr] + index + 1;
			if(this.identifierStack[ptr].length > 0 && CharOperation.prefixEquals(this.identifierStack[ptr], Keywords.INSTANCEOF)) {
				this.assistNode = new CompletionOnKeyword3(
						this.identifierStack[ptr],
						this.identifierPositionStack[ptr],
						Keywords.INSTANCEOF);
				this.lastCheckPoint = this.assistNode.sourceEnd + 1;
				this.isOrphanCompletionNode = true;
				return true;
			}
		}
	}
	return false;
}
private boolean checkYieldKeyword() {
	// Clients to ensure that we are already inside a method
	char[] id = this.scanner.getCurrentIdentifierSource();
	if(id.length > 0 && CharOperation.prefixEquals(id, Keywords.YIELD)) {
		return true;
	}
	return false;
}
/**
 * Checks if the completion is inside a method invocation or a constructor invocation.
 * Returns whether we found a completion node.
 */
private boolean checkInvocation() {
	Expression topExpression = this.expressionPtr >= 0 ?
		this.expressionStack[this.expressionPtr] :
		null;
	boolean isEmptyNameCompletion = false;
	boolean isEmptyAssistIdentifier = false;
	if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_SELECTOR_QUALIFIER
		&& ((isEmptyNameCompletion = topExpression == this.assistNode && isEmptyNameCompletion()) // e.g. it is something like "this.fred([cursor]" but it is not something like "this.fred(1 + [cursor]"
			|| (isEmptyAssistIdentifier = this.indexOfAssistIdentifier() >= 0 && this.identifierStack[this.identifierPtr].length == 0))) { // e.g. it is something like "this.fred(1 [cursor]"

		// pop empty name completion
		if (isEmptyNameCompletion) {
			this.expressionPtr--;
			this.expressionLengthStack[this.expressionLengthPtr]--;
		} else if (isEmptyAssistIdentifier) {
			this.identifierPtr--;
			this.identifierLengthPtr--;
		}

		// find receiver and qualifier
		int invocType = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER, 1);
		int qualifierExprPtr = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER);

		// find arguments
		int numArgs = this.expressionPtr - qualifierExprPtr;
		int argStart = qualifierExprPtr + 1;
		Expression[] arguments = null;
		if (numArgs > 0) {
			// remember the arguments
			arguments = new Expression[numArgs];
			System.arraycopy(this.expressionStack, argStart, arguments, 0, numArgs);

			// consume the expression arguments
			this.expressionPtr -= numArgs;
			int count = numArgs;
			while (count > 0) {
				count -= this.expressionLengthStack[this.expressionLengthPtr--];
			}
		}

		// build ast node
		if (invocType != ALLOCATION && invocType != QUALIFIED_ALLOCATION) {
			// creates completion on message send
			CompletionOnMessageSend messageSend = new CompletionOnMessageSend();
			messageSend.arguments = arguments;
			switch (invocType) {
				case NO_RECEIVER:
					// implicit this
					messageSend.receiver = ThisReference.implicitThis();
					break;
				case NAME_RECEIVER:
					// remove special flags for primitive types
					while (this.identifierLengthPtr >= 0 && this.identifierLengthStack[this.identifierLengthPtr] < 0) {
						this.identifierLengthPtr--;
					}

					// remove selector
					this.identifierPtr--;
					if(this.genericsPtr > -1 && this.genericsLengthPtr > -1 && this.genericsLengthStack[this.genericsLengthPtr] > 0) {
						// is inside a paremeterized method: bar.<X>.foo
						this.identifierLengthPtr--;
					} else {
						this.identifierLengthStack[this.identifierLengthPtr]--;
						int length = this.typeAnnotationLengthStack[this.typeAnnotationLengthPtr--];
						Annotation [] typeAnnotations;
						if (length != 0) {
							System.arraycopy(
									this.typeAnnotationStack,
									(this.typeAnnotationPtr -= length) + 1,
									typeAnnotations = new Annotation[length],
									0,
									length);
							problemReporter().misplacedTypeAnnotations(typeAnnotations[0], typeAnnotations[typeAnnotations.length - 1]);
						}
					}
					// consume the receiver
					messageSend.receiver = getUnspecifiedReference();
					break;
				case SUPER_RECEIVER:
					messageSend.receiver = new SuperReference(0, 0);
					break;
				case EXPLICIT_RECEIVER:
					messageSend.receiver = this.expressionStack[qualifierExprPtr];
			}

			// set selector
			int selectorPtr = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER, 2);
			messageSend.selector = this.identifierStack[selectorPtr];
			// remove selector
			if (this.identifierLengthPtr >=0 && this.identifierLengthStack[this.identifierLengthPtr] == 1) {
				this.identifierPtr--;
				this.identifierLengthPtr--;
			}

			// the entire message may be replaced in case qualification is needed
			messageSend.sourceStart = (int)(this.identifierPositionStack[selectorPtr] >> 32); //this.cursorLocation + 1;
			messageSend.sourceEnd = this.cursorLocation;

			// remember the message send as an orphan completion node
			this.assistNode = messageSend;
			this.lastCheckPoint = messageSend.sourceEnd + 1;
			this.isOrphanCompletionNode = true;
			return true;
		} else {
			int selectorPtr = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER, 2);
			if (selectorPtr == THIS_CONSTRUCTOR || selectorPtr == SUPER_CONSTRUCTOR) {
				// creates an explicit constructor call
				CompletionOnExplicitConstructorCall call = new CompletionOnExplicitConstructorCall(
					(selectorPtr == THIS_CONSTRUCTOR) ? ExplicitConstructorCall.This : ExplicitConstructorCall.Super);
				call.arguments = arguments;
				if (invocType == QUALIFIED_ALLOCATION) {
					call.qualification = this.expressionStack[qualifierExprPtr];
				}

				// no source is going to be replaced
				call.sourceStart = this.cursorLocation + 1;
				call.sourceEnd = this.cursorLocation;

				// remember the explicit constructor call as an orphan completion node
				this.assistNode = call;
				this.lastCheckPoint = call.sourceEnd + 1;
				this.isOrphanCompletionNode = true;
				return true;
			} else {
				// creates an allocation expression
				CompletionOnQualifiedAllocationExpression allocExpr = new CompletionOnQualifiedAllocationExpression();
				allocExpr.arguments = arguments;
				if(this.genericsLengthPtr < 0) {
					pushOnGenericsLengthStack(0);
					pushOnGenericsIdentifiersLengthStack(this.identifierLengthStack[this.identifierLengthPtr]);
				}
				allocExpr.type = super.getTypeReference(0); // we don't want a completion node here, so call super
				if (invocType == QUALIFIED_ALLOCATION) {
					allocExpr.enclosingInstance = this.expressionStack[qualifierExprPtr];
				}
				// no source is going to be replaced
				allocExpr.sourceStart = this.cursorLocation + 1;
				allocExpr.sourceEnd = this.cursorLocation;

				// remember the allocation expression as an orphan completion node
				this.assistNode = allocExpr;
				this.lastCheckPoint = allocExpr.sourceEnd + 1;
				this.isOrphanCompletionNode = true;
				return true;
			}
		}
	}
	return false;
}
private boolean checkLabelStatement() {
	if(isInsideMethod() || isInsideFieldInitialization()) {

		int kind = this.topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
		if(kind != K_INSIDE_BREAK_STATEMENT && kind != K_INSIDE_CONTINUE_STATEMENT) return false;

		if (indexOfAssistIdentifier() != 0) return false;

		char[][] labels = new char[this.labelPtr + 1][];
		int labelCount = 0;

		int labelKind = kind;
		int index = 1;
		while(labelKind != 0 && labelKind != K_METHOD_DELIMITER) {
			labelKind = this.topKnownElementKind(COMPLETION_OR_ASSIST_PARSER, index);
			if(labelKind == K_LABEL) {
				int ptr = this.topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER, index);
				labels[labelCount++] = this.labelStack[ptr];
			}
			index++;
		}
		System.arraycopy(labels, 0, labels = new char[labelCount][], 0, labelCount);

		long position = this.identifierPositionStack[this.identifierPtr];
		this.assistNode = kind == K_INSIDE_BREAK_STATEMENT
				? new CompletionOnBreakStatement(
						this.identifierStack[this.identifierPtr--],
						(int) (position >>> 32),
						(int)position,
						labels)
				: new CompletionOnContinueStatement(
						this.identifierStack[this.identifierPtr--],
						(int) (position >>> 32),
						(int)position,
						labels);
		this.lastCheckPoint = this.assistNode.sourceEnd + 1;
		this.isOrphanCompletionNode = true;
		return true;
	}
	return false;
}
/**
 * Checks if the completion is on a member access (i.e. in an identifier following a dot).
 * Returns whether we found a completion node.
 */
private boolean checkMemberAccess() {
	if (this.previousToken == TokenNameDOT && this.qualifier > -1 && this.expressionPtr == this.qualifier) {
		if (this.identifierLengthPtr > 1 && this.identifierLengthStack[this.identifierLengthPtr - 1] < 0) {
			// its not a  member access because the receiver is a base type
			// fix for bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=137623
			return false;
		}
		// the receiver is an expression
		pushCompletionOnMemberAccessOnExpressionStack(false);
		return true;
	}
	return false;
}
/**
 * Checks if the completion is on a name reference.
 * Returns whether we found a completion node.
 */
private boolean checkNameCompletion() {
	/*
		We didn't find any other completion, but the completion identifier is on the identifier stack,
		so it can only be a completion on name.
		Note that we allow the completion on a name even if nothing is expected (e.g. foo() b[cursor] would
		be a completion on 'b'). This policy gives more to the user than he/she would expect, but this
		simplifies the problem. To fix this, the recovery must be changed to work at a 'statement' granularity
		instead of at the 'expression' granularity as it does right now.
	*/

	// NB: at this point the completion identifier is on the identifier stack
	this.assistNode = getUnspecifiedReferenceOptimized();
	this.lastCheckPoint = this.assistNode.sourceEnd + 1;
	this.isOrphanCompletionNode = true;
	if (this.hasUnusedModifiers &&
			this.assistNode instanceof CompletionOnSingleNameReference) {
		((CompletionOnSingleNameReference)this.assistNode).isPrecededByModifiers = true;
	}
	return true;
}
private boolean checkParemeterizedMethodName() {
	if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_PARAMETERIZED_METHOD_INVOCATION &&
			topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == INSIDE_NAME) {
		if(this.identifierLengthPtr > -1 && this.genericsLengthPtr > -1 && this.genericsIdentifiersLengthPtr == -1) {
			CompletionOnMessageSendName m = null;
			switch (this.invocationType) {
				case EXPLICIT_RECEIVER:
				case NO_RECEIVER: // this case occurs with 'bar().foo'
					if(this.expressionPtr > -1 && this.expressionLengthStack[this.expressionLengthPtr] == 1) {
						char[] selector = this.identifierStack[this.identifierPtr];
						long position = this.identifierPositionStack[this.identifierPtr--];
						this.identifierLengthPtr--;
						int end = (int) position;
						int start = (int) (position >>> 32);
						m = new CompletionOnMessageSendName(selector, start, end, false);

						// handle type arguments
						int length = this.genericsLengthStack[this.genericsLengthPtr--];
						this.genericsPtr -= length;
						System.arraycopy(this.genericsStack, this.genericsPtr + 1, m.typeArguments = new TypeReference[length], 0, length);
						this.intPtr--;

						m.receiver = this.expressionStack[this.expressionPtr--];
						this.expressionLengthPtr--;
					}
					break;
				case NAME_RECEIVER:
					if(this.identifierPtr > 0) {
						char[] selector = this.identifierStack[this.identifierPtr];
						long position = this.identifierPositionStack[this.identifierPtr--];
						this.identifierLengthPtr--;
						int end = (int) position;
						int start = (int) (position >>> 32);
						m = new CompletionOnMessageSendName(selector, start, end, false);

						// handle type arguments
						int length = this.genericsLengthStack[this.genericsLengthPtr--];
						this.genericsPtr -= length;
						System.arraycopy(this.genericsStack, this.genericsPtr + 1, m.typeArguments = new TypeReference[length], 0, length);
						this.intPtr--;

						m.receiver = getUnspecifiedReference();
					}
					break;
				case SUPER_RECEIVER:
					char[] selector = this.identifierStack[this.identifierPtr];
					long position = this.identifierPositionStack[this.identifierPtr--];
					this.identifierLengthPtr--;
					int end = (int) position;
					int start = (int) (position >>> 32);
					m = new CompletionOnMessageSendName(selector, start, end, false);

					// handle type arguments
					int length = this.genericsLengthStack[this.genericsLengthPtr--];
					this.genericsPtr -= length;
					System.arraycopy(this.genericsStack, this.genericsPtr + 1, m.typeArguments = new TypeReference[length], 0, length);
					this.intPtr--;

					m.receiver = new SuperReference(start, end);
					break;
			}

			if(m != null) {
				pushOnExpressionStack(m);

				this.assistNode = m;
				this.lastCheckPoint = this.assistNode.sourceEnd + 1;
				this.isOrphanCompletionNode = true;
				return true;
			}
		}
	}
	return false;
}
private boolean checkParemeterizedType() {
	if(this.identifierLengthPtr > -1 && this.genericsLengthPtr > -1 && this.genericsIdentifiersLengthPtr > -1) {
		int length = this.identifierLengthStack[this.identifierLengthPtr];
		int numberOfIdentifiers = this.genericsIdentifiersLengthStack[this.genericsIdentifiersLengthPtr];
		if (length != numberOfIdentifiers || this.genericsLengthStack[this.genericsLengthPtr] != 0) {
			this.genericsIdentifiersLengthPtr--;
			this.identifierLengthPtr--;
			// generic type
			this.assistNode = getAssistTypeReferenceForGenericType(0, length, numberOfIdentifiers);
			this.lastCheckPoint = this.assistNode.sourceEnd + 1;
			this.isOrphanCompletionNode = true;
			return true;
		} else if(this.genericsPtr > -1 && this.genericsStack[this.genericsPtr] instanceof TypeReference) {
			// type of a cast expression
			numberOfIdentifiers++;

			this.genericsIdentifiersLengthPtr--;
			this.identifierLengthPtr--;
			// generic type
			this.assistNode = getAssistTypeReferenceForGenericType(0, length, numberOfIdentifiers);
			this.lastCheckPoint = this.assistNode.sourceEnd + 1;
			this.isOrphanCompletionNode = true;
			return true;
		}
	}
	return false;
}
/**
 * Checks if the completion is in the context of a method and on the type of one of its arguments
 * Returns whether we found a completion node.
 */
private boolean checkRecoveredMethod() {
	if (this.currentElement instanceof RecoveredMethod){
		/* check if current awaiting identifier is the completion identifier */
		if (this.indexOfAssistIdentifier() < 0) return false;

		/* check if on line with an error already - to avoid completing inside
			illegal type names e.g.  int[<cursor> */
		if (this.lastErrorEndPosition <= this.cursorLocation
			&& Util.getLineNumber(this.lastErrorEndPosition, this.scanner.lineEnds, 0, this.scanner.linePtr)
					== Util.getLineNumber(((CompletionScanner)this.scanner).completedIdentifierStart, this.scanner.lineEnds, 0, this.scanner.linePtr)){
			return false;
		}
 		RecoveredMethod recoveredMethod = (RecoveredMethod)this.currentElement;
		/* only consider if inside method header */
		if (!recoveredMethod.foundOpeningBrace
			&& this.lastIgnoredToken == -1) {
			//if (rParenPos < lParenPos){ // inside arguments
			this.assistNode = this.getTypeReference(0);
			this.lastCheckPoint = this.assistNode.sourceEnd + 1;
			this.isOrphanCompletionNode = true;
			return true;
		}
	}
	return false;
}
private boolean checkMemberValueName() {
	/* check if current awaiting identifier is the completion identifier */
	if (this.indexOfAssistIdentifier() < 0) return false;

	if (this.topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) != K_BETWEEN_ANNOTATION_NAME_AND_RPAREN) return false;

	if(this.identifierPtr > -1 && this.identifierLengthPtr > -1 && this.identifierLengthStack[this.identifierLengthPtr] == 1) {
		char[] simpleName = this.identifierStack[this.identifierPtr];
		long position = this.identifierPositionStack[this.identifierPtr--];
		this.identifierLengthPtr--;
		int end = (int) position;
		int start = (int) (position >>> 32);


		CompletionOnMemberValueName memberValueName = new CompletionOnMemberValueName(simpleName,start, end);
		this.assistNode = memberValueName;
		this.lastCheckPoint = this.assistNode.sourceEnd + 1;
		this.isOrphanCompletionNode = true;

		return true;
	}
	return false;
}
/**
 * Checks if the completion is in the context of a type and on a type reference in this type.
 * Persists the identifier into a fake field return type
 * Returns whether we found a completion node.
 */
private boolean checkRecoveredType() {
	if (this.currentElement instanceof RecoveredType){
		/* check if current awaiting identifier is the completion identifier */
		if (this.indexOfAssistIdentifier() < 0) return false;

		/* check if on line with an error already - to avoid completing inside
			illegal type names e.g.  int[<cursor> */
		if (this.lastErrorEndPosition <= this.cursorLocation
			&& ((RecoveredType)this.currentElement).lastMemberEnd() < this.lastErrorEndPosition
			&& Util.getLineNumber(this.lastErrorEndPosition, this.scanner.lineEnds, 0, this.scanner.linePtr)
					== Util.getLineNumber(((CompletionScanner)this.scanner).completedIdentifierStart, this.scanner.lineEnds, 0, this.scanner.linePtr)){
			return false;
		}
		RecoveredType recoveredType = (RecoveredType)this.currentElement;
		/* filter out cases where scanner is still inside type header */
		if (recoveredType.foundOpeningBrace) {
			// complete generics stack if necessary
			if((this.genericsIdentifiersLengthPtr < 0 && this.identifierPtr > -1)
					|| (this.genericsIdentifiersLengthStack[this.genericsIdentifiersLengthPtr] <= this.identifierPtr)) {
				pushOnGenericsIdentifiersLengthStack(this.identifierLengthStack[this.identifierLengthPtr]);
				pushOnGenericsLengthStack(0); // handle type arguments
			}
			this.assistNode = this.getTypeReference(0);
			this.lastCheckPoint = this.assistNode.sourceEnd + 1;
			this.isOrphanCompletionNode = true;
			return true;
		} else {
			if(recoveredType.typeDeclaration.superclass == null &&
					this.topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_EXTENDS_KEYWORD) {
				consumeClassOrInterfaceName();
				this.pushOnElementStack(K_NEXT_TYPEREF_IS_CLASS);
				this.assistNode = this.getTypeReference(0);
				popElement(K_NEXT_TYPEREF_IS_CLASS);
				this.lastCheckPoint = this.assistNode.sourceEnd + 1;
				this.isOrphanCompletionNode = true;
				return true;
			}
		}
	}
	return false;
}
private void classHeaderExtendsOrImplements(boolean isInterface, boolean isRecord) {
	if (this.currentElement != null
			&& this.currentToken == TokenNameIdentifier
			&& this.cursorLocation+1 >= this.scanner.startPosition
			&& this.cursorLocation < this.scanner.currentPosition){
			this.pushIdentifier();
		int index = -1;
		/* check if current awaiting identifier is the completion identifier */
		if ((index = this.indexOfAssistIdentifier()) > -1) {
			int ptr = this.identifierPtr - this.identifierLengthStack[this.identifierLengthPtr] + index + 1;
			RecoveredType recoveredType = (RecoveredType)this.currentElement;
			/* filter out cases where scanner is still inside type header */
			if (!recoveredType.foundOpeningBrace) {
				TypeDeclaration type = recoveredType.typeDeclaration;
				if(!isInterface) {
					char[][] keywords = new char[Keywords.COUNT][];
					int count = 0;


					if(type.superInterfaces == null) {
						if(!isRecord) {
							if(type.superclass == null) {
								keywords[count++] = Keywords.EXTENDS;
							}
						}
						keywords[count++] = Keywords.IMPLEMENTS;
					}
					if (JavaFeature.SEALED_CLASSES.isSupported(this.options)) {
						boolean sealed = (type.modifiers & ExtraCompilerModifiers.AccSealed) != 0;
						if (sealed)
							keywords[count++] = RestrictedIdentifiers.PERMITS;
					}

					System.arraycopy(keywords, 0, keywords = new char[count][], 0, count);

					if(count > 0) {
						CompletionOnKeyword1 completionOnKeyword = new CompletionOnKeyword1(
							this.identifierStack[ptr],
							this.identifierPositionStack[ptr],
							keywords);
						type.superclass = completionOnKeyword;
						type.superclass.bits |= ASTNode.IsSuperType;
						this.assistNode = completionOnKeyword;
						this.lastCheckPoint = completionOnKeyword.sourceEnd + 1;
					}
				} else {
					if(type.superInterfaces == null) {
						CompletionOnKeyword1 completionOnKeyword = new CompletionOnKeyword1(
							this.identifierStack[ptr],
							this.identifierPositionStack[ptr],
							Keywords.EXTENDS);
						type.superInterfaces = new TypeReference[]{completionOnKeyword};
						type.superInterfaces[0].bits |= ASTNode.IsSuperType;
						this.assistNode = completionOnKeyword;
						this.lastCheckPoint = completionOnKeyword.sourceEnd + 1;
					}
				}
			}
		}
	}
}
/*
 * Check whether about to shift beyond the completion token.
 * If so, depending on the context, a special node might need to be created
 * and attached to the existing recovered structure so as to be remember in the
 * resulting parsed structure.
 */
public void completionIdentifierCheck(){
	//if (assistNode != null) return;

	if (checkMemberValueName()) return;

	if(checkKeywordAndRestrictedIdentifiers()) return;
	if (checkModuleInfoConstructs()) return;
	if (checkRecoveredType()) return;
	if (checkRecoveredMethod()) return;

	// if not in a method in non diet mode and if not inside a field initializer, only record references attached to types
	if (!(isInsideMethod() && !this.diet)
		&& !isIndirectlyInsideFieldInitialization()
		&& !isIndirectlyInsideEnumConstantnitialization()
		&& !isInsideAttributeValue()
		&& !isInsideModuleInfo()) return;

	/*
	 	In some cases, the completion identifier may not have yet been consumed,
	 	e.g.  int.[cursor]
	 	This is because the grammar does not allow any (empty) identifier to follow
	 	a base type. We thus have to manually force the identifier to be consumed
	 	(that is, pushed).
	 */
	if (assistIdentifier() == null && this.currentToken == TokenNameIdentifier) { // Test below copied from CompletionScanner.getCurrentIdentifierSource()
		if (this.cursorLocation < this.scanner.startPosition && this.scanner.currentPosition == this.scanner.startPosition){ // fake empty identifier got issued
			this.pushIdentifier();
		} else if (this.cursorLocation+1 >= this.scanner.startPosition && this.cursorLocation < this.scanner.currentPosition){
			this.pushIdentifier();
		}
	}

	// check for different scenarii
	// no need to go further if we found a non empty completion node
	// (we still need to store labels though)
	if (this.assistNode != null) {
		// however inside an invocation, the completion identifier may already have been consumed into an empty name
		// completion, so this check should be before we check that we are at the cursor location
		if (!isEmptyNameCompletion() || checkInvocation()) return;
	}

	// no need to check further if we are not at the cursor location
	if (this.indexOfAssistIdentifier() < 0) return;

	if (checkModuleInfoConstructs()) return;
	if (checkClassInstanceCreation()) return;
	if (checkMemberAccess()) return;
	if (checkClassLiteralAccess()) return;
	if (checkInstanceofKeyword()) return;

	// if the completion was not on an empty name, it can still be inside an invocation (e.g. this.fred("abc"[cursor])
	// (NB: Put this check before checkNameCompletion() because the selector of the invocation can be on the identifier stack)
	if (checkInvocation()) return;

	if (checkParemeterizedType()) return;
	if (checkParemeterizedMethodName()) return;
	if (checkLabelStatement()) return;
	if (checkNameCompletion()) return;
}
@Override
protected void consumeArrayCreationExpressionWithInitializer() {
	super.consumeArrayCreationExpressionWithInitializer();
	popElement(K_ARRAY_CREATION);
}
@Override
protected void consumeArrayCreationExpressionWithoutInitializer() {
	super.consumeArrayCreationExpressionWithoutInitializer();
	popElement(K_ARRAY_CREATION);
}
@Override
protected void consumeArrayCreationHeader() {
	// nothing to do
}
@Override
protected void consumeAssignment() {
	popElement(K_ASSISGNMENT_OPERATOR);
	super.consumeAssignment();
}
@Override
protected void consumeAssignmentOperator(int pos) {
	super.consumeAssignmentOperator(pos);
	pushOnElementStack(K_ASSISGNMENT_OPERATOR, pos);
}
@Override
protected void consumeBinaryExpression(int op) {
	super.consumeBinaryExpression(op);
	popElement(K_BINARY_OPERATOR);

	if(this.expressionStack[this.expressionPtr] instanceof BinaryExpression) {
		BinaryExpression exp = (BinaryExpression) this.expressionStack[this.expressionPtr];
		if(this.assistNode != null && exp.right == this.assistNode) {
			this.assistNodeParent = exp;
		}
	}
}
@Override
protected void consumeBinaryExpressionWithName(int op) {
	super.consumeBinaryExpressionWithName(op);
	popElement(K_BINARY_OPERATOR);

	if(this.expressionStack[this.expressionPtr] instanceof BinaryExpression) {
		BinaryExpression exp = (BinaryExpression) this.expressionStack[this.expressionPtr];
		if(this.assistNode != null && exp.right == this.assistNode) {
			this.assistNodeParent = exp;
		}
	}
}
@Override
protected void consumeCaseLabel() {
	super.consumeCaseLabel();
	if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) != K_SWITCH_LABEL) {
		pushOnElementStack(K_SWITCH_LABEL);
	}
}
@Override
protected void consumeCastExpressionWithPrimitiveType() {
	popElement(K_CAST_STATEMENT);

	Expression exp;
	Expression cast;
	TypeReference castType;
	this.expressionPtr--;
	this.expressionLengthPtr--;
	this.expressionStack[this.expressionPtr] = cast = new CastExpression(exp = this.expressionStack[this.expressionPtr+1], castType = (TypeReference) this.expressionStack[this.expressionPtr]);
	cast.sourceStart = castType.sourceStart - 1;
	cast.sourceEnd = exp.sourceEnd;
}
@Override
protected void consumeCastExpressionWithGenericsArray() {
	popElement(K_CAST_STATEMENT);

	Expression exp;
	Expression cast;
	TypeReference castType;
	this.expressionPtr--;
	this.expressionLengthPtr--;
	this.expressionStack[this.expressionPtr] = cast = new CastExpression(exp = this.expressionStack[this.expressionPtr + 1], castType = (TypeReference) this.expressionStack[this.expressionPtr]);
	cast.sourceStart = castType.sourceStart - 1;
	cast.sourceEnd = exp.sourceEnd;
}

@Override
protected void consumeCastExpressionWithQualifiedGenericsArray() {
	popElement(K_CAST_STATEMENT);

	Expression exp;
	Expression cast;
	TypeReference castType;
	this.expressionPtr--;
	this.expressionLengthPtr--;
	this.expressionStack[this.expressionPtr] = cast = new CastExpression(exp = this.expressionStack[this.expressionPtr + 1], castType = (TypeReference) this.expressionStack[this.expressionPtr]);
	cast.sourceStart = castType.sourceStart - 1;
	cast.sourceEnd = exp.sourceEnd;
}
@Override
protected void consumeCastExpressionWithNameArray() {
	// CastExpression ::= PushLPAREN Name Dims PushRPAREN InsideCastExpression UnaryExpressionNotPlusMinus
	popElement(K_CAST_STATEMENT);

	Expression exp;
	Expression cast;
	TypeReference castType;
	this.expressionPtr--;
	this.expressionLengthPtr--;
	this.expressionStack[this.expressionPtr] = cast = new CastExpression(exp = this.expressionStack[this.expressionPtr+1], castType = (TypeReference) this.expressionStack[this.expressionPtr]);
	cast.sourceStart = castType.sourceStart - 1;
	cast.sourceEnd = exp.sourceEnd;
}
@Override
protected void consumeCastExpressionLL1() {
	popElement(K_CAST_STATEMENT);
	super.consumeCastExpressionLL1();
}
@Override
protected void consumeCatchFormalParameter() {
	if (this.indexOfAssistIdentifier() < 0) {
		super.consumeCatchFormalParameter();
		if (this.pendingAnnotation != null) {
			this.pendingAnnotation.potentialAnnotatedNode = this.astStack[this.astPtr];
			this.pendingAnnotation = null;
		}
	} else {
		this.identifierLengthPtr--;
		char[] identifierName = this.identifierStack[this.identifierPtr];
		long namePositions = this.identifierPositionStack[this.identifierPtr--];
		this.intPtr--; // dimension from the variabledeclaratorid
		TypeReference type = (TypeReference) this.astStack[this.astPtr--];
		this.intPtr -= 2;
		CompletionOnArgumentName arg =
			new CompletionOnArgumentName(
				identifierName,
				namePositions,
				type,
				this.intStack[this.intPtr + 1] & ~ClassFileConstants.AccDeprecated); // modifiers
		arg.bits &= ~ASTNode.IsArgument;
		// consume annotations
		int length;
		if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
			System.arraycopy(
				this.expressionStack,
				(this.expressionPtr -= length) + 1,
				arg.annotations = new Annotation[length],
				0,
				length);
		}

		arg.isCatchArgument = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BETWEEN_CATCH_AND_RIGHT_PAREN;
		pushOnAstStack(arg);

		this.assistNode = arg;
		this.lastCheckPoint = (int) namePositions;
		this.isOrphanCompletionNode = true;

		/* if incomplete method header, listLength counter will not have been reset,
			indicating that some arguments are available on the stack */
		this.listLength++;
	}
}
@Override
protected void consumeClassBodyDeclaration() {
	popElement(K_BLOCK_DELIMITER);
	super.consumeClassBodyDeclaration();
	this.pendingAnnotation = null; // the pending annotation cannot be attached to next nodes
}
@Override
protected void consumeClassBodyopt() {
	popElement(K_SELECTOR_QUALIFIER);
	popElement(K_SELECTOR_INVOCATION_TYPE);
	super.consumeClassBodyopt();
}

@Override
protected void consumeClassDeclaration() {
	if (this.astPtr >= 0) {
		int length = this.astLengthStack[this.astLengthPtr];
		TypeDeclaration typeDeclaration = (TypeDeclaration) this.astStack[this.astPtr-length];
		this.javadoc = null;
		CompletionJavadocParser completionJavadocParser = (CompletionJavadocParser)this.javadocParser;
		completionJavadocParser.allPossibleTags = true;
		checkComment();
		if (this.javadoc != null && this.cursorLocation > this.javadoc.sourceStart && this.cursorLocation < this.javadoc.sourceEnd) {
			// completion is in an orphan javadoc comment => replace in last read declaration to allow completion resolution
			typeDeclaration.javadoc = this.javadoc;
		}
		completionJavadocParser.allPossibleTags = false;
	}
	super.consumeClassDeclaration();
}
@Override
protected void consumeClassHeaderName1() {
	super.consumeClassHeaderName1();
	this.hasUnusedModifiers = false;
	if (this.pendingAnnotation != null) {
		this.pendingAnnotation.potentialAnnotatedNode = this.astStack[this.astPtr];
		this.pendingAnnotation = null;
	}
	classHeaderExtendsOrImplements(false,false);
}

@Override
protected void consumeRecordHeaderPart() {
	super.consumeRecordHeaderPart();
	this.hasUnusedModifiers = false;
	if (this.pendingAnnotation != null) {
		this.pendingAnnotation.potentialAnnotatedNode = this.astStack[this.astPtr];
		this.pendingAnnotation = null;
	}
	classHeaderExtendsOrImplements(false,true);
}

@Override
protected void consumeClassHeaderExtends() {
	pushOnElementStack(K_NEXT_TYPEREF_IS_CLASS);
	super.consumeClassHeaderExtends();
	if (this.assistNode != null && this.assistNodeParent == null) {
		TypeDeclaration typeDecl = (TypeDeclaration) this.astStack[this.astPtr];
		if (typeDecl != null && typeDecl.superclass == this.assistNode)
			this.assistNodeParent = typeDecl;
	}
	popElement(K_NEXT_TYPEREF_IS_CLASS);
	popElement(K_EXTENDS_KEYWORD);

	if (this.currentElement != null
		&& this.currentToken == TokenNameIdentifier
		&& this.cursorLocation+1 >= this.scanner.startPosition
		&& this.cursorLocation < this.scanner.currentPosition){
		this.pushIdentifier();

		int index = -1;
		/* check if current awaiting identifier is the completion identifier */
		if ((index = this.indexOfAssistIdentifier()) > -1) {
			int ptr = this.identifierPtr - this.identifierLengthStack[this.identifierLengthPtr] + index + 1;
			RecoveredType recoveredType = (RecoveredType)this.currentElement;
			/* filter out cases where scanner is still inside type header */
			if (!recoveredType.foundOpeningBrace) {
				char[][] keywords = new char[2][];
				int count = 0;
				TypeDeclaration type = recoveredType.typeDeclaration;
				if(type.superInterfaces == null) {
					keywords[count++] = Keywords.IMPLEMENTS;
				}
				if (JavaFeature.SEALED_CLASSES.isSupported(this.options)) {
					boolean sealed = (type.modifiers & ExtraCompilerModifiers.AccSealed) != 0;
					if (sealed)
						keywords[count++] = RestrictedIdentifiers.PERMITS;
				}
				System.arraycopy(keywords, 0, keywords = new char[count][], 0, count);
				if(count > 0) {
					CompletionOnKeyword1 completionOnKeyword = new CompletionOnKeyword1(
						this.identifierStack[ptr],
						this.identifierPositionStack[ptr],
						keywords);
					type.superclass = completionOnKeyword;
					type.superclass.bits |= ASTNode.IsSuperType;
					this.assistNode = completionOnKeyword;
					this.lastCheckPoint = completionOnKeyword.sourceEnd + 1;
				}
			}
		}
	}
}
@Override
protected void consumeClassHeaderImplements() {
	super.consumeClassHeaderImplements();
	if (this.assistNode != null && this.assistNodeParent == null) {
		TypeDeclaration typeDecl = (TypeDeclaration) this.astStack[this.astPtr];
		if (typeDecl != null) {
			TypeReference[] superInterfaces = typeDecl.superInterfaces;
			int length = superInterfaces == null ? 0 : superInterfaces.length;
			for (int i = 0; i < length; i++) {
				if (superInterfaces[i] == this.assistNode) {
					this.assistNodeParent = typeDecl;
				}
			}
		}
	}
}
@Override
protected void consumeClassInstanceCreationExpressionName() {
	super.consumeClassInstanceCreationExpressionName();
	this.invocationType = QUALIFIED_ALLOCATION;
	this.qualifier = this.expressionPtr;
}
@Override
protected void consumeClassTypeElt() {
	pushOnElementStack(K_NEXT_TYPEREF_IS_EXCEPTION);
	super.consumeClassTypeElt();
	popElement(K_NEXT_TYPEREF_IS_EXCEPTION);
}
@Override
protected void consumeCompilationUnit() {
	this.javadoc = null;
	checkComment();
	if (this.javadoc != null && this.cursorLocation > this.javadoc.sourceStart && this.cursorLocation < this.javadoc.sourceEnd) {
		// completion is in an orphan javadoc comment => replace compilation unit one to allow completion resolution
		this.compilationUnit.javadoc = this.javadoc;
		// create a fake interface declaration to allow resolution
		if (this.compilationUnit.types == null) {
			this.compilationUnit.types = new TypeDeclaration[1];
			TypeDeclaration declaration = new TypeDeclaration(this.compilationUnit.compilationResult);
			declaration.name = FAKE_TYPE_NAME;
			declaration.modifiers = ClassFileConstants.AccDefault | ClassFileConstants.AccInterface;
			this.compilationUnit.types[0] = declaration;
		}
	}
	super.consumeCompilationUnit();
}
@Override
protected void consumeSwitchExpression() {
	super.consumeSwitchExpression();
	if (this.assistNode != null) {
		SwitchExpression expr = (SwitchExpression) this.expressionStack[this.expressionPtr];
		expr.resolveAll = true;
	}
}
@Override
protected void consumeConditionalExpression(int op) {
	popElement(K_CONDITIONAL_OPERATOR);
	super.consumeConditionalExpression(op);
}
@Override
protected void consumeConditionalExpressionWithName(int op) {
	popElement(K_CONDITIONAL_OPERATOR);
	super.consumeConditionalExpressionWithName(op);
}
@Override
protected void consumeConstructorBody() {
	popElement(K_BLOCK_DELIMITER);
	super.consumeConstructorBody();
}
@Override
protected void consumeConstructorHeader() {
	super.consumeConstructorHeader();
	pushOnElementStack(K_BLOCK_DELIMITER);
}
@Override
protected void consumeConstructorHeaderName() {

	/* no need to take action if not inside assist identifiers */
	if (indexOfAssistIdentifier() < 0) {
		long selectorSourcePositions = this.identifierPositionStack[this.identifierPtr];
		int selectorSourceEnd = (int) selectorSourcePositions;
		int currentAstPtr = this.astPtr;
		/* recovering - might be an empty message send */
		if (this.currentElement != null && this.lastIgnoredToken == TokenNamenew){ // was an allocation expression
			super.consumeConstructorHeaderName();
		} else {
			super.consumeConstructorHeaderName();
			if (this.pendingAnnotation != null) {
				this.pendingAnnotation.potentialAnnotatedNode = this.astStack[this.astPtr];
				this.pendingAnnotation = null;
			}
		}
		if (this.sourceEnds != null && this.astPtr > currentAstPtr) { // if ast node was pushed on the ast stack
			this.sourceEnds.put(this.astStack[this.astPtr], selectorSourceEnd);
		}
		return;
	}

	/* force to start recovering in order to get fake field behavior */
	if (this.currentElement == null){
		this.hasReportedError = true; // do not report any error
	}
	pushOnGenericsIdentifiersLengthStack(this.identifierLengthStack[this.identifierLengthPtr]);
	pushOnGenericsLengthStack(0); // handle type arguments
	this.restartRecovery = true;
}
@Override
protected void consumeConstructorHeaderNameWithTypeParameters() {
	long selectorSourcePositions = this.identifierPositionStack[this.identifierPtr];
	int selectorSourceEnd = (int) selectorSourcePositions;
	int currentAstPtr = this.astPtr;
	if (this.currentElement != null && this.lastIgnoredToken == TokenNamenew){ // was an allocation expression
		super.consumeConstructorHeaderNameWithTypeParameters();
	} else {
		super.consumeConstructorHeaderNameWithTypeParameters();
		if (this.pendingAnnotation != null) {
			this.pendingAnnotation.potentialAnnotatedNode = this.astStack[this.astPtr];
			this.pendingAnnotation = null;
		}
	}
	if (this.sourceEnds != null && this.astPtr > currentAstPtr) { // if ast node was pushed on the ast stack
		this.sourceEnds.put(this.astStack[this.astPtr], selectorSourceEnd);
	}
}
@Override
protected void consumeDefaultLabel() {
	super.consumeDefaultLabel();
	if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_SWITCH_LABEL) {
		popElement(K_SWITCH_LABEL);
	}
	pushOnElementStack(K_SWITCH_LABEL, DEFAULT);
}
@Override
protected void consumeDimWithOrWithOutExpr() {
	// DimWithOrWithOutExpr ::= '[' ']'
	pushOnExpressionStack(null);
}
@Override
protected void consumeEmptyStatement() {
	boolean shouldAttachParent = (this.assistNodeParent instanceof MessageSend) ||
			(this.assistNodeParent instanceof ParameterizedSingleTypeReference) ||
			(this.assistNodeParent instanceof LocalDeclaration);
	ASTNode nodeToAttach = shouldAttachParent ? this.assistNodeParent : this.assistNode;
	if (this.shouldStackAssistNode && nodeToAttach != null) {
		for (int ptr = this.astPtr; ptr >= 0; ptr--) {
			if (new CompletionNodeDetector(nodeToAttach, this.astStack[ptr]).containsCompletionNode()) {
				// likely synthetic ';' detected after the nodeToAttach was already attached => skip
				this.astLengthStack[++this.astLengthPtr] = 0; // start an empty list instead of the unnecessary empty statement
				this.shouldStackAssistNode = false;
				return;
			}
		}
	}
	super.consumeEmptyStatement();
	/* Sneak in the assist node. The reason we can't do that when we see the assist node is that
	   we don't know whether it is the first or subsequent statement in a block to be able to
	   decide whether to call contactNodeLists. See Parser.consumeBlockStatement(s)
	*/
	if (this.shouldStackAssistNode && this.assistNode != null) {
		this.astStack[this.astPtr] = nodeToAttach;
	}
	this.shouldStackAssistNode = false;
}
@Override
protected void consumeBlockStatement() {
	super.consumeBlockStatement();
	if (this.shouldStackAssistNode && this.assistNode != null) {
		Statement stmt = (Statement) this.astStack[this.astPtr];
		if (stmt.sourceStart <= this.assistNode.sourceStart && stmt.sourceEnd >= this.assistNode.sourceEnd)
			this.shouldStackAssistNode = false;
	}
	if (this.currentElement != null
			&& this.scanner.currentPosition > this.cursorLocation
			&& this.astLengthStack[this.astLengthPtr] == 1 // first statement in the block?
			&& topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BLOCK_DELIMITER)
	{
		// if the new statement is the 1st child of a not-yet materialized control statement, synthesize that control statement (if) now:
		Statement stmt = (Statement) this.astStack[this.astPtr];
		Statement newStatement = buildMoreCompletionEnclosingContext(stmt);
		if (newStatement != this.astStack[this.astPtr]) {
			RecoveredElement newElement = this.currentElement.add(newStatement, 0);
			this.currentElement = newElement;
		}
	}
}
@Override
protected void consumeEnhancedForStatement() {
	super.consumeEnhancedForStatement();

	if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_CONTROL_STATEMENT_DELIMITER) {
		popElement(K_CONTROL_STATEMENT_DELIMITER);
	}
}
@Override
protected void consumeEnhancedForStatementHeader(){
	this.consumedEnhancedFor = true;
	super.consumeEnhancedForStatementHeader();

}

@Override
protected void consumeEnhancedForStatementHeaderInit(boolean hasModifiers) {
	super.consumeEnhancedForStatementHeaderInit(hasModifiers);
	this.hasUnusedModifiers = false;
	if (this.pendingAnnotation != null) {
		this.pendingAnnotation.potentialAnnotatedNode = this.astStack[this.astPtr];
		this.pendingAnnotation = null;
	}
}
@Override
protected void consumeEnterAnonymousClassBody(boolean qualified) {
	popElement(K_SELECTOR_QUALIFIER);
	popElement(K_SELECTOR_INVOCATION_TYPE);
	super.consumeEnterAnonymousClassBody(qualified);
}
@Override
protected void consumeEnterVariable() {
	this.identifierPtr--;
	this.identifierLengthPtr--;

	boolean isLocalDeclaration = this.nestedMethod[this.nestedType] != 0;
	int variableIndex = this.variablesCounter[this.nestedType];

	this.hasUnusedModifiers = false;

	if(isLocalDeclaration || indexOfAssistIdentifier() < 0 || variableIndex != 0) {
		this.identifierPtr++;
		this.identifierLengthPtr++;

		if (this.pendingAnnotation != null &&
				this.assistNode != null &&
				this.currentElement != null &&
				this.currentElement instanceof RecoveredMethod &&
				!this.currentElement.foundOpeningBrace &&
				((RecoveredMethod)this.currentElement).methodDeclaration.declarationSourceEnd == 0) {
			// this is a method parameter
			super.consumeEnterVariable();
			this.pendingAnnotation.potentialAnnotatedNode = this.astStack[this.astPtr];
			this.pendingAnnotation.isParameter = true;
			this.pendingAnnotation = null;

		} else {
			super.consumeEnterVariable();
			if (this.pendingAnnotation != null) {
				this.pendingAnnotation.potentialAnnotatedNode = this.astStack[this.astPtr];
				this.pendingAnnotation = null;
			}
		}
	} else {
		this.restartRecovery = true;

		// recovery
		if (this.currentElement != null) {
			if(!checkKeywordAndRestrictedIdentifiers() && !(this.currentElement instanceof RecoveredUnit && ((RecoveredUnit)this.currentElement).typeCount == 0)) {
				int nameSourceStart = (int)(this.identifierPositionStack[this.identifierPtr] >>> 32);
				this.intPtr--;
				TypeReference type = getTypeReference(this.intStack[this.intPtr--]);
				this.intPtr--;

				if (!(this.currentElement instanceof RecoveredType)
					&& (this.currentToken == TokenNameDOT
						|| (Util.getLineNumber(type.sourceStart, this.scanner.lineEnds, 0, this.scanner.linePtr)
								!= Util.getLineNumber(nameSourceStart, this.scanner.lineEnds, 0, this.scanner.linePtr)))){
					this.lastCheckPoint = nameSourceStart;
					this.restartRecovery = true;
					return;
				}

				FieldDeclaration completionFieldDecl = new CompletionOnFieldType(type, false);
				// consume annotations
				int length;
				if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
					System.arraycopy(
						this.expressionStack,
						(this.expressionPtr -= length) + 1,
						completionFieldDecl.annotations = new Annotation[length],
						0,
						length);
				}
				completionFieldDecl.modifiers = this.intStack[this.intPtr--];
				this.assistNode = completionFieldDecl;
				this.lastCheckPoint = type.sourceEnd + 1;
				this.currentElement = this.currentElement.add(completionFieldDecl, 0);
				this.lastIgnoredToken = -1;
			}
		}
	}
}
@Override
protected void consumeEnumConstantHeaderName() {
	if (this.currentElement != null) {
		if (!(this.currentElement instanceof RecoveredType
					|| (this.currentElement instanceof RecoveredField && ((RecoveredField)this.currentElement).fieldDeclaration.type == null))
				|| (this.lastIgnoredToken == TokenNameDOT)) {
			super.consumeEnumConstantHeaderName();
			return;
		}
	}
	super.consumeEnumConstantHeaderName();
	if (this.pendingAnnotation != null) {
		this.pendingAnnotation.potentialAnnotatedNode = this.astStack[this.astPtr];
		this.pendingAnnotation = null;
	}
}
@Override
protected void consumeEnumConstantNoClassBody() {
	super.consumeEnumConstantNoClassBody();
	if ((this.currentToken == TokenNameCOMMA || this.currentToken == TokenNameSEMICOLON)
			&& this.astStack[this.astPtr] instanceof FieldDeclaration) {
		if (this.sourceEnds != null) {
			this.sourceEnds.put(this.astStack[this.astPtr], this.scanner.currentPosition - 1);
		}
	}
}
@Override
protected void consumeEnumConstantWithClassBody() {
	super.consumeEnumConstantWithClassBody();
	if ((this.currentToken == TokenNameCOMMA || this.currentToken == TokenNameSEMICOLON)
			&& this.astStack[this.astPtr] instanceof FieldDeclaration) {
		if (this.sourceEnds != null) {
			this.sourceEnds.put(this.astStack[this.astPtr], this.scanner.currentPosition - 1);
		}
	}
}
@Override
protected void consumeEnumHeaderName() {
	super.consumeEnumHeaderName();
	this.hasUnusedModifiers = false;
	if (this.pendingAnnotation != null) {
		this.pendingAnnotation.potentialAnnotatedNode = this.astStack[this.astPtr];
		this.pendingAnnotation = null;
	}
}
@Override
protected void consumeEnumHeaderNameWithTypeParameters() {
	super.consumeEnumHeaderNameWithTypeParameters();
	if (this.pendingAnnotation != null) {
		this.pendingAnnotation.potentialAnnotatedNode = this.astStack[this.astPtr];
		this.pendingAnnotation = null;
	}
}
@Override
protected void consumeEqualityExpression(int op) {
	super.consumeEqualityExpression(op);
	popElement(K_BINARY_OPERATOR);

	BinaryExpression exp = (BinaryExpression) this.expressionStack[this.expressionPtr];
	if(this.assistNode != null && exp.right == this.assistNode) {
		this.assistNodeParent = exp;
	}
}
@Override
protected void consumeEqualityExpressionWithName(int op) {
	super.consumeEqualityExpressionWithName(op);
	popElement(K_BINARY_OPERATOR);

	BinaryExpression exp = (BinaryExpression) this.expressionStack[this.expressionPtr];
	if(this.assistNode != null && exp.right == this.assistNode) {
		this.assistNodeParent = exp;
	}
}
@Override
protected void consumeExitVariableWithInitialization() {
	super.consumeExitVariableWithInitialization();
	if ((this.currentToken == TokenNameCOMMA || this.currentToken == TokenNameSEMICOLON)
			&& this.astStack[this.astPtr] instanceof FieldDeclaration) {
		if (this.sourceEnds != null) {
			this.sourceEnds.put(this.astStack[this.astPtr], this.scanner.currentPosition - 1);
		}
	}

	// does not keep the initialization if completion is not inside
	AbstractVariableDeclaration variable = (AbstractVariableDeclaration) this.astStack[this.astPtr];
	if (this.cursorLocation + 1 < variable.initialization.sourceStart ||
		this.cursorLocation > variable.initialization.sourceEnd) {
		if (!variable.type.isTypeNameVar(null)) {
			if (! (variable instanceof LocalDeclaration && ((LocalDeclaration)variable).isTypeNameVar(this.compilationUnit.scope))) {
				variable.initialization = null;
			}
		}
	} else if (this.assistNode != null && this.assistNode == variable.initialization) {
			this.assistNodeParent = variable;
	}
	if (triggerRecoveryUponLambdaClosure(variable, false)) {
		if (this.currentElement != null) {
			this.restartRecovery = true;
		}
	}
}
@Override
protected void consumeExitVariableWithoutInitialization() {
	// ExitVariableWithoutInitialization ::= $empty
	// do nothing by default
	super.consumeExitVariableWithoutInitialization();
	if ((this.currentToken == TokenNameCOMMA || this.currentToken == TokenNameSEMICOLON)
			&& this.astStack[this.astPtr] instanceof FieldDeclaration) {
		if (this.sourceEnds != null) {
			this.sourceEnds.put(this.astStack[this.astPtr], this.scanner.currentPosition - 1);
		}
	}
}
@Override
protected void consumeExplicitConstructorInvocation(int flag, int recFlag) {
	popElement(K_SELECTOR_QUALIFIER);
	popElement(K_SELECTOR_INVOCATION_TYPE);
	super.consumeExplicitConstructorInvocation(flag, recFlag);
}
/*
 * Copy of code from superclass with the following change:
 * If the cursor location is on the field access, then create a
 * CompletionOnMemberAccess instead.
 */
@Override
protected void consumeFieldAccess(boolean isSuperAccess) {
	// FieldAccess ::= Primary '.' 'Identifier'
	// FieldAccess ::= 'super' '.' 'Identifier'

	// potential receiver is being poped, so reset potential receiver
	this.invocationType = NO_RECEIVER;
	this.qualifier = -1;

	if (this.indexOfAssistIdentifier() < 0) {
		super.consumeFieldAccess(isSuperAccess);
	} else {
		pushCompletionOnMemberAccessOnExpressionStack(isSuperAccess);
	}
}
@Override
protected void consumeForceNoDiet() {
	super.consumeForceNoDiet();
	if (isInsideMethod()) {
		pushOnElementStack(K_LOCAL_INITIALIZER_DELIMITER);
	}
}
@Override
protected void consumeFormalParameter(boolean isVarArgs) {

	this.invocationType = NO_RECEIVER;
	this.qualifier = -1;

	if (this.indexOfAssistIdentifier() < 0) {
		super.consumeFormalParameter(isVarArgs);
		if (this.pendingAnnotation != null) {
			this.pendingAnnotation.potentialAnnotatedNode = this.astStack[this.astPtr];
			this.pendingAnnotation = null;
		}
	} else {
		boolean isReceiver = this.intStack[this.intPtr--] == 0;
	    if (isReceiver) {
	    	this.expressionPtr--;
	    	this.expressionLengthPtr --;
	    }
		this.identifierLengthPtr--;
		char[] identifierName = this.identifierStack[this.identifierPtr];
		long namePositions = this.identifierPositionStack[this.identifierPtr--];
		int extendedDimensions = this.intStack[this.intPtr--];
		Annotation [][] annotationsOnExtendedDimensions = extendedDimensions == 0 ? null : getAnnotationsOnDimensions(extendedDimensions);
		Annotation [] varArgsAnnotations = null;
		int length;
		int endOfEllipsis = 0;
		if (isVarArgs) {
			endOfEllipsis = this.intStack[this.intPtr--];
			if ((length = this.typeAnnotationLengthStack[this.typeAnnotationLengthPtr--]) != 0) {
				System.arraycopy(
					this.typeAnnotationStack,
					(this.typeAnnotationPtr -= length) + 1,
					varArgsAnnotations = new Annotation[length],
					0,
					length);
			}
		}
		int firstDimensions = this.intStack[this.intPtr--];
		TypeReference type = getTypeReference(firstDimensions);

		if (isVarArgs || extendedDimensions != 0) {
			if (isVarArgs) {
				type = augmentTypeWithAdditionalDimensions(type, 1, varArgsAnnotations != null ? new Annotation[][] { varArgsAnnotations } : null, true);
			}
			if (extendedDimensions != 0) { // combination illegal.
				type = augmentTypeWithAdditionalDimensions(type, extendedDimensions, annotationsOnExtendedDimensions, false);
			}
			type.sourceEnd = type.isParameterizedTypeReference() ? this.endStatementPosition : this.endPosition;
		}
		if (isVarArgs) {
			if (extendedDimensions == 0) {
				type.sourceEnd = endOfEllipsis;
			}
			type.bits |= ASTNode.IsVarArgs; // set isVarArgs
		}
		this.intPtr -= 2;
		CompletionOnArgumentName arg =
			new CompletionOnArgumentName(
				identifierName,
				namePositions,
				type,
				this.intStack[this.intPtr + 1] & ~ClassFileConstants.AccDeprecated); // modifiers
		// consume annotations
		if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
			System.arraycopy(
				this.expressionStack,
				(this.expressionPtr -= length) + 1,
				arg.annotations = new Annotation[length],
				0,
				length);
			RecoveredType currentRecoveryType = this.currentRecoveryType();
			if (currentRecoveryType != null)
				currentRecoveryType.annotationsConsumed(arg.annotations);
		}

		arg.isCatchArgument = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BETWEEN_CATCH_AND_RIGHT_PAREN;
		pushOnAstStack(arg);

		this.assistNode = arg;
		this.lastCheckPoint = (int) namePositions;
		this.isOrphanCompletionNode = true;

		/* if incomplete method header, listLength counter will not have been reset,
			indicating that some arguments are available on the stack */
		this.listLength++;
	}
}
@Override
protected void consumeGenericTypeWithDiamond() {
	super.consumeGenericTypeWithDiamond();
	// we need to pop the <> of the diamond from the stack.
	// This is not required in usual case when the type argument isn't elided
	// since the < and > get popped while parsing the type argument.
	popElement(K_BINARY_OPERATOR); // pop >
	popElement(K_BINARY_OPERATOR); // pop <
}
@Override
protected void consumeStatementFor() {
	super.consumeStatementFor();

	if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_CONTROL_STATEMENT_DELIMITER) {
		popElement(K_CONTROL_STATEMENT_DELIMITER);
	}
}
@Override
protected void consumeStatementIfNoElse() {
	super.consumeStatementIfNoElse();

	if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_CONTROL_STATEMENT_DELIMITER) {
		popElement(K_CONTROL_STATEMENT_DELIMITER);
	}
}
@Override
protected void consumeStatementIfWithElse() {
	super.consumeStatementIfWithElse();

	if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_CONTROL_STATEMENT_DELIMITER) {
		popElement(K_CONTROL_STATEMENT_DELIMITER);
	}
}
@Override
protected void consumeInsideCastExpression() {
	TypeReference[] bounds = null;
	int additionalBoundsLength = this.genericsLengthStack[this.genericsLengthPtr--];
	if (additionalBoundsLength > 0) {
		bounds = new TypeReference[additionalBoundsLength + 1];
		this.genericsPtr -= additionalBoundsLength;
		System.arraycopy(this.genericsStack, this.genericsPtr + 1, bounds, 1, additionalBoundsLength);
	}

	int end = this.intStack[this.intPtr--];
	boolean isParameterized =(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_PARAMETERIZED_CAST);
	if(isParameterized) {
		popElement(K_PARAMETERIZED_CAST);

		if(this.identifierLengthStack[this.identifierLengthPtr] > 0) {
			pushOnGenericsIdentifiersLengthStack(this.identifierLengthStack[this.identifierLengthPtr]);
		}
	} else {
		if(this.identifierLengthStack[this.identifierLengthPtr] > 0) {
			pushOnGenericsIdentifiersLengthStack(this.identifierLengthStack[this.identifierLengthPtr]);
			pushOnGenericsLengthStack(0);
		}
	}
	Expression castType = getTypeReference(this.intStack[this.intPtr--]);
	if (additionalBoundsLength > 0) {
		bounds[0] = (TypeReference) castType;
		castType = createIntersectionCastTypeReference(bounds);
	}
	if(isParameterized) {
		this.intPtr--;
	}
	castType.sourceEnd = end - 1;
	castType.sourceStart = this.intStack[this.intPtr--] + 1;
	pushOnExpressionStack(castType);

	pushOnElementStack(K_CAST_STATEMENT);
}
@Override
protected void consumeInsideCastExpressionLL1() {
	if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_PARAMETERIZED_CAST) {
		popElement(K_PARAMETERIZED_CAST);
	}
	if (!this.record) {
		super.consumeInsideCastExpressionLL1();
	} else {
		boolean temp = this.skipRecord;
		try {
			this.skipRecord = true;
			super.consumeInsideCastExpressionLL1();
			if (this.record) {
				Expression typeReference = this.expressionStack[this.expressionPtr];
				if (!isAlreadyPotentialName(typeReference.sourceStart)) {
					addPotentialName(null, typeReference.sourceStart, typeReference.sourceEnd);
				}
			}
		} finally {
			this.skipRecord = temp;
		}
	}
	pushOnElementStack(K_CAST_STATEMENT);
}
@Override
protected void consumeInsideCastExpressionLL1WithBounds() {
	if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_PARAMETERIZED_CAST) {
		popElement(K_PARAMETERIZED_CAST);
	}
	if (!this.record) {
		super.consumeInsideCastExpressionLL1WithBounds();
	} else {
		boolean temp = this.skipRecord;
		try {
			this.skipRecord = true;
			super.consumeInsideCastExpressionLL1WithBounds();
			if (this.record) {
				int length =  this.expressionLengthStack[this.expressionLengthPtr];
				for (int i = 0; i < length; i++) {
					Expression typeReference = this.expressionStack[this.expressionPtr - length + i + 1];
					if (!isAlreadyPotentialName(typeReference.sourceStart)) {
						addPotentialName(null, typeReference.sourceStart, typeReference.sourceEnd);
					}
				}
			}
		} finally {
			this.skipRecord = temp;
		}
	}
	pushOnElementStack(K_CAST_STATEMENT);
}
@Override
protected void consumeInsideCastExpressionWithQualifiedGenerics() {
	popElement(K_PARAMETERIZED_CAST);

	Expression castType;
	int end = this.intStack[this.intPtr--];

	int dim = this.intStack[this.intPtr--];
	Annotation[][] annotationsOnDimensions = dim == 0 ? null : getAnnotationsOnDimensions(dim);

	TypeReference[] bounds = null;
	int additionalBoundsLength = this.genericsLengthStack[this.genericsLengthPtr--];
	if (additionalBoundsLength > 0) {
		bounds = new TypeReference[additionalBoundsLength + 1];
		this.genericsPtr -= additionalBoundsLength;
		System.arraycopy(this.genericsStack, this.genericsPtr + 1, bounds, 1, additionalBoundsLength);
	}

	TypeReference rightSide = getTypeReference(0);

	castType = computeQualifiedGenericsFromRightSide(rightSide, dim, annotationsOnDimensions);
	if (additionalBoundsLength > 0) {
		bounds[0] = (TypeReference) castType;
		castType = createIntersectionCastTypeReference(bounds);
	}
	this.intPtr--;
	castType.sourceEnd = end - 1;
	castType.sourceStart = this.intStack[this.intPtr--] + 1;
	pushOnExpressionStack(castType);

	pushOnElementStack(K_CAST_STATEMENT);
}
@Override
protected void consumeInstanceOfExpression() {
	super.consumeInstanceOfExpression();
	popElement(K_BINARY_OPERATOR);
	// to handle https://bugs.eclipse.org/bugs/show_bug.cgi?id=261534
	if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BETWEEN_IF_AND_RIGHT_PAREN) {
		pushOnElementStack(K_BETWEEN_INSTANCEOF_AND_RPAREN, IF, this.expressionStack[this.expressionPtr]);
	}

	InstanceOfExpression exp = (InstanceOfExpression) this.expressionStack[this.expressionPtr];
	if(this.assistNode != null && exp.type == this.assistNode) {
		this.assistNodeParent = exp;
	}
}
@Override
protected void consumeInstanceOfExpressionWithName() {
	super.consumeInstanceOfExpressionWithName();
	popElement(K_BINARY_OPERATOR);

	InstanceOfExpression exp = (InstanceOfExpression) this.expressionStack[this.expressionPtr];
	if(this.assistNode != null && exp.type == this.assistNode) {
		this.assistNodeParent = exp;
	}
}
@Override
protected void consumeInterfaceHeaderName1() {
	super.consumeInterfaceHeaderName1();
	this.hasUnusedModifiers = false;
	if (this.pendingAnnotation != null) {
		this.pendingAnnotation.potentialAnnotatedNode = this.astStack[this.astPtr];
		this.pendingAnnotation = null;
	}
	classHeaderExtendsOrImplements(true, false);
}
@Override
protected void consumeInterfaceHeaderExtends() {
	super.consumeInterfaceHeaderExtends();
	popElement(K_EXTENDS_KEYWORD);
}
@Override
protected void consumeInterfaceType() {
	pushOnElementStack(K_NEXT_TYPEREF_IS_INTERFACE);
	super.consumeInterfaceType();
	popElement(K_NEXT_TYPEREF_IS_INTERFACE);
}
@Override
protected void consumeMethodInvocationName() {
	popElement(K_SELECTOR_QUALIFIER);
	popElement(K_SELECTOR_INVOCATION_TYPE);
	super.consumeMethodInvocationName();
	adjustPositionsOfMessageSendOnStack();
}
@Override
protected void consumeMethodInvocationNameWithTypeArguments() {
	popElement(K_SELECTOR_QUALIFIER);
	popElement(K_SELECTOR_INVOCATION_TYPE);
	super.consumeMethodInvocationNameWithTypeArguments();
	adjustPositionsOfMessageSendOnStack();
}
@Override
protected void consumeMethodInvocationPrimary() {
	popElement(K_SELECTOR_QUALIFIER);
	popElement(K_SELECTOR_INVOCATION_TYPE);
	super.consumeMethodInvocationPrimary();
	adjustPositionsOfMessageSendOnStack();
}
@Override
protected void consumeMethodInvocationPrimaryWithTypeArguments() {
	popElement(K_SELECTOR_QUALIFIER);
	popElement(K_SELECTOR_INVOCATION_TYPE);
	super.consumeMethodInvocationPrimaryWithTypeArguments();
	adjustPositionsOfMessageSendOnStack();
}
@Override
protected void consumeMethodInvocationSuper() {
	popElement(K_SELECTOR_QUALIFIER);
	popElement(K_SELECTOR_INVOCATION_TYPE);
	super.consumeMethodInvocationSuper();
	adjustPositionsOfMessageSendOnStack();
}
protected void adjustPositionsOfMessageSendOnStack() {
	MessageSend messageSend = (MessageSend)this.expressionStack[this.expressionPtr];
	if (messageSend instanceof CompletionOnMessageSendName) {
		CompletionScanner completionScanner = (CompletionScanner)this.scanner;
		messageSend.sourceStart = completionScanner.completedIdentifierStart;
		messageSend.sourceEnd = completionScanner.completedIdentifierEnd;
	} else if (messageSend instanceof CompletionOnMessageSend) {
		messageSend.sourceStart = messageSend.nameSourceStart();
	}
}
@Override
protected void consumeMethodInvocationSuperWithTypeArguments() {
	popElement(K_SELECTOR_QUALIFIER);
	popElement(K_SELECTOR_INVOCATION_TYPE);
	super.consumeMethodInvocationSuperWithTypeArguments();
}
@Override
protected void consumeMethodHeaderName(boolean isAnnotationMethod) {
	if(this.indexOfAssistIdentifier() < 0) {
		this.identifierPtr--;
		this.identifierLengthPtr--;
		if(this.indexOfAssistIdentifier() != 0 ||
			this.identifierLengthStack[this.identifierLengthPtr] != this.genericsIdentifiersLengthStack[this.genericsIdentifiersLengthPtr]) {
			this.identifierPtr++;
			this.identifierLengthPtr++;
			long selectorSourcePositions = this.identifierPositionStack[this.identifierPtr];
			int selectorSourceEnd = (int) selectorSourcePositions;
			int currentAstPtr = this.astPtr;
			super.consumeMethodHeaderName(isAnnotationMethod);
			if (this.sourceEnds != null && this.astPtr > currentAstPtr) { // if ast node was pushed on the ast stack
				this.sourceEnds.put(this.astStack[this.astPtr], selectorSourceEnd);
			}
			if (this.pendingAnnotation != null) {
				this.pendingAnnotation.potentialAnnotatedNode = this.astStack[this.astPtr];
				this.pendingAnnotation = null;
			}
		} else {
			this.restartRecovery = true;

			// recovery
			if (this.currentElement != null) {
				//name
				char[] selector = this.identifierStack[this.identifierPtr + 1];
				long selectorSource = this.identifierPositionStack[this.identifierPtr + 1];

				//type
				TypeReference type = getTypeReference(this.intStack[this.intPtr--]);
				((CompletionOnSingleTypeReference)type).isCompletionNode = false;
				//modifiers
				int declarationSourceStart = this.intStack[this.intPtr--];
				int mod = this.intStack[this.intPtr--];

				if(Util.getLineNumber(type.sourceStart, this.scanner.lineEnds, 0, this.scanner.linePtr)
						!= Util.getLineNumber((int) (selectorSource >>> 32), this.scanner.lineEnds, 0, this.scanner.linePtr)) {
					FieldDeclaration completionFieldDecl = new CompletionOnFieldType(type, false);
					// consume annotations
					int length;
					if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
						System.arraycopy(
							this.expressionStack,
							(this.expressionPtr -= length) + 1,
							completionFieldDecl.annotations = new Annotation[length],
							0,
							length);
					}
					completionFieldDecl.modifiers = mod;
					this.assistNode = completionFieldDecl;
					this.lastCheckPoint = type.sourceEnd + 1;
					this.currentElement = this.currentElement.add(completionFieldDecl, 0);
					this.lastIgnoredToken = -1;
				} else {
					CompletionOnMethodReturnType md = new CompletionOnMethodReturnType(type, this.compilationUnit.compilationResult);
					// consume annotations
					int length;
					if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
						System.arraycopy(
							this.expressionStack,
							(this.expressionPtr -= length) + 1,
							md.annotations = new Annotation[length],
							0,
							length);
					}
					md.selector = selector;
					md.declarationSourceStart = declarationSourceStart;
					md.modifiers = mod;
					md.bodyStart = this.lParenPos+1;
					this.listLength = 0; // initialize listLength before reading parameters/throws
					this.assistNode = md;
					this.lastCheckPoint = md.bodyStart;
					this.currentElement = this.currentElement.add(md, 0);
					this.lastIgnoredToken = -1;
					// javadoc
					md.javadoc = this.javadoc;
					this.javadoc = null;
				}
			}
		}
	} else {
		// MethodHeaderName ::= Modifiersopt Type 'Identifier' '('
		CompletionOnMethodName md = new CompletionOnMethodName(this.compilationUnit.compilationResult);

		//name
		md.selector = this.identifierStack[this.identifierPtr];
		long selectorSource = this.identifierPositionStack[this.identifierPtr--];
		this.identifierLengthPtr--;
		//type
		md.returnType = getTypeReference(this.intStack[this.intPtr--]);
		md.bits |= (md.returnType.bits & ASTNode.HasTypeAnnotations);
		//modifiers
		md.declarationSourceStart = this.intStack[this.intPtr--];
		md.modifiers = this.intStack[this.intPtr--];
		// consume annotations
		int length;
		if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
			System.arraycopy(
				this.expressionStack,
				(this.expressionPtr -= length) + 1,
				md.annotations = new Annotation[length],
				0,
				length);
		}
		// javadoc
		md.javadoc = this.javadoc;
		this.javadoc = null;

		//highlight starts at selector start
		md.sourceStart = (int) (selectorSource >>> 32);
		md.selectorEnd = (int) selectorSource;
		pushOnAstStack(md);
		md.sourceEnd = this.lParenPos;
		md.bodyStart = this.lParenPos+1;
		this.listLength = 0; // initialize listLength before reading parameters/throws

		this.assistNode = md;
		this.lastCheckPoint = md.sourceEnd;
		// recovery
		if (this.currentElement != null){
			if (this.currentElement instanceof RecoveredType
				//|| md.modifiers != 0
				|| (Util.getLineNumber(md.returnType.sourceStart, this.scanner.lineEnds, 0, this.scanner.linePtr)
						== Util.getLineNumber(md.sourceStart, this.scanner.lineEnds, 0, this.scanner.linePtr))){
				this.lastCheckPoint = md.bodyStart;
				this.currentElement = this.currentElement.add(md, 0);
				this.lastIgnoredToken = -1;
			} else {
				this.lastCheckPoint = md.sourceStart;
				this.restartRecovery = true;
			}
		}
	}
}
@Override
protected void consumeMethodHeaderNameWithTypeParameters( boolean isAnnotationMethod) {
	long selectorSourcePositions = this.identifierPositionStack[this.identifierPtr];
	int selectorSourceEnd = (int) selectorSourcePositions;
	int currentAstPtr = this.astPtr;
	super.consumeMethodHeaderNameWithTypeParameters(isAnnotationMethod);
	if (this.sourceEnds != null && this.astPtr > currentAstPtr) {// if ast node was pushed on the ast stack
		this.sourceEnds.put(this.astStack[this.astPtr], selectorSourceEnd);
	}
	if (this.pendingAnnotation != null) {
		this.pendingAnnotation.potentialAnnotatedNode = this.astStack[this.astPtr];
		this.pendingAnnotation = null;
	}
}
@Override
protected void consumeMethodHeaderRightParen() {
	super.consumeMethodHeaderRightParen();

	if (this.currentElement != null
		&& this.currentToken == TokenNameIdentifier
		&& this.cursorLocation+1 >= this.scanner.startPosition
		&& this.cursorLocation < this.scanner.currentPosition){
		this.pushIdentifier();

		int index = -1;
		/* check if current awaiting identifier is the completion identifier */
		if ((index = this.indexOfAssistIdentifier()) > -1) {
			int ptr = this.identifierPtr - this.identifierLengthStack[this.identifierLengthPtr] + index + 1;
			if (this.currentElement instanceof RecoveredMethod){
				RecoveredMethod recoveredMethod = (RecoveredMethod)this.currentElement;
				/* filter out cases where scanner is still inside type header */
				if (!recoveredMethod.foundOpeningBrace) {
					AbstractMethodDeclaration method = recoveredMethod.methodDeclaration;
					if(method.thrownExceptions == null) {
						CompletionOnKeyword1 completionOnKeyword = new CompletionOnKeyword1(
							this.identifierStack[ptr],
							this.identifierPositionStack[ptr],
							Keywords.THROWS);
						method.thrownExceptions = new TypeReference[]{completionOnKeyword};
						recoveredMethod.foundOpeningBrace = true;
						this.assistNode = completionOnKeyword;
						this.lastCheckPoint = completionOnKeyword.sourceEnd + 1;
					}
				}
			}
		}
	}
}
@Override
protected void consumeMethodHeaderExtendedDims() {
	super.consumeMethodHeaderExtendedDims();

	if (this.currentElement != null
		&& this.currentToken == TokenNameIdentifier
		&& this.cursorLocation+1 >= this.scanner.startPosition
		&& this.cursorLocation < this.scanner.currentPosition){
		this.pushIdentifier();

		int index = -1;
		/* check if current awaiting identifier is the completion identifier */
		if ((index = this.indexOfAssistIdentifier()) > -1) {
			int ptr = this.identifierPtr - this.identifierLengthStack[this.identifierLengthPtr] + index + 1;
			RecoveredMethod recoveredMethod = (RecoveredMethod)this.currentElement;
			/* filter out cases where scanner is still inside type header */
			if (!recoveredMethod.foundOpeningBrace) {
				AbstractMethodDeclaration method = recoveredMethod.methodDeclaration;
				if(method.thrownExceptions == null) {
					CompletionOnKeyword1 completionOnKeyword = new CompletionOnKeyword1(
						this.identifierStack[ptr],
						this.identifierPositionStack[ptr],
						Keywords.THROWS);
					method.thrownExceptions = new TypeReference[]{completionOnKeyword};
					recoveredMethod.foundOpeningBrace = true;
					this.assistNode = completionOnKeyword;
					this.lastCheckPoint = completionOnKeyword.sourceEnd + 1;
				}
			}
		}
	}
}
@Override
protected void consumeAnnotationAsModifier() {
	super.consumeAnnotationAsModifier();

	if (isInsideMethod()) {
		this.hasUnusedModifiers = true;
	}
}
@Override
protected void consumeAdditionalBound() {
	super.consumeAdditionalBound();
	ASTNode node = this.genericsStack[this.genericsPtr];
	if (node instanceof CompletionOnSingleTypeReference) {
		((CompletionOnSingleTypeReference) node).setKind(CompletionOnQualifiedTypeReference.K_INTERFACE);
	} else if (node instanceof CompletionOnQualifiedTypeReference) {
		((CompletionOnQualifiedTypeReference) node).setKind(CompletionOnQualifiedTypeReference.K_INTERFACE);
	}
}
@Override
protected void consumeAdditionalBound1() {
	super.consumeAdditionalBound1();
	ASTNode node = this.genericsStack[this.genericsPtr];
	if (node instanceof CompletionOnSingleTypeReference) {
		((CompletionOnSingleTypeReference) node).setKind(CompletionOnQualifiedTypeReference.K_INTERFACE);
	} else if (node instanceof CompletionOnQualifiedTypeReference) {
		((CompletionOnQualifiedTypeReference) node).setKind(CompletionOnQualifiedTypeReference.K_INTERFACE);
	}
}
@Override
protected void consumeAnnotationName() {
	int index;

	if ((index = this.indexOfAssistIdentifier()) < 0) {
		super.consumeAnnotationName();
		this.pushOnElementStack(K_BETWEEN_ANNOTATION_NAME_AND_RPAREN, LPAREN_NOT_CONSUMED);
		return;
	}

	if (isInImportStatement()) {
		return;
	}
	MarkerAnnotation markerAnnotation = null;
	int length = this.identifierLengthStack[this.identifierLengthPtr];
	TypeReference typeReference;

	/* retrieve identifiers subset and whole positions, the assist node positions
		should include the entire replaced source. */

	char[][] subset = identifierSubSet(index);
	this.identifierLengthPtr--;
	this.identifierPtr -= length;
	long[] positions = new long[length];
	System.arraycopy(
		this.identifierPositionStack,
		this.identifierPtr + 1,
		positions,
		0,
		length);

	/* build specific assist on type reference */

	if (index == 0) {
		/* assist inside first identifier */
		typeReference = createSingleAssistTypeReference(
						assistIdentifier(),
						positions[0]);
	} else {
		/* assist inside subsequent identifier */
		typeReference =	createQualifiedAssistTypeReference(
						subset,
						assistIdentifier(),
						positions);
	}

	markerAnnotation = new CompletionOnMarkerAnnotationName(typeReference, typeReference.sourceStart);
	this.intPtr--;
	markerAnnotation.declarationSourceEnd = markerAnnotation.sourceEnd;
	pushOnExpressionStack(markerAnnotation);

	this.assistNode = markerAnnotation;
	this.isOrphanCompletionNode = true;

	this.lastCheckPoint = markerAnnotation.sourceEnd + 1;

	this.pushOnElementStack(K_BETWEEN_ANNOTATION_NAME_AND_RPAREN, LPAREN_NOT_CONSUMED | ANNOTATION_NAME_COMPLETION);
}
@Override
protected void consumeAnnotationTypeDeclarationHeaderName() {
	super.consumeAnnotationTypeDeclarationHeaderName();
	this.hasUnusedModifiers = false;
	if (this.pendingAnnotation != null) {
		this.pendingAnnotation.potentialAnnotatedNode = this.astStack[this.astPtr];
		this.pendingAnnotation = null;
	}
}
@Override
protected void consumeAnnotationTypeDeclarationHeaderNameWithTypeParameters() {
	super.consumeAnnotationTypeDeclarationHeaderNameWithTypeParameters();
	this.hasUnusedModifiers = false;
	if (this.pendingAnnotation != null) {
		this.pendingAnnotation.potentialAnnotatedNode = this.astStack[this.astPtr];
		this.pendingAnnotation = null;
	}
}
@Override
protected void consumeLabel() {
	super.consumeLabel();
	pushOnLabelStack(this.identifierStack[this.identifierPtr]);
	this.pushOnElementStack(K_LABEL, this.labelPtr);
}
@Override
protected void consumeLambdaExpression() {
	super.consumeLambdaExpression();
	Expression expression = this.expressionStack[this.expressionPtr];
	if (this.assistNode == null || !(this.assistNode.sourceStart >= expression.sourceStart && this.assistNode.sourceEnd <= expression.sourceEnd))
		popElement(K_LAMBDA_EXPRESSION_DELIMITER);
}
@Override
protected Argument typeElidedArgument() {
	long namePositions = this.identifierPositionStack[this.identifierPtr];
	Argument typeElidedArgument = super.typeElidedArgument();
	if (typeElidedArgument.sourceEnd == this.cursorLocation) {
		return new CompletionOnArgumentName(typeElidedArgument, namePositions);
	}
	return typeElidedArgument;
}
@Override
protected void consumeMarkerAnnotation(boolean isTypeAnnotation) {
	if (this.topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BETWEEN_ANNOTATION_NAME_AND_RPAREN &&
			(this.topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) & ANNOTATION_NAME_COMPLETION) != 0 ) {
		popElement(K_BETWEEN_ANNOTATION_NAME_AND_RPAREN);
		this.restartRecovery = true;
	} else {
		popElement(K_BETWEEN_ANNOTATION_NAME_AND_RPAREN);
		super.consumeMarkerAnnotation(isTypeAnnotation);
	}
}
@Override
protected void consumeMemberValuePair() {
	/* check if current awaiting identifier is the completion identifier */
	if (this.indexOfAssistIdentifier() < 0){
		super.consumeMemberValuePair();
		MemberValuePair memberValuePair = (MemberValuePair) this.astStack[this.astPtr];
		if(this.assistNode != null && memberValuePair.value == this.assistNode) {
			this.assistNodeParent = memberValuePair;
		}
		return;
	}

	char[] simpleName = this.identifierStack[this.identifierPtr];
	long position = this.identifierPositionStack[this.identifierPtr--];
	this.identifierLengthPtr--;
	int end = (int) position;
	int start = (int) (position >>> 32);

	this.expressionPtr--;
	this.expressionLengthPtr--;

	CompletionOnMemberValueName memberValueName = new CompletionOnMemberValueName(simpleName,start, end);
	pushOnAstStack(memberValueName);
	this.assistNode = memberValueName;
	this.lastCheckPoint = this.assistNode.sourceEnd + 1;
	this.isOrphanCompletionNode = true;

	this.restartRecovery = true;
}
@Override
protected void consumeMemberValueAsName() {
	if ((indexOfAssistIdentifier()) < 0) {
		super.consumeMemberValueAsName();
	} else {
		super.consumeMemberValueAsName();
		final int topKnownElementKind = this.topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
		if(topKnownElementKind == K_BETWEEN_ANNOTATION_NAME_AND_RPAREN || topKnownElementKind == K_MEMBER_VALUE_ARRAY_INITIALIZER) {
			this.restartRecovery = true;
		}
	}
}
@Override
protected void consumeMethodBody() {
	popElement(K_BLOCK_DELIMITER);
	super.consumeMethodBody();
}
@Override
protected void consumeMethodHeader() {
	super.consumeMethodHeader();
	pushOnElementStack(K_BLOCK_DELIMITER);
}
@Override
protected void consumeMethodDeclaration(boolean isNotAbstract, boolean isDefaultMethod) {
	if (!isNotAbstract) {
		popElement(K_BLOCK_DELIMITER);
	}
	super.consumeMethodDeclaration(isNotAbstract, isDefaultMethod);
}
@Override
protected void consumeModifiers() {
	super.consumeModifiers();
	// save from stack values
	this.lastModifiersStart = this.intStack[this.intPtr];
	this.lastModifiers = 	this.intStack[this.intPtr-1];
}
@Override
protected void consumeModuleHeader() {
	super.consumeModuleHeader();
}
@Override
protected void consumeProvidesInterface() {
	super.consumeProvidesInterface();
	pushOnElementStack(K_AFTER_NAME_IN_PROVIDES_STATEMENT);
}
@Override
protected void consumeProvidesStatement() {
	super.consumeProvidesStatement();
	popElement(K_INSIDE_PROVIDES_STATEMENT);
}
@Override
protected void consumeWithClause() {
	super.consumeWithClause();
	popElement(K_AFTER_WITH_IN_PROVIDES_STATEMENT);
}

@Override
protected void consumeReferenceType() {
	if (this.identifierLengthStack[this.identifierLengthPtr] > 1) { // reducing a qualified name
		// potential receiver is being poped, so reset potential receiver
		this.invocationType = NO_RECEIVER;
		this.qualifier = -1;
	}
	super.consumeReferenceType();
}
@Override
protected void consumeRequiresStatement() {
	super.consumeRequiresStatement();
	popElement(K_INSIDE_REQUIRES_STATEMENT);
}
@Override
protected void consumeRestoreDiet() {
	super.consumeRestoreDiet();
	if (isInsideMethod()) {
		popElement(K_LOCAL_INITIALIZER_DELIMITER);
	}
}
@Override
protected void consumeExportsStatement() {
	super.consumeExportsStatement();
	popElement(K_AFTER_PACKAGE_IN_PACKAGE_VISIBILITY_STATEMENT);
	popElement(K_INSIDE_EXPORTS_STATEMENT);
}
@Override
protected void consumeSinglePkgName() {
	super.consumeSinglePkgName();
	pushOnElementStack(K_AFTER_PACKAGE_IN_PACKAGE_VISIBILITY_STATEMENT);
}
@Override
protected void consumeSingleMemberAnnotation(boolean isTypeAnnotation) {
	if (this.topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BETWEEN_ANNOTATION_NAME_AND_RPAREN &&
			(this.topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) & ANNOTATION_NAME_COMPLETION) != 0 ) {
		popElement(K_BETWEEN_ANNOTATION_NAME_AND_RPAREN);
		this.restartRecovery = true;
	} else {
		popElement(K_BETWEEN_ANNOTATION_NAME_AND_RPAREN);
		super.consumeSingleMemberAnnotation(isTypeAnnotation);
	}
}
@Override
protected void consumeSingleStaticImportDeclarationName() {
	super.consumeSingleStaticImportDeclarationName();
	this.pendingAnnotation = null; // the pending annotation cannot be attached to next nodes
}
@Override
protected void consumeSingleTypeImportDeclarationName() {
	super.consumeSingleTypeImportDeclarationName();
	this.pendingAnnotation = null; // the pending annotation cannot be attached to next nodes
}
@Override
protected void consumeStatementBreakWithLabel() {
	super.consumeStatementBreakWithLabel();
	if (this.record) {
		ASTNode breakStatement = this.astStack[this.astPtr];
		if (!isAlreadyPotentialName(breakStatement.sourceStart)) {
			addPotentialName(null, breakStatement.sourceStart, breakStatement.sourceEnd);
		}
	}

}
@Override
protected void consumeStatementLabel() {
	popElement(K_LABEL);
	super.consumeStatementLabel();
}
@Override
protected void consumeStatementSwitch() {
	super.consumeStatementSwitch();
	if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_SWITCH_LABEL) {
		popElement(K_SWITCH_LABEL);
		popElement(K_BLOCK_DELIMITER);
	}
}
@Override
protected void consumeStatementWhile() {
	super.consumeStatementWhile();
	if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_CONTROL_STATEMENT_DELIMITER) {
		popElement(K_CONTROL_STATEMENT_DELIMITER);
	}
}
@Override
protected void consumeStaticImportOnDemandDeclarationName() {
	super.consumeStaticImportOnDemandDeclarationName();
	this.pendingAnnotation = null; // the pending annotation cannot be attached to next nodes
}
@Override
protected void consumeStaticInitializer() {
	super.consumeStaticInitializer();
	this.pendingAnnotation = null; // the pending annotation cannot be attached to next nodes
}
@Override
protected void consumeNestedMethod() {
	super.consumeNestedMethod();
	if(!(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BLOCK_DELIMITER)) pushOnElementStack(K_BLOCK_DELIMITER);
}
@Override
protected void consumeNormalAnnotation(boolean isTypeAnnotation) {
	if (this.topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BETWEEN_ANNOTATION_NAME_AND_RPAREN &&
			(this.topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) & ANNOTATION_NAME_COMPLETION) != 0 ) {
		popElement(K_BETWEEN_ANNOTATION_NAME_AND_RPAREN);
		this.restartRecovery = true;
	} else {
		popElement(K_BETWEEN_ANNOTATION_NAME_AND_RPAREN);
		if (this.expressionPtr >= 0 && this.expressionStack[this.expressionPtr] instanceof CompletionOnMarkerAnnotationName) {
			Annotation annotation = (Annotation)this.expressionStack[this.expressionPtr];
			if(this.currentElement != null) {
				annotationRecoveryCheckPoint(annotation.sourceStart, annotation.declarationSourceEnd);
				if (this.currentElement instanceof RecoveredAnnotation) {
					this.currentElement = ((RecoveredAnnotation)this.currentElement).addAnnotation(annotation, this.identifierPtr);
				}
			}

			if(!this.statementRecoveryActivated &&
					this.options.sourceLevel < ClassFileConstants.JDK1_5 &&
					this.lastErrorEndPositionBeforeRecovery < this.scanner.currentPosition) {
				problemReporter().invalidUsageOfAnnotation(annotation);
			}
			this.recordStringLiterals = true;
			return;
		}
		super.consumeNormalAnnotation(isTypeAnnotation);
	}
}
@Override
protected void consumePackageDeclarationName() {
	super.consumePackageDeclarationName();
	if (this.pendingAnnotation != null) {
		this.pendingAnnotation.potentialAnnotatedNode = this.compilationUnit.currentPackage;
		this.pendingAnnotation = null;
	}
}
@Override
protected void consumePackageDeclarationNameWithModifiers() {
	super.consumePackageDeclarationNameWithModifiers();
	if (this.pendingAnnotation != null) {
		this.pendingAnnotation.potentialAnnotatedNode = this.compilationUnit.currentPackage;
		this.pendingAnnotation = null;
	}
}
@Override
protected void consumePrimaryNoNewArrayName() {
	// this is class literal access, so reset potential receiver
	this.invocationType = NO_RECEIVER;
	this.qualifier = -1;

	super.consumePrimaryNoNewArrayName();
}
@Override
protected void consumeQualifiedSuperReceiver() {
	// this is class literal access, so reset potential receiver
	this.invocationType = NO_RECEIVER;
	this.qualifier = -1;

	super.consumeQualifiedSuperReceiver();
}
@Override
protected void consumePrimaryNoNewArrayNameThis() {
	// this is class literal access, so reset potential receiver
	this.invocationType = NO_RECEIVER;
	this.qualifier = -1;

	super.consumePrimaryNoNewArrayNameThis();
}
@Override
protected void consumePushPosition() {
	super.consumePushPosition();
	if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BINARY_OPERATOR) {
		int info = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER);
		popElement(K_BINARY_OPERATOR);
		pushOnElementStack(K_UNARY_OPERATOR, info);
	}
}
@Override
protected void consumeSwitchLabeledBlock() {
	popUntilElement(K_SWITCH_LABEL);
	popElement(K_SWITCH_LABEL);
	concatNodeLists();
}
@Override
protected int fetchNextToken() throws InvalidInputException {
	int token = this.scanner.getNextToken();
	if (token != TerminalTokens.TokenNameEOF && this.scanner.currentPosition > this.cursorLocation) {
		if (!this.diet || this.dietInt != 0) { // do this also when parsing field initializers:
			if (this.currentToken == TerminalTokens.TokenNameIdentifier
					&& this.identifierStack[this.identifierPtr].length == 0) {
				if (Scanner.isLiteral(token)) {
					// <emptyAssistIdentifier> <someLiteral> is illegal and most likely the literal should be replaced => discard it now
					return fetchNextToken();
				}
				if (token == TerminalTokens.TokenNameIdentifier) {
					// empty completion identifier followed by another identifier likely means the 2nd id should start another parse node.
					// To cleanly separate them find a suitable separator:
					this.scanner.currentPosition = this.scanner.startPosition; // return to startPosition after this charade
					if (this.unstackedAct < ERROR_ACTION) {
						if ("LocalVariableDeclaration".equals(reduce(TokenNameSEMICOLON))) //$NON-NLS-1$
							return TokenNameSEMICOLON; // send ';' to terminate a local variable declaration
						if ("ArgumentList".equals(reduce(TokenNameCOMMA))) //$NON-NLS-1$
							return TokenNameCOMMA; // send ',' to terminate an argument in the list
					}
				}
			}
		}
		if (!this.diet) { // only when parsing a method body:
			if (this.expressionPtr <= -1 && !requireExtendedRecovery()) {
				this.scanner.eofPosition = this.cursorLocation + 1; // revert to old strategy where we stop parsing right at the cursor

				// stop immediately or deferred?
				if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BLOCK_DELIMITER	// -> not within a complex structure?
						&& this.scanner.startPosition > this.cursorLocation + 1				// -> is current token entirely beyond cursorLocation?
						&& this.currentElement == null)										// -> clean slate?
				{
						return TerminalTokens.TokenNameEOF; // no need to look further
				}
			}
		}
	}
	return token;
}
/*
 * Variant of parse() without side effects that stops when another token would need to be fetched.
 * Returns the name of the last reduced rule, or empty string if nothing reduced, or null if error was detected.
 */
String reduce(int token) {
	int myStackTop = this.stateStackTop;
	int[] myStack = Arrays.copyOf(this.stack, this.stack.length);
	int act = this.unstackedAct;
	String reduceName = ""; //$NON-NLS-1$
	for (;;) {
		int stackLength = myStack.length;
		if (++myStackTop >= stackLength) {
			System.arraycopy(
				myStack, 0,
				myStack = new int[stackLength + StackIncrement], 0,
				stackLength);
		}
		myStack[myStackTop] = act;
		act = tAction(act, token);
		if (act == ERROR_ACTION)
			return null;
		if (act <= NUM_RULES) { // reduce
			myStackTop--;
		} else if (act > ERROR_ACTION) { // shift-reduce
			return reduceName; // ready to accept the next token
		} else {
			if (act < ACCEPT_ACTION) { // shift
				return reduceName; // ready to accept the next token
			}
			return reduceName; // ?
		}
		do {
			reduceName = name[non_terminal_index[lhs[act]]];
			myStackTop -= (rhs[act] - 1);
			act = ntAction(myStack[myStackTop], lhs[act]);
			if (act == ACCEPT_ACTION) {
				return reduceName;
			}
		} while (act <= NUM_RULES);
	}
}
@Override
protected void consumeToken(int token) {
	if(this.isFirst) {
		super.consumeToken(token);
		return;
	}
	if(this.canBeExplicitConstructor == NEXTTOKEN) {
		this.canBeExplicitConstructor = YES;
	} else {
		this.canBeExplicitConstructor = NO;
	}

	int previous = this.previousToken;
	int prevIdentifierPtr = this.previousIdentifierPtr;

	isInsideEnhancedForLoopWithoutBlock(token);

	if (isInsideMethod() || isInsideFieldInitialization() || isInsideAnnotation() || isInsideEnumConstantnitialization()) {
		switch(token) {
			case TokenNameLPAREN:
				if(previous == TokenNameIdentifier &&
						topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_PARAMETERIZED_METHOD_INVOCATION) {
					popElement(K_PARAMETERIZED_METHOD_INVOCATION);
				} else {
					popElement(K_BETWEEN_NEW_AND_LEFT_BRACKET);
				}
				break;
			case TokenNameLBRACE:
				popElement(K_BETWEEN_NEW_AND_LEFT_BRACKET);
				if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_SWITCH_LABEL
						&& previous == TokenNameARROW) {
					pushOnElementStack(K_SWITCH_EXPRESSION_DELIMITTER);
				}
				break;
			case TokenNameLBRACKET:
				if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BETWEEN_NEW_AND_LEFT_BRACKET) {
					popElement(K_BETWEEN_NEW_AND_LEFT_BRACKET);
					pushOnElementStack(K_ARRAY_CREATION);
				}
				break;
			case TokenNameRBRACE:
				int kind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
				switch (kind) {
					case K_BLOCK_DELIMITER:
						popElement(K_BLOCK_DELIMITER);
						break;
					case K_MEMBER_VALUE_ARRAY_INITIALIZER:
						popElement(K_MEMBER_VALUE_ARRAY_INITIALIZER);
						break;
					case K_LAMBDA_EXPRESSION_DELIMITER:
						break; // will be popped when the containing block statement is reduced.
					default:
						popElement(K_ARRAY_INITIALIZER);
						break;
				}
				break;
			case TokenNameRBRACKET:
				if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BETWEEN_LEFT_AND_RIGHT_BRACKET) {
					popElement(K_BETWEEN_LEFT_AND_RIGHT_BRACKET);
				}
				break;

		}
	}

	// this is to handle field definitions which doesn't end with a ';'.
	if(previous == TokenNameIdentifier && token == TokenNameAT308 && this.currentElement == null
			&& this.identifierStack[this.previousIdentifierPtr] == assistIdentifier()
			&& !isIndirectlyInsideFieldInitialization() && isInsideType()) {
		pushOnElementStack(K_FIELD_INITIALIZER_DELIMITER);
		this.scanner.eofPosition = this.cursorLocation < Integer.MAX_VALUE ? this.cursorLocation+1 : this.cursorLocation;
	}

	super.consumeToken(token);

	// if in field initializer (directly or not), on the completion identifier and not in recovery mode yet
	// then position end of file at cursor location (so that we have the same behavior as
	// in method bodies)
	if (token == TokenNameIdentifier
			&& this.identifierStack[this.identifierPtr] == assistIdentifier()
			&& this.currentElement == null
			&& (!isIndirectlyInsideLambdaExpression())
			&& isIndirectlyInsideFieldInitialization()) { // enum initializers indeed need more context
		this.scanner.eofPosition = this.cursorLocation < Integer.MAX_VALUE ? this.cursorLocation+1 : this.cursorLocation;
	}
	if (token == TokenNameimport) {
		pushOnElementStack(K_INSIDE_IMPORT_STATEMENT);
	}	else if (token == TokenNameexports) {
		pushOnElementStack(K_INSIDE_EXPORTS_STATEMENT);
	}	else if (token == TokenNameopens) {
		pushOnElementStack(K_INSIDE_OPENS_STATEMENT);
	}	else if (token == TokenNameto) {
		popElement(K_AFTER_PACKAGE_IN_PACKAGE_VISIBILITY_STATEMENT);
	}	else if (token == TokenNamerequires) {
		pushOnElementStack(K_INSIDE_REQUIRES_STATEMENT);
	} else if (token == TokenNameprovides) {
		pushOnElementStack(K_INSIDE_PROVIDES_STATEMENT);
	} else if (token == TokenNameuses) {
		pushOnElementStack(K_INSIDE_USES_STATEMENT);
	}	else if (token == TokenNamewith) {
		popElement(K_AFTER_NAME_IN_PROVIDES_STATEMENT);
		pushOnElementStack(K_AFTER_WITH_IN_PROVIDES_STATEMENT);
	}

	// if in a method or if in a field initializer
	if (isInsideMethod() || isInsideFieldInitialization() || isInsideAttributeValue() || isInsideEnumConstantnitialization()) {
		switch (token) {
			case TokenNameDOT:
				switch (previous) {
					case TokenNamethis: // e.g. this[.]fred()
						this.invocationType = EXPLICIT_RECEIVER;
						break;
					case TokenNamesuper: // e.g. super[.]fred()
						this.invocationType = SUPER_RECEIVER;
						break;
					case TokenNameIdentifier: // e.g. bar[.]fred()
						if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) != K_BETWEEN_NEW_AND_LEFT_BRACKET) {
							if (this.identifierPtr != prevIdentifierPtr) { // if identifier has been consumed, e.g. this.x[.]fred()
								this.invocationType = EXPLICIT_RECEIVER;
							} else {
								this.invocationType = NAME_RECEIVER;
							}
						}
						break;
				}
				break;
			case TokenNameCOLON_COLON:
				this.inReferenceExpression = true;
				break;
			case TokenNameIdentifier:
				if (this.inReferenceExpression)
					break;
				if (this.scanner.previewEnabled && isInsideSwitch() && checkYieldKeyword()) {
					pushOnElementStack(K_YIELD_KEYWORD);
					// Take the short cut here.
					// Instead of injecting the TokenNameRestrictedIdentifierYield, totally ignore it
					// and let completion take it course. We will not be constructing the
					// YieldStatement and thus not producing accurate completion, but completion doesn't have
					// enough information anyway about the LHS anyway.
					token = this.currentToken = this.getNextToken();
					super.consumeToken(this.currentToken);
				}
				if (previous == TokenNameDOT) { // e.g. foo().[fred]()
					if (this.invocationType != SUPER_RECEIVER // e.g. not super.[fred]()
						&& this.invocationType != NAME_RECEIVER // e.g. not bar.[fred]()
						&& this.invocationType != ALLOCATION // e.g. not new foo.[Bar]()
						&& this.invocationType != QUALIFIED_ALLOCATION) { // e.g. not fred().new foo.[Bar]()

						this.invocationType = EXPLICIT_RECEIVER;
						this.qualifier = this.expressionPtr;
					}
				}
				if (previous == TokenNameGREATER) { // e.g. foo().<X>[fred]()
					if (this.invocationType != SUPER_RECEIVER // e.g. not super.<X>[fred]()
						&& this.invocationType != NAME_RECEIVER // e.g. not bar.<X>[fred]()
						&& this.invocationType != ALLOCATION // e.g. not new foo.<X>[Bar]()
						&& this.invocationType != QUALIFIED_ALLOCATION) { // e.g. not fred().new foo.<X>[Bar]()

						if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_PARAMETERIZED_METHOD_INVOCATION) {
							this.invocationType = EXPLICIT_RECEIVER;
							this.qualifier = this.expressionPtr;
						}
					}
				}
				break;
			case TokenNamenew:
				if (this.inReferenceExpression)
					break;
				pushOnElementStack(K_BETWEEN_NEW_AND_LEFT_BRACKET);
				this.qualifier = this.expressionPtr; // NB: even if there is no qualification, set it to the expression ptr so that the number of arguments are correctly computed
				if (previous == TokenNameDOT) { // e.g. fred().[new] X()
					this.invocationType = QUALIFIED_ALLOCATION;
				} else { // e.g. [new] X()
					this.invocationType = ALLOCATION;
				}
				break;
			case TokenNamethis:
				if (previous == TokenNameDOT) { // e.g. fred().[this]()
					this.invocationType = QUALIFIED_ALLOCATION;
					this.qualifier = this.expressionPtr;
				}
				break;
			case TokenNamesuper:
				if (previous == TokenNameDOT) { // e.g. fred().[super]()
					this.invocationType = QUALIFIED_ALLOCATION;
					this.qualifier = this.expressionPtr;
				}
				break;
			case TokenNamecatch:
				pushOnElementStack(K_BETWEEN_CATCH_AND_RIGHT_PAREN);
				break;
			case TokenNameLPAREN:
				if (this.invocationType == NO_RECEIVER || this.invocationType == NAME_RECEIVER || this.invocationType == SUPER_RECEIVER) {
					this.qualifier = this.expressionPtr; // remenber the last expression so that arguments are correctly computed
				}
				switch (previous) {
					case TokenNameIdentifier: // e.g. fred[(]) or foo.fred[(])
						if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_SELECTOR) {
							int info = 0;
							if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER,1) == K_BETWEEN_ANNOTATION_NAME_AND_RPAREN &&
									(info=topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER,1) & LPAREN_NOT_CONSUMED) != 0) {
								popElement(K_SELECTOR);
								popElement(K_BETWEEN_ANNOTATION_NAME_AND_RPAREN);
								if ((info & ANNOTATION_NAME_COMPLETION) != 0) {
									this.pushOnElementStack(K_BETWEEN_ANNOTATION_NAME_AND_RPAREN, LPAREN_CONSUMED | ANNOTATION_NAME_COMPLETION);
								} else {
									this.pushOnElementStack(K_BETWEEN_ANNOTATION_NAME_AND_RPAREN, LPAREN_CONSUMED);
								}
							} else {
								this.pushOnElementStack(K_SELECTOR_INVOCATION_TYPE, this.invocationType);
								this.pushOnElementStack(K_SELECTOR_QUALIFIER, this.qualifier);
							}
						}
						this.qualifier = -1;
						this.invocationType = NO_RECEIVER;
						break;
					case TokenNamethis: // explicit constructor invocation, e.g. this[(]1, 2)
						if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_SELECTOR) {
							this.pushOnElementStack(K_SELECTOR_INVOCATION_TYPE, (this.invocationType == QUALIFIED_ALLOCATION) ? QUALIFIED_ALLOCATION : ALLOCATION);
							this.pushOnElementStack(K_SELECTOR_QUALIFIER, this.qualifier);
						}
						this.qualifier = -1;
						this.invocationType = NO_RECEIVER;
						break;
					case TokenNamesuper: // explicit constructor invocation, e.g. super[(]1, 2)
						if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_SELECTOR) {
							this.pushOnElementStack(K_SELECTOR_INVOCATION_TYPE, (this.invocationType == QUALIFIED_ALLOCATION) ? QUALIFIED_ALLOCATION : ALLOCATION);
							this.pushOnElementStack(K_SELECTOR_QUALIFIER, this.qualifier);
						}
						this.qualifier = -1;
						this.invocationType = NO_RECEIVER;
						break;
					case TokenNameGREATER: // explicit constructor invocation, e.g. Fred<X>[(]1, 2)
					case TokenNameRIGHT_SHIFT: // or fred<X<X>>[(]1, 2)
					case TokenNameUNSIGNED_RIGHT_SHIFT: //or Fred<X<X<X>>>[(]1, 2)
						if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_SELECTOR) {
							int info;
							if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER, 1) == K_BINARY_OPERATOR &&
									((info = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER, 1)) == GREATER || info == RIGHT_SHIFT || info == UNSIGNED_RIGHT_SHIFT)) {
								// it's not a selector invocation
								popElement(K_SELECTOR);
							} else {
								this.pushOnElementStack(K_SELECTOR_INVOCATION_TYPE, (this.invocationType == QUALIFIED_ALLOCATION) ? QUALIFIED_ALLOCATION : ALLOCATION);
								this.pushOnElementStack(K_SELECTOR_QUALIFIER, this.qualifier);
							}
						}
						this.qualifier = -1;
						this.invocationType = NO_RECEIVER;
						break;
				}
				break;
			case TokenNameLBRACE:
				int kind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
				if(kind == K_FIELD_INITIALIZER_DELIMITER
					|| kind == K_LOCAL_INITIALIZER_DELIMITER
					|| kind == K_ARRAY_CREATION) {
					pushOnElementStack(K_ARRAY_INITIALIZER, this.endPosition);
				} else if (kind == K_BETWEEN_ANNOTATION_NAME_AND_RPAREN || kind == K_ATTRIBUTE_VALUE_DELIMITER) {
					pushOnElementStack(K_MEMBER_VALUE_ARRAY_INITIALIZER, this.endPosition);
				} else {
					if (kind == K_CONTROL_STATEMENT_DELIMITER) {
						int info = topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER);
						popElement(K_CONTROL_STATEMENT_DELIMITER);
						switch (info) {
							case IF:
								// include top-expression of these just for the benefit of hasPendingExpression():
								// (TRY is not included, even Java9-t-w-r doesn't own an *expression*)
							case FOR:
							case WHILE:
								if (this.expressionPtr > -1) {
									pushOnElementStack(K_BLOCK_DELIMITER, info, this.expressionStack[this.expressionPtr]);
									break;
								}
								//$FALL-THROUGH$
							default:
								pushOnElementStack(K_BLOCK_DELIMITER, info);
						}
					} else {
						switch(previous) {
							case TokenNameRPAREN :
								switch(this.previousKind) {
									case K_BETWEEN_CATCH_AND_RIGHT_PAREN :
										pushOnElementStack(K_BLOCK_DELIMITER, CATCH);
										break;
									case K_BETWEEN_SWITCH_AND_RIGHT_PAREN :
										pushOnElementStack(K_BLOCK_DELIMITER, SWITCH);
										break;
									case K_BETWEEN_SYNCHRONIZED_AND_RIGHT_PAREN :
										pushOnElementStack(K_BLOCK_DELIMITER, SYNCHRONIZED);
										break;
									default :
										pushOnElementStack(K_BLOCK_DELIMITER);
										break;
								}
								break;
							case TokenNametry :
								pushOnElementStack(K_BLOCK_DELIMITER, TRY);
								break;
							case TokenNamedo:
								pushOnElementStack(K_BLOCK_DELIMITER, DO);
								break;
							case TokenNameARROW:
								break;
							default :
								pushOnElementStack(K_BLOCK_DELIMITER);
								break;
						}
					}
				}
				break;
			case TokenNameLBRACKET:
				if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) != K_ARRAY_CREATION) {
					pushOnElementStack(K_BETWEEN_LEFT_AND_RIGHT_BRACKET);
				} else {
					switch (previous) {
						case TokenNameIdentifier:
						case TokenNameboolean:
						case TokenNamebyte:
						case TokenNamechar:
						case TokenNamedouble:
						case TokenNamefloat:
						case TokenNameint:
						case TokenNamelong:
						case TokenNameshort:
						case TokenNameGREATER:
						case TokenNameRIGHT_SHIFT:
						case TokenNameUNSIGNED_RIGHT_SHIFT:
							this.invocationType = NO_RECEIVER;
							this.qualifier = -1;
							break;
					}
				}
				break;
			case TokenNameRPAREN:
				switch(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER)) {
					case K_BETWEEN_CATCH_AND_RIGHT_PAREN :
						popElement(K_BETWEEN_CATCH_AND_RIGHT_PAREN);
						break;
					case K_BETWEEN_INSTANCEOF_AND_RPAREN :
						popElement(K_BETWEEN_INSTANCEOF_AND_RPAREN);
						//$FALL-THROUGH$
					case K_BETWEEN_IF_AND_RIGHT_PAREN :
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == this.bracketDepth) {
							popElement(K_BETWEEN_IF_AND_RIGHT_PAREN);
							pushOnElementStack(K_CONTROL_STATEMENT_DELIMITER, IF, this.expressionStack[this.expressionPtr]);
						}
						break;
					case K_BETWEEN_WHILE_AND_RIGHT_PAREN :
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == this.bracketDepth) {
							popElement(K_BETWEEN_WHILE_AND_RIGHT_PAREN);
							pushOnElementStack(K_CONTROL_STATEMENT_DELIMITER, WHILE);
						}
						break;
					case K_BETWEEN_FOR_AND_RIGHT_PAREN :
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == this.bracketDepth) {
							popElement(K_BETWEEN_FOR_AND_RIGHT_PAREN);
							pushOnElementStack(K_CONTROL_STATEMENT_DELIMITER, FOR);
						}
						break;
					case K_BETWEEN_SWITCH_AND_RIGHT_PAREN :
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == this.bracketDepth) {
							popElement(K_BETWEEN_SWITCH_AND_RIGHT_PAREN);
						}
						break;
					case K_BETWEEN_SYNCHRONIZED_AND_RIGHT_PAREN :
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == this.bracketDepth) {
							popElement(K_BETWEEN_SYNCHRONIZED_AND_RIGHT_PAREN);
						}
						break;
				}
				break;
			case TokenNamethrow:
				pushOnElementStack(K_INSIDE_THROW_STATEMENT, this.bracketDepth);
				break;
			case TokenNameSEMICOLON:
				switch(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER)) {
					case K_INSIDE_THROW_STATEMENT :
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == this.bracketDepth) {
							popElement(K_INSIDE_THROW_STATEMENT);
						}
						break;
					case K_INSIDE_RETURN_STATEMENT :
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == this.bracketDepth) {
							popElement(K_INSIDE_RETURN_STATEMENT);
						}
						break;
					case K_INSIDE_ASSERT_STATEMENT :
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == this.bracketDepth) {
							popElement(K_INSIDE_ASSERT_STATEMENT);
						}
						break;
					case K_INSIDE_ASSERT_EXCEPTION :
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == this.bracketDepth) {
							popElement(K_INSIDE_ASSERT_EXCEPTION);
							popElement(K_INSIDE_ASSERT_STATEMENT);
						}
						break;
					case K_INSIDE_BREAK_STATEMENT:
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == this.bracketDepth) {
							popElement(K_INSIDE_BREAK_STATEMENT);
						}
						break;
					case K_INSIDE_CONTINUE_STATEMENT:
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == this.bracketDepth) {
							popElement(K_INSIDE_CONTINUE_STATEMENT);
						}
						break;
					case K_BETWEEN_FOR_AND_RIGHT_PAREN:
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == this.bracketDepth - 1) {
							popElement(K_BETWEEN_FOR_AND_RIGHT_PAREN);
							pushOnElementStack(K_INSIDE_FOR_CONDITIONAL, this.bracketDepth - 1);
						}
						break;
					case K_INSIDE_FOR_CONDITIONAL:
						if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == this.bracketDepth - 1) {
							popElement(K_INSIDE_FOR_CONDITIONAL);
							pushOnElementStack(K_BETWEEN_FOR_AND_RIGHT_PAREN, this.bracketDepth - 1);
						}
						break;
				}
				break;
			case TokenNamereturn:
				pushOnElementStack(K_INSIDE_RETURN_STATEMENT, this.bracketDepth);
				break;
			case TokenNameMULTIPLY:
				pushOnElementStack(K_BINARY_OPERATOR, MULTIPLY);
				break;
			case TokenNameDIVIDE:
				pushOnElementStack(K_BINARY_OPERATOR, DIVIDE);
				break;
			case TokenNameREMAINDER:
				pushOnElementStack(K_BINARY_OPERATOR, REMAINDER);
				break;
			case TokenNamePLUS:
				pushOnElementStack(K_BINARY_OPERATOR, PLUS);
				break;
			case TokenNameMINUS:
				pushOnElementStack(K_BINARY_OPERATOR, MINUS);
				break;
			case TokenNameLEFT_SHIFT:
				pushOnElementStack(K_BINARY_OPERATOR, LEFT_SHIFT);
				break;
			case TokenNameRIGHT_SHIFT:
				pushOnElementStack(K_BINARY_OPERATOR, RIGHT_SHIFT);
				break;
			case TokenNameUNSIGNED_RIGHT_SHIFT:
				pushOnElementStack(K_BINARY_OPERATOR, UNSIGNED_RIGHT_SHIFT);
				break;
			case TokenNameLESS:
				switch(previous) {
					case TokenNameDOT :
						pushOnElementStack(K_PARAMETERIZED_METHOD_INVOCATION);
						break;
					case TokenNamenew :
						pushOnElementStack(K_PARAMETERIZED_ALLOCATION);
						break;
				}
				pushOnElementStack(K_BINARY_OPERATOR, LESS);
				break;
			case TokenNameGREATER:
				pushOnElementStack(K_BINARY_OPERATOR, GREATER);
				break;
			case TokenNameLESS_EQUAL:
				pushOnElementStack(K_BINARY_OPERATOR, LESS_EQUAL);
				break;
			case TokenNameGREATER_EQUAL:
				pushOnElementStack(K_BINARY_OPERATOR, GREATER_EQUAL);
				break;
			case TokenNameAND:
				pushOnElementStack(K_BINARY_OPERATOR, AND);
				break;
			case TokenNameXOR:
				pushOnElementStack(K_BINARY_OPERATOR, XOR);
				break;
			case TokenNameOR:
				// Don't push the OR operator used for union types in a catch declaration
				if (topKnownElementKind(COMPLETION_PARSER) != K_BETWEEN_CATCH_AND_RIGHT_PAREN)
					pushOnElementStack(K_BINARY_OPERATOR, OR);
				break;
			case TokenNameAND_AND:
				pushOnElementStack(K_BINARY_OPERATOR, AND_AND);
				break;
			case TokenNameOR_OR:
				pushOnElementStack(K_BINARY_OPERATOR, OR_OR);
				break;
			case TokenNamePLUS_PLUS:
				pushOnElementStack(K_UNARY_OPERATOR, PLUS_PLUS);
				break;
			case TokenNameMINUS_MINUS:
				pushOnElementStack(K_UNARY_OPERATOR, MINUS_MINUS);
				break;
			case TokenNameTWIDDLE:
				pushOnElementStack(K_UNARY_OPERATOR, TWIDDLE);
				break;
			case TokenNameNOT:
				pushOnElementStack(K_UNARY_OPERATOR, NOT);
				break;
			case TokenNameEQUAL_EQUAL:
				pushOnElementStack(K_BINARY_OPERATOR, EQUAL_EQUAL);
				break;
			case TokenNameNOT_EQUAL:
				pushOnElementStack(K_BINARY_OPERATOR, NOT_EQUAL);
				break;
			case TokenNameinstanceof:
				pushOnElementStack(K_BINARY_OPERATOR, INSTANCEOF);
				break;
			case TokenNameQUESTION:
				if(previous != TokenNameLESS && previous != TokenNameCOMMA) {
					pushOnElementStack(K_CONDITIONAL_OPERATOR, QUESTION);
				}
				break;
			case TokenNameARROW:
				switch (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER)) {
					case K_BETWEEN_CASE_AND_COLON:
						popElement(K_BETWEEN_CASE_AND_COLON);
						break;
					case K_BETWEEN_DEFAULT_AND_COLON:
						popElement(K_BETWEEN_DEFAULT_AND_COLON);
						break;
				}
				break;
			case TokenNameCOLON:
				switch (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER)) {
					case K_CONDITIONAL_OPERATOR:
						if (topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == QUESTION) {
							popElement(K_CONDITIONAL_OPERATOR);
							pushOnElementStack(K_CONDITIONAL_OPERATOR, COLON);
						}
						break;
					case K_BETWEEN_CASE_AND_COLON:
						popElement(K_BETWEEN_CASE_AND_COLON);
						break;
					case K_BETWEEN_DEFAULT_AND_COLON:
						popElement(K_BETWEEN_DEFAULT_AND_COLON);
						break;
					case K_INSIDE_ASSERT_STATEMENT:
						pushOnElementStack(K_INSIDE_ASSERT_EXCEPTION, this.bracketDepth);
						break;
				}
				break;
			case TokenNameif:
				pushOnElementStack(K_BETWEEN_IF_AND_RIGHT_PAREN, this.bracketDepth);
				break;
			case TokenNameelse:
				if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_CONTROL_STATEMENT_DELIMITER) {
					popElement(K_CONTROL_STATEMENT_DELIMITER);
				}
				pushOnElementStack(K_CONTROL_STATEMENT_DELIMITER);
				break;
			case TokenNamewhile:
				pushOnElementStack(K_BETWEEN_WHILE_AND_RIGHT_PAREN, this.bracketDepth);
				break;
			case TokenNamefor:
				pushOnElementStack(K_BETWEEN_FOR_AND_RIGHT_PAREN, this.bracketDepth);
				break;
			case TokenNameswitch:
				popElement(K_LOCAL_INITIALIZER_DELIMITER);
				pushOnElementStack(K_BETWEEN_SWITCH_AND_RIGHT_PAREN, this.bracketDepth);
				break;
			case TokenNamesynchronized:
				pushOnElementStack(K_BETWEEN_SYNCHRONIZED_AND_RIGHT_PAREN, this.bracketDepth);
				break;
			case TokenNameassert:
				pushOnElementStack(K_INSIDE_ASSERT_STATEMENT, this.bracketDepth);
				break;
			case TokenNamecase :
				pushOnElementStack(K_BETWEEN_CASE_AND_COLON);
				break;
			case TokenNameCOMMA :
				switch (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER)) {
					// for multi constant case stmt
					// case MONDAY, FRI
					// if there's a comma, ignore the previous expression (constant)
					// Which doesn't matter for completing the next constant
					case K_BETWEEN_CASE_AND_COLON:
						this.expressionPtr--;
						this.expressionLengthStack[this.expressionLengthPtr]--;
				}
				break;
			case TokenNamedefault :
				pushOnElementStack(K_BETWEEN_DEFAULT_AND_COLON);
				break;
			case TokenNameextends:
				pushOnElementStack(K_EXTENDS_KEYWORD);
				break;
			case TokenNamebreak:
				pushOnElementStack(K_INSIDE_BREAK_STATEMENT, this.bracketDepth);
				break;
			case TokenNamecontinue:
				pushOnElementStack(K_INSIDE_CONTINUE_STATEMENT, this.bracketDepth);
				break;
		}
	} else if (isInsideAnnotation()){
		switch (token) {
			case TokenNameLBRACE:
				this.bracketDepth++;
				int kind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
				if (kind == K_BETWEEN_ANNOTATION_NAME_AND_RPAREN) {
					pushOnElementStack(K_MEMBER_VALUE_ARRAY_INITIALIZER, this.endPosition);
				}
				break;
		}
	} else {
		switch(token) {
			case TokenNameextends:
				pushOnElementStack(K_EXTENDS_KEYWORD);
				break;
			case TokenNameLESS:
				pushOnElementStack(K_BINARY_OPERATOR, LESS);
				break;
			case TokenNameGREATER:
				pushOnElementStack(K_BINARY_OPERATOR, GREATER);
				break;
			case TokenNameRIGHT_SHIFT:
				pushOnElementStack(K_BINARY_OPERATOR, RIGHT_SHIFT);
				break;
			case TokenNameUNSIGNED_RIGHT_SHIFT:
				pushOnElementStack(K_BINARY_OPERATOR, UNSIGNED_RIGHT_SHIFT);
				break;

		}
	}
}
private void isInsideEnhancedForLoopWithoutBlock(int token) {
	if( this.consumedEnhancedFor == true && token != TokenNameLBRACE) {
		consumeOpenFakeBlock();
	}
	this.consumedEnhancedFor = false;

}
@Override
protected void consumeInvocationExpression() { // on error, a message send's error reductions will take the expression path rather than the statement path since that is a dead end.
	super.consumeInvocationExpression();
	triggerRecoveryUponLambdaClosure(this.expressionStack[this.expressionPtr], false);
}
@Override
protected void consumeReferenceExpression(ReferenceExpression referenceExpression) {
	this.inReferenceExpression = false;
	super.consumeReferenceExpression(referenceExpression);
}
@Override
protected void consumeOnlySynchronized() {
	super.consumeOnlySynchronized();
	this.hasUnusedModifiers = false;
}
@Override
protected void consumeOnlyTypeArguments() {
	super.consumeOnlyTypeArguments();
	popElement(K_BINARY_OPERATOR);
	if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_PARAMETERIZED_METHOD_INVOCATION) {
		popElement(K_PARAMETERIZED_METHOD_INVOCATION);
		pushOnElementStack(K_PARAMETERIZED_METHOD_INVOCATION, INSIDE_NAME);
	} else {
		popElement(K_PARAMETERIZED_ALLOCATION);
	}
}
@Override
protected void consumeOnlyTypeArgumentsForCastExpression() {
	super.consumeOnlyTypeArgumentsForCastExpression();
	pushOnElementStack(K_PARAMETERIZED_CAST);
}
@Override
protected void consumeOpenFakeBlock() {
	super.consumeOpenFakeBlock();
	pushOnElementStack(K_BLOCK_DELIMITER);
}
@Override
protected void consumeOpensStatement() {
	super.consumeOpensStatement();
	popElement(K_AFTER_PACKAGE_IN_PACKAGE_VISIBILITY_STATEMENT);
	popElement(K_INSIDE_OPENS_STATEMENT);
}
@Override
protected void consumeRightParen() {
	super.consumeRightParen();
}
@Override
protected void consumeReferenceType1() {
	super.consumeReferenceType1();
	popElement(K_BINARY_OPERATOR);
}
@Override
protected void consumeReferenceType2() {
	super.consumeReferenceType2();
	popElement(K_BINARY_OPERATOR);
}
@Override
protected void consumeReferenceType3() {
	super.consumeReferenceType3();
	popElement(K_BINARY_OPERATOR);
}
@Override
protected void consumeTypeArgumentReferenceType1() {
	super.consumeTypeArgumentReferenceType1();
	popElement(K_BINARY_OPERATOR);
}
@Override
protected void consumeTypeArgumentReferenceType2() {
	super.consumeTypeArgumentReferenceType2();
	popElement(K_BINARY_OPERATOR);
}
@Override
protected void consumeTypeArguments() {
	super.consumeTypeArguments();
	popElement(K_BINARY_OPERATOR);
}
@Override
protected void consumeTypeHeaderNameWithTypeParameters() {
	super.consumeTypeHeaderNameWithTypeParameters();

	TypeDeclaration typeDecl = (TypeDeclaration)this.astStack[this.astPtr];
	classHeaderExtendsOrImplements((typeDecl.modifiers & ClassFileConstants.AccInterface) != 0, false);
}
@Override
protected void consumeTypeImportOnDemandDeclarationName() {
	super.consumeTypeImportOnDemandDeclarationName();
	this.pendingAnnotation = null; // the pending annotation cannot be attached to next nodes
}
@Override
protected void consumeImportDeclaration() {
	super.consumeImportDeclaration();
	popElement(K_INSIDE_IMPORT_STATEMENT);
}
@Override
protected void consumeTypeParameters() {
	super.consumeTypeParameters();
	popElement(K_BINARY_OPERATOR);
}
@Override
protected void consumeTypeParameterHeader() {
	super.consumeTypeParameterHeader();
	TypeParameter typeParameter = (TypeParameter) this.genericsStack[this.genericsPtr];
	if(typeParameter.type != null || (typeParameter.bounds != null && typeParameter.bounds.length > 0)) return;

	if (assistIdentifier() == null && this.currentToken == TokenNameIdentifier) { // Test below copied from CompletionScanner.getCurrentIdentifierSource()
		if (this.cursorLocation < this.scanner.startPosition && this.scanner.currentPosition == this.scanner.startPosition){ // fake empty identifier got issued
			this.pushIdentifier();
		} else if (this.cursorLocation+1 >= this.scanner.startPosition && this.cursorLocation < this.scanner.currentPosition){
			this.pushIdentifier();
		} else {
			return;
		}
	} else {
		return;
	}

	CompletionOnKeyword1 keyword = new CompletionOnKeyword1(
		this.identifierStack[this.identifierPtr],
		this.identifierPositionStack[this.identifierPtr],
		Keywords.EXTENDS);
	typeParameter.type = keyword;

	this.identifierPtr--;
	this.identifierLengthPtr--;

	this.assistNode = typeParameter.type;
	this.lastCheckPoint = typeParameter.type.sourceEnd + 1;
}
@Override
protected void consumeTypeParameter1() {
	super.consumeTypeParameter1();
	popElement(K_BINARY_OPERATOR);
}
@Override
protected void consumeTypeParameterWithExtends() {
	super.consumeTypeParameterWithExtends();
	if (this.assistNode != null && this.assistNodeParent == null) {
		TypeParameter typeParameter = (TypeParameter) this.genericsStack[this.genericsPtr];
		if (typeParameter != null && typeParameter.type == this.assistNode)
			this.assistNodeParent = typeParameter;
	}
	popElement(K_EXTENDS_KEYWORD);
}
@Override
protected void consumeTypeParameterWithExtendsAndBounds() {
	super.consumeTypeParameterWithExtendsAndBounds();
	if (this.assistNode != null && this.assistNodeParent == null) {
		TypeParameter typeParameter = (TypeParameter) this.genericsStack[this.genericsPtr];
		if (typeParameter != null && typeParameter.type == this.assistNode)
			this.assistNodeParent = typeParameter;
	}
	popElement(K_EXTENDS_KEYWORD);
}
@Override
protected void consumeTypeParameter1WithExtends() {
	super.consumeTypeParameter1WithExtends();
	if (this.assistNode != null && this.assistNodeParent == null) {
		TypeParameter typeParameter = (TypeParameter) this.genericsStack[this.genericsPtr];
		if (typeParameter != null && typeParameter.type == this.assistNode)
			this.assistNodeParent = typeParameter;
	}
	popElement(K_EXTENDS_KEYWORD);
}
@Override
protected void consumeTypeParameter1WithExtendsAndBounds() {
	super.consumeTypeParameter1WithExtendsAndBounds();
	if (this.assistNode != null && this.assistNodeParent == null) {
		TypeParameter typeParameter = (TypeParameter) this.genericsStack[this.genericsPtr];
		if (typeParameter != null && typeParameter.type == this.assistNode)
			this.assistNodeParent = typeParameter;
	}
	popElement(K_EXTENDS_KEYWORD);
}
@Override
protected void consumeUnionType() {
	pushOnElementStack(K_NEXT_TYPEREF_IS_EXCEPTION);
	super.consumeUnionType();
	popElement(K_NEXT_TYPEREF_IS_EXCEPTION);
}
@Override
protected void consumeUnionTypeAsClassType() {
	pushOnElementStack(K_NEXT_TYPEREF_IS_EXCEPTION);
	super.consumeUnionTypeAsClassType();
	popElement(K_NEXT_TYPEREF_IS_EXCEPTION);
}
@Override
protected void consumeUsesStatement() {
	super.consumeUsesStatement();
	popElement(K_INSIDE_USES_STATEMENT);
}

@Override
protected void consumeWildcard() {
	super.consumeWildcard();
	if (assistIdentifier() == null && this.currentToken == TokenNameIdentifier) { // Test below copied from CompletionScanner.getCurrentIdentifierSource()
		if (this.cursorLocation < this.scanner.startPosition && this.scanner.currentPosition == this.scanner.startPosition){ // fake empty identifier got issued
			this.pushIdentifier();
		} else if (this.cursorLocation+1 >= this.scanner.startPosition && this.cursorLocation < this.scanner.currentPosition){
			this.pushIdentifier();
		} else {
			return;
		}
	} else {
		return;
	}
	Wildcard wildcard = (Wildcard) this.genericsStack[this.genericsPtr];
	CompletionOnKeyword1 keyword = new CompletionOnKeyword1(
		this.identifierStack[this.identifierPtr],
		this.identifierPositionStack[this.identifierPtr],
		new char[][]{Keywords.EXTENDS, Keywords.SUPER} );
	wildcard.kind = Wildcard.EXTENDS;
	wildcard.bound = keyword;

	this.identifierPtr--;
	this.identifierLengthPtr--;

	this.assistNode = wildcard.bound;
	this.lastCheckPoint = wildcard.bound.sourceEnd + 1;
}
@Override
protected void consumeWildcard1() {
	super.consumeWildcard1();
	popElement(K_BINARY_OPERATOR);
}
@Override
protected void consumeWildcard2() {
	super.consumeWildcard2();
	popElement(K_BINARY_OPERATOR);
}
@Override
protected void consumeWildcard3() {
	super.consumeWildcard3();
	popElement(K_BINARY_OPERATOR);
}
@Override
protected void consumeWildcardBoundsExtends() {
	super.consumeWildcardBoundsExtends();
	if (this.assistNode != null && this.assistNodeParent == null) {
		Wildcard wildcard = (Wildcard) this.genericsStack[this.genericsPtr];
		if (wildcard != null && wildcard.bound == this.assistNode)
			this.assistNodeParent = wildcard;
	}
	popElement(K_EXTENDS_KEYWORD);
}
@Override
protected void consumeWildcardBounds1Extends() {
	super.consumeWildcardBounds1Extends();
	if (this.assistNode != null && this.assistNodeParent == null) {
		Wildcard wildcard = (Wildcard) this.genericsStack[this.genericsPtr];
		if (wildcard != null && wildcard.bound == this.assistNode)
			this.assistNodeParent = wildcard;
	}
	popElement(K_EXTENDS_KEYWORD);
}
@Override
protected void consumeWildcardBounds2Extends() {
	super.consumeWildcardBounds2Extends();
	if (this.assistNode != null && this.assistNodeParent == null) {
		Wildcard wildcard = (Wildcard) this.genericsStack[this.genericsPtr];
		if (wildcard != null && wildcard.bound == this.assistNode)
			this.assistNodeParent = wildcard;
	}
	popElement(K_EXTENDS_KEYWORD);
}
@Override
protected void consumeWildcardBounds3Extends() {
	super.consumeWildcardBounds3Extends();
	if (this.assistNode != null && this.assistNodeParent == null) {
		Wildcard wildcard = (Wildcard) this.genericsStack[this.genericsPtr];
		if (wildcard != null && wildcard.bound == this.assistNode)
			this.assistNodeParent = wildcard;
	}
	popElement(K_EXTENDS_KEYWORD);
}
@Override
protected void consumeUnaryExpression(int op) {
	super.consumeUnaryExpression(op);
	popElement(K_UNARY_OPERATOR);

	if(this.expressionStack[this.expressionPtr] instanceof UnaryExpression) {
		UnaryExpression exp = (UnaryExpression) this.expressionStack[this.expressionPtr];
		if(this.assistNode != null && exp.expression == this.assistNode) {
			this.assistNodeParent = exp;
		}
	}
}
@Override
protected void consumeUnaryExpression(int op, boolean post) {
	super.consumeUnaryExpression(op, post);
	popElement(K_UNARY_OPERATOR);

	if(this.expressionStack[this.expressionPtr] instanceof UnaryExpression) {
		UnaryExpression exp = (UnaryExpression) this.expressionStack[this.expressionPtr];
		if(this.assistNode != null && exp.expression == this.assistNode) {
			this.assistNodeParent = exp;
		}
	}
}
@Override
public MethodDeclaration convertToMethodDeclaration(ConstructorDeclaration c, CompilationResult compilationResult) {
	MethodDeclaration methodDeclaration = super.convertToMethodDeclaration(c, compilationResult);
	if (this.sourceEnds != null) {
		int selectorSourceEnd = this.sourceEnds.removeKey(c);
		if (selectorSourceEnd != -1)
			this.sourceEnds.put(methodDeclaration, selectorSourceEnd);
	}
	return methodDeclaration;
}
@Override
public ImportReference createAssistPackageVisibilityReference(char[][] tokens, long[] positions){
	return new CompletionOnPackageVisibilityReference(tokens, positions);
}
@Override
public ImportReference createAssistImportReference(char[][] tokens, long[] positions, int mod){
	return new CompletionOnImportReference(tokens, positions, mod);
}
@Override
public ModuleReference createAssistModuleReference(int index) {
	/* retrieve identifiers subset and whole positions, the assist node positions
	should include the entire replaced source. */
	int length = this.identifierLengthStack[this.identifierLengthPtr];
	char[][] subset = identifierSubSet(index+1); // include the assistIdentifier
	this.identifierLengthPtr--;
	this.identifierPtr -= length;
	long[] positions = new long[length];
	System.arraycopy(
			this.identifierPositionStack,
			this.identifierPtr + 1,
			positions,
			0,
			length);
	return new CompletionOnModuleReference(subset, positions);
}
@Override
public ModuleDeclaration createAssistModuleDeclaration(CompilationResult compilationResult, char[][] tokens,
		long[] positions) {
	return new CompletionOnModuleDeclaration(compilationResult, tokens, positions);
}
@Override
public ImportReference createAssistPackageReference(char[][] tokens, long[] positions){
	return new CompletionOnPackageReference(tokens, positions);
}
@Override
public NameReference createQualifiedAssistNameReference(char[][] previousIdentifiers, char[] assistName, long[] positions){
	return new CompletionOnQualifiedNameReference(
					previousIdentifiers,
					assistName,
					positions,
					isInsideAttributeValue());
}
private TypeReference checkAndCreateModuleQualifiedAssistTypeReference(char[][] previousIdentifiers, char[] assistName, long[] positions) {
	if (isInUsesStatement()) return new CompletionOnUsesQualifiedTypeReference(previousIdentifiers, assistName, positions);
	if (isInProvidesStatement()) {
		if (isAfterWithClause()) return new CompletionOnProvidesImplementationsQualifiedTypeReference(previousIdentifiers, assistName, positions);
		return new CompletionOnProvidesInterfacesQualifiedTypeReference(previousIdentifiers, assistName, positions);
	}
	return null;
}
@Override
public TypeReference createQualifiedAssistTypeReference(char[][] previousIdentifiers, char[] assistName, long[] positions){
	switch (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER)) {
		case K_NEXT_TYPEREF_IS_EXCEPTION :
			if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER, 1) == K_BETWEEN_CATCH_AND_RIGHT_PAREN)
				this.isOrphanCompletionNode = true;
			return new CompletionOnQualifiedTypeReference(
					previousIdentifiers,
					assistName,
					positions,
					CompletionOnQualifiedTypeReference.K_EXCEPTION);
		case K_NEXT_TYPEREF_IS_CLASS :
			return new CompletionOnQualifiedTypeReference(
					previousIdentifiers,
					assistName,
					positions,
					CompletionOnQualifiedTypeReference.K_CLASS);
		case K_NEXT_TYPEREF_IS_INTERFACE :
			return new CompletionOnQualifiedTypeReference(
					previousIdentifiers,
					assistName,
					positions,
					CompletionOnQualifiedTypeReference.K_INTERFACE);
		default :
			TypeReference ref = checkAndCreateModuleQualifiedAssistTypeReference(
					previousIdentifiers,
					assistName,
					positions);
			if (ref != null)
				return ref;
			return new CompletionOnQualifiedTypeReference(previousIdentifiers, assistName, positions,
					CompletionOnQualifiedTypeReference.K_TYPE);
	}
}
@Override
public TypeReference createParameterizedQualifiedAssistTypeReference(char[][] previousIdentifiers, TypeReference[][] typeArguments, char[] assistName, TypeReference[] assistTypeArguments, long[] positions) {
	boolean isParameterized = false;
	for (int i = 0; i < typeArguments.length; i++) {
		if(typeArguments[i] != null) {
			isParameterized = true;
			break;
		}
	}
	if(!isParameterized) {
		return createQualifiedAssistTypeReference(previousIdentifiers, assistName, positions);
	} else {
		switch (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER)) {
			case K_NEXT_TYPEREF_IS_EXCEPTION :
				if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER, 1) == K_BETWEEN_CATCH_AND_RIGHT_PAREN)
					this.isOrphanCompletionNode = true;
				return new CompletionOnParameterizedQualifiedTypeReference(
					previousIdentifiers,
					typeArguments,
					assistName,
					positions,
					CompletionOnParameterizedQualifiedTypeReference.K_EXCEPTION);
			case K_NEXT_TYPEREF_IS_CLASS :
				return new CompletionOnParameterizedQualifiedTypeReference(
					previousIdentifiers,
					typeArguments,
					assistName,
					positions,
					CompletionOnParameterizedQualifiedTypeReference.K_CLASS);
			case K_NEXT_TYPEREF_IS_INTERFACE :
				return new CompletionOnParameterizedQualifiedTypeReference(
					previousIdentifiers,
					typeArguments,
					assistName,
					positions,
					CompletionOnParameterizedQualifiedTypeReference.K_INTERFACE);
			default :
				return new CompletionOnParameterizedQualifiedTypeReference(
					previousIdentifiers,
					typeArguments,
					assistName,
					positions);
		}
	}
}
@Override
public NameReference createSingleAssistNameReference(char[] assistName, long position) {
	int kind = topKnownElementKind(COMPLETION_OR_ASSIST_PARSER);
	if(!isInsideMethod()) {
		if (isInsideFieldInitialization()) {
			return new CompletionOnSingleNameReference(
					assistName,
					position,
					new char[][]{Keywords.FALSE, Keywords.TRUE},
					false,
					isInsideAttributeValue());
		}
		return new CompletionOnSingleNameReference(assistName, position, isInsideAttributeValue());
	} else {
		boolean canBeExplicitConstructorCall = false;
		if((kind == K_BLOCK_DELIMITER || kind == K_LAMBDA_EXPRESSION_DELIMITER)
			&& this.previousKind == K_BLOCK_DELIMITER
			&& this.previousInfo == DO) {
			return new CompletionOnKeyword3(assistName, position, Keywords.WHILE);
		} else if((kind == K_BLOCK_DELIMITER || kind == K_LAMBDA_EXPRESSION_DELIMITER)
			&& this.previousKind == K_BLOCK_DELIMITER
			&& this.previousInfo == TRY) {
			return new CompletionOnKeyword3(assistName, position, new char[][]{Keywords.CATCH, Keywords.FINALLY}, true);
		} else if(kind == K_BLOCK_DELIMITER
			&& topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == SWITCH) {
			return new CompletionOnKeyword3(assistName, position, new char[][]{Keywords.CASE, Keywords.DEFAULT}, false);
		} else {
			List<char[]> keywordsList = new ArrayList<>(Keywords.COUNT);
			canBeExplicitConstructorCall = computeKeywords(kind, keywordsList);
			char[][] keywords = keywordsList.toArray(char[][]::new);
			return new CompletionOnSingleNameReference(assistName, position, keywords, canBeExplicitConstructorCall, isInsideAttributeValue());
		}
	}
}
boolean computeKeywords(int kind, List<char[]> keywords) {
	boolean canBeExplicitConstructorCall = false;

	if((this.lastModifiers & ClassFileConstants.AccStatic) == 0) {
		keywords.add(Keywords.SUPER);
		keywords.add(Keywords.THIS);
	}
	keywords.add(Keywords.NEW);
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=269493: Keywords are not proposed in a for
	// loop without block. Completion while at K_CONTROL_STATEMENT_DELIMITER case needs to handled
	// similar to the K_BLOCK_DELIMITER with minor differences.
	if(kind == K_BLOCK_DELIMITER || kind == K_CONTROL_STATEMENT_DELIMITER || kind == K_LAMBDA_EXPRESSION_DELIMITER
			|| kind == K_SWITCH_EXPRESSION_DELIMITTER) {
		if(this.canBeExplicitConstructor == YES) {
			canBeExplicitConstructorCall = true;
		}
		if (this.options.complianceLevel >= ClassFileConstants.JDK1_4) {
			keywords.add(Keywords.ASSERT);
		}
		keywords.add(Keywords.DO);
		keywords.add(Keywords.FOR);
		keywords.add(Keywords.IF);
		keywords.add(Keywords.RETURN);
		keywords.add(Keywords.SWITCH);
		keywords.add(Keywords.SYNCHRONIZED);
		keywords.add(Keywords.THROW);
		keywords.add(Keywords.TRY);
		keywords.add(Keywords.WHILE);

		keywords.add(Keywords.FINAL);
		keywords.add(Keywords.CLASS);
		if (this.options.complianceLevel >= ClassFileConstants.JDK10) {
			keywords.add(Keywords.VAR);
		}
		if (this.options.complianceLevel >= ClassFileConstants.JDK16) {
			keywords.add(Keywords.INTERFACE);
			keywords.add(Keywords.ENUM);
		}

		if(this.previousKind == K_BLOCK_DELIMITER) {
			switch (this.previousInfo) {
				case IF :
					keywords.add(Keywords.ELSE);
					break;
				case CATCH :
					keywords.add(Keywords.CATCH);
					keywords.add(Keywords.FINALLY);
					break;
			}
		} else if(this.previousKind == K_CONTROL_STATEMENT_DELIMITER && this.previousInfo == IF) {
			keywords.add(Keywords.ELSE);
		}
		if(isInsideLoop()) {
			keywords.add(Keywords.CONTINUE);
		}
		if(isInsideBreakable()) {
			keywords.add(Keywords.BREAK);
		}
		if(isInsideSwitch()) {
			keywords.add(Keywords.YIELD);
		}
	} else if (kind == K_BETWEEN_FOR_AND_RIGHT_PAREN) {
		if (this.options.complianceLevel >= ClassFileConstants.JDK10) {
			keywords.add(Keywords.VAR);
		}
	} else if(kind != K_BETWEEN_CASE_AND_COLON && kind != K_BETWEEN_DEFAULT_AND_COLON) {
		if (kind == K_LOCAL_INITIALIZER_DELIMITER && this.options.complianceLevel >= ClassFileConstants.JDK11) {
			keywords.add(Keywords.VAR);
		}
		if (kind == K_SELECTOR_QUALIFIER && this.options.complianceLevel >= ClassFileConstants.JDK12) {
			keywords.add(Keywords.SWITCH);
		}
		keywords.add(Keywords.TRUE);
		keywords.add(Keywords.FALSE);
		keywords.add(Keywords.NULL);
		if (kind == K_YIELD_KEYWORD) {
			keywords.add(Keywords.YIELD);
		}
		if(kind == K_SWITCH_LABEL) {
			if(topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) != DEFAULT) {
				keywords.add(Keywords.DEFAULT);
			}
			keywords.add(Keywords.BREAK);
			keywords.add(Keywords.CASE);
			keywords.add(Keywords.YIELD);
			if (this.options.complianceLevel >= ClassFileConstants.JDK1_4) {
				keywords.add(Keywords.ASSERT);
			}
			keywords.add(Keywords.DO);
			keywords.add(Keywords.FOR);
			keywords.add(Keywords.IF);
			keywords.add(Keywords.RETURN);
			keywords.add(Keywords.SWITCH);
			keywords.add(Keywords.SYNCHRONIZED);
			keywords.add(Keywords.THROW);
			keywords.add(Keywords.TRY);
			keywords.add(Keywords.WHILE);

			keywords.add(Keywords.FINAL);
			keywords.add(Keywords.CLASS);

			if (this.options.complianceLevel >= ClassFileConstants.JDK10) {
				keywords.add(Keywords.VAR);
			}
			if(isInsideLoop()) {
				keywords.add(Keywords.CONTINUE);
			}
		}
	}
	return canBeExplicitConstructorCall;
}
private TypeReference checkAndCreateModuleSingleAssistTypeReference(char[] assistName, long position) {
	if (isInUsesStatement()) return new CompletionOnUsesSingleTypeReference(assistName, position);
	if (isInProvidesStatement()) {
		if (isAfterWithClause()) return new CompletionOnProvidesImplementationsSingleTypeReference(assistName, position);
		return new CompletionOnProvidesInterfacesSingleTypeReference(assistName, position);
	}
	List<char[]> keywords = new ArrayList<>(Keywords.COUNT);
	boolean canBeCtorCall = computeKeywords(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER), keywords);
	return new CompletionOnSingleTypeReference(assistName,position, keywords.toArray(char[][]::new), canBeCtorCall);
}
@Override
public TypeReference createSingleAssistTypeReference(char[] assistName, long position) {
	switch (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER)) {
		case K_NEXT_TYPEREF_IS_EXCEPTION :
			if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER, 1) == K_BETWEEN_CATCH_AND_RIGHT_PAREN)
				this.isOrphanCompletionNode = true;
			return new CompletionOnSingleTypeReference(assistName, position, CompletionOnSingleTypeReference.K_EXCEPTION) ;
		case K_NEXT_TYPEREF_IS_CLASS :
			return new CompletionOnSingleTypeReference(assistName, position, CompletionOnSingleTypeReference.K_CLASS);
		case K_NEXT_TYPEREF_IS_INTERFACE :
			return new CompletionOnSingleTypeReference(assistName, position, CompletionOnSingleTypeReference.K_INTERFACE);
		default :
			return checkAndCreateModuleSingleAssistTypeReference(assistName, position);
	}
}
@Override
public TypeReference createParameterizedSingleAssistTypeReference(TypeReference[] typeArguments, char[] assistName, long position) {
	return createSingleAssistTypeReference(assistName, position);
}
@Override
protected StringLiteral createStringLiteral(char[] token, int start, int end, int lineNumber) {
	if (start <= this.cursorLocation && this.cursorLocation <= end){
		char[] source = this.scanner.source;

		int contentStart = start;
		int contentEnd = end;

		// " could be as unicode \u0022
		int pos = contentStart;
		if(source[pos] == '\"') {
			contentStart = pos + 1;
		} else if(source[pos] == '\\' && source[pos+1] == 'u') {
			pos += 2;
			while (source[pos] == 'u') {
				pos++;
			}
			if(source[pos] == 0 && source[pos + 1] == 0 && source[pos + 2] == 2 && source[pos + 3] == 2) {
				contentStart = pos + 4;
			}
		}

		pos = contentEnd;
		if(source[pos] == '\"') {
			contentEnd = pos - 1;
		} else if(source.length > 5 && source[pos-4] == 'u') {
			if(source[pos - 3] == 0 && source[pos - 2] == 0 && source[pos - 1] == 2 && source[pos] == 2) {
				pos -= 5;
				while (pos > -1 && source[pos] == 'u') {
					pos--;
				}
				if(pos > -1 && source[pos] == '\\') {
					contentEnd = pos - 1;
				}
			}
		}

		if(contentEnd < start) {
			contentEnd = end;
		}

		if(this.cursorLocation != end || end == contentEnd) {
			CompletionOnStringLiteral stringLiteral = new CompletionOnStringLiteral(
					token,
					start,
					end,
					contentStart,
					contentEnd,
					lineNumber);

			this.assistNode = stringLiteral;
			this.restartRecovery = true;
			this.lastCheckPoint = end;

			return stringLiteral;
		}
	}
	return super.createStringLiteral(token, start, end, lineNumber);
}
@Override
protected TypeReference augmentTypeWithAdditionalDimensions(TypeReference typeRef, int additionalDimensions, Annotation[][] additionalAnnotations, boolean isVarargs) {
	if (this.assistNode == typeRef) {
		return typeRef;
	}
	TypeReference result = super.augmentTypeWithAdditionalDimensions(typeRef, additionalDimensions, additionalAnnotations, isVarargs);
	if (this.assistNodeParent == typeRef) {
		this.assistNodeParent = result;
	}
	return result;
}
public CompilationUnitDeclaration dietParse(ICompilationUnit sourceUnit, CompilationResult compilationResult, int cursorLoc) {

	this.cursorLocation = cursorLoc;
	CompletionScanner completionScanner = (CompletionScanner)this.scanner;
	completionScanner.completionIdentifier = null;
	completionScanner.cursorLocation = cursorLoc;
	return this.dietParse(sourceUnit, compilationResult);
}
/*
 * Flush parser/scanner state regarding to code assist
 */
@Override
public void flushAssistState() {

	super.flushAssistState();
	this.isOrphanCompletionNode = false;
	this.isAlreadyAttached = false;
	this.assistNodeParent = null;
	CompletionScanner completionScanner = (CompletionScanner)this.scanner;
	completionScanner.completedIdentifierStart = 0;
	completionScanner.completedIdentifierEnd = -1;
}

@Override
protected TypeReference getTypeReferenceForGenericType(int dim,	int identifierLength, int numberOfIdentifiers) {
	TypeReference ref = super.getTypeReferenceForGenericType(dim, identifierLength, numberOfIdentifiers);
	// in completion case we might have encountered the assist node before really parsing
	// the complete class instance creation, and so a separate check for diamond is needed here.
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=346454
	checkForDiamond(ref);
	if(this.assistNode != null) {
		if (identifierLength == 1 && numberOfIdentifiers == 1) {
			ParameterizedSingleTypeReference singleRef = (ParameterizedSingleTypeReference) ref;
			TypeReference[] typeArguments = singleRef.typeArguments;
			for (int i = 0; i < typeArguments.length; i++) {
				if(typeArguments[i] == this.assistNode) {
					this.assistNodeParent = ref;
					return ref;
				}
			}
		} else {
			ParameterizedQualifiedTypeReference qualifiedRef = (ParameterizedQualifiedTypeReference) ref;
			TypeReference[][] typeArguments = qualifiedRef.typeArguments;
			for (int i = 0; i < typeArguments.length; i++) {
				if(typeArguments[i] != null) {
					for (int j = 0; j < typeArguments[i].length; j++) {
						if(typeArguments[i][j] == this.assistNode) {
							this.assistNodeParent = ref;
							return ref;
						}
					}
				}
			}

		}
	}

	return ref;
}
@Override
protected NameReference getUnspecifiedReference(boolean rejectTypeAnnotations) {
	// code copied from super, but conditionally creating CompletionOn* nodes:

	/* build a (unspecified) NameReference which may be qualified*/
	if (rejectTypeAnnotations) { // Compensate for overpermissive grammar.
		consumeNonTypeUseName();
	}
	int length;
	NameReference ref;
	if ((length = this.identifierLengthStack[this.identifierLengthPtr--]) == 1) {
		// single variable reference
		char[] token = this.identifierStack[this.identifierPtr];
		long position = this.identifierPositionStack[this.identifierPtr--];
		int start = (int) (position >>> 32), end = (int) position;
		if (this.assistNode == null && start <= this.cursorLocation && end >= this.cursorLocation) {
			ref = new CompletionOnSingleNameReference(token, position, isInsideAttributeValue());
			this.assistNode = ref;
		} else {
			ref = new SingleNameReference(token, position);
		}
	} else {
		//Qualified variable reference
		char[][] tokens = new char[length][];
		this.identifierPtr -= length;
		System.arraycopy(this.identifierStack, this.identifierPtr + 1, tokens, 0, length);
		long[] positions = new long[length];
		System.arraycopy(this.identifierPositionStack, this.identifierPtr + 1, positions, 0, length);
		int start = (int) (positions[0] >>> 32), end = (int) positions[length-1];
		if (this.assistNode == null && start <= this.cursorLocation && end >= this.cursorLocation) {
			// find the token at cursorLocation:
			int previousCount = 0;
			for (int i=0; i<length; i++) {
				if (((int) positions[i]) < this.cursorLocation)
					previousCount = i + 1;
			}
			if (previousCount > 0) {
				char[][] subset = new char[previousCount][];
				System.arraycopy(tokens, 0, subset, 0, previousCount);
				ref = new CompletionOnQualifiedNameReference(subset, tokens[previousCount], positions, isInsideAttributeValue());
			} else {
				// with only one token up-to cursorLocation avoid a bogus qualifiedNameReference (simply skipping the remainder):
				ref = new CompletionOnSingleNameReference(tokens[0], positions[0], isInsideAttributeValue());
			}
			this.assistNode = ref;
		} else {
			ref =
				new QualifiedNameReference(tokens,
					positions,
					(int) (this.identifierPositionStack[this.identifierPtr + 1] >> 32), // sourceStart
					(int) this.identifierPositionStack[this.identifierPtr + length]); // sourceEnd
		}
	}
	if (this.record) {
		recordReference(ref);
	}
	return ref;
}
@Override
protected void updateSourcePosition(Expression exp) {
	// handles total positions of parenthesized expressions, but don't extend position of the assist node:
	if (exp == this.assistNode)
		this.intPtr -= 2;
	else
		super.updateSourcePosition(exp);
}
@Override
protected void consumePostfixExpression() {
	// PostfixExpression ::= Name
	if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) != K_YIELD_KEYWORD) {
		super.consumePostfixExpression();
	}
}
@Override
protected NameReference getUnspecifiedReferenceOptimized() {
	if (this.identifierLengthStack[this.identifierLengthPtr] > 1) { // reducing a qualified name
		// potential receiver is being poped, so reset potential receiver
		this.invocationType = NO_RECEIVER;
		this.qualifier = -1;
	}
	NameReference nameReference = super.getUnspecifiedReferenceOptimized();
	if (this.record) {
		recordReference(nameReference);
	}
	return nameReference;
}
private boolean isAlreadyPotentialName(int identifierStart) {
	if (this.potentialVariableNamesPtr < 0) return false;

	return identifierStart <= this.potentialVariableNameEnds[this.potentialVariableNamesPtr];
}
@Override
protected int indexOfAssistIdentifier(boolean useGenericsStack) {
	if (this.record) return -1; // when names are recorded there is no assist identifier
	return super.indexOfAssistIdentifier(useGenericsStack);
}
@Override
public void initialize() {
	super.initialize();
	this.labelPtr = -1;
	initializeForBlockStatements();
}
@Override
public void initialize(boolean parsingCompilationUnit) {
	super.initialize(parsingCompilationUnit);
	this.labelPtr = -1;
	initializeForBlockStatements();
}
@Override
public void copyState(Parser from) {

	super.copyState(from);

	CompletionParser parser = (CompletionParser) from;

	this.invocationType = parser.invocationType;
	this.qualifier = parser.qualifier;
	this.inReferenceExpression = parser.inReferenceExpression;
	this.hasUnusedModifiers = parser.hasUnusedModifiers;
	this.canBeExplicitConstructor = parser.canBeExplicitConstructor;
}
/*
 * Initializes the state of the parser that is about to go for BlockStatements.
 */
private void initializeForBlockStatements() {
	this.previousToken = -1;
	this.previousIdentifierPtr = -1;
	this.invocationType = NO_RECEIVER;
	this.qualifier = -1;
	popUntilElement(K_SWITCH_LABEL);
	if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) != K_SWITCH_LABEL) {
		if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_ARRAY_INITIALIZER) {
			// if recovery is taking place in an array initializer, we should prevent popping
			// up to the enclosing block until the array initializer is properly closed
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=249704
			popUntilElement(K_ARRAY_INITIALIZER);
		} else {
			popUntilElement(K_BLOCK_DELIMITER);
		}
	}
}
@Override
public void initializeScanner(){
	this.scanner = new CompletionScanner(this.options.sourceLevel, this.options.enablePreviewFeatures);
}
/**
 * Returns whether the completion is just after an array type
 * e.g. String[].[cursor]
 */
private boolean isAfterArrayType() {
	// TBD: The following relies on the fact that array dimensions are small: it says that if the
	//      top of the intStack is less than 11, then it must be a dimension
	//      (smallest position of array type in a compilation unit is 11 as in "class X{Y[]")
	if ((this.intPtr > -1) && (this.intStack[this.intPtr] < 11)) {
		return true;
	}
	return false;
}
private boolean isEmptyNameCompletion() {
	return
		this.assistNode != null &&
		this.assistNode instanceof CompletionOnSingleNameReference &&
		(((CompletionOnSingleNameReference)this.assistNode).token.length == 0);
}
protected boolean isInsideAnnotation() {
	int i = this.elementPtr;
	while(i > -1) {
		if(this.elementKindStack[i] == K_BETWEEN_ANNOTATION_NAME_AND_RPAREN)
			return true;
		i--;
	}
	return false;
}

protected boolean isInsideSwitch(){
	int i = this.elementPtr;
	while(i > -1) {
		switch (this.elementKindStack[i]) {
			case K_SWITCH_LABEL : return true;
		}
		i--;
	}
	return false;
}
protected boolean isInsideBreakable(){
	int i = this.elementPtr;
	while(i > -1) {
		switch (this.elementKindStack[i]) {
			case K_TYPE_DELIMITER : return false;
			case K_METHOD_DELIMITER : return false;
			case K_FIELD_INITIALIZER_DELIMITER : return false;
			case K_SWITCH_LABEL : return true;
			case K_BLOCK_DELIMITER :
			case K_CONTROL_STATEMENT_DELIMITER:
				switch(this.elementInfoStack[i]) {
					case FOR :
					case DO :
					case WHILE :
						return true;
				}
		}
		i--;
	}
	return false;
}
protected boolean isInsideLoop(){
	int i = this.elementPtr;
	while(i > -1) {
		switch (this.elementKindStack[i]) {
			case K_TYPE_DELIMITER : return false;
			case K_METHOD_DELIMITER : return false;
			case K_FIELD_INITIALIZER_DELIMITER : return false;
			case K_BLOCK_DELIMITER :
			case K_CONTROL_STATEMENT_DELIMITER:
				switch(this.elementInfoStack[i]) {
					case FOR :
					case DO :
					case WHILE :
						return true;
				}
		}
		i--;
	}
	return false;
}
protected boolean isInsideReturn(){
	int i = this.elementPtr;
	while(i > -1) {
		switch (this.elementKindStack[i]) {
			case K_TYPE_DELIMITER : return false;
			case K_METHOD_DELIMITER : return false;
			case K_FIELD_INITIALIZER_DELIMITER : return false;
			case K_BLOCK_DELIMITER : return false;
			case K_CONTROL_STATEMENT_DELIMITER: return false; // FWIW
			case K_INSIDE_RETURN_STATEMENT : return true;
		}
		i--;
	}
	return false;
}
@Override
public ReferenceExpression newReferenceExpression() {
	char[] selector = this.identifierStack[this.identifierPtr];
	if (selector != assistIdentifier()){
		return super.newReferenceExpression();
	}
	ReferenceExpression referenceExpression = new CompletionOnReferenceExpressionName(this.scanner);
	this.assistNode = referenceExpression;
	return referenceExpression;
}
@Override
protected MessageSend newMessageSend() {
	// cases where we find the completion node in absence of any syntax error
	MessageSend m = internalNewMessageSend();
	if (m != null)
		return m;
	return super.newMessageSend();
}
@Override
protected MessageSend newMessageSendWithTypeArguments() {
	// cases where we find the completion node in absence of any syntax error
	MessageSend m = internalNewMessageSend();
	if (m != null)
		return m;
	return super.newMessageSendWithTypeArguments();
}
private MessageSend internalNewMessageSend() {
	MessageSend m = null;
	long nameStart = this.identifierPositionStack[this.identifierPtr] >>> 32;
	if (this.assistNode == null && this.lParenPos > this.cursorLocation && nameStart <= this.cursorLocation + 1) {
		boolean nextIsCast = this.expressionPtr > -1 && this.expressionStack[this.expressionPtr] instanceof CastExpression;
		m = new CompletionOnMessageSendName(null, 0, 0, nextIsCast); // positions will be set in consumeMethodInvocationName(), if that's who called us
	} else if (this.assistNode != null && this.lParenPos == this.assistNode.sourceEnd) {
		// this branch corresponds to work done in checkParemeterizedMethodName(), just the latter isn't called in absence of a syntax error
		if (this.expressionPtr != -1 && this.expressionStack[this.expressionPtr] == this.assistNode) {
			// pop name reference off the expression stack:
			this.expressionLengthStack[this.expressionLengthPtr]--;
			this.expressionPtr--;
		}
		m = new CompletionOnMessageSend();
	}
	if (m != null) {
		// like super but using the CompletionOnMessageSend* created above
		int length;
		if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
			this.expressionPtr -= length;
			System.arraycopy(
					this.expressionStack,
					this.expressionPtr + 1,
					m.arguments = new Expression[length],
					0,
					length);
		}
		this.assistNode = m;
		return m;
	}
	return null;
}
@Override
protected AllocationExpression newAllocationExpression(boolean isQualified) {
	if (this.assistNode != null && this.lParenPos == this.assistNode.sourceEnd) {
		CompletionOnQualifiedAllocationExpression allocation = new CompletionOnQualifiedAllocationExpression();
		this.assistNode = allocation;
		return allocation;
	}
	return super.newAllocationExpression(isQualified);
}
public CompilationUnitDeclaration parse(ICompilationUnit sourceUnit, CompilationResult compilationResult, int cursorLoc) {

	this.cursorLocation = cursorLoc;
	CompletionScanner completionScanner = (CompletionScanner)this.scanner;
	completionScanner.completionIdentifier = null;
	completionScanner.cursorLocation = cursorLoc;
	return this.parse(sourceUnit, compilationResult);
}
@Override
public void parseBlockStatements(AbstractMethodDeclaration md, CompilationUnitDeclaration unit) {
	if (md.arguments != null) {
		for (Argument argument : md.arguments) {
			if (argument instanceof CompletionOnArgumentName && argument == this.assistNode)
				return; // no need to parse more
		}
	}
	super.parseBlockStatements(md, unit);
	// if the assist node is parsed at the cursor location or the cursor is within the assist node,
	// then ignore syntax errors found due to chars such as . and (
	if((md.bits & ASTNode.HasSyntaxErrors) != 0 && this.lastAct == ERROR_ACTION
			&& this.assistNode != null && this.assistNode.sourceEnd >= this.cursorLocation) {
		md.bits &= ~ASTNode.HasSyntaxErrors;
	}
}
@Override
public void parseBlockStatements(
	ConstructorDeclaration cd,
	CompilationUnitDeclaration unit) {
	this.canBeExplicitConstructor = 1;
	if (cd.arguments != null) {
		for (Argument argument : cd.arguments) {
			if (argument instanceof CompletionOnArgumentName && argument == this.assistNode)
				return; // no need to parse more
		}
	}
	super.parseBlockStatements(cd, unit);
}
public MethodDeclaration parseSomeStatements(int start, int end, int fakeBlocksCount, CompilationUnitDeclaration unit) {
	this.methodRecoveryActivated = true;

	initialize();

	// simulate goForMethodBody except that we don't want to balance brackets because they are not going to be balanced
	goForBlockStatementsopt();

	MethodDeclaration fakeMethod = new MethodDeclaration(unit.compilationResult());
	fakeMethod.selector = FAKE_METHOD_NAME;
	fakeMethod.bodyStart = start;
	fakeMethod.bodyEnd = end;
	fakeMethod.declarationSourceStart = start;
	fakeMethod.declarationSourceEnd = end;
	fakeMethod.sourceStart = start;
	fakeMethod.sourceEnd = start; //fake method must ignore the method header

	this.referenceContext = fakeMethod;
	this.compilationUnit = unit;

	this.diet = false;
	this.restartRecovery = true;

	this.scanner.resetTo(start, end);
	consumeNestedMethod();
	for (int i = 0; i < fakeBlocksCount; i++) {
		consumeOpenFakeBlock();
	}
	try {
		parse();
	} catch (AbortCompilation ex) {
		this.lastAct = ERROR_ACTION;
	} finally {
		this.nestedMethod[this.nestedType]--;
	}
	if (!this.hasError) {
		int length;
		if (this.astLengthPtr > -1 && (length = this.astLengthStack[this.astLengthPtr--]) != 0) {
			System.arraycopy(
				this.astStack,
				(this.astPtr -= length) + 1,
				fakeMethod.statements = new Statement[length],
				0,
				length);
		}
	}

	return fakeMethod;
}
protected void popUntilCompletedAnnotationIfNecessary() {
	if(this.elementPtr < 0) return;

	int i = this.elementPtr;
	while(i > -1 &&
			(this.elementKindStack[i] != K_BETWEEN_ANNOTATION_NAME_AND_RPAREN ||
					(this.elementInfoStack[i] & ANNOTATION_NAME_COMPLETION) == 0)) {
		i--;
	}

	if(i >= 0) {
		this.previousKind = this.elementKindStack[i];
		this.previousInfo = this.elementInfoStack[i];
		this.previousObjectInfo = this.elementObjectInfoStack[i];

		for (int j = i; j <= this.elementPtr; j++) {
			this.elementObjectInfoStack[j] = null;
		}

		this.elementPtr = i - 1;
	}
}
/*
 * Prepares the state of the parser to go for BlockStatements.
 */
@Override
protected void prepareForBlockStatements() {
	this.nestedMethod[this.nestedType = 0] = 1;
	this.variablesCounter[this.nestedType] = 0;
	this.realBlockStack[this.realBlockPtr = 1] = 0;

	initializeForBlockStatements();
}
protected void pushOnLabelStack(char[] label){
	if (this.labelPtr < -1) return;

	int stackLength = this.labelStack.length;
	if (++this.labelPtr >= stackLength) {
		System.arraycopy(
			this.labelStack, 0,
			this.labelStack = new char[stackLength + LabelStackIncrement][], 0,
			stackLength);
	}
	this.labelStack[this.labelPtr] = label;
}
/**
 * Creates a completion on member access node and push it
 * on the expression stack.
 */
private void pushCompletionOnMemberAccessOnExpressionStack(boolean isSuperAccess) {
	char[] source = this.identifierStack[this.identifierPtr];
	long pos = this.identifierPositionStack[this.identifierPtr--];
	CompletionOnMemberAccess fr = new CompletionOnMemberAccess(source, pos, isInsideAnnotation());
	this.assistNode = fr;
	this.lastCheckPoint = fr.sourceEnd + 1;
	this.identifierLengthPtr--;
	if (isSuperAccess) { //considerates the fieldReference beginning at the 'super' ....
		fr.sourceStart = this.intStack[this.intPtr--];
		fr.receiver = new SuperReference(fr.sourceStart, this.endPosition);
		pushOnExpressionStack(fr);
	} else { //optimize push/pop
		if ((fr.receiver = this.expressionStack[this.expressionPtr]).isThis()) { //fieldreference begins at the this
			fr.sourceStart = fr.receiver.sourceStart;
		}
		this.expressionStack[this.expressionPtr] = fr;
	}
}
private void recordReference(NameReference nameReference) {
	if (!this.skipRecord &&
			this.recordFrom <= nameReference.sourceStart &&
			nameReference.sourceEnd <= this.recordTo &&
			!isAlreadyPotentialName(nameReference.sourceStart)) {
		char[] token;
		if (nameReference instanceof SingleNameReference) {
			token = ((SingleNameReference) nameReference).token;
		} else {
			token = ((QualifiedNameReference) nameReference).tokens[0];
		}

		// Most of the time a name which start with an uppercase is a type name.
		// As we don't want to resolve names to avoid to slow down performances then this name will be ignored
		if (Character.isUpperCase(token[0])) return;

		addPotentialName(token, nameReference.sourceStart, nameReference.sourceEnd);
	}
}
@Override
public void recoveryExitFromVariable() {
	if(this.currentElement != null && this.currentElement instanceof RecoveredLocalVariable) {
		RecoveredElement oldElement = this.currentElement;
		super.recoveryExitFromVariable();
		if(oldElement != this.currentElement) {
			popElement(K_LOCAL_INITIALIZER_DELIMITER);
		}
	} else if(this.currentElement != null && this.currentElement instanceof RecoveredField) {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=292087
		// To make sure the array initializer is popped when the focus is shifted to the parent
		// in case we're restarting recovery inside an array initializer
		RecoveredElement oldElement = this.currentElement;
		super.recoveryExitFromVariable();
		if(oldElement != this.currentElement) {
			if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_ARRAY_INITIALIZER) {
				popElement(K_ARRAY_INITIALIZER);
				popElement(K_FIELD_INITIALIZER_DELIMITER);
			}
		}
	} else {
		super.recoveryExitFromVariable();
	}
}
@Override
public void recoveryTokenCheck() {
	RecoveredElement oldElement = this.currentElement;
	switch (this.currentToken) {
		case TokenNameLBRACE :
			if(!this.ignoreNextOpeningBrace) {
				this.pendingAnnotation = null; // the pending annotation cannot be attached to next nodes
			}
			super.recoveryTokenCheck();
			break;
		case TokenNameRBRACE :
			super.recoveryTokenCheck();
			if(this.currentElement != oldElement && oldElement instanceof RecoveredBlock) {
				if (topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_ARRAY_INITIALIZER) {
					// When inside an array initializer, we should not prematurely pop the enclosing block
					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=249704
					popElement(K_ARRAY_INITIALIZER);
				} else {
					popElement(K_BLOCK_DELIMITER);
				}
			}
			break;
		case TokenNamecase :
			super.recoveryTokenCheck();
			if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BLOCK_DELIMITER
				&& topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == SWITCH) {
				pushOnElementStack(K_SWITCH_LABEL);
			}
			break;
		case TokenNamedefault :
			super.recoveryTokenCheck();
			if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_BLOCK_DELIMITER
				&& topKnownElementInfo(COMPLETION_OR_ASSIST_PARSER) == SWITCH) {
				pushOnElementStack(K_SWITCH_LABEL, DEFAULT);
			} else if(topKnownElementKind(COMPLETION_OR_ASSIST_PARSER) == K_SWITCH_LABEL) {
				popElement(K_SWITCH_LABEL);
				pushOnElementStack(K_SWITCH_LABEL, DEFAULT);
			}
			break;
		default :
			super.recoveryTokenCheck();
			break;
	}
}

@Override
protected CompletionParser createSnapShotParser() {
	return new CompletionParser(this.problemReporter, this.storeSourceEnds);
}
/*
 * Reset internal state after completion is over
 */

@Override
public void reset() {
	super.reset();
	this.cursorLocation = 0;
	if (this.storeSourceEnds) {
		this.sourceEnds = new HashtableOfObjectToInt();
	}
}
/*
 * Reset internal state after completion is over
 */

public void resetAfterCompletion() {
	this.cursorLocation = 0;
	flushAssistState();
}
@Override
public void restoreAssistParser(Object parserState) {
	SavedState state = (SavedState) parserState;

	CompletionScanner completionScanner = (CompletionScanner)this.scanner;

	this.cursorLocation = state.parserCursorLocation;
	completionScanner.cursorLocation = state.scannerCursorLocation;
	this.assistNodeParent = state.assistNodeParent;
}
@Override
protected int resumeOnSyntaxError() {
	if (this.monitor != null) {
		if (++this.resumeOnSyntaxError > 100) {
			this.resumeOnSyntaxError = 0;
			if (this.monitor.isCanceled())
				return HALT;
		}
	}
	return super.resumeOnSyntaxError();
}
/*
 * Reset context so as to resume to regular parse loop
 * If unable to reset for resuming, answers false.
 *
 * Move checkpoint location, reset internal stacks and
 * decide which grammar goal is activated.
 */
@Override
protected int resumeAfterRecovery() {
	this.hasUnusedModifiers = false;
	if (this.assistNode != null) {

		if (requireExtendedRecovery()) {
			if (this.unstackedAct != ERROR_ACTION) {
				return RESUME;
			}
			return super.resumeAfterRecovery();
		}

		/* if reached [eof] inside method body, but still inside nested type,
			or inside a field initializer, should continue in diet mode until
			the end of the method body or compilation unit */
		if ((this.scanner.eofPosition >= this.cursorLocation+1)
			&& (!(this.referenceContext instanceof CompilationUnitDeclaration)
			|| isIndirectlyInsideFieldInitialization()
			|| this.assistNodeParent instanceof FieldDeclaration && !(this.assistNodeParent instanceof Initializer))) {

			/*	disabled since does not handle possible field/message refs, that is, Obj[ASSIST HERE]ect.registerNatives()
			// consume extra tokens which were part of the qualified reference
			//   so that the replaced source comprises them as well
			if (this.assistNode instanceof NameReference){
				int oldEof = scanner.eofPosition;
				scanner.eofPosition = currentElement.topElement().sourceEnd()+1;
				scanner.currentPosition = this.cursorLocation+1;
				int token = -1;
				try {
					do {
						// first token might not have to be a dot
						if (token >= 0 || !this.completionBehindDot){
							if ((token = scanner.getNextToken()) != TokenNameDOT) break;
						}
						if ((token = scanner.getNextToken()) != TokenNameIdentifier) break;
						this.assistNode.sourceEnd = scanner.currentPosition - 1;
					} while (token != TokenNameEOF);
				} catch (InvalidInputException e){
				} finally {
					scanner.eofPosition = oldEof;
				}
			}
			*/

			/* restart in diet mode for finding sibling constructs */
			if (this.currentElement instanceof RecoveredType
				|| this.currentElement.enclosingType() != null){

				this.pendingAnnotation = null;

				if(this.lastCheckPoint <= this.assistNode.sourceEnd) {
					this.lastCheckPoint = this.assistNode.sourceEnd+1;
				}
				int end = this.currentElement.topElement().sourceEnd();
				this.scanner.eofPosition = end < Integer.MAX_VALUE ? end + 1 : end;
			} else {
				resetStacks();
				return HALT;
			}
		}
	}
	return super.resumeAfterRecovery();
}
@Override
public void setAssistIdentifier(char[] assistIdent){
	((CompletionScanner)this.scanner).completionIdentifier = assistIdent;
}

@Override
protected void shouldStackAssistNode() {
	this.shouldStackAssistNode = true;
}

@Override
protected boolean assistNodeNeedsStacking() {
	return this.shouldStackAssistNode;
}

@Override
public  String toString() {
	StringBuffer buffer = new StringBuffer();
	buffer.append("elementKindStack : int[] = {"); //$NON-NLS-1$
	for (int i = 0; i <= this.elementPtr; i++) {
		buffer.append(String.valueOf(this.elementKindStack[i])).append(',');
	}
	buffer.append("}\n"); //$NON-NLS-1$
	buffer.append("elementInfoStack : int[] = {"); //$NON-NLS-1$
	for (int i = 0; i <= this.elementPtr; i++) {
		buffer.append(String.valueOf(this.elementInfoStack[i])).append(',');
	}
	buffer.append("}\n"); //$NON-NLS-1$
	buffer.append(super.toString());
	return String.valueOf(buffer);
}

/*
 * Update recovery state based on current parser/scanner state
 */
@Override
protected void updateRecoveryState() {

	/* expose parser state to recovery state */
	this.currentElement.updateFromParserState();

	// completionIdentifierCheck && attachOrphanCompletionNode pops various stacks to construct astNodeParent and enclosingNode. This does not gel well with extended recovery.
	AssistParser parser = null;
	if (lastIndexOfElement(K_LAMBDA_EXPRESSION_DELIMITER) >= 0) {
		parser = createSnapShotParser();
		parser.copyState(this);
	}

	/* may be able to retrieve completionNode as an orphan, and then attach it */
	completionIdentifierCheck();
	attachOrphanCompletionNode();
	if (parser != null)
		this.copyState(parser);

	// if an assist node has been found and a recovered element exists,
	// mark enclosing blocks as to be preserved
	if (this.assistNode != null && this.currentElement != null) {
		this.currentElement.preserveEnclosingBlocks();
	}

	/* check and update recovered state based on current token,
		this action is also performed when shifting token after recovery
		got activated once.
	*/
	recoveryTokenCheck();

	recoveryExitFromVariable();
}
@Override
protected CompilationUnitDeclaration endParse(int act) {
	CompilationUnitDeclaration cud = super.endParse(act);
	// in absence of a syntax error we never call attachOrphaneCompletionNode().
	// do the minimal stuff now:
	ASTNode topNode = (this.astPtr > -1) ? this.astStack[this.astPtr]
						: (this.referenceContext instanceof ASTNode) ? (ASTNode) this.referenceContext
						: null;
	if (topNode != null) {
		if (this.referenceContext instanceof AbstractMethodDeclaration) {
			Statement statement = null;
			if (topNode instanceof Statement) {
				statement = (Statement) topNode;
			} else if (this.assistNodeParent instanceof Statement) {
				statement = (Statement) this.assistNodeParent;
			} else if (this.assistNode instanceof Statement) {
				statement = (Statement) this.assistNode;
			}
			AbstractMethodDeclaration method = (AbstractMethodDeclaration) this.referenceContext;
			if (statement != null && isInsideBody(statement, method)
					&& this.assistNode != null && !CompletionNodeDetector.findAny(cud, this.assistNode))
			{
				// automaton ended right before transferring statements into the method?
				if (method.statements == null) {
					method.statements = new Statement[] { statement };
				} else if (this.currentElement != null) {
					// or before transferring statements into a nested recovered element?
					this.currentElement.add(statement, 0);
					RecoveredElement element = this.currentElement;
					while (element != null) {
						if (element instanceof RecoveredMethod && ((RecoveredMethod) element).methodDeclaration == method) {
							element.updateParseTree();
							break;
						}
						element = element.parent;
					}
				}
			}
		}
		if (this.assistNode != null
				&& !(this.assistNode instanceof CompletionOnKeyword) // no useful context information for keywords
				&& (this.assistNodeParent == null || this.enclosingNode == null))
		{
			CompletionNodeDetector detector =  new CompletionNodeDetector(this.assistNode, topNode);
			if(detector.containsCompletionNode()) {
				if (this.assistNodeParent == null)
					this.assistNodeParent = detector.getCompletionNodeParent();
				if (this.enclosingNode == null)
					this.enclosingNode = detector.getCompletionEnclosingNode();
			}
		}
	}
	return cud;
}
private boolean isInsideBody(Statement statement, AbstractMethodDeclaration method) {
	// avoid interpreting garbage before '{' as a method statement, e.g.:
	// void foo(int arg) foo bar zork { ...
	// foo bar zork are not inside the method body.
	if (statement.sourceStart < method.bodyStart)
		return false;
	// Note that diet parsing may not have found the '{' to properly set bodyStart, so the above check is not sufficient
	this.scanner.resetTo(method.sourceEnd, statement.sourceStart);
	try {
		int tkn;
		while ((tkn = this.scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
			if (tkn == TerminalTokens.TokenNameLBRACE)
				return true;
		}
	} catch (InvalidInputException e) {
		return true; // don't over-interpret the result.
	}
	return false;
}
@Override
protected LocalDeclaration createLocalDeclaration(char[] assistName, int sourceStart, int sourceEnd) {
	if (this.indexOfAssistIdentifier() < 0) {
		return super.createLocalDeclaration(assistName, sourceStart, sourceEnd);
	} else {
		CompletionOnLocalName local = new CompletionOnLocalName(assistName, sourceStart, sourceEnd);
		this.assistNode = local;
		this.lastCheckPoint = sourceEnd + 1;
		return local;
	}
}

@Override
protected JavadocParser createJavadocParser() {
	return new CompletionJavadocParser(this);
}

@Override
protected FieldDeclaration createFieldDeclaration(char[] assistName, int sourceStart, int sourceEnd) {
	if (this.indexOfAssistIdentifier() < 0 || (this.currentElement instanceof RecoveredUnit && ((RecoveredUnit)this.currentElement).typeCount == 0)) {
		return super.createFieldDeclaration(assistName, sourceStart, sourceEnd);
	} else {
		CompletionOnFieldName field = new CompletionOnFieldName(assistName, sourceStart, sourceEnd);
		this.assistNode = field;
		this.lastCheckPoint = sourceEnd + 1;
		return field;
	}
}

/*
 * To find out if the given stack has an instanceof expression
 * at the given startIndex or at one prior to that
 */
private boolean stackHasInstanceOfExpression(Object[] stackToSearch, int startIndex) {
	int indexInstanceOf = startIndex;
	while (indexInstanceOf >= 0) {
		if (stackToSearch[indexInstanceOf] instanceof InstanceOfExpression) {
			return true;
		}
		indexInstanceOf--;
	}
	return false;
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=292087
@Override
protected boolean isInsideArrayInitializer(){
	int i = this.elementPtr;
	if (i > -1 && this.elementKindStack[i] == K_ARRAY_INITIALIZER) {
		return true;
	}
	return false;
}
private boolean foundToken(int token) {
	int i = this.elementPtr;
	while (i > -1) {
		if (this.elementKindStack[i] == token) {
			return true;
		}
		i--;
	}
	return false;
}
@Override
protected int actFromTokenOrSynthetic(int previousAct) {
	int newAct = tAction(previousAct, this.currentToken);
	if (this.hasError && !this.diet && newAct == ERROR_ACTION && this.scanner.currentPosition > this.cursorLocation) {
		if (requireExtendedRecovery()) {
			// during extended recovery, if EOF would be wrong, try a few things to reduce our stacks:
			for (int tok : RECOVERY_TOKENS) {
				newAct = tAction(previousAct, tok);
				if (newAct != ERROR_ACTION) {
					this.currentToken = tok; // this worked, pretend we really got this from the Scanner
					return newAct;
				}
			}
			// recovery tokens wouldn't help, so let's initiate the final phase
			this.currentToken = TerminalTokens.TokenNameEOF;
			return tAction(previousAct, this.currentToken);
		}
	}
	return newAct;
}

protected boolean isInImportStatement() {
	return foundToken(K_INSIDE_IMPORT_STATEMENT);
}
protected boolean isInExportsStatement() {
	return foundToken(K_INSIDE_EXPORTS_STATEMENT);
}
protected boolean isInOpensStatement() {
	return foundToken(K_INSIDE_OPENS_STATEMENT);
}
protected boolean isInRequiresStatement() {
	return foundToken(K_INSIDE_REQUIRES_STATEMENT);
}
protected boolean isInUsesStatement() {
	return foundToken(K_INSIDE_USES_STATEMENT);
}
protected boolean isInProvidesStatement() {
	return foundToken(K_INSIDE_PROVIDES_STATEMENT);
}
protected boolean isAfterWithClause() {
	return foundToken(K_AFTER_WITH_IN_PROVIDES_STATEMENT);
}
protected boolean isInModuleStatements() {
	return isInExportsStatement() ||
			isInOpensStatement() ||
			isInRequiresStatement() ||
			isInProvidesStatement() ||
			isInUsesStatement();
}
}
