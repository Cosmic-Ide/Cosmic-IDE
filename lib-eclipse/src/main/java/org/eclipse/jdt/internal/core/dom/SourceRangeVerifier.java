/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.dom;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.internal.core.dom.util.DOMASTUtil;

@SuppressWarnings("rawtypes")
public class SourceRangeVerifier extends ASTVisitor {

	public static boolean DEBUG = false;
	public static boolean DEBUG_THROW = false;

	private StringBuffer bugs;

	/**
	 * Verifies proper node nesting as specified in {@link ASTParser#setKind(int)}:
	 * <p>
	 * Source ranges nest properly: the source range for a child is always
	 * within the source range of its parent, and the source ranges of sibling
	 * nodes never overlap.
	 * </p>
	 *
	 * @param node
	 * @return <code>null</code> if everything is OK; a list of errors otherwise
	 */
	public String process(ASTNode node) {
		StringBuffer buffer = new StringBuffer();
		this.bugs = buffer;
		node.accept(this);
		this.bugs = null;
		if (buffer.length() == 0)
			return null;
		return buffer.toString();
	}

	@Override
	public boolean preVisit2(ASTNode node) {
		ASTNode previous = null;

		List properties = node.structuralPropertiesForType();
		for (int i = 0; i < properties.size(); i++) {
			StructuralPropertyDescriptor property = (StructuralPropertyDescriptor) properties.get(i);
			if (property.isChildProperty()) {
				ASTNode child = (ASTNode) node.getStructuralProperty(property);
				if (child != null) {
					boolean ok = checkChild(node, previous, child);
					if (ok) {
						previous = child;
					} else {
						return false;
					}
				}
			} else if (property.isChildListProperty()) {
				List children = (List) node.getStructuralProperty(property);
				for (int j= 0; j < children.size(); j++) {
					ASTNode child = (ASTNode) children.get(j);
					boolean ok = checkChild(node, previous, child);
					if (ok) {
						previous = child;
					} else {
						return false;
					}
				}
			}
		}
		return true;
	}

	private boolean checkChild(ASTNode parent, ASTNode previous, ASTNode child) {
		if ((parent.getFlags() & (ASTNode.RECOVERED | ASTNode.MALFORMED)) != 0
				|| (child.getFlags() & (ASTNode.RECOVERED | ASTNode.MALFORMED)) != 0)
			return false;
		if (DOMASTUtil.isRecordDeclarationSupported(child.getAST()) && child instanceof SingleVariableDeclaration) {
			if (previous != null && previous instanceof MethodDeclaration && ((MethodDeclaration)previous).isCompactConstructor()) {
				return true; // For compact constructors, do not validate for parameters
			}
		}

		int parentStart = parent.getStartPosition();
		int parentEnd = parentStart + parent.getLength();

		int childStart = child.getStartPosition();
		int childEnd = childStart + child.getLength();

		if (previous != null) {
			// Turn a blind eye on a known problem ... see https://bugs.eclipse.org/391894#c4
			if (child.getLocationInParent() == ArrayCreation.DIMENSIONS_PROPERTY)
				return false;

			int previousStart = previous.getStartPosition();
			int previousEnd = previousStart + previous.getLength();
			if (childStart < previousEnd) {
				String bug = "- parent [" + parentStart + ", " + parentEnd + "] " + parent.getClass().getName() + '\n' //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						+ "   previous [" + previousStart + ", " + previousEnd + "] "  + previous.getClass().getName() + '\n'//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						+ "   " + child.getLocationInParent().getId() + " [" + childStart + ", " + childEnd + "] " + child.getClass().getName() + '\n'; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				this.bugs.append(bug);
			}
		}
		if (!(parentStart <= childStart && childEnd <= parentEnd)) {
			String bug = "- parent [" + parentStart + ", " + parentEnd + "] " + parent.getClass().getName() + '\n' //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					   + "   " + child.getLocationInParent().getId() + " [" + childStart + ", " + childEnd + "] " + child.getClass().getName() + '\n'; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			this.bugs.append(bug);
		}
		return true;
	}
}
