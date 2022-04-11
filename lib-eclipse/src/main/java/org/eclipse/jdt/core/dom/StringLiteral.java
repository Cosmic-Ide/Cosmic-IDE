/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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

package org.eclipse.jdt.core.dom;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.jdt.internal.compiler.util.Util;

/**
 * String literal nodes.
 *
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public class StringLiteral extends Expression {

	/**
	 * The "escapedValue" structural property of this node type (type: {@link String}).
	 * @since 3.0
	 */
	public static final SimplePropertyDescriptor ESCAPED_VALUE_PROPERTY =
		new SimplePropertyDescriptor(StringLiteral.class, "escapedValue", String.class, MANDATORY); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List propertyList = new ArrayList(2);
		createPropertyList(StringLiteral.class, propertyList);
		addProperty(ESCAPED_VALUE_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(propertyList);
	}

	/**
	 * Returns a list of structural property descriptors for this node type.
	 * Clients must not modify the result.
	 *
	 * @param apiLevel the API level; one of the
	 * <code>AST.JLS*</code> constants

	 * @return a list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor})
	 * @since 3.0
	 */
	public static List propertyDescriptors(int apiLevel) {
		return PROPERTY_DESCRIPTORS;
	}

	/**
	 * The literal string, including quotes and escapes; defaults to the
	 * literal for the empty string.
	 */
	private String escapedValue = "\"\"";//$NON-NLS-1$

	/**
	 * Creates a new unparented string literal node owned by the given AST.
	 * By default, the string literal denotes the empty string.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	StringLiteral(AST ast) {
		super(ast);
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final Object internalGetSetObjectProperty(SimplePropertyDescriptor property, boolean get, Object value) {
		if (property == ESCAPED_VALUE_PROPERTY) {
			if (get) {
				return getEscapedValue();
			} else {
				setEscapedValue((String) value);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetObjectProperty(property, get, value);
	}

	@Override
	final int getNodeType0() {
		return STRING_LITERAL;
	}

	@Override
	ASTNode clone0(AST target) {
		StringLiteral result = new StringLiteral(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setEscapedValue(getEscapedValue());
		return result;
	}

	@Override
	final boolean subtreeMatch0(ASTMatcher matcher, Object other) {
		// dispatch to correct overloaded match method
		return matcher.match(this, other);
	}

	@Override
	void accept0(ASTVisitor visitor) {
		visitor.visit(this);
		visitor.endVisit(this);
	}

	/**
	 * Returns the string value of this literal node to the given string
	 * literal token. The token is the sequence of characters that would appear
	 * in the source program, including enclosing double quotes and embedded
	 * escapes.
	 *
	 * @return the string literal token, including enclosing double
	 *    quotes and embedded escapes
	 */
	public String getEscapedValue() {
		return this.escapedValue;
	}

	/**
	 * Sets the string value of this literal node to the given string literal
	 * token. The token is the sequence of characters that would appear in the
	 * source program, including enclosing double quotes and embedded escapes.
	 * For example,
	 * <ul>
	 * <li><code>""</code> <code>setLiteral("\"\"")</code></li>
	 * <li><code>"hello world"</code> <code>setLiteral("\"hello world\"")</code></li>
	 * <li><code>"boo\nhoo"</code> <code>setLiteral("\"boo\\nhoo\"")</code></li>
	 * </ul>
	 *
	 * @param token the string literal token, including enclosing double
	 *    quotes and embedded escapes
	 * @exception IllegalArgumentException if the argument is incorrect
	 */
	public void setEscapedValue(String token) {
		// update internalSetEscapedValue(String) if this is changed
		if (token == null) {
			throw new IllegalArgumentException("Token cannot be null"); //$NON-NLS-1$
		}
		Scanner scanner = this.ast.scanner;
		char[] source = token.toCharArray();
		scanner.setSource(source);
		scanner.resetTo(0, source.length);
		try {
			int tokenType = scanner.getNextToken();
			switch(tokenType) {
				case TerminalTokens.TokenNameStringLiteral:
					break;
				default:
					throw new IllegalArgumentException("Invalid string literal : >" + token + "<"); //$NON-NLS-1$//$NON-NLS-2$
			}
		} catch(InvalidInputException e) {
			throw new IllegalArgumentException("Invalid string literal : >" + token + "<");//$NON-NLS-1$//$NON-NLS-2$
		}
		preValueChange(ESCAPED_VALUE_PROPERTY);
		this.escapedValue = token;
		postValueChange(ESCAPED_VALUE_PROPERTY);
	}

	/* (omit javadoc for this method)
	 * This method is a copy of setEscapedValue(String) that doesn't do any validation.
	 */
	void internalSetEscapedValue(String token) {
		preValueChange(ESCAPED_VALUE_PROPERTY);
		this.escapedValue = token;
		postValueChange(ESCAPED_VALUE_PROPERTY);
	}

	/**
	 * Returns the value of this literal node.
	 * <p>
	 * For example,
	 * <pre>
	 * StringLiteral s;
	 * s.setEscapedValue("\"hello\\nworld\"");
	 * assert s.getLiteralValue().equals("hello\nworld");
	 * </pre>
	 * <p>
	 * Note that this is a convenience method that converts from the stored
	 * string literal token returned by <code>getEscapedLiteral</code>.
	 * </p>
	 *
	 * @return the string value without enclosing double quotes and embedded
	 *    escapes
	 * @exception IllegalArgumentException if the literal value cannot be converted
	 */
	public String getLiteralValue() {
		String s = getEscapedValue();
		int len = s.length();
		if (len < 2 || s.charAt(0) != '\"' || s.charAt(len-1) != '\"' ) {
			throw new IllegalArgumentException();
		}

		Scanner scanner = this.ast.scanner;
		char[] source = s.toCharArray();
		scanner.setSource(source);
		scanner.resetTo(0, source.length);
		try {
			int tokenType = scanner.getNextToken();
			switch(tokenType) {
				case TerminalTokens.TokenNameStringLiteral:
					return scanner.getCurrentStringLiteral();
				default:
					throw new IllegalArgumentException();
			}
		} catch(InvalidInputException e) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Sets the value of this literal node.
	 * <p>
	 * For example,
	 * <pre>
	 * StringLiteral s;
	 * s.setLiteralValue("hello\nworld");
	 * assert s.getEscapedValue().equals("\"hello\\nworld\"");
	 * assert s.getLiteralValue().equals("hello\nworld");
	 * </pre>
	 * <p>
	 * Note that this is a convenience method that converts to the stored
	 * string literal token acceptable to <code>setEscapedLiteral</code>.
	 * </p>
	 *
	 * @param value the string value without enclosing double quotes and
	 *    embedded escapes
	 * @exception IllegalArgumentException if the argument is incorrect
	 */
	public void setLiteralValue(String value) {
		if (value == null) {
			throw new IllegalArgumentException();
		}
		int len = value.length();
		StringBuffer b = new StringBuffer(len + 2);

		b.append("\""); // opening delimiter //$NON-NLS-1$
		for (int i = 0; i < len; i++) {
			char c = value.charAt(i);
			Util.appendEscapedChar(b, c, true);
		}
		b.append("\""); // closing delimiter //$NON-NLS-1$
		setEscapedValue(b.toString());
	}

	@Override
	int memSize() {
		int size = BASE_NODE_SIZE + 1 * 4 + stringSize(this.escapedValue);
		return size;
	}

	@Override
	int treeSize() {
		return memSize();
	}
}
