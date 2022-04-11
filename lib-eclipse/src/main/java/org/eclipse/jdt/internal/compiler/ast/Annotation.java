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
 *								bug 365662 - [compiler][null] warn on contradictory and redundant null annotations
 *								bug 331649 - [compiler][null] consider null annotations for fields
 *								Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								Bug 415043 - [1.8][null] Follow-up re null type annotations after bug 392099
 *								Bug 392238 - [1.8][compiler][null] Detect semantically invalid null type annotations
 *								Bug 415850 - [1.8] Ensure RunJDTCoreTests can cope with null annotations enabled
 *								Bug 417295 - [1.8[[null] Massage type annotated null analysis to gel well with deep encoded type bindings.
 *								Bug 424728 - [1.8][null] Unexpected error: The nullness annotation 'XXXX' is not applicable at this location
 *								Bug 392245 - [1.8][compiler][null] Define whether / how @NonNullByDefault applies to TYPE_USE locations
 *								Bug 429958 - [1.8][null] evaluate new DefaultLocation attribute of @NonNullByDefault
 *								Bug 435805 - [1.8][compiler][null] Java 8 compiler does not recognize declaration style null annotations
 *								Bug 457210 - [1.8][compiler][null] Wrong Nullness errors given on full build build but not on incremental build?
 *								Bug 469584 - ClassCastException in Annotation.detectStandardAnnotation (320)
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *                          Bug 409517 - [1.8][compiler] Type annotation problems on more elaborate array references
 *                          Bug 415397 - [1.8][compiler] Type Annotations on wildcard type argument dropped
 *                          Bug 414384 - [1.8] type annotation on abbreviated inner class is not marked as inner type
 *      Jesper S Moller <jesper@selskabet.org> -  Contributions for
 *                          Bug 412153 - [1.8][compiler] Check validity of annotations which may be repeatable
 *                          Bug 412151 - [1.8][compiler] Check repeating annotation's collection type
 *                          Bug 412149 - [1.8][compiler] Emit repeated annotations into the designated container
 *                          Bug 419209 - [1.8] Repeating container annotations should be rejected in the presence of annotation it contains
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import java.util.Stack;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.EnumConstantSignature;
import org.eclipse.jdt.internal.compiler.impl.BooleanConstant;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.impl.IrritantSet;
import org.eclipse.jdt.internal.compiler.lookup.*;

/**
 * Annotation
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class Annotation extends Expression {

	Annotation persistibleAnnotation = this;  // Emit this into class file, unless this is a repeating annotation, in which case package this into the designated container.

	/**
	 * Return the location for the corresponding annotation inside the type reference, <code>null</code> if none.
	 */
	public static int[] getLocations(
			final Expression reference,
			final Annotation annotation) {

		class LocationCollector extends ASTVisitor {
			Stack typePathEntries;
			Annotation searchedAnnotation;
			boolean continueSearch = true;

			public LocationCollector(Annotation currentAnnotation) {
				this.typePathEntries = new Stack();
				this.searchedAnnotation = currentAnnotation;
			}

			private int[] computeNestingDepth(TypeReference typeReference) {
				TypeBinding type = typeReference.resolvedType == null ? null : typeReference.resolvedType.leafComponentType();
				int[] nestingDepths = new int[typeReference.getAnnotatableLevels()];
				if (type != null && type.isNestedType()) {
					int depth = 0;
					TypeBinding currentType = type;
					while (currentType != null) {
						depth += (currentType.isStatic()) ? 0 : 1;
						currentType = currentType.enclosingType();
					}
					// Work backwards computing whether a INNER_TYPE entry is required for each level
					int counter = nestingDepths.length - 1;
					while (type != null && counter >= 0) {
						nestingDepths[counter--] = depth;
						depth -= type.isStatic() ? 0 : 1;
						type = type.enclosingType();
					}
				}
				return nestingDepths;
			}


			private void inspectAnnotations(Annotation [] annotations) {
				for (int i = 0, length = annotations == null ? 0 : annotations.length; this.continueSearch && i < length; i++) {
					if (annotations[i] == this.searchedAnnotation) {
						this.continueSearch = false;
						break;
					}
				}
			}

			private void inspectArrayDimensions(Annotation [][] annotationsOnDimensions, int dimensions) {
				for (int i = 0; this.continueSearch && i < dimensions; i++) {
					Annotation[] annotations = annotationsOnDimensions == null ? null : annotationsOnDimensions[i];
					inspectAnnotations(annotations);
					if (!this.continueSearch) return;
					this.typePathEntries.push(TYPE_PATH_ELEMENT_ARRAY);
				}
			}

			private void inspectTypeArguments(TypeReference[] typeReferences) {
				for (int i = 0, length = typeReferences == null ? 0 : typeReferences.length; this.continueSearch && i < length; i++) {
					int size = this.typePathEntries.size();
					this.typePathEntries.add(new int[]{3,i});
					typeReferences[i].traverse(this, (BlockScope) null);
					if (!this.continueSearch) return;
					this.typePathEntries.setSize(size);
				}
			}

			public boolean visit(TypeReference typeReference, BlockScope scope) {
				if (this.continueSearch) {
					inspectArrayDimensions(typeReference.getAnnotationsOnDimensions(), typeReference.dimensions());
					if (this.continueSearch) {
						int[] nestingDepths = computeNestingDepth(typeReference);
						Annotation[][] annotations = typeReference.annotations;
						TypeReference [][] typeArguments = typeReference.getTypeArguments();
						int levels = typeReference.getAnnotatableLevels();
						int size = this.typePathEntries.size();
						for (int i = levels - 1; this.continueSearch && i >= 0; i--) {  // traverse outwards, see comment below about type annotations from SE7 locations.
							this.typePathEntries.setSize(size);
							for (int j = 0, depth = nestingDepths[i]; j < depth; j++)
								this.typePathEntries.add(TYPE_PATH_INNER_TYPE);
							if (annotations != null)
								inspectAnnotations(annotations[i]);
							if (this.continueSearch && typeArguments != null) {
								inspectTypeArguments(typeArguments[i]);
							}
						}
					}
				}
				return false; // if annotation is not found in the type reference, it must be one from SE7 location, typePathEntries captures the proper path entries for them.
			}
			@Override
			public boolean visit(SingleTypeReference typeReference, BlockScope scope) {
				return visit((TypeReference) typeReference, scope);
			}

			@Override
			public boolean visit(ArrayTypeReference typeReference, BlockScope scope) {
				return visit((TypeReference) typeReference, scope);
			}

			@Override
			public boolean visit(ParameterizedSingleTypeReference typeReference, BlockScope scope) {
				return visit((TypeReference) typeReference, scope);
			}

			@Override
			public boolean visit(QualifiedTypeReference typeReference, BlockScope scope) {
				return visit((TypeReference) typeReference, scope);
			}

			@Override
			public boolean visit(ArrayQualifiedTypeReference typeReference, BlockScope scope) {
				return visit((TypeReference) typeReference, scope);
			}

			@Override
			public boolean visit(ParameterizedQualifiedTypeReference typeReference, BlockScope scope) {
				return visit((TypeReference) typeReference, scope);
			}

			@Override
			public boolean visit(Wildcard typeReference, BlockScope scope) {
				visit((TypeReference) typeReference, scope);
				if (this.continueSearch) {
					TypeReference bound = typeReference.bound;
					if (bound != null) {
						int size = this.typePathEntries.size();
						this.typePathEntries.push(TYPE_PATH_ANNOTATION_ON_WILDCARD_BOUND);
						bound.traverse(this, scope);
						if (this.continueSearch)
							this.typePathEntries.setSize(size);
					}
				}
				return false;
			}

			@Override
			public boolean visit(ArrayAllocationExpression allocationExpression, BlockScope scope) {
				if (this.continueSearch) {
					inspectArrayDimensions(allocationExpression.getAnnotationsOnDimensions(), allocationExpression.dimensions.length);
					if (this.continueSearch) {
						allocationExpression.type.traverse(this, scope);
					}
					if (this.continueSearch) throw new IllegalStateException();
				}
				return false;
			}

			@Override
			public String toString() {
				StringBuffer buffer = new StringBuffer();
				buffer
					.append("search location for ") //$NON-NLS-1$
					.append(this.searchedAnnotation)
					.append("\ncurrent type_path entries : "); //$NON-NLS-1$
				for (int i = 0, maxi = this.typePathEntries.size(); i < maxi; i++) {
					int[] typePathEntry = (int[]) this.typePathEntries.get(i);
					buffer
						.append('(')
						.append(typePathEntry[0])
						.append(',')
						.append(typePathEntry[1])
						.append(')');
				}
				return String.valueOf(buffer);
			}
		}
		if (reference == null) return null;
		LocationCollector collector = new LocationCollector(annotation);
		reference.traverse(collector, (BlockScope) null);
		if (collector.typePathEntries.isEmpty()) {
			return null;
		}
		int size = collector.typePathEntries.size();
		int[] result = new int[size*2];
		int offset=0;
		for (int i = 0; i < size; i++) {
			int[] pathElement = (int[])collector.typePathEntries.get(i);
			result[offset++] = pathElement[0];
			result[offset++] = pathElement[1];
		}
		return result;
	}

	final static MemberValuePair[] NoValuePairs = new MemberValuePair[0];

	static final int[] TYPE_PATH_ELEMENT_ARRAY = new int[]{0,0};
	static final int[] TYPE_PATH_INNER_TYPE = new int[]{1,0};
	static final int[] TYPE_PATH_ANNOTATION_ON_WILDCARD_BOUND = new int[]{2,0};

	public int declarationSourceEnd;
	public Binding recipient;

	public TypeReference type;
	/**
	 *  The representation of this annotation in the type system.
	 */
	protected AnnotationBinding compilerAnnotation = null;

	public static long getRetentionPolicy(char[] policyName) {
		if (policyName == null || policyName.length == 0)
			return 0;
		switch(policyName[0]) {
			case 'C' :
				if (CharOperation.equals(policyName, TypeConstants.UPPER_CLASS))
					return TagBits.AnnotationClassRetention;
				break;
			case 'S' :
				if (CharOperation.equals(policyName, TypeConstants.UPPER_SOURCE))
					return TagBits.AnnotationSourceRetention;
				break;
			case 'R' :
				if (CharOperation.equals(policyName, TypeConstants.UPPER_RUNTIME))
					return TagBits.AnnotationRuntimeRetention;
				break;
		}
		return 0; // unknown
	}

	public static long getTargetElementType(char[] elementName) {
		if (elementName == null || elementName.length == 0)
			return 0;
		switch(elementName[0]) {
			case 'A' :
				if (CharOperation.equals(elementName, TypeConstants.UPPER_ANNOTATION_TYPE))
					return TagBits.AnnotationForAnnotationType;
				break;
			case 'C' :
				if (CharOperation.equals(elementName, TypeConstants.UPPER_CONSTRUCTOR))
					return TagBits.AnnotationForConstructor;
				break;
			case 'F' :
				if (CharOperation.equals(elementName, TypeConstants.UPPER_FIELD))
					return TagBits.AnnotationForField;
				break;
			case 'L' :
				if (CharOperation.equals(elementName, TypeConstants.UPPER_LOCAL_VARIABLE))
					return TagBits.AnnotationForLocalVariable;
				break;
			case 'M' :
				if (CharOperation.equals(elementName, TypeConstants.UPPER_METHOD))
					return TagBits.AnnotationForMethod;
				else if (CharOperation.equals(elementName, TypeConstants.UPPER_MODULE))
					return TagBits.AnnotationForModule;
				break;
			case 'P' :
				if (CharOperation.equals(elementName, TypeConstants.UPPER_PARAMETER))
					return TagBits.AnnotationForParameter;
				else if (CharOperation.equals(elementName, TypeConstants.UPPER_PACKAGE))
					return TagBits.AnnotationForPackage;
				break;
			case 'R' :
				if (CharOperation.equals(elementName, TypeConstants.UPPER_RECORD_COMPONENT))
					return TagBits.AnnotationForRecordComponent;
				break;
			case 'T' :
				if (CharOperation.equals(elementName, TypeConstants.TYPE))
					return TagBits.AnnotationForType;
				if (CharOperation.equals(elementName, TypeConstants.TYPE_USE_TARGET))
					return TagBits.AnnotationForTypeUse;
				if (CharOperation.equals(elementName, TypeConstants.TYPE_PARAMETER_TARGET))
					return TagBits.AnnotationForTypeParameter;
				break;
		}
		return 0; // unknown
	}

	public ElementValuePair[] computeElementValuePairs() {
		return Binding.NO_ELEMENT_VALUE_PAIRS;
	}

	/**
	 * Compute the bit pattern for recognized standard annotations the compiler may need to act upon.
	 * The lower bits (Binding.NullnessDefaultMASK) do not belong in tagBits, but in defaultNullness.
	 */
	private long detectStandardAnnotation(Scope scope, ReferenceBinding annotationType, MemberValuePair valueAttribute) {
		long tagBits = 0;
		switch (annotationType.id) {
			// retention annotation
			case TypeIds.T_JavaLangAnnotationRetention :
				if (valueAttribute != null) {
					Expression expr = valueAttribute.value;
					if ((expr.bits & Binding.VARIABLE) == Binding.FIELD && expr instanceof Reference) { // anything but Reference would be a type error anyway
						FieldBinding field = ((Reference)expr).fieldBinding();
						if (field != null && field.declaringClass.id == T_JavaLangAnnotationRetentionPolicy) {
							tagBits |= getRetentionPolicy(field.name);
						}
					}
				}
				break;
			// target annotation
			case TypeIds.T_JavaLangAnnotationTarget :
				tagBits |= TagBits.AnnotationTarget; // target specified (could be empty)
				if (valueAttribute != null) {
					Expression expr = valueAttribute.value;
					if (expr instanceof ArrayInitializer) {
						ArrayInitializer initializer = (ArrayInitializer) expr;
						final Expression[] expressions = initializer.expressions;
						if (expressions != null) {
							for (int i = 0, length = expressions.length; i < length; i++) {
								Expression initExpr = expressions[i];
								if ((initExpr.bits & Binding.VARIABLE) == Binding.FIELD) {
									FieldBinding field = ((Reference) initExpr).fieldBinding();
									if (field != null && field.declaringClass.id == T_JavaLangAnnotationElementType) {
										long element = getTargetElementType(field.name);
										if ((tagBits & element) != 0) {
											scope.problemReporter().duplicateTargetInTargetAnnotation(annotationType, (NameReference)initExpr);
										} else {
											tagBits |= element;
										}
									}
								}
							}
						}
					} else if ((expr.bits & Binding.VARIABLE) == Binding.FIELD) {
						FieldBinding field = ((Reference) expr).fieldBinding();
						if (field != null && field.declaringClass.id == T_JavaLangAnnotationElementType) {
							tagBits |= getTargetElementType(field.name);
						}
					}
				}
				break;
			case TypeIds.T_JdkInternalPreviewFeature :
				tagBits |= TagBits.AnnotationPreviewFeature;
				for (MemberValuePair memberValuePair : memberValuePairs()) {
					if (CharOperation.equals(memberValuePair.name, TypeConstants.ESSENTIAL_API)) {
						if (memberValuePair.value instanceof TrueLiteral) {
							tagBits |= TagBits.EssentialAPI;
						}
					}
				}
				break;
			// marker annotations
			case TypeIds.T_JavaLangDeprecated :
				tagBits |= TagBits.AnnotationDeprecated;
				if (scope.compilerOptions().complianceLevel >= ClassFileConstants.JDK9) {
					for (MemberValuePair memberValuePair : memberValuePairs()) {
						if (CharOperation.equals(memberValuePair.name, TypeConstants.FOR_REMOVAL)) {
							if (memberValuePair.value instanceof TrueLiteral)
								tagBits |= TagBits.AnnotationTerminallyDeprecated;
							break;
						}
					}
				}
				break;
			case TypeIds.T_JavaLangAnnotationDocumented :
				tagBits |= TagBits.AnnotationDocumented;
				break;
			case TypeIds.T_JavaLangAnnotationInherited :
				tagBits |= TagBits.AnnotationInherited;
				break;
			case TypeIds.T_JavaLangOverride :
				tagBits |= TagBits.AnnotationOverride;
				break;
			case TypeIds.T_JavaLangFunctionalInterface :
				tagBits |= TagBits.AnnotationFunctionalInterface;
				break;
			case TypeIds.T_JavaLangAnnotationRepeatable:
				tagBits |= TagBits.AnnotationRepeatable;
				break;
			case TypeIds.T_JavaLangSuppressWarnings :
				tagBits |= TagBits.AnnotationSuppressWarnings;
				break;
			case TypeIds.T_JavaLangSafeVarargs :
				tagBits |= TagBits.AnnotationSafeVarargs;
				break;
			case TypeIds.T_JavaLangInvokeMethodHandlePolymorphicSignature :
				tagBits |= TagBits.AnnotationPolymorphicSignature;
				break;
		}
		if (annotationType.hasNullBit(TypeIds.BitNullableAnnotation)) {
			tagBits |= TagBits.AnnotationNullable;
		} else if (annotationType.hasNullBit(TypeIds.BitNonNullAnnotation)) {
			tagBits |= TagBits.AnnotationNonNull;
		} else if (annotationType.hasNullBit(TypeIds.BitNonNullByDefaultAnnotation)) {
			tagBits |= determineNonNullByDefaultTagBits(annotationType, valueAttribute);
		}

		return tagBits;
	}

	private long determineNonNullByDefaultTagBits(ReferenceBinding annotationType, MemberValuePair valueAttribute) {
		long tagBits = 0;
		Object value = null;
		if (valueAttribute != null) {
			if (valueAttribute.compilerElementPair != null)
				value = valueAttribute.compilerElementPair.value;
		} else { // fetch default value  - TODO: cache it?
			MethodBinding[] methods = annotationType.methods();
			if (methods != null && methods.length == 1)
				value = methods[0].getDefaultValue();
			else
				tagBits |= Binding.DefaultLocationsForTrueValue; // custom unconfigurable NNBD
		}
		if (value instanceof BooleanConstant) {
			// boolean value is used for declaration annotations, signal using the annotation tag bit:
			tagBits |= ((BooleanConstant)value).booleanValue() ? Binding.DefaultLocationsForTrueValue : Binding.NULL_UNSPECIFIED_BY_DEFAULT;
		} else if (value != null) {
			// non-boolean value signals type annotations, evaluate from DefaultLocation[] to bitvector a la Binding#NullnessDefaultMASK:
			tagBits |= nullLocationBitsFromAnnotationValue(value);
		} else {
			int result = BinaryTypeBinding.evaluateTypeQualifierDefault(annotationType);
			if(result != 0) {
				return result;
			}
		}
		return tagBits;
	}

	/**
	 * Convert the value() attribute of @NonNullByDefault into a bitvector a la {@link Binding#NullnessDefaultMASK}.
	 * This method understands value encodings from source and binary types.
	 *
	 * <b>pre:</b> null annotation analysis is enabled
	 */
	public static int nullLocationBitsFromAnnotationValue(Object value) {
		if (value instanceof Object[]) {
			if (((Object[]) value).length == 0) {					// ({})
				return Binding.NULL_UNSPECIFIED_BY_DEFAULT;
			} else {												// ({vals...})
				int bits = 0;
				for (Object single : (Object[])value)
					bits |= evaluateDefaultNullnessLocation(single);
				return bits;
			}
		} else {													// (val)
			return evaluateDefaultNullnessLocation(value);
		}
	}

	private static int evaluateDefaultNullnessLocation(Object value) {
		char[] name = null;
		if (value instanceof FieldBinding) {
			name = ((FieldBinding) value).name;
		} else if (value instanceof EnumConstantSignature) {
			name = ((EnumConstantSignature) value).getEnumConstantName();
		} else if (value instanceof ElementValuePair.UnresolvedEnumConstant) {
			name = ((ElementValuePair.UnresolvedEnumConstant) value).getEnumConstantName();
		} else if (value instanceof BooleanConstant) {
			return ((BooleanConstant)value).booleanValue() ? Binding.DefaultLocationsForTrueValue : Binding.NULL_UNSPECIFIED_BY_DEFAULT;
		}
		if (name != null) {
			switch (name.length) {
				case 5:
					if (CharOperation.equals(name, TypeConstants.DEFAULT_LOCATION__FIELD))
						return Binding.DefaultLocationField;
					break;
				case 9:
					if (CharOperation.equals(name, TypeConstants.DEFAULT_LOCATION__PARAMETER))
						return Binding.DefaultLocationParameter;
					break;
				case 10:
					if (CharOperation.equals(name, TypeConstants.DEFAULT_LOCATION__TYPE_BOUND))
						return Binding.DefaultLocationTypeBound;
					break;
				case 11:
					if (CharOperation.equals(name, TypeConstants.DEFAULT_LOCATION__RETURN_TYPE))
						return Binding.DefaultLocationReturnType;
					break;
				case 13 :
					if (CharOperation.equals(name, TypeConstants.DEFAULT_LOCATION__TYPE_ARGUMENT))
						return Binding.DefaultLocationTypeArgument;
					break;
				case 14 :
					if (CharOperation.equals(name, TypeConstants.DEFAULT_LOCATION__TYPE_PARAMETER))
						return Binding.DefaultLocationTypeParameter;
					if (CharOperation.equals(name, TypeConstants.DEFAULT_LOCATION__ARRAY_CONTENTS))
						return Binding.DefaultLocationArrayContents;
					break;
			}
		}
		return 0;
	}

	public static int nullLocationBitsFromElementTypeAnnotationValue(Object value) {
		if (value instanceof Object[]) {
			if (((Object[]) value).length == 0) {					// ({})
				return Binding.NULL_UNSPECIFIED_BY_DEFAULT;
			} else {												// ({vals...})
				int bits = 0;
				for (Object single : (Object[])value)
					bits |= evaluateElementTypeNullnessLocation(single);
				return bits;
			}
		} else {													// (val)
			return evaluateElementTypeNullnessLocation(value);
		}
	}

	private static int evaluateElementTypeNullnessLocation(Object value) {
		char[] name = null;
		if (value instanceof FieldBinding) {
			name = ((FieldBinding) value).name;
		} else if (value instanceof EnumConstantSignature) {
			name = ((EnumConstantSignature) value).getEnumConstantName();
		} else if (value instanceof ElementValuePair.UnresolvedEnumConstant) {
			name = ((ElementValuePair.UnresolvedEnumConstant) value).getEnumConstantName();
		}
		if (name != null) {
			switch (name.length) {
				case 5:
					if (CharOperation.equals(name, TypeConstants.UPPER_FIELD))
						return Binding.DefaultLocationField;
					break;
				case 6:
					if (CharOperation.equals(name, TypeConstants.UPPER_METHOD))
						return Binding.DefaultLocationReturnType;
					break;
				case 9:
					if (CharOperation.equals(name, TypeConstants.UPPER_PARAMETER))
						return Binding.DefaultLocationParameter;
					break;
			}
		}
		return 0;
	}


	static String getRetentionName(long tagBits) {
		if ((tagBits & TagBits.AnnotationRuntimeRetention) == TagBits.AnnotationRuntimeRetention) {
			// TagBits.AnnotationRuntimeRetention combines both TagBits.AnnotationClassRetention & TagBits.AnnotationSourceRetention
			return new String(UPPER_RUNTIME);
		} else if ((tagBits & TagBits.AnnotationSourceRetention) != 0) {
			return new String(UPPER_SOURCE);
		} else {
			return new String(TypeConstants.UPPER_CLASS);
		}
	}

	private static long getAnnotationRetention(ReferenceBinding binding) {
		long retention = binding.getAnnotationTagBits() & TagBits.AnnotationRetentionMASK;
		// Retention defaults to CLASS
		return retention != 0 ? retention : TagBits.AnnotationClassRetention;
	}

	public void checkRepeatableMetaAnnotation(BlockScope scope) {

		// `this' is the @Repeatable meta annotation, its recipient is the *repeatable* annotation type - we are at the declaration site, not the repeating use site.

		ReferenceBinding repeatableAnnotationType = (ReferenceBinding) this.recipient; // know it to be an annotation type. On target miss we don't get here

		MemberValuePair[] valuePairs = this.memberValuePairs();
		if (valuePairs == null || valuePairs.length != 1)
			return;

		Object value = valuePairs[0].compilerElementPair.value;
		if (!(value instanceof ReferenceBinding))
			return; // Has deeper problems, will bark elsewhere.
		ReferenceBinding containerAnnotationType = (ReferenceBinding) value;
		if (!containerAnnotationType.isAnnotationType())
			return; // Has deeper problems, will bark elsewhere.

		repeatableAnnotationType.setContainerAnnotationType(containerAnnotationType); // For now. May be reset later to PRB in case of problems.
		checkContainerAnnotationType(valuePairs[0], scope, containerAnnotationType, repeatableAnnotationType, false); // false => not use site, i.e declaration site error reporting requested.
	}

	public static void checkContainerAnnotationType(ASTNode culpritNode, BlockScope scope, ReferenceBinding containerAnnotationType, ReferenceBinding repeatableAnnotationType, boolean useSite) {
		MethodBinding[] annotationMethods = containerAnnotationType.methods();
		boolean sawValue = false;
		for (int i = 0, length = annotationMethods.length; i < length; ++i) {
			MethodBinding method = annotationMethods[i];
			if (CharOperation.equals(method.selector, TypeConstants.VALUE)) {
				sawValue = true;
				if (method.returnType.isArrayType() && method.returnType.dimensions() == 1) {
					ArrayBinding array = (ArrayBinding) method.returnType;
					if (TypeBinding.equalsEquals(array.elementsType(), repeatableAnnotationType)) continue;
				}
				repeatableAnnotationType.tagAsHavingDefectiveContainerType();
				scope.problemReporter().containerAnnotationTypeHasWrongValueType(culpritNode, containerAnnotationType, repeatableAnnotationType, method.returnType);
			} else {
				// Not the value() - must have default (or else isn't suitable as container)
				if ((method.modifiers & ClassFileConstants.AccAnnotationDefault) == 0) {
					repeatableAnnotationType.tagAsHavingDefectiveContainerType();
					scope.problemReporter().containerAnnotationTypeHasNonDefaultMembers(culpritNode, containerAnnotationType, method.selector);
				}
			}
		}
		if (!sawValue) {
			repeatableAnnotationType.tagAsHavingDefectiveContainerType();
			scope.problemReporter().containerAnnotationTypeMustHaveValue(culpritNode, containerAnnotationType);
		}

		if (useSite)
			checkContainingAnnotationTargetAtUse((Annotation) culpritNode, scope, containerAnnotationType, repeatableAnnotationType);
		else
			checkContainerAnnotationTypeTarget(culpritNode, scope, containerAnnotationType, repeatableAnnotationType);

		long annotationTypeBits = getAnnotationRetention(repeatableAnnotationType);
		long containerTypeBits = getAnnotationRetention(containerAnnotationType);
		// Due to clever layout of the bits, we can compare the absolute value directly
		if (containerTypeBits < annotationTypeBits) {
			repeatableAnnotationType.tagAsHavingDefectiveContainerType();
			scope.problemReporter().containerAnnotationTypeHasShorterRetention(culpritNode, repeatableAnnotationType, getRetentionName(annotationTypeBits), containerAnnotationType, getRetentionName(containerTypeBits));
		}

		if ((repeatableAnnotationType.getAnnotationTagBits() & TagBits.AnnotationDocumented) != 0 && (containerAnnotationType.getAnnotationTagBits() & TagBits.AnnotationDocumented) == 0) {
			repeatableAnnotationType.tagAsHavingDefectiveContainerType();
			scope.problemReporter().repeatableAnnotationTypeIsDocumented(culpritNode, repeatableAnnotationType, containerAnnotationType);
		}

		if ((repeatableAnnotationType.getAnnotationTagBits() & TagBits.AnnotationInherited) != 0 && (containerAnnotationType.getAnnotationTagBits() & TagBits.AnnotationInherited) == 0) {
			repeatableAnnotationType.tagAsHavingDefectiveContainerType();
			scope.problemReporter().repeatableAnnotationTypeIsInherited(culpritNode, repeatableAnnotationType, containerAnnotationType);
		}
	}

	// This is for error reporting for bad targets at annotation type declaration site (as opposed to the repeat site)
	private static void checkContainerAnnotationTypeTarget(ASTNode culpritNode, Scope scope, ReferenceBinding containerType, ReferenceBinding repeatableAnnotationType) {
		long tagBits = repeatableAnnotationType.getAnnotationTagBits();
		if ((tagBits & TagBits.AnnotationTargetMASK) == 0)
			tagBits = TagBits.SE7AnnotationTargetMASK; // absence of @Target meta-annotation implies all SE7 targets not all targets.

		long containerAnnotationTypeTypeTagBits = containerType.getAnnotationTagBits();
		if ((containerAnnotationTypeTypeTagBits & TagBits.AnnotationTargetMASK) == 0)
			containerAnnotationTypeTypeTagBits = TagBits.SE7AnnotationTargetMASK;

		final long targets = tagBits & TagBits.AnnotationTargetMASK;
		final long containerAnnotationTypeTargets = containerAnnotationTypeTypeTagBits & TagBits.AnnotationTargetMASK;

		if ((containerAnnotationTypeTargets & ~targets) != 0) {
			class MissingTargetBuilder {
				StringBuffer targetBuffer = new StringBuffer();
				void check(long targetMask, char[] targetName) {
					if ((containerAnnotationTypeTargets & targetMask & ~targets) != 0) {
						 // if targetMask equals TagBits.AnnotationForType implies
						 // TagBits.AnnotationForType is part of containerAnnotationTypeTargets
						if (targetMask == TagBits.AnnotationForType &&
								(targets & TagBits.AnnotationForTypeUse) != 0) {
							return;
						}
						add(targetName);
					}
				}
				void checkAnnotationType(char[] targetName) {
					if ((containerAnnotationTypeTargets & TagBits.AnnotationForAnnotationType) != 0 &&
							((targets & (TagBits.AnnotationForAnnotationType | TagBits.AnnotationForType))) == 0) {
						add(targetName);
					}
				}
				private void add(char[] targetName) {
					if (this.targetBuffer.length() != 0) {
						this.targetBuffer.append(", "); //$NON-NLS-1$
					}
					this.targetBuffer.append(targetName);
				}
				@Override
				public String toString() {
					return this.targetBuffer.toString();
				}
				public boolean hasError() {
					return this.targetBuffer.length() != 0;
				}
			}
			MissingTargetBuilder builder = new MissingTargetBuilder();

			builder.check(TagBits.AnnotationForType, TypeConstants.TYPE);
			builder.check(TagBits.AnnotationForField, TypeConstants.UPPER_FIELD);
			builder.check(TagBits.AnnotationForMethod, TypeConstants.UPPER_METHOD);
			builder.check(TagBits.AnnotationForParameter, TypeConstants.UPPER_PARAMETER);
			builder.check(TagBits.AnnotationForConstructor, TypeConstants.UPPER_CONSTRUCTOR);
			builder.check(TagBits.AnnotationForLocalVariable, TypeConstants.UPPER_LOCAL_VARIABLE);
			builder.checkAnnotationType(TypeConstants.UPPER_ANNOTATION_TYPE);
			builder.check(TagBits.AnnotationForPackage, TypeConstants.UPPER_PACKAGE);
			builder.check(TagBits.AnnotationForTypeParameter, TypeConstants.TYPE_PARAMETER_TARGET);
			builder.check(TagBits.AnnotationForTypeUse, TypeConstants.TYPE_USE_TARGET);
			builder.check(TagBits.AnnotationForModule, TypeConstants.UPPER_MODULE);
			builder.check(TagBits.AnnotationForRecordComponent, TypeConstants.UPPER_RECORD_COMPONENT);
			if (builder.hasError()) {
				repeatableAnnotationType.tagAsHavingDefectiveContainerType();
				scope.problemReporter().repeatableAnnotationTypeTargetMismatch(culpritNode, repeatableAnnotationType, containerType, builder.toString());
			}
		}
	}

	// This is for error reporting for bad targets at the repeated annotation use site (as opposed to repeatable annotation type declaration site) - Leads to better message.
	public static void checkContainingAnnotationTargetAtUse(Annotation repeatingAnnotation, BlockScope scope, TypeBinding containerAnnotationType, TypeBinding repeatingAnnotationType) {
		// check (meta)target compatibility
		if (!repeatingAnnotationType.isValidBinding()) {
			// no need to check annotation usage if missing
			return;
		}
		if (isAnnotationTargetAllowed(repeatingAnnotation, scope, containerAnnotationType, repeatingAnnotation.recipient.kind()) != AnnotationTargetAllowed.YES) {
			scope.problemReporter().disallowedTargetForContainerAnnotation(repeatingAnnotation, containerAnnotationType);
		}
	}

	public AnnotationBinding getCompilerAnnotation() {
		return this.compilerAnnotation;
	}

	public boolean isRuntimeInvisible() {
		final TypeBinding annotationBinding = this.resolvedType;
		if (annotationBinding == null) {
			return false;
		}
		long metaTagBits = annotationBinding.getAnnotationTagBits(); // could be forward reference

		// we need to filter out only "pure" type use and type parameter annotations, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=392119
		if ((metaTagBits & (TagBits.AnnotationForTypeParameter | TagBits.AnnotationForTypeUse)) != 0) {
			if ((metaTagBits & TagBits.SE7AnnotationTargetMASK) == 0) {  // not a hybrid target.
				return false;
			}
		}

		if ((metaTagBits & TagBits.AnnotationRetentionMASK) == 0)
			return true; // by default the retention is CLASS

		return (metaTagBits & TagBits.AnnotationRetentionMASK) == TagBits.AnnotationClassRetention;
	}

	public boolean isRuntimeTypeInvisible() {
		final TypeBinding annotationBinding = this.resolvedType;
		if (annotationBinding == null) {
			return false;
		}
		long metaTagBits = annotationBinding.getAnnotationTagBits(); // could be forward reference

		if ((metaTagBits & (TagBits.AnnotationTargetMASK)) != 0) {
			if ((metaTagBits & (TagBits.AnnotationForTypeParameter | TagBits.AnnotationForTypeUse)) == 0) {
				return false;
			}
		} // else: no-@Target always applicable

		if ((metaTagBits & TagBits.AnnotationRetentionMASK) == 0)
			return true; // by default the retention is CLASS

		return (metaTagBits & TagBits.AnnotationRetentionMASK) == TagBits.AnnotationClassRetention;
	}

	public boolean isRuntimeTypeVisible() {
		final TypeBinding annotationBinding = this.resolvedType;
		if (annotationBinding == null) {
			return false;
		}
		long metaTagBits = annotationBinding.getAnnotationTagBits();

		if ((metaTagBits & (TagBits.AnnotationTargetMASK)) != 0) {
			if ((metaTagBits & (TagBits.AnnotationForTypeParameter | TagBits.AnnotationForTypeUse)) == 0) {
				return false;
			}
		} // else: no-@Target always applicable
		if ((metaTagBits & TagBits.AnnotationRetentionMASK) == 0)
			return false; // by default the retention is CLASS

		return (metaTagBits & TagBits.AnnotationRetentionMASK) == TagBits.AnnotationRuntimeRetention;
	}

	public boolean isRuntimeVisible() {
		final TypeBinding annotationBinding = this.resolvedType;
		if (annotationBinding == null) {
			return false;
		}
		long metaTagBits = annotationBinding.getAnnotationTagBits();
		// we need to filter out only "pure" type use and type parameter annotations, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=392119
		if ((metaTagBits & (TagBits.AnnotationForTypeParameter | TagBits.AnnotationForTypeUse)) != 0) {
			if ((metaTagBits & TagBits.SE7AnnotationTargetMASK) == 0) { // not a hybrid target.
				return false;
			}
		}
		if ((metaTagBits & TagBits.AnnotationRetentionMASK) == 0)
			return false; // by default the retention is CLASS

		return (metaTagBits & TagBits.AnnotationRetentionMASK) == TagBits.AnnotationRuntimeRetention;
	}

	public abstract MemberValuePair[] memberValuePairs();

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output) {
		output.append('@');
		this.type.printExpression(0, output);
		return output;
	}

	public void recordSuppressWarnings(Scope scope, int startSuppresss, int endSuppress, boolean isSuppressingWarnings) {
		IrritantSet suppressWarningIrritants = null;
		MemberValuePair[] pairs = memberValuePairs();
		pairLoop: for (int i = 0, length = pairs.length; i < length; i++) {
			MemberValuePair pair = pairs[i];
			if (CharOperation.equals(pair.name, TypeConstants.VALUE)) {
				Expression value = pair.value;
				if (value instanceof ArrayInitializer) {
					ArrayInitializer initializer = (ArrayInitializer) value;
					Expression[] inits = initializer.expressions;
					if (inits != null) {
						for (int j = 0, initsLength = inits.length; j < initsLength; j++) {
							Constant cst = inits[j].constant;
							if (cst != Constant.NotAConstant && cst.typeID() == T_JavaLangString) {
								IrritantSet irritants = CompilerOptions.warningTokenToIrritants(cst.stringValue());
								if (irritants != null) {
									if (suppressWarningIrritants == null) {
										suppressWarningIrritants = new IrritantSet(irritants);
									} else if (suppressWarningIrritants.set(irritants) == null) {
											scope.problemReporter().unusedWarningToken(inits[j]);
									}
								} else {
									scope.problemReporter().unhandledWarningToken(inits[j]);
								}
							}
						}
					}
				} else {
					Constant cst = value.constant;
					if (cst != Constant.NotAConstant && cst.typeID() == T_JavaLangString) {
						IrritantSet irritants = CompilerOptions.warningTokenToIrritants(cst.stringValue());
						if (irritants != null) {
							suppressWarningIrritants = new IrritantSet(irritants);
							// TODO: should check for unused warning token against enclosing annotation as well ?
						} else {
							scope.problemReporter().unhandledWarningToken(value);
						}
					}
				}
				break pairLoop;
			}
		}
		if (isSuppressingWarnings && suppressWarningIrritants != null) {
			scope.referenceCompilationUnit().recordSuppressWarnings(suppressWarningIrritants, this, startSuppresss, endSuppress, scope.referenceContext());
		}
	}

	@Override
	public TypeBinding resolveType(BlockScope scope) {

		if (this.compilerAnnotation != null)
			return this.resolvedType;
		this.constant = Constant.NotAConstant;

		TypeBinding typeBinding;
		if (this.resolvedType == null) {
			typeBinding = this.type.resolveType(scope);
			if (typeBinding == null) {
				this.resolvedType = new ProblemReferenceBinding(this.type.getTypeName(), null, ProblemReasons.NotFound);
				return null;
			}
			this.resolvedType = typeBinding;
		} else {
			typeBinding = this.resolvedType;
		}
		// ensure type refers to an annotation type
		if (!typeBinding.isAnnotationType() && typeBinding.isValidBinding()) {
			scope.problemReporter().notAnnotationType(typeBinding, this.type);
			return null;
		}

		ReferenceBinding annotationType = (ReferenceBinding) this.resolvedType;
		MethodBinding[] methods = annotationType.methods();
		// clone valuePairs to keep track of unused ones
		MemberValuePair[] originalValuePairs = memberValuePairs();
		MemberValuePair valueAttribute = null; // remember the first 'value' pair
		MemberValuePair[] pairs;
		int pairsLength = originalValuePairs.length;
		if (pairsLength > 0) {
			System.arraycopy(originalValuePairs, 0, pairs = new MemberValuePair[pairsLength], 0, pairsLength);
		} else {
			pairs = originalValuePairs;
		}

		nextMember: for (int i = 0, requiredLength = methods.length; i < requiredLength; i++) {
			MethodBinding method = methods[i];
			char[] selector = method.selector;
			boolean foundValue = false;
			nextPair: for (int j = 0; j < pairsLength; j++) {
				MemberValuePair pair = pairs[j];
				if (pair == null) continue nextPair;
				char[] name = pair.name;
				if (CharOperation.equals(name, selector)) {
					if (valueAttribute == null && CharOperation.equals(name, TypeConstants.VALUE)) {
						valueAttribute = pair;
					}
					pair.binding = method;
					pair.resolveTypeExpecting(scope, method.returnType);
					pairs[j] = null; // consumed
					foundValue = true;

					// check duplicates
					boolean foundDuplicate = false;
					for (int k = j+1; k < pairsLength; k++) {
						MemberValuePair otherPair = pairs[k];
						if (otherPair == null) continue;
						if (CharOperation.equals(otherPair.name, selector)) {
							foundDuplicate = true;
							scope.problemReporter().duplicateAnnotationValue(annotationType, otherPair);
							otherPair.binding = method;
							otherPair.resolveTypeExpecting(scope, method.returnType);
							pairs[k] = null;
						}
					}
					if (foundDuplicate) {
						scope.problemReporter().duplicateAnnotationValue(annotationType, pair);
						continue nextMember;
					}
				}
			}
			if (!foundValue
					&& (method.modifiers & ClassFileConstants.AccAnnotationDefault) == 0
					&& (this.bits & IsRecovered) == 0
					&& annotationType.isValidBinding()) {
				scope.problemReporter().missingValueForAnnotationMember(this, selector);
			}
		}
		// check unused pairs
		for (int i = 0; i < pairsLength; i++) {
			if (pairs[i] != null) {
				if (annotationType.isValidBinding()) {
					scope.problemReporter().undefinedAnnotationValue(annotationType, pairs[i]);
				}
				pairs[i].resolveTypeExpecting(scope, null); // resilient
			}
		}
		this.compilerAnnotation = scope.environment().createAnnotation((ReferenceBinding) this.resolvedType, computeElementValuePairs());
		// recognize standard annotations ?
		long tagBits = detectStandardAnnotation(scope, annotationType, valueAttribute);
		int defaultNullness = (int)(tagBits & Binding.NullnessDefaultMASK);
		tagBits &= ~Binding.NullnessDefaultMASK;
		CompilerOptions compilerOptions = scope.compilerOptions();
		if ((tagBits & TagBits.AnnotationDeprecated) != 0 && compilerOptions.complianceLevel >= ClassFileConstants.JDK9 && !compilerOptions.storeAnnotations) {
			this.recipient.setAnnotations(new AnnotationBinding[] {this.compilerAnnotation}, true); // force storing enhanced deprecation
		}

		// record annotation positions in the compilation result
		scope.referenceCompilationUnit().recordSuppressWarnings(IrritantSet.NLS, null, this.sourceStart, this.declarationSourceEnd, scope.referenceContext());
		if (this.recipient != null) {
			int kind = this.recipient.kind();
			if (tagBits != 0 || defaultNullness != 0) {
				// tag bits onto recipient
				switch (kind) {
					case Binding.MODULE :
						SourceModuleBinding module = (SourceModuleBinding) this.recipient;
						module.tagBits |= tagBits;
						if ((tagBits & TagBits.AnnotationSuppressWarnings) != 0) {
							ModuleDeclaration moduleDeclaration =  module.scope.referenceContext.moduleDeclaration;
							recordSuppressWarnings(scope, 0, moduleDeclaration.declarationSourceEnd, compilerOptions.suppressWarnings);
						}
						module.defaultNullness |= defaultNullness;
						break;
					case Binding.PACKAGE :
						((PackageBinding)this.recipient).tagBits |= tagBits;
						break;
					case Binding.TYPE :
					case Binding.GENERIC_TYPE :
						SourceTypeBinding sourceType = (SourceTypeBinding) this.recipient;
						if ((tagBits & TagBits.AnnotationRepeatable) == 0 || sourceType.isAnnotationType()) // don't set AnnotationRepeatable on non-annotation types.
							sourceType.tagBits |= tagBits;
						if ((tagBits & TagBits.AnnotationSuppressWarnings) != 0) {
							TypeDeclaration typeDeclaration =  sourceType.scope.referenceContext;
							int start;
							if (scope.referenceCompilationUnit().types[0] == typeDeclaration) {
								start = 0;
							} else {
								start = typeDeclaration.declarationSourceStart;
							}
							recordSuppressWarnings(scope, start, typeDeclaration.declarationSourceEnd, compilerOptions.suppressWarnings);
						}
						sourceType.defaultNullness |= defaultNullness;
						break;
					case Binding.METHOD :
						MethodBinding sourceMethod = (MethodBinding) this.recipient;
						sourceMethod.tagBits |= tagBits;
						if ((tagBits & TagBits.AnnotationSuppressWarnings) != 0) {
							sourceType = (SourceTypeBinding) sourceMethod.declaringClass;
							AbstractMethodDeclaration methodDeclaration = sourceType.scope.referenceContext.declarationOf(sourceMethod);
							recordSuppressWarnings(scope, methodDeclaration.declarationSourceStart, methodDeclaration.declarationSourceEnd, compilerOptions.suppressWarnings);
						}
						long nullBits = sourceMethod.tagBits & TagBits.AnnotationNullMASK;
						if (nullBits == TagBits.AnnotationNullMASK) {
							scope.problemReporter().contradictoryNullAnnotations(this);
							sourceMethod.tagBits &= ~TagBits.AnnotationNullMASK; // avoid secondary problems
						}
						if (nullBits != 0 && sourceMethod.isConstructor()) {
							if (compilerOptions.sourceLevel >= ClassFileConstants.JDK1_8)
								scope.problemReporter().nullAnnotationUnsupportedLocation(this);
							// for declaration annotations the inapplicability will be reported below
							sourceMethod.tagBits &= ~TagBits.AnnotationNullMASK;
						}
						sourceMethod.defaultNullness |= defaultNullness;
						break;
					case Binding.RECORD_COMPONENT :
						RecordComponentBinding sourceRecordComponent = (RecordComponentBinding) this.recipient;
						sourceRecordComponent.tagBits |= tagBits;
						if ((tagBits & TagBits.AnnotationSuppressWarnings) != 0) {
							RecordComponent recordComponent = sourceRecordComponent.sourceRecordComponent();
							recordSuppressWarnings(scope, recordComponent.declarationSourceStart, recordComponent.declarationSourceEnd, compilerOptions.suppressWarnings);
						}
//						TODO: BUG 562478 Consideration - to uncomment/modify the following:
//						if (defaultNullness != 0) {
//							RecordComponent recordComponent = sourceRecordComponent.sourceRecordComponent();
//							// test merged value of defaultNullness contributed by this annotation and previous annotations on same target is redundant w.r.t. containing value
//							// (for targets other than fields the resulting value is tested only once after processing all annotations, but this is hard to do for fields)
//							Binding target = scope.parent.checkRedundantDefaultNullness(
//									defaultNullness | scope.localNonNullByDefaultValue(recordComponent.sourceStart),
//									recordComponent.sourceStart);
//							scope.recordNonNullByDefault(recordComponent.binding, defaultNullness, this, recordComponent.declarationSourceStart, recordComponent.declarationSourceEnd);
//							if (target != null) {
//								scope.problemReporter().nullDefaultAnnotationIsRedundant(recordComponent, new Annotation[]{this}, target);
//							}
//						}
//						// fields don't yet have their type resolved, in 1.8 null annotations
//						// will be transfered from the field to its type during STB.resolveTypeFor().
//						if ((sourceRecordComponent.tagBits & TagBits.AnnotationNullMASK) == TagBits.AnnotationNullMASK) {
//							scope.problemReporter().contradictoryNullAnnotations(this);
//							sourceRecordComponent.tagBits &= ~TagBits.AnnotationNullMASK; // avoid secondary problems
//						}
						break;
					case Binding.FIELD :
						FieldBinding sourceField = (FieldBinding) this.recipient;
						sourceField.tagBits |= tagBits;
						if ((tagBits & TagBits.AnnotationSuppressWarnings) != 0) {
							sourceType = (SourceTypeBinding) sourceField.declaringClass;
							FieldDeclaration fieldDeclaration = sourceType.scope.referenceContext.declarationOf(sourceField);
							recordSuppressWarnings(scope, fieldDeclaration.declarationSourceStart, fieldDeclaration.declarationSourceEnd, compilerOptions.suppressWarnings);
						}
						if (defaultNullness != 0) {
							sourceType = (SourceTypeBinding) sourceField.declaringClass;
							FieldDeclaration fieldDeclaration = sourceType.scope.referenceContext.declarationOf(sourceField);
							// test merged value of defaultNullness contributed by this annotation and previous annotations on same target is redundant w.r.t. containing value
							// (for targets other than fields the resulting value is tested only once after processing all annotations, but this is hard to do for fields)
							Binding target = scope.parent.checkRedundantDefaultNullness(
									defaultNullness | scope.localNonNullByDefaultValue(fieldDeclaration.sourceStart),
									fieldDeclaration.sourceStart);
							scope.recordNonNullByDefault(fieldDeclaration.binding, defaultNullness, this, fieldDeclaration.declarationSourceStart, fieldDeclaration.declarationSourceEnd);
							if (target != null) {
								scope.problemReporter().nullDefaultAnnotationIsRedundant(fieldDeclaration, new Annotation[]{this}, target);
							}
						}
						// fields don't yet have their type resolved, in 1.8 null annotations
						// will be transfered from the field to its type during STB.resolveTypeFor().
						if ((sourceField.tagBits & TagBits.AnnotationNullMASK) == TagBits.AnnotationNullMASK) {
							scope.problemReporter().contradictoryNullAnnotations(this);
							sourceField.tagBits &= ~TagBits.AnnotationNullMASK; // avoid secondary problems
						}
						break;
					case Binding.LOCAL :
						LocalVariableBinding variable = (LocalVariableBinding) this.recipient;
						variable.tagBits |= tagBits;
						if ((variable.tagBits & TagBits.AnnotationNullMASK) == TagBits.AnnotationNullMASK) {
							scope.problemReporter().contradictoryNullAnnotations(this);
							variable.tagBits &= ~TagBits.AnnotationNullMASK; // avoid secondary problems
						}
						if ((tagBits & TagBits.AnnotationSuppressWarnings) != 0) {
							LocalDeclaration localDeclaration = variable.declaration;
							recordSuppressWarnings(scope, localDeclaration.declarationSourceStart, localDeclaration.declarationSourceEnd, compilerOptions.suppressWarnings);
						}
						// note: defaultNullness for local declarations has been already been handled earlier by handleNonNullByDefault()
						break;
				}
			}
			if (kind == Binding.TYPE) {
				SourceTypeBinding sourceType = (SourceTypeBinding) this.recipient;
				if (CharOperation.equals(sourceType.sourceName, TypeConstants.PACKAGE_INFO_NAME))
					kind = Binding.PACKAGE;
			}
			checkAnnotationTarget(this, scope, annotationType, kind, this.recipient, tagBits & TagBits.AnnotationNullMASK);
		}
		return this.resolvedType;
	}

	public long handleNonNullByDefault(BlockScope scope) {
		TypeBinding typeBinding = this.resolvedType;
		if (typeBinding == null) {
			typeBinding = this.type.resolveType(scope);
			if (typeBinding == null) {
				return 0;
			}
			this.resolvedType = typeBinding;
		}
		if (!typeBinding.isAnnotationType()) {
			return 0;
		}

		ReferenceBinding annotationType = (ReferenceBinding) typeBinding;

		if (!annotationType.hasNullBit(TypeIds.BitNonNullByDefaultAnnotation)) {
			return 0;
		}

		MethodBinding[] methods = annotationType.methods();
		// clone valuePairs to keep track of unused ones
		MemberValuePair[] pairs = memberValuePairs();
		MemberValuePair valueAttribute = null; // remember the first 'value' pair
		int pairsLength = pairs.length;

		for (int i = 0, requiredLength = methods.length; i < requiredLength; i++) {
			MethodBinding method = methods[i];
			char[] selector = method.selector;
			nextPair: for (int j = 0; j < pairsLength; j++) {
				MemberValuePair pair = pairs[j];
				if (pair == null) continue nextPair;
				char[] name = pair.name;
				if (CharOperation.equals(name, selector)) {
					if (valueAttribute == null && CharOperation.equals(name, TypeConstants.VALUE)) {
						valueAttribute = pair;
						pair.binding = method;
						pair.resolveTypeExpecting(scope, method.returnType);
					}
				}
			}
		}
		// recognize standard annotations ?
		long tagBits = determineNonNullByDefaultTagBits(annotationType, valueAttribute);
		return (int) (tagBits & Binding.NullnessDefaultMASK);
	}

	public enum AnnotationTargetAllowed {
		YES, TYPE_ANNOTATION_ON_QUALIFIED_NAME, NO;
	}

	private static AnnotationTargetAllowed isAnnotationTargetAllowed(Binding recipient, BlockScope scope, TypeBinding annotationType, int kind, long metaTagBits) {
		switch (kind) {
			case Binding.PACKAGE :
				if ((metaTagBits & TagBits.AnnotationForPackage) != 0)
					return AnnotationTargetAllowed.YES;
				else if (scope.compilerOptions().sourceLevel <= ClassFileConstants.JDK1_6) {
					SourceTypeBinding sourceType = (SourceTypeBinding) recipient;
					if (CharOperation.equals(sourceType.sourceName, TypeConstants.PACKAGE_INFO_NAME))
						return AnnotationTargetAllowed.YES;
				}
				break;
			case Binding.TYPE_USE :
				if ((metaTagBits & TagBits.AnnotationForTypeUse) != 0) {
					// jsr 308
					return AnnotationTargetAllowed.YES;
				}
				if (scope.compilerOptions().sourceLevel < ClassFileConstants.JDK1_8) {
					// already reported as syntax error; don't report secondary problems
					return AnnotationTargetAllowed.YES;
				}
				break;
			case Binding.TYPE :
			case Binding.GENERIC_TYPE :
				if (((ReferenceBinding)recipient).isAnnotationType()) {
					if ((metaTagBits & (TagBits.AnnotationForAnnotationType | TagBits.AnnotationForType | TagBits.AnnotationForTypeUse)) != 0)
					return AnnotationTargetAllowed.YES;
				} else if ((metaTagBits & (TagBits.AnnotationForType | TagBits.AnnotationForTypeUse)) != 0) {
					return AnnotationTargetAllowed.YES;
				} else if ((metaTagBits & TagBits.AnnotationForPackage) != 0) {
					if (CharOperation.equals(((ReferenceBinding) recipient).sourceName, TypeConstants.PACKAGE_INFO_NAME))
						return AnnotationTargetAllowed.YES;
				}
				break;
			case Binding.METHOD :
				MethodBinding methodBinding = (MethodBinding) recipient;
				if (methodBinding.isConstructor()) {
					if ((metaTagBits & (TagBits.AnnotationForConstructor | TagBits.AnnotationForTypeUse)) != 0)
						return AnnotationTargetAllowed.YES;
				} else if ((metaTagBits & TagBits.AnnotationForMethod) != 0) {
					return AnnotationTargetAllowed.YES;
				} else if ((metaTagBits & TagBits.AnnotationForTypeUse) != 0) {
					SourceTypeBinding sourceType = (SourceTypeBinding) methodBinding.declaringClass;
					MethodDeclaration methodDecl = (MethodDeclaration) sourceType.scope.referenceContext.declarationOf(methodBinding);
					if (isTypeUseCompatible(methodDecl.returnType, scope)) {
						return AnnotationTargetAllowed.YES;
					} else {
						return AnnotationTargetAllowed.TYPE_ANNOTATION_ON_QUALIFIED_NAME;
					}
				}
				break;
			case Binding.FIELD :
				if ((metaTagBits & TagBits.AnnotationForField) != 0) {
					return AnnotationTargetAllowed.YES;
				} else if (((FieldBinding) recipient).isRecordComponent()){
					long recordComponentMask = TagBits.AnnotationForRecordComponent |
							TagBits.AnnotationForMethod |
							TagBits.AnnotationForParameter |
							TagBits.AnnotationForTypeUse;
					return (metaTagBits & recordComponentMask) != 0 ? AnnotationTargetAllowed.YES : AnnotationTargetAllowed.NO;
				} else if ((metaTagBits & TagBits.AnnotationForTypeUse) != 0) {
					FieldBinding sourceField = (FieldBinding) recipient;
					SourceTypeBinding sourceType = (SourceTypeBinding) sourceField.declaringClass;
					FieldDeclaration fieldDeclaration = sourceType.scope.referenceContext.declarationOf(sourceField);
					if (isTypeUseCompatible(fieldDeclaration.type, scope)) {
						return AnnotationTargetAllowed.YES;
					} else {
						return AnnotationTargetAllowed.TYPE_ANNOTATION_ON_QUALIFIED_NAME;
					}
				}
				break;
			case Binding.RECORD_COMPONENT :
				/* JLS 14 9.7.4 Record Preview
				 * It is a compile-time error if an annotation of type T is syntactically a modifier for:
				 * ...
				 * a record component but T is not applicable to record component declarations, field declarations,
				 * method declarations, or type contexts.
				 */
				long recordComponentMask = TagBits.AnnotationForRecordComponent |
				TagBits.AnnotationForField |
				TagBits.AnnotationForMethod |
				TagBits.AnnotationForParameter | // See JLS 14 8.10.4 Records Preview - TODO revisit in J15
				TagBits.AnnotationForTypeUse;
				return (metaTagBits & recordComponentMask) != 0 ? AnnotationTargetAllowed.YES :
					AnnotationTargetAllowed.NO;
			case Binding.LOCAL :
				LocalVariableBinding localVariableBinding = (LocalVariableBinding) recipient;
				if ((localVariableBinding.tagBits & TagBits.IsArgument) != 0) {
					if ((metaTagBits & TagBits.AnnotationForParameter) != 0) {
						return AnnotationTargetAllowed.YES;
					} else if ((metaTagBits & TagBits.AnnotationForTypeUse) != 0) {
						if (isTypeUseCompatible(localVariableBinding.declaration.type, scope)) {
							return AnnotationTargetAllowed.YES;
						} else {
							return AnnotationTargetAllowed.TYPE_ANNOTATION_ON_QUALIFIED_NAME;
						}
					}
				} else if ((annotationType.tagBits & TagBits.AnnotationForLocalVariable) != 0) {
					return AnnotationTargetAllowed.YES;
				} else if ((metaTagBits & TagBits.AnnotationForTypeUse) != 0) {
					if (localVariableBinding.declaration.isTypeNameVar(scope)) {
						return AnnotationTargetAllowed.NO;
					} else if (isTypeUseCompatible(localVariableBinding.declaration.type, scope)) {
						return AnnotationTargetAllowed.YES;
					} else {
						return AnnotationTargetAllowed.TYPE_ANNOTATION_ON_QUALIFIED_NAME;
					}
				}
				break;
			case Binding.TYPE_PARAMETER : // jsr308
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=391196
				if ((metaTagBits & (TagBits.AnnotationForTypeParameter | TagBits.AnnotationForTypeUse)) != 0) {
					return AnnotationTargetAllowed.YES;
				}
				break;
			case Binding.MODULE:
				if ((metaTagBits & (TagBits.AnnotationForModule)) != 0) {
					return AnnotationTargetAllowed.YES;
				}
				break;
		}
		return AnnotationTargetAllowed.NO;
	}

	public static boolean isAnnotationTargetAllowed(BlockScope scope, TypeBinding annotationType, Binding recipient) {
		long metaTagBits = annotationType.getAnnotationTagBits(); // could be forward reference
		if ((metaTagBits & TagBits.AnnotationTargetMASK) == 0) {
			return true;
		}
		return isAnnotationTargetAllowed(recipient, scope, annotationType, recipient.kind(), metaTagBits)==AnnotationTargetAllowed.YES;
	}

	static AnnotationTargetAllowed isAnnotationTargetAllowed(Annotation annotation, BlockScope scope, TypeBinding annotationType, int kind) {

		long metaTagBits = annotationType.getAnnotationTagBits(); // could be forward reference
		if ((metaTagBits & TagBits.AnnotationTargetMASK) == 0) {
			// does not specify any target restriction - all locations are possible
			return AnnotationTargetAllowed.YES;
		}

		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=391201
		if ((metaTagBits & TagBits.SE7AnnotationTargetMASK) == 0
				&& (metaTagBits & (TagBits.AnnotationForTypeUse | TagBits.AnnotationForTypeParameter)) != 0) {
			if (scope.compilerOptions().sourceLevel < ClassFileConstants.JDK1_8) {
				switch (kind) {
					case Binding.PACKAGE :
					case Binding.TYPE :
					case Binding.GENERIC_TYPE :
					case Binding.METHOD :
					case Binding.FIELD :
					case Binding.LOCAL :
					case Binding.RECORD_COMPONENT :
						scope.problemReporter().invalidUsageOfTypeAnnotations(annotation);
				}
			}
		}
		return isAnnotationTargetAllowed(annotation.recipient, scope, annotationType, kind, metaTagBits);
	}

	static void checkAnnotationTarget(Annotation annotation, BlockScope scope, ReferenceBinding annotationType, int kind, Binding recipient, long tagBitsToRevert) {
		// check (meta)target compatibility
		if (!annotationType.isValidBinding()) {
			// no need to check annotation usage if missing
			return;
		}
	AnnotationTargetAllowed annotationTargetAllowed = isAnnotationTargetAllowed(annotation, scope, annotationType, kind);
	if (annotationTargetAllowed != AnnotationTargetAllowed.YES) {
		if(annotationTargetAllowed == AnnotationTargetAllowed.TYPE_ANNOTATION_ON_QUALIFIED_NAME) {
			scope.problemReporter().typeAnnotationAtQualifiedName(annotation);
		} else {
			scope.problemReporter().disallowedTargetForAnnotation(annotation);
		}
			if (recipient instanceof TypeBinding)
				((TypeBinding)recipient).tagBits &= ~tagBitsToRevert;
		}
	}

	/**
	 * Check to see if a repeating annotation is in fact of a container annotation type for an annotation which is also present at the same target.
	 * @param scope The scope (for error reporting)
	 * @param repeatedAnnotationType Type of annotation which has been repeated (to check for possibly being a container for a repeatable annotation)
	 * @param sourceAnnotations The annotations to check
	 */
	public static void checkForInstancesOfRepeatableWithRepeatingContainerAnnotation(BlockScope scope, ReferenceBinding repeatedAnnotationType, Annotation[] sourceAnnotations) {
		// Fail fast if the repeating annotation type can't be a container, anyway
		MethodBinding[] valueMethods = repeatedAnnotationType.getMethods(TypeConstants.VALUE);
		if (valueMethods.length != 1) return; // No violations possible

		TypeBinding methodReturnType = valueMethods[0].returnType;
		// value must be an array
		if (! methodReturnType.isArrayType() || methodReturnType.dimensions() != 1) return;

		ArrayBinding array = (ArrayBinding) methodReturnType;
		TypeBinding elementsType = array.elementsType();
		if (! elementsType.isRepeatableAnnotationType()) return; // Can't be a problem, then

		for (int i= 0; i < sourceAnnotations.length; ++i) {
			Annotation annotation = sourceAnnotations[i];
			if (TypeBinding.equalsEquals(elementsType, annotation.resolvedType)) {
				scope.problemReporter().repeatableAnnotationWithRepeatingContainer(annotation, repeatedAnnotationType);
				return; // One is enough for this annotation type
			}
		}
	}

	// Check and answer if an attempt to annotate a package is being made. Error should be reported by caller.
	public static boolean isTypeUseCompatible(TypeReference reference, Scope scope) {
		if (reference != null && !(reference instanceof SingleTypeReference)) {
			Binding binding = scope.getPackage(reference.getTypeName());
			// In case of ProblemReferenceBinding, don't report additional error
			if (binding instanceof PackageBinding) {
				return false;
			}
		}
		return true;
	}

	// Complain if an attempt to annotate the enclosing type of a static member type is being made.
	public static void isTypeUseCompatible(TypeReference reference, Scope scope, Annotation[] annotations) {
		if (annotations == null || reference == null || reference.getAnnotatableLevels() == 1)
			return;
		if (scope.environment().globalOptions.sourceLevel < ClassFileConstants.JDK1_8)
			return;

		TypeBinding resolvedType = reference.resolvedType == null ? null : reference.resolvedType.leafComponentType();
		if (resolvedType == null || !resolvedType.isNestedType())
			return;

		nextAnnotation:
			for (int i = 0, annotationsLength = annotations.length; i < annotationsLength; i++) {
				Annotation annotation = annotations[i];
				long metaTagBits = annotation.resolvedType.getAnnotationTagBits();
				if ((metaTagBits & TagBits.AnnotationForTypeUse) != 0 && (metaTagBits & TagBits.SE7AnnotationTargetMASK) == 0) {
					ReferenceBinding currentType = (ReferenceBinding) resolvedType;
					while (currentType.isNestedType()) {
						if (currentType.isStatic()) {
							QualifiedTypeReference.rejectAnnotationsOnStaticMemberQualififer(scope, currentType, new Annotation [] { annotation });
							continue nextAnnotation;
						} else {
							if (annotation.hasNullBit(TypeIds.BitNonNullAnnotation|TypeIds.BitNullableAnnotation)) {
								scope.problemReporter().nullAnnotationAtQualifyingType(annotation);
								continue nextAnnotation;
							}
						}
						currentType = currentType.enclosingType();
					}
				}
			}
	}

	public boolean hasNullBit(int bit) {
		return this.resolvedType instanceof ReferenceBinding && ((ReferenceBinding) this.resolvedType).hasNullBit(bit);
	}

	@Override
	public abstract void traverse(ASTVisitor visitor, BlockScope scope);

	@Override
	public abstract void traverse(ASTVisitor visitor, ClassScope scope);

	public Annotation getPersistibleAnnotation() {
		return this.persistibleAnnotation;      // will be this for non-repeating annotation, the container for the first of the repeating ones and null for the followers.
	}

	public void setPersistibleAnnotation(ContainerAnnotation container) {
		this.persistibleAnnotation = container; // will be a legitimate container for the first of the repeating ones and null for the followers.
	}
}
