/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
 * Description of a Java opcodes visitor. This should be used to walk the opcodes
 * of a ICodeAttribute.
 *
 * Clients must subclass {@link ByteCodeVisitorAdapter} to define an implementation
 * of this interface.
 *
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IBytecodeVisitor {

	void _aaload(int pc);
	void _aastore(int pc);
	void _aconst_null(int pc);
	void _aload(int pc, int index);
	void _aload_0(int pc);
	void _aload_1(int pc);
	void _aload_2(int pc);
	void _aload_3(int pc);
	void _anewarray(
		int pc,
		int index,
		IConstantPoolEntry constantClass);
	void _areturn(int pc);
	void _arraylength(int pc);
	void _astore(int pc, int index);
	void _astore_0(int pc);
	void _astore_1(int pc);
	void _astore_2(int pc);
	void _astore_3(int pc);
	void _athrow(int pc);
	void _baload(int pc);
	void _bastore(int pc);
	void _bipush(int pc, byte _byte);
	void _caload(int pc);
	void _castore(int pc);
	void _checkcast(
		int pc,
		int index,
		IConstantPoolEntry constantClass);
	void _d2f(int pc);
	void _d2i(int pc);
	void _d2l(int pc);
	void _dadd(int pc);
	void _daload(int pc);
	void _dastore(int pc);
	void _dcmpg(int pc);
	void _dcmpl(int pc);
	void _dconst_0(int pc);
	void _dconst_1(int pc);
	void _ddiv(int pc);
	void _dload(int pc, int index);
	void _dload_0(int pc);
	void _dload_1(int pc);
	void _dload_2(int pc);
	void _dload_3(int pc);
	void _dmul(int pc);
	void _dneg(int pc);
	void _drem(int pc);
	void _dreturn(int pc);
	void _dstore(int pc, int index);
	void _dstore_0(int pc);
	void _dstore_1(int pc);
	void _dstore_2(int pc);
	void _dstore_3(int pc);
	void _dsub(int pc);
	void _dup(int pc);
	void _dup_x1(int pc);
	void _dup_x2(int pc);
	void _dup2(int pc);
	void _dup2_x1(int pc);
	void _dup2_x2(int pc);
	void _f2d(int pc);
	void _f2i(int pc);
	void _f2l(int pc);
	void _fadd(int pc);
	void _faload(int pc);
	void _fastore(int pc);
	void _fcmpg(int pc);
	void _fcmpl(int pc);
	void _fconst_0(int pc);
	void _fconst_1(int pc);
	void _fconst_2(int pc);
	void _fdiv(int pc);
	void _fload(int pc, int index);
	void _fload_0(int pc);
	void _fload_1(int pc);
	void _fload_2(int pc);
	void _fload_3(int pc);
	void _fmul(int pc);
	void _fneg(int pc);
	void _frem(int pc);
	void _freturn(int pc);
	void _fstore(int pc, int index);
	void _fstore_0(int pc);
	void _fstore_1(int pc);
	void _fstore_2(int pc);
	void _fstore_3(int pc);
	void _fsub(int pc);
	void _getfield(
		int pc,
		int index,
		IConstantPoolEntry constantFieldref);
	void _getstatic(
		int pc,
		int index,
		IConstantPoolEntry constantFieldref);
	void _goto(int pc, int branchOffset);
	void _goto_w(int pc, int branchOffset);
	void _i2b(int pc);
	void _i2c(int pc);
	void _i2d(int pc);
	void _i2f(int pc);
	void _i2l(int pc);
	void _i2s(int pc);
	void _iadd(int pc);
	void _iaload(int pc);
	void _iand(int pc);
	void _iastore(int pc);
	void _iconst_m1(int pc);
	void _iconst_0(int pc);
	void _iconst_1(int pc);
	void _iconst_2(int pc);
	void _iconst_3(int pc);
	void _iconst_4(int pc);
	void _iconst_5(int pc);
	void _idiv(int pc);
	void _if_acmpeq(int pc, int branchOffset);
	void _if_acmpne(int pc, int branchOffset);
	void _if_icmpeq(int pc, int branchOffset);
	void _if_icmpne(int pc, int branchOffset);
	void _if_icmplt(int pc, int branchOffset);
	void _if_icmpge(int pc, int branchOffset);
	void _if_icmpgt(int pc, int branchOffset);
	void _if_icmple(int pc, int branchOffset);
	void _ifeq(int pc, int branchOffset);
	void _ifne(int pc, int branchOffset);
	void _iflt(int pc, int branchOffset);
	void _ifge(int pc, int branchOffset);
	void _ifgt(int pc, int branchOffset);
	void _ifle(int pc, int branchOffset);
	void _ifnonnull(int pc, int branchOffset);
	void _ifnull(int pc, int branchOffset);
	void _iinc(int pc, int index, int _const);
	void _iload(int pc, int index);
	void _iload_0(int pc);
	void _iload_1(int pc);
	void _iload_2(int pc);
	void _iload_3(int pc);
	void _imul(int pc);
	void _ineg(int pc);
	void _instanceof(
		int pc,
		int index,
		IConstantPoolEntry constantClass);
	/**
	 * @since 3.6
	 * @deprecated This has been replaced with {@link #_invokedynamic(int, int, IConstantPoolEntry)}
	 */
	void _invokedynamic(
			int pc,
			int index,
			IConstantPoolEntry nameEntry,
			IConstantPoolEntry descriptorEntry);
	/**
	 * @since 3.8
	 */
	void _invokedynamic(
			int pc,
			int index,
			IConstantPoolEntry invokeDynamic);
	void _invokeinterface(
		int pc,
		int index,
		byte nargs,
		IConstantPoolEntry constantInterfaceMethodref);
	void _invokespecial(
		int pc,
		int index,
		IConstantPoolEntry constantMethodref);
	void _invokestatic(
		int pc,
		int index,
		IConstantPoolEntry constantMethodref);
	void _invokevirtual(
		int pc,
		int index,
		IConstantPoolEntry constantMethodref);
	void _ior(int pc);
	void _irem(int pc);
	void _ireturn(int pc);
	void _ishl(int pc);
	void _ishr(int pc);
	void _istore(int pc, int index);
	void _istore_0(int pc);
	void _istore_1(int pc);
	void _istore_2(int pc);
	void _istore_3(int pc);
	void _isub(int pc);
	void _iushr(int pc);
	void _ixor(int pc);
	void _jsr(int pc, int branchOffset);
	void _jsr_w(int pc, int branchOffset);
	void _l2d(int pc);
	void _l2f(int pc);
	void _l2i(int pc);
	void _ladd(int pc);
	void _laload(int pc);
	void _land(int pc);
	void _lastore(int pc);
	void _lcmp(int pc);
	void _lconst_0(int pc);
	void _lconst_1(int pc);
	void _ldc(int pc, int index, IConstantPoolEntry constantPoolEntry);
	void _ldc_w(int pc, int index, IConstantPoolEntry constantPoolEntry);
	void _ldc2_w(int pc, int index, IConstantPoolEntry constantPoolEntry);
	void _ldiv(int pc);
	void _lload(int pc, int index);
	void _lload_0(int pc);
	void _lload_1(int pc);
	void _lload_2(int pc);
	void _lload_3(int pc);
	void _lmul(int pc);
	void _lneg(int pc);
	void _lookupswitch(
		int pc,
		int defaultoffset,
		int npairs,
		int[][] offset_pairs);
	void _lor(int pc);
	void _lrem(int pc);
	void _lreturn(int pc);
	void _lshl(int pc);
	void _lshr(int pc);
	void _lstore(int pc, int index);
	void _lstore_0(int pc);
	void _lstore_1(int pc);
	void _lstore_2(int pc);
	void _lstore_3(int pc);
	void _lsub(int pc);
	void _lushr(int pc);
	void _lxor(int pc);
	void _monitorenter(int pc);
	void _monitorexit(int pc);
	void _multianewarray(
		int pc,
		int index,
		int dimensions,
		IConstantPoolEntry constantClass);
	void _new(
		int pc,
		int index,
		IConstantPoolEntry constantClass);
	void _newarray(int pc, int atype);
	void _nop(int pc);
	void _pop(int pc);
	void _pop2(int pc);
	void _putfield(
		int pc,
		int index,
		IConstantPoolEntry constantFieldref);
	void _putstatic(
		int pc,
		int index,
		IConstantPoolEntry constantFieldref);
	void _ret(int pc, int index);
	void _return(int pc);
	void _saload(int pc);
	void _sastore(int pc);
	void _sipush(int pc, short value);
	void _swap(int pc);
	void _tableswitch(
		int pc,
		int defaultoffset,
		int low,
		int high,
		int[] jump_offsets);
	void _wide(
		int pc,
		int opcode,
		int index);
	void _wide(
		int pc,
		int iincopcode,
		int index,
		int _const);
	void _breakpoint(int pc);
	void _impdep1(int pc);
	void _impdep2(int pc);
}
