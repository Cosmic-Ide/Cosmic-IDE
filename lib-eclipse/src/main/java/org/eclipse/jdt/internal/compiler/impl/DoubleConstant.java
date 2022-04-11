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

public class DoubleConstant extends Constant {

	private double value;

	public static Constant fromValue(double value) {
		return new DoubleConstant(value);
	}

	private DoubleConstant(double value) {
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
		return this.value;
	}

	@Override
	public float floatValue() {
		return (float) this.value;
	}

	@Override
	public int intValue() {
		return (int) this.value;
	}

	@Override
	public long longValue() {
		return (long) this.value;
	}

	@Override
	public short shortValue() {
		return (short) this.value;
	}

	@Override
	public String stringValue() {
		return String.valueOf(this.value);
	}

	@Override
	public String toString() {
		if (this == NotAConstant)
			return "(Constant) NotAConstant"; //$NON-NLS-1$
		return "(double)" + this.value;  //$NON-NLS-1$
	}

	@Override
	public int typeID() {
		return T_double;
	}

	@Override
	public int hashCode() {
		long temp = Double.doubleToLongBits(this.value);
		return (int) (temp ^ (temp >>> 32));
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
		DoubleConstant other = (DoubleConstant) obj;
		return Double.doubleToLongBits(this.value) == Double.doubleToLongBits(other.value);
	}
}
