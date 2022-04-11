/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;


public class JavadocModuleReference extends Expression implements IJavadocTypeReference {

	public int tagSourceStart, tagSourceEnd;
	public TypeReference typeReference;
	public ModuleReference moduleReference;
	public ModuleBinding moduleBinding;

	public JavadocModuleReference(char[][] sources, long[] pos, int tagStart, int tagEnd) {
		super();
		this.moduleReference = new ModuleReference(sources, pos);
		this.tagSourceStart = tagStart;
		this.tagSourceEnd = tagEnd;
		this.sourceStart = this.moduleReference.sourceStart;
		this.sourceEnd = this.moduleReference.sourceEnd;
		this.bits |= ASTNode.InsideJavadoc;
	}

	public JavadocModuleReference(ModuleReference moduleRef, int tagStart, int tagEnd) {
		super();
		this.moduleReference = moduleRef;
		this.tagSourceStart = tagStart;
		this.tagSourceEnd = tagEnd;
		this.sourceStart = this.moduleReference.sourceStart;
		this.sourceEnd = this.moduleReference.sourceEnd;
		this.bits |= ASTNode.InsideJavadoc;
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
	public int getTagSourceStart() {
		return this.tagSourceStart;
	}

	@Override
	public int getTagSourceEnd() {
		return this.tagSourceEnd;
	}

	public TypeReference getTypeReference() {
		return this.typeReference;
	}

	public void setTypeReference(TypeReference typeReference) {
		this.typeReference = typeReference;
		if (this.typeReference != null) {
			this.sourceEnd = this.typeReference.sourceEnd;
		}
	}

	public ModuleReference getModuleReference() {
		return this.moduleReference;
	}

	public void setModuleReference(ModuleReference moduleReference) {
		this.moduleReference = moduleReference;
		this.sourceStart = this.moduleReference.sourceStart;
		this.sourceStart = this.moduleReference.sourceEnd;
	}

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output) {
		if (this.moduleReference != null) {
			output.append(this.moduleReference.moduleName);
		}
		output.append('/');
		if (this.typeReference != null) {
			this.typeReference.printExpression(indent, output);
		}
		return output;
	}

	public ModuleBinding resolve(Scope scope) {
		ModuleBinding modBinding = this.moduleReference.resolve(scope);
		if (modBinding != null
				&& !modBinding.isUnnamed()
				&& modBinding.isValidBinding()) {
			this.moduleBinding = modBinding;
		} else {
			reportInvalidModule(scope);
		}
		return this.moduleBinding;
	}

	private ModuleBinding resolveModule(BlockScope scope) {
		return this.resolve((Scope)scope);
	}

	private ModuleBinding resolveModule(ClassScope scope) {
		return this.resolve(scope);
	}

	@Override
	public TypeBinding resolveType(BlockScope blockScope) {
		this.resolveModule(blockScope);
		TypeBinding tBinding= null;
		if (this.moduleBinding != null
				&& this.typeReference != null) {
			tBinding = this.typeReference.resolveType(blockScope);
			PackageBinding pBinding = null;
			if (tBinding!= null) {
				if (tBinding.isValidBinding()) {
					pBinding = tBinding.getPackage();
				} else {
					return tBinding;
				}
			} else {
				if(this.typeReference.resolvedType != null && !this.typeReference.resolvedType.isValidBinding()) {
					if (this.typeReference instanceof JavadocSingleTypeReference) {
						pBinding = ((JavadocSingleTypeReference)this.typeReference).packageBinding;
					} else if (this.typeReference instanceof JavadocQualifiedTypeReference) {
						pBinding = ((JavadocQualifiedTypeReference)this.typeReference).packageBinding;
					}
				}
			}
			if (pBinding != null && !this.moduleBinding.equals(pBinding.enclosingModule)) {
				reportInvalidType(blockScope);
				tBinding = null;
			}
		}
		return tBinding;
	}

	@Override
	public TypeBinding resolveType(ClassScope classScope) {
		this.resolveModule(classScope);
		TypeBinding tBinding= null;
		if (this.moduleBinding != null
				&& this.typeReference != null) {
			tBinding =  this.typeReference.resolveType(classScope, -1);
			PackageBinding pBinding = null;
			if (tBinding!= null) {
				if (tBinding.isValidBinding()) {
					pBinding = tBinding.getPackage();
				} else {
					return tBinding;
				}
			} else {
				if(this.resolvedType != null && !this.resolvedType.isValidBinding()) {
					if (this.typeReference instanceof JavadocSingleTypeReference) {
						pBinding = ((JavadocSingleTypeReference)this.typeReference).packageBinding;
					} else if (this.typeReference instanceof JavadocQualifiedTypeReference) {
						pBinding = ((JavadocQualifiedTypeReference)this.typeReference).packageBinding;
					}
				}
			}
			if (pBinding != null && !this.moduleBinding.equals(pBinding.enclosingModule)) {
				reportInvalidType(classScope);
				tBinding = null;
			}
		}
		return tBinding;
	}

	protected void reportInvalidModule(Scope scope) {
		scope.problemReporter().javadocInvalidModule(this.moduleReference);
	}

	protected void reportInvalidType(Scope scope) {
		scope.problemReporter().javadocInvalidMemberTypeQualification(this.typeReference.sourceStart, this.typeReference.sourceEnd, scope.getDeclarationModifiers());
	}
}