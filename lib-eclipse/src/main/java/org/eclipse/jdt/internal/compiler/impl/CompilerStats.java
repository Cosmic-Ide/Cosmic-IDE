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
package org.eclipse.jdt.internal.compiler.impl;

@SuppressWarnings("rawtypes")
public class CompilerStats implements Comparable {

	// overall
	public long startTime;
	public long endTime;
	public long overallTime;
	public long lineCount;

	// compile phases
	public long parseTime;
	public long resolveTime;
	public long analyzeTime;
	public long generateTime;

/**
 * Returns the total elapsed time (between start and end)
 * @return the time spent between start and end
 */
public long elapsedTime() {
	return this.overallTime;
}

@Override
public int compareTo(Object o) {
	CompilerStats otherStats = (CompilerStats) o;
	long time1 = elapsedTime();
	long time2 = otherStats.elapsedTime();
	return time1 < time2 ? -1 : (time1 == time2 ? 0 : 1);
}
}
