/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;


public class JavadocQualifiedTypeReference extends QualifiedTypeReference implements IJavadocTypeReference {

	public int tagSourceStart, tagSourceEnd;
	public PackageBinding packageBinding;
	public ModuleBinding moduleBinding;
	private boolean canBeModule;

	public JavadocQualifiedTypeReference(char[][] sources, long[] pos, int tagStart, int tagEnd) {
		this(sources, pos, tagStart, tagEnd, false);
	}

	public JavadocQualifiedTypeReference(char[][] sources, long[] pos, int tagStart, int tagEnd, boolean canBeModule) {
		super(sources, pos);
		this.tagSourceStart = tagStart;
		this.tagSourceEnd = tagEnd;
		this.bits |= ASTNode.InsideJavadoc;
		this.canBeModule = canBeModule;
	}

	/*
	 * We need to modify resolving behavior to handle package references
	 */
	private TypeBinding internalResolveType(Scope scope, boolean checkBounds) {
		// handle the error here
		this.constant = Constant.NotAConstant;
		if (this.resolvedType != null) // is a shared type reference which was already resolved
			return this.resolvedType.isValidBinding() ? this.resolvedType : this.resolvedType.closestMatch(); // already reported error

		TypeBinding type = this.resolvedType = getTypeBinding(scope);
		// End resolution when getTypeBinding(scope) returns null. This may happen in
		// certain circumstances, typically when an illegal access is done on a type
		// variable (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=204749)
		if (type == null) return null;
		if (!type.isValidBinding()) {
			Binding binding = scope.getTypeOrPackage(this.tokens);
			if (binding instanceof PackageBinding) {
				this.packageBinding = (PackageBinding) binding;
				// Valid package references are allowed in Javadoc (https://bugs.eclipse.org/bugs/show_bug.cgi?id=281609)
			} else {
				Binding modBinding = null;
				if (this.canBeModule) {
					char[] moduleName = CharOperation.concatWith(this.tokens, '.');
					modBinding = scope.environment().getModule(moduleName);
				}
				if (modBinding instanceof ModuleBinding
						&& !((ModuleBinding)modBinding).isUnnamed()
						&& modBinding.isValidBinding()) {
					this.moduleBinding = (ModuleBinding) modBinding;
				} else {
					reportInvalidType(scope);
				}
			}
			return null;
		}
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=209936
		// raw convert all enclosing types when dealing with Javadoc references
		if (type.isGenericType() || type.isParameterizedType()) {
			this.resolvedType = scope.environment().convertToRawType(type, true /*force the conversion of enclosing types*/);
		}
		return this.resolvedType;
	}
	@Override
	protected void reportDeprecatedType(TypeBinding type, Scope scope) {
		scope.problemReporter().javadocDeprecatedType(type, this, scope.getDeclarationModifiers());
	}

	@Override
	protected void reportDeprecatedType(TypeBinding type, Scope scope, int index) {
		scope.problemReporter().javadocDeprecatedType(type, this, scope.getDeclarationModifiers(), index);
	}

	@Override
	protected void reportInvalidType(Scope scope) {
		scope.problemReporter().javadocInvalidType(this, this.resolvedType, scope.getDeclarationModifiers());
	}
	@Override
	public TypeBinding resolveType(BlockScope blockScope, boolean checkBounds, int location) {
		return internalResolveType(blockScope, checkBounds);
	}

	@Override
	public TypeBinding resolveType(ClassScope classScope, int location) {
		return internalResolveType(classScope, false);
	}

	/* (non-Javadoc)
	 * Redefine to capture javadoc specific signatures
	 * @see org.eclipse.jdt.internal.compiler.ast.ASTNode#traverse(org.eclipse.jdt.internal.compiler.ASTVisitor, org.eclipse.jdt.internal.compiler.lookup.BlockScope)
	 */
	@Override
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}

	@Override
	public void traverse(ASTVisitor visitor, ClassScope scope) {
		visitor.visit(this, scope);
		visitor.endVisit(this, scope);
	}

	@Override
	public int getTagSourceStart() {
		return this.tagSourceStart;
	}

	@Override
	public int getTagSourceEnd() {
		return this.tagSourceEnd;
	}
}
