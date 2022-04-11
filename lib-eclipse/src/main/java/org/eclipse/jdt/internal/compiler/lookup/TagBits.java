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
 *     Stephan Herrmann - Contributions for
 *								bug 186342 - [compiler][null] Using annotations for null checking
 *								bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								bug 388281 - [compiler][null] inheritance of null annotations as an option
 *								Bug 415043 - [1.8][null] Follow-up re null type annotations after bug 392099
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;

public interface TagBits {

	// Tag bits in the tagBits int of every TypeBinding
	long IsArrayType = ASTNode.Bit1;
	long IsBaseType = ASTNode.Bit2;
	long IsNestedType = ASTNode.Bit3;
	long IsMemberType = ASTNode.Bit4;
	long ContainsNestedTypeReferences = ASTNode.Bit12; // method/parameterized type binding
	long MemberTypeMask = IsNestedType | IsMemberType | ContainsNestedTypeReferences;
	long IsLocalType = ASTNode.Bit5;
	long LocalTypeMask = IsNestedType | IsLocalType | ContainsNestedTypeReferences;
	long IsAnonymousType = ASTNode.Bit6;
	long AnonymousTypeMask = LocalTypeMask | IsAnonymousType | ContainsNestedTypeReferences;
	long IsBinaryBinding = ASTNode.Bit7;

	// set for all bindings either representing a missing type (type), or directly referencing a missing type (field/method/variable)
	long HasMissingType = ASTNode.Bit8;

	// for method
	long HasUncheckedTypeArgumentForBoundCheck = ASTNode.Bit9;

	// local variable
	long ForcedToBeRawType = ASTNode.Bit10;

	// set when method has argument(s) that couldn't be resolved
	long HasUnresolvedArguments = ASTNode.Bit10;

	// for the type cycle hierarchy check used by ClassScope
	long BeginHierarchyCheck = ASTNode.Bit9;  // type
	long EndHierarchyCheck = ASTNode.Bit10; // type
	long PauseHierarchyCheck = ASTNode.Bit20; // type
	long HasParameterAnnotations = ASTNode.Bit11; // method/constructor


	// test bit to see if default abstract methods were computed
	long KnowsDefaultAbstractMethods = ASTNode.Bit11; // type

	long IsArgument = ASTNode.Bit11; // local
	long ClearPrivateModifier = ASTNode.Bit10; // constructor binding

	// for java 7 - this bit is also set if the variable is explicitly or implicitly final
	long IsEffectivelyFinal = ASTNode.Bit12; // local
	long MultiCatchParameter = ASTNode.Bit13; // local
	long IsResource = ASTNode.Bit14; // local

	// have implicit null annotations been collected (inherited(?) & default)?
	long IsNullnessKnown = ASTNode.Bit13; // method

	// test bits to see if parts of binary types are faulted
	long AreFieldsSorted = ASTNode.Bit13;
	long AreFieldsComplete = ASTNode.Bit14; // sorted and all resolved
	long AreMethodsSorted = ASTNode.Bit15;
	long AreMethodsComplete = ASTNode.Bit16; // sorted and all resolved

	// test bit to avoid asking a type for a member type (includes inherited member types)
	long HasNoMemberTypes = ASTNode.Bit17;

	// test bit to identify if the type's hierarchy is inconsistent
	long HierarchyHasProblems = ASTNode.Bit18;

	// test bit to identify if the type's type variables have been connected
	long TypeVariablesAreConnected = ASTNode.Bit19;

	// set for parameterized type with successful bound check
	long PassedBoundCheck = ASTNode.Bit23;

	// set for parameterized type NOT of the form X<?,?>
	long IsBoundParameterizedType = ASTNode.Bit24; // PTB only.

	long HasAnnotatedVariants = ASTNode.Bit24; // TVB, STB

	// used by BinaryTypeBinding
	long HasUnresolvedTypeVariables = ASTNode.Bit25;
	long HasUnresolvedSuperclass = ASTNode.Bit26;
	long HasUnresolvedSuperinterfaces = ASTNode.Bit27;
	long HasUnresolvedEnclosingType = ASTNode.Bit28;
	long HasUnresolvedMemberTypes = ASTNode.Bit29;  // Also in use at STB.

	long HasTypeVariable = ASTNode.Bit30; // set either for type variables (direct) or parameterized types indirectly referencing type variables
	long HasDirectWildcard = ASTNode.Bit31; // set for parameterized types directly referencing wildcards

	// for the annotation cycle hierarchy check used by ClassScope
	long BeginAnnotationCheck = ASTNode.Bit32L;
	long EndAnnotationCheck = ASTNode.Bit33L;

	// standard annotations
	// 9-bits for targets
	long AnnotationResolved = ASTNode.Bit34L;
	long DeprecatedAnnotationResolved = ASTNode.Bit35L;
	long AnnotationTarget = ASTNode.Bit36L; // @Target({}) only sets this bit
	long AnnotationForType = ASTNode.Bit37L;
	long AnnotationForField = ASTNode.Bit38L;
	long AnnotationForMethod = ASTNode.Bit39L;
	long AnnotationForParameter = ASTNode.Bit40L;
	long AnnotationForConstructor = ASTNode.Bit41L;
	long AnnotationForLocalVariable = ASTNode.Bit42L;
	long AnnotationForAnnotationType = ASTNode.Bit43L;
	long AnnotationForPackage = ASTNode.Bit44L;
	long AnnotationForTypeUse = ASTNode.Bit54L;
	long AnnotationForTypeParameter = ASTNode.Bit55L;
	long AnnotationForModule = ASTNode.Bit62L;
	long SE7AnnotationTargetMASK = AnnotationForType | AnnotationForField | AnnotationForMethod
				| AnnotationForParameter | AnnotationForConstructor | AnnotationForLocalVariable
				| AnnotationForAnnotationType | AnnotationForPackage;
	// 2-bits for retention (should check (tagBits & RetentionMask) == RuntimeRetention
	long AnnotationSourceRetention = ASTNode.Bit45L;
	long AnnotationClassRetention = ASTNode.Bit46L;
	long AnnotationRuntimeRetention = AnnotationSourceRetention | AnnotationClassRetention;
	long AnnotationRetentionMASK = AnnotationSourceRetention | AnnotationClassRetention | AnnotationRuntimeRetention;
	// marker annotations
	long AnnotationDeprecated = ASTNode.Bit47L;
	long AnnotationDocumented = ASTNode.Bit48L;
	long AnnotationInherited = ASTNode.Bit49L;
	long AnnotationOverride = ASTNode.Bit50L;
	long AnnotationSuppressWarnings = ASTNode.Bit51L;
	/** @since 3.7 - java 7 safe vargs invocation */
	long AnnotationSafeVarargs = ASTNode.Bit52L;
	/** @since 3.7 - java 7 MethodHandle.invokeExact(..)/invoke(..)*/
	long AnnotationPolymorphicSignature = ASTNode.Bit53L;
	/** @since 3.8 null annotation for MethodBinding or LocalVariableBinding (argument): */
	long AnnotationNullable = ASTNode.Bit56L;
	/** @since 3.8 null annotation for MethodBinding or LocalVariableBinding (argument): */
	long AnnotationNonNull = ASTNode.Bit57L;
	/** @since 3.8 null-default annotation for PackageBinding or TypeBinding or MethodBinding: */
	@Deprecated
	long AnnotationNonNullByDefault = ASTNode.Bit58L;
	/** @since 3.8 canceling null-default annotation for PackageBinding or TypeBinding or MethodBinding: */
	@Deprecated
	long AnnotationNullUnspecifiedByDefault = ASTNode.Bit59L;
	/** From Java 8 */
	long AnnotationFunctionalInterface = ASTNode.Bit60L;
	/** From Java 8 */
	long AnnotationRepeatable = ASTNode.Bit61L; // Only for annotation types and since these cannot have constructors, we can overload HasNonPrivateConstructor.

	/** From Java 9 */
	long AnnotationTerminallyDeprecated = ASTNode.Bit63L;
	/** From Java 14 */
	long AnnotationForRecordComponent = ASTNode.Bit31;

	long AnnotationTargetMASK = SE7AnnotationTargetMASK | AnnotationTarget
			| AnnotationForTypeUse | AnnotationForTypeParameter | AnnotationForModule | AnnotationForRecordComponent;

	long AllStandardAnnotationsMask =
				  AnnotationTargetMASK
				| AnnotationRetentionMASK
				| AnnotationDeprecated
				| AnnotationTerminallyDeprecated
				| AnnotationDocumented
				| AnnotationInherited
				| AnnotationOverride
				| AnnotationSuppressWarnings
				| AnnotationSafeVarargs
				| AnnotationPolymorphicSignature
				| AnnotationNullable
				| AnnotationNonNull
				| AnnotationNonNullByDefault
				| AnnotationNullUnspecifiedByDefault
				| AnnotationRepeatable;

	long AnnotationNullMASK = AnnotationNullable | AnnotationNonNull;
	/** @since 3.10 marks a type that has a nullness annotation directly or on a detail (array dimension/type argument). */
	long HasNullTypeAnnotation = ASTNode.Bit21;

	long HasTypeAnnotations = ASTNode.Bit22;

	long DefaultValueResolved = ASTNode.Bit60L;

	long AnnotationPreviewFeature = ASTNode.Bit32L | ASTNode.Bit33L;
	long EssentialAPI = ASTNode.Bit11;

	// set when type contains non-private constructor(s)
	long HasNonPrivateConstructor = ASTNode.Bit61L;

	// set when type binding has a captured wildcard somewhere
	long HasCapturedWildcard = ASTNode.Bit62L;
}
