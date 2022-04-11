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
 *								bug 349326 - [1.7] new warning for missing try-with-resources
 *								bug 186342 - [compiler][null] Using annotations for null checking
 *								bug 364890 - BinaryTypeBinding should use char constants from Util
 *								bug 365387 - [compiler][null] bug 186342: Issues to follow up post review and verification.
 *								bug 358903 - Filter practically unimportant resource leak warnings
 *								bug 365531 - [compiler][null] investigate alternative strategy for internally encoding nullness defaults
 *								bug 388800 - [1.8][compiler] detect default methods in class files
 *								bug 388281 - [compiler][null] inheritance of null annotations as an option
 *								bug 331649 - [compiler][null] consider null annotations for fields
 *								bug 392384 - [1.8][compiler][null] Restore nullness info from type annotations in class files
 *								Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								Bug 415043 - [1.8][null] Follow-up re null type annotations after bug 392099
 *								Bug 415850 - [1.8] Ensure RunJDTCoreTests can cope with null annotations enabled
 *								Bug 417295 - [1.8[[null] Massage type annotated null analysis to gel well with deep encoded type bindings.
 *								Bug 427199 - [1.8][resource] avoid resource leak warnings on Streams that have no resource
 *								Bug 392245 - [1.8][compiler][null] Define whether / how @NonNullByDefault applies to TYPE_USE locations
 *								Bug 429958 - [1.8][null] evaluate new DefaultLocation attribute of @NonNullByDefault
 *								Bug 390889 - [1.8][compiler] Evaluate options to support 1.7- projects against 1.8 JRE.
 *								Bug 438458 - [1.8][null] clean up handling of null type annotations wrt type variables
 *								Bug 439516 - [1.8][null] NonNullByDefault wrongly applied to implicit type bound of binary type
 *								Bug 434602 - Possible error with inferred null annotations leading to contradictory null annotations
 *								Bug 440477 - [null] Infrastructure for feeding external annotations into compilation
 *								Bug 441693 - [1.8][null] Bogus warning for type argument annotated with @NonNull
 *								Bug 435805 - [1.8][compiler][null] Java 8 compiler does not recognize declaration style null annotations
 *								Bug 453475 - [1.8][null] Contradictory null annotations (4.5 M3 edition)
 *								Bug 454182 - Internal compiler error when using 1.8 compliance for simple project
 *								Bug 470467 - [null] Nullness of special Enum methods not detected from .class file
 *								Bug 447661 - [1.8][null] Incorrect 'expression needs unchecked conversion' warning
 *    Jesper Steen Moller - Contributions for
 *								Bug 412150 [1.8] [compiler] Enable reflected parameter names during annotation processing
 *								Bug 412153 - [1.8][compiler] Check validity of annotations which may be repeatable
 *    Sebastian Zarnekow - Contributions for
 *								bug 544921 - [performance] Poor performance with large source files
 *    Alexander Lehmann - Contributions for
 *								bug 566258 - Intermittent NPE in APT RoundDispatcher
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.ArrayList;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.classfmt.AnnotationInfo;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.classfmt.ExternalAnnotationProvider.IMethodAnnotationWalker;
import org.eclipse.jdt.internal.compiler.classfmt.MethodInfoWithAnnotations;
import org.eclipse.jdt.internal.compiler.classfmt.NonNullDefaultAwareTypeAnnotationWalker;
import org.eclipse.jdt.internal.compiler.classfmt.TypeAnnotationWalker;
import org.eclipse.jdt.internal.compiler.codegen.ConstantPool;
import org.eclipse.jdt.internal.compiler.env.ClassSignature;
import org.eclipse.jdt.internal.compiler.env.EnumConstantSignature;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryElementValuePair;
import org.eclipse.jdt.internal.compiler.env.IBinaryField;
import org.eclipse.jdt.internal.compiler.env.IBinaryMethod;
import org.eclipse.jdt.internal.compiler.env.IBinaryNestedType;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.IBinaryTypeAnnotation;
import org.eclipse.jdt.internal.compiler.env.IRecordComponent;
import org.eclipse.jdt.internal.compiler.env.ITypeAnnotationWalker;
import org.eclipse.jdt.internal.compiler.impl.BooleanConstant;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.jdt.internal.compiler.util.Util;

/*
Not all fields defined by this type are initialized when it is created.
Some are initialized only when needed.

Accessors have been provided for some public fields so all TypeBindings have the same API...
but access public fields directly whenever possible.
Non-public fields have accessors which should be used everywhere you expect the field to be initialized.

null is NOT a valid value for a non-public field... it just means the field is not initialized.
*/

@SuppressWarnings({ "rawtypes", "unchecked" })
public class BinaryTypeBinding extends ReferenceBinding {

	public static final char[] TYPE_QUALIFIER_DEFAULT = "TypeQualifierDefault".toCharArray(); //$NON-NLS-1$

	private static final IBinaryMethod[] NO_BINARY_METHODS = new IBinaryMethod[0];

	// all of these fields are ONLY guaranteed to be initialized if accessed using their public accessor method
	protected ReferenceBinding superclass;
	protected ReferenceBinding enclosingType;
	protected ReferenceBinding[] superInterfaces;
	protected ReferenceBinding[] permittedSubtypes;
	protected FieldBinding[] fields;
	protected RecordComponentBinding[] components;
	protected MethodBinding[] methods;
	protected ReferenceBinding[] memberTypes;
	protected TypeVariableBinding[] typeVariables;
	protected ModuleBinding module;
	private BinaryTypeBinding prototype;

	// For the link with the principle structure
	protected LookupEnvironment environment;

	protected SimpleLookupTable storedAnnotations = null; // keys are this ReferenceBinding & its fields and methods, value is an AnnotationHolder

	private ReferenceBinding containerAnnotationType;
	int defaultNullness = 0;
	boolean memberTypesSorted = false;
	public enum ExternalAnnotationStatus {
		FROM_SOURCE,
		NOT_EEA_CONFIGURED,
		NO_EEA_FILE,
		TYPE_IS_ANNOTATED;
		public boolean isPotentiallyUnannotatedLib() {
			switch (this) {
				case FROM_SOURCE:
				case TYPE_IS_ANNOTATED:
					return false;
				default:
					return true;
			}
		}
	}
	public ExternalAnnotationStatus externalAnnotationStatus = ExternalAnnotationStatus.NOT_EEA_CONFIGURED; // unless proven differently

static Object convertMemberValue(Object binaryValue, LookupEnvironment env, char[][][] missingTypeNames, boolean resolveEnumConstants) {
	if (binaryValue == null) return null;
	if (binaryValue instanceof Constant)
		return binaryValue;
	if (binaryValue instanceof ClassSignature)
		return env.getTypeFromSignature(((ClassSignature) binaryValue).getTypeName(), 0, -1, false, null, missingTypeNames, ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER);
	if (binaryValue instanceof IBinaryAnnotation)
		return createAnnotation((IBinaryAnnotation) binaryValue, env, missingTypeNames);
	if (binaryValue instanceof EnumConstantSignature) {
		EnumConstantSignature ref = (EnumConstantSignature) binaryValue;
		ReferenceBinding enumType = (ReferenceBinding) env.getTypeFromSignature(ref.getTypeName(), 0, -1, false, null, missingTypeNames, ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER);
		if (enumType.isUnresolvedType() && !resolveEnumConstants)
			return new ElementValuePair.UnresolvedEnumConstant(enumType, env, ref.getEnumConstantName());
		enumType = (ReferenceBinding) resolveType(enumType, env, false /* no raw conversion */);
		return enumType.getField(ref.getEnumConstantName(), false);
	}
	if (binaryValue instanceof Object[]) {
		Object[] objects = (Object[]) binaryValue;
		int length = objects.length;
		if (length == 0) return objects;
		Object[] values = new Object[length];
		for (int i = 0; i < length; i++)
			values[i] = convertMemberValue(objects[i], env, missingTypeNames, resolveEnumConstants);
		return values;
	}

	// should never reach here.
	throw new IllegalStateException();
}

@Override
public TypeBinding clone(TypeBinding outerType) {
	BinaryTypeBinding copy = new BinaryTypeBinding(this);
	copy.enclosingType = (ReferenceBinding) outerType;

	/* BinaryTypeBinding construction is not "atomic" and is split between the constructor and cachePartsFrom and between the two
	   stages of construction, clone can kick in when LookupEnvironment.createBinaryTypeFrom calls PackageBinding.addType. This
	   can result in some URB's being resolved, which could trigger the clone call, leaving the clone with semi-initialized prototype.
	   Fortunately, the protocol for this type demands all clients to use public access methods, where we can deflect the call to the
	   prototype. enclosingType() and memberTypes() should not delegate, so ...
	*/
	if (copy.enclosingType != null)
		copy.tagBits |= TagBits.HasUnresolvedEnclosingType;
	else
		copy.tagBits &= ~TagBits.HasUnresolvedEnclosingType;

	copy.tagBits |= TagBits.HasUnresolvedMemberTypes;
	return copy;
}

static AnnotationBinding createAnnotation(IBinaryAnnotation annotationInfo, LookupEnvironment env, char[][][] missingTypeNames) {
	// temporary debug for Bug 532176 - [10] NPE during reconcile
	if (annotationInfo instanceof AnnotationInfo) {
		RuntimeException ex = ((AnnotationInfo) annotationInfo).exceptionDuringDecode;
		if (ex != null)
			new IllegalStateException("Accessing annotation with decode error", ex).printStackTrace(); //$NON-NLS-1$
	}
	//--
	IBinaryElementValuePair[] binaryPairs = annotationInfo.getElementValuePairs();
	int length = binaryPairs == null ? 0 : binaryPairs.length;
	ElementValuePair[] pairs = length == 0 ? Binding.NO_ELEMENT_VALUE_PAIRS : new ElementValuePair[length];
	for (int i = 0; i < length; i++)
		pairs[i] = new ElementValuePair(binaryPairs[i].getName(), convertMemberValue(binaryPairs[i].getValue(), env, missingTypeNames, false), null);

	char[] typeName = annotationInfo.getTypeName();
	LookupEnvironment env2 = annotationInfo.isExternalAnnotation() ? env.root : env;
	ReferenceBinding annotationType = env2.getTypeFromConstantPoolName(typeName, 1, typeName.length - 1, false,
			missingTypeNames);
	return env2.createUnresolvedAnnotation(annotationType, pairs);
}

public static AnnotationBinding[] createAnnotations(IBinaryAnnotation[] annotationInfos, LookupEnvironment env, char[][][] missingTypeNames) {
	int length = annotationInfos == null ? 0 : annotationInfos.length;
	AnnotationBinding[] result = length == 0 ? Binding.NO_ANNOTATIONS : new AnnotationBinding[length];
	for (int i = 0; i < length; i++)
		result[i] = createAnnotation(annotationInfos[i], env, missingTypeNames);
	return result;
}

public static TypeBinding resolveType(TypeBinding type, LookupEnvironment environment, boolean convertGenericToRawType) {
	switch (type.kind()) {
		case Binding.PARAMETERIZED_TYPE :
			((ParameterizedTypeBinding) type).resolve();
			break;

		case Binding.WILDCARD_TYPE :
		case Binding.INTERSECTION_TYPE :
			return ((WildcardBinding) type).resolve();

		case Binding.ARRAY_TYPE :
			ArrayBinding arrayBinding = (ArrayBinding) type;
			TypeBinding leafComponentType = arrayBinding.leafComponentType;
			resolveType(leafComponentType, environment, convertGenericToRawType);
			if (leafComponentType.hasNullTypeAnnotations() && environment.usesNullTypeAnnotations()) {
				if (arrayBinding.nullTagBitsPerDimension == null)
					arrayBinding.nullTagBitsPerDimension = new long[arrayBinding.dimensions+1];
				arrayBinding.nullTagBitsPerDimension[arrayBinding.dimensions] = leafComponentType.tagBits & TagBits.AnnotationNullMASK;
			}
			break;

		case Binding.TYPE_PARAMETER :
			((TypeVariableBinding) type).resolve();
			break;

		case Binding.GENERIC_TYPE :
			if (convertGenericToRawType) // raw reference to generic ?
				return environment.convertUnresolvedBinaryToRawType(type);
			break;

		default:
			if (type instanceof UnresolvedReferenceBinding)
				return ((UnresolvedReferenceBinding) type).resolve(environment, convertGenericToRawType);
			if (convertGenericToRawType) // raw reference to generic ?
				return environment.convertUnresolvedBinaryToRawType(type);
			break;
	}
	return type;
}

/**
 * Default empty constructor for subclasses only.
 */
protected BinaryTypeBinding() {
	// only for subclasses
	this.prototype = this;
}

public BinaryTypeBinding(BinaryTypeBinding prototype) {
	super(prototype);
	this.superclass = prototype.superclass;
	this.enclosingType = prototype.enclosingType;
	this.superInterfaces = prototype.superInterfaces;
	this.permittedSubtypes = prototype.permittedSubtypes;
	this.fields = prototype.fields;
	this.methods = prototype.methods;
	this.memberTypes = prototype.memberTypes;
	this.typeVariables = prototype.typeVariables;
	this.prototype = prototype.prototype;
	this.environment = prototype.environment;
	this.storedAnnotations = prototype.storedAnnotations;
}

/**
 * Standard constructor for creating binary type bindings from binary models (classfiles)
 * @param packageBinding
 * @param binaryType
 * @param environment
 */
public BinaryTypeBinding(PackageBinding packageBinding, IBinaryType binaryType, LookupEnvironment environment) {
	this(packageBinding, binaryType, environment, false);
}
/**
 * Standard constructor for creating binary type bindings from binary models (classfiles)
 * @param packageBinding
 * @param binaryType
 * @param environment
 * @param needFieldsAndMethods
 */
public BinaryTypeBinding(PackageBinding packageBinding, IBinaryType binaryType, LookupEnvironment environment, boolean needFieldsAndMethods) {

	this.prototype = this;
	this.compoundName = CharOperation.splitOn('/', binaryType.getName());
	computeId();

	this.tagBits |= TagBits.IsBinaryBinding;
	this.environment = environment;
	this.fPackage = packageBinding;
	this.fileName = binaryType.getFileName();
	/* https://bugs.eclipse.org/bugs/show_bug.cgi?id=324850, even in a 1.4 project, we
	   must internalize type variables and observe any parameterization of super class
	   and/or super interfaces in order to be able to detect overriding in the presence
	   of generics.
	 */
	char[] typeSignature = binaryType.getGenericSignature();
	this.typeVariables = typeSignature != null && typeSignature.length > 0 && typeSignature[0] == Util.C_GENERIC_START
		? null // is initialized in cachePartsFrom (called from LookupEnvironment.createBinaryTypeFrom())... must set to null so isGenericType() answers true
		: Binding.NO_TYPE_VARIABLES;

	this.sourceName = binaryType.getSourceName();
	this.modifiers = binaryType.getModifiers();

	if ((binaryType.getTagBits() & TagBits.HierarchyHasProblems) != 0)
		this.tagBits |= TagBits.HierarchyHasProblems;

	if (binaryType.isAnonymous()) {
		this.tagBits |= TagBits.AnonymousTypeMask;
	} else if (binaryType.isLocal()) {
		this.tagBits |= TagBits.LocalTypeMask;
	} else if (binaryType.isMember()) {
		this.tagBits |= TagBits.MemberTypeMask;
	}
	// need enclosing type to access type variables
	char[] enclosingTypeName = binaryType.getEnclosingTypeName();
	if (enclosingTypeName != null) {
		// attempt to find the enclosing type if it exists in the cache (otherwise - resolve it when requested)
		this.enclosingType = environment.getTypeFromConstantPoolName(enclosingTypeName, 0, -1, true, null /* could not be missing */); // pretend parameterized to avoid raw
		this.tagBits |= TagBits.MemberTypeMask;   // must be a member type not a top-level or local type
		this.tagBits |= TagBits.HasUnresolvedEnclosingType;
		if (enclosingType().isStrictfp())
			this.modifiers |= ClassFileConstants.AccStrictfp;
		if (enclosingType().isDeprecated())
			this.modifiers |= ExtraCompilerModifiers.AccDeprecatedImplicitly;
	}
	if (needFieldsAndMethods)
		cachePartsFrom(binaryType, true);
}
@Override
public boolean canBeSeenBy(Scope sco) {
	ModuleBinding mod = sco.module();
	return mod.canAccess(this.fPackage) && super.canBeSeenBy(sco);
}
/**
 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#availableFields()
 */
@Override
public FieldBinding[] availableFields() {

	if (!isPrototype()) {
		return this.prototype.availableFields();
	}

	if ((this.tagBits & TagBits.AreFieldsComplete) != 0)
		return this.fields;

	// lazily sort fields
	if ((this.tagBits & TagBits.AreFieldsSorted) == 0) {
		int length = this.fields.length;
		if (length > 1)
			ReferenceBinding.sortFields(this.fields, 0, length);
		this.tagBits |= TagBits.AreFieldsSorted;
	}
	FieldBinding[] availableFields = new FieldBinding[this.fields.length];
	int count = 0;
	for (int i = 0; i < this.fields.length; i++) {
		try {
			availableFields[count] = resolveTypeFor(this.fields[i]);
			count++;
		} catch (AbortCompilation a){
			// silent abort
		}
	}
	if (count < availableFields.length)
		System.arraycopy(availableFields, 0, availableFields = new FieldBinding[count], 0, count);
	return availableFields;
}

private TypeVariableBinding[] addMethodTypeVariables(TypeVariableBinding[] methodTypeVars) {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.typeVariables == null || this.typeVariables == Binding.NO_TYPE_VARIABLES) {
		return methodTypeVars;
	}
	if (methodTypeVars == null || methodTypeVars == Binding.NO_TYPE_VARIABLES) {
		return this.typeVariables;
	}
	// uniq-merge both the arrays
	int total = this.typeVariables.length + methodTypeVars.length;
	TypeVariableBinding[] combinedTypeVars = new TypeVariableBinding[total];
	System.arraycopy(this.typeVariables, 0, combinedTypeVars, 0, this.typeVariables.length);
	int size = this.typeVariables.length;
	loop: for (int i = 0, len = methodTypeVars.length; i < len; i++) {
		for (int j = this.typeVariables.length -1 ; j >= 0; j--) {
			if (CharOperation.equals(methodTypeVars[i].sourceName, this.typeVariables[j].sourceName))
				continue loop;
		}
		combinedTypeVars[size++] = methodTypeVars[i];
	}
	if (size != total) {
		System.arraycopy(combinedTypeVars, 0, combinedTypeVars = new TypeVariableBinding[size], 0, size);
	}
	return combinedTypeVars;
}

/**
 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#availableMethods()
 */
@Override
public MethodBinding[] availableMethods() {

	if (!isPrototype()) {
		return this.prototype.availableMethods();
	}

	if ((this.tagBits & TagBits.AreMethodsComplete) != 0)
		return this.methods;

	// lazily sort methods
	if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
		int length = this.methods.length;
		if (length > 1)
			ReferenceBinding.sortMethods(this.methods, 0, length);
		this.tagBits |= TagBits.AreMethodsSorted;
	}
	MethodBinding[] availableMethods = new MethodBinding[this.methods.length];
	int count = 0;
	for (int i = 0; i < this.methods.length; i++) {
		try {
			availableMethods[count] = resolveTypesFor(this.methods[i]);
			count++;
		} catch (AbortCompilation a){
			// silent abort
		}
	}
	if (count < availableMethods.length)
		System.arraycopy(availableMethods, 0, availableMethods = new MethodBinding[count], 0, count);
	return availableMethods;
}

void cachePartsFrom(IBinaryType binaryType, boolean needFieldsAndMethods) {
	if (!isPrototype()) throw new IllegalStateException();
	try {
		// default initialization for super-interfaces early, in case some aborting compilation error occurs,
		// and still want to use binaries passed that point (e.g. type hierarchy resolver, see bug 63748).
		this.typeVariables = Binding.NO_TYPE_VARIABLES;
		this.superInterfaces = Binding.NO_SUPERINTERFACES;
		this.permittedSubtypes = Binding.NO_PERMITTEDTYPES;

		// must retrieve member types in case superclass/interfaces need them
		this.memberTypes = Binding.NO_MEMBER_TYPES;
		IBinaryNestedType[] memberTypeStructures = binaryType.getMemberTypes();
		if (memberTypeStructures != null) {
			int size = memberTypeStructures.length;
			if (size > 0) {
				this.memberTypes = new ReferenceBinding[size];
				for (int i = 0; i < size; i++) {
					// attempt to find each member type if it exists in the cache (otherwise - resolve it when requested)
					this.memberTypes[i] = this.environment.getTypeFromConstantPoolName(memberTypeStructures[i].getName(), 0, -1, false, null /* could not be missing */);
				}
				this.tagBits |= TagBits.HasUnresolvedMemberTypes;
			}
		}

		CompilerOptions globalOptions = this.environment.globalOptions;
		long sourceLevel = globalOptions.originalSourceLevel;
		/* https://bugs.eclipse.org/bugs/show_bug.cgi?id=324850, even in a 1.4 project, we
		   must internalize type variables and observe any parameterization of super class
		   and/or super interfaces in order to be able to detect overriding in the presence
		   of generics.
		 */
		if (this.environment.globalOptions.isAnnotationBasedNullAnalysisEnabled) {
			// need annotations on the type before processing null annotations on members respecting any @NonNullByDefault:
			scanTypeForNullDefaultAnnotation(binaryType, this.fPackage);
		}
		ITypeAnnotationWalker walker = getTypeAnnotationWalker(binaryType.getTypeAnnotations(), Binding.NO_NULL_DEFAULT);
		ITypeAnnotationWalker toplevelWalker = binaryType.enrichWithExternalAnnotationsFor(walker, null, this.environment);
		this.externalAnnotationStatus = binaryType.getExternalAnnotationStatus();
		if (this.externalAnnotationStatus.isPotentiallyUnannotatedLib() && this.defaultNullness != 0) {
			this.externalAnnotationStatus = ExternalAnnotationStatus.TYPE_IS_ANNOTATED;
		}
		char[] typeSignature = binaryType.getGenericSignature(); // use generic signature even in 1.4
		this.tagBits |= binaryType.getTagBits();
		if (this.environment.globalOptions.complianceLevel < ClassFileConstants.JDK1_8)
			this.tagBits &= ~TagBits.AnnotationForTypeUse; // avoid confusion with semantics that are not supported at 1.7-

		char[][][] missingTypeNames = binaryType.getMissingTypeNames();
		SignatureWrapper wrapper = null;
		if (typeSignature != null) {
			// ClassSignature = ParameterPart(optional) super_TypeSignature interface_signature
			wrapper = new SignatureWrapper(typeSignature);
			if (wrapper.signature[wrapper.start] == Util.C_GENERIC_START) {
				// ParameterPart = '<' ParameterSignature(s) '>'
				wrapper.start++; // skip '<'
				this.typeVariables = createTypeVariables(wrapper, true, missingTypeNames, toplevelWalker, true/*class*/);
				wrapper.start++; // skip '>'
				this.tagBits |=  TagBits.HasUnresolvedTypeVariables;
				this.modifiers |= ExtraCompilerModifiers.AccGenericSignature;
			}
		}
		TypeVariableBinding[] typeVars = Binding.NO_TYPE_VARIABLES;
		char[] methodDescriptor = binaryType.getEnclosingMethod();
		if (methodDescriptor != null) {
			MethodBinding enclosingMethod = findMethod(methodDescriptor, missingTypeNames);
			if (enclosingMethod != null) {
				typeVars = enclosingMethod.typeVariables;
				this.typeVariables = addMethodTypeVariables(typeVars);
			}
		}
		if (typeSignature == null)  {
			char[] superclassName = binaryType.getSuperclassName();
			if (superclassName != null) {
				// attempt to find the superclass if it exists in the cache (otherwise - resolve it when requested)
				this.superclass = this.environment.getTypeFromConstantPoolName(superclassName, 0, -1, false, missingTypeNames, toplevelWalker.toSupertype((short) -1, superclassName));
				this.tagBits |= TagBits.HasUnresolvedSuperclass;
				if (CharOperation.equals(superclassName, TypeConstants.CharArray_JAVA_LANG_RECORD_SLASH)){
					this.modifiers |= ExtraCompilerModifiers.AccRecord;
				}
			}

			this.superInterfaces = Binding.NO_SUPERINTERFACES;
			char[][] interfaceNames = binaryType.getInterfaceNames();
			if (interfaceNames != null) {
				int size = interfaceNames.length;
				if (size > 0) {
					this.superInterfaces = new ReferenceBinding[size];
					for (short i = 0; i < size; i++)
						// attempt to find each superinterface if it exists in the cache (otherwise - resolve it when requested)
						this.superInterfaces[i] = this.environment.getTypeFromConstantPoolName(interfaceNames[i], 0, -1, false, missingTypeNames, toplevelWalker.toSupertype(i, superclassName));
					this.tagBits |= TagBits.HasUnresolvedSuperinterfaces;
				}
			}
		} else {
			// attempt to find the superclass if it exists in the cache (otherwise - resolve it when requested)
			this.superclass = (ReferenceBinding) this.environment.getTypeFromTypeSignature(wrapper, typeVars, this, missingTypeNames,
																		toplevelWalker.toSupertype((short) -1, wrapper.peekFullType()));
			this.tagBits |= TagBits.HasUnresolvedSuperclass;

			this.superInterfaces = Binding.NO_SUPERINTERFACES;
			if (!wrapper.atEnd()) {
				// attempt to find each superinterface if it exists in the cache (otherwise - resolve it when requested)
				java.util.ArrayList types = new java.util.ArrayList(2);
				short rank = 0;
				do {
					types.add(this.environment.getTypeFromTypeSignature(wrapper, typeVars, this, missingTypeNames, toplevelWalker.toSupertype(rank++, wrapper.peekFullType())));
				} while (!wrapper.atEnd());
				this.superInterfaces = new ReferenceBinding[types.size()];
				types.toArray(this.superInterfaces);
				this.tagBits |= TagBits.HasUnresolvedSuperinterfaces;
			}

			this.permittedSubtypes = Binding.NO_PERMITTEDTYPES;
			if (!wrapper.atEnd()) {
				// attempt to find each permitted type if it exists in the cache (otherwise - resolve it when requested)
				java.util.ArrayList types = new java.util.ArrayList(2);
				short rank = 0;
				do {
					types.add(this.environment.getTypeFromTypeSignature(wrapper, typeVars, this, missingTypeNames, toplevelWalker.toSupertype(rank++, wrapper.peekFullType())));
				} while (!wrapper.atEnd());
				this.permittedSubtypes = new ReferenceBinding[types.size()];
				types.toArray(this.permittedSubtypes);
				this.extendedTagBits |= ExtendedTagBits.HasUnresolvedPermittedSubtypes;
			}

		}
		// fall back, in case we haven't got them from signature
		char[][] permittedSubtypeNames = binaryType.getPermittedSubtypeNames();
		if (this.permittedSubtypes == Binding.NO_PERMITTEDTYPES && permittedSubtypeNames != null) {
			this.modifiers |= ExtraCompilerModifiers.AccSealed;
			int size = permittedSubtypeNames.length;
			if (size > 0) {
				this.permittedSubtypes = new ReferenceBinding[size];
				for (short i = 0; i < size; i++)
					// attempt to find each superinterface if it exists in the cache (otherwise - resolve it when requested)
					this.permittedSubtypes[i] = this.environment.getTypeFromConstantPoolName(permittedSubtypeNames[i], 0, -1, false, missingTypeNames, toplevelWalker.toSupertype(i, null));
			}
		}
		boolean canUseNullTypeAnnotations = this.environment.globalOptions.isAnnotationBasedNullAnalysisEnabled && this.environment.globalOptions.sourceLevel >= ClassFileConstants.JDK1_8;
		if (canUseNullTypeAnnotations && this.externalAnnotationStatus.isPotentiallyUnannotatedLib()) {
			if (this.superclass != null && this.superclass.hasNullTypeAnnotations()) {
				this.externalAnnotationStatus = ExternalAnnotationStatus.TYPE_IS_ANNOTATED;
			} else {
				for (TypeBinding ifc : this.superInterfaces) {
					if (ifc.hasNullTypeAnnotations()) {
						this.externalAnnotationStatus = ExternalAnnotationStatus.TYPE_IS_ANNOTATED;
						break;
					}
				}
				for (TypeBinding permsub : this.permittedSubtypes) {
					if (permsub.hasNullTypeAnnotations()) {
						this.externalAnnotationStatus = ExternalAnnotationStatus.TYPE_IS_ANNOTATED;
						break;
					}
				}
			}
		}

		if (needFieldsAndMethods) {
			IRecordComponent[] iComponents = null;
			if (binaryType.isRecord()) {
				iComponents = binaryType.getRecordComponents();
				if (iComponents != null) {
					VariableBinding[] createFields = createFields(iComponents, binaryType, sourceLevel, missingTypeNames, true);
					this.components = (RecordComponentBinding[]) createFields;
				}
			}
			IBinaryField[] iFields = binaryType.getFields();
			VariableBinding[] createdFields = createFields(iFields, binaryType, sourceLevel, missingTypeNames, false);
			this.fields = (FieldBinding[]) createdFields;
			IBinaryMethod[] iMethods = createMethods(binaryType.getMethods(), binaryType, sourceLevel, missingTypeNames);
			boolean isViewedAsDeprecated = isViewedAsDeprecated();
			if (isViewedAsDeprecated) {
				for (int i = 0, max = this.fields.length; i < max; i++) {
					FieldBinding field = this.fields[i];
					if (!field.isDeprecated()) {
						field.modifiers |= ExtraCompilerModifiers.AccDeprecatedImplicitly;
					}
				}
				for (int i = 0, max = this.methods.length; i < max; i++) {
					MethodBinding method = this.methods[i];
					if (!method.isDeprecated()) {
						method.modifiers |= ExtraCompilerModifiers.AccDeprecatedImplicitly;
					}
				}
			}
			if (this.environment.globalOptions.isAnnotationBasedNullAnalysisEnabled) {
				if (iComponents != null) {
					for (int i = 0; i < iComponents.length; i++) {
						// below 1.8 we still might use an annotation walker to discover external annotations:
						ITypeAnnotationWalker fieldWalker = ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER;
						if (sourceLevel < ClassFileConstants.JDK1_8)
							fieldWalker = binaryType.enrichWithExternalAnnotationsFor(walker, iFields[i], this.environment);
						scanFieldForNullAnnotation(iComponents[i], this.components[i], this.isEnum(), fieldWalker);
					}
				}
				if (iFields != null) {
					for (int i = 0; i < iFields.length; i++) {
						// below 1.8 we still might use an annotation walker to discover external annotations:
						ITypeAnnotationWalker fieldWalker = ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER;
						if (sourceLevel < ClassFileConstants.JDK1_8)
							fieldWalker = binaryType.enrichWithExternalAnnotationsFor(walker, iFields[i], this.environment);
						scanFieldForNullAnnotation(iFields[i], this.fields[i], this.isEnum(), fieldWalker);
					}
				}
				if (iMethods != null) {
					for (int i = 0; i < iMethods.length; i++) {
						// below 1.8 we still might use an annotation walker to discover external annotations:
						ITypeAnnotationWalker methodWalker = ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER;
						if (sourceLevel < ClassFileConstants.JDK1_8)
							methodWalker = binaryType.enrichWithExternalAnnotationsFor(methodWalker, iMethods[i], this.environment);
						scanMethodForNullAnnotation(iMethods[i], this.methods[i], methodWalker, canUseNullTypeAnnotations);
					}
				}
			}
		}
		IBinaryAnnotation[] declAnnotations = binaryType.getAnnotations();
		if (declAnnotations != null) {
			if (hasValueBasedTypeAnnotation(declAnnotations)) {
				this.extendedTagBits |= ExtendedTagBits.AnnotationValueBased;
			}
			for (IBinaryAnnotation annotation : declAnnotations) {
				char[] typeName = annotation.getTypeName();
				if (isPreviewFeature(typeName)) {
					this.tagBits |= TagBits.AnnotationPreviewFeature;
					break;
				}
			}
		}
		if (this.environment.globalOptions.storeAnnotations) {
			setAnnotations(createAnnotations(declAnnotations, this.environment, missingTypeNames), false);
		} else if (sourceLevel >= ClassFileConstants.JDK9 && isDeprecated() && binaryType.getAnnotations() != null) {
			// prior to Java 9 all standard annotations were marker annotations, not needing to be stored,
			// but since Java 9 we need more information from the @Deprecated annotation:
			for (IBinaryAnnotation annotation : declAnnotations) {
				if (annotation.isDeprecatedAnnotation()) {
					AnnotationBinding[] annotationBindings = createAnnotations(new IBinaryAnnotation[] { annotation }, this.environment, missingTypeNames);
					setAnnotations(annotationBindings, true); // force storing
					for (ElementValuePair elementValuePair : annotationBindings[0].getElementValuePairs()) {
						if (CharOperation.equals(elementValuePair.name, TypeConstants.FOR_REMOVAL)) {
							if (elementValuePair.value instanceof BooleanConstant && ((BooleanConstant) elementValuePair.value).booleanValue()) {
								this.tagBits |= TagBits.AnnotationTerminallyDeprecated;
								markImplicitTerminalDeprecation(this);
							}
						}
					}
					break;
				}
			}
		}
		if (this.isAnnotationType())
			scanTypeForContainerAnnotation(binaryType, missingTypeNames);
	} finally {
		// protect against incorrect use of the needFieldsAndMethods flag, see 48459
		if (this.components == null)
			this.components = Binding.NO_COMPONENTS;
		if (this.fields == null)
			this.fields = Binding.NO_FIELDS;
		if (this.methods == null)
			this.methods = Binding.NO_METHODS;
	}
}
void markImplicitTerminalDeprecation(ReferenceBinding type) {
	for (ReferenceBinding member : type.memberTypes()) {
		member.tagBits |= TagBits.AnnotationTerminallyDeprecated;
		markImplicitTerminalDeprecation(member);
	}
	MethodBinding[] methodsOfType = type.unResolvedMethods();
	if (methodsOfType != null)
		for (MethodBinding methodBinding : methodsOfType)
			methodBinding.tagBits |= TagBits.AnnotationTerminallyDeprecated;

	FieldBinding[] fieldsOfType = type.unResolvedFields();
	if (fieldsOfType != null)
		for (FieldBinding fieldBinding : fieldsOfType)
			fieldBinding.tagBits |= TagBits.AnnotationTerminallyDeprecated;
}

/* When creating a method we need to pass in any default 'nullness' from a @NNBD immediately on this method. */
private ITypeAnnotationWalker getTypeAnnotationWalker(IBinaryTypeAnnotation[] annotations, int nullness) {
	if (!isPrototype()) throw new IllegalStateException();
	if (annotations == null || annotations.length == 0 || !this.environment.usesAnnotatedTypeSystem()) {
		if (this.environment.globalOptions.isAnnotationBasedNullAnalysisEnabled) {
			if (nullness == Binding.NO_NULL_DEFAULT)
				nullness = getNullDefault();
			if (nullness > Binding.NULL_UNSPECIFIED_BY_DEFAULT)
				return new NonNullDefaultAwareTypeAnnotationWalker(nullness, this.environment);
		}
		return ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER;
	}
	if (this.environment.globalOptions.isAnnotationBasedNullAnalysisEnabled) {
		if (nullness == Binding.NO_NULL_DEFAULT)
			nullness = getNullDefault();
		if (nullness > Binding.NULL_UNSPECIFIED_BY_DEFAULT)
			return new NonNullDefaultAwareTypeAnnotationWalker(annotations, nullness, this.environment);
	}
	return new TypeAnnotationWalker(annotations);
}

private boolean hasValueBasedTypeAnnotation(IBinaryAnnotation[] declAnnotations) {
	boolean hasValueBasedAnnotation = false;
	if (declAnnotations != null && declAnnotations.length > 0) {
		for (IBinaryAnnotation annot : declAnnotations) {
			char[] typeName= annot.getTypeName();
			if ( typeName == null || typeName.length < 25 || typeName[0] != 'L')
				continue;
			char[][] name = CharOperation.splitOn('/', typeName, 1, typeName.length-1);
			try {
				if (CharOperation.equals(name,TypeConstants.JDK_INTERNAL_VALUEBASED)) {
					hasValueBasedAnnotation= true;
					break;
				}
			} catch (Exception e) {
				//do nothing
			}
		}
	}
	return hasValueBasedAnnotation;
}

private int getNullDefaultFrom(IBinaryAnnotation[] declAnnotations) {
	int result = 0;
	if (declAnnotations != null) {
		for (IBinaryAnnotation annotation : declAnnotations) {
			char[][] typeName = signature2qualifiedTypeName(annotation.getTypeName());
			if (this.environment.getNullAnnotationBit(typeName) == TypeIds.BitNonNullByDefaultAnnotation)
				result |= getNonNullByDefaultValue(annotation, this.environment);
		}
	}
	return result;
}

private VariableBinding[] createFields(IBinaryField[] iFields, IBinaryType binaryType, long sourceLevel, char[][][] missingTypeNames, boolean isComponent) {
	if (!isPrototype()) throw new IllegalStateException();
	boolean save = this.environment.mayTolerateMissingType;
	this.environment.mayTolerateMissingType = true;
	VariableBinding[] tFields = isComponent ? Binding.NO_COMPONENTS : Binding.NO_FIELDS;
	try {
		if (iFields != null) {
			int size = iFields.length;
			if (size > 0) {
				VariableBinding[] fields1 = isComponent ? new RecordComponentBinding[size] : new FieldBinding[size];
				boolean use15specifics = sourceLevel >= ClassFileConstants.JDK1_5;
				boolean hasRestrictedAccess = hasRestrictedAccess();
				int firstAnnotatedFieldIndex = -1;
				for (int i = 0; i < size; i++) {
					IBinaryField binaryField = iFields[i];
					char[] fieldSignature = use15specifics ? binaryField.getGenericSignature() : null;
					IBinaryAnnotation[] declAnnotations = binaryField.getAnnotations();
					ITypeAnnotationWalker walker = getTypeAnnotationWalker(binaryField.getTypeAnnotations(), getNullDefaultFrom(declAnnotations));
					if (sourceLevel >= ClassFileConstants.JDK1_8) { // below 1.8, external annotations will be attached later
						walker = binaryType.enrichWithExternalAnnotationsFor(walker, iFields[i], this.environment);
					}
					walker = walker.toField();
					TypeBinding type = fieldSignature == null
						? this.environment.getTypeFromSignature(binaryField.getTypeName(), 0, -1, false, this, missingTypeNames, walker)
						: this.environment.getTypeFromTypeSignature(new SignatureWrapper(fieldSignature), Binding.NO_TYPE_VARIABLES, this, missingTypeNames, walker);
					VariableBinding field =
							isComponent ?
									new RecordComponentBinding(
										binaryField.getName(),
										type,
										binaryField.getModifiers() | ExtraCompilerModifiers.AccUnresolved,
										this) :
									new FieldBinding(
										binaryField.getName(),
										type,
										binaryField.getModifiers() | ExtraCompilerModifiers.AccUnresolved,
										this,
										binaryField.getConstant());
					if (declAnnotations != null) {
						for (IBinaryAnnotation annotation : declAnnotations) {
							char[] typeName = annotation.getTypeName();
							if (isPreviewFeature(typeName)) {
								field.tagBits |= TagBits.AnnotationPreviewFeature;
								break;
							}
						}
					}
					boolean forceStoreAnnotations = !this.environment.globalOptions.storeAnnotations
							&& (this.environment.globalOptions.sourceLevel >= ClassFileConstants.JDK9
							&& binaryField.getAnnotations() != null
							&& (binaryField.getTagBits() & TagBits.AnnotationDeprecated) != 0);
					if (firstAnnotatedFieldIndex < 0
							&& (this.environment.globalOptions.storeAnnotations || forceStoreAnnotations)
							&& binaryField.getAnnotations() != null) {
						firstAnnotatedFieldIndex = i;
						if (forceStoreAnnotations)
							storedAnnotations(true, true); // for Java 9 @Deprecated we need to force storing annotations
					}
					field.id = i; // ordinal
					if (use15specifics)
						field.tagBits |= binaryField.getTagBits();
					if (hasRestrictedAccess)
						field.modifiers |= ExtraCompilerModifiers.AccRestrictedAccess;
					if (fieldSignature != null)
						field.modifiers |= ExtraCompilerModifiers.AccGenericSignature;
					fields1[i] = field;
				}
				tFields = fields1;
				// second pass for reifying annotations, since may refer to fields being constructed (147875)
				if (firstAnnotatedFieldIndex >= 0) {
					for (int i = firstAnnotatedFieldIndex; i <size; i++) {
						IBinaryField binaryField = iFields[i];
						tFields[i].setAnnotations(createAnnotations(binaryField.getAnnotations(), this.environment, missingTypeNames), false);
					}
				}
			}
		}
	} finally {
		this.environment.mayTolerateMissingType = save;
	}
	return tFields;
}

private boolean isPreviewFeature(char[] typeName) {
	int index = CharOperation.lastIndexOf('/', typeName);
	if (index != -1)
		typeName = CharOperation.subarray(typeName, index, typeName.length);
	index = CharOperation.indexOf(ConstantPool.PREVIEW_FEATURE, typeName, true);
	return index == 0;
}

private MethodBinding createMethod(IBinaryMethod method, IBinaryType binaryType, long sourceLevel, char[][][] missingTypeNames) {
	if (!isPrototype()) throw new IllegalStateException();
	int methodModifiers = method.getModifiers() | ExtraCompilerModifiers.AccUnresolved;
	if (sourceLevel < ClassFileConstants.JDK1_5)
		methodModifiers &= ~ClassFileConstants.AccVarargs; // vararg methods are not recognized until 1.5
	if (isInterface() && (methodModifiers & ClassFileConstants.AccAbstract) == 0) {
		// see https://bugs.eclipse.org/388954 superseded by https://bugs.eclipse.org/390889
		if (((methodModifiers & ClassFileConstants.AccStatic) == 0
				&& (methodModifiers & ClassFileConstants.AccPrivate) == 0)) {
			// i.e. even at 1.7- we record AccDefaultMethod when reading a 1.8+ interface to avoid errors caused by default methods added to a library
			methodModifiers |= ExtraCompilerModifiers.AccDefaultMethod;
		}
	}
	ReferenceBinding[] exceptions = Binding.NO_EXCEPTIONS;
	TypeBinding[] parameters = Binding.NO_PARAMETERS;
	TypeVariableBinding[] typeVars = Binding.NO_TYPE_VARIABLES;
	AnnotationBinding[][] paramAnnotations = null;
	TypeBinding returnType = null;

	char[][] argumentNames = method.getArgumentNames();

	IBinaryAnnotation[] declAnnotations = method.getAnnotations();
	final boolean use15specifics = sourceLevel >= ClassFileConstants.JDK1_5;
	/* https://bugs.eclipse.org/bugs/show_bug.cgi?id=324850, Since a 1.4 project can have a 1.5
	   type as a super type and the 1.5 type could be generic, we must internalize usages of type
	   variables properly in order to be able to apply substitutions and thus be able to detect
	   overriding in the presence of generics. Seeing the erased form is not good enough.
	 */
	ITypeAnnotationWalker walker = getTypeAnnotationWalker(method.getTypeAnnotations(), getNullDefaultFrom(declAnnotations));
	char[] methodSignature = method.getGenericSignature(); // always use generic signature, even in 1.4
	if (methodSignature == null) { // no generics
		char[] methodDescriptor = method.getMethodDescriptor();   // of the form (I[Ljava/jang/String;)V
		if (sourceLevel >= ClassFileConstants.JDK1_8) { // below 1.8, external annotations will be attached later
			walker = binaryType.enrichWithExternalAnnotationsFor(walker, method, this.environment);
		}
		int numOfParams = 0;
		char nextChar;
		int index = 0; // first character is always '(' so skip it
		while ((nextChar = methodDescriptor[++index]) != Util.C_PARAM_END) {
			if (nextChar != Util.C_ARRAY) {
				numOfParams++;
				if (nextChar == Util.C_RESOLVED)
					while ((nextChar = methodDescriptor[++index]) != Util.C_NAME_END){/*empty*/}
			}
		}

		// Ignore synthetic argument for member types or enum types.
		int startIndex = 0;
		if (method.isConstructor()) {
			if (isMemberType() && !isStatic()) {
				// enclosing type
				startIndex++;
			}
			if (isEnum()) {
				// synthetic arguments (String, int)
				startIndex += 2;
			}
		}
		int size = numOfParams - startIndex;
		if (size > 0) {
			parameters = new TypeBinding[size];
			if (this.environment.globalOptions.storeAnnotations)
				paramAnnotations = new AnnotationBinding[size][];
			index = 1;
			short visibleIdx = 0;
			int end = 0;   // first character is always '(' so skip it
			for (int i = 0; i < numOfParams; i++) {
				while ((nextChar = methodDescriptor[++end]) == Util.C_ARRAY){/*empty*/}
				if (nextChar == Util.C_RESOLVED)
					while ((nextChar = methodDescriptor[++end]) != Util.C_NAME_END){/*empty*/}

				if (i >= startIndex) {   // skip the synthetic arg if necessary
					// checking for param specific non-null default is not necessary here as no type arguments are present (application to params themselves is handled by ImplicitNullAnnotationVerifier)
					parameters[i - startIndex] = this.environment.getTypeFromSignature(methodDescriptor, index, end, false, this, missingTypeNames, walker.toMethodParameter(visibleIdx++));
					// 'paramAnnotations' line up with 'parameters'
					// int parameter to method.getParameterAnnotations() include the synthetic arg
					if (paramAnnotations != null)
						paramAnnotations[i - startIndex] = createAnnotations(method.getParameterAnnotations(i - startIndex, this.fileName), this.environment, missingTypeNames);
				}
				index = end + 1;
			}
		}

		char[][] exceptionTypes = method.getExceptionTypeNames();
		if (exceptionTypes != null) {
			size = exceptionTypes.length;
			if (size > 0) {
				exceptions = new ReferenceBinding[size];
				for (int i = 0; i < size; i++)
					exceptions[i] = this.environment.getTypeFromConstantPoolName(exceptionTypes[i], 0, -1, false, missingTypeNames, walker.toThrows(i));
			}
		}

		if (!method.isConstructor())
			returnType = this.environment.getTypeFromSignature(methodDescriptor, index + 1, -1, false, this, missingTypeNames, walker.toMethodReturn());   // index is currently pointing at the ')'

		final int argumentNamesLength = argumentNames == null ? 0 : argumentNames.length;
		if (startIndex > 0 && argumentNamesLength > 0) {
			// We'll have to slice the starting arguments off
			if (startIndex >= argumentNamesLength) {
				argumentNames = Binding.NO_PARAMETER_NAMES; // We know nothing about the argument names
			} else {
				char[][] slicedArgumentNames = new char[argumentNamesLength - startIndex][];
				System.arraycopy(argumentNames, startIndex, slicedArgumentNames, 0, argumentNamesLength - startIndex);
				argumentNames = slicedArgumentNames;
			}
		}

	} else {
		if (sourceLevel >= ClassFileConstants.JDK1_8) { // below 1.8, external annotations will be attached later
			walker = binaryType.enrichWithExternalAnnotationsFor(walker, method, this.environment);
		}
		methodModifiers |= ExtraCompilerModifiers.AccGenericSignature;
		// MethodTypeSignature = ParameterPart(optional) '(' TypeSignatures ')' return_typeSignature ['^' TypeSignature (optional)]
		SignatureWrapper wrapper = new SignatureWrapper(methodSignature, use15specifics);
		if (wrapper.signature[wrapper.start] == Util.C_GENERIC_START) {
			// <A::Ljava/lang/annotation/Annotation;>(Ljava/lang/Class<TA;>;)TA;
			// ParameterPart = '<' ParameterSignature(s) '>'
			wrapper.start++; // skip '<'
			typeVars = createTypeVariables(wrapper, false, missingTypeNames, walker, false/*class*/);
			wrapper.start++; // skip '>'
		}

		if (wrapper.signature[wrapper.start] == Util.C_PARAM_START) {
			wrapper.start++; // skip '('
			if (wrapper.signature[wrapper.start] == Util.C_PARAM_END) {
				wrapper.start++; // skip ')'
			} else {
				java.util.ArrayList types = new java.util.ArrayList(2);
				short rank = 0;
				while (wrapper.signature[wrapper.start] != Util.C_PARAM_END) {
					IBinaryAnnotation[] binaryParameterAnnotations = method.getParameterAnnotations(rank, this.fileName);
					ITypeAnnotationWalker updatedWalker = NonNullDefaultAwareTypeAnnotationWalker.updateWalkerForParamNonNullDefault(walker, getNullDefaultFrom(binaryParameterAnnotations), this.environment);
					types.add(this.environment.getTypeFromTypeSignature(wrapper, typeVars, this, missingTypeNames, updatedWalker.toMethodParameter(rank)));
					rank++;
				}
				wrapper.start++; // skip ')'
				int numParam = types.size();
				parameters = new TypeBinding[numParam];
				types.toArray(parameters);
				if (this.environment.globalOptions.storeAnnotations) {
					paramAnnotations = new AnnotationBinding[numParam][];
					for (int i = 0; i < numParam; i++)
						paramAnnotations[i] = createAnnotations(method.getParameterAnnotations(i,  this.fileName), this.environment, missingTypeNames);
				}
			}
		}

		// always retrieve return type (for constructors, its V for void - will be ignored)
		returnType = this.environment.getTypeFromTypeSignature(wrapper, typeVars, this, missingTypeNames, walker.toMethodReturn());

		if (!wrapper.atEnd() && wrapper.signature[wrapper.start] == Util.C_EXCEPTION_START) {
			// attempt to find each exception if it exists in the cache (otherwise - resolve it when requested)
			java.util.ArrayList types = new java.util.ArrayList(2);
			int excRank = 0;
			do {
				wrapper.start++; // skip '^'
				types.add(this.environment.getTypeFromTypeSignature(wrapper, typeVars, this, missingTypeNames,
					walker.toThrows(excRank++)));
			} while (!wrapper.atEnd() && wrapper.signature[wrapper.start] == Util.C_EXCEPTION_START);
			exceptions = new ReferenceBinding[types.size()];
			types.toArray(exceptions);
		} else { // get the exceptions the old way
			char[][] exceptionTypes = method.getExceptionTypeNames();
			if (exceptionTypes != null) {
				int size = exceptionTypes.length;
				if (size > 0) {
					exceptions = new ReferenceBinding[size];
					for (int i = 0; i < size; i++)
						exceptions[i] = this.environment.getTypeFromConstantPoolName(exceptionTypes[i], 0, -1, false, missingTypeNames, walker.toThrows(i));
				}
			}
		}
	}

	MethodBinding result = method.isConstructor()
		? new MethodBinding(methodModifiers, parameters, exceptions, this)
		: new MethodBinding(methodModifiers, method.getSelector(), returnType, parameters, exceptions, this);

	if (declAnnotations != null) {
		for (IBinaryAnnotation annotation : declAnnotations) {
			char[] typeName = annotation.getTypeName();
			if (isPreviewFeature(typeName)) {
				result.tagBits |= TagBits.AnnotationPreviewFeature;
				break;
			}
		}
	}
	IBinaryAnnotation[] receiverAnnotations = walker.toReceiver().getAnnotationsAtCursor(this.id, false);
	if (receiverAnnotations != null && receiverAnnotations.length > 0) {
		result.receiver = this.environment.createAnnotatedType(this, createAnnotations(receiverAnnotations, this.environment, missingTypeNames));
	}

	boolean forceStoreAnnotations = !this.environment.globalOptions.storeAnnotations
										&& (this.environment.globalOptions.sourceLevel >= ClassFileConstants.JDK9
										&& method instanceof MethodInfoWithAnnotations
										&& (method.getTagBits() & TagBits.AnnotationDeprecated) != 0);
	if (this.environment.globalOptions.storeAnnotations || forceStoreAnnotations) {
		if (forceStoreAnnotations)
			storedAnnotations(true, true); // for Java 9 @Deprecated we need to force storing annotations
		IBinaryAnnotation[] annotations = method.getAnnotations();
		if (method.isConstructor()) {
			IBinaryAnnotation[] tAnnotations = walker.toMethodReturn().getAnnotationsAtCursor(this.id, false);
			result.setTypeAnnotations(createAnnotations(tAnnotations, this.environment, missingTypeNames));
		}
		result.setAnnotations(
				createAnnotations(annotations, this.environment, missingTypeNames),
				paramAnnotations,
				isAnnotationType() ? convertMemberValue(method.getDefaultValue(), this.environment, missingTypeNames, true) : null,
						this.environment);
	}

	if (argumentNames != null) result.parameterNames = argumentNames;

	if (use15specifics)
		result.tagBits |= method.getTagBits();
	result.typeVariables = typeVars;
	// fixup the declaring element of all type variables
	for (int i = 0, length = typeVars.length; i < length; i++)
		this.environment.typeSystem.fixTypeVariableDeclaringElement(typeVars[i], result);

	return result;
}

/**
 * Create method bindings for binary type, filtering out <clinit> and synthetics
 * As some iMethods may be ignored in this process we return the matching array of those
 * iMethods for which MethodBindings have been created; indices match those in this.methods.
 */
private IBinaryMethod[] createMethods(IBinaryMethod[] iMethods, IBinaryType binaryType, long sourceLevel, char[][][] missingTypeNames) {
	if (!isPrototype()) throw new IllegalStateException();
	boolean save = this.environment.mayTolerateMissingType;
	this.environment.mayTolerateMissingType = true;
	try {
		int total = 0, initialTotal = 0, iClinit = -1;
		int[] toSkip = null;
		if (iMethods != null) {
			total = initialTotal = iMethods.length;
			boolean keepBridgeMethods = sourceLevel < ClassFileConstants.JDK1_5; // https://bugs.eclipse.org/bugs/show_bug.cgi?id=330347
			for (int i = total; --i >= 0;) {
				IBinaryMethod method = iMethods[i];
				if ((method.getModifiers() & ClassFileConstants.AccSynthetic) != 0) {
					if (keepBridgeMethods && (method.getModifiers() & ClassFileConstants.AccBridge) != 0)
						continue; // want to see bridge methods as real methods
					// discard synthetics methods
					if (toSkip == null) toSkip = new int[iMethods.length];
					toSkip[i] = -1;
					total--;
				} else if (iClinit == -1) {
					char[] methodName = method.getSelector();
					if (methodName.length == 8 && methodName[0] == Util.C_GENERIC_START) {
						// discard <clinit>
						iClinit = i;
						total--;
					}
				}
			}
		}
		if (total == 0) {
			this.methods = Binding.NO_METHODS;
			return NO_BINARY_METHODS;
		}

		boolean hasRestrictedAccess = hasRestrictedAccess();
		MethodBinding[] methods1 = new MethodBinding[total];
		if (total == initialTotal) {
			for (int i = 0; i < initialTotal; i++) {
				MethodBinding method = createMethod(iMethods[i], binaryType, sourceLevel, missingTypeNames);
				if (hasRestrictedAccess)
					method.modifiers |= ExtraCompilerModifiers.AccRestrictedAccess;
				methods1[i] = method;
			}
			this.methods = methods1;
			return iMethods;
		} else {
			IBinaryMethod[] mappedBinaryMethods = new IBinaryMethod[total];
			for (int i = 0, index = 0; i < initialTotal; i++) {
				if (iClinit != i && (toSkip == null || toSkip[i] != -1)) {
					MethodBinding method = createMethod(iMethods[i], binaryType, sourceLevel, missingTypeNames);
					if (hasRestrictedAccess)
						method.modifiers |= ExtraCompilerModifiers.AccRestrictedAccess;
					mappedBinaryMethods[index] = iMethods[i];
					methods1[index++] = method;
				}
			}
			this.methods = methods1;
			return mappedBinaryMethods;
		}
	} finally {
		this.environment.mayTolerateMissingType = save;
	}
}

private TypeVariableBinding[] createTypeVariables(SignatureWrapper wrapper, boolean assignVariables, char[][][] missingTypeNames,
													ITypeAnnotationWalker walker, boolean isClassTypeParameter)
{
	if (!isPrototype()) throw new IllegalStateException();
	// detect all type variables first
	char[] typeSignature = wrapper.signature;
	int depth = 0, length = typeSignature.length;
	int rank = 0;
	ArrayList variables = new ArrayList(1);
	depth = 0;
	boolean pendingVariable = true;
	createVariables: {
		for (int i = 1; i < length; i++) {
			switch(typeSignature[i]) {
				case Util.C_GENERIC_START :
					depth++;
					break;
				case Util.C_GENERIC_END :
					if (--depth < 0)
						break createVariables;
					break;
				case Util.C_NAME_END :
					if ((depth == 0) && (i +1 < length) && (typeSignature[i+1] != Util.C_COLON))
						pendingVariable = true;
					break;
				default:
					if (pendingVariable) {
						pendingVariable = false;
						int colon = CharOperation.indexOf(Util.C_COLON, typeSignature, i);
						char[] variableName = CharOperation.subarray(typeSignature, i, colon);
						TypeVariableBinding typeVariable = new TypeVariableBinding(variableName, this, rank, this.environment);
						AnnotationBinding [] annotations = BinaryTypeBinding.createAnnotations(walker.toTypeParameter(isClassTypeParameter, rank++).getAnnotationsAtCursor(0, false),
																										this.environment, missingTypeNames);
						if (annotations != null && annotations != Binding.NO_ANNOTATIONS)
							typeVariable.setTypeAnnotations(annotations, this.environment.globalOptions.isAnnotationBasedNullAnalysisEnabled);
						variables.add(typeVariable);
					}
			}
		}
	}
	// initialize type variable bounds - may refer to forward variables
	TypeVariableBinding[] result;
	variables.toArray(result = new TypeVariableBinding[rank]);
	// when creating the type variables for a type, the type must remember them before initializing each variable
	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=163680
	if (assignVariables)
		this.typeVariables = result;
	for (int i = 0; i < rank; i++) {
		initializeTypeVariable(result[i], result, wrapper, missingTypeNames, walker.toTypeParameterBounds(isClassTypeParameter, i));
		if (this.externalAnnotationStatus.isPotentiallyUnannotatedLib() && result[i].hasNullTypeAnnotations())
			this.externalAnnotationStatus = ExternalAnnotationStatus.TYPE_IS_ANNOTATED;
	}
	return result;
}

/* Answer the receiver's enclosing type... null if the receiver is a top level type.
*
* NOTE: enclosingType of a binary type is resolved when needed
*/
@Override
public ReferenceBinding enclosingType() {  // should not delegate to prototype.
	if ((this.tagBits & TagBits.HasUnresolvedEnclosingType) == 0)
		return this.enclosingType;

	// finish resolving the type
	this.enclosingType = (ReferenceBinding) resolveType(this.enclosingType, this.environment, false /* no raw conversion */);
	this.tagBits &= ~TagBits.HasUnresolvedEnclosingType;
	return this.enclosingType;
}
@Override
public RecordComponentBinding[] components() {
	if (!isPrototype()) {
		return this.components = this.prototype.components;
	}
	if ((this.extendedTagBits & ExtendedTagBits.AreRecordComponentsComplete) != 0)
		return this.components;

	// Should we sort?
	for (int i = this.components.length; --i >= 0;) {
		resolveTypeFor(this.components[i]);
	}
	this.tagBits |= ExtendedTagBits.AreRecordComponentsComplete;
	return this.components;
}
// NOTE: the type of each field of a binary type is resolved when needed
@Override
public FieldBinding[] fields() {

	if (!isPrototype()) {
		return this.fields = this.prototype.fields();
	}

	if ((this.tagBits & TagBits.AreFieldsComplete) != 0)
		return this.fields;

	// lazily sort fields
	if ((this.tagBits & TagBits.AreFieldsSorted) == 0) {
		int length = this.fields.length;
		if (length > 1)
			ReferenceBinding.sortFields(this.fields, 0, length);
		this.tagBits |= TagBits.AreFieldsSorted;
	}
	for (int i = this.fields.length; --i >= 0;)
		resolveTypeFor(this.fields[i]);
	this.tagBits |= TagBits.AreFieldsComplete;
	return this.fields;
}

private MethodBinding findMethod(char[] methodDescriptor, char[][][] missingTypeNames) {
	if (!isPrototype()) throw new IllegalStateException();
	int index = -1;
	while (methodDescriptor[++index] != Util.C_PARAM_START) {
		// empty
	}
	char[] selector = new char[index];
	System.arraycopy(methodDescriptor, 0, selector, 0, index);
	TypeBinding[] parameters = Binding.NO_PARAMETERS;
	int numOfParams = 0;
	char nextChar;
	int paramStart = index;
	while ((nextChar = methodDescriptor[++index]) != Util.C_PARAM_END) {
		if (nextChar != Util.C_ARRAY) {
			numOfParams++;
			if (nextChar == Util.C_RESOLVED)
				while ((nextChar = methodDescriptor[++index]) != Util.C_NAME_END){/*empty*/}
		}
	}
	if (numOfParams > 0) {
		parameters = new TypeBinding[numOfParams];
		index = paramStart + 1;
		int end = paramStart; // first character is always '(' so skip it
		for (int i = 0; i < numOfParams; i++) {
			while ((nextChar = methodDescriptor[++end]) == Util.C_ARRAY){/*empty*/}
			if (nextChar == Util.C_RESOLVED)
				while ((nextChar = methodDescriptor[++end]) != Util.C_NAME_END){/*empty*/}

			// not interested in type annotations, type will be used for comparison only, and erasure() is used if needed
			TypeBinding param = this.environment.getTypeFromSignature(methodDescriptor, index, end, false, this, missingTypeNames, ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER);
			if (param instanceof UnresolvedReferenceBinding) {
				param = resolveType(param, this.environment, true /* raw conversion */);
			}
			parameters[i] = param;
			index = end + 1;
		}
	}

	int parameterLength = parameters.length;
	MethodBinding[] methods2 = this.enclosingType.getMethods(selector, parameterLength);
	// find matching method using parameters
	loop: for (int i = 0, max = methods2.length; i < max; i++) {
		MethodBinding currentMethod = methods2[i];
		TypeBinding[] parameters2 = currentMethod.parameters;
		int currentMethodParameterLength = parameters2.length;
		if (parameterLength == currentMethodParameterLength) {
			for (int j = 0; j < currentMethodParameterLength; j++) {
				if (TypeBinding.notEquals(parameters[j], parameters2[j]) && TypeBinding.notEquals(parameters[j].erasure(), parameters2[j].erasure())) {
					continue loop;
				}
			}
			return currentMethod;
		}
	}
	return null;
}

/**
 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#genericTypeSignature()
 */
@Override
public char[] genericTypeSignature() {
	if (!isPrototype())
		return this.prototype.computeGenericTypeSignature(this.typeVariables);
	return computeGenericTypeSignature(this.typeVariables);
}

//NOTE: the return type, arg & exception types of each method of a binary type are resolved when needed
@Override
public MethodBinding getExactConstructor(TypeBinding[] argumentTypes) {

	if (!isPrototype())
		return this.prototype.getExactConstructor(argumentTypes);

	// lazily sort methods
	if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
		int length = this.methods.length;
		if (length > 1)
			ReferenceBinding.sortMethods(this.methods, 0, length);
		this.tagBits |= TagBits.AreMethodsSorted;
	}
	int argCount = argumentTypes.length;
	long range;
	if ((range = ReferenceBinding.binarySearch(TypeConstants.INIT, this.methods)) >= 0) {
		nextMethod: for (int imethod = (int)range, end = (int)(range >> 32); imethod <= end; imethod++) {
			MethodBinding method = this.methods[imethod];
			if (method.parameters.length == argCount) {
				resolveTypesFor(method);
				TypeBinding[] toMatch = method.parameters;
				for (int iarg = 0; iarg < argCount; iarg++)
					if (TypeBinding.notEquals(toMatch[iarg], argumentTypes[iarg]))
						continue nextMethod;
				return method;
			}
		}
	}
	return null;
}

//NOTE: the return type, arg & exception types of each method of a binary type are resolved when needed
//searches up the hierarchy as long as no potential (but not exact) match was found.
@Override
public MethodBinding getExactMethod(char[] selector, TypeBinding[] argumentTypes, CompilationUnitScope refScope) {
	// sender from refScope calls recordTypeReference(this)

	if (!isPrototype())
		return this.prototype.getExactMethod(selector, argumentTypes, refScope);

	// lazily sort methods
	if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
		int length = this.methods.length;
		if (length > 1)
			ReferenceBinding.sortMethods(this.methods, 0, length);
		this.tagBits |= TagBits.AreMethodsSorted;
	}

	int argCount = argumentTypes.length;
	boolean foundNothing = true;

	long range;
	if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
		nextMethod: for (int imethod = (int)range, end = (int)(range >> 32); imethod <= end; imethod++) {
			MethodBinding method = this.methods[imethod];
			foundNothing = false; // inner type lookups must know that a method with this name exists
			if (method.parameters.length == argCount) {
				resolveTypesFor(method);
				TypeBinding[] toMatch = method.parameters;
				for (int iarg = 0; iarg < argCount; iarg++)
					if (TypeBinding.notEquals(toMatch[iarg], argumentTypes[iarg]))
						continue nextMethod;
				return method;
			}
		}
	}
	if (foundNothing) {
		if (isInterface()) {
			 if (superInterfaces().length == 1) { // ensure superinterfaces are resolved before checking
				if (refScope != null)
					refScope.recordTypeReference(this.superInterfaces[0]);
				return this.superInterfaces[0].getExactMethod(selector, argumentTypes, refScope);
			 }
		} else if (superclass() != null) { // ensure superclass is resolved before checking
			if (refScope != null)
				refScope.recordTypeReference(this.superclass);
			return this.superclass.getExactMethod(selector, argumentTypes, refScope);
		}
		// NOTE: not adding permitted types here since the search is up the hierarchy while permitted ones are down.
	}
	return null;
}
//NOTE: the type of a field of a binary type is resolved when needed
@Override
public FieldBinding getField(char[] fieldName, boolean needResolve) {

	if (!isPrototype())
		return this.prototype.getField(fieldName, needResolve);

	// lazily sort fields
	if ((this.tagBits & TagBits.AreFieldsSorted) == 0) {
		int length = this.fields.length;
		if (length > 1)
			ReferenceBinding.sortFields(this.fields, 0, length);
		this.tagBits |= TagBits.AreFieldsSorted;
	}
	FieldBinding field = ReferenceBinding.binarySearch(fieldName, this.fields);
	return needResolve && field != null ? resolveTypeFor(field) : field;
}
/**
 *  Rewrite of default memberTypes() to avoid resolving eagerly all member types when one is requested
 */
@Override
public ReferenceBinding getMemberType(char[] typeName) {

	if (!isPrototype()) {
		ReferenceBinding memberType = this.prototype.getMemberType(typeName);
		return memberType == null ? null : this.environment.createMemberType(memberType, this);
	}

	ReferenceBinding[] members = maybeSortedMemberTypes();
	// do not try to binary search while we are still resolving and the array is not necessarily sorted
	if (!this.memberTypesSorted) {
		for (int i = members.length; --i >= 0;) {
		    ReferenceBinding memberType = members[i];
		    if (memberType instanceof UnresolvedReferenceBinding) {
				char[] name = memberType.sourceName; // source name is qualified with enclosing type name
				int prefixLength = this.compoundName[this.compoundName.length - 1].length + 1; // enclosing$
				if (name.length == (prefixLength + typeName.length)) // enclosing $ typeName
					if (CharOperation.fragmentEquals(typeName, name, prefixLength, true)) // only check trailing portion
						return members[i] = (ReferenceBinding) resolveType(memberType, this.environment, false /* no raw conversion for now */);
		    } else if (CharOperation.equals(typeName, memberType.sourceName)) {
		        return memberType;
		    }
		}
		return null;
	}
	int memberTypeIndex = ReferenceBinding.binarySearch(typeName, members);
	if (memberTypeIndex >= 0) {
		return members[memberTypeIndex];
	}
	return null;
}

// NOTE: the return type, arg & exception types of each method of a binary type are resolved when needed
@Override
public MethodBinding[] getMethods(char[] selector) {

	if (!isPrototype())
		return this.prototype.getMethods(selector);

	if ((this.tagBits & TagBits.AreMethodsComplete) != 0) {
		long range;
		if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
			int start = (int) range, end = (int) (range >> 32);
			int length = end - start + 1;
			if ((this.tagBits & TagBits.AreMethodsComplete) != 0) {
				// simply clone method subset
				MethodBinding[] result;
				System.arraycopy(this.methods, start, result = new MethodBinding[length], 0, length);
				return result;
			}
		}
		return Binding.NO_METHODS;
	}
	// lazily sort methods
	if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
		int length = this.methods.length;
		if (length > 1)
			ReferenceBinding.sortMethods(this.methods, 0, length);
		this.tagBits |= TagBits.AreMethodsSorted;
	}
	long range;
	if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
		int start = (int) range, end = (int) (range >> 32);
		int length = end - start + 1;
		MethodBinding[] result = new MethodBinding[length];
		// iterate methods to resolve them
		for (int i = start, index = 0; i <= end; i++, index++)
			result[index] = resolveTypesFor(this.methods[i]);
		return result;
	}
	return Binding.NO_METHODS;
}
// Answer methods named selector, which take no more than the suggestedParameterLength.
// The suggested parameter length is optional and may not be guaranteed by every type.
@Override
public MethodBinding[] getMethods(char[] selector, int suggestedParameterLength) {

	if (!isPrototype())
		return this.prototype.getMethods(selector, suggestedParameterLength);

	if ((this.tagBits & TagBits.AreMethodsComplete) != 0)
		return getMethods(selector);
	// lazily sort methods
	if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
		int length = this.methods.length;
		if (length > 1)
			ReferenceBinding.sortMethods(this.methods, 0, length);
		this.tagBits |= TagBits.AreMethodsSorted;
	}
	long range;
	if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
		int start = (int) range, end = (int) (range >> 32);
		int length = end - start + 1;
		int count = 0;
		for (int i = start; i <= end; i++) {
			if (this.methods[i].doesParameterLengthMatch(suggestedParameterLength))
				count++;
		}
		if (count == 0) {
			MethodBinding[] result = new MethodBinding[length];
			// iterate methods to resolve them
			for (int i = start, index = 0; i <= end; i++)
				result[index++] = resolveTypesFor(this.methods[i]);
			return result;
		} else {
			MethodBinding[] result = new MethodBinding[count];
			// iterate methods to resolve them
			for (int i = start, index = 0; i <= end; i++) {
				if (this.methods[i].doesParameterLengthMatch(suggestedParameterLength))
					result[index++] = resolveTypesFor(this.methods[i]);
			}
			return result;
		}
	}
	return Binding.NO_METHODS;
}

@Override
public boolean hasMemberTypes() {
	if (!isPrototype())
		return this.prototype.hasMemberTypes();
    return this.memberTypes.length > 0;
}
// NOTE: member types of binary types are resolved when needed
@Override
public TypeVariableBinding getTypeVariable(char[] variableName) {
	if (!isPrototype())
		return this.prototype.getTypeVariable(variableName);

	TypeVariableBinding variable = super.getTypeVariable(variableName);
	variable.resolve();
	return variable;
}
@Override
public boolean hasTypeBit(int bit) {

	if (!isPrototype())
		return this.prototype.hasTypeBit(bit);

	// ensure hierarchy is resolved, which will propagate bits down to us
	boolean wasToleratingMissingTypeProcessingAnnotations = this.environment.mayTolerateMissingType;
	this.environment.mayTolerateMissingType = true;
	try {
		superclass();
		superInterfaces();
	} finally {
		this.environment.mayTolerateMissingType = wasToleratingMissingTypeProcessingAnnotations;
	}
	return (this.typeBits & bit) != 0;
}
private void initializeTypeVariable(TypeVariableBinding variable, TypeVariableBinding[] existingVariables, SignatureWrapper wrapper, char[][][] missingTypeNames, ITypeAnnotationWalker walker) {
	if (!isPrototype()) throw new IllegalStateException();
	// ParameterSignature = Identifier ':' TypeSignature
	//   or Identifier ':' TypeSignature(optional) InterfaceBound(s)
	// InterfaceBound = ':' TypeSignature
	int colon = CharOperation.indexOf(Util.C_COLON, wrapper.signature, wrapper.start);
	wrapper.start = colon + 1; // skip name + ':'
	ReferenceBinding type, firstBound = null;
	short rank = 0;
	if (wrapper.signature[wrapper.start] == Util.C_COLON) {
		type = this.environment.getResolvedJavaBaseType(TypeConstants.JAVA_LANG_OBJECT, null);
		rank++;
	} else {
		TypeBinding typeFromTypeSignature = this.environment.getTypeFromTypeSignature(wrapper, existingVariables, this, missingTypeNames, walker.toTypeBound(rank++));
		if (typeFromTypeSignature instanceof ReferenceBinding) {
			type = (ReferenceBinding) typeFromTypeSignature;
		} else {
			// this should only happen if the signature is corrupted (332423)
			type = this.environment.getResolvedJavaBaseType(TypeConstants.JAVA_LANG_OBJECT, null);
		}
		firstBound = type;
	}

	// variable is visible to its bounds
	variable.modifiers |= ExtraCompilerModifiers.AccUnresolved;
	variable.setSuperClass(type);

	ReferenceBinding[] bounds = null;
	if (wrapper.signature[wrapper.start] == Util.C_COLON) {
		java.util.ArrayList types = new java.util.ArrayList(2);
		do {
			wrapper.start++; // skip ':'
			types.add(this.environment.getTypeFromTypeSignature(wrapper, existingVariables, this, missingTypeNames, walker.toTypeBound(rank++)));
		} while (wrapper.signature[wrapper.start] == Util.C_COLON);
		bounds = new ReferenceBinding[types.size()];
		types.toArray(bounds);
	}

	variable.setSuperInterfaces(bounds == null ? Binding.NO_SUPERINTERFACES : bounds);
	if (firstBound == null) {
		firstBound = variable.superInterfaces.length == 0 ? null : variable.superInterfaces[0];
	}
	variable.setFirstBound(firstBound);
}
/**
 * Returns true if a type is identical to another one,
 * or for generic types, true if compared to its raw type.
 */
@Override
public boolean isEquivalentTo(TypeBinding otherType) {

	if (TypeBinding.equalsEquals(this, otherType)) return true;
	if (otherType == null) return false;
	switch(otherType.kind()) {
		case Binding.WILDCARD_TYPE :
		case Binding.INTERSECTION_TYPE :
			return ((WildcardBinding) otherType).boundCheck(this);
		case Binding.PARAMETERIZED_TYPE:
		/* With the hybrid 1.4/1.5+ projects modes, while establishing type equivalence, we need to
	       be prepared for a type such as Map appearing in one of three forms: As (a) a ParameterizedTypeBinding
	       e.g Map<String, String>, (b) as RawTypeBinding Map#RAW and finally (c) as a BinaryTypeBinding
	       When the usage of a type lacks type parameters, whether we land up with the raw form or not depends
	       on whether the underlying type was "seen to be" a generic type in the particular build environment or
	       not. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=186565 && https://bugs.eclipse.org/bugs/show_bug.cgi?id=328827
		*/
		case Binding.RAW_TYPE :
			return TypeBinding.equalsEquals(otherType.erasure(), this);
	}
	return false;
}
@Override
public boolean isGenericType() {

	if (!isPrototype())
		return this.prototype.isGenericType();

    return this.typeVariables != Binding.NO_TYPE_VARIABLES;
}
@Override
public boolean isHierarchyConnected() {

	if (!isPrototype())
		return this.prototype.isHierarchyConnected();

	return (this.tagBits & (TagBits.HasUnresolvedSuperclass | TagBits.HasUnresolvedSuperinterfaces)) == 0;
}
@Override
public boolean isRepeatableAnnotationType() {
	if (!isPrototype()) throw new IllegalStateException();
	return this.containerAnnotationType != null;
}
@Override
public int kind() {

	if (!isPrototype())
		return this.prototype.kind();

	if (this.typeVariables != Binding.NO_TYPE_VARIABLES)
		return Binding.GENERIC_TYPE;
	return Binding.TYPE;
}
// NOTE: member types of binary types are resolved when needed
@Override
public ReferenceBinding[] memberTypes() {
 	if (!isPrototype()) {
		if ((this.tagBits & TagBits.HasUnresolvedMemberTypes) == 0)
			return this.memberTypes;
		/*
		 * The members obtained from the prototype are already sorted
		 * thus we can safely assume that our local copy of the member types
		 * is sorted, too.
		 */
		ReferenceBinding [] members = this.prototype.memberTypes();
		if (members != null) {
			this.memberTypes = new ReferenceBinding[members.length];
			for (int i = 0; i < members.length; i++)
				this.memberTypes[i] = this.environment.createMemberType(members[i], this);
		}
		this.tagBits &= ~TagBits.HasUnresolvedMemberTypes;
		this.memberTypesSorted = true;
		return this.memberTypes;
	}

	if ((this.tagBits & TagBits.HasUnresolvedMemberTypes) == 0) {
		return maybeSortedMemberTypes();
	}
	for (int i = this.memberTypes.length; --i >= 0;)
		this.memberTypes[i] = (ReferenceBinding) resolveType(this.memberTypes[i], this.environment, false /* no raw conversion for now */);
	this.tagBits &= ~TagBits.HasUnresolvedMemberTypes;
	return maybeSortedMemberTypes();
}

private ReferenceBinding[] maybeSortedMemberTypes() {
	// do not try to sort while we are still resolving
	if ((this.tagBits & TagBits.HasUnresolvedMemberTypes) != 0) {
		return this.memberTypes;
	}
	if (!this.memberTypesSorted) {
		// lazily sort member types
		int length = this.memberTypes.length;
		if (length > 1)
			sortMemberTypes(this.memberTypes, 0, length);
		this.memberTypesSorted = true;
	}
	return this.memberTypes;
}

// NOTE: the return type, arg & exception types of each method of a binary type are resolved when needed
@Override
public MethodBinding[] methods() {

	if (!isPrototype()) {
		return this.methods = this.prototype.methods();
	}

	if ((this.tagBits & TagBits.AreMethodsComplete) != 0)
		return this.methods;

	// lazily sort methods
	if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
		int length = this.methods.length;
		if (length > 1)
			ReferenceBinding.sortMethods(this.methods, 0, length);
		this.tagBits |= TagBits.AreMethodsSorted;
	}
	for (int i = this.methods.length; --i >= 0;)
		resolveTypesFor(this.methods[i]);
	this.tagBits |= TagBits.AreMethodsComplete;
	return this.methods;
}
@Override
public void setHierarchyCheckDone() {
	this.tagBits |= TagBits.BeginHierarchyCheck | TagBits.EndHierarchyCheck;
}

@Override
public TypeBinding prototype() {
	return this.prototype;
}

private boolean isPrototype() {
	return this == this.prototype; //$IDENTITY-COMPARISON$
}
@Override
public boolean isRecord() {
	return (this.modifiers & ExtraCompilerModifiers.AccRecord) != 0;
}

@Override
public ReferenceBinding containerAnnotationType() {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.containerAnnotationType instanceof UnresolvedReferenceBinding) {
		this.containerAnnotationType = (ReferenceBinding) BinaryTypeBinding.resolveType(this.containerAnnotationType, this.environment, false);
	}
	return this.containerAnnotationType;
}
private RecordComponentBinding resolveTypeFor(RecordComponentBinding component) {
	if (!isPrototype())
		return this.prototype.resolveTypeFor(component);

	if ((component.modifiers & ExtraCompilerModifiers.AccUnresolved) == 0)
		return component;

	TypeBinding resolvedType = resolveType(component.type, this.environment, true /* raw conversion */);
	component.type = resolvedType;
	if ((resolvedType.tagBits & TagBits.HasMissingType) != 0) {
		component.tagBits |= TagBits.HasMissingType;
	}
	component.modifiers &= ~ExtraCompilerModifiers.AccUnresolved;
	return component;
}
private FieldBinding resolveTypeFor(FieldBinding field) {

	if (!isPrototype())
		return this.prototype.resolveTypeFor(field);

	if ((field.modifiers & ExtraCompilerModifiers.AccUnresolved) == 0)
		return field;

	TypeBinding resolvedType = resolveType(field.type, this.environment, true /* raw conversion */);
	field.type = resolvedType;
	if ((resolvedType.tagBits & TagBits.HasMissingType) != 0) {
		field.tagBits |= TagBits.HasMissingType;
	}
	field.modifiers &= ~ExtraCompilerModifiers.AccUnresolved;
	return field;
}
MethodBinding resolveTypesFor(MethodBinding method) {

	if (!isPrototype())
		return this.prototype.resolveTypesFor(method);

	if ((method.modifiers & ExtraCompilerModifiers.AccUnresolved) == 0)
		return method;

	if (!method.isConstructor()) {
		TypeBinding resolvedType = resolveType(method.returnType, this.environment, true /* raw conversion */);
		method.returnType = resolvedType;
		if ((resolvedType.tagBits & TagBits.HasMissingType) != 0) {
			method.tagBits |= TagBits.HasMissingType;
		}
	}
	for (int i = method.parameters.length; --i >= 0;) {
		TypeBinding resolvedType = resolveType(method.parameters[i], this.environment, true /* raw conversion */);
		method.parameters[i] = resolvedType;
		if ((resolvedType.tagBits & TagBits.HasMissingType) != 0) {
			method.tagBits |= TagBits.HasMissingType;
		}
	}
	for (int i = method.thrownExceptions.length; --i >= 0;) {
		ReferenceBinding resolvedType = (ReferenceBinding) resolveType(method.thrownExceptions[i], this.environment, true /* raw conversion */);
		method.thrownExceptions[i] = resolvedType;
		if ((resolvedType.tagBits & TagBits.HasMissingType) != 0) {
			method.tagBits |= TagBits.HasMissingType;
		}
	}
	for (int i = method.typeVariables.length; --i >= 0;) {
		method.typeVariables[i].resolve();
	}
	method.modifiers &= ~ExtraCompilerModifiers.AccUnresolved;
	return method;
}
@Override
AnnotationBinding[] retrieveAnnotations(Binding binding) {

	if (!isPrototype())
		return this.prototype.retrieveAnnotations(binding);

	return AnnotationBinding.addStandardAnnotations(super.retrieveAnnotations(binding), binding.getAnnotationTagBits(), this.environment);
}

@Override
public void setContainerAnnotationType(ReferenceBinding value) {
	if (!isPrototype()) throw new IllegalStateException();
	this.containerAnnotationType = value;
}

@Override
public void tagAsHavingDefectiveContainerType() {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.containerAnnotationType != null && this.containerAnnotationType.isValidBinding())
		this.containerAnnotationType = new ProblemReferenceBinding(this.containerAnnotationType.compoundName, this.containerAnnotationType, ProblemReasons.DefectiveContainerAnnotationType);
}

@Override
SimpleLookupTable storedAnnotations(boolean forceInitialize, boolean forceStore) {

	if (!isPrototype())
		return this.prototype.storedAnnotations(forceInitialize, forceStore);

	if (forceInitialize && this.storedAnnotations == null) {
		if (!this.environment.globalOptions.storeAnnotations && !forceStore)
			return null; // not supported during this compile
		this.storedAnnotations = new SimpleLookupTable(3);
	}
	return this.storedAnnotations;
}

//pre: null annotation analysis is enabled
private void scanFieldForNullAnnotation(IBinaryField field, VariableBinding fieldBinding, boolean isEnum, ITypeAnnotationWalker externalAnnotationWalker) {
	if (!isPrototype()) throw new IllegalStateException();

	if (isEnum && (field.getModifiers() & ClassFileConstants.AccEnum) != 0) {
		fieldBinding.tagBits |= TagBits.AnnotationNonNull;
		return; // we know it's nonnull, no need to look for null *annotations* on enum constants.
	}

	if (!CharOperation.equals(this.fPackage.compoundName, TypeConstants.JAVA_LANG_ANNOTATION) // avoid dangerous re-entry via usesNullTypeAnnotations()
			&& this.environment.usesNullTypeAnnotations()) {
		TypeBinding fieldType = fieldBinding.type;
		if (fieldType != null
				&& !fieldType.isBaseType()
				&& (fieldType.tagBits & TagBits.AnnotationNullMASK) == 0
				&& fieldType.acceptsNonNullDefault()) {
				int nullDefaultFromField = getNullDefaultFrom(field.getAnnotations());
				if (nullDefaultFromField == Binding.NO_NULL_DEFAULT
						? hasNonNullDefaultFor(DefaultLocationField, -1)
						: (nullDefaultFromField & DefaultLocationField) != 0) {
					fieldBinding.type = this.environment.createAnnotatedType(fieldType,
							new AnnotationBinding[] { this.environment.getNonNullAnnotation() });
				}
		}
		return; // not using fieldBinding.tagBits when we have type annotations.
	}

	// global option is checked by caller

	if (fieldBinding.type == null || fieldBinding.type.isBaseType())
		return; // null annotations are only applied to reference types

	boolean explicitNullness = false;
	IBinaryAnnotation[] annotations = externalAnnotationWalker != ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER
											? externalAnnotationWalker.getAnnotationsAtCursor(fieldBinding.type.id, false)
											: field.getAnnotations();
	if (annotations != null) {
		for (int i = 0; i < annotations.length; i++) {
			char[] annotationTypeName = annotations[i].getTypeName();
			if (annotationTypeName[0] != Util.C_RESOLVED)
				continue;
			int typeBit = this.environment.getNullAnnotationBit(signature2qualifiedTypeName(annotationTypeName));
			if (typeBit == TypeIds.BitNonNullAnnotation) {
				fieldBinding.tagBits |= TagBits.AnnotationNonNull;
				explicitNullness = true;
				break;
			}
			if (typeBit == TypeIds.BitNullableAnnotation) {
				fieldBinding.tagBits |= TagBits.AnnotationNullable;
				explicitNullness = true;
				break;
			}
		}
	}
	if (explicitNullness && this.externalAnnotationStatus.isPotentiallyUnannotatedLib())
		this.externalAnnotationStatus = ExternalAnnotationStatus.TYPE_IS_ANNOTATED;
	if (!explicitNullness) {
		int nullDefaultFromField = getNullDefaultFrom(field.getAnnotations());
		if (nullDefaultFromField == Binding.NO_NULL_DEFAULT ? hasNonNullDefaultFor(DefaultLocationField, -1)
				: (nullDefaultFromField & DefaultLocationField) != 0) {
			fieldBinding.tagBits |= TagBits.AnnotationNonNull;
		}
	}
}

private void scanMethodForNullAnnotation(IBinaryMethod method, MethodBinding methodBinding, ITypeAnnotationWalker externalAnnotationWalker, boolean useNullTypeAnnotations) {
	if (!isPrototype()) throw new IllegalStateException();
	if (isEnum()) {
		int purpose = 0;
		if (CharOperation.equals(TypeConstants.VALUEOF, method.getSelector())
				&& methodBinding.parameters.length == 1
				&& methodBinding.parameters[0].id == TypeIds.T_JavaLangString)
		{
			purpose = SyntheticMethodBinding.EnumValueOf;
		} else if (CharOperation.equals(TypeConstants.VALUES, method.getSelector())
				&& methodBinding.parameters == Binding.NO_PARAMETERS) {
			purpose = SyntheticMethodBinding.EnumValues;
		}
		if (purpose != 0) {
			boolean needToDefer = this.environment.globalOptions.useNullTypeAnnotations == null;
			if (needToDefer)
				this.environment.deferredEnumMethods.add(methodBinding);
			else
				SyntheticMethodBinding.markNonNull(methodBinding, purpose, this.environment);
			return;
		}
	}

	// return:
	ITypeAnnotationWalker returnWalker = externalAnnotationWalker.toMethodReturn();
	IBinaryAnnotation[] annotations = returnWalker != ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER
								? returnWalker.getAnnotationsAtCursor(methodBinding.returnType.id, false)
								: method.getAnnotations();
	if (annotations != null) {
		int methodDefaultNullness = NO_NULL_DEFAULT;
		for (int i = 0; i < annotations.length; i++) {
			char[] annotationTypeName = annotations[i].getTypeName();
			if (annotationTypeName[0] != Util.C_RESOLVED)
				continue;
			int typeBit = this.environment.getNullAnnotationBit(signature2qualifiedTypeName(annotationTypeName));
			if (typeBit == TypeIds.BitNonNullByDefaultAnnotation) {
				methodDefaultNullness |= getNonNullByDefaultValue(annotations[i], this.environment);
			} else if (typeBit == TypeIds.BitNonNullAnnotation) {
				methodBinding.tagBits |= TagBits.AnnotationNonNull;
				if (this.environment.usesNullTypeAnnotations()) {
					if (methodBinding.returnType != null && !methodBinding.returnType.hasNullTypeAnnotations()) {
						methodBinding.returnType = this.environment.createAnnotatedType(methodBinding.returnType,
								new AnnotationBinding[] { this.environment.getNonNullAnnotation() });
					}
				}
			} else if (typeBit == TypeIds.BitNullableAnnotation) {
				methodBinding.tagBits |= TagBits.AnnotationNullable;
				if (this.environment.usesNullTypeAnnotations()) {
					if (methodBinding.returnType != null && !methodBinding.returnType.hasNullTypeAnnotations()) {
						methodBinding.returnType = this.environment.createAnnotatedType(methodBinding.returnType,
								new AnnotationBinding[] { this.environment.getNullableAnnotation() });
					}
				}
			}
		}
		methodBinding.defaultNullness = methodDefaultNullness;
	}

	// parameters:
	TypeBinding[] parameters = methodBinding.parameters;
	int numVisibleParams = parameters.length;
	int numParamAnnotations = externalAnnotationWalker instanceof IMethodAnnotationWalker
							? ((IMethodAnnotationWalker) externalAnnotationWalker).getParameterCount()
							: method.getAnnotatedParametersCount();
	if (numParamAnnotations > 0) {
		for (int j = 0; j < numVisibleParams; j++) {
			if (numParamAnnotations > 0) {
				int startIndex = numParamAnnotations - numVisibleParams;
				ITypeAnnotationWalker parameterWalker = externalAnnotationWalker.toMethodParameter((short) (j+startIndex));
				IBinaryAnnotation[] paramAnnotations = parameterWalker != ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER
															? parameterWalker.getAnnotationsAtCursor(parameters[j].id, false)
															: method.getParameterAnnotations(j+startIndex, this.fileName);
				if (paramAnnotations != null) {
					for (int i = 0; i < paramAnnotations.length; i++) {
						char[] annotationTypeName = paramAnnotations[i].getTypeName();
						if (annotationTypeName[0] != Util.C_RESOLVED)
							continue;
						int typeBit = this.environment.getNullAnnotationBit(signature2qualifiedTypeName(annotationTypeName));
						if (typeBit == TypeIds.BitNonNullAnnotation) {
							if (methodBinding.parameterNonNullness == null)
								methodBinding.parameterNonNullness = new Boolean[numVisibleParams];
							methodBinding.parameterNonNullness[j] = Boolean.TRUE;
							if (this.environment.usesNullTypeAnnotations()) {
								if (methodBinding.parameters[j] != null
										&& !methodBinding.parameters[j].hasNullTypeAnnotations()) {
									methodBinding.parameters[j] = this.environment.createAnnotatedType(
											methodBinding.parameters[j],
											new AnnotationBinding[] { this.environment.getNonNullAnnotation() });
								}
							}
							break;
						} else if (typeBit == TypeIds.BitNullableAnnotation) {
							if (methodBinding.parameterNonNullness == null)
								methodBinding.parameterNonNullness = new Boolean[numVisibleParams];
							methodBinding.parameterNonNullness[j] = Boolean.FALSE;
							if (this.environment.usesNullTypeAnnotations()) {
								if (methodBinding.parameters[j] != null
										&& !methodBinding.parameters[j].hasNullTypeAnnotations()) {
									methodBinding.parameters[j] = this.environment.createAnnotatedType(
											methodBinding.parameters[j],
											new AnnotationBinding[] { this.environment.getNullableAnnotation() });
								}
							}
							break;
						}
					}
				}
			}
		}
	}
	if (useNullTypeAnnotations && this.externalAnnotationStatus.isPotentiallyUnannotatedLib()) {
		if (methodBinding.returnType.hasNullTypeAnnotations()
				|| (methodBinding.tagBits & TagBits.AnnotationNullMASK) != 0
				|| methodBinding.parameterNonNullness != null) {
			this.externalAnnotationStatus = ExternalAnnotationStatus.TYPE_IS_ANNOTATED;
		} else {
			for (TypeBinding parameter : parameters) {
				if (parameter.hasNullTypeAnnotations()) {
					this.externalAnnotationStatus = ExternalAnnotationStatus.TYPE_IS_ANNOTATED;
					break;
				}
			}
		}
	}
}
// pre: null annotation analysis is enabled
private void scanTypeForNullDefaultAnnotation(IBinaryType binaryType, PackageBinding packageBinding) {
	if (!isPrototype()) throw new IllegalStateException();
	char[][] nonNullByDefaultAnnotationName = this.environment.getNonNullByDefaultAnnotationName();
	if (nonNullByDefaultAnnotationName == null)
		return; // not well-configured to use null annotations

	if (CharOperation.equals(CharOperation.splitOn('/', binaryType.getName()), nonNullByDefaultAnnotationName))
		return; // don't recursively apply @NNBD on @NNBD, neither directly nor via the 'enclosing' package-info.java
	for (String name : this.environment.globalOptions.nonNullByDefaultAnnotationSecondaryNames)
		if (CharOperation.toString(this.compoundName).equals(name))
			return;

	IBinaryAnnotation[] annotations = binaryType.getAnnotations();
	boolean isPackageInfo = CharOperation.equals(sourceName(), TypeConstants.PACKAGE_INFO_NAME);
	if (annotations != null) {
		int nullness = NO_NULL_DEFAULT;
		int length = annotations.length;
		for (int i = 0; i < length; i++) {
			char[] annotationTypeName = annotations[i].getTypeName();
			if (annotationTypeName[0] != Util.C_RESOLVED)
				continue;
			int typeBit = this.environment.getNullAnnotationBit(signature2qualifiedTypeName(annotationTypeName));
			if (typeBit == TypeIds.BitNonNullByDefaultAnnotation) {
				// using NonNullByDefault we need to inspect the details of the value() attribute:
				nullness |= getNonNullByDefaultValue(annotations[i], this.environment);
			}
		}
		this.defaultNullness = nullness;
		if (nullness != NO_NULL_DEFAULT) {
			if (isPackageInfo)
				packageBinding.setDefaultNullness(nullness);
			return;
		}
	}
	if (isPackageInfo) {
		// no default annotations found in package-info
		packageBinding.setDefaultNullness(Binding.NO_NULL_DEFAULT);
		return;
	}
	ReferenceBinding enclosingTypeBinding = this.enclosingType;
	if (enclosingTypeBinding != null) {
		if (setNullDefault(enclosingTypeBinding.getNullDefault()))
			return;
	}
	// no annotation found on the type or its enclosing types
	// check the package-info for default annotation if not already done before
	if (packageBinding.getDefaultNullness() == Binding.NO_NULL_DEFAULT && !isPackageInfo
			&& ((this.typeBits & (TypeIds.BitAnyNullAnnotation)) == 0))
	{
		// this will scan the annotations in package-info
		ReferenceBinding packageInfo = packageBinding.getType(TypeConstants.PACKAGE_INFO_NAME, packageBinding.enclosingModule);
		if (packageInfo == null) {
			packageBinding.setDefaultNullness(Binding.NO_NULL_DEFAULT);
		}
	}
	// no @NonNullByDefault at type level, check containing package:
	setNullDefault(packageBinding.getDefaultNullness());
}

boolean setNullDefault(int newNullDefault) {
	this.defaultNullness = newNullDefault;
	if (newNullDefault != Binding.NO_NULL_DEFAULT) {
		return true;
	}
	return false;
}

/** given an application of @NonNullByDefault convert the annotation argument (if any) into a bitvector a la {@link Binding#NullnessDefaultMASK} */
// pre: null annotation analysis is enabled
static int getNonNullByDefaultValue(IBinaryAnnotation annotation, LookupEnvironment environment) {

	char[] annotationTypeName = annotation.getTypeName();
	char[][] typeName = signature2qualifiedTypeName(annotationTypeName);
	IBinaryElementValuePair[] elementValuePairs = annotation.getElementValuePairs();
	if (elementValuePairs == null || elementValuePairs.length == 0 ) {
		// no argument: apply default default
		ReferenceBinding annotationType = environment.getType(typeName, environment.UnNamedModule); // TODO(SHMOD): null annotations from a module?
		if (annotationType == null) return 0;
		if (annotationType.isUnresolvedType())
			annotationType = ((UnresolvedReferenceBinding) annotationType).resolve(environment, false);
		int nullness = evaluateTypeQualifierDefault(annotationType);
		if (nullness != 0)
			return nullness;
		MethodBinding[] annotationMethods = annotationType.methods();
		if (annotationMethods != null && annotationMethods.length == 1) {
			Object value = annotationMethods[0].getDefaultValue();
			return Annotation.nullLocationBitsFromAnnotationValue(value);
		}
		return DefaultLocationsForTrueValue; // custom unconfigurable NNBD
	} else if (elementValuePairs.length > 0) {
		// evaluate the contained EnumConstantSignatures:
		int nullness = 0;
		for (int i = 0; i < elementValuePairs.length; i++)
			nullness |= Annotation.nullLocationBitsFromAnnotationValue(elementValuePairs[i].getValue());
		return nullness;
	} else {
		// empty argument: cancel all defaults from enclosing scopes
		return NULL_UNSPECIFIED_BY_DEFAULT;
	}
}

public static int evaluateTypeQualifierDefault(ReferenceBinding annotationType) {
	for (AnnotationBinding annotationOnAnnotation : annotationType.getAnnotations()) {
		if(CharOperation.equals(annotationOnAnnotation.getAnnotationType().compoundName[annotationOnAnnotation.type.compoundName.length-1], TYPE_QUALIFIER_DEFAULT)) {
			ElementValuePair[] pairs2 = annotationOnAnnotation.getElementValuePairs();
			if(pairs2 != null) {
				for (ElementValuePair elementValuePair : pairs2) {
					char[] name = elementValuePair.getName();
					if(CharOperation.equals(name, TypeConstants.VALUE)) {
						int nullness = 0;
						Object value = elementValuePair.getValue();
						if(value instanceof Object[]) {
							Object[] values = (Object[]) value;
							for (Object value1 : values)
								nullness |= Annotation.nullLocationBitsFromElementTypeAnnotationValue(value1);
						} else {
							nullness |= Annotation.nullLocationBitsFromElementTypeAnnotationValue(value);
						}
						return nullness;
					}
				}
			}
		}
	}
	return 0;
}

static char[][] signature2qualifiedTypeName(char[] typeSignature) {
	return CharOperation.splitOn('/', typeSignature, 1, typeSignature.length-1); // cut off leading 'L' and trailing ';'
}

@Override
int getNullDefault() {
	return this.defaultNullness;
}

private void scanTypeForContainerAnnotation(IBinaryType binaryType, char[][][] missingTypeNames) {
	if (!isPrototype()) throw new IllegalStateException();
	IBinaryAnnotation[] annotations = binaryType.getAnnotations();
	if (annotations != null) {
		int length = annotations.length;
		for (int i = 0; i < length; i++) {
			char[] annotationTypeName = annotations[i].getTypeName();
			if (CharOperation.equals(annotationTypeName, ConstantPool.JAVA_LANG_ANNOTATION_REPEATABLE)) {
				IBinaryElementValuePair[] elementValuePairs = annotations[i].getElementValuePairs();
				if (elementValuePairs != null && elementValuePairs.length == 1) {
					Object value = elementValuePairs[0].getValue();
					if (value instanceof ClassSignature) {
						this.containerAnnotationType = (ReferenceBinding) this.environment.getTypeFromSignature(((ClassSignature)value).getTypeName(), 0, -1, false, null, missingTypeNames, ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER);
					}
				}
				break;
			}
		}
	}
}

/* Answer the receiver's superclass... null if the receiver is Object or an interface.
*
* NOTE: superclass of a binary type is resolved when needed
*/
@Override
public ReferenceBinding superclass() {

	if (!isPrototype()) {
		return this.superclass = this.prototype.superclass();
	}

	if ((this.tagBits & TagBits.HasUnresolvedSuperclass) == 0)
		return this.superclass;

	// finish resolving the type
	this.superclass = (ReferenceBinding) resolveType(this.superclass, this.environment, true /* raw conversion */);
	this.tagBits &= ~TagBits.HasUnresolvedSuperclass;
	if (this.superclass.problemId() == ProblemReasons.NotFound) {
		this.tagBits |= TagBits.HierarchyHasProblems; // propagate type inconsistency
	} else {
		// make super-type resolving recursive for propagating typeBits downwards
		boolean wasToleratingMissingTypeProcessingAnnotations = this.environment.mayTolerateMissingType;
		this.environment.mayTolerateMissingType = true; // https://bugs.eclipse.org/bugs/show_bug.cgi?id=360164
		try {
			this.superclass.superclass();
			this.superclass.superInterfaces();
		} finally {
			this.environment.mayTolerateMissingType = wasToleratingMissingTypeProcessingAnnotations;
		}
	}
	this.typeBits |= (this.superclass.typeBits & TypeIds.InheritableBits);
	if ((this.typeBits & (TypeIds.BitAutoCloseable|TypeIds.BitCloseable)) != 0) // avoid the side-effects of hasTypeBit()!
		this.typeBits |= applyCloseableClassWhitelists(this.environment.globalOptions);
	detectCircularHierarchy();
	return this.superclass;
}

private void breakLoop() {
	ReferenceBinding currentSuper = this.superclass;
	ReferenceBinding prevSuper = null;
	while (currentSuper != null) {
		if ((currentSuper.tagBits & TagBits.EndHierarchyCheck) != 0 && prevSuper instanceof BinaryTypeBinding) {
			((BinaryTypeBinding)prevSuper).superclass = this.environment.getResolvedType(TypeConstants.JAVA_LANG_OBJECT, null);
			break;
		}
		currentSuper.tagBits |= TagBits.EndHierarchyCheck;
		prevSuper = currentSuper;
		currentSuper = currentSuper.superclass();
	}
}

private void detectCircularHierarchy() {
	ReferenceBinding currentSuper = this.superclass;
	ReferenceBinding tempSuper = null;
	int count = 0;
	int skipCount = 20;
	while (currentSuper != null) {
		if (currentSuper.hasHierarchyCheckStarted())
			break;
		if (TypeBinding.equalsEquals(currentSuper, this) || TypeBinding.equalsEquals(currentSuper, tempSuper)) {
			currentSuper.tagBits |= TagBits.HierarchyHasProblems;
			if (currentSuper.isBinaryBinding())
				breakLoop();

			return;
		}
		if (count == skipCount) {
			tempSuper = currentSuper; // for finding loops that only start after a linear chain
			skipCount *= 2;
			count = 0;
		}
		//Ignore if the super is not yet resolved..
		if (!currentSuper.isHierarchyConnected())
			return;
		currentSuper = currentSuper.superclass();
		count++;
	}
	/* No loop detected and completely found that there is no loop
	 * So, set that info for all the classes
	 */
	tempSuper = this;
	while (TypeBinding.notEquals(currentSuper, tempSuper)) {
		tempSuper.setHierarchyCheckDone();
		tempSuper=tempSuper.superclass();
	}
}

// NOTE: superInterfaces of binary types are resolved when needed
@Override
public ReferenceBinding[] superInterfaces() {

	if (!isPrototype()) {
		return this.superInterfaces = this.prototype.superInterfaces();
	}
	if ((this.tagBits & TagBits.HasUnresolvedSuperinterfaces) == 0)
		return this.superInterfaces;

	for (int i = this.superInterfaces.length; --i >= 0;) {
		this.superInterfaces[i] = (ReferenceBinding) resolveType(this.superInterfaces[i], this.environment, true /* raw conversion */);
		if (this.superInterfaces[i].problemId() == ProblemReasons.NotFound) {
			this.tagBits |= TagBits.HierarchyHasProblems; // propagate type inconsistency
		} else {
			// make super-type resolving recursive for propagating typeBits downwards
			boolean wasToleratingMissingTypeProcessingAnnotations = this.environment.mayTolerateMissingType;
			this.environment.mayTolerateMissingType = true; // https://bugs.eclipse.org/bugs/show_bug.cgi?id=360164
			try {
				this.superInterfaces[i].superclass();
				if (this.superInterfaces[i].isParameterizedType()) {
					ReferenceBinding superType = this.superInterfaces[i].actualType();
					if (TypeBinding.equalsEquals(superType, this)) {
						this.tagBits |= TagBits.HierarchyHasProblems;
						continue;
					}
				}
				this.superInterfaces[i].superInterfaces();
			} finally {
				this.environment.mayTolerateMissingType = wasToleratingMissingTypeProcessingAnnotations;
			}
		}
		this.typeBits |= (this.superInterfaces[i].typeBits & TypeIds.InheritableBits);
		if ((this.typeBits & (TypeIds.BitAutoCloseable|TypeIds.BitCloseable)) != 0) // avoid the side-effects of hasTypeBit()!
			this.typeBits |= applyCloseableInterfaceWhitelists();
	}
	this.tagBits &= ~TagBits.HasUnresolvedSuperinterfaces;
	return this.superInterfaces;
}
@Override
public ReferenceBinding[] permittedTypes() {

	if (!isPrototype()) {
		return this.permittedSubtypes = this.prototype.permittedTypes();
	}
	for (int i = this.permittedSubtypes.length; --i >= 0;)
		this.permittedSubtypes[i] = (ReferenceBinding) resolveType(this.permittedSubtypes[i], this.environment, false);

	// Note: unlike for superinterfaces() hierarchy check not required here since these are subtypes
	return this.permittedSubtypes;
}
@Override
public TypeVariableBinding[] typeVariables() {

	if (!isPrototype()) {
		return this.typeVariables = this.prototype.typeVariables();
	}
 	if ((this.tagBits & TagBits.HasUnresolvedTypeVariables) == 0)
		return this.typeVariables;

 	for (int i = this.typeVariables.length; --i >= 0;)
		this.typeVariables[i].resolve();
	this.tagBits &= ~TagBits.HasUnresolvedTypeVariables;
	return this.typeVariables;
}
@Override
public String toString() {

	if (this.hasTypeAnnotations())
		return annotatedDebugName();

	StringBuilder buffer = new StringBuilder();

	if (isDeprecated()) buffer.append("deprecated "); //$NON-NLS-1$
	if (isPublic()) buffer.append("public "); //$NON-NLS-1$
	if (isProtected()) buffer.append("protected "); //$NON-NLS-1$
	if (isPrivate()) buffer.append("private "); //$NON-NLS-1$
	if (isAbstract() && isClass()) buffer.append("abstract "); //$NON-NLS-1$
	if (isStatic() && isNestedType()) buffer.append("static "); //$NON-NLS-1$
	if (isFinal()) buffer.append("final "); //$NON-NLS-1$

	if (isRecord()) buffer.append("record "); //$NON-NLS-1$
	else if (isEnum()) buffer.append("enum "); //$NON-NLS-1$
	else if (isAnnotationType()) buffer.append("@interface "); //$NON-NLS-1$
	else if (isClass()) buffer.append("class "); //$NON-NLS-1$
	else buffer.append("interface "); //$NON-NLS-1$
	buffer.append((this.compoundName != null) ? CharOperation.toString(this.compoundName) : "UNNAMED TYPE"); //$NON-NLS-1$

	if (this.typeVariables == null) {
		buffer.append("<NULL TYPE VARIABLES>"); //$NON-NLS-1$
	} else if (this.typeVariables != Binding.NO_TYPE_VARIABLES) {
		buffer.append("<"); //$NON-NLS-1$
		for (int i = 0, length = this.typeVariables.length; i < length; i++) {
			if (i  > 0) buffer.append(", "); //$NON-NLS-1$
			if (this.typeVariables[i] == null) {
				buffer.append("NULL TYPE VARIABLE"); //$NON-NLS-1$
				continue;
			}
			char[] varChars = this.typeVariables[i].toString().toCharArray();
			buffer.append(varChars, 1, varChars.length - 2);
		}
		buffer.append(">"); //$NON-NLS-1$
	}
	buffer.append("\n\textends "); //$NON-NLS-1$
	buffer.append((this.superclass != null) ? this.superclass.debugName() : "NULL TYPE"); //$NON-NLS-1$

	if (this.superInterfaces != null) {
		if (this.superInterfaces != Binding.NO_SUPERINTERFACES) {
			buffer.append("\n\timplements : "); //$NON-NLS-1$
			for (int i = 0, length = this.superInterfaces.length; i < length; i++) {
				if (i  > 0)
					buffer.append(", "); //$NON-NLS-1$
				buffer.append((this.superInterfaces[i] != null) ? this.superInterfaces[i].debugName() : "NULL TYPE"); //$NON-NLS-1$
			}
		}
	} else {
		buffer.append("NULL SUPERINTERFACES"); //$NON-NLS-1$
	}

	if (this.permittedSubtypes != null) {
		if (this.permittedSubtypes != Binding.NO_PERMITTEDTYPES) {
			buffer.append("\n\tpermits : "); //$NON-NLS-1$
			for (int i = 0, length = this.permittedSubtypes.length; i < length; i++) {
				if (i  > 0)
					buffer.append(", "); //$NON-NLS-1$
				buffer.append((this.permittedSubtypes[i] != null) ? this.permittedSubtypes[i].debugName() : "NULL TYPE"); //$NON-NLS-1$
			}
		}
	} else {
		buffer.append("NULL PERMITTEDSUBTYPES"); //$NON-NLS-1$
	}

	if (this.enclosingType != null) {
		buffer.append("\n\tenclosing type : "); //$NON-NLS-1$
		buffer.append(this.enclosingType.debugName());
	}

	if (this.fields != null) {
		if (this.fields != Binding.NO_FIELDS) {
			buffer.append("\n/*   fields   */"); //$NON-NLS-1$
			for (int i = 0, length = this.fields.length; i < length; i++)
				buffer.append((this.fields[i] != null) ? "\n" + this.fields[i].toString() : "\nNULL FIELD"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	} else {
		buffer.append("NULL FIELDS"); //$NON-NLS-1$
	}

	if (this.methods != null) {
		if (this.methods != Binding.NO_METHODS) {
			buffer.append("\n/*   methods   */"); //$NON-NLS-1$
			for (int i = 0, length = this.methods.length; i < length; i++)
				buffer.append((this.methods[i] != null) ? "\n" + this.methods[i].toString() : "\nNULL METHOD"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	} else {
		buffer.append("NULL METHODS"); //$NON-NLS-1$
	}

	if (this.memberTypes != null) {
		if (this.memberTypes != Binding.NO_MEMBER_TYPES) {
			buffer.append("\n/*   members   */"); //$NON-NLS-1$
			for (int i = 0, length = this.memberTypes.length; i < length; i++)
				buffer.append((this.memberTypes[i] != null) ? "\n" + this.memberTypes[i].toString() : "\nNULL TYPE"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	} else {
		buffer.append("NULL MEMBER TYPES"); //$NON-NLS-1$
	}

	buffer.append("\n\n\n"); //$NON-NLS-1$
	return buffer.toString();
}

@Override
public TypeBinding unannotated() {
	return this.prototype;
}
@Override
public TypeBinding withoutToplevelNullAnnotation() {
	if (!hasNullTypeAnnotations())
		return this;
	AnnotationBinding[] newAnnotations = this.environment.filterNullTypeAnnotations(this.typeAnnotations);
	if (newAnnotations.length > 0)
		return this.environment.createAnnotatedType(this.prototype, newAnnotations);
	return this.prototype;
}
@Override
MethodBinding[] unResolvedMethods() { // for the MethodVerifier so it doesn't resolve types

	if (!isPrototype())
		return this.prototype.unResolvedMethods();

	return this.methods;
}

@Override
public FieldBinding[] unResolvedFields() {

	if (!isPrototype())
		return this.prototype.unResolvedFields();

	return this.fields;
}
@Override
public ModuleBinding module() {
	if (!isPrototype())
		return this.prototype.module;
	return this.module;
}
}
