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
 *******************************************************************************/
package org.eclipse.jdt.internal.core.util;

import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.util.ClassFileBytesDisassembler;
import org.eclipse.jdt.core.util.IBytecodeVisitor;
import org.eclipse.jdt.core.util.ICodeAttribute;
import org.eclipse.jdt.core.util.IConstantPoolConstant;
import org.eclipse.jdt.core.util.IConstantPoolEntry;
import org.eclipse.jdt.core.util.IConstantPoolEntry2;
import org.eclipse.jdt.core.util.ILocalVariableAttribute;
import org.eclipse.jdt.core.util.ILocalVariableTableEntry;
import org.eclipse.jdt.core.util.OpcodeStringValues;
import org.eclipse.jdt.core.util.IOpcodeMnemonics;

/**
 * Default implementation of ByteCodeVisitor
 */
public class DefaultBytecodeVisitor implements IBytecodeVisitor {
	private static final String EMPTY_CLASS_NAME = "\"\""; //$NON-NLS-1$
	private static final String EMPTY_LOCAL_NAME = ""; //$NON-NLS-1$
	private static final int T_BOOLEAN = 4;
	private static final int T_CHAR = 5;
	private static final int T_FLOAT = 6;
	private static final int T_DOUBLE = 7;
	private static final int T_BYTE = 8;
	private static final int T_SHORT = 9;
	private static final int T_INT = 10;
	private static final int T_LONG = 11;

	private StringBuffer buffer;
	private String lineSeparator;
	private int tabNumber;
	private int digitNumberForPC;
	private ILocalVariableTableEntry[] localVariableTableEntries;
	private int localVariableAttributeLength;
	private int mode;
	private char[][] parameterNames;
	private boolean isStatic;
	private int[] argumentSizes;

	public DefaultBytecodeVisitor(ICodeAttribute codeAttribute, char[][] parameterNames, char[] methodDescriptor, boolean isStatic, StringBuffer buffer, String lineSeparator, int tabNumber, int mode) {
		ILocalVariableAttribute localVariableAttribute = codeAttribute.getLocalVariableAttribute();
		this.localVariableAttributeLength = localVariableAttribute == null ? 0 : localVariableAttribute.getLocalVariableTableLength();
		if (this.localVariableAttributeLength != 0) {
			this.localVariableTableEntries = localVariableAttribute.getLocalVariableTable();
		} else {
			this.localVariableTableEntries = null;
		}
		this.buffer = buffer;
		this.lineSeparator = lineSeparator;
		this.tabNumber = tabNumber + 1;
		long codeLength = codeAttribute.getCodeLength();
		this.digitNumberForPC = Long.toString(codeLength).length();
		this.mode = mode;
		this.parameterNames = parameterNames;
		this.isStatic = isStatic;
		// compute argument sizes
		if (parameterNames != null) {
			char[][] parameterTypes = Signature.getParameterTypes(methodDescriptor);
			int length = parameterTypes.length;
			this.argumentSizes = new int[length];
			for (int i = 0; i < length; i++) {
				char[] parameterType = parameterTypes[i];
				this.argumentSizes[i] = parameterType.length == 1 && (parameterType[0] == 'D' || parameterType[0] == 'J') ? 2 : 1;
			}
		}
	}
	/**
	 * @see IBytecodeVisitor#_aaload(int)
	 */
	@Override
	public void _aaload(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.AALOAD]);
		writeNewLine();
	}
	private void dumpPcNumber(int pc) {
		writeTabs();
		int digitForPC = 1;
		if (pc != 0) {
			digitForPC = Integer.toString(pc).length();
		}
		for (int i = 0, max = this.digitNumberForPC - digitForPC; i < max; i++) {
			this.buffer.append(' ');
		}
		this.buffer.append(pc);
		this.buffer.append(Messages.disassembler_indentation);
	}

	/**
	 * @see IBytecodeVisitor#_aastore(int)
	 */
	@Override
	public void _aastore(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.AASTORE]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_aconst_null(int)
	 */
	@Override
	public void _aconst_null(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ACONST_NULL]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_aload_0(int)
	 */
	@Override
	public void _aload_0(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load,
			new String[] {
				OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ALOAD_0],
				getLocalVariableName(pc, 0)
			}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_aload_1(int)
	 */
	@Override
	public void _aload_1(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ALOAD_1],
			getLocalVariableName(pc, 1)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_aload_2(int)
	 */
	@Override
	public void _aload_2(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ALOAD_2],
			getLocalVariableName(pc, 2)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_aload_3(int)
	 */
	@Override
	public void _aload_3(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ALOAD_3],
			getLocalVariableName(pc, 3)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_aload(int, int)
	 */
	@Override
	public void _aload(int pc, int index) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ALOAD],
			getLocalVariableName(pc, index, true)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_anewarray(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _anewarray(int pc, int index, IConstantPoolEntry constantClass) {
		dumpPcNumber(pc);
		this.buffer
			.append(Messages.bind(Messages.classformat_anewarray, new String[] {
				OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ANEWARRAY],
				Integer.toString(index),
				returnConstantClassName(constantClass)
			}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_areturn(int)
	 */
	@Override
	public void _areturn(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ARETURN]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_arraylength(int)
	 */
	@Override
	public void _arraylength(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ARRAYLENGTH]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_astore_0(int)
	 */
	@Override
	public void _astore_0(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ASTORE_0],
			getLocalVariableName(pc, 0)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_astore_1(int)
	 */
	@Override
	public void _astore_1(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ASTORE_1],
			getLocalVariableName(pc, 1)
		}));
		writeNewLine();
	}
	private String getLocalVariableName(int pc, int index) {
		return getLocalVariableName(pc, index, false);
	}

	private String getLocalVariableName(int pc, int index, boolean showIndex) {
		int nextPC = pc + 1;
		switch(index) {
			case 0 :
			case 1 :
			case 2 :
			case 3 :
				break;
			default :
				nextPC = index <= 255 ? pc + 2 : pc + 3;
		}

		for (int i = 0, max = this.localVariableAttributeLength; i < max; i++) {
			final ILocalVariableTableEntry entry = this.localVariableTableEntries[i];
			final int startPC = entry.getStartPC();
			if (entry.getIndex() == index && (startPC <= nextPC) && ((startPC + entry.getLength()) > nextPC)) {
				final StringBuffer stringBuffer = new StringBuffer();
				if (showIndex) {
					stringBuffer.append(' ').append(index);
				}
				stringBuffer.append(' ').append('[').append(entry.getName()).append(']');
				return String.valueOf(stringBuffer);
			}
		}
		if (this.parameterNames != null) {
			if (index == 0) {
				if (!this.isStatic) {
					final StringBuffer stringBuffer = new StringBuffer();
					stringBuffer.append(' ').append('[').append("this").append(']'); //$NON-NLS-1$
					return String.valueOf(stringBuffer);
				}
			}
			int indexInParameterNames = index;
			if (index != 0) {
				int resolvedPosition = 0;
				if (!this.isStatic) {
					resolvedPosition = 1;
				}
				int i = 0;
				loop: for (int max = this.argumentSizes.length; i < max; i++) {
					if (index == resolvedPosition) {
						break loop;
					}
					resolvedPosition += this.argumentSizes[i];
				}
				indexInParameterNames = i;
			}
			if (indexInParameterNames < this.parameterNames.length
					&& this.parameterNames[indexInParameterNames] != null) {
				final StringBuffer stringBuffer = new StringBuffer();
				if (showIndex) {
					stringBuffer.append(' ').append(index);
				}
				stringBuffer.append(' ').append('[').append(this.parameterNames[indexInParameterNames]).append(']');
				return String.valueOf(stringBuffer);
			}
		}
		if (showIndex) {
			final StringBuffer stringBuffer = new StringBuffer();
			stringBuffer.append(' ').append(index);
			return String.valueOf(stringBuffer);
		}
		return EMPTY_LOCAL_NAME;
	}

	/**
	 * @see IBytecodeVisitor#_astore_2(int)
	 */
	@Override
	public void _astore_2(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ASTORE_2],
			getLocalVariableName(pc, 2)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_astore_3(int)
	 */
	@Override
	public void _astore_3(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ASTORE_3],
			getLocalVariableName(pc, 3)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_astore(int, int)
	 */
	@Override
	public void _astore(int pc, int index) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ASTORE],
			getLocalVariableName(pc, index, true)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_athrow(int)
	 */
	@Override
	public void _athrow(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ATHROW]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_baload(int)
	 */
	@Override
	public void _baload(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.BALOAD]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_bastore(int)
	 */
	@Override
	public void _bastore(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.BASTORE]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_bipush(int, byte)
	 */
	@Override
	public void _bipush(int pc, byte _byte) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.BIPUSH])
			.append(Messages.disassembler_space)
			.append(_byte);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_caload(int)
	 */
	@Override
	public void _caload(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.CALOAD]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_castore(int)
	 */
	@Override
	public void _castore(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.CASTORE]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_checkcast(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _checkcast(int pc, int index, IConstantPoolEntry constantClass) {
		dumpPcNumber(pc);
		this.buffer
			.append(Messages.bind(Messages.classformat_checkcast, new String[] {
				OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.CHECKCAST],
				Integer.toString(index),
				returnConstantClassName(constantClass)
			}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_d2f(int)
	 */
	@Override
	public void _d2f(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.D2F]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_d2i(int)
	 */
	@Override
	public void _d2i(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.D2I]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_d2l(int)
	 */
	@Override
	public void _d2l(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.D2L]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dadd(int)
	 */
	@Override
	public void _dadd(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DADD]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_daload(int)
	 */
	@Override
	public void _daload(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DALOAD]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dastore(int)
	 */
	@Override
	public void _dastore(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DASTORE]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dcmpg(int)
	 */
	@Override
	public void _dcmpg(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DCMPG]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dcmpl(int)
	 */
	@Override
	public void _dcmpl(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DCMPL]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dconst_0(int)
	 */
	@Override
	public void _dconst_0(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DCONST_0]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dconst_1(int)
	 */
	@Override
	public void _dconst_1(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DCONST_1]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_ddiv(int)
	 */
	@Override
	public void _ddiv(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DDIV]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dload_0(int)
	 */
	@Override
	public void _dload_0(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load,new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DLOAD_0],
			getLocalVariableName(pc, 0)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dload_1(int)
	 */
	@Override
	public void _dload_1(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DLOAD_1],
			getLocalVariableName(pc, 1)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dload_2(int)
	 */
	@Override
	public void _dload_2(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DLOAD_2],
			getLocalVariableName(pc, 2)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dload_3(int)
	 */
	@Override
	public void _dload_3(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DLOAD_3],
			getLocalVariableName(pc, 3)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dload(int, int)
	 */
	@Override
	public void _dload(int pc, int index) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DLOAD],
			getLocalVariableName(pc, index, true)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dmul(int)
	 */
	@Override
	public void _dmul(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DMUL]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dneg(int)
	 */
	@Override
	public void _dneg(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DNEG]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_drem(int)
	 */
	@Override
	public void _drem(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DREM]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dreturn(int)
	 */
	@Override
	public void _dreturn(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DRETURN]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dstore_0(int)
	 */
	@Override
	public void _dstore_0(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DSTORE_0],
			getLocalVariableName(pc, 0)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dstore_1(int)
	 */
	@Override
	public void _dstore_1(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DSTORE_1],
			getLocalVariableName(pc, 1)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dstore_2(int)
	 */
	@Override
	public void _dstore_2(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DSTORE_2],
			getLocalVariableName(pc, 2)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dstore_3(int)
	 */
	@Override
	public void _dstore_3(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DSTORE_3],
			getLocalVariableName(pc, 3)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dstore(int,int)
	 */
	@Override
	public void _dstore(int pc, int index) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DSTORE],
			getLocalVariableName(pc, index, true)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dsub(int)
	 */
	@Override
	public void _dsub(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DSUB]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dup_x1(int)
	 */
	@Override
	public void _dup_x1(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DUP_X1]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dup_x2(int)
	 */
	@Override
	public void _dup_x2(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DUP_X2]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dup(int)
	 */
	@Override
	public void _dup(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DUP]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dup2_x1(int)
	 */
	@Override
	public void _dup2_x1(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DUP2_X1]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dup2_x2(int)
	 */
	@Override
	public void _dup2_x2(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DUP2_X2]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_dup2(int)
	 */
	@Override
	public void _dup2(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.DUP2]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_f2d(int)
	 */
	@Override
	public void _f2d(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.F2D]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_f2i(int)
	 */
	@Override
	public void _f2i(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.F2I]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_f2l(int)
	 */
	@Override
	public void _f2l(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.F2L]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fadd(int)
	 */
	@Override
	public void _fadd(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FADD]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_faload(int)
	 */
	@Override
	public void _faload(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FALOAD]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fastore(int)
	 */
	@Override
	public void _fastore(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FASTORE]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fcmpg(int)
	 */
	@Override
	public void _fcmpg(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FCMPG]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fcmpl(int)
	 */
	@Override
	public void _fcmpl(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FCMPL]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fconst_0(int)
	 */
	@Override
	public void _fconst_0(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FCONST_0]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fconst_1(int)
	 */
	@Override
	public void _fconst_1(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FCONST_1]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fconst_2(int)
	 */
	@Override
	public void _fconst_2(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FCONST_2]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fdiv(int)
	 */
	@Override
	public void _fdiv(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FDIV]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fload_0(int)
	 */
	@Override
	public void _fload_0(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FLOAD_0],
			getLocalVariableName(pc, 0)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fload_1(int)
	 */
	@Override
	public void _fload_1(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FLOAD_1],
			getLocalVariableName(pc, 1)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fload_2(int)
	 */
	@Override
	public void _fload_2(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FLOAD_2],
			getLocalVariableName(pc, 2)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fload_3(int)
	 */
	@Override
	public void _fload_3(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FLOAD_3],
			getLocalVariableName(pc, 3)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fload(int, int)
	 */
	@Override
	public void _fload(int pc, int index) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FLOAD],
			getLocalVariableName(pc, index, true)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fmul(int)
	 */
	@Override
	public void _fmul(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FMUL]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fneg(int)
	 */
	@Override
	public void _fneg(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FNEG]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_frem(int)
	 */
	@Override
	public void _frem(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FREM]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_freturn(int)
	 */
	@Override
	public void _freturn(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FRETURN]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fstore_0(int)
	 */
	@Override
	public void _fstore_0(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store,new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FSTORE_0],
			getLocalVariableName(pc, 0)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fstore_1(int)
	 */
	@Override
	public void _fstore_1(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FSTORE_1],
			getLocalVariableName(pc, 1)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fstore_2(int)
	 */
	@Override
	public void _fstore_2(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FSTORE_2],
			getLocalVariableName(pc, 2)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fstore_3(int)
	 */
	@Override
	public void _fstore_3(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FSTORE_3],
			getLocalVariableName(pc, 3)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fstore(int, int)
	 */
	@Override
	public void _fstore(int pc, int index) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FSTORE],
			getLocalVariableName(pc, index, true)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_fsub(int)
	 */
	@Override
	public void _fsub(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.FSUB]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_getfield(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _getfield(int pc, int index, IConstantPoolEntry constantFieldref) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_getfield, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.GETFIELD],
			Integer.toString(index),
			returnDeclaringClassName(constantFieldref),
			new String(constantFieldref.getFieldName()),
			returnClassName(Signature.toCharArray(constantFieldref.getFieldDescriptor()))
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_getstatic(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _getstatic(int pc, int index, IConstantPoolEntry constantFieldref) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_getstatic, new String[] {
				OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.GETSTATIC],
				Integer.toString(index),
				returnDeclaringClassName(constantFieldref),
				new String(constantFieldref.getFieldName()),
				returnClassName(Signature.toCharArray(constantFieldref.getFieldDescriptor()))
			}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_goto_w(int, int)
	 */
	@Override
	public void _goto_w(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.GOTO_W])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_goto(int, int)
	 */
	@Override
	public void _goto(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.GOTO])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_i2b(int)
	 */
	@Override
	public void _i2b(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.I2B]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_i2c(int)
	 */
	@Override
	public void _i2c(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.I2C]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_i2d(int)
	 */
	@Override
	public void _i2d(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.I2D]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_i2f(int)
	 */
	@Override
	public void _i2f(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.I2F]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_i2l(int)
	 */
	@Override
	public void _i2l(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.I2L]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_i2s(int)
	 */
	@Override
	public void _i2s(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.I2S]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_iadd(int)
	 */
	@Override
	public void _iadd(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IADD]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_iaload(int)
	 */
	@Override
	public void _iaload(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IALOAD]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_iand(int)
	 */
	@Override
	public void _iand(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IAND]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_iastore(int)
	 */
	@Override
	public void _iastore(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IASTORE]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_if_acmpeq(int, int)
	 */
	@Override
	public void _if_acmpeq(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IF_ACMPEQ])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_if_acmpne(int, int)
	 */
	@Override
	public void _if_acmpne(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IF_ACMPNE])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_if_icmpeq(int, int)
	 */
	@Override
	public void _if_icmpeq(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IF_ICMPEQ])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_if_icmpge(int, int)
	 */
	@Override
	public void _if_icmpge(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IF_ICMPGE])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_if_icmpgt(int, int)
	 */
	@Override
	public void _if_icmpgt(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IF_ICMPGT])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_if_icmple(int, int)
	 */
	@Override
	public void _if_icmple(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IF_ICMPLE])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_if_icmplt(int, int)
	 */
	@Override
	public void _if_icmplt(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IF_ICMPLT])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_if_icmpne(int, int)
	 */
	@Override
	public void _if_icmpne(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IF_ICMPNE])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_iconst_0(int)
	 */
	@Override
	public void _iconst_0(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ICONST_0]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_iconst_1(int)
	 */
	@Override
	public void _iconst_1(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ICONST_1]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_iconst_2(int)
	 */
	@Override
	public void _iconst_2(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ICONST_2]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_iconst_3(int)
	 */
	@Override
	public void _iconst_3(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ICONST_3]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_iconst_4(int)
	 */
	@Override
	public void _iconst_4(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ICONST_4]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_iconst_5(int)
	 */
	@Override
	public void _iconst_5(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ICONST_5]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_iconst_m1(int)
	 */
	@Override
	public void _iconst_m1(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ICONST_M1]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_idiv(int)
	 */
	@Override
	public void _idiv(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IDIV]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_ifeq(int, int)
	 */
	@Override
	public void _ifeq(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IFEQ])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_ifge(int, int)
	 */
	@Override
	public void _ifge(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IFGE])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_ifgt(int, int)
	 */
	@Override
	public void _ifgt(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IFGT])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_ifle(int, int)
	 */
	@Override
	public void _ifle(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IFLE])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_iflt(int, int)
	 */
	@Override
	public void _iflt(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IFLT])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_ifne(int, int)
	 */
	@Override
	public void _ifne(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IFNE])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_ifnonnull(int, int)
	 */
	@Override
	public void _ifnonnull(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IFNONNULL])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_ifnull(int, int)
	 */
	@Override
	public void _ifnull(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IFNULL])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_iinc(int, int, int)
	 */
	@Override
	public void _iinc(int pc, int index, int _const) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_iinc, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IINC],
			Integer.toString(index),
			Integer.toString(_const),
			getLocalVariableName(pc, index, false)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_iload_0(int)
	 */
	@Override
	public void _iload_0(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ILOAD_0],
			getLocalVariableName(pc, 0)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_iload_1(int)
	 */
	@Override
	public void _iload_1(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ILOAD_1],
			getLocalVariableName(pc, 1)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_iload_2(int)
	 */
	@Override
	public void _iload_2(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ILOAD_2],
			getLocalVariableName(pc, 2)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_iload_3(int)
	 */
	@Override
	public void _iload_3(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ILOAD_3],
			getLocalVariableName(pc, 3)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_iload(int, int)
	 */
	@Override
	public void _iload(int pc, int index) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ILOAD],
			getLocalVariableName(pc, index, true)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_imul(int)
	 */
	@Override
	public void _imul(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IMUL]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_ineg(int)
	 */
	@Override
	public void _ineg(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.INEG]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_instanceof(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _instanceof(int pc, int index, IConstantPoolEntry constantClass) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_instanceof, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.INSTANCEOF],
			Integer.toString(index),
			returnConstantClassName(constantClass)
		}));
		writeNewLine();
	}
	/**
	 * @see IBytecodeVisitor#_invokedynamic(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _invokedynamic(
		int pc,
		int index,
		IConstantPoolEntry nameEntry,
		IConstantPoolEntry descriptorEntry) {

		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_invokedynamic, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.INVOKEDYNAMIC],
			Integer.toString(index),
			Util.toString(
				null,
				nameEntry.getUtf8Value(),
				descriptorEntry.getUtf8Value(),
				true,
				isCompact())
		}));
		writeNewLine();
	}
	/**
	 * @see IBytecodeVisitor#_invokedynamic(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _invokedynamic(
		int pc,
		int index,
		IConstantPoolEntry invokeDynamicEntry) {

		dumpPcNumber(pc);
		IConstantPoolEntry2 entry = (IConstantPoolEntry2) invokeDynamicEntry;
		this.buffer.append(Messages.bind(Messages.classformat_invokedynamic,
			new String[] {
				OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.INVOKEDYNAMIC],
				Integer.toString(index),
				Integer.toString(entry.getBootstrapMethodAttributeIndex()),
				Util.toString(
					null,
					entry.getMethodName(),
					entry.getMethodDescriptor(),
					true,
					isCompact())
			}));
		writeNewLine();
	}
	/**
	 * @see IBytecodeVisitor#_invokeinterface(int, int, byte, IConstantPoolEntry)
	 */
	@Override
	public void _invokeinterface(
		int pc,
		int index,
		byte nargs,
		IConstantPoolEntry constantInterfaceMethodref) {

		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_invokeinterface, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.INVOKEINTERFACE],
			Integer.toString(index),
			Integer.toString(nargs),
			Util.toString(
				constantInterfaceMethodref.getClassName(),
				constantInterfaceMethodref.getMethodName(),
				constantInterfaceMethodref.getMethodDescriptor(),
				true,
				isCompact())
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_invokespecial(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _invokespecial(int pc, int index, IConstantPoolEntry constantMethodref) {
		dumpPcNumber(pc);
		final String signature = returnMethodSignature(constantMethodref);
		this.buffer.append(Messages.bind(Messages.classformat_invokespecial, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.INVOKESPECIAL],
			Integer.toString(index),
			signature
		}));
		writeNewLine();
	}
	/**
	 * @see IBytecodeVisitor#_invokestatic(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _invokestatic(int pc, int index, IConstantPoolEntry constantMethodref) {
		dumpPcNumber(pc);
		final String signature = returnMethodSignature(constantMethodref);
		this.buffer.append(Messages.bind(Messages.classformat_invokestatic, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.INVOKESTATIC],
			Integer.toString(index),
			signature
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_invokevirtual(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _invokevirtual(int pc, int index, IConstantPoolEntry constantMethodref) {
		dumpPcNumber(pc);
		final String signature = returnMethodSignature(constantMethodref);
		this.buffer.append(Messages.bind(Messages.classformat_invokevirtual,new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.INVOKEVIRTUAL],
			Integer.toString(index),
			signature
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_ior(int)
	 */
	@Override
	public void _ior(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IOR]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_irem(int)
	 */
	@Override
	public void _irem(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IREM]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_ireturn(int)
	 */
	@Override
	public void _ireturn(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IRETURN]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_ishl(int)
	 */
	@Override
	public void _ishl(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ISHL]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_ishr(int)
	 */
	@Override
	public void _ishr(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ISHR]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_istore_0(int)
	 */
	@Override
	public void _istore_0(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ISTORE_0],
			getLocalVariableName(pc, 0)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_istore_1(int)
	 */
	@Override
	public void _istore_1(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ISTORE_1],
			getLocalVariableName(pc, 1)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_istore_2(int)
	 */
	@Override
	public void _istore_2(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ISTORE_2],
			getLocalVariableName(pc, 2)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_istore_3(int)
	 */
	@Override
	public void _istore_3(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ISTORE_3],
			getLocalVariableName(pc, 3)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_istore(int, int)
	 */
	@Override
	public void _istore(int pc, int index) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ISTORE],
			getLocalVariableName(pc, index, true)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_isub(int)
	 */
	@Override
	public void _isub(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.ISUB]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_iushr(int)
	 */
	@Override
	public void _iushr(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IUSHR]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_ixor(int)
	 */
	@Override
	public void _ixor(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IXOR]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_jsr_w(int, int)
	 */
	@Override
	public void _jsr_w(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.JSR_W])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_jsr(int, int)
	 */
	@Override
	public void _jsr(int pc, int branchOffset) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.JSR])
			.append(Messages.disassembler_space)
			.append(branchOffset + pc);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_l2d(int)
	 */
	@Override
	public void _l2d(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.L2D]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_l2f(int)
	 */
	@Override
	public void _l2f(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.L2F]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_l2i(int)
	 */
	@Override
	public void _l2i(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.L2I]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_ladd(int)
	 */
	@Override
	public void _ladd(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LADD]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_laload(int)
	 */
	@Override
	public void _laload(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LALOAD]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_land(int)
	 */
	@Override
	public void _land(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LAND]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lastore(int)
	 */
	@Override
	public void _lastore(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LASTORE]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lcmp(int)
	 */
	@Override
	public void _lcmp(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LCMP]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lconst_0(int)
	 */
	@Override
	public void _lconst_0(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LCONST_0]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lconst_1(int)
	 */
	@Override
	public void _lconst_1(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LCONST_1]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_ldc_w(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _ldc_w(int pc, int index, IConstantPoolEntry constantPoolEntry) {
		dumpPcNumber(pc);
		switch (constantPoolEntry.getKind()) {
			case IConstantPoolConstant.CONSTANT_Float :
				this.buffer.append(Messages.bind(Messages.classformat_ldc_w_float, new String[] {
					OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LDC_W],
					Integer.toString(index),
					Float.toString(constantPoolEntry.getFloatValue())
				}));
				break;
			case IConstantPoolConstant.CONSTANT_Integer :
				this.buffer.append(Messages.bind(Messages.classformat_ldc_w_integer, new String[] {
					OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LDC_W],
					Integer.toString(index),
					Integer.toString(constantPoolEntry.getIntegerValue())
				}));
				break;
			case IConstantPoolConstant.CONSTANT_String :
				this.buffer.append(Messages.bind(Messages.classformat_ldc_w_string, new String[] {
					OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LDC_W],
					Integer.toString(index),
					Disassembler.escapeString(constantPoolEntry.getStringValue())
				}));
				break;
			case IConstantPoolConstant.CONSTANT_Class :
				this.buffer.append(Messages.bind(Messages.classformat_ldc_w_class, new String[] {
					OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LDC_W],
					Integer.toString(index),
					returnConstantClassName(constantPoolEntry)
				}));
				break;
			case IConstantPoolConstant.CONSTANT_MethodType :
				appendConstantMethodType(this.buffer, Messages.classformat_ldc_w_methodhandle,
						IOpcodeMnemonics.LDC_W, index, constantPoolEntry);
				break;
			case IConstantPoolConstant.CONSTANT_MethodHandle :
				appendConstantMethodHandle(this.buffer, Messages.classformat_ldc_w_methodhandle,
						IOpcodeMnemonics.LDC_W, index, constantPoolEntry);
				break;
		}
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_ldc(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _ldc(int pc, int index, IConstantPoolEntry constantPoolEntry) {
		dumpPcNumber(pc);
		switch (constantPoolEntry.getKind()) {
			case IConstantPoolConstant.CONSTANT_Float :
				this.buffer.append(Messages.bind(Messages.classformat_ldc_w_float, new String[] {
					OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LDC],
					Integer.toString(index),
					Float.toString(constantPoolEntry.getFloatValue())
				}));
				break;
			case IConstantPoolConstant.CONSTANT_Integer :
				this.buffer.append(Messages.bind(Messages.classformat_ldc_w_integer, new String[] {
					OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LDC],
					Integer.toString(index),
					Integer.toString(constantPoolEntry.getIntegerValue())
				}));
				break;
			case IConstantPoolConstant.CONSTANT_String :
				this.buffer.append(Messages.bind(Messages.classformat_ldc_w_string, new String[] {
					OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LDC],
					Integer.toString(index),
					Disassembler.escapeString(constantPoolEntry.getStringValue())
				}));
				break;
			case IConstantPoolConstant.CONSTANT_Class :
				this.buffer.append(Messages.bind(Messages.classformat_ldc_w_class, new String[] {
					OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LDC],
					Integer.toString(index),
					returnConstantClassName(constantPoolEntry)
				}));
				break;
			case IConstantPoolConstant.CONSTANT_MethodType :
				appendConstantMethodType(this.buffer, Messages.classformat_ldc_w_methodhandle,
						IOpcodeMnemonics.LDC, index, constantPoolEntry);
				break;
			case IConstantPoolConstant.CONSTANT_MethodHandle :
				appendConstantMethodHandle(this.buffer, Messages.classformat_ldc_w_methodhandle,
						IOpcodeMnemonics.LDC, index, constantPoolEntry);
				break;
			case IConstantPoolConstant.CONSTANT_Dynamic :
				appendConstantDynamic(this.buffer, Messages.classformat_ldc_w_dynamic,
						IOpcodeMnemonics.LDC, index, constantPoolEntry);
				break;
		}
		writeNewLine();
	}

	private StringBuffer appendConstantMethodType(StringBuffer s, String messageKind, int opcode,
			int index, IConstantPoolEntry constantPoolEntry) {
		return s.append(Messages.bind(messageKind, new String[] {
				OpcodeStringValues.BYTECODE_NAMES[opcode],
				Integer.toString(index),
				new String(constantPoolEntry.getMethodDescriptor())
			}));
	}

	private StringBuffer appendConstantMethodHandle(StringBuffer s, String messageKind, int opcode,
			int index, IConstantPoolEntry constantPoolEntry) {
		return s.append(Messages.bind(messageKind, new String[] {
				OpcodeStringValues.BYTECODE_NAMES[opcode],
				Integer.toString(index),
				Integer.toString(((IConstantPoolEntry2) constantPoolEntry).getReferenceKind()),
				Integer.toString(((IConstantPoolEntry2) constantPoolEntry).getReferenceIndex())
			}));
	}
	private StringBuffer appendConstantDynamic(StringBuffer s, String messageKind, int opcode,
			int index, IConstantPoolEntry entry) {
		return s.append(Messages.bind(messageKind, new String[] {
				OpcodeStringValues.BYTECODE_NAMES[opcode],
				Integer.toString(index),
				Integer.toString(((IConstantPoolEntry2) entry).getBootstrapMethodAttributeIndex()),
				new String(entry.getFieldName()),
				returnClassName(Signature.toCharArray(entry.getFieldDescriptor()))
			}));
	}
	/**
	 * @see IBytecodeVisitor#_ldc2_w(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _ldc2_w(int pc, int index, IConstantPoolEntry constantPoolEntry) {
		dumpPcNumber(pc);
		switch (constantPoolEntry.getKind()) {
			case IConstantPoolConstant.CONSTANT_Long :
				this.buffer.append(Messages.bind(Messages.classformat_ldc2_w_long, new String[] {
					OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LDC2_W],
					Integer.toString(index),
					Long.toString(constantPoolEntry.getLongValue())
				}));
				break;
			case IConstantPoolConstant.CONSTANT_Double :
				this.buffer.append(Messages.bind(Messages.classformat_ldc2_w_double, new String[] {
					OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LDC2_W],
					Integer.toString(index),
					Double.toString(constantPoolEntry.getDoubleValue())
				}));
				break;
			case IConstantPoolConstant.CONSTANT_MethodType :
				appendConstantMethodType(this.buffer, Messages.classformat_ldc_w_methodhandle,
						IOpcodeMnemonics.LDC2_W, index, constantPoolEntry);
				break;
			case IConstantPoolConstant.CONSTANT_MethodHandle :
				appendConstantMethodHandle(this.buffer, Messages.classformat_ldc_w_methodhandle,
						IOpcodeMnemonics.LDC2_W, index, constantPoolEntry);
				break;
			case IConstantPoolConstant.CONSTANT_Dynamic :
				appendConstantDynamic(this.buffer, Messages.classformat_ldc_w_dynamic,
						IOpcodeMnemonics.LDC2_W, index, constantPoolEntry);
				break;
		}
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_ldiv(int)
	 */
	@Override
	public void _ldiv(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LDIV]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lload_0(int)
	 */
	@Override
	public void _lload_0(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LLOAD_0],
			getLocalVariableName(pc, 0)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lload_1(int)
	 */
	@Override
	public void _lload_1(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LLOAD_1],
			getLocalVariableName(pc, 1)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lload_2(int)
	 */
	@Override
	public void _lload_2(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LLOAD_2],
			getLocalVariableName(pc, 2)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lload_3(int)
	 */
	@Override
	public void _lload_3(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LLOAD_3],
			getLocalVariableName(pc, 3)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lload(int, int)
	 */
	@Override
	public void _lload(int pc, int index) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_load, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LLOAD],
			getLocalVariableName(pc, index, true)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lmul(int)
	 */
	@Override
	public void _lmul(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LMUL]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lneg(int)
	 */
	@Override
	public void _lneg(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LNEG]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lookupswitch(int, int, int, int[][])
	 */
	@Override
	public void _lookupswitch(int pc, int defaultoffset, int npairs, int[][] offset_pairs) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LOOKUPSWITCH])
			.append(" default: ") //$NON-NLS-1$
			.append(defaultoffset + pc);
		writeNewLine();
		for (int i = 0; i < npairs; i++) {
			writeExtraTabs(3);
			this.buffer
				.append("case ") //$NON-NLS-1$
				.append(offset_pairs[i][0])
				.append(": ") //$NON-NLS-1$
				.append(offset_pairs[i][1] + pc);
			writeNewLine();
		}
	}

	/**
	 * @see IBytecodeVisitor#_lor(int)
	 */
	@Override
	public void _lor(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LOR]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lrem(int)
	 */
	@Override
	public void _lrem(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LREM]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lreturn(int)
	 */
	@Override
	public void _lreturn(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LRETURN]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lshl(int)
	 */
	@Override
	public void _lshl(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LSHL]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lshr(int)
	 */
	@Override
	public void _lshr(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LSHR]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lstore_0(int)
	 */
	@Override
	public void _lstore_0(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LSTORE_0],
			getLocalVariableName(pc, 0)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lstore_1(int)
	 */
	@Override
	public void _lstore_1(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LSTORE_1],
			getLocalVariableName(pc, 1)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lstore_2(int)
	 */
	@Override
	public void _lstore_2(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LSTORE_2],
			getLocalVariableName(pc, 2)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lstore_3(int)
	 */
	@Override
	public void _lstore_3(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LSTORE_3],
			getLocalVariableName(pc, 3)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lstore(int, int)
	 */
	@Override
	public void _lstore(int pc, int index) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_store, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LSTORE],
			getLocalVariableName(pc, index, true)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lsub(int)
	 */
	@Override
	public void _lsub(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LSUB]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lushr(int)
	 */
	@Override
	public void _lushr(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LUSHR]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_lxor(int)
	 */
	@Override
	public void _lxor(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.LXOR]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_monitorenter(int)
	 */
	@Override
	public void _monitorenter(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.MONITORENTER]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_monitorexit(int)
	 */
	@Override
	public void _monitorexit(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.MONITOREXIT]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_multianewarray(int, int, int, IConstantPoolEntry)
	 */
	@Override
	public void _multianewarray(
		int pc,
		int index,
		int dimensions,
		IConstantPoolEntry constantClass) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_multianewarray, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.MULTIANEWARRAY],
			Integer.toString(index),
			returnConstantClassName(constantClass)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_new(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _new(int pc, int index, IConstantPoolEntry constantClass) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_new, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.NEW],
			Integer.toString(index),
			returnConstantClassName(constantClass)
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_newarray(int, int)
	 */
	@Override
	public void _newarray(int pc, int atype) {
		dumpPcNumber(pc);
		switch(atype) {
			case T_BOOLEAN :
				this.buffer.append(Messages.bind(Messages.classformat_newarray_boolean, new String[] {
					OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.NEWARRAY],
					Integer.toString(atype)
				}));
				break;
			case T_CHAR :
				this.buffer.append(Messages.bind(Messages.classformat_newarray_char, new String[] {
					OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.NEWARRAY],
					Integer.toString(atype)
				}));
				break;
			case T_FLOAT :
				this.buffer.append(Messages.bind(Messages.classformat_newarray_float, new String[] {
					OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.NEWARRAY],
					Integer.toString(atype)
				}));
				break;
			case T_DOUBLE :
				this.buffer.append(Messages.bind(Messages.classformat_newarray_double, new String[] {
					OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.NEWARRAY],
					Integer.toString(atype)
				}));
				break;
			case T_BYTE :
				this.buffer.append(Messages.bind(Messages.classformat_newarray_byte, new String[] {
					OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.NEWARRAY],
					Integer.toString(atype)
				}));
				break;
			case T_SHORT :
				this.buffer.append(Messages.bind(Messages.classformat_newarray_short, new String[] {
					OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.NEWARRAY],
					Integer.toString(atype)
				}));
				break;
			case T_INT :
				this.buffer.append(Messages.bind(Messages.classformat_newarray_int, new String[] {
					OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.NEWARRAY],
					Integer.toString(atype)
				}));
				break;
			case T_LONG :
				this.buffer.append(Messages.bind(Messages.classformat_newarray_long, new String[] {
					OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.NEWARRAY],
					Integer.toString(atype)
				}));
		}
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_nop(int)
	 */
	@Override
	public void _nop(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.NOP]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_pop(int)
	 */
	@Override
	public void _pop(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.POP]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_pop2(int)
	 */
	@Override
	public void _pop2(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.POP2]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_putfield(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _putfield(int pc, int index, IConstantPoolEntry constantFieldref) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_putfield, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.PUTFIELD],
			Integer.toString(index),
			returnDeclaringClassName(constantFieldref),
			new String(constantFieldref.getFieldName()),
			returnClassName(Signature.toCharArray(constantFieldref.getFieldDescriptor()))
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_putstatic(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _putstatic(int pc, int index, IConstantPoolEntry constantFieldref) {
		dumpPcNumber(pc);
		this.buffer.append(Messages.bind(Messages.classformat_putstatic, new String[] {
			OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.PUTSTATIC],
			Integer.toString(index),
			returnDeclaringClassName(constantFieldref),
			new String(constantFieldref.getFieldName()),
			returnClassName(Signature.toCharArray(constantFieldref.getFieldDescriptor()))
		}));
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_ret(int, int)
	 */
	@Override
	public void _ret(int pc, int index) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.RET])
			.append(Messages.disassembler_space)
			.append(index);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_return(int)
	 */
	@Override
	public void _return(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.RETURN]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_saload(int)
	 */
	@Override
	public void _saload(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.SALOAD]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_sastore(int)
	 */
	@Override
	public void _sastore(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.SASTORE]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_sipush(int, short)
	 */
	@Override
	public void _sipush(int pc, short value) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.SIPUSH])
			.append(Messages.disassembler_space)
			.append(value);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_swap(int)
	 */
	@Override
	public void _swap(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.SWAP]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_tableswitch(int, int, int, int, int[])
	 */
	@Override
	public void _tableswitch(
		int pc,
		int defaultoffset,
		int low,
		int high,
		int[] jump_offsets) {

		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.TABLESWITCH])
			.append(" default: ") //$NON-NLS-1$
			.append(defaultoffset + pc);
		writeNewLine();
		for (int i = low; i < high + 1; i++) {
			writeExtraTabs(3);
			this.buffer
				.append("case ") //$NON-NLS-1$
				.append(i)
				.append(": ") //$NON-NLS-1$
				.append(jump_offsets[i - low] + pc);
			writeNewLine();
		}
	}

	/**
	 * @see IBytecodeVisitor#_wide(int, int, int)
	 */
	@Override
	public void _wide(int pc, int iincopcode, int index, int _const) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.WIDE]);
		writeNewLine();
		_iinc(pc + 1, index, _const);
	}

	/**
	 * @see IBytecodeVisitor#_wide(int, int, int)
	 */
	@Override
	public void _wide(int pc, int opcode, int index) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.WIDE]);
		writeNewLine();
		switch(opcode) {
			case IOpcodeMnemonics.ILOAD :
				_iload(pc + 1, index);
				break;
			case IOpcodeMnemonics.FLOAD :
				_fload(pc + 1, index);
				break;
			case IOpcodeMnemonics.ALOAD :
				_aload(pc + 1, index);
				break;
			case IOpcodeMnemonics.LLOAD :
				_lload(pc + 1, index);
				break;
			case IOpcodeMnemonics.DLOAD :
				_dload(pc + 1, index);
				break;
			case IOpcodeMnemonics.ISTORE :
				_istore(pc + 1, index);
				break;
			case IOpcodeMnemonics.FSTORE :
				_fstore(pc + 1, index);
				break;
			case IOpcodeMnemonics.ASTORE :
				_astore(pc + 1, index);
				break;
			case IOpcodeMnemonics.LSTORE :
				_lstore(pc + 1, index);
				break;
			case IOpcodeMnemonics.DSTORE :
				_dstore(pc + 1, index);
				break;
			case IOpcodeMnemonics.RET :
				_ret(pc + 1, index);
		}
	}

	/**
	 * @see IBytecodeVisitor#_breakpoint(int)
	 */
	@Override
	public void _breakpoint(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.BREAKPOINT]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_impdep1(int)
	 */
	@Override
	public void _impdep1(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IMPDEP1]);
		writeNewLine();
	}

	/**
	 * @see IBytecodeVisitor#_impdep2(int)
	 */
	@Override
	public void _impdep2(int pc) {
		dumpPcNumber(pc);
		this.buffer.append(OpcodeStringValues.BYTECODE_NAMES[IOpcodeMnemonics.IMPDEP2]);
		writeNewLine();
	}

	private boolean isCompact() {
		return (this.mode & ClassFileBytesDisassembler.COMPACT) != 0;
	}

	private String returnConstantClassName(IConstantPoolEntry constantClass) {
		char[] className = constantClass.getClassInfoName();
		if (className.length == 0) {
			return EMPTY_CLASS_NAME;
		}
		switch(className[0]) {
			case '[' :
				StringBuffer classNameBuffer = new StringBuffer();
				Util.appendTypeSignature(className, 0, classNameBuffer, isCompact());
				return classNameBuffer.toString();
			default:
				return returnClassName(className);
		}
	}
	private String returnClassName(char[] classInfoName) {
		if (classInfoName.length == 0) {
			return EMPTY_CLASS_NAME;
		} else if (isCompact()) {
			int lastIndexOfSlash = CharOperation.lastIndexOf('/', classInfoName);
			if (lastIndexOfSlash != -1) {
				return new String(classInfoName, lastIndexOfSlash + 1, classInfoName.length - lastIndexOfSlash - 1);
			}
		}
		CharOperation.replace(classInfoName, '/', '.');
		return new String(classInfoName);
	}

	private String returnDeclaringClassName(IConstantPoolEntry constantRef) {
		final char[] className = constantRef.getClassName();
		return returnClassName(className);
	}

	private String returnMethodSignature(IConstantPoolEntry constantMethodref) {
		final char[] methodDescriptor = constantMethodref.getMethodDescriptor();
		CharOperation.replace(methodDescriptor, '$', '#');
		final char[] signature = Util.toString(
				constantMethodref.getClassName(),
				constantMethodref.getMethodName(),
				methodDescriptor,
				true,
				isCompact()).toCharArray();
		CharOperation.replace(signature, '#', '$');
		return String.valueOf(signature);
	}

	private void writeNewLine() {
		this.buffer.append(this.lineSeparator);
	}

	private void writeTabs() {
		for (int i = 0, max = this.tabNumber; i < max; i++) {
			this.buffer.append(Messages.disassembler_indentation);
		}
	}

	private void writeExtraTabs(int extraTabs) {
		for (int i = 0, max = this.tabNumber + extraTabs; i < max; i++) {
			this.buffer.append(Messages.disassembler_indentation);
		}
	}

}
