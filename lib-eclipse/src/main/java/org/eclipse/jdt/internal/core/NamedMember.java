/*******************************************************************************
 * Copyright (c) 2004, 2017 IBM Corporation and others.
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

import org.eclipse.jdt.core.BindingKey;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IModularClassFile;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.codeassist.ISelectionRequestor;
import org.eclipse.jdt.internal.codeassist.SelectionEngine;

public abstract class NamedMember extends Member {

	/*
	 * This element's name, or an empty <code>String</code> if this
	 * element does not have a name.
	 */
	protected String name;

	public NamedMember(JavaElement parent, String name) {
		super(parent);
		this.name = name;
	}

	private void appendTypeParameters(StringBuffer buffer) throws JavaModelException {
		ITypeParameter[] typeParameters = getTypeParameters();
		int length = typeParameters.length;
		if (length == 0) return;
		buffer.append('<');
		for (int i = 0; i < length; i++) {
			ITypeParameter typeParameter = typeParameters[i];
			buffer.append(typeParameter.getElementName());
			String[] bounds = typeParameter.getBounds();
			int boundsLength = bounds.length;
			if (boundsLength > 0) {
				buffer.append(" extends "); //$NON-NLS-1$
				for (int j = 0; j < boundsLength; j++) {
					buffer.append(bounds[j]);
					if (j < boundsLength-1)
						buffer.append(" & "); //$NON-NLS-1$
				}
			}
			if (i < length-1)
				buffer.append(", "); //$NON-NLS-1$
		}
		buffer.append('>');
	}

	@Override
	public String getElementName() {
		return this.name;
	}

	protected String getKey(IField field, boolean forceOpen) throws JavaModelException {
		StringBuilder key = new StringBuilder();

		// declaring class
		String declaringKey = getKey((IType) field.getParent(), forceOpen);
		key.append(declaringKey);

		// field name
		key.append('.');
		key.append(field.getElementName());

		return key.toString();
	}

	protected String getKey(IMethod method, boolean forceOpen) throws JavaModelException {
		StringBuilder key = new StringBuilder();

		// declaring class
		String declaringKey = getKey((IType) method.getParent(), forceOpen);
		key.append(declaringKey);

		// selector
		key.append('.');
		if (!method.isConstructor()) { // empty selector for ctors, cf. BindingKeyResolver.consumeMethod()
			String selector = method.getElementName();
			key.append(selector);
		}

		// type parameters
		if (forceOpen) {
			ITypeParameter[] typeParameters = method.getTypeParameters();
			int length = typeParameters.length;
			if (length > 0) {
				key.append('<');
				for (int i = 0; i < length; i++) {
					ITypeParameter typeParameter = typeParameters[i];
					String[] bounds = typeParameter.getBounds();
					int boundsLength = bounds.length;
					char[][] boundSignatures = new char[boundsLength][];
					for (int j = 0; j < boundsLength; j++) {
						boundSignatures[j] = Signature.createCharArrayTypeSignature(bounds[j].toCharArray(), method.isBinary());
						CharOperation.replace(boundSignatures[j], '.', '/');
					}
					char[] sig = Signature.createTypeParameterSignature(typeParameter.getElementName().toCharArray(), boundSignatures);
					key.append(sig);
				}
				key.append('>');
			}
		}

		// parameters
		key.append('(');
		String[] parameters = method.getParameterTypes();
		for (int i = 0, length = parameters.length; i < length; i++)
			key.append(parameters[i].replace('.', '/'));
		key.append(')');

		// return type
		if (forceOpen)
			key.append(method.getReturnType().replace('.', '/'));
		else
			key.append('V');

		return key.toString();
	}

	protected String getKey(IType type, boolean forceOpen) throws JavaModelException {
		StringBuilder key = new StringBuilder();
		key.append('L');
		String packageName = type.getPackageFragment().getElementName();
		key.append(packageName.replace('.', '/'));
		if (packageName.length() > 0)
			key.append('/');
		String typeQualifiedName = type.getTypeQualifiedName('$');
		ICompilationUnit cu = (ICompilationUnit) type.getAncestor(IJavaElement.COMPILATION_UNIT);
		if (cu != null) {
			String cuName = cu.getElementName();
			String mainTypeName = cuName.substring(0, cuName.lastIndexOf('.'));
			int end = typeQualifiedName.indexOf('$');
			if (end == -1)
				end = typeQualifiedName.length();
			String topLevelTypeName = typeQualifiedName.substring(0, end);
			if (!mainTypeName.equals(topLevelTypeName)) {
				key.append(mainTypeName);
				key.append('~');
			}
		}
		key.append(typeQualifiedName);
		key.append(';');
		return key.toString();
	}
	protected String getKey(IModuleDescription module, boolean forceOpen) throws JavaModelException {
		StringBuilder key = new StringBuilder();
		key.append('"');
		String modName = module.getElementName();
		key.append(modName);
		return key.toString();
	}

	protected String getFullyQualifiedParameterizedName(String fullyQualifiedName, String uniqueKey) throws JavaModelException {
		String[] typeArguments = new BindingKey(uniqueKey).getTypeArguments();
		int length = typeArguments.length;
		if (length == 0) return fullyQualifiedName;
		StringBuilder buffer = new StringBuilder();
		buffer.append(fullyQualifiedName);
		buffer.append('<');
		for (int i = 0; i < length; i++) {
			String typeArgument = typeArguments[i];
			buffer.append(Signature.toString(typeArgument));
			if (i < length-1)
				buffer.append(',');
		}
		buffer.append('>');
		return buffer.toString();
	}

	protected IPackageFragment getPackageFragment() {
		return null;
	}

	public String getFullyQualifiedName(char enclosingTypeSeparator, boolean showParameters) throws JavaModelException {
		String packageName = getPackageFragment().getElementName();
		if (packageName.equals(IPackageFragment.DEFAULT_PACKAGE_NAME)) {
			return getTypeQualifiedName(enclosingTypeSeparator, showParameters);
		}
		return packageName + '.' + getTypeQualifiedName(enclosingTypeSeparator, showParameters);
	}

	public String getTypeQualifiedName(char enclosingTypeSeparator, boolean showParameters) throws JavaModelException {
		NamedMember declaringType;
		switch (this.getParent().getElementType()) {
			case IJavaElement.COMPILATION_UNIT:
				if (showParameters) {
					StringBuffer buffer = new StringBuffer(this.name);
					appendTypeParameters(buffer);
					return buffer.toString();
				}
				return this.name;
			case IJavaElement.CLASS_FILE:
				if (this.getParent() instanceof IModularClassFile)
					return null;
				String classFileName = this.getParent().getElementName();
				String typeName;
				if (classFileName.indexOf('$') == -1) {
					// top level class file: name of type is same as name of class file
					typeName = this.name;
				} else {
					// anonymous or local class file
					typeName = classFileName.substring(0, classFileName.lastIndexOf('.'))/*remove .class*/.replace('$', enclosingTypeSeparator);
				}
				if (showParameters) {
					StringBuffer buffer = new StringBuffer(typeName);
					appendTypeParameters(buffer);
					return buffer.toString();
				}
				return typeName;
			case IJavaElement.TYPE:
				declaringType = (NamedMember) this.getParent();
				break;
			case IJavaElement.FIELD:
			case IJavaElement.INITIALIZER:
			case IJavaElement.METHOD:
				declaringType = (NamedMember) ((IMember) this.getParent()).getDeclaringType();
				break;
			default:
				return null;
		}
		StringBuffer buffer = new StringBuffer(declaringType.getTypeQualifiedName(enclosingTypeSeparator, showParameters));
		buffer.append(enclosingTypeSeparator);
		String simpleName = this.name.length() == 0 ? getOccurrenceCountSignature() : this.name;
		buffer.append(simpleName);
		if (showParameters) {
			appendTypeParameters(buffer);
		}
		return buffer.toString();
	}
	/*
	 * Returns the String representation of the occurrence count for this element.
	 * The occurrence count is a unique number representation to identify the particular element.
	 *
	 * @return the occurrence count for this element in the form of String
	 */
	protected String getOccurrenceCountSignature() {
		return Integer.toString(this.occurrenceCount);
	}
	protected ITypeParameter[] getTypeParameters() throws JavaModelException {
		return null;
	}

	/**
	 * @see IType#resolveType(String)
	 */
	public String[][] resolveType(String typeName) throws JavaModelException {
		return resolveType(typeName, DefaultWorkingCopyOwner.PRIMARY);
	}

	/**
	 * @see IType#resolveType(String, WorkingCopyOwner)
	 */
	public String[][] resolveType(String typeName, WorkingCopyOwner owner) throws JavaModelException {
		JavaProject project = getJavaProject();
		SearchableEnvironment environment = project.newSearchableNameEnvironment(owner);

		class TypeResolveRequestor implements ISelectionRequestor {
			String[][] answers = null;
			@Override
			public void acceptType(char[] packageName, char[] tName, int modifiers, boolean isDeclaration, char[] uniqueKey, int start, int end) {
				String[] answer = new String[]  {new String(packageName), new String(tName) };
				if (this.answers == null) {
					this.answers = new String[][]{ answer };
				} else {
					// grow
					int length = this.answers.length;
					System.arraycopy(this.answers, 0, this.answers = new String[length+1][], 0, length);
					this.answers[length] = answer;
				}
			}
			@Override
			public void acceptError(CategorizedProblem error) {
				// ignore
			}
			@Override
			public void acceptField(char[] declaringTypePackageName, char[] declaringTypeName, char[] fieldName, boolean isDeclaration, char[] uniqueKey, int start, int end) {
				// ignore
			}
			@Override
			public void acceptMethod(char[] declaringTypePackageName, char[] declaringTypeName, String enclosingDeclaringTypeSignature, char[] selector, char[][] parameterPackageNames, char[][] parameterTypeNames, String[] parameterSignatures, char[][] typeParameterNames, char[][][] typeParameterBoundNames, boolean isConstructor, boolean isDeclaration, char[] uniqueKey, int start, int end) {
				// ignore
			}
			@Override
			public void acceptPackage(char[] packageName){
				// ignore
			}
			@Override
			public void acceptTypeParameter(char[] declaringTypePackageName, char[] declaringTypeName, char[] typeParameterName, boolean isDeclaration, int start, int end) {
				// ignore
			}
			@Override
			public void acceptMethodTypeParameter(char[] declaringTypePackageName, char[] declaringTypeName, char[] selector, int selectorStart, int selcetorEnd, char[] typeParameterName, boolean isDeclaration, int start, int end) {
				// ignore
			}
			@Override
			public void acceptModule(char[] moduleName, char[] uniqueKey, int start, int end) {
				// ignore
			}

		}
		TypeResolveRequestor requestor = new TypeResolveRequestor();
		SelectionEngine engine =
			new SelectionEngine(environment, requestor, project.getOptions(true), owner);

		engine.selectType(typeName.toCharArray(), (IType) this);
		if (NameLookup.VERBOSE) {
			System.out.println(Thread.currentThread() + " TIME SPENT in NameLoopkup#seekTypesInSourcePackage: " + environment.nameLookup.timeSpentInSeekTypesInSourcePackage + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(Thread.currentThread() + " TIME SPENT in NameLoopkup#seekTypesInBinaryPackage: " + environment.nameLookup.timeSpentInSeekTypesInBinaryPackage + "ms");  //$NON-NLS-1$ //$NON-NLS-2$
		}
		return requestor.answers;
	}
}
