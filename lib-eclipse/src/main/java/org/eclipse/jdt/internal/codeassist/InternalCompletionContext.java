/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.codeassist;

import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnJavadoc;
import org.eclipse.jdt.internal.codeassist.complete.CompletionParser;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.util.ObjectVector;


/**
 * Internal completion context
 * @since 3.1
 */
public class InternalCompletionContext extends CompletionContext {
	protected char[][] expectedTypesSignatures;
	protected char[][] expectedTypesKeys;
	protected int javadoc;

	protected int offset = -1;
	protected int tokenStart = -1;
	protected int tokenEnd = -1;
	protected char[] token = null;
	protected int tokenKind;
	protected int tokenLocation;

	protected boolean isExtended;
	protected InternalExtendedCompletionContext extendedContext;

	protected void setExpectedTypesKeys(char[][] expectedTypesKeys) {
		this.expectedTypesKeys = expectedTypesKeys;
	}

	protected void setExpectedTypesSignatures(char[][] expectedTypesSignatures) {
		this.expectedTypesSignatures = expectedTypesSignatures;
	}

	protected void setExtended() {
		this.isExtended = true;
	}

	protected void setExtendedData(
			ITypeRoot typeRoot,
			CompilationUnitDeclaration compilationUnitDeclaration,
			LookupEnvironment lookupEnvironment,
			Scope scope,
			ASTNode astNode,
			ASTNode astNodeParent,
			WorkingCopyOwner owner,
			CompletionParser parser) {
		this.isExtended = true;
		this.extendedContext =
			new InternalExtendedCompletionContext(
					this,
					typeRoot,
					compilationUnitDeclaration,
					lookupEnvironment,
					scope,
					astNode,
					astNodeParent,
					owner,
					parser);
	}

	protected void setJavadoc(int javadoc) {
		this.javadoc = javadoc;
	}

	protected void setOffset(int offset) {
		this.offset = offset;
	}

	protected void setToken(char[] token) {
		this.token = token;
	}

	protected void setTokenKind(int tokenKind) {
		this.tokenKind = tokenKind;
	}

	protected void setTokenLocation(int tokenLocation) {
		this.tokenLocation = tokenLocation;
	}

	protected void setTokenRange(int start, int end) {
		this.setTokenRange(start, end, -1);
	}

	protected void setTokenRange(int start, int end, int endOfEmptyToken) {
		this.tokenStart = start;
		this.tokenEnd = endOfEmptyToken > end ? endOfEmptyToken : end;

		// Work around for bug 132558 (https://bugs.eclipse.org/bugs/show_bug.cgi?id=132558).
		// completionLocation can be -1 if the completion occur at the start of a file or
		// the start of a code snippet but this API isn't design to support negative position.
		if(this.tokenEnd == -1) {
			this.tokenEnd = 0;
		}
	}

	@Override
	public IJavaElement getEnclosingElement() {
		if (!this.isExtended) throw new UnsupportedOperationException("Operation only supported in extended context"); //$NON-NLS-1$

		if (this.extendedContext == null) return null;

		return this.extendedContext.getEnclosingElement();
	}

	@Override
	public char[][] getExpectedTypesKeys() {
		return this.expectedTypesKeys;
	}

	@Override
	public char[][] getExpectedTypesSignatures() {
		return this.expectedTypesSignatures;
	}

	@Override
	public int getOffset() {
		return this.offset;
	}

	@Override
	public char[] getToken() {
		return this.token;
	}

	// TODO (david) https://bugs.eclipse.org/bugs/show_bug.cgi?id=132558
	@Override
	public int getTokenEnd() {
		return this.tokenEnd;
	}

	@Override
	public int getTokenKind() {
		return this.tokenKind;
	}

	@Override
	public int getTokenLocation() {
		return this.tokenLocation;
	}

	@Override
	public int getTokenStart() {
		return this.tokenStart;
	}

	@Override
	public IJavaElement[] getVisibleElements(String typeSignature) {
		if (!this.isExtended) throw new UnsupportedOperationException("Operation only supported in extended context"); //$NON-NLS-1$

		if (this.extendedContext == null) return new IJavaElement[0];

		return this.extendedContext.getVisibleElements(typeSignature);
	}

	@Override
	public boolean isExtended() {
		return this.isExtended;
	}

	@Override
	public boolean isInJavadoc() {
		return this.javadoc != 0;
	}

	@Override
	public boolean isInJavadocFormalReference() {
		return (this.javadoc & CompletionOnJavadoc.FORMAL_REFERENCE) != 0;
	}

	@Override
	public boolean isInJavadocText() {
		return (this.javadoc & CompletionOnJavadoc.TEXT) != 0;
	}

	/**
	 * Return the completion node associated with the current completion.
	 *
	 * @return completion AST node, or null if the extendedContext is null.
	 * @exception UnsupportedOperationException if the context is not an extended context
	 *
	 * @see #isExtended()
	 */
	public ASTNode getCompletionNode() {
		if (!this.isExtended) throw new UnsupportedOperationException("Operation only supported in extended context"); //$NON-NLS-1$

		if (this.extendedContext == null) return null;

		return this.extendedContext.getCompletionNode();
	}

	/**
	 * Return the parent AST node of the completion node associated with the current completion.
	 *
	 * @return completion parent AST node, or null if the extendedContext is null.
	 * @exception UnsupportedOperationException if the context is not an extended context
	 *
	 * @see #isExtended()
	 */
	public ASTNode getCompletionNodeParent() {
		if (!this.isExtended) throw new UnsupportedOperationException("Operation only supported in extended context"); //$NON-NLS-1$

		if (this.extendedContext == null) return null;

		return this.extendedContext.getCompletionNodeParent();
	}

	/**
	 * Return the bindings of all visible local variables in the current completion context.
	 *
	 * @return bindings of all visible local variables, or null if the extendedContext is null. Returned bindings are instances of
	 * {@link LocalVariableBinding}
	 * @exception UnsupportedOperationException if the context is not an extended context
	 *
	 * @see #isExtended()
	 */
	public ObjectVector getVisibleLocalVariables() {
		if (!this.isExtended) throw new UnsupportedOperationException("Operation only supported in extended context"); //$NON-NLS-1$

		if (this.extendedContext == null) return null;

		return this.extendedContext.getVisibleLocalVariables();
	}

	/**
	 * Return the bindings of all visible fields in the current completion context.
	 *
	 * @return bindings of all visible fields, or null if the extendedContext is null. Returned bindings are instances of
	 * {@link FieldBinding}
	 * @exception UnsupportedOperationException if the context is not an extended context
	 *
	 * @see #isExtended()
	 */
	public ObjectVector getVisibleFields() {
		if (!this.isExtended) throw new UnsupportedOperationException("Operation only supported in extended context"); //$NON-NLS-1$

		if (this.extendedContext == null) return null;

		return this.extendedContext.getVisibleFields();
	}

	/**
	 * Return the bindings of all visible methods in the current completion context.
	 *
	 * @return bindings of all visible methods, or null if the extendedContext is null. Returned bindings are instances of
	 * {@link MethodBinding}
	 * @exception UnsupportedOperationException if the context is not an extended context
	 *
	 * @see #isExtended()
	 */
	public ObjectVector getVisibleMethods() {
		if (!this.isExtended) throw new UnsupportedOperationException("Operation only supported in extended context"); //$NON-NLS-1$

		if (this.extendedContext == null) return null;

		return this.extendedContext.getVisibleMethods();
	}
}
