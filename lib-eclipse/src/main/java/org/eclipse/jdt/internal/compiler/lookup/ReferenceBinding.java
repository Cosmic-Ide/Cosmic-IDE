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
 *								bug 365519 - editorial cleanup after bug 186342 and bug 365387
 *								bug 358903 - Filter practically unimportant resource leak warnings
 *								bug 365531 - [compiler][null] investigate alternative strategy for internally encoding nullness defaults
 *								bug 388281 - [compiler][null] inheritance of null annotations as an option
 *								bug 395002 - Self bound generic class doesn't resolve bounds properly for wildcards for certain parametrisation.
 *								bug 392862 - [1.8][compiler][null] Evaluate null annotations on array types
 *								bug 400421 - [compiler] Null analysis for fields does not take @com.google.inject.Inject into account
 *								bug 382069 - [null] Make the null analysis consider JUnit's assertNotNull similarly to assertions
 *								bug 392384 - [1.8][compiler][null] Restore nullness info from type annotations in class files
 *								Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								Bug 415291 - [1.8][null] differentiate type incompatibilities due to null annotations
 *								Bug 415043 - [1.8][null] Follow-up re null type annotations after bug 392099
 *								Bug 416176 - [1.8][compiler][null] null type annotations cause grief on type variables
 *								Bug 400874 - [1.8][compiler] Inference infrastructure should evolve to meet JLS8 18.x (Part G of JSR335 spec)
 *								Bug 423504 - [1.8] Implement "18.5.3 Functional Interface Parameterization Inference"
 *								Bug 426792 - [1.8][inference][impl] generify new type inference engine
 *								Bug 428019 - [1.8][compiler] Type inference failure with nested generic invocation.
 *								Bug 427199 - [1.8][resource] avoid resource leak warnings on Streams that have no resource
 *								Bug 418743 - [1.8][null] contradictory annotations on invocation of generic method not reported
 *								Bug 429958 - [1.8][null] evaluate new DefaultLocation attribute of @NonNullByDefault
 *								Bug 431581 - Eclipse compiles what it should not
 *								Bug 440759 - [1.8][null] @NonNullByDefault should never affect wildcards and uses of a type variable
 *								Bug 452788 - [1.8][compiler] Type not correctly inferred in lambda expression
 *								Bug 446442 - [1.8] merge null annotations from super methods
 *								Bug 456532 - [1.8][null] ReferenceBinding.appendNullAnnotation() includes phantom annotations in error messages
 *								Bug 410218 - Optional warning for arguments of "unexpected" types to Map#get(Object), Collection#remove(Object) et al.
 *      Jesper S Moller - Contributions for
 *								bug 382701 - [1.8][compiler] Implement semantic analysis of Lambda expressions & Reference expression
 *								bug 412153 - [1.8][compiler] Check validity of annotations which may be repeatable
 *								bug 527554 - [18.3] Compiler support for JEP 286 Local-Variable Type
 *     Ulrich Grave <ulrich.grave@gmx.de> - Contributions for
 *                              bug 386692 - Missing "unused" warning on "autowired" fields
 *     Pierre-Yves B. <pyvesdev@gmail.com> - Contribution for
 *                              bug 542520 - [JUnit 5] Warning The method xxx from the type X is never used locally is shown when using MethodSource
 *     Sebastian Zarnekow - Contributions for
 *								bug 544921 - [performance] Poor performance with large source files
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.compiler.ast.LambdaExpression;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NullAnnotationMatching;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.JavaFeature;
import org.eclipse.jdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;

/*
Not all fields defined by this type (& its subclasses) are initialized when it is created.
Some are initialized only when needed.

Accessors have been provided for some public fields so all TypeBindings have the same API...
but access public fields directly whenever possible.
Non-public fields have accessors which should be used everywhere you expect the field to be initialized.

null is NOT a valid value for a non-public field... it just means the field is not initialized.
*/

abstract public class ReferenceBinding extends TypeBinding {

	public char[][] compoundName;
	public char[] sourceName;
	public int modifiers;
	public PackageBinding fPackage;
	char[] fileName;
	char[] constantPoolName;
	char[] signature;

	private SimpleLookupTable compatibleCache;

	int typeBits; // additional bits characterizing this type
	protected MethodBinding [] singleAbstractMethod;

	public static final ReferenceBinding LUB_GENERIC = new ReferenceBinding() { /* used for lub computation */
		{ this.id = TypeIds.T_undefined; }
		@Override
		public boolean hasTypeBit(int bit) { return false; }
	};

	private static final Comparator<FieldBinding> FIELD_COMPARATOR = new Comparator<FieldBinding>() {
		@Override
		public int compare(FieldBinding o1, FieldBinding o2) {
			char[] n1 = o1.name;
			char[] n2 = o2.name;
			return ReferenceBinding.compare(n1, n2, n1.length, n2.length);
		}
	};
	private static final Comparator<MethodBinding> METHOD_COMPARATOR = new Comparator<MethodBinding>() {
		@Override
		public int compare(MethodBinding m1, MethodBinding m2) {
			char[] s1 = m1.selector;
			char[] s2 = m2.selector;
			int c = ReferenceBinding.compare(s1, s2, s1.length, s2.length);
			return c == 0 ? m1.parameters.length - m2.parameters.length : c;
		}
	};
	static protected ProblemMethodBinding samProblemBinding = new ProblemMethodBinding(TypeConstants.ANONYMOUS_METHOD, null, ProblemReasons.NoSuchSingleAbstractMethod);


	public ReferenceBinding(ReferenceBinding prototype) {
	super(prototype);

	this.compoundName = prototype.compoundName;
	this.sourceName = prototype.sourceName;
	this.modifiers = prototype.modifiers;
	this.fPackage = prototype.fPackage;
	this.fileName = prototype.fileName;
	this.constantPoolName = prototype.constantPoolName;
	this.signature = prototype.signature;
	this.compatibleCache = prototype.compatibleCache;
	this.typeBits = prototype.typeBits;
	this.singleAbstractMethod = prototype.singleAbstractMethod;
}

public ReferenceBinding() {
	super();
}

public static FieldBinding binarySearch(char[] name, FieldBinding[] sortedFields) {
	if (sortedFields == null)
		return null;
	int max = sortedFields.length;
	if (max == 0)
		return null;
	int left = 0, right = max - 1, nameLength = name.length;
	int mid = 0;
	char[] midName;
	while (left <= right) {
		mid = left + (right - left) /2;
		int compare = compare(name, midName = sortedFields[mid].name, nameLength, midName.length);
		if (compare < 0) {
			right = mid-1;
		} else if (compare > 0) {
			left = mid+1;
		} else {
			return sortedFields[mid];
		}
	}
	return null;
}

/**
 * Returns a combined range value representing: (start + (end<<32)), where start is the index of the first matching method
 * (remember methods are sorted alphabetically on selectors), and end is the index of last contiguous methods with same
 * selector.
 * -1 means no method got found
 * @param selector
 * @param sortedMethods
 * @return (start + (end<<32)) or -1 if no method found
 */
public static long binarySearch(char[] selector, MethodBinding[] sortedMethods) {
	if (sortedMethods == null)
		return -1;
	int max = sortedMethods.length;
	if (max == 0)
		return -1;
	int left = 0, right = max - 1, selectorLength = selector.length;
	int mid = 0;
	char[] midSelector;
	while (left <= right) {
		mid = left + (right - left) /2;
		int compare = compare(selector, midSelector = sortedMethods[mid].selector, selectorLength, midSelector.length);
		if (compare < 0) {
			right = mid-1;
		} else if (compare > 0) {
			left = mid+1;
		} else {
			int start = mid, end = mid;
			// find first method with same selector
			while (start > left && CharOperation.equals(sortedMethods[start-1].selector, selector)){ start--; }
			// find last method with same selector
			while (end < right && CharOperation.equals(sortedMethods[end+1].selector, selector)){ end++; }
			return start + ((long)end<< 32);
		}
	}
	return -1;
}

/**
 * Compares two strings lexicographically.
 * The comparison is based on the Unicode value of each character in
 * the strings.
 *
 * @return  the value <code>0</code> if the str1 is equal to str2;
 *          a value less than <code>0</code> if str1
 *          is lexicographically less than str2;
 *          and a value greater than <code>0</code> if str1 is
 *          lexicographically greater than str2.
 */
static int compare(char[] str1, char[] str2, int len1, int len2) {
	int n= Math.min(len1, len2);
	int i= 0;
	while (n-- != 0) {
		char c1= str1[i];
		char c2= str2[i++];
		if (c1 != c2) {
			return c1 - c2;
		}
	}
	return len1 - len2;
}

/**
 * Sort the field array using a quicksort
 */
public static void sortFields(FieldBinding[] sortedFields, int left, int right) {
	Arrays.sort(sortedFields, left, right, FIELD_COMPARATOR);
}

/**
 * Sort the field array using a quicksort
 */
public static void sortMethods(MethodBinding[] sortedMethods, int left, int right) {
	Arrays.sort(sortedMethods, left, right, METHOD_COMPARATOR);
}

/**
 * Sort the member types using a quicksort
 */
static void sortMemberTypes(ReferenceBinding[] sortedMemberTypes, int left, int right) {
	Arrays.sort(sortedMemberTypes, left, right, BASIC_MEMBER_TYPES_COMPARATOR);
}

/**
 * Compares two reference bindings by the value of the {@link #sourceName} field.
 * A ReferenceBinding with a sourceName field that has the value null is considered
 * to be smaller than a ReferenceBinding that does have a source name.
 */
static final Comparator<ReferenceBinding> BASIC_MEMBER_TYPES_COMPARATOR = (ReferenceBinding o1, ReferenceBinding o2) -> {
	char[] n1 = o1.sourceName;
	char[] n2 = o2.sourceName;
	// n1 or n2 may be null - compare without accessing the length of the array
	if (n1 == null) {
		if (n2 == null) {
			return 0;
		}
		return -1;
	} else if (n2 == null) {
		return 1;
	}
	return ReferenceBinding.compare(n1, n2, n1.length, n2.length);
};

/**
 * Return the array of resolvable fields (resilience)
 */
public FieldBinding[] availableFields() {
	return fields();
}

/**
 * Return the array of resolvable methods (resilience)
 */
public MethodBinding[] availableMethods() {
	return methods();
}

public boolean hasHierarchyCheckStarted() {
	return  (this.tagBits & TagBits.BeginHierarchyCheck) != 0;
}

public void setHierarchyCheckDone() {
	return;
}


/**
 * Answer true if the receiver can be instantiated
 */
@Override
public boolean canBeInstantiated() {
	return (this.modifiers & (ClassFileConstants.AccAbstract | ClassFileConstants.AccInterface | ClassFileConstants.AccEnum | ClassFileConstants.AccAnnotation)) == 0;
}

/**
 * Answer true if the receiver is visible to the invocationPackage.
 */
public boolean canBeSeenBy(PackageBinding invocationPackage) {
	if (isPublic()) return true;
	if (isPrivate()) return false;

	// isProtected() or isDefault()
	return invocationPackage == this.fPackage;
}

/**
 * Answer true if the receiver is visible to the receiverType and the invocationType.
 */
public boolean canBeSeenBy(ReferenceBinding receiverType, ReferenceBinding invocationType) {
	if (isPublic()) return true;

	if (isStatic() && (receiverType.isRawType() || receiverType.isParameterizedType()))
		receiverType = receiverType.actualType(); // outer generics are irrelevant

	if (TypeBinding.equalsEquals(invocationType, this) && TypeBinding.equalsEquals(invocationType, receiverType)) return true;

	if (isProtected()) {
		// answer true if the invocationType is the declaringClass or they are in the same package
		// OR the invocationType is a subclass of the declaringClass
		//    AND the invocationType is the invocationType or its subclass
		//    OR the type is a static method accessed directly through a type
		//    OR previous assertions are true for one of the enclosing type
		if (TypeBinding.equalsEquals(invocationType, this)) return true;
		if (invocationType.fPackage == this.fPackage) return true;

		TypeBinding currentType = invocationType.erasure();
		TypeBinding declaringClass = enclosingType().erasure(); // protected types always have an enclosing one
		if (TypeBinding.equalsEquals(declaringClass, invocationType)) return true;
		if (declaringClass == null) return false; // could be null if incorrect top-level protected type
		//int depth = 0;
		do {
			if (currentType.findSuperTypeOriginatingFrom(declaringClass) != null) return true;
			//depth++;
			currentType = currentType.enclosingType();
		} while (currentType != null);
		return false;
	}

	if (isPrivate()) {
		// answer true if the receiverType is the receiver or its enclosingType
		// AND the invocationType and the receiver have a common enclosingType
		receiverCheck: {
			if (!(TypeBinding.equalsEquals(receiverType, this) || TypeBinding.equalsEquals(receiverType, enclosingType()))) {
				// special tolerance for type variable direct bounds, but only if compliance <= 1.6, see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=334622
				if (receiverType.isTypeVariable()) {
					TypeVariableBinding typeVariable = (TypeVariableBinding) receiverType;
					if (typeVariable.environment.globalOptions.complianceLevel <= ClassFileConstants.JDK1_6 && (typeVariable.isErasureBoundTo(erasure()) || typeVariable.isErasureBoundTo(enclosingType().erasure())))
						break receiverCheck;
				}
				return false;
			}
		}

		if (TypeBinding.notEquals(invocationType, this)) {
			ReferenceBinding outerInvocationType = invocationType;
			ReferenceBinding temp = outerInvocationType.enclosingType();
			while (temp != null) {
				outerInvocationType = temp;
				temp = temp.enclosingType();
			}

			ReferenceBinding outerDeclaringClass = (ReferenceBinding)erasure();
			temp = outerDeclaringClass.enclosingType();
			while (temp != null) {
				outerDeclaringClass = temp;
				temp = temp.enclosingType();
			}
			if (TypeBinding.notEquals(outerInvocationType, outerDeclaringClass)) return false;
		}
		return true;
	}

	// isDefault()
	if (invocationType.fPackage != this.fPackage) return false;

	ReferenceBinding currentType = receiverType;
	TypeBinding originalDeclaringClass = (enclosingType() == null ? this : enclosingType()).original();
	do {
		if (currentType.isCapture()) {  // https://bugs.eclipse.org/bugs/show_bug.cgi?id=285002
			if (TypeBinding.equalsEquals(originalDeclaringClass, currentType.erasure().original())) return true;
		} else {
			if (TypeBinding.equalsEquals(originalDeclaringClass, currentType.original())) return true;
		}
		PackageBinding currentPackage = currentType.fPackage;
		// package could be null for wildcards/intersection types, ignore and recurse in superclass
		if (currentPackage != null && currentPackage != this.fPackage) return false;
	} while ((currentType = currentType.superclass()) != null);
	return false;
}

/**
 * Answer true if the receiver is visible to the type provided by the scope.
 */
@Override
public boolean canBeSeenBy(Scope scope) {
	if (isPublic()) return true;

	SourceTypeBinding invocationType = scope.enclosingSourceType();
	if (TypeBinding.equalsEquals(invocationType, this)) return true;

	if (invocationType == null) // static import call
		return !isPrivate() && scope.getCurrentPackage() == this.fPackage;

	if (isProtected()) {
		// answer true if the invocationType is the declaringClass or they are in the same package
		// OR the invocationType is a subclass of the declaringClass
		//    AND the invocationType is the invocationType or its subclass
		//    OR the type is a static method accessed directly through a type
		//    OR previous assertions are true for one of the enclosing type
		if (invocationType.fPackage == this.fPackage) return true;

		TypeBinding declaringClass = enclosingType(); // protected types always have an enclosing one
		if (declaringClass == null) return false; // could be null if incorrect top-level protected type
		declaringClass = declaringClass.erasure();// erasure cannot be null
		TypeBinding currentType = invocationType.erasure();
		// int depth = 0;
		do {
			if (TypeBinding.equalsEquals(declaringClass, invocationType)) return true;
			if (currentType.findSuperTypeOriginatingFrom(declaringClass) != null) return true;
			// depth++;
			currentType = currentType.enclosingType();
		} while (currentType != null);
		return false;
	}
	if (isPrivate()) {
		// answer true if the receiver and the invocationType have a common enclosingType
		// already know they are not the identical type
		ReferenceBinding outerInvocationType = invocationType;
		ReferenceBinding temp = outerInvocationType.enclosingType();
		while (temp != null) {
			outerInvocationType = temp;
			temp = temp.enclosingType();
		}

		ReferenceBinding outerDeclaringClass = (ReferenceBinding)erasure();
		temp = outerDeclaringClass.enclosingType();
		while (temp != null) {
			outerDeclaringClass = temp;
			temp = temp.enclosingType();
		}
		return TypeBinding.equalsEquals(outerInvocationType, outerDeclaringClass);
	}

	// isDefault()
	return invocationType.fPackage == this.fPackage;
}

public char[] computeGenericTypeSignature(TypeVariableBinding[] typeVariables) {

	boolean isMemberOfGeneric = isMemberType() && hasEnclosingInstanceContext() && (enclosingType().modifiers & ExtraCompilerModifiers.AccGenericSignature) != 0;
	if (typeVariables == Binding.NO_TYPE_VARIABLES && !isMemberOfGeneric) {
		return signature();
	}
	StringBuilder sig = new StringBuilder(10);
	if (isMemberOfGeneric) {
	    char[] typeSig = enclosingType().genericTypeSignature();
	    sig.append(typeSig, 0, typeSig.length-1); // copy all but trailing semicolon
	    sig.append('.'); // NOTE: cannot override trailing ';' with '.' in enclosing signature, since shared char[]
	    sig.append(this.sourceName);
	}	else {
	    char[] typeSig = signature();
	    sig.append(typeSig, 0, typeSig.length-1); // copy all but trailing semicolon
	}
	if (typeVariables == Binding.NO_TYPE_VARIABLES) {
	    sig.append(';');
	} else {
	    sig.append('<');
	    for (int i = 0, length = typeVariables.length; i < length; i++) {
	        sig.append(typeVariables[i].genericTypeSignature());
	    }
	    sig.append(">;"); //$NON-NLS-1$
	}
	int sigLength = sig.length();
	char[] result = new char[sigLength];
	sig.getChars(0, sigLength, result, 0);
	return result;
}

public void computeId() {
	// note that more (configurable) ids are assigned from PackageBinding#checkIfNullAnnotationType()

	// try to avoid multiple checks against a package/type name
	switch (this.compoundName.length) {

		case 3 :
			char[] packageName = this.compoundName[0];
			// expect only java.*.* and javax.*.* and junit.*.* and org.junit.*
			switch (packageName.length) {
				case 3: // only one type in this group, yet:
					if (CharOperation.equals(TypeConstants.ORG_JUNIT_ASSERT, this.compoundName))
						this.id = TypeIds.T_OrgJunitAssert;
					else if (CharOperation.equals(TypeConstants.JDK_INTERNAL_PREVIEW_FEATURE, this.compoundName)
							|| CharOperation.equals(TypeConstants.JDK_INTERNAL_JAVAC_PREVIEW_FEATURE, this.compoundName))
						this.id = TypeIds.T_JdkInternalPreviewFeature;
					return;
				case 4:
					if (!CharOperation.equals(TypeConstants.JAVA, packageName))
						return;
					break; // continue below ...
				case 5:
					switch (packageName[1]) {
						case 'a':
							if (CharOperation.equals(TypeConstants.JAVAX_ANNOTATION_INJECT_INJECT, this.compoundName))
								this.id = TypeIds.T_JavaxInjectInject;
							return;
						case 'u':
							if (CharOperation.equals(TypeConstants.JUNIT_FRAMEWORK_ASSERT, this.compoundName))
								this.id = TypeIds.T_JunitFrameworkAssert;
							return;
					}
					return;
				default: return;
			}
			// ... at this point we know it's java.*.*

			packageName = this.compoundName[1];
			if (packageName.length == 0) return; // just to be safe
			char[] typeName = this.compoundName[2];
			if (typeName.length == 0) return; // just to be safe
			// remaining types MUST be in java.*.*
			if (!CharOperation.equals(TypeConstants.LANG, this.compoundName[1])) {
				switch (packageName[0]) {
					case 'i' :
						if (CharOperation.equals(packageName, TypeConstants.IO)) {
							switch (typeName[0]) {
								case 'C' :
									if (CharOperation.equals(typeName, TypeConstants.JAVA_IO_CLOSEABLE[2]))
										this.typeBits |= TypeIds.BitCloseable; // don't assign id, only typeBit (for analysis of resource leaks)
									return;
								case 'E' :
									if (CharOperation.equals(typeName, TypeConstants.JAVA_IO_EXTERNALIZABLE[2]))
										this.id = TypeIds.T_JavaIoExternalizable;
									return;
								case 'I' :
									if (CharOperation.equals(typeName, TypeConstants.JAVA_IO_IOEXCEPTION[2]))
										this.id = TypeIds.T_JavaIoException;
									return;
								case 'O' :
									if (CharOperation.equals(typeName, TypeConstants.JAVA_IO_OBJECTSTREAMEXCEPTION[2]))
										this.id = TypeIds.T_JavaIoObjectStreamException;
									return;
								case 'P' :
									if (CharOperation.equals(typeName, TypeConstants.JAVA_IO_PRINTSTREAM[2]))
										this.id = TypeIds.T_JavaIoPrintStream;
									return;
								case 'S' :
									if (CharOperation.equals(typeName, TypeConstants.JAVA_IO_SERIALIZABLE[2]))
										this.id = TypeIds.T_JavaIoSerializable;
									return;
							}
						}
						return;
					case 'u' :
						if (CharOperation.equals(packageName, TypeConstants.UTIL)) {
							switch (typeName[0]) {
								case 'C' :
									if (CharOperation.equals(typeName, TypeConstants.JAVA_UTIL_COLLECTION[2])) {
										this.id = TypeIds.T_JavaUtilCollection;
										this.typeBits |= TypeIds.BitCollection;
									}
									return;
								case 'I' :
									if (CharOperation.equals(typeName, TypeConstants.JAVA_UTIL_ITERATOR[2]))
										this.id = TypeIds.T_JavaUtilIterator;
									return;
								case 'L' :
									if (CharOperation.equals(typeName, TypeConstants.JAVA_UTIL_LIST[2])) {
										this.id = TypeIds.T_JavaUtilList;
										this.typeBits |= TypeIds.BitList;
									}
									return;
								case 'M' :
									if (CharOperation.equals(typeName, TypeConstants.JAVA_UTIL_MAP[2])) {
										this.id = TypeIds.T_JavaUtilMap;
										this.typeBits |= TypeIds.BitMap;
									}
									return;
								case 'O' :
									if (CharOperation.equals(typeName, TypeConstants.JAVA_UTIL_OBJECTS[2]))
										this.id = TypeIds.T_JavaUtilObjects;
									return;
							}
						}
						return;
				}
				return;
			}

			// remaining types MUST be in java.lang.*
			switch (typeName[0]) {
				case 'A' :
					switch(typeName.length) {
						case 13 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_AUTOCLOSEABLE[2])) {
								this.id = TypeIds.T_JavaLangAutoCloseable;
								this.typeBits |= TypeIds.BitAutoCloseable;
							}
							return;
						case 14:
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_ASSERTIONERROR[2]))
								this.id = TypeIds.T_JavaLangAssertionError;
							return;
					}
					return;
				case 'B' :
					switch (typeName.length) {
						case 4 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_BYTE[2]))
								this.id = TypeIds.T_JavaLangByte;
							return;
						case 7 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_BOOLEAN[2]))
								this.id = TypeIds.T_JavaLangBoolean;
							return;
					}
					return;
				case 'C' :
					switch (typeName.length) {
						case 5 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_CLASS[2]))
								this.id = TypeIds.T_JavaLangClass;
							return;
						case 9 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_CHARACTER[2]))
								this.id = TypeIds.T_JavaLangCharacter;
							else if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_CLONEABLE[2]))
							    this.id = TypeIds.T_JavaLangCloneable;
							return;
						case 22 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_CLASSNOTFOUNDEXCEPTION[2]))
								this.id = TypeIds.T_JavaLangClassNotFoundException;
							return;
					}
					return;
				case 'D' :
					switch (typeName.length) {
						case 6 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_DOUBLE[2]))
								this.id = TypeIds.T_JavaLangDouble;
							return;
						case 10 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_DEPRECATED[2]))
								this.id = TypeIds.T_JavaLangDeprecated;
							return;
					}
					return;
				case 'E' :
					switch (typeName.length) {
						case 4 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_ENUM[2]))
								this.id = TypeIds.T_JavaLangEnum;
							return;
						case 5 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_ERROR[2]))
								this.id = TypeIds.T_JavaLangError;
							return;
						case 9 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_EXCEPTION[2]))
								this.id = TypeIds.T_JavaLangException;
							return;
					}
					return;
				case 'F' :
					if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_FLOAT[2]))
						this.id = TypeIds.T_JavaLangFloat;
					else if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_FUNCTIONAL_INTERFACE[2]))
						this.id = TypeIds.T_JavaLangFunctionalInterface;
					return;
				case 'I' :
					switch (typeName.length) {
						case 7 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_INTEGER[2]))
								this.id = TypeIds.T_JavaLangInteger;
							return;
						case 8 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_ITERABLE[2]))
								this.id = TypeIds.T_JavaLangIterable;
							return;
						case 24 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_ILLEGALARGUMENTEXCEPTION[2]))
								this.id = TypeIds.T_JavaLangIllegalArgumentException;
							return;
					}
					return;
				case 'L' :
					if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_LONG[2]))
						this.id = TypeIds.T_JavaLangLong;
					return;
				case 'N' :
					if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_NOCLASSDEFERROR[2]))
						this.id = TypeIds.T_JavaLangNoClassDefError;
					return;
				case 'O' :
					switch (typeName.length) {
						case 6 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_OBJECT[2]))
								this.id = TypeIds.T_JavaLangObject;
							return;
						case 8 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_OVERRIDE[2]))
								this.id = TypeIds.T_JavaLangOverride;
							return;
					}
					return;
				case 'R' :
					if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_RUNTIMEEXCEPTION[2]))
						this.id = 	TypeIds.T_JavaLangRuntimeException;
					if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_RECORD[2]))
						this.id = TypeIds.T_JavaLangRecord;
					break;
				case 'S' :
					switch (typeName.length) {
						case 5 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_SHORT[2]))
								this.id = TypeIds.T_JavaLangShort;
							return;
						case 6 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_STRING[2]))
								this.id = TypeIds.T_JavaLangString;
							else if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_SYSTEM[2]))
								this.id = TypeIds.T_JavaLangSystem;
							return;
						case 11 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_SAFEVARARGS[2]))
								this.id = TypeIds.T_JavaLangSafeVarargs;
							return;
						case 12 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_STRINGBUFFER[2]))
								this.id = TypeIds.T_JavaLangStringBuffer;
							return;
						case 13 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_STRINGBUILDER[2]))
								this.id = TypeIds.T_JavaLangStringBuilder;
							return;
						case 16 :
							if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_SUPPRESSWARNINGS[2]))
								this.id = TypeIds.T_JavaLangSuppressWarnings;
							return;
					}
					return;
				case 'T' :
					if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_THROWABLE[2]))
						this.id = TypeIds.T_JavaLangThrowable;
					return;
				case 'V' :
					if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_VOID[2]))
						this.id = TypeIds.T_JavaLangVoid;
					return;
			}
		break;

		case 4:
			// expect one type from com.*.*.*:
			if (CharOperation.equals(TypeConstants.COM_GOOGLE_INJECT_INJECT, this.compoundName)) {
				this.id = TypeIds.T_ComGoogleInjectInject;
				return;
			}
			// otherwise only expect java.*.*.*
			if (!CharOperation.equals(TypeConstants.JAVA, this.compoundName[0]))
				return;
			packageName = this.compoundName[1];
			if (packageName.length == 0) return; // just to be safe

			packageName = this.compoundName[2];
			if (packageName.length == 0) return; // just to be safe
			typeName = this.compoundName[3];
			if (typeName.length == 0) return; // just to be safe
			switch (packageName[0]) {
				case 'a' :
					if (CharOperation.equals(packageName, TypeConstants.ANNOTATION)) {
						switch (typeName[0]) {
							case 'A' :
								if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_ANNOTATION_ANNOTATION[3]))
									this.id = TypeIds.T_JavaLangAnnotationAnnotation;
								return;
							case 'D' :
								if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_ANNOTATION_DOCUMENTED[3]))
									this.id = TypeIds.T_JavaLangAnnotationDocumented;
								return;
							case 'E' :
								if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_ANNOTATION_ELEMENTTYPE[3]))
									this.id = TypeIds.T_JavaLangAnnotationElementType;
								return;
							case 'I' :
								if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_ANNOTATION_INHERITED[3]))
									this.id = TypeIds.T_JavaLangAnnotationInherited;
								return;
							case 'R' :
								switch (typeName.length) {
									case 9 :
										if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_ANNOTATION_RETENTION[3]))
											this.id = TypeIds.T_JavaLangAnnotationRetention;
										return;
									case 10 :
										if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_ANNOTATION_REPEATABLE[3]))
											this.id = TypeIds.T_JavaLangAnnotationRepeatable;
										return;
									case 15 :
										if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_ANNOTATION_RETENTIONPOLICY[3]))
											this.id = TypeIds.T_JavaLangAnnotationRetentionPolicy;
										return;
								}
								return;
							case 'T' :
								if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_ANNOTATION_TARGET[3]))
									this.id = TypeIds.T_JavaLangAnnotationTarget;
								return;
						}
					}
					return;
				case 'i':
					if (CharOperation.equals(packageName, TypeConstants.INVOKE)) {
						if (typeName.length == 0) return; // just to be safe
						switch (typeName[0]) {
							case 'M' :
								if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_INVOKE_METHODHANDLE_$_POLYMORPHICSIGNATURE[3]))
									this.id = TypeIds.T_JavaLangInvokeMethodHandlePolymorphicSignature;
								return;
						}
					}
					return;
				case 'r' :
					if (CharOperation.equals(packageName, TypeConstants.REFLECT)) {
						switch (typeName[0]) {
							case 'C' :
								if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_REFLECT_CONSTRUCTOR[2]))
									this.id = TypeIds.T_JavaLangReflectConstructor;
								return;
							case 'F' :
								if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_REFLECT_FIELD[2]))
									this.id = TypeIds.T_JavaLangReflectField;
								return;
							case 'M' :
								if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_REFLECT_METHOD[2]))
									this.id = TypeIds.T_JavaLangReflectMethod;
								return;
						}
					}
					return;
			}
			break;
		case 5 :
			packageName = this.compoundName[0];
			switch (packageName[0]) {
				case 'j' :
					if (!CharOperation.equals(TypeConstants.JAVA, this.compoundName[0]))
						return;
					packageName = this.compoundName[1];
					if (packageName.length == 0) return; // just to be safe

					if (CharOperation.equals(TypeConstants.LANG, packageName)) {
						packageName = this.compoundName[2];
						if (packageName.length == 0) return; // just to be safe
						switch (packageName[0]) {
							case 'i' :
								if (CharOperation.equals(packageName, TypeConstants.INVOKE)) {
									typeName = this.compoundName[3];
									if (typeName.length == 0) return; // just to be safe
									switch (typeName[0]) {
										case 'M' :
											char[] memberTypeName = this.compoundName[4];
											if (memberTypeName.length == 0) return; // just to be safe
											if (CharOperation.equals(typeName, TypeConstants.JAVA_LANG_INVOKE_METHODHANDLE_POLYMORPHICSIGNATURE[3])
													&& CharOperation.equals(memberTypeName, TypeConstants.JAVA_LANG_INVOKE_METHODHANDLE_POLYMORPHICSIGNATURE[4]))
												this.id = TypeIds.T_JavaLangInvokeMethodHandlePolymorphicSignature;
											return;
									}
								}
								return;
						}
						return;
					}
					return;
				case 'o':
					if (!CharOperation.equals(TypeConstants.ORG, this.compoundName[0]))
						return;
					packageName = this.compoundName[1];
					if (packageName.length == 0) return; // just to be safe

					switch (packageName[0]) {
						case 'e':
							if (CharOperation.equals(TypeConstants.ECLIPSE, packageName)) {
								packageName = this.compoundName[2];
								if (packageName.length == 0) return; // just to be safe
								switch (packageName[0]) {
									case 'c' :
										if (CharOperation.equals(packageName, TypeConstants.CORE)) {
											typeName = this.compoundName[3];
											if (typeName.length == 0) return; // just to be safe
											switch (typeName[0]) {
												case 'r' :
													char[] memberTypeName = this.compoundName[4];
													if (memberTypeName.length == 0) return; // just to be safe
													if (CharOperation.equals(typeName, TypeConstants.ORG_ECLIPSE_CORE_RUNTIME_ASSERT[3])
															&& CharOperation.equals(memberTypeName, TypeConstants.ORG_ECLIPSE_CORE_RUNTIME_ASSERT[4]))
														this.id = TypeIds.T_OrgEclipseCoreRuntimeAssert;
													return;
											}
										}
										return;
								}
								return;
							}
							return;
						case 'a':
							if (CharOperation.equals(TypeConstants.APACHE, packageName)) {
								if (CharOperation.equals(TypeConstants.COMMONS, this.compoundName[2])) {
									if (CharOperation.equals(TypeConstants.ORG_APACHE_COMMONS_LANG_VALIDATE, this.compoundName))
										this.id = TypeIds.T_OrgApacheCommonsLangValidate;
									else if (CharOperation.equals(TypeConstants.ORG_APACHE_COMMONS_LANG3_VALIDATE, this.compoundName))
										this.id = TypeIds.T_OrgApacheCommonsLang3Validate;
								}
							}
							return;
						case 'j':
							if (CharOperation.equals(TypeConstants.ORG_JUNIT_JUPITER_API_ASSERTIONS, this.compoundName))
								this.id = TypeIds.T_OrgJunitJupiterApiAssertions;
							return;
					}
					return;
				case 'c':
					if (!CharOperation.equals(TypeConstants.COM, this.compoundName[0]))
						return;
					if (CharOperation.equals(TypeConstants.COM_GOOGLE_COMMON_BASE_PRECONDITIONS, this.compoundName))
						this.id = TypeIds.T_ComGoogleCommonBasePreconditions;
					return;
			}
			break;
		case 6:
			if (CharOperation.equals(TypeConstants.ORG, this.compoundName[0])) {
				if (CharOperation.equals(TypeConstants.SPRING, this.compoundName[1])) {
					if (CharOperation.equals(TypeConstants.AUTOWIRED, this.compoundName[5])) {
						if (CharOperation.equals(TypeConstants.ORG_SPRING_AUTOWIRED, this.compoundName)) {
							this.id = TypeIds.T_OrgSpringframeworkBeansFactoryAnnotationAutowired;
						}
					}
					return;
				}
				if (CharOperation.equals(TypeConstants.JUNIT, this.compoundName[1])) {
					if (CharOperation.equals(TypeConstants.METHOD_SOURCE, this.compoundName[5])) {
						if (CharOperation.equals(TypeConstants.ORG_JUNIT_METHOD_SOURCE, this.compoundName)) {
							this.id = TypeIds.T_OrgJunitJupiterParamsProviderMethodSource;
						}
					}
					return;
				}
				if (!CharOperation.equals(TypeConstants.JDT, this.compoundName[2]) || !CharOperation.equals(TypeConstants.ITYPEBINDING, this.compoundName[5]))
					return;
				if (CharOperation.equals(TypeConstants.ORG_ECLIPSE_JDT_CORE_DOM_ITYPEBINDING, this.compoundName))
					this.typeBits |= TypeIds.BitUninternedType;
			}
			break;
		case 7 :
			if (!CharOperation.equals(TypeConstants.JDT, this.compoundName[2]) || !CharOperation.equals(TypeConstants.TYPEBINDING, this.compoundName[6]))
				return;
			if (CharOperation.equals(TypeConstants.ORG_ECLIPSE_JDT_INTERNAL_COMPILER_LOOKUP_TYPEBINDING, this.compoundName))
				this.typeBits |= TypeIds.BitUninternedType;
			break;
	}
}

public void computeId(LookupEnvironment environment) {
	environment.getUnannotatedType(this);
}

/**
 * p.X<T extends Y & I, U extends Y> {} -> Lp/X<TT;TU;>;
 */
@Override
public char[] computeUniqueKey(boolean isLeaf) {
	if (!isLeaf) return signature();
	return genericTypeSignature();
}

/**
 * Answer the receiver's constant pool name.
 *
 * NOTE: This method should only be used during/after code gen.
 */
@Override
public char[] constantPoolName() /* java/lang/Object */ {
	if (this.constantPoolName != null) return this.constantPoolName;
	return this.constantPoolName = CharOperation.concatWith(this.compoundName, '/');
}

@Override
public String debugName() {
	return (this.compoundName != null) ? this.hasTypeAnnotations() ? annotatedDebugName() : new String(readableName()) : "UNNAMED TYPE"; //$NON-NLS-1$
}

@Override
public int depth() {
	int depth = 0;
	ReferenceBinding current = this;
	while ((current = current.enclosingType()) != null)
		depth++;
	return depth;
}

public boolean detectAnnotationCycle() {
	if ((this.tagBits & TagBits.EndAnnotationCheck) == TagBits.EndAnnotationCheck) return false; // already checked
	if ((this.tagBits & TagBits.BeginAnnotationCheck) == TagBits.BeginAnnotationCheck) return true; // in the middle of checking its methods

	this.tagBits |= TagBits.BeginAnnotationCheck;
	MethodBinding[] currentMethods = methods();
	boolean inCycle = false; // check each method before failing
	for (int i = 0, l = currentMethods.length; i < l; i++) {
		TypeBinding returnType = currentMethods[i].returnType.leafComponentType().erasure();
		if (TypeBinding.equalsEquals(this, returnType)) {
			if (this instanceof SourceTypeBinding) {
				MethodDeclaration decl = (MethodDeclaration) currentMethods[i].sourceMethod();
				((SourceTypeBinding) this).scope.problemReporter().annotationCircularity(this, this, decl != null ? decl.returnType : null);
			}
		} else if (returnType.isAnnotationType() && ((ReferenceBinding) returnType).detectAnnotationCycle()) {
			if (this instanceof SourceTypeBinding) {
				MethodDeclaration decl = (MethodDeclaration) currentMethods[i].sourceMethod();
				((SourceTypeBinding) this).scope.problemReporter().annotationCircularity(this, returnType, decl != null ? decl.returnType : null);
			}
			inCycle = true;
		}
	}
	if (inCycle)
		return true;
	this.tagBits &= (~TagBits.BeginAnnotationCheck);
	this.tagBits |= TagBits.EndAnnotationCheck;
	return false;
}

public final ReferenceBinding enclosingTypeAt(int relativeDepth) {
	ReferenceBinding current = this;
	while (relativeDepth-- > 0 && current != null)
		current = current.enclosingType();
	return current;
}

public int enumConstantCount() {
	int count = 0;
	FieldBinding[] fields = fields();
	for (int i = 0, length = fields.length; i < length; i++) {
		if ((fields[i].modifiers & ClassFileConstants.AccEnum) != 0) count++;
	}
	return count;
}

public int fieldCount() {
	return fields().length;
}

public FieldBinding[] fields() {
	return Binding.NO_FIELDS;
}

public RecordComponentBinding[] components() {
	return Binding.NO_COMPONENTS;
}

public final int getAccessFlags() {
	return this.modifiers & ExtraCompilerModifiers.AccJustFlag;
}

/**
 * @return the JSR 175 annotations for this type.
 */
@Override
public AnnotationBinding[] getAnnotations() {
	return retrieveAnnotations(prototype());
}

/**
 * @see org.eclipse.jdt.internal.compiler.lookup.Binding#getAnnotationTagBits()
 */
@Override
public long getAnnotationTagBits() {
	return this.tagBits;
}

/**
 * @return the enclosingInstancesSlotSize
 */
public int getEnclosingInstancesSlotSize() {
	if (isStatic()) return 0;
	return enclosingType() == null ? 0 : 1;
}

public MethodBinding getExactConstructor(TypeBinding[] argumentTypes) {
	return null;
}

public MethodBinding getExactMethod(char[] selector, TypeBinding[] argumentTypes, CompilationUnitScope refScope) {
	return null;
}
public FieldBinding getField(char[] fieldName, boolean needResolve) {
	return null;
}
/**
 * @see org.eclipse.jdt.internal.compiler.env.IDependent#getFileName()
 */
public char[] getFileName() {
	return this.fileName;
}

/**
 * Find the member type with the given simple typeName. Benefits from the fact that
 * the array of {@link #memberTypes()} is sorted.
 */
public ReferenceBinding getMemberType(char[] typeName) {
	ReferenceBinding[] memberTypes = memberTypes();
	int memberTypeIndex = binarySearch(typeName, memberTypes);
	if (memberTypeIndex >= 0) {
		return memberTypes[memberTypeIndex];
	}
	return null;
}

/**
 * Search the given sourceName in the list of sorted member types.
 *
 * Neither the array of sortedMemberTypes nor the given sourceName may be null.
 */
static int binarySearch(char[] sourceName, ReferenceBinding[] sortedMemberTypes) {
	if (sortedMemberTypes == null)
		return -1;
	int max = sortedMemberTypes.length, nameLength = sourceName.length;
	if (max == 0)
		return -1;
	int left = 0, right = max - 1;
	while (left <= right) {
		int mid = left + (right - left) / 2;
		char[] midName = sortedMemberTypes[mid].sourceName;
		// The read source name may be null. In that case, the given sourceName is considered
		// to be larger than the current value at mid.
		int compare = midName == null ? 1 : compare(sourceName, midName, nameLength, midName.length);
		if (compare < 0) {
			right = mid-1;
		} else if (compare > 0) {
			left = mid+1;
		} else {
			return mid;
		}
	}
	return -1;
}

@Override
public MethodBinding[] getMethods(char[] selector) {
	return Binding.NO_METHODS;
}

// Answer methods named selector, which take no more than the suggestedParameterLength.
// The suggested parameter length is optional and may not be guaranteed by every type.
public MethodBinding[] getMethods(char[] selector, int suggestedParameterLength) {
	return getMethods(selector);
}

/**
 * @return the outerLocalVariablesSlotSize
 */
public int getOuterLocalVariablesSlotSize() {
	return 0;
}

@Override
public PackageBinding getPackage() {
	return this.fPackage;
}

public TypeVariableBinding getTypeVariable(char[] variableName) {
	TypeVariableBinding[] typeVariables = typeVariables();
	for (int i = typeVariables.length; --i >= 0;)
		if (CharOperation.equals(typeVariables[i].sourceName, variableName))
			return typeVariables[i];
	return null;
}

@Override
public int hashCode() {
	// ensure ReferenceBindings hash to the same position as UnresolvedReferenceBindings so they can be replaced without rehashing
	// ALL ReferenceBindings are unique when created so equals() is the same as ==
	return (this.compoundName == null || this.compoundName.length == 0)
		? super.hashCode()
		: CharOperation.hashCode(this.compoundName[this.compoundName.length - 1]);
}

final int identityHashCode() {
	return super.hashCode();
}

/**
 * Returns true if the two types have an incompatible common supertype,
 * e.g. List<String> and List<Integer>
 */
public boolean hasIncompatibleSuperType(ReferenceBinding otherType) {

    if (TypeBinding.equalsEquals(this, otherType)) return false;

	ReferenceBinding[] interfacesToVisit = null;
	int nextPosition = 0;
    ReferenceBinding currentType = this;
	TypeBinding match;
	do {
		match = otherType.findSuperTypeOriginatingFrom(currentType);
		if (match != null && match.isProvablyDistinct(currentType))
			return true;

		ReferenceBinding[] itsInterfaces = currentType.superInterfaces();
		if (itsInterfaces != null && itsInterfaces != Binding.NO_SUPERINTERFACES) {
			if (interfacesToVisit == null) {
				interfacesToVisit = itsInterfaces;
				nextPosition = interfacesToVisit.length;
			} else {
				int itsLength = itsInterfaces.length;
				if (nextPosition + itsLength >= interfacesToVisit.length)
					System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[nextPosition + itsLength + 5], 0, nextPosition);
				nextInterface : for (int a = 0; a < itsLength; a++) {
					ReferenceBinding next = itsInterfaces[a];
					for (int b = 0; b < nextPosition; b++)
						if (TypeBinding.equalsEquals(next, interfacesToVisit[b])) continue nextInterface;
					interfacesToVisit[nextPosition++] = next;
				}
			}
		}
	} while ((currentType = currentType.superclass()) != null);

	for (int i = 0; i < nextPosition; i++) {
		currentType = interfacesToVisit[i];
		if (TypeBinding.equalsEquals(currentType, otherType)) return false;
		match = otherType.findSuperTypeOriginatingFrom(currentType);
		if (match != null && match.isProvablyDistinct(currentType))
			return true;

		ReferenceBinding[] itsInterfaces = currentType.superInterfaces();
		if (itsInterfaces != null && itsInterfaces != Binding.NO_SUPERINTERFACES) {
			int itsLength = itsInterfaces.length;
			if (nextPosition + itsLength >= interfacesToVisit.length)
				System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[nextPosition + itsLength + 5], 0, nextPosition);
			nextInterface : for (int a = 0; a < itsLength; a++) {
				ReferenceBinding next = itsInterfaces[a];
				for (int b = 0; b < nextPosition; b++)
					if (TypeBinding.equalsEquals(next, interfacesToVisit[b])) continue nextInterface;
				interfacesToVisit[nextPosition++] = next;
			}
		}
	}
	return false;
}

public boolean hasMemberTypes() {
    return false;
}

/**
 * Answer whether a @NonNullByDefault is applicable at the reference binding,
 * for 1.8 check if the default is applicable to the given kind of location.
 */
// pre: null annotation analysis is enabled
boolean hasNonNullDefaultFor(int location, int sourceStart) {
	// Note, STB overrides for correctly handling local types
	ReferenceBinding currentType = this;
	while (currentType != null) {
		int nullDefault = ((ReferenceBinding)currentType.original()).getNullDefault();
		if (nullDefault != 0)
			return (nullDefault & location) != 0;
		currentType = currentType.enclosingType();
	}
	// package
	return (this.getPackage().getDefaultNullness() & location) != 0;
}

int getNullDefault() {
	return 0;
}

@Override
public boolean acceptsNonNullDefault() {
	return true;
}

public final boolean hasRestrictedAccess() {
	return (this.modifiers & ExtraCompilerModifiers.AccRestrictedAccess) != 0;
}

/** Query typeBits without triggering supertype lookup. */
public boolean hasNullBit(int mask) {
	return (this.typeBits & mask) != 0;
}

/** Answer true if the receiver implements anInterface or is identical to anInterface.
* If searchHierarchy is true, then also search the receiver's superclasses.
*
* NOTE: Assume that anInterface is an interface.
*/
public boolean implementsInterface(ReferenceBinding anInterface, boolean searchHierarchy) {
	if (TypeBinding.equalsEquals(this, anInterface))
		return true;

	ReferenceBinding[] interfacesToVisit = null;
	int nextPosition = 0;
	ReferenceBinding currentType = this;
	do {
		ReferenceBinding[] itsInterfaces = currentType.superInterfaces();
		if (itsInterfaces != null && itsInterfaces != Binding.NO_SUPERINTERFACES) { // in code assist cases when source types are added late, may not be finished connecting hierarchy
			if (interfacesToVisit == null) {
				interfacesToVisit = itsInterfaces;
				nextPosition = interfacesToVisit.length;
			} else {
				int itsLength = itsInterfaces.length;
				if (nextPosition + itsLength >= interfacesToVisit.length)
					System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[nextPosition + itsLength + 5], 0, nextPosition);
				nextInterface : for (int a = 0; a < itsLength; a++) {
					ReferenceBinding next = itsInterfaces[a];
					for (int b = 0; b < nextPosition; b++)
						if (TypeBinding.equalsEquals(next, interfacesToVisit[b])) continue nextInterface;
					interfacesToVisit[nextPosition++] = next;
				}
			}
		}
	} while (searchHierarchy && (currentType = currentType.superclass()) != null);

	for (int i = 0; i < nextPosition; i++) {
		currentType = interfacesToVisit[i];
		if (currentType.isEquivalentTo(anInterface))
			return true;

		ReferenceBinding[] itsInterfaces = currentType.superInterfaces();
		if (itsInterfaces != null && itsInterfaces != Binding.NO_SUPERINTERFACES) { // in code assist cases when source types are added late, may not be finished connecting hierarchy
			int itsLength = itsInterfaces.length;
			if (nextPosition + itsLength >= interfacesToVisit.length)
				System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[nextPosition + itsLength + 5], 0, nextPosition);
			nextInterface : for (int a = 0; a < itsLength; a++) {
				ReferenceBinding next = itsInterfaces[a];
				for (int b = 0; b < nextPosition; b++)
					if (TypeBinding.equalsEquals(next, interfacesToVisit[b])) continue nextInterface;
				interfacesToVisit[nextPosition++] = next;
			}
		}
	}
	return false;
}

// Internal method... assume its only sent to classes NOT interfaces
boolean implementsMethod(MethodBinding method) {
	char[] selector = method.selector;
	ReferenceBinding type = this;
	while (type != null) {
		MethodBinding[] methods = type.methods();
		long range;
		if ((range = ReferenceBinding.binarySearch(selector, methods)) >= 0) {
			int start = (int) range, end = (int) (range >> 32);
			for (int i = start; i <= end; i++) {
				if (methods[i].areParametersEqual(method))
					return true;
			}
		}
		type = type.superclass();
	}
	return false;
}

/**
 * Answer true if the receiver is an abstract type
*/
public final boolean isAbstract() {
	return (this.modifiers & ClassFileConstants.AccAbstract) != 0;
}

@Override
public boolean isAnnotationType() {
	return (this.modifiers & ClassFileConstants.AccAnnotation) != 0;
}

public final boolean isBinaryBinding() {
	return (this.tagBits & TagBits.IsBinaryBinding) != 0;
}

@Override
public boolean isClass() {
	return (this.modifiers & (ClassFileConstants.AccInterface | ClassFileConstants.AccAnnotation | ClassFileConstants.AccEnum)) == 0;
}

private static SourceTypeBinding getSourceTypeBinding(ReferenceBinding ref) {
	if (ref instanceof SourceTypeBinding)
		return (SourceTypeBinding) ref;
	if (ref instanceof ParameterizedTypeBinding) {
		ParameterizedTypeBinding ptb = (ParameterizedTypeBinding) ref;
		return ptb.type instanceof SourceTypeBinding ? (SourceTypeBinding) ptb.type : null;
	}
	return null;
}
public  boolean isNestmateOf(ReferenceBinding other) {
	SourceTypeBinding s1 = getSourceTypeBinding(this);
	SourceTypeBinding s2 = getSourceTypeBinding(other);
	if (s1 == null || s2 == null) return false;

	return s1.isNestmateOf(s2);
}

@Override
public boolean isProperType(boolean admitCapture18) {
	ReferenceBinding outer = enclosingType();
	if (outer != null && !outer.isProperType(admitCapture18))
		return false;
	return super.isProperType(admitCapture18);
}

/**
 * Answer true if the receiver type can be assigned to the argument type (right)
 * In addition to improving performance, caching also ensures there is no infinite regression
 * since per nature, the compatibility check is recursive through parameterized type arguments (122775)
 */
@Override
public boolean isCompatibleWith(TypeBinding otherType, /*@Nullable*/ Scope captureScope) {
	if (equalsEquals(otherType, this))
		return true;

	if (otherType.id == TypeIds.T_JavaLangObject)
		return true;
	Object result;
	if (this.compatibleCache == null) {
		this.compatibleCache = new SimpleLookupTable(3);
		result = null;
	} else {
		result = this.compatibleCache.get(otherType); // [dbg reset] this.compatibleCache.put(otherType,null)
		if (result != null) {
			return result == Boolean.TRUE;
		}
	}
	this.compatibleCache.put(otherType, Boolean.FALSE); // protect from recursive call
	if (isCompatibleWith0(otherType, captureScope)) {
		this.compatibleCache.put(otherType, Boolean.TRUE);
		return true;
	}
	if (captureScope == null
			&& this instanceof TypeVariableBinding
			&& ((TypeVariableBinding)this).firstBound instanceof ParameterizedTypeBinding) {
		// see https://bugs.eclipse.org/395002#c9
		// in this case a subsequent check with captureScope != null may actually get
		// a better result, reset this info to ensure we're not blocking that re-check.
		this.compatibleCache.put(otherType, null);
	}
	return false;
}

/**
 * Answer true if the receiver type can be assigned to the argument type (right)
 */
private boolean isCompatibleWith0(TypeBinding otherType, /*@Nullable*/ Scope captureScope) {
	if (TypeBinding.equalsEquals(otherType, this))
		return true;
	if (otherType.id == TypeIds.T_JavaLangObject)
		return true;
	// equivalence may allow compatibility with array type through wildcard
	// bound
	if (isEquivalentTo(otherType))
		return true;
	switch (otherType.kind()) {
		case Binding.WILDCARD_TYPE :
		case Binding.INTERSECTION_TYPE:
			return false; // should have passed equivalence check above if
							// wildcard
		case Binding.TYPE_PARAMETER :
			// check compatibility with capture of ? super X
			if (otherType.isCapture()) {
				CaptureBinding otherCapture = (CaptureBinding) otherType;
				TypeBinding otherLowerBound;
				if ((otherLowerBound = otherCapture.lowerBound) != null) {
					if (otherLowerBound.isArrayType()) return false;
					return isCompatibleWith(otherLowerBound);
				}
			}
			if (otherType instanceof InferenceVariable) {
				// may interpret InferenceVariable as a joker, but only when within an outer lambda inference:
				if (captureScope != null) {
					MethodScope methodScope = captureScope.methodScope();
					if (methodScope != null) {
						ReferenceContext referenceContext = methodScope.referenceContext;
						if (referenceContext instanceof LambdaExpression
								&& ((LambdaExpression)referenceContext).inferenceContext != null)
							return true;
					}
				}
			}
			//$FALL-THROUGH$
		case Binding.GENERIC_TYPE :
		case Binding.TYPE :
		case Binding.PARAMETERIZED_TYPE :
		case Binding.RAW_TYPE :
		case Binding.INTERSECTION_TYPE18 :
			switch (kind()) {
				case Binding.GENERIC_TYPE :
				case Binding.PARAMETERIZED_TYPE :
				case Binding.RAW_TYPE :
					if (TypeBinding.equalsEquals(erasure(), otherType.erasure()))
						return false; // should have passed equivalence check
										// above if same erasure
			}
			ReferenceBinding otherReferenceType = (ReferenceBinding) otherType;
			if (otherReferenceType.isIntersectionType18()) {
				ReferenceBinding[] intersectingTypes = ((IntersectionTypeBinding18)otherReferenceType).intersectingTypes;
				for (ReferenceBinding binding : intersectingTypes) {
					if (!isCompatibleWith(binding))
						return false;
				}
				return true;
			}
			if (otherReferenceType.isInterface()) { // could be annotation type
				if (implementsInterface(otherReferenceType, true))
					return true;
				if (this instanceof TypeVariableBinding && captureScope != null) {
					TypeVariableBinding typeVariable = (TypeVariableBinding) this;
					if (typeVariable.firstBound instanceof ParameterizedTypeBinding) {
						TypeBinding bound = typeVariable.firstBound.capture(captureScope, -1, -1); // no position needed as this capture will never escape this context
						return bound.isCompatibleWith(otherReferenceType);
					}
				}
			}
			if (isInterface())  // Explicit conversion from an interface
										// to a class is not allowed
				return false;
			return otherReferenceType.isSuperclassOf(this);
		default :
			return false;
	}
}
/**
 * Answer true if the receiver has non-sealed modifier
 */
public final boolean isNonSealed() {
	return (this.modifiers & ExtraCompilerModifiers.AccNonSealed) != 0;
}

/**
 * Answer true if the receiver has sealed modifier
 */
public boolean isSealed() {
	return (this.modifiers & ExtraCompilerModifiers.AccSealed) != 0;
}

@Override
public boolean isSubtypeOf(TypeBinding other, boolean simulatingBugJDK8026527) {
	if (isSubTypeOfRTL(other))
		return true;
	// TODO: if this has wildcards, perform capture before the next call:
	TypeBinding candidate = findSuperTypeOriginatingFrom(other);
	if (candidate == null)
		return false;
	if (TypeBinding.equalsEquals(candidate, other))
		return true;

	// T<Ai...> <: T#RAW:
	if (other.isRawType() && TypeBinding.equalsEquals(candidate.erasure(), other.erasure()))
		return true;

	TypeBinding[] sis = other.typeArguments();
	TypeBinding[] tis = candidate.typeArguments();
	if (tis == null || sis == null)
		return false;
	if (sis.length != tis.length)
		return false;
	for (int i = 0; i < sis.length; i++) {
		if (!tis[i].isTypeArgumentContainedBy(sis[i]))
			return false;
	}
	return true;
}

protected boolean isSubTypeOfRTL(TypeBinding other) {
	if (TypeBinding.equalsEquals(this, other))
		return true;
	if (other instanceof CaptureBinding) {
		// for this one kind we must first unwrap the rhs:
		TypeBinding lower = ((CaptureBinding) other).lowerBound;
		return (lower != null && isSubtypeOf(lower, false));
	}
	if (other instanceof ReferenceBinding) {
		TypeBinding[] intersecting = ((ReferenceBinding) other).getIntersectingTypes();
		if (intersecting != null) {
			for (int i = 0; i < intersecting.length; i++) {
				if (!isSubtypeOf(intersecting[i], false))
					return false;
			}
			return true;
		}
	}
	return false;
}

/**
 * Answer true if the receiver has default visibility
 */
public final boolean isDefault() {
	return (this.modifiers & (ClassFileConstants.AccPublic | ClassFileConstants.AccProtected | ClassFileConstants.AccPrivate)) == 0;
}

/**
 * Answer true if the receiver is a deprecated type
 */
public final boolean isDeprecated() {
	return (this.modifiers & ClassFileConstants.AccDeprecated) != 0;
}

@Override
public boolean isEnum() {
	return (this.modifiers & ClassFileConstants.AccEnum) != 0;
}

/**
 * Answer true if the receiver is final and cannot be subclassed
 */
public final boolean isFinal() {
	return (this.modifiers & ClassFileConstants.AccFinal) != 0;
}

/**
 * Returns true if the type hierarchy is being connected
 */
public boolean isHierarchyBeingConnected() {
	return (this.tagBits & TagBits.EndHierarchyCheck) == 0 && (this.tagBits & TagBits.BeginHierarchyCheck) != 0;
}
/**
 * Returns true if the type hierarchy is being connected "actively" i.e not paused momentatrily,
 * while resolving type arguments. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=294057
 */
public boolean isHierarchyBeingActivelyConnected() {
	return (this.tagBits & TagBits.EndHierarchyCheck) == 0 && (this.tagBits & TagBits.BeginHierarchyCheck) != 0 && (this.tagBits & TagBits.PauseHierarchyCheck) == 0;
}

/**
 * Returns true if the type hierarchy is connected
 */
public boolean isHierarchyConnected() {
	return true;
}

@Override
public boolean isInterface() {
	// consider strict interfaces and annotation types
	return (this.modifiers & ClassFileConstants.AccInterface) != 0;
}

@Override
public boolean isFunctionalInterface(Scope scope) {
	MethodBinding method;
	return isInterface() && (method = getSingleAbstractMethod(scope, true)) != null && method.isValidBinding();
}

/**
 * Answer true if the receiver has private visibility
 */
public final boolean isPrivate() {
	return (this.modifiers & ClassFileConstants.AccPrivate) != 0;
}

/**
 * Answer true if the receiver or any of its enclosing types have private visibility
 */
public final boolean isOrEnclosedByPrivateType() {
	if (isLocalType()) return true; // catch all local types
	ReferenceBinding type = this;
	while (type != null) {
		if ((type.modifiers & ClassFileConstants.AccPrivate) != 0)
			return true;
		type = type.enclosingType();
	}
	return false;
}

/**
 * Answer true if the receiver has protected visibility
 */
public final boolean isProtected() {
	return (this.modifiers & ClassFileConstants.AccProtected) != 0;
}

/**
 * Answer true if the receiver has public visibility
 */
public final boolean isPublic() {
	return (this.modifiers & ClassFileConstants.AccPublic) != 0;
}

/**
 * Answer true if the receiver is a static member type (or toplevel)
 */
@Override
public final boolean isStatic() {
	return (this.modifiers & (ClassFileConstants.AccStatic | ClassFileConstants.AccInterface)) != 0 || (this.tagBits & TagBits.IsNestedType) == 0;
}

/**
 * Answer true if all float operations must adher to IEEE 754 float/double rules
 */
public final boolean isStrictfp() {
	return (this.modifiers & ClassFileConstants.AccStrictfp) != 0;
}

/**
 * Answer true if the receiver is in the superclass hierarchy of aType
 * NOTE: Object.isSuperclassOf(Object) -> false
 */
public boolean isSuperclassOf(ReferenceBinding otherType) {
	while ((otherType = otherType.superclass()) != null) {
		if (otherType.isEquivalentTo(this)) return true;
	}
	return false;
}

/**
 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#isThrowable()
 */
@Override
public boolean isThrowable() {
	ReferenceBinding current = this;
	do {
		switch (current.id) {
			case TypeIds.T_JavaLangThrowable :
			case TypeIds.T_JavaLangError :
			case TypeIds.T_JavaLangRuntimeException :
			case TypeIds.T_JavaLangException :
				return true;
		}
	} while ((current = current.superclass()) != null);
	return false;
}

/**
 * JLS 11.5 ensures that Throwable, Exception, RuntimeException and Error are directly connected.
 * (Throwable<- Exception <- RumtimeException, Throwable <- Error). Thus no need to check #isCompatibleWith
 * but rather check in type IDs so as to avoid some eager class loading for JCL writers.
 * When 'includeSupertype' is true, answers true if the given type can be a supertype of some unchecked exception
 * type (i.e. Throwable or Exception).
 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#isUncheckedException(boolean)
 */
@Override
public boolean isUncheckedException(boolean includeSupertype) {
	switch (this.id) {
			case TypeIds.T_JavaLangError :
			case TypeIds.T_JavaLangRuntimeException :
				return true;
			case TypeIds.T_JavaLangThrowable :
			case TypeIds.T_JavaLangException :
				return includeSupertype;
	}
	ReferenceBinding current = this;
	while ((current = current.superclass()) != null) {
		switch (current.id) {
			case TypeIds.T_JavaLangError :
			case TypeIds.T_JavaLangRuntimeException :
				return true;
			case TypeIds.T_JavaLangThrowable :
			case TypeIds.T_JavaLangException :
				return false;
		}
	}
	return false;
}

/**
 * Answer true if the receiver has private visibility and is used locally
 */
public final boolean isUsed() {
	return (this.modifiers & ExtraCompilerModifiers.AccLocallyUsed) != 0;
}

/**
 * Answer true if the receiver is deprecated (or any of its enclosing types)
 */
public final boolean isViewedAsDeprecated() {
	if ((this.modifiers & (ClassFileConstants.AccDeprecated | ExtraCompilerModifiers.AccDeprecatedImplicitly)) != 0)
		return true;
	if (getPackage().isViewedAsDeprecated()) {
		this.tagBits |= (getPackage().tagBits & TagBits.AnnotationTerminallyDeprecated);
		return true;
	}
	return false;
}

/**
 * Returns the member types of this type sorted by simple name.
 */
public ReferenceBinding[] memberTypes() {
	return Binding.NO_MEMBER_TYPES;
}

public MethodBinding[] methods() {
	return Binding.NO_METHODS;
}

public final ReferenceBinding outermostEnclosingType() {
	ReferenceBinding current = this;
	while (true) {
		ReferenceBinding last = current;
		if ((current = current.enclosingType()) == null)
			return last;
	}
}

/**
 * Answer the source name for the type.
 * In the case of member types, as the qualified name from its top level type.
 * For example, for a member type N defined inside M & A: "A.M.N".
 */
@Override
public char[] qualifiedSourceName() {
	if (isMemberType())
		return CharOperation.concat(enclosingType().qualifiedSourceName(), sourceName(), '.');
	return sourceName();
}

/**
 * Answer the receiver's signature.
 *
 * NOTE: This method should only be used during/after code gen.
 */
@Override
public char[] readableName() /*java.lang.Object,  p.X<T> */ {
	return readableName(true);
}
public char[] readableName(boolean showGenerics) /*java.lang.Object,  p.X<T> */ {
    char[] readableName;
	if (isMemberType()) {
		readableName = CharOperation.concat(enclosingType().readableName(showGenerics && hasEnclosingInstanceContext()), this.sourceName, '.');
	} else {
		readableName = CharOperation.concatWith(this.compoundName, '.');
	}
	if (showGenerics) {
		TypeVariableBinding[] typeVars;
		if ((typeVars = typeVariables()) != Binding.NO_TYPE_VARIABLES) {
		    StringBuilder nameBuffer = new StringBuilder(10);
		    nameBuffer.append(readableName).append('<');
		    for (int i = 0, length = typeVars.length; i < length; i++) {
		        if (i > 0) nameBuffer.append(',');
		        nameBuffer.append(typeVars[i].readableName());
		    }
		    nameBuffer.append('>');
			int nameLength = nameBuffer.length();
			readableName = new char[nameLength];
			nameBuffer.getChars(0, nameLength, readableName, 0);
		}
	}
	return readableName;
}

protected void appendNullAnnotation(StringBuffer nameBuffer, CompilerOptions options) {
	if (options.isAnnotationBasedNullAnalysisEnabled) {
		if (options.usesNullTypeAnnotations()) {
			for (AnnotationBinding annotation : this.typeAnnotations) {
				ReferenceBinding annotationType = annotation.getAnnotationType();
				if (annotationType.hasNullBit(TypeIds.BitNonNullAnnotation|TypeIds.BitNullableAnnotation)) {
					nameBuffer.append('@').append(annotationType.shortReadableName()).append(' ');
				}
			}
		} else {
			// restore applied null annotation from tagBits:
		    if ((this.tagBits & TagBits.AnnotationNonNull) != 0) {
		    	char[][] nonNullAnnotationName = options.nonNullAnnotationName;
				nameBuffer.append('@').append(nonNullAnnotationName[nonNullAnnotationName.length-1]).append(' ');
		    }
		    if ((this.tagBits & TagBits.AnnotationNullable) != 0) {
		    	char[][] nullableAnnotationName = options.nullableAnnotationName;
				nameBuffer.append('@').append(nullableAnnotationName[nullableAnnotationName.length-1]).append(' ');
		    }
		}
	}
}

public AnnotationHolder retrieveAnnotationHolder(Binding binding, boolean forceInitialization) {
	SimpleLookupTable store = storedAnnotations(forceInitialization, false);
	return store == null ? null : (AnnotationHolder) store.get(binding);
}

AnnotationBinding[] retrieveAnnotations(Binding binding) {
	AnnotationHolder holder = retrieveAnnotationHolder(binding, true);
	return holder == null ? Binding.NO_ANNOTATIONS : holder.getAnnotations();
}

@Override
public void setAnnotations(AnnotationBinding[] annotations, boolean forceStore) {
	storeAnnotations(this, annotations, forceStore);
}
public void setContainerAnnotationType(ReferenceBinding value) {
	// Leave this to subclasses
}
public void tagAsHavingDefectiveContainerType() {
	// Leave this to subclasses
}

/**
 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#nullAnnotatedReadableName(CompilerOptions,boolean)
 */
@Override
public char[] nullAnnotatedReadableName(CompilerOptions options, boolean shortNames) {
	if (shortNames)
		return nullAnnotatedShortReadableName(options);
	return nullAnnotatedReadableName(options);
}

char[] nullAnnotatedReadableName(CompilerOptions options) {
    StringBuffer nameBuffer = new StringBuffer(10);
	if (isMemberType()) {
		nameBuffer.append(enclosingType().nullAnnotatedReadableName(options, false));
		nameBuffer.append('.');
		appendNullAnnotation(nameBuffer, options);
		nameBuffer.append(this.sourceName);
	} else if (this.compoundName != null) {
		int i;
		int l=this.compoundName.length;
		for (i=0; i<l-1; i++) {
			nameBuffer.append(this.compoundName[i]);
			nameBuffer.append('.');
		}
	    appendNullAnnotation(nameBuffer, options);
		nameBuffer.append(this.compoundName[i]);
	} else {
		// case of TypeVariableBinding with nullAnnotationTagBits:
		appendNullAnnotation(nameBuffer, options);
		if (this.sourceName != null)
			nameBuffer.append(this.sourceName);
		else // WildcardBinding, CaptureBinding have no sourceName
			nameBuffer.append(this.readableName());
	}
	TypeBinding [] arguments = typeArguments();
	if (arguments != null && arguments.length > 0) { // empty arguments array happens when PTB has been created just to capture type annotations
		nameBuffer.append('<');
	    for (int i = 0, length = arguments.length; i < length; i++) {
	        if (i > 0) nameBuffer.append(',');
	        nameBuffer.append(arguments[i].nullAnnotatedReadableName(options, false));
	    }
	    nameBuffer.append('>');
	}
	int nameLength = nameBuffer.length();
	char[] readableName = new char[nameLength];
	nameBuffer.getChars(0, nameLength, readableName, 0);
    return readableName;
}

char[] nullAnnotatedShortReadableName(CompilerOptions options) {
    StringBuffer nameBuffer = new StringBuffer(10);
	if (isMemberType()) {
		nameBuffer.append(enclosingType().nullAnnotatedReadableName(options, true));
		nameBuffer.append('.');
		appendNullAnnotation(nameBuffer, options);
		nameBuffer.append(this.sourceName);
	} else {
		appendNullAnnotation(nameBuffer, options);
		if (this.sourceName != null)
			nameBuffer.append(this.sourceName);
		else // WildcardBinding, CaptureBinding have no sourceName
			nameBuffer.append(this.shortReadableName());
	}
	TypeBinding [] arguments = typeArguments();
	if (arguments != null && arguments.length > 0) { // empty arguments array happens when PTB has been created just to capture type annotations
		nameBuffer.append('<');
	    for (int i = 0, length = arguments.length; i < length; i++) {
	        if (i > 0) nameBuffer.append(',');
	        nameBuffer.append(arguments[i].nullAnnotatedReadableName(options, true));
	    }
	    nameBuffer.append('>');
	}
	int nameLength = nameBuffer.length();
	char[] shortReadableName = new char[nameLength];
	nameBuffer.getChars(0, nameLength, shortReadableName, 0);
    return shortReadableName;
}

@Override
public char[] shortReadableName() /*Object*/ {
	return shortReadableName(true);
}
public char[] shortReadableName(boolean showGenerics) /*Object*/ {
	char[] shortReadableName;
	if (isMemberType()) {
		shortReadableName = CharOperation.concat(enclosingType().shortReadableName(showGenerics && hasEnclosingInstanceContext()), this.sourceName, '.');
	} else {
		shortReadableName = this.sourceName;
	}
	if (showGenerics) {
		TypeVariableBinding[] typeVars;
		if ((typeVars = typeVariables()) != Binding.NO_TYPE_VARIABLES) {
		    StringBuilder nameBuffer = new StringBuilder(10);
		    nameBuffer.append(shortReadableName).append('<');
		    for (int i = 0, length = typeVars.length; i < length; i++) {
		        if (i > 0) nameBuffer.append(',');
		        nameBuffer.append(typeVars[i].shortReadableName());
		    }
		    nameBuffer.append('>');
			int nameLength = nameBuffer.length();
			shortReadableName = new char[nameLength];
			nameBuffer.getChars(0, nameLength, shortReadableName, 0);
		}
	}
	return shortReadableName;
}

@Override
public char[] signature() /* Ljava/lang/Object; */ {
	if (this.signature != null)
		return this.signature;

	return this.signature = CharOperation.concat('L', constantPoolName(), ';');
}

@Override
public char[] sourceName() {
	return this.sourceName;
}

/**
 * Perform an upwards type projection as per JLS 4.10.5
 * @param scope Relevant scope for evaluating type projection
 * @param mentionedTypeVariables Filter for mentioned type variabled
 * @returns Upwards type projection of 'this', or null if downwards projection is undefined
*/
@Override
public ReferenceBinding upwardsProjection(Scope scope, TypeBinding[] mentionedTypeVariables) {
	return this;
}

/**
 * Perform a downwards type projection as per JLS 4.10.5
 * @param scope Relevant scope for evaluating type projection
 * @param mentionedTypeVariables Filter for mentioned type variabled
 * @returns Downwards type projection of 'this', or null if downwards projection is undefined
*/
@Override
public ReferenceBinding downwardsProjection(Scope scope, TypeBinding[] mentionedTypeVariables) {
	return this;
}

void storeAnnotationHolder(Binding binding, AnnotationHolder holder) {
	if (holder == null) {
		SimpleLookupTable store = storedAnnotations(false, false);
		if (store != null)
			store.removeKey(binding);
	} else {
		SimpleLookupTable store = storedAnnotations(true, false);
		if (store != null)
			store.put(binding, holder);
	}
}

void storeAnnotations(Binding binding, AnnotationBinding[] annotations, boolean forceStore) {
	AnnotationHolder holder = null;
	if (annotations == null || annotations.length == 0) {
		SimpleLookupTable store = storedAnnotations(false, forceStore);
		if (store != null)
			holder = (AnnotationHolder) store.get(binding);
		if (holder == null) return; // nothing to delete
	} else {
		SimpleLookupTable store = storedAnnotations(true, forceStore);
		if (store == null) return; // not supported
		holder = (AnnotationHolder) store.get(binding);
		if (holder == null)
			holder = new AnnotationHolder();
	}
	storeAnnotationHolder(binding, holder.setAnnotations(annotations));
}

SimpleLookupTable storedAnnotations(boolean forceInitialize, boolean forceStore) {
	return null; // overrride if interested in storing annotations for the receiver, its fields and methods
}

@Override
public ReferenceBinding superclass() {
	return null;
}

@Override
public ReferenceBinding[] permittedTypes() {
	return Binding.NO_PERMITTEDTYPES;
}

@Override
public ReferenceBinding[] superInterfaces() {
	return Binding.NO_SUPERINTERFACES;
}

public ReferenceBinding[] syntheticEnclosingInstanceTypes() {
	if (isStatic()) return null;
	ReferenceBinding enclosingType = enclosingType();
	if (enclosingType == null)
		return null;
	return new ReferenceBinding[] {enclosingType};
}

MethodBinding[] unResolvedMethods() { // for the MethodVerifier so it doesn't resolve types
	return methods();
}

public FieldBinding[] unResolvedFields() {
	return Binding.NO_FIELDS;
}

/*
 * If a type - known to be a Closeable - is mentioned in one of our white lists
 * answer the typeBit for the white list (BitWrapperCloseable or BitResourceFreeCloseable).
 */
protected int applyCloseableClassWhitelists(CompilerOptions options) {
	switch (this.compoundName.length) {
		case 3:
			if (CharOperation.equals(TypeConstants.JAVA, this.compoundName[0])) {
				if (CharOperation.equals(TypeConstants.IO, this.compoundName[1])) {
					char[] simpleName = this.compoundName[2];
					int l = TypeConstants.JAVA_IO_WRAPPER_CLOSEABLES.length;
					for (int i = 0; i < l; i++) {
						if (CharOperation.equals(simpleName, TypeConstants.JAVA_IO_WRAPPER_CLOSEABLES[i]))
							return TypeIds.BitWrapperCloseable;
					}
					l = TypeConstants.JAVA_IO_RESOURCE_FREE_CLOSEABLES.length;
					for (int i = 0; i < l; i++) {
						if (CharOperation.equals(simpleName, TypeConstants.JAVA_IO_RESOURCE_FREE_CLOSEABLES[i]))
							return TypeIds.BitResourceFreeCloseable;
					}
				}
			}
			break;
		case 4:
			if (CharOperation.equals(TypeConstants.JAVA, this.compoundName[0])) {
				if (CharOperation.equals(TypeConstants.UTIL, this.compoundName[1])) {
					if (CharOperation.equals(TypeConstants.ZIP, this.compoundName[2])) {
						char[] simpleName = this.compoundName[3];
						int l = TypeConstants.JAVA_UTIL_ZIP_WRAPPER_CLOSEABLES.length;
						for (int i = 0; i < l; i++) {
							if (CharOperation.equals(simpleName, TypeConstants.JAVA_UTIL_ZIP_WRAPPER_CLOSEABLES[i]))
								return TypeIds.BitWrapperCloseable;
						}
					}
				}
			}
			break;
	}
	int l = TypeConstants.OTHER_WRAPPER_CLOSEABLES.length;
	for (int i = 0; i < l; i++) {
		if (CharOperation.equals(this.compoundName, TypeConstants.OTHER_WRAPPER_CLOSEABLES[i]))
			return TypeIds.BitWrapperCloseable;
	}
	if (options.analyseResourceLeaks) {
		ReferenceBinding mySuper = this.superclass();
		if (mySuper != null && mySuper.id != TypeIds.T_JavaLangObject) {
			if (hasMethodWithNumArgs(TypeConstants.CLOSE, 0))
				return 0; // close methods indicates: class may need more closing than super
			return mySuper.applyCloseableClassWhitelists(options);
		}
	}
	return 0;
}

protected boolean hasMethodWithNumArgs(char[] selector, int numArgs) {
	for (MethodBinding methodBinding : unResolvedMethods()) {
		if (CharOperation.equals(methodBinding.selector, selector)
				&& methodBinding.parameters.length == numArgs) {
			return true;
		}
	}
	return false;
}

/*
 * If a type - known to be a Closeable - is mentioned in one of our white lists
 * answer the typeBit for the white list (BitWrapperCloseable or BitResourceFreeCloseable).
 */
protected int applyCloseableInterfaceWhitelists() {
	switch (this.compoundName.length) {
		case 4:
			if (CharOperation.equals(this.compoundName[0], TypeConstants.JAVA_UTIL_STREAM[0])) {
				for (int i=1; i<3; i++) {
					if (!CharOperation.equals(this.compoundName[i], TypeConstants.JAVA_UTIL_STREAM[i])) {
						return 0;
					}
				}
				for (char[] streamName : TypeConstants.RESOURCE_FREE_CLOSEABLE_J_U_STREAMS) {
					if (CharOperation.equals(this.compoundName[3], streamName)) {
						return TypeIds.BitResourceFreeCloseable;
					}
				}
			} else {
				for (int i=0; i<3; i++) {
					if (!CharOperation.equals(this.compoundName[i], TypeConstants.ONE_UTIL_STREAMEX[i])) {
						return 0;
					}
				}
				for (char[] streamName : TypeConstants.RESOURCE_FREE_CLOSEABLE_STREAMEX) {
					if (CharOperation.equals(this.compoundName[3], streamName)) {
						return TypeIds.BitResourceFreeCloseable;
					}
				}
			}
			break;
	}
	return 0;
}

protected MethodBinding [] getInterfaceAbstractContracts(Scope scope, boolean replaceWildcards, boolean filterDefaultMethods) throws InvalidInputException {

	if (!isInterface() || !isValidBinding()) {
		throw new InvalidInputException("Not a functional interface"); //$NON-NLS-1$
	}

	MethodBinding [] methods = methods();
	MethodBinding [] contracts = new MethodBinding[0];
	int contractsCount = 0;
	int contractsLength = 0;

	ReferenceBinding [] superInterfaces = superInterfaces();
	for (int i = 0, length = superInterfaces.length; i < length; i++) {
		// filterDefaultMethods=false => keep default methods needed to filter out any abstract methods they may override:
		MethodBinding [] superInterfaceContracts = superInterfaces[i].getInterfaceAbstractContracts(scope, replaceWildcards, false);
		final int superInterfaceContractsLength = superInterfaceContracts == null  ? 0 : superInterfaceContracts.length;
		if (superInterfaceContractsLength == 0) continue;
		if (contractsLength < contractsCount + superInterfaceContractsLength) {
			System.arraycopy(contracts, 0, contracts = new MethodBinding[contractsLength = contractsCount + superInterfaceContractsLength], 0, contractsCount);
		}
		System.arraycopy(superInterfaceContracts, 0, contracts, contractsCount,	superInterfaceContractsLength);
		contractsCount += superInterfaceContractsLength;
	}

	LookupEnvironment environment = scope.environment();
	for (int i = 0, length = methods == null ? 0 : methods.length; i < length; i++) {
		final MethodBinding method = methods[i];
		if (method == null || method.isStatic() || method.redeclaresPublicObjectMethod(scope) || method.isPrivate())
			continue;
		if (!method.isValidBinding())
			throw new InvalidInputException("Not a functional interface"); //$NON-NLS-1$
		for (int j = 0; j < contractsCount;) {
			if ( contracts[j] != null && MethodVerifier.doesMethodOverride(method, contracts[j], environment)) {
				contractsCount--;
				// abstract method from super type overridden by present interface ==> contracts[j] = null;
				if (j < contractsCount) {
					System.arraycopy(contracts, j+1, contracts, j, contractsCount - j);
					continue;
				}
			}
			j++;
		}
		if (filterDefaultMethods && method.isDefaultMethod())
			continue; // skip default method itself
		if (contractsCount == contractsLength) {
			System.arraycopy(contracts, 0, contracts = new MethodBinding[contractsLength += 16], 0, contractsCount);
		}
		if(environment.globalOptions.isAnnotationBasedNullAnalysisEnabled) {
			ImplicitNullAnnotationVerifier.ensureNullnessIsKnown(method, scope);
		}
		contracts[contractsCount++] = method;
	}
	// check mutual overriding of inherited methods (i.e., not from current type):
	for (int i = 0; i < contractsCount; i++) {
		MethodBinding contractI = contracts[i];
		if (TypeBinding.equalsEquals(contractI.declaringClass, this))
			continue;
		for (int j = 0; j < contractsCount; j++) {
			MethodBinding contractJ = contracts[j];
			if (i == j || TypeBinding.equalsEquals(contractJ.declaringClass, this))
				continue;
			if (contractI == contractJ || MethodVerifier.doesMethodOverride(contractI, contractJ, environment)) {
				contractsCount--;
				// abstract method from one super type overridden by other super interface ==> contracts[j] = null;
				if (j < contractsCount) {
					System.arraycopy(contracts, j+1, contracts, j, contractsCount - j);
				}
				j--;
				if (j < i)
					i--;
				continue;
			}
		}
		if (filterDefaultMethods && contractI.isDefaultMethod()) {
			contractsCount--;
			// remove default method after it has eliminated any matching abstract methods from contracts
			if (i < contractsCount) {
				System.arraycopy(contracts, i+1, contracts, i, contractsCount - i);
			}
			i--;
		}
	}
	if (contractsCount < contractsLength) {
		System.arraycopy(contracts, 0, contracts = new MethodBinding[contractsCount], 0, contractsCount);
	}
	return contracts;
}
@Override
public MethodBinding getSingleAbstractMethod(Scope scope, boolean replaceWildcards) {

	int index = replaceWildcards ? 0 : 1;
	if (this.singleAbstractMethod != null) {
		if (this.singleAbstractMethod[index] != null)
		return this.singleAbstractMethod[index];
	} else {
		this.singleAbstractMethod = new MethodBinding[2];
		// Sec 9.8 of sealed preview - A functional interface is an interface that is not declared sealed...
		if (JavaFeature.SEALED_CLASSES.isSupported(scope.compilerOptions())
				&& this.isSealed())
			return this.singleAbstractMethod[index] = samProblemBinding;
	}

	if (this.compoundName != null)
		scope.compilationUnitScope().recordQualifiedReference(this.compoundName);
	MethodBinding[] methods = null;
	try {
		methods = getInterfaceAbstractContracts(scope, replaceWildcards, true);
		if (methods == null || methods.length == 0)
			return this.singleAbstractMethod[index] = samProblemBinding;
		int contractParameterLength = 0;
		char [] contractSelector = null;
		for (int i = 0, length = methods.length; i < length; i++) {
			MethodBinding method = methods[i];
			if (method == null) continue;
			if (contractSelector == null) {
				contractSelector = method.selector;
				contractParameterLength = method.parameters == null ? 0 : method.parameters.length;
			} else {
				int methodParameterLength = method.parameters == null ? 0 : method.parameters.length;
				if (methodParameterLength != contractParameterLength || !CharOperation.equals(method.selector, contractSelector))
					return this.singleAbstractMethod[index] = samProblemBinding;
			}
		}
	} catch (InvalidInputException e) {
		return this.singleAbstractMethod[index] = samProblemBinding;
	}
	if (methods.length == 1)
		return this.singleAbstractMethod[index] = methods[0];

	final LookupEnvironment environment = scope.environment();
	boolean genericMethodSeen = false;
	int length = methods.length;
	boolean analyseNullAnnotations = environment.globalOptions.isAnnotationBasedNullAnalysisEnabled;

	next:for (int i = length - 1; i >= 0; --i) {
		MethodBinding method = methods[i], otherMethod = null;
		if (method.typeVariables != Binding.NO_TYPE_VARIABLES)
			genericMethodSeen = true;
		TypeBinding returnType = method.returnType;
		TypeBinding[] parameters = method.parameters;
		for (int j = 0; j < length; j++) {
			if (i == j) continue;
			otherMethod = methods[j];
			if (otherMethod.typeVariables != Binding.NO_TYPE_VARIABLES)
				genericMethodSeen = true;

			if (genericMethodSeen) { // adapt type parameters.
				otherMethod = MethodVerifier.computeSubstituteMethod(otherMethod, method, environment);
				if (otherMethod == null)
					continue next;
			}
			if (!MethodVerifier.isSubstituteParameterSubsignature(method, otherMethod, environment) || !MethodVerifier.areReturnTypesCompatible(method, otherMethod, environment))
				continue next;
			if (analyseNullAnnotations) {
				returnType = NullAnnotationMatching.strongerType(returnType, otherMethod.returnType, environment);
				parameters = NullAnnotationMatching.weakerTypes(parameters, otherMethod.parameters, environment);
			}
		}
		// If we reach here, we found a method that is override equivalent with every other method and is also return type substitutable. Compute kosher exceptions now ...
		ReferenceBinding [] exceptions = new ReferenceBinding[0];
		int exceptionsCount = 0, exceptionsLength = 0;
		final MethodBinding theAbstractMethod = method;
		boolean shouldEraseThrows = theAbstractMethod.typeVariables == Binding.NO_TYPE_VARIABLES && genericMethodSeen;
		boolean shouldAdaptThrows = theAbstractMethod.typeVariables != Binding.NO_TYPE_VARIABLES;
		final int typeVariableLength = theAbstractMethod.typeVariables.length;

		none:for (i = 0; i < length; i++) {
			method = methods[i];
			ReferenceBinding[] methodThrownExceptions = method.thrownExceptions;
			int methodExceptionsLength = methodThrownExceptions == null ? 0: methodThrownExceptions.length;
			if (methodExceptionsLength == 0) break none;
			if (shouldAdaptThrows && method != theAbstractMethod) {
				System.arraycopy(methodThrownExceptions, 0, methodThrownExceptions = new ReferenceBinding[methodExceptionsLength], 0, methodExceptionsLength);
				for (int tv = 0; tv < typeVariableLength; tv++) {
					if (methodThrownExceptions[tv] instanceof TypeVariableBinding) {
						methodThrownExceptions[tv] = theAbstractMethod.typeVariables[tv];
					}
				}
			}
			nextException: for (int j = 0; j < methodExceptionsLength; j++) {
				ReferenceBinding methodException = methodThrownExceptions[j];
				if (shouldEraseThrows)
					methodException = (ReferenceBinding) methodException.erasure();
				nextMethod: for (int k = 0; k < length; k++) {
					if (i == k) continue;
					otherMethod = methods[k];
					ReferenceBinding[] otherMethodThrownExceptions = otherMethod.thrownExceptions;
					int otherMethodExceptionsLength =  otherMethodThrownExceptions == null ? 0 : otherMethodThrownExceptions.length;
					if (otherMethodExceptionsLength == 0) break none;
					if (shouldAdaptThrows && otherMethod != theAbstractMethod) {
						System.arraycopy(otherMethodThrownExceptions,
								0,
								otherMethodThrownExceptions = new ReferenceBinding[otherMethodExceptionsLength],
								0,
								otherMethodExceptionsLength);
						for (int tv = 0; tv < typeVariableLength; tv++) {
							if (otherMethodThrownExceptions[tv] instanceof TypeVariableBinding) {
								otherMethodThrownExceptions[tv] = theAbstractMethod.typeVariables[tv];
							}
						}
					}
					for (int l = 0; l < otherMethodExceptionsLength; l++) {
						ReferenceBinding otherException = otherMethodThrownExceptions[l];
						if (shouldEraseThrows)
							otherException = (ReferenceBinding) otherException.erasure();
						if (methodException.isCompatibleWith(otherException))
							continue nextMethod;
					}
					continue nextException;
				}
				// If we reach here, method exception or its super type is covered by every throws clause.
				if (exceptionsCount == exceptionsLength) {
					System.arraycopy(exceptions, 0, exceptions = new ReferenceBinding[exceptionsLength += 16], 0, exceptionsCount);
				}
				exceptions[exceptionsCount++] = methodException;
			}
		}
		if (exceptionsCount != exceptionsLength) {
			System.arraycopy(exceptions, 0, exceptions = new ReferenceBinding[exceptionsCount], 0, exceptionsCount);
		}
		this.singleAbstractMethod[index] = new MethodBinding(theAbstractMethod.modifiers | ClassFileConstants.AccSynthetic,
				theAbstractMethod.selector,
				returnType,
				parameters,
				exceptions,
				theAbstractMethod.declaringClass);
	    this.singleAbstractMethod[index].typeVariables = theAbstractMethod.typeVariables;
		return this.singleAbstractMethod[index];
	}
	return this.singleAbstractMethod[index] = samProblemBinding;
}

// See JLS 4.9 bullet 1
public static boolean isConsistentIntersection(TypeBinding[] intersectingTypes) {
	TypeBinding[] ci = new TypeBinding[intersectingTypes.length];
	for (int i = 0; i < ci.length; i++) {
		TypeBinding current = intersectingTypes[i];
		ci[i] = (current.isClass() || current.isArrayType())
					? current : current.superclass();
	}
	TypeBinding mostSpecific = ci[0];
	for (int i = 1; i < ci.length; i++) {
		TypeBinding current = ci[i];
		// when invoked during type inference we only want to check inconsistency among real types:
		if (current.isTypeVariable() || current.isWildcard() || !current.isProperType(true))
			continue;
		if (mostSpecific.isSubtypeOf(current, false))
			continue;
		else if (current.isSubtypeOf(mostSpecific, false))
			mostSpecific = current;
		else
			return false;
	}
	return true;
}
public ModuleBinding module() {
	if (this.fPackage != null)
		return this.fPackage.enclosingModule;
	return null;
}

public boolean hasEnclosingInstanceContext() {
	if (isMemberType() && !isStatic())
		return true;
	if (isLocalType() && isStatic())
		return false;
	MethodBinding enclosingMethod = enclosingMethod();
	if (enclosingMethod != null)
		return !enclosingMethod.isStatic();
	return false;
}
}
