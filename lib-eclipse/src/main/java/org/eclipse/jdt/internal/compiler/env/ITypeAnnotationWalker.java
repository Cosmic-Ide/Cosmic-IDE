/*******************************************************************************
 * Copyright (c) 2015, 2017 GK Software AG.
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
package org.eclipse.jdt.internal.compiler.env;


/**
 * A TypeAnnotationWalker is initialized with all type annotations found at a given element.
 * It can be used to walk into the types at the given element and finally answer the
 * actual annotations at any node of the walk.
 *
 * The walker is implemented as immutable objects. During the walk either new instances
 * are created, or the current instance is shared if no difference is encountered.
 */
public interface ITypeAnnotationWalker {

	public static final IBinaryAnnotation[] NO_ANNOTATIONS = new IBinaryAnnotation[0];
	/**
	 * A no-effect annotation walker, all walking methods are implemented as identity-functions.
	 * At the end of any walk an empty array of annotations is returned.
	 */
	public static final ITypeAnnotationWalker EMPTY_ANNOTATION_WALKER = new ITypeAnnotationWalker() {
		@Override
		public ITypeAnnotationWalker toField() { return this; }
		@Override
		public ITypeAnnotationWalker toThrows(int rank) { return this; }
		@Override
		public ITypeAnnotationWalker toTypeArgument(int rank) { return this; }
		@Override
		public ITypeAnnotationWalker toMethodParameter(short index) { return this; }
		@Override
		public ITypeAnnotationWalker toSupertype(short index, char[] superTypeSignature) { return this; }
		@Override
		public ITypeAnnotationWalker toTypeParameterBounds(boolean isClassTypeParameter, int parameterRank) { return this; }
		@Override
		public ITypeAnnotationWalker toTypeBound(short boundIndex) { return this; }
		@Override
		public ITypeAnnotationWalker toTypeParameter(boolean isClassTypeParameter, int rank) { return this; }
		@Override
		public ITypeAnnotationWalker toMethodReturn() { return this; }
		@Override
		public ITypeAnnotationWalker toReceiver() { return this; }
		@Override
		public ITypeAnnotationWalker toWildcardBound() { return this; }
		@Override
		public ITypeAnnotationWalker toNextArrayDimension() { return this; }
		@Override
		public ITypeAnnotationWalker toNextNestedType() { return this; }
		@Override
		public IBinaryAnnotation[] getAnnotationsAtCursor(int currentTypeId, boolean mayApplyArrayContentsDefaultNullness) { return NO_ANNOTATIONS; }
	};

	/** Walk to a field. */
	public abstract ITypeAnnotationWalker toField();


	/** Walk to the return type of a method. */
	public abstract ITypeAnnotationWalker toMethodReturn();

	/**
	 * Walk to the receiver type of a method.
	 * Note: Type annotations on receiver are not currently used by the compiler.
	 */
	public abstract ITypeAnnotationWalker toReceiver();

	/**
	 * Walk to the type parameter of the given rank.
	 * @param isClassTypeParameter whether we are looking for a class type parameter (else: method type parameter)
	 * @param rank rank of the type parameter
	 */
	public abstract ITypeAnnotationWalker toTypeParameter(boolean isClassTypeParameter, int rank);

	/**
	 * Walk to the bounds of a type parameter of either a class or a method (signaled by isClassTypeParameter).
	 * Clients must then call {@link #toTypeBound(short)} on the resulting walker.
	 * @param isClassTypeParameter whether we are looking at a class type parameter (else: method type parameter)
	 * @param parameterRank rank of the type parameter.
	 */
	public abstract ITypeAnnotationWalker toTypeParameterBounds(boolean isClassTypeParameter, int parameterRank);

	/**
	 * Detail of {@link #toTypeParameterBounds(boolean, int)}: walk to the bounds
	 * of the previously selected type parameter.
	 * @param boundIndex
	 */
	public abstract ITypeAnnotationWalker toTypeBound(short boundIndex);

	/** Walk to the specified supertype either index based or name based:
	 * @param index -1 is superclass, else index into the list of superinterfaces
	 * @param superTypeSignature name and type arguments of the super type to visit
	 */
	public abstract ITypeAnnotationWalker toSupertype(short index, char[] superTypeSignature);

	/** Walk to the index'th visible formal method parameter (i.e., not counting synthetic args). */
	public abstract ITypeAnnotationWalker toMethodParameter(short index);

	/**
	 * Walk to the throws type at the given index.
	 */
	public abstract ITypeAnnotationWalker toThrows(int index);

	/** Walk to the type argument of the given rank. */
	public abstract ITypeAnnotationWalker toTypeArgument(int rank);

	/** Walk to the bound of a wildcard. */
	public abstract ITypeAnnotationWalker toWildcardBound();

	/**
	 * Descend down one level of array dimensions.
	 */
	public abstract ITypeAnnotationWalker toNextArrayDimension();

	/**
	 * Descend down one level of type nesting.
	 */
	public abstract ITypeAnnotationWalker toNextNestedType();

	/**
	 * Retrieve the type annotations at the current position
	 * reached by invocations of toXYZ() methods.
	 * @param currentTypeId the id of the type being annotated; 0 signals don't care / unknown;
	 * 		 -1 signals if annotating a wildcard or a use of a type variable.
	 */
	public abstract IBinaryAnnotation[] getAnnotationsAtCursor(int currentTypeId, boolean mayApplyArrayContentsDefaultNullness);

}