/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *								bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								bug 392862 - [1.8][compiler][null] Evaluate null annotations on array types
 *								bug 392384 - [1.8][compiler][null] Restore nullness info from type annotations in class files
 *								Bug 415043 - [1.8][null] Follow-up re null type annotations after bug 392099
 *								Bug 415850 - [1.8] Ensure RunJDTCoreTests can cope with null annotations enabled
 *								Bug 417295 - [1.8[[null] Massage type annotated null analysis to gel well with deep encoded type bindings.
 *								Bug 427163 - [1.8][null] bogus error "Contradictory null specification" on varags
 *								Bug 429958 - [1.8][null] evaluate new DefaultLocation attribute of @NonNullByDefault
 *								Bug 434570 - Generic type mismatch for parametrized class annotation attribute with inner class
 *								Bug 434600 - Incorrect null analysis error reporting on type parameters
 *								Bug 439516 - [1.8][null] NonNullByDefault wrongly applied to implicit type bound of binary type
 *								Bug 438458 - [1.8][null] clean up handling of null type annotations wrt type variables
 *								Bug 435570 - [1.8][null] @NonNullByDefault illegally tries to affect "throws E"
 *								Bug 435805 - [1.8][compiler][null] Java 8 compiler does not recognize declaration style null annotations
 *								Bug 437072 - [compiler][null] Null analysis emits possibly incorrect warning for new int[][] despite @NonNullByDefault
 *								Bug 466713 - Null Annotations: NullPointerException using <int @Nullable []> as Type Param
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *                          Bug 409236 - [1.8][compiler] Type annotations on intersection cast types dropped by code generator
 *                          Bug 415399 - [1.8][compiler] Type annotations on constructor results dropped by the code generator
 *      Jesper S Møller <jesper@selskabet.org> -  Contributions for
 *                          bug 527554 - [18.3] Compiler support for JEP 286 Local-Variable Type
 *                          bug 529556 - [18.3] Add content assist support for 'var' as a type
 *
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.NullAnnotationMatching.CheckMode;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.AnnotationContext;
import org.eclipse.jdt.internal.compiler.codegen.AnnotationTargetTypeConstants;
import org.eclipse.jdt.internal.compiler.flow.FlowContext;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.RecordComponentBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.Substitution;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;
@SuppressWarnings({})
public abstract class TypeReference extends Expression {
	public static final TypeReference[] NO_TYPE_ARGUMENTS = new TypeReference[0];

	/**
	 * Simplified specification of where in a (possibly complex) type reference
	 * we are looking for type annotations.
	 * @see TypeReference#hasNullTypeAnnotation(AnnotationPosition)
	 */
	public static enum AnnotationPosition {
		/**
		 * For arrays: the outermost dimension, for parameterized types the type, for nested types the innermost type.
		 * This is the level that a declaration annotation would apply to.
		 */
		MAIN_TYPE,
		/** For arrays: the leaf component type, else like MAIN_TYPE. */
		LEAF_TYPE,
		/** Any position admitting type annotations. */
		ANY
	}

static class AnnotationCollector extends ASTVisitor {
	List<AnnotationContext> annotationContexts;
	Expression typeReference;
	int targetType;
	int info = 0;
	int info2 = 0;
	LocalVariableBinding localVariable;
	Annotation[][] annotationsOnDimensions;
	int dimensions;
	Wildcard currentWildcard;
	RecordComponentBinding recordComponentBinding;

	public AnnotationCollector(
			TypeParameter typeParameter,
			int targetType,
			int typeParameterIndex,
			List<AnnotationContext> annotationContexts) {
		this.annotationContexts = annotationContexts;
		this.typeReference = typeParameter.type;
		this.targetType = targetType;
		this.info = typeParameterIndex;
	}

	public AnnotationCollector(
			LocalDeclaration localDeclaration,
			int targetType,
			LocalVariableBinding localVariable,
			List<AnnotationContext> annotationContexts) {
		this.annotationContexts = annotationContexts;
		this.typeReference = localDeclaration.type;
		this.targetType = targetType;
		this.localVariable = localVariable;
	}

	public AnnotationCollector(
			LocalDeclaration localDeclaration,
			int targetType,
			int parameterIndex,
			List<AnnotationContext> annotationContexts) {
		this.annotationContexts = annotationContexts;
		this.typeReference = localDeclaration.type;
		this.targetType = targetType;
		this.info = parameterIndex;
	}

	public AnnotationCollector(
			TypeReference typeReference,
			int targetType,
			List<AnnotationContext> annotationContexts) {
		this.annotationContexts = annotationContexts;
		this.typeReference = typeReference;
		this.targetType = targetType;
	}
	public AnnotationCollector(
			Expression typeReference,
			int targetType,
			int info,
			List<AnnotationContext> annotationContexts) {
		this.annotationContexts = annotationContexts;
		this.typeReference = typeReference;
		this.info = info;
		this.targetType = targetType;
	}
	public AnnotationCollector(
			TypeReference typeReference,
			int targetType,
			int info,
			int typeIndex,
			List<AnnotationContext> annotationContexts) {
		this.annotationContexts = annotationContexts;
		this.typeReference = typeReference;
		this.info = info;
		this.targetType = targetType;
		this.info2 = typeIndex;
	}
	public AnnotationCollector(
			TypeReference typeReference,
			int targetType,
			int info,
			List<AnnotationContext> annotationContexts,
			Annotation[][] annotationsOnDimensions,
			int dimensions) {
		this.annotationContexts = annotationContexts;
		this.typeReference = typeReference;
		this.info = info;
		this.targetType = targetType;
		this.annotationsOnDimensions = annotationsOnDimensions;
		// Array references like 'new String[]' manifest as an ArrayAllocationExpression
		// with a 'type' of String.  When the type is not carrying the dimensions count
		// it is passed in via the dimensions parameter.  It is not possible to use
		// annotationsOnDimensions as it will be null if there are no annotations on any
		// of the dimensions.
		this.dimensions = dimensions;
	}

	public AnnotationCollector(RecordComponent recordComponent, int targetType, List<AnnotationContext> annotationContexts) {
		this.annotationContexts = annotationContexts;
		this.typeReference = recordComponent.type;
		this.targetType = targetType;
		this.recordComponentBinding = recordComponent.binding;
	}

	private boolean internalVisit(Annotation annotation) {
		AnnotationContext annotationContext = null;
		if (annotation.isRuntimeTypeInvisible()) {
			annotationContext = new AnnotationContext(annotation, this.typeReference, this.targetType, AnnotationContext.INVISIBLE);
		} else if (annotation.isRuntimeTypeVisible()) {
			annotationContext = new AnnotationContext(annotation, this.typeReference, this.targetType, AnnotationContext.VISIBLE);
		}
		if (annotationContext != null) {
			annotationContext.wildcard = this.currentWildcard;
			switch(this.targetType) {
				case AnnotationTargetTypeConstants.CLASS_TYPE_PARAMETER :
				case AnnotationTargetTypeConstants.METHOD_TYPE_PARAMETER :
				case AnnotationTargetTypeConstants.CLASS_EXTENDS:
				case AnnotationTargetTypeConstants.METHOD_FORMAL_PARAMETER :
				case AnnotationTargetTypeConstants.THROWS :
				case AnnotationTargetTypeConstants.EXCEPTION_PARAMETER :
				case AnnotationTargetTypeConstants.INSTANCEOF:
				case AnnotationTargetTypeConstants.NEW :
				case AnnotationTargetTypeConstants.CONSTRUCTOR_REFERENCE :
				case AnnotationTargetTypeConstants.METHOD_REFERENCE :
					annotationContext.info = this.info;
					break;
				case AnnotationTargetTypeConstants.LOCAL_VARIABLE :
				case AnnotationTargetTypeConstants.RESOURCE_VARIABLE :
					annotationContext.variableBinding = this.localVariable;
					break;
				case AnnotationTargetTypeConstants.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT :
				case AnnotationTargetTypeConstants.METHOD_INVOCATION_TYPE_ARGUMENT :
				case AnnotationTargetTypeConstants.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT :
				case AnnotationTargetTypeConstants.METHOD_REFERENCE_TYPE_ARGUMENT :
				case AnnotationTargetTypeConstants.CLASS_TYPE_PARAMETER_BOUND :
				case AnnotationTargetTypeConstants.METHOD_TYPE_PARAMETER_BOUND :
				case AnnotationTargetTypeConstants.CAST:
					annotationContext.info2 = this.info2;
					annotationContext.info = this.info;
					break;
				case AnnotationTargetTypeConstants.FIELD :
// 				case AnnotationTargetTypeConstants.RECORD_COMPONENT : // value same as FIELD
				case AnnotationTargetTypeConstants.METHOD_RETURN :
				case AnnotationTargetTypeConstants.METHOD_RECEIVER :
					break;

			}
			this.annotationContexts.add(annotationContext);
		}
		return true;
	}
	@Override
	public boolean visit(MarkerAnnotation annotation, BlockScope scope) {
		return internalVisit(annotation);
	}
	@Override
	public boolean visit(NormalAnnotation annotation, BlockScope scope) {
		return internalVisit(annotation);
	}
	@Override
	public boolean visit(SingleMemberAnnotation annotation, BlockScope scope) {
		return internalVisit(annotation);
	}
	@Override
	public boolean visit(Wildcard wildcard, BlockScope scope) {
		this.currentWildcard = wildcard;
		return true;
	}
	@Override
	public boolean visit(Argument argument, BlockScope scope) {
		if ((argument.bits & ASTNode.IsUnionType) == 0) {
			return true;
		}
		for (int i = 0, max = this.localVariable.initializationCount; i < max; i++) {
			int startPC = this.localVariable.initializationPCs[i << 1];
			int endPC = this.localVariable.initializationPCs[(i << 1) + 1];
			if (startPC != endPC) { // only entries for non zero length
				return true;
			}
		}
		return false;
	}
	@Override
	public boolean visit(Argument argument, ClassScope scope) {
		if ((argument.bits & ASTNode.IsUnionType) == 0) {
			return true;
		}
		for (int i = 0, max = this.localVariable.initializationCount; i < max; i++) {
			int startPC = this.localVariable.initializationPCs[i << 1];
			int endPC = this.localVariable.initializationPCs[(i << 1) + 1];
			if (startPC != endPC) { // only entries for non zero length
				return true;
			}
		}
		return false;
	}
	@Override
	public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
		for (int i = 0, max = this.localVariable.initializationCount; i < max; i++) {
			int startPC = this.localVariable.initializationPCs[i << 1];
			int endPC = this.localVariable.initializationPCs[(i << 1) + 1];
			if (startPC != endPC) { // only entries for non zero length
				return true;
			}
		}
		return false;
	}
	@Override
	public void endVisit(Wildcard wildcard, BlockScope scope) {
		this.currentWildcard = null;
	}
}
/*
 * Answer a base type reference (can be an array of base type).
 */
public static final TypeReference baseTypeReference(int baseType, int dim, Annotation [][] dimAnnotations) {

	if (dim == 0) {
		switch (baseType) {
			case (TypeIds.T_void) :
				return new SingleTypeReference(TypeBinding.VOID.simpleName, 0);
			case (TypeIds.T_boolean) :
				return new SingleTypeReference(TypeBinding.BOOLEAN.simpleName, 0);
			case (TypeIds.T_char) :
				return new SingleTypeReference(TypeBinding.CHAR.simpleName, 0);
			case (TypeIds.T_float) :
				return new SingleTypeReference(TypeBinding.FLOAT.simpleName, 0);
			case (TypeIds.T_double) :
				return new SingleTypeReference(TypeBinding.DOUBLE.simpleName, 0);
			case (TypeIds.T_byte) :
				return new SingleTypeReference(TypeBinding.BYTE.simpleName, 0);
			case (TypeIds.T_short) :
				return new SingleTypeReference(TypeBinding.SHORT.simpleName, 0);
			case (TypeIds.T_int) :
				return new SingleTypeReference(TypeBinding.INT.simpleName, 0);
			default : //T_long
				return new SingleTypeReference(TypeBinding.LONG.simpleName, 0);
		}
	}
	switch (baseType) {
		case (TypeIds.T_void) :
			return new ArrayTypeReference(TypeBinding.VOID.simpleName, dim, dimAnnotations, 0);
		case (TypeIds.T_boolean) :
			return new ArrayTypeReference(TypeBinding.BOOLEAN.simpleName, dim, dimAnnotations, 0);
		case (TypeIds.T_char) :
			return new ArrayTypeReference(TypeBinding.CHAR.simpleName, dim, dimAnnotations, 0);
		case (TypeIds.T_float) :
			return new ArrayTypeReference(TypeBinding.FLOAT.simpleName, dim, dimAnnotations, 0);
		case (TypeIds.T_double) :
			return new ArrayTypeReference(TypeBinding.DOUBLE.simpleName, dim, dimAnnotations, 0);
		case (TypeIds.T_byte) :
			return new ArrayTypeReference(TypeBinding.BYTE.simpleName, dim, dimAnnotations, 0);
		case (TypeIds.T_short) :
			return new ArrayTypeReference(TypeBinding.SHORT.simpleName, dim, dimAnnotations, 0);
		case (TypeIds.T_int) :
			return new ArrayTypeReference(TypeBinding.INT.simpleName, dim, dimAnnotations, 0);
		default : //T_long
			return new ArrayTypeReference(TypeBinding.LONG.simpleName, dim, dimAnnotations, 0);
	}
}

public static final TypeReference baseTypeReference(int baseType, int dim) {
	return baseTypeReference(baseType, dim, null);
}

// JSR308 type annotations...
public Annotation[][] annotations = null;

// allows us to trap completion & selection nodes
public void aboutToResolve(Scope scope) {
	// default implementation: do nothing
}
@Override
public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	return flowInfo;
}
public void checkBounds(Scope scope) {
	// only parameterized type references have bounds
}
public abstract TypeReference augmentTypeWithAdditionalDimensions(int additionalDimensions, Annotation[][] additionalAnnotations, boolean isVarargs);

protected Annotation[][] getMergedAnnotationsOnDimensions(int additionalDimensions, Annotation[][] additionalAnnotations) {
	/* Note, we actually concatenate the additional annotations after base annotations, in bindings, they should appear before base annotations.
	   Given @English int @Nullable [] x @NonNull []; the type x is a @NonNull arrays of of @Nullable arrays of @English Strings, not the other
	   way about. Changing this in the compiler AST representation will cause too many ripples, so we leave it as is. On the bindings, the type
	   will reflect rotated (i.e will reflect correctly). See AnnotatableTypeSystem.flattenedAnnotations
	*/
	Annotation[][] annotationsOnDimensions = this.getAnnotationsOnDimensions(true);
	int dimensions = this.dimensions();

	if (annotationsOnDimensions == null && additionalAnnotations == null)
		return null;

	final int totalDimensions = dimensions + additionalDimensions;
	Annotation [][] mergedAnnotations = new Annotation[totalDimensions][];
	if (annotationsOnDimensions != null) {
		System.arraycopy(annotationsOnDimensions, 0, mergedAnnotations, 0, dimensions);
	}
	if (additionalAnnotations != null) {
		for (int i = dimensions, j = 0; i < totalDimensions; i++, j++) {
			mergedAnnotations[i] = additionalAnnotations[j];
		}
	}
	return mergedAnnotations;
}

public int dimensions() {
	return 0;
}


/**
 * This method is used to return the array dimension declared after the
 * name of a local or a field declaration.
 * For example:
 *    int i, j[] = null, k[][] = {{}};
 *    It should return 0 for i, 1 for j and 2 for k.
 * @return int the extra dimension found
 */
public int extraDimensions() {
	return 0;
}

public AnnotationContext[] getAllAnnotationContexts(int targetType) {
	List<AnnotationContext> allAnnotationContexts = new ArrayList<>();
	AnnotationCollector collector = new AnnotationCollector(this, targetType, allAnnotationContexts);
	this.traverse(collector, (BlockScope) null);
	return allAnnotationContexts.toArray(new AnnotationContext[allAnnotationContexts.size()]);
}
/**
 * info can be either a type index (superclass/superinterfaces) or a pc into the bytecode
 * @param targetType
 * @param info
 * @param allAnnotationContexts
 */
public void getAllAnnotationContexts(int targetType, int info, List<AnnotationContext> allAnnotationContexts) {
	AnnotationCollector collector = new AnnotationCollector(this, targetType, info, allAnnotationContexts);
	this.traverse(collector, (BlockScope) null);
}
public void getAllAnnotationContexts(int targetType, int info, List<AnnotationContext> allAnnotationContexts, Annotation [] se7Annotations) {
	AnnotationCollector collector = new AnnotationCollector(this, targetType, info, allAnnotationContexts);
	for (int i = 0, length = se7Annotations == null ? 0 : se7Annotations.length; i < length; i++) {
		Annotation annotation = se7Annotations[i];
		annotation.traverse(collector, (BlockScope) null);
	}
	this.traverse(collector, (BlockScope) null);
}
/**
 * info can be either a type index (superclass/superinterfaces) or a pc into the bytecode
 */
public void getAllAnnotationContexts(int targetType, int info, List<AnnotationContext> allAnnotationContexts, Annotation[][] annotationsOnDimensions, int dimensions) {
	AnnotationCollector collector = new AnnotationCollector(this, targetType, info, allAnnotationContexts, annotationsOnDimensions, dimensions);
	this.traverse(collector, (BlockScope) null);
	if (annotationsOnDimensions != null) {
		for (int i = 0, max = annotationsOnDimensions.length; i < max; i++) {
			Annotation[] annotationsOnDimension = annotationsOnDimensions[i];
			if (annotationsOnDimension != null) {
				for (int j = 0, max2 = annotationsOnDimension.length; j< max2; j++) {
					annotationsOnDimension[j].traverse(collector, (BlockScope) null);
				}
			}
		}
	}
}
public void getAllAnnotationContexts(int targetType, int info, int typeIndex, List<AnnotationContext> allAnnotationContexts) {
	AnnotationCollector collector = new AnnotationCollector(this, targetType, info, typeIndex, allAnnotationContexts);
	this.traverse(collector, (BlockScope) null);
}
public void getAllAnnotationContexts(int targetType, List<AnnotationContext> allAnnotationContexts) {
	AnnotationCollector collector = new AnnotationCollector(this, targetType, allAnnotationContexts);
	this.traverse(collector, (BlockScope) null);
}
public Annotation[][] getAnnotationsOnDimensions() {
	return getAnnotationsOnDimensions(false);
}

public TypeReference [][] getTypeArguments() {
	return null;
}
/**
 * @param useSourceOrder if true annotations on dimensions are returned in source order, otherwise they are returned per
 * how they ought to be interpreted by a type system, or external persistence view. For example, given the following:
 * int @Nullable [] f @NonNull [] ==> f is really a @NonNull array of @Nullable arrays of ints. This is the type system
 * view since extended dimensions bind more readily than type components that precede the identifier. This is how it ought
 * to be encoded in bindings and how it ought to be persisted in class files. However for DOM/AST construction, we need the
 * dimensions in source order, so we provide a way for the clients to ask what they want.
 *
 */
public Annotation[][] getAnnotationsOnDimensions(boolean useSourceOrder) {
	return null;
}

public void setAnnotationsOnDimensions(Annotation [][] annotationsOnDimensions) {
	// nothing to do. Subtypes should react suitably.
}

public abstract char[] getLastToken();

/**
 * @return char[][]
 * TODO (jerome) should merge back into #getTypeName()
 */
public char [][] getParameterizedTypeName(){
	return getTypeName();
}
protected abstract TypeBinding getTypeBinding(Scope scope);
/**
 * @return char[][]
 */
public abstract char [][] getTypeName() ;

protected TypeBinding internalResolveType(Scope scope, int location) {
	// handle the error here
	this.constant = Constant.NotAConstant;
	if (this.resolvedType != null) { // is a shared type reference which was already resolved
		if (this.resolvedType.isValidBinding()) {
			return this.resolvedType;
		} else {
			switch (this.resolvedType.problemId()) {
				case ProblemReasons.NotFound :
				case ProblemReasons.NotVisible :
				case ProblemReasons.InheritedNameHidesEnclosingName :
					TypeBinding type = this.resolvedType.closestMatch();
					if (type == null) return null;
					return scope.environment().convertToRawType(type, false /*do not force conversion of enclosing types*/);
				default :
					return null;
			}
		}
	}
	boolean hasError = false;
	TypeBinding type = this.resolvedType = getTypeBinding(scope);
	if (type == null) {
		return null; // detected cycle while resolving hierarchy
	} else if ((hasError = !type.isValidBinding()) == true) {
		if (this.isTypeNameVar(scope)) {
			reportVarIsNotAllowedHere(scope);
		} else if (!scope.problemReporter().validateRestrictedKeywords(getLastToken(), this)) {
			reportInvalidType(scope);
		}
		switch (type.problemId()) {
			case ProblemReasons.NotFound :
			case ProblemReasons.NotVisible :
			case ProblemReasons.InheritedNameHidesEnclosingName :
				type = type.closestMatch();
				if (type == null) return null;
				break;
			default :
				return null;
		}
	} else { // check anyway - to cover a illegally declared "permits" type
		scope.problemReporter().validateRestrictedKeywords(getLastToken(), this);
	}
	if (type.isArrayType() && ((ArrayBinding) type).leafComponentType == TypeBinding.VOID) {
		scope.problemReporter().cannotAllocateVoidArray(this);
		return null;
	}
	if (!(this instanceof QualifiedTypeReference)   // QualifiedTypeReference#getTypeBinding called above will have already checked deprecation
			&& isTypeUseDeprecated(type, scope)) {
		reportDeprecatedType(type, scope);
	}
	type = scope.environment().convertToRawType(type, false /*do not force conversion of enclosing types*/);
	if (type.leafComponentType().isRawType()
			&& (this.bits & ASTNode.IgnoreRawTypeCheck) == 0
			&& scope.compilerOptions().getSeverity(CompilerOptions.RawTypeReference) != ProblemSeverities.Ignore) {
		scope.problemReporter().rawTypeReference(this, type);
	}
	if (hasError) {
		resolveAnnotations(scope, 0); // don't apply null defaults to buggy type
		return type;
	} else {
		// store the computed type only if no error, otherwise keep the problem type instead
		this.resolvedType = type;
		resolveAnnotations(scope, location);
		return this.resolvedType; // pick up value that may have been changed in resolveAnnotations(..)
	}
}
@Override
public boolean isTypeReference() {
	return true;
}
public boolean isWildcard() {
	return false;
}
public boolean isUnionType() {
	return false;
}
public boolean isVarargs() {
	return (this.bits & ASTNode.IsVarArgs) != 0;
}
public boolean isParameterizedTypeReference() {
	return false;
}
protected void reportDeprecatedType(TypeBinding type, Scope scope, int index) {
	scope.problemReporter().deprecatedType(type, this, index);
}

protected void reportDeprecatedType(TypeBinding type, Scope scope) {
	scope.problemReporter().deprecatedType(type, this, Integer.MAX_VALUE);
}

protected void reportInvalidType(Scope scope) {
	scope.problemReporter().invalidType(this, this.resolvedType);
}

protected void reportVarIsNotAllowedHere(Scope scope) {
	scope.problemReporter().varIsNotAllowedHere(this);
}

public TypeBinding resolveSuperType(ClassScope scope) {
	// assumes the implementation of resolveType(ClassScope) will call back to detect cycles
	TypeBinding superType = resolveType(scope);
	if (superType == null) return null;

	if (superType.isTypeVariable()) {
		if (this.resolvedType.isValidBinding()) {
			this.resolvedType = new ProblemReferenceBinding(getTypeName(), (ReferenceBinding)this.resolvedType, ProblemReasons.IllegalSuperTypeVariable);
			reportInvalidType(scope);
		}
		return null;
	}
	return superType;
}

@Override
public final TypeBinding resolveType(BlockScope blockScope) {
	return resolveType(blockScope, true /* checkbounds if any */);
}

public TypeBinding resolveType(BlockScope scope, boolean checkBounds) {
	return resolveType(scope, checkBounds, 0);
}

public TypeBinding resolveType(BlockScope scope, boolean checkBounds, int location) {
	return internalResolveType(scope, location);
}

@Override
public TypeBinding resolveType(ClassScope scope) {
	return resolveType(scope, 0);
}

public TypeBinding resolveType(ClassScope scope, int location) {
	return internalResolveType(scope, location);
}

public TypeBinding resolveTypeArgument(BlockScope blockScope, ReferenceBinding genericType, int rank) {
    return resolveType(blockScope, true /* check bounds*/, Binding.DefaultLocationTypeArgument);
}

public TypeBinding resolveTypeArgument(ClassScope classScope, ReferenceBinding genericType, int rank) {
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=294057, circularity is allowed when we are
	// resolving type arguments i.e interface A<T extends C> {}	interface B extends A<D> {}
	// interface D extends C {}	interface C extends B {}
	ReferenceBinding ref = classScope.referenceContext.binding;
	boolean pauseHierarchyCheck = false;
	try {
		if (ref.isHierarchyBeingConnected()) {
			pauseHierarchyCheck = (ref.tagBits & TagBits.PauseHierarchyCheck) == 0;
			ref.tagBits |= TagBits.PauseHierarchyCheck;
		}
	    return resolveType(classScope, Binding.DefaultLocationTypeArgument);
	} finally {
		if (pauseHierarchyCheck) {
			ref.tagBits &= ~TagBits.PauseHierarchyCheck;
		}
	}
}

@Override
public abstract void traverse(ASTVisitor visitor, BlockScope scope);

@Override
public abstract void traverse(ASTVisitor visitor, ClassScope scope);

protected void resolveAnnotations(Scope scope, int location) {
	Annotation[][] annotationsOnDimensions = getAnnotationsOnDimensions();
	if (this.annotations != null || annotationsOnDimensions != null) {
		BlockScope resolutionScope = Scope.typeAnnotationsResolutionScope(scope);
		if (resolutionScope != null) {
			int dimensions = this.dimensions();
			if (this.annotations != null) {
				TypeBinding leafComponentType = this.resolvedType.leafComponentType();
				leafComponentType = resolveAnnotations(resolutionScope, this.annotations, leafComponentType);
				this.resolvedType = dimensions > 0 ? scope.environment().createArrayType(leafComponentType, dimensions) : leafComponentType;
				// contradictory null annotations on the type are already detected in Annotation.resolveType() (SE7 treatment)
			}
			if (annotationsOnDimensions != null) {
				this.resolvedType = resolveAnnotations(resolutionScope, annotationsOnDimensions, this.resolvedType);
				if (this.resolvedType instanceof ArrayBinding) {
					long[] nullTagBitsPerDimension = ((ArrayBinding)this.resolvedType).nullTagBitsPerDimension;
					if (nullTagBitsPerDimension != null) {
						for (int i = 0; i < dimensions; i++) { // skip last annotations at [dimensions] (concerns the leaf type)
							if ((nullTagBitsPerDimension[i] & TagBits.AnnotationNullMASK) == TagBits.AnnotationNullMASK) {
								scope.problemReporter().contradictoryNullAnnotations(annotationsOnDimensions[i]);
								nullTagBitsPerDimension[i] = 0;
							}
						}
					}
				}
			}
		}
	}
	if (scope.compilerOptions().isAnnotationBasedNullAnalysisEnabled
			&& this.resolvedType != null
			&& (this.resolvedType.tagBits & TagBits.AnnotationNullMASK) == 0
			&& !this.resolvedType.isTypeVariable()
			&& !this.resolvedType.isWildcard()
			&& location != 0
			&& scope.hasDefaultNullnessFor(location, this.sourceStart))
	{
		if (location == Binding.DefaultLocationTypeBound && this.resolvedType.id == TypeIds.T_JavaLangObject) {
			scope.problemReporter().implicitObjectBoundNoNullDefault(this);
		} else {
			LookupEnvironment environment = scope.environment();
			AnnotationBinding[] annots = new AnnotationBinding[]{environment.getNonNullAnnotation()};
			this.resolvedType = environment.createAnnotatedType(this.resolvedType, annots);
		}
	}
}
public int getAnnotatableLevels() {
	return 1;
}
/** Check all typeArguments for illegal null annotations on base types. */
protected void checkIllegalNullAnnotations(Scope scope, TypeReference[] typeArguments) {
	if (scope.environment().usesNullTypeAnnotations() && typeArguments != null) {
		for (int i = 0; i < typeArguments.length; i++) {
			TypeReference arg = typeArguments[i];
			if (arg.resolvedType != null)
				arg.checkIllegalNullAnnotation(scope);
		}
	}
}
/** Check whether this type reference conforms to the null constraints defined for the corresponding type variable. */
protected void checkNullConstraints(Scope scope, Substitution substitution, TypeBinding[] variables, int rank) {
	if (variables != null && variables.length > rank) {
		TypeBinding variable = variables[rank];
		if (variable.hasNullTypeAnnotations()) {
			if (NullAnnotationMatching.analyse(variable, this.resolvedType, null, substitution, -1, null, CheckMode.BOUND_CHECK).isAnyMismatch())
				scope.problemReporter().nullityMismatchTypeArgument(variable, this.resolvedType, this);
    	}
	}
	checkIllegalNullAnnotation(scope);
}
protected void checkIllegalNullAnnotation(Scope scope) {
	if (this.resolvedType.leafComponentType().isBaseType() && hasNullTypeAnnotation(AnnotationPosition.LEAF_TYPE))
		scope.problemReporter().illegalAnnotationForBaseType(this, this.annotations[0], this.resolvedType.tagBits & TagBits.AnnotationNullMASK);
}
/** Retrieve the null annotation that has been translated to the given nullTagBits. */
public Annotation findAnnotation(long nullTagBits) {
	if (this.annotations != null) {
		Annotation[] innerAnnotations = this.annotations[this.annotations.length-1];
		if (innerAnnotations != null) {
			int annBit = nullTagBits == TagBits.AnnotationNonNull ? TypeIds.BitNonNullAnnotation : TypeIds.BitNullableAnnotation;
			for (int i = 0; i < innerAnnotations.length; i++) {
				if (innerAnnotations[i] != null && innerAnnotations[i].hasNullBit(annBit))
					return innerAnnotations[i];
			}
		}
	}
	return null;
}
public boolean hasNullTypeAnnotation(AnnotationPosition position) {
	if (this.annotations != null) {
		if (position == AnnotationPosition.MAIN_TYPE) {
			Annotation[] innerAnnotations = this.annotations[this.annotations.length-1];
			return containsNullAnnotation(innerAnnotations);
		} else {
			for (Annotation[] someAnnotations: this.annotations) {
				if (containsNullAnnotation(someAnnotations))
					return true;
			}
		}
	}
	return false;
}
public static boolean containsNullAnnotation(Annotation[] annotations) {
	if (annotations != null) {
		for (int i = 0; i < annotations.length; i++) {
			if (annotations[i] != null && (annotations[i].hasNullBit(TypeIds.BitNonNullAnnotation|TypeIds.BitNullableAnnotation)))
				return true;
		}
	}
	return false;
}
public TypeReference[] getTypeReferences() {
	return new TypeReference [] { this };
}

public boolean isBaseTypeReference() {
	return false;
}
private char[] getTypeName(int index) {
	char[][] typeName = this.getTypeName();
	return typeName != null && typeName.length > index ? typeName[index] :
		CharOperation.NO_CHAR;
}
/**
 * Checks to see if the declaration uses 'var' as type name
 * @param scope Relevant scope, for error reporting
 * @return true, if source level is Java 10 or above and the type name is just 'var', false otherwise
 */
public boolean isTypeNameVar(Scope scope) {
	CompilerOptions compilerOptions = scope != null ? scope.compilerOptions() : null;
	if (compilerOptions != null && compilerOptions.sourceLevel < ClassFileConstants.JDK10) {
		return false;
	}
	return CharOperation.equals(getTypeName(0), TypeConstants.VAR);
}
}
