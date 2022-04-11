/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.jdt.core.dom;

import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.eclipse.jdt.internal.core.PackageFragment;

/**
 * This class represents the recovered binding for a type
 */
@SuppressWarnings("rawtypes")
class RecoveredTypeBinding implements ITypeBinding {

	private VariableDeclaration variableDeclaration;
	private Type currentType;
	private BindingResolver resolver;
	private int dimensions;
	private RecoveredTypeBinding innerTypeBinding;
	private ITypeBinding[] typeArguments;
	private org.eclipse.jdt.internal.compiler.lookup.TypeBinding binding;

	RecoveredTypeBinding(BindingResolver resolver, VariableDeclaration variableDeclaration) {
		this.variableDeclaration = variableDeclaration;
		this.resolver = resolver;
		this.currentType = getType();
		this.dimensions = variableDeclaration.getExtraDimensions();
		if (this.currentType.isArrayType()) {
			this.dimensions += ((ArrayType) this.currentType).getDimensions();
		}
	}

	RecoveredTypeBinding(BindingResolver resolver, org.eclipse.jdt.internal.compiler.lookup.TypeBinding typeBinding) {
		this.resolver = resolver;
		this.dimensions = typeBinding.dimensions();
		this.binding = typeBinding;
	}

	RecoveredTypeBinding(BindingResolver resolver, Type type) {
		this.currentType = type;
		this.resolver = resolver;
		this.dimensions = 0;
		if (type.isArrayType()) {
			this.dimensions += ((ArrayType) type).getDimensions();
		}
	}

	RecoveredTypeBinding(BindingResolver resolver, RecoveredTypeBinding typeBinding, int dimensions) {
		this.innerTypeBinding = typeBinding;
		this.dimensions = typeBinding.getDimensions() + dimensions;
		this.resolver = resolver;
	}

	@Override
	public ITypeBinding createArrayType(int dims) {
		return this.resolver.getTypeBinding(this, dims);
	}

	@Override
	public String getBinaryName() {
		return null;
	}

	@Override
	public ITypeBinding getBound() {
		return null;
	}

	@Override
	public ITypeBinding getGenericTypeOfWildcardType() {
		return null;
	}

	@Override
	public int getRank() {
		return -1;
	}

	@Override
	public ITypeBinding getComponentType() {
		if (this.dimensions == 0) return null;
		return this.resolver.getTypeBinding(this, -1);
	}

	@Override
	public IVariableBinding[] getDeclaredFields() {
		return TypeBinding.NO_VARIABLE_BINDINGS;
	}

	@Override
	public IMethodBinding[] getDeclaredMethods() {
		return TypeBinding.NO_METHOD_BINDINGS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ITypeBinding#getDeclaredModifiers()
	 * @deprecated Use ITypeBinding#getModifiers() instead
	 */
	@Override
	public int getDeclaredModifiers() {
		return 0;
	}

	@Override
	public ITypeBinding[] getDeclaredTypes() {
		return TypeBinding.NO_TYPE_BINDINGS;
	}

	@Override
	public ITypeBinding getDeclaringClass() {
		return null;
	}

	@Override
	public IMethodBinding getDeclaringMethod() {
		return null;
	}

	@Override
	public IBinding getDeclaringMember() {
		return null;
	}

	@Override
	public int getDimensions() {
		return this.dimensions;
	}

	@Override
	public ITypeBinding getElementType() {
		if (this.binding != null) {
			if (this.binding.isArrayType()) {
				ArrayBinding arrayBinding = (ArrayBinding) this.binding;
				return new RecoveredTypeBinding(this.resolver, arrayBinding.leafComponentType);
			} else {
				return new RecoveredTypeBinding(this.resolver, this.binding);
			}
		}
		if (this.innerTypeBinding != null) {
			return this.innerTypeBinding.getElementType();
		}
		if (this.currentType!= null && this.currentType.isArrayType()) {
			return this.resolver.getTypeBinding(((ArrayType) this.currentType).getElementType());
		}
		if (this.variableDeclaration != null && this.variableDeclaration.getExtraDimensions() != 0) {
			return this.resolver.getTypeBinding(getType());
		}
		return null;
	}

	@Override
	public ITypeBinding getErasure() {
		return this;
	}

	@Override
	public IMethodBinding getFunctionalInterfaceMethod() {
		return null;
	}

	@Override
	public ITypeBinding[] getInterfaces() {
		return TypeBinding.NO_TYPE_BINDINGS;
	}

	@Override
	public int getModifiers() {
		return Modifier.NONE;
	}

	@Override
	public String getName() {
		char[] brackets = new char[this.dimensions * 2];
		for (int i = this.dimensions * 2 - 1; i >= 0; i -= 2) {
			brackets[i] = ']';
			brackets[i - 1] = '[';
		}
		StringBuffer buffer = new StringBuffer(getInternalName());
		buffer.append(brackets);
		return String.valueOf(buffer);
	}

	private String getInternalName() {
		if (this.innerTypeBinding != null) {
			return this.innerTypeBinding.getInternalName();
		}
		ReferenceBinding referenceBinding = getReferenceBinding();
		if (referenceBinding != null) {
			return new String(referenceBinding.compoundName[referenceBinding.compoundName.length - 1]);
		}
		return getTypeNameFrom(getType());
	}

	@Override
	public IModuleBinding getModule() {
		if (this.binding != null) {
			switch (this.binding.kind()) {
				case Binding.BASE_TYPE :
				case Binding.ARRAY_TYPE :
				case Binding.TYPE_PARAMETER : // includes capture scenario
				case Binding.WILDCARD_TYPE :
				case Binding.INTERSECTION_TYPE:
					return null;
			}
			return getModule(this.binding.getPackage());
		}
		CompilationUnitScope scope = this.resolver.scope();
		return scope != null ? getModule(scope.getCurrentPackage()) : null;
	}

	private IModuleBinding getModule(PackageBinding pBinding) {
		IPackageBinding packageBinding = this.resolver.getPackageBinding(pBinding);
		return packageBinding != null ? packageBinding.getModule() : null;
	}

	@Override
	public IPackageBinding getPackage() {
		if (this.binding != null) {
			switch (this.binding.kind()) {
				case Binding.BASE_TYPE :
				case Binding.ARRAY_TYPE :
				case Binding.TYPE_PARAMETER : // includes capture scenario
				case Binding.WILDCARD_TYPE :
				case Binding.INTERSECTION_TYPE:
					return null;
			}
			IPackageBinding packageBinding = this.resolver.getPackageBinding(this.binding.getPackage());
			if (packageBinding != null) return packageBinding;
		}
		if (this.innerTypeBinding != null && this.dimensions > 0) {
			return null;
		}
		CompilationUnitScope scope = this.resolver.scope();
		if (scope != null) {
			return this.resolver.getPackageBinding(scope.getCurrentPackage());
		}
		return null;
	}

	@Override
	public String getQualifiedName() {
		ReferenceBinding referenceBinding = getReferenceBinding();
		if (referenceBinding != null) {
			StringBuffer buffer = new StringBuffer();
			char[] brackets = new char[this.dimensions * 2];
			for (int i = this.dimensions * 2 - 1; i >= 0; i -= 2) {
				brackets[i] = ']';
				brackets[i - 1] = '[';
			}
			buffer.append(CharOperation.toString(referenceBinding.compoundName));
			buffer.append(brackets);
			return String.valueOf(buffer);
		} else {
			return getName();
		}
	}

	private ReferenceBinding getReferenceBinding() {
		if (this.binding != null) {
			if (this.binding.isArrayType()) {
				ArrayBinding arrayBinding = (ArrayBinding) this.binding;
				if (arrayBinding.leafComponentType instanceof ReferenceBinding) {
					return (ReferenceBinding) arrayBinding.leafComponentType;
				}
			} else if (this.binding instanceof ReferenceBinding) {
				return (ReferenceBinding) this.binding;
			}
		} else if (this.innerTypeBinding != null) {
			return this.innerTypeBinding.getReferenceBinding();
		}
		return null;
	}

	@Override
	public ITypeBinding getSuperclass() {
		if (getQualifiedName().equals("java.lang.Object")) {	//$NON-NLS-1$
			return null;
		}
		return this.resolver.resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
	}

	@Override
	public ITypeBinding[] getTypeArguments() {
		if (this.binding != null) {
			return this.typeArguments = TypeBinding.NO_TYPE_BINDINGS;
		}
		if (this.typeArguments != null) {
			return this.typeArguments;
		}

		if (this.innerTypeBinding != null) {
			return this.innerTypeBinding.getTypeArguments();
		}

		if (this.currentType.isParameterizedType()) {
			ParameterizedType parameterizedType = (ParameterizedType) this.currentType;
			List typeArgumentsList = parameterizedType.typeArguments();
			int size = typeArgumentsList.size();
			ITypeBinding[] temp = new ITypeBinding[size];
			for (int i = 0; i < size; i++) {
				ITypeBinding currentTypeBinding = ((Type) typeArgumentsList.get(i)).resolveBinding();
				if (currentTypeBinding == null) {
					return this.typeArguments = TypeBinding.NO_TYPE_BINDINGS;
				}
				temp[i] = currentTypeBinding;
			}
			return this.typeArguments = temp;
		}
		return this.typeArguments = TypeBinding.NO_TYPE_BINDINGS;
	}

	@Override
	public ITypeBinding[] getTypeBounds() {
		return TypeBinding.NO_TYPE_BINDINGS;
	}

	@Override
	public ITypeBinding getTypeDeclaration() {
		return this;
	}

	@Override
	public ITypeBinding[] getTypeParameters() {
		return TypeBinding.NO_TYPE_BINDINGS;
	}

	@Override
	public ITypeBinding getWildcard() {
		return null;
	}

	@Override
	public boolean isAnnotation() {
		return false;
	}

	@Override
	public boolean isAnonymous() {
		return false;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public boolean isAssignmentCompatible(ITypeBinding typeBinding) {
		if ("java.lang.Object".equals(typeBinding.getQualifiedName())) { //$NON-NLS-1$
			return true;
		}
		// since recovered binding are not unique isEqualTo is required
		return isEqualTo(typeBinding);
	}

	@Override
	public boolean isCapture() {
		return false;
	}

	@Override
	public boolean isCastCompatible(ITypeBinding typeBinding) {
		if ("java.lang.Object".equals(typeBinding.getQualifiedName())) { //$NON-NLS-1$
			return true;
		}
		// since recovered binding are not unique isEqualTo is required
		return isEqualTo(typeBinding);
	}

	@Override
	public boolean isClass() {
		return true;
	}

	@Override
	public boolean isEnum() {
		return false;
	}

	@Override
	public boolean isRecord() {
		return false;
	}

	@Override
	public boolean isFromSource() {
		return false;
	}

	@Override
	public boolean isGenericType() {
		return false;
	}

	@Override
	public boolean isInterface() {
		return false;
	}

	@Override
	public boolean isIntersectionType() {
		return false;
	}

	@Override
	public boolean isLocal() {
		return false;
	}

	@Override
	public boolean isMember() {
		return false;
	}

	@Override
	public boolean isNested() {
		return false;
	}

	@Override
	public boolean isNullType() {
		return false;
	}

	@Override
	public boolean isParameterizedType() {
		if (this.innerTypeBinding != null) {
			return this.innerTypeBinding.isParameterizedType();
		}
		if (this.currentType != null) {
			return this.currentType.isParameterizedType();
		}
		return false;
	}

	@Override
	public boolean isPrimitive() {
		return false;
	}

	@Override
	public boolean isRawType() {
		return false;
	}

	@Override
	public boolean isSubTypeCompatible(ITypeBinding typeBinding) {
		if ("java.lang.Object".equals(typeBinding.getQualifiedName())) { //$NON-NLS-1$
			return true;
		}
		// since recovered binding are not unique isEqualTo is required
		return isEqualTo(typeBinding);
	}

	@Override
	public boolean isTopLevel() {
		return true;
	}

	@Override
	public boolean isTypeVariable() {
		return false;
	}

	@Override
	public boolean isUpperbound() {
		return false;
	}

	@Override
	public boolean isWildcardType() {
		return false;
	}

	@Override
	public IAnnotationBinding[] getAnnotations() {
		return AnnotationBinding.NoAnnotations;
	}

	@Override
	public IJavaElement getJavaElement() {
		IPackageBinding packageBinding = getPackage();
		if (packageBinding != null) {
			final IJavaElement javaElement = packageBinding.getJavaElement();
			if (javaElement != null && javaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
				// best effort: we don't know if the recovered binding is a binary or source binding, so go with a simple source type
				return ((PackageFragment) javaElement).getCompilationUnit(getInternalName() + SuffixConstants.SUFFIX_STRING_java).getType(this.getName());
			}
		}
		return null;
	}

	@Override
	public String getKey() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Recovered#"); //$NON-NLS-1$
		if (this.innerTypeBinding != null) {
			buffer.append("innerTypeBinding") //$NON-NLS-1$
			      .append(this.innerTypeBinding.getKey());
		} else if (this.currentType != null) {
			buffer.append("currentType") //$NON-NLS-1$
			      .append(this.currentType.toString());
		} else if (this.binding != null) {
			buffer.append("typeBinding") //$NON-NLS-1$
				  .append(this.binding.computeUniqueKey());
		} else if (this.variableDeclaration != null) {
			buffer
				.append("variableDeclaration") //$NON-NLS-1$
				.append(this.variableDeclaration.getClass())
				.append(this.variableDeclaration.getName().getIdentifier())
				.append(this.variableDeclaration.getExtraDimensions());
		}
		buffer.append(getDimensions());
		if (this.typeArguments != null) {
			buffer.append('<');
			for (int i = 0, max = this.typeArguments.length; i < max; i++) {
				if (i != 0) {
					buffer.append(',');
				}
				buffer.append(this.typeArguments[i].getKey());
			}
			buffer.append('>');
		}
		return String.valueOf(buffer);
	}

	@Override
	public int getKind() {
		return IBinding.TYPE;
	}

	@Override
	public boolean isDeprecated() {
		return false;
	}

	@Override
	public boolean isEqualTo(IBinding other) {
		if (!other.isRecovered() || other.getKind() != IBinding.TYPE) return false;
		return getKey().equals(other.getKey());
	}

	@Override
	public boolean isRecovered() {
		return true;
	}

	@Override
	public boolean isSynthetic() {
		return false;
	}

	private String getTypeNameFrom(Type type) {
		if (type == null) return Util.EMPTY_STRING;
		switch(type.getNodeType0()) {
			case ASTNode.ARRAY_TYPE :
				ArrayType arrayType = (ArrayType) type;
				type = arrayType.getElementType();
				return getTypeNameFrom(type);
			case ASTNode.PARAMETERIZED_TYPE :
				ParameterizedType parameterizedType = (ParameterizedType) type;
				StringBuffer buffer = new StringBuffer(getTypeNameFrom(parameterizedType.getType()));
				ITypeBinding[] tArguments = getTypeArguments();
				final int typeArgumentsLength = tArguments.length;
				if (typeArgumentsLength != 0) {
					buffer.append('<');
					for (int i = 0; i < typeArgumentsLength; i++) {
						if (i > 0) {
							buffer.append(',');
						}
						buffer.append(tArguments[i].getName());
					}
					buffer.append('>');
				}
				return String.valueOf(buffer);
			case ASTNode.PRIMITIVE_TYPE :
				PrimitiveType primitiveType = (PrimitiveType) type;
				return primitiveType.getPrimitiveTypeCode().toString();
			case ASTNode.QUALIFIED_TYPE :
				QualifiedType qualifiedType = (QualifiedType) type;
				return qualifiedType.getName().getIdentifier();
			case ASTNode.NAME_QUALIFIED_TYPE :
				NameQualifiedType nameQualifiedType = (NameQualifiedType) type;
				return nameQualifiedType.getName().getIdentifier();
			case ASTNode.SIMPLE_TYPE :
				SimpleType simpleType = (SimpleType) type;
				Name name = simpleType.getName();
				if (name.isQualifiedName()) {
					QualifiedName qualifiedName = (QualifiedName) name;
					return qualifiedName.getName().getIdentifier();
				}
				return ((SimpleName) name).getIdentifier();
		}
		return Util.EMPTY_STRING;
	}

	private Type getType() {
		if (this.currentType != null) {
			return this.currentType;
		}
		if (this.variableDeclaration == null) return null;
		switch(this.variableDeclaration.getNodeType()) {
			case ASTNode.SINGLE_VARIABLE_DECLARATION :
				SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) this.variableDeclaration;
				return singleVariableDeclaration.getType();
			default :
				// this is a variable declaration fragment
				ASTNode parent = this.variableDeclaration.getParent();
				switch(parent.getNodeType()) {
					case ASTNode.VARIABLE_DECLARATION_EXPRESSION :
						VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression) parent;
						return variableDeclarationExpression.getType();
					case ASTNode.VARIABLE_DECLARATION_STATEMENT :
						VariableDeclarationStatement statement = (VariableDeclarationStatement) parent;
						return statement.getType();
					case ASTNode.FIELD_DECLARATION :
						FieldDeclaration fieldDeclaration  = (FieldDeclaration) parent;
						return fieldDeclaration.getType();
				}
		}
		return null; // should not happen
	}

	@Override
	public IAnnotationBinding[] getTypeAnnotations() {
		return AnnotationBinding.NoAnnotations;
	}
}
