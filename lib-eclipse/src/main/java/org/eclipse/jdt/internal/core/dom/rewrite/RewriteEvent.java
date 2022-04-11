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


/**
 *
 */
public abstract class RewriteEvent {

	/**
	 * Change kind to describe that the event is an insert event.
	 * Does not apply for list events.
	 */
	public static final int INSERTED= 1;

	/**
	 * Change kind to describe that the event is an remove event.
	 * Does not apply for list events.
	 */
	public static final int REMOVED= 2;

	/**
	 * Change kind to describe that the event is an replace event.
	 * Does not apply for list events.
	 */
	public static final int REPLACED= 4;

	/**
	 * Change kind to signal that children changed. Does only apply for list events.
	 */
	public static final int CHILDREN_CHANGED= 8;

	/**
	 * Change kind to signal that the property did not change
	 */
	public static final int UNCHANGED= 0;

	/**
	 * @return Returns the event's change kind.
	 */
	public abstract int getChangeKind();

	/**
	 * @return Returns true if the given event is a list event.
	 */
	public abstract boolean isListRewrite();

	/**
	 * @return Returns the original value. For lists this is a List of ASTNodes, for non-list
	 * events this can be an ASTNode (for node properties), Integer (for an integer property),
	 * Boolean (for boolean node properties) or properties like Operator.
	 * <code>null</code> is returned if the event is an insert event.
	 */
	public abstract Object getOriginalValue();

	/**
	 * @return Returns the new value. For lists this is a List of ASTNodes, for non-list
	 * events this can be an ASTNode (for node properties), Integer (for an integer property),
	 * Boolean (for boolean node properties) or properties like Operator.
	 * <code>null</code> is returned if the event is a remove event.
	 */
	public abstract Object getNewValue();

	/**
	 * @return Return the events describing the changes in a list. returns <code>null</code> if the
	 * event is not a list event.
	 */
	public abstract RewriteEvent[] getChildren();

}
