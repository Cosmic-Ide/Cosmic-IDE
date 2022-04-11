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
package org.eclipse.jdt.internal.compiler;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ISourceElementRequestor.ParameterInfo;
import org.eclipse.jdt.internal.compiler.ISourceElementRequestor.TypeParameterInfo;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.AnnotationMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.ArrayReference;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ExplicitConstructorCall;
import org.eclipse.jdt.internal.compiler.ast.ExportsStatement;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ModuleDeclaration;
import org.eclipse.jdt.internal.compiler.ast.OpensStatement;
import org.eclipse.jdt.internal.compiler.ast.QualifiedAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObjectToInt;

@SuppressWarnings({"rawtypes", "unchecked"})
public class SourceElementNotifier {
	/**
	 * An ast visitor that visits local type declarations.
	 */
	public class LocalDeclarationVisitor extends ASTVisitor {
		public ImportReference currentPackage;
		ArrayList declaringTypes;
		public void pushDeclaringType(TypeDeclaration declaringType) {
			if (this.declaringTypes == null) {
				this.declaringTypes = new ArrayList();
			}
			this.declaringTypes.add(declaringType);
		}
		public void popDeclaringType() {
			this.declaringTypes.remove(this.declaringTypes.size()-1);
		}
		public TypeDeclaration peekDeclaringType() {
			if (this.declaringTypes == null) return null;
			int size = this.declaringTypes.size();
			if (size == 0) return null;
			return (TypeDeclaration) this.declaringTypes.get(size-1);
		}
		@Override
		public boolean visit(TypeDeclaration typeDeclaration, BlockScope scope) {
			notifySourceElementRequestor(typeDeclaration, true, peekDeclaringType(), this.currentPackage);
			return false; // don't visit members as this was done during notifySourceElementRequestor(...)
		}
		@Override
		public boolean visit(TypeDeclaration typeDeclaration, ClassScope scope) {
			notifySourceElementRequestor(typeDeclaration, true, peekDeclaringType(), this.currentPackage);
			return false; // don't visit members as this was done during notifySourceElementRequestor(...)
		}
	}

	ISourceElementRequestor requestor;
	boolean reportReferenceInfo;
	char[][] typeNames;
	char[][] superTypeNames;
	int nestedTypeIndex;
	LocalDeclarationVisitor localDeclarationVisitor = null;

	HashtableOfObjectToInt sourceEnds;
	Map<ASTNode,char[][]> nodesToCategories;

	int initialPosition;
	int eofPosition;

public SourceElementNotifier(ISourceElementRequestor requestor, boolean reportLocalDeclarations) {
	this.requestor = requestor;
	if (reportLocalDeclarations) {
		this.localDeclarationVisitor = new LocalDeclarationVisitor();
	}
	this.typeNames = new char[4][];
	this.superTypeNames = new char[4][];
	this.nestedTypeIndex = 0;
}
protected Object[][] getArgumentInfos(Argument[] arguments) {
	int argumentLength = arguments.length;
	char[][] argumentTypes = new char[argumentLength][];
	char[][] argumentNames = new char[argumentLength][];
	ParameterInfo[] parameterInfos = new ParameterInfo[argumentLength];
	for (int i = 0; i < argumentLength; i++) {
		Argument argument = arguments[i];
		argumentTypes[i] = CharOperation.concatWith(argument.type.getParameterizedTypeName(), '.');
		char[] name = argument.name;
		argumentNames[i] = name;
		ParameterInfo parameterInfo = new ParameterInfo();
		parameterInfo.declarationStart = argument.declarationSourceStart;
		parameterInfo.declarationEnd = argument.declarationSourceEnd;
		parameterInfo.nameSourceStart = argument.sourceStart;
		parameterInfo.nameSourceEnd = argument.sourceEnd;
		parameterInfo.modifiers = argument.modifiers;
		parameterInfo.name = name;
		parameterInfos[i] = parameterInfo;
	}

	return new Object[][] { parameterInfos, new char[][][] { argumentTypes, argumentNames } };
}
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
		for (int i = 0; i < superInterfacesLength; i++) {
			interfaceNames[i] =
				CharOperation.concatWith(superInterfaces[i].getParameterizedTypeName(), '.');
		}
	}
	return interfaceNames;
}
protected char[] getSuperclassName(TypeDeclaration typeDeclaration) {
	TypeReference superclass = typeDeclaration.superclass;
	return superclass != null ? CharOperation.concatWith(superclass.getParameterizedTypeName(), '.') : null;
}
protected char[][] getPermittedSubTypes(TypeDeclaration typeDeclaration) {
	return extractTypeReferences(typeDeclaration.permittedTypes);
}
protected char[][] getThrownExceptions(AbstractMethodDeclaration methodDeclaration) {
	return extractTypeReferences(methodDeclaration.thrownExceptions);
}
private char[][] extractTypeReferences(TypeReference[] thrownExceptions) {
	char[][] names = null;
	if (thrownExceptions != null) {
		int thrownExceptionLength = thrownExceptions.length;
		names = new char[thrownExceptionLength][];
		for (int i = 0; i < thrownExceptionLength; i++) {
			names[i] =
				CharOperation.concatWith(thrownExceptions[i].getParameterizedTypeName(), '.');
		}
	}
	return names;
}
protected char[][] getTypeParameterBounds(TypeParameter typeParameter) {
	TypeReference firstBound = typeParameter.type;
	TypeReference[] otherBounds = typeParameter.bounds;
	char[][] typeParameterBounds = null;
	if (firstBound != null) {
		if (otherBounds != null) {
			int otherBoundsLength = otherBounds.length;
			char[][] boundNames = new char[otherBoundsLength+1][];
			boundNames[0] = CharOperation.concatWith(firstBound.getParameterizedTypeName(), '.');
			for (int j = 0; j < otherBoundsLength; j++) {
				boundNames[j+1] =
					CharOperation.concatWith(otherBounds[j].getParameterizedTypeName(), '.');
			}
			typeParameterBounds = boundNames;
		} else {
			typeParameterBounds = new char[][] { CharOperation.concatWith(firstBound.getParameterizedTypeName(), '.')};
		}
	} else {
		typeParameterBounds = CharOperation.NO_CHAR_CHAR;
	}

	return typeParameterBounds;
}
private TypeParameterInfo[] getTypeParameterInfos(TypeParameter[] typeParameters) {
	if (typeParameters == null) return null;
	int typeParametersLength = typeParameters.length;
	TypeParameterInfo[] result = new TypeParameterInfo[typeParametersLength];
	for (int i = 0; i < typeParametersLength; i++) {
		TypeParameter typeParameter = typeParameters[i];
		char[][] typeParameterBounds = getTypeParameterBounds(typeParameter);
		ISourceElementRequestor.TypeParameterInfo typeParameterInfo = new ISourceElementRequestor.TypeParameterInfo();
		typeParameterInfo.typeAnnotated = ((typeParameter.bits & ASTNode.HasTypeAnnotations) != 0);
		typeParameterInfo.declarationStart = typeParameter.declarationSourceStart;
		typeParameterInfo.declarationEnd = typeParameter.declarationSourceEnd;
		typeParameterInfo.name = typeParameter.name;
		typeParameterInfo.nameSourceStart = typeParameter.sourceStart;
		typeParameterInfo.nameSourceEnd = typeParameter.sourceEnd;
		typeParameterInfo.bounds = typeParameterBounds;
		result[i] = typeParameterInfo;
	}
	return result;
}
/*
 * Checks whether one of the annotations is the @Deprecated annotation
 * (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=89807)
 */
private boolean hasDeprecatedAnnotation(Annotation[] annotations) {
	if (annotations != null) {
		for (int i = 0, length = annotations.length; i < length; i++) {
			Annotation annotation = annotations[i];
			if (CharOperation.equals(annotation.type.getLastToken(), TypeConstants.JAVA_LANG_DEPRECATED[2])) {
				return true;
			}
		}
	}
	return false;
}
/*
 * Update the bodyStart of the corresponding parse node
 */
protected void notifySourceElementRequestor(AbstractMethodDeclaration methodDeclaration, TypeDeclaration declaringType, ImportReference currentPackage) {

	// range check
	boolean isInRange =
				this.initialPosition <= methodDeclaration.declarationSourceStart
				&& this.eofPosition >= methodDeclaration.declarationSourceEnd;

	if (methodDeclaration.isClinit()) {
		this.visitIfNeeded(methodDeclaration);
		return;
	}
	if ((methodDeclaration.bits & org.eclipse.jdt.internal.compiler.ast.ASTNode.IsImplicit) != 0)
		return;

	if (methodDeclaration.isDefaultConstructor()) {
		if (this.reportReferenceInfo) {
			ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) methodDeclaration;
			ExplicitConstructorCall constructorCall = constructorDeclaration.constructorCall;
			if (constructorCall != null) {
				switch(constructorCall.accessMode) {
					case ExplicitConstructorCall.This :
						this.requestor.acceptConstructorReference(
							this.typeNames[this.nestedTypeIndex-1],
							constructorCall.arguments == null ? 0 : constructorCall.arguments.length,
							constructorCall.sourceStart);
						break;
					case ExplicitConstructorCall.Super :
					case ExplicitConstructorCall.ImplicitSuper :
						this.requestor.acceptConstructorReference(
							this.superTypeNames[this.nestedTypeIndex-1],
							constructorCall.arguments == null ? 0 : constructorCall.arguments.length,
							constructorCall.sourceStart);
						break;
				}
			}
		}
		return;
	}
	char[][] argumentTypes = null;
	char[][] argumentNames = null;
	boolean isVarArgs = false;
	Argument[] arguments = methodDeclaration.arguments;
	ParameterInfo[] parameterInfos = null;
	ISourceElementRequestor.MethodInfo methodInfo = new ISourceElementRequestor.MethodInfo();
	methodInfo.typeAnnotated = ((methodDeclaration.bits & ASTNode.HasTypeAnnotations) != 0);

	if (arguments != null && arguments.length > 0) {
		Object[][] argumentInfos = getArgumentInfos(arguments);
		parameterInfos = (ParameterInfo[]) argumentInfos[0];
		argumentTypes = (char[][]) argumentInfos[1][0];
		argumentNames = (char[][]) argumentInfos[1][1];

		isVarArgs = arguments[arguments.length-1].isVarArgs();
	}
	char[][] thrownExceptionTypes = getThrownExceptions(methodDeclaration);
	// by default no selector end position
	int selectorSourceEnd = -1;
	if (methodDeclaration.isConstructor()) {
		selectorSourceEnd = this.sourceEnds.get(methodDeclaration);
		if (isInRange){
			int currentModifiers = methodDeclaration.modifiers;
			currentModifiers &= ExtraCompilerModifiers.AccJustFlag | ClassFileConstants.AccDeprecated;
			if (isVarArgs)
				currentModifiers |= ClassFileConstants.AccVarargs;
			if (hasDeprecatedAnnotation(methodDeclaration.annotations))
				currentModifiers |= ClassFileConstants.AccDeprecated;

			methodInfo.isConstructor = true;
			methodInfo.isCanonicalConstr = methodDeclaration.isCanonicalConstructor();
			methodInfo.declarationStart = methodDeclaration.declarationSourceStart;
			methodInfo.modifiers = currentModifiers;
			methodInfo.name = methodDeclaration.selector;
			methodInfo.nameSourceStart = methodDeclaration.sourceStart;
			methodInfo.nameSourceEnd = selectorSourceEnd;
			methodInfo.parameterTypes = argumentTypes;
			methodInfo.parameterNames = argumentNames;
			methodInfo.exceptionTypes = thrownExceptionTypes;
			methodInfo.typeParameters = getTypeParameterInfos(methodDeclaration.typeParameters());
			methodInfo.parameterInfos = parameterInfos;
			methodInfo.categories = this.nodesToCategories.get(methodDeclaration);
			methodInfo.annotations = methodDeclaration.annotations;
			methodInfo.declaringPackageName = currentPackage == null ? CharOperation.NO_CHAR : CharOperation.concatWith(currentPackage.tokens, '.');
			methodInfo.declaringTypeModifiers = declaringType.modifiers;
			methodInfo.extraFlags = ExtraFlags.getExtraFlags(declaringType);
			methodInfo.node = methodDeclaration;
			this.requestor.enterConstructor(methodInfo);
		}
		if (this.reportReferenceInfo) {
			ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) methodDeclaration;
			ExplicitConstructorCall constructorCall = constructorDeclaration.constructorCall;
			if (constructorCall != null) {
				switch(constructorCall.accessMode) {
					case ExplicitConstructorCall.This :
						this.requestor.acceptConstructorReference(
							this.typeNames[this.nestedTypeIndex-1],
							constructorCall.arguments == null ? 0 : constructorCall.arguments.length,
							constructorCall.sourceStart);
						break;
					case ExplicitConstructorCall.Super :
					case ExplicitConstructorCall.ImplicitSuper :
						this.requestor.acceptConstructorReference(
							this.superTypeNames[this.nestedTypeIndex-1],
							constructorCall.arguments == null ? 0 : constructorCall.arguments.length,
							constructorCall.sourceStart);
						break;
				}
			}
		}
		this.visitIfNeeded(methodDeclaration);
		if (isInRange){
			this.requestor.exitConstructor(methodDeclaration.declarationSourceEnd);
		}
		return;
	}
	selectorSourceEnd = this.sourceEnds.get(methodDeclaration);
	if (isInRange) {
		int currentModifiers = methodDeclaration.modifiers;
		currentModifiers &= ExtraCompilerModifiers.AccJustFlag | ClassFileConstants.AccDeprecated | ClassFileConstants.AccAnnotationDefault | ExtraCompilerModifiers.AccDefaultMethod;
		if (isVarArgs)
			currentModifiers |= ClassFileConstants.AccVarargs;
		if (hasDeprecatedAnnotation(methodDeclaration.annotations))
			currentModifiers |= ClassFileConstants.AccDeprecated;

		TypeReference returnType = methodDeclaration instanceof MethodDeclaration
			? ((MethodDeclaration) methodDeclaration).returnType
			: null;
		methodInfo.isAnnotation = methodDeclaration instanceof AnnotationMethodDeclaration;
		methodInfo.declarationStart = methodDeclaration.declarationSourceStart;
		methodInfo.modifiers = currentModifiers;
		methodInfo.returnType = returnType == null ? null : CharOperation.concatWith(returnType.getParameterizedTypeName(), '.');
		methodInfo.name = methodDeclaration.selector;
		methodInfo.nameSourceStart = methodDeclaration.sourceStart;
		methodInfo.nameSourceEnd = selectorSourceEnd;
		methodInfo.parameterTypes = argumentTypes;
		methodInfo.parameterNames = argumentNames;
		methodInfo.exceptionTypes = thrownExceptionTypes;
		methodInfo.typeParameters = getTypeParameterInfos(methodDeclaration.typeParameters());
		methodInfo.parameterInfos = parameterInfos;
		methodInfo.categories = this.nodesToCategories.get(methodDeclaration);
		methodInfo.annotations = methodDeclaration.annotations;
		methodInfo.node = methodDeclaration;
		methodInfo.enclosingType = declaringType;
		methodInfo.declaringPackageName = currentPackage == null ? CharOperation.NO_CHAR : CharOperation.concatWith(currentPackage.tokens, '.');
		this.requestor.enterMethod(methodInfo);
	}

	this.visitIfNeeded(methodDeclaration);

	if (isInRange) {
		if (methodDeclaration instanceof AnnotationMethodDeclaration) {
			AnnotationMethodDeclaration annotationMethodDeclaration = (AnnotationMethodDeclaration) methodDeclaration;
			Expression expression = annotationMethodDeclaration.defaultValue;
			if (expression != null) {
				this.requestor.exitMethod(methodDeclaration.declarationSourceEnd, expression);
				return;
			}
		}
		this.requestor.exitMethod(methodDeclaration.declarationSourceEnd, null);
	}
}

/*
 * Update the bodyStart of the corresponding parse node
 */
public void notifySourceElementRequestor(
		CompilationUnitDeclaration parsedUnit,
		int sourceStart,
		int sourceEnd,
		boolean reportReference,
		HashtableOfObjectToInt sourceEndsMap,
		Map nodesToCategoriesMap) {

	this.initialPosition = sourceStart;
	this.eofPosition = sourceEnd;

	this.reportReferenceInfo = reportReference;
	this.sourceEnds = sourceEndsMap;
	this.nodesToCategories = nodesToCategoriesMap;

	try {
		// range check
		boolean isInRange =
					this.initialPosition <= parsedUnit.sourceStart
					&& this.eofPosition >= parsedUnit.sourceEnd;

		// collect the top level ast nodes
		int length = 0;
		ASTNode[] nodes = null;
		if (isInRange) {
			this.requestor.enterCompilationUnit();
		}
		ImportReference currentPackage = parsedUnit.currentPackage;
		if (this.localDeclarationVisitor !=  null) {
			this.localDeclarationVisitor.currentPackage = currentPackage;
		}
		ImportReference[] imports = parsedUnit.imports;
		TypeDeclaration[] types = parsedUnit.types;
		length =
			(currentPackage == null ? 0 : 1)
			+ (imports == null ? 0 : imports.length)
			+ (types == null ? 0 : types.length)
			+ (parsedUnit.moduleDeclaration == null ? 0 : 1);
		nodes = new ASTNode[length];
		int index = 0;
		if (currentPackage != null) {
			nodes[index++] = currentPackage;
		}
		if (imports != null) {
			for (int i = 0, max = imports.length; i < max; i++) {
				nodes[index++] = imports[i];
			}
		}
		if (types != null) {
			for (int i = 0, max = types.length; i < max; i++) {
				nodes[index++] = types[i];
			}
		}

		if (parsedUnit.moduleDeclaration != null)
			nodes[index++] = parsedUnit.moduleDeclaration;

		// notify the nodes in the syntactical order
		if (length > 0) {
			quickSort(nodes, 0, length-1);
			for (int i=0;i<length;i++) {
				ASTNode node = nodes[i];
				if (node instanceof ImportReference) {
					ImportReference importRef = (ImportReference)node;
					if (node == parsedUnit.currentPackage) {
						notifySourceElementRequestor(importRef, true);
					} else {
						notifySourceElementRequestor(importRef, false);
					}
				} else if (node instanceof TypeDeclaration) {
					notifySourceElementRequestor((TypeDeclaration)node, true, null, currentPackage);
				} else if (node instanceof ModuleDeclaration) {
					notifySourceElementRequestor(parsedUnit.moduleDeclaration);
				}
			}
		}

		if (isInRange) {
			this.requestor.exitCompilationUnit(parsedUnit.sourceEnd);
		}
	} finally {
		reset();
	}
}

/*
* Update the bodyStart of the corresponding parse node
*/
protected void notifySourceElementRequestor(FieldDeclaration fieldDeclaration, TypeDeclaration declaringType) {

	// range check
	boolean isInRange =
				this.initialPosition <= fieldDeclaration.declarationSourceStart
				&& this.eofPosition >= fieldDeclaration.declarationSourceEnd;

	switch(fieldDeclaration.getKind()) {
		case AbstractVariableDeclaration.ENUM_CONSTANT:
			if (this.reportReferenceInfo) {
				// accept constructor reference for enum constant
				if (fieldDeclaration.initialization instanceof AllocationExpression) {
					AllocationExpression alloc = (AllocationExpression) fieldDeclaration.initialization;
					this.requestor.acceptConstructorReference(
						declaringType.name,
						alloc.arguments == null ? 0 : alloc.arguments.length,
						alloc.sourceStart);
				}
			}
			// $FALL-THROUGH$
		case AbstractVariableDeclaration.FIELD:
			int fieldEndPosition = this.sourceEnds.get(fieldDeclaration);
			if (fieldEndPosition == -1) {
				// use the declaration source end by default
				fieldEndPosition = fieldDeclaration.declarationSourceEnd;
			}
			if (isInRange) {
				int currentModifiers = fieldDeclaration.modifiers;

				// remember deprecation so as to not lose it below
				boolean deprecated = (currentModifiers & ClassFileConstants.AccDeprecated) != 0 || hasDeprecatedAnnotation(fieldDeclaration.annotations);

				char[] typeName = null;
				if (fieldDeclaration.type == null) {
					// enum constant
					typeName = declaringType.name;
					currentModifiers |= ClassFileConstants.AccEnum;
				} else {
					// regular field
					typeName = CharOperation.concatWith(fieldDeclaration.type.getParameterizedTypeName(), '.');
				}
				ISourceElementRequestor.FieldInfo fieldInfo = new ISourceElementRequestor.FieldInfo();
				fieldInfo.typeAnnotated = ((fieldDeclaration.bits & ASTNode.HasTypeAnnotations) != 0);
				fieldInfo.declarationStart = fieldDeclaration.declarationSourceStart;
				fieldInfo.name = fieldDeclaration.name;
				fieldInfo.modifiers = deprecated ? (currentModifiers & ExtraCompilerModifiers.AccJustFlag) | ClassFileConstants.AccDeprecated : currentModifiers & ExtraCompilerModifiers.AccJustFlag;
				if (fieldDeclaration.isARecordComponent) {
					fieldInfo.modifiers |= ExtraCompilerModifiers.AccRecord;
					fieldInfo.isRecordComponent = true;
				}
				fieldInfo.type = typeName;
				fieldInfo.nameSourceStart = fieldDeclaration.sourceStart;
				fieldInfo.nameSourceEnd = fieldDeclaration.sourceEnd;
				fieldInfo.categories = this.nodesToCategories.get(fieldDeclaration);
				fieldInfo.annotations = fieldDeclaration.annotations;
				fieldInfo.node = fieldDeclaration;
				this.requestor.enterField(fieldInfo);
			}
			this.visitIfNeeded(fieldDeclaration, declaringType);
			if (isInRange){
				this.requestor.exitField(
					// filter out initializations that are not a constant (simple check)
					(fieldDeclaration.initialization == null
							|| fieldDeclaration.initialization instanceof ArrayInitializer
							|| fieldDeclaration.initialization instanceof AllocationExpression
							|| fieldDeclaration.initialization instanceof ArrayAllocationExpression
							|| fieldDeclaration.initialization instanceof Assignment
							|| fieldDeclaration.initialization instanceof ClassLiteralAccess
							|| fieldDeclaration.initialization instanceof MessageSend
							|| fieldDeclaration.initialization instanceof ArrayReference
							|| fieldDeclaration.initialization instanceof ThisReference) ?
						-1 :
						fieldDeclaration.initialization.sourceStart,
					fieldEndPosition,
					fieldDeclaration.declarationSourceEnd);
			}
			break;
		case AbstractVariableDeclaration.INITIALIZER:
			if (isInRange){
				this.requestor.enterInitializer(
					fieldDeclaration.declarationSourceStart,
					fieldDeclaration.modifiers);
			}
			this.visitIfNeeded((Initializer)fieldDeclaration);
			if (isInRange){
				this.requestor.exitInitializer(fieldDeclaration.declarationSourceEnd);
			}
			break;
	}
}
protected void notifySourceElementRequestor(
	ImportReference importReference,
	boolean isPackage) {
	if (isPackage) {
		this.requestor.acceptPackage(importReference);
	} else {
		final boolean onDemand = (importReference.bits & ASTNode.OnDemand) != 0;
		this.requestor.acceptImport(
			importReference.declarationSourceStart,
			importReference.declarationSourceEnd,
			importReference.sourceStart,
			onDemand ? importReference.trailingStarPosition : importReference.sourceEnd,
			importReference.tokens,
			onDemand,
			importReference.modifiers);
	}
}
protected void notifySourceElementRequestor(ModuleDeclaration moduleDeclaration) {
	boolean isInRange =
			this.initialPosition <= moduleDeclaration.declarationSourceStart
			&& this.eofPosition >= moduleDeclaration.declarationSourceEnd;
	ISourceElementRequestor.ModuleInfo info = new ISourceElementRequestor.ModuleInfo();
	if (isInRange) {

		int currentModifiers = moduleDeclaration.modifiers;

		// remember deprecation so as to not lose it below
		boolean deprecated = (currentModifiers & ClassFileConstants.AccDeprecated) != 0 || hasDeprecatedAnnotation(moduleDeclaration.annotations);

		info.declarationStart = moduleDeclaration.declarationSourceStart;
		info.modifiers = deprecated ? (currentModifiers & ExtraCompilerModifiers.AccJustFlag) | ClassFileConstants.AccDeprecated : currentModifiers & ExtraCompilerModifiers.AccJustFlag;
		info.name = TypeConstants.MODULE_INFO_NAME;
		info.nameSourceStart = moduleDeclaration.sourceStart;
		info.nameSourceEnd = moduleDeclaration.sourceEnd;
		info.moduleName = moduleDeclaration.moduleName;
		info.annotations = moduleDeclaration.annotations;
		info.node = moduleDeclaration;
		info.categories = this.nodesToCategories.get(moduleDeclaration);
		fillModuleInfo(moduleDeclaration, info);
		this.requestor.enterModule(info);
		this.requestor.exitModule(moduleDeclaration.declarationSourceEnd);
	}
}
//protected void notifySourceElementRequestor(RecordComponent recordComponent, TypeDeclaration declaringType) {
//	assert declaringType.isRecord();
//
//	// range check
//	boolean isInRange =
//				this.initialPosition <= recordComponent.declarationSourceStart
//				&& this.eofPosition >= recordComponent.declarationSourceEnd;
//	int recordComponentEndPosition = this.sourceEnds.get(recordComponent);
//	if (recordComponentEndPosition == -1) {
//		// use the declaration source end by default
//		recordComponentEndPosition = recordComponent.declarationSourceEnd;
//	}
//	if (isInRange) {
//		char[] typeName = CharOperation.concatWith(recordComponent.type.getParameterizedTypeName(), '.');
//		ISourceElementRequestor.RecordComponentInfo recordComponentInfo = new ISourceElementRequestor.RecordComponentInfo();
//		recordComponentInfo.typeAnnotated = ((recordComponent.bits & ASTNode.HasTypeAnnotations) != 0);
//		recordComponentInfo.declarationStart = recordComponent.declarationSourceStart;
//		recordComponentInfo.name = recordComponent.name;
//		recordComponentInfo.type = typeName;
//		recordComponentInfo.nameSourceStart = recordComponent.sourceStart;
//		recordComponentInfo.nameSourceEnd = recordComponent.sourceEnd;
//		recordComponentInfo.categories = this.nodesToCategories.get(recordComponent);
//		recordComponentInfo.annotations = recordComponent.annotations;
//		recordComponentInfo.node = recordComponent;
//		this.requestor.enterRecordComponent(recordComponentInfo);
//	}
//	this.visitIfNeeded(recordComponent, declaringType);
//	if (isInRange){
//		this.requestor.exitRecordComponent(
//			recordComponentEndPosition,
//			recordComponent.declarationSourceEnd);
//	}
//
//}
protected void notifySourceElementRequestor(TypeDeclaration typeDeclaration, boolean notifyTypePresence, TypeDeclaration declaringType, ImportReference currentPackage) {

	if (CharOperation.equals(TypeConstants.PACKAGE_INFO_NAME, typeDeclaration.name)) return;

	// range check
	boolean isInRange =
		this.initialPosition <= typeDeclaration.declarationSourceStart
		&& this.eofPosition >= typeDeclaration.declarationSourceEnd;

	FieldDeclaration[] fields = typeDeclaration.fields;
	AbstractMethodDeclaration[] methods = typeDeclaration.methods;
	TypeDeclaration[] memberTypes = typeDeclaration.memberTypes;
	int fieldCounter = fields == null ? 0 : fields.length;
	int methodCounter = methods == null ? 0 : methods.length;
	int memberTypeCounter = memberTypes == null ? 0 : memberTypes.length;
	int fieldIndex = 0;
	int methodIndex = 0;
	int memberTypeIndex = 0;

	if (notifyTypePresence){
		char[][] interfaceNames = getInterfaceNames(typeDeclaration);
		int kind = TypeDeclaration.kind(typeDeclaration.modifiers);
		char[] implicitSuperclassName = TypeConstants.CharArray_JAVA_LANG_OBJECT;
		ISourceElementRequestor.TypeInfo typeInfo = new ISourceElementRequestor.TypeInfo();
		typeInfo.typeAnnotated = ((typeDeclaration.bits & ASTNode.HasTypeAnnotations) != 0);
		if (isInRange) {
			int currentModifiers = typeDeclaration.modifiers;

			// remember deprecation so as to not lose it below
			boolean deprecated = (currentModifiers & ClassFileConstants.AccDeprecated) != 0 || hasDeprecatedAnnotation(typeDeclaration.annotations);

			boolean isEnumInit = typeDeclaration.allocation != null && typeDeclaration.allocation.enumConstant != null;
			char[] superclassName;
			if (isEnumInit) {
				currentModifiers |= ClassFileConstants.AccEnum;
				superclassName = declaringType.name;
			} else {
				superclassName = getSuperclassName(typeDeclaration);
			}
			if (typeDeclaration.allocation == null) {
				typeInfo.declarationStart = typeDeclaration.declarationSourceStart;
			} else if (isEnumInit) {
				typeInfo.declarationStart = typeDeclaration.allocation.enumConstant.sourceStart;
			} else {
				typeInfo.declarationStart = typeDeclaration.allocation.sourceStart;
			}
			typeInfo.modifiers = deprecated
					? (currentModifiers & ExtraCompilerModifiers.AccJustFlag) | ClassFileConstants.AccDeprecated
					: currentModifiers & ExtraCompilerModifiers.AccJustFlag;
			typeInfo.modifiers |= currentModifiers & (ExtraCompilerModifiers.AccSealed | ExtraCompilerModifiers.AccNonSealed);
			typeInfo.name = typeDeclaration.name;
			typeInfo.nameSourceStart = isEnumInit ? typeDeclaration.allocation.enumConstant.sourceStart : typeDeclaration.sourceStart;
			typeInfo.nameSourceEnd = sourceEnd(typeDeclaration);
			typeInfo.superclass = superclassName;
			typeInfo.superinterfaces = interfaceNames;
			typeInfo.typeParameters = getTypeParameterInfos(typeDeclaration.typeParameters);
			typeInfo.categories = this.nodesToCategories.get(typeDeclaration);
			typeInfo.secondary = typeDeclaration.isSecondary();
			typeInfo.anonymousMember = typeDeclaration.allocation != null && typeDeclaration.allocation.enclosingInstance != null;
			typeInfo.annotations = typeDeclaration.annotations;
			typeInfo.extraFlags = ExtraFlags.getExtraFlags(typeDeclaration);
			typeInfo.node = typeDeclaration;
			if ((currentModifiers & ExtraCompilerModifiers.AccSealed) != 0) {
				typeInfo.permittedSubtypes = getPermittedSubTypes(typeDeclaration);
			}
			switch (kind) {
				case TypeDeclaration.CLASS_DECL :
					if (superclassName != null)
						implicitSuperclassName = superclassName;
					break;
				case TypeDeclaration.INTERFACE_DECL :
					implicitSuperclassName = TypeConstants.CharArray_JAVA_LANG_OBJECT;
					break;
				case TypeDeclaration.ENUM_DECL :
					implicitSuperclassName = TypeConstants.CharArray_JAVA_LANG_ENUM;
					break;
				case TypeDeclaration.ANNOTATION_TYPE_DECL :
					implicitSuperclassName = TypeConstants.CharArray_JAVA_LANG_ANNOTATION_ANNOTATION;
					break;
				case TypeDeclaration.RECORD_DECL :
 					implicitSuperclassName = TypeConstants.CharArray_JAVA_LANG_RECORD;
 					typeInfo.modifiers |= ExtraCompilerModifiers.AccRecord;
 					break;
			}
			this.requestor.enterType(typeInfo);
		}
		if (this.nestedTypeIndex == this.typeNames.length) {
			// need a resize
			System.arraycopy(this.typeNames, 0, (this.typeNames = new char[this.nestedTypeIndex * 2][]), 0, this.nestedTypeIndex);
			System.arraycopy(this.superTypeNames, 0, (this.superTypeNames = new char[this.nestedTypeIndex * 2][]), 0, this.nestedTypeIndex);
		}
		this.typeNames[this.nestedTypeIndex] = typeDeclaration.name;
		this.superTypeNames[this.nestedTypeIndex++] = implicitSuperclassName;
	}
	while ((fieldIndex < fieldCounter)
			|| (memberTypeIndex < memberTypeCounter)
			|| (methodIndex < methodCounter)) {
		FieldDeclaration nextFieldDeclaration = null;
		AbstractMethodDeclaration nextMethodDeclaration = null;
		TypeDeclaration nextMemberDeclaration = null;

		int position = Integer.MAX_VALUE;
		int nextDeclarationType = -1;
		if (fieldIndex < fieldCounter) {
			nextFieldDeclaration = fields[fieldIndex];
			if (nextFieldDeclaration.declarationSourceStart < position) {
				position = nextFieldDeclaration.declarationSourceStart;
				nextDeclarationType = 0; // FIELD
			}
		}
		if (methodIndex < methodCounter) {
			nextMethodDeclaration = methods[methodIndex];
			if (nextMethodDeclaration.declarationSourceStart < position) {
				position = nextMethodDeclaration.declarationSourceStart;
				nextDeclarationType = 1; // METHOD
			}
		}
		if (memberTypeIndex < memberTypeCounter) {
			nextMemberDeclaration = memberTypes[memberTypeIndex];
			if (nextMemberDeclaration.declarationSourceStart < position) {
				position = nextMemberDeclaration.declarationSourceStart;
				nextDeclarationType = 2; // MEMBER
			}
		}
		switch (nextDeclarationType) {
			case 0 :
				fieldIndex++;
				notifySourceElementRequestor(nextFieldDeclaration, typeDeclaration);
				break;
			case 1 :
				methodIndex++;
				notifySourceElementRequestor(nextMethodDeclaration, typeDeclaration, currentPackage);
				break;
			case 2 :
				memberTypeIndex++;
				notifySourceElementRequestor(nextMemberDeclaration, true, null, currentPackage);
				break;
		}
	}
	if (notifyTypePresence){
		if (isInRange){
			this.requestor.exitType(typeDeclaration.declarationSourceEnd);
		}
		this.nestedTypeIndex--;
	}
}
private void fillModuleInfo(ModuleDeclaration mod, ISourceElementRequestor.ModuleInfo modInfo) {
	if (mod.requiresCount > 0) {
		ISourceElementRequestor.RequiresInfo reqs[] = new ISourceElementRequestor.RequiresInfo[mod.requiresCount];
		for (int i = 0; i < mod.requiresCount; i++) {
			ISourceElementRequestor.RequiresInfo req = new ISourceElementRequestor.RequiresInfo();
			req.moduleName = CharOperation.concatWith(mod.requires[i].module.tokens, '.');
			req.modifiers = mod.requires[i].modifiers;
			reqs[i] = req;
		}
		modInfo.requires = reqs;
	}
	if (mod.exportsCount > 0) {
		ISourceElementRequestor.PackageExportInfo exps[] = new ISourceElementRequestor.PackageExportInfo[mod.exportsCount];
		for (int i = 0; i < mod.exportsCount; i++) {
			ISourceElementRequestor.PackageExportInfo exp = new ISourceElementRequestor.PackageExportInfo();
			ExportsStatement exportsStatement = mod.exports[i];
			exp.pkgName = exportsStatement.pkgName;
			if (exportsStatement.targets == null) {
				exp.targets = CharOperation.NO_CHAR_CHAR;
			} else {
				exp.targets = new char[exportsStatement.targets.length][];
				for(int j = 0; j < exp.targets.length; j++) {
					exp.targets[j] = CharOperation.concatWith(exportsStatement.targets[j].tokens, '.');
				}
			}
			exps[i] = exp;
		}
		modInfo.exports = exps;
	}
	if (mod.servicesCount > 0) {
		ISourceElementRequestor.ServicesInfo[] services = new ISourceElementRequestor.ServicesInfo[mod.servicesCount];
		for (int i = 0; i < services.length; i++) {
			ISourceElementRequestor.ServicesInfo ser = new ISourceElementRequestor.ServicesInfo();
			ser.serviceName = CharOperation.concatWith(mod.services[i].serviceInterface.getParameterizedTypeName(), '.');
			ser.implNames = new char[mod.services[i].implementations.length][];
			for (int j = 0; j < ser.implNames.length; j++) {
				ser.implNames[j] = CharOperation.concatWith(mod.services[i].implementations[j].getParameterizedTypeName(), '.');
			}
			services[i] = ser;
		}
		modInfo.services = services;
	}
	if (mod.usesCount > 0) {
		char[][] uses = new char[mod.usesCount][];
		for (int i = 0; i < uses.length; i++) {
			uses[i] = CharOperation.concatWith(mod.uses[i].serviceInterface.getParameterizedTypeName(), '.');
		}
		modInfo.usedServices = uses;
	}
	if (mod.opensCount > 0) {
		ISourceElementRequestor.PackageExportInfo opens[] = new ISourceElementRequestor.PackageExportInfo[mod.opensCount];
		for (int i = 0; i < mod.opensCount; i++) {
			ISourceElementRequestor.PackageExportInfo op = new ISourceElementRequestor.PackageExportInfo();
			OpensStatement openStmt = mod.opens[i];
			op.pkgName = openStmt.pkgName;
			if (openStmt.targets == null) {
				op.targets = CharOperation.NO_CHAR_CHAR;
			} else {
				op.targets = new char[openStmt.targets.length][];
				for(int j = 0; j < op.targets.length; j++) {
					op.targets[j] = CharOperation.concatWith(openStmt.targets[j].tokens, '.');
				}
			}
			opens[i] = op;
		}
		modInfo.opens = opens;
	}
}
/*
 * Sort the given ast nodes by their positions.
 */
private static void quickSort(ASTNode[] sortedCollection, int left, int right) {
	int original_left = left;
	int original_right = right;
	ASTNode mid = sortedCollection[left +  (right - left) / 2];
	do {
		while (sortedCollection[left].sourceStart < mid.sourceStart) {
			left++;
		}
		while (mid.sourceStart < sortedCollection[right].sourceStart) {
			right--;
		}
		if (left <= right) {
			ASTNode tmp = sortedCollection[left];
			sortedCollection[left] = sortedCollection[right];
			sortedCollection[right] = tmp;
			left++;
			right--;
		}
	} while (left <= right);
	if (original_left < right) {
		quickSort(sortedCollection, original_left, right);
	}
	if (left < original_right) {
		quickSort(sortedCollection, left, original_right);
	}
}
private void reset() {
	this.typeNames = new char[4][];
	this.superTypeNames = new char[4][];
	this.nestedTypeIndex = 0;

	this.sourceEnds = null;
}
private int sourceEnd(TypeDeclaration typeDeclaration) {
	if ((typeDeclaration.bits & ASTNode.IsAnonymousType) != 0) {
		QualifiedAllocationExpression allocation = typeDeclaration.allocation;
		if (allocation.enumConstant != null) // case of enum constant body
			return allocation.enumConstant.sourceEnd;
		return allocation.type.sourceEnd;
	} else {
		return typeDeclaration.sourceEnd;
	}
}
private void visitIfNeeded(AbstractMethodDeclaration method) {
	if (this.localDeclarationVisitor != null
		&& (method.bits & ASTNode.HasLocalType) != 0) {
			if (method instanceof ConstructorDeclaration) {
				ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) method;
				if (constructorDeclaration.constructorCall != null) {
					constructorDeclaration.constructorCall.traverse(this.localDeclarationVisitor, method.scope);
				}
			}
			if (method.statements != null) {
				int statementsLength = method.statements.length;
				for (int i = 0; i < statementsLength; i++)
					method.statements[i].traverse(this.localDeclarationVisitor, method.scope);
			}
	}
}

private void visitIfNeeded(FieldDeclaration field, TypeDeclaration declaringType) {
	if (this.localDeclarationVisitor != null
		&& (field.bits & ASTNode.HasLocalType) != 0) {
			if (field.initialization != null) {
				try {
					this.localDeclarationVisitor.pushDeclaringType(declaringType);
					field.initialization.traverse(this.localDeclarationVisitor, (MethodScope) null);
				} finally {
					this.localDeclarationVisitor.popDeclaringType();
				}
			}
	}
}

private void visitIfNeeded(Initializer initializer) {
	if (this.localDeclarationVisitor != null
		&& (initializer.bits & ASTNode.HasLocalType) != 0) {
			if (initializer.block != null) {
				initializer.block.traverse(this.localDeclarationVisitor, null);
			}
	}
}
}
