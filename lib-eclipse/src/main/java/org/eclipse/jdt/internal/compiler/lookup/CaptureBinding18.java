/*******************************************************************************
 * Copyright (c) 2013, 2019 GK Software AG.
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

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;

/**
 * Capture-like type variable introduced during 1.8 type inference.
 */
public class CaptureBinding18 extends CaptureBinding {

	TypeBinding[] upperBounds;
	private char[] originalName;
	private CaptureBinding18 prototype;

	public CaptureBinding18(ReferenceBinding contextType, char[] sourceName, char[] originalName, int start, int end, int captureID, LookupEnvironment environment) {
		super(contextType, sourceName, start, end, captureID, environment);
		this.originalName = originalName;
		this.prototype = this;
	}

	private CaptureBinding18(CaptureBinding18 prototype) {
		super(prototype);
		this.sourceName = CharOperation.append(prototype.sourceName, '\'');
		this.originalName = prototype.originalName;
		this.upperBounds = prototype.upperBounds;
		this.prototype = prototype.prototype;
	}

	public boolean setUpperBounds(TypeBinding[] upperBounds, ReferenceBinding javaLangObject) {
		this.upperBounds = upperBounds;
		if (upperBounds.length > 0)
			this.firstBound = upperBounds[0];
		int numReferenceInterfaces = 0;
		if (!isConsistentIntersection(upperBounds))
			return false;
		for (int i = 0; i < upperBounds.length; i++) {
			TypeBinding aBound = upperBounds[i];
			if (aBound instanceof ReferenceBinding) {
				if (this.superclass == null && aBound.isClass())
					this.superclass = (ReferenceBinding) aBound;
				else if (aBound.isInterface())
					numReferenceInterfaces++;
			} else if (TypeBinding.equalsEquals(aBound.leafComponentType(), this)) {
				return false; // cycle detected
			}
		}
		this.superInterfaces = new ReferenceBinding[numReferenceInterfaces];
		int idx = 0;
		for (int i = 0; i < upperBounds.length; i++) {
			TypeBinding aBound = upperBounds[i];
			if (aBound.isInterface())
				this.superInterfaces[idx++] = (ReferenceBinding) aBound;
		}
		if (this.superclass == null)
			this.superclass = javaLangObject;
		return true;
	}

	@Override
	public void initializeBounds(Scope scope, ParameterizedTypeBinding capturedParameterizedType) {
		// nothing to initialize here (and cannot use super methods which requires wildcard to be set).
	}

	@Override
	public TypeBinding clone(TypeBinding enclosingType) {
		return new CaptureBinding18(this);
	}

	@Override
	public MethodBinding[] getMethods(char[] selector) {
		if (this.upperBounds.length == 1 && this.upperBounds[0] instanceof ReferenceBinding)
			return ((ReferenceBinding)this.upperBounds[0]).getMethods(selector);
		return super.getMethods(selector);
	}

	@Override
	public TypeBinding erasure() {
		if (this.upperBounds != null && this.upperBounds.length > 1) {
			ReferenceBinding[] erasures = new ReferenceBinding[this.upperBounds.length];
			boolean multipleErasures = false;
			for (int i = 0; i < this.upperBounds.length; i++) {
				erasures[i] = (ReferenceBinding) this.upperBounds[i].erasure(); // FIXME cast?
				if (i > 0) {
					if (TypeBinding.notEquals(erasures[0], erasures[i]))
						multipleErasures = true;
				}
			}
			if (!multipleErasures)
				return erasures[0];
			return this.environment.createIntersectionType18(erasures);
		}
		if (this.superclass == null)
			return this.environment.getType(TypeConstants.JAVA_LANG_OBJECT);
		return super.erasure();
	}

	/**
	 * @see TypeBinding#isEquivalentTo(TypeBinding)
	 */
	@Override
	public boolean isEquivalentTo(TypeBinding otherType) {
		// from CaptureBinding:
		if (equalsEquals(this, otherType)) return true;
		if (otherType == null) return false;
		if (this.upperBounds != null) {
			// from CaptureBinding:
			for (int i = 0; i < this.upperBounds.length; i++) {
				TypeBinding aBound = this.upperBounds[i];
				// capture of ? extends X[]
				if (aBound != null && aBound.isArrayType()) {
					if (!aBound.isCompatibleWith(otherType))
						return false;
				} else switch (otherType.kind()) {
					case Binding.WILDCARD_TYPE :
					case Binding.INTERSECTION_TYPE :
						if (!((WildcardBinding) otherType).boundCheck(aBound))
							return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean isCompatibleWith(TypeBinding otherType, Scope captureScope) {
		if (TypeBinding.equalsEquals(this, otherType))
			return true;
		if (this.inRecursiveFunction)
			return true;
		this.inRecursiveFunction = true;
		try {
			if (this.upperBounds != null) {
				int length = this.upperBounds.length;

				// need to compare two intersection types? (borrowed from IntersectionType18)
				int rightKind = otherType.kind();
				TypeBinding[] rightIntersectingTypes = null;
				if (rightKind == INTERSECTION_TYPE && otherType.boundKind() == Wildcard.EXTENDS) {
					TypeBinding allRightBounds = ((WildcardBinding) otherType).allBounds();
					if (allRightBounds instanceof IntersectionTypeBinding18)
						rightIntersectingTypes = ((IntersectionTypeBinding18) allRightBounds).intersectingTypes;
				} else if (rightKind == INTERSECTION_TYPE18) {
					rightIntersectingTypes = ((IntersectionTypeBinding18) otherType).intersectingTypes;
				}
				if (rightIntersectingTypes != null) {
					nextRequired:
					for (TypeBinding required : rightIntersectingTypes) {
						for (TypeBinding provided : this.upperBounds) {
							if (provided.isCompatibleWith(required, captureScope))
								continue nextRequired;
						}
						return false;
					}
					return true;
				}

				for (int i = 0; i < length; i++) {
					if (this.upperBounds[i].isCompatibleWith(otherType, captureScope))
						return true;
				}
			}
			return false;
		} finally {
			this.inRecursiveFunction = false;
		}
	}

	@Override
	public TypeBinding findSuperTypeOriginatingFrom(TypeBinding otherType) {
		if (this.upperBounds != null && this.upperBounds.length > 1) {
			for (int i = 0; i < this.upperBounds.length; i++) {
				TypeBinding candidate = this.upperBounds[i].findSuperTypeOriginatingFrom(otherType);
				if (candidate != null)
					return candidate;
				// TODO: maybe we should double check about multiple candidates here,
				// but upper bounds should be consistent so hopefully the first non-null candidate is good enough.
			}
		}
		return super.findSuperTypeOriginatingFrom(otherType);
	}

	@Override
	TypeBinding substituteInferenceVariable(InferenceVariable var, TypeBinding substituteType) {
		if (this.inRecursiveFunction) return this;
		this.inRecursiveFunction = true;
		try {
			boolean haveSubstitution = false;
			ReferenceBinding currentSuperclass = this.superclass;
			if (currentSuperclass != null) {
				currentSuperclass = (ReferenceBinding) currentSuperclass.substituteInferenceVariable(var, substituteType);
				haveSubstitution |= TypeBinding.notEquals(currentSuperclass, this.superclass);
			}
			ReferenceBinding[] currentSuperInterfaces = null;
			if (this.superInterfaces != null) {
				int length = this.superInterfaces.length;
				if (haveSubstitution)
					System.arraycopy(this.superInterfaces, 0, currentSuperInterfaces=new ReferenceBinding[length], 0, length);
				for (int i = 0; i < length; i++) {
					ReferenceBinding currentSuperInterface = this.superInterfaces[i];
					if (currentSuperInterface != null) {
						currentSuperInterface = (ReferenceBinding) currentSuperInterface.substituteInferenceVariable(var, substituteType);
						if (TypeBinding.notEquals(currentSuperInterface, this.superInterfaces[i])) {
							if (currentSuperInterfaces == null)
								System.arraycopy(this.superInterfaces, 0, currentSuperInterfaces=new ReferenceBinding[length], 0, length);
							currentSuperInterfaces[i] = currentSuperInterface;
							haveSubstitution = true;
						}
					}
				}
			}
			TypeBinding[] currentUpperBounds = null;
			if (this.upperBounds != null) {
				int length = this.upperBounds.length;
				if (haveSubstitution)
					System.arraycopy(this.upperBounds, 0, currentUpperBounds=new TypeBinding[length], 0, length);
				for (int i = 0; i < length; i++) {
					TypeBinding currentBound = this.upperBounds[i];
					if (currentBound != null) {
						currentBound = currentBound.substituteInferenceVariable(var, substituteType);
						if (TypeBinding.notEquals(currentBound, this.upperBounds[i])) {
							if (currentUpperBounds == null)
								System.arraycopy(this.upperBounds, 0, currentUpperBounds=new TypeBinding[length], 0, length);
							currentUpperBounds[i] = currentBound;
							haveSubstitution = true;
						}
					}
				}
			}
			TypeBinding currentFirstBound = null;
			if (this.firstBound != null) {
				currentFirstBound = this.firstBound.substituteInferenceVariable(var, substituteType);
				haveSubstitution |= TypeBinding.notEquals(this.firstBound, currentFirstBound);
			}
			if (haveSubstitution) {
				final CaptureBinding18 newCapture = (CaptureBinding18) clone(enclosingType());
				newCapture.tagBits = this.tagBits;
				Substitution substitution = new Substitution() {
					@Override
					public TypeBinding substitute(TypeVariableBinding typeVariable) {
						return  (typeVariable == CaptureBinding18.this) ? newCapture : typeVariable; //$IDENTITY-COMPARISON$
					}
					@Override
					public boolean isRawSubstitution() {
						return false;
					}
					@Override
					public LookupEnvironment environment() {
						return CaptureBinding18.this.environment;
					}
				};
				if (currentFirstBound != null)
					newCapture.firstBound = Scope.substitute(substitution, currentFirstBound);
				newCapture.superclass = (ReferenceBinding) Scope.substitute(substitution, currentSuperclass);
				newCapture.superInterfaces = Scope.substitute(substitution, currentSuperInterfaces);
				newCapture.upperBounds = Scope.substitute(substitution, currentUpperBounds);
				return newCapture;
			}
			return this;
		} finally {
			this.inRecursiveFunction = false;
		}
	}

	@Override
	public boolean isProperType(boolean admitCapture18) {
		if (!admitCapture18)
			return false;
		if (this.inRecursiveFunction)
			return true;
		this.inRecursiveFunction = true;
		try {
			if (this.lowerBound != null && !this.lowerBound.isProperType(admitCapture18))
				return false;
			if (this.upperBounds != null) {
				for (int i = 0; i < this.upperBounds.length; i++) {
					if (!this.upperBounds[i].isProperType(admitCapture18))
						return false;
				}
			}
		} finally {
			this.inRecursiveFunction = false;
		}
		return true;
	}

	int recursionLevel = 0; // used to give a hint at recursive types without going into infinity

	@Override
	public char[] readableName() {
		if (this.lowerBound == null && this.firstBound != null) {
			if (this.prototype.recursionLevel < 2) {
				try {
					this.prototype.recursionLevel ++;
					if (this.upperBounds != null && this.upperBounds.length > 1) {
						StringBuilder sb = new StringBuilder();
						sb.append(this.upperBounds[0].readableName());
						for (int i = 1; i < this.upperBounds.length; i++)
							sb.append('&').append(this.upperBounds[i].readableName());
						int len = sb.length();
						char[] name = new char[len];
						sb.getChars(0, len, name, 0);
						return name;
					}
					return this.firstBound.readableName();
				} finally {
					this.prototype.recursionLevel--;
				}
			} else {
				return this.originalName;
			}
		}
		return super.readableName();
	}

	@Override
	public char[] shortReadableName() {
		if (this.lowerBound == null && this.firstBound != null) {
			if (this.prototype.recursionLevel < 2) {
				try {
					this.prototype.recursionLevel++;
					if (this.upperBounds != null && this.upperBounds.length > 1) {
						StringBuilder sb = new StringBuilder();
						sb.append(this.upperBounds[0].shortReadableName());
						for (int i = 1; i < this.upperBounds.length; i++)
							sb.append('&').append(this.upperBounds[i].shortReadableName());
						int len = sb.length();
						char[] name = new char[len];
						sb.getChars(0, len, name, 0);
						return name;
					}
					return this.firstBound.shortReadableName();
				} finally {
					this.prototype.recursionLevel--;
				}
			} else {
				return this.originalName;
			}
		}
		return super.shortReadableName();
	}

	@Override
	public TypeBinding uncapture(Scope scope) {
		return this;
	}
	@Override
	public char[] computeUniqueKey(boolean isLeaf) {
		StringBuilder buffer = new StringBuilder();
		buffer.append(TypeConstants.CAPTURE18);
		buffer.append('{').append(this.end).append('#').append(this.captureID).append('}');
		buffer.append(';');
		int length = buffer.length();
		char[] uniqueKey = new char[length];
		buffer.getChars(0, length, uniqueKey, 0);
		return uniqueKey;
	}
}
