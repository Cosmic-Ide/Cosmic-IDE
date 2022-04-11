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
package org.eclipse.jdt.internal.core.search.indexing;

/**
 * Monitor ensuring no more than one writer working concurrently.
 * Multiple readers are allowed to perform simultaneously.
 */
public class ReadWriteMonitor {

/**
 * <0 : writing (cannot go beyond -1, i.e one concurrent writer)
 * =0 : idle
 * >0 : reading (number of concurrent readers)
 */
private int status = 0;
/**
 * Concurrent reading is allowed
 * Blocking only when already writing.
 */
public synchronized void enterRead() {
	while (this.status < 0) {
		try {
			wait();
		} catch(InterruptedException e) {
			// ignore
		}
	}
	this.status++;
}
/**
 * Only one writer at a time is allowed to perform
 * Blocking only when already writing or reading.
 */
public synchronized void enterWrite() {
	while (this.status != 0) {
		try {
			wait();
		} catch(InterruptedException e) {
			// ignore
		}
	}
	this.status--;
}
/**
 * Only notify waiting writer(s) if last reader
 */
public synchronized void exitRead() {

	if (--this.status == 0) notifyAll();
}
/**
 * When writing is over, all readers and possible
 * writers are granted permission to restart concurrently
 */
public synchronized void exitWrite() {

	if (++this.status == 0) notifyAll();
}
/**
 * Atomic exitRead/enterWrite: Allows to keep monitor in between
 * exit read and next enter write.
 * Use when writing changes is optional, otherwise call the individual methods.
 * Returns false if multiple readers are accessing the index.
 */
public synchronized boolean exitReadEnterWrite() {
	if (this.status != 1) return false; // only continue if this is the only reader

	this.status = -1;
	return true;
}
/**
 * Atomic exitWrite/enterRead: Allows to keep monitor in between
 * exit write and next enter read.
 * When writing is over, all readers are granted permissing to restart
 * concurrently.
 * This is the same as:
 * <pre>
 * synchronized(monitor) {
 *   monitor.exitWrite();
 *   monitor.enterRead();
 * }
 * </pre>
 */
public synchronized void exitWriteEnterRead() {
	exitWrite();
	enterRead();
}
@Override
public String toString() {
	StringBuilder buffer = new StringBuilder();
	if (this.status == 0) {
		buffer.append("Monitor idle "); //$NON-NLS-1$
	} else if (this.status < 0) {
		buffer.append("Monitor writing "); //$NON-NLS-1$
	} else if (this.status > 0) {
		buffer.append("Monitor reading "); //$NON-NLS-1$
	}
	buffer.append("(status = "); //$NON-NLS-1$
	buffer.append(this.status);
	buffer.append(")"); //$NON-NLS-1$
	return buffer.toString();
}
}
