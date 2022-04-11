/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Stephan Herrmann - Contributions for
 *								bug 319201 - [null] no warning when unboxing SingleNameReference causes NPE
 *								bug 345305 - [compiler][null] Compiler misidentifies a case of "variable can only be null"
 *								bug 403147 - [compiler][null] FUP of bug 400761: consolidate interaction between unboxing, NPE, and deferred checking
 *								Bug 417758 - [1.8][null] Null safety compromise during array creation.
 *								Bug 427163 - [1.8][null] bogus error "Contradictory null specification" on varags
 *     Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *                          Bug 409247 - [1.8][compiler] Verify error with code allocating multidimensional array
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import java.util.List;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.impl.*;
import org.eclipse.jdt.internal.compiler.ast.TypeReference.AnnotationCollector;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class ArrayAllocationExpression extends Expression {

	public TypeReference type;

	//dimensions.length gives the number of dimensions, but the
	// last ones may be nulled as in new int[4][5][][]
	public Expression[] dimensions;
	public Annotation [][] annotationsOnDimensions; // jsr308 style annotations.
	public ArrayInitializer initializer;

	@Override
	public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
		for (int i = 0, max = this.dimensions.length; i < max; i++) {
			Expression dim;
			if ((dim = this.dimensions[i]) != null) {
				flowInfo = dim.analyseCode(currentScope, flowContext, flowInfo);
				dim.checkNPEbyUnboxing(currentScope, flowContext, flowInfo);
			}
		}
		// account for potential OutOfMemoryError:
		flowContext.recordAbruptExit();
		if (this.initializer != null) {
			return this.initializer.analyseCode(currentScope, flowContext, flowInfo);
		}
		return flowInfo;
	}

	/**
	 * Code generation for a array allocation expression
	 */
	@Override
	public void generateCode(BlockScope currentScope, 	CodeStream codeStream, boolean valueRequired) {

		int pc = codeStream.position;

		if (this.initializer != null) {
			this.initializer.generateCode(this.type, this, currentScope, codeStream, valueRequired);
			return;
		}

		int explicitDimCount = 0;
		for (int i = 0, max = this.dimensions.length; i < max; i++) {
			Expression dimExpression;
			if ((dimExpression = this.dimensions[i]) == null) break; // implicit dim, no further explict after this point
			dimExpression.generateCode(currentScope, codeStream, true);
			explicitDimCount++;
		}

		// array allocation
		if (explicitDimCount == 1) {
			// Mono-dimensional array
			codeStream.newArray(this.type, this, (ArrayBinding)this.resolvedType);
		} else {
			// Multi-dimensional array
			codeStream.multianewarray(this.type, this.resolvedType, explicitDimCount, this);
		}
		if (valueRequired) {
			codeStream.generateImplicitConversion(this.implicitConversion);
		} else {
			codeStream.pop();
		}
		codeStream.recordPositionsFrom(pc, this.sourceStart);
	}


	@Override
	public StringBuffer printExpression(int indent, StringBuffer output) {
		output.append("new "); //$NON-NLS-1$
		this.type.print(0, output);
		for (int i = 0; i < this.dimensions.length; i++) {
			if (this.annotationsOnDimensions != null && this.annotationsOnDimensions[i] != null) {
				output.append(' ');
				printAnnotations(this.annotationsOnDimensions[i], output);
				output.append(' ');
			}
			if (this.dimensions[i] == null)
				output.append("[]"); //$NON-NLS-1$
			else {
				output.append('[');
				this.dimensions[i].printExpression(0, output);
				output.append(']');
			}
		}
		if (this.initializer != null) this.initializer.printExpression(0, output);
		return output;
	}

	@Override
	public TypeBinding resolveType(BlockScope scope) {
		// Build an array type reference using the current dimensions
		// The parser does not check for the fact that dimension may be null
		// only at the -end- like new int [4][][]. The parser allows new int[][4][]
		// so this must be checked here......(this comes from a reduction to LL1 grammar)

		TypeBinding referenceType = this.type.resolveType(scope, true /* check bounds*/);

		// will check for null after dimensions are checked
		this.constant = Constant.NotAConstant;
		if (referenceType == TypeBinding.VOID) {
			scope.problemReporter().cannotAllocateVoidArray(this);
			referenceType = null;
		}

		// check the validity of the dimension syntax (and test for all null dimensions)
		int explicitDimIndex = -1;
		loop: for (int i = this.dimensions.length; --i >= 0;) {
			if (this.dimensions[i] != null) {
				if (explicitDimIndex < 0) explicitDimIndex = i;
			} else if (explicitDimIndex > 0) {
				// should not have an empty dimension before an non-empty one
				scope.problemReporter().incorrectLocationForNonEmptyDimension(this, explicitDimIndex);
				break loop;
			}
		}

		// explicitDimIndex < 0 says if all dimensions are nulled
		// when an initializer is given, no dimension must be specified
		if (this.initializer == null) {
			if (explicitDimIndex < 0) {
				scope.problemReporter().mustDefineDimensionsOrInitializer(this);
			}
			// allow new List<?>[5] - only check for generic array when no initializer, since also checked inside initializer resolution
			if (referenceType != null && !referenceType.isReifiable()) {
			    scope.problemReporter().illegalGenericArray(referenceType, this);
			}
		} else if (explicitDimIndex >= 0) {
			scope.problemReporter().cannotDefineDimensionsAndInitializer(this);
		}

		// dimensions resolution
		for (int i = 0; i <= explicitDimIndex; i++) {
			Expression dimExpression;
			if ((dimExpression = this.dimensions[i]) != null) {
				TypeBinding dimensionType = dimExpression.resolveTypeExpecting(scope, TypeBinding.INT);
				if (dimensionType != null) {
					this.dimensions[i].computeConversion(scope, TypeBinding.INT, dimensionType);
				}
			}
		}

		// building the array binding
		if (referenceType != null) {
			if (this.dimensions.length > 255) {
				scope.problemReporter().tooManyDimensions(this);
			}
			if (this.type.annotations != null
					&& (referenceType.tagBits & TagBits.AnnotationNullMASK) == TagBits.AnnotationNullMASK)
			{
				scope.problemReporter().contradictoryNullAnnotations(this.type.annotations[this.type.annotations.length-1]);
			}
			this.resolvedType = scope.createArrayType(referenceType, this.dimensions.length);

			if (this.annotationsOnDimensions != null) {
				this.resolvedType = resolveAnnotations(scope, this.annotationsOnDimensions, this.resolvedType);
				long[] nullTagBitsPerDimension = ((ArrayBinding)this.resolvedType).nullTagBitsPerDimension;
				if (nullTagBitsPerDimension != null) {
					for (int i = 0; i < this.annotationsOnDimensions.length; i++) {
						if ((nullTagBitsPerDimension[i] & TagBits.AnnotationNullMASK) == TagBits.AnnotationNullMASK) {
							scope.problemReporter().contradictoryNullAnnotations(this.annotationsOnDimensions[i]);
							nullTagBitsPerDimension[i] = 0;
						}
					}
				}
			}

			// check the initializer
			if (this.initializer != null) {
				this.resolvedType = ArrayTypeReference.maybeMarkArrayContentsNonNull(scope, this.resolvedType, this.sourceStart, this.dimensions.length, null);
				if ((this.initializer.resolveTypeExpecting(scope, this.resolvedType)) != null)
					this.initializer.binding = (ArrayBinding)this.resolvedType;
			}
			if ((referenceType.tagBits & TagBits.HasMissingType) != 0) {
				return null;
			}
		}
		return this.resolvedType;
	}

	@Override
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			int dimensionsLength = this.dimensions.length;
			this.type.traverse(visitor, scope);
			for (int i = 0; i < dimensionsLength; i++) {
				Annotation [] annotations = this.annotationsOnDimensions == null ? null : this.annotationsOnDimensions[i];
				int annotationsLength = annotations == null ? 0 : annotations.length;
				for (int j = 0; j < annotationsLength; j++) {
					annotations[j].traverse(visitor, scope);
				}
				if (this.dimensions[i] != null)
					this.dimensions[i].traverse(visitor, scope);
			}
			if (this.initializer != null)
				this.initializer.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}

	public void getAllAnnotationContexts(int targetType, int info, List<AnnotationContext> allTypeAnnotationContexts) {
		AnnotationCollector collector = new AnnotationCollector(this, targetType, info, allTypeAnnotationContexts);
		this.type.traverse(collector, (BlockScope) null);
		if (this.annotationsOnDimensions != null)  {
			int dimensionsLength = this.dimensions.length;
			for (int i = 0; i < dimensionsLength; i++) {
				Annotation [] annotations = this.annotationsOnDimensions[i];
				int annotationsLength = annotations == null ? 0 : annotations.length;
				for (int j = 0; j < annotationsLength; j++) {
					annotations[j].traverse(collector, (BlockScope) null);
				}
			}
		}
	}

	public Annotation[][] getAnnotationsOnDimensions() {
		return this.annotationsOnDimensions;
	}
}
