/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Jesper S Moller <jesper@selskabet.org> - Contributions for
 *								bug 378674 - "The method can be declared as static" is wrong
 *     Stephan Herrmann - Contribution for
 *								Bug 424167 - [1.8] Fully integrate type inference with overload resolution
 *******************************************************************************/
package org.eclipse.jdt.internal.eval;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.InvocationSite;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemFieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.lookup.VariableBinding;

/**
 * This scope is used for code snippet lookup to emulate private, protected and default access.
 * These accesses inside inner classes are not managed yet.
 */
public class CodeSnippetScope extends BlockScope {
/**
 * CodeSnippetScope constructor comment.
 * @param kind int
 * @param parent org.eclipse.jdt.internal.compiler.lookup.Scope
 */
protected CodeSnippetScope(int kind, Scope parent) {
	super(kind, parent);
}
/**
 * CodeSnippetScope constructor comment.
 * @param parent org.eclipse.jdt.internal.compiler.lookup.BlockScope
 */
public CodeSnippetScope(BlockScope parent) {
	super(parent);
}
/**
 * CodeSnippetScope constructor comment.
 * @param parent org.eclipse.jdt.internal.compiler.lookup.BlockScope
 * @param variableCount int
 */
public CodeSnippetScope(BlockScope parent, int variableCount) {
	super(parent, variableCount);
}
/* Answer true if the receiver is visible to the type provided by the scope.
* InvocationSite implements isSuperAccess() to provide additional information
* if the receiver is protected.
*
* NOTE: Cannot invoke this method with a compilation unit scope.
*/

public final boolean canBeSeenByForCodeSnippet(FieldBinding fieldBinding, TypeBinding receiverType, InvocationSite invocationSite, Scope scope) {
	if (fieldBinding.isPublic()) return true;

	ReferenceBinding invocationType = (ReferenceBinding) receiverType;
	if (TypeBinding.equalsEquals(invocationType, fieldBinding.declaringClass)) return true;

	if (fieldBinding.isProtected()) {
		// answer true if the invocationType is the declaringClass or they are in the same package
		// OR the invocationType is a subclass of the declaringClass
		//    AND the receiverType is the invocationType or its subclass
		//    OR the field is a static field accessed directly through a type
		if (TypeBinding.equalsEquals(invocationType, fieldBinding.declaringClass)) return true;
		if (invocationType.fPackage == fieldBinding.declaringClass.fPackage) return true;
		if (fieldBinding.declaringClass.isSuperclassOf(invocationType)) {
			if (invocationSite.isSuperAccess()) return true;
			// receiverType can be an array binding in one case... see if you can change it
			if (receiverType instanceof ArrayBinding)
				return false;
			if (invocationType.isSuperclassOf((ReferenceBinding) receiverType))
				return true;
			if (fieldBinding.isStatic())
				return true; // see 1FMEPDL - return invocationSite.isTypeAccess();
		}
		return false;
	}

	if (fieldBinding.isPrivate()) {
		// answer true if the receiverType is the declaringClass
		// AND the invocationType and the declaringClass have a common enclosingType
		if (TypeBinding.notEquals(receiverType, fieldBinding.declaringClass)) return false;

		if (TypeBinding.notEquals(invocationType, fieldBinding.declaringClass)) {
			ReferenceBinding outerInvocationType = invocationType;
			ReferenceBinding temp = outerInvocationType.enclosingType();
			while (temp != null) {
				outerInvocationType = temp;
				temp = temp.enclosingType();
			}

			ReferenceBinding outerDeclaringClass = fieldBinding.declaringClass;
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
	if (invocationType.fPackage != fieldBinding.declaringClass.fPackage) return false;

	// receiverType can be an array binding in one case... see if you can change it
	if (receiverType instanceof ArrayBinding)
		return false;
	ReferenceBinding type = (ReferenceBinding) receiverType;
	PackageBinding declaringPackage = fieldBinding.declaringClass.fPackage;
	TypeBinding originalDeclaringClass = fieldBinding.declaringClass .original();
	do {
		if (type.isCapture()) { // https://bugs.eclipse.org/bugs/show_bug.cgi?id=285002
			if (TypeBinding.equalsEquals(originalDeclaringClass, type.erasure().original())) return true;
		} else {
			if (TypeBinding.equalsEquals(originalDeclaringClass, type.original())) return true;
		}
		if (declaringPackage != type.fPackage) return false;
	} while ((type = type.superclass()) != null);
	return false;
}
/* Answer true if the receiver is visible to the type provided by the scope.
* InvocationSite implements isSuperAccess() to provide additional information
* if the receiver is protected.
*
* NOTE: Cannot invoke this method with a compilation unit scope.
*/
public final boolean canBeSeenByForCodeSnippet(MethodBinding methodBinding, TypeBinding receiverType, InvocationSite invocationSite, Scope scope) {
	if (methodBinding.isPublic()) return true;

	ReferenceBinding invocationType = (ReferenceBinding) receiverType;
	if (TypeBinding.equalsEquals(invocationType, methodBinding.declaringClass)) return true;

	if (methodBinding.isProtected()) {
		// answer true if the invocationType is the declaringClass or they are in the same package
		// OR the invocationType is a subclass of the declaringClass
		//    AND the receiverType is the invocationType or its subclass
		//    OR the method is a static method accessed directly through a type
		if (TypeBinding.equalsEquals(invocationType, methodBinding.declaringClass)) return true;
		if (invocationType.fPackage == methodBinding.declaringClass.fPackage) return true;
		if (methodBinding.declaringClass.isSuperclassOf(invocationType)) {
			if (invocationSite.isSuperAccess()) return true;
			// receiverType can be an array binding in one case... see if you can change it
			if (receiverType instanceof ArrayBinding)
				return false;
			if (invocationType.isSuperclassOf((ReferenceBinding) receiverType))
				return true;
			if (methodBinding.isStatic())
				return true; // see 1FMEPDL - return invocationSite.isTypeAccess();
		}
		return false;
	}

	if (methodBinding.isPrivate()) {
		// answer true if the receiverType is the declaringClass
		// AND the invocationType and the declaringClass have a common enclosingType
		if (TypeBinding.notEquals(receiverType, methodBinding.declaringClass)) return false;

		if (TypeBinding.notEquals(invocationType, methodBinding.declaringClass)) {
			ReferenceBinding outerInvocationType = invocationType;
			ReferenceBinding temp = outerInvocationType.enclosingType();
			while (temp != null) {
				outerInvocationType = temp;
				temp = temp.enclosingType();
			}

			ReferenceBinding outerDeclaringClass = methodBinding.declaringClass;
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
	if (invocationType.fPackage != methodBinding.declaringClass.fPackage) return false;

	// receiverType can be an array binding in one case... see if you can change it
	if (receiverType instanceof ArrayBinding)
		return false;
	ReferenceBinding type = (ReferenceBinding) receiverType;
	PackageBinding declaringPackage = methodBinding.declaringClass.fPackage;
	TypeBinding originalDeclaringClass = methodBinding.declaringClass .original();
	do {
		if (type.isCapture()) { // https://bugs.eclipse.org/bugs/show_bug.cgi?id=285002
			if (TypeBinding.equalsEquals(originalDeclaringClass, type.erasure().original())) return true;
		} else {
			if (TypeBinding.equalsEquals(originalDeclaringClass, type.original())) return true;
		}
		if (declaringPackage != type.fPackage) return false;
	} while ((type = type.superclass()) != null);
	return false;
}
/* Answer true if the receiver is visible to the type provided by the scope.
* InvocationSite implements isSuperAccess() to provide additional information
* if the receiver is protected.
*
* NOTE: Cannot invoke this method with a compilation unit scope.
*/

public final boolean canBeSeenByForCodeSnippet(ReferenceBinding referenceBinding, ReferenceBinding receiverType) {
	if (referenceBinding.isPublic()) return true;

	if (TypeBinding.equalsEquals(receiverType, referenceBinding)) return true;

	if (referenceBinding.isProtected()) {
		// answer true if the receiver (or its enclosing type) is the superclass
		//	of the receiverType or in the same package
		return receiverType.fPackage == referenceBinding.fPackage
				|| referenceBinding.isSuperclassOf(receiverType)
				|| referenceBinding.enclosingType().isSuperclassOf(receiverType); // protected types always have an enclosing one
	}

	if (referenceBinding.isPrivate()) {
		// answer true if the receiver and the receiverType have a common enclosingType
		// already know they are not the identical type
		ReferenceBinding outerInvocationType = receiverType;
		ReferenceBinding temp = outerInvocationType.enclosingType();
		while (temp != null) {
			outerInvocationType = temp;
			temp = temp.enclosingType();
		}

		ReferenceBinding outerDeclaringClass = referenceBinding;
		temp = outerDeclaringClass.enclosingType();
		while (temp != null) {
			outerDeclaringClass = temp;
			temp = temp.enclosingType();
		}
		return TypeBinding.equalsEquals(outerInvocationType, outerDeclaringClass);
	}

	// isDefault()
	return receiverType.fPackage == referenceBinding.fPackage;
}
// Internal use only
@Override
public MethodBinding findExactMethod(ReferenceBinding receiverType, char[] selector, TypeBinding[] argumentTypes, InvocationSite invocationSite) {
	MethodBinding exactMethod = receiverType.getExactMethod(selector, argumentTypes, null);
	if (exactMethod != null){
		if (receiverType.isInterface() || canBeSeenByForCodeSnippet(exactMethod, receiverType, invocationSite, this))
			return exactMethod;
	}
	return null;
}
// Internal use only

/*	Answer the field binding that corresponds to fieldName.
	Start the lookup at the receiverType.
	InvocationSite implements
		isSuperAccess(); this is used to determine if the discovered field is visible.
	Only fields defined by the receiverType or its supertypes are answered;
	a field of an enclosing type will not be found using this API.

	If no visible field is discovered, null is answered.
*/

public FieldBinding findFieldForCodeSnippet(TypeBinding receiverType, char[] fieldName, InvocationSite invocationSite) {
	if (receiverType.isBaseType())
		return null;
	if (receiverType.isArrayType()) {
		TypeBinding leafType = receiverType.leafComponentType();
		if (leafType instanceof ReferenceBinding)
		if (!((ReferenceBinding)leafType).canBeSeenBy(this)) {
			return new ProblemFieldBinding((ReferenceBinding)leafType, fieldName, ProblemReasons.ReceiverTypeNotVisible);
		}
		if (CharOperation.equals(fieldName, TypeConstants.LENGTH))
			return ArrayBinding.ArrayLength;
		return null;
	}

	ReferenceBinding currentType = (ReferenceBinding) receiverType;
	if (!currentType.canBeSeenBy(this))
		return new ProblemFieldBinding(currentType, fieldName, ProblemReasons.ReceiverTypeNotVisible);

	FieldBinding field = currentType.getField(fieldName, true /*resolve*/);
	if (field != null) {
		if (canBeSeenByForCodeSnippet(field, currentType, invocationSite, this))
			return field;
		else
			return new ProblemFieldBinding(field /* closest match*/, field.declaringClass, fieldName, ProblemReasons.NotVisible);
	}

	// collect all superinterfaces of receiverType until the field is found in a supertype
	ReferenceBinding[][] interfacesToVisit = null;
	int lastPosition = -1;
	FieldBinding visibleField = null;
	boolean keepLooking = true;
	boolean notVisible = false; // we could hold onto the not visible field for extra error reporting
	while (keepLooking) {
		ReferenceBinding[] itsInterfaces = currentType.superInterfaces();
		if (itsInterfaces != Binding.NO_SUPERINTERFACES) {
			if (interfacesToVisit == null)
				interfacesToVisit = new ReferenceBinding[5][];
			if (++lastPosition == interfacesToVisit.length)
				System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[lastPosition * 2][], 0, lastPosition);
			interfacesToVisit[lastPosition] = itsInterfaces;
		}
		if ((currentType = currentType.superclass()) == null)
			break;

		if ((field = currentType.getField(fieldName, true /*resolve*/)) != null) {
			keepLooking = false;
			if (canBeSeenByForCodeSnippet(field, receiverType, invocationSite, this)) {
				if (visibleField == null)
					visibleField = field;
				else
					return new ProblemFieldBinding(visibleField, visibleField.declaringClass, fieldName, ProblemReasons.Ambiguous);
			} else {
				notVisible = true;
			}
		}
	}

	// walk all visible interfaces to find ambiguous references
	if (interfacesToVisit != null) {
		ProblemFieldBinding ambiguous = null;
		org.eclipse.jdt.internal.compiler.util.SimpleSet interfacesSeen = new org.eclipse.jdt.internal.compiler.util.SimpleSet(lastPosition * 2);
		done : for (int i = 0; i <= lastPosition; i++) {
			ReferenceBinding[] interfaces = interfacesToVisit[i];
			for (int j = 0, length = interfaces.length; j < length; j++) {
				ReferenceBinding anInterface = interfaces[j];
				if (interfacesSeen.addIfNotIncluded(anInterface) == anInterface) {
					// if interface as not already been visited
					if ((field = anInterface.getField(fieldName, true /*resolve*/)) != null) {
						if (visibleField == null) {
							visibleField = field;
						} else {
							ambiguous = new ProblemFieldBinding(visibleField, visibleField.declaringClass, fieldName, ProblemReasons.Ambiguous);
							break done;
						}
					} else {
						ReferenceBinding[] itsInterfaces = anInterface.superInterfaces();
						if (itsInterfaces != Binding.NO_SUPERINTERFACES) {
							if (++lastPosition == interfacesToVisit.length)
								System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[lastPosition * 2][], 0, lastPosition);
							interfacesToVisit[lastPosition] = itsInterfaces;
						}
					}
				}
			}
		}
		if (ambiguous != null) return ambiguous;
	}

	if (visibleField != null)
		return visibleField;
	if (notVisible)
		return new ProblemFieldBinding(currentType, fieldName, ProblemReasons.NotVisible);
	return null;
}
// Internal use only
@Override
public MethodBinding findMethod(ReferenceBinding receiverType, char[] selector, TypeBinding[] argumentTypes, InvocationSite invocationSite, boolean inStaticContext) {
	MethodBinding methodBinding = super.findMethod(receiverType, selector, argumentTypes, invocationSite, inStaticContext);
	if (methodBinding != null && methodBinding.isValidBinding())
		if (!canBeSeenByForCodeSnippet(methodBinding, receiverType, invocationSite, this))
			return new ProblemMethodBinding(methodBinding, selector, argumentTypes, ProblemReasons.NotVisible);
	return methodBinding;
}

// Internal use only
@Override
public MethodBinding findMethodForArray(ArrayBinding receiverType, char[] selector, TypeBinding[] argumentTypes, InvocationSite invocationSite) {
	ReferenceBinding object = getJavaLangObject();
	MethodBinding methodBinding = object.getExactMethod(selector, argumentTypes, null);
	if (methodBinding != null) {
		// handle the method clone() specially... cannot be protected or throw exceptions
		if (argumentTypes == Binding.NO_PARAMETERS && CharOperation.equals(selector, TypeConstants.CLONE))
			return new MethodBinding((methodBinding.modifiers & ~ClassFileConstants.AccProtected) | ClassFileConstants.AccPublic, TypeConstants.CLONE, methodBinding.returnType, argumentTypes, null, object);
		if (canBeSeenByForCodeSnippet(methodBinding, receiverType, invocationSite, this))
			return methodBinding;
	}

	// answers closest approximation, may not check argumentTypes or visibility
	methodBinding = findMethod(object, selector, argumentTypes, invocationSite, false);
	if (methodBinding == null)
		return new ProblemMethodBinding(selector, argumentTypes, ProblemReasons.NotFound);
	if (methodBinding.isValidBinding()) {
	    MethodBinding compatibleMethod = computeCompatibleMethod(methodBinding, argumentTypes, invocationSite);
	    if (compatibleMethod == null)
			return new ProblemMethodBinding(methodBinding, selector, argumentTypes, ProblemReasons.NotFound);
	    methodBinding = compatibleMethod;
		if (!canBeSeenByForCodeSnippet(methodBinding, receiverType, invocationSite, this))
			return new ProblemMethodBinding(methodBinding, selector, methodBinding.parameters, ProblemReasons.NotVisible);
	}
	return methodBinding;
}
/* API
	flag is a mask of the following values VARIABLE (= FIELD or LOCAL), TYPE.
	Only bindings corresponding to the mask will be answered.

	if the VARIABLE mask is set then
		If the first name provided is a field (or local) then the field (or local) is answered
		Otherwise, package names and type names are consumed until a field is found.
		In this case, the field is answered.

	if the TYPE mask is set,
		package names and type names are consumed until the end of the input.
		Only if all of the input is consumed is the type answered

	All other conditions are errors, and a problem binding is returned.

	NOTE: If a problem binding is returned, senders should extract the compound name
	from the binding & not assume the problem applies to the entire compoundName.

	The VARIABLE mask has precedence over the TYPE mask.

	InvocationSite implements
		isSuperAccess(); this is used to determine if the discovered field is visible.
		setFieldIndex(int); this is used to record the number of names that were consumed.

	For example, getBinding({"foo","y","q", VARIABLE, site) will answer
	the binding for the field or local named "foo" (or an error binding if none exists).
	In addition, setFieldIndex(1) will be sent to the invocation site.
	If a type named "foo" exists, it will not be detected (and an error binding will be answered)

	IMPORTANT NOTE: This method is written under the assumption that compoundName is longer than length 1.
*/

public Binding getBinding(char[][] compoundName, int mask, InvocationSite invocationSite, ReferenceBinding receiverType) {
	Binding binding = getBinding(compoundName[0], mask | Binding.TYPE | Binding.PACKAGE, invocationSite, true /*resolve*/);
	invocationSite.setFieldIndex(1);
	if (!binding.isValidBinding() || binding instanceof VariableBinding)
		return binding;

	int length = compoundName.length;
	int currentIndex = 1;
	foundType: if (binding instanceof PackageBinding) {
		PackageBinding packageBinding = (PackageBinding) binding;

		while (currentIndex < length) {
			binding = packageBinding.getTypeOrPackage(compoundName[currentIndex++], null, currentIndex<length);
			invocationSite.setFieldIndex(currentIndex);
 			if (binding == null) {
	 			if (currentIndex == length) // must be a type if its the last name, otherwise we have no idea if its a package or type
					return new ProblemReferenceBinding(CharOperation.subarray(compoundName, 0, currentIndex), null, ProblemReasons.NotFound);
				else
					return new ProblemBinding(CharOperation.subarray(compoundName, 0, currentIndex), ProblemReasons.NotFound);
 			}
 			if (binding instanceof ReferenceBinding) {
	 			if (!binding.isValidBinding())
					return new ProblemReferenceBinding(
									CharOperation.subarray(compoundName, 0, currentIndex),
									(ReferenceBinding)((ReferenceBinding)binding).closestMatch(),
									binding.problemId());
	 			if (!this.canBeSeenByForCodeSnippet((ReferenceBinding) binding, receiverType))
					return new ProblemReferenceBinding(CharOperation.subarray(compoundName, 0, currentIndex), (ReferenceBinding) binding, ProblemReasons.NotVisible);
	 			break foundType;
 			}
 			packageBinding = (PackageBinding) binding;
		}

		// It is illegal to request a PACKAGE from this method.
		return new ProblemReferenceBinding(CharOperation.subarray(compoundName, 0, currentIndex), null, ProblemReasons.NotFound);
	}

	// know binding is now a ReferenceBinding
	while (currentIndex < length) {
		ReferenceBinding typeBinding = (ReferenceBinding) binding;
		char[] nextName = compoundName[currentIndex++];
		invocationSite.setFieldIndex(currentIndex);
		if ((binding = findFieldForCodeSnippet(typeBinding, nextName, invocationSite)) != null) {
			if (!binding.isValidBinding()) {
				return new ProblemFieldBinding(
						(FieldBinding)binding,
						((FieldBinding)binding).declaringClass,
						CharOperation.concatWith(CharOperation.subarray(compoundName, 0, currentIndex), '.'),
						binding.problemId());
			}
			break; // binding is now a field
		}
		if ((binding = findMemberType(nextName, typeBinding)) == null)
			return new ProblemBinding(CharOperation.subarray(compoundName, 0, currentIndex), typeBinding, ProblemReasons.NotFound);
		 if (!binding.isValidBinding())
			return new ProblemReferenceBinding(
								CharOperation.subarray(compoundName, 0, currentIndex),
								(ReferenceBinding)((ReferenceBinding)binding).closestMatch(),
								binding.problemId());
	}

	if ((mask & Binding.FIELD) != 0 && (binding instanceof FieldBinding)) { // was looking for a field and found a field
		FieldBinding field = (FieldBinding) binding;
		if (!field.isStatic()) {
			return new ProblemFieldBinding(
					field,
					field.declaringClass,
					CharOperation.concatWith(CharOperation.subarray(compoundName, 0, currentIndex), '.'),
					ProblemReasons.NonStaticReferenceInStaticContext);
		}
		// Since a qualified reference must be for a static member, it won't affect static-ness of the enclosing method,
		// so we don't have to call resetEnclosingMethodStaticFlag() in this case
		return binding;
	}
	if ((mask & Binding.TYPE) != 0 && (binding instanceof ReferenceBinding)) { // was looking for a type and found a type
		return binding;
	}

	// handle the case when a field or type was asked for but we resolved the compoundName to a type or field
	return new ProblemBinding(CharOperation.subarray(compoundName, 0, currentIndex), ProblemReasons.NotFound);
}
/* API

	Answer the constructor binding that corresponds to receiverType, argumentTypes.

	InvocationSite implements
		isSuperAccess(); this is used to determine if the discovered constructor is visible.

	If no visible constructor is discovered, an error binding is answered.
*/

@Override
public MethodBinding getConstructor(ReferenceBinding receiverType, TypeBinding[] argumentTypes, InvocationSite invocationSite) {
	MethodBinding methodBinding = receiverType.getExactConstructor(argumentTypes);
	if (methodBinding != null) {
		if (canBeSeenByForCodeSnippet(methodBinding, receiverType, invocationSite, this)) {
			return methodBinding;
		}
	}
	MethodBinding[] methods = receiverType.getMethods(TypeConstants.INIT);
	if (methods == Binding.NO_METHODS) {
		return new ProblemMethodBinding(TypeConstants.INIT, argumentTypes, ProblemReasons.NotFound);
	}
	MethodBinding[] compatible = new MethodBinding[methods.length];
	int compatibleIndex = 0;
	for (int i = 0, length = methods.length; i < length; i++) {
	    MethodBinding compatibleMethod = computeCompatibleMethod(methods[i], argumentTypes, invocationSite);
		if (compatibleMethod != null)
			compatible[compatibleIndex++] = compatibleMethod;
	}
	if (compatibleIndex == 0)
		return new ProblemMethodBinding(TypeConstants.INIT, argumentTypes, ProblemReasons.NotFound); // need a more descriptive error... cannot convert from X to Y

	MethodBinding[] visible = new MethodBinding[compatibleIndex];
	int visibleIndex = 0;
	for (int i = 0; i < compatibleIndex; i++) {
		MethodBinding method = compatible[i];
		if (canBeSeenByForCodeSnippet(method, receiverType, invocationSite, this)) {
			visible[visibleIndex++] = method;
		}
	}
	if (visibleIndex == 1) {
		return visible[0];
	}
	if (visibleIndex == 0) {
		return new ProblemMethodBinding(compatible[0], TypeConstants.INIT, compatible[0].parameters, ProblemReasons.NotVisible);
	}
	return mostSpecificClassMethodBinding(visible, visibleIndex, invocationSite);
}
/* API

	Answer the field binding that corresponds to fieldName.
	Start the lookup at the receiverType.
	InvocationSite implements
		isSuperAccess(); this is used to determine if the discovered field is visible.
	Only fields defined by the receiverType or its supertypes are answered;
	a field of an enclosing type will not be found using this API.

	If no visible field is discovered, an error binding is answered.
*/

public FieldBinding getFieldForCodeSnippet(TypeBinding receiverType, char[] fieldName, InvocationSite invocationSite) {
	FieldBinding field = findFieldForCodeSnippet(receiverType, fieldName, invocationSite);
	if (field == null)
		return new ProblemFieldBinding(receiverType instanceof ReferenceBinding ? (ReferenceBinding) receiverType : null, fieldName, ProblemReasons.NotFound);
	else
		return field;
}
/* API

	Answer the method binding that corresponds to selector, argumentTypes.
	Start the lookup at the enclosing type of the receiver.
	InvocationSite implements
		isSuperAccess(); this is used to determine if the discovered method is visible.
		setDepth(int); this is used to record the depth of the discovered method
			relative to the enclosing type of the receiver. (If the method is defined
			in the enclosing type of the receiver, the depth is 0; in the next enclosing
			type, the depth is 1; and so on

	If no visible method is discovered, an error binding is answered.
*/

public MethodBinding getImplicitMethod(ReferenceBinding receiverType, char[] selector, TypeBinding[] argumentTypes, InvocationSite invocationSite) {
	// retrieve an exact visible match (if possible)
	MethodBinding methodBinding = findExactMethod(receiverType, selector, argumentTypes, invocationSite);
	if (methodBinding == null)
		methodBinding = findMethod(receiverType, selector, argumentTypes, invocationSite, false);
	if (methodBinding != null) { // skip it if we did not find anything
		if (methodBinding.isValidBinding())
		    if (!canBeSeenByForCodeSnippet(methodBinding, receiverType, invocationSite, this))
				return new ProblemMethodBinding(methodBinding, selector, argumentTypes, ProblemReasons.NotVisible);
		return methodBinding;
	}
	return new ProblemMethodBinding(selector, argumentTypes, ProblemReasons.NotFound);
}
}
