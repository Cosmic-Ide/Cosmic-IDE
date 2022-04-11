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
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.text.edits.TextEdit;

/**
 * This operation deletes a collection of elements (and
 * all of their children).
 * If an element does not exist, it is ignored.
 *
 * <p>NOTE: This operation only deletes elements contained within leaf resources -
 * that is, elements within compilation units. To delete a compilation unit or
 * a package, etc (which have an actual resource), a DeleteResourcesOperation
 * should be used.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DeleteElementsOperation extends MultiOperation {
	/**
	 * The elements this operation processes grouped by compilation unit
	 * @see #processElements() Keys are compilation units,
	 * values are <code>IRegion</code>s of elements to be processed in each
	 * compilation unit.
	 */
	protected Map childrenToRemove;
	/**
	 * The <code>ASTParser</code> used to manipulate the source code of
	 * <code>ICompilationUnit</code>.
	 */
	protected ASTParser parser;
	/**
	 * When executed, this operation will delete the given elements. The elements
	 * to delete cannot be <code>null</code> or empty, and must be contained within a
	 * compilation unit.
	 */
	public DeleteElementsOperation(IJavaElement[] elementsToDelete, boolean force) {
		super(elementsToDelete, force);
		initASTParser();
	}

	private void deleteElement(IJavaElement elementToRemove, ICompilationUnit cu) throws JavaModelException {
		// ensure cu is consistent (noop if already consistent)
		cu.makeConsistent(this.progressMonitor);
		this.parser.setSource(cu);
		CompilationUnit astCU = (CompilationUnit) this.parser.createAST(this.progressMonitor);
		ASTNode node = ((JavaElement) elementToRemove).findNode(astCU);
		if (node == null)
			Assert.isTrue(false, "Failed to locate " + elementToRemove.getElementName() + " in " + cu.getElementName()); //$NON-NLS-1$//$NON-NLS-2$
		AST ast = astCU.getAST();
		ASTRewrite rewriter = ASTRewrite.create(ast);
		rewriter.remove(node, null);
 		TextEdit edits = rewriter.rewriteAST();
 		applyTextEdit(cu, edits);
	}

	private void initASTParser() {
		this.parser = ASTParser.newParser(getLatestASTLevel());
	}

	/**
	 * @see MultiOperation
	 */
	@Override
	protected String getMainTaskName() {
		return Messages.operation_deleteElementProgress;
	}
	@Override
	protected ISchedulingRule getSchedulingRule() {
		if (this.elementsToProcess != null && this.elementsToProcess.length == 1) {
			IResource resource = this.elementsToProcess[0].getResource();
			if (resource != null)
				return ResourcesPlugin.getWorkspace().getRuleFactory().modifyRule(resource);
		}
		return super.getSchedulingRule();
	}
	/**
	 * Groups the elements to be processed by their compilation unit.
	 * If parent/child combinations are present, children are
	 * discarded (only the parents are processed). Removes any
	 * duplicates specified in elements to be processed.
	 */
	protected void groupElements() throws JavaModelException {
		this.childrenToRemove = new HashMap(1);
		int uniqueCUs = 0;
		for (int i = 0, length = this.elementsToProcess.length; i < length; i++) {
			IJavaElement e = this.elementsToProcess[i];
			ICompilationUnit cu = getCompilationUnitFor(e);
			if (cu == null) {
				throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.READ_ONLY, e));
			} else {
				IRegion region = (IRegion) this.childrenToRemove.get(cu);
				if (region == null) {
					region = new Region();
					this.childrenToRemove.put(cu, region);
					uniqueCUs += 1;
				}
				region.add(e);
			}
		}
		this.elementsToProcess = new IJavaElement[uniqueCUs];
		Iterator iter = this.childrenToRemove.keySet().iterator();
		int i = 0;
		while (iter.hasNext()) {
			this.elementsToProcess[i++] = (IJavaElement) iter.next();
		}
	}
	/**
	 * Deletes this element from its compilation unit.
	 * @see MultiOperation
	 */
	@Override
	protected void processElement(IJavaElement element) throws JavaModelException {
		ICompilationUnit cu = (ICompilationUnit) element;

		// keep track of the import statements - if all are removed, delete
		// the import container (and report it in the delta)
		int numberOfImports = cu.getImports().length;

		JavaElementDelta delta = new JavaElementDelta(cu);
		IJavaElement[] cuElements = ((IRegion) this.childrenToRemove.get(cu)).getElements();
		for (int i = 0, length = cuElements.length; i < length; i++) {
			IJavaElement e = cuElements[i];
			if (e.exists()) {
				deleteElement(e, cu);
				delta.removed(e);
				if (e.getElementType() == IJavaElement.IMPORT_DECLARATION) {
					numberOfImports--;
					if (numberOfImports == 0) {
						delta.removed(cu.getImportContainer());
					}
				}
			}
		}
		if (delta.getAffectedChildren().length > 0) {
			cu.save(getSubProgressMonitor(1), this.force);
			if (!cu.isWorkingCopy()) { // if unit is working copy, then save will have already fired the delta
				addDelta(delta);
				setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);
			}
		}
	}
	/**
	 * @see MultiOperation
	 * This method first group the elements by <code>ICompilationUnit</code>,
	 * and then processes the <code>ICompilationUnit</code>.
	 */
	@Override
	protected void processElements() throws JavaModelException {
		groupElements();
		super.processElements();
	}
	/**
	 * @see MultiOperation
	 */
	@Override
	protected void verify(IJavaElement element) throws JavaModelException {
		IJavaElement[] children = ((IRegion) this.childrenToRemove.get(element)).getElements();
		for (int i = 0; i < children.length; i++) {
			IJavaElement child = children[i];
			if (child.getCorrespondingResource() != null)
				error(IJavaModelStatusConstants.INVALID_ELEMENT_TYPES, child);

			if (child.isReadOnly())
				error(IJavaModelStatusConstants.READ_ONLY, child);
		}
	}
}
