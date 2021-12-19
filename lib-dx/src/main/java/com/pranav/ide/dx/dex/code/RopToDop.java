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

package com.pranav.ide.dx.dex.code;

import com.pranav.ide.dx.rop.code.Insn;
import com.pranav.ide.dx.rop.code.RegOps;
import com.pranav.ide.dx.rop.code.RegisterSpec;
import com.pranav.ide.dx.rop.code.Rop;
import com.pranav.ide.dx.rop.code.Rops;
import com.pranav.ide.dx.rop.code.ThrowingCstInsn;
import com.pranav.ide.dx.rop.cst.Constant;
import com.pranav.ide.dx.rop.cst.CstFieldRef;
import com.pranav.ide.dx.rop.cst.CstMethodHandle;
import com.pranav.ide.dx.rop.cst.CstProtoRef;
import com.pranav.ide.dx.rop.cst.CstString;
import com.pranav.ide.dx.rop.cst.CstType;
import com.pranav.ide.dx.rop.type.Type;

import java.util.HashMap;

/**
 * Translator from rop-level {@link com.pranav.ide.dx.rop.code.Insn} instances to corresponding
 * {@link com.pranav.ide.dx.dex.code.Dop} instances.
 */
public final class RopToDop {
    /**
     * {@code non-null;} map from all the common rops to dalvik opcodes
     */
    private static final HashMap<com.pranav.ide.dx.rop.code.Rop, com.pranav.ide.dx.dex.code.Dop> MAP;

    static {
        /*
         * Note: The choices made here are to pick the optimistically
         * smallest Dalvik opcode, and leave it to later processing to
         * pessimize. See the automatically-generated comment above
         * for reference.
         */
        MAP = new HashMap<com.pranav.ide.dx.rop.code.Rop, com.pranav.ide.dx.dex.code.Dop>(400);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.NOP, com.pranav.ide.dx.dex.code.Dops.NOP);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.MOVE_INT, com.pranav.ide.dx.dex.code.Dops.MOVE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.MOVE_LONG, com.pranav.ide.dx.dex.code.Dops.MOVE_WIDE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.MOVE_FLOAT, com.pranav.ide.dx.dex.code.Dops.MOVE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.MOVE_DOUBLE, com.pranav.ide.dx.dex.code.Dops.MOVE_WIDE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.MOVE_OBJECT, com.pranav.ide.dx.dex.code.Dops.MOVE_OBJECT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.MOVE_PARAM_INT, com.pranav.ide.dx.dex.code.Dops.MOVE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.MOVE_PARAM_LONG, com.pranav.ide.dx.dex.code.Dops.MOVE_WIDE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.MOVE_PARAM_FLOAT, com.pranav.ide.dx.dex.code.Dops.MOVE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.MOVE_PARAM_DOUBLE, com.pranav.ide.dx.dex.code.Dops.MOVE_WIDE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.MOVE_PARAM_OBJECT, com.pranav.ide.dx.dex.code.Dops.MOVE_OBJECT);

        /*
         * Note: No entry for MOVE_EXCEPTION, since it varies by
         * exception type. (That is, there is no unique instance to
         * add to the map.)
         */

        MAP.put(com.pranav.ide.dx.rop.code.Rops.CONST_INT, com.pranav.ide.dx.dex.code.Dops.CONST_4);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CONST_LONG, com.pranav.ide.dx.dex.code.Dops.CONST_WIDE_16);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CONST_FLOAT, com.pranav.ide.dx.dex.code.Dops.CONST_4);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CONST_DOUBLE, com.pranav.ide.dx.dex.code.Dops.CONST_WIDE_16);

        /*
         * Note: No entry for CONST_OBJECT, since it needs to turn
         * into either CONST_STRING or CONST_CLASS.
         */

        /*
         * TODO: I think the only case of this is for null, and
         * const/4 should cover that.
         */
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CONST_OBJECT_NOTHROW, com.pranav.ide.dx.dex.code.Dops.CONST_4);

        MAP.put(com.pranav.ide.dx.rop.code.Rops.GOTO, com.pranav.ide.dx.dex.code.Dops.GOTO);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.IF_EQZ_INT, com.pranav.ide.dx.dex.code.Dops.IF_EQZ);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.IF_NEZ_INT, com.pranav.ide.dx.dex.code.Dops.IF_NEZ);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.IF_LTZ_INT, com.pranav.ide.dx.dex.code.Dops.IF_LTZ);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.IF_GEZ_INT, com.pranav.ide.dx.dex.code.Dops.IF_GEZ);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.IF_LEZ_INT, com.pranav.ide.dx.dex.code.Dops.IF_LEZ);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.IF_GTZ_INT, com.pranav.ide.dx.dex.code.Dops.IF_GTZ);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.IF_EQZ_OBJECT, com.pranav.ide.dx.dex.code.Dops.IF_EQZ);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.IF_NEZ_OBJECT, com.pranav.ide.dx.dex.code.Dops.IF_NEZ);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.IF_EQ_INT, com.pranav.ide.dx.dex.code.Dops.IF_EQ);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.IF_NE_INT, com.pranav.ide.dx.dex.code.Dops.IF_NE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.IF_LT_INT, com.pranav.ide.dx.dex.code.Dops.IF_LT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.IF_GE_INT, com.pranav.ide.dx.dex.code.Dops.IF_GE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.IF_LE_INT, com.pranav.ide.dx.dex.code.Dops.IF_LE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.IF_GT_INT, com.pranav.ide.dx.dex.code.Dops.IF_GT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.IF_EQ_OBJECT, com.pranav.ide.dx.dex.code.Dops.IF_EQ);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.IF_NE_OBJECT, com.pranav.ide.dx.dex.code.Dops.IF_NE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.SWITCH, com.pranav.ide.dx.dex.code.Dops.SPARSE_SWITCH);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.ADD_INT, com.pranav.ide.dx.dex.code.Dops.ADD_INT_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.ADD_LONG, com.pranav.ide.dx.dex.code.Dops.ADD_LONG_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.ADD_FLOAT, com.pranav.ide.dx.dex.code.Dops.ADD_FLOAT_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.ADD_DOUBLE, com.pranav.ide.dx.dex.code.Dops.ADD_DOUBLE_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.SUB_INT, com.pranav.ide.dx.dex.code.Dops.SUB_INT_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.SUB_LONG, com.pranav.ide.dx.dex.code.Dops.SUB_LONG_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.SUB_FLOAT, com.pranav.ide.dx.dex.code.Dops.SUB_FLOAT_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.SUB_DOUBLE, com.pranav.ide.dx.dex.code.Dops.SUB_DOUBLE_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.MUL_INT, com.pranav.ide.dx.dex.code.Dops.MUL_INT_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.MUL_LONG, com.pranav.ide.dx.dex.code.Dops.MUL_LONG_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.MUL_FLOAT, com.pranav.ide.dx.dex.code.Dops.MUL_FLOAT_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.MUL_DOUBLE, com.pranav.ide.dx.dex.code.Dops.MUL_DOUBLE_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.DIV_INT, com.pranav.ide.dx.dex.code.Dops.DIV_INT_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.DIV_LONG, com.pranav.ide.dx.dex.code.Dops.DIV_LONG_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.DIV_FLOAT, com.pranav.ide.dx.dex.code.Dops.DIV_FLOAT_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.DIV_DOUBLE, com.pranav.ide.dx.dex.code.Dops.DIV_DOUBLE_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.REM_INT, com.pranav.ide.dx.dex.code.Dops.REM_INT_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.REM_LONG, com.pranav.ide.dx.dex.code.Dops.REM_LONG_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.REM_FLOAT, com.pranav.ide.dx.dex.code.Dops.REM_FLOAT_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.REM_DOUBLE, com.pranav.ide.dx.dex.code.Dops.REM_DOUBLE_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.NEG_INT, com.pranav.ide.dx.dex.code.Dops.NEG_INT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.NEG_LONG, com.pranav.ide.dx.dex.code.Dops.NEG_LONG);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.NEG_FLOAT, com.pranav.ide.dx.dex.code.Dops.NEG_FLOAT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.NEG_DOUBLE, com.pranav.ide.dx.dex.code.Dops.NEG_DOUBLE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.AND_INT, com.pranav.ide.dx.dex.code.Dops.AND_INT_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.AND_LONG, com.pranav.ide.dx.dex.code.Dops.AND_LONG_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.OR_INT, com.pranav.ide.dx.dex.code.Dops.OR_INT_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.OR_LONG, com.pranav.ide.dx.dex.code.Dops.OR_LONG_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.XOR_INT, com.pranav.ide.dx.dex.code.Dops.XOR_INT_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.XOR_LONG, com.pranav.ide.dx.dex.code.Dops.XOR_LONG_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.SHL_INT, com.pranav.ide.dx.dex.code.Dops.SHL_INT_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.SHL_LONG, com.pranav.ide.dx.dex.code.Dops.SHL_LONG_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.SHR_INT, com.pranav.ide.dx.dex.code.Dops.SHR_INT_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.SHR_LONG, com.pranav.ide.dx.dex.code.Dops.SHR_LONG_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.USHR_INT, com.pranav.ide.dx.dex.code.Dops.USHR_INT_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.USHR_LONG, com.pranav.ide.dx.dex.code.Dops.USHR_LONG_2ADDR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.NOT_INT, com.pranav.ide.dx.dex.code.Dops.NOT_INT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.NOT_LONG, com.pranav.ide.dx.dex.code.Dops.NOT_LONG);

        MAP.put(com.pranav.ide.dx.rop.code.Rops.ADD_CONST_INT, com.pranav.ide.dx.dex.code.Dops.ADD_INT_LIT8);
        // Note: No dalvik ops for other types of add_const.

        MAP.put(com.pranav.ide.dx.rop.code.Rops.SUB_CONST_INT, com.pranav.ide.dx.dex.code.Dops.RSUB_INT_LIT8);
        /*
         * Note: No dalvik ops for any type of sub_const; instead
         * there's a *reverse* sub (constant - reg) for ints only.
         */

        MAP.put(com.pranav.ide.dx.rop.code.Rops.MUL_CONST_INT, com.pranav.ide.dx.dex.code.Dops.MUL_INT_LIT8);
        // Note: No dalvik ops for other types of mul_const.

        MAP.put(com.pranav.ide.dx.rop.code.Rops.DIV_CONST_INT, com.pranav.ide.dx.dex.code.Dops.DIV_INT_LIT8);
        // Note: No dalvik ops for other types of div_const.

        MAP.put(com.pranav.ide.dx.rop.code.Rops.REM_CONST_INT, com.pranav.ide.dx.dex.code.Dops.REM_INT_LIT8);
        // Note: No dalvik ops for other types of rem_const.

        MAP.put(com.pranav.ide.dx.rop.code.Rops.AND_CONST_INT, com.pranav.ide.dx.dex.code.Dops.AND_INT_LIT8);
        // Note: No dalvik op for and_const_long.

        MAP.put(com.pranav.ide.dx.rop.code.Rops.OR_CONST_INT, com.pranav.ide.dx.dex.code.Dops.OR_INT_LIT8);
        // Note: No dalvik op for or_const_long.

        MAP.put(com.pranav.ide.dx.rop.code.Rops.XOR_CONST_INT, com.pranav.ide.dx.dex.code.Dops.XOR_INT_LIT8);
        // Note: No dalvik op for xor_const_long.

        MAP.put(com.pranav.ide.dx.rop.code.Rops.SHL_CONST_INT, com.pranav.ide.dx.dex.code.Dops.SHL_INT_LIT8);
        // Note: No dalvik op for shl_const_long.

        MAP.put(com.pranav.ide.dx.rop.code.Rops.SHR_CONST_INT, com.pranav.ide.dx.dex.code.Dops.SHR_INT_LIT8);
        // Note: No dalvik op for shr_const_long.

        MAP.put(com.pranav.ide.dx.rop.code.Rops.USHR_CONST_INT, com.pranav.ide.dx.dex.code.Dops.USHR_INT_LIT8);
        // Note: No dalvik op for shr_const_long.

        MAP.put(com.pranav.ide.dx.rop.code.Rops.CMPL_LONG, com.pranav.ide.dx.dex.code.Dops.CMP_LONG);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CMPL_FLOAT, com.pranav.ide.dx.dex.code.Dops.CMPL_FLOAT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CMPL_DOUBLE, com.pranav.ide.dx.dex.code.Dops.CMPL_DOUBLE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CMPG_FLOAT, com.pranav.ide.dx.dex.code.Dops.CMPG_FLOAT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CMPG_DOUBLE, com.pranav.ide.dx.dex.code.Dops.CMPG_DOUBLE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CONV_L2I, com.pranav.ide.dx.dex.code.Dops.LONG_TO_INT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CONV_F2I, com.pranav.ide.dx.dex.code.Dops.FLOAT_TO_INT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CONV_D2I, com.pranav.ide.dx.dex.code.Dops.DOUBLE_TO_INT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CONV_I2L, com.pranav.ide.dx.dex.code.Dops.INT_TO_LONG);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CONV_F2L, com.pranav.ide.dx.dex.code.Dops.FLOAT_TO_LONG);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CONV_D2L, com.pranav.ide.dx.dex.code.Dops.DOUBLE_TO_LONG);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CONV_I2F, com.pranav.ide.dx.dex.code.Dops.INT_TO_FLOAT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CONV_L2F, com.pranav.ide.dx.dex.code.Dops.LONG_TO_FLOAT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CONV_D2F, com.pranav.ide.dx.dex.code.Dops.DOUBLE_TO_FLOAT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CONV_I2D, com.pranav.ide.dx.dex.code.Dops.INT_TO_DOUBLE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CONV_L2D, com.pranav.ide.dx.dex.code.Dops.LONG_TO_DOUBLE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CONV_F2D, com.pranav.ide.dx.dex.code.Dops.FLOAT_TO_DOUBLE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.TO_BYTE, com.pranav.ide.dx.dex.code.Dops.INT_TO_BYTE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.TO_CHAR, com.pranav.ide.dx.dex.code.Dops.INT_TO_CHAR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.TO_SHORT, com.pranav.ide.dx.dex.code.Dops.INT_TO_SHORT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.RETURN_VOID, com.pranav.ide.dx.dex.code.Dops.RETURN_VOID);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.RETURN_INT, com.pranav.ide.dx.dex.code.Dops.RETURN);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.RETURN_LONG, com.pranav.ide.dx.dex.code.Dops.RETURN_WIDE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.RETURN_FLOAT, com.pranav.ide.dx.dex.code.Dops.RETURN);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.RETURN_DOUBLE, com.pranav.ide.dx.dex.code.Dops.RETURN_WIDE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.RETURN_OBJECT, com.pranav.ide.dx.dex.code.Dops.RETURN_OBJECT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.ARRAY_LENGTH, com.pranav.ide.dx.dex.code.Dops.ARRAY_LENGTH);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.THROW, com.pranav.ide.dx.dex.code.Dops.THROW);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.MONITOR_ENTER, com.pranav.ide.dx.dex.code.Dops.MONITOR_ENTER);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.MONITOR_EXIT, com.pranav.ide.dx.dex.code.Dops.MONITOR_EXIT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.AGET_INT, com.pranav.ide.dx.dex.code.Dops.AGET);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.AGET_LONG, com.pranav.ide.dx.dex.code.Dops.AGET_WIDE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.AGET_FLOAT, com.pranav.ide.dx.dex.code.Dops.AGET);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.AGET_DOUBLE, com.pranav.ide.dx.dex.code.Dops.AGET_WIDE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.AGET_OBJECT, com.pranav.ide.dx.dex.code.Dops.AGET_OBJECT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.AGET_BOOLEAN, com.pranav.ide.dx.dex.code.Dops.AGET_BOOLEAN);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.AGET_BYTE, com.pranav.ide.dx.dex.code.Dops.AGET_BYTE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.AGET_CHAR, com.pranav.ide.dx.dex.code.Dops.AGET_CHAR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.AGET_SHORT, com.pranav.ide.dx.dex.code.Dops.AGET_SHORT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.APUT_INT, com.pranav.ide.dx.dex.code.Dops.APUT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.APUT_LONG, com.pranav.ide.dx.dex.code.Dops.APUT_WIDE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.APUT_FLOAT, com.pranav.ide.dx.dex.code.Dops.APUT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.APUT_DOUBLE, com.pranav.ide.dx.dex.code.Dops.APUT_WIDE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.APUT_OBJECT, com.pranav.ide.dx.dex.code.Dops.APUT_OBJECT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.APUT_BOOLEAN, com.pranav.ide.dx.dex.code.Dops.APUT_BOOLEAN);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.APUT_BYTE, com.pranav.ide.dx.dex.code.Dops.APUT_BYTE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.APUT_CHAR, com.pranav.ide.dx.dex.code.Dops.APUT_CHAR);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.APUT_SHORT, com.pranav.ide.dx.dex.code.Dops.APUT_SHORT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.NEW_INSTANCE, com.pranav.ide.dx.dex.code.Dops.NEW_INSTANCE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.CHECK_CAST, com.pranav.ide.dx.dex.code.Dops.CHECK_CAST);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.INSTANCE_OF, com.pranav.ide.dx.dex.code.Dops.INSTANCE_OF);

        MAP.put(com.pranav.ide.dx.rop.code.Rops.GET_FIELD_LONG, com.pranav.ide.dx.dex.code.Dops.IGET_WIDE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.GET_FIELD_FLOAT, com.pranav.ide.dx.dex.code.Dops.IGET);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.GET_FIELD_DOUBLE, com.pranav.ide.dx.dex.code.Dops.IGET_WIDE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.GET_FIELD_OBJECT, com.pranav.ide.dx.dex.code.Dops.IGET_OBJECT);
        /*
         * Note: No map entries for get_field_* for non-long integral types,
         * since they need to be handled specially (see dopFor() below).
         */

        MAP.put(com.pranav.ide.dx.rop.code.Rops.GET_STATIC_LONG, com.pranav.ide.dx.dex.code.Dops.SGET_WIDE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.GET_STATIC_FLOAT, com.pranav.ide.dx.dex.code.Dops.SGET);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.GET_STATIC_DOUBLE, com.pranav.ide.dx.dex.code.Dops.SGET_WIDE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.GET_STATIC_OBJECT, com.pranav.ide.dx.dex.code.Dops.SGET_OBJECT);
        /*
         * Note: No map entries for get_static* for non-long integral types,
         * since they need to be handled specially (see dopFor() below).
         */

        MAP.put(com.pranav.ide.dx.rop.code.Rops.PUT_FIELD_LONG, com.pranav.ide.dx.dex.code.Dops.IPUT_WIDE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.PUT_FIELD_FLOAT, com.pranav.ide.dx.dex.code.Dops.IPUT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.PUT_FIELD_DOUBLE, com.pranav.ide.dx.dex.code.Dops.IPUT_WIDE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.PUT_FIELD_OBJECT, com.pranav.ide.dx.dex.code.Dops.IPUT_OBJECT);
        /*
         * Note: No map entries for put_field_* for non-long integral types,
         * since they need to be handled specially (see dopFor() below).
         */

        MAP.put(com.pranav.ide.dx.rop.code.Rops.PUT_STATIC_LONG, com.pranav.ide.dx.dex.code.Dops.SPUT_WIDE);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.PUT_STATIC_FLOAT, com.pranav.ide.dx.dex.code.Dops.SPUT);
        MAP.put(com.pranav.ide.dx.rop.code.Rops.PUT_STATIC_DOUBLE, com.pranav.ide.dx.dex.code.Dops.SPUT_WIDE);
        MAP.put(Rops.PUT_STATIC_OBJECT, com.pranav.ide.dx.dex.code.Dops.SPUT_OBJECT);
        /*
         * Note: No map entries for put_static* for non-long integral types,
         * since they need to be handled specially (see dopFor() below).
         */

        /*
         * Note: No map entries for invoke*, new_array, and
         * filled_new_array, since they need to be handled specially
         * (see dopFor() below).
         */
    }

    /*
     * The following comment lists each opcode that should be considered
     * the "head" of an opcode chain, in terms of the process of fitting
     * an instruction's arguments to an actual opcode. This list is
     * automatically generated and may be of use in double-checking the
     * manually-generated static initialization code for this class.
     *
     * TODO: Make opcode-gen produce useful code in this case instead
     * of just a comment.
     */

    // BEGIN(first-opcodes); GENERATED AUTOMATICALLY BY opcode-gen
    //     Opcodes.NOP
    //     Opcodes.MOVE
    //     Opcodes.MOVE_WIDE
    //     Opcodes.MOVE_OBJECT
    //     Opcodes.MOVE_RESULT
    //     Opcodes.MOVE_RESULT_WIDE
    //     Opcodes.MOVE_RESULT_OBJECT
    //     Opcodes.MOVE_EXCEPTION
    //     Opcodes.RETURN_VOID
    //     Opcodes.RETURN
    //     Opcodes.RETURN_WIDE
    //     Opcodes.RETURN_OBJECT
    //     Opcodes.CONST_4
    //     Opcodes.CONST_WIDE_16
    //     Opcodes.CONST_STRING
    //     Opcodes.CONST_CLASS
    //     Opcodes.MONITOR_ENTER
    //     Opcodes.MONITOR_EXIT
    //     Opcodes.CHECK_CAST
    //     Opcodes.INSTANCE_OF
    //     Opcodes.ARRAY_LENGTH
    //     Opcodes.NEW_INSTANCE
    //     Opcodes.NEW_ARRAY
    //     Opcodes.FILLED_NEW_ARRAY
    //     Opcodes.FILL_ARRAY_DATA
    //     Opcodes.THROW
    //     Opcodes.GOTO
    //     Opcodes.PACKED_SWITCH
    //     Opcodes.SPARSE_SWITCH
    //     Opcodes.CMPL_FLOAT
    //     Opcodes.CMPG_FLOAT
    //     Opcodes.CMPL_DOUBLE
    //     Opcodes.CMPG_DOUBLE
    //     Opcodes.CMP_LONG
    //     Opcodes.IF_EQ
    //     Opcodes.IF_NE
    //     Opcodes.IF_LT
    //     Opcodes.IF_GE
    //     Opcodes.IF_GT
    //     Opcodes.IF_LE
    //     Opcodes.IF_EQZ
    //     Opcodes.IF_NEZ
    //     Opcodes.IF_LTZ
    //     Opcodes.IF_GEZ
    //     Opcodes.IF_GTZ
    //     Opcodes.IF_LEZ
    //     Opcodes.AGET
    //     Opcodes.AGET_WIDE
    //     Opcodes.AGET_OBJECT
    //     Opcodes.AGET_BOOLEAN
    //     Opcodes.AGET_BYTE
    //     Opcodes.AGET_CHAR
    //     Opcodes.AGET_SHORT
    //     Opcodes.APUT
    //     Opcodes.APUT_WIDE
    //     Opcodes.APUT_OBJECT
    //     Opcodes.APUT_BOOLEAN
    //     Opcodes.APUT_BYTE
    //     Opcodes.APUT_CHAR
    //     Opcodes.APUT_SHORT
    //     Opcodes.IGET
    //     Opcodes.IGET_WIDE
    //     Opcodes.IGET_OBJECT
    //     Opcodes.IGET_BOOLEAN
    //     Opcodes.IGET_BYTE
    //     Opcodes.IGET_CHAR
    //     Opcodes.IGET_SHORT
    //     Opcodes.IPUT
    //     Opcodes.IPUT_WIDE
    //     Opcodes.IPUT_OBJECT
    //     Opcodes.IPUT_BOOLEAN
    //     Opcodes.IPUT_BYTE
    //     Opcodes.IPUT_CHAR
    //     Opcodes.IPUT_SHORT
    //     Opcodes.SGET
    //     Opcodes.SGET_WIDE
    //     Opcodes.SGET_OBJECT
    //     Opcodes.SGET_BOOLEAN
    //     Opcodes.SGET_BYTE
    //     Opcodes.SGET_CHAR
    //     Opcodes.SGET_SHORT
    //     Opcodes.SPUT
    //     Opcodes.SPUT_WIDE
    //     Opcodes.SPUT_OBJECT
    //     Opcodes.SPUT_BOOLEAN
    //     Opcodes.SPUT_BYTE
    //     Opcodes.SPUT_CHAR
    //     Opcodes.SPUT_SHORT
    //     Opcodes.INVOKE_VIRTUAL
    //     Opcodes.INVOKE_SUPER
    //     Opcodes.INVOKE_DIRECT
    //     Opcodes.INVOKE_STATIC
    //     Opcodes.INVOKE_INTERFACE
    //     Opcodes.NEG_INT
    //     Opcodes.NOT_INT
    //     Opcodes.NEG_LONG
    //     Opcodes.NOT_LONG
    //     Opcodes.NEG_FLOAT
    //     Opcodes.NEG_DOUBLE
    //     Opcodes.INT_TO_LONG
    //     Opcodes.INT_TO_FLOAT
    //     Opcodes.INT_TO_DOUBLE
    //     Opcodes.LONG_TO_INT
    //     Opcodes.LONG_TO_FLOAT
    //     Opcodes.LONG_TO_DOUBLE
    //     Opcodes.FLOAT_TO_INT
    //     Opcodes.FLOAT_TO_LONG
    //     Opcodes.FLOAT_TO_DOUBLE
    //     Opcodes.DOUBLE_TO_INT
    //     Opcodes.DOUBLE_TO_LONG
    //     Opcodes.DOUBLE_TO_FLOAT
    //     Opcodes.INT_TO_BYTE
    //     Opcodes.INT_TO_CHAR
    //     Opcodes.INT_TO_SHORT
    //     Opcodes.ADD_INT_2ADDR
    //     Opcodes.SUB_INT_2ADDR
    //     Opcodes.MUL_INT_2ADDR
    //     Opcodes.DIV_INT_2ADDR
    //     Opcodes.REM_INT_2ADDR
    //     Opcodes.AND_INT_2ADDR
    //     Opcodes.OR_INT_2ADDR
    //     Opcodes.XOR_INT_2ADDR
    //     Opcodes.SHL_INT_2ADDR
    //     Opcodes.SHR_INT_2ADDR
    //     Opcodes.USHR_INT_2ADDR
    //     Opcodes.ADD_LONG_2ADDR
    //     Opcodes.SUB_LONG_2ADDR
    //     Opcodes.MUL_LONG_2ADDR
    //     Opcodes.DIV_LONG_2ADDR
    //     Opcodes.REM_LONG_2ADDR
    //     Opcodes.AND_LONG_2ADDR
    //     Opcodes.OR_LONG_2ADDR
    //     Opcodes.XOR_LONG_2ADDR
    //     Opcodes.SHL_LONG_2ADDR
    //     Opcodes.SHR_LONG_2ADDR
    //     Opcodes.USHR_LONG_2ADDR
    //     Opcodes.ADD_FLOAT_2ADDR
    //     Opcodes.SUB_FLOAT_2ADDR
    //     Opcodes.MUL_FLOAT_2ADDR
    //     Opcodes.DIV_FLOAT_2ADDR
    //     Opcodes.REM_FLOAT_2ADDR
    //     Opcodes.ADD_DOUBLE_2ADDR
    //     Opcodes.SUB_DOUBLE_2ADDR
    //     Opcodes.MUL_DOUBLE_2ADDR
    //     Opcodes.DIV_DOUBLE_2ADDR
    //     Opcodes.REM_DOUBLE_2ADDR
    //     Opcodes.ADD_INT_LIT8
    //     Opcodes.RSUB_INT_LIT8
    //     Opcodes.MUL_INT_LIT8
    //     Opcodes.DIV_INT_LIT8
    //     Opcodes.REM_INT_LIT8
    //     Opcodes.AND_INT_LIT8
    //     Opcodes.OR_INT_LIT8
    //     Opcodes.XOR_INT_LIT8
    //     Opcodes.SHL_INT_LIT8
    //     Opcodes.SHR_INT_LIT8
    //     Opcodes.USHR_INT_LIT8
    //     Opcodes.INVOKE_POLYMORPHIC
    //     Opcodes.INVOKE_CUSTOM
    //     Opcodes.CONST_METHOD_HANDLE
    //     Opcodes.CONST_METHOD_TYPE
    // END(first-opcodes)

    /**
     * This class is uninstantiable.
     */
    private RopToDop() {
        // This space intentionally left blank.
    }

    /**
     * Returns the dalvik opcode appropriate for the given register-based
     * instruction.
     *
     * @param insn {@code non-null;} the original instruction
     * @return the corresponding dalvik opcode; one of the constants in
     * {@link com.pranav.ide.dx.dex.code.Dops}
     */
    public static com.pranav.ide.dx.dex.code.Dop dopFor(Insn insn) {
        Rop rop = insn.getOpcode();

        /*
         * First, just try looking up the rop in the MAP of easy
         * cases.
         */
        Dop result = MAP.get(rop);
        if (result != null) {
            return result;
        }

        /*
         * There was no easy case for the rop, so look up the opcode, and
         * do something special for each:
         *
         * The move_exception, new_array, filled_new_array, and
         * invoke* opcodes won't be found in MAP, since they'll each
         * have different source and/or result register types / lists.
         *
         * The get* and put* opcodes for (non-long) integral types
         * aren't in the map, since the type signatures aren't
         * sufficient to distinguish between the types (the salient
         * source or result will always be just "int").
         *
         * And const instruction need to distinguish between strings and
         * classes.
         */

        switch (rop.getOpcode()) {
            case com.pranav.ide.dx.rop.code.RegOps.MOVE_EXCEPTION:
                return com.pranav.ide.dx.dex.code.Dops.MOVE_EXCEPTION;
            case com.pranav.ide.dx.rop.code.RegOps.INVOKE_STATIC:
                return com.pranav.ide.dx.dex.code.Dops.INVOKE_STATIC;
            case com.pranav.ide.dx.rop.code.RegOps.INVOKE_VIRTUAL:
                return com.pranav.ide.dx.dex.code.Dops.INVOKE_VIRTUAL;
            case com.pranav.ide.dx.rop.code.RegOps.INVOKE_SUPER:
                return com.pranav.ide.dx.dex.code.Dops.INVOKE_SUPER;
            case com.pranav.ide.dx.rop.code.RegOps.INVOKE_DIRECT:
                return com.pranav.ide.dx.dex.code.Dops.INVOKE_DIRECT;
            case com.pranav.ide.dx.rop.code.RegOps.INVOKE_INTERFACE:
                return com.pranav.ide.dx.dex.code.Dops.INVOKE_INTERFACE;
            case com.pranav.ide.dx.rop.code.RegOps.INVOKE_POLYMORPHIC:
                return com.pranav.ide.dx.dex.code.Dops.INVOKE_POLYMORPHIC;
            case com.pranav.ide.dx.rop.code.RegOps.INVOKE_CUSTOM:
                return com.pranav.ide.dx.dex.code.Dops.INVOKE_CUSTOM;
            case com.pranav.ide.dx.rop.code.RegOps.NEW_ARRAY:
                return com.pranav.ide.dx.dex.code.Dops.NEW_ARRAY;
            case com.pranav.ide.dx.rop.code.RegOps.FILLED_NEW_ARRAY:
                return com.pranav.ide.dx.dex.code.Dops.FILLED_NEW_ARRAY;
            case com.pranav.ide.dx.rop.code.RegOps.FILL_ARRAY_DATA:
                return com.pranav.ide.dx.dex.code.Dops.FILL_ARRAY_DATA;
            case com.pranav.ide.dx.rop.code.RegOps.MOVE_RESULT: {
                RegisterSpec resultReg = insn.getResult();

                if (resultReg == null) {
                    return com.pranav.ide.dx.dex.code.Dops.NOP;
                } else {
                    switch (resultReg.getBasicType()) {
                        case com.pranav.ide.dx.rop.type.Type.BT_INT:
                        case com.pranav.ide.dx.rop.type.Type.BT_FLOAT:
                        case com.pranav.ide.dx.rop.type.Type.BT_BOOLEAN:
                        case com.pranav.ide.dx.rop.type.Type.BT_BYTE:
                        case com.pranav.ide.dx.rop.type.Type.BT_CHAR:
                        case com.pranav.ide.dx.rop.type.Type.BT_SHORT:
                            return com.pranav.ide.dx.dex.code.Dops.MOVE_RESULT;
                        case com.pranav.ide.dx.rop.type.Type.BT_LONG:
                        case com.pranav.ide.dx.rop.type.Type.BT_DOUBLE:
                            return com.pranav.ide.dx.dex.code.Dops.MOVE_RESULT_WIDE;
                        case com.pranav.ide.dx.rop.type.Type.BT_OBJECT:
                            return com.pranav.ide.dx.dex.code.Dops.MOVE_RESULT_OBJECT;
                        default: {
                            throw new RuntimeException("Unexpected basic type");
                        }
                    }
                }
            }

            case com.pranav.ide.dx.rop.code.RegOps.GET_FIELD: {
                com.pranav.ide.dx.rop.cst.CstFieldRef ref =
                        (com.pranav.ide.dx.rop.cst.CstFieldRef) ((com.pranav.ide.dx.rop.code.ThrowingCstInsn) insn).getConstant();
                int basicType = ref.getBasicType();
                switch (basicType) {
                    case com.pranav.ide.dx.rop.type.Type.BT_BOOLEAN:
                        return com.pranav.ide.dx.dex.code.Dops.IGET_BOOLEAN;
                    case com.pranav.ide.dx.rop.type.Type.BT_BYTE:
                        return com.pranav.ide.dx.dex.code.Dops.IGET_BYTE;
                    case com.pranav.ide.dx.rop.type.Type.BT_CHAR:
                        return com.pranav.ide.dx.dex.code.Dops.IGET_CHAR;
                    case com.pranav.ide.dx.rop.type.Type.BT_SHORT:
                        return com.pranav.ide.dx.dex.code.Dops.IGET_SHORT;
                    case com.pranav.ide.dx.rop.type.Type.BT_INT:
                        return com.pranav.ide.dx.dex.code.Dops.IGET;
                }
                break;
            }
            case com.pranav.ide.dx.rop.code.RegOps.PUT_FIELD: {
                com.pranav.ide.dx.rop.cst.CstFieldRef ref =
                        (com.pranav.ide.dx.rop.cst.CstFieldRef) ((com.pranav.ide.dx.rop.code.ThrowingCstInsn) insn).getConstant();
                int basicType = ref.getBasicType();
                switch (basicType) {
                    case com.pranav.ide.dx.rop.type.Type.BT_BOOLEAN:
                        return com.pranav.ide.dx.dex.code.Dops.IPUT_BOOLEAN;
                    case com.pranav.ide.dx.rop.type.Type.BT_BYTE:
                        return com.pranav.ide.dx.dex.code.Dops.IPUT_BYTE;
                    case com.pranav.ide.dx.rop.type.Type.BT_CHAR:
                        return com.pranav.ide.dx.dex.code.Dops.IPUT_CHAR;
                    case com.pranav.ide.dx.rop.type.Type.BT_SHORT:
                        return com.pranav.ide.dx.dex.code.Dops.IPUT_SHORT;
                    case com.pranav.ide.dx.rop.type.Type.BT_INT:
                        return com.pranav.ide.dx.dex.code.Dops.IPUT;
                }
                break;
            }
            case com.pranav.ide.dx.rop.code.RegOps.GET_STATIC: {
                com.pranav.ide.dx.rop.cst.CstFieldRef ref =
                        (com.pranav.ide.dx.rop.cst.CstFieldRef) ((com.pranav.ide.dx.rop.code.ThrowingCstInsn) insn).getConstant();
                int basicType = ref.getBasicType();
                switch (basicType) {
                    case com.pranav.ide.dx.rop.type.Type.BT_BOOLEAN:
                        return com.pranav.ide.dx.dex.code.Dops.SGET_BOOLEAN;
                    case com.pranav.ide.dx.rop.type.Type.BT_BYTE:
                        return com.pranav.ide.dx.dex.code.Dops.SGET_BYTE;
                    case com.pranav.ide.dx.rop.type.Type.BT_CHAR:
                        return com.pranav.ide.dx.dex.code.Dops.SGET_CHAR;
                    case com.pranav.ide.dx.rop.type.Type.BT_SHORT:
                        return com.pranav.ide.dx.dex.code.Dops.SGET_SHORT;
                    case com.pranav.ide.dx.rop.type.Type.BT_INT:
                        return com.pranav.ide.dx.dex.code.Dops.SGET;
                }
                break;
            }
            case com.pranav.ide.dx.rop.code.RegOps.PUT_STATIC: {
                com.pranav.ide.dx.rop.cst.CstFieldRef ref =
                        (CstFieldRef) ((com.pranav.ide.dx.rop.code.ThrowingCstInsn) insn).getConstant();
                int basicType = ref.getBasicType();
                switch (basicType) {
                    case com.pranav.ide.dx.rop.type.Type.BT_BOOLEAN:
                        return com.pranav.ide.dx.dex.code.Dops.SPUT_BOOLEAN;
                    case com.pranav.ide.dx.rop.type.Type.BT_BYTE:
                        return com.pranav.ide.dx.dex.code.Dops.SPUT_BYTE;
                    case com.pranav.ide.dx.rop.type.Type.BT_CHAR:
                        return com.pranav.ide.dx.dex.code.Dops.SPUT_CHAR;
                    case com.pranav.ide.dx.rop.type.Type.BT_SHORT:
                        return com.pranav.ide.dx.dex.code.Dops.SPUT_SHORT;
                    case Type.BT_INT:
                        return com.pranav.ide.dx.dex.code.Dops.SPUT;
                }
                break;
            }
            case RegOps.CONST: {
                Constant cst = ((ThrowingCstInsn) insn).getConstant();
                if (cst instanceof CstType) {
                    return com.pranav.ide.dx.dex.code.Dops.CONST_CLASS;
                } else if (cst instanceof CstString) {
                    return com.pranav.ide.dx.dex.code.Dops.CONST_STRING;
                } else if (cst instanceof CstMethodHandle) {
                    return com.pranav.ide.dx.dex.code.Dops.CONST_METHOD_HANDLE;
                } else if (cst instanceof CstProtoRef) {
                    return Dops.CONST_METHOD_TYPE;
                } else {
                    throw new RuntimeException("Unexpected constant type");
                }
            }
        }

        throw new RuntimeException("unknown rop: " + rop);
    }
}
