/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * A java element delta builder creates a java element delta on
 * a java element between the version of the java element
 * at the time the comparator was created and the current version
 * of the java element.
 *
 * It performs this operation by locally caching the contents of
 * the java element when it is created. When the method
 * createDeltas() is called, it creates a delta over the cached
 * contents and the new contents.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class JavaElementDeltaBuilder {
	/**
	 * The java element handle
	 */
	IJavaElement javaElement;

	/**
	 * The maximum depth in the java element children we should look into
	 */
	int maxDepth = Integer.MAX_VALUE;

	/**
	 * The old handle to info relationships
	 */
	Map infos;
	Map annotationInfos;

	/**
	 * The old position info
	 */
	Map oldPositions;

	/**
	 * The new position info
	 */
	Map newPositions;

	/**
	 * Change delta
	 */
	public JavaElementDelta delta = null;

	/**
	 * List of added elements
	 */
	HashSet added;

	/**
	 * List of removed elements
	 */
	HashSet removed;

	/**
	 * Doubly linked list item
	 */
	static class ListItem {
		public IJavaElement previous;
		public IJavaElement next;

		public ListItem(IJavaElement previous, IJavaElement next) {
			this.previous = previous;
			this.next = next;
		}
	}
/**
 * Creates a java element comparator on a java element
 * looking as deep as necessary.
 */
public JavaElementDeltaBuilder(IJavaElement javaElement) {
	this.javaElement = javaElement;
	initialize();
	recordElementInfo(
		javaElement,
		(JavaModel)this.javaElement.getJavaModel(),
		0);
}
/**
 * Creates a java element comparator on a java element
 * looking only 'maxDepth' levels deep.
 */
public JavaElementDeltaBuilder(IJavaElement javaElement, int maxDepth) {
	this.javaElement = javaElement;
	this.maxDepth = maxDepth;
	initialize();
	recordElementInfo(
		javaElement,
		(JavaModel)this.javaElement.getJavaModel(),
		0);
}
/**
 * Repairs the positioning information
 * after an element has been added
 */
private void added(IJavaElement element) {
	this.added.add(element);
	ListItem current = getNewPosition(element);
	ListItem previous = null, next = null;
	if (current.previous != null)
		previous = getNewPosition(current.previous);
	if (current.next != null)
		next = getNewPosition(current.next);
	if (previous != null)
		previous.next = current.next;
	if (next != null)
		next.previous = current.previous;
}
/**
 * Builds the java element deltas between the old content of the compilation
 * unit and its new content.
 */
public void buildDeltas() {
	this.delta = new JavaElementDelta(this.javaElement);
	// if building a delta on a compilation unit or below,
	// it's a fine grained delta
	if (this.javaElement.getElementType() >= IJavaElement.COMPILATION_UNIT) {
		this.delta.fineGrained();
	}
	recordNewPositions(this.javaElement, 0);
	findAdditions(this.javaElement, 0);
	findDeletions();
	findChangesInPositioning(this.javaElement, 0);
	trimDelta(this.delta);
	if (this.delta.getAffectedChildren().length == 0) {
		// this is a fine grained but not children affected -> mark as content changed
		this.delta.contentChanged();
	}
}
private boolean equals(char[][][] first, char[][][] second) {
	if (first == second)
		return true;
	if (first == null || second == null)
		return false;
	if (first.length != second.length)
		return false;

	for (int i = first.length; --i >= 0;)
		if (!CharOperation.equals(first[i], second[i]))
			return false;
	return true;
}
/**
 * Finds elements which have been added or changed.
 */
private void findAdditions(IJavaElement newElement, int depth) {
	JavaElementInfo oldInfo = getElementInfo(newElement);
	if (oldInfo == null && depth < this.maxDepth) {
		this.delta.added(newElement);
		added(newElement);
	} else {
		removeElementInfo(newElement);
	}

	if (depth >= this.maxDepth) {
		// mark element as changed
		this.delta.changed(newElement, IJavaElementDelta.F_CONTENT);
		return;
	}

	JavaElementInfo newInfo = null;
	try {
		newInfo = (JavaElementInfo)((JavaElement)newElement).getElementInfo();
	} catch (JavaModelException npe) {
		return;
	}

	findContentChange(oldInfo, newInfo, newElement);

	if (oldInfo != null && newElement instanceof IParent) {

		IJavaElement[] children = newInfo.getChildren();
		if (children != null) {
			int length = children.length;
			for(int i = 0; i < length; i++) {
				findAdditions(children[i], depth + 1);
			}
		}
	}
}
/**
 * Looks for changed positioning of elements.
 */
private void findChangesInPositioning(IJavaElement element, int depth) {
	if (depth >= this.maxDepth || this.added.contains(element) || this.removed.contains(element))
		return;

	if (!isPositionedCorrectly(element)) {
		this.delta.changed(element, IJavaElementDelta.F_REORDER);
	}

	if (element instanceof IParent) {
		JavaElementInfo info = null;
		try {
			info = (JavaElementInfo)((JavaElement)element).getElementInfo();
		} catch (JavaModelException npe) {
			return;
		}

		IJavaElement[] children = info.getChildren();
		if (children != null) {
			int length = children.length;
			for(int i = 0; i < length; i++) {
				findChangesInPositioning(children[i], depth + 1);
			}
		}
	}
}
private void findAnnotationChanges(IAnnotation[] oldAnnotations, IAnnotation[] newAnnotations, IJavaElement parent) {
	ArrayList annotationDeltas = null;
	for (int i = 0, length = newAnnotations.length; i < length; i++) {
		IAnnotation newAnnotation = newAnnotations[i];
		Object oldInfo = this.annotationInfos.remove(newAnnotation);
		if (oldInfo == null) {
			JavaElementDelta annotationDelta = new JavaElementDelta(newAnnotation);
			annotationDelta.added();
			if (annotationDeltas == null) annotationDeltas = new ArrayList();
			annotationDeltas.add(annotationDelta);
			continue;
		} else {
			AnnotationInfo newInfo = null;
			try {
				newInfo = (AnnotationInfo) ((JavaElement) newAnnotation).getElementInfo();
			} catch (JavaModelException npe) {
				return;
			}
			if (!Util.equalArraysOrNull(((AnnotationInfo) oldInfo).members, newInfo.members)) {
				JavaElementDelta annotationDelta = new JavaElementDelta(newAnnotation);
				annotationDelta.changed(IJavaElementDelta.F_CONTENT);
				if (annotationDeltas == null) annotationDeltas = new ArrayList();
				annotationDeltas.add(annotationDelta);
			}
		}
	}
	for (int i = 0, length = oldAnnotations.length; i < length; i++) {
		IAnnotation oldAnnotation = oldAnnotations[i];
		if (this.annotationInfos.remove(oldAnnotation) != null) {
			JavaElementDelta annotationDelta = new JavaElementDelta(oldAnnotation);
			annotationDelta.removed();
			if (annotationDeltas == null) annotationDeltas = new ArrayList();
			annotationDeltas.add(annotationDelta);		}
	}
	if (annotationDeltas == null)
		return;
	int size = annotationDeltas.size();
	if (size > 0) {
		JavaElementDelta parentDelta = this.delta.changed(parent, IJavaElementDelta.F_ANNOTATIONS);
		parentDelta.annotationDeltas = (IJavaElementDelta[]) annotationDeltas.toArray(new IJavaElementDelta[size]);
	}
}
/**
 * The elements are equivalent, but might have content changes.
 */
private void findContentChange(JavaElementInfo oldInfo, JavaElementInfo newInfo, IJavaElement newElement) {
	if (oldInfo instanceof MemberElementInfo && newInfo instanceof MemberElementInfo) {
		if (((MemberElementInfo)oldInfo).getModifiers() != ((MemberElementInfo)newInfo).getModifiers()) {
			this.delta.changed(newElement, IJavaElementDelta.F_MODIFIERS);
		}
		if (oldInfo instanceof AnnotatableInfo && newInfo instanceof AnnotatableInfo) {
			findAnnotationChanges(((AnnotatableInfo) oldInfo).annotations, ((AnnotatableInfo) newInfo).annotations, newElement);
		}
		if (oldInfo instanceof SourceMethodElementInfo && newInfo instanceof SourceMethodElementInfo) {
			SourceMethodElementInfo oldSourceMethodInfo = (SourceMethodElementInfo)oldInfo;
			SourceMethodElementInfo newSourceMethodInfo = (SourceMethodElementInfo)newInfo;
			if (!CharOperation.equals(oldSourceMethodInfo.getReturnTypeName(), newSourceMethodInfo.getReturnTypeName())
					|| !CharOperation.equals(oldSourceMethodInfo.getTypeParameterNames(), newSourceMethodInfo.getTypeParameterNames())
					|| !equals(oldSourceMethodInfo.getTypeParameterBounds(), newSourceMethodInfo.getTypeParameterBounds())) {
				this.delta.changed(newElement, IJavaElementDelta.F_CONTENT);
			}
		} else if (oldInfo instanceof SourceFieldElementInfo && newInfo instanceof SourceFieldElementInfo) {
			if (!CharOperation.equals(
					((SourceFieldElementInfo)oldInfo).getTypeName(),
					((SourceFieldElementInfo)newInfo).getTypeName())) {
				this.delta.changed(newElement, IJavaElementDelta.F_CONTENT);
			}
		} else if (oldInfo instanceof SourceTypeElementInfo && newInfo instanceof SourceTypeElementInfo) {
			SourceTypeElementInfo oldSourceTypeInfo = (SourceTypeElementInfo)oldInfo;
			SourceTypeElementInfo newSourceTypeInfo = (SourceTypeElementInfo)newInfo;
			if (!CharOperation.equals(oldSourceTypeInfo.getSuperclassName(), newSourceTypeInfo.getSuperclassName())
					|| !CharOperation.equals(oldSourceTypeInfo.getInterfaceNames(), newSourceTypeInfo.getInterfaceNames())) {
				this.delta.changed(newElement, IJavaElementDelta.F_SUPER_TYPES);
			}
			if (!CharOperation.equals(oldSourceTypeInfo.getTypeParameterNames(), newSourceTypeInfo.getTypeParameterNames())
					|| !equals(oldSourceTypeInfo.getTypeParameterBounds(), newSourceTypeInfo.getTypeParameterBounds())) {
				this.delta.changed(newElement, IJavaElementDelta.F_CONTENT);
			}
			findCategoryChange(oldSourceTypeInfo.categories, newSourceTypeInfo.categories);
		} else if (oldInfo instanceof ModuleDescriptionInfo && newInfo instanceof ModuleDescriptionInfo) {
			ModuleDescriptionInfo oldSourceModuleInfo = (ModuleDescriptionInfo)oldInfo;
			ModuleDescriptionInfo newSourceModuleInfo = (ModuleDescriptionInfo)newInfo;
			findCategoryChange(oldSourceModuleInfo.getCategories(), newSourceModuleInfo.getCategories());
		}
	}
}
private void findCategoryChange(Map<IJavaElement, String[]> oldCategoriesMap, Map<IJavaElement, String[]> newCategoriesMap) {
	if (oldCategoriesMap != null) {
		// take the union of old and new categories elements (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=125675)
		Set elements;
		if (newCategoriesMap != null) {
			elements = new HashSet(oldCategoriesMap.keySet());
			elements.addAll(newCategoriesMap.keySet());
		} else
			elements = oldCategoriesMap.keySet();
		Iterator iterator = elements.iterator();
		while (iterator.hasNext()) {
			IJavaElement element = (IJavaElement) iterator.next();
			String[] oldCategories = oldCategoriesMap.get(element);
			String[] newCategories = newCategoriesMap == null ? null : (String[]) newCategoriesMap.get(element);
			if (!Util.equalArraysOrNull(oldCategories, newCategories)) {
				this.delta.changed(element, IJavaElementDelta.F_CATEGORIES);
			}
		}
	} else if (newCategoriesMap != null) {
		Iterator elements = newCategoriesMap.keySet().iterator();
		while (elements.hasNext()) {
			IJavaElement element = (IJavaElement) elements.next();
			this.delta.changed(element, IJavaElementDelta.F_CATEGORIES); // all categories for this element were removed
		}
	}
}
/**
 * Adds removed deltas for any handles left in the table
 */
private void findDeletions() {
	Iterator iter = this.infos.keySet().iterator();
	while(iter.hasNext()) {
		IJavaElement element = (IJavaElement)iter.next();
		this.delta.removed(element);
		removed(element);
	}
}
private JavaElementInfo getElementInfo(IJavaElement element) {
	return (JavaElementInfo)this.infos.get(element);
}
private ListItem getNewPosition(IJavaElement element) {
	return (ListItem)this.newPositions.get(element);
}
private ListItem getOldPosition(IJavaElement element) {
	return (ListItem)this.oldPositions.get(element);
}
private void initialize() {
	this.infos = new HashMap(20);
	this.oldPositions = new HashMap(20);
	this.newPositions = new HashMap(20);
	this.oldPositions.put(this.javaElement, new ListItem(null, null));
	this.newPositions.put(this.javaElement, new ListItem(null, null));
	this.added = new HashSet(5);
	this.removed = new HashSet(5);
}
/**
 * Inserts position information for the elements into the new or old positions table
 */
private void insertPositions(IJavaElement[] elements, boolean isNew) {
	int length = elements.length;
	IJavaElement previous = null, current = null, next = (length > 0) ? elements[0] : null;
	for(int i = 0; i < length; i++) {
		previous = current;
		current = next;
		next = (i + 1 < length) ? elements[i + 1] : null;
		if (isNew) {
			this.newPositions.put(current, new ListItem(previous, next));
		} else {
			this.oldPositions.put(current, new ListItem(previous, next));
		}
	}
}
/**
 * Returns whether the elements position has not changed.
 */
private boolean isPositionedCorrectly(IJavaElement element) {
	ListItem oldListItem = getOldPosition(element);
	if (oldListItem == null) return false;

	ListItem newListItem = getNewPosition(element);
	if (newListItem == null) return false;

	IJavaElement oldPrevious = oldListItem.previous;
	IJavaElement newPrevious = newListItem.previous;
	if (oldPrevious == null) {
		return newPrevious == null;
	} else {
		return oldPrevious.equals(newPrevious);
	}
}
/**
 * Records this elements info, and attempts
 * to record the info for the children.
 */
private void recordElementInfo(IJavaElement element, JavaModel model, int depth) {
	if (depth >= this.maxDepth) {
		return;
	}
	JavaElementInfo info = (JavaElementInfo)JavaModelManager.getJavaModelManager().getInfo(element);
	if (info == null) // no longer in the java model.
		return;
	this.infos.put(element, info);

	if (element instanceof IParent) {
		IJavaElement[] children = info.getChildren();
		if (children != null) {
			insertPositions(children, false);
			for(int i = 0, length = children.length; i < length; i++)
				recordElementInfo(children[i], model, depth + 1);
		}
	}
	IAnnotation[] annotations = null;
	if (info instanceof AnnotatableInfo)
		annotations = ((AnnotatableInfo) info).annotations;
	if (annotations != null) {
		if (this.annotationInfos == null)
			this.annotationInfos = new HashMap();
		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		for (int i = 0, length = annotations.length; i < length; i++) {
			this.annotationInfos.put(annotations[i], manager.getInfo(annotations[i]));
		}
	}
}
/**
 * Fills the newPositions hashtable with the new position information
 */
private void recordNewPositions(IJavaElement newElement, int depth) {
	if (depth < this.maxDepth && newElement instanceof IParent) {
		JavaElementInfo info = null;
		try {
			info = (JavaElementInfo)((JavaElement)newElement).getElementInfo();
		} catch (JavaModelException npe) {
			return;
		}

		IJavaElement[] children = info.getChildren();
		if (children != null) {
			insertPositions(children, true);
			for(int i = 0, length = children.length; i < length; i++) {
				recordNewPositions(children[i], depth + 1);
			}
		}
	}
}
/**
 * Repairs the positioning information
 * after an element has been removed
 */
private void removed(IJavaElement element) {
	this.removed.add(element);
	ListItem current = getOldPosition(element);
	ListItem previous = null, next = null;
	if (current.previous != null)
		previous = getOldPosition(current.previous);
	if (current.next != null)
		next = getOldPosition(current.next);
	if (previous != null)
		previous.next = current.next;
	if (next != null)
		next.previous = current.previous;

}
private void removeElementInfo(IJavaElement element) {
	this.infos.remove(element);
}
@Override
public String toString() {
	StringBuilder buffer = new StringBuilder();
	buffer.append("Built delta:\n"); //$NON-NLS-1$
	buffer.append(this.delta == null ? "<null>" : this.delta.toString()); //$NON-NLS-1$
	return buffer.toString();
}
/**
 * Trims deletion deltas to only report the highest level of deletion
 */
private void trimDelta(JavaElementDelta elementDelta) {
	if (elementDelta.getKind() == IJavaElementDelta.REMOVED) {
		elementDelta.clearAffectedChildren();
	} else {
		IJavaElementDelta[] children = elementDelta.getAffectedChildren();
		for(int i = 0, length = children.length; i < length; i++) {
			trimDelta((JavaElementDelta)children[i]);
		}
	}
}
}
