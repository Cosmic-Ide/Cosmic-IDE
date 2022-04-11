/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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

public class ShortConstant extends Constant {

	private short value;

	public static Constant fromValue(short value) {
		return new ShortConstant(value);
	}

	private ShortConstant(short value) {
		this.value = value;
	}

	@Override
	public byte byteValue() {
		return (byte) this.value;
	}

	@Override
	public char charValue() {
		return (char) this.value;
	}

	@Override
	public double doubleValue() {
		return this.value; // implicit cast to return type
	}

	@Override
	public float floatValue() {
		return this.value; // implicit cast to return type
	}

	@Override
	public int intValue() {
		return this.value; // implicit cast to return type
	}

	@Override
	public long longValue() {
		return this.value; // implicit cast to return type
	}

	@Override
	public short shortValue() {
		return this.value;
	}

	@Override
	public String stringValue() {
		// spec 15.17.11
		return String.valueOf(this.value);
	}

	@Override
	public String toString() {

		return "(short)" + this.value; //$NON-NLS-1$
	}

	@Override
	public int typeID() {
		return T_short;
	}

	@Override
	public int hashCode() {
		return this.value;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ShortConstant other = (ShortConstant) obj;
		return this.value == other.value;
	}
}
