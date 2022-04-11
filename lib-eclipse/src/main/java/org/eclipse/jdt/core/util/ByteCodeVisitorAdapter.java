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
 * Adapter that implements the IBytecodeVisitor. This class is intended to
 * be subclassed by clients.
 *
 * @since 2.0
 */
public class ByteCodeVisitorAdapter implements IBytecodeVisitor {
	/**
	 * @see IBytecodeVisitor#_aaload(int)
	 */
	@Override
	public void _aaload(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_aastore(int)
	 */
	@Override
	public void _aastore(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_aconst_null(int)
	 */
	@Override
	public void _aconst_null(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_aload_0(int)
	 */
	@Override
	public void _aload_0(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_aload_1(int)
	 */
	@Override
	public void _aload_1(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_aload_2(int)
	 */
	@Override
	public void _aload_2(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_aload_3(int)
	 */
	@Override
	public void _aload_3(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_aload(int, int)
	 */
	@Override
	public void _aload(int pc, int index) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_anewarray(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _anewarray(int pc, int index, IConstantPoolEntry constantClass) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_areturn(int)
	 */
	@Override
	public void _areturn(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_arraylength(int)
	 */
	@Override
	public void _arraylength(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_astore_0(int)
	 */
	@Override
	public void _astore_0(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_astore_1(int)
	 */
	@Override
	public void _astore_1(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_astore_2(int)
	 */
	@Override
	public void _astore_2(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_astore_3(int)
	 */
	@Override
	public void _astore_3(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_astore(int, int)
	 */
	@Override
	public void _astore(int pc, int index) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_athrow(int)
	 */
	@Override
	public void _athrow(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_baload(int)
	 */
	@Override
	public void _baload(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_bastore(int)
	 */
	@Override
	public void _bastore(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_bipush(int, byte)
	 */
	@Override
	public void _bipush(int pc, byte _byte) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_caload(int)
	 */
	@Override
	public void _caload(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_castore(int)
	 */
	@Override
	public void _castore(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_checkcast(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _checkcast(int pc, int index, IConstantPoolEntry constantClass) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_d2f(int)
	 */
	@Override
	public void _d2f(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_d2i(int)
	 */
	@Override
	public void _d2i(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_d2l(int)
	 */
	@Override
	public void _d2l(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dadd(int)
	 */
	@Override
	public void _dadd(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_daload(int)
	 */
	@Override
	public void _daload(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dastore(int)
	 */
	@Override
	public void _dastore(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dcmpg(int)
	 */
	@Override
	public void _dcmpg(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dcmpl(int)
	 */
	@Override
	public void _dcmpl(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dconst_0(int)
	 */
	@Override
	public void _dconst_0(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dconst_1(int)
	 */
	@Override
	public void _dconst_1(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ddiv(int)
	 */
	@Override
	public void _ddiv(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dload_0(int)
	 */
	@Override
	public void _dload_0(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dload_1(int)
	 */
	@Override
	public void _dload_1(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dload_2(int)
	 */
	@Override
	public void _dload_2(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dload_3(int)
	 */
	@Override
	public void _dload_3(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dload(int, int)
	 */
	@Override
	public void _dload(int pc, int index) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dmul(int)
	 */
	@Override
	public void _dmul(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dneg(int)
	 */
	@Override
	public void _dneg(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_drem(int)
	 */
	@Override
	public void _drem(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dreturn(int)
	 */
	@Override
	public void _dreturn(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dstore_0(int)
	 */
	@Override
	public void _dstore_0(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dstore_1(int)
	 */
	@Override
	public void _dstore_1(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dstore_2(int)
	 */
	@Override
	public void _dstore_2(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dstore_3(int)
	 */
	@Override
	public void _dstore_3(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dstore(int, int)
	 */
	@Override
	public void _dstore(int pc, int index) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dsub(int)
	 */
	@Override
	public void _dsub(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dup_x1(int)
	 */
	@Override
	public void _dup_x1(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dup_x2(int)
	 */
	@Override
	public void _dup_x2(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dup(int)
	 */
	@Override
	public void _dup(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dup2_x1(int)
	 */
	@Override
	public void _dup2_x1(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dup2_x2(int)
	 */
	@Override
	public void _dup2_x2(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_dup2(int)
	 */
	@Override
	public void _dup2(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_f2d(int)
	 */
	@Override
	public void _f2d(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_f2i(int)
	 */
	@Override
	public void _f2i(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_f2l(int)
	 */
	@Override
	public void _f2l(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fadd(int)
	 */
	@Override
	public void _fadd(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_faload(int)
	 */
	@Override
	public void _faload(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fastore(int)
	 */
	@Override
	public void _fastore(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fcmpg(int)
	 */
	@Override
	public void _fcmpg(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fcmpl(int)
	 */
	@Override
	public void _fcmpl(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fconst_0(int)
	 */
	@Override
	public void _fconst_0(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fconst_1(int)
	 */
	@Override
	public void _fconst_1(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fconst_2(int)
	 */
	@Override
	public void _fconst_2(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fdiv(int)
	 */
	@Override
	public void _fdiv(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fload_0(int)
	 */
	@Override
	public void _fload_0(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fload_1(int)
	 */
	@Override
	public void _fload_1(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fload_2(int)
	 */
	@Override
	public void _fload_2(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fload_3(int)
	 */
	@Override
	public void _fload_3(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fload(int, int)
	 */
	@Override
	public void _fload(int pc, int index) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fmul(int)
	 */
	@Override
	public void _fmul(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fneg(int)
	 */
	@Override
	public void _fneg(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_frem(int)
	 */
	@Override
	public void _frem(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_freturn(int)
	 */
	@Override
	public void _freturn(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fstore_0(int)
	 */
	@Override
	public void _fstore_0(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fstore_1(int)
	 */
	@Override
	public void _fstore_1(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fstore_2(int)
	 */
	@Override
	public void _fstore_2(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fstore_3(int)
	 */
	@Override
	public void _fstore_3(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fstore(int, int)
	 */
	@Override
	public void _fstore(int pc, int index) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_fsub(int)
	 */
	@Override
	public void _fsub(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_getfield(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _getfield(int pc, int index, IConstantPoolEntry constantFieldref) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_getstatic(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _getstatic(
		int pc,
		int index,
		IConstantPoolEntry constantFieldref) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_goto_w(int, int)
	 */
	@Override
	public void _goto_w(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_goto(int, int)
	 */
	@Override
	public void _goto(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_i2b(int)
	 */
	@Override
	public void _i2b(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_i2c(int)
	 */
	@Override
	public void _i2c(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_i2d(int)
	 */
	@Override
	public void _i2d(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_i2f(int)
	 */
	@Override
	public void _i2f(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_i2l(int)
	 */
	@Override
	public void _i2l(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_i2s(int)
	 */
	@Override
	public void _i2s(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_iadd(int)
	 */
	@Override
	public void _iadd(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_iaload(int)
	 */
	@Override
	public void _iaload(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_iand(int)
	 */
	@Override
	public void _iand(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_iastore(int)
	 */
	@Override
	public void _iastore(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_iconst_0(int)
	 */
	@Override
	public void _iconst_0(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_iconst_1(int)
	 */
	@Override
	public void _iconst_1(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_iconst_2(int)
	 */
	@Override
	public void _iconst_2(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_iconst_3(int)
	 */
	@Override
	public void _iconst_3(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_iconst_4(int)
	 */
	@Override
	public void _iconst_4(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_iconst_5(int)
	 */
	@Override
	public void _iconst_5(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_iconst_m1(int)
	 */
	@Override
	public void _iconst_m1(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_idiv(int)
	 */
	@Override
	public void _idiv(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_if_acmpeq(int, int)
	 */
	@Override
	public void _if_acmpeq(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_if_acmpne(int, int)
	 */
	@Override
	public void _if_acmpne(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_if_icmpeq(int, int)
	 */
	@Override
	public void _if_icmpeq(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_if_icmpge(int, int)
	 */
	@Override
	public void _if_icmpge(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_if_icmpgt(int, int)
	 */
	@Override
	public void _if_icmpgt(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_if_icmple(int, int)
	 */
	@Override
	public void _if_icmple(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_if_icmplt(int, int)
	 */
	@Override
	public void _if_icmplt(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_if_icmpne(int, int)
	 */
	@Override
	public void _if_icmpne(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ifeq(int, int)
	 */
	@Override
	public void _ifeq(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ifge(int, int)
	 */
	@Override
	public void _ifge(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ifgt(int, int)
	 */
	@Override
	public void _ifgt(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ifle(int, int)
	 */
	@Override
	public void _ifle(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_iflt(int, int)
	 */
	@Override
	public void _iflt(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ifne(int, int)
	 */
	@Override
	public void _ifne(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ifnonnull(int, int)
	 */
	@Override
	public void _ifnonnull(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ifnull(int, int)
	 */
	@Override
	public void _ifnull(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_iinc(int, int, int)
	 */
	@Override
	public void _iinc(int pc, int index, int _const) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_iload_0(int)
	 */
	@Override
	public void _iload_0(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_iload_1(int)
	 */
	@Override
	public void _iload_1(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_iload_2(int)
	 */
	@Override
	public void _iload_2(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_iload_3(int)
	 */
	@Override
	public void _iload_3(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_iload(int, int)
	 */
	@Override
	public void _iload(int pc, int index) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_imul(int)
	 */
	@Override
	public void _imul(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ineg(int)
	 */
	@Override
	public void _ineg(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_instanceof(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _instanceof(int pc, int index, IConstantPoolEntry constantClass) {
		// default behavior is to do nothing
	}
	/**
	 * @see IBytecodeVisitor#_invokedynamic(int, int, IConstantPoolEntry, IConstantPoolEntry)
	 * @since 3.6
	 * @deprecated This has been replaced with {@link IBytecodeVisitor#_invokedynamic(int, int, IConstantPoolEntry)}
	 */
	@Override
	public void _invokedynamic(
			int pc,
			int index,
			IConstantPoolEntry nameEntry,
			IConstantPoolEntry descriptorEntry) {
		// default behavior is to do nothing
	}
	/**
	 * @see IBytecodeVisitor#_invokedynamic(int, int, IConstantPoolEntry)
	 * @since 3.8
	 */
	@Override
	public void _invokedynamic(
			int pc,
			int index,
			IConstantPoolEntry invokeDynamicEntry) {
		// default behavior is to do nothing
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
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_invokespecial(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _invokespecial(
		int pc,
		int index,
		IConstantPoolEntry constantMethodref) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_invokestatic(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _invokestatic(
		int pc,
		int index,
		IConstantPoolEntry constantMethodref) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_invokevirtual(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _invokevirtual(
		int pc,
		int index,
		IConstantPoolEntry constantMethodref) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ior(int)
	 */
	@Override
	public void _ior(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_irem(int)
	 */
	@Override
	public void _irem(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ireturn(int)
	 */
	@Override
	public void _ireturn(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ishl(int)
	 */
	@Override
	public void _ishl(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ishr(int)
	 */
	@Override
	public void _ishr(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_istore_0(int)
	 */
	@Override
	public void _istore_0(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_istore_1(int)
	 */
	@Override
	public void _istore_1(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_istore_2(int)
	 */
	@Override
	public void _istore_2(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_istore_3(int)
	 */
	@Override
	public void _istore_3(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_istore(int, int)
	 */
	@Override
	public void _istore(int pc, int index) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_isub(int)
	 */
	@Override
	public void _isub(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_iushr(int)
	 */
	@Override
	public void _iushr(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ixor(int)
	 */
	@Override
	public void _ixor(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_jsr_w(int, int)
	 */
	@Override
	public void _jsr_w(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_jsr(int, int)
	 */
	@Override
	public void _jsr(int pc, int branchOffset) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_l2d(int)
	 */
	@Override
	public void _l2d(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_l2f(int)
	 */
	@Override
	public void _l2f(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_l2i(int)
	 */
	@Override
	public void _l2i(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ladd(int)
	 */
	@Override
	public void _ladd(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_laload(int)
	 */
	@Override
	public void _laload(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_land(int)
	 */
	@Override
	public void _land(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lastore(int)
	 */
	@Override
	public void _lastore(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lcmp(int)
	 */
	@Override
	public void _lcmp(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lconst_0(int)
	 */
	@Override
	public void _lconst_0(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lconst_1(int)
	 */
	@Override
	public void _lconst_1(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ldc_w(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _ldc_w(int pc, int index, IConstantPoolEntry constantPoolEntry) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ldc(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _ldc(int pc, int index, IConstantPoolEntry constantPoolEntry) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ldc2_w(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _ldc2_w(int pc, int index, IConstantPoolEntry constantPoolEntry) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ldiv(int)
	 */
	@Override
	public void _ldiv(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lload_0(int)
	 */
	@Override
	public void _lload_0(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lload_1(int)
	 */
	@Override
	public void _lload_1(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lload_2(int)
	 */
	@Override
	public void _lload_2(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lload_3(int)
	 */
	@Override
	public void _lload_3(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lload(int, int)
	 */
	@Override
	public void _lload(int pc, int index) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lmul(int)
	 */
	@Override
	public void _lmul(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lneg(int)
	 */
	@Override
	public void _lneg(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lookupswitch(int, int, int, int[][])
	 */
	@Override
	public void _lookupswitch(
		int pc,
		int defaultoffset,
		int npairs,
		int[][] offset_pairs) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lor(int)
	 */
	@Override
	public void _lor(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lrem(int)
	 */
	@Override
	public void _lrem(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lreturn(int)
	 */
	@Override
	public void _lreturn(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lshl(int)
	 */
	@Override
	public void _lshl(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lshr(int)
	 */
	@Override
	public void _lshr(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lstore_0(int)
	 */
	@Override
	public void _lstore_0(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lstore_1(int)
	 */
	@Override
	public void _lstore_1(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lstore_2(int)
	 */
	@Override
	public void _lstore_2(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lstore_3(int)
	 */
	@Override
	public void _lstore_3(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lstore(int, int)
	 */
	@Override
	public void _lstore(int pc, int index) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lsub(int)
	 */
	@Override
	public void _lsub(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lushr(int)
	 */
	@Override
	public void _lushr(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_lxor(int)
	 */
	@Override
	public void _lxor(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_monitorenter(int)
	 */
	@Override
	public void _monitorenter(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_monitorexit(int)
	 */
	@Override
	public void _monitorexit(int pc) {
		// default behavior is to do nothing
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
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_new(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _new(int pc, int index, IConstantPoolEntry constantClass) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_newarray(int, int)
	 */
	@Override
	public void _newarray(int pc, int atype) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_nop(int)
	 */
	@Override
	public void _nop(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_pop(int)
	 */
	@Override
	public void _pop(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_pop2(int)
	 */
	@Override
	public void _pop2(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_putfield(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _putfield(int pc, int index, IConstantPoolEntry constantFieldref) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_putstatic(int, int, IConstantPoolEntry)
	 */
	@Override
	public void _putstatic(
		int pc,
		int index,
		IConstantPoolEntry constantFieldref) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_ret(int, int)
	 */
	@Override
	public void _ret(int pc, int index) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_return(int)
	 */
	@Override
	public void _return(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_saload(int)
	 */
	@Override
	public void _saload(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_sastore(int)
	 */
	@Override
	public void _sastore(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_sipush(int, short)
	 */
	@Override
	public void _sipush(int pc, short value) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_swap(int)
	 */
	@Override
	public void _swap(int pc) {
		// default behavior is to do nothing
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
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_wide(int, int, int, int)
	 */
	@Override
	public void _wide(int pc, int iincopcode, int index, int _const) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_wide(int, int, int)
	 */
	@Override
	public void _wide(int pc, int opcode, int index) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_breakpoint(int)
	 */
	@Override
	public void _breakpoint(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_impdep1(int)
	 */
	@Override
	public void _impdep1(int pc) {
		// default behavior is to do nothing
	}

	/**
	 * @see IBytecodeVisitor#_impdep2(int)
	 */
	@Override
	public void _impdep2(int pc) {
		// default behavior is to do nothing
	}

}
