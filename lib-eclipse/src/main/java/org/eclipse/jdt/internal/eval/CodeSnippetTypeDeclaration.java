/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.jdt.internal.eval;

import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;
import org.eclipse.jdt.internal.compiler.problem.AbortType;
import org.eclipse.jdt.internal.compiler.util.Util;

public class CodeSnippetTypeDeclaration extends TypeDeclaration {

public CodeSnippetTypeDeclaration(CompilationResult compilationResult){
	super(compilationResult);
}

/**
 * Generic bytecode generation for type
 */
@Override
public void generateCode(ClassFile enclosingClassFile) {
	if ((this.bits & ASTNode.HasBeenGenerated) != 0) return;
	this.bits |= ASTNode.HasBeenGenerated;

	if (this.ignoreFurtherInvestigation) {
		if (this.binding == null)
			return;
		CodeSnippetClassFile.createProblemType(this, this.scope.referenceCompilationUnit().compilationResult);
		return;
	}
	try {
		// create the result for a compiled type
		ClassFile classFile = new CodeSnippetClassFile(this.binding, enclosingClassFile, false);
		// generate all fiels
		classFile.addFieldInfos();
		if (this.binding.isMemberType()) {
			classFile.recordInnerClasses(this.binding);
		} else if (this.binding.isLocalType()) {
			enclosingClassFile.recordInnerClasses(this.binding);
			classFile.recordInnerClasses(this.binding);
		}
		TypeVariableBinding[] typeVariables = this.binding.typeVariables();
		for (int i = 0, max = typeVariables.length; i < max; i++) {
			TypeVariableBinding typeVariableBinding = typeVariables[i];
			if ((typeVariableBinding.tagBits & TagBits.ContainsNestedTypeReferences) != 0) {
				Util.recordNestedType(classFile, typeVariableBinding);
			}
		}
		if (this.memberTypes != null) {
			for (int i = 0, max = this.memberTypes.length; i < max; i++) {
				TypeDeclaration memberType = this.memberTypes[i];
				classFile.recordInnerClasses(memberType.binding);
				memberType.generateCode(this.scope, classFile);
			}
		}
		// generate all methods
		classFile.setForMethodInfos();
		if (this.methods != null) {
			for (int i = 0, max = this.methods.length; i < max; i++) {
				this.methods[i].generateCode(this.scope, classFile);
			}
		}

		// generate all methods
		classFile.addSpecialMethods(this);

		if (this.ignoreFurtherInvestigation){ // trigger problem type generation for code gen errors
			throw new AbortType(this.scope.referenceCompilationUnit().compilationResult, null);
		}

		// finalize the compiled type result
		classFile.addAttributes();
		this.scope.referenceCompilationUnit().compilationResult.record(this.binding.constantPoolName(), classFile);
	} catch (AbortType e) {
		if (this.binding == null)
			return;
		CodeSnippetClassFile.createProblemType(this, this.scope.referenceCompilationUnit().compilationResult);
	}
}
}
