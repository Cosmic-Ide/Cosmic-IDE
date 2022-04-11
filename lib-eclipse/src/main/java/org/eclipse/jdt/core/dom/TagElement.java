/*******************************************************************************
 * Copyright (c) 2004, 2019 IBM Corporation and others.
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

/**
 * AST node for a tag within a doc comment.
 * Tag elements nested within another tag element are called
 * inline doc tags.
 * <pre>
 * TagElement:
 *     [ <b>@</b> Identifier ] { DocElement }
 * DocElement:
 *     TextElement
 *     Name
 *     MethodRef
 *     MemberRef
 *     <b>{</b> TagElement <b>}</b>
 * </pre>
 *
 * @see Javadoc
 * @since 3.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class TagElement extends ASTNode implements IDocElement {

	/**
	 * The "tagName" structural property of this node type (type: {@link String}).
	 *
	 * @since 3.0
	 */
	public static final SimplePropertyDescriptor TAG_NAME_PROPERTY =
		new SimplePropertyDescriptor(TagElement.class, "tagName", String.class, OPTIONAL); //$NON-NLS-1$

	/**
	 * The "fragments" structural property of this node type (element type: {@link IDocElement}).
	 * @since 3.0
	 */
	public static final ChildListPropertyDescriptor FRAGMENTS_PROPERTY =
		new ChildListPropertyDescriptor(TagElement.class, "fragments", IDocElement.class, CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 * @since 3.0
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List propertyList = new ArrayList(3);
		createPropertyList(TagElement.class, propertyList);
		addProperty(TAG_NAME_PROPERTY, propertyList);
		addProperty(FRAGMENTS_PROPERTY, propertyList);
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
	 * Standard doc tag name (value {@value}).
	 */
	public static final String TAG_AUTHOR = "@author"; //$NON-NLS-1$

	/**
	 * Standard inline doc tag name (value {@value}).
	 * <p>
	 * Note that this tag first appeared in J2SE 5.
	 * </p>
	 * @since 3.1
	 */
	public static final String TAG_CODE = "@code"; //$NON-NLS-1$

	/**
	 * Standard doc tag name (value {@value}).
	 */
	public static final String TAG_DEPRECATED = "@deprecated"; //$NON-NLS-1$

	/**
	 * Standard inline doc tag name (value {@value}).
	 */
	public static final String TAG_DOCROOT = "@docRoot"; //$NON-NLS-1$

	/**
	 * Standard doc tag name (value {@value}).
	 */
	public static final String TAG_EXCEPTION = "@exception"; //$NON-NLS-1$

	/**
	 * Standard doc tag name (value {@value}).
	 * @since 3.18
	 */
	public static final String TAG_HIDDEN = "@hidden"; //$NON-NLS-1$

	/**
	 * Standard doc tag name (value {@value}).
	 * @since 3.18
	 */
	public static final String TAG_INDEX = "@index"; //$NON-NLS-1$

	/**
	 * Standard inline doc tag name (value {@value}).
	 */
	public static final String TAG_INHERITDOC = "@inheritDoc"; //$NON-NLS-1$

	/**
	 * Standard inline doc tag name (value {@value}).
	 */
	public static final String TAG_LINK = "@link"; //$NON-NLS-1$

	/**
	 * Standard inline doc tag name (value {@value}).
	 */
	public static final String TAG_LINKPLAIN = "@linkplain"; //$NON-NLS-1$

	/**
	 * Standard inline doc tag name (value {@value}).
	 * <p>
	 * Note that this tag first appeared in J2SE 5.
	 * </p>
	 * @since 3.1
	 */
	public static final String TAG_LITERAL = "@literal"; //$NON-NLS-1$

	/**
	 * Standard doc tag name (value {@value}).
	 */
	public static final String TAG_PARAM = "@param"; //$NON-NLS-1$

	/**
	 * Standard doc tag name (value {@value}).
	 * @since 3.18
	 */
	public static final String TAG_PROVIDES = "@provides"; //$NON-NLS-1$

	/**
	 * Standard doc tag name (value {@value}).
	 */
	public static final String TAG_RETURN = "@return"; //$NON-NLS-1$

	/**
	 * Standard doc tag name (value {@value}).
	 */
	public static final String TAG_SEE = "@see"; //$NON-NLS-1$

	/**
	 * Standard doc tag name (value {@value}).
	 */
	public static final String TAG_SERIAL = "@serial"; //$NON-NLS-1$

	/**
	 * Standard doc tag name (value {@value}).
	 */
	public static final String TAG_SERIALDATA= "@serialData"; //$NON-NLS-1$

	/**
	 * Standard doc tag name (value {@value}).
	 */
	public static final String TAG_SERIALFIELD= "@serialField"; //$NON-NLS-1$

	/**
	 * Standard doc tag name (value {@value}).
	 */
	public static final String TAG_SINCE = "@since"; //$NON-NLS-1$

	/**
	 * Standard doc tag name (value {@value}).
	 * @since 3.18
	 */
	public static final String TAG_SUMMARY = "@summary"; //$NON-NLS-1$

	/**
	 * Standard doc tag name (value {@value}).
	 */
	public static final String TAG_THROWS = "@throws"; //$NON-NLS-1$

	/**
	 * Standard doc tag name (value {@value}).
	 * @since 3.18
	 */
	public static final String TAG_USES = "@uses"; //$NON-NLS-1$

	/**
	 * Standard inline doc tag name (value {@value}).
	 */
	public static final String TAG_VALUE= "@value"; //$NON-NLS-1$

	/**
	 * Standard doc tag name (value {@value}).
	 */
	public static final String TAG_VERSION = "@version"; //$NON-NLS-1$

	/**
	 * Javadoc tag name (value {@value}).
	 * @since 3.18
	 */
	public static final String TAG_API_NOTE = "@apiNote"; //$NON-NLS-1$

	/**
	 * Javadoc tag name (value {@value}).
	 * @since 3.18
	 */
	public static final String TAG_IMPL_SPEC = "@implSpec"; //$NON-NLS-1$

	/**
	 * Javadoc tag name (value {@value}).
	 * @since 3.18
	 */
	public static final String TAG_IMPL_NOTE = "@implNote"; //$NON-NLS-1$

	/**
	 * The tag name, or null if none; defaults to null.
	 */
	private String optionalTagName = null;

	/**
	 * The list of doc elements (element type: {@link IDocElement}).
	 * Defaults to an empty list.
	 */
	private ASTNode.NodeList fragments =
		new ASTNode.NodeList(FRAGMENTS_PROPERTY);

	/**
	 * Creates a new AST node for a tag element owned by the given AST.
	 * The new node has no name and an empty list of fragments.
	 * <p>
	 * N.B. This constructor is package-private; all subclasses must be
	 * declared in the same package; clients are unable to declare
	 * additional subclasses.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	TagElement(AST ast) {
		super(ast);
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final Object internalGetSetObjectProperty(SimplePropertyDescriptor property, boolean get, Object value) {
		if (property == TAG_NAME_PROPERTY) {
			if (get) {
				return getTagName();
			} else {
				setTagName((String) value);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetObjectProperty(property, get, value);
	}

	@Override
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == FRAGMENTS_PROPERTY) {
			return fragments();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final int getNodeType0() {
		return TAG_ELEMENT;
	}

	@Override
	ASTNode clone0(AST target) {
		TagElement result = new TagElement(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setTagName(getTagName());
		result.fragments().addAll(ASTNode.copySubtrees(target, fragments()));
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
			acceptChildren(visitor, this.fragments);
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns this node's tag name, or <code>null</code> if none.
	 * For top level doc tags such as parameter tags, the tag name
     * includes the "@" character ("@param").
	 * For inline doc tags such as link tags, the tag name
     * includes the "@" character ("@link").
     * The tag name may also be <code>null</code>; this is used to
     * represent the material at the start of a doc comment preceding
     * the first explicit tag.
     *
	 * @return the tag name, or <code>null</code> if none
	 */
	public String getTagName() {
		return this.optionalTagName;
	}

	/**
	 * Sets the tag name of this node to the given value.
	 * For top level doc tags such as parameter tags, the tag name
	 * includes the "@" character ("@param").
	 * For inline doc tags such as link tags, the tag name
	 * includes the "@" character ("@link").
	 * The tag name may also be <code>null</code>; this is used to
	 * represent the material at the start of a doc comment preceding
	 * the first explicit tag.
	 *
	 * @param tagName the tag name, or <code>null</code> if none
	 */
	public void setTagName(String tagName) {
		preValueChange(TAG_NAME_PROPERTY);
		this.optionalTagName = tagName;
		postValueChange(TAG_NAME_PROPERTY);
	}

	/**
	 * Returns the live list of fragments in this tag element.
	 * <p>
	 * The fragments cover everything following the tag name
	 * (or everything if there is no tag name), and generally omit
	 * embedded line breaks (and leading whitespace on new lines,
	 * including any leading "*"). {@link org.eclipse.jdt.core.dom.TagElement}
	 * nodes are used to represent tag elements (e.g., "@link")
	 * nested within this tag element.
	 * </p>
	 * <p>
	 * Here are some typical examples:
	 * <ul>
	 * <li>"@see Foo#bar()" - TagElement with tag name "@see";
	 * fragments() contains a single MethodRef node</li>
	 * <li>"@param args the program arguments" -
	 * TagElement with tag name "@param";
	 * 2 fragments: SimpleName ("args"), TextElement
	 * (" the program arguments")</li>
	 * <li>"@return See {&#64;link #foo foo} instead." -
	 * TagElement with tag name "@return";
	 * 3 fragments: TextElement ("See "),
	 * TagElement (for "&#64;link #foo foo"),
	 * TextElement (" instead.")</li>
	 * </ul>
	 * The use of Name, MethodRef, and MemberRef nodes within
	 * tag elements allows these fragments to be queried for
	 * binding information.
	 * <p>
	 * Adding and removing nodes from this list affects this node
	 * dynamically. The nodes in this list may be of various
	 * types, including {@link TextElement},
	 * {@link org.eclipse.jdt.core.dom.TagElement}, {@link Name},
	 * {@link MemberRef}, and {@link MethodRef}.
	 * Clients should assume that the list of types may grow in
	 * the future, and write their code to deal with unexpected
	 * nodes types. However, attempts to add a non-proscribed type
	 * of node will trigger an exception.
	 *
	 * @return the live list of doc elements in this tag element
	 * (element type: {@link IDocElement})
	 */
	public List fragments() {
		return this.fragments;
	}

	/**
	 * Returns whether this tag element is nested within another
	 * tag element. Nested tag elements appears enclosed in
	 * "{" and "}"; certain doc tags, including "@link" and
	 * "@linkplain" are only meaningful as nested tags.
	 * Top-level (i.e., non-nested) doc tags begin on a new line;
	 * certain doc tags, including "@param" and
	 * "@see" are only meaningful as top-level tags.
	 * <p>
	 * This convenience methods checks to see whether the parent
	 * of this node is of type {@link org.eclipse.jdt.core.dom.TagElement}.
	 * </p>
	 *
	 * @return <code>true</code> if this node is a nested tag element,
	 * and false if this node is either parented by a doc comment node
	 * ({@link Javadoc}), or is unparented
	 */
	public boolean isNested() {
		return (getParent() instanceof TagElement);
	}

	@Override
	int memSize() {
		int size = BASE_NODE_SIZE + 2 * 4 + stringSize(this.optionalTagName);
		return size;
	}

	@Override
	int treeSize() {
		return memSize() + this.fragments.listSize();
	}
}
