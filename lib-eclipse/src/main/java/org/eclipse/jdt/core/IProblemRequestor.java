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
package org.eclipse.jdt.core;

import org.eclipse.jdt.core.compiler.IProblem;

/**
 * A callback interface for receiving java problem as they are discovered
 * by some Java operation.
 *
 * @see IProblem
 * @since 2.0
 */
public interface IProblemRequestor {

	/**
	 * Notification of a Java problem.
	 *
	 * @param problem IProblem - The discovered Java problem.
	 */
	void acceptProblem(IProblem problem);

	/**
	 * Notification sent before starting the problem detection process.
	 * Typically, this would tell a problem collector to clear previously recorded problems.
	 */
	void beginReporting();

	/**
	 * Notification sent after having completed problem detection process.
	 * Typically, this would tell a problem collector that no more problems should be expected in this
	 * iteration.
	 */
	void endReporting();

	/**
	 * Predicate allowing the problem requestor to signal whether or not it is currently
	 * interested by problem reports. When answering <code>false</code>, problem will
	 * not be discovered any more until the next iteration.
	 *
	 * This  predicate will be invoked once prior to each problem detection iteration.
	 *
	 * @return boolean - indicates whether the requestor is currently interested by problems.
	 */
	boolean isActive();
}
