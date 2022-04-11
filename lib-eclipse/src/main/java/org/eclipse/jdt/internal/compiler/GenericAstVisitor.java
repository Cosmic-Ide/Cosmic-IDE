/*******************************************************************************
 * Copyright (c) 2021 GK Software SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler;

import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;

public abstract class GenericAstVisitor extends ASTVisitor {

	// override this to handle all nodes:
	protected abstract boolean visitNode(ASTNode node);

	@Override
	public boolean visit(AllocationExpression allocationExpression, BlockScope scope) {
		return visitNode(allocationExpression);
	}

	@Override
	public boolean visit(AND_AND_Expression and_and_Expression, BlockScope scope) {
		return visitNode(and_and_Expression);
	}

	@Override
	public boolean visit(AnnotationMethodDeclaration annotationTypeDeclaration, ClassScope classScope) {
		return visitNode(annotationTypeDeclaration);
	}

	@Override
	public boolean visit(Argument argument, BlockScope scope) {
		return visitNode(argument);
	}

	@Override
	public boolean visit(Argument argument, ClassScope scope) {
		return visitNode(argument);
	}

	@Override
	public boolean visit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
		return visitNode(arrayAllocationExpression);
	}

	@Override
	public boolean visit(ArrayInitializer arrayInitializer, BlockScope scope) {
		return visitNode(arrayInitializer);
	}

	@Override
	public boolean visit(ArrayInitializer arrayInitializer, ClassScope scope) {
		return visitNode(arrayInitializer);
	}

	@Override
	public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, BlockScope scope) {
		return visitNode(arrayQualifiedTypeReference);
	}

	@Override
	public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, ClassScope scope) {
		return visitNode(arrayQualifiedTypeReference);
	}

	@Override
	public boolean visit(ArrayReference arrayReference, BlockScope scope) {
		return visitNode(arrayReference);
	}

	@Override
	public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
		return visitNode(arrayTypeReference);
	}

	@Override
	public boolean visit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
		return visitNode(arrayTypeReference);
	}

	@Override
	public boolean visit(AssertStatement assertStatement, BlockScope scope) {
		return visitNode(assertStatement);
	}

	@Override
	public boolean visit(Assignment assignment, BlockScope scope) {
		return visitNode(assignment);
	}

	@Override
	public boolean visit(BinaryExpression binaryExpression, BlockScope scope) {
		return visitNode(binaryExpression);
	}

	@Override
	public boolean visit(Block block, BlockScope scope) {
		return visitNode(block);
	}

	@Override
	public boolean visit(BreakStatement breakStatement, BlockScope scope) {
		return visitNode(breakStatement);
	}

	@Override
	public boolean visit(CaseStatement caseStatement, BlockScope scope) {
		return visitNode(caseStatement);
	}

	@Override
	public boolean visit(CastExpression castExpression, BlockScope scope) {
		return visitNode(castExpression);
	}

	@Override
	public boolean visit(CharLiteral charLiteral, BlockScope scope) {
		return visitNode(charLiteral);
	}

	@Override
	public boolean visit(ClassLiteralAccess classLiteral, BlockScope scope) {
		return visitNode(classLiteral);
	}

	@Override
	public boolean visit(Clinit clinit, ClassScope scope) {
		return visitNode(clinit);
	}

	@Override
	public boolean visit(ModuleDeclaration module, CompilationUnitScope scope) {
		return visitNode(module);
	}

	@Override
	public boolean visit(CompactConstructorDeclaration ccd, ClassScope scope) {
		return visitNode(ccd);
	}

	@Override
	public boolean visit(CompilationUnitDeclaration compilationUnitDeclaration, CompilationUnitScope scope) {
		return visitNode(compilationUnitDeclaration);
	}

	@Override
	public boolean visit(CompoundAssignment compoundAssignment, BlockScope scope) {
		return visitNode(compoundAssignment);
	}

	@Override
	public boolean visit(ConditionalExpression conditionalExpression, BlockScope scope) {
		return visitNode(conditionalExpression);
	}

	@Override
	public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
		return visitNode(constructorDeclaration);
	}

	@Override
	public boolean visit(ContinueStatement continueStatement, BlockScope scope) {
		return visitNode(continueStatement);
	}

	@Override
	public boolean visit(DoStatement doStatement, BlockScope scope) {
		return visitNode(doStatement);
	}

	@Override
	public boolean visit(DoubleLiteral doubleLiteral, BlockScope scope) {
		return visitNode(doubleLiteral);
	}

	@Override
	public boolean visit(EmptyStatement emptyStatement, BlockScope scope) {
		return visitNode(emptyStatement);
	}

	@Override
	public boolean visit(EqualExpression equalExpression, BlockScope scope) {
		return visitNode(equalExpression);
	}

	@Override
	public boolean visit(ExplicitConstructorCall explicitConstructor, BlockScope scope) {
		return visitNode(explicitConstructor);
	}

	@Override
	public boolean visit(ExtendedStringLiteral extendedStringLiteral, BlockScope scope) {
		return visitNode(extendedStringLiteral);
	}

	@Override
	public boolean visit(FalseLiteral falseLiteral, BlockScope scope) {
		return visitNode(falseLiteral);
	}

	@Override
	public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
		return visitNode(fieldDeclaration);
	}

	@Override
	public boolean visit(FieldReference fieldReference, BlockScope scope) {
		return visitNode(fieldReference);
	}

	@Override
	public boolean visit(FieldReference fieldReference, ClassScope scope) {
		return visitNode(fieldReference);
	}

	@Override
	public boolean visit(FloatLiteral floatLiteral, BlockScope scope) {
		return visitNode(floatLiteral);
	}

	@Override
	public boolean visit(ForeachStatement forStatement, BlockScope scope) {
		return visitNode(forStatement);
	}

	@Override
	public boolean visit(ForStatement forStatement, BlockScope scope) {
		return visitNode(forStatement);
	}

	@Override
	public boolean visit(IfStatement ifStatement, BlockScope scope) {
		return visitNode(ifStatement);
	}

	@Override
	public boolean visit(ImportReference importRef, CompilationUnitScope scope) {
		return visitNode(importRef);
	}

	@Override
	public boolean visit(Initializer initializer, MethodScope scope) {
		return visitNode(initializer);
	}

	@Override
	public boolean visit(InstanceOfExpression instanceOfExpression, BlockScope scope) {
		return visitNode(instanceOfExpression);
	}

	@Override
	public boolean visit(IntLiteral intLiteral, BlockScope scope) {
		return visitNode(intLiteral);
	}

	@Override
	public boolean visit(Javadoc javadoc, BlockScope scope) {
		return visitNode(javadoc);
	}

	@Override
	public boolean visit(Javadoc javadoc, ClassScope scope) {
		return visitNode(javadoc);
	}

	@Override
	public boolean visit(JavadocAllocationExpression expression, BlockScope scope) {
		return visitNode(expression);
	}

	@Override
	public boolean visit(JavadocAllocationExpression expression, ClassScope scope) {
		return visitNode(expression);
	}

	@Override
	public boolean visit(JavadocArgumentExpression expression, BlockScope scope) {
		return visitNode(expression);
	}

	@Override
	public boolean visit(JavadocArgumentExpression expression, ClassScope scope) {
		return visitNode(expression);
	}

	@Override
	public boolean visit(JavadocArrayQualifiedTypeReference typeRef, BlockScope scope) {
		return visitNode(typeRef);
	}

	@Override
	public boolean visit(JavadocArrayQualifiedTypeReference typeRef, ClassScope scope) {
		return visitNode(typeRef);
	}

	@Override
	public boolean visit(JavadocArraySingleTypeReference typeRef, BlockScope scope) {
		return visitNode(typeRef);
	}

	@Override
	public boolean visit(JavadocArraySingleTypeReference typeRef, ClassScope scope) {
		return visitNode(typeRef);
	}

	@Override
	public boolean visit(JavadocFieldReference fieldRef, BlockScope scope) {
		return visitNode(fieldRef);
	}

	@Override
	public boolean visit(JavadocFieldReference fieldRef, ClassScope scope) {
		return visitNode(fieldRef);
	}

	@Override
	public boolean visit(JavadocImplicitTypeReference implicitTypeReference, BlockScope scope) {
		return visitNode(implicitTypeReference);
	}

	@Override
	public boolean visit(JavadocImplicitTypeReference implicitTypeReference, ClassScope scope) {
		return visitNode(implicitTypeReference);
	}

	@Override
	public boolean visit(JavadocMessageSend messageSend, BlockScope scope) {
		return visitNode(messageSend);
	}

	@Override
	public boolean visit(JavadocMessageSend messageSend, ClassScope scope) {
		return visitNode(messageSend);
	}

	@Override
	public boolean visit(JavadocQualifiedTypeReference typeRef, BlockScope scope) {
		return visitNode(typeRef);
	}

	@Override
	public boolean visit(JavadocQualifiedTypeReference typeRef, ClassScope scope) {
		return visitNode(typeRef);
	}

	@Override
	public boolean visit(JavadocModuleReference moduleRef, BlockScope scope) {
		return visitNode(moduleRef);
	}

	@Override
	public boolean visit(JavadocModuleReference moduleRef, ClassScope scope) {
		return visitNode(moduleRef);
	}

	@Override
	public boolean visit(JavadocReturnStatement statement, BlockScope scope) {
		return visitNode(statement);
	}

	@Override
	public boolean visit(JavadocReturnStatement statement, ClassScope scope) {
		return visitNode(statement);
	}

	@Override
	public boolean visit(JavadocSingleNameReference argument, BlockScope scope) {
		return visitNode(argument);
	}

	@Override
	public boolean visit(JavadocSingleNameReference argument, ClassScope scope) {
		return visitNode(argument);
	}

	@Override
	public boolean visit(JavadocSingleTypeReference typeRef, BlockScope scope) {
		return visitNode(typeRef);
	}

	@Override
	public boolean visit(JavadocSingleTypeReference typeRef, ClassScope scope) {
		return visitNode(typeRef);
	}

	@Override
	public boolean visit(LabeledStatement labeledStatement, BlockScope scope) {
		return visitNode(labeledStatement);
	}

	@Override
	public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
		return visitNode(localDeclaration);
	}

	@Override
	public boolean visit(LongLiteral longLiteral, BlockScope scope) {
		return visitNode(longLiteral);
	}

	@Override
	public boolean visit(MarkerAnnotation annotation, BlockScope scope) {
		return visitNode(annotation);
	}

	@Override
	public boolean visit(MarkerAnnotation annotation, ClassScope scope) {
		return visitNode(annotation);
	}

	@Override
	public boolean visit(MemberValuePair pair, BlockScope scope) {
		return visitNode(pair);
	}

	@Override
	public boolean visit(MemberValuePair pair, ClassScope scope) {
		return visitNode(pair);
	}

	@Override
	public boolean visit(MessageSend messageSend, BlockScope scope) {
		return visitNode(messageSend);
	}

	@Override
	public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
		return visitNode(methodDeclaration);
	}

	@Override
	public boolean visit(StringLiteralConcatenation literal, BlockScope scope) {
		return visitNode(literal);
	}

	@Override
	public boolean visit(NormalAnnotation annotation, BlockScope scope) {
		return visitNode(annotation);
	}

	@Override
	public boolean visit(NormalAnnotation annotation, ClassScope scope) {
		return visitNode(annotation);
	}

	@Override
	public boolean visit(NullLiteral nullLiteral, BlockScope scope) {
		return visitNode(nullLiteral);
	}

	@Override
	public boolean visit(OR_OR_Expression or_or_Expression, BlockScope scope) {
		return visitNode(or_or_Expression);
	}

	@Override
	public boolean visit(ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference, BlockScope scope) {
		return visitNode(parameterizedQualifiedTypeReference);
	}

	@Override
	public boolean visit(ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference, ClassScope scope) {
		return visitNode(parameterizedQualifiedTypeReference);
	}

	@Override
	public boolean visit(ParameterizedSingleTypeReference parameterizedSingleTypeReference, BlockScope scope) {
		return visitNode(parameterizedSingleTypeReference);
	}

	@Override
	public boolean visit(ParameterizedSingleTypeReference parameterizedSingleTypeReference, ClassScope scope) {
		return visitNode(parameterizedSingleTypeReference);
	}

	@Override
	public boolean visit(PostfixExpression postfixExpression, BlockScope scope) {
		return visitNode(postfixExpression);
	}

	@Override
	public boolean visit(PrefixExpression prefixExpression, BlockScope scope) {
		return visitNode(prefixExpression);
	}

	@Override
	public boolean visit(QualifiedAllocationExpression qualifiedAllocationExpression, BlockScope scope) {
		return visitNode(qualifiedAllocationExpression);
	}

	@Override
	public boolean visit(QualifiedNameReference qualifiedNameReference, BlockScope scope) {
		return visitNode(qualifiedNameReference);
	}

	@Override
	public boolean visit(QualifiedNameReference qualifiedNameReference, ClassScope scope) {
		return visitNode(qualifiedNameReference);
	}

	@Override
	public boolean visit(QualifiedSuperReference qualifiedSuperReference, BlockScope scope) {
		return visitNode(qualifiedSuperReference);
	}

	@Override
	public boolean visit(QualifiedSuperReference qualifiedSuperReference, ClassScope scope) {
		return visitNode(qualifiedSuperReference);
	}

	@Override
	public boolean visit(QualifiedThisReference qualifiedThisReference, BlockScope scope) {
		return visitNode(qualifiedThisReference);
	}

	@Override
	public boolean visit(QualifiedThisReference qualifiedThisReference, ClassScope scope) {
		return visitNode(qualifiedThisReference);
	}

	@Override
	public boolean visit(QualifiedTypeReference qualifiedTypeReference, BlockScope scope) {
		return visitNode(qualifiedTypeReference);
	}

	@Override
	public boolean visit(QualifiedTypeReference qualifiedTypeReference, ClassScope scope) {
		return visitNode(qualifiedTypeReference);
	}

	@Override
	public boolean visit(ReturnStatement returnStatement, BlockScope scope) {
		return visitNode(returnStatement);
	}

	@Override
	public boolean visit(SingleMemberAnnotation annotation, BlockScope scope) {
		return visitNode(annotation);
	}

	@Override
	public boolean visit(SingleMemberAnnotation annotation, ClassScope scope) {
		return visitNode(annotation);
	}

	@Override
	public boolean visit(SingleNameReference singleNameReference, BlockScope scope) {
		return visitNode(singleNameReference);
	}

	@Override
	public boolean visit(SingleNameReference singleNameReference, ClassScope scope) {
		return visitNode(singleNameReference);
	}

	@Override
	public boolean visit(SingleTypeReference singleTypeReference, BlockScope scope) {
		return visitNode(singleTypeReference);
	}

	@Override
	public boolean visit(SingleTypeReference singleTypeReference, ClassScope scope) {
		return visitNode(singleTypeReference);
	}

	@Override
	public boolean visit(StringLiteral stringLiteral, BlockScope scope) {
		return visitNode(stringLiteral);
	}

	@Override
	public boolean visit(SuperReference superReference, BlockScope scope) {
		return visitNode(superReference);
	}

	@Override
	public boolean visit(SwitchStatement switchStatement, BlockScope scope) {
		return visitNode(switchStatement);
	}

	@Override
	public boolean visit(SynchronizedStatement synchronizedStatement, BlockScope scope) {
		return visitNode(synchronizedStatement);
	}

	@Override
	public boolean visit(ThisReference thisReference, BlockScope scope) {
		return visitNode(thisReference);
	}

	@Override
	public boolean visit(ThisReference thisReference, ClassScope scope) {
		return visitNode(thisReference);
	}

	@Override
	public boolean visit(ThrowStatement throwStatement, BlockScope scope) {
		return visitNode(throwStatement);
	}

	@Override
	public boolean visit(TrueLiteral trueLiteral, BlockScope scope) {
		return visitNode(trueLiteral);
	}

	@Override
	public boolean visit(TryStatement tryStatement, BlockScope scope) {
		return visitNode(tryStatement);
	}

	@Override
	public boolean visit(TypeDeclaration localTypeDeclaration, BlockScope scope) {
		return visitNode(localTypeDeclaration);
	}

	@Override
	public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
		return visitNode(memberTypeDeclaration);
	}

	@Override
	public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
		return visitNode(typeDeclaration);
	}

	@Override
	public boolean visit(TypeParameter typeParameter, BlockScope scope) {
		return visitNode(typeParameter);
	}

	@Override
	public boolean visit(TypeParameter typeParameter, ClassScope scope) {
		return visitNode(typeParameter);
	}

	@Override
	public boolean visit(UnaryExpression unaryExpression, BlockScope scope) {
		return visitNode(unaryExpression);
	}

	@Override
	public boolean visit(UnionTypeReference unionTypeReference, BlockScope scope) {
		return visitNode(unionTypeReference);
	}

	@Override
	public boolean visit(UnionTypeReference unionTypeReference, ClassScope scope) {
		return visitNode(unionTypeReference);
	}

	@Override
	public boolean visit(YieldStatement yieldStatement, BlockScope scope) {
		return visitNode(yieldStatement);
	}

	@Override
	public boolean visit(WhileStatement whileStatement, BlockScope scope) {
		return visitNode(whileStatement);
	}

	@Override
	public boolean visit(Wildcard wildcard, BlockScope scope) {
		return visitNode(wildcard);
	}

	@Override
	public boolean visit(Wildcard wildcard, ClassScope scope) {
		return visitNode(wildcard);
	}

	@Override
	public boolean visit(LambdaExpression lambdaExpression, BlockScope blockScope) {
		return visitNode(lambdaExpression);
	}

	@Override
	public boolean visit(ReferenceExpression referenceExpression, BlockScope blockScope) {
		return visitNode(referenceExpression);
	}

	@Override
	public boolean visit(IntersectionCastTypeReference intersectionCastTypeReference, ClassScope scope) {
		return visitNode(intersectionCastTypeReference);
	}

	@Override
	public boolean visit(IntersectionCastTypeReference intersectionCastTypeReference, BlockScope scope) {
		return visitNode(intersectionCastTypeReference);
	}

	@Override
	public boolean visit(SwitchExpression switchExpression, BlockScope blockScope) {
		return visitNode(switchExpression);
	}

	@Override
	public boolean visit(RecordComponent recordComponent, BlockScope scope) {
		return visitNode(recordComponent);
	}
}
