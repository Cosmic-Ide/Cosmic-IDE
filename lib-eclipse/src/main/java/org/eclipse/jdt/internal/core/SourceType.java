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
package org.eclipse.jdt.internal.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.*;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.codeassist.CompletionEngine;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.core.hierarchy.TypeHierarchy;
import org.eclipse.jdt.internal.core.util.MementoTokenizer;
import org.eclipse.jdt.internal.core.util.Messages;

/**
 * Handle for a source type. Info object is a SourceTypeElementInfo.
 *
 * Note: Parent is either an IClassFile, an ICompilationUnit or an IType.
 *
 * @see IType
 */

@SuppressWarnings({ "rawtypes", "unchecked" })
public class SourceType extends NamedMember implements IType {

/*
 * A count to uniquely identify this element within the context of the enclosing type.
 * Currently this is computed and used only for anonymous types.
 */
public int localOccurrenceCount = 1;
private static final IField[] NO_FIELDS = new IField[0];

protected SourceType(JavaElement parent, String name) {
	super(parent, name);
}
@Override
protected void closing(Object info) throws JavaModelException {
	super.closing(info);
	SourceTypeElementInfo elementInfo = (SourceTypeElementInfo) info;
	ITypeParameter[] typeParameters = elementInfo.typeParameters;
	for (int i = 0, length = typeParameters.length; i < length; i++) {
		((TypeParameter) typeParameters[i]).close();
	}
}
/**
 * @see IType
 * @deprecated
 */
@Override
public void codeComplete(char[] snippet,int insertion,int position,char[][] localVariableTypeNames,char[][] localVariableNames,int[] localVariableModifiers,boolean isStatic,ICompletionRequestor requestor) throws JavaModelException {
	codeComplete(snippet, insertion, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic, requestor, DefaultWorkingCopyOwner.PRIMARY);
}
/**
 * @see IType
 * @deprecated
 */
@Override
public void codeComplete(char[] snippet,int insertion,int position,char[][] localVariableTypeNames,char[][] localVariableNames,int[] localVariableModifiers,boolean isStatic,ICompletionRequestor requestor, WorkingCopyOwner owner) throws JavaModelException {
	if (requestor == null) {
		throw new IllegalArgumentException("Completion requestor cannot be null"); //$NON-NLS-1$
	}
	codeComplete(snippet, insertion, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic, new org.eclipse.jdt.internal.codeassist.CompletionRequestorWrapper(requestor), owner);

}
/**
 * @see IType
 */
@Override
public void codeComplete(char[] snippet,int insertion,int position,char[][] localVariableTypeNames,char[][] localVariableNames,int[] localVariableModifiers,boolean isStatic,CompletionRequestor requestor) throws JavaModelException {
	codeComplete(snippet, insertion, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic, requestor, DefaultWorkingCopyOwner.PRIMARY);
}
/**
 * @see IType
 */
@Override
public void codeComplete(char[] snippet,int insertion,int position,char[][] localVariableTypeNames,char[][] localVariableNames,int[] localVariableModifiers,boolean isStatic,CompletionRequestor requestor, IProgressMonitor monitor) throws JavaModelException {
	codeComplete(snippet, insertion, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic, requestor, DefaultWorkingCopyOwner.PRIMARY, monitor);
}
/**
 * @see IType
 */
@Override
public void codeComplete(char[] snippet,int insertion,int position,char[][] localVariableTypeNames,char[][] localVariableNames,int[] localVariableModifiers,boolean isStatic,CompletionRequestor requestor, WorkingCopyOwner owner) throws JavaModelException {
	codeComplete(snippet, insertion, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic, requestor, owner, null);
}
/**
 * @see IType
 */
@Override
public void codeComplete(
		char[] snippet,
		int insertion,
		int position,
		char[][] localVariableTypeNames,
		char[][] localVariableNames,
		int[] localVariableModifiers,
		boolean isStatic,
		CompletionRequestor requestor,
		WorkingCopyOwner owner,
		IProgressMonitor monitor) throws JavaModelException {
	if (requestor == null) {
		throw new IllegalArgumentException("Completion requestor cannot be null"); //$NON-NLS-1$
	}

	JavaProject project = getJavaProject();
	SearchableEnvironment environment = project.newSearchableNameEnvironment(owner, requestor.isTestCodeExcluded());
	CompletionEngine engine = new CompletionEngine(environment, requestor, project.getOptions(true), project, owner, monitor);

	String source = getCompilationUnit().getSource();
	if (source != null && insertion > -1 && insertion < source.length()) {

		char[] prefix = CharOperation.concat(source.substring(0, insertion).toCharArray(), new char[]{'{'});
		char[] suffix = CharOperation.concat(new char[]{'}'}, source.substring(insertion).toCharArray());
		char[] fakeSource = CharOperation.concat(prefix, snippet, suffix);

		BasicCompilationUnit cu =
			new BasicCompilationUnit(
				fakeSource,
				null,
				getElementName(),
				getParent());

		engine.complete(cu, prefix.length + position, prefix.length, null/*extended context isn't computed*/);
	} else {
		engine.complete(this, snippet, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic);
	}
	if (NameLookup.VERBOSE) {
		System.out.println(Thread.currentThread() + " TIME SPENT in NameLoopkup#seekTypesInSourcePackage: " + environment.nameLookup.timeSpentInSeekTypesInSourcePackage + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
		System.out.println(Thread.currentThread() + " TIME SPENT in NameLoopkup#seekTypesInBinaryPackage: " + environment.nameLookup.timeSpentInSeekTypesInBinaryPackage + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
	}
}
/**
 * @see IType
 */
@Override
public IField createField(String contents, IJavaElement sibling, boolean force, IProgressMonitor monitor) throws JavaModelException {
	CreateFieldOperation op = new CreateFieldOperation(this, contents, force);
	if (sibling != null) {
		op.createBefore(sibling);
	}
	op.runOperation(monitor);
	return (IField) op.getResultElements()[0];
}
/**
 * @see IType
 */
@Override
public IInitializer createInitializer(String contents, IJavaElement sibling, IProgressMonitor monitor) throws JavaModelException {
	CreateInitializerOperation op = new CreateInitializerOperation(this, contents);
	if (sibling != null) {
		op.createBefore(sibling);
	}
	op.runOperation(monitor);
	return (IInitializer) op.getResultElements()[0];
}
/**
 * @see IType
 */
@Override
public IMethod createMethod(String contents, IJavaElement sibling, boolean force, IProgressMonitor monitor) throws JavaModelException {
	CreateMethodOperation op = new CreateMethodOperation(this, contents, force);
	if (sibling != null) {
		op.createBefore(sibling);
	}
	op.runOperation(monitor);
	return (IMethod) op.getResultElements()[0];
}
/**
 * @see IType
 */
@Override
public IType createType(String contents, IJavaElement sibling, boolean force, IProgressMonitor monitor) throws JavaModelException {
	CreateTypeOperation op = new CreateTypeOperation(this, contents, force);
	if (sibling != null) {
		op.createBefore(sibling);
	}
	op.runOperation(monitor);
	return (IType) op.getResultElements()[0];
}
@Override
public boolean equals(Object o) {
	if (!(o instanceof SourceType)) return false;
	if (((SourceType) o).isLambda())
		return false;
	return super.equals(o);
}
/*
 * @see IType
 */
@Override
public IMethod[] findMethods(IMethod method) {
	try {
		return findMethods(method, getMethods());
	} catch (JavaModelException e) {
		// if type doesn't exist, no matching method can exist
		return null;
	}
}
@Override
public IAnnotation[] getAnnotations() throws JavaModelException {
	AnnotatableInfo info = (AnnotatableInfo) getElementInfo();
	return info.annotations;
}
@Override
public IJavaElement[] getChildrenForCategory(String category) throws JavaModelException {
	IJavaElement[] children = getChildren();
	int length = children.length;
	if (length == 0) return NO_ELEMENTS;
	SourceTypeElementInfo info = (SourceTypeElementInfo) getElementInfo();
	HashMap categories = info.getCategories();
	if (categories == null) return NO_ELEMENTS;
	IJavaElement[] result = new IJavaElement[length];
	int index = 0;
	for (int i = 0; i < length; i++) {
		IJavaElement child = children[i];
		String[] elementCategories = (String[]) categories.get(child);
		if (elementCategories != null)
			for (int j = 0, length2 = elementCategories.length; j < length2; j++) {
				if (elementCategories[j].equals(category))
					result[index++] = child;
			}
	}
	if (index == 0) return NO_ELEMENTS;
	if (index < length)
		System.arraycopy(result, 0, result = new IJavaElement[index], 0, index);
	return result;
}
/**
 * @see IMember
 */
@Override
public IType getDeclaringType() {
	IJavaElement parentElement = getParent();
	while (parentElement != null) {
		if (parentElement.getElementType() == IJavaElement.TYPE) {
			return (IType) parentElement;
		} else
			if (parentElement instanceof IMember) {
				parentElement = parentElement.getParent();
			} else {
				return null;
			}
	}
	return null;
}
@Override
public IOrdinaryClassFile getClassFile() {
	return null;
}
/**
 * @see IJavaElement
 */
@Override
public int getElementType() {
	return TYPE;
}
/**
 * @see IType#getField
 */
@Override
public IField getField(String fieldName) {
	return new SourceField(this, fieldName);
}
/**
 * @see IType
 */
@Override
public IField[] getFields() throws JavaModelException {
	if (!isRecord()) {
		ArrayList list = getChildrenOfType(FIELD);
		if (list.size() == 0) {
			return NO_FIELDS;
		}
		IField[] array= new IField[list.size()];
		list.toArray(array);
		return array;
	}
	return getFieldsOrComponents(false);
}
@Override
public IField[] getRecordComponents() throws JavaModelException {
	if (!isRecord())
		return NO_FIELDS;
	return getFieldsOrComponents(true);
}
@Override
public String[] getPermittedSubtypeNames() throws JavaModelException {
	SourceTypeElementInfo info = (SourceTypeElementInfo) getElementInfo();
	char[][] names= info.getPermittedSubtypeNames();
	return CharOperation.toStrings(names);
}
private IField[] getFieldsOrComponents(boolean component) throws JavaModelException {
	ArrayList list = getChildrenOfType(FIELD);
	if (list.size() == 0) {
		return NO_FIELDS;
	}
	ArrayList<IField> fields = new ArrayList<>();
	for (Object object : list) {
		IField field = (IField) object;
		if (field.isRecordComponent() == component)
			fields.add(field);
	}
	IField[] array= new IField[fields.size()];
	fields.toArray(array);
	return array;
}
@Override
public IField getRecordComponent(String compName) {
	try {
		if (isRecord())
			return new SourceField(this, compName);
	} catch (JavaModelException e) {
		// Should throw an exception instead?
	}
	return null;
}
/**
 * @see IType#getFullyQualifiedName()
 */
@Override
public String getFullyQualifiedName() {
	return this.getFullyQualifiedName('$');
}
/**
 * @see IType#getFullyQualifiedName(char)
 */
@Override
public String getFullyQualifiedName(char enclosingTypeSeparator) {
	try {
		return getFullyQualifiedName(enclosingTypeSeparator, false/*don't show parameters*/);
	} catch (JavaModelException e) {
		// exception thrown only when showing parameters
		return null;
	}
}

@Override
public String getFullyQualifiedParameterizedName() throws JavaModelException {
	return getFullyQualifiedName('.', true/*show parameters*/);
}
/*
 * For source types, the occurrence count is the one computed in the context of the immediately enclosing type.
 *
 */
@Override
protected String getOccurrenceCountSignature() {
	return Integer.toString(this.localOccurrenceCount);
}
/*
 * @see JavaElement
 */
@Override
public IJavaElement getHandleFromMemento(String token, MementoTokenizer memento, WorkingCopyOwner workingCopyOwner) {
	switch (token.charAt(0)) {
		case JEM_COUNT:
			return getHandleUpdatingCountFromMemento(memento, workingCopyOwner);
		case JEM_FIELD:
			if (!memento.hasMoreTokens()) return this;
			String fieldName = memento.nextToken();
			JavaElement field = (JavaElement)getField(fieldName);
			return field.getHandleFromMemento(memento, workingCopyOwner);
		case JEM_INITIALIZER:
			if (!memento.hasMoreTokens()) return this;
			String count = memento.nextToken();
			JavaElement initializer = (JavaElement)getInitializer(Integer.parseInt(count));
			return initializer.getHandleFromMemento(memento, workingCopyOwner);
		case JEM_METHOD:
			if (!memento.hasMoreTokens()) return this;
			String selector = memento.nextToken();
			ArrayList params = new ArrayList();
			nextParam: while (memento.hasMoreTokens()) {
				token = memento.nextToken();
				switch (token.charAt(0)) {
					case JEM_TYPE:
					case JEM_TYPE_PARAMETER:
					case JEM_ANNOTATION:
						break nextParam;
					case JEM_METHOD:
						if (!memento.hasMoreTokens()) return this;
						String param = memento.nextToken();
						StringBuilder buffer = new StringBuilder();
						while (param.length() == 1 && Signature.C_ARRAY == param.charAt(0)) { // backward compatible with 3.0 mementos
							buffer.append(Signature.C_ARRAY);
							if (!memento.hasMoreTokens()) return this;
							param = memento.nextToken();
						}
						params.add(buffer.toString() + param);
						break;
					default:
						break nextParam;
				}
			}
			String[] parameters = new String[params.size()];
			params.toArray(parameters);
			JavaElement method = (JavaElement)getMethod(selector, parameters);
			switch (token.charAt(0)) {
				case JEM_LAMBDA_EXPRESSION:
				case JEM_TYPE:
				case JEM_TYPE_PARAMETER:
				case JEM_LOCALVARIABLE:
				case JEM_ANNOTATION:
					return method.getHandleFromMemento(token, memento, workingCopyOwner);
				default:
					return method;
			}
		case JEM_TYPE:
			String typeName;
			if (memento.hasMoreTokens()) {
				typeName = memento.nextToken();
				char firstChar = typeName.charAt(0);
				if (firstChar == JEM_FIELD || firstChar == JEM_INITIALIZER || firstChar == JEM_METHOD || firstChar == JEM_TYPE || firstChar == JEM_COUNT) {
					token = typeName;
					typeName = ""; //$NON-NLS-1$
				} else {
					token = null;
				}
			} else {
				typeName = ""; //$NON-NLS-1$
				token = null;
			}
			JavaElement type = (JavaElement)getType(typeName);
			if (token == null) {
				return type.getHandleFromMemento(memento, workingCopyOwner);
			} else {
				return type.getHandleFromMemento(token, memento, workingCopyOwner);
			}
		case JEM_TYPE_PARAMETER:
			if (!memento.hasMoreTokens()) return this;
			String typeParameterName = memento.nextToken();
			JavaElement typeParameter = new TypeParameter(this, typeParameterName);
			return typeParameter.getHandleFromMemento(memento, workingCopyOwner);
		case JEM_ANNOTATION:
			if (!memento.hasMoreTokens()) return this;
			String annotationName = memento.nextToken();
			JavaElement annotation = new Annotation(this, annotationName);
			return annotation.getHandleFromMemento(memento, workingCopyOwner);
	}
	return null;
}
/**
 * @see IType
 */
@Override
public IInitializer getInitializer(int count) {
	return new Initializer(this, count);
}
/**
 * @see IType
 */
@Override
public IInitializer[] getInitializers() throws JavaModelException {
	ArrayList list = getChildrenOfType(INITIALIZER);
	IInitializer[] array= new IInitializer[list.size()];
	list.toArray(array);
	return array;
}

@Override
public String getKey() {
	try {
		return getKey(this, false/*don't open*/);
	} catch (JavaModelException e) {
		// happen only if force open is true
		return null;
	}
}
/**
 * @see IType#getMethod
 */
@Override
public IMethod getMethod(String selector, String[] parameterTypeSignatures) {
	return new SourceMethod(this, selector, parameterTypeSignatures);
}
/**
 * @see IType
 */
@Override
public IMethod[] getMethods() throws JavaModelException {
	ArrayList list = getChildrenOfType(METHOD);
	IMethod[] array= new IMethod[list.size()];
	list.toArray(array);
	return array;
}
/**
 * @see IType
 */
@Override
public IPackageFragment getPackageFragment() {
	IJavaElement parentElement = this.getParent();
	while (parentElement != null) {
		if (parentElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
			return (IPackageFragment)parentElement;
		}
		else {
			parentElement = parentElement.getParent();
		}
	}
	Assert.isTrue(false);  // should not happen
	return null;
}

@Override
public JavaElement getPrimaryElement(boolean checkOwner) {
	if (checkOwner) {
		CompilationUnit cu = (CompilationUnit)getAncestor(COMPILATION_UNIT);
		if (cu.isPrimary()) return this;
	}
	IJavaElement primaryParent = this.getParent().getPrimaryElement(false);
	switch (primaryParent.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
			return (JavaElement)((ICompilationUnit)primaryParent).getType(this.name);
		case IJavaElement.TYPE:
			return (JavaElement)((IType)primaryParent).getType(this.name);
		case IJavaElement.FIELD:
		case IJavaElement.INITIALIZER:
		case IJavaElement.METHOD:
			return (JavaElement)((IMember)primaryParent).getType(this.name, this.occurrenceCount);
	}
	return this;
}
/**
 * @see IType
 */
@Override
public String getSuperclassName() throws JavaModelException {
	SourceTypeElementInfo info = (SourceTypeElementInfo) getElementInfo();
	char[] superclassName= info.getSuperclassName();
	if (superclassName == null) {
		return null;
	}
	return new String(superclassName);
}

/**
 * @see IType#getSuperclassTypeSignature()
 * @since 3.0
 */
@Override
public String getSuperclassTypeSignature() throws JavaModelException {
	SourceTypeElementInfo info = (SourceTypeElementInfo) getElementInfo();
	char[] superclassName= info.getSuperclassName();
	if (superclassName == null) {
		return null;
	}
	return Signature.createTypeSignature(superclassName, false);
}

/**
 * @see IType
 */
@Override
public String[] getSuperInterfaceNames() throws JavaModelException {
	SourceTypeElementInfo info = (SourceTypeElementInfo) getElementInfo();
	char[][] names= info.getInterfaceNames();
	return CharOperation.toStrings(names);
}

/**
 * @see IType#getSuperInterfaceTypeSignatures()
 * @since 3.0
 */
@Override
public String[] getSuperInterfaceTypeSignatures() throws JavaModelException {
	SourceTypeElementInfo info = (SourceTypeElementInfo) getElementInfo();
	char[][] names= info.getInterfaceNames();
	if (names == null) {
		return CharOperation.NO_STRINGS;
	}
	String[] strings= new String[names.length];
	for (int i= 0; i < names.length; i++) {
		strings[i]= Signature.createTypeSignature(names[i], false);
	}
	return strings;
}

@Override
public ITypeParameter[] getTypeParameters() throws JavaModelException {
	SourceTypeElementInfo info = (SourceTypeElementInfo) getElementInfo();
	return info.typeParameters;
}

/**
 * @see IType#getTypeParameterSignatures()
 * @since 3.0
 */
@Override
public String[] getTypeParameterSignatures() throws JavaModelException {
	ITypeParameter[] typeParameters = getTypeParameters();
	int length = typeParameters.length;
	String[] typeParameterSignatures = new String[length];
	for (int i = 0; i < length; i++) {
		TypeParameter typeParameter = (TypeParameter) typeParameters[i];
		TypeParameterElementInfo info = (TypeParameterElementInfo) typeParameter.getElementInfo();
		char[][] bounds = info.bounds;
		if (bounds == null) {
			typeParameterSignatures[i] = Signature.createTypeParameterSignature(typeParameter.getElementName(), CharOperation.NO_STRINGS);
		} else {
			int boundsLength = bounds.length;
			char[][] boundSignatures = new char[boundsLength][];
			for (int j = 0; j < boundsLength; j++) {
				boundSignatures[j] = Signature.createCharArrayTypeSignature(bounds[j], false);
			}
			typeParameterSignatures[i] = new String(Signature.createTypeParameterSignature(typeParameter.getElementName().toCharArray(), boundSignatures));
		}
	}
	return typeParameterSignatures;
}

/**
 * @see IType
 */
@Override
public IType getType(String typeName) {
	return new SourceType(this, typeName);
}
@Override
public ITypeParameter getTypeParameter(String typeParameterName) {
	return new TypeParameter(this, typeParameterName);
}
/**
 * @see IType#getTypeQualifiedName()
 */
@Override
public String getTypeQualifiedName() {
	return this.getTypeQualifiedName('$');
}
/**
 * @see IType#getTypeQualifiedName(char)
 */
@Override
public String getTypeQualifiedName(char enclosingTypeSeparator) {
	try {
		return getTypeQualifiedName(enclosingTypeSeparator, false/*don't show parameters*/);
	} catch (JavaModelException e) {
		// exception thrown only when showing parameters
		return null;
	}
}

/**
 * @see IType
 */
@Override
public IType[] getTypes() throws JavaModelException {
	ArrayList list= getChildrenOfType(TYPE);
	IType[] array= new IType[list.size()];
	list.toArray(array);
	return array;
}
/**
 * @see IType#isAnonymous()
 */
@Override
public boolean isAnonymous() {
	return this.name.length() == 0;
}

/**
 * @see IType
 */
@Override
public boolean isClass() throws JavaModelException {
	SourceTypeElementInfo info = (SourceTypeElementInfo) getElementInfo();
	return TypeDeclaration.kind(info.getModifiers()) == TypeDeclaration.CLASS_DECL;
}

/**
 * @see IType#isEnum()
 * @since 3.0
 */
@Override
public boolean isEnum() throws JavaModelException {
	SourceTypeElementInfo info = (SourceTypeElementInfo) getElementInfo();
	return TypeDeclaration.kind(info.getModifiers()) == TypeDeclaration.ENUM_DECL;
}

/**
 * @see IType#isRecord()
 * @since 3.26
 */
@Override
public boolean isRecord() throws JavaModelException {
	SourceTypeElementInfo info = (SourceTypeElementInfo) getElementInfo();
	return TypeDeclaration.kind(info.getModifiers()) == TypeDeclaration.RECORD_DECL;
}
/**
 * @see IType#isSealed()
 * @noreference This method is not intended to be referenced by clients as it is a part of Java preview feature.
 */
@Override
public boolean isSealed() throws JavaModelException {
	SourceTypeElementInfo info = (SourceTypeElementInfo) getElementInfo();
	return Flags.isSealed(info.getModifiers());
}

/**
 * @see IType
 */
@Override
public boolean isInterface() throws JavaModelException {
	SourceTypeElementInfo info = (SourceTypeElementInfo) getElementInfo();
	switch (TypeDeclaration.kind(info.getModifiers())) {
		case TypeDeclaration.INTERFACE_DECL:
		case TypeDeclaration.ANNOTATION_TYPE_DECL: // annotation is interface too
			return true;
	}
	return false;
}

/**
 * @see IType#isAnnotation()
 * @since 3.0
 */
@Override
public boolean isAnnotation() throws JavaModelException {
	SourceTypeElementInfo info = (SourceTypeElementInfo) getElementInfo();
	return TypeDeclaration.kind(info.getModifiers()) == TypeDeclaration.ANNOTATION_TYPE_DECL;
}

/**
 * @see IType#isLocal()
 */
@Override
public boolean isLocal() {
	switch (this.getParent().getElementType()) {
		case IJavaElement.METHOD:
		case IJavaElement.INITIALIZER:
		case IJavaElement.FIELD:
			return true;
		default:
			return false;
	}
}
/**
 * @see IType#isMember()
 */
@Override
public boolean isMember() {
	return getDeclaringType() != null;
}

@Override
public boolean isResolved() {
	return false;
}
/**
 * @see IType
 */
@Override
public ITypeHierarchy loadTypeHierachy(InputStream input, IProgressMonitor monitor) throws JavaModelException {
	return loadTypeHierachy(input, DefaultWorkingCopyOwner.PRIMARY, monitor);
}
/**
 * NOTE: This method is not part of the API has it is not clear clients would easily use it: they would need to
 * first make sure all working copies for the given owner exist before calling it. This is especially har at startup
 * time.
 * In case clients want this API, here is how it should be specified:
 * <p>
 * Loads a previously saved ITypeHierarchy from an input stream. A type hierarchy can
 * be stored using ITypeHierachy#store(OutputStream). A compilation unit of a
 * loaded type has the given owner if such a working copy exists, otherwise the type's
 * compilation unit is a primary compilation unit.
 *
 * Only hierarchies originally created by the following methods can be loaded:
 * <ul>
 * <li>IType#newSupertypeHierarchy(IProgressMonitor)</li>
 * <li>IType#newSupertypeHierarchy(WorkingCopyOwner, IProgressMonitor)</li>
 * <li>IType#newTypeHierarchy(IJavaProject, IProgressMonitor)</li>
 * <li>IType#newTypeHierarchy(IJavaProject, WorkingCopyOwner, IProgressMonitor)</li>
 * <li>IType#newTypeHierarchy(IProgressMonitor)</li>
 * <li>IType#newTypeHierarchy(WorkingCopyOwner, IProgressMonitor)</li>
 * </u>
 *
 * @param input stream where hierarchy will be read
 * @param monitor the given progress monitor
 * @return the stored hierarchy
 * @exception JavaModelException if the hierarchy could not be restored, reasons include:
 *      - type is not the focus of the hierarchy or
 *		- unable to read the input stream (wrong format, IOException during reading, ...)
 * @see ITypeHierarchy#store(java.io.OutputStream, IProgressMonitor)
 * @since 3.0
 */
public ITypeHierarchy loadTypeHierachy(InputStream input, WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaModelException {
	// TODO monitor should be passed to TypeHierarchy.load(...)
	return TypeHierarchy.load(this, input, owner);
}
/**
 * @see IType
 */
@Override
public ITypeHierarchy newSupertypeHierarchy(IProgressMonitor monitor) throws JavaModelException {
	return this.newSupertypeHierarchy(DefaultWorkingCopyOwner.PRIMARY, monitor);
}

@Override
public ITypeHierarchy newSupertypeHierarchy(
	ICompilationUnit[] workingCopies,
	IProgressMonitor monitor)
	throws JavaModelException {

	CreateTypeHierarchyOperation op= new CreateTypeHierarchyOperation(this, workingCopies, SearchEngine.createWorkspaceScope(), false);
	op.runOperation(monitor);
	return op.getResult();
}
/**
 * @param workingCopies the working copies that take precedence over their original compilation units
 * @param monitor the given progress monitor
 * @return a type hierarchy for this type containing this type and all of its supertypes
 * @exception JavaModelException if this element does not exist or if an
 *		exception occurs while accessing its corresponding resource.
 *
 * @see IType#newSupertypeHierarchy(IWorkingCopy[], IProgressMonitor)
 * @deprecated
 */
@Override
public ITypeHierarchy newSupertypeHierarchy(
	IWorkingCopy[] workingCopies,
	IProgressMonitor monitor)
	throws JavaModelException {

	ICompilationUnit[] copies;
	if (workingCopies == null) {
		copies = null;
	} else {
		int length = workingCopies.length;
		System.arraycopy(workingCopies, 0, copies = new ICompilationUnit[length], 0, length);
	}
	return newSupertypeHierarchy(copies, monitor);
}
/**
 * @see IType#newSupertypeHierarchy(WorkingCopyOwner, IProgressMonitor)
 */
@Override
public ITypeHierarchy newSupertypeHierarchy(
	WorkingCopyOwner owner,
	IProgressMonitor monitor)
	throws JavaModelException {

	ICompilationUnit[] workingCopies = JavaModelManager.getJavaModelManager().getWorkingCopies(owner, true/*add primary working copies*/);
	CreateTypeHierarchyOperation op= new CreateTypeHierarchyOperation(this, workingCopies, SearchEngine.createWorkspaceScope(), false);
	op.runOperation(monitor);
	return op.getResult();
}
/**
 * @see IType
 */
@Override
public ITypeHierarchy newTypeHierarchy(IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
	return newTypeHierarchy(project, DefaultWorkingCopyOwner.PRIMARY, monitor);
}
/**
 * @see IType#newTypeHierarchy(IJavaProject, WorkingCopyOwner, IProgressMonitor)
 */
@Override
public ITypeHierarchy newTypeHierarchy(IJavaProject project, WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaModelException {
	if (project == null) {
		throw new IllegalArgumentException(Messages.hierarchy_nullProject);
	}
	ICompilationUnit[] workingCopies = JavaModelManager.getJavaModelManager().getWorkingCopies(owner, true/*add primary working copies*/);
	ICompilationUnit[] projectWCs = null;
	if (workingCopies != null) {
		int length = workingCopies.length;
		projectWCs = new ICompilationUnit[length];
		int index = 0;
		for (int i = 0; i < length; i++) {
			ICompilationUnit wc = workingCopies[i];
			if (project.equals(wc.getJavaProject())) {
				projectWCs[index++] = wc;
			}
		}
		if (index != length) {
			System.arraycopy(projectWCs, 0, projectWCs = new ICompilationUnit[index], 0, index);
		}
	}
	CreateTypeHierarchyOperation op= new CreateTypeHierarchyOperation(
		this,
		projectWCs,
		project,
		true);
	op.runOperation(monitor);
	return op.getResult();
}
/**
 * @see IType
 */
@Override
public ITypeHierarchy newTypeHierarchy(IProgressMonitor monitor) throws JavaModelException {
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=228845, The new type hierarchy should consider changes in primary
	// working copy.
	return newTypeHierarchy(DefaultWorkingCopyOwner.PRIMARY, monitor);
}

@Override
public ITypeHierarchy newTypeHierarchy(
	ICompilationUnit[] workingCopies,
	IProgressMonitor monitor)
	throws JavaModelException {

	CreateTypeHierarchyOperation op= new CreateTypeHierarchyOperation(this, workingCopies, SearchEngine.createWorkspaceScope(), true);
	op.runOperation(monitor);
	return op.getResult();
}
/**
 * @see IType#newTypeHierarchy(IWorkingCopy[], IProgressMonitor)
 * @deprecated
 */
@Override
public ITypeHierarchy newTypeHierarchy(
	IWorkingCopy[] workingCopies,
	IProgressMonitor monitor)
	throws JavaModelException {

	ICompilationUnit[] copies;
	if (workingCopies == null) {
		copies = null;
	} else {
		int length = workingCopies.length;
		System.arraycopy(workingCopies, 0, copies = new ICompilationUnit[length], 0, length);
	}
	return newTypeHierarchy(copies, monitor);
}
/**
 * @see IType#newTypeHierarchy(WorkingCopyOwner, IProgressMonitor)
 */
@Override
public ITypeHierarchy newTypeHierarchy(
	WorkingCopyOwner owner,
	IProgressMonitor monitor)
	throws JavaModelException {

	ICompilationUnit[] workingCopies = JavaModelManager.getJavaModelManager().getWorkingCopies(owner, true/*add primary working copies*/);
	CreateTypeHierarchyOperation op= new CreateTypeHierarchyOperation(this, workingCopies, SearchEngine.createWorkspaceScope(), true);
	op.runOperation(monitor);
	return op.getResult();
}
@Override
public JavaElement resolved(Binding binding) {
	ResolvedSourceType resolvedHandle = new ResolvedSourceType(this.getParent(), this.name, new String(binding.computeUniqueKey()));
	resolvedHandle.occurrenceCount = this.occurrenceCount;
	resolvedHandle.localOccurrenceCount = this.localOccurrenceCount;
	return resolvedHandle;
}
/**
 * @private Debugging purposes
 */
@Override
protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
	buffer.append(tabString(tab));
	if (info == null) {
		if (isAnonymous()) {
			buffer.append("<anonymous #"); //$NON-NLS-1$
			buffer.append(this.occurrenceCount);
			buffer.append(">"); //$NON-NLS-1$
		} else {
			toStringName(buffer);
		}
		buffer.append(" (not open)"); //$NON-NLS-1$
	} else if (info == NO_INFO) {
		if (isAnonymous()) {
			buffer.append("<anonymous #"); //$NON-NLS-1$
			buffer.append(this.occurrenceCount);
			buffer.append(">"); //$NON-NLS-1$
		} else {
			toStringName(buffer);
		}
	} else {
		try {
			if (isSealed()) {
				buffer.append("sealed "); //$NON-NLS-1$
			}
			if (isRecord()) {
				buffer.append("record "); //$NON-NLS-1$
			} else if (isEnum()) {
				buffer.append("enum "); //$NON-NLS-1$
			} else if (isAnnotation()) {
				buffer.append("@interface "); //$NON-NLS-1$
			} else if (isInterface()) {
				buffer.append("interface "); //$NON-NLS-1$
			} else {
				buffer.append("class "); //$NON-NLS-1$
			}
			if (isAnonymous()) {
				buffer.append("<anonymous #"); //$NON-NLS-1$
				buffer.append(this.occurrenceCount);
				buffer.append(">"); //$NON-NLS-1$
			} else {
				toStringName(buffer);
			}
		} catch (JavaModelException e) {
			buffer.append("<JavaModelException in toString of " + getElementName()); //$NON-NLS-1$
		}
	}
}
@Override
public boolean isLambda() {
	return false;
}
}
