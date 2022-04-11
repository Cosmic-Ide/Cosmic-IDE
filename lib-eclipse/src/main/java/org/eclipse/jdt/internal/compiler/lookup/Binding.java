/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *								bug 365531 - [compiler][null] investigate alternative strategy for internally encoding nullness defaults
 *								Bug 400874 - [1.8][compiler] Inference infrastructure should evolve to meet JLS8 18.x (Part G of JSR335 spec)
 *								Bug 429958 - [1.8][null] evaluate new DefaultLocation attribute of @NonNullByDefault
 *     Jesper Steen Moller - Contributions for
 *								Bug 412150 [1.8] [compiler] Enable reflected parameter names during annotation processing
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;

public abstract class Binding {

	// binding kinds
	public static final int FIELD = ASTNode.Bit1;
	public static final int LOCAL = ASTNode.Bit2;
	public static final int VARIABLE = FIELD | LOCAL;
	public static final int TYPE = ASTNode.Bit3;
	public static final int METHOD = ASTNode.Bit4;
	public static final int PACKAGE = ASTNode.Bit5;
	public static final int IMPORT = ASTNode.Bit6;
	public static final int MODULE = ASTNode.Bit7;
	public static final int ARRAY_TYPE = TYPE | ASTNode.Bit7;
	public static final int BASE_TYPE = TYPE | ASTNode.Bit8;
	public static final int PARAMETERIZED_TYPE = TYPE | ASTNode.Bit9;
	public static final int WILDCARD_TYPE = TYPE | ASTNode.Bit10;
	public static final int RAW_TYPE = TYPE | ASTNode.Bit11;
	public static final int GENERIC_TYPE = TYPE | ASTNode.Bit12;
	public static final int TYPE_PARAMETER = TYPE | ASTNode.Bit13;
	public static final int INTERSECTION_TYPE = TYPE | ASTNode.Bit14;
	// jsr 308
	public static final int TYPE_USE = TYPE | ASTNode.Bit15;
	public static final int INTERSECTION_TYPE18 = TYPE | ASTNode.Bit16;
	public static final int POLY_TYPE = TYPE | ASTNode.Bit17;
	// Java 14 - Records - preview
	public static final int RECORD_COMPONENT = ASTNode.Bit18;
//	public static final int VARIABLE = FIELD | LOCAL | RECORD_COMPONENT;
	public static final int PATTERN = ASTNode.Bit19;

	// In the unlikely event you add a new type binding, remember to update TypeBindingVisitor and Scope.substitute methods.

	// Shared binding collections
	public static final ModuleBinding[] NO_MODULES = new ModuleBinding[0];
	public static final PackageBinding[] NO_PACKAGES = new PackageBinding[0];
	public static final PlainPackageBinding[] NO_PLAIN_PACKAGES = new PlainPackageBinding[0];
	public static final TypeBinding[] NO_TYPES = new TypeBinding[0];
	public static final ReferenceBinding[] NO_REFERENCE_TYPES = new ReferenceBinding[0];
	public static final TypeBinding[] NO_PARAMETERS = new TypeBinding[0];
	public static final ReferenceBinding[] NO_EXCEPTIONS = new ReferenceBinding[0];
	public static final ReferenceBinding[] ANY_EXCEPTION = new ReferenceBinding[] { null }; // special handler for all exceptions
	public static final FieldBinding[] NO_FIELDS = new FieldBinding[0];
	public static final MethodBinding[] NO_METHODS = new MethodBinding[0];
	public static final ReferenceBinding[] NO_PERMITTEDTYPES = new ReferenceBinding[0];
	public static final ReferenceBinding[] NO_SUPERINTERFACES = new ReferenceBinding[0];
	public static final ReferenceBinding[] NO_MEMBER_TYPES = new ReferenceBinding[0];
	public static final TypeVariableBinding[] NO_TYPE_VARIABLES = new TypeVariableBinding[0];
	public static final AnnotationBinding[] NO_ANNOTATIONS = new AnnotationBinding[0];
	public static final ElementValuePair[] NO_ELEMENT_VALUE_PAIRS = new ElementValuePair[0];
	public static final char[][] NO_PARAMETER_NAMES = new char[0][];
	public static final RecordComponentBinding[] NO_COMPONENTS = new RecordComponentBinding[0];

	public static final RecordComponentBinding[] UNINITIALIZED_COMPONENTS = new RecordComponentBinding[0];
	public static final FieldBinding[] UNINITIALIZED_FIELDS = new FieldBinding[0];
	public static final MethodBinding[] UNINITIALIZED_METHODS = new MethodBinding[0];
	public static final ReferenceBinding[] UNINITIALIZED_REFERENCE_TYPES = new ReferenceBinding[0];

	static final InferenceVariable[] NO_INFERENCE_VARIABLES = new InferenceVariable[0];
	static final TypeBound[] NO_TYPE_BOUNDS = new TypeBound[0];

	// Nullness defaults:
	public static final int NO_NULL_DEFAULT = 0;

	// not used any longer (was in the old implementation when NonNullByDefault only supported a boolean arg)
	// corresponds to #DefaultLocationsForTrueValue
	// public static final int NONNULL_BY_DEFAULT = 1;

	public static final int NULL_UNSPECIFIED_BY_DEFAULT = 2;
	// JSR308 style:
	/**
	 * Bit in defaultNullness bit vectors, representing the enum constant DefaultLocation#PARAMETER
	 */
	public static final int DefaultLocationParameter = ASTNode.Bit4;
	/**
	 * Bit in defaultNullness bit vectors, representing the enum constant DefaultLocation#RETURN_TYPE
	 */
	public static final int DefaultLocationReturnType = ASTNode.Bit5;
	/**
	 * Bit in defaultNullness bit vectors, representing the enum constant DefaultLocation#FIELD
	 */
	public static final int DefaultLocationField = ASTNode.Bit6;
	/**
	 * Bit in defaultNullness bit vectors, representing the enum constant DefaultLocation#TYPE_ARGUMENT
	 */
	public static final int DefaultLocationTypeArgument = ASTNode.Bit7;
	/**
	 * Bit in defaultNullness bit vectors, representing the enum constant DefaultLocation#TYPE_PARAMETER
	 */
	public static final int DefaultLocationTypeParameter = ASTNode.Bit8;
	/**
	 * Bit in defaultNullness bit vectors, representing the enum constant DefaultLocation#TYPE_BOUND
	 */
	public static final int DefaultLocationTypeBound = ASTNode.Bit9;
	/**
	 * Bit in defaultNullness bit vectors, representing the enum constant DefaultLocation#ARRAY_CONTENTS
	 */
	public static final int DefaultLocationArrayContents = ASTNode.Bit10;

	public static final int DefaultLocationsForTrueValue = DefaultLocationParameter | DefaultLocationReturnType | DefaultLocationField;

	public static final int NullnessDefaultMASK =
			NULL_UNSPECIFIED_BY_DEFAULT | // included to terminate search up the parent chain
			DefaultLocationParameter | DefaultLocationReturnType | DefaultLocationField |
			DefaultLocationTypeArgument | DefaultLocationTypeParameter | DefaultLocationTypeBound | DefaultLocationArrayContents;

	/*
	* Answer the receiver's binding type from Binding.BindingID.
	*/
	public abstract int kind();
	/*
	 * Computes a key that uniquely identifies this binding.
	 * Returns null if binding is not a TypeBinding, a MethodBinding, a FieldBinding, a LocalVariableBinding, a PackageBinding (i.e. an ImportBinding)
	 * or a ModuleBinding.
	 */
	public char[] computeUniqueKey() {
		return computeUniqueKey(true/*leaf*/);
	}
	/*
	 * Computes a key that uniquely identifies this binding. Optionally include access flags.
	 * Returns null if binding is not a TypeBinding, a MethodBinding, a FieldBinding, a LocalVariableBinding, a PackageBinding (i.e. an ImportBinding)
	 * or a ModuleBinding.
	 */
	public char[] computeUniqueKey(boolean isLeaf) {
		return null;
	}

	/**
	 * Compute the tagbits for standard annotations. For source types, these could require
	 * lazily resolving corresponding annotation nodes, in case of forward references.
	 * For type use bindings, this method still returns the tagbits corresponding to the type
	 * declaration binding.
	 * @see org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding#getAnnotationTagBits()
	 */
	public long getAnnotationTagBits() {
		return 0;
	}

	/**
	 * Compute the tag bits for @Deprecated annotations, avoiding resolving
	 * entire annotation if not necessary.
	 * @see org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding#initializeDeprecatedAnnotationTagBits()
	 */
	public void initializeDeprecatedAnnotationTagBits() {
		// empty block
	}

	public boolean isAnnotationType() {
		return false;
	}

	/* API
	* Answer true if the receiver is not a problem binding
	*/
	public final boolean isValidBinding() {
		return problemId() == ProblemReasons.NoError;
	}
	public static boolean isValid(/*@Nullable*/Binding binding) {
		return binding != null && binding.isValidBinding();
	}
	public boolean isVolatile() {
		return false;
	}
	public boolean isTaggedRepeatable() {
		return false;
	}
	public boolean isParameter() {
		return false;
	}
	/* API
	* Answer the problem id associated with the receiver.
	* NoError if the receiver is a valid binding.
	* Note: a parameterized type or an array type are always valid, but may be formed of invalid pieces.
	*/
	// TODO (philippe) should rename into problemReason()
	public int problemId() {
		return ProblemReasons.NoError;
	}
	/* Answer a printable representation of the receiver.
	*/
	public abstract char[] readableName();
	/* Shorter printable representation of the receiver (no qualified type)
	 */
	public char[] shortReadableName(){
		return readableName();
	}
	public AnnotationBinding[] getAnnotations() {
		return Binding.NO_ANNOTATIONS;
	}
	public void setAnnotations(AnnotationBinding[] annotations, Scope scope, boolean forceStore) {
		setAnnotations(annotations, forceStore);
	}
	public void setAnnotations(AnnotationBinding[] annotations, boolean forceStore) {
		// Left to subtypes.
	}
}
