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

package org.eclipse.jdt.core.dom;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;

/**
 * Number literal nodes.
 *
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("rawtypes")
public class NumberLiteral extends Expression {

	/**
	 * The "token" structural property of this node type (type: {@link String}).
	 * @since 3.0
	 */
	public static final SimplePropertyDescriptor TOKEN_PROPERTY =
		new SimplePropertyDescriptor(NumberLiteral.class, "token", String.class, MANDATORY); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List propertyList = new ArrayList(2);
		createPropertyList(NumberLiteral.class, propertyList);
		addProperty(TOKEN_PROPERTY, propertyList);
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
	 * The token string; defaults to the integer literal "0".
	 */
	private String tokenValue = "0";//$NON-NLS-1$

	/**
	 * Creates a new unparented number literal node owned by the given AST.
	 * By default, the number literal is the token "<code>0</code>".
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	NumberLiteral(AST ast) {
		super(ast);
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final Object internalGetSetObjectProperty(SimplePropertyDescriptor property, boolean get, Object value) {
		if (property == TOKEN_PROPERTY) {
			if (get) {
				return getToken();
			} else {
				setToken((String) value);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetObjectProperty(property, get, value);
	}

	@Override
	final int getNodeType0() {
		return NUMBER_LITERAL;
	}

	@Override
	ASTNode clone0(AST target) {
		NumberLiteral result = new NumberLiteral(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setToken(getToken());
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
	 * Returns the token of this number literal node. The value is the sequence
	 * of characters that would appear in the source program.
	 *
	 * @return the numeric literal token
	 */
	public String getToken() {
		return this.tokenValue;
	}

	/**
	 * Sets the token of this number literal node. The value is the sequence
	 * of characters that would appear in the source program.
	 *
	 * @param token the numeric literal token
	 * @exception IllegalArgumentException if the argument is incorrect
	 */
	public void setToken(String token) {
		// update internalSetToken(String) if this is changed
		if (token == null || token.length() == 0) {
			throw new IllegalArgumentException();
		}
		Scanner scanner = this.ast.scanner;
		char[] source = token.toCharArray();
		scanner.setSource(source);
		scanner.resetTo(0, source.length);
		scanner.tokenizeComments = false;
		scanner.tokenizeWhiteSpace = false;
		try {
			int tokenType = scanner.getNextToken();
			switch(tokenType) {
				case TerminalTokens.TokenNameDoubleLiteral:
				case TerminalTokens.TokenNameIntegerLiteral:
				case TerminalTokens.TokenNameFloatingPointLiteral:
				case TerminalTokens.TokenNameLongLiteral:
					break;
				case TerminalTokens.TokenNameMINUS :
					tokenType = scanner.getNextToken();
					switch(tokenType) {
						case TerminalTokens.TokenNameDoubleLiteral:
						case TerminalTokens.TokenNameIntegerLiteral:
						case TerminalTokens.TokenNameFloatingPointLiteral:
						case TerminalTokens.TokenNameLongLiteral:
							break;
						default:
							throw new IllegalArgumentException("Invalid number literal : >" + token + "<"); //$NON-NLS-1$//$NON-NLS-2$
					}
					break;
				default:
					throw new IllegalArgumentException("Invalid number literal : >" + token + "<");//$NON-NLS-1$//$NON-NLS-2$
			}
		} catch(InvalidInputException e) {
			throw new IllegalArgumentException();
		} finally {
			scanner.tokenizeComments = true;
			scanner.tokenizeWhiteSpace = true;
		}
		preValueChange(TOKEN_PROPERTY);
		this.tokenValue = token;
		postValueChange(TOKEN_PROPERTY);
	}

	/* (omit javadoc for this method)
	 * This method is a copy of setToken(String) that doesn't do any validation.
	 */
	void internalSetToken(String token) {
		preValueChange(TOKEN_PROPERTY);
		this.tokenValue = token;
		postValueChange(TOKEN_PROPERTY);
	}

	@Override
	int memSize() {
		int size = BASE_NODE_SIZE + 1 * 4 + stringSize(this.tokenValue);
		return size;
	}

	@Override
	int treeSize() {
		return memSize();
	}
}
