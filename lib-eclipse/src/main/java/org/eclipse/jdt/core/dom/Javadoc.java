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
 * AST node for a Javadoc-style doc comment.
 * <pre>
 * Javadoc:
 *   <b>/** </b> { TagElement } <b>*</b><b>/</b>
 * </pre>
 *
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Javadoc extends Comment {

	/**
	 * The "comment" structural property of this node type (type: {@link String}) (JLS2 API only).
	 * @since 3.0
	 * @deprecated Replaced by {@link #TAGS_PROPERTY} in the JLS3 API.
	 */
	public static final SimplePropertyDescriptor COMMENT_PROPERTY =
		new SimplePropertyDescriptor(Javadoc.class, "comment", String.class, MANDATORY); //$NON-NLS-1$

	/**
	 * The "tags" structural property of this node type (element type: {@link TagElement}).
	 * @since 3.1
	 */
	public static final ChildListPropertyDescriptor TAGS_PROPERTY =
		new ChildListPropertyDescriptor(Javadoc.class, "tags", TagElement.class, CYCLE_RISK); //$NON-NLS-1$


	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 * @since 3.0
	 */
	private static final List PROPERTY_DESCRIPTORS_2_0;

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 * @since 3.1
	 */
	private static final List PROPERTY_DESCRIPTORS_3_0;

	static {
		List properyList = new ArrayList(3);
		createPropertyList(Javadoc.class, properyList);
		addProperty(COMMENT_PROPERTY, properyList);
		addProperty(TAGS_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS_2_0 = reapPropertyList(properyList);

		properyList = new ArrayList(2);
		createPropertyList(Javadoc.class, properyList);
		addProperty(TAGS_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS_3_0 = reapPropertyList(properyList);
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
		if (apiLevel == AST.JLS2_INTERNAL) {
			return PROPERTY_DESCRIPTORS_2_0;
		} else {
			return PROPERTY_DESCRIPTORS_3_0;
		}
	}

	/**
	 * Canonical minimal doc comment.
     * @since 3.0
	 */
	private static final String MINIMAL_DOC_COMMENT = "/** */";//$NON-NLS-1$

	/**
	 * The doc comment string, including opening and closing comment
	 * delimiters; defaults to a minimal Javadoc comment.
	 * @deprecated The comment string was replaced in the 3.0 release
	 * by a representation of the structure of the doc comment.
	 * For backwards compatibility, it is still funcational as before.
	 */
	private String comment = MINIMAL_DOC_COMMENT;

	/**
	 * The list of tag elements (element type: {@link TagElement}).
	 * Defaults to an empty list.
	 * @since 3.0
	 */
	private ASTNode.NodeList tags =
		new ASTNode.NodeList(TAGS_PROPERTY);

	/**
	 * Creates a new AST node for a doc comment owned by the given AST.
	 * The new node has an empty list of tag elements (and, for backwards
	 * compatability, an unspecified, but legal, doc comment string).
	 * <p>
	 * N.B. This constructor is package-private; all subclasses must be
	 * declared in the same package; clients are unable to declare
	 * additional subclasses.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	Javadoc(AST ast) {
		super(ast);
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final Object internalGetSetObjectProperty(SimplePropertyDescriptor property, boolean get, Object value) {
		if (property == COMMENT_PROPERTY) {
			if (get) {
				return getComment();
			} else {
				setComment((String) value);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetObjectProperty(property, get, value);
	}

	@Override
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == TAGS_PROPERTY) {
			return tags();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final int getNodeType0() {
		return JAVADOC;
	}

	@Override
	ASTNode clone0(AST target) {
		Javadoc result = new Javadoc(target);
		result.setSourceRange(getStartPosition(), getLength());
		if (this.ast.apiLevel == AST.JLS2_INTERNAL) {
			result.setComment(getComment());
		}
		result.tags().addAll(ASTNode.copySubtrees(target, tags()));
		return result;
	}

	@Override
	final boolean subtreeMatch0(ASTMatcher matcher, Object other) {
		// dispatch to correct overloaded match method
		return matcher.match(this, other);
	}

	@Override
	void accept0(ASTVisitor visitor) {
		boolean visitChildren = visitor.visit(this);
		if (visitChildren) {
			// visit children in normal left to right reading order
			acceptChildren(visitor, this.tags);
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the doc comment string, including the starting
	 * and ending comment delimiters, and any embedded line breaks.
	 *
	 * @return the doc comment string
	 * @exception UnsupportedOperationException if this operation is used in
	 * an AST later than JLS2
	 * @deprecated The comment string was replaced in the 3.0 release
	 * by a representation of the structure of the doc comment.
	 * See {@link #tags() tags}.
	 */
	public String getComment() {
	    supportedOnlyIn2();
		return this.comment;
	}

	/**
	 * Sets or clears the doc comment string. The documentation
	 * string must include the starting and ending comment delimiters,
	 * and any embedded line breaks.
	 *
	 * @param docComment the doc comment string
	 * @exception IllegalArgumentException if the Java comment string is invalid
	 * @exception UnsupportedOperationException if this operation is used in
	 * an AST later than JLS2
	 * @deprecated The comment string was replaced in the 3.0 release
	 * by a representation of the structure of the doc comment.
	 * See {@link #tags() tags}.
	 */
	public void setComment(String docComment) {
	    supportedOnlyIn2();
		if (docComment == null) {
			throw new IllegalArgumentException();
		}
		char[] source = docComment.toCharArray();
		Scanner scanner = this.ast.scanner;
		scanner.resetTo(0, source.length);
		scanner.setSource(source);
		try {
			int token;
			boolean onlyOneComment = false;
			while ((token = scanner.getNextToken()) != TerminalTokens.TokenNameEOF) {
				switch(token) {
					case TerminalTokens.TokenNameCOMMENT_JAVADOC :
						if (onlyOneComment) {
							throw new IllegalArgumentException();
						}
						onlyOneComment = true;
						break;
					default:
						onlyOneComment = false;
				}
			}
			if (!onlyOneComment) {
				throw new IllegalArgumentException();
			}
		} catch (InvalidInputException e) {
			throw new IllegalArgumentException(e);
		}
		preValueChange(COMMENT_PROPERTY);
		this.comment = docComment;
		postValueChange(COMMENT_PROPERTY);
	}

	/**
	 * Returns the live list of tag elements that make up this doc
	 * comment.
	 * <p>
	 * The tag elements cover everything except the starting and ending
	 * comment delimiters, and generally omit leading whitespace
	 * (including a leading "*") and embedded line breaks.
	 * The first tag element of a typical doc comment represents
	 * all the material before the first explicit doc tag; this
	 * first tag element has a <code>null</code> tag name and
	 * generally contains 1 or more {@link TextElement}s,
	 * and possibly interspersed with tag elements for nested tags
	 * like "{@link String String}".
	 * Subsequent tag elements represent successive top-level doc
	 * tag (e.g., "@param", "@return", "@see").
	 * </p>
	 * <p>
	 * Adding and removing nodes from this list affects this node
	 * dynamically.
	 * </p>
	 *
	 * @return the live list of tag elements in this doc comment
	 * (element type: {@link TagElement})
	 * @since 3.0
	 */
	public List tags() {
		return this.tags;
	}

	@Override
	int memSize() {
		int size = super.memSize() + 2 * 4;
		if (this.comment != MINIMAL_DOC_COMMENT) {
			// anything other than the default string takes space
			size += stringSize(this.comment);
		}
		return size;
	}

	@Override
	int treeSize() {
		return memSize() + this.tags.listSize();
	}
}
