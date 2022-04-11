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
 *******************************************************************************/
package org.eclipse.jdt.internal.core.hierarchy;

import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.core.SimpleDelta;

/*
 * Collects changes (reported through fine-grained deltas) that can affect a type hierarchy.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ChangeCollector {

	/*
	 * A table from ITypes to TypeDeltas
	 */
	HashMap changes = new HashMap();

	TypeHierarchy hierarchy;

	public ChangeCollector(TypeHierarchy hierarchy) {
		this.hierarchy = hierarchy;
	}

	/*
	 * Adds the children of the given delta to the list of changes.
	 */
	private void addAffectedChildren(IJavaElementDelta delta) throws JavaModelException {
		IJavaElementDelta[] children = delta.getAffectedChildren();
		for (int i = 0, length = children.length; i < length; i++) {
			IJavaElementDelta child = children[i];
			IJavaElement childElement = child.getElement();
			switch (childElement.getElementType()) {
				case IJavaElement.IMPORT_CONTAINER:
					addChange((IImportContainer)childElement, child);
					break;
				case IJavaElement.IMPORT_DECLARATION:
					addChange((IImportDeclaration)childElement, child);
					break;
				case IJavaElement.TYPE:
					addChange((IType)childElement, child);
					break;
				case IJavaElement.INITIALIZER:
				case IJavaElement.FIELD:
				case IJavaElement.METHOD:
					addChange((IMember)childElement, child);
					break;
			}
		}
	}

	/*
	 * Adds the given delta on a compilation unit to the list of changes.
	 */
	public void addChange(ICompilationUnit cu, IJavaElementDelta newDelta) throws JavaModelException {
		int newKind = newDelta.getKind();
		switch (newKind) {
			case IJavaElementDelta.ADDED:
				ArrayList allTypes = new ArrayList();
				getAllTypesFromElement(cu, allTypes);
				for (int i = 0, length = allTypes.size(); i < length; i++) {
					IType type = (IType)allTypes.get(i);
					addTypeAddition(type, (SimpleDelta)this.changes.get(type));
				}
				break;
			case IJavaElementDelta.REMOVED:
				allTypes = new ArrayList();
				getAllTypesFromHierarchy((JavaElement)cu, allTypes);
				for (int i = 0, length = allTypes.size(); i < length; i++) {
					IType type = (IType)allTypes.get(i);
					addTypeRemoval(type, (SimpleDelta)this.changes.get(type));
				}
				break;
			case IJavaElementDelta.CHANGED:
				addAffectedChildren(newDelta);
				break;
		}
	}

	private void addChange(IImportContainer importContainer, IJavaElementDelta newDelta) throws JavaModelException {
		int newKind = newDelta.getKind();
		if (newKind == IJavaElementDelta.CHANGED) {
			addAffectedChildren(newDelta);
			return;
		}
		SimpleDelta existingDelta = (SimpleDelta)this.changes.get(importContainer);
		if (existingDelta != null) {
			switch (newKind) {
				case IJavaElementDelta.ADDED:
					if (existingDelta.getKind() == IJavaElementDelta.REMOVED) {
						// REMOVED then ADDED
						this.changes.remove(importContainer);
					}
					break;
				case IJavaElementDelta.REMOVED:
					if (existingDelta.getKind() == IJavaElementDelta.ADDED) {
						// ADDED then REMOVED
						this.changes.remove(importContainer);
					}
					break;
					// CHANGED handled above
			}
		} else {
			SimpleDelta delta = new SimpleDelta();
			switch (newKind) {
				case IJavaElementDelta.ADDED:
					delta.added();
					break;
				case IJavaElementDelta.REMOVED:
					delta.removed();
					break;
			}
			this.changes.put(importContainer, delta);
		}
	}

	private void addChange(IImportDeclaration importDecl, IJavaElementDelta newDelta) {
		SimpleDelta existingDelta = (SimpleDelta)this.changes.get(importDecl);
		int newKind = newDelta.getKind();
		if (existingDelta != null) {
			switch (newKind) {
				case IJavaElementDelta.ADDED:
					if (existingDelta.getKind() == IJavaElementDelta.REMOVED) {
						// REMOVED then ADDED
						this.changes.remove(importDecl);
					}
					break;
				case IJavaElementDelta.REMOVED:
					if (existingDelta.getKind() == IJavaElementDelta.ADDED) {
						// ADDED then REMOVED
						this.changes.remove(importDecl);
					}
					break;
				// CHANGED cannot happen for import declaration
			}
		} else {
			SimpleDelta delta = new SimpleDelta();
			switch (newKind) {
				case IJavaElementDelta.ADDED:
					delta.added();
					break;
				case IJavaElementDelta.REMOVED:
					delta.removed();
					break;
			}
			this.changes.put(importDecl, delta);
		}
	}

	/*
	 * Adds a change for the given member (a method, a field or an initializer) and the types it defines.
	 */
	private void addChange(IMember member, IJavaElementDelta newDelta) throws JavaModelException {
		int newKind = newDelta.getKind();
		switch (newKind) {
			case IJavaElementDelta.ADDED:
				ArrayList allTypes = new ArrayList();
				getAllTypesFromElement(member, allTypes);
				for (int i = 0, length = allTypes.size(); i < length; i++) {
					IType innerType = (IType)allTypes.get(i);
					addTypeAddition(innerType, (SimpleDelta)this.changes.get(innerType));
				}
				break;
			case IJavaElementDelta.REMOVED:
				allTypes = new ArrayList();
				getAllTypesFromHierarchy((JavaElement)member, allTypes);
				for (int i = 0, length = allTypes.size(); i < length; i++) {
					IType type = (IType)allTypes.get(i);
					addTypeRemoval(type, (SimpleDelta)this.changes.get(type));
				}
				break;
			case IJavaElementDelta.CHANGED:
				addAffectedChildren(newDelta);
				break;
		}
	}

	/*
	 * Adds a change for the given type and the types it defines.
	 */
	private void addChange(IType type, IJavaElementDelta newDelta) throws JavaModelException {
		 int newKind = newDelta.getKind();
		SimpleDelta existingDelta = (SimpleDelta)this.changes.get(type);
		switch (newKind) {
			case IJavaElementDelta.ADDED:
				addTypeAddition(type, existingDelta);
				ArrayList allTypes = new ArrayList();
				getAllTypesFromElement(type, allTypes);
				for (int i = 0, length = allTypes.size(); i < length; i++) {
					IType innerType = (IType)allTypes.get(i);
					addTypeAddition(innerType, (SimpleDelta)this.changes.get(innerType));
				}
				break;
			case IJavaElementDelta.REMOVED:
				addTypeRemoval(type, existingDelta);
				allTypes = new ArrayList();
				getAllTypesFromHierarchy((JavaElement)type, allTypes);
				for (int i = 0, length = allTypes.size(); i < length; i++) {
					IType innerType = (IType)allTypes.get(i);
					addTypeRemoval(innerType, (SimpleDelta)this.changes.get(innerType));
				}
				break;
			case IJavaElementDelta.CHANGED:
				addTypeChange(type, newDelta.getFlags(), existingDelta);
				addAffectedChildren(newDelta);
				break;
		}
	}

	private void addTypeAddition(IType type, SimpleDelta existingDelta) throws JavaModelException {
		if (existingDelta != null) {
			switch (existingDelta.getKind()) {
				case IJavaElementDelta.REMOVED:
					// REMOVED then ADDED
					boolean hasChange = false;
					if (hasSuperTypeChange(type)) {
						existingDelta.superTypes();
						hasChange = true;
					}
					if (hasVisibilityChange(type)) {
						existingDelta.modifiers();
						hasChange = true;
					}
					if (!hasChange) {
						this.changes.remove(type);
					}
					break;
					// CHANGED then ADDED
					// or ADDED then ADDED: should not happen
			}
		} else {
			// check whether the type addition affects the hierarchy
			String typeName = type.getElementName();
			if (this.hierarchy.hasSupertype(typeName)
					|| this.hierarchy.subtypesIncludeSupertypeOf(type)
					|| this.hierarchy.missingTypes.contains(typeName)) {
				SimpleDelta delta = new SimpleDelta();
				delta.added();
				this.changes.put(type, delta);
			}
		}
	}

	private void addTypeChange(IType type, int newFlags, SimpleDelta existingDelta) throws JavaModelException {
		if (existingDelta != null) {
			switch (existingDelta.getKind()) {
				case IJavaElementDelta.CHANGED:
					// CHANGED then CHANGED
					int existingFlags = existingDelta.getFlags();
					boolean hasChange = false;
					if ((existingFlags & IJavaElementDelta.F_SUPER_TYPES) != 0
							&& hasSuperTypeChange(type)) {
						existingDelta.superTypes();
						hasChange = true;
					}
					if ((existingFlags & IJavaElementDelta.F_MODIFIERS) != 0
							&& hasVisibilityChange(type)) {
						existingDelta.modifiers();
						hasChange = true;
					}
					if (!hasChange) {
						// super types and visibility are back to the ones in the existing hierarchy
						this.changes.remove(type);
					}
					break;
					// ADDED then CHANGED: leave it as ADDED
					// REMOVED then CHANGED: should not happen
			}
		} else {
			// check whether the type change affects the hierarchy
			SimpleDelta typeDelta = null;
			if ((newFlags & IJavaElementDelta.F_SUPER_TYPES) != 0
					&& this.hierarchy.includesTypeOrSupertype(type)) {
				typeDelta = new SimpleDelta();
				typeDelta.superTypes();
			}
			if ((newFlags & IJavaElementDelta.F_MODIFIERS) != 0
					&& (this.hierarchy.hasSupertype(type.getElementName())
						|| type.equals(this.hierarchy.focusType))) {
				if (typeDelta == null) {
					typeDelta = new SimpleDelta();
				}
				typeDelta.modifiers();
			}
			if (typeDelta != null) {
				this.changes.put(type, typeDelta);
			}
		}
	}

	private void addTypeRemoval(IType type, SimpleDelta existingDelta) {
		if (existingDelta != null) {
			switch (existingDelta.getKind()) {
				case IJavaElementDelta.ADDED:
					// ADDED then REMOVED
					this.changes.remove(type);
					break;
				case IJavaElementDelta.CHANGED:
					// CHANGED then REMOVED
					existingDelta.removed();
					break;
					// REMOVED then REMOVED: should not happen
			}
		} else {
			// check whether the type removal affects the hierarchy
			if (this.hierarchy.contains(type)) {
				SimpleDelta typeDelta = new SimpleDelta();
				typeDelta.removed();
				this.changes.put(type, typeDelta);
			}
		}
	}

	/*
	 * Returns all types defined in the given element excluding the given element.
	 */
	private void getAllTypesFromElement(IJavaElement element, ArrayList allTypes) throws JavaModelException {
		switch (element.getElementType()) {
			case IJavaElement.COMPILATION_UNIT:
				IType[] types = ((ICompilationUnit)element).getTypes();
				for (int i = 0, length = types.length; i < length; i++) {
					IType type = types[i];
					allTypes.add(type);
					getAllTypesFromElement(type, allTypes);
				}
				break;
			case IJavaElement.TYPE:
				types = ((IType)element).getTypes();
				for (int i = 0, length = types.length; i < length; i++) {
					IType type = types[i];
					allTypes.add(type);
					getAllTypesFromElement(type, allTypes);
				}
				break;
			case IJavaElement.INITIALIZER:
			case IJavaElement.FIELD:
			case IJavaElement.METHOD:
				IJavaElement[] children = ((IMember)element).getChildren();
				for (int i = 0, length = children.length; i < length; i++) {
					IType type = (IType)children[i];
					allTypes.add(type);
					getAllTypesFromElement(type, allTypes);
				}
				break;
		}
	}

	/*
	 * Returns all types in the existing hierarchy that have the given element as a parent.
	 */
	private void getAllTypesFromHierarchy(JavaElement element, ArrayList allTypes) {
		switch (element.getElementType()) {
			case IJavaElement.COMPILATION_UNIT:
				IOpenable o = (IOpenable) element;
				ArrayList types = this.hierarchy.files.get(o);
				if (types != null) {
					allTypes.addAll(types);
				}
				break;
			case IJavaElement.TYPE:
			case IJavaElement.INITIALIZER:
			case IJavaElement.FIELD:
			case IJavaElement.METHOD:
				types = this.hierarchy.files.get(((IMember)element).getCompilationUnit());
				if (types != null) {
					for (int i = 0, length = types.size(); i < length; i++) {
						IType type = (IType)types.get(i);
						if (element.isAncestorOf(type)) {
							allTypes.add(type);
						}
					}
				}
				break;
		}
	}

	private boolean hasSuperTypeChange(IType type) throws JavaModelException {
		// check super class
		IType superclass = this.hierarchy.getSuperclass(type);
		String existingSuperclassName = superclass == null ? null : superclass.getElementName();
		String newSuperclassName = type.getSuperclassName();
		if (existingSuperclassName != null && !existingSuperclassName.equals(newSuperclassName)) {
			return true;
		}

		// check super interfaces
		IType[] existingSuperInterfaces = this.hierarchy.getSuperInterfaces(type);
		String[] newSuperInterfaces = type.getSuperInterfaceNames();
		if (existingSuperInterfaces.length != newSuperInterfaces.length) {
			return true;
		}
		for (int i = 0, length = newSuperInterfaces.length; i < length; i++) {
			String superInterfaceName = newSuperInterfaces[i];
			if (!superInterfaceName.equals(newSuperInterfaces[i])) {
				return true;
			}
		}

		return false;
	}

	private boolean hasVisibilityChange(IType type) throws JavaModelException {
		int existingFlags = this.hierarchy.getCachedFlags(type);
		int newFlags = type.getFlags();
		return existingFlags != newFlags;
	}

	/*
	 * Whether the hierarchy needs refresh according to the changes collected so far.
	 */
	public boolean needsRefresh() {
		return this.changes.size() != 0;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		Iterator iterator = this.changes.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry entry = (Map.Entry)iterator.next();
			buffer.append(((JavaElement)entry.getKey()).toDebugString());
			buffer.append(entry.getValue());
			if (iterator.hasNext()) {
				buffer.append('\n');
			}
		}
		return buffer.toString();
	}
}
