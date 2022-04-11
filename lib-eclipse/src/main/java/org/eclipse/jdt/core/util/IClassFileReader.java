/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.core.util;

/**
 * Description of a .class file. This class reifies the internal structure of a .class
 * file following the JVM specifications.
 * <p>
 * Note that several changes were introduced with J2SE 1.5.
 * Class file reader implementations should use support these
 * new class file attributes by returning objects implementing
 * the appropriate specialized attribute interfaces. Class
 * file reader clients can search for these new attributes
 * and downcast to the new interfaces as appropriate.
 * </p>
 *
 * This interface may be implemented by clients.
 *
 * @since 2.0
 */
public interface IClassFileReader {
	/**
	 * This value should be used to read completely each part of a .class file.
	 */
	int ALL 					= 0xFFFF;

	/**
	 * This value should be used to read only the constant pool entries of a .class file.
	 */
	int CONSTANT_POOL 			= 0x0001;

	/**
	 * This value should be used to read the constant pool entries and
	 * the method infos of a .class file.
	 */
	int METHOD_INFOS 			= 0x0002 + CONSTANT_POOL;

	/**
	 * This value should be used to read the constant pool entries and
	 * the field infos of a .class file.
	 */
	int FIELD_INFOS 			= 0x0004 + CONSTANT_POOL;

	/**
	 * This value should be used to read the constant pool entries and
	 * the super interface names of a .class file.
	 */
	int SUPER_INTERFACES 		= 0x0008 + CONSTANT_POOL;

	/**
	 * This value should be used to read the constant pool entries and
	 * the attributes of a .class file.
	 */
	int CLASSFILE_ATTRIBUTES 	= 0x0010 + CONSTANT_POOL;

	/**
	 * This value should be used to read the method bodies.
	 * It has to be used with METHOD_INFOS.
	 */
	int METHOD_BODIES 			= 0x0020;

	/**
	 * This value should be used to read the whole contents of the .class file except the
	 * method bodies.
	 */
	int ALL_BUT_METHOD_BODIES   = ALL & ~METHOD_BODIES;

	/**
	 * Answer back the access flags of the .class file.
	 *
	 * @return the access flags of the .class file
	 */
	int getAccessFlags();

	/**
	 * Answer back the array of field infos of the .class file,
	 * an empty array if none.
	 *
	 * @return the array of field infos of the .class file, an empty array if none
	 */
	IFieldInfo[] getFieldInfos();

	/**
	 * Answer back the names of interfaces implemented by this .class file,
	 * an empty array if none. The names are returned as described in the
	 * JVM specifications.
	 *
	 * @return the names of interfaces implemented by this .class file, an empty array if none
	 */
	char[][] getInterfaceNames();

	/**
	 * Answer back the indexes in the constant pool of interfaces implemented
	 * by this .class file, an empty array if none.
	 *
	 * @return the indexes in the constant pool of interfaces implemented
	 * by this .class file, an empty array if none
	 */
	int[] getInterfaceIndexes();

	/**
	 * Answer back the inner classes attribute of this .class file, null if none.
	 *
	 * @return the inner classes attribute of this .class file, null if none
	 */
	IInnerClassesAttribute getInnerClassesAttribute();

	/**
	 * Answer back the nest members attribute of this .class file, null if none.
	 *
	 * @return the nest members attribute of this .class file, null if none
	 * @since 3.16
	 */
	default INestMembersAttribute getNestMembersAttribute() {
		return null;
	}

	/**
	 * Answer back the record attribute of this .class file, null if none.
	 *
	 * @return the nest record of this .class file, null if none
	 * @since 3.22
	 */
	default IRecordAttribute getRecordAttribute() {
		return null;
	}

	/**
	 * Answer back the permitted subclasses attribute of this .class file, null if none.
	 *
	 * @return the permitted subclasses attribute of this .class file, null if none
	 * @since 3.24
	 */
	default IPermittedSubclassesAttribute getPermittedSubclassesAttribute() {
		return null;
	}

	/**
	 * Answer back the array of method infos of this .class file,
	 * an empty array if none.
	 *
	 * @return the array of method infos of this .class file,
	 * an empty array if none
	 */
	IMethodInfo[] getMethodInfos();

	/**
	 * Answer back the qualified name of the .class file.
	 * The name is returned as described in the JVM specifications.
	 *
	 * @return the qualified name of the .class file
	 */
	char[] getClassName();

	/**
	 * Answer back the index of the class name in the constant pool
	 * of the .class file.
	 *
	 * @return the index of the class name in the constant pool
	 */
	int getClassIndex();

	/**
	 * Answer back the qualified name of the superclass of this .class file.
	 * The name is returned as described in the JVM specifications. Answer null if
	 * getSuperclassIndex() is zero.
	 *
	 * @return the qualified name of the superclass of this .class file, null if getSuperclassIndex() is zero
	 */
	char[] getSuperclassName();

	/**
	 * Answer back the index of the superclass name in the constant pool
	 * of the .class file. Answer 0 if this .class file represents java.lang.Object.
	 *
	 * @return the index of the superclass name in the constant pool
	 * of the .class file, 0 if this .class file represents java.lang.Object.
	 */
	int getSuperclassIndex();

	/**
	 * Answer true if this .class file represents a class, false otherwise.
	 *
	 * @return true if this .class file represents a class, false otherwise
	 */
	boolean isClass();

	/**
	 * Answer true if this .class file represents an interface, false otherwise.
	 *
	 * @return true if this .class file represents an interface, false otherwise
	 */
	boolean isInterface();

	/**
	 * Answer the source file attribute, if it exists, null otherwise.
	 *
	 * @return the source file attribute, if it exists, null otherwise
	 */
	ISourceAttribute getSourceFileAttribute();

	/**
	 * Answer the constant pool of this .class file.
	 *
	 * @return the constant pool of this .class file
	 */
	IConstantPool getConstantPool();

	/**
	 * Answer the minor version of this .class file.
	 *
	 * @return the minor version of this .class file
	 */
	int getMinorVersion();

	/**
	 * Answer the major version of this .class file.
	 *
	 * @return the major version of this .class file
	 */
	int getMajorVersion();

	/**
	 * Answer back the attribute number of the .class file.
	 *
	 * @return the attribute number of the .class file
	 */
	int getAttributeCount();

	/**
	 * Answer back the collection of all attributes of the field info. It
	 * includes SyntheticAttribute, ConstantValueAttributes, etc. Answers an empty
	 * array if none.
	 *
	 * @return the collection of all attributes of the field info. It
	 * includes SyntheticAttribute, ConstantValueAttributes, etc. Answers an empty
	 * array if none
	 */
	IClassFileAttribute[] getAttributes();

	/**
	 * Answer back the magic number.
	 *
	 * @return the magic number
	 */
	int getMagic();

	/**
	 * Answer back the number of field infos.
	 *
	 * @return the number of field infos
	 */
	int getFieldsCount();

	/**
	 * Answer back the number of method infos.
	 *
	 * @return the number of method infos
	 */
	int getMethodsCount();
}
