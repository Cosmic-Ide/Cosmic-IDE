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

import java.util.HashMap;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.ModuleDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;

/*
 * Part of the source element parser responsible for building the output. It
 * gets notified of structural information as they are detected, relying on the
 * requestor to assemble them together, based on the notifications it got.
 *
 * The structural investigation includes: - package statement - import
 * statements - top-level types: package member, member types (member types of
 * member types...) - fields - methods. From Java 9 onwards it includes the
 * module name in a module declaration
 *
 * If reference information is requested, then all source constructs are
 * investigated and type, field & method references are provided as well.
 *
 * Any (parsing) problem encountered is also provided.
 *
 * All positions are relative to the exact source fed to the parser.
 *
 * Elements which are complex are notified in two steps: - enter <Element> :
 * once the element header has been identified - exit <Element> : once the
 * element has been fully consumed
 *
 * other simpler elements (package, import) are read all at once: - accept
 * <Element>
 */
@SuppressWarnings("rawtypes")
public interface ISourceElementRequestor {

	public static class ModuleInfo {
		public int declarationStart;
		public int modifiers;
		public char[] name;
		public int nameSourceStart;
		public int nameSourceEnd;
		public char[] moduleName;
		public RequiresInfo[] requires;
		public PackageExportInfo[] exports;
		public ServicesInfo[] services;
		public PackageExportInfo[] opens;
		public char[][] usedServices;
		public Annotation[] annotations;
		public ModuleDeclaration node;
		public char[][] categories;
	}
	public static class RequiresInfo {
		public char[] moduleName;
		public int modifiers;
	}
	public static class PackageExportInfo {
		public char[] pkgName;
		public char[][] targets;
	}
	public static class ServicesInfo {
		public char[] serviceName;
		public char[][] implNames;
	}
	public static class TypeInfo {
		public boolean typeAnnotated;
		public int declarationStart;
		public int modifiers;
		public char[] name;
		public int nameSourceStart;
		public int nameSourceEnd;
		public char[] superclass;
		public char[][] superinterfaces;
		public char[][] permittedSubtypes;
		public TypeParameterInfo[] typeParameters;
		public char[][] categories;
		public boolean secondary;
		public boolean anonymousMember;
		public Annotation[] annotations;
		public int extraFlags;
		public TypeDeclaration node;
		public HashMap childrenCategories = new HashMap();
	}

	public static class TypeParameterInfo {
		public boolean typeAnnotated;
		public int declarationStart;
		public int declarationEnd;
		public char[] name;
		public int nameSourceStart;
		public int nameSourceEnd;
		public char[][] bounds;
	}

	public static class MethodInfo {
		public boolean typeAnnotated;
		public boolean isCanonicalConstr;
		public boolean isConstructor;
		public boolean isAnnotation;
		public int declarationStart;
		public int modifiers;
		public char[] returnType;
		public char[] name;
		public int nameSourceStart;
		public int nameSourceEnd;
		public char[][] parameterTypes;
		public char[][] parameterNames;
		public char[][] exceptionTypes;
		public TypeParameterInfo[] typeParameters;
		public char[][] categories;
		public Annotation[] annotations;
		public char[] declaringPackageName;
		public int declaringTypeModifiers;
		public int extraFlags;
		public AbstractMethodDeclaration node;
		public ParameterInfo[] parameterInfos;
		public TypeDeclaration enclosingType;
	}

	public static class ParameterInfo {
		public int modifiers;
		public int declarationStart;
		public int declarationEnd;
		public int nameSourceStart;
		public int nameSourceEnd;
		public char[] name;
	}
	public static class FieldInfo {
		public boolean isRecordComponent;
		public boolean typeAnnotated;
		public int declarationStart;
		public int modifiers;
		public char[] type;
		public char[] name;
		public int nameSourceStart;
		public int nameSourceEnd;
		public char[][] categories;
		public Annotation[] annotations;
		public AbstractVariableDeclaration node;
	}

	void acceptAnnotationTypeReference(char[][] annotation, int sourceStart, int sourceEnd);

	void acceptAnnotationTypeReference(char[] annotation, int sourcePosition);

	void acceptConstructorReference(char[] typeName, int argCount, int sourcePosition);

	void acceptFieldReference(char[] fieldName, int sourcePosition);
	/**
	 * @param declarationStart
	 *                   This is the position of the first character of the import
	 *                   keyword.
	 * @param declarationEnd
	 *                   This is the position of the ';' ending the import statement or
	 *                   the end of the comment following the import.
	 * @param nameStart
	 *                   This is the position of the first character of the import declaration's
	 *                   name.
	 * @param nameEnd
	 *                   This is the position of the last character of the import declaration's
	 *                   name.
	 * @param tokens
	 *                   This are the tokens of the import like specified in the source.
	 * @param onDemand
	 *                   set to true if the import is an import on demand (e.g. import
	 *                   java.io.*). False otherwise.
	 * @param modifiers
	 *                   can be set to static from 1.5 on.
	 */
	void acceptImport(int declarationStart, int declarationEnd, int nameStart, int nameEnd, char[][] tokens, boolean onDemand, int modifiers);

	/*
	 * Table of line separator position. This table is passed once at the end of
	 * the parse action, so as to allow computation of normalized ranges.
	 *
	 * A line separator might corresponds to several characters in the source,
	 *
	 */
	void acceptLineSeparatorPositions(int[] positions);

	void acceptMethodReference(char[] methodName, int argCount, int sourcePosition);

	void acceptPackage(ImportReference importReference);

	void acceptProblem(CategorizedProblem problem);

	void acceptTypeReference(char[][] typeName, int sourceStart, int sourceEnd);

	void acceptTypeReference(char[] typeName, int sourcePosition);

	void acceptUnknownReference(char[][] name, int sourceStart, int sourceEnd);

	void acceptUnknownReference(char[] name, int sourcePosition);

	void enterCompilationUnit();

	void enterConstructor(MethodInfo methodInfo);

	void enterField(FieldInfo fieldInfo);

	void enterInitializer(int declarationStart, int modifiers);

	void enterMethod(MethodInfo methodInfo);

	void enterType(TypeInfo typeInfo);

	void exitCompilationUnit(int declarationEnd);

	void exitConstructor(int declarationEnd);

	/*
	 * initializationStart denotes the source start of the expression used for
	 * initializing the field if any (-1 if no initialization).
	 */
	void exitField(int initializationStart, int declarationEnd, int declarationSourceEnd);

	void exitRecordComponent(int declarationEnd, int declarationSourceEnd);

	void exitInitializer(int declarationEnd);

	void exitMethod(int declarationEnd, Expression defaultValue);

	void exitType(int declarationEnd);

	default void enterModule(ModuleInfo info) {
		// do nothing
	}
	default void exitModule(int declarationEnd) {
		// do nothing
	}
}
