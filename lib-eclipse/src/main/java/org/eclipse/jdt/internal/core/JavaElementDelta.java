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
 *     Vladimir Piskarev <pisv@1c.ru> - Building large Java element deltas is really slow - https://bugs.eclipse.org/443928
 *     Vladimir Piskarev <pisv@1c.ru> - F_CONTENT sometimes lost when merging deltas - https://bugs.eclipse.org/520336
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * @see IJavaElementDelta
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JavaElementDelta extends SimpleDelta implements IJavaElementDelta {
	/**
	 * @see #getAffectedChildren()
	 */
	IJavaElementDelta[] affectedChildren = EMPTY_DELTA;

	/*
	 * The AST created during the last reconcile operation.
	 * Non-null only iff:
	 * - in a POST_RECONCILE event
	 * - an AST was requested during the last reconcile operation
	 * - the changed element is an ICompilationUnit in working copy mode
	 */
	CompilationUnit ast = null;

	/*
	 * The element that this delta describes the change to.
	 */
	IJavaElement changedElement;

	/**
	 * Collection of resource deltas that correspond to non java resources deltas.
	 */
	IResourceDelta[] resourceDeltas = null;

	/**
	 * Counter of resource deltas
	 */
	int resourceDeltasCounter;

	/**
	 * @see #getMovedFromElement()
	 */
	IJavaElement movedFromHandle = null;

	/**
	 * @see #getMovedToElement()
	 */
	IJavaElement movedToHandle = null;

	IJavaElementDelta[] annotationDeltas = EMPTY_DELTA;

	/**
	 * Empty array of IJavaElementDelta
	 */
	static  IJavaElementDelta[] EMPTY_DELTA= new IJavaElementDelta[] {};

	/**
	 * Child index is needed iff affectedChildren.length >= NEED_CHILD_INDEX
	*/
	static int NEED_CHILD_INDEX = 3;

	/**
	 * On-demand index into affectedChildren
	 */
	Map<Key, Integer> childIndex;

	public boolean ignoreFromTests = false;

	/**
	 * The delta key
	 */
	protected static class Key {
		public final IJavaElement element;

		public Key(IJavaElement element) {
			this.element = element;
		}
		@Override
		public int hashCode() {
			return this.element.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Key))
				return false;
			return equalsAndSameParent(this.element, ((Key) obj).element);
		}
	}
/**
 * Creates the root delta. To create the nested delta
 * hierarchies use the following convenience methods. The root
 * delta can be created at any level (for example: project, package root,
 * package fragment...).
 * <ul>
 * <li><code>added(IJavaElement)</code>
 * <li><code>changed(IJavaElement)</code>
 * <li><code>moved(IJavaElement, IJavaElement)</code>
 * <li><code>removed(IJavaElement)</code>
 * <li><code>renamed(IJavaElement, IJavaElement)</code>
 * </ul>
 */
public JavaElementDelta(IJavaElement element) {
	this.changedElement = element;
}
/**
 * Adds the child delta to the collection of affected children.  If the
 * child is already in the collection, walk down the hierarchy.
 */
protected void addAffectedChild(JavaElementDelta child) {
	switch (this.kind) {
		case ADDED:
		case REMOVED:
			// no need to add a child if this parent is added or removed
			return;
		case CHANGED:
			this.changeFlags |= F_CHILDREN;
			break;
		default:
			this.kind = CHANGED;
			this.changeFlags |= F_CHILDREN;
	}

	// if a child delta is added to a compilation unit delta or below,
	// it's a fine grained delta
	if (this.changedElement.getElementType() >= IJavaElement.COMPILATION_UNIT) {
		fineGrained();
	}

	Key childKey = new Key(child.getElement());
	Integer existingChildIndex = getChildIndex(childKey);
	if (existingChildIndex == null) { //new affected child
		addNewChild(child);
	} else {
		JavaElementDelta existingChild = (JavaElementDelta) this.affectedChildren[existingChildIndex];
		switch (existingChild.getKind()) {
			case ADDED:
				switch (child.getKind()) {
					case ADDED: // child was added then added -> it is added
					case CHANGED: // child was added then changed -> it is added
						return;
					case REMOVED: // child was added then removed -> noop
						removeExistingChild(childKey, existingChildIndex);
						return;
				}
				break;
			case REMOVED:
				switch (child.getKind()) {
					case ADDED: // child was removed then added -> it is changed
						child.kind = CHANGED;
						this.affectedChildren[existingChildIndex] = child;
						return;
					case CHANGED: // child was removed then changed -> it is removed
					case REMOVED: // child was removed then removed -> it is removed
						return;
				}
				break;
			case CHANGED:
				switch (child.getKind()) {
					case ADDED: // child was changed then added -> it is added
					case REMOVED: // child was changed then removed -> it is removed
						this.affectedChildren[existingChildIndex] = child;
						return;
					case CHANGED: // child was changed then changed -> it is changed
						IJavaElementDelta[] children = child.getAffectedChildren();
						for (int i = 0; i < children.length; i++) {
							JavaElementDelta childsChild = (JavaElementDelta) children[i];
							existingChild.addAffectedChild(childsChild);
						}

						// update flags
						int flags = child.changeFlags;
						// case of fine grained delta (existing child) and delta coming from
						// DeltaProcessor (child): ensure F_CONTENT is not propagated from child
						if ((existingChild.changeFlags & F_FINE_GRAINED) != 0 && (flags & F_FINE_GRAINED) == 0) {
							flags &= ~F_CONTENT;
						}
						existingChild.changeFlags |= flags;

						// add the non-java resource deltas if needed
						// note that the child delta always takes precedence over this existing child delta
						// as non-java resource deltas are always created last (by the DeltaProcessor)
						IResourceDelta[] resDeltas = child.getResourceDeltas();
						if (resDeltas != null) {
							existingChild.resourceDeltas = resDeltas;
							existingChild.resourceDeltasCounter = child.resourceDeltasCounter;
						}

						return;
				}
				break;
			default:
				// unknown -> existing child becomes the child with the existing child's flags
				int flags = existingChild.getFlags();
				this.affectedChildren[existingChildIndex] = child;
				child.changeFlags |= flags;
		}
	}
}
/**
 * Creates the nested deltas resulting from an add operation.
 * Convenience method for creating add deltas.
 * The constructor should be used to create the root delta
 * and then an add operation should call this method.
 */
public void added(IJavaElement element) {
	added(element, 0);
}
public void added(IJavaElement element, int flags) {
	JavaElementDelta addedDelta = new JavaElementDelta(element);
	addedDelta.added();
	addedDelta.changeFlags |= flags;
	insertDeltaTree(element, addedDelta);
}
/**
 * Adds the new child delta to the collection of affected children.
 */
protected void addNewChild(JavaElementDelta child) {
	this.affectedChildren = growAndAddToArray(this.affectedChildren, child);
	if (this.childIndex != null) {
		this.childIndex.put(new Key(child.getElement()), this.affectedChildren.length - 1);
	}
}
/**
 * Adds the child delta to the collection of affected children.  If the
 * child is already in the collection, walk down the hierarchy.
 */
protected void addResourceDelta(IResourceDelta child) {
	switch (this.kind) {
		case ADDED:
		case REMOVED:
			// no need to add a child if this parent is added or removed
			return;
		case CHANGED:
			this.changeFlags |= F_CONTENT;
			break;
		default:
			this.kind = CHANGED;
			this.changeFlags |= F_CONTENT;
	}
	if (this.resourceDeltas == null) {
		this.resourceDeltas = new IResourceDelta[5];
		this.resourceDeltas[this.resourceDeltasCounter++] = child;
		return;
	}
	if (this.resourceDeltas.length == this.resourceDeltasCounter) {
		// need a resize
		System.arraycopy(this.resourceDeltas, 0, (this.resourceDeltas = new IResourceDelta[this.resourceDeltasCounter * 2]), 0, this.resourceDeltasCounter);
	}
	this.resourceDeltas[this.resourceDeltasCounter++] = child;
}
/**
 * Creates the nested deltas resulting from a change operation.
 * Convenience method for creating change deltas.
 * The constructor should be used to create the root delta
 * and then a change operation should call this method.
 */
public JavaElementDelta changed(IJavaElement element, int changeFlag) {
	JavaElementDelta changedDelta = new JavaElementDelta(element);
	changedDelta.changed(changeFlag);
	insertDeltaTree(element, changedDelta);
	return changedDelta;
}
/*
 * Records the last changed AST  .
 */
public void changedAST(CompilationUnit changedAST) {
	this.ast = changedAST;
	changed(F_AST_AFFECTED);
}
/**
 * Clears the collection of affected children.
 */
protected void clearAffectedChildren() {
	this.affectedChildren = EMPTY_DELTA;
	this.childIndex = null;
}
/**
 * Mark this delta as a content changed delta.
 */
public void contentChanged() {
	this.changeFlags |= F_CONTENT;
}
/**
 * Creates the nested deltas for a closed element.
 */
public void closed(IJavaElement element) {
	JavaElementDelta delta = new JavaElementDelta(element);
	delta.changed(F_CLOSED);
	insertDeltaTree(element, delta);
}
/**
 * Creates the nested delta deltas based on the affected element
 * its delta, and the root of this delta tree. Returns the root
 * of the created delta tree.
 */
protected JavaElementDelta createDeltaTree(IJavaElement element, JavaElementDelta delta) {
	JavaElementDelta childDelta = delta;
	ArrayList ancestors= getAncestors(element);
	if (ancestors == null) {
		if (equalsAndSameParent(delta.getElement(), getElement())) { // handle case of two jars that can be equals but not in the same project
			// the element being changed is the root element
			this.kind= delta.kind;
			this.changeFlags = delta.changeFlags;
			this.movedToHandle = delta.movedToHandle;
			this.movedFromHandle = delta.movedFromHandle;
		}
	} else {
		for (int i = 0, size = ancestors.size(); i < size; i++) {
			IJavaElement ancestor = (IJavaElement) ancestors.get(i);
			JavaElementDelta ancestorDelta = new JavaElementDelta(ancestor);
			ancestorDelta.addAffectedChild(childDelta);
			childDelta = ancestorDelta;
		}
	}
	return childDelta;
}
/**
 * Returns whether the two java elements are equals and have the same parent.
 */
protected static boolean equalsAndSameParent(IJavaElement e1, IJavaElement e2) {
	IJavaElement parent1;
	return e1.equals(e2) && ((parent1 = e1.getParent()) != null) && parent1.equals(e2.getParent());
}
/**
 * Returns the <code>JavaElementDelta</code> for the given element
 * in the delta tree, or null, if no delta for the given element is found.
 */
protected JavaElementDelta find(IJavaElement e) {
	if (equalsAndSameParent(getElement(), e)) // handle case of two jars that can be equals but not in the same project
		return this;
	return findDescendant(new Key(e));
}
/**
 * Returns the descendant delta for the given key, or <code>null</code>,
 * if no delta for the given key is found in the delta tree below this delta.
 */
protected JavaElementDelta findDescendant(Key key) {
	if (this.affectedChildren.length == 0)
		return null;
	Integer index = getChildIndex(key);
	if (index != null)
		return (JavaElementDelta) this.affectedChildren[index];
	for (IJavaElementDelta child : this.affectedChildren) {
		JavaElementDelta delta = ((JavaElementDelta) child).findDescendant(key);
		if (delta != null)
			return delta;
	}
	return null;
}
/**
 * Mark this delta as a fine-grained delta.
 */
public void fineGrained() {
	changed(F_FINE_GRAINED);
}
/**
 * @see IJavaElementDelta
 */
@Override
public IJavaElementDelta[] getAddedChildren() {
	return getChildrenOfType(ADDED);
}
/**
 * @see IJavaElementDelta
 */
@Override
public IJavaElementDelta[] getAffectedChildren() {
	return this.affectedChildren;
}
/**
 * Returns a collection of all the parents of this element up to (but
 * not including) the root of this tree in bottom-up order. If the given
 * element is not a descendant of the root of this tree, <code>null</code>
 * is returned.
 */
private ArrayList getAncestors(IJavaElement element) {
	IJavaElement parent = element.getParent();
	if (parent == null) {
		return null;
	}
	ArrayList parents = new ArrayList();
	while (!parent.equals(this.changedElement)) {
		parents.add(parent);
		parent = parent.getParent();
		if (parent == null) {
			return null;
		}
	}
	parents.trimToSize();
	return parents;
}
@Override
public CompilationUnit getCompilationUnitAST() {
	return this.ast;
}
@Override
public IJavaElementDelta[] getAnnotationDeltas() {
	return this.annotationDeltas;
}
/**
 * @see IJavaElementDelta
 */
@Override
public IJavaElementDelta[] getChangedChildren() {
	return getChildrenOfType(CHANGED);
}
/**
 * Returns the index of the delta in the collection of affected children,
 * or <code>null</code> if the child delta for the given key is not found.
 */
protected Integer getChildIndex(Key key) {
	int length = this.affectedChildren.length;
	if (length < NEED_CHILD_INDEX) {
		for (int i = 0; i < length; i++) {
			if (equalsAndSameParent(key.element, this.affectedChildren[i].getElement())) {
				return i;
			}
		}
		return null;
	}
	if (this.childIndex == null) {
		this.childIndex = new HashMap<Key, Integer>();
		for (int i = 0; i < length; i++) {
			this.childIndex.put(new Key(this.affectedChildren[i].getElement()), i);
		}
	}
	return this.childIndex.get(key);
}
/**
 * @see IJavaElementDelta
 */
protected IJavaElementDelta[] getChildrenOfType(int type) {
	int length = this.affectedChildren.length;
	if (length == 0) {
		return new IJavaElementDelta[] {};
	}
	ArrayList children= new ArrayList(length);
	for (int i = 0; i < length; i++) {
		if (this.affectedChildren[i].getKind() == type) {
			children.add(this.affectedChildren[i]);
		}
	}

	IJavaElementDelta[] childrenOfType = new IJavaElementDelta[children.size()];
	children.toArray(childrenOfType);

	return childrenOfType;
}
/**
 * Returns the delta for a given element.  Only looks below this
 * delta.
 */
protected JavaElementDelta getDeltaFor(IJavaElement element) {
	return find(element);
}
/**
 * @see IJavaElementDelta
 */
@Override
public IJavaElement getElement() {
	return this.changedElement;
}
/**
 * @see IJavaElementDelta
 */
@Override
public IJavaElement getMovedFromElement() {
	return this.movedFromHandle;
}
/**
 * @see IJavaElementDelta
 */
@Override
public IJavaElement getMovedToElement() {
	return this.movedToHandle;
}
/**
 * @see IJavaElementDelta
 */
@Override
public IJavaElementDelta[] getRemovedChildren() {
	return getChildrenOfType(REMOVED);
}
/**
 * Return the collection of resource deltas. Return null if none.
 */
@Override
public IResourceDelta[] getResourceDeltas() {
	if (this.resourceDeltas == null) return null;
	if (this.resourceDeltas.length != this.resourceDeltasCounter) {
		System.arraycopy(this.resourceDeltas, 0, this.resourceDeltas = new IResourceDelta[this.resourceDeltasCounter], 0, this.resourceDeltasCounter);
	}
	return this.resourceDeltas;
}
/**
 * Adds the new element to a new array that contains all of the elements of the old array.
 * Returns the new array.
 */
protected IJavaElementDelta[] growAndAddToArray(IJavaElementDelta[] array, IJavaElementDelta addition) {
	IJavaElementDelta[] old = array;
	array = new IJavaElementDelta[old.length + 1];
	System.arraycopy(old, 0, array, 0, old.length);
	array[old.length] = addition;
	return array;
}
/**
 * Creates the delta tree for the given element and delta, and then
 * inserts the tree as an affected child of this node.
 */
protected void insertDeltaTree(IJavaElement element, JavaElementDelta delta) {
	JavaElementDelta childDelta= createDeltaTree(element, delta);
	if (!equalsAndSameParent(element, getElement())) { // handle case of two jars that can be equals but not in the same project
		addAffectedChild(childDelta);
	}
}
/**
 * Creates the nested deltas resulting from an move operation.
 * Convenience method for creating the "move from" delta.
 * The constructor should be used to create the root delta
 * and then the move operation should call this method.
 */
public void movedFrom(IJavaElement movedFromElement, IJavaElement movedToElement) {
	JavaElementDelta removedDelta = new JavaElementDelta(movedFromElement);
	removedDelta.kind = REMOVED;
	removedDelta.changeFlags |= F_MOVED_TO;
	removedDelta.movedToHandle = movedToElement;
	insertDeltaTree(movedFromElement, removedDelta);
}
/**
 * Creates the nested deltas resulting from an move operation.
 * Convenience method for creating the "move to" delta.
 * The constructor should be used to create the root delta
 * and then the move operation should call this method.
 */
public void movedTo(IJavaElement movedToElement, IJavaElement movedFromElement) {
	JavaElementDelta addedDelta = new JavaElementDelta(movedToElement);
	addedDelta.kind = ADDED;
	addedDelta.changeFlags |= F_MOVED_FROM;
	addedDelta.movedFromHandle = movedFromElement;
	insertDeltaTree(movedToElement, addedDelta);
}
/**
 * Creates the nested deltas for an opened element.
 */
public void opened(IJavaElement element) {
	JavaElementDelta delta = new JavaElementDelta(element);
	delta.changed(F_OPENED);
	insertDeltaTree(element, delta);
}
/**
 * Removes the child delta from the collection of affected children.
 */
protected void removeAffectedChild(JavaElementDelta child) {
	if (this.affectedChildren.length == 0)
		return;
	Key childKey = new Key(child.getElement());
	Integer exisingChildIndex = getChildIndex(childKey);
	if (exisingChildIndex != null) {
		removeExistingChild(childKey, exisingChildIndex);
	}
}
/**
 * Removes the element from the array.
 * Returns the a new array which has shrunk.
 */
protected IJavaElementDelta[] removeAndShrinkArray(IJavaElementDelta[] old, int index) {
	IJavaElementDelta[] array = new IJavaElementDelta[old.length - 1];
	if (index > 0)
		System.arraycopy(old, 0, array, 0, index);
	int rest = old.length - index - 1;
	if (rest > 0)
		System.arraycopy(old, index + 1, array, index, rest);
	return array;
}
/**
 * Creates the nested deltas resulting from an delete operation.
 * Convenience method for creating removed deltas.
 * The constructor should be used to create the root delta
 * and then the delete operation should call this method.
 */
public void removed(IJavaElement element) {
	removed(element, 0);
}
public void removed(IJavaElement element, int flags) {
	JavaElementDelta removedDelta= new JavaElementDelta(element);
	insertDeltaTree(element, removedDelta);
	JavaElementDelta actualDelta = getDeltaFor(element);
	if (actualDelta != null) {
		actualDelta.removed();
		actualDelta.changeFlags |= flags;
		actualDelta.clearAffectedChildren();
	}
}
/**
 * Removes the existing child delta from the collection of affected children.
 */
protected void removeExistingChild(Key key, int index) {
	this.affectedChildren = removeAndShrinkArray(this.affectedChildren, index);
	if (this.childIndex != null) {
		int length = this.affectedChildren.length;
		if (length < NEED_CHILD_INDEX)
			this.childIndex = null;
		else {
			this.childIndex.remove(key);
			for (int i = index; i < length; i++) {
				this.childIndex.put(new Key(this.affectedChildren[i].getElement()), i);
			}
		}
	}
}
/**
 * Creates the nested deltas resulting from a change operation.
 * Convenience method for creating change deltas.
 * The constructor should be used to create the root delta
 * and then a change operation should call this method.
 */
public void sourceAttached(IJavaElement element) {
	JavaElementDelta attachedDelta = new JavaElementDelta(element);
	attachedDelta.changed(F_SOURCEATTACHED);
	insertDeltaTree(element, attachedDelta);
}
/**
 * Creates the nested deltas resulting from a change operation.
 * Convenience method for creating change deltas.
 * The constructor should be used to create the root delta
 * and then a change operation should call this method.
 */
public void sourceDetached(IJavaElement element) {
	JavaElementDelta detachedDelta = new JavaElementDelta(element);
	detachedDelta.changed(F_SOURCEDETACHED);
	insertDeltaTree(element, detachedDelta);
}
/**
 * Returns a string representation of this delta's
 * structure suitable for debug purposes.
 *
 * @see #toString()
 */
public String toDebugString(int depth) {
	StringBuffer buffer = new StringBuffer();
	for (int i= 0; i < depth; i++) {
		buffer.append('\t');
	}
	buffer.append(((JavaElement)getElement()).toDebugString());
	toDebugString(buffer);
	IJavaElementDelta[] children = getAffectedChildren();
	if (children != null) {
		for (int i = 0; i < children.length; ++i) {
			buffer.append("\n"); //$NON-NLS-1$
			buffer.append(((JavaElementDelta) children[i]).toDebugString(depth + 1));
		}
	}
	for (int i = 0; i < this.resourceDeltasCounter; i++) {
		buffer.append("\n");//$NON-NLS-1$
		for (int j = 0; j < depth+1; j++) {
			buffer.append('\t');
		}
		IResourceDelta resourceDelta = this.resourceDeltas[i];
		buffer.append(resourceDelta.toString());
		buffer.append("["); //$NON-NLS-1$
		switch (resourceDelta.getKind()) {
			case IResourceDelta.ADDED :
				buffer.append('+');
				break;
			case IResourceDelta.REMOVED :
				buffer.append('-');
				break;
			case IResourceDelta.CHANGED :
				buffer.append('*');
				break;
			default :
				buffer.append('?');
				break;
		}
		buffer.append("]"); //$NON-NLS-1$
	}
	IJavaElementDelta[] annotations = getAnnotationDeltas();
	if (annotations != null) {
		for (int i = 0; i < annotations.length; ++i) {
			buffer.append("\n"); //$NON-NLS-1$
			buffer.append(((JavaElementDelta) annotations[i]).toDebugString(depth + 1));
		}
	}
	return buffer.toString();
}
@Override
protected boolean toDebugString(StringBuffer buffer, int flags) {
	boolean prev = super.toDebugString(buffer, flags);

	if ((flags & IJavaElementDelta.F_CHILDREN) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("CHILDREN"); //$NON-NLS-1$
		prev = true;
	}
	if ((flags & IJavaElementDelta.F_CONTENT) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("CONTENT"); //$NON-NLS-1$
		prev = true;
	}
	if ((flags & IJavaElementDelta.F_MOVED_FROM) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("MOVED_FROM(" + ((JavaElement)getMovedFromElement()).toStringWithAncestors() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		prev = true;
	}
	if ((flags & IJavaElementDelta.F_MOVED_TO) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("MOVED_TO(" + ((JavaElement)getMovedToElement()).toStringWithAncestors() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		prev = true;
	}
	if ((flags & IJavaElementDelta.F_ADDED_TO_CLASSPATH) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("ADDED TO CLASSPATH"); //$NON-NLS-1$
		prev = true;
	}
	if ((flags & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("REMOVED FROM CLASSPATH"); //$NON-NLS-1$
		prev = true;
	}
	if ((flags & IJavaElementDelta.F_REORDER) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("REORDERED"); //$NON-NLS-1$
		prev = true;
	}
	if ((flags & IJavaElementDelta.F_ARCHIVE_CONTENT_CHANGED) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("ARCHIVE CONTENT CHANGED"); //$NON-NLS-1$
		prev = true;
	}
	if ((flags & IJavaElementDelta.F_SOURCEATTACHED) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("SOURCE ATTACHED"); //$NON-NLS-1$
		prev = true;
	}
	if ((flags & IJavaElementDelta.F_SOURCEDETACHED) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("SOURCE DETACHED"); //$NON-NLS-1$
		prev = true;
	}
	if ((flags & IJavaElementDelta.F_FINE_GRAINED) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("FINE GRAINED"); //$NON-NLS-1$
		prev = true;
	}
	if ((flags & IJavaElementDelta.F_PRIMARY_WORKING_COPY) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("PRIMARY WORKING COPY"); //$NON-NLS-1$
		prev = true;
	}
	if ((flags & IJavaElementDelta.F_CLASSPATH_CHANGED) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("RAW CLASSPATH CHANGED"); //$NON-NLS-1$
		prev = true;
	}
	if ((flags & IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("RESOLVED CLASSPATH CHANGED"); //$NON-NLS-1$
		prev = true;
	}
	if ((flags & IJavaElementDelta.F_PRIMARY_RESOURCE) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("PRIMARY RESOURCE"); //$NON-NLS-1$
		prev = true;
	}
	if ((flags & IJavaElementDelta.F_OPENED) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("OPENED"); //$NON-NLS-1$
		prev = true;
	}
	if ((flags & IJavaElementDelta.F_CLOSED) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("CLOSED"); //$NON-NLS-1$
		prev = true;
	}
	if ((flags & IJavaElementDelta.F_AST_AFFECTED) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("AST AFFECTED"); //$NON-NLS-1$
		prev = true;
	}
	if ((flags & IJavaElementDelta.F_CATEGORIES) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("CATEGORIES"); //$NON-NLS-1$
		prev = true;
	}
	if ((flags & IJavaElementDelta.F_ANNOTATIONS) != 0) {
		if (prev)
			buffer.append(" | "); //$NON-NLS-1$
		buffer.append("ANNOTATIONS"); //$NON-NLS-1$
		prev = true;
	}
	return prev;
}
/**
 * Returns a string representation of this delta's
 * structure suitable for debug purposes.
 */
@Override
public String toString() {
	return toDebugString(0);
}
}
