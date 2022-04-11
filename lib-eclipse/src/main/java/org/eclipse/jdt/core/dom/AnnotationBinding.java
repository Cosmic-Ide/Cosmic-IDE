/*******************************************************************************
 * Copyright (c) 2005, 2017 BEA Systems, Inc, IBM Corporation and others.
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
 *    IBM Corporation - renamed from ResolvedAnnotation to AnnotationBinding
 *    IBM Corporation - Fix for 328969
 *******************************************************************************/
package org.eclipse.jdt.core.dom;

import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.internal.compiler.lookup.ElementValuePair;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.util.*;

/**
 * Internal class
 */
class AnnotationBinding implements IAnnotationBinding {
	static final AnnotationBinding[] NoAnnotations = new AnnotationBinding[0];
	private org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding binding;
	private BindingResolver bindingResolver;
	private String key;

	AnnotationBinding(org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding annotation, BindingResolver resolver) {
		if (annotation == null)
			throw new IllegalStateException();
		this.binding = annotation;
		this.bindingResolver = resolver;
	}

	@Override
	public IAnnotationBinding[] getAnnotations() {
		return NoAnnotations;
	}

	@Override
	public ITypeBinding getAnnotationType() {
		ITypeBinding typeBinding = this.bindingResolver.getTypeBinding(this.binding.getAnnotationType());
		if (typeBinding == null)
			return null;
		return typeBinding;
	}

	@Override
	public IMemberValuePairBinding[] getDeclaredMemberValuePairs() {
		ReferenceBinding typeBinding = this.binding.getAnnotationType();
		if (typeBinding == null || ((typeBinding.tagBits & TagBits.HasMissingType) != 0)) {
			return MemberValuePairBinding.NoPair;
		}
		ElementValuePair[] internalPairs = this.binding.getElementValuePairs();
		int length = internalPairs.length;
		IMemberValuePairBinding[] pairs = length == 0 ? MemberValuePairBinding.NoPair : new MemberValuePairBinding[length];
		int counter = 0;
		for (int i = 0; i < length; i++) {
			ElementValuePair valuePair = internalPairs[i];
			if (valuePair.binding == null) continue;
			pairs[counter++] = this.bindingResolver.getMemberValuePairBinding(valuePair);
		}
		if (counter == 0) return MemberValuePairBinding.NoPair;
		if (counter != length) {
			// resize
			System.arraycopy(pairs, 0, (pairs = new MemberValuePairBinding[counter]), 0, counter);
		}
		return pairs;
	}

	@Override
	public IMemberValuePairBinding[] getAllMemberValuePairs() {
		IMemberValuePairBinding[] pairs = getDeclaredMemberValuePairs();
		ReferenceBinding typeBinding = this.binding.getAnnotationType();
		if (typeBinding == null || ((typeBinding.tagBits & TagBits.HasMissingType) != 0)) return pairs;
		MethodBinding[] methods = typeBinding.availableMethods(); // resilience
		int methodLength = methods == null ? 0 : methods.length;
		if (methodLength == 0) return pairs;

		int declaredLength = pairs.length;
		if (declaredLength == methodLength)
			return pairs;

		HashtableOfObject table = new HashtableOfObject(declaredLength);
		for (int i = 0; i < declaredLength; i++) {
			char[] internalName = ((MemberValuePairBinding) pairs[i]).internalName();
			if (internalName == null) continue;
			table.put(internalName, pairs[i]);
		}

		// handle case of more methods than declared members
		IMemberValuePairBinding[] allPairs = new  IMemberValuePairBinding[methodLength];
		for (int i = 0; i < methodLength; i++) {
			Object pair = table.get(methods[i].selector);
			allPairs[i] = pair == null ? new DefaultValuePairBinding(methods[i], this.bindingResolver) : (IMemberValuePairBinding) pair;
		}
		return allPairs;
	}

	@Override
	public IJavaElement getJavaElement() {
		if (!(this.bindingResolver instanceof DefaultBindingResolver)) return null;
		ASTNode node = (ASTNode) ((DefaultBindingResolver) this.bindingResolver).bindingsToAstNodes.get(this);
		if (!(node instanceof Annotation)) return null;
		ASTNode parent = node.getParent();
		IJavaElement parentElement = null;
		switch (parent.getNodeType()) {
		case ASTNode.PACKAGE_DECLARATION:
			IJavaElement cu = ((CompilationUnit) parent.getParent()).getJavaElement();
			if (cu instanceof ICompilationUnit) {
				String pkgName = ((PackageDeclaration) parent).getName().getFullyQualifiedName();
				parentElement =  ((ICompilationUnit) cu).getPackageDeclaration(pkgName);
			}
			break;
		case ASTNode.ENUM_DECLARATION:
		case ASTNode.TYPE_DECLARATION:
		case ASTNode.ANNOTATION_TYPE_DECLARATION:
			parentElement = ((AbstractTypeDeclaration) parent).resolveBinding().getJavaElement();
			break;
		case ASTNode.FIELD_DECLARATION:
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) ((FieldDeclaration) parent).fragments().get(0);
			IVariableBinding variableBinding = fragment.resolveBinding();
			if (variableBinding == null) {
				return null;
			}
			parentElement = variableBinding.getJavaElement();
			break;
		case ASTNode.METHOD_DECLARATION:
				IMethodBinding methodBinding = ((MethodDeclaration) parent).resolveBinding();
				if (methodBinding == null) return null;
				parentElement = methodBinding.getJavaElement();
			break;
		case ASTNode.MODULE_DECLARATION:
			IModuleBinding moduleBinding = ((ModuleDeclaration) parent).resolveBinding();
			if (moduleBinding == null) return null;
			parentElement = moduleBinding.getJavaElement();
		break;
		case ASTNode.VARIABLE_DECLARATION_STATEMENT:
			fragment = (VariableDeclarationFragment) ((VariableDeclarationStatement) parent).fragments().get(0);
			variableBinding = fragment.resolveBinding();
			if (variableBinding == null) {
				return null;
			}
			parentElement = variableBinding.getJavaElement();
			break;
		default:
			return null;
		}
		if (! (parentElement instanceof IAnnotatable)) return null;
		if ((parentElement instanceof IMember) && ((IMember) parentElement).isBinary()) {
			return ((IAnnotatable) parentElement).getAnnotation(getAnnotationType().getQualifiedName());
		}
		return ((IAnnotatable) parentElement).getAnnotation(getName());
	}

	@Override
	public String getKey() {
		if (this.key == null) {
			String recipientKey = getRecipientKey();
			this.key = new String(this.binding.computeUniqueKey(recipientKey.toCharArray()));
		}
		return this.key;
	}

	private String getRecipientKey() {
		if (!(this.bindingResolver instanceof DefaultBindingResolver)) return ""; //$NON-NLS-1$
		DefaultBindingResolver resolver = (DefaultBindingResolver) this.bindingResolver;
		ASTNode node = (ASTNode) resolver.bindingsToAstNodes.get(this);
		if (node == null) {
			// Can happen if annotation bindings have been resolved before having parsed the declaration
			return ""; //$NON-NLS-1$
		}
		ASTNode recipient = node.getParent();
		switch (recipient.getNodeType()) {
		case ASTNode.PACKAGE_DECLARATION:
			String pkgName = ((PackageDeclaration) recipient).getName().getFullyQualifiedName();
			return pkgName.replace('.', '/');
		case ASTNode.TYPE_DECLARATION:
			return ((TypeDeclaration) recipient).resolveBinding().getKey();
		case ASTNode.FIELD_DECLARATION:
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) ((FieldDeclaration) recipient).fragments().get(0);
			return fragment.resolveBinding().getKey();
		case ASTNode.METHOD_DECLARATION:
			return ((MethodDeclaration) recipient).resolveBinding().getKey();
		case ASTNode.MODULE_DECLARATION:
			return ((ModuleDeclaration) recipient).resolveBinding().getKey();
		case ASTNode.VARIABLE_DECLARATION_STATEMENT:
			fragment = (VariableDeclarationFragment) ((VariableDeclarationStatement) recipient).fragments().get(0);
			return fragment.resolveBinding().getKey();
		default:
			return ""; //$NON-NLS-1$
		}
	}

	@Override
	public int getKind() {
		return IBinding.ANNOTATION;
	}

	@Override
	public int getModifiers() {
		return Modifier.NONE;
	}

	@Override
	public String getName() {
		ITypeBinding annotationType = getAnnotationType();
		if (annotationType == null) {
			return new String(this.binding.getAnnotationType().sourceName());
		} else {
			return annotationType.getName();
		}
	}

	@Override
	public boolean isDeprecated() {
		ReferenceBinding typeBinding = this.binding.getAnnotationType();
		if (typeBinding == null) return false;
		return typeBinding.isDeprecated();
	}

	@Override
	public boolean isEqualTo(IBinding otherBinding) {
		if (this == otherBinding)
			return true;
		if (otherBinding.getKind() != IBinding.ANNOTATION)
			return false;
		IAnnotationBinding other = (IAnnotationBinding) otherBinding;
		if (!getAnnotationType().isEqualTo(other.getAnnotationType()))
			return false;
		IMemberValuePairBinding[] memberValuePairs = getDeclaredMemberValuePairs();
		IMemberValuePairBinding[] otherMemberValuePairs = other.getDeclaredMemberValuePairs();
		if (memberValuePairs.length != otherMemberValuePairs.length)
			return false;
		for (int i = 0, length = memberValuePairs.length; i < length; i++) {
			if (!memberValuePairs[i].isEqualTo(otherMemberValuePairs[i]))
				return false;
		}
		return true;
	}

	@Override
	public boolean isRecovered() {
        ReferenceBinding annotationType = this.binding.getAnnotationType();
        return annotationType == null || (annotationType.tagBits & TagBits.HasMissingType) != 0;	}

	@Override
	public boolean isSynthetic() {
		return false;
	}

	@Override
	public String toString() {
		ITypeBinding type = getAnnotationType();
		final StringBuilder buffer = new StringBuilder();
		buffer.append('@');
		if (type != null)
			buffer.append(type.getName());
		buffer.append('(');
		IMemberValuePairBinding[] pairs = getDeclaredMemberValuePairs();
		for (int i = 0, len = pairs.length; i < len; i++) {
			if (i != 0)
				buffer.append(", "); //$NON-NLS-1$
			buffer.append(pairs[i].toString());
		}
		buffer.append(')');
		return buffer.toString();
	}

}
