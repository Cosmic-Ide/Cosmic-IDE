/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
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
 *     Stephan Herrmann - Contribution for
 *								Bug 432977 - [1.8][null] Incorrect 'type is not visible' compiler error
 *								Bug 446434 - [1.8][null] Enable interned captures also when analysing null type annotations
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.util.Util;

/* AnnotatableTypeSystem: Keep track of annotated types so as to provide unique bindings for identically annotated versions identical underlying "naked" types.
   As of now, we ensure uniqueness only for marker annotated types and for others that default to all default attribute values, i.e two instances of @NonNull String
   would have the same binding, while @T(1) X and @T(2) X will not. Binding uniqueness is only a memory optimization and is not essential for correctness of compilation.
   Various subsystems should expect to determine binding identity/equality by calling TypeBinding.equalsEquals and not by using == operator.

   ATS is AnnotatableTypeSystem and not AnnotatedTypeSystem, various methods may actually return unannotated types if the input arguments do not specify any annotations
   and component types of the composite type being constructed are themselves also unannotated. We rely on the master type table maintained by TypeSystem and use
   getDerivedTypes() and cacheDerivedType() to get/put.
*/

public class AnnotatableTypeSystem extends TypeSystem {

	private boolean isAnnotationBasedNullAnalysisEnabled;

	public AnnotatableTypeSystem(LookupEnvironment environment) {
		super(environment);
		this.environment = environment;
		this.isAnnotationBasedNullAnalysisEnabled = environment.globalOptions.isAnnotationBasedNullAnalysisEnabled;
	}

	// Given a type, return all its annotated variants: parameter may be annotated.
	@Override
	public TypeBinding[] getAnnotatedTypes(TypeBinding type) {

		TypeBinding[] derivedTypes = getDerivedTypes(type);
		final int length = derivedTypes.length;
		TypeBinding [] annotatedVersions = new TypeBinding[length];
		int versions = 0;
		for (int i = 0; i < length; i++) {
			final TypeBinding derivedType = derivedTypes[i];
			if (derivedType == null)
				break;
			if (!derivedType.hasTypeAnnotations())
				continue;
			if (derivedType.id == type.id)
				annotatedVersions[versions++] = derivedType;
		}

		if (versions != length)
			System.arraycopy(annotatedVersions, 0, annotatedVersions = new TypeBinding[versions], 0, versions);
		return annotatedVersions;
	}

	/* This method replaces the version that used to sit in LE. The parameter `annotations' is a flattened sequence of annotations,
	   where each dimension's annotations end with a sentinel null. Leaf type can be an already annotated type.

	   See ArrayBinding.swapUnresolved for further special case handling if incoming leafType is a URB that would resolve to a raw
	   type later.
	*/
	@Override
	public ArrayBinding getArrayType(TypeBinding leafType, int dimensions, AnnotationBinding [] annotations) {
		if (leafType instanceof ArrayBinding) { // substitution attempts can cause this, don't create array of arrays.
			dimensions += leafType.dimensions();
			AnnotationBinding[] leafAnnotations = leafType.getTypeAnnotations();
			leafType = leafType.leafComponentType();
			AnnotationBinding [] allAnnotations = new AnnotationBinding[leafAnnotations.length + annotations.length + 1];
			System.arraycopy(annotations, 0, allAnnotations, 0, annotations.length);
			System.arraycopy(leafAnnotations, 0, allAnnotations, annotations.length + 1 /* leave a null */, leafAnnotations.length);
			annotations = allAnnotations;
		}
		ArrayBinding nakedType = null;
		TypeBinding[] derivedTypes = getDerivedTypes(leafType);
		for (int i = 0, length = derivedTypes.length; i < length; i++) {
			TypeBinding derivedType = derivedTypes[i];
			if (derivedType == null) break;
			if (!derivedType.isArrayType() || derivedType.dimensions() != dimensions || derivedType.leafComponentType() != leafType) //$IDENTITY-COMPARISON$
				continue;
			if (Util.effectivelyEqual(derivedType.getTypeAnnotations(), annotations))
				return (ArrayBinding) derivedType;
			if (!derivedType.hasTypeAnnotations())
				nakedType = (ArrayBinding) derivedType;
		}
		if (nakedType == null)
			nakedType = super.getArrayType(leafType, dimensions);

		if (!haveTypeAnnotations(leafType, annotations))
			return nakedType;

		ArrayBinding arrayType = new ArrayBinding(leafType, dimensions, this.environment);
		arrayType.id = nakedType.id;
		arrayType.setTypeAnnotations(annotations, this.isAnnotationBasedNullAnalysisEnabled);
		return (ArrayBinding) cacheDerivedType(leafType, nakedType, arrayType);
	}

	@Override
	public ArrayBinding getArrayType(TypeBinding leaftType, int dimensions) {
		return getArrayType(leaftType, dimensions, Binding.NO_ANNOTATIONS);
	}

	@Override
	public ReferenceBinding getMemberType(ReferenceBinding memberType, ReferenceBinding enclosingType) {
		if (!haveTypeAnnotations(memberType, enclosingType))
			return super.getMemberType(memberType, enclosingType);
		return (ReferenceBinding) getAnnotatedType(memberType, enclosingType, memberType.getTypeAnnotations());
	}

	@Override
	public ParameterizedTypeBinding getParameterizedType(ReferenceBinding genericType, TypeBinding[] typeArguments, ReferenceBinding enclosingType, AnnotationBinding [] annotations) {

		if (genericType.hasTypeAnnotations())   // @NonNull (List<String>) and not (@NonNull List)<String>
			throw new IllegalStateException();

		ParameterizedTypeBinding parameterizedType = this.parameterizedTypes.get(genericType, typeArguments, enclosingType, annotations);
		if (parameterizedType != null)
			return parameterizedType;

		ParameterizedTypeBinding nakedType = super.getParameterizedType(genericType, typeArguments, enclosingType);

		if (!haveTypeAnnotations(genericType, enclosingType, typeArguments, annotations))
			return nakedType;

		parameterizedType = new ParameterizedTypeBinding(genericType, typeArguments, enclosingType, this.environment);
		parameterizedType.id = nakedType.id;
		parameterizedType.setTypeAnnotations(annotations, this.isAnnotationBasedNullAnalysisEnabled);
		this.parameterizedTypes.put(genericType, typeArguments, enclosingType, parameterizedType);
		return (ParameterizedTypeBinding) cacheDerivedType(genericType, nakedType, parameterizedType);
	}

	@Override
	public ParameterizedTypeBinding getParameterizedType(ReferenceBinding genericType, TypeBinding[] typeArguments, ReferenceBinding enclosingType) {
		return getParameterizedType(genericType, typeArguments, enclosingType, Binding.NO_ANNOTATIONS);
	}

	@Override
	public RawTypeBinding getRawType(ReferenceBinding genericType, ReferenceBinding enclosingType, AnnotationBinding [] annotations) {
		if (genericType.hasTypeAnnotations())
			throw new IllegalStateException();
		if (!genericType.hasEnclosingInstanceContext() && enclosingType != null) {
			enclosingType = (ReferenceBinding) enclosingType.original();
		}

		RawTypeBinding nakedType = null;
		TypeBinding[] derivedTypes = getDerivedTypes(genericType);
		for (int i = 0, length = derivedTypes.length; i < length; i++) {
			TypeBinding derivedType = derivedTypes[i];
			if (derivedType == null)
				break;
			if (!derivedType.isRawType() || derivedType.actualType() != genericType || derivedType.enclosingType() != enclosingType) //$IDENTITY-COMPARISON$
				continue;
			if (Util.effectivelyEqual(derivedType.getTypeAnnotations(), annotations))
				return (RawTypeBinding) derivedType;
			if (!derivedType.hasTypeAnnotations())
				nakedType = (RawTypeBinding) derivedType;
		}
		if (nakedType == null)
			nakedType = super.getRawType(genericType, enclosingType);

		if (!haveTypeAnnotations(genericType, enclosingType, null, annotations))
			return nakedType;

		RawTypeBinding rawType = new RawTypeBinding(genericType, enclosingType, this.environment);
		rawType.id = nakedType.id;
		rawType.setTypeAnnotations(annotations, this.isAnnotationBasedNullAnalysisEnabled);
		return (RawTypeBinding) cacheDerivedType(genericType, nakedType, rawType);
	}

	@Override
	public RawTypeBinding getRawType(ReferenceBinding genericType, ReferenceBinding enclosingType) {
		return getRawType(genericType, enclosingType, Binding.NO_ANNOTATIONS);
	}

	@Override
	public WildcardBinding getWildcard(ReferenceBinding genericType, int rank, TypeBinding bound, TypeBinding[] otherBounds, int boundKind, AnnotationBinding [] annotations) {

		if (genericType == null) // pseudo wildcard denoting composite bounds for lub computation
			genericType = ReferenceBinding.LUB_GENERIC;

		if (genericType.hasTypeAnnotations())
			throw new IllegalStateException();

		WildcardBinding nakedType = null;
		boolean useDerivedTypesOfBound = bound instanceof TypeVariableBinding || (bound instanceof ParameterizedTypeBinding && !(bound instanceof RawTypeBinding)) ;
		TypeBinding[] derivedTypes = getDerivedTypes(useDerivedTypesOfBound ? bound : genericType);
		for (int i = 0, length = derivedTypes.length; i < length; i++) {
			TypeBinding derivedType = derivedTypes[i];
			if (derivedType == null)
				break;
			if (!derivedType.isWildcard() || derivedType.actualType() != genericType || derivedType.rank() != rank) //$IDENTITY-COMPARISON$
				continue;
			if (derivedType.boundKind() != boundKind || derivedType.bound() != bound || !Util.effectivelyEqual(derivedType.additionalBounds(), otherBounds)) //$IDENTITY-COMPARISON$
				continue;
			if (Util.effectivelyEqual(derivedType.getTypeAnnotations(), annotations))
				return (WildcardBinding) derivedType;
			if (!derivedType.hasTypeAnnotations())
				nakedType = (WildcardBinding) derivedType;
		}

		if (nakedType == null)
			nakedType = super.getWildcard(genericType, rank, bound, otherBounds, boundKind);

		if (!haveTypeAnnotations(genericType, bound, otherBounds, annotations))
			return nakedType;

		WildcardBinding wildcard = new WildcardBinding(genericType, rank, bound, otherBounds, boundKind, this.environment);
		wildcard.id = nakedType.id;
		wildcard.setTypeAnnotations(annotations, this.isAnnotationBasedNullAnalysisEnabled);
		return (WildcardBinding) cacheDerivedType(useDerivedTypesOfBound ? bound : genericType, nakedType, wildcard);
	}

	@Override
	public WildcardBinding getWildcard(ReferenceBinding genericType, int rank, TypeBinding bound, TypeBinding[] otherBounds, int boundKind) {
		return getWildcard(genericType, rank, bound, otherBounds, boundKind, Binding.NO_ANNOTATIONS);
	}

	/* Take a type and apply annotations to various components of it. By construction when we see the type reference @Outer Outer.@Middle Middle.@Inner Inner,
	   we first construct the binding for Outer.Middle.Inner and then annotate various parts of it. Likewise for PQTR's binding.
	*/
	@Override
	public TypeBinding getAnnotatedType(TypeBinding type, AnnotationBinding[][] annotations) {

		if (type == null || !type.isValidBinding() || annotations == null || annotations.length == 0)
			return type;

		TypeBinding annotatedType = null;
		switch (type.kind()) {
			case Binding.ARRAY_TYPE:
				ArrayBinding arrayBinding = (ArrayBinding) type;
				annotatedType = getArrayType(arrayBinding.leafComponentType, arrayBinding.dimensions, flattenedAnnotations(annotations));
				break;
			case Binding.BASE_TYPE:
			case Binding.TYPE:
			case Binding.GENERIC_TYPE:
			case Binding.PARAMETERIZED_TYPE:
			case Binding.RAW_TYPE:
			case Binding.TYPE_PARAMETER:
			case Binding.WILDCARD_TYPE:
			case Binding.INTERSECTION_TYPE:
			case Binding.INTERSECTION_TYPE18:
				/* Taking the binding of QTR as an example, there could be different annotatable components, but we come in a with a single binding, e.g:
				   @T Z;                                      type => Z  annotations => [[@T]]
				   @T Y.@T Z                                  type => Z  annotations => [[@T][@T]]
				   @T X.@T Y.@T Z                             type => Z  annotations => [[@T][@T][@T]]
				   java.lang.@T X.@T Y.@T Z                   type => Z  annotations => [[][][@T][@T][@T]]
				   in all these cases the incoming type binding is for Z, but annotations are for different levels. We need to align their layout for proper attribution.
				 */

				if (type.isUnresolvedType() && CharOperation.indexOf('$', type.sourceName()) > 0)
				    type = BinaryTypeBinding.resolveType(type, this.environment, true); // must resolve member types before asking for enclosingType

				int levels = type.depth() + 1;
				TypeBinding [] types = new TypeBinding[levels];
				types[--levels] = type;
				TypeBinding enclosingType = type.enclosingType();
				while (enclosingType != null) {
					types[--levels] = enclosingType;
					enclosingType = enclosingType.enclosingType();
				}
				// Locate the outermost type being annotated. Beware annotations.length could be > types.length (for package qualified names in QTR/PQTR)
				levels = annotations.length;
				int i, j = types.length - levels;
				for (i = 0 ; i < levels; i++, j++) {
					if (annotations[i] != null && annotations[i].length > 0)
						break;
				}
				if (i == levels) // empty annotations array ?
					return type;
				if (j < 0) // Not kosher, broken type that is not flagged as invalid while reporting compilation error ? don't touch.
					return type;
				// types[j] is the first component being annotated. Its annotations are annotations[i]
				for (enclosingType = j == 0 ? null : types[j - 1]; i < levels; i++, j++) {
					final TypeBinding currentType = types[j];
					// while handling annotations from SE7 locations, take care not to drop existing annotations.
					AnnotationBinding [] currentAnnotations = annotations[i] != null && annotations[i].length > 0 ? annotations[i] : currentType.getTypeAnnotations();
					annotatedType = getAnnotatedType(currentType, enclosingType, currentAnnotations);
					enclosingType = annotatedType;
				}
				break;
			default:
				throw new IllegalStateException();
		}
		return annotatedType;
	}

	/* Private subroutine for public APIs. Create an annotated version of the type. To materialize the annotated version, we can't use new since
	   this is a general purpose method designed to deal type bindings of all types. "Clone" the incoming type, specializing for any enclosing type
	   that may itself be possibly be annotated. This is so the binding for @Outer Outer.Inner != Outer.@Inner Inner != @Outer Outer.@Inner Inner.
	   Likewise so the bindings for @Readonly List<@NonNull String> != @Readonly List<@Nullable String> != @Readonly List<@Interned String>
	*/
	private TypeBinding getAnnotatedType(TypeBinding type, TypeBinding enclosingType, AnnotationBinding[] annotations) {
		if (type.kind() == Binding.PARAMETERIZED_TYPE) {
			return getParameterizedType(type.actualType(), type.typeArguments(), (ReferenceBinding) enclosingType, annotations);
		}
		TypeBinding nakedType = null;
		TypeBinding[] derivedTypes = getDerivedTypes(type);
		for (int i = 0, length = derivedTypes.length; i < length; i++) {
			TypeBinding derivedType = derivedTypes[i];
			if (derivedType == null) break;

			if (derivedType.enclosingType() != enclosingType || !Util.effectivelyEqual(derivedType.typeArguments(), type.typeArguments())) //$IDENTITY-COMPARISON$
				continue;

			switch(type.kind()) {
				case Binding.ARRAY_TYPE:
					if (!derivedType.isArrayType() || derivedType.dimensions() != type.dimensions() || derivedType.leafComponentType() != type.leafComponentType()) //$IDENTITY-COMPARISON$
						continue;
					break;
				case Binding.RAW_TYPE:
					if (!derivedType.isRawType() || derivedType.actualType() != type.actualType()) //$IDENTITY-COMPARISON$
						continue;
					break;
				case Binding.INTERSECTION_TYPE:
				case Binding.WILDCARD_TYPE:
					if (!derivedType.isWildcard() || derivedType.actualType() != type.actualType() || derivedType.rank() != type.rank() || derivedType.boundKind() != type.boundKind()) //$IDENTITY-COMPARISON$
						continue;
					if (derivedType.bound() != type.bound() || !Util.effectivelyEqual(derivedType.additionalBounds(), type.additionalBounds())) //$IDENTITY-COMPARISON$
						continue;
					break;
				default:
					switch(derivedType.kind()) {
						case Binding.ARRAY_TYPE:
						case Binding.RAW_TYPE:
						case Binding.WILDCARD_TYPE:
						case Binding.INTERSECTION_TYPE18:
						case Binding.INTERSECTION_TYPE:
							continue;
					}
					break;
			}
			if (Util.effectivelyEqual(derivedType.getTypeAnnotations(), annotations)) {
				return derivedType;
			}
			if (!derivedType.hasTypeAnnotations())
				nakedType = derivedType;
		}
		if (nakedType == null)
			nakedType = getUnannotatedType(type);

		if (!haveTypeAnnotations(type, enclosingType, null, annotations))
			return nakedType;

		TypeBinding annotatedType = type.clone(enclosingType);
		annotatedType.id = nakedType.id;
		annotatedType.setTypeAnnotations(annotations, this.isAnnotationBasedNullAnalysisEnabled);
		if (this.isAnnotationBasedNullAnalysisEnabled && (annotatedType.tagBits & TagBits.AnnotationNullMASK) == 0) {
			// propagate nullness unless overridden in 'annotations':
			annotatedType.tagBits |= type.tagBits & TagBits.AnnotationNullMASK;
		}
		TypeBinding keyType;
		switch (type.kind()) {
			case Binding.ARRAY_TYPE:
				keyType = type.leafComponentType();
				break;
			case Binding.RAW_TYPE:
			case Binding.WILDCARD_TYPE:
				keyType = type.actualType();
				break;
			default:
				keyType = nakedType;
				break;
		}
		return cacheDerivedType(keyType, nakedType, annotatedType);
	}

	private boolean haveTypeAnnotations(TypeBinding baseType, TypeBinding someType, TypeBinding[] someTypes, AnnotationBinding[] annotations) {
		if (baseType != null && baseType.hasTypeAnnotations())
			return true;
		if (someType != null && someType.hasTypeAnnotations())
			return true;
		for (int i = 0, length = annotations == null ? 0 : annotations.length; i < length; i++)
			if (annotations [i] != null)
				return true;
		for (int i = 0, length = someTypes == null ? 0 : someTypes.length; i < length; i++)
			if (someTypes[i].hasTypeAnnotations())
				return true;
		return false;
	}

	private boolean haveTypeAnnotations(TypeBinding leafType, AnnotationBinding[] annotations) {
		return haveTypeAnnotations(leafType, null, null, annotations);
	}

	private boolean haveTypeAnnotations(TypeBinding memberType, TypeBinding enclosingType) {
		return haveTypeAnnotations(memberType, enclosingType, null, null);
	}

	/* Utility method to "flatten" annotations. For multidimensional arrays, we encode the annotations into a flat array
	   where a null separates the annotations of dimension n from dimension n - 1 as well as dimenion n + 1. There is a
	   final null always.
	*/
	static AnnotationBinding [] flattenedAnnotations (AnnotationBinding [][] annotations) {

		if (annotations == null || annotations.length == 0)
			return Binding.NO_ANNOTATIONS;

		int levels = annotations.length;
		int length = levels;
		for (int i = 0; i < levels; i++) {
			length += annotations[i] == null ? 0 : annotations[i].length;
		}
		if (length == 0)
			return Binding.NO_ANNOTATIONS;

		AnnotationBinding[] series = new AnnotationBinding [length];
		int index = 0;
		for (int i = 0; i < levels; i++) {
			final int annotationsLength = annotations[i] == null ? 0 : annotations[i].length;
			if (annotationsLength > 0) {
				System.arraycopy(annotations[i], 0, series, index, annotationsLength);
				index += annotationsLength;
			}
			series[index++] = null;
		}
		if (index != length)
			throw new IllegalStateException();
		return series;
	}

	@Override
	public boolean isAnnotatedTypeSystem() {
		return true;
	}
}