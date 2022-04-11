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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Primitive type nodes.
 * <pre>
 * PrimitiveType:
 *    { Annotation } <b>byte</b>
 *    { Annotation } <b>short</b>
 *    { Annotation } <b>char</b>
 *    { Annotation } <b>int</b>
 *    { Annotation } <b>long</b>
 *    { Annotation } <b>float</b>
 *    { Annotation } <b>double</b>
 *    { Annotation } <b>boolean</b>
 *    { Annotation } <b>void</b>
 * </pre>
 * <p>
 * Note that due to the fact that AST nodes belong to a specific AST and
 * have a specific parent, there needs to multiple instances of these
 * nodes.
 * </p>
 *
 * @since 2.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PrimitiveType extends AnnotatableType {

	/**
 	 * Primitive type codes (typesafe enumeration).
	 * <pre>
	 *    <b>byte</b> 	BYTE
	 *    <b>short</b> 	SHORT
	 *    <b>char</b> 	CHAR
	 *    <b>int</b>  	INT
	 *    <b>long</b>  	LONG
	 *    <b>float</b>  	FLOAT
	 *    <b>double</b> 	DOUBLE
	 *    <b>boolean</b>	BOOLEAN
	 *    <b>void</b>  	VOID
	 * </pre>
	 */
	public static class Code {

		/**
		 * The name of the type.
		 */
		private String name;

		/**
		 * Creates a new primitive type code with the given name.
		 * <p>
		 * Note: this constructor is package-private. The only instances
		 * ever created are the ones for the standard primitive types.
		 * </p>
		 *
		 * @param name the standard name of the primitive type
		 */
		Code(String name) {
			this.name = name;
		}

		/**
		 * Returns the standard name of the primitive type.
		 *
		 * @return the standard name of the primitive type
		 */
		@Override
		public String toString() {
			return this.name;
		}
	}

	/** Type code for the primitive type "int". */
	public static final Code INT = new Code("int");//$NON-NLS-1$
	/** Type code for the primitive type "char". */
	public static final Code CHAR = new Code("char");//$NON-NLS-1$
	/** Type code for the primitive type "boolean". */
	public static final Code BOOLEAN = new Code("boolean");//$NON-NLS-1$
	/** Type code for the primitive type "short". */
	public static final Code SHORT = new Code("short");//$NON-NLS-1$
	/** Type code for the primitive type "long". */
	public static final Code LONG = new Code("long");//$NON-NLS-1$
	/** Type code for the primitive type "float". */
	public static final Code FLOAT = new Code("float");//$NON-NLS-1$
	/** Type code for the primitive type "double". */
	public static final Code DOUBLE = new Code("double");//$NON-NLS-1$
	/** Type code for the primitive type "byte". */
	public static final Code BYTE = new Code("byte");//$NON-NLS-1$

	/** Type code for the primitive type "void". Note that "void" is
	 * special in that its only legitimate uses are as a method return
	 * type and as a type literal.
	 */
	public static final Code VOID = new Code("void");//$NON-NLS-1$

	/**
	 * The primitive type code; one of the PrimitiveType constants; default
	 * is int.
	 */
	private PrimitiveType.Code typeCode = INT;

	/**
	 * Map from token to primitive type code (key type: <code>String</code>;
	 * value type: <code>PrimitiveType.Code</code>).
	 */
	private static final Map CODES;
	static {
		CODES = new HashMap(20);
		Code[] ops = {
				INT,
				BYTE,
				CHAR,
				BOOLEAN,
				SHORT,
				LONG,
				FLOAT,
				DOUBLE,
				VOID,
			};
		for (int i = 0; i < ops.length; i++) {
			CODES.put(ops[i].toString(), ops[i]);
		}
	}

	/**
	 * Returns the primitive type code corresponding to the given string,
	 * or <code>null</code> if none.
	 * <p>
	 * <code>toCode</code> is the converse of <code>toString</code>:
	 * that is,
	 * <code>PrimitiveType.Code.toCode(code.toString()) == code</code>
	 * for all type code <code>code</code>.
	 * </p>
	 *
	 * @param token the standard name of the primitive type
	 * @return the primitive type code, or <code>null</code> if none
	 */
	public static PrimitiveType.Code toCode(String token) {
		return (PrimitiveType.Code) CODES.get(token);
	}

	/**
	 * The "annotations" structural property of this node type (element type: {@link Annotation}).
	 * @since 3.10
	 */
	public static final ChildListPropertyDescriptor ANNOTATIONS_PROPERTY =
			internalAnnotationsPropertyFactory(PrimitiveType.class);

	/**
	 * The "primitiveTypeCode" structural property of this node type (type: {@link PrimitiveType.Code}).
	 * @since 3.0
	 */
	public static final SimplePropertyDescriptor PRIMITIVE_TYPE_CODE_PROPERTY =
		new SimplePropertyDescriptor(PrimitiveType.class, "primitiveTypeCode", PrimitiveType.Code.class, MANDATORY); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;
	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 * @since 3.10
	 */
	private static final List PROPERTY_DESCRIPTORS_8_0;
	static {
		List propertyList = new ArrayList(2);
		createPropertyList(PrimitiveType.class, propertyList);
		addProperty(PRIMITIVE_TYPE_CODE_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(propertyList);

		propertyList = new ArrayList(3);
		createPropertyList(PrimitiveType.class, propertyList);
		addProperty(ANNOTATIONS_PROPERTY, propertyList);
		addProperty(PRIMITIVE_TYPE_CODE_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS_8_0 = reapPropertyList(propertyList);
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
		switch (apiLevel) {
			case AST.JLS2_INTERNAL :
			case AST.JLS3_INTERNAL :
			case AST.JLS4_INTERNAL:
				return PROPERTY_DESCRIPTORS;
			default :
				return PROPERTY_DESCRIPTORS_8_0;
		}
	}

	/**
	 * Creates a new unparented node for a primitive type owned by the given
	 * AST. By default, the node has type "int".
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	PrimitiveType(AST ast) {
		super(ast);
	}

	@Override
	final ChildListPropertyDescriptor internalAnnotationsProperty() {
		return ANNOTATIONS_PROPERTY;
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	@Override
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == ANNOTATIONS_PROPERTY) {
			return annotations();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	@Override
	final Object internalGetSetObjectProperty(SimplePropertyDescriptor property, boolean get, Object value) {
		if (property == PRIMITIVE_TYPE_CODE_PROPERTY) {
			if (get) {
				return getPrimitiveTypeCode();
			} else {
				setPrimitiveTypeCode((Code) value);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetObjectProperty(property, get, value);
	}

	@Override
	final int getNodeType0() {
		return PRIMITIVE_TYPE;
	}

	@Override
	ASTNode clone0(AST target) {
		PrimitiveType result = new PrimitiveType(target);
		result.setSourceRange(getStartPosition(), getLength());
		if (this.ast.apiLevel >= AST.JLS8_INTERNAL) {
			result.annotations().addAll(
					ASTNode.copySubtrees(target, annotations()));
		}
		result.setPrimitiveTypeCode(getPrimitiveTypeCode());
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
			if (this.ast.apiLevel >= AST.JLS8_INTERNAL) {
				acceptChildren(visitor, this.annotations);
			}
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the primitive type code.
	 *
	 * @return one of the primitive type code constants declared in this
	 *    class
	 */
	public PrimitiveType.Code getPrimitiveTypeCode() {
		return this.typeCode;
	}

	/**
	 * Sets the primitive type code.
	 *
	 * @param typeCode one of the primitive type code constants declared in
	 *    this class
	 * @exception IllegalArgumentException if the argument is incorrect
	 */
	public void setPrimitiveTypeCode(PrimitiveType.Code typeCode) {
		if (typeCode == null) {
			throw new IllegalArgumentException();
		}
		preValueChange(PRIMITIVE_TYPE_CODE_PROPERTY);
		this.typeCode = typeCode;
		postValueChange(PRIMITIVE_TYPE_CODE_PROPERTY);
	}

	@Override
	int memSize() {
		// treat Code as free
		return BASE_NODE_SIZE + 2 * 4;
	}

	@Override
	int treeSize() {
		return memSize()
				+ (this.annotations == null ? 0 : this.annotations.listSize());
	}
}
