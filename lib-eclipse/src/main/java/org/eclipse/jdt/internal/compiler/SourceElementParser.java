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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.*;
import org.eclipse.jdt.internal.compiler.impl.*;
import org.eclipse.jdt.core.compiler.*;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.problem.*;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObjectToInt;
import org.eclipse.jdt.internal.core.util.CommentRecorderParser;
import org.eclipse.jdt.internal.core.util.Messages;

/**
 * A source element parser extracts structural and reference information
 * from a piece of source.
 *
 * also see @ISourceElementRequestor
 *
 * The structural investigation includes:
 * - the package statement
 * - import statements
 * - top-level types: package member, member types (member types of member types...)
 * - fields
 * - methods
 *
 * If reference information is requested, then all source constructs are
 * investigated and type, field & method references are provided as well.
 *
 * Any (parsing) problem encountered is also provided.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SourceElementParser extends CommentRecorderParser {

	ISourceElementRequestor requestor;
	boolean reportReferenceInfo;
	boolean reportLocalDeclarations;
	HashtableOfObjectToInt sourceEnds = new HashtableOfObjectToInt();
	HashMap nodesToCategories = new HashMap(); // a map from ASTNode to char[][]
	boolean useSourceJavadocParser = true;

	SourceElementNotifier notifier;

public SourceElementParser(
		final ISourceElementRequestor requestor,
		IProblemFactory problemFactory,
		CompilerOptions options,
		boolean reportLocalDeclarations,
		boolean optimizeStringLiterals) {
	this(requestor, problemFactory, options, reportLocalDeclarations, optimizeStringLiterals, true/* use SourceJavadocParser */);
}

public SourceElementParser(
		ISourceElementRequestor requestor,
		IProblemFactory problemFactory,
		CompilerOptions options,
		boolean reportLocalDeclarations,
		boolean optimizeStringLiterals,
		boolean useSourceJavadocParser) {

	super(
		new ProblemReporter(
			DefaultErrorHandlingPolicies.exitAfterAllProblems(),
			options,
			problemFactory),
		optimizeStringLiterals);

	this.reportLocalDeclarations = reportLocalDeclarations;

	// we want to notify all syntax error with the acceptProblem API
	// To do so, we define the record method of the ProblemReporter
	this.problemReporter = new ProblemReporter(
		DefaultErrorHandlingPolicies.exitAfterAllProblems(),
		options,
		problemFactory) {
		@Override
		public void record(CategorizedProblem problem, CompilationResult unitResult, ReferenceContext context, boolean mandatoryError) {
			unitResult.record(problem, context, mandatoryError); // TODO (jerome) clients are trapping problems either through factory or requestor... is result storing needed?
			SourceElementParser.this.requestor.acceptProblem(problem);
		}
	};
	this.requestor = requestor;
	this.options = options;

	this.notifier = new SourceElementNotifier(this.requestor, reportLocalDeclarations);

	// set specific javadoc parser
	this.useSourceJavadocParser = useSourceJavadocParser;
	if (useSourceJavadocParser) {
		this.javadocParser = new SourceJavadocParser(this);
	}
}
private void acceptJavadocTypeReference(Expression expression) {
	if (expression instanceof JavadocSingleTypeReference) {
		JavadocSingleTypeReference singleRef = (JavadocSingleTypeReference) expression;
		this.requestor.acceptTypeReference(singleRef.token, singleRef.sourceStart);
	} else if (expression instanceof JavadocQualifiedTypeReference) {
		JavadocQualifiedTypeReference qualifiedRef = (JavadocQualifiedTypeReference) expression;
		this.requestor.acceptTypeReference(qualifiedRef.tokens, qualifiedRef.sourceStart, qualifiedRef.sourceEnd);
	} else if (expression instanceof JavadocModuleReference) {
		Expression exp = ((JavadocModuleReference) expression).getTypeReference();
		if (exp != null) {
			acceptJavadocTypeReference(exp);
		}
	}
}
public void addUnknownRef(NameReference nameRef) {
	// Note that:
	// - the only requestor interested in references is the SourceIndexerRequestor
	// - a name reference can become a type reference only during the cast case, it is then tagged later with the Binding.TYPE bit
	// However since the indexer doesn't make the distinction between name reference and type reference, there is no need
	// to report a type reference in the SourceElementParser.
	// This gained 3.7% in the indexing performance test.
	if (nameRef instanceof SingleNameReference) {
		this.requestor.acceptUnknownReference(((SingleNameReference) nameRef).token, nameRef.sourceStart);
	} else {
		//QualifiedNameReference
		this.requestor.acceptUnknownReference(((QualifiedNameReference) nameRef).tokens, nameRef.sourceStart, nameRef.sourceEnd);
	}
}
@Override
public void checkComment() {
	int lastComment = getCommentPtr();
	// discard obsolete comments while inside methods or fields initializer (see bug 74369)
	if (!(this.diet && this.dietInt==0) && lastComment >= 0) {
		flushCommentsDefinedPriorTo(this.endStatementPosition);
		lastComment = getCommentPtr();
	}

	if (this.modifiersSourceStart >= 0 && this.modifiersSourceStart > this.annotationAsModifierSourceStart) {
		// eliminate comments located after modifierSourceStart if positioned
		while (lastComment >= 0) {
			int commentSourceStart = this.scanner.commentStarts[lastComment];
			if (commentSourceStart < 0) commentSourceStart = -commentSourceStart;
			if (commentSourceStart <= this.modifiersSourceStart) break;
			lastComment--;
		}
	}
	if (lastComment >= 0) {
		int lastCommentStart = this.scanner.commentStarts[0];
		if (lastCommentStart < 0) lastCommentStart = -lastCommentStart;
		if (this.forStartPosition == 0 || this.forStartPosition < lastCommentStart) {
			// consider all remaining leading comments to be part of current declaration
			this.modifiersSourceStart = lastCommentStart;
		}

		// check deprecation in last comment if javadoc (can be followed by non-javadoc comments which are simply ignored)
		while (lastComment >= 0 && this.scanner.commentStops[lastComment] < 0) lastComment--; // non javadoc comment have negative end positions
		if (lastComment >= 0 && this.javadocParser != null) {
			int commentEnd = this.scanner.commentStops[lastComment] - 1; //stop is one over
			// do not report problem before last parsed comment while recovering code...
			if (this.javadocParser.shouldReportProblems) {
				this.javadocParser.reportProblems = this.currentElement == null || commentEnd > this.lastJavadocEnd;
			} else {
				this.javadocParser.reportProblems = false;
			}
			if (this.javadocParser.checkDeprecation(lastComment)) {
				checkAndSetModifiers(ClassFileConstants.AccDeprecated);
			}
			this.javadoc = this.javadocParser.docComment;	// null if check javadoc is not activated
			if (this.currentElement == null) this.lastJavadocEnd = commentEnd;
		}
	}

	if (this.reportReferenceInfo && this.javadocParser.checkDocComment && this.javadoc != null) {
		// Report reference info in javadoc comment @throws/@exception tags
		TypeReference[] thrownExceptions = this.javadoc.exceptionReferences;
		if (thrownExceptions != null) {
			for (int i = 0, max=thrownExceptions.length; i < max; i++) {
				TypeReference typeRef = thrownExceptions[i];
				if (typeRef instanceof JavadocSingleTypeReference) {
					JavadocSingleTypeReference singleRef = (JavadocSingleTypeReference) typeRef;
					this.requestor.acceptTypeReference(singleRef.token, singleRef.sourceStart);
				} else if (typeRef instanceof JavadocQualifiedTypeReference) {
					JavadocQualifiedTypeReference qualifiedRef = (JavadocQualifiedTypeReference) typeRef;
					this.requestor.acceptTypeReference(qualifiedRef.tokens, qualifiedRef.sourceStart, qualifiedRef.sourceEnd);
				}
			}
		}

		// Report reference info in javadoc comment @see tags
		Expression[] references = this.javadoc.seeReferences;
		if (references != null) {
			for (int i = 0, max=references.length; i < max; i++) {
				Expression reference = references[i];
				acceptJavadocTypeReference(reference);
				if (reference instanceof JavadocFieldReference) {
					JavadocFieldReference fieldRef = (JavadocFieldReference) reference;
					this.requestor.acceptFieldReference(fieldRef.token, fieldRef.sourceStart);
					if (fieldRef.receiver != null && !fieldRef.receiver.isThis()) {
						acceptJavadocTypeReference(fieldRef.receiver);
					}
				} else if (reference instanceof JavadocMessageSend) {
					JavadocMessageSend messageSend = (JavadocMessageSend) reference;
					int argCount = messageSend.arguments == null ? 0 : messageSend.arguments.length;
					this.requestor.acceptMethodReference(messageSend.selector, argCount, messageSend.sourceStart);
					this.requestor.acceptConstructorReference(messageSend.selector, argCount, messageSend.sourceStart);
					if (messageSend.receiver != null && !messageSend.receiver.isThis()) {
						acceptJavadocTypeReference(messageSend.receiver);
					}
				} else if (reference instanceof JavadocAllocationExpression) {
					JavadocAllocationExpression constructor = (JavadocAllocationExpression) reference;
					int argCount = constructor.arguments == null ? 0 : constructor.arguments.length;
					if (constructor.type != null) {
						char[][] compoundName = constructor.type.getParameterizedTypeName();
						this.requestor.acceptConstructorReference(compoundName[compoundName.length-1], argCount, constructor.sourceStart);
						if (!constructor.type.isThis()) {
							acceptJavadocTypeReference(constructor.type);
						}
					}
				}
			}
		}
	}
}
@Override
protected void classInstanceCreation(boolean alwaysQualified) {

	boolean previousFlag = this.reportReferenceInfo;
	this.reportReferenceInfo = false; // not to see the type reference reported in super call to getTypeReference(...)
	super.classInstanceCreation(alwaysQualified);
	this.reportReferenceInfo = previousFlag;
	if (this.reportReferenceInfo){
		AllocationExpression alloc = (AllocationExpression)this.expressionStack[this.expressionPtr];
		TypeReference typeRef = alloc.type;
		this.requestor.acceptConstructorReference(
			typeRef instanceof SingleTypeReference
				? ((SingleTypeReference) typeRef).token
				: CharOperation.concatWith(alloc.type.getParameterizedTypeName(), '.'),
			alloc.arguments == null ? 0 : alloc.arguments.length,
			alloc.sourceStart);
	}
}
@Override
protected void consumeAnnotationAsModifier() {
	super.consumeAnnotationAsModifier();
	Annotation annotation = (Annotation)this.expressionStack[this.expressionPtr];
	if (this.reportReferenceInfo) { // accept annotation type reference
		this.requestor.acceptAnnotationTypeReference(annotation.type.getTypeName(), annotation.sourceStart, annotation.sourceEnd);
	}
}
@Override
protected void consumeClassInstanceCreationExpressionQualifiedWithTypeArguments() {
	boolean previousFlag = this.reportReferenceInfo;
	this.reportReferenceInfo = false; // not to see the type reference reported in super call to getTypeReference(...)
	super.consumeClassInstanceCreationExpressionQualifiedWithTypeArguments();
	this.reportReferenceInfo = previousFlag;
	if (this.reportReferenceInfo){
		AllocationExpression alloc = (AllocationExpression)this.expressionStack[this.expressionPtr];
		TypeReference typeRef = alloc.type;
		this.requestor.acceptConstructorReference(
			typeRef instanceof SingleTypeReference
				? ((SingleTypeReference) typeRef).token
				: CharOperation.concatWith(alloc.type.getParameterizedTypeName(), '.'),
			alloc.arguments == null ? 0 : alloc.arguments.length,
			alloc.sourceStart);
	}
}
@Override
protected void consumeAnnotationTypeDeclarationHeaderName() {
	int currentAstPtr = this.astPtr;
	super.consumeAnnotationTypeDeclarationHeaderName();
	if (this.astPtr > currentAstPtr) // if ast node was pushed on the ast stack
		rememberCategories();
}
@Override
protected void consumeAnnotationTypeDeclarationHeaderNameWithTypeParameters() {
	int currentAstPtr = this.astPtr;
	super.consumeAnnotationTypeDeclarationHeaderNameWithTypeParameters();
	if (this.astPtr > currentAstPtr) // if ast node was pushed on the ast stack
		rememberCategories();
}
@Override
protected void consumeCatchFormalParameter() {
	super.consumeCatchFormalParameter();

	// Flush comments prior to this formal parameter so the declarationSourceStart of the following parameter
	// is correctly set (see bug 80904)
	// Note that this could be done in the Parser itself, but this would slow down all parsers, when they don't need
	// the declarationSourceStart to be set
	flushCommentsDefinedPriorTo(this.scanner.currentPosition);
}
@Override
protected void consumeClassHeaderName1() {
	int currentAstPtr = this.astPtr;
	super.consumeClassHeaderName1();
	if (this.astPtr > currentAstPtr) // if ast node was pushed on the ast stack
		rememberCategories();
}
@Override
protected void consumeModuleHeader() {
	int currentAstPtr = this.astPtr;
	super.consumeModuleHeader();
	if (this.astPtr > currentAstPtr) // if ast node was pushed on the ast stack
		rememberCategories();
}
@Override
protected void consumeClassInstanceCreationExpressionWithTypeArguments() {
	boolean previousFlag = this.reportReferenceInfo;
	this.reportReferenceInfo = false; // not to see the type reference reported in super call to getTypeReference(...)
	super.consumeClassInstanceCreationExpressionWithTypeArguments();
	this.reportReferenceInfo = previousFlag;
	if (this.reportReferenceInfo){
		AllocationExpression alloc = (AllocationExpression)this.expressionStack[this.expressionPtr];
		TypeReference typeRef = alloc.type;
		this.requestor.acceptConstructorReference(
			typeRef instanceof SingleTypeReference
				? ((SingleTypeReference) typeRef).token
				: CharOperation.concatWith(alloc.type.getParameterizedTypeName(), '.'),
			alloc.arguments == null ? 0 : alloc.arguments.length,
			alloc.sourceStart);
	}
}
@Override
protected void consumeConstructorHeaderName() {
	long selectorSourcePositions = this.identifierPositionStack[this.identifierPtr];
	int selectorSourceEnd = (int) selectorSourcePositions;
	int currentAstPtr = this.astPtr;
	super.consumeConstructorHeaderName();
	if (this.astPtr > currentAstPtr) { // if ast node was pushed on the ast stack
		this.sourceEnds.put(this.astStack[this.astPtr], selectorSourceEnd);
		rememberCategories();
	}
}

@Override
protected void consumeCompactConstructorHeaderName() {
	long selectorSourcePositions = this.identifierPositionStack[this.identifierPtr];
	int selectorSourceEnd = (int) selectorSourcePositions;
	int currentAstPtr = this.astPtr;
	super.consumeCompactConstructorHeaderName();
	if (this.astPtr > currentAstPtr) { // if ast node was pushed on the ast stack
		this.sourceEnds.put(this.astStack[this.astPtr], selectorSourceEnd);
		rememberCategories();
	}
}


@Override
protected void consumeConstructorHeaderNameWithTypeParameters() {
	long selectorSourcePositions = this.identifierPositionStack[this.identifierPtr];
	int selectorSourceEnd = (int) selectorSourcePositions;
	int currentAstPtr = this.astPtr;
	super.consumeConstructorHeaderNameWithTypeParameters();
	if (this.astPtr > currentAstPtr) { // if ast node was pushed on the ast stack
		this.sourceEnds.put(this.astStack[this.astPtr], selectorSourceEnd);
		rememberCategories();
	}
}
@Override
protected void consumeEnumConstantWithClassBody() {
	super.consumeEnumConstantWithClassBody();
	if ((this.currentToken == TokenNameCOMMA || this.currentToken == TokenNameSEMICOLON)
			&& this.astStack[this.astPtr] instanceof FieldDeclaration) {
		this.sourceEnds.put(this.astStack[this.astPtr], this.scanner.currentPosition - 1);
		rememberCategories();
	}
}
@Override
protected void consumeEnumConstantNoClassBody() {
	super.consumeEnumConstantNoClassBody();
	if ((this.currentToken == TokenNameCOMMA || this.currentToken == TokenNameSEMICOLON)
			&& this.astStack[this.astPtr] instanceof FieldDeclaration) {
		this.sourceEnds.put(this.astStack[this.astPtr], this.scanner.currentPosition - 1);
		rememberCategories();
	}
}
@Override
protected void consumeEnumHeaderName() {
	int currentAstPtr = this.astPtr;
	super.consumeEnumHeaderName();
	if (this.astPtr > currentAstPtr) // if ast node was pushed on the ast stack
		rememberCategories();
}
@Override
protected void consumeEnumHeaderNameWithTypeParameters() {
	int currentAstPtr = this.astPtr;
	super.consumeEnumHeaderNameWithTypeParameters();
	if (this.astPtr > currentAstPtr) // if ast node was pushed on the ast stack
		rememberCategories();
}
@Override
protected void consumeExitVariableWithInitialization() {
	// ExitVariableWithInitialization ::= $empty
	// the scanner is located after the comma or the semi-colon.
	// we want to include the comma or the semi-colon
	super.consumeExitVariableWithInitialization();
	if ((this.currentToken == TokenNameCOMMA || this.currentToken == TokenNameSEMICOLON)
			&& this.astStack[this.astPtr] instanceof FieldDeclaration) {
		this.sourceEnds.put(this.astStack[this.astPtr], this.scanner.currentPosition - 1);
		rememberCategories();
	}
}
@Override
protected void consumeExitVariableWithoutInitialization() {
	// ExitVariableWithoutInitialization ::= $empty
	// do nothing by default
	super.consumeExitVariableWithoutInitialization();
	if ((this.currentToken == TokenNameCOMMA || this.currentToken == TokenNameSEMICOLON)
			&& this.astStack[this.astPtr] instanceof FieldDeclaration) {
		this.sourceEnds.put(this.astStack[this.astPtr], this.scanner.currentPosition - 1);
		rememberCategories();
	}
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeFieldAccess(boolean isSuperAccess) {
	// FieldAccess ::= Primary '.' 'Identifier'
	// FieldAccess ::= 'super' '.' 'Identifier'
	super.consumeFieldAccess(isSuperAccess);
	FieldReference fr = (FieldReference) this.expressionStack[this.expressionPtr];
	if (this.reportReferenceInfo) {
		this.requestor.acceptFieldReference(fr.token, fr.sourceStart);
	}
}
@Override
protected void consumeFormalParameter(boolean isVarArgs) {
	super.consumeFormalParameter(isVarArgs);

	// Flush comments prior to this formal parameter so the declarationSourceStart of the following parameter
	// is correctly set (see bug 80904)
	// Note that this could be done in the Parser itself, but this would slow down all parsers, when they don't need
	// the declarationSourceStart to be set
	flushCommentsDefinedPriorTo(this.scanner.currentPosition);
}
@Override
protected void consumeTypeElidedLambdaParameter(boolean parenthesized) {
	super.consumeTypeElidedLambdaParameter(parenthesized);
	flushCommentsDefinedPriorTo(this.scanner.currentPosition);
}
@Override
protected void consumeInterfaceHeaderName1() {
	int currentAstPtr = this.astPtr;
	super.consumeInterfaceHeaderName1();
	if (this.astPtr > currentAstPtr) // if ast node was pushed on the ast stack
		rememberCategories();
}
@Override
protected void consumeMemberValuePair() {
	super.consumeMemberValuePair();
	MemberValuePair memberValuepair = (MemberValuePair) this.astStack[this.astPtr];
	if (this.reportReferenceInfo) {
		this.requestor.acceptMethodReference(memberValuepair.name, 0, memberValuepair.sourceStart);
	}
}
@Override
protected void consumeMarkerAnnotation(boolean isTypeAnnotation) {
	super.consumeMarkerAnnotation(isTypeAnnotation);
	Annotation annotation = (Annotation) (isTypeAnnotation ? this.typeAnnotationStack[this.typeAnnotationPtr] : this.expressionStack[this.expressionPtr]);
	if (this.reportReferenceInfo) { // accept annotation type reference
		this.requestor.acceptAnnotationTypeReference(annotation.type.getTypeName(), annotation.sourceStart, annotation.sourceEnd);
	}
}
@Override
protected void consumeMethodHeaderName(boolean isAnnotationMethod) {
	long selectorSourcePositions = this.identifierPositionStack[this.identifierPtr];
	int selectorSourceEnd = (int) selectorSourcePositions;
	int currentAstPtr = this.astPtr;
	super.consumeMethodHeaderName(isAnnotationMethod);
	if (this.astPtr > currentAstPtr) { // if ast node was pushed on the ast stack
		this.sourceEnds.put(this.astStack[this.astPtr], selectorSourceEnd);
		rememberCategories();
	}
	flushCommentsDefinedPriorTo(this.scanner.currentPosition);
}

@Override
protected void consumeMethodHeaderNameWithTypeParameters(boolean isAnnotationMethod) {
	long selectorSourcePositions = this.identifierPositionStack[this.identifierPtr];
	int selectorSourceEnd = (int) selectorSourcePositions;
	int currentAstPtr = this.astPtr;
	super.consumeMethodHeaderNameWithTypeParameters(isAnnotationMethod);
	if (this.astPtr > currentAstPtr) // if ast node was pushed on the ast stack
		this.sourceEnds.put(this.astStack[this.astPtr], selectorSourceEnd);
		rememberCategories();
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeMethodInvocationName() {
	// MethodInvocation ::= Name '(' ArgumentListopt ')'
	super.consumeMethodInvocationName();

	// when the name is only an identifier...we have a message send to "this" (implicit)
	MessageSend messageSend = (MessageSend) this.expressionStack[this.expressionPtr];
	Expression[] args = messageSend.arguments;
	if (this.reportReferenceInfo) {
		this.requestor.acceptMethodReference(
			messageSend.selector,
			args == null ? 0 : args.length,
			(int)(messageSend.nameSourcePosition >>> 32));
	}
}
@Override
protected void consumeMethodInvocationNameWithTypeArguments() {
	// MethodInvocation ::= Name '.' TypeArguments 'Identifier' '(' ArgumentListopt ')'
	super.consumeMethodInvocationNameWithTypeArguments();

	// when the name is only an identifier...we have a message send to "this" (implicit)
	MessageSend messageSend = (MessageSend) this.expressionStack[this.expressionPtr];
	Expression[] args = messageSend.arguments;
	if (this.reportReferenceInfo) {
		this.requestor.acceptMethodReference(
			messageSend.selector,
			args == null ? 0 : args.length,
			(int)(messageSend.nameSourcePosition >>> 32));
	}
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeMethodInvocationPrimary() {
	super.consumeMethodInvocationPrimary();
	MessageSend messageSend = (MessageSend) this.expressionStack[this.expressionPtr];
	Expression[] args = messageSend.arguments;
	if (this.reportReferenceInfo) {
		this.requestor.acceptMethodReference(
			messageSend.selector,
			args == null ? 0 : args.length,
			(int)(messageSend.nameSourcePosition >>> 32));
	}
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeMethodInvocationPrimaryWithTypeArguments() {
	super.consumeMethodInvocationPrimaryWithTypeArguments();
	MessageSend messageSend = (MessageSend) this.expressionStack[this.expressionPtr];
	Expression[] args = messageSend.arguments;
	if (this.reportReferenceInfo) {
		this.requestor.acceptMethodReference(
			messageSend.selector,
			args == null ? 0 : args.length,
			(int)(messageSend.nameSourcePosition >>> 32));
	}
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeMethodInvocationSuper() {
	// MethodInvocation ::= 'super' '.' 'Identifier' '(' ArgumentListopt ')'
	super.consumeMethodInvocationSuper();
	MessageSend messageSend = (MessageSend) this.expressionStack[this.expressionPtr];
	Expression[] args = messageSend.arguments;
	if (this.reportReferenceInfo) {
		this.requestor.acceptMethodReference(
			messageSend.selector,
			args == null ? 0 : args.length,
			(int)(messageSend.nameSourcePosition >>> 32));
	}
}
@Override
protected void consumeMethodInvocationSuperWithTypeArguments() {
	// MethodInvocation ::= 'super' '.' TypeArguments 'Identifier' '(' ArgumentListopt ')'
	super.consumeMethodInvocationSuperWithTypeArguments();
	MessageSend messageSend = (MessageSend) this.expressionStack[this.expressionPtr];
	Expression[] args = messageSend.arguments;
	if (this.reportReferenceInfo) {
		this.requestor.acceptMethodReference(
			messageSend.selector,
			args == null ? 0 : args.length,
			(int)(messageSend.nameSourcePosition >>> 32));
	}
}
@Override
protected void consumeNormalAnnotation(boolean isTypeAnnotation) {
	super.consumeNormalAnnotation(isTypeAnnotation);
	Annotation annotation = (Annotation) (isTypeAnnotation ? this.typeAnnotationStack[this.typeAnnotationPtr] : this.expressionStack[this.expressionPtr]);
	if (this.reportReferenceInfo) { // accept annotation type reference
		this.requestor.acceptAnnotationTypeReference(annotation.type.getTypeName(), annotation.sourceStart, annotation.sourceEnd);
	}
}
@Override
protected void consumeProvidesStatement() {
	super.consumeProvidesStatement();
	ProvidesStatement service = (ProvidesStatement) this.astStack[this.astPtr];
	TypeReference ref = service.serviceInterface;
	this.requestor.acceptTypeReference(ref.getTypeName(), ref.sourceStart, ref.sourceEnd);
}
@Override
protected void consumeSingleMemberAnnotation(boolean isTypeAnnotation) {
	super.consumeSingleMemberAnnotation(isTypeAnnotation);
	SingleMemberAnnotation member = (SingleMemberAnnotation) (isTypeAnnotation ? this.typeAnnotationStack[this.typeAnnotationPtr] : this.expressionStack[this.expressionPtr]);
	if (this.reportReferenceInfo) {
		this.requestor.acceptAnnotationTypeReference(member.type.getTypeName(), member.sourceStart, member.sourceEnd);
		this.requestor.acceptMethodReference(TypeConstants.VALUE, 0, member.sourceStart);
	}
}
@Override
protected void consumeSingleStaticImportDeclarationName() {
	// SingleTypeImportDeclarationName ::= 'import' 'static' Name
	ImportReference impt;
	int length;
	char[][] tokens = new char[length = this.identifierLengthStack[this.identifierLengthPtr--]][];
	this.identifierPtr -= length;
	long[] positions = new long[length];
	System.arraycopy(this.identifierStack, this.identifierPtr + 1, tokens, 0, length);
	System.arraycopy(this.identifierPositionStack, this.identifierPtr + 1, positions, 0, length);
	pushOnAstStack(impt = newImportReference(tokens, positions, false, ClassFileConstants.AccStatic));

	this.modifiers = ClassFileConstants.AccDefault;
	this.modifiersSourceStart = -1; // <-- see comment into modifiersFlag(int)

	if (this.currentToken == TokenNameSEMICOLON){
		impt.declarationSourceEnd = this.scanner.currentPosition - 1;
	} else {
		impt.declarationSourceEnd = impt.sourceEnd;
	}
	impt.declarationEnd = impt.declarationSourceEnd;
	//this.endPosition is just before the ;
	impt.declarationSourceStart = this.intStack[this.intPtr--];

	if(!this.statementRecoveryActivated &&
			this.options.sourceLevel < ClassFileConstants.JDK1_5 &&
			this.lastErrorEndPositionBeforeRecovery < this.scanner.currentPosition) {
		impt.modifiers = ClassFileConstants.AccDefault; // convert the static import reference to a non-static importe reference
		problemReporter().invalidUsageOfStaticImports(impt);
	}

	// recovery
	if (this.currentElement != null){
		this.lastCheckPoint = impt.declarationSourceEnd+1;
		this.currentElement = this.currentElement.add(impt, 0);
		this.lastIgnoredToken = -1;
		this.restartRecovery = true; // used to avoid branching back into the regular automaton
	}
	if (this.reportReferenceInfo) {
		// Name for static import is TypeName '.' Identifier
		// => accept unknown ref on identifier
		int tokensLength = impt.tokens.length-1;
		int start = (int) (impt.sourcePositions[tokensLength] >>> 32);
		char[] last = impt.tokens[tokensLength];
		// accept all possible kind for last name, index users will have to select the right one...
		// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=86901
		this.requestor.acceptFieldReference(last, start);
		this.requestor.acceptMethodReference(last, 0,start);
		this.requestor.acceptTypeReference(last, start);
		// accept type name
		if (tokensLength > 0) {
			char[][] compoundName = new char[tokensLength][];
			System.arraycopy(impt.tokens, 0, compoundName, 0, tokensLength);
			int end = (int) impt.sourcePositions[tokensLength-1];
			this.requestor.acceptTypeReference(compoundName, impt.sourceStart, end);
		}
	}
}

@Override
protected void consumeSingleTypeImportDeclarationName() {
	// SingleTypeImportDeclarationName ::= 'import' Name
	/* push an ImportRef build from the last name
	stored in the identifier stack. */

	ImportReference impt;
	int length;
	char[][] tokens = new char[length = this.identifierLengthStack[this.identifierLengthPtr--]][];
	this.identifierPtr -= length;
	long[] positions = new long[length];
	System.arraycopy(this.identifierStack, this.identifierPtr + 1, tokens, 0, length);
	System.arraycopy(this.identifierPositionStack, this.identifierPtr + 1, positions, 0, length);
	pushOnAstStack(impt = newImportReference(tokens, positions, false, ClassFileConstants.AccDefault));

	if (this.currentToken == TokenNameSEMICOLON){
		impt.declarationSourceEnd = this.scanner.currentPosition - 1;
	} else {
		impt.declarationSourceEnd = impt.sourceEnd;
	}
	impt.declarationEnd = impt.declarationSourceEnd;
	//this.endPosition is just before the ;
	impt.declarationSourceStart = this.intStack[this.intPtr--];

	// recovery
	if (this.currentElement != null){
		this.lastCheckPoint = impt.declarationSourceEnd+1;
		this.currentElement = this.currentElement.add(impt, 0);
		this.lastIgnoredToken = -1;
		this.restartRecovery = true; // used to avoid branching back into the regular automaton
	}
	if (this.reportReferenceInfo) {
		this.requestor.acceptTypeReference(impt.tokens, impt.sourceStart, impt.sourceEnd);
	}
}
@Override
protected void consumeStaticImportOnDemandDeclarationName() {
	// TypeImportOnDemandDeclarationName ::= 'import' 'static' Name '.' '*'
	/* push an ImportRef build from the last name
	stored in the identifier stack. */

	ImportReference impt;
	int length;
	char[][] tokens = new char[length = this.identifierLengthStack[this.identifierLengthPtr--]][];
	this.identifierPtr -= length;
	long[] positions = new long[length];
	System.arraycopy(this.identifierStack, this.identifierPtr + 1, tokens, 0, length);
	System.arraycopy(this.identifierPositionStack, this.identifierPtr + 1, positions, 0, length);
	pushOnAstStack(impt = new ImportReference(tokens, positions, true, ClassFileConstants.AccStatic));

	// star end position
	impt.trailingStarPosition = this.intStack[this.intPtr--];
	this.modifiers = ClassFileConstants.AccDefault;
	this.modifiersSourceStart = -1; // <-- see comment into modifiersFlag(int)

	if (this.currentToken == TokenNameSEMICOLON){
		impt.declarationSourceEnd = this.scanner.currentPosition - 1;
	} else {
		impt.declarationSourceEnd = impt.sourceEnd;
	}
	impt.declarationEnd = impt.declarationSourceEnd;
	//this.endPosition is just before the ;
	impt.declarationSourceStart = this.intStack[this.intPtr--];

	if(!this.statementRecoveryActivated &&
			this.options.sourceLevel < ClassFileConstants.JDK1_5 &&
			this.lastErrorEndPositionBeforeRecovery < this.scanner.currentPosition) {
		impt.modifiers = ClassFileConstants.AccDefault; // convert the static import reference to a non-static importe reference
		problemReporter().invalidUsageOfStaticImports(impt);
	}

	// recovery
	if (this.currentElement != null){
		this.lastCheckPoint = impt.declarationSourceEnd+1;
		this.currentElement = this.currentElement.add(impt, 0);
		this.lastIgnoredToken = -1;
		this.restartRecovery = true; // used to avoid branching back into the regular automaton
	}
	if (this.reportReferenceInfo) {
		this.requestor.acceptTypeReference(impt.tokens, impt.sourceStart, impt.sourceEnd);
	}
}
@Override
protected void consumeTypeImportOnDemandDeclarationName() {
	// TypeImportOnDemandDeclarationName ::= 'import' Name '.' '*'
	/* push an ImportRef build from the last name
	stored in the identifier stack. */

	ImportReference impt;
	int length;
	char[][] tokens = new char[length = this.identifierLengthStack[this.identifierLengthPtr--]][];
	this.identifierPtr -= length;
	long[] positions = new long[length];
	System.arraycopy(this.identifierStack, this.identifierPtr + 1, tokens, 0, length);
	System.arraycopy(this.identifierPositionStack, this.identifierPtr + 1, positions, 0, length);
	pushOnAstStack(impt = new ImportReference(tokens, positions, true, ClassFileConstants.AccDefault));

	// star end position
	impt.trailingStarPosition = this.intStack[this.intPtr--];
	if (this.currentToken == TokenNameSEMICOLON){
		impt.declarationSourceEnd = this.scanner.currentPosition - 1;
	} else {
		impt.declarationSourceEnd = impt.sourceEnd;
	}
	impt.declarationEnd = impt.declarationSourceEnd;
	//this.endPosition is just before the ;
	impt.declarationSourceStart = this.intStack[this.intPtr--];

	// recovery
	if (this.currentElement != null){
		this.lastCheckPoint = impt.declarationSourceEnd+1;
		this.currentElement = this.currentElement.add(impt, 0);
		this.lastIgnoredToken = -1;
		this.restartRecovery = true; // used to avoid branching back into the regular automaton
	}
	if (this.reportReferenceInfo) {
		this.requestor.acceptUnknownReference(impt.tokens, impt.sourceStart, impt.sourceEnd);
	}
}
@Override
protected void consumeUsesStatement() {
	super.consumeUsesStatement();
	UsesStatement ref = (UsesStatement) this.astStack[this.astPtr];
	this.requestor.acceptTypeReference(ref.serviceInterface.getTypeName(), ref.sourceStart, ref.sourceEnd);
}
@Override
protected void consumeWithClause() {
	super.consumeWithClause();
	ProvidesStatement service = (ProvidesStatement) this.astStack[this.astPtr];
		for (int i = 0; i < service.implementations.length; i++) {
			TypeReference ref = service.implementations[i];
			this.requestor.acceptTypeReference(ref.getTypeName(), ref.sourceStart, ref.sourceEnd);
		}
}
@Override
public MethodDeclaration convertToMethodDeclaration(ConstructorDeclaration c, CompilationResult compilationResult) {
	MethodDeclaration methodDeclaration = super.convertToMethodDeclaration(c, compilationResult);
	int selectorSourceEnd = this.sourceEnds.removeKey(c);
	if (selectorSourceEnd != -1)
		this.sourceEnds.put(methodDeclaration, selectorSourceEnd);
	char[][] categories =  (char[][]) this.nodesToCategories.remove(c);
	if (categories != null)
		this.nodesToCategories.put(methodDeclaration, categories);

	return methodDeclaration;
}
@Override
protected CompilationUnitDeclaration endParse(int act) {
	if (this.scanner.recordLineSeparator) {
		this.requestor.acceptLineSeparatorPositions(this.scanner.getLineEnds());
	}
	if (this.compilationUnit != null) {
		CompilationUnitDeclaration result = super.endParse(act);
		return result;
	} else {
		return null;
	}
}
@Override
public TypeReference getTypeReference(int dim) {
	/* build a Reference on a variable that may be qualified or not
	 * This variable is a type reference and dim will be its dimensions
	 */
	Annotation [][] annotationsOnDimensions = null;
	TypeReference ref;
	int length = this.identifierLengthStack[this.identifierLengthPtr--];
	if (length < 0) { //flag for precompiled type reference on base types
		annotationsOnDimensions = getAnnotationsOnDimensions(dim);
		ref = TypeReference.baseTypeReference(-length, dim, annotationsOnDimensions);
		ref.sourceStart = this.intStack[this.intPtr--];
		if (dim == 0) {
			ref.sourceEnd = this.intStack[this.intPtr--];
		} else {
			this.intPtr--; // no need to use this position as it is an array
			ref.sourceEnd = this.rBracketPosition;
		}
		if (this.reportReferenceInfo){
				this.requestor.acceptTypeReference(ref.getParameterizedTypeName(), ref.sourceStart, ref.sourceEnd);
		}
	} else {
		int numberOfIdentifiers = this.genericsIdentifiersLengthStack[this.genericsIdentifiersLengthPtr--];
		if (length != numberOfIdentifiers || this.genericsLengthStack[this.genericsLengthPtr] != 0) {
			// generic type
			ref = getTypeReferenceForGenericType(dim, length, numberOfIdentifiers);
			if (this.reportReferenceInfo) {
				if (length == 1 && numberOfIdentifiers == 1) {
					ParameterizedSingleTypeReference parameterizedSingleTypeReference = (ParameterizedSingleTypeReference) ref;
					this.requestor.acceptTypeReference(parameterizedSingleTypeReference.token, parameterizedSingleTypeReference.sourceStart);
				} else {
					ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference = (ParameterizedQualifiedTypeReference) ref;
					this.requestor.acceptTypeReference(parameterizedQualifiedTypeReference.tokens, parameterizedQualifiedTypeReference.sourceStart, parameterizedQualifiedTypeReference.sourceEnd);
				}
			}
		} else if (length == 1) {
			// single type reference
			this.genericsLengthPtr--; // pop the 0
			if (dim == 0) {
				ref =
					new SingleTypeReference(
						this.identifierStack[this.identifierPtr],
						this.identifierPositionStack[this.identifierPtr--]);
				if (this.reportReferenceInfo) {
					this.requestor.acceptTypeReference(((SingleTypeReference)ref).token, ref.sourceStart);
				}
			} else {
				annotationsOnDimensions = getAnnotationsOnDimensions(dim);
				ref =
					new ArrayTypeReference(
						this.identifierStack[this.identifierPtr],
						dim,
						annotationsOnDimensions,
						this.identifierPositionStack[this.identifierPtr--]);
				ref.sourceEnd = this.endPosition;
				if (annotationsOnDimensions != null) {
					ref.bits |= ASTNode.HasTypeAnnotations;
				}
				if (this.reportReferenceInfo) {
					this.requestor.acceptTypeReference(((ArrayTypeReference)ref).token, ref.sourceStart);
				}
			}
		} else { // Qualified type reference
			this.genericsLengthPtr--;
			char[][] tokens = new char[length][];
			this.identifierPtr -= length;
			long[] positions = new long[length];
			System.arraycopy(this.identifierStack, this.identifierPtr + 1, tokens, 0, length);
			System.arraycopy(
				this.identifierPositionStack,
				this.identifierPtr + 1,
				positions,
				0,
				length);
			if (dim == 0) {
				ref = new QualifiedTypeReference(tokens, positions);
				if (this.reportReferenceInfo) {
					this.requestor.acceptTypeReference(((QualifiedTypeReference)ref).tokens, ref.sourceStart, ref.sourceEnd);
				}
			} else {
				annotationsOnDimensions = getAnnotationsOnDimensions(dim);
				ref =
					new ArrayQualifiedTypeReference(tokens, dim, annotationsOnDimensions, positions);
				ref.sourceEnd = this.endPosition;
				if (annotationsOnDimensions != null) {
					ref.bits |= ASTNode.HasTypeAnnotations;
				}
				if (this.reportReferenceInfo) {
					this.requestor.acceptTypeReference(((ArrayQualifiedTypeReference)ref).tokens, ref.sourceStart, ref.sourceEnd);
				}
			}
		}
	}
	int levels = ref.getAnnotatableLevels();
	for (int i = levels - 1; i >= 0; i--) {
		if ((length = this.typeAnnotationLengthStack[this.typeAnnotationLengthPtr--]) != 0) {
			if (ref.annotations == null)
				ref.annotations = new Annotation[levels][];
			System.arraycopy(
					this.typeAnnotationStack,
					(this.typeAnnotationPtr -= length) + 1,
					ref.annotations[i] = new Annotation[length],
					0,
					length);
			if (i == 0) {
				ref.sourceStart = ref.annotations[0][0].sourceStart;
			}
			ref.bits |= ASTNode.HasTypeAnnotations;
		}
	}
	return ref;
}
@Override
public NameReference getUnspecifiedReference(boolean rejectTypeAnnotations) {
	/* build a (unspecified) NameReference which may be qualified*/
    if (rejectTypeAnnotations) {
    	consumeNonTypeUseName();
    }
	int length;
	if ((length = this.identifierLengthStack[this.identifierLengthPtr--]) == 1) {
		// single variable reference
		SingleNameReference ref =
			newSingleNameReference(
				this.identifierStack[this.identifierPtr],
				this.identifierPositionStack[this.identifierPtr--]);
		if (this.reportReferenceInfo) {
			addUnknownRef(ref);
		}
		return ref;
	} else {
		//Qualified variable reference
		char[][] tokens = new char[length][];
		this.identifierPtr -= length;
		System.arraycopy(this.identifierStack, this.identifierPtr + 1, tokens, 0, length);
		long[] positions = new long[length];
		System.arraycopy(this.identifierPositionStack, this.identifierPtr + 1, positions, 0, length);
		QualifiedNameReference ref =
			newQualifiedNameReference(
				tokens,
				positions,
				(int) (this.identifierPositionStack[this.identifierPtr + 1] >> 32), // sourceStart
				(int) this.identifierPositionStack[this.identifierPtr + length]); // sourceEnd
		if (this.reportReferenceInfo) {
			addUnknownRef(ref);
		}
		return ref;
	}
}
@Override
public NameReference getUnspecifiedReferenceOptimized() {
	/* build a (unspecified) NameReference which may be qualified
	The optimization occurs for qualified reference while we are
	certain in this case the last item of the qualified name is
	a field access. This optimization is IMPORTANT while it results
	that when a NameReference is build, the type checker should always
	look for that it is not a type reference */
	consumeNonTypeUseName();
	int length;
	if ((length = this.identifierLengthStack[this.identifierLengthPtr--]) == 1) {
		// single variable reference
		SingleNameReference ref =
			newSingleNameReference(
				this.identifierStack[this.identifierPtr],
				this.identifierPositionStack[this.identifierPtr--]);
		ref.bits &= ~ASTNode.RestrictiveFlagMASK;
		ref.bits |= Binding.LOCAL | Binding.FIELD;
		if (this.reportReferenceInfo) {
			addUnknownRef(ref);
		}
		return ref;
	}

	//Qualified-variable-reference
	//In fact it is variable-reference DOT field-ref , but it would result in a type
	//conflict tha can be only reduce by making a superclass (or inetrface ) between
	//nameReference and FiledReference or putting FieldReference under NameReference
	//or else..........This optimisation is not really relevant so just leave as it is

	char[][] tokens = new char[length][];
	this.identifierPtr -= length;
	System.arraycopy(this.identifierStack, this.identifierPtr + 1, tokens, 0, length);
	long[] positions = new long[length];
	System.arraycopy(this.identifierPositionStack, this.identifierPtr + 1, positions, 0, length);
	QualifiedNameReference ref =
		newQualifiedNameReference(
			tokens,
			positions,
			(int) (this.identifierPositionStack[this.identifierPtr + 1] >> 32),
	// sourceStart
	 (int) this.identifierPositionStack[this.identifierPtr + length]); // sourceEnd
	ref.bits &= ~ASTNode.RestrictiveFlagMASK;
	ref.bits |= Binding.LOCAL | Binding.FIELD;
	if (this.reportReferenceInfo) {
		addUnknownRef(ref);
	}
	return ref;
}
protected ImportReference newImportReference(char[][] tokens, long[] positions, boolean onDemand, int mod) {
	return new ImportReference(tokens, positions, onDemand, mod);
}
protected QualifiedNameReference newQualifiedNameReference(char[][] tokens, long[] positions, int sourceStart, int sourceEnd) {
	return new QualifiedNameReference(tokens, positions, sourceStart, sourceEnd);
}
protected SingleNameReference newSingleNameReference(char[] source, long positions) {
	return new SingleNameReference(source, positions);
}
private static class DummyTypeReference extends TypeReference {
	char[] token;
	DummyTypeReference(char[] name) {
		this.token = name;
	}
	@Override
	public TypeReference augmentTypeWithAdditionalDimensions(int additionalDimensions,
			Annotation[][] additionalAnnotations, boolean isVarargs) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public char[] getLastToken() {
		return this.token;
	}
	@Override
	protected TypeBinding getTypeBinding(Scope scope) {
		return null;
	}
	@Override
	public char[][] getTypeName() {
		return new char[][] {this.token};
	}
	@Override
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		// TODO Auto-generated method stub
	}

	@Override
	public void traverse(ASTVisitor visitor, ClassScope scope) {
		// TODO Auto-generated method stub
	}

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output) {
		return output.append(this.token);
	}
	@Override
	public String toString() {
		return new String(this.token);
	}
}
private void processImplicitPermittedTypes(TypeDeclaration typeDecl, TypeDeclaration[] allTypes) {
	if (typeDecl.permittedTypes == null &&
			(typeDecl.modifiers & ExtraCompilerModifiers.AccSealed) != 0) {
		List<TypeReference> list = new ArrayList();
		for (TypeDeclaration type : allTypes) {
			if (type != typeDecl) {
				char[][] qName = type.superclass == null ? null : type.superclass.getTypeName();
				if (qName != null &&
						CharOperation.equals(qName[qName.length -1], typeDecl.name)) {
					list.add(new DummyTypeReference(type.name));
				}
				if (type.superInterfaces != null) {
					for (TypeReference ref : type.superInterfaces) {
						qName = ref.getTypeName();
						if (CharOperation.equals(qName[qName.length -1], typeDecl.name)) {
							list.add(new DummyTypeReference(type.name));
							break;
						}
					}
				}
			}
		}
		typeDecl.permittedTypes = list.toArray(new TypeReference[list.size()]);
	}
}
public CompilationUnitDeclaration parseCompilationUnit(
	ICompilationUnit unit,
	boolean fullParse,
	IProgressMonitor pm) {

	boolean old = this.diet;
	int oldInt = this.dietInt;
	CompilationUnitDeclaration parsedUnit = null;
	try {
		this.diet = true;
		this.dietInt = 0;
		this.reportReferenceInfo = fullParse;
		CompilationResult compilationUnitResult = new CompilationResult(unit, 0, 0, this.options.maxProblemsPerUnit);
		parsedUnit = parse(unit, compilationUnitResult);
		TypeDeclaration[] types = parsedUnit.types;
		if (types != null) {
			for (TypeDeclaration typeDecl : types) {
				processImplicitPermittedTypes(typeDecl, types);
			}
		}
		if (pm != null && pm.isCanceled())
			throw new OperationCanceledException(Messages.operation_cancelled);
		if (this.scanner.recordLineSeparator) {
			this.requestor.acceptLineSeparatorPositions(compilationUnitResult.getLineSeparatorPositions());
		}
		int initialStart = this.scanner.initialPosition;
		int initialEnd = this.scanner.eofPosition;
		if (this.reportLocalDeclarations || fullParse){
			this.diet = false;
			getMethodBodies(parsedUnit);
		}
		this.scanner.resetTo(initialStart, initialEnd);
		this.notifier.notifySourceElementRequestor(
				parsedUnit,
				this.scanner.initialPosition,
				this.scanner.eofPosition,
				this.reportReferenceInfo,
				this.sourceEnds,
				this.nodesToCategories);
		return parsedUnit;
	} catch (AbortCompilation e) {
		// ignore this exception
	} finally {
		this.diet = old;
		this.dietInt = oldInt;
		reset();
	}
	return parsedUnit;
}
private void rememberCategories() {
	if (this.useSourceJavadocParser) {
		SourceJavadocParser sourceJavadocParser = (SourceJavadocParser) this.javadocParser;
		char[][] categories =  sourceJavadocParser.categories;
		if (categories.length > 0) {
			this.nodesToCategories.put(this.astStack[this.astPtr], categories);
			sourceJavadocParser.categories = CharOperation.NO_CHAR_CHAR;
		}
	}
}
private void reset() {
	this.sourceEnds = new HashtableOfObjectToInt();
	this.nodesToCategories = new HashMap();
}
public void setRequestor(ISourceElementRequestor requestor) {
	this.requestor = requestor;
	this.notifier.requestor = requestor;
}
}
