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
package org.eclipse.jdt.internal.core.search.matching;

import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;

/**
 * A parser that locates ast nodes that match a given search pattern.
 */
public class MatchLocatorParser extends Parser {

	MatchingNodeSet nodeSet;
	PatternLocator patternLocator;
	private ASTVisitor localDeclarationVisitor;
	final int patternFineGrain;

public static MatchLocatorParser createParser(ProblemReporter problemReporter, MatchLocator locator) {
	if ((locator.matchContainer & PatternLocator.COMPILATION_UNIT_CONTAINER) != 0) {
		return new ImportMatchLocatorParser(problemReporter, locator);
	}
	return new MatchLocatorParser(problemReporter, locator);
}

/**
 * An ast visitor that visits local type declarations.
 */
public static class NoClassNoMethodDeclarationVisitor extends ASTVisitor {
	@Override
	public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
		return (constructorDeclaration.bits & ASTNode.HasLocalType) != 0; // continue only if it has local type
	}
	@Override
	public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
		return (fieldDeclaration.bits & ASTNode.HasLocalType) != 0; // continue only if it has local type;
	}
	@Override
	public boolean visit(Initializer initializer, MethodScope scope) {
		return (initializer.bits & ASTNode.HasLocalType) != 0; // continue only if it has local type
	}
	@Override
	public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
		return (methodDeclaration.bits & ASTNode.HasLocalType) != 0; // continue only if it has local type
	}
}
public class MethodButNoClassDeclarationVisitor extends NoClassNoMethodDeclarationVisitor {
	@Override
	public boolean visit(TypeDeclaration localTypeDeclaration, BlockScope scope) {
		MatchLocatorParser.this.patternLocator.match(localTypeDeclaration, MatchLocatorParser.this.nodeSet);
		return true;
	}
}
public class ClassButNoMethodDeclarationVisitor extends ASTVisitor {
	@Override
	public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
		MatchLocatorParser.this.patternLocator.match(constructorDeclaration, MatchLocatorParser.this.nodeSet);
		return (constructorDeclaration.bits & ASTNode.HasLocalType) != 0; // continue only if it has local type
	}
	@Override
	public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
		MatchLocatorParser.this.patternLocator.match(fieldDeclaration, MatchLocatorParser.this.nodeSet);
		return (fieldDeclaration.bits & ASTNode.HasLocalType) != 0; // continue only if it has local type;
	}
	@Override
	public boolean visit(Initializer initializer, MethodScope scope) {
		MatchLocatorParser.this.patternLocator.match(initializer, MatchLocatorParser.this.nodeSet);
		return (initializer.bits & ASTNode.HasLocalType) != 0; // continue only if it has local type
	}
	@Override
	public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
		MatchLocatorParser.this.patternLocator.match(memberTypeDeclaration, MatchLocatorParser.this.nodeSet);
		return true;
	}
	@Override
	public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
		MatchLocatorParser.this.patternLocator.match(methodDeclaration, MatchLocatorParser.this.nodeSet);
		return (methodDeclaration.bits & ASTNode.HasLocalType) != 0; // continue only if it has local type
	}
	@Override
	public boolean visit(AnnotationMethodDeclaration methodDeclaration, ClassScope scope) {
		MatchLocatorParser.this.patternLocator.match(methodDeclaration, MatchLocatorParser.this.nodeSet);
		return false; // no local type for annotation type members
	}
}
public class ClassAndMethodDeclarationVisitor extends ClassButNoMethodDeclarationVisitor {
	@Override
	public boolean visit(TypeDeclaration localTypeDeclaration, BlockScope scope) {
		MatchLocatorParser.this.patternLocator.match(localTypeDeclaration, MatchLocatorParser.this.nodeSet);
		return true;
	}
}

protected MatchLocatorParser(ProblemReporter problemReporter, MatchLocator locator) {
	super(problemReporter, true);
	this.reportOnlyOneSyntaxError = true;
	this.patternLocator = locator.patternLocator;
	if ((locator.matchContainer & PatternLocator.CLASS_CONTAINER) != 0) {
		this.localDeclarationVisitor = (locator.matchContainer & PatternLocator.METHOD_CONTAINER) != 0
			? new ClassAndMethodDeclarationVisitor()
			: new ClassButNoMethodDeclarationVisitor();
	} else {
		this.localDeclarationVisitor = (locator.matchContainer & PatternLocator.METHOD_CONTAINER) != 0
			? new MethodButNoClassDeclarationVisitor()
			: new NoClassNoMethodDeclarationVisitor();
	}
	this.patternFineGrain = this.patternLocator.fineGrain();
}
@Override
public void checkComment() {
	super.checkComment();
	if (this.javadocParser.checkDocComment && this.javadoc != null && this.patternFineGrain == 0 /* there's no fine grain concerning Javadoc*/) {

		// Search for pattern locator matches in javadoc comment parameters @param tags
		JavadocSingleNameReference[] paramReferences = this.javadoc.paramReferences;
		if (paramReferences != null) {
			for (int i=0, length=paramReferences.length; i < length; i++) {
				this.patternLocator.match(paramReferences[i], this.nodeSet);
			}
		}

		// Search for pattern locator matches in javadoc comment type parameters @param tags
		JavadocSingleTypeReference[] paramTypeParameters = this.javadoc.paramTypeParameters;
		if (paramTypeParameters != null) {
			for (int i=0, length=paramTypeParameters.length; i < length; i++) {
				this.patternLocator.match(paramTypeParameters[i], this.nodeSet);
			}
		}

		// Search for pattern locator matches in javadoc comment @throws/@exception tags
		TypeReference[] thrownExceptions = this.javadoc.exceptionReferences;
		if (thrownExceptions != null) {
			for (int i=0, length=thrownExceptions.length; i < length; i++) {
				this.patternLocator.match(thrownExceptions[i], this.nodeSet);
			}
		}

		// Search for pattern locator matches in javadoc comment @see tags
		Expression[] references = this.javadoc.seeReferences;
		if (references != null) {
			for (int i=0, length=references.length; i < length; i++) {
				Expression reference = references[i];
				if (reference instanceof JavadocModuleReference) {
					JavadocModuleReference modRef = (JavadocModuleReference)reference;
					if (modRef.typeReference != null) {
						this.patternLocator.match(modRef.typeReference, this.nodeSet);
					}
				}
				if (reference instanceof TypeReference) {
					TypeReference typeRef = (TypeReference) reference;
					this.patternLocator.match(typeRef, this.nodeSet);
				} else if (reference instanceof JavadocFieldReference) {
					JavadocFieldReference fieldRef = (JavadocFieldReference) reference;
					this.patternLocator.match(fieldRef, this.nodeSet);
					if (fieldRef.receiver instanceof TypeReference && !fieldRef.receiver.isThis()) {
						TypeReference typeRef = (TypeReference) fieldRef.receiver;
						this.patternLocator.match(typeRef, this.nodeSet);
					}
					if (fieldRef.receiver instanceof JavadocModuleReference) {
						JavadocModuleReference modRef = (JavadocModuleReference)fieldRef.receiver;
						if (modRef.typeReference != null) {
							this.patternLocator.match(modRef.typeReference, this.nodeSet);
						}
					}
				} else if (reference instanceof JavadocMessageSend) {
					JavadocMessageSend messageSend = (JavadocMessageSend) reference;
					this.patternLocator.match(messageSend, this.nodeSet);
					if (messageSend.receiver instanceof TypeReference && !messageSend.receiver.isThis()) {
						TypeReference typeRef = (TypeReference) messageSend.receiver;
						this.patternLocator.match(typeRef, this.nodeSet);
					}
					if (messageSend.arguments != null) {
						for (int a=0,al=messageSend.arguments.length; a<al; a++) {
							JavadocArgumentExpression argument = (JavadocArgumentExpression) messageSend.arguments[a];
							if (argument.argument != null && argument.argument.type != null) {
								this.patternLocator.match(argument.argument.type, this.nodeSet);
							}
						}
					}
				} else if (reference instanceof JavadocAllocationExpression) {
					JavadocAllocationExpression constructor = (JavadocAllocationExpression) reference;
					this.patternLocator.match(constructor, this.nodeSet);
					if (constructor.type != null && !constructor.type.isThis()) {
						this.patternLocator.match(constructor.type, this.nodeSet);
					}
					if (constructor.arguments != null) {
						for (int a=0,al=constructor.arguments.length; a<al; a++) {
							this.patternLocator.match(constructor.arguments[a], this.nodeSet);
							JavadocArgumentExpression argument = (JavadocArgumentExpression) constructor.arguments[a];
							if (argument.argument != null && argument.argument.type != null) {
								this.patternLocator.match(argument.argument.type, this.nodeSet);
							}
						}
					}
				}
			}
		}
	}
}

@Override
protected void classInstanceCreation(boolean alwaysQualified) {
	super.classInstanceCreation(alwaysQualified);
	if (this.patternFineGrain == 0) {
		this.patternLocator.match(this.expressionStack[this.expressionPtr], this.nodeSet);
	} else if ((this.patternFineGrain & IJavaSearchConstants.CLASS_INSTANCE_CREATION_TYPE_REFERENCE) != 0) {
		AllocationExpression allocation = (AllocationExpression) this.expressionStack[this.expressionPtr];
		this.patternLocator.match(allocation.type, this.nodeSet);
	}
}

@Override
protected void consumeAdditionalBound() {
	super.consumeAdditionalBound();
	if ((this.patternFineGrain & IJavaSearchConstants.TYPE_VARIABLE_BOUND_TYPE_REFERENCE) != 0) {
		TypeReference typeReference = (TypeReference) this.genericsStack[this.genericsPtr];
		this.patternLocator.match(typeReference, this.nodeSet);
	}
}

@Override
protected void consumeAssignment() {
	super.consumeAssignment();
	if (this.patternFineGrain == 0) {
		this.patternLocator.match(this.expressionStack[this.expressionPtr], this.nodeSet);
	}
}

@Override
protected void consumeCastExpressionLL1() {
	super.consumeCastExpressionLL1();
	if ((this.patternFineGrain & IJavaSearchConstants.CAST_TYPE_REFERENCE) != 0) {
		CastExpression castExpression = (CastExpression) this.expressionStack[this.expressionPtr];
		this.patternLocator.match(castExpression.type, this.nodeSet);
	}
}
@Override
protected void consumeCastExpressionLL1WithBounds() {
	super.consumeCastExpressionLL1WithBounds();
	if ((this.patternFineGrain & IJavaSearchConstants.CAST_TYPE_REFERENCE) != 0) {
		CastExpression castExpression = (CastExpression) this.expressionStack[this.expressionPtr];
		TypeReference[] typeReferences = ((IntersectionCastTypeReference) castExpression.type).typeReferences;
		for (int i = 0, length = typeReferences.length; i < length; i++)
			this.patternLocator.match(typeReferences[i], this.nodeSet);
	}
}
@Override
protected void consumeCastExpressionWithGenericsArray() {
	super.consumeCastExpressionWithGenericsArray();
	if ((this.patternFineGrain & IJavaSearchConstants.CAST_TYPE_REFERENCE) != 0) {
		CastExpression castExpression = (CastExpression) this.expressionStack[this.expressionPtr];
		this.patternLocator.match(castExpression.type, this.nodeSet);
	}
}
@Override
protected void consumeCastExpressionWithNameArray() {
	super.consumeCastExpressionWithNameArray();
	if ((this.patternFineGrain & IJavaSearchConstants.CAST_TYPE_REFERENCE) != 0) {
		CastExpression castExpression = (CastExpression) this.expressionStack[this.expressionPtr];
		this.patternLocator.match(castExpression.type, this.nodeSet);
	}
}
@Override
protected void consumeCastExpressionWithPrimitiveType() {
	super.consumeCastExpressionWithPrimitiveType();
	if ((this.patternFineGrain & IJavaSearchConstants.CAST_TYPE_REFERENCE) != 0) {
		CastExpression castExpression = (CastExpression) this.expressionStack[this.expressionPtr];
		this.patternLocator.match(castExpression.type, this.nodeSet);
	}
}
@Override
protected void consumeCastExpressionWithQualifiedGenericsArray() {
	super.consumeCastExpressionWithQualifiedGenericsArray();
	if ((this.patternFineGrain & IJavaSearchConstants.CAST_TYPE_REFERENCE) != 0) {
		CastExpression castExpression = (CastExpression) this.expressionStack[this.expressionPtr];
		this.patternLocator.match(castExpression.type, this.nodeSet);
	}
}
@Override
protected void consumeCatchFormalParameter() {
	super.consumeCatchFormalParameter();
	this.patternLocator.match((LocalDeclaration) this.astStack[this.astPtr], this.nodeSet);
}

@Override
protected void consumeClassHeaderExtends() {
	this.patternLocator.setFlavors(PatternLocator.SUPERTYPE_REF_FLAVOR);
	super.consumeClassHeaderExtends();
	if ((this.patternFineGrain & IJavaSearchConstants.SUPERTYPE_TYPE_REFERENCE) != 0) {
		TypeDeclaration typeDeclaration = (TypeDeclaration) this.astStack[this.astPtr];
		this.patternLocator.match(typeDeclaration.superclass, this.nodeSet);
	}
	this.patternLocator.setFlavors(PatternLocator.NO_FLAVOR);
}

@Override
protected void consumeClassInstanceCreationExpressionQualifiedWithTypeArguments() {
	super.consumeClassInstanceCreationExpressionWithTypeArguments();
	if (this.patternFineGrain == 0) {
		this.patternLocator.match(this.expressionStack[this.expressionPtr], this.nodeSet);
	} else if ((this.patternFineGrain & IJavaSearchConstants.CLASS_INSTANCE_CREATION_TYPE_REFERENCE) != 0) {
		AllocationExpression allocation = (AllocationExpression) this.expressionStack[this.expressionPtr];
		this.patternLocator.match(allocation.type, this.nodeSet);
	}
}

@Override
protected void consumeClassInstanceCreationExpressionWithTypeArguments() {
	super.consumeClassInstanceCreationExpressionWithTypeArguments();
	if (this.patternFineGrain == 0) {
		this.patternLocator.match(this.expressionStack[this.expressionPtr], this.nodeSet);
	} else if ((this.patternFineGrain & IJavaSearchConstants.CLASS_INSTANCE_CREATION_TYPE_REFERENCE) != 0) {
		AllocationExpression allocation = (AllocationExpression) this.expressionStack[this.expressionPtr];
		this.patternLocator.match(allocation.type, this.nodeSet);
	}
}

@Override
protected void consumeEnterAnonymousClassBody(boolean qualified) {
	this.patternLocator.setFlavors(PatternLocator.SUPERTYPE_REF_FLAVOR);
	super.consumeEnterAnonymousClassBody(qualified);
	this.patternLocator.setFlavors(PatternLocator.NO_FLAVOR);
}

@Override
protected void consumeEnterVariable() {
	boolean isLocalDeclaration = this.nestedMethod[this.nestedType] != 0;
	super.consumeEnterVariable();
	if (isLocalDeclaration) {
		if ((this.patternFineGrain & IJavaSearchConstants.LOCAL_VARIABLE_DECLARATION_TYPE_REFERENCE) != 0) {
			LocalDeclaration localDeclaration = (LocalDeclaration) this.astStack[this.astPtr];
			this.patternLocator.match(localDeclaration.type, this.nodeSet);
		}
	} else {
		if ((this.patternFineGrain & IJavaSearchConstants.FIELD_DECLARATION_TYPE_REFERENCE) != 0) {
			FieldDeclaration fieldDeclaration = (FieldDeclaration) this.astStack[this.astPtr];
			this.patternLocator.match(fieldDeclaration.type, this.nodeSet);
		}
	}
}

@Override
protected void consumeExplicitConstructorInvocation(int flag, int recFlag) {
	super.consumeExplicitConstructorInvocation(flag, recFlag);
	this.patternLocator.match(this.astStack[this.astPtr], this.nodeSet);
}
@Override
protected void consumeExplicitConstructorInvocationWithTypeArguments(int flag, int recFlag) {
	super.consumeExplicitConstructorInvocationWithTypeArguments(flag, recFlag);
	this.patternLocator.match(this.astStack[this.astPtr], this.nodeSet);
}
@Override
protected void consumeExportsHeader() {
	super.consumeExportsHeader();
	this.patternLocator.match(((ExportsStatement) this.astStack[this.astPtr]).pkgRef, this.nodeSet);
}
@Override
protected void consumeFieldAccess(boolean isSuperAccess) {
	super.consumeFieldAccess(isSuperAccess);

	int fineGrain = isSuperAccess ? IJavaSearchConstants.SUPER_REFERENCE : IJavaSearchConstants.THIS_REFERENCE;
	if (this.patternFineGrain == 0 || (this.patternFineGrain & fineGrain) != 0) {
		// this is always a Reference
		this.patternLocator.match((Reference) this.expressionStack[this.expressionPtr], this.nodeSet);
	}
}

@Override
protected void consumeFormalParameter(boolean isVarArgs) {
	super.consumeFormalParameter(isVarArgs);
	this.patternLocator.match((LocalDeclaration) this.astStack[this.astPtr], this.nodeSet);
}

@Override
protected void consumeInstanceOfExpression() {
	super.consumeInstanceOfExpression();
	if ((this.patternFineGrain & IJavaSearchConstants.INSTANCEOF_TYPE_REFERENCE) != 0) {
		InstanceOfExpression expression = (InstanceOfExpression) this.expressionStack[this.expressionPtr];
		this.patternLocator.match(expression.type, this.nodeSet);
	}
	matchPatternVariable();
}
@Override
protected void consumeInstanceOfExpressionWithName() {
	super.consumeInstanceOfExpressionWithName();
	if ((this.patternFineGrain & IJavaSearchConstants.INSTANCEOF_TYPE_REFERENCE) != 0) {
		InstanceOfExpression expression = (InstanceOfExpression) this.expressionStack[this.expressionPtr];
		this.patternLocator.match(expression.type, this.nodeSet);
	}
	matchPatternVariable();
}

private void matchPatternVariable() {
	if (this.patternFineGrain == 0) {
		InstanceOfExpression expression = (InstanceOfExpression) this.expressionStack[this.expressionPtr];
		LocalDeclaration elementVariable = expression.elementVariable;
		if (elementVariable != null) {
			// if pattern variable present, match that
			this.patternLocator.match(elementVariable, this.nodeSet);
		}
	}
}
@Override
protected void consumeInterfaceType() {
	this.patternLocator.setFlavors(PatternLocator.SUPERTYPE_REF_FLAVOR);
	super.consumeInterfaceType();
	if ((this.patternFineGrain & IJavaSearchConstants.SUPERTYPE_TYPE_REFERENCE) != 0) {
		TypeReference typeReference = (TypeReference) this.astStack[this.astPtr];
		this.patternLocator.match(typeReference, this.nodeSet);
	}
	this.patternLocator.setFlavors(PatternLocator.NO_FLAVOR);
}

@Override
protected void consumeLambdaExpression() {
	super.consumeLambdaExpression();
	this.patternLocator.match((LambdaExpression) this.expressionStack[this.expressionPtr], this.nodeSet);
}

@Override
protected void consumeLocalVariableDeclaration() {
	super.consumeLocalVariableDeclaration();
	this.patternLocator.match((LocalDeclaration) this.astStack[this.astPtr], this.nodeSet);
}

@Override
protected void consumeMarkerAnnotation(boolean isTypeAnnotation) {
	super.consumeMarkerAnnotation(isTypeAnnotation);
	if (this.patternFineGrain == 0 || (this.patternFineGrain & IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE) != 0) {
		Annotation annotation = (Annotation) (isTypeAnnotation ? this.typeAnnotationStack[this.typeAnnotationPtr] : this.expressionStack[this.expressionPtr]);
		this.patternLocator.match(annotation, this.nodeSet);
	}
}
@Override
protected void consumeMemberValuePair() {
	super.consumeMemberValuePair();
	if ((this.patternFineGrain & ~IJavaSearchConstants.METHOD_REFERENCE_EXPRESSION) != 0) {
		this.patternLocator.match((MemberValuePair) this.astStack[this.astPtr], this.nodeSet);
	}
}

@Override
protected void consumeMethodHeaderName(boolean isAnnotationMethod) {
	super.consumeMethodHeaderName(isAnnotationMethod);
	if ((this.patternFineGrain & IJavaSearchConstants.RETURN_TYPE_REFERENCE) != 0) {
		// when no fine grain flag is set, type reference match is evaluated in getTypeReference(int) method
		MethodDeclaration methodDeclaration = (MethodDeclaration) this.astStack[this.astPtr];
		this.patternLocator.match(methodDeclaration.returnType, this.nodeSet);
	}
}

@Override
protected void consumeMethodHeaderNameWithTypeParameters(boolean isAnnotationMethod) {
	super.consumeMethodHeaderNameWithTypeParameters(isAnnotationMethod);
	if ((this.patternFineGrain & IJavaSearchConstants.RETURN_TYPE_REFERENCE) != 0) {
		// when no fine grain flag is set, type reference match is evaluated in getTypeReference(int) method
		MethodDeclaration methodDeclaration = (MethodDeclaration) this.astStack[this.astPtr];
		this.patternLocator.match(methodDeclaration.returnType, this.nodeSet);
	}
}

@Override
protected void consumeMethodHeaderRightParen() {
	super.consumeMethodHeaderRightParen();
	if ((this.patternFineGrain & IJavaSearchConstants.PARAMETER_DECLARATION_TYPE_REFERENCE) != 0) {
		// when no fine grain flag is set, type reference match is evaluated in getTypeReference(int) method
		AbstractMethodDeclaration methodDeclaration = (AbstractMethodDeclaration) this.astStack[this.astPtr];
		Argument[] arguments = methodDeclaration.arguments;
		if (arguments != null) {
			int argLength = arguments.length;
			for (int i=0; i<argLength; i++) {
				this.patternLocator.match(arguments[i].type, this.nodeSet);
			}
		}
	}
}
@Override
protected void consumeMethodHeaderThrowsClause() {
	super.consumeMethodHeaderThrowsClause();
	if ((this.patternFineGrain & IJavaSearchConstants.THROWS_CLAUSE_TYPE_REFERENCE) != 0) {
		// when no fine grain flag is set, type reference match is evaluated in getTypeReference(int) method
		AbstractMethodDeclaration methodDeclaration = (AbstractMethodDeclaration) this.astStack[this.astPtr];
		TypeReference[] thrownExceptions = methodDeclaration.thrownExceptions;
		if (thrownExceptions != null) {
			int thrownLength = thrownExceptions.length;
			for (int i=0; i<thrownLength; i++) {
				this.patternLocator.match(thrownExceptions[i], this.nodeSet);
			}
		}
	}
}

@Override
protected void consumeMethodInvocationName() {
	super.consumeMethodInvocationName();
	MessageSend messageSend = (MessageSend) this.expressionStack[this.expressionPtr];
	if (this.patternFineGrain == 0) {
		this.patternLocator.match(messageSend, this.nodeSet);
	} else {
		if (messageSend.receiver.isThis()) {
			if ((this.patternFineGrain & IJavaSearchConstants.IMPLICIT_THIS_REFERENCE) != 0) {
				this.patternLocator.match(messageSend, this.nodeSet);
			}
		} else {
			if ((this.patternFineGrain & IJavaSearchConstants.QUALIFIED_REFERENCE) != 0) {
				this.patternLocator.match(messageSend, this.nodeSet);
			}
		}
	}
}

@Override
protected void consumeMethodInvocationNameWithTypeArguments() {
	super.consumeMethodInvocationNameWithTypeArguments();
	MessageSend messageSend = (MessageSend) this.expressionStack[this.expressionPtr];
	if (this.patternFineGrain == 0) {
		this.patternLocator.match(messageSend, this.nodeSet);
	} else {
		if (messageSend.receiver.isThis()) {
			if ((this.patternFineGrain & IJavaSearchConstants.IMPLICIT_THIS_REFERENCE) != 0) {
				this.patternLocator.match(messageSend, this.nodeSet);
			}
		} else {
			if ((this.patternFineGrain & IJavaSearchConstants.QUALIFIED_REFERENCE) != 0) {
				this.patternLocator.match(messageSend, this.nodeSet);
			}
		}
	}
}

@Override
protected void consumeMethodInvocationPrimary() {
	super.consumeMethodInvocationPrimary();
	if (this.patternFineGrain == 0 || (this.patternFineGrain & IJavaSearchConstants.THIS_REFERENCE) != 0) {
		this.patternLocator.match((MessageSend) this.expressionStack[this.expressionPtr], this.nodeSet);
	}
}

@Override
protected void consumeMethodInvocationPrimaryWithTypeArguments() {
	super.consumeMethodInvocationPrimaryWithTypeArguments();
	if (this.patternFineGrain == 0 || (this.patternFineGrain & IJavaSearchConstants.THIS_REFERENCE) != 0) {
		this.patternLocator.match((MessageSend) this.expressionStack[this.expressionPtr], this.nodeSet);
	}
}

@Override
protected void consumeMethodInvocationSuper() {
	super.consumeMethodInvocationSuper();
	if (this.patternFineGrain == 0 || (this.patternFineGrain & IJavaSearchConstants.SUPER_REFERENCE) != 0) {
		this.patternLocator.match((MessageSend) this.expressionStack[this.expressionPtr], this.nodeSet);
	}
}

@Override
protected void consumeMethodInvocationSuperWithTypeArguments() {
	super.consumeMethodInvocationSuperWithTypeArguments();
	if (this.patternFineGrain == 0 || (this.patternFineGrain & IJavaSearchConstants.SUPER_REFERENCE) != 0) {
		this.patternLocator.match((MessageSend) this.expressionStack[this.expressionPtr], this.nodeSet);
	}
}

@Override
protected void consumeModuleHeader() {
	super.consumeModuleHeader();
	this.patternLocator.match(((ModuleDeclaration) this.astStack[this.astPtr]), this.nodeSet);
}
@Override
protected void consumeNormalAnnotation(boolean isTypeAnnotation) {
	super.consumeNormalAnnotation(isTypeAnnotation);
	if (this.patternFineGrain == 0 || (this.patternFineGrain & IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE) != 0) {
		// this is always an Annotation
		Annotation annotation = (Annotation) (isTypeAnnotation ? this.typeAnnotationStack[this.typeAnnotationPtr] : this.expressionStack[this.expressionPtr]);
		this.patternLocator.match(annotation, this.nodeSet);
	}
}

@Override
protected void consumeOnlyTypeArguments() {
	super.consumeOnlyTypeArguments();
	if ((this.patternFineGrain & IJavaSearchConstants.TYPE_ARGUMENT_TYPE_REFERENCE) != 0) {
		int length = this.genericsLengthStack[this.genericsLengthPtr];
		if (length == 1) {
			TypeReference typeReference = (TypeReference)this.genericsStack[this.genericsPtr];
			if (!(typeReference instanceof Wildcard)) {
				this.patternLocator.match(typeReference, this.nodeSet);
            }
		}
	}
}
@Override
protected void consumeOpensHeader() {
	super.consumeOpensHeader();
	this.patternLocator.match(((OpensStatement) this.astStack[this.astPtr]).pkgRef, this.nodeSet);
}
@Override
protected void consumeProvidesInterface() {
	super.consumeProvidesInterface();
	ProvidesStatement ref = (ProvidesStatement) this.astStack[this.astPtr];
	this.patternLocator.match(ref.serviceInterface, this.nodeSet);
}
@Override
protected void consumeProvidesStatement() {
	super.consumeProvidesStatement();
	ProvidesStatement ref = (ProvidesStatement) this.astStack[this.astPtr];
	TypeReference[] impls = ref.implementations;
	for (TypeReference impl : impls) {
		this.patternLocator.match(impl, this.nodeSet);
	}
}
@Override
protected void consumePrimaryNoNewArrayWithName() {
	// PrimaryNoNewArray ::=  PushLPAREN Expression PushRPAREN
	pushOnExpressionStack(getUnspecifiedReferenceOptimized());
	// pop parenthesis positions (and don't update expression positions
	// (see http://bugs.eclipse.org/bugs/show_bug.cgi?id=23329)
	this.intPtr--;
	this.intPtr--;
}

@Override
protected void consumeReferenceExpression(ReferenceExpression referenceExpression) {
	super.consumeReferenceExpression(referenceExpression);
	if (this.patternFineGrain == 0) {
		this.patternLocator.match(referenceExpression, this.nodeSet);
	} else if ((this.patternFineGrain & IJavaSearchConstants.METHOD_REFERENCE_EXPRESSION) != 0) {
		this.patternLocator.match(referenceExpression, this.nodeSet);
	} else if (referenceExpression.lhs.isThis()) {
		if ((this.patternFineGrain & IJavaSearchConstants.THIS_REFERENCE) != 0) {
			this.patternLocator.match(referenceExpression, this.nodeSet);
		}
	} else if (referenceExpression.lhs.isSuper()) {
		if ((this.patternFineGrain & IJavaSearchConstants.SUPER_REFERENCE) != 0) {
			this.patternLocator.match(referenceExpression, this.nodeSet);
		}
	} else if (referenceExpression.lhs instanceof QualifiedNameReference || referenceExpression.lhs instanceof QualifiedTypeReference) {
		if ((this.patternFineGrain & IJavaSearchConstants.QUALIFIED_REFERENCE) != 0) {
			this.patternLocator.match(referenceExpression, this.nodeSet);
		}
	}
}

@Override
protected void consumeSingleMemberAnnotation(boolean isTypeAnnotation) {
	super.consumeSingleMemberAnnotation(isTypeAnnotation);
	if (this.patternFineGrain == 0 || (this.patternFineGrain & IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE) != 0) {
		// this is always an Annotation
		Annotation annotation = (Annotation) (isTypeAnnotation ? this.typeAnnotationStack[this.typeAnnotationPtr] : this.expressionStack[this.expressionPtr]);
		this.patternLocator.match(annotation, this.nodeSet);
	}
}
@Override
protected void consumeSingleRequiresModuleName() {
	super.consumeSingleRequiresModuleName();
	RequiresStatement req = (RequiresStatement) this.astStack[this.astPtr];
	this.patternLocator.match(req.module, this.nodeSet);
}
private void setTarget(boolean flag) {
	if (this.patternLocator instanceof ModuleLocator) {
		((ModuleLocator) this.patternLocator).target = flag;
	}
}
@Override
protected void consumeSingleTargetModuleName() {
	super.consumeSingleTargetModuleName();
	setTarget(true);
	this.patternLocator.match((ModuleReference)this.astStack[this.astPtr], this.nodeSet);
	setTarget(false);
}

@Override
protected void consumeStatementCatch() {
	super.consumeStatementCatch();
	if ((this.patternFineGrain & IJavaSearchConstants.CATCH_TYPE_REFERENCE) != 0) {
		// when no fine grain flag is set, type reference match is evaluated in getTypeReference(int) method
		LocalDeclaration localDeclaration = (LocalDeclaration) this.astStack[this.astPtr-1];
		if (localDeclaration.type instanceof UnionTypeReference) {
			TypeReference[] refs = ((UnionTypeReference)localDeclaration.type).typeReferences;
			for (int i = 0, len  = refs.length; i < len; i++) {
				this.patternLocator.match(refs[i], this.nodeSet);
			}
		} else {
			this.patternLocator.match(localDeclaration.type, this.nodeSet);
		}
	}
}

@Override
protected void consumeTypeArgumentList1() {
	super.consumeTypeArgumentList1();
	if ((this.patternFineGrain & IJavaSearchConstants.TYPE_ARGUMENT_TYPE_REFERENCE) != 0) {
		for (int i=this.genericsPtr-this.genericsLengthStack[this.genericsLengthPtr]+1; i<=this.genericsPtr; i++) {
			TypeReference typeReference = (TypeReference)this.genericsStack[i];
			if (!(typeReference instanceof Wildcard)) {
				this.patternLocator.match(typeReference, this.nodeSet);
            }
		}
	}
}

@Override
protected void consumeTypeArgumentList2() {
	super.consumeTypeArgumentList2();
	if ((this.patternFineGrain & IJavaSearchConstants.TYPE_ARGUMENT_TYPE_REFERENCE) != 0) {
		for (int i=this.genericsPtr-this.genericsLengthStack[this.genericsLengthPtr]+1; i<=this.genericsPtr; i++) {
			TypeReference typeReference = (TypeReference)this.genericsStack[i];
			if (!(typeReference instanceof Wildcard)) {
				this.patternLocator.match(typeReference, this.nodeSet);
            }
		}
	}
}

@Override
protected void consumeTypeArgumentList3() {
	super.consumeTypeArgumentList3();
	if ((this.patternFineGrain & IJavaSearchConstants.TYPE_ARGUMENT_TYPE_REFERENCE) != 0) {
		for (int i=this.genericsPtr-this.genericsLengthStack[this.genericsLengthPtr]+1; i<=this.genericsPtr; i++) {
			TypeReference typeReference = (TypeReference)this.genericsStack[i];
			if (!(typeReference instanceof Wildcard)) {
				this.patternLocator.match(typeReference, this.nodeSet);
            }
		}
	}
}

@Override
protected void consumeTypeArgumentReferenceType1() {
	super.consumeTypeArgumentReferenceType1();
	if ((this.patternFineGrain & IJavaSearchConstants.TYPE_ARGUMENT_TYPE_REFERENCE) != 0) {
		int length = this.genericsLengthStack[this.genericsLengthPtr];
		if (length == 1) {
			TypeReference typeReference = (TypeReference)this.genericsStack[this.genericsPtr];
			TypeReference[] typeArguments = null;
			if (typeReference instanceof ParameterizedSingleTypeReference) {
	            typeArguments = ((ParameterizedSingleTypeReference) typeReference).typeArguments;
            } else if (typeReference instanceof ParameterizedQualifiedTypeReference) {
	            TypeReference[][] allTypeArguments = ((ParameterizedQualifiedTypeReference) typeReference).typeArguments;
	            typeArguments = allTypeArguments[allTypeArguments.length-1];
            }
			if (typeArguments != null) {
	            for (int i=0, ln=typeArguments.length; i<ln; i++) {
	            	if (!(typeArguments[i] instanceof Wildcard)) {
						this.patternLocator.match(typeArguments[i], this.nodeSet);
	            	}
	            }
			}
		}
	}
}

@Override
protected void consumeTypeArgumentReferenceType2() {
	super.consumeTypeArgumentReferenceType2();
	if ((this.patternFineGrain & IJavaSearchConstants.TYPE_ARGUMENT_TYPE_REFERENCE) != 0) {
		int length = this.genericsLengthStack[this.genericsLengthPtr];
		if (length == 1) {
			TypeReference typeReference = (TypeReference)this.genericsStack[this.genericsPtr];
			TypeReference[] typeArguments = null;
			if (typeReference instanceof ParameterizedSingleTypeReference) {
	            typeArguments = ((ParameterizedSingleTypeReference) typeReference).typeArguments;
            } else if (typeReference instanceof ParameterizedQualifiedTypeReference) {
	            TypeReference[][] allTypeArguments = ((ParameterizedQualifiedTypeReference) typeReference).typeArguments;
	            typeArguments = allTypeArguments[allTypeArguments.length-1];
            }
			if (typeArguments != null) {
	            for (int i=0, ln=typeArguments.length; i<ln; i++) {
	            	if (!(typeArguments[i] instanceof Wildcard)) {
						this.patternLocator.match(typeArguments[i], this.nodeSet);
	            	}
	            }
			}
		}
	}
}

@Override
protected void consumeTypeArguments() {
	super.consumeTypeArguments();
	if ((this.patternFineGrain & IJavaSearchConstants.TYPE_ARGUMENT_TYPE_REFERENCE) != 0) {
		int length = this.genericsLengthStack[this.genericsLengthPtr];
		if (length == 1) {
			TypeReference typeReference = (TypeReference)this.genericsStack[this.genericsPtr];
			if (!(typeReference instanceof Wildcard)) {
				this.patternLocator.match(typeReference, this.nodeSet);
            }
		}
	}
}

@Override
protected void consumeTypeElidedLambdaParameter(boolean parenthesized) {
	super.consumeTypeElidedLambdaParameter(parenthesized);
	this.patternLocator.match((LocalDeclaration) this.astStack[this.astPtr], this.nodeSet);
}

@Override
protected void consumeTypeParameter1WithExtends() {
	super.consumeTypeParameter1WithExtends();
	if ((this.patternFineGrain & IJavaSearchConstants.TYPE_VARIABLE_BOUND_TYPE_REFERENCE) != 0) {
		TypeParameter typeParameter = (TypeParameter) this.genericsStack[this.genericsPtr];
		this.patternLocator.match(typeParameter.type, this.nodeSet);
	}
}

@Override
protected void consumeTypeParameter1WithExtendsAndBounds() {
	super.consumeTypeParameter1WithExtendsAndBounds();
	if ((this.patternFineGrain & IJavaSearchConstants.TYPE_VARIABLE_BOUND_TYPE_REFERENCE) != 0) {
		TypeParameter typeParameter = (TypeParameter) this.genericsStack[this.genericsPtr];
		this.patternLocator.match(typeParameter.type, this.nodeSet);
	}
}

@Override
protected void consumeTypeParameterHeader() {
	super.consumeTypeParameterHeader();
	this.patternLocator.match((TypeParameter)this.genericsStack[this.genericsPtr], this.nodeSet);
}

@Override
protected void consumeTypeParameterWithExtends() {
	super.consumeTypeParameterWithExtends();
	if ((this.patternFineGrain & IJavaSearchConstants.TYPE_VARIABLE_BOUND_TYPE_REFERENCE) != 0) {
		TypeParameter typeParameter = (TypeParameter) this.genericsStack[this.genericsPtr];
		this.patternLocator.match(typeParameter.type, this.nodeSet);
	}
}

@Override
protected void consumeTypePattern() {
	super.consumeTypePattern();
	if (this.patternFineGrain == 0) {
		TypePattern td = (TypePattern) this.astStack[this.astPtr];
		if (td.local != null) {
			this.patternLocator.match(td.local, this.nodeSet);
		}
	}
}

@Override
protected void consumeTypeParameterWithExtendsAndBounds() {
	super.consumeTypeParameterWithExtendsAndBounds();
	if ((this.patternFineGrain & IJavaSearchConstants.TYPE_VARIABLE_BOUND_TYPE_REFERENCE) != 0) {
		TypeParameter typeParameter = (TypeParameter) this.genericsStack[this.genericsPtr];
		this.patternLocator.match(typeParameter.type, this.nodeSet);
	}
}

@Override
protected void consumeUnaryExpression(int op, boolean post) {
	super.consumeUnaryExpression(op, post);
	this.patternLocator.match(this.expressionStack[this.expressionPtr], this.nodeSet);
}

@Override
protected void consumeWildcardBounds1Extends() {
	super.consumeWildcardBounds1Extends();
	if ((this.patternFineGrain & IJavaSearchConstants.WILDCARD_BOUND_TYPE_REFERENCE) != 0) {
		Wildcard wildcard = (Wildcard) this.genericsStack[this.genericsPtr];
		this.patternLocator.match(wildcard.bound, this.nodeSet);
	}
}

@Override
protected void consumeWildcardBounds1Super() {
	super.consumeWildcardBounds1Super();
	if ((this.patternFineGrain & IJavaSearchConstants.WILDCARD_BOUND_TYPE_REFERENCE) != 0) {
		Wildcard wildcard = (Wildcard) this.genericsStack[this.genericsPtr];
		this.patternLocator.match(wildcard.bound, this.nodeSet);
	}
}

@Override
protected void consumeWildcardBounds2Extends() {
	super.consumeWildcardBounds2Extends();
	if ((this.patternFineGrain & IJavaSearchConstants.WILDCARD_BOUND_TYPE_REFERENCE) != 0) {
		Wildcard wildcard = (Wildcard) this.genericsStack[this.genericsPtr];
		this.patternLocator.match(wildcard.bound, this.nodeSet);
	}
}

@Override
protected void consumeWildcardBounds2Super() {
	super.consumeWildcardBounds2Super();
	if ((this.patternFineGrain & IJavaSearchConstants.WILDCARD_BOUND_TYPE_REFERENCE) != 0) {
		Wildcard wildcard = (Wildcard) this.genericsStack[this.genericsPtr];
		this.patternLocator.match(wildcard.bound, this.nodeSet);
	}
}

@Override
protected void consumeWildcardBounds3Extends() {
	super.consumeWildcardBounds3Extends();
	if ((this.patternFineGrain & IJavaSearchConstants.WILDCARD_BOUND_TYPE_REFERENCE) != 0) {
		Wildcard wildcard = (Wildcard) this.genericsStack[this.genericsPtr];
		this.patternLocator.match(wildcard.bound, this.nodeSet);
	}
}

@Override
protected void consumeWildcardBounds3Super() {
	super.consumeWildcardBounds3Super();
	if ((this.patternFineGrain & IJavaSearchConstants.WILDCARD_BOUND_TYPE_REFERENCE) != 0) {
		Wildcard wildcard = (Wildcard) this.genericsStack[this.genericsPtr];
		this.patternLocator.match(wildcard.bound, this.nodeSet);
	}
}

@Override
protected void consumeWildcardBoundsExtends() {
	super.consumeWildcardBoundsExtends();
	if ((this.patternFineGrain & IJavaSearchConstants.WILDCARD_BOUND_TYPE_REFERENCE) != 0) {
		Wildcard wildcard = (Wildcard) this.genericsStack[this.genericsPtr];
		this.patternLocator.match(wildcard.bound, this.nodeSet);
	}
}

@Override
protected void consumeWildcardBoundsSuper() {
	super.consumeWildcardBoundsSuper();
	if ((this.patternFineGrain & IJavaSearchConstants.WILDCARD_BOUND_TYPE_REFERENCE) != 0) {
		Wildcard wildcard = (Wildcard) this.genericsStack[this.genericsPtr];
		this.patternLocator.match(wildcard.bound, this.nodeSet);
	}
}

@Override
protected  void consumeInterfaceHeaderPermittedSubClassesAndSubInterfaces(){
	super.consumeInterfaceHeaderPermittedSubClassesAndSubInterfaces();
	updatePatternLocaterMatch();
}
private void updatePatternLocaterMatch() {
	if ((this.patternFineGrain & IJavaSearchConstants.PERMITTYPE_TYPE_REFERENCE) != 0) {
		TypeDeclaration td = (TypeDeclaration) this.astStack[this.astPtr];
		TypeReference[] permittedTypes = td.permittedTypes;
		for (TypeReference pt : permittedTypes) {
			this.patternLocator.match(pt, this.nodeSet);
		}
	}
}

@Override
protected void consumeClassHeaderPermittedSubclasses() {
	super.consumeClassHeaderPermittedSubclasses();
	updatePatternLocaterMatch();
}

@Override
protected TypeReference augmentTypeWithAdditionalDimensions(TypeReference typeRef, int additionalDimensions, Annotation [][] additionalAnnotations, boolean isVarargs) {
	TypeReference result = super.augmentTypeWithAdditionalDimensions(typeRef, additionalDimensions, additionalAnnotations, isVarargs);
	 if (this.nodeSet.removePossibleMatch(typeRef) != null)
		this.nodeSet.addPossibleMatch(result);
	 else if (this.nodeSet.removeTrustedMatch(typeRef) != null)
		this.nodeSet.addTrustedMatch(result, true);
	return result;
}
@Override
protected TypeReference getTypeReference(int dim) {
	TypeReference typeRef = super.getTypeReference(dim);
	if (this.patternFineGrain == 0) {
		this.patternLocator.match(typeRef, this.nodeSet); // NB: Don't check container since type reference can happen anywhere
	}
	return typeRef;
}
@Override
protected NameReference getUnspecifiedReference(boolean rejectTypeAnnotations) {
	NameReference nameRef = super.getUnspecifiedReference(rejectTypeAnnotations);
	if (this.patternFineGrain == 0) {
		this.patternLocator.match(nameRef, this.nodeSet); // NB: Don't check container since unspecified reference can happen anywhere
	} else if ((this.patternFineGrain & IJavaSearchConstants.QUALIFIED_REFERENCE) != 0) {
		if (nameRef instanceof QualifiedNameReference) {
			this.patternLocator.match(nameRef, this.nodeSet);
		}
	} else if ((this.patternFineGrain & IJavaSearchConstants.IMPLICIT_THIS_REFERENCE) != 0) {
		if (nameRef instanceof SingleNameReference) {
			this.patternLocator.match(nameRef, this.nodeSet);
		}
	}
	return nameRef;
}
@Override
protected NameReference getUnspecifiedReferenceOptimized() {
	NameReference nameRef = super.getUnspecifiedReferenceOptimized();
	if (this.patternFineGrain == 0) {
		this.patternLocator.match(nameRef, this.nodeSet); // NB: Don't check container since unspecified reference can happen anywhere
	} else {
		boolean flagQualifiedRef = (this.patternFineGrain & IJavaSearchConstants.QUALIFIED_REFERENCE) != 0;
		boolean flagImplicitThis = (this.patternFineGrain & IJavaSearchConstants.IMPLICIT_THIS_REFERENCE) != 0;
		if (flagQualifiedRef && flagImplicitThis) {
			this.patternLocator.match(nameRef, this.nodeSet);
		} else if (flagQualifiedRef) {
			if (nameRef instanceof QualifiedNameReference) {
				this.patternLocator.match(nameRef, this.nodeSet);
			}
		} else if (flagImplicitThis) {
			if (nameRef instanceof SingleNameReference) {
				this.patternLocator.match(nameRef, this.nodeSet);
			}
		}
	}
	return nameRef;
}
/**
 * Parses the method bodies in the given compilation unit
 * @param unit CompilationUnitDeclaration
 */
public void parseBodies(CompilationUnitDeclaration unit) {
	TypeDeclaration[] types = unit.types;
	if (types == null) return;

	for (int i = 0; i < types.length; i++) {
		TypeDeclaration type = types[i];
		this.patternLocator.match(type, this.nodeSet);
		this.parseBodies(type, unit);
	}
}
/**
 * Parses the member bodies in the given type.
 * @param type TypeDeclaration
 * @param unit CompilationUnitDeclaration
 */
protected void parseBodies(TypeDeclaration type, CompilationUnitDeclaration unit) {
	FieldDeclaration[] fields = type.fields;
	if (fields != null) {
		for (int i = 0; i < fields.length; i++) {
			FieldDeclaration field = fields[i];
			if (field instanceof Initializer)
				this.parse((Initializer) field, type, unit);
			field.traverse(this.localDeclarationVisitor, null);
		}
	}

	AbstractMethodDeclaration[] methods = type.methods;
	if (methods != null) {
		for (int i = 0; i < methods.length; i++) {
			AbstractMethodDeclaration method = methods[i];
			if (method.sourceStart >= type.bodyStart) { // if not synthetic
				if (method instanceof MethodDeclaration) {
					MethodDeclaration methodDeclaration = (MethodDeclaration) method;
					this.parse(methodDeclaration, unit);
					methodDeclaration.traverse(this.localDeclarationVisitor, (ClassScope) null);
				} else if (method instanceof ConstructorDeclaration) {
					ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) method;
					this.parse(constructorDeclaration, unit, false);
					constructorDeclaration.traverse(this.localDeclarationVisitor, (ClassScope) null);
				}
			} else if (method.isDefaultConstructor()) {
				method.parseStatements(this, unit);
			}
		}
	}

	TypeDeclaration[] memberTypes = type.memberTypes;
	if (memberTypes != null) {
		for (int i = 0; i < memberTypes.length; i++) {
			TypeDeclaration memberType = memberTypes[i];
			this.parseBodies(memberType, unit);
			memberType.traverse(this.localDeclarationVisitor, (ClassScope) null);
		}
	}
}

}

