/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
package org.eclipse.jdt.internal.codeassist;

import java.util.Map;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnAnnotationOfType;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnArgumentName;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnFieldName;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnFieldType;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnImportReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnKeyword;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnKeyword2;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnMethodName;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnMethodReturnType;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnMethodTypeParameter;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnPackageReference;
import org.eclipse.jdt.internal.compiler.SourceElementNotifier;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObjectToInt;

public class CompletionElementNotifier extends SourceElementNotifier {

	private ASTNode assistNode;

	public CompletionElementNotifier(
			CompletionUnitStructureRequestor requestor,
			boolean reportLocalDeclarations,
			ASTNode assistNode) {
		super(requestor, reportLocalDeclarations);
		this.assistNode = assistNode;
	}

	protected char[][][] getArguments(Argument[] arguments) {
		int argumentLength = arguments.length;
		char[][] argumentTypes = new char[argumentLength][];
		char[][] argumentNames = new char[argumentLength][];
		int argumentCount = 0;
		next : for (int i = 0; i < argumentLength; i++) {
			Argument argument = arguments[i];

			if (argument instanceof CompletionOnArgumentName && argument.name.length == 0) continue next;

			argumentTypes[argumentCount] = CharOperation.concatWith(argument.type.getParameterizedTypeName(), '.');
			argumentNames[argumentCount++] = argument.name;
		}

		if (argumentCount < argumentLength) {
			System.arraycopy(argumentTypes, 0, argumentTypes = new char[argumentCount][], 0, argumentCount);
			System.arraycopy(argumentNames, 0, argumentNames = new char[argumentCount][], 0, argumentCount);
		}

		return new char[][][] {argumentTypes, argumentNames};
	}

	@Override
	protected char[][] getInterfaceNames(TypeDeclaration typeDeclaration) {
		char[][] interfaceNames = null;
		int superInterfacesLength = 0;
		TypeReference[] superInterfaces = typeDeclaration.superInterfaces;
		if (superInterfaces != null) {
			superInterfacesLength = superInterfaces.length;
			interfaceNames = new char[superInterfacesLength][];
		} else {
			if ((typeDeclaration.bits & ASTNode.IsAnonymousType) != 0) {
				// see PR 3442
				QualifiedAllocationExpression alloc = typeDeclaration.allocation;
				if (alloc != null && alloc.type != null) {
					superInterfaces = new TypeReference[] { alloc.type};
					superInterfacesLength = 1;
					interfaceNames = new char[1][];
				}
			}
		}
		if (superInterfaces != null) {
			int superInterfaceCount = 0;
			next: for (int i = 0; i < superInterfacesLength; i++) {
				TypeReference superInterface = superInterfaces[i];

				if (superInterface instanceof CompletionOnKeyword) continue next;
				if (CompletionUnitStructureRequestor.hasEmptyName(superInterface, this.assistNode)) continue next;

				interfaceNames[superInterfaceCount++] = CharOperation.concatWith(superInterface.getParameterizedTypeName(), '.');
			}

			if (superInterfaceCount == 0) return null;
			if (superInterfaceCount < superInterfacesLength) {
				System.arraycopy(interfaceNames, 0, interfaceNames = new char[superInterfaceCount][], 0, superInterfaceCount);
			}
		}
		return interfaceNames;
	}

	@Override
	protected char[] getSuperclassName(TypeDeclaration typeDeclaration) {
		TypeReference superclass = typeDeclaration.superclass;

		if (superclass instanceof CompletionOnKeyword) return null;
		if (CompletionUnitStructureRequestor.hasEmptyName(superclass, this.assistNode)) return null;

		return superclass != null ? CharOperation.concatWith(superclass.getParameterizedTypeName(), '.') : null;
	}

	@Override
	protected char[][] getThrownExceptions(AbstractMethodDeclaration methodDeclaration) {
		char[][] thrownExceptionTypes = null;
		TypeReference[] thrownExceptions = methodDeclaration.thrownExceptions;
		if (thrownExceptions != null) {
			int thrownExceptionLength = thrownExceptions.length;
			int thrownExceptionCount = 0;
			thrownExceptionTypes = new char[thrownExceptionLength][];
			next : for (int i = 0; i < thrownExceptionLength; i++) {
				TypeReference thrownException = thrownExceptions[i];

				if (thrownException instanceof CompletionOnKeyword) continue next;
				if (CompletionUnitStructureRequestor.hasEmptyName(thrownException, this.assistNode)) continue next;

				thrownExceptionTypes[thrownExceptionCount++] =
					CharOperation.concatWith(thrownException.getParameterizedTypeName(), '.');
			}

			if (thrownExceptionCount == 0) return null;
			if (thrownExceptionCount < thrownExceptionLength) {
				System.arraycopy(thrownExceptionTypes, 0, thrownExceptionTypes = new char[thrownExceptionCount][], 0, thrownExceptionCount);
			}
		}
		return thrownExceptionTypes;
	}

	@Override
	protected char[][] getTypeParameterBounds(TypeParameter typeParameter) {
		TypeReference firstBound = typeParameter.type;
		TypeReference[] otherBounds = typeParameter.bounds;
		char[][] typeParameterBounds = null;
		if (firstBound != null) {
			if (otherBounds != null) {
				int otherBoundsLength = otherBounds.length;
				char[][] boundNames = new char[otherBoundsLength+1][];
				int boundCount = 0;
				if (!CompletionUnitStructureRequestor.hasEmptyName(firstBound, this.assistNode)) {
					boundNames[boundCount++] = CharOperation.concatWith(firstBound.getParameterizedTypeName(), '.');
				}
				for (int j = 0; j < otherBoundsLength; j++) {
					TypeReference otherBound = otherBounds[j];
					if (!CompletionUnitStructureRequestor.hasEmptyName(otherBound, this.assistNode)) {
						boundNames[boundCount++] =
							CharOperation.concatWith(otherBound.getParameterizedTypeName(), '.');
					}
				}

				if (boundCount == 0) {
					boundNames = CharOperation.NO_CHAR_CHAR;
				} else if (boundCount < otherBoundsLength + 1){
					System.arraycopy(boundNames, 0, boundNames = new char[boundCount][], 0, boundCount);
				}
				typeParameterBounds = boundNames;
			} else {
				if (!CompletionUnitStructureRequestor.hasEmptyName(firstBound, this.assistNode)) {
					typeParameterBounds = new char[][] { CharOperation.concatWith(firstBound.getParameterizedTypeName(), '.')};
				} else {
					typeParameterBounds = CharOperation.NO_CHAR_CHAR;
				}
			}
		} else {
			typeParameterBounds = CharOperation.NO_CHAR_CHAR;
		}

		return typeParameterBounds;
	}

	@Override
	protected void notifySourceElementRequestor(AbstractMethodDeclaration methodDeclaration, TypeDeclaration declaringType, ImportReference currentPackage) {
		if (methodDeclaration instanceof CompletionOnMethodReturnType) return;
		if (methodDeclaration instanceof CompletionOnMethodTypeParameter) return;
		if (methodDeclaration instanceof CompletionOnMethodName) return;
		super.notifySourceElementRequestor(methodDeclaration, declaringType, currentPackage);
	}

	@Override
	public void notifySourceElementRequestor(CompilationUnitDeclaration parsedUnit, int sourceStart, int sourceEnd, boolean reportReference, HashtableOfObjectToInt sourceEndsMap, Map nodesToCategoriesMap) {
		super.notifySourceElementRequestor(parsedUnit, sourceStart, sourceEnd, reportReference, sourceEndsMap, nodesToCategoriesMap);
	}

	@Override
	protected void notifySourceElementRequestor(FieldDeclaration fieldDeclaration, TypeDeclaration declaringType) {
		if (fieldDeclaration instanceof CompletionOnFieldType) return;
		if (fieldDeclaration instanceof CompletionOnFieldName) return;
		super.notifySourceElementRequestor(fieldDeclaration, declaringType);
	}

	@Override
	protected void notifySourceElementRequestor(ImportReference importReference, boolean isPackage) {
		if (importReference instanceof CompletionOnKeyword2) return;
		if (importReference instanceof CompletionOnImportReference ||
				importReference instanceof CompletionOnPackageReference) {
			if (importReference.tokens[importReference.tokens.length - 1].length == 0) return;
		}

		super.notifySourceElementRequestor(importReference, isPackage);
	}

	@Override
	protected void notifySourceElementRequestor(TypeDeclaration typeDeclaration, boolean notifyTypePresence, TypeDeclaration declaringType, ImportReference currentPackage) {
		if (typeDeclaration instanceof CompletionOnAnnotationOfType) return;
		super.notifySourceElementRequestor(typeDeclaration, notifyTypePresence, declaringType, currentPackage);
	}
}
