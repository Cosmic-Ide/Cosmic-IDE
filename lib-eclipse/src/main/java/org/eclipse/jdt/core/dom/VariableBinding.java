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
 *     Stephan Herrmann - Contribution for
 *								Bug 429813 - [1.8][dom ast] IMethodBinding#getJavaElement() should return IMethod for lambda
 *								Bug 466308 - [hovering] Javadoc header for parameter is wrong with annotation-based null analysis
 *******************************************************************************/

package org.eclipse.jdt.core.dom;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.util.IModifierConstants;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.RecordComponentBinding;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.core.LocalVariable;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * Internal implementation of variable bindings.
 */
class VariableBinding implements IVariableBinding {

	private static final int VALID_MODIFIERS = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE |
		Modifier.STATIC | Modifier.FINAL | Modifier.TRANSIENT | Modifier.VOLATILE;

	private org.eclipse.jdt.internal.compiler.lookup.VariableBinding binding;
	private ITypeBinding declaringClass;
	private String key;
	private String name;
	private BindingResolver resolver;
	private ITypeBinding type;
	private IAnnotationBinding[] annotations;

	VariableBinding(BindingResolver resolver, org.eclipse.jdt.internal.compiler.lookup.VariableBinding binding) {
		this.resolver = resolver;
		this.binding = binding;
	}

	@Override
	public IAnnotationBinding[] getAnnotations() {
		if (this.annotations != null) {
			return this.annotations;
		}
		org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding[] internalAnnotations = this.binding.getAnnotations();
		int length = internalAnnotations == null ? 0 : internalAnnotations.length;
		if (length != 0) {
			IAnnotationBinding[] tempAnnotations = new IAnnotationBinding[length];
			int convertedAnnotationCount = 0;
			for (int i = 0; i < length; i++) {
				org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding internalAnnotation = internalAnnotations[i];
				final IAnnotationBinding annotationInstance = this.resolver.getAnnotationInstance(internalAnnotation);
				if (annotationInstance == null) {
					continue;
				}
				tempAnnotations[convertedAnnotationCount++] = annotationInstance;
			}
			if (convertedAnnotationCount != length) {
				if (convertedAnnotationCount == 0) {
					return this.annotations = AnnotationBinding.NoAnnotations;
				}
				System.arraycopy(tempAnnotations, 0, (tempAnnotations = new IAnnotationBinding[convertedAnnotationCount]), 0, convertedAnnotationCount);
			}
			return this.annotations = tempAnnotations;
		}
		return this.annotations = AnnotationBinding.NoAnnotations;
	}

	@Override
	public Object getConstantValue() {
		Constant c = this.binding.constant();
		if (c == null || c == Constant.NotAConstant) return null;
		switch (c.typeID()) {
			case TypeIds.T_boolean:
				return Boolean.valueOf(c.booleanValue());
			case TypeIds.T_byte:
				return Byte.valueOf(c.byteValue());
			case TypeIds.T_char:
				return Character.valueOf(c.charValue());
			case TypeIds.T_double:
				return Double.valueOf(c.doubleValue());
			case TypeIds.T_float:
				return Float.valueOf(c.floatValue());
			case TypeIds.T_int:
				return Integer.valueOf(c.intValue());
			case TypeIds.T_long:
				return Long.valueOf(c.longValue());
			case TypeIds.T_short:
				return Short.valueOf(c.shortValue());
			case TypeIds.T_JavaLangString:
				return c.stringValue();
		}
		return null;
	}

	@Override
	public ITypeBinding getDeclaringClass() {
		if (isField()) {
			if (this.declaringClass == null) {
				FieldBinding fieldBinding = (FieldBinding) this.binding;
				this.declaringClass = this.resolver.getTypeBinding(fieldBinding.declaringClass);
			}
			return this.declaringClass;
		} else {
			return null;
		}
	}

	@Override
	public IMethodBinding getDeclaringMethod() {
		if (!isField()) {
			ASTNode node = this.resolver.findDeclaringNode(this);
			while (true) {
				if (node == null) {
					if (this.binding instanceof LocalVariableBinding) {
						LocalVariableBinding localVariableBinding = (LocalVariableBinding) this.binding;
						org.eclipse.jdt.internal.compiler.lookup.MethodBinding enclosingMethod = localVariableBinding.getEnclosingMethod();
						if (enclosingMethod != null)
							return this.resolver.getMethodBinding(enclosingMethod);
					}
					return null;
				}
				switch(node.getNodeType()) {
					case ASTNode.INITIALIZER :
						return null;
					case ASTNode.METHOD_DECLARATION :
						MethodDeclaration methodDeclaration = (MethodDeclaration) node;
						return methodDeclaration.resolveBinding();
					case ASTNode.LAMBDA_EXPRESSION :
						LambdaExpression lambdaExpression = (LambdaExpression) node;
						return lambdaExpression.resolveMethodBinding();
					default:
						node = node.getParent();
				}
			}
		}
		return null;
	}

	@Override
	public IJavaElement getJavaElement() {
		JavaElement element = getUnresolvedJavaElement();
		if (element == null)
			return null;
		return element.resolved(this.binding);
	}

	@Override
	public String getKey() {
		if (this.key == null) {
			this.key = new String(this.binding.computeUniqueKey());
		}
		return this.key;
	}

	@Override
	public int getKind() {
		return IBinding.VARIABLE;
	}

	@Override
	public int getModifiers() {
		if (isField()) {
			return ((FieldBinding) this.binding).getAccessFlags() & VALID_MODIFIERS;
		}
		if (this.binding.isFinal()) {
			return IModifierConstants.ACC_FINAL;
		}
		return Modifier.NONE;
	}

	@Override
	public String getName() {
		if (this.name == null) {
			this.name = new String(this.binding.name);
		}
		return this.name;
	}

	@Override
	public ITypeBinding getType() {
		if (this.type == null) {
			this.type = this.resolver.getTypeBinding(this.binding.type);
		}
		return this.type;
	}

	private JavaElement getUnresolvedJavaElement() {
		if (JavaCore.getPlugin() == null) {
			return null;
		}
		if (isField()) {
			if (this.resolver instanceof DefaultBindingResolver) {
				DefaultBindingResolver defaultBindingResolver = (DefaultBindingResolver) this.resolver;
				if (!defaultBindingResolver.fromJavaProject) return null;
				return Util.getUnresolvedJavaElement(
						(FieldBinding) this.binding,
						defaultBindingResolver.workingCopyOwner,
						defaultBindingResolver.getBindingsToNodesMap());
			}
			return null;
		}else if (isRecordComponent()) {
			if (this.resolver instanceof DefaultBindingResolver) {
				DefaultBindingResolver defaultBindingResolver = (DefaultBindingResolver) this.resolver;
				if (!defaultBindingResolver.fromJavaProject) return null;
				return Util.getUnresolvedJavaElement(
						(RecordComponentBinding) this.binding,
						defaultBindingResolver.workingCopyOwner,
						defaultBindingResolver.getBindingsToNodesMap());
			}
			return null;
		}
		// local variable
		if (!(this.resolver instanceof DefaultBindingResolver)) return null;
		DefaultBindingResolver defaultBindingResolver = (DefaultBindingResolver) this.resolver;
		if (!defaultBindingResolver.fromJavaProject) return null;
		VariableDeclaration localVar = (VariableDeclaration) defaultBindingResolver.bindingsToAstNodes.get(this);
		if (localVar == null) return null;
		SimpleName localName = localVar.getName();
		int nameStart = localName.getStartPosition();
		int nameLength = localName.getLength();
		int sourceStart;
		int sourceLength;
		int modifiers = 0;
		if (localVar instanceof SingleVariableDeclaration) {
			sourceStart = localVar.getStartPosition();
			sourceLength = localVar.getLength();
			final SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) localVar;
			modifiers = singleVariableDeclaration.getModifiers();
		} else {
			ASTNode node = localVar.getParent();
			sourceStart = node.getStartPosition();
			sourceLength = node.getLength();
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) localVar;
			final ASTNode parent = fragment.getParent();
			switch (parent.getNodeType()) {
				case ASTNode.VARIABLE_DECLARATION_EXPRESSION :
					VariableDeclarationExpression expression = (VariableDeclarationExpression) parent;
					modifiers = expression.getModifiers();
					break;
				case ASTNode.VARIABLE_DECLARATION_STATEMENT :
					VariableDeclarationStatement statement = (VariableDeclarationStatement) parent;
					modifiers = statement.getModifiers();
					break;
				case ASTNode.FIELD_DECLARATION :
					FieldDeclaration fieldDeclaration = (FieldDeclaration) parent;
					modifiers = fieldDeclaration.getModifiers();
					break;
			}
		}
		int sourceEnd = sourceStart+sourceLength-1;
		char[] typeSig = this.binding.type.genericTypeSignature();
		JavaElement parent = null;
		IMethodBinding declaringMethod = getDeclaringMethod();
		if (this.binding instanceof RecordComponentBinding) {
			return null; // TODO : SEE Bug 562736/ BUG 562637
		}
		final LocalVariableBinding localVariableBinding = (LocalVariableBinding) this.binding;
		if (declaringMethod == null) {
			ReferenceContext referenceContext = localVariableBinding.declaringScope.referenceContext();
			if (referenceContext instanceof TypeDeclaration){
				// Local variable is declared inside an initializer
				TypeDeclaration typeDeclaration = (TypeDeclaration) referenceContext;
				JavaElement typeHandle = null;
				typeHandle = Util.getUnresolvedJavaElement(
					typeDeclaration.binding,
					defaultBindingResolver.workingCopyOwner,
					defaultBindingResolver.getBindingsToNodesMap());
				parent = Util.getUnresolvedJavaElement(sourceStart, sourceEnd, typeHandle);
			} else {
				return null;
			}
		} else {
			parent = (JavaElement) declaringMethod.getJavaElement();
		}
		if (parent == null) return null;
		return new LocalVariable(
				parent,
				localName.getIdentifier(),
				sourceStart,
				sourceEnd,
				nameStart,
				nameStart+nameLength-1,
				new String(typeSig),
				localVariableBinding.declaration.annotations,
				modifiers,
				(localVariableBinding.tagBits & TagBits.IsArgument) != 0);
	}

	@Override
	public IVariableBinding getVariableDeclaration() {
		if (isField()) {
			FieldBinding fieldBinding = (FieldBinding) this.binding;
			return this.resolver.getVariableBinding(fieldBinding.original());
		}
		return this;
	}

	@Override
	public int getVariableId() {
		return this.binding.id;
	}

	@Override
	public boolean isParameter() {
		return (this.binding.tagBits & TagBits.IsArgument) != 0;
	}

	@Override
	public boolean isDeprecated() {
		if (isField()) {
			return ((FieldBinding) this.binding).isDeprecated();
		}
		return false;
	}

	@Override
	public boolean isEnumConstant() {
		return (this.binding.modifiers & ClassFileConstants.AccEnum) != 0;
	}

	@Override
	public boolean isEqualTo(IBinding other) {
		if (other == this) {
			// identical binding - equal (key or no key)
			return true;
		}
		if (other == null) {
			// other binding missing
			return false;
		}
		if (!(other instanceof VariableBinding)) {
			return false;
		}
		org.eclipse.jdt.internal.compiler.lookup.VariableBinding otherBinding = ((VariableBinding) other).binding;
		if (this.binding instanceof FieldBinding) {
			if (otherBinding instanceof FieldBinding) {
				return BindingComparator.isEqual((FieldBinding) this.binding, (FieldBinding) otherBinding);
			} else {
				return false;
			}
		} else {
			if (BindingComparator.isEqual(this.binding, otherBinding)) {
				IMethodBinding declaringMethod = getDeclaringMethod();
				IMethodBinding otherDeclaringMethod = ((VariableBinding) other).getDeclaringMethod();
				if (declaringMethod == null) {
					if (otherDeclaringMethod != null) {
						return false;
					}
					return true;
				}
				return declaringMethod.isEqualTo(otherDeclaringMethod);
			}
			return false;
		}
	}

	@Override
	public boolean isField() {
		return this.binding instanceof FieldBinding;
	}

	@Override
	public boolean isSynthetic() {
		if (isField()) {
			return ((FieldBinding) this.binding).isSynthetic();
		}
		return false;
	}

	@Override
	public boolean isRecovered() {
		return false;
	}

	@Override
	public boolean isEffectivelyFinal() {
		return (!this.binding.isFinal() && this.binding.isEffectivelyFinal());
	}

	@Override
	public boolean isRecordComponent() {
		return this.binding instanceof RecordComponentBinding;
	}

	/*
	 * For debugging purpose only.
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.binding.toString();
	}

}
