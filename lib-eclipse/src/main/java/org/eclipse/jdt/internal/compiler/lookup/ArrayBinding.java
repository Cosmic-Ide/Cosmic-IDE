/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *								bug 392862 - [1.8][compiler][null] Evaluate null annotations on array types
 *								bug 395002 - Self bound generic class doesn't resolve bounds properly for wildcards for certain parametrisation.
 *								bug 392384 - [1.8][compiler][null] Restore nullness info from type annotations in class files
 *								Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								Bug 415291 - [1.8][null] differentiate type incompatibilities due to null annotations
 *								Bug 415850 - [1.8] Ensure RunJDTCoreTests can cope with null annotations enabled
 *								Bug 416176 - [1.8][compiler][null] null type annotations cause grief on type variables
 *								Bug 417295 - [1.8[[null] Massage type annotated null analysis to gel well with deep encoded type bindings.
 *								Bug 400874 - [1.8][compiler] Inference infrastructure should evolve to meet JLS8 18.x (Part G of JSR335 spec)
 *								Bug 425460 - [1.8] [inference] Type not inferred on stream.toArray
 *								Bug 426792 - [1.8][inference][impl] generify new type inference engine
 *								Bug 428019 - [1.8][compiler] Type inference failure with nested generic invocation.
 *								Bug 438458 - [1.8][null] clean up handling of null type annotations wrt type variables
 *								Bug 440759 - [1.8][null] @NonNullByDefault should never affect wildcards and uses of a type variable
 *								Bug 441693 - [1.8][null] Bogus warning for type argument annotated with @NonNull
 *     Jesper S Møller - Contributions for bug 381345 : [1.8] Take care of the Java 8 major version
 *								Bug 527554 - [18.3] Compiler support for JEP 286 Local-Variable Type
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.Constant;

public final class ArrayBinding extends TypeBinding {
	// creation and initialization of the length field
	// the declaringClass of this field is intentionally set to null so it can be distinguished.
	public static final FieldBinding ArrayLength = new FieldBinding(TypeConstants.LENGTH, TypeBinding.INT, ClassFileConstants.AccPublic | ClassFileConstants.AccFinal, null, Constant.NotAConstant);

	public TypeBinding leafComponentType;
	public int dimensions;
	LookupEnvironment environment;
	char[] constantPoolName;
	char[] genericTypeSignature;

	// One bitset for each dimension plus one more for the leaf component type at position 'dimensions',
	// possible bits are TagBits.AnnotationNonNull and TagBits.AnnotationNullable
	// (only ever set when CompilerOptions.isAnnotationBasedNullAnalysisEnabled == true):
	public long[] nullTagBitsPerDimension;

	private MethodBinding clone;

public ArrayBinding(TypeBinding type, int dimensions, LookupEnvironment environment) {
	this.tagBits |= TagBits.IsArrayType;
	this.leafComponentType = type;
	this.dimensions = dimensions;
	this.environment = environment;
	if (type instanceof UnresolvedReferenceBinding)
		((UnresolvedReferenceBinding) type).addWrapper(this, environment);
	else
		this.tagBits |= type.tagBits & (TagBits.HasTypeVariable | TagBits.HasDirectWildcard | TagBits.HasMissingType | TagBits.ContainsNestedTypeReferences | TagBits.HasCapturedWildcard);
	long mask = type.tagBits & TagBits.AnnotationNullMASK;
	if (mask != 0) {
		this.nullTagBitsPerDimension = new long[this.dimensions + 1];
		this.nullTagBitsPerDimension[this.dimensions] = mask;
		this.tagBits |= TagBits.HasNullTypeAnnotation;
	}
}

@Override
public TypeBinding closestMatch() {
	if (isValidBinding()) {
		return this;
	}
	TypeBinding leafClosestMatch = this.leafComponentType.closestMatch();
	if (leafClosestMatch == null) {
		return null;
	}
	return this.environment.createArrayType(this.leafComponentType.closestMatch(), this.dimensions);
}

/**
 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#collectMissingTypes(java.util.List)
 */
@Override
public List<TypeBinding> collectMissingTypes(List<TypeBinding> missingTypes) {
	if ((this.tagBits & TagBits.HasMissingType) != 0) {
		missingTypes = this.leafComponentType.collectMissingTypes(missingTypes);
	}
	return missingTypes;
}

@Override
public void collectSubstitutes(Scope scope, TypeBinding actualType, InferenceContext inferenceContext, int constraint) {

	if ((this.tagBits & TagBits.HasTypeVariable) == 0) return;
	if (actualType == TypeBinding.NULL || actualType.kind() == POLY_TYPE) return;

	switch(actualType.kind()) {
		case Binding.ARRAY_TYPE :
	        int actualDim = actualType.dimensions();
	        if (actualDim == this.dimensions) {
			    this.leafComponentType.collectSubstitutes(scope, actualType.leafComponentType(), inferenceContext, constraint);
	        } else if (actualDim > this.dimensions) {
	            ArrayBinding actualReducedType = this.environment.createArrayType(actualType.leafComponentType(), actualDim - this.dimensions);
	            this.leafComponentType.collectSubstitutes(scope, actualReducedType, inferenceContext, constraint);
	        }
			break;
		case Binding.TYPE_PARAMETER :
			//TypeVariableBinding variable = (TypeVariableBinding) otherType;
			// TODO (philippe) should consider array bounds, and recurse
			break;
	}
}

@Override
public boolean mentionsAny(TypeBinding[] parameters, int idx) {
	return this.leafComponentType.mentionsAny(parameters, idx);
}

@Override
void collectInferenceVariables(Set<InferenceVariable> variables) {
	this.leafComponentType.collectInferenceVariables(variables);
}

@Override
TypeBinding substituteInferenceVariable(InferenceVariable var, TypeBinding substituteType) {
	TypeBinding substitutedLeaf = this.leafComponentType.substituteInferenceVariable(var, substituteType);
	if (TypeBinding.notEquals(substitutedLeaf, this.leafComponentType))
		return this.environment.createArrayType(substitutedLeaf, this.dimensions, this.typeAnnotations);
	return this;
}

/*
 * brakets leafUniqueKey
 * p.X[][] --> [[Lp/X;
 */
@Override
public char[] computeUniqueKey(boolean isLeaf) {
	char[] brackets = new char[this.dimensions];
	for (int i = this.dimensions - 1; i >= 0; i--) brackets[i] = '[';
	return CharOperation.concat(brackets, this.leafComponentType.computeUniqueKey(isLeaf));
 }

@Override
public char[] constantPoolName() {
	if (this.constantPoolName != null)
		return this.constantPoolName;

	char[] brackets = new char[this.dimensions];
	for (int i = this.dimensions - 1; i >= 0; i--) brackets[i] = '[';
	return this.constantPoolName = CharOperation.concat(brackets, this.leafComponentType.signature());
}
@Override
public String debugName() {
	if (this.hasTypeAnnotations())
		return annotatedDebugName();
	StringBuilder brackets = new StringBuilder(this.dimensions * 2);
	for (int i = this.dimensions; --i >= 0;)
		brackets.append("[]"); //$NON-NLS-1$
	return this.leafComponentType.debugName() + brackets.toString();
}

@Override
public String annotatedDebugName() {
	StringBuilder brackets = new StringBuilder(this.dimensions * 2);
	brackets.append(this.leafComponentType.annotatedDebugName());
	brackets.append(' ');
	AnnotationBinding [] annotations = getTypeAnnotations();
	for (int i = 0, j = -1; i < this.dimensions; i++) {
		if (annotations != null) {
			if (i != 0)
				brackets.append(' ');
			while (++j < annotations.length && annotations[j] != null) {
				brackets.append(annotations[j]);
				brackets.append(' ');
			}
		}
		brackets.append("[]"); //$NON-NLS-1$
	}
	return brackets.toString();
}

@Override
public int dimensions() {
	return this.dimensions;
}

/* Answer an array whose dimension size is one less than the receiver.
*
* When the receiver's dimension size is one then answer the leaf component type.
*/

public TypeBinding elementsType() {

	if (this.dimensions == 1)
		return this.leafComponentType;

	AnnotationBinding [] oldies = getTypeAnnotations();
	AnnotationBinding [] newbies = Binding.NO_ANNOTATIONS;

	for (int i = 0, length = oldies == null ? 0 : oldies.length; i < length; i++) {
		if (oldies[i] == null) {
			System.arraycopy(oldies, i+1, newbies = new AnnotationBinding[length - i - 1], 0, length - i - 1);
			break;
		}
	}
	return this.environment.createArrayType(this.leafComponentType, this.dimensions - 1, newbies);
}

/**
 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#erasure()
 */
@Override
public TypeBinding erasure() {
    TypeBinding erasedType = this.leafComponentType.erasure();
    if (TypeBinding.notEquals(this.leafComponentType, erasedType))
        return this.environment.createArrayType(erasedType, this.dimensions);
    return this;
}

@Override
public ArrayBinding upwardsProjection(Scope scope, TypeBinding[] mentionedTypeVariables) {
	TypeBinding leafType = this.leafComponentType.upwardsProjection(scope, mentionedTypeVariables);
	return scope.environment().createArrayType(leafType, this.dimensions, this.typeAnnotations);
}

@Override
public ArrayBinding downwardsProjection(Scope scope, TypeBinding[] mentionedTypeVariables) {
	TypeBinding leafType = this.leafComponentType.downwardsProjection(scope, mentionedTypeVariables);
	return scope.environment().createArrayType(leafType, this.dimensions, this.typeAnnotations);
}

public LookupEnvironment environment() {
    return this.environment;
}

@Override
public char[] genericTypeSignature() {

    if (this.genericTypeSignature == null) {
		char[] brackets = new char[this.dimensions];
		for (int i = this.dimensions - 1; i >= 0; i--) brackets[i] = '[';
		this.genericTypeSignature = CharOperation.concat(brackets, this.leafComponentType.genericTypeSignature());
    }
    return this.genericTypeSignature;
}

@Override
public PackageBinding getPackage() {
	return this.leafComponentType.getPackage();
}

@Override
public int hashCode() {
	return this.leafComponentType == null ? super.hashCode() : this.leafComponentType.hashCode();
}

/* Answer true if the receiver type can be assigned to the argument type (right)
*/
@Override
public boolean isCompatibleWith(TypeBinding otherType, Scope captureScope) {
	if (equalsEquals(this, otherType))
		return true;

	switch (otherType.kind()) {
		case Binding.ARRAY_TYPE :
			ArrayBinding otherArray = (ArrayBinding) otherType;
			if (otherArray.leafComponentType.isBaseType())
				return false; // relying on the fact that all equal arrays are identical
			if (this.dimensions == otherArray.dimensions)
				return this.leafComponentType.isCompatibleWith(otherArray.leafComponentType);
			if (this.dimensions < otherArray.dimensions)
				return false; // cannot assign 'String[]' into 'Object[][]' but can assign 'byte[][]' into 'Object[]'
			break;
		case Binding.BASE_TYPE :
			return false;
		case Binding.WILDCARD_TYPE :
		case Binding.INTERSECTION_TYPE :
		    return ((WildcardBinding) otherType).boundCheck(this);

		case Binding.INTERSECTION_TYPE18:
			for (ReferenceBinding intersecting : ((IntersectionTypeBinding18) otherType).intersectingTypes) {
				if (!isCompatibleWith(intersecting, captureScope))
					return false;
			}
			return true;

		case Binding.TYPE_PARAMETER :
			// check compatibility with capture of ? super X
			if (otherType.isCapture()) {
				CaptureBinding otherCapture = (CaptureBinding) otherType;
				TypeBinding otherLowerBound;
				if ((otherLowerBound = otherCapture.lowerBound) != null) {
					if (!otherLowerBound.isArrayType()) return false;
					return isCompatibleWith(otherLowerBound, captureScope);
				}
			}
			return false;

	}
	//Check dimensions - Java does not support explicitly sized dimensions for types.
	//However, if it did, the type checking support would go here.
	switch (otherType.leafComponentType().id) {
	    case TypeIds.T_JavaLangObject :
	    case TypeIds.T_JavaLangCloneable :
	    case TypeIds.T_JavaIoSerializable :
	        return true;
	}
	return false;
}

@Override
public boolean isSubtypeOf(TypeBinding otherType, boolean simulatingBugJDK8026527) {
	if (equalsEquals(this, otherType))
		return true;

	switch (otherType.kind()) {
		case Binding.ARRAY_TYPE :
			ArrayBinding otherArray = (ArrayBinding) otherType;
			if (otherArray.leafComponentType.isBaseType())
				return false; // relying on the fact that all equal arrays are identical
			if (this.dimensions == otherArray.dimensions)
				return this.leafComponentType.isSubtypeOf(otherArray.leafComponentType, simulatingBugJDK8026527);
			if (this.dimensions < otherArray.dimensions)
				return false; // cannot assign 'String[]' into 'Object[][]' but can assign 'byte[][]' into 'Object[]'
			break;
		case Binding.BASE_TYPE :
			return false;
		case Binding.INTERSECTION_TYPE18:
			for (ReferenceBinding intersecting : ((IntersectionTypeBinding18) otherType).intersectingTypes) {
				if (!isSubtypeOf(intersecting, simulatingBugJDK8026527))
					return false;
			}
			return true;
		case Binding.TYPE_PARAMETER:
			// check compatibility with capture of ? super X
			if (otherType.isCapture()) {
				CaptureBinding otherCapture = (CaptureBinding) otherType;
				TypeBinding otherLowerBound;
				if ((otherLowerBound = otherCapture.lowerBound) != null) {
					if (!otherLowerBound.isArrayType()) return false;
					return isSubtypeOf(otherLowerBound, simulatingBugJDK8026527);
				}
			}
	}
	switch (otherType.leafComponentType().id) {
	    case TypeIds.T_JavaLangObject :
	    case TypeIds.T_JavaLangCloneable :
	    case TypeIds.T_JavaIoSerializable :
	        return true;
	}
	return false;
}

@Override
public boolean isProperType(boolean admitCapture18) {
	return this.leafComponentType.isProperType(admitCapture18);
}

@Override
public int kind() {
	return ARRAY_TYPE;
}

@Override
public TypeBinding leafComponentType(){
	return this.leafComponentType;
}

@Override
public char[] nullAnnotatedReadableName(CompilerOptions options, boolean shortNames) /* java.lang.Object @o.e.j.a.NonNull[] */ {
	if (this.nullTagBitsPerDimension == null)
		return shortNames ? shortReadableName() : readableName();
	char[][] brackets = new char[this.dimensions][];
	for (int i = 0; i < this.dimensions; i++) {
		if ((this.nullTagBitsPerDimension[i] & TagBits.AnnotationNullMASK) != 0) {
			char[][] fqAnnotationName;
			if ((this.nullTagBitsPerDimension[i] & TagBits.AnnotationNonNull) != 0)
				fqAnnotationName = options.nonNullAnnotationName;
			else
				fqAnnotationName = options.nullableAnnotationName;
			char[] annotationName = shortNames
										? fqAnnotationName[fqAnnotationName.length-1]
										: CharOperation.concatWith(fqAnnotationName, '.');
			brackets[i] = new char[annotationName.length+3];
			brackets[i][0] = '@';
			System.arraycopy(annotationName, 0, brackets[i], 1, annotationName.length);
			brackets[i][annotationName.length+1] = '[';
			brackets[i][annotationName.length+2] = ']';
		} else {
			brackets[i] = new char[]{'[', ']'};
		}
	}
	return CharOperation.concat(this.leafComponentType.nullAnnotatedReadableName(options, shortNames),
								 CharOperation.concatWith(brackets, ' '),
								 ' ');
}

/* API
* Answer the problem id associated with the receiver.
* NoError if the receiver is a valid binding.
*/
@Override
public int problemId() {
	return this.leafComponentType.problemId();
}

@Override
public char[] qualifiedSourceName() {
	char[] brackets = new char[this.dimensions * 2];
	for (int i = this.dimensions * 2 - 1; i >= 0; i -= 2) {
		brackets[i] = ']';
		brackets[i - 1] = '[';
	}
	return CharOperation.concat(this.leafComponentType.qualifiedSourceName(), brackets);
}
@Override
public char[] readableName() /* java.lang.Object[] */ {
	char[] brackets = new char[this.dimensions * 2];
	for (int i = this.dimensions * 2 - 1; i >= 0; i -= 2) {
		brackets[i] = ']';
		brackets[i - 1] = '[';
	}
	return CharOperation.concat(this.leafComponentType.readableName(), brackets);
}

@Override
public void setTypeAnnotations(AnnotationBinding[] annotations, boolean evalNullAnnotations) {
	this.tagBits |= TagBits.HasTypeAnnotations;
	if (annotations == null || annotations.length == 0)
		return;
	this.typeAnnotations = annotations;

	if (evalNullAnnotations) {
		long nullTagBits = 0;
		if (this.nullTagBitsPerDimension == null)
			this.nullTagBitsPerDimension = new long[this.dimensions + 1];

		int dimension = 0;
		for (int i = 0, length = annotations.length; i < length; i++) {
			AnnotationBinding annotation = annotations[i];
			if (annotation != null) {
				if (annotation.type.hasNullBit(TypeIds.BitNullableAnnotation)) {
					nullTagBits  |= TagBits.AnnotationNullable;
					this.tagBits |= TagBits.HasNullTypeAnnotation;
				} else if (annotation.type.hasNullBit(TypeIds.BitNonNullAnnotation)) {
					nullTagBits  |= TagBits.AnnotationNonNull;
					this.tagBits |= TagBits.HasNullTypeAnnotation;
				}
			} else {
				// null signals end of annotations for the current dimension in the serialized form.
				if (nullTagBits != 0) {
					this.nullTagBitsPerDimension[dimension] = nullTagBits;
					nullTagBits = 0;
				}
				dimension++;
			}
		}
		this.tagBits |= this.nullTagBitsPerDimension[0]; // outer-most dimension
	}
}
@Override
public char[] shortReadableName(){
	char[] brackets = new char[this.dimensions * 2];
	for (int i = this.dimensions * 2 - 1; i >= 0; i -= 2) {
		brackets[i] = ']';
		brackets[i - 1] = '[';
	}
	return CharOperation.concat(this.leafComponentType.shortReadableName(), brackets);
}
@Override
public char[] sourceName() {
	char[] brackets = new char[this.dimensions * 2];
	for (int i = this.dimensions * 2 - 1; i >= 0; i -= 2) {
		brackets[i] = ']';
		brackets[i - 1] = '[';
	}
	return CharOperation.concat(this.leafComponentType.sourceName(), brackets);
}
@Override
public void swapUnresolved(UnresolvedReferenceBinding unresolvedType, ReferenceBinding resolvedType, LookupEnvironment env) {
	if (this.leafComponentType == unresolvedType) { //$IDENTITY-COMPARISON$
		this.leafComponentType = env.convertUnresolvedBinaryToRawType(resolvedType);
		/* Leaf component type is the key in the type system. If it undergoes change, the array has to be rehashed.
		   We achieve by creating a fresh array with the new component type and equating this array's id with that.
		   This means this array can still be found under the old key, but that is harmless (since the component type
		   is always consulted (see TypeSystem.getArrayType()).

		   This also means that this array type is not a fully interned singleton: There is `this' object and there is
		   the array that is being created down below that gets cached by the type system and doled out for all further
		   array creations against the same (raw) component type, dimensions and annotations. This again is harmless,
		   since TypeBinding.id is consulted for (in)equality checks.

		   See https://bugs.eclipse.org/bugs/show_bug.cgi?id=430425 for details and a test case.
		*/
		if (this.leafComponentType != resolvedType) //$IDENTITY-COMPARISON$
			this.id = env.createArrayType(this.leafComponentType, this.dimensions, this.typeAnnotations).id;
		this.tagBits |= this.leafComponentType.tagBits & (TagBits.HasTypeVariable | TagBits.HasDirectWildcard | TagBits.HasMissingType | TagBits.HasCapturedWildcard);
	}
}
@Override
public String toString() {
	return this.leafComponentType != null ? debugName() : "NULL TYPE ARRAY"; //$NON-NLS-1$
}
@Override
public TypeBinding unannotated() {
	return this.hasTypeAnnotations() ? this.environment.getUnannotatedType(this) : this;
}
@Override
public TypeBinding withoutToplevelNullAnnotation() {
	if (!hasNullTypeAnnotations())
		return this;
	AnnotationBinding[] newAnnotations = this.environment.filterNullTypeAnnotations(this.typeAnnotations);
	return this.environment.createArrayType(this.leafComponentType, this.dimensions, newAnnotations);
}
@Override
public TypeBinding uncapture(Scope scope) {
	if ((this.tagBits & TagBits.HasCapturedWildcard) == 0)
		return this;
	TypeBinding leafType = this.leafComponentType.uncapture(scope);
	return scope.environment().createArrayType(leafType, this.dimensions, this.typeAnnotations);
}
@Override
public boolean acceptsNonNullDefault() {
	return true;
}
@Override
public long updateTagBits() {
	if (this.leafComponentType != null)
		this.tagBits |= this.leafComponentType.updateTagBits();
	return super.updateTagBits();
}

/**
 * The type of x.clone() is substituted from 'Object' into the type of the receiver array (non-null)
 */
public MethodBinding getCloneMethod(final MethodBinding originalMethod) {
	if (this.clone != null)
		return this.clone;
	MethodBinding method = new MethodBinding() {
		@Override
		public char[] signature(ClassFile classFile) {
			return originalMethod.signature(); // for codeGen we need to answer the signature of j.l.Object.clone()
		}
	};
	method.modifiers = originalMethod.modifiers;
	method.selector = originalMethod.selector;
	method.declaringClass = originalMethod.declaringClass; // cannot set array binding as declaring class, will be tweaked in CodeStream.getConstantPoolDeclaringClass()
	method.typeVariables = Binding.NO_TYPE_VARIABLES;
	method.parameters = originalMethod.parameters;
	method.thrownExceptions = Binding.NO_EXCEPTIONS;
	method.tagBits = originalMethod.tagBits;
	method.returnType = this.environment.globalOptions.sourceLevel >= ClassFileConstants.JDK1_5 ? this : originalMethod.returnType;
	if (this.environment.globalOptions.isAnnotationBasedNullAnalysisEnabled) {
		if (this.environment.usesNullTypeAnnotations())
			method.returnType = this.environment.createAnnotatedType(method.returnType, new AnnotationBinding[] { this.environment.getNonNullAnnotation() });
		else
			method.tagBits |= TagBits.AnnotationNonNull;
	}
	if ((method.returnType.tagBits & TagBits.HasMissingType) != 0) {
		method.tagBits |=  TagBits.HasMissingType;
	}
	return this.clone = method;
}
public static boolean isArrayClone(TypeBinding receiverType, MethodBinding binding) {
	if (receiverType instanceof ArrayBinding) {
		MethodBinding clone = ((ArrayBinding) receiverType).clone;
		return clone != null && binding == clone;
	}
	return false;
}
}
