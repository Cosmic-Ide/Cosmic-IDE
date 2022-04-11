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
 *******************************************************************************/
package org.eclipse.jdt.internal.codeassist.complete;

/*
 * Completion node build by the parser in any case it was intending to
 * reduce a type reference containing the completion identifier as a single
 * name reference.
 * e.g.
 *
 *	class X extends Obj[cursor]
 *
 *	---> class X extends <CompleteOnType:Obj>
 *
 * The source range of the completion node denotes the source range
 * which should be replaced by the completion.
 */

import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class CompletionOnSingleTypeReference extends SingleTypeReference {
public static final int K_TYPE = 0;
public static final int K_CLASS = 1;
public static final int K_INTERFACE = 2;
public static final int K_EXCEPTION = 3;

private int kind = K_TYPE;
public boolean isCompletionNode;
public boolean isConstructorType;
public CompletionOnFieldType fieldTypeCompletionNode;
public char[][] possibleKeywords;
public boolean canBeExplicitConstructor;

public CompletionOnSingleTypeReference(char[] source, long pos) {
	this(source, pos, K_TYPE);
}
public CompletionOnSingleTypeReference(char[] source, long pos, int kind) {
	super(source, pos);
	this.isCompletionNode = true;
	this.kind = kind;
}
public CompletionOnSingleTypeReference(char[] assistName, long position, char[][] keywords, boolean canBeSuperCall) {
	this(assistName, position);
	this.possibleKeywords = keywords;
	this.canBeExplicitConstructor = canBeSuperCall;
}
@Override
public void aboutToResolve(Scope scope) {
	getTypeBinding(scope);
}
/*
 * No expansion of the completion reference into an array one
 */
@Override
public TypeReference augmentTypeWithAdditionalDimensions(int additionalDimensions, Annotation[][] additionalAnnotations, boolean isVarargs) {
	return this;
}
@Override
protected TypeBinding getTypeBinding(Scope scope) {
    if (this.fieldTypeCompletionNode != null) {
		throw new CompletionNodeFound(this.fieldTypeCompletionNode, scope);
    }
	if(this.isCompletionNode) {
		throw new CompletionNodeFound(this, scope);
	} else {
		return super.getTypeBinding(scope);
	}
}
public boolean isClass(){
	return this.kind == K_CLASS;
}
public boolean isInterface(){
	return this.kind == K_INTERFACE;
}
public boolean isException(){
	return this.kind == K_EXCEPTION;
}
public boolean isSuperType(){
	return this.kind == K_CLASS || this.kind == K_INTERFACE;
}
@Override
public StringBuffer printExpression(int indent, StringBuffer output){
	switch (this.kind) {
		case K_CLASS :
			output.append("<CompleteOnClass:");//$NON-NLS-1$
			break;
		case K_INTERFACE :
			output.append("<CompleteOnInterface:");//$NON-NLS-1$
			break;
		case K_EXCEPTION :
			output.append("<CompleteOnException:");//$NON-NLS-1$
			break;
		default :
			output.append("<CompleteOnType:");//$NON-NLS-1$
			break;
	}
	return output.append(this.token).append('>');
}
@Override
public TypeBinding resolveTypeEnclosing(BlockScope scope, ReferenceBinding enclosingType) {
    if (this.fieldTypeCompletionNode != null) {
		throw new CompletionNodeFound(this.fieldTypeCompletionNode, scope);
    }
	if(this.isCompletionNode) {
		throw new CompletionNodeFound(this, enclosingType, scope);
	} else {
		return super.resolveTypeEnclosing(scope, enclosingType);
	}
}
public void setKind(int kind) {
	this.kind = kind;
}
}
