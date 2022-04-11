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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.codeassist.CompletionEngine;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.JavaModelManager.PerProjectInfo;
import org.eclipse.jdt.internal.core.hierarchy.TypeHierarchy;
import org.eclipse.jdt.internal.core.util.MementoTokenizer;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * Parent is an IClassFile.
 *
 * @see IType
 */

@SuppressWarnings({ "rawtypes", "unchecked" })
public class BinaryType extends BinaryMember implements IType, SuffixConstants {

	private static final IField[] NO_FIELDS = new IField[0];
	private static final IMethod[] NO_METHODS = new IMethod[0];
	private static final IType[] NO_TYPES = new IType[0];
	private static final IInitializer[] NO_INITIALIZERS = new IInitializer[0];
	public static final JavadocContents EMPTY_JAVADOC = new JavadocContents(null, org.eclipse.jdt.internal.compiler.util.Util.EMPTY_STRING);

protected BinaryType(JavaElement parent, String name) {
	super(parent, name);
}
/*
 * Remove my cached children from the Java Model
 */
@Override
protected void closing(Object info) throws JavaModelException {
	ClassFileInfo cfi = getClassFileInfo();
	cfi.removeBinaryChildren();
}

/**
 * @see IType#codeComplete(char[], int, int, char[][], char[][], int[], boolean, ICompletionRequestor)
 * @deprecated
 */
@Override
public void codeComplete(char[] snippet,int insertion,int position,char[][] localVariableTypeNames,char[][] localVariableNames,int[] localVariableModifiers,boolean isStatic,ICompletionRequestor requestor) throws JavaModelException {
	codeComplete(snippet, insertion, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic, requestor, DefaultWorkingCopyOwner.PRIMARY);
}

/**
 * @see IType#codeComplete(char[], int, int, char[][], char[][], int[], boolean, ICompletionRequestor, WorkingCopyOwner)
 * @deprecated
 */
@Override
public void codeComplete(char[] snippet,int insertion,int position,char[][] localVariableTypeNames,char[][] localVariableNames,int[] localVariableModifiers,boolean isStatic,ICompletionRequestor requestor, WorkingCopyOwner owner) throws JavaModelException {
	if (requestor == null) {
		throw new IllegalArgumentException("Completion requestor cannot be null"); //$NON-NLS-1$
	}
	codeComplete(snippet, insertion, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic, new org.eclipse.jdt.internal.codeassist.CompletionRequestorWrapper(requestor), owner);
}

@Override
public void codeComplete(char[] snippet,int insertion,int position,char[][] localVariableTypeNames,char[][] localVariableNames,int[] localVariableModifiers,boolean isStatic,CompletionRequestor requestor) throws JavaModelException {
	codeComplete(snippet, insertion, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic, requestor, DefaultWorkingCopyOwner.PRIMARY);
}

@Override
public void codeComplete(char[] snippet,int insertion,int position,char[][] localVariableTypeNames,char[][] localVariableNames,int[] localVariableModifiers,boolean isStatic,CompletionRequestor requestor, IProgressMonitor monitor) throws JavaModelException {
	codeComplete(snippet, insertion, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic, requestor, DefaultWorkingCopyOwner.PRIMARY, monitor);
}

@Override
public void codeComplete(char[] snippet,int insertion,int position,char[][] localVariableTypeNames,char[][] localVariableNames,int[] localVariableModifiers,boolean isStatic,CompletionRequestor requestor, WorkingCopyOwner owner) throws JavaModelException {
	codeComplete(snippet, insertion, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic, requestor, owner, null);
}

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

	String source = getClassFile().getSource();
	if (source != null && insertion > -1 && insertion < source.length()) {
		// code complete

		char[] prefix = CharOperation.concat(source.substring(0, insertion).toCharArray(), new char[]{'{'});
		char[] suffix =  CharOperation.concat(new char[]{'}'}, source.substring(insertion).toCharArray());
		char[] fakeSource = CharOperation.concat(prefix, snippet, suffix);

		BasicCompilationUnit cu =
			new BasicCompilationUnit(
				fakeSource,
				null,
				getElementName(),
				project); // use project to retrieve corresponding .java IFile

		engine.complete(cu, prefix.length + position, prefix.length, null/*extended context isn't computed*/);
	} else {
		engine.complete(this, snippet, position, localVariableTypeNames, localVariableNames, localVariableModifiers, isStatic);
	}
	if (NameLookup.VERBOSE) {
		System.out.println(Thread.currentThread() + " TIME SPENT in NameLoopkup#seekTypesInSourcePackage: " + environment.nameLookup.timeSpentInSeekTypesInSourcePackage + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
		System.out.println(Thread.currentThread() + " TIME SPENT in NameLoopkup#seekTypesInBinaryPackage: " + environment.nameLookup.timeSpentInSeekTypesInBinaryPackage + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
	}
}

@Override
public IField createField(String contents, IJavaElement sibling, boolean force, IProgressMonitor monitor) throws JavaModelException {
	throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.READ_ONLY, this));
}

@Override
public IInitializer createInitializer(String contents, IJavaElement sibling, IProgressMonitor monitor) throws JavaModelException {
	throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.READ_ONLY, this));
}

@Override
public IMethod createMethod(String contents, IJavaElement sibling, boolean force, IProgressMonitor monitor) throws JavaModelException {
	throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.READ_ONLY, this));
}

@Override
public IType createType(String contents, IJavaElement sibling, boolean force, IProgressMonitor monitor) throws JavaModelException {
	throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.READ_ONLY, this));
}
@Override
public boolean equals(Object o) {
	if (!(o instanceof BinaryType)) return false;
	return super.equals(o);
}

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
	IBinaryType info = (IBinaryType) getElementInfo();
	IBinaryAnnotation[] binaryAnnotations = info.getAnnotations();
	return getAnnotations(binaryAnnotations, info.getTagBits());
}

@Override
public IJavaElement[] getChildren() throws JavaModelException {
	ClassFileInfo cfi = getClassFileInfo();
	return cfi.binaryChildren;
}
@Override
public IJavaElement[] getChildrenForCategory(String category) throws JavaModelException {
	IJavaElement[] children = getChildren();
	int length = children.length;
	if (length == 0) return children;
	SourceMapper mapper= getSourceMapper();
	if (mapper != null) {
		// ensure the class file's buffer is open so that categories are computed
		getClassFile().getBuffer();

		HashMap categories = mapper.categories;
		IJavaElement[] result = new IJavaElement[length];
		int index = 0;
		if (categories != null) {
			for (int i = 0; i < length; i++) {
				IJavaElement child = children[i];
				String[] cats = (String[]) categories.get(child);
				if (cats != null) {
					for (int j = 0, length2 = cats.length; j < length2; j++) {
						if (cats[j].equals(category)) {
							result[index++] = child;
							break;
						}
					}
				}
			}
		}
		if (index < length)
			System.arraycopy(result, 0, result = new IJavaElement[index], 0, index);
		return result;
	}
	return NO_ELEMENTS;
}
protected ClassFileInfo getClassFileInfo() throws JavaModelException {
	return (ClassFileInfo) this.getParent().getElementInfo();
}
@Override
public IOrdinaryClassFile getClassFile() {
	return (IOrdinaryClassFile) super.getClassFile();
}
@Override
public IType getDeclaringType() {
	IClassFile classFile = getClassFile();
	if (classFile.isOpen()) {
		try {
			char[] enclosingTypeName = ((IBinaryType) getElementInfo()).getEnclosingTypeName();
			if (enclosingTypeName == null) {
				return null;
			}
		 	enclosingTypeName = ClassFile.unqualifiedName(enclosingTypeName);

			// workaround problem with class files compiled with javac 1.1.*
			// that return a non-null enclosing type name for local types defined in anonymous (e.g. A$1$B)
			if (classFile.getElementName().length() > enclosingTypeName.length+1
					&& Character.isDigit(classFile.getElementName().charAt(enclosingTypeName.length+1))) {
				return null;
			}

			return getPackageFragment().getOrdinaryClassFile(new String(enclosingTypeName) + SUFFIX_STRING_class).getType();
		} catch (JavaModelException npe) {
			return null;
		}
	} else {
		// cannot access .class file without opening it
		// and getDeclaringType() is supposed to be a handle-only method,
		// so default to assuming $ is an enclosing type separator
		String classFileName = classFile.getElementName();
		int lastDollar = -1;
		for (int i = 0, length = classFileName.length(); i < length; i++) {
			char c = classFileName.charAt(i);
			if (Character.isDigit(c) && lastDollar == i-1) {
				// anonymous or local type
				return null;
			} else if (c == '$') {
				lastDollar = i;
			}
		}
		if (lastDollar == -1) {
			return null;
		} else {
			String enclosingName = classFileName.substring(0, lastDollar);
			String enclosingClassFileName = enclosingName + SUFFIX_STRING_class;
			return
				new BinaryType(
					(JavaElement)getPackageFragment().getClassFile(enclosingClassFileName),
					Util.localTypeName(enclosingName, enclosingName.lastIndexOf('$'), enclosingName.length()));
		}
	}
}
@Override
public Object getElementInfo(IProgressMonitor monitor) throws JavaModelException {
	JavaModelManager manager = JavaModelManager.getJavaModelManager();
	Object info = manager.getInfo(this);
	if (info != null && info != JavaModelCache.NON_EXISTING_JAR_TYPE_INFO) return info;
	return openWhenClosed(createElementInfo(), false, monitor);
}
/*
 * @see IJavaElement
 */
@Override
public int getElementType() {
	return TYPE;
}

@Override
public IField getField(String fieldName) {
	return new BinaryField(this, fieldName);
}
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
		return new IField[0];
	return getFieldsOrComponents(true);
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
			return new BinaryField(this, compName);
	} catch (JavaModelException e) {
		//
	}
	return null;
}
@Override
public int getFlags() throws JavaModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	return info.getModifiers() & ~ClassFileConstants.AccSuper;
}

@Override
public String getFullyQualifiedName() {
	return this.getFullyQualifiedName('$');
}

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

@Override
public IInitializer getInitializer(int count) {
	return new Initializer(this, count);
}

@Override
public IInitializer[] getInitializers() {
	return NO_INITIALIZERS;
}
@Override
public String getKey(boolean forceOpen) throws JavaModelException {
	return getKey(this, forceOpen);
}

@Override
public IMethod getMethod(String selector, String[] parameterTypeSignatures) {
	return new BinaryMethod(this, selector, parameterTypeSignatures);
}

@Override
public IMethod[] getMethods() throws JavaModelException {
	ArrayList list = getChildrenOfType(METHOD);
	int size;
	if ((size = list.size()) == 0) {
		return NO_METHODS;
	} else {
		IMethod[] array= new IMethod[size];
		list.toArray(array);
		return array;
	}
}

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

/**
 * @see IType#getSuperclassTypeSignature()
 * @since 3.0
 */
@Override
public String getSuperclassTypeSignature() throws JavaModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	char[] genericSignature = info.getGenericSignature();
	if (genericSignature != null) {
		int signatureLength = genericSignature.length;
		// skip type parameters
		int index = 0;
		if (genericSignature[0] == '<') {
			int count = 1;
			while (count > 0 && ++index < signatureLength) {
				switch (genericSignature[index]) {
					case '<':
						count++;
						break;
					case '>':
						count--;
						break;
				}
			}
			index++;
		}
		int start = index;
		index = org.eclipse.jdt.internal.compiler.util.Util.scanClassTypeSignature(genericSignature, start) + 1;
		char[] superclassSig = CharOperation.subarray(genericSignature, start, index);
		return new String(ClassFile.translatedName(superclassSig));
	} else {
		char[] superclassName = info.getSuperclassName();
		if (superclassName == null) {
			return null;
		}
		return Signature.createTypeSignature(ClassFile.translatedName(superclassName), true);
	}
}

public String getSourceFileName(IBinaryType info) {
	if (info == null) {
		try {
			info = (IBinaryType) getElementInfo();
		} catch (JavaModelException e) {
			// default to using the outer most declaring type name
			IType type = this;
			IType enclosingType = getDeclaringType();
			while (enclosingType != null) {
				type = enclosingType;
				enclosingType = type.getDeclaringType();
			}
			return type.getElementName() + Util.defaultJavaExtension();
		}
	}
	return sourceFileName(info);
}

@Override
public String getSuperclassName() throws JavaModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	char[] superclassName = info.getSuperclassName();
	if (superclassName == null) {
		return null;
	}
	return new String(ClassFile.translatedName(superclassName));
}

@Override
public String[] getSuperInterfaceNames() throws JavaModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	char[][] names= info.getInterfaceNames();
	int length;
	if (names == null || (length = names.length) == 0) {
		return CharOperation.NO_STRINGS;
	}
	names= ClassFile.translatedNames(names);
	String[] strings= new String[length];
	for (int i= 0; i < length; i++) {
		strings[i]= new String(names[i]);
	}
	return strings;
}
@Override
public String[] getPermittedSubtypeNames() throws JavaModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	char[][] names= info.getPermittedSubtypeNames();
	int length;
	if (names == null || (length = names.length) == 0) {
		return CharOperation.NO_STRINGS;
	}
	names= ClassFile.translatedNames(names);
	String[] strings= new String[length];
	for (int i= 0; i < length; i++) {
		strings[i]= new String(names[i]);
	}
	return strings;
}

/**
 * @see IType#getSuperInterfaceTypeSignatures()
 * @since 3.0
 */
@Override
public String[] getSuperInterfaceTypeSignatures() throws JavaModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	char[] genericSignature = info.getGenericSignature();
	if (genericSignature != null) {
		ArrayList interfaces = new ArrayList();
		int signatureLength = genericSignature.length;
		// skip type parameters
		int index = 0;
		if (genericSignature[0] == '<') {
			int count = 1;
			while (count > 0 && ++index < signatureLength) {
				switch (genericSignature[index]) {
					case '<':
						count++;
						break;
					case '>':
						count--;
						break;
				}
			}
			index++;
		}
		// skip superclass
		index = org.eclipse.jdt.internal.compiler.util.Util.scanClassTypeSignature(genericSignature, index) + 1;
		while (index  < signatureLength) {
			int start = index;
			index = org.eclipse.jdt.internal.compiler.util.Util.scanClassTypeSignature(genericSignature, start) + 1;
			char[] interfaceSig = CharOperation.subarray(genericSignature, start, index);
			interfaces.add(new String(ClassFile.translatedName(interfaceSig)));
		}
		int size = interfaces.size();
		String[] result = new String[size];
		interfaces.toArray(result);
		return result;
	} else {
		char[][] names= info.getInterfaceNames();
		int length;
		if (names == null || (length = names.length) == 0) {
			return CharOperation.NO_STRINGS;
		}
		names= ClassFile.translatedNames(names);
		String[] strings= new String[length];
		for (int i= 0; i < length; i++) {
			strings[i]= Signature.createTypeSignature(names[i], true);
		}
		return strings;
	}
}

@Override
public ITypeParameter[] getTypeParameters() throws JavaModelException {
	String[] typeParameterSignatures = getTypeParameterSignatures();
	int length = typeParameterSignatures.length;
	if (length == 0) return TypeParameter.NO_TYPE_PARAMETERS;
	ITypeParameter[] typeParameters = new ITypeParameter[length];
	for (int i = 0; i < typeParameterSignatures.length; i++) {
		String typeParameterName = Signature.getTypeVariable(typeParameterSignatures[i]);
		typeParameters[i] = new TypeParameter(this, typeParameterName);
	}
	return typeParameters;
}

/**
 * @see IType#getTypeParameterSignatures()
 * @since 3.0
 */
@Override
public String[] getTypeParameterSignatures() throws JavaModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	char[] genericSignature = info.getGenericSignature();
	if (genericSignature == null)
		return CharOperation.NO_STRINGS;

	char[] dotBaseSignature = CharOperation.replaceOnCopy(genericSignature, '/', '.');
	char[][] typeParams = Signature.getTypeParameters(dotBaseSignature);
	return CharOperation.toStrings(typeParams);
}

@Override
public IType getType(String typeName) {
	IClassFile classFile= getPackageFragment().getClassFile(getTypeQualifiedName() + "$" + typeName + SUFFIX_STRING_class); //$NON-NLS-1$
	return new BinaryType((JavaElement)classFile, typeName);
}
@Override
public ITypeParameter getTypeParameter(String typeParameterName) {
	return new TypeParameter(this, typeParameterName);
}

@Override
public String getTypeQualifiedName() {
	return this.getTypeQualifiedName('$');
}

@Override
public String getTypeQualifiedName(char enclosingTypeSeparator) {
	try {
		return getTypeQualifiedName(enclosingTypeSeparator, false/*don't show parameters*/);
	} catch (JavaModelException e) {
		// exception thrown only when showing parameters
		return null;
	}
}

@Override
public IType[] getTypes() throws JavaModelException {
	ArrayList list = getChildrenOfType(TYPE);
	int size;
	if ((size = list.size()) == 0) {
		return NO_TYPES;
	} else {
		IType[] array= new IType[size];
		list.toArray(array);
		return array;
	}
}

@Override
public boolean isAnonymous() throws JavaModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	return info.isAnonymous();
}

@Override
public boolean isClass() throws JavaModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	return TypeDeclaration.kind(info.getModifiers()) == TypeDeclaration.CLASS_DECL;

}

/**
 * @see IType#isEnum()
 * @since 3.0
 */
@Override
public boolean isEnum() throws JavaModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	return TypeDeclaration.kind(info.getModifiers()) == TypeDeclaration.ENUM_DECL;
}

/**
 * @see IType#isRecord()
 * @since 3.26
 */
@Override
public boolean isRecord() throws JavaModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	return TypeDeclaration.kind(info.getModifiers()) == TypeDeclaration.RECORD_DECL;
}
/**
 * @see IType#isSealed()
 * @noreference This method is not intended to be referenced by clients as it is a part of Java preview feature.
 */
@Override
public boolean isSealed() throws JavaModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	char[][] names = info.getPermittedSubtypeNames();
	return (names != null && names.length > 0);
}

@Override
public boolean isInterface() throws JavaModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
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
	IBinaryType info = (IBinaryType) getElementInfo();
	return TypeDeclaration.kind(info.getModifiers()) == TypeDeclaration.ANNOTATION_TYPE_DECL;
}

@Override
public boolean isLocal() throws JavaModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	return info.isLocal();
}

@Override
public boolean isMember() throws JavaModelException {
	IBinaryType info = (IBinaryType) getElementInfo();
	return info.isMember();
}

@Override
public boolean isResolved() {
	return false;
}
/*
 * @see IType
 */
@Override
public ITypeHierarchy loadTypeHierachy(InputStream input, IProgressMonitor monitor) throws JavaModelException {
	return loadTypeHierachy(input, DefaultWorkingCopyOwner.PRIMARY, monitor);
}
/*
 * @see IType
 */
public ITypeHierarchy loadTypeHierachy(InputStream input, WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaModelException {
	return TypeHierarchy.load(this, input, owner);
}

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

@Override
public ITypeHierarchy newTypeHierarchy(IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
	return newTypeHierarchy(project, DefaultWorkingCopyOwner.PRIMARY, monitor);
}

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
 * @param monitor the given progress monitor
 * @exception JavaModelException if this element does not exist or if an
 *		exception occurs while accessing its corresponding resource.
 * @return a type hierarchy for this type containing
 *
 * @see IType#newTypeHierarchy(IProgressMonitor monitor)
 * @deprecated
 */
@Override
public ITypeHierarchy newTypeHierarchy(IProgressMonitor monitor) throws JavaModelException {
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=228845, consider any
	// changes that may exist on primary working copies.
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
	SourceRefElement resolvedHandle = new ResolvedBinaryType(this.getParent(), this.name, new String(binding.computeUniqueKey()));
	resolvedHandle.occurrenceCount = this.occurrenceCount;
	return resolvedHandle;
}
/*
 * Returns the source file name as defined in the given info.
 * If not present in the info, infers it from this type.
 */
public String sourceFileName(IBinaryType info) {
	char[] sourceFileName = info.sourceFileName();
	if (sourceFileName == null) {
		/*
		 * We assume that this type has been compiled from a file with its name
		 * For example, A.class comes from A.java and p.A.class comes from a file A.java
		 * in the folder p.
		 */
		if (info.isMember()) {
			IType enclosingType = getDeclaringType();
			if (enclosingType == null) return null; // play it safe
			while (enclosingType.getDeclaringType() != null) {
				enclosingType = enclosingType.getDeclaringType();
			}
			return enclosingType.getElementName() + Util.defaultJavaExtension();
		} else if (info.isLocal() || info.isAnonymous()){
			String typeQualifiedName = getTypeQualifiedName();
			int dollar = typeQualifiedName.indexOf('$');
			if (dollar == -1) {
				// malformed inner type: name doesn't contain a dollar
				return getElementName() + Util.defaultJavaExtension();
			}
			return typeQualifiedName.substring(0, dollar) + Util.defaultJavaExtension();
		} else {
			return getElementName() + Util.defaultJavaExtension();
		}
	} else {
		int index = CharOperation.lastIndexOf('/', sourceFileName);
		return new String(sourceFileName, index + 1, sourceFileName.length - index - 1);
	}
}
/*
 * @private Debugging purposes
 */
@Override
protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
	buffer.append(tabString(tab));
	if (info == null) {
		toStringName(buffer);
		buffer.append(" (not open)"); //$NON-NLS-1$
	} else if (info == NO_INFO) {
		toStringName(buffer);
	} else {
		try {
			if (isRecord()) {
				buffer.append("record "); //$NON-NLS-1$
			} else if (isAnnotation()) {
				buffer.append("@interface "); //$NON-NLS-1$
			} else if (isEnum()) {
				buffer.append("enum "); //$NON-NLS-1$
			} else if (isInterface()) {
				buffer.append("interface "); //$NON-NLS-1$
			} else {
				buffer.append("class "); //$NON-NLS-1$
			}
			toStringName(buffer);
		} catch (JavaModelException e) {
			buffer.append("<JavaModelException in toString of " + getElementName()); //$NON-NLS-1$
		}
	}
}
@Override
protected void toStringName(StringBuffer buffer) {
	if (getElementName().length() > 0)
		super.toStringName(buffer);
	else
		buffer.append("<anonymous>"); //$NON-NLS-1$
}
@Override
public String getAttachedJavadoc(IProgressMonitor monitor) throws JavaModelException {
	JavadocContents javadocContents = getJavadocContents(monitor);
	if (javadocContents == null) return null;
	return javadocContents.getTypeDoc();
}
public JavadocContents getJavadocContents(IProgressMonitor monitor) throws JavaModelException {
	PerProjectInfo projectInfo = JavaModelManager.getJavaModelManager().getPerProjectInfoCheckExistence(getJavaProject().getProject());
	JavadocContents cachedJavadoc = null;
	synchronized (projectInfo.javadocCache) {
		cachedJavadoc = (JavadocContents) projectInfo.javadocCache.get(this);
	}

	if (cachedJavadoc != null && cachedJavadoc != EMPTY_JAVADOC) {
		return cachedJavadoc;
	}
	URL baseLocation= getJavadocBaseLocation();
	if (baseLocation == null) {
		return null;
	}
	StringBuffer pathBuffer = new StringBuffer(baseLocation.toExternalForm());

	if (!(pathBuffer.charAt(pathBuffer.length() - 1) == '/')) {
		pathBuffer.append('/');
	}
	IPackageFragment pack= getPackageFragment();
	String typeQualifiedName = null;
	if (isMember()) {
		IType currentType = this;
		StringBuffer typeName = new StringBuffer();
		while (currentType != null) {
			typeName.insert(0, currentType.getElementName());
			currentType = currentType.getDeclaringType();
			if (currentType != null) {
				typeName.insert(0, '.');
			}
		}
		typeQualifiedName = typeName.toString();
	} else {
		typeQualifiedName = getElementName();
	}

	appendModulePath(pack, pathBuffer);
	pathBuffer.append(pack.getElementName().replace('.', '/')).append('/').append(typeQualifiedName).append(JavadocConstants.HTML_EXTENSION);
	if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
	final String contents = getURLContents(baseLocation, String.valueOf(pathBuffer));
	JavadocContents javadocContents = new JavadocContents(this, contents);
	synchronized (projectInfo.javadocCache) {
		projectInfo.javadocCache.put(this, javadocContents);
	}
	return javadocContents;
}
@Override
public boolean isLambda() {
	return false;
}

private static void appendModulePath(IPackageFragment pack, StringBuffer buf) {
	IModuleDescription moduleDescription= getModuleDescription(pack);
	if (moduleDescription != null) {
		String moduleName= moduleDescription.getElementName();
		if (moduleName != null && moduleName.length() > 0) {
			buf.append(moduleName);
			buf.append('/');
		}
	}
}

private static IModuleDescription getModuleDescription(IPackageFragment pack) {
	if (pack == null) {
		return null;
	}
	IModuleDescription moduleDescription= null;
	/*
	 * The Javadoc tool for Java SE 11 uses module name in the created URL.
	 * We can't know what format is required, so we just guess by the project's compiler compliance.
	 */
	IJavaProject javaProject= pack.getJavaProject();
	if (javaProject != null && isComplianceJava11OrHigher(javaProject)) {
		if (pack.isReadOnly()) {
			IPackageFragmentRoot root= (IPackageFragmentRoot) pack.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
			if (root instanceof JrtPackageFragmentRoot) {
				moduleDescription= root.getModuleDescription();
			}
		} else {
			try {
				moduleDescription= javaProject.getModuleDescription();
			} catch (JavaModelException e) {
				// do nothing
			}
		}
	}
	return moduleDescription;
}

private static boolean isComplianceJava11OrHigher(IJavaProject javaProject) {
	if (javaProject == null) {
		return false;
	}
	return CompilerOptions.versionToJdkLevel(javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true)) >= ClassFileConstants.JDK11;
}
}
