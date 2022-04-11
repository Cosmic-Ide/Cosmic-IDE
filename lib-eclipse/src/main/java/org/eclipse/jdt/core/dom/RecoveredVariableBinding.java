/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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
package org.eclipse.jdt.core.dom;

import org.eclipse.jdt.core.IJavaElement;

/**
 * This class represents the recovered binding for a variable
 */
class RecoveredVariableBinding implements IVariableBinding {

	private VariableDeclaration variableDeclaration;
	private BindingResolver resolver;

	RecoveredVariableBinding(BindingResolver resolver, VariableDeclaration variableDeclaration) {
		this.resolver = resolver;
		this.variableDeclaration = variableDeclaration;
	}
	@Override
	public Object getConstantValue() {
		return null;
	}

	@Override
	public ITypeBinding getDeclaringClass() {
		ASTNode parent = this.variableDeclaration.getParent();
		while (parent != null && parent.getNodeType() != ASTNode.TYPE_DECLARATION) {
			parent = parent.getParent();
		}
		if (parent != null) {
			return ((TypeDeclaration) parent).resolveBinding();
		}
		return null;
	}

	@Override
	public IMethodBinding getDeclaringMethod() {
		ASTNode parent = this.variableDeclaration.getParent();
		while (parent != null && parent.getNodeType() != ASTNode.METHOD_DECLARATION) {
			parent = parent.getParent();
		}
		if (parent != null) {
			return ((MethodDeclaration) parent).resolveBinding();
		}
		return null;
	}

	@Override
	public String getName() {
		return this.variableDeclaration.getName().getIdentifier();
	}

	@Override
	public ITypeBinding getType() {
		return this.resolver.getTypeBinding(this.variableDeclaration);
	}

	@Override
	public IVariableBinding getVariableDeclaration() {
		return this;
	}

	@Override
	public int getVariableId() {
		return 0;
	}

	@Override
	public boolean isEnumConstant() {
		return false;
	}

	@Override
	public boolean isField() {
		return this.variableDeclaration.getParent() instanceof FieldDeclaration;
	}

	@Override
	public boolean isParameter() {
		return this.variableDeclaration instanceof SingleVariableDeclaration;
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
		StringBuffer buffer = new StringBuffer();
		buffer.append("Recovered#"); //$NON-NLS-1$
		if (this.variableDeclaration != null) {
			buffer
				.append("variableDeclaration") //$NON-NLS-1$
				.append(this.variableDeclaration.getClass())
				.append(this.variableDeclaration.getName().getIdentifier())
				.append(this.variableDeclaration.getExtraDimensions());
		}
		return String.valueOf(buffer);
	}

	@Override
	public int getKind() {
		return IBinding.VARIABLE;
	}

	@Override
	public int getModifiers() {
		return 0;
	}

	@Override
	public boolean isDeprecated() {
		return false;
	}

	@Override
	public boolean isEqualTo(IBinding binding) {
		if (binding.isRecovered() && binding.getKind() == IBinding.VARIABLE) {
			return getKey().equals(binding.getKey());
		}
		return false;
	}

	@Override
	public boolean isRecovered() {
		return true;
	}

	@Override
	public boolean isSynthetic() {
		return false;
	}
	@Override
	public boolean isEffectivelyFinal() {
		return false;
	}
}
