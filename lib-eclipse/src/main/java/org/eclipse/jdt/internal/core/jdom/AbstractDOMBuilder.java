/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.jdom;

import java.util.Stack;

import org.eclipse.jdt.core.jdom.*;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.core.util.ReferenceInfoAdapter;

/**
 * An abstract DOM builder that contains shared functionality of DOMBuilder and SimpleDOMBuilder.
 * @deprecated The JDOM was made obsolete by the addition in 2.0 of the more
 * powerful, fine-grained DOM/AST API found in the
 * org.eclipse.jdt.core.dom package.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class AbstractDOMBuilder extends ReferenceInfoAdapter implements ILineStartFinder {
	/**
	 * Set to true when an error is encounterd while
	 * fuzzy parsing
	 */
	protected boolean fAbort;

	/**
	 * True when a compilation unit is being constructed.
	 * False when any other type of document fragment is
	 * being constructed.
	 */
	protected boolean fBuildingCU = false;

	/**
	 * True when a compilation unit or type is being
	 * constructed. False when any other type of document
	 * fragment is being constructed.
	 */
	protected boolean fBuildingType= false;

	/**
	 * The String on which the JDOM is being created.
	 */
	protected char[] fDocument= null;

	/**
	 * The source positions of all of the line separators in the document.
	 */
	protected int[] fLineStartPositions = new int[] { 0 };

	/**
	 * A stack of enclosing scopes used when constructing
	 * a compilation unit or type. The top of the stack
	 * is the document fragment that children are added to.
	 */
	protected Stack fStack = null;

	/**
	 * The number of fields constructed in the current
	 * document. This is used when building a single
	 * field document fragment, since the DOMBuilder only
	 * accepts documents with one field declaration.
	 */
	protected int fFieldCount;

	/**
	 * The current node being constructed.
	 */
	protected DOMNode fNode;
/**
 * AbstractDOMBuilder constructor.
 */
public AbstractDOMBuilder() {
	super();
}
/**
 * Accepts the line separator table and converts it into a line start table.
 *
 * <p>A line separator might corresponds to several characters in the source.
 *
 * @see org.eclipse.jdt.internal.compiler.IDocumentElementRequestor#acceptLineSeparatorPositions(int[])
 */
public void acceptLineSeparatorPositions(int[] positions) {
	if (positions != null) {
		int length = positions.length;
		if (length > 0) {
			this.fLineStartPositions = new int[length + 1];
			this.fLineStartPositions[0] = 0;
			int documentLength = this.fDocument.length;
			for (int i = 0; i < length; i++) {
				int iPlusOne = i + 1;
				int positionPlusOne = positions[i] + 1;
				if (positionPlusOne < documentLength) {
					if (iPlusOne < length) {
						// more separators
						this.fLineStartPositions[iPlusOne] = positionPlusOne;
					} else {
						// no more separators
						if (this.fDocument[positionPlusOne] == '\n') {
							this.fLineStartPositions[iPlusOne] = positionPlusOne + 1;
						} else {
							this.fLineStartPositions[iPlusOne] = positionPlusOne;
						}
					}
				} else {
					this.fLineStartPositions[iPlusOne] = positionPlusOne;
				}
			}
		}
	}
}
/**
 * Adds the given node to the current enclosing scope, building the JDOM
 * tree. Nodes are only added to an enclosing scope when a compilation unit or type
 * is being built (since those are the only nodes that have children).
 *
 * <p>NOTE: nodes are added to the JDOM via the method #basicAddChild such that
 * the nodes in the newly created JDOM are not fragmented.
 */
protected void addChild(IDOMNode child) {
	if (this.fStack.size() > 0) {
		DOMNode parent = (DOMNode) this.fStack.peek();
		if (this.fBuildingCU || this.fBuildingType) {
			parent.basicAddChild(child);
		}
	}
}
/**
 * @see IDOMFactory#createCompilationUnit(String, String)
 */
public IDOMCompilationUnit createCompilationUnit(char[] contents, char[] name) {
	return createCompilationUnit(new CompilationUnit(contents, name));
}
/**
 * @see IDOMFactory#createCompilationUnit(String, String)
 */
public IDOMCompilationUnit createCompilationUnit(ICompilationUnit compilationUnit) {
	if (this.fAbort) {
		return null;
	}
	this.fNode.normalize(this);
	return (IDOMCompilationUnit)this.fNode;
}
/**
 * @see org.eclipse.jdt.internal.compiler.IDocumentElementRequestor#enterClass(int, int[], int, int, int, char[], int, int, char[], int, int, char[][], int[], int[], int)
 */
public void enterCompilationUnit() {
 	if (this.fBuildingCU) {
 		IDOMCompilationUnit cu= new DOMCompilationUnit(this.fDocument, new int[] {0, this.fDocument.length - 1});
 		this.fStack.push(cu);
 	}
}
/**
 * Finishes the configuration of the compilation unit DOM object which
 * was created by a previous enterCompilationUnit call.
 *
 * @see org.eclipse.jdt.internal.compiler.IDocumentElementRequestor#exitCompilationUnit(int)
 */
public void exitCompilationUnit(int declarationEnd) {
	DOMCompilationUnit cu = (DOMCompilationUnit) this.fStack.pop();
	cu.setSourceRangeEnd(declarationEnd);
	this.fNode = cu;
}
/**
 * Finishes the configuration of the class and interface DOM objects.
 *
 * @param bodyEnd - a source position corresponding to the closing bracket of the class
 * @param declarationEnd - a source position corresponding to the end of the class
 *		declaration.  This can include whitespace and comments following the closing bracket.
 */
protected void exitType(int bodyEnd, int declarationEnd) {
	DOMType type = (DOMType)this.fStack.pop();
	type.setSourceRangeEnd(declarationEnd);
	type.setCloseBodyRangeStart(bodyEnd);
	type.setCloseBodyRangeEnd(bodyEnd);
	this.fNode = type;
}
/**
 * @see ILineStartFinder#getLineStart(int)
 */
@Override
public int getLineStart(int position) {
	int lineSeparatorCount = this.fLineStartPositions.length;
	// reverse traversal intentional.
	for(int i = lineSeparatorCount - 1; i >= 0; i--) {
		if (this.fLineStartPositions[i] <= position)
			return this.fLineStartPositions[i];
	}
	return 0;
}
/**
 * Initializes the builder to create a document fragment.
 *
 * @param sourceCode - the document containing the source code to be analyzed
 * @param buildingCompilationUnit - true if a the document is being analyzed to
 *		create a compilation unit, otherwise false
 * @param buildingType - true if the document is being analyzed to create a
 *		type or compilation unit
 */
protected void initializeBuild(char[] sourceCode, boolean buildingCompilationUnit, boolean buildingType) {
	this.fBuildingCU = buildingCompilationUnit;
	this.fBuildingType = buildingType;
	this.fStack = new Stack();
	this.fDocument = sourceCode;
	this.fFieldCount = 0;
	this.fAbort = false;
}
}
