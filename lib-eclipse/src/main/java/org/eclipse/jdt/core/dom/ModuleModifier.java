/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Module Modifier node - represents the modifiers for the requires directive in module declaration (added in JLS9 API).
 * <pre>
 * ModuleModifier:
 *    <b>static</b>
 *    <b>transitive</b>
 * </pre>
 * <p>
 * The numeric values of these flags match the ones for class
 * files as described in the Java Virtual Machine Specification.
 * Note that the value of <b>static</b> does <b>not</b> correspond to the value of {@link Modifier#STATIC}!
 * </p>
 *
 * @since 3.14
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class ModuleModifier extends ASTNode {

	/**
 	 * Module Modifier keywords (typesafe enumeration).
	 */
	public static class ModuleModifierKeyword {

		/** "static" modifier with flag value {@link ModuleModifier#STATIC_PHASE}. */
		public static final ModuleModifierKeyword STATIC_KEYWORD = new ModuleModifierKeyword("static", STATIC_PHASE);//$NON-NLS-1$

		/** "transitive" modifier with flag value {@link ModuleModifier#TRANSITIVE}. */
		public static final ModuleModifierKeyword TRANSITIVE_KEYWORD = new ModuleModifierKeyword("transitive", TRANSITIVE);//$NON-NLS-1$


		/**
		 * Map from token to operator (key type: <code>String</code>;
		 * value type: <code>Operator</code>).
		 */
		private static final Map KEYWORDS;

		static {
			KEYWORDS = new HashMap(2);
			ModuleModifierKeyword[] ops = {
					STATIC_KEYWORD,
					TRANSITIVE_KEYWORD,
				};
			for (int i = 0; i < ops.length; i++) {
				KEYWORDS.put(ops[i].toString(), ops[i]);
			}
		}

		/**
		 * Returns the module modifier corresponding to the given single-bit flag value,
		 * or <code>null</code> if none or if more than one bit is set.
		 * <p>
		 * <code>fromFlagValue</code> is the converse of <code>toFlagValue</code>:
		 * that is, <code>ModuleModifierKind.fromFlagValue(k.toFlagValue()) == k</code> for
		 * all module modifier keywords <code>k</code>.
		 * </p>
		 *
		 * @param flagValue the single-bit flag value for the module modifier
		 * @return the module modifier keyword, or <code>null</code> if none
		 * @see #toFlagValue()
		 */
		public static ModuleModifierKeyword fromFlagValue(int flagValue) {
			for (Iterator it = KEYWORDS.values().iterator(); it.hasNext(); ) {
				ModuleModifierKeyword k = (ModuleModifierKeyword) it.next();
				if (k.toFlagValue() == flagValue) {
					return k;
				}
			}
			return null;
		}

		/**
		 * Returns the module modifier corresponding to the given string,
		 * or <code>null</code> if none.
		 * <p>
		 * <code>toKeyword</code> is the converse of <code>toString</code>:
		 * that is, <code>ModuleModifierKind.toKeyword(k.toString()) == k</code> for
		 * all module modifier keywords <code>k</code>.
		 * </p>
		 *
		 * @param keyword the lowercase string name for the module modifier
		 * @return the module modifier keyword, or <code>null</code> if none
		 * @see #toString()
		 */
		public static ModuleModifierKeyword toKeyword(String keyword) {
			return (ModuleModifierKeyword) KEYWORDS.get(keyword);
		}

		/**
		 * The flag value for the module modifier.
		 */
		private int flagValue;

		/**
		 * The keyword module modifier string.
		 */
		private String keyword;

		/**
		 * Creates a new module modifier with the given keyword.
		 * <p>
		 * Note: this constructor is private. The only instances
		 * ever created are the ones for the standard modifiers.
		 * </p>
		 *
		 * @param keyword the character sequence for the module modifier
		 * @param flagValue flag value as described in the Java Virtual Machine Specification
		 */
		private ModuleModifierKeyword(String keyword, int flagValue) {
			this.keyword = keyword;
			this.flagValue = flagValue;
		}

		/**
		 * Returns the module modifier flag value corresponding to this module modifier keyword.
		 * These flag values are as described in the Java Virtual Machine Specification.
		 *
		 * @return one of the <code>ModuleModifier</code> constants
		 * @see #fromFlagValue(int)
		 */
		public int toFlagValue() {
			return this.flagValue;
		}

		/**
		 * Returns the keyword for the module modifier.
		 *
		 * @return the keyword for the module modifier
		 * @see #toKeyword(String)
		 */
		@Override
		public String toString() {
			return this.keyword;
		}
	}

	/**
	 * The "keyword" structural property of this node type (type: {@link ModuleModifier.ModuleModifierKeyword}).
	 */
	public static final SimplePropertyDescriptor KEYWORD_PROPERTY =
		new SimplePropertyDescriptor(ModuleModifier.class, "keyword", ModuleModifier.ModuleModifierKeyword.class, MANDATORY); //$NON-NLS-1$

	/**
	 * Module Modifier constant (bit mask, value 0) indicating no module modifiers.
	 */
	public static final int NONE = 0x0000;

	/**
	 * "static" module modifier constant (bit mask).
	 * Applicable to requires directive.
	 * <p>
	 * Note that the value of <b>static</b> does <b>not</b> correspond to the value of {@link Modifier#STATIC}!
	 * </p>
	 */
	public static final int STATIC_PHASE = 0x0040;

	/**
	 * "transitive" module modifier constant (bit mask).
	 * Applicable only to requires directive.
	 */
	public static final int TRANSITIVE = 0x0080;

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;

	static {
		List properyList = new ArrayList(2);
		createPropertyList(ModuleModifier.class, properyList);
		addProperty(KEYWORD_PROPERTY, properyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(properyList);
	}

	/**
	 * Returns whether the given flags includes the "transitive" module modifier.
	 *
	 * @param flags the module modifier flags
	 * @return <code>true</code> if the <code>TRANSITIVE</code> bit is
	 *   set, and <code>false</code> otherwise
	 */
	public static boolean isTransitive(int flags) {
		return (flags & TRANSITIVE) != 0;
	}

	/**
	 * Returns whether the given flags includes the "static" module modifier.
	 *
	 * @param flags the module modifier flags
	 * @return <code>true</code> if the <code>STATIC</code> bit is
	 *   set, and <code>false</code> otherwise
	 */
	public static boolean isStatic(int flags) {
		return (flags & STATIC_PHASE) != 0;
	}

	/**
	 * Returns a list of structural property descriptors for this node type.
	 * Clients must not modify the result.
	 *
	 * @param apiLevel the API level; one of the
	 * <code>AST.JLS*</code> constants

	 * @return a list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor})
	 */
	public static List propertyDescriptors(int apiLevel) {
		return PROPERTY_DESCRIPTORS;
	}

	/**
	 * The modifier keyword; defaults to an unspecified modifier.
	 */
	private ModuleModifierKeyword modifierKeyword = ModuleModifierKeyword.STATIC_KEYWORD;

	/**
	 * Creates a new unparented MODULE modifier node owned by the given AST.
	 * By default, the node has unspecified (but legal) modifier.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	ModuleModifier(AST ast) {
		super(ast);
	    unsupportedBelow9();
	}

	@Override
	void accept0(ASTVisitor visitor) {
		visitor.visit(this);
		visitor.endVisit(this);
	}

	@Override
	ASTNode clone0(AST target) {
		ModuleModifier result = new ModuleModifier(target);
		result.setSourceRange(getStartPosition(), getLength());
		result.setKeyword(getKeyword());
		return result;
	}

	/**
	 * Returns the modifier keyword of this modifier node.
	 *
	 * @return the modifier keyword
	 */
	public ModuleModifierKeyword getKeyword() {
		return this.modifierKeyword;
	}

	/**
	 * Sets the module modifier keyword of this module modifier node.
	 *
	 * @param modifierKeyord the module modifier keyword
	 * @exception IllegalArgumentException if the argument is <code>null</code>
	 */
	public void setKeyword(ModuleModifierKeyword modifierKeyord) {
		if (modifierKeyord == null) {
			throw new IllegalArgumentException();
		}
		preValueChange(KEYWORD_PROPERTY);
		this.modifierKeyword = modifierKeyord;
		postValueChange(KEYWORD_PROPERTY);
	}

	@Override
	final int getNodeType0() {
		return MODULE_MODIFIER;
	}

	@Override
	final Object internalGetSetObjectProperty(SimplePropertyDescriptor property, boolean get, Object value) {
		if (property == KEYWORD_PROPERTY) {
			if (get) {
				return getKeyword();
			} else {
				setKeyword((ModuleModifierKeyword) value);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetObjectProperty(property, get, value);
	}

	@Override
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	/**
	 * Answer true if the receiver is the static module modifier, false otherwise.
	 *
	 * @return true if the receiver is the static module modifier, false otherwise
	 */
	public boolean isStatic() {
		return this.modifierKeyword == ModuleModifierKeyword.STATIC_KEYWORD;
	}

	/**
	 * Answer true if the receiver is the transitive module modifier, false otherwise.
	 *
	 * @return true if the receiver is the transitive module modifier, false otherwise
	 */
	public boolean isTransitive() {
		return this.modifierKeyword == ModuleModifierKeyword.TRANSITIVE_KEYWORD;
	}

	@Override
	int memSize() {
		// treat ModifierKeyword as free
		return BASE_NODE_SIZE + 1 * 4;
	}

	@Override
	final boolean subtreeMatch0(ASTMatcher matcher, Object other) {
		// dispatch to correct overloaded match method
		return matcher.match(this, other);
	}

	@Override
	int treeSize() {
		return memSize();
	}
}