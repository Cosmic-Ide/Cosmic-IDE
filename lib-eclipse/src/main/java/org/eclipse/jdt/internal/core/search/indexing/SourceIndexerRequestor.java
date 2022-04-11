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
package org.eclipse.jdt.internal.core.search.indexing;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.*;
import org.eclipse.jdt.internal.compiler.ExtraFlags;
import org.eclipse.jdt.internal.compiler.ISourceElementRequestor;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.core.search.processing.JobManager;

/**
 * This class is used by the JavaParserIndexer. When parsing the java file, the requestor
 * recognizes the java elements (methods, fields, ...) and add them to an index.
 */
public class SourceIndexerRequestor implements ISourceElementRequestor, IIndexConstants {
	SourceIndexer indexer;

	char[] packageName = CharOperation.NO_CHAR;
	char[][] enclosingTypeNames = new char[5][];
	int depth = 0;
	int methodDepth = 0;

public SourceIndexerRequestor(SourceIndexer indexer) {
	this.indexer = indexer;
}
/**
 * @see ISourceElementRequestor#acceptAnnotationTypeReference(char[][], int, int)
 */
@Override
public void acceptAnnotationTypeReference(char[][] typeName, int sourceStart, int sourceEnd) {
	int length = typeName.length;
	for (int i = 0; i < length - 1; i++)
		acceptUnknownReference(typeName[i], 0);
	acceptAnnotationTypeReference(typeName[length - 1], 0);
}
/**
 * @see ISourceElementRequestor#acceptAnnotationTypeReference(char[], int)
 */
@Override
public void acceptAnnotationTypeReference(char[] simpleTypeName, int sourcePosition) {
	this.indexer.addAnnotationTypeReference(simpleTypeName);
}
/**
 * @see ISourceElementRequestor#acceptConstructorReference(char[], int, int)
 */
@Override
public void acceptConstructorReference(char[] typeName, int argCount, int sourcePosition) {
	if (CharOperation.indexOf(Signature.C_GENERIC_START, typeName) > 0) {
		typeName = Signature.toCharArray(Signature.getTypeErasure(Signature.createTypeSignature(typeName, false)).toCharArray());
	}
	this.indexer.addConstructorReference(typeName, argCount);
	int lastDot = CharOperation.lastIndexOf('.', typeName);
	if (lastDot != -1) {
		char[][] qualification = CharOperation.splitOn('.', CharOperation.subarray(typeName, 0, lastDot));
		for (int i = 0, length = qualification.length; i < length; i++) {
			this.indexer.addNameReference(qualification[i]);
		}
	}
}
/**
 * @see ISourceElementRequestor#acceptFieldReference(char[], int)
 */
@Override
public void acceptFieldReference(char[] fieldName, int sourcePosition) {
	this.indexer.addFieldReference(fieldName);
}
/**
 * @see ISourceElementRequestor#acceptImport(int, int, int, int, char[][], boolean, int)
 */
@Override
public void acceptImport(int declarationStart, int declarationEnd, int nameStart, int nameEnd, char[][] tokens, boolean onDemand, int modifiers) {
	// imports have already been reported while creating the ImportRef node (see SourceElementParser#comsume*ImportDeclarationName() methods)
}
/**
 * @see ISourceElementRequestor#acceptLineSeparatorPositions(int[])
 */
@Override
public void acceptLineSeparatorPositions(int[] positions) {
	// implements interface method
}
/**
 * @see ISourceElementRequestor#acceptMethodReference(char[], int, int)
 */
@Override
public void acceptMethodReference(char[] methodName, int argCount, int sourcePosition) {
	this.indexer.addMethodReference(methodName, argCount);
}
/**
 * @see ISourceElementRequestor#acceptPackage(ImportReference)
 */
@Override
public void acceptPackage(ImportReference importReference) {
	this.packageName = CharOperation.concatWith(importReference.getImportName(), '.');
}
/**
 * @see ISourceElementRequestor#acceptProblem(CategorizedProblem)
 */
@Override
public void acceptProblem(CategorizedProblem problem) {
	// implements interface method
}
/**
 * @see ISourceElementRequestor#acceptTypeReference(char[][], int, int)
 */
@Override
public void acceptTypeReference(char[][] typeName, int sourceStart, int sourceEnd) {
	int length = typeName.length;
	for (int i = 0; i < length - 1; i++)
		acceptUnknownReference(typeName[i], 0); // ?
	acceptTypeReference(typeName[length - 1], 0);
}
/**
 * @see ISourceElementRequestor#acceptTypeReference(char[], int)
 */
@Override
public void acceptTypeReference(char[] simpleTypeName, int sourcePosition) {
	this.indexer.addTypeReference(simpleTypeName);
}
/**
 * @see ISourceElementRequestor#acceptUnknownReference(char[][], int, int)
 */
@Override
public void acceptUnknownReference(char[][] name, int sourceStart, int sourceEnd) {
	for (int i = 0; i < name.length; i++) {
		acceptUnknownReference(name[i], 0);
	}
}
/**
 * @see ISourceElementRequestor#acceptUnknownReference(char[], int)
 */
@Override
public void acceptUnknownReference(char[] name, int sourcePosition) {
	this.indexer.addNameReference(name);
}

private void addDefaultConstructorIfNecessary(TypeInfo typeInfo) {
	boolean hasConstructor = false;

	TypeDeclaration typeDeclaration = typeInfo.node;
	AbstractMethodDeclaration[] methods = typeDeclaration.methods;
	int methodCounter = methods == null ? 0 : methods.length;
	done : for (int i = 0; i < methodCounter; i++) {
		AbstractMethodDeclaration method = methods[i];
		if (method.isConstructor() && !method.isDefaultConstructor()) {
			hasConstructor = true;
			break done;
		}
	}

	if (!hasConstructor) {
		this.indexer.addDefaultConstructorDeclaration(
				typeInfo.name,
				this.packageName == null ? CharOperation.NO_CHAR : this.packageName,
				typeInfo.modifiers,
				getMoreExtraFlags(typeInfo.extraFlags));
	}
}
/*
 * Rebuild the proper qualification for the current source type:
 *
 * java.lang.Object ---> null
 * java.util.Hashtable$Entry --> [Hashtable]
 * x.y.A$B$C --> [A, B]
 */
public char[][] enclosingTypeNames(){

	if (this.depth == 0) return null;

	char[][] qualification = new char[this.depth][];
	System.arraycopy(this.enclosingTypeNames, 0, qualification, 0, this.depth);
	return qualification;
}
private void enterAnnotationType(TypeInfo typeInfo) {
	char[][] typeNames;
	if (this.methodDepth > 0) {
		typeNames = ONE_ZERO_CHAR;
	} else {
		typeNames = enclosingTypeNames();
	}
	this.indexer.addAnnotationTypeDeclaration(typeInfo.modifiers, this.packageName, typeInfo.name, typeNames, typeInfo.secondary);
	addDefaultConstructorIfNecessary(typeInfo);
	pushTypeName(typeInfo.name);
}

private void enterRecord(TypeInfo typeInfo) {
	enterClass(typeInfo);
	// TODO : Need to handle Compact Constructor here
}

private void enterClass(TypeInfo typeInfo) {

	// eliminate possible qualifications, given they need to be fully resolved again
	if (typeInfo.superclass != null) {
		typeInfo.superclass = getSimpleName(typeInfo.superclass);

		// add implicit constructor reference to default constructor
		this.indexer.addConstructorReference(typeInfo.superclass, 0);
	}
	if (typeInfo.superinterfaces != null){
		for (int i = 0, length = typeInfo.superinterfaces.length; i < length; i++) {
			typeInfo.superinterfaces[i] = getSimpleName(typeInfo.superinterfaces[i]);
		}
	}
	char[][] typeNames;
	if (this.methodDepth > 0) {
		// set specific ['0'] value for local and anonymous to be able to filter them
		typeNames = ONE_ZERO_CHAR;
	} else {
		typeNames = enclosingTypeNames();
	}
	char[][] typeParameterSignatures = null;
	if (typeInfo.typeParameters != null) {
		int typeParametersLength = typeInfo.typeParameters.length;
		typeParameterSignatures = new char[typeParametersLength][];
		for (int i = 0; i < typeParametersLength; i++) {
			ISourceElementRequestor.TypeParameterInfo typeParameterInfo = typeInfo.typeParameters[i];
			typeParameterSignatures[i] = Signature.createTypeParameterSignature(typeParameterInfo.name, typeParameterInfo.bounds == null ? CharOperation.NO_CHAR_CHAR : typeParameterInfo.bounds);
		}
	}
	this.indexer.addClassDeclaration(typeInfo.modifiers, this.packageName, typeInfo.name, typeNames, typeInfo.superclass, typeInfo.superinterfaces, typeParameterSignatures, typeInfo.secondary);
	addDefaultConstructorIfNecessary(typeInfo);
	pushTypeName(typeInfo.name);
}
/**
 * @see ISourceElementRequestor#enterCompilationUnit()
 */
@Override
public void enterCompilationUnit() {
	// implements interface method
}
/**
 * @see ISourceElementRequestor#enterConstructor(ISourceElementRequestor.MethodInfo)
 */
@Override
public void enterConstructor(MethodInfo methodInfo) {
	int argCount = methodInfo.parameterTypes == null ? 0 : methodInfo.parameterTypes.length;
	this.indexer.addConstructorDeclaration(
			methodInfo.name,
			argCount,
			null,
			methodInfo.parameterTypes,
			methodInfo.parameterNames,
			methodInfo.modifiers,
			methodInfo.declaringPackageName,
			methodInfo.declaringTypeModifiers,
			methodInfo.exceptionTypes,
			getMoreExtraFlags(methodInfo.extraFlags));
	this.methodDepth++;
}
private void enterEnum(TypeInfo typeInfo) {
	// eliminate possible qualifications, given they need to be fully resolved again
	if (typeInfo.superinterfaces != null){
		for (int i = 0, length = typeInfo.superinterfaces.length; i < length; i++){
			typeInfo.superinterfaces[i] = getSimpleName(typeInfo.superinterfaces[i]);
		}
	}
	char[][] typeNames;
	if (this.methodDepth > 0) {
		typeNames = ONE_ZERO_CHAR;
	} else {
		typeNames = enclosingTypeNames();
	}
	char[] superclass = typeInfo.superclass == null ? CharOperation.concatWith(TypeConstants.JAVA_LANG_ENUM, '.'): typeInfo.superclass;
	this.indexer.addEnumDeclaration(typeInfo.modifiers, this.packageName, typeInfo.name, typeNames, superclass, typeInfo.superinterfaces, typeInfo.secondary);
	addDefaultConstructorIfNecessary(typeInfo);
	pushTypeName(typeInfo.name);
}
/**
 * @see ISourceElementRequestor#enterField(ISourceElementRequestor.FieldInfo)
 */
@Override
public void enterField(FieldInfo fieldInfo) {
	this.indexer.addFieldDeclaration(fieldInfo.type, fieldInfo.name);
	this.methodDepth++;
}
/**
 * @see ISourceElementRequestor#enterInitializer(int, int)
 */
@Override
public void enterInitializer(int declarationSourceStart, int modifiers) {
	this.methodDepth++;
}
private void enterInterface(TypeInfo typeInfo) {
	// eliminate possible qualifications, given they need to be fully resolved again
	if (typeInfo.superinterfaces != null){
		for (int i = 0, length = typeInfo.superinterfaces.length; i < length; i++){
			typeInfo.superinterfaces[i] = getSimpleName(typeInfo.superinterfaces[i]);
		}
	}
	char[][] typeNames;
	if (this.methodDepth > 0) {
		typeNames = ONE_ZERO_CHAR;
	} else {
		typeNames = enclosingTypeNames();
	}
	char[][] typeParameterSignatures = null;
	if (typeInfo.typeParameters != null) {
		int typeParametersLength = typeInfo.typeParameters.length;
		typeParameterSignatures = new char[typeParametersLength][];
		for (int i = 0; i < typeParametersLength; i++) {
			ISourceElementRequestor.TypeParameterInfo typeParameterInfo = typeInfo.typeParameters[i];
			typeParameterSignatures[i] = Signature.createTypeParameterSignature(typeParameterInfo.name, typeParameterInfo.bounds);
		}
	}
	this.indexer.addInterfaceDeclaration(typeInfo.modifiers, this.packageName, typeInfo.name, typeNames, typeInfo.superinterfaces, typeParameterSignatures, typeInfo.secondary);
	addDefaultConstructorIfNecessary(typeInfo);
	pushTypeName(typeInfo.name);
}

@Override
public void enterModule(ModuleInfo moduleInfo) {
	this.indexer.addModuleDeclaration(moduleInfo.moduleName);
	if (moduleInfo.requires != null) {
		for (ISourceElementRequestor.RequiresInfo req : moduleInfo.requires) {
			if (req == null || req.moduleName == null || req.moduleName == CharOperation.NO_CHAR) continue;
			this.indexer.addModuleReference(req.moduleName);
		}
	}
	enterPackageVisibilityInfo(moduleInfo.exports);
	enterPackageVisibilityInfo(moduleInfo.opens);
	/* note: provides and uses directives processed automatically on IParser (SEParser) */
}
private void enterPackageVisibilityInfo(ISourceElementRequestor.PackageExportInfo[] packInfos) {
	if (packInfos == null)
		return;
	for (ISourceElementRequestor.PackageExportInfo packInfo : packInfos) {
		if (packInfo == null || packInfo.pkgName == null || packInfo.pkgName == CharOperation.NO_CHAR) continue;
		this.indexer.addModuleExportedPackages(packInfo.pkgName);
		char[][] tgts = packInfo.targets;
		if (tgts == null || tgts == CharOperation.NO_CHAR_CHAR) continue;
		for (char[] tgt : tgts) {
			if (tgt != null && tgt != CharOperation.NO_CHAR)
				this.indexer.addModuleReference(tgt);
		}
}
}
/**
 * @see ISourceElementRequestor#enterMethod(ISourceElementRequestor.MethodInfo)
 */
@Override
public void enterMethod(MethodInfo methodInfo) {
	this.indexer.addMethodDeclaration(methodInfo.name, methodInfo.parameterTypes, methodInfo.returnType, methodInfo.exceptionTypes);
	int argCount = methodInfo.parameterTypes == null ? 0 : methodInfo.parameterTypes.length;
	char[] typeName = methodInfo.enclosingType != null ? methodInfo.enclosingType.name : null;
	if (typeName == null || typeName.length == 0)  {
		this.methodDepth++;
		return;
	}
	this.indexer.addMethodDeclaration(
			typeName,
			getDeclaringQualification(methodInfo.enclosingType),
			methodInfo.name,
			argCount,
			null,
			methodInfo.parameterTypes,
			methodInfo.parameterNames,
			methodInfo.returnType,
			methodInfo.modifiers,
			methodInfo.declaringPackageName,
			methodInfo.declaringTypeModifiers,
			methodInfo.exceptionTypes,
			getMoreExtraFlags(methodInfo.extraFlags));
	this.methodDepth++;
}

private static char[] getDeclaringQualification(TypeDeclaration typeDecl) {
	if (typeDecl.name == null) return null;
	TypeDeclaration enclosingType = typeDecl.enclosingType;

	List<char[]> nlist = new ArrayList<>();
	char[] name = null;
	int size = 0;
	while (enclosingType != null && (name = enclosingType.name) != null) {
		nlist.add(0, name);
		size += name.length + 1;
		enclosingType = enclosingType.enclosingType;
	}
	if (name == null) return null;

	int l = nlist.size();
	if (l == 1) return name;

	name = new char[size];
	int index = 0;
	for (int i = 0; i < l - 1; ++i) {
		char[] e = nlist.get(i);
		System.arraycopy(e, 0, name, index, e.length);
		index += e.length;
		name[index++] = '.';
	}
	char[] e = nlist.get(l - 1);
	System.arraycopy(e, 0, name, index, e.length);
	return name;
}
/**
 * @see ISourceElementRequestor#enterType(ISourceElementRequestor.TypeInfo)
 */
@Override
public void enterType(TypeInfo typeInfo) {
	// TODO (jerome) might want to merge the 4 methods
	switch (TypeDeclaration.kind(typeInfo.modifiers)) {
		case TypeDeclaration.CLASS_DECL:
			enterClass(typeInfo);
			break;
		case TypeDeclaration.ANNOTATION_TYPE_DECL:
			enterAnnotationType(typeInfo);
			break;
		case TypeDeclaration.INTERFACE_DECL:
			enterInterface(typeInfo);
			break;
		case TypeDeclaration.ENUM_DECL:
			enterEnum(typeInfo);
			break;
		case TypeDeclaration.RECORD_DECL:
			enterRecord(typeInfo);
			break;
	}
}

/**
 * @see ISourceElementRequestor#exitCompilationUnit(int)
 */
@Override
public void exitCompilationUnit(int declarationEnd) {
	// implements interface method
}
/**
 * @see ISourceElementRequestor#exitConstructor(int)
 */
@Override
public void exitConstructor(int declarationEnd) {
	this.methodDepth--;
}
/**
 * @see ISourceElementRequestor#exitField(int, int, int)
 */
@Override
public void exitField(int initializationStart, int declarationEnd, int declarationSourceEnd) {
	this.methodDepth--;
}
/**
 * @see ISourceElementRequestor#exitRecordComponent(int, int)
 */
@Override
public void exitRecordComponent(int declarationEnd, int declarationSourceEnd) {
	//Nothing by default
}
/**
 * @see ISourceElementRequestor#exitInitializer(int)
 */
@Override
public void exitInitializer(int declarationEnd) {
	this.methodDepth--;
}
/**
 * @see ISourceElementRequestor#exitMethod(int, Expression)
 */
@Override
public void exitMethod(int declarationEnd, Expression defaultValue) {
	this.methodDepth--;
}
/**
 * @see ISourceElementRequestor#exitType(int)
 */
@Override
public void exitType(int declarationEnd) {
	popTypeName();
}
/*
 * Returns the unqualified name without parameters from the given type name.
 */
private char[] getSimpleName(char[] typeName) {
	int lastDot = -1, lastGenericStart = -1;
	int depthCount = 0;
	int length = typeName.length;
	lastDotLookup: for (int i = length -1; i >= 0; i--) {
		switch (typeName[i]) {
			case '.':
				if (depthCount == 0) {
					lastDot = i;
					break lastDotLookup;
				}
				break;
			case '<':
				depthCount--;
				if (depthCount == 0) lastGenericStart = i;
				break;
			case '>':
				depthCount++;
				break;
		}
	}
	if (lastGenericStart < 0) {
		if (lastDot < 0) {
			return typeName;
		}
		return  CharOperation.subarray(typeName, lastDot + 1, length);
	}
	return  CharOperation.subarray(typeName, lastDot + 1, lastGenericStart);
}
private int getMoreExtraFlags(int extraFlags) {
	if (this.methodDepth > 0) {
		extraFlags |= ExtraFlags.IsLocalType;
	}
	return extraFlags;
}
public void popTypeName() {
	if (this.depth > 0) {
		this.enclosingTypeNames[--this.depth] = null;
	} else if (JobManager.VERBOSE) {
		// dump a trace so it can be tracked down
		try {
			this.enclosingTypeNames[-1] = null;
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
	}
}
public void pushTypeName(char[] typeName) {
	if (this.depth == this.enclosingTypeNames.length)
		System.arraycopy(this.enclosingTypeNames, 0, this.enclosingTypeNames = new char[this.depth*2][], 0, this.depth);
	this.enclosingTypeNames[this.depth++] = typeName;
}
}
