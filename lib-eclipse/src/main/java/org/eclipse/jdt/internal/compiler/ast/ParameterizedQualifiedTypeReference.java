/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Stephan Herrmann - Contributions for
 *								bug 342671 - ClassCastException: org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding cannot be cast to org.eclipse.jdt.internal.compiler.lookup.ArrayBinding
 *								bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								Bug 415043 - [1.8][null] Follow-up re null type annotations after bug 392099
 *								Bug 416181 - [1.8][compiler][null] Invalid assignment is not rejected by the compiler
 *								Bug 429958 - [1.8][null] evaluate new DefaultLocation attribute of @NonNullByDefault
 *								Bug 434600 - Incorrect null analysis error reporting on type parameters
 *								Bug 435570 - [1.8][null] @NonNullByDefault illegally tries to affect "throws E"
 *								Bug 456508 - Unexpected RHS PolyTypeBinding for: <code-snippet>
 *								Bug 466713 - Null Annotations: NullPointerException using <int @Nullable []> as Type Param
 *        Andy Clement - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.*;

/**
 * Syntactic representation of a reference to a generic type.
 * Note that it might also have a dimension.
 */
public class ParameterizedQualifiedTypeReference extends ArrayQualifiedTypeReference {

	public TypeReference[][] typeArguments;
	ReferenceBinding[] typesPerToken;

	/**
	 * @param tokens
	 * @param dim
	 * @param positions
	 */
	public ParameterizedQualifiedTypeReference(char[][] tokens, TypeReference[][] typeArguments, int dim, long[] positions) {

		super(tokens, dim, positions);
		this.typeArguments = typeArguments;
		annotationSearch: for (int i = 0, max = typeArguments.length; i < max; i++) {
			TypeReference[] typeArgumentsOnTypeComponent = typeArguments[i];
			if (typeArgumentsOnTypeComponent != null) {
				for (int j = 0, max2 = typeArgumentsOnTypeComponent.length; j < max2; j++) {
					if ((typeArgumentsOnTypeComponent[j].bits & ASTNode.HasTypeAnnotations) != 0) {
						this.bits |= ASTNode.HasTypeAnnotations;
						break annotationSearch;
					}
				}
			}
		}
	}
	public ParameterizedQualifiedTypeReference(char[][] tokens, TypeReference[][] typeArguments, int dim, Annotation[][] annotationsOnDimensions, long[] positions) {
		this(tokens, typeArguments, dim, positions);
		setAnnotationsOnDimensions(annotationsOnDimensions);
		if (annotationsOnDimensions != null) {
			this.bits |= ASTNode.HasTypeAnnotations;
		}
	}
	@Override
	public void checkBounds(Scope scope) {
		if (this.resolvedType == null || !this.resolvedType.isValidBinding()) return;

		checkBounds(
			(ReferenceBinding) this.resolvedType.leafComponentType(),
			scope,
			this.typeArguments.length - 1);
	}
	public void checkBounds(ReferenceBinding type, Scope scope, int index) {
		// recurse on enclosing type if any, and assuming explictly  part of the reference (index>0)
		if (index > 0) {
			ReferenceBinding enclosingType = this.typesPerToken[index-1];
			if (enclosingType != null)
				checkBounds(enclosingType, scope, index - 1);
		}
		if (type.isParameterizedTypeWithActualArguments()) {
			ParameterizedTypeBinding parameterizedType = (ParameterizedTypeBinding) type;
			ReferenceBinding currentType = parameterizedType.genericType();
			TypeVariableBinding[] typeVariables = currentType.typeVariables();
			if (typeVariables != null) { // argTypes may be null in error cases
				parameterizedType.boundCheck(scope, this.typeArguments[index]);
			}
		}
	}
	@Override
	public TypeReference augmentTypeWithAdditionalDimensions(int additionalDimensions, Annotation[][] additionalAnnotations, boolean isVarargs) {
		int totalDimensions = this.dimensions() + additionalDimensions;
		Annotation [][] allAnnotations = getMergedAnnotationsOnDimensions(additionalDimensions, additionalAnnotations);
		ParameterizedQualifiedTypeReference pqtr = new ParameterizedQualifiedTypeReference(this.tokens, this.typeArguments, totalDimensions, allAnnotations, this.sourcePositions);
		pqtr.annotations = this.annotations;
		pqtr.bits |= (this.bits & ASTNode.HasTypeAnnotations);
		if (!isVarargs)
			pqtr.extendedDimensions = additionalDimensions;
		return pqtr;
	}
	@Override
	public boolean isParameterizedTypeReference() {
		return true;
	}

	@Override
    public boolean hasNullTypeAnnotation(AnnotationPosition position) {
		if (super.hasNullTypeAnnotation(position))
			return true;
		if (position == AnnotationPosition.ANY) {
	    	if (this.resolvedType != null && !this.resolvedType.hasNullTypeAnnotations())
	    		return false; // shortcut
	    	if (this.typeArguments != null) {
	    		for (int i = 0; i < this.typeArguments.length; i++) {
	    			TypeReference[] arguments = this.typeArguments[i];
	    			if (arguments != null) {
		    			for (int j = 0; j < arguments.length; j++) {
		    				if (arguments[j].hasNullTypeAnnotation(position))
		    					return true;
		    			}
					}
				}
	    	}
		}
    	return false;
    }

	/**
	 * @return char[][]
	 */
	@Override
	public char [][] getParameterizedTypeName(){
		int length = this.tokens.length;
		char[][] qParamName = new char[length][];
		for (int i = 0; i < length; i++) {
			TypeReference[] arguments = this.typeArguments[i];
			if (arguments == null) {
				qParamName[i] = this.tokens[i];
			} else {
				StringBuilder buffer = new StringBuilder(5);
				buffer.append(this.tokens[i]);
				buffer.append('<');
				for (int j = 0, argLength =arguments.length; j < argLength; j++) {
					if (j > 0) buffer.append(',');
					buffer.append(CharOperation.concatWith(arguments[j].getParameterizedTypeName(), '.'));
				}
				buffer.append('>');
				int nameLength = buffer.length();
				qParamName[i] = new char[nameLength];
				buffer.getChars(0, nameLength, qParamName[i], 0);
			}
		}
		int dim = this.dimensions;
		if (dim > 0) {
			char[] dimChars = new char[dim*2];
			for (int i = 0; i < dim; i++) {
				int index = i*2;
				dimChars[index] = '[';
				dimChars[index+1] = ']';
			}
			qParamName[length-1] = CharOperation.concat(qParamName[length-1], dimChars);
		}
		return qParamName;
	}

	@Override
	public TypeReference[][] getTypeArguments() {
		return this.typeArguments;
	}

    @Override
	protected TypeBinding getTypeBinding(Scope scope) {
        return null; // not supported here - combined with resolveType(...)
    }

    /*
     * No need to check for reference to raw type per construction
     */
	private TypeBinding internalResolveType(Scope scope, boolean checkBounds, int location) {
		// handle the error here
		this.constant = Constant.NotAConstant;
		if ((this.bits & ASTNode.DidResolve) != 0) { // is a shared type reference which was already resolved
			if (this.resolvedType != null) { // is a shared type reference which was already resolved
				if (this.resolvedType.isValidBinding()) {
					return this.resolvedType;
				} else {
					switch (this.resolvedType.problemId()) {
						case ProblemReasons.NotFound :
						case ProblemReasons.NotVisible :
						case ProblemReasons.InheritedNameHidesEnclosingName :
							TypeBinding type = this.resolvedType.closestMatch();
							return type;
						default :
							return null;
					}
				}
			}
		}
		this.bits |= ASTNode.DidResolve;
		TypeBinding type = internalResolveLeafType(scope, checkBounds);
		createArrayType(scope);
		resolveAnnotations(scope, location);
		if(this.dimensions > 0) {
			this.resolvedType = ArrayTypeReference.maybeMarkArrayContentsNonNull(scope, this.resolvedType, this.sourceStart, this.dimensions, null);
		}

		if (this.typeArguments != null)
			// relevant null annotations are on the inner most type:
			checkIllegalNullAnnotations(scope, this.typeArguments[this.typeArguments.length-1]);
		return type == null ? type : this.resolvedType;
	}
	private TypeBinding internalResolveLeafType(Scope scope, boolean checkBounds) {
		boolean isClassScope = scope.kind == Scope.CLASS_SCOPE;
		Binding binding = scope.getPackage(this.tokens);
		if (binding != null && !binding.isValidBinding()) {
			this.resolvedType = (ReferenceBinding) binding;
			reportInvalidType(scope);
			// be resilient, still attempt resolving arguments
			for (int i = 0, max = this.tokens.length; i < max; i++) {
				TypeReference[] args = this.typeArguments[i];
				if (args != null) {
					int argLength = args.length;
					for (int j = 0; j < argLength; j++) {
						TypeReference typeArgument = args[j];
						if (isClassScope) {
							typeArgument.resolveType((ClassScope) scope);
						} else {
							typeArgument.resolveType((BlockScope) scope, checkBounds);
						}
					}
				}
			}
			return null;
		}

		PackageBinding packageBinding = binding == null ? null : (PackageBinding) binding;
		rejectAnnotationsOnPackageQualifiers(scope, packageBinding);

		boolean typeIsConsistent = true;
		ReferenceBinding qualifyingType = null;
		int max = this.tokens.length;
		this.typesPerToken = new ReferenceBinding[max];
		for (int i = packageBinding == null ? 0 : packageBinding.compoundName.length; i < max; i++) {
			findNextTypeBinding(i, scope, packageBinding);
			if (!(this.resolvedType.isValidBinding())) {
				reportInvalidType(scope);
				// be resilient, still attempt resolving arguments
				for (int j = i; j < max; j++) {
				    TypeReference[] args = this.typeArguments[j];
				    if (args != null) {
						int argLength = args.length;
						for (int k = 0; k < argLength; k++) {
						    TypeReference typeArgument = args[k];
						    if (isClassScope) {
						    	typeArgument.resolveType((ClassScope) scope);
						    } else {
						    	typeArgument.resolveType((BlockScope) scope);
						    }
						}
				    }
				}
				return null;
			}
			ReferenceBinding currentType = (ReferenceBinding) this.resolvedType;
			if (qualifyingType == null) {
				qualifyingType = currentType.enclosingType(); // if member type
				if (qualifyingType != null && currentType.hasEnclosingInstanceContext()) {
					qualifyingType = scope.environment().convertToParameterizedType(qualifyingType);
				}
			} else {
				if (this.annotations != null)
					rejectAnnotationsOnStaticMemberQualififer(scope, currentType, this.annotations[i-1]);
				if (typeIsConsistent && currentType.isStatic()
						&& (qualifyingType.isParameterizedTypeWithActualArguments() || qualifyingType.isGenericType())) {
					scope.problemReporter().staticMemberOfParameterizedType(this, currentType, qualifyingType, i);
					typeIsConsistent = false;
					qualifyingType = qualifyingType.actualType(); // avoid raw/parameterized enclosing of static member
				}
				ReferenceBinding enclosingType = currentType.enclosingType();
				if (enclosingType != null && TypeBinding.notEquals(enclosingType.erasure(), qualifyingType.erasure())) { // qualifier != declaring/enclosing
					qualifyingType = enclosingType; // inherited member type, leave it associated with its enclosing rather than subtype
				}
			}

			// check generic and arity
		    TypeReference[] args = this.typeArguments[i];
		    if (args != null) {
			    TypeReference keep = null;
			    if (isClassScope) {
			    	keep = ((ClassScope) scope).superTypeReference;
			    	((ClassScope) scope).superTypeReference = null;
			    }
				int argLength = args.length;
				boolean isDiamond = argLength == 0 && (i == (max -1)) && ((this.bits & ASTNode.IsDiamond) != 0);
				TypeBinding[] argTypes = new TypeBinding[argLength];
				boolean argHasError = false;
				ReferenceBinding currentOriginal = (ReferenceBinding)currentType.original();
				for (int j = 0; j < argLength; j++) {
				    TypeReference arg = args[j];
				    TypeBinding argType = isClassScope
						? arg.resolveTypeArgument((ClassScope) scope, currentOriginal, j)
						: arg.resolveTypeArgument((BlockScope) scope, currentOriginal, j);
					if (argType == null) {
						argHasError = true;
					} else {
						argTypes[j] = argType;
					}
				}
				if (argHasError) {
					return null;
				}
				if (isClassScope) {
					((ClassScope) scope).superTypeReference = keep;
					if (((ClassScope) scope).detectHierarchyCycle(currentOriginal, this))
						return null;
				}

			    TypeVariableBinding[] typeVariables = currentOriginal.typeVariables();
				if (typeVariables == Binding.NO_TYPE_VARIABLES) { // check generic
					if (scope.compilerOptions().originalSourceLevel >= ClassFileConstants.JDK1_5) { // below 1.5, already reported as syntax error
						scope.problemReporter().nonGenericTypeCannotBeParameterized(i, this, currentType, argTypes);
						return null;
					}
					this.resolvedType =  (qualifyingType != null && qualifyingType.isParameterizedType())
						? scope.environment().createParameterizedType(currentOriginal, null, qualifyingType)
						: currentType;
					return this.resolvedType;
				} else if (argLength != typeVariables.length) {
					if (!isDiamond) { // check arity
						scope.problemReporter().incorrectArityForParameterizedType(this, currentType, argTypes, i);
						return null;
					}
				}
				// check parameterizing (non-)static member type of raw type
				if (typeIsConsistent) {
					if (!currentType.hasEnclosingInstanceContext()) {
						if (qualifyingType != null && qualifyingType.isRawType())
							this.typesPerToken[i-1] = qualifyingType = qualifyingType.actualType(); // revert rawification of enclosing, since its generics are inaccessible
					} else {
						ReferenceBinding actualEnclosing = currentType.enclosingType();
						if (actualEnclosing != null && actualEnclosing.isRawType()) {
							scope.problemReporter().rawMemberTypeCannotBeParameterized(
									this, scope.environment().createRawType(currentOriginal, actualEnclosing), argTypes);
							typeIsConsistent = false;
						}
					}
				}
				ParameterizedTypeBinding parameterizedType = scope.environment().createParameterizedType(currentOriginal, argTypes, qualifyingType);
				// check argument type compatibility for non <> cases - <> case needs no bounds check, we will scream foul if needed during inference.
				if (!isDiamond) {
					if (checkBounds) // otherwise will do it in Scope.connectTypeVariables() or generic method resolution
						parameterizedType.boundCheck(scope, args);
					else
						scope.deferBoundCheck(this);
				} else {
		    		parameterizedType.arguments = ParameterizedSingleTypeReference.DIAMOND_TYPE_ARGUMENTS;
				}
				qualifyingType = parameterizedType;
		    } else {
				ReferenceBinding currentOriginal = (ReferenceBinding)currentType.original();
				if (isClassScope)
					if (((ClassScope) scope).detectHierarchyCycle(currentOriginal, this))
						return null;
				if (currentOriginal.isGenericType()) {
	   			    if (typeIsConsistent && qualifyingType != null && qualifyingType.isParameterizedType() && currentOriginal.hasEnclosingInstanceContext()) {
						scope.problemReporter().parameterizedMemberTypeMissingArguments(this, scope.environment().createParameterizedType(currentOriginal, null, qualifyingType), i);
						typeIsConsistent = false;
					}
	   			    qualifyingType = scope.environment().createRawType(currentOriginal, qualifyingType); // raw type
				} else {
					qualifyingType = scope.environment().maybeCreateParameterizedType(currentOriginal, qualifyingType);
				}
			}
			if (isTypeUseDeprecated(qualifyingType, scope))
				reportDeprecatedType(qualifyingType, scope, i);
			this.resolvedType = qualifyingType;
			this.typesPerToken[i] = qualifyingType;
			recordResolution(scope.environment(), this.resolvedType);
		}
		return this.resolvedType;
	}
	private void createArrayType(Scope scope) {
		if (this.dimensions > 0) {
			if (this.dimensions > 255)
				scope.problemReporter().tooManyDimensions(this);
			this.resolvedType = scope.createArrayType(this.resolvedType, this.dimensions);
		}
	}

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output) {
		int length = this.tokens.length;
		for (int i = 0; i < length - 1; i++) {
			if (this.annotations != null && this.annotations[i] != null) {
				printAnnotations(this.annotations[i], output);
				output.append(' ');
			}
			output.append(this.tokens[i]);
			TypeReference[] typeArgument = this.typeArguments[i];
			if (typeArgument != null) {
				output.append('<');
				int typeArgumentLength = typeArgument.length;
				if (typeArgumentLength > 0) {
					int max = typeArgumentLength - 1;
					for (int j = 0; j < max; j++) {
						typeArgument[j].print(0, output);
						output.append(", ");//$NON-NLS-1$
					}
					typeArgument[max].print(0, output);
				}
				output.append('>');
			}
			output.append('.');
		}
		if (this.annotations != null && this.annotations[length - 1] != null) {
			output.append(" "); //$NON-NLS-1$
			printAnnotations(this.annotations[length - 1], output);
			output.append(' ');
		}
		output.append(this.tokens[length - 1]);
		TypeReference[] typeArgument = this.typeArguments[length - 1];
		if (typeArgument != null) {
			output.append('<');
			int typeArgumentLength = typeArgument.length;
			if (typeArgumentLength > 0) {
				int max = typeArgumentLength - 1;
				for (int j = 0; j < max; j++) {
					typeArgument[j].print(0, output);
					output.append(", ");//$NON-NLS-1$
				}
				typeArgument[max].print(0, output);
			}
			output.append('>');
		}
		Annotation [][] annotationsOnDimensions = this.getAnnotationsOnDimensions();
		if ((this.bits & IsVarArgs) != 0) {
			for (int i= 0 ; i < this.dimensions - 1; i++) {
				if (annotationsOnDimensions != null && annotationsOnDimensions[i] != null) {
					output.append(" "); //$NON-NLS-1$
					printAnnotations(annotationsOnDimensions[i], output);
					output.append(" "); //$NON-NLS-1$
				}
				output.append("[]"); //$NON-NLS-1$
			}
			if (annotationsOnDimensions != null && annotationsOnDimensions[this.dimensions - 1] != null) {
				output.append(" "); //$NON-NLS-1$
				printAnnotations(annotationsOnDimensions[this.dimensions - 1], output);
				output.append(" "); //$NON-NLS-1$
			}
			output.append("..."); //$NON-NLS-1$
		} else {
			for (int i= 0 ; i < this.dimensions; i++) {
				if (annotationsOnDimensions != null && annotationsOnDimensions[i] != null) {
					output.append(" "); //$NON-NLS-1$
					printAnnotations(annotationsOnDimensions[i], output);
					output.append(" "); //$NON-NLS-1$
				}
				output.append("[]"); //$NON-NLS-1$
			}
		}
		return output;
	}

	@Override
	public TypeBinding resolveType(BlockScope scope, boolean checkBounds, int location) {
	    return internalResolveType(scope, checkBounds, location);
	}
	@Override
	public TypeBinding resolveType(ClassScope scope, int location) {
	    return internalResolveType(scope, false, location);
	}
	@Override
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.annotations != null) {
				int annotationsLevels = this.annotations.length;
				for (int i = 0; i < annotationsLevels; i++) {
					int annotationsLength = this.annotations[i] == null ? 0 : this.annotations[i].length;
					for (int j = 0; j < annotationsLength; j++)
						this.annotations[i][j].traverse(visitor, scope);
				}
			}
			Annotation [][] annotationsOnDimensions = getAnnotationsOnDimensions(true);
			if (annotationsOnDimensions != null) {
				for (int i = 0, max = annotationsOnDimensions.length; i < max; i++) {
					Annotation[] annotations2 = annotationsOnDimensions[i];
					for (int j = 0, max2 = annotations2 == null ? 0 : annotations2.length; j < max2; j++) {
						Annotation annotation = annotations2[j];
						annotation.traverse(visitor, scope);
					}
				}
			}
			for (int i = 0, max = this.typeArguments.length; i < max; i++) {
				if (this.typeArguments[i] != null) {
					for (int j = 0, max2 = this.typeArguments[i].length; j < max2; j++) {
						this.typeArguments[i][j].traverse(visitor, scope);
					}
				}
			}
		}
		visitor.endVisit(this, scope);
	}

	@Override
	public void traverse(ASTVisitor visitor, ClassScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.annotations != null) {
				int annotationsLevels = this.annotations.length;
				for (int i = 0; i < annotationsLevels; i++) {
					int annotationsLength = this.annotations[i] == null ? 0 : this.annotations[i].length;
					for (int j = 0; j < annotationsLength; j++)
						this.annotations[i][j].traverse(visitor, scope);
				}
			}
			Annotation [][] annotationsOnDimensions = getAnnotationsOnDimensions(true);
			if (annotationsOnDimensions != null) {
				for (int i = 0, max = annotationsOnDimensions.length; i < max; i++) {
					Annotation[] annotations2 = annotationsOnDimensions[i];
					for (int j = 0, max2 = annotations2 == null ? 0 : annotations2.length; j < max2; j++) {
						Annotation annotation = annotations2[j];
						annotation.traverse(visitor, scope);
					}
				}
			}
			for (int i = 0, max = this.typeArguments.length; i < max; i++) {
				if (this.typeArguments[i] != null) {
					for (int j = 0, max2 = this.typeArguments[i].length; j < max2; j++) {
						this.typeArguments[i][j].traverse(visitor, scope);
					}
				}
			}
		}
		visitor.endVisit(this, scope);
	}

}
