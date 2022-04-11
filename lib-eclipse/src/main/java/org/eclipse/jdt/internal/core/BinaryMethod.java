/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Terry Parker <tparker@google.com> - Bug 418092
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryMethod;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.jdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.jdt.internal.core.JavaModelManager.PerProjectInfo;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * @see IMethod
 */

public class BinaryMethod extends BinaryMember implements IMethod {
	/**
	 * The parameter type signatures of the method - stored locally
	 * to perform equality test. <code>CharOperation.NO_STRINGS</code> indicates no
	 * parameters. Note that the parameter type signatures are expected to be dot-based.
	 */
	protected String[] parameterTypes;
	protected String [] erasedParamaterTypes; // lazily initialized via call to getErasedParameterTypes

	/**
	 * The parameter names for the method.
	 */
	protected String[] parameterNames;

	protected String[] exceptionTypes;
	protected String returnType;

protected BinaryMethod(JavaElement parent, String name, String[] paramTypes) {
	super(parent, name);
	// Assertion disabled since bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=179011
	// Assert.isTrue(name.indexOf('.') == -1);
	if (paramTypes == null) {
		this.parameterTypes= CharOperation.NO_STRINGS;
	} else {
		this.parameterTypes= paramTypes;
	}
}
@Override
public boolean equals(Object o) {
	if (!(o instanceof BinaryMethod)) return false;
	return super.equals(o) && Util.equalArraysOrNull(getErasedParameterTypes(), ((BinaryMethod)o).getErasedParameterTypes());
}
@Override
public IAnnotation[] getAnnotations() throws JavaModelException {
	IBinaryMethod info = (IBinaryMethod) getElementInfo();
	IBinaryAnnotation[] binaryAnnotations = info.getAnnotations();
	return getAnnotations(binaryAnnotations, info.getTagBits());
}
@Override
public ILocalVariable[] getParameters() throws JavaModelException {
	IBinaryMethod info = (IBinaryMethod) getElementInfo();
	int length = this.parameterTypes.length;
	if (length == 0) {
		return LocalVariable.NO_LOCAL_VARIABLES;
	}
	ILocalVariable[] localVariables = new ILocalVariable[length];
	char[][] argumentNames = info.getArgumentNames();
	if (argumentNames == null || argumentNames.length < length) {
		argumentNames = new char[length][];
		for (int j = 0; j < length; j++) {
			argumentNames[j] = ("arg" + j).toCharArray(); //$NON-NLS-1$
		}
	}
	int startIndex = 0;
	try {
		if (isConstructor()) {
			IType declaringType = this.getDeclaringType();
			if (declaringType.isEnum()) {
				startIndex = 2;
			} else if (declaringType.isMember()
					&& !Flags.isStatic(declaringType.getFlags())) {
				startIndex = 1;
			}
		}
	} catch(JavaModelException e) {
		// ignore
	}
	for (int i= 0; i < length; i++) {
		if (i < startIndex) {
			LocalVariable localVariable = new LocalVariable(
					this,
					new String(argumentNames[i]),
					0,
					-1,
					0,
					-1,
					this.parameterTypes[i],
					null,
					-1,
					true);
			localVariables[i] = localVariable;
			localVariable.annotations = Annotation.NO_ANNOTATIONS;
		} else {
			LocalVariable localVariable = new LocalVariable(
					this,
					new String(argumentNames[i]),
					0,
					-1,
					0,
					-1,
					this.parameterTypes[i],
					null,
					-1,
					true);
			localVariables[i] = localVariable;
			IAnnotation[] annotations = getAnnotations(localVariable, info.getParameterAnnotations(i - startIndex, getDeclaringType().getElementName().toCharArray()));
			localVariable.annotations = annotations;
		}
	}
	return localVariables;
}
private IAnnotation[] getAnnotations(JavaElement annotationParent, IBinaryAnnotation[] binaryAnnotations) {
	if (binaryAnnotations == null) return Annotation.NO_ANNOTATIONS;
	int length = binaryAnnotations.length;
	IAnnotation[] annotations = new IAnnotation[length];
	for (int i = 0; i < length; i++) {
		annotations[i] = Util.getAnnotation(annotationParent, binaryAnnotations[i], null);
	}
	return annotations;
}
@Override
public IMemberValuePair getDefaultValue() throws JavaModelException {
	IBinaryMethod info = (IBinaryMethod) getElementInfo();
	Object defaultValue = info.getDefaultValue();
	if (defaultValue == null)
		return null;
	MemberValuePair memberValuePair = new MemberValuePair(getElementName());
	memberValuePair.value = Util.getAnnotationMemberValue(this, memberValuePair, defaultValue);
	return memberValuePair;
}
/*
 * @see IMethod
 */
@Override
public String[] getExceptionTypes() throws JavaModelException {
	if (this.exceptionTypes == null) {
		IBinaryMethod info = (IBinaryMethod) getElementInfo();
		char[] genericSignature = info.getGenericSignature();
		if (genericSignature != null) {
			char[] dotBasedSignature = CharOperation.replaceOnCopy(genericSignature, '/', '.');
			this.exceptionTypes = Signature.getThrownExceptionTypes(new String(dotBasedSignature));
		}
		if (this.exceptionTypes == null || this.exceptionTypes.length == 0) {
			char[][] eTypeNames = info.getExceptionTypeNames();
			if (eTypeNames == null || eTypeNames.length == 0) {
				this.exceptionTypes = CharOperation.NO_STRINGS;
			} else {
				eTypeNames = ClassFile.translatedNames(eTypeNames);
				this.exceptionTypes = new String[eTypeNames.length];
				for (int j = 0, length = eTypeNames.length; j < length; j++) {
					// 1G01HRY: ITPJCORE:WINNT - method.getExceptionType not in correct format
					int nameLength = eTypeNames[j].length;
					char[] convertedName = new char[nameLength + 2];
					System.arraycopy(eTypeNames[j], 0, convertedName, 1, nameLength);
					convertedName[0] = 'L';
					convertedName[nameLength + 1] = ';';
					this.exceptionTypes[j] = new String(convertedName);
				}
			}
		}
	}
	return this.exceptionTypes;
}
/*
 * @see IJavaElement
 */
@Override
public int getElementType() {
	return METHOD;
}
/*
 * @see IMember
 */
@Override
public int getFlags() throws JavaModelException {
	IBinaryMethod info = (IBinaryMethod) getElementInfo();
	int modifiers = info.getModifiers();
	if (((IType) this.getParent()).isInterface() && (modifiers & (ClassFileConstants.AccAbstract | ClassFileConstants.AccStatic)) == 0)
		modifiers |= ExtraCompilerModifiers.AccDefaultMethod;
	return modifiers;
}
/*
 * @see JavaElement#getHandleMemento(StringBuffer)
 */
@Override
protected void getHandleMemento(StringBuffer buff) {
	getParent().getHandleMemento(buff);
	char delimiter = getHandleMementoDelimiter();
	buff.append(delimiter);
	escapeMementoName(buff, getElementName());
	for (int i = 0; i < this.parameterTypes.length; i++) {
		buff.append(delimiter);
		escapeMementoName(buff, this.parameterTypes[i]);
	}
	if (this.occurrenceCount > 1) {
		buff.append(JEM_COUNT);
		buff.append(this.occurrenceCount);
	}
}
/*
 * @see JavaElement#getHandleMemento()
 */
@Override
protected char getHandleMementoDelimiter() {
	return JavaElement.JEM_METHOD;
}
@Override
public String getKey(boolean forceOpen) throws JavaModelException {
	return getKey(this, forceOpen);
}
/*
 * @see IMethod
 */
@Override
public int getNumberOfParameters() {
	return this.parameterTypes == null ? 0 : this.parameterTypes.length;
}
/*
 * @see IMethod
 * Look for source attachment information to retrieve the actual parameter names as stated in source.
 */
@Override
public String[] getParameterNames() throws JavaModelException {
	if (this.parameterNames != null)
		return this.parameterNames;

	// force source mapping if not already done
	IType type = (IType) getParent();
	SourceMapper mapper = getSourceMapper();
	if (mapper != null) {
		char[][] paramNames = mapper.getMethodParameterNames(this);

		// map source and try to find parameter names
		if(paramNames == null) {
			IBinaryType info = (IBinaryType) ((BinaryType) getDeclaringType()).getElementInfo();
			char[] source = mapper.findSource(type, info);
			if (source != null) {
				mapper.mapSource((NamedMember) type, source, info);
			}
			paramNames = mapper.getMethodParameterNames(this);
		}

		// if parameter names exist, convert parameter names to String array
		if(paramNames != null) {
			String[] names = new String[paramNames.length];
			for (int i = 0; i < paramNames.length; i++) {
				names[i] = new String(paramNames[i]);
			}
			return this.parameterNames = names;
		}
	}

	// try to see if we can retrieve the names from the attached javadoc
	IBinaryMethod info = (IBinaryMethod) getElementInfo();
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=316937
	// Use Signature#getParameterCount() only if the argument names are not already available.
	int paramCount = Signature.getParameterCount(new String(info.getMethodDescriptor()));
	if (this.isConstructor()) {
		final IType declaringType = this.getDeclaringType();
		if (declaringType.isMember()
				&& !Flags.isStatic(declaringType.getFlags())) {
			paramCount--; // remove synthetic argument from constructor param count
		} else if (declaringType.isEnum()) {
			if (paramCount >= 2) // https://bugs.eclipse.org/bugs/show_bug.cgi?id=436347
				paramCount -= 2;
		}
	}

	if (paramCount != 0) {
		// don't try to look for javadoc for synthetic methods
		int modifiers = getFlags();
		if ((modifiers & ClassFileConstants.AccSynthetic) != 0) {
			return this.parameterNames = getRawParameterNames(paramCount);
		}
		JavadocContents javadocContents = null;
		IType declaringType = getDeclaringType();
		PerProjectInfo projectInfo = JavaModelManager.getJavaModelManager().getPerProjectInfoCheckExistence(getJavaProject().getProject());
		synchronized (projectInfo.javadocCache) {
			javadocContents = (JavadocContents) projectInfo.javadocCache.get(declaringType);
			if (javadocContents == null) {
				projectInfo.javadocCache.put(declaringType, BinaryType.EMPTY_JAVADOC);
			}
		}

		String methodDoc = null;
		if (javadocContents == null) {
			long timeOut = 50; // default value
			try {
				String option = getJavaProject().getOption(JavaCore.TIMEOUT_FOR_PARAMETER_NAME_FROM_ATTACHED_JAVADOC, true);
				if (option != null) {
					timeOut = Long.parseLong(option);
				}
			} catch(NumberFormatException e) {
				// ignore
			}
			if (timeOut == 0) {
				// don't try to fetch the values and don't cache either (https://bugs.eclipse.org/bugs/show_bug.cgi?id=329671)
				return getRawParameterNames(paramCount);
			}
			final class ParametersNameCollector {
				String javadoc;
				public void setJavadoc(String s) {
					this.javadoc = s;
				}
				public String getJavadoc() {
					return this.javadoc;
				}
			}
			/*
			 * The declaring type is not in the cache yet. The thread wil retrieve the javadoc contents
			 */
			final ParametersNameCollector nameCollector = new ParametersNameCollector();
			Thread collect = new Thread() {
				@Override
				public void run() {
					try {
						// this call has a side-effect on the per project info cache
						nameCollector.setJavadoc(BinaryMethod.this.getAttachedJavadoc(null));
					} catch (JavaModelException e) {
						// ignore
					}
					synchronized(nameCollector) {
						nameCollector.notify();
					}
				}
			};
			collect.start();
			synchronized(nameCollector) {
				try {
					nameCollector.wait(timeOut);
				} catch (InterruptedException e) {
					// ignore
				}
			}
			methodDoc = nameCollector.getJavadoc();
		} else if (javadocContents != BinaryType.EMPTY_JAVADOC){
			// need to extract the part relative to the binary method since javadoc contains the javadoc for the declaring type
			try {
				methodDoc = javadocContents.getMethodDoc(this);
			} catch(JavaModelException e) {
				javadocContents = null;
			}
		}
		if (methodDoc != null) {
			int indexOfOpenParen = methodDoc.indexOf('(');
			// Annotations may have parameters, so make sure we are parsing the actual method parameters.
			if (info.getAnnotations() != null) {
				while (indexOfOpenParen != -1 && !isOpenParenForMethod(methodDoc, getElementName(), indexOfOpenParen)) {
					indexOfOpenParen = methodDoc.indexOf('(', indexOfOpenParen + 1);
				}
			}
			if (indexOfOpenParen != -1) {
				final int indexOfClosingParen = methodDoc.indexOf(')', indexOfOpenParen);
				if (indexOfClosingParen != -1) {
					final char[] paramsSource =
						CharOperation.replace(
							methodDoc.substring(indexOfOpenParen + 1, indexOfClosingParen).toCharArray(),
							"&nbsp;".toCharArray(), //$NON-NLS-1$
							new char[] {' '});
					final char[][] params = splitParameters(paramsSource, paramCount);
					final int paramsLength = params.length;
					String[] names = new String[paramsLength];
					for (int i = 0; i < paramsLength; i++) {
						final char[] param = params[i];
						int indexOfSpace = CharOperation.lastIndexOf(' ', param);
						if (indexOfSpace != -1) {
							names[i] = String.valueOf(param, indexOfSpace + 1, param.length - indexOfSpace -1);
						} else {
							names[i] = "arg" + i; //$NON-NLS-1$
						}
					}
					return this.parameterNames = names;
				}
			}
		}
		// let's see if we can retrieve them from the debug infos
		char[][] argumentNames = info.getArgumentNames();
		if (argumentNames != null && argumentNames.length == paramCount) {
			String[] names = new String[paramCount];
			for (int i = 0; i < paramCount; i++) {
				names[i] = new String(argumentNames[i]);
			}
			return this.parameterNames = names;
		}
	}
	// If still no parameter names, produce fake ones, but don't cache them (https://bugs.eclipse.org/bugs/show_bug.cgi?id=329671)
	return getRawParameterNames(paramCount);
}
private boolean isOpenParenForMethod(String javaDoc, String methodName, int index) {
	/*
	 * Annotations can have parameters associated with them, so we need to validate that this parameter list is
	 * actually for the method. Determine this by scanning backwards from where the paren was seen, skipping over
	 * HTML tags to find the identifier that precedes the paren, and then matching that identifier against the
	 * method's name.
	 */
	boolean scanningTag = false;
	int endIndex = 0;
	while (--index > methodName.length()) {
		char previousChar = javaDoc.charAt(index);
		if (endIndex > 0) {
			if (!ScannerHelper.isJavaIdentifierPart(previousChar)
					|| !ScannerHelper.isJavaIdentifierStart(previousChar))
				return methodName.equals(javaDoc.substring(index + 1, endIndex));
		} else if (!scanningTag) {
			if (previousChar == '>')
				scanningTag = true;
			else if (ScannerHelper.isJavaIdentifierPart(previousChar)
					|| ScannerHelper.isJavaIdentifierStart(previousChar))
				endIndex = index + 1;
		} else if (previousChar == '<')
			// We are only matching angle brackets here, without any other validation of the tags.
			scanningTag = false;
	}
	return false;
}
private char[][] splitParameters(char[] parametersSource, int paramCount) {
	// we have generic types as one of the parameter types
	char[][] params = new char[paramCount][];
	int paramIndex = 0;
	int index = 0;
	int balance = 0;
	int length = parametersSource.length;
	int start = 0;
	while(index < length) {
		switch (parametersSource[index]) {
			case '<':
				balance++;
				index++;
				while(index < length && parametersSource[index] != '>') {
					index++;
				}
				break;
			case '>' :
				balance--;
				index++;
				break;
			case ',' :
				if (balance == 0 && paramIndex < paramCount) {
					params[paramIndex++] = CharOperation.subarray(parametersSource, start, index);
					start = index + 1;
				}
				index++;
				break;
			case '&' :
				if ((index + 4) < length) {
					if (parametersSource[index + 1] == 'l'
							&& parametersSource[index + 2] == 't'
							&& parametersSource[index + 3] == ';') {
						balance++;
						index += 4;
					} else if (parametersSource[index + 1] == 'g'
							&& parametersSource[index + 2] == 't'
							&& parametersSource[index + 3] == ';') {
						balance--;
						index += 4;
					} else {
						index++;
					}
				} else {
					index++;
				}
				break;
			default:
				index++;
		}
	}
	if (paramIndex < paramCount) {
		params[paramIndex++] = CharOperation.subarray(parametersSource, start, index);
	}
	if (paramIndex != paramCount) {
		// happens only for constructors with synthetic enclosing type in the signature
		System.arraycopy(params, 0, (params = new char[paramIndex][]), 0, paramIndex);
	}
	return params;
}
/*
 * @see IMethod
 */
@Override
public String[] getParameterTypes() {
	return this.parameterTypes;
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=299384
private String [] getErasedParameterTypes() {
	if (this.erasedParamaterTypes == null) {
		int paramCount = this.parameterTypes.length;
		String [] erasedTypes = new String [paramCount];
		boolean erasureNeeded = false;
		for (int i = 0; i < paramCount; i++) {
			String parameterType = this.parameterTypes[i];
			if ((erasedTypes[i] = Signature.getTypeErasure(parameterType)) != parameterType)
				erasureNeeded = true;
		}
		this.erasedParamaterTypes = erasureNeeded ? erasedTypes : this.parameterTypes;
	}
	return this.erasedParamaterTypes;
}
private String getErasedParameterType(int index) {
	return getErasedParameterTypes()[index];
}

@Override
public ITypeParameter getTypeParameter(String typeParameterName) {
	return new TypeParameter(this, typeParameterName);
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
 * @see IMethod#getTypeParameterSignatures()
 * @since 3.0
 * @deprecated
 */
@Override
public String[] getTypeParameterSignatures() throws JavaModelException {
	IBinaryMethod info = (IBinaryMethod) getElementInfo();
	char[] genericSignature = info.getGenericSignature();
	if (genericSignature == null)
		return CharOperation.NO_STRINGS;
	char[] dotBasedSignature = CharOperation.replaceOnCopy(genericSignature, '/', '.');
	char[][] typeParams = Signature.getTypeParameters(dotBasedSignature);
	return CharOperation.toStrings(typeParams);
}

@Override
public String[] getRawParameterNames() throws JavaModelException {
	IBinaryMethod info = (IBinaryMethod) getElementInfo();
	int paramCount = Signature.getParameterCount(new String(info.getMethodDescriptor()));
	return getRawParameterNames(paramCount);
}
private String[] getRawParameterNames(int paramCount) {
	String[] result = new String[paramCount];
	for (int i = 0; i < paramCount; i++) {
		result[i] = "arg" + i; //$NON-NLS-1$
	}
	return result;
}

/*
 * @see IMethod
 */
@Override
public String getReturnType() throws JavaModelException {
	if (this.returnType == null) {
		IBinaryMethod info = (IBinaryMethod) getElementInfo();
		this.returnType = getReturnType(info);
	}
	return this.returnType;
}
private String getReturnType(IBinaryMethod info) {
	char[] genericSignature = info.getGenericSignature();
	char[] signature = genericSignature == null ? info.getMethodDescriptor() : genericSignature;
	char[] dotBasedSignature = CharOperation.replaceOnCopy(signature, '/', '.');
	String returnTypeName= Signature.getReturnType(new String(dotBasedSignature));
	return new String(ClassFile.translatedName(returnTypeName.toCharArray()));
}
/*
 * @see IMethod
 */
@Override
public String getSignature() throws JavaModelException {
	IBinaryMethod info = (IBinaryMethod) getElementInfo();
	return new String(info.getMethodDescriptor());
}
/**
 * @see org.eclipse.jdt.internal.core.JavaElement#hashCode()
 */
@Override
public int hashCode() {
   int hash = super.hashCode();
	for (int i = 0, length = this.parameterTypes.length; i < length; i++) {
	    hash = Util.combineHashCodes(hash, getErasedParameterType(i).hashCode());
	}
	return hash;
}
/*
 * @see IMethod
 */
@Override
public boolean isConstructor() throws JavaModelException {
	if (!getElementName().equals(this.getParent().getElementName())) {
		// faster than reaching the info
		return false;
	}
	IBinaryMethod info = (IBinaryMethod) getElementInfo();
	return info.isConstructor();
}
/*
 * @see IMethod#isMainMethod()
 */
@Override
public boolean isMainMethod() throws JavaModelException {
	return this.isMainMethod(this);
}
/*
 * @see IMethod#isLambdaMethod()
 */
@Override
public boolean isLambdaMethod() {
	return false;
}

@Override
public boolean isResolved() {
	return false;
}
/*
 * @see IMethod#isSimilar(IMethod)
 */
@Override
public boolean isSimilar(IMethod method) {
	return
		areSimilarMethods(
			getElementName(), getParameterTypes(),
			method.getElementName(), method.getParameterTypes(),
			null);
}

@Override
public String readableName() {

	StringBuilder buffer = new StringBuilder(super.readableName());
	buffer.append("("); //$NON-NLS-1$
	String[] paramTypes = this.parameterTypes;
	int length;
	if (paramTypes != null && (length = paramTypes.length) > 0) {
		for (int i = 0; i < length; i++) {
			buffer.append(Signature.toString(paramTypes[i]));
			if (i < length - 1) {
				buffer.append(", "); //$NON-NLS-1$
			}
		}
	}
	buffer.append(")"); //$NON-NLS-1$
	return buffer.toString();
}
@Override
public JavaElement resolved(Binding binding) {
	SourceRefElement resolvedHandle = new ResolvedBinaryMethod(this.getParent(), this.name, this.parameterTypes, new String(binding.computeUniqueKey()));
	resolvedHandle.occurrenceCount = this.occurrenceCount;
	return resolvedHandle;
}/*
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
		IBinaryMethod methodInfo = (IBinaryMethod) info;
		int flags = methodInfo.getModifiers();
		if (Flags.isStatic(flags)) {
			buffer.append("static "); //$NON-NLS-1$
		}
		if (!methodInfo.isConstructor()) {
			buffer.append(Signature.toString(getReturnType(methodInfo)));
			buffer.append(' ');
		}
		toStringName(buffer, flags);
	}
}
@Override
protected void toStringName(StringBuffer buffer) {
	toStringName(buffer, 0);
}
protected void toStringName(StringBuffer buffer, int flags) {
	buffer.append(getElementName());
	buffer.append('(');
	String[] parameters = getParameterTypes();
	int length;
	if (parameters != null && (length = parameters.length) > 0) {
		boolean isVarargs = Flags.isVarargs(flags);
		for (int i = 0; i < length; i++) {
			try {
				if (i < length - 1) {
					buffer.append(Signature.toString(parameters[i]));
					buffer.append(", "); //$NON-NLS-1$
				} else if (isVarargs) {
					// remove array from signature
					String parameter = parameters[i].substring(1);
					buffer.append(Signature.toString(parameter));
					buffer.append(" ..."); //$NON-NLS-1$
				} else {
					buffer.append(Signature.toString(parameters[i]));
				}
			} catch (IllegalArgumentException e) {
				// parameter signature is malformed
				buffer.append("*** invalid signature: "); //$NON-NLS-1$
				buffer.append(parameters[i]);
			}
		}
	}
	buffer.append(')');
	if (this.occurrenceCount > 1) {
		buffer.append("#"); //$NON-NLS-1$
		buffer.append(this.occurrenceCount);
	}
}
@Override
public String getAttachedJavadoc(IProgressMonitor monitor) throws JavaModelException {
	JavadocContents javadocContents = ((BinaryType) this.getDeclaringType()).getJavadocContents(monitor);
	if (javadocContents == null) return null;
	return javadocContents.getMethodDoc(this);
}
}
