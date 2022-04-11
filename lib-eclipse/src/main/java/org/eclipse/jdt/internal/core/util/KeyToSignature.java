/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
 *	   Stephan Herrmann - Contribution for
 *								Bug 425183 - [1.8][inference] make CaptureBinding18 safe
 *								Bug 462025 - [null][test] create tests for manipulating external null annotations
 *******************************************************************************/
package org.eclipse.jdt.internal.core.util;

import java.util.ArrayList;

import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.codegen.ConstantPool;

/*
 * Converts a binding key into a signature
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class KeyToSignature extends BindingKeyParser {

	public static final int SIGNATURE = 0;
	public static final int TYPE_ARGUMENTS = 1;
	public static final int DECLARING_TYPE = 2;
	public static final int THROWN_EXCEPTIONS = 3;

	public StringBuffer signature = new StringBuffer();
	private int kind;
	private boolean asBinarySignature = false; // '.' vs. '/' and '$'
	private ArrayList arguments = new ArrayList();
	private ArrayList typeArguments = new ArrayList();
	private ArrayList typeParameters = new ArrayList();
	private ArrayList thrownExceptions = new ArrayList();
	private int mainTypeStart = -1;
	private int mainTypeEnd;
	private int typeSigStart = -1;

	public KeyToSignature(BindingKeyParser parser) {
		super(parser);
		KeyToSignature keyToSignature = (KeyToSignature) parser;
		this.kind = keyToSignature.kind;
		this.asBinarySignature = keyToSignature.asBinarySignature;
	}

	public KeyToSignature(String key, int kind) {
		super(key);
		this.kind = kind;
	}

	public KeyToSignature(String key, int kind, boolean asBinarySignature) {
		super(key);
		this.kind = kind;
		this.asBinarySignature = asBinarySignature;
	}

	@Override
	public void consumeArrayDimension(char[] brakets) {
		this.signature.append(brakets);
	}

	@Override
	public void consumeBaseType(char[] baseTypeSig) {
		this.typeSigStart = this.signature.length();
		this.signature.append(baseTypeSig);
	}

	@Override
	public void consumeCapture(int position) {
		this.signature.append('!');
		this.signature.append(((KeyToSignature) this.arguments.get(0)).signature);
	}

	@Override
	public void consumeCapture18ID(int id, int position) {
		// see https://bugs.eclipse.org/429264
		this.signature.append("!*"); // pretend a 'capture-of ?' //$NON-NLS-1$
	}

	@Override
	public void consumeLocalType(char[] uniqueKey) {
		this.signature = new StringBuffer();
		// remove trailing semi-colon as it is added later in comsumeType()
		uniqueKey = CharOperation.subarray(uniqueKey, 0, uniqueKey.length-1);
		if (!this.asBinarySignature)
			CharOperation.replace(uniqueKey, '/', '.');
		this.signature.append(uniqueKey);
	}

	@Override
	public void consumeMethod(char[] selector, char[] methodSignature) {
		this.arguments = new ArrayList();
		this.typeArguments = new ArrayList();
		if (!this.asBinarySignature)
			CharOperation.replace(methodSignature, '/', '.');
		switch(this.kind) {
			case SIGNATURE:
				this.signature = new StringBuffer();
				this.signature.append(methodSignature);
				break;
			case THROWN_EXCEPTIONS:
				if (CharOperation.indexOf('^', methodSignature) > 0) {
					char[][] types = Signature.getThrownExceptionTypes(methodSignature);
					int length = types.length;
					for (int i=0; i<length; i++) {
						this.thrownExceptions.add(new String(types[i]));
					}
				}
				break;
		}
	}

	@Override
	public void consumeMemberType(char[] simpleTypeName) {
		this.signature.append('$');
		this.signature.append(simpleTypeName);
	}

	@Override
	public void consumePackage(char[] pkgName) {
		this.signature.append(pkgName);
	}

	@Override
	public void consumeParameterizedGenericMethod() {
		this.typeArguments = this.arguments;
		int typeParametersSize = this.arguments.size();
		if (typeParametersSize > 0) {
			int sigLength = this.signature.length();
			char[] methodSignature = new char[sigLength];
			this.signature.getChars(0, sigLength, methodSignature, 0);
			char[][] typeParameterSigs = Signature.getTypeParameters(methodSignature);
			if (typeParameterSigs.length != typeParametersSize)
				return;
			this.signature = new StringBuffer();

			// type parameters
			for (int i = 0; i < typeParametersSize; i++)
				typeParameterSigs[i] = CharOperation.concat(Signature.C_TYPE_VARIABLE,Signature.getTypeVariable(typeParameterSigs[i]), Signature.C_SEMICOLON);
			int paramStart = CharOperation.indexOf(Signature.C_PARAM_START, methodSignature);
			char[] typeParametersString = CharOperation.subarray(methodSignature, 0, paramStart);
			this.signature.append(typeParametersString);

			// substitute parameters
			this.signature.append(Signature.C_PARAM_START);
			char[][] parameters = Signature.getParameterTypes(methodSignature);
			for (int i = 0, parametersLength = parameters.length; i < parametersLength; i++)
				substitute(parameters[i], typeParameterSigs, typeParametersSize);
			this.signature.append(Signature.C_PARAM_END);

			// substitute return type
			char[] returnType = Signature.getReturnType(methodSignature);
			substitute(returnType, typeParameterSigs, typeParametersSize);

			// substitute exceptions
			char[][] exceptions = Signature.getThrownExceptionTypes(methodSignature);
			for (int i = 0, exceptionsLength = exceptions.length; i < exceptionsLength; i++) {
				this.signature.append(Signature.C_EXCEPTION_START);
				substitute(exceptions[i], typeParameterSigs, typeParametersSize);
			}

		}
	}

	/*
	 * Substitutes the type variables referenced in the given parameter (a parameterized type signature) with the corresponding
	 * type argument.
	 * Appends the given parameter if it is not a parameterized type signature.
	 */
	private void substitute(char[] parameter, char[][] typeParameterSigs, int typeParametersLength) {
		for (int i = 0; i < typeParametersLength; i++) {
			if (CharOperation.equals(parameter, typeParameterSigs[i])) {
				String typeArgument = ((KeyToSignature) this.arguments.get(i)).signature.toString();
				this.signature.append(typeArgument);
				return;
			}
		}
		int genericStart = CharOperation.indexOf(Signature.C_GENERIC_START, parameter);
		if (genericStart > -1) {
			this.signature.append(CharOperation.subarray(parameter, 0, genericStart));
			char[][] parameters = Signature.getTypeArguments(parameter);
			this.signature.append(Signature.C_GENERIC_START);
			for (int j = 0, paramsLength = parameters.length; j < paramsLength; j++)
				substitute(parameters[j], typeParameterSigs, typeParametersLength);
			this.signature.append(Signature.C_GENERIC_END);
			this.signature.append(Signature.C_SEMICOLON);
		} else {
			// handle array, wildcard and capture
			int index = 0;
			int length = parameter.length;
			loop: while (index < length) {
				char current = parameter[index];
				switch (current) {
					case Signature.C_CAPTURE:
					case Signature.C_EXTENDS:
					case Signature.C_SUPER:
					case Signature.C_ARRAY:
						this.signature.append(current);
						index++;
						break;
					default:
						break loop;
				}
			}
			if (index > 0)
				substitute(CharOperation.subarray(parameter, index, length), typeParameterSigs, typeParametersLength);
			else
				this.signature.append(parameter);
		}
	}

	@Override
	public void consumeParameterizedType(char[] simpleTypeName, boolean isRaw) {
		if (simpleTypeName != null) {
			// member type
			this.signature.append(this.asBinarySignature ? '$' : '.');
			this.signature.append(simpleTypeName);
		}
		if (!isRaw) {
			this.signature.append('<');
			int length = this.arguments.size();
			for (int i = 0; i < length; i++) {
				this.signature.append(((KeyToSignature) this.arguments.get(i)).signature);
			}
			this.signature.append('>');
			this.typeArguments = this.arguments;
			this.arguments = new ArrayList();
		}
	}

	@Override
	public void consumeParser(BindingKeyParser parser) {
		this.arguments.add(parser);
	}

	@Override
	public void consumeField(char[] fieldName) {
		if (this.kind == SIGNATURE) {
			this.signature = ((KeyToSignature) this.arguments.get(0)).signature;
		}
	}

	@Override
	public void consumeException() {
		int size = this.arguments.size();
		if (size > 0) {
			for (int i=0; i<size; i++) {
				this.thrownExceptions.add(((KeyToSignature) this.arguments.get(i)).signature.toString());
			}
			this.arguments = new ArrayList();
			this.typeArguments = new ArrayList();
		}
	}

	@Override
	public void consumeFullyQualifiedName(char[] fullyQualifiedName) {
		this.typeSigStart = this.signature.length();
		this.signature.append('L');
		if (!this.asBinarySignature)
			fullyQualifiedName = CharOperation.replaceOnCopy(fullyQualifiedName, '/', '.');
		this.signature.append(fullyQualifiedName);
	}

	@Override
	public void consumeSecondaryType(char[] simpleTypeName) {
		this.signature.append('~');
		this.mainTypeStart = this.signature.lastIndexOf(this.asBinarySignature ? "/" : ".") + 1; //$NON-NLS-1$ //$NON-NLS-2$
		if (this.mainTypeStart == 0) {
			this.mainTypeStart = 1; // default package (1 for the 'L')
			int i = 0;
			// we need to preserve the array if needed
			while (this.signature.charAt(i) == Signature.C_ARRAY) {
				this.mainTypeStart ++;
				i++;
			}
		}
		this.mainTypeEnd = this.signature.length();
		this.signature.append(simpleTypeName);
	}

	@Override
	public void consumeType() {
		// remove main type if needed
		if (this.mainTypeStart != -1) {
			this.signature.replace(this.mainTypeStart, this.mainTypeEnd, ""); //$NON-NLS-1$
		}
		// parameter types
		int length = this.typeParameters.size();
		if (length > 0) {
			StringBuilder typeParametersSig = new StringBuilder();
			typeParametersSig.append('<');
			for (int i = 0; i < length; i++) {
				char[] typeParameterSig = Signature.createTypeParameterSignature(
						(char[]) this.typeParameters.get(i),
						new char[][]{ ConstantPool.ObjectSignature });
				typeParametersSig.append(typeParameterSig);
				// TODO (jerome) add type parameter bounds in binding key
			}
			typeParametersSig.append('>');
			this.signature.insert(this.typeSigStart, typeParametersSig.toString());
			this.typeParameters = new ArrayList();
		}
		this.signature.append(';');
	}

	@Override
	public void consumeTypeParameter(char[] typeParameterName) {
		this.typeParameters.add(typeParameterName);
	}

	@Override
	public void consumeTypeVariable(char[] position, char[] typeVariableName) {
		this.signature = new StringBuffer();
		this.signature.append('T');
		this.signature.append(typeVariableName);
		this.signature.append(';');
	}

	@Override
	public void consumeTypeWithCapture() {
		KeyToSignature keyToSignature = (KeyToSignature) this.arguments.get(0);
		this.signature = keyToSignature.signature;
		this.arguments = keyToSignature.arguments;
		this.typeArguments = keyToSignature.typeArguments;
		this.thrownExceptions = keyToSignature.thrownExceptions;
	}

	@Override
	public void consumeWildCard(int wildCardKind) {
		// don't put generic type in signature
		this.signature = new StringBuffer();
		switch (wildCardKind) {
			case Wildcard.UNBOUND:
				this.signature.append('*');
				break;
			case Wildcard.EXTENDS:
				this.signature.append('+');
				this.signature.append(((KeyToSignature) this.arguments.get(0)).signature);
				break;
			case Wildcard.SUPER:
				this.signature.append('-');
				this.signature.append(((KeyToSignature) this.arguments.get(0)).signature);
				break;
			default:
				// malformed
				return;
		}
	}

	public String[] getThrownExceptions() {
		int length = this.thrownExceptions.size();
		String[] result = new String[length];
		for (int i = 0; i < length; i++) {
			result[i] = (String) this.thrownExceptions.get(i);
		}
		return result;
	}

	public String[] getTypeArguments() {
		int length = this.typeArguments.size();
		String[] result = new String[length];
		for (int i = 0; i < length; i++) {
			result[i] = ((KeyToSignature) this.typeArguments.get(i)).signature.toString();
		}
		return result;
	}

	@Override
	public BindingKeyParser newParser() {
		return new KeyToSignature(this);
	}

	@Override
	public String toString() {
		return this.signature.toString();
	}

}
