/*******************************************************************************
 * Copyright (c) 2013, 2015 GK Software AG.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CharOperation;

/**
 * Implementation of 18.1.1 in JLS8
 */
public class InferenceVariable extends TypeVariableBinding {

	/** Structured key for interning. */
	static class InferenceVarKey {
		/*@NonNull*/ TypeBinding typeParameter;
		long position;
		int rank;
		InferenceVarKey(TypeBinding typeParameter, InvocationSite site, int rank) {
			this.typeParameter = typeParameter;
			this.position = ((long) site.sourceStart() << 32) + site.sourceEnd();
			this.rank = rank;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (this.position ^ (this.position >>> 32));
			result = prime * result + this.rank;
			result = prime * result + this.typeParameter.id;
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof InferenceVarKey))
				return false;
			InferenceVarKey other = (InferenceVarKey) obj;
			if (this.position != other.position)
				return false;
			if (this.rank != other.rank)
				return false;
			if (TypeBinding.notEquals(this.typeParameter, other.typeParameter))
				return false;
			return true;
		}
	}

	/**
	 * Create or retrieve the inference variable representing the given typeParameter.
	 * Inference variables are interned to avoid duplication due to lambda copying.
	 */
	public static InferenceVariable get(TypeBinding typeParameter, int rank, InvocationSite site, Scope scope, ReferenceBinding object, boolean initial) {
		Map<InferenceVarKey, InferenceVariable> uniqueInferenceVariables = scope.compilationUnitScope().uniqueInferenceVariables;
		InferenceVariable var = null;
		InferenceVarKey key = null;
		if (site != null && typeParameter != null) {
			key = new InferenceVarKey(typeParameter, site, rank);
			var = uniqueInferenceVariables.get(key);
		}
		if (var == null) {
			int newVarId = uniqueInferenceVariables.size();
			var = new InferenceVariable(typeParameter, rank, newVarId, site, scope.environment(), object, initial);
			if (key != null)
				uniqueInferenceVariables.put(key, var);
		}
		return var;
	}


	InvocationSite site;
	TypeBinding typeParameter;
	long nullHints; // one of TagBits.{AnnotationNonNull,AnnotationNullable} may steer inference into inferring nullness as well; set both bits to request avoidance.
	private InferenceVariable prototype;
	int varId; // this is used for constructing a source name like T#0.
	public boolean isFromInitialSubstitution; 	// further ivars created during 18.5.2 (for capture bounds) set this to false
												// to mark that they don't participate in any theta substitution

	private InferenceVariable(TypeBinding typeParameter, int parameterRank, int iVarId, InvocationSite site, LookupEnvironment environment, ReferenceBinding object, boolean initial) {
		this(typeParameter, parameterRank, site, makeName(typeParameter, iVarId), environment, object);
		this.varId = iVarId;
		this.isFromInitialSubstitution = initial;
	}
	private static char[] makeName(TypeBinding typeParameter, int iVarId) {
		if (typeParameter.getClass() == TypeVariableBinding.class) {
			return CharOperation.concat(typeParameter.shortReadableName(), Integer.toString(iVarId).toCharArray(), '#');
		}
		return CharOperation.concat(
					CharOperation.concat('(', typeParameter.shortReadableName(), ')'),
					Integer.toString(iVarId).toCharArray(), '#');
	}
	private InferenceVariable(TypeBinding typeParameter, int parameterRank, InvocationSite site, char[] sourceName, LookupEnvironment environment, ReferenceBinding object) {
		super(sourceName, null/*declaringElement*/, parameterRank, environment);
		this.site = site;
		this.typeParameter = typeParameter;
		this.tagBits |= typeParameter.tagBits & TagBits.AnnotationNullMASK;
		if (typeParameter.isTypeVariable()) {
			TypeVariableBinding typeVariable = (TypeVariableBinding) typeParameter;
			if (typeVariable.firstBound != null) {
				long boundBits = typeVariable.firstBound.tagBits & TagBits.AnnotationNullMASK;
				if (boundBits == TagBits.AnnotationNonNull)
					this.tagBits |= boundBits; // @NonNull must be preserved
				else
					this.nullHints |= boundBits; // @Nullable is only a hint
			}
		}
		this.superclass = object;
		this.prototype = this;
	}

	@Override
	public TypeBinding clone(TypeBinding enclosingType) {
		InferenceVariable clone = new InferenceVariable(this.typeParameter, this.rank, this.site, this.sourceName, this.environment, this.superclass);
		clone.tagBits = this.tagBits;
		clone.nullHints = this.nullHints;
		clone.varId = this.varId;
		clone.isFromInitialSubstitution = this.isFromInitialSubstitution;
		clone.prototype = this;
		return clone;
	}

	@Override
	public InferenceVariable prototype() {
		return this.prototype;
	}

	@Override
	public char[] constantPoolName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PackageBinding getPackage() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCompatibleWith(TypeBinding right, Scope scope) {
		// if inference variables are ever checked for compatibility
		// (like during inner resolve of a ReferenceExpression during inference)
		// treat it as a wildcard, compatible with any any and every type.
		return true;
	}

	@Override
	public boolean isProperType(boolean admitCapture18) {
		return false;
	}

	@Override
	TypeBinding substituteInferenceVariable(InferenceVariable var, TypeBinding substituteType) {
		if (TypeBinding.equalsEquals(this, var))
			return substituteType;
		return this;
	}

	@Override
	void collectInferenceVariables(Set<InferenceVariable> variables) {
		variables.add(this);
	}

	@Override
	public ReferenceBinding[] superInterfaces() {
		return Binding.NO_SUPERINTERFACES;
	}

	@Override
	public char[] qualifiedSourceName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public char[] sourceName() {
		return this.sourceName;
	}

	@Override
	public char[] readableName() {
		return this.sourceName;
	}

	@Override
	public boolean hasTypeBit(int bit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String debugName() {
		return String.valueOf(this.sourceName);
	}

	@Override
	public String toString() {
		return debugName();
	}

	@Override
	public int hashCode() {
		int code = this.typeParameter.hashCode() + 17 * this.rank;
		if (this.site != null) {
			code = 31 * code + this.site.sourceStart();
			code = 31 * code + this.site.sourceEnd();
		}
		return code;
	}
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof InferenceVariable))
			return false;
		InferenceVariable other = (InferenceVariable) obj;
		return this.rank == other.rank
				&& InferenceContext18.isSameSite(this.site, other.site)
				&& TypeBinding.equalsEquals(this.typeParameter, other.typeParameter);
	}

	@Override
	public TypeBinding erasure() {
		// lazily initialize field that may be required in super.erasure():
		if (this.superclass == null)
			this.superclass = this.environment.getType(TypeConstants.JAVA_LANG_OBJECT);
		return super.erasure();
	}
}
