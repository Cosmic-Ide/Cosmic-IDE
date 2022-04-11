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
package org.eclipse.jdt.internal.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ISourceElementRequestor;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.Literal;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.NullLiteral;
import org.eclipse.jdt.internal.compiler.ast.OperatorIds;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.UnaryExpression;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.parser.RecoveryScanner;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObject;
import org.eclipse.jdt.internal.core.util.ReferenceInfoAdapter;
import org.eclipse.jdt.internal.core.util.Util;
/**
 * A requestor for the fuzzy parser, used to compute the children of an ICompilationUnit.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CompilationUnitStructureRequestor extends ReferenceInfoAdapter implements ISourceElementRequestor {

	/**
	 * The handle to the compilation unit being parsed
	 */
	protected ICompilationUnit unit;

	/**
	 * The info object for the compilation unit being parsed
	 */
	protected CompilationUnitElementInfo unitInfo;

	/**
	 * The import container info - null until created
	 */
	protected ImportContainerInfo importContainerInfo = null;
	protected ImportContainer importContainer;

	/**
	 * Hashtable of children elements of the compilation unit.
	 * Children are added to the table as they are found by
	 * the parser. Keys are handles, values are corresponding
	 * info objects.
	 */
	protected Map newElements;

	/*
	 * A table from a handle (with occurenceCount == 1) to the current occurence count for this handle
	 */
	private HashMap<Object, Integer> occurenceCounts;

	/*
	 * A table to store the occurrence count of anonymous types. The key will be the handle to the
	 * enclosing type of the anonymous.
	 */
	private HashMap<Object, Integer> localOccurrenceCounts;

	/**
	 * Stack of parent scope info objects. The info on the
	 * top of the stack is the parent of the next element found.
	 * For example, when we locate a method, the parent info object
	 * will be the type the method is contained in.
	 */
	protected Stack infoStack;

	/*
	 * Map from info to of ArrayList of IJavaElement representing the children
	 * of the given info.
	 */
	protected HashMap children;

	/**
	 * Stack of parent handles, corresponding to the info stack. We
	 * keep both, since info objects do not have back pointers to
	 * handles.
	 */
	protected Stack handleStack;

	/**
	 * The number of references reported thus far. Used to
	 * expand the arrays of reference kinds and names.
	 */
	protected int referenceCount= 0;

	/**
	 * Problem requestor which will get notified of discovered problems
	 */
	protected boolean hasSyntaxErrors = false;

	/*
	 * The parser this requestor is using.
	 */
	protected Parser parser;

	protected HashtableOfObject fieldRefCache;
	protected HashtableOfObject messageRefCache;
	protected HashtableOfObject typeRefCache;
	protected HashtableOfObject unknownRefCache;

protected CompilationUnitStructureRequestor(ICompilationUnit unit, CompilationUnitElementInfo unitInfo, Map newElements) {
	this.unit = unit;
	this.unitInfo = unitInfo;
	this.newElements = newElements;
	this.occurenceCounts = new HashMap<>();
	this.localOccurrenceCounts = new HashMap<>(5);
}
/**
 * @see ISourceElementRequestor
 */
@Override
public void acceptImport(int declarationStart, int declarationEnd, int nameSourceStart, int nameSourceEnd, char[][] tokens, boolean onDemand, int modifiers) {
	JavaElement parentHandle= (JavaElement) this.handleStack.peek();
	if (!(parentHandle.getElementType() == IJavaElement.COMPILATION_UNIT)) {
		Assert.isTrue(false); // Should not happen
	}

	ICompilationUnit parentCU= (ICompilationUnit)parentHandle;
	//create the import container and its info
	if (this.importContainer == null) {
		this.importContainer = createImportContainer(parentCU);
		this.importContainerInfo = new ImportContainerInfo();
		Object parentInfo = this.infoStack.peek();
		addToChildren(parentInfo, this.importContainer);
		this.newElements.put(this.importContainer, this.importContainerInfo);
	}

	String elementName = JavaModelManager.getJavaModelManager().intern(new String(CharOperation.concatWith(tokens, '.')));
	ImportDeclaration handle = createImportDeclaration(this.importContainer, elementName, onDemand);
	resolveDuplicates(handle);

	ImportDeclarationElementInfo info = new ImportDeclarationElementInfo();
	info.setSourceRangeStart(declarationStart);
	info.setSourceRangeEnd(declarationEnd);
	info.setNameSourceStart(nameSourceStart);
	info.setNameSourceEnd(nameSourceEnd);
	info.setFlags(modifiers);

	addToChildren(this.importContainerInfo, handle);
	this.newElements.put(handle, info);
}
/*
 * Table of line separator position. This table is passed once at the end
 * of the parse action, so as to allow computation of normalized ranges.
 *
 * A line separator might corresponds to several characters in the source,
 *
 */
@Override
public void acceptLineSeparatorPositions(int[] positions) {
	// ignore line separator positions
}
/**
 * @see ISourceElementRequestor
 */
@Override
public void acceptPackage(ImportReference importReference) {

		Object parentInfo = this.infoStack.peek();
		JavaElement parentHandle= (JavaElement) this.handleStack.peek();
		PackageDeclaration handle = null;

		if (parentHandle.getElementType() == IJavaElement.COMPILATION_UNIT) {
			char[] name = CharOperation.concatWith(importReference.getImportName(), '.');
			handle = createPackageDeclaration(parentHandle, new String(name));
		}
		else {
			Assert.isTrue(false); // Should not happen
		}
		resolveDuplicates(handle);

		AnnotatableInfo info = new AnnotatableInfo();
		info.setSourceRangeStart(importReference.declarationSourceStart);
		info.setSourceRangeEnd(importReference.declarationSourceEnd);
		info.setNameSourceStart(importReference.sourceStart);
		info.setNameSourceEnd(importReference.sourceEnd);

		addToChildren(parentInfo, handle);
		this.newElements.put(handle, info);

		if (importReference.annotations != null) {
			for (int i = 0, length = importReference.annotations.length; i < length; i++) {
				org.eclipse.jdt.internal.compiler.ast.Annotation annotation = importReference.annotations[i];
				acceptAnnotation(annotation, info, handle);
			}
		}
}
@Override
public void acceptProblem(CategorizedProblem problem) {
	if ((problem.getID() & IProblem.Syntax) != 0){
		this.hasSyntaxErrors = true;
	}
}
private void addToChildren(Object parentInfo, JavaElement handle) {
	ArrayList childrenList = (ArrayList) this.children.get(parentInfo);
	if (childrenList == null)
		this.children.put(parentInfo, childrenList = new ArrayList());
	childrenList.add(handle);
}
protected Annotation createAnnotation(JavaElement parent, String name) {
	return new Annotation(parent, name);
}
protected SourceField createField(JavaElement parent, FieldInfo fieldInfo) {
	String fieldName = JavaModelManager.getJavaModelManager().intern(new String(fieldInfo.name));
	return new SourceField(parent, fieldName);
}
protected SourceField createRecordComponent(JavaElement parent, FieldInfo compInfo) {
	String name = JavaModelManager.getJavaModelManager().intern(new String(compInfo.name));
	SourceField field = new SourceField(parent, name) {
		@Override
		public boolean isRecordComponent() throws JavaModelException {
			return true;
		}
	};
	return field;
}
protected ImportContainer createImportContainer(ICompilationUnit parent) {
	return (ImportContainer)parent.getImportContainer();
}
protected ImportDeclaration createImportDeclaration(ImportContainer parent, String name, boolean onDemand) {
	return new ImportDeclaration(parent, name, onDemand);
}
protected Initializer createInitializer(JavaElement parent) {
	return new Initializer(parent, 1);
}
protected SourceMethod createMethodHandle(JavaElement parent, MethodInfo methodInfo) {
	String selector = JavaModelManager.getJavaModelManager().intern(new String(methodInfo.name));
	String[] parameterTypeSigs = convertTypeNamesToSigs(methodInfo.parameterTypes);
	return new SourceMethod(parent, selector, parameterTypeSigs);
}
protected PackageDeclaration createPackageDeclaration(JavaElement parent, String name) {
	return new PackageDeclaration((CompilationUnit) parent, name);
}
protected SourceType createTypeHandle(JavaElement parent, TypeInfo typeInfo) {
	String nameString= new String(typeInfo.name);
	return new SourceType(parent, nameString);
}
protected SourceModule createModuleHandle(JavaElement parent, ModuleInfo modInfo) {
	String nameString= new String(modInfo.moduleName);
	return new org.eclipse.jdt.internal.core.SourceModule(parent, nameString);
}
protected TypeParameter createTypeParameter(JavaElement parent, String name) {
	return new TypeParameter(parent, name);
}
/**
 * Convert these type names to signatures.
 * @see Signature
 */
protected static String[] convertTypeNamesToSigs(char[][] typeNames) {
	if (typeNames == null)
		return CharOperation.NO_STRINGS;
	int n = typeNames.length;
	if (n == 0)
		return CharOperation.NO_STRINGS;
	JavaModelManager manager = JavaModelManager.getJavaModelManager();
	String[] typeSigs = new String[n];
	for (int i = 0; i < n; ++i) {
		typeSigs[i] = manager.intern(Signature.createTypeSignature(typeNames[i], false));
	}
	return typeSigs;
}
protected IAnnotation acceptAnnotation(org.eclipse.jdt.internal.compiler.ast.Annotation annotation, AnnotatableInfo parentInfo, JavaElement parentHandle) {
	String nameString = new String(CharOperation.concatWith(annotation.type.getTypeName(), '.'));
	Annotation handle = createAnnotation(parentHandle, nameString); //NB: occurenceCount is computed in resolveDuplicates
	resolveDuplicates(handle);

	AnnotationInfo info = new AnnotationInfo();

	// populate the maps here as getValue(...) below may need them
	this.newElements.put(handle, info);
	this.handleStack.push(handle);

	info.setSourceRangeStart(annotation.sourceStart());
	info.nameStart = annotation.type.sourceStart();
	info.nameEnd = annotation.type.sourceEnd();
	MemberValuePair[] memberValuePairs = annotation.memberValuePairs();
	int membersLength = memberValuePairs.length;
	if (membersLength == 0) {
		info.members = Annotation.NO_MEMBER_VALUE_PAIRS;
	} else {
		info.members = getMemberValuePairs(memberValuePairs);
	}

	if (parentInfo != null) {
		IAnnotation[] annotations = parentInfo.annotations;
		int length = annotations.length;
		System.arraycopy(annotations, 0, annotations = new IAnnotation[length+1], 0, length);
		annotations[length] = handle;
		parentInfo.annotations = annotations;
	}
	info.setSourceRangeEnd(annotation.declarationSourceEnd);
	this.handleStack.pop();
	return handle;
}
/**
 * @see ISourceElementRequestor
 */
@Override
public void enterCompilationUnit() {
	this.infoStack = new Stack();
	this.children = new HashMap();
	this.handleStack= new Stack();
	this.infoStack.push(this.unitInfo);
	this.handleStack.push(this.unit);
}
/**
 * @see ISourceElementRequestor
 */
@Override
public void enterConstructor(MethodInfo methodInfo) {
	enterMethod(methodInfo);
}
/**
 * @see ISourceElementRequestor
 */
@Override
public void enterField(FieldInfo fieldInfo) {

	TypeInfo parentInfo = (TypeInfo) this.infoStack.peek();
	JavaElement parentHandle= (JavaElement) this.handleStack.peek();
	SourceField handle = null;
	if (parentHandle.getElementType() == IJavaElement.TYPE) {
		if (fieldInfo.isRecordComponent) {
			handle = createRecordComponent(parentHandle, fieldInfo);
		} else {
			handle = createField(parentHandle, fieldInfo);
		}
	}
	else {
		Assert.isTrue(false); // Should not happen
	}
	resolveDuplicates(handle);

	addToChildren(parentInfo, handle);
	parentInfo.childrenCategories.put(handle, fieldInfo.categories);

	this.infoStack.push(fieldInfo);
	this.handleStack.push(handle);

}
/**
 * @see ISourceElementRequestor
 */
@Override
public void enterInitializer(int declarationSourceStart, int modifiers) {
	Object parentInfo = this.infoStack.peek();
	JavaElement parentHandle= (JavaElement) this.handleStack.peek();
	Initializer handle = null;

	if (parentHandle.getElementType() == IJavaElement.TYPE) {
		handle = createInitializer(parentHandle);
	}
	else {
		Assert.isTrue(false); // Should not happen
	}
	resolveDuplicates(handle);

	addToChildren(parentInfo, handle);

	this.infoStack.push(new int[] {declarationSourceStart, modifiers});
	this.handleStack.push(handle);
}
/**
 * @see ISourceElementRequestor
 */
@Override
public void enterMethod(MethodInfo methodInfo) {

	TypeInfo parentInfo = (TypeInfo) this.infoStack.peek();
	JavaElement parentHandle= (JavaElement) this.handleStack.peek();
	SourceMethod handle = null;

	// translate nulls to empty arrays
	if (methodInfo.parameterTypes == null) {
		methodInfo.parameterTypes= CharOperation.NO_CHAR_CHAR;
	}
	if (methodInfo.parameterNames == null) {
		methodInfo.parameterNames= CharOperation.NO_CHAR_CHAR;
	}
	if (methodInfo.exceptionTypes == null) {
		methodInfo.exceptionTypes= CharOperation.NO_CHAR_CHAR;
	}

	if (parentHandle.getElementType() == IJavaElement.TYPE) {
		handle = createMethodHandle(parentHandle, methodInfo);
	}
	else {
		Assert.isTrue(false); // Should not happen
	}
	resolveDuplicates(handle);

	this.infoStack.push(methodInfo);
	this.handleStack.push(handle);

	addToChildren(parentInfo, handle);
	parentInfo.childrenCategories.put(handle, methodInfo.categories);
}
private SourceMethodElementInfo createMethodInfo(MethodInfo methodInfo, SourceMethod handle) {
	IJavaElement[] elements = getChildren(methodInfo);
	SourceMethodElementInfo info;
	if (methodInfo.isConstructor) {
		info = elements.length == 0 ? new SourceConstructorInfo() : new SourceConstructorWithChildrenInfo(elements);
	} else if (methodInfo.isAnnotation) {
		info = new SourceAnnotationMethodInfo();
	} else {
		info = elements.length == 0 ? new SourceMethodInfo() : new SourceMethodWithChildrenInfo(elements);
	}
	info.isCanonicalConstructor = methodInfo.isCanonicalConstr;
	info.setSourceRangeStart(methodInfo.declarationStart);
	int flags = methodInfo.modifiers;
	info.setNameSourceStart(methodInfo.nameSourceStart);
	info.setNameSourceEnd(methodInfo.nameSourceEnd);
	info.setFlags(flags);
	JavaModelManager manager = JavaModelManager.getJavaModelManager();
	char[][] parameterNames = methodInfo.parameterNames;
	for (int i = 0, length = parameterNames.length; i < length; i++)
		parameterNames[i] = manager.intern(parameterNames[i]);
	info.setArgumentNames(parameterNames);
	char[] returnType = methodInfo.returnType == null ? new char[]{'v', 'o','i', 'd'} : methodInfo.returnType;
	info.setReturnType(manager.intern(returnType));
	char[][] exceptionTypes = methodInfo.exceptionTypes;
	info.setExceptionTypeNames(exceptionTypes);
	for (int i = 0, length = exceptionTypes.length; i < length; i++)
		exceptionTypes[i] = manager.intern(exceptionTypes[i]);
	this.newElements.put(handle, info);

	if (methodInfo.typeParameters != null) {
		for (int i = 0, length = methodInfo.typeParameters.length; i < length; i++) {
			TypeParameterInfo typeParameterInfo = methodInfo.typeParameters[i];
			acceptTypeParameter(typeParameterInfo, info);
		}
	}
	if (methodInfo.annotations != null) {
		int length = methodInfo.annotations.length;
		this.unitInfo.annotationNumber += length;
		for (int i = 0; i < length; i++) {
			org.eclipse.jdt.internal.compiler.ast.Annotation annotation = methodInfo.annotations[i];
			acceptAnnotation(annotation, info, handle);
		}
	}
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=334783
	// Process the parameter annotations from the arguments
	if (methodInfo.node != null && methodInfo.node.arguments != null) {
		info.arguments = acceptMethodParameters(methodInfo.node.arguments, handle, methodInfo);
	}
	if (methodInfo.typeAnnotated) {
		this.unitInfo.annotationNumber = CompilationUnitElementInfo.ANNOTATION_THRESHOLD_FOR_DIET_PARSE;
	}
	return info;
}
private LocalVariable[] acceptMethodParameters(Argument[] arguments, JavaElement methodHandle, MethodInfo methodInfo) {
	if (arguments == null) return null;
	LocalVariable[] result = new LocalVariable[arguments.length];
	Annotation[][] paramAnnotations = new Annotation[arguments.length][];
	for(int i = 0; i < arguments.length; i++) {
		Argument argument = arguments[i];
		AnnotatableInfo localVarInfo = new AnnotatableInfo();
		localVarInfo.setSourceRangeStart(argument.declarationSourceStart);
		localVarInfo.setSourceRangeEnd(argument.declarationSourceStart);
		localVarInfo.setNameSourceStart(argument.sourceStart);
		localVarInfo.setNameSourceEnd(argument.sourceEnd);

		String paramTypeSig = JavaModelManager.getJavaModelManager().intern(Signature.createTypeSignature(methodInfo.parameterTypes[i], false));
		result[i] = new LocalVariable(
				methodHandle,
				new String(argument.name),
				argument.declarationSourceStart,
				argument.declarationSourceEnd,
				argument.sourceStart,
				argument.sourceEnd,
				paramTypeSig,
				argument.annotations,
				argument.modifiers,
				true);
		this.newElements.put(result[i], localVarInfo);
		this.infoStack.push(localVarInfo);
		this.handleStack.push(result[i]);
		if (argument.annotations != null) {
			paramAnnotations[i] = new Annotation[argument.annotations.length];
			for (int  j = 0; j < argument.annotations.length; j++ ) {
				org.eclipse.jdt.internal.compiler.ast.Annotation annotation = argument.annotations[j];
				acceptAnnotation(annotation, localVarInfo, result[i]);
			}
		}
		this.infoStack.pop();
		this.handleStack.pop();
	}
	return result;
}
@Override
public void enterModule(ModuleInfo info) {

	Object parentInfo = this.infoStack.peek();
	JavaElement parentHandle= (JavaElement) this.handleStack.peek();
	JavaElement handle = createModuleHandle(parentHandle, info);

	this.infoStack.push(info);
	this.handleStack.push(handle);

	addToChildren(parentInfo, handle);
}
/**
 * @see ISourceElementRequestor
 */
@Override
public void enterType(TypeInfo typeInfo) {

	Object parentInfo = this.infoStack.peek();
	JavaElement parentHandle= (JavaElement) this.handleStack.peek();
	JavaElement handle = createTypeHandle(parentHandle, typeInfo);
	 //NB: occurenceCount is computed in resolveDuplicates
	resolveDuplicates((SourceType) handle);
	this.infoStack.push(typeInfo);
	this.handleStack.push(handle);

	if (parentHandle.getElementType() == IJavaElement.TYPE)
		((TypeInfo) parentInfo).childrenCategories.put(handle, typeInfo.categories);
	addToChildren(parentInfo, handle);
}
private org.eclipse.jdt.internal.core.ModuleDescriptionInfo createModuleInfo(ModuleInfo modInfo, org.eclipse.jdt.internal.core.SourceModule handle) {
	org.eclipse.jdt.internal.core.ModuleDescriptionInfo info = org.eclipse.jdt.internal.core.ModuleDescriptionInfo.createModule(modInfo.node);
	info.setHandle(handle);
	info.setSourceRangeStart(modInfo.declarationStart);
	info.setFlags(modInfo.modifiers);
	info.setNameSourceStart(modInfo.nameSourceStart);
	info.setNameSourceEnd(modInfo.nameSourceEnd);
	info.addCategories(handle, modInfo.categories);
	if (modInfo.annotations != null) {
		int length = modInfo.annotations.length;
		for (int i = 0; i < length; i++) {
			org.eclipse.jdt.internal.compiler.ast.Annotation annotation = modInfo.annotations[i];
			acceptAnnotation(annotation, info, handle);
		}
	}
	this.newElements.put(handle, info);

	return info;
}
private SourceTypeElementInfo createTypeInfo(TypeInfo typeInfo, SourceType handle) {
	SourceTypeElementInfo info =
		typeInfo.anonymousMember ?
			new SourceTypeElementInfo() {
				@Override
				public boolean isAnonymousMember() {
					return true;
				}
			} :
		new SourceTypeElementInfo();
	info.setHandle(handle);
	info.setSourceRangeStart(typeInfo.declarationStart);
	info.setFlags(typeInfo.modifiers);
	info.setNameSourceStart(typeInfo.nameSourceStart);
	info.setNameSourceEnd(typeInfo.nameSourceEnd);
	JavaModelManager manager = JavaModelManager.getJavaModelManager();
	char[] superclass = typeInfo.superclass;
	info.setSuperclassName(superclass == null ? null : manager.intern(superclass));
	char[][] typeNames = typeInfo.superinterfaces;
	for (int i = 0, length = typeNames == null ? 0 : typeNames.length; i < length; i++)
		typeNames[i] = manager.intern(typeNames[i]);
	info.setSuperInterfaceNames(typeNames);
	typeNames = typeInfo.permittedSubtypes;
	for (int i = 0, length = typeNames == null ? 0 : typeNames.length; i < length; i++)
		typeNames[i] = manager.intern(typeNames[i]);
	info.setPermittedSubtypeNames(typeNames);
	info.addCategories(handle, typeInfo.categories);
	this.newElements.put(handle, info);

	if (typeInfo.typeParameters != null) {
		for (int i = 0, length = typeInfo.typeParameters.length; i < length; i++) {
			TypeParameterInfo typeParameterInfo = typeInfo.typeParameters[i];
			acceptTypeParameter(typeParameterInfo, info);
		}
	}
	if (typeInfo.annotations != null) {
		int length = typeInfo.annotations.length;
		this.unitInfo.annotationNumber += length;
		for (int i = 0; i < length; i++) {
			org.eclipse.jdt.internal.compiler.ast.Annotation annotation = typeInfo.annotations[i];
			acceptAnnotation(annotation, info, handle);
		}
	}
	if (typeInfo.childrenCategories != null) {
		Iterator iterator = typeInfo.childrenCategories.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry entry = (Map.Entry) iterator.next();
			info.addCategories((IJavaElement) entry.getKey(), (char[][]) entry.getValue());
		}

	}
	if (typeInfo.typeAnnotated) {
		this.unitInfo.annotationNumber = CompilationUnitElementInfo.ANNOTATION_THRESHOLD_FOR_DIET_PARSE;
	}
	return info;
}
protected void acceptTypeParameter(TypeParameterInfo typeParameterInfo, JavaElementInfo parentInfo) {
	JavaElement parentHandle = (JavaElement) this.handleStack.peek();
	String nameString = new String(typeParameterInfo.name);
	TypeParameter handle = createTypeParameter(parentHandle, nameString); //NB: occurenceCount is computed in resolveDuplicates
	resolveDuplicates(handle);

	TypeParameterElementInfo info = new TypeParameterElementInfo();
	info.setSourceRangeStart(typeParameterInfo.declarationStart);
	info.nameStart = typeParameterInfo.nameSourceStart;
	info.nameEnd = typeParameterInfo.nameSourceEnd;
	info.bounds = typeParameterInfo.bounds;
	if (parentInfo instanceof SourceTypeElementInfo) {
		SourceTypeElementInfo elementInfo = (SourceTypeElementInfo) parentInfo;
		ITypeParameter[] typeParameters = elementInfo.typeParameters;
		int length = typeParameters.length;
		System.arraycopy(typeParameters, 0, typeParameters = new ITypeParameter[length+1], 0, length);
		typeParameters[length] = handle;
		elementInfo.typeParameters = typeParameters;
	} else {
		SourceMethodElementInfo elementInfo = (SourceMethodElementInfo) parentInfo;
		ITypeParameter[] typeParameters = elementInfo.typeParameters;
		int length = typeParameters.length;
		System.arraycopy(typeParameters, 0, typeParameters = new ITypeParameter[length+1], 0, length);
		typeParameters[length] = handle;
		elementInfo.typeParameters = typeParameters;
	}
	this.newElements.put(handle, info);
	info.setSourceRangeEnd(typeParameterInfo.declarationEnd);
	if (typeParameterInfo.typeAnnotated) {
		this.unitInfo.annotationNumber = CompilationUnitElementInfo.ANNOTATION_THRESHOLD_FOR_DIET_PARSE;
	}
}
/**
 * @see ISourceElementRequestor
 */
@Override
public void exitCompilationUnit(int declarationEnd) {
	// set import container children
	if (this.importContainerInfo != null) {
		this.importContainerInfo.children = getChildren(this.importContainerInfo);
	}

	this.unitInfo.children = getChildren(this.unitInfo);
	this.unitInfo.setSourceLength(declarationEnd + 1);

	// determine if there were any parsing errors
	this.unitInfo.setIsStructureKnown(!this.hasSyntaxErrors);
}
/**
 * @see ISourceElementRequestor
 */
@Override
public void exitConstructor(int declarationEnd) {
	exitMethod(declarationEnd, null);
}
/**
 * @see ISourceElementRequestor
 */
@Override
public void exitField(int initializationStart, int declarationEnd, int declarationSourceEnd) {
	JavaElement handle = (JavaElement) this.handleStack.peek();
	FieldInfo fieldInfo = (FieldInfo) this.infoStack.peek();
	IJavaElement[] elements = getChildren(fieldInfo);
	SourceFieldElementInfo info = elements.length == 0 ? new SourceFieldElementInfo() : new SourceFieldWithChildrenInfo(elements);
	info.setNameSourceStart(fieldInfo.nameSourceStart);
	info.setNameSourceEnd(fieldInfo.nameSourceEnd);
	info.setSourceRangeStart(fieldInfo.declarationStart);
	info.setFlags(fieldInfo.modifiers);
	char[] typeName = JavaModelManager.getJavaModelManager().intern(fieldInfo.type);
	info.setTypeName(typeName);
	this.newElements.put(handle, info);

	if (fieldInfo.annotations != null) {
		int length = fieldInfo.annotations.length;
		this.unitInfo.annotationNumber += length;
		for (int i = 0; i < length; i++) {
			org.eclipse.jdt.internal.compiler.ast.Annotation annotation = fieldInfo.annotations[i];
			acceptAnnotation(annotation, info, handle);
		}
	}
	info.setSourceRangeEnd(declarationSourceEnd);
	this.handleStack.pop();
	this.infoStack.pop();

	// remember initializer source if field is a constant
	if (initializationStart != -1) {
		int flags = info.flags;
		Object typeInfo;
		if (Flags.isFinal(flags)
				|| ((typeInfo = this.infoStack.peek()) instanceof TypeInfo
					 && (Flags.isInterface(((TypeInfo)typeInfo).modifiers)))) {
			int length = declarationEnd - initializationStart;
			if (length > 0) {
				char[] initializer = new char[length];
				System.arraycopy(this.parser.scanner.source, initializationStart, initializer, 0, length);
				info.initializationSource = initializer;
			}
		}
	}
	if (fieldInfo.typeAnnotated) {
		this.unitInfo.annotationNumber = CompilationUnitElementInfo.ANNOTATION_THRESHOLD_FOR_DIET_PARSE;
	}
}
/**
 * @see ISourceElementRequestor
 */
@Override
public void exitRecordComponent(int declarationEnd, int declarationSourceEnd) {
	JavaElement handle = (JavaElement) this.handleStack.peek();
	FieldInfo compInfo = (FieldInfo) this.infoStack.peek();
	IJavaElement[] elements = getChildren(compInfo);
	SourceFieldElementInfo info = elements.length == 0 ? new SourceFieldElementInfo() : new SourceFieldWithChildrenInfo(elements);
	info.isRecordComponent = true;
	info.setNameSourceStart(compInfo.nameSourceStart);
	info.setNameSourceEnd(compInfo.nameSourceEnd);
	info.setSourceRangeStart(compInfo.declarationStart);
	info.setFlags(compInfo.modifiers);
	char[] typeName = JavaModelManager.getJavaModelManager().intern(compInfo.type);
	info.setTypeName(typeName);
	this.newElements.put(handle, info);

	if (compInfo.annotations != null) {
		int length = compInfo.annotations.length;
		this.unitInfo.annotationNumber += length;
		for (int i = 0; i < length; i++) {
			org.eclipse.jdt.internal.compiler.ast.Annotation annotation = compInfo.annotations[i];
			acceptAnnotation(annotation, info, handle);
		}
	}
	info.setSourceRangeEnd(declarationSourceEnd);
	this.handleStack.pop();
	this.infoStack.pop();

	if (compInfo.typeAnnotated) {
		this.unitInfo.annotationNumber = CompilationUnitElementInfo.ANNOTATION_THRESHOLD_FOR_DIET_PARSE;
	}
}
/**
 * @see ISourceElementRequestor
 */
@Override
public void exitInitializer(int declarationEnd) {
	JavaElement handle = (JavaElement) this.handleStack.peek();
	int[] initializerInfo = (int[]) this.infoStack.peek();
	IJavaElement[] elements = getChildren(initializerInfo);

	InitializerElementInfo info = elements.length == 0 ? new InitializerElementInfo() : new InitializerWithChildrenInfo(elements);
	info.setSourceRangeStart(initializerInfo[0]);
	info.setFlags(initializerInfo[1]);
	info.setSourceRangeEnd(declarationEnd);

	this.newElements.put(handle, info);

	this.handleStack.pop();
	this.infoStack.pop();
}
/**
 * @see ISourceElementRequestor
 */
@Override
public void exitMethod(int declarationEnd, Expression defaultValue) {
	SourceMethod handle = (SourceMethod) this.handleStack.peek();
	MethodInfo methodInfo = (MethodInfo) this.infoStack.peek();

	SourceMethodElementInfo info = createMethodInfo(methodInfo, handle);
	info.setSourceRangeEnd(declarationEnd);

	// remember default value of annotation method
	if (info.isAnnotationMethod() && defaultValue != null) {
		SourceAnnotationMethodInfo annotationMethodInfo = (SourceAnnotationMethodInfo) info;
		annotationMethodInfo.defaultValueStart = defaultValue.sourceStart;
		annotationMethodInfo.defaultValueEnd = defaultValue.sourceEnd;
		JavaElement element = (JavaElement) this.handleStack.peek();
		org.eclipse.jdt.internal.core.MemberValuePair defaultMemberValuePair = new org.eclipse.jdt.internal.core.MemberValuePair(element.getElementName());
		defaultMemberValuePair.value = getMemberValue(defaultMemberValuePair, defaultValue);
		annotationMethodInfo.defaultValue = defaultMemberValuePair;
	}

	this.handleStack.pop();
	this.infoStack.pop();
}
@Override
public void exitModule(int declarationEnd) {
	ModuleInfo moduleInfo = (ModuleInfo) this.infoStack.peek();
	SourceModule handle = (SourceModule) this.handleStack.peek();
	JavaProject proj = (JavaProject) handle.getAncestor(IJavaElement.JAVA_PROJECT);
	if (proj != null) {
		try {
			org.eclipse.jdt.internal.core.SourceModule moduleDecl = handle;
			org.eclipse.jdt.internal.core.ModuleDescriptionInfo info = createModuleInfo(moduleInfo, moduleDecl);
			info.setSourceRangeEnd(declarationEnd);
			info.children = getChildren(info);
			this.unitInfo.setModule(moduleDecl);
			proj.setModuleDescription(moduleDecl);
		} catch (JavaModelException e) {
			// Unexpected while creating
		}
	}
	this.handleStack.pop();
	this.infoStack.pop();
}
/**
 * @see ISourceElementRequestor
 */
@Override
public void exitType(int declarationEnd) {
	TypeInfo typeInfo = (TypeInfo) this.infoStack.peek();
	SourceType handle = (SourceType) this.handleStack.peek();
	SourceTypeElementInfo info = createTypeInfo(typeInfo, handle);
	info.setSourceRangeEnd(declarationEnd);
	info.children = getChildren(typeInfo);
	this.handleStack.pop();
	this.infoStack.pop();
}
/**
 * Resolves duplicate handles by incrementing the occurrence count
 * of the handle being created.
 */
protected void resolveDuplicates(SourceRefElement handle) {
	Integer occurenceCount = this.occurenceCounts.get(handle);
	if (occurenceCount == null)
		this.occurenceCounts.put(handle, Integer.valueOf(1));
	else {
		this.occurenceCounts.put(handle, Integer.valueOf(occurenceCount.intValue() + 1));
		handle.occurrenceCount = occurenceCount.intValue() + 1;
	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=342393
	// For anonymous source types, the occurrence count should be in the context
	// of the enclosing type.
	if (handle instanceof SourceType && ((SourceType) handle).isAnonymous()) {
		Object key = handle.getParent().getAncestor(IJavaElement.TYPE);
		occurenceCount = this.localOccurrenceCounts.get(key);
		if (occurenceCount == null)
			this.localOccurrenceCounts.put(key, Integer.valueOf(1));
		else {
			this.localOccurrenceCounts.put(key, Integer.valueOf(occurenceCount.intValue() + 1));
			((SourceType)handle).localOccurrenceCount = occurenceCount.intValue() + 1;
		}
	}
}
protected IMemberValuePair getMemberValuePair(MemberValuePair memberValuePair) {
	String memberName = new String(memberValuePair.name);
	org.eclipse.jdt.internal.core.MemberValuePair result = new org.eclipse.jdt.internal.core.MemberValuePair(memberName);
	result.value = getMemberValue(result, memberValuePair.value);
	return result;
}
protected IMemberValuePair[] getMemberValuePairs(MemberValuePair[] memberValuePairs) {
	int membersLength = memberValuePairs.length;
	IMemberValuePair[] members = new IMemberValuePair[membersLength];
	for (int j = 0; j < membersLength; j++) {
		members[j] = getMemberValuePair(memberValuePairs[j]);
	}
	return members;
}
private IJavaElement[] getChildren(Object info) {
	ArrayList childrenList = (ArrayList) this.children.get(info);
	if (childrenList != null) {
		return (IJavaElement[]) childrenList.toArray(new IJavaElement[childrenList.size()]);
	}
	return JavaElement.NO_ELEMENTS;
}
/*
 * Creates the value from the given expression, and sets the valueKind on the given memberValuePair
 */
protected Object getMemberValue(org.eclipse.jdt.internal.core.MemberValuePair memberValuePair, Expression expression) {
	if (expression instanceof NullLiteral) {
		return null;
	} else if (expression instanceof Literal) {
		((Literal) expression).computeConstant();
		return Util.getAnnotationMemberValue(memberValuePair, expression.constant);
	} else if (expression instanceof org.eclipse.jdt.internal.compiler.ast.Annotation) {
		org.eclipse.jdt.internal.compiler.ast.Annotation annotation = (org.eclipse.jdt.internal.compiler.ast.Annotation) expression;
		Object handle = acceptAnnotation(annotation, null, (JavaElement) this.handleStack.peek());
		memberValuePair.valueKind = IMemberValuePair.K_ANNOTATION;
		return handle;
	} else if (expression instanceof ClassLiteralAccess) {
		ClassLiteralAccess classLiteral = (ClassLiteralAccess) expression;
		char[] name = CharOperation.concatWith(classLiteral.type.getTypeName(), '.');
		memberValuePair.valueKind = IMemberValuePair.K_CLASS;
		return new String(name);
	} else if (expression instanceof QualifiedNameReference) {
		char[] qualifiedName = CharOperation.concatWith(((QualifiedNameReference) expression).tokens, '.');
		memberValuePair.valueKind = IMemberValuePair.K_QUALIFIED_NAME;
		return new String(qualifiedName);
	} else if (expression instanceof SingleNameReference) {
		char[] simpleName = ((SingleNameReference) expression).token;
		if (simpleName == RecoveryScanner.FAKE_IDENTIFIER) {
			memberValuePair.valueKind = IMemberValuePair.K_UNKNOWN;
			return null;
		}
		memberValuePair.valueKind = IMemberValuePair.K_SIMPLE_NAME;
		return new String(simpleName);
	} else if (expression instanceof ArrayInitializer) {
		memberValuePair.valueKind = -1; // modified below by the first call to getMemberValue(...)
		Expression[] expressions = ((ArrayInitializer) expression).expressions;
		int length = expressions == null ? 0 : expressions.length;
		Object[] values = new Object[length];
		for (int i = 0; i < length; i++) {
			int previousValueKind = memberValuePair.valueKind;
			Object value = getMemberValue(memberValuePair, expressions[i]);
			if (previousValueKind != -1 && memberValuePair.valueKind != previousValueKind) {
				// values are heterogeneous, value kind is thus unknown
				memberValuePair.valueKind = IMemberValuePair.K_UNKNOWN;
			}
			values[i] = value;
		}
		if (memberValuePair.valueKind == -1)
			memberValuePair.valueKind = IMemberValuePair.K_UNKNOWN;
		return values;
	} else if (expression instanceof UnaryExpression) {			// to deal with negative numerals (see bug - 248312)
		UnaryExpression unaryExpression = (UnaryExpression) expression;
		if ((unaryExpression.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT == OperatorIds.MINUS) {
			if (unaryExpression.expression instanceof Literal) {
				Literal subExpression = (Literal) unaryExpression.expression;
				subExpression.computeConstant();
				return Util.getNegativeAnnotationMemberValue(memberValuePair, subExpression.constant);
			}
		}
		memberValuePair.valueKind = IMemberValuePair.K_UNKNOWN;
		return null;
	} else {
		memberValuePair.valueKind = IMemberValuePair.K_UNKNOWN;
		return null;
	}
}
}
