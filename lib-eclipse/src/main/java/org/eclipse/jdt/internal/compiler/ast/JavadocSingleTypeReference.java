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

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;


public class JavadocSingleTypeReference extends SingleTypeReference implements IJavadocTypeReference {

	public int tagSourceStart, tagSourceEnd;
	public PackageBinding packageBinding;
	public ModuleBinding moduleBinding;
	private boolean canBeModule;

	public JavadocSingleTypeReference(char[] source, long pos, int tagStart, int tagEnd) {
		this(source, pos, tagStart, tagEnd, false);
	}

	public JavadocSingleTypeReference(char[] source, long pos, int tagStart, int tagEnd, boolean canBeModule) {
		super(source, pos);
		this.tagSourceStart = tagStart;
		this.tagSourceEnd = tagEnd;
		this.bits |= ASTNode.InsideJavadoc;
		this.canBeModule = canBeModule;
	}

	/*
	 * We need to modify resolving behavior to handle package references
	 */
	@Override
	protected TypeBinding internalResolveType(Scope scope, int location) {
		// handle the error here
		this.constant = Constant.NotAConstant;
		if (this.resolvedType != null) { // is a shared type reference which was already resolved
			if (this.resolvedType.isValidBinding()) {
				return this.resolvedType;
			} else {
				switch (this.resolvedType.problemId()) {
					case ProblemReasons.NotFound :
					case ProblemReasons.NotVisible :
					case ProblemReasons.InheritedNameHidesEnclosingName :
						TypeBinding type = this.resolvedType.closestMatch();
						return type;
					default :
						return null;
				}
			}
		}
		this.resolvedType = getTypeBinding(scope);
		if (this.resolvedType instanceof LocalTypeBinding) {
			// scope grants access to local types within this method, which, however, are illegal in javadoc
			LocalTypeBinding localType = (LocalTypeBinding) this.resolvedType;
			if (localType.scope != null && localType.scope.parent == scope) {
				this.resolvedType = new ProblemReferenceBinding(new char[][] { localType.sourceName },
						(ReferenceBinding) this.resolvedType, ProblemReasons.NotFound);
			}
		}
		// End resolution when getTypeBinding(scope) returns null. This may happen in
		// certain circumstances, typically when an illegal access is done on a type
		// variable (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=204749)
		if (this.resolvedType == null) return null;

		if (!this.resolvedType.isValidBinding()) {
			char[][] tokens = { this.token };
			Binding binding = scope.getTypeOrPackage(tokens);
			if (binding instanceof PackageBinding) {
				this.packageBinding = (PackageBinding) binding;
				// Valid package references are allowed in Javadoc (https://bugs.eclipse.org/bugs/show_bug.cgi?id=281609)
			} else {
				Binding modBinding = null;
				if (this.canBeModule) {
					modBinding = scope.environment().getModule(this.token);
				}
				if (modBinding instanceof ModuleBinding
						&& !((ModuleBinding)modBinding).isUnnamed()
						&& modBinding.isValidBinding()) {
					this.moduleBinding = (ModuleBinding) modBinding;
				} else {
					if (this.resolvedType.problemId() == ProblemReasons.NonStaticReferenceInStaticContext) {
						TypeBinding closestMatch = this.resolvedType.closestMatch();
						if (closestMatch != null && closestMatch.isTypeVariable()) {
							this.resolvedType = closestMatch; // ignore problem as we want report specific javadoc one instead
							return this.resolvedType;
						}
					}
					reportInvalidType(scope);
				}
			}
			return null;
		}
		if (isTypeUseDeprecated(this.resolvedType, scope))
			reportDeprecatedType(this.resolvedType, scope);
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=209936
		// raw convert all enclosing types when dealing with Javadoc references
		if (this.resolvedType.isGenericType() || this.resolvedType.isParameterizedType()) {
			this.resolvedType = scope.environment().convertToRawType(this.resolvedType, true /*force the conversion of enclosing types*/);
		}
		return this.resolvedType;
	}
	@Override
	protected void reportDeprecatedType(TypeBinding type, Scope scope) {
		scope.problemReporter().javadocDeprecatedType(type, this, scope.getDeclarationModifiers());
	}

	@Override
	protected void reportInvalidType(Scope scope) {
		scope.problemReporter().javadocInvalidType(this, this.resolvedType, scope.getDeclarationModifiers());
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
