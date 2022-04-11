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
import org.eclipse.jdt.internal.compiler.parser.ScannerHelper;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.jdt.internal.compiler.util.Util;

/**
 * Character literal nodes.
 *
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public class CharacterLiteral extends Expression {

	/**
	 * The "escapedValue" structural property of this node type (type: {@link String}).
	 * @since 3.0
	 */
	public static final SimplePropertyDescriptor ESCAPED_VALUE_PROPERTY =
		new SimplePropertyDescriptor(CharacterLiteral.class, "escapedValue", String.class, MANDATORY); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List properyList = new ArrayList(2);
		createPropertyList(CharacterLiteral.class, properyList);
		addProperty(ESCAPED_VALUE_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(properyList);
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
	 * literal for the character 'X'.
	 */
	private String escapedValue = "\'X\'";//$NON-NLS-1$

	/**
	 * Creates a new unparented character literal node owned by the given AST.
	 * By default, the character literal denotes an unspecified character.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	CharacterLiteral(AST ast) {
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
		return CHARACTER_LITERAL;
	}

	@Override
	ASTNode clone0(AST target) {
		CharacterLiteral result = new CharacterLiteral(target);
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
	 * Returns the string value of this literal node. The value is the sequence
	 * of characters that would appear in the source program, including
	 * enclosing single quotes and embedded escapes.
	 *
	 * @return the escaped string value, including enclosing single quotes
	 *    and embedded escapes
	 */
	public String getEscapedValue() {
		return this.escapedValue;
	}

	/**
	 * Sets the string value of this literal node. The value is the sequence
	 * of characters that would appear in the source program, including
	 * enclosing single quotes and embedded escapes. For example,
	 * <ul>
	 * <li><code>'a'</code> <code>setEscapedValue("\'a\'")</code></li>
	 * <li><code>'\n'</code> <code>setEscapedValue("\'\\n\'")</code></li>
	 * </ul>
	 *
	 * @param value the string value, including enclosing single quotes
	 *    and embedded escapes
	 * @exception IllegalArgumentException if the argument is incorrect
	 */
	public void setEscapedValue(String value) {
		// check setInternalEscapedValue(String) if this method is changed
		if (value == null) {
			throw new IllegalArgumentException();
		}
		Scanner scanner = this.ast.scanner;
		char[] source = value.toCharArray();
		scanner.setSource(source);
		scanner.resetTo(0, source.length);
		try {
			int tokenType = scanner.getNextToken();
			switch(tokenType) {
				case TerminalTokens.TokenNameCharacterLiteral:
					break;
				default:
					throw new IllegalArgumentException();
			}
		} catch(InvalidInputException e) {
			throw new IllegalArgumentException();
		}
		preValueChange(ESCAPED_VALUE_PROPERTY);
		this.escapedValue = value;
		postValueChange(ESCAPED_VALUE_PROPERTY);
	}


	/* (omit javadoc for this method)
	 * This method is a copy of setEscapedValue(String) that doesn't do any validation.
	 */
	void internalSetEscapedValue(String value) {
		preValueChange(ESCAPED_VALUE_PROPERTY);
		this.escapedValue = value;
		postValueChange(ESCAPED_VALUE_PROPERTY);
	}

	/**
	 * Returns the value of this literal node.
	 * <p>
	 * For example,
	 * <pre>
	 * CharacterLiteral s;
	 * s.setEscapedValue("\'x\'");
	 * assert s.charValue() == 'x';
	 * </pre>
	 *
	 * @return the character value without enclosing quotes and embedded
	 *    escapes
	 * @exception IllegalArgumentException if the literal value cannot be converted
	 */
	public char charValue() {
		Scanner scanner = this.ast.scanner;
		char[] source = this.escapedValue.toCharArray();
		scanner.setSource(source);
		scanner.resetTo(0, source.length);
		int firstChar = scanner.getNextChar();
		int secondChar = scanner.getNextChar();

		if (firstChar == -1 || firstChar != '\'') {
			throw new IllegalArgumentException("illegal character literal");//$NON-NLS-1$
		}
		char value = (char) secondChar;
		int nextChar = scanner.getNextChar();
		if (secondChar == '\\') {
			if (nextChar == -1) {
				throw new IllegalArgumentException("illegal character literal");//$NON-NLS-1$
			}
			switch(nextChar) {
				case 'b' :
					value = '\b';
					break;
				case 't' :
					value = '\t';
					break;
				case 'n' :
					value = '\n';
					break;
				case 'f' :
					value = '\f';
					break;
				case 'r' :
					value = '\r';
					break;
				case '\"':
					value = '\"';
					break;
				case '\'':
					value = '\'';
					break;
				case '\\':
					value = '\\';
					break;
				default : //octal (well-formed: ended by a ' )
					try {
						if (ScannerHelper.isDigit((char) nextChar)) {
							int number = ScannerHelper.getNumericValue((char) nextChar);
							nextChar = scanner.getNextChar();
							if (nextChar == -1) {
								throw new IllegalArgumentException("illegal character literal");//$NON-NLS-1$
							}
							if (nextChar != '\'') {
								if (!ScannerHelper.isDigit((char) nextChar)) {
									throw new IllegalArgumentException("illegal character literal");//$NON-NLS-1$
								}
								number = (number * 8) + ScannerHelper.getNumericValue((char) nextChar);
								nextChar = scanner.getNextChar();
								if (nextChar == -1) {
									throw new IllegalArgumentException("illegal character literal");//$NON-NLS-1$
								}
								if (nextChar != '\'') {
									if (!ScannerHelper.isDigit((char) nextChar)) {
										throw new IllegalArgumentException("illegal character literal");//$NON-NLS-1$
									}
									number = (number * 8) + ScannerHelper.getNumericValue((char) nextChar);
								}
							}
							return (char) number;
						} else {
							throw new IllegalArgumentException("illegal character literal");//$NON-NLS-1$
						}
					} catch (InvalidInputException e) {
						throw new IllegalArgumentException("illegal character literal", e);//$NON-NLS-1$
					}
			}
			nextChar = scanner.getNextChar();
			if (nextChar == -1) {
				throw new IllegalArgumentException("illegal character literal");//$NON-NLS-1$
			}
		}
		if (nextChar == -1 || nextChar != '\'') {
			throw new IllegalArgumentException("illegal character literal");//$NON-NLS-1$
		}
		return value;
	}
	/**
	 * Sets the value of this character literal node to the given character.
	 * <p>
	 * For example,
	 * <pre>
	 * CharacterLiteral s;
	 * s.setCharValue('x');
	 * assert s.charValue() == 'x';
	 * assert s.getEscapedValue().equals("\'x\'");
	 * </pre>
	 *
	 * @param value the character value
	 */
	public void setCharValue(char value) {
		StringBuffer b = new StringBuffer(3);

		b.append('\''); // opening delimiter
		Util.appendEscapedChar(b, value, false);
		b.append('\''); // closing delimiter
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

