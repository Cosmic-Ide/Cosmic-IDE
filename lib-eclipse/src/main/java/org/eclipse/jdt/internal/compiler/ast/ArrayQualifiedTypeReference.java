/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;

public class ArrayQualifiedTypeReference extends QualifiedTypeReference {
	int dimensions;
	private Annotation[][] annotationsOnDimensions;  // jsr308 style type annotations on dimensions
	public int extendedDimensions;

	public ArrayQualifiedTypeReference(char[][] sources , int dim, long[] poss) {

		super( sources , poss);
		this.dimensions = dim ;
		this.annotationsOnDimensions = null;
	}

	public ArrayQualifiedTypeReference(char[][] sources, int dim, Annotation[][] annotationsOnDimensions, long[] poss) {
		this(sources, dim, poss);
		this.annotationsOnDimensions = annotationsOnDimensions;
		if (annotationsOnDimensions != null)
			this.bits |= ASTNode.HasTypeAnnotations;
	}

	@Override
	public int dimensions() {

		return this.dimensions;
	}

	@Override
	public int extraDimensions() {
		return this.extendedDimensions;
	}

	/**
	 @see org.eclipse.jdt.internal.compiler.ast.TypeReference#getAnnotationsOnDimensions(boolean)
	*/
	@Override
	public Annotation[][] getAnnotationsOnDimensions(boolean useSourceOrder) {
		if (useSourceOrder || this.annotationsOnDimensions == null || this.annotationsOnDimensions.length == 0 || this.extendedDimensions == 0 || this.extendedDimensions == this.dimensions)
			return this.annotationsOnDimensions;
		Annotation [][] externalAnnotations = new Annotation[this.dimensions][];
		final int baseDimensions = this.dimensions - this.extendedDimensions;
		System.arraycopy(this.annotationsOnDimensions, baseDimensions, externalAnnotations, 0, this.extendedDimensions);
		System.arraycopy(this.annotationsOnDimensions, 0, externalAnnotations, this.extendedDimensions, baseDimensions);
		return externalAnnotations;
	}

	@Override
	public void setAnnotationsOnDimensions(Annotation [][] annotationsOnDimensions) {
		this.annotationsOnDimensions = annotationsOnDimensions;
	}

	/**
	 * @return char[][]
	 */
	@Override
	public char [][] getParameterizedTypeName(){
		int dim = this.dimensions;
		char[] dimChars = new char[dim*2];
		for (int i = 0; i < dim; i++) {
			int index = i*2;
			dimChars[index] = '[';
			dimChars[index+1] = ']';
		}
		int length = this.tokens.length;
		char[][] qParamName = new char[length][];
		System.arraycopy(this.tokens, 0, qParamName, 0, length-1);
		qParamName[length-1] = CharOperation.concat(this.tokens[length-1], dimChars);
		return qParamName;
	}

	@Override
	protected TypeBinding getTypeBinding(Scope scope) {

		if (this.resolvedType != null)
			return this.resolvedType;
		if (this.dimensions > 255) {
			scope.problemReporter().tooManyDimensions(this);
		}
		LookupEnvironment env = scope.environment();
		try {
			env.missingClassFileLocation = this;
			TypeBinding leafComponentType = super.getTypeBinding(scope);
			if (leafComponentType != null) {
				return this.resolvedType = scope.createArrayType(leafComponentType, this.dimensions);
			}
			return null;
		} catch (AbortCompilation e) {
			e.updateContext(this, scope.referenceCompilationUnit().compilationResult);
			throw e;
		} finally {
			env.missingClassFileLocation = null;
		}
	}

	@Override
	protected TypeBinding internalResolveType(Scope scope, int location) {
		TypeBinding internalResolveType = super.internalResolveType(scope, location);
		internalResolveType = ArrayTypeReference.maybeMarkArrayContentsNonNull(scope, internalResolveType, this.sourceStart, this.dimensions, null);

		return internalResolveType;
	}

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output){

		super.printExpression(indent, output);
		if ((this.bits & IsVarArgs) != 0) {
			for (int i= 0 ; i < this.dimensions - 1; i++) {
				if (this.annotationsOnDimensions != null && this.annotationsOnDimensions[i] != null) {
					output.append(' ');
					printAnnotations(this.annotationsOnDimensions[i], output);
					output.append(' ');
				}
				output.append("[]"); //$NON-NLS-1$
			}
			if (this.annotationsOnDimensions != null && this.annotationsOnDimensions[this.dimensions - 1] != null) {
				output.append(' ');
				printAnnotations(this.annotationsOnDimensions[this.dimensions - 1], output);
				output.append(' ');
			}
			output.append("..."); //$NON-NLS-1$
		} else {
			for (int i= 0 ; i < this.dimensions; i++) {
				if (this.annotationsOnDimensions != null && this.annotationsOnDimensions[i] != null) {
					output.append(" "); //$NON-NLS-1$
					printAnnotations(this.annotationsOnDimensions[i], output);
					output.append(" "); //$NON-NLS-1$
				}
				output.append("[]"); //$NON-NLS-1$
			}
		}
		return output;
	}

	@Override
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.annotations != null) {
				int annotationsLevels = this.annotations.length;
				for (int i = 0; i < annotationsLevels; i++) {
					int annotationsLength = this.annotations[i] == null ? 0 : this.annotations[i].length;
					for (int j = 0; j < annotationsLength; j++)
						this.annotations[i][j].traverse(visitor, scope);
				}
			}
			if (this.annotationsOnDimensions != null) {
				for (int i = 0, max = this.annotationsOnDimensions.length; i < max; i++) {
					Annotation[] annotations2 = this.annotationsOnDimensions[i];
					for (int j = 0, max2 = annotations2 == null ? 0 : annotations2.length; j < max2; j++) {
						Annotation annotation = annotations2[j];
						annotation.traverse(visitor, scope);
					}
				}
			}
		}
		visitor.endVisit(this, scope);
	}

	@Override
	public void traverse(ASTVisitor visitor, ClassScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.annotations != null) {
				int annotationsLevels = this.annotations.length;
				for (int i = 0; i < annotationsLevels; i++) {
					int annotationsLength = this.annotations[i] == null ? 0 : this.annotations[i].length;
					for (int j = 0; j < annotationsLength; j++)
						this.annotations[i][j].traverse(visitor, scope);
				}
			}
			if (this.annotationsOnDimensions != null) {
				for (int i = 0, max = this.annotationsOnDimensions.length; i < max; i++) {
					Annotation[] annotations2 = this.annotationsOnDimensions[i];
					for (int j = 0, max2 = annotations2 == null ? 0 : annotations2.length; j < max2; j++) {
						Annotation annotation = annotations2[j];
						annotation.traverse(visitor, scope);
					}
				}
			}
		}
		visitor.endVisit(this, scope);
	}
}
