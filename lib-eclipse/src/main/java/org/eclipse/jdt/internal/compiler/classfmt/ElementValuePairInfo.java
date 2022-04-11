/*******************************************************************************
 * Copyright (c) 2005, 2016 BEA Systems, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    tyeung@bea.com - initial API and implementation
 *    olivier_thomann@ca.ibm.com - add hashCode() and equals(..) methods
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.classfmt;

import java.util.Arrays;

import org.eclipse.jdt.core.compiler.CharOperation;

public class ElementValuePairInfo implements org.eclipse.jdt.internal.compiler.env.IBinaryElementValuePair {

	static final ElementValuePairInfo[] NoMembers = new ElementValuePairInfo[0];

	private char[] name;
	private Object value;

public ElementValuePairInfo(char[] name, Object value) {
	this.name = name;
	this.value = value;
}
@Override
public char[] getName() {
	return this.name;
}
@Override
public Object getValue() {
	return this.value;
}
@Override
public String toString() {
	StringBuilder buffer = new StringBuilder();
	buffer.append(this.name);
	buffer.append('=');
	if (this.value instanceof Object[]) {
		final Object[] values = (Object[]) this.value;
		buffer.append('{');
		for (int i = 0, l = values.length; i < l; i++) {
			if (i > 0)
				buffer.append(", "); //$NON-NLS-1$
			buffer.append(values[i]);
		}
		buffer.append('}');
	} else {
		buffer.append(this.value);
	}
	return buffer.toString();
}
@Override
public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + CharOperation.hashCode(this.name);
	result = prime * result + ((this.value == null) ? 0 : this.value.hashCode());
	return result;
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
	ElementValuePairInfo other = (ElementValuePairInfo) obj;
	if (!Arrays.equals(this.name, other.name)) {
		return false;
	}
	if (this.value == null) {
		if (other.value != null) {
			return false;
		}
	} else if (!this.value.equals(other.value)) {
		return false;
	}
	return true;
}
}
