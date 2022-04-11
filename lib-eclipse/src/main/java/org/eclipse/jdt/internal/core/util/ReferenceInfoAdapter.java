/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.util;

/**
 * An adapter which implements the methods for handling
 * reference information from the parser.
 */
public abstract class ReferenceInfoAdapter {
/**
 * Does nothing.
 */
public void acceptAnnotationTypeReference(char[][] typeName, int sourceStart, int sourceEnd) {
	// Does nothing
}
/**
 * Does nothing.
 */
public void acceptAnnotationTypeReference(char[] typeName, int sourcePosition) {
	// Does nothing
}
/**
 * Does nothing.
 */
public void acceptConstructorReference(char[] typeName, int argCount, int sourcePosition) {
	// Does nothing
}
/**
 * Does nothing.
 */
public void acceptFieldReference(char[] fieldName, int sourcePosition) {
	// Does nothing
}
/**
 * Does nothing.
 */
public void acceptMethodReference(char[] methodName, int argCount, int sourcePosition) {
	// Does nothing
}
/**
 * Does nothing.
 */
public void acceptTypeReference(char[][] typeName, int sourceStart, int sourceEnd) {
	// Does nothing
}
/**
 * Does nothing.
 */
public void acceptTypeReference(char[] typeName, int sourcePosition) {
	// Does nothing
}
/**
 * Does nothing.
 */
public void acceptUnknownReference(char[][] name, int sourceStart, int sourceEnd) {
	// Does nothing
}
/**
 * Does nothing.
 */
public void acceptUnknownReference(char[] name, int sourcePosition) {
	// Does nothing
}
}
