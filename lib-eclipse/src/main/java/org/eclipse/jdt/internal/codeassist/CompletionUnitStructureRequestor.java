/*******************************************************************************
 * Copyright (c) 2008, 2020 IBM Corporation and others.
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

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnMarkerAnnotationName;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnMemberValueName;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnQualifiedNameReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnQualifiedTypeReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnSingleNameReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnSingleTypeReference;
import org.eclipse.jdt.internal.codeassist.impl.AssistAnnotation;
import org.eclipse.jdt.internal.codeassist.impl.AssistImportContainer;
import org.eclipse.jdt.internal.codeassist.impl.AssistImportDeclaration;
import org.eclipse.jdt.internal.codeassist.impl.AssistInitializer;
import org.eclipse.jdt.internal.codeassist.impl.AssistPackageDeclaration;
import org.eclipse.jdt.internal.codeassist.impl.AssistSourceField;
import org.eclipse.jdt.internal.codeassist.impl.AssistSourceMethod;
import org.eclipse.jdt.internal.codeassist.impl.AssistSourceType;
import org.eclipse.jdt.internal.codeassist.impl.AssistTypeParameter;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.core.AnnotatableInfo;
import org.eclipse.jdt.internal.core.Annotation;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.CompilationUnitElementInfo;
import org.eclipse.jdt.internal.core.CompilationUnitStructureRequestor;
import org.eclipse.jdt.internal.core.ImportContainer;
import org.eclipse.jdt.internal.core.ImportDeclaration;
import org.eclipse.jdt.internal.core.Initializer;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.PackageDeclaration;
import org.eclipse.jdt.internal.core.SourceField;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jdt.internal.core.TypeParameter;

@SuppressWarnings({"rawtypes"})
public class CompletionUnitStructureRequestor extends CompilationUnitStructureRequestor {
	private ASTNode assistNode;

	private Map<JavaElement, Binding> bindingCache;
	private Map<Binding, JavaElement> elementCache;
	private Map<ASTNode, JavaElement> elementWithProblemCache;

	public CompletionUnitStructureRequestor(
			ICompilationUnit unit,
			CompilationUnitElementInfo unitInfo,
			Parser parser,
			ASTNode assistNode,
			Map<JavaElement, Binding> bindingCache,
			Map<Binding, JavaElement> elementCache,
			Map<ASTNode, JavaElement> elementWithProblemCache,
			Map newElements) {
		super(unit, unitInfo, newElements);
		this.parser = parser;
		this.assistNode = assistNode;
		this.bindingCache = bindingCache;
		this.elementCache = elementCache;
		this.elementWithProblemCache = elementWithProblemCache;
	}

	@Override
	protected Annotation createAnnotation(JavaElement parent, String name) {
		return new AssistAnnotation(parent, name, this.newElements);
	}

	@Override
	protected SourceField createField(JavaElement parent, FieldInfo fieldInfo) {
		String fieldName = JavaModelManager.getJavaModelManager().intern(new String(fieldInfo.name));
		AssistSourceField field = new AssistSourceField(parent, fieldName, this.bindingCache, this.newElements);
		FieldDeclaration decl = (FieldDeclaration) (fieldInfo.node);
		if (decl.binding != null) {
			this.bindingCache.put(field, decl.binding);
			this.elementCache.put(decl.binding, field);
		} else {
			this.elementWithProblemCache.put(fieldInfo.node, field);
		}
		return field;
	}
	@Override
	protected SourceField createRecordComponent(JavaElement parent, FieldInfo compInfo) {
		String compName = JavaModelManager.getJavaModelManager().intern(new String(compInfo.name));
		SourceField comp = new AssistSourceField(parent, compName, this.bindingCache, this.newElements) {
			@Override
			public boolean isRecordComponent() throws JavaModelException {
				return true;
			}
		};
		FieldDeclaration decl = (FieldDeclaration) (compInfo.node);
		if (decl.binding != null) {
			this.bindingCache.put(comp, decl.binding);
			this.elementCache.put(decl.binding, comp);
		} else {
			this.elementWithProblemCache.put(compInfo.node, comp);
		}
		return comp;
	}

	@Override
	protected ImportContainer createImportContainer(ICompilationUnit parent) {
		return new AssistImportContainer((CompilationUnit)parent, this.newElements);
	}

	@Override
	protected ImportDeclaration createImportDeclaration(ImportContainer parent, String name, boolean onDemand) {
		return new AssistImportDeclaration(parent, name, onDemand, this.newElements);
	}

	@Override
	protected Initializer createInitializer(JavaElement parent) {
		return new AssistInitializer(parent, 1, this.bindingCache, this.newElements);
	}

	@Override
	protected SourceMethod createMethodHandle(JavaElement parent, MethodInfo methodInfo) {
		String selector = JavaModelManager.getJavaModelManager().intern(new String(methodInfo.name));
		String[] parameterTypeSigs = convertTypeNamesToSigs(methodInfo.parameterTypes);
		AssistSourceMethod method = new AssistSourceMethod(parent, selector, parameterTypeSigs, this.bindingCache, this.newElements);
		if (methodInfo.node.binding != null) {
			this.bindingCache.put(method, methodInfo.node.binding);
			this.elementCache.put(methodInfo.node.binding, method);
		} else {
			this.elementWithProblemCache.put(methodInfo.node, method);
		}
		return method;
	}

	@Override
	protected PackageDeclaration createPackageDeclaration(JavaElement parent, String name) {
		return new AssistPackageDeclaration((CompilationUnit) parent, name, this.newElements);
	}

	@Override
	protected SourceType createTypeHandle(JavaElement parent, TypeInfo typeInfo) {
		String nameString= new String(typeInfo.name);
		AssistSourceType type = new AssistSourceType(parent, nameString, this.bindingCache, this.newElements);
		if (typeInfo.node.binding != null) {
			this.bindingCache.put(type, typeInfo.node.binding);
			this.elementCache.put(typeInfo.node.binding, type);
		} else {
			this.elementWithProblemCache.put(typeInfo.node, type);
		}
		return type;
	}

	@Override
	protected TypeParameter createTypeParameter(JavaElement parent, String name) {
		return new AssistTypeParameter(parent, name, this.newElements);
	}

	@Override
	protected IAnnotation acceptAnnotation(
			org.eclipse.jdt.internal.compiler.ast.Annotation annotation,
			AnnotatableInfo parentInfo,
			JavaElement parentHandle) {
		if (annotation instanceof CompletionOnMarkerAnnotationName) {
			if (hasEmptyName(annotation.type, this.assistNode)) {
				super.acceptAnnotation(annotation, null, parentHandle);
				return null;
			}
		}
		return super.acceptAnnotation(annotation, parentInfo, parentHandle);
	}

	@Override
	protected Object getMemberValue(
			org.eclipse.jdt.internal.core.MemberValuePair memberValuePair,
			Expression expression) {
		if (expression instanceof CompletionOnSingleNameReference) {
			CompletionOnSingleNameReference reference = (CompletionOnSingleNameReference) expression;
			if (reference.token.length == 0) return null;
		} else if (expression instanceof CompletionOnQualifiedNameReference) {
			CompletionOnQualifiedNameReference reference = (CompletionOnQualifiedNameReference) expression;
			if (reference.tokens[reference.tokens.length - 1].length == 0) return null;
		}
		return super.getMemberValue(memberValuePair, expression);
	}
	@Override
	protected IMemberValuePair[] getMemberValuePairs(MemberValuePair[] memberValuePairs) {
		int membersLength = memberValuePairs.length;
		int membersCount = 0;
		IMemberValuePair[] members = new IMemberValuePair[membersLength];
		next : for (int j = 0; j < membersLength; j++) {
			if (memberValuePairs[j] instanceof CompletionOnMemberValueName) continue next;

			members[membersCount++] = getMemberValuePair(memberValuePairs[j]);
		}

		if (membersCount > membersLength) {
			System.arraycopy(members, 0, members, 0, membersCount);
		}
		return members;
	}

	protected static boolean hasEmptyName(TypeReference reference, ASTNode assistNode) {
		if (reference == null) return false;

		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=397070
		if (reference != assistNode &&
				reference.sourceStart <= assistNode.sourceStart && assistNode.sourceEnd <= reference.sourceEnd) return false;

		if (reference instanceof CompletionOnSingleTypeReference ||
				reference instanceof CompletionOnQualifiedTypeReference ||
				reference instanceof CompletionOnParameterizedQualifiedTypeReference) {
			char[][] typeName = reference.getTypeName();
			if (typeName[typeName.length - 1].length == 0) return true;
		}
		if (reference instanceof ParameterizedSingleTypeReference) {
			ParameterizedSingleTypeReference parameterizedReference = (ParameterizedSingleTypeReference) reference;
			TypeReference[] typeArguments = parameterizedReference.typeArguments;
			if (typeArguments != null) {
				for (int i = 0; i < typeArguments.length; i++) {
					if (hasEmptyName(typeArguments[i], assistNode)) return true;
				}
			}
		} else if (reference instanceof ParameterizedQualifiedTypeReference) {
			ParameterizedQualifiedTypeReference parameterizedReference = (ParameterizedQualifiedTypeReference) reference;
			TypeReference[][] typeArguments = parameterizedReference.typeArguments;
			if (typeArguments != null) {
				for (int i = 0; i < typeArguments.length; i++) {
					if (typeArguments[i] != null) {
						for (int j = 0; j < typeArguments[i].length; j++) {
							if (hasEmptyName(typeArguments[i][j], assistNode)) return true;
						}
					}
				}
			}
		}
		return false;
	}
}
