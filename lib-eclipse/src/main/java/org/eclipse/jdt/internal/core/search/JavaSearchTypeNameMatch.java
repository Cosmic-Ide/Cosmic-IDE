/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.search;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.search.*;

/**
 * Java Search concrete class for a type name match.
 *
 * @since 3.3
 */
public class JavaSearchTypeNameMatch extends TypeNameMatch {

	private IType type;
	private int modifiers = -1; // store modifiers to avoid java model population

	private int accessibility = IAccessRule.K_ACCESSIBLE;

/**
 * Creates a new Java Search type name match.
 */
public JavaSearchTypeNameMatch(IType type, int modifiers) {
	this.type = type;
	this.modifiers = modifiers;
}

/* (non-Javadoc)
 * Returns whether the matched type is equals to the given object or not.
 * @see java.lang.Object#equals(java.lang.Object)
 */
@Override
public boolean equals(Object obj) {
	if (obj == this) return true; // avoid unnecessary calls for identical objects
	if (obj instanceof TypeNameMatch) {
		TypeNameMatch match = (TypeNameMatch) obj;
		if (this.type == null) {
			return match.getType() == null && match.getModifiers() == this.modifiers;
		}
		return this.type.equals(match.getType()) && match.getModifiers() == this.modifiers;
	}
	return false;
}

@Override
public int getAccessibility() {
	return this.accessibility;
}

@Override
public int getModifiers() {
	return this.modifiers;
}

/* (non-Javadoc)
 * Note that returned handle exists as it matches a type accepted
 * from up-to-date index file.
 * @see org.eclipse.jdt.core.search.TypeNameMatch#getType()
 */
@Override
public IType getType() {
	return this.type;
}

/* (non-Javadoc)
 * Returns the hash code of the matched type.
 * @see java.lang.Object#hashCode()
 */
@Override
public int hashCode() {
	if (this.type == null) return this.modifiers;
	return this.type.hashCode();
}

/**
 * Sets the accessibility of the accepted match.
 *
 * @param accessibility the accessibility of the current match
 */
public void setAccessibility(int accessibility) {
	this.accessibility = accessibility;
}

/**
 * Set modifiers of the matched type.
 *
 * @param modifiers the modifiers of the matched type.
 */
public void setModifiers(int modifiers) {
	this.modifiers = modifiers;
}

/**
 * Set matched type.
 *
 * @param type the matched type.
 */
public void setType(IType type) {
	this.type = type;
}

/* (non-Javadoc)
 * Returns the string of the matched type.
 * @see java.lang.Object#toString()
 */
@Override
public String toString() {
	if (this.type == null) return super.toString();
	return this.type.toString();
}
}
