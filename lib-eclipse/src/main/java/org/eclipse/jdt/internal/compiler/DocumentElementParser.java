/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.*;

import org.eclipse.jdt.internal.compiler.impl.*;
import org.eclipse.jdt.core.compiler.*;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.parser.*;
import org.eclipse.jdt.internal.compiler.problem.*;
import org.eclipse.jdt.internal.compiler.util.Util;

public class DocumentElementParser extends Parser {
	IDocumentElementRequestor requestor;
	private int lastFieldEndPosition;
	private int lastFieldBodyEndPosition;
	private int typeStartPosition;
	private long selectorSourcePositions;
	private int typeDims;
	private int extendsDim;
	private int declarationSourceStart;

	/* int[] stack for storing javadoc positions */
	int[][] intArrayStack;
	int intArrayPtr;

public DocumentElementParser(
	final IDocumentElementRequestor requestor,
	IProblemFactory problemFactory,
	CompilerOptions options) {
	super(new ProblemReporter(
		DefaultErrorHandlingPolicies.exitAfterAllProblems(),
		options,
		problemFactory),
	false);
	this.requestor = requestor;
	this.intArrayStack = new int[30][];
	this.options = options;
	this.javadocParser.checkDocComment = false;

	setMethodsFullRecovery(false);
	setStatementsRecovery(false);
}
/*
 * Will clear the comment stack when looking
 * for a potential JavaDoc which might contain @deprecated.
 *
 * Additionally, before investigating for @deprecated, retrieve the positions
 * of the JavaDoc comments so as to notify requestor with them.
 */
@Override
public void checkComment() {

	/* persisting javadoc positions */
	pushOnIntArrayStack(getJavaDocPositions());
	boolean deprecated = false;
	int lastCommentIndex = -1;
	int commentPtr = this.scanner.commentPtr;

	//since jdk1.2 look only in the last java doc comment...
	nextComment : for (lastCommentIndex = this.scanner.commentPtr; lastCommentIndex >= 0; lastCommentIndex--){
		// skip all non-javadoc comments or those which are after the last modifier
		int commentSourceStart = this.scanner.commentStarts[lastCommentIndex];
		if (commentSourceStart < 0 || // line comment
			this.scanner.commentStops[lastCommentIndex] < 0 || // block comment
			(this.modifiersSourceStart != -1 && this.modifiersSourceStart < commentSourceStart)) // the comment is after the modifier
		{
			continue nextComment;
		}
		// check comment
		deprecated = this.javadocParser.checkDeprecation(lastCommentIndex);
		break nextComment;
	}
	if (deprecated) {
		checkAndSetModifiers(ClassFileConstants.AccDeprecated);
	}
	// modify the modifier source start to point at the first comment
	if (commentPtr >= 0) {
		this.declarationSourceStart = this.scanner.commentStarts[0];
		if (this.declarationSourceStart < 0) this.declarationSourceStart = -this.declarationSourceStart;
	}
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeCatchFormalParameter() {
	// FormalParameter ::= Type VariableDeclaratorId ==> false
	// FormalParameter ::= Modifiers Type VariableDeclaratorId ==> true
	/*
	astStack :
	identifierStack : type identifier
	intStack : dim dim
	 ==>
	astStack : Argument
	identifierStack :
	intStack :
	*/

	this.identifierLengthPtr--;
	char[] parameterName = this.identifierStack[this.identifierPtr];
	long namePositions = this.identifierPositionStack[this.identifierPtr--];
	this.intPtr--; // dimension from the variabledeclaratorid
	TypeReference type = (TypeReference) this.astStack[this.astPtr--];
	this.intPtr -= 3;
	Argument arg =
		new Argument(
			parameterName,
			namePositions,
			type,
			this.intStack[this.intPtr + 1]);// modifiers
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
	pushOnAstStack(arg);
	this.intArrayPtr--;
}
@Override
protected void consumeClassBodyDeclaration() {
	// ClassBodyDeclaration ::= Diet Block
	//push an Initializer
	//optimize the push/pop

	super.consumeClassBodyDeclaration();
	Initializer initializer = (Initializer) this.astStack[this.astPtr];
	this.requestor.acceptInitializer(
		initializer.declarationSourceStart,
		initializer.declarationSourceEnd,
		this.intArrayStack[this.intArrayPtr--],
		0,
		this.modifiersSourceStart,
		initializer.block.sourceStart,
		initializer.block.sourceEnd);
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeClassDeclaration() {
	super.consumeClassDeclaration();
	// we know that we have a TypeDeclaration on the top of the astStack
	if (isLocalDeclaration()) {
		// we ignore the local variable declarations
		return;
	}
	this.requestor.exitClass(this.endStatementPosition, // '}' is the end of the body
	 ((TypeDeclaration) this.astStack[this.astPtr]).declarationSourceEnd);
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeClassHeader() {
	//ClassHeader ::= $empty
	super.consumeClassHeader();
	if (isLocalDeclaration()) {
		// we ignore the local variable declarations
		this.intArrayPtr--;
		return;
	}
	TypeDeclaration typeDecl = (TypeDeclaration) this.astStack[this.astPtr];
	TypeReference[] superInterfaces = typeDecl.superInterfaces;
	char[][] interfaceNames = null;
	int[] interfaceNameStarts = null;
	int[] interfaceNameEnds = null;
	if (superInterfaces != null) {
		int superInterfacesLength = superInterfaces.length;
		interfaceNames = new char[superInterfacesLength][];
		interfaceNameStarts = new int[superInterfacesLength];
		interfaceNameEnds = new int[superInterfacesLength];
		for (int i = 0; i < superInterfacesLength; i++) {
			TypeReference superInterface = superInterfaces[i];
			interfaceNames[i] = CharOperation.concatWith(superInterface.getTypeName(), '.');
			interfaceNameStarts[i] = superInterface.sourceStart;
			interfaceNameEnds[i] = superInterface.sourceEnd;
		}
	}
	// flush the comments related to the class header
	this.scanner.commentPtr = -1;
	TypeReference superclass = typeDecl.superclass;
	if (superclass == null) {
		this.requestor.enterClass(
			typeDecl.declarationSourceStart,
			this.intArrayStack[this.intArrayPtr--],
			typeDecl.modifiers,
			typeDecl.modifiersSourceStart,
			this.typeStartPosition,
			typeDecl.name,
			typeDecl.sourceStart,
			typeDecl.sourceEnd,
			null,
			-1,
			-1,
			interfaceNames,
			interfaceNameStarts,
			interfaceNameEnds,
			this.scanner.currentPosition - 1);
	} else {
		this.requestor.enterClass(
			typeDecl.declarationSourceStart,
			this.intArrayStack[this.intArrayPtr--],
			typeDecl.modifiers,
			typeDecl.modifiersSourceStart,
			this.typeStartPosition,
			typeDecl.name,
			typeDecl.sourceStart,
			typeDecl.sourceEnd,
			CharOperation.concatWith(superclass.getTypeName(), '.'),
			superclass.sourceStart,
			superclass.sourceEnd,
			interfaceNames,
			interfaceNameStarts,
			interfaceNameEnds,
			this.scanner.currentPosition - 1);

	}
}
@Override
protected void consumeClassHeaderName1() {
	// ClassHeaderName ::= Modifiersopt 'class' 'Identifier'
	TypeDeclaration typeDecl = new TypeDeclaration(this.compilationUnit.compilationResult);
	if (this.nestedMethod[this.nestedType] == 0) {
		if (this.nestedType != 0) {
			typeDecl.bits |= ASTNode.IsMemberType;
		}
	} else {
		// Record that the block has a declaration for local types
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
	// 'class' and 'interface' push an int position
	this.typeStartPosition = typeDecl.declarationSourceStart = this.intStack[this.intPtr--];
	this.intPtr--;
	int declSourceStart = this.intStack[this.intPtr--];
	typeDecl.modifiersSourceStart = this.intStack[this.intPtr--];
	typeDecl.modifiers = this.intStack[this.intPtr--];
	if (typeDecl.declarationSourceStart > declSourceStart) {
		typeDecl.declarationSourceStart = declSourceStart;
	}
	// consume annotations
	int length;
	if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
		System.arraycopy(
			this.expressionStack,
			(this.expressionPtr -= length) + 1,
			typeDecl.annotations = new Annotation[length],
			0,
			length);
	}
	typeDecl.bodyStart = typeDecl.sourceEnd + 1;
	pushOnAstStack(typeDecl);
	// javadoc
	typeDecl.javadoc = this.javadoc;
	this.javadoc = null;
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeCompilationUnit() {
	// CompilationUnit ::= EnterCompilationUnit PackageDeclarationopt ImportDeclarationsopt
	this.requestor.exitCompilationUnit(this.scanner.source.length - 1);
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeConstructorDeclaration() {
	// ConstructorDeclaration ::= ConstructorHeader ConstructorBody
	super.consumeConstructorDeclaration();
	if (isLocalDeclaration()) {
		// we ignore the local variable declarations
		return;
	}
	ConstructorDeclaration cd = (ConstructorDeclaration) this.astStack[this.astPtr];
	this.requestor.exitConstructor(this.endStatementPosition, cd.declarationSourceEnd);
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeConstructorHeader() {
	// ConstructorHeader ::= ConstructorHeaderName MethodHeaderParameters MethodHeaderThrowsClauseopt
	super.consumeConstructorHeader();
	if (isLocalDeclaration()) {
		// we ignore the local variable declarations
		this.intArrayPtr--;
		return;
	}
	ConstructorDeclaration cd = (ConstructorDeclaration) this.astStack[this.astPtr];
	Argument[] arguments = cd.arguments;
	char[][] argumentTypes = null;
	char[][] argumentNames = null;
	int[] argumentTypeStarts = null;
	int[] argumentTypeEnds = null;
	int[] argumentNameStarts = null;
	int[] argumentNameEnds = null;
	if (arguments != null) {
		int argumentLength = arguments.length;
		argumentTypes = new char[argumentLength][];
		argumentNames = new char[argumentLength][];
		argumentNameStarts = new int[argumentLength];
		argumentNameEnds = new int[argumentLength];
		argumentTypeStarts = new int[argumentLength];
		argumentTypeEnds = new int[argumentLength];
		for (int i = 0; i < argumentLength; i++) {
			Argument argument = arguments[i];
			TypeReference argumentType = argument.type;
			argumentTypes[i] = returnTypeName(argumentType);
			argumentNames[i] = argument.name;
			argumentNameStarts[i] = argument.sourceStart;
			argumentNameEnds[i] = argument.sourceEnd;
			argumentTypeStarts[i] = argumentType.sourceStart;
			argumentTypeEnds[i] = argumentType.sourceEnd;
		}
	}
	TypeReference[] thrownExceptions = cd.thrownExceptions;
	char[][] exceptionTypes = null;
	int[] exceptionTypeStarts = null;
	int[] exceptionTypeEnds = null;
	if (thrownExceptions != null) {
		int thrownExceptionLength = thrownExceptions.length;
		exceptionTypes = new char[thrownExceptionLength][];
		exceptionTypeStarts = new int[thrownExceptionLength];
		exceptionTypeEnds = new int[thrownExceptionLength];
		for (int i = 0; i < thrownExceptionLength; i++) {
			TypeReference exception = thrownExceptions[i];
			exceptionTypes[i] = CharOperation.concatWith(exception.getTypeName(), '.');
			exceptionTypeStarts[i] = exception.sourceStart;
			exceptionTypeEnds[i] = exception.sourceEnd;
		}
	}
	this.requestor
		.enterConstructor(
			cd.declarationSourceStart,
			this.intArrayStack[this.intArrayPtr--],
			cd.modifiers,
			cd.modifiersSourceStart,
			cd.selector,
			cd.sourceStart,
			(int) (this.selectorSourcePositions & 0xFFFFFFFFL),
			// retrieve the source end of the name
			argumentTypes,
			argumentTypeStarts,
			argumentTypeEnds,
			argumentNames,
			argumentNameStarts,
			argumentNameEnds,
			this.rParenPos,
			// right parenthesis
			exceptionTypes,
			exceptionTypeStarts,
			exceptionTypeEnds,
			this.scanner.currentPosition - 1);
}
@Override
protected void consumeConstructorHeaderName() {
	// ConstructorHeaderName ::=  Modifiersopt 'Identifier' '('
	ConstructorDeclaration cd = new ConstructorDeclaration(this.compilationUnit.compilationResult);

	//name -- this is not really revelant but we do .....
	cd.selector = this.identifierStack[this.identifierPtr];
	this.selectorSourcePositions = this.identifierPositionStack[this.identifierPtr--];
	this.identifierLengthPtr--;

	//modifiers
	cd.declarationSourceStart = this.intStack[this.intPtr--];
	cd.modifiersSourceStart = this.intStack[this.intPtr--];
	cd.modifiers = this.intStack[this.intPtr--];
	// consume annotations
	int length;
	if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
		System.arraycopy(
			this.expressionStack,
			(this.expressionPtr -= length) + 1,
			cd.annotations = new Annotation[length],
			0,
			length);
	}
	// javadoc
	cd.javadoc = this.javadoc;
	this.javadoc = null;

	//highlight starts at the selector starts
	cd.sourceStart = (int) (this.selectorSourcePositions >>> 32);
	pushOnAstStack(cd);

	cd.sourceEnd = this.lParenPos;
	cd.bodyStart = this.lParenPos + 1;
}
@Override
protected void consumeDefaultModifiers() {
	checkComment(); // might update modifiers with AccDeprecated
	pushOnIntStack(this.modifiers); // modifiers
	pushOnIntStack(-1);
	pushOnIntStack(
		this.declarationSourceStart >= 0 ? this.declarationSourceStart : this.scanner.startPosition);
	resetModifiers();
	pushOnExpressionStackLengthStack(0);
}
@Override
protected void consumeDiet() {
	// Diet ::= $empty
	super.consumeDiet();
	/* persisting javadoc positions
	 * Will be consume in consumeClassBodyDeclaration
	 */
	pushOnIntArrayStack(getJavaDocPositions());
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeEnterCompilationUnit() {
	// EnterCompilationUnit ::= $empty
	this.requestor.enterCompilationUnit();
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeEnterVariable() {
	// EnterVariable ::= $empty
	boolean isLocalDeclaration = isLocalDeclaration();
	if (!isLocalDeclaration && (this.variablesCounter[this.nestedType] != 0)) {
		this.requestor.exitField(this.lastFieldBodyEndPosition, this.lastFieldEndPosition);
	}
	char[] varName = this.identifierStack[this.identifierPtr];
	long namePosition = this.identifierPositionStack[this.identifierPtr--];
	int extendedTypeDimension = this.intStack[this.intPtr--];
	// pop any annotations on extended dimensions now, so they don't pollute the base dimensions.
	Annotation [][] annotationsOnExtendedDimensions = extendedTypeDimension == 0 ? null : getAnnotationsOnDimensions(extendedTypeDimension);


	AbstractVariableDeclaration declaration;
	if (this.nestedMethod[this.nestedType] != 0) {
		// create the local variable declarations
		declaration =
			new LocalDeclaration(varName, (int) (namePosition >>> 32), (int) namePosition);
	} else {
		// create the field declaration
		declaration =
			new FieldDeclaration(varName, (int) (namePosition >>> 32), (int) namePosition);
	}
	this.identifierLengthPtr--;
	TypeReference type;
	int variableIndex = this.variablesCounter[this.nestedType];
	if (variableIndex == 0) {
		// first variable of the declaration (FieldDeclaration or LocalDeclaration)
		if (this.nestedMethod[this.nestedType] != 0) {
			// local declaration
			declaration.declarationSourceStart = this.intStack[this.intPtr--];
			declaration.modifiersSourceStart = this.intStack[this.intPtr--];
			declaration.modifiers = this.intStack[this.intPtr--];
			type = getTypeReference(this.intStack[this.intPtr--]); // type dimension
			pushOnAstStack(type);
		} else {
			// field declaration
			type = getTypeReference(this.intStack[this.intPtr--]); // type dimension
			pushOnAstStack(type);
			declaration.declarationSourceStart = this.intStack[this.intPtr--];
			declaration.modifiersSourceStart = this.intStack[this.intPtr--];
			declaration.modifiers = this.intStack[this.intPtr--];
		}
		// consume annotations
		int length;
		if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
			System.arraycopy(
				this.expressionStack,
				(this.expressionPtr -= length) + 1,
				declaration.annotations = new Annotation[length],
				0,
				length);
		}
	} else {
		type = (TypeReference) this.astStack[this.astPtr - variableIndex];
		AbstractVariableDeclaration previousVariable =
			(AbstractVariableDeclaration) this.astStack[this.astPtr];
		declaration.declarationSourceStart = previousVariable.declarationSourceStart;
		declaration.modifiers = previousVariable.modifiers;
		declaration.modifiersSourceStart = previousVariable.modifiersSourceStart;
		final Annotation[] annotations = previousVariable.annotations;
		if (annotations != null) {
			final int annotationsLength = annotations.length;
			System.arraycopy(annotations, 0, declaration.annotations = new Annotation[annotationsLength], 0, annotationsLength);
		}
	}

	declaration.type = extendedTypeDimension != 0 ? augmentTypeWithAdditionalDimensions(type, extendedTypeDimension, annotationsOnExtendedDimensions, false) : type;
	declaration.bits |= (type.bits & ASTNode.HasTypeAnnotations);

	this.variablesCounter[this.nestedType]++;
	this.nestedMethod[this.nestedType]++;
	pushOnAstStack(declaration);

	int[] javadocPositions = this.intArrayStack[this.intArrayPtr];
	if (!isLocalDeclaration) {
		this.requestor
			.enterField(
				declaration.declarationSourceStart,
				javadocPositions,
				declaration.modifiers,
				declaration.modifiersSourceStart,
				returnTypeName(declaration.type),
				type.sourceStart,
				type.sourceEnd,
				this.typeDims,
				varName,
				(int) (namePosition >>> 32),
				(int) namePosition,
				extendedTypeDimension,
				extendedTypeDimension == 0 ? -1 : this.endPosition);
	}
}
@Override
protected void consumeEnhancedForStatementHeaderInit(boolean hasModifiers) {
	TypeReference type;

	char[] identifierName = this.identifierStack[this.identifierPtr];
	long namePosition = this.identifierPositionStack[this.identifierPtr];

	LocalDeclaration localDeclaration = createLocalDeclaration(identifierName, (int) (namePosition >>> 32), (int) namePosition);
	localDeclaration.declarationSourceEnd = localDeclaration.declarationEnd;

	int extraDims = this.intStack[this.intPtr--];
	this.identifierPtr--;
	this.identifierLengthPtr--;
	// remove fake modifiers/modifiers start
	int declarationSourceStart1 = 0;
	int modifiersSourceStart1 = 0;
	int modifiersValue  = 0;
	if (hasModifiers) {
		declarationSourceStart1 = this.intStack[this.intPtr--];
		modifiersSourceStart1 = this.intStack[this.intPtr--];
		modifiersValue = this.intStack[this.intPtr--];
	} else {
		this.intPtr-=3;
	}

	type = getTypeReference(this.intStack[this.intPtr--] + extraDims); // type dimension

	// consume annotations
	int length;
	if ((length = this.expressionLengthStack[this.expressionLengthPtr--])!= 0) {
		System.arraycopy(
			this.expressionStack,
			(this.expressionPtr -= length) + 1,
			localDeclaration.annotations = new Annotation[length],
			0,
			length);
		localDeclaration.bits |= ASTNode.HasTypeAnnotations;
	}
	if (hasModifiers) {
		localDeclaration.declarationSourceStart = declarationSourceStart1;
		localDeclaration.modifiersSourceStart = modifiersSourceStart1;
		localDeclaration.modifiers = modifiersValue;
	} else {
		localDeclaration.declarationSourceStart = type.sourceStart;
	}
	localDeclaration.type = type;
	localDeclaration.bits |= (type.bits & ASTNode.HasTypeAnnotations);
	ForeachStatement iteratorForStatement =
		new ForeachStatement(
			localDeclaration,
			this.intStack[this.intPtr--]);
	pushOnAstStack(iteratorForStatement);

	iteratorForStatement.sourceEnd = localDeclaration.declarationSourceEnd;
}
@Override
protected void consumeMethodHeaderNameWithTypeParameters(boolean isAnnotationMethod) {
	// MethodHeaderName ::= Modifiersopt TypeParameters Type 'Identifier' '('
	// AnnotationMethodHeaderName ::= Modifiersopt TypeParameters Type 'Identifier' '('
	// RecoveryMethodHeaderName ::= Modifiersopt TypeParameters Type 'Identifier' '('
	MethodDeclaration md = null;
	if(isAnnotationMethod) {
		md = new AnnotationMethodDeclaration(this.compilationUnit.compilationResult);
		this.recordStringLiterals = false;
	} else {
		md = new MethodDeclaration(this.compilationUnit.compilationResult);
	}

	//name
	md.selector = this.identifierStack[this.identifierPtr];
	long selectorSource = this.identifierPositionStack[this.identifierPtr--];
	this.identifierLengthPtr--;
	//type
	md.returnType = getTypeReference(this.intStack[this.intPtr--]);
	if (isAnnotationMethod)
		rejectIllegalLeadingTypeAnnotations(md.returnType);
	md.bits |= (md.returnType.bits & ASTNode.HasTypeAnnotations);
	// consume type parameters
	int length = this.genericsLengthStack[this.genericsLengthPtr--];
	this.genericsPtr -= length;
	System.arraycopy(this.genericsStack, this.genericsPtr + 1, md.typeParameters = new TypeParameter[length], 0, length);

	//modifiers
	md.declarationSourceStart = this.intStack[this.intPtr--];
	md.modifiersSourceStart = this.intStack[this.intPtr--];
	md.modifiers = this.intStack[this.intPtr--];
	// consume annotations
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
	pushOnAstStack(md);
	md.sourceEnd = this.lParenPos;
	md.bodyStart = this.lParenPos+1;
	this.listLength = 0; // initialize this.listLength before reading parameters/throws

	// recovery
	if (this.currentElement != null){
		boolean isType;
		if ((isType = this.currentElement instanceof RecoveredType)
			//|| md.modifiers != 0
			|| (Util.getLineNumber(md.returnType.sourceStart, this.scanner.lineEnds, 0, this.scanner.linePtr)
					== Util.getLineNumber(md.sourceStart, this.scanner.lineEnds, 0, this.scanner.linePtr))){
			if(isType) {
				((RecoveredType) this.currentElement).pendingTypeParameters = null;
			}
			this.lastCheckPoint = md.bodyStart;
			this.currentElement = this.currentElement.add(md, 0);
			this.lastIgnoredToken = -1;
		} else {
			this.lastCheckPoint = md.sourceStart;
			this.restartRecovery = true;
		}
	}
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeExitVariableWithInitialization() {
	// ExitVariableWithInitialization ::= $empty
	// the scanner is located after the comma or the semi-colon.
	// we want to include the comma or the semi-colon
	super.consumeExitVariableWithInitialization();
	this.nestedMethod[this.nestedType]--;
	this.lastFieldEndPosition = this.scanner.currentPosition - 1;
	this.lastFieldBodyEndPosition = 	((AbstractVariableDeclaration) this.astStack[this.astPtr]).initialization.sourceEnd;
}
@Override
protected void consumeExitVariableWithoutInitialization() {
	// ExitVariableWithoutInitialization ::= $empty
	// do nothing by default
	super.consumeExitVariableWithoutInitialization();
	this.nestedMethod[this.nestedType]--;
	this.lastFieldEndPosition = this.scanner.currentPosition - 1;
	this.lastFieldBodyEndPosition = this.scanner.startPosition - 1;
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeFieldDeclaration() {
	// See consumeLocalVariableDeclarationDefaultModifier() in case of change: duplicated code
	// FieldDeclaration ::= Modifiersopt Type VariableDeclarators ';'
	// the super.consumeFieldDeclaration will reinitialize the variableCounter[nestedType]
	int variableIndex = this.variablesCounter[this.nestedType];
	super.consumeFieldDeclaration();
	this.intArrayPtr--;
	if (isLocalDeclaration())
		return;
	if (variableIndex != 0) {
		this.requestor.exitField(this.lastFieldBodyEndPosition, this.lastFieldEndPosition);
	}
}
@Override
protected void consumeFormalParameter(boolean isVarArgs) {
	// FormalParameter ::= Type VariableDeclaratorId ==> false
	// FormalParameter ::= Modifiers Type VariableDeclaratorId ==> true
	/*
	astStack :
	identifierStack : type identifier
	intStack : dim dim 1||0  // 1 => normal parameter, 0 => this parameter
	 ==>
	astStack : Argument
	identifierStack :
	intStack :
	*/
	NameReference qualifyingNameReference = null;
    boolean isReceiver = this.intStack[this.intPtr--] == 0;
    if (isReceiver) {
    	qualifyingNameReference = (NameReference) this.expressionStack[this.expressionPtr--];
    	this.expressionLengthPtr --;
    }
	this.identifierLengthPtr--;
	char[] parameterName = this.identifierStack[this.identifierPtr];
	long namePositions = this.identifierPositionStack[this.identifierPtr--];
	int extendedDimensions = this.intStack[this.intPtr--];
	Annotation [][] annotationsOnExtendedDimensions = extendedDimensions == 0 ? null : getAnnotationsOnDimensions(extendedDimensions);
	int endOfEllipsis = 0;
	int length;
	Annotation [] varArgsAnnotations = null;
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
	this.intPtr -= 3;
	Argument arg;
	if (isReceiver) {
		arg = new Receiver(
				parameterName,
				namePositions,
				type,
				qualifyingNameReference,
				this.intStack[this.intPtr + 1] & ~ClassFileConstants.AccDeprecated);
	} else {
		arg = new Argument(
			parameterName,
			namePositions,
			type,
			this.intStack[this.intPtr + 1]);// modifiers
	}
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
	pushOnAstStack(arg);
	this.intArrayPtr--;
}
@Override
protected void consumeInstanceOfExpression() {
	super.consumeInstanceOfExpression();
	this.intPtr--; // skip declarationSourceStart of modifiers
}
@Override
protected void consumeInstanceOfExpressionWithName() {
	super.consumeInstanceOfExpressionWithName();
	this.intPtr--; // skip declarationSourceStart of modifiers
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeInterfaceDeclaration() {
	super.consumeInterfaceDeclaration();
	// we know that we have a TypeDeclaration on the top of the astStack
	if (isLocalDeclaration()) {
		// we ignore the local variable declarations
		return;
	}
	this.requestor.exitInterface(this.endStatementPosition, // the '}' is the end of the body
	 ((TypeDeclaration) this.astStack[this.astPtr]).declarationSourceEnd);
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeInterfaceHeader() {
	//InterfaceHeader ::= $empty
	super.consumeInterfaceHeader();
	if (isLocalDeclaration()) {
		// we ignore the local variable declarations
		this.intArrayPtr--;
		return;
	}
	TypeDeclaration typeDecl = (TypeDeclaration) this.astStack[this.astPtr];
	TypeReference[] superInterfaces = typeDecl.superInterfaces;
	char[][] interfaceNames = null;
	int[] interfaceNameStarts = null;
	int[] interfacenameEnds = null;
	int superInterfacesLength = 0;
	if (superInterfaces != null) {
		superInterfacesLength = superInterfaces.length;
		interfaceNames = new char[superInterfacesLength][];
		interfaceNameStarts = new int[superInterfacesLength];
		interfacenameEnds = new int[superInterfacesLength];
	}
	if (superInterfaces != null) {
		for (int i = 0; i < superInterfacesLength; i++) {
			TypeReference superInterface = superInterfaces[i];
			interfaceNames[i] = CharOperation.concatWith(superInterface.getTypeName(), '.');
			interfaceNameStarts[i] = superInterface.sourceStart;
			interfacenameEnds[i] = superInterface.sourceEnd;
		}
	}
	// flush the comments related to the interface header
	this.scanner.commentPtr = -1;
	this.requestor.enterInterface(
		typeDecl.declarationSourceStart,
		this.intArrayStack[this.intArrayPtr--],
		typeDecl.modifiers,
		typeDecl.modifiersSourceStart,
		this.typeStartPosition,
		typeDecl.name,
		typeDecl.sourceStart,
		typeDecl.sourceEnd,
		interfaceNames,
		interfaceNameStarts,
		interfacenameEnds,
		this.scanner.currentPosition - 1);
}
@Override
protected void consumeInterfaceHeaderName1() {
	// InterfaceHeaderName ::= Modifiersopt 'interface' 'Identifier'
	TypeDeclaration typeDecl = new TypeDeclaration(this.compilationUnit.compilationResult);
	if (this.nestedMethod[this.nestedType] == 0) {
		if (this.nestedType != 0) {
			typeDecl.bits |= ASTNode.IsMemberType;
		}
	} else {
		// Record that the block has a declaration for local types
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
	// 'class' and 'interface' push an int position
	this.typeStartPosition = typeDecl.declarationSourceStart = this.intStack[this.intPtr--];
	this.intPtr--;
	int declSourceStart = this.intStack[this.intPtr--];
	typeDecl.modifiersSourceStart = this.intStack[this.intPtr--];
	typeDecl.modifiers = this.intStack[this.intPtr--] | ClassFileConstants.AccInterface;
	if (typeDecl.declarationSourceStart > declSourceStart) {
		typeDecl.declarationSourceStart = declSourceStart;
	}
	// consume annotations
	int length;
	if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
		System.arraycopy(
			this.expressionStack,
			(this.expressionPtr -= length) + 1,
			typeDecl.annotations = new Annotation[length],
			0,
			length);
	}
	typeDecl.bodyStart = typeDecl.sourceEnd + 1;
	pushOnAstStack(typeDecl);
	// javadoc
	typeDecl.javadoc = this.javadoc;
	this.javadoc = null;
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
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeLocalVariableDeclaration() {
	// See consumeLocalVariableDeclarationDefaultModifier() in case of change: duplicated code
	// FieldDeclaration ::= Modifiersopt Type VariableDeclarators ';'

	super.consumeLocalVariableDeclaration();
	this.intArrayPtr--;
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeMethodDeclaration(boolean isNotAbstract, boolean isDefaultMethod) {
	// MethodDeclaration ::= MethodHeader MethodBody
	// AbstractMethodDeclaration ::= MethodHeader ';'
	super.consumeMethodDeclaration(isNotAbstract, isDefaultMethod);
	if (isLocalDeclaration()) {
		// we ignore the local variable declarations
		return;
	}
	MethodDeclaration md = (MethodDeclaration) this.astStack[this.astPtr];
	this.requestor.exitMethod(this.endStatementPosition, md.declarationSourceEnd);
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeMethodHeader() {
	// MethodHeader ::= MethodHeaderName MethodHeaderParameters MethodHeaderExtendedDims ThrowsClauseopt
	super.consumeMethodHeader();
	if (isLocalDeclaration()) {
		// we ignore the local variable declarations
		this.intArrayPtr--;
		return;
	}
	MethodDeclaration md = (MethodDeclaration) this.astStack[this.astPtr];

	TypeReference returnType = md.returnType;
	char[] returnTypeName = returnTypeName(returnType);
	Argument[] arguments = md.arguments;
	char[][] argumentTypes = null;
	char[][] argumentNames = null;
	int[] argumentTypeStarts = null;
	int[] argumentTypeEnds = null;
	int[] argumentNameStarts = null;
	int[] argumentNameEnds = null;
	if (arguments != null) {
		int argumentLength = arguments.length;
		argumentTypes = new char[argumentLength][];
		argumentNames = new char[argumentLength][];
		argumentNameStarts = new int[argumentLength];
		argumentNameEnds = new int[argumentLength];
		argumentTypeStarts = new int[argumentLength];
		argumentTypeEnds = new int[argumentLength];
		for (int i = 0; i < argumentLength; i++) {
			Argument argument = arguments[i];
			TypeReference argumentType = argument.type;
			argumentTypes[i] = returnTypeName(argumentType);
			argumentNames[i] = argument.name;
			argumentNameStarts[i] = argument.sourceStart;
			argumentNameEnds[i] = argument.sourceEnd;
			argumentTypeStarts[i] = argumentType.sourceStart;
			argumentTypeEnds[i] = argumentType.sourceEnd;
		}
	}
	TypeReference[] thrownExceptions = md.thrownExceptions;
	char[][] exceptionTypes = null;
	int[] exceptionTypeStarts = null;
	int[] exceptionTypeEnds = null;
	if (thrownExceptions != null) {
		int thrownExceptionLength = thrownExceptions.length;
		exceptionTypeStarts = new int[thrownExceptionLength];
		exceptionTypeEnds = new int[thrownExceptionLength];
		exceptionTypes = new char[thrownExceptionLength][];
		for (int i = 0; i < thrownExceptionLength; i++) {
			TypeReference exception = thrownExceptions[i];
			exceptionTypes[i] = CharOperation.concatWith(exception.getTypeName(), '.');
			exceptionTypeStarts[i] = exception.sourceStart;
			exceptionTypeEnds[i] = exception.sourceEnd;
		}
	}
	this.requestor
		.enterMethod(
			md.declarationSourceStart,
			this.intArrayStack[this.intArrayPtr--],
			md.modifiers,
			md.modifiersSourceStart,
			returnTypeName,
			returnType.sourceStart,
			returnType.sourceEnd,
			this.typeDims,
			md.selector,
			md.sourceStart,
			(int) (this.selectorSourcePositions & 0xFFFFFFFFL),
			argumentTypes,
			argumentTypeStarts,
			argumentTypeEnds,
			argumentNames,
			argumentNameStarts,
			argumentNameEnds,
			this.rParenPos,
			this.extendsDim,
			this.extendsDim == 0 ? -1 : this.endPosition,
			exceptionTypes,
			exceptionTypeStarts,
			exceptionTypeEnds,
			this.scanner.currentPosition - 1);
}
@Override
protected void consumeMethodHeaderExtendedDims() {
	// MethodHeaderExtendedDims ::= Dimsopt
	// now we update the returnType of the method
	MethodDeclaration md = (MethodDeclaration) this.astStack[this.astPtr];
	int extendedDims = this.intStack[this.intPtr--];
	this.extendsDim = extendedDims;
	if (extendedDims != 0) {
		md.sourceEnd = this.endPosition;
		md.returnType = augmentTypeWithAdditionalDimensions(md.returnType, extendedDims, getAnnotationsOnDimensions(extendedDims), false);
		md.bits |= (md.returnType.bits & ASTNode.HasTypeAnnotations);
		if (this.currentToken == TokenNameLBRACE) {
			md.bodyStart = this.endPosition + 1;
		}
	}
}
@Override
protected void consumeMethodHeaderName(boolean isAnnotationMethod) {
	// MethodHeaderName ::= Modifiersopt Type 'Identifier' '('
	MethodDeclaration md = null;
	if(isAnnotationMethod) {
		md = new AnnotationMethodDeclaration(this.compilationUnit.compilationResult);
	} else {
		md = new MethodDeclaration(this.compilationUnit.compilationResult);
	}
	//name
	md.selector = this.identifierStack[this.identifierPtr];
	this.selectorSourcePositions = this.identifierPositionStack[this.identifierPtr--];
	this.identifierLengthPtr--;
	//type
	md.returnType = getTypeReference(this.typeDims = this.intStack[this.intPtr--]);
	md.bits |= (md.returnType.bits & ASTNode.HasTypeAnnotations);
	//modifiers
	md.declarationSourceStart = this.intStack[this.intPtr--];
	md.modifiersSourceStart = this.intStack[this.intPtr--];
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
	md.sourceStart = (int) (this.selectorSourcePositions >>> 32);
	pushOnAstStack(md);
	md.bodyStart = this.scanner.currentPosition-1;
}
@Override
protected void consumeModifiers() {
	checkComment(); // might update modifiers with AccDeprecated
	pushOnIntStack(this.modifiers); // modifiers
	pushOnIntStack(this.modifiersSourceStart);
	pushOnIntStack(
		this.declarationSourceStart >= 0 ? this.declarationSourceStart : this.modifiersSourceStart);
	resetModifiers();
}
@Override
protected void consumePackageComment() {
	// get possible comment for syntax since 1.5
	if(this.options.sourceLevel >= ClassFileConstants.JDK1_5) {
		checkComment();
	} else {
		pushOnIntArrayStack(getJavaDocPositions());
	}
	resetModifiers();
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumePackageDeclarationName() {
	/*
	 * Javadoc positions are persisted in consumePackageComment
	 */
	super.consumePackageDeclarationName();
	ImportReference importReference = this.compilationUnit.currentPackage;

	this.requestor.acceptPackage(
		importReference.declarationSourceStart,
		importReference.declarationSourceEnd,
		this.intArrayStack[this.intArrayPtr--],
		CharOperation.concatWith(importReference.getImportName(), '.'),
		importReference.sourceStart);
}
/*
*
* INTERNAL USE-ONLY
*/
@Override
protected void consumePackageDeclarationNameWithModifiers() {
	super.consumePackageDeclarationNameWithModifiers();
	ImportReference importReference = this.compilationUnit.currentPackage;

	this.requestor.acceptPackage(
		importReference.declarationSourceStart,
		importReference.declarationSourceEnd,
		this.intArrayStack[this.intArrayPtr--],
		CharOperation.concatWith(importReference.getImportName(), '.'),
		importReference.sourceStart);
}
@Override
protected void consumePushModifiers() {
	checkComment(); // might update modifiers with AccDeprecated
	pushOnIntStack(this.modifiers); // modifiers
	if (this.modifiersSourceStart < 0) {
		pushOnIntStack(-1);
		pushOnIntStack(
			this.declarationSourceStart >= 0 ? this.declarationSourceStart : this.scanner.startPosition);
	} else {
		pushOnIntStack(this.modifiersSourceStart);
		pushOnIntStack(
			this.declarationSourceStart >= 0 ? this.declarationSourceStart : this.modifiersSourceStart);
	}
	resetModifiers();
	pushOnExpressionStackLengthStack(0);
}
@Override
protected void consumePushRealModifiers() {
	checkComment(); // might update modifiers with AccDeprecated
	pushOnIntStack(this.modifiers); // modifiers
	if (this.modifiersSourceStart < 0) {
		pushOnIntStack(-1);
		pushOnIntStack(
			this.declarationSourceStart >= 0 ? this.declarationSourceStart : this.scanner.startPosition);
	} else {
		pushOnIntStack(this.modifiersSourceStart);
		pushOnIntStack(
			this.declarationSourceStart >= 0 ? this.declarationSourceStart : this.modifiersSourceStart);
	}
	resetModifiers();
}
@Override
protected void consumeSingleStaticImportDeclarationName() {
	// SingleTypeImportDeclarationName ::= 'import' 'static' Name

	/* persisting javadoc positions */
	pushOnIntArrayStack(getJavaDocPositions());

	super.consumeSingleStaticImportDeclarationName();
	ImportReference importReference = (ImportReference) this.astStack[this.astPtr];
	this.requestor.acceptImport(
		importReference.declarationSourceStart,
		importReference.declarationSourceEnd,
		this.intArrayStack[this.intArrayPtr--],
		CharOperation.concatWith(importReference.getImportName(), '.'),
		importReference.sourceStart,
		false,
		ClassFileConstants.AccStatic);
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeSingleTypeImportDeclarationName() {
	// SingleTypeImportDeclarationName ::= 'import' Name

	/* persisting javadoc positions */
	pushOnIntArrayStack(getJavaDocPositions());

	super.consumeSingleTypeImportDeclarationName();
	ImportReference importReference = (ImportReference) this.astStack[this.astPtr];
	this.requestor.acceptImport(
		importReference.declarationSourceStart,
		importReference.declarationSourceEnd,
		this.intArrayStack[this.intArrayPtr--],
		CharOperation.concatWith(importReference.getImportName(), '.'),
		importReference.sourceStart,
		false,
		ClassFileConstants.AccDefault);
}
@Override
protected void consumeStaticImportOnDemandDeclarationName() {
	// SingleTypeImportDeclarationName ::= 'import' 'static' Name '.' '*'

	/* persisting javadoc positions */
	pushOnIntArrayStack(getJavaDocPositions());

	super.consumeStaticImportOnDemandDeclarationName();
	ImportReference importReference = (ImportReference) this.astStack[this.astPtr];
	this.requestor.acceptImport(
		importReference.declarationSourceStart,
		importReference.declarationSourceEnd,
		this.intArrayStack[this.intArrayPtr--],
		CharOperation.concatWith(importReference.getImportName(), '.'),
		importReference.sourceStart,
		true,
		ClassFileConstants.AccStatic);
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeStaticInitializer() {
	// StaticInitializer ::=  StaticOnly Block
	//push an Initializer
	//optimize the push/pop
	super.consumeStaticInitializer();
	Initializer initializer = (Initializer) this.astStack[this.astPtr];
	this.requestor.acceptInitializer(
		initializer.declarationSourceStart,
		initializer.declarationSourceEnd,
		this.intArrayStack[this.intArrayPtr--],
		ClassFileConstants.AccStatic,
		this.intStack[this.intPtr--],
		initializer.block.sourceStart,
		initializer.declarationSourceEnd);
}
@Override
protected void consumeStaticOnly() {
	// StaticOnly ::= 'static'
	checkComment(); // might update declaration source start
	pushOnIntStack(this.modifiersSourceStart);
	pushOnIntStack(this.scanner.currentPosition);
	pushOnIntStack(
		this.declarationSourceStart >= 0 ? this.declarationSourceStart : this.modifiersSourceStart);
	jumpOverMethodBody();
	this.nestedMethod[this.nestedType]++;
	resetModifiers();
}
/*
 *
 * INTERNAL USE-ONLY
 */
@Override
protected void consumeTypeImportOnDemandDeclarationName() {
	// TypeImportOnDemandDeclarationName ::= 'import' Name '.' '*'

	/* persisting javadoc positions */
	pushOnIntArrayStack(getJavaDocPositions());

	super.consumeTypeImportOnDemandDeclarationName();
	ImportReference importReference = (ImportReference) this.astStack[this.astPtr];
	this.requestor.acceptImport(
		importReference.declarationSourceStart,
		importReference.declarationSourceEnd,
		this.intArrayStack[this.intArrayPtr--],
		CharOperation.concatWith(importReference.getImportName(), '.'),
		importReference.sourceStart,
		true,
		ClassFileConstants.AccDefault);
}
/*
 * Flush javadocs defined prior to a given positions.
 *
 * Note: javadocs are stacked in syntactical order
 *
 * Either answer given <position>, or the end position of a comment line
 * immediately following the <position> (same line)
 *
 * e.g.
 * void foo(){
 * } // end of method foo
 */

@Override
public int flushCommentsDefinedPriorTo(int position) {

	return this.lastFieldEndPosition = super.flushCommentsDefinedPriorTo(position);
}
@Override
public CompilationUnitDeclaration endParse(int act) {
	if (this.scanner.recordLineSeparator) {
		this.requestor.acceptLineSeparatorPositions(this.scanner.getLineEnds());
	}
	return super.endParse(act);
}
@Override
public void initialize(boolean parsingCompilationUnit) {
	//positionning the parser for a new compilation unit
	//avoiding stack reallocation and all that....
	super.initialize(parsingCompilationUnit);
	this.intArrayPtr = -1;
}
@Override
public void initialize() {
	//positionning the parser for a new compilation unit
	//avoiding stack reallocation and all that....
	super.initialize();
	this.intArrayPtr = -1;
}
/*
 *
 * INTERNAL USE-ONLY
 */
private boolean isLocalDeclaration() {
	int nestedDepth = this.nestedType;
	while (nestedDepth >= 0) {
		if (this.nestedMethod[nestedDepth] != 0) {
			return true;
		}
		nestedDepth--;
	}
	return false;
}
@Override
protected void parse() {
	this.diet = true;
	this.dietInt = 0;
	super.parse();
}
/*
 * Investigate one entire unit.
 */
public void parseCompilationUnit(ICompilationUnit unit) {
	char[] regionSource = unit.getContents();
	try {
		initialize(true);
		goForCompilationUnit();
		this.referenceContext =
			this.compilationUnit =
				new CompilationUnitDeclaration(
					problemReporter(),
					new CompilationResult(unit, 0, 0, this.options.maxProblemsPerUnit),
					regionSource.length);
		this.scanner.resetTo(0, regionSource.length);
		this.scanner.setSource(regionSource);
		parse();
	} catch (AbortCompilation ex) {
		// ignore this exception
	}
}
/*
 * Investigate one constructor declaration.
 */
public void parseConstructor(char[] regionSource) {
	try {
		initialize();
		goForClassBodyDeclarations();
		this.referenceContext =
			this.compilationUnit =
				new CompilationUnitDeclaration(
					problemReporter(),
					new CompilationResult(regionSource, 0, 0, this.options.maxProblemsPerUnit),
					regionSource.length);
		this.scanner.resetTo(0, regionSource.length);
		this.scanner.setSource(regionSource);
		parse();
	} catch (AbortCompilation ex) {
		// ignore this exception
	}
}
/*
 * Investigate one field declaration statement (might have multiple declarations in it).
 */
public void parseField(char[] regionSource) {
	try {
		initialize();
		goForFieldDeclaration();
		this.referenceContext =
			this.compilationUnit =
				new CompilationUnitDeclaration(
					problemReporter(),
					new CompilationResult(regionSource, 0, 0, this.options.maxProblemsPerUnit),
					regionSource.length);
		this.scanner.resetTo(0, regionSource.length);
		this.scanner.setSource(regionSource);
		parse();
	} catch (AbortCompilation ex) {
		// ignore this exception
	}

}
/*
 * Investigate one import statement declaration.
 */
public void parseImport(char[] regionSource) {
	try {
		initialize();
		goForImportDeclaration();
		this.referenceContext =
			this.compilationUnit =
				new CompilationUnitDeclaration(
					problemReporter(),
					new CompilationResult(regionSource, 0, 0, this.options.maxProblemsPerUnit),
					regionSource.length);
		this.scanner.resetTo(0, regionSource.length);
		this.scanner.setSource(regionSource);
		parse();
	} catch (AbortCompilation ex) {
		// ignore this exception
	}

}
/*
 * Investigate one initializer declaration.
 * regionSource need to content exactly an initializer declaration.
 * e.g: static { i = 4; }
 * { name = "test"; }
 */
public void parseInitializer(char[] regionSource) {
	try {
		initialize();
		goForInitializer();
		this.referenceContext =
			this.compilationUnit =
				new CompilationUnitDeclaration(
					problemReporter(),
					new CompilationResult(regionSource, 0, 0, this.options.maxProblemsPerUnit),
					regionSource.length);
		this.scanner.resetTo(0, regionSource.length);
		this.scanner.setSource(regionSource);
		parse();
	} catch (AbortCompilation ex) {
		// ignore this exception
	}

}
/*
 * Investigate one method declaration.
 */
public void parseMethod(char[] regionSource) {
	try {
		initialize();
		goForGenericMethodDeclaration();
		this.referenceContext =
			this.compilationUnit =
				new CompilationUnitDeclaration(
					problemReporter(),
					new CompilationResult(regionSource, 0, 0, this.options.maxProblemsPerUnit),
					regionSource.length);
		this.scanner.resetTo(0, regionSource.length);
		this.scanner.setSource(regionSource);
		parse();
	} catch (AbortCompilation ex) {
		// ignore this exception
	}
}
/*
 * Investigate one package statement declaration.
 */
public void parsePackage(char[] regionSource) {
	try {
		initialize();
		goForPackageDeclaration();
		this.referenceContext =
			this.compilationUnit =
				new CompilationUnitDeclaration(
					problemReporter(),
					new CompilationResult(regionSource, 0, 0, this.options.maxProblemsPerUnit),
					regionSource.length);
		this.scanner.resetTo(0, regionSource.length);
		this.scanner.setSource(regionSource);
		parse();
	} catch (AbortCompilation ex) {
		// ignore this exception
	}

}
/*
 * Investigate one type declaration, its fields, methods and member types.
 */
public void parseType(char[] regionSource) {
	try {
		initialize();
		goForTypeDeclaration();
		this.referenceContext =
			this.compilationUnit =
				new CompilationUnitDeclaration(
					problemReporter(),
					new CompilationResult(regionSource, 0, 0, this.options.maxProblemsPerUnit),
					regionSource.length);
		this.scanner.resetTo(0, regionSource.length);
		this.scanner.setSource(regionSource);
		parse();
	} catch (AbortCompilation ex) {
		// ignore this exception
	}

}
/**
 * Returns this parser's problem reporter initialized with its reference context.
 * Also it is assumed that a problem is going to be reported, so initializes
 * the compilation result's line positions.
 *
 * @return ProblemReporter
 */
@Override
public ProblemReporter problemReporter() {
	this.problemReporter.referenceContext = this.referenceContext;
	return this.problemReporter;
}
protected void pushOnIntArrayStack(int[] positions) {

	int stackLength = this.intArrayStack.length;
	if (++this.intArrayPtr >= stackLength) {
		System.arraycopy(
			this.intArrayStack, 0,
			this.intArrayStack = new int[stackLength + StackIncrement][], 0,
			stackLength);
	}
	this.intArrayStack[this.intArrayPtr] = positions;
}
@Override
protected void resetModifiers() {
	super.resetModifiers();
	this.declarationSourceStart = -1;
}
/*
 * Syntax error was detected. Will attempt to perform some recovery action in order
 * to resume to the regular parse loop.
 */
@Override
protected int resumeOnSyntaxError() {
	return HALT;
}
/*
 * Answer a char array representation of the type name formatted like:
 * - type name + dimensions
 * Example:
 * "A[][]".toCharArray()
 * "java.lang.String".toCharArray()
 */
private char[] returnTypeName(TypeReference type) {
	int dimension = type.dimensions();
	if (dimension != 0) {
		char[] dimensionsArray = new char[dimension * 2];
		for (int i = 0; i < dimension; i++) {
			dimensionsArray[i*2] = '[';
			dimensionsArray[(i*2) + 1] = ']';
		}
		return CharOperation.concat(
			CharOperation.concatWith(type.getTypeName(), '.'),
			dimensionsArray);
	}
	return CharOperation.concatWith(type.getTypeName(), '.');
}
@Override
public String toString() {
	StringBuilder buffer = new StringBuilder();
	buffer.append("intArrayPtr = " + this.intArrayPtr + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
	buffer.append(super.toString());
	return buffer.toString();
}
}
