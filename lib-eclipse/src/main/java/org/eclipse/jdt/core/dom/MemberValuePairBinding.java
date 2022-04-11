/*******************************************************************************
 * Copyright (c) 2005, 2021 BEA Systems, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    tyeung@bea.com - initial API and implementation
 *    IBM Corporation - implemented methods from IBinding
 *    IBM Corporation - renamed from ResolvedMemberValuePair to MemberValuePairBinding
 *    jgarms@bea.com - Fix for IllegalStateException
 *    IBM Corporation - Fix for 223225
 *******************************************************************************/
package org.eclipse.jdt.core.dom;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.ElementValuePair;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;

/**
 * Internal class.
 */
class MemberValuePairBinding implements IMemberValuePairBinding {
	static final MemberValuePairBinding[] NoPair = new MemberValuePairBinding[0];
	private static final Object NoValue = new Object();
	private static final Object[] EmptyArray = new Object[0];

	private ElementValuePair internalPair;
	protected Object value = null;
	protected BindingResolver bindingResolver;

	static void appendValue(Object value, StringBuffer buffer) {
		if (value instanceof Object[]) {
			Object[] values = (Object[]) value;
			buffer.append('{');
			for (int i = 0, l = values.length; i < l; i++) {
				if (i != 0)
					buffer.append(", "); //$NON-NLS-1$
				appendValue(values[i], buffer);
			}
			buffer.append('}');
		} else if (value instanceof ITypeBinding) {
			buffer.append(((ITypeBinding) value).getName());
			buffer.append(".class"); //$NON-NLS-1$
		} else {
			buffer.append(value);
		}
	}

	static Object buildDOMValue(final Object internalObject, BindingResolver resolver) {
		if (internalObject == null)
			return null;

		if (internalObject instanceof Constant) {
			Constant constant = (Constant) internalObject;
			switch (constant.typeID()) {
				case TypeIds.T_boolean:
					return Boolean.valueOf(constant.booleanValue());
				case TypeIds.T_byte:
					return Byte.valueOf(constant.byteValue());
				case TypeIds.T_char:
					return Character.valueOf(constant.charValue());
				case TypeIds.T_double:
					return Double.valueOf(constant.doubleValue());
				case TypeIds.T_float:
					return Float.valueOf(constant.floatValue());
				case TypeIds.T_int:
					return Integer.valueOf(constant.intValue());
				case TypeIds.T_long:
					return Long.valueOf(constant.longValue());
				case TypeIds.T_short:
					return Short.valueOf(constant.shortValue());
				default:
					// TypeIds.T_JavaLangString:
					return constant.stringValue();
			}
		} else if (internalObject instanceof org.eclipse.jdt.internal.compiler.lookup.TypeBinding) {
			return resolver.getTypeBinding((org.eclipse.jdt.internal.compiler.lookup.TypeBinding) internalObject);
		} else if (internalObject instanceof org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding) {
			return resolver.getAnnotationInstance((org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding) internalObject);
		} else if (internalObject instanceof org.eclipse.jdt.internal.compiler.lookup.FieldBinding) {
			return resolver.getVariableBinding((org.eclipse.jdt.internal.compiler.lookup.FieldBinding) internalObject);
		} else if (internalObject instanceof Object[]) {
			Object[] elements = (Object[]) internalObject;
			int length = elements.length;
			Object[] values = length == 0 ? EmptyArray : new Object[length];
			for (int i = 0; i < length; i++)
				values[i] = buildDOMValue(elements[i], resolver);
			return values;
		}
		return null;
	}

	MemberValuePairBinding(ElementValuePair pair, BindingResolver resolver) {
		this.internalPair = pair;
		this.bindingResolver = resolver;
	}

	@Override
	public IAnnotationBinding[] getAnnotations() {
		return AnnotationBinding.NoAnnotations;
	}

	@Override
	public IJavaElement getJavaElement() {
		return null;
	}

	@Override
	public String getKey() {
		// TODO when implementing, update spec in IBinding
		return null;
	}

	@Override
	public int getKind() {
		return IBinding.MEMBER_VALUE_PAIR;
	}

	@Override
	public IMethodBinding getMethodBinding() {
		return this.bindingResolver.getMethodBinding(this.internalPair.getMethodBinding());
	}

	@Override
	public int getModifiers() {
		return Modifier.NONE;
	}

	@Override
	public String getName() {
		if (this.internalPair == null)
			return null;
		final char[] membername = this.internalPair.getName();
		return membername == null ? null : new String(membername);
	}

	@Override
	public Object getValue() {
		if (this.value == null)
			init();
		return this.value == NoValue ? null : this.value;
	}

	private void init() {
		this.value = buildDOMValue(this.internalPair.getValue(), this.bindingResolver);
		if (this.value == null)
			this.value = NoValue;
		IMethodBinding methodBinding = getMethodBinding();
		if (methodBinding.getReturnType().isArray() && !this.value.getClass().isArray()) {
			this.value = new Object[] { this.value };
		}
	}

	char[] internalName() {
		return this.internalPair == null ? null : this.internalPair.getName();
	}

	@Override
	public boolean isDefault() {
		Object value2 = getValue();
		Object defaultValue = getMethodBinding().getDefaultValue();
		if (value2 instanceof IBinding) {
			if (defaultValue instanceof IBinding) {
				return ((IBinding) value2).isEqualTo((IBinding) defaultValue);
			}
			return false;
		}
		if (defaultValue == null) return false;
		return defaultValue.equals(value2);
	}

	@Override
	public boolean isDeprecated() {
		MethodBinding methodBinding = this.internalPair.getMethodBinding();
		return methodBinding == null ? false : methodBinding.isDeprecated();
	}

	@Override
	public boolean isEqualTo(IBinding binding) {
		if (this == binding)
			return true;
		if (binding.getKind() != IBinding.MEMBER_VALUE_PAIR)
			return false;
		IMemberValuePairBinding otherMemberValuePairBinding = (IMemberValuePairBinding) binding;
		if (!getMethodBinding().isEqualTo(otherMemberValuePairBinding.getMethodBinding())) {
			return false;
		}
		Object otherValue = otherMemberValuePairBinding.getValue();
		Object currentValue = getValue();
		if (currentValue == null) {
			return otherValue == null;
		}
		if (currentValue instanceof IBinding) {
			if (otherValue instanceof IBinding) {
				return ((IBinding) currentValue).isEqualTo((IBinding) otherValue);
			}
			return false;
		}
		if (currentValue.getClass().isArray()) {
			if (!otherValue.getClass().isArray()) {
				return false;
			}
			Object[] currentValues = (Object[]) currentValue;
			Object[] otherValues = (Object[]) otherValue;
			final int length = currentValues.length;
			if (length != otherValues.length) {
				return false;
			}
			for (int i = 0; i < length; i++) {
				Object current = currentValues[i];
				Object other = otherValues[i];
				if (current instanceof IBinding) {
					if (!(other instanceof IBinding)) {
						return false;
					}
					if (!((IBinding) current).isEqualTo((IBinding) other)) {
						return false;
					}
				} else if (!current.equals(other)) {
					return false;
				}
			}
			return true;
		} else {
			return currentValue.equals(otherValue);
		}
	}

	@Override
	public boolean isRecovered() {
		return false;
	}

	@Override
	public boolean isSynthetic() {
		return false;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(getName());
		buffer.append(" = "); //$NON-NLS-1$
		appendValue(getValue(), buffer);
		return buffer.toString();
	}
}
