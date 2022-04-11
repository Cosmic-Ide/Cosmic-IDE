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
 *     Stephan Herrmann - Contribution for
 *								Bug 429958 - [1.8][null] evaluate new DefaultLocation attribute of @NonNullByDefault
 *								Bug 435570 - [1.8][null] @NonNullByDefault illegally tries to affect "throws E"
 *								Bug 466713 - Null Annotations: NullPointerException using <int @Nullable []> as Type Param
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import java.util.function.Consumer;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;

public class ArrayTypeReference extends SingleTypeReference {
	public int dimensions;
	private Annotation[][] annotationsOnDimensions; // jsr308 style type annotations on dimensions.
	public int originalSourceEnd;
	public int extendedDimensions;
	public TypeBinding leafComponentTypeWithoutDefaultNullness;

	/**
	 * ArrayTypeReference constructor comment.
	 * @param source char[]
	 * @param dimensions int
	 * @param pos int
	 */
	public ArrayTypeReference(char[] source, int dimensions, long pos) {

		super(source, pos);
		this.originalSourceEnd = this.sourceEnd;
		this.dimensions = dimensions ;
		this.annotationsOnDimensions = null;
	}

	public ArrayTypeReference(char[] source, int dimensions, Annotation[][] annotationsOnDimensions, long pos) {
		this(source, dimensions, pos);
		if (annotationsOnDimensions != null) {
			this.bits |= ASTNode.HasTypeAnnotations;
		}
		this.annotationsOnDimensions = annotationsOnDimensions;
	}

	@Override
	public int dimensions() {

		return this.dimensions;
	}

	@Override
	public int extraDimensions() {
		return this.extendedDimensions;
	}

	/**
	 @see org.eclipse.jdt.internal.compiler.ast.TypeReference#getAnnotationsOnDimensions(boolean)
	*/
	@Override
	public Annotation[][] getAnnotationsOnDimensions(boolean useSourceOrder) {
		if (useSourceOrder || this.annotationsOnDimensions == null || this.annotationsOnDimensions.length == 0 || this.extendedDimensions == 0 || this.extendedDimensions == this.dimensions)
			return this.annotationsOnDimensions;
		Annotation [][] externalAnnotations = new Annotation[this.dimensions][];
		final int baseDimensions = this.dimensions - this.extendedDimensions;
		System.arraycopy(this.annotationsOnDimensions, baseDimensions, externalAnnotations, 0, this.extendedDimensions);
		System.arraycopy(this.annotationsOnDimensions, 0, externalAnnotations, this.extendedDimensions, baseDimensions);
		return externalAnnotations;
	}

	@Override
	public void setAnnotationsOnDimensions(Annotation [][] annotationsOnDimensions) {
		this.annotationsOnDimensions = annotationsOnDimensions;
	}
	/**
	 * @return char[][]
	 */
	@Override
	public char [][] getParameterizedTypeName(){
		int dim = this.dimensions;
		char[] dimChars = new char[dim*2];
		for (int i = 0; i < dim; i++) {
			int index = i*2;
			dimChars[index] = '[';
			dimChars[index+1] = ']';
		}
		return new char[][]{ CharOperation.concat(this.token, dimChars) };
	}
	@Override
	protected TypeBinding getTypeBinding(Scope scope) {

		if (this.resolvedType != null) {
			return this.resolvedType;
		}
		if (this.dimensions > 255) {
			scope.problemReporter().tooManyDimensions(this);
		}
		TypeBinding leafComponentType = scope.getType(this.token);
		return scope.createArrayType(leafComponentType, this.dimensions);

	}

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output){

		super.printExpression(indent, output);
		if ((this.bits & IsVarArgs) != 0) {
			for (int i= 0 ; i < this.dimensions - 1; i++) {
				if (this.annotationsOnDimensions != null && this.annotationsOnDimensions[i] != null) {
					output.append(' ');
					printAnnotations(this.annotationsOnDimensions[i], output);
					output.append(' ');
				}
				output.append("[]"); //$NON-NLS-1$
			}
			if (this.annotationsOnDimensions != null && this.annotationsOnDimensions[this.dimensions - 1] != null) {
				output.append(' ');
				printAnnotations(this.annotationsOnDimensions[this.dimensions - 1], output);
				output.append(' ');
			}
			output.append("..."); //$NON-NLS-1$
		} else {
			for (int i= 0 ; i < this.dimensions; i++) {
				if (this.annotationsOnDimensions != null && this.annotationsOnDimensions[i] != null) {
					output.append(" "); //$NON-NLS-1$
					printAnnotations(this.annotationsOnDimensions[i], output);
					output.append(" "); //$NON-NLS-1$
				}
				output.append("[]"); //$NON-NLS-1$
			}
		}
		return output;
	}

	@Override
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.annotations != null) {
				Annotation [] typeAnnotations = this.annotations[0];
				for (int i = 0, length = typeAnnotations == null ? 0 : typeAnnotations.length; i < length; i++) {
					typeAnnotations[i].traverse(visitor, scope);
				}
			}
			if (this.annotationsOnDimensions != null) {
				for (int i = 0, max = this.annotationsOnDimensions.length; i < max; i++) {
					Annotation[] annotations2 = this.annotationsOnDimensions[i];
					if (annotations2 != null) {
						for (int j = 0, max2 = annotations2.length; j < max2; j++) {
							Annotation annotation = annotations2[j];
							annotation.traverse(visitor, scope);
						}
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
				Annotation [] typeAnnotations = this.annotations[0];
				for (int i = 0, length = typeAnnotations == null ? 0 : typeAnnotations.length; i < length; i++) {
					typeAnnotations[i].traverse(visitor, scope);
				}
			}
			if (this.annotationsOnDimensions != null) {
				for (int i = 0, max = this.annotationsOnDimensions.length; i < max; i++) {
					Annotation[] annotations2 = this.annotationsOnDimensions[i];
					if (annotations2 != null) {
						for (int j = 0, max2 = annotations2.length; j < max2; j++) {
							Annotation annotation = annotations2[j];
							annotation.traverse(visitor, scope);
						}
					}
				}
			}
		}
		visitor.endVisit(this, scope);
	}

	@Override
	protected TypeBinding internalResolveType(Scope scope, int location) {
		TypeBinding internalResolveType = super.internalResolveType(scope, location);
		internalResolveType = maybeMarkArrayContentsNonNull(scope, internalResolveType, this.sourceStart, this.dimensions,
									leafType -> this.leafComponentTypeWithoutDefaultNullness = leafType);

		return internalResolveType;
	}

	static TypeBinding maybeMarkArrayContentsNonNull(Scope scope, TypeBinding typeBinding, int sourceStart, int dimensions, Consumer<TypeBinding> leafConsumer) {
		LookupEnvironment environment = scope.environment();
		if (environment.usesNullTypeAnnotations()
				&& scope.hasDefaultNullnessFor(Binding.DefaultLocationArrayContents, sourceStart)) {
			AnnotationBinding nonNullAnnotation = environment.getNonNullAnnotation();
			typeBinding = addNonNullToDimensions(scope, typeBinding, nonNullAnnotation, dimensions);

			TypeBinding leafComponentType = typeBinding.leafComponentType();
			if ((leafComponentType.tagBits & TagBits.AnnotationNullMASK) == 0 && leafComponentType.acceptsNonNullDefault()) {
				if (leafConsumer != null)
					leafConsumer.accept(leafComponentType);
				TypeBinding nonNullLeafComponentType = scope.environment().createAnnotatedType(leafComponentType,
						new AnnotationBinding[] { nonNullAnnotation });
				typeBinding = scope.createArrayType(nonNullLeafComponentType, typeBinding.dimensions(),
						typeBinding.getTypeAnnotations());
			}
		}
		return typeBinding;
	}

	static TypeBinding addNonNullToDimensions(Scope scope, TypeBinding typeBinding,
			AnnotationBinding nonNullAnnotation, int dimensions2) {
		AnnotationBinding[][] newAnnots = new AnnotationBinding[dimensions2][];
		AnnotationBinding[] oldAnnots = typeBinding.getTypeAnnotations();
		if (oldAnnots == null) {
			for (int i = 1; i < dimensions2; i++) {
				newAnnots[i] = new AnnotationBinding[] { nonNullAnnotation };
			}
		} else {
			int j = 0;
			for (int i = 0; i < dimensions2; i++) {
				if (j >= oldAnnots.length || oldAnnots[j] == null) {
					if (i != 0) {
						newAnnots[i] = new AnnotationBinding[] { nonNullAnnotation };
					}
					j++;
				} else {
					int k = j;
					boolean seen = false;
					while (oldAnnots[k] != null) {
						seen |= oldAnnots[k].getAnnotationType()
								.hasNullBit(TypeIds.BitNonNullAnnotation | TypeIds.BitNullableAnnotation);
						k++;
					}
					if (seen || i == 0) {
						if (k > j) {
							AnnotationBinding[] annotationsForDimension = new AnnotationBinding[k - j];
							System.arraycopy(oldAnnots, j, annotationsForDimension, 0, k - j);
							newAnnots[i] = annotationsForDimension;
						}
					} else {
						AnnotationBinding[] annotationsForDimension = new AnnotationBinding[k - j + 1];
						annotationsForDimension[0] = nonNullAnnotation;
						System.arraycopy(oldAnnots, j, annotationsForDimension, 1, k - j);
						newAnnots[i] = annotationsForDimension;
					}
					j = k + 1;
				}
			}
		}
		return scope.environment().createAnnotatedType(typeBinding, newAnnots);
	}

	@Override
	public boolean hasNullTypeAnnotation(AnnotationPosition position) {
		switch (position) {
			case LEAF_TYPE:
				// ignore annotationsOnDimensions:
				return super.hasNullTypeAnnotation(position);
			case MAIN_TYPE:
				// outermost dimension only:
				if (this.annotationsOnDimensions != null && this.annotationsOnDimensions.length > 0) {
					Annotation[] innerAnnotations = this.annotationsOnDimensions[0];
					return containsNullAnnotation(innerAnnotations);
				}
				// e.g. subclass ParameterizedSingleTypeReference is not only used for arrays
				return super.hasNullTypeAnnotation(position);
			case ANY:
				if (super.hasNullTypeAnnotation(position))
					return true;
				if (this.resolvedType != null && !this.resolvedType.hasNullTypeAnnotations())
					return false; // shortcut
				if (this.annotationsOnDimensions != null) {
					for (int i = 0; i < this.annotationsOnDimensions.length; i++) {
						Annotation[] innerAnnotations = this.annotationsOnDimensions[i];
						if (containsNullAnnotation(innerAnnotations))
							return true;
					}
				}
		}
    	return false;
	}
}
