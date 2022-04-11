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
package org.eclipse.jdt.core;

import java.util.EventObject;

/**
 * An element changed event describes a change to the structure or contents
 * of a tree of Java elements. The changes to the elements are described by
 * the associated delta object carried by this event.
 * <p>
 * This class is not intended to be instantiated or subclassed by clients.
 * Instances of this class are automatically created by the Java model.
 * </p>
 *
 * @see IElementChangedListener
 * @see IJavaElementDelta
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ElementChangedEvent extends EventObject {

	/**
	 * Event type constant (bit mask) indicating an after-the-fact
	 * report of creations, deletions, and modifications
	 * to one or more Java element(s) expressed as a hierarchical
	 * java element delta as returned by <code>getDelta()</code>.
	 *
	 * Note: this notification occurs during the corresponding POST_CHANGE
	 * resource change notification, and contains a full delta accounting for
	 * any JavaModel operation  and/or resource change.
	 *
	 * @see IJavaElementDelta
	 * @see org.eclipse.core.resources.IResourceChangeEvent
	 * @see #getDelta()
	 * @since 2.0
	 */
	public static final int POST_CHANGE = 1;

	/**
	 * Event type constant (bit mask) indicating an after-the-fact
	 * report of creations, deletions, and modifications
	 * to one or more Java element(s) expressed as a hierarchical
	 * java element delta as returned by <code>getDelta</code>.
	 *
	 * Note: this notification occurs during the corresponding PRE_AUTO_BUILD
	 * resource change notification. The delta, which is notified here, only contains
	 * information relative to the previous JavaModel operations (in other words,
	 * it ignores the possible resources which have changed outside Java operations).
	 * In particular, it is possible that the JavaModel be inconsistent with respect to
	 * resources, which got modified outside JavaModel operations (it will only be
	 * fully consistent once the POST_CHANGE notification has occurred).
	 *
	 * @see IJavaElementDelta
	 * @see org.eclipse.core.resources.IResourceChangeEvent
	 * @see #getDelta()
	 * @since 2.0
	 * @deprecated - no longer used, such deltas are now notified during POST_CHANGE
	 */
	public static final int PRE_AUTO_BUILD = 2;

	/**
	 * Event type constant (bit mask) indicating an after-the-fact
	 * report of creations, deletions, and modifications
	 * to one or more Java element(s) expressed as a hierarchical
	 * java element delta as returned by <code>getDelta</code>.
	 *
	 * Note: this notification occurs as a result of a working copy reconcile
	 * operation.
	 *
	 * @see IJavaElementDelta
	 * @see org.eclipse.core.resources.IResourceChangeEvent
	 * @see #getDelta()
	 * @since 2.0
	 */
	public static final int 	POST_RECONCILE = 4;

	private static final long serialVersionUID = -8947240431612844420L; // backward compatible

	/*
	 * Event type indicating the nature of this event.
	 * It can be a combination either:
	 *  - POST_CHANGE
	 *  - PRE_AUTO_BUILD
	 *  - POST_RECONCILE
	 */
	private int type;

	/**
	 * Creates an new element changed event (based on a <code>IJavaElementDelta</code>).
	 *
	 * @param delta the Java element delta.
	 * @param type the type of delta (ADDED, REMOVED, CHANGED) this event contains
	 */
	public ElementChangedEvent(IJavaElementDelta delta, int type) {
		super(delta);
		this.type = type;
	}
	/**
	 * Returns the delta describing the change.
	 *
	 * @return the delta describing the change
	 */
	public IJavaElementDelta getDelta() {
		return (IJavaElementDelta) this.source;
	}

	/**
	 * Returns the type of event being reported.
	 *
	 * @return one of the event type constants
	 * @see #POST_CHANGE
	 * @see #PRE_AUTO_BUILD
	 * @see #POST_RECONCILE
	 * @since 2.0
	 */
	public int getType() {
		return this.type;
	}
}
