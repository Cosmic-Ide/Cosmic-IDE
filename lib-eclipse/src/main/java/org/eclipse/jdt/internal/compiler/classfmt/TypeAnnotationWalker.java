/*******************************************************************************
 * Copyright (c) 2013, 2017 GK Software AG.
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
package org.eclipse.jdt.internal.compiler.classfmt;

import org.eclipse.jdt.internal.compiler.codegen.AnnotationTargetTypeConstants;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryTypeAnnotation;
import org.eclipse.jdt.internal.compiler.env.ITypeAnnotationWalker;

/** Type annotation walker implementation based an actual annotations decoded from a .class file. */
public class TypeAnnotationWalker implements ITypeAnnotationWalker {

	final protected IBinaryTypeAnnotation[] typeAnnotations;	// the actual material we're managing here
	final protected long matches;								// bit mask of indices into typeAnnotations, 1 means active, 0 is filtered during the walk
	final protected int pathPtr;								// pointer into the typePath

	// precondition: not-empty typeAnnotations
	public TypeAnnotationWalker(IBinaryTypeAnnotation[] typeAnnotations) {
		this(typeAnnotations, -1L >>> (64-typeAnnotations.length)); // initialize so lowest length bits are 1
	}
	TypeAnnotationWalker(IBinaryTypeAnnotation[] typeAnnotations, long matchBits) {
		this(typeAnnotations, matchBits, 0);
	}
	protected TypeAnnotationWalker(IBinaryTypeAnnotation[] typeAnnotations, long matchBits, int pathPtr) {
		this.typeAnnotations = typeAnnotations;
		this.matches = matchBits;
		this.pathPtr = pathPtr;
	}

	protected ITypeAnnotationWalker restrict(long newMatches, int newPathPtr) {
		if (this.matches == newMatches && this.pathPtr == newPathPtr) return this;
		if (newMatches == 0 || this.typeAnnotations == null || this.typeAnnotations.length == 0)
			return EMPTY_ANNOTATION_WALKER;
		return new TypeAnnotationWalker(this.typeAnnotations, newMatches, newPathPtr);
	}

	// ==== filter by top-level targetType: ====

	@Override
	public ITypeAnnotationWalker toField() {
		return toTarget(AnnotationTargetTypeConstants.FIELD);
	}

	@Override
	public ITypeAnnotationWalker toMethodReturn() {
		return toTarget(AnnotationTargetTypeConstants.METHOD_RETURN);
	}

	@Override
	public ITypeAnnotationWalker toReceiver() {
		return toTarget(AnnotationTargetTypeConstants.METHOD_RECEIVER);
	}

	/*
	 * Implementation for walking to methodReturn, receiver type or field.
	 */
	protected ITypeAnnotationWalker toTarget(int targetType) {
		long newMatches = this.matches;
		if (newMatches == 0)
			return EMPTY_ANNOTATION_WALKER;
		int length = this.typeAnnotations.length;
		long mask = 1;
		for (int i = 0; i < length; i++, mask = mask << 1) {
			if (this.typeAnnotations[i].getTargetType() != targetType)
				newMatches &= ~mask;
		}
		return restrict(newMatches, 0);
	}

	@Override
	public ITypeAnnotationWalker toTypeParameter(boolean isClassTypeParameter, int rank) {
		long newMatches = this.matches;
		if (newMatches == 0)
			return EMPTY_ANNOTATION_WALKER;
		int targetType = isClassTypeParameter ? AnnotationTargetTypeConstants.CLASS_TYPE_PARAMETER : AnnotationTargetTypeConstants.METHOD_TYPE_PARAMETER;
		int length = this.typeAnnotations.length;
		long mask = 1;
		for (int i = 0; i < length; i++, mask = mask << 1) {
			IBinaryTypeAnnotation candidate = this.typeAnnotations[i];
			if (candidate.getTargetType() != targetType || candidate.getTypeParameterIndex() != rank) {
				newMatches &= ~mask;
			}
		}
		return restrict(newMatches, 0);
	}

	@Override
	public ITypeAnnotationWalker toTypeParameterBounds(boolean isClassTypeParameter, int parameterRank) {
		long newMatches = this.matches;
		if (newMatches == 0)
			return EMPTY_ANNOTATION_WALKER;
		int length = this.typeAnnotations.length;
		int targetType = isClassTypeParameter ?
				AnnotationTargetTypeConstants.CLASS_TYPE_PARAMETER_BOUND : AnnotationTargetTypeConstants.METHOD_TYPE_PARAMETER_BOUND;
		long mask = 1;
		for (int i = 0; i < length; i++, mask = mask << 1) {
			IBinaryTypeAnnotation candidate = this.typeAnnotations[i];
			if (candidate.getTargetType() != targetType || (short)candidate.getTypeParameterIndex() != parameterRank) {
				newMatches &= ~mask;
			}
		}
		return restrict(newMatches, 0);
	}

	@Override
	public ITypeAnnotationWalker toTypeBound(short boundIndex) {
		long newMatches = this.matches;
		if (newMatches == 0)
			return EMPTY_ANNOTATION_WALKER;
		int length = this.typeAnnotations.length;
		long mask = 1;
		for (int i = 0; i < length; i++, mask = mask << 1) {
			IBinaryTypeAnnotation candidate = this.typeAnnotations[i];
			if ((short)candidate.getBoundIndex() != boundIndex) {
				newMatches &= ~mask;
			}
		}
		return restrict(newMatches, 0);
	}


	/**
	 * {@inheritDoc}
	 * <p>(superTypesSignature is ignored in this implementation).</p>
	 */
	@Override
	public ITypeAnnotationWalker toSupertype(short index, char[] superTypeSignature) {
		long newMatches = this.matches;
		if (newMatches == 0)
			return EMPTY_ANNOTATION_WALKER;
		int length = this.typeAnnotations.length;
		long mask = 1;
		for (int i = 0; i < length; i++, mask = mask << 1) {
			IBinaryTypeAnnotation candidate = this.typeAnnotations[i];
			if (candidate.getTargetType() != AnnotationTargetTypeConstants.CLASS_EXTENDS || (short)candidate.getSupertypeIndex() != index) {
				newMatches &= ~mask;
			}
		}
		return restrict(newMatches, 0);
	}

	@Override
	public ITypeAnnotationWalker toMethodParameter(short index) {
		long newMatches = this.matches;
		if (newMatches == 0)
			return EMPTY_ANNOTATION_WALKER;
		int length = this.typeAnnotations.length;
		long mask = 1;
		for (int i = 0; i < length; i++, mask = mask << 1) {
			IBinaryTypeAnnotation candidate = this.typeAnnotations[i];
			if (candidate.getTargetType() != AnnotationTargetTypeConstants.METHOD_FORMAL_PARAMETER || (short)candidate.getMethodFormalParameterIndex() != index) {
				newMatches &= ~mask;
			}
		}
		return restrict(newMatches, 0);
	}

	@Override
	public ITypeAnnotationWalker toThrows(int index) {
		long newMatches = this.matches;
		if (newMatches == 0)
			return EMPTY_ANNOTATION_WALKER;
		int length = this.typeAnnotations.length;
		long mask = 1;
		for (int i = 0; i < length; i++, mask = mask << 1) {
			IBinaryTypeAnnotation candidate = this.typeAnnotations[i];
			if (candidate.getTargetType() != AnnotationTargetTypeConstants.THROWS || candidate.getThrowsTypeIndex() != index) {
				newMatches &= ~mask;
			}
		}
		return restrict(newMatches, 0);
	}

	// ==== descending into details: ====

	@Override
	public ITypeAnnotationWalker toTypeArgument(int rank) {
		// like toNextDetail() but also checking byte 2 against rank
		long newMatches = this.matches;
		if (newMatches == 0)
			return EMPTY_ANNOTATION_WALKER;
		int length = this.typeAnnotations.length;
		long mask = 1;
		for (int i = 0; i < length; i++, mask = mask << 1) {
			IBinaryTypeAnnotation candidate = this.typeAnnotations[i];
			int[] path = candidate.getTypePath();
			if (this.pathPtr >= path.length
					|| path[this.pathPtr] != AnnotationTargetTypeConstants.TYPE_ARGUMENT
					|| path[this.pathPtr+1] != rank) {
				newMatches &= ~mask;
			}
		}
		return restrict(newMatches, this.pathPtr+2);
	}

	@Override
	public ITypeAnnotationWalker toWildcardBound() {
		long newMatches = this.matches;
		if (newMatches == 0)
			return EMPTY_ANNOTATION_WALKER;
		int length = this.typeAnnotations.length;
		long mask = 1;
		for (int i = 0; i < length; i++, mask = mask << 1) {
			IBinaryTypeAnnotation candidate = this.typeAnnotations[i];
			int[] path = candidate.getTypePath();
			if (this.pathPtr >= path.length
					|| path[this.pathPtr] != AnnotationTargetTypeConstants.WILDCARD_BOUND) {
				newMatches &= ~mask;
			}
		}
		return restrict(newMatches, this.pathPtr+2);
	}

	@Override
	public ITypeAnnotationWalker toNextArrayDimension() {
		return toNextDetail(AnnotationTargetTypeConstants.NEXT_ARRAY_DIMENSION);
	}

	@Override
	public ITypeAnnotationWalker toNextNestedType() {
		return toNextDetail(AnnotationTargetTypeConstants.NEXT_NESTED_TYPE);
	}

	/*
	 * Implementation for walking along the type_path for array dimensions & nested types.
	 */
	protected ITypeAnnotationWalker toNextDetail(int detailKind) {
		long newMatches = this.matches;
		if (newMatches == 0)
			return restrict(newMatches, this.pathPtr+2);
		int length = this.typeAnnotations.length;
		long mask = 1;
		for (int i = 0; i < length; i++, mask = mask << 1) {
			IBinaryTypeAnnotation candidate = this.typeAnnotations[i];
			int[] path = candidate.getTypePath();
			if (this.pathPtr >= path.length || path[this.pathPtr] != detailKind) {
				newMatches &= ~mask;
			}
		}
		return restrict(newMatches, this.pathPtr+2);
	}

	// ==== leaves: the actual annotations: ====

	@Override
	public IBinaryAnnotation[] getAnnotationsAtCursor(int currentTypeId, boolean mayApplyArrayContentsDefaultNullness) {
		int length = this.typeAnnotations.length;
		IBinaryAnnotation[] filtered = new IBinaryAnnotation[length];
		long ptr = 1;
		int count = 0;
		for (int i = 0; i < length; i++, ptr<<=1) {
			if ((this.matches & ptr) == 0)
				continue;
			IBinaryTypeAnnotation candidate = this.typeAnnotations[i];
			if (candidate.getTypePath().length > this.pathPtr)
				continue;
			filtered[count++] = candidate.getAnnotation();
		}
		if (count == 0)
			return NO_ANNOTATIONS;
		if (count < length)
			System.arraycopy(filtered, 0, filtered = new IBinaryAnnotation[count], 0, count);
		return filtered;
	}
}
