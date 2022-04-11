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
package org.eclipse.jdt.internal.eval;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;

/**
 * A parser for code snippets.
 */
public class CodeSnippetParser extends Parser implements EvaluationConstants {
	int codeSnippetStart, codeSnippetEnd;
	EvaluationContext evaluationContext;
	boolean hasRecoveredOnExpression;
	int lastStatement = -1; // end of last top level statement
	int lineSeparatorLength;

	int problemCountBeforeRecovery = 0;
/**
 * Creates a new code snippet parser.
 */
public CodeSnippetParser(ProblemReporter problemReporter, EvaluationContext evaluationContext, boolean optimizeStringLiterals, int codeSnippetStart, int codeSnippetEnd) {
	super(problemReporter, optimizeStringLiterals);
	this.codeSnippetStart = codeSnippetStart;
	this.codeSnippetEnd = codeSnippetEnd;
	this.evaluationContext = evaluationContext;
	this.reportOnlyOneSyntaxError = true;
	this.javadocParser.checkDocComment = false;
}
@Override
protected void classInstanceCreation(boolean alwaysQualified) {
	// ClassInstanceCreationExpression ::= 'new' ClassType '(' ArgumentListopt ')' ClassBodyopt

	// ClassBodyopt produces a null item on the astStak if it produces NO class body
	// An empty class body produces a 0 on the length stack.....

	AllocationExpression alloc;
	int length;
	if (((length = this.astLengthStack[this.astLengthPtr--]) == 1)
		&& (this.astStack[this.astPtr] == null)) {
		//NO ClassBody
		this.astPtr--;
		if (alwaysQualified) {
			alloc = new QualifiedAllocationExpression();
		} else {
			alloc = new CodeSnippetAllocationExpression(this.evaluationContext);
		}
		alloc.sourceEnd = this.endPosition; //the position has been stored explicitly

		if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
			this.expressionPtr -= length;
			System.arraycopy(
				this.expressionStack,
				this.expressionPtr + 1,
				alloc.arguments = new Expression[length],
				0,
				length);
		}
		alloc.type = getTypeReference(0);
		checkForDiamond(alloc.type);

		//the default constructor with the correct number of argument
		//will be created and added by the TC (see createsInternalConstructorWithBinding)
		alloc.sourceStart = this.intStack[this.intPtr--];
		pushOnExpressionStack(alloc);
	} else {
		dispatchDeclarationInto(length);
		TypeDeclaration anonymousTypeDeclaration = (TypeDeclaration) this.astStack[this.astPtr];
		anonymousTypeDeclaration.declarationSourceEnd = this.endStatementPosition;
		if (anonymousTypeDeclaration.allocation != null) {
			anonymousTypeDeclaration.allocation.sourceEnd = this.endStatementPosition;
		}
		this.astPtr--;
		this.astLengthPtr--;
	}
}
@Override
protected void consumeClassInstanceCreationExpressionWithTypeArguments() {
	// ClassInstanceCreationExpression ::= 'new' TypeArguments ClassType '(' ArgumentListopt ')' ClassBodyopt
	AllocationExpression alloc;
	int length;
	if (((length = this.astLengthStack[this.astLengthPtr--]) == 1)
		&& (this.astStack[this.astPtr] == null)) {
		//NO ClassBody
		this.astPtr--;
		alloc = new CodeSnippetAllocationExpression(this.evaluationContext);
		alloc.sourceEnd = this.endPosition; //the position has been stored explicitly

		if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
			this.expressionPtr -= length;
			System.arraycopy(
				this.expressionStack,
				this.expressionPtr + 1,
				alloc.arguments = new Expression[length],
				0,
				length);
		}
		alloc.type = getTypeReference(0);

		length = this.genericsLengthStack[this.genericsLengthPtr--];
		this.genericsPtr -= length;
		System.arraycopy(this.genericsStack, this.genericsPtr + 1, alloc.typeArguments = new TypeReference[length], 0, length);
		this.intPtr--;

		//the default constructor with the correct number of argument
		//will be created and added by the TC (see createsInternalConstructorWithBinding)
		alloc.sourceStart = this.intStack[this.intPtr--];
		pushOnExpressionStack(alloc);
	} else {
		dispatchDeclarationInto(length);
		TypeDeclaration anonymousTypeDeclaration = (TypeDeclaration)this.astStack[this.astPtr];
		anonymousTypeDeclaration.declarationSourceEnd = this.endStatementPosition;
		anonymousTypeDeclaration.bodyEnd = this.endStatementPosition;
		if (length == 0 && !containsComment(anonymousTypeDeclaration.bodyStart, anonymousTypeDeclaration.bodyEnd)) {
			anonymousTypeDeclaration.bits |= ASTNode.UndocumentedEmptyBlock;
		}
		this.astPtr--;
		this.astLengthPtr--;

		QualifiedAllocationExpression allocationExpression = anonymousTypeDeclaration.allocation;
		if (allocationExpression != null) {
			allocationExpression.sourceEnd = this.endStatementPosition;
			// handle type arguments
			length = this.genericsLengthStack[this.genericsLengthPtr--];
			this.genericsPtr -= length;
			System.arraycopy(this.genericsStack, this.genericsPtr + 1, allocationExpression.typeArguments = new TypeReference[length], 0, length);
			allocationExpression.sourceStart = this.intStack[this.intPtr--];
		}
	}
}
@Override
protected void consumeClassDeclaration() {
	super.consumeClassDeclaration();
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected void consumeClassHeaderName1() {
	// ClassHeaderName ::= Modifiersopt 'class' 'Identifier'
	TypeDeclaration typeDecl;
	if (this.nestedMethod[this.nestedType] == 0) {
		if (this.nestedType != 0) {
			typeDecl = new TypeDeclaration(this.compilationUnit.compilationResult);
			typeDecl.bits |= ASTNode.IsMemberType;
		} else {
			typeDecl = new CodeSnippetTypeDeclaration(this.compilationUnit.compilationResult);
		}
	} else {
		// Record that the block has a declaration for local types
		typeDecl = new TypeDeclaration(this.compilationUnit.compilationResult);
		typeDecl.bits |= ASTNode.IsLocalType;
		markEnclosingMemberWithLocalType();
		blockReal();
	}

	//highlight the name of the type
	long pos = this.identifierPositionStack[this.identifierPtr];
	typeDecl.sourceEnd = (int) pos;
	typeDecl.sourceStart = (int) (pos >>> 32);
	typeDecl.name = this.identifierStack[this.identifierPtr--];
	this.identifierLengthPtr--;

	//compute the declaration source too
	typeDecl.declarationSourceStart = this.intStack[this.intPtr--];
	this.intPtr--;
	// 'class' and 'interface' push an int position
	typeDecl.modifiersSourceStart = this.intStack[this.intPtr--];
	typeDecl.modifiers = this.intStack[this.intPtr--];
	if (typeDecl.modifiersSourceStart >= 0) {
		typeDecl.declarationSourceStart = typeDecl.modifiersSourceStart;
	}
	typeDecl.bodyStart = typeDecl.sourceEnd + 1;
	pushOnAstStack(typeDecl);

	this.listLength = 0; // will be updated when reading super-interfaces
	// recovery
	if (this.currentElement != null){
		this.lastCheckPoint = typeDecl.bodyStart;
		this.currentElement = this.currentElement.add(typeDecl, 0);
		this.lastIgnoredToken = -1;
	}
	// javadoc
	typeDecl.javadoc = this.javadoc;
	this.javadoc = null;
}
@Override
protected void consumeEmptyStatement() {
	super.consumeEmptyStatement();
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected void consumeEnhancedForStatement() {
	super.consumeEnhancedForStatement();
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected void consumeExpressionStatement() {
	super.consumeExpressionStatement();
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected void consumeFieldAccess(boolean isSuperAccess) {
	// FieldAccess ::= Primary '.' 'Identifier'
	// FieldAccess ::= 'super' '.' 'Identifier'

	FieldReference fr =
		new CodeSnippetFieldReference(
			this.identifierStack[this.identifierPtr],
			this.identifierPositionStack[this.identifierPtr--],
			this.evaluationContext);
	this.identifierLengthPtr--;
	if (isSuperAccess) {
		//considerates the fieldReference beginning at the 'super' ....
		fr.sourceStart = this.intStack[this.intPtr--];
		problemReporter().codeSnippetMissingClass(null,0, 0);
		fr.receiver = new CodeSnippetSuperReference(fr.sourceStart, this.endPosition);
		pushOnExpressionStack(fr);
	} else {
		//optimize push/pop
		if ((fr.receiver = this.expressionStack[this.expressionPtr]).isThis()) {
			//fieldreference begins at the this
			fr.sourceStart = fr.receiver.sourceStart;
		}
		this.expressionStack[this.expressionPtr] = fr;
	}
}
@Override
protected void consumeInternalCompilationUnit() {
	// InternalCompilationUnit ::= PackageDeclaration
	// InternalCompilationUnit ::= PackageDeclaration ImportDeclarations ReduceImports
	// InternalCompilationUnit ::= ImportDeclarations ReduceImports
}
@Override
protected void consumeInternalCompilationUnitWithTypes() {
	// InternalCompilationUnit ::= PackageDeclaration ImportDeclarations ReduceImports TypeDeclarations
	// InternalCompilationUnit ::= PackageDeclaration TypeDeclarations
	// InternalCompilationUnit ::= TypeDeclarations
	// InternalCompilationUnit ::= ImportDeclarations ReduceImports TypeDeclarations
	// consume type declarations
	int length;
	if ((length = this.astLengthStack[this.astLengthPtr--]) != 0) {
		this.compilationUnit.types = new TypeDeclaration[length];
		this.astPtr -= length;
		System.arraycopy(this.astStack, this.astPtr + 1, this.compilationUnit.types, 0, length);
	}
}
@Override
protected void consumeLocalVariableDeclarationStatement() {
	super.consumeLocalVariableDeclarationStatement();
	/* recovery */
	recordLastStatementIfNeeded();
}

/**
 * In case emulating local variables, wrap the (recovered) statements inside a
 * try statement so as to achieve local state commiting (copy local vars back to fields).
 * The CSToCuMapper could not be used, since it could have interfered with
 * the syntax recovery specific to code snippets.
 */
@Override
protected void consumeMethodDeclaration(boolean isNotAbstract, boolean isDefaultMethod) {
	// MethodDeclaration ::= MethodHeader MethodBody
	// AbstractMethodDeclaration ::= MethodHeader ';'

	super.consumeMethodDeclaration(isNotAbstract, isDefaultMethod);

	// now we know that we have a method declaration at the top of the ast stack
	MethodDeclaration methodDecl = (MethodDeclaration) this.astStack[this.astPtr];

	// automatically wrap the last statement inside a return statement, if it is an expression
	// support have to be defined at toplevel only
	if (isTopLevelType()) {
		int last = methodDecl.statements == null ? -1 : methodDecl.statements.length - 1;
		if (last >= 0 && methodDecl.statements[last] instanceof Expression &&
				((Expression) methodDecl.statements[last]).isTrulyExpression()) {
			Expression lastExpression = (Expression) methodDecl.statements[last];
			methodDecl.statements[last] = new CodeSnippetReturnStatement(
											lastExpression,
											lastExpression.sourceStart,
											lastExpression.sourceEnd);
		}
	}

	int start = methodDecl.bodyStart-1, end = start;
	long position = ((long)start << 32) + end;
	long[] positions = new long[]{position};
	if (this.evaluationContext.localVariableNames != null) {

		int varCount = this.evaluationContext.localVariableNames.length; // n local decls+ try statement

		// generate n local variable declarations: [type] [name] = val$[name];
		Statement[] newStatements = new Statement[varCount+1];
		for (int i = 0; i < varCount; i++){
			char[] trimmedTypeName = this.evaluationContext.localVariableTypeNames[i];
			int nameEnd = CharOperation.indexOf('[', trimmedTypeName);
			if (nameEnd >= 0) {
				trimmedTypeName = CharOperation.subarray(trimmedTypeName, 0, nameEnd);
			}
			nameEnd = CharOperation.indexOf(' ', trimmedTypeName);
			if (nameEnd >= 0) {
				trimmedTypeName = CharOperation.subarray(trimmedTypeName, 0, nameEnd);
			}
			TypeReference typeReference;
			if (CharOperation.indexOf('.', trimmedTypeName) == -1) {
				typeReference = new SingleTypeReference(trimmedTypeName, position);
			} else {
				typeReference = new QualifiedTypeReference(
						CharOperation.splitOn('.', trimmedTypeName),
						positions);
			}
			int dimCount = CharOperation.occurencesOf('[', this.evaluationContext.localVariableTypeNames[i]);
			if (dimCount > 0) {
				typeReference = augmentTypeWithAdditionalDimensions(typeReference, dimCount, null, false);
			}
			NameReference init = new SingleNameReference(
									CharOperation.concat(LOCAL_VAR_PREFIX, this.evaluationContext.localVariableNames[i]), position);
			LocalDeclaration declaration = new LocalDeclaration(this.evaluationContext.localVariableNames[i], start, end);
			declaration.initialization = init;
			declaration.type = typeReference;
			declaration.modifiers = this.evaluationContext.localVariableModifiers[i];
			newStatements[i] = declaration;
		}

		// generate try { [snippet] } finally { [save locals to fields] }
		// try block
		TryStatement tryStatement = new TryStatement();
		Block tryBlock = new Block(methodDecl.explicitDeclarations);
		tryBlock.sourceStart = start;
		tryBlock.sourceEnd = end;
		tryBlock.statements = methodDecl.statements; // snippet statements
		tryStatement.tryBlock = tryBlock;
		// finally block
		Block finallyBlock = new Block(0);
		finallyBlock.sourceStart = start;
		finallyBlock.sourceEnd = end;
		finallyBlock.statements = new Statement[varCount];
		for (int i = 0; i < varCount; i++){
			SingleNameReference nameRef = new SingleNameReference(this.evaluationContext.localVariableNames[i], position);
			finallyBlock.statements[i] = new Assignment(
				new SingleNameReference(CharOperation.concat(LOCAL_VAR_PREFIX, this.evaluationContext.localVariableNames[i]), position),
				nameRef,
				nameRef.sourceEnd);
		}
		tryStatement.finallyBlock = finallyBlock;

		newStatements[varCount] = tryStatement;
		methodDecl.statements = newStatements;
	}
}
@Override
protected void consumeMethodInvocationName() {
	// MethodInvocation ::= Name '(' ArgumentListopt ')'

	if (this.scanner.startPosition >= this.codeSnippetStart
		&& this.scanner.startPosition <= this.codeSnippetEnd + 1 + this.lineSeparatorLength // 14838
		&& isTopLevelType()) {

		// when the name is only an identifier...we have a message send to "this" (implicit)

		MessageSend m = newMessageSend();
		m.sourceEnd = this.rParenPos;
		m.sourceStart =
			(int) ((m.nameSourcePosition = this.identifierPositionStack[this.identifierPtr]) >>> 32);
		m.selector = this.identifierStack[this.identifierPtr--];
		if (this.identifierLengthStack[this.identifierLengthPtr] == 1) {
			m.receiver = new CodeSnippetThisReference(0,0,this.evaluationContext, true);
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
			m.receiver = getUnspecifiedReference();
			m.sourceStart = m.receiver.sourceStart;
		}
		pushOnExpressionStack(m);
	} else {
		super.consumeMethodInvocationName();
	}
}
@Override
protected void consumeMethodInvocationNameWithTypeArguments() {
	// MethodInvocation ::= Name '.' TypeArguments 'Identifier' '(' ArgumentListopt ')'

	// when the name is only an identifier...we have a message send to "this" (implicit)
	if (this.scanner.startPosition >= this.codeSnippetStart
			&& this.scanner.startPosition <= this.codeSnippetEnd + 1 + this.lineSeparatorLength // 14838
			&& isTopLevelType()) {


		MessageSend m = newMessageSendWithTypeArguments();
		m.sourceEnd = this.rParenPos;
		m.sourceStart =
			(int) ((m.nameSourcePosition = this.identifierPositionStack[this.identifierPtr]) >>> 32);
		m.selector = this.identifierStack[this.identifierPtr--];
		this.identifierLengthPtr--;

		// handle type arguments
		int length = this.genericsLengthStack[this.genericsLengthPtr--];
		this.genericsPtr -= length;
		System.arraycopy(this.genericsStack, this.genericsPtr + 1, m.typeArguments = new TypeReference[length], 0, length);
		this.intPtr--;

		m.receiver = getUnspecifiedReference();
		m.sourceStart = m.receiver.sourceStart;
		pushOnExpressionStack(m);
	} else {
		super.consumeMethodInvocationNameWithTypeArguments();
	}
}
@Override
protected void consumeMethodInvocationSuper() {
	// MethodInvocation ::= 'super' '.' 'Identifier' '(' ArgumentListopt ')'

	MessageSend m = newMessageSend();
	m.sourceStart = this.intStack[this.intPtr--];
	m.sourceEnd = this.rParenPos;
	m.nameSourcePosition = this.identifierPositionStack[this.identifierPtr];
	m.selector = this.identifierStack[this.identifierPtr--];
	this.identifierLengthPtr--;
	m.receiver = new CodeSnippetSuperReference(m.sourceStart, this.endPosition);
	pushOnExpressionStack(m);
}
@Override
protected void consumeMethodInvocationSuperWithTypeArguments() {
	// MethodInvocation ::= 'super' '.' TypeArguments 'Identifier' '(' ArgumentListopt ')'

	MessageSend m = newMessageSendWithTypeArguments();
	this.intPtr--; // start position of the typeArguments
	m.sourceEnd = this.rParenPos;
	m.nameSourcePosition = this.identifierPositionStack[this.identifierPtr];
	m.selector = this.identifierStack[this.identifierPtr--];
	this.identifierLengthPtr--;

	// handle type arguments
	int length = this.genericsLengthStack[this.genericsLengthPtr--];
	this.genericsPtr -= length;
	System.arraycopy(this.genericsStack, this.genericsPtr + 1, m.typeArguments = new TypeReference[length], 0, length);
	m.sourceStart = this.intStack[this.intPtr--]; // start position of the super keyword

	m.receiver = new CodeSnippetSuperReference(m.sourceStart, this.endPosition);
	pushOnExpressionStack(m);
}
@Override
protected void consumePrimaryNoNewArrayThis() {
	// PrimaryNoNewArray ::= 'this'

	if (this.scanner.startPosition >= this.codeSnippetStart
		&& this.scanner.startPosition <= this.codeSnippetEnd + 1 + this.lineSeparatorLength // 14838
		&& isTopLevelType()) {
		pushOnExpressionStack(
			new CodeSnippetThisReference(this.intStack[this.intPtr--], this.endPosition, this.evaluationContext, false));
	} else {
		super.consumePrimaryNoNewArrayThis();
	}
}
@Override
protected void consumeStatementBreak() {
	super.consumeStatementBreak();
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected void consumeStatementBreakWithLabel() {
	super.consumeStatementBreakWithLabel();
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected void consumeStatementCatch() {
	super.consumeStatementCatch();
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected void consumeStatementContinue() {
	super.consumeStatementContinue();
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected void consumeStatementContinueWithLabel() {
	super.consumeStatementContinueWithLabel();
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected void consumeStatementDo() {
	super.consumeStatementDo();
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected void consumeStatementFor() {
	super.consumeStatementFor();
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected void consumeStatementIfNoElse() {
	super.consumeStatementIfNoElse();
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected void consumeStatementIfWithElse() {
	super.consumeStatementIfWithElse();
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected void consumeStatementLabel() {
	super.consumeStatementLabel();
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected void consumeStatementReturn() {
	// ReturnStatement ::= 'return' Expressionopt ';'

	// returned value intercepted by code snippet
	// support have to be defined at toplevel only
	if ((this.hasRecoveredOnExpression
			|| (this.scanner.startPosition >= this.codeSnippetStart && this.scanner.startPosition <= this.codeSnippetEnd+1+this.lineSeparatorLength /* 14838*/))
		&& this.expressionLengthStack[this.expressionLengthPtr] != 0
		&& isTopLevelType()) {
		this.expressionLengthPtr--;
		Expression expression = this.expressionStack[this.expressionPtr--];
		pushOnAstStack(
			new CodeSnippetReturnStatement(
				expression,
				expression.sourceStart,
				expression.sourceEnd));
	} else {
		super.consumeStatementReturn();
	}
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected void consumeStatementSwitch() {
	super.consumeStatementSwitch();
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected void consumeStatementSynchronized() {
	super.consumeStatementSynchronized();
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected void consumeStatementThrow() {
	super.consumeStatementThrow();
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected void consumeStatementTry(boolean arg_0, boolean arg_1) {
	super.consumeStatementTry(arg_0, arg_1);
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected void consumeStatementWhile() {
	super.consumeStatementWhile();
	/* recovery */
	recordLastStatementIfNeeded();
}
@Override
protected CompilationUnitDeclaration endParse(int act) {
	if (this.hasRecoveredOnExpression) {
		CompilationResult unitResult = this.compilationUnit.compilationResult;
		if (act != ERROR_ACTION) { // expression recovery worked
			// flush previously recorded problems
			for (int i = 0; i < unitResult.problemCount; i++) {
				unitResult.problems[i] = null; // discard problem
			}
			unitResult.problemCount = 0;
			if (this.referenceContext instanceof AbstractMethodDeclaration) {
				((AbstractMethodDeclaration)this.referenceContext).ignoreFurtherInvestigation = false;
			}
			if (this.referenceContext instanceof CompilationUnitDeclaration) {
				((CompilationUnitDeclaration)this.referenceContext).ignoreFurtherInvestigation = false;
			}

			// consume expresion as a return statement
			consumeStatementReturn();
			int fieldsCount =
				(this.evaluationContext.localVariableNames == null ? 0 : this.evaluationContext.localVariableNames.length)
				+ (this.evaluationContext.declaringTypeName == null ? 0 : 1);
			if (this.astPtr > (this.diet ? 0 : 2 + fieldsCount)) {
					// in diet mode, the ast stack was empty when we went for method body
					// otherwise it contained the type, the generated fields for local variables,
					// the generated field for 'this' and the method
				consumeBlockStatements();
			}
			consumeMethodBody();
			if (!this.diet) {
				consumeMethodDeclaration(true, false);
				if (fieldsCount > 0) {
					consumeClassBodyDeclarations();
				}
				consumeClassBodyDeclarationsopt();
				consumeClassDeclaration();
				consumeInternalCompilationUnitWithTypes();
				consumeCompilationUnit();
			}
			this.lastAct = ACCEPT_ACTION;
		} else {
			// might have more than one error recorded:
			// 1. during regular parse
			// 2. during expression recovery
			// -> must filter out one of them, the earliest one is less accurate
			int maxRegularPos = 0, problemCount = unitResult.problemCount;
			for (int i = 0; i < this.problemCountBeforeRecovery; i++) {
				// skip unmatched bracket problems
				if (unitResult.problems[i].getID() == IProblem.UnmatchedBracket) continue;

				int start = unitResult.problems[i].getSourceStart();
				if (start > maxRegularPos && start <= this.codeSnippetEnd) {
					maxRegularPos = start;
				}
			}
			int maxRecoveryPos = 0;
			for (int i = this.problemCountBeforeRecovery; i < problemCount; i++) {
				// skip unmatched bracket problems
				if (unitResult.problems[i].getID() == IProblem.UnmatchedBracket) continue;

				int start = unitResult.problems[i].getSourceStart();
				if (start > maxRecoveryPos && start <= this.codeSnippetEnd) {
					maxRecoveryPos = start;
				}
			}
			if (maxRecoveryPos > maxRegularPos) {
				System.arraycopy(unitResult.problems, this.problemCountBeforeRecovery, unitResult.problems, 0, problemCount - this.problemCountBeforeRecovery);
				unitResult.problemCount -= this.problemCountBeforeRecovery;
			} else {
				unitResult.problemCount -= (problemCount - this.problemCountBeforeRecovery);
			}
			for (int i = unitResult.problemCount; i < problemCount; i++) {
				unitResult.problems[i] = null; // discard problem
			}

		}
	}
	return super.endParse(act);
}
@Override
protected NameReference getUnspecifiedReference(boolean rejectTypeAnnotations) {
	/* build a (unspecified) NameReference which may be qualified*/
	if (rejectTypeAnnotations) {
		consumeNonTypeUseName();
	}

	if (this.scanner.startPosition >= this.codeSnippetStart
		&& this.scanner.startPosition <= this.codeSnippetEnd+1+this.lineSeparatorLength /*14838*/){
		int length;
		NameReference ref;
		if ((length = this.identifierLengthStack[this.identifierLengthPtr--]) == 1) {
			// single variable reference
			ref =
				new CodeSnippetSingleNameReference(
					this.identifierStack[this.identifierPtr],
					this.identifierPositionStack[this.identifierPtr--],
					this.evaluationContext);
		} else {
			//Qualified variable reference
			char[][] tokens = new char[length][];
			this.identifierPtr -= length;
			System.arraycopy(this.identifierStack, this.identifierPtr + 1, tokens, 0, length);
			long[] positions = new long[length];
			System.arraycopy(this.identifierPositionStack, this.identifierPtr + 1, positions, 0, length);
			ref =
				new CodeSnippetQualifiedNameReference(tokens,
					positions,
					(int) (this.identifierPositionStack[this.identifierPtr + 1] >> 32), // sourceStart
					(int) this.identifierPositionStack[this.identifierPtr + length],
					this.evaluationContext); // sourceEnd
		}
		return ref;
	} else {
		return super.getUnspecifiedReference(rejectTypeAnnotations);
	}
}
@Override
protected NameReference getUnspecifiedReferenceOptimized() {
	/* build a (unspecified) NameReference which may be qualified
	The optimization occurs for qualified reference while we are
	certain in this case the last item of the qualified name is
	a field access. This optimization is IMPORTANT while it results
	that when a NameReference is build, the type checker should always
	look for that it is not a type reference */
	consumeNonTypeUseName();

	if (this.scanner.startPosition >= this.codeSnippetStart
		&& this.scanner.startPosition <= this.codeSnippetEnd+1+this.lineSeparatorLength /*14838*/){
		int length;
		NameReference ref;
		if ((length = this.identifierLengthStack[this.identifierLengthPtr--]) == 1) {
			// single variable reference
			ref =
				new CodeSnippetSingleNameReference(
					this.identifierStack[this.identifierPtr],
					this.identifierPositionStack[this.identifierPtr--],
					this.evaluationContext);
			ref.bits &= ~ASTNode.RestrictiveFlagMASK;
			ref.bits |= Binding.LOCAL | Binding.FIELD;
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
		ref = new CodeSnippetQualifiedNameReference(
				tokens,
				positions,
				(int) (this.identifierPositionStack[this.identifierPtr + 1] >> 32), // sourceStart
				(int) this.identifierPositionStack[this.identifierPtr + length],
				this.evaluationContext); // sourceEnd
		ref.bits &= ~ASTNode.RestrictiveFlagMASK;
		ref.bits |= Binding.LOCAL | Binding.FIELD;
		return ref;
	} else {
		return super.getUnspecifiedReferenceOptimized();
	}
}
@Override
protected void ignoreExpressionAssignment() {
	super.ignoreExpressionAssignment();
	/* recovery */
	recordLastStatementIfNeeded();
}
/**
 * Returns whether we are parsing a top level type or not.
 */
private boolean isTopLevelType() {
	return (this.nestedType - this.switchNestingLevel) == (this.diet ? 0 : 1);
}
@Override
protected MessageSend newMessageSend() {
	// '(' ArgumentListopt ')'
	// the arguments are on the expression stack

	CodeSnippetMessageSend m = new CodeSnippetMessageSend(this.evaluationContext);
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
	return m;
}
@Override
protected MessageSend newMessageSendWithTypeArguments() {
	// '(' ArgumentListopt ')'
	// the arguments are on the expression stack
	CodeSnippetMessageSend m = new CodeSnippetMessageSend(this.evaluationContext);
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
	return m;
}
/**
 * Records the scanner position if we're parsing a top level type.
 */
private void recordLastStatementIfNeeded() {
	if ((isTopLevelType()) && (this.scanner.startPosition <= this.codeSnippetEnd+this.lineSeparatorLength /*14838*/)) {
		this.lastStatement = this.scanner.startPosition;
	}
}

@Override
protected void reportSyntaxErrors(boolean isDietParse, int oldFirstToken) {
	if (!isDietParse) {
		this.scanner.initialPosition = this.lastStatement;
		this.scanner.eofPosition = this.codeSnippetEnd + 1; // stop after expression
		oldFirstToken = TokenNameTWIDDLE;//TokenNameREMAINDER; // first token of th expression parse
	}
	super.reportSyntaxErrors(isDietParse, oldFirstToken);
}
/*
 * A syntax error was detected. If a method is being parsed, records the number of errors and
 * attempts to restart from the last statement by going for an expression.
 */
@Override
protected int resumeOnSyntaxError() {
	if (this.diet || this.hasRecoveredOnExpression) { // no reentering inside expression recovery
		return HALT;
	}

	// record previous error, in case more accurate than potential one in expression recovery
	// e.g. "return foo(a a); 1+3"
	this.problemCountBeforeRecovery = this.compilationUnit.compilationResult.problemCount;

	// reposition for expression parsing
	if (this.lastStatement < 0) {
		this.lastStatement = this.codeSnippetStart; // no statement reduced prior to error point
	}
	this.scanner.initialPosition = this.lastStatement;
	this.scanner.startPosition = this.lastStatement;
	this.scanner.currentPosition = this.lastStatement;
	this.scanner.eofPosition = this.codeSnippetEnd < Integer.MAX_VALUE ? this.codeSnippetEnd + 1 : this.codeSnippetEnd; // stop after expression
	this.scanner.commentPtr = -1;

	// reset stacks in consistent state
	this.expressionPtr = -1;
	this.typeAnnotationLengthPtr = -1;
	this.typeAnnotationPtr = -1;
	this.identifierPtr = -1;
	this.identifierLengthPtr = -1;

	// go for the expression
	goForExpression(true /* record line separators */);
	this.hasRecoveredOnExpression = true;
	this.hasReportedError = false;
	this.hasError = false;
	return RESTART;
}
}
