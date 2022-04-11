/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;

/**
 * TextBolck  AST node type.
 *
 * These are block of String literal nodes.
 *
 * @since 3.22
 */
@SuppressWarnings("rawtypes")
public class TextBlock extends Expression {

	/**
	 * The "escapedValue" structural property of this node type (type: {@link String}).
	 * @since 3.0
	 */
	public static final SimplePropertyDescriptor ESCAPED_VALUE_PROPERTY =
		new SimplePropertyDescriptor(TextBlock.class, "escapedValue", String.class, MANDATORY); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List propertyList = new ArrayList(2);
		createPropertyList(TextBlock.class, propertyList);
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
	 * @since 3.24
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
	 * The literal string without the quotes and the applicable preceding and
	 * trailing whitespace if any ; defaults to empty string.
	 */
	private String literalValue = "";//$NON-NLS-1$

	/**
	 * Creates a new unparented TextBlock node owned by the given AST.
	 * By default, the TextBlock denotes the empty string.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 * @exception UnsupportedOperationException if this operation is used beloe JLS15
	 */
	TextBlock(AST ast) {
		super(ast);
		unsupportedBelow15();
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
		return TEXT_BLOCK;
	}

	@Override
	ASTNode clone0(AST target) {
		TextBlock result = new TextBlock(target);
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
	 * @since 3.24
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
	 * @since 3.24
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
				case TerminalTokens.TokenNameTextBlock:
					break;
				default:
					throw new IllegalArgumentException("Invalid Text Block : >" + token + "<"); //$NON-NLS-1$//$NON-NLS-2$
			}
		} catch(InvalidInputException e) {
			throw new IllegalArgumentException("Invalid Text Block : >" + token + "<");//$NON-NLS-1$//$NON-NLS-2$
		}
		preValueChange(ESCAPED_VALUE_PROPERTY);
		this.escapedValue = token;
		postValueChange(ESCAPED_VALUE_PROPERTY);
	}

	/* (omit javadoc for this method)
	 * This method is does what setEscapedValue(String) does but without any validation.
	 * In addition, it also sets the literalValue property.
	 */
	void internalSetEscapedValue(String token, String literal) {
		preValueChange(ESCAPED_VALUE_PROPERTY);
		this.escapedValue = token;
		this.literalValue = literal;
		postValueChange(ESCAPED_VALUE_PROPERTY);
	}
	/**
	 * Returns the value of this literal node.
	 * <p>
	 * For example,
	 * <pre>
	 * TextBlock s;
	 * s.setEscapedValue("\"\"\"  \n hello\\n world\"");
	 * assert s.getLiteralValue().equals("hello\n world");
	 * </pre>
	 * <p>
	 * Note that this is a convenience method that converts from the stored
	 * TextBlock token returned by <code>getEscapedLiteral</code>.
	 * </p>
	 *
	 * @return the string value without enclosing triple quotes
	 * @exception IllegalArgumentException if the literal value cannot be converted
	 * @since 3.24
	 */
	public String getLiteralValue() {
		if (!this.literalValue.isEmpty()) {
			return this.literalValue;
		}
		char[] escaped = getEscapedValue().toCharArray();
		int len = escaped.length;
		if (len < 7) {
			throw new IllegalArgumentException();
		}

		int start = -1;
		loop: for (int i = 3; i < len; i++) {
			char c = escaped[i];
			if (ScannerHelper.isWhitespace(c)) {
				switch (c) {
					case 10 : /* \ u000a: LINE FEED               */
					case 13 : /* \ u000d: CARRIAGE RETURN         */
						start =  i + 1;
						break loop;
					default:
						break;
				}
			} else {
				break loop;
			}
		}
		if (start == -1) {
			throw new IllegalArgumentException();
		}
		return new String(
				CharOperation.subarray(escaped, start, len - 3)
				);
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

