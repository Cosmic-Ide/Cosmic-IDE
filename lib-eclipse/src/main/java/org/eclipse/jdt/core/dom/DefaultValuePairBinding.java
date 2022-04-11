/*******************************************************************************
 * Copyright (c) 2005, 2013 BEA Systems, Inc.
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
 *    IBM Corporation - renamed from ResolvedDefaultValuePair to DefaultValuePairBinding
 *    IBM Corporation - Fix for 328969
 *******************************************************************************/
package org.eclipse.jdt.core.dom;

/**
 * Member value pair which compose of default values.
 */
class DefaultValuePairBinding extends MemberValuePairBinding {

	private org.eclipse.jdt.internal.compiler.lookup.MethodBinding method;

	DefaultValuePairBinding(org.eclipse.jdt.internal.compiler.lookup.MethodBinding binding, BindingResolver resolver) {
		super(null, resolver);
		this.method = binding;
		this.value = MemberValuePairBinding.buildDOMValue(binding.getDefaultValue(), resolver);
		if (binding.returnType != null && binding.returnType.isArrayType()) {
			// wrap into an array
			if (this.value == null) {
				this.value = new Object[0];
			} else if (!this.value.getClass().isArray()) {
				this.value = new Object[] { this.value };
			}
		}
	}

	@Override
	public IMethodBinding getMethodBinding() {
		return this.bindingResolver.getMethodBinding(this.method);
	}

	@Override
	public String getName() {
		return new String(this.method.selector);
	}

	@Override
	public Object getValue() {
		return this.value;
	}

	@Override
	public boolean isDefault() {
		return true;
	}

	@Override
	public boolean isDeprecated() {
		return this.method.isDeprecated();
	}
}
