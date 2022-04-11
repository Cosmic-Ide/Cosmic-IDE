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
package org.eclipse.jdt.internal.core.dom.rewrite;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ListRewriteEvent extends RewriteEvent {

	public final static int NEW= 1;
	public final static int OLD= 2;
	public final static int BOTH= NEW | OLD;

	/** original list of 'ASTNode' */
	private List originalNodes;

	/** list of type 'RewriteEvent' */
	private List listEntries;

	/**
	 * Creates a ListRewriteEvent from the original ASTNodes. The resulting event
	 * represents the unmodified list.
	 * @param originalNodes The original nodes (type ASTNode)
	 */
	public ListRewriteEvent(List originalNodes) {
		this.originalNodes= new ArrayList(originalNodes);
	}

	/**
	 * Creates a ListRewriteEvent from existing rewrite events.
	 * @param children The rewrite events for this list.
	 */
	public ListRewriteEvent(RewriteEvent[] children) {
		this.listEntries= new ArrayList(children.length * 2);
		this.originalNodes= new ArrayList(children.length * 2);
		for (int i= 0; i < children.length; i++) {
			RewriteEvent curr= children[i];
			this.listEntries.add(curr);
			if (curr.getOriginalValue() != null) {
				this.originalNodes.add(curr.getOriginalValue());
			}
		}
	}

	private List getEntries() {
		if (this.listEntries == null) {
			// create if not yet existing
			int nNodes= this.originalNodes.size();
			this.listEntries= new ArrayList(nNodes * 2);
			for (int i= 0; i < nNodes; i++) {
				ASTNode node= (ASTNode) this.originalNodes.get(i);
				// all nodes unchanged
				this.listEntries.add(new NodeRewriteEvent(node, node));
			}
		}
		return this.listEntries;
	}

	@Override
	public int getChangeKind() {
		if (this.listEntries != null) {
			for (int i= 0; i < this.listEntries.size(); i++) {
				RewriteEvent curr= (RewriteEvent) this.listEntries.get(i);
				if (curr.getChangeKind() != UNCHANGED) {
					return CHILDREN_CHANGED;
				}
			}
		}
		return UNCHANGED;
	}

	@Override
	public boolean isListRewrite() {
		return true;
	}

	@Override
	public RewriteEvent[] getChildren() {
		List entries= getEntries();
		return (RewriteEvent[]) entries.toArray(new RewriteEvent[entries.size()]);
	}

	@Override
	public Object getOriginalValue() {
		return this.originalNodes;
	}

	@Override
	public Object getNewValue() {
		List entries= getEntries();
		ArrayList res= new ArrayList(entries.size());
		for (int i= 0; i < entries.size(); i++) {
			RewriteEvent curr= (RewriteEvent) entries.get(i);
			Object newVal= curr.getNewValue();
			if (newVal != null) {
				res.add(newVal);
			}
		}
		return res;
	}

	// API to modify the list nodes

	public RewriteEvent removeEntry(ASTNode originalEntry) {
		return replaceEntry(originalEntry, null);
	}

	public RewriteEvent replaceEntry(ASTNode entry, ASTNode newEntry) {
		if (entry == null) {
			throw new IllegalArgumentException();
		}

		List entries= getEntries();
		int nEntries= entries.size();
		for (int i= 0; i < nEntries; i++) {
			NodeRewriteEvent curr= (NodeRewriteEvent) entries.get(i);
			if (curr.getOriginalValue() == entry || curr.getNewValue() == entry) {
				curr.setNewValue(newEntry);
				if (curr.getNewValue() == null && curr.getOriginalValue() == null) { // removed an inserted node
					entries.remove(i);
					return null;
				}
				return curr;
			}
		}
		return null;
	}

	public void revertChange(NodeRewriteEvent event) {
		Object originalValue = event.getOriginalValue();
		if (originalValue == null) {
			List entries= getEntries();
			entries.remove(event);
		} else {
			event.setNewValue(originalValue);
		}
	}

	public int getIndex(ASTNode node, int kind) {
		List entries= getEntries();
		for (int i= entries.size() - 1; i >= 0; i--) {
			RewriteEvent curr= (RewriteEvent) entries.get(i);
			if (((kind & OLD) != 0) && (curr.getOriginalValue() == node)) {
				return i;
			}
			if (((kind & NEW) != 0) && (curr.getNewValue() == node)) {
				return i;
			}
		}
		return -1;
	}

	public RewriteEvent insert(ASTNode insertedNode, int insertIndex) {
		NodeRewriteEvent change= new NodeRewriteEvent(null, insertedNode);
		if (insertIndex != -1) {
			getEntries().add(insertIndex, change);
		} else {
			getEntries().add(change);
		}
		return change;
	}

	public void setNewValue(ASTNode newValue, int insertIndex) {
		NodeRewriteEvent curr= (NodeRewriteEvent) getEntries().get(insertIndex);
		curr.setNewValue(newValue);
	}

	public int getChangeKind(int index) {
		return ((NodeRewriteEvent) getEntries().get(index)).getChangeKind();
	}

	@Override
	public String toString() {
		StringBuilder buf= new StringBuilder();
		buf.append(" [list change\n\t"); //$NON-NLS-1$

		RewriteEvent[] events= getChildren();
		for (int i= 0; i < events.length; i++) {
			if (i != 0) {
				buf.append("\n\t"); //$NON-NLS-1$
			}
			buf.append(events[i]);
		}
		buf.append("\n]"); //$NON-NLS-1$
		return buf.toString();
	}



}
