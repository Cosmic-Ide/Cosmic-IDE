/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *        Andy Clement - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *******************************************************************************/
package org.eclipse.jdt.internal.core.util;

/**
 * Default implementation of IClassFileReader.
 */
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.util.ClassFormatException;
import org.eclipse.jdt.core.util.IAttributeNamesConstants;
import org.eclipse.jdt.core.util.IClassFileAttribute;
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.jdt.core.util.IConstantPool;
import org.eclipse.jdt.core.util.IConstantPoolConstant;
import org.eclipse.jdt.core.util.IFieldInfo;
import org.eclipse.jdt.core.util.IInnerClassesAttribute;
import org.eclipse.jdt.core.util.IMethodInfo;
import org.eclipse.jdt.core.util.IModifierConstants;
import org.eclipse.jdt.core.util.INestMembersAttribute;
import org.eclipse.jdt.core.util.IPermittedSubclassesAttribute;
import org.eclipse.jdt.core.util.IRecordAttribute;
import org.eclipse.jdt.core.util.ISourceAttribute;
import org.eclipse.jdt.internal.compiler.util.Util;

public class ClassFileReader extends ClassFileStruct implements IClassFileReader {
	private static final IFieldInfo[] NO_FIELD_INFOS = new IFieldInfo[0];
	private static final char[][] NO_INTERFACES_NAMES = CharOperation.NO_CHAR_CHAR;
	private static final IMethodInfo[] NO_METHOD_INFOS = new IMethodInfo[0];
	private int accessFlags;
	private IClassFileAttribute[] attributes;
	private int attributesCount;
	private char[] className;
	private int classNameIndex;

	private IConstantPool constantPool;
	private IFieldInfo[] fields;
	private int fieldsCount;
	private IInnerClassesAttribute innerClassesAttribute;
	private INestMembersAttribute nestMembersAttribute;
	private IPermittedSubclassesAttribute permittedSubclassesAttribute;
	private int[] interfaceIndexes;
	private char[][] interfaceNames;
	private int interfacesCount;
	private int magicNumber;
	private int majorVersion;
	private IMethodInfo[] methods;
	private int methodsCount;
	private int minorVersion;
	private ISourceAttribute sourceFileAttribute;
	private char[] superclassName;
	private int superclassNameIndex;
	private IRecordAttribute recordAttribute;

	/**
	 * Constructor for ClassFileReader.
	 *
	 * @param classFileBytes the raw bytes of the .class file
	 * @param decodingFlags the decoding flags
	 *
	 * @see IClassFileReader#ALL
	 * @see IClassFileReader#CLASSFILE_ATTRIBUTES
	 * @see IClassFileReader#CONSTANT_POOL
	 * @see IClassFileReader#FIELD_INFOS
	 */
	public ClassFileReader(byte[] classFileBytes, int decodingFlags) throws ClassFormatException {

		// This method looks ugly but is actually quite simple, the constantPool is constructed
		// in 3 passes.  All non-primitive constant pool members that usually refer to other members
		// by index are tweaked to have their value in inst vars, this minor cost at read-time makes
		// all subsequent uses of the constant pool element faster.
		int constantPoolCount;
		int[] constantPoolOffsets;
		try {
			this.magicNumber = (int) u4At(classFileBytes, 0, 0);
			if (this.magicNumber != 0xCAFEBABE) {
				throw new ClassFormatException(ClassFormatException.INVALID_MAGIC_NUMBER);
			}

			int readOffset = 10;
			this.minorVersion = u2At(classFileBytes, 4, 0);
			this.majorVersion = u2At(classFileBytes, 6, 0);

			if ((decodingFlags & IClassFileReader.CONSTANT_POOL) == 0) {
				// no need to go further
				return;
			}

			constantPoolCount = u2At(classFileBytes, 8, 0);
			// Pass #1 - Fill in all primitive constants
			constantPoolOffsets = new int[constantPoolCount];
			for (int i = 1; i < constantPoolCount; i++) {
				int tag = u1At(classFileBytes, readOffset, 0);
				switch (tag) {
					case IConstantPoolConstant.CONSTANT_Utf8 :
						constantPoolOffsets[i] = readOffset;
						readOffset += u2At(classFileBytes, readOffset + 1, 0);
						readOffset += IConstantPoolConstant.CONSTANT_Utf8_SIZE;
						break;
					case IConstantPoolConstant.CONSTANT_Integer :
						constantPoolOffsets[i] = readOffset;
						readOffset += IConstantPoolConstant.CONSTANT_Integer_SIZE;
						break;
					case IConstantPoolConstant.CONSTANT_Float :
						constantPoolOffsets[i] = readOffset;
						readOffset += IConstantPoolConstant.CONSTANT_Float_SIZE;
						break;
					case IConstantPoolConstant.CONSTANT_Long :
						constantPoolOffsets[i] = readOffset;
						readOffset += IConstantPoolConstant.CONSTANT_Long_SIZE;
						i++;
						break;
					case IConstantPoolConstant.CONSTANT_Double :
						constantPoolOffsets[i] = readOffset;
						readOffset += IConstantPoolConstant.CONSTANT_Double_SIZE;
						i++;
						break;
					case IConstantPoolConstant.CONSTANT_Class :
						constantPoolOffsets[i] = readOffset;
						readOffset += IConstantPoolConstant.CONSTANT_Class_SIZE;
						break;
					case IConstantPoolConstant.CONSTANT_String :
						constantPoolOffsets[i] = readOffset;
						readOffset += IConstantPoolConstant.CONSTANT_String_SIZE;
						break;
					case IConstantPoolConstant.CONSTANT_Fieldref :
						constantPoolOffsets[i] = readOffset;
						readOffset += IConstantPoolConstant.CONSTANT_Fieldref_SIZE;
						break;
					case IConstantPoolConstant.CONSTANT_Methodref :
						constantPoolOffsets[i] = readOffset;
						readOffset += IConstantPoolConstant.CONSTANT_Methodref_SIZE;
						break;
					case IConstantPoolConstant.CONSTANT_InterfaceMethodref :
						constantPoolOffsets[i] = readOffset;
						readOffset += IConstantPoolConstant.CONSTANT_InterfaceMethodref_SIZE;
						break;
					case IConstantPoolConstant.CONSTANT_NameAndType :
						constantPoolOffsets[i] = readOffset;
						readOffset += IConstantPoolConstant.CONSTANT_NameAndType_SIZE;
						break;
					case IConstantPoolConstant.CONSTANT_MethodHandle :
						constantPoolOffsets[i] = readOffset;
						readOffset += IConstantPoolConstant.CONSTANT_MethodHandle_SIZE;
						break;
					case IConstantPoolConstant.CONSTANT_MethodType :
						constantPoolOffsets[i] = readOffset;
						readOffset += IConstantPoolConstant.CONSTANT_MethodType_SIZE;
						break;
					case IConstantPoolConstant.CONSTANT_InvokeDynamic :
						constantPoolOffsets[i] = readOffset;
						readOffset += IConstantPoolConstant.CONSTANT_InvokeDynamic_SIZE;
						break;
					case IConstantPoolConstant.CONSTANT_Dynamic :
						constantPoolOffsets[i] = readOffset;
						readOffset += IConstantPoolConstant.CONSTANT_Dynamic_SIZE;
						break;
					case IConstantPoolConstant.CONSTANT_Module:
						constantPoolOffsets[i] = readOffset;
						readOffset += IConstantPoolConstant.CONSTANT_Module_SIZE;
						break;
					case IConstantPoolConstant.CONSTANT_Package:
						constantPoolOffsets[i] = readOffset;
						readOffset += IConstantPoolConstant.CONSTANT_Package_SIZE;
						break;
					default:
						throw new ClassFormatException(ClassFormatException.INVALID_TAG_CONSTANT);
				}
			}

			this.constantPool = new ConstantPool(classFileBytes, constantPoolOffsets);
			// Read and validate access flags
			this.accessFlags = u2At(classFileBytes, readOffset, 0);
			readOffset += 2;

			// Read the classname, use exception handlers to catch bad format
			this.classNameIndex = u2At(classFileBytes, readOffset, 0);
			this.className = getConstantClassNameAt(classFileBytes, constantPoolOffsets, this.classNameIndex);
			readOffset += 2;

			// Read the superclass name, can be zero for java.lang.Object
			this.superclassNameIndex = u2At(classFileBytes, readOffset, 0);
			readOffset += 2;
			// if superclassNameIndex is equals to 0 there is no need to set a value for the
			// field this.superclassName. null is fine.
			if (this.superclassNameIndex != 0) {
				this.superclassName = getConstantClassNameAt(classFileBytes, constantPoolOffsets, this.superclassNameIndex);
			}

			// Read the interfaces, use exception handlers to catch bad format
			this.interfacesCount = u2At(classFileBytes, readOffset, 0);
			readOffset += 2;
			this.interfaceNames = NO_INTERFACES_NAMES;
			this.interfaceIndexes = Util.EMPTY_INT_ARRAY;
			if (this.interfacesCount != 0) {
				if ((decodingFlags & IClassFileReader.SUPER_INTERFACES) != IClassFileReader.CONSTANT_POOL) {
					this.interfaceNames = new char[this.interfacesCount][];
					this.interfaceIndexes = new int[this.interfacesCount];
					for (int i = 0; i < this.interfacesCount; i++) {
						this.interfaceIndexes[i] = u2At(classFileBytes, readOffset, 0);
						this.interfaceNames[i] = getConstantClassNameAt(classFileBytes, constantPoolOffsets, this.interfaceIndexes[i]);
						readOffset += 2;
					}
				} else {
					readOffset += (2 * this.interfacesCount);
				}
			}
			// Read the this.fields, use exception handlers to catch bad format
			this.fieldsCount = u2At(classFileBytes, readOffset, 0);
			readOffset += 2;
			this.fields = NO_FIELD_INFOS;
			if (this.fieldsCount != 0) {
				if ((decodingFlags & IClassFileReader.FIELD_INFOS) != IClassFileReader.CONSTANT_POOL) {
					FieldInfo field;
					this.fields = new FieldInfo[this.fieldsCount];
					for (int i = 0; i < this.fieldsCount; i++) {
						field = new FieldInfo(classFileBytes, this.constantPool, readOffset);
						this.fields[i] = field;
						readOffset += field.sizeInBytes();
					}
				} else {
					for (int i = 0; i < this.fieldsCount; i++) {
						int attributeCountForField = u2At(classFileBytes, 6, readOffset);
						readOffset += 8;
						if (attributeCountForField != 0) {
							for (int j = 0; j < attributeCountForField; j++) {
								int attributeLength = (int) u4At(classFileBytes, 2, readOffset);
								readOffset += (6 + attributeLength);
							}
						}
					}
				}
			}
			// Read the this.methods
			this.methodsCount = u2At(classFileBytes, readOffset, 0);
			readOffset += 2;
			this.methods = NO_METHOD_INFOS;
			if (this.methodsCount != 0) {
				if ((decodingFlags & IClassFileReader.METHOD_INFOS) != IClassFileReader.CONSTANT_POOL) {
					this.methods = new MethodInfo[this.methodsCount];
					MethodInfo method;
					for (int i = 0; i < this.methodsCount; i++) {
						method = new MethodInfo(classFileBytes, this.constantPool, readOffset, decodingFlags);
						this.methods[i] = method;
						readOffset += method.sizeInBytes();
					}
				} else {
					for (int i = 0; i < this.methodsCount; i++) {
						int attributeCountForMethod = u2At(classFileBytes, 6, readOffset);
						readOffset += 8;
						if (attributeCountForMethod != 0) {
							for (int j = 0; j < attributeCountForMethod; j++) {
								int attributeLength = (int) u4At(classFileBytes, 2, readOffset);
								readOffset += (6 + attributeLength);
							}
						}
					}
				}
			}

			// Read the attributes
			this.attributesCount = u2At(classFileBytes, readOffset, 0);
			readOffset += 2;

			int attributesIndex = 0;
			this.attributes = ClassFileAttribute.NO_ATTRIBUTES;
			if (this.attributesCount != 0) {
				if ((decodingFlags & IClassFileReader.CLASSFILE_ATTRIBUTES) != IClassFileReader.CONSTANT_POOL) {
					this.attributes = new IClassFileAttribute[this.attributesCount];
					for (int i = 0; i < this.attributesCount; i++) {
						int utf8Offset = constantPoolOffsets[u2At(classFileBytes, readOffset, 0)];
						char[] attributeName = utf8At(classFileBytes, utf8Offset + 3, 0, u2At(classFileBytes, utf8Offset + 1, 0));
						if (equals(attributeName, IAttributeNamesConstants.INNER_CLASSES)) {
							this.innerClassesAttribute = new InnerClassesAttribute(classFileBytes, this.constantPool, readOffset);
							this.attributes[attributesIndex++] = this.innerClassesAttribute;
						} else if (equals(attributeName, IAttributeNamesConstants.SOURCE)) {
							this.sourceFileAttribute = new SourceFileAttribute(classFileBytes, this.constantPool, readOffset);
							this.attributes[attributesIndex++] = this.sourceFileAttribute;
						} else if (equals(attributeName, IAttributeNamesConstants.ENCLOSING_METHOD)) {
							this.attributes[attributesIndex++] = new EnclosingMethodAttribute(classFileBytes, this.constantPool, readOffset);
						} else if (equals(attributeName, IAttributeNamesConstants.SIGNATURE)) {
							this.attributes[attributesIndex++] = new SignatureAttribute(classFileBytes, this.constantPool, readOffset);
						} else if (equals(attributeName, IAttributeNamesConstants.RUNTIME_VISIBLE_ANNOTATIONS)) {
							this.attributes[attributesIndex++] = new RuntimeVisibleAnnotationsAttribute(classFileBytes, this.constantPool, readOffset);
						} else if (equals(attributeName, IAttributeNamesConstants.RUNTIME_INVISIBLE_ANNOTATIONS)) {
							this.attributes[attributesIndex++] = new RuntimeInvisibleAnnotationsAttribute(classFileBytes, this.constantPool, readOffset);
						} else if (equals(attributeName, IAttributeNamesConstants.BOOTSTRAP_METHODS)) {
							this.attributes[attributesIndex++] = new BootstrapMethodsAttribute(classFileBytes, this.constantPool, readOffset);
						} else if (equals(attributeName, IAttributeNamesConstants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS)) {
							this.attributes[attributesIndex++] = new RuntimeVisibleTypeAnnotationsAttribute(classFileBytes, this.constantPool, readOffset);
						} else if (equals(attributeName, IAttributeNamesConstants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS)) {
							this.attributes[attributesIndex++] = new RuntimeInvisibleTypeAnnotationsAttribute(classFileBytes, this.constantPool, readOffset);
						} else if (equals(attributeName, IAttributeNamesConstants.MODULE)) {
							this.attributes[attributesIndex++] = new ModuleAttribute(classFileBytes, this.constantPool, readOffset);
						} else if (equals(attributeName, IAttributeNamesConstants.MODULE_PACKAGES)) {
							this.attributes[attributesIndex++] = new ModulePackagesAttribute(classFileBytes, this.constantPool, readOffset);
						} else if (equals(attributeName, IAttributeNamesConstants.MODULE_MAIN_CLASS)) {
							this.attributes[attributesIndex++] = new ModuleMainClassAttribute(classFileBytes, this.constantPool, readOffset);
						} else if (equals(attributeName, IAttributeNamesConstants.NEST_HOST)) {
							this.attributes[attributesIndex++] = new NestHostAttribute(classFileBytes, this.constantPool, readOffset);
						} else if (equals(attributeName, IAttributeNamesConstants.NEST_MEMBERS)) {
							this.nestMembersAttribute = new NestMembersAttribute(classFileBytes, this.constantPool, readOffset);
							this.attributes[attributesIndex++] = this.nestMembersAttribute;
						} else if (equals(attributeName, IAttributeNamesConstants.RECORD)) {
							this.recordAttribute = new RecordAttribute(classFileBytes, this.constantPool, readOffset);
							this.attributes[attributesIndex++] = this.recordAttribute;
						} else if (equals(attributeName, IAttributeNamesConstants.PERMITTED_SUBCLASSES)) {
							this.permittedSubclassesAttribute = new PermittedSubclassesAttribute(classFileBytes, this.constantPool, readOffset);
							this.attributes[attributesIndex++] = this.permittedSubclassesAttribute;
						} else {
							this.attributes[attributesIndex++] = new ClassFileAttribute(classFileBytes, this.constantPool, readOffset);
						}
						long tmp = u4At(classFileBytes, readOffset + 2, 0);
						readOffset += (6 + tmp);
					}
				} else {
					for (int i = 0; i < this.attributesCount; i++) {
						readOffset += (6 + u4At(classFileBytes, readOffset + 2, 0));
					}
				}
			}
			if (readOffset != classFileBytes.length) {
				throw new ClassFormatException(ClassFormatException.TOO_MANY_BYTES);
			}
		} catch(ClassFormatException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new ClassFormatException(ClassFormatException.ERROR_TRUNCATED_INPUT);
		}
	}

	/**
	 * @see IClassFileReader#getAccessFlags()
	 */
	@Override
	public int getAccessFlags() {
		return this.accessFlags;
	}
	/**
	 * @see IClassFileReader#getAttributeCount()
	 */
	@Override
	public int getAttributeCount() {
		return this.attributesCount;
	}

	/**
	 * @see IClassFileReader#getAttributes()
	 */
	@Override
	public IClassFileAttribute[] getAttributes() {
		return this.attributes;
	}

	/**
	 * @see IClassFileReader#getClassIndex()
	 */
	@Override
	public int getClassIndex() {
		return this.classNameIndex;
	}

	/**
	 * @see IClassFileReader#getClassName()
	 */
	@Override
	public char[] getClassName() {
		return this.className;
	}

	private char[] getConstantClassNameAt(byte[] classFileBytes, int[] constantPoolOffsets, int constantPoolIndex) {
		int utf8Offset = constantPoolOffsets[u2At(classFileBytes, constantPoolOffsets[constantPoolIndex] + 1, 0)];
		return utf8At(classFileBytes, utf8Offset + 3, 0, u2At(classFileBytes, utf8Offset + 1, 0));
	}

	/**
	 * @see IClassFileReader#getConstantPool()
	 */
	@Override
	public IConstantPool getConstantPool() {
		return this.constantPool;
	}
	/**
	 * @see IClassFileReader#getFieldInfos()
	 */
	@Override
	public IFieldInfo[] getFieldInfos() {
		return this.fields;
	}
	/*
	 * @see IClassFileReader#getFieldInfos()
	 */

	/**
	 * @see IClassFileReader#getFieldsCount()
	 */
	@Override
	public int getFieldsCount() {
		return this.fieldsCount;
	}

	/**
	 * @see IClassFileReader#getInnerClassesAttribute()
	 */
	@Override
	public IInnerClassesAttribute getInnerClassesAttribute() {
		return this.innerClassesAttribute;
	}

	@Override
	public INestMembersAttribute getNestMembersAttribute() {
		return this.nestMembersAttribute;
	}

	@Override
	public IPermittedSubclassesAttribute getPermittedSubclassesAttribute() {
		return this.permittedSubclassesAttribute;
	}

	/**
	 * @see IClassFileReader#getInterfaceIndexes()
	 */
	@Override
	public int[] getInterfaceIndexes() {
		return this.interfaceIndexes;
	}

	/**
	 * @see IClassFileReader#getInterfaceNames()
	 */
	@Override
	public char[][] getInterfaceNames() {
		return this.interfaceNames;
	}

	/**
	 * @see IClassFileReader#getMagic()
	 */
	@Override
	public int getMagic() {
		return this.magicNumber;
	}

	/**
	 * @see IClassFileReader#getMajorVersion()
	 */
	@Override
	public int getMajorVersion() {
		return this.majorVersion;
	}

	/**
	 * @see IClassFileReader#getMethodInfos()
	 */
	@Override
	public IMethodInfo[] getMethodInfos() {
		return this.methods;
	}

	/**
	 * @see IClassFileReader#getMethodsCount()
	 */
	@Override
	public int getMethodsCount() {
		return this.methodsCount;
	}

	/**
	 * @see IClassFileReader#getMinorVersion()
	 */
	@Override
	public int getMinorVersion() {
		return this.minorVersion;
	}

	/**
	 * @see IClassFileReader#getSourceFileAttribute()
	 */
	@Override
	public ISourceAttribute getSourceFileAttribute() {
		return this.sourceFileAttribute;
	}

	/**
	 * @see IClassFileReader#getSuperclassIndex()
	 */
	@Override
	public int getSuperclassIndex() {
		return this.superclassNameIndex;
	}

	/**
	 * @see IClassFileReader#getSuperclassName()
	 */
	@Override
	public char[] getSuperclassName() {
		return this.superclassName;
	}
	/**
	 * @see IClassFileReader#isClass()
	 */
	@Override
	public boolean isClass() {
		return !(isInterface() || isModule());
	}

	/**
	 * @see IClassFileReader#isInterface()
	 */
	@Override
	public boolean isInterface() {
		return (getAccessFlags() & IModifierConstants.ACC_INTERFACE) != 0;
	}
	private boolean isModule() {
		return (getAccessFlags() & IModifierConstants.ACC_MODULE) != 0;
	}
	@Override
	public IRecordAttribute getRecordAttribute() {
		return this.recordAttribute;
	}
}
