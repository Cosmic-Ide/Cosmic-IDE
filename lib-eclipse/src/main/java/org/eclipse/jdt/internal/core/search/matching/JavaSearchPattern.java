/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.search.matching;

import org.eclipse.jdt.core.BindingKey;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IParallelizable;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.core.search.indexing.IIndexConstants;
import org.eclipse.jdt.internal.core.util.Util;


public class JavaSearchPattern extends SearchPattern implements IIndexConstants, IParallelizable, Cloneable {

	/*
	 * Whether this pattern is case sensitive.
	 */
	boolean isCaseSensitive;
	/*
	 * Whether this pattern is camel case.
	 */
	boolean isCamelCase;

	/**
	 * One of following pattern value:
	 * <ul>
	 * 	<li>{@link #R_EXACT_MATCH}</li>
	 *		<li>{@link #R_PREFIX_MATCH}</li>
	 *		<li>{@link #R_PATTERN_MATCH}</li>
	 *		<li>{@link #R_REGEXP_MATCH}</li>
	 *		<li>{@link #R_CAMELCASE_MATCH}</li>
	 *		<li>{@link #R_CAMELCASE_SAME_PART_COUNT_MATCH}</li>
	 * </ul>
	 */
	int matchMode;

	/**
	 * One of {@link #R_ERASURE_MATCH}, {@link #R_EQUIVALENT_MATCH}, {@link #R_FULL_MATCH}.
	 */
	int matchCompatibility;

	/**
	 * Fine grain limitation
	 */
	public int fineGrain = 0;

	/**
	 * Mask used on match rule for match mode.
	 */
	public static final int MATCH_MODE_MASK = R_EXACT_MATCH
		| R_PREFIX_MATCH
		| R_PATTERN_MATCH
		| R_REGEXP_MATCH
		| R_CAMELCASE_MATCH
		| R_CAMELCASE_SAME_PART_COUNT_MATCH;

	/**
	 * Mask used on match rule for generic relevance.
	 */
	public static final int MATCH_COMPATIBILITY_MASK = R_ERASURE_MATCH | R_EQUIVALENT_MATCH | R_FULL_MATCH;

	// Signatures and arguments for parameterized types search
	char[][] typeSignatures;
	private char[][][] typeArguments;
	private int flags = 0;
	static final int HAS_TYPE_ARGUMENTS = 1;

	protected JavaSearchPattern(int patternKind, int matchRule) {
		super(matchRule);
		this.kind = patternKind;
		// Use getMatchRule() instead of matchRule as super constructor may modify its value
		// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=81377
		int rule = getMatchRule();
		this.isCaseSensitive = (rule & R_CASE_SENSITIVE) != 0;
		this.isCamelCase = (rule & (R_CAMELCASE_MATCH | R_CAMELCASE_SAME_PART_COUNT_MATCH)) != 0;
		this.matchCompatibility = rule & MATCH_COMPATIBILITY_MASK;
		this.matchMode = rule & MATCH_MODE_MASK;
	}

	/**
	 * @param fineGrain
	 */
	public static String getFineGrainFlagString(final int fineGrain) {
		if (fineGrain == 0) {
			return "none"; //$NON-NLS-1$
		}
		StringBuilder buffer = new StringBuilder();
		for (int i=1; i<=32; i++) {
			int bit = fineGrain & (1<<(i-1));
			if (bit != 0 && buffer.length()>0) buffer.append(" | "); //$NON-NLS-1$
			switch (bit) {
				case IJavaSearchConstants.FIELD_DECLARATION_TYPE_REFERENCE:
					buffer.append("FIELD_DECLARATION_TYPE_REFERENCE"); //$NON-NLS-1$
					break;
				case IJavaSearchConstants.LOCAL_VARIABLE_DECLARATION_TYPE_REFERENCE:
					buffer.append("LOCAL_VARIABLE_DECLARATION_TYPE_REFERENCE"); //$NON-NLS-1$
					break;
				case IJavaSearchConstants.PARAMETER_DECLARATION_TYPE_REFERENCE:
					buffer.append("PARAMETER_DECLARATION_TYPE_REFERENCE"); //$NON-NLS-1$
					break;
				case IJavaSearchConstants.SUPERTYPE_TYPE_REFERENCE:
					buffer.append("SUPERTYPE_TYPE_REFERENCE"); //$NON-NLS-1$
					break;
				case IJavaSearchConstants.THROWS_CLAUSE_TYPE_REFERENCE:
					buffer.append("THROWS_CLAUSE_TYPE_REFERENCE"); //$NON-NLS-1$
					break;
				case IJavaSearchConstants.CAST_TYPE_REFERENCE:
					buffer.append("CAST_TYPE_REFERENCE"); //$NON-NLS-1$
					break;
				case IJavaSearchConstants.CATCH_TYPE_REFERENCE:
					buffer.append("CATCH_TYPE_REFERENCE"); //$NON-NLS-1$
					break;
				case IJavaSearchConstants.CLASS_INSTANCE_CREATION_TYPE_REFERENCE:
					buffer.append("CLASS_INSTANCE_CREATION_TYPE_REFERENCE"); //$NON-NLS-1$
					break;
				case IJavaSearchConstants.RETURN_TYPE_REFERENCE:
					buffer.append("RETURN_TYPE_REFERENCE"); //$NON-NLS-1$
					break;
				case IJavaSearchConstants.IMPORT_DECLARATION_TYPE_REFERENCE:
					buffer.append("IMPORT_DECLARATION_TYPE_REFERENCE"); //$NON-NLS-1$
					break;
				case IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE:
					buffer.append("ANNOTATION_TYPE_REFERENCE"); //$NON-NLS-1$
					break;
				case IJavaSearchConstants.TYPE_ARGUMENT_TYPE_REFERENCE:
					buffer.append("TYPE_ARGUMENT_TYPE_REFERENCE"); //$NON-NLS-1$
					break;
				case IJavaSearchConstants.TYPE_VARIABLE_BOUND_TYPE_REFERENCE:
					buffer.append("TYPE_VARIABLE_BOUND_TYPE_REFERENCE"); //$NON-NLS-1$
					break;
				case IJavaSearchConstants.WILDCARD_BOUND_TYPE_REFERENCE:
					buffer.append("WILDCARD_BOUND_TYPE_REFERENCE"); //$NON-NLS-1$
					break;
				case IJavaSearchConstants.SUPER_REFERENCE:
					buffer.append("SUPER_REFERENCE"); //$NON-NLS-1$
					break;
				case IJavaSearchConstants.QUALIFIED_REFERENCE:
					buffer.append("QUALIFIED_REFERENCE"); //$NON-NLS-1$
					break;
				case IJavaSearchConstants.THIS_REFERENCE:
					buffer.append("THIS_REFERENCE"); //$NON-NLS-1$
					break;
				case IJavaSearchConstants.IMPLICIT_THIS_REFERENCE:
					buffer.append("IMPLICIT_THIS_REFERENCE"); //$NON-NLS-1$
					break;
				case IJavaSearchConstants.METHOD_REFERENCE_EXPRESSION:
					buffer.append("METHOD_REFERENCE_EXPRESSION"); //$NON-NLS-1$
					break;
				case IJavaSearchConstants.PERMITTYPE_TYPE_REFERENCE:
					buffer.append("PERMITTYPE_TYPE_REFERENCE"); //$NON-NLS-1$
					break;
			}
		}
		return buffer.toString();
	}

	@Override
	public SearchPattern getBlankPattern() {
		return null;
	}

	final int getMatchMode() {
		return this.matchMode;
	}

	final boolean isCamelCase() {
		return this.isCamelCase;
	}

	final boolean isCaseSensitive () {
		return this.isCaseSensitive;
	}

	final boolean isErasureMatch() {
		return (this.matchCompatibility & R_ERASURE_MATCH) != 0;
	}

	final boolean isEquivalentMatch() {
		return (this.matchCompatibility & R_EQUIVALENT_MATCH) != 0;
	}

	/*
	 * Extract method arguments using unique key for parameterized methods
	 * and type parameters for non-generic ones.
	 */
	char[][] extractMethodArguments(IMethod method) {

		// Use bind key if the element is resolved
		if (method.isResolved()) {
			BindingKey bindingKey = new BindingKey(method.getKey());
			if (bindingKey.isParameterizedMethod()) {
				String[] argumentsSignatures = bindingKey.getTypeArguments();
				int length = argumentsSignatures.length;
				if (length > 0) {
					char[][] methodArguments = new char[length][];
					for (int i=0; i<length; i++) {
						methodArguments[i] = argumentsSignatures[i].toCharArray();
						CharOperation.replace(methodArguments[i], new char[] { '$', '/' }, '.');
					}
					return methodArguments;
				}
			}
			return null;
		}

		// Try to get the argument using the JavaModel info
		try {
			ITypeParameter[] parameters = method.getTypeParameters();
			if (parameters != null) {
				int length = parameters.length;
				if (length > 0) {
					char[][] arguments = new char[length][];
					for (int i=0; i<length; i++) {
						arguments[i] = Signature.createTypeSignature(parameters[i].getElementName(), false).toCharArray();
					}
					return arguments;
				}
			}
		}
		catch (JavaModelException jme) {
			// do nothing
		}
		return null;
	}

	/**
	 * @return Returns the typeArguments.
	 */
	final char[][][] getTypeArguments() {
		return this.typeArguments;
	}

	/**
	 * Returns whether the pattern has signatures or not.
	 * If pattern {@link #typeArguments} field, this field shows that it was built
	 * on a generic source type.
	 * @return true if {@link #typeSignatures} field is not null and has a length greater than 0.
	 */
	public final boolean hasSignatures() {
		return this.typeSignatures != null && this.typeSignatures.length > 0;
	}

	/**
	 * Returns whether the pattern includes type arguments information or not.
	 * @return default is false
	 */
	public final boolean hasTypeArguments() {
		return (this.flags & HAS_TYPE_ARGUMENTS) != 0;
	}

	/**
	 * Returns whether the pattern includes type parameters information or not.
	 * @return true if {@link #typeArguments} contains type parameters instead
	 * 	type arguments signatures.
	 */
	public final boolean hasTypeParameters() {
		return !hasSignatures() && hasTypeArguments();
	}

	/**
	 * Return whether two suffixes are compatible.
	 *
	 * Note that obvious compatibility values as equals and {@link IIndexConstants#TYPE_SUFFIX}
	 * has to be tested by caller to avoid unnecessary method call...
	 *
	 * @param typeSuffix
	 * @param patternSuffix
	 * @return true if suffixes are compatible, false otherwise
	 */
	boolean matchDifferentTypeSuffixes(int typeSuffix, int patternSuffix) {
		switch(typeSuffix) {
			case CLASS_SUFFIX :
				switch (patternSuffix) {
					case CLASS_AND_INTERFACE_SUFFIX :
					case CLASS_AND_ENUM_SUFFIX :
						return true;
				}
				return false;

			case INTERFACE_SUFFIX :
				switch (patternSuffix) {
					case CLASS_AND_INTERFACE_SUFFIX :
					case INTERFACE_AND_ANNOTATION_SUFFIX:
						return true;
				}
				return false;

			case ENUM_SUFFIX :
				return patternSuffix == CLASS_AND_ENUM_SUFFIX;

			case ANNOTATION_TYPE_SUFFIX :
				return patternSuffix == INTERFACE_AND_ANNOTATION_SUFFIX;

			case CLASS_AND_INTERFACE_SUFFIX :
				switch (patternSuffix) {
					case CLASS_SUFFIX :
					case INTERFACE_SUFFIX :
						return true;
				}
				return false;

			case CLASS_AND_ENUM_SUFFIX :
				switch (patternSuffix) {
					case CLASS_SUFFIX :
					case ENUM_SUFFIX :
						return true;
				}
				return false;

			case INTERFACE_AND_ANNOTATION_SUFFIX :
				switch (patternSuffix) {
					case INTERFACE_SUFFIX :
					case ANNOTATION_TYPE_SUFFIX :
						return true;
				}
				return false;
		}

		// Default behavior is to match suffixes
		return true;
	}

	protected StringBuffer print(StringBuffer output) {
		output.append(", "); //$NON-NLS-1$
		if (hasTypeArguments() && hasSignatures()) {
			output.append("signature:\""); //$NON-NLS-1$
			output.append(this.typeSignatures[0]);
			output.append("\", "); //$NON-NLS-1$
		}
		switch(getMatchMode()) {
			case R_EXACT_MATCH :
				output.append("exact match, "); //$NON-NLS-1$
				break;
			case R_PREFIX_MATCH :
				output.append("prefix match, "); //$NON-NLS-1$
				break;
			case R_PATTERN_MATCH :
				output.append("pattern match, "); //$NON-NLS-1$
				break;
			case R_REGEXP_MATCH :
				output.append("regexp match, "); //$NON-NLS-1$
				break;
			case R_CAMELCASE_MATCH :
				output.append("camel case match, "); //$NON-NLS-1$
				break;
			case R_CAMELCASE_SAME_PART_COUNT_MATCH:
				output.append("camel case same part count match, "); //$NON-NLS-1$
				break;
		}
		if (isCaseSensitive())
			output.append("case sensitive, "); //$NON-NLS-1$
		else
			output.append("case insensitive, "); //$NON-NLS-1$
		if ((this.matchCompatibility & R_FULL_MATCH) != 0) {
			output.append("generic full match, "); //$NON-NLS-1$
		}
		if ((this.matchCompatibility & R_ERASURE_MATCH) != 0) {
			output.append("generic erasure match, "); //$NON-NLS-1$
		}
		if ((this.matchCompatibility & R_EQUIVALENT_MATCH) != 0) {
			output.append("generic equivalent match, "); //$NON-NLS-1$
		}
		output.append("fine grain: "); //$NON-NLS-1$
		output.append(getFineGrainFlagString(this.fineGrain));
		return output;
	}

	/**
	 * @param typeArguments The typeArguments to set.
	 */
	final void setTypeArguments(char[][][] typeArguments) {
		this.typeArguments = typeArguments;
		// update flags
		if (this.typeArguments != null) {
			int length = this.typeArguments.length;
			for (int i=0; i<length; i++) {
				if (this.typeArguments[i] != null && this.typeArguments[i].length > 0) {
					this.flags |= HAS_TYPE_ARGUMENTS;
					break;
				}
			}
		}
	}

	/*
	 * Extract and store type signatures and arguments using unique key for parameterized types
	 * and type parameters for non-generic ones
	 */
	void storeTypeSignaturesAndArguments(IType type) {
		if (type.isResolved()) {
			BindingKey bindingKey = new BindingKey(type.getKey());
			if (bindingKey.isParameterizedType() || bindingKey.isRawType()) {
				String signature = bindingKey.toSignature();
				this.typeSignatures = Util.splitTypeLevelsSignature(signature);
				setTypeArguments(Util.getAllTypeArguments(this.typeSignatures));
			}
			return;
		}

		// Scan hierarchy to store type arguments at each level
		char[][][] typeParameters = new char[10][][];
		int ptr = -1;
		boolean hasParameters = false;
		try {
			IJavaElement parent = type;
			ITypeParameter[] parameters = null;
			while (parent != null && parent.getElementType() == IJavaElement.TYPE) {
				if (++ptr > typeParameters.length) {
					System.arraycopy(typeParameters, 0, typeParameters = new char[typeParameters.length+10][][], 0, ptr);
				}
				IType parentType = (IType) parent;
				parameters = parentType.getTypeParameters();
				if (parameters !=null) {
					int length = parameters.length;
					if (length > 0) {
						hasParameters = true;
						typeParameters[ptr] = new char[length][];
						for (int i=0; i<length; i++)
							typeParameters[ptr][i] = Signature.createTypeSignature(parameters[i].getElementName(), false).toCharArray();
					}
				}
				parent = parent.getParent();
			}
		}
		catch (JavaModelException jme) {
			return;
		}

		// Store type arguments if any
		if (hasParameters) {
			if (++ptr < typeParameters.length)
				System.arraycopy(typeParameters, 0, typeParameters = new char[ptr][][], 0, ptr);
			setTypeArguments(typeParameters);
		}
	}
	@Override
	public final String toString() {
		return print(new StringBuffer(30)).toString();
	}

	@Override
	public boolean isParallelSearchSupported() {
		return true;
	}

}
