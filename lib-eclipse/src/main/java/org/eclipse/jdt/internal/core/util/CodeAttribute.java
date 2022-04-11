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
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *******************************************************************************/
package org.eclipse.jdt.internal.core.util;

import org.eclipse.jdt.core.util.ClassFormatException;
import org.eclipse.jdt.core.util.IAttributeNamesConstants;
import org.eclipse.jdt.core.util.IBytecodeVisitor;
import org.eclipse.jdt.core.util.IClassFileAttribute;
import org.eclipse.jdt.core.util.ICodeAttribute;
import org.eclipse.jdt.core.util.IConstantPool;
import org.eclipse.jdt.core.util.IConstantPoolConstant;
import org.eclipse.jdt.core.util.IConstantPoolEntry;
import org.eclipse.jdt.core.util.IExceptionTableEntry;
import org.eclipse.jdt.core.util.ILineNumberAttribute;
import org.eclipse.jdt.core.util.ILocalVariableAttribute;
import org.eclipse.jdt.core.util.IOpcodeMnemonics;

/**
 * Default implementation of ICodeAttribute.
 */
public class CodeAttribute extends ClassFileAttribute implements ICodeAttribute {
	private static final IExceptionTableEntry[] NO_EXCEPTION_TABLE = new IExceptionTableEntry[0];
	private IClassFileAttribute[] attributes;
	private int attributesCount;
	private byte[] bytecodes;
	private byte[] classFileBytes;
	private long codeLength;
	private int codeOffset;
	private IConstantPool constantPool;
	private IExceptionTableEntry[] exceptionTableEntries;
	private int exceptionTableLength;
	private ILineNumberAttribute lineNumberAttribute;
	private ILocalVariableAttribute localVariableAttribute;
	private int maxLocals;
	private int maxStack;

	CodeAttribute(byte[] classFileBytes, IConstantPool constantPool, int offset) throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		this.classFileBytes = classFileBytes;
		this.constantPool = constantPool;
		this.maxStack = u2At(classFileBytes, 6, offset);
		this.maxLocals = u2At(classFileBytes, 8, offset);
		this.codeLength = u4At(classFileBytes, 10, offset);
		this.codeOffset = offset + 14;
		int readOffset = (int) (14 + this.codeLength);
		this.exceptionTableLength = u2At(classFileBytes, readOffset, offset);
		readOffset += 2;
		this.exceptionTableEntries = NO_EXCEPTION_TABLE;
		if (this.exceptionTableLength != 0) {
			this.exceptionTableEntries = new ExceptionTableEntry[this.exceptionTableLength];
			for (int i = 0; i < this.exceptionTableLength; i++) {
				this.exceptionTableEntries [i] = new ExceptionTableEntry(classFileBytes, constantPool, offset + readOffset);
				readOffset += 8;
			}
		}
		this.attributesCount = u2At(classFileBytes, readOffset, offset);
		this.attributes = ClassFileAttribute.NO_ATTRIBUTES;
		if (this.attributesCount != 0) {
			this.attributes = new IClassFileAttribute[this.attributesCount];
		}
		int attributesIndex = 0;
		readOffset += 2;
		for (int i = 0; i < this.attributesCount; i++) {
			IConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(u2At(classFileBytes, readOffset, offset));
			if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Utf8) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			char[] attributeName = constantPoolEntry.getUtf8Value();
			if (equals(attributeName, IAttributeNamesConstants.LINE_NUMBER)) {
				this.lineNumberAttribute = new LineNumberAttribute(classFileBytes, constantPool, offset + readOffset);
				this.attributes[attributesIndex++] = this.lineNumberAttribute;
			} else if (equals(attributeName, IAttributeNamesConstants.LOCAL_VARIABLE)) {
				this.localVariableAttribute = new LocalVariableAttribute(classFileBytes, constantPool, offset + readOffset);
				this.attributes[attributesIndex++] = this.localVariableAttribute;
			} else if (equals(attributeName, IAttributeNamesConstants.LOCAL_VARIABLE_TYPE_TABLE)) {
				this.attributes[attributesIndex++] = new LocalVariableTypeAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (equals(attributeName, IAttributeNamesConstants.STACK_MAP_TABLE)) {
				this.attributes[attributesIndex++] = new StackMapTableAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (equals(attributeName, IAttributeNamesConstants.STACK_MAP)) {
				this.attributes[attributesIndex++] = new StackMapAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (equals(attributeName, IAttributeNamesConstants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS)) {
				this.attributes[attributesIndex++] = new RuntimeVisibleTypeAnnotationsAttribute(classFileBytes, constantPool, offset + readOffset);
			} else if (equals(attributeName, IAttributeNamesConstants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS)) {
				this.attributes[attributesIndex++] = new RuntimeInvisibleTypeAnnotationsAttribute(classFileBytes, constantPool, offset + readOffset);
			} else {
				this.attributes[attributesIndex++] = new ClassFileAttribute(classFileBytes, constantPool, offset + readOffset);
			}
			readOffset += (6 + u4At(classFileBytes, readOffset + 2, offset));
		}
	}
	/**
	 * @see ICodeAttribute#getAttributes()
	 */
	@Override
	public IClassFileAttribute[] getAttributes() {
		return this.attributes;
	}

	/**
	 * @see ICodeAttribute#getAttributesCount()
	 */
	@Override
	public int getAttributesCount() {
		return this.attributesCount;
	}

	/**
	 * @see ICodeAttribute#getBytecodes()
	 */
	@Override
	public byte[] getBytecodes() {
		if (this.bytecodes == null) {
			System.arraycopy(this.classFileBytes, this.codeOffset, (this.bytecodes = new byte[(int) this.codeLength]), 0, (int) this.codeLength);
		}
		return this.bytecodes;
	}

	/**
	 * @see ICodeAttribute#getCodeLength()
	 */
	@Override
	public long getCodeLength() {
		return this.codeLength;
	}

	/**
	 * @see ICodeAttribute#getExceptionTable()
	 */
	@Override
	public IExceptionTableEntry[] getExceptionTable() {
		return this.exceptionTableEntries;
	}

	/**
	 * @see ICodeAttribute#getExceptionTableLength()
	 */
	@Override
	public int getExceptionTableLength() {
		return this.exceptionTableLength;
	}

	/**
	 * @see ICodeAttribute#getLineNumberAttribute()
	 */
	@Override
	public ILineNumberAttribute getLineNumberAttribute() {
		return this.lineNumberAttribute;
	}

	/**
	 * @see ICodeAttribute#getLocalVariableAttribute()
	 */
	@Override
	public ILocalVariableAttribute getLocalVariableAttribute() {
		return this.localVariableAttribute;
	}

	/**
	 * @see ICodeAttribute#getMaxLocals()
	 */
	@Override
	public int getMaxLocals() {
		return this.maxLocals;
	}

	/**
	 * @see ICodeAttribute#getMaxStack()
	 */
	@Override
	public int getMaxStack() {
		return this.maxStack;
	}

	/**
	 * @see ICodeAttribute#traverse(IBytecodeVisitor visitor)
	 */
	@Override
	public void traverse(IBytecodeVisitor visitor) throws ClassFormatException {
		int pc = this.codeOffset;
		int opcode, index, _const, branchOffset;
		IConstantPoolEntry constantPoolEntry;
		while (true) {
			opcode = u1At(this.classFileBytes, 0, pc);
			switch(opcode) {
				case IOpcodeMnemonics.NOP :
					visitor._nop(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ACONST_NULL :
					visitor._aconst_null(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ICONST_M1 :
					visitor._iconst_m1(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ICONST_0 :
					visitor._iconst_0(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ICONST_1 :
					visitor._iconst_1(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ICONST_2 :
					visitor._iconst_2(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ICONST_3 :
					visitor._iconst_3(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ICONST_4 :
					visitor._iconst_4(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ICONST_5 :
					visitor._iconst_5(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LCONST_0 :
					visitor._lconst_0(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LCONST_1 :
					visitor._lconst_1(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FCONST_0 :
					visitor._fconst_0(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FCONST_1 :
					visitor._fconst_1(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FCONST_2 :
					visitor._fconst_2(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DCONST_0 :
					visitor._dconst_0(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DCONST_1 :
					visitor._dconst_1(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.BIPUSH :
					visitor._bipush(pc - this.codeOffset, (byte) i1At(this.classFileBytes, 1, pc));
					pc+=2;
					break;
				case IOpcodeMnemonics.SIPUSH :
					visitor._sipush(pc - this.codeOffset, (short) i2At(this.classFileBytes, 1, pc));
					pc+=3;
					break;
				case IOpcodeMnemonics.LDC :
					index = u1At(this.classFileBytes, 1, pc);
					constantPoolEntry = this.constantPool.decodeEntry(index);
					checkConstantAndThrow(constantPoolEntry.getKind());
					visitor._ldc(pc - this.codeOffset, index, constantPoolEntry);
					pc+=2;
					break;
				case IOpcodeMnemonics.LDC_W :
					index = u2At(this.classFileBytes, 1, pc);
					constantPoolEntry = this.constantPool.decodeEntry(index);
					checkConstantAndThrow(constantPoolEntry.getKind());
					visitor._ldc_w(pc - this.codeOffset, index, constantPoolEntry);
					pc+=3;
					break;
				case IOpcodeMnemonics.LDC2_W :
					index = u2At(this.classFileBytes, 1, pc);
					constantPoolEntry = this.constantPool.decodeEntry(index);
					if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Double
						&& constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Long) {
							throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					visitor._ldc2_w(pc - this.codeOffset, index, constantPoolEntry);
					pc+=3;
					break;
				case IOpcodeMnemonics.ILOAD :
					index = u1At(this.classFileBytes, 1, pc);
					visitor._iload(pc - this.codeOffset, index);
					pc+=2;
					break;
				case IOpcodeMnemonics.LLOAD :
					index = u1At(this.classFileBytes, 1, pc);
					visitor._lload(pc - this.codeOffset, index);
					pc+=2;
					break;
				case IOpcodeMnemonics.FLOAD :
					index = u1At(this.classFileBytes, 1, pc);
					visitor._fload(pc - this.codeOffset, index);
					pc+=2;
					break;
				case IOpcodeMnemonics.DLOAD :
					index = u1At(this.classFileBytes, 1, pc);
					visitor._dload(pc - this.codeOffset, index);
					pc+=2;
					break;
				case IOpcodeMnemonics.ALOAD :
					index = u1At(this.classFileBytes, 1, pc);
					visitor._aload(pc - this.codeOffset, index);
					pc+=2;
					break;
				case IOpcodeMnemonics.ILOAD_0 :
					visitor._iload_0(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ILOAD_1 :
					visitor._iload_1(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ILOAD_2 :
					visitor._iload_2(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ILOAD_3 :
					visitor._iload_3(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LLOAD_0 :
					visitor._lload_0(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LLOAD_1 :
					visitor._lload_1(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LLOAD_2 :
					visitor._lload_2(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LLOAD_3 :
					visitor._lload_3(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FLOAD_0 :
					visitor._fload_0(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FLOAD_1 :
					visitor._fload_1(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FLOAD_2 :
					visitor._fload_2(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FLOAD_3 :
					visitor._fload_3(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DLOAD_0 :
					visitor._dload_0(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DLOAD_1 :
					visitor._dload_1(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DLOAD_2 :
					visitor._dload_2(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DLOAD_3 :
					visitor._dload_3(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ALOAD_0 :
					visitor._aload_0(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ALOAD_1 :
					visitor._aload_1(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ALOAD_2 :
					visitor._aload_2(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ALOAD_3 :
					visitor._aload_3(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.IALOAD :
					visitor._iaload(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LALOAD :
					visitor._laload(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FALOAD :
					visitor._faload(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DALOAD :
					visitor._daload(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.AALOAD :
					visitor._aaload(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.BALOAD :
					visitor._baload(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.CALOAD :
					visitor._caload(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.SALOAD :
					visitor._saload(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ISTORE :
					index = u1At(this.classFileBytes, 1, pc);
					visitor._istore(pc - this.codeOffset, index);
					pc+=2;
					break;
				case IOpcodeMnemonics.LSTORE :
					index = u1At(this.classFileBytes, 1, pc);
					visitor._lstore(pc - this.codeOffset, index);
					pc+=2;
					break;
				case IOpcodeMnemonics.FSTORE :
					index = u1At(this.classFileBytes, 1, pc);
					visitor._fstore(pc - this.codeOffset, index);
					pc+=2;
					break;
				case IOpcodeMnemonics.DSTORE :
					index = u1At(this.classFileBytes, 1, pc);
					visitor._dstore(pc - this.codeOffset, index);
					pc+=2;
					break;
				case IOpcodeMnemonics.ASTORE :
					index = u1At(this.classFileBytes, 1, pc);
					visitor._astore(pc - this.codeOffset, index);
					pc+=2;
					break;
				case IOpcodeMnemonics.ISTORE_0 :
					visitor._istore_0(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ISTORE_1 :
					visitor._istore_1(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ISTORE_2 :
					visitor._istore_2(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ISTORE_3 :
					visitor._istore_3(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LSTORE_0 :
					visitor._lstore_0(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LSTORE_1 :
					visitor._lstore_1(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LSTORE_2 :
					visitor._lstore_2(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LSTORE_3 :
					visitor._lstore_3(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FSTORE_0 :
					visitor._fstore_0(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FSTORE_1 :
					visitor._fstore_1(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FSTORE_2 :
					visitor._fstore_2(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FSTORE_3 :
					visitor._fstore_3(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DSTORE_0 :
					visitor._dstore_0(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DSTORE_1 :
					visitor._dstore_1(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DSTORE_2 :
					visitor._dstore_2(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DSTORE_3 :
					visitor._dstore_3(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ASTORE_0 :
					visitor._astore_0(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ASTORE_1 :
					visitor._astore_1(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ASTORE_2 :
					visitor._astore_2(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ASTORE_3 :
					visitor._astore_3(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.IASTORE :
					visitor._iastore(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LASTORE :
					visitor._lastore(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FASTORE :
					visitor._fastore(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DASTORE :
					visitor._dastore(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.AASTORE :
					visitor._aastore(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.BASTORE :
					visitor._bastore(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.CASTORE :
					visitor._castore(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.SASTORE :
					visitor._sastore(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.POP :
					visitor._pop(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.POP2 :
					visitor._pop2(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DUP :
					visitor._dup(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DUP_X1 :
					visitor._dup_x1(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DUP_X2 :
					visitor._dup_x2(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DUP2 :
					visitor._dup2(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DUP2_X1 :
					visitor._dup2_x1(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DUP2_X2 :
					visitor._dup2_x2(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.SWAP :
					visitor._swap(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.IADD :
					visitor._iadd(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LADD :
					visitor._ladd(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FADD :
					visitor._fadd(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DADD :
					visitor._dadd(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ISUB :
					visitor._isub(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LSUB :
					visitor._lsub(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FSUB :
					visitor._fsub(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DSUB :
					visitor._dsub(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.IMUL :
					visitor._imul(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LMUL :
					visitor._lmul(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FMUL :
					visitor._fmul(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DMUL :
					visitor._dmul(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.IDIV :
					visitor._idiv(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LDIV :
					visitor._ldiv(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FDIV :
					visitor._fdiv(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DDIV :
					visitor._ddiv(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.IREM :
					visitor._irem(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LREM :
					visitor._lrem(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FREM :
					visitor._frem(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DREM :
					visitor._drem(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.INEG :
					visitor._ineg(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LNEG :
					visitor._lneg(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FNEG :
					visitor._fneg(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DNEG :
					visitor._dneg(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ISHL :
					visitor._ishl(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LSHL :
					visitor._lshl(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ISHR :
					visitor._ishr(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LSHR :
					visitor._lshr(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.IUSHR :
					visitor._iushr(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LUSHR :
					visitor._lushr(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.IAND :
					visitor._iand(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LAND :
					visitor._land(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.IOR :
					visitor._ior(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LOR :
					visitor._lor(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.IXOR :
					visitor._ixor(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LXOR :
					visitor._lxor(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.IINC :
					index = u1At(this.classFileBytes, 1, pc);
					_const = i1At(this.classFileBytes, 2, pc);
					visitor._iinc(pc - this.codeOffset, index, _const);
					pc+=3;
					break;
				case IOpcodeMnemonics.I2L :
					visitor._i2l(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.I2F :
					visitor._i2f(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.I2D :
					visitor._i2d(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.L2I :
					visitor._l2i(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.L2F :
					visitor._l2f(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.L2D :
					visitor._l2d(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.F2I :
					visitor._f2i(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.F2L :
					visitor._f2l(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.F2D :
					visitor._f2d(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.D2I :
					visitor._d2i(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.D2L :
					visitor._d2l(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.D2F :
					visitor._d2f(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.I2B :
					visitor._i2b(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.I2C :
					visitor._i2c(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.I2S :
					visitor._i2s(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LCMP :
					visitor._lcmp(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FCMPL :
					visitor._fcmpl(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FCMPG :
					visitor._fcmpg(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DCMPL :
					visitor._dcmpl(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DCMPG :
					visitor._dcmpg(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.IFEQ :
					branchOffset = i2At(this.classFileBytes, 1, pc);
					visitor._ifeq(pc - this.codeOffset , branchOffset);
					pc+=3;
					break;
				case IOpcodeMnemonics.IFNE :
					branchOffset = i2At(this.classFileBytes, 1, pc);
					visitor._ifne(pc - this.codeOffset , branchOffset);
					pc+=3;
					break;
				case IOpcodeMnemonics.IFLT :
					branchOffset = i2At(this.classFileBytes, 1, pc);
					visitor._iflt(pc - this.codeOffset , branchOffset);
					pc+=3;
					break;
				case IOpcodeMnemonics.IFGE :
					branchOffset = i2At(this.classFileBytes, 1, pc);
					visitor._ifge(pc - this.codeOffset , branchOffset);
					pc+=3;
					break;
				case IOpcodeMnemonics.IFGT :
					branchOffset = i2At(this.classFileBytes, 1, pc);
					visitor._ifgt(pc - this.codeOffset , branchOffset);
					pc+=3;
					break;
				case IOpcodeMnemonics.IFLE :
					branchOffset = i2At(this.classFileBytes, 1, pc);
					visitor._ifle(pc - this.codeOffset , branchOffset);
					pc+=3;
					break;
				case IOpcodeMnemonics.IF_ICMPEQ :
					branchOffset = i2At(this.classFileBytes, 1, pc);
					visitor._if_icmpeq(pc - this.codeOffset , branchOffset);
					pc+=3;
					break;
				case IOpcodeMnemonics.IF_ICMPNE :
					branchOffset = i2At(this.classFileBytes, 1, pc);
					visitor._if_icmpne(pc - this.codeOffset , branchOffset);
					pc+=3;
					break;
				case IOpcodeMnemonics.IF_ICMPLT :
					branchOffset = i2At(this.classFileBytes, 1, pc);
					visitor._if_icmplt(pc - this.codeOffset , branchOffset);
					pc+=3;
					break;
				case IOpcodeMnemonics.IF_ICMPGE :
					branchOffset = i2At(this.classFileBytes, 1, pc);
					visitor._if_icmpge(pc - this.codeOffset , branchOffset);
					pc+=3;
					break;
				case IOpcodeMnemonics.IF_ICMPGT :
					branchOffset = i2At(this.classFileBytes, 1, pc);
					visitor._if_icmpgt(pc - this.codeOffset , branchOffset);
					pc+=3;
					break;
				case IOpcodeMnemonics.IF_ICMPLE :
					branchOffset = i2At(this.classFileBytes, 1, pc);
					visitor._if_icmple(pc - this.codeOffset , branchOffset);
					pc+=3;
					break;
				case IOpcodeMnemonics.IF_ACMPEQ :
					branchOffset = i2At(this.classFileBytes, 1, pc);
					visitor._if_acmpeq(pc - this.codeOffset , branchOffset);
					pc+=3;
					break;
				case IOpcodeMnemonics.IF_ACMPNE :
					branchOffset = i2At(this.classFileBytes, 1, pc);
					visitor._if_acmpne(pc - this.codeOffset , branchOffset);
					pc+=3;
					break;
				case IOpcodeMnemonics.GOTO :
					branchOffset = i2At(this.classFileBytes, 1, pc);
					visitor._goto(pc - this.codeOffset , branchOffset);
					pc+=3;
					break;
				case IOpcodeMnemonics.JSR :
					branchOffset = i2At(this.classFileBytes, 1, pc);
					visitor._jsr(pc - this.codeOffset , branchOffset);
					pc+=3;
					break;
				case IOpcodeMnemonics.RET :
					index = u1At(this.classFileBytes, 1, pc);
					visitor._ret(pc - this.codeOffset, index);
					pc+=2;
					break;
				case IOpcodeMnemonics.TABLESWITCH :
					int startpc = pc;
					pc++;
					while (((pc - this.codeOffset) & 0x03) != 0) { // faster than % 4
						pc++;
					}
					int defaultOffset = i4At(this.classFileBytes, 0, pc);
					pc += 4;
					int low = i4At(this.classFileBytes, 0, pc);
					pc += 4;
					int high = i4At(this.classFileBytes, 0, pc);
					pc += 4;
					int length = high - low + 1;
					int[] jumpOffsets = new int[length];
					for (int i = 0; i < length; i++) {
						jumpOffsets[i] = i4At(this.classFileBytes, 0, pc);
						pc += 4;
					}
					visitor._tableswitch(startpc - this.codeOffset, defaultOffset, low, high, jumpOffsets);
					break;
				case IOpcodeMnemonics.LOOKUPSWITCH :
					startpc = pc;
					pc++;
					while (((pc - this.codeOffset) & 0x03) != 0) {
						pc++;
					}
					defaultOffset = i4At(this.classFileBytes, 0, pc);
					pc += 4;
					int npairs = (int) u4At(this.classFileBytes, 0, pc);
					int[][] offset_pairs = new int[npairs][2];
					pc += 4;
					for (int i = 0; i < npairs; i++) {
						offset_pairs[i][0] = i4At(this.classFileBytes, 0, pc);
						pc += 4;
						offset_pairs[i][1] = i4At(this.classFileBytes, 0, pc);
						pc += 4;
					}
					visitor._lookupswitch(startpc - this.codeOffset, defaultOffset, npairs, offset_pairs);
					break;
				case IOpcodeMnemonics.IRETURN :
					visitor._ireturn(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.LRETURN :
					visitor._lreturn(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.FRETURN :
					visitor._freturn(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.DRETURN :
					visitor._dreturn(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ARETURN :
					visitor._areturn(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.RETURN :
					visitor._return(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.GETSTATIC :
					index = u2At(this.classFileBytes, 1, pc);
					constantPoolEntry = this.constantPool.decodeEntry(index);
					if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Fieldref) {
						throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					visitor._getstatic(pc - this.codeOffset, index, constantPoolEntry);
					pc+=3;
					break;
				case IOpcodeMnemonics.PUTSTATIC :
					index = u2At(this.classFileBytes, 1, pc);
					constantPoolEntry = this.constantPool.decodeEntry(index);
					if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Fieldref) {
						throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					visitor._putstatic(pc - this.codeOffset, index, constantPoolEntry);
					pc+=3;
					break;
				case IOpcodeMnemonics.GETFIELD :
					index = u2At(this.classFileBytes, 1, pc);
					constantPoolEntry = this.constantPool.decodeEntry(index);
					if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Fieldref) {
						throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					visitor._getfield(pc - this.codeOffset, index, constantPoolEntry);
					pc+=3;
					break;
				case IOpcodeMnemonics.PUTFIELD :
					index = u2At(this.classFileBytes, 1, pc);
					constantPoolEntry = this.constantPool.decodeEntry(index);
					if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Fieldref) {
						throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					visitor._putfield(pc - this.codeOffset, index, constantPoolEntry);
					pc+=3;
					break;
				case IOpcodeMnemonics.INVOKEVIRTUAL :
					index = u2At(this.classFileBytes, 1, pc);
					constantPoolEntry = this.constantPool.decodeEntry(index);
					if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Methodref) {
						throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					visitor._invokevirtual(pc - this.codeOffset, index, constantPoolEntry);
					pc+=3;
					break;
				case IOpcodeMnemonics.INVOKESPECIAL :
					index = u2At(this.classFileBytes, 1, pc);
					constantPoolEntry = this.constantPool.decodeEntry(index);
					if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Methodref
							&& constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_InterfaceMethodref) {
						throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					visitor._invokespecial(pc - this.codeOffset, index, constantPoolEntry);
					pc+=3;
					break;
				case IOpcodeMnemonics.INVOKESTATIC :
					index = u2At(this.classFileBytes, 1, pc);
					constantPoolEntry = this.constantPool.decodeEntry(index);
					if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Methodref
							&& constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_InterfaceMethodref) {
						throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					visitor._invokestatic(pc - this.codeOffset, index, constantPoolEntry);
					pc+=3;
					break;
				case IOpcodeMnemonics.INVOKEINTERFACE :
					index = u2At(this.classFileBytes, 1, pc);
					constantPoolEntry = this.constantPool.decodeEntry(index);
					if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_InterfaceMethodref) {
						throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					byte count = (byte) u1At(this.classFileBytes, 3, pc);
					int extraArgs = u1At(this.classFileBytes, 4, pc);
					if (extraArgs != 0) {
						throw new ClassFormatException(ClassFormatException.INVALID_ARGUMENTS_FOR_INVOKEINTERFACE);
					}
					visitor._invokeinterface(pc - this.codeOffset, index, count, constantPoolEntry);
					pc += 5;
					break;
				case IOpcodeMnemonics.INVOKEDYNAMIC :
					index = u2At(this.classFileBytes, 1, pc);
					constantPoolEntry = this.constantPool.decodeEntry(index);
					if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_InvokeDynamic) {
						throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					visitor._invokedynamic(
							pc - this.codeOffset,
							index,
							constantPoolEntry);
					pc += 5;
					break;
				case IOpcodeMnemonics.NEW :
					index = u2At(this.classFileBytes, 1, pc);
					constantPoolEntry = this.constantPool.decodeEntry(index);
					if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Class) {
						throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					visitor._new(pc - this.codeOffset, index, constantPoolEntry);
					pc+=3;
					break;
				case IOpcodeMnemonics.NEWARRAY :
					int atype = u1At(this.classFileBytes, 1, pc);
					visitor._newarray(pc - this.codeOffset, atype);
					pc+=2;
					break;
				case IOpcodeMnemonics.ANEWARRAY :
					index = u2At(this.classFileBytes, 1, pc);
					constantPoolEntry = this.constantPool.decodeEntry(index);
					if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Class) {
						throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					visitor._anewarray(pc - this.codeOffset, index, constantPoolEntry);
					pc+=3;
					break;
				case IOpcodeMnemonics.ARRAYLENGTH :
					visitor._arraylength(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.ATHROW :
					visitor._athrow(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.CHECKCAST :
					index = u2At(this.classFileBytes, 1, pc);
					constantPoolEntry = this.constantPool.decodeEntry(index);
					if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Class) {
						throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					visitor._checkcast(pc - this.codeOffset, index, constantPoolEntry);
					pc+=3;
					break;
				case IOpcodeMnemonics.INSTANCEOF :
					index = u2At(this.classFileBytes, 1, pc);
					constantPoolEntry = this.constantPool.decodeEntry(index);
					if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Class) {
						throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					visitor._instanceof(pc - this.codeOffset, index, constantPoolEntry);
					pc+=3;
					break;
				case IOpcodeMnemonics.MONITORENTER :
					visitor._monitorenter(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.MONITOREXIT :
					visitor._monitorexit(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.WIDE :
					opcode = u1At(this.classFileBytes, 1, pc);
					if (opcode == IOpcodeMnemonics.IINC) {
						index = u2At(this.classFileBytes, 2, pc);
						_const = i2At(this.classFileBytes, 4, pc);
						visitor._wide(pc - this.codeOffset, opcode, index, _const);
						pc += 6;
					} else {
						index = u2At(this.classFileBytes, 2, pc);
						visitor._wide(pc - this.codeOffset , opcode, index);
						pc += 4;
					}
					break;
				case IOpcodeMnemonics.MULTIANEWARRAY :
					index = u2At(this.classFileBytes, 1, pc);
					constantPoolEntry = this.constantPool.decodeEntry(index);
					if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Class) {
						throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
					}
					int dimensions = u1At(this.classFileBytes, 3, pc);
					visitor._multianewarray(pc - this.codeOffset, index, dimensions, constantPoolEntry);
					pc+=4;
					break;
				case IOpcodeMnemonics.IFNULL :
					branchOffset = i2At(this.classFileBytes, 1, pc);
					visitor._ifnull(pc - this.codeOffset , branchOffset);
					pc+=3;
					break;
				case IOpcodeMnemonics.IFNONNULL :
					branchOffset = i2At(this.classFileBytes, 1, pc);
					visitor._ifnonnull(pc - this.codeOffset , branchOffset);
					pc+=3;
					break;
				case IOpcodeMnemonics.GOTO_W :
					branchOffset = i4At(this.classFileBytes, 1, pc);
					visitor._goto_w(pc - this.codeOffset, branchOffset);
					pc+=5;
					break;
				case IOpcodeMnemonics.JSR_W :
					branchOffset = i4At(this.classFileBytes, 1, pc);
					visitor._jsr_w(pc - this.codeOffset, branchOffset);
					pc+=5;
					break;
				case IOpcodeMnemonics.BREAKPOINT :
					visitor._breakpoint(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.IMPDEP1 :
					visitor._impdep1(pc - this.codeOffset);
					pc++;
					break;
				case IOpcodeMnemonics.IMPDEP2 :
					visitor._impdep2(pc - this.codeOffset);
					pc++;
					break;
				default:
					throw new ClassFormatException(ClassFormatException.INVALID_BYTECODE);
			}
			if (pc >= (this.codeLength + this.codeOffset)) {
				break;
			}
		}
	}
	private void checkConstantAndThrow(int kind) throws ClassFormatException {
		if (kind == IConstantPoolConstant.CONSTANT_Long ||	kind == IConstantPoolConstant.CONSTANT_Double) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
	}
}
