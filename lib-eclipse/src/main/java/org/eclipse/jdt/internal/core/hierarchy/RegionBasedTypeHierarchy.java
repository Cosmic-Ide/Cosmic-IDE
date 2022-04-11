/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.hierarchy;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.core.Openable;
import org.eclipse.jdt.internal.core.Region;
import org.eclipse.jdt.internal.core.TypeVector;

public class RegionBasedTypeHierarchy extends TypeHierarchy {
	/**
	 * The region of types for which to build the hierarchy
	 */
	protected IRegion region;

/**
 * Creates a TypeHierarchy on the types in the specified region,
 * considering first the given working copies,
 * using the projects in the given region for a name lookup context. If a specific
 * type is also specified, the type hierarchy is pruned to only
 * contain the branch including the specified type.
 */
public RegionBasedTypeHierarchy(IRegion region, ICompilationUnit[] workingCopies, IType type, boolean computeSubtypes) {
	super(type, workingCopies, (IJavaSearchScope)null, computeSubtypes);

	Region newRegion = new Region();
	IJavaElement[] elements = region.getElements();
	for (int i = 0, length = elements.length; i < length; i++) {
		newRegion.add(elements[i]);

	}
	this.region = newRegion;
	if (elements.length > 0)
		this.project = elements[0].getJavaProject();
}
/*
 * @see TypeHierarchy#initializeRegions
 */
@Override
protected void initializeRegions() {
	super.initializeRegions();
	IJavaElement[] roots = this.region.getElements();
	for (int i = 0; i < roots.length; i++) {
		IJavaElement root = roots[i];
		if (root instanceof IOpenable) {
			this.files.put((IOpenable) root, new ArrayList<IType>());
		} else {
			Openable o = (Openable) ((JavaElement) root).getOpenableParent();
			if (o != null) {
				this.files.put(o, new ArrayList<IType>());
			}
		}
		checkCanceled();
	}
}
/**
 * Compute this type hierarchy.
 */
@Override
protected void compute() throws JavaModelException, CoreException {
	HierarchyBuilder builder = new RegionBasedHierarchyBuilder(this);
	builder.build(this.computeSubtypes);
}
@Override
protected boolean isAffectedByOpenable(IJavaElementDelta delta, IJavaElement element, int eventType) {
	// change to working copy
	if (element instanceof CompilationUnit && ((CompilationUnit)element).isWorkingCopy()) {
		return super.isAffectedByOpenable(delta, element, eventType);
	}

	// if no focus, hierarchy is affected if the element is part of the region
	if (this.focusType == null) {
		return this.region.contains(element);
	} else {
		return super.isAffectedByOpenable(delta, element, eventType);
	}
}
/**
 * Returns the java project this hierarchy was created in.
 */
@Override
public IJavaProject javaProject() {
	return this.project;
}
public void pruneDeadBranches() {
	pruneDeadBranches(getRootClasses());
	pruneDeadBranches(getRootInterfaces());
}
/*
 * Returns whether all subtypes of the given type have been pruned.
 */
private boolean pruneDeadBranches(IType type) {
	TypeVector subtypes = this.typeToSubtypes.get(type);
	if (subtypes == null) return true;
	pruneDeadBranches(subtypes.copy().elements());
	subtypes = this.typeToSubtypes.get(type);
	return (subtypes == null || subtypes.size == 0);
}
private void pruneDeadBranches(IType[] types) {
	for (int i = 0, length = types.length; i < length; i++) {
		IType type = types[i];
		if (pruneDeadBranches(type) && !this.region.contains(type)) {
			removeType(type);
		}
	}
}
/**
 * Removes all the subtypes of the given type from the type hierarchy,
 * removes its superclass entry and removes the references from its super types.
 */
protected void removeType(IType type) {
	IType[] subtypes = getSubtypes(type);
	this.typeToSubtypes.remove(type);
	if (subtypes != null) {
		for (int i= 0; i < subtypes.length; i++) {
			removeType(subtypes[i]);
		}
	}
	IType superclass = this.classToSuperclass.remove(type);
	if (superclass != null) {
		TypeVector types = this.typeToSubtypes.get(superclass);
		if (types != null) types.remove(type);
	}
	IType[] superinterfaces = this.typeToSuperInterfaces.remove(type);
	if (superinterfaces != null) {
		for (int i = 0, length = superinterfaces.length; i < length; i++) {
			IType superinterface = superinterfaces[i];
			TypeVector types = this.typeToSubtypes.get(superinterface);
			if (types != null) types.remove(type);
		}
	}
	this.interfaces.remove(type);
}

}
