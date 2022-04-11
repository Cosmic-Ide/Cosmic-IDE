/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.Constant;

public class ElementValuePair {
	char[] name;
	public Object value;
	public MethodBinding binding;

	/**
	 * We want to avoid eagerly resolving of all enums that are used in annotations.
	 * This class encapsulates an unresolved enum constant as referenced in an ElementValuePair.
	 * The enum constant will be resolved when asking getValue()
	 */
	public static class UnresolvedEnumConstant {
		ReferenceBinding enumType;
		LookupEnvironment environment;
		char[] enumConstantName;
		UnresolvedEnumConstant(ReferenceBinding enumType, LookupEnvironment environment, char[] enumConstantName) {
			this.enumType = enumType;
			this.environment = environment;
			this.enumConstantName = enumConstantName;
		}
		FieldBinding getResolved() {
			if (this.enumType.isUnresolvedType())
				this.enumType = (ReferenceBinding) BinaryTypeBinding.resolveType(this.enumType, this.environment, false /* no raw conversion */);
			return this.enumType.getField(this.enumConstantName, false);
		}
		public char[] getEnumConstantName() {
			return this.enumConstantName;
		}
	}

public static Object getValue(Expression expression) {
	if (expression == null)
		return null;
	Constant constant = expression.constant;
	// literals would hit this case.
	if (constant != null && constant != Constant.NotAConstant)
		return constant;

	if (expression instanceof Annotation)
		return ((Annotation) expression).getCompilerAnnotation();
	if (expression instanceof ArrayInitializer) {
		Expression[] exprs = ((ArrayInitializer) expression).expressions;
		int length = exprs == null ? 0 : exprs.length;
		Object[] values = new Object[length];
		for (int i = 0; i < length; i++)
			values[i] = getValue(exprs[i]);
		return values;
	}
	if (expression instanceof ClassLiteralAccess)
		return ((ClassLiteralAccess) expression).targetType;
	if (expression instanceof Reference) {
		FieldBinding fieldBinding = null;
		if (expression instanceof FieldReference) {
			fieldBinding = ((FieldReference) expression).fieldBinding();
		} else if (expression instanceof NameReference) {
			Binding binding = ((NameReference) expression).binding;
			if (binding != null && binding.kind() == Binding.FIELD)
				fieldBinding = (FieldBinding) binding;
		}
		if (fieldBinding != null && (fieldBinding.modifiers & ClassFileConstants.AccEnum) > 0)
			return fieldBinding;
	}
	// something that isn't a compile time constant.
	return null;
}

public ElementValuePair(char[] name, Expression expression, MethodBinding binding) {
	this(name, ElementValuePair.getValue(expression), binding);
}

public ElementValuePair(char[] name, Object value, MethodBinding binding) {
	this.name = name;
	this.value = value;
	this.binding = binding;
}

/**
 * @return the name of the element value pair.
 */
public char[] getName() {
	return this.name;
}

/**
 * @return the method binding that defined this member value pair or null if no such binding exists.
 */
public MethodBinding getMethodBinding() {
	return this.binding;
}

/**
 * Return {@link TypeBinding} for member value of type {@link java.lang.Class}
 * Return {@link org.eclipse.jdt.internal.compiler.impl.Constant} for member of primitive type or String
 * Return {@link FieldBinding} for enum constant
 * Return {@link AnnotationBinding} for annotation instance
 * Return <code>Object[]</code> for member value of array type.
 * @return the value of this member value pair or null if the value is missing or is not a compile-time constant
 */
public Object getValue() {
	if (this.value instanceof UnresolvedEnumConstant)
		this.value = ((UnresolvedEnumConstant)this.value).getResolved();
	else if (this.value instanceof Object[]) {
		Object[] valueArray = (Object[]) this.value;
		for(int i = 0; i < valueArray.length; i++) {
			Object object = valueArray[i];
			if (object instanceof UnresolvedEnumConstant)
				valueArray[i] = ((UnresolvedEnumConstant) object).getResolved();
		}
	}
	return this.value;
}

void setMethodBinding(MethodBinding binding) {
	// lazily set after annotation type was resolved
	this.binding = binding;
}

void setValue(Object value) {
	// can be modified after the initialization if holding an unresolved ref
	this.value = value;
}

@Override
public String toString() {
	StringBuilder buffer = new StringBuilder(5);
	buffer.append(this.name).append(" = "); //$NON-NLS-1$
	buffer.append(this.value);
	return buffer.toString();
}
}
