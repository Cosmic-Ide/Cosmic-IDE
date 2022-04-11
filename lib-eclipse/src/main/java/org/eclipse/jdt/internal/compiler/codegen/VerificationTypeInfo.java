/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.codegen;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;

public class VerificationTypeInfo {
	/**
	 * The tag value representing top variable info
	 *
	 * @since 3.2
	 */
	public static final int ITEM_TOP = 0;
	/**
	 * The tag value representing integer variable info
	 *
	 * @since 3.2
	 */
	public static final int ITEM_INTEGER = 1;
	/**
	 * The tag value representing float variable info
	 *
	 * @since 3.2
	 */
	public static final int ITEM_FLOAT = 2;
	/**
	 * The tag value representing double variable info
	 *
	 * @since 3.2
	 */
	public static final int ITEM_DOUBLE = 3;
	/**
	 * The tag value representing long variable info
	 *
	 * @since 3.2
	 */
	public static final int ITEM_LONG = 4;
	/**
	 * The tag value representing null variable info
	 *
	 * @since 3.2
	 */
	public static final int ITEM_NULL = 5;
	/**
	 * The tag value representing uninitialized this variable info
	 *
	 * @since 3.2
	 */
	public static final int ITEM_UNINITIALIZED_THIS = 6;
	/**
	 * The tag value representing object variable info
	 *
	 * @since 3.2
	 */
	public static final int ITEM_OBJECT = 7;
	/**
	 * The tag value representing uninitialized variable info
	 *
	 * @since 3.2
	 */
	public static final int ITEM_UNINITIALIZED = 8;

	public int tag;
	private int id;
	private TypeBinding binding;
	public int offset; // where the new opcode is used
	private List<TypeBinding> bindings;

	public VerificationTypeInfo(int tag, TypeBinding binding) {
		this(binding);
		this.tag = tag;
	}

	public VerificationTypeInfo(TypeBinding binding) {
		if (binding == null) return;
		this.id = binding.id;
		this.binding = binding;
		switch (binding.id) {
			case TypeIds.T_boolean:
			case TypeIds.T_byte:
			case TypeIds.T_char:
			case TypeIds.T_int:
			case TypeIds.T_short:
				this.tag = VerificationTypeInfo.ITEM_INTEGER;
				break;
			case TypeIds.T_float:
				this.tag = VerificationTypeInfo.ITEM_FLOAT;
				break;
			case TypeIds.T_long:
				this.tag = VerificationTypeInfo.ITEM_LONG;
				break;
			case TypeIds.T_double:
				this.tag = VerificationTypeInfo.ITEM_DOUBLE;
				break;
			case TypeIds.T_null:
				this.tag = VerificationTypeInfo.ITEM_NULL;
				break;
			default:
				this.tag = VerificationTypeInfo.ITEM_OBJECT;
		}
	}

	public void setBinding(TypeBinding binding) {
		final int typeBindingId = binding.id;
		this.id = typeBindingId;
		switch (typeBindingId) {
			case TypeIds.T_boolean:
			case TypeIds.T_byte:
			case TypeIds.T_char:
			case TypeIds.T_int:
			case TypeIds.T_short:
				this.tag = VerificationTypeInfo.ITEM_INTEGER;
				break;
			case TypeIds.T_float:
				this.tag = VerificationTypeInfo.ITEM_FLOAT;
				break;
			case TypeIds.T_long:
				this.tag = VerificationTypeInfo.ITEM_LONG;
				break;
			case TypeIds.T_double:
				this.tag = VerificationTypeInfo.ITEM_DOUBLE;
				break;
			case TypeIds.T_null:
				this.tag = VerificationTypeInfo.ITEM_NULL;
				break;
			default:
				this.tag = VerificationTypeInfo.ITEM_OBJECT;
		}
	}

	public int id() {
		return this.id;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		switch (this.tag) {
			case VerificationTypeInfo.ITEM_UNINITIALIZED_THIS:
				buffer.append("uninitialized_this(").append(readableName()).append(")"); //$NON-NLS-1$//$NON-NLS-2$
				break;
			case VerificationTypeInfo.ITEM_UNINITIALIZED:
				buffer.append("uninitialized(").append(readableName()).append(")"); //$NON-NLS-1$//$NON-NLS-2$
				break;
			case VerificationTypeInfo.ITEM_OBJECT:
				buffer.append(readableName());
				break;
			case VerificationTypeInfo.ITEM_DOUBLE:
				buffer.append('D');
				break;
			case VerificationTypeInfo.ITEM_FLOAT:
				buffer.append('F');
				break;
			case VerificationTypeInfo.ITEM_INTEGER:
				buffer.append('I');
				break;
			case VerificationTypeInfo.ITEM_LONG:
				buffer.append('J');
				break;
			case VerificationTypeInfo.ITEM_NULL:
				buffer.append("null"); //$NON-NLS-1$
				break;
			case VerificationTypeInfo.ITEM_TOP:
				buffer.append("top"); //$NON-NLS-1$
				break;
		}
		return String.valueOf(buffer);
	}

	public VerificationTypeInfo duplicate() {
		VerificationTypeInfo verificationTypeInfo = new VerificationTypeInfo(this.tag, this.binding);
		verificationTypeInfo.offset = this.offset;
		return verificationTypeInfo;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VerificationTypeInfo) {
			VerificationTypeInfo info1 = (VerificationTypeInfo) obj;
			return info1.tag == this.tag
					&& info1.offset == this.offset
					&& CharOperation.equals(info1.constantPoolName(), constantPoolName());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.tag + this.offset + CharOperation.hashCode(constantPoolName());
	}

	public char[] constantPoolName() {
		return this.binding.constantPoolName();
	}

	public char[] readableName() {
		return this.constantPoolName();
	}

	public void replaceWithElementType() {
		ArrayBinding arrayBinding = (ArrayBinding) this.binding;
		this.binding = arrayBinding.elementsType();
		this.id = this.binding.id;
	}

	public VerificationTypeInfo merge(VerificationTypeInfo verificationTypeInfo, Scope scope) {
		if (this.binding.isBaseType() && verificationTypeInfo.binding.isBaseType()) {
			return this;
		}
		if (!this.binding.equals(verificationTypeInfo.binding)) {
			if (this.bindings == null) {
				this.bindings = new ArrayList<TypeBinding>();
				this.bindings.add(this.binding);
			}
			this.bindings.add(verificationTypeInfo.binding);
			this.binding = scope.lowerUpperBound(this.bindings.toArray(new TypeBinding[this.bindings.size()]));
			if (this.binding != null) {
				this.id = this.binding.id;
				switch (this.id) {
					case TypeIds.T_null:
						this.tag = VerificationTypeInfo.ITEM_NULL;
						break;
					default:
						this.tag = VerificationTypeInfo.ITEM_OBJECT;
				}
			} else {
				this.binding = scope.getJavaLangObject();
				this.tag = VerificationTypeInfo.ITEM_OBJECT;
			}
		}
		return this;
	}
}
