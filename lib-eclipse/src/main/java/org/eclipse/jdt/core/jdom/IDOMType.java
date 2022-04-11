/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.core.jdom;

/**
 * Represents a source type in a compilation unit, either as a top-level type or a member type.
 * The corresponding syntactic units are ClassDeclaration (JLS2 8.1) and InterfaceDeclaration (JLS2 9.1).
 * Enumeration types and annotation types, added in J2SE 1.5, are represented as
 * classes and interfaces, respectively.
 * <p>
 * Allowable child types for a type are <code>IDOMType</code>, <code>IDOMField</code>,
 * <code>IDOMMethod</code>, and <code>IDOMInitializer</code>.
 * Children are listed in the order in which they appear in the source. The parent of a type
 * is a type (in the case of a member type) or a compilation unit (in the case of a top-level type).
 * </p>
 *
 * @deprecated The JDOM was made obsolete by the addition in 2.0 of the more
 * powerful, fine-grained DOM/AST API found in the
 * org.eclipse.jdt.core.dom package.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IDOMType extends IDOMMember {
/**
 * Adds the given interface name to the names of interfaces that this type implements or extends
 * (the name will be added after the existing interface names). This is a convenience method.
 *
 * For classes, this represents the interfaces that this class implements.
 * For interfaces, this represents the interfaces that this interface extends.
 * The name may or may not be fully qualified.
 *
 * @param interfaceName the syntax for an interface name is defined by
 *  Interfaces in ClassDeclaration (JLS2 8.1). Type names must be specified as they would
 *  appear in source code. For example: "Cloneable", "java.io.Serializable".
 *
 * @exception IllegalArgumentException if <code>null</code> is specified
 */
public void addSuperInterface(String interfaceName) throws IllegalArgumentException;

/**
 * Returns the formal type parameters for this type.
 * Returns an empty array if this method has no formal type parameters.
 * <p>Formal type parameters are as they appear in the source
 * code; for example:
 * <code>"X extends List&lt;String&gt; & Serializable"</code>.
 * </p>
 *
 * @return the formal type parameters of this type,
 * in the order declared in the source, an empty array if none
 * @since 3.0
 */
String[] getTypeParameters();

/**
 * The <code>IDOMType</code> refinement of this <code>IDOMNode</code>
 * method returns the name of this type. The name of a class is defined by
 * ClassDeclaration (JLS2 8.1); the name of an interface is defined by
 * InterfaceDeclaration (JLS2 9.1).
 *
 * @return the name of this type
 */
@Override
public String getName();
/**
 * Returns the name of this type's superclass. The syntax for a superclass name
 * is specified by Super in ClassDeclaration (JLS2 8.1). Type names must be
 * specified as they would appear in source code. For example:
 * <code>"Object"</code>, or <code>"java.io.File"</code>.
 * As of J2SE 1.5, the superclass may also include parameterized
 * types like <code>"ArrayList&lt;String&gt;"</code>.
 *
 * @return the superclass name, or <code>null</code> if this type represents
 *   an interface or if no superclass has been assigned to this class
 */
public String getSuperclass();
/**
 * Returns the names of interfaces that this type implements or extends,
 * in the order in which they are listed in the source, or an empty array
 * if no superinterfaces are present. The syntax for interface names is
 * defined by Interfaces in ClassDeclaration (JLS2 8.1). Type names appear
 * as they would in source code. For example: <code>"Cloneable"</code>,
 * or <code>"java.io.Serializable"</code>.
 * As of J2SE 1.5, superinterfaces may also include parameterized
 * types like <code>"List&lt;String&gt;"</code>.
 * <p>
 * For classes, this method returns the interfaces that this class implements.
 * For interfaces, this method returns the interfaces that this interface extends.
 * </p>
 *
 * @return the list of interface names
 */
public String[] getSuperInterfaces();
/**
 * Returns whether this type is a class.
 *
 * @return <code>true</code> for classes, and <code>false</code> for interfaces
 */
public boolean isClass();

/**
 * Returns whether this type represents an enumeration class ("enum" instead of "class").
 *
 * @return true if this type represents an enumeration class, false otherwise
 * @since 3.0
 */
boolean isEnum();

/**
 * Returns whether this type represents an annotation type ("@interface" instead of "interface").
 *
 * @return true if this type represents an annotation type, false otherwise
 * @since 3.0
 */
boolean isAnnotation();

/**
 * Sets whether this type is a class or an interface. If this type is
 * a class, and is changed to an interface, this type's superclass
 * becomes <code>null</code>. When a class becomes an interface or an
 * interface becomes a class, superinterfaces remain (as part of an
 * <code>implements</code> clause for classes, or an <code>extends</code>
 * clause for interfaces).
 *
 * @param b <code>true</code> for classes, and <code>false</code> for interfaces
 */
public void setClass(boolean b);

/**
 * Sets whether this type represents an enumeration class.
 * If this type is a class and is changed to an enum,
 * this type's superclass becomes <code>null</code>.
 * If this type is an interface (including an annotation type),
 * and is changed to an enum, this type is also changed to a class.
 *
 * @param b <code>true</code> for enum classes, and <code>false</code> otherwise
 * @since 3.0
 */
public void setEnum(boolean b);

/**
 * Sets whether this type represents an annotation type ("@interface" instead of "interface").
 * If this type is a interface and is changed to an enum,
 * this type's superclass becomes <code>null</code> and its superinterface list
 * becomes empty. If this type is an class (including an enum),
 * and is changed to an annotation type, this type is also changed to an interface.
 *
 * @param b <code>true</code> for an annotation type, and <code>false</code> otherwise
 * @since 3.0
 */
public void setAnnotation(boolean b);

/**
 * Sets the formal type parameters for this type.
 * <p>Formal type parameters are given as they appear in the source
 * code; for example:
 * <code>"X extends List&lt;String&gt; & Serializable"</code>.
 * </p>
 *
 * @param typeParameters the formal type parameters of this type,
 * in the order to appear in the source, an empty array if none
 * @since 3.0
 */
void setTypeParameters(String[] typeParameters);

/**
 * The <code>IDOMType</code> refinement of this <code>IDOMNode</code>
 * method sets the name of this type. The name of a class is defined by
 * ClassDeclaration (JLS2 8.1); the name of an interface is defined by
 * InterfaceDeclaration (JLS2 9.1).
 *
 * @param name the given name
 * @exception IllegalArgumentException if <code>null</code> is specified
 */
@Override
public void setName(String name) throws IllegalArgumentException;
/**
 * Sets the name of this type's superclass. Has no effect if this type
 * represents an interface. A <code>null</code> name indicates that no
 * superclass name (extends clause) should appear in the source code.
 * The syntax for a superclass name is specified by Super in ClassDeclaration
 * (JLS2 8.1). Type names must be specified as they would appear in source code.
 * For example: <code>"Object"</code>, or <code>"java.io.File"</code>.
 * As of J2SE 1.5, the superclass may also include parameterized
 * types like <code>"ArrayList&lt;String&gt;"</code>.
 *
 * @param superclassName the superclass name, or <code>null</code> if this type
 *   should have to no explicitly specified superclass
 */
public void setSuperclass(String superclassName);
/**
 * Sets the names of interfaces that this type implements or extends,
 * in the order in which they are to be listed in the source. An empty array
 * parameter indicates that no superinterfaces are present. The syntax for
 * interface names is defined by Interfaces in ClassDeclaration (JLS2 8.1).
 * Type names appear as they would in source code. For example:
 * <code>"Cloneable"</code>, or <code>"java.io.Serializable"</code>.
 * As of J2SE 1.5, superinterfaces may also include parameterized
 * types like <code>"List&lt;String&gt;"</code>.
 * <p>
 * For classes, this method sets the interfaces that this class implements.
 * For interfaces, this method sets the interfaces that this interface extends.
 * </p>
 *
 * @param interfaceNames the list of interface names
 */
public void setSuperInterfaces(String[] interfaceNames);
}
