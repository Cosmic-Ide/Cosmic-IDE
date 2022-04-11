/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
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
 *     Andreas Magnusson <andreas.ch.magnusson@gmail.com>- contribution for bug 151500
 *******************************************************************************/
package org.eclipse.jdt.internal.codeassist;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.CompletionFlags;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.IBinaryMethod;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.core.BinaryType;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.NameLookup;
import org.eclipse.jdt.internal.core.NamedMember;
import org.eclipse.jdt.internal.core.SourceMapper;

/**
 * Internal completion proposal
 * @since 3.1
 */
public class InternalCompletionProposal extends CompletionProposal {
	private static Object NO_ATTACHED_SOURCE = new Object();

	protected CompletionEngine completionEngine;
	protected NameLookup nameLookup;

	protected char[] declarationPackageName;
	protected char[] declarationTypeName;
	protected char[] moduleName;
	protected char[] packageName;
	protected char[] typeName;
	protected char[][] parameterPackageNames;
	protected char[][] parameterTypeNames;

	protected char[] originalSignature;

	private boolean hasNoParameterNamesFromIndex = false;
	private boolean updateCompletion = false;

	protected int accessibility = IAccessRule.K_ACCESSIBLE;

	protected boolean isConstructor = false;

	/**
	 * Kind of completion request.
	 */
	private int completionKind;

	/**
	 * Offset in original buffer where ICodeAssist.codeComplete() was
	 * requested.
	 */
	private int completionLocation;

	/**
	 * Start position (inclusive) of source range in original buffer
	 * containing the relevant token
	 * defaults to empty subrange at [0,0).
	 */
	private int tokenStart = 0;

	/**
	 * End position (exclusive) of source range in original buffer
	 * containing the relevant token;
	 * defaults to empty subrange at [0,0).
	 */
	private int tokenEnd = 0;

	/**
	 * Completion string; defaults to empty string.
	 */
	private char[] completion = CharOperation.NO_CHAR;

	/**
	 * Start position (inclusive) of source range in original buffer
	 * to be replaced by completion string;
	 * defaults to empty subrange at [0,0).
	 */
	private int replaceStart = 0;

	/**
	 * End position (exclusive) of source range in original buffer
	 * to be replaced by completion string;
	 * defaults to empty subrange at [0,0).
	 */
	private int replaceEnd = 0;

	/**
	 * Relevance rating; positive; higher means better;
	 * defaults to minimum rating.
	 */
	private int relevance = 1;

	/**
	 * Signature of the relevant package or type declaration
	 * in the context, or <code>null</code> if none.
	 * Defaults to null.
	 */
	private char[] declarationSignature = null;

	/**
	 * Unique key of the relevant package or type declaration
	 * in the context, or <code>null</code> if none.
	 * Defaults to null.
	 */
	private char[] declarationKey = null;

	/**
	 * Simple name of the method, field,
	 * member, or variable relevant in the context, or
	 * <code>null</code> if none.
	 * Defaults to null.
	 */
	private char[] name = null;

	/**
	 * Binding of the method or constructor being proposed, or
	 * <code>null</code> if none.
	 * Defaults to null.
	 */
	private Binding binding = null;

	/**
	 * Signature of the method, field type, member type,
	 * relevant in the context, or <code>null</code> if none.
	 * Defaults to null.
	 */
	private char[] signature = null;

	/**
	 * Unique of the method, field type, member type,
	 * relevant in the context, or <code>null</code> if none.
	 * Defaults to null.
	 */
	private char[] key = null;

	/**
	 * Array of required completion proposals, or <code>null</code> if none.
	 * The proposal can not be applied if the required proposals aren't applied.
	 * Defaults to <code>null</code>.
	 */
	private CompletionProposal[] requiredProposals;

	/**
	 * Modifier flags relevant in the context, or
	 * <code>Flags.AccDefault</code> if none.
	 * Defaults to <code>Flags.AccDefault</code>.
	 */
	private int flags = Flags.AccDefault;

	/**
	 * Completion flags relevant in the context, or
	 * <code>CompletionFlags.Default</code> if none.
	 * Defaults to <code>CompletionFlags.Default</code>.
	 */
	private int additionalFlags = CompletionFlags.Default;

	/**
	 * Parameter names (for method completions), or
	 * <code>null</code> if none. Lazily computed.
	 * Defaults to <code>null</code>.
	 */
	private char[][] parameterNames = null;

	/**
	 * Indicates whether parameter names have been computed.
	 */
	private boolean parameterNamesComputed = false;

	protected char[][] findConstructorParameterNames(char[] declaringTypePackageName, char[] declaringTypeName, char[] selector, char[][] paramTypeNames){
		if(paramTypeNames == null || declaringTypeName == null) return null;

		char[][] parameters = null;
		int length = paramTypeNames.length;

		char[] tName = CharOperation.concat(declaringTypePackageName,declaringTypeName,'.');
		Object cachedType = this.completionEngine.typeCache.get(tName);

		IType type = null;
		if(cachedType != null) {
			if(cachedType != NO_ATTACHED_SOURCE && cachedType instanceof BinaryType) {
				type = (BinaryType)cachedType;
			}
		} else {
			// TODO (david) shouldn't it be NameLookup.ACCEPT_ALL ?
			NameLookup.Answer answer = this.nameLookup.findType(new String(declaringTypeName),
				new String(declaringTypePackageName),
				false,
				NameLookup.ACCEPT_CLASSES & NameLookup.ACCEPT_INTERFACES,
				true/* consider secondary types */,
				false/* do NOT wait for indexes */,
				false/*don't check restrictions*/,
				null);
			type = answer == null ? null : answer.type;
			if(type instanceof BinaryType){
				this.completionEngine.typeCache.put(tName, type);
			} else {
				type = null;
			}
		}

		if(type != null) {
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=316937
			// BinaryType#getMethod() creates a new instance of BinaryMethod, which is a dummy.
			// Instead we have to use IType#findMethods() to get a handle to the method of our interest.
			try {
				IMethod method = findMethod(type, selector, paramTypeNames);
				if (this.hasNoParameterNamesFromIndex) {

					IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot)type.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
					if (packageFragmentRoot.isArchive() ||
							this.completionEngine.openedBinaryTypes < getOpenedBinaryTypesThreshold()) {
						SourceMapper mapper = ((JavaElement)method).getSourceMapper();
						if (mapper != null) {
							char[][] paramNames = mapper.getMethodParameterNames(method);

							// map source and try to find parameter names
							if(paramNames == null) {
								if (!packageFragmentRoot.isArchive()) this.completionEngine.openedBinaryTypes++;
								IBinaryType info = (IBinaryType) ((BinaryType) type).getElementInfo();
								char[] source = mapper.findSource(type, info);
								if (source != null){
									mapper.mapSource((NamedMember) type, source, info);
								}
								paramNames = mapper.getMethodParameterNames(method);
							}

							if(paramNames != null) {
								parameters = paramNames;
							}
						}
					}
				} else {
					IBinaryMethod info = (IBinaryMethod) ((JavaElement)method).getElementInfo();
					char[][] argumentNames = info.getArgumentNames();
					if (argumentNames != null && argumentNames.length == length) {
						parameters = argumentNames;
						return parameters;
					}

					parameters = new char[length][];
					String[] params = method.getParameterNames();
					for(int i = 0;	i< length ; i++){
						parameters[i] = params[i].toCharArray();
					}
				}
			} catch(JavaModelException e){
				parameters = null;
			}
		}

		// default parameters name
		if(parameters == null) {
			parameters = CompletionEngine.createDefaultParameterNames(length);
		}

		return parameters;
	}

	protected char[][] findMethodParameterNames(char[] declaringTypePackageName, char[] declaringTypeName, char[] selector, char[][] paramTypeNames){
		if(paramTypeNames == null || declaringTypeName == null) return null;

		char[][] parameters = null;
		int length = paramTypeNames.length;

		char[] tName = CharOperation.concat(declaringTypePackageName,declaringTypeName,'.');
		Object cachedType = this.completionEngine.typeCache.get(tName);

		IType type = null;
		if(cachedType != null) {
			if(cachedType != NO_ATTACHED_SOURCE && cachedType instanceof BinaryType) {
				type = (BinaryType)cachedType;
			}
		} else {
			// TODO (david) shouldn't it be NameLookup.ACCEPT_ALL ?
			NameLookup.Answer answer = this.nameLookup.findType(new String(declaringTypeName),
				new String(declaringTypePackageName),
				false,
				NameLookup.ACCEPT_CLASSES & NameLookup.ACCEPT_INTERFACES,
				true/* consider secondary types */,
				false/* do NOT wait for indexes */,
				false/*don't check restrictions*/,
				null);
			type = answer == null ? null : answer.type;
			if(type instanceof BinaryType){
				this.completionEngine.typeCache.put(tName, type);
			} else {
				type = null;
			}
		}

		if(type != null) {
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=316937
			// BinaryType#getMethod() creates a new instance of BinaryMethod, which is a dummy.
			// Instead we have to use IType#findMethods() to get a handle to the method of our interest.
			try{
				IMethod method = findMethod(type, selector, paramTypeNames);
				parameters = new char[length][];
				String[] params = method.getParameterNames();
				for(int i = 0;	i< length ; i++){
					parameters[i] = params[i].toCharArray();
				}
			} catch(JavaModelException e){
				parameters = null;
			}
		}

		// default parameters name
		if(parameters == null) {
			parameters = CompletionEngine.createDefaultParameterNames(length);
		}

		return parameters;
	}

	private IMethod findMethod(IType type, char[] selector, char[][] paramTypeNames) throws JavaModelException {
		IMethod method = null;
		int startingIndex = 0;
		String[] args;
		IType enclosingType = type.getDeclaringType();
		// If the method is a constructor of a non-static inner type, add the enclosing type as an
		// additional parameter to the constructor
		if (enclosingType != null
				&& CharOperation.equals(type.getElementName().toCharArray(), selector)
				&& !Flags.isStatic(type.getFlags())) {
			args = new String[paramTypeNames.length+1];
			startingIndex = 1;
			args[0] = Signature.createTypeSignature(enclosingType.getFullyQualifiedName(), true);
		} else {
			args = new String[paramTypeNames.length];
		}
		int length = args.length;
		for(int i = startingIndex;	i< length ; i++){
			args[i] = new String(paramTypeNames[i-startingIndex]);
		}
		method = type.getMethod(new String(selector), args);

		IMethod[] methods = type.findMethods(method);
		if (methods != null && methods.length > 0) {
			method = methods[0];
		}
		return method;
	}

	protected char[] getDeclarationPackageName() {
		return this.declarationPackageName;
	}

	protected char[] getDeclarationTypeName() {
		return this.declarationTypeName;
	}

	private int getOpenedBinaryTypesThreshold() {
		return JavaModelManager.getJavaModelManager().getOpenableCacheSize() / 10;
	}

	protected char[] getModuleName() {
		return this.moduleName;
	}

	protected char[] getPackageName() {
		return this.packageName;
	}

	protected char[] getTypeName() {
		return this.typeName;
	}

	protected char[][] getParameterPackageNames() {
		return this.parameterPackageNames;
	}


	protected char[][] getParameterTypeNames() {
		return this.parameterTypeNames;
	}

	protected void setDeclarationPackageName(char[] declarationPackageName) {
		this.declarationPackageName = declarationPackageName;
	}

	protected void setDeclarationTypeName(char[] declarationTypeName) {
		this.declarationTypeName = declarationTypeName;
	}

	protected void setModuleName(char[] moduleName) {
		this.moduleName = moduleName;
	}

	protected void setPackageName(char[] packageName) {
		this.packageName = packageName;
	}

	protected void setTypeName(char[] typeName) {
		this.typeName = typeName;
	}

	protected void setParameterPackageNames(char[][] parameterPackageNames) {
		this.parameterPackageNames = parameterPackageNames;
	}

	protected void setParameterTypeNames(char[][] parameterTypeNames) {
		this.parameterTypeNames = parameterTypeNames;
	}

	protected void setAccessibility(int kind) {
		this.accessibility = kind;
	}

	protected void setIsContructor(boolean isConstructor) {
		this.isConstructor = isConstructor;
	}
	public void setOriginalSignature(char[] originalSignature) {
		this.originalSignature = originalSignature;
	}
	/**
	 * Creates a basic completion proposal. All instance
	 * field have plausible default values unless otherwise noted.
	 * <p>
	 * Note that the constructors for this class are internal to the
	 * Java model implementation. Clients cannot directly create
	 * CompletionProposal objects.
	 * </p>
	 *
	 * @param kind one of the kind constants declared on this class
	 * @param completionLocation original offset of code completion request
	 */
	public InternalCompletionProposal(int kind, int completionLocation) {
		if ((kind < FIRST_KIND)
				|| (kind > LAST_KIND)) {
			throw new IllegalArgumentException();
		}
		if (this.completion == null || completionLocation < 0) {
			// Work around for bug 132558 (https://bugs.eclipse.org/bugs/show_bug.cgi?id=132558).
			// completionLocation can be -1 if the completion occur at the start of a file or
			// the start of a code snippet but this API isn't design to support negative position.
			if(this.completion == null || completionLocation != -1) {
				throw new IllegalArgumentException();
			}
			completionLocation = 0;
		}
		this.completionKind = kind;
		this.completionLocation = completionLocation;
	}

	@Override
	public int getAdditionalFlags() {
		return this.additionalFlags;
	}

	@Override
	public void setAdditionalFlags(int additionalFlags) {
		this.additionalFlags = additionalFlags;
	}

	@Override
	public int getKind() {
		return this.completionKind;
	}

	// TODO (david) https://bugs.eclipse.org/bugs/show_bug.cgi?id=132558
	@Override
	public int getCompletionLocation() {
		return this.completionLocation;
	}

	@Override
	public int getTokenStart() {
		return this.tokenStart;
	}

	@Override
	public int getTokenEnd() {
		return this.tokenEnd;
	}

	@Override
	public void setTokenRange(int startIndex, int endIndex) {
		if (startIndex < 0 || endIndex < startIndex) {
			throw new IllegalArgumentException();
		}
		this.tokenStart = startIndex;
		this.tokenEnd = endIndex;
	}

	@Override
	public char[] getCompletion() {
		if(this.completionKind == METHOD_DECLARATION) {
			findParameterNames(null);
			if(this.updateCompletion) {
				this.updateCompletion = false;

				if(this.parameterNames != null) {
					int length = this.parameterNames.length;
					StringBuilder completionBuffer = new StringBuilder(this.completion.length);

					int start = 0;
					int end = CharOperation.indexOf('%', this.completion);

					completionBuffer.append(this.completion, start, end - start);

					for(int i = 0 ; i < length ; i++){
						completionBuffer.append(this.parameterNames[i]);
						start = end + 1;
						end = CharOperation.indexOf('%', this.completion, start);
						if(end > -1){
							completionBuffer.append(this.completion, start, end - start);
						} else {
							completionBuffer.append(this.completion, start, this.completion.length - start);
						}
					}
					int nameLength = completionBuffer.length();
					this.completion = new char[nameLength];
					completionBuffer.getChars(0, nameLength, this.completion, 0);
				}
			}
		}
		return this.completion;
	}

	@Override
	public void setCompletion(char[] completion) {
		this.completion = completion;
	}

	@Override
	public int getReplaceStart() {
		return this.replaceStart;
	}

	@Override
	public int getReplaceEnd() {
		return this.replaceEnd;
	}

	@Override
	public void setReplaceRange(int startIndex, int endIndex) {
		if (startIndex < 0 || endIndex < startIndex) {
			throw new IllegalArgumentException();
		}
		this.replaceStart = startIndex;
		this.replaceEnd = endIndex;
	}

	@Override
	public int getRelevance() {
		return this.relevance;
	}

	@Override
	public void setRelevance(int rating) {
		if (rating <= 0) {
			throw new IllegalArgumentException();
		}
		this.relevance = rating;
	}

	@Override
	public char[] getDeclarationSignature() {
		return this.declarationSignature;
	}

	@Override
	public char[] getDeclarationKey() {
		return this.declarationKey;
	}

	@Override
	public void setDeclarationSignature(char[] signature) {
		this.declarationSignature = signature;
	}

	@Override
	public void setDeclarationKey(char[] key) {
		this.declarationKey = key;
	}

	@Override
	public char[] getName() {
		return this.name;
	}

	@Override
	public void setName(char[] name) {
		this.name = name;
	}

	/**
	 * Returns a binding of the method or field corresponding to this proposal or <code>null</code> if none.
	 * <p>
	 * The binding <em>may</em> be available for the following kinds of completion proposals:
	 * <ul>
	 *  <li><code>ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION</code> - {@link MethodBinding}
	 * of the constructor being proposed</li>
	 *  <li><code>CONSTRUCTOR_INVOCATION</code> - {@link MethodBinding}
	 * of the constructor being proposed</li>
	 *  <li><code>FIELD_REF</code> - {@link FieldBinding}
	 * of the field being proposed</li>
	 *  <li><code>FIELD_REF_WITH_CASTED_RECEIVER</code> - {@link FieldBinding}
	 * of the field being proposed</li>
	 *  <li><code>JAVADOC_FIELD_REF</code> - {@link FieldBinding}
	 * of the field being proposed</li>
	 *  <li><code>JAVADOC_METHOD_REF</code> - {@link MethodBinding}
	 * of the method or constructor being proposed</li>
	 *  <li><code>METHOD_DECLARATION</code> - {@link MethodBinding}
	 * of the method or constructor being proposed</li>
	 *  <li><code>METHOD_NAME_REFERENCE</code> - {@link MethodBinding}
	 * of the method or constructor being proposed</li>
	 *  <li><code>METHOD_REF</code> - {@link MethodBinding}
	 * of the method or constructor being proposed</li>
	 *  <li><code>METHOD_REF_WITH_CASTED_RECEIVER</code> - {@link MethodBinding}
	 * of the method or constructor being proposed</li>
	 * </ul>
	 * For other kinds of completion proposals, this method returns <code>null</code>.
	 * </p>
	 *
	 * @return the binding corresponding to this proposal (if available), or <code>null</code> if none
	 */
	public Binding getBinding() {
		return this.binding;
	}

	public void setBinding(Binding binding) {
		this.binding = binding;
	}

	@Override
	public char[] getSignature() {
		return this.signature;
	}

	@Override
	public char[] getKey() {
		return this.key;
	}

//	/**
//	 * Returns the package name of the relevant
//	 * declaration in the context, or <code>null</code> if none.
//	 * <p>
//	 * This field is available for the following kinds of
//	 * completion proposals:
//	 * <ul>
//	 * <li><code>ANONYMOUS_CLASS_DECLARATION</code> - the dot-based package name
//	 * of the type that is being subclassed or implemented</li>
//	 * 	<li><code>FIELD_REF</code> - the dot-based package name
//	 * of the type that declares the field that is referenced</li>
//	 * 	<li><code>METHOD_REF</code> - the dot-based package name
//	 * of the type that declares the method that is referenced</li>
//	 * 	<li><code>METHOD_DECLARATION</code> - the dot-based package name
//	 * of the type that declares the method that is being
//	 * implemented or overridden</li>
//	 * </ul>
//	 * For kinds of completion proposals, this method returns
//	 * <code>null</code>. Clients must not modify the array
//	 * returned.
//	 * </p>
//	 *
//	 * @return the dot-based package name, or
//	 * <code>null</code> if none
//	 * @see #getDeclarationSignature()
//	 * @see #getSignature()
//	 *
//	 * @since 3.1
//	 */
//	public char[] getDeclarationPackageName() {
//		return this.declarationPackageName;
//	}
//
//	/**
//	 * Returns the type name of the relevant
//	 * declaration in the context without the package fragment,
//	 * or <code>null</code> if none.
//	 * <p>
//	 * This field is available for the following kinds of
//	 * completion proposals:
//	 * <ul>
//	 * <li><code>ANONYMOUS_CLASS_DECLARATION</code> - the dot-based type name
//	 * of the type that is being subclassed or implemented</li>
//	 * 	<li><code>FIELD_REF</code> - the dot-based type name
//	 * of the type that declares the field that is referenced
//	 * or an anonymous type instantiation ("new X(){}") if it is an anonymous type</li>
//	 * 	<li><code>METHOD_REF</code> - the dot-based type name
//	 * of the type that declares the method that is referenced
//	 * or an anonymous type instantiation ("new X(){}") if it is an anonymous type</li>
//	 * 	<li><code>METHOD_DECLARATION</code> - the dot-based type name
//	 * of the type that declares the method that is being
//	 * implemented or overridden</li>
//	 * </ul>
//	 * For kinds of completion proposals, this method returns
//	 * <code>null</code>. Clients must not modify the array
//	 * returned.
//	 * </p>
//	 *
//	 * @return the dot-based package name, or
//	 * <code>null</code> if none
//	 * @see #getDeclarationSignature()
//	 * @see #getSignature()
//	 *
//	 * @since 3.1
//	 */
//	public char[] getDeclarationTypeName() {
//		return this.declarationTypeName;
//	}
//
//	/**
//	 * Returns the package name of the method or type
//	 * relevant in the context, or <code>null</code> if none.
//	 * <p>
//	 * This field is available for the following kinds of
//	 * completion proposals:
//	 * <ul>
//	 * 	<li><code>FIELD_REF</code> - the dot-based package name
//	 * of the referenced field's type</li>
//	 * 	<li><code>LOCAL_VARIABLE_REF</code> - the dot-based package name
//	 * of the referenced local variable's type</li>
//	 * 	<li><code>METHOD_REF</code> -  the dot-based package name
//	 * of the return type of the method that is referenced</li>
//	 * 	<li><code>METHOD_DECLARATION</code> - the dot-based package name
//	 * of the return type of the method that is being implemented
//	 * or overridden</li>
//	 * 	<li><code>PACKAGE_REF</code> - the dot-based package name
//	 * of the package that is referenced</li>
//	 * 	<li><code>TYPE_REF</code> - the dot-based package name
//	 * of the type that is referenced</li>
//	 * 	<li><code>VARIABLE_DECLARATION</code> - the dot-based package name
//	 * of the type of the variable being declared</li>
//	 * </ul>
//	 * For kinds of completion proposals, this method returns
//	 * <code>null</code>. Clients must not modify the array
//	 * returned.
//	 * </p>
//	 *
//	 * @return the package name, or <code>null</code> if none
//	 *
//	 * @see #getDeclarationSignature()
//	 * @see #getSignature()
//	 *
//	 * @since 3.1
//	 */
//	public char[] getPackageName() {
//		return this.packageName;
//	}
//
//	/**
//	 * Returns the type name without the package fragment of the method or type
//	 * relevant in the context, or <code>null</code> if none.
//	 * <p>
//	 * This field is available for the following kinds of
//	 * completion proposals:
//	 * <ul>
//	 * 	<li><code>FIELD_REF</code> - the dot-based type name
//	 * of the referenced field's type</li>
//	 * 	<li><code>LOCAL_VARIABLE_REF</code> - the dot-based type name
//	 * of the referenced local variable's type</li>
//	 * 	<li><code>METHOD_REF</code> -  the dot-based type name
//	 * of the return type of the method that is referenced</li>
//	 * 	<li><code>METHOD_DECLARATION</code> - the dot-based type name
//	 * of the return type of the method that is being implemented
//	 * or overridden</li>
//	 * 	<li><code>TYPE_REF</code> - the dot-based type name
//	 * of the type that is referenced</li>
//	 * 	<li><code>VARIABLE_DECLARATION</code> - the dot-based package name
//	 * of the type of the variable being declared</li>
//	 * </ul>
//	 * For kinds of completion proposals, this method returns
//	 * <code>null</code>. Clients must not modify the array
//	 * returned.
//	 * </p>
//	 *
//	 * @return the package name, or <code>null</code> if none
//	 *
//	 * @see #getDeclarationSignature()
//	 * @see #getSignature()
//	 *
//	 * @since 3.1
//	 */
//	public char[] getTypeName() {
//		return this.typeName;
//	}
//
//	/**
//	 * Returns the parameter package names of the method
//	 * relevant in the context, or <code>null</code> if none.
//	 * <p>
//	 * This field is available for the following kinds of
//	 * completion proposals:
//	 * <ul>
//	 * 	<li><code>ANONYMOUS_CLASS_DECLARATION</code> - parameter package names
//	 * of the constructor that is being invoked</li>
//	 * 	<li><code>METHOD_REF</code> - parameter package names
//	 * of the method that is referenced</li>
//	 * 	<li><code>METHOD_DECLARATION</code> - parameter package names
//	 * of the method that is being implemented or overridden</li>
//	 * </ul>
//	 * For kinds of completion proposals, this method returns
//	 * <code>null</code>. Clients must not modify the array
//	 * returned.
//	 * </p>
//	 *
//	 * @return the package name, or <code>null</code> if none
//	 *
//	 * @see #getDeclarationSignature()
//	 * @see #getSignature()
//	 *
//	 * @since 3.1
//	 */
//	public char[][] getParameterPackageNames() {
//		return this.parameterPackageNames;
//	}
//
//	/**
//	 * Returns the parameter type names without the package fragment of
//	 * the method relevant in the context, or <code>null</code> if none.
//	 * <p>
//	 * This field is available for the following kinds of
//	 * completion proposals:
//	 * <ul>
//	 * 	<li><code>ANONYMOUS_CLASS_DECLARATION</code> - parameter type names
//	 * of the constructor that is being invoked</li>
//	 * 	<li><code>METHOD_REF</code> - parameter type names
//	 * of the method that is referenced</li>
//	 * 	<li><code>METHOD_DECLARATION</code> - parameter type names
//	 * of the method that is being implemented or overridden</li>
//	 * </ul>
//	 * For kinds of completion proposals, this method returns
//	 * <code>null</code>. Clients must not modify the array
//	 * returned.
//	 * </p>
//	 *
//	 * @return the package name, or <code>null</code> if none
//	 *
//	 * @see #getDeclarationSignature()
//	 * @see #getSignature()
//	 *
//	 * @since 3.1
//	 */
//	public char[][] getParameterTypeNames() {
//		return this.parameterTypeNames;
//	}

	@Override
	public void setSignature(char[] signature) {
		this.signature = signature;
	}

	@Override
	public void setKey(char[] key) {
		this.key = key;
	}

	@Override
	public int getFlags() {
		return this.flags;
	}

	@Override
	public void setFlags(int flags) {
		this.flags = flags;
	}

	public void setHasNoParameterNamesFromIndex(boolean hasNoParameterNamesFromIndex) {
		this.hasNoParameterNamesFromIndex = hasNoParameterNamesFromIndex;
	}

	@Override
	public CompletionProposal[] getRequiredProposals() {
		return this.requiredProposals;
	}

	@Override
	public void setRequiredProposals(CompletionProposal[] proposals) {
		this.requiredProposals = proposals;
	}

	@Override
	public char[][] findParameterNames(IProgressMonitor monitor) {
		if (!this.parameterNamesComputed) {
			this.parameterNamesComputed = true;

			switch(this.completionKind) {
				case ANONYMOUS_CLASS_DECLARATION:
					try {
						this.parameterNames = findMethodParameterNames(
								this.declarationPackageName,
								this.declarationTypeName,
								CharOperation.lastSegment(this.declarationTypeName, '.'),
								Signature.getParameterTypes(this.originalSignature == null ? this.signature : this.originalSignature));
					} catch(IllegalArgumentException e) {
						// protection for invalid signature
						if(this.parameterTypeNames != null) {
							this.parameterNames =  CompletionEngine.createDefaultParameterNames(this.parameterTypeNames.length);
						} else {
							this.parameterNames = null;
						}
					}
					break;
				case ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION:
					try {
						this.parameterNames = findConstructorParameterNames(
								this.declarationPackageName,
								this.declarationTypeName,
								CharOperation.lastSegment(this.declarationTypeName, '.'),
								Signature.getParameterTypes(this.originalSignature == null ? this.signature : this.originalSignature));
					} catch(IllegalArgumentException e) {
						// protection for invalid signature
						if(this.parameterTypeNames != null) {
							this.parameterNames =  CompletionEngine.createDefaultParameterNames(this.parameterTypeNames.length);
						} else {
							this.parameterNames = null;
						}
					}
					break;
				case METHOD_REF:
				case METHOD_REF_WITH_CASTED_RECEIVER:
				case LAMBDA_EXPRESSION:
					try {
						this.parameterNames = findMethodParameterNames(
								this.declarationPackageName,
								this.declarationTypeName,
								this.name,
								Signature.getParameterTypes(this.originalSignature == null ? this.signature : this.originalSignature));
					} catch(IllegalArgumentException e) {
						// protection for invalid signature
						if(this.parameterTypeNames != null) {
							this.parameterNames =  CompletionEngine.createDefaultParameterNames(this.parameterTypeNames.length);
						} else {
							this.parameterNames = null;
						}
					}
					break;
				case CONSTRUCTOR_INVOCATION:
					try {
						this.parameterNames = findConstructorParameterNames(
								this.declarationPackageName,
								this.declarationTypeName,
								this.name,
								Signature.getParameterTypes(this.originalSignature == null ? this.signature : this.originalSignature));
					} catch(IllegalArgumentException e) {
						// protection for invalid signature
						if(this.parameterTypeNames != null) {
							this.parameterNames =  CompletionEngine.createDefaultParameterNames(this.parameterTypeNames.length);
						} else {
							this.parameterNames = null;
						}
					}
					break;
				case METHOD_DECLARATION:
					try {
						this.parameterNames = findMethodParameterNames(
								this.declarationPackageName,
								this.declarationTypeName,
								this.name,
								Signature.getParameterTypes(this.originalSignature == null ? this.signature : this.originalSignature));
					} catch(IllegalArgumentException e) {
						// protection for invalid signature
						if(this.parameterTypeNames != null) {
							this.parameterNames =  CompletionEngine.createDefaultParameterNames(this.parameterTypeNames.length);
						} else {
							this.parameterNames = null;
						}
					}
					if(this.parameterNames != null) {
						this.updateCompletion = true;
					}
					break;
			}
		}
		return this.parameterNames;
	}

	@Override
	public void setParameterNames(char[][] parameterNames) {
		this.parameterNames = parameterNames;
		this.parameterNamesComputed = true;
	}

	@Override
	public int getAccessibility() {
		return this.accessibility;
	}

	@Override
	public boolean isConstructor() {
		return this.isConstructor;
	}

	private int receiverStart;
	private int receiverEnd;
	private char[] receiverSignature;

	@Override
	public char[] getReceiverSignature() {
		return this.receiverSignature;
	}

	@Override
	public int getReceiverStart() {
		return this.receiverStart;
	}

	@Override
	public int getReceiverEnd() {
		return this.receiverEnd;
	}

	@Override
	public void setReceiverSignature(char[] signature) {
		this.receiverSignature = signature;
	}

	@Override
	public void setReceiverRange(int startIndex, int endIndex) {
		this.receiverStart = startIndex;
		this.receiverEnd = endIndex;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append('[');
		switch(this.completionKind) {
			case CompletionProposal.ANONYMOUS_CLASS_DECLARATION :
				buffer.append("ANONYMOUS_CLASS_DECLARATION"); //$NON-NLS-1$
				break;
			case CompletionProposal.FIELD_REF :
				buffer.append("FIELD_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.KEYWORD :
				buffer.append("KEYWORD"); //$NON-NLS-1$
				break;
			case CompletionProposal.LABEL_REF :
				buffer.append("LABEL_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.LOCAL_VARIABLE_REF :
				buffer.append("LOCAL_VARIABLE_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.METHOD_DECLARATION :
				buffer.append("METHOD_DECLARATION"); //$NON-NLS-1$
				if(this.isConstructor) {
					buffer.append("<CONSTRUCTOR>"); //$NON-NLS-1$
				}
				break;
			case CompletionProposal.METHOD_REF :
				buffer.append("METHOD_REF"); //$NON-NLS-1$
				if(this.isConstructor) {
					buffer.append("<CONSTRUCTOR>"); //$NON-NLS-1$
				}
				break;
			case CompletionProposal.MODULE_DECLARATION :
				buffer.append("MODULE_DECLARATION"); //$NON-NLS-1$
				break;
			case CompletionProposal.MODULE_REF :
				buffer.append("MODULE_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.PACKAGE_REF :
				buffer.append("PACKAGE_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.TYPE_REF :
				buffer.append("TYPE_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.VARIABLE_DECLARATION :
				buffer.append("VARIABLE_DECLARATION"); //$NON-NLS-1$
				break;
			case CompletionProposal.POTENTIAL_METHOD_DECLARATION :
				buffer.append("POTENTIAL_METHOD_DECLARATION"); //$NON-NLS-1$
				break;
			case CompletionProposal.METHOD_NAME_REFERENCE :
				buffer.append("METHOD_NAME_REFERENCE"); //$NON-NLS-1$
				break;
			case CompletionProposal.ANNOTATION_ATTRIBUTE_REF :
				buffer.append("ANNOTATION_ATTRIBUTE_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.JAVADOC_BLOCK_TAG :
				buffer.append("JAVADOC_BLOCK_TAG"); //$NON-NLS-1$
				break;
			case CompletionProposal.JAVADOC_INLINE_TAG :
				buffer.append("JAVADOC_INLINE_TAG"); //$NON-NLS-1$
				break;
			case CompletionProposal.JAVADOC_FIELD_REF:
				buffer.append("JAVADOC_FIELD_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.JAVADOC_METHOD_REF :
				buffer.append("JAVADOC_METHOD_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.JAVADOC_TYPE_REF :
				buffer.append("JAVADOC_TYPE_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.JAVADOC_PARAM_REF :
				buffer.append("JAVADOC_PARAM_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.JAVADOC_VALUE_REF :
				buffer.append("JAVADOC_VALUE_REF"); //$NON-NLS-1$
				break;
			case CompletionProposal.FIELD_IMPORT :
				buffer.append("FIELD_IMPORT"); //$NON-NLS-1$
				break;
			case CompletionProposal.METHOD_IMPORT :
				buffer.append("METHOD_IMPORT"); //$NON-NLS-1$
				break;
			case CompletionProposal.TYPE_IMPORT :
				buffer.append("TYPE_IMPORT"); //$NON-NLS-1$
				break;
			case CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER :
				buffer.append("METHOD_REF_WITH_CASTED_RECEIVER"); //$NON-NLS-1$
				break;
			case CompletionProposal.FIELD_REF_WITH_CASTED_RECEIVER :
				buffer.append("FIELD_REF_WITH_CASTED_RECEIVER"); //$NON-NLS-1$
				break;
			case CompletionProposal.CONSTRUCTOR_INVOCATION :
				buffer.append("CONSTRUCTOR_INVOCATION"); //$NON-NLS-1$
				break;
			case CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION :
				buffer.append("ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION"); //$NON-NLS-1$
				break;
			case CompletionProposal.LAMBDA_EXPRESSION :
				buffer.append("LAMBDA_EXPRESSION"); //$NON-NLS-1$
				break;
			default :
				buffer.append("PROPOSAL"); //$NON-NLS-1$
				break;

		}
		buffer.append("]{completion:"); //$NON-NLS-1$
		if (this.completion != null) buffer.append(this.completion);
		buffer.append(", declSign:"); //$NON-NLS-1$
		if (this.declarationSignature != null) buffer.append(this.declarationSignature);
		buffer.append(", sign:"); //$NON-NLS-1$
		if (this.signature != null) buffer.append(this.signature);
		buffer.append(", declKey:"); //$NON-NLS-1$
		if (this.declarationKey != null) buffer.append(this.declarationKey);
		buffer.append(", key:"); //$NON-NLS-1$
		if (this.key != null) buffer.append(this.key);
		buffer.append(", name:"); //$NON-NLS-1$
		if (this.name != null) buffer.append(this.name);
		buffer.append(", replace:["); //$NON-NLS-1$
		buffer.append(this.replaceStart);
		buffer.append(',');
		buffer.append(this.replaceEnd);
		buffer.append("], token:["); //$NON-NLS-1$
		buffer.append(this.tokenStart);
		buffer.append(',');
		buffer.append(this.tokenEnd);
		buffer.append("], relevance:"); //$NON-NLS-1$
		buffer.append(this.relevance);
		buffer.append('}');
		return buffer.toString();
	}

	@Override
	public boolean canUseDiamond(CompletionContext coreContext) {
		if (this.getKind() != CONSTRUCTOR_INVOCATION) return false;
		if (coreContext instanceof InternalCompletionContext) {
			InternalCompletionContext internalCompletionContext = (InternalCompletionContext) coreContext;
			if (internalCompletionContext.extendedContext == null) return false;
			char[] name1 = this.declarationPackageName;
			char[] name2 = this.declarationTypeName;
			char[] declarationType = CharOperation.concat(name1, name2, '.');  // fully qualified name
			// even if the type arguments used in the method have been substituted,
			// extract the original type arguments only, since thats what we want to compare with the class
			// type variables (Substitution might have happened when the constructor is coming from another
			// CU and not the current one).
			char[] sign = (this.originalSignature != null)? this.originalSignature : getSignature();
			if (!(sign == null || sign.length < 2)) {
				sign = Signature.removeCapture(sign);
			}
			char[][] types= Signature.getParameterTypes(sign);
			String[] paramTypeNames= new String[types.length];
			for (int i= 0; i < types.length; i++) {
				paramTypeNames[i]= new String(Signature.toCharArray(types[i]));
			}
			return internalCompletionContext.extendedContext.canUseDiamond(paramTypeNames,declarationType);
		}
		else {
			return false;
		}
	}
}
