/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
package org.eclipse.jdt.internal.codeassist.impl;

import java.util.Arrays;

/*
 * Parser extension for code assist task
 *
 */

import java.util.HashSet;

import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.CaseStatement;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ForeachStatement;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.LambdaExpression;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ModuleDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ModuleReference;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.RequiresStatement;
import org.eclipse.jdt.internal.compiler.ast.Statement;
import org.eclipse.jdt.internal.compiler.ast.SuperReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.parser.RecoveredBlock;
import org.eclipse.jdt.internal.compiler.parser.RecoveredElement;
import org.eclipse.jdt.internal.compiler.parser.RecoveredExportsStatement;
import org.eclipse.jdt.internal.compiler.parser.RecoveredField;
import org.eclipse.jdt.internal.compiler.parser.RecoveredInitializer;
import org.eclipse.jdt.internal.compiler.parser.RecoveredLocalVariable;
import org.eclipse.jdt.internal.compiler.parser.RecoveredMethod;
import org.eclipse.jdt.internal.compiler.parser.RecoveredStatement;
import org.eclipse.jdt.internal.compiler.parser.RecoveredType;
import org.eclipse.jdt.internal.compiler.parser.RecoveredUnit;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
public abstract class AssistParser extends Parser {
	public ASTNode assistNode;
	public boolean isOrphanCompletionNode;
	private boolean resumedAfterRepair = false;
	// last modifiers info
	protected int lastModifiers = ClassFileConstants.AccDefault;
	protected int lastModifiersStart = -1;
	/* recovery */
	int[] blockStarts = new int[30];

	// the previous token read by the scanner
	protected int previousToken;

	// the index in the identifier stack of the previous identifier
	protected int previousIdentifierPtr;

	// depth of '(', '{' and '[]'
	protected int bracketDepth;

	// element stack
	protected static final int ElementStackIncrement = 100;
	protected int elementPtr;
	protected int[] elementKindStack = new int[ElementStackIncrement];
	protected int[] elementInfoStack = new int[ElementStackIncrement];
	protected Object[] elementObjectInfoStack = new Object[ElementStackIncrement];
	protected int previousKind;
	protected int previousInfo;
	protected Object previousObjectInfo;

	// OWNER
	protected static final int ASSIST_PARSER = 512;

	// KIND : all values known by AssistParser are between 513 and 1023
	protected static final int K_SELECTOR = ASSIST_PARSER + 1; // whether we are inside a message send
	protected static final int K_TYPE_DELIMITER = ASSIST_PARSER + 2; // whether we are inside a type declaration
	protected static final int K_METHOD_DELIMITER = ASSIST_PARSER + 3; // whether we are inside a method declaration
	protected static final int K_FIELD_INITIALIZER_DELIMITER = ASSIST_PARSER + 4; // whether we are inside a field initializer
	protected static final int K_ATTRIBUTE_VALUE_DELIMITER = ASSIST_PARSER + 5; // whether we are inside a annotation attribute valuer
	protected static final int K_ENUM_CONSTANT_DELIMITER = ASSIST_PARSER + 6; // whether we are inside a field initializer
	protected static final int K_LAMBDA_EXPRESSION_DELIMITER = ASSIST_PARSER + 7; // whether we are inside a lambda expression
	protected static final int K_MODULE_INFO_DELIMITER = ASSIST_PARSER + 8; // whether we are inside a module info declaration
	protected static final int K_SWITCH_EXPRESSION_DELIMITTER = ASSIST_PARSER + 9; // whether we are inside a switch expression

	// selector constants
	protected static final int THIS_CONSTRUCTOR = -1;
	protected static final int SUPER_CONSTRUCTOR = -2;

	// enum constant constants
	protected static final int NO_BODY = 0;
	protected static final int WITH_BODY = 1;

	protected static final int EXPRESSION_BODY = 0;
	protected static final int BLOCK_BODY = 1;

	protected boolean isFirst = false;

	/**
	 * Each nested block may capture a snapshot at its start, which may be updated during commit().
	 * {@link #snapShotPositions} is a matching stack of bodyStart positions.
	 * Both stacks are indexed by their shared stack pointer {@link #snapShotPtr}.
	 */
	AssistParser[] snapShotStack = new AssistParser[3];
	int[] snapShotPositions = new int[3];
	int snapShotPtr = -1;
	AssistParser lastResumeState;
	int resumeCount = 0;

	public int cursorLocation = Integer.MAX_VALUE;

	protected static final int[] RECOVERY_TOKENS = { TokenNameSEMICOLON, TokenNameRPAREN, TokenNameRBRACE, TokenNameRBRACKET};


public AssistParser(ProblemReporter problemReporter) {
	super(problemReporter, true);
	this.javadocParser.checkDocComment = false;

	setMethodsFullRecovery(false);
	setStatementsRecovery(false);
}

public abstract char[] assistIdentifier();

@Override
public void copyState(Parser from) {

	super.copyState(from);

	AssistParser parser = (AssistParser) from;

	this.previousToken = parser.previousToken;
	this.previousIdentifierPtr = parser.previousIdentifierPtr;

	this.lastModifiers = parser.lastModifiers;
	this.lastModifiersStart = parser.lastModifiersStart;

	this.bracketDepth = parser.bracketDepth;
	this.elementPtr = parser.elementPtr;

	int length;
	System.arraycopy(parser.blockStarts, 0, this.blockStarts = new int [length = parser.blockStarts.length], 0, length);
	System.arraycopy(parser.elementKindStack, 0, this.elementKindStack = new int [length = parser.elementKindStack.length], 0, length);
	System.arraycopy(parser.elementInfoStack, 0, this.elementInfoStack = new int [length = parser.elementInfoStack.length], 0, length);
	System.arraycopy(parser.elementObjectInfoStack, 0, this.elementObjectInfoStack = new Object [length = parser.elementObjectInfoStack.length], 0, length);

	this.previousKind = parser.previousKind;
	this.previousInfo = parser.previousInfo;
	this.previousObjectInfo = parser.previousObjectInfo;
}
/**
 * The parser become a simple parser which behave like a Parser
 * @return the state of the assist parser to be able to restore the assist parser state
 */
public Object becomeSimpleParser() {
	return null;
}
/**
 * Restore the parser as an assist parser
 * @param parserState
 */
public void restoreAssistParser(Object parserState) {
	//Do nothing
}
public int bodyEnd(AbstractMethodDeclaration method){
	return method.bodyEnd;
}
public int bodyEnd(Initializer initializer){
	return initializer.declarationSourceEnd;
}
/*
 * Build initial recovery state.
 * Recovery state is inferred from the current state of the parser (reduced node stack).
 */
@Override
public RecoveredElement buildInitialRecoveryState(){
	/* recovery in unit structure */
	if (this.referenceContext instanceof CompilationUnitDeclaration){
		RecoveredElement element = super.buildInitialRecoveryState();
		flushAssistState();
		flushElementStack();
		this.snapShotPtr = -1;
		initModuleInfo(element);
		return element;
	}

	/* recovery in method body */
	this.lastCheckPoint = 0;

	RecoveredElement element = null;
	if (this.referenceContext instanceof AbstractMethodDeclaration){
		element = new RecoveredMethod((AbstractMethodDeclaration) this.referenceContext, null, 0, this);
		this.lastCheckPoint = ((AbstractMethodDeclaration) this.referenceContext).bodyStart;
	} else {
		/* Initializer bodies are parsed in the context of the type declaration, we must thus search it inside */
		if (this.referenceContext instanceof TypeDeclaration){
			TypeDeclaration type = (TypeDeclaration) this.referenceContext;
			FieldDeclaration[] fields = type.fields;
			int length = fields == null ? 0 : fields.length;
			for (int i = 0; i < length; i++){
				FieldDeclaration field = fields[i];
				if (field != null
						&& field.getKind() == AbstractVariableDeclaration.INITIALIZER
						&& field.declarationSourceStart <= this.scanner.initialPosition
						&& this.scanner.initialPosition <= field.declarationSourceEnd
						&& this.scanner.eofPosition <= field.declarationSourceEnd+1){
					element = new RecoveredInitializer(field, null, 1, this);
					this.lastCheckPoint = field.declarationSourceStart;
					break;
				}
			}
		}
	}

	if (element == null) return element;

	/* add initial block */
	Block block = new Block(0);
	int lastStart = this.blockStarts[0];
	block.sourceStart = lastStart;
	element = element.add(block, 1);
	int blockIndex = 1;	// ignore first block start, since manually rebuilt here

	ASTNode node = null, lastNode = null;
	for (int i = 0; i <= this.astPtr; i++, lastNode = node) {
		node = this.astStack[i];
		/* check for intermediate block creation, so recovery can properly close them afterwards */
		int nodeStart = node.sourceStart;
		for (int j = blockIndex; j <= this.realBlockPtr; j++){
			if (this.blockStarts[j] >= 0) {
				if (this.blockStarts[j] > nodeStart){
					blockIndex = j; // shift the index to the new block
					break;
				}
				if (this.blockStarts[j] != lastStart){ // avoid multiple block if at same position
					block = new Block(0);
					block.sourceStart = lastStart = this.blockStarts[j];
					element = element.add(block, 1);
				}
			} else {
				if (-this.blockStarts[j] > nodeStart){
					blockIndex = j; // shift the index to the new block
					break;
				}
				block = new Block(0);
				block.sourceStart = lastStart = -this.blockStarts[j];
				element = element.add(block, 1);
			}
			blockIndex = j+1; // shift the index to the new block
		}

		if (node instanceof LocalDeclaration){
			LocalDeclaration local = (LocalDeclaration) node;
			if (local.declarationSourceEnd == 0){
				element = element.add(local, 0);
				if (local.initialization == null){
					this.lastCheckPoint = local.sourceEnd + 1;
				} else {
					this.lastCheckPoint = local.initialization.sourceEnd + 1;
				}
			} else {
				if (!local.isArgument()) {
					element = element.add(local, 0);
				} else {
					// Bug 442868 - For arguments, let the method continue to be the current element.
					element.add(local, 0);
				}
				this.lastCheckPoint = local.declarationSourceEnd + 1;
			}
			continue;
		}
		if (node instanceof AbstractMethodDeclaration){
			AbstractMethodDeclaration method = (AbstractMethodDeclaration) node;
			if (method.declarationSourceEnd == 0){
				element = element.add(method, 0);
				this.lastCheckPoint = method.bodyStart;
			} else {
				element = element.add(method, 0);
				this.lastCheckPoint = method.declarationSourceEnd + 1;
			}
			continue;
		}
		if (node instanceof Initializer){
			Initializer initializer = (Initializer) node;
			if (initializer.declarationSourceEnd == 0){
				element = element.add(initializer, 1);
				this.lastCheckPoint = initializer.sourceStart;
			} else {
				element = element.add(initializer, 0);
				this.lastCheckPoint = initializer.declarationSourceEnd + 1;
			}
			continue;
		}
		if (node instanceof FieldDeclaration){
			FieldDeclaration field = (FieldDeclaration) node;
			if (field.declarationSourceEnd == 0){
				element = element.add(field, 0);
				if (field.initialization == null){
					this.lastCheckPoint = field.sourceEnd + 1;
				} else {
					this.lastCheckPoint = field.initialization.sourceEnd + 1;
				}
			} else {
				element = element.add(field, 0);
				this.lastCheckPoint = field.declarationSourceEnd + 1;
			}
			continue;
		}
		if (node instanceof TypeDeclaration){
			TypeDeclaration type = (TypeDeclaration) node;
			if (type.declarationSourceEnd == 0){
				element = element.add(type, 0);
				this.lastCheckPoint = type.bodyStart;
			} else {
				element = element.add(type, 0);
				this.lastCheckPoint = type.declarationSourceEnd + 1;
			}
			continue;
		}
		if (this.assistNode != null && node instanceof Statement) {
			Statement stmt = (Statement) node;
			if (!(stmt instanceof Expression && ((Expression) stmt).isTrulyExpression()) || ((Expression) stmt).statementExpression()) {
				if (this.assistNode.sourceStart >= stmt.sourceStart && this.assistNode.sourceEnd <= stmt.sourceEnd) {
					element.add(stmt, 0);
					this.lastCheckPoint = stmt.sourceEnd + 1;
					this.isOrphanCompletionNode = false;
				} else if ((stmt instanceof ForeachStatement) && ((ForeachStatement) stmt).action == null) {
					element = element.add(stmt, 0);
					this.lastCheckPoint = stmt.sourceEnd + 1;
				} else if (stmt.containsPatternVariable()) {
					LocalDeclaration localDeclaration = null;
					if(stmt instanceof CaseStatement) {
						localDeclaration = ((CaseStatement)stmt).getLocalDeclaration();
						if(localDeclaration !=null)
							element.add(localDeclaration, 0);
					}
					element.add(stmt, 0);
					this.lastCheckPoint = stmt.sourceEnd + 1;
				}
			}
			continue;
		}
		if (node instanceof ImportReference){
			ImportReference importRef = (ImportReference) node;
			element = element.add(importRef, 0);
			this.lastCheckPoint = importRef.declarationSourceEnd + 1;
		}
	}
	if (this.currentToken == TokenNameRBRACE) {
		 if (isIndirectlyInsideLambdaExpression())
			 this.ignoreNextClosingBrace = true;
		 else
			 this.currentToken = 0; // closing brace has already been taken care of
	}

	/* might need some extra block (after the last reduced node) */
	/* For block bodied lambdas we should create a block even though the lambda header appears before it, so elements from within don't get misattributed. */
	int pos = this.assistNode == null ? this.lastCheckPoint : this.assistNode.sourceStart;
	boolean createLambdaBlock = lastNode instanceof LambdaExpression && ((LambdaExpression) node).body() instanceof Block;
	for (int j = blockIndex; j <= this.realBlockPtr; j++){
		if (this.blockStarts[j] >= 0) {
			if ((this.blockStarts[j] < pos || createLambdaBlock) && (this.blockStarts[j] != lastStart)){ // avoid multiple block if at same position
				block = new Block(0);
				block.sourceStart = lastStart = this.blockStarts[j];
				element = element.add(block, 1);
				createLambdaBlock = false;
			}
		} else {
			if ((this.blockStarts[j] < pos)){ // avoid multiple block if at same position
				block = new Block(0);
				block.sourceStart = lastStart = -this.blockStarts[j];
				element = element.add(block, 1);
			}
		}
	}

	return element;
}

private void initModuleInfo(RecoveredElement element) {
	if (element  instanceof RecoveredUnit) {
		RecoveredUnit unit = (RecoveredUnit) element;
		if (unit.unitDeclaration.isModuleInfo()) {
			ASTNode node = null;
			int i = 0;
			for (; i <= this.astPtr; i++) {
				if ((node = this.astStack[i]) instanceof ModuleDeclaration) {
					unit.add((ModuleDeclaration) node, this.bracketDepth);
					break;
				}
			}
		}
	}
}
@Override
protected void consumeAnnotationTypeDeclarationHeader() {
	super.consumeAnnotationTypeDeclarationHeader();
	pushOnElementStack(K_TYPE_DELIMITER);
}
@Override
protected void consumeClassBodyDeclaration() {
	popElement(K_METHOD_DELIMITER);
	super.consumeClassBodyDeclaration();
}
@Override
protected void consumeClassBodyopt() {
	super.consumeClassBodyopt();
	popElement(K_SELECTOR);
}
@Override
protected void consumeClassHeader() {
	super.consumeClassHeader();
	pushOnElementStack(K_TYPE_DELIMITER);
}
@Override
protected void consumeConstructorBody() {
	super.consumeConstructorBody();
	popElement(K_METHOD_DELIMITER);
}
@Override
protected void consumeConstructorHeader() {
	super.consumeConstructorHeader();
	pushOnElementStack(K_METHOD_DELIMITER);
}
@Override
protected void consumeEnhancedForStatementHeaderInit(boolean hasModifiers) {
	super.consumeEnhancedForStatementHeaderInit(hasModifiers);

	if (this.currentElement != null) {
		LocalDeclaration localDecl = ((ForeachStatement)this.astStack[this.astPtr]).elementVariable;
		this.lastCheckPoint = localDecl.sourceEnd + 1;
		this.currentElement = this.currentElement.add(localDecl, 0);
	}
}
@Override
protected void consumeEnterAnonymousClassBody(boolean qualified) {
	super.consumeEnterAnonymousClassBody(qualified);
	popElement(K_SELECTOR);
	pushOnElementStack(K_TYPE_DELIMITER);
}
@Override
protected void consumeEnterMemberValue() {
	super.consumeEnterMemberValue();
	pushOnElementStack(K_ATTRIBUTE_VALUE_DELIMITER, this.identifierPtr);
}
@Override
protected void consumeEnumConstantHeader() {
	if(this.currentToken == TokenNameLBRACE) {
		popElement(K_ENUM_CONSTANT_DELIMITER);
		pushOnElementStack(K_ENUM_CONSTANT_DELIMITER, WITH_BODY);
		pushOnElementStack(K_FIELD_INITIALIZER_DELIMITER);
		pushOnElementStack(K_TYPE_DELIMITER);
	}
	super.consumeEnumConstantHeader();
	if (triggerRecoveryUponLambdaClosure((Statement) this.astStack[this.astPtr], true) && this.currentElement != null)
		this.restartRecovery = true;
}
@Override
protected void consumeEnumConstantHeaderName() {
	super.consumeEnumConstantHeaderName();
	pushOnElementStack(K_ENUM_CONSTANT_DELIMITER);
}
@Override
protected void consumeEnumConstantWithClassBody() {
	popElement(K_TYPE_DELIMITER);
	popElement(K_FIELD_INITIALIZER_DELIMITER);
	popElement(K_ENUM_CONSTANT_DELIMITER);
	super.consumeEnumConstantWithClassBody();
}
@Override
protected void consumeEnumConstantNoClassBody() {
	popElement(K_ENUM_CONSTANT_DELIMITER);
	super.consumeEnumConstantNoClassBody();
}
@Override
protected void consumeEnumHeader() {
	super.consumeEnumHeader();
	pushOnElementStack(K_TYPE_DELIMITER);
}
@Override
protected void consumeExitMemberValue() {
	super.consumeExitMemberValue();
	popElement(K_ATTRIBUTE_VALUE_DELIMITER);
}

@Override
protected void consumeExplicitConstructorInvocation(int flag, int recFlag) {
	super.consumeExplicitConstructorInvocation(flag, recFlag);
	popElement(K_SELECTOR);
}

protected boolean triggerRecoveryUponLambdaClosure(Statement statement, boolean shouldCommit) {
	// Last block statement reduced is required to be on the AST stack top.
	boolean lambdaClosed = false;
	int statementStart, statementEnd;
	statementStart = statement.sourceStart;
	if (statement instanceof AbstractVariableDeclaration) {
		AbstractVariableDeclaration variable = (AbstractVariableDeclaration)statement;
		if (variable.initialization != null && variable.initialization.sourceEnd == 0) {
			statementEnd = Integer.MAX_VALUE; // variable end is not yet known, don't make it a limiting factor
		} else {
			statementEnd = variable.declarationSourceEnd;
		}
	} else {
		statementEnd = statement.sourceEnd;
	}
	for (int i = this.elementPtr; i >= 0; --i) {
		if (this.elementKindStack[i] != K_LAMBDA_EXPRESSION_DELIMITER)
			continue;
		LambdaExpression expression = (LambdaExpression) this.elementObjectInfoStack[i];

		if (expression == null) {
			return false;
		} else if (lambdaClosed == true) {
			// if we already have found the correct lambda and we still reach here, means we have another outer
			// lambda, so try to push current statement into the ASTStack by trying to simulate a block
			// statement processing.
			shouldCommit = true;
		}

		if (expression.sourceStart >= statementStart && expression.sourceEnd <= statementEnd) {
			this.elementPtr = i - 1;
			lambdaClosed = true;
		} else {
			if (shouldCommit) {
				int stackLength = this.stack.length;
				if (++this.stateStackTop >= stackLength) {
					System.arraycopy(
							this.stack, 0,
							this.stack = new int[stackLength + StackIncrement], 0,
							stackLength);
				}
				this.stack[this.stateStackTop] = this.unstackedAct;
				commit(false);
				this.stateStackTop --;
			}
			return false;
		}
	}

	if (lambdaClosed && this.currentElement != null && !(this.currentElement instanceof RecoveredField)) {
		if (!(statement instanceof AbstractVariableDeclaration)) { // added already as part of standard recovery since these contribute a name to the scope prevailing at the cursor.
			/* See if CompletionParser.attachOrphanCompletionNode has already added bits and pieces of AST to the recovery tree. If so, we want to
			   replace those fragments with the fuller statement that provides target type for the lambda that got closed just now. There is prior
			   art/precedent in the Java 7 world to this: Search for recoveredBlock.statements[--recoveredBlock.statementCount] = null;
			   See also that this concern does not arise in the case of field/local initialization since the initializer is replaced with full tree by consumeExitVariableWithInitialization.
			*/
			/*
			 * All the above comments will not work if the assist node is buried deeper. This happens when there the
			 * lambda was part of a complex statement, such as it was one of the arguments to a method invocation. In which case,
			 * we start from the topmost recovery element and look for assist nodes. If the operation is successful, the method
			 * replaceAssistStatement() returns null. Else, it returns the original statement, thus falling back to replacing the
			 * last statement in the current recovered element.
			 */
			statement = replaceAssistStatement(this.currentElement.topElement(),
					this.assistNodeParent(), statementStart, statementEnd, statement);

			if (statement != null) {
				RecoveredBlock recoveredBlock = (RecoveredBlock) (this.currentElement instanceof RecoveredBlock ? this.currentElement :
					(this.currentElement.parent instanceof RecoveredBlock) ? this.currentElement.parent :
						this.currentElement instanceof RecoveredMethod ? ((RecoveredMethod) this.currentElement).methodBody : null);
				if (recoveredBlock != null) {
					RecoveredStatement recoveredStatement = recoveredBlock.statementCount > 0 ? recoveredBlock.statements[recoveredBlock.statementCount - 1] : null;
					ASTNode parseTree = recoveredStatement != null ? recoveredStatement.updatedStatement(0, new HashSet<TypeDeclaration>()) : null;
					if (parseTree != null) {
						detectAssistNodeParent(parseTree);
						if ((parseTree.sourceStart == 0 || parseTree.sourceEnd == 0) || (parseTree.sourceStart >= statementStart && parseTree.sourceEnd <= statementEnd)) {
							recoveredBlock.statements[recoveredBlock.statementCount - 1] = new RecoveredStatement(statement, recoveredBlock, 0);
							statement = null;
						} else if (recoveredStatement instanceof RecoveredLocalVariable && statement instanceof Expression &&
								((Expression) statement).isTrulyExpression()) {
							RecoveredLocalVariable local = (RecoveredLocalVariable) recoveredStatement;
							if (local.localDeclaration != null && local.localDeclaration.initialization != null) {
								if ((local.localDeclaration.initialization.sourceStart == 0 || local.localDeclaration.initialization.sourceEnd == 0) ||
								        (local.localDeclaration.initialization.sourceStart >= statementStart && local.localDeclaration.initialization.sourceEnd <= statementEnd) ){
									local.localDeclaration.initialization = (Expression) statement;
									local.localDeclaration.declarationSourceEnd = statement.sourceEnd;
									local.localDeclaration.declarationEnd = statement.sourceEnd;
									statement = null;
								}
							}
						}
					}
				}
			}
			if (statement != null) {
				while (this.currentElement != null) {
					ASTNode tree = this.currentElement.parseTree();
					if (tree.sourceStart < statement.sourceStart) {
						this.currentElement.add(statement, 0);
						break;
					}
					this.currentElement = this.currentElement.parent;
				}
			}
		}
	}
	if (this.snapShotPtr > -1)
		popSnapShot();
	return lambdaClosed;
}

protected void detectAssistNodeParent(ASTNode parseTree) {
	// only for completion
}

public Statement replaceAssistStatement(RecoveredElement top, ASTNode assistParent, int start, int end, Statement stmt) {
	if (top == null) return null;
	if (top instanceof RecoveredBlock) {
		RecoveredBlock blk = (RecoveredBlock) top;
		RecoveredStatement[] statements = blk.statements;
		boolean found = false;
		if (statements != null) {
			for(int i = 0; i < statements.length; i++) {
				if (statements[i] == null) break;
				ASTNode node = statements[i].parseTree();
				if ((node.sourceStart >= start && node.sourceEnd <= end)) {
					if (!found) {
						statements[i] = new RecoveredStatement(stmt, blk, 0);
						found = true;
						blk.statementCount = i + 1;
					} else {
						// Proceed to wipe out the nodes after the assist node. Most likely
						// these are wrapped inside the just-closed-lambda. Even otherwise,
						// they are not much useful for completion.
						statements[i] = null;
					}
				}
			}
			if (found) return null;
		}
	} else if (top instanceof RecoveredMethod) {
		stmt = replaceAssistStatement(((RecoveredMethod) top).methodBody, assistParent, start, end, stmt);
	} else if (top instanceof RecoveredInitializer) {
		stmt = replaceAssistStatement(((RecoveredInitializer) top).initializerBody, assistParent, start, end, stmt);
	}
	return stmt;
}
protected ASTNode assistNodeParent() {
	return null;
}
@Override
protected boolean isAssistParser() {
	return true;
}
@Override
protected void consumeBlockStatement() {
	super.consumeBlockStatement();
	if (triggerRecoveryUponLambdaClosure((Statement) this.astStack[this.astPtr], true) && this.currentElement != null)
		this.restartRecovery = true;
}
@Override
protected void consumeBlockStatements() {
	super.consumeBlockStatements();
	if (triggerRecoveryUponLambdaClosure((Statement) this.astStack[this.astPtr], true) && this.currentElement != null) {
		this.restartRecovery = true;
	}
}
@Override
protected void consumeBlock() {
	super.consumeBlock();
	if (this.snapShotPtr > -1) {
		ASTNode top = this.astStack[this.astPtr];
		if (top instanceof Block) {
			// check positions for sanity:
			assert this.snapShotPositions[this.snapShotPtr] == top.sourceStart : "Block positions should be consistent"; //$NON-NLS-1$
			popSnapShot();
		}
	}
}
@Override
protected void consumeFieldDeclaration() {
	super.consumeFieldDeclaration();
	if (triggerRecoveryUponLambdaClosure((Statement) this.astStack[this.astPtr], true)) {
		if (this.currentElement instanceof RecoveredType)
			popUntilElement(K_TYPE_DELIMITER);
		if (this.currentElement != null)
			this.restartRecovery = true;
	}
}
@Override
protected void consumeForceNoDiet() {
	super.consumeForceNoDiet();
	// if we are not in a method (i.e. we are not in a local variable initializer)
	// then we are entering a field initializer
	if (!isInsideMethod()) {
		if(topKnownElementKind(ASSIST_PARSER) != K_ENUM_CONSTANT_DELIMITER) {
			if(topKnownElementKind(ASSIST_PARSER, 2) != K_ENUM_CONSTANT_DELIMITER) {
				pushOnElementStack(K_FIELD_INITIALIZER_DELIMITER);
			}
		} else {
			int info = topKnownElementInfo(ASSIST_PARSER);
			if(info != NO_BODY) {
				pushOnElementStack(K_FIELD_INITIALIZER_DELIMITER);
			}
		}

	}
}
@Override
protected void consumeInterfaceHeader() {
	super.consumeInterfaceHeader();
	pushOnElementStack(K_TYPE_DELIMITER);
}
@Override
protected void consumeNestedLambda() {
	super.consumeNestedLambda();
	LambdaExpression lexp = (LambdaExpression) this.astStack[this.astPtr];
	pushOnElementStack(K_LAMBDA_EXPRESSION_DELIMITER, EXPRESSION_BODY, lexp);
}
@Override
protected void consumeMethodBody() {
	super.consumeMethodBody();
	popElement(K_METHOD_DELIMITER);
}
@Override
protected void consumeMethodDeclaration(boolean isNotAbstract, boolean isDefaultMethod) {
	if (!isNotAbstract) {
		popElement(K_METHOD_DELIMITER);
	}
	super.consumeMethodDeclaration(isNotAbstract, isDefaultMethod);
	if (this.snapShotPtr > -1) {
		ASTNode top = this.astStack[this.astPtr];
		if (top instanceof AbstractMethodDeclaration) {
			// check positions for sanity:
			assert this.snapShotPositions[this.snapShotPtr] + 1 == ((AbstractMethodDeclaration) top).bodyStart : "Method positions should be consistent"; //$NON-NLS-1$
			popSnapShot();
		}
	}
}
@Override
protected void consumeMethodHeader() {
	super.consumeMethodHeader();
	pushOnElementStack(K_METHOD_DELIMITER);
}
@Override
protected void consumeMethodInvocationName() {
	super.consumeMethodInvocationName();
	popElement(K_SELECTOR);
	MessageSend messageSend = (MessageSend)this.expressionStack[this.expressionPtr];
	if (messageSend == this.assistNode){
		this.lastCheckPoint = messageSend.sourceEnd + 1;
	}
}
@Override
protected void consumeMethodInvocationNameWithTypeArguments() {
	super.consumeMethodInvocationNameWithTypeArguments();
	popElement(K_SELECTOR);
	MessageSend messageSend = (MessageSend)this.expressionStack[this.expressionPtr];
	if (messageSend == this.assistNode){
		this.lastCheckPoint = messageSend.sourceEnd + 1;
	}
}
@Override
protected void consumeMethodInvocationPrimary() {
	super.consumeMethodInvocationPrimary();
	popElement(K_SELECTOR);
	MessageSend messageSend = (MessageSend)this.expressionStack[this.expressionPtr];
	if (messageSend == this.assistNode){
		this.lastCheckPoint = messageSend.sourceEnd + 1;
	}
}
@Override
protected void consumeMethodInvocationPrimaryWithTypeArguments() {
	super.consumeMethodInvocationPrimaryWithTypeArguments();
	popElement(K_SELECTOR);
	MessageSend messageSend = (MessageSend)this.expressionStack[this.expressionPtr];
	if (messageSend == this.assistNode){
		this.lastCheckPoint = messageSend.sourceEnd + 1;
	}
}
@Override
protected void consumeMethodInvocationSuper() {
	super.consumeMethodInvocationSuper();
	popElement(K_SELECTOR);
	MessageSend messageSend = (MessageSend)this.expressionStack[this.expressionPtr];
	if (messageSend == this.assistNode){
		this.lastCheckPoint = messageSend.sourceEnd + 1;
	}
}
@Override
protected void consumeMethodInvocationSuperWithTypeArguments() {
	super.consumeMethodInvocationSuperWithTypeArguments();
	popElement(K_SELECTOR);
	MessageSend messageSend = (MessageSend)this.expressionStack[this.expressionPtr];
	if (messageSend == this.assistNode){
		this.lastCheckPoint = messageSend.sourceEnd + 1;
	}
}
@Override
protected void consumeModuleHeader() {
	pushOnElementStack(K_MODULE_INFO_DELIMITER);
	// ModuleHeader ::= 'module' Name
	/* build an ImportRef build from the last name
	stored in the identifier stack. */

	int index;

	/* no need to take action if not inside assist identifiers */
	if ((index = indexOfAssistIdentifier()) < 0) {
		super.consumeModuleHeader();
		return;
	}
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
	ModuleDeclaration typeDecl = createAssistModuleDeclaration(this.compilationUnit.compilationResult, subset, positions);

	this.compilationUnit.moduleDeclaration = typeDecl;
	this.assistNode = typeDecl;
	this.lastCheckPoint = typeDecl.sourceEnd + 1;

	//compute the declaration source too
	typeDecl.declarationSourceStart = this.intStack[this.intPtr--];

	typeDecl.bodyStart = typeDecl.sourceEnd + 1;
	pushOnAstStack(typeDecl);

	this.listLength = 0; // will be updated when reading super-interfaces
	// recovery
	if (this.currentElement != null){
		this.lastCheckPoint = typeDecl.bodyStart;
		this.currentElement = this.currentElement.add(typeDecl, 0);
		this.lastIgnoredToken = -1;
	}
}

@Override
protected void consumeModuleDeclaration() {
	super.consumeModuleDeclaration();
	popElement(K_MODULE_INFO_DELIMITER);
}
@Override
protected void consumeNestedMethod() {
	super.consumeNestedMethod();
	if(!isInsideMethod()) pushOnElementStack(K_METHOD_DELIMITER);
}
@Override
protected void consumeOpenBlock() {
	// OpenBlock ::= $empty
	super.consumeOpenBlock();

	int stackLength = this.blockStarts.length;
	if (this.realBlockPtr >= stackLength) {
		System.arraycopy(
				this.blockStarts, 0,
				this.blockStarts = new int[stackLength + StackIncrement], 0,
				stackLength);
	}
	this.blockStarts[this.realBlockPtr] = this.scanner.startPosition;
	if (requireExtendedRecovery()) {
		// This is an epsilon production: We are in the state with kernel item: Block ::= .OpenBlock LBRACE BlockStatementsopt RBRACE
		if (this.currentToken == TokenNameLBRACE && this.unstackedAct > NUM_RULES) { // wait for chain reductions to finish before commit.
			stackLength = this.stack.length;
			if (++this.stateStackTop >= stackLength - 1) {   // Need two slots.
				System.arraycopy(
						this.stack, 0,
						this.stack = new int[stackLength + StackIncrement], 0,
						stackLength);
			}
			this.stack[this.stateStackTop++] = this.unstackedAct; // transition to Block ::= OpenBlock  .LBRACE BlockStatementsopt RBRACE
			this.stack[this.stateStackTop] = tAction(this.unstackedAct, this.currentToken); // transition to Block ::= OpenBlock LBRACE  .BlockStatementsopt RBRACE
			commit(true);
			this.stateStackTop -= 2;
		}
	}
}
protected void consumeOpenFakeBlock() {
	// OpenBlock ::= $empty

	super.consumeOpenBlock();
	int stackLength = this.blockStarts.length;
	if (this.realBlockPtr >= stackLength) {
		System.arraycopy(
			this.blockStarts, 0,
			this.blockStarts = new int[stackLength + StackIncrement], 0,
			stackLength);
	}
	this.blockStarts[this.realBlockPtr] = -this.scanner.startPosition;
}
@Override
protected void consumePackageDeclarationName() {
	// PackageDeclarationName ::= 'package' Name
	/* build an ImportRef build from the last name
	stored in the identifier stack. */

	int index;

	/* no need to take action if not inside assist identifiers */
	if ((index = indexOfAssistIdentifier()) < 0) {
		super.consumePackageDeclarationName();
		return;
	}
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

	/* build specific assist node on package statement */
	ImportReference reference = createAssistPackageReference(subset, positions);
	this.assistNode = reference;
	this.lastCheckPoint = reference.sourceEnd + 1;
	this.compilationUnit.currentPackage = reference;

	if (this.currentToken == TokenNameSEMICOLON){
		reference.declarationSourceEnd = this.scanner.currentPosition - 1;
	} else {
		reference.declarationSourceEnd = (int) positions[length-1];
	}
	//endPosition is just before the ;
	reference.declarationSourceStart = this.intStack[this.intPtr--];
	// flush comments defined prior to import statements
	reference.declarationSourceEnd = flushCommentsDefinedPriorTo(reference.declarationSourceEnd);

	// recovery
	if (this.currentElement != null){
		this.lastCheckPoint = reference.declarationSourceEnd+1;
		this.restartRecovery = true; // used to avoid branching back into the regular automaton
	}
}
@Override
protected void consumePackageDeclarationNameWithModifiers() {
	// PackageDeclarationName ::= Modifiers 'package' PushRealModifiers Name
	/* build an ImportRef build from the last name
	stored in the identifier stack. */

	int index;

	/* no need to take action if not inside assist identifiers */
	if ((index = indexOfAssistIdentifier()) < 0) {
		super.consumePackageDeclarationNameWithModifiers();
		return;
	}
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

	this.intPtr--; // we don't need the modifiers start
	this.intPtr--; // we don't need the package modifiers
	ImportReference reference = createAssistPackageReference(subset, positions);
	// consume annotations
	if ((length = this.expressionLengthStack[this.expressionLengthPtr--]) != 0) {
		System.arraycopy(
			this.expressionStack,
			(this.expressionPtr -= length) + 1,
			reference.annotations = new Annotation[length],
			0,
			length);
	}
	/* build specific assist node on package statement */
	this.assistNode = reference;
	this.lastCheckPoint = reference.sourceEnd + 1;
	this.compilationUnit.currentPackage = reference;

	if (this.currentToken == TokenNameSEMICOLON){
		reference.declarationSourceEnd = this.scanner.currentPosition - 1;
	} else {
		reference.declarationSourceEnd = (int) positions[length-1];
	}
	//endPosition is just before the ;
	reference.declarationSourceStart = this.intStack[this.intPtr--];
	// flush comments defined prior to import statements
	reference.declarationSourceEnd = flushCommentsDefinedPriorTo(reference.declarationSourceEnd);

	// recovery
	if (this.currentElement != null){
		this.lastCheckPoint = reference.declarationSourceEnd+1;
		this.restartRecovery = true; // used to avoid branching back into the regular automaton
	}
}
@Override
protected void consumeRestoreDiet() {
	super.consumeRestoreDiet();
	// if we are not in a method (i.e. we were not in a local variable initializer)
	// then we are exiting a field initializer
	if (!isInsideMethod()) {
		popUntilElement(K_FIELD_INITIALIZER_DELIMITER);
		popElement(K_FIELD_INITIALIZER_DELIMITER);
	}
}
@Override
protected void consumeSingleStaticImportDeclarationName() {
	// SingleTypeImportDeclarationName ::= 'import' 'static' Name
	/* push an ImportRef build from the last name
	stored in the identifier stack. */

	int index;

	/* no need to take action if not inside assist identifiers */
	if ((index = indexOfAssistIdentifier()) < 0) {
		super.consumeSingleStaticImportDeclarationName();
		return;
	}
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

	/* build specific assist node on import statement */
	ImportReference reference = createAssistImportReference(subset, positions, ClassFileConstants.AccStatic);
	this.assistNode = reference;
	this.lastCheckPoint = reference.sourceEnd + 1;

	pushOnAstStack(reference);

	if (this.currentToken == TokenNameSEMICOLON){
		reference.declarationSourceEnd = this.scanner.currentPosition - 1;
	} else {
		reference.declarationSourceEnd = (int) positions[length-1];
	}
	//endPosition is just before the ;
	reference.declarationSourceStart = this.intStack[this.intPtr--];
	// flush annotations defined prior to import statements
	reference.declarationSourceEnd = flushCommentsDefinedPriorTo(reference.declarationSourceEnd);

	// recovery
	if (this.currentElement != null){
		this.lastCheckPoint = reference.declarationSourceEnd+1;
		this.currentElement = this.currentElement.add(reference, 0);
		this.lastIgnoredToken = -1;
		this.restartRecovery = true; // used to avoid branching back into the regular automaton
	}
}
@Override
protected void consumeSinglePkgName() {
	int index;
	/* no need to take action if not inside assist identifiers */
	if ((index = indexOfAssistIdentifier()) < 0) {
		super.consumeSinglePkgName();
		return;
	}
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

	/* build specific assist node on import statement */
	ImportReference reference = createAssistPackageVisibilityReference(subset, positions);
	this.assistNode = reference;
	this.lastCheckPoint = reference.sourceEnd + 1;

	pushOnAstStack(reference);

	if (this.currentToken == TokenNameSEMICOLON) {
		reference.declarationSourceEnd = this.scanner.currentPosition - 1;
	} else {
		reference.declarationSourceEnd = (int) positions[length-1];
	}
}
@Override
protected void consumeSingleTargetModuleName() {
	int index;
	/* no need to take action if not inside assist identifiers */
	if ((index = indexOfAssistIdentifier()) < 0) {
		super.consumeSingleTargetModuleName();
		return;
	}

	/* build specific assist node on targetted exports statement */
	ModuleReference reference = createAssistModuleReference(index);
	this.assistNode = reference;
	this.lastCheckPoint = reference.sourceEnd + 1;
	pushOnAstStack(reference);

	// recovery - TBD
	if (this.currentElement instanceof RecoveredExportsStatement){
		// TODO
		this.lastCheckPoint = reference.sourceEnd+1;
		this.currentElement = ((RecoveredExportsStatement) this.currentElement).add(reference, 0);
		this.lastIgnoredToken = -1;
		//this.restartRecovery = true; // used to avoid branching back into the regular automaton
	}

}
@Override
protected void consumeSingleRequiresModuleName() {

	int index = indexOfAssistIdentifier();
	/* no need to take action if not inside assist identifiers */
	if (index < 0) {
		super.consumeSingleRequiresModuleName();
		return;
	}

	/* build specific assist node on requires statement */
	ModuleReference reference = createAssistModuleReference(index);
	this.assistNode = reference;
	this.lastCheckPoint = reference.sourceEnd + 1;
	RequiresStatement req = new RequiresStatement(reference);
	if (this.currentToken == TokenNameSEMICOLON){
		req.declarationSourceEnd = this.scanner.currentPosition - 1;
	} else {
		req.declarationSourceEnd = reference.sourceEnd;
	}
	req.sourceStart = req.declarationSourceStart;
	req.declarationEnd = req.declarationSourceEnd;
	req.modifiersSourceStart = this.intStack[this.intPtr--];
	req.modifiers |= this.intStack[this.intPtr--];
	req.declarationSourceStart = this.intStack[this.intPtr--];
	if (req.modifiersSourceStart >= 0) {
		req.declarationSourceStart = req.modifiersSourceStart;
	}
	req.sourceEnd = reference.sourceEnd;
	pushOnAstStack(req);

	// recovery TBD

	if (this.currentElement != null){
		this.lastCheckPoint = req.declarationSourceEnd + 1;
		this.currentElement = this.currentElement.add(req, 0);
		this.lastIgnoredToken = -1;
	}

}

@Override
protected void consumeSingleTypeImportDeclarationName() {
	// SingleTypeImportDeclarationName ::= 'import' Name
	/* push an ImportRef build from the last name
	stored in the identifier stack. */

	int index;

	/* no need to take action if not inside assist identifiers */
	if ((index = indexOfAssistIdentifier()) < 0) {
		super.consumeSingleTypeImportDeclarationName();
		return;
	}
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

	/* build specific assist node on import statement */
	ImportReference reference = createAssistImportReference(subset, positions, ClassFileConstants.AccDefault);
	this.assistNode = reference;
	this.lastCheckPoint = reference.sourceEnd + 1;

	pushOnAstStack(reference);

	if (this.currentToken == TokenNameSEMICOLON){
		reference.declarationSourceEnd = this.scanner.currentPosition - 1;
	} else {
		reference.declarationSourceEnd = (int) positions[length-1];
	}
	//endPosition is just before the ;
	reference.declarationSourceStart = this.intStack[this.intPtr--];
	// flush comments defined prior to import statements
	reference.declarationSourceEnd = flushCommentsDefinedPriorTo(reference.declarationSourceEnd);

	// recovery
	if (this.currentElement != null){
		this.lastCheckPoint = reference.declarationSourceEnd+1;
		this.currentElement = this.currentElement.add(reference, 0);
		this.lastIgnoredToken = -1;
		this.restartRecovery = true; // used to avoid branching back into the regular automaton
	}
}
@Override
protected void consumeStaticImportOnDemandDeclarationName() {
	// TypeImportOnDemandDeclarationName ::= 'import' 'static' Name '.' '*'
	/* push an ImportRef build from the last name
	stored in the identifier stack. */

	int index;

	/* no need to take action if not inside assist identifiers */
	if ((index = indexOfAssistIdentifier()) < 0) {
		super.consumeStaticImportOnDemandDeclarationName();
		return;
	}
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

	/* build specific assist node on import statement */
	ImportReference reference = createAssistImportReference(subset, positions, ClassFileConstants.AccStatic);
	reference.bits |= ASTNode.OnDemand;
	// star end position
	reference.trailingStarPosition = this.intStack[this.intPtr--];
	this.assistNode = reference;
	this.lastCheckPoint = reference.sourceEnd + 1;

	pushOnAstStack(reference);

	if (this.currentToken == TokenNameSEMICOLON){
		reference.declarationSourceEnd = this.scanner.currentPosition - 1;
	} else {
		reference.declarationSourceEnd = (int) positions[length-1];
	}
	//endPosition is just before the ;
	reference.declarationSourceStart = this.intStack[this.intPtr--];
	// flush annotations defined prior to import statements
	reference.declarationSourceEnd = flushCommentsDefinedPriorTo(reference.declarationSourceEnd);

	// recovery
	if (this.currentElement != null){
		this.lastCheckPoint = reference.declarationSourceEnd+1;
		this.currentElement = this.currentElement.add(reference, 0);
		this.lastIgnoredToken = -1;
		this.restartRecovery = true; // used to avoid branching back into the regular automaton
	}
}
@Override
protected void consumeStaticInitializer() {
	super.consumeStaticInitializer();
	popElement(K_METHOD_DELIMITER);
}
@Override
protected void consumeStaticOnly() {
	super.consumeStaticOnly();
	pushOnElementStack(K_METHOD_DELIMITER);
}
private void adjustBracket(int token) {
	switch (token) {
		case TokenNameLPAREN :
		case TokenNameLBRACE:
		case TokenNameLBRACKET:
			this.bracketDepth++;
			break;
		case TokenNameRBRACE:
		case TokenNameRBRACKET:
		case TokenNameRPAREN:
			this.bracketDepth--;
			break;
	}
}
@Override
protected void consumeToken(int token) {
	super.consumeToken(token);

	if(this.isFirst) {
		this.isFirst = false;
		return;
	}
	// register message send selector only if inside a method or if looking at a field initializer
	// and if the current token is an open parenthesis
	if (isInsideMethod() || isInsideFieldInitialization() || isInsideAttributeValue() || isInsideEnumConstantnitialization()) {
		adjustBracket(token);
		switch (token) {
			case TokenNameLPAREN :
				switch (this.previousToken) {
					case TokenNameIdentifier:
						this.pushOnElementStack(K_SELECTOR, this.identifierPtr);
						break;
					case TokenNamethis: // explicit constructor invocation, e.g. this(1, 2)
						this.pushOnElementStack(K_SELECTOR, THIS_CONSTRUCTOR);
						break;
					case TokenNamesuper: // explicit constructor invocation, e.g. super(1, 2)
						this.pushOnElementStack(K_SELECTOR, SUPER_CONSTRUCTOR);
						break;
					case TokenNameGREATER: // explicit constructor invocation, e.g. Fred<X>[(]1, 2)
					case TokenNameRIGHT_SHIFT: // or fred<X<X>>[(]1, 2)
					case TokenNameUNSIGNED_RIGHT_SHIFT: //or Fred<X<X<X>>>[(]1, 2)
						if(this.identifierPtr > -1) {
							this.pushOnElementStack(K_SELECTOR, this.identifierPtr);
						}
						break;
				}
				break;
			case TokenNameLBRACE:
				if (this.previousToken == TokenNameARROW) {
					popElement(K_LAMBDA_EXPRESSION_DELIMITER);
					if (topKnownElementKind(ASSIST_PARSER, 1) != K_SWITCH_EXPRESSION_DELIMITTER)
						pushOnElementStack(K_LAMBDA_EXPRESSION_DELIMITER, BLOCK_BODY, this.previousObjectInfo);
				}
				break;
		}
	} else if (isInsideModuleInfo()) {
		adjustBracket(token);
	} else {
		switch (token) {
			case TokenNameRBRACE :
				if(topKnownElementKind(ASSIST_PARSER) == K_TYPE_DELIMITER) {
					popElement(K_TYPE_DELIMITER);
				}
				break;
		}
	}
	this.previousToken = token;
	if (token == TokenNameIdentifier) {
		this.previousIdentifierPtr = this.identifierPtr;
	}
}
@Override
protected void consumeTypeImportOnDemandDeclarationName() {
	// TypeImportOnDemandDeclarationName ::= 'import' Name '.' '*'
	/* push an ImportRef build from the last name
	stored in the identifier stack. */

	int index;

	/* no need to take action if not inside assist identifiers */
	if ((index = indexOfAssistIdentifier()) < 0) {
		super.consumeTypeImportOnDemandDeclarationName();
		return;
	}
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

	/* build specific assist node on import statement */
	ImportReference reference = createAssistImportReference(subset, positions, ClassFileConstants.AccDefault);
	reference.bits |= ASTNode.OnDemand;
	// star end position
	reference.trailingStarPosition = this.intStack[this.intPtr--];
	this.assistNode = reference;
	this.lastCheckPoint = reference.sourceEnd + 1;

	pushOnAstStack(reference);

	if (this.currentToken == TokenNameSEMICOLON){
		reference.declarationSourceEnd = this.scanner.currentPosition - 1;
	} else {
		reference.declarationSourceEnd = (int) positions[length-1];
	}
	//endPosition is just before the ;
	reference.declarationSourceStart = this.intStack[this.intPtr--];
	// flush comments defined prior to import statements
	reference.declarationSourceEnd = flushCommentsDefinedPriorTo(reference.declarationSourceEnd);

	// recovery
	if (this.currentElement != null){
		this.lastCheckPoint = reference.declarationSourceEnd+1;
		this.currentElement = this.currentElement.add(reference, 0);
		this.lastIgnoredToken = -1;
		this.restartRecovery = true; // used to avoid branching back into the regular automaton
	}
}

// TODO : Change to ExportsReference/PackageReference once we have the new compiler ast.node
public abstract ImportReference createAssistPackageVisibilityReference(char[][] tokens, long[] positions);
public abstract ImportReference createAssistImportReference(char[][] tokens, long[] positions, int mod);
public abstract ModuleReference createAssistModuleReference(int index);
public abstract ImportReference createAssistPackageReference(char[][] tokens, long[] positions);
public abstract NameReference createQualifiedAssistNameReference(char[][] previousIdentifiers, char[] assistName, long[] positions);
public abstract TypeReference createQualifiedAssistTypeReference(char[][] previousIdentifiers, char[] assistName, long[] positions);
public abstract TypeReference createParameterizedQualifiedAssistTypeReference(char[][] previousIdentifiers, TypeReference[][] typeArguments, char[] asistIdentifier, TypeReference[] assistTypeArguments, long[] positions);
public abstract NameReference createSingleAssistNameReference(char[] assistName, long position);
public abstract TypeReference createSingleAssistTypeReference(char[] assistName, long position);
public abstract TypeReference createParameterizedSingleAssistTypeReference(TypeReference[] typeArguments, char[] assistName, long position);
public abstract ModuleDeclaration createAssistModuleDeclaration(CompilationResult compilationResult, char[][] tokens, long[] positions);
/*
 * Flush parser/scanner state regarding to code assist
 */
public void flushAssistState(){
	this.assistNode = null;
	this.isOrphanCompletionNode = false;
	setAssistIdentifier(null);
}
protected void flushElementStack() {
	for (int j = 0; j <= this.elementPtr; j++) {
		this.elementObjectInfoStack[j] = null;
	}

	this.elementPtr = -1;
	this.previousKind = 0;
	this.previousInfo = 0;
	this.previousObjectInfo = null;
}
/*
 * Build specific type reference nodes in case the cursor is located inside the type reference
 */
@Override
protected TypeReference getTypeReference(int dim) {

	int index;

	/* no need to take action if not inside completed identifiers */
	if ((index = indexOfAssistIdentifier(true)) < 0) {
		return super.getTypeReference(dim);
	}
	int length = this.identifierLengthStack[this.identifierLengthPtr];
	TypeReference reference;
	int numberOfIdentifiers = this.genericsIdentifiersLengthStack[this.genericsIdentifiersLengthPtr--];
	if (length != numberOfIdentifiers || this.genericsLengthStack[this.genericsLengthPtr] != 0) {
		this.identifierLengthPtr--;
		// generic type
		reference = getAssistTypeReferenceForGenericType(dim, length, numberOfIdentifiers);
	} else {
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
//			genericsIdentifiersLengthPtr--;
			this.genericsLengthPtr--;
			/* assist inside first identifier */
			reference = createSingleAssistTypeReference(
							assistIdentifier(),
							positions[0]);
		} else {
//			genericsIdentifiersLengthPtr--;
			this.genericsLengthPtr--;
			/* assist inside subsequent identifier */
			reference =	createQualifiedAssistTypeReference(
							subset,
							assistIdentifier(),
							positions);
		}
		this.assistNode = reference;
		this.lastCheckPoint = reference.sourceEnd + 1;
	}
	return reference;
}
protected TypeReference getAssistTypeReferenceForGenericType(int dim, int identifierLength, int numberOfIdentifiers) {
	/* no need to take action if not inside completed identifiers */
	if (/*(indexOfAssistIdentifier()) < 0 ||*/ (identifierLength == 1 && numberOfIdentifiers == 1)) {
		int currentTypeArgumentsLength = this.genericsLengthStack[this.genericsLengthPtr--];
		TypeReference[] typeArguments;
		if (currentTypeArgumentsLength > -1) {
			typeArguments = new TypeReference[currentTypeArgumentsLength];
			this.genericsPtr -= currentTypeArgumentsLength;
			System.arraycopy(this.genericsStack, this.genericsPtr + 1, typeArguments, 0, currentTypeArgumentsLength);
		} else {
			typeArguments = TypeReference.NO_TYPE_ARGUMENTS;
		}
		long[] positions = new long[identifierLength];
		System.arraycopy(
			this.identifierPositionStack,
			this.identifierPtr,
			positions,
			0,
			identifierLength);

		this.identifierPtr--;

		TypeReference reference = createParameterizedSingleAssistTypeReference(
				typeArguments,
				assistIdentifier(),
				positions[0]);

		this.assistNode = reference;
		this.lastCheckPoint = reference.sourceEnd + 1;
		return reference;
	}

	TypeReference[][] typeArguments = new TypeReference[numberOfIdentifiers][];
	char[][] tokens = new char[numberOfIdentifiers][];
	long[] positions = new long[numberOfIdentifiers];
	int index = numberOfIdentifiers;
	int currentIdentifiersLength = identifierLength;
	while (index > 0) {
		int currentTypeArgumentsLength = this.genericsLengthStack[this.genericsLengthPtr--];
		if (currentTypeArgumentsLength > 0) {
			this.genericsPtr -= currentTypeArgumentsLength;
			System.arraycopy(this.genericsStack, this.genericsPtr + 1, typeArguments[index - 1] = new TypeReference[currentTypeArgumentsLength], 0, currentTypeArgumentsLength);
		}
		switch(currentIdentifiersLength) {
			case 1 :
				// we are in a case A<B>.C<D> or A<B>.C<D>
				tokens[index - 1] = this.identifierStack[this.identifierPtr];
				positions[index - 1] = this.identifierPositionStack[this.identifierPtr--];
				break;
			default:
				// we are in a case A.B.C<B>.C<D> or A.B.C<B>...
				this.identifierPtr -= currentIdentifiersLength;
				System.arraycopy(this.identifierStack, this.identifierPtr + 1, tokens, index - currentIdentifiersLength, currentIdentifiersLength);
				System.arraycopy(this.identifierPositionStack, this.identifierPtr + 1, positions, index - currentIdentifiersLength, currentIdentifiersLength);
		}
		index -= currentIdentifiersLength;
		if (index > 0) {
			currentIdentifiersLength = this.identifierLengthStack[this.identifierLengthPtr--];
		}
	}

	// remove completion token
	int realLength = numberOfIdentifiers;
	for (int i = 0; i < numberOfIdentifiers; i++) {
		if(tokens[i] == assistIdentifier()) {
			realLength = i;
		}
	}
	TypeReference reference;
	if(realLength == 0) {
		if(typeArguments[0] != null && typeArguments[0].length > 0) {
			reference = createParameterizedSingleAssistTypeReference(typeArguments[0], assistIdentifier(), positions[0]);
		} else {
			reference = createSingleAssistTypeReference(assistIdentifier(), positions[0]);
		}
	} else {
		TypeReference[] assistTypeArguments = typeArguments[realLength];
		System.arraycopy(tokens, 0, tokens = new char[realLength][], 0, realLength);
		System.arraycopy(typeArguments, 0, typeArguments = new TypeReference[realLength][], 0, realLength);

		boolean isParameterized = false;
		for (int i = 0; i < typeArguments.length; i++) {
			if(typeArguments[i] != null) {
				isParameterized = true;
				break;
			}
		}
		if(isParameterized || (assistTypeArguments != null && assistTypeArguments.length > 0)) {
			reference = createParameterizedQualifiedAssistTypeReference(tokens, typeArguments, assistIdentifier(), assistTypeArguments, positions);
		} else {
			reference = createQualifiedAssistTypeReference(tokens, assistIdentifier(), positions);
		}
	}

	this.assistNode = reference;
	this.lastCheckPoint = reference.sourceEnd + 1;
	return reference;
}
/*
 * Copy of code from superclass with the following change:
 * In the case of qualified name reference if the cursor location is on the
 * qualified name reference, then create a CompletionOnQualifiedNameReference
 * instead.
 */
@Override
protected NameReference getUnspecifiedReferenceOptimized() {

	int completionIndex;

	/* no need to take action if not inside completed identifiers */
	if ((completionIndex = indexOfAssistIdentifier()) < 0) {
		return super.getUnspecifiedReferenceOptimized();
	}

	consumeNonTypeUseName();

	/* retrieve identifiers subset and whole positions, the completion node positions
		should include the entire replaced source. */
	int length = this.identifierLengthStack[this.identifierLengthPtr];
	char[][] subset = identifierSubSet(completionIndex);
	this.identifierLengthPtr--;
	this.identifierPtr -= length;
	long[] positions = new long[length];
	System.arraycopy(
		this.identifierPositionStack,
		this.identifierPtr + 1,
		positions,
		0,
		length);

	/* build specific completion on name reference */
	NameReference reference;
	if (completionIndex == 0) {
		/* completion inside first identifier */
		reference = createSingleAssistNameReference(assistIdentifier(), positions[0]);
	} else {
		/* completion inside subsequent identifier */
		reference = createQualifiedAssistNameReference(subset, assistIdentifier(), positions);
	}
	reference.bits &= ~ASTNode.RestrictiveFlagMASK;
	reference.bits |= Binding.LOCAL | Binding.FIELD;

	this.assistNode = reference;
	this.lastCheckPoint = reference.sourceEnd + 1;
	return reference;
}
@Override
public void goForBlockStatementsopt() {
	super.goForBlockStatementsopt();
	this.isFirst = true;
}
@Override
public void goForHeaders(){
	super.goForHeaders();
	this.isFirst = true;
}
@Override
public void goForCompilationUnit(){
	super.goForCompilationUnit();
	this.isFirst = true;
}
@Override
public void goForBlockStatementsOrCatchHeader() {
	super.goForBlockStatementsOrCatchHeader();
	this.isFirst = true;
}
/*
 * Retrieve a partial subset of a qualified name reference up to the completion point.
 * It does not pop the actual awaiting identifiers, so as to be able to retrieve position
 * information afterwards.
 */
protected char[][] identifierSubSet(int subsetLength){

	if (subsetLength == 0) return null;

	char[][] subset;
	System.arraycopy(
		this.identifierStack,
		this.identifierPtr - this.identifierLengthStack[this.identifierLengthPtr] + 1,
		(subset = new char[subsetLength][]),
		0,
		subsetLength);
	return subset;
}

protected int indexOfAssistIdentifier(){
	return this.indexOfAssistIdentifier(false);
}
/*
 * Iterate the most recent group of awaiting identifiers (grouped for qualified name reference (e.g. aa.bb.cc)
 * so as to check whether one of them is the assist identifier.
 * If so, then answer the index of the assist identifier (0 being the first identifier of the set).
 *	e.g. aa(0).bb(1).cc(2)
 * If no assist identifier was found, answers -1.
 */
protected int indexOfAssistIdentifier(boolean useGenericsStack){

	if (this.identifierLengthPtr < 0){
		return -1; // no awaiting identifier
	}

	char[] assistIdentifier ;
	if ((assistIdentifier = assistIdentifier()) == null){
		return -1; // no assist identifier found yet
	}

	// iterate awaiting identifiers backwards
	int length = this.identifierLengthStack[this.identifierLengthPtr];
	if(useGenericsStack && length > 0 && this.genericsIdentifiersLengthPtr > -1 ) {
		length = this.genericsIdentifiersLengthStack[this.genericsIdentifiersLengthPtr];
	}
	for (int i = 0; i < length; i++){
		if (this.identifierStack[this.identifierPtr - i] == assistIdentifier){
			return length - i - 1;
		}
	}
	// none of the awaiting identifiers is the completion one
	return -1;
}
@Override
public void initialize() {
	super.initialize();
	flushAssistState();
	flushElementStack();
	this.previousIdentifierPtr = -1;
	this.bracketDepth = 0;
}
@Override
public void initialize(boolean parsingCompilationUnit) {
	super.initialize(parsingCompilationUnit);
	flushAssistState();
	flushElementStack();
	this.previousIdentifierPtr = -1;
	this.bracketDepth = 0;
}
@Override
public abstract void initializeScanner();
protected boolean isIndirectlyInsideFieldInitialization(){
	int i = this.elementPtr;
	while(i > -1) {
		if(this.elementKindStack[i] == K_FIELD_INITIALIZER_DELIMITER)
			return true;
		i--;
	}
	return false;
}
protected boolean isIndirectlyInsideEnumConstantnitialization(){
	int i = this.elementPtr;
	while(i > -1) {
		if(this.elementKindStack[i] == K_ENUM_CONSTANT_DELIMITER)
			return true;
		i--;
	}
	return false;
}
protected boolean isIndirectlyInsideMethod(){
	int i = this.elementPtr;
	while(i > -1) {
		if(this.elementKindStack[i] == K_METHOD_DELIMITER)
			return true;
		i--;
	}
	return false;
}
@Override
protected boolean isIndirectlyInsideLambdaExpression(){
	int i = this.elementPtr;
	while (i > -1) {
		if (this.elementKindStack[i] == K_LAMBDA_EXPRESSION_DELIMITER)
			return true;
		i--;
	}
	return false;
}
protected boolean isIndirectlyInsideLambdaBlock(){
	int i = this.elementPtr;
	while (i > -1) {
		if (this.elementKindStack[i] == K_LAMBDA_EXPRESSION_DELIMITER && this.elementInfoStack[i] == BLOCK_BODY)
			return true;
		i--;
	}
	return false;
}
protected boolean isIndirectlyInsideType(){
	int i = this.elementPtr;
	while(i > -1) {
		if(this.elementKindStack[i] == K_TYPE_DELIMITER)
			return true;
		i--;
	}
	return false;
}
protected boolean isInsideAttributeValue(){
	int i = this.elementPtr;
	while(i > -1) {
		switch (this.elementKindStack[i]) {
			case K_TYPE_DELIMITER : return false;
			case K_METHOD_DELIMITER : return false;
			case K_FIELD_INITIALIZER_DELIMITER : return false;
			case K_ATTRIBUTE_VALUE_DELIMITER : return true;
		}
		i--;
	}
	return false;
}
protected boolean isInsideFieldInitialization(){
	int i = this.elementPtr;
	while(i > -1) {
		switch (this.elementKindStack[i]) {
			case K_TYPE_DELIMITER : return false;
			case K_METHOD_DELIMITER : return false;
			case K_FIELD_INITIALIZER_DELIMITER :
				return true;
		}
		i--;
	}
	return false;
}
protected boolean isInsideEnumConstantnitialization(){
	int i = this.elementPtr;
	while(i > -1) {
		switch (this.elementKindStack[i]) {
			case K_TYPE_DELIMITER : return false;
			case K_METHOD_DELIMITER : return false;
			case K_ENUM_CONSTANT_DELIMITER :
				return true;
		}
		i--;
	}
	return false;
}
protected boolean isInsideModuleInfo(){
	int i = this.elementPtr;
	while(i > -1) {
		switch (this.elementKindStack[i]) {
			case K_TYPE_DELIMITER :
			case K_METHOD_DELIMITER :
			case K_FIELD_INITIALIZER_DELIMITER :
				return false;
			case K_MODULE_INFO_DELIMITER:
				return true;
		}
		i--;
	}
	return false;
}
protected boolean isInsideMethod(){
	int i = this.elementPtr;
	while(i > -1) {
		switch (this.elementKindStack[i]) {
			case K_TYPE_DELIMITER : return false;
			case K_METHOD_DELIMITER : return true;
			case K_FIELD_INITIALIZER_DELIMITER : return false;
		}
		i--;
	}
	return false;
}
protected boolean isInsideType(){
	int i = this.elementPtr;
	while(i > -1) {
		switch (this.elementKindStack[i]) {
			case K_TYPE_DELIMITER : return true;
			case K_METHOD_DELIMITER : return false;
			case K_FIELD_INITIALIZER_DELIMITER : return false;
		}
		i--;
	}
	return false;
}
protected int lastIndexOfElement(int kind) {
	int i = this.elementPtr;
	while(i > -1) {
		if(this.elementKindStack[i] == kind) return i;
		i--;
	}
	return -1;
}
/**
 * Parse the block statements inside the given method declaration and try to complete at the
 * cursor location.
 */
public void parseBlockStatements(AbstractMethodDeclaration md, CompilationUnitDeclaration unit) {
	if (md instanceof MethodDeclaration) {
		parseBlockStatements((MethodDeclaration) md, unit);
	} else if (md instanceof ConstructorDeclaration) {
		parseBlockStatements((ConstructorDeclaration) md, unit);
	}
}
/**
 * Parse the block statements inside the given constructor declaration and try to complete at the
 * cursor location.
 */
public void parseBlockStatements(ConstructorDeclaration cd, CompilationUnitDeclaration unit) {
	//only parse the method body of cd
	//fill out its statements

	//convert bugs into parse error

	initialize();
	// set the lastModifiers to reflect the modifiers of the constructor whose
	// block statements are being parsed
	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=202634
	this.lastModifiers = cd.modifiers;
	this.lastModifiersStart = cd.modifiersSourceStart;
	// simulate goForConstructorBody except that we don't want to balance brackets because they are not going to be balanced
	goForBlockStatementsopt();

	this.referenceContext = cd;
	this.compilationUnit = unit;

	this.scanner.resetTo(cd.bodyStart, bodyEnd(cd));
	consumeNestedMethod();
	try {
		parse();
	} catch (AbortCompilation ex) {
		this.lastAct = ERROR_ACTION;
	}

	if (this.lastAct == ERROR_ACTION) {
		cd.bits |= ASTNode.HasSyntaxErrors;
		return;
	}

	// attach the statements as we might be searching for a reference to a local type
	cd.explicitDeclarations = this.realBlockStack[this.realBlockPtr--];
	int length;
	if ((length = this.astLengthStack[this.astLengthPtr--]) != 0) {
		this.astPtr -= length;
		if (this.astStack[this.astPtr + 1] instanceof ExplicitConstructorCall)
			//avoid a isSomeThing that would only be used here BUT what is faster between two alternatives ?
			{
			System.arraycopy(
				this.astStack,
				this.astPtr + 2,
				cd.statements = new Statement[length - 1],
				0,
				length - 1);
			cd.constructorCall = (ExplicitConstructorCall) this.astStack[this.astPtr + 1];
		} else { //need to add explicitly the super();
			System.arraycopy(
				this.astStack,
				this.astPtr + 1,
				cd.statements = new Statement[length],
				0,
				length);
			cd.constructorCall = SuperReference.implicitSuperConstructorCall();
		}
	} else {
		cd.constructorCall = SuperReference.implicitSuperConstructorCall();
		if (!containsComment(cd.bodyStart, cd.bodyEnd)) {
			cd.bits |= ASTNode.UndocumentedEmptyBlock;
		}
	}

	if (cd.constructorCall.sourceEnd == 0) {
		cd.constructorCall.sourceEnd = cd.sourceEnd;
		cd.constructorCall.sourceStart = cd.sourceStart;
	}
}
/**
 * Parse the block statements inside the given initializer and try to complete at the
 * cursor location.
 */
public void parseBlockStatements(
	Initializer initializer,
	TypeDeclaration type,
	CompilationUnitDeclaration unit) {

	initialize();
	// set the lastModifiers to reflect the modifiers of the initializer whose
	// block statements are being parsed
	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=202634
	this.lastModifiers = initializer.modifiers;
	this.lastModifiersStart = initializer.modifiersSourceStart;
	// simulate goForInitializer except that we don't want to balance brackets because they are not going to be balanced
	goForBlockStatementsopt();

	this.referenceContext = type;
	this.compilationUnit = unit;

	this.scanner.resetTo(initializer.sourceStart, bodyEnd(initializer)); // just after the beginning {
	consumeNestedMethod();
	try {
		parse();
	} catch (AbortCompilation ex) {
		this.lastAct = ERROR_ACTION;
	} finally {
		this.nestedMethod[this.nestedType]--;
	}

	if (this.lastAct == ERROR_ACTION) {
		initializer.bits |= ASTNode.HasSyntaxErrors;
		return;
	}

	// attach the statements as we might be searching for a reference to a local type
	initializer.block.explicitDeclarations = this.realBlockStack[this.realBlockPtr--];
	int length;
	if ((length = this.astLengthStack[this.astLengthPtr--]) > 0) {
		System.arraycopy(this.astStack, (this.astPtr -= length) + 1, initializer.block.statements = new Statement[length], 0, length);
	} else {
		// check whether this block at least contains some comment in it
		if (!containsComment(initializer.block.sourceStart, initializer.block.sourceEnd)) {
			initializer.block.bits |= ASTNode.UndocumentedEmptyBlock;
		}
	}

	// mark initializer with local type if one was found during parsing
	if ((type.bits & ASTNode.HasLocalType) != 0) {
		initializer.bits |= ASTNode.HasLocalType;
	}
}

/**
 * Parse the block statements inside the given method declaration and try to complete at the
 * cursor location.
 */
public void parseBlockStatements(MethodDeclaration md, CompilationUnitDeclaration unit) {
	//only parse the method body of md
	//fill out method statements

	//convert bugs into parse error

	if (md.isNative())
		return;
	if ((md.modifiers & ExtraCompilerModifiers.AccSemicolonBody) != 0)
		return;

	initialize();
	// set the lastModifiers to reflect the modifiers of the method whose
	// block statements are being parsed
	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=202634
	this.lastModifiers = md.modifiers;
	this.lastModifiersStart = md.modifiersSourceStart;
	// simulate goForMethodBody except that we don't want to balance brackets because they are not going to be balanced
	goForBlockStatementsopt();

	this.referenceContext = md;
	this.compilationUnit = unit;

	this.scanner.resetTo(md.bodyStart, bodyEnd(md)); // reset the scanner to parser from { down to the cursor location
	consumeNestedMethod();
	try {
		parse();
	} catch (AbortCompilation ex) {
		this.lastAct = ERROR_ACTION;
	} finally {
		this.nestedMethod[this.nestedType]--;
	}

	if (this.lastAct == ERROR_ACTION) {
		md.bits |= ASTNode.HasSyntaxErrors;
		return;
	}

	// attach the statements as we might be searching for a reference to a local type
	md.explicitDeclarations = this.realBlockStack[this.realBlockPtr--];
	int length;
	if ((length = this.astLengthStack[this.astLengthPtr--]) != 0) {
		System.arraycopy(
			this.astStack,
			(this.astPtr -= length) + 1,
			md.statements = new Statement[length],
			0,
			length);
	} else {
		if (!containsComment(md.bodyStart, md.bodyEnd)) {
			md.bits |= ASTNode.UndocumentedEmptyBlock;
		}
	}

}

// the name is a misnomer, we allow "pop"s not just at the TOS. Lambda wants to be sticky till fully reduced, however we do want other elements popped at the right point, so ...
protected void popElement(int kind) {

	if (this.elementPtr < 0)
		return;

	int stackPointer = this.elementPtr;

	if (kind != K_LAMBDA_EXPRESSION_DELIMITER) {
		while (this.elementKindStack[stackPointer] == K_LAMBDA_EXPRESSION_DELIMITER) {
			if (--stackPointer < 0) break;
		}
	}
	if (stackPointer < 0 || this.elementKindStack[stackPointer] != kind)
		return;

	this.previousKind = this.elementKindStack[stackPointer];
	this.previousInfo = this.elementInfoStack[stackPointer];
	this.previousObjectInfo = this.elementObjectInfoStack[stackPointer];

	final int length = this.elementPtr - stackPointer;
	if (length > 0) {
		System.arraycopy(this.elementKindStack, stackPointer + 1, this.elementKindStack, stackPointer, length);
		System.arraycopy(this.elementInfoStack, stackPointer + 1, this.elementInfoStack, stackPointer, length);
		System.arraycopy(this.elementObjectInfoStack, stackPointer + 1, this.elementObjectInfoStack, stackPointer, length);
	}
	this.elementObjectInfoStack[this.elementPtr] = null;
	this.elementPtr--;
 }
protected void popUntilElement(int kind){
	if(this.elementPtr < 0) return;
	int i = this.elementPtr;
	while (i >= 0 && this.elementKindStack[i] != kind) {
		i--;
	}
	if(i >= 0) {
		if(i < this.elementPtr) {
			this.previousKind = this.elementKindStack[i+1];
			this.previousInfo = this.elementInfoStack[i+1];
			this.previousObjectInfo = this.elementObjectInfoStack[i+1];

			for (int j = i + 1; j <= this.elementPtr; j++) {
				this.elementObjectInfoStack[j] = null;
			}
		}
		this.elementPtr = i;
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

	// initialize element stack
	int fieldInitializerIndex = lastIndexOfElement(K_FIELD_INITIALIZER_DELIMITER);
	int methodIndex = lastIndexOfElement(K_METHOD_DELIMITER);
	if(methodIndex == fieldInitializerIndex) {
		// there is no method and no field initializer
		flushElementStack();
	} else if(methodIndex > fieldInitializerIndex) {
		popUntilElement(K_METHOD_DELIMITER);
	} else {
		popUntilElement(K_FIELD_INITIALIZER_DELIMITER);
	}
}
/*
 * Prepares the state of the parser to go for Headers.
 */
protected void prepareForHeaders() {
	this.nestedMethod[this.nestedType = 0] = 0;
	this.variablesCounter[this.nestedType] = 0;
	this.realBlockStack[this.realBlockPtr = 0] = 0;

	popUntilElement(K_TYPE_DELIMITER);

	if(this.topKnownElementKind(ASSIST_PARSER) != K_TYPE_DELIMITER) {
		// is outside a type and inside a compilation unit.
		// remove all elements.
		flushElementStack();
	}
}

public boolean requireExtendedRecovery() {
	return lastIndexOfElement(K_LAMBDA_EXPRESSION_DELIMITER) >= 0;
}

protected void pushOnElementStack(int kind){
	this.pushOnElementStack(kind, 0, null);
}
protected void pushOnElementStack(int kind, int info){
	this.pushOnElementStack(kind, info, null);
}
protected void pushOnElementStack(int kind, int info, Object objectInfo){
	if (this.elementPtr < -1) return;

	this.previousKind = 0;
	this.previousInfo = 0;
	this.previousObjectInfo = null;

	int stackLength = this.elementKindStack.length;
	if (++this.elementPtr >= stackLength) {
		System.arraycopy(
			this.elementKindStack, 0,
			this.elementKindStack = new int[stackLength + StackIncrement], 0,
			stackLength);
		System.arraycopy(
			this.elementInfoStack, 0,
			this.elementInfoStack = new int[stackLength + StackIncrement], 0,
			stackLength);
		System.arraycopy(
			this.elementObjectInfoStack, 0,
			this.elementObjectInfoStack = new Object[stackLength + StackIncrement], 0,
			stackLength);
	}
	this.elementKindStack[this.elementPtr] = kind;
	this.elementInfoStack[this.elementPtr] = info;
	this.elementObjectInfoStack[this.elementPtr] = objectInfo;
}
@Override
public void recoveryExitFromVariable() {
	if(this.currentElement != null && this.currentElement instanceof RecoveredField
		&& !(this.currentElement instanceof RecoveredInitializer)) {
		RecoveredElement oldElement = this.currentElement;
		super.recoveryExitFromVariable();
		if(oldElement != this.currentElement) {
			popElement(K_FIELD_INITIALIZER_DELIMITER);
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
			super.recoveryTokenCheck();
			if(this.currentElement instanceof RecoveredInitializer) {
				if(oldElement instanceof RecoveredField) {
					popUntilElement(K_FIELD_INITIALIZER_DELIMITER);
					popElement(K_FIELD_INITIALIZER_DELIMITER);
				}
				if(this.currentElement != oldElement
					&& topKnownElementKind(ASSIST_PARSER) != K_METHOD_DELIMITER
					&& topKnownElementKind(ASSIST_PARSER) != K_LAMBDA_EXPRESSION_DELIMITER) {
					pushOnElementStack(K_METHOD_DELIMITER);
				}
			}
			break;
		case TokenNameRBRACE :
			super.recoveryTokenCheck();
			if(this.currentElement != oldElement && !isInsideAttributeValue() && !isIndirectlyInsideLambdaExpression()) {
				if(oldElement instanceof RecoveredInitializer
					|| oldElement instanceof RecoveredMethod
					|| (oldElement instanceof RecoveredBlock && oldElement.parent instanceof RecoveredInitializer)
					|| (oldElement instanceof RecoveredBlock && oldElement.parent instanceof RecoveredMethod)) {
					popUntilElement(K_METHOD_DELIMITER);
					popElement(K_METHOD_DELIMITER);
				} else if(oldElement instanceof RecoveredType) {
					popUntilElement(K_TYPE_DELIMITER);
					if(!(this.referenceContext instanceof CompilationUnitDeclaration)
							|| isIndirectlyInsideFieldInitialization()
							|| this.currentElement instanceof RecoveredUnit) {
						popElement(K_TYPE_DELIMITER);
					}
				}
			}
			break;
		default :
			super.recoveryTokenCheck();
			break;
	}
}
public void reset(){
	flushAssistState();
}

void commit(boolean isStart) {
	int newSnapShotPosition = this.scanner.startPosition;
	if (this.snapShotPtr == -1) {
		// first commit:
		addNewSnapShot(newSnapShotPosition);
	} else {
		// already have a snapshot, does it match the current position and can thus be reused?
		int currentStartPosition = isStart ? newSnapShotPosition : this.blockStarts[this.realBlockPtr];
		if (currentStartPosition != this.snapShotPositions[this.snapShotPtr])
			addNewSnapShot(newSnapShotPosition); // no match, create a new one
	}
	this.snapShotStack[this.snapShotPtr].copyState(this);
}

void addNewSnapShot(int newSnapShotPosition) {
	if (++this.snapShotPtr >= this.snapShotStack.length) {
		int len = this.snapShotStack.length;
		System.arraycopy(this.snapShotStack, 0, this.snapShotStack = new AssistParser[len+3], 0, len);
		System.arraycopy(this.snapShotPositions, 0, this.snapShotPositions = new int[len+3], 0, len);
	}
	this.snapShotStack[this.snapShotPtr] = createSnapShotParser();
	this.snapShotPositions[this.snapShotPtr] = newSnapShotPosition;
}

void popSnapShot() {
	this.snapShotStack[this.snapShotPtr--] = null;
}

protected boolean assistNodeNeedsStacking() {
	return false;
}

protected void shouldStackAssistNode() {
	// Not relevant here.
}

protected int getNextToken() {
	try {
		return this.scanner.getNextToken();
	} catch (InvalidInputException e) {
		return TokenNameEOF;
	}
}

protected abstract AssistParser createSnapShotParser();

// We get here on real syntax error or syntax error triggered by fake EOF at completion site, never due to triggered recovery.
protected int fallBackToSpringForward(Statement unused) {
	int nextToken;
	int automatonState = automatonState();

	// If triggered fake EOF at completion site, see if the real next token would have passed muster.
	if (this.currentToken == TokenNameEOF) {
		int extendedEnd = this.scanner.source.length;
		if (this.referenceContext instanceof AbstractMethodDeclaration)
			extendedEnd = ((AbstractMethodDeclaration) this.referenceContext).bodyEnd; // no use parsing beyond the method's body end
		if (this.cursorLocation < extendedEnd) {
			shouldStackAssistNode();
			// the following is against the new strategy as of https://bugs.eclipse.org/539685
			// but needed because CompletionParser.consumeToken(token == TokenNameIdentifier) still sets eof to cursorLocation
			if (this.scanner.eofPosition < extendedEnd)
				this.scanner.eofPosition = extendedEnd;
			nextToken = getNextToken();
			if (automatonWillShift(nextToken, automatonState)) {
				this.currentToken = nextToken;
				return RESUME;
			}
			this.scanner.ungetToken(nextToken); // spit out what has been bitten more than we can chew.
		} else {
			return HALT; // don't know how to proceed.
		}
	} else {
		if (this.scanner.currentPosition > this.cursorLocation)
			shouldStackAssistNode();
		nextToken = this.currentToken;
		this.scanner.ungetToken(nextToken);
		if (nextToken == TokenNameRBRACE)
			ignoreNextClosingBrace(); // having ungotten it, recoveryTokenCheck will see this again.
	}
	// OK, next token is no good to resume "in place", attempt some local repair. FIXME: need to make sure we don't get stuck keep reducing empty statements !!
	for (int i = 0, length = RECOVERY_TOKENS.length; i < length; i++) {
		if (automatonWillShift(RECOVERY_TOKENS[i], automatonState)) {
			this.currentToken = RECOVERY_TOKENS[i];
			return RESUME;
		}
	}
	// OK, no in place resumption, no local repair, fast forward to next statement.
	if (this.snapShotPtr == -1)
		return RESTART;

	this.copyState(this.snapShotStack[this.snapShotPtr]);
	if (assistNodeNeedsStacking()) {
		this.currentToken = TokenNameSEMICOLON;
		return RESUME;
	}
	this.currentToken = this.scanner.fastForward(unused);
	return RESUME;
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
	if (requireExtendedRecovery()) {
		if (this.unstackedAct == ERROR_ACTION) {
			int mode = fallBackToSpringForward((Statement) null);
			if (mode == RESUME && !safeToResume()) {
				mode = HALT; // emergency halt to prevent infinite recovery attempts
			}
			this.resumedAfterRepair = mode == RESUME;
			if (mode == RESUME || mode == HALT)
				return mode;
			// else fall through and RESTART
		} else {
			if (this.currentToken == TokenNameLBRACE)
				this.ignoreNextOpeningBrace = true;  // already accounted for in recovery token check.
			return RESUME;
		}
	}

	// reset internal stacks
	this.astPtr = -1;
	this.astLengthPtr = -1;
	this.expressionPtr = -1;
	this.expressionLengthPtr = -1;
	this.typeAnnotationLengthPtr = -1;
	this.typeAnnotationPtr = -1;

	this.identifierPtr = -1;
	this.identifierLengthPtr	= -1;
	this.intPtr = -1;


	this.dimensions = 0 ;
	this.recoveredStaticInitializerStart = 0;

	this.genericsIdentifiersLengthPtr = -1;
	this.genericsLengthPtr = -1;
	this.genericsPtr = -1;

	this.valueLambdaNestDepth = -1;

	this.modifiers = ClassFileConstants.AccDefault;
	this.modifiersSourceStart = -1;

	// if in diet mode, reset the diet counter because we're going to restart outside an initializer.
	if (this.diet) this.dietInt = 0;

	/* attempt to move checkpoint location */
	if (this.unstackedAct != ERROR_ACTION && this.resumedAfterRepair) {
		this.scanner.ungetToken(this.currentToken);  // effectively move recovery checkpoint *backwards*.
	} else {
		if (!moveRecoveryCheckpoint()) return HALT;
	}
	this.resumedAfterRepair = false;

	// only look for headers
	if (this.referenceContext instanceof CompilationUnitDeclaration
		|| this.assistNode != null){
		if(isInsideMethod() &&
			isIndirectlyInsideFieldInitialization() &&
			this.assistNode == null
			){
			prepareForBlockStatements();
			goForBlockStatementsOrCatchHeader();
		} else if((isInsideArrayInitializer()) &&
				isIndirectlyInsideFieldInitialization() &&
				this.assistNode == null){
			prepareForBlockStatements();
			goForBlockStatementsopt();
		} else {
			prepareForHeaders();
			if (this.referenceContext instanceof CompilationUnitDeclaration) {
				CompilationUnitDeclaration unit = (CompilationUnitDeclaration) this.referenceContext;
				if (unit.isModuleInfo()) {
					pushOnElementStack(K_MODULE_INFO_DELIMITER);
				}
			}
			goForHeaders();
			this.diet = true; // passed this point, will not consider method bodies
			this.dietInt = 0;
		}
		return RESTART;
	}
	if (this.referenceContext instanceof AbstractMethodDeclaration
		|| this.referenceContext instanceof TypeDeclaration){

		if (this.currentElement instanceof RecoveredType){
			prepareForHeaders();
			goForHeaders();
		} else {
			prepareForBlockStatements();
			goForBlockStatementsOrCatchHeader();
		}
		return RESTART;
	}
	// does not know how to restart
	return HALT;
}
private boolean safeToResume() {
	if (this.lastResumeState != null && this.noProgressSince(this.lastResumeState)) {
		if (this.resumeCount++ > 0)
			return false; // cause an emergency halt to prevent infinite recovery attempts
	} else {
		this.resumeCount = 0;
	}
	this.lastResumeState = createSnapShotParser();
	this.lastResumeState.copyState(this);
	return true;
}
private boolean noProgressSince(AssistParser previous) {
	int stateTop = this.stateStackTop;
	int astTop = this.astPtr;
	int exprTop = this.expressionPtr;
	return stateTop == previous.stateStackTop && astTop == previous.astPtr && exprTop == previous.expressionPtr
			&& Arrays.equals(this.stack, 0, stateTop, previous.stack, 0, stateTop)
			&& (astTop == -1 || Arrays.equals(this.astStack, 0, astTop, previous.astStack, 0, astTop))
			&& (exprTop == -1 || Arrays.equals(this.expressionStack, 0, exprTop, previous.expressionStack, 0, exprTop));
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=292087
// To be implemented in children viz. CompletionParser that are aware of array initializers
protected boolean isInsideArrayInitializer() {
	return false;
}
public abstract void setAssistIdentifier(char[] assistIdent);
protected int topKnownElementInfo(int owner) {
	return topKnownElementInfo(owner, 0);
}
protected int topKnownElementInfo(int owner, int offSet) {
	int i = this.elementPtr;
	while(i > -1) {
		if((this.elementKindStack[i] & owner) != 0) {
			if(offSet <= 0) return this.elementInfoStack[i];
			offSet--;
		}
		i--;
	}
	return 0;
}
protected int topKnownElementKind(int owner) {
	return topKnownElementKind(owner, 0);
}
protected int topKnownElementKind(int owner, int offSet) {
	int i = this.elementPtr;
	while(i > -1) {
		if((this.elementKindStack[i] & owner) != 0) {
			if(offSet <= 0) return this.elementKindStack[i];
			offSet--;
		}
		i--;
	}
	return 0;
}
protected Object topKnownElementObjectInfo(int owner, int offSet) {
	int i = this.elementPtr;
	while(i > -1) {
		if((this.elementKindStack[i] & owner) != 0) {
			if(offSet <= 0) return this.elementObjectInfoStack[i];
			offSet--;
		}
		i--;
	}
	return null;
}
protected Object topKnownElementObjectInfo(int owner) {
	return topKnownElementObjectInfo(owner, 0);
}
/**
 * If the given ast node is inside an explicit constructor call
 * then wrap it with a fake constructor call.
 * Returns the wrapped completion node or the completion node itself.
 */
protected ASTNode wrapWithExplicitConstructorCallIfNeeded(ASTNode ast) {
	int selector;
	if (ast != null && topKnownElementKind(ASSIST_PARSER) == K_SELECTOR && ast instanceof Expression &&
			((Expression) ast).isTrulyExpression() &&
			(((selector = topKnownElementInfo(ASSIST_PARSER)) == THIS_CONSTRUCTOR) ||
			(selector == SUPER_CONSTRUCTOR))) {
		ExplicitConstructorCall call = new ExplicitConstructorCall(
			(selector == THIS_CONSTRUCTOR) ?
				ExplicitConstructorCall.This :
				ExplicitConstructorCall.Super
		);
		call.arguments = new Expression[] {(Expression)ast};
		call.sourceStart = ast.sourceStart;
		call.sourceEnd = ast.sourceEnd;
		return call;
	} else {
		return ast;
	}
}
}
