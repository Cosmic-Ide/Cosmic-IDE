/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.core.dom;

/**
 * A variable binding represents either a field of a class or interface, or
 * a local variable declaration (including formal parameters, local variables,
 * and exception variables).
 *
 * @see ITypeBinding#getDeclaredFields()
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IVariableBinding extends IBinding {

	/**
	 * Returns whether this binding is for a field.
	 * Note that this method returns <code>true</code> for constants,
	 * including enum constants. This method returns <code>false</code>
	 * for local variables.
	 *
	 * @return <code>true</code> if this is the binding for a field,
	 *    and <code>false</code> otherwise
	 */
	public boolean isField();

	/**
	 * Returns whether this binding is for an enum constant.
	 * Note that this method returns <code>false</code> for local variables
	 * and for fields other than enum constants.
	 *
	 * @return <code>true</code> if this is the binding for an enum constant,
	 *    and <code>false</code> otherwise
	 * @since 3.1
	 */
	public boolean isEnumConstant();

	/**
	 * Returns whether this binding corresponds to a parameter.
	 *
	 * @return <code>true</code> if this is the binding for a parameter,
	 *    and <code>false</code> otherwise
	 * @since 3.2
	 */
	public boolean isParameter();

	/**
	 * Returns whether this binding is for a record component constant.
	 * Note that this method returns <code>false</code> for local variables
	 * and for fields other than record component.
	 *
	 * @return <code>true</code> if this is the binding for a record component,
	 *    and <code>false</code> otherwise
	 * @since 3.26
	 */
	public default boolean isRecordComponent() {
		return false;
	}

	/**
	 * Returns the name of the field or local variable declared in this binding.
	 * The name is always a simple identifier.
	 *
	 * @return the name of this field or local variable
	 */
	@Override
	public String getName();

	/**
	 * Returns the type binding representing the class or interface
	 * that declares this field.
	 * <p>
	 * The declaring class of a field is the class or interface of which it is
	 * a member. Local variables have no declaring class. The field length of an
	 * array type has no declaring class.
	 * </p>
	 *
	 * @return the binding of the class or interface that declares this field,
	 *   or <code>null</code> if none
	 */
	public ITypeBinding getDeclaringClass();

	/**
	 * Returns the binding for the type of this field or local variable.
	 *
	 * @return the binding for the type of this field or local variable
	 */
	public ITypeBinding getType();

	/**
	 * Returns a small integer variable id for this variable binding.
	 * <p>
	 * <b>Local variables inside methods:</b> Local variables (and parameters)
	 * declared within a single method are assigned ascending ids in normal
	 * code reading order; var1.getVariableId()&lt;var2.getVariableId() means that var1 is
	 * declared before var2.
	 * </p>
	 * <p>
	 * <b>Local variables outside methods:</b> Local variables declared in a
	 * type's static initializers (or initializer expressions of static fields)
	 * are assigned ascending ids in normal code reading order. Local variables
	 * declared in a type's instance initializers (or initializer expressions
	 * of non-static fields) are assigned ascending ids in normal code reading
	 * order. These ids are useful when checking definite assignment for
	 * static initializers (JLS 16.7) and instance initializers (JLS 16.8),
	 * respectively.
	 * </p>
	 * <p>
	 * <b>Fields:</b> Fields declared as members of a type are assigned
	 * ascending ids in normal code reading order;
	 * field1.getVariableId()&lt;field2.getVariableId() means that field1 is declared before
	 * field2.
	 * </p>
	 *
	 * @return a small non-negative variable id
	 */
	public int getVariableId();

	/**
	 * Returns this binding's constant value if it has one.
	 * Some variables may have a value computed at compile-time. If the type of
	 * the value is a primitive type, the result is the boxed equivalent (i.e.,
	 * int returned as an <code>Integer</code>). If the type of the value is
	 * <code>String</code>, the result is the string itself. If the variable has
	 * no compile-time computed value, the result is <code>null</code>.
	 * (Note: compile-time constant expressions cannot denote <code>null</code>;
	 * JLS2 15.28.). The result is always <code>null</code> for enum constants.
	 *
	 * @return the constant value, or <code>null</code> if none
	 * @since 3.0
	 */
	public Object getConstantValue();

	/**
	 * Returns the method binding representing the method containing the scope
	 * in which this local variable is declared.
	 * <p>
	 * The declaring method of a method formal parameter is the method itself.
	 * For a local variable declared somewhere within the body of a method,
	 * the declaring method is the enclosing method. When local or anonymous
	 * classes are involved, the declaring method is the innermost such method.
	 * There is no declaring method for a field, or for a local variable
	 * declared in a static or instance initializer; this method returns
	 * <code>null</code> in those cases.
	 * </p>
	 *
	 * @return the binding of the method or constructor that declares this
	 * local variable, or <code>null</code> if none
	 * @since 3.1
	 */
	public IMethodBinding getDeclaringMethod();

	/**
	 * Returns the binding for the variable declaration corresponding to this
	 * variable binding. For a binding for a field declaration in an instance
	 * of a generic type, this method returns the binding for the corresponding
	 * field declaration in the generic type. For other variable bindings,
	 * including all ones for local variables and parameters, this method
	 * returns the same binding.
	 *
	 * @return the variable binding for the originating declaration
	 * @since 3.1
	 */
	public IVariableBinding getVariableDeclaration();

	/**
	 * Returns whether this binding corresponds to an effectively final local
	 * variable (JLS8 4.12.4). A variable is said to be effectively final if
	 * it is not final and never assigned to after its initialization.
	 *
	 * @return <code>true</code> if this is an effectively final local variable
	 * 				and <code>false</code> otherwise
	 * @since 3.10
	 */
	public boolean isEffectivelyFinal();
}
