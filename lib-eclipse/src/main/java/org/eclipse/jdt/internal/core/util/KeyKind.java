/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *     Stephan Herrmann - Contribution for
 *								Bug 466308 - [hovering] Javadoc header for parameter is wrong with annotation-based null analysis
 *******************************************************************************/
package org.eclipse.jdt.internal.core.util;

public class KeyKind extends BindingKeyParser {

	public static final int F_TYPE = 0x0001;
	public static final int F_METHOD = 0x0002;
	public static final int F_FIELD = 0x0004;
	public static final int F_TYPE_PARAMETER = 0x0008;
	public static final int F_LOCAL_VAR = 0x0010;
	public static final int F_MEMBER = 0x0020;
	public static final int F_LOCAL = 0x0040;
	public static final int F_PARAMETERIZED_TYPE = 0x0080;
	public static final int F_RAW_TYPE = 0x0100;
	public static final int F_WILDCARD_TYPE = 0x0200;
	public static final int F_PARAMETERIZED_METHOD = 0x0400;
	public static final int F_CAPTURE = 0x0800;
	public static final int F_CONSTRUCTOR = 0x1000;

	public int flags = 0;
	private KeyKind innerKeyKind;

	public KeyKind(BindingKeyParser parser) {
		super(parser);
	}

	public KeyKind(String key) {
		super(key);
	}

	@Override
	public void consumeBaseType(char[] baseTypeSig) {
		this.flags |= F_TYPE;
	}

	@Override
	public void consumeCapture(int position) {
		this.flags |= F_CAPTURE;
	}

	@Override
	public void consumeField(char[] fieldName) {
		this.flags |= F_FIELD;
	}

	@Override
	public void consumeLocalType(char[] uniqueKey) {
		this.flags |= F_LOCAL;
	}

	@Override
	public void consumeLocalVar(char[] varName, int occurrenceCount, int argumentPosition) {
		this.flags |= F_LOCAL_VAR;
	}

	@Override
	public void consumeMemberType(char[] simpleTypeName) {
		this.flags |= F_MEMBER;
	}

	@Override
	public void consumeMethod(char[] selector, char[] signature) {
		this.flags |= F_METHOD;
		if (selector.length == 0)
			this.flags |= F_CONSTRUCTOR;
	}

	@Override
	public void consumeParameterizedGenericMethod() {
		this.flags |= F_PARAMETERIZED_METHOD;
	}

	@Override
	public void consumeParameterizedType(char[] simpleTypeName, boolean isRaw) {
		this.flags |= isRaw ? F_RAW_TYPE : F_PARAMETERIZED_TYPE;
	}

	@Override
	public void consumeParser(BindingKeyParser parser) {
		this.innerKeyKind = (KeyKind) parser;
	}

	@Override
	public void consumeRawType() {
		this.flags |= F_RAW_TYPE;
	}

	@Override
	public void consumeTopLevelType() {
		this.flags |= F_TYPE;
	}

	@Override
	public void consumeTypeParameter(char[] typeParameterName) {
		this.flags |= F_TYPE_PARAMETER;
	}

	@Override
	public void consumeTypeWithCapture() {
		this.flags = this.innerKeyKind.flags;
	}

	@Override
	public void consumeWildCard(int kind) {
		this.flags |= F_WILDCARD_TYPE;
	}

	@Override
	public BindingKeyParser newParser() {
		return new KeyKind(this);
	}
}
