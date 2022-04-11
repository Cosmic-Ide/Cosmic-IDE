/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
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
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *                          Bug 409247 - [1.8][compiler] Verify error with code allocating multidimensional array
 *                          Bug 409517 - [1.8][compiler] Type annotation problems on more elaborate array references
 *                          Bug 409250 - [1.8][compiler] Various loose ends in 308 code generation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.codegen;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.ArrayAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class TypeAnnotationCodeStream extends StackMapFrameCodeStream {
	public List<AnnotationContext> allTypeAnnotationContexts;

	public TypeAnnotationCodeStream(ClassFile givenClassFile) {
		super(givenClassFile);
		this.generateAttributes |= ClassFileConstants.ATTR_TYPE_ANNOTATION;
		this.allTypeAnnotationContexts = new ArrayList<>();
	}

	private void addAnnotationContext(TypeReference typeReference, int info, int targetType, ArrayAllocationExpression allocationExpression) {
		allocationExpression.getAllAnnotationContexts(targetType, info, this.allTypeAnnotationContexts);
	}

	private void addAnnotationContext(TypeReference typeReference, int info, int targetType) {
		typeReference.getAllAnnotationContexts(targetType, info, this.allTypeAnnotationContexts);
	}

	private void addAnnotationContext(TypeReference typeReference, int info, int typeIndex, int targetType) {
		typeReference.getAllAnnotationContexts(targetType, info, typeIndex, this.allTypeAnnotationContexts);
	}

	@Override
	public void instance_of(TypeReference typeReference, TypeBinding typeBinding) {
		if (typeReference != null && (typeReference.bits & ASTNode.HasTypeAnnotations) != 0) {
			addAnnotationContext(typeReference, this.position, AnnotationTargetTypeConstants.INSTANCEOF);
		}
		super.instance_of(typeReference, typeBinding);
	}

	@Override
	public void multianewarray(
			TypeReference typeReference,
			TypeBinding typeBinding,
			int dimensions,
			ArrayAllocationExpression allocationExpression) {
		if (typeReference != null && (typeReference.bits & ASTNode.HasTypeAnnotations) != 0) {
			addAnnotationContext(typeReference, this.position, AnnotationTargetTypeConstants.NEW, allocationExpression);
		}
		super.multianewarray(typeReference, typeBinding, dimensions, allocationExpression);
	}

	@Override
	public void new_(TypeReference typeReference, TypeBinding typeBinding) {
		if (typeReference != null && (typeReference.bits & ASTNode.HasTypeAnnotations) != 0) {
			addAnnotationContext(typeReference, this.position, AnnotationTargetTypeConstants.NEW);
		}
		super.new_(typeReference, typeBinding);
	}

	@Override
	public void newArray(TypeReference typeReference, ArrayAllocationExpression allocationExpression, ArrayBinding arrayBinding) {
		if (typeReference != null && (typeReference.bits & ASTNode.HasTypeAnnotations) != 0) {
			addAnnotationContext(typeReference, this.position, AnnotationTargetTypeConstants.NEW, allocationExpression);
		}
		super.newArray(typeReference, allocationExpression, arrayBinding);
	}

	@Override
	public void checkcast(TypeReference typeReference, TypeBinding typeBinding, int currentPosition) {
		/* We use a slightly sub-optimal generation for intersection casts by resorting to a runtime cast for every intersecting type, but in
		   reality this should not matter. In its intended use form such as (I & Serializable) () -> {}, no cast is emitted at all. Also note
		   intersection cast type references cannot nest i.e ((X & I) & J) is not valid syntax.
		*/
		if (typeReference != null) {
			TypeReference [] typeReferences = typeReference.getTypeReferences();
			for (int i = typeReferences.length - 1; i >= 0; i--) {  // need to emit right to left.
				typeReference = typeReferences[i];
				if (typeReference != null) {
					if ((typeReference.bits & ASTNode.HasTypeAnnotations) != 0) {
						if (!typeReference.resolvedType.isBaseType()) {
							addAnnotationContext(typeReference, this.position, i, AnnotationTargetTypeConstants.CAST);
						} else {
							// for base type record it against the start position of the expression
							addAnnotationContext(typeReference, currentPosition, i, AnnotationTargetTypeConstants.CAST);
						}
					}
					if (!typeReference.resolvedType.isBaseType()) {
						super.checkcast(typeReference, typeReference.resolvedType, currentPosition);
					}
				}
			}
		} else {
			super.checkcast(null, typeBinding, currentPosition);
		}
	}

	@Override
	public void invoke(byte opcode, MethodBinding methodBinding, TypeBinding declaringClass, TypeReference[] typeArguments) {
		if (typeArguments != null) {
			int targetType = methodBinding.isConstructor()
					? AnnotationTargetTypeConstants.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT
					: AnnotationTargetTypeConstants.METHOD_INVOCATION_TYPE_ARGUMENT;
			for (int i = 0, max = typeArguments.length; i < max; i++) {
				TypeReference typeArgument = typeArguments[i];
				if ((typeArgument.bits & ASTNode.HasTypeAnnotations) != 0) {
					addAnnotationContext(typeArgument, this.position, i, targetType);
				}
			}
		}
		super.invoke(opcode, methodBinding, declaringClass, typeArguments);
	}

	@Override
	public void invokeDynamic(int bootStrapIndex, int argsSize, int returnTypeSize, char[] selector, char[] signature,
			boolean isConstructorReference, TypeReference lhsTypeReference, TypeReference [] typeArguments,
			int typeId, TypeBinding type) {
		if (lhsTypeReference != null && (lhsTypeReference.bits & ASTNode.HasTypeAnnotations) != 0) {
			if (isConstructorReference) {
				addAnnotationContext(lhsTypeReference, this.position, 0, AnnotationTargetTypeConstants.CONSTRUCTOR_REFERENCE);
			} else {
				addAnnotationContext(lhsTypeReference, this.position, 0, AnnotationTargetTypeConstants.METHOD_REFERENCE);
			}
		}
		if (typeArguments != null) {
			int targetType =
					isConstructorReference
					? AnnotationTargetTypeConstants.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
					: AnnotationTargetTypeConstants.METHOD_REFERENCE_TYPE_ARGUMENT;
			for (int i = 0, max = typeArguments.length; i < max; i++) {
				TypeReference typeArgument = typeArguments[i];
				if ((typeArgument.bits & ASTNode.HasTypeAnnotations) != 0) {
					addAnnotationContext(typeArgument, this.position, i, targetType);
				}
			}
		}
		super.invokeDynamic(bootStrapIndex, argsSize, returnTypeSize, selector, signature, isConstructorReference, lhsTypeReference, typeArguments, typeId, type);
	}

	@Override
	public void reset(ClassFile givenClassFile) {
		super.reset(givenClassFile);
		this.allTypeAnnotationContexts = new ArrayList<>();
	}

	@Override
	public void init(ClassFile targetClassFile) {
		super.init(targetClassFile);
		this.allTypeAnnotationContexts = new ArrayList<>();
	}
}
