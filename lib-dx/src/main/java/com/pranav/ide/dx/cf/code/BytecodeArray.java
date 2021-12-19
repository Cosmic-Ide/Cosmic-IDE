/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pranav.ide.dx.cf.code;

import com.pranav.ide.dx.rop.cst.Constant;
import com.pranav.ide.dx.rop.cst.ConstantPool;
import com.pranav.ide.dx.rop.cst.CstDouble;
import com.pranav.ide.dx.rop.cst.CstFloat;
import com.pranav.ide.dx.rop.cst.CstInteger;
import com.pranav.ide.dx.rop.cst.CstInvokeDynamic;
import com.pranav.ide.dx.rop.cst.CstKnownNull;
import com.pranav.ide.dx.rop.cst.CstLiteralBits;
import com.pranav.ide.dx.rop.cst.CstLong;
import com.pranav.ide.dx.rop.cst.CstType;
import com.pranav.ide.dx.rop.type.Type;
import com.pranav.ide.dx.util.Bits;
import com.pranav.ide.dx.util.ByteArray;
import com.pranav.ide.dx.util.Hex;

import java.util.ArrayList;

/**
 * Bytecode array, which is part of a standard {@code Code} attribute.
 */
public final class BytecodeArray {
    /**
     * convenient no-op implementation of {@link Visitor}
     */
    public static final Visitor EMPTY_VISITOR = new BaseVisitor();

    /**
     * {@code non-null;} underlying bytes
     */
    private final com.pranav.ide.dx.util.ByteArray bytes;

    /**
     * {@code non-null;} constant pool to use when resolving constant
     * pool indices
     */
    private final com.pranav.ide.dx.rop.cst.ConstantPool pool;

    /**
     * Constructs an instance.
     *
     * @param bytes {@code non-null;} underlying bytes
     * @param pool  {@code non-null;} constant pool to use when
     *              resolving constant pool indices
     */
    public BytecodeArray(com.pranav.ide.dx.util.ByteArray bytes, ConstantPool pool) {
        if (bytes == null) {
            throw new NullPointerException("bytes == null");
        }

        if (pool == null) {
            throw new NullPointerException("pool == null");
        }

        this.bytes = bytes;
        this.pool = pool;
    }

    /**
     * Gets the underlying byte array.
     *
     * @return {@code non-null;} the byte array
     */
    public ByteArray getBytes() {
        return bytes;
    }

    /**
     * Gets the size of the bytecode array, per se.
     *
     * @return {@code >= 0;} the length of the bytecode array
     */
    public int size() {
        return bytes.size();
    }

    /**
     * Gets the total length of this structure in bytes, when included in
     * a {@code Code} attribute. The returned value includes the
     * array size plus four bytes for {@code code_length}.
     *
     * @return {@code >= 4;} the total length, in bytes
     */
    public int byteLength() {
        return 4 + bytes.size();
    }

    /**
     * Parses each instruction in the array, in order.
     *
     * @param visitor {@code null-ok;} visitor to call back to for
     *                each instruction
     */
    public void forEach(Visitor visitor) {
        int sz = bytes.size();
        int at = 0;

        while (at < sz) {
            /*
             * Don't record the previous offset here, so that we get to see the
             * raw code that initializes the array
             */
            at += parseInstruction(at, visitor);
        }
    }

    /**
     * Finds the offset to each instruction in the bytecode array. The
     * result is a bit set with the offset of each opcode-per-se flipped on.
     *
     * @return {@code non-null;} appropriately constructed bit set
     * @see com.pranav.ide.dx.util.Bits
     */
    public int[] getInstructionOffsets() {
        int sz = bytes.size();
        int[] result = com.pranav.ide.dx.util.Bits.makeBitSet(sz);
        int at = 0;

        while (at < sz) {
            com.pranav.ide.dx.util.Bits.set(result, at, true);
            int length = parseInstruction(at, null);
            at += length;
        }

        return result;
    }

    /**
     * Processes the given "work set" by repeatedly finding the lowest bit
     * in the set, clearing it, and parsing and visiting the instruction at
     * the indicated offset (that is, the bit index), repeating until the
     * work set is empty. It is expected that the visitor will regularly
     * set new bits in the work set during the process.
     *
     * @param workSet {@code non-null;} the work set to process
     * @param visitor {@code non-null;} visitor to call back to for
     *                each instruction
     */
    public void processWorkSet(int[] workSet, Visitor visitor) {
        if (visitor == null) {
            throw new NullPointerException("visitor == null");
        }

        for (; ; ) {
            int offset = com.pranav.ide.dx.util.Bits.findFirst(workSet, 0);
            if (offset < 0) {
                break;
            }
            Bits.clear(workSet, offset);
            parseInstruction(offset, visitor);
            visitor.setPreviousOffset(offset);
        }
    }

    /**
     * Parses the instruction at the indicated offset. Indicate the
     * result by calling the visitor if supplied and by returning the
     * number of bytes consumed by the instruction.
     *
     * <p>In order to simplify further processing, the opcodes passed
     * to the visitor are canonicalized, altering the opcode to a more
     * universal one and making formerly implicit arguments
     * explicit. In particular:</p>
     *
     * <ul>
     * <li>The opcodes to push literal constants of primitive types all become
     *   {@code ldc}.
     *   E.g., {@code fconst_0}, {@code sipush}, and
     *   {@code lconst_0} qualify for this treatment.</li>
     * <li>{@code aconst_null} becomes {@code ldc} of a
     *   "known null."</li>
     * <li>Shorthand local variable accessors become the corresponding
     *   longhand. E.g. {@code aload_2} becomes {@code aload}.</li>
     * <li>{@code goto_w} and {@code jsr_w} become {@code goto}
     *   and {@code jsr} (respectively).</li>
     * <li>{@code ldc_w} becomes {@code ldc}.</li>
     * <li>{@code tableswitch} becomes {@code lookupswitch}.
     * <li>Arithmetic, array, and value-returning ops are collapsed
     *   to the {@code int} variant opcode, with the {@code type}
     *   argument set to indicate the actual type. E.g.,
     *   {@code fadd} becomes {@code iadd}, but
     *   {@code type} is passed as {@code Type.FLOAT} in that
     *   case. Similarly, {@code areturn} becomes
     *   {@code ireturn}. (However, {@code return} remains
     *   unchanged.</li>
     * <li>Local variable access ops are collapsed to the {@code int}
     *   variant opcode, with the {@code type} argument set to indicate
     *   the actual type. E.g., {@code aload} becomes {@code iload},
     *   but {@code type} is passed as {@code Type.OBJECT} in
     *   that case.</li>
     * <li>Numeric conversion ops ({@code i2l}, etc.) are left alone
     *   to avoid too much confustion, but their {@code type} is
     *   the pushed type. E.g., {@code i2b} gets type
     *   {@code Type.INT}, and {@code f2d} gets type
     *   {@code Type.DOUBLE}. Other unaltered opcodes also get
     *   their pushed type. E.g., {@code arraylength} gets type
     *   {@code Type.INT}.</li>
     * </ul>
     *
     * @param offset  {@code >= 0, < bytes.size();} offset to the start of the
     *                instruction
     * @param visitor {@code null-ok;} visitor to call back to
     * @return the length of the instruction, in bytes
     */
    public int parseInstruction(int offset, Visitor visitor) {
        if (visitor == null) {
            visitor = EMPTY_VISITOR;
        }

        try {
            int opcode = bytes.getUnsignedByte(offset);
            int info = com.pranav.ide.dx.cf.code.ByteOps.opInfo(opcode);
            int fmt = info & com.pranav.ide.dx.cf.code.ByteOps.FMT_MASK;

            switch (opcode) {
                case com.pranav.ide.dx.cf.code.ByteOps.NOP: {
                    visitor.visitNoArgs(opcode, offset, 1, com.pranav.ide.dx.rop.type.Type.VOID);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.ACONST_NULL: {
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC, offset, 1,
                            CstKnownNull.THE_ONE, 0);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.ICONST_M1: {
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC, offset, 1,
                            com.pranav.ide.dx.rop.cst.CstInteger.VALUE_M1, -1);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.ICONST_0: {
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC, offset, 1,
                            com.pranav.ide.dx.rop.cst.CstInteger.VALUE_0, 0);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.ICONST_1: {
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC, offset, 1,
                            com.pranav.ide.dx.rop.cst.CstInteger.VALUE_1, 1);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.ICONST_2: {
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC, offset, 1,
                            com.pranav.ide.dx.rop.cst.CstInteger.VALUE_2, 2);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.ICONST_3: {
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC, offset, 1,
                            com.pranav.ide.dx.rop.cst.CstInteger.VALUE_3, 3);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.ICONST_4: {
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC, offset, 1,
                            com.pranav.ide.dx.rop.cst.CstInteger.VALUE_4, 4);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.ICONST_5: {
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC, offset, 1,
                            com.pranav.ide.dx.rop.cst.CstInteger.VALUE_5, 5);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.LCONST_0: {
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC, offset, 1,
                            com.pranav.ide.dx.rop.cst.CstLong.VALUE_0, 0);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.LCONST_1: {
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC, offset, 1,
                            CstLong.VALUE_1, 0);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.FCONST_0: {
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC, offset, 1,
                            com.pranav.ide.dx.rop.cst.CstFloat.VALUE_0, 0);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.FCONST_1: {
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC, offset, 1,
                            com.pranav.ide.dx.rop.cst.CstFloat.VALUE_1, 0);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.FCONST_2: {
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC, offset, 1,
                            CstFloat.VALUE_2, 0);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.DCONST_0: {
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC, offset, 1,
                            com.pranav.ide.dx.rop.cst.CstDouble.VALUE_0, 0);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.DCONST_1: {
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC, offset, 1,
                            CstDouble.VALUE_1, 0);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.BIPUSH: {
                    int value = bytes.getByte(offset + 1);
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC, offset, 2,
                            com.pranav.ide.dx.rop.cst.CstInteger.make(value), value);
                    return 2;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.SIPUSH: {
                    int value = bytes.getShort(offset + 1);
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC, offset, 3,
                            com.pranav.ide.dx.rop.cst.CstInteger.make(value), value);
                    return 3;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.LDC: {
                    int idx = bytes.getUnsignedByte(offset + 1);
                    com.pranav.ide.dx.rop.cst.Constant cst = pool.get(idx);
                    int value = (cst instanceof com.pranav.ide.dx.rop.cst.CstInteger) ?
                            ((com.pranav.ide.dx.rop.cst.CstInteger) cst).getValue() : 0;
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC, offset, 2, cst, value);
                    return 2;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.LDC_W: {
                    int idx = bytes.getUnsignedShort(offset + 1);
                    com.pranav.ide.dx.rop.cst.Constant cst = pool.get(idx);
                    int value = (cst instanceof com.pranav.ide.dx.rop.cst.CstInteger) ?
                            ((com.pranav.ide.dx.rop.cst.CstInteger) cst).getValue() : 0;
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC, offset, 3, cst, value);
                    return 3;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.LDC2_W: {
                    int idx = bytes.getUnsignedShort(offset + 1);
                    com.pranav.ide.dx.rop.cst.Constant cst = pool.get(idx);
                    visitor.visitConstant(com.pranav.ide.dx.cf.code.ByteOps.LDC2_W, offset, 3, cst, 0);
                    return 3;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.ILOAD: {
                    int idx = bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ILOAD, offset, 2, idx,
                            com.pranav.ide.dx.rop.type.Type.INT, 0);
                    return 2;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.LLOAD: {
                    int idx = bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ILOAD, offset, 2, idx,
                            com.pranav.ide.dx.rop.type.Type.LONG, 0);
                    return 2;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.FLOAD: {
                    int idx = bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ILOAD, offset, 2, idx,
                            com.pranav.ide.dx.rop.type.Type.FLOAT, 0);
                    return 2;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.DLOAD: {
                    int idx = bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ILOAD, offset, 2, idx,
                            com.pranav.ide.dx.rop.type.Type.DOUBLE, 0);
                    return 2;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.ALOAD: {
                    int idx = bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ILOAD, offset, 2, idx,
                            com.pranav.ide.dx.rop.type.Type.OBJECT, 0);
                    return 2;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.ILOAD_0:
                case com.pranav.ide.dx.cf.code.ByteOps.ILOAD_1:
                case com.pranav.ide.dx.cf.code.ByteOps.ILOAD_2:
                case com.pranav.ide.dx.cf.code.ByteOps.ILOAD_3: {
                    int idx = opcode - com.pranav.ide.dx.cf.code.ByteOps.ILOAD_0;
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ILOAD, offset, 1, idx,
                            com.pranav.ide.dx.rop.type.Type.INT, 0);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.LLOAD_0:
                case com.pranav.ide.dx.cf.code.ByteOps.LLOAD_1:
                case com.pranav.ide.dx.cf.code.ByteOps.LLOAD_2:
                case com.pranav.ide.dx.cf.code.ByteOps.LLOAD_3: {
                    int idx = opcode - com.pranav.ide.dx.cf.code.ByteOps.LLOAD_0;
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ILOAD, offset, 1, idx,
                            com.pranav.ide.dx.rop.type.Type.LONG, 0);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.FLOAD_0:
                case com.pranav.ide.dx.cf.code.ByteOps.FLOAD_1:
                case com.pranav.ide.dx.cf.code.ByteOps.FLOAD_2:
                case com.pranav.ide.dx.cf.code.ByteOps.FLOAD_3: {
                    int idx = opcode - com.pranav.ide.dx.cf.code.ByteOps.FLOAD_0;
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ILOAD, offset, 1, idx,
                            com.pranav.ide.dx.rop.type.Type.FLOAT, 0);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.DLOAD_0:
                case com.pranav.ide.dx.cf.code.ByteOps.DLOAD_1:
                case com.pranav.ide.dx.cf.code.ByteOps.DLOAD_2:
                case com.pranav.ide.dx.cf.code.ByteOps.DLOAD_3: {
                    int idx = opcode - com.pranav.ide.dx.cf.code.ByteOps.DLOAD_0;
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ILOAD, offset, 1, idx,
                            com.pranav.ide.dx.rop.type.Type.DOUBLE, 0);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.ALOAD_0:
                case com.pranav.ide.dx.cf.code.ByteOps.ALOAD_1:
                case com.pranav.ide.dx.cf.code.ByteOps.ALOAD_2:
                case com.pranav.ide.dx.cf.code.ByteOps.ALOAD_3: {
                    int idx = opcode - com.pranav.ide.dx.cf.code.ByteOps.ALOAD_0;
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ILOAD, offset, 1, idx,
                            com.pranav.ide.dx.rop.type.Type.OBJECT, 0);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.IALOAD: {
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IALOAD, offset, 1, com.pranav.ide.dx.rop.type.Type.INT);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.LALOAD: {
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IALOAD, offset, 1, com.pranav.ide.dx.rop.type.Type.LONG);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.FALOAD: {
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IALOAD, offset, 1,
                            com.pranav.ide.dx.rop.type.Type.FLOAT);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.DALOAD: {
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IALOAD, offset, 1,
                            com.pranav.ide.dx.rop.type.Type.DOUBLE);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.AALOAD: {
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IALOAD, offset, 1,
                            com.pranav.ide.dx.rop.type.Type.OBJECT);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.BALOAD: {
                    /*
                     * Note: This is a load from either a byte[] or a
                     * boolean[].
                     */
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IALOAD, offset, 1, com.pranav.ide.dx.rop.type.Type.BYTE);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.CALOAD: {
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IALOAD, offset, 1, com.pranav.ide.dx.rop.type.Type.CHAR);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.SALOAD: {
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IALOAD, offset, 1,
                            com.pranav.ide.dx.rop.type.Type.SHORT);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.ISTORE: {
                    int idx = bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ISTORE, offset, 2, idx,
                            com.pranav.ide.dx.rop.type.Type.INT, 0);
                    return 2;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.LSTORE: {
                    int idx = bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ISTORE, offset, 2, idx,
                            com.pranav.ide.dx.rop.type.Type.LONG, 0);
                    return 2;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.FSTORE: {
                    int idx = bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ISTORE, offset, 2, idx,
                            com.pranav.ide.dx.rop.type.Type.FLOAT, 0);
                    return 2;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.DSTORE: {
                    int idx = bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ISTORE, offset, 2, idx,
                            com.pranav.ide.dx.rop.type.Type.DOUBLE, 0);
                    return 2;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.ASTORE: {
                    int idx = bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ISTORE, offset, 2, idx,
                            com.pranav.ide.dx.rop.type.Type.OBJECT, 0);
                    return 2;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.ISTORE_0:
                case com.pranav.ide.dx.cf.code.ByteOps.ISTORE_1:
                case com.pranav.ide.dx.cf.code.ByteOps.ISTORE_2:
                case com.pranav.ide.dx.cf.code.ByteOps.ISTORE_3: {
                    int idx = opcode - com.pranav.ide.dx.cf.code.ByteOps.ISTORE_0;
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ISTORE, offset, 1, idx,
                            com.pranav.ide.dx.rop.type.Type.INT, 0);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.LSTORE_0:
                case com.pranav.ide.dx.cf.code.ByteOps.LSTORE_1:
                case com.pranav.ide.dx.cf.code.ByteOps.LSTORE_2:
                case com.pranav.ide.dx.cf.code.ByteOps.LSTORE_3: {
                    int idx = opcode - com.pranav.ide.dx.cf.code.ByteOps.LSTORE_0;
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ISTORE, offset, 1, idx,
                            com.pranav.ide.dx.rop.type.Type.LONG, 0);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.FSTORE_0:
                case com.pranav.ide.dx.cf.code.ByteOps.FSTORE_1:
                case com.pranav.ide.dx.cf.code.ByteOps.FSTORE_2:
                case com.pranav.ide.dx.cf.code.ByteOps.FSTORE_3: {
                    int idx = opcode - com.pranav.ide.dx.cf.code.ByteOps.FSTORE_0;
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ISTORE, offset, 1, idx,
                            com.pranav.ide.dx.rop.type.Type.FLOAT, 0);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.DSTORE_0:
                case com.pranav.ide.dx.cf.code.ByteOps.DSTORE_1:
                case com.pranav.ide.dx.cf.code.ByteOps.DSTORE_2:
                case com.pranav.ide.dx.cf.code.ByteOps.DSTORE_3: {
                    int idx = opcode - com.pranav.ide.dx.cf.code.ByteOps.DSTORE_0;
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ISTORE, offset, 1, idx,
                            com.pranav.ide.dx.rop.type.Type.DOUBLE, 0);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.ASTORE_0:
                case com.pranav.ide.dx.cf.code.ByteOps.ASTORE_1:
                case com.pranav.ide.dx.cf.code.ByteOps.ASTORE_2:
                case com.pranav.ide.dx.cf.code.ByteOps.ASTORE_3: {
                    int idx = opcode - com.pranav.ide.dx.cf.code.ByteOps.ASTORE_0;
                    visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ISTORE, offset, 1, idx,
                            com.pranav.ide.dx.rop.type.Type.OBJECT, 0);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.IASTORE: {
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IASTORE, offset, 1, com.pranav.ide.dx.rop.type.Type.INT);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.LASTORE: {
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IASTORE, offset, 1,
                            com.pranav.ide.dx.rop.type.Type.LONG);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.FASTORE: {
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IASTORE, offset, 1,
                            com.pranav.ide.dx.rop.type.Type.FLOAT);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.DASTORE: {
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IASTORE, offset, 1,
                            com.pranav.ide.dx.rop.type.Type.DOUBLE);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.AASTORE: {
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IASTORE, offset, 1,
                            com.pranav.ide.dx.rop.type.Type.OBJECT);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.BASTORE: {
                    /*
                     * Note: This is a load from either a byte[] or a
                     * boolean[].
                     */
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IASTORE, offset, 1,
                            com.pranav.ide.dx.rop.type.Type.BYTE);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.CASTORE: {
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IASTORE, offset, 1,
                            com.pranav.ide.dx.rop.type.Type.CHAR);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.SASTORE: {
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IASTORE, offset, 1,
                            com.pranav.ide.dx.rop.type.Type.SHORT);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.POP:
                case com.pranav.ide.dx.cf.code.ByteOps.POP2:
                case com.pranav.ide.dx.cf.code.ByteOps.DUP:
                case com.pranav.ide.dx.cf.code.ByteOps.DUP_X1:
                case com.pranav.ide.dx.cf.code.ByteOps.DUP_X2:
                case com.pranav.ide.dx.cf.code.ByteOps.DUP2:
                case com.pranav.ide.dx.cf.code.ByteOps.DUP2_X1:
                case com.pranav.ide.dx.cf.code.ByteOps.DUP2_X2:
                case com.pranav.ide.dx.cf.code.ByteOps.SWAP: {
                    visitor.visitNoArgs(opcode, offset, 1, com.pranav.ide.dx.rop.type.Type.VOID);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.IADD:
                case com.pranav.ide.dx.cf.code.ByteOps.ISUB:
                case com.pranav.ide.dx.cf.code.ByteOps.IMUL:
                case com.pranav.ide.dx.cf.code.ByteOps.IDIV:
                case com.pranav.ide.dx.cf.code.ByteOps.IREM:
                case com.pranav.ide.dx.cf.code.ByteOps.INEG:
                case com.pranav.ide.dx.cf.code.ByteOps.ISHL:
                case com.pranav.ide.dx.cf.code.ByteOps.ISHR:
                case com.pranav.ide.dx.cf.code.ByteOps.IUSHR:
                case com.pranav.ide.dx.cf.code.ByteOps.IAND:
                case com.pranav.ide.dx.cf.code.ByteOps.IOR:
                case com.pranav.ide.dx.cf.code.ByteOps.IXOR: {
                    visitor.visitNoArgs(opcode, offset, 1, com.pranav.ide.dx.rop.type.Type.INT);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.LADD:
                case com.pranav.ide.dx.cf.code.ByteOps.LSUB:
                case com.pranav.ide.dx.cf.code.ByteOps.LMUL:
                case com.pranav.ide.dx.cf.code.ByteOps.LDIV:
                case com.pranav.ide.dx.cf.code.ByteOps.LREM:
                case com.pranav.ide.dx.cf.code.ByteOps.LNEG:
                case com.pranav.ide.dx.cf.code.ByteOps.LSHL:
                case com.pranav.ide.dx.cf.code.ByteOps.LSHR:
                case com.pranav.ide.dx.cf.code.ByteOps.LUSHR:
                case com.pranav.ide.dx.cf.code.ByteOps.LAND:
                case com.pranav.ide.dx.cf.code.ByteOps.LOR:
                case com.pranav.ide.dx.cf.code.ByteOps.LXOR: {
                    /*
                     * It's "opcode - 1" because, conveniently enough, all
                     * these long ops are one past the int variants.
                     */
                    visitor.visitNoArgs(opcode - 1, offset, 1, com.pranav.ide.dx.rop.type.Type.LONG);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.FADD:
                case com.pranav.ide.dx.cf.code.ByteOps.FSUB:
                case com.pranav.ide.dx.cf.code.ByteOps.FMUL:
                case com.pranav.ide.dx.cf.code.ByteOps.FDIV:
                case com.pranav.ide.dx.cf.code.ByteOps.FREM:
                case com.pranav.ide.dx.cf.code.ByteOps.FNEG: {
                    /*
                     * It's "opcode - 2" because, conveniently enough, all
                     * these float ops are two past the int variants.
                     */
                    visitor.visitNoArgs(opcode - 2, offset, 1, com.pranav.ide.dx.rop.type.Type.FLOAT);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.DADD:
                case com.pranav.ide.dx.cf.code.ByteOps.DSUB:
                case com.pranav.ide.dx.cf.code.ByteOps.DMUL:
                case com.pranav.ide.dx.cf.code.ByteOps.DDIV:
                case com.pranav.ide.dx.cf.code.ByteOps.DREM:
                case com.pranav.ide.dx.cf.code.ByteOps.DNEG: {
                    /*
                     * It's "opcode - 3" because, conveniently enough, all
                     * these double ops are three past the int variants.
                     */
                    visitor.visitNoArgs(opcode - 3, offset, 1, com.pranav.ide.dx.rop.type.Type.DOUBLE);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.IINC: {
                    int idx = bytes.getUnsignedByte(offset + 1);
                    int value = bytes.getByte(offset + 2);
                    visitor.visitLocal(opcode, offset, 3, idx,
                            com.pranav.ide.dx.rop.type.Type.INT, value);
                    return 3;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.I2L:
                case com.pranav.ide.dx.cf.code.ByteOps.F2L:
                case com.pranav.ide.dx.cf.code.ByteOps.D2L: {
                    visitor.visitNoArgs(opcode, offset, 1, com.pranav.ide.dx.rop.type.Type.LONG);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.I2F:
                case com.pranav.ide.dx.cf.code.ByteOps.L2F:
                case com.pranav.ide.dx.cf.code.ByteOps.D2F: {
                    visitor.visitNoArgs(opcode, offset, 1, com.pranav.ide.dx.rop.type.Type.FLOAT);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.I2D:
                case com.pranav.ide.dx.cf.code.ByteOps.L2D:
                case com.pranav.ide.dx.cf.code.ByteOps.F2D: {
                    visitor.visitNoArgs(opcode, offset, 1, com.pranav.ide.dx.rop.type.Type.DOUBLE);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.L2I:
                case com.pranav.ide.dx.cf.code.ByteOps.F2I:
                case com.pranav.ide.dx.cf.code.ByteOps.D2I:
                case com.pranav.ide.dx.cf.code.ByteOps.I2B:
                case com.pranav.ide.dx.cf.code.ByteOps.I2C:
                case com.pranav.ide.dx.cf.code.ByteOps.I2S:
                case com.pranav.ide.dx.cf.code.ByteOps.LCMP:
                case com.pranav.ide.dx.cf.code.ByteOps.FCMPL:
                case com.pranav.ide.dx.cf.code.ByteOps.FCMPG:
                case com.pranav.ide.dx.cf.code.ByteOps.DCMPL:
                case com.pranav.ide.dx.cf.code.ByteOps.DCMPG:
                case com.pranav.ide.dx.cf.code.ByteOps.ARRAYLENGTH: {
                    visitor.visitNoArgs(opcode, offset, 1, com.pranav.ide.dx.rop.type.Type.INT);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.IFEQ:
                case com.pranav.ide.dx.cf.code.ByteOps.IFNE:
                case com.pranav.ide.dx.cf.code.ByteOps.IFLT:
                case com.pranav.ide.dx.cf.code.ByteOps.IFGE:
                case com.pranav.ide.dx.cf.code.ByteOps.IFGT:
                case com.pranav.ide.dx.cf.code.ByteOps.IFLE:
                case com.pranav.ide.dx.cf.code.ByteOps.IF_ICMPEQ:
                case com.pranav.ide.dx.cf.code.ByteOps.IF_ICMPNE:
                case com.pranav.ide.dx.cf.code.ByteOps.IF_ICMPLT:
                case com.pranav.ide.dx.cf.code.ByteOps.IF_ICMPGE:
                case com.pranav.ide.dx.cf.code.ByteOps.IF_ICMPGT:
                case com.pranav.ide.dx.cf.code.ByteOps.IF_ICMPLE:
                case com.pranav.ide.dx.cf.code.ByteOps.IF_ACMPEQ:
                case com.pranav.ide.dx.cf.code.ByteOps.IF_ACMPNE:
                case com.pranav.ide.dx.cf.code.ByteOps.GOTO:
                case com.pranav.ide.dx.cf.code.ByteOps.JSR:
                case com.pranav.ide.dx.cf.code.ByteOps.IFNULL:
                case com.pranav.ide.dx.cf.code.ByteOps.IFNONNULL: {
                    int target = offset + bytes.getShort(offset + 1);
                    visitor.visitBranch(opcode, offset, 3, target);
                    return 3;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.RET: {
                    int idx = bytes.getUnsignedByte(offset + 1);
                    visitor.visitLocal(opcode, offset, 2, idx,
                            com.pranav.ide.dx.rop.type.Type.RETURN_ADDRESS, 0);
                    return 2;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.TABLESWITCH: {
                    return parseTableswitch(offset, visitor);
                }
                case com.pranav.ide.dx.cf.code.ByteOps.LOOKUPSWITCH: {
                    return parseLookupswitch(offset, visitor);
                }
                case com.pranav.ide.dx.cf.code.ByteOps.IRETURN: {
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IRETURN, offset, 1, com.pranav.ide.dx.rop.type.Type.INT);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.LRETURN: {
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IRETURN, offset, 1,
                            com.pranav.ide.dx.rop.type.Type.LONG);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.FRETURN: {
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IRETURN, offset, 1,
                            com.pranav.ide.dx.rop.type.Type.FLOAT);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.DRETURN: {
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IRETURN, offset, 1,
                            com.pranav.ide.dx.rop.type.Type.DOUBLE);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.ARETURN: {
                    visitor.visitNoArgs(com.pranav.ide.dx.cf.code.ByteOps.IRETURN, offset, 1,
                            com.pranav.ide.dx.rop.type.Type.OBJECT);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.RETURN:
                case com.pranav.ide.dx.cf.code.ByteOps.ATHROW:
                case com.pranav.ide.dx.cf.code.ByteOps.MONITORENTER:
                case com.pranav.ide.dx.cf.code.ByteOps.MONITOREXIT: {
                    visitor.visitNoArgs(opcode, offset, 1, com.pranav.ide.dx.rop.type.Type.VOID);
                    return 1;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.GETSTATIC:
                case com.pranav.ide.dx.cf.code.ByteOps.PUTSTATIC:
                case com.pranav.ide.dx.cf.code.ByteOps.GETFIELD:
                case com.pranav.ide.dx.cf.code.ByteOps.PUTFIELD:
                case com.pranav.ide.dx.cf.code.ByteOps.INVOKEVIRTUAL:
                case com.pranav.ide.dx.cf.code.ByteOps.INVOKESPECIAL:
                case com.pranav.ide.dx.cf.code.ByteOps.INVOKESTATIC:
                case com.pranav.ide.dx.cf.code.ByteOps.NEW:
                case com.pranav.ide.dx.cf.code.ByteOps.ANEWARRAY:
                case com.pranav.ide.dx.cf.code.ByteOps.CHECKCAST:
                case com.pranav.ide.dx.cf.code.ByteOps.INSTANCEOF: {
                    int idx = bytes.getUnsignedShort(offset + 1);
                    com.pranav.ide.dx.rop.cst.Constant cst = pool.get(idx);
                    visitor.visitConstant(opcode, offset, 3, cst, 0);
                    return 3;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.INVOKEINTERFACE: {
                    int idx = bytes.getUnsignedShort(offset + 1);
                    int count = bytes.getUnsignedByte(offset + 3);
                    int expectZero = bytes.getUnsignedByte(offset + 4);
                    com.pranav.ide.dx.rop.cst.Constant cst = pool.get(idx);
                    visitor.visitConstant(opcode, offset, 5, cst,
                            count | (expectZero << 8));
                    return 5;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.INVOKEDYNAMIC: {
                    int idx = bytes.getUnsignedShort(offset + 1);
                    // Skip to must-be-zero bytes at offsets 3 and 4
                    com.pranav.ide.dx.rop.cst.CstInvokeDynamic cstInvokeDynamic = (CstInvokeDynamic) pool.get(idx);
                    visitor.visitConstant(opcode, offset, 5, cstInvokeDynamic, 0);
                    return 5;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.NEWARRAY: {
                    return parseNewarray(offset, visitor);
                }
                case com.pranav.ide.dx.cf.code.ByteOps.WIDE: {
                    return parseWide(offset, visitor);
                }
                case com.pranav.ide.dx.cf.code.ByteOps.MULTIANEWARRAY: {
                    int idx = bytes.getUnsignedShort(offset + 1);
                    int dimensions = bytes.getUnsignedByte(offset + 3);
                    com.pranav.ide.dx.rop.cst.Constant cst = pool.get(idx);
                    visitor.visitConstant(opcode, offset, 4, cst, dimensions);
                    return 4;
                }
                case com.pranav.ide.dx.cf.code.ByteOps.GOTO_W:
                case com.pranav.ide.dx.cf.code.ByteOps.JSR_W: {
                    int target = offset + bytes.getInt(offset + 1);
                    int newop =
                            (opcode == com.pranav.ide.dx.cf.code.ByteOps.GOTO_W) ? com.pranav.ide.dx.cf.code.ByteOps.GOTO :
                                    com.pranav.ide.dx.cf.code.ByteOps.JSR;
                    visitor.visitBranch(newop, offset, 5, target);
                    return 5;
                }
                default: {
                    visitor.visitInvalid(opcode, offset, 1);
                    return 1;
                }
            }
        } catch (SimException ex) {
            ex.addContext("...at bytecode offset " + com.pranav.ide.dx.util.Hex.u4(offset));
            throw ex;
        } catch (RuntimeException ex) {
            SimException se = new SimException(ex);
            se.addContext("...at bytecode offset " + com.pranav.ide.dx.util.Hex.u4(offset));
            throw se;
        }
    }

    /**
     * Helper to deal with {@code tableswitch}.
     *
     * @param offset  the offset to the {@code tableswitch} opcode itself
     * @param visitor {@code non-null;} visitor to use
     * @return instruction length, in bytes
     */
    private int parseTableswitch(int offset, Visitor visitor) {
        int at = (offset + 4) & ~3; // "at" skips the padding.

        // Collect the padding.
        int padding = 0;
        for (int i = offset + 1; i < at; i++) {
            padding = (padding << 8) | bytes.getUnsignedByte(i);
        }

        int defaultTarget = offset + bytes.getInt(at);
        int low = bytes.getInt(at + 4);
        int high = bytes.getInt(at + 8);
        int count = high - low + 1;
        at += 12;

        if (low > high) {
            throw new SimException("low / high inversion");
        }

        com.pranav.ide.dx.cf.code.SwitchList cases = new com.pranav.ide.dx.cf.code.SwitchList(count);
        for (int i = 0; i < count; i++) {
            int target = offset + bytes.getInt(at);
            at += 4;
            cases.add(low + i, target);
        }
        cases.setDefaultTarget(defaultTarget);
        cases.removeSuperfluousDefaults();
        cases.setImmutable();

        int length = at - offset;
        visitor.visitSwitch(com.pranav.ide.dx.cf.code.ByteOps.LOOKUPSWITCH, offset, length, cases,
                padding);

        return length;
    }

    /**
     * Helper to deal with {@code lookupswitch}.
     *
     * @param offset  the offset to the {@code lookupswitch} opcode itself
     * @param visitor {@code non-null;} visitor to use
     * @return instruction length, in bytes
     */
    private int parseLookupswitch(int offset, Visitor visitor) {
        int at = (offset + 4) & ~3; // "at" skips the padding.

        // Collect the padding.
        int padding = 0;
        for (int i = offset + 1; i < at; i++) {
            padding = (padding << 8) | bytes.getUnsignedByte(i);
        }

        int defaultTarget = offset + bytes.getInt(at);
        int npairs = bytes.getInt(at + 4);
        at += 8;

        com.pranav.ide.dx.cf.code.SwitchList cases = new com.pranav.ide.dx.cf.code.SwitchList(npairs);
        for (int i = 0; i < npairs; i++) {
            int match = bytes.getInt(at);
            int target = offset + bytes.getInt(at + 4);
            at += 8;
            cases.add(match, target);
        }
        cases.setDefaultTarget(defaultTarget);
        cases.removeSuperfluousDefaults();
        cases.setImmutable();

        int length = at - offset;
        visitor.visitSwitch(com.pranav.ide.dx.cf.code.ByteOps.LOOKUPSWITCH, offset, length, cases,
                padding);

        return length;
    }

    /**
     * Helper to deal with {@code newarray}.
     *
     * @param offset  the offset to the {@code newarray} opcode itself
     * @param visitor {@code non-null;} visitor to use
     * @return instruction length, in bytes
     */
    private int parseNewarray(int offset, Visitor visitor) {
        int value = bytes.getUnsignedByte(offset + 1);
        com.pranav.ide.dx.rop.cst.CstType type;
        switch (value) {
            case com.pranav.ide.dx.cf.code.ByteOps.NEWARRAY_BOOLEAN: {
                type = com.pranav.ide.dx.rop.cst.CstType.BOOLEAN_ARRAY;
                break;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.NEWARRAY_CHAR: {
                type = com.pranav.ide.dx.rop.cst.CstType.CHAR_ARRAY;
                break;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.NEWARRAY_DOUBLE: {
                type = com.pranav.ide.dx.rop.cst.CstType.DOUBLE_ARRAY;
                break;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.NEWARRAY_FLOAT: {
                type = com.pranav.ide.dx.rop.cst.CstType.FLOAT_ARRAY;
                break;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.NEWARRAY_BYTE: {
                type = com.pranav.ide.dx.rop.cst.CstType.BYTE_ARRAY;
                break;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.NEWARRAY_SHORT: {
                type = com.pranav.ide.dx.rop.cst.CstType.SHORT_ARRAY;
                break;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.NEWARRAY_INT: {
                type = com.pranav.ide.dx.rop.cst.CstType.INT_ARRAY;
                break;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.NEWARRAY_LONG: {
                type = com.pranav.ide.dx.rop.cst.CstType.LONG_ARRAY;
                break;
            }
            default: {
                throw new SimException("bad newarray code " +
                        Hex.u1(value));
            }
        }

        // Revisit the previous bytecode to find out the length of the array
        int previousOffset = visitor.getPreviousOffset();
        ConstantParserVisitor constantVisitor = new ConstantParserVisitor();
        int arrayLength = 0;

        /*
         * For visitors that don't record the previous offset, -1 will be
         * seen here
         */
        if (previousOffset >= 0) {
            parseInstruction(previousOffset, constantVisitor);
            if (constantVisitor.cst instanceof com.pranav.ide.dx.rop.cst.CstInteger &&
                    constantVisitor.length + previousOffset == offset) {
                arrayLength = constantVisitor.value;

            }
        }

        /*
         * Try to match the array initialization idiom. For example, if the
         * subsequent code is initializing an int array, we are expecting the
         * following pattern repeatedly:
         *  dup
         *  push index
         *  push value
         *  *astore
         *
         * where the index value will be incrimented sequentially from 0 up.
         */
        int nInit = 0;
        int curOffset = offset + 2;
        int lastOffset = curOffset;
        ArrayList<com.pranav.ide.dx.rop.cst.Constant> initVals = new ArrayList<com.pranav.ide.dx.rop.cst.Constant>();

        if (arrayLength != 0) {
            while (true) {
                boolean punt = false;

                // First, check if the next bytecode is dup.
                int nextByte = bytes.getUnsignedByte(curOffset++);
                if (nextByte != com.pranav.ide.dx.cf.code.ByteOps.DUP)
                    break;

                /*
                 * Next, check if the expected array index is pushed to
                 * the stack.
                 */
                parseInstruction(curOffset, constantVisitor);
                if (constantVisitor.length == 0 ||
                        !(constantVisitor.cst instanceof CstInteger) ||
                        constantVisitor.value != nInit)
                    break;

                // Next, fetch the init value and record it.
                curOffset += constantVisitor.length;

                /*
                 * Next, find out what kind of constant is pushed onto
                 * the stack.
                 */
                parseInstruction(curOffset, constantVisitor);
                if (constantVisitor.length == 0 ||
                        !(constantVisitor.cst instanceof CstLiteralBits))
                    break;

                curOffset += constantVisitor.length;
                initVals.add(constantVisitor.cst);

                nextByte = bytes.getUnsignedByte(curOffset++);
                // Now, check if the value is stored to the array properly.
                switch (value) {
                    case com.pranav.ide.dx.cf.code.ByteOps.NEWARRAY_BYTE:
                    case com.pranav.ide.dx.cf.code.ByteOps.NEWARRAY_BOOLEAN: {
                        if (nextByte != com.pranav.ide.dx.cf.code.ByteOps.BASTORE) {
                            punt = true;
                        }
                        break;
                    }
                    case com.pranav.ide.dx.cf.code.ByteOps.NEWARRAY_CHAR: {
                        if (nextByte != com.pranav.ide.dx.cf.code.ByteOps.CASTORE) {
                            punt = true;
                        }
                        break;
                    }
                    case com.pranav.ide.dx.cf.code.ByteOps.NEWARRAY_DOUBLE: {
                        if (nextByte != com.pranav.ide.dx.cf.code.ByteOps.DASTORE) {
                            punt = true;
                        }
                        break;
                    }
                    case com.pranav.ide.dx.cf.code.ByteOps.NEWARRAY_FLOAT: {
                        if (nextByte != com.pranav.ide.dx.cf.code.ByteOps.FASTORE) {
                            punt = true;
                        }
                        break;
                    }
                    case com.pranav.ide.dx.cf.code.ByteOps.NEWARRAY_SHORT: {
                        if (nextByte != com.pranav.ide.dx.cf.code.ByteOps.SASTORE) {
                            punt = true;
                        }
                        break;
                    }
                    case com.pranav.ide.dx.cf.code.ByteOps.NEWARRAY_INT: {
                        if (nextByte != com.pranav.ide.dx.cf.code.ByteOps.IASTORE) {
                            punt = true;
                        }
                        break;
                    }
                    case com.pranav.ide.dx.cf.code.ByteOps.NEWARRAY_LONG: {
                        if (nextByte != com.pranav.ide.dx.cf.code.ByteOps.LASTORE) {
                            punt = true;
                        }
                        break;
                    }
                    default:
                        punt = true;
                        break;
                }
                if (punt) {
                    break;
                }
                lastOffset = curOffset;
                nInit++;
            }
        }

        /*
         * For singleton arrays it is still more economical to
         * generate the aput.
         */
        if (nInit < 2 || nInit != arrayLength) {
            visitor.visitNewarray(offset, 2, type, null);
            return 2;
        } else {
            visitor.visitNewarray(offset, lastOffset - offset, type, initVals);
            return lastOffset - offset;
        }
    }


    /**
     * Helper to deal with {@code wide}.
     *
     * @param offset  the offset to the {@code wide} opcode itself
     * @param visitor {@code non-null;} visitor to use
     * @return instruction length, in bytes
     */
    private int parseWide(int offset, Visitor visitor) {
        int opcode = bytes.getUnsignedByte(offset + 1);
        int idx = bytes.getUnsignedShort(offset + 2);
        switch (opcode) {
            case com.pranav.ide.dx.cf.code.ByteOps.ILOAD: {
                visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ILOAD, offset, 4, idx,
                        com.pranav.ide.dx.rop.type.Type.INT, 0);
                return 4;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.LLOAD: {
                visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ILOAD, offset, 4, idx,
                        com.pranav.ide.dx.rop.type.Type.LONG, 0);
                return 4;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.FLOAD: {
                visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ILOAD, offset, 4, idx,
                        com.pranav.ide.dx.rop.type.Type.FLOAT, 0);
                return 4;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.DLOAD: {
                visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ILOAD, offset, 4, idx,
                        com.pranav.ide.dx.rop.type.Type.DOUBLE, 0);
                return 4;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.ALOAD: {
                visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ILOAD, offset, 4, idx,
                        com.pranav.ide.dx.rop.type.Type.OBJECT, 0);
                return 4;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.ISTORE: {
                visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ISTORE, offset, 4, idx,
                        com.pranav.ide.dx.rop.type.Type.INT, 0);
                return 4;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.LSTORE: {
                visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ISTORE, offset, 4, idx,
                        com.pranav.ide.dx.rop.type.Type.LONG, 0);
                return 4;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.FSTORE: {
                visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ISTORE, offset, 4, idx,
                        com.pranav.ide.dx.rop.type.Type.FLOAT, 0);
                return 4;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.DSTORE: {
                visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ISTORE, offset, 4, idx,
                        com.pranav.ide.dx.rop.type.Type.DOUBLE, 0);
                return 4;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.ASTORE: {
                visitor.visitLocal(com.pranav.ide.dx.cf.code.ByteOps.ISTORE, offset, 4, idx,
                        com.pranav.ide.dx.rop.type.Type.OBJECT, 0);
                return 4;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.RET: {
                visitor.visitLocal(opcode, offset, 4, idx,
                        com.pranav.ide.dx.rop.type.Type.RETURN_ADDRESS, 0);
                return 4;
            }
            case com.pranav.ide.dx.cf.code.ByteOps.IINC: {
                int value = bytes.getShort(offset + 4);
                visitor.visitLocal(opcode, offset, 6, idx,
                        com.pranav.ide.dx.rop.type.Type.INT, value);
                return 6;
            }
            default: {
                visitor.visitInvalid(ByteOps.WIDE, offset, 1);
                return 1;
            }
        }
    }

    /**
     * Instruction visitor interface.
     */
    public interface Visitor {
        /**
         * Visits an invalid instruction.
         *
         * @param opcode the opcode
         * @param offset offset to the instruction
         * @param length length of the instruction, in bytes
         */
        void visitInvalid(int opcode, int offset, int length);

        /**
         * Visits an instruction which has no inline arguments
         * (implicit or explicit).
         *
         * @param opcode the opcode
         * @param offset offset to the instruction
         * @param length length of the instruction, in bytes
         * @param type   {@code non-null;} type the instruction operates on
         */
        void visitNoArgs(int opcode, int offset, int length,
                         com.pranav.ide.dx.rop.type.Type type);

        /**
         * Visits an instruction which has a local variable index argument.
         *
         * @param opcode the opcode
         * @param offset offset to the instruction
         * @param length length of the instruction, in bytes
         * @param idx    the local variable index
         * @param type   {@code non-null;} the type of the accessed value
         * @param value  additional literal integer argument, if salient (i.e.,
         *               for {@code iinc})
         */
        void visitLocal(int opcode, int offset, int length,
                        int idx, com.pranav.ide.dx.rop.type.Type type, int value);

        /**
         * Visits an instruction which has a (possibly synthetic)
         * constant argument, and possibly also an
         * additional literal integer argument. In the case of
         * {@code multianewarray}, the argument is the count of
         * dimensions. In the case of {@code invokeinterface},
         * the argument is the parameter count or'ed with the
         * should-be-zero value left-shifted by 8. In the case of entries
         * of type {@code int}, the {@code value} field always
         * holds the raw value (for convenience of clients).
         *
         * <p><b>Note:</b> In order to avoid giving it a barely-useful
         * visitor all its own, {@code newarray} also uses this
         * form, passing {@code value} as the array type code and
         * {@code cst} as a {@link com.pranav.ide.dx.rop.cst.CstType} instance
         * corresponding to the array type.</p>
         *
         * @param opcode the opcode
         * @param offset offset to the instruction
         * @param length length of the instruction, in bytes
         * @param cst    {@code non-null;} the constant
         * @param value  additional literal integer argument, if salient
         *               (ignore if not)
         */
        void visitConstant(int opcode, int offset, int length,
                           com.pranav.ide.dx.rop.cst.Constant cst, int value);

        /**
         * Visits an instruction which has a branch target argument.
         *
         * @param opcode the opcode
         * @param offset offset to the instruction
         * @param length length of the instruction, in bytes
         * @param target the absolute (not relative) branch target
         */
        void visitBranch(int opcode, int offset, int length,
                         int target);

        /**
         * Visits a switch instruction.
         *
         * @param opcode  the opcode
         * @param offset  offset to the instruction
         * @param length  length of the instruction, in bytes
         * @param cases   {@code non-null;} list of (value, target)
         *                pairs, plus the default target
         * @param padding the bytes found in the padding area (if any),
         *                packed
         */
        void visitSwitch(int opcode, int offset, int length,
                         com.pranav.ide.dx.cf.code.SwitchList cases, int padding);

        /**
         * Visits a newarray instruction.
         *
         * @param offset   offset to the instruction
         * @param length   length of the instruction, in bytes
         * @param type     {@code non-null;} the type of the array
         * @param initVals {@code non-null;} list of bytecode offsets
         *                 for init values
         */
        void visitNewarray(int offset, int length, com.pranav.ide.dx.rop.cst.CstType type,
                           ArrayList<com.pranav.ide.dx.rop.cst.Constant> initVals);

        /**
         * Get previous bytecode offset
         *
         * @return return the recored offset of the previous bytecode
         */
        int getPreviousOffset();

        /**
         * Set previous bytecode offset
         *
         * @param offset offset of the previous fully parsed bytecode
         */
        void setPreviousOffset(int offset);
    }

    /**
     * Base implementation of {@link Visitor}, which has empty method
     * bodies for all methods.
     */
    public static class BaseVisitor implements Visitor {

        /**
         * offset of the previously parsed bytecode
         */
        private int previousOffset;

        BaseVisitor() {
            previousOffset = -1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitInvalid(int opcode, int offset, int length) {
            // This space intentionally left blank.
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitNoArgs(int opcode, int offset, int length,
                                com.pranav.ide.dx.rop.type.Type type) {
            // This space intentionally left blank.
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitLocal(int opcode, int offset, int length,
                               int idx, com.pranav.ide.dx.rop.type.Type type, int value) {
            // This space intentionally left blank.
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitConstant(int opcode, int offset, int length,
                                  com.pranav.ide.dx.rop.cst.Constant cst, int value) {
            // This space intentionally left blank.
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitBranch(int opcode, int offset, int length,
                                int target) {
            // This space intentionally left blank.
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitSwitch(int opcode, int offset, int length,
                                com.pranav.ide.dx.cf.code.SwitchList cases, int padding) {
            // This space intentionally left blank.
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitNewarray(int offset, int length, com.pranav.ide.dx.rop.cst.CstType type,
                                  ArrayList<com.pranav.ide.dx.rop.cst.Constant> initValues) {
            // This space intentionally left blank.
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getPreviousOffset() {
            return previousOffset;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setPreviousOffset(int offset) {
            previousOffset = offset;
        }
    }

    /**
     * Implementation of {@link Visitor}, which just pays attention
     * to constant values.
     */
    class ConstantParserVisitor extends BaseVisitor {
        com.pranav.ide.dx.rop.cst.Constant cst;
        int length;
        int value;

        /**
         * Empty constructor
         */
        ConstantParserVisitor() {
        }

        private void clear() {
            length = 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitInvalid(int opcode, int offset, int length) {
            clear();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitNoArgs(int opcode, int offset, int length,
                                com.pranav.ide.dx.rop.type.Type type) {
            clear();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitLocal(int opcode, int offset, int length,
                               int idx, Type type, int value) {
            clear();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitConstant(int opcode, int offset, int length,
                                  com.pranav.ide.dx.rop.cst.Constant cst, int value) {
            this.cst = cst;
            this.length = length;
            this.value = value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitBranch(int opcode, int offset, int length,
                                int target) {
            clear();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitSwitch(int opcode, int offset, int length,
                                SwitchList cases, int padding) {
            clear();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visitNewarray(int offset, int length, CstType type,
                                  ArrayList<Constant> initVals) {
            clear();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getPreviousOffset() {
            // Intentionally left empty
            return -1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setPreviousOffset(int offset) {
            // Intentionally left empty
        }
    }
}
