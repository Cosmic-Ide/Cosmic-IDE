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
 *		IBM Corporation - initial API and implementation
 *		Stephan Herrmann - Contribution for
 *								bug 400710 - [1.8][compiler] synthetic access to default method generates wrong code
 *								Bug 459967 - [null] compiler should know about nullness of special methods like MyEnum.valueOf()
 *								Bug 470467 - [null] Nullness of special Enum methods not detected from .class file
 *      Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          	Bug 405104 - [1.8][compiler][codegen] Implement support for serializeable lambdas
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.stream.Stream;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LambdaExpression;
import org.eclipse.jdt.internal.compiler.ast.ReferenceExpression;
import org.eclipse.jdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;

public class SyntheticMethodBinding extends MethodBinding {

	public FieldBinding targetReadField;		// read access to a field
	public FieldBinding targetWriteField;		// write access to a field
	public MethodBinding targetMethod;			// method or constructor
	public TypeBinding targetEnumType; 			// enum type
	public LambdaExpression lambda;
	public RecordComponentBinding recordComponentBinding;

	/** Switch (one from many) linked to the switch table */
	public SwitchStatement switchStatement;
	/**
	 * Method reference expression whose target FI is Serializable. Should be set when
	 * purpose is {@link #SerializableMethodReference}
	 */
	public ReferenceExpression serializableMethodRef;
	public int purpose;

	// fields used to generate enum constants when too many
	public int startIndex;
	public int endIndex;

	public final static int FieldReadAccess = 1; 		// field read
	public final static int FieldWriteAccess = 2; 		// field write
	public final static int SuperFieldReadAccess = 3; // super field read
	public final static int SuperFieldWriteAccess = 4; // super field write
	public final static int MethodAccess = 5; 		// normal method
	public final static int ConstructorAccess = 6; 	// constructor
	public final static int SuperMethodAccess = 7; // super method
	public final static int BridgeMethod = 8; // bridge method
	public final static int EnumValues = 9; // enum #values()
	public final static int EnumValueOf = 10; // enum #valueOf(String)
	public final static int SwitchTable = 11; // switch table method
	public final static int TooManyEnumsConstants = 12; // too many enum constants
	public static final int LambdaMethod = 13; // Lambda body emitted as a method.
	public final static int ArrayConstructor = 14; // X[]::new
	public static final int ArrayClone = 15; // X[]::clone
    public static final int FactoryMethod = 16; // for indy call to private constructor.
    public static final int DeserializeLambda = 17; // For supporting lambda deserialization.
    /**
     * Serves as a placeholder for a method reference whose target FI is Serializable.
     * Is never directly materialized in bytecode
     */
    public static final int SerializableMethodReference = 18;
    public static final int RecordOverrideToString = 19;
    public static final int RecordOverrideHashCode = 20;
    public static final int RecordOverrideEquals = 21;
    public static final int RecordCanonicalConstructor = 22;

	public int sourceStart = 0; // start position of the matching declaration
	public int index; // used for sorting access methods in the class file
	public int fakePaddedParameters = 0; // added in synthetic constructor to avoid name clash.

	public SyntheticMethodBinding(FieldBinding targetField, boolean isReadAccess, boolean isSuperAccess, ReferenceBinding declaringClass) {
		this.modifiers = ClassFileConstants.AccDefault | ClassFileConstants.AccStatic | ClassFileConstants.AccSynthetic;
		this.tagBits |= (TagBits.AnnotationResolved | TagBits.DeprecatedAnnotationResolved);
		SourceTypeBinding declaringSourceType = (SourceTypeBinding) declaringClass;
		SyntheticMethodBinding[] knownAccessMethods = declaringSourceType.syntheticMethods();
		int methodId = knownAccessMethods == null ? 0 : knownAccessMethods[knownAccessMethods.length - 1].index + 1; //index may miss some numbers in between. get the highest index and assign next number.;
		this.index = methodId;
		this.selector = CharOperation.concat(TypeConstants.SYNTHETIC_ACCESS_METHOD_PREFIX, String.valueOf(methodId).toCharArray());
		if (isReadAccess) {
			this.returnType = targetField.type;
			if (targetField.isStatic()) {
				this.parameters = Binding.NO_PARAMETERS;
			} else {
				this.parameters = new TypeBinding[1];
				this.parameters[0] = declaringSourceType;
			}
			this.targetReadField = targetField;
			this.purpose = isSuperAccess ? SyntheticMethodBinding.SuperFieldReadAccess : SyntheticMethodBinding.FieldReadAccess;
		} else {
			this.returnType = TypeBinding.VOID;
			if (targetField.isStatic()) {
				this.parameters = new TypeBinding[1];
				this.parameters[0] = targetField.type;
			} else {
				this.parameters = new TypeBinding[2];
				this.parameters[0] = declaringSourceType;
				this.parameters[1] = targetField.type;
			}
			this.targetWriteField = targetField;
			this.purpose = isSuperAccess ? SyntheticMethodBinding.SuperFieldWriteAccess : SyntheticMethodBinding.FieldWriteAccess;
		}
		this.thrownExceptions = Binding.NO_EXCEPTIONS;
		this.declaringClass = declaringSourceType;

		// check for method collision
		boolean needRename;
		do {
			check : {
				needRename = false;
				// check for collision with known methods
				long range;
				MethodBinding[] methods = declaringSourceType.methods();
				if ((range = ReferenceBinding.binarySearch(this.selector, methods)) >= 0) {
					int paramCount = this.parameters.length;
					nextMethod: for (int imethod = (int)range, end = (int)(range >> 32); imethod <= end; imethod++) {
						MethodBinding method = methods[imethod];
						if (method.parameters.length == paramCount) {
							TypeBinding[] toMatch = method.parameters;
							for (int i = 0; i < paramCount; i++) {
								if (TypeBinding.notEquals(toMatch[i], this.parameters[i])) {
									continue nextMethod;
								}
							}
							needRename = true;
							break check;
						}
					}
				}
				// check for collision with synthetic accessors
				if (knownAccessMethods != null) {
					for (int i = 0, length = knownAccessMethods.length; i < length; i++) {
						if (knownAccessMethods[i] == null) continue;
						if (CharOperation.equals(this.selector, knownAccessMethods[i].selector) && areParametersEqual(methods[i])) {
							needRename = true;
							break check;
						}
					}
				}
			}
			if (needRename) { // retry with a selector postfixed by a growing methodId
				setSelector(CharOperation.concat(TypeConstants.SYNTHETIC_ACCESS_METHOD_PREFIX, String.valueOf(++methodId).toCharArray()));
			}
		} while (needRename);
		// retrieve sourceStart position for the target field for line number attributes
		FieldDeclaration[] fieldDecls = declaringSourceType.scope.referenceContext.fields;
		if (fieldDecls != null) {
			for (int i = 0, max = fieldDecls.length; i < max; i++) {
				if (fieldDecls[i].binding == targetField) {
					this.sourceStart = fieldDecls[i].sourceStart;
					return;
				}
			}
		}

	/* did not find the target field declaration - it is a synthetic one
		public class A {
			public class B {
				public class C {
					void foo() {
						System.out.println("A.this = " + A.this);
					}
				}
			}
			public static void main(String args[]) {
				new A().new B().new C().foo();
			}
		}
	*/
		// We now at this point - per construction - it is for sure an enclosing instance, we are going to
		// show the target field type declaration location.
		this.sourceStart = declaringSourceType.scope.referenceContext.sourceStart; // use the target declaring class name position instead
	}

	public SyntheticMethodBinding(FieldBinding targetField, ReferenceBinding declaringClass, TypeBinding enumBinding, char[] selector,  SwitchStatement switchStatement) {
		this.modifiers = (declaringClass.isInterface() ? ClassFileConstants.AccPublic : ClassFileConstants.AccDefault) | ClassFileConstants.AccStatic | ClassFileConstants.AccSynthetic;
		this.tagBits |= (TagBits.AnnotationResolved | TagBits.DeprecatedAnnotationResolved);
		SourceTypeBinding declaringSourceType = (SourceTypeBinding) declaringClass;
		SyntheticMethodBinding[] knownAccessMethods = declaringSourceType.syntheticMethods();
		int methodId = knownAccessMethods == null ? 0 : knownAccessMethods[knownAccessMethods.length - 1].index + 1; //index may miss some numbers in between. get the highest index and assign next number.;
		this.index = methodId;
		this.selector = selector;
		this.returnType = declaringSourceType.scope.createArrayType(TypeBinding.INT, 1);
		this.parameters = Binding.NO_PARAMETERS;
		this.targetReadField = targetField;
		this.targetEnumType = enumBinding;
		this.purpose = SyntheticMethodBinding.SwitchTable;
		this.thrownExceptions = Binding.NO_EXCEPTIONS;
		this.declaringClass = declaringSourceType;
		this.switchStatement = switchStatement;
		if (declaringSourceType.isStrictfp()) {
			this.modifiers |= ClassFileConstants.AccStrictfp;
		}
		// check for method collision
		boolean needRename;
		do {
			check : {
				needRename = false;
				// check for collision with known methods
				long range;
				MethodBinding[] methods = declaringSourceType.methods();
				if ((range = ReferenceBinding.binarySearch(this.selector, methods)) >= 0) {
					int paramCount = this.parameters.length;
					nextMethod: for (int imethod = (int)range, end = (int)(range >> 32); imethod <= end; imethod++) {
						MethodBinding method = methods[imethod];
						if (method.parameters.length == paramCount) {
							TypeBinding[] toMatch = method.parameters;
							for (int i = 0; i < paramCount; i++) {
								if (TypeBinding.notEquals(toMatch[i], this.parameters[i])) {
									continue nextMethod;
								}
							}
							needRename = true;
							break check;
						}
					}
				}
				// check for collision with synthetic accessors
				if (knownAccessMethods != null) {
					for (int i = 0, length = knownAccessMethods.length; i < length; i++) {
						if (knownAccessMethods[i] == null) continue;
						if (CharOperation.equals(this.selector, knownAccessMethods[i].selector) && areParametersEqual(methods[i])) {
							needRename = true;
							break check;
						}
					}
				}
			}
			if (needRename) { // retry with a selector postfixed by a growing methodId
				setSelector(CharOperation.concat(selector, String.valueOf(++methodId).toCharArray()));
			}
		} while (needRename);

		// We now at this point - per construction - it is for sure an enclosing instance, we are going to
		// show the target field type declaration location.
		this.sourceStart = declaringSourceType.scope.referenceContext.sourceStart; // use the target declaring class name position instead
	}

	public SyntheticMethodBinding(MethodBinding targetMethod, boolean isSuperAccess, ReferenceBinding declaringClass) {

		if (targetMethod.isConstructor()) {
			initializeConstructorAccessor(targetMethod);
		} else {
			initializeMethodAccessor(targetMethod, isSuperAccess, declaringClass);
		}
	}

	/**
	 * Construct a bridge method
	 */
	public SyntheticMethodBinding(MethodBinding overridenMethodToBridge, MethodBinding targetMethod, SourceTypeBinding declaringClass) {

	    this.declaringClass = declaringClass;
	    this.selector = overridenMethodToBridge.selector;
	    // amongst other, clear the AccGenericSignature, so as to ensure no remains of original inherited persist (101794)
	    // also use the modifiers from the target method, as opposed to inherited one (147690)
	    this.modifiers = (targetMethod.modifiers | ClassFileConstants.AccBridge | ClassFileConstants.AccSynthetic) & ~(ClassFileConstants.AccSynchronized | ClassFileConstants.AccAbstract | ClassFileConstants.AccNative  | ClassFileConstants.AccFinal | ExtraCompilerModifiers.AccGenericSignature);
		this.tagBits |= (TagBits.AnnotationResolved | TagBits.DeprecatedAnnotationResolved);
	    this.returnType = overridenMethodToBridge.returnType;
	    this.parameters = overridenMethodToBridge.parameters;
	    this.thrownExceptions = overridenMethodToBridge.thrownExceptions;
	    this.targetMethod = targetMethod;
	    this.purpose = SyntheticMethodBinding.BridgeMethod;
		this.index = nextSmbIndex();
	}

	/**
	 * Construct enum special methods: values or valueOf methods
	 */
	public SyntheticMethodBinding(SourceTypeBinding declaringEnum, char[] selector) {
	    this.declaringClass = declaringEnum;
	    this.selector = selector;
	    this.modifiers = ClassFileConstants.AccPublic | ClassFileConstants.AccStatic;
		this.tagBits |= (TagBits.AnnotationResolved | TagBits.DeprecatedAnnotationResolved);
		LookupEnvironment environment = declaringEnum.scope.environment();
	    this.thrownExceptions = Binding.NO_EXCEPTIONS;
		if (selector == TypeConstants.VALUES) {
		    this.returnType = environment.createArrayType(environment.convertToParameterizedType(declaringEnum), 1);
		    this.parameters = Binding.NO_PARAMETERS;
		    this.purpose = SyntheticMethodBinding.EnumValues;
		} else if (selector == TypeConstants.VALUEOF) {
		    this.returnType = environment.convertToParameterizedType(declaringEnum);
		    this.parameters = new TypeBinding[]{ declaringEnum.scope.getJavaLangString() };
		    this.purpose = SyntheticMethodBinding.EnumValueOf;
		}
		this.index = nextSmbIndex();
		if (declaringEnum.isStrictfp()) {
			this.modifiers |= ClassFileConstants.AccStrictfp;
		}
	}

	private int nextSmbIndex() {
		SyntheticMethodBinding[] knownAccessMethods = ((SourceTypeBinding)this.declaringClass).syntheticMethods();
		int methodId = knownAccessMethods == null ? 0 : knownAccessMethods[knownAccessMethods.length - 1].index + 1; //index may miss some numbers in between. get the highest index and assign next number.;
		return methodId;
	}

	/**
	 * Construct $deserializeLambda$ method
	 */
	public SyntheticMethodBinding(SourceTypeBinding declaringClass) {
		this.declaringClass = declaringClass;
		this.selector = TypeConstants.DESERIALIZE_LAMBDA;
		this.modifiers = ClassFileConstants.AccPrivate | ClassFileConstants.AccStatic | ClassFileConstants.AccSynthetic;
		this.tagBits |= (TagBits.AnnotationResolved | TagBits.DeprecatedAnnotationResolved);
		this.thrownExceptions = Binding.NO_EXCEPTIONS;
		this.returnType = declaringClass.scope.getJavaLangObject();
	    this.parameters = new TypeBinding[]{declaringClass.scope.getJavaLangInvokeSerializedLambda()};
	    this.purpose = SyntheticMethodBinding.DeserializeLambda;
		this.index = nextSmbIndex();
	}

	/**
	 * Construct enum special methods: values or valueOf methods
	 */
	public SyntheticMethodBinding(SourceTypeBinding declaringEnum, int startIndex, int endIndex) {
		this.declaringClass = declaringEnum;
		this.index = nextSmbIndex();
		StringBuffer buffer = new StringBuffer();
		buffer.append(TypeConstants.SYNTHETIC_ENUM_CONSTANT_INITIALIZATION_METHOD_PREFIX).append(this.index);
		this.selector = String.valueOf(buffer).toCharArray();
		this.modifiers = ClassFileConstants.AccPrivate | ClassFileConstants.AccStatic;
		this.tagBits |= (TagBits.AnnotationResolved | TagBits.DeprecatedAnnotationResolved);
		this.purpose = SyntheticMethodBinding.TooManyEnumsConstants;
		this.thrownExceptions = Binding.NO_EXCEPTIONS;
		this.returnType = TypeBinding.VOID;
		this.parameters = Binding.NO_PARAMETERS;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
	}

	// Create a synthetic method that will simply call the super classes method.
	// Used when a public method is inherited from a non-public class into a public class.
	// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=288658
	// Also applies for inherited default methods with the same visibility issue.
	// See https://bugs.eclipse.org/400710
	public SyntheticMethodBinding(MethodBinding overridenMethodToBridge, SourceTypeBinding declaringClass) {

	    this.declaringClass = declaringClass;
	    this.selector = overridenMethodToBridge.selector;
	    // amongst other, clear the AccGenericSignature, so as to ensure no remains of original inherited persist (101794)
	    this.modifiers = (overridenMethodToBridge.modifiers | ClassFileConstants.AccBridge | ClassFileConstants.AccSynthetic) & ~(ClassFileConstants.AccSynchronized | ClassFileConstants.AccAbstract | ClassFileConstants.AccNative  | ClassFileConstants.AccFinal | ExtraCompilerModifiers.AccGenericSignature);
		this.tagBits |= (TagBits.AnnotationResolved | TagBits.DeprecatedAnnotationResolved);
	    this.returnType = overridenMethodToBridge.returnType;
	    this.parameters = overridenMethodToBridge.parameters;
	    this.thrownExceptions = overridenMethodToBridge.thrownExceptions;
	    this.targetMethod = overridenMethodToBridge;
	    this.purpose = SyntheticMethodBinding.SuperMethodAccess;
		this.index = nextSmbIndex();
	}

	public SyntheticMethodBinding(int purpose, ArrayBinding arrayType, char [] selector, SourceTypeBinding declaringClass) {
	    this.declaringClass = declaringClass;
	    this.selector = selector;
	    this.modifiers = ClassFileConstants.AccSynthetic | ClassFileConstants.AccPrivate | ClassFileConstants.AccStatic;
		this.tagBits |= (TagBits.AnnotationResolved | TagBits.DeprecatedAnnotationResolved);
	    this.returnType = arrayType;
	    LookupEnvironment environment = declaringClass.environment;
		if (environment.globalOptions.isAnnotationBasedNullAnalysisEnabled) {
			// mark X[]::new and X[]::clone as returning 'X @NonNull' (don't wait (cf. markNonNull()), because we're called as late as codeGen):
	    	if (environment.usesNullTypeAnnotations())
	    		this.returnType = environment.createAnnotatedType(this.returnType, new AnnotationBinding[]{ environment.getNonNullAnnotation() });
	    	else
	    		this.tagBits |= TagBits.AnnotationNonNull;
	    }
	    this.parameters = new TypeBinding[] { purpose == SyntheticMethodBinding.ArrayConstructor ? TypeBinding.INT : (TypeBinding) arrayType};
	    this.thrownExceptions = Binding.NO_EXCEPTIONS;
	    this.purpose = purpose;
		this.index = nextSmbIndex();
	}

	public SyntheticMethodBinding(LambdaExpression lambda, char [] lambdaName, SourceTypeBinding declaringClass) {
		this.lambda = lambda;
	    this.declaringClass = declaringClass;
	    this.selector = lambdaName;
	    this.modifiers = lambda.binding.modifiers;
		this.tagBits |= (TagBits.AnnotationResolved | TagBits.DeprecatedAnnotationResolved) | (lambda.binding.tagBits & TagBits.HasParameterAnnotations);
	    this.returnType = lambda.binding.returnType;
	    this.parameters = lambda.binding.parameters;
	    TypeVariableBinding[] vars = Stream.of(this.parameters).filter(param -> param.isTypeVariable()).toArray(TypeVariableBinding[]::new);
	    if (vars != null && vars.length > 0)
	    	this.typeVariables = vars;
	    this.thrownExceptions = lambda.binding.thrownExceptions;
	    this.purpose = SyntheticMethodBinding.LambdaMethod;
		this.index = nextSmbIndex();
	}

	public SyntheticMethodBinding(ReferenceExpression ref, SourceTypeBinding declaringClass) {
		this.serializableMethodRef = ref;
	    this.declaringClass = declaringClass;
	    this.selector = ref.binding.selector;
	    this.modifiers = ref.binding.modifiers;
		this.tagBits |= (TagBits.AnnotationResolved | TagBits.DeprecatedAnnotationResolved) | (ref.binding.tagBits & TagBits.HasParameterAnnotations);
	    this.returnType = ref.binding.returnType;
	    this.parameters = ref.binding.parameters;
	    this.thrownExceptions = ref.binding.thrownExceptions;
	    this.purpose = SyntheticMethodBinding.SerializableMethodReference;
		this.index = nextSmbIndex();
	}

	public SyntheticMethodBinding(MethodBinding privateConstructor, MethodBinding publicConstructor, char[] selector, TypeBinding[] enclosingInstances, SourceTypeBinding declaringClass) {
	    this.declaringClass = declaringClass;
	    this.selector = selector;
	    this.modifiers = ClassFileConstants.AccSynthetic | ClassFileConstants.AccPrivate | ClassFileConstants.AccStatic;
		this.tagBits |= (TagBits.AnnotationResolved | TagBits.DeprecatedAnnotationResolved);
	    this.returnType = publicConstructor.declaringClass;

	    int realParametersLength = privateConstructor.parameters.length;
	    int enclosingInstancesLength = enclosingInstances.length;
	    int parametersLength =  enclosingInstancesLength + realParametersLength;
	    this.parameters = new TypeBinding[parametersLength];
	    System.arraycopy(enclosingInstances, 0, this.parameters, 0, enclosingInstancesLength);
	    System.arraycopy(privateConstructor.parameters, 0, this.parameters, enclosingInstancesLength, realParametersLength);
	    this.fakePaddedParameters = publicConstructor.parameters.length - realParametersLength;

	    this.thrownExceptions = publicConstructor.thrownExceptions;
	    this.purpose = SyntheticMethodBinding.FactoryMethod;
	    this.targetMethod = publicConstructor;
		this.index = nextSmbIndex();
	}

	public SyntheticMethodBinding(ReferenceBinding declaringClass, RecordComponentBinding[] rcb) {
		SourceTypeBinding declaringSourceType = (SourceTypeBinding) declaringClass;
		assert declaringSourceType.isRecord();
		this.declaringClass = declaringSourceType;
		this.modifiers = declaringClass.modifiers & (ClassFileConstants.AccPublic|ClassFileConstants.AccPrivate|ClassFileConstants.AccProtected);
		if (this.declaringClass.isStrictfp())
			this.modifiers |= ClassFileConstants.AccStrictfp;
		this.tagBits |= (TagBits.AnnotationResolved | TagBits.DeprecatedAnnotationResolved);
		this.extendedTagBits |= ExtendedTagBits.IsCanonicalConstructor;
		this.extendedTagBits |= ExtendedTagBits.isImplicit;
		this.parameters = rcb.length == 0 ? Binding.NO_PARAMETERS : new TypeBinding[rcb.length];
		for (int i = 0; i < rcb.length; i++) this.parameters[i] = TypeBinding.VOID; // placeholder
		this.selector = TypeConstants.INIT;
		this.returnType = TypeBinding.VOID;
		this.purpose = SyntheticMethodBinding.RecordCanonicalConstructor;
		this.thrownExceptions = Binding.NO_EXCEPTIONS;
		this.declaringClass = declaringSourceType;
		this.index = nextSmbIndex();
	}
	public SyntheticMethodBinding(ReferenceBinding declaringClass, RecordComponentBinding rcb, int index) {
		SourceTypeBinding declaringSourceType = (SourceTypeBinding) declaringClass;
		assert declaringSourceType.isRecord();
		this.declaringClass = declaringSourceType;
		this.modifiers = ClassFileConstants.AccPublic;
		// rcb not resolved fully yet - to be filled in later - see STB.components()
//		if (rcb.type instanceof TypeVariableBinding ||
//				rcb.type instanceof ParameterizedTypeBinding)
//			this.modifiers |= ExtraCompilerModifiers.AccGenericSignature;
		if (this.declaringClass.isStrictfp())
			this.modifiers |= ClassFileConstants.AccStrictfp;
		this.tagBits |= (TagBits.AnnotationResolved | TagBits.DeprecatedAnnotationResolved);
		this.parameters = Binding.NO_PARAMETERS;
//		this.returnType = rcb.type; Not resolved yet - to be filled in later
		this.selector = rcb.name;
		this.recordComponentBinding = rcb;
//		this.targetReadField = ??; // not fully resolved yet - to be filled in later
		this.purpose = SyntheticMethodBinding.FieldReadAccess;
		this.thrownExceptions = Binding.NO_EXCEPTIONS;
		this.declaringClass = declaringSourceType;
		this.index = nextSmbIndex();
		this.sourceStart = rcb.sourceRecordComponent().sourceStart;
	}
	public SyntheticMethodBinding(ReferenceBinding declaringClass, char[] selector, int index) {
		SourceTypeBinding declaringSourceType = (SourceTypeBinding) declaringClass;
		assert declaringSourceType.isRecord();
		this.declaringClass = declaringSourceType;
		this.modifiers = ClassFileConstants.AccPublic | ClassFileConstants.AccFinal;
		if (this.declaringClass.isStrictfp())
				this.modifiers |= ClassFileConstants.AccStrictfp;
		this.tagBits |= (TagBits.AnnotationResolved | TagBits.DeprecatedAnnotationResolved);
	    this.selector = selector;
	    this.thrownExceptions = Binding.NO_EXCEPTIONS;
		if (selector == TypeConstants.TOSTRING) {
			this.returnType = declaringSourceType.scope.getJavaLangString();
		    this.parameters = Binding.NO_PARAMETERS;
		    this.purpose = SyntheticMethodBinding.RecordOverrideToString;
		} else if (selector == TypeConstants.HASHCODE) {
			this.returnType = TypeBinding.INT;
		    this.parameters = Binding.NO_PARAMETERS;
		    this.purpose = SyntheticMethodBinding.RecordOverrideHashCode;
		} else if (selector == TypeConstants.EQUALS) {
			this.returnType = TypeBinding.BOOLEAN;
		    this.parameters = new TypeBinding[] {declaringSourceType.scope.getJavaLangObject()};
		    this.purpose = SyntheticMethodBinding.RecordOverrideEquals;
		}
		this.index = nextSmbIndex();
	}
	/**
	 * An constructor accessor is a constructor with an extra argument (declaringClass), in case of
	 * collision with an existing constructor, then add again an extra argument (declaringClass again).
	 */
	 public void initializeConstructorAccessor(MethodBinding accessedConstructor) {

		this.targetMethod = accessedConstructor;
		this.modifiers = ClassFileConstants.AccDefault | ClassFileConstants.AccSynthetic;
		this.tagBits |= (TagBits.AnnotationResolved | TagBits.DeprecatedAnnotationResolved);
		SourceTypeBinding sourceType = (SourceTypeBinding) accessedConstructor.declaringClass;
		SyntheticMethodBinding[] knownSyntheticMethods = sourceType.syntheticMethods();   // returns synthetic methods sorted with index.
		this.index = knownSyntheticMethods == null ? 0 : knownSyntheticMethods[knownSyntheticMethods.length - 1].index + 1; //index may miss some numbers in between. get the highest index and assign next number.

		this.selector = accessedConstructor.selector;
		this.returnType = accessedConstructor.returnType;
		this.purpose = SyntheticMethodBinding.ConstructorAccess;
		final int parametersLength = accessedConstructor.parameters.length;
		this.parameters = new TypeBinding[parametersLength + 1];
		System.arraycopy(
			accessedConstructor.parameters,
			0,
			this.parameters,
			0,
			parametersLength);
		this.parameters[parametersLength] =
			accessedConstructor.declaringClass;
		this.thrownExceptions = accessedConstructor.thrownExceptions;
		this.declaringClass = sourceType;

		// check for method collision
		boolean needRename;
		do {
			check : {
				needRename = false;
				// check for collision with known methods
				MethodBinding[] methods = sourceType.methods();
				for (int i = 0, length = methods.length; i < length; i++) {
					if (CharOperation.equals(this.selector, methods[i].selector) && areParameterErasuresEqual(methods[i])) {
						needRename = true;
						break check;
					}
				}
				// check for collision with synthetic accessors
				if (knownSyntheticMethods != null) {
					for (int i = 0, length = knownSyntheticMethods.length; i < length; i++) {
						if (knownSyntheticMethods[i] == null)
							continue;
						if (CharOperation.equals(this.selector, knownSyntheticMethods[i].selector) && areParameterErasuresEqual(knownSyntheticMethods[i])) {
							needRename = true;
							break check;
						}
					}
				}
			}
			if (needRename) { // retry with a new extra argument
				int length = this.parameters.length;
				System.arraycopy(
					this.parameters,
					0,
					this.parameters = new TypeBinding[length + 1],
					0,
					length);
				this.parameters[length] = this.declaringClass;
			}
		} while (needRename);

		// retrieve sourceStart position for the target method for line number attributes
		AbstractMethodDeclaration[] methodDecls =
			sourceType.scope.referenceContext.methods;
		if (methodDecls != null) {
			for (int i = 0, length = methodDecls.length; i < length; i++) {
				if (methodDecls[i].binding == accessedConstructor) {
					this.sourceStart = methodDecls[i].sourceStart;
					return;
				}
			}
		}
	}

	/**
	 * An method accessor is a method with an access$N selector, where N is incremented in case of collisions.
	 */
	public void initializeMethodAccessor(MethodBinding accessedMethod, boolean isSuperAccess, ReferenceBinding receiverType) {

		this.targetMethod = accessedMethod;
		if (isSuperAccess && receiverType.isInterface() && !accessedMethod.isStatic())
			this.modifiers = ClassFileConstants.AccPrivate | ClassFileConstants.AccSynthetic;
		else {
			if (receiverType.isInterface()) // default is not allowed. TODO: do we need a target level check here?
				this.modifiers = ClassFileConstants.AccPublic | ClassFileConstants.AccStatic | ClassFileConstants.AccSynthetic;
			else
				this.modifiers = ClassFileConstants.AccDefault | ClassFileConstants.AccStatic | ClassFileConstants.AccSynthetic;
		}
		this.tagBits |= (TagBits.AnnotationResolved | TagBits.DeprecatedAnnotationResolved);
		SourceTypeBinding declaringSourceType = (SourceTypeBinding) receiverType;
		SyntheticMethodBinding[] knownAccessMethods = declaringSourceType.syntheticMethods();
		int methodId = knownAccessMethods == null ? 0 : knownAccessMethods[knownAccessMethods.length - 1].index + 1; //index may miss some numbers in between. get the highest index and assign next number.
		this.index = methodId;

		this.selector = CharOperation.concat(TypeConstants.SYNTHETIC_ACCESS_METHOD_PREFIX, String.valueOf(methodId).toCharArray());
		this.returnType = accessedMethod.returnType;
		this.purpose = isSuperAccess ? SyntheticMethodBinding.SuperMethodAccess : SyntheticMethodBinding.MethodAccess;

		if (accessedMethod.isStatic() || (isSuperAccess && receiverType.isInterface())) {
			this.parameters = accessedMethod.parameters;
		} else {
			this.parameters = new TypeBinding[accessedMethod.parameters.length + 1];
			this.parameters[0] = declaringSourceType;
			System.arraycopy(accessedMethod.parameters, 0, this.parameters, 1, accessedMethod.parameters.length);
		}
		this.thrownExceptions = accessedMethod.thrownExceptions;
		this.declaringClass = declaringSourceType;

		// check for method collision
		boolean needRename;
		do {
			check : {
				needRename = false;
				// check for collision with known methods
				MethodBinding[] methods = declaringSourceType.methods();
				for (int i = 0, length = methods.length; i < length; i++) {
					if (CharOperation.equals(this.selector, methods[i].selector) && areParameterErasuresEqual(methods[i])) {
						needRename = true;
						break check;
					}
				}
				// check for collision with synthetic accessors
				if (knownAccessMethods != null) {
					for (int i = 0, length = knownAccessMethods.length; i < length; i++) {
						if (knownAccessMethods[i] == null) continue;
						if (CharOperation.equals(this.selector, knownAccessMethods[i].selector) && areParameterErasuresEqual(knownAccessMethods[i])) {
							needRename = true;
							break check;
						}
					}
				}
			}
			if (needRename) { // retry with a selector & a growing methodId
				setSelector(CharOperation.concat(TypeConstants.SYNTHETIC_ACCESS_METHOD_PREFIX, String.valueOf(++methodId).toCharArray()));
			}
		} while (needRename);

		// retrieve sourceStart position for the target method for line number attributes
		AbstractMethodDeclaration[] methodDecls = declaringSourceType.scope.referenceContext.methods;
		if (methodDecls != null) {
			for (int i = 0, length = methodDecls.length; i < length; i++) {
				if (methodDecls[i].binding == accessedMethod) {
					this.sourceStart = methodDecls[i].sourceStart;
					return;
				}
			}
		}
	}

	protected boolean isConstructorRelated() {
		return this.purpose == SyntheticMethodBinding.ConstructorAccess;
	}

	@Override
	public LambdaExpression sourceLambda() {
		return this.lambda;
	}

	public void markNonNull(LookupEnvironment environment) {
		markNonNull(this, this.purpose, environment);
	}

	static void markNonNull(MethodBinding method, int purpose, LookupEnvironment environment) {
		// deferred update of the return type
	    switch (purpose) {
			case EnumValues:
				if (environment.usesNullTypeAnnotations()) {
					TypeBinding elementType = ((ArrayBinding)method.returnType).leafComponentType();
					AnnotationBinding nonNullAnnotation = environment.getNonNullAnnotation();
					elementType = environment.createAnnotatedType(elementType, new AnnotationBinding[]{ environment.getNonNullAnnotation() });
					method.returnType = environment.createArrayType(elementType, 1, new AnnotationBinding[]{ nonNullAnnotation, null });
				} else {
					method.tagBits |= TagBits.AnnotationNonNull;
				}
				return;
			case EnumValueOf:
				if (environment.usesNullTypeAnnotations()) {
					method.returnType = environment.createAnnotatedType(method.returnType, new AnnotationBinding[]{ environment.getNonNullAnnotation() });
				} else {
					method.tagBits |= TagBits.AnnotationNonNull;
				}
				return;
		}
	}
	@Override
	public void setAnnotations(AnnotationBinding[] annotations, Scope scope, boolean forceStore) {
		if (this.declaringClass.isRecord() && (!this.isVarargs())) {
			for (AnnotationBinding annot: annotations) {
				if ((annot.getAnnotationType().id == TypeIds.T_JavaLangSafeVarargs)) {
					scope.problemReporter().safeVarargsOnOnSyntheticRecordAccessor(this.recordComponentBinding.sourceRecordComponent());
				}
			}
		}
		setAnnotations(annotations, forceStore);
	}

}
